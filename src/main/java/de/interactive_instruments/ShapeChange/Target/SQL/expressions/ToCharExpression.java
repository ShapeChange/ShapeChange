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
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange.Target.SQL.expressions;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class ToCharExpression implements Expression {

	private String dateLanguage;
	private Expression date;
	private String format;

	/**
	 * @param date
	 * @param format
	 * @param dateLanguage
	 */
	public ToCharExpression(Expression date, String format,
			String dateLanguage) {
		super();
		this.dateLanguage = dateLanguage;
		this.date = date;
		this.format = format;
	}

	/**
	 * @return the dateLanguage
	 */
	public String getDateLanguage() {
		return dateLanguage;
	}

	/**
	 * @param dateLanguage
	 *            the dateLanguage to set
	 */
	public void setDateLanguage(String dateLanguage) {
		this.dateLanguage = dateLanguage;
	}

	/**
	 * @return the date
	 */
	public Expression getDate() {
		return date;
	}

	/**
	 * @param date
	 *            the date to set
	 */
	public void setDate(Expression date) {
		this.date = date;
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
		sb.append(date.toString());

		if (format != null) {
			sb.append(", '");
			sb.append(format);
			sb.append("'");
		}

		if (dateLanguage != null) {
			sb.append(", 'NLS_DATE_LANGUAGE = ");
			sb.append(dateLanguage);
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
