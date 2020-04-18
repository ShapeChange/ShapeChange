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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.stream.Collectors;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.StructuredNumber;
import de.interactive_instruments.ShapeChange.Model.AssociationInfo;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.ClassInfoImpl;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.OperationInfo;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Model.Stereotypes;
import de.interactive_instruments.ShapeChange.Model.TaggedValues;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel.PropertyCopyDuplicatBehaviorIndicator;
import de.interactive_instruments.ShapeChange.Profile.Profiles;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 */
public class GenericClassInfo extends ClassInfoImpl implements MessageSource {

	protected Options options = null;
	protected ShapeChangeResult result = null;
	protected GenericModel model = null;

	protected String id = null;
	protected String name = null;

	protected boolean isAbstract = false;
	protected boolean isLeaf = false;

	protected PackageInfo pkg = null;

	protected AssociationInfo assocClass = null;
	/**
	 * Set of ids of all base classes of this class
	 */
	protected TreeSet<String> supertypes = new TreeSet<String>();
	protected TreeSet<String> subtypes = new TreeSet<String>();
	protected SortedMap<StructuredNumber, PropertyInfo> properties = new TreeMap<StructuredNumber, PropertyInfo>();
	/**
	 * May be null
	 */
	protected Vector<Constraint> constraints = null;
	protected boolean includesConstraintsFromSupertypes = false;

	public GenericClassInfo() {

	}

	public GenericClassInfo(GenericModel model, String id, String name,
			int category) {

		this.model = model;
		this.options = model.options();
		this.result = model.result();
		this.id = id;
		this.name = name;
		this.category = category;
	}

	public void setPkg(PackageInfo pkg) {
		this.pkg = pkg;
	}

	public void setIsAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	public void setIsLeaf(boolean isLeaf) {
		this.isLeaf = isLeaf;
	}

	public void setAssocInfo(AssociationInfo assocClass) {
		this.assocClass = assocClass;
	}

	/**
	 * @param supertypes
	 *            the supertypes to set, can be <code>null</code>
	 */
	public void setSupertypes(TreeSet<String> supertypes) {
		this.supertypes = (supertypes == null || supertypes.isEmpty()) ? null
				: supertypes;
	}

	/**
	 * @param subtypes
	 *            the subtypes to set, can be <code>null</code>
	 */
	public void setSubtypes(TreeSet<String> subtypes) {
		this.subtypes = subtypes;
	}

	public void setProperties(
			SortedMap<StructuredNumber, PropertyInfo> properties) {
		this.properties = properties;
	}

	/**
	 * @param list
	 *            the constraints to set, may be <code>null</code>
	 */
	public void setConstraints(Vector<Constraint> list) {
		this.constraints = list;
	}

	@Override
	public String fullName() {
		if (pkg != null && name != null)
			return pkg.fullName() + "::" + name;
		else
			return null;
	}

	public String id() {
		return id;
	}

	public GenericModel model() {
		return model;
	}

	public String name() {
		return name;
	}

	public Options options() {
		return options;
	}

	public ShapeChangeResult result() {
		return result;
	}

	public void setId(String id) {
		this.id = id;

	}

	public void setModel(GenericModel model) {
		this.model = model;

	}

	public void setName(String name) {
		this.name = name;

	}

	public void setOptions(Options options) {
		this.options = options;
	}

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

	}

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
	 * @param taggedValues tbd
	 * @param updateFields
	 *            true if class fields should be updated based upon information
	 *            from given tagged values, else false
	 */
	public void setTaggedValues(TaggedValues taggedValues,
			boolean updateFields) {

		// clone tagged values
		taggedValuesCache = options().taggedValueFactory(taggedValues);

		// Now update fields, if they are affected by tagged values
		if (updateFields && !taggedValuesCache.isEmpty()) {

			for (String key : taggedValues.keySet()) {
				updateFieldsForTaggedValue(key,
						taggedValuesCache.getFirstValue(key)); // FIXME first
																// only?
			}
		}
	}

	/**
	 * Encapsulates the logic to update class fields based upon the value of a
	 * named tagged value.
	 * 
	 * @param tvName tbd
	 * @param tvValue tbd
	 */
	private void updateFieldsForTaggedValue(String tvName, String tvValue) {

		// TODO add more updates for relevant tagged values
		
		if (tvName.equalsIgnoreCase("profiles")) {
			
			// unset existing profiles
			this.profiles = null;
			
			/*
			 * invoke ClassInfoImpl.profiles() method to parse profile info from TV
			 * profiles
			 */
			super.profiles();
		}

		/*
		 * TBD: Descriptors should not be modified right away, since the
		 * descriptor source might not be the tagged value
		 */
		// if (tvName.equalsIgnoreCase("alias")) {
		//
		// LangString ls = LangString.parse(tvValue);
		// this.descriptors.put(Descriptor.ALIAS, ls);
		// // this.setAliasNameAll(new Descriptors(tvValue));
		//
		// } else if (tvName.equalsIgnoreCase("documentation")) {
		//
		// // we map this to the descriptor 'definition'
		//
		// LangString ls = LangString.parse(tvValue);
		// this.descriptors.put(Descriptor.DEFINITION, ls);
		// // this.setDefinitionAll(new Descriptors(tvValue));
		//
		// }
	}

	public PackageInfo pkg() {
		return pkg;
	}

	public boolean isAbstract() {
		return isAbstract;
	}

	public boolean isLeaf() {
		return isLeaf;
	}

	public AssociationInfo isAssocClass() {
		return assocClass;
	}

	@Override
	public TreeSet<String> supertypes() {

		if (supertypes == null) {
			return new TreeSet<String>();
		} else {
			return supertypes;
		}
	}

	/**
	 * Returns the list of ids of the subtypes that belong to this class.
	 * <p>
	 * NOTE: does NOT return a shallow copy, thus modifications of the returned
	 * set will modify the subtype information for this class
	 * 
	 * @return Set with the ids of all direct subtypes of this class. Can be
	 *         empty but not <code>null</code>.
	 */
	public TreeSet<String> subtypes() {

		if (subtypes == null) {
			return new TreeSet<String>();
		} else {
			return subtypes;
		}
	}

	public boolean hasSupertypes() {
		if (supertypes != null && supertypes.size() > 0)
			return true;
		else
			return false;
	}

	public boolean hasSubtypes() {
		if (subtypes != null && subtypes.size() > 0)
			return true;
		else
			return false;
	}

	public boolean hasConstraints() {
		if (this.constraints == null || this.constraints.isEmpty()) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public SortedMap<StructuredNumber, PropertyInfo> properties() {
		if (properties == null) {
			return new TreeMap<StructuredNumber, PropertyInfo>();
		} else {
			return properties;
		}
	}

	@Override
	public List<Constraint> constraints() {
		
		if (!includesConstraintsFromSupertypes) {
			// constraints() is expected to return also all constraints of the supertypes 
			// that are not overloaded by a constraint with the same name in this class.
			// The list of constraints is updated the first time the list of constraints 
			// is requested.
			if (Objects.nonNull(supertypes)) {
			    Set<String> namefilter = Objects.isNull(constraints) ? 
			    		new HashSet<String>() : 
				    	constraints.stream()
			    			.map(Constraint::name)
			    			.filter(name -> Objects.nonNull(name) && !name.isEmpty())
			    			.collect(Collectors.toSet());
				
				Vector<Constraint> supertypeConstraints = supertypes.stream()
						.map(name -> model.classById(name))
						.filter(ci -> Objects.nonNull(ci)) // ignore any supertypes where we only have the name
						.map(ClassInfo::constraints) // get the constraints of the supertypes
						.flatMap(List::stream)
						.filter(constr -> Objects.nonNull(constr)) // skip cases with no constraint
						.filter(constr -> Objects.isNull(constr.name()) || constr.name().isEmpty() || !namefilter.contains(constr.name())) // remove any constraints from supertypes that are overloaded by this class
				        .collect(Collectors.toCollection(Vector::new));
				
				if (Objects.nonNull(constraints)) {
					if (!supertypeConstraints.isEmpty())
						constraints = supertypeConstraints;
				} else {
					if (!supertypeConstraints.isEmpty())
						constraints.addAll(supertypeConstraints);				
				}
			}
			includesConstraintsFromSupertypes = true;
		}
		
		if (constraints == null) {
			return new Vector<Constraint>(1);
		} else {
			return constraints;
		}
	}

	@Override
	public PropertyInfo property(String name) {
		// Search in own properties
		for (PropertyInfo pi : properties.values()) {
			if (pi.name().equals(name))
				return pi;
		}
		// Go and search in base class, if it exists
		Map<String, ClassInfo> baseClassesById = new HashMap<String, ClassInfo>();		
		if (supertypes != null && !supertypes.isEmpty()) {
			for (String supertypeId : supertypes) {
				baseClassesById.put(supertypeId, model.classById(supertypeId));
			}
		}

		for (ClassInfo baseCi : baseClassesById.values()) {

			PropertyInfo pi = baseCi.property(name);
			if (pi != null)
				return pi;
		}

		return null;
	}

	/**
	 * NOTE: Operations are currently not supported
	 */
	public OperationInfo operation(String name, String[] types) {
		// currently not supported
		return null;
	}

	public String printAsString(String indent) {

		StringBuffer sb = new StringBuffer();

		sb.append(indent + name + "\n");

		sb.append(indent + indent + "properties:\n");

		for (PropertyInfo property : properties.values()) {

			/*
			 * NOTE for cast: the cast should be safe, because the property
			 * belongs to the class which is a GenericClassInfo, thus the
			 * property should also be a GenericPropertyInfo ... at least after
			 * the GenericModel has fully been parsed/created
			 */
			sb.append(((GenericPropertyInfo) property)
					.printAsString(indent + indent));
		}
		return sb.toString();

	}

	public void setCategory(int category) {
		this.category = category;
	}

	public void removeSubtype(String subtypeId) {
		if (this.subtypes != null) {
			this.subtypes.remove(subtypeId);
		}
	}

	/**
	 * @param subtypeId tbd
	 * @return <code>true</code> if the specified subtype was not already
	 *         contained in the set of subtypes
	 */
	public boolean addSubtype(String subtypeId) {
		if (this.subtypes == null) {
			this.subtypes = new TreeSet<String>();
		}
		return this.subtypes.add(subtypeId);
	}

	/**
	 * Removes the supertype with given ID from the list of supertypes defined
	 * for this class.
	 * 
	 * @param supertypeId tbd
	 */
	public void removeSupertype(String supertypeId) {
		if (supertypes == null || supertypes.size() == 0 || supertypeId == null)
			return;
		else
			this.supertypes.remove(supertypeId);
	}

	/**
	 * @param supertypeId tbd
	 * @return <code>true</code> if the specified supertypeId was not already
	 *         contained in the set of supertypes
	 */
	public boolean addSupertype(String supertypeId) {

		if (this.supertypes == null) {
			this.supertypes = new TreeSet<String>();
		}

		return this.supertypes().add(supertypeId);
	}

	/**
	 * Adds the new property to the set of properties of this class. The
	 * behavior for adding a property that has the same name as an existing one
	 * is determined by a parameter.
	 * 
	 * WARNING1: if the given property does not have the same name as an
	 * existing one but the same sequence number, this will overwrite the
	 * existing property with that sequence number!
	 * 
	 * WARNING2: duplicates are detected by name. For classes with many
	 * properties and many properties to be added this can take a significant
	 * amount of time. In that case, better use the addProperties(...) method.
	 * 
	 * @param newProperty tbd
	 * @param duplicateHandling tbd
	 */
	public void addProperty(GenericPropertyInfo newProperty,
			PropertyCopyDuplicatBehaviorIndicator duplicateHandling) {

		if (this.properties == null) {
			properties = new TreeMap<StructuredNumber, PropertyInfo>();
		}

		GenericPropertyInfo existingPropWithSameName = (GenericPropertyInfo) this
				.ownedProperty(newProperty.name());

		if (existingPropWithSameName == null) {

			properties.put(newProperty.sequenceNumber(), newProperty);

			this.model.genPropertiesById.put(newProperty.id(), newProperty);

		} else if (duplicateHandling == PropertyCopyDuplicatBehaviorIndicator.ADD) {

			// add but log a warning
			result.addWarning(this, 30200, newProperty.name(), this.name());
			properties.put(newProperty.sequenceNumber(), newProperty);

			this.model.genPropertiesById.put(newProperty.id(), newProperty);

		} else if (duplicateHandling == PropertyCopyDuplicatBehaviorIndicator.IGNORE) {

			/*
			 * alright, we do not add the new property to the properties of this
			 * class, but log a warning
			 */

			result.addWarning(this, 30201, newProperty.name(), this.name());

		} else if (duplicateHandling == PropertyCopyDuplicatBehaviorIndicator.IGNORE_UNRESTRICT) {

			/*
			 * alright, we do not add the new property to the properties of this
			 * class, but we need to "unrestrict" the existing one - and log a
			 * warning
			 */

			result.addWarning(this, 30202, newProperty.name(), this.name());

			existingPropWithSameName.setRestriction(false);

		} else if (duplicateHandling == PropertyCopyDuplicatBehaviorIndicator.OVERWRITE) {

			/*
			 * Remove the existing property and add the new one - and log a
			 * warning
			 */

			result.addWarning(this, 30203, newProperty.name(), this.name());

			properties.remove(existingPropWithSameName.sequenceNumber());

			this.model.genPropertiesById.remove(existingPropWithSameName.id());

			properties.put(newProperty.sequenceNumber(), newProperty);

			this.model.genPropertiesById.put(newProperty.id(), newProperty);
		}
	}

	public StructuredNumber getNextSequenceNumber() {

		int maxSequenceNumber = Integer.MIN_VALUE;

		if (properties == null || properties.isEmpty()) {
			maxSequenceNumber = 0;
		} else {
			Set<StructuredNumber> enumSeqNumbers = properties.keySet();
			// look up highest sequence number in list of existing
			// properties (via first component of the structured number)
			for (StructuredNumber strucNum : enumSeqNumbers) {
				if (strucNum.components[0] > maxSequenceNumber) {
					maxSequenceNumber = strucNum.components[0];
				}
			}
		}

		int result = maxSequenceNumber + 1;
		return new StructuredNumber(result);
	}

	/**
	 * Adds the given constraints to the constraints of this class, preventing
	 * duplicates (references to same constraint object).
	 * 
	 * @param list
	 *            constraints to add; can be empty or <code>null</code>
	 */
	public void addConstraints(List<Constraint> list) {
		if (list == null || list.isEmpty())
			return;
		else {
			if (this.constraints == null) {
				this.constraints = new Vector<Constraint>();
			}
			for (Constraint con : list) {
				if (!this.constraints.contains(con)) {
					this.constraints.addElement(con);
				}
			}
		}
	}

	public void removePropertyById(String id) {

		StructuredNumber keyFound = null;

		for (Entry<StructuredNumber, PropertyInfo> entry : properties
				.entrySet()) {

			PropertyInfo pi = entry.getValue();

			if (pi.id().equals(id)) {
				keyFound = entry.getKey();
			}
		}

		if (keyFound != null)
			properties.remove(keyFound);
	}

	public void updateSubtypeId(String currentId, String newId) {
		if (subtypes != null && subtypes.contains(currentId)) {
			subtypes.remove(currentId);
			subtypes.add(newId);
		}
	}

	public void updateSupertypeId(String currentId, String newId) {
		if (supertypes != null && supertypes.contains(currentId)) {
			supertypes.remove(currentId);
			supertypes.add(newId);
		}
	}

	/**
	 * WARNING: creates copies of attributes, but NOT of association roles
	 * 
	 * @param copyId tbd
	 * @param copyName tbd
	 * @param copyCategory tbd
	 * @return tbd
	 */
	@SuppressWarnings("unchecked")
	public GenericClassInfo createCopy(String copyId, String copyName,
			int copyCategory) {

		GenericClassInfo copy = new GenericClassInfo(this.model, copyId,
				copyName, copyCategory);

		// set descriptors
		copy.setDescriptors(this.descriptors().createCopy());
		copy.setProfiles(this.profiles().createCopy());
		// copy.setGlobalIdentifierAll(globalIdentifier);
		// copy.setAliasNameAll(aliasName);
		// copy.setDefinitionAll(definition);
		// copy.setDescriptionAll(description);
		// copy.setPrimaryCodeAll(primaryCode);
		// copy.setLanguageAll(language);
		// copy.setLegalBasisAll(legalBasis);
		// copy.setDataCaptureStatementsAll(dataCaptureStatements);
		// copy.setExamplesAll(examples);

		copy.setStereotypes(stereotypesCache);
		copy.setTaggedValues(taggedValuesCache, false);
		copy.setPkg(pkg);
		copy.setIsAbstract(isAbstract);
		copy.setIsLeaf(isLeaf);
		copy.setAssocInfo(assocClass);
		copy.setSupertypes(supertypes == null ? null
				: (TreeSet<String>) supertypes.clone());
		copy.setSubtypes(
				subtypes == null ? null : (TreeSet<String>) subtypes.clone());

		copy.setDiagrams(diagrams);
		copy.setLinkedDocument(linkedDocument);

		TreeMap<StructuredNumber, PropertyInfo> copyProperties = new TreeMap<StructuredNumber, PropertyInfo>();

		for (PropertyInfo propI : properties.values()) {

			// ignore association roles
			if (!propI.isAttribute())
				continue;

			/*
			 * NOTE for cast: the cast should be safe, because propI belongs to
			 * this class, which is a GenericClassInfo
			 */
			GenericPropertyInfo genProp = (GenericPropertyInfo) propI;

			String newId = genProp.name() + "_propertyCopyFor_" + copyId;

			GenericPropertyInfo propCopy = genProp.createCopy(newId);

			/*
			 * Updating the "sequenceNumber" tagged value is not necessary
			 * because we didn't change it here.
			 */
			propCopy.setSequenceNumber(propI.sequenceNumber().createCopy(),
					false);
			propCopy.setInClass(copy);
			copyProperties.put(propCopy.sequenceNumber(), propCopy);
		}
		copy.setProperties(copyProperties);

		if (this.constraints == null) {
			copy.setConstraints(null);
		} else {
			copy.setConstraints((Vector<Constraint>) constraints.clone());
		}

		return copy;
	}

	/**
	 * Removes the property from this class where
	 * <code>property.sequenceNumber().equals(sn)</code>.
	 * 
	 * @param sn tbd
	 */
	public void removeByStructuredNumber(StructuredNumber sn) {
		StructuredNumber keyFound = null;

		for (StructuredNumber key : properties.keySet()) {
			if (key.equals(sn))
				keyFound = key;
		}

		if (keyFound != null)
			properties.remove(keyFound);

	}

	/**
	 * Adds the given list of new properties to this class. Their sequence
	 * numbers are used as-is. However, the sequence numbers of the already
	 * existing properties are shifted to "make space" for the new properties.
	 * The sequence/list of new properties will thus be placed before the
	 * existing properties, at the "top" of the list of class properties.
	 * 
	 * The behavior for adding a property that has the same name as an existing
	 * one is determined by a parameter.
	 * 
	 * @param newProps tbd
	 * @param duplicateHandling tbd
	 */
	public void addPropertiesAtTop(List<GenericPropertyInfo> newProps,
			PropertyCopyDuplicatBehaviorIndicator duplicateHandling) {

		if (newProps == null || newProps.size() == 0) {

			return;

		} else if (properties == null || properties.size() == 0) {

			properties = new TreeMap<StructuredNumber, PropertyInfo>();

			for (GenericPropertyInfo newProp : newProps) {
				this.addProperty(newProp, duplicateHandling);
			}

		} else {

			/*
			 * Now determine if the sequence numbers of existing properties need
			 * to be updated before adding the new properties.
			 */
			int minMajorComponentExistingProps = Integer.MAX_VALUE;
			int maxMajorComponentNewProps = Integer.MIN_VALUE;

			/*
			 * Identify the highest major component of the sequence numbers in
			 * the list of new properties.
			 */
			for (GenericPropertyInfo newProp : newProps) {

				StructuredNumber snNewProp = newProp.sequenceNumber();

				if (snNewProp.components[0] > maxMajorComponentNewProps) {
					maxMajorComponentNewProps = snNewProp.components[0];
				}
			}

			/*
			 * Now identify the lowest major component of the sequence numbers
			 * in the collection of existing properties.
			 */
			for (StructuredNumber snExistingProp : properties.keySet()) {

				if (snExistingProp.components[0] < minMajorComponentExistingProps) {
					minMajorComponentExistingProps = snExistingProp.components[0];
				}
			}

			if (maxMajorComponentNewProps < minMajorComponentExistingProps) {

				/*
				 * Perfect, we can just add the new properties to the collection
				 * of existing properties because the sequence numbers do not
				 * conflict.
				 */

			} else {

				/*
				 * We need to shift the sequence numbers of existing properties
				 * to make space for the new properties.
				 */
				int shift = maxMajorComponentNewProps
						- minMajorComponentExistingProps + 1;

				for (StructuredNumber snOfExistingProp : properties.keySet()) {

					snOfExistingProp.components[0] = snOfExistingProp.components[0]
							+ shift;
				}
			}

			// add the new properties
			for (GenericPropertyInfo newProp : newProps) {
				this.addProperty(newProp, duplicateHandling);
			}

		}
	}

	/**
	 * 
	 * @param sn tbd
	 * @return The property with the given sequence number, or <code>null</code>
	 *         if no such property exists.
	 */
	public GenericPropertyInfo propertyBySequenceNumber(StructuredNumber sn) {

		if (properties == null || properties.isEmpty()) {

			return null;

		} else {

			for (PropertyInfo pi : properties.values()) {

				if (pi.sequenceNumber().equals(sn)) {

					/*
					 * NOTE for cast: the cast should be safe, because pi
					 * belongs to this class which is a GenericClassInfo (this
					 * is true once the GenericModel has fully been
					 * parsed/created)
					 */
					return (GenericPropertyInfo) pi;
				}
			}
			return null;
		}
	}

	/**
	 * Adds the given list of new properties to this class. Their sequence
	 * numbers are used as-is - unless a property with different name but same
	 * sequence number already exists; in that case a suffix is added to the
	 * sequence number of the new property. The sequence/list of new properties
	 * will thus be merged with the existing properties.
	 * 
	 * The behavior for adding a property that has the same name as an existing
	 * one is determined by a parameter.
	 * 
	 * @param newProps tbd
	 * @param duplicateHandling tbd
	 */
	public void addPropertiesInSequence(List<GenericPropertyInfo> newProps,
			PropertyCopyDuplicatBehaviorIndicator duplicateHandling) {

		if (newProps == null || newProps.isEmpty()) {

			return;

		} else {

			if (this.properties == null) {
				properties = new TreeMap<StructuredNumber, PropertyInfo>();
			}

			if (this.properties.isEmpty()) {

				// simply add all new properties
				for (GenericPropertyInfo newProperty : newProps) {

					properties.put(newProperty.sequenceNumber(), newProperty);

					this.model.genPropertiesById.put(newProperty.id(),
							newProperty);
				}

			} else {

				// compute map with all existing props by name first
				Map<String, GenericPropertyInfo> existingPropsByName = new HashMap<String, GenericPropertyInfo>();

				for (PropertyInfo existingProp : this.properties.values()) {

					existingPropsByName.put(existingProp.name(),
							(GenericPropertyInfo) existingProp);
				}

				for (GenericPropertyInfo newProperty : newProps) {

					/*
					 * ensure that sequence number of the new property is unique
					 * within the set of existing properties
					 */
					StructuredNumber newpropsn = newProperty.sequenceNumber();
					if (this.properties.containsKey(newpropsn)) {
						newProperty.setSequenceNumber(
								newpropsn.createCopyWithSuffix(1), true);
					}

					GenericPropertyInfo existingPropWithSameName = existingPropsByName
							.get(newProperty.name());

					if (existingPropWithSameName == null) {

						properties.put(newProperty.sequenceNumber(),
								newProperty);
						existingPropsByName.put(newProperty.name(),
								newProperty);

						this.model.genPropertiesById.put(newProperty.id(),
								newProperty);

					} else if (duplicateHandling == PropertyCopyDuplicatBehaviorIndicator.ADD) {

						// add but log a warning
						result.addWarning(this, 30200, newProperty.name(),
								this.name());
						properties.put(newProperty.sequenceNumber(),
								newProperty);
						existingPropsByName.put(newProperty.name(),
								newProperty);

						this.model.genPropertiesById.put(newProperty.id(),
								newProperty);

					} else if (duplicateHandling == PropertyCopyDuplicatBehaviorIndicator.IGNORE) {

						/*
						 * alright, we do not add the new property to the
						 * properties of this class, but log a warning
						 */

						result.addWarning(this, 30201, newProperty.name(),
								this.name());

					} else if (duplicateHandling == PropertyCopyDuplicatBehaviorIndicator.IGNORE_UNRESTRICT) {

						/*
						 * alright, we do not add the new property to the
						 * properties of this class, but we need to "unrestrict"
						 * the existing one - and log a warning
						 */

						result.addWarning(this, 30202, newProperty.name(),
								this.name());

						existingPropWithSameName.setRestriction(false);

					} else if (duplicateHandling == PropertyCopyDuplicatBehaviorIndicator.OVERWRITE) {

						/*
						 * Remove the existing property and add the new one -
						 * and log a warning
						 */

						result.addWarning(this, 30203, newProperty.name(),
								this.name());

						properties.remove(
								existingPropWithSameName.sequenceNumber());
						this.model.genPropertiesById
								.remove(existingPropWithSameName.id());

						properties.put(newProperty.sequenceNumber(),
								newProperty);
						existingPropsByName.put(newProperty.name(),
								newProperty);

						this.model.genPropertiesById.put(newProperty.id(),
								newProperty);
					}
				}
			}
		}
	}

	/**
	 * Adds the given property to this class. Its sequence number is shifted so
	 * that the property is placed after the existing properties, at the
	 * "bottom" of the list of class properties.
	 * 
	 * The behavior for adding a property that has the same name as an existing
	 * one is determined by a parameter.
	 * 
	 * @param newProp tbd
	 * @param duplicateHandling tbd
	 */
	public void addPropertyAtBottom(GenericPropertyInfo newProp,
			PropertyCopyDuplicatBehaviorIndicator duplicateHandling) {

		List<GenericPropertyInfo> newProps = new ArrayList<GenericPropertyInfo>();
		newProps.add(newProp);

		addPropertiesAtBottom(newProps, duplicateHandling);
	}

	/**
	 * Adds the given list of new properties to this class. Their sequence
	 * numbers are shifted so that the sequence/list of new properties is placed
	 * after the existing properties, at the "bottom" of the list of class
	 * properties.
	 * 
	 * The behavior for adding a property that has the same name as an existing
	 * one is determined by a parameter.
	 * 
	 * @param newProps tbd
	 * @param duplicateHandling tbd
	 */
	public void addPropertiesAtBottom(List<GenericPropertyInfo> newProps,
			PropertyCopyDuplicatBehaviorIndicator duplicateHandling) {

		if (newProps == null || newProps.size() == 0) {

			return;

		} else {

			if (properties == null) {
				properties = new TreeMap<StructuredNumber, PropertyInfo>();
			}

			/*
			 * Sort list of new props by their sequence number so that we make
			 * good use of the sequence number value space.
			 */
			Collections.sort(newProps, new Comparator<GenericPropertyInfo>() {
				@Override
				public int compare(GenericPropertyInfo o1,
						GenericPropertyInfo o2) {
					return o1.sequenceNumber().compareTo(o2.sequenceNumber());
				}

			});

			for (GenericPropertyInfo newProp : newProps) {

				/*
				 * Determine if the sequence number of the new property needs to
				 * be updated before adding it.
				 */
				int minMajorComponentNewProp = newProp
						.sequenceNumber().components[0];
				int maxMajorComponentExistingProps = Integer.MIN_VALUE;

				/*
				 * Now identify the highest major component of the sequence
				 * numbers in the collection of existing properties.
				 */
				for (StructuredNumber snExistingProp : properties.keySet()) {

					if (snExistingProp.components[0] > maxMajorComponentExistingProps) {
						maxMajorComponentExistingProps = snExistingProp.components[0];
					}
				}

				if (minMajorComponentNewProp > maxMajorComponentExistingProps) {

					/*
					 * Perfect, we can just add the new properties to the
					 * collection of existing properties because the sequence
					 * numbers do not conflict.
					 */

				} else {

					StructuredNumber snNewProp = newProp.sequenceNumber();

					snNewProp.components[0] = maxMajorComponentExistingProps
							+ 1;
				}

				this.addProperty(newProp, duplicateHandling);
			}
		}
	}

	/**
	 * Puts the given tagged value into the existing tagged values cache,
	 * updating fields if requested via parameter.
	 * 
	 * @param tvName tbd
	 * @param tvValue tbd
	 * @param updateFields
	 *            true if class fields should be updated based upon information
	 *            from given tagged value, else false
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
	 * Adds the prefix to the 'id' of this class as well as the 'subtypes' (if
	 * not <code>null</code>) and the 'supertypes' (if not <code>null</code>).
	 * Does NOT update the 'globalIdentifier'.
	 * 
	 * NOTE: this method is used by the FeatureCatalogue target to ensure that
	 * IDs used in a reference model are unique to that model and do not get
	 * mixed up with the IDs of the input model.
	 * 
	 * @param prefix tbd
	 */
	public void addPrefixToModelElementIDs(String prefix) {

		this.id = prefix + id;

		if (subtypes != null) {
			TreeSet<String> tmp_subtypes = new TreeSet<String>();
			for (String id : subtypes) {
				tmp_subtypes.add(prefix + id);
			}
			this.subtypes = tmp_subtypes;
		}

		if (supertypes != null) {
			TreeSet<String> tmp_supertypes = new TreeSet<String>();
			for (String id : supertypes) {
				tmp_supertypes.add(prefix + id);
			}
			this.supertypes = tmp_supertypes;
		}
	}

	/**
	 * @param profiles
	 *            new set of profiles for this class; may be <code>null</code>
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
		case 1:
			return "(GenericClassInfo) When setting tagged value '$1$', a boolean value (either 'false' or 'true') was expected. Found '$2$' - cannot set class field(s) for this tagged value.";

		case 30200:
			return "(GenericModel.java) Duplicate property encountered. Property with name '$1$' already exists in class '$2$'. Because the duplicate property behavior is set to 'ADD' the duplicate will nevertheless be added, resulting in two properties with the same name in the class.";
		case 30201:
			return "(GenericModel.java) Duplicate property encountered. Property with name '$1$' already exists in class '$2$'. Because the duplicate property behavior is set to 'IGNORE' the duplicate will be ignored and the existing property kept. The isRestriction setting of the existing property will not be changed.";
		case 30202:
			return "(GenericModel.java) Duplicate property encountered. Property with name '$1$' already exists in class '$2$'. Because the duplicate property behavior is set to 'IGNORE_UNRESTRICT' the duplicate will be ignored and the existing property kept. In case that the existing property is a restriction, it is set to not being a restriction.";
		case 30203:
			return "(GenericModel.java) Duplicate property encountered. Property with name '$1$' already exists in class '$2$'. Because the duplicate property behavior is set to 'OVERWRITE' the duplicate/new property will overwrite the existing one.";

		default:
			return "(" + GenericClassInfo.class.getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
