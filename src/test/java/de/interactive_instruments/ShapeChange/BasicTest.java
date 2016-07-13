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
 * (c) 2002-2012 interactive instruments GmbH, Bonn, Germany
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

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.SystemUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.HTMLDocumentBuilder;
import org.custommonkey.xmlunit.TolerantSaxDocumentBuilder;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.jaxp13.Validator;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXParseException;

import de.interactive_instruments.ShapeChange.Util.ZipHandler;
import de.interactive_instruments.ShapeChange.TestInstance;

/**
 * Basic unit test for ShapeChange
 * 
 * <li>Process without configuration file and verify that no error is reported
 * <li>Process XMI 1.0 test model and verify that no error is reported
 * <li>On Windows, process EA test model and verify that no error is reported
 */
public class BasicTest {

	boolean testTime = false;

	@Test
	public void test() {
		initialise();

		/*
		 * Invoke without parameters, if we are connected
		 */
		String[] xsdTest = { "test" };
		try {
			URL url = new URL(
					"http://shapechange.net/resources/config/minimal.xml");
			InputStream configStream = url.openStream();
			if (configStream != null) {
				xsdTest(null, xsdTest, null, ".",
						"src/test/resources/reference/xsd");
			}
		} catch (Exception e) {
		}

		/*
		 * Process the XMI 1.0 test model
		 */
		xsdTest("src/test/resources/config/testXMI.xml", xsdTest, null,
				"testResults/xmi/INPUT", "src/test/resources/reference/xsd");

		/*
		 * On Windows process also the EA test models
		 */
		if (SystemUtils.IS_OS_WINDOWS) {

			/*
			 * First the same test model as the XMI 1.0 test model
			 */
			xsdTest("src/test/resources/config/testEA.xml", xsdTest, null,
					"testResults/ea/INPUT", "src/test/resources/reference/xsd");

			/*
			 * Now with some replacement values
			 */
			HashMap<String, String> replace = new HashMap<String, String>();
			replace.put("$eap$", "src/test/resources/test.eap");
			replace.put("$log$", "testResults/ea/log.xml");
			replace.put("$out$", "testResults/ea");
			xsdTest("src/test/resources/config/testEA_x.xml", xsdTest, null,
					replace, "testResults/ea/INPUT",
					"src/test/resources/reference/xsd");

			/*
			 * Test property options: ordered, uniqueness and inline/byReference
			 */
			multiTest("src/test/resources/config/testEA_prop.xml", new String[] { "xsd", "xml", "html" },
					"testResults/ea/prop/INPUT", "src/test/resources/reference/prop");

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

			/*
			 * Test multiple tagged values and stereotypes - with array
			 * implementation for tagged values.
			 */
			replace2 = new HashMap<String, String>();
			replace2.put("$tvimpl$", "array");
			replace2.put("$logout$",
					"testResults/multipleTaggedValuesAndStereotypes/tv_array_impl/log.xml");
			replace2.put("$xsdout$",
					"testResults/multipleTaggedValuesAndStereotypes/tv_array_impl");
			xsdTest("src/test/resources/config/testEA_multipleTaggedValuesAndStereotypes.xml",
					new String[] { "multTvAndSt" }, null, replace2,
					"testResults/multipleTaggedValuesAndStereotypes/tv_array_impl/INPUT",
					"src/test/resources/reference/xsd/multipleTaggedValuesAndStereotypes/INPUT");

			/*
			 * Test the descriptor functionality
			 */
			multiTest("src/test/resources/config/testEA_descriptors_fc_en.xml",
					new String[] { "xml", "html" },
					"testResults/html/descriptors/INPUT",
					"src/test/resources/reference/html/descriptors");

			multiTest("src/test/resources/config/testEA_descriptors_fc_de.xml",
					new String[] { "xml", "html" },
					"testResults/html/descriptors/INPUT",
					"src/test/resources/reference/html/descriptors");

			multiTest(
					"src/test/resources/config/testEA_descriptors_inspire.xml",
					new String[] { "xml", "html" },
					"testResults/html/descriptors/INPUT",
					"src/test/resources/reference/html/descriptors");

			multiTest("src/test/resources/config/testEA_descriptors_aaa.xml",
					new String[] { "xml", "html" },
					"testResults/html/descriptors/INPUT",
					"src/test/resources/reference/html/descriptors");

			multiTest("src/test/resources/config/testEA_descriptors_bbr.xml",
					new String[] { "xml", "html" },
					"testResults/html/descriptors/INPUT",
					"src/test/resources/reference/html/descriptors");

			/*
			 * Test rule-xsd-cls-codelist-constraints-codeAbsenceInModelAllowed.
			 */
			multiTest("src/test/resources/config/testEA_codeAbsenceInModel.xml",
					new String[] { "xml" },
					"testResults/codeAbsenceInModel/input",
					"src/test/resources/reference/xsd/codeAbsenceInModel/input");
			
			/*
			 * Test derivation of application schema metadata.
			 */
			multiTest("src/test/resources/config/testEA_schema_metadata.xml",
					new String[] { "xml" },
					"testResults/schema_metadata/INPUT",
					"src/test/resources/reference/schema_metadata/INPUT");
			
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

			/*
			 * Test derivation of application schema differences (output as
			 * single page HTML feature catalogue).
			 */
			multiTest("src/test/resources/config/testEA_model_diff.xml",
					new String[] { "xml", "html" },
					"testResults/html/diff/INPUT",
					"src/test/resources/reference/html/diff/INPUT");

			/*
			 * SQL - basic text
			 */
			multiTest("src/test/resources/config/testEA_sql.xml",
					new String[] { "sql" }, "testResults/sql/basic",
					"src/test/resources/reference/sql/basic");

			/*
			 * SQL - associative tables tests
			 */
			multiTest(
					"src/test/resources/config/testEA_sqlAssociativeTables.xml",
					new String[] { "sql" }, "testResults/sql/associativeTables",
					"src/test/resources/reference/sql/associativeTables");

			/*
			 * SQL - geometry parameters test
			 */
			multiTest(
					"src/test/resources/config/testEA_sqlGeometryParameters.xml",
					new String[] { "sql" },
					"testResults/sql/geometryParameters",
					"src/test/resources/reference/sql/geometryParameters");

			/*
			 * Test XML Schema creation for Application Schema with multiple
			 * packages that shall be output as individual xsd files.
			 */
			multiTest("src/test/resources/config/testEA_packageIncludes.xml",
					new String[] { "xsd" }, "testResults/xsd/packageIncludes",
					"src/test/resources/reference/xsd/packageIncludes");

			/*
			 * Test XML Schema creation for two Application Schema, with
			 * identity transformation.
			 */
			multiTest("src/test/resources/config/testEA_MultipleAppSchema.xml",
					new String[] { "xsd" }, "testResults/xsd/multiAppSchema",
					"src/test/resources/reference/xsd/multiAppSchema");

			/*
			 * A simple model to test the creation of a single-file html feature
			 * catalogue
			 */
			htmlTest("src/test/resources/config/testEA_Html.xml",
					new String[] { "test" }, "testResults/html/INPUT",
					"src/test/resources/reference/html");

			/*
			 * A simple model to test the localization functionality
			 */
			htmlTest("src/test/resources/config/testEA_HtmlLocalization.xml",
					new String[] { "test" },
					"testResults/html/localization/INPUT",
					"src/test/resources/reference/html/localization");

			/*
			 * A simple model to test the creation of a docx feature catalogue
			 */
			docxTest("src/test/resources/config/testEA_Docx.xml",
					new String[] { "test" }, "testResults/docx/myInputId",
					"src/test/resources/reference/docx");

			/*
			 * A simple model to test the creation of a docx feature catalogue
			 * that includes UML diagrams
			 */
			// TODO image file names and sizes not stable
			// docxTest("src/test/resources/config/testEA_Docx_FC_with_images.xml",
			// new String[]{"test_featurecatalog_with_images"},
			// "testResults/docx_with_images/myInputId",
			// "src/test/resources/reference/docx");

			/*
			 * A test model where documentation is retrieved from classifiers
			 * that are suppliers of a dependency (feature, attribute and value
			 * concepts)
			 */
			xsdTest("src/test/resources/config/testEA_dep.xml", xsdTest, null,
					"testResults/ea/INPUT", "src/test/resources/reference/xsd");

			/*
			 * A simple CityGML Application Domain Extension (ADE)
			 */
			String[] xsdADE = { "ade" };
			xsdTest("src/test/resources/config/testEA_ADE.xml", xsdADE, null,
					"testResults/ea/INPUT", "src/test/resources/reference/xsd");

			/*
			 * Qualified associations as well as array and list properties. Note
			 * that there are errors reported during the conversion (on purpose)
			 * unlike in most other tests.
			 */
			String[] xsdaaask = { "testaaask" };
			xsdTest("src/test/resources/config/testEA_aaa-sk.xml", xsdaaask,
					null, "testResults/ea/INPUT",
					"src/test/resources/reference/xsd", false);

			/*
			 * An association class and the GML 3.3 code list values
			 */
			String[] xsdgml33 = { "testgml33" };
			xsdTest("src/test/resources/config/testEA_gml33.xml", xsdgml33,
					xsdgml33, "testResults/ea/INPUT",
					"src/test/resources/reference/xsd");

			/*
			 * Test the mixin options
			 */
			String[] xsdmixin = { "testgroupmixin" };
			xsdTest("src/test/resources/config/testEA_groupmixin.xml", xsdmixin,
					null, "testResults/ea/INPUT",
					"src/test/resources/reference/xsd");

			/*
			 * A simple 19115 metadata profile and Schematron tests
			 */
			String[] xsdmd = { "testbasetypes", "testprofile", "testlet" };
			String[] schmd = { "testprofile", "testlet" };
			xsdTest("src/test/resources/config/testEA_md.xml", xsdmd, schmd,
					"testResults/ea/md/INPUT",
					"src/test/resources/reference/xsd");

			/*
			 * The SWE Common 2.0 encoding
			 */
			String[] xsdswe = { "advanced_encodings", "basic_types",
					"block_components", "choice_components",
					"record_components", "simple_components",
					"simple_encodings", "swe" };
			xsdTest("src/test/resources/config/testEA_swe.xml", xsdswe, null,
					"testResults/ea/swe/INPUT",
					"src/test/resources/reference/xsd/swe");

			// TODO add INSPIRE, OKSTRA, no GML, more Schematron tests

			/*
			 * JSON encoding with geoservices encoding rule
			 */
			String[] typenamesGsr = { "FeatureType1", "FeatureType2" };
			jsonTest("src/test/resources/config/testEA_JsonGsr.xml",
					typenamesGsr, "testResults/ea/json/geoservices/INPUT",
					"src/test/resources/reference/json/geoservices");

			/*
			 * JSON encoding with extended geoservices encoding rule
			 */
			String[] typenamesGsrExtended = { "DataType", "DataType2",
					"FeatureType1", "FeatureType2", "NilUnion", "Union" };
			jsonTest("src/test/resources/config/testEA_JsonGsrExtended.xml",
					typenamesGsrExtended,
					"testResults/ea/json/geoservices_extended/INPUT",
					"src/test/resources/reference/json/geoservices_extended");

			/*
			 * SKOS codelists
			 */
			String[] rdfskos = { "Codelists" };
			rdfTest("src/test/resources/config/testEA_skos.xml", rdfskos,
					"testResults/ea/skos/INPUT",
					"src/test/resources/reference/rdf/skos");

			/*
			 * OWL 19150-2 ontologies
			 */
			String[] rdfowl = { "SchemaA/SchemaA", "SchemaB/SchemaB" };
			// FIXME text comparison does not work, order changes each time,
			// compare ttl files on a triple level (with Jena?)
			// ttlTest("src/test/resources/config/testEA_owliso19150_default.xml",
			// rdfowl, "testResults/ea/owl/default/INPUT",
			// "src/test/resources/reference/rdf/owliso19150/default");
			// ttlTest("src/test/resources/config/testEA_owliso19150_extensions.xml",
			// rdfowl, "testResults/ea/owl/extensions/INPUT",
			// "src/test/resources/reference/rdf/owliso19150/extensions");

			/*
			 * Flattening transformation
			 */
			multiTest("src/test/resources/config/testEA_Flattening.xml",
					new String[] { "xsd" }, "testResults/flattening/xsd",
					"src/test/resources/reference/flattening/xsd");

			/*
			 * Flattening transformation - only homogeneous geometries
			 */
			multiTest(
					"src/test/resources/config/testEA_Flattening_homogeneousGeometries.xml",
					new String[] { "xsd" },
					"testResults/flattening/homogeneousGeometry_core/",
					"src/test/resources/reference/flattening/homogeneousGeometry");

			/*
			 * Flattening transformation - only homogeneous geometries - Test1
			 * (handling of associations)
			 */
			multiTest(
					"src/test/resources/config/testEA_Flattening_homogeneousGeometries_test1.xml",
					new String[] { "xsd" },
					"testResults/flattening/homogeneousGeometry_test1/",
					"src/test/resources/reference/flattening/homogeneousGeometry_test1");

			/*
			 * Flattening transformation - only inheritance
			 */
			multiTest(
					"src/test/resources/config/testEA_Flattening_inheritance.xml",
					new String[] { "xsd" },
					"testResults/flattening/inheritance/",
					"src/test/resources/reference/flattening/inheritance");

			/*
			 * Flattening transformation - cycles (and isFlatTarget setting)
			 */
			multiTest("src/test/resources/config/testEA_Flattening_cycles.xml",
					new String[] { "xsd" },
					"testResults/flattening/xsd/cycles_step1",
					"src/test/resources/reference/flattening/xsd/cycles_step1");

			/*
			 * Flattening transformation - remove feature-2-feature
			 * relationships
			 */
			multiTest(
					"src/test/resources/config/testEA_Flattening_removeFeatureTypeRelationships.xml",
					new String[] { "xsd" },
					"testResults/flattening/removeFeatureTypeRelationships",
					"src/test/resources/reference/flattening/removeFeatureTypeRelationships");

			/*
			 * Flattening transformation - remove object-2-feature relationships
			 * for specific object types
			 */
			multiTest(
					"src/test/resources/config/testEA_Flattening_removeObjectToFeatureTypeNavigability.xml",
					new String[] { "xsd" },
					"testResults/flattening/removeObjectToFeatureTypeNavigability",
					"src/test/resources/reference/flattening/removeObjectToFeatureTypeNavigability");

			/*
			 * Replication schema target
			 */
			multiTest("src/test/resources/config/testEA_repSchema.xml",
					new String[] { "xsd" }, "testResults/repSchema/repXsd",
					"src/test/resources/reference/xsd/replicationSchema");

			/*
			 * Attribute creation transformer
			 */
			multiTest("src/test/resources/config/testEA_attributeCreation.xml",
					new String[] { "xsd" },
					"testResults/attribute_creation/xsd",
					"src/test/resources/reference/xsd/attributeCreation");

			/*
			 * Test the profiling functionality
			 */
			multiTest("src/test/resources/config/testEA_Profiling.xml",
					new String[] { "xsd", "xml" }, "testResults/profiling/xsd",
					"src/test/resources/reference/profiling/xsd");

			/*
			 * Test the constraint validation functionality
			 */
			multiTest("src/test/resources/config/testEA_Profiling_withConstraintValidation.xml",
					new String[] { "xml" }, "testResults/profiling/constraintValidation/results",
					"src/test/resources/reference/profiling/constraintValidation/results");
			
			/*
			 * Test the profiling functionality - with explicit profile settings
			 * behavior
			 */
			multiTest(
					"src/test/resources/config/testEA_Profiling_explicitProfileSettings.xml",
					new String[] { "xsd", "xml" },
					"testResults/profiling_explicitProfileSettings/xsd",
					"src/test/resources/reference/profiling_explicitProfileSettings/xsd");

			// /*
			// * Test the creation of a frame-based html feature catalogue
			// */
			// multiTest("src/test/resources/config/testEA_HtmlFrame.xml",
			// new String[] { "html" }, "testResults/html/frame/INPUT",
			// "src/test/resources/reference/html/frame/INPUT");

			/*
			 * Schematron derived from SBVR (with intermediate translation to
			 * FOL)
			 */
			multiTest("src/test/resources/config/testEA_Sbvr.xml",
					new String[] { "xml" },
					"testResults/fol/fromSbvr/sch/step3",
					"src/test/resources/reference/sch/fromSbvr");
			
			/*
			 * Test the GML 3.3 based transformation of association classes.
			 */
			multiTest("src/test/resources/config/testEA_associationClassMapper.xml",
					new String[] { "xsd" },
					"testResults/associationClassTransform/associationClassMapper",
					"src/test/resources/reference/associationClassTransform/associationClassMapper");
		}
	}

	private void multiTest(String config, String[] fileFormatsToCheck,
			String basedirResults, String basedirReference) {

		Set<String> fileFormatsToCheckLC = null;

		if (fileFormatsToCheck != null) {

			fileFormatsToCheckLC = new HashSet<String>();

			for (int i = 0; i < fileFormatsToCheck.length; i++) {
				fileFormatsToCheckLC
						.add(fileFormatsToCheck[i].trim().toLowerCase());
			}
		}

		long start = (new Date()).getTime();
		TestInstance test = new TestInstance(config);
		long end = (new Date()).getTime();
		System.out.println(
				"Execution time " + config + ": " + (end - start) + "ms");
		assertTrue("Test model execution failed", test.noError());
		if (testTime)
			assertTrue("Execution time too long", end - start < 90000);

		multiTestInDirs(fileFormatsToCheckLC, basedirResults, basedirReference);
	}

	private void multiTestInDirs(Set<String> fileFormatsToCheck,
			String dirResults, String dirReference) {

		File resDir = new File(dirResults);

		// we check that the ref files are similar to the result files
		// we determine the files to check by inspecting the result files, then
		// doing the similarity test
		// TBD: this does not check that all reference files are contained by
		// the result
		File[] filesInResDir = resDir.listFiles();

		if (filesInResDir == null) {
			fail("Result directory " + dirResults
					+ " is not a directory or an I/O error occurred.");
		} else {
			for (File fres : filesInResDir) {
				if (fres.isDirectory()) {
					String pathAdd = File.separator + fres.getName();
					multiTestInDirs(fileFormatsToCheck, dirResults + pathAdd,
							dirReference + pathAdd);
				} else {

					String fresExtension = FilenameUtils
							.getExtension(fres.getName()).trim()
							.toLowerCase(Locale.ENGLISH);

					if (fileFormatsToCheck == null) {

						if (fresExtension.equals("xsd")
								|| fresExtension.equals("xml")
								|| fresExtension.equals("rdf")) {
							similar(dirResults + File.separator
									+ fres.getName(),
									dirReference + File.separator
											+ fres.getName());
						} else if (fresExtension.equals("html")) {
							similarHtml(
									dirResults + File.separator
											+ fres.getName(),
									dirReference + File.separator
											+ fres.getName());
						} else if (fresExtension.equals("docx")) {
							similarDocx(
									dirResults + File.separator
											+ fres.getName(),
									dirReference + File.separator
											+ fres.getName());
						} else if (fresExtension.equals("sql")) {
							similarTxt(
									dirResults + File.separator
											+ fres.getName(),
									dirReference + File.separator
											+ fres.getName(),
									true, true);
						} else {
							// TBD add more similarity tests for further file
							// formats, or add them to one of the above
						}
					} else {
						if ((fresExtension.equals("xsd")
								&& fileFormatsToCheck.contains("xsd"))
								|| (fresExtension.equals("xml")
										&& fileFormatsToCheck.contains("xml"))
								|| (fresExtension.equals("rdf")
										&& fileFormatsToCheck
												.contains("rdf"))) {
							similar(dirResults + File.separator
									+ fres.getName(),
									dirReference + File.separator
											+ fres.getName());
						} else if (fresExtension.equals("html")
								&& fileFormatsToCheck.contains("html")) {
							similarHtml(
									dirResults + File.separator
											+ fres.getName(),
									dirReference + File.separator
											+ fres.getName());
						} else if (fresExtension.equals("docx")
								&& fileFormatsToCheck.contains("docx")) {
							similarDocx(
									dirResults + File.separator
											+ fres.getName(),
									dirReference + File.separator
											+ fres.getName());
						} else if (fresExtension.equals("sql")
								&& fileFormatsToCheck.contains("sql")) {
							similarTxt(
									dirResults + File.separator
											+ fres.getName(),
									dirReference + File.separator
											+ fres.getName(),
									true, true);
						} else {
							// TBD add more similarity tests for further file
							// formats, or add them to one of the above
						}
					}
				}
			}
		}
	}

	/**
	 * Checks that two text files are similar. They must have the same number of
	 * lines. Each line must be similar, which is checked via an equals test on
	 * the line content. The content may be trimmed and whitespace normalized
	 * before comparing if the parameters are set accordingly.
	 * 
	 * @param txtFileName
	 * @param referenceTxtFileName
	 * @param normalizeWhitespace
	 *            true if all sequences of whitespace within a line shall be
	 *            replaced by a single space character before comparing
	 * @param trimEachLine
	 *            <code>true</code> if whitespace surrounding lines shall be
	 *            trimmed before comparing
	 */
	private void similarTxt(String txtFileName, String referenceTxtFileName,
			boolean normalizeWhitespace, boolean trimEachLine) {

		List<String> lines = new ArrayList<String>();
		List<String> linesReference = new ArrayList<String>();

		// read files

		BufferedReader reader = null;
		try {
			InputStream stream = new FileInputStream(new File(txtFileName));
			reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
			String txt;
			while ((txt = reader.readLine()) != null) {
				lines.add(txt);
			}
		} catch (Exception e) {
			fail("Could not read file to compare at " + txtFileName);
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		BufferedReader readerReference = null;
		try {
			InputStream streamReference = new FileInputStream(
					new File(referenceTxtFileName));

			readerReference = new BufferedReader(
					new InputStreamReader(streamReference, "UTF-8"));
			String txtReference;
			while ((txtReference = readerReference.readLine()) != null) {
				linesReference.add(txtReference);
			}
		} catch (Exception e) {
			fail("Could not read reference file at " + referenceTxtFileName);
		} finally {
			try {
				if (readerReference != null) {
					readerReference.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// compare

		if (lines.size() > linesReference.size()) {
			fail("Reference file has " + linesReference.size()
					+ " lines. File to be compared has less lines("
					+ lines.size() + ").");
		} else if (lines.size() < linesReference.size()) {
			fail("Reference file has " + linesReference.size()
					+ " lines. File to be compared has more lines("
					+ lines.size() + ").");
		} else {

			for (int i = 0; i < lines.size(); i++) {

				// get original line content
				String txt = lines.get(i);
				String txtReference = linesReference.get(i);

				/*
				 * copy line content so that modifications may be applied before
				 * comparing, without losing the original text
				 */
				String txtComp = txt;
				String txtCompRef = txtReference;

				if (trimEachLine) {
					txtComp = txtComp.trim();
					txtCompRef = txtCompRef.trim();
				}

				if (normalizeWhitespace) {
					txtComp = txtComp.replaceAll("\\s+", " ");
					txtCompRef = txtCompRef.replaceAll("\\s+", " ");
				}

				if (!txtComp.equals(txtCompRef)) {
					fail("Mismatch in line " + (i + 1) + ". Expected: '"
							+ txtReference + "'; found: '" + txt + "'.");
				} else {
					// fine, continue
				}
			}
		}
	}

	private void xsdTest(String config, String[] xsds, String[] schs,
			String basedirResults, String basedirReference) {
		xsdTest(config, xsds, schs, null, basedirResults, basedirReference,
				true);
	}

	private void xsdTest(String config, String[] xsds, String[] schs,
			HashMap<String, String> replacevalues, String basedirResults,
			String basedirReference) {
		xsdTest(config, xsds, schs, replacevalues, basedirResults,
				basedirReference, true);
	}

	private void xsdTest(String config, String[] xsds, String[] schs,
			String basedirResults, String basedirReference, boolean noErrors) {
		xsdTest(config, xsds, schs, null, basedirResults, basedirReference,
				noErrors);
	}

	private void xsdTest(String config, String[] xsds, String[] schs,
			HashMap<String, String> replacevalues, String basedirResults,
			String basedirReference, boolean noErrors) {
		long start = (new Date()).getTime();
		TestInstance test = new TestInstance(config, replacevalues);
		long end = (new Date()).getTime();
		System.out.println(
				"Execution time " + config + ": " + (end - start) + "ms");
		if (noErrors)
			assertTrue("Test model execution failed", test.noError());
		if (testTime)
			assertTrue("Execution time too long", end - start < 90000);
		if (xsds != null)
			for (String xsd : xsds) {
				similar(basedirResults + "/" + xsd + ".xsd",
						basedirReference + "/" + xsd + ".xsd");
			}
		if (schs != null)
			for (String xsd : schs) {
				similar(basedirResults + "/" + xsd
						+ ".xsd_SchematronSchema.xml",
						basedirReference + "/" + xsd
								+ ".xsd_SchematronSchema.xml");
			}
	}

	private void sqlTest(String config, String[] sqls, String basedirResults,
			String basedirReference) {
		sqlTest(config, sqls, null, basedirResults, basedirReference, true);
	}

	private void sqlTest(String config, String[] sqls,
			HashMap<String, String> replacevalues, String basedirResults,
			String basedirReference) {
		sqlTest(config, sqls, replacevalues, basedirResults, basedirReference,
				true);
	}

	private void sqlTest(String config, String[] sqls, String basedirResults,
			String basedirReference, boolean noErrors) {
		sqlTest(config, sqls, null, basedirResults, basedirReference, noErrors);
	}

	private void sqlTest(String config, String[] sqls,
			HashMap<String, String> replacevalues, String basedirResults,
			String basedirReference, boolean noErrors) {

		long start = (new Date()).getTime();
		TestInstance test = new TestInstance(config, replacevalues);
		long end = (new Date()).getTime();

		System.out.println(
				"Execution time " + config + ": " + (end - start) + "ms");

		if (noErrors)
			assertTrue("Test model execution failed", test.noError());

		if (testTime)
			assertTrue("Execution time too long", end - start < 90000);

		if (sqls != null) {
			for (String sql : sqls) {
				similarTxt(basedirResults + "/" + sql + ".sql",
						basedirReference + "/" + sql + ".sql", true, true);
			}
		}
	}

	private void jsonTest(String config, String[] typenames,
			String basedirResults, String basedirReference) {
		long start = (new Date()).getTime();
		TestInstance test = new TestInstance(config);
		long end = (new Date()).getTime();
		System.out.println(
				"Execution time " + config + ": " + (end - start) + "ms");
		assertTrue("Test model execution failed", test.noError());
		if (testTime)
			assertTrue("Exceution time too long", end - start < 60000);

		ObjectMapper m = new ObjectMapper();
		for (String typename : typenames) {
			try {
				JsonNode rootNodeResult = m.readValue(
						new File(
								basedirResults + "/test/" + typename + ".json"),
						JsonNode.class);
				JsonNode rootNodeReference = m.readValue(new File(
						basedirReference + "/test/" + typename + ".json"),
						JsonNode.class);
				boolean equal = rootNodeResult.equals(rootNodeReference);
				assertTrue("JSON output differs from reference result for type "
						+ typename, equal);
			} catch (JsonParseException e) {
				fail("JSON Parse Exception: " + e.getMessage());
			} catch (JsonMappingException e) {
				fail("JSON Mapping Exception: " + e.getMessage());
			} catch (IOException e) {
				fail("IO Exception: " + e.getMessage());
			}
		}
	}

	private void rdfTest(String config, String[] rdfs, String basedirResults,
			String basedirReference) {
		long start = (new Date()).getTime();
		TestInstance test = new TestInstance(config);
		long end = (new Date()).getTime();
		System.out.println(
				"Execution time " + config + ": " + (end - start) + "ms");
		assertTrue("Test model execution failed", test.noError());
		if (testTime)
			assertTrue("Execution time too long", end - start < 60000);
		if (rdfs != null)
			for (String rdf : rdfs) {
				similar(basedirResults + "/" + rdf + ".rdf",
						basedirReference + "/" + rdf + ".rdf");
			}
	}

	private void ttlTest(String config, String[] rdfs, String basedirResults,
			String basedirReference) {
		long start = (new Date()).getTime();
		TestInstance test = new TestInstance(config);
		long end = (new Date()).getTime();
		System.out.println(
				"Execution time " + config + ": " + (end - start) + "ms");
		assertTrue("Test model execution failed", test.noError());
		if (testTime)
			assertTrue("Execution time too long", end - start < 60000);
		if (rdfs != null)
			for (String rdf : rdfs) {
				similar(basedirResults + "/" + rdf + ".ttl",
						basedirReference + "/" + rdf + ".ttl");
			}
	}

	private void htmlTest(String config, String[] htmls, String basedirResults,
			String basedirReference) {
		long start = (new Date()).getTime();
		TestInstance test = new TestInstance(config);
		long end = (new Date()).getTime();
		System.out.println(
				"Execution time " + config + ": " + (end - start) + "ms");
		assertTrue("Test model execution failed", test.noError());
		if (testTime)
			assertTrue("Execution time too long", end - start < 60000);
		if (htmls != null)
			for (String htmlFileName : htmls) {
				similarHtml(basedirResults + "/" + htmlFileName + ".html",
						basedirReference + "/" + htmlFileName + ".html");
			}
	}

	/**
	 * Tests if there are differences in the contents of two html files.
	 * 
	 * Internally, the html files are converted to XHTML and then an XML diff is
	 * performed.
	 * 
	 * @param htmlFileName
	 * @param referenceHtmlFileName
	 */
	private void similarHtml(String htmlFileName,
			String referenceHtmlFileName) {

		String myControlHTML = null;
		String myTestHTML = null;
		try {
			myControlHTML = readFile(referenceHtmlFileName);
		} catch (Exception e) {
			fail("Could not read " + referenceHtmlFileName);
		}
		try {
			myTestHTML = readFile(htmlFileName);
		} catch (Exception e) {
			fail("Could not read " + htmlFileName);
		}

		try {
			TolerantSaxDocumentBuilder tolerantSaxDocumentBuilder = new TolerantSaxDocumentBuilder(
					XMLUnit.newTestParser());
			HTMLDocumentBuilder htmlDocumentBuilder = new HTMLDocumentBuilder(
					tolerantSaxDocumentBuilder);

			Document wellFormedControlHTMLDocument = htmlDocumentBuilder
					.parse(myControlHTML);
			Document wellFormedTestHTMLDocument = htmlDocumentBuilder
					.parse(myTestHTML);

			Diff myDiff = new Diff(wellFormedControlHTMLDocument,
					wellFormedTestHTMLDocument);
			assertTrue(
					"HTML: " + htmlFileName + " similar to "
							+ referenceHtmlFileName + " - " + myDiff.toString(),
					myDiff.similar());
		} catch (Exception e) {
			fail("Could not compare " + htmlFileName + " and "
					+ referenceHtmlFileName);
		}
	}

	private void docxTest(String config, String[] docxs, String basedirResults,
			String basedirReference) {
		long start = (new Date()).getTime();
		TestInstance test = new TestInstance(config);
		long end = (new Date()).getTime();
		System.out.println(
				"Execution time " + config + ": " + (end - start) + "ms");
		assertTrue("Test model execution failed", test.noError());
		if (testTime)
			assertTrue("Execution time too long", end - start < 60000);
		if (docxs != null)
			for (String docxFileName : docxs) {
				similarDocx(basedirResults + "/" + docxFileName + ".docx",
						basedirReference + "/" + docxFileName + ".docx");
			}
	}

	/**
	 * Tests if there are differences in the contents of two docx files.
	 * 
	 * Currently only tests the internal word/document.xml.
	 * 
	 * WARNING: word will most likely modify the content of a docx file
	 * generated by ShapeChange when it is saved by word! This means that the
	 * reference docx file must be the file generated by ShapeChange, WITHOUT it
	 * having been opened in and saved by word.
	 * 
	 * @param docxFileName
	 * @param referenceDocxFileName
	 */
	private void similarDocx(String docxFileName,
			String referenceDocxFileName) {

		ZipHandler zipHandler = new ZipHandler();

		File tmpDir = null;

		try {
			File myControlFile = new File(referenceDocxFileName);
			File myTestFile = new File(docxFileName);

			// create temporary directories
			File myTestFileDir = myTestFile.getParentFile();
			tmpDir = new File(myTestFileDir, "docxUnitTest.tmp");
			File tmpDirUnzippedTestFile = new File(tmpDir, "test");
			File tmpDirUnzippedControlFile = new File(tmpDir, "control");

			// unzip docx files
			zipHandler.unzip(myControlFile, tmpDirUnzippedControlFile);
			zipHandler.unzip(myTestFile, tmpDirUnzippedTestFile);

			// perform diff
			// TODO: at the moment only compares the document.xml - this could
			// also test all docx-internal files
			File testDocument = new File(tmpDirUnzippedTestFile,
					"word/document.xml");
			File controlDocument = new File(tmpDirUnzippedControlFile,
					"word/document.xml");

			Reader testFileReader = new FileReader(testDocument);
			Reader controlFileReader = new FileReader(controlDocument);

			Diff myDiff = new Diff(controlFileReader, testFileReader);
			assertTrue(
					"DOCX: " + docxFileName + " similar to "
							+ referenceDocxFileName + " - " + myDiff.toString(),
					myDiff.similar());

			// finally, delete the temporary directory
			// -> handled in 'finally' clause

		} catch (Exception e) {
			fail("Could not compare " + docxFileName + " and "
					+ referenceDocxFileName);
		} finally {
			if (tmpDir != null && tmpDir.exists()) {
				try {
					FileUtils.deleteDirectory(tmpDir);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void similar(String xsdFileName, String referenceXsdFileName) {
		String myControlXML = null;
		String myTestXML = null;
		try {
			myControlXML = readFile(referenceXsdFileName);
		} catch (Exception e) {
			fail("Could not read " + referenceXsdFileName);
		}
		try {
			myTestXML = readFile(xsdFileName);
		} catch (Exception e) {
			fail("Could not read " + xsdFileName);
		}
		try {
			Diff myDiff = new Diff(myControlXML, myTestXML);
			assertTrue(
					"XML: " + xsdFileName + " similar to "
							+ referenceXsdFileName + " - " + myDiff.toString(),
					myDiff.similar());
		} catch (Exception e) {
			fail("Could not compare " + xsdFileName + " and "
					+ referenceXsdFileName);
		}
	}

	private String readFile(String fileName) throws Exception {
		InputStream stream = new FileInputStream(new File(fileName));

		Writer writer = new StringWriter();
		char[] buffer = new char[1024];
		Reader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
			int n;
			while ((n = reader.read(buffer)) != -1) {
				writer.write(buffer, 0, n);
			}
		} finally {
			if (reader != null)
				reader.close();
		}
		return writer.toString();
	}

	@SuppressWarnings("unused")
	private void validate(String xmlFileName, String xsdFileName) {
		Validator v = new Validator();

		v.addSchemaSource(new StreamSource(new File(xsdFileName)));
		if (!v.isSchemaValid()) {
			for (Object o : v.getSchemaErrors()) {
				SAXParseException error = (SAXParseException) o;
				if (error != null)
					fail("Validation of " + xsdFileName
							+ " failed. First error: " + error.getMessage());
			}
		} else {
			StreamSource is = new StreamSource(new File(xmlFileName));
			if (!v.isInstanceValid(is)) {
				for (Object o : v.getInstanceErrors(is)) {
					SAXParseException error = (SAXParseException) o;
					if (error != null)
						fail("Validation of " + xmlFileName + " failed against "
								+ xsdFileName + ". First error: "
								+ error.getMessage());
				}
			}
		}
	}

	private void initialise() {
		System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
				"org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
		System.setProperty("javax.xml.parsers.SAXParserFactory",
				"org.apache.xerces.jaxp.SAXParserFactoryImpl");
		System.setProperty("javax.xml.transform.TransformerFactory",
				"org.apache.xalan.processor.TransformerFactoryImpl");

		XMLUnit.setIgnoreComments(true);
		XMLUnit.setNormalizeWhitespace(true);
	}

}
