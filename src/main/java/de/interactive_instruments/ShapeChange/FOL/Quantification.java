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
package de.interactive_instruments.ShapeChange.FOL;


/**
 * @author Johannes Echterhoff
 */
public class Quantification extends Predicate {

	private Quantifier quantifier;
	private Variable var;
	private Predicate condition;

	public Quantification() {

	}

	/**
	 * @return the quantifier
	 */
	public Quantifier getQuantifier() {
		return quantifier;
	}

	/**
	 * @param quantifier
	 *            the quantifier to set
	 */
	public void setQuantifier(Quantifier quantifier) {
		this.quantifier = quantifier;
	}

	/**
	 * @return the var
	 */
	public Variable getVar() {
		return var;
	}

	/**
	 * @param var
	 *            the var to set
	 */
	public void setVar(Variable var) {
		this.var = var;
	}

	/**
	 * @return the condition
	 */
	public Predicate getCondition() {
		return condition;
	}

	/**
	 * @param condition
	 *            the condition to set
	 */
	public void setCondition(Predicate condition) {
		this.condition = condition;
	}

	@Override
	public String toString() {

		String q = quantifier != null ? quantifier.toString()
				: "<quantifier is null>";
		String v = var != null ? var.toStringWithValue() : "<variable is null>";
		String c = condition != null ? condition.toString()
				: "<condition is null>";

		return q + " (" + v + " | " + c + ")";
	}

	public boolean hasCondition() {
		return this.condition != null;
	}
}