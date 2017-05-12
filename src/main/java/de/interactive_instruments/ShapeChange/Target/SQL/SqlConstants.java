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

import de.interactive_instruments.ShapeChange.Options;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class SqlConstants {

	/**
	 * Name for the identifier column when generating table creation statements.
	 * This parameter is optional. The default is
	 * {@value #DEFAULT_ID_COLUMN_NAME}.
	 */
	public static final String PARAM_ID_COLUMN_NAME = "idColumnName";

	/**
	 * Specification for the primary key column that is created for a 'normal'
	 * table (neither an associative table nor representing a code list).
	 * Default is {@value #DEFAULT_PRIMARYKEY_COLUMNSPEC}. For example, if the
	 * parameter is set to 'GENERATED ALWAYS AS IDENTITY (START WITH 1 INCREMENT
	 * BY 1 ORDER NOCACHE)' then the primary key field would be 'OBJECTID
	 * INTEGER GENERATED ALWAYS AS IDENTITY (START WITH 1 INCREMENT BY 1 ORDER
	 * NOCACHE) PRIMARY KEY' instead of 'OBJECTID INTEGER NOT NULL PRIMARY KEY'.
	 */
	public static final String PARAM_PRIMARYKEY_COLUMNSPEC = "primaryKeyColumnSpecification";

	/**
	 * Suffix to append to the name of columns that contain foreign keys (except
	 * if it references a table that represents a data type). This parameter is
	 * optional. The default is the empty string.
	 */
	public static final String PARAM_FOREIGN_KEY_COLUMN_SUFFIX = "foreignKeyColumnSuffix";

	/**
	 * Suffix to append to the name of columns that contain foreign keys
	 * referencing tables that represent data types. This parameter is optional.
	 * The default is the empty string.
	 */
	public static final String PARAM_FOREIGN_KEY_COLUMN_SUFFIX_DATATYPE = "foreignKeyColumnSuffixDatatype";

	/**
	 * Datatype to use for foreign key fields, for example 'bigint' in case of a
	 * PostgreSQL database. The default is the primary key type defined by the
	 * database strategy.
	 */
	public static final String PARAM_FOREIGN_KEY_COLUMN_DATA_TYPE = "foreignKeyColumnDataType";

	/**
	 * Set of SDO_DIM_ELEMENT values, in the following structure:
	 */
	public static final String PARAM_SDO_DIM_ELEMENTS = "sdoDimElements";

	/**
	 * Regular expression to validate the value of parameter
	 * {@value #PARAM_SDO_DIM_ELEMENTS}:
	 * (\([^,]+(,[-]?([0-9]+\.[0-9]+|[0-9]+)){3}\))+
	 */
	public static final String PATTERN_SDO_DIM_ELEMENTS = "(\\([^,]+(,[-]?([0-9]+\\.[0-9]+|[0-9]+)){3}\\))+";

	/**
	 * Size for fields representing textual properties with limited length, to
	 * be used in case that the property represented by the field does not have
	 * a 'size' tagged value; default is {@value #DEFAULT_SIZE}
	 */
	public static final String PARAM_SIZE = "size";

	/**
	 * EPSG code of the spatial reference system to use for geometries; default
	 * is {@value #DEFAULT_SRID}
	 */
	public static final String PARAM_SRID = "srid";

	/**
	 * Flag to indicate that foreign key creation is desired (true); default is
	 * false.
	 */
	public static final String PARAM_CREATE_REFERENCES = "createReferences";

	/**
	 * Flag to indicate that model documentation shall be added to the DDL via
	 * comments; default is true.
	 */
	public static final String PARAM_CREATE_DOCUMENTATION = "createDocumentation";

	/**
	 * Identifier of the database system for which SQL DDL shall be created.
	 * Supported systems - and also relevant identifiers - are:
	 * <ul>
	 * <li>PostgreSQL</li>
	 * <li>Oracle</li>
	 * <li>SQLServer</li>
	 * </ul>
	 * The default is PostgreSQL.
	 */
	public static final String PARAM_DATABASE_SYSTEM = "databaseSystem";

	/**
	 * Optional changes to the default documentation template and the default
	 * strings for descriptors without value
	 */
	public static final String PARAM_DOCUMENTATION_TEMPLATE = "documentationTemplate";
	public static final String PARAM_DOCUMENTATION_NOVALUE = "documentationNoValue";

	/**
	 * Comma-separated list of descriptors that shall be encoded as individual
	 * columns in a table representing a code list. The descriptors are
	 * specified by their identifier ('alias', 'definition', 'description',
	 * 'example', 'legalBasis', 'dataCaptureStatement', 'primaryCode'). NOTE:
	 * 'documentation' can also be used to include documentation that is derived
	 * from descriptors using the {@value #PARAM_DOCUMENTATION_TEMPLATE} and
	 * {@value #PARAM_DOCUMENTATION_NOVALUE}. The default value for this
	 * parameter is 'documentation'.
	 * <p>
	 * Applies to {@value #RULE_TGT_SQL_CLS_CODELISTS}
	 */
	public static final String PARAM_DESCRIPTORS_FOR_CODELIST = "descriptorsForCodelist";

	/**
	 * This parameter controls the name of the column that contains the name or
	 * - if available - the initial value of a code. Default is 'name'. NOTE:
	 * The column name will be normalized according to the rules of the chosen
	 * database system.
	 * <p>
	 * Additional columns can be defined via the configuration parameter
	 * {@value #PARAM_DESCRIPTORS_FOR_CODELIST}.
	 * <p>
	 * Applies to {@value #RULE_TGT_SQL_CLS_CODELISTS}
	 */
	public static final String PARAM_CODE_NAME_COLUMN_NAME = "codeNameColumnName";

	public static final String PARAM_CODE_NAME_SIZE = "codeNameSize";

	/**
	 * Ensures that table creation statements are generated for feature types.
	 */
	public static final String RULE_TGT_SQL_CLS_FEATURE_TYPES = "rule-sql-cls-feature-types";

	/**
	 * Ensures that table creation statements are generated for object types.
	 */
	public static final String RULE_TGT_SQL_CLS_OBJECT_TYPES = "rule-sql-cls-object-types";

	/**
	 * Ensures that CHECK constraints are created for fields representing
	 * enumeration values.
	 */
	public static final String RULE_TGT_SQL_PROP_CHECK_CONSTRAINTS_FOR_ENUMERATIONS = "rule-sql-prop-check-constraints-for-enumerations";

	/**
	 * NOTE: currently only applicable when deriving DDL for the Oracle database
	 * system.
	 */
	public static final String RULE_TGT_SQL_PROP_CHECK_CONSTRAINT_RESTRICT_TIME_OF_DATE = "rule-sql-prop-check-constraint-restrictTimeOfDate";

	/**
	 * Ensures that table creation statements are generated for complex data
	 * types.
	 */
	public static final String RULE_TGT_SQL_CLS_DATATYPES = "rule-sql-cls-data-types";

	/**
	 * Tables are generated for code lists. Insert statements are created for
	 * the codes of a code list. Properties with a code list as value type will
	 * be converted to fields with foreign key type.
	 */
	public static final String RULE_TGT_SQL_CLS_CODELISTS = "rule-sql-cls-code-lists";

	/**
	 * 
	 */
	public static final String RULE_TGT_SQL_CLS_CODELISTS_PODS = "rule-sql-cls-code-lists-pods";

	/**
	 * If this rule is enabled, then a property whose type is neither covered by
	 * a type mapping entry nor contained in the currently processed schema -
	 * but in the overall model - is still encoded as a field with a foreign key
	 * - if other rules allow table creation for this type. Otherwise the field
	 * is encoded using a textual data type.
	 */
	public static final String RULE_TGT_SQL_CLS_REFERENCES_TO_EXTERNAL_TYPES = "rule-sql-cls-references-to-external-types";

	/**
	 * This rule ensures that associative tables are created for cases in which
	 * an n:m relationship exists between types.
	 *
	 * The name of the associative table is taken from the tagged value
	 * {@value #TV_ASSOCIATIVETABLE} - which exists either on an association or
	 * an attribute. If the tagged value is not present or empty, the name is
	 * created as follows:
	 * <ul>
	 * <li>If the table represents an n:m relationship represented by an
	 * association, then:
	 * <ul>
	 * <li>for a bi-directional association: the name of the class (from both
	 * ends of the association) that is lower in alphabetical order is used,
	 * concatenated with the according property name</li>
	 * <li>for a uni-directional association: the name of the inClass of the
	 * navigable property is used, concatenated with the property name</li>
	 * </ul>
	 * </li>
	 * <li>If the table represents an n:m relationship that is caused by an
	 * attribute with max multiplicity greater than one, then the name of the
	 * class that the attribute belongs to is used, concatenated with the
	 * property name</li>
	 * </ul>
	 */
	public static final String RULE_TGT_SQL_ALL_ASSOCIATIVETABLES = "rule-sql-all-associativetables";

	/**
	 * If this rule is enabled derived properties will be ignored.
	 */
	public static final String RULE_TGT_SQL_PROP_EXCLUDE_DERIVED = "rule-sql-prop-exclude-derived";

	/**
	 * If this rule is enabled, abstract classes will be ignored by the target.
	 */
	public static final String RULE_TGT_SQL_ALL_EXCLUDE_ABSTRACT = "rule-sql-all-exclude-abstract";

	public static final String RULE_TGT_SQL_ALL_NOTENCODED = "rule-sql-all-notEncoded";

	/**
	 * Under this rule, foreign key identifiers are generated as follows:
	 * <p>
	 * "fk_" + tableNameForFK + "" + targetTableNameForFK + "" + fieldNameForFK
	 * + count where:
	 * <ul>
	 * <li>tableNameForFK is the name of the table that contains the field with
	 * the foreign key, clipped to the first eight characters</li>
	 * <li>targetTableNameForFK is the name of the table that the field with
	 * foreign key references, clipped to the first eight characters</li>
	 * <li>fieldNameForFK is the name of the field that contains the foreign
	 * key, clipped to the first eight characters</li>
	 * <li>count is the number of times the foreign key identifier has been
	 * assigned; it ranges from 0-9 and can also be omitted, thus supporting
	 * eleven unambiguous uses of the foreign key identifier (NOTE: if the
	 * foreign key identifier is used more than eleven times, ShapeChange logs
	 * an error)</li>
	 * </ul>
	 */
	public static final String RULE_TGT_SQL_ALL_FOREIGNKEY_ORACLE_NAMING_STYLE = "rule-sql-all-foreign-key-oracle-naming-style";
	public static final String RULE_TGT_SQL_ALL_FOREIGNKEY_PEARSONHASH_NAMING = "rule-sql-all-foreign-key-personhash-naming";
	public static final String RULE_TGT_SQL_ALL_FOREIGNKEY_DEFAULT_NAMING = "rule-sql-all-foreign-key-default-naming";

	public static final String RULE_TGT_SQL_ALL_CHECK_CONSTRAINT_NAMING_ORACLE_DEFAULT = "rule-sql-all-check-constraint-naming-oracle-default";
	public static final String RULE_TGT_SQL_ALL_CHECK_CONSTRAINT_NAMING_POSTGRESQL_DEFAULT = "rule-sql-all-check-constraint-naming-postgresql-default";
	public static final String RULE_TGT_SQL_ALL_CHECK_CONSTRAINT_NAMING_SQLSERVER_DEFAULT = "rule-sql-all-check-constraint-naming-sqlserver-default";
	public static final String RULE_TGT_SQL_ALL_CHECK_CONSTRAINT_NAMING_PEARSONHASH = "rule-sql-all-check-constraint-naming-pearsonhash";

	/**
	 * If this rule is included, case is not changed when normalizing names,
	 * regardless of the rule for normalizing names.
	 */
	public static final String RULE_TGT_SQL_ALL_NORMALIZING_IGNORE_CASE = "rule-sql-all-normalizing-ignore-case";
	public static final String RULE_TGT_SQL_ALL_NORMALIZING_LOWER_CASE = "rule-sql-all-normalizing-lower-case";
	public static final String RULE_TGT_SQL_ALL_NORMALIZING_UPPER_CASE = "rule-sql-all-normalizing-upper-case";
	public static final String RULE_TGT_SQL_ALL_NORMALIZING_SQLSERVER = "rule-sql-all-normalizing-sqlserver";
	public static final String RULE_TGT_SQL_ALL_NORMALIZING_ORACLE = "rule-sql-all-normalizing-oracle";

	public static final String RULE_TGT_SQL_ALL_UNIQUE_NAMING_COUNT_SUFFIX = "rule-sql-all-unique-naming-count-suffix";

	/* --------------------- */
	/* --- Tagged Values --- */
	/* --------------------- */

	public static final String TV_ASSOCIATIVETABLE = "associativeTable";

	/* -------------------- */
	/* --- other fields --- */
	/* -------------------- */

	/**
	 * <pre>
	 * (name|documentation|alias|definition|description|example|legalBasis|dataCaptureStatement|primaryCode)(\(((columnName|size)=\w+)(,(columnName|size)=\w+)*\))?
	 * </pre>
	 */
	public static final String DESCRIPTORS_FOR_CODELIST_REGEX = "(name|documentation|alias|definition|description|example|legalBasis|dataCaptureStatement|primaryCode)(\\(((columnName|size)=\\w+)(;(columnName|size)=\\w+)*\\))?";

	public static final String DEFAULT_CODE_NAME_COLUMN_NAME = "name";
	public static final String DEFAULT_ID_COLUMN_NAME = "_id";
	public static final String DEFAULT_FOREIGN_KEY_COLUMN_SUFFIX = "";
	public static final String DEFAULT_FOREIGN_KEY_COLUMN_SUFFIX_DATATYPE = "";
	public static final String DEFAULT_PRIMARYKEY_COLUMNSPEC = "NOT NULL";
	public static final int DEFAULT_SIZE = 1024;
	public static final int DEFAULT_CODE_NAME_SIZE = 0;
	public static final int DEFAULT_SRID = 4326;
	public static final boolean DEFAULT_CREATE_REFERNCES = false;
	public static final boolean DEFAULT_CREATE_DOCUMENTATION = true;

	/**
	 * Name of the parameter to provide characteristics for encoding an initial
	 * value of an attribute as a default value.
	 */
	public static final String ME_PARAM_DEFAULTVALUE = "defaultValue";

	/**
	 * Characteristic for the parameter {@value #ME_PARAM_DEFAULTVALUE} that
	 * specifies the value to represent the Boolean value 'true' in the mapping
	 * that the parameter applies to. Default is TRUE.
	 */
	public static final String ME_PARAM_DEFAULTVALUE_CHARACT_TRUE = "true";
	/**
	 * Characteristic for the parameter {@value #ME_PARAM_DEFAULTVALUE} that
	 * specifies the value to represent the Boolean value 'true' in the mapping
	 * that the parameter applies to. Default is FALSE.
	 */
	public static final String ME_PARAM_DEFAULTVALUE_CHARACT_FALSE = "false";
	/**
	 * Characteristic for the parameter {@value #ME_PARAM_DEFAULTVALUE} that, if
	 * set to 'true' (ignoring case) specifies that the default value shall be
	 * quoted. Default is false. Typically, this characteristic is set to true
	 * in map entries for types that map to a textual type, but it can also
	 * apply to date types. It usually does not apply to numeric types.
	 */
	public static final String ME_PARAM_DEFAULTVALUE_CHARACT_QUOTED = "quoted";

	/**
	 * Name of the parameter to indicate (via the 'param' attribute) that a map
	 * entry contains information about a geometry type.
	 */
	public static final String ME_PARAM_GEOMETRY = "geometry";
	/**
	 * Name of the parameter to indicate (via the 'param' attribute) that the
	 * type of a map entry is represented by a table.
	 */
	public static final String ME_PARAM_TABLE = "table";
	/**
	 * A characteristic for the parameter {@value #ME_PARAM_TABLE} that gives
	 * information about the category of the conceptual type that is identified
	 * by the map entry.
	 *
	 * Recognized values are (currently there is only one):
	 * <ul>
	 * <li>datatype</li>
	 * </ul>
	 */
	public static final String ME_PARAM_TABLE_CHARACT_REP_CAT = "representedCategory";
	/**
	 * Regular expression (?i:datatype) to check that a given string is one of a
	 * list of allowed values (NOTE1: check is case-insensitive; NOTE2: at the
	 * moment there is only one valid value).
	 */
	public static final String ME_PARAM_TABLE_CHARACT_REP_CAT_VALIDATION_REGEX = "(?i:datatype)";
	public static final String ME_PARAM_TEXTORCHARACTERVARYING = "textOrCharacterVarying";

	/*
	 * MAP_TARGETTYPE_COND_PART and MAP_TARGETTYPE_COND_TEXTORCHARACTERVARYING
	 * are kept for backwards compatibility
	 */
	public static final String MAP_TARGETTYPE_COND_PART = "cond:";
	public static final String MAP_TARGETTYPE_COND_TEXTORCHARACTERVARYING = "textOrCharacterVarying";

	public static final String CRLF = Options.CRLF;
	public static final String INDENT = "   ";
}
