package de.interactive_instruments.ShapeChange;

import org.junit.Test;

public class VariousFunctionalityTest extends WindowsBasicTest {
	
	@Test
	public void testDescriptorFunctionality() {
		/*
		 * Test the descriptor functionality
		 */
		multiTest("src/test/resources/config/testEA_descriptors_fc_en.xml",
				new String[] { "xml", "html" },
				"testResults/html/descriptors/INPUT",
				"src/test/resources/reference/html/descriptors");
		
		multiTest("src/test/resources/config/testEA_descriptors_fc_de.xml",
				new String[] { "xml", "html" },
				"testResults/html/descriptors/INPUT",
				"src/test/resources/reference/html/descriptors");
		
		multiTest(
				"src/test/resources/config/testEA_descriptors_inspire.xml",
				new String[] { "xml", "html" },
				"testResults/html/descriptors/INPUT",
				"src/test/resources/reference/html/descriptors");
		
		multiTest("src/test/resources/config/testEA_descriptors_aaa.xml",
				new String[] { "xml", "html" },
				"testResults/html/descriptors/INPUT",
				"src/test/resources/reference/html/descriptors");
		
		multiTest("src/test/resources/config/testEA_descriptors_bbr.xml",
				new String[] { "xml", "html" },
				"testResults/html/descriptors/INPUT",
				"src/test/resources/reference/html/descriptors");
	}

}
