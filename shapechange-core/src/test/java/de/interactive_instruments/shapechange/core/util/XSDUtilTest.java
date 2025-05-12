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
 * (c) 2002-2025 interactive instruments GmbH, Bonn, Germany
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

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.interactive_instruments.shapechange.core.ShapeChangeErrorHandler;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
class XSDUtilTest {

    @TempDir
    Path tempDir;

    String scxmlXsdLocation = "src/main/resources/schema/ShapeChangeExportedModel.xsd";
    String scXsdLocation = "src/main/resources/schema/ShapeChangeConfiguration.xsd";

    @Test
    void testValidateScxmlWithValidXml() throws Exception {

	// Arrange
	String xml = "src/test/resources/xsdutiltest/scxml_valid.xml";
	File xmlFile = new File(xml);
	URI u = xmlFile.toURI();
	StringWriter sw = new StringWriter();
	ShapeChangeErrorHandler handler = new ShapeChangeErrorHandler(new PrintWriter(sw));

	// Act & Assert
	XSDUtil.validate(u.toString(), handler, Optional.of(scxmlXsdLocation));

	assert !handler.errorsFound();

	sw.flush();
	String validationMessages = sw.toString();
	assert validationMessages.isEmpty();
    }

    @Test
    void testValidateScxmlWithInvalidXml() throws Exception {

	// Arrange
	String xml = "src/test/resources/xsdutiltest/scxml_invalid.xml";
	File xmlFile = new File(xml);
	URI u = xmlFile.toURI();
	StringWriter sw = new StringWriter();
	ShapeChangeErrorHandler handler = new ShapeChangeErrorHandler(new PrintWriter(sw));

	// Act & Assert
	XSDUtil.validate(u.toString(), handler, Optional.of(scxmlXsdLocation));

	assert handler.errorsFound();

	sw.flush();
	String validationMessages = sw.toString();

	assert validationMessages.contains(
		"Error (19:32) cvc-assertion-failure-mesg: Assertion failed for schema type '#AnonType_stereotypes'. Within a collection of stereotypes, all stereotype names shall be unique (ignoring case).");
	assert validationMessages.contains(
		"Error (74:36) cvc-assertion-failure-mesg: Assertion failed for schema type '#AnonType_stereotypes'. Within a collection of stereotypes, all stereotype names shall be unique (ignoring case).");
	assert validationMessages.contains(
		"Error (128:35) cvc-assertion-failure-mesg: Assertion failed for schema type 'PropertyType'. If a property is an attribute, then it shall be navigable.");
	assert validationMessages.contains(
		"Error (130:28) cvc-assertion-failure-mesg: Assertion failed for schema type 'ClassType'. All properties of a class shall be navigable.");
	assert validationMessages.contains(
		"Error (672:40) cvc-assertion-failure-mesg: Assertion failed for schema type '#AnonType_stereotypes'. Within a collection of stereotypes, all stereotype names shall be unique (ignoring case).");
	assert validationMessages.contains(
		"Error (686:35) cvc-assertion-failure-mesg: Assertion failed for schema type 'PropertyType'. sc:associationId shall only be used on association roles.");
	assert validationMessages.contains(
		"Error (707:35) cvc-assertion-failure-mesg: Assertion failed for schema type 'PropertyType'. Qualifiers can only be defined for an association role.");
	assert validationMessages.contains(
		"Error (726:35) cvc-assertion-failure-mesg: Assertion failed for schema type 'PropertyType'. An initial value can only be defined for an attribute.");
	assert validationMessages.contains(
		"Error (874:28) cvc-assertion-failure-mesg: Assertion failed for schema type 'ClassType'. sc:inClassId shall not be defined for properties encoded within a Class.");
	assert validationMessages.contains(
		"Error (951:22) cvc-assertion-failure-mesg: Assertion failed for schema type 'AssociationType'. For association end1, either @ref or sc:Property shall be present.");
	assert validationMessages.contains(
		"Error (951:22) cvc-assertion-failure-mesg: Assertion failed for schema type 'AssociationType'. For association end2, either @ref or sc:Property shall be present.");
	assert validationMessages.contains(
		"Error (995:22) cvc-assertion-failure-mesg: Assertion failed for schema type 'AssociationType'. An sc:Property element that is directly encoded within an association - here: sc:end1/sc:Property - shall not be navigable.");
	assert validationMessages.contains(
		"Error (995:22) cvc-assertion-failure-mesg: Assertion failed for schema type 'AssociationType'. An sc:Property element that is directly encoded within an association - here: sc:end2/sc:Property - shall not be navigable.");
	assert validationMessages.contains(
		"Error (995:22) cvc-assertion-failure-mesg: Assertion failed for schema type 'AssociationType'. sc:inClassId shall be defined for association roles - here: sc:end1/sc:Property - encoded within an Association.");
	assert validationMessages.contains(
		"Error (995:22) cvc-assertion-failure-mesg: Assertion failed for schema type 'AssociationType'. sc:inClassId shall be defined for association roles - here: sc:end2/sc:Property - encoded within an Association.");
	assert validationMessages.contains(
		"Error (997:12) cvc-identity-constraint.4.3: Key 'propertyToAssociationKeyRef' with value 'test' not found for identity constraint of element 'Model'.");
	assert validationMessages.contains(
		"Error (997:12) cvc-identity-constraint.4.3: Key 'propertyInClassKeyRef' with value 'test' not found for identity constraint of element 'Model'.");
	assert validationMessages.contains(
		"Error (997:12) cvc-identity-constraint.4.3: Key 'propertyTypeKeyref' with value 'xyz' not found for identity constraint of element 'Model'.");
    }

    @Test
    void testValidateWithNonExistentFile() {

	// Arrange
	String xml = "src/test/resources/xsdutiltest/nonexistent.xml";
	File xmlFile = new File(xml);
	URI u = xmlFile.toURI();
	StringWriter sw = new StringWriter();
	ShapeChangeErrorHandler handler = new ShapeChangeErrorHandler(new PrintWriter(sw));

	// Act & Assert
	Exception exception = assertThrows(ValidationException.class,
		() -> XSDUtil.validate(u.toString(), handler, Optional.of(scxmlXsdLocation)));

	assert ExceptionUtils.getStackTrace(exception).contains("No XML file found at");

	assert !handler.errorsFound();

	sw.flush();
	String validationMessages = sw.toString();
	assert validationMessages.isEmpty();
    }

    @Test
    void testValidateScConfigWithValidXml() throws Exception {

	// Arrange
	String xml = "src/test/resources/xsdutiltest/scconfig_valid.xml";
	File xmlFile = new File(xml);
	URI u = xmlFile.toURI();
	StringWriter sw = new StringWriter();
	ShapeChangeErrorHandler handler = new ShapeChangeErrorHandler(new PrintWriter(sw));

	// Act & Assert
	XSDUtil.validate(u.toString(), handler, Optional.of(scXsdLocation));

	assert !handler.errorsFound();

	sw.flush();
	String validationMessages = sw.toString();
	assert validationMessages.isEmpty();
    }

    @Test
    void testValidateScConfigWithInvalidXml() throws Exception {

	// Arrange
	String xml = "src/test/resources/xsdutiltest/scconfig_invalid.xml";
	File xmlFile = new File(xml);
	URI u = xmlFile.toURI();
	StringWriter sw = new StringWriter();
	ShapeChangeErrorHandler handler = new ShapeChangeErrorHandler(new PrintWriter(sw));

	// Act & Assert
	XSDUtil.validate(u.toString(), handler, Optional.of(scXsdLocation));

	assert handler.errorsFound();

	sw.flush();
	String validationMessages = sw.toString();

	assert validationMessages.contains(
		"Error (32:28) cvc-assertion-failure-mesg: Assertion failed for schema type 'ShapeChangeConfigurationType'. Every ID in a validators XML-attribute must be the ID of a configured Validator element. Check if any validators XML-attribute contains the ID of the input element or of a transformer.");
	assert validationMessages.contains(
		"Error (32:28) cvc-assertion-failure-mesg: Assertion failed for schema type 'ShapeChangeConfigurationType'. Every ID in an input or inputs XML-attribute must be the ID of a configured transformer or input element. Check if any input or inputs XML-attribute contains the ID of a validator.");
    }
}