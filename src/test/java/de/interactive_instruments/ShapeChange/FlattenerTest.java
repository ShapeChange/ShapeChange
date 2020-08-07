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
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("SCXML")
public class FlattenerTest extends BasicTestSCXML {

    @Test
    public void test_flatten() {
	multiTest("src/test/resources/flattener/basic/testEA_Flattening.xml", new String[] { "xsd" },
		"testResults/flattening/basic/results/xsd", "src/test/resources/flattener/basic/reference/results");
    }

    @Test
    public void test_flatten_homogeneousGeometries() {
	multiTest("src/test/resources/flattener/homogeneousGeometries_core/testEA_Flattening_homogeneousGeometries.xml",
		new String[] { "xsd" }, "testResults/flattening/homogeneousGeometry_core/",
		"src/test/resources/flattener/homogeneousGeometries_core/reference");
    }

    @Test
    public void test_flatten_homogeneousGeometries_associations() {
	multiTest(
		"src/test/resources/flattener/homogeneousGeometries_associations/testEA_Flattening_homogeneousGeometries_associations.xml",
		new String[] { "xsd" }, "testResults/flattening/homogeneousGeometry_associations/",
		"src/test/resources/flattener/homogeneousGeometries_associations/reference");
    }

    @Test
    public void test_flatten_inheritance() {
	multiTest("src/test/resources/flattener/inheritance/testEA_Flattening_inheritance.xml", new String[] { "xsd" },
		"testResults/flattening/inheritance/", "src/test/resources/flattener/inheritance/reference");
    }

    @Test
    public void test_flatten_removeInheritance() {
	multiTest("src/test/resources/flattener/removeInheritance/testEA_Flattening_removeInheritance.xml",
		new String[] { "xsd" }, "testResults/flattening/removeInheritance/xsd",
		"src/test/resources/flattener/removeInheritance/reference/xsd");
    }

    @Test
    public void test_flatten_cycles() {
	multiTest("src/test/resources/flattener/cycles/testEA_Flattening_cycles.xml", new String[] { "xsd" },
		"testResults/flattening/cycles", "src/test/resources/flattener/cycles/reference");
    }

    @Test
    public void test_flatten_removeFeatureToFeatureTypeRelationships() {
	multiTest(
		"src/test/resources/flattener/removeFeatureToFeatureTypeRelationships/testEA_Flattening_removeFeatureTypeRelationships.xml",
		new String[] { "xsd" }, "testResults/flattening/removeFeatureTypeRelationships",
		"src/test/resources/flattener/removeFeatureToFeatureTypeRelationships/reference");
    }

    @Test
    public void test_flatten_removeObjectToFeatureTypeRelationships() {
	multiTest(
		"src/test/resources/flattener/removeObjectToFeatureTypeRelationships/testEA_Flattening_removeObjectToFeatureTypeNavigability.xml",
		new String[] { "xsd" }, "testResults/flattening/removeObjectToFeatureTypeNavigability",
		"src/test/resources/flattener/removeObjectToFeatureTypeRelationships/reference");
    }

    @Test
    public void test_flatten_inheritance_addAttributesAtBottom() {
	/*
	 * Test rule-trf-cls-flatten-inheritance-add-attributes-at-bottom
	 */
	multiTest(
		"src/test/resources/flattener/inheritance_addAttributesAtBottom/testEA_Flattening_inheritanceAddAttributesAtBottom.xml",
		new String[] { "xsd" }, "testResults/flattening/inheritanceAddAttributesAtBottom",
		"src/test/resources/flattener/inheritance_addAttributesAtBottom/reference");
    }

    @Test
    public void test_flatten_geometryTypeInheritance() {
	multiTest(
		"src/test/resources/flattener/flattenGeometryTypeInheritance/testEA_Flattener_geometryTypeInheritance.xml",
		new String[] { "xsd" }, "testResults/flattening/flattenGeometryTypeInheritance",
		"src/test/resources/flattener/flattenGeometryTypeInheritance/reference");
    }

    @Test
    public void test_flatten_removeNameAndCodeComponents_duplicateProperties() {
	executeAndError(
		"src/test/resources/flattener/duplicateProperties_removeNameAndCodeComponent/test_Flattener_duplicateProps_removeNameAndCodeComponent.xml",
		"It is expected that the log contains an error, informing the user about classes with duplicate properties as result of the transformation.");
    }

    @Test
    public void test_flatten_inheritanceIgnoringArcGISSubtypes() {
	multiTest(
		"src/test/resources/flattener/inheritanceIgnoringArcGISSubtypes/testEA_Flattener_inheritanceIgnoringArcGISSubtypes.xml",
		new String[] { "xsd" }, "testResults/flattening/inheritanceIgnoringArcGISSubtypes",
		"src/test/resources/flattener/inheritanceIgnoringArcGISSubtypes/reference");
    }

    @Test
    public void test_flattenTypes_propertyCopyDuplicateBehavior() {
	multiTest(
		"src/test/resources/flattener/flattenTypes_propertyCopyDuplicateBehavior/test_Flattener_flattenTypes_propertyCopyDuplicateBehavior.xml",
		new String[] { "xsd" }, "testResults/flattening/flattenTypes_propertyCopyDuplicateBehavior",
		"src/test/resources/flattener/flattenTypes_propertyCopyDuplicateBehavior/reference");
    }
    
    @Test
    public void test_measureTypedProperties() {
	multiTest(
		"src/test/resources/flattener/measureTypedProperties/testEA_flatten_measureTypedProperties.xml",
		new String[] { "xsd" }, "testResults/flattening/measureTypedProperties",
		"src/test/resources/flattener/measureTypedProperties/reference");
    }
    
}
