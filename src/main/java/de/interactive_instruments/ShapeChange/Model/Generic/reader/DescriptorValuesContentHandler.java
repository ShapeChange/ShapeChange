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
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.LangString;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class DescriptorValuesContentHandler extends AbstractContentHandler {

	private List<LangString> descriptorValues = new ArrayList<LangString>();

	private String value = null;
	private String lang = null;

	public DescriptorValuesContentHandler(ShapeChangeResult result,
			Options options, XMLReader reader, AbstractContentHandler parent) {
		super(result, options, reader, parent);
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {

		if (localName.equals("descriptorValues")) {

			// ignore

		} else if (localName.equals("DescriptorValue")) {

			if (atts != null) {
				this.lang = atts.getValue("lang");
			} else {
				this.lang = null;
			}

			sb = new StringBuffer();
			this.value = null;
			// this.lang = null;

		}
		// else if (localName.equals("value")) {
		//
		// sb = new StringBuffer();
		//
		// }
		// else if (localName.equals("lang")) {
		//
		// sb = new StringBuffer();
		//
		// }
		else {

			// do not throw an exception, just log a warning - the schema could
			// have been extended
			result.addWarning(null, 30800, "DescriptorValuesContentHandler",
					localName);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

		if (localName.equals("descriptorValues")) {

			/*
			 * the parent has a reference to this content handler so can invoke
			 * getDescriptors()
			 */

			/*
			 * let parent know that we reached the end of the descriptorValues
			 * element (so that for example depth can properly be tracked)
			 */
			parent.endElement(uri, localName, qName);

			// Switch handler back to parent
			reader.setContentHandler(parent);

		} else if (localName.equals("DescriptorValue")) {

			this.value = sb.toString();

			LangString dv = new LangString(options.internalize(this.value),
					options.internalize(this.lang));
			this.descriptorValues.add(dv);

		}
		// else if (localName.equals("value")) {
		//
		// this.value = sb.toString();
		//
		// } else if (localName.equals("lang")) {
		//
		// this.lang = sb.toString();
		//
		// }
		else {

			// do not throw an exception, just log a warning - the schema could
			// have been extended
			result.addWarning(null, 30801, "DescriptorValuesContentHandler",
					localName);
		}
	}

	public List<LangString> getDescriptorValues() {
		return this.descriptorValues;
	}

}
