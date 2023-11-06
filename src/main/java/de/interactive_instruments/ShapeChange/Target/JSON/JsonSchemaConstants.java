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
    
    public static final String SC_LINK_OBJECT_DEF_NAME = "SCLinkObject";
    
    public static final String TV_COLLECTION_URI_TEMPLATE = "collectionUriTemplate";
    public static final String TV_SUPERTYPES_ENCODING_ORDER = "jsonSupertypesEncodingOrder";

    public static final String PLATFORM = "json";

    public static final String ME_PARAM_KEYWORDS = "keywords";
    public static final String ME_PARAM_GEOMETRY = "geometry";
    public static final String ME_PARAM_IGNORE_FOR_TYPE_FROM_SEL_SCHEMA = "ignoreForTypeFromSchemaSelectedForProcessing";
    public static final String ME_PARAM_MEASURE = "measure";
    public static final String ME_PARAM_ENCODING_INFOS = "encodingInfos";
    public static final String ME_PARAM_ENCODING_INFOS_CHAR_ENTITY_TYPE_MEMBER_PATH = "entityTypeMemberPath";
    public static final String ME_PARAM_ENCODING_INFOS_CHAR_ENTITY_TYPE_MEMBER_REQUIRED = "entityTypeMemberRequired";
    public static final String ME_PARAM_ENCODING_INFOS_CHAR_ID_MEMBER_PATH = "idMemberPath";
    public static final String ME_PARAM_ENCODING_INFOS_CHAR_ID_MEMBER_REQUIRED = "idMemberRequired";
    public static final String ME_PARAM_ENCODING_INFOS_CHAR_ID_MEMBER_TYPES = "idMemberTypes";
    public static final String ME_PARAM_ENCODING_INFOS_CHAR_ID_MEMBER_FORMATS = "idMemberFormats";
    public static final String ME_PARAM_COLLECTION_INFOS = "collectionInfos";
    public static final String ME_PARAM_COLLECTION_INFOS_CHAR_URI_TEMPLATE = "uriTemplate";
    public static final String ME_PARAM_COLLECTION_INFOS_CHAR_COLLECTION_IDS = "collectionIds";
    public static final String ME_PARAM_COLLECTION_INFOS_CHAR_COLLECTION_ID_TYPES = "collectionIdTypes";
    

    public static final String PARAM_USE_ANCHOR_IN_LINKS_TO_GEN_SCHEMA_DEFS = "useAnchorsInLinksToGeneratedSchemaDefinitions";
    /**
     * Optional changes to the default documentation template and the default
     * strings for descriptors without value
     */
    public static final String PARAM_DOCUMENTATION_TEMPLATE = "documentationTemplate";
    public static final String PARAM_DOCUMENTATION_NOVALUE = "documentationNoValue";

    public static final String PARAM_FEATURE_COLLECTION_ONLY = "featureCollectionOnly";

    /**
     * True (the default) if the resulting json schemas shall be pretty printed,
     * else false.
     */
    public static final String PARAM_PRETTY_PRINT = "prettyPrint";

    public static final String PARAM_WRITE_MAP_ENTRIES = "writeMapEntries";

    /**
     * Define the version of the JSON Schema documents that shall be created by the
     * target. Available values are: "2019-09" (default) and "draft-07".
     */
    public static final String PARAM_JSON_SCHEMA_VERSION = "jsonSchemaVersion";

    public static final String PARAM_JSON_BASE_URI = "jsonBaseUri";

    public static final String PARAM_ENTITY_TYPE_NAME = "entityTypeName";

    public static final String PARAM_BY_REFERENCE_JSON_SCHEMA_DEFINITION = "byReferenceJsonSchemaDefinition";
    public static final String PARAM_BY_REFERENCE_FORMAT = "byReferenceFormat";

    public static final String PARAM_BASE_JSON_SCHEMA_DEF_COLLECTIONS = "baseJsonSchemaDefinitionForCollections";
    public static final String PARAM_BASE_JSON_SCHEMA_DEF_FEATURE_TYPES = "baseJsonSchemaDefinitionForFeatureTypes";
    public static final String PARAM_BASE_JSON_SCHEMA_DEF_OBJECT_TYPES = "baseJsonSchemaDefinitionForObjectTypes";
    public static final String PARAM_BASE_JSON_SCHEMA_DEF_DATA_TYPES = "baseJsonSchemaDefinitionForDataTypes";

    public static final String PARAM_BASE_JSON_SCHEMA_DEF_FEATURE_TYPES_ENCODING_INFOS = "baseJsonSchemaDefinitionForFeatureTypes_encodingInfos";
    public static final String PARAM_BASE_JSON_SCHEMA_DEF_OBJECT_TYPES_ENCODING_INFOS = "baseJsonSchemaDefinitionForObjectTypes_encodingInfos";
    public static final String PARAM_BASE_JSON_SCHEMA_DEF_DATA_TYPES_ENCODING_INFOS = "baseJsonSchemaDefinitionForDataTypes_encodingInfos";

    public static final String PARAM_COLLECTION_SCHEMA_FILE_NAME = "collectionSchemaFileName";

    public static final String PARAM_OBJECT_IDENTIFIER_NAME = "objectIdentifierName";
    public static final String PARAM_OBJECT_IDENTIFIER_TYPE = "objectIdentifierType";
    public static final String PARAM_OBJECT_IDENTIFIER_REQUIRED = "objectIdentifierRequired";

    public static final String PARAM_PREVENT_UNKNOWN_TYPES_IN_FEATURE_COLLECTIONS = "preventUnknownTypesInFeatureCollection";

    public static final String PARAM_ID_MEMBER_ENCODING_RESTRICTIONS = "idMemberEncodingRestrictions";

    public static final String PARAM_INLINEORBYREF_DEFAULT = "inlineOrByReferenceDefault";

    public static final String PARAM_LINK_OBJECT_URI = "linkObjectUri";
    public static final String PARAM_MEASURE_OBJECT_URI = "measureObjectUri";

    /**
     * NOTE: Undocumented right now; background: separate property definitions could
     * be used in ldproxy provider configurations, but it was decided to create the
     * full provider config instead, since it provides a better level of control,
     * especially for GML encoding (location of the primary geometry element).
     */
    public static final String PARAM_CREATE_SEPARATE_PROPERTY_DEFINITIONS = "createSeparatePropertyDefinitions";
    public static final String PARAM_GEOJSON_COMPATIBLE_GEOMETRY_TYPES = "geoJsonCompatibleGeometryTypes";
    
    public static final String PARAM_FEATURE_REF_ID_TYPES = "featureRefIdTypes";
    public static final String PARAM_FEATURE_REF_PROFILES = "featureRefProfiles";
    public static final String PARAM_FEATURE_REF_ANY_COLLECTION_ID = "featureRefWithAnyCollectionId";

    public static final String RULE_ALL_DOCUMENTATION = "rule-json-all-documentation";
    public static final String RULE_CLS_DOCUMENTATION_ENUM_DESCRIPTION = "rule-json-cls-documentation-enumDescription";

    public static final String RULE_ALL_FEATURE_REFS = "rule-json-all-featureRefs";
    public static final String RULE_ALL_NOT_ENCODED = "rule-json-all-notEncoded";

    public static final String RULE_CLS_BASIC_TYPE = "rule-json-cls-basictype";
    public static final String RULE_CLS_NAME_AS_ANCHOR = "rule-json-cls-name-as-anchor";
    public static final String RULE_CLS_NAME_AS_ENTITYTYPE = "rule-json-cls-name-as-entityType";
    public static final String RULE_CLS_NAME_AS_ENTITYTYPE_DATATYPE = "rule-json-cls-name-as-entityType-dataType";
    public static final String RULE_CLS_NAME_AS_ENTITYTYPE_UNION = "rule-json-cls-name-as-entityType-union";
    // Specialization not implemented yet; need for it unclear at the moment
//    public static final String RULE_CLS_SPECIALIZATION = "rule-json-cls-specialization";
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
    public static final String RULE_CLS_CODELIST_LINK = "rule-json-cls-codelist-link";
    public static final String RULE_CLS_PRIMARY_GEOMETRY = "rule-json-cls-primaryGeometry";
    public static final String RULE_CLS_PRIMARY_PLACE = "rule-json-cls-primaryPlace";
    public static final String RULE_CLS_PRIMARY_TIME = "rule-json-cls-primaryTime";
    public static final String RULE_PROP_VOIDABLE = "rule-json-prop-voidable";
    public static final String RULE_PROP_READONLY = "rule-json-prop-readOnly";
    public static final String RULE_PROP_DERIVEDASREADONLY = "rule-json-prop-derivedAsReadOnly";
    public static final String RULE_PROP_INITIAL_VALUE_AS_DEFAULT = "rule-json-prop-initialValueAsDefault";
    public static final String RULE_PROP_INLINEORBYREFTAG = "rule-json-prop-inlineOrByReferenceTag";
    public static final String RULE_PROP_MEASURE = "rule-json-prop-measure";

    public static final String RULE_CLS_VALUE_TYPE_OPTIONS = "rule-json-cls-valueTypeOptions";
    public static final String RULE_CLS_RESTRICT_EXT_ENTITY_TYPE_MEMBER = "rule-json-cls-restrictExternalEntityTypeMember";
    public static final String RULE_CLS_RESTRICT_EXT_ID_MEMBER = "rule-json-cls-restrictExternalIdentifierMember";

    public static final String RULE_CLS_COLLECTIONS_BASED_ON_ENTITY_TYPE = "rule-json-cls-collectionsBasedOnEntityType";
    public static final String RULE_CLS_COLLECTIONS_WITH_TOP_LEVEL_ENTITY_TYPE = "rule-json-cls-collectionsWithTopLevelEntityType";

    public static final String RULE_CLS_JSON_FG_GEOMETRY = "rule-json-cls-jsonFgGeometry";

}
