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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange.Target.SQL;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
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
	 * example, for Oracle, one could set this to "USERS". There is no default
	 * value.
	 */
	public static final String PARAM_TABLESPACE = "tablespace";

	/**
	 * When creating a database model, ShapeChange loads DBMS specific templates
	 * into an EA repository (which is either created on the fly, or taken from
	 * a configured location). These templates add &lt;&lt;DataModel&gt;&gt;
	 * packages with specific names to the repository. If such a package already
	 * exists, ShapeChange will create another package with the same name (but
	 * different GUIDs for its contents). This could cause confusion. To avoid
	 * any confusion, ShapeChange can delete a pre-existing data model package
	 * that has the same name as the one that would be added via the template.
	 * To do so, set this parameter to true. Default is false.
	 */
	public static final String PARAM_DELETE_PREEXISTING_DATAMODEL_PACKAGE = "deletePreExistingDataModelPackage";

	/**
	 * This optional parameter can be used to provide the path to the EA repo file
	 * in which the data model shall be created. If the value is a URL, the file
	 * will be copied to the output directory. If the value is a path to a local
	 * file, that file will be used. If the local file (including the directory
	 * structure) does not exist yet, it will be created. The parameter can be
	 * used to write data models for different DBMSs into the same EA repo file,
	 * rather than having them written into different EA repos. To achieve this,
	 * simply specify a local EA repo file using this parameter in the SQL DDL
	 * target configurations via which the DBMS specific data models are
	 * created.
	 * 
	 */
	public static final String PARAM_DATAMODEL_EA_REPOSITORY_PATH = "dataModelEaRepositoryPath";
	public static final String PARAM_DATAMODEL_EAP_PATH = "dataModelEapPath";

	/**
	 * By default, all table elements of the database model will be created as
	 * direct children of the table package defined by the database model
	 * pattern. If this parameter is set to true, then a package hierarchy will
	 * be created inside that table package, corresponding to the hierarchy of
	 * packages that the class represented by a table is in within its
	 * application schema. If the number of encoded schemas is greater than 1,
	 * then the application schema packages are included in the hierarchy.
	 * Tables that do not represent a specific class (example: associative
	 * tables) will still be created inside the tables package.
	 */
	public static final String PARAM_ESTABLISH_PACKAGE_HIERARCHY = "dataModelEstablishPackageHierarchy";
	
	  /**
	   * Optional (default is determined by the EA process) - Value for the field 'Author' of an EA element.
	   */
	  public static final String PARAM_EA_AUTHOR = "eaAuthor";
	  
	  /**
	   * Optional (default is determined by the EA process) - Value for the field 'Status' of an EA element.
	   */
	  public static final String PARAM_EA_STATUS = "eaStatus";
}
