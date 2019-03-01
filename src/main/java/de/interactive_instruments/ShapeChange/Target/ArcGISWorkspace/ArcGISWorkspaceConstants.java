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
 * (c) 2002-2018 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target.ArcGISWorkspace;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class ArcGISWorkspaceConstants {
	
	/**
	 * If this rule is enabled, the initial value for a <<DomainCodedValue>>,
	 * which is an attribute of a <<CodedValueDomain>> that results from
	 * conversion of enumerations and code lists from the application schema, is
	 * taken from the alias of the respective enums and codes, rather than from
	 * the initial value defined in the application schema.
	 */
	public static final String RULE_ENUM_INITIAL_VALUE_BY_ALIAS = "rule-arcgis-prop-initialValueByAlias";

	/**
	 * Enables use of stereotype &lt;&lt;identifier>> on class attributes. If an
	 * attribute with that stereotype belongs to a class, then it will be used
	 * as primary key (the OBJECTID field will still be generated).
	 * 
	 * NOTE: Multiple <<identifier>> attributes per class are not supported. In
	 * such a case, ShapeChange will log a warning and use only one of them as
	 * primary key. If the maximum multiplicity of an <<identifier>> attribute
	 * is greater than 1, ShapeChange will log an error.
	 */
	public static final String RULE_CLS_IDENTIFIER_STEREOTYPE = "rule-arcgis-cls-identifierStereotype";

	/**
	 * If a feature type has the tagged value 'HasZ' set to 'true', and the
	 * feature type is converted to an ArcGIS feature class (Point, Polyline,
	 * etc.), then with this rule enabled the ArcGIS feature class will have the
	 * tagged value 'HasZ' set to 'true' (default is 'false').
	 */
	public static final String RULE_CLS_HASZ = "rule-arcgis-cls-hasZ";

	/**
	 * If a feature type has the tagged value 'HasM' set to 'true', and the
	 * feature type is converted to an ArcGIS feature class (Point, Polyline,
	 * etc.), then with this rule enabled the ArcGIS feature class will have the
	 * tagged value 'HasM' set to 'true' (default is 'false').
	 */
	public static final String RULE_CLS_HASM = "rule-arcgis-cls-hasM";

	/**
	 * Identifies range domains for class properties based upon the tagged
	 * values 'rangeMinimum' and 'rangeMaximum'. Each boundary is inclusive. If
	 * one of the tagged value is not provided, the default value for that
	 * boundary is used. If both tagged values are empty, a range domain is not
	 * created. This rule overrides the range domain parsed from an OCL
	 * constraint, if the tagged values also specify a range domain for that
	 * property.
	 */
	public static final String RULE_CLS_RANGE_DOMAIN_FROM_TAGGED_VALUES = "rule-arcgis-cls-rangeDomainFromTaggedValues";

	/**
	 * If this rule is enabled, ShapeChange will use the value of the tagged
	 * value 'size' (must be an integer) to populate the ‘length’ tagged value
	 * of the &lt;&lt;field&gt;&gt; that will represent the property in the
	 * ArcGIS model. NOTE: Only applies to properties that are implemented as
	 * fields with type esriFieldTypeString. If the value is 0 or empty,
	 * unlimited length is assumed - unless an OCL constraint exists that
	 * restricts the length for the property. That also means that this rule has
	 * precedence over an OCL constraint: if the tagged value 'size' has an
	 * integer value > 1, then this value will be used as the length in the
	 * &lt;&lt;field&gt;&gt;.
	 */
	public static final String RULE_PROP_LENGTH_FROM_TAGGED_VALUE = "rule-arcgis-prop-lengthFromTaggedValue";

	public static final String RULE_ALL_POSTPROCESS_REMOVE_UNUSED_CODED_VALUE_DOMAINS = "rule-arcgis-all-postprocess-removeUnusedCodedValueDomains";

	/**
	 * If this rule is enabled, then - for properties with a code list or
	 * enumeration as value type - ShapeChange will use the value of the tagged
	 * value 'size' (must be an integer) to populate the ‘length’ tagged value
	 * of the &lt;&lt;field&gt;&gt; that will represent the property in the
	 * ArcGIS model. This rule has higher priority than
	 * {@value #RULE_PROP_LENGTH_FROM_CODES_OR_ENUMS_OF_VALUE_TYPE}. If none of
	 * these rules apply, the length will be set to 0.
	 */
	public static final String RULE_PROP_LENGTH_FROM_TAGGED_VALUE_FOR_CODELIST_OR_ENUMERATION_VALUE_TYPE = "rule-arcgis-prop-lengthFromTaggedValueForCodelistOrEnumerationValueType";

	/**
	 * If this rule is enabled then the length of a property that has a code
	 * list or enumeration as value type is computed as the maximum name length
	 * from the codes/enums of the value type (if codes/enums are defined by
	 * that type). This rule has lower priority than
	 * {@value #RULE_PROP_LENGTH_FROM_TAGGED_VALUE_FOR_CODELIST_OR_ENUMERATION_VALUE_TYPE}
	 * If none of these rules apply, the length will be set to 0.
	 */
	public static final String RULE_PROP_LENGTH_FROM_CODES_OR_ENUMS_OF_VALUE_TYPE = "rule-arcgis-prop-lengthFromCodesOrEnumsOfValueType";

	public static final String RULE_PROP_INITIAL_VALUE = "rule-arcgis-prop-initialValue";

	public static final String RULE_ALL_PRECISION = "rule-arcgis-all-precision";
	/**
	 * NOTE: This rule identifier is deprecated. Use
	 * {@value #RULE_ALL_PRECISION} instead.
	 */
	public static final String RULE_PROP_PRECISION = "rule-arcgis-prop-precision";
	
	public static final String RULE_ALL_SUBTYPES = "rule-arcgis-all-subtypes";

	public static final String RULE_ALL_SCALE = "rule-arcgis-all-scale";
	/**
	 * NOTE: This rule identifier is deprecated. Use {@value #RULE_ALL_SCALE}
	 * instead.
	 */
	public static final String RULE_PROP_SCALE = "rule-arcgis-prop-scale";

	public static final String RULE_PROP_ISNULLABLE = "rule-arcgis-prop-isNullable";

	public static final String RULE_PROP_ATTINDEX = "rule-arcgis-prop-attIndex";

	public static final String RULE_PROP_REFLEXIVE_AS_FIELD = "rule-arcgis-prop-reflexiveRelationshipAsField";

	/**
	 * If this rule is enabled, then the base name of a relationship class will
	 * be constructed from the short names of the source and target class,
	 * combined by an underscore. The short name of a class is given via the
	 * tagged value specified by parameter
	 * {@value #PARAM_SHORT_NAME_BY_TAGGED_VALUE}. If no short name is
	 * specified, the original class name will be used as fallback. Note that
	 * the base name can be subject to additional modifications (such as
	 * normalization, addition of suffix to make the name unique, and clipping
	 * in case that the name exceeds the allowed length).
	 */
	public static final String RULE_ALL_RELCLASSNAME_BY_TAGGEDVALUE_OF_CLASSES = "rule-arcgis-all-relationshipClassNameByTaggedValueOfClasses";

	public static final String RULE_ALL_REPRESENT_TAGGED_VALUES = "rule-arcgis-all-representTaggedValues";

	/* ------------------------------------------- */
	/* --- configuration parameter identifiers --- */
	/* ------------------------------------------- */

	/* --- parameters required for / available in default behavior --- */
	/**
	 * Optional (defaults to 255) - Default length to set in the 'length' tagged
	 * value of <<field>>s that have a textual value, in case that there is no
	 * OCL constraint that defines the length.
	 */
	public static final String PARAM_LENGTH_TAGGED_VALUE_DEFAULT = "defaultLength";

	/**
	 * Optional (defaults to 'size') - Name of the tagged value that is used to
	 * determine the length of a &lt;&lt;field&gt;&gt; that represents a
	 * property under {@value #RULE_PROP_LENGTH_FROM_TAGGED_VALUE}.
	 */
	public static final String PARAM_NAME_OF_TV_TO_DETERMINE_FIELD_LENGTH = "nameOfTaggedValueToDetermineFieldLength";

	/**
	 * Optional (defaults to 0.01) - Delta to add to / subtract from a range
	 * limit in case that the lower and/or upper boundary comparison operator is
	 * not inclusive.
	 */
	public static final String PARAM_VALUE_RANGE_DELTA = "valueRangeExcludedBoundaryDelta";
	/**
	 * Optional (default is the current run directory) - The path to the folder
	 * in which the resulting ArcGIS workspace (UML) model will be created.
	 */
	public static final String PARAM_OUTPUT_DIR = "outputDirectory";
	/**
	 * Optional (defaults to "ArcGISWorkspace.eap") The name of the output file.
	 * ShapeChange will append the file extension '.eap' as suffix if the file
	 * name does not already contain it.
	 */
	public static final String PARAM_OUTPUT_FILENAME = "outputFilename";
	/**
	 * Optional (defaults to
	 * "http://shapechange.net/resources/templates/ArcGISWorkspace_template.eap"
	 * ) - Path to the ArcGIS workspace UML model template file (can be local or
	 * an online resource).
	 */
	public static final String PARAM_WORKSPACE_TEMPLATE = "workspaceTemplate";
	public static final String WORKSPACE_TEMPLATE_URL = "http://shapechange.net/resources/templates/ArcGISWorkspace_template.eap";
	/**
	 * Optional changes to the default documentation template and the default
	 * strings for descriptors without value
	 */
	public static final String PARAM_DOCUMENTATION_TEMPLATE = "documentationTemplate";
	public static final String PARAM_DOCUMENTATION_NOVALUE = "documentationNoValue";

	/**
	 * Suffix to append to the name of foreign keys. Default is 'ID'.
	 */
	public static final String PARAM_FOREIGN_KEY_SUFFIX = "foreignKeySuffix";

	public static final String PARAM_REFLEXIVE_REL_FIELD_SUFFIX = "reflexiveRelationshipFieldSuffix";

	/**
	 * If set to 'true', do not switch the first character of a target or source
	 * role name in a relationship class to lower case. Default is 'false'.
	 */
	public static final String PARAM_KEEP_CASE_OF_ROLENAME = "keepCaseOfRoleName";

	public static final String PARAM_MAX_NAME_LENGTH = "maxNameLength";

	/**
	 * Name of the tagged value that provides the short name for a model
	 * element, when used in constructing specific names of the ArcGIS
	 * workspace. Default is 'shortName'.
	 */
	public static final String PARAM_SHORT_NAME_BY_TAGGED_VALUE = "shortNameByTaggedValue";

	/* --------------------------------------------------------------- */
	/* --- Constants for elements of the ArcGIS workspace template --- */
	/* --------------------------------------------------------------- */

	public static final String TEMPLATE_PKG_FEATURES_NAME = "Features";
	public static final String TEMPLATE_PKG_DOMAINS_NAME = "Domains";
	public static final String TEMPLATE_PKG_TABLES_NAME = "Tables";
	public static final String TEMPLATE_PKG_ASSOCIATION_CLASSES_NAME = "Association Classes";

	/* ------------------------------------ */
	/* --- ArcGIS Workspace Stereotypes --- */
	/* ------------------------------------ */

	public static final String STEREOTYPE_RELATIONSHIP_CLASS = "ArcGIS::RelationshipClass";

	public static final String STEREOTYPE_DOMAIN_CODED_VALUE = "ArcGIS::DomainCodedValue";

	/* ----------------------- */
	/* --- Other constants --- */
	/* ----------------------- */

	public static final int DEFAULT_MAX_NAME_LENGTH = 30;

	public static final double NUM_RANGE_DELTA = 0.01;

	public static final Double DEFAULT_NUM_RANGE_MIN_LOWER_BOUNDARY = Double.valueOf(
			-1000000000);
	public static final Double DEFAULT_NUM_RANGE_MAX_UPPER_BOUNDARY = Double.valueOf(
			1000000000);

	public static final int LENGTH_TAGGED_VALUE_DEFAULT = 255;

	public static final String ILLEGAL_NAME_CHARACTERS_DETECTION_REGEX = "\\W";

	/**
	 * Setting this tagged value on a code list or enumeration indicates that
	 * the codes are numeric. The tagged value contains the name of the
	 * conceptual type that represents the code values best, for example
	 * 'Number' or 'Integer'. The ArcGIS data type will be determined by mapping
	 * that type using the map entries defined in the configuration.
	 * <p>
	 * NOTE: The field type determined by processing this tagged value will be
	 * overridden if tagged value {@value #TV_FIELD_TYPE} is also set on the
	 * code list / enumeration.
	 */
	public static final String TV_NUMERIC_TYPE = "numericType";

	public static final String TV_FIELD_TYPE = "fieldType";
}
