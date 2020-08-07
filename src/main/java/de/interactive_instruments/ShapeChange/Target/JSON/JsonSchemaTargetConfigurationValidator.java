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
package de.interactive_instruments.ShapeChange.Target.JSON;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.ShapeChange.ConfigurationValidator;
import de.interactive_instruments.ShapeChange.MapEntryParamInfos;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessConfiguration;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.TargetConfiguration;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.JsonSchemaType;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class JsonSchemaTargetConfigurationValidator implements ConfigurationValidator, MessageSource {

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

	SortedMap<String, ProcessMapEntry> mapEntryByType = new TreeMap<String, ProcessMapEntry>();

	for (ProcessMapEntry pme : config.getMapEntries()) {
	    mapEntryByType.put(pme.getType(), pme);
	}

	// general validation of map entry parameters
	MapEntryParamInfos mepis = new MapEntryParamInfos(result, mapEntryByType.values());

	isValid = isValid && mepis.isValid();

	// validation of known JSON Schema map entry parameters
	isValid = isValid && checkMapEntryParameters(mepis);

	String inlineOrByRefDefault = options.parameterAsString(this.getClass().getName(),
		JsonSchemaConstants.PARAM_INLINEORBYREF_DEFAULT, "byreference", false, true);

	if (!("inline".equalsIgnoreCase(inlineOrByRefDefault) || "byreference".equalsIgnoreCase(inlineOrByRefDefault)
		|| "inlineorbyreference".equalsIgnoreCase(inlineOrByRefDefault))) {

	    isValid = false;
	    result.addError(this, 100, JsonSchemaConstants.PARAM_INLINEORBYREF_DEFAULT, inlineOrByRefDefault);
	}

	return isValid;
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

	    Map<String, Map<String, String>> characteristicsByParameter = entry.getValue();

	    if (characteristicsByParameter.containsKey(JsonSchemaConstants.ME_PARAM_KEYWORDS)) {

		Map<String, String> characteristics = characteristicsByParameter
			.get(JsonSchemaConstants.ME_PARAM_KEYWORDS);

		Optional<JsonSchemaType> simpleType = JsonSchemaType.fromString(targetType);

		if (simpleType.isPresent()) {

		    JsonSchemaType jsType = simpleType.get();

		    for (String characteristic : characteristics.keySet()) {

			String value = characteristics.get(characteristic);

			if (StringUtils.isBlank(value)) {

			    isValid = false;
			    MessageContext mc = result.addError(this, 102, JsonSchemaConstants.ME_PARAM_KEYWORDS,
				    characteristic);
			    if (mc != null) {
				mc.addDetail(this, 1, this.targetConfigInputs, typeRuleKey, targetType);
			    }

			} else if (characteristic.equalsIgnoreCase("format")) {

			    // no specific checks (yet)

			} else if (jsType == JsonSchemaType.INTEGER || jsType == JsonSchemaType.NUMBER) {

			    if (characteristic.equalsIgnoreCase("enum")) {

				String[] values = value.split("\\s*,\\s*");
				double[] doubleValues = new double[values.length];

				try {
				    for (int i = 0; i < values.length; i++) {
					double d = Double.parseDouble(values[i]);
					doubleValues[i] = d;
				    }
				} catch (NumberFormatException e) {
				    isValid = false;
				    MessageContext mc = result.addError(this, 103,
					    JsonSchemaConstants.ME_PARAM_KEYWORDS, characteristic, value, targetType);
				    if (mc != null) {
					mc.addDetail(this, 1, this.targetConfigInputs, typeRuleKey, targetType);
				    }
				}

			    } else {

				try {

				    double d = Double.parseDouble(value);

				    if (characteristic.equalsIgnoreCase("multipleOf")) {

					if (d <= 0) {
					    isValid = false;
					    MessageContext mc = result.addError(this, 104,
						    JsonSchemaConstants.ME_PARAM_KEYWORDS, characteristic, value);
					    if (mc != null) {
						mc.addDetail(this, 1, this.targetConfigInputs, typeRuleKey, targetType);
					    }
					}

				    } else if (characteristic.equalsIgnoreCase("maximum")
					    || characteristic.equalsIgnoreCase("minimum")
					    || characteristic.equalsIgnoreCase("exclusiveMinimum")
					    || characteristic.equalsIgnoreCase("exclusiveMaximum")
					    || characteristic.equalsIgnoreCase("const")) {

					// no specific checks (yet)

				    } else {
					isValid = false;
					MessageContext mc = result.addError(this, 106,
						JsonSchemaConstants.ME_PARAM_KEYWORDS, characteristic, targetType);
					if (mc != null) {
					    mc.addDetail(this, 1, this.targetConfigInputs, typeRuleKey, targetType);
					}
				    }

				} catch (NumberFormatException e) {
				    isValid = false;
				    MessageContext mc = result.addError(this, 107,
					    JsonSchemaConstants.ME_PARAM_KEYWORDS, characteristic, value, targetType);
				    if (mc != null) {
					mc.addDetail(this, 1, this.targetConfigInputs, typeRuleKey, targetType);
				    }
				}
			    }

			} else if (jsType == JsonSchemaType.STRING) {

			    if (characteristic.equalsIgnoreCase("enum") || characteristic.equalsIgnoreCase("const")
				    || characteristic.equalsIgnoreCase("pattern")
				    || characteristic.equalsIgnoreCase("patternBase64")) {

				// no specific checks (yet)

			    } else if (characteristic.equalsIgnoreCase("maxLength")
				    || characteristic.equalsIgnoreCase("minLength")) {

				try {

				    int i = Integer.parseInt(value);

				    if (i <= 0) {
					isValid = false;
					MessageContext mc = result.addError(this, 104,
						JsonSchemaConstants.ME_PARAM_KEYWORDS, characteristic, value);
					if (mc != null) {
					    mc.addDetail(this, 1, this.targetConfigInputs, typeRuleKey, targetType);
					}
				    }

				} catch (NumberFormatException e) {
				    isValid = false;
				    MessageContext mc = result.addError(this, 108,
					    JsonSchemaConstants.ME_PARAM_KEYWORDS, characteristic, value, targetType);
				    if (mc != null) {
					mc.addDetail(this, 1, this.targetConfigInputs, typeRuleKey, targetType);
				    }
				}

			    } else {

				isValid = false;
				MessageContext mc = result.addError(this, 106, JsonSchemaConstants.ME_PARAM_KEYWORDS,
					characteristic, targetType);
				if (mc != null) {
				    mc.addDetail(this, 1, this.targetConfigInputs, typeRuleKey, targetType);
				}
			    }

			} else {

			    isValid = false;
			    MessageContext mc = result.addError(this, 106, JsonSchemaConstants.ME_PARAM_KEYWORDS,
				    characteristic, targetType);
			    if (mc != null) {
				mc.addDetail(this, 1, this.targetConfigInputs, typeRuleKey, targetType);
			    }
			}
		    }

		} else {

		    // map entry defines a reference
		    if (!characteristics.isEmpty()) {
			isValid = false;
			MessageContext mc = result.addError(this, 101);
			if (mc != null) {
			    mc.addDetail(this, 1, this.targetConfigInputs, typeRuleKey, targetType);
			}
		    }
		}
	    }
	}

	return isValid;

    }

    @Override
    public String message(int mnr) {

	switch (mnr) {
	case 0:
	    return "Context: JsonSchemaTarget configuration element with 'inputs'='$1$'.";
	case 1:
	    return "Context: JsonSchemaTarget configuration element with 'inputs'='$1$', map entry with type#rule '$2$' and target type '$3$'.";

	case 100:
	    return "Parameter '$1$' is set to '$2$'. This is not a valid value.";
	case 101:
	    return "Invalid map entry: target type is not a simple JSON Schema type but parameter 'keywords' is set. Setting keywords for a schema reference is not supported.";
	case 102:
	    return "Invalid map entry: parameter '$1$' is set, but its characteristic '$2$' has no value (which is required for the parameter).";
	case 103:
	    return "Invalid map entry: parameter '$1$' is set, but its characteristic '$2$' has one or more values ('$3$'), at least one of which cannot be parsed as double (which is required for that characteristic and/or the target type of the map entry, which is '$4$').";
	case 104:
	    return "Invalid map entry: parameter '$1$' with characteristic '$2$' is set, but the value of the characteristic is not a non-negative number (found: '$3$') (which is required for that characteristic).";
	case 105:
	    return "";
	case 106:
	    return "Invalid map entry: parameter '$1$' has characteristic '$2$', which is not supported for the target type of the map entry (which is '$3$').";
	case 107:
	    return "Invalid map entry: parameter '$1$' is set, but its characteristic '$2$' has value '$3$', which cannot be parsed as double (which is required for that characteristic and/or the target type of the map entry, which is '$4$').";
	case 108:
	    return "Invalid map entry: parameter '$1$' is set, but its characteristic '$2$' has value '$3$', which cannot be parsed as integer (which is required for that characteristic and/or the target type of the map entry, which is '$4$').";
	
	default:
	    return "(" + JsonSchemaTargetConfigurationValidator.class.getName() + ") Unknown message with number: "
		    + mnr;
	}
    }
}
