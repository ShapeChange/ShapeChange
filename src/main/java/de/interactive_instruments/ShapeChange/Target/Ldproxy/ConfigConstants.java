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
package de.interactive_instruments.ShapeChange.Target.Ldproxy;

/**
 * @author Clemens Portele (portele at interactive-instruments dot de)
 *
 */
public class ConfigConstants {

	/**
	 * Alias: none
	 * <p>
	 * Required / Optional: optional
	 * <p>
	 * Type: String
	 * <p>
	 * Default Value: the current directory
	 * <p>
	 * Explanation: The directory where the configuration files will be created.
	 * <p>
	 * Applies to Rule(s): none – default behavior 
	 */
	public static final String PARAM_OUTPUT_DIRECTORY = "outputDirectory";

	/**
	 * Alias: none
	 * <p>
	 * Required / Optional: optional
	 * <p>
	 * Type: String (no whitespace etc.)
	 * <p>
	 * Default Value: name of the XML namespace prefix of a selected schema; or "fixme", if none is specified
	 * <p>
	 * Explanation: The id of this service. The id is used as the last step in the base 
	 * URI of the service.
	 * <p>
	 * Applies to Rule(s): none – default behavior 
	 */
	public static final String PARAM_SERVICE_ID = "serviceId";

	/**
	 * Alias: none
	 * <p>
	 * Required / Optional: optional
	 * <p>
	 * Type: String 
	 * <p>
	 * Default Value: Alias or name of a selected schema; or "Some Dataset", if none is specified
	 * <p>
	 * Explanation: Human readable label of this service
	 * <p>
	 * Applies to Rule(s): none – default behavior 
	 */
	public static final String PARAM_SERVICE_LABEL = "serviceLabel";

	/**
	 * Alias: none
	 * <p>
	 * Required / Optional: optional
	 * <p>
	 * Type: String
	 * <p>
	 * Default Value: Documentation or the definition of a selected schema; or an empty string, if none is specified
	 * <p>
	 * Explanation: Description of this service
	 * <p>
	 * Applies to Rule(s): none – default behavior 
	 */
	public static final String PARAM_SERVICE_DESC = "serviceDescription";

	/**
	 * Alias: none
	 * <p>
	 * Required / Optional: optional
	 * <p>
	 * Type: String
	 * <p>
	 * Default Value: 1.0.0
	 * <p>
	 * Explanation: Version of this service API
	 * <p>
	 * Applies to Rule(s): none – default behavior 
	 */
	public static final String PARAM_SERVICE_VERSION = "serviceVersion";

	/**
	 * Alias: none
	 * <p>
	 * Required / Optional: optional
	 * <p>
	 * Type: Boolean ("true" or "false")
	 * <p>
	 * Default Value: not secured
	 * <p>
	 * Explanation: If "true", the service is marked as secured.  
	 * <p>
	 * Applies to Rule(s): none – default behavior 
	 */
	public static final String PARAM_SECURED = "secured";

	/**
	 * Alias: none
	 * <p>
	 * Required / Optional: optional
	 * <p>
	 * Type: String
	 * <p>
	 * Default Value: "id"
	 * <p>
	 * Explanation: Name of the field in tables used for the primary key
	 * <p>
	 * Applies to Rule(s): {@value #RULE_TGT_LDP_CLS_ID_FIELD}
	 */
	public static final String PARAM_PRIMARY_KEY_FIELD = "primaryKeyField";

	/**
	 * Alias: none
	 * <p>
	 * Required / Optional: optional
	 * <p>
	 * Type: String
	 * <p>
	 * Default Value: "_id"
	 * <p>
	 * Explanation: Suffix of the foreign key field in tables
	 * <p>
	 * Applies to Rule(s): {@value #RULE_TGT_LDP_CLS_ID_FIELD}
	 */
	public static final String PARAM_FOREIGN_KEY_SUFFIX = "foreignKeySuffix";

	/**
	 * Alias: none
	 * <p>
	 * Required / Optional: optional
	 * <p>
	 * Type: Integer
	 * <p>
	 * Default Value: 60
	 * <p>
	 * Explanation: Maximum length of field names
	 * <p>
	 * Applies to Rule(s): {@value #RULE_TGT_LDP_ALL_NAMES_MAXLENGTH}
	 */
	public static final String PARAM_MAX_LENGTH = "fieldNameMaxLength";

	/**
	 * Alias: none
	 * <p>
	 * Required / Optional: optional
	 * <p>
	 * Type: String
	 * <p>
	 * Default Value: "geom", if there is a separate table for all geometries
	 * <p>
	 * Explanation: By default, it is assumed that geometry fields are part of the feature tables.
	 * However, if all geometries are in a separate table, specify the name of that table.
	 * <p>
	 * Applies to Rule(s): {@value #RULE_TGT_LDP_PROP_GEOMETRY_TABLE}
	 */
	public static final String PARAM_GEOMETRY_TABLE = "geometryTableName";

	/**
	 * Alias: none
	 * <p>
	 * Required / Optional: optional
	 * <p>
	 * Type: String
	 * <p>
	 * Default Value: "geom", if there is a separate table for all geometries
	 * <p>
	 * Explanation: By default, it is assume that geometry fields are part of the feature tables.
	 * However, if all geometries are in a separate table, specify the field name of the geometry 
	 * in that table. 
	 * <p>
	 * Applies to Rule(s): {@value #RULE_TGT_LDP_PROP_GEOMETRY_TABLE}
	 */
	public static final String PARAM_GEOMETRY_FIELD = "geometryFieldName";

	/**
	 * Alias: none
	 * <p>
	 * Required / Optional: optional
	 * <p>
	 * Type: String
	 * <p>
	 * Default Value: "{{class}}_2_{{property}}"
	 * <p>
	 * Explanation: By default, all intermediate tables capable of representing properties that could be
	 * n-to-m relations are called "{{class}}_2_{{property}}" where "class" is the name of the table
	 * representing objects that have the property and "property" would be the standard field name of the
	 * property according to the encoding rule.
	 * That table will include foreign keys to the identifier of the "class" table and the table that 
	 * represents the objects / data structures that are the values of the property.
	 * <p>
	 * Applies to Rule(s): none – default behavior
	 */
	public static final String PARAM_NTOM_TABLE_TEMPLATE = "templateNtoMTable";

	/**
	 * Alias: none
	 * <p>
	 * Required / Optional: optional
	 * <p>
	 * Type: String
	 * <p>
	 * Default Value: "{{class}}_{{property}}"
	 * <p>
	 * Explanation: By default, all dependent tables capable of representing properties that could be
	 * 1-to-n relations are called "{{class}}_{{property}}" where "class" is the name of the table
	 * representing objects that have the property and "property" would be the standard field name of the
	 * property according to the encoding rule.
	 * That table will include a foreign key to the identifier of the "class" table.
	 * <p>
	 * Applies to Rule(s): none – default behavior
	 */
	public static final String PARAM_1TON_TABLE_TEMPLATE = "template1toNTable";

	/**
	 * Alias: none
	 * <p>
	 * Required / Optional: optional
	 * <p>
	 * Type: String
	 * <p>
	 * Default Value: "root"
	 * <p>
	 * Explanation: By default, the collection to use as the target for a feature reference is determined by collection 
	 * the feature is associated with. However, for associations to an abstract (root) feature type, this is unclear.
	 * In that case, rule {@value #RULE_TGT_LDP_CLS_TABLE_PER_FT} is required and the name of a table that includes
	 * a row for each feature in the dataset must be provided.
	 * That table must include a field that has the collection name of the feature as its value. See 
	 * {@value #PARAM_ROOT_COLLECTION_FIELD}.
	 * For example, if the table name is "root" and the field name is "collection" then this parameter will be "root"
	 * and {@value #PARAM_ROOT_COLLECTION_FIELD} will be "collection". These are also the default values.
	 * <p>
	 * Applies to Rule(s): {@value #RULE_TGT_LDP_CLS_TABLE_PER_FT}
	 */
	public static final String PARAM_ROOT_FEATURE_TABLE = "rootFeatureTable";

	/**
	 * Alias: none
	 * <p>
	 * Required / Optional: optional
	 * <p>
	 * Type: String
	 * <p>
	 * Default Value: "collection"
	 * <p>
	 * Explanation: By default, the collection to use as the target for a feature reference is determined by collection 
	 * the feature is associated with. However, for associations to an abstract (root) feature type, this is unclear.
	 * In that case, rule {@value #RULE_TGT_LDP_CLS_TABLE_PER_FT} is required and the name of a table that includes
	 * a row for each feature in the dataset must be provided. See {@value #PARAM_ROOT_FEATURE_TABLE}.
	 * That table must include a field that has the collection name of the feature as its value. 
	 * For example, if the table name is "root" and the field name is "collection" then this parameter will be "collection"
	 * and {@value #PARAM_ROOT_FEATURE_TABLE} will be "root". These are also the default values.
	 * <p>
	 * Applies to Rule(s): {@value #RULE_TGT_LDP_CLS_TABLE_PER_FT}
	 */
	public static final String PARAM_ROOT_COLLECTION_FIELD = "rootCollectionField";

	/**
	 * Alias: none
	 * <p>
	 * Required / Optional: optional
	 * <p>
	 * Type: String
	 * <p>
	 * Default Value: none
	 * <p>
	 * Explanation: By default, only the geometry fields are set as "filterable". To mark other fields that are represented
	 * by a field (string, number or date) as filterable, specify them in a comma-separated list where each value uses the
	 * template "{{tablename}}.{{fieldname}}" where "tablename" is the name of the table that includes the filterable property
	 * (note that for nested data structures this is not the feature table) and "fieldname" is the name of the field. Example:
	 * "table1.field1,table2.field2". 
	 * <p>
	 * Applies to Rule(s): none - default behaviour
	 */
	public static final String PARAM_FILTERS = "filterableFields";

	/**
	 * Alias: none
	 * <p>
	 * Required / Optional: optional
	 * <p>
	 * Type: String
	 * <p>
	 * Default Value: primary key fields
	 * <p>
	 * Explanation: By default, the field that is the primary key is used as the label of a feature in HTML. To set the label
	 * for a feature type to another field, specify them in a comma-separated list where each value uses the
	 * template "{{tablename}}.{{fieldlabel}}" where "tablename" is the name of the feature table that includes the property
	 * and "fieldlabel" is the label of the field. Example: "table1.Field 1,table2.Field 2". Use "*" as the table name to
	 * select a field for all feature types.  Example: "*.Full Name".
	 * <p>
	 * Applies to Rule(s): none - default behaviour
	 */
	public static final String PARAM_HTML_LABEL = "htmlLabels";

	/**
	 * Alias: none
	 * <p>
	 * Required / Optional: optional
	 * <p>
	 * Type: String
	 * <p>
	 * Default Value: all feature types
	 * <p>
	 * Explanation: By default, the configuration is generated for all feature types. To reduce the configuration to a subset of 
	 * feature types, specify them in a comma-separated list. 
	 * <p>
	 * Applies to Rule(s): none - default behaviour
	 */
	public static final String PARAM_FEATURE_TYPES = "featureTypes";

	/**
	 * Alias: none
	 * <p>
	 * Required / Optional: optional
	 * <p>
	 * Type: String
	 * <p>
	 * Default Value: 'codeList'
	 * <p>
	 * Explanation: By default, code lists are accessed using the standard 'codeList' tagged value. To support other
	 * UML profiles, another tagged value may be specified that is not normalized to 'codeList'.
	 * <p>
	 * Applies to Rule(s): {@value #RULE_TGT_LDP_CLS_CODELIST}
	 */
	public static final String PARAM_CL_URL = "taggedValueForCodeListUrl";
	
	/**
	 * Alias: none
	 * <p>
	 * Required / Optional: optional
	 * <p>
	 * Type: String
	 * <p>
	 * Default Value: all properties
	 * <p>
	 * Explanation: By default, all properties are included in the output. To support publishing only a subset,
	 * only the properties are enabled in the configuration with a tagged value 'reportable' and one of the values
	 * in this parameter. Specify the values in a comma-separated list, e.g. 'true,internal'.
	 * <p>
	 * Applies to Rule(s): none - default behaviour
	 */
	public static final String PARAM_REPORTABLE = "enablePropertiesReportable";
	
	/**
	 * Alias: none
	 * <p>
	 * Required / Optional: optional
	 * <p>
	 * Type: String
	 * <p>
	 * Default Value: no onDelete trigger
	 * <p>
	 * Explanation: If a feature is deleted using the HTTP DELETE method, additional SQL statements can be 
	 * executed in the database. Example: "DELETE FROM othertable WHERE objectid={{id}}" where "{{id}}" is the
	 * value of the "id" column in the table of the deleted feature. Specify the values in a 
	 * semicolon-separated list.
	 * <p>
	 * Applies to Rule(s): none - default behaviour
	 */
	public static final String PARAM_TRIGGER_ONDELETE = "trigger_onDelete";
	
	/**
	 * This rule states that all properties with a value that is a data type are represented in the database
	 * using an intermediate table. The name of the table is determined by {@value #PARAM_NTOM_TABLE_TEMPLATE}.
	 */
	public static final String RULE_TGT_LDP_PROP_DT_AS_NTOM = "rule-ldp-prop-all-datatype-relations-as-n-to-m-relations";

	/**
	 * This rule states that all properties with a value that is a feature type are represented in the database
	 * using an intermediate table. The name of the table is determined by {@value #PARAM_NTOM_TABLE_TEMPLATE}.
	 */
	public static final String RULE_TGT_LDP_PROP_FT_AS_NTOM = "rule-ldp-prop-all-featuretype-relations-as-n-to-m-relations";

	/**
	 * This rule states that all properties with a code list value are represented in the database
	 * using a string field.
	 */
	public static final String RULE_TGT_LDP_PROP_CL_AS_STRING = "rule-ldp-prop-all-codelist-values-as-strings";

	/**
	 * This rule states that all properties with a value that is a simple value and a maximum multiplicity greater than 1
	 * are represented in the database using a separate table. The name of the table is determined by 
	 * {@value #PARAM_1TON_TABLE_TEMPLATE}.
	 */
	public static final String RULE_TGT_LDP_PROP_MV_AS_1TON = "rule-ldp-prop-multiple-single-values-as-1-to-n-relations";

	/**
	 * This rule states that all geometries are stored in a single geometry table (and not directly 
	 * in the feature tables).
	 */
	public static final String RULE_TGT_LDP_PROP_GEOMETRY_TABLE = "rule-ldp-prop-separate-geometry-table";

	/**
	 * This rule derives all table and field names as lower case from the model.
	 */
	public static final String RULE_TGT_LDP_ALL_NAMES_LOWERCASE = "rule-ldp-all-names-in-lowercase";

	/**
	 * This rule limits table and field names to n characters where n is set using {@value #PARAM_MAX_LENGTH}.
	 */
	public static final String RULE_TGT_LDP_ALL_NAMES_MAXLENGTH = "rule-ldp-all-names-max-length";

	/**
	 * This rule maps all non-abstract feature types to a collection.
	 */
	public static final String RULE_TGT_LDP_CLS_NAFT_AS_COLLECTION = "rule-ldp-cls-non-abstract-feature-types-as-collection";

	/**
	 * This rule states that all feature type classifiers are mapped to a separate table. I.e., for
	 * classes with superclasses, each of them will have a separate table. Each feature will have a row
	 * in each of those tables, all with the same identifier.
	 * Note that this rule is a requirement, if the schema includes feature associations that involve non-abstract
	 * feature types.
	 */
	public static final String RULE_TGT_LDP_CLS_TABLE_PER_FT = "rule-ldp-cls-table-per-feature-type";

	/**
	 * Create an id field for each feature or data type table
	 */
	public static final String RULE_TGT_LDP_CLS_ID_FIELD = "rule-ldp-cls-id-field";
	
	/**
	 * Try to process also the codelist values and add a codelist file to the configuration.
	 * The 'codeList' tagged value is taken by default, the value can be changed using {@value #PARAM_CL_URL}.
	 */
	public static final String RULE_TGT_LDP_CLS_CODELIST = "rule-ldp-cls-generate-codelist";
	
	/**
	 * Add oNeo metadata fields in feature tables. This assumes the following fields in each feature table:
	 * <ul>
	 * <li> erstelltvon character varying(255)</li>
	 * <li> erstelltam timestamp without time zone</li>
	 * <li> geaendertvon character varying(255)</li>
	 * <li> geaendertam timestamp without time zone</li>
	 * </ul>
	 */
	public static final String RULE_TGT_LDP_CLS_ONEO_METADATA = "rule-ldp-cls-oneo-metadata";
	
	public static final String PARAM_PRETTY_PRINT = "prettyPrint";

	// a "secret" override used to support unit tests
	public static final String PARAM_UNITTEST_OVERRIDE = "_unitTestOverride";
}
