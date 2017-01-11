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
import java.util.Set;

import de.interactive_instruments.ShapeChange.MapEntryParamInfos;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;

public interface DatabaseStrategy {

	/**
	 *
	 * @return the database data type to be used as data type for primary key
	 *         columns
	 */
	String primaryKeyDataType();

	/**
	 *
	 * @param me
	 *            map entry in configuration file
	 * @param srid
	 *            defined in configuration file
	 * @return (complex) database data type to be used as data type for the
	 *         geometry column
	 */
	String geometryDataType(ProcessMapEntry me, int srid);

	String unlimitedLengthCharacterDataType();

	String limitedLengthCharacterDataType(int size);

	/**
	 * @param columnname
	 * @param geometryCharacteristics
	 *            additional geometry specific characteristics - can be
	 *            <code>null</code>
	 * @return
	 */
	String geometryIndexColumnPart(String columnname,
			Map<String, String> geometryCharacteristics);

	/**
	 *
	 * @param normalizedClassName
	 * @param columnname
	 * @param srid
	 * @return update statement, without ; and line ending, this is done in
	 *         {@link SqlDdl}
	 */
	String geometryMetadataUpdateStatement(String normalizedClassName,
			String columnname, int srid);

	/**
	 *
	 * @param name
	 * @return name that is according to the default case of the database
	 *         system, and that does not exceed the max length for names in the
	 *         database system
	 */
	String normalizeName(String name);

	/**
	 * Database specific validation of the parameters (including their
	 * characteristics) defined by the map entries declared for the SQL DDL
	 * target.
	 * @param mapEntryByType
	 * @param mepp
	 */
	void validate(Map<String, ProcessMapEntry> mapEntryByType, MapEntryParamInfos mepp);

	/**
	 * Implementations of this method should add the generated constraint to the set with constraint names.
	 *
	 * @return name that is according to the default case of the database
	 *         system, and that does not exceed the max length for names in the
	 *         database system
	 */
	String createNameCheckConstraint(String tableName, String propertyName, Set<String> allConstraintNames);
	
	/**
	 * Implementations of this method should add the generated constraint to the set with constraint names.
	 * 
	 */
	String createNameForeignKey(String tableName, String targetTableName, String fieldName, Set<String> allConstraintNames);

}
