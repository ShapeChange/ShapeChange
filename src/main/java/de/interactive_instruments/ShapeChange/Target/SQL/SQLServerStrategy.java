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
 * (c) 2002-2016 interactive instruments GmbH, Bonn, Germany
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

import java.util.Map;

import de.interactive_instruments.ShapeChange.MapEntryParamInfos;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.SQL.expressions.Expression;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Column;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.ColumnDataType;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.CreateIndex;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.ForeignKeyConstraint;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Index;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Statement;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Table;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class SQLServerStrategy implements DatabaseStrategy, MessageSource {

	public static final String IDX_PARAM_USING = "USING";
	public static final String IDX_PARAM_BOUNDING_BOX = "BOUNDING_BOX";

	private ShapeChangeResult result;

	public SQLServerStrategy(ShapeChangeResult result) {
		this.result = result;
	}

	@Override
	public ColumnDataType primaryKeyDataType() {
		return new ColumnDataType("bigint");
	}

	@Override
	public String geometryDataType(ProcessMapEntry me, int srid) {
		// srid is ignored here
		return me.getTargetType();
	}

	@Override
	public ColumnDataType unlimitedLengthCharacterDataType() {
		return new ColumnDataType("nvarchar(max)", null, null, null);
	}

	@Override
	public ColumnDataType limitedLengthCharacterDataType(int size) {

		/*
		 * Apparently there is a restriction how long a limited nvarchar can be
		 * (4000). Source:
		 * https://msdn.microsoft.com/de-de/library/ms186939.aspx
		 */

		if (size > 4000) {
			// TODO: log warning?
			return new ColumnDataType("nvarchar(max)", null, null, null);
		} else {
			return new ColumnDataType("nvarchar", null, null, size);
		}
	}

	@Override
	public Statement geometryIndexColumnPart(String indexName, Table table,
			Column column, Map<String, String> geometryCharacteristics) {

		Index index = new Index(indexName);
		index.setType("SPATIAL");

		index.addColumn(column);

		// TBD: declaration of tesselation
		if (geometryCharacteristics.containsKey(IDX_PARAM_USING)) {
			index.addSpec(
					"USING " + geometryCharacteristics.get(IDX_PARAM_USING));
		}

		if (geometryCharacteristics.containsKey(IDX_PARAM_BOUNDING_BOX)) {
			index.addSpec("WITH (BOUNDING_BOX = "
					+ geometryCharacteristics.get(IDX_PARAM_BOUNDING_BOX)
					+ ")");
		}

		CreateIndex cIndex = new CreateIndex();
		cIndex.setIndex(index);
		cIndex.setTable(table);

		return cIndex;
	}

	@Override
	public Statement geometryMetadataUpdateStatement(Table tableWithColumn,
			Column columForGeometryTypedProperty, int srid) {

		// TBD: should we constrain the SRID as follows?
		// ALTER TABLE xyz ADD CONSTRAINT cname CHECK (column.STSrid = srid)
		return null;
	}

	@Override
	public boolean validate(Map<String, ProcessMapEntry> mapEntryByType,
			MapEntryParamInfos mepp) {

		// TODO implement specific checks

		// BOUNDING_BOX must contain four numbers, etc.
		return true;
	}

	/**
	 * TBD - not implemented yet
	 * 
	 * @see de.interactive_instruments.ShapeChange.Target.SQL.DatabaseStrategy#expressionForCheckConstraintToRestrictTimeOfDate(de.interactive_instruments.ShapeChange.Model.PropertyInfo,
	 *      de.interactive_instruments.ShapeChange.Target.SQL.structure.Column)
	 */
	@Override
	public Expression expressionForCheckConstraintToRestrictTimeOfDate(
			PropertyInfo pi, Column columnForPi) {
		return null;
	}
	
	@Override
	public boolean isForeignKeyOnDeleteOptionSupported(
			ForeignKeyConstraint.Option o) {

		// https://msdn.microsoft.com/en-us/library/ms188066(v=sql.110).aspx
		if (o == ForeignKeyConstraint.Option.CASCADE
				|| o == ForeignKeyConstraint.Option.SET_NULL
				|| o == ForeignKeyConstraint.Option.NO_ACTION
				|| o == ForeignKeyConstraint.Option.SET_DEFAULT) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean isForeignKeyOnUpdateOptionSupported(
			ForeignKeyConstraint.Option o) {

		// https://msdn.microsoft.com/en-us/library/ms188066(v=sql.110).aspx
		if (o == ForeignKeyConstraint.Option.CASCADE
				|| o == ForeignKeyConstraint.Option.SET_NULL
				|| o == ForeignKeyConstraint.Option.NO_ACTION
				|| o == ForeignKeyConstraint.Option.SET_DEFAULT) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public String name() {
		return "SQLServer";
	}

	@Override
	public String message(int mnr) {
		switch (mnr) {
		case 0:
			return "Context: class SQLServerStrategy";
		default:
			return "(" + SQLServerStrategy.class.getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
