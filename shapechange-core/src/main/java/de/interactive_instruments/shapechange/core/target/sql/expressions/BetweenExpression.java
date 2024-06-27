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
 * (c) 2002-2018 interactive instruments GmbH, Bonn, Germany
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

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
public class BetweenExpression implements Expression {

	private boolean not = false;
	private Expression testExpression = null;
	private Expression beginExpression = null;
	private Expression endExpression = null;

	/**
	 * @return the not
	 */
	public boolean isNot() {
		return not;
	}

	/**
	 * @param not
	 *            the not to set
	 */
	public void setNot(boolean not) {
		this.not = not;
	}

	/**
	 * @return the testExpression
	 */
	public Expression getTestExpression() {
		return testExpression;
	}

	/**
	 * @param testExpression
	 *            the testExpression to set
	 */
	public void setTestExpression(Expression testExpression) {
		this.testExpression = testExpression;
	}

	/**
	 * @return the beginExpression
	 */
	public Expression getBeginExpression() {
		return beginExpression;
	}

	/**
	 * @param beginExpression
	 *            the beginExpression to set
	 */
	public void setBeginExpression(Expression beginExpression) {
		this.beginExpression = beginExpression;
	}

	/**
	 * @return the endExpression
	 */
	public Expression getEndExpression() {
		return endExpression;
	}

	/**
	 * @param endExpression
	 *            the endExpression to set
	 */
	public void setEndExpression(Expression endExpression) {
		this.endExpression = endExpression;
	}

	@Override
	public void accept(ExpressionVisitor expressionVisitor) {
		expressionVisitor.visit(this);
	}

	@Override
	public String toString() {
		return testExpression + " " + (not ? "NOT " : "") + "BETWEEN "
				+ beginExpression + " AND " + endExpression;
	}
}
