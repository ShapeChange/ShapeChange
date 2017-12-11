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

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class DatabaseModelConstants {

	/**
	 * If this rule is included, the target creates an internal SQL structure.
	 * However, instead of deriving DDL from this structure, a database model
	 * inside an Enterprise Architect repository is created.
	 */
	public static final String RULE_TGT_SQL_ALL_DBMODEL = "rule-sql-all-databaseModel";

	/**
	 * Path (without a trailing "/") to the directory that contains the database
	 * model pattern XMI templates. The default is
	 * "http://shapechange.net/resources/dataModelPatterns".
	 */
	public static final String PARAM_DM_PATTERN_PATH = "dataModelPatternPath";

	/**
	 * This optional parameter can be used to specify the database owner. For
	 * example, for PostgreSQL, one could set this to "public". There is no
	 * default value.
	 */
	public static final String PARAM_DB_OWNER = "dbOwner";
	
	/**
	 * This optional parameter can be used to specify the database version. For
	 * example, for Oracle, one could set this to "12.01.0020". There is no
	 * default value.
	 */
	public static final String PARAM_DB_VERSION = "dbVersion";
	
	/**
	 * This optional parameter can be used to specify the tablespace. For
	 * example, for Oracle, one could set this to "USERS". There is no
	 * default value.
	 */
	public static final String PARAM_TABLESPACE = "tablespace";
}
