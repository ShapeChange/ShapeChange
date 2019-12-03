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
		/**
		 * An OCL-constraint of type 'inv:'
		 */
		INVARIANT,
		/**
		 * An OCL-expression of type 'init:'
		 */
		INITIAL,
		/**
		 * An OCL-expression of type 'derive:'
		 */
		DERIVE
	}

	/** Inquire context class - i.e. 'self' */
	public ClassInfo contextClass();

	/** Inquire condition type */
	public ConditionType conditionType();

	/**
	 * Inquire constraint syntax tree
	 * 
	 * @return the constraint syntax tree; can be <code>null</code>, in case
	 *         that compilation failed
	 */
	public OclNode.Expression syntaxTree();

	/**
	 * Retrieve informative documentation for the OCL expression.
	 * 
	 * Background: OCL constraint expressions can be hard to understand for
	 * humans. Therefore, informative, human-readable documentation of the
	 * constraint is useful. ShapeChange can parse such information from the
	 * text of an OCL constraint, by looking for java-like comments, i.e. text
	 * surrounded by /* and *&#47;. However, in some cases it is also possible
	 * to provide the information by some other means, for example via
	 * &lt;description&gt; elements within an SCXML &lt;OclConstraint&gt;
	 * element.
	 * 
	 * @return informative documentation of the constraint (if originally
	 *         contained in java-like comment, i.e. surrounded by /* and *&#47;,
	 *         that markup has been removed); may be empty but not
	 *         <code>null</code>
	 */
	public String[] comments();

	public boolean hasComments();

	public void setComments(String[] comments);

	public void mergeComments(String[] additionalComments);
}
