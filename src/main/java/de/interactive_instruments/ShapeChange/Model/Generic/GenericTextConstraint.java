package de.interactive_instruments.ShapeChange.Model.Generic;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.TextConstraint;
import de.interactive_instruments.ShapeChange.Model.TextConstraintImpl;

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
 * (c) 2002-2013 interactive instruments GmbH, Bonn, Germany
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

/**
 * @author echterhoff
 * 
 */
public class GenericTextConstraint extends TextConstraintImpl {

	public GenericTextConstraint() {
		super();
	}
			
	/**
	 * Creates a generic TextConstraint implementation from a given one.
	 * 
	 * @param constraint tbd
	 */
	public GenericTextConstraint(TextConstraint constraint) {
		super();
		
		contextModelElmt = constraint.contextModelElmt();
		contextModelElmtType = constraint.contextModelElmtType();
		constraintName = constraint.name();
		constraintStatus = constraint.status();
		constraintText = constraint.text();
		constraintType = constraint.type();
	}

	/**
	 * @param genCi context model element of the new constraint
	 * @param origCon Constraint to copy the text information from
	 */
	public GenericTextConstraint(GenericClassInfo genCi, Constraint origCon) {
		super();
		contextModelElmt = genCi;
		contextModelElmtType = ModelElmtContextType.CLASS;
		constraintName = origCon.name();
		constraintStatus = origCon.status();
		constraintText = origCon.text();
		constraintType = determineConstraintType(origCon);
	}
	
	/**
	 * @param genPi context model element of the new constraint
	 * @param origCon Constraint to copy the text information from
	 */
	public GenericTextConstraint(GenericPropertyInfo genPi, Constraint origCon) {
		super();
		contextModelElmt = genPi;
		contextModelElmtType = ModelElmtContextType.ATTRIBUTE;
		constraintName = origCon.name();
		constraintStatus = origCon.status();
		constraintText = origCon.text();
		constraintType = determineConstraintType(origCon);
	}
	
	/**
	 * @param constr
	 * @return the type of the constraint; can be empty but not
	 *         <code>null</code>
	 */
	private String determineConstraintType(Constraint constr) {

		String type = "OCL";
		if (constr instanceof TextConstraint) {
			type = ((TextConstraint) constr).type();
			if(StringUtils.isBlank(type)) {
				type = "Text";
			}
		}
		return type;
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
	
	public void setType(String type) {
		constraintType = type;
	}

}
