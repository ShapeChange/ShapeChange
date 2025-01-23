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
 * (c) 2002-2019 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core.target.sql;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import de.interactive_instruments.shapechange.core.target.sql.expressions.SpatiaLiteAddGeometryColumn;
import de.interactive_instruments.shapechange.core.target.sql.structure.Alter;
import de.interactive_instruments.shapechange.core.target.sql.structure.AlterExpression;
import de.interactive_instruments.shapechange.core.target.sql.structure.Column;
import de.interactive_instruments.shapechange.core.target.sql.structure.ConstraintAlterExpression;
import de.interactive_instruments.shapechange.core.target.sql.structure.CreateTable;
import de.interactive_instruments.shapechange.core.target.sql.structure.Select;
import de.interactive_instruments.shapechange.core.target.sql.structure.SqlConstraint;
import de.interactive_instruments.shapechange.core.target.sql.structure.Statement;
import de.interactive_instruments.shapechange.core.target.sql.structure.Table;
import de.interactive_instruments.shapechange.core.model.PropertyInfo;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot
 *         de)
 *
 */
public class SQLiteDdlFixer {

	/**
	 * Ensures that all ALTER statements that create constraints are transformed
	 * into constraints that are directly defined on tables. In addition,
	 * geometry typed columns are transformed into SELECT AddGeometryColumn(...)
	 * statements.
	 * 
	 * @param stmtsIn tbd
	 * @return tbd
	 */
	public static List<Statement> fixDdl(List<Statement> stmtsIn) {

		List<Statement> result = new ArrayList<>();

		SortedMap<String, List<SqlConstraint>> constraintsFromAlterStatementsByFullTableName = new TreeMap<>();
		SortedMap<String, Table> tableByFullName = new TreeMap<>();

		for (Statement stmt : stmtsIn) {

			if (stmt instanceof Alter alter) {

				AlterExpression ae = alter.getExpression();

				if (ae.getOperation().equals(AlterExpression.AlterOperation.ADD)
						&& ae instanceof ConstraintAlterExpression expression) {

					String fullTableName = alter.getTable().getFullName();

					List<SqlConstraint> cons;
					if (!constraintsFromAlterStatementsByFullTableName
							.containsKey(fullTableName)) {
						cons = new ArrayList<>();
						constraintsFromAlterStatementsByFullTableName.put(fullTableName,
								cons);
					} else {
						cons = constraintsFromAlterStatementsByFullTableName
								.get(fullTableName);
					}

					cons.add(expression.getConstraint());

				} else {

					result.add(stmt);
				}

			} else if (stmt instanceof CreateTable ct) {

				result.add(stmt);

				Table table = ct.getTable();
				
				tableByFullName.put(table.getFullName(), table);

				List<Column> columnsToRemoveFromTable = new ArrayList<>();

				for (Column col : table.getColumns()) {

					PropertyInfo pi = col.getRepresentedProperty();

					if (pi != null) {

						if (SqlUtil.isGeometryTypedProperty(pi)) {

							columnsToRemoveFromTable.add(col);

							String valueTypeName = pi.typeInfo().name;
							String piEncodingRule = pi.encodingRule("sql");

							String geometryDimension = SqlDdl.mapEntryParamInfos
									.getCharacteristic(valueTypeName,
											piEncodingRule,
											SqlConstants.ME_PARAM_GEOMETRY,
											SqlConstants.ME_PARAM_GEOMETRY_CHARACT_DIMENSION);
							if (geometryDimension == null) {
								geometryDimension = SqlDdl.geometryDimension;
							}

							Select select = new Select();
							SpatiaLiteAddGeometryColumn agc = new SpatiaLiteAddGeometryColumn(
									table, col, SqlDdl.srid, geometryDimension);
							select.setExpression(agc);

							result.add(select);
						}
					}
				}

				table.removeColumns(columnsToRemoveFromTable);
				
			} else {
				
				result.add(stmt);
			}
		}
		
		for(Entry<String,List<SqlConstraint>> e : constraintsFromAlterStatementsByFullTableName.entrySet()) {
			
			String fullTableName = e.getKey();
			List<SqlConstraint> cons = e.getValue();
			
			Table table = tableByFullName.get(fullTableName);
			
			table.addConstraints(cons);
		}

		return result;
	}

}
