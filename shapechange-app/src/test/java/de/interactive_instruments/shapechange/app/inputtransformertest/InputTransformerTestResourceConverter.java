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
 * (c) 2002-2026 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.app.inputtransformertest;

import java.io.File;
import java.util.Optional;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.interactive_instruments.shapechange.core.ShapeChangeErrorHandler;
import de.interactive_instruments.shapechange.core.util.XMLUtil;
import de.interactive_instruments.shapechange.core.util.XSDUtil;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class InputTransformerTestResourceConverter {

    /**
     * Name of the VM argument / system property to indicate that the ShapeChange
     * configuration of EaInputTransformation-tagged unit tests shall be updated (by
     * setting the value of the system property to 'true') based upon the original
     * model and configuration files - or created, if they do not exist yet.
     */
    public static final String UPDATE_OR_CREATE_EAINPUTTRANFORMATION_RESOURCES_SYSTEM_PROPERTY_NAME = "updateOrCreateEaInputTransformationResources";

    String suffix_copiedInputModel = "_copiedModel";
    String suffix_configRunWithModelCopy = "_runWithModelCopy";

    public String updateEaInputTransformationTestResources(String configPath) throws Exception {

	// validate config, as gatekeeper action:
	// 1. validate original config
	ShapeChangeErrorHandler handler1 = new ShapeChangeErrorHandler();
	XSDUtil.validate(configPath, handler1, Optional.empty());

	if (!handler1.errorsFound()) {

	    // 2. validate config, with xincludes resolved
	    ShapeChangeErrorHandler handler2 = new ShapeChangeErrorHandler();
	    XSDUtil.validate(XMLUtil.loadXml(configPath), true, configPath, handler2, Optional.empty());

	    if (!handler2.errorsFound()) {

		// load original config

		Document doc1 = XMLUtil.loadXml(configPath);

		Pair<String, String> inputModelInfos = getInputModelInfos(doc1);

		if (!inputModelInfos.getLeft().equalsIgnoreCase("EA7")) {
		    throw new Exception(
			    "Input model type for the input transformation is not EA7. This is not supported.");
		}

		// create input model copy
		File inputModelCopyFile = createInputModelCopy(doc1, inputModelInfos.getRight());

		if ("true".equalsIgnoreCase(
			System.getProperty(UPDATE_OR_CREATE_EAINPUTTRANFORMATION_RESOURCES_SYSTEM_PROPERTY_NAME))) {

		    /*
		     * Load original config again (it would be incorrect to use doc1, because it may
		     * have been updated and used as export configuration).
		     */
		    Document doc2 = XMLUtil.loadXml(configPath);
		    switchInputModelToCopy(doc2, configPath, inputModelCopyFile);
		}

		String pathToRelevantConfig = getFileForUpdatedConfiguration(configPath).getPath();
		System.out.println("Unit test execution uses model copy based configuration " + pathToRelevantConfig);

		return pathToRelevantConfig;

	    } else {
		throw new Exception("Validation of ShapeChange configuration failed.");
	    }
	} else {
	    throw new Exception("Validation of ShapeChange configuration failed.");
	}
    }

    private void switchInputModelToCopy(Document configDoc, String configPath, File inputModelCopyFile)
	    throws Exception {

	XPath xpath = XPathFactory.newInstance().newXPath();

	NodeList inputModelFileNodes = (NodeList) xpath.evaluate(
		"//*[local-name() = 'input']/*[local-name() = 'parameter'][@name = 'inputFile' or @name = 'repositoryFileNameOrConnectionString']",
		configDoc, XPathConstants.NODESET);

	Node inputModelFileNode = inputModelFileNodes.item(0);

	// set the model file
	inputModelFileNode.getAttributes().getNamedItem("value").setTextContent(inputModelCopyFile.getPath());

	// store updated config
	File updatedConfig = getFileForUpdatedConfiguration(configPath);

	XMLUtil.writeXml(configDoc, updatedConfig);
	System.out.println("Input model copy based config created: " + updatedConfig.getPath());
    }

    private File getFileForUpdatedConfiguration(String configPath) {
	return new File(FilenameUtils.getPath(configPath) + FilenameUtils.getBaseName(configPath)
		+ suffix_configRunWithModelCopy + ".xml");
    }

    /**
     * @param inputModelFilePath
     * @throws Exception
     * @return file of input model copy
     */
    private File createInputModelCopy(Document configDoc, String inputModelFilePath) throws Exception {

	// copy the input model to the folder in which the log file shall be created

	XPath xpath = XPathFactory.newInstance().newXPath();

	NodeList logFileNodes = (NodeList) xpath.evaluate(
		"//*[local-name() = 'log']/*[local-name() = 'parameter'][(@name = 'logFile')]", configDoc,
		XPathConstants.NODESET);

	Node logFileNode = logFileNodes.item(0);
	String logFilePath = logFileNode.getAttributes().getNamedItem("value").getTextContent();

	File inputFile = new File(inputModelFilePath);
	File copy = new File(logFilePath.subSequence(0, logFilePath.lastIndexOf("/")).toString() + "/"
		+ FilenameUtils.getBaseName(inputModelFilePath) + suffix_copiedInputModel + "."
		+ FilenameUtils.getExtension(inputModelFilePath));

	FileUtils.copyFile(inputFile, copy);

	return copy;
    }

    /**
     * @param configDoc
     * @return pair with input model type as left value and input model file path as
     *         right value; can be empty but not <code>null</code>
     * @throws Exception
     */
    private Pair<String, String> getInputModelInfos(Document configDoc) throws Exception {

	XPath xpath = XPathFactory.newInstance().newXPath();

	NodeList inputModelTypeNodes = (NodeList) xpath.evaluate(
		"//*[local-name() = 'input']/*[local-name() = 'parameter'][(@name = 'inputModelType' or @name = 'referenceModelType')]",
		configDoc, XPathConstants.NODESET);

	String inputModelType = null;
	String inputModelFilePath = null;

	for (int idx = 0; idx < inputModelTypeNodes.getLength(); idx++) {

	    Node inputModelTypeNode = inputModelTypeNodes.item(idx);

	    // identify the model type
	    inputModelType = inputModelTypeNode.getAttributes().getNamedItem("value").getTextContent();

	    // now also get the input model file path
	    Node inputModelPathNode = (Node) xpath.evaluate(
		    "//*[local-name() = 'input']/*[local-name() = 'parameter'][@name = 'inputFile' or @name = 'repositoryFileNameOrConnectionString']",
		    inputModelTypeNode, XPathConstants.NODE);

	    if (inputModelPathNode == null) {
		System.out.println("Input model path node not found!");
	    } else {
		inputModelFilePath = inputModelPathNode.getAttributes().getNamedItem("value").getTextContent();
	    }
	}

	return ImmutablePair.of(inputModelType, inputModelFilePath);
    }

}
