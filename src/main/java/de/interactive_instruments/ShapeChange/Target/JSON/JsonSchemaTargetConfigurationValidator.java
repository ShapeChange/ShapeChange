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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange.Target.JSON;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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
import org.w3c.dom.Element;

import de.interactive_instruments.ShapeChange.AbstractConfigurationValidator;
import de.interactive_instruments.ShapeChange.MapEntryParamInfos;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessConfiguration;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.ShapeChangeParseException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.TargetConfiguration;
import de.interactive_instruments.ShapeChange.Model.DescriptorAndTagResolver;
import de.interactive_instruments.ShapeChange.Target.JSON.config.AbstractJsonSchemaAnnotationElement;
import de.interactive_instruments.ShapeChange.Target.JSON.config.SimpleAnnotationElement;
import de.interactive_instruments.ShapeChange.Target.JSON.config.TemplateAnnotationElement;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.JsonSchemaType;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class JsonSchemaTargetConfigurationValidator extends AbstractConfigurationValidator {

    protected SortedSet<String> allowedParametersWithStaticNames = new TreeSet<>(Stream.of(
	    JsonSchemaConstants.PARAM_BASE_JSON_SCHEMA_DEF_COLLECTIONS,
	    JsonSchemaConstants.PARAM_BASE_JSON_SCHEMA_DEF_DATA_TYPES,
	    JsonSchemaConstants.PARAM_BASE_JSON_SCHEMA_DEF_DATA_TYPES_ENCODING_INFOS,
	    JsonSchemaConstants.PARAM_BASE_JSON_SCHEMA_DEF_FEATURE_TYPES,
	    JsonSchemaConstants.PARAM_BASE_JSON_SCHEMA_DEF_FEATURE_TYPES_ENCODING_INFOS,
	    JsonSchemaConstants.PARAM_BASE_JSON_SCHEMA_DEF_OBJECT_TYPES,
	    JsonSchemaConstants.PARAM_BASE_JSON_SCHEMA_DEF_OBJECT_TYPES_ENCODING_INFOS,
	    JsonSchemaConstants.PARAM_BY_REFERENCE_JSON_SCHEMA_DEFINITION,
	    JsonSchemaConstants.PARAM_BY_REFERENCE_FORMAT, JsonSchemaConstants.PARAM_COLLECTION_SCHEMA_FILE_NAME,
	    JsonSchemaConstants.PARAM_CREATE_SEPARATE_PROPERTY_DEFINITIONS,
	    JsonSchemaConstants.PARAM_DOCUMENTATION_NOVALUE, JsonSchemaConstants.PARAM_DOCUMENTATION_TEMPLATE,
	    JsonSchemaConstants.PARAM_ENTITY_TYPE_NAME, JsonSchemaConstants.PARAM_FEATURE_COLLECTION_ONLY,
	    JsonSchemaConstants.PARAM_FEATURE_REF_ID_TYPES, JsonSchemaConstants.PARAM_FEATURE_REF_PROFILES,
	    JsonSchemaConstants.PARAM_FEATURE_REF_ANY_COLLECTION_ID, JsonSchemaConstants.PARAM_GENERIC_VALUE_TYPES,
	    JsonSchemaConstants.PARAM_GEOJSON_COMPATIBLE_GEOMETRY_TYPES,
	    JsonSchemaConstants.PARAM_ID_MEMBER_ENCODING_RESTRICTIONS, JsonSchemaConstants.PARAM_INLINEORBYREF_DEFAULT,
	    JsonSchemaConstants.PARAM_JSON_BASE_URI, JsonSchemaConstants.PARAM_JSON_SCHEMA_VERSION,
	    JsonSchemaConstants.PARAM_LINK_OBJECT_URI, JsonSchemaConstants.PARAM_LOWER_CASE_COLLID_REL_AS_KEY,
	    JsonSchemaConstants.PARAM_MEASURE_OBJECT_URI, JsonSchemaConstants.PARAM_OBJECT_IDENTIFIER_NAME,
	    JsonSchemaConstants.PARAM_OBJECT_IDENTIFIER_REQUIRED, JsonSchemaConstants.PARAM_OBJECT_IDENTIFIER_TYPE,
	    JsonSchemaConstants.PARAM_PREVENT_UNKNOWN_TYPES_IN_FEATURE_COLLECTIONS,
	    JsonSchemaConstants.PARAM_PRETTY_PRINT, JsonSchemaConstants.PARAM_SCHEMA_DEF_VOIDABLE,
	    JsonSchemaConstants.PARAM_USE_ANCHOR_IN_LINKS_TO_GEN_SCHEMA_DEFS,
	    JsonSchemaConstants.PARAM_WRITE_MAP_ENTRIES).collect(Collectors.toSet()));
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

	// validation of known JSON Schema map entry parameters
	isValid = isValid && checkMapEntryParameters(mepis);

	String inlineOrByRefDefault = options.parameterAsString(this.getClass().getName(),
		JsonSchemaConstants.PARAM_INLINEORBYREF_DEFAULT, "byreference", false, true);

	if (!("inline".equalsIgnoreCase(inlineOrByRefDefault) || "byreference".equalsIgnoreCase(inlineOrByRefDefault)
		|| "inlineorbyreference".equalsIgnoreCase(inlineOrByRefDefault))) {

	    isValid = false;
	    result.addError(this, 100, JsonSchemaConstants.PARAM_INLINEORBYREF_DEFAULT, inlineOrByRefDefault);
	}

	String byRefFormat = options.parameterAsString(this.getClass().getName(),
		JsonSchemaConstants.PARAM_BY_REFERENCE_FORMAT, "uri", false, true);

	if (!"uri".equalsIgnoreCase(byRefFormat) && !"uri-reference".equalsIgnoreCase(byRefFormat)) {

	    isValid = false;
	    result.addError(this, 100, JsonSchemaConstants.PARAM_BY_REFERENCE_FORMAT, byRefFormat);
	}

	isValid = isValid && checkEncodingInfosParameter(
		JsonSchemaConstants.PARAM_BASE_JSON_SCHEMA_DEF_DATA_TYPES_ENCODING_INFOS);
	isValid = isValid && checkEncodingInfosParameter(
		JsonSchemaConstants.PARAM_BASE_JSON_SCHEMA_DEF_FEATURE_TYPES_ENCODING_INFOS);
	isValid = isValid && checkEncodingInfosParameter(
		JsonSchemaConstants.PARAM_BASE_JSON_SCHEMA_DEF_OBJECT_TYPES_ENCODING_INFOS);

	isValid = isValid
		&& checkEncodingRestrictionsParameter(JsonSchemaConstants.PARAM_ID_MEMBER_ENCODING_RESTRICTIONS);

	List<String> featureRefProfiles = options.parameterAsStringList(this.getClass().getName(),
		JsonSchemaConstants.PARAM_FEATURE_REF_PROFILES, new String[] { "rel-as-link" }, false, true);
	if (featureRefProfiles.stream().anyMatch(s -> !("rel-as-link".equalsIgnoreCase(s)
		|| "rel-as-uri".equalsIgnoreCase(s) || "rel-as-key".equalsIgnoreCase(s)))) {

	    List<String> invalidFeatureRefProfiles = featureRefProfiles.stream()
		    .filter(s -> !("rel-as-link".equalsIgnoreCase(s) || "rel-as-uri".equalsIgnoreCase(s)
			    || "rel-as-key".equalsIgnoreCase(s)))
		    .collect(Collectors.toList());
	    isValid = false;
	    result.addError(this, 113, JsonSchemaConstants.PARAM_FEATURE_REF_PROFILES,
		    StringUtils.join(invalidFeatureRefProfiles, ", "));
	}

	List<String> featureRefIdTypes = options.parameterAsStringList(this.getClass().getName(),
		JsonSchemaConstants.PARAM_FEATURE_REF_ID_TYPES, new String[] { "integer" }, false, true);
	if (featureRefIdTypes.stream()
		.anyMatch(s -> !("string".equalsIgnoreCase(s) || "integer".equalsIgnoreCase(s)))) {

	    List<String> invalidFeatureRefIdTypes = featureRefIdTypes.stream()
		    .filter(s -> !("string".equalsIgnoreCase(s) || "integer".equalsIgnoreCase(s)))
		    .collect(Collectors.toList());
	    isValid = false;
	    result.addError(this, 113, JsonSchemaConstants.PARAM_FEATURE_REF_ID_TYPES,
		    StringUtils.join(invalidFeatureRefIdTypes, ", "));
	}

	// ===== JSON Schema annotations =====

	Pattern templatePattern = Pattern.compile("\\[\\[(.+?)\\]\\]");

	if (config.getAdvancedProcessConfigurations() != null) {

	    Element advancedProcessConfigElmt = config.getAdvancedProcessConfigurations();

	    // identify annotation elements
	    try {
		List<AbstractJsonSchemaAnnotationElement> annotationElmts = AnnotationGenerator
			.parseAndValidateJsonSchemaAnnotationElements(advancedProcessConfigElmt);

		for (AbstractJsonSchemaAnnotationElement annElmt : annotationElmts) {

		    if (annElmt instanceof SimpleAnnotationElement) {
			SimpleAnnotationElement ann = (SimpleAnnotationElement) annElmt;
			String desc = ann.getDescriptorOrTaggedValue();
			if (desc.startsWith("TV")) {
			    Matcher m = DescriptorAndTagResolver.taggedValuePattern.matcher(desc);
			    if (!m.matches()) {
				result.addError(this, 109, desc, ann.getAnnotation());
				isValid = false;
			    }
			}
		    } else {
			TemplateAnnotationElement ann = (TemplateAnnotationElement) annElmt;

			/* Check valueTemplate */
			Matcher matcher = templatePattern.matcher(ann.getValueTemplate());
			while (matcher.find()) {
			    String desc = matcher.group(1).trim();
			    if (desc.startsWith("TV")) {
				Matcher m = DescriptorAndTagResolver.taggedValuePattern.matcher(desc);
				if (!m.matches()) {
				    result.addError(this, 110, desc, ann.getAnnotation());
				    isValid = false;
				}
			    }
			}
		    }
		}
	    } catch (ShapeChangeParseException e) {
		isValid = false;
		result.addError(this, 112, e.getMessage());
	    }
	}

	return isValid;
    }

    private boolean checkEncodingInfosParameter(String parameterName) {

	if (targetConfig.hasParameter(parameterName)) {

	    String paramValue = targetConfig.parameterAsString(parameterName, null, false, true);

	    try {
		EncodingInfos.from(paramValue);
	    } catch (IllegalArgumentException e) {

		result.addError(this, 111, parameterName, paramValue, e.getMessage());
		return false;
	    }
	}

	return true;
    }

    private boolean checkEncodingRestrictionsParameter(String parameterName) {

	if (targetConfig.hasParameter(parameterName)) {

	    String paramValue = targetConfig.parameterAsString(parameterName, null, false, true);

	    try {
		EncodingRestrictions.from(paramValue);
	    } catch (IllegalArgumentException e) {

		result.addError(this, 111, parameterName, paramValue, e.getMessage());
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

	    } else if (characteristicsByParameter.containsKey(JsonSchemaConstants.ME_PARAM_ENCODING_INFOS)) {

		try {
		    EncodingInfos.from(characteristicsByParameter.get(JsonSchemaConstants.ME_PARAM_ENCODING_INFOS));
		} catch (IllegalArgumentException e) {
		    isValid = false;
		    MessageContext mc = result.addError(this, 105, JsonSchemaConstants.ME_PARAM_ENCODING_INFOS,
			    e.getMessage());
		    if (mc != null) {
			mc.addDetail(this, 1, this.targetConfigInputs, typeRuleKey, targetType);
		    }
		}

	    } else if (characteristicsByParameter.containsKey(JsonSchemaConstants.ME_PARAM_COLLECTION_INFOS)) {

		Map<String, String> collectionInfosCharacteristis = characteristicsByParameter
			.get(JsonSchemaConstants.ME_PARAM_COLLECTION_INFOS);

		// uriTemplate is required
		if (!collectionInfosCharacteristis
			.containsKey(JsonSchemaConstants.ME_PARAM_COLLECTION_INFOS_CHAR_URI_TEMPLATE)
			|| StringUtils.isBlank(collectionInfosCharacteristis
				.get(JsonSchemaConstants.ME_PARAM_COLLECTION_INFOS_CHAR_URI_TEMPLATE))) {
		    isValid = false;
		    MessageContext mc = result.addError(this, 116,
			    JsonSchemaConstants.ME_PARAM_COLLECTION_INFOS_CHAR_URI_TEMPLATE);
		    if (mc != null) {
			mc.addDetail(this, 1, this.targetConfigInputs, typeRuleKey, targetType);
		    }

		} else {

		    String uriTemplate = collectionInfosCharacteristis
			    .get(JsonSchemaConstants.ME_PARAM_COLLECTION_INFOS_CHAR_URI_TEMPLATE).trim();

		    if (!uriTemplate.contains("(featureId)")) {
			isValid = false;
			MessageContext mc = result.addError(this, 117,
				JsonSchemaConstants.ME_PARAM_COLLECTION_INFOS_CHAR_URI_TEMPLATE, uriTemplate);
			if (mc != null) {
			    mc.addDetail(this, 1, this.targetConfigInputs, typeRuleKey, targetType);
			}
		    }
		}

		// collection id types must have valid values
		if (collectionInfosCharacteristis
			.containsKey(JsonSchemaConstants.ME_PARAM_COLLECTION_INFOS_CHAR_COLLECTION_ID_TYPES)) {

		    String meCharCollectionIdTypes = collectionInfosCharacteristis
			    .get(JsonSchemaConstants.ME_PARAM_COLLECTION_INFOS_CHAR_COLLECTION_ID_TYPES);

		    if (StringUtils.isBlank(meCharCollectionIdTypes)) {

			isValid = false;
			MessageContext mc = result.addError(this, 114, JsonSchemaConstants.ME_PARAM_COLLECTION_INFOS);
			if (mc != null) {
			    mc.addDetail(this, 1, this.targetConfigInputs, typeRuleKey, targetType);
			}

		    } else {

			String[] meCollectionIdTypes = StringUtils.split(meCharCollectionIdTypes, ", ");
			for (String s : meCollectionIdTypes) {
			    if (!("string".equalsIgnoreCase(s) || "integer".equalsIgnoreCase(s))) {
				isValid = false;
				MessageContext mc = result.addError(this, 115,
					JsonSchemaConstants.ME_PARAM_COLLECTION_INFOS, s);
				if (mc != null) {
				    mc.addDetail(this, 1, this.targetConfigInputs, typeRuleKey, targetType);
				}
				break;
			    }
			}
		    }

		} else {
		    // default will apply (integer)
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
	    return "Invalid map entry: parameter '$1$' is invalid. Details: $2$";
	case 106:
	    return "Invalid map entry: parameter '$1$' has characteristic '$2$', which is not supported for the target type of the map entry (which is '$3$').";
	case 107:
	    return "Invalid map entry: parameter '$1$' is set, but its characteristic '$2$' has value '$3$', which cannot be parsed as double (which is required for that characteristic and/or the target type of the map entry, which is '$4$').";
	case 108:
	    return "Invalid map entry: parameter '$1$' is set, but its characteristic '$2$' has value '$3$', which cannot be parsed as integer (which is required for that characteristic and/or the target type of the map entry, which is '$4$').";
	case 109:
	    return "Value '$1$' in @descriptorOrTaggedValue of SimpleAnnotation configuration element with @annotation '$2$' does not match regular expression TV(\\(.+?\\))?:(.+)";
	case 110:
	    return "Value of field [[$1$]] in @valueTemplate of TemplateAnnotation configuration element with @annotation '$2$' does not match regular expression TV(\\(.+?\\))?:(.+)";
	case 111:
	    return "Parameter '$1$' is set to '$2$'. This is not a valid value. Details: $3$";
	case 112:
	    return "Invalid JSON Schema annotation(s) encountered: $1$";
	case 113:
	    return "Parameter '$1$' contains the following invalid values: '$2$'";
	case 114:
	    return "Invalid map entry: parameter '$1$' is invalid. Characteristic "
		    + JsonSchemaConstants.ME_PARAM_COLLECTION_INFOS_CHAR_COLLECTION_ID_TYPES
		    + " is defined but has no value.";
	case 115:
	    return "Invalid map entry: parameter '$1$' is invalid. Characteristic "
		    + JsonSchemaConstants.ME_PARAM_COLLECTION_INFOS_CHAR_COLLECTION_ID_TYPES
		    + " is defined with invalid value '$2$'. Only use valid values.";
	case 116:
	    return "Invalid map entry: parameter '$1$' is invalid. Required characteristic "
		    + JsonSchemaConstants.ME_PARAM_COLLECTION_INFOS_CHAR_URI_TEMPLATE
		    + " is undefined or has no value.";
	case 117:
	    return "Invalid map entry: parameter '$1$' is invalid. The value of characteristic "
		    + JsonSchemaConstants.ME_PARAM_COLLECTION_INFOS_CHAR_URI_TEMPLATE
		    + " does not contain the mandatory variable '(featureId)'. The value is: $2$";

	default:
	    return "(" + JsonSchemaTargetConfigurationValidator.class.getName() + ") Unknown message with number: "
		    + mnr;
	}
    }
}
