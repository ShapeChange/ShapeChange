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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
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
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.FormatKeyword;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.JsonSchema;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.JsonSchemaType;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.JsonSchemaVersion;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.JsonSerializationContext;
import de.interactive_instruments.ShapeChange.Util.ValueTypeOptions;

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
    protected String rootSchemaId;
    protected File jsonSchemaOutputFile;
    protected MapEntryParamInfos mapEntryParamInfos;
    protected JsonSchemaVersion jsonSchemaVersion;

    protected SortedMap<String, ClassInfo> classesByName = new TreeMap<>();
    protected Map<ClassInfo, JsonSchemaTypeInfo> basicTypeInfoByClass = new HashMap<>();

    protected JsonSchema rootSchema = new JsonSchema();

    public JsonSchemaDocument(PackageInfo representedPackage, Model model, Options options, ShapeChangeResult result,
	    JsonSchemaTarget jsonSchemaTarget, String rootSchemaId,
	    File jsonSchemaOutputFile, MapEntryParamInfos mapEntryParamInfos) {

	this.representedPackage = representedPackage;
	this.options = options;
	this.result = result;
	this.model = model;
	this.jsonSchemaTarget = jsonSchemaTarget;
	this.rootSchemaId = rootSchemaId;
	this.jsonSchemaOutputFile = jsonSchemaOutputFile;
	this.mapEntryParamInfos = mapEntryParamInfos;

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
    
    public boolean isBasicType(ClassInfo ci) {
	return basicTypeInfoByClass.containsKey(ci);
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

	    // add schema definition to root schema
	    /*
	     * Also compute the URL to the schema definition, ignoring the anchor capability
	     * (because it would not be supported in an OpenAPI 3.0 schema).
	     */
	    String jsDefinitionReference;
	    if (jsonSchemaVersion == JsonSchemaVersion.DRAFT_2019_09) {
		rootSchema.def(ci.name(), js);
		jsDefinitionReference = rootSchemaId + "#/$defs/" + ci.name();
	    } else {
		rootSchema.definition(ci.name(), js);
		jsDefinitionReference = rootSchemaId + "#/definitions/" + ci.name();
	    }

	    // create map entry
	    PackageInfo schemaPi = ci.pkg().rootPackage();
	    String rule = "*";
	    ProcessMapEntry pme;

	    /*
	     * Determine if the map entry must have a parameter - e.g. to convey the entity
	     * type member path.
	     */
	    if (JsonSchemaTarget.entityTypeMemberPathByCi.containsKey(ci)) {
		String entityTypeMemberPath = JsonSchemaTarget.entityTypeMemberPathByCi.get(ci);
		String paramValue = JsonSchemaConstants.ME_PARAM_ENCODING_INFOS + "{"
			+ JsonSchemaConstants.ME_PARAM_ENCODING_INFOS_CHAR_ENTITY_TYPE_MEMBER_PATH + "="
			+ entityTypeMemberPath + "}";
		pme = new ProcessMapEntry(ci.name(), rule, jsDefinitionReference, paramValue);
	    } else {
		pme = new ProcessMapEntry(ci.name(), rule, jsDefinitionReference);
	    }

	    jsonSchemaTarget.addMapEntry(schemaPi, pme);
	}
    }

    /**
     * @param jsd The JSON Schema document in which the class is encoded
     * @param ci  the class for which to get the reference of its JSON Schema
     *            definition
     * @return the reference to the JSON Schema definition of the given class
     */
    private String jsonSchemaDefinitionReference(JsonSchemaDocument jsd, ClassInfo ci) {

	String schemaId = jsd.getSchemaId();

	if (jsd.getSchemaVersion() != JsonSchemaVersion.OPENAPI_30
		&& ci.matches(JsonSchemaConstants.RULE_CLS_NAME_AS_ANCHOR)) {

	    // the encoding of the class contains an anchor - use it
	    return schemaId + "#" + ci.name();

	} else {

	    // use a JSON Pointer to reference the definition of the class
	    if (jsd.getSchemaVersion() == JsonSchemaVersion.DRAFT_2019_09) {
		return schemaId + "#/$defs/" + ci.name();
	    } else {
		return schemaId + "#/definitions/" + ci.name();
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
	    if (StringUtils.isBlank(length)) {
		length = ci.taggedValue("maxLength");
	    }
	    if (StringUtils.isBlank(length)) {
		length = ci.taggedValue("size");
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

	    String pattern = ci.taggedValue("jsonPattern");

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

	    JsonSchemaTypeInfo jsti = identifyLiteralEncodingType(ci);
	    createTypeDefinition(jsti, jsClass);
	}

	return jsClass;
    }

    private JsonSchemaTypeInfo identifyLiteralEncodingType(ClassInfo ci) {

	String literalEncodingType = "CharacterString";
	if (StringUtils.isNotBlank(ci.taggedValue("literalEncodingType"))) {
	    literalEncodingType = ci.taggedValue("literalEncodingType").trim();
	}

	String jsonEncodingRuleForCi = ci.encodingRule(JsonSchemaConstants.PLATFORM);

	Optional<JsonSchemaTypeInfo> jstiOpt = identifyJsonSchemaType(literalEncodingType, null, jsonEncodingRuleForCi);

	if (jstiOpt.isPresent()) {
	    return jstiOpt.get();
	} else {
	    MessageContext mc = result.addError(this, 119, ci.name(), literalEncodingType);
	    if (mc != null) {
		result.addInfo(this, 0, ci.fullNameInSchema());
	    }
	    JsonSchemaTypeInfo jsti = new JsonSchemaTypeInfo();
	    jsti.setSimpleType(JsonSchemaType.STRING);
	    return jsti;
	}
    }

    private JsonSchema jsonSchemaForEnumeration(ClassInfo ci) {

	JsonSchema jsClass = new JsonSchema();

	addCommonSchemaMembers(jsClass, ci);

	// identify and set the JSON Schema type as which the enumeration is implemented
	JsonSchemaTypeInfo jsti = identifyLiteralEncodingType(ci);
	createTypeDefinition(jsti, jsClass);

	JsonSchemaType enumerationJsType = jsti.getSimpleType();

	if (enumerationJsType == null) {
	    MessageContext mc = result.addWarning(this, 120, ci.name(), jsti.getRef());
	    if (mc != null) {
		mc.addDetail(this, 0, ci.fullNameInSchema());
	    }
	    enumerationJsType = JsonSchemaType.STRING;
	}

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
	    } else if (enumerationJsType == JsonSchemaType.BOOLEAN) {

		String stringValue = StringUtils.isNotBlank(pi.initialValue()) ? pi.initialValue() : pi.name();

		if ("true".equalsIgnoreCase(stringValue.trim()) || "1".equals(stringValue.trim())) {
		    jsClass.enum_(new JsonBoolean(true));
		} else {
		    jsClass.enum_(new JsonBoolean(false));
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

	createTypeDefinition(typeInfos, null, jsClass);

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

	if (ci.category() == Options.FEATURE || ci.category() == Options.OBJECT || ci.category() == Options.DATATYPE
		|| ci.category() == Options.MIXIN) {

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
			MessageContext mc = result.addWarning(this, 117, directGeometryPi.name(), ci.name());
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

	/*
	 * Check if an entity type member is available in the encodings of ci's
	 * supertypes. If so, simply keep track of it (for map entry creation, and
	 * potentially also type specific checks). Otherwise, check if such a member
	 * shall be added to the encoding of ci. If so, do it, and also keep track of
	 * the member path.
	 */
	String entityTypeMemberPathFromSupertypes = identifyEntityTypeMemberPathFromSupertypes(ci);
	String entityTypeMemberPath = null;

	if (entityTypeMemberPathFromSupertypes == null) {

	    if (ci.matches(JsonSchemaConstants.RULE_CLS_NAME_AS_ENTITYTYPE) && (ci.category() != Options.UNION
		    || ci.matches(JsonSchemaConstants.RULE_CLS_NAME_AS_ENTITYTYPE_UNION))) {

		String entityTypeMemberName = jsonSchemaTarget.getEntityTypeName();
		jsProperties.property(entityTypeMemberName, new JsonSchema().type(JsonSchemaType.STRING))
			.required(entityTypeMemberName);

		entityTypeMemberPath = (ci.matches(JsonSchemaConstants.RULE_CLS_NESTED_PROPERTIES) ? "properties/" : "")
			+ entityTypeMemberName;
	    }
	} else {
	    entityTypeMemberPath = entityTypeMemberPathFromSupertypes;
	}

	if (entityTypeMemberPath != null) {
	    /*
	     * keep track of new entity type member path for creation of map entry for ci
	     */
	    JsonSchemaTarget.entityTypeMemberPathByCi.put(ci, entityTypeMemberPath);
	}

	if ((ci.category() == Options.FEATURE || ci.category() == Options.OBJECT)
		&& ci.matches(JsonSchemaConstants.RULE_CLS_IDENTIFIER_FOR_TYPE_WITH_IDENTITY)
		&& !(ci.matches(JsonSchemaConstants.RULE_CLS_IGNORE_IDENTIFIER)
			|| ci.matches(JsonSchemaConstants.RULE_CLS_IDENTIFIER_STEREOTYPE))) {

	    // check if supertype matches
	    boolean supertypeMatch = false;
	    for (ClassInfo supertype : ci.supertypeClasses()) {
		if ((supertype.category() == Options.FEATURE || supertype.category() == Options.OBJECT)
			&& supertype.matches(JsonSchemaConstants.RULE_CLS_IDENTIFIER_FOR_TYPE_WITH_IDENTITY)
			&& !(supertype.matches(JsonSchemaConstants.RULE_CLS_IGNORE_IDENTIFIER)
				|| supertype.matches(JsonSchemaConstants.RULE_CLS_IDENTIFIER_STEREOTYPE))) {
		    supertypeMatch = true;
		    break;
		}
	    }

	    if (!supertypeMatch) {
		JsonSchema objectIdentifierTypeSchema = new JsonSchema();
		createTypeDefinition(jsonSchemaTarget.objectIdentifierType(), objectIdentifierTypeSchema);
		jsProperties.property(jsonSchemaTarget.objectIdentifierName(), objectIdentifierTypeSchema);

		if (jsonSchemaTarget.objectIdentifierRequired()) {
		    jsProperties.required(jsonSchemaTarget.objectIdentifierName());
		}
	    }
	}

	String valueTypeOptionsTV = ci.taggedValue("valueTypeOptions");
	ValueTypeOptions vto;
	if (StringUtils.isNotBlank(valueTypeOptionsTV) && ci.matches(JsonSchemaConstants.RULE_CLS_VALUE_TYPE_OPTIONS)) {
	    vto = new ValueTypeOptions(valueTypeOptionsTV);
	} else {
	    vto = new ValueTypeOptions();
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

	    jsProperties.property(pi.name(), jsonSchema(pi, vto));

	    vto.remove(pi.name());

	    if (ci.category() != Options.UNION) {
		// if the property is not optional, add it to the required properties
		if (pi.cardinality().minOccurs > 0) {
		    jsProperties.required(pi.name());
		}
	    }
	}

	// create property definitions for valueTypeOptions that target properties from
	// supertypes
	for (String supertypePropertyName : vto.getPropertiesWithValueTypeOptions()) {

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

	    SortedSet<String> supertypePropertyTypeOptions = vto.getValueTypeOptions(supertypePropertyName);

	    List<JsonSchemaTypeInfo> supertypePropertyTypeInfos = new ArrayList<>();

	    // Create a JSON Schema to restrict the type of the supertype property
	    JsonSchema supertypePropertyTypeRestrictionSchema = new JsonSchema();

	    boolean isAssociationClassRole = vto.isAssociationClassRole(supertypePropertyName);
	    SortedMap<String, JsonSchemaTypeInfo> valueTypeOptionsByTypeName = new TreeMap<>();
	    for (String supertypePropertyTypeOption : supertypePropertyTypeOptions) {
		Optional<JsonSchemaTypeInfo> jstdOpt = identifyJsonSchemaType(supertypePropertyTypeOption, null,
			ci.encodingRule(JsonSchemaConstants.PLATFORM));
		if (jstdOpt.isPresent()) {
		    valueTypeOptionsByTypeName.put(supertypePropertyTypeOption, jstdOpt.get());
		}
	    }

	    // Take into account inline/inlineOrByReference (byReference has already been
	    // ruled out before)
	    boolean byReferenceAllowedForSupertypeProperty = false;

	    if ((supertypePi.categoryOfValue() == Options.FEATURE || supertypePi.categoryOfValue() == Options.OBJECT)
		    && !"inline".equalsIgnoreCase(inlineOrByReference(supertypePi))) {

		byReferenceAllowedForSupertypeProperty = true;
		supertypePropertyTypeInfos.add(createJsonSchemaTypeInfoForReference());
	    }

	    // Now add the actual type restrictions

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
	    if (valueTypeOptionsByTypeName.isEmpty()) {
		createTypeDefinition(supertypePropertyTypeInfos, null, parentForTypeSchema);
	    } else {
		if (isAssociationClassRole) {
		    createTypeDefinitionWithValueTypeOptionsForAssociationClassRole(valueTypeOptionsByTypeName,
			    supertypePropertyTypeInfos, identifyJsonSchemaType(supertypePi), supertypePi.name(),
			    byReferenceAllowedForSupertypeProperty, parentForTypeSchema);
		} else {
		    createTypeDefinition(valueTypeOptionsByTypeName, supertypePropertyTypeInfos, parentForTypeSchema);
		}
	    }
	    /*
	     * Finally, add another member to the "properties", to represent the type
	     * restriction of the supertype property
	     */
	    jsProperties.property(supertypePropertyName, supertypePropertyTypeRestrictionSchema);
	}

	if (ci.category() == Options.UNION) {

	    if (ci.matches(JsonSchemaConstants.RULE_CLS_UNION_PROPERTY_COUNT)) {

		jsClassContents.additionalProperties(JsonSchema.FALSE);

		int exactPropCount = 1;

		if (ci.matches(JsonSchemaConstants.RULE_CLS_NAME_AS_ENTITYTYPE)
			&& ci.matches(JsonSchemaConstants.RULE_CLS_NAME_AS_ENTITYTYPE_UNION)) {
		    exactPropCount = 2;
		}

		jsClassContents.minProperties(exactPropCount);
		jsClassContents.maxProperties(exactPropCount);
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

    /**
     * @param ci the class for which to identify the entity type member path
     * @return the path of the JSON member that is used to encode the type of the
     *         class; can be <code>null</code> if no such member is available
     */
    private String identifyEntityTypeMemberPath(ClassInfo ci) {

	if (JsonSchemaTarget.entityTypeMemberPathByCi.containsKey(ci)) {
	    return JsonSchemaTarget.entityTypeMemberPathByCi.get(ci);
	} else {

	    // check supertypes
	    String entityTypeMemberPathFromSupertypes = identifyEntityTypeMemberPathFromSupertypes(ci);
	    if (entityTypeMemberPathFromSupertypes != null) {
		return entityTypeMemberPathFromSupertypes;
	    } else {

		// check if the entity type member is added to the type itself
		if (ci.matches(JsonSchemaConstants.RULE_CLS_NAME_AS_ENTITYTYPE) && (ci.category() != Options.UNION
			|| ci.matches(JsonSchemaConstants.RULE_CLS_NAME_AS_ENTITYTYPE_UNION))) {
		    return (ci.matches(JsonSchemaConstants.RULE_CLS_NESTED_PROPERTIES) ? "properties/" : "")
			    + jsonSchemaTarget.getEntityTypeName();
		} else {
		    return null;
		}
	    }
	}
    }

    /**
     * @param ci the class for which to search an entity type member in its
     *           supertype encodings
     * @return the path of the JSON member available in one of the supertype
     *         encodings of ci, that is used to encode the type of the class; can be
     *         <code>null</code> if no such member is available in the supertypes
     *         (especially, of course, if ci does not have any superclass)
     */
    private String identifyEntityTypeMemberPathFromSupertypes(ClassInfo ci) {

	/*
	 * If an entity type member path is defined in the map entry of a supertype, use
	 * that path. Otherwise, if an entity type member path can be determined for a
	 * supertype of one of the supertypes, use that path. If that also did not
	 * succeed, check if the supertype itself would receive an entity type member,
	 * and if so, use the path of that member.
	 */
	String resultFromMapEntries = null;
	String resultFromSuperSupertypes = null;
	String resultFromSupertypes = null;

	for (ClassInfo supertype : ci.supertypeClasses()) {

	    // check for map entry first
	    Optional<ProcessMapEntry> supertypePmeOpt = jsonSchemaTarget.mapEntry(supertype);
	    if (supertypePmeOpt.isPresent()) {
		/*
		 * So a map entry is defined for the supertype; it is the definitive source of
		 * information for the entity type member path.
		 */
		if (mapEntryParamInfos.hasCharacteristic(supertype.name(),
			supertype.encodingRule(JsonSchemaConstants.PLATFORM),
			JsonSchemaConstants.ME_PARAM_ENCODING_INFOS,
			JsonSchemaConstants.ME_PARAM_ENCODING_INFOS_CHAR_ENTITY_TYPE_MEMBER_PATH)) {

		    resultFromMapEntries = mapEntryParamInfos.getCharacteristic(supertype.name(),
			    supertype.encodingRule(JsonSchemaConstants.PLATFORM),
			    JsonSchemaConstants.ME_PARAM_ENCODING_INFOS,
			    JsonSchemaConstants.ME_PARAM_ENCODING_INFOS_CHAR_ENTITY_TYPE_MEMBER_PATH);

		    /*
		     * since member path from map entry has highest priority, we can skip the
		     * supertype checks now
		     */
		    break;

		}
	    } else {

		String entityMemberPathFromSupertypesOfSupertype = identifyEntityTypeMemberPathFromSupertypes(
			supertype);
		if (entityMemberPathFromSupertypesOfSupertype != null) {
		    resultFromSuperSupertypes = entityMemberPathFromSupertypesOfSupertype;
		} else {

		    if (supertype.matches(JsonSchemaConstants.RULE_CLS_NAME_AS_ENTITYTYPE)
			    && (supertype.category() != Options.UNION
				    || supertype.matches(JsonSchemaConstants.RULE_CLS_NAME_AS_ENTITYTYPE_UNION))) {

			resultFromSupertypes = (supertype.matches(JsonSchemaConstants.RULE_CLS_NESTED_PROPERTIES)
				? "properties/"
				: "") + jsonSchemaTarget.getEntityTypeName();
		    }
		}
	    }
	}

	if (resultFromMapEntries != null) {
	    return resultFromMapEntries;
	} else if (resultFromSuperSupertypes != null) {
	    return resultFromSuperSupertypes;
	} else if (resultFromSupertypes != null) {
	    return resultFromSupertypes;
	} else {
	    return null;
	}
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

		if (!supertypes.isEmpty()) {
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
	    byRefInfo.setKeyword(new FormatKeyword("uri"));
	}

	return byRefInfo;
    }

    /**
     * @param pi               the property for which to generate a JSON Schema
     * @param valueTypeOptions can be <code>null</code>, and not contain any options
     *                         for the property
     * @return JSON Schema that defines the property
     */
    private JsonSchema jsonSchema(PropertyInfo pi, ValueTypeOptions valueTypeOptions) {

	// convert value type, voidable, and inlineOrByReference

	// First, identify the JSON Schema type infos for all allowed types
	List<JsonSchemaTypeInfo> typeOptions = new ArrayList<>();
	JsonSchema associationClassRoleWithTypeValueOptionsSchema = null;

	Optional<JsonSchemaTypeInfo> typeInfoOpt = identifyJsonSchemaType(pi);

	boolean isAssociationClassRole = valueTypeOptions.isAssociationClassRole(pi.name());
	SortedMap<String, JsonSchemaTypeInfo> valueTypeOptionsByTypeName = new TreeMap<>();
	boolean byReferenceAllowed = false;

	if (typeInfoOpt.isPresent()) {

	    JsonSchemaTypeInfo typeInfo = typeInfoOpt.get();

	    boolean byReferenceOnly = false;

	    if ((pi.categoryOfValue() == Options.FEATURE || (pi.categoryOfValue() == Options.OBJECT && !valueTypeIsBasicType(pi)))
		    && !typeInfo.isSimpleType()) {

		boolean addByReferenceOption = false;

		if ("byReference".equalsIgnoreCase(inlineOrByReference(pi))) {
		    byReferenceOnly = true;
		    addByReferenceOption = true;
		} else if (!"inline".equalsIgnoreCase(inlineOrByReference(pi))) {
		    addByReferenceOption = true;
		    byReferenceAllowed = true;
		}

		if (addByReferenceOption) {
		    typeOptions.add(createJsonSchemaTypeInfoForReference());
		}
	    }

	    if (!byReferenceOnly) {

		if (valueTypeOptions != null && valueTypeOptions.hasValueTypeOptions(pi.name())) {

		    SortedSet<String> options = valueTypeOptions.getValueTypeOptions(pi.name());

		    for (String piValueTypeOption : options) {

			Optional<JsonSchemaTypeInfo> jstdOpt = identifyJsonSchemaType(piValueTypeOption, null,
				pi.encodingRule(JsonSchemaConstants.PLATFORM));

			if (jstdOpt.isPresent()) {
			    valueTypeOptionsByTypeName.put(piValueTypeOption, jstdOpt.get());
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
	if (pi.cardinality().maxOccurs > 1) {

	    parentForTypeSchema.type(JsonSchemaType.ARRAY);

	    if (pi.cardinality().minOccurs > 0) {
		parentForTypeSchema.minItems(pi.cardinality().minOccurs);
	    }

	    if (pi.cardinality().maxOccurs < Integer.MAX_VALUE) {
		parentForTypeSchema.maxItems(pi.cardinality().maxOccurs);
	    }

	    JsonSchema itemsSchema = null;
	    if (!typeOptions.isEmpty() || associationClassRoleWithTypeValueOptionsSchema != null) {
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
	if (valueTypeOptionsByTypeName.isEmpty()) {
	    createTypeDefinition(typeOptions, null, parentForTypeSchema);
	} else {
	    if (isAssociationClassRole) {
		createTypeDefinitionWithValueTypeOptionsForAssociationClassRole(valueTypeOptionsByTypeName, typeOptions,
			typeInfoOpt, pi.name(), byReferenceAllowed, parentForTypeSchema);
	    } else {
		createTypeDefinition(valueTypeOptionsByTypeName, typeOptions, parentForTypeSchema);
	    }
	}

	// --- convert initial value
	if (typeInfoOpt.isPresent()) {

	    JsonSchemaTypeInfo typeInfo = typeInfoOpt.get();

	    if (pi.isAttribute() && StringUtils.isNotBlank(pi.initialValue())
		    && pi.matches(JsonSchemaConstants.RULE_PROP_INITIAL_VALUE_AS_DEFAULT)) {
		
		JsonSchemaTypeInfo actualTypeInfo = typeInfo;
		
		if(valueTypeIsBasicType(pi)) {
		    actualTypeInfo = getBasicValueTypeDefinition(pi).get();
		}

		if (typeInfo.isSimpleType() || valueTypeIsBasicType(pi)) {

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

    private boolean valueTypeIsBasicType(PropertyInfo pi) {
	
	ClassInfo tci = model.classByIdOrName(pi.typeInfo());
	
	if(tci == null) {
	    return false;
	} else {
	    Optional<JsonSchemaDocument> jsdOpt = jsonSchemaTarget.jsonSchemaDocument(tci);
	    if(jsdOpt.isEmpty()) {
		return false;
	    } else {
		return jsdOpt.get().hasBasicTypeDefinition(tci);
	    }
	}	
    }
    
    private Optional<JsonSchemaTypeInfo> getBasicValueTypeDefinition(PropertyInfo pi) {
	
	ClassInfo tci = model.classByIdOrName(pi.typeInfo());
	
	JsonSchemaTypeInfo result;
	
	if(tci == null) {
	    result = null;
	} else {
	    Optional<JsonSchemaDocument> jsdOpt = jsonSchemaTarget.jsonSchemaDocument(tci);
	    if(jsdOpt.isEmpty()) {
		result = null;
	    } else {
		result = jsdOpt.get().getBasicTypeDefinition(tci);
	    }
	}	
	
	return Optional.of(result);
    }
    
    public boolean hasBasicTypeDefinition(ClassInfo ci) {
	return this.basicTypeInfoByClass.containsKey(ci);
    }
    
    public JsonSchemaTypeInfo getBasicTypeDefinition(ClassInfo ci) {
	return this.basicTypeInfoByClass.get(ci);
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
	createTypeDefinition(list, null, parentForTypeSchema);
    }

    private void createTypeDefinition(JsonSchemaType[] types, JsonSchema parentForTypeSchema) {
	List<JsonSchemaTypeInfo> list = new ArrayList<>();
	for (JsonSchemaType jst : types) {
	    JsonSchemaTypeInfo jsti = new JsonSchemaTypeInfo();
	    jsti.setSimpleType(jst);
	    list.add(jsti);
	}
	createTypeDefinition(list, null, parentForTypeSchema);
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
    private void createTypeDefinition(List<JsonSchemaTypeInfo> typeInfos, JsonSchema specificTypeSchema,
	    JsonSchema parentForTypeSchema) {

	List<JsonSchemaTypeInfo> simpleTypeOptions = typeInfos.stream()
		.filter(opt -> opt.isSimpleType() && !opt.hasKeywords())
		.sorted((o1, o2) -> o1.getSimpleType().getName().compareTo(o2.getSimpleType().getName()))
		.collect(Collectors.toList());

	JsonSchema simpleTypeSchema = new JsonSchema();

	// identify other type options
	List<JsonSchemaTypeInfo> otherTypeOptions = typeInfos.stream()
		.filter(opt -> !opt.isSimpleType() || opt.hasKeywords()).collect(Collectors.toList());

	if (!simpleTypeOptions.isEmpty()) {

	    if (jsonSchemaVersion == JsonSchemaVersion.OPENAPI_30) {

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
		} else if (!simpleTypeOptions.isEmpty()) {
		    simpleTypeSchema.type(
			    simpleTypeOptions.stream().map(sto -> sto.getSimpleType()).toArray(JsonSchemaType[]::new));
		}

	    } else {

		simpleTypeSchema.type(
			simpleTypeOptions.stream().map(sto -> sto.getSimpleType()).toArray(JsonSchemaType[]::new));
	    }
	}

	List<JsonSchema> otherTypeSchemas = new ArrayList<>();
	for (JsonSchemaTypeInfo otherTypeOption : otherTypeOptions) {
	    JsonSchema otherTypeSchema = new JsonSchema();
	    if (otherTypeOption.isReference()) {
		otherTypeSchema.ref(otherTypeOption.getRef());
	    } else {
		otherTypeSchema.type(otherTypeOption.getSimpleType());
		if (otherTypeOption.hasKeywords()) {
		    otherTypeSchema.addAll(otherTypeOption.getKeywords());
		}
	    }
	    otherTypeSchemas.add(otherTypeSchema);
	}

	if (specificTypeSchema != null && !specificTypeSchema.isEmpty()) {
	    otherTypeSchemas.add(specificTypeSchema);
	}

	if (!simpleTypeSchema.isEmpty() && !otherTypeSchemas.isEmpty()) {

	    /*
	     * TODO - is "oneOf" ok here? What if two schemas match for an instance, then
	     * this would return false. An alternative could be to use an if-then-else for
	     * actual JSON object definitions (see other createTypeDefinition methods on how
	     * to do that), using the entityType as condition. Or use anyOf, but that could
	     * lead to an incomplete validation.
	     */

	    // create a "oneOf" with everything else and maybe the simple type def
	    parentForTypeSchema.oneOf(simpleTypeSchema);
	    parentForTypeSchema.oneOf(otherTypeSchemas.toArray(new JsonSchema[otherTypeSchemas.size()]));

	} else if (simpleTypeSchema.isEmpty() && !otherTypeSchemas.isEmpty()) {

	    if (otherTypeSchemas.size() > 1) {
		parentForTypeSchema.oneOf(otherTypeSchemas.toArray(new JsonSchema[otherTypeSchemas.size()]));
	    } else {
		parentForTypeSchema.addAll(otherTypeSchemas.get(0));
	    }

	} else if (!simpleTypeSchema.isEmpty() && otherTypeSchemas.isEmpty()) {
	    parentForTypeSchema.addAll(simpleTypeSchema);
	} else {
	    // both simpleTypeOptions and otherTypeOptions are empty
	    // possible if value type of a property was not found (error would be logged)
	}
    }

    private void createTypeDefinition(SortedMap<String, JsonSchemaTypeInfo> typeSpecificJsonTypeInfos,
	    List<JsonSchemaTypeInfo> additionalJsonTypes, JsonSchema parentForTypeSchema) {

	List<JsonSchemaTypeInfo> simpleTypeOptions = new ArrayList<>();

	List<JsonSchemaTypeInfo> simpleTypeOptionsFromAdditionalSimpleJsonTypes = additionalJsonTypes.stream()
		.filter(opt -> opt.isSimpleType() && !opt.hasKeywords())
		.sorted((o1, o2) -> o1.getSimpleType().getName().compareTo(o2.getSimpleType().getName()))
		.collect(Collectors.toList());
	simpleTypeOptions.addAll(simpleTypeOptionsFromAdditionalSimpleJsonTypes);

	List<JsonSchemaTypeInfo> simpleTypeOptionsFromTypeSpecificJsonTypeInfos = typeSpecificJsonTypeInfos.values()
		.stream().filter(opt -> opt.isSimpleType() && !opt.hasKeywords())
		.sorted((o1, o2) -> o1.getSimpleType().getName().compareTo(o2.getSimpleType().getName()))
		.collect(Collectors.toList());
	simpleTypeOptions.addAll(simpleTypeOptionsFromTypeSpecificJsonTypeInfos);
	simpleTypeOptions = simpleTypeOptions.stream()
		.sorted((o1, o2) -> o1.getSimpleType().getName().compareTo(o2.getSimpleType().getName()))
		.collect(Collectors.toList());

	JsonSchema simpleTypeSchema = new JsonSchema();

	// identify other type options
	List<JsonSchemaTypeInfo> otherTypeOptions = new ArrayList<>();

	List<JsonSchemaTypeInfo> otherTypeOptionsFromAdditionalSimpleJsonTypes = additionalJsonTypes.stream()
		.filter(opt -> !opt.isSimpleType() || opt.hasKeywords()).collect(Collectors.toList());
	otherTypeOptions.addAll(otherTypeOptionsFromAdditionalSimpleJsonTypes);

	List<JsonSchemaTypeInfo> otherTypeOptionsFromTypeSpecificJsonTypeInfos = typeSpecificJsonTypeInfos.values()
		.stream().filter(opt -> opt.isSimpleType() && opt.hasKeywords()).collect(Collectors.toList());
	otherTypeOptions.addAll(otherTypeOptionsFromTypeSpecificJsonTypeInfos);

	// identify type specific JSON type options that are schema references
	SortedMap<String, String> remainingSpecificTypeOptions = new TreeMap<>();

	for (String typeName : typeSpecificJsonTypeInfos.keySet()) {
	    JsonSchemaTypeInfo jsti = typeSpecificJsonTypeInfos.get(typeName);
	    if (jsti.hasRef()) {
		remainingSpecificTypeOptions.put(typeName, jsti.getRef());
	    }
	}

	if (!simpleTypeOptions.isEmpty()) {

	    if (jsonSchemaVersion == JsonSchemaVersion.OPENAPI_30) {

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
		} else if (!simpleTypeOptions.isEmpty()) {
		    simpleTypeSchema.type(
			    simpleTypeOptions.stream().map(sto -> sto.getSimpleType()).toArray(JsonSchemaType[]::new));
		}

	    } else {

		simpleTypeSchema.type(
			simpleTypeOptions.stream().map(sto -> sto.getSimpleType()).toArray(JsonSchemaType[]::new));
	    }
	}

	List<JsonSchema> otherTypeSchemas = new ArrayList<>();
	for (JsonSchemaTypeInfo otherTypeOption : otherTypeOptions) {
	    JsonSchema otherTypeSchema = new JsonSchema();
	    if (otherTypeOption.isReference()) {
		otherTypeSchema.ref(otherTypeOption.getRef());
	    } else {
		otherTypeSchema.type(otherTypeOption.getSimpleType());
		if (otherTypeOption.hasKeywords()) {
		    otherTypeSchema.addAll(otherTypeOption.getKeywords());
		}
	    }
	    otherTypeSchemas.add(otherTypeSchema);
	}

	JsonSchema typeSpecificSchema = new JsonSchema();
	if (!remainingSpecificTypeOptions.isEmpty()) {
	    JsonSchema parentSchema = typeSpecificSchema;
	    JsonSchema ifSchema, thenSchema, elseSchema;

	    for (String typeName : remainingSpecificTypeOptions.keySet()) {

		String ref = remainingSpecificTypeOptions.get(typeName);

		ifSchema = new JsonSchema();
		// get type specific entity type member name
		String entityTypeMemberPath = null;
		ClassInfo typeCi = model.classByName(typeName);
		if (typeCi != null) {
		    entityTypeMemberPath = identifyEntityTypeMemberPath(typeCi);
		}
		if (StringUtils.isBlank(entityTypeMemberPath)) {
		    entityTypeMemberPath = jsonSchemaTarget.getEntityTypeName();
		    result.addError(this, 122, typeName, entityTypeMemberPath);
		}

		String[] entityTypeMemberPathComponents = entityTypeMemberPath.split("/");
		JsonSchema entityTypeMemberPropertySchema = ifSchema;
		for (int i = 0; i < entityTypeMemberPathComponents.length - 1; i++) {
		    JsonSchema newEntityTypeMemberPropertySchema = new JsonSchema();
		    entityTypeMemberPropertySchema.property(entityTypeMemberPathComponents[i],
			    newEntityTypeMemberPropertySchema);
		    entityTypeMemberPropertySchema = newEntityTypeMemberPropertySchema;
		}
		entityTypeMemberPropertySchema.property(
			entityTypeMemberPathComponents[entityTypeMemberPathComponents.length - 1],
			(new JsonSchema()).const_(new JsonString(typeName)));

		parentSchema.if_(ifSchema);

		thenSchema = new JsonSchema();
		thenSchema.ref(ref);
		parentSchema.then(thenSchema);

		if (remainingSpecificTypeOptions.lastKey().equals(typeName)) {
		    // this is the last type specific option
		    elseSchema = JsonSchema.FALSE;
		    parentSchema.else_(elseSchema);
		} else {
		    elseSchema = new JsonSchema();
		    parentSchema.else_(elseSchema);
		    parentSchema = elseSchema;
		}
	    }
	}

	if (!typeSpecificSchema.isEmpty()) {
	    otherTypeSchemas.add(typeSpecificSchema);
	}

	if (!simpleTypeSchema.isEmpty() && !otherTypeSchemas.isEmpty()) {

	    parentForTypeSchema.oneOf(simpleTypeSchema);
	    parentForTypeSchema.oneOf(otherTypeSchemas.toArray(new JsonSchema[otherTypeSchemas.size()]));

	} else if (simpleTypeSchema.isEmpty() && !otherTypeSchemas.isEmpty()) {

	    if (otherTypeSchemas.size() > 1) {
		parentForTypeSchema.oneOf(otherTypeSchemas.toArray(new JsonSchema[otherTypeSchemas.size()]));
	    } else {
		parentForTypeSchema.addAll(otherTypeSchemas.get(0));
	    }

	} else if (!simpleTypeSchema.isEmpty() && otherTypeSchemas.isEmpty()) {
	    parentForTypeSchema.addAll(simpleTypeSchema);
	} else {
	    // both simpleTypeOptions and otherTypeOptions are empty
	    // possible if value type of a property was not found (error would be logged)
	}
    }

    private void createTypeDefinitionWithValueTypeOptionsForAssociationClassRole(
	    SortedMap<String, JsonSchemaTypeInfo> valueTypeOptionsByTypeName,
	    List<JsonSchemaTypeInfo> additionalJsonTypeInfosForAssociationRole,
	    Optional<JsonSchemaTypeInfo> jsonTypeInfoForAssociationRoleValueType, String associationRoleName,
	    boolean byReferenceAllowedForAssociationRole, JsonSchema parentForTypeSchema) {

	// create schema to restrict association role copy)
	JsonSchema associationClassRoleCopyRestrictionSchema = new JsonSchema();
	associationClassRoleCopyRestrictionSchema.type(JsonSchemaType.OBJECT);

	JsonSchema roleCopyRestrictionSchema = new JsonSchema();
	roleCopyRestrictionSchema.type(JsonSchemaType.OBJECT);

	JsonSchema roleCopyTypeRestrictionSchema = new JsonSchema();
	List<JsonSchemaTypeInfo> additionalJsonTypes = new ArrayList<>();
	if (byReferenceAllowedForAssociationRole) {
	    additionalJsonTypes.add(createJsonSchemaTypeInfoForReference());
	}

	createTypeDefinition(valueTypeOptionsByTypeName, additionalJsonTypes, roleCopyTypeRestrictionSchema);

	associationClassRoleCopyRestrictionSchema.property(associationRoleName, roleCopyTypeRestrictionSchema);

	if (jsonTypeInfoForAssociationRoleValueType.isPresent()) {

	    JsonSchema result = new JsonSchema();

	    // create and add first schema (association class schema ref)
	    JsonSchema typeSchema = new JsonSchema();
	    typeSchema.ref(jsonTypeInfoForAssociationRoleValueType.get().getRef());
	    result.allOf(typeSchema);

	    // add second schema (restriction of association role copy)
	    result.allOf(associationClassRoleCopyRestrictionSchema);

	    associationClassRoleCopyRestrictionSchema = result;

	}

	createTypeDefinition(additionalJsonTypeInfosForAssociationRole, associationClassRoleCopyRestrictionSchema,
		parentForTypeSchema);
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

	if (pme != null && !jsonSchemaTarget.ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme, ci.id())) {

	    jsTypeInfo = jsonSchemaTarget.identifyJsonSchemaType(pme, typeName, encodingRule);

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

		    jsTypeInfo.setRef(jsonSchemaDefinitionReference(jsd, ci));
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

	if (pme != null && !jsonSchemaTarget.ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme, typeId)) {

	    if (pme.hasTargetType()) {
		jsTypeInfo = jsonSchemaTarget.identifyJsonSchemaType(pme, typeName, encodingRule);
	    } else {
		result.addInfo(this, 121, typeName);
		jsTypeInfo = null;
	    }

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
	    return "Restricting facet 'maxLength' of basic type '$1$' defined by tagged value 'length' (or 'maxLength' or 'size') could not be parsed as an integer. Found value: $2$. The facet will be ignored.";
	case 102:
	    return "Restricting facet 'maxLength' of basic type '$1$' defined by tagged value 'length' (or 'maxLength' or 'size') has a negative integer value ($2$), which is not allowed for the facet. The facet will be ignored.";
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
	    return "";
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
	case 119:
	    return "Literal encoding type for class '$1$' determined to be '$2$'. No JSON Schema type could be identified for that encoding type. Using JSON Schema type 'string'.";
	case 120:
	    return "Literal encoding type for enumeration '$1$' must be a simple JSON Schema type. A schema reference was found: '$2$'. Assuming that the referenced JSON Schema contains a type definition for JSON Schema simple type 'string'. This will affect how the enums are encoded.";
	case 121:
	    return "??No target type is defined in map entry for type '$1$'. This is valid if, in the JSON encoding, the type does not require a specific type restriction.";
	case 122:
	    return "??No entity type member path found for specific type option '$1$'. Using '$2$' instead.";

	default:
	    return "(" + JsonSchemaDocument.class.getName() + ") Unknown message with number: " + mnr;
	}
    }

}
