package de.interactive_instruments.ShapeChange;

import org.junit.Test;

public class FlattenerTest extends WindowsBasicTest {

	@Test
	public void testFlattening1() {
		/*
		 * Flattening transformation
		 */
		multiTest("src/test/resources/config/testEA_Flattening.xml", new String[] { "xsd" },
				"testResults/flattening/xsd", "src/test/resources/reference/flattening/xsd");
	}

	@Test
	public void testFlattening2() {
		/*
		 * Flattening transformation - only homogeneous geometries
		 */
		multiTest("src/test/resources/config/testEA_Flattening_homogeneousGeometries.xml", new String[] { "xsd" },
				"testResults/flattening/homogeneousGeometry_core/",
				"src/test/resources/reference/flattening/homogeneousGeometry");
	}

	@Test
	public void testFlattening3() {
		/*
		 * Flattening transformation - only homogeneous geometries - Test1
		 * (handling of associations)
		 */
		multiTest("src/test/resources/config/testEA_Flattening_homogeneousGeometries_test1.xml", new String[] { "xsd" },
				"testResults/flattening/homogeneousGeometry_test1/",
				"src/test/resources/reference/flattening/homogeneousGeometry_test1");
	}

	@Test
	public void testFlattening4() {
		/*
		 * Flattening transformation - only inheritance
		 */
		multiTest("src/test/resources/config/testEA_Flattening_inheritance.xml", new String[] { "xsd" },
				"testResults/flattening/inheritance/", "src/test/resources/reference/flattening/inheritance");
	}

	@Test
	public void testFlattening5() {
		/*
		 * Flattening transformation - removing inheritance
		 */
		multiTest("src/test/resources/config/testEA_Flattening_removeInheritance.xml", new String[] { "xsd" },
				"testResults/flattening/removeInheritance/xsd",
				"src/test/resources/reference/flattening/removeInheritance/xsd");
	}

	@Test
	public void testFlattening6() {
		/*
		 * Flattening transformation - cycles (and isFlatTarget setting)
		 */
		multiTest("src/test/resources/config/testEA_Flattening_cycles.xml", new String[] { "xsd" },
				"testResults/flattening/xsd/cycles_step1", "src/test/resources/reference/flattening/xsd/cycles_step1");
	}

	@Test
	public void testFlattening7() {
		/*
		 * Flattening transformation - remove feature-2-feature relationships
		 */
		multiTest("src/test/resources/config/testEA_Flattening_removeFeatureTypeRelationships.xml",
				new String[] { "xsd" }, "testResults/flattening/removeFeatureTypeRelationships",
				"src/test/resources/reference/flattening/removeFeatureTypeRelationships");
	}

	@Test
	public void testFlattening8() {
		/*
		 * Flattening transformation - remove object-2-feature relationships for
		 * specific object types
		 */
		multiTest("src/test/resources/config/testEA_Flattening_removeObjectToFeatureTypeNavigability.xml",
				new String[] { "xsd" }, "testResults/flattening/removeObjectToFeatureTypeNavigability",
				"src/test/resources/reference/flattening/removeObjectToFeatureTypeNavigability");
	}

}
