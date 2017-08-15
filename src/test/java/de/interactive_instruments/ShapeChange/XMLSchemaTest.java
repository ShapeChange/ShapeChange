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

import java.util.HashMap;

import org.junit.Test;

public class XMLSchemaTest extends WindowsBasicTest {

	// TODO add INSPIRE, OKSTRA, no GML, more Schematron tests

	@Test
	public void xmlSchemaTest1() {
		/*
		 * First the same test model as the XMI 1.0 test model
		 */
		xsdTest("src/test/resources/config/testEA.xml", new String[] { "test" },
				null, "testResults/ea/INPUT",
				"src/test/resources/reference/xsd");
	}

	@Test
	public void xmlSchemaTest2() {
		/*
		 * Now with some replacement values
		 */
		HashMap<String, String> replace = new HashMap<String, String>();
		replace.put("$eap$", "src/test/resources/test.eap");
		replace.put("$log$", "testResults/ea/log.xml");
		replace.put("$out$", "testResults/ea");
		xsdTest("src/test/resources/config/testEA_x.xml",
				new String[] { "test" }, null, replace, "testResults/ea/INPUT",
				"src/test/resources/reference/xsd");
	}

	@Test
	public void xmlSchemaTest3() {
		/*
		 * Test property options: ordered, uniqueness and inline/byReference
		 */
		multiTest("src/test/resources/config/testEA_prop.xml",
				new String[] { "xsd", "xml", "html" },
				"testResults/ea/prop/INPUT",
				"src/test/resources/reference/prop");
	}

	@Test
	public void xmlSchemaTest4() {
		/*
		 * Test multiple tagged values and stereotypes
		 */
		HashMap<String, String> replace2 = new HashMap<String, String>();
		replace2.put("$tvimpl$", "");
		replace2.put("$logout$",
				"testResults/multipleTaggedValuesAndStereotypes/tv_map_impl/log.xml");
		replace2.put("$xsdout$",
				"testResults/multipleTaggedValuesAndStereotypes/tv_map_impl");
		xsdTest("src/test/resources/config/testEA_multipleTaggedValuesAndStereotypes.xml",
				new String[] { "multTvAndSt" }, null, replace2,
				"testResults/multipleTaggedValuesAndStereotypes/tv_map_impl/INPUT",
				"src/test/resources/reference/xsd/multipleTaggedValuesAndStereotypes/INPUT");
	}

	@Test
	public void xmlSchemaTest5() {
		HashMap<String, String> replace21;
		/*
		 * Test multiple tagged values and stereotypes - with array
		 * implementation for tagged values.
		 */
		replace21 = new HashMap<String, String>();
		replace21.put("$tvimpl$", "array");
		replace21.put("$logout$",
				"testResults/multipleTaggedValuesAndStereotypes/tv_array_impl/log.xml");
		replace21.put("$xsdout$",
				"testResults/multipleTaggedValuesAndStereotypes/tv_array_impl");
		xsdTest("src/test/resources/config/testEA_multipleTaggedValuesAndStereotypes.xml",
				new String[] { "multTvAndSt" }, null, replace21,
				"testResults/multipleTaggedValuesAndStereotypes/tv_array_impl/INPUT",
				"src/test/resources/reference/xsd/multipleTaggedValuesAndStereotypes/INPUT");
	}

	@Test
	public void test_codelist_constraints_codeAbsenceInModelAllowed() {
		/*
		 * Test rule-xsd-cls-codelist-constraints-codeAbsenceInModelAllowed.
		 */
		multiTest("src/test/resources/config/testEA_codeAbsenceInModel.xml",
				new String[] { "xml" }, "testResults/codeAbsenceInModel/input",
				"src/test/resources/reference/xsd/codeAbsenceInModel/input");
	}

	@Test
	public void xmlSchemaTest7() {
		/*
		 * Test XML Schema creation for Application Schema with multiple
		 * packages that shall be output as individual xsd files.
		 */
		multiTest("src/test/resources/config/testEA_packageIncludes.xml",
				new String[] { "xsd" }, "testResults/xsd/packageIncludes",
				"src/test/resources/reference/xsd/packageIncludes");
	}

	@Test
	public void xmlSchemaTest8() {
		/*
		 * Test XML Schema creation for two Application Schema, with identity
		 * transformation.
		 */
		multiTest("src/test/resources/config/testEA_MultipleAppSchema.xml",
				new String[] { "xsd" }, "testResults/xsd/multiAppSchema",
				"src/test/resources/reference/xsd/multiAppSchema");
	}

	@Test
	public void xmlSchemaTest9() {
		/*
		 * A test model where documentation is retrieved from classifiers that
		 * are suppliers of a dependency (feature, attribute and value concepts)
		 */
		xsdTest("src/test/resources/config/testEA_dep.xml",
				new String[] { "test_dep" }, null, "testResults/ea/INPUT",
				"src/test/resources/reference/xsd");
	}

	@Test
	public void xmlSchemaTest10() {
		/*
		 * A simple CityGML Application Domain Extension (ADE)
		 */
		String[] xsdADE = { "ade" };
		xsdTest("src/test/resources/config/testEA_ADE.xml", xsdADE, null,
				"testResults/ea/INPUT", "src/test/resources/reference/xsd");
	}

	@Test
	public void xmlSchemaTest11() {
		/*
		 * Qualified associations as well as array and list properties. Note
		 * that there are errors reported during the conversion (on purpose)
		 * unlike in most other tests.
		 */
		String[] xsdaaask = { "testaaask" };
		xsdTest("src/test/resources/config/testEA_aaa-sk.xml", xsdaaask, null,
				"testResults/ea/INPUT", "src/test/resources/reference/xsd",
				false);
	}

	@Test
	public void xmlSchemaTest12() {
		/*
		 * An association class and the GML 3.3 code list values
		 */
		String[] xsdgml33 = { "testgml33" };
		xsdTest("src/test/resources/config/testEA_gml33.xml", xsdgml33,
				xsdgml33, "testResults/ea/INPUT",
				"src/test/resources/reference/xsd");
	}

	@Test
	public void xmlSchemaTest13() {
		/*
		 * Test the mixin options
		 */
		String[] xsdmixin = { "testgroupmixin" };
		xsdTest("src/test/resources/config/testEA_groupmixin.xml", xsdmixin,
				null, "testResults/ea/INPUT",
				"src/test/resources/reference/xsd");
	}

	@Test
	public void xmlSchemaTest14() {
		/*
		 * A simple 19115 metadata profile and Schematron tests
		 */
		String[] xsdmd = { "testbasetypes", "testprofile", "testlet" };
		String[] schmd = { "testprofile", "testlet" };
		xsdTest("src/test/resources/config/testEA_md.xml", xsdmd, schmd,
				"testResults/ea/md/INPUT", "src/test/resources/reference/xsd");
	}

	@Test
	public void xmlSchemaTest15() {
		/*
		 * The SWE Common 2.0 encoding
		 */
		String[] xsdswe = { "advanced_encodings", "basic_types",
				"block_components", "choice_components", "record_components",
				"simple_components", "simple_encodings", "swe" };
		xsdTest("src/test/resources/config/testEA_swe.xml", xsdswe, null,
				"testResults/ea/swe/INPUT",
				"src/test/resources/reference/xsd/swe");
	}

}
