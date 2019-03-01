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
package de.interactive_instruments.ShapeChange.Profile;

import java.util.Arrays;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 */
public class VersionNumber {

	public int[] components = null;

	public VersionNumber(String vn) {
		String[] cmp = vn.split("\\.");
		components = new int[cmp.length];
		for (int i = 0; i < cmp.length; i++) {
			try {
				components[i] = Integer.parseInt(cmp[i]);
			} catch (NumberFormatException e) {
				components[i] = 0;
			}
		}
	}

	public VersionNumber(int[] components) {
		this.components = components;
	}

	public VersionNumber(int component) {
		this.components = new int[] { component };
	}

	/**
	 * Compares this version number with the given one. If the length of the
	 * component arrays is different, they are compared by assuming '0' as
	 * values for the missing components in the shorter component array.
	 * 
	 * @param other
	 *            the version number to compare this number with
	 * @return -1 if this version number is less than the other number, 0 if it
	 *         is equal, 1 if it is bigger
	 */
	public int compareTo(VersionNumber other) {

		int[] thisComps, otherComps;
		if (components.length == other.components.length) {
			thisComps = this.components;
			otherComps = other.components;
		} else if (components.length > other.components.length) {
			thisComps = this.components;
			otherComps = Arrays.copyOf(other.components,
					this.components.length);
		} else {
			// components.length < other.components.length
			thisComps = Arrays.copyOf(this.components, other.components.length);
			otherComps = other.components;
		}

		for (int i = 0; i < thisComps.length; i++) {
			// Compare component
			int diff = thisComps[i] - otherComps[i];
			// If other than equal return difference
			if (diff != 0)
				return diff;
		}
		return 0;
	}

	public VersionNumber copyForVersionRangeEnd() {
		int[] componentCopy = Arrays.copyOf(components, components.length);
		componentCopy[components.length - 1]++;
		return new VersionNumber(componentCopy);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < components.length; i++) {
			sb.append(components[i]);
			if (i < components.length - 1) {
				sb.append(".");
			}
		}
		return sb.toString();
	}

	public VersionNumber createCopy() {

		int[] componentsCopy = Arrays.copyOf(this.components,
				this.components.length);
		return new VersionNumber(componentsCopy);
	}

}
