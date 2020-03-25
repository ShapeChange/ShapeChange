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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import de.interactive_instruments.ShapeChange.ConfigurationValidator;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessConfiguration;
import de.interactive_instruments.ShapeChange.ProcessRuleSet;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class ConstraintConverterConfigurationValidator implements ConfigurationValidator, MessageSource {

    @Override
    public boolean isValid(ProcessConfiguration config, Options options, ShapeChangeResult result) {

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

	String geomRestrToGeomTVRule = null;

	if (rules.contains(ConstraintConverter.RULE_TRF_CLS_CONSTRAINTS_GEOMRESTRICTIONTOGEOMTV_INCL)) {

	    geomRestrToGeomTVRule = ConstraintConverter.RULE_TRF_CLS_CONSTRAINTS_GEOMRESTRICTIONTOGEOMTV_INCL;

	} else if (rules.contains(ConstraintConverter.RULE_TRF_CLS_CONSTRAINTS_GEOMRESTRICTIONTOGEOMTV_EXCL)) {

	    geomRestrToGeomTVRule = ConstraintConverter.RULE_TRF_CLS_CONSTRAINTS_GEOMRESTRICTIONTOGEOMTV_EXCL;
	}

	if (geomRestrToGeomTVRule != null) {

	    // check that required parameters are set

	    String geomRepConstrRegex = config.parameterAsString(ConstraintConverter.PARAM_GEOM_REP_CONSTRAINT_REGEX,
		    null, false, false);

	    if (geomRepConstrRegex == null) {
		isValid = false;
		result.addError(this, 100, ConstraintConverter.PARAM_GEOM_REP_CONSTRAINT_REGEX, geomRestrToGeomTVRule);
	    } else {

		// ensure that regex is valid
		try {
		    Pattern.compile(geomRepConstrRegex);
		} catch (PatternSyntaxException e) {
		    isValid = false;
		    result.addError(this, 4, ConstraintConverter.PARAM_GEOM_REP_CONSTRAINT_REGEX, geomRepConstrRegex,
			    e.getMessage());
		}
	    }

	    List<String> geomRepTypesIn = config.parameterAsStringList(ConstraintConverter.PARAM_GEOM_REP_TYPES, null,
		    true, true, ";");

	    if (geomRepTypesIn == null || geomRepTypesIn.isEmpty()) {
		isValid = false;
		result.addError(this, 100, ConstraintConverter.PARAM_GEOM_REP_TYPES, geomRestrToGeomTVRule);
	    } else {

		// ensure that individual values are valid
		for (String s : geomRepTypesIn) {
		    String[] map = s.split("=");
		    if (map.length != 2 || map[0].trim().isEmpty() || map[1].trim().isEmpty()) {
			isValid = false;
			result.addError(this, 101, s);
			break;
		    }
		}
	    }

	}

	if (rules.contains(
		ConstraintConverter.RULE_TRF_CLS_CONSTRAINTS_GEOMRESTRICTIONTOGEOMTV_NORESTRICTION_BYVALUETYPE)) {

	    // check that required parameter is set

	    String geomRepValueTypeRegex = config.parameterAsString(ConstraintConverter.PARAM_GEOM_REP_VALUE_TYPE_REGEX,
		    null, false, false);

	    if (geomRepValueTypeRegex == null) {
		isValid = false;
		result.addError(this, 100, ConstraintConverter.PARAM_GEOM_REP_VALUE_TYPE_REGEX,
			ConstraintConverter.RULE_TRF_CLS_CONSTRAINTS_GEOMRESTRICTIONTOGEOMTV_NORESTRICTION_BYVALUETYPE);
	    } else {

		// ensure that regex is valid
		try {
		    Pattern.compile(geomRepValueTypeRegex);
		} catch (PatternSyntaxException e) {
		    isValid = false;
		    result.addError(this, 4, ConstraintConverter.PARAM_GEOM_REP_VALUE_TYPE_REGEX, geomRepValueTypeRegex,
			    e.getMessage());
		}
	    }
	}

	if (rules.contains(ConstraintConverter.RULE_TRF_CLS_CONSTRAINTS_VALUETYPERESTRICTIONTOTV_EXCL)) {

	    // check that required parameter is set

	    String valueTypeRepresentationConstraintRegex = config
		    .parameterAsString(ConstraintConverter.PARAM_VALUETYPE_REP_CONSTRAINT_REGEX, null, false, false);

	    if (valueTypeRepresentationConstraintRegex == null) {
		isValid = false;
		result.addError(this, 100, ConstraintConverter.PARAM_VALUETYPE_REP_CONSTRAINT_REGEX,
			ConstraintConverter.RULE_TRF_CLS_CONSTRAINTS_VALUETYPERESTRICTIONTOTV_EXCL);
	    } else {

		// ensure that regex is valid
		try {
		    Pattern.compile(valueTypeRepresentationConstraintRegex);
		} catch (PatternSyntaxException e) {
		    isValid = false;
		    result.addError(this, 4, ConstraintConverter.PARAM_VALUETYPE_REP_CONSTRAINT_REGEX,
			    valueTypeRepresentationConstraintRegex, e.getMessage());
		}
	    }

	    // check PARAM_VALUETYPE_REP_TYPES

	    String valueTypeRepresentationTypes = config
		    .parameterAsString(ConstraintConverter.PARAM_VALUETYPE_REP_TYPES, null, false, false);

	    if (valueTypeRepresentationTypes == null) {
		isValid = false;
		result.addError(this, 100, ConstraintConverter.PARAM_VALUETYPE_REP_TYPES,
			ConstraintConverter.RULE_TRF_CLS_CONSTRAINTS_VALUETYPERESTRICTIONTOTV_EXCL);
	    } else {

		// ensure that value is valid

		Pattern valuePattern = Pattern.compile(
			"^\\s*(\\w+)\\s*\\{\\s*(\\w+)\\s*(=\\s*\\w+\\s*)?(,\\s*(\\w+)\\s*(=\\s*\\w+\\s*)?)*\\}\\s*(;\\s*(\\w+)\\s*\\{\\s*(\\w+)\\s*(=\\s*\\w+\\s*)?(,\\s*(\\w+)\\s*(=\\s*\\w+\\s*)?)*\\}\\s*)*$");

		Matcher matcher = valuePattern.matcher(valueTypeRepresentationTypes);

		if (!matcher.matches()) {
		    isValid = false;
		    result.addError(this, 102, ConstraintConverter.PARAM_VALUETYPE_REP_TYPES);
		}
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
	case 101:
	    return "Parameter '" + ConstraintConverter.PARAM_GEOM_REP_TYPES
		    + "' is malformed. Multiple values must be separated by semicolon, and each value must contain two non-empty strings, separated by '='. Found: '$1$' (check the other values as well).";
	case 102:
	    return "Parameter '$1$' is invalid. Check the structure of the parameter value.";

	default:
	    return "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	}
    }
}
