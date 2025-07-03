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
 * (c) 2002-2025 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core.transformation.taggedvalues;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class TaggedValueTransformerConstants {


    /**
     * Comma-separated list of names of tagged values for which
     * {@value #RULE_TV_INHERITANCE} shall be applied. This parameter is required.
     */
    public static final String PARAM_TV_INHERITANCE_GENERAL_LIST = "taggedValueInheritanceGeneralList";
    /**
     * Comma-separated list of names of tagged values. If a subtype already has a
     * tagged value that would be copied from a supertype under
     * {@value #RULE_TV_INHERITANCE}, and that tagged value is contained in the
     * list, then the tagged value shall be overwritten in the subtype, rather than
     * being retained.
     * <p>
     * NOTE: Overwriting a tagged value has higher priority than appending (see
     * {@value #PARAM_TV_INHERITANCE_APPEND_LIST}). If a tagged value is listed for
     * both parameters {@value #PARAM_TV_INHERITANCE_OVERWRITE_LIST} and
     * {@value #PARAM_TV_INHERITANCE_APPEND_LIST} then it will be ignored in the
     * latter.
     */
    public static final String PARAM_TV_INHERITANCE_OVERWRITE_LIST = "taggedValueInheritanceOverwriteList";
    /**
     * Comma-separated list of names of tagged values. If a subtype already has a
     * tagged value that would be copied from a supertype under
     * {@value #RULE_TV_INHERITANCE}, and that tagged value is contained in the
     * list, then the value from the tagged value of the supertype shall be appended
     * to the value of the tagged value from the subtype, using the separator
     * defined by configuration parameter
     * {@value #PARAM_TV_INHERITANCE_APPEND_SEPARATOR}.
     * <p>
     * NOTE: Appending a tagged value has lower priority than overwriting (see
     * {@value #PARAM_TV_INHERITANCE_OVERWRITE_LIST}). If a tagged value is listed
     * for both parameters {@value #PARAM_TV_INHERITANCE_OVERWRITE_LIST} and
     * {@value #PARAM_TV_INHERITANCE_APPEND_LIST} then it will be ignored in the
     * latter.
     */
    public static final String PARAM_TV_INHERITANCE_APPEND_LIST = "taggedValueInheritanceAppendList";
    /**
     * Define the separator to use when a tagged value inherited from a supertype
     * under {@value #RULE_TV_INHERITANCE} shall be appended to the tagged value of
     * the subtype. Default value is
     * {@value #DEFAULT_TV_INHERITANCE_APPEND_SEPARATOR}.
     */
    public static final String PARAM_TV_INHERITANCE_APPEND_SEPARATOR = "taggedValueInheritanceAppendSeparator";
    public static final String DEFAULT_TV_INHERITANCE_APPEND_SEPARATOR = ", ";

    /**
     * Comma-separated list of names of tagged values to copy in
     * {@value #RULE_TV_COPY_FROM_VALUE_TYPE}. Default value is the empty string.
     */
    public static final String PARAM_TV_COPYFROMVALUETYPE_TVSTOCOPY = "taggedValuesToCopy";

    /**
     * Regular expression to match the name of value types from which to copy tagged
     * values in {@value #RULE_TV_COPY_FROM_VALUE_TYPE}. Default is '.*' - to match
     * any value type.
     */
    public static final String PARAM_TV_COPYFROMVALUETYPE_TYPENAMEREGEX = "valueTypeNameRegex";

    /**
     * Copies the tagged values specified via configuration parameter
     * {@value #PARAM_TV_INHERITANCE_GENERAL_LIST} from supertypes of the whole
     * model down to their subtypes, starting at the top of inheritance trees. If
     * the tagged value already exists in the subtype, then by default it is
     * retained. However, the value can also be overwritten and the two values can
     * be merged - for further details, see configuration parameters
     * {@value #PARAM_TV_INHERITANCE_OVERWRITE_LIST} and
     * {@value #PARAM_TV_INHERITANCE_APPEND_LIST}.
     * <p>
     * NOTE: Care should be taken in case that the model contains classes with
     * multiple supertypes.
     * <p>
     * NOTE: The implementation currently does not support tagged values with
     * multiple values.
     */
    public static final String RULE_TV_INHERITANCE = "rule-trf-taggedValue-inheritance";

    /**
     * Copy specific set of tagged values (specified via parameter
     * {@value #PARAM_TV_COPYFROMVALUETYPE_TVSTOCOPY}) from types (specified via
     * parameter {@value #PARAM_TV_COPYFROMVALUETYPE_TYPENAMEREGEX}) to properties
     * that have one of these types as value type. This can be useful for in case of
     * tagged values like 'length', 'rangeMinimum', 'rangeMaximum', and 'pattern'
     * that are defined on types (especially: basic types) rather than on
     * properties, and these types are mapped to other types (e.g.
     * 'CharacterString').
     */
    public static final String RULE_TV_COPY_FROM_VALUE_TYPE = "rule-trf-taggedValue-copyFromValueType";

    public static final String RULE_TV_CREATE_CLASSIFIER_NAMESPACE_TAGS = "rule-trf-taggedValue-createClassifierNamespaceTags";
    public static final String RULE_TV_CREATE_ORIGINAL_SCHEMA_INFO_TAGS = "rule-trf-taggedValue-createOriginalSchemaInformationTags";
    public static final String TV_ORIG_SCHEMA_NAME = "originalSchemaName";
    public static final String TV_ORIG_CLASS_NAME = "originalClassName";
    public static final String TV_ORIG_INCLASS_NAME = "originalInClassName";
    public static final String TV_ORIG_PROPERTY_NAME = "originalPropertyName";
    public static final String TV_ORIG_PROPERTY_MULTIPLICITY = "originalPropertyMultiplicity";
    public static final String TV_ORIG_PROPERTY_VALUETYPE = "originalPropertyValueType";

    public static final String RULE_TV_CREATE_PROPERTY_VALUE_TYPE_INFO_TAG = "rule-trf-taggedValue-createPropertyValueTypeInformationTag";
    public static final String PARAM_CREATEPROPERTYVALUETYPEINFO_TAGNAME = "propertyValueTypeInfoTagName";
    public static final String PARAM_CREATEPROPERTYVALUETYPEINFO_TAGNAME_DEFAULT = "propertyValueTypeName";

    public static final String RULE_TV_CREATE_REVERSE_PROPERTY_NAME_TAG = "rule-trf-taggedValue-createReversePropertyNameTag";
    
    public static final String RULE_TV_CREATE_ROLE_SOURE_OR_TARGET_TAG = "rule-trf-taggedValue-createAssociationRoleSourceOrTargetTag";
    
}
