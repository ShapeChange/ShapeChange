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
package de.interactive_instruments.ShapeChange.Model.Generic.reader;

import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.Qualifier;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
public class QualifiersContentHandler extends AbstractContentHandler {

	private Vector<Qualifier> qualifiers = new Vector<Qualifier>();

	protected GenericPropertyContentHandler parent;

	private String name = null;
	private String type = null;

	public QualifiersContentHandler(ShapeChangeResult result, Options options,
			XMLReader reader, GenericPropertyContentHandler parent) {

		super(result, options, reader, parent);

		this.parent = parent;
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {

		if (localName.equals("Qualifier")) {

			// reset qualifier fields
			this.name = null;
			this.type = null;

		} else if (localName.equals("name")) {

			this.sb = new StringBuffer();

		} else if (localName.equals("type")) {

			this.sb = new StringBuffer();
		} else {
			// do not throw an exception, just log a message - the schema could
			// have been extended
			result.addDebug(null, 30801, "QualifiersContentHandler", localName);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

		if (localName.equals("name")) {

			this.name = sb.toString();

		} else if (localName.equals("type")) {

			this.type = sb.toString();

		} else if (localName.equals("Qualifier")) {

			Qualifier qualifier = new Qualifier();
			qualifier.name = this.name;
			qualifier.type = this.type;

			this.qualifiers.addElement(qualifier);

		} else if (localName.equals("qualifiers")) {

			parent.setQualifiers(this.qualifiers);

			parent.endElement(uri, localName, qName);

			// Switch handler back to parent
			reader.setContentHandler(parent);
		} else {
			// do not throw an exception, just log a message - the schema could
			// have been extended
			result.addDebug(null, 30801, "QualifiersContentHandler", localName);
		}
	}
}
