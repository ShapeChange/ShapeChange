package de.interactive_instruments.ShapeChange;

import org.junit.Test;

public class AttributeCreatorTest extends WindowsBasicTest {
	
	@Test
	public void testAttributeCreation() {
		/*
		 * Attribute creation transformer
		 */
		multiTest("src/test/resources/config/testEA_attributeCreation.xml",
				new String[] { "xsd" },
				"testResults/attribute_creation/xsd",
				"src/test/resources/reference/xsd/attributeCreation");
	}

}
