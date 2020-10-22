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
package de.interactive_instruments.ShapeChange.Target.Codelists;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.ShapeChange.AbstractConfigurationValidator;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessConfiguration;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class CodelistDictionariesMLConfigurationValidator extends AbstractConfigurationValidator {

    protected SortedSet<String> allowedParametersWithStaticNames = new TreeSet<>(
	    Stream.of(CodelistDictionariesML.PARAM_DEFAULT_LANG, CodelistDictionariesML.PARAM_LANGUAGES,
		    CodelistDictionariesML.PARAM_NO_NEWLINE_OMIT, CodelistDictionariesML.PARAM_CLPACKAGENAME,
		    CodelistDictionariesML.PARAM_INFOURL).collect(Collectors.toSet()));
    protected Pattern regexForAllowedParametersWithDynamicNames = Pattern.compile("localeRef_.*");

    @Override
    public boolean isValid(ProcessConfiguration config, Options options, ShapeChangeResult result) {

	boolean isValid = true;

	allowedParametersWithStaticNames.addAll(getCommonTargetParameters());
	isValid = validateParameters(allowedParametersWithStaticNames, regexForAllowedParametersWithDynamicNames,
		config.getParameters().keySet(), result) && isValid;

	String defaultLang = config.parameterAsString(CodelistDictionariesML.PARAM_DEFAULT_LANG, "de", false, true);

	String sLangs = config.parameterAsString(CodelistDictionariesML.PARAM_LANGUAGES, null, false, true);

	String[] langs;
	if (sLangs != null) {
	    langs = sLangs.split(" ");
	} else {
	    langs = new String[] { defaultLang };
	}

	/*
	 * For each language other than the default language, a corresponding localeRef
	 * must be given.
	 */
	for (String lang : langs) {

	    if (!lang.equalsIgnoreCase(defaultLang)) {

		String localeVal = config.parameterAsString("localeRef_" + lang, null, false, true);

		if (StringUtils.isBlank(localeVal)) {
		    isValid = false;
		    result.addError(this, 100, lang);
		}
	    }
	}

	return isValid;
    }

    @Override
    public String message(int mnr) {

	switch (mnr) {

	// 100 - 199: parameter validation
	case 100:
	    return "Configuration parameter for locale reference is missing for language '$1$'. A locale reference must be provided for each language other than the default language.";

	default:
	    return "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	}
    }
}
