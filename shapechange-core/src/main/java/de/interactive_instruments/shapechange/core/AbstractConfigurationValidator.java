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
 * (c) 2002-2026 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.text.similarity.LevenshteinDistance;

import de.interactive_instruments.shapechange.core.ShapeChangeResult.MessageContext;
import de.interactive_instruments.shapechange.core.target.Target;
import de.interactive_instruments.shapechange.core.target.TargetOutputProcessor;
import de.interactive_instruments.shapechange.core.transformation.TransformationManager;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public abstract class AbstractConfigurationValidator implements ConfigurationValidator, MessageSource {

    protected ProcessConfiguration config = null;
    protected Options options = null;
    protected ShapeChangeResult result = null;

    protected LevenshteinDistance levDistance = new LevenshteinDistance(3);

    protected void setProcessConfiguration(ProcessConfiguration pc) {
	this.config = pc;
    }

    protected void setOptions(Options o) {
	this.options = o;
    }

    protected void setShapeChangeResult(ShapeChangeResult scr) {
	this.result = scr;
    }

    /**
     * Checks if all relevant parameters from the ShapeChange configuration belong
     * to allowed parameters.
     * 
     * @param allowedParametersWithStaticNames            names of allowed process
     *                                                    specific parameters; can
     *                                                    be <code>null</code>
     * @param regexesForAllowedParametersWithDynamicNames Regular expressions for
     *                                                    allowed parameters with
     *                                                    dynamic names (if one of
     *                                                    the regexes matches a
     *                                                    parameter name, that
     *                                                    parameter is allowed); can
     *                                                    be <code>null</code>
     * @param actualParameters                            names of parameters
     *                                                    contained in the
     *                                                    configuration, to be
     *                                                    validated; can be
     *                                                    <code>null</code>
     * @param result                                      for reporting any
     *                                                    parameter that is not
     *                                                    allowed
     * @return <code>true</code> if all actual parameters belong to allowed
     *         parameters, else <code>false</code>
     */
    public boolean validateParameters(SortedSet<String> allowedParametersWithStaticNames,
	    List<Pattern> regexesForAllowedParametersWithDynamicNames, Set<String> actualParameters,
	    ShapeChangeResult result) {

	boolean reportUnrecognizedParametersAsWarnings = result.options().reportUnrecognizedParametersAsWarnings();

	boolean allParametersValid = true;

	if (actualParameters != null) {

	    for (String parameter : actualParameters) {

		boolean isAllowed = false;

		if (allowedParametersWithStaticNames != null) {
		    isAllowed = allowedParametersWithStaticNames.contains(parameter);
		}

		if (!isAllowed && regexesForAllowedParametersWithDynamicNames != null) {
		    for (Pattern regex : regexesForAllowedParametersWithDynamicNames) {
			if (regex.matcher(parameter).matches()) {
			    isAllowed = true;
			    break;
			}
		    }
		}

		if (!isAllowed) {

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
			if (reportUnrecognizedParametersAsWarnings) {
			    result.addWarning(null, 1_000_000, parameter, allowedParameterWithNearStringDistance);
			} else {
			    result.addError(null, 1_000_000, parameter, allowedParameterWithNearStringDistance);
			}
		    } else {
			if (reportUnrecognizedParametersAsWarnings) {
			    result.addWarning(null, 1_000_001, parameter);
			} else {
			    result.addError(null, 1_000_001, parameter);
			}
		    }

		}
	    }
	}

	return reportUnrecognizedParametersAsWarnings || allParametersValid;
    }

    /**
     * Checks if the process configuration has a parameter with given name. If it
     * is, and if its value is neither 'true' nor 'false' (ignoring case), then the
     * check will fail and an error will be logged. Otherwise the check will
     * succeed.
     * 
     * @param parameterName name of the parameter for which to check its boolean
     *                      value, if it is set
     * @return <code>true</code> if either the parameter is not set in the process
     *         configuration, or if its value is 'true' or 'false' (ignoring case).
     *         Otherwise, <code>false</code> is returned.
     */
    protected boolean checkIsBooleanValueIfSet(String parameterName) {

	String paramValue = config.getParameterValue(parameterName);

	if (Strings.CI.equalsAny(paramValue, null, "true", "false")) {
	    return true;
	}

	MessageContext mc = result.addError(null, 1_000_002, parameterName, paramValue);
	addMessageDetails(mc);
	return false;
    }

    protected boolean checkStringParameterNotBlankIfSet(String paramName) {

	if (config.hasParameter(paramName)) {

	    if (StringUtils.isBlank(config.getParameterValue(paramName))) {

		MessageContext mc = result.addError(null, 1_000_004, paramName);
		addMessageDetails(mc);
		return false;
	    }
	}

	return true;
    }

    protected boolean checkParameterOptionalCharacteristicHasValue(
	    Map<String, Map<String, String>> characteristicsByParameter, String meParamName,
	    String meParamCharacteristic, String typeRuleKey, String targetType) {

	Map<String, String> characteristics = characteristicsByParameter.get(meParamName);

	if (characteristics.containsKey(meParamCharacteristic)) {

	    if (StringUtils.isBlank(characteristics.get(meParamCharacteristic))) {

		MessageContext mc = result.addError(null, 1_000_008, meParamName, meParamCharacteristic);
		addMessageDetailsWithMapParamInfos(mc, typeRuleKey, targetType);
		return false;
	    }
	}

	return true;
    }

    protected boolean checkParameterRequiredCharacteristicHasValue(
	    Map<String, Map<String, String>> characteristicsByParameter, String meParamName,
	    String meParamCharacteristic, String typeRuleKey, String targetType) {

	Map<String, String> characteristics = characteristicsByParameter.get(meParamName);

	String characteristicValue = characteristics.get(meParamCharacteristic);

	if (StringUtils.isBlank(characteristicValue)) {

	    MessageContext mc = result.addError(null, 1_000_009, meParamName, meParamCharacteristic);
	    addMessageDetailsWithMapParamInfos(mc, typeRuleKey, targetType);
	    return false;
	}

	return true;
    }

    protected boolean checkParameterCharacteristicHasAllowedValueIgnoringCase(
	    Map<String, Map<String, String>> characteristicsByParameter, String meParamName,
	    String meParamCharacteristic, String[] allowedValues, String typeRuleKey, String targetType) {

	Map<String, String> characteristics = characteristicsByParameter.get(meParamName);

	if (characteristics.containsKey(meParamCharacteristic)) {

	    String characteristicValue = characteristics.get(meParamCharacteristic);

	    if (StringUtils.isNotBlank(characteristicValue)
		    && !Strings.CI.equalsAny(characteristicValue, allowedValues)) {

		MessageContext mc = result.addError(null, 1_000_010, meParamName, meParamCharacteristic,
			characteristicValue, StringUtils.join(allowedValues, ", "));
		addMessageDetailsWithMapParamInfos(mc, typeRuleKey, targetType);
		return false;
	    }
	}

	return true;
    }

    protected void addMessageDetailsWithMapParamInfos(MessageContext mc, String typeRuleKey, String targetType) {
	if (mc != null) {
	    if (config instanceof TransformerConfiguration trfConfig) {
		mc.addDetail(null, 1_000_996, trfConfig.getId(), typeRuleKey, targetType);
	    } else if (config instanceof TargetConfiguration tgtConfig) {
		mc.addDetail(null, 1_000_997, tgtConfig.getClassName(), String.join(", ", tgtConfig.getInputIds()),
			typeRuleKey, targetType);
	    }
	}
    }

    protected boolean checkParameterHasAllowedValueIgnoringCase(String paramName, String[] allowedValues) {

	if (config.hasParameter(paramName)) {

	    String paramValue = config.getParameterValue(paramName);

	    if (!Strings.CI.equalsAny(paramValue, allowedValues)) {

		MessageContext mc = result.addError(null, 1_000_005, paramName, paramValue);
		addMessageDetails(mc);
		return false;
	    }
	}

	return true;
    }

    protected boolean checkParameterRequiredForRule(String parameterName, String ruleName) {

	String paramValue = config.parameterAsString(parameterName, null, false, true);

	if (paramValue == null) {
	    MessageContext mc = result.addError(null, 1_000_006, parameterName, ruleName);
	    addMessageDetails(mc);
	    return false;
	} else {
	    return true;
	}
    }

    protected boolean checkIntegerParameter(String paramName) {

	String valueByConfig = config.getParameterValue(paramName);

	if (valueByConfig != null) {

	    try {
		Integer.parseInt(valueByConfig);
	    } catch (NumberFormatException e) {
		MessageContext mc = result.addError(null, 1_000_901, paramName, e.getMessage());
		addMessageDetails(mc);
		return false;
	    }
	}

	return true;
    }

    protected boolean checkNonNegativeIntegerParameter(String paramName) {

	String valueByConfig = config.getParameterValue(paramName);

	if (valueByConfig != null) {

	    try {

		Integer i = Integer.parseInt(valueByConfig);
		if (i < 0) {
		    MessageContext mc = result.addError(null, 1_000_007, paramName, valueByConfig);
		    addMessageDetails(mc);
		    return false;
		}

	    } catch (NumberFormatException e) {
		MessageContext mc = result.addError(null, 1_000_901, paramName, e.getMessage());
		addMessageDetails(mc);
		return false;
	    }
	}

	return true;
    }

    protected void addMessageDetails(MessageContext mc) {
	if (mc != null) {
	    if (config instanceof TransformerConfiguration trfConfig) {
		mc.addDetail(null, 1_000_998, trfConfig.getId());
	    } else if (config instanceof TargetConfiguration tgtConfig) {
		mc.addDetail(null, 1_000_999, tgtConfig.getClassName(), String.join(", ", tgtConfig.getInputIds()));
	    }
	}
    }

    protected boolean checkEnumeration(String parameterName, String parameterValue, String... enums) {

	String v = parameterValue.trim();
	for (String e : enums) {
	    if (v.equals(e)) {
		return true;
	    }
	}

	MessageContext mc = result.addError(null, 1_000_003, parameterName, parameterValue.trim(),
		StringUtils.join(enums, ", "));
	addMessageDetails(mc);
	return false;
    }

    public SortedSet<String> getCommonTransformerParameters() {
	return TransformationManager.getRecognizedParameters();
    }

    public SortedSet<String> getCommonTargetParameters() {
	SortedSet<String> result = new TreeSet<>();
	result.addAll(Target.COMMON_TARGET_PARAMETERS);
	result.addAll(TargetOutputProcessor.getRecognizedParameters());
	return result;
    }
}
