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
 * (c) 2002-2018 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Util;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class ArcGISUtil {

	/**
	 * @param ci
	 * @return <code>true</code> if one of the attributes of the given class has
	 *         tagged value 'arcgisDefaultSubtype' with non-empty value, else
	 *         <code>false</code>.
	 */
	public static boolean hasArcGISDefaultSubtypeAttribute(ClassInfo ci) {

		for (PropertyInfo pi : ci.properties().values()) {
			if (StringUtils
					.isNotBlank(pi.taggedValue("arcgisDefaultSubtype"))) {
				return true;
			}
		}

		return false;
	}

	/**
	 * @param ci
	 * @return <code>true</code> if one of the supertypes of the given class has
	 *         a property with non-empty tagged value 'arcgisDefaultSubtype';
	 *         else <code>false</code>.
	 */
	public static boolean isArcGISSubtype(ClassInfo ci) {

		Model model = ci.model();

		for (String supertypeID : ci.supertypes()) {

			ClassInfo supertype = model.classById(supertypeID);

			if (supertype != null
					&& hasArcGISDefaultSubtypeAttribute(supertype)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * @param ci
	 * @return <code>true</code> if the class has a subtype that represents an
	 *         ArcGIS subtype (i.e., one of its supertypes has a property with
	 *         non-empty tagged value 'arcgisDefaultSubtype').
	 */
	public static boolean hasArcGISSubtype(ClassInfo ci) {

		Model model = ci.model();

		for (String subtypeID : ci.subtypes()) {

			ClassInfo subtype = model.classById(subtypeID);

			if (subtype != null && isArcGISSubtype(subtype)) {
				return true;
			}
		}

		return false;
	}
}
