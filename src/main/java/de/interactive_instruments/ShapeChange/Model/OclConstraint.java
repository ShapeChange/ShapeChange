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
 * The interface OclConstraint handles constraints specified in Object
 * Constraint Language (OCL). OCL expressions are represented by a syntax tree
 * similar to the one implied by the OCL specification, however, only part of
 * the language is supported.
 */
public interface OclConstraint extends Constraint {

	/** Type for possible OCL expressions */
	enum ConditionType {
		INVARIANT, // An OCL-constraint of type 'inv:'
		INITIAL, // An OCL-expression of type 'init:'
		DERIVE // An OCL-expression of type 'derive:'
	}

	/** Inquire context class - i.e. 'self' */
	public ClassInfo contextClass();

	/** Inquire condition type */
	public ConditionType conditionType();

	/** Inquire constraint syntax tree */
	public OclNode.Expression syntaxTree();

	/**
	 * @return comments contained in the OCL expression; may be empty but not
	 *         <code>null</code>
	 */
	public String[] comments();
}
