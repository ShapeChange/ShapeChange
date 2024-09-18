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
 * (c) 2002-2017 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core.target.sql.expressions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
public class SdoDimArrayExpression implements Expression {

	private List<SdoDimElement> elements = new ArrayList<SdoDimElement>();

	/**
	 * @return the elements
	 */
	public List<SdoDimElement> getElements() {
		return elements;
	}

	public boolean hasElements() {
		return elements != null && !elements.isEmpty();
	}

	/**
	 * @param elements
	 *            the elements to set
	 */
	public void setElements(List<SdoDimElement> elements) {
		this.elements = elements;
	}

	public void addElement(SdoDimElement sde) {
		this.elements.add(sde);
	}

	@Override
	public void accept(ExpressionVisitor expressionVisitor) {
		expressionVisitor.visit(this);
	}

	@Override
	public String toString() {

		StringBuffer sb = new StringBuffer();

		sb.append("MDSYS.SDO_DIM_ARRAY(");
		if (hasElements()) {

			for (Iterator<SdoDimElement> iter = getElements().iterator(); iter.hasNext();) {

				SdoDimElement sde = iter.next();
				sb.append(sde.toString());

				if (iter.hasNext()) {
					sb.append(", ");
				}
			}
		} else {
			sb.append("FIXME");
		}
		sb.append(")");

		return sb.toString();
	}

}
