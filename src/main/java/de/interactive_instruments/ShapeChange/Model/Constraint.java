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

package de.interactive_instruments.ShapeChange.Model;

/**
 * The Constraint interface stands for any type of constraints, which may be
 * attached to UML metamodel objects. A Constraint is supposed to have a name, a
 * status and a textual representation.
 */
public interface Constraint {

    /** Type for possible model elements the constraint applies to */
    enum ModelElmtContextType {
	CLASS, // Context is a Class
	ATTRIBUTE // Context is a Property being a class attribute
    }

    /**
     * Name of the constraint
     * 
     * @return tbd
     */
    public String name();

    /**
     * Status of the constraint. Note: While this is experimental software the
     * 'status' of a constraint is meant to be some string in conspiracy between the
     * model source and the code generator. This may be changed to an enum later,
     * when some practice is achieved. The 'status' is supposed to express some
     * state of refinedness, validity or purpose of the constraint.
     * 
     * @return tbd
     */
    public String status();

    /**
     * The textual representation of the constraint.
     * 
     * @return tbd
     */
    public String text();

    /**
     * Inquire context model element, i.e. the parent. Currently this in one of
     * ClassInfo or PropertyInfo.
     * 
     * @return tbd
     */
    public Info contextModelElmt();

    /**
     * Inquire model element context type.
     * 
     * @return tbd
     */
    public ModelElmtContextType contextModelElmtType();

}
