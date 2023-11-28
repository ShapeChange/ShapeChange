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
package de.interactive_instruments.ShapeChange.Util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.interactive_instruments.ShapeChange.ShapeChangeErrorHandler;
import de.interactive_instruments.ShapeChange.ShapeChangeException;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class XMLUtil {

    /**
     * @param parent      the element in which to look for the first child element
     *                    with given name
     * @param elementName name of the child element to look up
     * @return the first child element with given name; can be <code>null</code> if
     *         no such element was found
     */
    public static Element getFirstElement(Element parent, String elementName) {

	NodeList nl = parent.getElementsByTagName(elementName);

	if (nl != null && nl.getLength() != 0) {

	    for (int k = 0; k < nl.getLength(); k++) {

		Node n = nl.item(k);
		if (n.getNodeType() == Node.ELEMENT_NODE) {

		    return (Element) n;
		}
	    }
	}

	return null;
    }

    /**
     * @param parent      the element in which to look for the first child element
     *                    with given name
     * @param elementName name of the child element to look up
     * @return the text content of the first child element with given name; can be
     *         <code>null</code> if no such element was found
     */
    public static String getTextContentOfFirstElement(Element parent, String elementName) {
	Element e = getFirstElement(parent, elementName);
	return e != null ? e.getTextContent() : null;
    }

    /**
     * @param parent      the element in which to look for the first child element
     *                    with given name
     * @param elementName name of the child element to look up
     * @return the text content, trimmed, of the first child element with given
     *         name; can be <code>null</code> if no such element was found
     */
    public static String getTrimmedTextContentOfFirstElement(Element parent, String elementName) {
	return StringUtils.trim(getTextContentOfFirstElement(parent, elementName));
    }

    /**
     * @param parentElement Element in which to look up the children with given name
     * @param elementName   name of child elements to look up
     * @return List of child elements of the given parent element that have the
     *         given element name. Can be empty but not <code>null</code>.
     */
    public static List<Element> getChildElements(Element parentElement, String elementName) {

	List<Element> result = new ArrayList<Element>();

	NodeList nl = parentElement.getElementsByTagName(elementName);

	if (nl != null && nl.getLength() != 0) {
	    for (int k = 0; k < nl.getLength(); k++) {

		Node n = nl.item(k);

		if (n.getNodeType() == Node.ELEMENT_NODE) {

		    result.add((Element) n);
		}
	    }
	}

	return result;
    }

    /**
     * @param parentElement Element in which to look up the children with given name
     * @param elementName   name of child elements to look up
     * @return List of text contents of the child elements of the given parent
     *         element that have the given element name. Can be empty but not
     *         <code>null</code>.
     */
    public static List<String> getTextContentOfChildElements(Element parentElement, String elementName) {

	List<String> result = new ArrayList<>();

	List<Element> elements = getChildElements(parentElement, elementName);

	for (Element e : elements) {
	    result.add(e.getTextContent());
	}

	return result;
    }

    /**
     * @param s tbd
     * @return <code>true</code> if the given string equals '1' or equals, ignoring
     *         case, 'true'; else <code>false</code>
     */
    public static boolean parseBoolean(String s) {

	if (StringUtils.isBlank(s)) {
	    return false;
	} else if (s.trim().equalsIgnoreCase("true") || s.trim().equals("1")) {
	    return true;
	} else {
	    return false;
	}
    }

    public static Document loadXml(String xmlPath) throws Exception {

	InputStream xmlStream = null;

	File file = new File(xmlPath);
	if (file == null || !file.exists()) {
	    try {
		xmlStream = (new URL(xmlPath)).openStream();
	    } catch (MalformedURLException e) {
		throw new Exception("No XML file found at " + xmlPath + " (malformed URL)");
	    } catch (IOException e) {
		throw new Exception("No XML file found at " + xmlPath + " (IO exception)");
	    }
	} else {
	    try {
		xmlStream = new FileInputStream(file);
	    } catch (FileNotFoundException e) {
		throw new Exception("No XML file found at " + xmlPath);
	    }
	}
	if (xmlStream == null) {
	    throw new Exception("No XML file found at " + xmlPath);
	}

	DocumentBuilder builder = null;
	ShapeChangeErrorHandler handler = null;
	try {
	    System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
		    "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    factory.setNamespaceAware(true);
	    factory.setValidating(true);
	    factory.setFeature("http://apache.org/xml/features/validation/schema", true);
	    factory.setIgnoringElementContentWhitespace(true);
	    factory.setIgnoringComments(true);
	    factory.setXIncludeAware(true);
	    factory.setFeature("http://apache.org/xml/features/xinclude/fixup-base-uris", false);
	    builder = factory.newDocumentBuilder();
	    handler = new ShapeChangeErrorHandler();
	    builder.setErrorHandler(handler);
	} catch (FactoryConfigurationError e) {
	    throw new Exception("Unable to get a document builder factory.");
	} catch (ParserConfigurationException e) {
	    throw new Exception("XML Parser was unable to be configured.");
	}

	try {

	    Document document = builder.parse(xmlStream);
	    if (handler.errorsFound()) {
		throw new Exception("Invalid XML file.");
	    } else {
		return document;
	    }

	} catch (SAXException e) {
	    String m = e.getMessage();
	    if (m != null) {
		throw new Exception("Error while loading XML file: " + m);
	    } else {
		e.printStackTrace(System.err);
		throw new Exception("Error while loading XML file");
	    }
	} catch (IOException e) {
	    String m = e.getMessage();
	    if (m != null) {
		throw new Exception("Error while loading XML file: " + m);
	    } else {
		e.printStackTrace(System.err);
		throw new Exception("Error while loading XML file");
	    }
	}
    }

    /**
     * Writes the document to the destination, with XML declaration
     * 
     * @param doc         tbd
     * @param destination tbd
     * @throws ShapeChangeException tbd
     */
    public static void writeXml(Document doc, File destination) throws ShapeChangeException {
	writeXml(doc, destination, false);
    }

    /**
     * Writes the document to the destination
     * 
     * @param doc                tbd
     * @param destination        tbd
     * @param omitXmlDeclaration <code>true</code>, if the XML declaration shall be
     *                           omitted, <code>false</code> if it shall be created
     * @throws ShapeChangeException tbd
     */
    public static void writeXml(Document doc, File destination, boolean omitXmlDeclaration)
	    throws ShapeChangeException {

	File parentFile = destination.getParentFile();
	if (parentFile != null && !parentFile.exists()) {
	    try {
		FileUtils.forceMkdir(parentFile);
	    } catch (IOException e) {
		throw new ShapeChangeException("Error while creating folder structure for XML file (for destination: "
			+ destination + "). Exception message is: " + e.getMessage(), e);
	    }
	}

	try (BufferedWriter writer = new BufferedWriter(
		new OutputStreamWriter(new FileOutputStream(destination), "UTF-8"))) {

	    TransformerFactory transformerFactory = TransformerFactory.newInstance();

	    Transformer transformer = transformerFactory.newTransformer();
	    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
	    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	    if (omitXmlDeclaration) {
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	    }
	    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

	    DOMSource source = new DOMSource(doc);

	    StreamResult result = new StreamResult(writer);

	    transformer.transform(source, result);

	} catch (IOException | TransformerException e) {
	    throw new ShapeChangeException("Error while writing XML file. Exception message is: " + e.getMessage(), e);
	}
    }

    /**
     * @param nl must not be null
     * @return a list of element nodes contained in the given node list; can be
     *         empty but not <code>null</code>
     */
    public static List<Element> getElementNodes(NodeList nl) {

	List<Element> result = new ArrayList<Element>();

	if (nl != null && nl.getLength() != 0) {
	    for (int k = 0; k < nl.getLength(); k++) {
		Node n = nl.item(k);
		if (n.getNodeType() == Node.ELEMENT_NODE) {
		    result.add((Element) n);
		}
	    }
	}

	return result;
    }

    /**
     * Adds an attribute to the given element.
     * 
     * @param document The document to which the element belongs
     * @param e        The element to which the attribute shall be added
     * @param attName  name of the new attribute
     * @param attValue value of the new attribute
     */
    public static void addAttribute(Document document, Element e, String attName, String attValue) {
	Attr att = document.createAttribute(attName);
	att.setValue(attValue);
	e.setAttributeNode(att);
    }
}
