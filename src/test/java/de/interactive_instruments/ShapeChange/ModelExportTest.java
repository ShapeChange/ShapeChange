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

public class ModelExportTest extends WindowsBasicTest {

	@Test
	public void testModelExport_explicitProfileSettings() {

		multiTest(
				"src/test/resources/modelExport/explicitProfileSettings/testEA_export_scxml_explicitProfileSettings.xml",
				new String[] { "xml" },
				"testResults/modelexport/explicitProfileSettings/scxml",
				"src/test/resources/modelExport/explicitProfileSettings/reference/scxml");
	}

	@Test
	public void testModelExport_profileInheritance() {

		multiTest(
				"src/test/resources/modelExport/profileInheritance/testEA_export_scxml_profileInheritance.xml",
				new String[] { "xml" },
				"testResults/modelexport/profileInheritance/scxml",
				"src/test/resources/modelExport/profileInheritance/reference/scxml");
	}
	
	@Test
	public void testModelExport_suppressMeaninglessCodeEnumCharacteristics() {

		multiTest(
				"src/test/resources/modelExport/suppressMeaninglessCodeEnumCharacteristics/testEA_export_suppressMeaninglessCodeEnumCharacteristics.xml",
				new String[] { "xml" },
				"testResults/modelexport/suppressMeaninglessCodeEnumCharacteristics/scxml",
				"src/test/resources/modelExport/suppressMeaninglessCodeEnumCharacteristics/reference/scxml");
	}
	
	@Test
	public void testModelExport_addStereotypes() {

		multiTest(
				"src/test/resources/modelExport/addStereotypes/test_addStereotypes_all.xml",
				new String[] { "xml" },
				"testResults/modelExport/addStereotypes/results/all",
				"src/test/resources/modelExport/addStereotypes/reference/results/all");
		
		multiTest(
			"src/test/resources/modelExport/addStereotypes/test_addStereotypes_some.xml",
			new String[] { "xml" },
			"testResults/modelExport/addStereotypes/results/some",
			"src/test/resources/modelExport/addStereotypes/reference/results/some");
	}
	
	@Test
	public void testModelExport_suppressIsNavigable() {

		multiTest(
				"src/test/resources/modelExport/suppressIsNavigable/test_suppressIsNavigable.xml",
				new String[] { "xml" },
				"testResults/modelexport/suppressIsNavigable/results",
				"src/test/resources/modelExport/suppressIsNavigable/reference/results");
	}
}
