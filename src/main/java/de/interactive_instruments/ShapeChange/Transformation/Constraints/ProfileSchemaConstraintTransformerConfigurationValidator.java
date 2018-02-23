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
 * (c) 2002-2018 interactive instruments GmbH, Bonn, Germany
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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.interactive_instruments.ShapeChange.ConfigurationValidator;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessConfiguration;
import de.interactive_instruments.ShapeChange.ProcessRuleSet;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class ProfileSchemaConstraintTransformerConfigurationValidator
		implements ConfigurationValidator, MessageSource {

	@Override
	public boolean isValid(ProcessConfiguration config, Options options,
			ShapeChangeResult result) {

		boolean isValid = true;

		Map<String, ProcessRuleSet> ruleSets = config.getRuleSets();

		// get the set of all rules defined for the transformation
		Set<String> rules = new HashSet<String>();
		if (!ruleSets.isEmpty()) {
			for (ProcessRuleSet ruleSet : ruleSets.values()) {
				if (ruleSet.getAdditionalRules() != null) {
					rules.addAll(ruleSet.getAdditionalRules());
				}
			}
		}

		if (rules.contains(
				ProfileSchemaConstraintTransformer.RULE_TRF_CLS_CREATE_GENERAL_OUT_OF_SCOPE_CONSTRAINTS)) {

			String baseSchemaPackageName = config.parameterAsString(
					ProfileSchemaConstraintTransformer.PARAM_BASE_SCHEMA_PACKAGE_NAME,
					null, false, true);

			if (baseSchemaPackageName == null) {
				isValid = false;
				result.addError(this, 100,
						ProfileSchemaConstraintTransformer.PARAM_BASE_SCHEMA_PACKAGE_NAME,
						ProfileSchemaConstraintTransformer.RULE_TRF_CLS_CREATE_GENERAL_OUT_OF_SCOPE_CONSTRAINTS);
			}

			String profileSchemaPackageName = config.parameterAsString(
					ProfileSchemaConstraintTransformer.PARAM_PROFILE_SCHEMA_PACKAGE_NAME,
					null, false, true);

			if (profileSchemaPackageName == null) {
				isValid = false;
				result.addError(this, 100,
						ProfileSchemaConstraintTransformer.PARAM_PROFILE_SCHEMA_PACKAGE_NAME,
						ProfileSchemaConstraintTransformer.RULE_TRF_CLS_CREATE_GENERAL_OUT_OF_SCOPE_CONSTRAINTS);
			}

			String profileName = config.parameterAsString(
					ProfileSchemaConstraintTransformer.PARAM_PROFILE_NAME, null,
					false, true);

			if (profileName == null) {
				isValid = false;
				result.addError(this, 100,
						ProfileSchemaConstraintTransformer.PARAM_PROFILE_NAME,
						ProfileSchemaConstraintTransformer.RULE_TRF_CLS_CREATE_GENERAL_OUT_OF_SCOPE_CONSTRAINTS);
			}
		}

		return isValid;
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
		case 4:
			return "Syntax exception for regular expression value of configuration parameter '$1$'. Regular expression value was: $2$. Exception message: $3$.";

		// Validation messages
		case 100:
			return "Parameter '$1$' is required for rule '$2$' but no actual value was found in the configuration.";

		default:
			return "(" + this.getClass().getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
