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
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */

package de.interactive_instruments.ShapeChange.Model;

import de.interactive_instruments.ShapeChange.Ocl.OclNode;

/**
 * The OclConstraintImpl class is the common root of all OclConstraint 
 * interface implementations of the supported input models. 
 */
public abstract class OclConstraintImpl implements OclConstraint {
	
	/** Class Context - the class, which represents 'self' */
	protected ClassInfo contextClass = null;
	
	/** Model Element Context - class, attribute, operation, etc. This has to
	 * be downcast to the proper xxxInfo as specified by contextModelElmtType. 
	 */
	protected Info contextModelElmt = null;
	
	/** Model Element Context Type - the nature of the model context the
	 * OCL expression is specified in. */
	protected ModelElmtContextType contextModelElmtType = null;
	
	/** Condition Type - the nature of the condition */
	protected ConditionType conditionType = null;
	
	/** Name of the constraint */
	protected String constraintName = null;

	/** The textual representation of the constraint */
	protected String constraintText = null;
	
	/** Constraint status. A string reflecting the status of the constraint
	 * in conspiracy between the model source and the code generator. */
	protected String constraintStatus = null;

	/** Compiled representation */
	protected OclNode.Expression syntaxTree;
	
	/** Comments contained in the constraint */
	protected String[] comments = null;
	
	/** Inquire the condition type. */
	public ConditionType conditionType() {
		return conditionType;
	} // conditionType()

	/** Inquire the context class of the OCL constraint - the 'self' */
	public ClassInfo contextClass() {
		return contextClass;
	} // contextClass()

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

	/** Inquire name of the constraint. */
	public String name() {
		return constraintName.trim();
	} // name()

	/** Inquire status of the constraint. */
	public String status() {
		return constraintStatus;
	} // status()

	/** Inquire the textual representation of the OCL expression. The text is
	 * supposed to start with condition type inv:, derive: or init:. The string 
	 * may contain \n characters. */
	public String text() {
		return constraintText;
	} // text()
	
	/** If compilation went well, this returns the OCL syntax tree. */
	public OclNode.Expression syntaxTree() {
		return syntaxTree;
	} // syntaxTree()
	
	/** The comments contained in the OCL expression */
	public String[] comments() {
		return comments;
	}
}
