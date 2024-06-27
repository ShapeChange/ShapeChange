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
 * (c) 2002-2024 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core;

import java.util.Locale;

/**
 * Enumeration of the validation modes available for ShapeChange validators.
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public enum ValidationMode {

    /**
     * Signifies that if model validation failed, the process shall not be executed.
     */
    strict,

    /**
     * Signifies that if model validation failed, the process shall still be
     * executed.
     */
    lax;

    /**
     * Parses the given string to identify if it represents one of the declared
     * validation modes.
     * 
     * @param mode String to check if it is one of the modes represented by this
     *             enumeration.
     * @return The ValidationMode that represents the given String, or null.
     */
    public static ValidationMode fromString(String mode) {

	ValidationMode result = null;

	try {
	    result = ValidationMode.valueOf(mode.trim().toLowerCase(Locale.ENGLISH));
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