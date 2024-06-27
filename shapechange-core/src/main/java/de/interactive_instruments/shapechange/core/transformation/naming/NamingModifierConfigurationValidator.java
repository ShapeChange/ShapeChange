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
 * (c) 2002-2020 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core.transformation.naming;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.shapechange.core.AbstractConfigurationValidator;
import de.interactive_instruments.shapechange.core.ModelElementSelectionInfo;
import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ProcessConfiguration;
import de.interactive_instruments.shapechange.core.ProcessRuleModelElementSelectionConfigurationEntry;
import de.interactive_instruments.shapechange.core.ShapeChangeParseException;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.ModelElementSelectionInfo.ModelElementType;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class NamingModifierConfigurationValidator extends AbstractConfigurationValidator {

    protected SortedSet<String> allowedParametersWithStaticNames = new TreeSet<>(Stream
	    .of(NamingModifier.PARAM_CAMEL_CASE_TO_UPPER_CASE_TAGGED_VALUES, NamingModifier.PARAM_SUFFIX,
		    NamingModifier.PARAM_SUFFIX_REGEX, NamingModifier.PARAM_SUFFIXES_TO_IGNORE)
	    .collect(Collectors.toSet()));
    protected List<Pattern> regexForAllowedParametersWithDynamicNames = null;

    @Override
    public boolean isValid(ProcessConfiguration config, Options options, ShapeChangeResult result) {

	boolean isValid = true;

	allowedParametersWithStaticNames.addAll(getCommonTransformerParameters());
	isValid = validateParameters(allowedParametersWithStaticNames, regexForAllowedParametersWithDynamicNames,
		config.getParameters().keySet(), result) && isValid;

	if (config.getAllRules().contains(NamingModifier.RULE_TRF_ADD_SUFFIX)) {
	    List<ProcessRuleModelElementSelectionConfigurationEntry> prmesces = new ArrayList<>();

	    try {

		prmesces = ProcessRuleModelElementSelectionConfigurationEntry
			.parseAndValidateConfigurationEntries(config.getAdvancedProcessConfigurations());

		/*
		 * Checks for RULE_TRF_ADD_SUFFIX
		 */
		List<ModelElementSelectionInfo> mesi = prmesces.stream()
			.filter(e -> StringUtils.isNotBlank(e.getRule())
				&& e.getRule().equalsIgnoreCase(NamingModifier.RULE_TRF_ADD_SUFFIX))
			.map(e -> e.getModelElementSelectionInfo()).collect(Collectors.toList());

		if (config.hasParameter(NamingModifier.PARAM_SUFFIX_REGEX)) {

		    if (mesi.size() > 0) {

			isValid = false;
			result.addError(this, 102);

		    } else {

			try {

			    Pattern regex = config.parameterAsRegexPattern(NamingModifier.PARAM_SUFFIX_REGEX, null);
			    /*
			     * that regex is null should not happen since we already established that the
			     * parameter is defined in the configuration
			     */

			} catch (PatternSyntaxException e) {

			    isValid = false;
			    result.addError(this, 103, e.getMessage());
			}
		    }

		} else if (mesi.size() == 0) {

		    isValid = false;
		    result.addError(this, 101);

		} else {

		    if (mesi.stream()
			    .anyMatch(e -> e.getModelElementType() == ModelElementType.ASSOCIATION
				    || e.getModelElementType() == ModelElementType.PACKAGE)) {
			isValid = false;
			result.addError(this, 105);
		    }
		}

	    } catch (ShapeChangeParseException e) {
		isValid = false;
		result.addError(this, 100, e.getMessage());
	    }
	}

	return isValid;
    }

    @Override
    public String message(int mnr) {
	switch (mnr) {

	// Validation messages
	case 100:
	    return "Parameter '" + NamingModifier.PARAM_SUFFIX_REGEX + "' is required for rule '"
		    + NamingModifier.RULE_TRF_ADD_SUFFIX
		    + "' if no ProcessRuleModelElementSelection element is defined for that rule in the advanced process configuration, but no actual value was found in the configuration.";
	case 101:
	    return "Neither parameter '" + NamingModifier.PARAM_SUFFIX_REGEX
		    + "' nor any ProcessRuleModelElementSelection element (in the advanced process configuration) is defined for rule '"
		    + NamingModifier.RULE_TRF_ADD_SUFFIX
		    + "'. Either the (deprecated) parameter or a ProcessRuleModelElementSelection element must be defined.";
	case 102:
	    return "Both parameter '" + NamingModifier.PARAM_SUFFIX_REGEX
		    + "' and one or more ProcessRuleModelElementSelection elements (in the advanced process configuration) are defined for rule '"
		    + NamingModifier.RULE_TRF_ADD_SUFFIX
		    + "'. Either the (deprecated) parameter or a ProcessRuleModelElementSelection element must be defined - not both.";
	case 103:
	    return "Pattern syntax exception encountered while parsing (deprecated) parameter '"
		    + NamingModifier.PARAM_SUFFIX_REGEX + "' for rule '" + NamingModifier.RULE_TRF_ADD_SUFFIX
		    + "'. Fix the regular expression. The exception message is: $1$";

	case 104:
	    return "Invalid ProcessRuleModelElementSelection element(s) encountered. Details: $1$";

	case 105:
	    return "ProcessRuleModelElementSelection element(s) for " + NamingModifier.RULE_TRF_ADD_SUFFIX
		    + " with @modelElementType set to association or package encountered, which is invalid for this rule.";

	default:
	    return "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	}
    }
}
