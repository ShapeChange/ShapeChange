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
import java.util.SortedSet;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.ShapeChange.MapEntryParamInfos;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.Target.SQL.expressions.Expression;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Column;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.ColumnDataType;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.CreateIndex;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.CreateSchema;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.DropSchema;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.ForeignKeyConstraint;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Index;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.PostgreSQLAlterRole;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Statement;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Table;

public class PostgreSQLStrategy implements DatabaseStrategy {

    @Override
    public ColumnDataType primaryKeyDataType() {
	return new ColumnDataType("bigserial");
    }

    @Override
    public String geometryDataType(ProcessMapEntry me, int srid) {
	return "geometry(" + me.getTargetType() + "," + srid + ")";
    }

    @Override
    public ColumnDataType unlimitedLengthCharacterDataType() {
	return new ColumnDataType("text");
    }

    @Override
    public ColumnDataType limitedLengthCharacterDataType(int size, String lengthQualifier) {
	return new ColumnDataType("character varying", null, null, size, null);
    }

    @Override
    public Statement geometryIndexColumnPart(String indexName, Table table, Column column,
	    Map<String, String> geometryCharacteristics) {

	Index index = new Index(indexName);
	index.addColumn(column);

	index.getProperties().setProperty(PostgreSQLConstants.PROPERTY_METHOD, "GIST");

	CreateIndex cIndex = new CreateIndex();
	cIndex.setIndex(index);
	cIndex.setTable(table);

	return cIndex;
    }

    @Override
    public Statement geometryMetadataUpdateStatement(Table tableWithColumn, Column columForGeometryTypedProperty,
	    int srid) {
	// in PostGIS 2.0, geometry_column is a view
	return null;
    }

    @Override
    public boolean validate(Map<String, ProcessMapEntry> mapEntryByType, MapEntryParamInfos mepp) {
	// nothing specific to check
	return true;
    }

    /**
     * TBD - not implemented yet
     * 
     */
    @Override
    public Expression expressionForCheckConstraintToRestrictTimeOfDate(Column columnForPi) {
	return null;
    }

    @Override
    public boolean isForeignKeyOnDeleteOptionSupported(ForeignKeyConstraint.Option o) {
	// all options are supported
	// https://www.postgresql.org/docs/10/static/sql-createtable.html
	return true;
    }

    @Override
    public boolean isForeignKeyOnUpdateOptionSupported(ForeignKeyConstraint.Option o) {
	// all options are supported
	// https://www.postgresql.org/docs/10/static/sql-createtable.html
	return true;
    }

    @Override
    public List<Statement> schemaInitializationStatements(SortedSet<String> schemaNames) {

	List<Statement> stmts = new ArrayList<>();

	if (!schemaNames.isEmpty()) {

	    DropSchema dp = new DropSchema(schemaNames);
	    dp.setIfExists(true);
	    dp.addSpec("CASCADE");
	    stmts.add(dp);

	    for (String name : schemaNames) {

		CreateSchema cs = new CreateSchema(name);
		cs.setIfNotExists(false);
		stmts.add(cs);
	    }

	    PostgreSQLAlterRole ar = new PostgreSQLAlterRole();
	    ar.setRoleSpecificationOrALL(SqlDdl.postgreSqlRole);
	    List<String> searchPathValues = new ArrayList<>(schemaNames);
	    searchPathValues.add("public");
	    ar.setSpec("SET search_path TO " + StringUtils.join(searchPathValues, ","));
	    stmts.add(ar);
	}

	return stmts;
    }

    @Override
    public String name() {
	return "PostgreSQL";
    }

}
