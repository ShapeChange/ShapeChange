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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.HTMLDocumentBuilder;
import org.custommonkey.xmlunit.TolerantSaxDocumentBuilder;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.jaxp13.Validator;
import org.junit.Before;
import org.w3c.dom.Document;
import org.xml.sax.SAXParseException;

import com.google.common.base.Joiner;

import de.interactive_instruments.ShapeChange.Util.ZipHandler;
import de.interactive_instruments.ShapeChange.Util.ea.EAModelDiff;

/**
 * Basic unit test for ShapeChange
 */
public abstract class BasicTest {

	String suffix_configToExportModel = "_exportModel";
	String suffix_configRunWithExportedModel = "_runWithExportedModel";

	boolean testTime = false;
	boolean exportModel = false;
	boolean runWithExportedModel = false;

	/*
	 * The following two fields are primarily used during test development. They
	 * should be set to false for actual tests.
	 */
	boolean justCompareResults = false;
	boolean skipActualTest = false;

	protected void multiTest(String config, String[] fileFormatsToCheck,
			String basedirResults, String basedirReference) {

		String actualConfig = getActualConfig(config);

		Set<String> fileFormatsToCheckLC = null;

		if (fileFormatsToCheck != null) {

			fileFormatsToCheckLC = new HashSet<String>();

			for (int i = 0; i < fileFormatsToCheck.length; i++) {
				fileFormatsToCheckLC
						.add(fileFormatsToCheck[i].trim().toLowerCase());
			}
		}

		if (!justCompareResults) {
			long start = (new Date()).getTime();
			TestInstance test = new TestInstance(actualConfig);
			long end = (new Date()).getTime();
			System.out.println("Execution time " + actualConfig + ": "
					+ (end - start) + "ms");
			assertTrue("Test model execution failed", test.noError());
			if (testTime)
				assertTrue("Execution time too long", end - start < 90000);
		}

		if (!exportModel && !skipActualTest) {
			multiTestInDirs(fileFormatsToCheckLC, basedirResults,
					basedirReference);
		}
	}

	/**
	 * Simply processes the given configuration and ensures that no errors were
	 * reported. This can be used whenever some processing needs to occur where
	 * the result type is not yet supported for comparison (e.g. EA
	 * repositories). It can also be useful if the execution of the
	 * configuration is a necessary precondition for execution of another
	 * configuration (that may require the output of the first process as
	 * input).
	 * 
	 * @param config
	 * @return result of ShapeChange, can be used to inspect e.g. the options
	 *         object in it
	 */
	protected ShapeChangeResult execute(String config) {

		long start = (new Date()).getTime();
		TestInstance test = new TestInstance(config);
		long end = (new Date()).getTime();
		System.out.println(
				"Execution time " + config + ": " + (end - start) + "ms");
		assertTrue("Test model execution failed", test.noError());
		if (testTime)
			assertTrue("Execution time too long", end - start < 90000);
		return test.result;
	}

	/**
	 * Simply processes the given configuration and ensure that an error was
	 * reported.
	 * 
	 * @param config
	 */
	protected void executeAndError(String config,
			String detailsOnExpectedError) {

		String details = detailsOnExpectedError != null ? detailsOnExpectedError
				: "<no details on expected error provided in test configuration>";

		long start = (new Date()).getTime();
		TestInstance test = new TestInstance(config);
		long end = (new Date()).getTime();
		System.out.println(
				"Execution time " + config + ": " + (end - start) + "ms");
		assertTrue(
				"Test model execution did not fail with an error. " + details,
				!test.noError());
	}

	/**
	 * Modifies the configuration file path depending upon whether the model
	 * shall be exported or whether the test should be run with the exported
	 * model.
	 * 
	 * @param config
	 * @return
	 */
	private String getActualConfig(String config) {

		if (exportModel || runWithExportedModel) {

			String actualConfig = config;

			if (config.indexOf(".xml") > -1) {
				actualConfig = config.substring(0, config.indexOf(".xml"));
			}

			String[] parts = actualConfig.split("config");
			String base = parts[0];
			String filename = parts[1];

			if (exportModel) {

				actualConfig = base + "config" + suffix_configToExportModel
						+ File.separator + filename + suffix_configToExportModel
						+ ".xml";

			} else if (runWithExportedModel) {

				actualConfig = base + "config"
						+ suffix_configRunWithExportedModel + File.separator
						+ filename + suffix_configRunWithExportedModel + ".xml";
			}

			return actualConfig;

		} else {

			return config;
		}
	}

	private void multiTestInDirs(Set<String> fileFormatsToCheck,
			String dirResults, String dirReference) {

		String[] extensions = fileFormatsToCheck
				.toArray(new String[fileFormatsToCheck.size()]);

		File refDir = new File(dirReference);
		File resultDir = new File(dirResults);

		Collection<File> refFiles = null;
		Collection<File> resultFiles = FileUtils.listFiles(resultDir,
				extensions, false);

		if (refDir.exists()) {
			refFiles = FileUtils.listFiles(refDir, extensions, false);
		} else if (!resultFiles.isEmpty()) {
			fail("Result directory " + resultDir.getAbsolutePath()
					+ " contains relevant files to check. However, the reference directory "
					+ dirResults + " does not exist.");
		}

		SortedSet<String> refFileNames = new TreeSet<String>();
		if (refFiles != null) {
			for (File refFile : refFiles) {
				refFileNames.add(refFile.getName());
			}
		}

		File resDir = new File(dirResults);

		// we check that the ref files are similar to the result files
		// we determine the files to check by inspecting the result files, then
		// doing the similarity test

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

					refFileNames.remove(fres.getName());

					String fresExtension = FilenameUtils
							.getExtension(fres.getName()).trim()
							.toLowerCase(Locale.ENGLISH);

					if ((fresExtension.equals("xsd")
							&& fileFormatsToCheck.contains("xsd"))
							|| (fresExtension.equals("xml")
									&& fileFormatsToCheck.contains("xml"))
							|| (fresExtension.equals("rdf")
									&& fileFormatsToCheck.contains("rdf"))) {
						similar(dirResults + File.separator + fres.getName(),
								dirReference + File.separator + fres.getName());
					} else if (fresExtension.equals("html")
							&& fileFormatsToCheck.contains("html")) {
						similarHtml(
								dirResults + File.separator + fres.getName(),
								dirReference + File.separator + fres.getName());
					} else if (fresExtension.equals("docx")
							&& fileFormatsToCheck.contains("docx")) {
						similarDocx(
								dirResults + File.separator + fres.getName(),
								dirReference + File.separator + fres.getName());
					} else if (fresExtension.equals("sql")
							&& fileFormatsToCheck.contains("sql")) {
						similarTxt(dirResults + File.separator + fres.getName(),
								dirReference + File.separator + fres.getName(),
								true, true, true);
					} else if (fresExtension.equals("ttl")
							&& fileFormatsToCheck.contains("ttl")) {
						similarJenaModel(
								dirResults + File.separator + fres.getName(),
								dirReference + File.separator + fres.getName());
					} else if (fresExtension.equals("json")
							&& fileFormatsToCheck.contains("json")) {
						similarJson(
								dirResults + File.separator + fres.getName(),
								dirReference + File.separator + fres.getName());
					} else if (fresExtension.equals("eap")
							&& fileFormatsToCheck.contains("eap")) {
						similarEap(dirResults + File.separator + fres.getName(),
								dirReference + File.separator + fres.getName());
					} else {
						// TBD add more similarity tests for further file
						// formats, or add them to one of the above
					}
				}
			}

			if (!refFileNames.isEmpty()) {

				String unmatchedReferenceFiles = Joiner.on(", ").skipNulls()
						.join(refFileNames);
				fail("No corresponding result files found for the following reference files (in directory "
						+ refDir.getAbsolutePath() + "): "
						+ unmatchedReferenceFiles);
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
			boolean normalizeWhitespace, boolean trimEachLine,
			boolean ignoreLinesWithWhitespaceOnly) {

		List<String> lines = new ArrayList<String>();
		List<String> linesReference = new ArrayList<String>();

		// read files

		BufferedReader reader = null;
		try {
			InputStream stream = new FileInputStream(new File(txtFileName));
			reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
			String txt;
			while ((txt = reader.readLine()) != null) {
				if (!txt.trim().isEmpty() || !ignoreLinesWithWhitespaceOnly) {
					lines.add(txt);
				}
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
				if (!txtReference.trim().isEmpty()
						|| !ignoreLinesWithWhitespaceOnly) {
					linesReference.add(txtReference);
				}
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
					+ " lines. File to be compared has more lines("
					+ lines.size() + ").");
		} else if (lines.size() < linesReference.size()) {
			fail("Reference file has " + linesReference.size()
					+ " lines. File to be compared has less lines("
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

	protected void xsdTest(String config, String[] xsds, String[] schs,
			String basedirResults, String basedirReference) {
		xsdTest(config, xsds, schs, null, basedirResults, basedirReference,
				true);
	}

	protected void xsdTest(String config, String[] xsds, String[] schs,
			HashMap<String, String> replacevalues, String basedirResults,
			String basedirReference) {
		xsdTest(config, xsds, schs, replacevalues, basedirResults,
				basedirReference, true);
	}

	protected void xsdTest(String config, String[] xsds, String[] schs,
			String basedirResults, String basedirReference, boolean noErrors) {
		xsdTest(config, xsds, schs, null, basedirResults, basedirReference,
				noErrors);
	}

	protected void xsdTest(String config, String[] xsds, String[] schs,
			HashMap<String, String> replacevalues, String basedirResults,
			String basedirReference, boolean noErrors) {

		String actualConfig = getActualConfig(config);

		long start = (new Date()).getTime();
		TestInstance test = new TestInstance(actualConfig, replacevalues);
		long end = (new Date()).getTime();
		System.out.println(
				"Execution time " + actualConfig + ": " + (end - start) + "ms");
		if (noErrors)
			assertTrue("Test model execution failed", test.noError());
		if (testTime)
			assertTrue("Execution time too long", end - start < 90000);

		if (!exportModel) {
			if (xsds != null) {
				for (String xsd : xsds) {
					similar(basedirResults + "/" + xsd + ".xsd",
							basedirReference + "/" + xsd + ".xsd");
				}
			}
			if (schs != null) {
				for (String xsd : schs) {
					similar(basedirResults + "/" + xsd
							+ ".xsd_SchematronSchema.xml",
							basedirReference + "/" + xsd
									+ ".xsd_SchematronSchema.xml");
				}
			}
		}
	}

	protected void sqlTest(String config, String[] sqls,
			HashMap<String, String> replacevalues, String basedirResults,
			String basedirReference, boolean noErrors) {

		String actualConfig = getActualConfig(config);

		long start = (new Date()).getTime();
		TestInstance test = new TestInstance(actualConfig, replacevalues);
		long end = (new Date()).getTime();

		System.out.println(
				"Execution time " + actualConfig + ": " + (end - start) + "ms");

		if (noErrors)
			assertTrue("Test model execution failed", test.noError());

		if (testTime)
			assertTrue("Execution time too long", end - start < 90000);

		if (!exportModel) {
			if (sqls != null) {
				for (String sql : sqls) {
					similarTxt(basedirResults + "/" + sql + ".sql",
							basedirReference + "/" + sql + ".sql", true, true,
							true);
				}
			}
		}
	}

	protected void jsonTest(String config, String[] typenames,
			String basedirResults, String basedirReference) {

		String actualConfig = getActualConfig(config);

		long start = (new Date()).getTime();
		TestInstance test = new TestInstance(actualConfig);
		long end = (new Date()).getTime();
		System.out.println(
				"Execution time " + actualConfig + ": " + (end - start) + "ms");
		assertTrue("Test model execution failed", test.noError());
		if (testTime)
			assertTrue("Exceution time too long", end - start < 60000);

		if (!exportModel) {
			for (String typename : typenames) {

				String fileName = basedirResults + "/test/" + typename
						+ ".json";
				String referenceFileName = basedirReference + "/test/"
						+ typename + ".json";

				similarJson(fileName, referenceFileName);
			}
		}
	}

	private void similarJson(String fileName, String referenceFileName) {

		ObjectMapper m = new ObjectMapper();

		try {
			JsonNode rootNodeResult = m.readValue(new File(fileName),
					JsonNode.class);
			JsonNode rootNodeReference = m
					.readValue(new File(referenceFileName), JsonNode.class);
			boolean equal = rootNodeResult.equals(rootNodeReference);
			assertTrue(
					"JSON output differs from reference result. Result file: "
							+ fileName + " - Reference file: "
							+ referenceFileName,
					equal);
		} catch (JsonParseException e) {
			fail("JSON Parse Exception: " + e.getMessage());
		} catch (JsonMappingException e) {
			fail("JSON Mapping Exception: " + e.getMessage());
		} catch (IOException e) {
			fail("IO Exception: " + e.getMessage());
		}
	}

	private void similarEap(String fileName, String referenceFileName) {

		try {

			File file = new File(fileName);
			File referenceFile = new File(referenceFileName);

			EAModelDiff differ = new EAModelDiff();

			boolean similar = differ.similar(file, referenceFile);

			assertTrue("EAP output differs from reference result. Result file: "
					+ fileName + " - Reference file: " + referenceFileName
					+ ". Details:\n" + differ.getDiffDetails(), similar);

		} catch (Exception e) {
			fail("Exception while comparing EAP '" + fileName
					+ "' to reference file '" + referenceFileName
					+ "'. Exception message is: " + e.getMessage());
		}

	}

	protected void rdfTest(String config, String[] rdfs, String basedirResults,
			String basedirReference) {

		String actualConfig = getActualConfig(config);

		long start = (new Date()).getTime();
		TestInstance test = new TestInstance(actualConfig);
		long end = (new Date()).getTime();
		System.out.println(
				"Execution time " + actualConfig + ": " + (end - start) + "ms");
		assertTrue("Test model execution failed", test.noError());
		if (testTime)
			assertTrue("Execution time too long", end - start < 60000);

		if (!exportModel) {
			if (rdfs != null) {
				for (String rdf : rdfs) {
					similar(basedirResults + "/" + rdf + ".rdf",
							basedirReference + "/" + rdf + ".rdf");
				}
			}
		}
	}

	private void similarJenaModel(String fileName, String referenceFileName) {

		Model model = ModelFactory.createDefaultModel();
		model.read(fileName);

		Model ref = ModelFactory.createDefaultModel();
		ref.read(referenceFileName);

		if (model.isIsomorphicWith(ref)) {
			// fine
		} else {

			/*
			 * FIXME: blank nodes are an issue - they always create a difference
			 * 
			 * See
			 * http://answers.semanticweb.com/questions/21247/can-we-compare-two
			 * -rdf-statements-objects
			 */
			// statements in model that aren't in ref
			Model modelMinusRef = model.difference(ref);
			modelMinusRef.setNsPrefixes(model.getNsPrefixMap());
			// statements in ref that aren't in model
			Model refMinusModel = ref.difference(model);
			refMinusModel.setNsPrefixes(ref.getNsPrefixMap());

			try {

				ByteArrayOutputStream fout1 = new ByteArrayOutputStream();
				RDFDataMgr.write(fout1, modelMinusRef, RDFFormat.TURTLE_PRETTY);
				String diff1 = fout1.toString("UTF-8");

				ByteArrayOutputStream fout2 = new ByteArrayOutputStream();
				RDFDataMgr.write(fout2, refMinusModel, RDFFormat.TURTLE_PRETTY);
				String diff2 = fout2.toString("UTF-8");

				fail("Input and reference model are not isomorphic.\r\n"
						+ "Statements in input model that are not in the reference model (NOTE: triples involving blank nodes may be false positives):\r\n"
						+ diff1 + "\r\n\r\n------------\r\n\r\n"
						+ "Statements in reference model that are not in the input model (NOTE: triples involving blank nodes may be false positives):\r\n"
						+ diff2);
			} catch (Exception e) {
				fail("Could not compare " + fileName + " and "
						+ referenceFileName);
			}
		}
	}

	protected void htmlTest(String config, String[] htmls,
			String basedirResults, String basedirReference) {

		String actualConfig = getActualConfig(config);

		long start = (new Date()).getTime();
		TestInstance test = new TestInstance(actualConfig);
		long end = (new Date()).getTime();
		System.out.println(
				"Execution time " + actualConfig + ": " + (end - start) + "ms");

		assertTrue("Test model execution failed", test.noError());

		if (testTime)
			assertTrue("Execution time too long", end - start < 60000);

		if (!exportModel) {
			if (htmls != null) {
				for (String htmlFileName : htmls) {
					similarHtml(basedirResults + "/" + htmlFileName + ".html",
							basedirReference + "/" + htmlFileName + ".html");
				}
			}
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

	protected void docxTest(String config, String[] docxs,
			String basedirResults, String basedirReference) {

		String actualConfig = getActualConfig(config);

		long start = (new Date()).getTime();
		TestInstance test = new TestInstance(actualConfig);
		long end = (new Date()).getTime();
		System.out.println(
				"Execution time " + actualConfig + ": " + (end - start) + "ms");

		assertTrue("Test model execution failed", test.noError());

		if (testTime)
			assertTrue("Execution time too long", end - start < 60000);

		if (!exportModel) {
			if (docxs != null) {
				for (String docxFileName : docxs) {
					similarDocx(basedirResults + "/" + docxFileName + ".docx",
							basedirReference + "/" + docxFileName + ".docx");
				}
			}
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

	@Before
	public void initialise() {
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
