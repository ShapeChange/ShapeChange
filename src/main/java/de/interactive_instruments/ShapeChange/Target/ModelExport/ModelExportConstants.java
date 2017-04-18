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
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class ModelExportConstants {

	/**
	 * Comma-separated list of names of profiles to export.
	 * <p>
	 * default: none
	 * <p>
	 * applies to {@value #RULE_TGT_EXP_ALL_RESTRICT_EXISTING_PROFILES}
	 */
	public static final String PARAM_PROFILES_TO_EXPORT = "profilesToExport";

//	/**
//	 * Regular expression to match the name of schemas in which the profile
//	 * definition shall be converted to explicit definitions. By default, i.e.
//	 * if this parameter is not provided, the conversion will be applied to all
//	 * classes and properties whose profiles will be exported (so by default the
//	 * model elements belonging to schemas selected for processing, or the whole
//	 * model if {@value #RULE_TGT_EXP_ALL_EXPORT_PROFILES_FROM_WHOLE_MODEL} is
//	 * enabled). This parameter is only relevant if parameter
//	 * {@value #PARAM_MODEL_EXPLICIT_PROFILES} is <code>false</code> and if
//	 * {@value #RULE_TGT_EXP_ALL_OMIT_EXISTING_PROFILES} is not enabled.
//	 * <p>
//	 * Applies to rule: none - default behavior
//	 */
//	public static final String PARAM_CONVERT_TO_EXPLICIT_PROFILE_DEF_SCHEMA_NAME_REGEX = "convertToExplicitProfileDefinition_schemaNameRegex";

	/**
	 * Comma-separated list of names of profiles that will be set for classes
	 * that do not belong to a specific profile. This is relevant in case that
	 * the profiles are not set explicitly in the model (parameter
	 * {@value #PARAM_MODEL_EXPLICIT_PROFILES} is <code>false</code>) and if
	 * {@value #RULE_TGT_EXP_ALL_OMIT_EXISTING_PROFILES} is not enabled.
	 * <p>
	 * Default: all profiles defined in the model
	 * <p>
	 * Applies to rule: none - default behavior
	 */
	public static final String PARAM_PROFILES_FOR_CLASSES_WITHOUT_EXPLICIT_PROFILES = "profilesForClassesWithoutExplicitProfileAssignments";

	/**
	 * Indicates if profile definitions in the input model are explicitly set (
	 * <code>true</code>) or not (<code>false</code>). If they are not, then
	 * profile inheritance would apply, which is converted during the export
	 * (see parameter
	 * {@value #PARAM_CONVERT_TO_EXPLICIT_PROFILE_DEF_SCHEMA_NAME_REGEX}) unless
	 * {@value #RULE_TGT_EXP_ALL_OMIT_EXISTING_PROFILES} is enabled.
	 * <p>
	 * default: true
	 * <p>
	 * applies to conversion rule: none - default behavior
	 */
	public static final String PARAM_MODEL_EXPLICIT_PROFILES = "profilesInModelSetExplicitly";

	/**
	 * Defines if the output should be compressed in a zip file (
	 * <code>true</code>) or not (<code>false</code>).
	 * <p>
	 * default: false
	 * <p>
	 * applies to conversion rule: none - default behavior
	 */
	public static final String PARAM_ZIP_OUTPUT = "zipOutput";

	/**
	 * By default, existing profiles are exported. With this rule, that behavior
	 * can be changed to omit existing profiles. This rule has higher priority
	 * than {@link #RULE_TGT_EXP_ALL_RESTRICT_EXISTING_PROFILES}.
	 */
	public static final String RULE_TGT_EXP_ALL_OMIT_EXISTING_PROFILES = "rule-exp-all-omitExistingProfiles";

	/**
	 * By default, existing profiles are exported. With this rule, that behavior
	 * can be changed to restrict the set of profiles that are exported. The
	 * target parameter {@value #PARAM_PROFILES_TO_EXPORT} contains a
	 * (comma-separated) list of names of the profiles that shall be exported.
	 * This rule has lower priority than
	 * {@value #RULE_TGT_EXP_ALL_OMIT_EXISTING_PROFILES}.
	 */
	public static final String RULE_TGT_EXP_ALL_RESTRICT_EXISTING_PROFILES = "rule-exp-all-restrictExistingProfiles";

	/**
	 * If this rule is enabled, then the tagged value 'profiles' will be removed
	 * on exported classes and properties.
	 */
	public static final String RULE_TGT_EXP_ALL_IGNORE_PROFILES_TAGGED_VALUE = "rule-exp-all-ignoreProfilesTaggedValue";

	/**
	 * By default, profiles are exported only for classes (and their properties)
	 * from schemas that are selected for processing. If this rule is enabled,
	 * profiles are exported for all model classes (and their properties).
	 */
	public static final String RULE_TGT_EXP_ALL_EXPORT_PROFILES_FROM_WHOLE_MODEL = "rule-exp-all-exportProfilesFromWholeModel";

	/**
	 * By default, packages that do not belong to the schemas selected for
	 * processing are marked as not editable. If this rule is enabled, all
	 * packages are exported as editable.
	 */
	public static final String RULE_TGT_EXP_PKG_ALL_EDITABLE = "rule-exp-pkg-allPackagesAreEditable";

}
