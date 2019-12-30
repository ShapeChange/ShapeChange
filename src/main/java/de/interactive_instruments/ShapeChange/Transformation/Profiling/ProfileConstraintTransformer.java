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
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessRuleSet;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.StructuredNumber;
import de.interactive_instruments.ShapeChange.TransformerConfiguration;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.Descriptors;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericClassInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericOclConstraint;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericPackageInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericPropertyInfo;
import de.interactive_instruments.ShapeChange.Transformation.Transformer;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
public class ProfileConstraintTransformer
		implements Transformer, MessageSource {

	/* ------------------------------------------- */
	/* --- configuration parameter identifiers --- */
	/* ------------------------------------------- */

	public static final String PARAM_BASE_SCHEMA_NAME = "baseSchemaName";
	public static final String PARAM_BASE_SCHEMA_NAME_REGEX = "baseSchemaNameRegex";
	public static final String PARAM_BASE_SCHEMA_NAMESPACE_REGEX = "baseSchemaNamespaceRegex";

	public static final String PARAM_PROFILE_SCHEMA_NAME = "profileSchemaName";
	public static final String PARAM_PROFILE_NAME = "profileName";
	public static final String PARAM_SUBTYPE_NAME_PREFIX = "subtypeNamePrefix";

	public static final String PARAM_BASE_SCHEMA_CLASSES_NOT_TO_BE_PROHIBITED_REGEX = "baseSchemaClassesNotToBeProhibitedRegex";

	/* ------------------------ */
	/* --- rule identifiers --- */
	/* ------------------------ */

	public static final String RULE_TRF_CLS_CREATE_GENERAL_OUT_OF_SCOPE_CONSTRAINTS = "rule-trf-cls-createGeneralOutOfScopeConstraints";
	public static final String RULE_TRF_CLS_PROHIBIT_BASE_SCHEMA_TYPES_WITH_DIRECT_UNSUPPRESSED_PROFILE_SCHEMA_SUBTYPES = "rule-trf-cls-prohibitBaseSchemaTypesWithDirectUnsuppressedProfileSchemaSubtypes";

	public static final String TV_PROHIBITED_IN_PROFILE_SCHEMA = "prohibitedInProfile";

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

		if (rules.contains(
				RULE_TRF_CLS_PROHIBIT_BASE_SCHEMA_TYPES_WITH_DIRECT_UNSUPPRESSED_PROFILE_SCHEMA_SUBTYPES)) {

			result.addInfo(null, 20103,
					RULE_TRF_CLS_PROHIBIT_BASE_SCHEMA_TYPES_WITH_DIRECT_UNSUPPRESSED_PROFILE_SCHEMA_SUBTYPES);
			applyRuleProhibitBaseSchemaTypesWithDirectUnsuppressedProfileSchemaSubtypes(
					genModel, trfConfig, rules);
		}

		// apply post-processing (nothing to do right now)
	}

	private void applyRuleProhibitBaseSchemaTypesWithDirectUnsuppressedProfileSchemaSubtypes(
			GenericModel genModel, TransformerConfiguration config,
			Set<String> rules) {

		/*
		 * NOTE: Parameter checks are performed by the configuration validator.
		 */

		String baseSchemaClassesNotToBeProhibitedRegex = config
				.getParameterValue(
						PARAM_BASE_SCHEMA_CLASSES_NOT_TO_BE_PROHIBITED_REGEX);

		// Get base schemas
		SortedSet<PackageInfo> baseSchemas = getBaseSchemas(genModel, config);

		if (baseSchemas.isEmpty()) {
			result.addError(this, 100);
			return;
		}

		String profileSchemaName = config
				.getParameterValue(PARAM_PROFILE_SCHEMA_NAME);
		String profileName = config.getParameterValue(PARAM_PROFILE_NAME);

		// Get profile schema
		SortedSet<PackageInfo> profileSchemaSet = genModel
				.schemas(profileSchemaName);

		if (profileSchemaSet.isEmpty()) {
			result.addError(this, 100, profileSchemaName);
			return;
		} else if (profileSchemaSet.size() > 1) {
			result.addError(this, 101, profileSchemaName);
			return;
		}

		GenericPackageInfo profileSchema = (GenericPackageInfo) profileSchemaSet
				.first();

		// Get classes of base schemas
		SortedSet<GenericClassInfo> baseSchemaClasses = getBaseSchemaClasses(
				genModel, baseSchemas);

		for (GenericClassInfo baseSchemaClass : baseSchemaClasses) {

			if (!baseSchemaClass.isAbstract()
					&& baseSchemaClass.profiles().hasProfile(profileName)
					&& baseSchemaClass.category() != Options.ENUMERATION
					&& baseSchemaClass.category() != Options.CODELIST
					&& (StringUtils
							.isBlank(baseSchemaClassesNotToBeProhibitedRegex)
							|| !baseSchemaClass.name().matches(
									baseSchemaClassesNotToBeProhibitedRegex))) {

				SortedSet<GenericClassInfo> profileSchemaSubtypes = getDirectUnsuppressedSubtypesFromProfileSchema(
						baseSchemaClass, profileSchema);

				/*
				 * If one of the direct subtypes belongs to the profile schema
				 * and is not suppressed, create a constraint that prohibits the
				 * base schema class.
				 */

				if (!profileSchemaSubtypes.isEmpty()) {

					String pss_names = profileSchemaSubtypes.stream()
							.map(ClassInfo::name).sorted()
							.collect(Collectors.joining(", "));

					/*
					 * Get or create a suppressed subtype in the profile schema.
					 */
					GenericClassInfo suppressedSubtype = getOrCreateSuppressedSubtypeInProfileSchema(
							(GenericClassInfo) baseSchemaClass, profileSchema,
							genModel, config);

					String constrName = baseSchemaClass.name() + "_prohibited";
					String constrStatus = "Approved";

					String constrText = "/*" + baseSchemaClass.name()
							+ " is prohibited. Use (one of) the following type(s) instead: "
							+ pss_names + ".*/\r\n" + "inv: "
							+ "self->isEmpty()";

					GenericOclConstraint con = new GenericOclConstraint(
							suppressedSubtype, constrName, constrStatus,
							constrText);

					List<Constraint> newCons = new ArrayList<>();
					newCons.add(con);

					suppressedSubtype.addConstraints(newCons);

					baseSchemaClass.setTaggedValue(
							TV_PROHIBITED_IN_PROFILE_SCHEMA, "true", false);
				}
			}
		}

	}

	private SortedSet<GenericClassInfo> getBaseSchemaClasses(
			GenericModel genModel, SortedSet<PackageInfo> baseSchemas) {

		SortedSet<GenericClassInfo> result = new TreeSet<>();

		for (PackageInfo bs : baseSchemas) {

			for (ClassInfo ci : genModel.classes(bs)) {
				result.add((GenericClassInfo) ci);
			}
		}

		return result;
	}

	/**
	 * Identify the base schemas. For a schema to be a base schema, it must
	 * match the parameters {@value #PARAM_BASE_SCHEMA_NAME},
	 * {@value #PARAM_BASE_SCHEMA_NAME_REGEX}, and
	 * {@value #PARAM_BASE_SCHEMA_NAMESPACE_REGEX}.
	 * 
	 * @param genModel
	 * @param config
	 * @return list of base schemas; can be empty but not <code>null</code>
	 */
	private SortedSet<PackageInfo> getBaseSchemas(GenericModel genModel,
			TransformerConfiguration config) {

		String baseSchemaName = config
				.getParameterValue(PARAM_BASE_SCHEMA_NAME);
		String baseSchemaNameRegex = config
				.getParameterValue(PARAM_BASE_SCHEMA_NAME_REGEX);
		String baseSchemaNamespaceRegex = config
				.getParameterValue(PARAM_BASE_SCHEMA_NAMESPACE_REGEX);

		SortedSet<PackageInfo> baseSchemas = new TreeSet<>();

		for (PackageInfo schema : genModel.schemas(null)) {

			String name = schema.name();
			String ns = schema.targetNamespace();

			if ((StringUtils.isNotBlank(baseSchemaName)
					&& !baseSchemaName.equals(name))
					|| (StringUtils.isNotBlank(baseSchemaNameRegex)
							&& !name.matches(baseSchemaNameRegex))
					|| (StringUtils.isNotBlank(baseSchemaNamespaceRegex)
							&& !ns.matches(baseSchemaNamespaceRegex))) {
				// skip this schema
			} else {
				baseSchemas.add(schema);
			}
		}

		return baseSchemas;
	}

	/**
	 * @param ci
	 * @param profileSchema
	 * @return <code>true</code> if the given class has a direct subtype from
	 *         the profile schema which is not suppressed; else
	 *         <code>false</code>
	 */
	private boolean hasDirectUnsuppressedSubtypeFromProfileSchema(ClassInfo ci,
			GenericPackageInfo profileSchema) {

		if (this.getDirectUnsuppressedSubtypesFromProfileSchema(ci,
				profileSchema).isEmpty()) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * @param ci
	 * @param profileSchema
	 * @return the direct unsuppressed subtypes of the given class that belong
	 *         to the given schema; can be empty but not <code>null</code>
	 */
	private SortedSet<GenericClassInfo> getDirectUnsuppressedSubtypesFromProfileSchema(
			ClassInfo ci, GenericPackageInfo profileSchema) {

		/*
		 * get direct (suppressed and unsuppressed) subtypes from the profile
		 * schema
		 */
		SortedSet<GenericClassInfo> result = this
				.getDirectSubtypesInProfileSchema(ci, profileSchema);

		// remove suppressed classes, keep unsuppressed ones
		result.removeIf(p -> p.suppressed());

		return result;
	}

	/**
	 * @param ci
	 * @param profileSchema
	 * @return the direct suppressed subtypes of the given class that belong to
	 *         the given schema; can be empty but not <code>null</code>
	 */
	private SortedSet<GenericClassInfo> getDirectSuppressedSubtypesFromProfileSchema(
			ClassInfo ci, GenericPackageInfo profileSchema) {

		/*
		 * get direct (suppressed and unsuppressed) subtypes from the profile
		 * schema
		 */
		SortedSet<GenericClassInfo> result = this
				.getDirectSubtypesInProfileSchema(ci, profileSchema);

		// remove unsuppressed classes, keep suppressed ones
		result.removeIf(p -> !p.suppressed());

		return result;
	}

	private void applyRuleCreateGeneralOutOfScopeConstraints(
			GenericModel genModel, TransformerConfiguration config,
			Set<String> rules) {

		/*
		 * NOTE: Parameter checks are performed by the configuration validator.
		 */

		String baseSchemaClassesNotToBeProhibitedRegex = config
				.getParameterValue(
						PARAM_BASE_SCHEMA_CLASSES_NOT_TO_BE_PROHIBITED_REGEX);

		// Get base schemas
		SortedSet<PackageInfo> baseSchemas = getBaseSchemas(genModel, config);

		if (baseSchemas.isEmpty()) {
			result.addError(this, 100);
			return;
		}

		String profileSchemaName = config
				.getParameterValue(PARAM_PROFILE_SCHEMA_NAME);
		String profileName = config.getParameterValue(PARAM_PROFILE_NAME);

		// Get profile schema
		SortedSet<PackageInfo> profileSchemaSet = genModel
				.schemas(profileSchemaName);

		if (profileSchemaSet.isEmpty()) {
			result.addError(this, 100, profileSchemaName);
			return;
		} else if (profileSchemaSet.size() > 1) {
			result.addError(this, 101, profileSchemaName);
			return;
		}

		GenericPackageInfo profileSchema = (GenericPackageInfo) profileSchemaSet
				.first();

		/*
		 * Get classes of base schemas
		 */
		SortedSet<GenericClassInfo> baseSchemaClasses = getBaseSchemaClasses(
				genModel, baseSchemas);

		for (GenericClassInfo baseSchemaClass : baseSchemaClasses) {

			/*
			 * Check if the class is irrelevant.
			 */
			if (!baseSchemaClass.profiles().hasProfile(profileName)) {

				/*
				 * Ignore enumerations and code lists.
				 */
				if (baseSchemaClass.category() == Options.ENUMERATION
						|| baseSchemaClass.category() == Options.CODELIST) {
					continue;
				}

				/*
				 * No need to create a constraint to prohibit the use of this
				 * class if it is abstract.
				 */
				if (baseSchemaClass.isAbstract()) {
					continue;
				}

				/*
				 * Check if this irrelevant class has at least one unsuppressed
				 * direct subclass in the profile schema, which would indicate
				 * that the class is in fact relevant.
				 */
				SortedSet<GenericClassInfo> profileSchemaSubtypes = getDirectUnsuppressedSubtypesFromProfileSchema(
						baseSchemaClass, profileSchema);

				if (!profileSchemaSubtypes.isEmpty()) {

					MessageContext mc = result.addWarning(this, 102,
							baseSchemaClass.name(),
							profileSchemaSubtypes.stream().map(ClassInfo::name)
									.sorted()
									.collect(Collectors.joining(", ")));
					if (mc != null) {
						mc.addDetail(this, 1,
								baseSchemaClass.fullNameInSchema());
					}

				} else {

					/*
					 * Get or create a suppressed subtype in the profile schema.
					 */
					GenericClassInfo subtype = getOrCreateSuppressedSubtypeInProfileSchema(
							(GenericClassInfo) baseSchemaClass, profileSchema,
							genModel, config);

					/*
					 * Add an OCL constraint to mark the base schema class as
					 * prohibited.
					 */

					String constrName = baseSchemaClass.name() + "_prohibited";
					String constrStatus = "Approved";

					String constrText = "/*" + baseSchemaClass.name()
							+ " is prohibited.*/\r\n" + "inv: "
							+ "self->isEmpty()";

					GenericOclConstraint con = new GenericOclConstraint(subtype,
							constrName, constrStatus, constrText);

					List<Constraint> newCons = new ArrayList<>();
					newCons.add(con);

					subtype.addConstraints(newCons);

					baseSchemaClass.setTaggedValue(
							TV_PROHIBITED_IN_PROFILE_SCHEMA, "true", false);
				}

			} else {

				/*
				 * So, the class is relevant, i.e. it belongs to the profile.
				 * Now check if the class has irrelevant properties, i.e. some
				 * of its direct (non-inherited) properties do not belong to the
				 * profile.
				 */

				SortedSet<PropertyInfo> irrelevantProps = new TreeSet<>();

				for (PropertyInfo pi : baseSchemaClass.properties().values()) {

					if (!pi.profiles().hasProfile(profileName)) {

						irrelevantProps.add(pi);

						((GenericPropertyInfo) pi).setTaggedValue(
								TV_PROHIBITED_IN_PROFILE_SCHEMA, "true", false);
					}
				}

				if (irrelevantProps.isEmpty()) {
					continue;
				}

				/*
				 * If the base schema class is a code list or enumeration with
				 * irrelevant properties, log a message and continue.
				 */
				if (baseSchemaClass.category() == Options.CODELIST
						|| baseSchemaClass.category() == Options.ENUMERATION) {

					String namesOfIrrelevantProperties = irrelevantProps
							.stream().map(PropertyInfo::name).sorted()
							.collect(Collectors.joining(", "));

					MessageContext mc = result.addInfo(this, 103,
							baseSchemaClass.name(),
							namesOfIrrelevantProperties);
					if (mc != null) {
						mc.addDetail(this, 1,
								baseSchemaClass.fullNameInSchema());
					}

					continue;
				}

				/*
				 * Now, investigate the class hierarchy of the base class (i.e.,
				 * the base class itself and all its direct and indirect
				 * subtypes). Identify all relevant base classes in this
				 * hierarchy, i.e. classes that 1) are not abstract, 2) belong
				 * to the profile (identified via parameter 'profileName'), and
				 * 3) belong to the base schema.
				 */
				SortedSet<ClassInfo> classesInClassHierarchy = baseSchemaClass
						.subtypesInCompleteHierarchy();
				classesInClassHierarchy.add(baseSchemaClass);

				SortedSet<ClassInfo> relevantBaseClassesInClassHierarchy = new TreeSet<>();

				for (ClassInfo ci : classesInClassHierarchy) {
					if (!ci.isAbstract()
							&& ci.profiles().hasProfile(profileName)
							&& baseSchemaClasses.contains(ci)) {
						relevantBaseClassesInClassHierarchy.add(ci);
					}
				}

				for (ClassInfo relevantBaseClass : relevantBaseClassesInClassHierarchy) {

					/*
					 * Create a set of the subtypes of the relevantBaseClass for
					 * which OCL constraints to prohibit irrelevant properties
					 * shall be created. The direct unsuppressed subtypes (from
					 * the profile schema) are definitely part of this set, so
					 * start with them.
					 */
					SortedSet<GenericClassInfo> profileSchemaSubtypesToCreateOCLConstraintsFor = getDirectUnsuppressedSubtypesFromProfileSchema(
							relevantBaseClass, profileSchema);

					/*
					 * The relevantBaseClass should also get OCL constraints to
					 * prohibit the irrelevant properties, via a suppressed
					 * subtype.
					 * 
					 * However, if the rule to prohibit base schema types with
					 * direct unsuppressed profile schema subtypes applies, and
					 * the relevantBaseClass fulfills these criteria, then we do
					 * not want to create any OCL constraints for the
					 * relevantBaseClass. Exceptions can be made via a
					 * configuration parameter.
					 */
					if (rules.contains(
							RULE_TRF_CLS_PROHIBIT_BASE_SCHEMA_TYPES_WITH_DIRECT_UNSUPPRESSED_PROFILE_SCHEMA_SUBTYPES)
							&& hasDirectUnsuppressedSubtypeFromProfileSchema(
									relevantBaseClass, profileSchema)
							&& (StringUtils.isBlank(
									baseSchemaClassesNotToBeProhibitedRegex)
									|| !baseSchemaClass.name().matches(
											baseSchemaClassesNotToBeProhibitedRegex))) {

						/*
						 * Do not create OCL constraints for the
						 * relevantBaseClass itself (via a suppressed subtype in
						 * the profile schema).
						 */
					} else {
						/*
						 * Get or create a suppressed subtype of
						 * relevantBaseClass in the profile schema. Add it to
						 * the set of subtypes for which OCL constraints to
						 * prohibit irrelevant properties are created.
						 */
						GenericClassInfo subtype = getOrCreateSuppressedSubtypeInProfileSchema(
								(GenericClassInfo) relevantBaseClass,
								profileSchema, genModel, config);
						profileSchemaSubtypesToCreateOCLConstraintsFor
								.add(subtype);
					}

					/*
					 * Now create the OCL constraints to prohibit the irrelevant
					 * properties.
					 */
					for (GenericClassInfo profileSchemaSubtype : profileSchemaSubtypesToCreateOCLConstraintsFor) {

						List<Constraint> newCons = new ArrayList<>();

						for (PropertyInfo pi : irrelevantProps) {

							String constrName = pi.name() + "_prohibited";
							String constrStatus = "Approved";

							String constrText = "/*" + pi.name()
									+ " is prohibited.*/\r\n" + "inv: "
									+ pi.name() + "->isEmpty()";

							GenericOclConstraint con = new GenericOclConstraint(
									profileSchemaSubtype, constrName,
									constrStatus, constrText);
							newCons.add(con);
						}

						profileSchemaSubtype.addConstraints(newCons);
					}
				}
			}
		}
	}

	/**
	 * @param ci
	 * @param profileSchema
	 * @return the direct subtypes (suppressed or unsuppressed) of the given
	 *         class that belong to the given schema; can be empty but not
	 *         <code>null</code>
	 */
	private SortedSet<GenericClassInfo> getDirectSubtypesInProfileSchema(
			ClassInfo ci, GenericPackageInfo profileSchema) {

		Model model = ci.model();

		SortedSet<ClassInfo> profileSchemaClasses = model
				.classes(profileSchema);

		SortedSet<GenericClassInfo> result = new TreeSet<>();

		for (String subtypeId : ci.subtypes()) {

			ClassInfo subtype = model.classById(subtypeId);

			if (profileSchemaClasses.contains(subtype)) {

				result.add((GenericClassInfo) subtype);
			}
		}

		return result;
	}

	/**
	 * @param baseClass
	 *            class in the base schema
	 * @param profileSchema
	 *            the profile schema package
	 * @param genModel
	 * @param config
	 * @return A non-abstract class that 1) is a subtype of the baseClass, 2)
	 *         belongs to the profile schema, and 3) has tagged value
	 *         suppress=true. If such a class already exists, it is returned.
	 *         Otherwise, a new class is created, with name being the name of
	 *         the base class, and name prefix as defined by configuration
	 *         parameter {@value #PARAM_SUBTYPE_NAME_PREFIX}.
	 */
	private GenericClassInfo getOrCreateSuppressedSubtypeInProfileSchema(
			GenericClassInfo baseClass, GenericPackageInfo profileSchema,
			GenericModel genModel, TransformerConfiguration config) {

		/*
		 * If a suppressed subtype from the profile schema already exists,
		 * return it.
		 */
		SortedSet<GenericClassInfo> dss = getDirectSuppressedSubtypesFromProfileSchema(
				baseClass, profileSchema);

		if (!dss.isEmpty()) {

			return dss.first();

		} else {

			/*
			 * Otherwise, create a new suppressed subtype
			 */

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
			subtype.setProperties(
					new TreeMap<StructuredNumber, PropertyInfo>());

			subtype.setTaggedValue("suppress", "true", false);

			// finally, set model links and register the class
			subtype.setPkg(profileSchema);

			subtype.addSupertype(baseClass.id());
			baseClass.addSubtype(subtype.id());

			subtype.setSubtypes(null);

			genModel.register(subtype);

			profileSchema.addClass(subtype);

			return subtype;
		}
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

		case 100:
			return "No base schema found in the model. Rule will not be processed.";
		case 101:
			return "";
		case 102:
			return "Base schema class '$1$' is tagged as irrelevant. However, the class has at least one subtype in the profile schema, which indicates that the class is in fact relevant. The (direct and indirect) subtypes in the profile schema are: $2$. No OCL constraint will be created to mark class '$1$' as prohibited.";
		case 103:
			return "Base schema class '$1$' is tagged as relevant and it contains irrelevant properties ($2$). However, the class is a code list or enumeration. This case is currently not supported. No specific OCL constraints will be created to prohibit these properties.";

		default:
			return "(" + this.getClass().getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
