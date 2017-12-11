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

import java.util.SortedMap;
import java.util.TreeMap;

import org.sparx.Collection;
import org.sparx.ParamTag;
import org.sparx.Parameter;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class EAParameterUtil extends AbstractEAUtil {

	public static void setEAType(Parameter p, String type) throws EAException {

		p.SetType(type);

		if (!p.Update()) {
			throw new EAException(
					createMessage(message(101), p.GetName(), p.GetLastError()));
		}
	}
	
	/**
	 * @param att
	 * @return sorted map of the tagged values (key: {name '#' fqName}; value:
	 *         according EATaggedValue); can be empty but not <code>null</code>
	 */
	public static SortedMap<String, EATaggedValue> getEATaggedValues(
			Parameter att) {

		/*
		 * key: {name '#' fqName}; value: according EATaggedValue
		 */
		SortedMap<String, EATaggedValue> result = new TreeMap<String, EATaggedValue>();

		Collection<ParamTag> tvs = att.GetTaggedValues();

		for (short i = 0; i < tvs.GetCount(); i++) {

			ParamTag tv = tvs.GetAt(i);

			String name = tv.GetTag();
			String fqName = tv.GetFQName();
			String value = tv.GetValue();

			String key = name + "#" + fqName;

			if (result.containsKey(key)) {
				EATaggedValue eatv = result.get(name);
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
			return "EA error encountered while updating 'Type' of EA parameter '$1$'. Error message is: $2$";
		default:
			return "(" + EAMethodUtil.class.getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
