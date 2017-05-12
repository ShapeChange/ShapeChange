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

	@Test
	public void testProfilingWithMetadata() {
		/*
		 * Test the profiling functionality - with profile metadata
		 */
		multiTest(
				"src/test/resources/config/testEA_Profiling_withMetadata.xml",
				new String[] { "xsd", "xml" },
				"testResults/profiling_withProfileMetadata/results",
				"src/test/resources/reference/profiling_withProfileMetadata/results");
	}

}
