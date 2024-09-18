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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.shapechange.core.transformation.profiling;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.shapechange.core.AbstractConfigurationValidator;
import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ProcessConfiguration;
import de.interactive_instruments.shapechange.core.ProcessRuleSet;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class ProfileConstraintTransformerConfigurationValidator extends AbstractConfigurationValidator {

    protected SortedSet<String> allowedParametersWithStaticNames = new TreeSet<>(Stream.of(
	    ProfileConstraintTransformer.PARAM_BASE_SCHEMA_CLASSES_NOT_TO_BE_PROHIBITED_REGEX,
	    ProfileConstraintTransformer.PARAM_BASE_SCHEMA_NAME,
	    ProfileConstraintTransformer.PARAM_BASE_SCHEMA_NAME_REGEX,
	    ProfileConstraintTransformer.PARAM_BASE_SCHEMA_NAMESPACE_REGEX,
	    ProfileConstraintTransformer.PARAM_PROFILE_NAME, ProfileConstraintTransformer.PARAM_PROFILE_SCHEMA_NAME,
	    ProfileConstraintTransformer.PARAM_SUBTYPE_NAME_PREFIX).collect(Collectors.toSet()));
    protected List<Pattern> regexForAllowedParametersWithDynamicNames = null;

    @Override
    public boolean isValid(ProcessConfiguration config, Options options, ShapeChangeResult result) {

	boolean isValid = true;

	allowedParametersWithStaticNames.addAll(getCommonTransformerParameters());
	isValid = validateParameters(allowedParametersWithStaticNames, regexForAllowedParametersWithDynamicNames,
		config.getParameters().keySet(), result) && isValid;

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

	if (rules.contains(ProfileConstraintTransformer.RULE_TRF_CLS_CREATE_GENERAL_OUT_OF_SCOPE_CONSTRAINTS)) {

	    isValid &= checkCombinationOfParameters(
		    ProfileConstraintTransformer.RULE_TRF_CLS_CREATE_GENERAL_OUT_OF_SCOPE_CONSTRAINTS, config, result,
		    ProfileConstraintTransformer.PARAM_BASE_SCHEMA_NAME,
		    ProfileConstraintTransformer.PARAM_BASE_SCHEMA_NAME_REGEX,
		    ProfileConstraintTransformer.PARAM_BASE_SCHEMA_NAMESPACE_REGEX);

	    isValid &= checkRequiredParameter(ProfileConstraintTransformer.PARAM_PROFILE_SCHEMA_NAME,
		    ProfileConstraintTransformer.RULE_TRF_CLS_CREATE_GENERAL_OUT_OF_SCOPE_CONSTRAINTS, config, result);

	    isValid &= checkRequiredParameter(ProfileConstraintTransformer.PARAM_PROFILE_NAME,
		    ProfileConstraintTransformer.RULE_TRF_CLS_CREATE_GENERAL_OUT_OF_SCOPE_CONSTRAINTS, config, result);
	}

	if (rules.contains(
		ProfileConstraintTransformer.RULE_TRF_CLS_PROHIBIT_BASE_SCHEMA_TYPES_WITH_DIRECT_UNSUPPRESSED_PROFILE_SCHEMA_SUBTYPES)) {

	    isValid &= checkCombinationOfParameters(
		    ProfileConstraintTransformer.RULE_TRF_CLS_PROHIBIT_BASE_SCHEMA_TYPES_WITH_DIRECT_UNSUPPRESSED_PROFILE_SCHEMA_SUBTYPES,
		    config, result, ProfileConstraintTransformer.PARAM_BASE_SCHEMA_NAME,
		    ProfileConstraintTransformer.PARAM_BASE_SCHEMA_NAME_REGEX,
		    ProfileConstraintTransformer.PARAM_BASE_SCHEMA_NAMESPACE_REGEX);

	    isValid &= checkRequiredParameter(ProfileConstraintTransformer.PARAM_PROFILE_SCHEMA_NAME,
		    ProfileConstraintTransformer.RULE_TRF_CLS_PROHIBIT_BASE_SCHEMA_TYPES_WITH_DIRECT_UNSUPPRESSED_PROFILE_SCHEMA_SUBTYPES,
		    config, result);

	    isValid &= checkRequiredParameter(ProfileConstraintTransformer.PARAM_PROFILE_NAME,
		    ProfileConstraintTransformer.RULE_TRF_CLS_PROHIBIT_BASE_SCHEMA_TYPES_WITH_DIRECT_UNSUPPRESSED_PROFILE_SCHEMA_SUBTYPES,
		    config, result);
	}

	return isValid;
    }

    private boolean checkCombinationOfParameters(String ruleName, ProcessConfiguration config, ShapeChangeResult result,
	    String... parameterNames) {

	for (String pn : parameterNames) {
	    String paramValue = config.parameterAsString(pn, null, false, true);
	    if (paramValue != null) {
		return true;
	    }
	}

	result.addError(this, 101, StringUtils.join(parameterNames, ", "));
	return false;
    }

    private boolean checkRequiredParameter(String parameterName, String ruleName, ProcessConfiguration config,
	    ShapeChangeResult result) {

	String paramValue = config.parameterAsString(parameterName, null, false, true);

	if (paramValue == null) {
	    result.addError(this, 100, parameterName, ruleName);
	    return false;
	} else {
	    return true;
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
	case 4:
	    return "Syntax exception for regular expression value of configuration parameter '$1$'. Regular expression value was: $2$. Exception message: $3$.";

	// Validation messages
	case 100:
	    return "Parameter '$1$' is required for rule '$2$' but no actual value was found in the configuration.";
	case 101:
	    return "At least one of the parameters '$1$' must be provided.";
	default:
	    return "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	}
    }
}
