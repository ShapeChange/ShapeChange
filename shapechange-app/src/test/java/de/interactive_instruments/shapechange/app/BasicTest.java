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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
import java.net.MalformedURLException;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.HTMLDocumentBuilder;
import org.custommonkey.xmlunit.TolerantSaxDocumentBuilder;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.jaxp13.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.w3c.dom.Document;
import org.xml.sax.SAXParseException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.flipkart.zjsonpatch.JsonDiff;
import com.google.common.base.Joiner;

import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.util.ExternalCallException;
import de.interactive_instruments.shapechange.core.util.ExternalCallUtil;
import de.interactive_instruments.shapechange.core.util.ZipHandler;
import de.interactive_instruments.shapechange.ea.util.EAModelDiff;

/**
 * Basic unit test for ShapeChange
 */
public abstract class BasicTest {

    boolean testTime = false;

    /*
     * The following fields are primarily used during test development. They should
     * be set to false for actual tests.
     */
    boolean justCompareResults = false;

    protected void multiTest(String config, String[] fileFormatsToCheck, String basedirResults,
	    String basedirReference) {

	Set<String> fileFormatsToCheckLC = null;

	if (fileFormatsToCheck != null) {

	    fileFormatsToCheckLC = new HashSet<String>();

	    for (int i = 0; i < fileFormatsToCheck.length; i++) {
		fileFormatsToCheckLC.add(fileFormatsToCheck[i].trim().toLowerCase());
	    }
	}

	if (!justCompareResults) {
	    long start = (new Date()).getTime();
	    TestInstance test = new TestInstance(config);
	    long end = (new Date()).getTime();
	    System.out.println("Execution time " + config + ": " + (end - start) + "ms");
	    assertTrue(test.noError(), "Test model execution failed");
	    if (testTime)
		assertTrue(end - start < 90000, "Execution time too long");
	}

	multiTestInDirs(fileFormatsToCheckLC, basedirResults, basedirReference);
    }

    /**
     * Simply processes the given configuration and ensures that no errors were
     * reported. This can be used whenever some processing needs to occur where the
     * result type is not yet supported for comparison (e.g. EA repositories). It
     * can also be useful if the execution of the configuration is a necessary
     * precondition for execution of another configuration (that may require the
     * output of the first process as input).
     * 
     * @param config tbd
     * @return result of ShapeChange, can be used to inspect e.g. the options object
     *         in it
     */
    protected ShapeChangeResult execute(String config) {

	long start = (new Date()).getTime();
	TestInstance test = new TestInstance(config);
	long end = (new Date()).getTime();
	System.out.println("Execution time " + config + ": " + (end - start) + "ms");
	assertTrue(test.noError(), "Test model execution failed");
	if (testTime)
	    assertTrue(end - start < 90000, "Execution time too long");
	return test.result;
    }

    /**
     * Simply processes the given configuration and ensure that an error was
     * reported.
     * 
     * @param config                 tbd
     * @param detailsOnExpectedError tbd
     */
    protected void executeAndError(String config, String detailsOnExpectedError) {

	String details = detailsOnExpectedError != null ? detailsOnExpectedError
		: "<no details on expected error provided in test configuration>";

	long start = (new Date()).getTime();
	TestInstance test = new TestInstance(config);
	long end = (new Date()).getTime();
	System.out.println("Execution time " + config + ": " + (end - start) + "ms");
	assertTrue(!test.noError(), "Test model execution did not fail with an error. " + details);
    }

    protected void multiTestInDirs(Set<String> fileFormatsToCheck, String dirResults, String dirReference) {

	String[] extensions = fileFormatsToCheck.toArray(new String[fileFormatsToCheck.size()]);

	File refDir = new File(dirReference);
	File resultDir = new File(dirResults);

	Collection<File> refFiles = null;
	Collection<File> resultFiles = FileUtils.listFiles(resultDir, extensions, false);

	if (refDir.exists()) {
	    refFiles = FileUtils.listFiles(refDir, extensions, false);
	} else if (!resultFiles.isEmpty()) {
	    fail("Result directory " + resultDir.getAbsolutePath()
		    + " contains relevant files to check. However, the reference directory " + dirReference
		    + " does not exist.");
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
	    fail("Result directory " + dirResults + " is not a directory or an I/O error occurred.");
	} else {

	    for (File fres : filesInResDir) {

		if (fres.isDirectory()) {
		    String pathAdd = File.separator + fres.getName();
		    multiTestInDirs(fileFormatsToCheck, dirResults + pathAdd, dirReference + pathAdd);
		} else {

		    refFileNames.remove(fres.getName());

		    String fresExtension = FilenameUtils.getExtension(fres.getName()).trim()
			    .toLowerCase(Locale.ENGLISH);

		    if ((fresExtension.equals("xsd") && fileFormatsToCheck.contains("xsd"))
			    || (fresExtension.equals("xml") && fileFormatsToCheck.contains("xml"))
			    || (fresExtension.equals("rdf") && fileFormatsToCheck.contains("rdf"))
			    || (fresExtension.equals("sch") && fileFormatsToCheck.contains("sch"))
			    || (fresExtension.equals("gfs") && fileFormatsToCheck.contains("gfs"))) {
			similar(dirResults + File.separator + fres.getName(),
				dirReference + File.separator + fres.getName());
		    } else if (fresExtension.equals("html") && fileFormatsToCheck.contains("html")) {
			similarHtml(dirResults + File.separator + fres.getName(),
				dirReference + File.separator + fres.getName());
		    } else if (fresExtension.equals("docx") && fileFormatsToCheck.contains("docx")) {
			similarDocx(dirResults + File.separator + fres.getName(),
				dirReference + File.separator + fres.getName());
		    } else if (fresExtension.equals("sql") && fileFormatsToCheck.contains("sql")) {
			similarTxt(dirResults + File.separator + fres.getName(),
				dirReference + File.separator + fres.getName(), true, true, true);
		    } else if (fresExtension.equals("ttl") && fileFormatsToCheck.contains("ttl")) {
			similarJenaModel(dirResults + File.separator + fres.getName(),
				dirReference + File.separator + fres.getName());
		    } else if (fresExtension.equals("json") && fileFormatsToCheck.contains("json")) {
			similarJson(dirResults + File.separator + fres.getName(),
				dirReference + File.separator + fres.getName());
		    } else if (fresExtension.equals("qea") && fileFormatsToCheck.contains("qea")) {
			similarEaRepo(dirResults + File.separator + fres.getName(),
				dirReference + File.separator + fres.getName());
		    } else if ((fresExtension.equals("yaml") && fileFormatsToCheck.contains("yaml"))
			    || (fresExtension.equals("yml") && fileFormatsToCheck.contains("yml"))) {
			similarYaml(dirResults + File.separator + fres.getName(),
				dirReference + File.separator + fres.getName());
		    } else if (fresExtension.equals("gpkg") && fileFormatsToCheck.contains("gpkg")) {
			similarGpkg(dirResults + File.separator + fres.getName(),
				dirReference + File.separator + fres.getName());
		    } else {
			// TBD add more similarity tests for further file
			// formats, or add them to one of the above
		    }
		}
	    }

	    if (!refFileNames.isEmpty()) {

		String unmatchedReferenceFiles = Joiner.on(", ").skipNulls().join(refFileNames);
		fail("No corresponding result files found for the following reference files (in directory "
			+ refDir.getAbsolutePath() + "): " + unmatchedReferenceFiles);
	    }
	}
    }

    private void similarGpkg(String gpkgFileName, String referenceGpkgFileName) {

	File gpkgFile = new File(gpkgFileName);
	File refGpkgFile = new File(referenceGpkgFileName);

	/*
	 * Use sqlite sqldiff tool to detect changes between the two files.
	 * 
	 * NOTE and TODO: The documentation of sqldiff says that it
	 * "does not (currently) display differences in TRIGGERs or VIEWs." source:
	 * https://www.sqlite.org/sqldiff.html (28.10.2022)
	 */
	String sqlDiffOutput = sqliteDiff(refGpkgFile, gpkgFile);

	if (StringUtils.isNotBlank(sqlDiffOutput)) {

	    // differences detected -> fail

	    // dump both schema and data of both files

	    File tmpFolder = new File(gpkgFile.getParentFile(), "unittesttmp");
	    if (tmpFolder.exists()) {
		FileUtils.deleteQuietly(tmpFolder);
	    }

	    try {

		FileUtils.forceMkdir(tmpFolder);

		storeSqliteDump(refGpkgFile, tmpFolder, "reference_");
		storeSqliteDump(gpkgFile, tmpFolder, "result_");

		fail("Differences detected between GeoPackages " + gpkgFile.getAbsolutePath() + " and "
			+ refGpkgFile.getAbsolutePath() + "\n" + "Dumps of both files have been stored in "
			+ tmpFolder.getAbsolutePath() + "\n"
			+ "sqldiff results (from reference file to result file):\n\n" + sqlDiffOutput);

	    } catch (IOException e) {

		fail("Differences detected between GeoPackages " + gpkgFile.getAbsolutePath() + " and "
			+ refGpkgFile.getAbsolutePath() + "\n"
			+ "Exception occurred while creating folder for storing the dumps for both files in "
			+ tmpFolder.getAbsolutePath() + " (exception message is: " + e.getMessage() + ")\n"
			+ "Here are the sqldiff results (from reference file to result file):\n\n" + sqlDiffOutput);
	    }
	}
    }

    private String sqliteDiff(File refGpkgFile, File gpkgFile) {

	List<String> cmds = new ArrayList<String>();
	cmds.add("sqldiff");
	cmds.add("--primarykey");
	cmds.add(refGpkgFile.getAbsolutePath());
	cmds.add(gpkgFile.getAbsolutePath());

	String sqlDiffOutput = "";
	try {
	    sqlDiffOutput = ExternalCallUtil.call(cmds);
	} catch (ExternalCallException e) {
	    fail("Exception occurred while performing sqldiff  of " + refGpkgFile.getAbsolutePath() + " and "
		    + gpkgFile.getAbsolutePath() + ". Exception message is: " + e.getMessage());
	}

	return sqlDiffOutput;
    }

    private void storeSqliteDump(File gpkgFile, File tmpFolder, String dumpFileNamePrefix) {

	File targetFile = new File(tmpFolder, dumpFileNamePrefix + gpkgFile.getName() + ".dump");

	List<String> cmds = new ArrayList<String>();
	cmds.add("sqlite3");
	cmds.add(gpkgFile.getAbsolutePath());
	cmds.add(".dump");

	try {
	    String dumpOutput = ExternalCallUtil.call(cmds);

	    FileUtils.writeStringToFile(targetFile, dumpOutput, "UTF-8");

	} catch (ExternalCallException | IOException e) {
	    fail("Exception occurred while storing the dump of " + gpkgFile.getAbsolutePath() + " in "
		    + targetFile.getAbsolutePath() + ". Exception message is: " + e.getMessage());
	}
    }

    /**
     * Checks that two text files are similar. They must have the same number of
     * lines. Each line must be similar, which is checked via an equals test on the
     * line content. The content may be trimmed and whitespace normalized before
     * comparing if the parameters are set accordingly.
     * 
     * @param txtFileName
     * @param referenceTxtFileName
     * @param normalizeWhitespace  true if all sequences of whitespace within a line
     *                             shall be replaced by a single space character
     *                             before comparing
     * @param trimEachLine         <code>true</code> if whitespace surrounding lines
     *                             shall be trimmed before comparing
     */
    private void similarTxt(String txtFileName, String referenceTxtFileName, boolean normalizeWhitespace,
	    boolean trimEachLine, boolean ignoreLinesWithWhitespaceOnly) {

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
	    InputStream streamReference = new FileInputStream(new File(referenceTxtFileName));

	    readerReference = new BufferedReader(new InputStreamReader(streamReference, "UTF-8"));
	    String txtReference;
	    while ((txtReference = readerReference.readLine()) != null) {
		if (!txtReference.trim().isEmpty() || !ignoreLinesWithWhitespaceOnly) {
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
	    fail("Reference file has " + linesReference.size() + " lines. File to be compared has more lines("
		    + lines.size() + ").");
	} else if (lines.size() < linesReference.size()) {
	    fail("Reference file has " + linesReference.size() + " lines. File to be compared has less lines("
		    + lines.size() + ").");
	} else {

	    for (int i = 0; i < lines.size(); i++) {

		// get original line content
		String txt = lines.get(i);
		String txtReference = linesReference.get(i);

		/*
		 * copy line content so that modifications may be applied before comparing,
		 * without losing the original text
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
		    fail("Mismatch in line " + (i + 1) + ". Expected: '" + txtReference + "'; found: '" + txt + "'.");
		} else {
		    // fine, continue
		}
	    }
	}
    }

    protected void xsdTest(String config, String[] xsds, String[] schs, String basedirResults,
	    String basedirReference) {
	xsdTest(config, xsds, schs, null, basedirResults, basedirReference, true);
    }

    protected void xsdTest(String config, String[] xsds, String[] schs, HashMap<String, String> replacevalues,
	    String basedirResults, String basedirReference) {
	xsdTest(config, xsds, schs, replacevalues, basedirResults, basedirReference, true);
    }

    protected void xsdTest(String config, String[] xsds, String[] schs, String basedirResults, String basedirReference,
	    boolean noErrors) {
	xsdTest(config, xsds, schs, null, basedirResults, basedirReference, noErrors);
    }

    protected void xsdTest(String config, String[] xsdFileNamesWithoutExtension, String[] schFileNamesWithoutExtension,
	    HashMap<String, String> replacevalues, String basedirResults, String basedirReference, boolean noErrors) {

	long start = (new Date()).getTime();
	TestInstance test = new TestInstance(config, replacevalues);
	long end = (new Date()).getTime();
	System.out.println("Execution time " + config + ": " + (end - start) + "ms");
	if (noErrors)
	    assertTrue(test.noError(), "Test model execution failed");
	if (testTime)
	    assertTrue(end - start < 90000, "Execution time too long");

	if (xsdFileNamesWithoutExtension != null) {
	    for (String xsdFileNameWithoutExtension : xsdFileNamesWithoutExtension) {
		similar(basedirResults + "/" + xsdFileNameWithoutExtension + ".xsd",
			basedirReference + "/" + xsdFileNameWithoutExtension + ".xsd");
	    }
	}
	if (schFileNamesWithoutExtension != null) {
	    for (String schFileNameWithoutExtension : schFileNamesWithoutExtension) {
		similar(basedirResults + "/" + schFileNameWithoutExtension + ".xsd_SchematronSchema.xml",
			basedirReference + "/" + schFileNameWithoutExtension + ".xsd_SchematronSchema.xml");
	    }
	}
    }

    protected void sqlTest(String config, String[] fileNamesWithoutExtension, HashMap<String, String> replacevalues,
	    String basedirResults, String basedirReference, boolean noErrors) {

	long start = (new Date()).getTime();
	TestInstance test = new TestInstance(config, replacevalues);
	long end = (new Date()).getTime();

	System.out.println("Execution time " + config + ": " + (end - start) + "ms");

	if (noErrors)
	    assertTrue(test.noError(), "Test model execution failed");

	if (testTime)
	    assertTrue(end - start < 90000, "Execution time too long");

	if (fileNamesWithoutExtension != null) {
	    for (String fileNameWithoutExtension : fileNamesWithoutExtension) {
		similarTxt(basedirResults + "/" + fileNameWithoutExtension + ".sql",
			basedirReference + "/" + fileNameWithoutExtension + ".sql", true, true, true);
	    }
	}
    }

    protected void yamlTest(String config, String[] fileNamesWithoutExtension, String basedirResults,
	    String basedirReference) {

	long start = (new Date()).getTime();
	TestInstance test = new TestInstance(config);
	long end = (new Date()).getTime();
	System.out.println("Execution time " + config + ": " + (end - start) + "ms");
	assertTrue(test.noError(), "Test model execution failed");
	if (testTime)
	    assertTrue(end - start < 90000, "Exceution time too long");

	for (String fileNameWithoutExtension : fileNamesWithoutExtension) {

	    String fileName = basedirResults + "/" + fileNameWithoutExtension + ".yml";
	    String referenceFileName = basedirReference + "/" + fileNameWithoutExtension + ".yml";

	    similarYaml(fileName, referenceFileName);
	}
    }

    protected void jsonTest(String config, String[] fileNamesWithoutExtension, String basedirResults,
	    String basedirReference) {

	long start = (new Date()).getTime();
	TestInstance test = new TestInstance(config);
	long end = (new Date()).getTime();
	System.out.println("Execution time " + config + ": " + (end - start) + "ms");
	assertTrue(test.noError(), "Test model execution failed");
	if (testTime)
	    assertTrue(end - start < 60000, "Exceution time too long");

	for (String fileNameWithoutExtension : fileNamesWithoutExtension) {

	    String fileName = basedirResults + "/" + fileNameWithoutExtension + ".json";
	    String referenceFileName = basedirReference + "/" + fileNameWithoutExtension + ".json";

	    similarJson(fileName, referenceFileName);
	}
    }

    private void similarJson(String fileName, String referenceFileName) {

	ObjectMapper m = new ObjectMapper();

	try {
	    JsonNode rootNodeResult = m.readValue(new File(fileName), JsonNode.class);
	    JsonNode rootNodeReference = m.readValue(new File(referenceFileName), JsonNode.class);

	    JsonNode patch = JsonDiff.asJson(rootNodeReference, rootNodeResult);
	    if (!patch.isEmpty()) {
		fail("JSON output differs from reference result.\r\n" + "Result file: " + fileName + "\r\n"
			+ "Reference file: " + referenceFileName + "\r\n" + "JSON patch info is:\r\n"
			+ "------------\r\n\r\n" + patch.toPrettyString());
	    }
	} catch (JsonParseException e) {
	    fail("JSON Parse Exception: " + e.getMessage());
	} catch (JsonMappingException e) {
	    fail("JSON Mapping Exception: " + e.getMessage());
	} catch (IOException e) {
	    fail("IO Exception: " + e.getMessage());
	}
    }

    private void similarYaml(String fileName, String referenceFileName) {

	ObjectMapper m = new ObjectMapper(new YAMLFactory());

	try {
	    JsonNode rootNodeResult = m.readValue(new File(fileName), JsonNode.class);
	    JsonNode rootNodeReference = m.readValue(new File(referenceFileName), JsonNode.class);

	    JsonNode patch = JsonDiff.asJson(rootNodeReference, rootNodeResult);
	    if (!patch.isEmpty()) {
		fail("YAML output differs from reference result.\r\n" + "Result file: " + fileName + "\r\n"
			+ "Reference file: " + referenceFileName + "\r\n" + "JSON patch info is:\r\n"
			+ "------------\r\n\r\n" + patch.toPrettyString());
	    }
	} catch (JsonParseException e) {
	    fail("Parse Exception: " + e.getMessage());
	} catch (JsonMappingException e) {
	    fail("Mapping Exception: " + e.getMessage());
	} catch (IOException e) {
	    fail("IO Exception: " + e.getMessage());
	}
    }

    private void similarEaRepo(String fileName, String referenceFileName) {

	try {

	    File file = new File(fileName);
	    if (!file.exists()) {
		fail("Result file " + file.getAbsolutePath() + " does not exist.");
	    }

	    File referenceFile = new File(referenceFileName);
	    if (!referenceFile.exists()) {
		fail("Reference file " + referenceFile.getAbsolutePath() + " does not exist.");
	    }

	    EAModelDiff differ = new EAModelDiff();

	    boolean similar = differ.similar(file, referenceFile);

	    assertTrue(similar, "EA repository output differs from reference result. Result file: " + fileName
		    + " - Reference file: " + referenceFileName + ". Details:\n" + differ.getDiffDetails());

	} catch (Exception e) {
	    fail("Exception while comparing EA repository '" + fileName + "' to reference file '" + referenceFileName
		    + "'. Exception message is: " + e.getMessage());
	}

    }

    protected void rdfTest(String config, String[] rdfs, String basedirResults, String basedirReference) {

	long start = (new Date()).getTime();
	TestInstance test = new TestInstance(config);
	long end = (new Date()).getTime();
	System.out.println("Execution time " + config + ": " + (end - start) + "ms");
	assertTrue(test.noError(), "Test model execution failed");
	if (testTime)
	    assertTrue(end - start < 60000, "Execution time too long");

	if (rdfs != null) {
	    for (String rdf : rdfs) {
		similar(basedirResults + "/" + rdf + ".rdf", basedirReference + "/" + rdf + ".rdf");
	    }
	}
    }

    private void similarJenaModel(String fileName, String referenceFileName) {

	try {
	    Model model = ModelFactory.createDefaultModel();
	    File f = new File(fileName);
	    model.read(f.toURI().toURL().toExternalForm());

	    Model ref = ModelFactory.createDefaultModel();
	    File rf = new File(referenceFileName);
	    ref.read(rf.toURI().toURL().toExternalForm());

	    if (model.isIsomorphicWith(ref)) {
		// fine
	    } else {

		/*
		 * FIXME: blank nodes are an issue - they always create a difference
		 * 
		 * See http://answers.semanticweb.com/questions/21247/can-we-compare-two
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
		    fail("Could not compare " + fileName + " and " + referenceFileName);
		}
	    }
	} catch (MalformedURLException e) {
	    fail("Could not compare " + fileName + " and " + referenceFileName);
	}
    }

    protected void htmlTest(String config, String[] fileNamesWithoutExtension, String basedirResults,
	    String basedirReference) {

	long start = (new Date()).getTime();
	TestInstance test = new TestInstance(config);
	long end = (new Date()).getTime();
	System.out.println("Execution time " + config + ": " + (end - start) + "ms");

	assertTrue(test.noError(), "Test model execution failed");

	if (testTime)
	    assertTrue(end - start < 60000, "Execution time too long");

	if (fileNamesWithoutExtension != null) {
	    for (String fileNameWithoutExtension : fileNamesWithoutExtension) {
		similarHtml(basedirResults + "/" + fileNameWithoutExtension + ".html",
			basedirReference + "/" + fileNameWithoutExtension + ".html");
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
    private void similarHtml(String htmlFileName, String referenceHtmlFileName) {

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
	    HTMLDocumentBuilder htmlDocumentBuilder = new HTMLDocumentBuilder(tolerantSaxDocumentBuilder);

	    Document wellFormedControlHTMLDocument = htmlDocumentBuilder.parse(myControlHTML);
	    Document wellFormedTestHTMLDocument = htmlDocumentBuilder.parse(myTestHTML);

	    Diff myDiff = new Diff(wellFormedControlHTMLDocument, wellFormedTestHTMLDocument);
	    assertTrue(myDiff.similar(),
		    "HTML: " + htmlFileName + " similar to " + referenceHtmlFileName + " - " + myDiff.toString());
	} catch (Exception e) {
	    fail("Could not compare " + htmlFileName + " and " + referenceHtmlFileName);
	}
    }

    protected void docxTest(String config, String[] fileNamesWithoutExtension, String basedirResults,
	    String basedirReference) {

	long start = (new Date()).getTime();
	TestInstance test = new TestInstance(config);
	long end = (new Date()).getTime();
	System.out.println("Execution time " + config + ": " + (end - start) + "ms");

	assertTrue(test.noError(), "Test model execution failed");

	if (testTime)
	    assertTrue(end - start < 60000, "Execution time too long");

	if (fileNamesWithoutExtension != null) {
	    for (String fileNameWithoutExtension : fileNamesWithoutExtension) {
		similarDocx(basedirResults + "/" + fileNameWithoutExtension + ".docx",
			basedirReference + "/" + fileNameWithoutExtension + ".docx");
	    }
	}
    }

    /**
     * Tests if there are differences in the contents of two docx files.
     * 
     * Currently only tests the internal word/document.xml.
     * 
     * WARNING: word will most likely modify the content of a docx file generated by
     * ShapeChange when it is saved by word! This means that the reference docx file
     * must be the file generated by ShapeChange, WITHOUT it having been opened in
     * and saved by word.
     * 
     * @param docxFileName
     * @param referenceDocxFileName
     */
    private void similarDocx(String docxFileName, String referenceDocxFileName) {

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
	    File testDocument = new File(tmpDirUnzippedTestFile, "word/document.xml");
	    File controlDocument = new File(tmpDirUnzippedControlFile, "word/document.xml");

	    Reader testFileReader = new FileReader(testDocument);
	    Reader controlFileReader = new FileReader(controlDocument);

	    Diff myDiff = new Diff(controlFileReader, testFileReader);
	    assertTrue(myDiff.similar(),
		    "DOCX: " + docxFileName + " similar to " + referenceDocxFileName + " - " + myDiff.toString());

	    // finally, delete the temporary directory
	    // -> handled in 'finally' clause

	} catch (Exception e) {
	    fail("Could not compare " + docxFileName + " and " + referenceDocxFileName);
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
	    assertTrue(myDiff.similar(),
		    "XML: " + xsdFileName + " similar to " + referenceXsdFileName + " - " + myDiff.toString());
	} catch (Exception e) {
	    fail("Could not compare " + xsdFileName + " and " + referenceXsdFileName);
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
		    fail("Validation of " + xsdFileName + " failed. First error: " + error.getMessage());
	    }
	} else {
	    StreamSource is = new StreamSource(new File(xmlFileName));
	    if (!v.isInstanceValid(is)) {
		for (Object o : v.getInstanceErrors(is)) {
		    SAXParseException error = (SAXParseException) o;
		    if (error != null)
			fail("Validation of " + xmlFileName + " failed against " + xsdFileName + ". First error: "
				+ error.getMessage());
		}
	    }
	}
    }

    @BeforeEach
    public void initEach() {
	System.setProperty("java.xml.transform.TransformerFactory", "");
    }

    @BeforeAll
    public static void initialise() {
	System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
		"org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
	System.setProperty("javax.xml.parsers.SAXParserFactory", "org.apache.xerces.jaxp.SAXParserFactoryImpl");
	System.setProperty("java.xml.transform.TransformerFactory", "");
	System.setProperty("scunittesting", "true");

	XMLUnit.setIgnoreComments(true);
	XMLUnit.setNormalizeWhitespace(true);
    }

    @AfterAll
    public static void tearDown() {
	System.setProperty("scunittesting", "");
	System.setProperty("java.xml.transform.TransformerFactory", "");
    }
}
