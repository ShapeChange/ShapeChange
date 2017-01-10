package de.interactive_instruments.ShapeChange;

import org.junit.Test;

public class AssociationClassMapperTest extends WindowsBasicTest {
	
	@Test
	public void testGML33BasedTransformationOfAssociationClasses() {
		/*
		 * Test the GML 3.3 based transformation of association classes.
		 */
		multiTest(
				"src/test/resources/config/testEA_associationClassMapper.xml",
				new String[] { "xsd" },
				"testResults/associationClassTransform/associationClassMapper",
				"src/test/resources/reference/associationClassTransform/associationClassMapper");
	}

}
