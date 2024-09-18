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

import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.model.Descriptors;
import de.interactive_instruments.shapechange.core.model.ImageMetadata;
import de.interactive_instruments.shapechange.core.model.StereotypeNormalizer;
import de.interactive_instruments.shapechange.core.model.Stereotypes;
import de.interactive_instruments.shapechange.core.model.TaggedValues;
import de.interactive_instruments.shapechange.core.model.generic.GenericAssociationInfo;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot
 *         de)
 *
 */
public class GenericAssociationContentHandler
		extends AbstractGenericInfoContentHandler {

	private GenericAssociationInfo genAi = new GenericAssociationInfo();

	private boolean isInEnd1 = false;
	private boolean isInEnd2 = false;

	private String assocClassId = null;
	private String end1Id = null;
	private String end2Id = null;
	private GenericPropertyContentHandler end1PropertyContentHandler = null;
	private GenericPropertyContentHandler end2PropertyContentHandler = null;

	public GenericAssociationContentHandler(ShapeChangeResult result,
			Options options, XMLReader reader, AbstractContentHandler parent) {
		super(result, options, reader, parent);

		this.genAi.setResult(result);
		this.genAi.setOptions(options);
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {

		if (GenericModelReaderConstants.SIMPLE_INFO_FIELDS
				.contains(localName)) {

			sb = new StringBuffer();

		} else if (localName.equals("descriptors")) {

			DescriptorsContentHandler handler = new DescriptorsContentHandler(
					result, options, reader, this);

			super.descriptorsHandler = handler;
			reader.setContentHandler(handler);

		} else if (localName.equals("taggedValues")) {

			reader.setContentHandler(new TaggedValuesContentHandler(result,
					options, reader, this));

		} else if (localName.equals("stereotypes")) {

			reader.setContentHandler(new StringListContentHandler(result,
					options, reader, this));

		} else if (localName.equals("assocClassId")) {

			sb = new StringBuffer();

		} else if (localName.equals("end1")) {

			isInEnd1 = true;

			// NOTE: will be null if attribute does not exist
			this.end1Id = atts.getValue("", "ref");

		} else if (localName.equals("end2")) {

			isInEnd2 = true;

			// NOTE: will be null if attribute does not exist
			this.end2Id = atts.getValue("", "ref");

		} else if (localName.equals("Property")) {

			GenericPropertyContentHandler handler = new GenericPropertyContentHandler(
					result, options, reader, this);
			if (isInEnd1) {
				this.end1PropertyContentHandler = handler;
			} else if (isInEnd2) {
				this.end2PropertyContentHandler = handler;
			}
			reader.setContentHandler(handler);

		} else {

			// do not throw an exception, just log a message - the schema could
			// have been extended
			result.addDebug(null, 30800, "GenericAssociationContentHandler",
					localName);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

		if (localName.equals("name")) {

			this.genAi.setName(sb.toString());

		} else if (localName.equals("id")) {

			this.genAi.setId(sb.toString());

		} else if (localName.equals("stereotypes")) {

			Stereotypes stereotypesCache = StereotypeNormalizer
					.normalizeAndMapToWellKnownStereotype(
							this.stringList.toArray(
									new String[this.stringList.size()]),
							this.genAi);

			this.genAi.setStereotypes(stereotypesCache);

		} else if (localName.equals("descriptors")) {

			/*
			 * ignore - we have a reference to the DescriptorsContentHandler
			 */

		} else if (localName.equals("taggedValues")) {

			/*
			 * ignore - TaggedValuesContentHandler calls
			 * this.setTaggedValues(...)
			 */

		} else if (localName.equals("assocClassId")) {

			this.assocClassId = sb.toString();

		} else if (localName.equals("end1")) {

			isInEnd1 = false;

		} else if (localName.equals("end2")) {

			isInEnd2 = false;

		} else if (localName.equals("Property")) {

			// ignore

		} else if (localName.equals("Association")) {

			// set descriptors in genAi

			Descriptors desc;

			if (options.parameterAsBoolean(null,
					"applyDescriptorSourcesWhenLoadingScxml", false)) {
				desc = null;
			} else if (descriptorsHandler == null) {
				desc = new Descriptors();
			} else {
				desc = descriptorsHandler.getDescriptors();
			}
			this.genAi.setDescriptors(desc);

			// let parent know that we reached the end of the Association entry
			// (so that for example depth can properly be tracked)
			parent.endElement(uri, localName, qName);

			// Switch handler back to parent
			reader.setContentHandler(parent);

		} else {
			// do not throw an exception, just log a message - the schema could
			// have been extended
			result.addDebug(null, 30801, "GenericAssociationContentHandler",
					localName);
		}
	}

	@Override
	public void setTaggedValues(TaggedValues taggedValues) {

		this.genAi.setTaggedValues(taggedValues, false);
	}

	@Override
	public void setDiagrams(List<ImageMetadata> diagrams) {

		// ignore
	}

	/**
	 * @return the genAi
	 */
	public GenericAssociationInfo getGenericAssociationInfo() {
		return genAi;
	}

	/**
	 * @return the assocClassId
	 */
	public String getAssocClassId() {
		return assocClassId;
	}

	/**
	 * @return the end1Id
	 */
	public String getEnd1Id() {
		return end1Id;
	}

	/**
	 * @return the end2Id
	 */
	public String getEnd2Id() {
		return end2Id;
	}

	/**
	 * @return the end1PropertyContentHandler
	 */
	public GenericPropertyContentHandler getEnd1PropertyContentHandler() {
		return end1PropertyContentHandler;
	}

	/**
	 * @return the end2PropertyContentHandler
	 */
	public GenericPropertyContentHandler getEnd2PropertyContentHandler() {
		return end2PropertyContentHandler;
	}

}
