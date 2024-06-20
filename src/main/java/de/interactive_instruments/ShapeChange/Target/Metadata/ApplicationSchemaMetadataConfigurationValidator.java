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
 * (c) 2002-2019 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target.Metadata;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.interactive_instruments.ShapeChange.AbstractConfigurationValidator;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessConfiguration;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class ApplicationSchemaMetadataConfigurationValidator extends AbstractConfigurationValidator {

    protected SortedSet<String> allowedParametersWithStaticNames = new TreeSet<>(
	    Stream.of(ApplicationSchemaMetadata.PARAM_INHERITED_PROPERTIES,
		    ApplicationSchemaMetadata.PARAM_TAG_NAME_REGEX, ApplicationSchemaMetadata.PARAM_TAG_VALUE_REGEX,
		    ApplicationSchemaMetadata.PARAM_TYPES_FOR_TYPE_USAGE_IDENTIFICATION).collect(Collectors.toSet()));
    protected List<Pattern> regexForAllowedParametersWithDynamicNames = null;

    @Override
    public boolean isValid(ProcessConfiguration config, Options options, ShapeChangeResult result) {

	boolean isValid = true;

	allowedParametersWithStaticNames.addAll(getCommonTargetParameters());
	isValid = validateParameters(allowedParametersWithStaticNames, regexForAllowedParametersWithDynamicNames,
		config.getParameters().keySet(), result) && isValid;

	if (config.getAllRules().contains(ApplicationSchemaMetadata.RULE_ALL_IDENTIFY_TYPE_USAGE)) {

	    Set<String> typesForTypeUsage = new HashSet<>(
		    options.parameterAsStringList(ApplicationSchemaMetadata.class.getName(),
			    ApplicationSchemaMetadata.PARAM_TYPES_FOR_TYPE_USAGE_IDENTIFICATION, null, true, true));

	    if (typesForTypeUsage.isEmpty()) {

		isValid = false;

		result.addError(this, 3, ApplicationSchemaMetadata.RULE_ALL_IDENTIFY_TYPE_USAGE,
			ApplicationSchemaMetadata.PARAM_TYPES_FOR_TYPE_USAGE_IDENTIFICATION);
	    }
	}

	if (config.getAllRules()
		.contains(ApplicationSchemaMetadata.RULE_ALL_IDENTIFY_PROPERTIES_WITH_SPECIFIC_TAGGED_VALUES)) {

	    // check required parameter PARAM_TAG_NAME_REGEX
	    try {
		Pattern tagNamePattern = config.parameterAsRegexPattern(ApplicationSchemaMetadata.PARAM_TAG_NAME_REGEX,
			null);
		if (tagNamePattern == null) {
		    isValid = false;
		    result.addError(this, 100, ApplicationSchemaMetadata.PARAM_TAG_NAME_REGEX,
			    ApplicationSchemaMetadata.RULE_ALL_IDENTIFY_PROPERTIES_WITH_SPECIFIC_TAGGED_VALUES);
		}
	    } catch (PatternSyntaxException e) {
		isValid = false;
		result.addError(this, 1, ApplicationSchemaMetadata.PARAM_TAG_NAME_REGEX, e.getMessage());
	    }

	    // check optional parameter PARAM_TAG_VALUE_REGEX
	    try {
		config.parameterAsRegexPattern(ApplicationSchemaMetadata.PARAM_TAG_VALUE_REGEX, null);
	    } catch (PatternSyntaxException e) {
		isValid = false;
		result.addError(this, 1, ApplicationSchemaMetadata.PARAM_TAG_VALUE_REGEX, e.getMessage());
	    }
	}

	return isValid;
    }

    @Override
    public String message(int mnr) {

	switch (mnr) {

	case 1:
	    return "Syntax exception while compiling the regular expression defined by target parameter '$1$': '$2$'.";
	case 2:
	    return "Output directory '$1$' does not exist or is not accessible.";
	case 3:
	    return "Rule '$1$' is contained in the target configuration. Parameter '$2$' required by that rule was not provided or is invalid. Provide a valid value for this parameter.";
	case 100:
	    return "Parameter '$1$' is required for rule '$2$' but no actual value was found in the configuration.";

	default:
	    return "(ApplicationSchemaMetadata.java) Unknown message with number: " + mnr;
	}
    }

}
