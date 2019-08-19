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

public class FeatureCatalogueTest extends WindowsBasicTest {

	// 2018-11-09 JE: NOTE: the following test is commented out on purpose: It
	// is only used for internal testing, since the image size depends
	// on the settings in EA, which are user dependent.

//	 @Test
//	 public void testUMLDiagrams() {
//	 /*
//	 * A simple model to test the creation of a docx feature catalogue that
//	 * includes UML diagrams
//	 */
//	
//	 docxTest("src/test/resources/config/testEA_Docx_FC_with_images.xml",
//	 new String[] { "test_featurecatalog_with_images" },
//	 "testResults/docx_with_images/myInputId",
//	 "src/test/resources/reference/docx");
//	 }

	@Test
	public void testSingleFileHtmlFeatureCatalogue() {
		/*
		 * A simple model to test the creation of a single-file html feature
		 * catalogue
		 */
		htmlTest("src/test/resources/config/testEA_Html.xml",
				new String[] { "test" }, "testResults/html/INPUT",
				"src/test/resources/reference/html");
	}

	@Test
	public void testLocalizationFunctionality() {
		/*
		 * A simple model to test the localization functionality
		 */
		htmlTest("src/test/resources/config/testEA_HtmlLocalization.xml",
				new String[] { "test" }, "testResults/html/localization/INPUT",
				"src/test/resources/reference/html/localization");
	}

	@Test
	public void testDocxFeatureCatalogue() {
		/*
		 * A simple model to test the creation of a docx feature catalogue
		 */
		docxTest("src/test/resources/config/testEA_Docx.xml",
				new String[] { "test" }, "testResults/docx/myInputId",
				"src/test/resources/reference/docx");
	}

	@Test
	public void testInheritedPropertiesAndNoAlphabeticSortingForProperties() {
		/*
		 * Test creation of an HTML feature catalogue with
		 * inheritedProperties=true and noAlphabeticSortingForProperties = true
		 */
		multiTest("src/test/resources/config/testEA_fc_inheritedProperties.xml",
				new String[] { "xml", "html" },
				"testResults/html/inheritedProperties/INPUT",
				"src/test/resources/reference/html/inheritedProperties/INPUT");
	}

	@Test
	public void testDerivationOfApplicationSchemaDifferences() {
		/*
		 * Test derivation of application schema differences (output as single
		 * page HTML feature catalogue).
		 */
		multiTest("src/test/resources/config/testEA_model_diff.xml",
				new String[] { "xml", "html" }, "testResults/html/diff/INPUT",
				"src/test/resources/reference/html/diff/INPUT");
	}

	@Test
	public void testTaggedValues() {

		multiTest(
				"src/test/resources/featureCatalogue/taggedValues/testEA_featureCatalogue_taggedValues.xml",
				new String[] { "xml", "html", "docx" },
				"testResults/featureCatalogue/taggedValues/results",
				"src/test/resources/featureCatalogue/taggedValues/reference/results");
	}

	@Test
	public void testInheritedConstraints() {

		multiTest(
				"src/test/resources/featureCatalogue/inheritedConstraints/testEA_featureCatalogue_inheritedConstraints.xml",
				new String[] { "xml", "html", "docx" },
				"testResults/featureCatalogue/inheritedConstraints/results",
				"src/test/resources/featureCatalogue/inheritedConstraints/reference/results");
	}

	@Test
	public void testLogo() {

		multiTest(
				"src/test/resources/featureCatalogue/logo/testEA_featureCatalogue_logo.xml",
				new String[] { "xml", "html", "docx" },
				"testResults/featureCatalogue/logo/results",
				"src/test/resources/featureCatalogue/logo/reference/results");
	}

	@Test
	public void testDocxStyle_custom1() {

		multiTest(
				"src/test/resources/featureCatalogue/docxStyle_custom1/test_featureCatalogue_docxStyle_custom1.xml",
				new String[] { "docx" },
				"testResults/featureCatalogue/docxStyle_custom1/results",
				"src/test/resources/featureCatalogue/docxStyle_custom1/reference/results");
	}
}
