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
 * (c) 2002-2013 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Model.Generic;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Multiplicity;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.StructuredNumber;
import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.Model.AssociationInfo;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfoImpl;
import de.interactive_instruments.ShapeChange.Model.Qualifier;
import de.interactive_instruments.ShapeChange.Model.Stereotypes;
import de.interactive_instruments.ShapeChange.Model.TaggedValues;
import de.interactive_instruments.ShapeChange.Profile.Profiles;

/**
 * 
 */
public class GenericPropertyInfo extends PropertyInfoImpl
		implements MessageSource {

	protected Options options = null;
	protected ShapeChangeResult result = null;
	protected GenericModel model = null;

	protected String id = null;
	protected String name = null;

	protected Multiplicity cardinality = null;
	
	/**
	 * Default value is <code>true</code>.
	 */
	protected boolean isNavigable = true;
	
	protected StructuredNumber sequenceNumber = null;
	protected Type typeInfo = null;

	/**
	 * Default value is <code>false</code>.
	 */
	protected boolean isDerived = false;
	/**
	 * Default value is <code>false</code>.
	 */
	protected boolean isReadOnly = false;
	/**
	 * Default value is <code>true</code>.
	 */
	protected boolean isAttribute = true;

	/**
	 * Default value is <code>false</code>.
	 */
	protected boolean isOrdered = false;
	/**
	 * Default value is <code>true</code>.
	 */
	protected boolean isUnique = true;
	/**
	 * Default value is <code>false</code>.
	 */
	protected boolean isComposition = false;
	/**
	 * Default value is <code>false</code>.
	 */
	protected boolean isAggregation = false;

	protected String initialValue = null;
	protected String inlineOrByReference = null;

	protected PropertyInfo reverseProperty = null;
	protected AssociationInfo association = null;
	protected List<Constraint> constraints = null;

	protected ClassInfo inClass = null;

	public GenericPropertyInfo() {

	}

	public GenericPropertyInfo(GenericModel model, String id, String name) {

		this.model = model;
		this.options = model.options();

		this.result = model.result();
		setId(id);
		setName(name);

		this.cardinality = new Multiplicity("1");
	}

	/**
	 * @param isAttribute
	 *                        the isAttribute to set
	 */
	public void setAttribute(boolean isAttribute) {
		this.isAttribute = isAttribute;
	}

	/**
	 * @param reverseProperty
	 *                            the reverseProperty to set
	 */
	public void setReverseProperty(PropertyInfo reverseProperty) {
		this.reverseProperty = reverseProperty;
	}

	/**
	 * @param inClass
	 *                    the inClass to set
	 */
	public void setInClass(ClassInfo inClass) {
		this.inClass = inClass;
	}

	/**
	 * @param association
	 *                        the association to set
	 */
	public void setAssociation(AssociationInfo association) {
		this.association = association;
	}

	@Override
	public boolean isDerived() {
		return isDerived;
	}

	@Override
	public boolean isAttribute() {
		return isAttribute;
	}

	@Override
	public Type typeInfo() {
		return typeInfo;
	}

	@Override
	public boolean isNavigable() {
		return isNavigable;
	}

	@Override
	public boolean isOrdered() {
		return isOrdered;
	}

	@Override
	public boolean isUnique() {
		return isUnique;
	}

	public boolean hasConstraints() {

		if (this.constraints == null || this.constraints.isEmpty()) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public boolean isComposition() {
		return isComposition;
	}

	@Override
	public boolean isAggregation() {
		return isAggregation;
	}

	@Override
	public Multiplicity cardinality() {
		return cardinality;
	}

	@Override
	public String initialValue() {

		/*
		 * 2016-01-08 JE: returning null is relevant, for example for XSD
		 * encoding of an enum
		 */
		return initialValue;
	}

	@Override
	public String inlineOrByReference() {
		if (inlineOrByReference == null) {
			return "inlineOrByReference";
		} else {
			return inlineOrByReference;
		}
	}

	@Override
	public PropertyInfo reverseProperty() {
		return reverseProperty;
	}

	@Override
	public ClassInfo inClass() {
		return inClass;
	}

	@Override
	public void inClass(ClassInfo ci) {
		/*
		 * NOTE for cast: the cast should be safe, at least once the
		 * GenericModel has been created/parsed (because the constructor there
		 * ensures that inClass for GenericPropertyInfos is set to the correct
		 * GenericClassInfos
		 */
		inClass = (GenericClassInfo) ci;
	}

	@Override
	public StructuredNumber sequenceNumber() {
		return sequenceNumber;
	}

	@Override
	public List<Constraint> constraints() {

		if (this.constraints != null) {
			return constraints;
		} else {
			return new ArrayList<Constraint>(1);
		}
	}

	@Override
	public AssociationInfo association() {
		return association;
	}

	/**
	 * @param cardinality
	 *                        the cardinality to set
	 */
	public void setCardinality(Multiplicity cardinality) {
		this.cardinality = cardinality;
	}

	/**
	 * @param list
	 *                 the constraints to set; can be empty or <code>null</code>
	 */
	public void setConstraints(List<Constraint> list) {

		if (list != null && !list.isEmpty()) {
			this.constraints = list;
		} else {
			this.constraints = null;
		}
	}

	/**
	 * @param initialValue
	 *                         the initialValue to set
	 */
	public void setInitialValue(String initialValue) {

		if (initialValue != null) {
			this.initialValue = options.internalize(initialValue);
		} else {
			this.initialValue = null;
		}
	}

	/**
	 * @param inlineOrByReference
	 *                                the inlineOrByReference to set
	 */
	public void setInlineOrByReference(String inlineOrByReference) {

		if (inlineOrByReference != null && !inlineOrByReference
				.equalsIgnoreCase("inlineOrByReference")) {
			this.inlineOrByReference = options.internalize(inlineOrByReference);
		} else {
			this.inlineOrByReference = null;
		}
	}

	/**
	 * @param isAggregation
	 *                          the isAggregation to set
	 */
	public void setAggregation(boolean isAggregation) {
		this.isAggregation = isAggregation;
	}

	/**
	 * @param isComposition
	 *                          the isComposition to set
	 */
	public void setComposition(boolean isComposition) {
		this.isComposition = isComposition;
	}

	/**
	 * @param isDerived
	 *                      the isDerived to set
	 */
	public void setDerived(boolean isDerived) {
		this.isDerived = isDerived;
	}

	/**
	 * @param isNavigable
	 *                        the isNavigable to set
	 */
	public void setNavigable(boolean isNavigable) {
		this.isNavigable = isNavigable;
	}

	/**
	 * @param isOrdered
	 *                      the isOrdered to set
	 */
	public void setOrdered(boolean isOrdered) {
		this.isOrdered = isOrdered;
	}

	public void setQualifiers(Vector<Qualifier> qualifiers) {
		this.qualifiers = qualifiers;
	}

	/**
	 * @param isUnique
	 *                     the isUnique to set
	 */
	public void setUnique(boolean isUnique) {
		this.isUnique = isUnique;
	}

	/**
	 * Sets the structured number to be used by this object. Also sets the
	 * "sequenceNumber" tagged value to this number if desired.
	 * 
	 * IMPORTANT: update the "sequenceNumber" tagged value whenever the new
	 * number is not reflected by the current value of the "sequenceNumber"
	 * tagged value!
	 * 
	 * One example where this would be the case is when the tagged value was
	 * undefined in the model and a sequence/structured number has automatically
	 * been assigned while loading the model. A GenericPropertyInfo copy of the
	 * original PropertyInfo (for example a PropertyInfoEA) should then store
	 * the automatically assigned number in the tagged value. The reason is that
	 * any subsequent update of fields within the GenericPropertyInfo based upon
	 * its tagged values would overwrite the structuredNumber field - setting it
	 * to '0' if the tagged value was undefined at that time (which can lead to
	 * multiple class properties having '0' as structured number, even if in the
	 * inClass' properties map the keys may still show the automatically
	 * assigned number).
	 * 
	 * @param sequenceNumber
	 *                              the sequenceNumber to set
	 * @param updateTaggedValue
	 *                              <code>true</code> if the "sequenceNumber"
	 *                              tagged value shall be set to the given
	 *                              sequence number, else <code>false</code>
	 */
	public void setSequenceNumber(StructuredNumber sequenceNumber,
			boolean updateTaggedValue) {

		this.sequenceNumber = sequenceNumber;

		if (updateTaggedValue) {

			this.setTaggedValue("sequenceNumber",
					this.sequenceNumber.getString(), false);
		}
	}

	/**
	 * NOTE: sets the value of the typeInfo attribute of this
	 * GenericPropertyInfo object to reference the given one. Care needs to be
	 * taken to ensure that there are no side effects by sharing the Type
	 * object, and modifying it in different places. If in doubt, create a new
	 * Type object to set via this method, or use the copyTypeInfo method
	 * instead.
	 * 
	 * @param typeInfo
	 *                     the typeInfo to set
	 */
	public void setTypeInfo(Type typeInfo) {
		this.typeInfo = typeInfo;
	}

	/**
	 * Uses the id and name from the given Type object to initialise the fields
	 * of this object's typeInfo value (which is initialized first in case it is
	 * <code>null</code>). The typeInfo value of this object thus does not
	 * reference the given Type object (which could cause trouble).
	 * 
	 * @param typeInfo
	 */
	public void copyTypeInfo(Type typeInfo) {

		if (this.typeInfo == null) {
			this.typeInfo = new Type();
		}
		this.typeInfo.id = typeInfo.id;
		this.typeInfo.name = typeInfo.name;
	}

	public void setNilReasonAllowed(boolean nilReasonAllowed) {
		this.nilReasonAllowed = nilReasonAllowed;
	}

	public void setRestriction(boolean isRestriction) {
		this.restriction = isRestriction;
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public GenericModel model() {
		return model;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public Options options() {
		return options;
	}

	@Override
	public ShapeChangeResult result() {
		return result;
	}

	/**
	 * @param id
	 */
	public void setId(String id) {

		this.id = options.internalize(id);
	}

	/**
	 * @param model
	 */
	public void setModel(GenericModel model) {
		this.model = model;

	}

	/**
	 * @param name
	 */
	public void setName(String name) {

		this.name = options.internalize(name);
	}

	/**
	 * @param options
	 */
	public void setOptions(Options options) {
		this.options = options;
	}

	/**
	 * @param result
	 */
	public void setResult(ShapeChangeResult result) {
		this.result = result;

	}

	/** Save the (normalized) stereotypes in the cache. */
	public void validateStereotypesCache() {
		// create cache, if necessary
		if (stereotypesCache == null)
			stereotypesCache = options().stereotypesFactory();

		// do nothing else, stereotypes have to be set explicitly using
		// setStereotypes

	} // validateStereotypesCache

	/**
	 * @param stereotypeSet
	 */
	public void setStereotypes(Stereotypes stereotypeSet) {
		// reset cache
		stereotypesCache = options().stereotypesFactory();
		if (stereotypeSet != null && !stereotypeSet.isEmpty()) {
			for (String st : stereotypeSet.asArray()) {
				stereotypesCache.add(
						options.internalize(options.normalizeStereotype(st)));
			}
		}
	}

	/**
	 * @param stereotype
	 */
	public void setStereotype(String stereotype) {
		// reset cache
		stereotypesCache = options().stereotypesFactory();
		if (stereotype != null) {
			stereotypesCache.add(options
					.internalize(options.normalizeStereotype(stereotype)));
		}
	}

	public void validateTaggedValuesCache() {
		// create cache, if necessary
		if (taggedValuesCache == null)
			taggedValuesCache = options().taggedValueFactory();

		// do nothing else, tagged values have to be set explicitly using
		// setTaggedValues
	}

	/**
	 * @param taggedValues
	 * @param updateFields
	 *                         true if class fields should be updated based upon
	 *                         information from given tagged values, else false
	 */
	public void setTaggedValues(TaggedValues taggedValues,
			boolean updateFields) {

		// clone tagged values
		taggedValuesCache = options().taggedValueFactory(taggedValues);

		// Now update fields, if they are affected by tagged values
		if (updateFields && !taggedValuesCache.isEmpty()) {
			for (String key : taggedValues.keySet()) {
				updateFieldsForTaggedValue(key,
						taggedValuesCache.getFirstValue(key)); // FIXME
																// first
																// only?
			}
		}
	}

	public GenericPropertyInfo createCopy(String copyId) {

		GenericPropertyInfo copy = new GenericPropertyInfo(model, copyId, name);

		copy.setDescriptors(this.descriptors().createCopy());
		copy.setProfiles(this.profiles().createCopy());

		this.validateTaggedValuesCache();
		if (taggedValuesCache != null && !taggedValuesCache.isEmpty()) {
			copy.setTaggedValues(taggedValuesCache, false);
		}

		copy.setStereotypes(this.stereotypesCache);

		copy.setDerived(isDerived);
		copy.setReadOnly(isReadOnly);
		copy.setAttribute(isAttribute);
		copy.copyTypeInfo(this.typeInfo);
		copy.setNavigable(isNavigable);
		copy.setOrdered(isOrdered);
		copy.setUnique(isUnique);
		copy.setComposition(isComposition);
		copy.setAggregation(isAggregation);
		copy.setCardinality(new Multiplicity(cardinality.toString()));
		copy.setInitialValue(initialValue);
		copy.setInlineOrByReference(inlineOrByReference);
		copy.setReverseProperty(reverseProperty);
		copy.setInClass(inClass);

		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < sequenceNumber.components.length - 1; i++) {
			sb.append(sequenceNumber.components[i] + ".");
		}
		sb.append(sequenceNumber.components[sequenceNumber.components.length
				- 1]);
		StructuredNumber res = new StructuredNumber(sb.toString());

		/*
		 * Updating the "sequenceNumber" tagged value is not necessary because
		 * we didn't change it here.
		 */
		copy.setSequenceNumber(res, false);
		copy.setConstraints(constraints);
		copy.setAssociation(association);
		copy.setRestriction(restriction);
		copy.setNilReasonAllowed(nilReasonAllowed);

		return copy;
	}

	public String printAsString(String indent) {

		StringBuffer sb = new StringBuffer();

		sb.append(indent + name + "\n");
		sb.append(indent + indent + "tagged values: ");
		this.validateTaggedValuesCache();
		if (taggedValuesCache == null || taggedValuesCache.isEmpty()) {
			sb.append("none");
		} else {
			sb.append(taggedValuesCache.toString());
		}
		sb.append(System.getProperty("line.separator"));

		return sb.toString();
	}

	@Override
	public boolean isReadOnly() {
		return isReadOnly;
	}

	/**
	 * @param isReadOnly
	 *                       the isReadOnly to set
	 */
	public void setReadOnly(boolean isReadOnly) {
		this.isReadOnly = isReadOnly;
	}

	/**
	 * @param tvName
	 * @param tvValue
	 * @param updateFields
	 *                         true if property fields should be updated based
	 *                         upon information from given tagged value, else
	 *                         false
	 */
	public void setTaggedValue(String tvName, String tvValue,
			boolean updateFields) {

		validateTaggedValuesCache();

		taggedValuesCache.put(tvName, tvValue);

		if (updateFields) {
			updateFieldsForTaggedValue(tvName, tvValue);
		}
	}

	/**
	 * Encapsulates the logic to update property fields based upon the value of
	 * a named tagged value. Updates 'nilReasonAllowed', 'inlineOrByReference',
	 * and 'sequenceNumber'.
	 * 
	 * @param taggedValueName
	 * @param taggedValueValue
	 */
	protected void updateFieldsForTaggedValue(String tvName, String tvValue) {

		// TODO add more updates for relevant tagged values

		if (tvName.equalsIgnoreCase("nilReasonAllowed")) {

			if (tvValue.equalsIgnoreCase("true")) {
				this.setNilReasonAllowed(true);
			} else if (tvValue.equalsIgnoreCase("false")) {
				this.setNilReasonAllowed(false);
			} else {
				MessageContext mc = result.addWarning(this, 1, tvName, tvValue);
				if (mc != null) {
					mc.addDetail(this, 0, this.fullName());
				}
			}

		} else if (tvName.equalsIgnoreCase("inlineOrByReference")) {

			if (tvValue.equalsIgnoreCase("inline")) {
				this.setInlineOrByReference("inline");
			} else if (tvValue.equalsIgnoreCase("byReference")) {
				this.setInlineOrByReference("byreference");
			} else if (tvValue.equalsIgnoreCase("inlineOrByReference")) {
				this.setInlineOrByReference("inlineOrByReference");
			} else {
				MessageContext mc = result.addWarning(this, 2, tvName, tvValue);
				if (mc != null) {
					mc.addDetail(this, 0, this.fullName());
				}
			}

		} else if (tvName.equalsIgnoreCase("sequenceNumber")) {

			/*
			 * Updating the "sequenceNumber" tagged value is not necessary
			 * because this method is only called after setting a tagged value.
			 */
			this.setSequenceNumber(new StructuredNumber(tvValue), false);

		} else if (tvName.equalsIgnoreCase("profiles")) {

			// unset existing profiles
			this.profiles = null;

			/*
			 * invoke PropertyInfoImpl.profiles() method to parse profile info
			 * from TV profiles
			 */
			super.profiles();
		}

		/*
		 * TBD: Descriptors should not be modified right away, since the
		 * descriptor source might not be the tagged value
		 */
		// else if (tvName.equalsIgnoreCase("alias")) {
		//
		// LangString ls = LangString.parse(tvValue);
		// this.descriptors.put(Descriptor.ALIAS, ls);
		//
		// // this.setAliasNameAll(
		// // new Descriptors(options().internalize(tvValue)));
		//
		// } else if (tvName.equalsIgnoreCase("documentation")) {
		//
		// // we map this to the descriptor 'definition'
		// LangString ls = LangString.parse(tvValue);
		// this.descriptors.put(Descriptor.DEFINITION, ls);
		// // this.setDefinitionAll(
		// // new Descriptors(options().internalize(tvValue)));
		//
		// }
	}

	/**
	 * Adds the prefix to the 'id' of this class. Also updates the id of the
	 * 'typeInfo' (if not <code>null</code>). Does NOT update the 'globalId'.
	 * 
	 * NOTE: this method is used by the FeatureCatalogue target to ensure that
	 * IDs used in a reference model are unique to that model and do not get
	 * mixed up with the IDs of the input model.
	 * 
	 * @param prefix
	 */
	public void addPrefixToModelElementIDs(String prefix) {

		this.id = prefix + id;

		if (typeInfo.id != null) {
			typeInfo.id = prefix + typeInfo.id;
		}
	}

	/**
	 * @param profiles
	 *                     new set of profiles for this property; may be
	 *                     <code>null</code>
	 */
	public void setProfiles(Profiles profiles) {

		if (profiles == null) {
			this.profiles = new Profiles();
		} else {
			this.profiles = profiles;
		}
	}

	@Override
	public String message(int mnr) {

		switch (mnr) {
		case 0:
			return "Context: property '$1$'.";
		case 1:
			return "(GenericPropertyInfo) When setting tagged value '$1$', a boolean value (either 'false' or 'true') was expected. Found '$2$' - cannot set field(s) for this tagged value.";
		case 2:
			return "(GenericPropertyInfo) When setting tagged value '$1$', one of the values 'inline', 'byReference', or 'inlineOrByReference' was expected. Found '$2$' - cannot set field for this tagged value.";

		default:
			return "(" + GenericPropertyInfo.class.getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
