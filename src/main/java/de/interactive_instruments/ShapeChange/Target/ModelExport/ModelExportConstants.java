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
package de.interactive_instruments.ShapeChange.Target.ModelExport;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments <dot>
 *         de)
 *
 */
public class ModelExportConstants {

    /**
     * Alias: none
     * <p>
     * Required / Optional: required
     * <p>
     * Type: String (comma separated list of values)
     * <p>
     * Default Value: none
     * <p>
     * Explanation: Names of profiles to export
     * <p>
     * Applies to Rule(s): {@value #RULE_TGT_EXP_ALL_RESTRICT_EXISTING_PROFILES}
     */
    public static final String PARAM_PROFILES_TO_EXPORT = "profilesToExport";

    /**
     * Alias: none
     * <p>
     * Required / Optional: optional
     * <p>
     * Type: String (comma separated list of values)
     * <p>
     * Default Value: all profiles defined in the model
     * <p>
     * Explanation: Names of profiles that will be set for classes that do not
     * belong to a specific profile. This is relevant in case that the profiles are
     * not set explicitly in the model (parameter
     * {@value #PARAM_MODEL_EXPLICIT_PROFILES} is <code>false</code>) and if
     * {@value #RULE_TGT_EXP_ALL_OMIT_EXISTING_PROFILES} is not enabled.
     * <p>
     * Applies to Rule(s): none – default behavior
     */
    public static final String PARAM_PROFILES_FOR_CLASSES_WITHOUT_EXPLICIT_PROFILES = "profilesForClassesWithoutExplicitProfileAssignments";

    /**
     * Alias: none
     * <p>
     * Required / Optional: optional
     * <p>
     * Type: Boolean
     * <p>
     * Default Value: <code>true</code>
     * <p>
     * Explanation: Indicates if profile definitions in the input model are
     * explicitly set (<code>true</code>) or not (<code>false</code>). If they are
     * not, then profile inheritance would apply, which is converted during the
     * export (see parameter
     * {@value #PARAM_PROFILES_FOR_CLASSES_WITHOUT_EXPLICIT_PROFILES}) unless
     * {@value #RULE_TGT_EXP_ALL_OMIT_EXISTING_PROFILES} is enabled.
     * <p>
     * Applies to Rule(s): none – default behavior
     */
    public static final String PARAM_MODEL_EXPLICIT_PROFILES = "profilesInModelSetExplicitly";

    /**
     * Alias: none
     * <p>
     * Required / Optional: optional
     * <p>
     * Type: String
     * <p>
     * Default Value:
     * http://shapechange.net/resources/schema/ShapeChangeExportedModel.xsd
     * <p>
     * Explanation: The location of the XML Schema that shall be referenced by the
     * xsi:schemaLocation attribute, which will be added to the root of the
     * generated SCXML file. Note that the namespace, which is the first part of the
     * xsi:schemaLocation, will not be changed by this parameter. Only the schema
     * location is changed.
     * <p>
     * Applies to Rule(s): none – default behavior
     */
    public static final String PARAM_SCHEMA_LOCATION = "schemaLocation";
    public static final String DEFAULT_SCHEMA_LOCATION = "http://shapechange.net/resources/schema/ShapeChangeExportedModel.xsd";

    /**
     * Alias: none
     * <p>
     * Required / Optional: optional
     * <p>
     * Type: Boolean
     * <p>
     * Default Value: <code>false</code>
     * <p>
     * Explanation: Defines if the output should be compressed in a zip file (
     * <code>true</code>) or not (<code>false</code>).
     * <p>
     * Applies to Rule(s): none – default behavior
     */
    public static final String PARAM_ZIP_OUTPUT = "zipOutput";

    /**
     * By default, existing profiles are exported. With this rule, that behavior can
     * be changed to omit existing profiles. This rule has higher priority than
     * {@value #RULE_TGT_EXP_ALL_RESTRICT_EXISTING_PROFILES}.
     */
    public static final String RULE_TGT_EXP_ALL_OMIT_EXISTING_PROFILES = "rule-exp-all-omitExistingProfiles";

    /**
     * If this rule is included, descriptors of model elements will not be encoded.
     */
    public static final String RULE_TGT_EXP_ALL_OMIT_DESCRIPTORS = "rule-exp-all-omitDescriptors";

    /**
     * By default, existing profiles are exported. With this rule, that behavior can
     * be changed to restrict the set of profiles that are exported. The target
     * parameter {@value #PARAM_PROFILES_TO_EXPORT} contains a (comma-separated)
     * list of names of the profiles that shall be exported. This rule has lower
     * priority than {@value #RULE_TGT_EXP_ALL_OMIT_EXISTING_PROFILES}.
     */
    public static final String RULE_TGT_EXP_ALL_RESTRICT_EXISTING_PROFILES = "rule-exp-all-restrictExistingProfiles";

    /**
     * Alias: none
     * <p>
     * Required / Optional: optional
     * <p>
     * Type: String (with regular expression)
     * <p>
     * Default Value: (profiles)
     * <p>
     * Explanation: A tagged value that matches the regular expression defined by
     * this parameter will not be exported.
     * <p>
     * Applies to Rule(s): none – default behavior
     */
    public static final String PARAM_IGNORE_TAGGED_VALUES_REGEX = "ignoreTaggedValuesRegex";
    public static final String DEFAULT_IGNORE_TAGGED_VALUES_REGEX = "(profiles)";

    /**
     * Alias: none
     * <p>
     * Required / Optional: optional
     * <p>
     * Type: Boolean
     * <p>
     * Default Value: <code>false</code>
     * <p>
     * Explanation: By default, profiles are exported only for classes (and their
     * properties) from schemas that are selected for processing. If this parameter
     * is set to <code>true</code>, profiles are exported for all model classes (and
     * their properties).
     * <p>
     * Applies to Rule(s): none – default behavior
     */
    public static final String PARAM_EXPORT_PROFILES_FROM_WHOLE_MODEL = "exportProfilesFromWholeModel";

    /**
     * Alias: none
     * <p>
     * Required / Optional: optional
     * <p>
     * Type: Boolean
     * <p>
     * Default Value: <code>false</code>
     * <p>
     * Explanation: If <code>true</code>, descriptive information of OCL and FOL
     * constraints is encoded in &lt;description&gt; elements. If <code>false</code>
     * (the default behavior, for backwards-compatibility reasons), no
     * &lt;description&gt; elements will be created for constraints.
     * 
     * Applies to Rule(s): none – default behavior
     */
    public static final String PARAM_INCLUDE_CONSTRAINT_DESCRIPTIONS = "includeConstraintDescriptions";

    /**
     * By default, packages that do not belong to the schemas selected for
     * processing are marked as not editable. If this rule is enabled, all packages
     * are exported as editable.
     */
    public static final String RULE_TGT_EXP_PKG_ALL_EDITABLE = "rule-exp-pkg-allPackagesAreEditable";

    /**
     * By default, navigability of a property is encoded within the sc:isNavigable
     * element. The element is omitted if navigability is true (since that is the
     * reasonable default for attributes, which typically represent the bulk of
     * properties within an application schema). Thus, sc:isNavigable is typically
     * only set if navigability is false.
     * <p>
     * However: In UML 1, property navigability indicated property ownership. A
     * navigable association role was always owned by a class. In UML 2.4+, that
     * convention is deprecated. In UML 2.4+, ownership and navigability are
     * separate concepts, and navigability does not have much useful meaning
     * anymore. For UML 1 and UML 2 based schemas, where ownership is not explicitly
     * defined, ownership is derived from property navigability. Since the
     * sc:isNavigable element is optional, without a default value, SCXML supports
     * use cases where the schema explicitly models property ownership and
     * navigability is not used at all. However, ShapeChange encodes navigability by
     * default, and this default behavior supports use cases where ownership is
     * defined through navigability.
     * <p>
     * If this rule is included in the encoding rule, then isNavigable elements will
     * not be created, which supports creating SCXML for use cases where ownership
     * is modeled explicitly and not defined implicitly through navigability.
     */
    public static final String RULE_TGT_EXP_PROP_SUPPRESS_ISNAVIGABLE = "rule-exp-prop-suppressIsNavigable";

    /**
     * Alias: none
     * <p>
     * Required / Optional: optional
     * <p>
     * Type: Boolean
     * <p>
     * Default Value: <code>false</code>
     * <p>
     * Explanation: If <code>true</code>, then the following property
     * characteristics will not be encoded for codes/enums, because they do not have
     * semantic meaning (for a code/enum): isOrdered, isUnique, isAggregation,
     * isComposition, isOwned.
     * 
     * Applies to Rule(s): none – default behavior
     */
    public static final String PARAM_SUPPRESS_MEANINGLESS_CODE_ENUM_CHARACTERISTICS = "suppressCodeAndEnumCharacteristicsWithoutSemanticMeaning";
}
