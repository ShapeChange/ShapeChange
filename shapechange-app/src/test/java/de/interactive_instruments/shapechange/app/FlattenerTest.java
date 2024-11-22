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
 * (c) 2002-2017 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.app;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("SCXML")
public class FlattenerTest extends BasicTestSCXML {

    @Test
    public void test_flatten() {
	multiTest("src/integrationtests/flattener/basic/testEA_Flattening.xml", new String[] { "xsd" },
		"testResults/flattening/basic/results/xsd", "src/integrationtests/flattener/basic/reference/results");
    }

    @Test
    public void test_flatten_homogeneousGeometries() {
	multiTest("src/integrationtests/flattener/homogeneousGeometries_core/testEA_Flattening_homogeneousGeometries.xml",
		new String[] { "xsd" }, "testResults/flattening/homogeneousGeometry_core/",
		"src/integrationtests/flattener/homogeneousGeometries_core/reference");
    }

    @Test
    public void test_flatten_homogeneousGeometries_associations() {
	multiTest(
		"src/integrationtests/flattener/homogeneousGeometries_associations/testEA_Flattening_homogeneousGeometries_associations.xml",
		new String[] { "xsd" }, "testResults/flattening/homogeneousGeometry_associations/",
		"src/integrationtests/flattener/homogeneousGeometries_associations/reference");
    }

    @Test
    public void test_flatten_inheritance() {
	multiTest("src/integrationtests/flattener/inheritance/testEA_Flattening_inheritance.xml", new String[] { "xsd" },
		"testResults/flattening/inheritance/", "src/integrationtests/flattener/inheritance/reference");
    }
    
    @Test
    public void test_flatten_inheritance2() {
	multiTest("src/integrationtests/flattener/inheritance2/testEA_Flattening_inheritance2.xml", new String[] { "xsd", "xml" },
		"testResults/flattening/inheritance2/results", "src/integrationtests/flattener/inheritance2/reference/results");
    }

    @Test
    public void test_flatten_removeInheritance() {
	multiTest("src/integrationtests/flattener/removeInheritance/testEA_Flattening_removeInheritance.xml",
		new String[] { "xsd" }, "testResults/flattening/removeInheritance/xsd",
		"src/integrationtests/flattener/removeInheritance/reference/xsd");
    }

    @Test
    public void test_flatten_cycles() {
	multiTest("src/integrationtests/flattener/cycles/testEA_Flattening_cycles.xml", new String[] { "xsd" },
		"testResults/flattening/cycles", "src/integrationtests/flattener/cycles/reference");
    }

    @Test
    public void test_flatten_removeFeatureToFeatureTypeRelationships() {
	multiTest(
		"src/integrationtests/flattener/removeFeatureToFeatureTypeRelationships/testEA_Flattening_removeFeatureTypeRelationships.xml",
		new String[] { "xsd" }, "testResults/flattening/removeFeatureTypeRelationships",
		"src/integrationtests/flattener/removeFeatureToFeatureTypeRelationships/reference");
    }

    @Test
    public void test_flatten_removeObjectToFeatureTypeRelationships() {
	multiTest(
		"src/integrationtests/flattener/removeObjectToFeatureTypeRelationships/testEA_Flattening_removeObjectToFeatureTypeNavigability.xml",
		new String[] { "xsd" }, "testResults/flattening/removeObjectToFeatureTypeNavigability",
		"src/integrationtests/flattener/removeObjectToFeatureTypeRelationships/reference");
    }

    @Test
    public void test_flatten_inheritance_addAttributesAtBottom() {
	/*
	 * Test rule-trf-cls-flatten-inheritance-add-attributes-at-bottom
	 */
	multiTest(
		"src/integrationtests/flattener/inheritance_addAttributesAtBottom/testEA_Flattening_inheritanceAddAttributesAtBottom.xml",
		new String[] { "xsd" }, "testResults/flattening/inheritanceAddAttributesAtBottom",
		"src/integrationtests/flattener/inheritance_addAttributesAtBottom/reference");
    }

    @Test
    public void test_flatten_geometryTypeInheritance() {
	multiTest(
		"src/integrationtests/flattener/flattenGeometryTypeInheritance/testEA_Flattener_geometryTypeInheritance.xml",
		new String[] { "xsd" }, "testResults/flattening/flattenGeometryTypeInheritance",
		"src/integrationtests/flattener/flattenGeometryTypeInheritance/reference");
    }

    @Test
    public void test_flatten_removeNameAndCodeComponents_duplicateProperties() {
	executeAndError(
		"src/integrationtests/flattener/duplicateProperties_removeNameAndCodeComponent/test_Flattener_duplicateProps_removeNameAndCodeComponent.xml",
		"It is expected that the log contains an error, informing the user about classes with duplicate properties as result of the transformation.");
    }

    @Test
    public void test_flatten_inheritanceIgnoringArcGISSubtypes() {
	multiTest(
		"src/integrationtests/flattener/inheritanceIgnoringArcGISSubtypes/testEA_Flattener_inheritanceIgnoringArcGISSubtypes.xml",
		new String[] { "xsd" }, "testResults/flattening/inheritanceIgnoringArcGISSubtypes",
		"src/integrationtests/flattener/inheritanceIgnoringArcGISSubtypes/reference");
    }

    @Test
    public void test_flattenTypes_propertyCopyDuplicateBehavior() {
	multiTest(
		"src/integrationtests/flattener/flattenTypes_propertyCopyDuplicateBehavior/test_Flattener_flattenTypes_propertyCopyDuplicateBehavior.xml",
		new String[] { "xsd" }, "testResults/flattening/flattenTypes_propertyCopyDuplicateBehavior",
		"src/integrationtests/flattener/flattenTypes_propertyCopyDuplicateBehavior/reference");
    }
    
    @Test
    public void test_measureTypedProperties() {
	multiTest(
		"src/integrationtests/flattener/measureTypedProperties/testEA_flatten_measureTypedProperties.xml",
		new String[] { "xsd" }, "testResults/flattening/measureTypedProperties",
		"src/integrationtests/flattener/measureTypedProperties/reference");
    }
    
    @Test
    public void test_directPositionTypedProperties() {
    	multiTest(
    			"src/integrationtests/flattener/directPositionTypedProperties/testEA_flatten_directPositionTypedProperties.xml",
    			new String[] { "xsd" }, "testResults/flattening/directPositionTypedProperties",
    			"src/integrationtests/flattener/directPositionTypedProperties/reference");
    }
    
    @Test
    public void test_mediaTypeTypedProperties() {
    	multiTest(
    			"src/integrationtests/flattener/mediaTypeTypedProperties/testEA_flatten_mediaTypeTypedProperties.xml",
    			new String[] { "xsd" }, "testResults/flattening/mediaTypeTypedProperties",
    			"src/integrationtests/flattener/mediaTypeTypedProperties/reference");
    }
    
    @Test
    public void test_explicitTimeInterval() {
	multiTest(
		"src/integrationtests/flattener/explicitTimeInterval/testEA_flatten_explicitTimeInterval.xml",
		new String[] { "xsd" }, "testResults/flattening/explicitTimeInterval",
		"src/integrationtests/flattener/explicitTimeInterval/reference");
    }
    
    @Test
    public void test_reverseInheritance() {
	multiTest(
		"src/integrationtests/flattener/reverseInheritance/test_flattener_reverseInheritance.xml",
		new String[] { "xsd" }, "testResults/flattening/reverseInheritance",
		"src/integrationtests/flattener/reverseInheritance/reference");
    }
    
    @Test
    public void test_nonDefaultGeometryToFeatureType() {
	multiTest(
		"src/integrationtests/flattener/nonDefaultGeometryToFeatureType/test_flattener_nonDefaultGeometryToFeatureType.xml",
		new String[] { "xsd" }, "testResults/flattening/nonDefaultGeometryToFeatureType",
		"src/integrationtests/flattener/nonDefaultGeometryToFeatureType/reference");
    }   
    
    @Test 
    public void test_flatten_inheritance_addAttributesInSequence() {
	/*
	 * Test rule-trf-cls-flatten-inheritance-add-attributes-in-sequence
	 */
	multiTest(
		"src/integrationtests/flattener/inheritance_addAttributesInSequence/testEA_Flattening_inheritanceAddAttributesInSequence.xml",
		new String[] { "xsd", "json" }, "testResults/flattening/inheritanceAddAttributesInSequence",
		"src/integrationtests/flattener/inheritance_addAttributesInSequence/reference");
    }
    
    @Test
    public void test_flatten_inheritance3() {
	multiTest("src/integrationtests/flattener/inheritance3/testEA_Flattening_inheritance3.xml", new String[] { "xsd" },
		"testResults/flattening/inheritance3/results", "src/integrationtests/flattener/inheritance3/reference/results");
    }
}
