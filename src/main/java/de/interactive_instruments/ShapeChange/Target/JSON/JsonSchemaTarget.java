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
 * (c) 2002-2023 interactive instruments GmbH, Bonn, Germany
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;

import de.interactive_instruments.ShapeChange.MapEntryParamInfos;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.ProcessRuleSet;
import de.interactive_instruments.ShapeChange.RuleRegistry;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeParseException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Target.MapEntries;
import de.interactive_instruments.ShapeChange.Target.SingleTarget;
import de.interactive_instruments.ShapeChange.Target.TargetUtil;
import de.interactive_instruments.ShapeChange.Target.JSON.config.AbstractJsonSchemaAnnotationElement;
import de.interactive_instruments.ShapeChange.Target.JSON.json.JsonNumber;
import de.interactive_instruments.ShapeChange.Target.JSON.json.JsonString;
import de.interactive_instruments.ShapeChange.Target.JSON.json.JsonValue;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.ConstKeyword;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.EnumKeyword;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.ExclusiveMaximumKeyword;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.ExclusiveMinimumKeyword;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.FormatKeyword;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.JsonSchemaType;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.JsonSchemaVersion;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.MaxLengthKeyword;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.MaximumKeyword;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.MinLengthKeyword;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.MinimumKeyword;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.MultipleOfKeyword;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.PatternKeyword;

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
    protected static String measureObjectUri = "FIXME";

    protected static String byReferenceJsonSchemaDefinition = null;
    protected static String byReferenceFormat = "uri";

    protected static String baseJsonSchemaDefinitionForCollections = null;
    protected static String baseJsonSchemaDefinitionForFeatureTypes = null;
    protected static String baseJsonSchemaDefinitionForObjectTypes = null;
    protected static String baseJsonSchemaDefinitionForDataTypes = null;

    protected static EncodingInfos baseJsonSchemaDefinitionForFeatureTypes_encodingInfos = null;
    protected static EncodingInfos baseJsonSchemaDefinitionForObjectTypes_encodingInfos = null;
    protected static EncodingInfos baseJsonSchemaDefinitionForDataTypes_encodingInfos = null;

    protected static String collectionSchemaFileName = null;
    protected static boolean featureCollectionOnly = false;

    protected static boolean preventUnknownTypesInFeatureCollection = false;

    protected static Optional<EncodingRestrictions> idMemberEncodingRestrictions = Optional.empty();

    protected static String objectIdentifierName = "id";
    protected static JsonSchemaType[] objectIdentifierType = new JsonSchemaType[] { JsonSchemaType.STRING };
    protected static boolean objectIdentifierRequired = false;

    /**
     * Contains information parsed from the 'param' attributes of each map entry
     * defined for this target.
     */
    protected static MapEntryParamInfos mapEntryParamInfos = null;
    protected static SortedMap<PackageInfo, List<ProcessMapEntry>> mapEntriesForEncodedTypesBySchemaPackage = new TreeMap<>();
    protected static Map<ClassInfo, EncodingInfos> encodingInfosByCi = new HashMap<>();

    protected static SortedMap<PackageInfo, JsonSchemaDocument> jsDocsByPkg = new TreeMap<>();
    protected static Map<ClassInfo, JsonSchemaDocument> jsDocsByCi = new HashMap<>();

    protected static List<AbstractJsonSchemaAnnotationElement> annotationElements = new ArrayList<>();

    protected static boolean createFeatureCollection = false;
    protected static PackageInfo schemaForFeatureCollection = null;

    protected static boolean useAnchorsInLinksToGeneratedSchemaDefinitions = true;

    protected static boolean createSeparatePropertyDefinitions = false;
    protected static SortedSet<String> geoJsonCompatibleGeometryTypes = null;
     
    protected static SortedSet<String> featureRefProfiles = null;
    protected static SortedSet<JsonSchemaType> featureRefIdTypes = new TreeSet<>();
    protected static boolean featureRefWithAnyCollectionId = false;

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

	    /*
	     * Identify the schema in which to create the "FeatureCollection" definition
	     * schema.
	     */
	    if (schemaForFeatureCollection == null) {

		// try to identify the main schema (just once)
		if (!initialised) {
		    schemaForFeatureCollection = TargetUtil.findMainSchemaForSingleTargets(model.selectedSchemas(), o,
			    r);
		}

		// otherwise we use the first schema we encounter
		if (schemaForFeatureCollection == null) {
		    schemaForFeatureCollection = schema;
		}
	    }
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

	    useAnchorsInLinksToGeneratedSchemaDefinitions = options.parameterAsBoolean(this.getClass().getName(),
		    JsonSchemaConstants.PARAM_USE_ANCHOR_IN_LINKS_TO_GEN_SCHEMA_DEFS, true);

	    entityTypeName = options.parameterAsString(this.getClass().getName(),
		    JsonSchemaConstants.PARAM_ENTITY_TYPE_NAME, "entityType", false, true);

	    inlineOrByRefDefault = options.parameterAsString(this.getClass().getName(),
		    JsonSchemaConstants.PARAM_INLINEORBYREF_DEFAULT, "byreference", false, true);

	    linkObjectUri = options.parameterAsString(this.getClass().getName(),
		    JsonSchemaConstants.PARAM_LINK_OBJECT_URI, "", false, true);
	    // TODO: set useful default for linkObjectUri

	    measureObjectUri = options.parameterAsString(this.getClass().getName(),
		    JsonSchemaConstants.PARAM_MEASURE_OBJECT_URI, "FIXME", false, true);

	    byReferenceJsonSchemaDefinition = options.parameterAsString(this.getClass().getName(),
		    JsonSchemaConstants.PARAM_BY_REFERENCE_JSON_SCHEMA_DEFINITION, null, false, true);

	    byReferenceFormat = options.parameterAsString(this.getClass().getName(),
		    JsonSchemaConstants.PARAM_BY_REFERENCE_FORMAT, "uri", false, true);

	    baseJsonSchemaDefinitionForCollections = options.parameterAsString(this.getClass().getName(),
		    JsonSchemaConstants.PARAM_BASE_JSON_SCHEMA_DEF_COLLECTIONS, null, false, true);

	    baseJsonSchemaDefinitionForFeatureTypes = options.parameterAsString(this.getClass().getName(),
		    JsonSchemaConstants.PARAM_BASE_JSON_SCHEMA_DEF_FEATURE_TYPES, null, false, true);
	    if (StringUtils.isNotBlank(baseJsonSchemaDefinitionForFeatureTypes)
		    && options.hasParameter(this.getClass().getName(),
			    JsonSchemaConstants.PARAM_BASE_JSON_SCHEMA_DEF_FEATURE_TYPES_ENCODING_INFOS)) {
		// format already checked by validation of the configuration
		baseJsonSchemaDefinitionForFeatureTypes_encodingInfos = EncodingInfos
			.from(options.parameterAsString(this.getClass().getName(),
				JsonSchemaConstants.PARAM_BASE_JSON_SCHEMA_DEF_FEATURE_TYPES_ENCODING_INFOS, null,
				false, true));
	    }

	    baseJsonSchemaDefinitionForObjectTypes = options.parameterAsString(this.getClass().getName(),
		    JsonSchemaConstants.PARAM_BASE_JSON_SCHEMA_DEF_OBJECT_TYPES, null, false, true);
	    if (StringUtils.isNotBlank(baseJsonSchemaDefinitionForObjectTypes)
		    && options.hasParameter(this.getClass().getName(),
			    JsonSchemaConstants.PARAM_BASE_JSON_SCHEMA_DEF_OBJECT_TYPES_ENCODING_INFOS)) {
		// format already checked by validation of the configuration
		baseJsonSchemaDefinitionForObjectTypes_encodingInfos = EncodingInfos.from(options.parameterAsString(
			this.getClass().getName(),
			JsonSchemaConstants.PARAM_BASE_JSON_SCHEMA_DEF_OBJECT_TYPES_ENCODING_INFOS, null, false, true));
	    }

	    baseJsonSchemaDefinitionForDataTypes = options.parameterAsString(this.getClass().getName(),
		    JsonSchemaConstants.PARAM_BASE_JSON_SCHEMA_DEF_DATA_TYPES, null, false, true);
	    if (StringUtils.isNotBlank(baseJsonSchemaDefinitionForDataTypes)
		    && options.hasParameter(this.getClass().getName(),
			    JsonSchemaConstants.PARAM_BASE_JSON_SCHEMA_DEF_DATA_TYPES_ENCODING_INFOS)) {
		// format already checked by validation of the configuration
		baseJsonSchemaDefinitionForDataTypes_encodingInfos = EncodingInfos.from(options.parameterAsString(
			this.getClass().getName(),
			JsonSchemaConstants.PARAM_BASE_JSON_SCHEMA_DEF_DATA_TYPES_ENCODING_INFOS, null, false, true));
	    }

	    collectionSchemaFileName = options.parameterAsString(this.getClass().getName(),
		    JsonSchemaConstants.PARAM_COLLECTION_SCHEMA_FILE_NAME, null, false, true);
	    if (StringUtils.isNotBlank(collectionSchemaFileName) && !collectionSchemaFileName.endsWith(".json")) {
		collectionSchemaFileName = collectionSchemaFileName + ".json";
	    }

	    featureCollectionOnly = options.parameterAsBoolean(this.getClass().getName(),
		    JsonSchemaConstants.PARAM_FEATURE_COLLECTION_ONLY, false);

	    preventUnknownTypesInFeatureCollection = options.parameterAsBoolean(this.getClass().getName(),
		    JsonSchemaConstants.PARAM_PREVENT_UNKNOWN_TYPES_IN_FEATURE_COLLECTIONS, false);

	    if (options.hasParameter(this.getClass().getName(),
		    JsonSchemaConstants.PARAM_ID_MEMBER_ENCODING_RESTRICTIONS)) {
		// format already checked by validation of the configuration
		idMemberEncodingRestrictions = Optional
			.of(EncodingRestrictions.from(options.parameterAsString(this.getClass().getName(),
				JsonSchemaConstants.PARAM_ID_MEMBER_ENCODING_RESTRICTIONS, null, false, true)));
	    }

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

	    createSeparatePropertyDefinitions = options.parameterAsBoolean(this.getClass().getName(),
		    JsonSchemaConstants.PARAM_CREATE_SEPARATE_PROPERTY_DEFINITIONS, false);
	    geoJsonCompatibleGeometryTypes = new TreeSet<>(
		    options.parameterAsStringList(this.getClass().getName(),
			    JsonSchemaConstants.PARAM_GEOJSON_COMPATIBLE_GEOMETRY_TYPES, new String[] { "GM_Point",
				    "GM_Curve", "GM_Surface", "GM_MultiPoint", "GM_MultiCurve", "GM_MultiSurface" },
			    false, true));
	    featureRefProfiles = new TreeSet<>(options.parameterAsStringList(this.getClass().getName(),
		    JsonSchemaConstants.PARAM_FEATURE_REF_PROFILES, new String[] { "rel-as-link" }, false, true));
	    List<String> featureRefIdTypes_ = options.parameterAsStringList(this.getClass().getName(),
		    JsonSchemaConstants.PARAM_FEATURE_REF_ID_TYPES, new String[] { "integer" }, false, true);

	    for (String s : featureRefIdTypes_) {
		if ("string".equalsIgnoreCase(s)) {
		    featureRefIdTypes.add(JsonSchemaType.STRING);
		} else if ("integer".equalsIgnoreCase(s)) {
		    featureRefIdTypes.add(JsonSchemaType.INTEGER);
		}
	    }

	    featureRefWithAnyCollectionId = options.parameterAsBoolean(this.getClass().getName(),
		    JsonSchemaConstants.PARAM_FEATURE_REF_ANY_COLLECTION_ID, false);

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

	    if (options.getCurrentProcessConfig().getAdvancedProcessConfigurations() == null) {

		result.addDebug(this, 12);

	    } else {

		Element advancedProcessConfigElmt = options.getCurrentProcessConfig()
			.getAdvancedProcessConfigurations();

		// identify annotation elements
		try {
		    List<AbstractJsonSchemaAnnotationElement> annotationElmts = AnnotationGenerator
			    .parseAndValidateJsonSchemaAnnotationElements(advancedProcessConfigElmt);
		    annotationElements = annotationElmts;
		} catch (ShapeChangeParseException e) {
		    result.addError(this, 104, e.getMessage());
		}
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

	String jsonBaseUri = baseUriForSchemaPackage(schema);

	String jsonSubdirectory = identifyJsonDirectory(schema);

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

    private String baseUriForSchemaPackage(PackageInfo schemaPi) {

	String jsonBaseUri = schemaPi.taggedValue("jsonBaseUri");
	if (StringUtils.isBlank(jsonBaseUri)) {
	    jsonBaseUri = options.parameterAsString(this.getClass().getName(), JsonSchemaConstants.PARAM_JSON_BASE_URI,
		    "http://example.org/FIXME", false, true);
	} else {
	    jsonBaseUri = jsonBaseUri.trim();
	}

	return jsonBaseUri;
    }

    private String jsonDocumentName(PackageInfo pi) {

	String s = pi.taggedValue("jsonDocument");

	if (StringUtils.isBlank(s)) {

	    if (pi.isAppSchema()) {

		String appSchemaName = pi.name();

		s = normalizedJsonDocumentName(pi);

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

	String schemaId = determineSchemaId(pi, jsDoc, jsonBaseUri, jsonSubdirectory);

	if (StringUtils.isNotBlank(jsDoc)) {
	    result.addDebug(this, 102, jsDoc, pi.name());
	    jsd = new JsonSchemaDocument(pi, model, options, result, this, schemaId, new File(subDirectoryFile, jsDoc),
		    mapEntryParamInfos, false);
	    res = true;

	} else {
	    jsd = jsdcurr;
	    if (jsd == null) {
		jsDoc = normalizedJsonDocumentName(pi);
		result.addWarning(this, 103, pi.name(), jsDoc);

		result.addDebug(this, 102, jsDoc, pi.name());
		jsd = new JsonSchemaDocument(pi, model, options, result, this, schemaId,
			new File(subDirectoryFile, jsDoc), mapEntryParamInfos, false);
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

    private String determineSchemaId(PackageInfo someAppSchemaPackage, String docName, String jsonBaseUri,
	    String jsonSubdirectory) {

	String result;
	if (someAppSchemaPackage != null && StringUtils.isNotBlank(someAppSchemaPackage.taggedValue("jsonId"))) {
	    result = someAppSchemaPackage.taggedValue("jsonId").trim();
	} else {
	    result = StringUtils.join(new String[] { StringUtils.removeEnd(jsonBaseUri, "/"),
		    StringUtils.removeEnd(jsonSubdirectory, "/"), docName }, "/");
	}

	return result;
    }

    public static boolean isEncoded(Info i) {

	if (i.matches(JsonSchemaConstants.RULE_ALL_NOT_ENCODED)
		&& i.encodingRule("json").equalsIgnoreCase("notencoded")) {

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

    public List<AbstractJsonSchemaAnnotationElement> getAnnotationElements() {
	return annotationElements;
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

	if (pme.isPresent() && !ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme.get(), ci.id())) {
	    if (mapEntryParamInfos.hasParameter(ci.name(), ci.encodingRule(JsonSchemaConstants.PLATFORM),
		    JsonSchemaConstants.ME_PARAM_KEYWORDS)) {
		result.addInfo(this, 23, ci.name(), pme.get().getTargetType());
	    } else {
		result.addInfo(this, 22, ci.name(), pme.get().getTargetType());
	    }
	    return;
	}

	if (schemaNotEncoded) {
	    result.addInfo(this, 18, schema.name(), ci.name());
	    return;
	}

	/*
	 * Check if ci inherits (directly or indirectly) from a type that is mapped to a
	 * simple JSON Schema type (string, number, integer, or boolean); if so, ci is a
	 * basic type.
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

	    if (ci.category() == Options.FEATURE
		    && ci.matches(JsonSchemaConstants.RULE_CLS_COLLECTIONS_BASED_ON_ENTITY_TYPE)) {
		createFeatureCollection = true;
	    }

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
     * @param pme    map entry that would apply for the type with given ID
     * @param typeId ID of the type to check
     * @return <code>true</code>, if the map entry shall be ignored for the type
     *         with given id because the map entry has parameter
     *         {@value JsonSchemaConstants#ME_PARAM_IGNORE_FOR_TYPE_FROM_SEL_SCHEMA}
     *         and the type is encoded and owned by one of the schemas selected for
     *         processing; else <code>false</code>
     */
    public boolean ignoreMapEntryForTypeFromSchemaSelectedForProcessing(ProcessMapEntry pme, String typeId) {

	if (StringUtils.isBlank(typeId)) {

	    return false;

	} else {

	    ClassInfo type = model.classById(typeId);

	    if (type == null || !JsonSchemaTarget.isEncoded(type) || !model.isInSelectedSchemas(type)) {
		return false;
	    } else {
		if (mapEntryParamInfos.hasParameter(pme,
			JsonSchemaConstants.ME_PARAM_IGNORE_FOR_TYPE_FROM_SEL_SCHEMA)) {
		    return true;
		} else
		    return false;
	    }
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
	if (pme != null && !ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme, ci.id())) {
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

	    JsonSchemaType jsType = simpleType.get();
	    jsTypeInfo.setSimpleType(jsType);

	    if (mapEntryParamInfos.hasParameter(typeName, encodingRule, JsonSchemaConstants.ME_PARAM_KEYWORDS)) {

		Map<String, String> characteristics = mapEntryParamInfos.getCharacteristics(typeName, encodingRule,
			JsonSchemaConstants.ME_PARAM_KEYWORDS);

		for (String characteristic : characteristics.keySet()) {

		    String value = characteristics.get(characteristic);

		    if (StringUtils.isBlank(value)) {

			// will be detected and reported by the JSON Schema target configuration
			// validator

		    } else if (characteristic.equalsIgnoreCase("format")) {

			jsTypeInfo.setKeyword(new FormatKeyword(value));

		    } else if (jsType == JsonSchemaType.INTEGER || jsType == JsonSchemaType.NUMBER) {

			if (characteristic.equalsIgnoreCase("enum")) {

			    String[] values = value.split("\\s*,\\s*");
			    double[] doubleValues = new double[values.length];

			    for (int i = 0; i < values.length; i++) {
				try {
				    double d = Double.parseDouble(values[i]);
				    doubleValues[i] = d;
				} catch (NumberFormatException e) {
				    // will be detected and reported by the JSON Schema target configuration
				    // validator
				}
			    }

			    List<JsonValue> enums = new ArrayList<>();
			    for (double d : doubleValues) {
				enums.add(new JsonNumber(d));
			    }

			    jsTypeInfo.setKeyword(new EnumKeyword(enums));

			} else {

			    try {

				double d = Double.parseDouble(value);

				if (characteristic.equalsIgnoreCase("multipleOf")) {

				    // error if <= 0 - will be detected and reported by the JSON Schema target
				    // configuration validator
				    jsTypeInfo.setKeyword(new MultipleOfKeyword(d));
				} else if (characteristic.equalsIgnoreCase("maximum")) {
				    jsTypeInfo.setKeyword(new MaximumKeyword(d));
				} else if (characteristic.equalsIgnoreCase("minimum")) {
				    jsTypeInfo.setKeyword(new MinimumKeyword(d));
				} else if (characteristic.equalsIgnoreCase("exclusiveMinimum")) {
				    jsTypeInfo.setKeyword(new ExclusiveMinimumKeyword(d));
				} else if (characteristic.equalsIgnoreCase("exclusiveMaximum")) {
				    jsTypeInfo.setKeyword(new ExclusiveMaximumKeyword(d));
				} else if (characteristic.equalsIgnoreCase("const")) {
				    jsTypeInfo.setKeyword(new ConstKeyword(new JsonNumber(d)));
				} else {
				    // unsupported keyword - will be detected and reported by the JSON Schema target
				    // configuration validator
				}

			    } catch (NumberFormatException e) {
				// will be detected and reported by the JSON Schema target configuration
				// validator
			    }
			}

		    } else if (jsType == JsonSchemaType.STRING) {

			if (characteristic.equalsIgnoreCase("enum")) {

			    String[] values = value.split("\\s*,\\s*");
			    List<JsonValue> enums = new ArrayList<>();
			    for (String v : values) {
				enums.add(new JsonString(v));
			    }
			    jsTypeInfo.setKeyword(new EnumKeyword(enums));

			} else if (characteristic.equalsIgnoreCase("const")) {

			    jsTypeInfo.setKeyword(new ConstKeyword(new JsonString(value)));

			} else if (characteristic.equalsIgnoreCase("pattern")) {

			    jsTypeInfo.setKeyword(new PatternKeyword(value));

			} else if (characteristic.equalsIgnoreCase("patternBase64")) {

			    Decoder decoder = Base64.getDecoder();
			    String decodedValue = new String(decoder.decode(value));

			    jsTypeInfo.setKeyword(new PatternKeyword(decodedValue));

			} else if (characteristic.equalsIgnoreCase("maxLength")
				|| characteristic.equalsIgnoreCase("minLength")) {

			    try {

				int i = Integer.parseInt(value);

				// error if <= 0 - will be detected and reported by the JSON Schema target
				// configuration validator

				if (characteristic.equalsIgnoreCase("maxLength")) {
				    jsTypeInfo.setKeyword(new MaxLengthKeyword(i));
				} else {
				    jsTypeInfo.setKeyword(new MinLengthKeyword(i));
				}

			    } catch (NumberFormatException e) {
				// will be detected and reported by the JSON Schema target configuration
				// validator
			    }

			} else {

			    // unsupported keyword - will be detected and reported by the JSON Schema target
			    // configuration validator
			}

		    } else {

			// unsupported targetType/keyword - will be detected and reported by the JSON
			// Schema target configuration validator
		    }
		}
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

	// check if the type is a measure type
	if (mapEntryParamInfos.hasParameter(pme, JsonSchemaConstants.ME_PARAM_MEASURE)) {
	    jsTypeInfo.setMeasure(true);
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

    public PackageInfo getSchemaForFeatureCollection() {
	return schemaForFeatureCollection;
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

	// get the set of all non-empty json schema documents, including the feature
	// collection document
	Set<JsonSchemaDocument> jsdocs = jsDocsByPkg.values().stream()
		.filter(jsd -> jsd.hasClasses() || (createFeatureCollection && jsd.isFeatureCollectionDocument()))
		.collect(Collectors.toCollection(HashSet::new));

	/*
	 * 1. create the actual JSON Schema definitions
	 */
	for (JsonSchemaDocument jsdoc : jsdocs) {
	    jsdoc.createDefinitions();
	}

	/*
	 * 2. compute encoding infos (requires JSON Schema definitions to be created)
	 */
	for (JsonSchemaDocument jsdoc : jsdocs) {
	    jsdoc.computeEncodingInfos();
	}

	/*
	 * 3. apply member restrictions (requires basic encoding infos to be computed
	 * for all relevant classes)
	 */
	for (JsonSchemaDocument jsdoc : jsdocs) {
	    jsdoc.applyMemberRestrictions();
	}

	/*
	 * 4. create collection definitions (requires encoding infos to be computed for
	 * all relevant classes)
	 */
	if (StringUtils.isNotBlank(collectionSchemaFileName)) {

	    String jsDoc = collectionSchemaFileName;
	    String jsonSubdirectory = identifyJsonDirectory(schemaForFeatureCollection);
	    File outputDirectoryFile = new File(outputDirectory);
	    File subDirectoryFile = new File(outputDirectoryFile, jsonSubdirectory);
	    String jsonBaseUri = baseUriForSchemaPackage(schemaForFeatureCollection);
	    String baseSchemaId = determineSchemaId(schemaForFeatureCollection, jsDoc, jsonBaseUri, jsonSubdirectory);

	    String collSchemaId;
	    if (baseSchemaId.endsWith(".json")) {
		collSchemaId = baseSchemaId.substring(0, baseSchemaId.lastIndexOf("/") + 1) + collectionSchemaFileName;
	    } else {
		collSchemaId = StringUtils.removeEnd(baseSchemaId, "/") + "/" + collectionSchemaFileName;
	    }

	    JsonSchemaDocument collJsd = new JsonSchemaDocument(null, model, options, result, this, collSchemaId,
		    new File(subDirectoryFile, jsDoc), mapEntryParamInfos, true);
	    jsdocs.add(collJsd);
	    collJsd.createCollectionDefinitions();

	} else {
	    for (JsonSchemaDocument jsdoc : jsdocs) {
		jsdoc.createCollectionDefinitions();
	    }
	}

	/*
	 * 5. create map entries (requires encoding infos to be computed, and
	 * restrictions to be applied)
	 */
	for (JsonSchemaDocument jsdoc : jsdocs) {
	    jsdoc.createMapEntries();
	}

	if (!diagnosticsOnly) {

	    // Write the JSON Schema definitions to files
	    for (JsonSchemaDocument jsdoc : jsdocs) {
		jsdoc.write();
	    }

	    // if configured, write map entries file(s)
	    if (options.parameterAsBoolean(this.getClass().getName(), JsonSchemaConstants.PARAM_WRITE_MAP_ENTRIES,
		    false)) {

		File outputDirectoryFile = new File(outputDirectory);

		for (Entry<PackageInfo, List<ProcessMapEntry>> e : mapEntriesForEncodedTypesBySchemaPackage
			.entrySet()) {

		    PackageInfo schemaPi = e.getKey();
		    List<ProcessMapEntry> mapEntriesList = e.getValue();

		    String mapEntriesFileName = schemaPi.name().trim().replaceAll("\\W", "_") + "_mapEntries.xml";
		    File mapEntriesFile = new File(outputDirectoryFile, mapEntriesFileName);
		    MapEntries mapEntries = new MapEntries();
		    mapEntries.add(mapEntriesList);
		    mapEntries.toXml(mapEntriesFile, result);
		}

	    }
	}
    }

    public static String normalizedJsonDocumentName(PackageInfo pi) {
	return pi.name().replace("/", "_").replace(" ", "_") + ".json";
    }

    public static String identifyJsonDirectory(PackageInfo pi) {

	String jsonDirectory = pi.taggedValue("jsonDirectory");
	if (StringUtils.isBlank(jsonDirectory)) {
	    jsonDirectory = pi.xmlns();
	}
	if (StringUtils.isBlank(jsonDirectory)) {
	    jsonDirectory = "default";
	} else {
	    jsonDirectory = jsonDirectory.trim();
	}

	return jsonDirectory;
    }

    public void addMapEntry(PackageInfo schemaPackage, ProcessMapEntry me) {

	List<ProcessMapEntry> mapEntries;

	if (mapEntriesForEncodedTypesBySchemaPackage.containsKey(schemaPackage)) {
	    mapEntries = mapEntriesForEncodedTypesBySchemaPackage.get(schemaPackage);
	} else {
	    mapEntries = new ArrayList<>();
	    mapEntriesForEncodedTypesBySchemaPackage.put(schemaPackage, mapEntries);
	}

	mapEntries.add(me);
    }

    public EncodingInfos getOrCreateEncodingInfos(ClassInfo ci) {

	if (encodingInfosByCi.containsKey(ci)) {
	    return encodingInfosByCi.get(ci);
	} else {
	    EncodingInfos encInfo = new EncodingInfos();
	    encodingInfosByCi.put(ci, encInfo);
	    return encInfo;
	}
    }

    public Set<ClassInfo> getAllEncodedTypes() {
	return jsDocsByCi.keySet();
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
	measureObjectUri = "FIXME";
	byReferenceJsonSchemaDefinition = null;
	byReferenceFormat = "uri";

	baseJsonSchemaDefinitionForCollections = null;
	baseJsonSchemaDefinitionForFeatureTypes = null;
	baseJsonSchemaDefinitionForObjectTypes = null;
	baseJsonSchemaDefinitionForDataTypes = null;

	baseJsonSchemaDefinitionForFeatureTypes_encodingInfos = null;
	baseJsonSchemaDefinitionForObjectTypes_encodingInfos = null;
	baseJsonSchemaDefinitionForDataTypes_encodingInfos = null;

	collectionSchemaFileName = null;
	featureCollectionOnly = false;
	preventUnknownTypesInFeatureCollection = false;

	idMemberEncodingRestrictions = Optional.empty();

	objectIdentifierName = "id";
	objectIdentifierType = new JsonSchemaType[] { JsonSchemaType.STRING };
	objectIdentifierRequired = false;

	mapEntryParamInfos = null;
	mapEntriesForEncodedTypesBySchemaPackage = new TreeMap<>();
	encodingInfosByCi = new HashMap<>();
	annotationElements = new ArrayList<>();

	jsDocsByPkg = new TreeMap<>();
	jsDocsByCi = new HashMap<>();

	createFeatureCollection = false;
	schemaForFeatureCollection = null;

	useAnchorsInLinksToGeneratedSchemaDefinitions = true;

	createSeparatePropertyDefinitions = false;
	geoJsonCompatibleGeometryTypes = null;
	featureRefProfiles = null;
	featureRefIdTypes = new TreeSet<>();
	featureRefWithAnyCollectionId = false;
    }

    @Override
    public void registerRulesAndRequirements(RuleRegistry r) {

	r.addRule("rule-json-all-documentation");
	r.addRule("rule-json-all-featureRefs");
	r.addRule("rule-json-all-notEncoded");
	r.addRule("rule-json-cls-basictype");
	r.addRule("rule-json-cls-codelist-link");
	r.addRule("rule-json-cls-codelist-uri-format");
	r.addRule("rule-json-cls-collectionsBasedOnEntityType");
	r.addRule("rule-json-cls-collectionsWithTopLevelEntityType");
	r.addRule("rule-json-cls-defaultGeometry-singleGeometryProperty");
	r.addRule("rule-json-cls-defaultGeometry-multipleGeometryProperties");
	r.addRule("rule-json-cls-documentation-enumDescription");
	r.addRule("rule-json-cls-identifierForTypeWithIdentity");
	r.addRule("rule-json-cls-identifierStereotype");
	r.addRule("rule-json-cls-ignoreIdentifier");
	r.addRule("rule-json-cls-jsonFgGeometry");
	r.addRule("rule-json-cls-name-as-anchor");
	r.addRule("rule-json-cls-name-as-entityType");
	r.addRule("rule-json-cls-name-as-entityType-dataType");
	r.addRule("rule-json-cls-name-as-entityType-union");
	r.addRule("rule-json-cls-nestedProperties");
	r.addRule("rule-json-cls-primaryGeometry");
	r.addRule("rule-json-cls-primaryPlace");
	r.addRule("rule-json-cls-primaryTime");
	r.addRule("rule-json-cls-restrictExternalEntityTypeMember");
	r.addRule("rule-json-cls-restrictExternalIdentifierMember");
	r.addRule("rule-json-cls-union-propertyCount");
	r.addRule("rule-json-cls-union-typeDiscriminator");
	r.addRule("rule-json-cls-valueTypeOptions");
	r.addRule("rule-json-cls-virtualGeneralization");
	r.addRule("rule-json-prop-derivedAsReadOnly");
	r.addRule("rule-json-prop-initialValueAsDefault");
	r.addRule("rule-json-prop-inlineOrByReferenceTag");
	r.addRule("rule-json-prop-measure");
	r.addRule("rule-json-prop-readOnly");
	r.addRule("rule-json-prop-voidable");

	ProcessRuleSet defaultGeoJsonPrs = new ProcessRuleSet("defaultGeoJson", "*", new TreeSet<>(Stream
		.of("rule-json-cls-defaultGeometry-singleGeometryProperty", "rule-json-cls-ignoreIdentifier",
			"rule-json-cls-name-as-anchor", "rule-json-cls-nestedProperties",
			"rule-json-cls-virtualGeneralization", "rule-json-prop-derivedAsReadOnly",
			"rule-json-prop-initialValueAsDefault", "rule-json-prop-readOnly", "rule-json-prop-voidable")
		.collect(Collectors.toSet())));
	r.addRuleSet(defaultGeoJsonPrs);

	ProcessRuleSet defaultPlainJsonPrs = new ProcessRuleSet("defaultPlainJson", "*",
		new TreeSet<>(Stream.of("rule-json-cls-name-as-anchor", "rule-json-prop-derivedAsReadOnly",
			"rule-json-prop-initialValueAsDefault", "rule-json-prop-readOnly", "rule-json-prop-voidable")
			.collect(Collectors.toSet())));
	r.addRuleSet(defaultPlainJsonPrs);
    }

    @Override
    public String getDefaultEncodingRule() {
	return "*";
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
	    return "System error: Exception encountered. Message is: '$1$'";
	case 7:
	    return "Schema '$1$' is not encoded.";
	case 8:
	    return "Class '$1$' is not encoded.";

	case 10:
	    return "Configuration parameter '$1$' has invalid value '$2$'. Using value '$3$' instead.";
	case 12:
	    return "The target configuration does not contain an advanced process configuration element with definitions of JSON Schema annotations.";

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
	case 23:
	    return "Type '$1$' has been mapped to '$2$' with keywords, as defined by the configuration.";

	case 101:
	    return "??Application schema '$1$' is not associated with a JSON Schema document. A default name is used for the JSON Schema document: '$2$'.";
	case 102:
	    return "Creating JSON Schema document '$1$' for package '$2$'.";
	case 103:
	    return "Package '$1$' not associated with any JSON Schema document. Set tagged value 'jsonDocument' on the according schema package. Package '$1$' will be associated with JSON Schema document '$2$'.";
	case 104:
	    return "Invalid JSON Schema annotation(s) encountered (they will be ignored): $1$";

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
