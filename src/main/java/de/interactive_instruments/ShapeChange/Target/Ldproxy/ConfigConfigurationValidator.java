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
package de.interactive_instruments.ShapeChange.Target.Ldproxy;

import java.io.File;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.interactive_instruments.ShapeChange.AbstractConfigurationValidator;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessConfiguration;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;

/**
 * @author Clemens Portele (portele at interactive-instruments dot de)
 *
 */
public class ConfigConfigurationValidator extends AbstractConfigurationValidator {

    protected SortedSet<String> allowedParametersWithStaticNames = new TreeSet<>(Stream
	    .of(ConfigConstants.PARAM_1TON_TABLE_TEMPLATE, ConfigConstants.PARAM_CL_URL,
		    ConfigConstants.PARAM_FEATURE_TYPES, ConfigConstants.PARAM_FILTERS,
		    ConfigConstants.PARAM_FOREIGN_KEY_SUFFIX, ConfigConstants.PARAM_GEOMETRY_FIELD,
		    ConfigConstants.PARAM_GEOMETRY_TABLE, ConfigConstants.PARAM_HTML_LABEL,
		    ConfigConstants.PARAM_MAX_LENGTH, ConfigConstants.PARAM_NTOM_TABLE_TEMPLATE,
		    ConfigConstants.PARAM_OUTPUT_DIRECTORY, ConfigConstants.PARAM_PRETTY_PRINT,
		    ConfigConstants.PARAM_PRIMARY_KEY_FIELD, ConfigConstants.PARAM_REPORTABLE,
		    ConfigConstants.PARAM_ROOT_COLLECTION_FIELD, ConfigConstants.PARAM_ROOT_FEATURE_TABLE,
		    ConfigConstants.PARAM_SECURED, ConfigConstants.PARAM_SERVICE_DESC, ConfigConstants.PARAM_SERVICE_ID,
		    ConfigConstants.PARAM_SERVICE_LABEL, ConfigConstants.PARAM_SERVICE_VERSION,
		    ConfigConstants.PARAM_TRIGGER_ONDELETE, ConfigConstants.PARAM_UNITTEST_OVERRIDE)
	    .collect(Collectors.toSet()));
    protected List<Pattern> regexForAllowedParametersWithDynamicNames = null;

    @Override
    public boolean isValid(ProcessConfiguration pConfig, Options options, ShapeChangeResult result) {

	boolean isValid = true;

	allowedParametersWithStaticNames.addAll(getCommonTargetParameters());
	isValid = validateParameters(allowedParametersWithStaticNames, regexForAllowedParametersWithDynamicNames,
		pConfig.getParameters().keySet(), result) && isValid;

	// ensure that output directory exists
	String outputDirectory = pConfig.getParameterValue("outputDirectory");
	if (outputDirectory == null)
	    outputDirectory = options.parameter("outputDirectory");
	if (outputDirectory == null)
	    outputDirectory = ".";

	File outputDirectoryFile = new File(outputDirectory);
	boolean exi = outputDirectoryFile.exists();
	if (!exi) {
	    outputDirectoryFile.mkdirs();
	    exi = outputDirectoryFile.exists();
	}
	boolean dir = outputDirectoryFile.isDirectory();
	boolean wrt = outputDirectoryFile.canWrite();
	boolean rea = outputDirectoryFile.canRead();
	if (!exi || !dir || !wrt || !rea) {
	    isValid = false;
	    result.addError(this, 2, outputDirectory);
	}

	// validate PARAM_SERVICE_ID
	if (pConfig.hasParameter(ConfigConstants.PARAM_SERVICE_ID)) {

	    String sid = options.parameter(Config.class.getName(), ConfigConstants.PARAM_SERVICE_ID);

	    if (!sid.matches("^[a-zA-Z_][a-zA-Z0-9_]+$")) {
		result.addError(this, 1, sid);
		isValid = false;
	    }
	}

	// validate PARAM_MAX_LENGTH
	if (pConfig.hasParameter(ConfigConstants.PARAM_MAX_LENGTH)) {

	    String s = options.parameter(Config.class.getName(), ConfigConstants.PARAM_MAX_LENGTH);

	    try {
		Integer.valueOf(s);
	    } catch (Exception e) {
		result.addError(this, 3, ConfigConstants.PARAM_MAX_LENGTH, s);
		isValid = false;
	    }
	}

	return isValid;
    }

    @Override
    public String message(int mnr) {

	switch (mnr) {

	case 1:
	    return "The id of a service must be alphanumeric (underscore is allowed). The first character must be a letter or an underscore. Found: '$1$'.";
	case 2:
	    return "Output directory '$1$' does not exist or is not accessible.";
	case 3:
	    return "The parameter '$1$' must be an integer. Found: '$2$'.";
	default:
	    return "(ModelExport.java) Unknown message with number: " + mnr;
	}
    }
}
