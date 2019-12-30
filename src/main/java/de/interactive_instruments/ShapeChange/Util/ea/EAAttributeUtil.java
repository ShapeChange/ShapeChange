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
import java.util.SortedMap;
import java.util.TreeMap;

import org.sparx.Attribute;
import org.sparx.AttributeTag;
import org.sparx.Collection;

import de.interactive_instruments.ShapeChange.Model.TaggedValues;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
public class EAAttributeUtil extends AbstractEAUtil {

	/**
	 * Adds the given collection of tagged values to the tagged values of the
	 * given attribute, NOT checking for duplicate tags.
	 * <p>
	 * <b>WARNING:</b> Enterprise Architect may initialize default tagged values
	 * for a model element that adheres to a specific UML profile. In that case,
	 * adding the same tagged values would lead to duplicates. If duplicates
	 * shall be prevented, set the tagged value instead of adding it.
	 * 
	 * @param att
	 *            the attribute to which the tagged values shall be added
	 * @param tv
	 *            collection of tagged values to add
	 * @throws EAException  tbd
	 */
	public static void addTaggedValue(Attribute att, EATaggedValue tv)
			throws EAException {

		if (tv == null) {
			// nothing to do
		} else {

			Collection<AttributeTag> cTV = att.GetTaggedValues();

			String tag = tv.getName();

			List<String> values = tv.getValues();

			if (values != null) {

				for (String v : values) {

					AttributeTag at = cTV.AddNew(tag, "");
					cTV.Refresh();

					if (v.length() > 255) {
						at.SetValue("<memo>");
						at.SetNotes(v);
					} else {
						at.SetValue(v);
						at.SetNotes("");
					}

					if (!at.Update()) {
						throw new EAException(createMessage(message(102), tag,
								att.GetName(), v, at.GetLastError()));
					}
				}
			}
		}
	}

	/**
	 * Adds the given collection of tagged values to the tagged values of the
	 * given attribute, NOT checking for duplicate tags.
	 * <p>
	 * <b>WARNING:</b> Enterprise Architect may initialize default tagged values
	 * for a model element that adheres to a specific UML profile. In that case,
	 * adding the same tagged values would lead to duplicates. If duplicates
	 * shall be prevented, set the tagged value instead of adding it.
	 * 
	 * @param att
	 *            the attribute to which the tagged values shall be added
	 * @param tvs
	 *            collection of tagged values to add
	 * @throws EAException  tbd
	 */
	public static void addTaggedValues(Attribute att, List<EATaggedValue> tvs)
			throws EAException {

		if (tvs == null || tvs.isEmpty()) {

			// nothing to do

		} else {

			Collection<AttributeTag> cTV = att.GetTaggedValues();
			String attName = att.GetName();

			for (EATaggedValue tv : tvs) {

				String name = tv.getName();
				List<String> values = tv.getValues();

				if (values != null) {

					for (String v : values) {

						AttributeTag eaTv = cTV.AddNew(name, "");
						cTV.Refresh();

						if (tv.createAsMemoField() || v.length() > 255) {
							eaTv.SetValue("<memo>");
							eaTv.SetNotes(v);
						} else {
							eaTv.SetValue(v);
							eaTv.SetNotes("");
						}

						if (!eaTv.Update()) {
							throw new EAException(createMessage(message(102),
									name, attName, v, eaTv.GetLastError()));
						}
					}
				}
			}
		}
	}

	/**
	 * Adds the given collection of tagged values to the tagged values of the
	 * given attribute, NOT checking for duplicate tags.
	 * <p>
	 * <b>WARNING:</b> Enterprise Architect may initialize default tagged values
	 * for a model element that adheres to a specific UML profile. In that case,
	 * adding the same tagged values would lead to duplicates. If duplicates
	 * shall be prevented, set the tagged value instead of adding it.
	 * 
	 * @param att
	 *            the attribute to which the tagged values shall be added
	 * @param tvs
	 *            collection of tagged values to add
	 * @throws EAException  tbd
	 */
	public static void addTaggedValues(Attribute att, TaggedValues tvs)
			throws EAException {

		if (tvs == null || tvs.isEmpty()) {
			// nothing to do
		} else {

			Collection<AttributeTag> cTV = att.GetTaggedValues();
			String attName = att.GetName();

			for (String tag : tvs.keySet()) {

				String[] values = tvs.get(tag);

				for (String v : values) {

					AttributeTag tv = cTV.AddNew(tag, "");
					cTV.Refresh();

					if (v.length() > 255) {
						tv.SetValue("<memo>");
						tv.SetNotes(v);
					} else {
						tv.SetValue(v);
						tv.SetNotes("");
					}

					if (!tv.Update()) {
						throw new EAException(createMessage(message(102), tag,
								attName, v, tv.GetLastError()));
					}
				}
			}
		}
	}

	public static void deleteTaggedValue(Attribute a, String nameOfTVToDelete) {

		Collection<AttributeTag> cTV = a.GetTaggedValues();
		cTV.Refresh();

		for (short i = 0; i < cTV.GetCount(); i++) {
			AttributeTag tv = cTV.GetAt(i);
			if (tv.GetName().equalsIgnoreCase(nameOfTVToDelete)) {
				cTV.Delete(i);
			}
		}

		cTV.Refresh();
	}

	public static void setEAAllowDuplicates(Attribute att,
			boolean allowDuplicates) throws EAException {

		att.SetAllowDuplicates(allowDuplicates);

		if (!att.Update()) {
			throw new EAException(createMessage(message(101), att.GetName(),
					att.GetLastError()));
		}
	}

	public static void setEAIsCollection(Attribute att, boolean isCollection)
			throws EAException {

		att.SetIsCollection(isCollection);

		if (!att.Update()) {
			throw new EAException(createMessage(message(103), att.GetName(),
					att.GetLastError()));
		}
	}

	public static void setEAIsOrdered(Attribute att, boolean isOrdered)
			throws EAException {

		att.SetIsOrdered(isOrdered);

		if (!att.Update()) {
			throw new EAException(createMessage(message(104), att.GetName(),
					att.GetLastError()));
		}
	}

	public static void setEALength(Attribute att, String length)
			throws EAException {

		att.SetLength(length);

		if (!att.Update()) {
			throw new EAException(createMessage(message(105), att.GetName(),
					length, att.GetLastError()));
		}
	}

	public static void setEAPrecision(Attribute att, int precision)
			throws EAException {

		att.SetPrecision("" + precision);

		if (!att.Update()) {
			throw new EAException(createMessage(message(106), att.GetName(),
					att.GetLastError()));
		}
	}

	public static void setEAScale(Attribute att, int scale) throws EAException {

		att.SetScale("" + scale);

		if (!att.Update()) {
			throw new EAException(createMessage(message(107), att.GetName(),
					att.GetLastError()));
		}
	}

	/**
	 * Sets the given tagged value in the tagged values of the given attribute.
	 * If tagged values with the same tag name already exist, they will be
	 * deleted. Then the tagged value will be added.
	 * 
	 * @param att
	 *            the attribute in which the tagged value shall be set
	 * @param tv
	 *            tagged value to set, must not be <code>null</code>
	 * @throws EAException  tbd
	 */
	public static void setTaggedValue(Attribute att, EATaggedValue tv)
			throws EAException {

		deleteTaggedValue(att, tv.getName());
		addTaggedValue(att, tv);
	}

	/**
	 * Sets the given tagged value in the tagged values of the given attribute.
	 * If tagged values with the same tag name already exist, they will be
	 * deleted. Then the tagged value will be added.
	 * 
	 * @param att
	 *            the attribute in which the tagged value shall be set
	 * @param name
	 *            name of the tagged value to set, must not be <code>null</code>
	 * @param value
	 *            value of the tagged value to set, can be <code>null</code>
	 * @throws EAException  tbd
	 */
	public static void setTaggedValue(Attribute att, String name, String value)
			throws EAException {

		EATaggedValue tv = new EATaggedValue(name, value);

		deleteTaggedValue(att, tv.getName());
		addTaggedValue(att, tv);
	}

	/**
	 * Sets the given tagged values in the given attribute. If tagged values
	 * with the same name as the given ones already exist, they will be deleted.
	 * Then the tagged values will be added.
	 * 
	 * @param att
	 *            the attribute in which the tagged values shall be set
	 * @param tvs
	 *            tagged values to set, must not be <code>null</code>
	 * @throws EAException  tbd
	 */
	public static void setTaggedValues(Attribute att, List<EATaggedValue> tvs)
			throws EAException {

		for (EATaggedValue tv : tvs) {
			deleteTaggedValue(att, tv.getName());
		}
		addTaggedValues(att, tvs);
	}

	/**
	 * Sets the given tagged values in the given attribute. If tagged values
	 * with the same name as the given ones already exist, they will be deleted.
	 * Then the tagged values will be added.
	 * 
	 * @param att
	 *            the attribute in which the tagged values shall be set
	 * @param tvs
	 *            tagged values to set, must not be <code>null</code>
	 * @throws EAException  tbd
	 */
	public static void setTaggedValues(Attribute att, TaggedValues tvs)
			throws EAException {

		for (String tvName : tvs.asMap().keySet()) {
			deleteTaggedValue(att, tvName);
		}
		addTaggedValues(att, tvs);
	}

	/**
	 * Retrieves the first tagged value with given name of the given attribute.
	 * Does not apply normalization of tags, i.e. comparison is performed using
	 * string equality. With UML 2, there may be multiple values per tag. This
	 * method does NOT issue a warning if more than one value exists for the
	 * tag. I.e., use this method only for cases, where only one value per tag
	 * may be provided.
	 * 
	 * @param att
	 *            attribute that contains the tagged values to search
	 * @param tvName
	 *            name of the tagged value to retrieve
	 * @return The tagged value for the tag with given name or <code>null</code>
	 *         if the tagged value was not found. If there are multiple values
	 *         with the tag only the first is provided.
	 */
	public static String taggedValue(Attribute att, String tvName) {

		org.sparx.Collection<org.sparx.AttributeTag> tvs = att
				.GetTaggedValues();

		for (org.sparx.AttributeTag tv : tvs) {

			if (tvName.equals(tv.GetName())) {

				String v = tv.GetValue();

				if (v.equals("<memo>")) {
					v = tv.GetNotes();
				}

				return v;
			}
		}

		return null;
	}

	/**
	 * @param att tbd
	 * @return sorted map of the tagged values (key: {name '#' fqName}; value:
	 *         according EATaggedValue); can be empty but not <code>null</code>
	 */
	public static SortedMap<String, EATaggedValue> getEATaggedValuesWithCombinedKeys(
			Attribute att) {

		/*
		 * key: {name '#' fqName}; value: according EATaggedValue
		 */
		SortedMap<String, EATaggedValue> result = new TreeMap<String, EATaggedValue>();

		Collection<AttributeTag> tvs = att.GetTaggedValues();

		for (short i = 0; i < tvs.GetCount(); i++) {

			AttributeTag tv = tvs.GetAt(i);

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
	 * @param att tbd
	 * @return sorted map of the tagged values (key: {tag name}; value:
	 *         according EATaggedValue); can be empty but not <code>null</code>
	 */
	public static SortedMap<String, EATaggedValue> getEATaggedValuesWithPlainKeys(
			Attribute att) {

		/*
		 * key: {tag name}; value: according EATaggedValue
		 */
		SortedMap<String, EATaggedValue> result = new TreeMap<String, EATaggedValue>();

		Collection<AttributeTag> tvs = att.GetTaggedValues();

		for (short i = 0; i < tvs.GetCount(); i++) {

			AttributeTag tv = tvs.GetAt(i);

			String name = tv.GetName();
			String value;

			String tvValue = tv.GetValue();

			if (tvValue.equals("<memo>")) {
				value = tv.GetNotes();
			} else {
				value = tvValue;
			}

			String key = name;

			if (result.containsKey(key)) {
				EATaggedValue eatv = result.get(key);
				eatv.addValue(value);
			} else {
				result.put(key, new EATaggedValue(name, value));
			}
		}

		return result;
	}

	/**
	 * @param att
	 *            Attribute to which the new constraint shall be added.
	 * @param name
	 *            Name of the new constraint
	 * @param type
	 *            Type of the new constraint
	 * @param text
	 *            Text of the new constraint
	 * @return The new constraint
	 * @throws EAException tbd
	 */
	public static org.sparx.AttributeConstraint addConstraint(Attribute att,
			String name, String type, String text) throws EAException {

		Collection<org.sparx.AttributeConstraint> cons = att.GetConstraints();

		org.sparx.AttributeConstraint con = cons.AddNew(name, type);

		cons.Refresh();

		con.SetNotes(text);
		if (!con.Update()) {
			throw new EAException(createMessage(message(108), name,
					att.GetName(), con.GetLastError()));
		} else {
			return con;
		}
	}
	
	/**
	 * Updates the tagged values with given name (which can be a fully qualified
	 * name) in the tagged values of the given attribute. Does NOT delete those
	 * tagged values. NOTE: This method is especially useful when setting tagged
	 * values that are defined by an MDG / UML Profile, since these tagged
	 * values cannot be created programmatically (they are created by EA - for
	 * further details, see
	 * http://sparxsystems.com/forums/smf/index.php?topic=3859.0).
	 * 
	 * @param att
	 *            the attribute in which the tagged values shall be updated
	 * @param name
	 *            (fully qualified or unqualified) name of the tagged value to
	 *            update, must not be <code>null</code>
	 * @param value
	 *            value of the tagged value to update, can be <code>null</code>
	 * @param createAsMemoField
	 *            If set to <code>true</code>, the value shall be encoded using a
	 *            &lt;memo&gt; field, regardless of the actual length of the
	 *            value.
	 * @throws EAException
	 *             If updating the attribute did not succeed, this exception
	 *             contains the error message.
	 */
	public static void updateTaggedValue(Attribute att, String name, String value,
			boolean createAsMemoField) throws EAException {

		boolean isQualifiedName = name.contains("::");

		Collection<AttributeTag> cTV = att.GetTaggedValues();

		cTV.Refresh();

		for (short i = 0; i < cTV.GetCount(); i++) {

			AttributeTag tv = cTV.GetAt(i);

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
					throw new EAException(createMessage(message(102), name,
							att.GetName(), value, tv.GetLastError()));
				}
			}
		}

		cTV.Refresh();
	}

	public static String message(int mnr) {

		switch (mnr) {

		case 101:
			return "EA error encountered while updating 'AllowDuplicates' on EA attribute '$1$'. Error message is: $2$";
		case 102:
			return "EA error encountered while updating EA tagged value '$1$' on attribute '$2$' with value '$3$'. Error message is: $4$";
		case 103:
			return "EA error encountered while updating 'IsCollection' on EA attribute '$1$'. Error message is: $2$";
		case 104:
			return "EA error encountered while updating 'IsOrdered' on EA attribute '$1$'. Error message is: $2$";
		case 105:
			return "EA error encountered while updating 'Length' on EA attribute '$1$' with value '$2$'. Error message is: $3$";
		case 106:
			return "EA error encountered while updating 'Precision' on EA attribute '$1$'. Error message is: $2$";
		case 107:
			return "EA error encountered while updating 'Scale' on EA attribute '$1$'. Error message is: $2$";
		case 108:
			return "EA error encountered while updating new EA constraint '$1$' for attribute '$2$'. Error message is: $3$";
		default:
			return "(" + EAAttributeUtil.class.getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
