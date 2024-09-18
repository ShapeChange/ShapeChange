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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.shapechange.app;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("SCXML")
public class ProfilerTest extends BasicTestSCXML {

	@Test
	public void testProfilingFunctionality() {
		/*
		 * Test the profiling functionality
		 */
		multiTest("src/integrationtests/profiling/basic/testEA_Profiling.xml",
				new String[] { "xsd", "xml" },
				"testResults/profiling/basic/xsd",
				"src/integrationtests/profiling/basic/reference/xsd");
	}

	@Test
	public void testProfilingWithConstraintValidation() {
		/*
		 * Test the constraint validation functionality
		 */
		multiTest(
				"src/integrationtests/profiling/constraintValidation/testEA_Profiling_withConstraintValidation.xml",
				new String[] { "xml" },
				"testResults/profiling/constraintValidation/results",
				"src/integrationtests/profiling/constraintValidation/reference/results");
	}

	@Test
	public void testProfilingWithExplicitProfileSettingsBehavior() {
		/*
		 * Test the profiling functionality - with explicit profile settings
		 * behavior
		 */
		multiTest(
				"src/integrationtests/profiling/explicitProfileSettings/testEA_Profiling_explicitProfileSettings.xml",
				new String[] { "xsd", "xml" },
				"testResults/profiling/explicitProfileSettings/xsd",
				"src/integrationtests/profiling/explicitProfileSettings/reference/xsd");
	}

	@Test
	public void testProfilingWithMetadata() {
		/*
		 * Test the profiling functionality - with profile metadata
		 */
		multiTest(
				"src/integrationtests/profiling/withProfileMetadata/testEA_Profiling_withMetadata.xml",
				new String[] { "xsd", "xml" },
				"testResults/profiling/withProfileMetadata/results",
				"src/integrationtests/profiling/withProfileMetadata/reference/results");
	}
	
	@Test
	public void testProfilingProfileParameters() {
		
		multiTest(
				"src/integrationtests/profiling/profileParameters/test_profiling_profileParameters.xml",
				new String[] { "xml" },
				"testResults/profiling/profileParameters/results",
				"src/integrationtests/profiling/profileParameters/reference/results");
	}

}
