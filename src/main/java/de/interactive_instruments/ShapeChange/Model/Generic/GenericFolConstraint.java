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
	 * Constraints a generic FOL constraint from the given one.
	 * 
	 * NOTE: after construction this constraint uses the references of the given
	 * constraint as-is (applies to contextModelElmt and folExpr); they must be
	 * updated to reference the correct objects
	 * 
	 * @param con
	 */
	public GenericFolConstraint(FolConstraint con) {

		contextModelElmtType = con.contextModelElmtType();
		contextModelElmt = con.contextModelElmt();

		constraintName = con.name();
		constraintStatus = con.status();

		constraintText = con.text();

		sourceType = con.sourceType();

		comments = con.comments();
		/*
		 * 2015-04-21 JE: reusing the first order logic expression as-is is
		 * probably the best approach right now. A transformation must be very
		 * advanced if it updated the model and then tried to update constraints
		 * as well to keep them in synch with the model.
		 */
		folExpr = con.folExpression();
	}
}
