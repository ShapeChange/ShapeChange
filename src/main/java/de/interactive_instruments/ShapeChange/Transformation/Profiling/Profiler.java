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
 * (c) 2002-2013 interactive instruments GmbH, Bonn, Germany
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessRuleSet;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.StructuredNumber;
import de.interactive_instruments.ShapeChange.TransformerConfiguration;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.OclConstraint;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Model.TextConstraint;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericClassInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericOclConstraint;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericPackageInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericPropertyInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericTextConstraint;
import de.interactive_instruments.ShapeChange.Transformation.Transformer;
import de.interactive_instruments.ShapeChange.Transformation.Profiling.ProfileIdentifier.IdentifierPattern;

/**
 * Creates a profile of the base model by removing all classes and properties
 * that do not belong to one of the profiles stated in the ShapeChange
 * configuration.
 * 
 * Each profile identifier in the configuration parameter must match the
 * following regular expression: (\w|-)+(\[[0-9]+(\.[0-9]+)*\])?
 * 
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 */
public class Profiler implements Transformer {

	/* Profiler rule identifiers */
	public static final String RULE_TRF_PROFILING_PREPROCESSING_MODELCONSISTENCYCHECK = "rule-trf-profiling-preprocessing-modelConsistencyCheck";
	public static final String RULE_TRF_PROFILING_PREPROCESSING_PROFILESVALUECONSISTENCYCHECK = "rule-trf-profiling-preprocessing-profilesValueConsistencyCheck";
	public static final String RULE_TRF_PROFILING_POSTPROCESSING_REMOVERESIDUALTYPES = "rule-trf-profiling-postprocessing-removeResidualTypes";
	public static final String RULE_TRF_PROFILING_POSTPROCESSING_REMOVEEMPTYPACKAGES = "rule-trf-profiling-postprocessing-removeEmptyPackages";
	/**
	 * If this rule is enabled, model elements without profile information are
	 * treated as if they belonged to no profile (which overrides the default
	 * behavior that classes belong to all profiles and properties inherit
	 * profiles from their class).
	 */
	public static final String RULE_TRF_PROFILING_PROCESSING_EXPLICITPROFILESETTINGS = "rule-trf-profiling-processing-explicitProfileSettings";
	/**
	 * If this rule is enabled, then the profiler does not only remove
	 * individual classes if their profiles do not match, but also all their
	 * (direct and indirect) subclasses.
	 */
	public static final String RULE_TRF_PROFILING_PROCESSING_CLASS_REMOVAL_INCLUDES_ALL_SUBTYPES = "rule-trf-profiling-processing-classRemovalIncludesAllSubtypes";
	/**
	 * If this rule is enabled, the profiler ignores properties that belong to
	 * associations while profiling individual properties.
	 */
	public static final String RULE_TRF_PROFILING_PROCESSING_KEEP_ASSOCIATION_AS_IS = "rule-trf-profiling-processing-keepAssociationAsIs";

	/* Profiler configuration parameters */
	/**
	 * Regular expression to identify the name of the classes for which subtypes
	 * shall be included. Can also be used to identify the names of classes to
	 * exclude.
	 * 
	 * Note: only direct subclasses of classes identified via this parameter are
	 * included in the profile!
	 */
	public static final String PROFILING_CFG_PARAM_RESIDUALTYPEREMOVAL_INCLUDESUBTYPESFOR = "residualTypeRemoval_includeSubtypesFor";

	/**
	 * Name of the configuration parameter that contains information about the
	 * profile(s) to restrict the model to.
	 */
	public static final String PROFILES_PARAMETER = "profiles";
	/**
	 * Name of the tagged value that contains the profile information.
	 */
	public static final String PROFILES_TAGGED_VALUE = "profiles";
	/**
	 * Name of the configuration parameter that contains information on how to
	 * handle constraints during profiling.
	 */
	public static final String CONSTRAINTHANDLING_PARAMETER = "constraintHandling";

	/**
	 * Enumeration of the different behaviors for handling constraints during
	 * profiling.
	 * 
	 * @author Johannes Echterhoff
	 * 
	 */
	public enum ConstraintHandling {
		/**
		 * Constraints will be removed completely.
		 */
		remove,
		/**
		 * Constraints will be kept as is.
		 */
		keep,
		/**
		 * A class constraint whose name contains the name of a property that is
		 * being removed by the profiler will be removed.
		 */
		removeByPropertyNameInConstraintName
	}

	ConstraintHandling constraintHandling = ConstraintHandling.keep;
	ProfileIdentifierMap profilesFromConfig = null;

	private Map<String, ProfileIdentifierMap> classByIdProfileIdMap = new TreeMap<String, ProfileIdentifierMap>();
	private Map<String, ProfileIdentifierMap> propertyByIdProfileIdMap = new TreeMap<String, ProfileIdentifierMap>();
	private boolean isExplicitProfileSettingsRuleEnabled = false;
	private ShapeChangeResult log;
	private static final String PROFILER_ISSUE_LOG_MESSAGES_SEPARATOR = " ";

	public Profiler() {
		// nothing special to do here
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Transformation.Transformer#process(de.interactive_instruments.ShapeChange.Model.Model,
	 *      de.interactive_instruments.ShapeChange.Options,
	 *      de.interactive_instruments.ShapeChange.TransformerConfiguration,
	 *      de.interactive_instruments.ShapeChange.ShapeChangeResult)
	 */
	public void process(GenericModel genModel, Options options,
			TransformerConfiguration trfConfig, ShapeChangeResult result)
					throws ShapeChangeAbortException {

		this.log = result;

		Map<String, ProcessRuleSet> ruleSets = trfConfig.getRuleSets();

		// Get and check required parameter(s)

		// 'profiles'
		String profilesParameterValue = trfConfig
				.getParameterValue(PROFILES_PARAMETER);

		// residual type removal - include subtypes for
		Pattern residualTypeRemoval_includeSubtypesFor = trfConfig.hasParameter(
				PROFILING_CFG_PARAM_RESIDUALTYPEREMOVAL_INCLUDESUBTYPESFOR)
						? Pattern.compile(trfConfig.getParameterValue(
								PROFILING_CFG_PARAM_RESIDUALTYPEREMOVAL_INCLUDESUBTYPESFOR))
						: null;

		boolean checkOk = true;

		try {
			this.profilesFromConfig = ProfileIdentifierMap.parse(
					profilesParameterValue, IdentifierPattern.strict,
					PROFILES_PARAMETER + "_configuration_parameter");
		} catch (MalformedProfileIdentifierException e) {
			checkOk = false;
			log.addError(null, 20206, PROFILES_PARAMETER, e.getMessage());
		}

		// 'constraintHandling'
		String constraintHandlingValue = trfConfig
				.getParameterValue(CONSTRAINTHANDLING_PARAMETER);
		if (constraintHandlingValue != null
				&& constraintHandlingValue.length() > 0) {
			boolean validConstraintHandlingParameter = false;
			for (ConstraintHandling conHandlingEnum : ConstraintHandling
					.values()) {
				if (conHandlingEnum.name()
						.equalsIgnoreCase(constraintHandlingValue)) {
					this.constraintHandling = conHandlingEnum;
					validConstraintHandlingParameter = true;
					break;
				}
			}
			if (!validConstraintHandlingParameter) {
				throw new ShapeChangeAbortException(
						"Value of configuration parameter '"
								+ CONSTRAINTHANDLING_PARAMETER
								+ "' does not match one of the defined values (was: "
								+ constraintHandlingValue + ")");
			}
		}

		// for now we simply get the set of all rules defined for the
		// transformation
		Set<String> rules = new HashSet<String>();
		if (!ruleSets.isEmpty()) {
			for (ProcessRuleSet ruleSet : ruleSets.values()) {
				if (ruleSet.getAdditionalRules() != null) {
					rules.addAll(ruleSet.getAdditionalRules());
				}
			}
		}

		// 0. Keep track of global rules
		this.isExplicitProfileSettingsRuleEnabled = rules.contains(
				RULE_TRF_PROFILING_PROCESSING_EXPLICITPROFILESETTINGS);

		// 1. Execute any preprocessing
		if (rules.contains(
				RULE_TRF_PROFILING_PREPROCESSING_PROFILESVALUECONSISTENCYCHECK)) {
			/*
			 * Check that the values of the 'profiles' tagged value in classes
			 * are consistent:
			 * 
			 * NOTE: Undefined or empty 'profiles' tagged values are allowed,
			 * but the interpretation depends on the rules that are set
			 * 
			 * - future work: Ensure that profile identifiers are members of a
			 * specific set (defined by enumeration in the model or a given list
			 * of identifiers)
			 */

			/*
			 * Parse profile identifiers to check that they are correctly
			 * formatted, that the version numbers/ranges are correct and that
			 * information for a profile is not given twice.
			 */
			for (GenericClassInfo ci : genModel.selectedSchemaClasses()) {

				String profiles = ci.taggedValue(PROFILES_TAGGED_VALUE);

				if (profiles == null || profiles.trim().length() == 0) {

					// No specific profiles declared, which is valid.
					this.classByIdProfileIdMap.put(ci.id(), null);

				} else {

					// check that the profile identifiers are well-formed
					try {

						ProfileIdentifierMap piMap = ProfileIdentifierMap.parse(
								profiles, IdentifierPattern.loose, ci.name());

						this.classByIdProfileIdMap.put(ci.id(), piMap);

					} catch (MalformedProfileIdentifierException e) {
						log.addWarning(null, 20201, profiles, ci.name(),
								e.getMessage());
					}
				}

			}

			for (GenericPropertyInfo pi : genModel.selectedSchemaProperties()) {

				String profiles = pi.taggedValue(PROFILES_TAGGED_VALUE);

				if (profiles == null || profiles.trim().length() == 0) {

					// No specific profiles declared, which is valid.

					this.propertyByIdProfileIdMap.put(pi.id(), null);

				} else {

					try {

						ProfileIdentifierMap piMap = ProfileIdentifierMap.parse(
								profiles, IdentifierPattern.loose,
								pi.name() + " (property of class "
										+ pi.inClass().name() + ")");

						this.propertyByIdProfileIdMap.put(pi.id(), piMap);

					} catch (MalformedProfileIdentifierException e) {
						log.addWarning(null, 20202, profiles, pi.name(),
								pi.inClass().name(), e.getMessage());
					}
				}
			}
		}

		if (!checkOk) {
			throw new ShapeChangeAbortException(
					"One or more pre-conditions for executing the profiler were not met. Consult the log file for further information.");
		}

		if (rules.contains(
				RULE_TRF_PROFILING_PREPROCESSING_MODELCONSISTENCYCHECK)) {

			// TBD This could be moved to a separate Transformer class
			/*
			 * Check that the 'profiles' tagged value information is consistent
			 * regarding the model:
			 * 
			 * NOTE: the creation of a generalization relationship between model
			 * classes and versioning of this model change is currently not
			 * taken into account via the profiling mechanism, because adding
			 * the 'profiles' tagged value to generalization relationships is
			 * currently not foreseen.
			 */

			/*
			 * Check that the profile set of a supertype contains the profile
			 * set of its subtypes. Also check that the profile set of a class
			 * contains the profile sets of its properties (unless the rule for
			 * explicit profile settings is enabled and the class does not
			 * belong to a profile).
			 * 
			 * In both cases, take into account undefined/empty 'profiles'
			 * tagged values: profiler behavior is different depending upon
			 * whether or not the rule on explicit profile settings is enabled.
			 * If it is enabled, classes and properties without profile
			 * information belong to no profile. If the rule is disabled
			 * (default setting) then classes belong to all profiles and
			 * properties inherit the profile set from their owner.
			 * 
			 * Non-navigable properties belonging to the class are ignored.
			 */

			for (GenericClassInfo ci : genModel.selectedSchemaClasses()) {

				ProfileIdentifierMap ciPidMap = classByIdProfileIdMap
						.get(ci.id());

				// Test for subtypes
				if (ci.hasSubtypes()) {

					SortedSet<String> subtypeIds = ci.subtypes();

					for (String subtypeId : subtypeIds) {

						ProfileIdentifierMap subtypePidMap = classByIdProfileIdMap
								.get(subtypeId);

						// used for logging messages
						List<String> messages = new ArrayList<String>();

						if (!contains(ciPidMap, ci.name(), subtypePidMap,
								genModel.classById(subtypeId).name(), false,
								messages)) {

							/*
							 * This can be dangerous in case that the subtype
							 * has constraints from properties of its supertype.
							 * In such a case the constraints won't have a
							 * proper context (because the properties they are
							 * referring to may not exist in case that the
							 * supertype is omitted in a profile for which the
							 * subtype remains).
							 * 
							 * However, if all subtypes would be removed as well
							 * then it would be ok.
							 */

							if (rules.contains(
									RULE_TRF_PROFILING_PROCESSING_CLASS_REMOVAL_INCLUDES_ALL_SUBTYPES)) {

								/*
								 * as all subtypes will be removed this is not
								 * problematic - just log an info
								 */
								log.addInfo(null, 20214, ci.name(),
										genModel.classById(subtypeId).name(),
										StringUtils.join(messages,
												PROFILER_ISSUE_LOG_MESSAGES_SEPARATOR));

							} else {

								log.addError(null, 20203, ci.name(),
										genModel.classById(subtypeId).name(),
										StringUtils.join(messages,
												PROFILER_ISSUE_LOG_MESSAGES_SEPARATOR));
							}
						}
					}
				}

				// Test for navigable properties
				for (PropertyInfo pi : ci.properties().values()) {

					if (pi.isNavigable()) {

						ProfileIdentifierMap propertyPidMap = propertyByIdProfileIdMap
								.get(pi.id());

						List<String> messages = new ArrayList<String>();

						if (isExplicitProfileSettingsRuleEnabled
								&& ciPidMap == null) {

							// this is allowed

						} else if (!contains(ciPidMap, ci.name(),
								propertyPidMap,
								pi.name() + "(in class " + pi.inClass() + ")",
								true, messages)) {

							log.addWarning(null, 20204, ci.name(), pi.name(),
									StringUtils.join(messages,
											PROFILER_ISSUE_LOG_MESSAGES_SEPARATOR));
						}
					}
				}
			}
		}

		// 2. Execute profiling
		/*
		 * For each class in the generic model:
		 * 
		 * If it does not have the 'profiles' tagged value or that value is
		 * empty, keep it - unless the rule for explicit profile settings is
		 * enabled; in that case the class shall be removed.
		 * 
		 * Otherwise, if the class does not contain the target profiles, remove
		 * it. Each of its properties is removed as well.
		 * 
		 * If the rule to remove all subtypes of a class that is removed is
		 * enabled, ensure that all direct and indirect subtypes are removed.
		 * 
		 * Consistency check for constraints in remaining classes and properties
		 * is performed afterwards.
		 * 
		 * TBD: at the moment the complete association is removed if the class
		 * is an association class
		 */
		Set<GenericClassInfo> cisToRemove = new HashSet<GenericClassInfo>();

		for (GenericClassInfo ci : genModel.selectedSchemaClasses()) {

			if (cisToRemove.contains(ci)) {
				/* fine, the class is already marked for removal */
				continue;
			}

			ProfileIdentifierMap ciPidMap = classByIdProfileIdMap.get(ci.id());

			if (!contains(ciPidMap, null, profilesFromConfig, null, false,
					null)) {

				cisToRemove.add(ci);

				if (rules.contains(
						RULE_TRF_PROFILING_PROCESSING_CLASS_REMOVAL_INCLUDES_ALL_SUBTYPES)) {

					/*
					 * identify all direct and indirect subtypes of ci and add
					 * them to the list of classes to be removed
					 */
					cisToRemove.addAll(this.getAllSubtypes(ci));
				}
			}
		}

		for (GenericClassInfo ci : cisToRemove) {
			genModel.remove(ci);
		}

		/*
		 * For each (remaining) property in the generic model:
		 * 
		 * If the property does not have the profiles tagged value or that value
		 * is empty, set its value to the profile set of its class (profile set
		 * inheritance) - unless the rule for explicit profile settings is
		 * enabled (in that case the property does not belong to a profile).
		 * 
		 * If the property does not belong to any of the target profiles, remove
		 * it unless a) it belongs to an association in which the other end is
		 * navigable (in that case, set the property to be non-navigable) or b)
		 * the rule to keep associations is enabled.
		 * 
		 * When removing properties, take into account constraint handling.
		 */
		List<PropertyInfo> pisToRemove = new ArrayList<PropertyInfo>();

		for (GenericClassInfo ci : genModel.selectedSchemaClasses()) {

			for (PropertyInfo pi : ci.properties().values()) {

				if (!pi.isAttribute() && rules.contains(
						RULE_TRF_PROFILING_PROCESSING_KEEP_ASSOCIATION_AS_IS)) {

					/* ignore this property */
					continue;
				}

				if (!contains(pi, profilesFromConfig, null)) {

					pisToRemove.add(pi);
				}
			}
		}

		// now delete the properties that were marked for removal
		for (PropertyInfo pi : pisToRemove) {

			/*
			 * Cast should be safe because the pis originate from
			 * GenericClassInfos (see previous loop)
			 */
			genModel.remove((GenericPropertyInfo) pi, true);

			/*
			 * Cast should be safe: if pi is a GenericPropertyInfo then so
			 * should be its inClass()
			 */
			GenericClassInfo genCi = (GenericClassInfo) pi.inClass();

			if (constraintHandling.equals(ConstraintHandling.remove)) {

				if (!genCi.constraints().isEmpty()) {
					genCi.setConstraints(new Vector<Constraint>());
				}

			} else if (constraintHandling.equals(
					ConstraintHandling.removeByPropertyNameInConstraintName)) {

				// ensure that constraint is removed in inClass and all subtypes
				// of it, to the deepest level (currently required because
				// constraints from supertype are copied to all subtypes during
				// initial model load)

				removeConstraintByPropertyNameInTypeTree(genCi, pi.name(),
						genModel);

			} else {
				// then the constraints are kept as is
			}
		}

		for (GenericClassInfo genCi : genModel.selectedSchemaClasses()) {

			if (constraintHandling.equals(ConstraintHandling.remove)) {

				if (!genCi.constraints().isEmpty()) {
					genCi.setConstraints(new Vector<Constraint>());
				}

			} else {

				/*
				 * Perform a consistency check for constraints in remaining
				 * model elements
				 */

				/*
				 * Constraints in subtypes may be invalid if their context
				 * points to properties of supertypes that no longer exist.
				 * Check that context model element of constraints exists.
				 */

				genCi.setConstraints(this.validateConstraintContext(
						genCi.constraints(), genCi, result, genModel));

				if (genCi.properties() != null) {

					for (PropertyInfo pi : genCi.properties().values()) {

						/*
						 * cast should be safe because the properties of a
						 * GenericClassInfo should all be of type
						 * GenericPropertyInfo
						 */
						GenericPropertyInfo genPi = (GenericPropertyInfo) pi;

					List<Constraint> genPiConstraints = genPi.constraints();

						genPi.setConstraints(this.validateConstraintContext(
								genPiConstraints, genPi, result, genModel));
					}
				}

				if (constraintHandling.equals(
						ConstraintHandling.removeByPropertyNameInConstraintName)) {

					/*
					 * Class constraints may be invalid if they reference
					 * properties that have been removed during profiling
					 * 
					 * TBD - for now we try to identify such constraints by
					 * their name suffix '_Type'
					 */
					if (!genCi.constraints().isEmpty()) {

						Vector<Constraint> newConstraints = new Vector<Constraint>();

						for (Constraint con : genCi.constraints()) {

							if (con.name().endsWith("_Type")) {
								String targetPropertyName = con.name()
										.replace("_Type", "");
								if (genCi
										.property(targetPropertyName) == null) {

									log.addInfo(null, 20207, con.name(),
											genCi.name());

								} else {
									newConstraints.add(con);
								}
								
							} else {
								newConstraints.add(con);
							}
						}

						genCi.setConstraints(newConstraints);
					}

				} else {
					// then the remaining constraints are kept as is
				}
			}
		}

		// 3. Execute any postprocessing
		if (rules.contains(
				RULE_TRF_PROFILING_POSTPROCESSING_REMOVERESIDUALTYPES)) {

			// key: classid
			HashSet<GenericClassInfo> genCis = genModel.selectedSchemaClasses();

			if (genCis != null && !genCis.isEmpty()) {

				List<GenericClassInfo> featureTypes = new ArrayList<GenericClassInfo>();
				Map<String, GenericClassInfo> residualTypes = new TreeMap<String, GenericClassInfo>();

				// determine feature types and non feature types (which may be
				// residual types - to be determined later on)
				for (GenericClassInfo genCi : genCis) {

					if (genCi.category() == Options.FEATURE) {
						featureTypes.add(genCi);
					} else {
						residualTypes.put(genCi.id(), genCi);
					}
				}

				/*
				 * remove all types used by feature types from the residual
				 * types map; the remaining classes are not used by feature
				 * types and can thus be removed from the model
				 */
				Set<String> usedCisById = new HashSet<String>();

				for (GenericClassInfo ftCi : featureTypes) {

					this.deepSearchForTypesUsedByClass(ftCi, usedCisById,
							genModel, residualTypeRemoval_includeSubtypesFor);
				}

				// remove all identified Cis from the residual types map
				for (String usedCiId : usedCisById) {
					residualTypes.remove(usedCiId);
				}

				// now remove the residual types from the model
				genModel.remove(residualTypes.values());
			}

		}

		if (rules.contains(
				RULE_TRF_PROFILING_POSTPROCESSING_REMOVEEMPTYPACKAGES)) {

			// If, after profiling, there is no class in a package (or its
			// child-packages), remove it.

			Set<GenericPackageInfo> appSchemaPackages = genModel
					.selectedSchemas();

			for (GenericPackageInfo appSchemaPackage : appSchemaPackages) {

				Set<PackageInfo> emptyPackages = new HashSet<PackageInfo>();
				appSchemaPackage.getEmptyPackages(emptyPackages);

				if (emptyPackages.contains(appSchemaPackage)) {
					log.addWarning(null, 20205, appSchemaPackage.name());
					emptyPackages.remove(appSchemaPackage);
				}

				genModel.remove(emptyPackages);
			}
		}
	}

	private void removeConstraintByPropertyNameInTypeTree(
			GenericClassInfo genCi, String propertyName,
			GenericModel genModel) {

		if (!genCi.constraints().isEmpty()) {
			List<Constraint> constraints = genCi.constraints();
			for (int i = 0; i < constraints.size(); i++) {
				if (constraints.get(i).name()
						.contains(propertyName + "_Type")) {
					constraints.remove(i);
					break;
				}
			}
		}

		SortedSet<String> subtypeIds = genCi.subtypes();

		for (String subtypeId : subtypeIds) {
			GenericClassInfo subtype = genModel.getGenClasses().get(subtypeId);
			removeConstraintByPropertyNameInTypeTree(subtype, propertyName,
					genModel);
		}
	}

	/**
	 * Looks up the IDs of all classes used by the given class, either directly
	 * (through attributes, associations, supertypes, and subtypes [possibly
	 * restricted via a configuration parameter]) or indirectly (e.g. through
	 * association classes and other complex types used by the class - to the
	 * deepest sublevel).
	 * 
	 * Type ids are only added to the set if they are not already contained in
	 * it.
	 * 
	 * This method is called recursively for each type that is added to the set
	 * (so if that type is already contained in the set, no recursive call is
	 * performed).
	 * 
	 * @param ci
	 * @param usedCisById
	 * @param residualTypeRemoval_includeSubtypesFor
	 */
	private void deepSearchForTypesUsedByClass(ClassInfo ci,
			Set<String> usedCisById, GenericModel genModel,
			Pattern residualTypeRemoval_includeSubtypesFor) {

		if (usedCisById.contains(ci.id())) {
			/*
			 * apparently we already searched and found the given class, through
			 * a previous search
			 */
			return;
		}

		// add the id of the given class to the set
		usedCisById.add(ci.id());

		/* look up all properties of the class */
		SortedMap<StructuredNumber, PropertyInfo> ciPis = ci.properties();
		if (ciPis != null && !ciPis.isEmpty()) {

			// look up types used by properties
			for (PropertyInfo pi : ciPis.values()) {

				ClassInfo typeCi = genModel.classById(pi.typeInfo().id);

				// if the type is not contained in the set, perform a deep
				// search on it (which automatically adds it to the set)
				if (typeCi != null && !usedCisById.contains(pi.typeInfo().id)) {

					deepSearchForTypesUsedByClass(typeCi, usedCisById, genModel,
							residualTypeRemoval_includeSubtypesFor);

					// if the property has an association with an association
					// class and that class is not already in the set, perform a
					// deep search on it (which automatically adds it to the set
					if (pi.association() != null) {

						ClassInfo assoClass = pi.association().assocClass();

						if (assoClass != null
								&& !usedCisById.contains(assoClass.id())) {

							deepSearchForTypesUsedByClass(assoClass,
									usedCisById, genModel,
									residualTypeRemoval_includeSubtypesFor);
						}

					}
				}
			}
		}

		// look up and search through any supertype(s)
		Set<String> supertypeIds = ci.supertypes();

		/*
		 * if the class has one or more supertypes and the supertypes are not
		 * already in the set, perform a deep search on each of them (which
		 * automatically adds them to the set)
		 */
		if (supertypeIds != null && !supertypeIds.isEmpty()) {

			for (String supertypeId : supertypeIds) {

				ClassInfo supertype = genModel.classById(supertypeId);

				if (supertype != null
						&& !usedCisById.contains(supertype.id())) {

					deepSearchForTypesUsedByClass(supertype, usedCisById,
							genModel, residualTypeRemoval_includeSubtypesFor);
				}
			}
		}

		if (residualTypeRemoval_includeSubtypesFor != null
				&& residualTypeRemoval_includeSubtypesFor.matcher(ci.name())
						.matches()) {
			// look up and search through any subtype(s)
			Set<String> subtypeIds = ci.subtypes();

			/*
			 * if the class has one or more subtypes and the subtypes are not
			 * already in the set, perform a deep search on each of them (which
			 * automatically adds them to the set)
			 * 
			 * If the configuration contains a parameter that provides a regular
			 * expression to identify relevant class names, take that into
			 * account.
			 */
			if (subtypeIds != null && !subtypeIds.isEmpty()) {

				for (String subtypeId : subtypeIds) {

					ClassInfo subtype = genModel.classById(subtypeId);

					if (subtype != null
							&& !usedCisById.contains(subtype.id())) {

						deepSearchForTypesUsedByClass(subtype, usedCisById,
								genModel,
								residualTypeRemoval_includeSubtypesFor);
					}
				}
			}
		}

	}

	/**
	 * Checks if the target profiles provided via the configuration are
	 * contained in the profile information of a property. If the property does
	 * not have profile information specified, it inherits it from its class -
	 * unless the rule for explicit profile settings is enabled (in that case
	 * the property does not belong to any profile).
	 * 
	 * @param pi
	 * @param profilesFromConfig
	 * @return
	 */
	private boolean contains(PropertyInfo pi,
			ProfileIdentifierMap profilesFromConfig, List<String> messages) {

		ProfileIdentifierMap propertyProfileIdentifierMap = this.propertyByIdProfileIdMap
				.get(pi.id());
		ProfileIdentifierMap classProfileIdentifierMap = this.classByIdProfileIdMap
				.get(pi.inClass().id());

		if (propertyProfileIdentifierMap == null
				&& !isExplicitProfileSettingsRuleEnabled) {

			/* property gets its profile info from its class */

			return this.contains(classProfileIdentifierMap, pi.inClass().name(),
					profilesFromConfig,
					PROFILES_PARAMETER + "_config_parameter", false, messages);

		} else {

			/*
			 * property either has profile infos or - if rule for explicit
			 * profile setting is enabled - does not belong to a profile
			 */

			return this.contains(propertyProfileIdentifierMap,
					pi.name() + (" (property in class '" + pi.inClass().name()
							+ "')"),
					profilesFromConfig,
					PROFILES_PARAMETER + "_config_parameter", false, messages);
		}

	}

	/**
	 * Checks if one set of profiles contains another one.
	 * <p>
	 * Takes into account whether or not the rule for explicit profile settings
	 * is enabled, which matters in case that a model element has no profile
	 * information.
	 * <p>
	 * Useful for:
	 * <ul>
	 * <li>Checking if the profile information for a property is contained in
	 * the profile set of its class (profileInheritance: true).</li>
	 * <li>Checking if the profiles of the subtypes of a class are contained in
	 * that classes profiles (profileInheritance: false).</li>
	 * <li>Checking if the target profiles provided via the configuration are
	 * contained in the profile information of a class (profileInheritance:
	 * false).</li>
	 * </ul>
	 * 
	 * Profile inheritance: if profile map B is null but map A is not, map B
	 * inherits the profiles from map A. This is irrelevant if the rule for
	 * explicit profile settings is enabled.
	 * 
	 * 
	 * 
	 * @param profilesA
	 *            profile map that contains profilesB (or not)
	 * @param profilesB
	 *            profile map that is contained in profilesA (or not)
	 * @param profileInheritance
	 *            true if profile inheritance shall be applied, else false
	 *            (irrelevant if rule for explicit profile settings is enabled)
	 * @param messages
	 *            used to log the reason(s) why profilesA does not contain
	 *            profilesB
	 * @return
	 */
	public boolean contains(ProfileIdentifierMap profilesA, String aOwnerName,
			ProfileIdentifierMap profilesB, String bOwnerName,
			boolean profileInheritance, List<String> messages) {

		if (profilesA == null && profilesB == null) {

			return true;

		} else if (profilesA == null && profilesB != null) {

			if (isExplicitProfileSettingsRuleEnabled) {

				/*
				 * profile map A is empty while profile map B is not; the empty
				 * set does not contain a non-empty set:
				 */
				return false;

			} else {

				/*
				 * profile map A is unlimited and thus contains profile map B
				 * (which is limited):
				 */
				return true;
			}

		} else if (profilesA != null && profilesB == null) {

			if (isExplicitProfileSettingsRuleEnabled) {

				/*
				 * profile map B is empty while profile map A is not; a
				 * non-empty set always contains the empty set
				 */
				return true;

			} else {

				/*
				 * Now it depends if profile inheritance shall be applied or not
				 * This is the case for properties but not for subtypes, for
				 * example.
				 */
				if (profileInheritance) {
					// Ok, profile map B inherits profile map A:
					return true;
				} else {
					/*
					 * As profile inheritance is false, profile map B is
					 * unlimited while profile map A is not, therefore the
					 * latter does not contain the former:
					 */
					if (messages != null) {
						messages.add("The profiles owned by '" + aOwnerName
								+ "' do not contain the profiles owned by '"
								+ bOwnerName
								+ "' because the latter does not inherit the profiles from the former, and because the latter has an unlimited profile set while the former does not.");
					}
					return false;
				}
			}

		} else {
			// Both profiles are limited, thus compare their contents
			return profilesA.contains(profilesB, messages);
		}

	}

	/**
	 * Assesses the context model element of each constraint in the given
	 * vector. If the context model element is unknown, which can result during
	 * profiling, then the constraint is not added to the result. An appropriate
	 * log message is added in such a case.
	 * 
	 * @param constraints
	 * @param owner
	 *            Info class that the constraints belong to (PropertyInfo or
	 *            ClassInfo)
	 * @param result
	 * @param genModel
	 * @return
	 */
	private Vector<Constraint> validateConstraintContext(
			List<Constraint> constraints, Info owner,
			ShapeChangeResult result, GenericModel genModel) {

		Vector<Constraint> results = new Vector<Constraint>();

		if (constraints != null) {

			for (Constraint con : constraints) {

				if (!(con instanceof GenericTextConstraint)
						&& !(con instanceof GenericOclConstraint)) {
					log.addError(null, 20208, con.name(),
							con.contextModelElmt().name());
					continue;
				}

				Info contextModelElement = con.contextModelElmt();

				if (contextModelElement == null) {

					StringBuffer sb = new StringBuffer();
					sb.append(
							"(Profiler) contextModelElement for constraint named '"
									+ con.name() + "' [" + con.text()
									+ "] is null.");

					if (owner instanceof PropertyInfo) {

						PropertyInfo pi = (PropertyInfo) owner;

						sb.append(" Omitting constraint in property '"
								+ pi.name() + "' of class '"
								+ pi.inClass().name() + "'.");

					} else if (owner instanceof ClassInfo) {

						ClassInfo ci = (ClassInfo) owner;

						sb.append(" Omitting constraint in class '" + ci.name()
								+ "'.");

					} else {

						sb.append(" Omitting constraint in Info class '"
								+ owner.name() + "'.");
					}

					log.addWarning(null, 20209, sb.toString());
					continue;
				}

				if (con instanceof TextConstraint
						|| con instanceof OclConstraint) {

					if (con.contextModelElmtType().equals(
							Constraint.ModelElmtContextType.ATTRIBUTE)) {

						GenericPropertyInfo genPi = genModel.getGenProperties()
								.get(contextModelElement.id());

						if (genPi == null) {

							log.addError(null, 20210,
									contextModelElement.name(), con.name());

						} else {

							results.add(con);
						}

					} else if (con.contextModelElmtType().equals(

							Constraint.ModelElmtContextType.CLASS)) {

						GenericClassInfo genCi = genModel.getGenClasses()
								.get(contextModelElement.id());

						if (genCi == null) {

							log.addError(null, 20211,
									contextModelElement.name(), con.name());

						} else {

							results.add(con);
						}

					} else {
						log.addWarning(null, 20212,
								con.contextModelElmtType().name());
					}

				} else {
					log.addWarning(null, 20213, con.getClass().getName());
				}
			}
		}
		return results;
	}

	/**
	 * @return Set with all direct and indirect subtypes of the given class (if
	 *         they are contained in the generic model and instance of
	 *         GenericClassInfo)
	 */
	public Set<GenericClassInfo> getAllSubtypes(GenericClassInfo genCi) {

		Set<GenericClassInfo> result = new HashSet<GenericClassInfo>();

		if (genCi == null)
			return result;

		Set<String> directSubtypes = genCi.subtypes();

		if (directSubtypes != null && !directSubtypes.isEmpty()) {

			Set<GenericClassInfo> directGenSubtypes = new HashSet<GenericClassInfo>();

			for (String subtypeId : directSubtypes) {

				GenericModel genModel = genCi.model();

				ClassInfo ciSub = genModel.classById(subtypeId);

				if (ciSub instanceof GenericClassInfo) {

					GenericClassInfo genCiSub = (GenericClassInfo) ciSub;

					directGenSubtypes.add(genCiSub);

					if (ciSub != null && ciSub instanceof GenericClassInfo) {

						result.addAll(getAllSubtypes(genCiSub));
					}

				} else {

					log.addInfo(null, 20215, ciSub.name(), genCi.name());
				}
			}

			result.addAll(directGenSubtypes);
		}

		return result;
	}
}
