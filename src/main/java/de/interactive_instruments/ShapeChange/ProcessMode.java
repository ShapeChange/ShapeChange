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

import java.util.Locale;

/**
 * Enumeration of the execution modes available for ShapeChange processes.
 * 
 * @author echterhoff
 * 
 */
public enum ProcessMode {

	/**
	 * Signifies that the process shall be executed.
	 */
	enabled,

	/**
	 * Signifies that the process shall not be executed.
	 */
	disabled,

	/**
	 * Signifies that the process shall only be run for diagnostic purpose.
	 */
	diagnosticsonly;

	/**
	 * Parses the given string to identify if it represents one of the declared
	 * process modes.
	 * 
	 * @param mode String to check if it is one of the modes represented by this enumeration.
	 * @return The ProcessMode that represents the given String, or null.
	 */
	public static ProcessMode fromString(String mode) {

		ProcessMode result = null;
		
		try {
			result = ProcessMode.valueOf(mode.trim().toLowerCase(Locale.ENGLISH));
		} catch (IllegalArgumentException e1) {
			// given String mode does not match any of the defined enum values
			System.err.println(e1);
		} catch (NullPointerException e2) {
			// given String mode is null
			System.err.println(e2);
		}			
		
		
		return result;
	}

}
