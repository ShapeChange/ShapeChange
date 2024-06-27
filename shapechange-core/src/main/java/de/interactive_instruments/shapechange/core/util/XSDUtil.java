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
 * (c) 2002-2024 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core.util;

import java.io.InputStream;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;

import de.interactive_instruments.shapechange.core.ShapeChangeErrorHandler;

/**
 * Provides utility methods for performing XML Schema validation.
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class XSDUtil {

    /**
     * Performs XSD 1.1 validation of the XML document at the given path.
     * <p>
     * NOTE: XInclude statements in the XML document that is being validated are NOT
     * resolved! However, locator information should be available.
     * <p>
     * For further details, see {@link #validate(Source, boolean)}.
     * 
     * @param xmlPath the location of the XML document; either the path to a local
     *                file, or the URL of an HTTP resource
     * @throws Exception If an exception occurred, or an error was detected, while
     *                   validating the XML document.
     */
    public static void validate(String xmlPath) throws Exception {

	InputStream xmlStream = XMLUtil.inputStreamFromXml(xmlPath);

	if (xmlStream == null) {
	    throw new Exception("No XML file found at " + xmlPath);
	}

	validate(xmlStream);
    }

    /**
     * Performs XSD 1.1 validation on the given XML stream (created, for example,
     * using {@link XMLUtil#inputStreamFromXml(String)}).
     * <p>
     * NOTE: XInclude statements in the XML document that is being validated are NOT
     * resolved! However, locator information should be available.
     * <p>
     * For further details, see {@link #validate(Source, boolean)}.
     * 
     * @param xmlStream Input stream with content of an XML document.
     * @throws Exception If a validation error was detected. Note that validation
     *                   errors are logged. They are not part of the exception.
     */
    public static void validate(InputStream xmlStream) throws Exception {

	/*
	 * 2024-06-11 JE: We want locator infos (line and column numbers) in validation
	 * messages. A DOM source does not give such information (at least not by
	 * default; workarounds can be found on the web). Thus, we use a StreamSource
	 * for reading the input stream. A SAX source also worked during tests.
	 */
//	Source source = new SAXSource(new InputSource(xmlStream));
	Source source = new StreamSource(xmlStream);

	validate(source, false);
    }

    /**
     * Performs XSD 1.1 validation of the XML document at the given path. For
     * further details, see {@link #validate(Source, boolean)}.
     * <p>
     * NOTE: XInclude statements in the XML document that is being validated MAY
     * have been resolved! That depends on how the DOM document was created. See
     * parameter xincludesResolved. Locator information is typically not available.
     * <p>
     * 
     * @param domDocument       The XML document to be validated, as a DOM document.
     * @param xincludesResolved <code>true</code>, if XInclude statements have been
     *                          resolved in the given DOM document, else
     *                          <code>false</code>.
     * @throws Exception If a validation error was detected. Note that validation
     *                   errors are logged. They are not part of the exception.
     */
    public static void validate(Document domDocument, boolean xincludesResolved) throws Exception {

	// NOTE: Will (typically) not have locator infos (line and column numbers)!
	Source source = new DOMSource(domDocument);

	validate(source, xincludesResolved);
    }

    /**
     * Performs XSD 1.1 validation on the given XML source. The schema file(s)
     * identified using the xsi:schemaLocation attribute within the source is used
     * for validation.
     * <p>
     * Whether or not locator information is available in validation messages
     * depends upon the type of source (a DOM source typically does not have locator
     * information, while SAX or stream source do).
     * <p>
     * Further details:
     * 
     * <ul>
     * <li>http://apache.org/xml/features/validation/schema: true</li>
     * <li>http://apache.org/xml/features/validation/schema-full-checking: true</li>
     * </ul>
     * 
     * @param xmlSource         The XML document to be validated.
     * @param xincludesResolved <code>true</code>, if XInclude statements have been
     *                          resolved in the given source, else
     *                          <code>false</code>.
     * @throws Exception If a validation error was detected. Note that validation
     *                   errors are logged. They are not part of the exception.
     */
    public static void validate(Source xmlSource, boolean xincludesResolved) throws Exception {

	ShapeChangeErrorHandler handler = new ShapeChangeErrorHandler();

	System.setProperty("javax.xml.validation.SchemaFactory:http://www.w3.org/XML/XMLSchema/v1.1",
		"org.apache.xerces.jaxp.validation.XMLSchema11Factory");

	SchemaFactory sf = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
	Schema schema = sf.newSchema();

	Validator v = schema.newValidator();
	v.setErrorHandler(handler);
	v.setFeature("http://apache.org/xml/features/validation/schema", true);
	v.setFeature("http://apache.org/xml/features/validation/schema-full-checking", true);

	v.validate(xmlSource);

	if (handler.errorsFound()) {
	    // error messages have already been logged by the handler
	    throw new Exception("Invalid XML file. NOTE: XInclude statements have " + (xincludesResolved ? "" : "not ")
		    + "been resolved before validation.");
	}
    }
}
