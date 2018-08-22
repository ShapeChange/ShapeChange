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
package de.interactive_instruments.ShapeChange.Util.ea;

import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.sparx.Attribute;
import org.sparx.Collection;
import org.sparx.Connector;
import org.sparx.ConnectorEnd;
import org.sparx.Element;
import org.sparx.Method;
import org.sparx.Repository;
import org.sparx.TaggedValue;

import de.interactive_instruments.ShapeChange.Multiplicity;
import de.interactive_instruments.ShapeChange.Model.TaggedValues;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class EAElementUtil extends AbstractEAUtil {

	/**
	 * Adds the given tagged value to the tagged values of the given element,
	 * NOT checking for duplicate tags.
	 * <p>
	 * <b>WARNING:</b> Enterprise Architect may initialize default tagged values
	 * for a model element that adheres to a specific UML profile. In that case,
	 * adding the same tagged values would lead to duplicates. If duplicates
	 * shall be prevented, set the tagged value instead of adding it.
	 * 
	 * @param e
	 *            the element to which the tagged value shall be added
	 * @param tv
	 *            tagged value to add
	 */
	public static void addTaggedValue(Element e, EATaggedValue tv)
			throws EAException {

		Collection<TaggedValue> cTV = e.GetTaggedValues();

		String name = tv.getName();
		String type = "";

		List<String> values = tv.getValues();

		if (values != null) {

			for (String v : values) {

				TaggedValue eaTv = cTV.AddNew(name, type);
				cTV.Refresh();

				if (tv.createAsMemoField() || v.length() > 255) {
					eaTv.SetValue("<memo>");
					eaTv.SetNotes(v);
				} else {
					eaTv.SetValue(v);
					eaTv.SetNotes("");
				}

				if (!eaTv.Update()) {
					throw new EAException(createMessage(message(106), name,
							e.GetName(), v, eaTv.GetLastError()));
				}
			}
		}
	}

	/**
	 * Adds the given list of tagged values to the collection of (EA) tagged
	 * values of the given element, NOT checking for duplicate tags.
	 * <p>
	 * <b>WARNING:</b> Enterprise Architect may initialize default tagged values
	 * for a model element that adheres to a specific UML profile. In that case,
	 * adding the same tagged values would lead to duplicates. If duplicates
	 * shall be prevented, set the tagged value instead of adding it.
	 * 
	 * @param e
	 * @param tvs
	 *            collection of tagged values to set
	 */
	public static void addTaggedValues(Element e, List<EATaggedValue> tvs)
			throws EAException {

		if (tvs == null || tvs.isEmpty()) {

			// nothing to do

		} else {

			Collection<TaggedValue> cTV = e.GetTaggedValues();

			for (EATaggedValue tv : tvs) {

				String name = tv.getName();
				List<String> values = tv.getValues();

				if (values != null) {

					for (String v : values) {

						TaggedValue eaTv = cTV.AddNew(name, "");
						cTV.Refresh();

						if (tv.createAsMemoField() || v.length() > 255) {
							eaTv.SetValue("<memo>");
							eaTv.SetNotes(v);
						} else {
							eaTv.SetValue(v);
							eaTv.SetNotes("");
						}

						if (!eaTv.Update()) {
							throw new EAException(createMessage(message(106),
									name, e.GetName(), v, eaTv.GetLastError()));
						}
					}
				}
			}
		}
	}

	/**
	 * Adds the given collection of tagged values to the tagged values of the
	 * given element, NOT checking for duplicate tags.
	 * <p>
	 * <b>WARNING:</b> Enterprise Architect may initialize default tagged values
	 * for a model element that adheres to a specific UML profile. In that case,
	 * adding the same tagged values would lead to duplicates. If duplicates
	 * shall be prevented, set the tagged value instead of adding it.
	 * 
	 * @param e
	 *            the element to which the tagged values shall be added
	 * @param tvs
	 *            collection of tagged values to add
	 */
	public static void addTaggedValues(Element e, TaggedValues tvs)
			throws EAException {

		if (tvs == null || tvs.isEmpty()) {
			// nothing to do
		} else {

			Collection<TaggedValue> cTV = e.GetTaggedValues();

			for (String tag : tvs.keySet()) {

				String[] values = tvs.get(tag);

				for (String v : values) {

					TaggedValue tv = cTV.AddNew(tag, "");
					cTV.Refresh();

					if (v.length() > 255) {
						tv.SetValue("<memo>");
						tv.SetNotes(v);
					} else {
						tv.SetValue(v);
						tv.SetNotes("");
					}

					if (!tv.Update()) {
						throw new EAException(createMessage(message(106), tag,
								e.GetName(), v, tv.GetLastError()));
					}
				}
			}
		}
	}

	/**
	 * Deletes all tagged values whose name equals (ignoring case) the given
	 * name in the given element.
	 * 
	 * @param e
	 * @param nameOfTVToDelete
	 */
	public static void deleteTaggedValue(Element e, String nameOfTVToDelete) {

		Collection<TaggedValue> cTV = e.GetTaggedValues();
		cTV.Refresh();

		for (short i = 0; i < cTV.GetCount(); i++) {
			TaggedValue tv = cTV.GetAt(i);
			if (tv.GetName().equalsIgnoreCase(nameOfTVToDelete)) {
				cTV.Delete(i);
			}
		}

		cTV.Refresh();
	}

	public static void setEAAbstract(Element e, boolean isAbstract)
			throws EAException {

		e.SetAbstract("true");

		if (!e.Update()) {
			throw new EAException(
					createMessage(message(101), e.GetName(), e.GetLastError()));
		}
	}

	public static void setEAAlias(Element e, String aliasName)
			throws EAException {

		e.SetAlias(aliasName);

		if (!e.Update()) {
			throw new EAException(
					createMessage(message(102), e.GetName(), e.GetLastError()));
		}
	}

	public static void loadLinkedDocument(Element e,
			String linkedDocumentAbsolutePath) throws EAException {

		e.LoadLinkedDocument(linkedDocumentAbsolutePath);

		if (!e.Update()) {
			throw new EAException(createMessage(message(109), e.GetName(),
					linkedDocumentAbsolutePath, e.GetLastError()));
		}
	}

	/**
	 * Sets attribute GenType on the given EA element.
	 * 
	 * @param e
	 * @param gentype
	 * @throws EAException
	 *             If updating the element did not succeed, this exception
	 *             contains the error message.
	 */
	public static void setEAGenType(Element e, String gentype)
			throws EAException {

		e.SetGentype(gentype);

		if (!e.Update()) {
			throw new EAException(
					createMessage(message(103), e.GetName(), e.GetLastError()));
		}
	}

	public static void setEANotes(Element e, String documentation)
			throws EAException {

		e.SetNotes(documentation);

		if (!e.Update()) {
			throw new EAException(
					createMessage(message(104), e.GetName(), e.GetLastError()));
		}
	}

	/**
	 * Sets attribute Stereotype on the given EA element.
	 * 
	 * @param e
	 * @param stereotype
	 * @throws EAException
	 *             If updating the element did not succeed, this exception
	 *             contains the error message.
	 */
	public static void setEAStereotype(Element e, String stereotype)
			throws EAException {

		e.SetStereotype(stereotype);

		if (!e.Update()) {
			throw new EAException(
					createMessage(message(110), e.GetName(), e.GetLastError()));
		}
	}

	/**
	 * Sets attribute StereotypeEx on the given EA element.
	 * 
	 * @param e
	 * @param stereotypeEx
	 * @throws EAException
	 *             If updating the element did not succeed, this exception
	 *             contains the error message.
	 */
	public static void setEAStereotypeEx(Element e, String stereotypeEx)
			throws EAException {

		e.SetStereotypeEx(stereotypeEx);

		if (!e.Update()) {
			throw new EAException(
					createMessage(message(105), e.GetName(), e.GetLastError()));
		}
	}

	/**
	 * Sets the given tagged value in the tagged values of the given element. If
	 * tagged values with the same tag name already exist, they will be deleted.
	 * Then the tagged value will be added.
	 * 
	 * @param e
	 *            the element in which the tagged value shall be set
	 * @param tv
	 *            tagged value to set, must not be <code>null</code>
	 */
	public static void setTaggedValue(Element e, EATaggedValue tv)
			throws EAException {

		deleteTaggedValue(e, tv.getName());
		addTaggedValue(e, tv);
	}

	/**
	 * Sets the given tagged value in the tagged values of the given element. If
	 * tagged values with the same tag name already exist, they will be deleted.
	 * Then the tagged value will be added.
	 * 
	 * @param e
	 *            the element in which the tagged value shall be set
	 * @param name
	 *            name of the tagged value to set, must not be <code>null</code>
	 * @param value
	 *            value of the tagged value to set, can be <code>null</code>
	 */
	public static void setTaggedValue(Element e, String name, String value)
			throws EAException {

		EATaggedValue tv = new EATaggedValue(name, value);

		deleteTaggedValue(e, tv.getName());
		addTaggedValue(e, tv);
	}

	/**
	 * Sets the given tagged values in the given element. If tagged values with
	 * the same name as the given ones already exist, they will be deleted. Then
	 * the tagged values will be added.
	 * 
	 * @param e
	 *            the element in which the tagged values shall be set
	 * @param tvs
	 *            tagged values to set, must not be <code>null</code>
	 */
	public static void setTaggedValues(Element e, List<EATaggedValue> tvs)
			throws EAException {

		for (EATaggedValue tv : tvs) {
			deleteTaggedValue(e, tv.getName());
		}
		addTaggedValues(e, tvs);
	}

	/**
	 * Sets the given tagged values in the given element. If tagged values with
	 * the same name as the given ones already exist, they will be deleted. Then
	 * the tagged values will be added.
	 * 
	 * @param e
	 *            the element in which the tagged values shall be set
	 * @param tvs
	 *            tagged values to set, must not be <code>null</code>
	 */
	public static void setTaggedValues(Element e, TaggedValues tvs)
			throws EAException {

		for (String tvName : tvs.asMap().keySet()) {
			deleteTaggedValue(e, tvName);
		}
		addTaggedValues(e, tvs);
	}

	/**
	 * Retrieves the first tagged value with given name of the given element.
	 * Does not apply normalization of tags, i.e. comparison is performed using
	 * string equality. With UML 2, there may be multiple values per tag. This
	 * method does NOT issue a warning if more than one value exists for the
	 * tag. I.e., use this method only for cases, where only one value per tag
	 * may be provided.
	 * 
	 * @param elmt
	 *            element that contains the tagged values to search
	 * @param tvName
	 *            name of the tagged value to retrieve
	 * @return The tagged value for the tag with given name or <code>null</code>
	 *         if the tagged value was not found. If there are multiple values
	 *         with the tag only the first is provided.
	 */
	public static String taggedValue(Element elmt, String tvName) {

		org.sparx.Collection<org.sparx.TaggedValue> tvs = elmt
				.GetTaggedValues();

		for (org.sparx.TaggedValue tv : tvs) {

			if (tvName.equals(tv.GetName())) {

				String value = tv.GetValue();

				if (value.equals("<memo>")) {
					value = tv.GetNotes();
				}

				return value;
			}
		}

		return null;
	}

	/**
	 * Updates the tagged values with given name (which can be a fully qualified
	 * name) in the tagged values of the given element. Does NOT delete those
	 * tagged values. NOTE: This method is especially useful when setting tagged
	 * values that are defined by an MDG / UML Profile, since these tagged
	 * values cannot be created programmatically (they are created by EA - for
	 * further details, see
	 * http://sparxsystems.com/forums/smf/index.php?topic=3859.0).
	 * 
	 * @param e
	 *            the element in which the tagged values shall be updated
	 * @param name
	 *            (fully qualified or unqualified) name of the tagged value to
	 *            update, must not be <code>null</code>
	 * @param value
	 *            value of the tagged value to update, can be <code>null</code>
	 * @param createAsMemoField
	 *            If set to <code>true</code>, the values shall be encoded using
	 *            &lt;memo&gt; fields, regardless of the actual length of each
	 *            value.
	 * @throws EAException
	 *             If updating the element did not succeed, this exception
	 *             contains the error message.
	 */
	public static void updateTaggedValue(Element e, String name, String value,
			boolean createAsMemoField) throws EAException {

		boolean isQualifiedName = name.contains("::");

		Collection<TaggedValue> cTV = e.GetTaggedValues();

		cTV.Refresh();

		for (short i = 0; i < cTV.GetCount(); i++) {

			TaggedValue tv = cTV.GetAt(i);

			if ((isQualifiedName && tv.GetFQName().equalsIgnoreCase(name))
					|| tv.GetName().equalsIgnoreCase(name)) {

				if (createAsMemoField || value.length() > 255) {
					tv.SetValue("<memo>");
					tv.SetNotes(value);
				} else {
					tv.SetValue(value);
					tv.SetNotes("");
				}

				if (!tv.Update()) {
					throw new EAException(createMessage(message(106), name,
							e.GetName(), value, tv.GetLastError()));
				}
			}
		}

		cTV.Refresh();
	}

	public static Method createEAMethod(Element elmt, String name)
			throws EAException {

		Collection<Method> methods = elmt.GetMethods();

		Method m = methods.AddNew(name, "");

		if (!m.Update()) {
			throw new EAException(createMessage(message(107), name,
					elmt.GetName(), m.GetLastError()));
		}

		methods.Refresh();

		return m;
	}

	/**
	 * Create a new attribute for the given element.
	 * 
	 * @param e
	 * @param name
	 *            EA attribute name, must not be <code>null</code>.
	 * @param alias
	 *            EA alias, can be <code>null</code>.
	 * @param documentation
	 *            EA notes, can be <code>null</code>.
	 * @param stereotypes
	 *            EA stereotypeEx, can be <code>null</code>.
	 * @param taggedValues
	 *            Tagged values of the attribute, can be <code>null</code>.
	 * @param isDerived
	 *            EA isDerived.
	 * @param isOrdered
	 *            EA isOrdered.
	 * @param allowDuplicates
	 *            EA allowDuplicates (the opposite of ShapeChange isUnique)
	 * @param initialValue
	 *            EA default/initialValue, can be <code>null</code>.
	 * @param m
	 *            Multiplicity of the attribute, can be <code>null</code>.
	 * @param type
	 *            EA type, can be <code>null</code>.
	 * @param classifierID
	 *            EA classifierID, can be <code>null</code>.
	 * @return the new attribute
	 * @throws EAException
	 *             If updating the attribute did not succeed, this exception
	 *             contains the error message.
	 */
	public static Attribute createEAAttribute(Element e, String name,
			String alias, String documentation, Set<String> stereotypes,
			List<EATaggedValue> taggedValues, boolean isDerived,
			boolean isOrdered, boolean allowDuplicates, String initialValue,
			Multiplicity m, String type, Integer classifierID)
			throws EAException {

		Collection<Attribute> atts = e.GetAttributes();

		Attribute att = atts.AddNew(name, "");
		if (!att.Update()) {
			throw new EAException(createMessage(message(108), name, e.GetName(),
					e.GetLastError()));
		}
		atts.Refresh();

		if (alias != null) {
			att.SetStyle(alias);
		}

		if (documentation != null) {
			att.SetNotes(documentation);
		}

		if (stereotypes != null) {
			att.SetStereotypeEx(stereotypesCSV(stereotypes));
		}

		if (taggedValues != null) {
			EAAttributeUtil.setTaggedValues(att, taggedValues);
		}

		att.SetIsDerived(isDerived);

		att.SetIsOrdered(isOrdered);

		att.SetAllowDuplicates(allowDuplicates);

		if (initialValue != null) {
			att.SetDefault(initialValue);
		}

		att.SetVisibility("public");

		if (m != null) {
			att.SetLowerBound("" + m.minOccurs);
			if (m.maxOccurs == Integer.MAX_VALUE)
				att.SetUpperBound("*");
			else
				att.SetUpperBound("" + m.maxOccurs);
		}

		if (type != null) {
			att.SetType(type);
		}

		if (classifierID != null) {
			att.SetClassifierID(classifierID);
		}

		if (!att.Update()) {
			throw new EAException(createMessage(message(108), name, e.GetName(),
					e.GetLastError()));
		}

		return att;
	}

	public static Connector createEAAssociation(Element client,
			Element supplier) throws EAException {

		Collection<Connector> clientCons = client.GetConnectors();

		Connector con = clientCons.AddNew("", "Association");

		con.SetSupplierID(supplier.GetElementID());

		if (!con.Update()) {
			throw new EAException(createMessage(message(1005), client.GetName(),
					supplier.GetName(), con.GetLastError()));
		}

		clientCons.Refresh();

		return con;
	}

	/**
	 * @param e
	 * @param stereotype
	 * @return the (first encountered) method whose 'StereotypeEx' contains the
	 *         given stereotype, or <code>null</code> if no such method was
	 *         found
	 */
	public static Method getEAMethodWithStereotypeEx(Element e,
			String stereotype) {

		Collection<Method> methods = e.GetMethods();

		methods.Refresh();

		for (short i = 0; i < methods.GetCount(); i++) {
			Method m = methods.GetAt(i);
			if (m.GetStereotypeEx().contains(stereotype)) {
				return m;
			}
		}

		return null;
	}

	/**
	 * @param elmt
	 *            EA element in which to look for the attribute
	 * @param attName
	 *            name of the attribute to look up
	 * @return the EA attribute that belongs to the EA element and has the given
	 *         attribute name; can be <code>null</code> if no such attribute was
	 *         found
	 */
	public static Attribute getAttributeByName(Element elmt, String attName) {

		Attribute result = null;

		Collection<Attribute> atts = elmt.GetAttributes();

		for (short i = 0; i < atts.GetCount(); i++) {
			Attribute att = atts.GetAt(i);
			if (att.GetName().equals(attName)) {
				result = att;
				break;
			}
		}

		return result;
	}

	/**
	 * @param elmt
	 * @return sorted map of the tagged values (key: {name '#' fqName}; value:
	 *         according EATaggedValue); can be empty but not <code>null</code>
	 */
	public static SortedMap<String, EATaggedValue> getEATaggedValues(
			Element elmt) {

		/*
		 * key: {name '#' fqName}; value: according EATaggedValue
		 */
		SortedMap<String, EATaggedValue> result = new TreeMap<String, EATaggedValue>();

		Collection<TaggedValue> tvs = elmt.GetTaggedValues();

		for (short i = 0; i < tvs.GetCount(); i++) {

			TaggedValue tv = tvs.GetAt(i);

			String name = tv.GetName();
			String fqName = tv.GetFQName();
			String value;

			String tvValue = tv.GetValue();

			if (tvValue.equals("<memo>")) {
				value = tv.GetNotes();
			} else {
				value = tvValue;
			}

			String key = name + "#" + fqName;

			if (result.containsKey(key)) {
				EATaggedValue eatv = result.get(key);
				eatv.addValue(value);
			} else {
				result.put(key, new EATaggedValue(name, fqName, value));
			}
		}

		return result;
	}

	/**
	 * @param e
	 *            Element to which the new constraint shall be added.
	 * @param name
	 *            Name of the new constraint
	 * @param type
	 *            Type of the new constraint
	 * @param text
	 *            Text of the new constraint
	 * @param status
	 *            Status of the new constraint
	 * @return The new constraint
	 * @throws EAException
	 */
	public static org.sparx.Constraint addConstraint(Element e, String name,
			String type, String text, String status) throws EAException {

		Collection<org.sparx.Constraint> cons = e.GetConstraints();

		org.sparx.Constraint con = cons.AddNew(name, type);

		cons.Refresh();

		con.SetNotes(text);

		if (StringUtils.isNotBlank(status)) {
			con.SetStatus(status);
		}

		if (!con.Update()) {
			throw new EAException(createMessage(message(1006), name,
					e.GetName(), con.GetLastError()));
		} else {
			return con;
		}
	}

	/**
	 * @param e
	 *            Element to which the new constraint shall be added.
	 * @param name
	 *            Name of the new constraint
	 * @param type
	 *            Type of the new constraint
	 * @param text
	 *            Text of the new constraint
	 * @return The new constraint
	 * @throws EAException
	 */
	public static org.sparx.Constraint addConstraint(Element e, String name,
			String type, String text) throws EAException {

		return addConstraint(e, name, type, text, null);
	}

	public static String message(int mnr) {

		switch (mnr) {

		case 101:
			return "EA error encountered while updating 'Abstract' of EA element '$1$'. Error message is: $2$";
		case 102:
			return "EA error encountered while updating 'Alias' of EA element '$1$'. Error message is: $2$";
		case 103:
			return "EA error encountered while updating 'GenType' of EA element '$1$'. Error message is: $2$";
		case 104:
			return "EA error encountered while updating 'Notes' of EA element '$1$'. Error message is: $2$";
		case 105:
			return "EA error encountered while updating 'StereotypeEx' of EA element '$1$'. Error message is: $2$";
		case 106:
			return "EA error encountered while updating EA tagged value '$1$' of element '$2$' with value '$3$'. Error message is: $4$";
		case 107:
			return "EA error encountered while updating new EA method '$1$' on element '$2$'. Error message is: $3$";
		case 108:
			return "EA error encountered while updating new EA attribute '$1$' on element '$2$'. Error message is: $3$";
		case 109:
			return "EA error encountered while loading linked document for EA element '$1$' from path '$2$'. Error message is: $3$";
		case 110:
			return "EA error encountered while updating 'Stereotype' of EA element '$1$'. Error message is: $2$";

		case 1005:
			return "EA error encountered while updating new EA connector between elements '$1$' and '$2$'. Error message is: $3$";
		case 1006:
			return "EA error encountered while updating new EA constraint '$1$' for element '$2$'. Error message is: $3$";
		default:
			return "(" + EAElementUtil.class.getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
