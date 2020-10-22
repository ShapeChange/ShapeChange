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
package de.interactive_instruments.ShapeChange.Target.XmlSchema;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.ShapeChange.AbstractConfigurationValidator;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessConfiguration;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.Descriptor;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 */
public class XmlSchemaConfigurationValidator extends AbstractConfigurationValidator {

    protected SortedSet<String> allowedParametersWithStaticNames = new TreeSet<>(Stream.of(
	    XmlSchemaConstants.PARAM_DEFAULT_CODELIST_REPRESENTATION,
	    XmlSchemaConstants.PARAM_DEFAULT_CODELIST_VALUE_PATTERN, XmlSchemaConstants.PARAM_DEFAULT_VOID_REASON_TYPE,
	    XmlSchemaConstants.PARAM_OKSTRA, XmlSchemaConstants.PARAM_OKSTRA_KEY_VALUE_BASE_TYPE,
	    XmlSchemaConstants.PARAM_OKSTRA_KEY_VALUE_PROPERTY_TYPE, XmlSchemaConstants.PARAM_OKSTRA_OBJECT_REF_TYPE,
	    XmlSchemaConstants.PARAM_REPRESENT_DESCRIPTORS, XmlSchemaConstants.PARAM_SCH_FILENAME_TEMPLATE,
	    XmlSchemaConstants.PARAM_SCH_QUERY_BINDING, XmlSchemaConstants.PARAM_SCH_XLINK_HREF_POSTFIX,
	    XmlSchemaConstants.PARAM_SCH_XLINK_HREF_PREFIX, XmlSchemaConstants.PARAM_SEGMENT_SCH,
	    XmlSchemaConstants.PARAM_SKIP_XML_SCHEMA_OUTPUT, XmlSchemaConstants.PARAM_SUPPRESSED_TYPE_INTERPRETATION)
	    .collect(Collectors.toSet()));
    protected Pattern regexForAllowedParametersWithDynamicNames = null;

    // these fields will be initialized when isValid(...) is called
    private ProcessConfiguration config = null;
    private Options options = null;
    private ShapeChangeResult result = null;

    @Override
    public boolean isValid(ProcessConfiguration config, Options options, ShapeChangeResult result) {

	this.config = config;
	this.options = options;
	this.result = result;

	boolean isValid = true;

	allowedParametersWithStaticNames.addAll(getCommonTargetParameters());
	isValid = validateParameters(allowedParametersWithStaticNames, regexForAllowedParametersWithDynamicNames,
		config.getParameters().keySet(), result) && isValid;

	// check parameter: schematronQueryBinding
	String explicitSchematronQueryBinding = options.parameterAsString(this.getClass().getName(),
		XmlSchemaConstants.PARAM_SCH_QUERY_BINDING, null, false, true);
	if (explicitSchematronQueryBinding != null) {
	    if (!explicitSchematronQueryBinding.equalsIgnoreCase("xslt2")) {
		result.addError(this, 101, explicitSchematronQueryBinding);
		isValid = false;
	    }
	}

	// check parameter: representDescriptors
	String representDescriptorsParamValue = options.parameterAsString(Options.TargetXmlSchemaClass,
		XmlSchemaConstants.PARAM_REPRESENT_DESCRIPTORS, null, false, true);
	if (representDescriptorsParamValue != null) {

	    List<String> namesOfDescriptorsToRepresent = options.parameterAsStringList(Options.TargetXmlSchemaClass,
		    XmlSchemaConstants.PARAM_REPRESENT_DESCRIPTORS, null, true, true);

	    SortedSet<String> unknownDescriptors = new TreeSet<>();

	    for (String descriptorName : namesOfDescriptorsToRepresent) {
		try {
		    Descriptor.valueOf(descriptorName.toUpperCase(Locale.ENGLISH));
		} catch (IllegalArgumentException e) {
		    unknownDescriptors.add(descriptorName);
		}
	    }

	    if (!unknownDescriptors.isEmpty()) {
		String unknownDescriptorsAsString = StringUtils.join(unknownDescriptors, ", ");
		result.addError(this, 100, representDescriptorsParamValue, unknownDescriptorsAsString);
		isValid = false;
	    }
	}

	return isValid;
    }

    @Override
    public boolean validateParameters(SortedSet<String> allowedParametersWithStaticNames,
	    Pattern regexForAllowedParametersWithDynamicNames, Set<String> actualParameters, ShapeChangeResult result) {

	boolean allParametersValid = true;

	if (actualParameters != null) {

	    String schExtFunctRegex = "^schematronExtension\\.(\\w+?)\\.function";
	    Pattern schExtFunctPattern = Pattern.compile(schExtFunctRegex);
	    Set<String> schExtFunctionParameters = new HashSet<>();
	    Set<String> schExtFunctionNames = new HashSet<>();
	    for (String parameter : config.getParameters().keySet()) {
		Matcher mat = schExtFunctPattern.matcher(parameter);
		if (mat.matches()) {
		    schExtFunctionParameters.add(parameter);
		    schExtFunctionNames.add(mat.group(1));
		}
	    }

	    String schExtNsRegex = "^schematronExtension\\.(\\w+?)\\.namespace";
	    Pattern schExtNsPattern = Pattern.compile(schExtNsRegex);

	    for (String parameter : actualParameters) {

		boolean isAllowed = false;

		if (allowedParametersWithStaticNames != null) {
		    isAllowed = allowedParametersWithStaticNames.contains(parameter);
		}

		if (!isAllowed && regexForAllowedParametersWithDynamicNames != null) {
		    isAllowed = regexForAllowedParametersWithDynamicNames.matcher(parameter).matches();
		}

		// Check if the parameter defines a schematron extension function
		if (schExtFunctionParameters.contains(parameter)) {
		    isAllowed = true;
		}

		if (!isAllowed) {

		    // check if the parameter defines a schematron extension namespace
		    Matcher mat = schExtNsPattern.matcher(parameter);
		    if (mat.matches()) {
			
			/*
			 * now check if the function has actually been defined (by another parameter
			 * whose name matches the schExtFunctRegex)
			 */
			String functName = mat.group(1);
			if (schExtFunctionNames.contains(functName)) {
			    // fine
			} else {
			    allParametersValid = false;
			    if (reportInvalidParameterAsError) {
				result.addError(this, 200, parameter, functName);
			    } else {
				result.addWarning(this, 200, parameter, functName);
			    }
			}
		    } else {

			allParametersValid = false;

			// report the invalid parameter

			/*
			 * check if the string distance of the parameter is near to one of the allowed
			 * parameters
			 */
			String allowedParameterWithNearStringDistance = null;

			if (allowedParametersWithStaticNames != null) {
			    for (String allowedParameter : allowedParametersWithStaticNames) {
				if (levDistance.apply(parameter, allowedParameter) != -1) {
				    allowedParameterWithNearStringDistance = allowedParameter;
				    break;
				}
			    }
			}

			if (allowedParameterWithNearStringDistance != null) {
			    if (reportInvalidParameterAsError) {
				result.addError(null, 1000000, parameter, allowedParameterWithNearStringDistance);
			    } else {
				result.addWarning(null, 1000000, parameter, allowedParameterWithNearStringDistance);
			    }
			} else {
			    if (reportInvalidParameterAsError) {
				result.addError(null, 1000001, parameter);
			    } else {
				result.addWarning(null, 1000001, parameter);
			    }
			}
		    }
		}
	    }
	}

	return !reportInvalidParameterAsError || allParametersValid;
    }

    @Override
    public String message(int mnr) {

	switch (mnr) {
	case 0:
	    return "Context: XmlSchema target configuration element with 'inputs'='$1$'.";

	case 100:
	    return "Configuration parameter 'representDescriptors' contains unknown descriptors. Parameter value is: '$1$'. Unknown descriptors are: '$2$'.";
	case 101:
	    return "Configuration parameter 'schematronQueryBinding', if set, must have a value equal to (ignoring case) 'xslt2'. Found parameter value: '$1$'.";

	case 200:
	    return "Parameter '$1$' defines a namespace for the schematron extension function '$2$'. However, the corresponding parameter that actually defines that function ('schematronExtension.$2$.function') was not found. The parameter will therefore be ignored.";

	default:
	    return "(" + XmlSchemaConfigurationValidator.class.getName() + ") Unknown message with number: " + mnr;
	}
    }
}
