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
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange.Target.SQL;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.SQL.expressions.SpatiaLiteAddGeometryColumn;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Alter;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.AlterExpression;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Column;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.ConstraintAlterExpression;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.CreateTable;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Select;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.SqlConstraint;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Statement;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Table;

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

		SortedMap<String, List<SqlConstraint>> constraintsFromAlterStatementsByTableName = new TreeMap<>();
		SortedMap<String, Table> tableByName = new TreeMap<>();

		for (Statement stmt : stmtsIn) {

			if (stmt instanceof Alter) {

				Alter alter = (Alter) stmt;

				AlterExpression ae = alter.getExpression();

				if (ae.getOperation().equals(AlterExpression.AlterOperation.ADD)
						&& ae instanceof ConstraintAlterExpression) {

					String tableName = alter.getTable().getName();

					List<SqlConstraint> cons;
					if (!constraintsFromAlterStatementsByTableName
							.containsKey(tableName)) {
						cons = new ArrayList<>();
						constraintsFromAlterStatementsByTableName.put(tableName,
								cons);
					} else {
						cons = constraintsFromAlterStatementsByTableName
								.get(tableName);
					}

					cons.add(((ConstraintAlterExpression) ae).getConstraint());

				} else {

					result.add(stmt);
				}

			} else if (stmt instanceof CreateTable) {

				result.add(stmt);

				CreateTable ct = (CreateTable) stmt;

				Table table = ct.getTable();
				
				tableByName.put(table.getName(), table);

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
		
		for(Entry<String,List<SqlConstraint>> e : constraintsFromAlterStatementsByTableName.entrySet()) {
			
			String tableName = e.getKey();
			List<SqlConstraint> cons = e.getValue();
			
			Table table = tableByName.get(tableName);
			
			table.addConstraints(cons);
		}

		return result;
	}

}
