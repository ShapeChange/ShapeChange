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
 * (c) 2002-2025 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.ea.util;

import org.sparx.AttributeConstraint;

import shadow.org.apache.commons.lang3.StringUtils;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class EAAttributeConstraintUtil extends AbstractEAUtil {

    public static void setEANotes(AttributeConstraint ac, String notes) throws EAException {

	ac.SetNotes(notes);

	if (!ac.Update()) {
	    throw new EAException(createMessage(message(101), ac.GetName(), ac.GetLastError()));
	}
    }

    public static void appendToEANotes(AttributeConstraint ac, String additionalText) throws EAException {

	String existingText = ac.GetNotes();

	if (StringUtils.isBlank(existingText)) {
	    ac.SetNotes(additionalText);
	} else {
	    ac.SetNotes(existingText + "\n\n" + additionalText);
	}

	if (!ac.Update()) {
	    throw new EAException(createMessage(message(102), ac.GetName(), ac.GetLastError()));
	}

    }

    public static String message(int mnr) {

	return switch (mnr) {
	case 101 -> "EA error encountered while updating 'Notes' of EA attribute constraint '$1$'. Error message is: $2$";
	case 102 -> "EA error encountered while appending to 'Notes' of EA attribute constraint '$1$'. Error message is: $2$";
	default -> "(" + EAAttributeConstraintUtil.class.getName() + ") Unknown message with number: " + mnr;
	};
    }
}
