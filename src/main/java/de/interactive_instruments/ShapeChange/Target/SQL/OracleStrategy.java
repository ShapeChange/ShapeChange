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
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange.Target.SQL;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.interactive_instruments.ShapeChange.MapEntryParamInfos;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.SQL.expressions.ColumnExpression;
import de.interactive_instruments.ShapeChange.Target.SQL.expressions.EqualsExpression;
import de.interactive_instruments.ShapeChange.Target.SQL.expressions.Expression;
import de.interactive_instruments.ShapeChange.Target.SQL.expressions.LongValueExpression;
import de.interactive_instruments.ShapeChange.Target.SQL.expressions.SdoDimArrayExpression;
import de.interactive_instruments.ShapeChange.Target.SQL.expressions.StringValueExpression;
import de.interactive_instruments.ShapeChange.Target.SQL.expressions.ToCharExpression;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Column;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.CreateIndex;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Index;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Insert;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Statement;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Table;

public class OracleStrategy implements DatabaseStrategy, MessageSource {

	public static final String GEOM_PARAM_LAYER_GTYPE = "layer_gtype";
	/**
	 * Regular expression
	 * (?i:(POINT|LINE|POLYGON|COLLECTION|MULTIPOINT|MULTILINE|MULTIPOLYGON)) to
	 * check that a given string is one of a list of allowed values (NOTE: check
	 * is case-insensitive).
	 */
	public static final String GEOM_PARAM_LAYER_GTYPE_VALIDATION_REGEX = "(?i:(POINT|LINE|POLYGON|COLLECTION|MULTIPOINT|MULTILINE|MULTIPOLYGON))";

	private ShapeChangeResult result;
	private Table userSdoGeomMetadataTable = new Table(
			"USER_SDO_GEOM_METADATA");

	public OracleStrategy(ShapeChangeResult result) {
		this.result = result;
	}

	@Override
	public String primaryKeyDataType() {
		return "INTEGER";
	}

	@Override
	public String geometryDataType(ProcessMapEntry me, int srid) {
		return me.getTargetType();
	}

	@Override
	public String unlimitedLengthCharacterDataType() {
		return "CLOB";
	}

	@Override
	public String limitedLengthCharacterDataType(int size) {
		return "VARCHAR2(" + size + ")";
	}

	@Override
	public Statement geometryIndexColumnPart(String indexName, Table table,
			Column column, Map<String, String> geometryCharacteristics) {

		Index index = new Index(indexName);
		index.addColumn(column);
		index.addSpec("INDEXTYPE IS MDSYS.SPATIAL_INDEX");

		if (geometryCharacteristics != null && geometryCharacteristics
				.containsKey(GEOM_PARAM_LAYER_GTYPE)) {

			String layergtype = geometryCharacteristics
					.get(GEOM_PARAM_LAYER_GTYPE);

			if (layergtype != null) {
				index.addSpec("PARAMETERS('layer_gtype="
						+ geometryCharacteristics.get(GEOM_PARAM_LAYER_GTYPE)
						+ "')");
			} else {

				/*
				 * Missing characteristic value should have been reported during
				 * validation of the map entry parameter infos.
				 */
			}
		}

		CreateIndex cIndex = new CreateIndex();
		cIndex.setIndex(index);
		cIndex.setTable(table);

		return cIndex;
	}

	@Override
	public Statement geometryMetadataUpdateStatement(Table tableWithColumn,
			Column columnForGeometryTypedProperty, int srid) {

		Insert ins = new Insert();

		ins.setTable(userSdoGeomMetadataTable);

		ins.setColumns(SqlUtil.toColumnList(userSdoGeomMetadataTable,
				"TABLE_NAME", "COLUMN_NAME", "DIMINFO", "SRID"));

		List<Expression> items = new ArrayList<Expression>();
		items.addAll(SqlUtil.toStringValueList(tableWithColumn.getName(),
				columnForGeometryTypedProperty.getName()));

		SdoDimArrayExpression dimArray = SqlDdl.sdoDimArrayExpression;

		items.add(dimArray);
		items.add(new LongValueExpression(srid));

		ins.setExpressionList(SqlUtil.toExpressionList(items));

		return ins;
	}

	@Override
	public boolean validate(Map<String, ProcessMapEntry> mapEntryByType,
			MapEntryParamInfos mepp) {

		boolean isValid = true;

		if (mepp != null) {

			// browse through the parameters and their characteristics that are
			// stored for each map entry
			for (Entry<String, Map<String, Map<String, String>>> entry : mepp
					.getParameterCache().entrySet()) {

				String typeRuleKey = entry.getKey();
				Map<String, Map<String, String>> characteristicsByParameter = entry
						.getValue();

				if (characteristicsByParameter
						.containsKey(SqlConstants.ME_PARAM_GEOMETRY)) {

					Map<String, String> geometryCharacteristics = characteristicsByParameter
							.get(SqlConstants.ME_PARAM_GEOMETRY);

					// ensure that layer_gtype has a value and that it is one of
					// the
					// allowed ones
					if (geometryCharacteristics
							.containsKey(GEOM_PARAM_LAYER_GTYPE)) {

						String layergtype = geometryCharacteristics
								.get(GEOM_PARAM_LAYER_GTYPE);

						if (layergtype == null) {

							result.addError(this, 3, typeRuleKey,
									GEOM_PARAM_LAYER_GTYPE,
									SqlConstants.ME_PARAM_GEOMETRY);
							isValid = false;

						} else if (!layergtype.matches(
								GEOM_PARAM_LAYER_GTYPE_VALIDATION_REGEX)) {

							result.addError(this, 4, typeRuleKey,
									GEOM_PARAM_LAYER_GTYPE,
									SqlConstants.ME_PARAM_GEOMETRY,
									GEOM_PARAM_LAYER_GTYPE_VALIDATION_REGEX);
							isValid = false;

						} else {
							// fine - no further tests at this point in time
						}
					}
				}
			}
		}

		return isValid;
	}

	@Override
	public String message(int mnr) {
		switch (mnr) {
		case 0:
			return "Context: class OracleStrategy";
		case 3:
			return "Invalid map entry for type#rule '$1$': no value is provided for the characteristic '$2$' of parameter '$3$'.";
		case 4:
			return "Invalid map entry for type#rule '$1$': value provided for characteristic '$2$' of parameter '$3$' is invalid. Check that the value matches the regular expression: $4$.";
		default:
			return "(" + OracleStrategy.class.getName()
					+ ") Unknown message with number: " + mnr;
		}
	}

	@Override
	public Expression expressionForCheckConstraintToRestrictTimeOfDate(
			PropertyInfo pi, Column columnForPi) {

		if (columnForPi.getDataType().getName().equalsIgnoreCase("DATE")) {

			ColumnExpression colexp = new ColumnExpression(columnForPi);
			ToCharExpression tcexp = new ToCharExpression(colexp, "HH24:MI:SS");
			StringValueExpression compareValue = new StringValueExpression(
					"00:00:00");
			EqualsExpression eexp = new EqualsExpression(tcexp, compareValue);

			return eexp;

		} else {
			return null;
		}
	}
}
