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
package de.interactive_instruments.ShapeChange.Model.Generic;

import java.io.StringReader;

import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.OclConstraint;
import de.interactive_instruments.ShapeChange.Model.OclConstraintImpl;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Ocl.OclParser;

/**
 * @author echterhoff
 * 
 */
public class GenericOclConstraint extends OclConstraintImpl {

	/**
	 * Creates a new OCL constraint from the given constraint (with same
	 * context, but having parsed the OCL anew from the text of the given
	 * constraint).
	 * 
	 * @param ci
	 *            context of the constraint
	 * @param constr
	 *            original constraint from which to create a copy
	 */
	public GenericOclConstraint(GenericClassInfo ci, OclConstraint constr) {

		super();

		contextClass = ci;
		contextModelElmtType = ModelElmtContextType.CLASS;
		contextModelElmt = ci;

		this.initializeNonContextFields(constr.name(), constr.status(),
				constr.text(), contextModelElmt);
	}

	/**
	 * Creates a new OCL constraint with the given class as context, and OCL
	 * parsed from the given text.
	 * 
	 * @param ci
	 *            context of the constraint
	 * @param constrName
	 * @param constrStatus
	 *            see {@link Constraint#status()}
	 * @param constrText
	 *            OCL to be parsed
	 */
	public GenericOclConstraint(GenericClassInfo ci, String constrName,
			String constrStatus, String constrText) {

		super();

		contextClass = ci;
		contextModelElmtType = ModelElmtContextType.CLASS;
		contextModelElmt = ci;

		this.initializeNonContextFields(constrName, constrStatus, constrText,
				contextModelElmt);
	}

	private void initializeNonContextFields(String constrName,
			String constrStatus, String constrText,
			Info contextModelElement) {

		constraintName = constrName;
		constraintStatus = constrStatus;
		constraintText = constrText;
		
		syntaxTree = null;
		Readable instream = new StringReader(constraintText);
		OclParser parse = new OclParser();

		syntaxTree = parse.parseOcl(instream, contextModelElmt);

		if (syntaxTree != null)
			conditionType = syntaxTree.expressionType;

		comments = parse.getComments();
	}

	/**
	 * Creates a new OCL constraint from the given constraint (with same
	 * context, but having parsed the OCL anew from the text of the given
	 * constraint).
	 * 
	 * @param pi
	 *            context model element of the constraint (its inClass() defines
	 *            the context class)
	 * @param constr
	 *            original constraint from which to create a copy
	 */
	public GenericOclConstraint(GenericPropertyInfo pi, OclConstraint constr) {

		super();

		contextClass = pi.inClass();
		contextModelElmtType = ModelElmtContextType.ATTRIBUTE;
		contextModelElmt = pi;

		this.initializeNonContextFields(constr.name(), constr.status(),
				constr.text(), contextModelElmt);
	}

	/**
	 * Creates a new OCL constraint with the given property as context, and OCL
	 * parsed from the given text.
	 * 
	 * @param pi
	 *            context of the constraint
	 * @param constrName
	 * @param constrStatus
	 *            see {@link Constraint#status()}
	 * @param constrText
	 *            OCL to be parsed
	 */
	public GenericOclConstraint(PropertyInfo pi, String constrName,
			String constrStatus, String constrText) {

		super();

		contextClass = pi.inClass();
		contextModelElmtType = ModelElmtContextType.ATTRIBUTE;
		contextModelElmt = pi;

		this.initializeNonContextFields(constrName, constrStatus, constrText,
				contextModelElmt);
	}

	/**
	 * Used to initialize a copy of the given constraint. This constructor is
	 * used while establishing the generic model. The context is not set when
	 * initializing the constraint with this constructor. It needs to be set
	 * explicitly via the setContext method. However, the constraint itself is
	 * parsed anew.
	 * 
	 * @param constr
	 */
	GenericOclConstraint(OclConstraint constr) {

		contextClass = constr.contextClass();
		contextModelElmt = constr.contextModelElmt();
		contextModelElmtType = constr.contextModelElmtType();

		this.initializeNonContextFields(constr.name(), constr.status(),
				constr.text(), contextModelElmt);
	}

	public void setContext(ClassInfo contextClass, Info contextModelElement) {
		this.contextModelElmt = contextModelElement;
		this.contextClass = contextClass;

		if (contextModelElement instanceof PropertyInfo) {
			this.contextModelElmtType = ModelElmtContextType.ATTRIBUTE;
		} else if (contextModelElement instanceof ClassInfo) {
			this.contextModelElmtType = ModelElmtContextType.CLASS;
		} else {
			// TODO debug info
		}
	}
}
