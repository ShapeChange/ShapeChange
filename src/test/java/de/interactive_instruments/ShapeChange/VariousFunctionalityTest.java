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

import org.junit.jupiter.api.Test;

public class VariousFunctionalityTest extends WindowsBasicTest {

	@Test
	public void testDescriptorFunctionality_fc_en() {
		/*
		 * Test the descriptor functionality
		 */
		multiTest("src/test/resources/config/testEA_descriptors_fc_en.xml",
				new String[] { "html" },
				"testResults/html/descriptors/fc_en/INPUT",
				"src/test/resources/reference/html/descriptors/fc_en");
	}
	
	@Test
	public void testDescriptorFunctionality_fc_de() {
		/*
		 * Test the descriptor functionality
		 */
		multiTest("src/test/resources/config/testEA_descriptors_fc_de.xml",
				new String[] { "html" },
				"testResults/html/descriptors/fc_de/INPUT",
				"src/test/resources/reference/html/descriptors/fc_de");
	}
	
	@Test
	public void testDescriptorFunctionality_inspire() {
		/*
		 * Test the descriptor functionality
		 */
		multiTest("src/test/resources/config/testEA_descriptors_inspire.xml",
				new String[] { "html" },
				"testResults/html/descriptors/inspire/INPUT",
				"src/test/resources/reference/html/descriptors/inspire");
	}
	
	@Test
	public void testDescriptorFunctionality_aaa() {
		/*
		 * Test the descriptor functionality
		 */
		multiTest("src/test/resources/config/testEA_descriptors_aaa.xml",
				new String[] { "html" },
				"testResults/html/descriptors/aaa/INPUT",
				"src/test/resources/reference/html/descriptors/aaa");
	}
	
	@Test
	public void testDescriptorFunctionality_bbr() {
		/*
		 * Test the descriptor functionality
		 */
		multiTest("src/test/resources/config/testEA_descriptors_bbr.xml",
				new String[] { "html" },
				"testResults/html/descriptors/bbr/INPUT",
				"src/test/resources/reference/html/descriptors/bbr");
	}

	@Test
	public void testNotEncoded() {
		
		multiTest("src/test/resources/config/testEA_notEncoded.xml",
				new String[] { "xsd", "json", "sql" },
				"testResults/notEncoded",
				"src/test/resources/reference/notEncoded");
	}
	
	@Test
	public void testLoadingWithProhibitedStatusSetting() {
		
		multiTest("src/test/resources/config/testEA_loadingWithProhibitedStatusSetting.xml",
				new String[] { "xsd", "html" },
				"testResults/loadingWithProhibitedStatusSetting/results",
				"src/test/resources/reference/loadingWithProhibitedStatusSetting");
	}
}
