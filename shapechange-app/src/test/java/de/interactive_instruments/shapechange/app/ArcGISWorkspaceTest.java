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

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
@Tag("EARequired")
public class ArcGISWorkspaceTest extends WindowsBasicTest {

	@Test
	public void testBasicFunctionality() {

		multiTest("src/integrationtests/arcgis/base/testEA_arcgis_base.xml",
				new String[] { "qea" }, "testResults/arcgis/base",
				"src/integrationtests/arcgis/base/reference");
	}

	@Test
	public void testReflexiveRelationship() {

		multiTest(
				"src/integrationtests/arcgis/reflexiveRelationship/testEA_arcgis_reflexiveRelationship.xml",
				new String[] { "qea" },
				"testResults/arcgis/reflexiveRelationship",
				"src/integrationtests/arcgis/reflexiveRelationship/reference");
	}

	@Test
	public void testRelationshipClasses() {

		multiTest(
				"src/integrationtests/arcgis/relationshipClasses/testEA_arcgis_relationshipClasses.xml",
				new String[] { "qea" },
				"testResults/arcgis/relationshipClasses",
				"src/integrationtests/arcgis/relationshipClasses/reference");
	}

	@Test
	public void testLinkedDocuments() {

		multiTest(
				"src/integrationtests/arcgis/linkedDocuments/testEA_arcgis_linkedDocuments.xml",
				new String[] { "qea" }, "testResults/arcgis/linkedDocuments",
				"src/integrationtests/arcgis/linkedDocuments/reference");
	}

	@Test
	public void testRepresentTaggedValues() {

		multiTest(
				"src/integrationtests/arcgis/representTaggedValues/testEA_arcgis_representTaggedValues.xml",
				new String[] { "qea" },
				"testResults/arcgis/representTaggedValues",
				"src/integrationtests/arcgis/representTaggedValues/reference");
	}

	@Test
	public void testSubtypesFromFeatureTypes() {

		multiTest(
				"src/integrationtests/arcgis/subtypesFromFeatureTypes/testEA_arcgis_subtypesFromFeatureTypes.xml",
				new String[] { "qea" },
				"testResults/arcgis/subtypesFromFeatureTypes",
				"src/integrationtests/arcgis/subtypesFromFeatureTypes/reference");
	}

	@Test
	public void testSubtypesFromFeatureTypes_lengthCorrection() {

		multiTest(
				"src/integrationtests/arcgis/subtypesFromFeatureTypes_lengthCorrection/testEA_arcgis_subtypesFromFeatureTypes_lengthCorrection.xml",
				new String[] { "qea" },
				"testResults/arcgis/subtypesFromFeatureTypes_lengthCorrection",
				"src/integrationtests/arcgis/subtypesFromFeatureTypes_lengthCorrection/reference");
	}

	@Test
	public void testSubtypesFromSubtypeSet() {

		multiTest(
				"src/integrationtests/arcgis/subtypesFromSubtypeSet/testEA_arcgis_subtypesFromSubtypeSet.xml",
				new String[] { "qea" },
				"testResults/arcgis/subtypesFromSubtypeSet",
				"src/integrationtests/arcgis/subtypesFromSubtypeSet/reference");
	}
}