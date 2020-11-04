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
package de.interactive_instruments.ShapeChange.Transformation.Profiling;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;

import de.interactive_instruments.ShapeChange.AbstractConfigurationValidator;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessConfiguration;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ModelDiff.DiffElement.ElementType;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class ProfileLoaderConfigurationValidator extends AbstractConfigurationValidator {

    protected SortedSet<String> allowedParametersWithStaticNames = new TreeSet<>(Stream.of(
	    ProfileLoader.PARAM_DIFF_ELEMENT_TYPES, ProfileLoader.PARAM_INPUT_MODEL_EXPLICIT_PROFILES,
	    ProfileLoader.PARAM_LOAD_MODEL_DIRECTORY, ProfileLoader.PARAM_LOAD_MODEL_FILE_REGEX,
	    ProfileLoader.PARAM_PROCESS_ALL_SCHEMAS, ProfileLoader.PARAM_PROFILES_FOR_CLASSES_WITHOUT_EXPLICIT_PROFILES,
	    ProfileLoader.PARAM_PROFILES_TO_LOAD).collect(Collectors.toSet()));
    protected List<Pattern> regexForAllowedParametersWithDynamicNames = null;

    @Override
    public boolean isValid(ProcessConfiguration config, Options options, ShapeChangeResult result) {

	boolean isValid = true;

	allowedParametersWithStaticNames.addAll(getCommonTransformerParameters());
	isValid = validateParameters(allowedParametersWithStaticNames, regexForAllowedParametersWithDynamicNames,
		config.getParameters().keySet(), result) && isValid;

	// validate PARAM_LOAD_MODEL_FILE_REGEX
	if (config.hasParameter(ProfileLoader.PARAM_LOAD_MODEL_FILE_REGEX)) {

	    try {
		Pattern.compile(config.parameterAsString(ProfileLoader.PARAM_LOAD_MODEL_FILE_REGEX, "", false, true));
	    } catch (PatternSyntaxException e) {
		isValid = false;
		result.addError(this, 103, ProfileLoader.PARAM_LOAD_MODEL_FILE_REGEX, e.getMessage());
	    }
	}

	if (!config.hasParameter(ProfileLoader.PARAM_LOAD_MODEL_DIRECTORY)) {

	    isValid = false;
	    result.addError(this, 100, ProfileLoader.PARAM_LOAD_MODEL_DIRECTORY);

	} else {

	    String loadModelDirectory = config.getParameterValue(ProfileLoader.PARAM_LOAD_MODEL_DIRECTORY);

	    // ensure that directory exists

	    File loadModelDirectoryFile = new File(loadModelDirectory);
	    boolean exi = loadModelDirectoryFile.exists();
	    boolean dir = loadModelDirectoryFile.isDirectory();
	    boolean rea = loadModelDirectoryFile.canRead();

	    if (!exi || !dir || !rea) {

		isValid = false;
		result.addError(this, 101, ProfileLoader.PARAM_LOAD_MODEL_DIRECTORY, loadModelDirectory);

	    } else {

		Collection<File> files = FileUtils.listFiles(loadModelDirectoryFile, new String[] { "xml", "zip" },
			false);

		if (files.isEmpty()) {

		    result.addWarning(this, 102, loadModelDirectory);
		}
	    }
	}

	// validate PARAM_DIFF_ELEMENT_TYPES
	List<String> diffElementTypeNames = config.parameterAsStringList(ProfileLoader.PARAM_DIFF_ELEMENT_TYPES,
		ProfileLoader.DEFAULT_DIFF_ELEMENT_TYPES, true, true);

	Collections.sort(diffElementTypeNames);

	for (String detn : diffElementTypeNames) {

	    try {

		ElementType.valueOf(detn.toUpperCase(Locale.ENGLISH));

	    } catch (IllegalArgumentException e) {
		result.addWarning(this, 104, ProfileLoader.PARAM_DIFF_ELEMENT_TYPES, detn);
	    }
	}

	return isValid;
    }

    @Override
    public String message(int mnr) {

	switch (mnr) {
	case 1:
	    return "Context: class $1$";
	case 2:
	    return "Context: property $1$";
	case 3:
	    return "Context: association role (end1) '$1$'";
	case 4:
	    return "Context: association role (end2) '$1$'";
	case 100:
	    return "Transformation parameter '$1$' is required, but was not found in the configuration.";
	case 101:
	    return "Value '$1$' of transformation parameter '$2$' does not identify an existing directory that can be read.";
	case 102:
	    return "The directory '$1$', from which profiles shall be loaded, is empty.";
	case 103:
	    return "Syntax exception while compiling the regular expression defined by transformation parameter '$1$': '$2$'.";
	case 104:
	    return "Element '$2$' from the value of transformation parameter '$1$' is unknown (even when ignoring case). The element will be ignored.";

	default:
	    return "(Unknown message in " + this.getClass().getName() + ". Message number was: " + mnr + ")";
	}
    }
}
