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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.StructuredNumber;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.Descriptors;
import de.interactive_instruments.ShapeChange.Model.ImageMetadata;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Model.Stereotypes;
import de.interactive_instruments.ShapeChange.Model.TaggedValues;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericClassInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericPropertyInfo;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class GenericClassContentHandler
		extends AbstractGenericInfoContentHandler {

	private static final Set<String> SIMPLE_CLASS_FIELDS = new HashSet<String>(
			Arrays.asList(new String[] { "isAbstract", "isLeaf",
					"associationId", "baseClassId", "linkedDocument" }));

	private GenericClassInfo genCi = new GenericClassInfo();

	private String associationId = null;
	private String baseClassId = null;
	private String linkedDocument = null;

	private List<GenericPropertyContentHandler> propertyContentHandlers = new ArrayList<GenericPropertyContentHandler>();
	private List<ConstraintContentHandler> constraintContentHandlers = new ArrayList<ConstraintContentHandler>();
	private ProfilesContentHandler profilesContentHandler = null;

	public GenericClassContentHandler(ShapeChangeResult result, Options options,
			XMLReader reader, AbstractContentHandler parent) {
		super(result, options, reader, parent);

		this.genCi.setResult(result);
		this.genCi.setOptions(options);
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

		} else if (localName.equals("diagrams")) {

			reader.setContentHandler(
					new DiagramsContentHandler(result, options, reader, this));

		} else if (SIMPLE_CLASS_FIELDS.contains(localName)) {

			sb = new StringBuffer();

		} else if (localName.equals("supertypes")) {

			reader.setContentHandler(new StringListContentHandler(result,
					options, reader, this));

		} else if (localName.equals("subtypes")) {

			reader.setContentHandler(new StringListContentHandler(result,
					options, reader, this));

		} else if (localName.equals("properties")) {

			// ignore

		} else if (localName.equals("Property")) {

			GenericPropertyContentHandler handler = new GenericPropertyContentHandler(
					result, options, reader, this);
			this.propertyContentHandlers.add(handler);
			reader.setContentHandler(handler);

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
			result.addWarning(null, 30800, "GenericClassContentHandler",
					localName);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

		if (localName.equals("id")) {

			this.genCi.setId(sb.toString());

			// String id = sb.toString();
			// // strip "_C" prefix added by ModelExport
			// id = id.substring(2);
			// this.genCi.setId(id);

		} else if (localName.equals("name")) {

			this.genCi.setName(sb.toString());

		} else if (localName.equals("stereotypes")) {

			Stereotypes stereotypesCache = options.stereotypesFactory();
			for (String stereotype : this.stringList) {
				stereotypesCache.add(stereotype);
			}
			this.genCi.setStereotypes(stereotypesCache);

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

			this.genCi.setProfiles(this.profilesContentHandler.getProfiles());

		} else if (localName.equals("diagrams")) {

			/*
			 * ignore - DiagramsContentHandler calls this.setDiagrams(...)
			 */

		} else if (localName.equals("isAbstract")) {

			this.genCi.setIsAbstract(toBooleanValue(sb));

		} else if (localName.equals("isLeaf")) {

			this.genCi.setIsLeaf(toBooleanValue(sb));

		} else if (localName.equals("associationId")) {

			this.associationId = sb.toString();

		}
		// else if (localName.equals("packageId")) {
		//
		// this.packageId = sb.toString();
		//
		// }
		else if (localName.equals("baseClassId")) {

			this.baseClassId = sb.toString();

		} else if (localName.equals("linkedDocument")) {

			this.linkedDocument = sb.toString();

		} else if (localName.equals("supertypes")) {

			this.genCi.setSupertypes(new TreeSet<String>(this.stringList));

		} else if (localName.equals("subtypes")) {

			this.genCi.setSubtypes(new TreeSet<String>(this.stringList));

		} else if (localName.equals("properties")) {

			// ignore

		} else if (localName.equals("Property")) {

			// ignore

		} else if (localName.equals("constraints")
				|| localName.equals("FolConstraint")
				|| localName.equals("OclConstraint")
				|| localName.equals("TextConstraint")) {

			// ignore

		} else if (localName.equals("Class")) {

			// set descriptors in genCi
			
			Descriptors desc;
			
			if(options.parameterAsBoolean(null, "applyDescriptorSourcesWhenLoadingScxml", false)) {
				desc = null;
			} else if(descriptorsHandler == null) {
				desc = new Descriptors();
			} else {
				desc = descriptorsHandler.getDescriptors();
			}
			this.genCi.setDescriptors(desc);
			
			// for (Entry<Descriptor, Descriptors> entry : descriptors
			// .getDescriptors().entrySet()) {
			//
			// if (entry.getKey() == Descriptor.ALIAS) {
			// this.genCi.setAliasNameAll(entry.getValue());
			// } else if (entry.getKey() == Descriptor.PRIMARYCODE) {
			// this.genCi.setPrimaryCodeAll(entry.getValue());
			// } else if (entry.getKey() == Descriptor.GLOBALIDENTIFIER) {
			// this.genCi.setGlobalIdentifierAll(entry.getValue());
			// }
			// // else if(entry.getKey() == Descriptor.DOCUMENTATION) {
			// // this.genCi.setDocumentationAll(entry.getValue());
			// // }
			// else if (entry.getKey() == Descriptor.DEFINITION) {
			// this.genCi.setDefinitionAll(entry.getValue());
			// } else if (entry.getKey() == Descriptor.DESCRIPTION) {
			// this.genCi.setDescriptionAll(entry.getValue());
			// } else if (entry.getKey() == Descriptor.LEGALBASIS) {
			// this.genCi.setLegalBasisAll(entry.getValue());
			// } else if (entry.getKey() == Descriptor.LANGUAGE) {
			// this.genCi.setLanguageAll(entry.getValue());
			// } else if (entry.getKey() == Descriptor.EXAMPLE) {
			// this.genCi.setExamplesAll(entry.getValue());
			// } else if (entry.getKey() == Descriptor.DATACAPTURESTATEMENT) {
			// this.genCi.setDataCaptureStatementsAll(entry.getValue());
			// }
			// }

			// set contained properties
			SortedMap<StructuredNumber, PropertyInfo> properties = new TreeMap<StructuredNumber, PropertyInfo>();

			/*
			 * Also keep track of any non-navigable property which might have
			 * been encoded (incorrectly) - this is for backwards compatibility
			 * for the time when the ModelExport did not enforce that only
			 * navigable properties should be encoded for a Class element.
			 */
			List<GenericPropertyContentHandler> handlersOfNonNavigableProperties = new ArrayList<>();

			for (GenericPropertyContentHandler gpch : this.propertyContentHandlers) {
				GenericPropertyInfo genPi = gpch.getGenericProperty();
				/*
				 * 20180907 JE: A ClassInfo should only store navigable
				 * properties. However, we've had the case that this contract
				 * was not fulfilled by an external model implementation, which
				 * resulted in non-navigable properties having been exported
				 * using ModelExport.java (which in the meantime has been
				 * revised to prevent writing non-navigable properties for a
				 * class element). Therefore, we enforce the contract here by
				 * ignoring any non-navigable property that might have been part
				 * of the class element in the SCXML.
				 */
				if (genPi.isNavigable()) {
					properties.put(gpch.getGenericProperty().sequenceNumber(),
							genPi);
				} else {
					handlersOfNonNavigableProperties.add(gpch);
				}
			}
			this.genCi.setProperties(properties);

			/*
			 * Now remove the property content handlers for non-navigable
			 * properties from the content handler list.
			 */
			this.propertyContentHandlers
					.removeAll(handlersOfNonNavigableProperties);

			// set contained constraints
			Vector<Constraint> cons = new Vector<Constraint>();
			for (ConstraintContentHandler cch : this.constraintContentHandlers) {
				cons.add(cch.getConstraint());
			}
			this.genCi.setConstraints(cons);

			// let parent know that we reached the end of the Class entry
			// (so that for example depth can properly be tracked)
			parent.endElement(uri, localName, qName);

			// Switch handler back to parent
			reader.setContentHandler(parent);

		} else {
			// do not throw an exception, just log a warning - the schema could
			// have been extended
			result.addWarning(null, 30801, "GenericClassContentHandler",
					localName);
		}
	}

	/**
	 * @return the genPi
	 */
	public GenericClassInfo getGenericClass() {
		return genCi;
	}

	@Override
	public void setTaggedValues(TaggedValues taggedValues) {

		this.genCi.setTaggedValues(taggedValues, false);
	}

	@Override
	public void setDiagrams(List<ImageMetadata> diagrams) {

		this.genCi.setDiagrams(diagrams);
	}

	/**
	 * @return the associationId
	 */
	public String getAssociationId() {
		return associationId;
	}

	// /**
	// * @return the packageId
	// */
	// public String getPackageId() {
	// return packageId;
	// }

	/**
	 * @return the baseClassId
	 */
	public String getBaseClassId() {
		return baseClassId;
	}

	/**
	 * @return the linkedDocument
	 */
	public String getLinkedDocument() {
		return linkedDocument;
	}

	/**
	 * @return the propertyContentHandlers
	 */
	public List<GenericPropertyContentHandler> getPropertyContentHandlers() {
		return propertyContentHandlers;
	}

	/**
	 * @return the constraintContentHandlers
	 */
	public List<ConstraintContentHandler> getConstraintContentHandlers() {
		return constraintContentHandlers;
	}

}
