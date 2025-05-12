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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Optional;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import de.interactive_instruments.shapechange.core.ShapeChangeErrorHandler;
import shadow.org.apache.commons.lang3.StringUtils;

/**
 * Provides utility methods for performing XML Schema validation.
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class XSDUtil {

    /**
     * Performs XSD 1.1 validation of the XML document at the given path. Validation
     * is performed using the schema file(s) defined defined by parameter
     * {@code xsdLocationOpt}.
     * <p>
     * Validation messages are printed via the given error handler (see
     * {@code handlerOpt}). In order to determine if validation failed, check the
     * handler for any errors after the validation process has been completed (see
     * {@link ShapeChangeErrorHandler#errorsFound()}).
     * <p>
     * NOTE: XInclude statements in the XML document that is being validated are NOT
     * resolved! However, locator information should be available.
     * <p>
     * For further details, see
     * {@link #validate(Source, boolean, ShapeChangeErrorHandler, Optional)}.
     * 
     * @param xmlPath        the location of the XML document; either the path to a
     *                       local file, or the URL of an HTTP resource
     * @param handler        The object to handle error messages. Check this object
     *                       to determine if any validation errors were detected.
     * @param xsdLocationOpt The location (either an HTTP-URI or a file pathname) of
     *                       the XML Schema to use for validation, if present.
     *                       Otherwise (i.e., the Optional is empty), the schema
     *                       file(s) identified using the xsi:schemaLocation
     *                       attribute within the source is used for validation.
     * @throws ValidationException If an exception occurred during the validation.
     */
    public static void validate(String xmlPath, ShapeChangeErrorHandler handler, Optional<String> xsdLocationOpt)
	    throws ValidationException {

	InputStream xmlStream;
	try {
	    xmlStream = XMLUtil.inputStreamFromXml(xmlPath);
	} catch (XmlHandlingException e) {
	    throw new ValidationException("Validation failed.",e);
	}

	if (xmlStream == null) {
	    throw new ValidationException("No XML file found at " + xmlPath);
	}

	validate(xmlStream, xmlPath, handler, xsdLocationOpt);
    }

    /**
     * Performs XSD 1.1 validation on the given XML stream (created, for example,
     * using {@link XMLUtil#inputStreamFromXml(String)}). Validation is performed
     * using the schema file(s) defined defined by parameter {@code xsdLocationOpt}.
     * <p>
     * Validation messages are printed via the given error handler (see
     * {@code handlerOpt}). In order to determine if validation failed, check the
     * handler for any errors after the validation process has been completed (see
     * {@link ShapeChangeErrorHandler#errorsFound()}).
     * <p>
     * NOTE: XInclude statements in the XML document that is being validated are NOT
     * resolved! However, locator information should be available.
     * <p>
     * For further details, see
     * {@link #validate(Source, boolean, ShapeChangeErrorHandler, Optional)}.
     * 
     * @param xmlStream      Input stream with content of an XML document.
     * @param systemId       The system identifier for the source from which the XML
     *                       stream was loaded. It is optional (so can be
     *                       <code>null</code>).The application can use a system
     *                       identifier, for example, to resolve relative URIs and
     *                       paths (like schema paths) and to include in error
     *                       messages and warnings.
     * @param handler        The object to handle error messages. Check this object
     *                       to determine if any validation errors were detected.
     * @param xsdLocationOpt The location (either an HTTP-URI or a file pathname) of
     *                       the XML Schema to use for validation, if present.
     *                       Otherwise (i.e., the Optional is empty), the schema
     *                       file(s) identified using the xsi:schemaLocation
     *                       attribute within the source is used for validation.
     * @throws ValidationException If an exception occurred during the validation.
     */
    public static void validate(InputStream xmlStream, String systemId, ShapeChangeErrorHandler handler,
	    Optional<String> xsdLocationOpt) throws ValidationException {

	/*
	 * 2024-06-11 JE: We want locator infos (line and column numbers) in validation
	 * messages. A DOM source does not give such information (at least not by
	 * default; workarounds can be found on the web). Thus, we use a StreamSource
	 * for reading the input stream. A SAX source also worked during tests.
	 */
//	Source source = new SAXSource(new InputSource(xmlStream));
	Source source = new StreamSource(xmlStream);
	if (StringUtils.isNotBlank(systemId)) {
	    source.setSystemId(systemId);
	}

	validate(source, false, handler, xsdLocationOpt);
    }

    /**
     * Performs XSD 1.1 validation of the XML document at the given path. For
     * further details, see
     * {@link #validate(Source, boolean, ShapeChangeErrorHandler, Optional)}.
     * Validation is performed using the schema file(s) defined defined by parameter
     * {@code xsdLocationOpt}.
     * <p>
     * Validation messages are printed via the given error handler (see
     * {@code handlerOpt}). In order to determine if validation failed, check the
     * handler for any errors after the validation process has been completed (see
     * {@link ShapeChangeErrorHandler#errorsFound()}).
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
     * @param systemId          The system identifier for the document. It is
     *                          optional (so can be <code>null</code>).The
     *                          application can use a system identifier, for
     *                          example, to resolve relative URIs and paths (like
     *                          schema paths) and to include in error messages and
     *                          warnings.
     * @param handler           The object to handle error messages. Check this
     *                          object to determine if any validation errors were
     *                          detected.
     * @param xsdLocationOpt    The location (either an HTTP-URI or a file pathname)
     *                          of the XML Schema to use for validation, if present.
     *                          Otherwise (i.e., the Optional is empty), the schema
     *                          file(s) identified using the xsi:schemaLocation
     *                          attribute within the source is used for validation.
     * @throws ValidationException If an exception occurred during the validation.
     */
    public static void validate(Document domDocument, boolean xincludesResolved, String systemId,
	    ShapeChangeErrorHandler handler, Optional<String> xsdLocationOpt) throws ValidationException {

	// NOTE: Will (typically) not have locator infos (line and column numbers)!
	Source source = new DOMSource(domDocument);
	if (StringUtils.isNotBlank(systemId)) {
	    source.setSystemId(systemId);
	}
	validate(source, xincludesResolved, handler, xsdLocationOpt);
    }

    /**
     * Performs XSD 1.1 validation on the given XML source. Validation is performed
     * using the schema file(s) defined defined by parameter {@code xsdLocationOpt}.
     * <p>
     * Validation messages are printed via the given error handler (see
     * {@code handler}). In order to determine if validation failed, check the
     * handler for any errors after the validation process has been completed (see
     * {@link ShapeChangeErrorHandler#errorsFound()}).
     * <p>
     * The systemId needs to be defined on the source, in order for validation on
     * XML schemas with relative location to succeed!
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
     * @param handler           The object to handle error messages. Check this
     *                          object to determine if any validation errors were
     *                          detected.
     * @param xsdLocationOpt    The location (either an HTTP-URI or a file pathname)
     *                          of the XML Schema to use for validation, if present.
     *                          Otherwise (i.e., the Optional is empty), the schema
     *                          file(s) identified using the xsi:schemaLocation
     *                          attribute within the source is used for validation.
     * @throws ValidationException If an exception occurred during the validation.
     */
    public static void validate(Source xmlSource, boolean xincludesResolved, ShapeChangeErrorHandler handler,
	    Optional<String> xsdLocationOpt) throws ValidationException {

	System.setProperty("javax.xml.validation.SchemaFactory:http://www.w3.org/XML/XMLSchema/v1.1",
		"org.apache.xerces.jaxp.validation.XMLSchema11Factory");

	SchemaFactory sf = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");

	Schema schema = null;

	if (xsdLocationOpt.isPresent()) {

	    String xsdLocation = xsdLocationOpt.get();

	    try {
		if (xsdLocation.startsWith("http")) {
		    schema = sf.newSchema(URI.create(xsdLocation).toURL());
		} else {
		    File schemaFile = new File(xsdLocation);
		    schema = sf.newSchema(schemaFile);
		}
	    } catch (SAXException e) {
		throw new ValidationException("Schema could not be created from XSD location '" + xsdLocation + "'.",
			e);
	    } catch (MalformedURLException e) {
		throw new ValidationException("XSD location URL '" + xsdLocation + "' is malformed.", e);
	    }

	} else {
	    try {
		schema = sf.newSchema();
	    } catch (SAXException e) {
		throw new ValidationException("Exception occurred while creating schema object.", e);
	    }
	}

	if (schema != null) {
	    try {
		Validator v = schema.newValidator();
		v.setErrorHandler(handler);
		v.setFeature("http://apache.org/xml/features/validation/schema", true);
		v.setFeature("http://apache.org/xml/features/validation/schema-full-checking", true);

		v.validate(xmlSource);

		if (handler.errorsFound()) {
		    handler.addMessage("NOTE: XInclude statements have " + (xincludesResolved ? "" : "not ")
			    + "been resolved before validation.");
		}
	    } catch (SAXException | IOException e) {
		throw new ValidationException("Exception occurred during XML Schema validation.", e);
	    }
	}
    }
}
