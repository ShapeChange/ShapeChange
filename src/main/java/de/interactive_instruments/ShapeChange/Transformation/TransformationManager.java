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
package de.interactive_instruments.ShapeChange.Transformation;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessRuleSet;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.StructuredNumber;
import de.interactive_instruments.ShapeChange.TaggedValueConfigurationEntry;
import de.interactive_instruments.ShapeChange.TaggedValueConfigurationEntry.ModelElementType;
import de.interactive_instruments.ShapeChange.TransformerConfiguration;
import de.interactive_instruments.ShapeChange.Model.AssociationInfo;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Model.Stereotypes;
import de.interactive_instruments.ShapeChange.Model.TaggedValues;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericClassInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericPackageInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericPropertyInfo;

/**
 * Manages the transformation of a model, executing common pre- and
 * postprocessing tasks (e.g. setting of tagged values).
 * 
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 */
public class TransformationManager implements MessageSource {

	public static final String REQ_ALL_TYPES_IDENTIFY_FEATURE_AND_OBJECT_ASSOCIATIONS = "req-trf-all-identify-feature-and-object-associations";
	public static final String RULE_SKIP_CONSTRAINT_VALIDATION = "rule-trf-all-postprocess-skip-constraint-validation";
	public static final String RULE_VALIDATE_PROFILES = "rule-trf-all-postprocess-validate-profiles";

	private Options options = null;
	private ShapeChangeResult result = null;
	private Set<String> rules = null;

	public GenericModel process(GenericModel genModel, Options o,
			TransformerConfiguration trfConfig, ShapeChangeResult r)
			throws ShapeChangeAbortException {

		this.options = o;
		this.result = r;

		@SuppressWarnings("rawtypes")
		Class theClass;
		de.interactive_instruments.ShapeChange.Transformation.Transformer transformer;
		try {
			theClass = Class.forName(trfConfig.getClassName());
			transformer = (de.interactive_instruments.ShapeChange.Transformation.Transformer) theClass
					.newInstance();
		} catch (Exception e) {
			throw new ShapeChangeAbortException(
					"Could not load transformer class '"
							+ trfConfig.getClassName() + " for transformer ID '"
							+ trfConfig.getId() + "'.");
		}

		/*
		 * get the set of all rules defined for the transformation
		 */
		Map<String, ProcessRuleSet> ruleSets = trfConfig.getRuleSets();

		rules = new HashSet<String>();
		if (!ruleSets.isEmpty()) {
			for (ProcessRuleSet ruleSet : ruleSets.values()) {
				if (ruleSet.getAdditionalRules() != null) {
					rules.addAll(ruleSet.getAdditionalRules());
				}
			}
		}

		// execute common pre-processing tasks

		this.preprocess(genModel, trfConfig);

		// execute actual processing
		transformer.process(genModel, o, trfConfig, r);

		// execute common post-processing tasks
		this.postprocess(genModel, trfConfig);

		return genModel;
	}

	/**
	 * Perform pre-processing tasks common to all transformers.
	 * 
	 * @param genModel
	 * @param trfConfig
	 */
	private void preprocess(GenericModel genModel,
			TransformerConfiguration trfConfig) {

		/* Check requirements */
		if (rules.contains(
				REQ_ALL_TYPES_IDENTIFY_FEATURE_AND_OBJECT_ASSOCIATIONS)) {

			result.addInfo(null, 20103,
					REQ_ALL_TYPES_IDENTIFY_FEATURE_AND_OBJECT_ASSOCIATIONS);

			identifyFeatureAndObjectAssociations(genModel, trfConfig);
		}

	}

	/**
	 * Logs occurrences of associations between feature and feature / object
	 * types where one end belongs to a class in a selected schema. Log level is
	 * WARN if the association is navigable from an object to a feature type.
	 * 
	 * @param genModel
	 * @param trfConfig
	 */
	private void identifyFeatureAndObjectAssociations(GenericModel genModel,
			TransformerConfiguration trfConfig) {

		SortedSet<GenericPackageInfo> selSchemas = genModel.selectedSchemas();

		for (GenericPackageInfo selSchema : selSchemas) {

			/*
			 * key: names of association end classes, sorted alphabetically
			 * ascending, with :: as separator. We do not use the Association
			 * name or id because we want to log occurrences of associations in
			 * alphabetical order
			 * 
			 * value: the association
			 */
			Map<String, Set<AssociationInfo>> schemaAssocsByKey = new TreeMap<String, Set<AssociationInfo>>();

			SortedSet<ClassInfo> cis = genModel.classes(selSchema);

			for (ClassInfo ci : cis) {

				SortedMap<StructuredNumber, PropertyInfo> pis = ci.properties();

				for (PropertyInfo pi : pis.values()) {

					if (pi.association() != null) {

						AssociationInfo ai = pi.association();

						PropertyInfo pi1 = ai.end1();
						PropertyInfo pi2 = ai.end2();

						// ensure that this is a relevant association
						if ((pi1.inClass().category() == Options.FEATURE
								|| pi1.inClass().category() == Options.OBJECT)
								&& (pi2.inClass().category() == Options.FEATURE
										|| pi2.inClass()
												.category() == Options.OBJECT)) {

							// relevant association

							// identify key value
							String key;

							String pi1CiName = pi1.inClass().name();
							String pi2CiName = pi2.inClass().name();

							if (pi1CiName.compareTo(pi2CiName) <= 0) {
								key = pi1CiName + "::" + pi2CiName;
							} else {
								key = pi2CiName + "::" + pi1CiName;
							}

							// log association
							if (schemaAssocsByKey.containsKey(key)) {

								schemaAssocsByKey.get(key).add(ai);

							} else {

								Set<AssociationInfo> aiSet = new HashSet<AssociationInfo>();
								aiSet.add(ai);
								schemaAssocsByKey.put(key, aiSet);
							}

						} else {

							// association is not relevant
						}
					}
				}
			}

			if (schemaAssocsByKey.isEmpty()) {

				// log that no relevant associations were found in this schema
				result.addInfo(this, 20104, selSchema.name());

			} else {

				int countAssocs = 0;
				for (Set<AssociationInfo> aiSet : schemaAssocsByKey.values()) {
					countAssocs += aiSet.size();
				}

				result.addInfo(this, 20108, "" + countAssocs, selSchema.name());

				for (Set<AssociationInfo> aiSet : schemaAssocsByKey.values()) {
					for (AssociationInfo ai : aiSet) {

						// determine order in which to print properties
						PropertyInfo pi1, pi2;

						String end1CiName = ai.end1().inClass().name();
						String end2CiName = ai.end2().inClass().name();

						if (end1CiName.compareTo(end2CiName) <= 0) {
							pi1 = ai.end1();
							pi2 = ai.end2();
						} else {
							pi1 = ai.end2();
							pi2 = ai.end1();
						}

						String pi1Ci, pi2Ci;

						StringBuilder sb = new StringBuilder();

						if (pi1.inClass().stereotypes().size() == 1) {
							sb.append("<<");
							sb.append(pi1.inClass().stereotypes().toString());
							sb.append(">> ");
						}
						sb.append(pi1.inClass().name());

						pi1Ci = sb.toString();

						sb = new StringBuilder();

						if (pi2.inClass().stereotypes().size() == 1) {
							sb.append("<<");
							sb.append(pi2.inClass().stereotypes().toString());
							sb.append(">> ");
						}
						sb.append(pi2.inClass().name());

						pi2Ci = sb.toString();

						boolean logLevelWarn = false;

						if ((pi1.isNavigable()
								&& pi1.inClass().category() == Options.OBJECT
								&& pi1.categoryOfValue() == Options.FEATURE)
								|| (pi2.isNavigable()
										&& pi2.inClass()
												.category() == Options.OBJECT
										&& pi2.categoryOfValue() == Options.FEATURE)) {

							logLevelWarn = true;
						}

						MessageContext mc;

						if (logLevelWarn) {
							mc = result.addWarning(this, 20105, pi1Ci, pi2Ci);
						} else {
							mc = result.addInfo(this, 20105, pi1Ci, pi2Ci);
						}

						/*
						 * not printing the name because AssociationInfoEA
						 * constructs a name for the association if it is not
						 * set in the model
						 */

						if (pi1.isNavigable()) {
							mc.addDetail(this, 20107, pi1.name(),
									pi1.inClass().name());
						}
						if (pi2.isNavigable()) {
							mc.addDetail(this, 20107, pi2.name(),
									pi2.inClass().name());
						}
					}
				}
			}
		}
	}

	/**
	 * Perform post-processing tasks common to all transformers.
	 * 
	 * @param genModel
	 * @param trfConfig
	 */
	private void postprocess(GenericModel genModel,
			TransformerConfiguration trfConfig) {

		if (trfConfig.hasParameter(
				TransformationConstants.TRF_CFG_PARAM_SETGENERATIONDATETIMETV)
				&& trfConfig
						.getParameterValue(
								TransformationConstants.TRF_CFG_PARAM_SETGENERATIONDATETIMETV)
						.trim().equalsIgnoreCase("true")) {
			setGenerationDateTimeTaggedValue(genModel);
		}

		if (trfConfig.hasTaggedValues()) {
			setTaggedValues(genModel, trfConfig.getTaggedValues());
		}

		if (!trfConfig.getAllRules()
				.contains(RULE_SKIP_CONSTRAINT_VALIDATION)) {

			result.addInfo(this, 20109);

			genModel.validateConstraints();

		}
	}

	/**
	 * Sets tagged values for specific model elements (packages, classes,
	 * properties).
	 * 
	 * @param genModel
	 * @param taggedValues
	 */
	private void setTaggedValues(GenericModel genModel,
			List<TaggedValueConfigurationEntry> taggedValues) {

		for (PackageInfo pi : genModel.allPackagesFromSelectedSchemas()) {

			GenericPackageInfo genPackage = (GenericPackageInfo) pi;

			TaggedValues genPaTVs = genPackage.taggedValuesAll();

			for (TaggedValueConfigurationEntry tvce : taggedValues) {

				if (matchesTaggedValuePatterns(genPackage, tvce)) {

					if (genPaTVs.containsKey(tvce.getName())) {
						// tagged value already exists on model element

						/*
						 * if the tagged value configuration contains an actual
						 * value, use it - otherwise use the existing value(s)
						 */
						if (tvce.hasValue()) {
							genPaTVs.put(tvce.getName(), tvce.getValue());
						}

					} else {
						// tagged value does not exist on model element
						/*
						 * if the tagged value configuration contains an actual
						 * value, use it - otherwise use the empty string
						 */
						genPaTVs.put(tvce.getName(),
								tvce.hasValue() ? tvce.getValue() : "");
					}
				}
			}

			genPackage.setTaggedValues(genPaTVs, true);
		}

		for (GenericClassInfo genCi : genModel.selectedSchemaClasses()) {

			TaggedValues genCiTVs = genCi.taggedValuesAll();

			for (TaggedValueConfigurationEntry tvce : taggedValues) {

				if (matchesTaggedValuePatterns(genCi, tvce)) {

					if (genCiTVs.containsKey(tvce.getName())) {
						// tagged value already exists on model element

						/*
						 * if the tagged value configuration contains an actual
						 * value, use it - otherwise keep the existing value(s)
						 */
						if (tvce.hasValue()) {
							genCiTVs.put(tvce.getName(), tvce.getValue());
						}

					} else {
						// tagged value does not exist on model element
						/*
						 * if the tagged value configuration contains an actual
						 * value, use it - otherwise use the empty string
						 */
						genCiTVs.put(tvce.getName(),
								tvce.hasValue() ? tvce.getValue() : "");
					}
				}
			}

			genCi.setTaggedValues(genCiTVs, true);
		}

		for (GenericPropertyInfo genPi : genModel.selectedSchemaProperties()) {

			TaggedValues genPiTVs = genPi.taggedValuesAll();

			for (TaggedValueConfigurationEntry tvce : taggedValues) {

				if (matchesTaggedValuePatterns(genPi, tvce)) {

					if (genPiTVs.containsKey(tvce.getName())) {
						// tagged value already exists on model element

						/*
						 * if the tagged value configuration contains an actual
						 * value, use it - otherwise use the existing value(s)
						 */
						if (tvce.hasValue()) {
							genPiTVs.put(tvce.getName(), tvce.getValue());
						}

					} else {
						// tagged value does not exist on model element
						/*
						 * if the tagged value configuration contains an actual
						 * value, use it - otherwise use the empty string
						 */
						genPiTVs.put(tvce.getName(),
								tvce.hasValue() ? tvce.getValue() : "");
					}
				}
			}

			genPi.setTaggedValues(genPiTVs, true);
		}

	}

	/**
	 * Determines if the given info type matches the patterns defined by the
	 * given tagged value configuration entry.
	 * 
	 * @param infoType
	 * @param tvce
	 * @return true if the info type matches the pattern(s) defined in the given
	 *         tagged value configuration entry.
	 */
	private boolean matchesTaggedValuePatterns(Info infoType,
			TaggedValueConfigurationEntry tvce) {

		boolean modelElementStereotypeMatch = true;
		boolean propertyValueTypeStereotypeMatch = true;
		boolean modelElementNameMatch = true;
		boolean applicationSchemaNameMatch = true;
		boolean modelElementTypeMatch = true;

		if (tvce.hasModelElementType()) {

			modelElementTypeMatch = false;
			ModelElementType met = tvce.getModelElementType();

			if ((infoType instanceof AssociationInfo
					&& met.equals(ModelElementType.ASSOCIATION))
					|| (infoType instanceof ClassInfo
							&& met.equals(ModelElementType.CLASS))
					|| (infoType instanceof PackageInfo
							&& met.equals(ModelElementType.PACKAGE))
					|| (infoType instanceof PropertyInfo
							&& met.equals(ModelElementType.PROPERTY))
					|| (infoType instanceof PropertyInfo
							&& met.equals(ModelElementType.ATTRIBUTE)
							&& ((PropertyInfo) infoType).isAttribute())
					|| (infoType instanceof PropertyInfo
							&& met.equals(ModelElementType.ASSOCIATIONROLE)
							&& !((PropertyInfo) infoType).isAttribute())) {
				modelElementTypeMatch = true;
			}
		}

		if (tvce.hasModelElementStereotypePattern()) {

			modelElementStereotypeMatch = false;

			Stereotypes stereotypes = infoType.stereotypes();

			// TBD: what if a model element has no stereotype?
			// stereotypes in info types have been normalized
			if (stereotypes.isEmpty()) {

				String stereotype = null;

				if (infoType instanceof PropertyInfo)
					stereotype = "";
				else if (infoType instanceof ClassInfo)
					stereotype = "";
				else if (infoType instanceof PackageInfo)
					stereotype = "";

				stereotypes = options.stereotypesFactory();
				stereotypes.add(stereotype);
			}

			Pattern pattern = tvce.getModelElementStereotypePattern();

			for (String stereotype : stereotypes.asArray()) {

				Matcher matcher = pattern.matcher(stereotype);

				if (matcher.matches()) {
					modelElementStereotypeMatch = true;
					result.addInfo(this, 20112, stereotype, pattern.pattern());
					break;
				} else {
					result.addInfo(this, 20113, stereotype, pattern.pattern());
				}
			}
		}

		if (tvce.hasPropertyValueTypeStereotypePattern()
				&& infoType instanceof PropertyInfo) {

			PropertyInfo pi = (PropertyInfo) infoType;

			/*
			 * Try to get the value type from the model
			 */
			Model model = pi.model();

			ClassInfo valueType = null;
			if (pi.typeInfo().id != null) {
				valueType = model.classById(pi.typeInfo().id);
			}
			if (valueType == null && pi.typeInfo().name != null) {
				valueType = model.classByName(pi.typeInfo().name);
			}

			if (valueType != null) {

				propertyValueTypeStereotypeMatch = false;

				Stereotypes stereotypes = valueType.stereotypes();

				// TBD: what if a model element has no stereotype?
				// stereotypes in info types have been normalized
				if (stereotypes.isEmpty()) {
					stereotypes = options.stereotypesFactory();
					stereotypes.add("");
				}

				Pattern pattern = tvce.getPropertyValueTypeStereotypePattern();

				for (String stereotype : stereotypes.asArray()) {

					Matcher matcher = pattern.matcher(stereotype);

					if (matcher.matches()) {
						propertyValueTypeStereotypeMatch = true;
						result.addInfo(this, 20112, stereotype, pattern.pattern());
						break;
					} else {
						result.addInfo(this, 20113, stereotype, pattern.pattern());
					}
				}
			}
		}

		if (tvce.hasModelElementNamePattern()) {

			modelElementNameMatch = false;

			Pattern pattern = tvce.getModelElementNamePattern();
			Matcher matcher = pattern
					.matcher(infoType.name());
			if (matcher.matches()) {
				modelElementNameMatch = true;
				result.addInfo(this, 20112, infoType.name(), pattern.pattern());
			} else {
				result.addInfo(this, 20113, infoType.name(), pattern.pattern());
			}
		}

		if (tvce.hasApplicationSchemaNamePattern()) {

			applicationSchemaNameMatch = false;

			Pattern pattern = tvce.getApplicationSchemaNamePattern();
			String applicationSchemaName = determineApplicationSchemaName(infoType);
			Matcher matcher = pattern
					.matcher(applicationSchemaName);

			if (matcher.matches()) {
				applicationSchemaNameMatch = true;
				result.addInfo(this, 20112, applicationSchemaName, pattern.pattern());
			} else {
				result.addInfo(this, 20113, applicationSchemaName, pattern.pattern());
			}
		}

		return modelElementStereotypeMatch && modelElementNameMatch
				&& propertyValueTypeStereotypeMatch && modelElementTypeMatch
				&& applicationSchemaNameMatch;
	}

	private String determineApplicationSchemaName(Info infoType) {

		PackageInfo pi = null;

		if (infoType instanceof PackageInfo) {

			pi = (PackageInfo) infoType;

		} else if (infoType instanceof ClassInfo) {

			ClassInfo ci = (ClassInfo) infoType;
			pi = ci.pkg();

		} else if (infoType instanceof PropertyInfo) {

			PropertyInfo propI = (PropertyInfo) infoType;
			pi = propI.inClass().pkg();

		} else {
			result.addWarning(this, 20101, infoType.name());
		}

		if (pi != null) {
			PackageInfo piAS = identifyApplicationSchema(pi);
			if (piAS != null) {
				return piAS.name();
			}
		}

		/*
		 * if we got here we could not find an application schema, log a warning
		 * but continue
		 */
		result.addWarning(this, 20100, infoType.name());

		return "";
	}

	private PackageInfo identifyApplicationSchema(PackageInfo pi) {

		if (pi.isAppSchema()) {

			return pi;

		} else {

			if (pi.owner() != null) {
				return identifyApplicationSchema(pi.owner());
			} else {
				return null;
			}
		}
	}

	/**
	 * Sets the tagged value "generationDateTime" on all selected application
	 * schemas in the model, creating the tagged value if necessary.
	 * 
	 * @param genModel
	 */
	private void setGenerationDateTimeTaggedValue(GenericModel genModel) {

		SortedSet<GenericPackageInfo> appSchema = genModel.selectedSchemas();

		for (GenericPackageInfo genPi : appSchema) {

			TaggedValues genPiTVs = genPi.taggedValuesAll();

			TimeZone tz = TimeZone.getTimeZone("UTC");
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			df.setTimeZone(tz);
			String currentTime = df.format(new Date());

			String[] val = new String[1];
			val[0] = currentTime;
			genPiTVs.put(TransformationConstants.TRF_TV_NAME_GENERATIONDATETIME,
					val);

			genPi.setTaggedValues(genPiTVs, false);
		}

	}

	@Override
	public String message(int mnr) {

		/*
		 * NOTE: A leading ?? in a message text suppresses multiple appearance
		 * of a message in the output.
		 */
		switch (mnr) {

		case 20100:
			return "Could not find application schema for Info type '$1$'";
		case 20101:
			return "Class type of Info object '$1$' not recognized by logic to determine the name of its application schema";
		case 20102:
			return "Value of configuration parameter '$1$' after parsing is '$2$'.";
		case 20104:
			return "No associations between feature and feature / object types found in schema '$1$'.";
		case 20105:
			return "Association exists between '$1$' and '$2$'.";
		case 20106:
			return "Association name is '$1$'.";
		case 20107:
			return "Navigable via property '$1$' of class '$2$'.";
		case 20108:
			return "$1$ associations between feature and feature / object types found in schema '$2$'.";
		case 20109:
			return "---------- TransformationManager postprocessing: validating constraints ----------";
		case 20110:
			return "The constraint '$1$' on '$2$' will be converted into a simple TextConstraint.";
		case 20111:
			return "The constraint '$1$' on '$2$' was not recognized as a constraint to be validated.";
		case 20112:
			return "??'$1$' matches regex '$2$'";
		case 20113:
			return "??'$1$' does not match regex '$2$'";
		default:
			return "(" + this.getClass().getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
