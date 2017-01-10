package de.interactive_instruments.ShapeChange;

import org.junit.Test;

public class JSONTest extends WindowsBasicTest {
	
	@Test
	public void testJsonWithGeoservicesEncodingRule() {
		/*
		 * JSON encoding with geoservices encoding rule
		 */
		String[] typenamesGsr = { "FeatureType1", "FeatureType2" };
		jsonTest("src/test/resources/config/testEA_JsonGsr.xml",
				typenamesGsr, "testResults/ea/json/geoservices/INPUT",
				"src/test/resources/reference/json/geoservices");
	}

	@Test
	public void testJsonWithExtendedGeoservicesEncodingRule() {
		/*
		 * JSON encoding with extended geoservices encoding rule
		 */
		String[] typenamesGsrExtended = { "DataType", "DataType2",
				"FeatureType1", "FeatureType2", "NilUnion", "Union" };
		jsonTest("src/test/resources/config/testEA_JsonGsrExtended.xml",
				typenamesGsrExtended,
				"testResults/ea/json/geoservices_extended/INPUT",
				"src/test/resources/reference/json/geoservices_extended");
	}

}
