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

import java.util.Map;

import de.interactive_instruments.ShapeChange.MapEntryParamInfos;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.SQL.expressions.Expression;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Column;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Statement;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Table;

public interface DatabaseStrategy {

	/**
	 *
	 * @return the database data type to be used as data type for primary key
	 *         columns
	 */
	public String primaryKeyDataType();

	/**
	 *
	 * @param me
	 *            map entry in configuration file
	 * @param srid
	 *            defined in configuration file
	 * @return (complex) database data type to be used as data type for the
	 *         geometry column
	 */
	public String geometryDataType(ProcessMapEntry me, int srid);

	public String unlimitedLengthCharacterDataType();

	public String limitedLengthCharacterDataType(int size);

	/**
	 * @param indexName
	 * @param table
	 * @param column
	 * @param geometryCharacteristics
	 *            additional geometry specific characteristics - can be
	 *            <code>null</code>
	 * @return
	 */
	public Statement geometryIndexColumnPart(String indexName, Table table,
			Column column, Map<String, String> geometryCharacteristics);

	/**
	 *
	 * @param tableWithColumn
	 * @param columForGeometryTypedProperty
	 * @param srid
	 * @return update statement; may be <code>null</code> if this operation is
	 *         not applicable to the actual database strategy
	 */
	public Statement geometryMetadataUpdateStatement(Table tableWithColumn,
			Column columForGeometryTypedProperty, int srid);

	/**
	 * Database specific validation of the parameters (including their
	 * characteristics) defined by the map entries declared for the SQL DDL
	 * target.
	 * 
	 * @param mapEntryByType
	 * @param mepp
	 * @return <code>true</code> if the parameters are valid, else
	 *         <code>false</code>
	 */
	boolean validate(Map<String, ProcessMapEntry> mapEntryByType,
			MapEntryParamInfos mepp);

	/**
	 * For a property with a value type that does not store time (e.g. ISO 19103
	 * - this check has already been performed), create an expression to
	 * restrict the field (i.e., the given column representing the property) in
	 * case that the data type of the field also stores time. In such a
	 * situation, ensure that the time stored in the field is always 00:00:00.
	 * Example: the value type of the property is (ISO 19103) "Date" and the
	 * type of the database field is Oracle DATE (which stores both date and
	 * time).
	 * 
	 * @param pi
	 * @param columnForPi
	 * @return the expression to restrict time of date, or <code>null</code> if
	 *         this is not necessary (or not implemented; check the actual
	 *         database strategies for details)
	 */
	public Expression expressionForCheckConstraintToRestrictTimeOfDate(
			PropertyInfo pi, Column columnForPi);
}
