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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import de.interactive_instruments.ShapeChange.MapEntryParamInfos;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.JSON.json.JsonBoolean;
import de.interactive_instruments.ShapeChange.Target.JSON.json.JsonInteger;
import de.interactive_instruments.ShapeChange.Target.JSON.json.JsonNumber;
import de.interactive_instruments.ShapeChange.Target.JSON.json.JsonString;
import de.interactive_instruments.ShapeChange.Target.JSON.json.JsonValue;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.JsonSchema;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.JsonSchemaType;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.JsonSchemaVersion;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.JsonSerializationContext;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class JsonSchemaDocument implements MessageSource {

    protected Options options;
    protected ShapeChangeResult result;
    protected Model model;
    protected JsonSchemaTarget jsonSchemaTarget;
    protected String docName;
    protected PackageInfo representedPackage;
    protected String jsonBaseUri;
    protected String jsonSubdirectory;
    protected String rootSchemaId;
    protected File jsonSchemaOutputFile;
    protected MapEntryParamInfos mapEntryParamInfos;
    protected JsonSchemaVersion jsonSchemaVersion;

    protected SortedMap<String, ClassInfo> classesByName = new TreeMap<>();
    protected Map<ClassInfo, JsonSchemaTypeInfo> basicTypeInfoByClass = new HashMap<>();

    protected JsonSchema rootSchema = new JsonSchema();

    public JsonSchemaDocument(PackageInfo representedPackage, Model model, Options options, ShapeChangeResult result,
	    String docName, JsonSchemaTarget jsonSchemaTarget, String jsonBaseUri, String jsonSubdirectory,
	    File jsonSchemaOutputFile, MapEntryParamInfos mapEntryParamInfos) {

	this.representedPackage = representedPackage;
	this.options = options;
	this.result = result;
	this.model = model;
	this.jsonSchemaTarget = jsonSchemaTarget;
	this.docName = docName;
	this.jsonBaseUri = jsonBaseUri;
	this.jsonSubdirectory = jsonSubdirectory;
	this.jsonSchemaOutputFile = jsonSchemaOutputFile;
	this.mapEntryParamInfos = mapEntryParamInfos;

	rootSchemaId = StringUtils.join(new String[] { StringUtils.removeEnd(jsonBaseUri, "/"),
		StringUtils.removeEnd(jsonSubdirectory, "/"), docName }, "/");

	jsonSchemaVersion = jsonSchemaTarget.getJsonSchemaVersion();

	// add JSON Schema version, if a URI is defined for the schema version
	if (jsonSchemaVersion.getSchemaUri().isPresent()) {
	    rootSchema.schema(jsonSchemaVersion.getSchemaUri().get());
	}

	// add schema identifier
	rootSchema.id(rootSchemaId);

	// TODO add schema comment?
    }

    public void addClass(ClassInfo ci) {
	classesByName.put(ci.name(), ci);
    }

    public void addBasicType(ClassInfo ci, JsonSchemaTypeInfo simpleJsTypeInfo) {
	classesByName.put(ci.name(), ci);
	basicTypeInfoByClass.put(ci, simpleJsTypeInfo);
    }

    public boolean hasClasses() {
	return !classesByName.isEmpty();
    }

    /**
     * Retrieve the "$id" of this JSON Schema document, which is the combination of
     * base URI, subdirectory, and document name.
     * 
     * @return the "$id" of the schema
     */
    public String getSchemaId() {
	return this.rootSchemaId;
    }

    public JsonSchemaVersion getSchemaVersion() {
	return this.jsonSchemaVersion;
    }

    public void createDefinitions() {

	for (ClassInfo ci : this.classesByName.values()) {

	    if (!ci.supertypes().isEmpty() && !ci.matches(JsonSchemaConstants.RULE_CLS_GENERALIZATION)) {
		// warn that generalization is not encoded for the type
		MessageContext mc = result.addWarning(this, 111, ci.name());
		if (mc != null) {
		    mc.addDetail(this, 0, ci.fullName());
		}
	    }

	    JsonSchema js;

	    if (basicTypeInfoByClass.containsKey(ci)) {
		js = jsonSchemaForBasicType(ci);
	    } else if (ci.category() == Options.UNION
		    && ci.matches(JsonSchemaConstants.RULE_CLS_UNION_TYPE_DISCRIMINATOR)) {
		js = jsonSchemaForTypeDiscriminatorUnion(ci);
	    } else if (ci.category() == Options.ENUMERATION) {
		js = jsonSchemaForEnumeration(ci);
	    } else if (ci.category() == Options.CODELIST) {
		js = jsonSchemaForCodelist(ci);
	    } else {
		/*
		 * Union (not type discriminator), data type, mixin, object type, feature type
		 */
		js = jsonSchema(ci);
	    }

	    if (jsonSchemaVersion == JsonSchemaVersion.DRAFT_2019_09) {
		rootSchema.def(ci.name(), js);
	    } else {
		rootSchema.definition(ci.name(), js);
	    }
	}
    }

    private JsonSchema jsonSchemaForBasicType(ClassInfo ci) {

	/*
	 * Add restricting facets based on tagged values defined for ci (and if
	 * applicable for the simple JSON Schema type as which ci ultimately is
	 * implemented).
	 */

	JsonSchema jsClass = new JsonSchema();

	addCommonSchemaMembers(jsClass, ci);

	List<JsonSchema> allOfMembers = new ArrayList<>();

	// There should be exactly one supertype for a basic type
	ClassInfo supertype = ci.supertypeClasses().first();

	// add schema definition for supertype
	Optional<JsonSchemaTypeInfo> supertypeTypeInfo = identifyJsonSchemaTypeForSupertype(supertype,
		ci.encodingRule(JsonSchemaConstants.PLATFORM));

	if (supertypeTypeInfo.isPresent()) {

	    JsonSchema parentForSupertypeTypeDefSchema = new JsonSchema();
	    createTypeDefinition(supertypeTypeInfo.get(), parentForSupertypeTypeDefSchema);

	    allOfMembers.add(parentForSupertypeTypeDefSchema);

	} else {
	    // ignore; error message has already been logged
	}

	// now identify the restricting facets defined by ci
	JsonSchemaTypeInfo jsImplementationTypeInfo = basicTypeInfoByClass.get(ci);
	JsonSchema restrictingFacetSchema = new JsonSchema();

	if (jsImplementationTypeInfo.getSimpleType() == JsonSchemaType.STRING) {

	    String length = ci.taggedValue("length");
	    if (length == null) {
		length = ci.taggedValue("maxLength");
	    }

	    if (StringUtils.isNotBlank(length)) {

		try {
		    int lengthValue = Integer.parseInt(length.trim());
		    if (lengthValue < 0) {
			MessageContext mc = result.addError(this, 102, ci.name(), length);
			if (mc != null) {
			    mc.addDetail(this, 0, ci.fullName());
			}
		    } else {
			restrictingFacetSchema.maxLength(lengthValue);
		    }
		} catch (NumberFormatException e) {
		    MessageContext mc = result.addError(this, 101, ci.name(), length);
		    if (mc != null) {
			mc.addDetail(this, 0, ci.fullName());
		    }
		}
	    }

	    String pattern = ci.taggedValue("pattern");

	    if (StringUtils.isNotBlank(pattern)) {
		restrictingFacetSchema.pattern(pattern);
	    }

	} else if (jsImplementationTypeInfo.getSimpleType() == JsonSchemaType.NUMBER
		|| jsImplementationTypeInfo.getSimpleType() == JsonSchemaType.INTEGER) {

	    String min = ci.taggedValue("rangeMinimum");

	    if (StringUtils.isNotBlank(min)) {
		Double minDouble = parseRestrictingFacetAsDouble(ci, min, "rangeMinimum", "minimum");
		if (minDouble != null) {
		    restrictingFacetSchema.minimum(minDouble);
		}
	    }

	    String max = ci.taggedValue("rangeMaximum");

	    if (StringUtils.isNotBlank(max)) {
		Double maxDouble = parseRestrictingFacetAsDouble(ci, max, "rangeMaximum", "maximum");
		if (maxDouble != null) {
		    restrictingFacetSchema.maximum(maxDouble);
		}
	    }

	} else {
	    // must be JsonSchemaType.BOOLEAN - nothing to do
	}

	if (jsImplementationTypeInfo.getSimpleType() != JsonSchemaType.BOOLEAN) {

	    String format = ci.taggedValue("jsonFormat");

	    if (StringUtils.isNotBlank(format)) {
		restrictingFacetSchema.format(format);
	    }
	}

	/*
	 * Only create "allOf" if ci defines a relevant restricting facet - otherwise
	 * just refer to the schema definition from the supertype
	 */
	if (restrictingFacetSchema.isEmpty()) {
	    jsClass.addAll(allOfMembers.get(0));
	} else {
	    allOfMembers.add(restrictingFacetSchema);
	    // create the "allOf"
	    jsClass.allOf(allOfMembers.toArray(new JsonSchema[allOfMembers.size()]));
	}

	return jsClass;
    }

    private Double parseRestrictingFacetAsDouble(ClassInfo ci, String stringValue, String tvName,
	    String jsKeywordName) {

	try {
	    double doubleValue = Double.parseDouble(stringValue.trim());
	    return doubleValue;
	} catch (NumberFormatException e) {
	    MessageContext mc = result.addError(this, 103, ci.name(), stringValue, tvName, jsKeywordName);
	    if (mc != null) {
		mc.addDetail(this, 0, ci.fullName());
	    }
	    return null;
	}
    }

    private JsonSchema jsonSchemaForCodelist(ClassInfo ci) {

	JsonSchema jsClass = new JsonSchema();

	addCommonSchemaMembers(jsClass, ci);

	if (ci.matches(JsonSchemaConstants.RULE_CLS_CODELIST_URI_FORMAT)) {
	    
	    jsClass.type(JsonSchemaType.STRING);
	    jsClass.format("uri");

	} else if (ci.matches(JsonSchemaConstants.RULE_CLS_CODELIST_LINK)) {
	    
	    jsClass.ref(jsonSchemaTarget.getLinkObjectUri());

	} else {

	    if ("Number".equalsIgnoreCase(ci.taggedValue("numericType"))) {
		jsClass.type(JsonSchemaType.NUMBER);
	    } else {
		jsClass.type(JsonSchemaType.STRING);
	    }
	}

	return jsClass;
    }

    private JsonSchema jsonSchemaForEnumeration(ClassInfo ci) {

	JsonSchema jsClass = new JsonSchema();

	addCommonSchemaMembers(jsClass, ci);

	// identify and set the JSON Schema type as which the enumeration is implemented
	JsonSchemaType enumerationJsType;
	if (StringUtils.isNotBlank(ci.taggedValue("numericType"))) {
	    if ("integer".equalsIgnoreCase(ci.taggedValue("numericType"))) {
		enumerationJsType = JsonSchemaType.INTEGER;
	    } else {
		enumerationJsType = JsonSchemaType.NUMBER;
	    }
	} else {
	    enumerationJsType = JsonSchemaType.STRING;
	}

	jsClass.type(enumerationJsType);

	for (PropertyInfo pi : ci.properties().values()) {

	    if (!JsonSchemaTarget.isEncoded(pi)) {
		result.addInfo(this, 9, pi.name(), pi.inClass().name());
		continue;
	    }

	    if (enumerationJsType == JsonSchemaType.INTEGER) {

		String stringValue = StringUtils.isNotBlank(pi.initialValue()) ? pi.initialValue() : pi.name();

		try {
		    int intValue = Integer.parseInt(stringValue);
		    jsClass.enum_(new JsonInteger(intValue));
		} catch (NumberFormatException e) {
		    MessageContext mc = result.addError(this, 109, stringValue, pi.name(), ci.name());
		    if (mc != null) {
			mc.addDetail(this, 1, pi.fullName());
		    }
		}

	    } else if (enumerationJsType == JsonSchemaType.NUMBER) {

		String stringValue = StringUtils.isNotBlank(pi.initialValue()) ? pi.initialValue() : pi.name();

		try {
		    double doubleValue = Double.parseDouble(stringValue);
		    jsClass.enum_(new JsonNumber(doubleValue));
		} catch (NumberFormatException e) {
		    MessageContext mc = result.addError(this, 110, stringValue, pi.name(), ci.name());
		    if (mc != null) {
			mc.addDetail(this, 1, pi.fullName());
		    }
		}
	    } else {

		String value = StringUtils.isNotBlank(pi.initialValue()) ? pi.initialValue() : pi.name();
		jsClass.enum_(new JsonString(value));

	    }
	}

	return jsClass;
    }

    private JsonSchema jsonSchemaForTypeDiscriminatorUnion(ClassInfo ci) {

	JsonSchema jsClass = new JsonSchema();

	addCommonSchemaMembers(jsClass, ci);

	List<JsonSchemaTypeInfo> typeInfos = new ArrayList<>();
	// content is string with typeInfo.name+typeInfo.id
	Set<String> encounteredTypes = new HashSet<>();

	for (PropertyInfo pi : ci.properties().values()) {

	    if (!JsonSchemaTarget.isEncoded(pi)) {
		result.addInfo(this, 9, pi.name(), pi.inClass().name());
		continue;
	    }

	    Type t = pi.typeInfo();
	    String typeKey = StringUtils.stripToEmpty(t.name) + "#" + StringUtils.stripToEmpty(t.id);
	    if (!encounteredTypes.contains(typeKey)) {
		encounteredTypes.add(typeKey);
		Optional<JsonSchemaTypeInfo> jstiOpt = identifyJsonSchemaType(pi);
		if (jstiOpt.isPresent()) {
		    typeInfos.add(jstiOpt.get());
		}
	    }
	}

	createTypeDefinition(typeInfos, jsClass);

	return jsClass;
    }

    /**
     * Currently, only applies {@value JsonSchemaConstants#RULE_CLS_NAME_AS_ANCHOR}
     * 
     * @param jsClass
     * @param ci
     */
    private void addCommonSchemaMembers(JsonSchema jsClass, ClassInfo ci) {

	if (jsonSchemaVersion != JsonSchemaVersion.OPENAPI_30
		&& ci.matches(JsonSchemaConstants.RULE_CLS_NAME_AS_ANCHOR)) {

	    if (jsonSchemaVersion == JsonSchemaVersion.DRAFT_2019_09) {
		jsClass.anchor(ci.name());
	    } else {
		jsClass.id("#" + ci.name());
	    }
	}
    }

    private JsonSchema jsonSchema(ClassInfo ci) {

	JsonSchema jsClass = new JsonSchema();

	addCommonSchemaMembers(jsClass, ci);

	// not an enumeration or code list

	JsonSchema jsClassContents = jsClass;

	List<JsonSchema> allOfMembers = new ArrayList<>();

	SortedSet<ClassInfo> supertypes = ci.supertypeClasses();

	handleVirtualGeneralization(ci, supertypes, allOfMembers);

	if ((ci.category() == Options.FEATURE || ci.category() == Options.OBJECT || ci.category() == Options.DATATYPE
		|| ci.category() == Options.MIXIN) && ci.matches(JsonSchemaConstants.RULE_CLS_GENERALIZATION)) {

	    // convert generalization
	    if (!supertypes.isEmpty()) {

		// add schema definitions for all supertypes
		for (ClassInfo supertype : supertypes) {
		    Optional<JsonSchemaTypeInfo> typeInfo = identifyJsonSchemaTypeForSupertype(supertype,
			    ci.encodingRule(JsonSchemaConstants.PLATFORM));

		    if (typeInfo.isPresent()) {

			if (typeInfo.get().isSimpleType()) {

			    MessageContext mc = result.addError(this, 108, supertype.name(), ci.name(),
				    typeInfo.get().getSimpleType().getName());
			    if (mc != null) {
				mc.addDetail(this, 0, ci.fullName());
			    }

			} else {

			    allOfMembers.add(new JsonSchema().ref(typeInfo.get().getRef()));
			}

		    } else {
			// ignore; error message has already been logged
		    }
		}
	    }
	}

	if (!allOfMembers.isEmpty()) {

	    // prepare the contents schema for ci
	    jsClassContents = new JsonSchema();

	    allOfMembers.add(jsClassContents);

	    // create the "allOf"
	    jsClass.allOf(allOfMembers.toArray(new JsonSchema[allOfMembers.size()]));
	}

	jsClassContents.type(JsonSchemaType.OBJECT);

	PropertyInfo defaultGeometryPi = null;
	if (ci.category() != Options.UNION && !ci.properties().isEmpty()
		&& (ci.matches(JsonSchemaConstants.RULE_CLS_DEFAULT_GEOMETRY_SINGLEGEOMPROP)
			|| ci.matches(JsonSchemaConstants.RULE_CLS_DEFAULT_GEOMETRY_MULTIGEOMPROPS))) {

	    Map<PropertyInfo, JsonSchemaTypeInfo> typeInfoByGeometryPi = new HashMap<>();
	    PropertyInfo directGeometryPi = null;
	    Set<PropertyInfo> directProperties = new HashSet<>(ci.properties().values());

	    for (PropertyInfo pi : ci.propertiesAll()) {

		if (!JsonSchemaTarget.isEncoded(pi)) {
		    result.addInfo(this, 9, pi.name(), pi.inClass().name());
		    continue;
		}

		Optional<JsonSchemaTypeInfo> typeInfoOpt = identifyJsonSchemaType(pi);

		if (typeInfoOpt.isPresent()) {

		    JsonSchemaTypeInfo typeInfo = typeInfoOpt.get();

		    if (typeInfo.isGeometry()
			    && (ci.matches(JsonSchemaConstants.RULE_CLS_DEFAULT_GEOMETRY_SINGLEGEOMPROP)
				    || "true".equalsIgnoreCase(pi.taggedValue("defaultGeometry")))) {

			if (directProperties.contains(pi)) {
			    directGeometryPi = pi;
			}

			typeInfoByGeometryPi.put(pi, typeInfo);
		    }
		}
	    }

	    if (directGeometryPi != null) {

		if (typeInfoByGeometryPi.size() == 1) {

		    // encode the default geometry property
		    jsClassContents.property("geometry",
			    new JsonSchema().ref(typeInfoByGeometryPi.get(directGeometryPi).getRef()));

		    if (directGeometryPi.cardinality().maxOccurs > 1) {
			MessageContext mc = result.addError(this, 117, directGeometryPi.name(), ci.name());
			if (mc != null) {
			    mc.addDetail(this, 1, directGeometryPi.fullName());
			}
		    }

		    /*
		     * keep track of this property, so that it can be ignored later on when encoding
		     * the other properties of the class
		     */
		    defaultGeometryPi = directGeometryPi;

		} else if (typeInfoByGeometryPi.size() > 1) {

		    // inform about multiple default geometry properties
		    String geometryPiNames = typeInfoByGeometryPi.keySet().stream().map(pi -> pi.name()).sorted()
			    .collect(Collectors.joining(", "));

		    MessageContext mc = result.addError(this, 112, ci.name(), geometryPiNames);
		    if (mc != null) {
			mc.addDetail(this, 0, ci.fullName());
		    }
		}
	    }
	}

	JsonSchema jsProperties = jsClassContents;
	if (!(ci.category() == Options.DATATYPE || ci.category() == Options.UNION)
		&& ci.matches(JsonSchemaConstants.RULE_CLS_NESTED_PROPERTIES)) {
	    /*
	     * NOTE: If a class does not define any property in "properties", that member
	     * will be removed later on, together with the requirement for it. If
	     * "properties" has no required members, the requirement for "properties" would
	     * also be removed.
	     */
	    jsProperties = new JsonSchema();
	    jsProperties.type(JsonSchemaType.OBJECT);
	    jsClassContents.property("properties", jsProperties).required("properties");
	}

	if (ci.matches(JsonSchemaConstants.RULE_CLS_NAME_AS_ENTITYTYPE) && !(ci.category() == Options.UNION)) {

	    // check if supertype matches
	    boolean supertypeMatch = false;
	    if (ci.matches(JsonSchemaConstants.RULE_CLS_GENERALIZATION)) {
		for (ClassInfo supertype : ci.supertypeClasses()) {
		    if (supertype.matches(JsonSchemaConstants.RULE_CLS_NAME_AS_ENTITYTYPE)
			    && !(supertype.category() == Options.UNION)) {
			supertypeMatch = true;
			break;
		    }
		}
	    }

	    if (!supertypeMatch) {
		jsProperties
			.property(jsonSchemaTarget.getEntityTypeName(), new JsonSchema().type(JsonSchemaType.STRING))
			.required(jsonSchemaTarget.getEntityTypeName());
	    }
	}

	if ((ci.category() == Options.FEATURE || ci.category() == Options.OBJECT)
		&& ci.matches(JsonSchemaConstants.RULE_CLS_IDENTIFIER_FOR_TYPE_WITH_IDENTITY)
		&& !(ci.matches(JsonSchemaConstants.RULE_CLS_IGNORE_IDENTIFIER)
			|| ci.matches(JsonSchemaConstants.RULE_CLS_IDENTIFIER_STEREOTYPE))) {

	    // check if supertype matches
	    boolean supertypeMatch = false;
	    if (ci.matches(JsonSchemaConstants.RULE_CLS_GENERALIZATION)) {
		for (ClassInfo supertype : ci.supertypeClasses()) {
		    if ((supertype.category() == Options.FEATURE || supertype.category() == Options.OBJECT)
			    && supertype.matches(JsonSchemaConstants.RULE_CLS_IDENTIFIER_FOR_TYPE_WITH_IDENTITY)
			    && !(supertype.matches(JsonSchemaConstants.RULE_CLS_IGNORE_IDENTIFIER)
				    || supertype.matches(JsonSchemaConstants.RULE_CLS_IDENTIFIER_STEREOTYPE))) {
			supertypeMatch = true;
			break;
		    }
		}
	    }

	    if (!supertypeMatch) {
		jsProperties.property(jsonSchemaTarget.objectIdentifierName(),
			new JsonSchema().type(jsonSchemaTarget.objectIdentifierType()));

		if (jsonSchemaTarget.objectIdentifierRequired()) {
		    jsProperties.required(jsonSchemaTarget.objectIdentifierName());
		}
	    }
	}

	String valueTypeOptionsTV = ci.taggedValue("valueTypeOptions");
	Map<String, SortedSet<String>> valueTypeOptionsByPropName = new HashMap<>();
	if (StringUtils.isNotBlank(valueTypeOptionsTV) && ci.matches(JsonSchemaConstants.RULE_CLS_VALUE_TYPE_OPTIONS)) {
	    String[] propValueTypeOptions = StringUtils.split(valueTypeOptionsTV, ";");
	    for (String propValueTypeOption : propValueTypeOptions) {
		String[] optionFacets = StringUtils.split(propValueTypeOption, "=");
		String propertyName = optionFacets[0].trim();
		SortedSet<String> valueTypes = Arrays.stream(optionFacets[1].split(",")).map(s -> s.trim())
			.collect(Collectors.toCollection(TreeSet::new));
		valueTypeOptionsByPropName.put(propertyName, valueTypes);
	    }
	}

	for (PropertyInfo pi : ci.properties().values()) {

	    if (!JsonSchemaTarget.isEncoded(pi)) {
		result.addInfo(this, 9, pi.name(), pi.inClass().name());
		continue;
	    }

	    if (pi == defaultGeometryPi) {
		continue;
	    }

	    if (pi.stereotype("identifier") && ci.matches(JsonSchemaConstants.RULE_CLS_IDENTIFIER_STEREOTYPE)) {

		if (ci.matches(JsonSchemaConstants.RULE_CLS_IGNORE_IDENTIFIER)) {
		    continue;
		} else if (pi.cardinality().maxOccurs > 1) {
		    MessageContext mc = result.addError(this, 113, pi.name(), ci.name());
		    if (mc != null) {
			mc.addDetail(this, 1, pi.fullName());
		    }
		}
	    }

	    Optional<SortedSet<String>> valueTypeOptions = Optional
		    .ofNullable(valueTypeOptionsByPropName.get(pi.name()));

	    jsProperties.property(pi.name(), jsonSchema(pi, valueTypeOptions));

	    valueTypeOptionsByPropName.remove(pi.name());

	    if (ci.category() != Options.UNION) {
		// if the property is not optional, add it to the required properties
		if (pi.cardinality().minOccurs > 0) {
		    jsProperties.required(pi.name());
		}
	    }
	}

	// create property definitions for valueTypeOptions that target properties from
	// supertypes
	for (Entry<String, SortedSet<String>> e : valueTypeOptionsByPropName.entrySet()) {

	    String supertypePropertyName = e.getKey();
	    PropertyInfo supertypePi = ci.property(supertypePropertyName);

	    if (supertypePi == null || ((supertypePi.categoryOfValue() == Options.FEATURE
		    || supertypePi.categoryOfValue() == Options.OBJECT)
		    && "byReference".equalsIgnoreCase(inlineOrByReference(supertypePi)))) {
		/*
		 * no need to restrict the value type of the supertype property, because values
		 * are only encoded by reference
		 */
		continue;
	    }

	    SortedSet<String> supertypePropertyTypeOptions = e.getValue();

	    List<JsonSchemaTypeInfo> supertypePropertyTypeInfos = new ArrayList<>();

	    // Create a JSON Schema to restrict the type of the supertype property
	    JsonSchema supertypePropertyTypeRestrictionSchema = new JsonSchema();

	    // Take into account inline/inlineOrByReference (byReference has already been
	    // ruled out before)
	    if ((supertypePi.categoryOfValue() == Options.FEATURE || supertypePi.categoryOfValue() == Options.OBJECT)
		    && !"inline".equalsIgnoreCase(inlineOrByReference(supertypePi))) {

		supertypePropertyTypeInfos.add(createJsonSchemaTypeInfoForReference());
	    }
	    // Now add the actual type restrictions
	    for (String supertypePropertyTypeOption : supertypePropertyTypeOptions) {

		Optional<JsonSchemaTypeInfo> jstdOpt = identifyJsonSchemaType(supertypePropertyTypeOption, null,
			ci.encodingRule(JsonSchemaConstants.PLATFORM));
		if (jstdOpt.isPresent()) {
		    supertypePropertyTypeInfos.add(jstdOpt.get());
		}
	    }
	    // Take into account multiplicity and voidable of the supertype property
	    JsonSchema parentForTypeSchema = supertypePropertyTypeRestrictionSchema;

	    // Take into account voidable
	    if (supertypePi.voidable() && supertypePi.matches(JsonSchemaConstants.RULE_PROP_VOIDABLE)) {

		// if maxOccurs == 1, simply add null
		if (supertypePi.cardinality().maxOccurs == 1) {

		    JsonSchemaTypeInfo nullTypeInfo = new JsonSchemaTypeInfo();
		    nullTypeInfo.setSimpleType(JsonSchemaType.NULL);
		    supertypePropertyTypeInfos.add(nullTypeInfo);

		} else {

		    // otherwise, we need to create a real choice between a single null value, and
		    // the usual type definition
		    if (jsonSchemaVersion == JsonSchemaVersion.OPENAPI_30) {

			parentForTypeSchema.nullable(true);

		    } else {

			JsonSchema nullTypeSchemaDef = new JsonSchema();
			nullTypeSchemaDef.type(JsonSchemaType.NULL);

			JsonSchema nonNullTypeSchemaDef = new JsonSchema();

			parentForTypeSchema.oneOf(nullTypeSchemaDef, nonNullTypeSchemaDef);

			parentForTypeSchema = nonNullTypeSchemaDef;
		    }
		}
	    }

	    if (supertypePi.cardinality().maxOccurs > 1) {
		parentForTypeSchema.type(JsonSchemaType.ARRAY);
		JsonSchema itemsSchema = new JsonSchema();
		parentForTypeSchema.items(itemsSchema);
		parentForTypeSchema = itemsSchema;
		// "required" and "minItems"/"maxItems" are defined by the supertype schema
	    }
	    // create the actual type restriction schema
	    createTypeDefinition(supertypePropertyTypeInfos, parentForTypeSchema);
	    /*
	     * Finally, add another member to the "properties", to represent the type
	     * restriction of the supertype property
	     */
	    jsProperties.property(supertypePropertyName, supertypePropertyTypeRestrictionSchema);
	}

	if (ci.category() == Options.UNION) {

	    if (ci.matches(JsonSchemaConstants.RULE_CLS_UNION_PROPERTY_COUNT)) {

		jsClassContents.additionalProperties(JsonSchema.FALSE);
		jsClassContents.minProperties(1);
		jsClassContents.maxProperties(1);
	    }
	}

	if (!(ci.category() == Options.DATATYPE || ci.category() == Options.UNION)
		&& ci.matches(JsonSchemaConstants.RULE_CLS_NESTED_PROPERTIES)) {
	    /*
	     * Check if "properties" member exists and is empty; if so, remove it (and the
	     * requirement for it). If the member is not empty, check if it defines required
	     * properties. If not, remove the requirement for the "properties" member.
	     */
	    Optional<LinkedHashMap<String, JsonSchema>> classPropertiesOpt = jsClassContents.properties();
	    if (classPropertiesOpt.isPresent()) {
		LinkedHashMap<String, JsonSchema> classProperties = classPropertiesOpt.get();
		if (classProperties.containsKey("properties")) {
		    JsonSchema propertiesMemberSchema = classProperties.get("properties");
		    Optional<LinkedHashMap<String, JsonSchema>> propertiesMemberPropertiesOpt = propertiesMemberSchema
			    .properties();
		    if (propertiesMemberPropertiesOpt.isEmpty()) {
			// remove "properties" member and requirement for it
			jsClassContents.removeProperty("properties").removeRequired("properties");
		    } else {
			Optional<SortedSet<String>> propertiesMemberRequiredOpt = propertiesMemberSchema.required();
			if (propertiesMemberRequiredOpt.isEmpty() || propertiesMemberRequiredOpt.get().isEmpty()) {
			    // remove requirement for "properties" member
			    jsClassContents.removeRequired("properties");
			}
		    }
		}
	    }
	}

	return jsClass;
    }

    private void handleVirtualGeneralization(ClassInfo ci, SortedSet<ClassInfo> supertypes,
	    List<JsonSchema> allOfMembers) {

	if (ci.matches(JsonSchemaConstants.RULE_CLS_VIRTUAL_GENERALIZATION)) {

	    String baseJsonSchemaDef = null;

	    if (ci.category() == Options.FEATURE) {
		baseJsonSchemaDef = jsonSchemaTarget.baseJsonSchemaDefinitionForFeatureTypes();
	    } else if (ci.category() == Options.OBJECT) {
		baseJsonSchemaDef = jsonSchemaTarget.baseJsonSchemaDefinitionForObjectTypes();
	    } else if (ci.category() == Options.DATATYPE) {
		baseJsonSchemaDef = jsonSchemaTarget.baseJsonSchemaDefinitionForDataTypes();
	    }

	    if (baseJsonSchemaDef != null) {

		boolean applyVirtualGeneralization = true;

		if (!supertypes.isEmpty() && ci.matches(JsonSchemaConstants.RULE_CLS_GENERALIZATION)) {
		    for (ClassInfo supertype : supertypes) {
			if (supertype.matches(JsonSchemaConstants.RULE_CLS_VIRTUAL_GENERALIZATION)) {
			    applyVirtualGeneralization = false;
			    break;
			}
		    }
		}

		if (applyVirtualGeneralization) {
		    allOfMembers.add(new JsonSchema().ref(baseJsonSchemaDef));
		}
	    }
	}
    }

    private String inlineOrByReference(PropertyInfo pi) {

	String s = pi.taggedValue("inlineOrByReference");

	if (StringUtils.isBlank(s)) {
	    return jsonSchemaTarget.getInlineOrByRefDefault();
	} else {
	    return s;
	}
    }

    private JsonSchemaTypeInfo createJsonSchemaTypeInfoForReference() {

	JsonSchemaTypeInfo byRefInfo = new JsonSchemaTypeInfo();

	if (jsonSchemaTarget.byReferenceJsonSchemaDefinition().isPresent()) {
	    byRefInfo.setRef(jsonSchemaTarget.byReferenceJsonSchemaDefinition().get());
	} else {
	    byRefInfo.setSimpleType(JsonSchemaType.STRING);
	    byRefInfo.setFormat("uri");
	}

	return byRefInfo;
    }

    private JsonSchema jsonSchema(PropertyInfo pi, Optional<SortedSet<String>> valueTypeOptions) {

	// convert value type, voidable, and inlineOrByReference

	// First, identify the JSON Schema type infos for all allowed types
	List<JsonSchemaTypeInfo> typeOptions = new ArrayList<>();

	Optional<JsonSchemaTypeInfo> typeInfoOpt = identifyJsonSchemaType(pi);

	if (typeInfoOpt.isPresent()) {

	    JsonSchemaTypeInfo typeInfo = typeInfoOpt.get();

	    boolean byReferenceOnly = false;

	    if ((pi.categoryOfValue() == Options.FEATURE || pi.categoryOfValue() == Options.OBJECT)
		    && !typeInfo.isSimpleType()) {

		boolean addByReferenceOption = false;

		if ("byReference".equalsIgnoreCase(inlineOrByReference(pi))) {
		    byReferenceOnly = true;
		    addByReferenceOption = true;
		} else if (!"inline".equalsIgnoreCase(inlineOrByReference(pi))) {
		    addByReferenceOption = true;
		}

		if (addByReferenceOption) {
		    typeOptions.add(createJsonSchemaTypeInfoForReference());
		}
	    }

	    if (!byReferenceOnly) {

		if (valueTypeOptions.isPresent()) {

		    for (String piValueTypeOption : valueTypeOptions.get()) {

			Optional<JsonSchemaTypeInfo> jstdOpt = identifyJsonSchemaType(piValueTypeOption, null,
				pi.encodingRule(JsonSchemaConstants.PLATFORM));
			if (jstdOpt.isPresent()) {
			    typeOptions.add(jstdOpt.get());
			}
		    }

		} else {

		    typeOptions.add(typeInfo);
		}
	    }
	}

	/*
	 * At this point, all type infos should be available (so that when converting
	 * multiplicity, it can be determined if "items" shall be created).
	 */

	JsonSchema jsProp = new JsonSchema();

	JsonSchema parentForTypeSchema = jsProp;

	// Take into account voidable
	if (pi.voidable() && pi.matches(JsonSchemaConstants.RULE_PROP_VOIDABLE)) {

	    // if maxOccurs == 1 simply add null
	    if (pi.cardinality().maxOccurs == 1) {

		JsonSchemaTypeInfo nullTypeInfo = new JsonSchemaTypeInfo();
		nullTypeInfo.setSimpleType(JsonSchemaType.NULL);
		typeOptions.add(nullTypeInfo);

	    } else {

		// otherwise, we need to create a real choice between a single null value, and
		// the usual type definition

		if (jsonSchemaVersion == JsonSchemaVersion.OPENAPI_30) {

		    parentForTypeSchema.nullable(true);

		} else {

		    JsonSchema nullTypeSchemaDef = new JsonSchema();
		    nullTypeSchemaDef.type(JsonSchemaType.NULL);

		    JsonSchema nonNullTypeSchemaDef = new JsonSchema();

		    jsProp.oneOf(nullTypeSchemaDef, nonNullTypeSchemaDef);

		    parentForTypeSchema = nonNullTypeSchemaDef;
		}
	    }
	}

	// convert multiplicity
	if (pi.cardinality().maxOccurs > 1)

	{

	    parentForTypeSchema.type(JsonSchemaType.ARRAY);

	    if (pi.cardinality().minOccurs > 0) {
		parentForTypeSchema.minItems(pi.cardinality().minOccurs);
	    }

	    if (pi.cardinality().maxOccurs < Integer.MAX_VALUE) {
		parentForTypeSchema.maxItems(pi.cardinality().maxOccurs);
	    }

	    JsonSchema itemsSchema = null;
	    if (!typeOptions.isEmpty()) {
		itemsSchema = new JsonSchema();
		parentForTypeSchema.items(itemsSchema);
	    }

	    if (pi.isUnique()) {
		parentForTypeSchema.uniqueItems(true);
	    }

	    if (itemsSchema != null) {
		parentForTypeSchema = itemsSchema;
	    }
	}

	// --- Create "type" member ---
	createTypeDefinition(typeOptions, parentForTypeSchema);

	// --- convert initial value
	if (typeInfoOpt.isPresent()) {

	    JsonSchemaTypeInfo typeInfo = typeInfoOpt.get();

	    if (pi.isAttribute() && StringUtils.isNotBlank(pi.initialValue())
		    && pi.matches(JsonSchemaConstants.RULE_PROP_INITIAL_VALUE_AS_DEFAULT)) {

		if (typeInfo.isSimpleType()) {

		    if (typeInfo.getSimpleType() == JsonSchemaType.BOOLEAN) {
			jsProp.default_(new JsonBoolean(pi.initialValue().trim()));
		    } else if (typeInfo.getSimpleType() == JsonSchemaType.INTEGER) {

			try {
			    int intValue = Integer.parseInt(pi.initialValue().trim());
			    jsProp.default_(new JsonInteger(intValue));
			} catch (NumberFormatException e) {
			    MessageContext mc = result.addError(this, 106, pi.initialValue(), pi.name());
			    if (mc != null) {
				mc.addDetail(this, 1, pi.fullName());
			    }
			}

		    } else if (typeInfo.getSimpleType() == JsonSchemaType.NUMBER) {
			try {
			    double doubleValue = Double.parseDouble(pi.initialValue().trim());
			    jsProp.default_(new JsonNumber(doubleValue));
			} catch (NumberFormatException e) {
			    MessageContext mc = result.addError(this, 107, pi.initialValue(), pi.name());
			    if (mc != null) {
				mc.addDetail(this, 1, pi.fullName());
			    }
			}

		    } else {
			jsProp.default_(new JsonString(pi.initialValue()));
		    }
		}
	    }
	}

	// --- convert fixed and derived
	if ((pi.isReadOnly() && pi.matches(JsonSchemaConstants.RULE_PROP_READONLY))
		|| (pi.isDerived() && pi.matches(JsonSchemaConstants.RULE_PROP_DERIVEDASREADONLY))) {
	    jsProp.readOnly(true);
	}

	return jsProp;
    }

    /**
     * Create a type definition (in JSON Schema) from the given JSON Schema type
     * info, and add it to the given parent schema object. A pure simple type will
     * be added using a single "type" key, a simple type with specific format will
     * result in a "type" key and format keyword, and a JSON Schema reference will
     * be added as a "$ref".
     * 
     * @param typeInfo            JSON Schema type information object, which
     *                            represents the type definition
     * @param parentForTypeSchema The JSON Schema definition to which the type
     *                            schema definition shall be added
     */
    private void createTypeDefinition(JsonSchemaTypeInfo typeInfo, JsonSchema parentForTypeSchema) {
	List<JsonSchemaTypeInfo> list = new ArrayList<>();
	list.add(typeInfo);
	createTypeDefinition(list, parentForTypeSchema);
    }

    /**
     * Create a type definition (in JSON Schema) from the given list of (choices of)
     * JSON Schema types, and add it to the given parent schema object. Pure simple
     * types will be added using a single "type" key, simple types with specific
     * format as well as JSON Schema references will be added to a "oneOf". Note
     * that a (set of) pure simple type(s) would be added to the "oneOf" as well (if
     * the type list contains types that are not pure simple types).
     * 
     * @param typeInfos           List of JSON Schema type information objects,
     *                            which represent the allowed choices for the type
     *                            definition
     * @param parentForTypeSchema The JSON Schema definition to which the type
     *                            schema definition shall be added
     */
    private void createTypeDefinition(List<JsonSchemaTypeInfo> typeInfos, JsonSchema parentForTypeSchema) {

	List<JsonSchemaTypeInfo> simpleTypeOptions = typeInfos.stream()
		.filter(opt -> opt.isSimpleType() && !opt.hasFormat())
		.sorted((o1, o2) -> o1.getSimpleType().getName().compareTo(o2.getSimpleType().getName()))
		.collect(Collectors.toList());

	JsonSchema simpleTypeSchema = new JsonSchema();

	// identify other type options
	List<JsonSchemaTypeInfo> otherTypeOptions = typeInfos.stream()
		.filter(opt -> !opt.isSimpleType() || opt.hasFormat()).collect(Collectors.toList());

	if (jsonSchemaVersion == JsonSchemaVersion.OPENAPI_30 && !simpleTypeOptions.isEmpty()) {

	    Optional<JsonSchemaTypeInfo> nullTypeInfoOpt = simpleTypeOptions.stream()
		    .filter(opt -> opt.getSimpleType() == JsonSchemaType.NULL).findFirst();

	    if (nullTypeInfoOpt.isPresent()) {
		simpleTypeOptions.remove(nullTypeInfoOpt.get());
		if (otherTypeOptions.stream().anyMatch(opt -> opt.getRef() != null)) {
		    result.addWarning(this, 118);
		} else {
		    parentForTypeSchema.nullable(true);
		}
	    }

	    if (simpleTypeOptions.size() > 1) {
		otherTypeOptions.addAll(simpleTypeOptions);
		simpleTypeOptions = new ArrayList<JsonSchemaTypeInfo>();
	    } else {
		simpleTypeSchema.type(
			simpleTypeOptions.stream().map(sto -> sto.getSimpleType()).toArray(JsonSchemaType[]::new));
	    }

	} else {

	    simpleTypeSchema
		    .type(simpleTypeOptions.stream().map(sto -> sto.getSimpleType()).toArray(JsonSchemaType[]::new));
	}

	List<JsonSchema> otherTypeSchemas = new ArrayList<>();
	for (JsonSchemaTypeInfo otherTypeOption : otherTypeOptions) {
	    JsonSchema otherTypeSchema = new JsonSchema();
	    if (otherTypeOption.isReference()) {
		otherTypeSchema.ref(otherTypeOption.getRef());
	    } else {
		otherTypeSchema.type(otherTypeOption.getSimpleType());
		if (otherTypeOption.hasFormat()) {
		    otherTypeSchema.format(otherTypeOption.getFormat());
		}
	    }
	    otherTypeSchemas.add(otherTypeSchema);
	}

	if (!simpleTypeOptions.isEmpty() && !otherTypeOptions.isEmpty()) {

	    /*
	     * TODO - is "oneOf" ok here? What if two schemas match for an instance, then
	     * this would return false. An alternative could be to use an if-then-else for
	     * actual JSON object definitions, using the entityType as condition. Or use
	     * anyOf, but that could lead to an incomplete validation.
	     */

	    // create a "oneOf" with everything else and maybe the simple type def
	    parentForTypeSchema.oneOf(simpleTypeSchema);
	    parentForTypeSchema.oneOf(otherTypeSchemas.toArray(new JsonSchema[otherTypeSchemas.size()]));

	} else if (simpleTypeOptions.isEmpty() && !otherTypeOptions.isEmpty()) {

	    if (otherTypeOptions.size() > 1) {
		parentForTypeSchema.oneOf(otherTypeSchemas.toArray(new JsonSchema[otherTypeSchemas.size()]));
	    } else {
		parentForTypeSchema.addAll(otherTypeSchemas.get(0));
	    }

	} else if (!simpleTypeOptions.isEmpty() && otherTypeOptions.isEmpty()) {
	    parentForTypeSchema.addAll(simpleTypeSchema);
	} else {
	    // both simpleTypeOptions and otherTypeOptions are empty
	    // possible if value type of a property was not found (error would be logged)
	}
    }

    /**
     * @param ci           supertype for which to look up the JSON Schema type
     * @param encodingRule encoding rule that applies to the subtype of the
     *                     supertype, which is being encoded
     * @return an {@link Optional} with the JSON Schema type definition for the
     *         supertype; can be empty if no map entry is defined for the supertype,
     *         or if the supertype is not part of the schemas selected for
     *         processing
     */
    private Optional<JsonSchemaTypeInfo> identifyJsonSchemaTypeForSupertype(ClassInfo ci, String encodingRule) {

	JsonSchemaTypeInfo jsTypeInfo = new JsonSchemaTypeInfo();

	String typeName = ci.name();

	ProcessMapEntry pme = mapEntryParamInfos.getMapEntry(typeName, encodingRule);

	if (pme != null) {

	    // check if the target type is one of the simple types defined by JSON Schema
	    Optional<JsonSchemaType> simpleType = JsonSchemaType.fromString(pme.getTargetType());

	    if (simpleType.isPresent()) {

		jsTypeInfo.setSimpleType(simpleType.get());

		if (mapEntryParamInfos.hasCharacteristic(typeName, encodingRule, JsonSchemaConstants.ME_PARAM_FORMATTED,
			JsonSchemaConstants.ME_PARAM_FORMATTED_CHAR_FORMAT)) {
		    jsTypeInfo.setFormat(mapEntryParamInfos.getCharacteristic(typeName, encodingRule,
			    JsonSchemaConstants.ME_PARAM_FORMATTED,
			    JsonSchemaConstants.ME_PARAM_FORMATTED_CHAR_FORMAT));
		}

	    } else {

		/*
		 * since the target type is not a simple type, it must be a reference to a JSON
		 * Schema
		 */
		jsTypeInfo.setRef(pme.getTargetType());
	    }

	    // check if the type is a geometry type
	    if (mapEntryParamInfos.hasParameter(pme, JsonSchemaConstants.ME_PARAM_GEOMETRY)) {
		jsTypeInfo.setGeometry(true);
	    }

	} else {

	    // is the supertype contained in the schemas selected for processing?

	    if (!model.isInSelectedSchemas(ci)) {

		// The supertype is not contained in the schemas selected for processing
		MessageContext mc = result.addError(this, 104, typeName, encodingRule);
		if (mc != null) {
		    mc.addDetail(this, 0, ci.fullName());
		}

		jsTypeInfo = null;

	    } else {

		Optional<JsonSchemaDocument> jsdopt = this.jsonSchemaTarget.jsonSchemaDocument(ci);

		if (jsdopt.isEmpty()) {

		    // only explanation is that the supertype is not encoded
		    MessageContext mc = result.addError(this, 105, typeName, encodingRule);
		    if (mc != null) {
			mc.addDetail(this, 0, ci.fullName());
		    }

		    jsTypeInfo = null;

		} else {

		    JsonSchemaDocument jsd = jsdopt.get();

		    String schemaId = jsd.getSchemaId();

		    if (jsonSchemaVersion != JsonSchemaVersion.OPENAPI_30
			    && ci.matches(JsonSchemaConstants.RULE_CLS_NAME_AS_ANCHOR)) {

			// the encoding of the supertype contains an anchor - use it
			jsTypeInfo.setRef(schemaId + "#" + typeName);

		    } else {

			// use a JSON Pointer to reference the definition of the supertype
			if (jsd.getSchemaVersion() == JsonSchemaVersion.DRAFT_2019_09) {
			    jsTypeInfo.setRef(schemaId + "#/$defs/" + typeName);
			} else {
			    jsTypeInfo.setRef(schemaId + "#/definitions/" + typeName);
			}
		    }
		}
	    }
	}

	return Optional.ofNullable(jsTypeInfo);
    }

    private Optional<JsonSchemaTypeInfo> identifyJsonSchemaType(PropertyInfo pi) {
	return identifyJsonSchemaType(pi.typeInfo().name, pi.typeInfo().id,
		pi.encodingRule(JsonSchemaConstants.PLATFORM));
    }

    private Optional<JsonSchemaTypeInfo> identifyJsonSchemaType(String typeName, String typeId, String encodingRule) {

	JsonSchemaTypeInfo jsTypeInfo = new JsonSchemaTypeInfo();

	ProcessMapEntry pme = mapEntryParamInfos.getMapEntry(typeName, encodingRule);

	if (pme != null) {

	    jsTypeInfo = jsonSchemaTarget.identifyJsonSchemaType(pme, typeName, encodingRule);

	} else {

	    // is the type contained in the schemas selected for processing?
	    ClassInfo valueType = null;
	    if (StringUtils.isNotBlank(typeId)) {
		valueType = model.classById(typeId);
	    }
	    if (valueType == null && StringUtils.isNotBlank(typeName)) {
		valueType = model.classByName(typeName);
	    }

	    if (valueType == null) {

		// The value type was not found in the model
		result.addWarning(this, 114, typeName);

		jsTypeInfo = null;

	    } else if (!JsonSchemaTarget.isEncoded(valueType)) {

		jsTypeInfo = null;

	    } else if (!model.isInSelectedSchemas(valueType)) {

		// The value type is not contained in the schemas selected for processing
		result.addWarning(this, 115, typeName);

		jsTypeInfo = null;

	    } else {

		Optional<JsonSchemaDocument> jsdopt = this.jsonSchemaTarget.jsonSchemaDocument(valueType);

		if (jsdopt.isEmpty()) {

		    // TODO - since we now check up front if the value type is not encoded, we
		    // should update the check here
		    // only explanation is that the value type is not encoded
		    result.addWarning(this, 116, typeName);

		    jsTypeInfo = null;

		} else {

		    JsonSchemaDocument jsd = jsdopt.get();

		    String schemaId = jsd.getSchemaId();

		    if (jsonSchemaVersion != JsonSchemaVersion.OPENAPI_30
			    && valueType.matches(JsonSchemaConstants.RULE_CLS_NAME_AS_ANCHOR)) {

			// the encoding of the value type contains an anchor - use it
			jsTypeInfo.setRef(schemaId + "#" + typeName);

		    } else {

			// use a JSON Pointer to reference the definition of the value type
			if (jsd.getSchemaVersion() == JsonSchemaVersion.DRAFT_2019_09) {
			    jsTypeInfo.setRef(schemaId + "#/$defs/" + typeName);
			} else {
			    jsTypeInfo.setRef(schemaId + "#/definitions/" + typeName);
			}
		    }
		}
	    }
	}

	return Optional.ofNullable(jsTypeInfo);
    }

    public void write() {

	JsonSerializationContext context = new JsonSerializationContext();

	JsonValue jValue = this.rootSchema.toJson(context);

	JsonElement gValue = jValue.toGson();

	GsonBuilder gsonBuilder = new GsonBuilder().serializeNulls();
	if (jsonSchemaTarget.prettyPrinting()) {
	    gsonBuilder.setPrettyPrinting();
	}

	Gson gson = gsonBuilder.create();

	String jsonstring = gson.toJson(gValue);

	try (BufferedWriter writer = new BufferedWriter(
		new OutputStreamWriter(new FileOutputStream(jsonSchemaOutputFile), "UTF-8"))) {

	    writer.write(jsonstring);

	    result.addResult(jsonSchemaTarget.getTargetName(), jsonSchemaOutputFile.getParent(),
		    jsonSchemaOutputFile.getName(), null);

	} catch (Exception e) {
	    result.addError(this, 100, jsonSchemaOutputFile.getAbsolutePath(), e.getMessage());
	    e.printStackTrace();
	}

    }

    @Override
    public String message(int mnr) {

	switch (mnr) {

	case 0:
	    return "Context: class '$1$'";
	case 1:
	    return "Context: property '$1$'";
	case 9:
	    return "??Property '$1$' of class '$2$' is not encoded.";

	case 100:
	    return "Exception occurred while writing JSON Schema to file: $1$. Exception message is: $2$.";
	case 101:
	    return "Restricting facet 'maxLength' of basic type '$1$' defined by tagged value 'length' (or 'maxLength') could not be parsed as an integer. Found value: $2$. The facet will be ignored.";
	case 102:
	    return "Restricting facet 'maxLength' of basic type '$1$' defined by tagged value 'length' (or 'maxLength') has a negative integer value ($2$), which is not allowed for the facet. The facet will be ignored.";
	case 103:
	    return "Restricting facet '$4$' of basic type '$1$' defined by tagged value '$3$' could not be parsed as a double. Found value: $2$. The facet will be ignored.";
	case 104:
	    return "??JSON Schema type for supertype '$1$' could not be identified for encoding rule '$2$'. No map entry is defined for the type, and the type is not contained in the schemas selected for processing. Generalization relationships to this type will be ignored.";
	case 105:
	    return "??JSON Schema type for supertype '$1$' could not be identified for encoding rule '$2$'. No map entry is defined for the type, and the type is not encoded. Generalization relationships to this type will be ignored.";
	case 106:
	    return "Initial value '$1$' of property '$2$' is not an integer, but the JSON Schema type identified for the value type of the property is 'integer'. The initial value will not be encoded as \"default\".";
	case 107:
	    return "Initial value '$1$' of property '$2$' is not a number (to be exact: not a double), but the JSON Schema type identified for the value type of the property is 'number'. The initial value will not be encoded as \"default\".";
	case 108:
	    return "Supertype '$1$' of type '$2$' has JSON Schema type '$3$', which is a simple type. That is not allowed. The generalization relationship from '$2$' to '$1$' will be ignored.";
	case 109:
	    return "(Initial) value '$1$' of enum '$2$' is not an integer, but the JSON Schema type identified for enumeration '$3$' is 'integer'. The enum will not be encoded.";
	case 110:
	    return "(Initial) value '$1$' of enum '$2$' is not a number (to be exact: not a double), but the JSON Schema type identified for enumeration '$3$' is 'number'. The enum will not be encoded.";
	case 111:
	    return JsonSchemaConstants.RULE_CLS_GENERALIZATION
		    + " does not apply to type '$1$'. The type has at least one supertype. No generalization relationship will be encoded for '$1$'.";
	case 112:
	    return "Class '$1$' has multiple default geometry properties ($2$). None of them will be encoded as 'geometry' member. They will be encoded as usual properties.";
	case 113:
	    return "Property '$1$' of class '$2$' is an <<identifier>> property with max multiplicity greater than 1. Such a property should have a max multiplicity of exactly 1.";
	case 114:
	    return "??JSON Schema definition for type '$1$' could not be identified. No map entry is defined for the type, and the type was not found in the model. No type restriction is created for properties with this type as value type.";
	case 115:
	    return "??JSON Schema definition for type '$1$' could not be identified. No map entry is defined for the type, and the type is not contained in the schemas selected for processing. No type restriction is created for properties with this type as value type.";
	case 116:
	    return "??JSON Schema definition for type '$1$' could not be identified. No map entry is defined for the type, and the type is not encoded. No type restriction is created for properties with this type as value type.";
	case 117:
	    return "Property '$1$' of type '$2$' has been identified as default geometry of the type. However, the maximum multiplicity of that property is greater than 1. The property is mapped to the \"geometry\" member, which can only have a single value. The multiplicity of the property will therefore be ignored.";
	case 118:
	    return "??The schema contains or restricts voidable properties whose value type is defined using the '$ref' keyword. At the same time, the json schema version is set to OpenAPI30, which does not support nullable in combination with $ref. Voidable will therefore be ignored for these cases.";

	default:
	    return "(" + JsonSchemaDocument.class.getName() + ") Unknown message with number: " + mnr;
	}
    }

}
