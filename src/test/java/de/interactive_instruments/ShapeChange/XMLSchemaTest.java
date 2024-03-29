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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("SCXML")
public class XMLSchemaTest extends BasicTestSCXML {

	// TODO add INSPIRE, OKSTRA, no GML, more Schematron tests

	@Test
	public void test_basic() {
		/*
		 * First the same test model as the XMI 1.0 test model
		 */
		xsdTest("src/test/resources/xsd/basic/testEA.xml", new String[] { "test" },
				null, "testResults/xsd/basic/xsd/INPUT",
				"src/test/resources/xsd/basic/reference");
	}

	@Test
	public void test_replacementValues() {
		/*
		 * Now with some replacement values
		 */
		HashMap<String, String> replace = new HashMap<String, String>();
		replace.put("$log$", "testResults/xsd/replacementValues/log.xml");
		replace.put("$out$", "testResults/xsd/replacementValues/results");
		xsdTest("src/test/resources/xsd/replacementValues/testEA_x.xml",
				new String[] { "test" }, null, replace, "testResults/xsd/replacementValues/results/INPUT",
				"src/test/resources/xsd/replacementValues/reference");
	}

	@Test
	public void test_propertyOptions() {
		/*
		 * Test property options: ordered, uniqueness and inline/byReference
		 */
		multiTest("src/test/resources/xsd/propertyOptions/testEA_prop.xml",
				new String[] { "xsd", "html" },
				"testResults/xsd/propertyOptions/INPUT",
				"src/test/resources/xsd/propertyOptions/reference");
	}

	@Test
	public void test_multipleTaggedValuesAndStereotypes() {
		/*
		 * Test multiple tagged values and stereotypes
		 */
		HashMap<String, String> replace2 = new HashMap<String, String>();
		replace2.put("$tvimpl$", "");
		replace2.put("$logout$",
				"testResults/xsd/multipleTaggedValuesAndStereotypes/tv_map_impl/log.xml");
		replace2.put("$xsdout$",
				"testResults/xsd/multipleTaggedValuesAndStereotypes/tv_map_impl");
		xsdTest("src/test/resources/xsd/multipleTaggedValuesAndStereotypes/testEA_multipleTaggedValuesAndStereotypes.xml",
				new String[] { "multTvAndSt" }, null, replace2,
				"testResults/xsd/multipleTaggedValuesAndStereotypes/tv_map_impl/INPUT",
				"src/test/resources/xsd/multipleTaggedValuesAndStereotypes/reference/INPUT");
	}

	@Test
	public void test_multipleTaggedValuesAndStereotypes2() {
		HashMap<String, String> replace21;
		/*
		 * Test multiple tagged values and stereotypes - with array
		 * implementation for tagged values.
		 */
		replace21 = new HashMap<String, String>();
		replace21.put("$tvimpl$", "array");
		replace21.put("$logout$",
				"testResults/xsd/multipleTaggedValuesAndStereotypes2/tv_array_impl/log.xml");
		replace21.put("$xsdout$",
				"testResults/xsd/multipleTaggedValuesAndStereotypes2/tv_array_impl");
		xsdTest("src/test/resources/xsd/multipleTaggedValuesAndStereotypes2/testEA_multipleTaggedValuesAndStereotypes.xml",
				new String[] { "multTvAndSt" }, null, replace21,
				"testResults/xsd/multipleTaggedValuesAndStereotypes2/tv_array_impl/INPUT",
				"src/test/resources/xsd/multipleTaggedValuesAndStereotypes/reference/INPUT");
	}

	@Test
	public void test_codelist_constraints_codeAbsenceInModelAllowed() {
		/*
		 * Test rule-xsd-cls-codelist-constraints-codeAbsenceInModelAllowed.
		 */
		multiTest("src/test/resources/xsd/codeAbsenceInModel/testEA_codeAbsenceInModel.xml",
				new String[] { "xml" }, "testResults/xsd/codeAbsenceInModel/results/input",
				"src/test/resources/xsd/codeAbsenceInModel/reference/input");
	}

	@Test
	public void test_packageIncludes() {
		/*
		 * Test XML Schema creation for Application Schema with multiple
		 * packages that shall be output as individual xsd files.
		 */
		multiTest("src/test/resources/xsd/packageIncludes/testEA_packageIncludes.xml",
				new String[] { "xsd" }, "testResults/xsd/packageIncludes",
				"src/test/resources/xsd/packageIncludes/reference");
	}

	@Test
	public void test_multipleAppSchema() {
		/*
		 * Test XML Schema creation for two Application Schema, with identity
		 * transformation.
		 */
		multiTest("src/test/resources/xsd/multipleAppSchema/testEA_MultipleAppSchema.xml",
				new String[] { "xsd" }, "testResults/xsd/multiAppSchema",
				"src/test/resources/xsd/multipleAppSchema/reference");
	}

	@Test
	public void test_documentationFromConceptDependencies() {
		/*
		 * A test model where documentation is retrieved from classifiers that
		 * are suppliers of a dependency (feature, attribute and value concepts)
		 */
		xsdTest("src/test/resources/xsd/documentationFromConceptDependencies/testEA_dep.xml",
				new String[] { "test_dep" }, null, "testResults/xsd/documentationFromConceptDependencies/INPUT",
				"src/test/resources/xsd/documentationFromConceptDependencies/reference");
	}

	@Test
	public void test_ade() {
		/*
		 * A simple CityGML Application Domain Extension (ADE)
		 */
		String[] xsdADE = { "ade" };
		xsdTest("src/test/resources/xsd/ade/testEA_ADE.xml", xsdADE, null,
				"testResults/xsd/ade/INPUT", "src/test/resources/xsd/ade/reference");
	}

	@Test
	public void test_aaa_sk() {
		/*
		 * Qualified associations as well as array and list properties. Note
		 * that there are errors reported during the conversion (on purpose)
		 * unlike in most other tests.
		 */
		String[] xsdaaask = { "testaaask" };
		xsdTest("src/test/resources/xsd/aaa_sk/testEA_aaa-sk.xml", xsdaaask, null,
				"testResults/xsd/aaa_sk/INPUT", "src/test/resources/xsd/aaa_sk/reference",
				false);
	}

	@Test
	public void test_gml33() {
		/*
		 * An association class and the GML 3.3 code list values
		 */
		String[] xsdgml33 = { "testgml33" };
		xsdTest("src/test/resources/xsd/gml33/testEA_gml33.xml", xsdgml33,
				xsdgml33, "testResults/xsd/gml33/INPUT",
				"src/test/resources/xsd/gml33/reference");
	}

	@Test
	public void test_groupmixin() {
		/*
		 * Test the mixin options
		 */
		String[] xsdmixin = { "testgroupmixin" };
		xsdTest("src/test/resources/xsd/groupmixin/testEA_groupmixin.xml", xsdmixin,
				null, "testResults/xsd/groupmixin/INPUT",
				"src/test/resources/xsd/groupmixin/reference");
	}

	@Test
	public void test_iso19115MetadataProfileAndSchematron() {
		/*
		 * A simple 19115 metadata profile and Schematron tests
		 */
		String[] xsdmd = { "testbasetypes", "testprofile", "testlet" };
		String[] schmd = { "testprofile", "testlet" };
		xsdTest("src/test/resources/xsd/iso19115MetadataProfileAndSchematron/testEA_md.xml", xsdmd, schmd,
				"testResults/xsd/iso19115MetadataProfileAndSchematron/INPUT", 
				"src/test/resources/xsd/iso19115MetadataProfileAndSchematron/reference");
	}

	@Test
	public void test_sweCommon20Encoding() {
		/*
		 * The SWE Common 2.0 encoding
		 */
		String[] xsdswe = { "advanced_encodings", "basic_types",
				"block_components", "choice_components", "record_components",
				"simple_components", "simple_encodings", "swe" };
		xsdTest("src/test/resources/xsd/sweCommon20Encoding/testEA_swe.xml", xsdswe, null,
				"testResults/xsd/sweCommon20Encoding/INPUT",
				"src/test/resources/xsd/sweCommon20Encoding/reference");
	}

	@Test
	public void test_targetCodeListURI() {

		String[] xsd = { "test" };
		xsdTest("src/test/resources/xsd/targetCodeListURI/testEA_xsd_targetCodeListURI.xml",
				xsd, null, "testResults/xsd/targetCodeListURI/INPUT",
				"src/test/resources/xsd/targetCodeListURI/reference");
	}

	@Test
	public void test_propNoTargetElement() {
		/*
		 * Test XML Schema creation for TODO
		 */
		String[] xsd = { "test2_prop" };
		xsdTest("src/test/resources/xsd/propNoTargetElement/testEA_propNoTargetElement.xml",
				xsd, null, "testResults/xsd/propNoTargetElement/INPUT",
				"src/test/resources/xsd/propNoTargetElement/reference");
	}

	@Test
	public void test_globalIdentifierAnnotation() {
		
		multiTest(
				"src/test/resources/xsd/globalIdentifierAnnotation/testEA_xsd_globalIdentifierAnnotation.xml",
				new String[] { "xsd" },
				"testResults/xsd/globalIdentifierAnnotation",
				"src/test/resources/xsd/globalIdentifierAnnotation/reference");
	}
	
	@Test
	public void test_descriptorAnnotation() {
		
		multiTest(
				"src/test/resources/xsd/descriptorAnnotation/testEA_xsd_descriptorAnnotation.xml",
				new String[] { "xsd" },
				"testResults/xsd/descriptorAnnotation",
				"src/test/resources/xsd/descriptorAnnotation/reference");
	}
	
	@Test
	public void test_unionSubtype() {
		
		multiTest(
				"src/test/resources/xsd/unionSubtype/testEA_xsd_unionSubtype.xml",
				new String[] { "xsd" },
				"testResults/xsd/unionSubtype",
				"src/test/resources/xsd/unionSubtype/reference");
	}
	
	@Test
	public void test_xsdPropertyMapEntry() {
		
		multiTest(
				"src/test/resources/xsd/xsdPropertyMapEntry/test_xsdPropertyMapEntry.xml",
				new String[] { "xsd" },
				"testResults/xsd/xsdPropertyMapEntry",
				"src/test/resources/xsd/xsdPropertyMapEntry/reference");
	}
	
	@Test
	public void test_allowAllTags() {
		
		multiTest(
				"src/test/resources/xsd/allowAllTags/test_allowAllTags.xml",
				new String[] { "xsd" },
				"testResults/xsd/allowAllTags",
				"src/test/resources/xsd/allowAllTags/reference");
	}
	
	@Test
	public void test_codelistAsEAEnumeration() {
		
		multiTest(
				"src/test/resources/xsd/codelistAsEAEnumeration/test_codelistAsEAEnumeration.xml",
				new String[] { "xsd" },
				"testResults/xsd/codelistAsEAEnumeration",
				"src/test/resources/xsd/codelistAsEAEnumeration/reference");
	}
	
	@Test
	public void test_dataTypeAsUMLClassifier() {
		
		multiTest(
				"src/test/resources/xsd/dataTypeAsUMLClassifier/test_dataTypeAsUMLClassifier.xml",
				new String[] { "xsd" },
				"testResults/xsd/dataTypeAsUMLClassifier",
				"src/test/resources/xsd/dataTypeAsUMLClassifier/reference");
	}
	
	@Test
	public void test_propertyMetadata() {
		
		multiTest(
				"src/test/resources/xsd/propertyMetadata/testEA_xsd_propertyMetadata.xml",
				new String[] { "xsd" },
				"testResults/xsd/propertyMetadata/results",
				"src/test/resources/xsd/propertyMetadata/reference/results");
		
		multiTest(
				"src/test/resources/xsd/propertyMetadata/testEA_xsd_propertyMetadata_iso19139.xml",
				new String[] { "xsd" },
				"testResults/xsd/propertyMetadata/results_iso19139",
				"src/test/resources/xsd/propertyMetadata/reference/results_iso19139");
	}
	
	@Test
	public void test_forcedImports() {
		
		multiTest(
				"src/test/resources/xsd/forcedImports/testEA_xsd_forcedImports.xml",
				new String[] { "xsd" },
				"testResults/xsd/forcedImports",
				"src/test/resources/xsd/forcedImports/reference");
	}
	
	@Test
	public void test_basicTypeList() {
		
		multiTest(
				"src/test/resources/xsd/basicTypeList/testEA_xsd_basicTypeList.xml",
				new String[] { "xsd" },
				"testResults/xsd/basicTypeList",
				"src/test/resources/xsd/basicTypeList/reference");
	}
	
	@Test
	public void test_xmlEncodingInfos() {
		
		multiTest(
				"src/test/resources/xsd/xmlEncodingInfos/test_xmlEncodingInfos.xml",
				new String[] { "xsd", "xml" },
				"testResults/xsd/xmlEncodingInfos/INPUT",
				"src/test/resources/xsd/xmlEncodingInfos/reference/INPUT");
	}
}
