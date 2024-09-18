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
public class FeatureCatalogueTest extends BasicTestSCXML {

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
//	 docxTest("src/integrationtests/featureCatalogue/umlDiagrams/testEA_Docx_FC_with_images.xml",
//	 new String[] { "test_featurecatalog_with_images" },
//	 "testResults/featureCatalogue/umlDiagrams/myInputId",
//	 "src/integrationtests/featureCatalogue/umlDiagrams/reference");
//	 
//	 multiTest("src/integrationtests/featureCatalogue/umlDiagrams/testEA_HtmlFrame_with_images.xml",
//		 new String[] { "test_featurecatalog_with_images" },
//		 "testResults/featureCatalogue/htmlframe_with_images/IDENTITY",
//		 "src/integrationtests/featureCatalogue/umlDiagrams/reference");
//	 }

    @Test
    public void testSingleFileHtmlFeatureCatalogue() {
	/*
	 * A simple model to test the creation of a single-file html feature catalogue
	 */
	multiTest("src/integrationtests/featureCatalogue/singleFileHtmlFeatureCatalogue/testEA_Html.xml",
		new String[] { "html" }, "testResults/featureCatalogue/singleFileHtmlFeatureCatalogue/INPUT",
		"src/integrationtests/featureCatalogue/singleFileHtmlFeatureCatalogue/reference");
    }

    @Test
    public void testLocalizationFunctionality() {
	/*
	 * A simple model to test the localization functionality
	 */
	multiTest("src/integrationtests/featureCatalogue/localizationFunctionality/testEA_HtmlLocalization.xml",
		new String[] { "html" }, "testResults/featureCatalogue/localizationFunctionality/INPUT",
		"src/integrationtests/featureCatalogue/localizationFunctionality/reference");
    }

    @Test
    public void testDocxFeatureCatalogue() {
	/*
	 * A simple model to test the creation of a docx feature catalogue
	 */
	multiTest("src/integrationtests/featureCatalogue/docxFeatureCatalogue/testEA_Docx.xml", new String[] { "docx" },
		"testResults/featureCatalogue/docxFeatureCatalogue/myInputId",
		"src/integrationtests/featureCatalogue/docxFeatureCatalogue/reference");
    }

    @Test
    public void testInheritedPropertiesAndNoAlphabeticSortingForProperties() {
	/*
	 * Test creation of an HTML feature catalogue with inheritedProperties=true and
	 * noAlphabeticSortingForProperties = true
	 */
	multiTest(
		"src/integrationtests/featureCatalogue/inheritedPropertiesAndNoAlphabeticSortingForProperties/testEA_fc_inheritedProperties.xml",
		new String[] { "html" },
		"testResults/featureCatalogue/inheritedPropertiesAndNoAlphabeticSortingForProperties/INPUT",
		"src/integrationtests/featureCatalogue/inheritedPropertiesAndNoAlphabeticSortingForProperties/reference/INPUT");
    }

    @Test
    public void testDerivationOfApplicationSchemaDifferences() {
	/*
	 * Test derivation of application schema differences (output as single page HTML
	 * feature catalogue).
	 */
	multiTest("src/integrationtests/featureCatalogue/derivationOfApplicationSchemaDifferences/testEA_model_diff.xml",
		new String[] { "html" }, "testResults/featureCatalogue/derivationOfApplicationSchemaDifferences/INPUT",
		"src/integrationtests/featureCatalogue/derivationOfApplicationSchemaDifferences/reference/INPUT");
    }

    @Test
    public void testTaggedValues() {

	multiTest("src/integrationtests/featureCatalogue/taggedValues/testEA_featureCatalogue_taggedValues.xml",
		new String[] { "html", "docx" }, "testResults/featureCatalogue/taggedValues/results",
		"src/integrationtests/featureCatalogue/taggedValues/reference/results");
    }

    @Test
    public void testInheritedConstraints() {

	multiTest(
		"src/integrationtests/featureCatalogue/inheritedConstraints/testEA_featureCatalogue_inheritedConstraints.xml",
		new String[] { "html", "docx" }, "testResults/featureCatalogue/inheritedConstraints/results",
		"src/integrationtests/featureCatalogue/inheritedConstraints/reference/results");
    }

    @Test
    public void testLogo() {

	multiTest("src/integrationtests/featureCatalogue/logo/testEA_featureCatalogue_logo.xml",
		new String[] { "html", "docx" }, "testResults/featureCatalogue/logo/results",
		"src/integrationtests/featureCatalogue/logo/reference/results");
    }

    @Test
    public void testDocxStyle_custom1() {

	multiTest("src/integrationtests/featureCatalogue/docxStyle_custom1/test_featureCatalogue_docxStyle_custom1.xml",
		new String[] { "docx" }, "testResults/featureCatalogue/docxStyle_custom1/results",
		"src/integrationtests/featureCatalogue/docxStyle_custom1/reference/results");
    }

    @Test
    public void testEATextFormatting() {

	multiTest("src/integrationtests/featureCatalogue/eaTextFormatting/testEA_featureCatalogue_eaTextFormatting.xml",
		new String[] { "docx", "html" }, "testResults/featureCatalogue/eaTextFormatting/results",
		"src/integrationtests/featureCatalogue/eaTextFormatting/reference/results");
    }

    @Test
    public void testDescriptorFunctionality_fc_en() {

	multiTest("src/integrationtests/featureCatalogue/descriptors_fc_en/testEA_descriptors_fc_en.xml",
		new String[] { "html" }, "testResults/featureCatalogue/descriptors_fc_en/fc/INPUT",
		"src/integrationtests/featureCatalogue/descriptors_fc_en/reference");
    }

    @Test
    public void testDescriptorFunctionality_fc_de() {

	multiTest("src/integrationtests/featureCatalogue/descriptors_fc_de/testEA_descriptors_fc_de.xml",
		new String[] { "html" }, "testResults/featureCatalogue/descriptors_fc_de/fc/INPUT",
		"src/integrationtests/featureCatalogue/descriptors_fc_de/reference");
    }

    @Test
    public void testDescriptorFunctionality_inspire() {

	multiTest("src/integrationtests/featureCatalogue/descriptors_inspire/testEA_descriptors_inspire.xml",
		new String[] { "html" }, "testResults/featureCatalogue/descriptors_inspire/fc/INPUT",
		"src/integrationtests/featureCatalogue/descriptors_inspire/reference");
    }

    @Test
    public void testDescriptorFunctionality_aaa() {

	multiTest("src/integrationtests/featureCatalogue/descriptors_aaa/testEA_descriptors_aaa.xml",
		new String[] { "html" }, "testResults/featureCatalogue/descriptors_aaa/fc/INPUT",
		"src/integrationtests/featureCatalogue/descriptors_aaa/reference");
    }

    @Test
    public void testDescriptorFunctionality_bbr() {

	multiTest("src/integrationtests/featureCatalogue/descriptors_bbr/testEA_descriptors_bbr.xml",
		new String[] { "html" }, "testResults/featureCatalogue/descriptors_bbr/fc/INPUT",
		"src/integrationtests/featureCatalogue/descriptors_bbr/reference");
    }
}
