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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments <dot>
 *         de)
 *
 */
public class XMLUtil {

	/**
	 * @param parent
	 *                        the element in which to look for the first child
	 *                        element with given name
	 * @param elementName
	 *                        name of the child element to look up
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

	/**
	 * @param parent
	 *                        the element in which to look for the first child
	 *                        element with given name
	 * @param elementName
	 *                        name of the child element to look up
	 * @return the text content of the first child element with given name; can
	 *         be <code>null</code> if no such element was found
	 */
	public static String getTextContentOfFirstElement(Element parent,
			String elementName) {
		Element e = getFirstElement(parent, elementName);
		return e != null ? e.getTextContent() : null;
	}

	/**
	 * @param parentElement
	 *                          Element in which to look up the children with
	 *                          given name
	 * @param elementName
	 *                          name of child elements to look up
	 * @return List of child elements of the given parent element that have the
	 *         given element name. Can be empty but not <code>null</code>.
	 */
	public static List<Element> getChildElements(Element parentElement,
			String elementName) {

		List<Element> result = new ArrayList<Element>();

		NodeList nl = parentElement.getElementsByTagName(elementName);

		if (nl != null && nl.getLength() != 0) {
			for (int k = 0; k < nl.getLength(); k++) {

				Node n = nl.item(k);

				if (n.getNodeType() == Node.ELEMENT_NODE) {

					result.add((Element) n);
				}
			}
		}

		return result;
	}

	/**
	 * @param parentElement
	 *                          Element in which to look up the children with
	 *                          given name
	 * @param elementName
	 *                          name of child elements to look up
	 * @return List of text contents of the child elements of the given parent
	 *         element that have the given element name. Can be empty but not
	 *         <code>null</code>.
	 */
	public static List<String> getTextContentOfChildElements(
			Element parentElement, String elementName) {

		List<String> result = new ArrayList<>();

		List<Element> elements = getChildElements(parentElement, elementName);

		for (Element e : elements) {
			result.add(e.getTextContent());
		}

		return result;
	}

	/**
	 * @param s
	 * @return <code>true</code> if the given string equals '1' or equals,
	 *         ignoring case, 'true'; else <code>false</code>
	 */
	public static boolean parseBoolean(String s) {

		if (StringUtils.isBlank(s)) {
			return false;
		} else if (s.trim().equalsIgnoreCase("true") || s.trim().equals("1")) {
			return true;
		} else {
			return false;
		}
	}
}
