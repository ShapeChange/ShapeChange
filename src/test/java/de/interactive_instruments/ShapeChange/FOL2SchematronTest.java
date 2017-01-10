package de.interactive_instruments.ShapeChange;

import org.junit.Test;

public class FOL2SchematronTest extends WindowsBasicTest {
	
	@Test
	public void testFOL2Schematron() {
		/*
		 * Schematron derived from SBVR (with intermediate translation to
		 * FOL)
		 */
		multiTest("src/test/resources/config/testEA_Sbvr.xml",
				new String[] { "xml" },
				"testResults/fol/fromSbvr/sch/step3",
				"src/test/resources/reference/sch/fromSbvr");
	}

}
