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
package de.interactive_instruments.ShapeChange.Transformation.Profiling;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessRuleSet;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.TransformerConfiguration;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericClassInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericPropertyInfo;
import de.interactive_instruments.ShapeChange.ModelDiff.DiffElement;
import de.interactive_instruments.ShapeChange.ModelDiff.Differ;
import de.interactive_instruments.ShapeChange.ModelDiff.DiffElement.ElementType;
import de.interactive_instruments.ShapeChange.Profile.ModelProfileValidator;
import de.interactive_instruments.ShapeChange.Profile.ProfileUtil;
import de.interactive_instruments.ShapeChange.Profile.Profiles;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.Transformation.Transformer;
import name.fraser.neil.plaintext.diff_match_patch;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class ProfileLoader implements Transformer, MessageSource {

	public static final String RULE_PRE_DELETE_INPUT_MODEL_PROFILES = "rule-trf-profileLoader-preProcessing-deleteInputModelProfiles";
	public static final String RULE_PRE_VALIDATE_INPUT_MODEL_PROFILES = "rule-trf-profileLoader-preProcessing-validateInputModelProfiles";
	public static final String RULE_POST_VALIDATE_RESULTING_MODEL_PROFILES = "rule-trf-profileLoader-postProcessing-validateResultingModelProfiles";
	public static final String RULE_VALIDATE_LOADED_MODEL_PROFILES = "rule-trf-profileLoader-validateLoadedModelProfiles";
	public static final String RULE_DIFF_MODELS = "rule-trf-profileLoader-diffModels";
	/**
	 * By default, only the profiles of classes (and their properties) from
	 * schemas that are selected for processing are processed. If this rule is
	 * enabled, all model classes (and their properties) are processed.
	 */
	public static final String RULE_PROCESS_WHOLE_MODEL = "rule-trf-profileLoader-processWholeModel";
	// /**
	// * By default, non-explicit profiles are converted only for classes (and
	// * their properties) from schemas that are selected for processing. If
	// this
	// * rule is enabled, non-explicit profiles are converted for model classes
	// * (and their properties) in packages whose name matches the regular
	// * expression given by parameter
	// * {@value #PARAM_CONVERT_TO_EXPLICIT_PROFILE_DEF_SCHEMA_NAME_REGEX}. This
	// * rule is only relevant if parameter
	// * {@value #PARAM_INPUT_MODEL_EXPLICIT_PROFILES} is <code>false</code>.
	// */
	// public static final String RULE_CONVERT_PROFILES_IN_PKG_MATCHING_REGEX =
	// "rule-trf-profileLoader-convertNonExplicitProfilesInPackageThatMatchesRegex";

	/**
	 * Indicates if profile definitions in the input model are explicitly set (
	 * <code>true</code>) or not (<code>false</code>). If they are not, then
	 * profile inheritance would apply, which must be converted before loading
	 * (also see parameter
	 * {@value #PARAM_PROFILES_FOR_CLASSES_WITHOUT_EXPLICIT_PROFILES}).
	 * <p>
	 * default: true
	 * <p>
	 * applies to conversion rule: none - default behavior
	 */
	public static final String PARAM_INPUT_MODEL_EXPLICIT_PROFILES = "profilesInModelSetExplicitly";
	// /**
	// * Regular expression to match the name of schemas in which the profile
	// * definition shall be converted to explicit definitions
	// * {@value #PARAM_PROFILES_FOR_CLASSES_WITHOUT_EXPLICIT_PROFILES}.
	// * <p>
	// * Applies to rule: {@value #RULE_CONVERT_PROFILES_IN_PKG_MATCHING_REGEX}
	// */
	// public static final String
	// PARAM_CONVERT_TO_EXPLICIT_PROFILE_DEF_SCHEMA_NAME_REGEX =
	// "convertNonExplicitProfiles_schemaNameRegex";

	/**
	 * By default, the ProfileLoader loads all XML and ZIP files in the
	 * directory that is stated by the transformation parameter
	 * {@value #PARAM_LOAD_MODEL_DIRECTORY} (excluding subdirectories). That
	 * behavior can be changed to only use the files from that directory whose
	 * name matches the regular expression given by the parameter
	 * {@value #PARAM_LOAD_MODEL_FILE_REGEX}.
	 * <p>
	 * Default: .*(\.xml|\.zip)$
	 * <p>
	 * Applies to rule: none - default behavior
	 */
	public static final String PARAM_LOAD_MODEL_FILE_REGEX = "regexToFilterProfilesToLoad";

	/**
	 * Comma-separated list of names of profiles that will be set for input
	 * model classes that do not belong to a specific profile. This is relevant
	 * while pre-processing the input model in case that the profiles are not
	 * set explicitly in the input model (parameter
	 * {@value #PARAM_INPUT_MODEL_EXPLICIT_PROFILES} is <code>false</code>).
	 * <p>
	 * Default: all profiles defined in the input model
	 * <p>
	 * Applies to rule: none - default behavior
	 */
	public static final String PARAM_PROFILES_FOR_CLASSES_WITHOUT_EXPLICIT_PROFILES = "profilesForClassesWithoutExplicitProfileAssignments";

	/**
	 * Comma separated list of names of profiles to be loaded into the input
	 * model.
	 * <p>
	 * Default: all profiles
	 * <p>
	 * Applies to rule: none - default behavior
	 */
	public static final String PARAM_PROFILES_TO_LOAD = "profilesToLoad";

	// /**
	// * <code>true</code> if the validation of profiles for property types
	// shall
	// * only be conducted if the property type belongs to a schema that has
	// been
	// * selected for processing; else <code>false</code>.
	// * <p>
	// * Default: true
	// * <p>
	// * Applies to rule(s): {@value #RULE_PRE_VALIDATE_INPUT_MODEL_PROFILES}
	// and
	// * {@link #RULE_VALIDATE_LOADED_MODEL_PROFILES}
	// */
	// public static final String
	// PARAM_VALIDATE_PROFILES_PROPERTY_TYPE_ONLY_FROM_SELECTED_SCHEMAS =
	// "checkPropertyTypeConsistencyOnlyForPropertyTypeFromSelectedSchemas";

	public static final String PARAM_LOAD_MODEL_DIRECTORY = "directoryWithProfilesToLoad";

	/**
	 * Comma-separated list of names of diff element types. The diff result will
	 * only provide information on these types of differences (in addition to a
	 * possibly existing schema version difference).
	 * <p>
	 * The following diff element types are currently supported: NAME,
	 * DOCUMENTATION, MULTIPLICITY, VALUETYPE, CLASS, SUPERTYPE, SUBPACKAGE,
	 * PROPERTY, ENUM, STEREOTYPE, TAG, ALIAS, DEFINITION, DESCRIPTION,
	 * PRIMARYCODE, GLOBALIDENTIFIER, LEGALBASIS, AAAMODELLART,
	 * AAAGRUNDDATENBESTAND
	 * <p>
	 * Default: CLASS, ENUM, MULTIPLICITY, PROPERTY, NAME, STEREOTYPE,
	 * SUPERTYPE, SUBPACKAGE, VALUETYPE
	 * <p>
	 * Applies to rule: {@value #RULE_DIFF_MODELS}
	 */
	public static final String PARAM_DIFF_ELEMENT_TYPES = "diffElementTypes";
	public static final String[] DEFAULT_DIFF_ELEMENT_TYPES = new String[] {
			"CLASS", "ENUM", "MULTIPLICITY", "PROPERTY", "NAME", "STEREOTYPE",
			"SUPERTYPE", "SUBPACKAGE", "VALUETYPE" };

	private ShapeChangeResult result = null;
	private Options options = null;
	private GenericModel inputModel = null;
	private Set<String> rules = new HashSet<String>();

	@Override
	public void process(GenericModel m, Options o,
			TransformerConfiguration trfConfig, ShapeChangeResult r)
			throws ShapeChangeAbortException {

		this.result = r;
		this.options = o;
		this.inputModel = m;

		/*
		 * Load rules and transformation parameters
		 */

		Map<String, ProcessRuleSet> ruleSets = trfConfig.getRuleSets();

		if (ruleSets != null && !ruleSets.isEmpty()) {
			for (ProcessRuleSet ruleSet : ruleSets.values()) {
				if (ruleSet.getAdditionalRules() != null) {
					this.rules.addAll(ruleSet.getAdditionalRules());
				}
			}
		}

		boolean processWholeModel = rules.contains(RULE_PROCESS_WHOLE_MODEL);

		// boolean
		// checkPropertyTypeConsistencyOnlyForPropertyTypeFromSelectedSchemas =
		// trfConfig
		// .parameterAsBoolean(
		// PARAM_VALIDATE_PROFILES_PROPERTY_TYPE_ONLY_FROM_SELECTED_SCHEMAS,
		// true);

		SortedSet<String> profilesToLoad = null;
		if (trfConfig.hasParameter(PARAM_PROFILES_TO_LOAD)) {
			profilesToLoad = new TreeSet<String>(
					trfConfig.parameterAsStringList(PARAM_PROFILES_TO_LOAD,
							null, true, true));
		}

		boolean removeProfilesInInputModel = rules
				.contains(RULE_PRE_DELETE_INPUT_MODEL_PROFILES);

		boolean inputModelExplicitProfiles = trfConfig
				.parameterAsBoolean(PARAM_INPUT_MODEL_EXPLICIT_PROFILES, true);

		SortedSet<File> loadedProfileFiles = null;
		String loadedModelDirectory = trfConfig
				.getParameterValue(PARAM_LOAD_MODEL_DIRECTORY);

		Pattern loadModelFilePattern = null;

		if (trfConfig.hasParameter(ProfileLoader.PARAM_LOAD_MODEL_FILE_REGEX)) {

			try {
				loadModelFilePattern = Pattern
						.compile(trfConfig.parameterAsString(
								ProfileLoader.PARAM_LOAD_MODEL_FILE_REGEX, "",
								false, true));
			} catch (PatternSyntaxException e) {
				result.addError(this, 100,
						ProfileLoader.PARAM_LOAD_MODEL_FILE_REGEX,
						e.getMessage());
			}
		}

		File loadedModelDirectoryFile = new File(loadedModelDirectory);
		boolean exi = loadedModelDirectoryFile.exists();
		boolean dir = loadedModelDirectoryFile.isDirectory();
		boolean rea = loadedModelDirectoryFile.canRead();

		if (!exi || !dir || !rea) {

			result.addError(this, 101, ProfileLoader.PARAM_LOAD_MODEL_DIRECTORY,
					loadedModelDirectory);

		} else {

			if (loadModelFilePattern == null) {
				loadedProfileFiles = new TreeSet<File>(
						FileUtils.listFiles(loadedModelDirectoryFile,
								new String[] { "xml", "zip" }, false));
			} else {
				RegexFileFilter fileFilter = new RegexFileFilter(
						loadModelFilePattern);
				loadedProfileFiles = new TreeSet<File>(FileUtils
						.listFiles(loadedModelDirectoryFile, fileFilter, null));
			}

			if (loadedProfileFiles.isEmpty()) {

				result.addWarning(this, 102, loadedModelDirectory);
			}
		}

		List<String> diffElementTypeNames = trfConfig.parameterAsStringList(
				PARAM_DIFF_ELEMENT_TYPES, DEFAULT_DIFF_ELEMENT_TYPES, true,
				true);

		Set<ElementType> relevantDiffElementTypes = new HashSet<ElementType>();

		for (String detn : diffElementTypeNames) {

			try {

				ElementType et = ElementType
						.valueOf(detn.toUpperCase(Locale.ENGLISH));
				relevantDiffElementTypes.add(et);

			} catch (IllegalArgumentException e) {
				/*
				 * Reporting of illegal value elements is done by configuration
				 * validator; we simply ignore illegal values here.
				 */
			}
		}

		/*
		 * 0.1 Delete all profiles in whole input model
		 * 
		 * 0.2 Validate profile definitions of input model (whole model or
		 * selected schemas only) - irrelevant if input model profiles have been
		 * removed
		 * 
		 * 1. Convert profile definitions of input model to explicit, if
		 * necessary (profiles have not been deleted, and profiles in model not
		 * yet set explicitly). NOTE: There is no need to update profile
		 * definitions in models that are loaded, because they are already
		 * explicit.
		 * 
		 * 2. For each loaded model:
		 * 
		 * 2.1 Validate profile definitions in loaded model (in whole model or
		 * only selected schemas).
		 * 
		 * 2.2 Perform a model diff (of whole model or only selected schemas).
		 * Log relevant differences.
		 * 
		 * 2.3 Load profile definitions for whole model or only selected schemas
		 * (if requested, only load profiles defined via configuration;
		 * overwrite in input model).
		 * 
		 * 3. Validate profile definitions in resulting model (whole model or
		 * selected schemas only).
		 */

		if (removeProfilesInInputModel) {

			/*
			 * 0.1 Delete all profiles in input model
			 */
			result.addInfo(this, 112);

			ProfileUtil.removeProfiles(inputModel);

		} else {

			if (rules.contains(RULE_PRE_VALIDATE_INPUT_MODEL_PROFILES)) {

				/*
				 * 0.2 Validate profile definitions of input model
				 */
				result.addInfo(this, 111);

				ModelProfileValidator mpv = new ModelProfileValidator(
						inputModel, result);
				mpv.validateModelConsistency(inputModelExplicitProfiles, true,
						!processWholeModel);

				result.addInfo(this, 115);
			}

			if (!inputModelExplicitProfiles) {

				/*
				 * 1. Convert profile definitions of input model to explicit
				 */
				result.addInfo(this, 113);

				SortedSet<String> profilesForClassesWithoutExplicitProfiles = null;

				if (trfConfig.hasParameter(
						PARAM_PROFILES_FOR_CLASSES_WITHOUT_EXPLICIT_PROFILES)) {

					profilesForClassesWithoutExplicitProfiles = new TreeSet<String>(
							trfConfig.parameterAsStringList(
									PARAM_PROFILES_FOR_CLASSES_WITHOUT_EXPLICIT_PROFILES,
									null, true, true));

				} else {

					/*
					 * gather the names of all profiles defined in the input
					 * model
					 */
					profilesForClassesWithoutExplicitProfiles = ProfileUtil
							.findNamesOfAllProfiles(inputModel,
									processWholeModel);
				}

				Profiles profilesForClassesBelongingToAllProfiles = new Profiles();
				for (String profileName : profilesForClassesWithoutExplicitProfiles) {
					profilesForClassesBelongingToAllProfiles.put(profileName);
				}

				Pattern schemaNameRegex = null;

				// if
				// (rules.contains(RULE_CONVERT_PROFILES_IN_PKG_MATCHING_REGEX)
				// && !processWholeModel && trfConfig.hasParameter(
				// PARAM_CONVERT_TO_EXPLICIT_PROFILE_DEF_SCHEMA_NAME_REGEX)) {
				//
				// try {
				// schemaNameRegex = Pattern
				// .compile(trfConfig.parameterAsString(
				// PARAM_CONVERT_TO_EXPLICIT_PROFILE_DEF_SCHEMA_NAME_REGEX,
				// "", false, true));
				// } catch (PatternSyntaxException e) {
				// result.addError(this, 100,
				// PARAM_CONVERT_TO_EXPLICIT_PROFILE_DEF_SCHEMA_NAME_REGEX,
				// e.getMessage());
				// }
				// }

				/*
				 * Convert model to one with explicit profile definitions
				 */
				ProfileUtil.convertToExplicitProfileDefinitions(inputModel,
						profilesForClassesBelongingToAllProfiles,
						schemaNameRegex, processWholeModel);
			}
		}

		/*
		 * 2. For each loaded model:
		 * 
		 * 2.1 Validate profile definitions in loaded model (enable via rule).
		 * 
		 * 2.2 Perform a model diff (enable via rule). Log relevant differences.
		 * 
		 * TBD: Restrict diff (output) to schemas selected for processing?
		 * 
		 * 2.3 Load profile definitions (if requested, only load profiles
		 * defined via configuration; overwrite in input model).
		 */

		if (loadedProfileFiles != null && !loadedProfileFiles.isEmpty()) {

			/*
			 * Get all classes of the input model by their full name
			 */
			SortedMap<String, GenericClassInfo> inputModelClassesByFullNameInSchema = getClassesByFullNameInSchema(
					inputModel);

			// now process each model file with profiles to load
			for (File loadedProfileFile : loadedProfileFiles) {

				String loadedProfileFileLocation = loadedProfileFile
						.getAbsolutePath();

				try {

					result.addInfo(this, 114, loadedProfileFileLocation);

					GenericModel loadedModel = new GenericModel();
					loadedModel.initialise(result, options,
							loadedProfileFileLocation);

					/*
					 * postprocessing irrelevant, since we only need profile
					 * infos
					 */

					SortedMap<String, GenericClassInfo> loadedModelClassesByFullNameInSchema = getClassesByFullNameInSchema(
							loadedModel);

					if (rules.contains(RULE_VALIDATE_LOADED_MODEL_PROFILES)) {

						/*
						 * 2.1 Validate profile definitions in loaded model
						 */
						result.addInfo(this, 110, loadedProfileFileLocation);

						ModelProfileValidator mpv = new ModelProfileValidator(
								loadedModel, result);
						mpv.validateModelConsistency(true, true,
								!processWholeModel);

						result.addInfo(this, 116, loadedProfileFileLocation);
					}

					if (rules.contains(RULE_DIFF_MODELS)) {

						/*
						 * 2.2 Perform a model diff. Log relevant differences.
						 */

						/*
						 * TBD: move diff functionality to Differ package, for
						 * example for re-use in common transformation that
						 * performs model diffs?
						 */
						result.addInfo(this, 105, loadedProfileFileLocation);

						SortedSet<? extends PackageInfo> schemasToDiff = processWholeModel
								? inputModel.schemas(null)
								: inputModel.selectedSchemas();

						for (PackageInfo inputSchema : schemasToDiff) {

							SortedSet<PackageInfo> set = loadedModel
									.schemas(inputSchema.name());

							if (set.size() == 1) {

								// compute diffs
								Differ differ = new Differ();
								PackageInfo refSchema = set.iterator().next();

								// compare schema versions
								String inputSchemaVersion = inputSchema
										.version();
								if (inputSchemaVersion == null) {
									inputSchemaVersion = "";
								}
								String refSchemaVersion = refSchema.version();
								if (refSchemaVersion == null) {
									refSchemaVersion = "";
								}
								if (!inputSchemaVersion
										.equals(refSchemaVersion)) {
									result.addInfo(this, 107,
											inputSchema.fullName(),
											inputSchemaVersion,
											refSchemaVersion);

								}

								SortedMap<Info, SortedSet<DiffElement>> pi_diffs = differ
										.diff(inputSchema, refSchema);

								// log the diffs found for pi
								for (Entry<Info, SortedSet<DiffElement>> me : pi_diffs
										.entrySet()) {

									SortedSet<DiffElement> relevantDiffs = new TreeSet<DiffElement>();

									for (DiffElement diff : me.getValue()) {

										if (relevantDiffElementTypes.contains(
												diff.subElementType)) {
											relevantDiffs.add(diff);
										}
									}

									if (!relevantDiffs.isEmpty()) {

										MessageContext mc = result.addInfo(this,
												108,
												me.getKey().fullName().replace(
														inputSchema.fullName(),
														inputSchema.name()));

										for (DiffElement diff : relevantDiffs) {

											String s = diff.change + " "
													+ diff.subElementType;
											if (diff.subElementType == ElementType.TAG)
												s += "(" + diff.tag + ")";
											if (diff.subElement != null)
												s += " " + diff.subElement
														.name();
											else if (diff.diff != null)
												s += " " + diff.diff_from_to();
											else
												s += " ???";
											mc.addDetail(s);
										}
									}
								}
							} else {
								result.addWarning(this, 104,
										inputSchema.name());
							}
						}

						result.addInfo(this, 106, loadedProfileFileLocation);
					}

					/*
					 * 2.3 Load profile definitions (if requested, only load
					 * profiles defined via configuration; overwrite in input
					 * model).
					 */

					SortedSet<PackageInfo> loadedModelSelectedSchemaPkgs = loadedModel
							.allPackagesFromSelectedSchemas();

					for (Entry<String, GenericClassInfo> loadedCiEntry : loadedModelClassesByFullNameInSchema
							.entrySet()) {

						String loadedCiFullNameInSchema = loadedCiEntry
								.getKey();
						GenericClassInfo loadedCi = loadedCiEntry.getValue();

						/*
						 * If the whole model is not processed, and the loaded
						 * class is not contained in one of the packages of
						 * schemas selected for processing, continue.
						 */
						if (!rules.contains(RULE_PROCESS_WHOLE_MODEL)
								&& !loadedModelSelectedSchemaPkgs
										.contains(loadedCi.pkg())) {
							continue;
						}

						/*
						 * Check if loaded class contains at least one profile
						 * that may be transferred.
						 */
						if (loadedCi.profiles().isEmpty()
								|| (profilesToLoad != null && loadedCi
										.profiles().getProfiles(profilesToLoad)
										.isEmpty())) {
							/*
							 * No profiles to transfer
							 */
							continue;
						}

						if (inputModelClassesByFullNameInSchema
								.containsKey(loadedCiFullNameInSchema)) {

							GenericClassInfo inputCi = inputModelClassesByFullNameInSchema
									.get(loadedCiFullNameInSchema);

							ProfileUtil.transferProfiles(profilesToLoad,
									loadedCi, inputCi);

							for (PropertyInfo loadedCiPi : loadedCi.properties()
									.values()) {

								GenericPropertyInfo inputCiPi = inputCi
										.propertyByName(loadedCiPi.name());

								if (inputCiPi != null) {

									ProfileUtil.transferProfiles(profilesToLoad,
											loadedCiPi, inputCiPi);
								}
							}

						} else {
							// Ok. No corresponding class in input model.
						}
					}

				} catch (ShapeChangeAbortException e) {

					result.addError(this, 103, loadedProfileFileLocation,
							e.getMessage());
				}
			}
		}

		/*
		 * 3. Validate profile definitions in resulting model
		 */
		if (rules.contains(RULE_POST_VALIDATE_RESULTING_MODEL_PROFILES)) {

			result.addInfo(this, 109);

			ModelProfileValidator mpv = new ModelProfileValidator(inputModel,
					result);

			mpv.validateModelConsistency(true, true, !processWholeModel);
		}
	}

	/**
	 * @param model
	 * @return map with classes of the given model (key: full name in schema of
	 *         a class, value: the class); can be empty but not
	 *         <code>null</code>
	 */
	private SortedMap<String, GenericClassInfo> getClassesByFullNameInSchema(
			GenericModel model) {

		SortedMap<String, GenericClassInfo> classesByFullNameInSchema = new TreeMap<String, GenericClassInfo>();

		for (GenericClassInfo genCi : model.getGenClasses().values()) {
			classesByFullNameInSchema.put(genCi.fullNameInSchema(), genCi);
		}

		return classesByFullNameInSchema;
	}

	@Override
	public String message(int mnr) {

		switch (mnr) {
		case 1:
			return "Context: class $1$";
		case 2:
			return "Context: property $1$";
		case 100:
			return "Syntax exception while compiling the regular expression defined by transformation parameter '$1$': '$2$'. The parameter will be ignored.";
		case 101:
			return "Value '$1$' of transformation parameter '$2$' does not identify an existing directory that can be read.";
		case 102:
			return "The directory '$1$', from which profiles shall be loaded, is empty.";
		case 103:
			return "Model could not be loaded from '$1$'. It will be ignored. Details: $2$";
		case 104:
			return "Schema from input model with name '$1$' has no equivalent package in the loaded model. Consequently, no diff was performed.";
		case 105:
			return "------ Start model diff (compare model loaded from '$1$' against input model)";
		case 106:
			return "------ End model diff (compare model loaded from '$1$' against input model)";
		case 107:
			return "Model difference - version of schema '$1$' from input model is '$2$', version of reference schema from loaded model is '$3$'.";
		case 108:
			return "Model difference - $1$";
		case 109:
			return "Validating profiles in resulting model.";
		case 110:
			return "------ Start validating profiles in model loaded from '$1$'.";
		case 111:
			return "------ Start validating profiles in input model.";
		case 112:
			return "Removing profiles in input model.";
		case 113:
			return "Converting profile definitions in input model to explicit definitions.";
		case 114:
			return "Loading profiles from model located at '$1$'.";
		case 115:
			return "------ End validating profiles in input model.";
		case 116:
			return "------ End validating profiles in model loaded from '$1$'.";

		default:
			return "(Unknown message in " + this.getClass().getName()
					+ ". Message number was: " + mnr + ")";
		}
	}
}
