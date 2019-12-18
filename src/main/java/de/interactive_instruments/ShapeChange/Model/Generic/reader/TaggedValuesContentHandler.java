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
package de.interactive_instruments.ShapeChange.Model.Generic.reader;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.TaggedValues;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments <dot>
 *         de)
 *
 */
public class TaggedValuesContentHandler extends AbstractContentHandler {

	private TaggedValues taggedValues;
	private String name;

	protected AbstractGenericInfoContentHandler parent;

	public TaggedValuesContentHandler(ShapeChangeResult result, Options options,
			XMLReader reader, AbstractGenericInfoContentHandler parent) {
		super(result, options, reader, parent);

		this.parent = parent;
		this.taggedValues = options.taggedValueFactory();
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {

		if (localName.equals("TaggedValue")) {

			/*
			 * ensure that string list for values is reset for each occurrence
			 * of a 'TaggedValue'
			 */
			this.stringList = new ArrayList<String>();

		} else if (localName.equals("name")) {

			this.sb = new StringBuffer();

		} else if (localName.equals("values")) {

			reader.setContentHandler(new StringListContentHandler(result,
					options, reader, this));

		} else {
			// do not throw an exception, just log a message - the schema could
			// have been extended
			result.addDebug(null, 30801, "TaggedValuesContentHandler",
					localName);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

		if (localName.equals("name")) {

			this.name = options.taggedValueNormalizer()
					.normalizeTaggedValue(sb.toString());

		} else if (localName.equals("values")) {

			// ignore

		} else if (localName.equals("TaggedValue")) {

			/*
			 * We need to check if this tag shall be ignored. That is the case
			 * if its name is null (due to normalization).
			 */
			if (this.name != null) {

				if (!this.stringList.isEmpty()) {

					taggedValues.put(this.name, this.stringList);

				} else {

					taggedValues.add(this.name, "");
				}
			}

		} else if (localName.equals("taggedValues")) {

			parent.setTaggedValues(taggedValues);

			parent.endElement(uri, localName, qName);

			// Switch handler back to parent
			reader.setContentHandler(parent);

		} else {
			// do not throw an exception, just log a message - the schema could
			// have been extended
			result.addDebug(null, 30801, "TaggedValuesContentHandler",
					localName);
		}
	}
}
