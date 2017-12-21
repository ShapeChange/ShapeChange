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

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class ArcGISWorkspaceTest extends WindowsBasicTest {

	@Test
	public void testReflexiveRelationship() {

		multiTest(
				"src/test/resources/arcgis/reflexiveRelationship/testEA_arcgis_reflexiveRelationship.xml",
				new String[] { "eap" },
				"testResults/arcgis/reflexiveRelationship",
				"src/test/resources/arcgis/reflexiveRelationship/reference");
	}

	@Test
	public void testRelationshipClasses() {

		multiTest(
				"src/test/resources/arcgis/relationshipClasses/testEA_arcgis_relationshipClasses.xml",
				new String[] { "eap" },
				"testResults/arcgis/relationshipClasses",
				"src/test/resources/arcgis/relationshipClasses/reference");
	}

	@Test
	public void testLinkedDocuments() {

		multiTest(
				"src/test/resources/arcgis/linkedDocuments/testEA_arcgis_linkedDocuments.xml",
				new String[] { "eap" }, "testResults/arcgis/linkedDocuments",
				"src/test/resources/arcgis/linkedDocuments/reference");
	}
}
