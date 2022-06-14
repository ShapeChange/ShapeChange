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
 * (c) 2002-2022 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target.Ldproxy2;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import de.ii.xtraplatform.crs.domain.EpsgCrs.Force;
import de.interactive_instruments.ShapeChange.AbstractConfigurationValidator;
import de.interactive_instruments.ShapeChange.MapEntryParamInfos;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessConfiguration;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.TargetConfiguration;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class Ldproxy2TargetConfigurationValidator extends AbstractConfigurationValidator {

    protected SortedSet<String> allowedParametersWithStaticNames = new TreeSet<>(
	    Stream.of(Ldproxy2Constants.PARAM_ASSOC_TABLE_COLUMN_SUFFIX, Ldproxy2Constants.PARAM_CFG_TEMPLATE_PATH,
		    Ldproxy2Constants.PARAM_CODE_TARGET_TAG_NAME, Ldproxy2Constants.PARAM_DATE_FORMAT,
		    Ldproxy2Constants.PARAM_DATE_TIME_FORMAT, Ldproxy2Constants.PARAM_DESCRIPTION_TEMPLATE,
		    Ldproxy2Constants.PARAM_DESCRIPTOR_NO_VALUE, Ldproxy2Constants.PARAM_FORCE_AXIS_ORDER,
		    Ldproxy2Constants.PARAM_FK_COLUMN_SUFFIX, Ldproxy2Constants.PARAM_FK_COLUMN_SUFFIX_DATATYPE,
		    Ldproxy2Constants.PARAM_FK_COLUMN_SUFFIX_CODELIST, Ldproxy2Constants.PARAM_LABEL_TEMPLATE,
		    Ldproxy2Constants.PARAM_MAX_NAME_LENGTH, Ldproxy2Constants.PARAM_NATIVE_TIME_ZONE,
		    Ldproxy2Constants.PARAM_OBJECT_IDENTIFIER_NAME, Ldproxy2Constants.PARAM_PK_COLUMN,
		    Ldproxy2Constants.PARAM_SERVICE_DESCRIPTION, Ldproxy2Constants.PARAM_SERVICE_LABEL,
		    Ldproxy2Constants.PARAM_SERVICE_CONFIG_TEMPLATE_PATH, Ldproxy2Constants.PARAM_SRID,
		    "_unitTestOverride").collect(Collectors.toSet()));
    protected List<Pattern> regexForAllowedParametersWithDynamicNames = null;

    // these fields will be initialized when isValid(...) is called
    private TargetConfiguration targetConfig = null;
    private String targetConfigInputs = "";
    private Options options = null;
    private ShapeChangeResult result = null;

    @Override
    public boolean isValid(ProcessConfiguration config, Options options, ShapeChangeResult result) {

	this.targetConfig = (TargetConfiguration) config;
	Set<String> inputIds = targetConfig.getInputIds();
	if (inputIds != null && !inputIds.isEmpty()) {
	    this.targetConfigInputs = String.join(" ", inputIds);
	}
	this.options = options;
	this.result = result;

	boolean isValid = true;

	allowedParametersWithStaticNames.addAll(getCommonTargetParameters());
	isValid = validateParameters(allowedParametersWithStaticNames, regexForAllowedParametersWithDynamicNames,
		config.getParameters().keySet(), result) && isValid;

	SortedMap<String, ProcessMapEntry> mapEntryByType = new TreeMap<String, ProcessMapEntry>();

	for (ProcessMapEntry pme : config.getMapEntries()) {
	    mapEntryByType.put(pme.getType(), pme);
	}

	// general validation of map entry parameters
	MapEntryParamInfos mepis = new MapEntryParamInfos(result, mapEntryByType.values());

	isValid = isValid && mepis.isValid();

	// validation of known map entry parameters
	isValid = isValid && checkMapEntryParameters(mepis);

	isValid = isValid & checkNonNegativeIntegerParameter(Ldproxy2Constants.PARAM_SRID);
	isValid = isValid & checkNonNegativeIntegerParameter(Ldproxy2Constants.PARAM_MAX_NAME_LENGTH);

	isValid = isValid & checkStringParameterNotBlankIfSet(Ldproxy2Constants.PARAM_CFG_TEMPLATE_PATH);
	isValid = isValid & checkStringParameterNotBlankIfSet(Ldproxy2Constants.PARAM_CODE_TARGET_TAG_NAME);
	isValid = isValid & checkStringParameterNotBlankIfSet(Ldproxy2Constants.PARAM_DATE_FORMAT);
	isValid = isValid & checkStringParameterNotBlankIfSet(Ldproxy2Constants.PARAM_DATE_TIME_FORMAT);
	isValid = isValid & checkStringParameterNotBlankIfSet(Ldproxy2Constants.PARAM_DESCRIPTION_TEMPLATE);
	isValid = isValid & checkStringParameterNotBlankIfSet(Ldproxy2Constants.PARAM_FORCE_AXIS_ORDER);
	isValid = isValid & checkStringParameterNotBlankIfSet(Ldproxy2Constants.PARAM_LABEL_TEMPLATE);
	isValid = isValid & checkStringParameterNotBlankIfSet(Ldproxy2Constants.PARAM_NATIVE_TIME_ZONE);
	isValid = isValid & checkStringParameterNotBlankIfSet(Ldproxy2Constants.PARAM_OBJECT_IDENTIFIER_NAME);
	isValid = isValid & checkStringParameterNotBlankIfSet(Ldproxy2Constants.PARAM_PK_COLUMN);
	isValid = isValid & checkStringParameterNotBlankIfSet(Ldproxy2Constants.PARAM_SERVICE_DESCRIPTION);
	isValid = isValid & checkStringParameterNotBlankIfSet(Ldproxy2Constants.PARAM_SERVICE_LABEL);
	isValid = isValid & checkStringParameterNotBlankIfSet(Ldproxy2Constants.PARAM_SERVICE_CONFIG_TEMPLATE_PATH);

	if (StringUtils.isNotBlank(targetConfig.getParameterValue(Ldproxy2Constants.PARAM_FORCE_AXIS_ORDER))) {
	    String paramValue = targetConfig.getParameterValue(Ldproxy2Constants.PARAM_FORCE_AXIS_ORDER);
	    try {
		Force.valueOf(paramValue);
	    } catch (IllegalArgumentException e) {
		MessageContext mc = result.addError(this, 107, Ldproxy2Constants.PARAM_FORCE_AXIS_ORDER, paramValue);
		mc.addDetail(this, 0, targetConfigInputs);
		isValid = false;
	    }
	}

	return isValid;
    }

    private boolean checkStringParameterNotBlankIfSet(String paramName) {

	if (targetConfig.hasParameter(paramName)) {

	    if (StringUtils.isBlank(targetConfig.getParameterValue(paramName))) {

		MessageContext mc = result.addError(this, 106, paramName);
		mc.addDetail(this, 0, targetConfigInputs);
		return false;
	    }
	}

	return true;
    }

    private boolean checkIntegerParameter(String paramName) {

	String valueByConfig = targetConfig.getParameterValue(paramName);

	if (valueByConfig != null) {

	    try {
		Integer.parseInt(valueByConfig);
	    } catch (NumberFormatException e) {
		MessageContext mc = result.addError(this, 4, paramName, e.getMessage());
		mc.addDetail(this, 0, targetConfigInputs);
		return false;
	    }
	}

	return true;
    }

    private boolean checkNonNegativeIntegerParameter(String paramName) {

	String valueByConfig = targetConfig.getParameterValue(paramName);

	if (valueByConfig != null) {

	    try {

		Integer i = Integer.parseInt(valueByConfig);
		if (i < 0) {
		    result.addError(this, 104, paramName, valueByConfig);
		    return false;
		}

	    } catch (NumberFormatException e) {
		MessageContext mc = result.addError(this, 4, paramName, e.getMessage());
		mc.addDetail(this, 0, targetConfigInputs);
		return false;
	    }
	}

	return true;
    }

    private boolean checkMapEntryParameters(MapEntryParamInfos mepp) {

	boolean isValid = true;

	for (Entry<String, Map<String, Map<String, String>>> entry : mepp.getParameterCache().entrySet()) {

	    String typeRuleKey = entry.getKey();

	    String[] keyParts = typeRuleKey.split("#");
	    String typeName = keyParts[0];
	    String ruleName = keyParts[1];
	    ProcessMapEntry pme = mepp.getMapEntry(typeName, ruleName);
	    String targetType = pme.getTargetType();

	    if (StringUtils.isBlank(targetType)) {
		isValid = false;
		MessageContext mc = result.addError(this, 108, typeName);
		if (mc != null) {
		    mc.addDetail(this, 2, targetConfigInputs, typeRuleKey);
		}
	    } else if (!StringUtils.equalsAnyIgnoreCase(targetType, "FLOAT", "INTEGER", "STRING", "BOOLEAN", "DATETIME",
		    "DATE", "GEOMETRY", "LINK")) {
		isValid = false;
		MessageContext mc = result.addError(this, 109, typeName, targetType);
		if (mc != null) {
		    mc.addDetail(this, 2, targetConfigInputs, typeRuleKey);
		}
	    }

	    Map<String, Map<String, String>> characteristicsByParameter = entry.getValue();

	    if (characteristicsByParameter.containsKey(Ldproxy2Constants.ME_PARAM_GEOMETRY_INFOS)) {

		if (!targetType.equalsIgnoreCase("GEOMETRY")) {

		    isValid = false;
		    MessageContext mc = result.addError(this, 101, Ldproxy2Constants.ME_PARAM_GEOMETRY_INFOS,
			    "GEOMETRY", targetType);
		    if (mc != null) {
			mc.addDetail(this, 1, targetConfigInputs, typeRuleKey, targetType);
		    }

		} else {

		    isValid = isValid & checkParameterRequiredCharacteristicHasValue(characteristicsByParameter,
			    Ldproxy2Constants.ME_PARAM_GEOMETRY_INFOS,
			    Ldproxy2Constants.ME_PARAM_GEOMETRY_INFOS_CHARACT_GEOMETRY_TYPE, typeRuleKey, targetType);

		    isValid = isValid
			    & checkParameterCharacteristicHasAllowedValueIgnoringCase(characteristicsByParameter,
				    Ldproxy2Constants.ME_PARAM_GEOMETRY_INFOS,
				    Ldproxy2Constants.ME_PARAM_GEOMETRY_INFOS_CHARACT_GEOMETRY_TYPE,
				    new String[] { "POINT", "MULTI_POINT", "LINE_STRING", "MULTI_LINE_STRING",
					    "POLYGON", "MULTI_POLYGON", "GEOMETRY_COLLECTION", "ANY" },
				    typeRuleKey, targetType);
		}
	    }

	    if (characteristicsByParameter.containsKey(Ldproxy2Constants.ME_PARAM_INITIAL_VALUE_ENCODING)) {

		if (!StringUtils.equalsAnyIgnoreCase(targetType, "FLOAT", "INTEGER", "STRING", "BOOLEAN", "DATETIME",
			"DATE")) {

		    isValid = false;
		    MessageContext mc = result.addError(this, 101, Ldproxy2Constants.ME_PARAM_INITIAL_VALUE_ENCODING,
			    "FLOAT, INTEGER, STRING, BOOLEAN, DATETIME, DATE", targetType);
		    if (mc != null) {
			mc.addDetail(this, 1, targetConfigInputs, typeRuleKey, targetType);
		    }

		} else {

		    isValid = isValid & checkParameterOptionalCharacteristicHasValue(characteristicsByParameter,
			    Ldproxy2Constants.ME_PARAM_INITIAL_VALUE_ENCODING,
			    Ldproxy2Constants.ME_PARAM_INITIAL_VALUE_ENCODING_CHARACT_FALSE, typeRuleKey, targetType);

		    isValid = isValid & checkParameterOptionalCharacteristicHasValue(characteristicsByParameter,
			    Ldproxy2Constants.ME_PARAM_INITIAL_VALUE_ENCODING,
			    Ldproxy2Constants.ME_PARAM_INITIAL_VALUE_ENCODING_CHARACT_TRUE, typeRuleKey, targetType);
		}
	    }

	    if (characteristicsByParameter.containsKey(Ldproxy2Constants.ME_PARAM_LINK_INFOS)) {

		if (!targetType.equalsIgnoreCase("LINK")) {

		    isValid = false;
		    MessageContext mc = result.addError(this, 101, Ldproxy2Constants.ME_PARAM_LINK_INFOS, "LINK",
			    targetType);
		    if (mc != null) {
			mc.addDetail(this, 1, targetConfigInputs, typeRuleKey, targetType);
		    }

		} else {

		    isValid = isValid & checkParameterRequiredCharacteristicHasValue(characteristicsByParameter,
			    Ldproxy2Constants.ME_PARAM_LINK_INFOS,
			    Ldproxy2Constants.ME_PARAM_LINK_INFOS_CHARACT_URL_TEMPLATE, typeRuleKey, targetType);
		}
	    }
	}

	return isValid;

    }

    private boolean checkParameterCharacteristicHasAllowedValueIgnoringCase(
	    Map<String, Map<String, String>> characteristicsByParameter, String meParamName,
	    String meParamCharacteristic, String[] allowedValues, String typeRuleKey, String targetType) {

	Map<String, String> characteristics = characteristicsByParameter.get(meParamName);

	if (characteristics.containsKey(meParamCharacteristic)) {

	    String characteristicValue = characteristics.get(meParamCharacteristic);

	    if (StringUtils.isNotBlank(characteristicValue)
		    && !StringUtils.equalsAnyIgnoreCase(characteristicValue, allowedValues)) {

		MessageContext mc = result.addError(this, 103, meParamName, meParamCharacteristic, characteristicValue,
			StringUtils.join(allowedValues, ", "));
		if (mc != null) {
		    mc.addDetail(this, 1, targetConfigInputs, typeRuleKey, targetType);
		}

		return false;
	    }
	}

	return true;
    }

    private boolean checkParameterOptionalCharacteristicHasValue(
	    Map<String, Map<String, String>> characteristicsByParameter, String meParamName,
	    String meParamCharacteristic, String typeRuleKey, String targetType) {

	Map<String, String> characteristics = characteristicsByParameter.get(meParamName);

	if (characteristics.containsKey(meParamCharacteristic)) {

	    if (StringUtils.isBlank(characteristics.get(meParamCharacteristic))) {

		MessageContext mc = result.addError(this, 105, meParamName, meParamCharacteristic);
		if (mc != null) {
		    mc.addDetail(this, 1, targetConfigInputs, typeRuleKey, targetType);
		}

		return false;
	    }
	}

	return true;
    }

    private boolean checkParameterRequiredCharacteristicHasValue(
	    Map<String, Map<String, String>> characteristicsByParameter, String meParamName,
	    String meParamCharacteristic, String typeRuleKey, String targetType) {

	Map<String, String> characteristics = characteristicsByParameter.get(meParamName);

	String characteristicValue = characteristics.get(meParamCharacteristic);

	if (StringUtils.isBlank(characteristicValue)) {

	    MessageContext mc = result.addError(this, 102, meParamName, meParamCharacteristic);
	    if (mc != null) {
		mc.addDetail(this, 1, targetConfigInputs, typeRuleKey, targetType);
	    }

	    return false;
	}

	return true;
    }

    @Override
    public String message(int mnr) {

	switch (mnr) {
	case 0:
	    return "Context: Ldproxy2Target configuration element with 'inputs'='$1$'.";
	case 1:
	    return "Context: Ldproxy2Target configuration element with 'inputs'='$1$', map entry with type#rule '$2$' and target type '$3$'.";
	case 2:
	    return "Context: Ldproxy2Target configuration element with 'inputs'='$1$', map entry with type#rule '$2$'.";
	case 4:
	    return "Number format exception while converting the value of configuration parameter '$1$' to an integer. Exception message: $2$. Ensure that the parameter value is an integer.";

	case 100:
	    return "Parameter '$1$' is set to '$2$'. This is not a valid value.";
	case 101:
	    return "Invalid map entry: parameter '$1$' is set, which is only applicable to a mapping with target type (one of) '$2$'. Found target type: '$3$'.";
	case 102:
	    return "Invalid map entry: parameter '$1$' is set, but its characteristic '$2$' (which is required for the parameter) is not set or has no value.";
	case 103:
	    return "Invalid map entry: parameter '$1$' is set, with characteristic '$2$', but the value '$3$' of the characteristic is not equal to (ignoring case) any of the allowed values, which are: '$4$'.";
	case 104:
	    return "Parameter '$1$' is set to '$2$'. This is not a valid non-negative integer value.";
	case 105:
	    return "Invalid map entry: parameter '$1$' is set, with characteristic '$2$', but no value is defined for the characteristic.";
	case 106:
	    return "Parameter '$1$' is set in the configuration, but has a blank value, which is not allowed for that parameter.";
	case 107:
	    return "Parameter '$1$' is set to '$2$'. This is not a valid value (case matters for this parameter).";
	case 108:
	    return "Invalid map entry for type '$1$': the target type is undefined.";
	case 109:
	    return "Invalid map entry for type '$1$': target type '$2$' does not equal (ignoring case) any of the allowed values: FLOAT, INTEGER, STRING, BOOLEAN, DATETIME, DATE, GEOMETRY, LINK. Check for typos or whitespace characters and correct the target type.";

	default:
	    return "(" + Ldproxy2TargetConfigurationValidator.class.getName() + ") Unknown message with number: " + mnr;
	}
    }
}
