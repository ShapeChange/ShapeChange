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
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange.Target.OpenApi;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
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
import de.interactive_instruments.ShapeChange.Target.JSON.JsonSchemaConstants;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.JsonSchemaVersion;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class OpenApiDefinitionConfigurationValidator extends AbstractConfigurationValidator {

    protected SortedSet<String> allowedParametersWithStaticNames = new TreeSet<>(Stream
	    .of(OpenApiConstants.PARAM_BASE_TEMPLATE, OpenApiConstants.PARAM_COLLECTIONS,
		    OpenApiConstants.PARAM_JSON_SCHEMA_VERSION, OpenApiConstants.PARAM_JSON_SCHEMAS_BASE_LOCATION)
	    .collect(Collectors.toSet()));
    protected Pattern regexForAllowedParametersWithDynamicNames = null;

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

	// ensure that advanced process configuration exists
	if (pConfig.getAdvancedProcessConfigurations() == null) {
	    isValid = false;
	    result.addError(this, 3);
	} else {

	    try {
		// parse OpenAPI config items, thereby ensure that overlays are accessible
		OpenApiConfigItems oapiConfig = new OpenApiConfigItems(result,
			pConfig.getAdvancedProcessConfigurations());

		// ensure that core conformance class is available
		Optional<ConformanceClass> coreCc = oapiConfig
			.conformanceClass(OpenApiConstants.CC_OGCAPI_FEATURES_1_1_CORE);
		if (coreCc.isEmpty()) {
		    isValid = false;
		    result.addError(this, 5);
		}

	    } catch (Exception e) {
		// exact cause of the exception has been logged while parsing the configuration
		isValid = false;
	    }
	}

	// ensure that baseTemplate is accessible
	String baseTemplateValue = options.parameterAsString(OpenApiDefinition.class.getName(),
		OpenApiConstants.PARAM_BASE_TEMPLATE,
		"https://shapechange.net/resources/openapi/overlays/default-template.json", false, true);
	if (StringUtils.isBlank(baseTemplateValue)) {
	    isValid = false;
	    result.addError(this, 8, OpenApiConstants.PARAM_BASE_TEMPLATE, baseTemplateValue);
	} else {
	    try {
		OpenApiDefinition.loadJson(baseTemplateValue);
	    } catch (IOException e) {
		isValid = false;
		result.addError(this, 7, baseTemplateValue, e.getMessage());
	    }
	}

	// ensure that jsonSchemasBaseLocation is configured
	String jsonSchemasBaseLocation = options.parameterAsString(OpenApiDefinition.class.getName(),
		OpenApiConstants.PARAM_JSON_SCHEMAS_BASE_LOCATION, null, false, true);
	if (StringUtils.isBlank(jsonSchemasBaseLocation)) {
	    isValid = false;
	    result.addError(this, 4, OpenApiConstants.PARAM_JSON_SCHEMAS_BASE_LOCATION);
	}

	// ensure that jsonSchemaVersion has a valid value
	String jsVersionParamValue = options.parameterAsString(OpenApiDefinition.class.getName(),
		OpenApiConstants.PARAM_JSON_SCHEMA_VERSION, "2019-09", false, true);
	Optional<JsonSchemaVersion> jsVersion = JsonSchemaVersion.fromString(jsVersionParamValue);

	if (!jsVersion.isPresent()) {
	    isValid = false;
	    result.addError(this, 8, JsonSchemaConstants.PARAM_JSON_SCHEMA_VERSION, jsVersionParamValue);
	}

	// check if 'collections' parameter is set, and if so, that it is not empty
	if (pConfig.hasParameter(OpenApiConstants.PARAM_COLLECTIONS)
		&& options.parameterAsStringList(OpenApiDefinition.class.getName(), OpenApiConstants.PARAM_COLLECTIONS,
			null, true, true).isEmpty()) {
	    isValid = false;
	    result.addError(this, 6, OpenApiConstants.PARAM_COLLECTIONS);
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
	    return "No 'advancedProcessConfigurations' element present in the configuration.";
	case 4:
	    return "Required target parameter '$1$' is not set in the configuration.";
	case 5:
	    return "The configuration does not define the required conformance class http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/core.";
	case 6:
	    return "Target parameter '$1$' is set in the configuration, but does not have an actual value.";
	case 7:
	    return "Target parameter 'baseTemplate' has value: '$1$'. Could not load JSON from that location. Exception message is: $2$";
	case 8:
	    return "Configuration parameter '$1$' has invalid value '$2$'";

	default:
	    return "(OpenApiDefinitionConfigurationValidator.java) Unknown message with number: " + mnr;
	}
    }
}
