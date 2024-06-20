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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import de.interactive_instruments.ShapeChange.Multiplicity;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.StructuredNumber;
import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.Descriptors;
import de.interactive_instruments.ShapeChange.Model.ImageMetadata;
import de.interactive_instruments.ShapeChange.Model.Qualifier;
import de.interactive_instruments.ShapeChange.Model.StereotypeNormalizer;
import de.interactive_instruments.ShapeChange.Model.Stereotypes;
import de.interactive_instruments.ShapeChange.Model.TaggedValues;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericPropertyInfo;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot
 *         de)
 *
 */
public class GenericPropertyContentHandler
		extends AbstractGenericInfoContentHandler {

	private static final Set<String> SIMPLE_PROPERTY_FIELDS = new HashSet<String>(
			Arrays.asList(new String[] { "cardinality", "isDerived",
					"isReadOnly", "isAttribute", "isNavigable", "isOrdered",
					"isUnique", "isComposition", "isAggregation",
					"initialValue", "inlineOrByReference", "sequenceNumber",
					"associationId", "typeId", "typeName",
					"inClassId", "isOwned" }));

	private static final Set<String> DEPRECATED_FIELDS = new HashSet<String>(
			Arrays.asList(new String[] { "reversePropertyId" }));
	
	private GenericPropertyInfo genPi = new GenericPropertyInfo();

	private String associationId = null;
	private String inClassId = null;

	/*
	 * "0" appears to be the default for unknown classifier when loading from EA
	 * model
	 */
	private String typeId = "0";
	private String typeName = null;

	private List<ConstraintContentHandler> constraintContentHandlers = new ArrayList<ConstraintContentHandler>();
	private ProfilesContentHandler profilesContentHandler = null;

	public GenericPropertyContentHandler(ShapeChangeResult result,
			Options options, XMLReader reader, AbstractContentHandler parent) {
		super(result, options, reader, parent);

		this.genPi.setResult(result);
		this.genPi.setOptions(options);
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {

		if (DEPRECATED_FIELDS.contains(localName)) {

			// ignore

		} else if (GenericModelReaderConstants.SIMPLE_INFO_FIELDS
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

		} else if (localName.equals("profiles")) {

			this.profilesContentHandler = new ProfilesContentHandler(result,
					options, reader, this);
			reader.setContentHandler(this.profilesContentHandler);

		} else if (SIMPLE_PROPERTY_FIELDS.contains(localName)) {

			sb = new StringBuffer();

		} else if (localName.equals("qualifiers")) {

			reader.setContentHandler(new QualifiersContentHandler(result,
					options, reader, this));

		} else if (localName.equals("constraints")) {

			// ignore

		} else if (localName.equals("FolConstraint")
				|| localName.equals("OclConstraint")
				|| localName.equals("TextConstraint")) {

			ConstraintContentHandler handler = new ConstraintContentHandler(
					result, options, reader, this);
			this.constraintContentHandlers.add(handler);
			reader.setContentHandler(handler);

		} else {

			// do not throw an exception, just log a message - the schema could
			// have been extended
			result.addDebug(null, 30800, "GenericPropertyContentHandler",
					localName);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

		if (DEPRECATED_FIELDS.contains(localName)) {

			// ignore

		} else if (localName.equals("id")) {

			this.genPi.setId(sb.toString());

		} else if (localName.equals("name")) {

			this.genPi.setName(sb.toString());

		} else if (localName.equals("stereotypes")) {

			Stereotypes stereotypesCache = StereotypeNormalizer
					.normalizeAndMapToWellKnownStereotype(
							this.stringList.toArray(
									new String[this.stringList.size()]),
							this.genPi);

			this.genPi.setStereotypes(stereotypesCache);

		} else if (localName.equals("descriptors")) {

			/*
			 * ignore - we have a reference to the DescriptorsContentHandler
			 */

		} else if (localName.equals("taggedValues")) {

			/*
			 * ignore - TaggedValuesContentHandler calls
			 * this.setTaggedValues(...)
			 */

		} else if (localName.equals("profiles")) {

			this.genPi.setProfiles(this.profilesContentHandler.getProfiles());

		} else if (localName.equals("cardinality")) {

			String value = sb.toString();
			Multiplicity m = new Multiplicity(value);
			this.genPi.setCardinality(m);

		} else if (localName.equals("isDerived")) {

			this.genPi.setDerived(toBooleanValue(sb));

		} else if (localName.equals("isReadOnly")) {

			this.genPi.setReadOnly(toBooleanValue(sb));

		} else if (localName.equals("isAttribute")) {

			this.genPi.setAttribute(toBooleanValue(sb));

		} else if (localName.equals("isNavigable")) {

			this.genPi.setNavigable(toBooleanValue(sb));

		} else if (localName.equals("isOrdered")) {

			this.genPi.setOrdered(toBooleanValue(sb));

		} else if (localName.equals("isUnique")) {

			this.genPi.setUnique(toBooleanValue(sb));

		} else if (localName.equals("isOwned")) {

			this.genPi.setOwned(toBooleanValue(sb));

		} else if (localName.equals("isComposition")) {

			this.genPi.setComposition(toBooleanValue(sb));

		} else if (localName.equals("isAggregation")) {

			this.genPi.setAggregation(toBooleanValue(sb));

		} else if (localName.equals("initialValue")) {

			this.genPi.setInitialValue(sb.toString());

		} else if (localName.equals("inlineOrByReference")) {

			this.genPi.setInlineOrByReference(sb.toString());

		} else if (localName.equals("sequenceNumber")) {

			StructuredNumber sn = new StructuredNumber(sb.toString());
			this.genPi.setSequenceNumber(sn, false);

		} else if (localName.equals("inClassId")) {

			this.inClassId = sb.toString();

		} else if (localName.equals("associationId")) {

			this.associationId = sb.toString();

		} else if (localName.equals("typeId")) {

			this.typeId = sb.toString();

		} else if (localName.equals("typeName")) {

			this.typeName = sb.toString();

		} else if (localName.equals("qualifiers")) {

			/*
			 * ignore - QualifiersContentHandler calls this.setQualifiers(...)
			 */

		} else if (localName.equals("constraints")
				|| localName.equals("FolConstraint")
				|| localName.equals("OclConstraint")
				|| localName.equals("TextConstraint")) {

			// ignore

		} else if (localName.equals("Property")) {

			// set descriptors in genPi
			Descriptors desc;

			if (options.parameterAsBoolean(null,
					"applyDescriptorSourcesWhenLoadingScxml", false)) {
				desc = null;
			} else if (descriptorsHandler == null) {
				desc = new Descriptors();
			} else {
				desc = descriptorsHandler.getDescriptors();
			}
			this.genPi.setDescriptors(desc);

			Type type = new Type();
			type.id = options.internalize(this.typeId);
			type.name = options.internalize(this.typeName);
			this.genPi.setTypeInfo(type);

			// set contained constraints
			Vector<Constraint> cons = new Vector<Constraint>();

			if (!options.constraintLoadingEnabled()
					|| !options.isConstraintCreationForProperties()) {
				/*
				 * drop constraint content handlers so that updating the
				 * constraint context is not performed
				 */
				this.constraintContentHandlers = new ArrayList<>();
			} else {
				for (ConstraintContentHandler cch : this.constraintContentHandlers) {
					cons.add(cch.getConstraint());
				}
			}

			this.genPi.setConstraints(cons);

			if (this.genPi.cardinality() == null) {
				// apply default, which is '1..1'
				this.genPi.setCardinality(new Multiplicity());
			}

			// let parent know that we reached the end of the Class entry
			// (so that for example depth can properly be tracked)
			parent.endElement(uri, localName, qName);

			// Switch handler back to parent
			reader.setContentHandler(parent);

		} else {
			// do not throw an exception, just log a message - the schema could
			// have been extended
			result.addDebug(null, 30801, "GenericPropertyContentHandler",
					localName);
		}
	}

	/**
	 * @return the genPi
	 */
	public GenericPropertyInfo getGenericProperty() {
		return genPi;
	}

	@Override
	public void setTaggedValues(TaggedValues taggedValues) {

		this.genPi.setTaggedValues(taggedValues, false);
	}

	@Override
	public void setDiagrams(List<ImageMetadata> diagrams) {
		// ignore
	}

	/**
	 * @return the associationId
	 */
	public String getAssociationId() {
		return associationId;
	}

	/**
	 * @return the inClassId
	 */
	public String getInClassId() {
		return inClassId;
	}

	/**
	 * @return the constraintContentHandlers
	 */
	public List<ConstraintContentHandler> getConstraintContentHandlers() {
		return constraintContentHandlers;
	}

	public void setQualifiers(Vector<Qualifier> qualifiers) {
		this.genPi.setQualifiers(qualifiers);
	}

}
