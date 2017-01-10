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

import org.junit.Test;

public class FlattenerTest extends WindowsBasicTest {

	@Test
	public void testFlattening1() {
		/*
		 * Flattening transformation
		 */
		multiTest("src/test/resources/config/testEA_Flattening.xml",
				new String[] { "xsd" }, "testResults/flattening/xsd",
				"src/test/resources/reference/flattening/xsd");
	}

	@Test
	public void testFlattening2() {
		/*
		 * Flattening transformation - only homogeneous geometries
		 */
		multiTest(
				"src/test/resources/config/testEA_Flattening_homogeneousGeometries.xml",
				new String[] { "xsd" },
				"testResults/flattening/homogeneousGeometry_core/",
				"src/test/resources/reference/flattening/homogeneousGeometry");
	}

	@Test
	public void testFlattening3() {
		/*
		 * Flattening transformation - only homogeneous geometries - Test1
		 * (handling of associations)
		 */
		multiTest(
				"src/test/resources/config/testEA_Flattening_homogeneousGeometries_test1.xml",
				new String[] { "xsd" },
				"testResults/flattening/homogeneousGeometry_test1/",
				"src/test/resources/reference/flattening/homogeneousGeometry_test1");
	}

	@Test
	public void testFlattening4() {
		/*
		 * Flattening transformation - only inheritance
		 */
		multiTest("src/test/resources/config/testEA_Flattening_inheritance.xml",
				new String[] { "xsd" }, "testResults/flattening/inheritance/",
				"src/test/resources/reference/flattening/inheritance");
	}

	@Test
	public void testFlattening5() {
		/*
		 * Flattening transformation - removing inheritance
		 */
		multiTest(
				"src/test/resources/config/testEA_Flattening_removeInheritance.xml",
				new String[] { "xsd" },
				"testResults/flattening/removeInheritance/xsd",
				"src/test/resources/reference/flattening/removeInheritance/xsd");
	}

	@Test
	public void testFlattening6() {
		/*
		 * Flattening transformation - cycles (and isFlatTarget setting)
		 */
		multiTest("src/test/resources/config/testEA_Flattening_cycles.xml",
				new String[] { "xsd" },
				"testResults/flattening/xsd/cycles_step1",
				"src/test/resources/reference/flattening/xsd/cycles_step1");
	}

	@Test
	public void testFlattening7() {
		/*
		 * Flattening transformation - remove feature-2-feature relationships
		 */
		multiTest(
				"src/test/resources/config/testEA_Flattening_removeFeatureTypeRelationships.xml",
				new String[] { "xsd" },
				"testResults/flattening/removeFeatureTypeRelationships",
				"src/test/resources/reference/flattening/removeFeatureTypeRelationships");
	}

	@Test
	public void testFlattening8() {
		/*
		 * Flattening transformation - remove object-2-feature relationships for
		 * specific object types
		 */
		multiTest(
				"src/test/resources/config/testEA_Flattening_removeObjectToFeatureTypeNavigability.xml",
				new String[] { "xsd" },
				"testResults/flattening/removeObjectToFeatureTypeNavigability",
				"src/test/resources/reference/flattening/removeObjectToFeatureTypeNavigability");
	}

	@Test
	public void testRuleTrfClsFlattenInheritanceAddAttributesAtBottom() {
		/*
		 * Test rule-trf-cls-flatten-inheritance-add-attributes-at-bottom
		 */
		multiTest(
				"src/test/resources/config/testEA_Flattening_inheritanceAddAttributesAtBottom.xml",
				new String[] { "xsd" },
				"testResults/flattening/inheritanceAddAttributesAtBottom",
				"src/test/resources/reference/flattening/inheritanceAddAttributesAtBottom");
	}

}
