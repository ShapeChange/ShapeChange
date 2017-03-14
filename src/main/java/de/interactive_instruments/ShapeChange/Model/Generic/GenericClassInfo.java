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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

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

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 */
public class GenericClassInfo extends ClassInfoImpl implements MessageSource {

	protected Options options = null;
	protected ShapeChangeResult result = null;
	protected GenericModel model = null;
	protected String id = null;
	protected String globalIdentifier = null;
	protected String name = null;

	protected boolean includePropertyType = true;
	protected boolean includeByValuePropertyType = false;
	protected boolean isCollection = false;
	protected boolean asDictionary = false;
	protected boolean asGroup = false;
	protected boolean asCharacterString = false;
	protected boolean hasNilReason = false;
	protected PackageInfo pkg = null;
	protected boolean isAbstract = false;
	protected boolean isLeaf = false;
	protected AssociationInfo assocClass = null;
	/**
	 * Set of ids of all base classes of this class
	 */
	protected TreeSet<String> supertypes = new TreeSet<String>();
	protected TreeSet<String> subtypes = new TreeSet<String>();
	protected ClassInfo baseClass = null;
	protected SortedMap<StructuredNumber, PropertyInfo> properties = new TreeMap<StructuredNumber, PropertyInfo>();
	/**
	 * Not null
	 */
	protected Vector<Constraint> constraints = new Vector<Constraint>();
	protected boolean suppressed = false;
	protected boolean asDictionaryGml33 = false;
	protected String xmlSchemaType = null;

	public GenericClassInfo(GenericModel model, String id, String name,
			int category) {

		this.model = model;
		this.options = model.options();
		this.result = model.result();
		this.id = id;
		this.name = name;
		this.category = category;

		// String stereotype;
		//
		// switch (category) {
		// case Options.FEATURE:
		// stereotype = "featuretype";
		// case Options.CODELIST:
		// stereotype = "codelist";
		// case Options.ENUMERATION:
		// stereotype = "enumeration";
		// case Options.DATATYPE:
		// stereotype = "datatype";
		// case Options.OBJECT:
		// stereotype = "type";
		// case Options.BASICTYPE:
		// stereotype = "basictype";
		// case Options.UNION:
		// stereotype = "union";
		// default:
		// stereotype = "";
		// }
		//
		// this.stereotypes = new HashSet<String>();
		// this.stereotypes.add(options.normalizeStereotype(stereotype));

	}

	/**
	 * @param xmlSchemaType
	 *            the xmlSchemaType to set
	 */
	public void setXmlSchemaType(String xmlSchemaType) {
		this.xmlSchemaType = xmlSchemaType;
	}

	/**
	 * @param includePropertyType
	 *            the includePropertyType to set
	 */
	public void setIncludePropertyType(boolean includePropertyType) {
		this.includePropertyType = includePropertyType;
	}

	/**
	 * @param includeByValuePropertyType
	 *            the includeByValuePropertyType to set
	 */
	public void setIncludeByValuePropertyType(
			boolean includeByValuePropertyType) {
		this.includeByValuePropertyType = includeByValuePropertyType;
	}

	/**
	 * @param isCollection
	 *            the isCollection to set
	 */
	public void setIsCollection(boolean isCollection) {
		this.isCollection = isCollection;
	}

	/**
	 * @param asDictionary
	 *            the asDictionary to set
	 */
	public void setAsDictionary(boolean asDictionary) {
		this.asDictionary = asDictionary;
	}

	/**
	 * @param asGroup
	 *            the asGroup to set
	 */
	public void setAsGroup(boolean asGroup) {
		this.asGroup = asGroup;
	}

	/**
	 * @param asCharacterString
	 *            the asCharacterString to set
	 */
	public void setAsCharacterString(boolean asCharacterString) {
		this.asCharacterString = asCharacterString;
	}

	/**
	 * @param hasNilReason
	 *            the hasNilReason to set
	 */
	public void setHasNilReason(boolean hasNilReason) {
		this.hasNilReason = hasNilReason;
	}

	/**
	 * @param pkg
	 *            the pkg to set
	 */
	public void setPkg(PackageInfo pkg) {
		this.pkg = pkg;
	}

	/**
	 * @param isAbstract
	 *            the isAbstract to set
	 */
	public void setIsAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	/**
	 * @param isLeaf
	 *            the isLeaf to set
	 */
	public void setIsLeaf(boolean isLeaf) {
		this.isLeaf = isLeaf;
	}

	/**
	 * @param isAssocClass
	 *            the isAssocClass to set
	 */
	public void setAssocInfo(AssociationInfo assocClass) {
		this.assocClass = assocClass;
	}

	/**
	 * @param supertypes
	 *            the supertypes to set
	 */
	public void setSupertypes(TreeSet<String> supertypes) {
		this.supertypes = supertypes;
	}

	/**
	 * @param subtypes
	 *            the subtypes to set
	 */
	public void setSubtypes(TreeSet<String> subtypes) {
		this.subtypes = subtypes;
	}

	/**
	 * @param baseClass
	 *            the baseClass to set
	 */
	public void setBaseClass(ClassInfo baseClass) {
		this.baseClass = baseClass;
	}

	/**
	 * @param properties
	 *            the properties to set
	 */
	public void setProperties(
			SortedMap<StructuredNumber, PropertyInfo> properties) {
		this.properties = properties;
	}

	/**
	 * @param list
	 *            the constraints to set
	 */
	public void setConstraints(Vector<Constraint> list) {
		this.constraints = list;
	}

	/**
	 * @param suppressed
	 *            the suppressed to set
	 */
	public void setSuppressed(boolean suppressed) {
		this.suppressed = suppressed;
	}

	/**
	 * @param asDictionaryGml33
	 *            the asDictionaryGml33 to set
	 */
	public void setAsDictionaryGml33(boolean asDictionaryGml33) {
		this.asDictionaryGml33 = asDictionaryGml33;
	}

	/**
	 * In the generic model, the values are actively managed and stored in the
	 * model elements.
	 * 
	 * @return locally stored descriptor value
	 */
	@Override
	public String aliasName() {
		return aliasName;
	}

	/**
	 * In the generic model, the values are actively managed and stored in the
	 * model elements.
	 * 
	 * @return locally stored descriptor value
	 */
	@Override
	public String primaryCode() {
		return primaryCode;
	}

	/**
	 * In the generic model, the values are actively managed and stored in the
	 * model elements.
	 * 
	 * @return locally stored descriptor value
	 */
	@Override
	public String definition() {
		return definition;
	}

	/**
	 * In the generic model, the values are actively managed and stored in the
	 * model elements.
	 * 
	 * @return locally stored descriptor value
	 */
	@Override
	public String description() {
		return description;
	}

	/**
	 * In the generic model, the values are actively managed and stored in the
	 * model elements.
	 * 
	 * @return locally stored descriptor value
	 */
	@Override
	public String language() {
		return language;
	}

	/**
	 * In the generic model, the values are actively managed and stored in the
	 * model elements.
	 * 
	 * @return locally stored descriptor value
	 */
	@Override
	public String legalBasis() {
		return legalBasis;
	}

	/**
	 * In the generic model, the values are actively managed and stored in the
	 * model elements.
	 * 
	 * @return locally stored descriptor value
	 */
	@Override
	public String[] examples() {
		return examples;
	}

	/**
	 * In the generic model, the values are actively managed and stored in the
	 * model elements.
	 * 
	 * @return locally stored descriptor value
	 */
	@Override
	public String[] dataCaptureStatements() {
		return dataCaptureStatements;
	}

	/**
	 * In the generic model, always return an empty string, the other
	 * descriptors should be used instead.
	 * 
	 * @return locally stored descriptor value
	 */
	@Override
	public String documentation() {
		return "";
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.PackageInfoImpl#fullName()
	 */
	@Override
	public String fullName() {
		if (pkg != null && name != null)
			return pkg.fullName() + "::" + name;
		else
			return null;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.Info#id()
	 */
	public String id() {
		return id;
	}

	@Override
	public String globalIdentifier() {
		return globalIdentifier;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.Info#model()
	 */
	public GenericModel model() {
		return model;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.Info#name()
	 */
	public String name() {
		return name;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.Info#options()
	 */
	public Options options() {
		return options;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.Info#result()
	 */
	public ShapeChangeResult result() {
		return result;
	}

	/**
	 * Set the value of this descriptor in the generic model. This invalidates
	 * the derived documentation so that it is derived again when needed
	 */
	public void setAliasName(String aliasName) {
		this.aliasName = aliasName;
	}

	/**
	 * Set the value of this descriptor in the generic model. This invalidates
	 * the derived documentation so that it is derived again when needed
	 */
	public void setDefinition(String definition) {
		this.definition = definition;
	}

	/**
	 * Set the value of this descriptor in the generic model. This invalidates
	 * the derived documentation so that it is derived again when needed
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Set the value of this descriptor in the generic model. This invalidates
	 * the derived documentation so that it is derived again when needed
	 */
	public void setLanguage(String language) {
		this.language = language;
	}

	/**
	 * Set the value of this descriptor in the generic model. This invalidates
	 * the derived documentation so that it is derived again when needed
	 */
	public void setPrimaryCode(String primaryCode) {
		this.primaryCode = primaryCode;
	}

	/**
	 * Set the value of this descriptor in the generic model. This invalidates
	 * the derived documentation so that it is derived again when needed
	 */
	public void setLegalBasis(String legalBasis) {
		this.legalBasis = legalBasis;
	}

	/**
	 * Set the value of this descriptor in the generic model. This invalidates
	 * the derived documentation so that it is derived again when needed
	 */
	public void setExamples(String[] examples) {
		this.examples = examples;
	}

	/**
	 * Set the value of this descriptor in the generic model. This invalidates
	 * the derived documentation so that it is derived again when needed
	 */
	public void setDataCaptureStatements(String[] dataCaptureStatements) {
		this.dataCaptureStatements = dataCaptureStatements;
	}

	/**
	 * @param id
	 */
	public void setId(String id) {
		this.id = id;

	}

	/**
	 * @param globalIdentifier
	 */
	public void setGlobalIdentifier(String globalIdentifier) {
		this.globalIdentifier = options.internalize(globalIdentifier);
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
		this.name = name;

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
	 * @param tvName
	 * @param tvValue
	 */
	private void updateFieldsForTaggedValue(String tvName, String tvValue) {

		// TODO add more updates for relevant tagged values

		if (tvName.equalsIgnoreCase("gmlAsCharacterString")) {

			if (tvValue.equalsIgnoreCase("true")) {
				this.setAsCharacterString(true);
			} else if (tvValue.equalsIgnoreCase("false")) {
				this.setAsCharacterString(false);
			} else {
				result.addWarning(this, 1, tvName, tvValue);
			}
		} else if (tvName.equalsIgnoreCase("asDictionary")) {

			if (tvValue.equalsIgnoreCase("true")) {
				this.setAsDictionary(true);
				this.setAsDictionaryGml33(true);
			} else if (tvValue.equalsIgnoreCase("false")) {
				this.setAsDictionary(false);
				this.setAsDictionaryGml33(false);
			} else {
				result.addWarning(this, 1, tvName, tvValue);
			}
		} else if (tvName.equalsIgnoreCase("gmlAsGroup")) {

			if (tvValue.equalsIgnoreCase("true")) {
				this.setAsGroup(true);
			} else if (tvValue.equalsIgnoreCase("false")) {
				this.setAsGroup(false);
			} else {
				result.addWarning(this, 1, tvName, tvValue);
			}
		} else if (tvName.equalsIgnoreCase("byValuePropertyType")) {

			if (tvValue.equalsIgnoreCase("true")) {
				this.setIncludeByValuePropertyType(true);
			} else if (tvValue.equalsIgnoreCase("false")) {
				this.setIncludeByValuePropertyType(false);
			} else {
				result.addWarning(this, 1, tvName, tvValue);
			}
		} else if (tvName.equalsIgnoreCase("noPropertyType")) {

			if (tvValue.equalsIgnoreCase("true")) {
				this.setIncludePropertyType(false);
			} else if (tvValue.equalsIgnoreCase("false")) {
				this.setIncludePropertyType(true);
			} else {
				result.addWarning(this, 1, tvName, tvValue);
			}
		} else if (tvName.equalsIgnoreCase("isCollection")) {

			if (tvValue.equalsIgnoreCase("true")) {
				this.setIsCollection(true);
			} else if (tvValue.equalsIgnoreCase("false")) {
				this.setIsCollection(false);
			} else {
				result.addWarning(this, 1, tvName, tvValue);
			}
		} else if (tvName.equalsIgnoreCase("xmlSchemaType")) {

			this.setXmlSchemaType(tvValue);

		} else if (tvName.equalsIgnoreCase("alias")) {

			this.setAliasName(tvValue);

		} else if (tvName.equalsIgnoreCase("documentation")) {

			// we map this to the descriptor 'definition'
			this.setDefinition(tvValue);

		} else if (tvName.equalsIgnoreCase("suppress")) {

			if (tvValue.equalsIgnoreCase("true")) {
				this.setSuppressed(true);
			} else if (tvValue.equalsIgnoreCase("false")) {
				this.setSuppressed(false);
			} else {
				result.addWarning(this, 1, tvName, tvValue);
			}
		}
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.ClassInfo#hasNilReason()
	 */
	public boolean hasNilReason() {
		return hasNilReason;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.ClassInfo#pkg()
	 */
	public PackageInfo pkg() {
		return pkg;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.ClassInfo#isAbstract()
	 */
	public boolean isAbstract() {
		return isAbstract;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.ClassInfo#isLeaf()
	 */
	public boolean isLeaf() {
		return isLeaf;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.ClassInfo#isAssocClass()
	 */
	public AssociationInfo isAssocClass() {
		return assocClass;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.ClassInfo#supertypes()
	 */
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

	public boolean hasConstraints() {
		if (this.constraints == null || this.constraints.isEmpty()) {
			return false;
		} else {
			return true;
		}
	}

	public boolean hasSubtypes() {
		if (subtypes != null && subtypes.size() > 0)
			return true;
		else
			return false;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.ClassInfo#baseClass()
	 */
	public ClassInfo baseClass() {
		return baseClass;
	}

	/**
	 * 
	 * @see de.interactive_instruments.ShapeChange.Model.ClassInfo#properties()
	 */
	@Override
	public SortedMap<StructuredNumber, PropertyInfo> properties() {
		if (properties == null) {
			return new TreeMap<StructuredNumber, PropertyInfo>();
		} else {
			return properties;
		}
	}

	/**
	 * 
	 * @see de.interactive_instruments.ShapeChange.Model.ClassInfo#constraints()
	 */
	public List<Constraint> constraints() {
		if (constraints == null) {
			return new Vector<Constraint>(1);
		} else {
			return constraints;
		}
	}

	/**
	 * Find the property given by its name in this class or (if not present
	 * there) recursively in its base classes / supertypes.
	 * 
	 * @see de.interactive_instruments.ShapeChange.Model.ClassInfo#property(java.
	 *      lang.String)
	 */
	public PropertyInfo property(String name) {
		// Search in own properties
		for (PropertyInfo pi : properties.values()) {
			if (pi.name().equals(name))
				return pi;
		}
		// Go and search in base class, if it exists
		Map<String, ClassInfo> baseClassesById = new HashMap<String, ClassInfo>();
		if (baseClass != null) {
			baseClassesById.put(baseClass.id(), baseClass);
		}
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
	 * 
	 * @see de.interactive_instruments.ShapeChange.Model.ClassInfo#operation(java
	 *      .lang.String, java.lang.String[])
	 */
	public OperationInfo operation(String name, String[] types) {
		// currently not supported
		return null;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.ClassInfoImpl#xmlSchemaType()
	 */
	@Override
	public String xmlSchemaType() {

		return xmlSchemaType;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.ClassInfoImpl#includeByValuePropertyType()
	 */
	@Override
	public boolean includeByValuePropertyType() {

		return includeByValuePropertyType;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.ClassInfoImpl#includePropertyType()
	 */
	@Override
	public boolean includePropertyType() {

		return includePropertyType;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.ClassInfoImpl#isCollection()
	 */
	@Override
	public boolean isCollection() {

		return isCollection;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.ClassInfoImpl#asDictionary()
	 */
	@Override
	public boolean asDictionary() {

		return asDictionary;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.ClassInfoImpl#asDictionaryGml33()
	 */
	@Override
	public boolean asDictionaryGml33() {

		return asDictionaryGml33;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.ClassInfoImpl#asGroup()
	 */
	@Override
	public boolean asGroup() {

		return asGroup;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.ClassInfoImpl#asCharacterString()
	 */
	@Override
	public boolean asCharacterString() {

		return asCharacterString;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.ClassInfoImpl#suppressed()
	 */
	@Override
	public boolean suppressed() {

		return suppressed;
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

	/**
	 * @param category
	 */
	public void setCategory(int category) {
		this.category = category;
	}

	public void removeSubtype(String subtypeId) {
		this.subtypes.remove(subtypeId);
	}

	/**
	 * Removes the supertype with given ID from the list of supertypes defined
	 * for this class.
	 * 
	 * @param supertypeId
	 */
	public void removeSupertype(String supertypeId) {
		if (supertypes == null || supertypes.size() == 0 || supertypeId == null)
			return;
		else
			this.supertypes.remove(supertypeId);
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
	 * @param newProperty
	 * @param duplicateHandling
	 */
	public void addProperty(GenericPropertyInfo newProperty,
			PropertyCopyDuplicatBehaviorIndicator duplicateHandling) {

		if (this.properties == null) {
			properties = new TreeMap<StructuredNumber, PropertyInfo>();
		}

		GenericPropertyInfo existingPropWithSameName = this
				.propertyByName(newProperty.name());

		if (existingPropWithSameName == null) {

			properties.put(newProperty.sequenceNumber(), newProperty);

			this.model.genPropertiesById.put(newProperty.id(), newProperty);

		} else if (duplicateHandling == PropertyCopyDuplicatBehaviorIndicator.ADD) {

			// add but log a warning
			result.addWarning(null, 30200, newProperty.name(), this.name());
			properties.put(newProperty.sequenceNumber(), newProperty);

			this.model.genPropertiesById.put(newProperty.id(), newProperty);

		} else if (duplicateHandling == PropertyCopyDuplicatBehaviorIndicator.IGNORE) {

			/*
			 * alright, we do not add the new property to the properties of this
			 * class, but log a warning
			 */

			result.addWarning(null, 30201, newProperty.name(), this.name());

		} else if (duplicateHandling == PropertyCopyDuplicatBehaviorIndicator.IGNORE_UNRESTRICT) {

			/*
			 * alright, we do not add the new property to the properties of this
			 * class, but we need to "unrestrict" the existing one - and log a
			 * warning
			 */

			result.addWarning(null, 30202, newProperty.name(), this.name());

			existingPropWithSameName.setRestriction(false);

		} else if (duplicateHandling == PropertyCopyDuplicatBehaviorIndicator.OVERWRITE) {

			/*
			 * Remove the existing property and add the new one - and log a
			 * warning
			 */

			result.addWarning(null, 30203, newProperty.name(), this.name());

			properties.remove(existingPropWithSameName.sequenceNumber());

			this.model.genPropertiesById.remove(existingPropWithSameName.id());

			properties.put(newProperty.sequenceNumber(), newProperty);

			this.model.genPropertiesById.put(newProperty.id(), newProperty);
		}
	}

	/**
	 * Adds the new properties to the set of properties of this class. The
	 * behavior for adding a property that has the same name as an existing one
	 * is determined by a parameter.
	 * 
	 * WARNING: if a new property does not have the same name as an existing one
	 * but the same sequence number, this will overwrite the existing property
	 * with that sequence number!
	 * 
	 * @param newProperty
	 * @param duplicateHandling
	 */
	public void addProperties(List<GenericPropertyInfo> newProperties,
			PropertyCopyDuplicatBehaviorIndicator duplicateHandling) {

		if (this.properties == null) {
			properties = new TreeMap<StructuredNumber, PropertyInfo>();
		}

		if (this.properties.isEmpty()) {

			// simply add all new properties
			for (GenericPropertyInfo newProperty : newProperties) {

				properties.put(newProperty.sequenceNumber(), newProperty);

				this.model.genPropertiesById.put(newProperty.id(), newProperty);
			}

		} else {

			// compute map with all existing props by name first
			Map<String, GenericPropertyInfo> existingPropsByName = new HashMap<String, GenericPropertyInfo>();

			for (PropertyInfo existingProp : this.properties.values()) {

				existingPropsByName.put(existingProp.name(),
						(GenericPropertyInfo) existingProp);
			}

			for (GenericPropertyInfo newProperty : newProperties) {

				GenericPropertyInfo existingPropWithSameName = existingPropsByName
						.get(newProperty.name());

				if (existingPropWithSameName == null) {

					properties.put(newProperty.sequenceNumber(), newProperty);
					existingPropsByName.put(newProperty.name(), newProperty);

					this.model.genPropertiesById.put(newProperty.id(),
							newProperty);

				} else if (duplicateHandling == PropertyCopyDuplicatBehaviorIndicator.ADD) {

					// add but log a warning
					result.addWarning(null, 30200, newProperty.name(),
							this.name());
					properties.put(newProperty.sequenceNumber(), newProperty);
					existingPropsByName.put(newProperty.name(), newProperty);

					this.model.genPropertiesById.put(newProperty.id(),
							newProperty);

				} else if (duplicateHandling == PropertyCopyDuplicatBehaviorIndicator.IGNORE) {

					/*
					 * alright, we do not add the new property to the properties
					 * of this class, but log a warning
					 */

					result.addWarning(null, 30201, newProperty.name(),
							this.name());

				} else if (duplicateHandling == PropertyCopyDuplicatBehaviorIndicator.IGNORE_UNRESTRICT) {

					/*
					 * alright, we do not add the new property to the properties
					 * of this class, but we need to "unrestrict" the existing
					 * one - and log a warning
					 */

					result.addWarning(null, 30202, newProperty.name(),
							this.name());

					existingPropWithSameName.setRestriction(false);

				} else if (duplicateHandling == PropertyCopyDuplicatBehaviorIndicator.OVERWRITE) {

					/*
					 * Remove the existing property and add the new one - and
					 * log a warning
					 */

					result.addWarning(null, 30203, newProperty.name(),
							this.name());

					properties
							.remove(existingPropWithSameName.sequenceNumber());
					this.model.genPropertiesById
							.remove(existingPropWithSameName.id());

					properties.put(newProperty.sequenceNumber(), newProperty);
					existingPropsByName.put(newProperty.name(), newProperty);

					this.model.genPropertiesById.put(newProperty.id(),
							newProperty);
				}
			}
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

	public void addConstraints(List<Constraint> list) {
		if (list == null || list.isEmpty())
			return;
		else {
			if (this.constraints == null) {
				this.constraints = new Vector<Constraint>();
			}
			this.constraints.addAll(list);
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

	/**
	 * NOTE: does not update baseclass info
	 * 
	 * @param currentId
	 * @param newId
	 */
	public void updateSupertypeId(String currentId, String newId) {
		if (supertypes != null && supertypes.contains(currentId)) {
			supertypes.remove(currentId);
			supertypes.add(newId);
		}
	}

	/**
	 * WARNING: creates copies of attributes, but NOT of association roles
	 * 
	 * @param copyId
	 * @param copyName
	 * @param copyCategory
	 * @param useProxies
	 *            <code>true</code> if copies of attributes shall be created as
	 *            GenericPropertyInfoProxy instances, else <code>false</code>.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public GenericClassInfo createCopy(String copyId, String copyName,
			int copyCategory) {

		GenericClassInfo copy = new GenericClassInfo(this.model, copyId,
				copyName, copyCategory);

		copy.setGlobalIdentifier(globalIdentifier);
		copy.setAliasName(aliasName);
		copy.setDefinition(definition);
		copy.setDescription(description);
		copy.setPrimaryCode(primaryCode);
		copy.setLanguage(language);
		copy.setLegalBasis(legalBasis);
		copy.setDataCaptureStatements(dataCaptureStatements);
		copy.setExamples(examples);
		copy.setStereotypes(stereotypesCache);
		copy.setTaggedValues(taggedValuesCache, false);
		copy.setIncludePropertyType(includePropertyType);
		copy.setIncludeByValuePropertyType(includeByValuePropertyType);
		copy.setIsCollection(isCollection);
		copy.setAsDictionary(asDictionary);
		copy.setAsGroup(asGroup);
		copy.setAsCharacterString(asCharacterString);
		copy.setHasNilReason(hasNilReason);
		copy.setPkg(pkg);
		copy.setIsAbstract(isAbstract);
		copy.setIsLeaf(isLeaf);
		copy.setAssocInfo(assocClass);
		copy.setSupertypes((TreeSet<String>) supertypes.clone());
		copy.setSubtypes((TreeSet<String>) subtypes.clone());
		copy.setBaseClass(baseClass);

		copy.setDiagrams(diagrams);

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

		copy.setConstraints((Vector<Constraint>) constraints.clone());
		copy.setSuppressed(suppressed);
		copy.setAsDictionaryGml33(asDictionaryGml33);
		copy.setXmlSchemaType(xmlSchemaType);

		return copy;
	}

	/**
	 * Removes the property from this class where
	 * <code>property.sequenceNumber().equals(sn)</code>.
	 * 
	 * @param sn
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
	 * @param newProps
	 * @param duplicateHandling
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
	 * @param name
	 * @return The property with the given name, or <code>null</code> if no such
	 *         property exists.
	 */
	public GenericPropertyInfo propertyByName(String name) {

		if (properties == null || properties.isEmpty()) {

			return null;

		} else {

			for (PropertyInfo pi : properties.values()) {

				if (pi.name().equals(name)) {

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
	 * 
	 * @param sn
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
	 * numbers are used as-is. The sequence/list of new properties will thus be
	 * merged with the existing properties. Sequence numbers may be duplicate
	 * (though still be different objects).
	 * 
	 * The behavior for adding a property that has the same name as an existing
	 * one is determined by a parameter.
	 * 
	 * @param newProps
	 * @param duplicateHandling
	 */
	public void addPropertiesInSequence(List<GenericPropertyInfo> newProps,
			PropertyCopyDuplicatBehaviorIndicator duplicateHandling) {

		if (newProps == null || newProps.isEmpty()) {

			return;

		} else {

			// if (properties == null) {
			// properties = new TreeMap<StructuredNumber, PropertyInfo>();
			// }

			// add the new properties
			this.addProperties(newProps, duplicateHandling);

			// // add the new properties
			// for (GenericPropertyInfo newProp : newProps) {
			// this.addProperty(newProp, duplicateHandling);
			// }
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
	 * @param newProps
	 * @param duplicateHandling
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
	 * @param newProps
	 * @param duplicateHandling
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

	public String message(int mnr) {

		switch (mnr) {
		case 1:
			return "(GenericClassInfo) When setting tagged value '$1$', a boolean value (either 'false' or 'true') was expected. Found '$2$' - cannot set class field(s) for this tagged value.";

		default:
			return "(Unknown message)";
		}
	}

	/**
	 * @param tvName
	 * @param tvValue
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
	 * @param prefix
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
}
