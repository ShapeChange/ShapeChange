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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import de.interactive_instruments.ShapeChange.MapEntryParamInfos;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.ShapeChangeException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.TargetOutputProcessor;
import de.interactive_instruments.ShapeChange.Target.JSON.json.JsonBoolean;
import de.interactive_instruments.ShapeChange.Target.JSON.json.JsonInteger;
import de.interactive_instruments.ShapeChange.Target.JSON.json.JsonNumber;
import de.interactive_instruments.ShapeChange.Target.JSON.json.JsonString;
import de.interactive_instruments.ShapeChange.Target.JSON.json.JsonValue;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.AdditionalPropertiesKeyword;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.FormatKeyword;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.GenericAnnotationKeyword;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.JsonSchema;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.JsonSchemaType;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.JsonSchemaVersion;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.JsonSerializationContext;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.MaxPropertiesKeyword;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.MinPropertiesKeyword;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.PropertiesKeyword;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.RequiredKeyword;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.TypeKeyword;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.XOgcCollectionIdKeyword;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.XOgcRoleKeyword;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.XOgcUriTemplateKeyword;
import de.interactive_instruments.ShapeChange.Util.ValueTypeOptions;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class JsonSchemaDocument implements MessageSource {

    protected enum PrimaryTimeOptionality {
	UNDEFINED, OPTIONAL, REQUIRED
    };

    protected Options options;
    protected ShapeChangeResult result;
    protected Model model;
    protected JsonSchemaTarget jsonSchemaTarget;
    protected String docName;
    /**
     * can be <code>null</code>
     */
    protected PackageInfo representedPackage;
    protected String schemaId;
    protected File jsonSchemaOutputFile;
    protected MapEntryParamInfos mapEntryParamInfos;
    protected JsonSchemaVersion jsonSchemaVersion;
    protected boolean isSingularCollectionsSchema;

    protected SortedMap<String, ClassInfo> classesByName = new TreeMap<>();
    protected SortedMap<ClassInfo, JsonSchema> defSchemaByClass = new TreeMap<>();
    protected SortedMap<ClassInfo, JsonSchema> propertyDefSchemaByClass = new TreeMap<>();
    protected SortedMap<ClassInfo, JsonSchema> propertyDefSchemaActualPropertiesSchemaByClass = new TreeMap<>();
    protected SortedMap<ClassInfo, JsonSchema> allOfSchemaByClass = new TreeMap<>();
    protected Map<ClassInfo, JsonSchemaTypeInfo> basicTypeInfoByClass = new HashMap<>();

    protected JsonSchema rootSchema = new JsonSchema();

    protected AnnotationGenerator annotationGenerator;

    protected boolean generateSCLinkObjectDefinition = false;

    public JsonSchemaDocument(PackageInfo representedPackage, Model model, Options options, ShapeChangeResult result,
	    JsonSchemaTarget jsonSchemaTarget, String schemaId, File jsonSchemaOutputFile,
	    MapEntryParamInfos mapEntryParamInfos, boolean isSingularCollectionsSchema) {

	this.representedPackage = representedPackage;
	this.options = options;
	this.result = result;
	this.model = model;
	this.jsonSchemaTarget = jsonSchemaTarget;
	this.schemaId = schemaId;
	this.jsonSchemaOutputFile = jsonSchemaOutputFile;
	this.mapEntryParamInfos = mapEntryParamInfos;
	this.isSingularCollectionsSchema = isSingularCollectionsSchema;

	if (options.getCurrentProcessConfig().parameterAsString(TargetOutputProcessor.PARAM_ADD_COMMENT, null, false,
		true) == null) {
	    rootSchema.comment("JSON Schema document created by ShapeChange - https://shapechange.net/");
	}

	jsonSchemaVersion = jsonSchemaTarget.getJsonSchemaVersion();

	// add JSON Schema version, if a URI is defined for the schema version
	if (jsonSchemaVersion.getSchemaUri().isPresent()) {
	    rootSchema.schema(jsonSchemaVersion.getSchemaUri().get());
	}

	// add schema identifier
	rootSchema.id(schemaId);

	this.annotationGenerator = new AnnotationGenerator(this.jsonSchemaTarget.getAnnotationElements(),
		options.language(), result);

	if (!isSingularCollectionsSchema && representedPackage != null
		&& representedPackage.matches(JsonSchemaConstants.RULE_ALL_DOCUMENTATION)) {
	    this.annotationGenerator.applyAnnotations(rootSchema, representedPackage);
	}
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
     * Retrieve the "$id" of this JSON Schema document.
     * 
     * @return the "$id" of the schema
     */
    public String getSchemaId() {
	return this.schemaId;
    }

    public JsonSchemaVersion getSchemaVersion() {
	return this.jsonSchemaVersion;
    }

    public void createDefinitions() {

	/*
	 * Generate the definitions schema
	 */
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

	    addToRootSchema(ci.name(), js);

	    if (jsonSchemaTarget.createSeparatePropertyDefinitions && propertyDefSchemaByClass.containsKey(ci)) {
		addToRootSchema(ci.name() + "_Properties", propertyDefSchemaByClass.get(ci));
	    }

	    defSchemaByClass.put(ci, js);
	}
    }

    private void addToRootSchema(String defName, JsonSchema js) {
	if (jsonSchemaVersion == JsonSchemaVersion.DRAFT_07 || jsonSchemaVersion == JsonSchemaVersion.OPENAPI_30) {
	    rootSchema.definition(defName, js);
	} else {
	    rootSchema.def(defName, js);
	}
    }

    public void computeEncodingInfos() {

	for (ClassInfo ci : this.classesByName.values()) {

	    /*
	     * entity type member infos have already been computed.
	     * 
	     * id member infos may need to be computed. Thus far, the encoding infos for a
	     * class only contain information about the id member if such a member has been
	     * generated for that class, and in the JSON Schema of that class. Subtypes need
	     * to get this information as well. Also, id member infos from relevant external
	     * schemas (base schema or mapping of a supertype) need to be taken into
	     * account.
	     * 
	     * Note that restrictions of the entity type and id members are not handled in
	     * this processing step.
	     */

	    EncodingInfos encInfo = JsonSchemaTarget.encodingInfosByCi.get(ci);

	    // can be null, for example for enumerations and code lists - ignore those
	    if (encInfo != null) {

		// also ignore cases where the id member path already exists
		if (encInfo.getIdMemberPath().isEmpty()) {

		    // look up information in all supertypes
		    EncodingInfos tmpEncInfo = identifyIdMemberInfosFromSupertypesAndBaseSchema(ci);

		    if (tmpEncInfo != null) {

			encInfo.setIdMemberPath(tmpEncInfo.getIdMemberPath().get());
			if (tmpEncInfo.getIdMemberRequired().isPresent()) {
			    encInfo.setIdMemberRequired(tmpEncInfo.getIdMemberRequired().get());
			}
			encInfo.setIdMemberFormats(tmpEncInfo.getIdMemberFormats());
			encInfo.setIdMemberTypes(tmpEncInfo.getIdMemberTypes());
		    }
		}
	    }
	}
    }

    public void applyMemberRestrictions() {

	Optional<EncodingRestrictions> encRestrictsOpt = JsonSchemaTarget.idMemberEncodingRestrictions;

	for (ClassInfo ci : this.classesByName.values()) {

	    EncodingInfos encInfo = JsonSchemaTarget.encodingInfosByCi.get(ci);

	    // can be null, for example for enumerations and code lists - ignore those
	    if (encInfo != null) {

		EncodingInfos extEncInfos = directExternalSchemaEncodingInfos(ci);

		/*
		 * only if encoding infos from an external schema directly apply to the JSON
		 * Schema definition of ci are restrictions handled in this processing step
		 */
		if (extEncInfos != null) {

		    /*
		     * So there are some encoding infos from one or more external schemas that
		     * directly apply to ci - via base class for ci, via mapping for a supertype of
		     * ci, or both.
		     */

		    boolean entityTypeMemberAlreadyPresentAndRequired = encInfo.getEntityTypeMemberPath().isPresent()
			    && encInfo.getEntityTypeMemberRequired().isPresent()
			    && encInfo.getEntityTypeMemberRequired().get();
		    boolean extEntityTypeMemberNotRequired = extEncInfos.getEntityTypeMemberRequired().isEmpty()
			    || !extEncInfos.getEntityTypeMemberRequired().get();

		    boolean createEntityTypeMemberRestriction = ci
			    .matches(JsonSchemaConstants.RULE_CLS_RESTRICT_EXT_ENTITY_TYPE_MEMBER)
			    && extEncInfos.getEntityTypeMemberPath().isPresent()
			    && !entityTypeMemberAlreadyPresentAndRequired && extEntityTypeMemberNotRequired;

		    boolean idMemberAlreadyPresentAndRequired = encInfo.getIdMemberPath().isPresent()
			    && encInfo.getIdMemberRequired().isPresent() && encInfo.getIdMemberRequired().get();
		    boolean extIdMemberNotRequired = extEncInfos.getIdMemberRequired().isEmpty()
			    || !extEncInfos.getIdMemberRequired().get();
		    boolean createIdMemberRestriction_required = encRestrictsOpt.isPresent()
			    && ci.matches(JsonSchemaConstants.RULE_CLS_RESTRICT_EXT_ID_MEMBER)
			    && extEncInfos.getIdMemberPath().isPresent() && encRestrictsOpt.get().isMemberRequired()
			    && !idMemberAlreadyPresentAndRequired && extIdMemberNotRequired;

		    /*
		     * We need to identify the type restrictions that would need to be set, and
		     * avoid superfluous restrictions.
		     */
		    final SortedSet<String> newTypeRestrictions = new TreeSet<>();
		    if (encRestrictsOpt.isPresent()) {
			for (String s : encRestrictsOpt.get().getMemberTypeRestrictions()) {
			    if ((extEncInfos.getIdMemberTypes().isEmpty() || extEncInfos.getIdMemberTypes().contains(s))
				    && (encInfo.getIdMemberTypes().isEmpty()
					    || encInfo.getIdMemberTypes().contains(s))) {
				newTypeRestrictions.add(s);
			    }
			}
		    }
		    if (newTypeRestrictions.equals(extEncInfos.getIdMemberTypes())
			    || newTypeRestrictions.equals(encInfo.getIdMemberTypes())) {
			newTypeRestrictions.clear();
		    }

		    boolean createIdMemberRestriction_types = encRestrictsOpt.isPresent()
			    && ci.matches(JsonSchemaConstants.RULE_CLS_RESTRICT_EXT_ID_MEMBER)
			    && extEncInfos.getIdMemberPath().isPresent() && !newTypeRestrictions.isEmpty();

		    SortedSet<String> establishedFormatRestrictions = new TreeSet<>();
		    establishedFormatRestrictions.addAll(encInfo.getIdMemberFormats());
		    establishedFormatRestrictions.addAll(extEncInfos.getIdMemberFormats());
		    SortedSet<String> newFormatRestrictions = new TreeSet<>();
		    if (encRestrictsOpt.isPresent()) {
			newFormatRestrictions.addAll(encRestrictsOpt.get().getMemberFormatRestrictions());
		    }
		    newFormatRestrictions.removeAll(establishedFormatRestrictions);

		    boolean createIdMemberRestriction_formats = encRestrictsOpt.isPresent()
			    && ci.matches(JsonSchemaConstants.RULE_CLS_RESTRICT_EXT_ID_MEMBER)
			    && extEncInfos.getIdMemberPath().isPresent() && !newFormatRestrictions.isEmpty();

		    if (createEntityTypeMemberRestriction || createIdMemberRestriction_required
			    || createIdMemberRestriction_types || createIdMemberRestriction_formats) {

			JsonSchema ciJs = this.defSchemaByClass.get(ci);

			if (ciJs.allOf().isEmpty()) {

			    /*
			     * The definitions schema thus far does not contain an allOf member. Now it gets
			     * tricky: We need to modify the definitions schema in a way that leaves the
			     * keywords relevant for the definitions schema ($anchor, $id, other generated
			     * 'annotations') on the definitions schema level, and pushes the keywords that
			     * define the structure to a new JSON Schema that is placed beneath a new allOf
			     * keyword. The latter should encompass keywords 'type', 'properties',
			     * 'required', 'additionalProperties', 'minProperties', 'maxProperties' (for
			     * each, if it exists in the original definitions schema). If 'type' is anything
			     * other than 'object', that would not make sense. However, that should not
			     * happen. Encoding infos are only created for feature, object, mixin, and data
			     * types - as well as for unions with property choice encoding. All of them have
			     * type=object.
			     */

			    JsonSchema newCiJs = new JsonSchema();
			    ciJs.allOf(newCiJs);

			    Optional<TypeKeyword> typeKeywordOpt = ciJs.removeTypeKeyword();
			    if (typeKeywordOpt.isPresent()) {
				newCiJs.type(
					typeKeywordOpt.get().toArray(new JsonSchemaType[typeKeywordOpt.get().size()]));
			    }

			    Optional<PropertiesKeyword> propertiesKeywordOpt = ciJs.removePropertiesKeyword();
			    if (propertiesKeywordOpt.isPresent()) {
				PropertiesKeyword propertiesKeyword = propertiesKeywordOpt.get();
				for (Entry<String, JsonSchema> entry : propertiesKeyword.entrySet()) {
				    newCiJs.property(entry.getKey(), entry.getValue());
				}
			    }

			    Optional<RequiredKeyword> reqKeywordOpt = ciJs.removeRequiredKeyword();
			    if (reqKeywordOpt.isPresent()) {
				newCiJs.required(reqKeywordOpt.get().toArray(new String[reqKeywordOpt.get().size()]));
			    }

			    Optional<AdditionalPropertiesKeyword> additionalPropertiesKeywordOpt = ciJs
				    .removeAdditionalPropertiesKeyword();
			    if (additionalPropertiesKeywordOpt.isPresent()) {
				newCiJs.additionalProperties(additionalPropertiesKeywordOpt.get().value());
			    }

			    Optional<MinPropertiesKeyword> minPropertiesKeywordOpt = ciJs.removeMinPropertiesKeyword();
			    if (minPropertiesKeywordOpt.isPresent()) {
				newCiJs.minProperties(minPropertiesKeywordOpt.get().value());
			    }

			    Optional<MaxPropertiesKeyword> maxPropertiesKeywordOpt = ciJs.removeMaxPropertiesKeyword();
			    if (maxPropertiesKeywordOpt.isPresent()) {
				newCiJs.maxProperties(maxPropertiesKeywordOpt.get().value());
			    }
			}

			/*
			 * Create another schema with the necessary restrictions.
			 */
			JsonSchema restrictionSchema = new JsonSchema();
			ciJs.allOf(restrictionSchema);

//			restrictionSchema.comment("Restriction(s) of entity type and / or id member");

			if (createEntityTypeMemberRestriction) {
			    String[] etmPathComponents = extEncInfos.getEntityTypeMemberPath().get().split("/");
			    String etPropName = etmPathComponents[etmPathComponents.length - 1];
			    JsonSchema schemaForRestriction = getOrCreatePropertyPath(restrictionSchema,
				    Arrays.copyOfRange(etmPathComponents, 0, etmPathComponents.length - 1), true);
			    schemaForRestriction.required(etPropName);
			    // update encoding infos
			    encInfo.setEntityTypeMemberRequired(true);
			    encodingInfosOfSubtypesInCompleteHierarchy(ci).stream()
				    .forEach(ei -> ei.setEntityTypeMemberRequired(true));
			}

			if (createIdMemberRestriction_required || createIdMemberRestriction_types
				|| createIdMemberRestriction_formats) {

			    String[] idmPathComponents = extEncInfos.getIdMemberPath().get().split("/");
			    String idPropName = idmPathComponents[idmPathComponents.length - 1];

			    if (createIdMemberRestriction_required) {
				JsonSchema schemaForRestriction = getOrCreatePropertyPath(restrictionSchema,
					Arrays.copyOfRange(idmPathComponents, 0, idmPathComponents.length - 1), true);
				schemaForRestriction.required(idPropName);
				// update encoding infos
				encInfo.setIdMemberRequired(true);
				encodingInfosOfSubtypesInCompleteHierarchy(ci).stream()
					.forEach(ei -> ei.setIdMemberRequired(true));
			    }

			    if (createIdMemberRestriction_types) {
				JsonSchema schemaForRestriction = getOrCreatePropertyPath(restrictionSchema,
					Arrays.copyOfRange(idmPathComponents, 0, idmPathComponents.length), true);

				List<JsonSchemaType> types = new ArrayList<>();
				for (String tr : newTypeRestrictions) {
				    Optional<JsonSchemaType> tOpt = JsonSchemaType.fromString(tr);
				    if (tOpt.isPresent()) {
					types.add(tOpt.get());
				    }
				}
				schemaForRestriction.type(types.toArray(new JsonSchemaType[types.size()]));

				// update encoding infos
				encInfo.setIdMemberTypes(newTypeRestrictions);
				encodingInfosOfSubtypesInCompleteHierarchy(ci).stream()
					.forEach(ei -> ei.setIdMemberTypes(newTypeRestrictions));
			    }

			    if (createIdMemberRestriction_formats) {
				JsonSchema schemaForRestriction = getOrCreatePropertyPath(restrictionSchema,
					Arrays.copyOfRange(idmPathComponents, 0, idmPathComponents.length), true);

				for (String fr : newFormatRestrictions) {
				    schemaForRestriction.format(fr);
				}

				// update encoding infos
				final SortedSet<String> allFormatRestrictions = new TreeSet<>();
				allFormatRestrictions.addAll(establishedFormatRestrictions);
				allFormatRestrictions.addAll(newFormatRestrictions);
				encInfo.setIdMemberFormats(allFormatRestrictions);
				encodingInfosOfSubtypesInCompleteHierarchy(ci).stream()
					.forEach(ei -> ei.setIdMemberFormats(allFormatRestrictions));
			    }
			}
		    }
		}
	    }
	}
    }

    private List<EncodingInfos> encodingInfosOfSubtypesInCompleteHierarchy(ClassInfo ci) {
	List<EncodingInfos> resEis = new ArrayList<>();
	for (ClassInfo subtype : ci.subtypesInCompleteHierarchy()) {
	    EncodingInfos ei = JsonSchemaTarget.encodingInfosByCi.get(subtype);
	    if (ei != null) {
		resEis.add(ei);
	    }
	}
	return resEis;
    }

    /**
     * Gets or creates the property definitions along the specified path in the
     * given JSON Schema. The complete path can be made required. The result is the
     * schema constraint for the last property in the path.
     * 
     * @param startSchema    the schema in which to get or create the property path
     * @param pathSegments   the names of the path segments
     * @param isOptionalPath <code>true</code>, if the complete path should consist
     *                       of optional properties; <code>false</code> if all path
     *                       segments should be required
     * @return The schema constraint of the last property in the path
     */
    private JsonSchema getOrCreatePropertyPath(JsonSchema startSchema, String[] pathSegments, boolean isOptionalPath) {

	JsonSchema schema = startSchema;

	if (pathSegments != null) {
	    for (int i = 0; i < pathSegments.length; i++) {

		String propName = pathSegments[i];

		if (!isOptionalPath) {
		    schema.required(propName);
		}
		Optional<JsonSchema> propSchema = schema.property(propName);

		if (propSchema.isPresent()) {
		    schema = propSchema.get();
		} else {
		    JsonSchema newSchema = new JsonSchema();
		    schema.property(pathSegments[i], newSchema);
		    schema = newSchema;
		}
	    }
	}

	return schema;
    }

    /**
     * NOTE: This method should only be called once the schema definitions for all
     * classes have been created.
     * 
     * @param ci the class for which to search for id member encoding infos in its
     *           supertype encodings and the applicable base schema
     * @return encoding infos for the identifier JSON member, available in one of
     *         the supertype encodings of ci or a base class; can be
     *         <code>null</code> if no such member is available in the supertypes
     *         (especially, of course, if ci does not have any superclass) or the
     *         applicable base schema
     */
    private EncodingInfos identifyIdMemberInfosFromSupertypesAndBaseSchema(ClassInfo ci) {

	// first, check for id member encoding infos from base schema
	EncodingInfos encodingInfosFromBaseJsonSchemaDefinition = encodingInfosFromBaseJsonSchemaDefinition(ci);
	if (encodingInfosFromBaseJsonSchemaDefinition != null
		&& encodingInfosFromBaseJsonSchemaDefinition.getIdMemberPath().isPresent()) {

	    return encodingInfosFromBaseJsonSchemaDefinition;

	} else {

	    /*
	     * If an id member path is defined in the map entry of a supertype, use that
	     * path. Otherwise, check if an id member path is defined in the already
	     * existing encoding infos of the supertype. If so, use the according info. If
	     * an id member path can be determined for a supertype of one of the supertypes,
	     * use that path.
	     */

	    for (ClassInfo supertype : ci.supertypeClasses()) {

		// check for map entry first
		Optional<ProcessMapEntry> supertypePmeOpt = jsonSchemaTarget.mapEntry(supertype);
		if (supertypePmeOpt.isPresent()) {
		    /*
		     * So a map entry is defined for the supertype; let us see if it contains
		     * encoding information for the id member.
		     */

		    Map<String, String> supertypeEncodingInfoCharacteristics = mapEntryParamInfos.getCharacteristics(
			    supertype.name(), supertype.encodingRule(JsonSchemaConstants.PLATFORM),
			    JsonSchemaConstants.ME_PARAM_ENCODING_INFOS);

		    if (supertypeEncodingInfoCharacteristics != null) {
			EncodingInfos encInfos = EncodingInfos.from(supertypeEncodingInfoCharacteristics);
			if (encInfos.getIdMemberPath().isPresent()) {
			    return encInfos;
			}
		    }

		} else {

		    // check existing encoding infos
		    EncodingInfos encInfoOfSupertype = JsonSchemaTarget.encodingInfosByCi.get(supertype);
		    if (encInfoOfSupertype != null && encInfoOfSupertype.getIdMemberPath().isPresent()) {
			return encInfoOfSupertype;
		    } else {

			EncodingInfos idInfosFromSupertypesOfSupertype = identifyIdMemberInfosFromSupertypesAndBaseSchema(
				supertype);
			if (idInfosFromSupertypesOfSupertype != null) {

			    /*
			     * fine - we found encoding infos for the id member in the supertypes of the
			     * supertype
			     */
			    return idInfosFromSupertypesOfSupertype;
			}
		    }
		}
	    }
	}

	return null;
    }

    public void createMapEntries() {

	for (ClassInfo ci : this.classesByName.values()) {

	    /*
	     * Compute the URL to the schema definition, ignoring the anchor capability
	     * (because it would not be supported in an OpenAPI 3.0 schema).
	     */
	    String jsDefinitionReference = schemaId + fragmentIdentifier(this, ci);

	    // create map entry
	    PackageInfo schemaPi = ci.pkg().rootPackage();
	    String rule = "*";
	    ProcessMapEntry pme;

	    /*
	     * Determine if the map entry must have a parameter - e.g. to convey the entity
	     * type member path. Currently, we only consider the 'encodingInfos' parameter.
	     */
	    if (JsonSchemaTarget.encodingInfosByCi.containsKey(ci)
		    && !JsonSchemaTarget.encodingInfosByCi.get(ci).isEmpty()) {
		String paramValue = JsonSchemaConstants.ME_PARAM_ENCODING_INFOS + "{"
			+ JsonSchemaTarget.encodingInfosByCi.get(ci).toParamValue() + "}";
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

	String fragmentIdentifier = fragmentIdentifier(jsd, ci);

	/*
	 * 2023-02-13 JE: Currently, relative references are only created when a
	 * definition within the same JSON Schema document is created. The reference
	 * then simply uses the fragment identifier. In all other cases, a complete URI
	 * is created, using the ID of the given schema document as well as the fragment
	 * identifier to the definition of the given class.
	 * 
	 * There may be a way to improve references between separate JSON Schema
	 * documents, using relative URIs. That is future work.
	 */

	return jsd == this ? fragmentIdentifier : schemaId + fragmentIdentifier;
    }

    private String jsonSchemaPropertyDefinitionReference(ClassInfo ci) {

	Optional<JsonSchemaDocument> jsdopt = this.jsonSchemaTarget.jsonSchemaDocument(ci);

	if (jsdopt.isEmpty()) {

	    String fixmeRef = "#/FIXME/FIXME_" + ci.name() + "_Properties";
	    result.addError(this, 136, ci.name(), fixmeRef);
	    return fixmeRef;

	} else {

	    JsonSchemaDocument jsd = jsdopt.get();
	    String schemaRef = jsonSchemaDefinitionReference(jsd, ci) + "_Properties";
	    return schemaRef;
	}
    }

    private String fragmentIdentifier(JsonSchemaDocument jsd, ClassInfo ci) {
	return fragmentIdentifier(jsd, ci.name(), ci.matches(JsonSchemaConstants.RULE_CLS_NAME_AS_ANCHOR));
    }

    private String fragmentIdentifier(JsonSchemaDocument jsd, String definitionName,
	    boolean anchorAvailableInDefinition) {

	if (JsonSchemaTarget.useAnchorsInLinksToGeneratedSchemaDefinitions
		&& jsd.getSchemaVersion() != JsonSchemaVersion.OPENAPI_30 && anchorAvailableInDefinition) {

	    // the definition contains an anchor - use it
	    return "#" + definitionName;

	} else {

	    // use a JSON Pointer to reference the definition
	    if (jsd.getSchemaVersion() == JsonSchemaVersion.DRAFT_07
		    || jsd.getSchemaVersion() == JsonSchemaVersion.OPENAPI_30) {
		return "#/definitions/" + definitionName;
	    } else {
		return "#/$defs/" + definitionName;
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

	    // Keyword: maxLength
	    String tvMaxLengthName = "length";
	    String maxLength = ci.taggedValue(tvMaxLengthName);
	    if (StringUtils.isBlank(maxLength)) {
		tvMaxLengthName = "maxLength";
		maxLength = ci.taggedValue(tvMaxLengthName);
	    }
	    if (StringUtils.isBlank(maxLength)) {
		tvMaxLengthName = "size";
		maxLength = ci.taggedValue(tvMaxLengthName);
	    }
	    if (StringUtils.isNotBlank(maxLength)) {
		int lengthValue = parseRestrictingFacetAsInteger(ci, maxLength, tvMaxLengthName, "maxLength");
		if (lengthValue < 0) {
		    MessageContext mc = result.addError(this, 102, ci.name(), maxLength, tvMaxLengthName, "maxLength");
		    if (mc != null) {
			mc.addDetail(this, 0, ci.fullName());
		    }
		} else {
		    restrictingFacetSchema.maxLength(lengthValue);
		}
	    }

	    // Keyword: minLength
	    String tvMinLengthName = "minLength";
	    String minLength = ci.taggedValue(tvMinLengthName);
	    if (StringUtils.isNotBlank(minLength)) {
		int lengthValue = parseRestrictingFacetAsInteger(ci, minLength, tvMinLengthName, "minLength");
		if (lengthValue < 0) {
		    MessageContext mc = result.addError(this, 102, ci.name(), minLength, tvMinLengthName, "minLength");
		    if (mc != null) {
			mc.addDetail(this, 0, ci.fullName());
		    }
		} else {
		    restrictingFacetSchema.minLength(lengthValue);
		}
	    }

	    // Keyword: pattern
	    String pattern = ci.taggedValue("jsonPattern");
	    if (StringUtils.isNotBlank(pattern)) {
		restrictingFacetSchema.pattern(pattern);
	    }

	} else if (jsImplementationTypeInfo.getSimpleType() == JsonSchemaType.NUMBER
		|| jsImplementationTypeInfo.getSimpleType() == JsonSchemaType.INTEGER) {

	    // Keyword: minimum
	    String tvNameMinimum = "rangeMinimum";
	    String min = ci.taggedValue(tvNameMinimum);
	    if (StringUtils.isBlank(min)) {
		tvNameMinimum = "minInclusive";
		min = ci.taggedValue(tvNameMinimum);
	    }
	    if (StringUtils.isNotBlank(min)) {
		Double minDouble = parseRestrictingFacetAsDouble(ci, min, tvNameMinimum, "minimum");
		if (minDouble != null) {
		    restrictingFacetSchema.minimum(minDouble);
		}
	    }

	    // Keyword: exclusiveMinimum
	    String tvNameExclusiveMinimum = "minExclusive";
	    String exclMin = ci.taggedValue(tvNameExclusiveMinimum);
	    if (StringUtils.isNotBlank(exclMin)) {
		Double minExclDouble = parseRestrictingFacetAsDouble(ci, exclMin, tvNameExclusiveMinimum,
			"exclusiveMinimum");
		if (minExclDouble != null) {
		    restrictingFacetSchema.exclusiveMinimum(minExclDouble);
		}
	    }

	    // Keyword: maximum
	    String tvNameMaximum = "rangeMaximum";
	    String max = ci.taggedValue(tvNameMaximum);
	    if (StringUtils.isBlank(max)) {
		tvNameMaximum = "maxInclusive";
		max = ci.taggedValue(tvNameMaximum);
	    }
	    if (StringUtils.isNotBlank(max)) {
		Double maxDouble = parseRestrictingFacetAsDouble(ci, max, tvNameMaximum, "maximum");
		if (maxDouble != null) {
		    restrictingFacetSchema.maximum(maxDouble);
		}
	    }

	    // Keyword: exclusiveMaximum
	    String tvNameExclusiveMaximum = "maxExclusive";
	    String exclMax = ci.taggedValue(tvNameExclusiveMaximum);
	    if (StringUtils.isNotBlank(exclMax)) {
		Double maxExclDouble = parseRestrictingFacetAsDouble(ci, exclMax, tvNameExclusiveMaximum,
			"exclusiveMaximum");
		if (maxExclDouble != null) {
		    restrictingFacetSchema.exclusiveMaximum(maxExclDouble);
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

    private Integer parseRestrictingFacetAsInteger(ClassInfo ci, String stringValue, String tvName,
	    String jsKeywordName) {

	try {
	    int integerValue = Integer.parseInt(stringValue.trim());
	    return integerValue;
	} catch (NumberFormatException e) {
	    MessageContext mc = result.addError(this, 101, ci.name(), stringValue, tvName, jsKeywordName);
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

	if (ci.matches(JsonSchemaConstants.RULE_ALL_DOCUMENTATION)
		&& ci.matches(JsonSchemaConstants.RULE_CLS_DOCUMENTATION_ENUM_DESCRIPTION)) {

	    for (PropertyInfo pi : ci.properties().values()) {
		if (JsonSchemaTarget.isEncoded(pi)) {
		    JsonSchema enumJs = new JsonSchema();
		    this.annotationGenerator.applyAnnotations(enumJs, pi);
//		    if (!enumJs.isEmpty()) {
		    String enumDescKey = StringUtils.isNotBlank(pi.initialValue()) ? pi.initialValue().trim()
			    : pi.name();
		    jsClass.enumDescription(enumDescKey, enumJs);
//		    }
		}
	    }
	}

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

	    if (jsonSchemaVersion == JsonSchemaVersion.DRAFT_07) {
		jsClass.id("#" + ci.name());
	    } else {
		jsClass.anchor(ci.name());
	    }
	}

	if (ci.matches(JsonSchemaConstants.RULE_ALL_DOCUMENTATION)) {
	    this.annotationGenerator.applyAnnotations(jsClass, ci);
	}
    }

    private JsonSchema jsonSchema(ClassInfo ci) {

	JsonSchema jsClass = new JsonSchema();

	addCommonSchemaMembers(jsClass, ci);

	// not an enumeration or code list

	JsonSchema jsClassContents = jsClass;

	EncodingInfos encInfo = jsonSchemaTarget.getOrCreateEncodingInfos(ci);

	/*
	 * =============
	 * 
	 * Handle generalization (virtual and explicit)
	 * 
	 * =============
	 */
	List<JsonSchema> allOfMembers = new ArrayList<>();

	SortedSet<ClassInfo> supertypes = ci.supertypeClasses();

	handleVirtualGeneralization(ci, supertypes, allOfMembers);

	// convert generalization
	if ((ci.category() == Options.FEATURE || ci.category() == Options.OBJECT || ci.category() == Options.DATATYPE
		|| ci.category() == Options.MIXIN) && !supertypes.isEmpty()) {

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

	if (!allOfMembers.isEmpty()) {

	    // prepare the contents schema for ci
	    jsClassContents = new JsonSchema();

	    allOfMembers.add(jsClassContents);

	    // create the "allOf"
	    JsonSchema allOfSchema = jsClass.allOf(allOfMembers.toArray(new JsonSchema[allOfMembers.size()]));
	    this.allOfSchemaByClass.put(ci, allOfSchema);
	}

	jsClassContents.type(JsonSchemaType.OBJECT);

	/*
	 * =============
	 * 
	 * Identify and handle special properties (default / primary geometry, primary
	 * place, primary instant, primary interval)
	 * 
	 * =============
	 */
	Set<PropertyInfo> specialPisToMapButNotEncode = new HashSet<>();

	handleGeometryRestriction(ci, specialPisToMapButNotEncode, jsClassContents);
	handlePrimaryPlaceRestriction(ci, specialPisToMapButNotEncode, jsClassContents);
	handlePrimaryTimeRestriction(ci, specialPisToMapButNotEncode, jsClassContents);

	JsonSchema jsProperties = jsClassContents;

	/*
	 * =============
	 * 
	 * Handle creation of separate property definitions
	 * 
	 * =============
	 */
	boolean createPropertyDefinitionReference = false;

	if (JsonSchemaTarget.createSeparatePropertyDefinitions
		&& (ci.category() == Options.FEATURE || ci.category() == Options.OBJECT)) {

	    createPropertyDefinitionReference = true;

	    /*
	     * Re-set the jsProperties variable to a new JSON Schema object
	     */
	    jsProperties = new JsonSchema();
	    jsProperties.type(JsonSchemaType.OBJECT);

	    /*
	     * Create a new JsonSchema with the new jsProperties schema and an allOf (if
	     * there are relevant supertypes).
	     */
	    JsonSchema propertyDefSchema = new JsonSchema();

	    List<JsonSchema> propertyDefAllOfMembers = new ArrayList<>();

	    // convert generalization
	    if (!supertypes.isEmpty()) {

		// add schema definitions for all supertypes
		for (ClassInfo supertype : supertypes) {
		    propertyDefAllOfMembers.add(new JsonSchema().ref(jsonSchemaPropertyDefinitionReference(supertype)));
		}
	    }

	    if (!propertyDefAllOfMembers.isEmpty()) {
		propertyDefAllOfMembers.add(jsProperties);
		propertyDefSchema
			.allOf(propertyDefAllOfMembers.toArray(new JsonSchema[propertyDefAllOfMembers.size()]));
	    } else {
		propertyDefSchema = jsProperties;
	    }

	    propertyDefSchemaByClass.put(ci, propertyDefSchema);
	    propertyDefSchemaActualPropertiesSchemaByClass.put(ci, jsProperties);
	}

	/*
	 * =============
	 * 
	 * Handle property nesting
	 * 
	 * =============
	 */

	if (!(ci.category() == Options.DATATYPE || ci.category() == Options.UNION)
		&& ci.matches(JsonSchemaConstants.RULE_CLS_NESTED_PROPERTIES)) {
	    /*
	     * NOTE: If a class does not define any property in "properties", that member
	     * will be removed later on, together with the requirement for it. If
	     * "properties" has no required members, the requirement for "properties" would
	     * also be removed.
	     */

	    if (createPropertyDefinitionReference) {
		jsClassContents.property("properties", new JsonSchema().ref(jsonSchemaPropertyDefinitionReference(ci)))
			.required("properties");
	    } else {
		jsProperties = new JsonSchema();
		jsProperties.type(JsonSchemaType.OBJECT);
		jsClassContents.property("properties", jsProperties).required("properties");
	    }

	} else if (createPropertyDefinitionReference) {

	    /*
	     * non-nested case where the properties are contained in a separate definition
	     * and shall be referenced
	     */
	    jsClassContents.ref(jsonSchemaPropertyDefinitionReference(ci));
	}

	/*
	 * =============
	 * 
	 * Handle entity type member
	 * 
	 * =============
	 */
	/*
	 * Check if an entity type member is available in the encodings of ci's
	 * supertypes or its base class. If so, simply keep track of relevant infos for
	 * it (for map entry creation, and potentially also type specific checks).
	 * Otherwise, check if such a member shall be added to the encoding of ci. If
	 * so, do it, and also keep track of relevant member infos.
	 */
	EncodingInfos entityTypeMemberInfosFromSupertypesAndBaseSchema = identifyEntityTypeMemberInfosFromSupertypesAndBaseSchema(
		ci);

	if (entityTypeMemberInfosFromSupertypesAndBaseSchema == null) {

	    if (matchesEntityTypeCreationRules(ci)) {

		String entityTypeMemberName = jsonSchemaTarget.getEntityTypeName();
		jsProperties.property(entityTypeMemberName, new JsonSchema().type(JsonSchemaType.STRING))
			.required(entityTypeMemberName);

		String entityTypeMemberPath = (ci.matches(JsonSchemaConstants.RULE_CLS_NESTED_PROPERTIES)
			? "properties/"
			: "") + entityTypeMemberName;
		encInfo.setEntityTypeMemberPath(entityTypeMemberPath);
		encInfo.setEntityTypeMemberRequired(true);
	    }

	} else {

	    // keep track of the infos for map entry creation
	    encInfo.setEntityTypeMemberPath(
		    entityTypeMemberInfosFromSupertypesAndBaseSchema.getEntityTypeMemberPath().get());
	    if (entityTypeMemberInfosFromSupertypesAndBaseSchema.getEntityTypeMemberRequired().isPresent()) {
		encInfo.setEntityTypeMemberRequired(
			entityTypeMemberInfosFromSupertypesAndBaseSchema.getEntityTypeMemberRequired().get());
	    }
	}

	/*
	 * =============
	 * 
	 * Handle identifier properties
	 * 
	 * =============
	 */
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

		String identifierName = jsonSchemaTarget.objectIdentifierName();
		JsonSchemaType[] identifierTypes = jsonSchemaTarget.objectIdentifierType();
		boolean identifierRequired = jsonSchemaTarget.objectIdentifierRequired();

		JsonSchema objectIdentifierTypeSchema = new JsonSchema();
		createTypeDefinition(identifierTypes, objectIdentifierTypeSchema);
		jsProperties.property(identifierName, objectIdentifierTypeSchema);

		if (identifierRequired) {
		    jsProperties.required(jsonSchemaTarget.objectIdentifierName());
		}

		// also keep track of encoding infos for the generated id member
		String idMemberPath = (ci.matches(JsonSchemaConstants.RULE_CLS_NESTED_PROPERTIES) ? "properties/" : "")
			+ identifierName;
		encInfo.setIdMemberPath(idMemberPath);
		encInfo.setIdMemberRequired(identifierRequired);
		for (JsonSchemaType type : identifierTypes) {
		    encInfo.addIdMemberType(type.getName());
		}
	    }
	}

	/*
	 * =============
	 * 
	 * Determine value type options
	 * 
	 * =============
	 */
	String valueTypeOptionsTV = ci.taggedValue("valueTypeOptions");
	ValueTypeOptions vto;
	if (StringUtils.isNotBlank(valueTypeOptionsTV) && ci.matches(JsonSchemaConstants.RULE_CLS_VALUE_TYPE_OPTIONS)) {
	    vto = new ValueTypeOptions(valueTypeOptionsTV);
	} else {
	    vto = new ValueTypeOptions();
	}

	/*
	 * =============
	 * 
	 * General handling of class properties
	 * 
	 * =============
	 */
	for (PropertyInfo pi : ci.properties().values()) {

	    if (!JsonSchemaTarget.isEncoded(pi)) {
		result.addInfo(this, 9, pi.name(), pi.inClass().name());
		continue;
	    }

	    if (specialPisToMapButNotEncode.contains(pi)) {
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

		/*
		 * Now that it has been determined that the identifier property is encoded, keep
		 * track of encoding infos for it.
		 */
		String idMemberPath = (ci.matches(JsonSchemaConstants.RULE_CLS_NESTED_PROPERTIES) ? "properties/" : "")
			+ pi.name();
		encInfo.setIdMemberPath(idMemberPath);
		encInfo.setIdMemberRequired(pi.cardinality().minOccurs > 0);
		Optional<JsonSchemaTypeInfo> piTypeOpt = identifyJsonSchemaType(pi);
		if (piTypeOpt.isPresent()) {
		    JsonSchemaTypeInfo ti = piTypeOpt.get();
		    if (ti.isSimpleType()) {
			encInfo.addIdMemberType(ti.getSimpleType().getName());
		    }
		    if (ti.hasKeywords()) {
			Optional<FormatKeyword> fkOpt = ti.getKeywords().stream()
				.filter(kw -> kw instanceof FormatKeyword).map(kw -> (FormatKeyword) kw).findFirst();
			if (fkOpt.isPresent()) {
			    encInfo.addIdMemberFormat(fkOpt.get().value());
			}
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
		supertypePropertyTypeInfos.addAll(createJsonSchemaTypeInfoForReference(supertypePi));
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
			    supertypePropertyTypeInfos, identifyJsonSchemaType(supertypePi), supertypePi,
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

	/*
	 * =============
	 * 
	 * Additional union specific constraints
	 * 
	 * =============
	 */
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

	/*
	 * =============
	 * 
	 * Clean-up
	 * 
	 * =============
	 */
	if (!(ci.category() == Options.DATATYPE || ci.category() == Options.UNION)
		&& ci.matches(JsonSchemaConstants.RULE_CLS_NESTED_PROPERTIES)) {
	    /*
	     * Check if "properties" member exists and is empty; if so, remove it (and the
	     * requirement for it). If the member is not empty, check if it defines required
	     * properties. If not, remove the requirement for the "properties" member. While
	     * doing so, take into account the potential presence of a schema reference to
	     * an external JSON Schema definition of the properties.
	     */
	    Optional<LinkedHashMap<String, JsonSchema>> classPropertiesOpt = jsClassContents.properties();
	    if (classPropertiesOpt.isPresent()) {
		LinkedHashMap<String, JsonSchema> classProperties = classPropertiesOpt.get();
		if (classProperties.containsKey("properties")) {

		    JsonSchema propertiesMemberSchema = classProperties.get("properties");

		    if (propertyDefSchemaActualPropertiesSchemaByClass.containsKey(ci)) {

			/*
			 * Case where the properties are defined in a separate definition schema.
			 * Definitely leave the "properties" member (to take into account the
			 * possibility of properties inherited from a supertype), but maybe remove the
			 * requirement for the "properties" member.
			 */
			JsonSchema propertyDefSchema = propertyDefSchemaActualPropertiesSchemaByClass.get(ci);

			Optional<SortedSet<String>> propertiesMemberRequiredOpt = propertyDefSchema.required();
			if (propertiesMemberRequiredOpt.isEmpty() || propertiesMemberRequiredOpt.get().isEmpty()) {
			    // remove requirement for "properties" member (within the class definition!)
			    jsClassContents.removeRequired("properties");
			}

		    } else {

			/*
			 * Normal case, where the properties are defined within the class definition
			 * (for the "properties" member)
			 */

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
	}

	return jsClass;
    }

    private void handlePrimaryTimeRestriction(ClassInfo ci, Set<PropertyInfo> specialPisToMapButNotEncode,
	    JsonSchema jsClassContents) {

	if (ci.category() == Options.FEATURE && ci.matches(JsonSchemaConstants.RULE_CLS_PRIMARY_TIME)) {

	    PropertyInfo primaryInstantPi = null;
	    PropertyInfo primaryIntervalPi_start = null;
	    PropertyInfo primaryIntervalPi_end = null;
	    PropertyInfo primaryIntervalPi_interval = null;

	    for (PropertyInfo pi : ci.properties().values()) {
		if (JsonSchemaTarget.isEncoded(pi)) {

		    // identify primary instant property
		    if ("true".equalsIgnoreCase(pi.taggedValue("jsonPrimaryInstant"))) {
			if (primaryInstantPi == null) {
			    primaryInstantPi = pi;
			} else {
			    MessageContext mc = result.addError(this, 131, ci.name(), primaryInstantPi.name(),
				    pi.name());
			    if (mc != null) {
				mc.addDetail(this, 1, pi.fullName());
			    }
			}
			specialPisToMapButNotEncode.add(pi);
		    }

		    // identify primary interval properties
		    String primaryIntervalTV = pi.taggedValue("jsonPrimaryInterval");
		    if ("start".equalsIgnoreCase(primaryIntervalTV)) {
			if (primaryIntervalPi_start == null) {
			    primaryIntervalPi_start = pi;
			} else {
			    MessageContext mc = result.addError(this, 132, ci.name(), primaryIntervalPi_start.name(),
				    pi.name());
			    if (mc != null) {
				mc.addDetail(this, 1, pi.fullName());
			    }
			}
			specialPisToMapButNotEncode.add(pi);

		    } else if ("end".equalsIgnoreCase(primaryIntervalTV)) {
			if (primaryIntervalPi_end == null) {
			    primaryIntervalPi_end = pi;
			} else {
			    MessageContext mc = result.addError(this, 133, ci.name(), primaryIntervalPi_end.name(),
				    pi.name());
			    if (mc != null) {
				mc.addDetail(this, 1, pi.fullName());
			    }
			}
			specialPisToMapButNotEncode.add(pi);

		    } else if ("interval".equalsIgnoreCase(primaryIntervalTV)) {
			if (primaryIntervalPi_interval == null) {
			    primaryIntervalPi_interval = pi;
			} else {
			    MessageContext mc = result.addError(this, 134, ci.name(), primaryIntervalPi_interval.name(),
				    pi.name());
			    if (mc != null) {
				mc.addDetail(this, 1, pi.fullName());
			    }
			}
			specialPisToMapButNotEncode.add(pi);
		    }
		}
	    }

	    /*
	     * Determine Optionality of primary instant and primary interval
	     */
	    PrimaryTimeOptionality primaryInstantOptionality = PrimaryTimeOptionality.UNDEFINED;
	    if (primaryInstantPi != null) {
		primaryInstantOptionality = isOptional(primaryInstantPi) ? PrimaryTimeOptionality.OPTIONAL
			: PrimaryTimeOptionality.REQUIRED;
	    }

	    PrimaryTimeOptionality primaryIntervalOptionality = PrimaryTimeOptionality.UNDEFINED;
	    if (primaryIntervalPi_start != null || primaryIntervalPi_end != null
		    || primaryIntervalPi_interval != null) {

		/*
		 * First, check primary interval consistency (duplicate start, end, or interval
		 * cases have already been identified and logged).
		 */
		if ((primaryIntervalPi_start != null || primaryIntervalPi_end != null)
			&& primaryIntervalPi_interval != null) {

		    MessageContext mc = result.addError(this, 135, ci.name(), primaryIntervalPi_interval.name(),
			    primaryIntervalPi_start != null ? primaryIntervalPi_start.name() : "<undefined>",
			    primaryIntervalPi_end != null ? primaryIntervalPi_end.name() : "<undefined>");
		    if (mc != null) {
			mc.addDetail(this, 0, ci.fullName());
		    }

		} else {

		    if (primaryIntervalPi_interval != null) {
			primaryIntervalOptionality = isOptional(primaryIntervalPi_interval)
				? PrimaryTimeOptionality.OPTIONAL
				: PrimaryTimeOptionality.REQUIRED;
		    } else {
			if (primaryIntervalPi_start == null && primaryIntervalPi_end == null) {
			    // nothing to do, really; added just in case the logic is changed in the future
			    primaryIntervalOptionality = PrimaryTimeOptionality.UNDEFINED;
			} else if ((primaryIntervalPi_start != null && !isOptional(primaryIntervalPi_start))
				|| (primaryIntervalPi_end != null && !isOptional(primaryIntervalPi_end))) {
			    primaryIntervalOptionality = PrimaryTimeOptionality.REQUIRED;
			} else {
			    primaryIntervalOptionality = PrimaryTimeOptionality.OPTIONAL;
			}
		    }
		}
	    }

	    // now determine and create the actual restriction
	    JsonSchema timeJs = new JsonSchema();

	    if ((primaryInstantOptionality == PrimaryTimeOptionality.UNDEFINED
		    && primaryIntervalOptionality == PrimaryTimeOptionality.UNDEFINED)
		    || (primaryInstantOptionality == PrimaryTimeOptionality.OPTIONAL
			    && primaryIntervalOptionality == PrimaryTimeOptionality.OPTIONAL)) {

		// no time restriction is necessary

	    } else if (primaryInstantOptionality == PrimaryTimeOptionality.UNDEFINED
		    && primaryIntervalOptionality == PrimaryTimeOptionality.OPTIONAL) {

		timeJs.oneOf(new JsonSchema().type(JsonSchemaType.NULL));
		timeJs.oneOf(new JsonSchema().type(JsonSchemaType.OBJECT).required("interval"));

	    } else if ((primaryInstantOptionality == PrimaryTimeOptionality.UNDEFINED
		    || primaryInstantOptionality == PrimaryTimeOptionality.OPTIONAL)
		    && primaryIntervalOptionality == PrimaryTimeOptionality.REQUIRED) {

		timeJs.type(JsonSchemaType.OBJECT).required("interval");

	    } else if (primaryInstantOptionality == PrimaryTimeOptionality.OPTIONAL
		    && primaryIntervalOptionality == PrimaryTimeOptionality.UNDEFINED) {

		timeJs.oneOf(new JsonSchema().type(JsonSchemaType.NULL));
		timeJs.oneOf(new JsonSchema().type(JsonSchemaType.OBJECT).required("date"));
		timeJs.oneOf(new JsonSchema().type(JsonSchemaType.OBJECT).required("timestamp"));

	    } else if (primaryInstantOptionality == PrimaryTimeOptionality.REQUIRED
		    && (primaryIntervalOptionality == PrimaryTimeOptionality.UNDEFINED
			    || primaryIntervalOptionality == PrimaryTimeOptionality.OPTIONAL)) {

		timeJs.oneOf(new JsonSchema().type(JsonSchemaType.OBJECT).required("date"));
		timeJs.oneOf(new JsonSchema().type(JsonSchemaType.OBJECT).required("timestamp"));

	    } else {

		timeJs.allOf(new JsonSchema().type(JsonSchemaType.OBJECT).required("interval"));

		JsonSchema oneOfJs = new JsonSchema();
		oneOfJs.oneOf(new JsonSchema().type(JsonSchemaType.OBJECT).required("date"));
		oneOfJs.oneOf(new JsonSchema().type(JsonSchemaType.OBJECT).required("timestamp"));

		timeJs.allOf(oneOfJs);
	    }

	    if (!timeJs.isEmpty()) {
		jsClassContents.property("time", timeJs);
	    }
	}
    }

    private boolean isOptional(PropertyInfo pi) {
	return pi.cardinality().minOccurs == 0 || (pi.voidable() && pi.matches(JsonSchemaConstants.RULE_PROP_VOIDABLE));
    }

    private void handlePrimaryPlaceRestriction(ClassInfo ci, Set<PropertyInfo> specialPisToMapButNotEncode,
	    JsonSchema jsClassContents) {

	if (ci.category() == Options.FEATURE && ci.matches(JsonSchemaConstants.RULE_CLS_PRIMARY_PLACE)) {

	    PropertyInfo primaryPlacePi = null;

	    for (PropertyInfo pi : ci.properties().values()) {
		if (JsonSchemaTarget.isEncoded(pi) && "true".equalsIgnoreCase(pi.taggedValue("jsonPrimaryPlace"))) {
		    primaryPlacePi = pi;
		}
	    }

	    if (primaryPlacePi != null) {
		specialPisToMapButNotEncode.add(primaryPlacePi);
		Optional<JsonSchemaTypeInfo> typeInfoOpt = identifyJsonSchemaType(primaryPlacePi);

		if (typeInfoOpt.isPresent()) {

		    JsonSchema placeJs = new JsonSchema();
		    specialPropertyValueRestriction(placeJs, primaryPlacePi, typeInfoOpt.get());
		    jsClassContents.property("place", placeJs);

		} else {
		    MessageContext mc = result.addError(this, 130, primaryPlacePi.name(), ci.name(),
			    primaryPlacePi.typeInfo().name);
		    if (mc != null) {
			mc.addDetail(this, 1, primaryPlacePi.fullName());
		    }
		}

		if (primaryPlacePi.cardinality().maxOccurs > 1) {
		    MessageContext mc = result.addWarning(this, 129, primaryPlacePi.name(), ci.name());
		    if (mc != null) {
			mc.addDetail(this, 1, primaryPlacePi.fullName());
		    }
		}
	    }

	}
    }

    private void specialPropertyValueRestriction(JsonSchema jsForValueRestriction, PropertyInfo pi,
	    JsonSchemaTypeInfo jsonSchemaTypeInfo) {

	if (isOptional(pi)) {
	    // create choice between null and an actual value
	    jsForValueRestriction.oneOf(new JsonSchema().type(JsonSchemaType.NULL));
	    jsForValueRestriction.oneOf(new JsonSchema().ref(jsonSchemaTypeInfo.getRef()));
	} else {
	    // the property must have an actual value
	    jsForValueRestriction.ref(jsonSchemaTypeInfo.getRef());
	}
    }

    private void handleGeometryRestriction(ClassInfo ci, Set<PropertyInfo> specialPisToMapButNotEncode,
	    JsonSchema jsClassContents) {

	PropertyInfo primaryGeometryPi = null;

	if (ci.category() == Options.FEATURE && ci.matches(JsonSchemaConstants.RULE_CLS_PRIMARY_GEOMETRY)) {

	    /*
	     * First, try to find a direct property that is explicitly marked to be the
	     * primary geometry property of the feature type.
	     */
	    for (PropertyInfo pi : ci.properties().values()) {
		if (JsonSchemaTarget.isEncoded(pi) && "true".equalsIgnoreCase(pi.taggedValue("jsonPrimaryGeometry"))) {
		    primaryGeometryPi = pi;
		}
	    }

	    /*
	     * Second, if the primary geometry was not explicitly marked or found, determine
	     * if the feature type only has a single geometric property (without tag
	     * jsonPrimaryGeometry being equal to, ignoring case, "false"), and owns that
	     * property. If so, note it as primary geometry.
	     */
	    if (primaryGeometryPi == null) {
		List<PropertyInfo> geometryPis = new ArrayList<>();
		for (PropertyInfo pi : ci.propertiesAll()) {
		    if (JsonSchemaTarget.isEncoded(pi)) {
			Optional<JsonSchemaTypeInfo> typeInfoOpt = identifyJsonSchemaType(pi);
			if (typeInfoOpt.isPresent() && typeInfoOpt.get().isGeometry()) {
			    geometryPis.add(pi);
			}
		    }
		}
		if (geometryPis.size() == 1) {
		    PropertyInfo geomPi = geometryPis.get(0);
		    if (geomPi.inClass() == ci
			    && !"false".equalsIgnoreCase(geomPi.taggedValue("jsonPrimaryGeometry"))) {
			primaryGeometryPi = geomPi;
		    }
		}
	    }

	} else if (ci.category() != Options.UNION && !ci.properties().isEmpty()
		&& (ci.matches(JsonSchemaConstants.RULE_CLS_DEFAULT_GEOMETRY_SINGLEGEOMPROP)
			|| ci.matches(JsonSchemaConstants.RULE_CLS_DEFAULT_GEOMETRY_MULTIGEOMPROPS))) {

	    Map<PropertyInfo, JsonSchemaTypeInfo> typeInfoByGeometryPi = new HashMap<>();
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
			    primaryGeometryPi = pi;
			}

			typeInfoByGeometryPi.put(pi, typeInfo);
		    }
		}
	    }

	    if (primaryGeometryPi != null) {

		if (typeInfoByGeometryPi.size() > 1) {

		    // multiple default geometry properties detected

		    // ensure that none is encoded
		    primaryGeometryPi = null;

		    String geometryPiNames = typeInfoByGeometryPi.keySet().stream().map(pi -> pi.name()).sorted()
			    .collect(Collectors.joining(", "));

		    MessageContext mc = result.addError(this, 112, ci.name(), geometryPiNames);
		    if (mc != null) {
			mc.addDetail(this, 0, ci.fullName());
		    }
		}
	    }
	}

	// Evaluate the primary / default geometry
	if (primaryGeometryPi != null) {

	    /*
	     * keep track of this property, so that it can be ignored later on when encoding
	     * the other properties of the class
	     */
	    specialPisToMapButNotEncode.add(primaryGeometryPi);

	    // check cardinality
	    if (primaryGeometryPi.cardinality().maxOccurs > 1) {
		MessageContext mc = result.addWarning(this, 127, primaryGeometryPi.name(), ci.name());
		if (mc != null) {
		    mc.addDetail(this, 1, primaryGeometryPi.fullName());
		}
	    }

	    Optional<JsonSchemaTypeInfo> typeInfoOpt = identifyJsonSchemaType(primaryGeometryPi);

	    if (typeInfoOpt.isEmpty()) {

		MessageContext mc = result.addError(this, 128, primaryGeometryPi.name(), ci.name(),
			primaryGeometryPi.typeInfo().name);
		if (mc != null) {
		    mc.addDetail(this, 1, primaryGeometryPi.fullName());
		}

	    } else {

		boolean isGeoJsonCompatibleGeometryType = JsonSchemaTarget.geoJsonCompatibleGeometryTypes
			.contains(primaryGeometryPi.typeInfo().name);

		if (primaryGeometryPi.inClass().matches(JsonSchemaConstants.RULE_CLS_JSON_FG_GEOMETRY)) {

		    // TODO Take into account the secondary geometry!

		    /*
		     * WARNING: Right now, the logic here only supports schemas with at most one
		     * geometric property per feature type.
		     */

		    // encode restriction for the "geometry" member only if the type is compatible
		    if (isGeoJsonCompatibleGeometryType) {

			createPrimaryGeometryValueRestriction(jsClassContents, "geometry", primaryGeometryPi,
				typeInfoOpt.get(), true);
			createPrimaryGeometryValueRestriction(jsClassContents, "place", primaryGeometryPi,
				typeInfoOpt.get(), true);
		    } else {

			/*
			 * Encode the restriction also for the "place" member.
			 * 
			 * TODO Take into account the secondary geometry!
			 */

			createPrimaryGeometryValueRestriction(jsClassContents, "place", primaryGeometryPi,
				typeInfoOpt.get(), false);
		    }

		} else if (isGeoJsonCompatibleGeometryType) {

		    JsonSchema geomJs = new JsonSchema();
		    specialPropertyValueRestriction(geomJs, primaryGeometryPi, typeInfoOpt.get());
		    jsClassContents.property("geometry", geomJs);

		} else {

		    MessageContext mc = result.addWarning(this, 139, primaryGeometryPi.typeInfo().name,
			    primaryGeometryPi.name());
		    if (mc != null) {
			mc.addDetail(this, 1, primaryGeometryPi.fullName());
		    }
		}
	    }
	}
    }

    private void createPrimaryGeometryValueRestriction(JsonSchema jsContentSchemaForPrimaryGeometryPropertyRestriction,
	    String jsonMemberName, PropertyInfo primaryGeometryPi, JsonSchemaTypeInfo jsonSchemaTypeInfo,
	    boolean alwaysAddNullOption) {

	JsonSchema jsForValueRestriction = new JsonSchema();

	boolean addNullOption = alwaysAddNullOption || isOptional(primaryGeometryPi);

	if (addNullOption) {
	    // create choice between null and an actual value
	    jsForValueRestriction.oneOf(new JsonSchema().type(JsonSchemaType.NULL));
	    jsForValueRestriction.oneOf(new JsonSchema().ref(jsonSchemaTypeInfo.getRef()));
	} else {
	    // the property must have an actual value
	    jsForValueRestriction.ref(jsonSchemaTypeInfo.getRef());
	}

	jsContentSchemaForPrimaryGeometryPropertyRestriction.property(jsonMemberName, jsForValueRestriction);
    }

    /**
     * @param ci the class for which to identify the entity type member path
     * @return the path of the JSON member that is used to encode the type of the
     *         class; can be <code>null</code> if no such member is available
     */
    private String identifyEntityTypeMemberPath(ClassInfo ci) {

	if (JsonSchemaTarget.encodingInfosByCi.containsKey(ci)
		&& JsonSchemaTarget.encodingInfosByCi.get(ci).getEntityTypeMemberPath().isPresent()) {
	    return JsonSchemaTarget.encodingInfosByCi.get(ci).getEntityTypeMemberPath().get();
	} else {

	    // check supertypes and base schema
	    EncodingInfos entityTypeMemberInfosFromSupertypes = identifyEntityTypeMemberInfosFromSupertypesAndBaseSchema(
		    ci);
	    if (entityTypeMemberInfosFromSupertypes != null) {
		return entityTypeMemberInfosFromSupertypes.getEntityTypeMemberPath().get();
	    } else {

		// check if the entity type member is added to the type itself
		if (matchesEntityTypeCreationRules(ci)) {
		    return (ci.matches(JsonSchemaConstants.RULE_CLS_NESTED_PROPERTIES) ? "properties/" : "")
			    + jsonSchemaTarget.getEntityTypeName();
		} else {
		    return null;
		}
	    }
	}
    }

    /**
     * @param ci the class for which to search for entity type member encoding infos
     *           in its supertype encodings and the applicable base schema
     * @return encoding infos for the entity type JSON member, available in one of
     *         the supertype encodings of ci or a base class; can be
     *         <code>null</code> if no such member is available in the supertypes
     *         (especially, of course, if ci does not have any superclass) or the
     *         applicable base schema
     */
    private EncodingInfos identifyEntityTypeMemberInfosFromSupertypesAndBaseSchema(ClassInfo ci) {

	// first, check for entity type member encoding infos from base schema
	EncodingInfos encodingInfosFromBaseJsonSchemaDefinition = encodingInfosFromBaseJsonSchemaDefinition(ci);
	if (encodingInfosFromBaseJsonSchemaDefinition != null
		&& encodingInfosFromBaseJsonSchemaDefinition.getEntityTypeMemberPath().isPresent()) {

	    return encodingInfosFromBaseJsonSchemaDefinition;

	} else {

	    /*
	     * If an entity type member path is defined in the map entry of a supertype, use
	     * that path. Otherwise, if an entity type member path can be determined for a
	     * supertype of one of the supertypes, use that path. If that also did not
	     * succeed, check if the supertype itself would receive an entity type member,
	     * and if so, use the path of that member.
	     */

	    for (ClassInfo supertype : ci.supertypeClasses()) {

		// check for map entry first
		Optional<ProcessMapEntry> supertypePmeOpt = jsonSchemaTarget.mapEntry(supertype);
		if (supertypePmeOpt.isPresent()) {
		    /*
		     * So a map entry is defined for the supertype; let us see if it contains
		     * encoding information for the entity type member.
		     */

		    Map<String, String> supertypeEncodingInfoCharacteristics = mapEntryParamInfos.getCharacteristics(
			    supertype.name(), supertype.encodingRule(JsonSchemaConstants.PLATFORM),
			    JsonSchemaConstants.ME_PARAM_ENCODING_INFOS);

		    if (supertypeEncodingInfoCharacteristics != null) {
			EncodingInfos encInfos = EncodingInfos.from(supertypeEncodingInfoCharacteristics);
			if (encInfos.getEntityTypeMemberPath().isPresent()) {
			    return encInfos;
			}
		    }

		} else {

		    EncodingInfos entityMemberInfosFromSupertypesOfSupertype = identifyEntityTypeMemberInfosFromSupertypesAndBaseSchema(
			    supertype);
		    if (entityMemberInfosFromSupertypesOfSupertype != null) {

			/*
			 * fine - we found encoding infos for the entity type member in the supertypes
			 * of the supertype
			 */
			return entityMemberInfosFromSupertypesOfSupertype;

		    } else {

			if (matchesEntityTypeCreationRules(supertype)) {

			    EncodingInfos entityTypeEncInfos = new EncodingInfos();

			    String entityTypeMemberPathForSupertype = (supertype
				    .matches(JsonSchemaConstants.RULE_CLS_NESTED_PROPERTIES) ? "properties/" : "")
				    + jsonSchemaTarget.getEntityTypeName();

			    entityTypeEncInfos.setEntityTypeMemberPath(entityTypeMemberPathForSupertype);
			    entityTypeEncInfos.setEntityTypeMemberRequired(true);

			    return entityTypeEncInfos;
			}
		    }
		}
	    }
	}

	return null;
    }

    private boolean matchesEntityTypeCreationRules(ClassInfo ci) {
	return ci.matches(JsonSchemaConstants.RULE_CLS_NAME_AS_ENTITYTYPE) && (ci.category() == Options.FEATURE
		|| ci.category() == Options.OBJECT
		|| (ci.category() == Options.UNION && ci.matches(JsonSchemaConstants.RULE_CLS_NAME_AS_ENTITYTYPE_UNION))
		|| (ci.category() == Options.DATATYPE
			&& ci.matches(JsonSchemaConstants.RULE_CLS_NAME_AS_ENTITYTYPE_DATATYPE)));
    }

    /**
     * @param ci           the class that is being converted
     * @param supertypes   set of ci supertypes
     * @param allOfMembers collection of JSON Schemas to add the base schema
     *                     reference to, if applicable
     */
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
			if (supertype.matches(JsonSchemaConstants.RULE_CLS_VIRTUAL_GENERALIZATION)
				&& supertype.category() != Options.MIXIN) {
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

    /**
     * @param ci the class for which to identify the encoding infos for external
     *           JSON Schema definitions that directly apply to the JSON Schema
     *           definition of the class
     * @return encoding infos regarding any external schemas that directly apply to
     *         the JSON Schema encoding of the given class, and if available; else
     *         <code>null</code>
     */
    private EncodingInfos directExternalSchemaEncodingInfos(ClassInfo ci) {

	EncodingInfos externalSchemaEncodingInfos = encodingInfosFromBaseJsonSchemaDefinition(ci);

	SortedSet<ClassInfo> supertypes = ci.supertypeClasses();

	// handle generalization
	if ((ci.category() == Options.FEATURE || ci.category() == Options.OBJECT || ci.category() == Options.DATATYPE
		|| ci.category() == Options.MIXIN) && !supertypes.isEmpty()) {

	    for (ClassInfo supertype : supertypes) {

		Optional<JsonSchemaTypeInfo> typeInfo = identifyJsonSchemaTypeForSupertype(supertype,
			ci.encodingRule(JsonSchemaConstants.PLATFORM));

		if (typeInfo.isPresent() && !typeInfo.get().isSimpleType()) {

		    /*
		     * The schema will be added via allOf to the definition of ci.
		     * 
		     * Check if a map entry applies to the supertype, with encoding infos regarding
		     * specific JSON members. If so, keep track of these infos.
		     */
		    Map<String, String> supertypeEncodingInfoCharacteristics = mapEntryParamInfos.getCharacteristics(
			    supertype.name(), ci.encodingRule(JsonSchemaConstants.PLATFORM),
			    JsonSchemaConstants.ME_PARAM_ENCODING_INFOS);

		    if (supertypeEncodingInfoCharacteristics != null) {

			EncodingInfos supertypeExternalSchemaEncodingInfos = EncodingInfos
				.from(supertypeEncodingInfoCharacteristics);

			if (externalSchemaEncodingInfos == null) {
			    externalSchemaEncodingInfos = supertypeExternalSchemaEncodingInfos;
			} else {
			    try {
				EncodingInfos mergedExternalSchemaEncInfo = EncodingInfos
					.merge(externalSchemaEncodingInfos, supertypeExternalSchemaEncodingInfos);
				externalSchemaEncodingInfos = mergedExternalSchemaEncInfo;
			    } catch (ShapeChangeException e) {
				MessageContext mc = result.addError(this, 125, ci.name(), supertype.name(),
					e.getMessage());
				if (mc != null) {
				    mc.addDetail(this, 0, ci.fullNameInSchema());
				}
			    }
			}
		    }
		}
	    }
	}

	return externalSchemaEncodingInfos;
    }

    private EncodingInfos encodingInfosFromBaseJsonSchemaDefinition(ClassInfo ci) {

	if (ci.matches(JsonSchemaConstants.RULE_CLS_VIRTUAL_GENERALIZATION)) {

	    String baseJsonSchemaDef = null;
	    EncodingInfos baseEncInfo = null;

	    if (ci.category() == Options.FEATURE) {
		baseJsonSchemaDef = jsonSchemaTarget.baseJsonSchemaDefinitionForFeatureTypes();
		baseEncInfo = JsonSchemaTarget.baseJsonSchemaDefinitionForFeatureTypes_encodingInfos;
	    } else if (ci.category() == Options.OBJECT) {
		baseJsonSchemaDef = jsonSchemaTarget.baseJsonSchemaDefinitionForObjectTypes();
		baseEncInfo = JsonSchemaTarget.baseJsonSchemaDefinitionForObjectTypes_encodingInfos;
	    } else if (ci.category() == Options.DATATYPE) {
		baseJsonSchemaDef = jsonSchemaTarget.baseJsonSchemaDefinitionForDataTypes();
		baseEncInfo = JsonSchemaTarget.baseJsonSchemaDefinitionForDataTypes_encodingInfos;
	    }

	    if (baseJsonSchemaDef != null) {

		boolean applyVirtualGeneralization = true;

		for (ClassInfo supertype : ci.supertypeClasses()) {
		    if (supertype.matches(JsonSchemaConstants.RULE_CLS_VIRTUAL_GENERALIZATION)
			    && supertype.category() != Options.MIXIN) {
			applyVirtualGeneralization = false;
			break;
		    }
		}

		if (applyVirtualGeneralization) {
		    // note that the baseEncInfo may well be null, if no such info was configured
		    return baseEncInfo;
		}
	    }
	}

	return null;
    }

    private String inlineOrByReference(PropertyInfo pi) {

	String s = pi.taggedValue("inlineOrByReference");

	if (StringUtils.isBlank(s)) {
	    if (pi.matches(JsonSchemaConstants.RULE_PROP_INLINEORBYREFTAG)) {
		return pi.isAttribute() ? "inline" : "byReference";
	    } else {
		return jsonSchemaTarget.getInlineOrByRefDefault();
	    }
	} else {
	    return s;
	}
    }

    private List<JsonSchemaTypeInfo> createJsonSchemaTypeInfoForReference(PropertyInfo pi) {

	List<JsonSchemaTypeInfo> res = new ArrayList<>();

	if (pi.matches(JsonSchemaConstants.RULE_ALL_FEATURE_REFS)) {

	    SortedSet<String> refProfiles = JsonSchemaTarget.featureRefProfiles;

	    if (refProfiles.contains("rel-as-uri")) {
		res.add(createJsonSchemaTypeInfoForRelAsUri());
	    }

	    if (refProfiles.contains("rel-as-link")) {
		this.generateSCLinkObjectDefinition = true;
		JsonSchemaTypeInfo linkInfo = new JsonSchemaTypeInfo();
		linkInfo.setRef(fragmentIdentifier(this, JsonSchemaConstants.SC_LINK_OBJECT_DEF_NAME, false));
		res.add(linkInfo);
	    }

	    if (refProfiles.contains("rel-as-key")) {

		SortedSet<JsonSchemaType> featureRefIdTypes = new TreeSet<>();
		String uriTemplate = null;
		SortedSet<String> collectionIds = new TreeSet<>();
		boolean skipFeatureRef = false;
		boolean collectionInfosDefinedByMapEntry = false;

		String valueTypeName = pi.typeInfo().name;
		ClassInfo typeCi = pi.typeClass();

		if (this.mapEntryParamInfos.getMapEntry(valueTypeName,
			pi.encodingRule(JsonSchemaConstants.PLATFORM)) != null) {

		    // a map entry is defined for the value type - use it

		    if (this.mapEntryParamInfos.hasParameter(valueTypeName,
			    pi.encodingRule(JsonSchemaConstants.PLATFORM),
			    JsonSchemaConstants.ME_PARAM_COLLECTION_INFOS)) {

			collectionInfosDefinedByMapEntry = true;

			// determine feature ref id types
			String meCharCollectionIdTypes = mapEntryParamInfos.getCharacteristic(valueTypeName,
				pi.encodingRule(JsonSchemaConstants.PLATFORM),
				JsonSchemaConstants.ME_PARAM_COLLECTION_INFOS,
				JsonSchemaConstants.ME_PARAM_COLLECTION_INFOS_CHAR_COLLECTION_ID_TYPES);

			if (StringUtils.isNotBlank(meCharCollectionIdTypes)) {
			    String[] meCollectionIdTypes = StringUtils.split(meCharCollectionIdTypes, ", ");
			    for (String s : meCollectionIdTypes) {
				if ("string".equalsIgnoreCase(s)) {
				    featureRefIdTypes.add(JsonSchemaType.STRING);
				} else if ("integer".equalsIgnoreCase(s)) {
				    featureRefIdTypes.add(JsonSchemaType.INTEGER);
				} else {
				    // is checked by the configuration validator
				}
			    }
			} else {
			    // use default
			    featureRefIdTypes.add(JsonSchemaType.INTEGER);
			}

			// determine uri template
			String meCharUriTemplate = mapEntryParamInfos.getCharacteristic(valueTypeName,
				pi.encodingRule(JsonSchemaConstants.PLATFORM),
				JsonSchemaConstants.ME_PARAM_COLLECTION_INFOS,
				JsonSchemaConstants.ME_PARAM_COLLECTION_INFOS_CHAR_URI_TEMPLATE);
			if (StringUtils.isNotBlank(meCharUriTemplate)) {
			    uriTemplate = meCharUriTemplate.trim().replace("(", "{").replace(")", "}");
			} else {
			    /*
			     * No uri template defined in the mapping! Reference via rel-as-key impossible.
			     * NOTE: This is checked by the configuration validator.
			     */
			    skipFeatureRef = true;
			    result.addError(this, 138, valueTypeName);
			}

			// determine collection ids
			String meCharCollectionIds = mapEntryParamInfos.getCharacteristic(valueTypeName,
				pi.encodingRule(JsonSchemaConstants.PLATFORM),
				JsonSchemaConstants.ME_PARAM_COLLECTION_INFOS,
				JsonSchemaConstants.ME_PARAM_COLLECTION_INFOS_CHAR_COLLECTION_IDS);

			if (StringUtils.isNotBlank(meCharCollectionIds)) {
			    String[] meCollectionIds = StringUtils.split(meCharCollectionIds, ", ");
			    for (String s : meCollectionIds) {
				collectionIds.add(s);
			    }
			} else {
			    // no collection ids defined by the map entry -> no restriction available
			}

		    } else {
			/*
			 * no collection infos defined in the mapping; reference via rel-as-key not
			 * possible; not a problem for types that are expected to be encoded inline
			 * anyways, such as geometry types
			 */
			skipFeatureRef = true;
			result.addInfo(this, 137, valueTypeName);
		    }

		} else {

		    // no mapping is defined for the value type

		    // determine feature ref id types
		    featureRefIdTypes = JsonSchemaTarget.featureRefIdTypes;

		    // determine uri template
		    if (typeCi != null) {
			String tv = typeCi.taggedValue(JsonSchemaConstants.TV_COLLECTION_URI_TEMPLATE);
			if (StringUtils.isNotBlank(tv)) {
			    uriTemplate = tv.trim();
			}
		    }

		    // determine collection ids
		    if (typeCi != null) {

			SortedSet<ClassInfo> typeSet = typeCi.subtypesInCompleteHierarchy();
			typeSet.add(typeCi);
			List<ClassInfo> relevantTypes = typeSet.stream().filter(ci -> !ci.isAbstract())
				.collect(Collectors.toList());
			for (ClassInfo ci : relevantTypes) {
			    collectionIds.add(formatCollectionId(ci.name()));
			}

		    } else {
			collectionIds.add(formatCollectionId(valueTypeName));
		    }
		}

		if (!skipFeatureRef) {

		    /*
		     * Create the JSON Schema type info
		     */
		    JsonSchemaTypeInfo keyInfo = new JsonSchemaTypeInfo();
		    keyInfo.setKeyword(new XOgcRoleKeyword("reference"));

		    if (StringUtils.isNotBlank(uriTemplate)) {

			// create schema for ref external

			keyInfo.setKeyword(new XOgcUriTemplateKeyword(uriTemplate));

			if (uriTemplate.contains("{collectionId}")) {

			    // create schema for complex feature ref external

			    keyInfo.setKeyword(new TypeKeyword(JsonSchemaType.OBJECT));

			    keyInfo.setKeyword(
				    new RequiredKeyword(Arrays.asList(new String[] { "collectionId", "featureId" })));

			    PropertiesKeyword props = new PropertiesKeyword();

			    JsonSchema collectionIdSchema = new JsonSchema();
			    collectionIdSchema.type(JsonSchemaType.STRING);
			    if (collectionIds.size() > 0 && (collectionInfosDefinedByMapEntry
				    || !JsonSchemaTarget.featureRefWithAnyCollectionId)) {
				JsonString[] array = collectionIds.stream().map(s -> new JsonString(s))
					.toArray(JsonString[]::new);
				collectionIdSchema.enum_(array);
			    }
			    props.put("collectionId", collectionIdSchema);

			    props.put("featureId", new JsonSchema()
				    .type(featureRefIdTypes.toArray(new JsonSchemaType[featureRefIdTypes.size()])));

			    props.put("title", new JsonSchema().type(JsonSchemaType.STRING));

			    keyInfo.setKeyword(props);

			} else {

			    /*
			     * Create schema for simple ref external; collection id count is ignored,
			     * because the URI template does not contain the variable '{collectionId}'.
			     */

			    keyInfo.setKeyword(new TypeKeyword(featureRefIdTypes));
			}

		    } else {

			// create schema for ref

			if (collectionIds.size() == 1 && !JsonSchemaTarget.featureRefWithAnyCollectionId) {

			    // create schema for simple feature ref

			    keyInfo.setKeyword(new TypeKeyword(featureRefIdTypes));
			    keyInfo.setKeyword(new XOgcCollectionIdKeyword(collectionIds.iterator().next()));

			} else {

			    // create schema for complex feature ref

			    keyInfo.setKeyword(new TypeKeyword(JsonSchemaType.OBJECT));

			    keyInfo.setKeyword(
				    new RequiredKeyword(Arrays.asList(new String[] { "collectionId", "featureId" })));

			    PropertiesKeyword props = new PropertiesKeyword();

			    JsonSchema collectionIdSchema = new JsonSchema();
			    collectionIdSchema.type(JsonSchemaType.STRING);
			    if (collectionIds.size() > 0 && !JsonSchemaTarget.featureRefWithAnyCollectionId) {
				JsonString[] array = collectionIds.stream().map(s -> new JsonString(s))
					.toArray(JsonString[]::new);
				collectionIdSchema.enum_(array);
			    }
			    props.put("collectionId", collectionIdSchema);

			    props.put("featureId", new JsonSchema()
				    .type(featureRefIdTypes.toArray(new JsonSchemaType[featureRefIdTypes.size()])));

			    props.put("title", new JsonSchema().type(JsonSchemaType.STRING));

			    keyInfo.setKeyword(props);
			}
		    }

		    res.add(keyInfo);
		}
	    }

	} else {

	    if (jsonSchemaTarget.byReferenceJsonSchemaDefinition().isPresent()) {
		JsonSchemaTypeInfo byRefInfo = new JsonSchemaTypeInfo();
		byRefInfo.setRef(jsonSchemaTarget.byReferenceJsonSchemaDefinition().get());
		res.add(byRefInfo);
	    } else {
		res.add(createJsonSchemaTypeInfoForRelAsUri());
	    }

	}

	return res;
    }

    private String formatCollectionId(String id) {
	// NOTE: In future, we may have different ways to format the collection id
	return id.toLowerCase(Locale.ENGLISH);
    }

    private JsonSchemaTypeInfo createJsonSchemaTypeInfoForRelAsUri() {
	JsonSchemaTypeInfo res = new JsonSchemaTypeInfo();
	res.setSimpleType(JsonSchemaType.STRING);
	res.setKeyword(new FormatKeyword(JsonSchemaTarget.byReferenceFormat));
	return res;
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
	String unit = null;

	if (typeInfoOpt.isPresent()) {

	    JsonSchemaTypeInfo typeInfo = typeInfoOpt.get();

	    boolean byReferenceOnly = false;

	    if ((pi.categoryOfValue() == Options.FEATURE
		    || (pi.categoryOfValue() == Options.OBJECT && !valueTypeIsBasicType(pi)))
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
		    typeOptions.addAll(createJsonSchemaTypeInfoForReference(pi));
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

		    if (typeInfo.isMeasure() && pi.matches(JsonSchemaConstants.RULE_PROP_MEASURE)) {

			unit = pi.taggedValue("unit");
			if (StringUtils.isNotBlank(unit)) {
			    typeInfo.setRef(null);
			    typeInfo.setSimpleType(JsonSchemaType.NUMBER);
			} else {
			    typeInfo.setRef(JsonSchemaTarget.measureObjectUri);
			    typeInfo.setSimpleType(null);
			}
		    }

		    typeOptions.add(typeInfo);
		}
	    }
	}

	/*
	 * At this point, all type infos should be available (so that when converting
	 * multiplicity, it can be determined if "items" shall be created).
	 */

	JsonSchema jsProp = new JsonSchema();

	if (pi.matches(JsonSchemaConstants.RULE_ALL_DOCUMENTATION)) {
	    this.annotationGenerator.applyAnnotations(jsProp, pi);
	}

	if (StringUtils.isNotBlank(unit)) {
	    jsProp.add(new GenericAnnotationKeyword("unit", new JsonString(unit)));
	}

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
			typeInfoOpt, pi, byReferenceAllowed, parentForTypeSchema);
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

		if (valueTypeIsBasicType(pi)) {
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

	if (tci == null) {
	    return false;
	} else {
	    Optional<JsonSchemaDocument> jsdOpt = jsonSchemaTarget.jsonSchemaDocument(tci);
	    if (jsdOpt.isEmpty()) {
		return false;
	    } else {
		return jsdOpt.get().hasBasicTypeDefinition(tci);
	    }
	}
    }

    private Optional<JsonSchemaTypeInfo> getBasicValueTypeDefinition(PropertyInfo pi) {

	ClassInfo tci = model.classByIdOrName(pi.typeInfo());

	JsonSchemaTypeInfo result;

	if (tci == null) {
	    result = null;
	} else {
	    Optional<JsonSchemaDocument> jsdOpt = jsonSchemaTarget.jsonSchemaDocument(tci);
	    if (jsdOpt.isEmpty()) {
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
		if (otherTypeOption.hasSimpleType()) {
		    otherTypeSchema.type(otherTypeOption.getSimpleType());
		}
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
	    Optional<JsonSchemaTypeInfo> jsonTypeInfoForAssociationRoleValueType, PropertyInfo associationRole,
	    boolean byReferenceAllowedForAssociationRole, JsonSchema parentForTypeSchema) {

	String associationRoleName = associationRole.name();

	// create schema to restrict association role copy)
	JsonSchema associationClassRoleCopyRestrictionSchema = new JsonSchema();
	associationClassRoleCopyRestrictionSchema.type(JsonSchemaType.OBJECT);

	JsonSchema roleCopyRestrictionSchema = new JsonSchema();
	roleCopyRestrictionSchema.type(JsonSchemaType.OBJECT);

	JsonSchema roleCopyTypeRestrictionSchema = new JsonSchema();
	List<JsonSchemaTypeInfo> additionalJsonTypes = new ArrayList<>();
	if (byReferenceAllowedForAssociationRole) {
	    additionalJsonTypes.addAll(createJsonSchemaTypeInfoForReference(associationRole));
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

    private Optional<JsonSchemaTypeInfo> identifyJsonSchemaType(ClassInfo ci) {
	return identifyJsonSchemaType(ci.name(), ci.id(), ci.encodingRule(JsonSchemaConstants.PLATFORM));
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
		    String schemaRef = jsonSchemaDefinitionReference(jsd, valueType);
		    jsTypeInfo.setRef(schemaRef);
		}
	    }
	}

	return Optional.ofNullable(jsTypeInfo);
    }

    public boolean isFeatureCollectionDocument() {
	return this.representedPackage == jsonSchemaTarget.getSchemaForFeatureCollection();
    }

    public void write() {

	if (this.generateSCLinkObjectDefinition) {
	    generateSCLinkObjectDefinition();
	}

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

    private void generateSCLinkObjectDefinition() {

	JsonSchema js = new JsonSchema();

	js.title("link object");
	js.description("definition of a link object");
	js.type(JsonSchemaType.OBJECT);
	js.required("href");
	js.property("href", new JsonSchema().type(JsonSchemaType.STRING)
		.description("Supplies the URI to a remote resource (or resource fragment)."));
	js.property("rel",
		new JsonSchema().type(JsonSchemaType.STRING).description("The type or semantics of the relation."));
	js.property("type", new JsonSchema().type(JsonSchemaType.STRING).description(
		"A hint indicating what the media type of the result of dereferencing the link should be."));
	js.property("hreflang", new JsonSchema().type(JsonSchemaType.STRING)
		.description("A hint indicating what the language of the result of dereferencing the link should be."));
	js.property("title", new JsonSchema().type(JsonSchemaType.STRING).description(
		"Used to label the destination of a link such that it can be used as a human-readable identifier."));
	js.property("length", new JsonSchema().type(JsonSchemaType.INTEGER));

	addToRootSchema(JsonSchemaConstants.SC_LINK_OBJECT_DEF_NAME, js);
    }

    public void createCollectionDefinitions() {

	// create uniform collections
	Collection<ClassInfo> classesForUniformCollections = new ArrayList<>();

	if (!JsonSchemaTarget.featureCollectionOnly) {
	    if (this.isSingularCollectionsSchemaDocument()) {
		classesForUniformCollections = jsonSchemaTarget.getAllEncodedTypes().stream()
			.filter(type -> type.category() == Options.FEATURE
				&& type.matches(JsonSchemaConstants.RULE_CLS_COLLECTIONS_BASED_ON_ENTITY_TYPE))
			.collect(Collectors.toSet());
	    } else {
		classesForUniformCollections = this.classesByName.values();
	    }
	}

	for (ClassInfo ci : classesForUniformCollections) {

	    if (ci.category() == Options.FEATURE
		    && ci.matches(JsonSchemaConstants.RULE_CLS_COLLECTIONS_BASED_ON_ENTITY_TYPE)) {

		Set<ClassInfo> collectionMembers = ci.subtypesInCompleteHierarchy().stream()
			.filter(subtype -> !subtype.isAbstract()
				&& subtype.matches(JsonSchemaConstants.RULE_CLS_COLLECTIONS_BASED_ON_ENTITY_TYPE))
			.collect(Collectors.toSet());
		if (!ci.isAbstract() || collectionMembers.isEmpty()) {
		    collectionMembers.add(ci);
		}
		createCollectionDefinition(ci.name() + "Collection", collectionMembers, true,
			new JsonSchema().ref(identifyJsonSchemaType(ci).get().getRef()));
	    }
	}

	if (this.isSingularCollectionsSchemaDocument() || this.isFeatureCollectionDocument()) {
	    // create general FeatureCollection schema
	    Set<ClassInfo> allEncodedTypes = jsonSchemaTarget.getAllEncodedTypes();
	    Set<ClassInfo> featureCollectionMembers = allEncodedTypes.stream()
		    .filter(type -> type.category() == Options.FEATURE && !type.isAbstract()
			    && type.matches(JsonSchemaConstants.RULE_CLS_COLLECTIONS_BASED_ON_ENTITY_TYPE))
		    .collect(Collectors.toSet());
	    if (!featureCollectionMembers.isEmpty()) {
		createCollectionDefinition("FeatureCollection", featureCollectionMembers,
			JsonSchemaTarget.preventUnknownTypesInFeatureCollection, new JsonSchema(false));

		if (this.isSingularCollectionsSchemaDocument() && JsonSchemaTarget.featureCollectionOnly) {
		    rootSchema.ref(fragmentIdentifier(this, "FeatureCollection", true));
		}
	    }
	}
    }

    /**
     * @return <code>true</code>, if this is the schema document where all
     *         collection schema definitions are created; else <code>false</code>
     */
    private boolean isSingularCollectionsSchemaDocument() {
	return this.isSingularCollectionsSchema;
    }

    private void createCollectionDefinition(String defName, Set<ClassInfo> collectionMembersIn,
	    boolean validateUnknownMembers, JsonSchema schemaForUnknownMembers) {

	/*
	 * Check availability of encoding infos, especially the presence of entity type
	 * member, for all collection members.
	 */
	List<ClassInfo> collectionMembersWithEncodingInfos = new ArrayList<>();
	for (ClassInfo ci : collectionMembersIn) {
	    EncodingInfos ei = JsonSchemaTarget.encodingInfosByCi.get(ci);
	    if (ei == null || ei.getEntityTypeMemberPath().isEmpty()) {
		result.addError(this, 126, ci.name(), defName);
	    } else {
		collectionMembersWithEncodingInfos.add(ci);
	    }
	}

	if (collectionMembersWithEncodingInfos.isEmpty()) {
	    result.addWarning(this, 111, defName);
	    return;
	}

	List<ClassInfo> collectionMembers = collectionMembersWithEncodingInfos.stream()
		.sorted((m1, m2) -> m1.name().compareTo(m2.name())).collect(Collectors.toList());

	JsonSchema js = new JsonSchema();
	addToRootSchema(defName, js);

	JsonSchema contentSchema = js;
	contentSchema.anchor(defName);
	String[] featuresMemberPath = null;

	if (StringUtils.isNotBlank(JsonSchemaTarget.baseJsonSchemaDefinitionForCollections)) {
	    contentSchema.allOf(new JsonSchema().ref(JsonSchemaTarget.baseJsonSchemaDefinitionForCollections));
	    JsonSchema newContentSchema = new JsonSchema();
	    contentSchema.allOf(newContentSchema);
	    contentSchema = newContentSchema;
	    featuresMemberPath = new String[] { "features" };
	}

	if (collectionMembers.size() == 1) {

	    // no need for entity type checks
	    JsonSchema featuresSchema = getOrCreatePropertyPath(contentSchema, featuresMemberPath, true);
	    featuresSchema.type(JsonSchemaType.ARRAY).items(
		    new JsonSchema().ref(identifyJsonSchemaType(collectionMembers.iterator().next()).get().getRef()));
	} else {

	    boolean collectionsWithTopLevelEntityType = collectionMembers.stream()
		    .filter(m -> m.matches(JsonSchemaConstants.RULE_CLS_COLLECTIONS_WITH_TOP_LEVEL_ENTITY_TYPE))
		    .findAny().isPresent();

	    if (collectionsWithTopLevelEntityType) {

		/*
		 * here, we generate the JSON Schema with the if-then-else check, and populate
		 * the if- as well as the then-constraints
		 * 
		 * The else-constraint is created but left empty here. It is only set as the
		 * contents schema, to be populated with the JSON Schema that is created for
		 * object-level entity type member checks (which follows later on).
		 */

		contentSchema.if_(new JsonSchema().required("featureType"));

		JsonSchema thenSchema = new JsonSchema();
		contentSchema.then(thenSchema);

		for (ClassInfo ci : collectionMembers) {

		    JsonSchema memberJs = new JsonSchema();
		    thenSchema.allOf(memberJs);

		    // if
		    JsonSchema ifMemberJs = new JsonSchema();
		    memberJs.if_(ifMemberJs);

		    JsonSchema ifMemberFeatureTypeJs = getOrCreatePropertyPath(ifMemberJs,
			    new String[] { "featureType" }, false);
		    ifMemberFeatureTypeJs.const_(new JsonString(ci.name()));

		    // then
		    JsonSchema thenMemberJs = new JsonSchema();
		    memberJs.then(thenMemberJs);

		    JsonSchema thenMemberFeaturesJs = getOrCreatePropertyPath(thenMemberJs, new String[] { "features" },
			    false);
		    thenMemberFeaturesJs.type(JsonSchemaType.ARRAY)
			    .items(new JsonSchema().ref(identifyJsonSchemaType(ci).get().getRef()));
		}

		if (validateUnknownMembers) {

		    JsonSchema unknownJs = new JsonSchema();
		    thenSchema.allOf(unknownJs);

		    JsonString[] collectionMemberNames = (JsonString[]) collectionMembers.stream()
			    .map(m -> new JsonString(m.name())).toArray(size -> new JsonString[size]);

		    // if
		    JsonSchema ifUnknownJs = new JsonSchema();
		    unknownJs.if_(ifUnknownJs);

		    JsonSchema ifNotUnknownJs = new JsonSchema();
		    ifUnknownJs.not(ifNotUnknownJs);

		    JsonSchema ifNotUnknownFeatureTypeJs = getOrCreatePropertyPath(ifNotUnknownJs,
			    new String[] { "featureType" }, false);
		    ifNotUnknownFeatureTypeJs.enum_(collectionMemberNames);

		    // then
		    JsonSchema thenUnknownJs = new JsonSchema();
		    unknownJs.then(thenUnknownJs);

		    JsonSchema thenUnknownFeaturesJs = getOrCreatePropertyPath(thenUnknownJs,
			    new String[] { "features" }, false);
		    thenUnknownFeaturesJs.type(JsonSchemaType.ARRAY).items(schemaForUnknownMembers);
		}

		JsonSchema elseSchema = new JsonSchema();
		contentSchema.else_(elseSchema);
		contentSchema = elseSchema;
	    }

	    /*
	     * Now create the items check for objects contained in the features member (see
	     * featuresMemberPath), based upon the entity type member within each of these
	     * objects.
	     */

	    JsonSchema featuresMemberJs = getOrCreatePropertyPath(contentSchema, featuresMemberPath, false);
	    featuresMemberJs.type(JsonSchemaType.ARRAY);
	    JsonSchema itemsSchema = new JsonSchema();
	    featuresMemberJs.items(itemsSchema);

	    for (ClassInfo ci : collectionMembers) {

		JsonSchema memberJs = new JsonSchema();
		itemsSchema.allOf(memberJs);

		// if
		JsonSchema ifMemberJs = new JsonSchema();
		memberJs.if_(ifMemberJs);

		String entityTypeMemberPath = JsonSchemaTarget.encodingInfosByCi.get(ci).getEntityTypeMemberPath()
			.get();

		JsonSchema ifMemberEntityTypeJs = getOrCreatePropertyPath(ifMemberJs, entityTypeMemberPath.split("/"),
			false);
		ifMemberEntityTypeJs.const_(new JsonString(ci.name()));

		// then
		memberJs.then(new JsonSchema().ref(identifyJsonSchemaType(ci).get().getRef()));
	    }

	    if (validateUnknownMembers) {

		// group classes by entity type member path
		SortedMap<String, List<ClassInfo>> classesByEntityTypeMemberPath = collectionMembers.stream()
			.collect(Collectors.groupingBy(
				ci -> JsonSchemaTarget.encodingInfosByCi.get(ci).getEntityTypeMemberPath().get(),
				TreeMap::new, Collectors.toList()));

		boolean isSingleEntityTypeMemberPath = classesByEntityTypeMemberPath.size() == 1;

		JsonSchema unknownJs = new JsonSchema();
		itemsSchema.allOf(unknownJs);

		// if
		JsonSchema ifUnknownJs = new JsonSchema();
		unknownJs.if_(ifUnknownJs);

		JsonSchema ifNotUnknownJs = new JsonSchema();
		ifUnknownJs.not(ifNotUnknownJs);

		if (isSingleEntityTypeMemberPath) {

		    Entry<String, List<ClassInfo>> e = classesByEntityTypeMemberPath.entrySet().iterator().next();

		    String entityTypeMemberPath = e.getKey();
		    List<ClassInfo> collectionMembersForGroup = e.getValue();

		    createUnknownCollectionMemberEntityTypeMemberCheck(ifNotUnknownJs, entityTypeMemberPath.split("/"),
			    collectionMembersForGroup);
		} else {

		    for (Entry<String, List<ClassInfo>> e : classesByEntityTypeMemberPath.entrySet()) {

			JsonSchema choiceJs = new JsonSchema();
			ifNotUnknownJs.oneOf(choiceJs);

			String entityTypeMemberPath = e.getKey();
			List<ClassInfo> collectionMembersForGroup = e.getValue();

			createUnknownCollectionMemberEntityTypeMemberCheck(choiceJs, entityTypeMemberPath.split("/"),
				collectionMembersForGroup);
		    }
		}

		// then
		unknownJs.then(schemaForUnknownMembers);
	    }
	}
    }

    private void createUnknownCollectionMemberEntityTypeMemberCheck(JsonSchema baseJs,
	    String[] entityTypeMemberPathSegments, List<ClassInfo> collectionMembers) {

	JsonString[] collectionMemberNames = (JsonString[]) collectionMembers.stream()
		.map(m -> new JsonString(m.name())).toArray(size -> new JsonString[size]);

	JsonSchema entityTypeJs = getOrCreatePropertyPath(baseJs, entityTypeMemberPathSegments, false);
	entityTypeJs.enum_(collectionMemberNames);
    }

    @Override
    public String message(int mnr) {

	switch (mnr) {

	case 0:
	    return "Context: class '$1$'";
	case 1:
	    return "Context: property '$1$'";
	case 2:
	    return "Context: $1$";
	case 9:
	    return "??Property '$1$' of class '$2$' is not encoded.";

	case 100:
	    return "Exception occurred while writing JSON Schema to file: $1$. Exception message is: $2$.";
	case 101:
	    return "Restricting facet '$4$' of basic type '$1$' defined by tagged value '$3$' could not be parsed as an integer. Found value: $2$. The facet will be ignored.";
	case 102:
	    return "Restricting facet '$4$' of basic type '$1$' defined by tagged value '$3$' has a negative integer value ($2$), which is not allowed for the facet. The facet will be ignored.";
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
	    return "Could not create collection schema '$1$' because no member schemas are available for it. Check if relevant feature types exist (are encoded in JSON Schema, have an entity type member, are not abstract [in case of the general 'FeatureCollection'], and match "
		    + JsonSchemaConstants.RULE_CLS_COLLECTIONS_BASED_ON_ENTITY_TYPE + ")";
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
	    return "";
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
	case 123:
	    return "??Could not parse value '$1$' as double / number while creating annotation '$2$' for model element $3$. The value will be ignored.";
	case 124:
	    return "??Could not parse value '$1$' as integer while creating annotation '$2$' for model element $3$. The value will be ignored.";
	case 125:
	    return "??Class '$1$' gets constraints from two external schemas, one from a base schema or supertype mapping, one from the mapping of supertype '$2$'. The encodings of these two schemas both define an $3$ member, but in different ways, which leads to ambiguity. The encoding infos from supertype '$2$' are ignored in the computation of encoding infos for class '$1$'.";
	case 126:
	    return "??Cannot add class '$1$' to collection '$2$', because the class has no entity type member.";
	case 127:
	    return "Property '$1$' of type '$2$' has been identified as primary geometry of the type. However, the maximum multiplicity of that property is greater than 1. The property is mapped to the \"geometry\" member (and maybe the JSON-FG \"place\" member as well), which can only have a single value. The multiplicity of the property will therefore be ignored.";
	case 128:
	    return "Property '$1$' of type '$2$' has been identified as primary geometry of the type. However, no JSON Schema type could be identified for the property. Therefore, no restriction is encoded for the \"geometry\" member (and maybe the JSON-FG \"place\" member as well). Ensure that a map entry is defined for the value type '$3$' of property '$1$'.";
	case 129:
	    return "Property '$1$' of type '$2$' has been identified as primary place of the type. However, the maximum multiplicity of that property is greater than 1. The property is mapped to the \"place\" member, which can only have a single value. The multiplicity of the property will therefore be ignored.";
	case 130:
	    return "Property '$1$' of type '$2$' has been identified as primary place of the type. However, no JSON Schema type could be identified for the property. Therefore, no restriction is encoded for the \"place\" member. Ensure that a map entry is defined for the value type '$3$' of property '$1$'.";
	case 131:
	    return "The primary instant property of feature type '$1$' was determined to be '$2$'. The feature type owns another property '$3$', which is marked to be the primary instant property. Property '$3$' will not be encoded, and ignored when encoding the time restriction.";
	case 132:
	    return "The primary interval start property of feature type '$1$' was determined to be '$2$'. The feature type owns another property '$3$', which is marked to be the primary interval start property. Property '$3$' will not be encoded, and ignored when encoding the time restriction.";
	case 133:
	    return "The primary interval end property of feature type '$1$' was determined to be '$2$'. The feature type owns another property '$3$', which is marked to be the primary interval end property. Property '$3$' will not be encoded, and ignored when encoding the time restriction.";
	case 134:
	    return "The primary interval property of feature type '$1$' was determined to be '$2$'. The feature type owns another property '$3$', which is marked to be the primary interval property. Property '$3$' will not be encoded, and ignored when encoding the time restriction.";
	case 135:
	    return "The primary interval of feature type '$1$' is defined both through an interval property ('$2$') as well as by start and/or end property (start: '$3$', end: '$4$'). This represents an inconsistency. All of these properties will not be encoded, and ignored when encoding the time restriction.";
	case 136:
	    return "??JSON Schema document for type '$1$' could not be identified. Property definitions that would reference this type will get a reference to definition '$2$'";
	case 137:
	    return "??A property with value type '$1$' shall be encoded by reference (and maybe inline). The reference shall be encoded using feature ref profile 'rel-as-key'. A map entry is defined for the type, but the mapping does not contain collection infos. The by reference case with 'rel-as-key' encoding is ignored for the property. Consider adding collection infos to the map entry, or a pure inline encoding of the property.";
	case 138:
	    return "??A property with value type '$1$' shall be encoded by reference (and maybe inline). The reference shall be encoded using feature ref profile 'rel-as-key'. A map entry is defined for the type. The map entry contains collection infos, but not the required value for characteristic "
		    + JsonSchemaConstants.ME_PARAM_COLLECTION_INFOS_CHAR_URI_TEMPLATE
		    + ". The by reference case with 'rel-as-key' encoding cannot be created for the property. Add the uri template to the collection infos of the map entry.";
	case 139:
	    return "Value type '$1$' of primary geometry property '$2$' is not one of the GeoJSON compatible geometry types defined via target parameter "
		    + JsonSchemaConstants.PARAM_GEOJSON_COMPATIBLE_GEOMETRY_TYPES
		    + ". No restriction will be encoded for the \"geometry\" member.";

	default:
	    return "(" + JsonSchemaDocument.class.getName() + ") Unknown message with number: " + mnr;
	}
    }
}
