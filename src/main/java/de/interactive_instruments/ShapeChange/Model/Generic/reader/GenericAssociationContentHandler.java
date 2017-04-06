package de.interactive_instruments.ShapeChange.Model.Generic.reader;

import java.util.List;
import java.util.Map.Entry;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.Model.Descriptor;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.Descriptors;
import de.interactive_instruments.ShapeChange.Model.ImageMetadata;
import de.interactive_instruments.ShapeChange.Model.Stereotypes;
import de.interactive_instruments.ShapeChange.Model.TaggedValues;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericAssociationInfo;

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

			// do not throw an exception, just log a warning - the schema could
			// have been extended
			result.addWarning(null, 30800, "GenericAssociationContentHandler",
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
			
//			String id = sb.toString();
//			// strip "_A" prefix added by ModelExport
//			id = id.substring(2);
//			this.genAi.setId(id);

		} else if (localName.equals("stereotypes")) {

			Stereotypes stereotypesCache = options.stereotypesFactory();
			for (String stereotype : this.stringList) {
				stereotypesCache.add(stereotype);
			}
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
			this.genAi.setDescriptors(descriptorsHandler.getDescriptors());
//			for (Entry<Descriptor, Descriptors> entry : descriptors
//					.getDescriptors().entrySet()) {
//				
//				if(entry.getKey() == Descriptor.ALIAS) {
//					this.genAi.setAliasNameAll(entry.getValue());
//				} else if(entry.getKey() == Descriptor.PRIMARYCODE) {
//					this.genAi.setPrimaryCodeAll(entry.getValue());
//				} else if(entry.getKey() == Descriptor.GLOBALIDENTIFIER) {
//					this.genAi.setGlobalIdentifierAll(entry.getValue());
//				}
////				else if(entry.getKey() == Descriptor.DOCUMENTATION) {
////					this.genAi.setDocumentationAll(entry.getValue());
////				}
//				else if(entry.getKey() == Descriptor.DEFINITION) {
//					this.genAi.setDefinitionAll(entry.getValue());
//				} else if(entry.getKey() == Descriptor.DESCRIPTION) {
//					this.genAi.setDescriptionAll(entry.getValue());
//				} else if(entry.getKey() == Descriptor.LEGALBASIS) {
//					this.genAi.setLegalBasisAll(entry.getValue());
//				} else if(entry.getKey() == Descriptor.LANGUAGE) {
//					this.genAi.setLanguageAll(entry.getValue());
//				} else if(entry.getKey() == Descriptor.EXAMPLE) {
//					this.genAi.setExamplesAll(entry.getValue());
//				}  else if(entry.getKey() == Descriptor.DATACAPTURESTATEMENT) {
//					this.genAi.setDataCaptureStatementsAll(entry.getValue());
//				}  
//			}

			// let parent know that we reached the end of the Association entry
			// (so that for example depth can properly be tracked)
			parent.endElement(uri, localName, qName);

			// Switch handler back to parent
			reader.setContentHandler(parent);

		} else {
			// do not throw an exception, just log a warning - the schema could
			// have been extended
			result.addWarning(null, 30801, "GenericAssociationContentHandler",
					localName);
		}
	}

	@Override
	public void setTaggedValues(TaggedValues taggedValues) {

		this.genAi.setTaggedValues(taggedValues);
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
