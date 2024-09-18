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
package de.interactive_instruments.shapechange.core.model.generic.reader;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
public abstract class AbstractContentHandler implements ContentHandler {

	protected ShapeChangeResult result;
	protected Options options;
	protected XMLReader reader;
	protected AbstractContentHandler parent = null;

	protected StringBuffer sb;
	protected List<String> stringList = new ArrayList<String>();

	public AbstractContentHandler(ShapeChangeResult result, Options options,
			XMLReader reader, AbstractContentHandler parent) {

		this.result = result;
		this.options = options;
		this.reader = reader;
		this.parent = parent;
	}

	@Override
	public void setDocumentLocator(Locator locator) {
		// irrelevant
	}

	@Override
	public void startDocument() throws SAXException {
		// irrelevant
	}

	@Override
	public void endDocument() throws SAXException {
		// irrelevant
	}

	@Override
	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {
		// irrelevant
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		// irrelevant
	}

	@Override
	public abstract void startElement(String uri, String localName,
			String qName, Attributes atts) throws SAXException;

	@Override
	public abstract void endElement(String uri, String localName, String qName)
			throws SAXException;

	/**
	 * Appends the content of the character array to the internal string buffer.
	 * 
	 * @see org.xml.sax.ContentHandler#characters(char[], int, int)
	 */
	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {

		if (sb != null) {
			sb.append(ch, start, length);
		}
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length)
			throws SAXException {
		// irrelevant
	}

	@Override
	public void processingInstruction(String target, String data)
			throws SAXException {
		// irrelevant
	}

	@Override
	public void skippedEntity(String name) throws SAXException {
		// irrelevant
	}

	/**
	 * @param sb tbd
	 * @return <code>true</code> if the value of the StringBuffer is 'true' or
	 *         '1', else <code>false</code>.
	 */
	protected boolean toBooleanValue(StringBuffer sb) {
		String value = sb.toString().trim();
		boolean res = value.equalsIgnoreCase("true") || value.equals("1");
		return res;
	}

}
