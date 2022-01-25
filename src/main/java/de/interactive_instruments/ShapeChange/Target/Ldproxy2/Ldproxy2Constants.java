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
 * (c) 2002-2022 interactive instruments GmbH, Bonn, Germany
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

package de.interactive_instruments.ShapeChange.Target.Ldproxy2;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class Ldproxy2Constants {

    public static final String PLATFORM = "ldp2";
    
    public static final long UNITTEST_UNIX_TIME = 1000000000000L;

    public static final String ME_PARAM_INITIAL_VALUE_ENCODING = "initialValueEncoding";
    public static final String ME_PARAM_INITIAL_VALUE_ENCODING_CHARACT_TRUE = "true";
    public static final String ME_PARAM_INITIAL_VALUE_ENCODING_CHARACT_FALSE = "false";
//    public static final String ME_PARAM_INITIAL_VALUE_ENCODING_CHARACT_QUOTED = "quoted";
    
    public static final String ME_PARAM_GEOMETRY_INFOS = "geometryInfos";
    public static final String ME_PARAM_GEOMETRY_INFOS_CHARACT_GEOMETRY_TYPE = "geometryType";
    
    public static final String ME_PARAM_LINK_INFOS = "linkInfos";
    public static final String ME_PARAM_LINK_INFOS_CHARACT_URL_TEMPLATE = "urlTemplate";
    
    public static final String ME_PARAM_IGNORE_FOR_TYPE_FROM_SEL_SCHEMA = "ignoreForTypeFromSchemaSelectedForProcessing";
    
    
    /**
     * Optional changes to the default documentation template and the default
     * strings for descriptors without value
     */
    public static final String PARAM_DOCUMENTATION_TEMPLATE = "documentationTemplate";
    public static final String PARAM_DOCUMENTATION_NOVALUE = "documentationNoValue";
    
    public static final String PARAM_ASSOC_TABLE_COLUMN_SUFFIX = "associativeTableColumnSuffix";
    public static final String PARAM_CFG_TEMPLATE_PATH = "cfgTemplatePath";
    public static final String PARAM_DATE_FORMAT = "dateFormat";
    public static final String PARAM_DATE_TIME_FORMAT = "dateTimeFormat";
    public static final String PARAM_DESCRIPTION_TEMPLATE = "descriptionTemplate";
    public static final String PARAM_DESCRIPTOR_NO_VALUE = "descriptorNoValue";
    public static final String PARAM_FORCE_AXIS_ORDER = "forceAxisOrder";
    public static final String PARAM_FK_COLUMN_SUFFIX = "foreignKeyColumnSuffix";
    public static final String PARAM_FK_COLUMN_SUFFIX_DATATYPE = "foreignKeyColumnSuffixDatatype";
    public static final String PARAM_LABEL_TEMPLATE = "labelTemplate";
    public static final String PARAM_MAX_NAME_LENGTH = "maxNameLength";
    public static final String PARAM_NATIVE_TIME_ZONE = "nativeTimeZone";
    public static final String PARAM_OBJECT_IDENTIFIER_NAME = "objectIdentifierName";
    public static final String PARAM_PK_COLUMN = "primaryKeyColumn";
    public static final String PARAM_SERVICE_API_TEMPLATE_PATH = "serviceApiTemplatePath";
    public static final String PARAM_SERVICE_DESCRIPTION = "serviceDescription";
    public static final String PARAM_SERVICE_LABEL = "serviceLabel";
    public static final String PARAM_SERVICE_METADATA_TEMPLATE_PATH = "serviceMetadataTemplatePath";
    public static final String PARAM_SRID = "srid";

    
    public static final String RULE_ALL_DOCUMENTATION = "rule-ldp2-all-documentation";
    public static final String RULE_ALL_NOT_ENCODED = "rule-ldp2-all-notEncoded";
    public static final String RULE_ALL_SCHEMAS = "rule-ldp2-all-schemas";
    public static final String RULE_CLS_CODELIST_DIRECT = "rule-ldp2-cls-codelist-direct";
    public static final String RULE_CLS_CODELIST_TARGETBYTV = "rule-ldp2-cls-codelist-targetbytaggedvalue";
    public static final String RULE_CLS_ENUMERATION_ENUM_CONSTRAINT = "rule-ldp2-cls-enumeration-enum-constraint";
    public static final String RULE_CLS_IDENTIFIER_STEREOTYPE = "rule-ldp2-cls-identifierStereotype";
    public static final String RULE_PROP_READONLY = "rule-ldp2-prop-readOnly";
   

}
