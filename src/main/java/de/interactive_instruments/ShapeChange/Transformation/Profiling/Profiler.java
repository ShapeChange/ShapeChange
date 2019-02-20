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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Multiplicity;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessRuleSet;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.StructuredNumber;
import de.interactive_instruments.ShapeChange.TransformerConfiguration;
import de.interactive_instruments.ShapeChange.Model.AssociationInfo;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.MalformedProfileIdentifierException;
import de.interactive_instruments.ShapeChange.Model.OclConstraint;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Model.TaggedValues;
import de.interactive_instruments.ShapeChange.Model.TextConstraint;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericClassInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericFolConstraint;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericOclConstraint;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericPackageInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericPropertyInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericTextConstraint;
import de.interactive_instruments.ShapeChange.Profile.ModelProfileValidator;
import de.interactive_instruments.ShapeChange.Profile.ProfileIdentifier;
import de.interactive_instruments.ShapeChange.Profile.Profiles;
import de.interactive_instruments.ShapeChange.Transformation.Transformer;
import de.interactive_instruments.ShapeChange.UI.StatusBoard;

/**
 * Creates a profile of the base model by removing all classes and properties
 * that do not belong to one of the profiles stated in the ShapeChange
 * configuration.
 * 
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 */
public class Profiler implements Transformer, MessageSource {

	private static final Splitter commaSplitter = Splitter.on(',')
			.omitEmptyStrings().trimResults();
	private static final Joiner commaJoiner = Joiner.on(",").skipNulls();

	/* Profiler status codes */
	public static final int STATUS_PREPROCESSING_PROFILESVALUECONSISTENCYCHECK = 200100;
	public static final int STATUS_PREPROCESSING_MODELCONSISTENCYCHECK = 200101;
	public static final int STATUS_PROCESSING_PROFILING = 200130;
	public static final int STATUS_POSTPROCESSING_REMOVERESIDUALTYPES = 200170;
	public static final int STATUS_POSTPROCESSING_REMOVEEMPTYPACKAGES = 200171;

	/* Profiler rule identifiers */
	public static final String RULE_TRF_PROFILING_PREPROCESSING_MODELCONSISTENCYCHECK = "rule-trf-profiling-preprocessing-modelConsistencyCheck";
	// public static final String
	// RULE_TRF_PROFILING_PREPROCESSING_PROFILESVALUECONSISTENCYCHECK =
	// "rule-trf-profiling-preprocessing-profilesValueConsistencyCheck";
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

	/**
	 * If this rule is enabled, all profile information (including the
	 * 'profiles' tagged value) will be removed in the processed model. This can
	 * be useful for cleaning up the model for subsequent processing steps where
	 * profile information shall not be included, like writing the profile back
	 * into an Enterprise Architect repository.
	 */
	public static final String RULE_TRF_PROFILING_POSTPROCESSING_REMOVE_PROFILE_INFOS = "rule-trf-profiling-postprocessing-removeProfileInfos";

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

	/*
	 * Profile parameter identifiers recognized by the Profiler itself (all
	 * other profile parameters will be transformed into tagged values).
	 */
	public static final String PROFILE_PARAMETER_MULTIPLICITY = "multiplicity";
	public static final String PROFILE_PARAMETER_ISNAVIGABLE = "isNavigable";
	public static final String PROFILE_PARAMETER_GEOMETRY = "geometry";

	ConstraintHandling constraintHandling = ConstraintHandling.keep;
	Profiles profilesFromConfig = null;

	private boolean isExplicitProfileSettingsRuleEnabled = false;
	private ShapeChangeResult result;

	public Profiler() {
		// nothing special to do here
	}

	@Override
	public void process(GenericModel genModel, Options options,
			TransformerConfiguration trfConfig, ShapeChangeResult result)
			throws ShapeChangeAbortException {

		this.result = result;

		Map<String, ProcessRuleSet> ruleSets = trfConfig.getRuleSets();

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

		try {
			this.profilesFromConfig = Profiles.parse(profilesParameterValue,
					true);
		} catch (MalformedProfileIdentifierException e) {
			this.profilesFromConfig = new Profiles();
			result.addError(this, 20219, PROFILES_PARAMETER, e.getMessage());
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
				result.addError(this, 20220, CONSTRAINTHANDLING_PARAMETER,
						constraintHandlingValue);
				this.constraintHandling = ConstraintHandling.keep;
			}
		}

		if (rules.contains(
				RULE_TRF_PROFILING_PREPROCESSING_MODELCONSISTENCYCHECK)) {

			StatusBoard.getStatusBoard()
					.statusChanged(STATUS_PREPROCESSING_MODELCONSISTENCYCHECK);

			ModelProfileValidator mpv = new ModelProfileValidator(genModel,
					result);

			boolean warnIfSupertypeProfilesDoNotContainSubtypeProfiles = rules
					.contains(
							RULE_TRF_PROFILING_PROCESSING_CLASS_REMOVAL_INCLUDES_ALL_SUBTYPES);

			mpv.validateModelConsistency(isExplicitProfileSettingsRuleEnabled,
					warnIfSupertypeProfilesDoNotContainSubtypeProfiles, true);
		}

		// 2. Execute profiling

		StatusBoard.getStatusBoard().statusChanged(STATUS_PROCESSING_PROFILING);

		/*
		 * For each class in schemas selected for processing:
		 * 
		 * If it does not have 'profiles', keep it - unless the rule for
		 * explicit profile settings is enabled; in that case the class shall be
		 * removed.
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

			if (!ci.profiles().contains(null, profilesFromConfig, null,
					isExplicitProfileSettingsRuleEnabled, false, null)) {

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

				if (!contains(pi, profilesFromConfig,
						isExplicitProfileSettingsRuleEnabled, null)) {

					pisToRemove.add(pi);
				}
			}
		}

		// now delete the properties that were marked for removal
		for (PropertyInfo pi : pisToRemove) {

			genModel.remove((GenericPropertyInfo) pi, true);

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

									result.addInfo(this, 20207, con.name(),
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

		if (this.profilesFromConfig.size() > 1) {

			result.addWarning(this, 100, "" + this.profilesFromConfig.size());

		} else {

			String nameOfProfileFromConfig = this.profilesFromConfig.get(0)
					.getName();

			// handle profile parameters in remaining classes and their
			// properties
			for (GenericClassInfo genCi : genModel.selectedSchemaClasses()) {

				ProfileIdentifier profileOfCi = genCi.profiles()
						.getProfile(nameOfProfileFromConfig);

				if (profileOfCi != null && profileOfCi.hasParameters()) {

					for (Entry<String, String> parameterEntry : profileOfCi
							.getParameter().entrySet()) {

						String parameterName = parameterEntry.getKey();
						String parameterValue = parameterEntry.getValue();

						if (parameterName
								.equalsIgnoreCase(PROFILE_PARAMETER_GEOMETRY)) {

							if (parameterValue != null) {

								String geometryTV = genCi.taggedValue(
										PROFILE_PARAMETER_GEOMETRY);

								if (geometryTV == null) {

									/*
									 * The class does not restrict geometries,
									 * so we can just set the 'geometry' tagged
									 * value to the metadata value.
									 */
									genCi.setTaggedValue(
											PROFILE_PARAMETER_GEOMETRY,
											parameterValue, false);

								} else {

									/*
									 * Validation would have been done before.
									 * We simply compute the intersection of
									 * geometry values defined by the class and
									 * by the profile.
									 */

									SortedSet<String> geometryTVValues = new TreeSet<String>(
											commaSplitter
													.splitToList(geometryTV));
									SortedSet<String> geometryProfileValues = new TreeSet<String>(
											commaSplitter.splitToList(
													parameterValue));

									SetView<String> intersection = Sets
											.intersection(geometryProfileValues,
													geometryTVValues);

									if (intersection.isEmpty()) {
										// then do nothing
									} else {
										genCi.setTaggedValue(
												PROFILE_PARAMETER_GEOMETRY,
												commaJoiner.join(intersection),
												false);
									}
								}
							}

						} else {

							/*
							 * Transform unknown metadata to tagged value of
							 * class
							 */
							String newTagName = parameterName;
							String newTagValue = parameterValue == null ? ""
									: parameterValue;

							genCi.setTaggedValue(newTagName, newTagValue,
									false);
						}
					}
				}

				/*
				 * Keep track of associations that may have been removed because
				 * navigability is set to false on both ends after application
				 * of profile metadata.
				 */
				Set<AssociationInfo> associationsToRemove = new HashSet<AssociationInfo>();

				for (PropertyInfo pi : genCi.properties().values()) {

					GenericPropertyInfo genPi = (GenericPropertyInfo) pi;

					ProfileIdentifier profileOfPi = genPi.profiles()
							.getProfile(nameOfProfileFromConfig);

					if (profileOfPi != null && profileOfPi.hasParameters()) {

						for (Entry<String, String> parameterEntry : profileOfPi
								.getParameter().entrySet()) {

							String parameterName = parameterEntry.getKey();
							String parameterValue = parameterEntry.getValue();

							if (parameterName.equalsIgnoreCase(
									PROFILE_PARAMETER_MULTIPLICITY)) {

								if (parameterValue != null) {

									Multiplicity multProfile = new Multiplicity(
											parameterValue);
									Multiplicity multPi = genPi.cardinality();

									/*
									 * Validation would have been done before.
									 * We simply compute the intersection of the
									 * multiplicity ranges defined by the
									 * property and by the profile.
									 */

									int newMin;
									int newMax;

									if (multProfile.minOccurs < multPi.minOccurs) {
										newMin = multPi.minOccurs;
									} else {
										newMin = multProfile.minOccurs;
									}

									if (multProfile.maxOccurs > multPi.maxOccurs) {
										newMax = multPi.maxOccurs;
									} else {
										newMax = multProfile.maxOccurs;
									}

									multPi.minOccurs = newMin;
									multPi.maxOccurs = newMax;
								}

							} else if (parameterName.equalsIgnoreCase(
									PROFILE_PARAMETER_ISNAVIGABLE)) {

								if (parameterValue != null
										&& !genPi.isAttribute()) {

									Boolean isNavigable = Boolean
											.parseBoolean(parameterValue);

									// only allow setting navigability to false
									if (!isNavigable) {
										genPi.setNavigable(isNavigable);
									}

									/*
									 * Note association for removal if both ends
									 * are no longer navigable.
									 */
									AssociationInfo ai = genPi.association();
									boolean aiEnd1IsNavigable = ai.end1()
											.isNavigable();
									boolean aiEnd2IsNavigable = ai.end2()
											.isNavigable();
									if (!(aiEnd1IsNavigable
											|| aiEnd2IsNavigable)) {
										associationsToRemove.add(ai);
									}
								}

							} else {

								/*
								 * Transform unknown metadata to tagged value of
								 * property
								 */
								String newTagName = parameterName;
								String newTagValue = parameterValue == null ? ""
										: parameterValue;

								genPi.setTaggedValue(newTagName, newTagValue,
										false);
							}
						}
					}
				}

				/*
				 * Remove associations for which navigability is false on both
				 * ends after application of profile metadata.
				 */
				for (AssociationInfo aiToRemove : associationsToRemove) {
					genModel.remove(aiToRemove);
				}
			}
		}

		// 3. Execute any postprocessing

		if (rules.contains(
				RULE_TRF_PROFILING_POSTPROCESSING_REMOVE_PROFILE_INFOS)) {

			for (GenericClassInfo genCi : genModel.getGenClasses().values()) {

				genCi.setProfiles(new Profiles());

				TaggedValues tvs = genCi.taggedValuesAll();
				tvs.remove("profiles");

				genCi.setTaggedValues(tvs, false);
			}

			for (GenericPropertyInfo genPi : genModel.getGenProperties()
					.values()) {

				genPi.setProfiles(new Profiles());

				TaggedValues tvs = genPi.taggedValuesAll();
				tvs.remove("profiles");

				genPi.setTaggedValues(tvs, false);
			}
		}

		if (rules.contains(
				RULE_TRF_PROFILING_POSTPROCESSING_REMOVERESIDUALTYPES)) {

			StatusBoard.getStatusBoard()
					.statusChanged(STATUS_POSTPROCESSING_REMOVERESIDUALTYPES);

			// key: classid
			SortedSet<GenericClassInfo> genCis = genModel
					.selectedSchemaClasses();

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

			StatusBoard.getStatusBoard()
					.statusChanged(STATUS_POSTPROCESSING_REMOVEEMPTYPACKAGES);

			// If, after profiling, there is no class in a package (or its
			// child-packages), remove it.

			Set<PackageInfo> appSchemaPackages = genModel.selectedSchemas();

			for (PackageInfo aspkg : appSchemaPackages) {

				GenericPackageInfo appSchemaPackage = (GenericPackageInfo) aspkg;

				Set<PackageInfo> emptyPackages = new HashSet<PackageInfo>();
				appSchemaPackage.getEmptyPackages(emptyPackages);

				if (emptyPackages.contains(appSchemaPackage)) {
					result.addWarning(this, 20205, appSchemaPackage.name());
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
	private boolean contains(PropertyInfo pi, Profiles profilesFromConfig,
			boolean isExplicitProfileSettingsRuleEnabled,
			List<String> messages) {

		Profiles propertyProfiles = pi.profiles();
		Profiles classProfiles = pi.inClass().profiles();

		if (propertyProfiles.isEmpty()
				&& !isExplicitProfileSettingsRuleEnabled) {

			/* property gets its profile info from its class */

			return classProfiles.contains(pi.inClass().name(),
					profilesFromConfig,
					PROFILES_PARAMETER + "_config_parameter",
					isExplicitProfileSettingsRuleEnabled, false, messages);

		} else {

			/*
			 * property either has profile infos or - if rule for explicit
			 * profile setting is enabled - does not belong to a profile
			 */

			return propertyProfiles.contains(
					pi.name() + (" (property in class '" + pi.inClass().name()
							+ "')"),
					profilesFromConfig,
					PROFILES_PARAMETER + "_config_parameter",
					isExplicitProfileSettingsRuleEnabled, false, messages);
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
			List<Constraint> constraints, Info owner, ShapeChangeResult result,
			GenericModel genModel) {

		Vector<Constraint> results = new Vector<Constraint>();

		if (constraints != null) {

			for (Constraint con : constraints) {

				if (!(con instanceof GenericTextConstraint)
						&& !(con instanceof GenericOclConstraint)
						&& !(con instanceof GenericFolConstraint)) {

					result.addError(this, 20208, con.name(),
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

					result.addWarning(this, 20209, sb.toString());
					continue;
				}

				if (con instanceof TextConstraint
						|| con instanceof OclConstraint) {

					if (con.contextModelElmtType().equals(
							Constraint.ModelElmtContextType.ATTRIBUTE)) {

						GenericPropertyInfo genPi = genModel.getGenProperties()
								.get(contextModelElement.id());

						if (genPi == null) {

							result.addError(this, 20210,
									contextModelElement.name(), con.name());

						} else {

							results.add(con);
						}

					} else if (con.contextModelElmtType().equals(

							Constraint.ModelElmtContextType.CLASS)) {

						GenericClassInfo genCi = genModel.getGenClasses()
								.get(contextModelElement.id());

						if (genCi == null) {

							result.addError(this, 20211,
									contextModelElement.name(), con.name());

						} else {

							results.add(con);
						}

					} else {
						result.addWarning(this, 20212,
								con.contextModelElmtType().name());
					}

				} else {
					result.addWarning(this, 20213, con.getClass().getName());
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

		Set<GenericClassInfo> subtypes = new HashSet<GenericClassInfo>();

		if (genCi == null)
			return subtypes;

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

						subtypes.addAll(getAllSubtypes(genCiSub));
					}

				} else {

					result.addInfo(this, 20215, ciSub.name(), genCi.name());
				}
			}

			subtypes.addAll(directGenSubtypes);
		}

		return subtypes;
	}

	public String message(int mnr) {

		switch (mnr) {
		case 1:
			return "Context: class $1$";
		case 2:
			return "Context: property $1$";
		case 100:
			return "??Configuration parameter '" + PROFILES_PARAMETER
					+ "' specifies more than one profile (found: $1$ profiles). Profile metadata is only processed if the parameter identifies exactly one profile.";
		case 101:
			return "";
		case 102:
			return "";
		case 103:
			return "";

		
//		case 20202:
//			return "<UNUSED_20202>";
//		case 20203:
//			return "The profile set of class '$1$' does not contain the profile set of its subtype '$2$': $3$";
//		case 20204:
//			return "The profile set of class '$1$' does not contain the profile set of its property '$2$': $3$";
		case 20205:
			return "The application schema package '$1$' is completely empty after profiling.";
		case 20207:
			return "Removing constraint '$1$' from class '$2$' because the constraint targets a property that is missing in the class or its supertypes (to highest level)";
		case 20208:
			return "System Error: Constraint '$1$' in Class '$2$' not of type 'GenericText/OclConstraint'.";
		case 20209:
			return "$1$";
		case 20210:
			return "GenericPropertyInfo '$1$' is the context model element of the constraint named '$2$'. The property does no longer exist in the model after profiling, thus the constraint is removed.";
		case 20211:
			return "GenericClassInfo '$1$' is the context model element of the constraint named '$2$'. The class does no longer exist in the model after profiling, thus the constraint is removed.";
		case 20212:
			return "Unrecognized constraint context model element type: '$1$'.";
		case 20213:
			return "Unrecognized constraint type: '$1$'.";
//		case 20214:
//			return "The profile set of class '$1$' does not contain the profile set of its subtype '$2$': $3$. Because of the chosen transformation rule(s), '$1$' and all its subtypes will be removed, so that the profile mismatch between super- and subtype does not lead to model inconsistencies.";
		case 20215:
			return "??Class '$1$' - which is a subtype of '$2$' - is not an instance of GenericClassInfo (likely reason: it belongs to a package that is not part of the schema selected for processing). It (and its possibly existing subtypes) won't be removed from the model (which should be ok, given that it is (likely) not part of the selected schema destined for final processing in target(s)).";
		case 20219:
			return "Error parsing transformation parameter '$1$': '$2$'. Assuming no profiles as value for the parameter. This may lead to unexpected results.";
		case 20220:
			return "Value of configuration parameter '$1$' does not match one of the defined values (was: '$2$'). Using default value.";
		
		default:
			return "(Unknown message in " + this.getClass().getName()
					+ ". Message number was: " + mnr + ")";
		}
	}
}
