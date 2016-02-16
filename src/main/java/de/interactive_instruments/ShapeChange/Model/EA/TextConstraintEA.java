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

package de.interactive_instruments.ShapeChange.Model.EA;

import org.sparx.AttributeConstraint;

import de.interactive_instruments.ShapeChange.Model.TextConstraint;
import de.interactive_instruments.ShapeChange.Model.TextConstraintImpl;

/**
 * This is the implementation of TextConstraint for the Enterprise Architect
 * model source. The constraint text is obtained from the Notes field of the
 * The type, status and name concepts are directly mapped to the corresponding 
 * EA dialog fields.
 */
public class TextConstraintEA extends TextConstraintImpl implements
		TextConstraint {

	/** The model object */
	protected EADocument document = null;
	
	/** The EA constraint object */
	protected org.sparx.Constraint eaConstraint;
	protected AttributeConstraint eaConstraintAttribute; 
	
	/** Ctor from class context */
	public TextConstraintEA(EADocument doc, ClassInfoEA ci,
		org.sparx.Constraint constr) {
		
		// Record the containment links.
		document = doc;

		contextModelElmtType = ModelElmtContextType.CLASS;
		contextModelElmt = ci;
		
		// The EA constraint object
		eaConstraint = constr;
		
		// Name, status and type
		constraintName = constr.GetName();
		constraintStatus = constr.GetStatus();
		constraintType = constr.GetType();

		// The constraint text - contains fix for EA7.5 bug
		constraintText = constr.GetNotes();
		if(constraintText!=null)
			constraintText = 
				EADocument.removeSpuriousEA75EntitiesFromStrings(
						constraintText);
	}

	/** Ctor from attribute context */
	public TextConstraintEA(EADocument doc, PropertyInfoEA pi,
		AttributeConstraint constr) {
		
		// Record the containment links.
		document = doc;

		contextModelElmtType = ModelElmtContextType.ATTRIBUTE;
		contextModelElmt = pi;

		// The EA constraint object
		eaConstraintAttribute = constr;
		
		// Name, status and type. Since EA does not deliver a status for 
		// attribute constraints we have to extract this from the name. 
		// Syntax is 'name[status]'.
		constraintName = constr.GetName().trim();
		int ib = constraintName.indexOf("[");
		int ie = constraintName.indexOf("]", ib);
		constraintStatus = "";
		if(ib!=-1&&ie!=-1) {
			constraintStatus = constraintName.substring(ib+1,ie).trim();
			constraintName = constraintName.substring(0, ib);
		}
		constraintType = constr.GetType();
			
		// The constraint text - contains fix for EA7.5 bug
		constraintText = constr.GetNotes();
		if(constraintText!=null)
			constraintText = 
				EADocument.removeSpuriousEA75EntitiesFromStrings(
						constraintText);
	}
}
