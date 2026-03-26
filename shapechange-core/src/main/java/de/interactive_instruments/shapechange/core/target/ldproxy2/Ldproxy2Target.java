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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */

package de.interactive_instruments.shapechange.core.target.ldproxy2;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;

import de.ii.ldproxy.cfg.LdproxyCfgWriter;
import de.ii.ogcapi.collections.queryables.domain.QueryablesConfiguration.PathSeparator;
//import de.ii.ogcapi.features.jsonfg.domain.JsonFgConfiguration.OPTION;
import de.ii.ogcapi.features.search.domain.ImmutableStoredQueryExpression;
import de.ii.ogcapi.foundation.domain.ImmutableOgcApiDataV2;
import de.ii.xtraplatform.codelists.domain.ImmutableCodelist;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.EpsgCrs.Force;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.sql.domain.ImmutableFeatureProviderSqlData;
import de.ii.xtraplatform.tiles.domain.ImmutableTileProviderFeaturesData;
import de.interactive_instruments.shapechange.core.MapEntryParamInfos;
import de.interactive_instruments.shapechange.core.MessageSource;
import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ProcessMapEntry;
import de.interactive_instruments.shapechange.core.RuleRegistry;
import de.interactive_instruments.shapechange.core.ShapeChangeAbortException;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.ShapeChangeResult.MessageContext;
import de.interactive_instruments.shapechange.core.model.ClassInfo;
import de.interactive_instruments.shapechange.core.model.Model;
import de.interactive_instruments.shapechange.core.model.PackageInfo;
import de.interactive_instruments.shapechange.core.model.PropertyInfo;
import de.interactive_instruments.shapechange.core.target.SingleTarget;
import de.interactive_instruments.shapechange.core.target.TargetUtil;
import de.interactive_instruments.shapechange.core.target.ldproxy2.provider.LdpProvider;
import de.interactive_instruments.shapechange.core.target.ldproxy2.provider.LdpSourcePathProvider;
import de.interactive_instruments.shapechange.core.target.ldproxy2.provider.coretable.LdpCoretableProvider;
import de.interactive_instruments.shapechange.core.target.ldproxy2.provider.coretable.LdpCoretableSourcePathProvider;
import de.interactive_instruments.shapechange.core.target.ldproxy2.provider.sql.LdpSqlProvider;
import de.interactive_instruments.shapechange.core.target.ldproxy2.provider.sql.LdpSqlSourcePathProvider;
import de.interactive_instruments.shapechange.core.target.ldproxy2.service.LdpBuildingBlockFeaturesGeoJsonBuilder;
import de.interactive_instruments.shapechange.core.target.ldproxy2.service.LdpBuildingBlockFeaturesGmlBuilder;
import de.interactive_instruments.shapechange.core.target.ldproxy2.service.LdpBuildingBlockFeaturesJsonFgBuilder;
import de.interactive_instruments.shapechange.core.target.ldproxy2.storedquery.LdproxyStoredQueryDefinitions;
import de.interactive_instruments.shapechange.core.target.sql_encoding_util.SqlEncodingInfos;
import de.interactive_instruments.shapechange.core.target.xml_encoding_util.XmlEncodingInfos;
import de.interactive_instruments.shapechange.core.util.GenericValueTypeUtil;
import de.interactive_instruments.shapechange.core.util.XMLUtil;
import shadow.com.fasterxml.jackson.annotation.JsonInclude.Include;
import shadow.com.fasterxml.jackson.databind.ObjectMapper;
import shadow.com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class Ldproxy2Target implements SingleTarget, MessageSource {

    public static Model model = null;

    private static boolean initialised = false;
    protected static boolean diagnosticsOnly = false;
    protected static int numberOfEncodedSchemas = 0;

//    /**
//     * NOTE: If not set via the configuration, the default applies which is
//     * {@value Options#DERIVED_DOCUMENTATION_DEFAULT_TEMPLATE}.
//     */
//    protected static String documentationTemplate = null;
//    /**
//     * NOTE: If not set via the configuration, the default applies which is
//     * {@value Options#DERIVED_DOCUMENTATION_DEFAULT_NOVALUE}.
//     */
//    protected static String documentationNoValue = null;

    public static List<EpsgCrs> additionalCrs = new ArrayList<>();
    public static String associativeTableColumnSuffix = null; // default: value of primaryKeyColumn parameter
    public static String cfgTemplatePath = "https://shapechange.net/resources/templates/ldproxy2/cfgTemplate.yml";
    public static String codeTargetTagName = Ldproxy2Constants.DEFAULT_CODE_TARGET_TAG_NAME_VALUE;
    public static String dateFormat = null; // no default value
    public static String dateTimeFormat = null; // no default value
    public static String descriptionTemplate = "[[definition]]";
    public static String descriptorNoValue = "";
    public static List<String> dropSqlEncodingInfosForTypes = new ArrayList<>();
    public static boolean embeddingForFeatureRefs = false;
    public static boolean enableFragments = false;
    public static boolean enableCodelists = false;
    public static boolean enableCrs = false;
    public static boolean enableFeaturesGml = false;
    public static boolean enableFeaturesGeoJson = true;
    public static boolean enableFeaturesJsonFg = false;
    public static boolean enableFeaturesResultType = false;
    public static boolean enableFilter = false;
    public static boolean enableProjections = false;
    public static boolean enableSorting = false;
    public static List<String> sortingIncluded = new ArrayList<>();
    public static List<String> sortingExcluded = new ArrayList<>();
    public static PathSeparator sortingPathSeparator = null;

    public static Force forceAxisOrder = Force.NONE;
    public static String foreignKeyColumnSuffix = "";
    public static String reflexiveRelationshipFieldSuffix = null;
    public static String foreignKeyColumnSuffixDatatype = "";
    public static String foreignKeyColumnSuffixCodelist = "";
    public static SortedSet<String> genericValueTypes = null;
//    public static Boolean jsonFgCoordRefSys = null;
    public static String jsonFgFeatureType = null;
//    public static List<OPTION> jsonFgIncludeInGeoJson = new ArrayList<>();

    public static String collectionIdFormat = "lowerCase";

    public static boolean linearizeCurves = false;

    public static String labelTemplate = "[[alias]]";
    public static int maxNameLength = 63;
    public static ZoneId nativeTimeZone = null;
    public static String objectIdentifierName = "oid";
    public static String primaryKeyColumn = "id";
    public static SortedSet<String> queryablesFromConfig = new TreeSet<>();
    public static String serviceConfigTemplatePathString = null;
    public static String serviceDescription = "FIXME";
    public static String serviceLabel = "FIXME";
    public static int srid = 4326;
    public static boolean sridConfigured = false;
    public static String uomTvName = null;
    public static String measureValueLabel = null;
    public static String measureUomLabel = null;
    public static SqlEncodingInfos sqlEncodingInfos = new SqlEncodingInfos();
    public static LdproxyStoredQueryDefinitions storedQueryDefinitions = new LdproxyStoredQueryDefinitions();
    public static String providerConfigLabelTemplate = null;

    public static boolean propertyIdByTaggedValue = false;
    public static String taggedValueForPropertyId = null;

    public static SortedSet<String> dbSchemaNames = new TreeSet<String>();

    public static String coretable = "features";
    public static String coretablePkColumn = "pk";
    public static String coretableIdColumn = null;
    public static Type coretableIdColumnLdproxyType = Type.INTEGER;
    public static String coretableFeatureTypeColumn = "featuretype";
    public static String coretableGeometryColumn = "geom";
    /*
     * 2023-11-23 JE: Can be re-activated in the future, when /featureId in
     * JSON-encoded references within the coretable approach is affirmed.
     */
//    public static boolean coretableJsonFeatureRefWithAnyCollectionId = false;

    public static boolean enableTiles = false;
    public static boolean tilesetDefaultSparse = false;
    public static double tilesetDefaultCenterLon = 0.0;
    public static double tilesetDefaultCenterLat = 0.0;
    public static int tilesetDefaultMinLevelWebMercatorQuad = 0;
    public static int tilesetDefaultMaxLevelWebMercatorQuad = 24;
    public static int tilesetDefaultDefaultLevelWebMercatorQuad = 0;
    public static int dynamicTilesCacheSeededMaxLevelWebMercatorQuad = 10;

    public static String coretableRelationsTable = "references";
    public static String coretableSourceColumn = null;
    public static String coretableRefColumn = "ref";
    public static String coretableRelationNameColumn = "rel";
    public static String coretableInverseRelationNameColumn = "rel_inv";
    public static String coretableVersion = null;
    public static String coretableVersionColumn = "version";

    public static boolean isUnitTest = false;

    public static String outputDirectory = null;
    public static File dataDirectoryFile = null;
    public static String mainId = null;
    public static PackageInfo mainAppSchema = null;

    /**
     * Contains information parsed from the 'param' attributes of each map entry
     * defined for this target.
     */
    public static MapEntryParamInfos mapEntryParamInfos = null;

    static List<ClassInfo> objectFeatureMixinAndDataTypes = new ArrayList<>();
    static SortedSet<ClassInfo> codelistsAndEnumerations = new TreeSet<>();

    public static LdproxyCfgWriter cfg = null;

    public static LdpBuildingBlockFeaturesGmlBuilder bbGmlBuilder = null;
    public static LdpBuildingBlockFeaturesGeoJsonBuilder bbGeoJsonBuilder = null;
    public static LdpBuildingBlockFeaturesJsonFgBuilder bbJsonFgBuilder = null;

    /* ------ */
    /*
     * Non-static fields
     */
    public ShapeChangeResult result = null;
    Options options = null;

    private PackageInfo schema = null;
    private boolean schemaNotEncoded = false;

    private Map<ClassInfo, LdpSpecialPropertiesInfo> specialPropertiesInfoByCi = new HashMap<>();

    @Override
    public void initialise(PackageInfo pi, Model m, Options o, ShapeChangeResult r, boolean diagOnly)
	    throws ShapeChangeAbortException {

	schema = pi;
	model = m;
	options = o;
	result = r;
	diagnosticsOnly = diagOnly;

	if (!LdpInfo.isEncoded(schema)) {

	    schemaNotEncoded = true;
	    result.addInfo(this, 7, schema.name());
	    return;

	} else {

	    numberOfEncodedSchemas++;

	    if (schema.matches(Ldproxy2Constants.RULE_ALL_SCHEMAS)) {
		String dbSchemaName = schema.taggedValue("sqlSchema");
		if (StringUtils.isNotBlank(dbSchemaName)) {
		    dbSchemaNames.add(dbSchemaName.trim());
		}
	    }
	}

	if (!initialised) {

	    initialised = true;

	    dbSchemaNames.add("public");

	    outputDirectory = options.parameter(this.getClass().getName(), "outputDirectory");
	    if (outputDirectory == null)
		outputDirectory = options.parameter("outputDirectory");
	    if (outputDirectory == null)
		outputDirectory = options.parameter(".");

	    mainAppSchema = TargetUtil.findMainSchemaForSingleTargets(model.selectedSchemas(), o, r);
	    if (mainAppSchema == null) {
		result.addWarning(this, 128, pi.name());
		mainAppSchema = pi;
	    }

	    propertyIdByTaggedValue = mainAppSchema.matches(Ldproxy2Constants.RULE_ALL_PROPIDBYTV);

	    String mainIdDefault = mainAppSchema.name().replaceAll("\\W", "_").toLowerCase(Locale.ENGLISH);
	    mainId = options.parameterAsString(this.getClass().getName(), Ldproxy2Constants.PARAM_API_ID, mainIdDefault,
		    false, true);

	    isUnitTest = options.parameterAsBoolean(this.getClass().getName(), "_unitTestOverride", false);

	    // change the default documentation template?
//	    documentationTemplate = options.parameter(this.getClass().getName(),
//		    Ldproxy2Constants.PARAM_DOCUMENTATION_TEMPLATE);
//	    documentationNoValue = options.parameter(this.getClass().getName(),
//		    Ldproxy2Constants.PARAM_DOCUMENTATION_NOVALUE);

	    collectionIdFormat = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_COLLECTION_ID_FORMAT, "lowerCase", false, true);

	    cfgTemplatePath = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_CFG_TEMPLATE_PATH,
		    "https://shapechange.net/resources/templates/ldproxy2/cfgTemplate.yml", false, true);

	    codeTargetTagName = options.getCurrentProcessConfig().parameterAsString(
		    Ldproxy2Constants.PARAM_CODE_TARGET_TAG_NAME, Ldproxy2Constants.DEFAULT_CODE_TARGET_TAG_NAME_VALUE,
		    false, true);

	    dateFormat = options.parameterAsString(this.getClass().getName(), Ldproxy2Constants.PARAM_DATE_FORMAT, null,
		    false, true);

	    dateTimeFormat = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_DATE_TIME_FORMAT, null, false, true);

	    descriptionTemplate = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_DESCRIPTION_TEMPLATE, "[[definition]]", false, true);

	    descriptorNoValue = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_DESCRIPTOR_NO_VALUE, "", false, true);

	    dropSqlEncodingInfosForTypes = options.parameterAsStringList(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_DROP_SQL_ENCODING_INFOS_FOR_TYPES, null, true, true);

	    String forceAxisOrderString = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_FORCE_AXIS_ORDER, "NONE", false, true);
	    forceAxisOrder = Force.valueOf(forceAxisOrderString);

	    foreignKeyColumnSuffix = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_FK_COLUMN_SUFFIX, "", false, true);

	    reflexiveRelationshipFieldSuffix = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_REFLEXIVE_REL_FIELD_SUFFIX, null, true, true);

	    foreignKeyColumnSuffixDatatype = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_FK_COLUMN_SUFFIX_DATATYPE, "", false, true);

	    foreignKeyColumnSuffixCodelist = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_FK_COLUMN_SUFFIX_CODELIST, "", false, true);

	    genericValueTypes = new TreeSet<>(options.parameterAsStringList(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_GENERIC_VALUE_TYPES, null, false, true));

	    labelTemplate = options.parameterAsString(this.getClass().getName(), Ldproxy2Constants.PARAM_LABEL_TEMPLATE,
		    "[[alias]]", false, true);

	    taggedValueForPropertyId = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_PROP_ID_TAGGED_VALUE, null, false, true);
	    if (propertyIdByTaggedValue && StringUtils.isBlank(taggedValueForPropertyId)) {
		result.addError(this, 138, mainAppSchema.name());
	    }

	    maxNameLength = options.parameterAsInteger(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_MAX_NAME_LENGTH, 63);

	    String nativeTimeZoneParamValue = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_NATIVE_TIME_ZONE, null, false, true);
	    if (nativeTimeZoneParamValue != null) {
		nativeTimeZone = ZoneId.of(nativeTimeZoneParamValue);
	    }

	    objectIdentifierName = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_OBJECT_IDENTIFIER_NAME, "oid", false, true);

	    primaryKeyColumn = options.parameterAsString(this.getClass().getName(), Ldproxy2Constants.PARAM_PK_COLUMN,
		    "id", false, true);

	    providerConfigLabelTemplate = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_PROVIDER_CONFIG_LABEL_TEMPLATE, null, false, true);

	    List<String> queryables = options.parameterAsStringList(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_QUERYABLES, null, true, true);
	    if (!queryables.isEmpty()) {
		queryablesFromConfig.addAll(queryables);
	    }

	    /*
	     * WARNING: Must only be set after parameter primaryKeyColumn has been set
	     * (since it is used as default)
	     */
	    associativeTableColumnSuffix = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_ASSOC_TABLE_COLUMN_SUFFIX, primaryKeyColumn, false, true);

	    serviceConfigTemplatePathString = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_SERVICE_CONFIG_TEMPLATE_PATH, null, false, true);

	    serviceDescription = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_SERVICE_DESCRIPTION, "FIXME", false, true);

	    serviceLabel = options.parameterAsString(this.getClass().getName(), Ldproxy2Constants.PARAM_SERVICE_LABEL,
		    "FIXME", false, true);

	    srid = options.parameterAsInteger(this.getClass().getName(), Ldproxy2Constants.PARAM_SRID, 4326);
	    sridConfigured = options.hasParameter(this.getClass().getName(), Ldproxy2Constants.PARAM_SRID);

	    embeddingForFeatureRefs = options.parameterAsBoolean(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_EMBEDDING_FOR_FEATURE_REFS, false);

	    enableFragments = options.parameterAsBoolean(this.getClass().getName(), Ldproxy2Constants.PARAM_FRAGMENTS,
		    false);

	    linearizeCurves = options.parameterAsBoolean(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_LINEARIZE_CURVES, false);

	    enableCodelists = options.parameterAsBoolean(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_ENABLE_CODELISTS, false);

	    enableCrs = options.parameterAsBoolean(this.getClass().getName(), Ldproxy2Constants.PARAM_ENABLE_CRS,
		    false);

	    /*
	     * Important: additionalCrs must be created after parameters 'srid' and
	     * 'forceAxisOrder' have been parsed
	     */
	    if (enableCrs) {
		additionalCrs.add(EpsgCrs.of(srid, forceAxisOrder));

		List<String> additionalCrsFromParam = options.parameterAsStringList(this.getClass().getName(),
			Ldproxy2Constants.PARAM_ADDITIONAL_CRS, null, true, true);

		// NOTE: parameter has been checked by configuration validator
		for (String ac : additionalCrsFromParam) {
		    String codeString;
		    Force force = Force.NONE;
		    if (ac.contains("|")) {
			String[] parts = ac.split("\\|");
			codeString = parts[0].trim();
			if (parts.length > 1 && StringUtils.isNotBlank(parts[1])) {
			    force = Force.valueOf(parts[1].trim());
			}
		    } else {
			codeString = ac;
		    }
		    additionalCrs.add(EpsgCrs.of(Integer.valueOf(codeString), force));
		}
	    }

	    // GML relevant parameters

	    if (options.hasParameter(this.getClass().getName(), Ldproxy2Constants.PARAM_FEATURES_GML)) {
		enableFeaturesGml = options.parameterAsBoolean(this.getClass().getName(),
			Ldproxy2Constants.PARAM_FEATURES_GML, false);
	    } else if (options.hasParameter(this.getClass().getName(), Ldproxy2Constants.PARAM_GML_OUTPUT)) {
		enableFeaturesGml = options.parameterAsBoolean(this.getClass().getName(),
			Ldproxy2Constants.PARAM_GML_OUTPUT, false);
	    }

	    enableFilter = options.parameterAsBoolean(this.getClass().getName(), Ldproxy2Constants.PARAM_ENABLE_FILTER,
		    false);

	    uomTvName = options.parameterAsString(this.getClass().getName(), Ldproxy2Constants.PARAM_UOM_TV_NAME, null,
		    false, true);
	    measureValueLabel = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_MEASURE_VALUE_LABEL, "value", false, true);
	    measureUomLabel = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_MEASURE_UOM_LABEL, "unit of measure", false, true);

	    if (enableFeaturesGml) {

		String gmlIdPrefix = options.parameterAsString(this.getClass().getName(),
			Ldproxy2Constants.PARAM_GML_ID_PREFIX, null, false, true);
		boolean gmlIdOnGeometries = options.parameterAsBoolean(this.getClass().getName(),
			Ldproxy2Constants.PARAM_GML_ID_ON_GEOMETRIES, false);
		int gmlSfLevel = options.parameterAsInteger(this.getClass().getName(),
			Ldproxy2Constants.PARAM_GML_SF_LEVEL, -1);

		String gmlFeatureCollectionElementName = options.parameterAsString(this.getClass().getName(),
			Ldproxy2Constants.PARAM_GML_FEATURE_COLLECTION_ELEMENT_NAME, null, false, true);

		String gmlFeatureMemberElementName = options.parameterAsString(this.getClass().getName(),
			Ldproxy2Constants.PARAM_GML_FEATURE_MEMBER_ELEMENT_NAME, null, false, true);

		boolean gmlSupportsStandardResponseParameters = options.parameterAsBoolean(this.getClass().getName(),
			Ldproxy2Constants.PARAM_GML_SUPPORTS_STANDARD_RESPONSE_PARAMETERS, false);

		XmlEncodingInfos xmlEncodingInfos = new XmlEncodingInfos();

		if (!options.getCurrentProcessConfig().hasAdvancedProcessConfigurations()) {
		    result.addInfo(this, 126);
		} else {
		    Element advancedProcessConfigElmt = options.getCurrentProcessConfig()
			    .getAdvancedProcessConfigurations();

		    List<Element> xeiElmts = XMLUtil.getChildElements(advancedProcessConfigElmt, "XmlEncodingInfos");

		    if (xeiElmts.isEmpty()) {
			result.addInfo(this, 131);
		    } else {
			for (Element xeiElmt : xeiElmts) {
			    xmlEncodingInfos.merge(XmlEncodingInfos.fromXml(xeiElmt));
			}
		    }
		}

		bbGmlBuilder = new LdpBuildingBlockFeaturesGmlBuilder(result, this, mainAppSchema, model, gmlIdPrefix,
			gmlIdOnGeometries, gmlSfLevel, gmlFeatureCollectionElementName, gmlFeatureMemberElementName,
			gmlSupportsStandardResponseParameters, xmlEncodingInfos);
	    }

	    if (!options.getCurrentProcessConfig().hasAdvancedProcessConfigurations()) {
		result.addInfo(this, 126);
	    } else {
		Element advancedProcessConfigElmt = options.getCurrentProcessConfig()
			.getAdvancedProcessConfigurations();

		List<Element> seiElmts = XMLUtil.getChildElements(advancedProcessConfigElmt, "SqlEncodingInfos");

		if (seiElmts.isEmpty()) {
		    result.addInfo(this, 132);
		} else {
		    for (Element seiElmt : seiElmts) {
			sqlEncodingInfos.merge(SqlEncodingInfos.fromXml(seiElmt));
		    }
		    if (!dropSqlEncodingInfosForTypes.isEmpty()) {
			sqlEncodingInfos.dropInfosForTypes(dropSqlEncodingInfosForTypes);
		    }
		}

		List<Element> ldpsqdElmts = XMLUtil.getChildElements(advancedProcessConfigElmt,
			"LdproxyStoredQueryDefinitions");
		if (ldpsqdElmts.isEmpty()) {
		    result.addInfo(this, 139);
		} else {
		    for (Element ldpsqdElmt : ldpsqdElmts) {
			storedQueryDefinitions
				.addLdproxyStoredQueries(LdproxyStoredQueryDefinitions.fromXml(ldpsqdElmt));
		    }
		}
	    }

	    enableFeaturesGeoJson = options.parameterAsBoolean(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_FEATURES_GEOJSON, true);

	    if (enableFeaturesGeoJson) {
		bbGeoJsonBuilder = new LdpBuildingBlockFeaturesGeoJsonBuilder();
	    }

	    enableFeaturesJsonFg = options.parameterAsBoolean(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_FEATURES_JSONFG, false);

	    if (enableFeaturesJsonFg) {
		bbJsonFgBuilder = new LdpBuildingBlockFeaturesJsonFgBuilder();

//		if (options.hasParameter(this.getClass().getName(), Ldproxy2Constants.PARAM_JSONFG_COORD_REF_SYS)) {
//		    jsonFgCoordRefSys = options.parameterAsBoolean(this.getClass().getName(),
//			    Ldproxy2Constants.PARAM_JSONFG_COORD_REF_SYS, true);
//		}
		jsonFgFeatureType = options.parameterAsString(this.getClass().getName(),
			Ldproxy2Constants.PARAM_JSONFG_FEATURE_TYPE, null, false, true);
//		List<String> jsonFgIncludeInGeoJsonIn = options.parameterAsStringList(this.getClass().getName(),
//			Ldproxy2Constants.PARAM_JSONFG_INCLUDE_IN_GEOJSON, null, true, true);
//		for (String s : jsonFgIncludeInGeoJsonIn) {
//		    try {
//			OPTION jsonFgOption = OPTION.valueOf(s);
//			jsonFgIncludeInGeoJson.add(jsonFgOption);
//		    } catch (IllegalArgumentException e) {
//			// ignore - should be reported by configuration validator
//		    }
//		}
	    }

	    enableFeaturesResultType = options.parameterAsBoolean(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_FEATURES_RESULT_TYPE_ENABLE, false);

	    enableProjections = options.parameterAsBoolean(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_PROJECTIONS_ENABLE, false);

	    enableSorting = options.parameterAsBoolean(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_SORTING_ENABLE, false);

	    if (enableSorting) {

		sortingIncluded = options.parameterAsStringList(this.getClass().getName(),
			Ldproxy2Constants.PARAM_SORTING_INCLUDED, null, true, true);

		sortingExcluded = options.parameterAsStringList(this.getClass().getName(),
			Ldproxy2Constants.PARAM_SORTING_EXCLUDED, null, true, true);

		String sortingPathSeparatorIn = options.parameterAsString(this.getClass().getName(),
			Ldproxy2Constants.PARAM_SORTING_PATH_SEPARATOR, null, false, true);
		if (StringUtils.isNotBlank(sortingPathSeparatorIn)) {
		    try {
			sortingPathSeparator = PathSeparator.valueOf(sortingPathSeparatorIn);
		    } catch (IllegalArgumentException e) {
			// ignore - should be reported by configuration validator
		    }
		}
	    }

	    if (mainAppSchema.matches(Ldproxy2Constants.RULE_ALL_CORETABLE)) {

		coretable = options.parameterAsString(this.getClass().getName(), Ldproxy2Constants.PARAM_CORETABLE,
			"features", false, true);

		coretablePkColumn = options.parameterAsString(this.getClass().getName(),
			Ldproxy2Constants.PARAM_CORETABLE_PK_COLUMN, "pk", false, true);

		// WARNING: MUST BE COMPUTED AFTER primaryKeyColumn HAS BEEN COMPUTED!
		coretableIdColumn = options.parameterAsString(this.getClass().getName(),
			Ldproxy2Constants.PARAM_CORETABLE_ID_COLUMN, primaryKeyColumn, false, true);

		String ldpTypeTmp = options.parameterAsString(this.getClass().getName(),
			Ldproxy2Constants.PARAM_CORETABLE_ID_COLUMN_LDPROXY_TYPE, "Integer", false, true);
		coretableIdColumnLdproxyType = Type.valueOf(ldpTypeTmp.toUpperCase(Locale.ENGLISH));

		coretableFeatureTypeColumn = options.parameterAsString(this.getClass().getName(),
			Ldproxy2Constants.PARAM_CORETABLE_FEATURE_TYPE_COLUMN, "featuretype", false, true);

		coretableGeometryColumn = options.parameterAsString(this.getClass().getName(),
			Ldproxy2Constants.PARAM_CORETABLE_GEOMETRY_COLUMN, "geom", false, true);

//		coretableJsonFeatureRefWithAnyCollectionId = options.parameterAsBoolean(this.getClass().getName(),
//			Ldproxy2Constants.PARAM_CORETABLE_JSON_FEATURE_REF_WITH_ANY_COLLECTION_ID, false);

		/*
		 * WARNING: Value of target parameter coretableIdColumn MUST be determined
		 * BEFORE the value of target parameter coretableSourceColumn is determined.
		 */
		coretableSourceColumn = options.parameterAsString(this.getClass().getName(),
			Ldproxy2Constants.PARAM_CORETABLE_SOURCE_COLUMN, coretableIdColumn, false, true);

		coretableRefColumn = options.parameterAsString(this.getClass().getName(),
			Ldproxy2Constants.PARAM_CORETABLE_REF_COLUMN, "ref", false, true);

		coretableRelationsTable = options.parameterAsString(this.getClass().getName(),
			Ldproxy2Constants.PARAM_CORETABLE_RELATIONS_TABLE, "references", false, true);

		coretableRelationNameColumn = options.parameterAsString(this.getClass().getName(),
			Ldproxy2Constants.PARAM_CORETABLE_RELATION_NAME_COLUMN, "rel", false, true);

		coretableInverseRelationNameColumn = options.parameterAsString(this.getClass().getName(),
			Ldproxy2Constants.PARAM_CORETABLE_INVERSE_RELATION_NAME_COLUMN, "rel_inv", false, true);

		coretableVersion = options.parameterAsString(this.getClass().getName(),
			Ldproxy2Constants.PARAM_CORETABLE_VERSION, null, false, true);

		coretableVersionColumn = options.parameterAsString(this.getClass().getName(),
			Ldproxy2Constants.PARAM_CORETABLE_VERSION_COLUMN, "version", false, true);
	    }

	    enableTiles = options.parameterAsBoolean(this.getClass().getName(), Ldproxy2Constants.PARAM_ENABLE_TILES,
		    false);

	    tilesetDefaultSparse = options.parameterAsBoolean(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_TILESET_DEFAULT_SPARSE, false);

	    tilesetDefaultCenterLon = options.parameterAsDouble(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_TILESET_DEFAULT_CENTER_LON, 0.0);

	    tilesetDefaultCenterLat = options.parameterAsDouble(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_TILESET_DEFAULT_CENTER_LAT, 0.0);

	    tilesetDefaultMinLevelWebMercatorQuad = options.parameterAsInteger(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_TILESET_DEFAULT_MIN_LEVEL_WMQ, 0);

	    tilesetDefaultMaxLevelWebMercatorQuad = options.parameterAsInteger(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_TILESET_DEFAULT_MAX_LEVEL_WMQ, 24);

	    // IMPORTANT: must be parsed after tilesetDefaultMinLevelWebMercatorQuad
	    tilesetDefaultDefaultLevelWebMercatorQuad = options.parameterAsInteger(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_TILESET_DEFAULT_DEFAULT_LEVEL_WMQ, tilesetDefaultMinLevelWebMercatorQuad);

	    dynamicTilesCacheSeededMaxLevelWebMercatorQuad = options.parameterAsInteger(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_DYNAMIC_TILES_CACHE_SEEDED_MAX_LEVEL_WMQ,
		    Math.max(10, tilesetDefaultMinLevelWebMercatorQuad));

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

//	    if(!dataDirectoryFile.exists()) {
//		dataDirectoryFile.mkdirs();
//	    }

	    dataDirectoryFile = new File(outputDirectory, "data");
	    Path dataDirectoryPath = dataDirectoryFile.toPath();
	    cfg = LdproxyCfgWriter.create(dataDirectoryPath);
	}

	/*
	 * Required to be performed for each application schema
	 */
	result.addDebug(this, 10001, pi.name());
    }

    @Override
    public void process(ClassInfo ci) {

	if (ci == null || ci.pkg() == null) {
	    return;
	}

	if (!LdpInfo.isEncoded(ci)) {
	    result.addInfo(this, 8, ci.name());
	    return;
	}

	result.addDebug(this, 4, ci.name());

	Optional<ProcessMapEntry> pme = mapEntry(ci);

	if (pme.isPresent() && !ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme.get(), ci.id())) {
	    result.addInfo(this, 22, ci.name(), pme.get().getTargetType());
	    return;
	}

	if (schemaNotEncoded) {
	    result.addInfo(this, 18, schema.name(), ci.name());
	    return;
	}

	if (!enableFragments && ci.isAbstract()) {
	    MessageContext mc = result.addWarning(this, 20, ci.name());
	    if (mc != null) {
		mc.addDetail(this, 0, ci.fullNameInSchema());
	    }
	    return;
	}

	if (!enableFragments && !ci.supertypes().isEmpty()) {
	    MessageContext mc = result.addError(this, 103, ci.name());
	    if (mc != null) {
		mc.addDetail(this, 0, ci.fullNameInSchema());
	    }
	}

	/*
	 * This target could support basic types, but does not do so yet. The JSON
	 * Schema target is an example of how basic types can be identified in the model
	 * (through determination if ci inherits (directly or indirectly) from a type
	 * that is mapped to a simple ldproxy type; if so, ci is a basic type.
	 */

	if (ci.category() == Options.DATATYPE && ci.matches(Ldproxy2Constants.RULE_CLS_GENERIC_VALUE_TYPE)
		&& GenericValueTypeUtil.isSubtypeOfGenericValueType(ci, genericValueTypes)) {

	    result.addInfo(this, 24, ci.name());

	} else if (!enableFragments && Ldproxy2Target.isDatatypeOrUnionEncodedLikeDatatype(ci)) {

	    // ignore here - will be encoded as needed

	    /*
	     * 2022-08-25 JE: Handling of unions just like data types deactivated. For the
	     * time being, we keep the approach with type flattening. 2023-06-23 JE: In
	     * other words, unions are not supported, but data types are. 2024-11-12 JE:
	     * Encoding unions like data types activated again
	     */

	} else if (ci.category() == Options.OBJECT || ci.category() == Options.FEATURE || (enableFragments
		&& (ci.category() == Options.MIXIN || Ldproxy2Target.isDatatypeOrUnionEncodedLikeDatatype(ci)))) {

	    if (objectFeatureMixinAndDataTypes.stream().anyMatch(t -> t.name().equalsIgnoreCase(ci.name()))) {
		MessageContext mc = result.addError(this, 125, ci.name());
		if (mc != null) {
		    mc.addDetail(this, 0, ci.fullNameInSchema());
		}
	    } else {
		objectFeatureMixinAndDataTypes.add(ci);
	    }

	} else if (ci.category() == Options.ENUMERATION || ci.category() == Options.CODELIST) {

	    if (codelistsAndEnumerations.stream().anyMatch(t -> t.name().equalsIgnoreCase(ci.name()))) {
		MessageContext mc = result.addError(this, 125, ci.name());
		if (mc != null) {
		    mc.addDetail(this, 0, ci.fullNameInSchema());
		}
	    } else {
		codelistsAndEnumerations.add(ci);
	    }

	} else {

	    /*
	     * NOTE: conversion of basic types not supported. Conversion of mixins only
	     * supported if fragments are enabled.
	     */

	    result.addInfo(this, 17, ci.name());
	}
    }

    /**
     * @param pme    map entry that would apply for the type with given ID
     * @param typeId ID of the type to check; can be <code>null</code>
     * @return <code>true</code>, if the map entry shall be ignored for the type
     *         with given id because the map entry has parameter
     *         {@value Ldproxy2Constants#ME_PARAM_IGNORE_FOR_TYPE_FROM_SEL_SCHEMA}
     *         and the type is encoded and owned by one of the schemas selected for
     *         processing; else <code>false</code>
     */
    public boolean ignoreMapEntryForTypeFromSchemaSelectedForProcessing(ProcessMapEntry pme, String typeId) {

	if (StringUtils.isBlank(typeId)) {

	    return false;

	} else {

	    ClassInfo type = model.classById(typeId);

	    if (type == null || !LdpInfo.isEncoded(type) || !model.isInSelectedSchemas(type)) {
		return false;
	    } else {
		if (mapEntryParamInfos.hasParameter(pme, Ldproxy2Constants.ME_PARAM_IGNORE_FOR_TYPE_FROM_SEL_SCHEMA)) {
		    return true;
		} else
		    return false;
	    }
	}
    }

    /**
     * Look up the map entry defined for a class. It is not guaranteed that such a
     * map entry exists.
     * 
     * @param ci the class for which to look up a map entry
     * @return an {@link Optional} with the map entry defined for the given class,
     *         under the ldp2 encoding rule that applies to the class
     */
    public Optional<ProcessMapEntry> mapEntry(ClassInfo ci) {

	return Optional.ofNullable(options.targetMapEntry(ci.name(), ci.encodingRule(Ldproxy2Constants.PLATFORM)));
    }

    public boolean valueTypeIsMapped(PropertyInfo pi) {

	return valueTypeIsMapped(pi.typeInfo().name, pi.typeInfo().id, pi.encodingRule(Ldproxy2Constants.PLATFORM));
    }

    public boolean valueTypeIsMapped(String typeName, String typeId, String encodingRule) {

	ProcessMapEntry pme = Ldproxy2Target.mapEntryParamInfos.getMapEntry(typeName, encodingRule);

	if (pme != null && !ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme, typeId)) {
	    return true;
	} else {
	    return false;
	}
    }

    /**
     * @param pi - tbd
     * @return <code>true</code>, if the value type of pi is not mapped, and either
     *         a) is not encoded, or b) is of an unsupported category; else
     *         <code>false</code>
     */
    public boolean isIgnored(PropertyInfo pi) {

	if (!valueTypeIsMapped(pi)) {

	    if (pi.typeClass() == null || !LdpInfo.isEncoded(pi.typeClass())) {
		MessageContext mc = result.addError(this, 124, pi.typeInfo().name, pi.name(), pi.inClass().name());
		if (mc != null) {
		    mc.addDetail(this, 1, pi.fullNameInSchema());
		}
		return true;
	    } else if (LdpInfo.unsupportedCategoryOfValue(pi)) {
		MessageContext mc = result.addError(this, 120, pi.typeInfo().name, pi.name(), pi.inClass().name());
		if (mc != null) {
		    mc.addDetail(this, 1, pi.fullNameInSchema());
		}
		return true;
	    }
	}

	return false;
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

	    // copy template file
	    if (StringUtils.isNotBlank(cfgTemplatePath)) {

		File cfgFile = new File(dataDirectoryFile, "cfg.yml");

		try {
		    if (cfgTemplatePath.startsWith("http")) {
			FileUtils.copyURLToFile(URI.create(cfgTemplatePath).toURL(), cfgFile);
		    } else {
			FileUtils.copyFile(new File(cfgTemplatePath), cfgFile);
		    }
		} catch (IOException e) {
		    result.addError(this, 123, Ldproxy2Constants.PARAM_CFG_TEMPLATE_PATH, cfgTemplatePath,
			    cfgFile.getAbsolutePath());
		}
	    }

	    File serviceConfigTemplateFile = new File(outputDirectory, "tmp_serviceConfigTemplate.yml");
	    if (StringUtils.isNotBlank(serviceConfigTemplatePathString)) {
		try {
		    if (serviceConfigTemplatePathString.startsWith("http")) {
			FileUtils.copyURLToFile(URI.create(serviceConfigTemplatePathString).toURL(),
				serviceConfigTemplateFile);
		    } else {
			FileUtils.copyFile(new File(serviceConfigTemplatePathString), serviceConfigTemplateFile);
		    }
		} catch (IOException e) {
		    result.addError(this, 123, Ldproxy2Constants.PARAM_SERVICE_CONFIG_TEMPLATE_PATH,
			    serviceConfigTemplatePathString, serviceConfigTemplateFile.getAbsolutePath());
		}
	    }

	    LdpProvider provider;
	    LdpSourcePathProvider sourcePathProvider;
	    if (mainAppSchema.matches(Ldproxy2Constants.RULE_ALL_CORETABLE)) {
		provider = new LdpCoretableProvider();
		sourcePathProvider = new LdpCoretableSourcePathProvider(this);
	    } else {
		provider = new LdpSqlProvider();
		sourcePathProvider = new LdpSqlSourcePathProvider(this);
	    }

	    LdpConfigBuilder configBuilder = new LdpConfigBuilder(this, cfg, bbGmlBuilder, bbGeoJsonBuilder,
		    bbJsonFgBuilder, objectFeatureMixinAndDataTypes, codelistsAndEnumerations, provider,
		    sourcePathProvider);

	    configBuilder.process();

	    ImmutableFeatureProviderSqlData featureProviderConfig = configBuilder.getFeatureProviderConfig();
	    ImmutableOgcApiDataV2 serviceConfig = configBuilder.getServiceConfig();
	    ImmutableTileProviderFeaturesData tileProviderConfig = configBuilder.getTileProviderConfig();

	    // write api
	    try {
		if (serviceConfigTemplateFile.exists()) {
		    cfg.writeEntity(serviceConfig, serviceConfigTemplateFile.toPath());
		} else {
		    cfg.writeEntity(serviceConfig);
		}
		cfg.writeEntity(featureProviderConfig);

		SortedMap<String, ImmutableCodelist> codelistById = configBuilder.getCodeListMap();
		for (String codelistId : codelistById.keySet()) {
		    ImmutableCodelist ic = codelistById.get(codelistId);
		    cfg.writeValue(ic, codelistId);
		}

		SortedMap<String, ImmutableStoredQueryExpression> storedQueriesByStoredQueryId = configBuilder
			.getStoredQueryMap();
		for (String sqId : storedQueriesByStoredQueryId.keySet()) {

		    ImmutableStoredQueryExpression sq = storedQueriesByStoredQueryId.get(sqId);

		    /*
		     * 2025-07-09: Writing the stored query via the ldproxy-cfg-lib resulted in
		     * exceptions (NPE due to missing path, it looked like). Therefore, until that
		     * is fixed in the lib, we write the stored query via jackson.
		     */
//		    cfg.writeValue(sq, sqId);

		    ObjectMapper outMapper = new ObjectMapper();
		    outMapper.registerModule(new Jdk8Module().configureReadAbsentAsNull(true));
		    outMapper.setSerializationInclusion(Include.NON_EMPTY);

		    File sqDir = new File(outputDirectory, "data/values/queries/" + Ldproxy2Target.mainId);
		    Files.createDirectories(sqDir.toPath());

		    File sqFile = new File(sqDir, sqId + ".json");
		    outMapper.writerWithDefaultPrettyPrinter().writeValue(sqFile, sq);
		}

		if (tileProviderConfig != null) {
		    cfg.writeEntity(tileProviderConfig);
		}

	    } catch (IOException e) {
		e.printStackTrace();
	    } finally {
		if (serviceConfigTemplateFile.exists()) {
		    FileUtils.deleteQuietly(serviceConfigTemplateFile);
		}
	    }
	}

    }

    public Type ldproxyType(PropertyInfo pi) {

	return ldproxyType(pi.typeInfo().name, pi.typeInfo().id, pi.encodingRule(Ldproxy2Constants.PLATFORM));
    }

    public Type ldproxyType(String typeName, String typeId, String encodingRule) {

	Type resType = Type.STRING;

	ProcessMapEntry pme = Ldproxy2Target.mapEntryParamInfos.getMapEntry(typeName, encodingRule);

	if (pme != null && !ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme, typeId)) {

	    if (pme.hasTargetType()) {

		if ("GEOMETRY".equalsIgnoreCase(pme.getTargetType())) {
		    resType = Type.GEOMETRY;
		} else if ("LINK".equalsIgnoreCase(pme.getTargetType())) {
		    resType = Type.OBJECT;
		} else {
		    resType = Type.valueOf(pme.getTargetType().trim().toUpperCase(Locale.ENGLISH));
		}

	    } else {
		// is checked via target configuration validator (which can be switched off)
		result.addError(this, 112, typeName);
	    }

	} else {

	    // is the type contained in the schemas selected for processing?
	    ClassInfo valueType = null;
	    if (StringUtils.isNotBlank(typeId)) {
		valueType = Ldproxy2Target.model.classById(typeId);
	    }
	    if (valueType == null && StringUtils.isNotBlank(typeName)) {
		valueType = Ldproxy2Target.model.classByName(typeName);
	    }

	    if (valueType == null) {

		// The value type was not found in the model
		result.addError(this, 113, typeName);

	    } else if (!LdpInfo.isEncoded(valueType)) {

		result.addError(this, 114, typeName);

	    } else if (valueType.category() == Options.ENUMERATION || valueType.category() == Options.CODELIST) {

		if (!valueType.matches(Ldproxy2Constants.RULE_CLS_CODELIST_TARGETBYTV)
			&& StringUtils.isNotBlank(valueType.taggedValue("numericType"))) {
		    resType = ldproxyType(valueType.taggedValue("numericType"), null,
			    valueType.encodingRule(Ldproxy2Constants.PLATFORM));
		} else {
		    resType = Type.STRING;
		}
	    } else {
		resType = Type.OBJECT;
	    }
	}

	return resType;
    }

    public LdpSpecialPropertiesInfo specialPropertiesInfo(ClassInfo ci) {

	if (this.specialPropertiesInfoByCi.containsKey(ci)) {
	    return this.specialPropertiesInfoByCi.get(ci);
	} else {
	    LdpSpecialPropertiesInfo specPropInfo = new LdpSpecialPropertiesInfo(ci, this);
	    this.specialPropertiesInfoByCi.put(ci, specPropInfo);
	    return specPropInfo;
	}
    }

    public boolean isProcessedType(ClassInfo ci) {
	return codelistsAndEnumerations.contains(ci) || objectFeatureMixinAndDataTypes.contains(ci);
    }

    public boolean isMappedToOrImplementedAsLink(PropertyInfo pi) {

	if (valueTypeIsMapped(pi)) {
	    return isMappedToLink(pi);
	} else {
	    return LdpInfo.isTypeWithIdentityValueType(pi);
	}
    }

    public boolean isImplementedAsMeasure(PropertyInfo pi) {
	return isMeasureMapping(pi) && pi.matches(Ldproxy2Constants.RULE_PROP_MEASURE)
		&& (uomTvName == null || StringUtils.isBlank(pi.taggedValue(uomTvName)));
    }

    public boolean isMappedToLink(ClassInfo ci) {

	return isMappedToLink(ci.name(), ci.id(), ci.encodingRule(Ldproxy2Constants.PLATFORM));
    }

    public boolean isMappedToLink(PropertyInfo pi) {

	return isMappedToLink(pi.typeInfo().name, pi.typeInfo().id, pi.encodingRule(Ldproxy2Constants.PLATFORM));
    }

    public boolean isMappedToLink(String typeName, String typeId, String encodingRule) {

	ProcessMapEntry pme = Ldproxy2Target.mapEntryParamInfos.getMapEntry(typeName, encodingRule);

	if (pme != null && !ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme, typeId) && pme.hasTargetType()
		&& "LINK".equalsIgnoreCase(pme.getTargetType())) {
	    return true;
	} else {
	    return false;
	}
    }

    public boolean isMeasureMapping(PropertyInfo pi) {

	return isMeasureMapping(pi.typeInfo().name, pi.typeInfo().id, pi.encodingRule(Ldproxy2Constants.PLATFORM));
    }

    public boolean isMeasureMapping(String typeName, String typeId, String encodingRule) {

	ProcessMapEntry pme = Ldproxy2Target.mapEntryParamInfos.getMapEntry(typeName, encodingRule);

	if (pme != null && !ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme, typeId) && pme.hasTargetType()
		&& mapEntryParamInfos.hasParameter(pme, Ldproxy2Constants.ME_PARAM_MEASURE)) {
	    return true;
	} else {
	    return false;
	}
    }

    public boolean isGenericValueType(ClassInfo ci) {
	return ci.category() == Options.DATATYPE && ci.matches(Ldproxy2Constants.RULE_CLS_GENERIC_VALUE_TYPE)
		&& genericValueTypes != null && genericValueTypes.contains(ci.name());
    }

    public static boolean isDatatypeOrUnionEncodedLikeDatatype(ClassInfo ci) {
	return ci.category() == Options.DATATYPE
		|| (ci.category() == Options.UNION && ci.matches(Ldproxy2Constants.RULE_CLS_UNION_LIKE_DATATYPE));
    }

    public static boolean categoryOfValueIsDatatypeOrSupportedUnion(PropertyInfo pi) {
	return pi.categoryOfValue() == Options.DATATYPE || (pi.categoryOfValue() == Options.UNION
		&& pi.typeClass() != null && pi.typeClass().matches(Ldproxy2Constants.RULE_CLS_UNION_LIKE_DATATYPE));
    }

    @Override
    public void reset() {

	model = null;

	initialised = false;
	diagnosticsOnly = false;
	numberOfEncodedSchemas = 0;

	isUnitTest = false;

	dbSchemaNames = new TreeSet<String>();

	collectionIdFormat = "lowerCase";

//	documentationTemplate = null;
//	documentationNoValue = null;

	additionalCrs = new ArrayList<>();
	associativeTableColumnSuffix = null; // default: value of primaryKeyColumn parameter
	cfgTemplatePath = "https://shapechange.net/resources/templates/ldproxy2/cfgTemplate.yml";
	codeTargetTagName = Ldproxy2Constants.DEFAULT_CODE_TARGET_TAG_NAME_VALUE;
	dateFormat = null; // no default value
	dateTimeFormat = null; // no default value
	descriptionTemplate = "[[definition]]";
	descriptorNoValue = "";
	dropSqlEncodingInfosForTypes = new ArrayList<>();
	embeddingForFeatureRefs = false;
	enableFragments = false;
	forceAxisOrder = Force.NONE;
	foreignKeyColumnSuffix = "";
	foreignKeyColumnSuffixDatatype = "";
	foreignKeyColumnSuffixCodelist = "";
	genericValueTypes = null;
//	jsonFgCoordRefSys = null;
	jsonFgFeatureType = null;
//	jsonFgIncludeInGeoJson = new ArrayList<>();
	labelTemplate = "[[alias]]";
	linearizeCurves = false;
	maxNameLength = 63;
	nativeTimeZone = null;
	objectIdentifierName = "oid";
	primaryKeyColumn = "id";
	providerConfigLabelTemplate = null;
	queryablesFromConfig = new TreeSet<>();
	reflexiveRelationshipFieldSuffix = "";
	serviceDescription = "FIXME";
	serviceLabel = "FIXME";
	srid = 4326;
	sridConfigured = false;
	serviceConfigTemplatePathString = null;
	taggedValueForPropertyId = null;
	propertyIdByTaggedValue = false;

	outputDirectory = null;
	dataDirectoryFile = null;
	mainId = null;
	mainAppSchema = null;

	enableCodelists = false;
	enableCrs = false;
	enableFeaturesGml = false;
	enableFeaturesGeoJson = true;
	enableFeaturesJsonFg = false;
	enableFeaturesResultType = false;
	enableFilter = false;
	enableProjections = false;

	enableSorting = false;
	sortingIncluded = new ArrayList<>();
	sortingExcluded = new ArrayList<>();
	sortingPathSeparator = null;

	uomTvName = null;
	measureUomLabel = null;
	measureValueLabel = null;
	bbGmlBuilder = null;
	bbGeoJsonBuilder = null;
	bbJsonFgBuilder = null;
	sqlEncodingInfos = new SqlEncodingInfos();
	storedQueryDefinitions = new LdproxyStoredQueryDefinitions();

	mapEntryParamInfos = null;

	objectFeatureMixinAndDataTypes = new ArrayList<>();
	codelistsAndEnumerations = new TreeSet<>();

	cfg = null;

	coretable = "features";
	coretablePkColumn = "pk";
	coretableIdColumn = null;
	coretableIdColumnLdproxyType = Type.INTEGER;
	coretableFeatureTypeColumn = "featuretype";
	coretableGeometryColumn = "geom";
//	coretableJsonFeatureRefWithAnyCollectionId = false;
	coretableSourceColumn = null;
	coretableRefColumn = "ref";
	coretableRelationsTable = "references";
	coretableRelationNameColumn = "rel";
	coretableInverseRelationNameColumn = "rel_inv";
	coretableVersion = null;
	coretableVersionColumn = "version";

	enableTiles = false;
	tilesetDefaultSparse = false;
	tilesetDefaultCenterLon = 0.0;
	tilesetDefaultCenterLat = 0.0;
	tilesetDefaultMinLevelWebMercatorQuad = 0;
	tilesetDefaultMaxLevelWebMercatorQuad = 24;
	tilesetDefaultDefaultLevelWebMercatorQuad = 0;
	dynamicTilesCacheSeededMaxLevelWebMercatorQuad = 10;
    }

    @Override
    public void registerRulesAndRequirements(RuleRegistry r) {

	r.addRule(Ldproxy2Constants.RULE_ALL_ASSOCIATIVETABLES_WITH_SEPARATE_PK_FIELD);
	r.addRule(Ldproxy2Constants.RULE_ALL_CORETABLE);
	r.addRule(Ldproxy2Constants.RULE_ALL_DOCUMENTATION);
	r.addRule(Ldproxy2Constants.RULE_ALL_GEOINFODOK);
	r.addRule(Ldproxy2Constants.RULE_ALL_LINK_OBJECT_AS_FEATURE_REF);
	r.addRule(Ldproxy2Constants.RULE_ALL_NOT_ENCODED);
	r.addRule(Ldproxy2Constants.RULE_ALL_QUERYABLES);
	r.addRule(Ldproxy2Constants.RULE_ALL_SCHEMAS);
	r.addRule(Ldproxy2Constants.RULE_CLS_CODELIST_DIRECT);
	r.addRule(Ldproxy2Constants.RULE_CLS_CODELIST_TARGETBYTV);
	r.addRule(Ldproxy2Constants.RULE_CLS_CODELIST_APPEND_CODE);
	r.addRule(Ldproxy2Constants.RULE_CLS_CODELIST_BY_TABLE);
	r.addRule(Ldproxy2Constants.RULE_CLS_DATATYPES_ONETOMANY_SEVERAL_TABLES);
	r.addRule(Ldproxy2Constants.RULE_CLS_ENUMERATION_ENUM_CONSTRAINT);
	r.addRule(Ldproxy2Constants.RULE_CLS_GENERIC_VALUE_TYPE);
	r.addRule(Ldproxy2Constants.RULE_CLS_IDENTIFIER_STEREOTYPE);
	r.addRule(Ldproxy2Constants.RULE_CLS_UNION_LIKE_DATATYPE);
	r.addRule(Ldproxy2Constants.RULE_ALL_PROPIDBYTV);
	r.addRule(Ldproxy2Constants.RULE_PROP_READONLY);
	r.addRule(Ldproxy2Constants.RULE_PROP_MEASURE);
    }

    @Override
    public String getDefaultEncodingRule() {
	return "*";
    }

    @Override
    public String getTargetName() {
	return "Ldproxy (v2)";
    }

    @Override
    public String getTargetIdentifier() {
	return "ldp2";
    }

    @Override
    public String message(int mnr) {

	return switch (mnr) {

	case 0 -> "Context: class '$1$'";
	case 1 -> "Context: property '$1$'";
	case 2 -> "Context: schema '$1$'";

	case 3 -> "Context: class Ldproxy2Target";
	case 4 -> "Processing class '$1$'.";
	case 5 -> "Directory named '$1$' does not exist or is not accessible.";
	case 6 -> "System error: Exception encountered. Message is: '$1$'";
	case 7 -> "Schema '$1$' is not encoded.";
	case 8 -> "Class '$1$' is not encoded.";

	case 15 -> "No map entries provided via the configuration.";
	case 16 ->
	    "Value '$1$' of configuration parameter $2$ does not match the regular expression: $3$. The parameter will be ignored.";
	case 17 ->
	    "Type '$1$' is of a category not enabled for conversion, meaning that no ldproxy configuration items will be created to represent it.";
	case 18 ->
	    "Schema '$1$' is not encoded. Thus class '$2$' (which belongs to that schema) is not encoded either.";
	case 19 -> "";
	case 20 ->
	    "Type '$1$' is abstract. Conversion of abstract types is only supported by this target if parameter '"
		    + Ldproxy2Constants.PARAM_FRAGMENTS + "' is set to 'true'. The type will be ignored.";
	case 21 ->
	    "Value '$1$' of configuration parameter '$2$' does not result in a valid path. Exception message is: $3$";
	case 22 -> "Type '$1$' has been mapped to '$2$', as defined by the configuration.";
	case 23 -> "";
	case 24 -> "Type '$1$' is subtype of a generic value type, and will be encoded as such.";

	case 100 -> Ldproxy2Constants.RULE_CLS_CODELIST_DIRECT
		+ " applies to codelist/enumeration '$1$'. However, code/enum '$2$' has no initial value. The code/enum cannot be encoded as defined by the conversion rule. Using the property name as fallback for the {code} in the enum/code mapping.";
	case 101 -> Ldproxy2Constants.RULE_CLS_CODELIST_TARGETBYTV
		+ " applies to codelist/enumeration '$1$'. However, code/enum '$2$' does not have a non-blank value for tag '$3$'. The code/enum cannot be encoded as defined by the conversion rule. Using the property name as fallback for the {target_value} in the enum/code mapping";
	case 102 ->
	    "Could not compute a label for codelist/enumeration '$1$'. A label is required for the ldproxy encoding as a codelist. Check the label template in the target configuration as well as the codelist/enumeration model, to ensure that a label can be created. Using the type name as label.";
	case 103 ->
	    "Type '$1$' has one or more supertypes. Conversion of inheritance relationships is only supported by this target if parameter '"
		    + Ldproxy2Constants.PARAM_FRAGMENTS + "' is set to 'true'. The relationship will be ignored.";
	case 104 ->
	    "??Type '$1$' has <<identifier>> property '$2$', with max multiplicity greater 1. Encoding will assume a max multiplicity of exactly 1.";
	case 105 ->
	    "??Type '$1$' has multiple properties (direct and maybe inherited) marked (via tagged value 'defaultGeometry') as default geometry property. Multiple default geometry properties per type are not allowed. None of the default geometry properties owned by '$1$' will be marked as default geometry.";
	case 106 ->
	    "??Property '$2$' of type '$1$' is marked as default instant as well as as default interval (start and/or end) via according tagged values. That is an invalid combination. The property is not recognized as defining a primary temporal property.";
	case 107 ->
	    "??Type '$1$' has multiple identifier properties (i.e., properties with stereotype 'identifier', and taking into account inherited properties). Multiple identifier properties per type definition are not allowed. None of the identifier properties owned by '$1$' will be encoded (because no informed decision can be made).";
	case 108 ->
	    "??Type '$1$' has multiple properties (direct and maybe inherited) marked (via tagged value 'defaultInstant') as default (temporal) instant property. Multiple default instant properties per type are not allowed. None of the default instant properties owned by '$1$' will be marked as default instant.";
	case 109 ->
	    "??Property '$2$' of type '$1$' is marked as default interval start as well as default instant and/or default interval end via according tagged values. That is an invalid combination. The property is not recognized as defining a primary temporal property.";
	case 110 ->
	    "??Type '$1$' has multiple properties (direct and maybe inherited) marked (via tagged value 'defaultIntervalStart') as default (temporal) interval start property. Multiple default interval start properties per type are not allowed. None will be marked as default interval start.";
	case 111 ->
	    "??Type '$1$' has multiple properties (direct and maybe inherited) marked (via tagged value 'defaultIntervalEnd') as default (temporal) interval end property. Multiple default interval end properties per type are not allowed. None will be marked as default interval end.";
	case 112 -> "??No target type is defined in map entry for type '$1$'. Assuming ldproxy type 'STRING'.";
	case 113 ->
	    "??Ldproxy type definition for type '$1$' could not be identified. No map entry is defined for the type, and the type was not found in the model. Assuming ldproxy type 'STRING'.";
	case 114 ->
	    "??Type '$1$' is marked to not be encoded. Could not identify an ldproxy type for it. Assuming ldproxy type 'STRING'.";
	case 115 ->
	    "??Type '$1$' is not part of the schemas selected for processing. Could not identify an ldproxy type for it. Assuming ldproxy type 'STRING'.";
	case 116 ->
	    "??Expected simple ldproxy type implementation of value type '$1$' of property '$2$' in class '$3$'. Found '$4$'. Assuming ldproxy type 'STRING'.";
	case 117 ->
	    "??Circular dependency detected when encoding type '$1$'. The property path '$2$' would create a circle. The last property in that path will not be encoded.";
	case 118 ->
	    "??No target type is defined in map entry for type '$1$'. Setting source path for properties with this type as value type to 'FIXME'.";
	case 119 ->
	    "??Value type '$1$' of property '$2$' is neither mapped nor found in the model. Is there a typo in value type name? Has the type not been loaded (e.g. by excluding some package during model loading) or removed (e.g. through a transformation)? Setting source path for the property to 'FIXME'.";
	case 120 ->
	    "??The value type '$1$' of property '$2$' (of class '$3$') is not mapped and of a category not supported by this target. The property will not be encoded. Either define a mapping for the value type or apply a model transformation (e.g. flattening inheritance or flattening complex types [including unions]) to cope with this situation.";
	case 121 ->
	    "??No geometry type defined via map entry for value type '$2$' of property '$1$'. Ensure that map entries are configured correctly. Proceeding with geometry type ANY.";
	case 122 ->
	    "??Value of tag 'ldpRemove' on property '$1$' is invalid. Allowed values are: IN_COLLECTION, ALWAYS, NEVER (case is ignored when parsing the value). Found value '$2$'.";
	case 123 ->
	    "Exception occurred while copying the template defined by target parameter '$1$' from '$2$' to '$3$'. Exception message is: '$3$'.";
	case 124 ->
	    "??The value type '$1$' of property '$2$' (of class '$3$') is not mapped and either not present in the model, not correctly linked, or not encoded. The property will therefore not be encoded. Either define a mapping for the value type, or ensure that the value type is correctly linked to and that it actually is defined to be encoded by this target.";
	case 125 ->
	    "Type '$1$' will be ignored, because a type with equal name (ignoring case) has already been encountered and marked for encoding by the target. The target does not support encoding of multiple types with equal name (ignoring case).";
	case 126 -> "??The target configuration does not contain advanced process configuration infos.";
	case 127 ->
	    "??Enumeration or code list '$1$', which is used as value type of at least one property that is encoded by the target, is either not encoded, not mapped, or not part of the schemas selected for processing. This is an issue UNLESS an ldproxy codelist with id '$2$' has already been or will be established for the desired deployment by other means (e.g. manually created).";
	case 128 ->
	    "??Main application schema could not be determined (using parameter '" + TargetUtil.PARAM_MAIN_APP_SCHEMA
		    + "' - if set - or by having only a single schema to process). Using '$1$'.";
	case 129 ->
	    "??Could not find a schema with name '$1$' in the model, or no target namespace is defined for that schema. XML namespace information for that schema is not available. Using value '$2$' as namespace and nsabr for that schema name.";
	case 130 ->
	    "??Could not find an abbreviation for XML namespace '$1$' (neither in XmlEncodingInfos, nor in the model). Using value '$2$'.";
	case 131 -> "The target configuration does not contain XML encoding infos.";
	case 132 -> "The target configuration does not contain SQL encoding infos.";
	case 133 ->
	    "??Property '$2$' of type '$1$' is marked as default interval end as well as default instant and/or default interval start via according tagged values. That is an invalid combination. The property is not recognized as defining a primary temporal property.";
	case 134 ->
	    "Multiple Source Paths detected, but not encoded! Please inform the ShapeChange developers about this error.";
	case 135 ->
	    "No singular common value property could be identified for generic value type '$1$'. Make sure that all subtypes of the generic value type have exactly one property in common (i.e., all direct and indirect subtypes all have a property with same name, and that there is only one such property).";
	case 136 ->
	    "??Property '$2$' of type '$1$' has invalid value for tag ldpExcludedScopes. '$3$' is not a valid ldproxy schema scope.";
	case 137 ->
	    "GeoInfoDok encoding of property '$1$' (value type is '$2$', max mult is '$3$', in fragment '$4$'): No source path info from SQL encoding info, ldpSourcePaths TV, or map entry.";
	case 138 -> "Conversion rule '" + Ldproxy2Constants.RULE_ALL_PROPIDBYTV
		+ "' applies to (main) application schema '$1$', but target parameter '"
		+ Ldproxy2Constants.PARAM_PROP_ID_TAGGED_VALUE
		+ "' either is not set or has no value. The conversion rule will be ignored.";
	case 139 -> "The target configuration does not contain ldproxy stored query definitions.";
	case 140 -> "Exception occurred while generating stored query with id '$1$'. Exception message is: '$2$'.";

	case 10001 -> "Generating ldproxy configuration items for application schema $1$.";
	case 10002 -> "Diagnostics-only mode. All output to files is suppressed.";
	default -> "(" + Ldproxy2Target.class.getName() + ") Unknown message with number: " + mnr;
	};
    }
}
