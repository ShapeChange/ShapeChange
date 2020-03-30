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

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.ShapeChange.MapEntryParamInfos;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.RuleRegistry;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Target.SingleTarget;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.JsonSchemaType;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.JsonSchemaVersion;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class JsonSchemaTarget implements SingleTarget, MessageSource {

    protected static Model model = null;
    private static boolean initialised = false;
    protected static boolean diagnosticsOnly = false;
    protected static int numberOfEncodedSchemas = 0;

    /**
     * NOTE: If not set via the configuration, the default applies which is
     * {@value Options#DERIVED_DOCUMENTATION_DEFAULT_TEMPLATE}.
     */
    protected static String documentationTemplate = null;
    /**
     * NOTE: If not set via the configuration, the default applies which is
     * {@value Options#DERIVED_DOCUMENTATION_DEFAULT_NOVALUE}.
     */
    protected static String documentationNoValue = null;

    protected static Boolean prettyPrint = null;

    protected static String outputDirectory = null;

    protected static JsonSchemaVersion jsonSchemaVersion = null;

    protected static String entityTypeName = null;

    protected static String inlineOrByRefDefault = null;
    
    protected static String linkObjectUri = null;

    protected static String byReferenceJsonSchemaDefinition = null;

    protected static String baseJsonSchemaDefinitionForFeatureTypes = null;
    protected static String baseJsonSchemaDefinitionForObjectTypes = null;
    protected static String baseJsonSchemaDefinitionForDataTypes = null;

    protected static String objectIdentifierName = "id";
    protected static JsonSchemaType[] objectIdentifierType = new JsonSchemaType[] { JsonSchemaType.STRING };
    protected static boolean objectIdentifierRequired = false;

    /**
     * Contains information parsed from the 'param' attributes of each map entry
     * defined for this target.
     */
    protected static MapEntryParamInfos mapEntryParamInfos = null;

    protected static SortedMap<PackageInfo, JsonSchemaDocument> jsDocsByPkg = new TreeMap<>();
    protected static Map<ClassInfo, JsonSchemaDocument> jsDocsByCi = new HashMap<>();

    /* ------ */
    /*
     * Non-static fields
     */
    protected ShapeChangeResult result = null;
    protected Options options = null;

    private PackageInfo schema = null;
    private boolean schemaNotEncoded = false;

//	private File outputDirectoryFile = null;

    @Override
    public void initialise(PackageInfo pi, Model m, Options o, ShapeChangeResult r, boolean diagOnly)
	    throws ShapeChangeAbortException {

	schema = pi;
	model = m;
	options = o;
	result = r;
	diagnosticsOnly = diagOnly;

	if (!isEncoded(schema)) {

	    schemaNotEncoded = true;
	    result.addInfo(this, 7, schema.name());
	    return;

	} else {
	    numberOfEncodedSchemas++;
	}

	if (!initialised) {

	    initialised = true;

	    outputDirectory = options.parameter(this.getClass().getName(), "outputDirectory");
	    if (outputDirectory == null)
		outputDirectory = options.parameter("outputDirectory");
	    if (outputDirectory == null)
		outputDirectory = options.parameter(".");

	    // change the default documentation template?
	    documentationTemplate = options.parameter(this.getClass().getName(),
		    JsonSchemaConstants.PARAM_DOCUMENTATION_TEMPLATE);
	    documentationNoValue = options.parameter(this.getClass().getName(),
		    JsonSchemaConstants.PARAM_DOCUMENTATION_NOVALUE);

	    prettyPrint = options.parameterAsBoolean(this.getClass().getName(), JsonSchemaConstants.PARAM_PRETTY_PRINT,
		    true);

	    entityTypeName = options.parameterAsString(this.getClass().getName(),
		    JsonSchemaConstants.PARAM_ENTITY_TYPE_NAME, "entityType", false, true);

	    inlineOrByRefDefault = options.parameterAsString(this.getClass().getName(),
		    JsonSchemaConstants.PARAM_INLINEORBYREF_DEFAULT, "byreference", false, true);

	    linkObjectUri = options.parameterAsString(this.getClass().getName(),
		    JsonSchemaConstants.PARAM_LINK_OBJECT_URI, "", false, true);
	    // TODO: set useful default for linkObjectUri
	    
	    byReferenceJsonSchemaDefinition = options.parameterAsString(this.getClass().getName(),
		    JsonSchemaConstants.PARAM_BY_REFERENCE_JSON_SCHEMA_DEFINITION, null, false, true);

	    baseJsonSchemaDefinitionForFeatureTypes = options.parameterAsString(this.getClass().getName(),
		    JsonSchemaConstants.PARAM_BASE_JSON_SCHEMA_DEF_FEATURE_TYPES, null, false, true);
	    baseJsonSchemaDefinitionForObjectTypes = options.parameterAsString(this.getClass().getName(),
		    JsonSchemaConstants.PARAM_BASE_JSON_SCHEMA_DEF_OBJECT_TYPES, null, false, true);
	    baseJsonSchemaDefinitionForDataTypes = options.parameterAsString(this.getClass().getName(),
		    JsonSchemaConstants.PARAM_BASE_JSON_SCHEMA_DEF_DATA_TYPES, null, false, true);

	    objectIdentifierName = options.parameterAsString(this.getClass().getName(),
		    JsonSchemaConstants.PARAM_OBJECT_IDENTIFIER_NAME, "id", false, true);

	    String objectIdentifierTypeString = options.parameterAsString(this.getClass().getName(),
		    JsonSchemaConstants.PARAM_OBJECT_IDENTIFIER_TYPE, "string", false, true);
	    switch (objectIdentifierTypeString.toLowerCase(Locale.ENGLISH)) {
	    case "number": {
		objectIdentifierType = new JsonSchemaType[] { JsonSchemaType.NUMBER };
		break;
	    }
	    case "number,string":
	    case "number, string":
	    case "string,number":
	    case "string, number": {
		objectIdentifierType = new JsonSchemaType[] { JsonSchemaType.STRING, JsonSchemaType.NUMBER };
		break;
	    }
	    default:
		objectIdentifierType = new JsonSchemaType[] { JsonSchemaType.STRING };
	    }

	    objectIdentifierRequired = options.parameterAsBoolean(this.getClass().getName(),
		    JsonSchemaConstants.PARAM_OBJECT_IDENTIFIER_REQUIRED, false);

	    String jsVersionParamValue = options.parameterAsString(this.getClass().getName(),
		    JsonSchemaConstants.PARAM_JSON_SCHEMA_VERSION, "2019-09", false, true);
	    Optional<JsonSchemaVersion> jsVersion = JsonSchemaVersion.fromString(jsVersionParamValue);

	    if (jsVersion.isPresent()) {
		jsonSchemaVersion = jsVersion.get();
	    } else {
		result.addWarning(this, 10, JsonSchemaConstants.PARAM_JSON_SCHEMA_VERSION, jsVersionParamValue,
			"2019-09");
		jsonSchemaVersion = JsonSchemaVersion.DRAFT_2019_09;
	    }

	    // identify map entries defined in the target configuration
	    List<ProcessMapEntry> mapEntries = options.getCurrentProcessConfig().getMapEntries();

	    if (mapEntries.isEmpty()) {

		/*
		 * It is unlikely but not impossible that an application schema does not make
		 * use of types that require a type mapping in order to be converted into a
		 * database schema.
		 */
		result.addWarning(this, 15);
		mapEntryParamInfos = new MapEntryParamInfos(result, null);

	    } else {

		/*
		 * Parse all parameter information
		 */
		mapEntryParamInfos = new MapEntryParamInfos(result, mapEntries);
	    }

	    File outputDirectoryFile = new File(outputDirectory);

	    // create output directory, if necessary
	    if (!diagnosticsOnly) {

		// Check whether we can use the output directory
		boolean exi = outputDirectoryFile.exists();
		if (!exi) {
		    outputDirectoryFile.mkdirs();
		    exi = outputDirectoryFile.exists();
		}
		boolean dir = outputDirectoryFile.isDirectory();
		boolean wrt = outputDirectoryFile.canWrite();
		boolean rea = outputDirectoryFile.canRead();
		if (!exi || !dir || !wrt || !rea) {
		    result.addFatalError(this, 5, outputDirectory);
		    throw new ShapeChangeAbortException();
		}

	    } else {
		result.addInfo(this, 10002);
	    }
	}

	/*
	 * Required to be performed for each application schema
	 */
	result.addDebug(this, 10001, pi.name());

	String jsonBaseUri = schema.taggedValue("jsonBaseUri");
	if (StringUtils.isBlank(jsonBaseUri)) {
	    jsonBaseUri = options.parameterAsString(this.getClass().getName(), JsonSchemaConstants.PARAM_JSON_BASE_URI,
		    "http://example.org/FIXME", false, true);
	} else {
	    jsonBaseUri = jsonBaseUri.trim();
	}

	String jsonSubdirectory = schema.taggedValue("jsonDirectory");
	if (StringUtils.isBlank(jsonSubdirectory)) {
	    jsonSubdirectory = schema.xmlns();
	}
	if (StringUtils.isBlank(jsonSubdirectory)) {
	    jsonSubdirectory = "default";
	} else {
	    jsonSubdirectory = jsonSubdirectory.trim();
	}

	File outputDirectoryFile = new File(outputDirectory);
	File subDirectoryFile = new File(outputDirectoryFile, jsonSubdirectory);

	createJsonSchemaDocuments(pi, null, jsonBaseUri, jsonSubdirectory, subDirectoryFile);

	if (!diagnosticsOnly) {

	    // create (if necessary) and check subdirectory

	    try {
		if (!subDirectoryFile.exists()) {
		    subDirectoryFile.mkdirs();
		}
		// Check if we have the necessary access
		boolean dir = subDirectoryFile.isDirectory();
		boolean wrt = subDirectoryFile.canWrite();
		boolean rea = subDirectoryFile.canRead();
		if (!dir || !wrt || !rea) {
		    result.addFatalError(this, 5, subDirectoryFile.getName());
		    throw new ShapeChangeAbortException();
		}
	    } catch (Exception e) {
		// Something went wrong with the io concerning the directory
		result.addFatalError(this, 5, subDirectoryFile.getName());
		result.addFatalError(this, 6, e.getMessage());
		throw new ShapeChangeAbortException();
	    }
	}
    }

    private String jsonDocumentName(PackageInfo pi) {

	String s = pi.taggedValue("jsonDocument");

	if (StringUtils.isBlank(s)) {

	    if (pi.isAppSchema()) {

		String appSchemaName = pi.name();

		s = appSchemaName.replace("/", "_").replace(" ", "_") + ".json";

		result.addWarning(this, 101, appSchemaName, s);

	    } else {

		// ensure that the value of s is null
		s = null;
	    }

	} else {

	    // trim the tagged value
	    s = s.trim();
	}

	return s;
    }

    /**
     * Used to create the JSON Schema document(s) for an application schema. Json
     * base URI and subdirectory are stored by each JsonSchemaDocument, because
     * their values can (and typically will) change with each application schema -
     * and this target is a SingleTarget, so can process multiple schemas at once.
     * 
     * @param pi               the package to convert; start with the application
     *                         schema package; the method will drill down to
     *                         subpackages, stopping at other schemas
     * @param jsdcurr          the JSON Schema document that applies to the parent;
     *                         can be <code>null</code> if the given package is the
     *                         application schema package; otherwise, it is used as
     *                         document for packages that are not themselves
     *                         converted to separate JSON Schema documents
     * @param jsonBaseUri      the base URI defined for the application schema (and
     *                         its child packages)
     * @param jsonSubdirectory the subdirectory defined for the application schema
     *                         (and its child packages)
     * @param subDirectoryFile the file fo the subdirectory, in which the JSON
     *                         Schema documents would ultimately be written
     * @return <code>true</code> if a new JSON Schema document was created for the
     *         given package, else <code>false</code> (then the package will have
     *         been assigned to the given JSON Schema document)
     */
    private boolean createJsonSchemaDocuments(PackageInfo pi, JsonSchemaDocument jsdcurr, String jsonBaseUri,
	    String jsonSubdirectory, File subDirectoryFile) {

	boolean res = false;

	/*
	 * Determine and if necessary create JSON Schema document for this package
	 */
	JsonSchemaDocument jsd;
	String jsDoc = jsonDocumentName(pi);

	if (StringUtils.isNotBlank(jsDoc)) {
	    result.addDebug(this, 102, jsDoc, pi.name());
	    jsd = new JsonSchemaDocument(pi, model, options, result, jsDoc, this, jsonBaseUri, jsonSubdirectory,
		    new File(subDirectoryFile, jsDoc), mapEntryParamInfos);
	    res = true;

	} else {
	    jsd = jsdcurr;
	    if (jsd == null) {
		jsDoc = pi.name().replace("/", "_").replace(" ", "_") + ".json";
		result.addWarning(this, 103, pi.name(), jsDoc);

		result.addDebug(this, 102, jsDoc, pi.name());
		jsd = new JsonSchemaDocument(pi, model, options, result, jsDoc, this, jsonBaseUri, jsonSubdirectory,
			new File(subDirectoryFile, jsDoc), mapEntryParamInfos);
		res = true;
	    }
	}

	jsDocsByPkg.put(pi, jsd);

	for (PackageInfo pix : pi.containedPackages()) {
	    if (!pix.isSchema()) {
		createJsonSchemaDocuments(pix, jsd, jsonBaseUri, jsonSubdirectory, subDirectoryFile);
	    }
	}

	return res;
    }

    public static boolean isEncoded(Info i) {

	if (i.matches("rule-json-all-notEncoded") && i.encodingRule("json").equalsIgnoreCase("notencoded")) {

	    return false;

	} else {

	    return true;
	}
    }

    public boolean prettyPrinting() {
	return prettyPrint;
    }

    /**
     * @param ci the class for which to look up the JSON Schema document
     * @return an {@link Optional} with the JSON Schema document in which the given
     *         class is encoded
     */
    public Optional<JsonSchemaDocument> jsonSchemaDocument(ClassInfo ci) {
	return Optional.ofNullable(jsDocsByCi.get(ci));
    }

    public Optional<String> byReferenceJsonSchemaDefinition() {
	return Optional.ofNullable(byReferenceJsonSchemaDefinition);
    }

    public String baseJsonSchemaDefinitionForFeatureTypes() {
	return baseJsonSchemaDefinitionForFeatureTypes;
    }

    public String baseJsonSchemaDefinitionForObjectTypes() {
	return baseJsonSchemaDefinitionForObjectTypes;
    }

    public String baseJsonSchemaDefinitionForDataTypes() {
	return baseJsonSchemaDefinitionForDataTypes;
    }

    public String getEntityTypeName() {
	return entityTypeName;
    }

    public String getInlineOrByRefDefault() {
	return inlineOrByRefDefault;
    }
    
    public String getLinkObjectUri() {
	return linkObjectUri;
    }

    public boolean objectIdentifierRequired() {
	return objectIdentifierRequired;
    }

    public String objectIdentifierName() {
	return objectIdentifierName;
    }

    public JsonSchemaType[] objectIdentifierType() {
	return objectIdentifierType;
    }

    @Override
    public void process(ClassInfo ci) {

	if (ci == null || ci.pkg() == null) {
	    return;
	}

	if (!isEncoded(ci)) {
	    result.addInfo(this, 8, ci.name());
	    return;
	}

	result.addDebug(this, 4, ci.name());

	Optional<ProcessMapEntry> pme = mapEntry(ci);

	if (pme.isPresent()) {
	    result.addInfo(this, 22, ci.name(), pme.get().getTargetType());
	    return;
	}

	if (schemaNotEncoded) {
	    result.addInfo(this, 18, schema.name(), ci.name());
	    return;
	}

	/*
	 * Check if ci inherits (directly or indirectly) from a type that is mapped to a
	 * simple JSON Schema type (string, number, integer, or boolean); if so, ci is a basic
	 * type.
	 */
	JsonSchemaTypeInfo simpleJsTypeInfo = determineIfImplementedBySimpleJsonSchemaType(ci);

	if (simpleJsTypeInfo != null) {

	    if (ci.matches(JsonSchemaConstants.RULE_CLS_BASIC_TYPE)) {

		if (simpleJsTypeInfo.getSimpleType() == JsonSchemaType.BOOLEAN
			|| simpleJsTypeInfo.getSimpleType() == JsonSchemaType.STRING
			|| simpleJsTypeInfo.getSimpleType() == JsonSchemaType.NUMBER
			|| simpleJsTypeInfo.getSimpleType() == JsonSchemaType.INTEGER) {
		    registerClass(ci, simpleJsTypeInfo);
		} else {
		    result.addError(this, 21, ci.name(), simpleJsTypeInfo.getSimpleType().getName());
		}

	    } else {
		result.addError(this, 20, ci.name(), simpleJsTypeInfo.getSimpleType().getName());
	    }

	} else if (ci.category() == Options.MIXIN || ci.category() == Options.OBJECT || ci.category() == Options.FEATURE
		|| ci.category() == Options.DATATYPE || ci.category() == Options.ENUMERATION
		|| ci.category() == Options.CODELIST) {

	    registerClass(ci, null);

	} else if (ci.category() == Options.UNION) {

	    if (ci.matches(JsonSchemaConstants.RULE_CLS_UNION_PROPERTY_COUNT)
		    || ci.matches(JsonSchemaConstants.RULE_CLS_UNION_TYPE_DISCRIMINATOR)) {

		registerClass(ci, null);

	    } else {
		result.addInfo(this, 19, ci.name());
	    }

	} else {

	    result.addInfo(this, 17, ci.name());
	}
    }

    /**
     * Search for the simple JSON Schema type with which the given class - or one of
     * its supertypes - is implemented. NOTE: a tagged value 'base' that may be
     * defined on the supertype (typically containing the name of an XML Schema data
     * type) is currently ignored.
     * 
     * @param ci
     * @return the simple JSON Schema type with which the class (or one of its
     *         supertypes) is implemented, or <code>null</code> if that is not the
     *         case
     */
    private JsonSchemaTypeInfo determineIfImplementedBySimpleJsonSchemaType(ClassInfo ci) {

	String typeName = ci.name();
	String encodingRule = ci.encodingRule(JsonSchemaConstants.PLATFORM);

	// First, check if the class itself is mapped to a simple JSON Schema type
	ProcessMapEntry pme = mapEntryParamInfos.getMapEntry(typeName, encodingRule);
	if (pme != null) {
	    JsonSchemaTypeInfo jsTypeInfo = identifyJsonSchemaType(pme, typeName, encodingRule);
	    if (jsTypeInfo.isSimpleType()) {
		return jsTypeInfo;
	    }
	}

	// Second, check if one of the direct or indirect supertypes is mapped to a
	// simple JSON Schema type
	if (ci.supertypes() != null) {

	    for (String supertypeId : ci.supertypes()) {

		ClassInfo cix = model.classById(supertypeId);

		if (cix != null) {
		    JsonSchemaTypeInfo cixJsTypeInfo = determineIfImplementedBySimpleJsonSchemaType(cix);
		    if (cixJsTypeInfo != null && cixJsTypeInfo.isSimpleType()) {
			return cixJsTypeInfo;
		    }
		}
	    }
	}

	return null;
    }

    public JsonSchemaTypeInfo identifyJsonSchemaType(ProcessMapEntry pme, String typeName, String encodingRule) {

	JsonSchemaTypeInfo jsTypeInfo = new JsonSchemaTypeInfo();

	// check if the target type is one of the simple types defined by JSON Schema
	Optional<JsonSchemaType> simpleType = JsonSchemaType.fromString(pme.getTargetType());

	if (simpleType.isPresent()) {

	    jsTypeInfo.setSimpleType(simpleType.get());

	    if (mapEntryParamInfos.hasCharacteristic(typeName, encodingRule, JsonSchemaConstants.ME_PARAM_FORMATTED,
		    JsonSchemaConstants.ME_PARAM_FORMATTED_CHAR_FORMAT)) {
		jsTypeInfo.setFormat(mapEntryParamInfos.getCharacteristic(typeName, encodingRule,
			JsonSchemaConstants.ME_PARAM_FORMATTED, JsonSchemaConstants.ME_PARAM_FORMATTED_CHAR_FORMAT));
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

	return jsTypeInfo;
    }

    /**
     * Look up the map entry defined for a class. It is not guaranteed that such a
     * map entry exists.
     * 
     * @param ci the class for which to look up a map entry
     * @return an {@link Optional} with the map entry defined for the given class,
     *         under the JSON Schema encoding rule that applies to the class
     */
    public Optional<ProcessMapEntry> mapEntry(ClassInfo ci) {

	return Optional.ofNullable(options.targetMapEntry(ci.name(), ci.encodingRule(JsonSchemaConstants.PLATFORM)));
    }

    /**
     * @param ci               The class to register
     * @param simpleJsTypeInfo if not <code>null</code>, it indicates that ci is a
     *                         basic type, and it provides the simple JSON Schema
     *                         type as which ci shall be implemented
     */
    private void registerClass(ClassInfo ci, JsonSchemaTypeInfo simpleJsTypeInfo) {

	// Identify the JsonSchemaDocument to which the class belongs
	JsonSchemaDocument jsd = jsDocsByPkg.get(ci.pkg());

	// Add the class to that JSON Schema document
	if (simpleJsTypeInfo == null) {
	    // ci is not a basic type
	    jsd.addClass(ci);
	} else {
	    // ci is a basic type
	    jsd.addBasicType(ci, simpleJsTypeInfo);
	}

	/*
	 * Also keep track of this relationship - for lookup of the JsonSchemaDocument
	 * given the class later on
	 */
	jsDocsByCi.put(ci, jsd);
    }

    @Override
    public void write() {

	// nothing to do here (this is a SingleTarget)
    }

    @Override
    public void writeAll(ShapeChangeResult r) {

	this.result = r;
	this.options = r.options();

	if (numberOfEncodedSchemas == 0) {
	    return;
	}

	// get the set of all non-empty json schema documents
	Set<JsonSchemaDocument> jsdocs = jsDocsByPkg.values().stream().filter(jsd -> jsd.hasClasses())
		.collect(Collectors.toCollection(HashSet::new));

	// create the actual JSON Schema definitions
	for (JsonSchemaDocument jsdoc : jsdocs) {
	    jsdoc.createDefinitions();
	}

	if (!diagnosticsOnly) {
	    // Write the JSON Schema definitions to files
	    for (JsonSchemaDocument jsdoc : jsdocs) {
		jsdoc.write();
	    }
	}
    }

    @Override
    public void reset() {

	model = null;

	initialised = false;
	diagnosticsOnly = false;
	numberOfEncodedSchemas = 0;

	documentationTemplate = null;
	documentationNoValue = null;

	outputDirectory = null;

	jsonSchemaVersion = null;
	entityTypeName = null;
	inlineOrByRefDefault = null;
	linkObjectUri = null;
	byReferenceJsonSchemaDefinition = null;

	baseJsonSchemaDefinitionForFeatureTypes = null;
	baseJsonSchemaDefinitionForObjectTypes = null;
	baseJsonSchemaDefinitionForDataTypes = null;

	objectIdentifierName = "id";
	objectIdentifierType = new JsonSchemaType[] { JsonSchemaType.STRING };
	objectIdentifierRequired = false;

	mapEntryParamInfos = null;

	jsDocsByPkg = new TreeMap<>();
	jsDocsByCi = new HashMap<>();

	// TODO - Was fehlt hier noch?
    }

    @Override
    public void registerRulesAndRequirements(RuleRegistry r) {
	/*
	 * JSON encoding rules
	 */
	r.addRule("rule-json-all-documentation");
	r.addRule("rule-json-all-notEncoded");
	r.addRule("rule-json-cls-basictype");
	r.addRule("rule-json-cls-name-as-anchor");
	r.addRule("rule-json-cls-name-as-entityType");
	r.addRule("rule-json-cls-name-as-entityType-union");
	r.addRule("rule-json-cls-generalization");
	r.addRule("rule-json-cls-specialization");
	r.addRule("rule-json-cls-identifierForTypeWithIdentity");
	r.addRule("rule-json-cls-identifierStereotype");
	r.addRule("rule-json-cls-ignoreIdentifier");
	r.addRule("rule-json-cls-virtualGeneralization");
	r.addRule("rule-json-cls-defaultGeometry-singleGeometryProperty");
	r.addRule("rule-json-cls-defaultGeometry-multipleGeometryProperties");
	r.addRule("rule-json-cls-nestedProperties");
	r.addRule("rule-json-cls-union-propertyCount");
	r.addRule("rule-json-cls-union-typeDiscriminator");
	r.addRule("rule-json-cls-codelist-uri-format");
	r.addRule("rule-json-cls-codelist-link");
	r.addRule("rule-json-prop-voidable");
	r.addRule("rule-json-prop-readOnly");
	r.addRule("rule-json-prop-derivedAsReadOnly");
	r.addRule("rule-json-prop-initialValueAsDefault");

	r.addRule("rule-json-cls-valueTypeOptions");

//	r.addExtendsEncRule("geojson", "*");
    }

    @Override
    public String getDefaultEncodingRule() {
	// TODO
	return "geojson";
    }

    @Override
    public String message(int mnr) {

	switch (mnr) {

	case 0:
	    return "Context: class '$1$'";
	case 1:
	    return "Context: property '$1$'";

	case 3:
	    return "Context: class JsonSchemaTarget";
	case 4:
	    return "Processing class '$1$'.";
	case 5:
	    return "Directory named '$1$' does not exist or is not accessible.";
	case 6:
	    return "System error: Exception raised '$1$'. '$2$'";
	case 7:
	    return "Schema '$1$' is not encoded.";
	case 8:
	    return "Class '$1$' is not encoded.";


	case 10:
	    return "Configuration parameter '$1$' has invalid value '$2$'. Using value '$3$' instead.";

	case 15:
	    return "No map entries provided via the configuration.";
//		case 16:
//			return "Value '$1$' of configuration parameter $2$ does not match the regular expression: $3$. The parameter will be ignored.";
	case 17:
	    return "Type '$1$' is of a category not enabled for conversion, meaning that no JSON Schema definition will be created to represent it.";
	case 18:
	    return "Schema '$1$' is not encoded. Thus class '$2$' (which belongs to that schema) is not encoded either.";
	case 19:
	    return "Type '$1$' is a union. By default, unions are not converted. The encoding rule that applies to '$1$' does not contain a conversion rule that would enable the encoding of the union. No JSON Schema definition will be created to represent '$1$'.";
	case 20:
	    return "Type '$1$' directly or indirectly has a supertype that is implemented as simple JSON Schema type '$2$'. However, "
		    + JsonSchemaConstants.RULE_CLS_BASIC_TYPE + " does not apply to '$1$'. The type will be ignored.";
	case 21:
	    return "Type '$1$' directly or indirectly has a supertype that is implemented as simple JSON Schema type. However, that JSON Schema type is not one of 'string', 'number', or 'boolean'. The JSON Schema type is '$2$' - which does not make sense for a basic type. Type '$1$' will be ignored.";
	case 22:
	    return "Type '$1$' has been mapped to '$2$', as defined by the configuration.";

	case 101:
	    return "??Application schema '$1$' is not associated with a JSON Schema document. A default name is used for the JSON Schema document: '$2$'.";
	case 102:
	    return "Creating JSON Schema document '$1$' for package '$2$'.";
	case 103:
	    return "Package '$1$' not associated with any JSON Schema document. Set tagged value 'jsonDocument' on the according schema package. Package '$1$' will be associated with JSON Schema document '$2$'.";

	case 503:
	    return "Output file '$1$' already exists in output directory ('$2$'). It will be deleted prior to processing.";
	case 504:
	    return "File has been deleted.";

	case 10001:
	    return "Generating JSON schemas for application schema $1$.";
	case 10002:
	    return "Diagnostics-only mode. All output to files is suppressed.";
	default:
	    return "(" + JsonSchemaTarget.class.getName() + ") Unknown message with number: " + mnr;
	}
    }

    @Override
    public String getTargetName() {
	return "JSON Schema";
    }

    @Override
    public String getTargetIdentifier() {
	return "json";
    }

    /**
     * @return the jsonSchemaVersion
     */
    public JsonSchemaVersion getJsonSchemaVersion() {
	return jsonSchemaVersion;
    }

}
