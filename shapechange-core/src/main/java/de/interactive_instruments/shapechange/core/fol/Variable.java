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
package de.interactive_instruments.shapechange.core.fol;

import de.interactive_instruments.shapechange.core.sbvr.SbvrUtil;

/**
 * @author Johannes Echterhoff
 */
public class Variable extends Expression {

	public static int index = 1;
	public static final String NAME_PREFIX = "x";
	public static final String SELF_VARIABLE_NAME = "self";

	private String name;
	/**
	 * Quantifications declare a variable. They can themselves contain
	 * quantifications (as part of the condition) and can thus be nested. The
	 * evaluation of variable declarations in nested quantifications depends on
	 * variables defined in the outer scope. The outmost scope naturally does
	 * not have an outer scope, which therefore is null.
	 */
	private Variable nextOuterScope;
	private SchemaCall value;

	/**
	 * Ctor for Variable with automatic assignment of name ({@link #NAME_PREFIX}
	 * + current {@link #index} [which is increased after the name has been
	 * constructed]).
	 */
	public Variable() {
		this.name = NAME_PREFIX + index;
		index++;
	}

	public Variable(String name) {
		this.name = name;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the nextOuterScope
	 */
	public Variable getNextOuterScope() {
		return nextOuterScope;
	}

	/**
	 * @param nextOuterScope
	 *            the nextOuterScope to set
	 */
	public void setNextOuterScope(Variable nextOuterScope) {
		this.nextOuterScope = nextOuterScope;
	}

	/**
	 * @return the value
	 */
	public SchemaCall getValue() {
		return value;
	}

	/**
	 * @param value
	 *            the value to set
	 */
	public void setValue(SchemaCall value) {
		this.value = value;
	}

	public SchemaCall getLastSegmentInValue() {

		if (value == null) {
			return null;

		} else {

			SchemaCall current = value;

			while (current.getNextElement() != null) {
				current = current.getNextElement();
			}

			return current;
		}
	}

	/**
	 * @return the last property call in this variable or in the closest next
	 *         outer scope that has a property call; <code>null</code> if no
	 *         such property call was found
	 */
	public PropertyCall lastPropertyCallInEffectiveValue() {

		if (value != null) {

			SchemaCall current = value;

			PropertyCall lastPC = current instanceof PropertyCall ? (PropertyCall) current
					: null;

			while (current.getNextElement() != null) {

				current = current.getNextElement();

				if (current instanceof PropertyCall) {
					lastPC = (PropertyCall) current;
				}
			}

			if (lastPC != null) {
				return lastPC;
			}
		}

		/*
		 * we did not find a PropertyCall in the value or the variable has no
		 * value, thus we try to look it up in the next outer scope
		 */

		if (nextOuterScope != null) {
			return nextOuterScope.lastPropertyCallInEffectiveValue();
		} else {
			return null;
		}
	}

	@Override
	public String toString() {

		return name;
	}

	public String toStringWithValue() {

		return name + ":" + (value == null ? "<null>" : value.toString());
	}

	public boolean isSelf() {
		return this.name.equals(SELF_VARIABLE_NAME);
	}

	/**
	 * Sets index to '1'.
	 */
	public static void reset() {

		index = 1;
	}

	/**
	 * Creates a copy of the given SchemaCall and sets it as the front of the
	 * path that is the value of this variable, updating references accordingly.
	 * 
	 * Does not validate the correctness of the given SchemaCall within the
	 * context provided by the possibly existing nextOuterScope and the current
	 * value.
	 * 
	 * Does nothing if the given SchemaCall is null.
	 * 
	 * @param sc tbd
	 */
	public void prependSchemaCallToValue(SchemaCall sc) {

		if (sc == null)
			return;

		SchemaCall scCopy = SbvrUtil.copy(sc);

		scCopy.setVariableContext(this);
		this.value.setVariableContext(null);
		scCopy.getLastElement().setNextElement(this.value);
		this.value = scCopy;
	}
}