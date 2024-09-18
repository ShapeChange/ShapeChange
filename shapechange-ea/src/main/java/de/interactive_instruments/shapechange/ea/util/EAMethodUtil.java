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
package de.interactive_instruments.shapechange.ea.util;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.sparx.Collection;
import org.sparx.Method;
import org.sparx.MethodTag;
import org.sparx.Parameter;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
public class EAMethodUtil extends AbstractEAUtil {

	/**
	 * Sets 'StereotypeEx' on the given EA method.
	 * 
	 * @param m tbd
	 * @param stereotype tbd
	 * @throws EAException
	 *             If updating the method did not succeed, this exception
	 *             contains the error message.
	 */
	public static void setEAStereotypeEx(Method m, String stereotype)
			throws EAException {

		m.SetStereotypeEx(stereotype);

		if (!m.Update()) {
			throw new EAException(
					createMessage(message(101), m.GetName(), m.GetLastError()));
		}
	}

	/**
	 * Sets 'Code' on the given EA method.
	 * 
	 * @param m tbd
	 * @param code tbd
	 * @throws EAException
	 *             If updating the method did not succeed, this exception
	 *             contains the error message.
	 */
	public static void setEACode(Method m, String code) throws EAException {

		m.SetCode(code);

		if (!m.Update()) {
			throw new EAException(
					createMessage(message(103), m.GetName(), m.GetLastError()));
		}
	}

	/**
	 * Deletes all tagged values whose name equals (ignoring case) the given
	 * name in the given method.
	 * 
	 * @param m tbd
	 * @param nameOfTVToDelete tbd
	 */
	public static void deleteTaggedValue(Method m, String nameOfTVToDelete) {

		Collection<MethodTag> cTV = m.GetTaggedValues();
		cTV.Refresh();

		for (short i = 0; i < cTV.GetCount(); i++) {
			MethodTag tv = cTV.GetAt(i);
			if (tv.GetName().equalsIgnoreCase(nameOfTVToDelete)) {
				cTV.Delete(i);
			}
		}

		cTV.Refresh();
	}

	/**
	 * Sets the given tagged values in the given method. If tagged values with
	 * the same name as the given ones already exist, they will be deleted. Then
	 * the tagged values will be added.
	 * 
	 * @param m
	 *            the method in which the tagged values shall be set
	 * @param tvs
	 *            tagged values to set, must not be <code>null</code>
	 * @throws EAException  tbd
	 */
	public static void setTaggedValues(Method m, List<EATaggedValue> tvs)
			throws EAException {

		for (EATaggedValue tv : tvs) {
			deleteTaggedValue(m, tv.getName());
		}
		addTaggedValues(m, tvs);
	}

	/**
	 * Adds the given list of tagged values to the collection of (EA) tagged
	 * values of the given method, NOT checking for duplicate tags.
	 * <p>
	 * <b>WARNING:</b> Enterprise Architect may initialize default tagged values
	 * for a method that adheres to a specific UML profile. In that case, adding
	 * the same tagged values would lead to duplicates. If duplicates shall be
	 * prevented, set the tagged value instead of adding it.
	 * 
	 * @param m tbd
	 * @param tvs
	 *            collection of tagged values to add
	 * @throws EAException  tbd
	 */
	public static void addTaggedValues(Method m, List<EATaggedValue> tvs)
			throws EAException {

		if (tvs == null || tvs.isEmpty()) {

			// nothing to do

		} else {

			Collection<MethodTag> cTV = m.GetTaggedValues();
			cTV.Refresh();

			for (EATaggedValue tv : tvs) {

				String name = tv.getName();
				List<String> values = tv.getValues();

				if (values != null) {

					for (String v : values) {

						MethodTag eaTv = cTV.AddNew(name, "");
						cTV.Refresh();

						if (tv.createAsMemoField() || v.length() > 255) {
							eaTv.SetValue("<memo>");
							eaTv.SetNotes(v);
						} else {
							eaTv.SetValue(v);
							eaTv.SetNotes("");
						}

						if (!eaTv.Update()) {
							throw new EAException(createMessage(message(105),
									name, m.GetName(), v, eaTv.GetLastError()));
						}
					}
				}
			}
		}
	}

	/**
	 * Sets 'Pos' on the given EA method.
	 * 
	 * @param m tbd
	 * @param pos tbd
	 * @throws EAException
	 *             If updating the method did not succeed, this exception
	 *             contains the error message.
	 */
	public static void setEAPos(Method m, int pos) throws EAException {

		m.SetPos(pos);

		if (!m.Update()) {
			throw new EAException(
					createMessage(message(104), m.GetName(), m.GetLastError()));
		}
	}

	public static Parameter createEAParameter(Method m, String name)
			throws EAException {

		Collection<Parameter> parameters = m.GetParameters();
		parameters.Refresh();

		Parameter param = parameters.AddNew(name, "");

		if (!param.Update()) {
			throw new EAException(createMessage(message(107), name, m.GetName(),
					m.GetLastError()));
		}

		parameters.Refresh();

		return param;
	}

	/**
	 * @param m tbd
	 * @return the first parameter defined for this method, or <code>null</code>
	 *         if no such parameter exists
	 */
	public static Parameter getFirstParameter(Method m) {

		Collection<Parameter> params = m.GetParameters();
		params.Refresh();

		if (params.GetCount() > 0) {
			return params.GetAt((short) 0);
		} else {
			return null;
		}
	}
	
	/**
	 * @param meth tbd
	 * @return sorted map of the tagged values (key: {name '#' fqName}; value:
	 *         according EATaggedValue); can be empty but not <code>null</code>
	 */
	public static SortedMap<String, EATaggedValue> getEATaggedValues(
			Method meth) {

		/*
		 * key: {name '#' fqName}; value: according EATaggedValue
		 */
		SortedMap<String, EATaggedValue> result = new TreeMap<String, EATaggedValue>();

		Collection<MethodTag> tvs = meth.GetTaggedValues();
		tvs.Refresh();

		for (short i = 0; i < tvs.GetCount(); i++) {

			MethodTag tv = tvs.GetAt(i);

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

	public static String message(int mnr) {

		switch (mnr) {
		case 101:
			return "EA error encountered while updating 'StereotypeEx' of EA method '$1$'. Error message is: $2$";
		case 102:
			return "EA error encountered while updating new EA parameter '$1$' on method '$2$'. Error message is: $3$";
		case 103:
			return "EA error encountered while updating 'Code' of EA method '$1$'. Error message is: $2$";
		case 104:
			return "EA error encountered while updating 'Pos' of EA method '$1$'. Error message is: $2$";
		case 105:
			return "EA error encountered while updating EA tagged value '$1$' of element '$2$' with value '$3$'. Error message is: $4$";
		default:
			return "(" + EAMethodUtil.class.getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
