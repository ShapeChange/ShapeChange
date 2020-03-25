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
 * (c) 2002-2020 interactive instruments GmbH, Bonn, Germany
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

package de.interactive_instruments.ShapeChange.Target.JSON;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class JsonSchemaConstants {

    public static final String PLATFORM = "json";

    public static final String ME_PARAM_FORMATTED = "formatted";
    public static final String ME_PARAM_FORMATTED_CHAR_FORMAT = "format";
    public static final String ME_PARAM_GEOMETRY = "geometry";

    /**
     * Optional changes to the default documentation template and the default
     * strings for descriptors without value
     */
    public static final String PARAM_DOCUMENTATION_TEMPLATE = "documentationTemplate";
    public static final String PARAM_DOCUMENTATION_NOVALUE = "documentationNoValue";

    /**
     * True (the default) if the resulting json schemas shall be pretty printed,
     * else false.
     */
    public static final String PARAM_PRETTY_PRINT = "prettyPrint";

    /**
     * Define the version of the JSON Schema documents that shall be created by the
     * target. Available values are: "2019-09" (default) and "draft-07".
     */
    public static final String PARAM_JSON_SCHEMA_VERSION = "jsonSchemaVersion";

    public static final String PARAM_JSON_BASE_URI = "jsonBaseUri";

    public static final String PARAM_ENTITY_TYPE_NAME = "entityTypeName";

    public static final String PARAM_BY_REFERENCE_JSON_SCHEMA_DEFINITION = "byReferenceJsonSchemaDefinition";
    
    public static final String PARAM_BASE_JSON_SCHEMA_DEF_FEATURE_TYPES = "baseJsonSchemaDefinitionForFeatureTypes";
    public static final String PARAM_BASE_JSON_SCHEMA_DEF_OBJECT_TYPES = "baseJsonSchemaDefinitionForObjectTypes";
    public static final String PARAM_BASE_JSON_SCHEMA_DEF_DATA_TYPES = "baseJsonSchemaDefinitionForDataTypes";

    public static final String PARAM_OBJECT_IDENTIFIER_NAME = "objectIdentifierName";
    public static final String PARAM_OBJECT_IDENTIFIER_TYPE = "objectIdentifierType";
    public static final String PARAM_OBJECT_IDENTIFIER_REQUIRED = "objectIdentifierRequired";
    
    public static final String PARAM_INLINEORBYREF_DEFAULT = "inlineOrByReferenceDefault";
    
    public static final String RULE_ALL_DOCUMENTATION = "rule-json-all-documentation";
    public static final String RULE_ALL_NOT_ENCODED = "rule-json-all-notEncoded";

    public static final String RULE_CLS_BASIC_TYPE = "rule-json-cls-basictype";
    public static final String RULE_CLS_NAME_AS_ANCHOR = "rule-json-cls-name-as-anchor";
    public static final String RULE_CLS_NAME_AS_ENTITYTYPE = "rule-json-cls-name-as-entityType";
    public static final String RULE_CLS_GENERALIZATION = "rule-json-cls-generalization";
    public static final String RULE_CLS_SPECIALIZATION = "rule-json-cls-specialization";
    public static final String RULE_CLS_IDENTIFIER_FOR_TYPE_WITH_IDENTITY = "rule-json-cls-identifierForTypeWithIdentity";
    public static final String RULE_CLS_IDENTIFIER_STEREOTYPE = "rule-json-cls-identifierStereotype";
    public static final String RULE_CLS_IGNORE_IDENTIFIER = "rule-json-cls-ignoreIdentifier";
    public static final String RULE_CLS_VIRTUAL_GENERALIZATION = "rule-json-cls-virtualGeneralization";
    public static final String RULE_CLS_DEFAULT_GEOMETRY_SINGLEGEOMPROP = "rule-json-cls-defaultGeometry-singleGeometryProperty";
    public static final String RULE_CLS_DEFAULT_GEOMETRY_MULTIGEOMPROPS = "rule-json-cls-defaultGeometry-multipleGeometryProperties";
    public static final String RULE_CLS_NESTED_PROPERTIES = "rule-json-cls-nestedProperties";
    public static final String RULE_CLS_UNION_PROPERTY_COUNT = "rule-json-cls-union-propertyCount";
    public static final String RULE_CLS_UNION_TYPE_DISCRIMINATOR = "rule-json-cls-union-typeDiscriminator";
    public static final String RULE_CLS_CODELIST_URI_FORMAT = "rule-json-cls-codelist-uri-format";
    public static final String RULE_PROP_VOIDABLE = "rule-json-prop-voidable";
    public static final String RULE_PROP_READONLY = "rule-json-prop-readOnly";
    public static final String RULE_PROP_DERIVEDASREADONLY = "rule-json-prop-derivedAsReadOnly";
    public static final String RULE_PROP_INITIAL_VALUE_AS_DEFAULT = "rule-json-prop-initialValueAsDefault";

    public static final String RULE_CLS_VALUE_TYPE_OPTIONS = "rule-json-cls-valueTypeOptions";

}
