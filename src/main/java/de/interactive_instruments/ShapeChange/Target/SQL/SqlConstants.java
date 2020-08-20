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

import java.util.regex.Pattern;

import de.interactive_instruments.ShapeChange.Options;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class SqlConstants {

    /* ------------------ */
    /* --- Parameters --- */
    /* ------------------ */

    public static final String PARAM_POSTGRESQL_ROLE = "postgreSqlRole";
    
    /**
     * Optional (default is determined by the EA process) - Value for the field
     * 'Author' of an EA element.
     */
    public static final String PARAM_AUTHOR = "eaAuthor";

    /**
     * Optional (default is determined by the EA process) - Value for the field
     * 'Status' of an EA element.
     */
    public static final String PARAM_STATUS = "eaStatus";

    /**
     * Set to <code>true</code> if empty lines should be removed in SQL DDL files
     * created by the target. Some SQL clients choke on such lines. Default is
     * <code>false</code>.
     */
    public static final String PARAM_REMOVE_EMPTY_LINES_IN_DDL_OUTPUT = "removeEmptyLinesInDdlOutput";

    /**
     * Absolute or relative path to the text file (character encoding is assumed to
     * be UTF-8) whose contents shall be added at the top of DDL files produced by
     * the target. This parameter is optional. No default value exists.
     */
    public static final String PARAM_FILE_DDL_TOP = "fileDdlTop";

    /**
     * Absolute or relative path to the text file (character encoding is assumed to
     * be UTF-8) whose contents shall be added at the bottom of DDL files produced
     * by the target. This parameter is optional. No default value exists.
     */
    public static final String PARAM_FILE_DDL_BOTTOM = "fileDdlBottom";

    /**
     * Name for the identifier column when generating table creation statements.
     * This parameter is optional. The default is {@value #DEFAULT_ID_COLUMN_NAME}.
     */
    public static final String PARAM_ID_COLUMN_NAME = "idColumnName";

    /**
     * To qualify textual data types with limited length, for all cases in which
     * such a type is not generated based upon a type map entry.
     *
     * Recognized values for the Oracle database system are:
     * <ul>
     * <li>BYTE</li>
     * <li>CHAR</li>
     * </ul>
     */
    public static final String PARAM_LENGTH_QUALIFIER = "lengthQualifier";

    /**
     * Specification for the primary key of a 'normal' table (neither an associative
     * table nor representing a code list). Default is
     * {@value #DEFAULT_PRIMARYKEY_SPEC}. For example, if the parameter is set to
     * 'GENERATED ALWAYS AS IDENTITY (START WITH 1 INCREMENT BY 1 ORDER NOCACHE)'
     * then the primary key would be 'OBJECTID INTEGER GENERATED ALWAYS AS IDENTITY
     * (START WITH 1 INCREMENT BY 1 ORDER NOCACHE) PRIMARY KEY' instead of 'OBJECTID
     * INTEGER NOT NULL PRIMARY KEY'.
     */
    public static final String PARAM_PRIMARYKEY_SPEC = "primaryKeySpecification";

    /**
     * Specification for the primary key of a code list table. Default is
     * {@value #DEFAULT_PRIMARYKEY_SPEC_CODELIST}.
     */
    public static final String PARAM_PRIMARYKEY_SPEC_CODELIST = "primaryKeySpecificationCodelist";

    /**
     * Suffix to append to the name of columns that contain foreign keys (except if
     * it references a table that represents a data type). This parameter is
     * optional. The default is the empty string.
     */
    public static final String PARAM_FOREIGN_KEY_COLUMN_SUFFIX = "foreignKeyColumnSuffix";

    /**
     * Replaces the value of parameter {@value #PARAM_FOREIGN_KEY_COLUMN_SUFFIX} if
     * the property represented is a reflexive property. This parameter is optional.
     * The default is the empty string.
     */
    public static final String PARAM_REFLEXIVE_REL_FIELD_SUFFIX = "reflexiveRelationshipFieldSuffix";

    /**
     * Suffix to append to the name of columns that contain foreign keys referencing
     * tables that represent code lists. This parameter is optional. The default is
     * the value of parameter {@value #PARAM_FOREIGN_KEY_COLUMN_SUFFIX} (for
     * backwards compatibility).
     */
    public static final String PARAM_FOREIGN_KEY_COLUMN_SUFFIX_CODELIST = "foreignKeyColumnSuffixCodelist";

    /**
     * Suffix to append to the name of columns that contain foreign keys referencing
     * tables that represent data types. This parameter is optional. The default is
     * the empty string.
     */
    public static final String PARAM_FOREIGN_KEY_COLUMN_SUFFIX_DATATYPE = "foreignKeyColumnSuffixDatatype";

    /**
     * Global definition of the dimension of geometry types, which is used by DBMSs
     * such as SQLite (more specifically, SQLite in combination with the spatial
     * extension SpatiaLite). Used as fallback if no specific geometry dimension is
     * defined via the map entry (and its geometry dimension characteristic) that
     * applies to the value type of a geometry typed property.
     */
    public static final String PARAM_GEOMETRY_DIMENSION = "geometryDimension";

    /**
     * Datatype to use for foreign key fields, for example 'bigint' in case of a
     * PostgreSQL database. The default is the primary key type defined by the
     * database strategy.
     */
    public static final String PARAM_FOREIGN_KEY_COLUMN_DATA_TYPE = "foreignKeyColumnDataType";
    public static final String PARAM_FOREIGN_KEY_COLUMN_DATA_TYPE_ALIAS = "foreignKeyColumnDatatype";

    /**
     * NOTE: This parameter applies to the Oracle database system only. Set of
     * SDO_DIM_ELEMENT values, to be used for constructing a SDO_DIM_ARRAY when
     * inserting data into USER_SDO_GEOM_METADATA. Each value has the following
     * structure: (&lt;first_dimension_name&gt;,
     * &lt;first_dimension_lower_bound&gt;, &lt;first_dimension_upper_bound&gt;,
     * &lt;first_dimension_tolerance&gt;). There is no separator between individual
     * values (the parentheses serve as separator). Example:
     * (dim1,-1,1,1.1)(dim2,2,-2.2,2)(dim3,3.3,3,-3).
     */
    public static final String PARAM_SDO_DIM_ELEMENTS = "sdoDimElements";

    /**
     * If the value of this parameter is 'true' (ignoring case), then SQL statements
     * related to spatial indexes (creation, but also insertion of geometry
     * metadata) are written to a separate output file. The name of that file will
     * be that of the main DDL file, plus suffix '_spatial'.
     */
    public static final String PARAM_SEPARATE_SPATIAL_INDEX_STATEMENTS = "separateSpatialIndexStatements";

    /**
     * If this parameter is included in the configuration, then SQL statements for
     * insertion of codes into codelist tables are written to separate output files.
     * The value of the parameter is a (comma-separated) list of categories. For
     * each of these categories, the insert statements where the code list has
     * tagged value 'codelistType' with a value equal to the category are written to
     * a new output file. The name of that file will be that of the main DDL file,
     * plus suffix '_inserts_codelistType_{category}'.
     */
    public static final String PARAM_SEPARATE_CODE_INSERT_STATEMENTS_BY_CODELIST_TYPE = "separateCodeInsertStatementsByCodelistType";

    /**
     * Regular expression to validate the value of parameter
     * {@value #PARAM_SDO_DIM_ELEMENTS}:
     * (\([^,]+(,[-]?([0-9]+\.[0-9]+|[0-9]+)){3}\))+
     */
    public static final String PATTERN_SDO_DIM_ELEMENTS = "(\\([^,]+(,[-]?([0-9]+\\.[0-9]+|[0-9]+)){3}\\))+";

    /**
     * Name of the tagged value that provides the short name for a model element,
     * when used in constructing specific names (e.g. of constraints). Default is
     * 'shortName'.
     */
    public static final String PARAM_SHORT_NAME_BY_TAGGED_VALUE = "shortNameByTaggedValue";

    /**
     * Size for fields representing textual properties with limited length, to be
     * used in case that the property represented by the field does not have a
     * 'size' tagged value; default is {@value #DEFAULT_SIZE}
     */
    public static final String PARAM_SIZE = "size";

    /**
     * EPSG code of the spatial reference system to use for geometries; default is
     * {@value #DEFAULT_SRID}
     */
    public static final String PARAM_SRID = "srid";

    public static final String PARAM_TVS_TO_KEEP = "taggedValuesToKeep";

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
     * columns in a table representing a code list. The descriptors are specified by
     * their identifier ('alias', 'definition', 'description', 'example',
     * 'legalBasis', 'dataCaptureStatement', 'primaryCode'). NOTE: 'documentation'
     * can also be used to include documentation that is derived from descriptors
     * using the {@value #PARAM_DOCUMENTATION_TEMPLATE} and
     * {@value #PARAM_DOCUMENTATION_NOVALUE}. The default value for this parameter
     * is 'documentation'.
     * <p>
     * Applies to {@value #RULE_TGT_SQL_CLS_CODELISTS}
     */
    public static final String PARAM_DESCRIPTORS_FOR_CODELIST = "descriptorsForCodelist";

    /**
     * Specify the conceptual type that applies to the codeStatusCL column added by
     * {@value #RULE_TGT_SQL_CLS_CODELISTS_PODS}. Default value is
     * {@value #DEFAULT_CODESTATUSCL_TYPE}.
     */
    public static final String PARAM_CODESTATUSCL_TYPE = "codeStatusCLType";

    /**
     * Specify the length of an codeStatusCL column added by
     * {@value #RULE_TGT_SQL_CLS_CODELISTS_PODS}, in case that the code status type
     * is an enumeration. Default value is {@value #DEFAULT_CODESTATUSCL_LENGTH}.
     */
    public static final String PARAM_CODESTATUSCL_LENGTH = "codeStatusCLLength";

    /**
     * This parameter controls the name of the column that contains the name or - if
     * available - the initial value of a code. Default is 'name'. NOTE: The column
     * name will be normalized according to the rules of the chosen database system.
     * <p>
     * Additional columns can be defined via the configuration parameter
     * {@value #PARAM_DESCRIPTORS_FOR_CODELIST}.
     * <p>
     * Applies to {@value #RULE_TGT_SQL_CLS_CODELISTS}
     */
    public static final String PARAM_CODE_NAME_COLUMN_NAME = "codeNameColumnName";

    public static final String PARAM_CODE_NAME_COLUMN_DOCUMENTATION = "codeNameColumnDocumentation";

    public static final String PARAM_CODE_NAME_SIZE = "codeNameSize";

    /**
     * Defines the first part of the name of the column in a data type table that is
     * used to reference tables that represent types from the conceptual model which
     * have a one to many relationship with the data type. Applies to
     * {@value #RULE_TGT_SQL_CLS_DATATYPES_ONETOMANY_ONETABLE}. This parameter is
     * optional. The default is {@value #DEFAULT_ONE_TO_MANY_REF_COLUMN_NAME}.
     */
    public static final String PARAM_ONE_TO_MANY_REF_COLUMN_NAME = "oneToManyReferenceColumnName";

    public static final String DEFAULT_ONE_TO_MANY_REF_COLUMN_NAME = "dataTypeOwner";

    /**
     * Define the name for the column that stores the value of the code status code
     * list. Applies to rule {@value #RULE_TGT_SQL_CLS_CODELISTS_PODS}. Default
     * value is: {@value #DEFAULT_NAME_CODESTATUS_CL_COLUMN}.
     */
    public static final String PARAM_NAME_CODESTATUS_CL_COLUMN = "nameForCodeStatusCLColumn";
    public static final String DEFAULT_NAME_CODESTATUS_CL_COLUMN = "CODE_STATUS_CL";

    public static final String PARAM_CODESTATUS_CL_COLUMN_DOCUMENTATION = "codeStatusCLColumnDocumentation";

    /**
     * Define the name for the column that stores a note on the code status. Applies
     * to rule {@value #RULE_TGT_SQL_CLS_CODELISTS_PODS}. Default value is:
     * {@value #DEFAULT_NAME_CODESTATUSNOTES_COLUMN}.
     */
    public static final String PARAM_NAME_CODESTATUSNOTES_COLUMN = "nameForCodeStatusNotesColumn";
    public static final String DEFAULT_NAME_CODESTATUSNOTES_COLUMN = "CODE_STATUS_NOTES";
    public static final String PARAM_CODESTATUS_NOTES_COLUMN_DOCUMENTATION = "codeStatusNotesColumnDocumentation";

    public static final String PARAM_NAME_CODESUPERCEDES_COLUMN = "nameForCodeSupercedesColumn";
    public static final String DEFAULT_NAME_CODESUPERCEDES_COLUMN = "CODE_SUPERCEDES";
    public static final String PARAM_CODE_SUPERSEDES_COLUMN_DOCUMENTATION = "codeSupercedesColumnDocumentation";

    /* ------------------------ */
    /* --- Conversion rules --- */
    /* ------------------------ */

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
     * Unique constraints are created for fields representing a property with tagged
     * value 'sqlUnique' = true.
     */
    public static final String RULE_TGT_SQL_PROP_UNIQUE_CONSTRAINTS = "rule-sql-prop-uniqueConstraints";

    /**
     * NOTE: currently only applicable when deriving DDL for the Oracle database
     * system.
     */
    public static final String RULE_TGT_SQL_PROP_CHECK_CONSTRAINT_RESTRICT_TIME_OF_DATE = "rule-sql-prop-check-constraint-restrictTimeOfDate";

    public static final String RULE_TGT_SQL_PROP_CHECK_CONSTRAINT_FOR_RANGE = "rule-sql-prop-check-constraint-for-range";
    /**
     * Ensures that table creation statements are generated for complex data types.
     */
    public static final String RULE_TGT_SQL_CLS_DATATYPES = "rule-sql-cls-data-types";

    /**
     * Specific implementation of a one to many relationship to a data type: the
     * table that represents the data type contains an additional column that
     * references other tables (which represent classes that have a one-to-many
     * relationship with the data type). The type of the column is configured via
     * parameter {@value #PARAM_FOREIGN_KEY_COLUMN_DATA_TYPE}. The name of the
     * column is set via tagged value {@value #TV_ONE_TO_MANY_REF_COLUMN_NAME} on
     * the data type or, if the tagged value is not available, via the configuration
     * parameter {@value #PARAM_ONE_TO_MANY_REF_COLUMN_NAME}. NOTE: This approach
     * does not support specification of a foreign key constraint for the column,
     * since the data type may be used as property value type in multiple other
     * types. Thus, in this approach, one cannot directly identify which table is
     * referenced by the column, for a given row of the data type table. NOTE: This
     * rule has lower priority than
     * {@value #RULE_TGT_SQL_CLS_DATATYPES_ONETOMANY_SEVERALTABLES}.
     */
    public static final String RULE_TGT_SQL_CLS_DATATYPES_ONETOMANY_ONETABLE = "rule-sql-cls-data-types-oneToMany-oneTable";

    /**
     * Extends {@value #RULE_TGT_SQL_CLS_DATATYPES_ONETOMANY_ONETABLE} to prevent
     * creation of a field for an attribute with a data type - for which a table is
     * created - as type, when the attribute has max cardinality 1. The
     * 'dataTypeOwner' field on the data type table can be used to establish the
     * relationship. That field will be encoded as NOT NULL. Note that the
     * 'dataTypeOwner' could be misused in such a case, when more than one entry
     * references the table that represents the class with the attribute. That would
     * result in multiple values for the attribute, which is not allowed by the
     * conceptual model.
     */
    public static final String RULE_TGT_SQL_CLS_DATATYPES_ONETOMANY_ONETABLE_IGNORE_SINGLE_VALUED_CASE = "rule-sql-cls-data-types-oneToMany-oneTable-ignoreSingleValuedCase";

    /**
     * Specific implementation of a one to many relationship between a type A and a
     * data type B: for each such relationship, a new table is created for the data
     * type (as defined by {@value #RULE_TGT_SQL_CLS_DATATYPES}. The name of such a
     * table is constructed as follows: name of type A (that references the data
     * type) + "_" + name of the property with the data type as value type. A column
     * is added to the table to reference the table that represents type A.
     * <p>
     * NOTE: This rule has higher priority than
     * {@value #RULE_TGT_SQL_CLS_DATATYPES_ONETOMANY_ONETABLE}.
     */
    public static final String RULE_TGT_SQL_CLS_DATATYPES_ONETOMANY_SEVERALTABLES = "rule-sql-cls-data-types-oneToMany-severalTables";

    /**
     * Extends {@value #RULE_TGT_SQL_CLS_DATATYPES_ONETOMANY_SEVERALTABLES} to avoid
     * the creation of the general table for a data type, if that table is not used.
     * More specifically, check if no attribute in the schemas selected for
     * processing with the data type as type has max cardinality = 1. In that case,
     * the table that represents the data type, and that supports such a case, would
     * not be used - and is thus not needed in the resulting DDL schema.
     */
    public static final String RULE_TGT_SQL_CLS_DATATYPES_ONETOMANY_SEVERALTABLES_AVOID_TABLE_FOR_DATATYPE_IF_UNUSED = "rule-sql-cls-data-types-oneToMany-severalTables-avoidTableForDatatypeIfUnused";

    /**
     * Tables are generated for code lists. Insert statements are created for the
     * codes of a code list. Properties with a code list as value type will be
     * converted to fields with foreign key type.
     */
    public static final String RULE_TGT_SQL_CLS_CODELISTS = "rule-sql-cls-code-lists";

    /**
     * Enables use of stereotype 'identifier' on class attributes. If an attribute
     * with that stereotype belongs to a class, then the column to represent that
     * attribute will be used as primary key (and no extra identifier column will be
     * generated).
     */
    public static final String RULE_TGT_SQL_CLS_IDENTIFIER_STEREOTYPE = "rule-sql-cls-identifierStereotype";

    /**
     * 
     */
    public static final String RULE_TGT_SQL_CLS_CODELISTS_PODS = "rule-sql-cls-code-lists-pods";

    /**
     * If this rule is enabled, then a property whose type is neither covered by a
     * type mapping entry nor contained in the currently processed schema - but in
     * the overall model - is still encoded as a field with a foreign key - if other
     * rules allow table creation for this type. Otherwise the field is encoded
     * using a textual data type.
     */
    public static final String RULE_TGT_SQL_CLS_REFERENCES_TO_EXTERNAL_TYPES = "rule-sql-cls-references-to-external-types";

    /**
     * This rule ensures that associative tables are created for cases in which an
     * n:m relationship exists between types.
     *
     * The name of the associative table is taken from the tagged value
     * {@value #TV_ASSOCIATIVETABLE} - which exists either on an association or an
     * attribute. If the tagged value is not present or empty, the name is created
     * as follows:
     * <ul>
     * <li>If the table represents an n:m relationship represented by an
     * association, then:
     * <ul>
     * <li>for a bi-directional association: the name of the class (from both ends
     * of the association) that is lower in alphabetical order is used, concatenated
     * with the according property name</li>
     * <li>for a uni-directional association: the name of the inClass of the
     * navigable property is used, concatenated with the property name</li>
     * </ul>
     * </li>
     * <li>If the table represents an n:m relationship that is caused by an
     * attribute with max multiplicity greater than one, then the name of the class
     * that the attribute belongs to is used, concatenated with the property
     * name</li>
     * </ul>
     */
    public static final String RULE_TGT_SQL_ALL_ASSOCIATIVETABLES = "rule-sql-all-associativetables";

    /**
     * If this rule is enabled derived properties will be ignored.
     */
    public static final String RULE_TGT_SQL_PROP_EXCLUDE_DERIVED = "rule-sql-prop-exclude-derived";

    /**
     * For properties or numerically valued code lists with tagged value 'precision'
     * and 'scale' (both with integer value), an according suffix is added to the
     * datatype declaration, resulting in, for example, 'number(5,2)' instead of
     * just 'number'. Scale can be omitted. If scale is provided but precision is
     * omitted, a warning is logged and the datatype is not changed.
     */
    public static final String RULE_TGT_SQL_ALL_PRECISION_AND_SCALE = "rule-sql-all-precisionAndScale";

    /**
     * If this rule is enabled, abstract classes will be ignored by the target.
     */
    public static final String RULE_TGT_SQL_ALL_EXCLUDE_ABSTRACT = "rule-sql-all-exclude-abstract";

    public static final String RULE_TGT_SQL_ALL_NOTENCODED = "rule-sql-all-notEncoded";

    /**
     * Under this rule, foreign key identifiers are generated as follows:
     * <p>
     * "fk_" + tableNameForFK + "" + targetTableNameForFK + "" + fieldNameForFK +
     * count where:
     * <ul>
     * <li>tableNameForFK is the name of the table that contains the field with the
     * foreign key, clipped to the first eight characters</li>
     * <li>targetTableNameForFK is the name of the table that the field with foreign
     * key references, clipped to the first eight characters</li>
     * <li>fieldNameForFK is the name of the field that contains the foreign key,
     * clipped to the first eight characters</li>
     * <li>count is the number of times the foreign key identifier has been
     * assigned; it ranges from 0-9 and can also be omitted, thus supporting eleven
     * unambiguous uses of the foreign key identifier (NOTE: if the foreign key
     * identifier is used more than eleven times, ShapeChange logs a warning)</li>
     * </ul>
     */
    public static final String RULE_TGT_SQL_ALL_FOREIGNKEY_ORACLE_NAMING_STYLE = "rule-sql-all-foreign-key-oracle-naming-style";
    /**
     * Under this rule, foreign key identifiers are generated as follows:
     * <p>
     * "fk_" + tableName + "_" + targetTableName + "_" + fieldName + pearsonHash
     * <p>
     * where:
     * <ul>
     * <li>tableName is the name of the table that contains the field with the
     * foreign key, clipped to the first seven characters</li>
     * <li>targetTableName is the name of the table that the field with foreign key
     * references, clipped to the first seven characters</li>
     * <li>fieldName is the name of the field that contains the foreign key, clipped
     * to the first seven characters</li>
     * <li>pearsonHash is the pearson hash (see
     * https://en.wikipedia.org/wiki/Pearson_hashing and the original paper:
     * Pearson, Peter K. (June 1990), "Fast Hashing of Variable-Length Text
     * Strings", Communications of the ACM, 33 (6): 677, doi:10.1145/78973.78978) of
     * the concatenation of tableName, targetTableName, and fieldName, padded with
     * zeros so it has a length of 3</li>
     * </ul>
     * NOTE: The total length of the foreign key constraint will not exceed 29
     * characters.
     * 
     */
    public static final String RULE_TGT_SQL_ALL_FOREIGNKEY_PEARSONHASH_NAMING = "rule-sql-all-foreign-key-pearsonhash-naming";
    public static final String RULE_TGT_SQL_ALL_CHECK_CONSTRAINT_NAMING_ORACLE_DEFAULT = "rule-sql-all-check-constraint-naming-oracle-default";
    public static final String RULE_TGT_SQL_ALL_CHECK_CONSTRAINT_NAMING_POSTGRESQL_DEFAULT = "rule-sql-all-check-constraint-naming-postgresql-default";
    public static final String RULE_TGT_SQL_ALL_CHECK_CONSTRAINT_NAMING_SQLSERVER_DEFAULT = "rule-sql-all-check-constraint-naming-sqlserver-default";
    public static final String RULE_TGT_SQL_ALL_CHECK_CONSTRAINT_NAMING_PEARSONHASH = "rule-sql-all-check-constraint-naming-pearsonhash";

    public static final String RULE_TGT_SQL_ALL_CONSTRAINTNAMEUSINGSHORTNAME = "rule-sql-all-constraintNameUsingShortName";
    public static final String RULE_TGT_SQL_ALL_INDEXNAMEUSINGSHORTNAME = "rule-sql-all-indexNameUsingShortName";

    /**
     * If this rule is included, case is not changed when normalizing names,
     * regardless of the rule for normalizing names.
     */
    public static final String RULE_TGT_SQL_ALL_NORMALIZING_IGNORE_CASE = "rule-sql-all-normalizing-ignore-case";
    public static final String RULE_TGT_SQL_ALL_NORMALIZING_LOWER_CASE = "rule-sql-all-normalizing-lower-case";
    public static final String RULE_TGT_SQL_ALL_NORMALIZING_UPPER_CASE = "rule-sql-all-normalizing-upper-case";
    public static final String RULE_TGT_SQL_ALL_NORMALIZING_SQLSERVER = "rule-sql-all-normalizing-sqlserver";
    public static final String RULE_TGT_SQL_ALL_NORMALIZING_ORACLE = "rule-sql-all-normalizing-oracle";

    public static final String RULE_TGT_SQL_ALL_REPRESENT_TAGGED_VALUES = "rule-sql-all-representTaggedValues";

    /**
     * Prevents creation of documentation of schema elements via inline comments.
     * This rule overrides parameter {@value #PARAM_CREATE_DOCUMENTATION}.
     */
    public static final String RULE_TGT_SQL_ALL_SUPPRESS_INLINE_DOCUMENTATION = "rule-sql-all-suppressDocumentationViaInlineComments";

    /**
     * Create database objects in specific database schemas. The name of such a
     * schema is given by the tagged value 'sqlSchema' of the conceptual schema to
     * which the model element that is being converted belongs (for n:m associative
     * tables, that is determined by one of the roles of the represented
     * association). If that tagged value is not set, then the tagged value 'xmlns'
     * of the conceptual schema is used (and if that does not have a value either,
     * 'fixme' is used).
     * 
     * NOTE: Map entries that represent tables are expected to include the schema
     * name in XML attribute targetType.
     * 
     * NOTE: Currently only supported for the PostgreSQL database system
     */
    public static final String RULE_TGT_SQL_ALL_SCHEMAS = "rule-sql-all-schemas";

    /**
     * Creates COMMENT statements to document tables and columns that represent
     * application schema elements.
     */
    public static final String RULE_TGT_SQL_ALL_DOCUMENTATION_EXPLICIT_COMMENTS = "rule-sql-all-documentationViaExplicitCommentStatements";

    /* --------------------- */
    /* --- Tagged Values --- */
    /* --------------------- */

    public static final String TV_ASSOCIATIVETABLE = "associativeTable";

    /**
     * Name of the tagged value that overwrites the value of configuration parameter
     * {@value #PARAM_ONE_TO_MANY_REF_COLUMN_NAME} for a given datatype.
     */
    public static final String TV_ONE_TO_MANY_REF_COLUMN_NAME = "oneToManyReferenceColumnName";

    /**
     * Setting this tagged value on a code list indicates that the codes are
     * numeric. The tagged value contains the name of the conceptual type that
     * represents the code values best, for example 'Number' or 'Integer'. The SQL
     * data type will be determined by mapping that type using the map entries
     * defined in the configuration, resulting in a DBMS specific implementation of
     * the SQL data type.
     */
    public static final String TV_NUMERIC_TYPE = "numericType";
    
    public static final String TV_SQLSCHEMA = "sqlSchema";

    /* -------------------- */
    /* --- other fields --- */
    /* -------------------- */

    /**
     * <pre>
     * (name|documentation|alias|definition|description|example|legalBasis|dataCaptureStatement|primaryCode)(\(((columnName|size|columnDocumentation)=([^,;\)]|(?&lt;=\\)[,;\)])+)(;(columnName|size|columnDocumentation)=([^,;\)]|(?&lt;=\\)[,;\)])+)*\))?
     * </pre>
     */
    public static final String DESCRIPTORS_FOR_CODELIST_REGEX = "(name|documentation|alias|definition|description|example|legalBasis|dataCaptureStatement|primaryCode)(\\(((columnName|size|columnDocumentation)=([^,;\\)]|(?<=\\\\)[,;\\)])+)(;(columnName|size|columnDocumentation)=([^,;\\)]|(?<=\\\\)[,;\\)])+)*\\))?";

    public static final String DEFAULT_CODESTATUSCL_TYPE = "CodeStatusCL";
    public static final int DEFAULT_CODESTATUSCL_LENGTH = 50;
    public static final String DEFAULT_CODE_NAME_COLUMN_NAME = "name";
    public static final String DEFAULT_ID_COLUMN_NAME = "_id";
    public static final String DEFAULT_FOREIGN_KEY_COLUMN_SUFFIX = "";
    public static final String DEFAULT_FOREIGN_KEY_COLUMN_SUFFIX_DATATYPE = "";
    public static final String DEFAULT_PRIMARYKEY_SPEC = "NOT NULL PRIMARY KEY";
    public static final String DEFAULT_PRIMARYKEY_SPEC_CODELIST = "NOT NULL PRIMARY KEY";
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
     * specifies the value to represent the Boolean value 'true' in the mapping that
     * the parameter applies to. Default is TRUE.
     */
    public static final String ME_PARAM_DEFAULTVALUE_CHARACT_TRUE = "true";
    /**
     * Characteristic for the parameter {@value #ME_PARAM_DEFAULTVALUE} that
     * specifies the value to represent the Boolean value 'false' in the mapping
     * that the parameter applies to. Default is FALSE.
     */
    public static final String ME_PARAM_DEFAULTVALUE_CHARACT_FALSE = "false";
    /**
     * Characteristic for the parameter {@value #ME_PARAM_DEFAULTVALUE} that, if set
     * to 'true' (ignoring case) specifies that the default value shall be quoted.
     * Default is false. Typically, this characteristic is set to true in map
     * entries for types that map to a textual type, but it can also apply to date
     * types. It usually does not apply to numeric types.
     */
    public static final String ME_PARAM_DEFAULTVALUE_CHARACT_QUOTED = "quoted";

    /**
     * Name of the parameter to indicate (via the 'param' attribute) that a map
     * entry contains information about a geometry type.
     */
    public static final String ME_PARAM_GEOMETRY = "geometry";
    /**
     * Characteristic for the parameter {@value #ME_PARAM_GEOMETRY} that specifies
     * the dimension of the geometry in the mapping that the parameter applies to.
     * There is no default value for this characteristic.
     */
    public static final String ME_PARAM_GEOMETRY_CHARACT_DIMENSION = "dimension";
    /**
     * Name of the parameter to indicate (via the 'param' attribute) that the type
     * of a map entry is represented by a table.
     */
    public static final String ME_PARAM_TABLE = "table";
    /**
     * A characteristic for the parameter {@value #ME_PARAM_TABLE} that gives
     * information about the category of the conceptual type that is identified by
     * the map entry.
     *
     * Recognized values are (currently there is only one):
     * <ul>
     * <li>datatype</li>
     * </ul>
     */
    public static final String ME_PARAM_TABLE_CHARACT_REP_CAT = "representedCategory";
    /**
     * Regular expression to check that a given string is one of a list of allowed
     * values (NOTE: check is case-insensitive).
     */
    public static final String ME_PARAM_TABLE_CHARACT_REP_CAT_VALIDATION_REGEX = "(?i:(datatype|codelist))";
    public static final String ME_PARAM_TEXTORCHARACTERVARYING = "textOrCharacterVarying";

    /**
     * The target type can have a length. This is important for correctly parsing
     * the length from the targetType (more specifically, its parameterization). The
     * parameter is mutually exclusive with 'precision'.
     */
    public static final String ME_PARAM_LENGTH = "length";

    /**
     * A qualification for a textual data type with limited length.
     *
     * Recognized values for the Oracle database system are:
     * <ul>
     * <li>BYTE</li>
     * <li>CHAR</li>
     * </ul>
     */
    public static final String ME_PARAM_LENGTH_CHARACT_LENGTH_QUALIFIER = "lengthQualifier";

    /**
     * The target type can have precision. This is important for correctly parsing
     * the precision (and optional scale) from the targetType (more specifically,
     * its parameterization). The parameter is mutually exclusive with 'length'.
     */
    public static final String ME_PARAM_PRECISION = "precision";

    /*
     * MAP_TARGETTYPE_COND_PART and MAP_TARGETTYPE_COND_TEXTORCHARACTERVARYING are
     * kept for backwards compatibility
     */
    public static final String MAP_TARGETTYPE_COND_PART = "cond:";
    public static final String MAP_TARGETTYPE_COND_TEXTORCHARACTERVARYING = "textOrCharacterVarying";

    public static final String CRLF = Options.CRLF;
    public static final String INDENT = "   ";

    public static final String NOT_NULL_COLUMN_SPEC = "NOT NULL";

    /**
     * Regular expression to extract the data type name as well as length, precision
     * and scale from the target type defined by a map entry. Group 0 contains the
     * whole string, group 1 the data type name, group 2 the first number, and group
     * 3 the optional second number (which can be <code>null</code>).
     */
    public static final Pattern PATTERN_ME_TARGETTYPE_LENGTH_PRECISION_SCALE = Pattern
	    .compile("(.+)\\(([+-]?\\d+(?:\\.\\d)*)(?:,([+-]?\\d+(?:\\.\\d)*))?\\)");
}
