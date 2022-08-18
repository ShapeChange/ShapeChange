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
import java.util.Map;
import java.util.SortedSet;
import java.util.Map.Entry;

import de.interactive_instruments.ShapeChange.MapEntryParamInfos;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Target.SQL.expressions.Expression;
import de.interactive_instruments.ShapeChange.Target.SQL.expressions.SpatiaLiteCreateSpatialIndexExpression;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Column;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.ColumnDataType;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.ForeignKeyConstraint.ReferentialAction;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Select;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Statement;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Table;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot
 *         de)
 *
 */
public class SQLiteStrategy implements DatabaseStrategy, MessageSource {

	/**
	 * Regular expression (?i:(XY|XYM|XYZ|XYZM|2|3|4)) to check that a given
	 * string is one of a list of allowed values (NOTE: check is
	 * case-insensitive).
	 */
	public static final String GEOMETRY_DIMENSION_VALIDATION_REGEX = "(?i:(XY|XYM|XYZ|XYZM|2|3|4))";

	private ShapeChangeResult result;

	public SQLiteStrategy(ShapeChangeResult result) {
		this.result = result;
	}

	@Override
	public ColumnDataType primaryKeyDataType() {
		return new ColumnDataType("INTEGER");
	}

	@Override
	public String geometryDataType(ProcessMapEntry me, int srid) {
		return me.getTargetType();
	}

	@Override
	public ColumnDataType unlimitedLengthCharacterDataType() {
		return new ColumnDataType("TEXT");
	}

	@Override
	public ColumnDataType limitedLengthCharacterDataType(int size,
			String lengthQualifier) {
		// SQLite has no length restrictions
		return new ColumnDataType("TEXT");
	}

	@Override
	public Statement geometryIndexColumnPart(String indexName, Table table,
			Column column, Map<String, String> geometryCharacteristics) {

		Select select = new Select();

		SpatiaLiteCreateSpatialIndexExpression createSpatialIndexExpr = new SpatiaLiteCreateSpatialIndexExpression(
				table, column);

		select.setExpression(createSpatialIndexExpr);

		return select;
	}

	@Override
	public Statement geometryMetadataUpdateStatement(Table tableWithColumn,
			Column columForGeometryTypedProperty, int srid) {

		// in SpatiaLite, CreateSpatialIndex takes care of everything
		return null;
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

					if (geometryCharacteristics.containsKey(
							SqlConstants.ME_PARAM_GEOMETRY_CHARACT_DIMENSION)) {

						String dimension = geometryCharacteristics.get(
								SqlConstants.ME_PARAM_GEOMETRY_CHARACT_DIMENSION);

						if (dimension == null) {

							result.addError(this, 3, typeRuleKey,
									SqlConstants.ME_PARAM_GEOMETRY_CHARACT_DIMENSION,
									SqlConstants.ME_PARAM_GEOMETRY);
							isValid = false;

						} else if (!dimension
								.matches(GEOMETRY_DIMENSION_VALIDATION_REGEX)) {

							result.addError(this, 4, typeRuleKey,
									SqlConstants.ME_PARAM_GEOMETRY_CHARACT_DIMENSION,
									SqlConstants.ME_PARAM_GEOMETRY,
									GEOMETRY_DIMENSION_VALIDATION_REGEX);
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

	/**
	 * TBD - not implemented yet
	 */
	@Override
	public Expression expressionForCheckConstraintToRestrictTimeOfDate(
			Column column) {
		return null;
	}

	@Override
	public boolean isForeignKeyOnDeleteSupported(ReferentialAction o) {

		// all options are supported:
		// https://www.sqlite.org/syntax/foreign-key-clause.html
		return true;
	}

	@Override
	public boolean isForeignKeyOnUpdateSupported(ReferentialAction o) {

		// all options are supported:
		// https://www.sqlite.org/syntax/foreign-key-clause.html
		return true;
	}
	
	@Override
	public List<Statement> schemaInitializationStatements(SortedSet<String> schemaNames) {
	    // database schema creation currently not supported for SQLite db strategy
	    return new ArrayList<>();
	}

	@Override
	public String name() {
		return "SQLite";
	}
	
	@Override
	public boolean isForeignKeyCheckingOptionsSupported() {
	    // https://www.sqlite.org/foreignkeys.html#fk_deferred
	    return true;
	}

	@Override
	public String message(int mnr) {
		switch (mnr) {
		case 0:
			return "Context: class SQLiteStrategy";
		case 3:
			return "Invalid map entry for type#rule '$1$': no value is provided for the characteristic '$2$' of parameter '$3$'.";
		case 4:
			return "Invalid map entry for type#rule '$1$': value provided for characteristic '$2$' of parameter '$3$' is invalid. Check that the value matches the regular expression: $4$.";
		default:
			return "(" + SQLiteStrategy.class.getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
