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
 * (c) 2002-2012 interactive instruments GmbH, Bonn, Germany
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

/**
 * <p>
 * This mixin is for letting the retrieval of messages by message number being
 * redirected to the Targets of whatever subsystem of ShapeChange. Specific
 * Target-related message texts can therefore be kept separated from general
 * ShapeChange core messages.
 * </p>
 * <p>
 * An object implementing this interface can be passed to message generating
 * methods on the ShapeChangeResult object, such as <i>addWarning(...)</i>. If
 * <i>null</i> is handed over instead, the message text will be automatically
 * retrieved from the pool of centralized ShapeChangeResult messages.
 * </p>
 * 
 * @author Reinhard Erstling
 */
public interface MessageSource {

    /**
     * Return a message for a message number.
     * 
     * @param mnr the number of the message
     * @return the message
     */
    public String message(int mnr);
}
