package de.interactive_instruments.ShapeChange;

import org.junit.Test;

public class FeatureCatalogueTest extends WindowsBasicTest {
	
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
				new String[] { "test" },
				"testResults/html/localization/INPUT",
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
		 * inheritedProperties=true and noAlphabeticSortingForProperties =
		 * true
		 */
		multiTest(
				"src/test/resources/config/testEA_fc_inheritedProperties.xml",
				new String[] { "xml", "html" },
				"testResults/html/inheritedProperties/INPUT",
				"src/test/resources/reference/html/inheritedProperties/INPUT");
	}
	
	@Test
	public void testDerivationOfApplicationSchemaDifferences() {
		/*
		 * Test derivation of application schema differences (output as
		 * single page HTML feature catalogue).
		 */
		multiTest("src/test/resources/config/testEA_model_diff.xml",
				new String[] { "xml", "html" },
				"testResults/html/diff/INPUT",
				"src/test/resources/reference/html/diff/INPUT");
	}


}
