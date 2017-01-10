package de.interactive_instruments.ShapeChange;

import org.junit.Test;

public class ProfilerTest extends WindowsBasicTest {
	
	@Test
	public void testProfilingFunctionality() {
		/*
		 * Test the profiling functionality
		 */
		multiTest("src/test/resources/config/testEA_Profiling.xml",
				new String[] { "xsd", "xml" }, "testResults/profiling/xsd",
				"src/test/resources/reference/profiling/xsd");
	}
	
	@Test
	public void testProfilingWithConstraintValidation() {
		/*
		 * Test the constraint validation functionality
		 */
		multiTest(
				"src/test/resources/config/testEA_Profiling_withConstraintValidation.xml",
				new String[] { "xml" },
				"testResults/profiling/constraintValidation/results",
				"src/test/resources/reference/profiling/constraintValidation/results");
	}

	@Test
	public void testProfilingWithExplicitProfileSettingsBehavior() {
		/*
		 * Test the profiling functionality - with explicit profile settings
		 * behavior
		 */
		multiTest(
				"src/test/resources/config/testEA_Profiling_explicitProfileSettings.xml",
				new String[] { "xsd", "xml" },
				"testResults/profiling_explicitProfileSettings/xsd",
				"src/test/resources/reference/profiling_explicitProfileSettings/xsd");
	}

}
