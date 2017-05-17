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
 * (c) 2002-2016 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Transformation.TypeConversion;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessRuleSet;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.StructuredNumber;
import de.interactive_instruments.ShapeChange.TransformerConfiguration;
import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Stereotypes;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericAssociationInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericClassInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericPropertyInfo;
import de.interactive_instruments.ShapeChange.Transformation.Transformer;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class TypeConverter implements Transformer, MessageSource {

	public static final String TV_DISSOLVE_ASSOCIATION = "dissolveAssociation";

	/**
	 * Define a suffix to be added to the names of attributes that have been
	 * transformed from association roles. Applies to
	 * {@value #RULE_DISSOLVE_ASSOCIATIONS}. Default value is the empty string.
	 */
	public static final String PARAM_DISSOLVE_ASSOCIATIONS_ATTRIBUTE_NAME_SUFFIX = "attributeNameSuffix";

	/**
	 * Define the type to set for attributes that have been transformed from
	 * association roles. Applies to {@value #RULE_DISSOLVE_ASSOCIATIONS}.
	 * Default value is {@value #DEFAULT_DISSOLVE_ASSOCIATIONS_ATTRIBUTE_TYPE}.
	 */
	public static final String PARAM_DISSOLVE_ASSOCIATIONS_ATTRIBUTE_TYPE = "attributeType";
	public static final String DEFAULT_DISSOLVE_ASSOCIATIONS_ATTRIBUTE_TYPE = "CharacterString";

	public static final String RULE_ENUMERATION_TO_CODELIST = "rule-trf-enumeration-to-codelist";

	/**
	 * Dissolves associations that are navigable from types in the schemas
	 * selected for processing. Navigable roles of such associations are
	 * transformed into attributes. A suffix can be added to the names of these
	 * attributes, via parameter
	 * {@value #PARAM_DISSOLVE_ASSOCIATIONS_ATTRIBUTE_NAME_SUFFIX}. The type of
	 * these attributes is set to the type defined by parameter
	 * {@value #PARAM_DISSOLVE_ASSOCIATIONS_ATTRIBUTE_TYPE}.
	 * <p>
	 * Associations can be excluded from this transformation by:
	 * <ul>
	 * <li>setting tagged value {@value #TV_DISSOLVE_ASSOCIATION} on the
	 * association with value 'false'</li>
	 * <li>including
	 * {@value #RULE_DISSOLVE_ASSOCIATIONS_EXCLUDE_MANYTOMANY_RELATIONSHIPS} in
	 * the encoding rule</li>
	 * </ul>
	 * If an association that is dissolves has an association class, a warning
	 * is logged and the class is removed.
	 */
	public static final String RULE_DISSOLVE_ASSOCIATIONS = "rule-trf-dissolveAssociations";

	/**
	 * If this rule is included together with
	 * {@value #RULE_DISSOLVE_ASSOCIATIONS}, an association is not dissolved if
	 * it represents a many-to-many relationship between two types (i.e., all
	 * navigable roles have max multiplicity greater than 1).
	 */
	public static final String RULE_DISSOLVE_ASSOCIATIONS_EXCLUDE_MANYTOMANY_RELATIONSHIPS = "rule-trf-dissolveAssociations-excludeManyToManyRelationships";

	/**
	 * If this rule is included together with
	 * {@value #RULE_DISSOLVE_ASSOCIATIONS}, then attributes that result from
	 * dissolving an association and have maximum multiplicity greater than 1
	 * are removed.
	 */
	public static final String RULE_DISSOLVE_ASSOCIATIONS_REMOVE_TRANSFORMED_ROLE_IF_MULTIPLE = "rule-trf-dissolveAssociations-removeTransformedAttributeIfMultiple";

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

		if (rules.contains(RULE_ENUMERATION_TO_CODELIST)) {
			applyRuleEnumerationToCodelist(genModel, trfConfig);
		}

		if (rules.contains(RULE_DISSOLVE_ASSOCIATIONS)) {
			applyRuleDissolveAssociations(genModel, trfConfig);
		}

		// apply post-processing (nothing to do right now)
	}

	private void applyRuleDissolveAssociations(GenericModel genModel,
			TransformerConfiguration trfConfig) {

		String attributeNameSuffix = trfConfig.parameterAsString(
				PARAM_DISSOLVE_ASSOCIATIONS_ATTRIBUTE_NAME_SUFFIX, "", false,
				true);
		String attributeTypeName = trfConfig.parameterAsString(
				PARAM_DISSOLVE_ASSOCIATIONS_ATTRIBUTE_TYPE,
				DEFAULT_DISSOLVE_ASSOCIATIONS_ATTRIBUTE_TYPE, false, true);
		ClassInfo attributeType = genModel.classByName(attributeTypeName);
		Type attributeTypeInfoTemplate = new Type();
		attributeTypeInfoTemplate.name = attributeTypeName;
		if (attributeType != null) {
			attributeTypeInfoTemplate.id = attributeType.id();
		}

		SortedSet<GenericAssociationInfo> associations = new TreeSet<GenericAssociationInfo>();

		/*
		 * Identify associations that are navigable from types in the schemas
		 * selected for processing.
		 */
		for (GenericPropertyInfo genPi : genModel.selectedSchemaProperties()) {
			if (!genPi.isAttribute()) {
				associations.add((GenericAssociationInfo) genPi.association());
			}
		}

		SortedSet<GenericAssociationInfo> associationsToExclude = new TreeSet<GenericAssociationInfo>();

		for (GenericAssociationInfo genAi : associations) {

			/*
			 * Exclude association if tagged value "dissolveAssociation" on the
			 * association is set to 'false'.
			 */
			if ("false".equalsIgnoreCase(
					genAi.taggedValue(TV_DISSOLVE_ASSOCIATION))) {
				associationsToExclude.add(genAi);
			}

			/*
			 * Exclude association if it represents a many-to-many relationship
			 * and rule-trf-dissolveAssociations-excludeManyToManyRelationships
			 * is enabled.
			 */
			if (trfConfig.hasRule(
					RULE_DISSOLVE_ASSOCIATIONS_EXCLUDE_MANYTOMANY_RELATIONSHIPS)
					&& (!genAi.end1().isNavigable()
							|| genAi.end1().cardinality().maxOccurs > 1)
					&& (!genAi.end2().isNavigable()
							|| genAi.end2().cardinality().maxOccurs > 1)) {
				associationsToExclude.add(genAi);
			}
		}

		associations.removeAll(associationsToExclude);

		// Dissolve remaining associations
		for (GenericAssociationInfo genAi : associations) {

			/*
			 * If an association that is dissolved has an association class, log
			 * a warning.
			 */
			if (genAi.assocClass() != null) {
				MessageContext mc = result.addWarning(this, 100,
						genAi.assocClass().name());
				if (mc != null) {
					mc.addDetail(this, 2,
							genAi.assocClass().fullNameInSchema());
				}
			}

			// Dissolve the association
			StructuredNumber end1_sn = null;
			GenericClassInfo end1_inClass = null;

			StructuredNumber end2_sn = null;
			GenericClassInfo end2_inClass = null;

			if (genAi.end1().isNavigable()) {
				end1_sn = genAi.end1().sequenceNumber();
				end1_inClass = (GenericClassInfo) genAi.end1().inClass();
			}

			if (genAi.end2().isNavigable()) {
				end2_sn = genAi.end2().sequenceNumber();
				end2_inClass = (GenericClassInfo) genAi.end2().inClass();
			}

			/*
			 * Transform association roles into attributes, remove possibly
			 * existing association class, remove association
			 */
			genModel.dissolveAssociation(genAi);

			handleAttributeAfterDissolvingAssociation(end1_sn, end1_inClass,
					trfConfig, genModel, attributeTypeInfoTemplate,
					attributeNameSuffix);
			handleAttributeAfterDissolvingAssociation(end2_sn, end2_inClass,
					trfConfig, genModel, attributeTypeInfoTemplate,
					attributeNameSuffix);
		}
	}

	private void handleAttributeAfterDissolvingAssociation(StructuredNumber sn,
			GenericClassInfo ci, TransformerConfiguration trfConfig,
			GenericModel genModel, Type attributeTypeInfoTemplate,
			String attributeNameSuffix) {

		if (sn != null) {

			GenericPropertyInfo attribute = ci.propertyBySequenceNumber(sn);

			if (attribute.cardinality().maxOccurs > 1 && trfConfig.hasRule(
					RULE_DISSOLVE_ASSOCIATIONS_REMOVE_TRANSFORMED_ROLE_IF_MULTIPLE)) {

				genModel.remove(attribute, false);

			} else {

				// Add suffix to attribute name and set attribute type
				attribute.setName(attribute.name() + attributeNameSuffix);
				attribute.setTypeInfo(attributeTypeInfoTemplate.createCopy());
			}
		}
	}

	private void applyRuleEnumerationToCodelist(GenericModel genModel,
			TransformerConfiguration trfConfig) {

		/*
		 * NOTE: This rule converts all enumerations found in the model to code
		 * lists. If a restriction is desired, for example to enumerations that
		 * belong to the schemas selected for processing, or to specifically
		 * identified application schemas (identified by name or target
		 * namespace regex), then that would need to be added (for example
		 * through new parameters).
		 * 
		 * Likewise, no specific tagged values are set. Rules and parameters to
		 * control the tagged values of relevant classes can be added in the
		 * future.
		 * 
		 * Constraints are not updated either.
		 */

		/*
		 * --- update class category ---
		 */
		for (GenericClassInfo genCi : genModel.getGenClasses().values()) {

			if (genCi.category() == Options.ENUMERATION) {
				genCi.setCategory(Options.CODELIST);
				Stereotypes st = genCi.stereotypes();
				st.remove("enumeration");
				st.add("codelist");
				// we need to explicitly set the new Stereotypes cache
				genCi.setStereotypes(st);
			}
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

		// Messages for RULE_DISSOLVE_ASSOCIATIONS
		case 100:
			return "Association class '$1$' will be removed.";

		default:
			return "(" + TypeConverter.class.getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
