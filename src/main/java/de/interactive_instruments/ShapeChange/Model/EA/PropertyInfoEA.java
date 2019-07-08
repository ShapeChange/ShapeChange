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
 * (c) 2002-2012 interactive instruments GmbH, Bonn, Germany
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

package de.interactive_instruments.ShapeChange.Model.EA;

import java.util.List;
import java.util.Locale;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;
import org.sparx.Attribute;
import org.sparx.AttributeConstraint;
import org.sparx.AttributeTag;
import org.sparx.Collection;
import org.sparx.ConnectorEnd;
import org.sparx.RoleTag;

import de.interactive_instruments.ShapeChange.Multiplicity;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.StructuredNumber;
import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.Model.AssociationInfo;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.Descriptor;
import de.interactive_instruments.ShapeChange.Model.LangString;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfoImpl;
import de.interactive_instruments.ShapeChange.Model.Qualifier;
import de.interactive_instruments.ShapeChange.Model.StereotypeNormalizer;

public class PropertyInfoEA extends PropertyInfoImpl implements PropertyInfo {

	/**
	 * Flag used to prevent duplicate retrieval/computation of the alias of this
	 * property.
	 */
	protected boolean aliasAccessed = false;
	/**
	 * Flag used to prevent duplicate retrieval/computation of the documentation
	 * of this property.
	 */
	protected boolean documentationAccessed = false;
	/**
	 * Flag used to prevent duplicate retrieval/computation of the global
	 * identifier of this property.
	 */
	protected boolean globalIdentifierAccessed = false;

	/** Access to the document object */
	protected EADocument document = null;

	/** Class the property belongs to */
	protected ClassInfoEA classInfo = null;

	/**
	 * Model-unique id. For attributes this is the ID from EA Attributes
	 * prefixed by the class id. For roles this is derived from the Connector
	 * and a sign.
	 */
	protected String eaPropertyId = null;

	/** Name of the property */
	protected String eaName = null;

	/** Type information */
	protected Type typeInfo = new Type();
	protected ClassInfoEA typeClassInfo = null;

	/** EA attribute object, if this is an attribute */
	protected Attribute eaAttribute = null;

	protected int eaAttributeId = -1;

	/** Association context and EA ConnectorEnd if this is a role */
	AssociationInfoEA associationInfo = null;
	boolean reversedAssoc = false;
	ConnectorEnd eaConnectorEnd = null;

	/** Sequence number of property */
	protected StructuredNumber sequenceNumber = new StructuredNumber(
			Integer.MIN_VALUE);

	/** Multiplicity of property */
	protected Multiplicity multiplicity = new Multiplicity();

	/** Initial value cache */
	protected String initialValueCache = null;

	/** Is read only cache */
	protected Boolean isReadOnlyCache = null;

	/** Is derived cache */
	protected Boolean isDerivedCache = null;

	/** Is navigable cache */
	protected Boolean isNavigableCache = null;

	/** Aggregation type cache */
	protected String aggregationTypeCache = null;

	/** Cache for ordering in property */
	protected Boolean isOrderedCache = null;

	/** Cache for uniqueness in property */
	protected Boolean isUniqueCache = null;

	/** Cache for isOwned in property */
	protected Boolean isOwnedCache = null;

	/** Cache set for stereotypes */
	// this map is already defined in InfoImpl

	/** Cache map for tagged values */
	// this map is already defined in InfoImpl

	/** Cache set of constraints */
	protected Vector<Constraint> constraintsCache = null;

	/** Create a PropertyInfo object given an EA Attribute. */
	public PropertyInfoEA(EADocument doc, ClassInfoEA ci, Attribute attr) {

		// Record references ...
		document = doc;
		classInfo = ci;
		eaAttribute = attr;
		eaAttributeId = eaAttribute.GetAttributeID();

		// The Id
		eaPropertyId = ci.id();
		eaPropertyId += "_";
		eaPropertyId += Integer.valueOf(eaAttributeId).toString();

		// Property name
		eaName = eaAttribute.GetName();
		if (eaName != null)
			eaName = eaName.trim();

		// Assign sequence number. */
		String s = taggedValue("sequenceNumber");
		if (s != null && s.length() > 0 && s.matches("[0-9\\.]*")) {
			sequenceNumber = new StructuredNumber(s);
		} else {
			sequenceNumber = new StructuredNumber(this
					.getNextNumberForAttributeWithoutExplicitSequenceNumber());

			/*
			 * 2015-09-29 JE: tests using EA v12 showed that GetPos returns 0
			 * for all attributes in a class, thus we stepped away from using
			 * that method and rely on the global counter instead.
			 */
			// // Use the "Pos" attribute of the EA model.
			// int pos = eaAttribute.GetPos();
			// PropertyInfo piTemp = classInfo.properties()
			// .get(new StructuredNumber((Integer.MIN_VALUE / 2) + pos));
			// if (piTemp != null) {
			// while (piTemp != null) {
			// sequenceNumber = new StructuredNumber(
			// (document.globalSequenceNumber++)
			// + (Integer.MIN_VALUE / 4));
			// piTemp = classInfo.properties().get(sequenceNumber);
			// }
			// } else
			// sequenceNumber = new StructuredNumber((Integer.MIN_VALUE / 2) +
			// pos);
		}

		// Type info
		int typeid = eaAttribute.GetClassifierID();
		typeInfo.id = String.valueOf(typeid);
		typeInfo.name = eaAttribute.GetType().trim();
		typeClassInfo = document.fClassById.get(typeInfo.id);
		if (typeClassInfo == null)
			typeClassInfo = document.fClassByName.get(typeInfo.name);
		if (typeClassInfo != null) {
			typeInfo.name = typeClassInfo.name();
			typeInfo.id = typeClassInfo.id();
		}

		// Multiplicity
		// Attribute multiplicity is delivered by EA in two separate fields,
		// which, if empty, are meant as bound value 1.
		String[] bounds = new String[2];
		bounds[0] = eaAttribute.GetLowerBound().trim();
		bounds[1] = eaAttribute.GetUpperBound().trim();
		int[] mult = { 0, 0 };
		for (int i = 0; i < 2; i++) {
			if (bounds[i].length() == 0)
				mult[i] = 1;
			else if (bounds[i].equals("*"))
				mult[i] = i == 0 ? 0 : Integer.MAX_VALUE;
			else {
				try {
					mult[i] = Integer.parseInt(bounds[i]);
				} catch (NumberFormatException e) {
					MessageContext mc = document.result.addWarning(null, 1003,
							bounds[i]);
					if (mc != null)
						mc.addDetail(null, 400, "Class", inClass().fullName());
					mult[i] = 1;
				}
			}
		}
		multiplicity.minOccurs = mult[0];
		multiplicity.maxOccurs = mult[1];

		// Trace
		document.result.addDebug(null, 10013, "property", id(), name());
	}

	/**
	 * Create a PropertyInfo object given an AssociationInfo and a ConnectorEnd.
	 */
	public PropertyInfoEA(EADocument doc, ClassInfoEA ci, AssociationInfoEA ai,
			boolean reversed, ConnectorEnd eaCE, ClassInfoEA tci) {

		// Record references ...
		document = doc;
		classInfo = ci;
		associationInfo = ai;
		reversedAssoc = reversed;
		eaConnectorEnd = eaCE;

		// Id of property. Since ConnectorEnds have no Id, we resort to the
		// Id of the Connector and prefix a letter S or T.
		eaPropertyId = (reversed ? "S" : "T")
				+ Integer.valueOf(ai.eaConnectorId).toString();

		// Name of role
		eaName = eaCE.GetRole();
		if (eaName == null || eaName.length() == 0) {
			eaName = "role_" + eaPropertyId;
		}
		eaName = eaName.trim();

		// Type info
		typeClassInfo = tci;
		typeInfo.id = tci.id();
		typeInfo.name = tci.name();

		// Assign sequence number.
		String s = taggedValue("sequenceNumber");
		if (s != null && s.length() > 0) {
			sequenceNumber = new StructuredNumber(s);
		} else {
			sequenceNumber = new StructuredNumber(
					getNextNumberForAssociationRoleWithoutExplicitSequenceNumber());
		}

		// Multiplicity
		// Full UML cardinality syntax can be applied here, especially also
		// lists of values and ranges. We will aggregate this to a single range.
		String card = eaConnectorEnd.GetCardinality();
		String[] ranges = card.split(",");
		int minv = Integer.MAX_VALUE;
		int maxv = Integer.MIN_VALUE;
		int lower, upper;
		for (int i = 0; i < ranges.length; i++) {
			if (ranges[i].indexOf("..") > 0) {
				// If it is a range, separated by '..'
				String[] minmax = ranges[i].split("\\.\\.", 2);
				lower = Integer.parseInt(minmax[0]);
				if (minmax[1].equals("*") || minmax[1].length() == 0) {
					upper = Integer.MAX_VALUE;
				} else {
					try {
						upper = Integer.parseInt(minmax[1]);
					} catch (NumberFormatException e) {
						MessageContext mc = document.result.addWarning(null,
								1003, minmax[1]);
						if (mc != null)
							mc.addDetail(null, 400, "Class",
									inClass().fullName());
						upper = Integer.MAX_VALUE;
					}
				}
			} else {
				// Not a range. Must be a single value.
				if (ranges[i].length() == 0) {
					// default for unspecified multiplicity is 1
					lower = 1;
					upper = 1;
				} else if (ranges[i].equals("*")) {
					lower = 0;
					upper = Integer.MAX_VALUE;
				} else {
					try {
						lower = Integer.parseInt(ranges[i]);
						upper = lower;
					} catch (NumberFormatException e) {
						MessageContext mc = document.result.addWarning(null,
								1003, ranges[i]);
						if (mc != null)
							mc.addDetail(null, 400, "Class",
									inClass().fullName());
						lower = 0;
						upper = Integer.MAX_VALUE;
					}
				}
			}
			if (lower < minv && lower >= 0)
				minv = lower;
			if (upper < 0)
				maxv = Integer.MAX_VALUE;
			if (upper > maxv)
				maxv = upper;
		}
		multiplicity.minOccurs = minv;
		multiplicity.maxOccurs = maxv;

		// Analyse qualifiers
		String qualifierString = eaConnectorEnd.GetQualifier();
		if (qualifierString != null && !qualifierString.isEmpty()) {
			qualifiers = new Vector<Qualifier>();
			for (String st : qualifierString.split(";")) {
				st = st.trim();
				int idx = st.indexOf(":");
				Qualifier q = new Qualifier();
				qualifiers.addElement(q);
				if (idx > 0) {
					q.name = st.substring(0, idx).trim();
					q.type = st.substring(idx + 1).trim();
				} else {
					q.name = st.trim();
					// leave type null
				}
			}
		}

		// Trace
		document.result.addDebug(null, 10013, "property", id(), name());
	}

	/** Return EA model object. */
	public Model model() {
		return document;
	} // model()

	/** Return options and configuration object. */
	public Options options() {
		return document.options;
	} // options()

	/** Return result object for error reporting. */
	public ShapeChangeResult result() {
		return document.result;
	} // result()

	/** Return multiplicity of property */
	public Multiplicity cardinality() {
		return multiplicity;
	} // cardinality()

	@Override
	public ClassInfo inClass() {
		return classInfo;
	} // incClass()

	/** Make the property belong to the class given. */
	public void inClass(ClassInfo ci) {
		// TODO Note that this is an incomplete implementation of this method,
		// which I just copied from its Xmi10 cousin. A real class ownership
		// redirection would also have to consider references from the class
		// context. Maybe this method should be abolished in the long run by
		// making the PropertyInfo Ctor responsible for setting up all
		// necessary links.
		classInfo = (ClassInfoEA) ci;
	} // inClass()

	/**
	 * Return the initialValue of the property in case such a thing is specified
	 * in the model, null otherwise. This works only for attributes.
	 */
	public String initialValue() {
		// Only check if not already known and if this is an attribute
		if (initialValueCache == null && isAttribute()) {

			// Fetch from EA model
			initialValueCache = eaAttribute.GetDefault();
			// GetDefault() returns empty string, not null
			if (initialValueCache.equals("")) {
				initialValueCache = null;
			}

			// Normalize
			if (initialValueCache != null) {
				initialValueCache = initialValueCache.trim();
				initialValueCache = StringUtils.removeStart(initialValueCache,
						"\"");
				initialValueCache = StringUtils.removeEnd(initialValueCache,
						"\"");
				String iv = initialValueCache.toLowerCase();
				if (iv.equals("true"))
					initialValueCache = "true";
				if (iv.equals("false"))
					initialValueCache = "false";
			}
		}
		return initialValueCache;
	} // initialValue()

	/**
	 * Retrieves information on whether the property is read only directly via
	 * the EA API.
	 * 
	 * @see de.interactive_instruments.ShapeChange.Model.PropertyInfoImpl#isReadOnly()
	 */
	@Override
	public boolean isReadOnly() {

		if (isReadOnlyCache == null) {

			if (isAttribute()) {
				// Fetch from EA model
				isReadOnlyCache = Boolean.valueOf(eaAttribute.GetIsConst());
			} else {
				isReadOnlyCache = false;
			}
		}
		return isReadOnlyCache;
	}

	/**
	 * From the tagged value "inlineOrByReference" find out whether the property
	 * shall be translated into a construct which embeds the value into the
	 * property ("inline") or refers it ("byreference").
	 */
	public String inlineOrByReference() {
		String s = taggedValue("inlineOrByReference");
		if (s == null)
			s = "";

		// If still not set, find out from model
		if (s.length() == 0)
			s = super.inlineOrByReferenceFromEncodingRule();

		// If still not set, find out from EA settings
		if (s.length() == 0) {
			String cont = null;
			if (isAttribute())
				cont = eaAttribute.GetContainment();
			else
				cont = eaConnectorEnd.GetContainment();
			if (cont != null) {
				if (cont.equals("By Reference"))
					s = "byreference";
				else if (cont.equals("By Value"))
					s = "inline";
			}
		}
		return s.toLowerCase();
	} // inlineOrByReference()

	// This auxiliary method makes sure, that a cached value for the aggregation
	// type of the property is available. Note that attributes are always of
	// type "composite". The other values are "shared" and "none".
	private void validateAggregationType() {
		if (aggregationTypeCache == null) {
			if (isAttribute()) {
				// Attributes are always deemed compositions
				aggregationTypeCache = "composite";
			} else {
				// For a role we have a look into the model. And, curiously,
				// we need to look at the other end of the association.
				ConnectorEnd otherEnd = ((PropertyInfoEA) reverseProperty()).eaConnectorEnd;
				int agt = otherEnd.GetAggregation();
				if (agt == 1)
					aggregationTypeCache = "shared";
				else if (agt == 2)
					aggregationTypeCache = "composite";
			}
			// For all other categories aggregation type will be ignored.
			if (aggregationTypeCache == null)
				aggregationTypeCache = "none";
		}
	} // validateAggregationType()

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.PropertyInfo#isAggregation()
	 */
	public boolean isAggregation() {
		validateAggregationType();
		if (aggregationTypeCache.equals("shared"))
			return true;
		return false;
	} // isAggregation()

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.PropertyInfo#isAttribute()
	 */
	public boolean isAttribute() {
		return eaAttribute != null;
	} // isAttribute()

	public int getEAAttributeId() {
		return this.eaAttributeId;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.PropertyInfo#isComposition()
	 */
	public boolean isComposition() {
		validateAggregationType();
		if (aggregationTypeCache.equals("composite"))
			return true;
		return false;
	} // isComposition()

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.PropertyInfo#isDerived()
	 */
	public boolean isDerived() {
		if (isDerivedCache == null) {
			isDerivedCache = Boolean
					.valueOf(isAttribute() ? eaAttribute.GetIsDerived()
							: eaConnectorEnd.GetDerived());
		}
		return isDerivedCache;
	} // isDerived()

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.PropertyInfo#isNavigable()
	 */
	public boolean isNavigable() {

		if (isNavigableCache == null) {
			isNavigableCache = Boolean.valueOf(true);
			// Attributes always are.
			if (!isAttribute()) {
				// First get navigability from role
				boolean nav = eaConnectorEnd.GetIsNavigable();
				// If not explicitly set, also accept unspecified navigability,
				// if present in both directions.
				if (!nav)
					nav = associationInfo.navigability == 0;

				// AssociationEnds with not-known stereotypes are skipped
				if (nav) {
					String sts = eaConnectorEnd.GetStereotypeEx();
					if (sts != null) {

						String[] stereotypes = sts.split("\\,");

						for (String stereotype : stereotypes) {

							String st = options()
									.normalizeStereotype(stereotype.trim());
							boolean found = false;

							if (st.length() == 0) {
								found = true;
							} else {
								for (String s : Options.propertyStereotypes) {
									if (st.toLowerCase().equals(s)) {
										found = true;
										break;
									}
								}
							}

							if (!found) {
								MessageContext mc = document.result.addWarning(
										null, 1005, stereotype,
										"AssociationEnd");
								if (mc != null)
									mc.addDetail(null, 400, "Property",
											fullName());
								nav = false;
							}
						}
					}
				}

				// AssociationEnds with Tagged Value "xsdEncodingRule" ==
				// "notEncoded" are skipped

				// 2017-02-21 JE: if xsdEncodingRule=notEncoded the role may
				// still be relevant for other encodings; thus, this needs
				// to be handled in the XmlSchema target.
				// if (nav) {
				// for (RoleTag rt : eaConnectorEnd.GetTaggedValues()) {
				// if (rt.GetTag().equals("xsdEncodingRule"))
				// if (rt.GetValue().toLowerCase()
				// .equals(Options.NOT_ENCODED))
				// nav = false;
				// }
				// }

				// navigable only with a name, but not with a default name
				if (eaName == null || eaName
						.substring(0, eaName.length() < 5 ? eaName.length() : 5)
						.compareTo("role_") == 0)
					nav = false;

				isNavigableCache = nav;
			}
		}
		return isNavigableCache;
	} // isNavigable()

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.PropertyInfo#isOrdered()
	 */
	public boolean isOrdered() {
		if (isOrderedCache == null) {
			isOrderedCache = Boolean.FALSE;
			if (isAttribute()) {
				// Inquire from Attribute
				isOrderedCache = eaAttribute.GetIsOrdered();
			} else {
				// Inquire from ConnectorEnd
				int ordering = eaConnectorEnd.GetOrdering();
				if (ordering != 0)
					isOrderedCache = true;
			}
		}
		return isOrderedCache;
	} // isOrdered()

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.PropertyInfo#isUnique()
	 */
	public boolean isUnique() {
		if (isUniqueCache == null) {
			isUniqueCache = Boolean.TRUE;
			if (isAttribute()) {
				// Inquire from Attribute
				isUniqueCache = !eaAttribute.GetAllowDuplicates();
			} else {
				// Inquire from ConnectorEnd
				boolean uniqueness = !eaConnectorEnd.GetAllowDuplicates();
				if (!uniqueness)
					isUniqueCache = false;
			}
		}
		return isUniqueCache;
	} // isUnique()

	@Override
	public boolean isOwned() {
		if (isOwnedCache == null) {
			isOwnedCache = Boolean.FALSE;
			if (!isAttribute()) {
				// Inquire from ConnectorEnd
				isOwnedCache = eaConnectorEnd.GetOwnedByClassifier();
			}
		}
		return isOwnedCache;
	}

	// Return the sequence number of the property. */
	public StructuredNumber sequenceNumber() {
		return sequenceNumber;
	} // sequenceNumber()

	// Validate stereotypes cache of the property. The stereotypes found are 1.
	// restricted to those defined within ShapeChange and 2. deprecated ones
	// are normalized to the lastest definitions.
	public void validateStereotypesCache() {
		if (stereotypesCache == null) {
			// Fetch stereotypes 'collection' ...
			String sts;
			if (isAttribute())
				sts = eaAttribute.GetStereotypeEx();
			else
				sts = eaConnectorEnd.GetStereotypeEx();
			String[] stereotypes = sts.split("\\,");
			// Allocate cache
			stereotypesCache = StereotypeNormalizer
					.normalizeAndMapToWellKnownStereotype(stereotypes, this);
		}
	} // validateStereotypesCache()

	// Validate tagged values cache, filtering on tagged values defined within
	// ShapeChange ...
	public void validateTaggedValuesCache() {
		if (taggedValuesCache == null) {

			taggedValuesCache = options().taggedValueFactory(0);

			if (isAttribute()) {
				// Attribute case:
				// Fetch tagged values collection
				Collection<AttributeTag> tvs = eaAttribute.GetTaggedValues();

				// ensure that there are tagged values
				if (tvs != null) {

					// Allocate cache
					int ntvs = tvs.GetCount();
					taggedValuesCache = options().taggedValueFactory(ntvs);
					// Copy tag-value-pairs, leave out non-ShapeChange stuff and
					// normalize deprecated tags.
					for (AttributeTag tv : tvs) {
						String t = tv.GetName();
						t = options().taggedValueNormalizer()
								.normalizeTaggedValue(t);
						if (t != null) {
							String v = tv.GetValue();
							if (v.equals("<memo>"))
								v = tv.GetNotes();
							taggedValuesCache.add(t, v);
						}
					}
				}
			} else {
				// Role case:
				// Fetch tagged values collection
				Collection<RoleTag> tvs = eaConnectorEnd.GetTaggedValues();

				// ensure that there are tagged values
				if (tvs != null) {

					// Allocate cache
					int ntvs = tvs.GetCount();
					taggedValuesCache = options().taggedValueFactory(ntvs);

					// Copy tag-value-pairs, leave out non-ShapeChange stuff and
					// normalize deprecated tags.
					for (RoleTag tv : tvs) {

						String t = tv.GetTag();
						t = options().taggedValueNormalizer()
								.normalizeTaggedValue(t);

						if (t != null) {

							String v = tv.GetValue();

							/*
							 * An EA memo-field is used to provide convenient
							 * support (via a dialog in EA) for entering a
							 * tagged value with very long text. Such fields
							 * always start with the string '<memo>' (six
							 * characters long).
							 * 
							 * If a tagged value with a memo-field has an actual
							 * textual value then the value starts with
							 * '<memo>$ea_notes=' (16 characters long). So if a
							 * tag with memo-field does not have an actual
							 * value, we will only find '<memo>', but not
							 * followed by '$ea_notes='.
							 * 
							 * If the tagged value does not use a memo-field,
							 * then it may still contain or start with
							 * '$ea_notes='. In that case, the part after
							 * '$ea_notes=' provides the documentation of the
							 * tag (e.g. from the MDG Technology - UnitTests
							 * showed that the documentation can be empty) and
							 * the part before provides the actual value.
							 * 
							 * Otherwise (does not start with '<memo>' and does
							 * not contain '$ea_notes=') we can use the value as
							 * is.
							 */

							if (v.startsWith("<memo>$ea_notes=")) {

								v = v.substring(16);

							} else if (v.startsWith("<memo>")) {

								// no actual value in the memo-field
								v = "";

							} else if (v.contains("$ea_notes=")) {

								// retrieve the value
								v = v.substring(0, v.indexOf("$ea_notes="));

							} else {
								// fine - use the value as is
							}

							taggedValuesCache.add(t, v);
						}
					}
				}
			}
		}
	} // validateTaggedValuesCache()

	/** Set the tagged value for the tag given. */
	public void taggedValue(String tag, String value) {
		if (isAttribute()) {
			boolean upd = false;
			for (AttributeTag tv : eaAttribute.GetTaggedValues()) {
				if (tv.GetName().equals(tag)) {
					if (value == null)
						value = "";
					if (tv.GetValue().equals(value))
						return; // value already set, nothing to change
					tv.SetValue(value);
					tv.Update();
					upd = true;
					break;
				}
			}
			if (!upd && value != null) {
				AttributeTag tv = eaAttribute.GetTaggedValues().AddNew(tag,
						value);
				tv.Update();
			} else
				return; // nothing to change
		} else {
			boolean upd = false;
			for (RoleTag tv : eaConnectorEnd.GetTaggedValues()) {
				if (tv.GetTag().equals(tag)) {
					if (value == null)
						value = "";
					if (tv.GetValue().equals(value))
						return; // value already set, nothing to change
					tv.SetValue(value);
					tv.Update();
					upd = true;
					break;
				}
			}
			if (!upd && value != null) {
				RoleTag tv = eaConnectorEnd.GetTaggedValues().AddNew(tag,
						value);
				tv.Update();
			} else
				return; // nothing to change
		}
		// invalidate cache
		taggedValuesCache = null;
	} // taggedValue()

	/** Return id and name of type of property */
	public Type typeInfo() {
		return typeInfo;
	} // typeInfo

	// /**
	// * Return the documentation attached to the property object. This is
	// fetched
	// * from tagged values and - if this is absent - from the 'notes' specific
	// to
	// * the EA objects model.
	// */
	// @Override
	// public Descriptors documentationAll() {
	//
	// // Retrieve/compute the documentation only once
	// // Cache the result for subsequent use
	// if (!documentationAccessed) {
	//
	// documentationAccessed = true;
	//
	// // Fetch from tagged values
	// Descriptors ls = super.documentationAll();
	//
	// // Try EA notes, if both tagged values fail
	// if (ls.isEmpty()) {

	// String s = null;
	//
	// if (descriptorSource(Descriptor.DOCUMENTATION)
	// .equals("ea:notes")) {
	//
	// if (isAttribute())
	// s = eaAttribute.GetNotes();
	// else
	// s = eaConnectorEnd.GetRoleNote();
	// // Fix for EA7.5 bug
	// if (s != null) {
	// s = EADocument.removeSpuriousEA75EntitiesFromStrings(s);
	// }
	// }
	//
	// /*
	// * If result is empty, check if we can get the documentation
	// * from a dependency
	// */
	// if (s == null || s.isEmpty()) {
	//
	// /*
	// * NOTE: the string comparison approach chosen here (with
	// * comparison of strings that were converted to lower case)
	// * may not work in every case. See
	// * http://stackoverflow.com/a/6996550 for further details.
	// */
	// String thisNameLowerCase = this.name().trim()
	// .toLowerCase(Locale.ENGLISH);
	// String thisNameLowerCaseForValueConcept = "_"
	// + thisNameLowerCase;
	//
	// for (String cid : classInfo.supplierIds()) {
	//
	// ClassInfoEA cix = document.fClassById.get(cid);
	//
	// if (cix != null) {
	//
	// String cixNameLowerCase = cix.name().trim()
	// .toLowerCase(Locale.ENGLISH);
	//
	// if (classInfo.category() == Options.ENUMERATION
	// && cix.stereotype("valueconcept")
	// && (cixNameLowerCase
	// .equals(thisNameLowerCase)
	// || cixNameLowerCase.endsWith(
	// thisNameLowerCaseForValueConcept))) {
	// s = cix.documentation();
	// break;
	//
	// } else if (classInfo
	// .category() != Options.ENUMERATION
	// && (cix.stereotype("attributeconcept")
	// || cix.stereotype("roleconcept"))
	// && cixNameLowerCase
	// .equals(thisNameLowerCase)) {
	// s = cix.documentation();
	// break;
	// }
	//
	// // if (cixNameLowerCase.equals(thisNameLowerCase)) {
	// // if (classInfo.category() != Options.ENUMERATION
	// // && cix.stereotype("attributeconcept")) {
	// // s = cix.documentation();
	// // break;
	// // }
	// // } else if (cixNameLowerCase.endsWith(
	// // thisNameLowerCaseForValueConcept)) {
	// // if (classInfo.category() == Options.ENUMERATION
	// // && cix.stereotype("valueconcept")) {
	// // s = cix.documentation();
	// // break;
	// // }
	// // }
	// }
	// }
	// }
	//
	// if (s == null) {
	// super.documentation = new Descriptors();
	// } else {
	// super.documentation = new Descriptors(
	// new LangString(options().internalize(s)));
	// }
	// }
	// }
	//
	// return super.documentation;
	//
	// }

	/** Return model-unique id of property. */
	public String id() {
		return eaPropertyId;
	} // id()

	/** Obtain the name of the property. */
	public String name() {
		// Get the name obtained from the model
		if (eaName == null || eaName.equals("")) {
			if (classInfo == null || (classInfo.category() != Options.CODELIST
					&& classInfo.category() != Options.ENUMERATION)) {
				eaName = id();
				MessageContext mc = document.result.addWarning(null, 100,
						"property", eaName);
				if (mc != null)
					mc.addDetail(null, 400, "Class", classInfo.fullName());
			} else {
				MessageContext mc = document.result.addWarning(null, 136, id(),
						classInfo.name());
				if (mc != null)
					mc.addDetail(null, 400, "Class", classInfo.fullName());
			}
		}
		return eaName;
	} // name()

	// @Override
	// public Descriptors aliasNameAll() {
	//
	// // Retrieve/compute the alias only once
	// // Cache the result for subsequent use
	// if (!aliasAccessed) {
	//
	// aliasAccessed = true;
	//
	// // Obtain alias name from default implementation
	// Descriptors ls = super.aliasNameAll();
	//
	// // If not present, obtain from EA model directly
	// if (ls.isEmpty()
	// && descriptorSource(Descriptor.ALIAS).equals("ea:alias")) {
	//
	// String alias;
	// if (isAttribute())
	// alias = eaAttribute.GetStyle();
	// else
	// alias = eaConnectorEnd.GetAlias();
	//
	// if (alias != null && !alias.isEmpty()) {
	//
	// super.aliasName = new Descriptors(
	// new LangString(options().internalize(alias)));
	// } else {
	// super.aliasName = new Descriptors();
	// }
	// }
	// }
	//
	// return super.aliasName;
	// }

	// Validate constraints cache. This makes sure the constraints cache
	// contains all constraints ordered by their appearance in the property.
	// Note that only constraints on attributes are considered currently.
	// If constraints are disabled the cache is empty.
	private void validateConstraintsCache() {
		if (constraintsCache == null) {
			// Allocate cache
			constraintsCache = new Vector<Constraint>();

			// Constraints disabled?
			String check = document.options.parameter("checkingConstraints");
			if (check != null && check.equalsIgnoreCase("disabled"))
				return;

			// Constraints for properties irrelevant?
			if (!document.options.isConstraintCreationForProperties())
				return;

			// Constraints from selected schemas only?
			if (document.options.isLoadConstraintsForSelectedSchemasOnly()
					&& !document.isInSelectedSchemas(this.inClass())) {
				return;
			}

			if (isAttribute()) {
				// Access EA constraints data
				Collection<AttributeConstraint> constrs = eaAttribute
						.GetConstraints();
				// Ensure that there are constraints before continuing
				if (constrs != null) {
					// Determine constraint types to be parsed as OCL
					String types = document.options
							.parameter("oclConstraintTypeRegex");
					// Enumerate all constraints found
					for (AttributeConstraint constr : constrs) {
						// Wrap into constraint object
						String type = constr.GetType();
						Constraint oc;

						if (types.length() > 0 && type.matches(types)) {
							OclConstraintEA ocl = new OclConstraintEA(document,
									this, constr);
							if (ocl.syntaxTree() == null)
								// Text constraint is a fallback in case of
								// parsing
								// issues
								oc = new TextConstraintEA(document, this,
										constr);
							else
								oc = ocl;
						} else if (type.equalsIgnoreCase("SBVR")) {
							oc = new FolConstraintEA(document, this, constr);
						} else {
							oc = new TextConstraintEA(document, this, constr);
						}
						// Collect in cache
						constraintsCache.add(oc);
					}
				}
			}
		}
	}

	/** This method returns the constraints associated with the class. */
	public Vector<Constraint> constraints() {
		validateConstraintsCache();
		return constraintsCache;
	}

	public AssociationInfo association() {
		return associationInfo;
	}

	@Override
	protected List<LangString> descriptorValues(Descriptor descriptor) {

		// get default first
		List<LangString> ls = super.descriptorValues(descriptor);

		if (ls.isEmpty()) {

			if (!documentationAccessed
					&& descriptor == Descriptor.DOCUMENTATION) {

				documentationAccessed = true;

				String s = null;

				if (model().descriptorSource(Descriptor.DOCUMENTATION)
						.equals("ea:notes")) {

					if (isAttribute())
						s = eaAttribute.GetNotes();
					else
						s = eaConnectorEnd.GetRoleNote();
					// Fix for EA7.5 bug
					if (s != null) {
						s = EADocument.removeSpuriousEA75EntitiesFromStrings(s);
					}
				}

				/*
				 * If result is empty, check if we can get the documentation
				 * from a dependency
				 */
				if (s == null || s.isEmpty()) {

					/*
					 * NOTE: the string comparison approach chosen here (with
					 * comparison of strings that were converted to lower case)
					 * may not work in every case. See
					 * http://stackoverflow.com/a/6996550 for further details.
					 */
					String thisNameLowerCase = this.name().trim()
							.toLowerCase(Locale.ENGLISH);
					String thisNameLowerCaseForValueConcept = "_"
							+ thisNameLowerCase;

					for (String cid : classInfo.supplierIds()) {

						ClassInfoEA cix = document.fClassById.get(cid);

						if (cix != null) {

							String cixNameLowerCase = cix.name().trim()
									.toLowerCase(Locale.ENGLISH);

							if (classInfo.category() == Options.ENUMERATION
									&& cix.stereotype("valueconcept")
									&& (cixNameLowerCase
											.equals(thisNameLowerCase)
											|| cixNameLowerCase.endsWith(
													thisNameLowerCaseForValueConcept))) {
								s = cix.documentation();
								break;

							} else if (classInfo
									.category() != Options.ENUMERATION
									&& (cix.stereotype("attributeconcept")
											|| cix.stereotype("roleconcept"))
									&& cixNameLowerCase
											.equals(thisNameLowerCase)) {
								s = cix.documentation();
								break;
							}

							// if (cixNameLowerCase.equals(thisNameLowerCase)) {
							// if (classInfo.category() != Options.ENUMERATION
							// && cix.stereotype("attributeconcept")) {
							// s = cix.documentation();
							// break;
							// }
							// } else if (cixNameLowerCase.endsWith(
							// thisNameLowerCaseForValueConcept)) {
							// if (classInfo.category() == Options.ENUMERATION
							// && cix.stereotype("valueconcept")) {
							// s = cix.documentation();
							// break;
							// }
							// }
						}
					}
				}
				// String s = null;
				//
				// // Try EA notes if ea:notes is the source
				// if (descriptorSource(Descriptor.DOCUMENTATION)
				// .equals("ea:notes")) {
				// s = eaClassElement.GetNotes();
				// // Fix for EA7.5 bug
				// if (s != null) {
				// s = EADocument.removeSpuriousEA75EntitiesFromStrings(s);
				// }
				// }
				//
				// /*
				// * If result is empty, check if we can get the documentation
				// * from a dependency
				// */
				// if (s == null || s.isEmpty()) {
				//
				// for (String cid : this.supplierIds()) {
				//
				// ClassInfoEA cix = document.fClassById.get(cid);
				//
				// if (cix != null) {
				// if (cix.name().equalsIgnoreCase(this.name())
				// && cix.stereotype("featureconcept")) {
				// s = cix.documentation();
				// break;
				// }
				// }
				// }
				// }
				//
				// // If result is empty, check if we can get the documentation
				// // from a
				// // supertype with the same name (added for ELF/INSPIRE)
				// if (s == null || s.isEmpty()) {
				//
				// HashSet<ClassInfoEA> sts = supertypesAsClassInfoEA();
				//
				// if (sts != null) {
				// for (ClassInfoEA stci : sts) {
				// if (stci.name().equals(this.name())) {
				// s = stci.documentation();
				// break;
				// }
				// }
				// }
				// }
				//
				if (s != null) {
					ls.add(new LangString(options().internalize(s)));
					this.descriptors().put(descriptor, ls);
				}

			} else if (!globalIdentifierAccessed
					&& descriptor == Descriptor.GLOBALIDENTIFIER) {

				globalIdentifierAccessed = true;

				// obtain from EA model directly
				if (model().descriptorSource(Descriptor.GLOBALIDENTIFIER)
						.equals("ea:guidtoxml")) {

					String gi;

					if (this.isAttribute()) {

						gi = document.repository.GetProjectInterface()
								.GUIDtoXML(eaAttribute.GetAttributeGUID());

					} else {

						String connectorGUID = associationInfo.eaConnector
								.GetConnectorGUID();
						String xmlGuid = document.repository
								.GetProjectInterface().GUIDtoXML(connectorGUID);
						String assocRoleGUID = "EAID_"
								+ (reversedAssoc ? "src" : "dst")
								+ xmlGuid.substring(7);
						gi = assocRoleGUID;
					}

					// String gi = document.repository.GetProjectInterface()
					// .GUIDtoXML(eaClassElement.GetElementGUID());

					if (gi != null && !gi.isEmpty()) {
						ls.add(new LangString(options().internalize(gi)));
						this.descriptors().put(descriptor, ls);
					}
				}

			} else if (!aliasAccessed && descriptor == Descriptor.ALIAS) {

				aliasAccessed = true;

				/*
				 * obtain from EA model directly if ea:alias is identified as
				 * the source
				 */
				if (model().descriptorSource(Descriptor.ALIAS)
						.equals("ea:alias")) {

					String a;
					if (isAttribute())
						a = eaAttribute.GetStyle();
					else
						a = eaConnectorEnd.GetAlias();

					if (a != null && !a.isEmpty()) {
						ls.add(new LangString(options().internalize(a)));
						this.descriptors().put(descriptor, ls);
					}
				}
			}

		}

		return ls;
	}

	// @Override
	// public Descriptors globalIdentifierAll() {
	//
	// // Obtain global identifier from default implementation
	// Descriptors ls = super.globalIdentifierAll();
	//
	// // If not present, obtain from EA model directly
	// if (ls.isEmpty() && descriptorSource(Descriptor.GLOBALIDENTIFIER)
	// .equals("ea:guidtoxml")) {
	//
	// String gi;
	//
	// if (this.isAttribute()) {
	//
	// gi = document.repository.GetProjectInterface()
	// .GUIDtoXML(eaAttribute.GetAttributeGUID());
	//
	// } else {
	//
	// String connectorGUID = associationInfo.eaConnector
	// .GetConnectorGUID();
	// String xmlGuid = document.repository.GetProjectInterface()
	// .GUIDtoXML(connectorGUID);
	// String assocRoleGUID = "EAID_" + (reversedAssoc ? "src" : "dst")
	// + xmlGuid.substring(7);
	// gi = assocRoleGUID;
	// }
	//
	// super.globalIdentifier = new Descriptors(
	// new LangString(options().internalize(gi)));
	// }
	// return super.globalIdentifier;
	// }
}
