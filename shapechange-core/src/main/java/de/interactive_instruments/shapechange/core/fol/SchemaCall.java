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

/**
 * @author Johannes Echterhoff
 */
public abstract class SchemaCall extends Expression {

	/**
	 * Could be useful for debugging.
	 */
	private String nameInSbvr;

	private Variable variableContext;

	private SchemaCall nextElement;

	public SchemaCall() {

	}

	/**
	 * @return the nameInSbvr
	 */
	public String getNameInSbvr() {
		return nameInSbvr;
	}

	/**
	 * @param nameInSbvr
	 *            the nameInSbvr to set
	 */
	public void setNameInSbvr(String nameInSbvr) {
		this.nameInSbvr = nameInSbvr;
	}

	/**
	 * @return the variableContext
	 */
	public Variable getVariableContext() {
		return variableContext;
	}

	/**
	 * Sets the variable context.
	 * 
	 * @param variableContext
	 *            the variableContext to set
	 */
	public void setVariableContext(Variable variableContext) {

		this.variableContext = variableContext;
	}

	/**
	 * @return the nextElement
	 */
	public SchemaCall getNextElement() {
		return nextElement;
	}

	/**
	 * Sets the nextElement.
	 * 
	 * @param nextElement
	 *            the nextElement to set
	 */
	public void setNextElement(SchemaCall nextElement) {

		this.nextElement = nextElement;
	}

	/**
	 * Returns a string that starts with the nameInSbvr of this SchemaCall,
	 * followed by the toString() representation of the nextElement if it exists
	 * (separated by a '.'). The variableContext is not represented.
	 */
	@Override
	public String toString() {

		return (variableContext != null ? variableContext.getName() + "." : "")
				+ nameInSbvr
				+ (nextElement != null ? "." + nextElement.toString() : "");
	}

	public boolean hasVariableContext() {
		return this.variableContext != null;
	}

	public boolean hasNextElement() {
		return this.nextElement != null;
	}

	public SchemaCall getLastElement() {
		if (this.nextElement == null) {
			return this;
		} else {
			return this.nextElement.getLastElement();
		}
	}

	public String toStringWithoutVariablePrefix() {
		return nameInSbvr
				+ (nextElement != null ? "." + nextElement.toString() : "");
	}	
}