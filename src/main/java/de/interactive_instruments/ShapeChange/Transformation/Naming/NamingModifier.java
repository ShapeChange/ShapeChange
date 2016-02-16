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
 * (c) 2002-2014 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Transformation.Naming;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessRuleSet;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.TransformerConfiguration;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericClassInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel;
import de.interactive_instruments.ShapeChange.Transformation.Transformer;

/**
 * Encapsulates the logic for execution of rules that modify the naming of
 * application schema elements.
 * 
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments <dot>
 *         de)
 * 
 */
public class NamingModifier implements Transformer, MessageSource {

	/* ------------------------------------------- */
	/* --- configuration parameter identifiers --- */
	/* ------------------------------------------- */

	/**
	 * Identifier of the parameter that provides the suffix to add.
	 * 
	 * Applies to rule {@value #RULE_TRF_ADD_SUFFIX}.
	 * 
	 * The parameter is optional - the default value is:
	 * {@value #DEFAULT_SUFFIX}.
	 */
	public static final String PARAM_SUFFIX = "suffix";

	/**
	 * Identifier of the parameter that defines the regular expression for
	 * matching model element names to add a suffix.
	 * 
	 * Applies to rule {@value #RULE_TRF_ADD_SUFFIX}.
	 * 
	 * The parameter is required.
	 */
	public static final String PARAM_SUFFIX_REGEX = "modelElementNamesToAddSuffixRegex";

	/* ------------------------ */
	/* --- rule identifiers --- */
	/* ------------------------ */

	/**
	 * Identifies the rule used for adding a suffix to the name of specific
	 * model elements.
	 */
	public static final String RULE_TRF_ADD_SUFFIX = "rule-trf-add-suffix";

	/* -------------------- */
	/* --- other fields --- */
	/* -------------------- */

	/**
	 * Default suffix used for adding a suffix to model element names, in case
	 * that the configuration parameter {@value #PARAM_SUFFIX} is not used.
	 */
	public static final String DEFAULT_SUFFIX = "_";

	private Options options = null;
	private ShapeChangeResult result = null;

	public NamingModifier() {
		// nothing special to do here
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Transformation.Transformer#process(de.interactive_instruments.ShapeChange.Model.Generic.GenericModel,
	 *      de.interactive_instruments.ShapeChange.Options,
	 *      de.interactive_instruments.ShapeChange.TransformerConfiguration,
	 *      de.interactive_instruments.ShapeChange.ShapeChangeResult)
	 */
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

		if (rules.contains(RULE_TRF_ADD_SUFFIX)) {
			applyRuleAddSuffix(genModel, trfConfig);
		}

		// apply post-processing (nothing to do right now)

	}

	/**
	 * Adds the suffix (given by configuration parameter {@value #PARAM_SUFFIX},
	 * or otherwise using the default: {@value #DEFAULT_SUFFIX}) to the name of
	 * all class info objects that match the regular expression given via the
	 * configuration parameter {@value #PARAM_SUFFIX_REGEX}.
	 * 
	 * @param genModel
	 * @param trfConfig
	 */
	private void applyRuleAddSuffix(GenericModel genModel,
			TransformerConfiguration trfConfig) {

		/* --- determine and validate parameter values --- */
		String suffix = trfConfig.getParameterValue(PARAM_SUFFIX);

		if (suffix != null) {

			suffix = suffix.trim();

			if (suffix.length() == 0) {
				MessageContext mc = result.addError(this, 1, PARAM_SUFFIX,
						RULE_TRF_ADD_SUFFIX);
				mc.addDetail(this, 0);
				return;
			}

		} else {
			// use the default suffix
			suffix = DEFAULT_SUFFIX;
		}

		String suffixRegex = trfConfig.getParameterValue(PARAM_SUFFIX_REGEX);

		if (suffixRegex != null) {

			suffixRegex = suffixRegex.trim();

			if (suffixRegex.length() == 0) {
				// the suffix regular expression is required but was not
				// provided
				MessageContext mc = result.addError(this, 1,
						PARAM_SUFFIX_REGEX, RULE_TRF_ADD_SUFFIX);
				mc.addDetail(this, 0);
				return;
			}
		} else {
			// the suffix regular expression is required but was not provided
			MessageContext mc = result.addError(this, 2, PARAM_SUFFIX_REGEX,
					RULE_TRF_ADD_SUFFIX);
			mc.addDetail(this, 0);
			return;
		}

		Pattern suffixPattern = null;

		try {

			suffixPattern = Pattern.compile(suffixRegex);

		} catch (PatternSyntaxException e) {

			MessageContext mc = result.addError(this, 3, PARAM_SUFFIX_REGEX,
					RULE_TRF_ADD_SUFFIX, suffixRegex, e.getMessage());
			mc.addDetail(this, 0);
			return;
		}

		/* --- add suffix --- */
		for (GenericClassInfo genCi : genModel.selectedSchemaClasses()) {

			Matcher matcher = suffixPattern.matcher(genCi.name());

			if (matcher.matches()) {

				String newName = genCi.name() + suffix;

				genModel.updateClassName(genCi,newName);
			}
		}
	}

	public String message(int mnr) {

		switch (mnr) {
		case 0:
			return "Context: class NamingModifier";
		case 1:
			return "No non-empty string value provided for configuration parameter '$1$'. Execution of rule '$2$' aborted.";
		case 2:
			return "Configuration parameter '$1$' required for execution of rule '$2$' was not provided. Execution of rule '$2$' aborted.";
		case 3:
			return "Syntax exception for regular expression value of configuration parameter '$1$' (required for execution of rule '$2$'). Regular expression value was: $3$. Exception message: $4$. Execution of rule '$2$' aborted.";
		default:
			return "(Unknown message)";
		}
	}
}
