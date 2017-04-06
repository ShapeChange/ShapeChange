package de.interactive_instruments.ShapeChange.Model.Generic.reader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;

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
import de.interactive_instruments.ShapeChange.Model.Stereotypes;
import de.interactive_instruments.ShapeChange.Model.TaggedValues;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericPropertyInfo;
import de.interactive_instruments.ShapeChange.Model.Descriptor;

public class GenericPropertyContentHandler
		extends AbstractGenericInfoContentHandler {

	private static final Set<String> SIMPLE_PROPERTY_FIELDS = new HashSet<String>(
			Arrays.asList(new String[] { "cardinality", "isDerived",
					"isReadOnly", "isAttribute", "isNavigable", "isOrdered",
					"isUnique", "isComposition", "isAggregation",
					"initialValue", "inlineOrByReference", "defaultCodeSpace",
					"isMetadata", "sequenceNumber", "implementedByNilReason",
					"voidable", "reversePropertyId", "associationId", "typeId",
					"typeName", "inClassId" }));

	private GenericPropertyInfo genPi = new GenericPropertyInfo();

	private String reversePropertyId = null;
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

			// do not throw an exception, just log a warning - the schema could
			// have been extended
			result.addWarning(null, 30800, "GenericPropertyContentHandler",
					localName);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

		if (localName.equals("id")) {

			this.genPi.setId(sb.toString());

			// String id = sb.toString();
			// // strip "_P" prefix added by ModelExport
			// id = id.substring(2);
			// this.genPi.setId(id);

		} else if (localName.equals("name")) {

			this.genPi.setName(sb.toString());

		} else if (localName.equals("stereotypes")) {

			Stereotypes stereotypesCache = options.stereotypesFactory();
			for (String stereotype : this.stringList) {
				stereotypesCache.add(stereotype);
			}
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

		} else if (localName.equals("isComposition")) {

			this.genPi.setComposition(toBooleanValue(sb));

		} else if (localName.equals("isAggregation")) {

			this.genPi.setAggregation(toBooleanValue(sb));

		} else if (localName.equals("initialValue")) {

			this.genPi.setInitialValue(sb.toString());

		} else if (localName.equals("inlineOrByReference")) {

			this.genPi.setInlineOrByReference(sb.toString());

		} else if (localName.equals("defaultCodeSpace")) {

			this.genPi.setDefaultCodeSpace(sb.toString());

		} else if (localName.equals("isMetadata")) {

			this.genPi.setMetadata(toBooleanValue(sb));

		} else if (localName.equals("sequenceNumber")) {

			StructuredNumber sn = new StructuredNumber(sb.toString());
			this.genPi.setSequenceNumber(sn, false);

		} else if (localName.equals("implementedByNilReason")) {

			this.genPi.setImplementedByNilReason(toBooleanValue(sb));

		} else if (localName.equals("voidable")) {

			this.genPi.setVoidable(toBooleanValue(sb));

		} else if (localName.equals("inClassId")) {

			this.inClassId = sb.toString();

		} else if (localName.equals("reversePropertyId")) {

			this.reversePropertyId = sb.toString();

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
			this.genPi.setDescriptors(descriptorsHandler.getDescriptors());
			// for (Entry<Descriptor, Descriptors> entry : descriptors
			// .getDescriptors().entrySet()) {
			//
			// if (entry.getKey() == Descriptor.ALIAS) {
			// this.genPi.setAliasNameAll(entry.getValue());
			// } else if (entry.getKey() == Descriptor.PRIMARYCODE) {
			// this.genPi.setPrimaryCodeAll(entry.getValue());
			// } else if (entry.getKey() == Descriptor.GLOBALIDENTIFIER) {
			// this.genPi.setGlobalIdentifierAll(entry.getValue());
			// }
			// // else if(entry.getKey() == Descriptor.DOCUMENTATION) {
			// // this.genPi.setDocumentationAll(entry.getValue());
			// // }
			// else if (entry.getKey() == Descriptor.DEFINITION) {
			// this.genPi.setDefinitionAll(entry.getValue());
			// } else if (entry.getKey() == Descriptor.DESCRIPTION) {
			// this.genPi.setDescriptionAll(entry.getValue());
			// } else if (entry.getKey() == Descriptor.LEGALBASIS) {
			// this.genPi.setLegalBasisAll(entry.getValue());
			// } else if (entry.getKey() == Descriptor.LANGUAGE) {
			// this.genPi.setLanguageAll(entry.getValue());
			// } else if (entry.getKey() == Descriptor.EXAMPLE) {
			// this.genPi.setExamplesAll(entry.getValue());
			// } else if (entry.getKey() == Descriptor.DATACAPTURESTATEMENT) {
			// this.genPi.setDataCaptureStatementsAll(entry.getValue());
			// }
			// }

			Type type = new Type();
			type.id = options.internalize(this.typeId);
			type.name = options.internalize(this.typeName);
			this.genPi.setTypeInfo(type);

			// set contained constraints
			Vector<Constraint> cons = new Vector<Constraint>();
			for (ConstraintContentHandler cch : this.constraintContentHandlers) {
				cons.add(cch.getConstraint());
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
			// do not throw an exception, just log a warning - the schema could
			// have been extended
			result.addWarning(null, 30801, "GenericPropertyContentHandler",
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
	 * @return the reversePropertyId
	 */
	public String getReversePropertyId() {
		return reversePropertyId;
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
