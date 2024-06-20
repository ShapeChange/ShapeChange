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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange.Transformation.TaggedValues;

import java.util.List;
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
import de.interactive_instruments.ShapeChange.TransformerConfiguration;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class TaggedValueTransformerConfigurationValidator extends AbstractConfigurationValidator {

    protected SortedSet<String> allowedParametersWithStaticNames = new TreeSet<>(
	    Stream.of(TaggedValueTransformer.PARAM_TV_COPYFROMVALUETYPE_TVSTOCOPY,
		    TaggedValueTransformer.PARAM_TV_COPYFROMVALUETYPE_TYPENAMEREGEX,
		    TaggedValueTransformer.PARAM_TV_INHERITANCE_APPEND_LIST,
		    TaggedValueTransformer.PARAM_TV_INHERITANCE_APPEND_SEPARATOR,
		    TaggedValueTransformer.PARAM_TV_INHERITANCE_GENERAL_LIST,
		    TaggedValueTransformer.PARAM_TV_INHERITANCE_OVERWRITE_LIST).collect(Collectors.toSet()));
    protected List<Pattern> regexForAllowedParametersWithDynamicNames = null;

    @Override
    public boolean isValid(ProcessConfiguration config, Options options, ShapeChangeResult result) {

	TransformerConfiguration trfConfig = (TransformerConfiguration) config;

	boolean isValid = true;

	allowedParametersWithStaticNames.addAll(getCommonTransformerParameters());
	isValid = validateParameters(allowedParametersWithStaticNames, regexForAllowedParametersWithDynamicNames,
		config.getParameters().keySet(), result) && isValid;

	if (trfConfig.hasRule(TaggedValueTransformer.RULE_TV_INHERITANCE)) {

	    // check that required parameter is set
	    List<String> generalIn = config
		    .parameterAsStringList(TaggedValueTransformer.PARAM_TV_INHERITANCE_GENERAL_LIST, null, true, true);
	    List<String> overwriteIn = config.parameterAsStringList(
		    TaggedValueTransformer.PARAM_TV_INHERITANCE_OVERWRITE_LIST, null, true, true);
	    List<String> appendIn = config
		    .parameterAsStringList(TaggedValueTransformer.PARAM_TV_INHERITANCE_APPEND_LIST, null, true, true);

	    if (generalIn.isEmpty()) {
		isValid = false;
		result.addError(this, 100, TaggedValueTransformer.PARAM_TV_INHERITANCE_GENERAL_LIST,
			TaggedValueTransformer.RULE_TV_INHERITANCE);
	    }

	    /*
	     * determine if overwrite and append list contain tagged values that would be
	     * ignored
	     */
	    SortedSet<String> generalTVs = new TreeSet<String>();
	    SortedSet<String> overwriteTVs = new TreeSet<String>();
	    SortedSet<String> appendTVs = new TreeSet<String>();

	    for (String tv : generalIn) {
		generalTVs.add(options.normalizeTag(tv));
	    }

	    for (String tv : overwriteIn) {

		String normalizedTV = options.normalizeTag(tv);

		if (generalTVs.contains(normalizedTV)) {

		    if (overwriteTVs.contains(normalizedTV)) {

			result.addInfo(this, 101, tv);

		    } else {
			overwriteTVs.add(normalizedTV);
		    }

		} else {
		    result.addWarning(this, 102, tv);
		}
	    }

	    for (String tv : appendIn) {

		String normalizedTV = options.normalizeTag(tv);

		if (generalTVs.contains(normalizedTV)) {

		    if (overwriteTVs.contains(normalizedTV)) {

			result.addWarning(this, 103, tv);

		    } else if (appendTVs.contains(normalizedTV)) {

			result.addInfo(this, 104, tv);

		    } else {

			appendTVs.add(normalizedTV);
		    }

		} else {

		    result.addWarning(this, 105, tv);
		}
	    }
	}

	if (trfConfig.hasRule(TaggedValueTransformer.RULE_TV_COPY_FROM_VALUE_TYPE)) {

	    List<String> tvsToCopy = trfConfig.parameterAsStringList(
		    TaggedValueTransformer.PARAM_TV_COPYFROMVALUETYPE_TVSTOCOPY, null, true, true);

	    if (tvsToCopy.isEmpty()) {
		result.addError(this, 100, TaggedValueTransformer.PARAM_TV_COPYFROMVALUETYPE_TVSTOCOPY,
			TaggedValueTransformer.RULE_TV_COPY_FROM_VALUE_TYPE);
		isValid = false;
	    }

	    String typeNameRegexParamValue = trfConfig.parameterAsString(
		    TaggedValueTransformer.PARAM_TV_COPYFROMVALUETYPE_TYPENAMEREGEX, ".*", false, true);
	    try {
		Pattern.compile(typeNameRegexParamValue);
	    } catch (PatternSyntaxException e) {
		result.addError(this, 10, typeNameRegexParamValue,
			TaggedValueTransformer.PARAM_TV_COPYFROMVALUETYPE_TYPENAMEREGEX, e.getMessage());
		isValid = false;
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

	case 10:
	    return "Syntax exception for regular expression '$1$' of parameter '$2$'. Message is: $3$.";

	// Validation messages
	case 100:
	    return "Parameter '$1$' is required for rule '$2$' but no actual value was found in the configuration.";
	case 101:
	    return "?? Duplicate tag in parameter '" + TaggedValueTransformer.PARAM_TV_INHERITANCE_OVERWRITE_LIST
		    + "': '$1$'. This is not critical, since duplicate tags are ignored, but you may want to clean up the configuration.";
	case 102:
	    return "?? Tag '$1$' specified by parameter '" + TaggedValueTransformer.PARAM_TV_INHERITANCE_OVERWRITE_LIST
		    + "' is not specified by parameter '" + TaggedValueTransformer.PARAM_TV_INHERITANCE_GENERAL_LIST
		    + "'. The tag will be ignored.";
	case 103:
	    return "?? Tag '$1$' specified by parameter '" + TaggedValueTransformer.PARAM_TV_INHERITANCE_APPEND_LIST
		    + "' is also specified by parameter '" + TaggedValueTransformer.PARAM_TV_INHERITANCE_OVERWRITE_LIST
		    + "'. The tag will be ignored in the append list. These tags will therefore be overwritten, which may or may not be the intent.";
	case 104:
	    return "?? Duplicate tag in parameter '" + TaggedValueTransformer.PARAM_TV_INHERITANCE_APPEND_LIST
		    + "': '$1$'. This is not critical, since duplicate tags are ignored, but you may want to clean up the configuration.";
	case 105:
	    return "?? Tag '$1$' specified by parameter '" + TaggedValueTransformer.PARAM_TV_INHERITANCE_APPEND_LIST
		    + "' is not specified by parameter '" + TaggedValueTransformer.PARAM_TV_INHERITANCE_GENERAL_LIST
		    + "'. The tag will be ignored.";

	case 200:
	    return "Required parameter '" + TaggedValueTransformer.PARAM_TV_COPYFROMVALUETYPE_TVSTOCOPY
		    + "' was not set or does not contain any value. '";

	default:
	    return "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	}
    }
}
