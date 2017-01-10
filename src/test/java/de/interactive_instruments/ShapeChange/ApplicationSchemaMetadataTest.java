package de.interactive_instruments.ShapeChange;

import org.junit.Test;

public class ApplicationSchemaMetadataTest extends WindowsBasicTest {
	
	@Test
	public void testApplicationSchemaMetadataDerivation() {
		/*
		 * Test derivation of application schema metadata.
		 */
		multiTest("src/test/resources/config/testEA_schema_metadata.xml",
				new String[] { "xml" }, "testResults/schema_metadata/INPUT",
				"src/test/resources/reference/schema_metadata/INPUT");
	}

}
