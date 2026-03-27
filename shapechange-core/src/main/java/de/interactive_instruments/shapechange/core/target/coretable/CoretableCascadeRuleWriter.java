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
import de.interactive_instruments.shapechange.core.target.coretable.CoretableCascadeRule.RelDirection;
import de.interactive_instruments.shapechange.core.transformation.flattening.PropertySetEdge;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class CoretableCascadeRuleWriter implements MessageSource {

    private ShapeChangeResult result;

    private String dbSchemaName;

    private SortedSet<CoretableCascadeRule> cascadeRules = new TreeSet<>();
    private DirectedMultigraph<String, PropertySetEdge> graph = new DirectedMultigraph<String, PropertySetEdge>(
	    PropertySetEdge.class);

    public CoretableCascadeRuleWriter(ShapeChangeResult result, String dbSchemaName) {
	this.result = result;
	this.dbSchemaName = dbSchemaName;
    }

    public void computeCascadeRules(SortedSet<ClassInfo> featureObjectAndMixinTypes, String appSchema,
	    String appSchemaVersion) {

	cascadeRules = new TreeSet<>();

	/*
	 * Identify associations between the given classes, in which a navigable
	 * association role has tag 'existentiallyDependentPart' with boolean value
	 * 'true'. That association role references the existentially dependent part in
	 * a whole-part relationship (where a part may also be shared by multiple
	 * wholes).
	 */

	SortedSet<AssociationInfo> ais = new TreeSet<>();

	for (ClassInfo ci : featureObjectAndMixinTypes) {
	    for (PropertyInfo pi : ci.properties().values()) {
		if (!pi.isAttribute()) {
		    AssociationInfo ai = pi.association();
		    if (featureObjectAndMixinTypes.contains(ai.end1().inClass())
			    && featureObjectAndMixinTypes.contains(ai.end2().inClass())
			    && (isExistentiallyDependentPart(ai.end1()) || isExistentiallyDependentPart(ai.end2()))) {
			ais.add(ai);
		    }
		}
	    }
	}

	/*
	 * For each such association, determine the non-abstract (sub-)classes on both
	 * end(s) (thereby excluding mixins). Add a cascade rule for each combination.
	 */

	SortedSet<ClassInfo> wholeFeatureTypes = new TreeSet<>();
	SortedSet<ClassInfo> partFeatureTypes = new TreeSet<>();

	for (AssociationInfo ai : ais) {

	    PropertyInfo wholeOwnedRole = isExistentiallyDependentPart(ai.end1()) ? ai.end1() : ai.end2();

	    if (!wholeOwnedRole.isNavigable()) {

		MessageContext mc = result.addError(this, 100, wholeOwnedRole.name(), wholeOwnedRole.inClass().name());
		if (mc != null) {
		    mc.addDetail(this, 0, wholeOwnedRole.fullName());
		}

	    } else {

		RelDirection relDirection;
		if (!wholeOwnedRole.reverseProperty().isNavigable() || wholeOwnedRole == ai.end2()) {
		    relDirection = RelDirection.forward;
		} else {
		    relDirection = RelDirection.inverse;
		}

		for (ClassInfo wholeCi : relevantClassesInHierarchy(wholeOwnedRole.inClass(),
			featureObjectAndMixinTypes)) {

		    wholeFeatureTypes.add(wholeCi);

		    for (ClassInfo partCi : relevantClassesInHierarchy(wholeOwnedRole.typeClass(),
			    featureObjectAndMixinTypes)) {

			partFeatureTypes.add(partCi);

			CoretableCascadeRule rule = new CoretableCascadeRule();
			rule.setAppSchema(appSchema);
			rule.setPartFeatureType(partCi);
			rule.setRelDirection(relDirection);
			rule.setVersion(appSchemaVersion);
			rule.setWholeFeatureType(wholeCi);
			rule.setWholeOwnedRole(wholeOwnedRole);
			cascadeRules.add(rule);
		    }
		}
	    }
	}

	boolean hasCircularDependencies = false;

	result.addInfo(this, 1001);

	SortedSet<ClassInfo> wholeAndPartFeatureTypes = new TreeSet<>();
	wholeAndPartFeatureTypes.addAll(wholeFeatureTypes);
	wholeAndPartFeatureTypes.addAll(partFeatureTypes);

	/*
	 * Create a directed graph from the cascade rules. Check for any loops or cycles
	 * in it. If one is detected, log an error and prevent writing cascade rules.
	 * Otherwise, determine and log the maximum depth of the trees that each node in
	 * the graph spans.
	 */

	// establish graph vertices
	for (ClassInfo wholeAndPartFeatureType : wholeAndPartFeatureTypes) {
	    graph.addVertex(wholeAndPartFeatureType.pkg().name() + "::" + wholeAndPartFeatureType.name());
	}

	// establish edges

	/*
	 * key: name of data type with reflexive relationship(s); value: properties that
	 * cause the reflexive relationship(s)
	 */
	Map<String, Set<String>> refTypeInfo = new TreeMap<String, Set<String>>();

	for (ClassInfo wholeFeatureType : wholeFeatureTypes) {

	    String wholeFeatureTypeKey = wholeFeatureType.pkg().name() + "::" + wholeFeatureType.name();

	    /*
	     * key: {part feature type package name}::{part feature type name}, value: names
	     * of properties of wholeFeatureType that have that part feature type
	     */
	    Map<String, Set<String>> propertiesByPartFeatureTypeName = new HashMap<String, Set<String>>();

	    for (CoretableCascadeRule rule : this.cascadeRules.stream()
		    .filter(cr -> cr.getWholeFeatureType() == wholeFeatureType).toList()) {

		ClassInfo partFeatureType = rule.getPartFeatureType();

		String key = partFeatureType.pkg().name() + "::" + partFeatureType.name();
		Set<String> props;
		if (propertiesByPartFeatureTypeName.containsKey(key)) {
		    props = propertiesByPartFeatureTypeName.get(key);
		} else {
		    props = new TreeSet<String>();
		    propertiesByPartFeatureTypeName.put(key, props);
		}
		props.add(rule.getWholeOwnedRole().name());
	    }

	    /*
	     * create directed edges and thereby identify reflexive relationships
	     */
	    for (String targetKey : propertiesByPartFeatureTypeName.keySet()) {

		Set<String> props = propertiesByPartFeatureTypeName.get(targetKey);

		if (wholeFeatureTypeKey.equals(targetKey)) {
		    /*
		     * loops are not supported in cycle detection of JGraphT, thus log infos to
		     * create an error later on
		     */
		    refTypeInfo.put(wholeFeatureTypeKey, props);

		} else {

		    graph.addEdge(wholeFeatureTypeKey, targetKey,
			    new PropertySetEdge(wholeFeatureTypeKey, targetKey, props));
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

	DirectedSimpleCycles<String, PropertySetEdge> alg = new TiernanSimpleCycles<String, PropertySetEdge>(graph);

	List<List<String>> cycles = alg.findSimpleCycles();

	if (cycles != null && cycles.size() > 0) {

	    for (List<String> cycle : cycles) {

		hasCircularDependencies = true;
		result.addError(this, 1004);

		for (int i = 0; i < cycle.size(); i++) {

		    String source = cycle.get(i);
		    String target = i == cycle.size() - 1 ? cycle.getFirst() : cycle.get(i + 1);

		    PropertySetEdge edge = graph.getEdge(source, target);

		    result.addError(this, 1005, source, target, edge.toString());
		}
	    }
	} else {
	    result.addInfo(this, 1006);
	}

	if (hasCircularDependencies) {
	    this.cascadeRules = new TreeSet<>();
	    this.graph = new DirectedMultigraph<String, PropertySetEdge>(PropertySetEdge.class);
	} else {

	    AllDirectedPaths<String, PropertySetEdge> paths = new AllDirectedPaths<>(graph);
	    SortedSet<String> vertices = new TreeSet<>(graph.vertexSet());

	    int maxDepthAll = -1;
	    String maxDepthPath = "";

	    for (String startVertex : vertices) {

		BreadthFirstIterator<String, PropertySetEdge> bfi = new BreadthFirstIterator<>(graph, startVertex);

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

    private void printGraph(DirectedMultigraph<String, PropertySetEdge> graph, String outputDirectory,
	    String outputFilename) {

	String imgFileName = outputFilename + "-cascade-rule-graph.png";
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
	    boolean createCascadeRuleGraphImage) {

	if (this.cascadeRules.isEmpty()) {
	    result.addInfo(this, 102);
	} else {

	    checkOutputDirectory(outputDirectory);

	    if (createCascadeRuleGraphImage) {
		printGraph(graph, outputDirectory, outputFilename);
	    }

	    printCascadeRules(cascadeRules, outputDirectory, outputFilename, targetName);
	}
    }

    private void printCascadeRules(SortedSet<CoretableCascadeRule> cascadeRules2, String outputDirectory,
	    String outputFilename, String targetName) {

	String fileName = outputFilename + "-cascade-rules.sql";
	File file = new File(outputDirectory, fileName);

	try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8))) {

	    for (CoretableCascadeRule rule : this.cascadeRules) {

		writer.println("SELECT " + dbSchemaName + ".add_cascade_rule('" + rule.getWholeFeatureType().name()
			+ "','" + rule.getWholeOwnedRole().name() + "','" + rule.getPartFeatureType().name() + "','"
			+ rule.getAppSchema() + "','" + rule.getVersion() + "','" + rule.getRelDirection().name()
			+ "');");
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
	case 102 -> "??No cascade rules have been identified.";

	case 1001 ->
	    "---------- Checking for reflexive relationships and cyles in whole-part relationship graph ----------";
	case 1002 ->
	    "--- Reflexive relationship detected for whole feature type '$1$' (via whole owned role(s)): $2$).";
	case 1003 -> "--- No reflexive relationships detected.";
	case 1004 -> "--- Found cycle:";
	case 1005 -> "   Class '$1$' -> class '$2$' (via whole owned roles): $3$)";
	case 1006 -> "--- No cycles found.";

	default -> "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	};
    }
}
