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
 * (c) 2002-2015 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Transformation.Flattening;

import java.util.Set;

import org.jgrapht.graph.DefaultEdge;

/**
 * @author Johannes Echterhoff
 *
 * @param <V>
 */
public class PropertySetEdge extends DefaultEdge {
	/**
		 * 
		 */
	private static final long serialVersionUID = 1L;

	private String source;
	private String target;
	private String label;
	private Set<String> propertyNames;

	public PropertySetEdge(String source, String target, Set<String> propertyNames) {
		this.source = source;
		this.target = target;
		this.propertyNames = propertyNames;
		this.label = join(propertyNames, ",");
	}

	public String getV1() {
		return source;
	}

	public String getV2() {
		return target;
	}

	public Set<String> getPropertyNames() {
		return propertyNames;
	}

	public String toString() {
		return label;
	}

	/**
	 * Creates a string that contains the parts, separated by the given
	 * delimiter (if <code>null</code> it defaults to the empty string). If the
	 * set contains a null element, it is ignored. Joins the parts in the order
	 * returned by the iterator. If order of the set is important, ensure that
	 * an ordered set is used (e.g. TreeSet).
	 * 
	 * @param parts
	 * @param delimiter
	 * @return
	 */
	public static String join(Set<String> parts, String delimiter) {

		if (parts == null || parts.isEmpty()) {
			return "";
		}

		StringBuilder sb = new StringBuilder();

		String delim = delimiter == null ? "" : delimiter;

		for (String part : parts) {
			if (part != null) {
				sb.append(part);
			}
			sb.append(delim);
		}

		return sb.substring(0, sb.length() - delim.length());
	}
}
