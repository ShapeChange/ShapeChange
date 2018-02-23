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
package de.interactive_instruments.ShapeChange.Transformation.Constraints;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessRuleSet;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.StructuredNumber;
import de.interactive_instruments.ShapeChange.TransformerConfiguration;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.Descriptors;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericClassInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericOclConstraint;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericPackageInfo;
import de.interactive_instruments.ShapeChange.Transformation.Transformer;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class ProfileSchemaConstraintTransformer
		implements Transformer, MessageSource {

	/* ------------------------------------------- */
	/* --- configuration parameter identifiers --- */
	/* ------------------------------------------- */

	/**
	 *
	 */
	public static final String PARAM_BASE_SCHEMA_PACKAGE_NAME = "baseSchemaPackageName";

	/**
	 * 
	 */
	public static final String PARAM_PROFILE_SCHEMA_PACKAGE_NAME = "profileSchemaPackageName";

	/**
	 * 
	 */
	public static final String PARAM_PROFILE_NAME = "profileName";

	/**
	 * Optional
	 */
	public static final String PARAM_SUBTYPE_NAME_PREFIX = "subtypeNamePrefix";

	/* ------------------------ */
	/* --- rule identifiers --- */
	/* ------------------------ */

	/**
	 * 
	 */
	public static final String RULE_TRF_CLS_CREATE_GENERAL_OUT_OF_SCOPE_CONSTRAINTS = "rule-trf-cls-createGeneralOutOfScopeConstraints";

	private Options options = null;
	private ShapeChangeResult result = null;

	@Override
	public void process(GenericModel genModel, Options options,
			TransformerConfiguration trfConfig, ShapeChangeResult result)
			throws ShapeChangeAbortException {

		this.options = options;
		this.result = result;

		Map<String, ProcessRuleSet> ruleSets = trfConfig.getRuleSets();

		// get the set of all rules defined for the transformation
		Set<String> rules = new HashSet<String>();
		if (!ruleSets.isEmpty()) {
			for (ProcessRuleSet ruleSet : ruleSets.values()) {
				if (ruleSet.getAdditionalRules() != null) {
					rules.addAll(ruleSet.getAdditionalRules());
				}
			}
		}

		/*
		 * because there are no mandatory - in other words default - rules for
		 * this transformer simply return the model if no rules are defined in
		 * the rule sets (which the schema allows)
		 */
		if (rules.isEmpty())
			return;

		// apply pre-processing (nothing to do right now)

		// execute rules

		if (rules.contains(
				RULE_TRF_CLS_CREATE_GENERAL_OUT_OF_SCOPE_CONSTRAINTS)) {

			result.addInfo(null, 20103,
					RULE_TRF_CLS_CREATE_GENERAL_OUT_OF_SCOPE_CONSTRAINTS);
			applyRuleCreateGeneralOutOfScopeConstraints(genModel, trfConfig,
					rules);
		}

		// apply post-processing (nothing to do right now)
	}

	private void applyRuleCreateGeneralOutOfScopeConstraints(
			GenericModel genModel, TransformerConfiguration config,
			Set<String> rules) {

		/*
		 * NOTE: Parameter checks are performed by the configuration validator.
		 */

		String baseSchemaPackageName = config
				.getParameterValue(PARAM_BASE_SCHEMA_PACKAGE_NAME);
		String profileSchemaPackageName = config
				.getParameterValue(PARAM_PROFILE_SCHEMA_PACKAGE_NAME);
		String profileName = config.getParameterValue(PARAM_PROFILE_NAME);

		// Get base schema
		SortedSet<PackageInfo> baseSchemaSet = genModel
				.schemas(baseSchemaPackageName);

		if (baseSchemaSet.isEmpty()) {
			result.addError(this, 100, baseSchemaPackageName);
			return;
		} else if (baseSchemaSet.size() > 1) {
			result.addError(this, 101, baseSchemaPackageName);
			return;
		}

		PackageInfo baseSchema = baseSchemaSet.first();

		// Get profile schema
		SortedSet<PackageInfo> profileSchemaSet = genModel
				.schemas(profileSchemaPackageName);

		if (profileSchemaSet.isEmpty()) {
			result.addError(this, 100, profileSchemaPackageName);
			return;
		} else if (profileSchemaSet.size() > 1) {
			result.addError(this, 101, profileSchemaPackageName);
			return;
		}

		GenericPackageInfo profileSchema = (GenericPackageInfo) profileSchemaSet
				.first();

		// Get classes of base schema and the profile schema
		SortedSet<ClassInfo> baseSchemaClasses = genModel.classes(baseSchema);
		SortedSet<ClassInfo> profileSchemaClasses = genModel
				.classes(profileSchema);

		for (ClassInfo baseSchemaClass : baseSchemaClasses) {

			/*
			 * First, check that this class is relevant, i.e. it belongs to the
			 * profile, and that it has irrelevant properties, i.e. some of its
			 * direct (non-inherited) properties do not belong to the profile.
			 */
			if (!baseSchemaClass.profiles().hasProfile(profileName)) {
				continue;
			}

			SortedSet<PropertyInfo> irrelevantProps = new TreeSet<>();

			for (PropertyInfo pi : baseSchemaClass.properties().values()) {

				if (!pi.profiles().hasProfile(profileName)) {
					irrelevantProps.add(pi);
				}
			}

			if (irrelevantProps.isEmpty()) {
				continue;
			}

			/*
			 * Now, investigate the class hierarchy of the base class (i.e., the
			 * base class itself and all its direct and indirect subtypes).
			 * Identify all relevant base classes in this hierarchy, i.e.
			 * classes that 1) are not abstract, 2) belong to the profile
			 * (identified via parameter 'profileName'), 3) belong to the base
			 * schema.
			 */
			SortedSet<ClassInfo> classesInClassHierarchy = baseSchemaClass
					.subtypesInCompleteHierarchy();
			classesInClassHierarchy.add(baseSchemaClass);

			SortedSet<ClassInfo> relevantBaseClassesInClassHierarchy = new TreeSet<>();

			for (ClassInfo ci : classesInClassHierarchy) {
				if (!ci.isAbstract() && ci.profiles().hasProfile(profileName)
						&& baseSchemaClasses.contains(ci)) {
					relevantBaseClassesInClassHierarchy.add(ci);
				}
			}

			/*
			 * For each relevant class, check if a direct subtype from the
			 * profile schema exists (regardless whether it is suppressed or
			 * not). Otherwise, create a new subtype in the profile schema
			 * package (with suppress=true).
			 */

			for (ClassInfo relevantBaseClass : relevantBaseClassesInClassHierarchy) {

				SortedSet<String> subtypeIDs = relevantBaseClass.subtypes();
				SortedSet<ClassInfo> subtypes = new TreeSet<>();

				for (String subtypeID : subtypeIDs) {
					subtypes.add(genModel.classById(subtypeID));
				}

				/*
				 * Not entirely sure if more than one subtype makes sense, so
				 * let's cover that situation as well.
				 */
				SortedSet<GenericClassInfo> profileSchemaSubtypes = new TreeSet<>();

				for (ClassInfo subtype : subtypes) {
					if (profileSchemaClasses.contains(subtype)) {
						profileSchemaSubtypes.add((GenericClassInfo) subtype);
					}
				}

				if (profileSchemaSubtypes.isEmpty()) {

					/*
					 * Create a new subtype in the profile schema, with
					 * suppressed=true.
					 */
					GenericClassInfo subtype = createSubtypeInProfileSchema(
							(GenericClassInfo) relevantBaseClass, profileSchema,
							genModel, config);

					subtype.setTaggedValue("suppress", "true", false);

					profileSchemaSubtypes.add(subtype);
				}

				for (GenericClassInfo profileSchemaSubtype : profileSchemaSubtypes) {

					List<Constraint> newCons = new ArrayList<>();

					/*
					 * Add an OCL constraint for each irrelevant property.
					 */
					for (PropertyInfo pi : irrelevantProps) {

						String constrName = pi.name() + "_prohibited";
						String constrStatus = "Approved";

						String constrText = "/*" + pi.name()
								+ " is prohibited.*/\r\n" + "inv: " + pi.name()
								+ "->isEmpty()";

						GenericOclConstraint con = new GenericOclConstraint(
								profileSchemaSubtype, constrName, constrStatus,
								constrText);
						newCons.add(con);
					}

					profileSchemaSubtype.addConstraints(newCons);
				}
			}
		}
	}

	private GenericClassInfo createSubtypeInProfileSchema(
			GenericClassInfo baseClass, GenericPackageInfo profileSchema,
			GenericModel genModel, TransformerConfiguration config) {

		String subtypeNamePrefix = config.parameterAsString(
				PARAM_SUBTYPE_NAME_PREFIX, null, false, true);

		String baseName = baseClass.name();
		String subtypeName = subtypeNamePrefix == null ? baseName
				: subtypeNamePrefix + baseName;

		GenericClassInfo subtype = new GenericClassInfo(genModel,
				baseClass.id() + "_ProfileSchemaSubtype", subtypeName,
				baseClass.category());

		subtype.setDescriptors(new Descriptors());
		subtype.setProfiles(null);
		subtype.setStereotypes(baseClass.stereotypes());
		subtype.setIsAbstract(false);
		subtype.setIsLeaf(false);
		subtype.setAssocInfo(null);
		subtype.setProperties(new TreeMap<StructuredNumber, PropertyInfo>());

		// finally, set model links and register the class
		subtype.setPkg(profileSchema);

		subtype.setBaseClass(baseClass);
		subtype.addSupertype(baseClass.id());
		baseClass.addSubtype(subtype.id());

		subtype.setSubtypes(null);

		genModel.register(subtype);

		profileSchema.addClass(subtype);

		return subtype;
	}

	@Override
	public String message(int mnr) {

		switch (mnr) {
		case 0:
			return "Context: property '$1$'.";
		case 1:
			return "Context: class '$1$'.";
		case 2:
			return "Context: association class '$1$'.";
		case 3:
			return "Context: association between class '$1$' (with property '$2$') and class '$3$' (with property '$4$')";

		// Messages for RULE_
		case 100:
			return "No schema with package name '$1$' was found in the model. Rule will not be processed.";
		case 101:
			return "More than one schema with package name '$1$' was found in the model. Rule will not be processed.";
		case 102:
			return "";

		default:
			return "(" + this.getClass().getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
