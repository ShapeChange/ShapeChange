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

import org.sparx.Element;
import org.sparx.Package;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class EAPackageUtil extends AbstractEAUtil {

	/**
	 * Sets the given tagged value in the tagged values of the given package. If
	 * tagged values with the same tag name already exist, they will be deleted.
	 * Then the tagged value will be added.
	 * 
	 * @param pkg
	 *            the package in which the tagged value shall be set
	 * @param name
	 *            name of the tagged value to set, must not be <code>null</code>
	 * @param value
	 *            value of the tagged value to set, can be <code>null</code>
	 */
	public static void setTaggedValue(Package pkg, String name, String value)
			throws EAException {

		Element e = pkg.GetElement();

		EATaggedValue tv = new EATaggedValue(name, value);

		EAElementUtil.deleteTaggedValue(e, tv.getName());
		EAElementUtil.addTaggedValue(e, tv);
	}

	public static String message(int mnr) {

		switch (mnr) {

		case 101:
			return "";
		default:
			return "(" + EAPackageUtil.class.getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
