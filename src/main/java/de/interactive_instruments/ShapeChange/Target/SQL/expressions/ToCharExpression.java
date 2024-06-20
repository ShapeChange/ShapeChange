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

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
public class ToCharExpression implements Expression {

	/**
	 * Oracle-specific property for setting National Language Support parameters.
	 */
	private String nlsParameters;
	/**
	 * Expression, e.g. a date, a date time, a number, ...
	 */
	private Expression expression;
	private String format;
	
	public ToCharExpression(Expression expression, String format) {
		this(expression, format, null);
	}

	public ToCharExpression(Expression expression, String format,
			String nlsParameters) {
		super();
		this.nlsParameters = nlsParameters;
		this.expression = expression;
		this.format = format;
	}

	/**
	 * @return the NLS parameters
	 */
	public String getNlsParameters() {
		return nlsParameters;
	}

	/**
	 * @param nlsParameters
	 *            the NLS parameters to set
	 */
	public void setNlsParameters(String nlsParameters) {
		this.nlsParameters = nlsParameters;
	}

	/**
	 * @return the expression
	 */
	public Expression getExpression() {
		return expression;
	}

	/**
	 * @param expression
	 *            the expression to set
	 */
	public void setExpression(Expression expression) {
		this.expression = expression;
	}

	/**
	 * @return the format
	 */
	public String getFormat() {
		return format;
	}

	/**
	 * @param format
	 *            the format to set
	 */
	public void setFormat(String format) {
		this.format = format;
	}

	@Override
	public String toString() {

		StringBuffer sb = new StringBuffer();

		sb.append("TO_CHAR(");
		sb.append(expression.toString());

		if (format != null) {
			sb.append(", '");
			sb.append(format);
			sb.append("'");
		}

		if (nlsParameters != null) {
			sb.append(", '");
			sb.append(nlsParameters);
			sb.append("'");
		}

		sb.append(")");

		return sb.toString();
	}

	@Override
	public void accept(ExpressionVisitor visitor) {
		visitor.visit(this);
	}
}
