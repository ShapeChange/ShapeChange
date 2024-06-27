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

package de.interactive_instruments.shapechange.core.model;

/**
 * The TextConstraintImpl class is the common root of all TextConstraint 
 * interface implementations of the supported input models. 
 */
public class TextConstraintImpl implements TextConstraint {

	/** Model Element Context - class, attribute, operation, etc. This has to
	 * be downcast to the proper xxxInfo as specified by contextModelElmtType. 
	 */
	protected Info contextModelElmt = null;
	
	/** Model Element Context Type - the nature of the model context the
	 * OCL expression is specified in. */
	protected ModelElmtContextType contextModelElmtType = null;
		
	/** Type of the text-based constraint */
	protected String constraintType = null;
	
	/** Name of the constraint */
	protected String constraintName = null;

	/** The textual representation of the constraint */
	protected String constraintText = null;
	
	/** Constraint status. A string reflecting the status of the constraint
	 * in conspiracy between the model source and the code generator. */
	protected String constraintStatus = null;
	
	/** Inquire type of the constraint. */
	public String type() {
		return constraintType;
	} // type()

	/** Inquire name of the constraint. */
	public String name() {
		return constraintName.trim();
	} // name()

	/** Inquire status of the constraint. */
	public String status() {
		return constraintStatus;
	} // status()

	/** Inquire the textual representation of the constraint. For text-based
	 * constraints the textual representation is supposed to be the only valid
	 * representation. */
	public String text() {
		return constraintText;
	} // text()
	
	/** Inquire the model element context. Note the result must be downcast
	 * according to the value of the model element context type. */
	public Info contextModelElmt() {
		return contextModelElmt;
	} // contextModelElmt()

	/** Find out about the type of the context model element. Currently only 
	 * CLASS and ATTRIBUTE are supported. */ 
	public ModelElmtContextType contextModelElmtType() {
		return contextModelElmtType;
	} // contextModelElmtType()
}
