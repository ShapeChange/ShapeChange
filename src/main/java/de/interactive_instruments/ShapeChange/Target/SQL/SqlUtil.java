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
package de.interactive_instruments.ShapeChange.Target.SQL;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import de.interactive_instruments.ShapeChange.Target.SQL.expressions.Expression;
import de.interactive_instruments.ShapeChange.Target.SQL.expressions.ExpressionList;
import de.interactive_instruments.ShapeChange.Target.SQL.expressions.StringValueExpression;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Column;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Table;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class SqlUtil {

	public static List<StringValueExpression> toStringValueList(
			String... strings) {

		List<StringValueExpression> result = new ArrayList<StringValueExpression>();

		for (String string : strings) {
			result.add(new StringValueExpression(string));
		}

		return result;
	}

	public static List<Column> toColumnList(Table inTable,
			String... columnNames) {

		List<Column> result = new ArrayList<Column>();

		for (String name : columnNames) {
			result.add(new Column(name, null, inTable));
		}

		return result;
	}

	public static ExpressionList toExpressionList(
			List<? extends Expression> list) {

		ExpressionList result = new ExpressionList();
		List<Expression> tmp = new ArrayList<Expression>();
		result.setExpressions(tmp);

		for (Expression exp : list) {
			tmp.add(exp);
		}

		return result;
	}

	/**
	 * Prints the list, using the toString() method of its members.
	 *
	 * @param list
	 *            list of objects to print
	 * @param useComma
	 *            true if the printed members shall be separated by a comma, in
	 *            addition to a space character
	 * @param useBrackets
	 *            true if the list shall be enclosed in brackets
	 * @return String representation of the list
	 */
	public static String getStringList(List<?> list, boolean useComma,
			boolean useBrackets) {

		StringBuilder sb = new StringBuilder();

		if (list != null && !list.isEmpty()) {

			String separator = ", ";
			if (!useComma) {
				separator = " ";
			}

			if (useBrackets) {
				sb.append("(");
			}

			sb.append(StringUtils.join(list, separator));

			if (useBrackets) {
				sb.append(")");
			}
		}

		return sb.toString();
	}
}
