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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange.Model.Generic;

import java.io.StringReader;

import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.OclConstraint;
import de.interactive_instruments.ShapeChange.Model.OclConstraintImpl;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Ocl.MessageCollection;
import de.interactive_instruments.ShapeChange.Ocl.OclParser;

/**
 * @author echterhoff
 * 
 */
public class GenericOclConstraint extends OclConstraintImpl {

	public GenericOclConstraint() {
		super();
	}

	/**
	 * Creates a new OCL constraint from the given constraint (with same context
	 * and comments, but having parsed the OCL anew from the text of the given
	 * constraint).
	 * 
	 * @param ci
	 *                   context of the constraint
	 * @param constr
	 *                   original constraint from which to create a copy
	 */
	public GenericOclConstraint(GenericClassInfo ci, OclConstraint constr) {

		super();

		contextClass = ci;
		contextModelElmtType = ModelElmtContextType.CLASS;
		contextModelElmt = ci;

		/*
		 * Initialise comments with that from the given constraint. During the
		 * following initialization, they may be merged with those parsed from
		 * the constraint text.
		 */
		comments = constr.comments();

		this.initializeNonContextFields(constr.name(), constr.status(),
				constr.text(), contextModelElmt);
	}

	/**
	 * Creates a new OCL constraint with the given class as context, and OCL
	 * parsed from the given text.
	 * 
	 * @param ci
	 *                         context of the constraint
	 * @param constrName tbd
	 * @param constrStatus
	 *                         see {@link Constraint#status()}
	 * @param constrText
	 *                         OCL to be parsed
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
			String constrStatus, String constrText, Info contextModelElement) {

		constraintName = constrName;
		constraintStatus = constrStatus;
		constraintText = constrText;

		syntaxTree = null;
		Readable instream = new StringReader(constraintText);
		OclParser parse = new OclParser();

		syntaxTree = parse.parseOcl(instream, contextModelElmt);

		if (syntaxTree != null)
			conditionType = syntaxTree.expressionType;
		
		mergeComments(parse.getComments());
		
		// Output error messages as a warning, if there are ones
		if (parse.getNumberOfMessages() > 0) {

			String ctx, ctxType;
			if (this.contextModelElmtType == ModelElmtContextType.CLASS) {
				ctx = contextModelElement.name();
				ctxType = "class";
			} else {
				ctx = contextClass.name() + "." + contextModelElement.name();
				ctxType = "property";
			}

			ShapeChangeResult.MessageContext messctx = contextModelElement
					.result().addInfo(null, 133, ctx, constraintName, ctxType);

			if (messctx != null) {
				MessageCollection messages = parse.getMessageCollection();
				MessageCollection.Message[] msg = messages.getMessages();
				for (MessageCollection.Message m : msg) {
					final String[] del = { "/", "-", "," };
					String sr = m.getFormattedSourceReferences(1, 1, del);
					String ms = m.getMessageText();
					messctx.addDetail(null, 134, sr, ms);
				}
				String includeConstraintInMessages = contextModelElement
						.options().parameter("includeConstraintInMessages");
				if (includeConstraintInMessages != null
						&& includeConstraintInMessages.equals("true"))
					messctx.addDetail("Constraint: " + constraintText);
			}
		}
	}

	/**
	 * Creates a new OCL constraint from the given constraint (with same context
	 * and comments, but having parsed the OCL anew from the text of the given
	 * constraint).
	 * 
	 * @param pi
	 *                   context model element of the constraint (its inClass()
	 *                   defines the context class)
	 * @param constr
	 *                   original constraint from which to create a copy
	 */
	public GenericOclConstraint(GenericPropertyInfo pi, OclConstraint constr) {

		super();

		contextClass = pi.inClass();
		contextModelElmtType = ModelElmtContextType.ATTRIBUTE;
		contextModelElmt = pi;

		/*
		 * Initialise comments with that from the given constraint. During the
		 * following initialization, they may be merged with those parsed from
		 * the constraint text.
		 */
		comments = constr.comments();

		this.initializeNonContextFields(constr.name(), constr.status(),
				constr.text(), contextModelElmt);
	}

	/**
	 * Creates a new OCL constraint with the given property as context, and OCL
	 * parsed from the given text.
	 * 
	 * @param pi
	 *                         context of the constraint
	 * @param constrName tbd
	 * @param constrStatus
	 *                         see {@link Constraint#status()}
	 * @param constrText
	 *                         OCL to be parsed
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
	 * explicitly via the setContext method.
	 * <p>
	 * The constraint is NOT parsed again when creating a GenericModel and the
	 * syntax tree is set to <code>null</code>, for the following reason(s):
	 * <ul>
	 * <li>Save processing resources: parsing constraints each time a model is
	 * transformed can be costly, especially if the model is large and contains
	 * many constraints.
	 * <p>
	 * NOTE: When postprocessing a transformed model, the TransformationManager
	 * parses and validates OCL and FOL constraints by default. However, the
	 * TransformationManager has a rule with which this can be skipped if
	 * required.</li>
	 * <li>Avoid reference to previous model: a parsed expression usually
	 * references elements from the model that was used while parsing the
	 * constraint. If the expression from the original constraint was kept
	 * as-is, then this can lead to incorrect references.</li>
	 * <ul>
	 * 
	 * @param constr
	 */
	GenericOclConstraint(OclConstraint constr) {

		contextClass = constr.contextClass();
		contextModelElmt = constr.contextModelElmt();
		contextModelElmtType = constr.contextModelElmtType();

		constraintName = constr.name();
		constraintStatus = constr.status();
		constraintText = constr.text();

		syntaxTree = null;
		conditionType = null;

		comments = constr.comments();
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

	public void setContextModelElmt(Info contextModelElmt) {
		this.contextModelElmt = contextModelElmt;
	}

	public void setContextModelElmtType(
			ModelElmtContextType contextModelElmtType) {
		this.contextModelElmtType = contextModelElmtType;
	}

	public void setName(String name) {
		constraintName = name;
	}

	public void setStatus(String status) {
		constraintStatus = status;
	}

	public void setText(String text) {
		constraintText = text;
	}
}
