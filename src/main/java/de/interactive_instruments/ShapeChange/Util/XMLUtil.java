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
package de.interactive_instruments.ShapeChange.Util;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class XMLUtil {

	/**
	 * @param parent
	 *            the element in which to look for the first child element with
	 *            given name
	 * @param elementName
	 *            name of the child element to look up
	 * @return the first child element with given name; can be <code>null</code>
	 *         if no such element was found
	 */
	public static Element getFirstElement(Element parent, String elementName) {

		NodeList nl = parent.getElementsByTagName(elementName);

		if (nl != null && nl.getLength() != 0) {

			for (int k = 0; k < nl.getLength(); k++) {

				Node n = nl.item(k);
				if (n.getNodeType() == Node.ELEMENT_NODE) {

					return (Element) n;
				}
			}
		}

		return null;
	}
}
