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

import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.FolConstraint;
import de.interactive_instruments.ShapeChange.Model.FolConstraintImpl;

/**
 * @author Johannes Echterhoff
 *
 */
public class GenericFolConstraint extends FolConstraintImpl {

	
	public GenericFolConstraint() {
		super();
	}
	
	/**
	 * Creates a generic First Order Logic constraint.
	 * 
	 * @param ci
	 *            context for this constraint in the model; NOTE: after
	 *            construction this constraint has the reference to the given
	 *            Info object as-is; it must be updated to reference the correct
	 *            generic Info object during GenericModel construction
	 * @param name
	 * @param status
	 * @param text
	 */
	public GenericFolConstraint(ClassInfo ci, String name, String status,
			String sourceType, String text) {

		contextModelElmtType = ModelElmtContextType.CLASS;
		contextModelElmt = ci;

		constraintName = name;
		constraintStatus = status;

		constraintText = text;

		this.sourceType = sourceType;
	}

	/**
	 * Used to initialize a copy of the given constraint. This constructor is
	 * used while establishing the generic model. After construction this
	 * constraint uses the contextModelElmt of the given constraint as-is. It
	 * must be updated to reference the correct object.
	 * <p>
	 * The constraint itself is NOT parsed again when creating a GenericModel
	 * and fol expression is set to <code>null</code>, for the following
	 * reason(s):
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
	 * @param con
	 */
	GenericFolConstraint(FolConstraint con) {

		contextModelElmtType = con.contextModelElmtType();
		contextModelElmt = con.contextModelElmt();

		constraintName = con.name();
		constraintStatus = con.status();

		constraintText = con.text();

		sourceType = con.sourceType();

		comments = con.comments();

		folExpr = null;
	}

}
