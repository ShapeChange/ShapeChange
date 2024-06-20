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
package de.interactive_instruments.ShapeChange.Target.Diff;

import java.io.File;
import java.util.List;
import java.util.Locale;
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
import de.interactive_instruments.ShapeChange.ModelDiff.DiffElement2.ElementChangeType;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class DiffTargetConfigurationValidator extends AbstractConfigurationValidator {

    protected SortedSet<String> allowedParametersWithStaticNames = new TreeSet<>(Stream
	    .of(DiffTargetConstants.PARAM_REFERENCE_MODEL_USER, DiffTargetConstants.PARAM_REFERENCE_MODEL_PWD,
		    DiffTargetConstants.PARAM_REFERENCE_MODEL_FILENAME_OR_CONSTRING,
		    DiffTargetConstants.PARAM_REFERENCE_MODEL_TYPE, DiffTargetConstants.PARAM_DIFF_ELEMENT_TYPES,
		    DiffTargetConstants.PARAM_TAG_PATTERN, DiffTargetConstants.PARAM_INCLUDE_MODEL_DATA,
		    DiffTargetConstants.PARAM_PRINT_MODEL_ELEMENT_PATHS, DiffTargetConstants.PARAM_AAA_MODEL,
		    DiffTargetConstants.PARAM_RELEVANTE_MODELLARTEN, DiffTargetConstants.PARAM_TAGS_TO_SPLIT)
	    .collect(Collectors.toSet()));
    protected List<Pattern> regexForAllowedParametersWithDynamicNames = null;

    @Override
    public boolean isValid(ProcessConfiguration config, Options options, ShapeChangeResult result) {

	boolean isValid = true;

	allowedParametersWithStaticNames.addAll(getCommonTargetParameters());
	isValid = validateParameters(allowedParametersWithStaticNames, regexForAllowedParametersWithDynamicNames,
		config.getParameters().keySet(), result) && isValid;

	// check tag pattern (optional parameter, with default value)
	if (config.hasParameter(DiffTargetConstants.PARAM_TAG_PATTERN)) {
	    try {
		config.parameterAsRegexPattern(DiffTargetConstants.PARAM_TAG_PATTERN,
			DiffTargetConstants.DEFAULT_TAG_PATTERN);
	    } catch (PatternSyntaxException e) {
		result.addError(this, 1, DiffTargetConstants.PARAM_TAG_PATTERN, e.getMessage());
		isValid = false;
	    }
	}

	// check tags to split pattern (optional parameter, with default value)
	if (config.hasParameter(DiffTargetConstants.PARAM_TAGS_TO_SPLIT)) {
	    try {
		config.parameterAsRegexPattern(DiffTargetConstants.PARAM_TAGS_TO_SPLIT, null);
	    } catch (PatternSyntaxException e) {
		result.addError(this, 1, DiffTargetConstants.PARAM_TAGS_TO_SPLIT, e.getMessage());
		isValid = false;
	    }
	}

	/*
	 * check diff element types parameter values (optional parameter, with default
	 * value)
	 */
	List<String> diffElementTypeNames = config.parameterAsStringList(DiffTargetConstants.PARAM_DIFF_ELEMENT_TYPES,
		null, true, true);

	if (config.hasParameter(DiffTargetConstants.PARAM_DIFF_ELEMENT_TYPES) && diffElementTypeNames.isEmpty()) {
	    result.addError(this, 103, DiffTargetConstants.PARAM_DIFF_ELEMENT_TYPES);
	    isValid = false;
	} else {

	    SortedSet<String> invalidValues = new TreeSet<>();
	    for (String detn : diffElementTypeNames) {

		try {

		    ElementChangeType.valueOf(detn.toUpperCase(Locale.ENGLISH));

		} catch (IllegalArgumentException e) {
		    invalidValues.add(detn);
		}
	    }
	    if (!invalidValues.isEmpty()) {
		result.addError(this, 104, DiffTargetConstants.PARAM_DIFF_ELEMENT_TYPES,
			String.join(", ", invalidValues));
		isValid = false;
	    }
	}

	/*
	 * Check the two parameters required for loading the reference model. Note that
	 * the other two parameters - username and password - are optional and thus not
	 * checked here.
	 * 
	 * In the future, the DefaultModelProvider might be enhanced to perform checks
	 * that a given model file can be read.
	 */
	String rmt = config.parameterAsString(DiffTargetConstants.PARAM_REFERENCE_MODEL_TYPE, null, false, true);
	if (StringUtils.isBlank(rmt)) {
	    result.addError(this, 100, DiffTargetConstants.PARAM_REFERENCE_MODEL_TYPE);
	    isValid = false;
	} else {
	    // no further checks for reference model type here
	}

	String mdl = config.parameterAsString(DiffTargetConstants.PARAM_REFERENCE_MODEL_FILENAME_OR_CONSTRING, null,
		false, true);
	if (StringUtils.isBlank(mdl)) {
	    result.addError(this, 100, DiffTargetConstants.PARAM_REFERENCE_MODEL_FILENAME_OR_CONSTRING);
	    isValid = false;
	} else {
	    // ensure that the file exists and can be read
	    File mdlFile = new File(mdl);
	    if (!mdlFile.exists()) {
		result.addError(this, 101, DiffTargetConstants.PARAM_REFERENCE_MODEL_FILENAME_OR_CONSTRING,
			mdlFile.getAbsolutePath());
		isValid = false;
	    } else if (!mdlFile.canRead()) {
		result.addError(this, 102, DiffTargetConstants.PARAM_REFERENCE_MODEL_FILENAME_OR_CONSTRING,
			mdlFile.getAbsolutePath());
		isValid = false;
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
	case 101:
	    return "File identified by parameter '$1$' does not exist. File path is '$2$'";
	case 102:
	    return "File identified by parameter '$1$' cannot be read. File path is '$2$'";
	case 103:
	    return "Parameter '$1$' is set in the configuration, but does not contain an actual value.";
	case 104:
	    return "Parameter '$1$' has invalid value(s): $2$";

	default:
	    return "(DiffTargetConfigurationValidator.java) Unknown message with number: " + mnr;
	}
    }

}
