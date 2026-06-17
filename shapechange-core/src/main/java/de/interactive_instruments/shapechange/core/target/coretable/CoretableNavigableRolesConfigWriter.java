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

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.cycle.DirectedSimpleCycles;
import org.jgrapht.alg.cycle.TiernanSimpleCycles;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.traverse.BreadthFirstIterator;

import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.util.mxCellRenderer;

import de.interactive_instruments.shapechange.core.MessageSource;
import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.ShapeChangeResult.MessageContext;
import de.interactive_instruments.shapechange.core.model.AssociationInfo;
import de.interactive_instruments.shapechange.core.model.ClassInfo;
import de.interactive_instruments.shapechange.core.model.PropertyInfo;
import de.interactive_instruments.shapechange.core.target.coretable.CoretableNavigableRole.RelDirection;
import de.interactive_instruments.shapechange.core.transformation.flattening.PropertySetEdge;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class CoretableNavigableRolesConfigWriter implements MessageSource {

    private ShapeChangeResult result;

    private String dbSchemaName;

    private SortedSet<CoretableNavigableRole> navigableRoles = new TreeSet<>();

    private DirectedMultigraph<String, PropertySetEdge> ownershipRolesGraph = new DirectedMultigraph<String, PropertySetEdge>(
	    PropertySetEdge.class);

    public CoretableNavigableRolesConfigWriter(ShapeChangeResult result, String dbSchemaName) {
	this.result = result;
	this.dbSchemaName = dbSchemaName;
    }

    public void computeNavigableRolesConfig(SortedSet<ClassInfo> featureObjectAndMixinTypes, String appSchema,
	    String appSchemaVersion) {

	navigableRoles = new TreeSet<>();

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

	    checkNavigabilityOfExistentiallyDependentPartRole(ai.end1());
	    checkNavigabilityOfExistentiallyDependentPartRole(ai.end2());

	    identifyNavigableRole(ai.end1(), appSchema, appSchemaVersion, featureObjectAndMixinTypes,
		    sourceFeatureTypesForOwnershipRole, targetFeatureTypesForOwnershipRole);
	    identifyNavigableRole(ai.end2(), appSchema, appSchemaVersion, featureObjectAndMixinTypes,
		    sourceFeatureTypesForOwnershipRole, targetFeatureTypesForOwnershipRole);
	}

	computeReflexiveRelationshipsAndCyclesInOwnershipRelationGraph(sourceFeatureTypesForOwnershipRole,
		targetFeatureTypesForOwnershipRole);
    }

    private void computeReflexiveRelationshipsAndCyclesInOwnershipRelationGraph(
	    SortedSet<ClassInfo> sourceFeatureTypesForOwnershipRole,
	    SortedSet<ClassInfo> targetFeatureTypesForOwnershipRole) {

	boolean hasCircularDependencies = false;

	result.addInfo(this, 1001);

	SortedSet<ClassInfo> wholeAndPartFeatureTypes = new TreeSet<>();
	wholeAndPartFeatureTypes.addAll(sourceFeatureTypesForOwnershipRole);
	wholeAndPartFeatureTypes.addAll(targetFeatureTypesForOwnershipRole);

	/*
	 * Create a directed graph from navigable roles with
	 * isExistentiallyDependentPart = true. Check for any loops or cycles in it. If
	 * one is detected, log an error and prevent writing the navigable roles config.
	 * Otherwise, determine and log the maximum depth of the trees that each node in
	 * the graph spans.
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

	    String sourceFeatureTypeKey = sourceFeatureType.pkg().name() + "::" + sourceFeatureType.name();

	    /*
	     * key: {target feature type package name}::{target feature type name}, value:
	     * names of properties of sourceFeatureType that have that target feature type
	     */
	    Map<String, Set<String>> propertiesByTargetFeatureTypeName = new HashMap<String, Set<String>>();

	    for (CoretableNavigableRole nvConfigEntry : this.navigableRoles.stream()
		    .filter(cr -> cr.isExistentiallyDependentPart() && cr.getSourceFeatureType() == sourceFeatureType)
		    .toList()) {

		ClassInfo targetFeatureType = nvConfigEntry.getTargetFeatureType();

		String key = targetFeatureType.pkg().name() + "::" + targetFeatureType.name();
		Set<String> props;
		if (propertiesByTargetFeatureTypeName.containsKey(key)) {
		    props = propertiesByTargetFeatureTypeName.get(key);
		} else {
		    props = new TreeSet<String>();
		    propertiesByTargetFeatureTypeName.put(key, props);
		}
		props.add(nvConfigEntry.getNavigableRole().name());
	    }

	    /*
	     * create directed edges and thereby identify reflexive relationships
	     */
	    for (String targetKey : propertiesByTargetFeatureTypeName.keySet()) {

		Set<String> props = propertiesByTargetFeatureTypeName.get(targetKey);

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

	if (hasCircularDependencies) {
	    this.navigableRoles = new TreeSet<>();
	    this.ownershipRolesGraph = new DirectedMultigraph<String, PropertySetEdge>(PropertySetEdge.class);
	} else {

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
     *                                           for ownership relations (navigable
     *                                           association roles with tag
     *                                           'existentiallyDependentPart=true')
     * @param targetFeatureTypesForOwnershipRole Set to add all target feature types
     *                                           for ownership relations (navigable
     *                                           association roles with tag
     *                                           'existentiallyDependentPart=true')
     */
    private void identifyNavigableRole(PropertyInfo pi, String appSchema, String appSchemaVersion,
	    SortedSet<ClassInfo> featureObjectAndMixinTypes, SortedSet<ClassInfo> sourceFeatureTypesForOwnershipRole,
	    SortedSet<ClassInfo> targetFeatureTypesForOwnershipRole) {

	if (!pi.isAttribute() && pi.isNavigable()) {

	    boolean isExistentiallyDependentPart = isExistentiallyDependentPart(pi);

	    RelDirection relDirection;
	    if (!pi.reverseProperty().isNavigable() || pi == pi.association().end2()) {
		relDirection = RelDirection.forward;
	    } else {
		relDirection = RelDirection.inverse;
	    }

	    for (ClassInfo sourceCi : relevantClassesInHierarchy(pi.inClass(), featureObjectAndMixinTypes)) {

		if (isExistentiallyDependentPart) {
		    sourceFeatureTypesForOwnershipRole.add(sourceCi);
		}

		for (ClassInfo targetCi : relevantClassesInHierarchy(pi.typeClass(), featureObjectAndMixinTypes)) {

		    if (isExistentiallyDependentPart) {
			targetFeatureTypesForOwnershipRole.add(targetCi);
		    }

		    CoretableNavigableRole rule = new CoretableNavigableRole();
		    rule.setSourceFeatureType(sourceCi);
		    rule.setNavigableRole(pi);
		    rule.setTargetFeatureType(targetCi);
		    rule.setAppSchema(appSchema);
		    rule.setVersion(appSchemaVersion);
		    rule.setRelDirection(relDirection);
		    rule.setExistentiallyDependentPart(isExistentiallyDependentPart(pi));
		    navigableRoles.add(rule);
		}
	    }
	}
    }

    private void checkNavigabilityOfExistentiallyDependentPartRole(PropertyInfo pi) {

	if (isExistentiallyDependentPart(pi) && !pi.isNavigable()) {
	    MessageContext mc = result.addError(this, 100, pi.name(), pi.inClass().name());
	    if (mc != null) {
		mc.addDetail(this, 0, pi.fullName());
	    }
	}
    }

    private void printGraph(DirectedMultigraph<String, PropertySetEdge> graph, String outputDirectory,
	    String outputFilename) {

	String imgFileName = outputFilename + "-ownership-relations-graph.png";
	File imgFile = new File(outputDirectory, imgFileName);

	JGraphXAdapter<String, PropertySetEdge> graphAdapter = new JGraphXAdapter<String, PropertySetEdge>(graph);
	mxIGraphLayout layout = new mxCircleLayout(graphAdapter);
	/*
	 * All layouts are somewhat meh - would need to dig into this, to see if a
	 * better visualization can be achieved.
	 */
//	mxIGraphLayout layout = new mxCompactTreeLayout(graphAdapter);
//	mxIGraphLayout layout = new mxFastOrganicLayout(graphAdapter);
//	mxIGraphLayout layout = new mxParallelEdgeLayout(graphAdapter);
//	mxIGraphLayout layout = new mxPartitionLayout(graphAdapter);
//	mxIGraphLayout layout = new mxStackLayout(graphAdapter);
	layout.execute(graphAdapter.getDefaultParent());

	BufferedImage image = mxCellRenderer.createBufferedImage(graphAdapter, null, 2, Color.WHITE, true, null);
	try {
	    ImageIO.write(image, "PNG", imgFile);
	} catch (IOException e) {
	    String m = e.getMessage();
	    if (m != null) {
		result.addError(m);
	    }

	    e.printStackTrace(System.err);
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

    private boolean isExistentiallyDependentPart(PropertyInfo pi) {
	return Strings.CI.equals("true", StringUtils.stripToEmpty(pi.taggedValue(CoretableConstants.TV_EX_DEP_PART)));
    }

    public void write(String outputDirectory, String outputFilename, String targetName,
	    boolean createOwnershipRolesGraphImage, boolean generateInsertStatements) {

	if (this.navigableRoles.isEmpty()) {
	    result.addInfo(this, 102);
	} else {

	    checkOutputDirectory(outputDirectory);

	    if (createOwnershipRolesGraphImage) {
		printGraph(ownershipRolesGraph, outputDirectory, outputFilename);
	    }

	    printNavigableRolesConfiguration(navigableRoles, outputDirectory, outputFilename, targetName,
		    generateInsertStatements);
	}
    }

    private void printNavigableRolesConfiguration(SortedSet<CoretableNavigableRole> navigableRoles2,
	    String outputDirectory, String outputFilename, String targetName, boolean generateInsertStatements) {

	String fileName = outputFilename + "-navigable-roles.sql";
	File file = new File(outputDirectory, fileName);

	try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8))) {

	    for (CoretableNavigableRole cnr : this.navigableRoles) {

		String s;

		if (generateInsertStatements) {
		    s = "INSERT INTO " + dbSchemaName + ".navigable_roles_config "
			    + "(source_featuretype, navigable_role, target_featuretype, appschema, version, rel_direction, is_existentially_dependent_part) VALUES ('";
		} else {
		    s = "SELECT " + dbSchemaName + ".add_navigable_role('";
		}

		s += cnr.getSourceFeatureType().name() + "','" + cnr.getNavigableRole().name() + "','"
			+ cnr.getTargetFeatureType().name() + "','" + cnr.getAppSchema() + "','" + cnr.getVersion()
			+ "','" + cnr.getRelDirection().name() + "',"
			+ (cnr.isExistentiallyDependentPart() ? "TRUE" : "FALSE");

		if (generateInsertStatements) {

		    s += ") ON CONFLICT (source_featuretype, navigable_role, target_featuretype, appschema, version, rel_direction) "
			    + "DO NOTHING;";

		} else {
		    s += ");";
		}

		writer.println(s);
	    }

	    writer.close();

	    result.addResult(targetName, outputDirectory, fileName, null);

	} catch (IOException e) {

	    String m = e.getMessage();
	    if (m != null) {
		result.addError(m);
	    }

	    e.printStackTrace(System.err);
	}
    }

    private void checkOutputDirectory(String outputDirectory) {
	File outputDirectoryFile = new File(outputDirectory);
	if (!outputDirectoryFile.exists()) {
	    try {
		FileUtils.forceMkdir(outputDirectoryFile);
	    } catch (Exception e) {
		result.addError(this, 100, e.getMessage());
		e.printStackTrace();
	    }
	}
    }

    @Override
    public String message(int mnr) {

	return switch (mnr) {
	case 0 -> "Context: association role $1$";
	case 100 -> "Association role '$1$' of class $2$ has tagged value " + CoretableConstants.TV_EX_DEP_PART
		+ "=true, but is not navigable.";
	case 101 -> "Maximum depth: $1$ - (e.g.) via path: $2$";
	case 102 -> "??No navigable roles have been identified.";

	case 1001 ->
	    "---------- Checking for reflexive relationships and cyles in ownership relationship graph ----------";
	case 1002 -> "--- Reflexive relationship detected for source feature type '$1$' (via role(s) with tag '"
		+ CoretableConstants.TV_EX_DEP_PART + "' = true): $2$).";
	case 1003 -> "--- No reflexive relationships detected.";
	case 1004 -> "--- Found cycle:";
	case 1005 -> "   Class '$1$' -> class '$2$' (via roles with tag '" + CoretableConstants.TV_EX_DEP_PART
		+ "' = true): $3$)";
	case 1006 -> "--- No cycles found.";

	default -> "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	};
    }
}
