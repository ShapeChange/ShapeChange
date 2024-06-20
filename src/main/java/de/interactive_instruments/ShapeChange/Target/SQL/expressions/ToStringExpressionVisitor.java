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
package de.interactive_instruments.ShapeChange.Target.SQL.expressions;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Used to convert a (list of) expressions into a (comma-separated) string. The
 * resulting string can for example be used in comparisons, when sorting Insert
 * statements.
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
public class ToStringExpressionVisitor implements ExpressionVisitor {

	protected StringBuffer sb = new StringBuffer();
	private List<String> expressions = new ArrayList<String>();

	@Override
	public void visit(ExpressionList expressionList) {

		for (Expression expr : expressionList.getExpressions()) {
			expr.accept(this);
		}
	}

	public String getResult() {

		sb.append("(");

		sb.append(StringUtils.join(expressions, ","));

		sb.append(")");

		return sb.toString();
	}

	@Override
	public void visit(NullValueExpression nullValue) {
		this.expressions.add("NULL");
	}

	@Override
	public void visit(StringValueExpression stringValue) {
		this.expressions.add(stringValue.getQuotedValue());
	}

	@Override
	public void visit(OrExpression orExpression) {
		this.expressions.add(orExpression.toString());
	}

	@Override
	public void visit(InExpression inExpression) {
		this.expressions.add(inExpression.toString());
	}

	@Override
	public void visit(IsNullExpression isNullExpression) {
		this.expressions.add(isNullExpression.toString());
	}

	@Override
	public void visit(ColumnExpression columnExpression) {
		this.expressions.add(columnExpression.toString());
	}

	@Override
	public void visit(SdoDimArrayExpression sdoDimArrayExpression) {
		this.expressions.add(sdoDimArrayExpression.toString());
	}

	@Override
	public void visit(LongValueExpression longValue) {
		this.expressions.add(longValue.toString());
	}

	@Override
	public void visit(ToCharExpression toCharExpression) {
		this.expressions.add(toCharExpression.toString());
	}

	@Override
	public void visit(EqualsExpression equalsExpression) {
		this.expressions.add(equalsExpression.toString());
	}

	@Override
	public void visit(UnquotedStringExpression unquotedStringExpression) {
		this.expressions.add(unquotedStringExpression.toString());
	}

	@Override
	public void visit(BetweenExpression betweenExpression) {
		this.expressions.add(betweenExpression.toString());
	}

	@Override
	public void visit(DoubleValueExpression doubleValueExpression) {
		this.expressions.add(doubleValueExpression.toString());
	}

	@Override
	public void visit(
			SpatiaLiteCreateSpatialIndexExpression spatiaLiteCreateSpatialIndexExpression) {
		this.expressions.add(spatiaLiteCreateSpatialIndexExpression.toString());
	}

	@Override
	public void visit(SpatiaLiteAddGeometryColumn spatiaLiteAddGeometryColumn) {
		this.expressions.add(spatiaLiteAddGeometryColumn.toString());
	}
}
