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
 * (c) 2002-2022 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Model.Writer;

import java.io.File;

import org.xml.sax.SAXException;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Util.XMLWriter;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public abstract class AbstractModelWriter {

    public static final String NS = "http://shapechange.net/model";

    protected String encoding = null;

    protected XMLWriter writer = null;

    protected File outputXmlFile = null;
    protected boolean zipOutput = false;
    protected String schemaLocation = null;
    
    protected Options options = null;
    protected ShapeChangeResult result = null;

    // set buffer size for streams (in bytes)
    protected int streamBufferSize = 8 * 1042;

    public AbstractModelWriter(Options o, ShapeChangeResult r, String encoding, File outputXmlFile, boolean zipOutput,
	    String schemaLocation) {

	this.options = o;
	this.result = r;
	this.encoding = encoding;
	this.outputXmlFile = outputXmlFile;
	this.zipOutput = zipOutput;
	this.schemaLocation = schemaLocation;
    }
    
    public AbstractModelWriter(Options o, ShapeChangeResult r) {

	this.options = o;
	this.result = r;
    }

    /**
     * Creates an element with the given name, containing the given boolean as value
     * ('true' or 'false') - if and only if the given boolean does not have the same
     * value as the given default value.
     * 
     * @param elementName
     * @param value
     * @param defaultValue
     * @throws SAXException
     */
    protected void printDataElement(String elementName, boolean value, boolean defaultValue) throws SAXException {

	// java logical XOR operator is: ^
	if (value ^ defaultValue) {
	    printDataElement(elementName, "" + value);
	}
    }

    /**
     * Creates an element with the given name, containing the given string as value
     * - if and only if the given string is not <code>null</code>, has a length
     * greater than 0, and does not equal (ignoring case!) the given default value.
     * 
     * @param elementName
     * @param value
     * @param defaultValue
     * @throws SAXException
     */
    protected void printDataElement(String elementName, String value, String defaultValue) throws SAXException {

	if (value != null && value.length() > 0 && !value.equalsIgnoreCase(defaultValue)) {
	    writer.dataElement(NS, elementName, value);
	}
    }

    /**
     * Creates an element with the given name, containing the given string as value
     * - if and only if the given string is not <code>null</code> and has a length
     * greater than 0.
     * 
     * @param elementName
     * @param s
     * @throws SAXException
     */
    protected void printDataElement(String elementName, String s) throws SAXException {

	if (s != null && s.length() > 0) {
	    writer.dataElement(NS, elementName, s);
	}
    }

    /**
     * For each string in the given array of strings, creates an element with the
     * given name, containing the string as value - if and only if the string is not
     * <code>null</code> and has a length greater than 0.
     * 
     * @param elementName
     * @param strings
     * @throws SAXException
     */
    protected void printDataElements(String elementName, String[] strings) throws SAXException {

	if (strings != null) {
	    for (String s : strings)
		printDataElement(elementName, s);
	}
    }

    /**
     * Creates an element with the given name, containing the given string as value
     * and having an attribute with given name and value - if and only if the given
     * string is not <code>null</code> and has a length greater than 0.
     * 
     * @param elementName
     * @param elementContent
     * @param attributeName
     * @param attributeValue
     * @throws SAXException
     */
    protected void printDataElement(String elementName, String elementContent, String attributeName,
	    String attributeValue) throws SAXException {

	if (elementContent != null && elementContent.length() > 0) {
	    writer.dataElement(NS, elementName, elementContent, attributeName, attributeValue);
	}
    }
}
