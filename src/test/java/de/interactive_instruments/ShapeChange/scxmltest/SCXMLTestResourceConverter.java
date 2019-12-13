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
 * (c) 2002-2019 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.scxmltest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.interactive_instruments.ShapeChange.TestInstance;
import de.interactive_instruments.ShapeChange.Util.XMLUtil;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments <dot>
 *         de)
 *
 */
public class SCXMLTestResourceConverter {

    /**
     * Name of the VM argument / system property to indicate that unit tests shall
     * be executed using the original configuration (by setting the value of the
     * system property to 'true'), for unit tests with tag SCXML.
     */
    public static final String RUN_ORIGINAL_CONFIGURATIONS_SYSTEM_PROPERTY_NAME = "runOriginalConfigurations";

    String suffix_configToExportModel = "_exportModel";
    String suffix_configRunWithSCXML = "_runWithSCXML";

    File tmpDir = new File("scxmlTmpDir");

    public String updateSCXMLTestResources(String configPath) throws Exception {

	if ("true".equalsIgnoreCase(System.getProperty(RUN_ORIGINAL_CONFIGURATIONS_SYSTEM_PROPERTY_NAME))) {

	    return configPath;

	} else {

	    if (!tmpDir.exists()) {
		tmpDir.mkdir();
	    }

	    // load original config, for creation of SCXML export configs
	    /*
	     * WARNING: The in-memory document will be updated if SCXML really is created!
	     * log, transformers, and targets from the original config will be removed, only
	     * the input will be kept and a new model export target section added.
	     */

	    Document doc1 = XMLUtil.loadXml(configPath);

	    if (creationOfScxmlIsNecessary(doc1, configPath)) {
		createScxml(doc1, configPath);
	    }

	    String pathToRelevantConfig;

	    if (currentScxmlBasedConfigExists(configPath)) {
		// Creation of SCXML based configuration not necessary.
		// Still, we need to set the path to the SCXML based configuration file
		pathToRelevantConfig = getFileForScxmlBasedConfiguration(configPath).getPath();
		System.out.println("Unit test execution uses SCXML based configuration " + pathToRelevantConfig);

	    } else {

		/*
		 * Load original config again (it would be incorrect to use doc1, because it may
		 * have been updated and used as export configuration).
		 */
		Document doc2 = XMLUtil.loadXml(configPath);

		if (hasModelTypeOtherThanSCXML(doc2)) {
		    pathToRelevantConfig = switchModelsToScxml(doc2, configPath);
		    System.out.println("Unit test execution uses SCXML based configuration " + pathToRelevantConfig);
		} else {
		    // Configuration already based only on SCXML. Use original configuration.
		    pathToRelevantConfig = configPath;
		    System.out.println("Unit test execution uses original (already SCXML based) configuration "
			    + pathToRelevantConfig);
		}
	    }

	    return pathToRelevantConfig;
	}
    }

    private boolean currentScxmlBasedConfigExists(String configPath) {

	File configFile = new File(configPath);
	File scxmlBasedConfigFile = getFileForScxmlBasedConfiguration(configPath);

	if (!scxmlBasedConfigFile.exists() || FileUtils.isFileOlder(scxmlBasedConfigFile, configFile)) {
	    return false;
	} else {
	    return true;
	}
    }

    private String switchModelsToScxml(Document configDoc, String configPath) throws Exception {

	XPath xpath = XPathFactory.newInstance().newXPath();

	NodeList modelTypeNodes = (NodeList) xpath.evaluate(
		"//*[(@name = 'inputModelType' or @name = 'referenceModelType') and @value != 'SCXML']", configDoc,
		XPathConstants.NODESET);

	for (int idx = 0; idx < modelTypeNodes.getLength(); idx++) {

	    Node modelTypeNode = modelTypeNodes.item(idx);

	    // set the model type
	    modelTypeNode.getAttributes().getNamedItem("value").setTextContent("SCXML");

	    // now also get the node with the model file path
	    Node modelPathNode = getModelFilePathNode(modelTypeNode);

	    if (modelPathNode == null) {
		System.out.println("Model path node not found!");
	    } else {
		String modelFilePath = modelPathNode.getAttributes().getNamedItem("value").getTextContent();
		String newModelFilePath = getScxmlFilePathForModel(modelFilePath);
		modelPathNode.getAttributes().getNamedItem("value").setTextContent(newModelFilePath);
	    }
	}

	// store updated config

	File updatedConfig = getFileForScxmlBasedConfiguration(configPath);

	XMLUtil.writeXml(configDoc, updatedConfig);
	System.out.println("SCXML based config created: " + updatedConfig.getPath());

	// return path to updated config
	return updatedConfig.getPath();
    }

    private Node getModelFilePathNode(Node modelTypeNode) throws Exception {

	XPath xpath = XPathFactory.newInstance().newXPath();

	Node modelPathNode = (Node) xpath.evaluate(
		"./preceding-sibling::*[@name = 'inputFile' or @name = 'repositoryFileNameOrConnectionString' or @name = 'referenceModelFileNameOrConnectionString']",
		modelTypeNode, XPathConstants.NODE);
	if (modelPathNode == null) {
	    modelPathNode = (Node) xpath.evaluate(
		    "./following-sibling::*[@name = 'inputFile' or @name = 'repositoryFileNameOrConnectionString' or @name = 'referenceModelFileNameOrConnectionString']",
		    modelTypeNode, XPathConstants.NODE);
	}

	return modelPathNode;
    }

    private boolean creationOfScxmlIsNecessary(Document doc, String configPath) throws Exception {

	return !findRelevantNonScxmlModelOccurrences(doc, configPath).isEmpty();
    }

    private File getScxmlFileForModel(String modelFilePath) {
	return new File(getScxmlFilePathForModel(modelFilePath));
    }

    private File getFileForScxmlBasedConfiguration(String configPath) {

	return new File(FilenameUtils.getPath(configPath) + FilenameUtils.getBaseName(configPath)
		+ suffix_configRunWithSCXML + ".xml");

    }

    private String getScxmlFilePathForModel(String modelFilePath) {
	return modelFilePath.subSequence(0, modelFilePath.lastIndexOf(".")).toString() + ".zip";
    }

    private boolean hasModelTypeOtherThanSCXML(Document doc) throws Exception {

	return !findNonScxmlModelOccurrences(doc).isEmpty();
    }

    /**
     * @param configPath
     * @throws Exception
     */
    private void createScxml(Document doc, String configPath) throws Exception {

	// identify all model files that need to be converted to SCXML; for each such
	// file we also need to know the model type

	/*
	 * key: model file path, value: model type
	 */

	SortedMap<String, String> nonScxmlModelMap = findRelevantNonScxmlModelOccurrences(doc, configPath);

	/*
	 * now remove the log, transformation and target elements of the configuration
	 * document
	 */
	NodeList logNodes = doc.getElementsByTagName("log");

	for (int idx = 0; idx < logNodes.getLength(); idx++) {
	    Node ln = logNodes.item(idx);
	    ln.getParentNode().removeChild(ln);
	}

	NodeList transformersNodes = doc.getElementsByTagName("transformers");

	for (int idx = 0; idx < transformersNodes.getLength(); idx++) {
	    Node tn = transformersNodes.item(idx);
	    tn.getParentNode().removeChild(tn);
	}

	NodeList targetsNodes = doc.getElementsByTagName("targets");

	for (int idx = 0; idx < targetsNodes.getLength(); idx++) {
	    Node tn = targetsNodes.item(idx);
	    tn.getParentNode().removeChild(tn);
	}

	// now add the model export target configuration
	Node scConfigNode = doc.getElementsByTagName("ShapeChangeConfiguration").item(0);
	String scNs = scConfigNode.getNamespaceURI();
	String scNsPrefix = scConfigNode.getPrefix();
	Node inputNode = doc.getElementsByTagName("input").item(0);
	Node inputIdAttributeNode = inputNode.getAttributes().getNamedItem("id");
	String inputIdAttribute = inputIdAttributeNode == null ? null : inputIdAttributeNode.getTextContent();

	Element targetsE = doc.createElementNS(scNs, qname(scNsPrefix, "targets"));
	scConfigNode.appendChild(targetsE);

	Element targetE = doc.createElementNS(scNs, qname(scNsPrefix, "Target"));
	targetsE.appendChild(targetE);
	targetE.setAttribute("class", "de.interactive_instruments.ShapeChange.Target.ModelExport.ModelExport");
	targetE.setAttribute("mode", "enabled");
	if (inputIdAttribute != null) {
	    targetE.setAttribute("inputs", inputIdAttribute);
	}

	Element outputDirE = doc.createElementNS(scNs, qname(scNsPrefix, "targetParameter"));
	targetE.appendChild(outputDirE);
	outputDirE.setAttribute("name", "outputDirectory");
	// value will be set per relevant non-scxml model

	Element outputFilenameE = doc.createElementNS(scNs, qname(scNsPrefix, "targetParameter"));
	targetE.appendChild(outputFilenameE);
	outputFilenameE.setAttribute("name", "outputFilename");
	// value will be set per relevant non-scxml model

	Element sortedOutputE = doc.createElementNS(scNs, qname(scNsPrefix, "targetParameter"));
	targetE.appendChild(sortedOutputE);
	sortedOutputE.setAttribute("name", "sortedOutput");
	sortedOutputE.setAttribute("value", "true");

	Element exportProfilesFromWholeModelE = doc.createElementNS(scNs, qname(scNsPrefix, "targetParameter"));
	targetE.appendChild(exportProfilesFromWholeModelE);
	exportProfilesFromWholeModelE.setAttribute("name", "exportProfilesFromWholeModel");
	exportProfilesFromWholeModelE.setAttribute("value", "true");

	Element zipOutputE = doc.createElementNS(scNs, qname(scNsPrefix, "targetParameter"));
	targetE.appendChild(zipOutputE);
	zipOutputE.setAttribute("name", "zipOutput");
	zipOutputE.setAttribute("value", "true");

	Element ignoreTaggedValuesRegexE = doc.createElementNS(scNs, qname(scNsPrefix, "targetParameter"));
	targetE.appendChild(ignoreTaggedValuesRegexE);
	ignoreTaggedValuesRegexE.setAttribute("name", "ignoreTaggedValuesRegex");
	ignoreTaggedValuesRegexE.setAttribute("value", "^$");

	// TBD: maybe profilesInModelSetExplicitly needs to be configured per unit test

	Element defaultEncodingRuleE = doc.createElementNS(scNs, qname(scNsPrefix, "targetParameter"));
	targetE.appendChild(defaultEncodingRuleE);
	defaultEncodingRuleE.setAttribute("name", "defaultEncodingRule");
	defaultEncodingRuleE.setAttribute("value", "export");

	Element rulesE = doc.createElementNS(scNs, qname(scNsPrefix, "rules"));
	targetE.appendChild(rulesE);

	Element encodingRuleE = doc.createElementNS(scNs, qname(scNsPrefix, "EncodingRule"));
	rulesE.appendChild(encodingRuleE);
	encodingRuleE.setAttribute("name", "export");

	Element ruleE = doc.createElementNS(scNs, qname(scNsPrefix, "rule"));
	encodingRuleE.appendChild(ruleE);
	ruleE.setAttribute("name", "rule-exp-pkg-allPackagesAreEditable");

	XPath xpath = XPathFactory.newInstance().newXPath();

	Node inputModelTypeNode = (Node) xpath.evaluate(
		"/*/*[local-name() = 'input']/*[local-name() = 'parameter' and @name = 'inputModelType']", doc,
		XPathConstants.NODE);
	Node inputFileNode = (Node) xpath.evaluate(
		"/*/*[local-name() = 'input']/*[local-name() = 'parameter' and (@name = 'inputFile' or @name = 'repositoryFileNameOrConnectionString')]",
		doc, XPathConstants.NODE);

	// create SCXML for each relevant non-scxml model
	for (Entry<String, String> entry : nonScxmlModelMap.entrySet()) {

	    String modelType = entry.getValue();
	    String modelFilePath = entry.getKey();

	    ((Element) inputModelTypeNode).setAttribute("value", modelType);
	    ((Element) inputFileNode).setAttribute("value", modelFilePath);

	    File exportDirectory = new File(tmpDir, "export" + File.separator + FilenameUtils.getBaseName(configPath));
	    outputDirE.setAttribute("value", exportDirectory.getPath());
	    outputFilenameE.setAttribute("value", FilenameUtils.getBaseName(modelFilePath));

	    // config document in temporäres Verzeichnis speichern

	    File exportConfig = new File(tmpDir,
		    FilenameUtils.getBaseName(configPath) + suffix_configToExportModel + ".xml");

	    XMLUtil.writeXml(doc, exportConfig);

	    System.out.println("Export config created: " + exportConfig.getPath());

	    // execute export config
	    System.out.println("Running export config ...");
	    @SuppressWarnings("unused")
	    TestInstance test = new TestInstance(exportConfig.getPath());
	    System.out.println("... export done");

	    // get SCXML file that was produced
	    List<Path> scxmlFilePaths = null;
	    try (Stream<Path> files = Files.walk(Paths.get(exportDirectory.toURI()))) {
		scxmlFilePaths = files.filter(
			f -> f.getFileName().toString().equals(FilenameUtils.getBaseName(modelFilePath) + ".zip"))
			.collect(Collectors.toList());

	    }

	    File scxmlFile = scxmlFilePaths.get(0).toFile();
	    FileUtils.copyFile(scxmlFile,
		    new File(FilenameUtils.getPath(modelFilePath) + FilenameUtils.getBaseName(modelFilePath) + ".zip"));
	}

    }

    private String qname(String prefix, String name) {
	return prefix == null ? name : prefix + ":" + name;
    }

    /**
     * @param configDoc
     * @return can be empty but not <code>null</code>
     * @throws Exception
     */
    private SortedMap<String, String> findRelevantNonScxmlModelOccurrences(Document configDoc, String configPath)
	    throws Exception {

	SortedMap<String, String> result = new TreeMap<>();

	SortedMap<String, String> modelTypeByFilePath = findNonScxmlModelOccurrences(configDoc);

	/*
	 * Now determine which of these models has an SCXML file that is younger than
	 * both the original model AND the ShapeChange configuration (because the
	 * configuration influences the way that a model is loaded, and thus how the
	 * exported SCXML looks like).
	 */

	File configFile = new File(configPath);

	for (Entry<String, String> e : modelTypeByFilePath.entrySet()) {

	    String modelFilePath = e.getKey();
	    String modelType = e.getValue();

	    File modelFile = new File(modelFilePath);
	    File scxmlFile = getScxmlFileForModel(modelFilePath);

	    if (!scxmlFile.exists() || FileUtils.isFileOlder(scxmlFile, modelFile)
		    || FileUtils.isFileOlder(scxmlFile, configFile)) {
		result.put(modelFilePath, modelType);
	    }
	}

	return result;
    }

    /**
     * @param configDoc
     * @return can be empty but not <code>null</code>
     * @throws Exception
     */
    private SortedMap<String, String> findNonScxmlModelOccurrences(Document configDoc) throws Exception {

	SortedMap<String, String> modelTypeByFilePath = new TreeMap<>();

	XPath xpath = XPathFactory.newInstance().newXPath();

	NodeList modelTypeNodes = (NodeList) xpath.evaluate(
		"//*[(@name = 'inputModelType' or @name = 'referenceModelType') and @value != 'SCXML']", configDoc,
		XPathConstants.NODESET);

	for (int idx = 0; idx < modelTypeNodes.getLength(); idx++) {

	    Node modelTypeNode = modelTypeNodes.item(idx);

	    // identify the model type
	    String modelType = modelTypeNode.getAttributes().getNamedItem("value").getTextContent();

	    // now also get the model file path
	    Node modelPathNode = (Node) xpath.evaluate(
		    "./preceding-sibling::*[@name = 'inputFile' or @name = 'repositoryFileNameOrConnectionString' or @name = 'referenceModelFileNameOrConnectionString']",
		    modelTypeNode, XPathConstants.NODE);
	    if (modelPathNode == null) {
		modelPathNode = (Node) xpath.evaluate(
			"./following-sibling::*[@name = 'inputFile' or @name = 'repositoryFileNameOrConnectionString' or @name = 'referenceModelFileNameOrConnectionString']",
			modelTypeNode, XPathConstants.NODE);
	    }

	    if (modelPathNode == null) {
		System.out.println("Model path node not found!");
	    } else {
		String modelFilePath = modelPathNode.getAttributes().getNamedItem("value").getTextContent();
		modelTypeByFilePath.put(modelFilePath, modelType);
	    }
	}

	return modelTypeByFilePath;
    }

}
