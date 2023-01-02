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
package de.interactive_instruments.ShapeChange.Target.gfs;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

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
public class GfsTemplateTargetConfigurationValidator extends AbstractConfigurationValidator {

    protected SortedSet<String> allowedParametersWithStaticNames = new TreeSet<>(Stream
	    .of(GfsTemplateConstants.PARAM_ALWAYS_ENCODE_DATA_TYPE_NAME,
		    GfsTemplateConstants.PARAM_CHOICE_FOR_INLINE_OR_BY_REFERENCE,
		    GfsTemplateConstants.PARAM_GML_CODE_LIST_ENCODING_VERSION,
		    GfsTemplateConstants.PARAM_PROPERTY_NAME_SEPARATOR,
		    GfsTemplateConstants.PARAM_SORT_PROPERTIES_BY_NAME, GfsTemplateConstants.PARAM_SRS_NAME,
		    GfsTemplateConstants.PARAM_XML_ATTRIBUTE_NAME_SEPARATOR,
		    GfsTemplateConstants.PARAM_XML_ATTRIBUTES_TO_ENCODE, "_unitTestOverride")
	    .collect(Collectors.toSet()));
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

	isValid = isValid & checkStringParameterNotBlankIfSet(GfsTemplateConstants.PARAM_ALWAYS_ENCODE_DATA_TYPE_NAME);
	isValid = isValid
		& checkStringParameterNotBlankIfSet(GfsTemplateConstants.PARAM_CHOICE_FOR_INLINE_OR_BY_REFERENCE);
	isValid = isValid
		& checkStringParameterNotBlankIfSet(GfsTemplateConstants.PARAM_GML_CODE_LIST_ENCODING_VERSION);
	isValid = isValid & checkStringParameterNotBlankIfSet(GfsTemplateConstants.PARAM_PROPERTY_NAME_SEPARATOR);
	isValid = isValid & checkStringParameterNotBlankIfSet(GfsTemplateConstants.PARAM_SORT_PROPERTIES_BY_NAME);	
	isValid = isValid & checkStringParameterNotBlankIfSet(GfsTemplateConstants.PARAM_SRS_NAME);
	isValid = isValid & checkStringParameterNotBlankIfSet(GfsTemplateConstants.PARAM_XML_ATTRIBUTE_NAME_SEPARATOR);

	isValid = isValid & checkParameterHasAllowedValueIgnoringCase(
		GfsTemplateConstants.PARAM_CHOICE_FOR_INLINE_OR_BY_REFERENCE, new String[] { "inline", "byReference" });
	isValid = isValid & checkParameterHasAllowedValueIgnoringCase(
		GfsTemplateConstants.PARAM_GML_CODE_LIST_ENCODING_VERSION, new String[] { "3.2", "3.3" });

	return isValid;
    }

    private boolean checkParameterHasAllowedValueIgnoringCase(String paramName, String[] allowedValues) {

	if (targetConfig.hasParameter(paramName)) {

	    String paramValue = targetConfig.getParameterValue(paramName);

	    if (!StringUtils.equalsAnyIgnoreCase(paramValue, allowedValues)) {

		MessageContext mc = result.addError(this, 110, paramName, paramValue);
		mc.addDetail(this, 0, targetConfigInputs);
		return false;
	    }
	}

	return true;
    }

    private boolean checkStringParameterNotBlankIfSet(String paramName) {

	if (targetConfig.hasParameter(paramName)) {

	    if (StringUtils.isBlank(targetConfig.getParameterValue(paramName))) {

		MessageContext mc = result.addError(this, 104, paramName);
		mc.addDetail(this, 0, targetConfigInputs);
		return false;
	    }
	}

	return true;
    }

    private boolean checkMapEntryParameters(MapEntryParamInfos mepp) {

	boolean isValid = true;

	final Pattern targetTypePattern = Pattern.compile(GfsTemplateConstants.GFS_TYPE_REGEX);

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
	    } else {

		Matcher targetTypeMatcher = targetTypePattern.matcher(targetType);

		if (!targetTypeMatcher.matches()) {
		    isValid = false;
		    MessageContext mc = result.addError(this, 109, typeName, targetType);
		    if (mc != null) {
			mc.addDetail(this, 2, targetConfigInputs, typeRuleKey);
		    }
		}
	    }

	    Map<String, Map<String, String>> characteristicsByParameter = entry.getValue();

	    if (characteristicsByParameter.containsKey(GfsTemplateConstants.ME_PARAM_TYPE_DETAILS)) {

		Map<String, String> characteristics = characteristicsByParameter
			.get(GfsTemplateConstants.ME_PARAM_TYPE_DETAILS);

		if (characteristics.containsKey(GfsTemplateConstants.ME_PARAM_TYPE_DETAILS_CHARACT_SUBTYPE)) {

		    // ensure that a value exists - using common method
		    isValid = isValid & checkParameterOptionalCharacteristicHasValue(characteristicsByParameter,
			    GfsTemplateConstants.ME_PARAM_TYPE_DETAILS,
			    GfsTemplateConstants.ME_PARAM_TYPE_DETAILS_CHARACT_SUBTYPE, typeRuleKey, targetType);

		    String subtype = StringUtils.stripToEmpty(
			    characteristics.get(GfsTemplateConstants.ME_PARAM_TYPE_DETAILS_CHARACT_SUBTYPE));

		    if (StringUtils.isNotBlank(subtype)) {

			// check that the subtype is one of the allowed ones
			if (StringUtils.equalsAnyIgnoreCase(subtype, GfsTemplateConstants.GFS_SUBTYPES)) {

			    MessageContext mc = null;

			    // now check that the combination is allowed
			    if (targetType.equalsIgnoreCase("Integer")
				    && !StringUtils.equalsAnyIgnoreCase(subtype, "Integer64", "Short")) {
				isValid = false;
				mc = result.addError(this, 106, GfsTemplateConstants.ME_PARAM_TYPE_DETAILS,
					GfsTemplateConstants.ME_PARAM_TYPE_DETAILS_CHARACT_SUBTYPE, subtype,
					targetType);
			    } else if (targetType.equalsIgnoreCase("Real")
				    && !StringUtils.equalsAnyIgnoreCase(subtype, "Float")) {
				isValid = false;
				mc = result.addError(this, 106, GfsTemplateConstants.ME_PARAM_TYPE_DETAILS,
					GfsTemplateConstants.ME_PARAM_TYPE_DETAILS_CHARACT_SUBTYPE, subtype,
					targetType);
			    } else if (targetType.equalsIgnoreCase("String") && !StringUtils
				    .equalsAnyIgnoreCase(subtype, "Boolean", "Date", "Datetime", "Time")) {
				isValid = false;
				mc = result.addError(this, 106, GfsTemplateConstants.ME_PARAM_TYPE_DETAILS,
					GfsTemplateConstants.ME_PARAM_TYPE_DETAILS_CHARACT_SUBTYPE, subtype,
					targetType);
			    }

			    if (mc != null) {
				mc.addDetail(this, 1, targetConfigInputs, typeRuleKey, targetType);
			    }

			} else {

			    isValid = false;
			    MessageContext mc = result.addError(this, 107, GfsTemplateConstants.ME_PARAM_TYPE_DETAILS,
				    GfsTemplateConstants.ME_PARAM_TYPE_DETAILS_CHARACT_SUBTYPE, subtype,
				    StringUtils.join(GfsTemplateConstants.GFS_SUBTYPES, ", "));
			    if (mc != null) {
				mc.addDetail(this, 1, targetConfigInputs, typeRuleKey, targetType);
			    }
			}
		    }
		}
	    }
	}

	return isValid;

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

    @Override
    public String message(int mnr) {

	switch (mnr) {
	case 0:
	    return "Context: GfsTemplateTarget configuration element with 'inputs'='$1$'.";
	case 1:
	    return "Context: GfsTemplateTarget configuration element with 'inputs'='$1$', map entry with type#rule '$2$' and target type '$3$'.";
	case 2:
	    return "Context: GfsTemplateTarget configuration element with 'inputs'='$1$', map entry with type#rule '$2$'.";
	case 4:
	    return "Number format exception while converting the value of configuration parameter '$1$' to an integer. Exception message: $2$. Ensure that the parameter value is an integer.";

	case 104:
	    return "Parameter '$1$' is set in the configuration, but has a blank value, which is not allowed for that parameter.";
	case 105:
	    return "Invalid map entry: parameter '$1$' is set, with characteristic '$2$', but no value is defined for the characteristic.";
	case 106:
	    return "Invalid map entry: parameter '$1$' is set, with characteristic '$2$'. The characteristic has value '$3$', which is not allowed for target type $4$";
	case 107:
	    return "Invalid map entry: parameter '$1$' is set, with characteristic '$2$'. The characteristic has value '$3$', which is not one of the recognized values: $4$";
	case 108:
	    return "Invalid map entry for type '$1$': the target type is undefined.";
	case 109:
	    return "Invalid map entry for type '$1$': target type '$2$' is not one of the allowed values. Check for typos or whitespace characters and correct the target type.";
	case 110:
	    return "Parameter '$1$' is set to '$2$', which is not a valid value for the parameter.";

	default:
	    return "(" + GfsTemplateTargetConfigurationValidator.class.getName() + ") Unknown message with number: "
		    + mnr;
	}
    }
}
