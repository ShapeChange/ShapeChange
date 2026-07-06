/**
 * ShapeChange - processing application schemas for geographic information
 *
 * This file is part of ShapeChange. ShapeChange takes a ISO 19109 
 * Application Schema from a UML model and translates it into a 
 * GML Application Schema or other implementation representations.
 *
 * Additional information about the software can be found at
 * http://shapechange.net/
 *
 * (c) 2002-2026 interactive instruments GmbH, Bonn, Germany
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact:
 * interactive instruments GmbH
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.shapechange.core.target.coretable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.cycle.DirectedSimpleCycles;
import org.jgrapht.alg.cycle.TiernanSimpleCycles;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.traverse.BreadthFirstIterator;

import de.interactive_instruments.shapechange.core.MessageSource;
import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.ShapeChangeResult.MessageContext;
import de.interactive_instruments.shapechange.core.model.AssociationInfo;
import de.interactive_instruments.shapechange.core.model.ClassInfo;
import de.interactive_instruments.shapechange.core.model.PropertyInfo;
import de.interactive_instruments.shapechange.core.target.coretable.CoretableNavigableRole.DependentPart;
import de.interactive_instruments.shapechange.core.target.coretable.CoretableNavigableRole.RelDirection;
import de.interactive_instruments.shapechange.core.transformation.flattening.PropertySetEdge;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class CoretableNavigableRolesConfigBuilder implements MessageSource {

    private ShapeChangeResult result;

    public CoretableNavigableRolesConfigBuilder(ShapeChangeResult result) {
	this.result = result;
    }

    public SortedSet<CoretableNavigableRole> getNavigableRolesConfig(SortedSet<ClassInfo> featureObjectAndMixinTypes,
	    String appSchema, String appSchemaVersion) {

	SortedSet<CoretableNavigableRole> navigableRoles = new TreeSet<>();

	/*
	 * Identify associations between classes in the schemas selected for processing.
	 */

	SortedSet<AssociationInfo> ais = new TreeSet<>();

	for (ClassInfo ci : featureObjectAndMixinTypes) {
	    for (PropertyInfo pi : ci.properties().values()) {
		if (!pi.isAttribute()) {
		    AssociationInfo ai = pi.association();
		    if (featureObjectAndMixinTypes.contains(ai.end1().inClass())
			    && featureObjectAndMixinTypes.contains(ai.end2().inClass())) {
			ais.add(ai);
		    }
		}
	    }
	}

	SortedSet<ClassInfo> sourceFeatureTypesForOwnershipRole = new TreeSet<>();
	SortedSet<ClassInfo> targetFeatureTypesForOwnershipRole = new TreeSet<>();

	for (AssociationInfo ai : ais) {

	    /*
	     * Determine if the association roles have existential dependency infos, and if
	     * the according tags have correct values.
	     */
	    boolean sourceHasExistentialDependencyInfo = hasExistentialDependencyInfo(ai.end1());
	    boolean targetHasExistentialDependencyInfo = hasExistentialDependencyInfo(ai.end2());

	    /*
	     * Start by assuming a valid setup if at least one or none of the association
	     * roles has existential dependency type information.
	     */
	    boolean isValidSetup = sourceHasExistentialDependencyInfo || targetHasExistentialDependencyInfo
		    || (!sourceHasExistentialDependencyInfo && !targetHasExistentialDependencyInfo);

	    /*
	     * Now check if roles with existential dependency type information are
	     * navigable.
	     */
	    if (sourceHasExistentialDependencyInfo) {
		isValidSetup = isValidSetup & checkNavigabilityOfRoleWithExistentialDependencyInfo(ai.end1());
	    }
	    if (targetHasExistentialDependencyInfo) {
		isValidSetup = isValidSetup & checkNavigabilityOfRoleWithExistentialDependencyInfo(ai.end2());
	    }

	    /*
	     * Now check that bi-directional associations do not have the same existential
	     * dependency type info on both ends
	     * 
	     * REMOVED:, and if one role has existential dependency type info, then the
	     * other must have it, too.
	     */
	    if (ai.end1().isNavigable() && ai.end2().isNavigable()) {

		if (sourceHasExistentialDependencyInfo && targetHasExistentialDependencyInfo) {

		    /*
		     * If both roles have existential dependency type information, they must not be
		     * equal.
		     */

		    if (ai.end1().taggedValue(CoretableConstants.TV_EX_DEP_TYPE).trim()
			    .equals(ai.end2().taggedValue(CoretableConstants.TV_EX_DEP_TYPE).trim())) {

			MessageContext mc = result.addError(this, 104, ai.name());
			if (mc != null) {
			    mc.addDetail(this, 1, ai.end1().inClass().name(), ai.end1().name(),
				    ai.end2().inClass().name(), ai.end2().name());
			}

			isValidSetup = false;
		    }

		} else if (sourceHasExistentialDependencyInfo || targetHasExistentialDependencyInfo) {

//		    /*
//		     * If the association is bi-directional, existential dependency type
//		     * information, if present on one role, must also be present on the other role.
//		     */
//
//		    if (!(sourceHasExistentialDependencyInfo && targetHasExistentialDependencyInfo)) {
//
//			MessageContext mc = result.addError(this, 105, ai.name());
//			if (mc != null) {
//			    mc.addDetail(this, 1, ai.end1().inClass().name(), ai.end1().name(),
//				    ai.end2().inClass().name(), ai.end2().name());
//			}
//
//			isValidSetup = false;
//		    }
		}
	    }

	    if (!isValidSetup) {

		MessageContext mc = result.addError(this, 106, ai.name());
		if (mc != null) {
		    mc.addDetail(this, 1, ai.end1().inClass().name(), ai.end1().name(), ai.end2().inClass().name(),
			    ai.end2().name());
		}

	    } else {

		navigableRoles.addAll(
			identifyNavigableRole(ai.end1(), appSchema, appSchemaVersion, featureObjectAndMixinTypes,
				sourceFeatureTypesForOwnershipRole, targetFeatureTypesForOwnershipRole));
		navigableRoles.addAll(
			identifyNavigableRole(ai.end2(), appSchema, appSchemaVersion, featureObjectAndMixinTypes,
				sourceFeatureTypesForOwnershipRole, targetFeatureTypesForOwnershipRole));
	    }
	}

	hasReflexiveRelationshipsOrCyclesInOwnershipRelationGraph(navigableRoles, sourceFeatureTypesForOwnershipRole,
		targetFeatureTypesForOwnershipRole);

	return navigableRoles;
    }

    private boolean hasReflexiveRelationshipsOrCyclesInOwnershipRelationGraph(
	    SortedSet<CoretableNavigableRole> navigableRoles, SortedSet<ClassInfo> sourceFeatureTypesForOwnershipRole,
	    SortedSet<ClassInfo> targetFeatureTypesForOwnershipRole) {

	boolean hasCircularDependencies = false;

	result.addInfo(this, 1001);

	DirectedMultigraph<String, PropertySetEdge> ownershipRolesGraph = new DirectedMultigraph<String, PropertySetEdge>(
		PropertySetEdge.class);

	SortedSet<ClassInfo> wholeAndPartFeatureTypes = new TreeSet<>();
	wholeAndPartFeatureTypes.addAll(sourceFeatureTypesForOwnershipRole);
	wholeAndPartFeatureTypes.addAll(targetFeatureTypesForOwnershipRole);

	/*
	 * Create a directed graph from navigable roles with existential dependency info
	 * (here: a directed edge points to the 'part' feature type. Check for any loops
	 * or cycles in it. If one is detected, log an error and prevent writing the
	 * navigable roles config. Otherwise, determine and log the maximum depth of the
	 * trees that each node in the graph spans.
	 */

	// establish graph vertices
	for (ClassInfo wholeAndPartFeatureType : wholeAndPartFeatureTypes) {
	    ownershipRolesGraph.addVertex(wholeAndPartFeatureType.pkg().name() + "::" + wholeAndPartFeatureType.name());
	}

	// establish edges

	/*
	 * key: name of data type with reflexive relationship(s); value: properties that
	 * cause the reflexive relationship(s)
	 */
	Map<String, Set<String>> refTypeInfo = new TreeMap<String, Set<String>>();

	for (ClassInfo sourceFeatureType : sourceFeatureTypesForOwnershipRole) {

	    /*
	     * Look for any edges that go from the sourceFeatureType (which is a whole) to a
	     * target feature type (which is the part).
	     */

	    String sourceFeatureTypeKey = sourceFeatureType.pkg().name() + "::" + sourceFeatureType.name();

	    /*
	     * key: {target feature type package name}::{target feature type name}, value:
	     * names of properties of sourceFeatureType that have that target feature type
	     */
	    Map<String, Set<String>> propertiesByTargetEdgeFeatureTypeName = new HashMap<String, Set<String>>();

	    for (CoretableNavigableRole nvConfigEntry : navigableRoles.stream()
		    .filter(cr -> sourceFeatureType == cr.getSourceFeatureType()
			    && cr.getDependentPart() == DependentPart.target)
		    .toList()) {

		ClassInfo targetEdgeFeatureType = nvConfigEntry.getTargetFeatureType();

		String key = targetEdgeFeatureType.pkg().name() + "::" + targetEdgeFeatureType.name();
		Set<String> props;
		if (propertiesByTargetEdgeFeatureTypeName.containsKey(key)) {
		    props = propertiesByTargetEdgeFeatureTypeName.get(key);
		} else {
		    props = new TreeSet<String>();
		    propertiesByTargetEdgeFeatureTypeName.put(key, props);
		}

		String propName = nvConfigEntry.getNavigableRole().name();
		props.add(propName);
	    }

	    /*
	     * Only use cases of dependent_part = source for uni-directional association.
	     * For bi-directional associations, there must be an nvConfigEntry where
	     * dependent_part = target, and that is encoded in the previous loop.
	     */
	    for (CoretableNavigableRole nvConfigEntry : navigableRoles.stream()
		    .filter(cr -> sourceFeatureType == cr.getTargetFeatureType()
			    && cr.getDependentPart() == DependentPart.source
			    && !cr.getNavigableRole().reverseProperty().isNavigable())
		    .toList()) {

		ClassInfo targetEdgeFeatureType = nvConfigEntry.getSourceFeatureType();

		String key = targetEdgeFeatureType.pkg().name() + "::" + targetEdgeFeatureType.name();
		Set<String> props;
		if (propertiesByTargetEdgeFeatureTypeName.containsKey(key)) {
		    props = propertiesByTargetEdgeFeatureTypeName.get(key);
		} else {
		    props = new TreeSet<String>();
		    propertiesByTargetEdgeFeatureTypeName.put(key, props);
		}

		String propName = nvConfigEntry.getNavigableRole().reverseProperty().name() + " (reverse property)";

		props.add(propName);
	    }

	    /*
	     * create directed edges and thereby identify reflexive relationships
	     */
	    for (String targetKey : propertiesByTargetEdgeFeatureTypeName.keySet()) {

		Set<String> props = propertiesByTargetEdgeFeatureTypeName.get(targetKey);

		if (sourceFeatureTypeKey.equals(targetKey)) {
		    /*
		     * loops are not supported in cycle detection of JGraphT, thus log infos to
		     * create an error later on
		     */
		    refTypeInfo.put(sourceFeatureTypeKey, props);

		} else {

		    ownershipRolesGraph.addEdge(sourceFeatureTypeKey, targetKey,
			    new PropertySetEdge(sourceFeatureTypeKey, targetKey, props));
		}
	    }
	}

	/*
	 * Log occurrence of reflexive relationships.
	 */
	if (refTypeInfo.isEmpty()) {
	    result.addInfo(this, 1003);
	} else {
	    hasCircularDependencies = true;
	    for (String key : refTypeInfo.keySet()) {
		result.addError(this, 1002, key, StringUtils.join(refTypeInfo.get(key), ","));
	    }
	}

	DirectedSimpleCycles<String, PropertySetEdge> alg = new TiernanSimpleCycles<String, PropertySetEdge>(
		ownershipRolesGraph);

	List<List<String>> cycles = alg.findSimpleCycles();

	if (cycles != null && cycles.size() > 0) {

	    for (List<String> cycle : cycles) {

		hasCircularDependencies = true;
		result.addError(this, 1004);

		for (int i = 0; i < cycle.size(); i++) {

		    String source = cycle.get(i);
		    String target = i == cycle.size() - 1 ? cycle.getFirst() : cycle.get(i + 1);

		    PropertySetEdge edge = ownershipRolesGraph.getEdge(source, target);

		    result.addError(this, 1005, source, target, edge.toString());
		}
	    }
	} else {
	    result.addInfo(this, 1006);
	}

	if (!hasCircularDependencies) {

	    AllDirectedPaths<String, PropertySetEdge> paths = new AllDirectedPaths<>(ownershipRolesGraph);
	    SortedSet<String> vertices = new TreeSet<>(ownershipRolesGraph.vertexSet());

	    int maxDepthAll = -1;
	    String maxDepthPath = "";

	    for (String startVertex : vertices) {

		BreadthFirstIterator<String, PropertySetEdge> bfi = new BreadthFirstIterator<>(ownershipRolesGraph,
			startVertex);

		String v;
		int maxDepthCurrent = 0;
		String maxDepthPathCurrent = "";
		while (bfi.hasNext()) {
		    v = bfi.next();

		    GraphPath<String, PropertySetEdge> longestPath = paths.getAllPaths(startVertex, v, true, null)
			    .stream()
			    .sorted((GraphPath<String, PropertySetEdge> path1,
				    GraphPath<String, PropertySetEdge> path2) -> Integer.valueOf(path2.getLength())
					    .compareTo(path1.getLength()))
			    .findFirst().get();

		    int depth = longestPath.getLength();
		    if (maxDepthCurrent < depth) {
			maxDepthCurrent = depth;
			maxDepthPathCurrent = longestPath.toString();
		    }
		}

		if (maxDepthCurrent > maxDepthAll) {
		    maxDepthAll = maxDepthCurrent;
		    maxDepthPath = maxDepthPathCurrent;
		}
	    }

	    result.addInfo(this, 101, "" + maxDepthAll, maxDepthPath);
	}

	return hasCircularDependencies;
    }

    /**
     * Checks if the given association role is navigable. If so, a navigable roles
     * config entry is created.
     * 
     * @param pi                                 the association role to process
     * @param appSchema                          application schema identifier
     * @param appSchemaVersion                   application schema version
     *                                           identifier
     * @param featureObjectAndMixinTypes         feature, object, and mixin types
     *                                           from the schemas selected for
     *                                           processing that are encoded
     * @param sourceFeatureTypesForOwnershipRole Set to add all source feature types
     *                                           for ownership relations
     * @param targetFeatureTypesForOwnershipRole Set to add all target feature types
     *                                           for ownership relations
     * @return The navigable roles that were identified
     */
    private SortedSet<CoretableNavigableRole> identifyNavigableRole(PropertyInfo pi, String appSchema,
	    String appSchemaVersion, SortedSet<ClassInfo> featureObjectAndMixinTypes,
	    SortedSet<ClassInfo> sourceFeatureTypesForOwnershipRole,
	    SortedSet<ClassInfo> targetFeatureTypesForOwnershipRole) {

	SortedSet<CoretableNavigableRole> res = new TreeSet<>();

	if (!pi.isAttribute() && pi.isNavigable()) {

	    DependentPart dependentPart = determineDependentPart(pi);

	    RelDirection relDirection;
	    if (!pi.reverseProperty().isNavigable() || pi == pi.association().end2()) {
		relDirection = RelDirection.forward;
	    } else {
		relDirection = RelDirection.inverse;
	    }

	    for (ClassInfo sourceCi : relevantClassesInHierarchy(pi.inClass(), featureObjectAndMixinTypes)) {

		if (dependentPart == DependentPart.target) {
		    sourceFeatureTypesForOwnershipRole.add(sourceCi);
		} else if (dependentPart == DependentPart.source) {
		    targetFeatureTypesForOwnershipRole.add(sourceCi);
		}

		for (ClassInfo targetCi : relevantClassesInHierarchy(pi.typeClass(), featureObjectAndMixinTypes)) {

		    if (dependentPart == DependentPart.target) {
			targetFeatureTypesForOwnershipRole.add(targetCi);
		    } else if (dependentPart == DependentPart.source) {
			sourceFeatureTypesForOwnershipRole.add(targetCi);
		    }

		    CoretableNavigableRole rule = new CoretableNavigableRole();
		    rule.setSourceFeatureType(sourceCi);
		    rule.setNavigableRole(pi);
		    rule.setTargetFeatureType(targetCi);
		    rule.setAppSchema(appSchema);
		    rule.setVersion(appSchemaVersion);
		    rule.setRelDirection(relDirection);
		    rule.setDependentPart(dependentPart);
		    res.add(rule);
		}
	    }
	}

	return res;
    }

    private DependentPart determineDependentPart(PropertyInfo pi) {

	String tv = pi.taggedValue(CoretableConstants.TV_EX_DEP_TYPE);

	if ("owner".equalsIgnoreCase(tv)) {

	    return DependentPart.source;

	} else if ("part".equalsIgnoreCase(tv)) {

	    return DependentPart.target;

	} else {

	    /*
	     * For cases of bi-directional associations, check the other association end.
	     * That covers cases where the tagged value is only set on one of the ends. Then
	     * automatically determine the dependent_part value for pi based upon the
	     * existential dependency type setting of the reverse property, i.e. the other
	     * association end.
	     */
	    PropertyInfo revPi = pi.reverseProperty();

	    if (revPi.isNavigable() && hasExistentialDependencyInfo(revPi)) {

		String revTv = revPi.taggedValue(CoretableConstants.TV_EX_DEP_TYPE);

		if ("owner".equalsIgnoreCase(revTv)) {

		    return DependentPart.target;

		} else if ("part".equalsIgnoreCase(revTv)) {

		    return DependentPart.source;

		} else {

		    return DependentPart.none;
		}

	    } else {
		return DependentPart.none;
	    }
	}
    }

    private boolean checkNavigabilityOfRoleWithExistentialDependencyInfo(PropertyInfo pi) {

	if (!pi.isNavigable()) {
	    MessageContext mc = result.addError(this, 103, pi.name(), pi.inClass().name());
	    if (mc != null) {
		mc.addDetail(this, 0, pi.fullName());
	    }
	    return false;
	} else {
	    return true;
	}
    }

    private List<ClassInfo> relevantClassesInHierarchy(ClassInfo ci, SortedSet<ClassInfo> featureObjectAndMixinTypes) {

	List<ClassInfo> relevantCis = new ArrayList<>();

	if (isRelevant(ci, featureObjectAndMixinTypes)) {
	    relevantCis.add(ci);
	}

	for (ClassInfo subCi : ci.subtypesInCompleteHierarchy()) {
	    if (isRelevant(subCi, featureObjectAndMixinTypes)) {
		relevantCis.add(subCi);
	    }
	}

	return relevantCis;
    }

    private boolean isRelevant(ClassInfo ci, SortedSet<ClassInfo> featureObjectAndMixinTypes) {
	return !ci.isAbstract() && featureObjectAndMixinTypes.contains(ci) && ci.category() != Options.MIXIN;
    }

    private boolean hasExistentialDependencyInfo(PropertyInfo pi) {

	String tv = pi.taggedValue(CoretableConstants.TV_EX_DEP_TYPE);

	if (StringUtils.isNotBlank(tv) && !Strings.CI.equalsAny(tv, "owner", "part")) {
	    MessageContext mc = result.addError(this, 100, pi.name(), pi.inClass().name(), tv);
	    if (mc != null) {
		mc.addDetail(this, 0, pi.fullName());
	    }
	    return false;
	}

	return StringUtils.isNotBlank(tv);
    }

    @Override
    public String message(int mnr) {

	return switch (mnr) {
	case 0 -> "Context: association role $1$";
	case 1 ->
	    "Context: association between class '$1$' (with property '$2$') and class '$3$' (with property '$4$')";

	case 100 -> "$$Association role '$1$' of class $2$ has tag " + CoretableConstants.TV_EX_DEP_TYPE
		+ " with value '$3$. Only the following values are allowed: 'owner', 'part'.";
	case 101 -> "Maximum depth: $1$ - (e.g.) via path: $2$";
	case 102 -> "";
	case 103 -> "$$Association role '$1$' of class $2$ has existential dependency type info but is not navigable.";
	case 104 ->
	    "Bi-directional association '$1$' has equal existential dependency type tagging on both association roles, which is not allowed.";
	case 105 ->
	    "Bi-directional association '$1$' has existential dependency type tagging on only one association role, which is not allowed (both roles must be tagged correctly).";
	case 106 ->
	    "Association '$1$' has existential dependency type tagging on (at least one of) its roles, but the setup is invalid (see previous messages). The association will be ignored.";

	case 1001 ->
	    "---------- Checking for reflexive relationships and cyles in ownership relationship graph ----------";
	case 1002 -> "--- Reflexive relationship detected for source feature type '$1$' (via role(s): $2$).";
	case 1003 -> "--- No reflexive relationships detected.";
	case 1004 -> "--- Found cycle:";
	case 1005 -> "   Class '$1$' -> class '$2$' (via roles: $3$)";
	case 1006 -> "--- No cycles found.";

	default -> "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	};
    }
}
