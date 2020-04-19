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
package de.interactive_instruments.ShapeChange.Target.OpenApi;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import de.interactive_instruments.ShapeChange.Target.JSON.JsonSchemaTarget;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonException;
import jakarta.json.JsonMergePatch;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonPointer;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class OpenApiDefinition implements SingleTarget, MessageSource {

    protected static Model model = null;
    private static boolean initialised = false;
    protected static boolean diagnosticsOnly = false;
    protected static int numberOfEncodedSchemas = 0;

    protected static String outputDirectory = null;
    protected static String outputFilename = null;

    protected static OpenApiConfigItems oapiConfig = null;

    protected static JsonObject baseTemplate = null;
    protected static String jsonSchemasBaseLocation = null;
    protected static String jsonSchemasPathSeparator = null;

    protected static SortedSet<String> collections = new TreeSet<>();
    protected static SortedMap<ClassInfo, String> jsonSchemaPathByFeatureType = new TreeMap<>();

    /* ------ */
    /*
     * Non-static fields
     */

    protected ShapeChangeResult result = null;
    protected Options options = null;

    private PackageInfo schema = null;
    private boolean schemaNotEncoded = false;

    @Override
    public void initialise(PackageInfo pi, Model m, Options o, ShapeChangeResult r, boolean diagOnly)
	    throws ShapeChangeAbortException {

	schema = pi;
	model = m;
	options = o;
	result = r;
	diagnosticsOnly = diagOnly;

	if (!isEncoded(schema) || !isJsonEncoded(schema)) {

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

	    outputFilename = options.parameter(this.getClass().getName(), "outputFilename");
	    if (outputFilename == null)
		outputFilename = "OpenApiDefinition";

	    outputFilename = outputFilename.endsWith(".json") ? outputFilename : outputFilename + ".json";

	    try {
		oapiConfig = new OpenApiConfigItems(result,
			options.getCurrentProcessConfig().getAdvancedProcessConfigurations());
	    } catch (Exception e) {
		result.addFatalError(this, 11);
		throw new ShapeChangeAbortException();
	    }

	    String baseTemplateValue = options.parameterAsString(this.getClass().getName(),
		    OpenApiConstants.PARAM_BASE_TEMPLATE, null, false, true);

	    try {
		baseTemplate = loadJson(baseTemplateValue);
	    } catch (IOException e) {
		result.addFatalError(this, 12, baseTemplateValue, e.getMessage());
		throw new ShapeChangeAbortException();
	    }

	    if (options.hasParameter(OpenApiDefinition.class.getName(), OpenApiConstants.PARAM_COLLECTIONS)) {
		collections.addAll(options.parameterAsStringList(OpenApiDefinition.class.getName(),
			OpenApiConstants.PARAM_COLLECTIONS, null, true, true));
	    }

	    jsonSchemasBaseLocation = options.parameterAsString(OpenApiDefinition.class.getName(),
		    OpenApiConstants.PARAM_JSON_SCHEMAS_BASE_LOCATION, null, false, true);

	    if (jsonSchemasBaseLocation.contains("\\")) {
		jsonSchemasPathSeparator = "\\";
	    } else {
		jsonSchemasPathSeparator = "/";
	    }

	    if (!jsonSchemasBaseLocation.endsWith(jsonSchemasPathSeparator)) {
		jsonSchemasBaseLocation = jsonSchemasBaseLocation + jsonSchemasPathSeparator;
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
    }

    public static JsonObject loadJson(String uri) throws IOException {

	InputStream stream = null;

	if (uri.startsWith("http")) {
	    URL url = new URL(uri);
	    URLConnection urlConnection = url.openConnection();
	    stream = urlConnection.getInputStream();
	} else {
	    File f = new File(uri);
	    stream = new FileInputStream(f);
	}

	JsonReader reader = Json.createReader(stream);

	JsonObject jsonObj = reader.readObject();
	reader.close();

	return jsonObj;
    }

    public static boolean isEncoded(Info i) {

	if (i.matches("rule-openapi-all-notEncoded") && i.encodingRule("openapi").equalsIgnoreCase("notencoded")) {

	    return false;

	} else {

	    return true;
	}
    }

    public static boolean isJsonEncoded(Info i) {

	if (i.matches("rule-json-all-notEncoded") && i.encodingRule("json").equalsIgnoreCase("notencoded")) {

	    return false;

	} else {

	    return true;
	}
    }

    @Override
    public void process(ClassInfo ci) {

	if (ci == null || ci.pkg() == null) {
	    return;
	}

	if (!isEncoded(ci) || !isJsonEncoded(ci)) {
	    result.addInfo(this, 8, ci.name());
	    return;
	}

	result.addDebug(this, 4, ci.name());

	if (schemaNotEncoded) {
	    result.addInfo(this, 18, schema.name(), ci.name());
	    return;
	}

	if (ci.category() == Options.FEATURE) {

	    if (ci.matches(OpenApiConstants.RULE_ALL_EXPLICIT_COLLECTIONS)) {

		if (collections.contains(ci.name())) {
		    register(ci);
		}
	    } else if (ci.matches(OpenApiConstants.RULE_CLS_INSTANTIABLE_FEATURE_TYPES)) {

		if (!ci.isAbstract()) {
		    register(ci);
		}
	    } else if (ci.matches(OpenApiConstants.RULE_CLS_TOP_LEVEL_FEATURE_TYPES)) {

		// skip feature types with a supertype in the same schema
		if (ci.supertypes().isEmpty()
			|| ci.supertypeClasses().stream().filter(st -> st.inSchema(schema)).findAny().isEmpty()) {
		    register(ci);
		}
	    } else {
		result.addInfo(this, 13, ci.name());
	    }

	} else {
	    /*
	     * logging on debug level, because this target is expected to ignore everything
	     * that is not a feature type
	     */
	    result.addDebug(this, 17, ci.name());
	}
    }

    protected void register(ClassInfo ci) {

	// identify the JSON Schema definition file for the class
	PackageInfo ciSchemaPkg = ci.model().schemaPackage(ci);
	String jsonDirectory = JsonSchemaTarget.identifyJsonDirectory(ciSchemaPkg);

	String fileName = identifyJsonDocumentName(ci.pkg());

	String jsonSchemaDefinitionPath = jsonSchemasBaseLocation + jsonDirectory + jsonSchemasPathSeparator + fileName
		+ "#/definitions/" + ci.name();

	jsonSchemaPathByFeatureType.put(ci, jsonSchemaDefinitionPath);
    }

    private String identifyJsonDocumentName(PackageInfo pi) {

	String s = pi.taggedValue("jsonDocument");
	if (StringUtils.isBlank(s)) {
	    if (pi.isAppSchema()) {
		s = JsonSchemaTarget.normalizedJsonDocumentName(pi);
	    } else {
		// look at owner package
		s = identifyJsonDocumentName(pi.owner());
	    }
	} else {
	    s = s.trim();
	}

	return s;
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

	return Optional.ofNullable(options.targetMapEntry(ci.name(), ci.encodingRule(OpenApiConstants.PLATFORM)));
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

	if (!diagnosticsOnly) {

	    // produce the OpenAPI definition file

	    // core must be defined
	    Optional<ConformanceClass> coreCcOpt = oapiConfig
		    .conformanceClass(OpenApiConstants.CC_OGCAPI_FEATURES_1_1_CORE);
	    JsonObject bbCore = coreCcOpt.get().getOverlay();

	    JsonMergePatch mergePatch1 = Json.createMergePatch(bbCore);
	    JsonValue res1 = mergePatch1.apply(baseTemplate);

	    JsonValue res2 = res1;
	    Optional<ConformanceClass> geojsonCcOpt = oapiConfig
		    .conformanceClass(OpenApiConstants.CC_OGCAPI_FEATURES_1_1_GEOJSON);
	    boolean isGeojsonApplicable = geojsonCcOpt.isPresent();

	    if (isGeojsonApplicable) {
		JsonObject bbGeoJson = geojsonCcOpt.get().getOverlay();
		JsonMergePatch mergePatchGeoJson = Json.createMergePatch(bbGeoJson);
		res2 = mergePatchGeoJson.apply(res1);
	    }

	    JsonObject res3 = (JsonObject) res2;
	    Optional<ConformanceClass> htmlCcOpt = oapiConfig
		    .conformanceClass(OpenApiConstants.CC_OGCAPI_FEATURES_1_1_HTML);
	    boolean isHtmlApplicable = htmlCcOpt.isPresent();

	    if (isHtmlApplicable) {
		JsonObject bbHtml = htmlCcOpt.get().getOverlay();
		JsonMergePatch mergePatchHtml = Json.createMergePatch(bbHtml);
		res3 = (JsonObject) mergePatchHtml.apply(res2);
	    }

	    JsonObject res4 = (JsonObject) res3;
	    Optional<ConformanceClass> crsCcOpt = oapiConfig
		    .conformanceClass(OpenApiConstants.CC_OGCAPI_FEATURES_2_1_CRS);
	    boolean isCrsApplicable = crsCcOpt.isPresent();

	    if (isCrsApplicable) {

		ConformanceClass crsCc = crsCcOpt.get();

		// copy query parameters existing in template into CRS overlay before merge
		JsonObject crs_queryParametersMerged = mergeQueryParameters(res3, crsCc.getOverlay());

		JsonMergePatch mergePatchCrs = Json.createMergePatch(crs_queryParametersMerged);
		res4 = (JsonObject) mergePatchCrs.apply(res3);

		if (crsCc.getParam().isPresent()) {
		    List<String> supportedCrs = Arrays.asList(StringUtils.split(crsCc.getParam().get()));

		    JsonObject supportedCrsOverlay = Json.createObjectBuilder()
			    .add("components", Json.createObjectBuilder().add("parameters", Json.createObjectBuilder()
				    .add("crs", Json.createObjectBuilder().add("schema",
					    Json.createObjectBuilder().add("enum",
						    Json.createArrayBuilder(supportedCrs))))
				    .add("bbox-crs", Json.createObjectBuilder().add("schema", Json.createObjectBuilder()
					    .add("enum", Json.createArrayBuilder(supportedCrs))))))
			    .build();

		    JsonMergePatch mergePatchSupportedCrs = Json.createMergePatch(supportedCrsOverlay);
		    res4 = (JsonObject) mergePatchSupportedCrs.apply(res4);
		}
	    }

	    // now apply all query parameter overlays defined for the
	    // pre-feature-identification phase
	    List<QueryParameter> queryParametersPreFeatureIdent = oapiConfig.getQueryParameters().stream()
		    .filter(qp -> qp.getPhase() == OpenApiDefinitionProcessingPhase.PRE_FEATURE_IDENTIFICATION)
		    .collect(Collectors.toList());

	    for (QueryParameter qp : queryParametersPreFeatureIdent) {

		JsonObject qpOverlay = qp.getOverlay();

		// copy query parameters existing in template into next overlay before merge
		JsonObject queryParametersMerged = mergeQueryParameters(res4, qpOverlay);

		JsonMergePatch mergePatchQueryParameterOverlay = Json.createMergePatch(queryParametersMerged);
		res4 = (JsonObject) mergePatchQueryParameterOverlay.apply(res4);
	    }

	    /*
	     * now we need to update the merge result for each feature type (as defined in
	     * section 9.3.1.9)
	     */
	    JsonObject featureTypesMerged = featureTypeUpdates(res4, isGeojsonApplicable);

	    JsonObject removeGenericsOverlay = Json.createObjectBuilder()
		    .add("paths",
			    Json.createObjectBuilder().add("/collections/{collectionId}", JsonValue.NULL)
				    .add("/collections/{collectionId}/items", JsonValue.NULL)
				    .add("/collections/{collectionId}/items/{featureId}", JsonValue.NULL))
		    .add("components",
			    Json.createObjectBuilder()
				    .add("parameters", Json.createObjectBuilder().add("collectionId", JsonValue.NULL))
				    .add("schemas",
					    Json.createObjectBuilder().add("Features", JsonValue.NULL).add("Feature",
						    JsonValue.NULL))
				    .add("responses", Json.createObjectBuilder().add("Features", JsonValue.NULL)
					    .add("Feature", JsonValue.NULL)))
		    .build();

	    JsonMergePatch mergePatchRemoveGenerics = Json.createMergePatch(removeGenericsOverlay);
	    JsonObject res5 = (JsonObject) mergePatchRemoveGenerics.apply(featureTypesMerged);

	    // now apply all query parameter overlays defined for the
	    // finalization phase
	    List<QueryParameter> queryParametersFinalization = oapiConfig.getQueryParameters().stream()
		    .filter(qp -> qp.getPhase() == OpenApiDefinitionProcessingPhase.FINALIZATION)
		    .collect(Collectors.toList());

	    for (QueryParameter qp : queryParametersFinalization) {

		JsonObject qpOverlay = qp.getOverlay();

		// copy query parameters existing in template into next overlay before merge
		JsonObject queryParametersMerged = mergeQueryParameters(res5, qpOverlay);

		JsonMergePatch mergePatchQueryParameterOverlay = Json.createMergePatch(queryParametersMerged);
		res5 = (JsonObject) mergePatchQueryParameterOverlay.apply(res5);
	    }

	    // ======
	    // Now write the OpenAPI definition to file
	    // ======

	    Map<String, Boolean> jsonWriterConfig = Map.of(JsonGenerator.PRETTY_PRINTING, Boolean.TRUE);
	    JsonWriterFactory factory = Json.createWriterFactory(jsonWriterConfig);

	    File outputFile = new File(outputDirectory, outputFilename);

	    try (JsonWriter writer = factory.createWriter(
		    new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8")))) {
		writer.write(res5);
		result.addResult(this.getTargetName(), outputFile.getParent(), outputFile.getName(), null);
	    } catch (Exception e) {
		result.addError(this, 100, outputFile.getAbsolutePath(), e.getMessage());
		e.printStackTrace();
	    }
	}
    }

    private JsonObject featureTypeUpdates(JsonObject source, boolean isGeojsonApplicable) {

	JsonObject overlay = Json.createObjectBuilder().add("paths", JsonValue.EMPTY_JSON_OBJECT)
		.add("components", Json.createObjectBuilder().add("responses", JsonValue.EMPTY_JSON_OBJECT)
			.add("schemas", JsonValue.EMPTY_JSON_OBJECT))
		.build();

	for (Entry<ClassInfo, String> entry : jsonSchemaPathByFeatureType.entrySet()) {

	    ClassInfo ftCi = entry.getKey();
	    String featureType = ftCi.name();
	    String jsonSchemaPath = entry.getValue();

	    /*
	     * Create a copy of the path "/collections/{collectionId}" and replace all
	     * occurrences of "{collectionId}" with the feature type identifier.
	     */
	    String collectionsCollectionIdTemplatePath = "/paths/~1collections~1{collectionId}";
	    JsonPointer jp1 = Json.createPointer(collectionsCollectionIdTemplatePath);
	    JsonObject collectionsCollectionIdTemplate = (JsonObject) jp1.getValue(source);

	    JsonObject modified1 = (JsonObject) replaceParameter(collectionsCollectionIdTemplate, "\\{collectionId\\}",
		    featureType);

	    String collectionsFeatureTypeBaseJsonPointer = "/paths/~1collections~1" + featureType;

	    JsonPointer jp1add = Json.createPointer(collectionsFeatureTypeBaseJsonPointer);
	    overlay = jp1add.add(overlay, modified1);

	    // remove parameter "$ref": "#/components/parameters/collectionId"
	    overlay = removeCollectionIdParameter(overlay, collectionsFeatureTypeBaseJsonPointer);

	    /*
	     * Create a copy of the path "/collections/{collectionId}/items", change the
	     * "200"-response to { "$ref": "#/components/responses/Features_{collectionId}"
	     * } and replace all occurrences of "{collectionId}" with the feature type
	     * identifier.
	     */

	    String collectionsCollectionIdItemsTemplatePath = collectionsCollectionIdTemplatePath + "~1items";
	    JsonPointer jp2 = Json.createPointer(collectionsCollectionIdItemsTemplatePath);
	    JsonObject collectionsCollectionIdItemsTemplate = (JsonObject) jp2.getValue(source);

	    // update 200 response
	    JsonPointer jpUpdate200Response = Json.createPointer("/get/responses/200/$ref");
	    collectionsCollectionIdItemsTemplate = jpUpdate200Response.replace(collectionsCollectionIdItemsTemplate,
		    Json.createValue("#/components/responses/Features_{collectionId}"));

	    JsonObject modified2 = (JsonObject) replaceParameter(collectionsCollectionIdItemsTemplate,
		    "\\{collectionId\\}", featureType);

	    String collectionsFeatureTypeItemsBaseJsonPointer = "/paths/~1collections~1" + featureType + "~1items";
	    JsonPointer jp2add = Json.createPointer(collectionsFeatureTypeItemsBaseJsonPointer);
	    overlay = jp2add.add(overlay, modified2);

	    // remove parameter "$ref": "#/components/parameters/collectionId"
	    overlay = removeCollectionIdParameter(overlay, collectionsFeatureTypeItemsBaseJsonPointer);

	    /*
	     * Create a copy of the path "/collections/{collectionId}/items/{featureId}",
	     * change the "200"-response to { "$ref":
	     * "#/components/responses/Feature_{collectionId}" } and replace all occurrences
	     * of "{collectionId}" with the feature type identifier.
	     */

	    String collectionsCollectionIdItemsFeatureIdTemplatePath = collectionsCollectionIdItemsTemplatePath
		    + "~1{featureId}";
	    JsonPointer jp3 = Json.createPointer(collectionsCollectionIdItemsFeatureIdTemplatePath);
	    JsonObject collectionsCollectionIdItemsFeatureIdTemplate = (JsonObject) jp3.getValue(source);

	    // update 200 response
	    JsonPointer jp2Update200Response = Json.createPointer("/get/responses/200/$ref");
	    collectionsCollectionIdItemsFeatureIdTemplate = jp2Update200Response.replace(
		    collectionsCollectionIdItemsFeatureIdTemplate,
		    Json.createValue("#/components/responses/Feature_{collectionId}"));

	    JsonObject modified3 = (JsonObject) replaceParameter(collectionsCollectionIdItemsFeatureIdTemplate,
		    "\\{collectionId\\}", featureType);

	    String collectionsFeatureTypeItemsFeatureIdBaseJsonPointer = "/paths/~1collections~1" + featureType
		    + "~1items~1{featureId}";
	    JsonPointer jp3add = Json.createPointer(collectionsFeatureTypeItemsFeatureIdBaseJsonPointer);
	    overlay = jp3add.add(overlay, modified3);

	    // remove parameter "$ref": "#/components/parameters/collectionId"
	    overlay = removeCollectionIdParameter(overlay, collectionsFeatureTypeItemsFeatureIdBaseJsonPointer);

	    {/*
	      * Create a copy of the response "Features" and rename it to
	      * "Features_{collectionId}", where "{collectionId}" is replaced with the
	      * feature type identifier.
	      */
		String responsesFeaturesTemplatePath = "/components/responses/Features";
		JsonPointer jp4 = Json.createPointer(responsesFeaturesTemplatePath);
		JsonObject responsesFeaturesTemplate = (JsonObject) jp4.getValue(source);

		if (isGeojsonApplicable) {

		    /*
		     * Change the schema of the "application/geo+json" response to { "$ref":
		     * "#/components/schemas/Features_{collectionId}" } after replacing
		     * "{collectionId}" with the feature type identifier.
		     */
		    JsonPointer jpUpdateGeojsonResponse = Json
			    .createPointer("/content/application~1geo+json/schema/$ref");
		    responsesFeaturesTemplate = jpUpdateGeojsonResponse.replace(responsesFeaturesTemplate,
			    Json.createValue("#/components/schemas/Features_" + featureType));
		}

		JsonPointer jp4add = Json.createPointer("/components/responses/Features_" + featureType);
		overlay = jp4add.add(overlay, responsesFeaturesTemplate);

		if (isGeojsonApplicable) {
		    /*
		     * Create a copy of the schema "Features" and rename it to
		     * "Features_{collectionId}", where "{collectionId}" is replaced with the
		     * feature type identifier. Change value of "items" in the "features" property
		     * to { "$ref": "#/components/schemas/Feature_{collectionId}" } after replacing
		     * "{collectionId}" with the feature type identifier.
		     */
		    String schemasFeaturesTemplatePath = "/components/schemas/Features";
		    JsonPointer jpSchemasFeatures = Json.createPointer(schemasFeaturesTemplatePath);
		    JsonObject schemasFeaturesTemplate = (JsonObject) jpSchemasFeatures.getValue(source);

		    // update properties/features/items
		    JsonPointer jpUpdateFeaturesItems = Json.createPointer("/properties/features/items/$ref");
		    schemasFeaturesTemplate = jpUpdateFeaturesItems.replace(schemasFeaturesTemplate,
			    Json.createValue("#/components/schemas/Feature_" + featureType));

		    JsonPointer jpSchemasFeaturesAdd = Json
			    .createPointer("/components/schemas/Features_" + featureType);
		    overlay = jpSchemasFeaturesAdd.add(overlay, schemasFeaturesTemplate);
		}
	    }
	    { /*
	       * Create a copy of the response "Feature" and rename it to
	       * "Feature_{collectionId}", where "{collectionId}" is replaced with the feature
	       * type identifier.
	       */
		String responsesFeatureTemplatePath = "/components/responses/Feature";
		JsonPointer jp5 = Json.createPointer(responsesFeatureTemplatePath);
		JsonObject responsesFeatureTemplate = (JsonObject) jp5.getValue(source);

		responsesFeatureTemplate = (JsonObject) replaceParameter(responsesFeatureTemplate, "\\{collectionId\\}",
			featureType);

		if (isGeojsonApplicable) {

		    /*
		     * Change the schema of the "application/geo+json" response to { "$ref":
		     * "#/components/schemas/Feature_{collectionId}" } after replacing
		     * "{collectionId}" with the feature type identifier.
		     */
		    JsonPointer jpUpdateGeojsonResponse = Json
			    .createPointer("/content/application~1geo+json/schema/$ref");
		    responsesFeatureTemplate = jpUpdateGeojsonResponse.replace(responsesFeatureTemplate,
			    Json.createValue("#/components/schemas/Feature_" + featureType));
		}

		JsonPointer jp5add = Json.createPointer("/components/responses/Feature_" + featureType);
		overlay = jp5add.add(overlay, responsesFeatureTemplate);

		if (isGeojsonApplicable) {

		    /*
		     * Create a new schema "Feature_{collectionId}", where "{collectionId}" is
		     * replaced with the feature type identifier, referencing the schema created by
		     * the JSON Schema target for the feature type.
		     */
		    JsonObject featureSchemaObject = Json.createObjectBuilder()
			    .add("$ref", Json.createValue(jsonSchemaPath)).build();

		    JsonPointer jpSchemasFeatureAdd = Json.createPointer("/components/schemas/Feature_" + featureType);
		    overlay = jpSchemasFeatureAdd.add(overlay, featureSchemaObject);
		}
	    }
	}

	JsonMergePatch mergePatch = Json.createMergePatch(overlay);
	JsonObject result = (JsonObject) mergePatch.apply(source);

	return result;
    }

    private JsonObject removeCollectionIdParameter(JsonObject overlay, String baseJsonPointerPath) {

	JsonObject result = overlay;

	JsonPointer basePathPointer = Json.createPointer(baseJsonPointerPath);

	JsonObject pathObject = (JsonObject) basePathPointer.getValue(overlay);
	Set<String> httpMethods = pathObject.keySet();
	for (String httpMethod : httpMethods) {
	    String parametersBaseJsonPointerPath = baseJsonPointerPath + "/" + httpMethod + "/parameters";
	    JsonPointer jpParameters = Json.createPointer(parametersBaseJsonPointerPath);
	    try {
		JsonStructure tmp = (JsonStructure) jpParameters.getValue(overlay);
		if (tmp instanceof JsonArray) {
		    JsonArray array = (JsonArray) tmp;
		    for (int i = 0; i < array.size(); i++) {
			JsonObject p = (JsonObject) array.get(i);
			if (p.containsValue(Json.createValue("#/components/parameters/collectionId"))) {
			    JsonPointer jpRemoveCollIdParam = Json
				    .createPointer(parametersBaseJsonPointerPath + "/" + i);
			    result = jpRemoveCollIdParam.remove(overlay);
			    break;
			}
		    }
		}
	    } catch (JsonException e) {
		// parameters does not exist in overlay, so nothing to do
	    }
	}

	return result;
    }

    private JsonValue replaceParameter(JsonValue jv, String parameter, String replacement) {

	JsonValue result;

	if (jv instanceof JsonObject) {

	    JsonObject jvObj = (JsonObject) jv;

	    JsonObjectBuilder builder = Json.createObjectBuilder();

	    for (Entry<String, JsonValue> e : jvObj.entrySet()) {
		builder.add(e.getKey().replaceAll(parameter, replacement),
			replaceParameter(e.getValue(), parameter, replacement));
	    }

	    result = builder.build();

	} else if (jv instanceof JsonArray) {

	    JsonArray jvArr = (JsonArray) jv;

	    JsonArrayBuilder builder = Json.createArrayBuilder();

	    for (JsonValue arrValue : jvArr) {
		builder.add(replaceParameter(arrValue, parameter, replacement));
	    }

	    result = builder.build();

	} else if (jv instanceof JsonString) {

	    JsonString jvString = (JsonString) jv;

	    result = Json.createValue(jvString.getString().replaceAll(parameter, replacement));

	} else {
	    result = jv;
	}

	return result;
    }

    private JsonObject mergeQueryParameters(JsonObject source, JsonObject target) {

	JsonObject paths = source.getJsonObject("paths");

	if (paths == null)
	    return target;

	Set<String> pathsProps = paths.keySet();

	if (pathsProps.isEmpty())
	    return target;

	JsonObject result = target;

	for (String pathsProp : pathsProps) {

	    JsonObject propObj = paths.getJsonObject(pathsProp);

	    // TBD: Is case for http method property relevant? -> no, must always be
	    // lower-case

	    for (String httpMethodProp : propObj.keySet()) {

		JsonObject httpMethodObj = propObj.getJsonObject(httpMethodProp);

		JsonArray parametersArray = httpMethodObj.getJsonArray("parameters");

		if (parametersArray != null) {

		    String parametersPath = "/paths/" + pathsProp.replace("~", "~0").replace("/", "~1") + "/"
			    + httpMethodProp + "/parameters";

		    JsonPointer jp1 = Json.createPointer(parametersPath);

		    try {
			// TBD: will throw a JsonException if the path (maybe except the last element)
			// does not exist in the target - strange
			if (jp1.containsValue(result)) {

			    /*
			     * we need to add the elements from the array in reverse order, to counter the
			     * effect of adding each array value individually (which reverses the order)
			     */
			    List<JsonValue> paramArrayReverseOrder = new ArrayList<>(parametersArray);
			    Collections.reverse(paramArrayReverseOrder);

			    JsonPointer jp2 = Json.createPointer(parametersPath + "/0");

			    for (JsonValue arrayVal : paramArrayReverseOrder) {
				result = jp2.add(result, arrayVal);
			    }
			}
		    } catch (JsonException je) {
			//
		    }
		}
	    }
	}

	return result;
    }

    @Override
    public void reset() {

	model = null;
	initialised = false;
	diagnosticsOnly = false;
	numberOfEncodedSchemas = 0;

	outputDirectory = null;
	outputFilename = null;

	oapiConfig = null;

	baseTemplate = null;
	jsonSchemasBaseLocation = null;
	jsonSchemasPathSeparator = null;

	collections = new TreeSet<>();
	jsonSchemaPathByFeatureType = new TreeMap<>();
    }

    @Override
    public void registerRulesAndRequirements(RuleRegistry r) {

	/*
	 * OpenAPI encoding rules
	 */

	r.addRule("rule-openapi-all-explicit-collections");
	r.addRule("rule-openapi-all-notEncoded");
	r.addRule("rule-openapi-cls-instantiable-feature-types");
	r.addRule("rule-openapi-cls-top-level-feature-types");

	r.addExtendsEncRule("openapi", "*");
    }

    @Override
    public String getDefaultEncodingRule() {
	return "openapi";
    }

    @Override
    public String getTargetName() {
	return "OpenAPI Definition";
    }

    @Override
    public String getTargetIdentifier() {
	return "openapi";
    }

    @Override
    public String message(int mnr) {

	switch (mnr) {

	case 0:
	    return "Context: class '$1$'";
	case 1:
	    return "Context: property '$1$'";

	case 3:
	    return "Context: class OpenAPI target";
	case 4:
	    return "Processing class '$1$'.";
	case 5:
	    return "Directory named '$1$' does not exist or is not accessible.";
	case 6:
	    return "System error: Exception raised '$1$'. '$2$'";
	case 7:
	    return "Schema '$1$' is not encoded (via the applicable OpenAPI or the JSON Schema encoding rule).";
	case 8:
	    return "Class '$1$' is not encoded (via the applicable OpenAPI or the JSON Schema encoding rule).";
	case 9:
	    return "";

	case 10:
	    return "Configuration parameter '$1$' has invalid value '$2$'. Using value '$3$' instead.";
	case 11:
	    return "The OpenApiConfigItems element, a required item within the advancedProcessConfigurations element of the OpenApiDefinition "
		    + "target configuration, could not be loaded. Consult the log file for further details.";
	case 12:
	    return "Target parameter 'baseTemplate' has value: $1$. Could not load JSON from that location. Exception message is: $2$";
	case 13:
	    return "Feature type '$1$' is not encoded, because it matches none of the feature type conversion rules defined for the OpenAPI definition target.";
	case 15:
	    return "";
	case 17:
	    return "Type '$1$' is of a category not enabled for conversion, meaning that the OpenAPI definition will not represent it.";
	case 18:
	    return "Schema '$1$' is not encoded. Thus class '$2$' (which belongs to that schema) is not encoded either.";

	case 100:
	    return "Exception occurred while writing OpenAPI definition to file: $1$. Exception message is: $2$.";

	case 503:
	    return "Output file '$1$' already exists in output directory ('$2$'). It will be deleted prior to processing.";
	case 504:
	    return "File has been deleted.";

	case 10001:
	    return "Generating OpenAPI definition for application schema $1$.";
	case 10002:
	    return "Diagnostics-only mode. All output to files is suppressed.";
	default:
	    return "(" + OpenApiDefinition.class.getName() + ") Unknown message with number: " + mnr;
	}
    }
}
