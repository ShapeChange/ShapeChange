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
 * (c) 2002-2013 interactive instruments GmbH, Bonn, Germany
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

package de.interactive_instruments.ShapeChange;

/**
 * A general class to represent entries in a mapping table.
 */
public class MapEntry {
	// Data
	public String rule;
	public String p1 = "";
	public String p2 = "";
	public String p3 = "";

	/**
	 * @param s1 ...
	 */
	public MapEntry(String s1) {
		rule = s1;
	}

	public MapEntry(String s1, String s2) {
		rule = s1;
		p1 = s2;
	}

	public MapEntry(String s1, String s2, String s3) {
		rule = s1;
		p1 = s2;
		p2 = s3;
	}

	public MapEntry(String s1, String s2, String s3, String s4) {
		rule = s1;
		p1 = s2;
		p2 = s3;
		p3 = s4;
	}
}
