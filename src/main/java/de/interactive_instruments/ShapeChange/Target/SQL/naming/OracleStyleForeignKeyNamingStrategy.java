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
package de.interactive_instruments.ShapeChange.Target.SQL.naming;

import java.util.HashMap;
import java.util.Map;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
public class OracleStyleForeignKeyNamingStrategy
		implements ForeignKeyNamingStrategy, MessageSource {

	private Map<String, Integer> countByForeignKeyOracleStyle = new HashMap<String, Integer>();
	private ShapeChangeResult result;

	public OracleStyleForeignKeyNamingStrategy(ShapeChangeResult result) {
		this.result = result;
	}

	@Override
	public String nameForForeignKeyConstraint(String tableName,
			String fieldName, String targetTableName) {

		String tableNameForFK = tableName.length() > 8
				? tableName.substring(0, 8) : tableName;

		String fieldNameForFK = fieldName.length() > 8
				? fieldName.substring(0, 8) : fieldName;

		String targetTableNameForFK = targetTableName.length() > 8
				? targetTableName.substring(0, 8) : targetTableName;

		String fk = "fk_" + tableNameForFK + "_" + targetTableNameForFK + "_"
				+ fieldNameForFK;

		String res;

		if (countByForeignKeyOracleStyle.containsKey(fk)) {

			Integer count = countByForeignKeyOracleStyle.get(fk);

			if (count > 9) {
				result.addWarning(this, 1, fk);
			}

			res = fk + count;
			countByForeignKeyOracleStyle.put(fk, Integer.valueOf(count + 1));

		} else {

			res = fk;
			countByForeignKeyOracleStyle.put(fk, Integer.valueOf(0));
		}

		return res;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.MessageSource#message(int)
	 */
	public String message(int mnr) {

		switch (mnr) {

		case 1:
			return "??More than eleven occurrences of foreign key '$1$'.";

		default:
			return "(" + OracleStyleForeignKeyNamingStrategy.class.getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
