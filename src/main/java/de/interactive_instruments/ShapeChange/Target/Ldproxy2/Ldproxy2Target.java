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

package de.interactive_instruments.ShapeChange.Target.Ldproxy2;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;

import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.ogcapi.features.gml.domain.ImmutableGmlConfiguration;
import de.ii.ogcapi.features.html.domain.ImmutableFeaturesHtmlConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.ImmutableFeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.ImmutableOgcApiDataV2;
import de.ii.ogcapi.resources.domain.ImmutableResourcesConfiguration;
import de.ii.xtraplatform.codelists.domain.CodelistData.ImportType;
import de.ii.xtraplatform.codelists.domain.ImmutableCodelistData;
import de.ii.xtraplatform.crs.domain.EpsgCrs.Force;
import de.ii.xtraplatform.crs.domain.ImmutableEpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableSchemaConstraints;
import de.ii.xtraplatform.features.domain.SchemaBase.Role;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.features.sql.domain.ConnectionInfoSql.Dialect;
import de.ii.xtraplatform.features.sql.domain.ImmutableConnectionInfoSql;
import de.ii.xtraplatform.features.sql.domain.ImmutableFeatureProviderSqlData;
import de.ii.xtraplatform.features.sql.domain.ImmutableQueryGeneratorSettings;
import de.ii.xtraplatform.features.sql.domain.ImmutableSqlPathDefaults;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import de.interactive_instruments.ShapeChange.MapEntryParamInfos;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.RuleRegistry;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.XmlNamespace;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.SingleTarget;
import de.interactive_instruments.ShapeChange.Target.TargetUtil;
import de.interactive_instruments.ShapeChange.Target.xml_encoding_util.XmlEncodingInfos;
import de.interactive_instruments.ShapeChange.Transformation.Flattening.Flattener;
import de.interactive_instruments.ShapeChange.Util.XMLUtil;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class Ldproxy2Target implements SingleTarget, MessageSource {

    protected static Model model = null;
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

    protected static String associativeTableColumnSuffix = null; // default: value of primaryKeyColumn parameter
    protected static String cfgTemplatePath = "https://shapechange.net/resources/templates/ldproxy2/cfgTemplate.yml";
    protected static String codeTargetTagName = Ldproxy2Constants.DEFAULT_CODE_TARGET_TAG_NAME_VALUE;
    protected static String dateFormat = null; // no default value
    protected static String dateTimeFormat = null; // no default value
    protected static String descriptionTemplate = "[[definition]]";
    protected static String descriptorNoValue = "";
    protected static boolean enableGmlOutput = false;
    protected static Force forceAxisOrder = Force.NONE;
    protected static String foreignKeyColumnSuffix = "";
    protected static String foreignKeyColumnSuffixDatatype = "";
    protected static String foreignKeyColumnSuffixCodelist = "";
    protected static String gmlIdPrefix = null;
    protected static Integer gmlSfLevel = null;
    protected static String gmlFeatureCollectionElementName = null;
    protected static String gmlFeatureMemberElementName = null;
    protected static boolean gmlSupportsStandardResponseParameters = false;
    protected static String labelTemplate = "[[alias]]";
    protected static int maxNameLength = 63;
    protected static ZoneId nativeTimeZone = ZoneId.systemDefault();
    protected static String objectIdentifierName = "oid";
    protected static String primaryKeyColumn = "id";
    protected static String serviceConfigTemplatePathString = null;
    protected static String serviceDescription = "FIXME";
    protected static String serviceLabel = "FIXME";
    protected static int srid = 4326;
    protected static String uomTvName = null;
    protected static XmlEncodingInfos xmlEncodingInfos = new XmlEncodingInfos();

    protected static SortedSet<String> dbSchemaNames = new TreeSet<String>();

    protected static boolean isUnitTest = false;

    protected static String outputDirectory = null;
    protected static File dataDirectoryFile = null;
    protected static String mainId = null;
    protected static PackageInfo mainAppSchema = null;

    /**
     * Contains information parsed from the 'param' attributes of each map entry
     * defined for this target.
     */
    protected static MapEntryParamInfos mapEntryParamInfos = null;

    protected static List<ClassInfo> typesWithIdentity = new ArrayList<>();
    protected static SortedSet<ClassInfo> codelistsAndEnumerations = new TreeSet<>();

    protected static LdproxyCfg cfg = null;

    /* ------ */
    /*
     * Non-static fields
     */
    protected ShapeChangeResult result = null;
    protected Options options = null;

    private PackageInfo schema = null;
    private boolean schemaNotEncoded = false;

    protected SortedMap<String, String> gmlNsabrByNs = new TreeMap<>();
    protected SortedMap<String, String> gmlObjectTypeNamespacesMap = new TreeMap<>();

    /**
     * Property transformations, to be added to the FEATURES_HTML building blocks of
     * type collections in the service configuration.
     * 
     * key outer map: top level type; value outer map: map with key: property path,
     * value: list of property transformations
     */
    protected Map<ClassInfo, SortedMap<String, List<PropertyTransformation>>> propertyTransformationsForBuildingBlock_FeaturesHtml_OfServiceConfigCollectionsByTopLevelClass = new HashMap<>();

    /**
     * Property transformations, to be added to the GML building blocks of type
     * collections in the service configuration.
     * 
     * key outer map: top level type; value outer map: map with key: property path,
     * value: list of property transformations
     */
    protected Map<ClassInfo, SortedMap<String, List<PropertyTransformation>>> propertyTransformationsForBuildingBlock_Gml_OfServiceConfigCollectionsByTopLevelClass = new HashMap<>();

    protected Map<ClassInfo, List<String>> xmlAttributes_Gml_OfServiceConfigCollectionsByTopLevelClass = new HashMap<>();

    protected Map<String, String> gmlFixmeByOriginalSchemaNameMap = new HashMap<>();
    protected int gmlFixmeCounter = 1;

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

	    mainId = mainAppSchema.name().replaceAll("\\W", "_").toLowerCase(Locale.ENGLISH);

	    isUnitTest = options.parameterAsBoolean(this.getClass().getName(), "_unitTestOverride", false);

	    // change the default documentation template?
//	    documentationTemplate = options.parameter(this.getClass().getName(),
//		    Ldproxy2Constants.PARAM_DOCUMENTATION_TEMPLATE);
//	    documentationNoValue = options.parameter(this.getClass().getName(),
//		    Ldproxy2Constants.PARAM_DOCUMENTATION_NOVALUE);

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

	    String forceAxisOrderString = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_FORCE_AXIS_ORDER, "NONE", false, true);
	    forceAxisOrder = Force.valueOf(forceAxisOrderString);

	    foreignKeyColumnSuffix = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_FK_COLUMN_SUFFIX, "", false, true);

	    foreignKeyColumnSuffixDatatype = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_FK_COLUMN_SUFFIX_DATATYPE, "", false, true);

	    foreignKeyColumnSuffixCodelist = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_FK_COLUMN_SUFFIX_CODELIST, "", false, true);

	    labelTemplate = options.parameterAsString(this.getClass().getName(), Ldproxy2Constants.PARAM_LABEL_TEMPLATE,
		    "[[alias]]", false, true);

	    maxNameLength = options.parameterAsInteger(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_MAX_NAME_LENGTH, 63);

	    String nativeTimeZoneParamValue = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_NATIVE_TIME_ZONE, ZoneId.systemDefault().toString(), false, true);
	    nativeTimeZone = ZoneId.of(nativeTimeZoneParamValue);

	    objectIdentifierName = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_OBJECT_IDENTIFIER_NAME, "oid", false, true);

	    primaryKeyColumn = options.parameterAsString(this.getClass().getName(), Ldproxy2Constants.PARAM_PK_COLUMN,
		    "id", false, true);

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

	    // GML relevant parameters

	    enableGmlOutput = options.parameterAsBoolean(this.getClass().getName(), Ldproxy2Constants.PARAM_GML_OUTPUT,
		    false);

	    if (enableGmlOutput) {
		gmlIdPrefix = options.parameterAsString(this.getClass().getName(),
			Ldproxy2Constants.PARAM_GML_ID_PREFIX, null, false, true);
		gmlSfLevel = options.parameterAsInteger(this.getClass().getName(), Ldproxy2Constants.PARAM_GML_SF_LEVEL,
			-1);
		uomTvName = options.parameterAsString(this.getClass().getName(), Ldproxy2Constants.PARAM_UOM_TV_NAME,
			null, false, true);

		gmlFeatureCollectionElementName = options.parameterAsString(this.getClass().getName(),
			Ldproxy2Constants.PARAM_GML_FEATURE_COLLECTION_ELEMENT_NAME, null, false, true);

		gmlFeatureMemberElementName = options.parameterAsString(this.getClass().getName(),
			Ldproxy2Constants.PARAM_GML_FEATURE_MEMBER_ELEMENT_NAME, null, false, true);

		gmlSupportsStandardResponseParameters = options.parameterAsBoolean(this.getClass().getName(),
			Ldproxy2Constants.PARAM_GML_SUPPORTS_STANDARD_RESPONSE_PARAMETERS, false);

		if (!options.getCurrentProcessConfig().hasAdvancedProcessConfigurations()) {
		    result.addInfo(this, 126);
		} else {
		    Element advancedProcessConfigElmt = options.getCurrentProcessConfig()
			    .getAdvancedProcessConfigurations();
		    List<Element> xeiElmts = XMLUtil.getChildElements(advancedProcessConfigElmt, "XmlEncodingInfos");

		    if (xeiElmts.isEmpty()) {
			result.addInfo(this, 126);
		    } else {
			for (Element xeiElmt : xeiElmts) {
			    xmlEncodingInfos.merge(XmlEncodingInfos.fromXml(xeiElmt));
			}
		    }
		}
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

	    dataDirectoryFile = new File(outputDirectory, "data");
	    Path dataDirectoryPath = dataDirectoryFile.toPath();
	    cfg = new LdproxyCfg(dataDirectoryPath);
	}

	/*
	 * Required to be performed for each application schema
	 */
	result.addDebug(this, 10001, pi.name());
    }

    public static boolean isEncoded(Info i) {

	if (i.matches(Ldproxy2Constants.RULE_ALL_NOT_ENCODED)
		&& i.encodingRule(Ldproxy2Constants.PLATFORM).equalsIgnoreCase("notencoded")) {

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

	if (!isEncoded(ci)) {
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

	if (ci.isAbstract()) {
	    MessageContext mc = result.addWarning(this, 20, ci.name());
	    if (mc != null) {
		mc.addDetail(this, 0, ci.fullNameInSchema());
	    }
	    return;
	}

	if (!ci.supertypes().isEmpty()) {
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

	/*
	 * 2022-08-25 JE: Handling of unions just like data types deactivated. For the
	 * time being, we keep the approach with type flattening.
	 */
	if (ci.category() == Options.DATATYPE /* || ci.category() == Options.UNION */) {

	    // ignore here - will be encoded as needed

	} else if (ci.category() == Options.OBJECT || ci.category() == Options.FEATURE) {

	    if (typesWithIdentity.stream().anyMatch(t -> t.name().equalsIgnoreCase(ci.name()))) {
		MessageContext mc = result.addError(this, 125, ci.name());
		if (mc != null) {
		    mc.addDetail(this, 0, ci.fullNameInSchema());
		}
	    } else {
		typesWithIdentity.add(ci);
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

	    // NOTE: conversion of unions and mixins not supported

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

	    if (type == null || !Ldproxy2Target.isEncoded(type) || !model.isInSelectedSchemas(type)) {
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

	    if (enableGmlOutput) {
		gmlNsabrByNs.put(mainAppSchema.targetNamespace(), mainAppSchema.xmlns());
	    }

	    // copy template file
	    if (StringUtils.isNotBlank(cfgTemplatePath)) {

		File cfgFile = new File(dataDirectoryFile, "cfg.yml");

		try {
		    if (cfgTemplatePath.startsWith("http")) {
			FileUtils.copyURLToFile(new URL(cfgTemplatePath), cfgFile);
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
			FileUtils.copyURLToFile(new URL(serviceConfigTemplatePathString), serviceConfigTemplateFile);
		    } else {
			FileUtils.copyFile(new File(serviceConfigTemplatePathString), serviceConfigTemplateFile);
		    }
		} catch (IOException e) {
		    result.addError(this, 123, Ldproxy2Constants.PARAM_SERVICE_CONFIG_TEMPLATE_PATH,
			    serviceConfigTemplatePathString, serviceConfigTemplateFile.getAbsolutePath());
		}
	    }

	    /*
	     * ===================================
	     * 
	     * BUILD CODELIST AND TYPE DEFINITIONS
	     * 
	     * ===================================
	     */

	    List<ImmutableCodelistData> codelists = new ArrayList<>();

	    for (ClassInfo ci : codelistsAndEnumerations) {

		ImmutableCodelistData icd = createCodelistEntity(ci);

		codelists.add(icd);
	    }

	    SortedMap<String, FeatureSchema> providerTypeDefinitions = new TreeMap<>();
	    SortedMap<String, FeatureTypeConfigurationOgcApi> serviceCollectionDefinitions = new TreeMap<>();

	    for (ClassInfo ci : typesWithIdentity) {

		String typeDefName = ci.name().toLowerCase(Locale.ENGLISH);

		/*
		 * Create provider config entry
		 */
		LinkedHashMap<String, FeatureSchema> propertyDefs = propertyDefinitions(ci,
			new ArrayList<PropertyInfo>());

		ImmutableFeatureSchema typeDef = new ImmutableFeatureSchema.Builder().type(Type.OBJECT)
			.name(typeDefName).objectType(ci.name()).label(label(ci))
			.sourcePath("/" + databaseTableName(ci, false)).description(description(ci))
			.propertyMap(propertyDefs).build();

		if (enableGmlOutput) {
		    String nsabr = gmlNsabr(gmlXmlNamespace(ci));
		    if (!nsabr.equals(mainAppSchema.xmlns())) {
			gmlObjectTypeNamespacesMap.put(ci.name(), nsabr);
		    }
		}

		providerTypeDefinitions.put(typeDefName, typeDef);

		/*
		 * Create service config entry (must be done after provider config entry
		 * creation, in order for codelist transformation infos to be available for
		 * inclusion in the service config)
		 */

		List<ExtensionConfiguration> extensionConfigurations = new ArrayList<>();

		ImmutableFeaturesHtmlConfiguration.Builder fhtmlBuilder = cfg.builder().ogcApiExtension()
			.featuresHtml();
		fhtmlBuilder.featureTitleTemplate(featureTitleTemplate(ci));
		if (propertyTransformationsForBuildingBlock_FeaturesHtml_OfServiceConfigCollectionsByTopLevelClass
			.containsKey(ci)) {
		    SortedMap<String, List<PropertyTransformation>> featuresHtmlPropertyTransformations = propertyTransformationsForBuildingBlock_FeaturesHtml_OfServiceConfigCollectionsByTopLevelClass
			    .get(ci);
		    fhtmlBuilder.transformations(featuresHtmlPropertyTransformations);
		}
		extensionConfigurations.add(fhtmlBuilder.build());

		if (enableGmlOutput) {
		    ImmutableGmlConfiguration.Builder gmlBuilder = cfg.builder().ogcApiExtension().gml();
		    if (propertyTransformationsForBuildingBlock_Gml_OfServiceConfigCollectionsByTopLevelClass
			    .containsKey(ci)) {
			SortedMap<String, List<PropertyTransformation>> gmlPropertyTransformations = propertyTransformationsForBuildingBlock_Gml_OfServiceConfigCollectionsByTopLevelClass
				.get(ci);
			gmlBuilder.transformations(gmlPropertyTransformations);
		    }
		    if (xmlAttributes_Gml_OfServiceConfigCollectionsByTopLevelClass.containsKey(ci)) {
			List<String> xmlAttributeCases = xmlAttributes_Gml_OfServiceConfigCollectionsByTopLevelClass
				.get(ci);
			gmlBuilder.addAllXmlAttributes(xmlAttributeCases);
		    }
		    extensionConfigurations.add(gmlBuilder.build());
		}

		ImmutableFeatureTypeConfigurationOgcApi serviceCollDef = new ImmutableFeatureTypeConfigurationOgcApi.Builder()
			.id(typeDefName).label(typeDefName).addAllExtensions(extensionConfigurations).build();

		serviceCollectionDefinitions.put(typeDefName, serviceCollDef);
	    }

	    /*
	     * ================================
	     * 
	     * CREATE COMMON GML CONFIGURATION
	     * 
	     * ================================
	     */

	    ImmutableGmlConfiguration.Builder gmlBuilder = null;

	    ImmutableResourcesConfiguration.Builder resourcesBuilder = null;

	    if (enableGmlOutput) {

		gmlBuilder = cfg.builder().ogcApiExtension().gml();

		gmlBuilder.enabled(true);

		SortedMap<String, String> appNamespaces = new TreeMap<>();
		for (Entry<String, String> e : gmlNsabrByNs.entrySet()) {
		    appNamespaces.put(e.getValue(), e.getKey());
		}
		gmlBuilder.putAllApplicationNamespaces(appNamespaces);

		gmlBuilder.defaultNamespace(mainAppSchema.xmlns());

		String schemaLocationForMainAppSchema = xmlEncodingInfos.getXmlNamespaces().stream()
			.filter(xn -> xn.getNs().equals(mainAppSchema.targetNamespace()) && xn.hasLocation())
			.map(xn -> xn.getLocation()).findFirst().orElse(null);
		if (StringUtils.isBlank(schemaLocationForMainAppSchema)) {
		    // Assume that the XSD for the main schema is hosted as local resource
		    resourcesBuilder = cfg.builder().ogcApiExtension().resources();
		    resourcesBuilder.enabled(true);
		    schemaLocationForMainAppSchema = "{{serviceUrl}}/resources/" + mainAppSchema.xsdDocument();
		    if (!schemaLocationForMainAppSchema.toLowerCase(Locale.ENGLISH).endsWith(".xsd")) {
			schemaLocationForMainAppSchema += ".xsd";
		    }
		}

		SortedMap<String, String> schemaLocations = new TreeMap<>();
		schemaLocations.put(mainAppSchema.xmlns(), schemaLocationForMainAppSchema);
		for (XmlNamespace xns : xmlEncodingInfos.getXmlNamespaces()) {
		    if (xns.hasLocation() && gmlNsabrByNs.containsKey(xns.getNs())) {
			schemaLocations.put(xns.getNs(), xns.getLocation());
		    }
		}

		gmlBuilder.putAllSchemaLocations(schemaLocations);

		gmlBuilder.objectTypeNamespaces(gmlObjectTypeNamespacesMap);

		if (StringUtils.isNotBlank(gmlIdPrefix)) {
		    gmlBuilder.gmlIdPrefix(gmlIdPrefix);
		}
		if (gmlSfLevel != null && gmlSfLevel != -1) {
		    gmlBuilder.gmlSfLevel(gmlSfLevel);
		}
		if (StringUtils.isNotBlank(gmlFeatureCollectionElementName)) {
		    gmlBuilder.featureCollectionElementName(gmlFeatureCollectionElementName);
		}
		if (StringUtils.isNotBlank(gmlFeatureMemberElementName)) {
		    gmlBuilder.featureMemberElementName(gmlFeatureMemberElementName);
		}
		gmlBuilder.supportsStandardResponseParameters(gmlSupportsStandardResponseParameters);
	    }

	    /*
	     * ================================
	     * 
	     * BUILD THE SERVICE CONFIGURATION
	     * 
	     * ================================
	     */

	    List<ExtensionConfiguration> generalExtensionConfigurations = new ArrayList<>();
	    if (resourcesBuilder != null) {
		generalExtensionConfigurations.add(resourcesBuilder.build());
	    }
	    if (gmlBuilder != null) {
		generalExtensionConfigurations.add(gmlBuilder.build());
	    }

	    ImmutableOgcApiDataV2 serviceConfig = cfg.builder().entity().api().id(mainId).entityStorageVersion(2)
		    .label(serviceLabel).description(serviceDescription).serviceType("OGC_API")
		    .addAllExtensions(generalExtensionConfigurations).collections(serviceCollectionDefinitions).build();

	    if (isUnitTest) {
		serviceConfig = serviceConfig.withCreatedAt(Ldproxy2Constants.UNITTEST_UNIX_TIME)
			.withLastModified(Ldproxy2Constants.UNITTEST_UNIX_TIME);
	    }

	    /*
	     * =================================
	     * 
	     * BUILD THE PROVIDER CONFIGURATION
	     * 
	     * =================================
	     */

	    ImmutableConnectionInfoSql connectionInfo = cfg.builder().entity().provider().connectionInfoBuilder()
		    .dialect(Dialect.PGIS).database("FIXME").host("FIXME").user("FIXME")
		    .password("FIXME-base64-encoded").schemas(dbSchemaNames).build();

	    ImmutableSqlPathDefaults sourcePathDefaults = cfg.builder().entity().provider().sourcePathDefaultsBuilder()
		    .primaryKey(primaryKeyColumn).sortKey(primaryKeyColumn).build();

	    ImmutableQueryGeneratorSettings queryGeneration = cfg.builder().entity().provider().queryGenerationBuilder()
		    .computeNumberMatched(true).build();

	    ImmutableEpsgCrs nativeCrs = cfg.builder().entity().provider().nativeCrsBuilder().code(srid)
		    .forceAxisOrder(forceAxisOrder).build();

	    ImmutableFeatureProviderSqlData providerConfig = cfg.builder().entity().provider().id(mainId)
		    .providerType("FEATURE").entityStorageVersion(2).providerType("FEATURE").featureProviderType("SQL")
		    .connectionInfo(connectionInfo).sourcePathDefaults(sourcePathDefaults)
		    .queryGeneration(queryGeneration).nativeCrs(nativeCrs).nativeTimeZone(nativeTimeZone)
		    .types(providerTypeDefinitions).build();

	    if (isUnitTest) {
		providerConfig = providerConfig.withCreatedAt(Ldproxy2Constants.UNITTEST_UNIX_TIME)
			.withLastModified(Ldproxy2Constants.UNITTEST_UNIX_TIME);
	    }

	    // write api
	    try {
		if (serviceConfigTemplateFile.exists()) {
		    cfg.writeEntity(serviceConfig, serviceConfigTemplateFile.toPath());
		} else {
		    cfg.writeEntity(serviceConfig);
		}
		cfg.writeEntity(providerConfig);
		for (ImmutableCodelistData icd : codelists) {
		    cfg.writeEntity(icd);
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

    private LinkedHashMap<String, FeatureSchema> propertyDefinitions(ClassInfo currentCi,
	    List<PropertyInfo> alreadyVisitedPiList) {

	LinkedHashMap<String, FeatureSchema> propertyDefs = new LinkedHashMap<>();

	/*
	 * DETERMINE SPECIAL PROPERTIES - ONLY RELEVANT FOR TOP-LEVEL CLASS
	 */

	PropertyInfo identifierPi = null;
	boolean multipleIdentifierPisEncountered = false;
	PropertyInfo defaultGeometryPi = null;
	boolean multipleDefaultGeometriesEncountered = false;
	PropertyInfo defaultInstantPi = null;
	boolean multipleDefaultInstantsEncountered = false;
	PropertyInfo defaultIntervalStartPi = null;
	boolean multipleDefaultIntervalStartsEncountered = false;
	PropertyInfo defaultIntervalEndPi = null;
	boolean multipleDefaultIntervalEndsEncountered = false;

	if (alreadyVisitedPiList.isEmpty()) {

	    for (PropertyInfo pi : currentCi.properties().values()) {

		if (!isEncoded(pi)) {
		    continue;
		}

		if (!valueTypeIsMapped(pi)) {

		    if (pi.typeClass() == null || !isEncoded(pi.typeClass())) {
			MessageContext mc = result.addError(this, 124, pi.typeInfo().name, pi.name(),
				pi.inClass().name());
			if (mc != null) {
			    mc.addDetail(this, 1, pi.fullNameInSchema());
			}
			continue;
		    } else if (unsupportedCategoryOfValue(pi)) {
			MessageContext mc = result.addError(this, 120, pi.typeInfo().name, pi.name(),
				pi.inClass().name());
			if (mc != null) {
			    mc.addDetail(this, 1, pi.fullNameInSchema());
			}
			continue;
		    }
		}

		if (pi.stereotype("identifier")
			&& currentCi.matches(Ldproxy2Constants.RULE_CLS_IDENTIFIER_STEREOTYPE)) {

		    if (!multipleIdentifierPisEncountered) {
			identifierPi = pi;
			if (pi.cardinality().maxOccurs > 1) {
			    MessageContext mc = result.addWarning(this, 104, currentCi.name(), pi.name());
			    if (mc != null) {
				mc.addDetail(this, 1, pi.fullNameInSchema());
			    }
			}
		    } else {
			multipleIdentifierPisEncountered = true;
			MessageContext mc = result.addError(this, 107, pi.inClass().name(), pi.name(),
				identifierPi.name());
			if (mc != null) {
			    mc.addDetail(this, 1, pi.fullNameInSchema());
			}
		    }

		}

		if (isTrueIgnoringCase(pi.taggedValue("defaultGeometry"))) {
		    if (!multipleDefaultGeometriesEncountered) {
			defaultGeometryPi = pi;
		    } else {
			multipleDefaultGeometriesEncountered = true;
			MessageContext mc = result.addError(this, 105, pi.inClass().name(), pi.name(),
				defaultGeometryPi.name());
			if (mc != null) {
			    mc.addDetail(this, 1, pi.fullNameInSchema());
			}
		    }
		}

		boolean isDefaultInstant = isTrueIgnoringCase(pi.taggedValue("defaultInstant"));
		boolean isDefaultIntervalStart = isTrueIgnoringCase(pi.taggedValue("defaultIntervalStart"));
		boolean isDefaultIntervalEnd = isTrueIgnoringCase(pi.taggedValue("defaultIntervalEnd"));

		if (isDefaultInstant && (isDefaultIntervalStart || isDefaultIntervalEnd)) {

		    MessageContext mc = result.addError(this, 106, pi.inClass().name(), pi.name());
		    if (mc != null) {
			mc.addDetail(this, 1, pi.fullNameInSchema());
		    }

		} else if (isDefaultInstant) {

		    if (!multipleDefaultInstantsEncountered) {
			defaultInstantPi = pi;
		    } else {
			multipleDefaultInstantsEncountered = true;
			MessageContext mc = result.addError(this, 108, pi.inClass().name(), pi.name(),
				defaultInstantPi.name());
			if (mc != null) {
			    mc.addDetail(this, 1, pi.fullNameInSchema());
			}
		    }

		} else {

		    if (isDefaultIntervalStart && isDefaultIntervalEnd) {

			MessageContext mc = result.addError(this, 109, pi.inClass().name(), pi.name());
			if (mc != null) {
			    mc.addDetail(this, 1, pi.fullNameInSchema());
			}

		    } else if (isDefaultIntervalStart) {

			if (!multipleDefaultIntervalStartsEncountered) {
			    defaultIntervalStartPi = pi;
			} else {
			    multipleDefaultIntervalStartsEncountered = true;
			    MessageContext mc = result.addError(this, 110, pi.inClass().name(), pi.name(),
				    defaultIntervalStartPi.name());
			    if (mc != null) {
				mc.addDetail(this, 1, pi.fullNameInSchema());
			    }
			}

		    } else if (isDefaultIntervalEnd) {
			if (!multipleDefaultIntervalEndsEncountered) {
			    defaultIntervalEndPi = pi;
			} else {
			    multipleDefaultIntervalEndsEncountered = true;
			    MessageContext mc = result.addError(this, 111, pi.inClass().name(), pi.name(),
				    defaultIntervalEndPi.name());
			    if (mc != null) {
				mc.addDetail(this, 1, pi.fullNameInSchema());
			    }
			}
		    }
		}

	    }

	    if (identifierPi == null || multipleIdentifierPisEncountered) {
		ImmutableFeatureSchema identifierMemberDef = new ImmutableFeatureSchema.Builder()
			.name(objectIdentifierName).sourcePath(primaryKeyColumn).type(Type.INTEGER).role(Role.ID)
			.build();
		propertyDefs.put(identifierMemberDef.getName(), identifierMemberDef);
	    }
	}

	for (PropertyInfo pi : currentCi.properties().values()) {

	    if (!isEncoded(pi)) {
		continue;
	    }

	    if (!valueTypeIsMapped(pi)) {

		if (pi.typeClass() == null || !isEncoded(pi.typeClass())) {
		    MessageContext mc = result.addError(this, 124, pi.typeInfo().name, pi.name(), pi.inClass().name());
		    if (mc != null) {
			mc.addDetail(this, 1, pi.fullNameInSchema());
		    }
		    continue;
		} else if (unsupportedCategoryOfValue(pi)) {
		    MessageContext mc = result.addError(this, 120, pi.typeInfo().name, pi.name(), pi.inClass().name());
		    if (mc != null) {
			mc.addDetail(this, 1, pi.fullNameInSchema());
		    }
		    continue;
		}
	    }

	    List<PropertyInfo> nowVisitedList = new ArrayList<>(alreadyVisitedPiList);
	    nowVisitedList.add(pi);

	    ImmutableFeatureSchema.Builder propMemberDefBuilder = new ImmutableFeatureSchema.Builder();

	    Type ldpType = ldproxyType(pi);

	    if (pi.cardinality().maxOccurs > 1) {
		if (ldpType == Type.OBJECT) {
		    propMemberDefBuilder.type(Type.OBJECT_ARRAY);
		} else if (ldpType == Type.GEOMETRY || pi == identifierPi) {
		    // no array for geometry and identifier properties
		    propMemberDefBuilder.type(ldpType);
		} else {
		    propMemberDefBuilder.type(Type.VALUE_ARRAY);
		    propMemberDefBuilder.valueType(ldpType);
		}
	    } else {
		propMemberDefBuilder.type(ldpType);
	    }

	    if (ldpType == Type.GEOMETRY) {
		propMemberDefBuilder.geometryType(geometryType(pi));
	    }

	    if ((ldpType == Type.FLOAT || ldpType == Type.INTEGER) && StringUtils.isNotBlank(uomTvName)
		    && StringUtils.isNotBlank(pi.taggedValue(uomTvName))) {
		propMemberDefBuilder.unit(pi.taggedValue(uomTvName).trim());
	    }

	    Optional<Role> propRole;

	    if (identifierPi != null && !multipleIdentifierPisEncountered && pi == identifierPi) {
		propRole = Optional.of(Role.ID);
	    } else if (defaultGeometryPi != null && !multipleDefaultGeometriesEncountered && pi == defaultGeometryPi) {
		propRole = Optional.of(Role.PRIMARY_GEOMETRY);
	    } else if (defaultInstantPi != null && !multipleDefaultInstantsEncountered && pi == defaultInstantPi) {
		propRole = Optional.of(Role.PRIMARY_INSTANT);
	    } else if (defaultIntervalStartPi != null && !multipleDefaultIntervalStartsEncountered
		    && pi == defaultIntervalStartPi) {
		propRole = Optional.of(Role.PRIMARY_INTERVAL_START);
	    } else if (defaultIntervalEndPi != null && !multipleDefaultIntervalEndsEncountered
		    && pi == defaultIntervalEndPi) {
		propRole = Optional.of(Role.PRIMARY_INTERVAL_END);
	    } else {
		propRole = Optional.empty();
	    }
	    propMemberDefBuilder.role(propRole);

	    Optional<String> sourcePathProperty = sourcePathPropertyLevel(pi);

	    boolean ignoreSourcePathOnPropertyLevel = false;

	    // handle embedded cases (datatype or link properties)
	    if (isMappedToOrImplementedAsLink(pi)) {

		// IMPORTANT: The object type must be 'Link', NOT 'LINK'!
		propMemberDefBuilder.objectType("Link");

		ignoreSourcePathOnPropertyLevel = pi.cardinality().maxOccurs == 1;

		LinkedHashMap<String, FeatureSchema> linkPropertyDefs = new LinkedHashMap<>();

		String sourcePathInLinkProps = sourcePathLinkLevel(pi);

		linkPropertyDefs.put("title", new ImmutableFeatureSchema.Builder().name("title")
			.label(pi.typeInfo().name + "-ID").type(Type.STRING).sourcePath(sourcePathInLinkProps).build());

		ImmutableFeatureSchema.Builder linkPropHrefBuilder = new ImmutableFeatureSchema.Builder();
		linkPropHrefBuilder.name("href").label(pi.typeInfo().name + "-ID").type(Type.STRING)
			.sourcePath(sourcePathInLinkProps);
		linkPropHrefBuilder.addAllTransformationsBuilders(
			new ImmutablePropertyTransformation.Builder().stringFormat(urlTemplateForValueType(pi)));
		linkPropertyDefs.put("href", linkPropHrefBuilder.build());

		propMemberDefBuilder.propertyMap(linkPropertyDefs);

	    } else if (!isLdproxySimpleType(ldpType)
		    && (pi.categoryOfValue() == Options.DATATYPE /* || pi.categoryOfValue() == Options.UNION */)) {

		/*
		 * 2022-08-25 JE: Handling of unions just like data types deactivated. For the
		 * time being, we keep the approach with type flattening.
		 */
		
		ClassInfo typeCi = pi.typeClass();

		// detect circular dependency in the property path
		if (alreadyVisitedPiList.stream().anyMatch(vPi -> vPi.inClass() == typeCi) || typeCi == currentCi) {

		    ClassInfo topType = alreadyVisitedPiList.get(0).inClass();

		    MessageContext mc = result.addError(this, 117, topType.name(), propertyPath(nowVisitedList));
		    if (mc != null) {
			mc.addDetail(this, 0, topType.fullNameInSchema());
		    }

		    continue;

		} else {

		    LinkedHashMap<String, FeatureSchema> datatypePropertyDefs = propertyDefinitions(typeCi,
			    nowVisitedList);
		    propMemberDefBuilder.propertyMap(datatypePropertyDefs);

		    propMemberDefBuilder.objectType(typeCi.name());
		    if (enableGmlOutput) {
			String nsabr = gmlNsabr(gmlXmlNamespace(typeCi));
			if (!nsabr.equals(mainAppSchema.xmlns())) {
			    gmlObjectTypeNamespacesMap.put(typeCi.name(), nsabr);
			}
		    }
		}
	    }

	    if (StringUtils.isNotBlank(pi.initialValue()) && pi.isReadOnly()
		    && pi.matches(Ldproxy2Constants.RULE_PROP_READONLY)) {

		propMemberDefBuilder.constantValue(constantValue(pi));

	    } else if (!ignoreSourcePathOnPropertyLevel) {

		propMemberDefBuilder.sourcePath(sourcePathProperty);
	    }

	    // Generate constraints
	    Optional<ImmutableSchemaConstraints> constraints = Optional.empty();
	    boolean providerConfigConstraintCreated = false;
	    ImmutableSchemaConstraints.Builder constraintsBuilder = new ImmutableSchemaConstraints.Builder();
	    if (pi.cardinality().minOccurs != 0 && !pi.voidable()/* && pi.inClass().category() != Options.UNION */) {
		/*
		 * 2022-08-25 JE: Handling of unions just like data types deactivated. For the
		 * time being, we keep the approach with type flattening.
		 */
		providerConfigConstraintCreated = true;
		constraintsBuilder.required(true);
	    }
	    if (pi.cardinality().maxOccurs > 1 && !(pi == identifierPi || pi == defaultGeometryPi)
		    && ldpType != Type.GEOMETRY) {
		if (pi.voidable()) {
		    providerConfigConstraintCreated = true;
		    constraintsBuilder.minOccurrence(0);
		} else {
		    providerConfigConstraintCreated = true;
		    constraintsBuilder.minOccurrence(pi.cardinality().minOccurs);
		}
		if (pi.cardinality().maxOccurs != Integer.MAX_VALUE) {
		    providerConfigConstraintCreated = true;
		    constraintsBuilder.maxOccurrence(pi.cardinality().maxOccurs);
		}
	    }
	    if (isEnumerationOrCodelistValueType(pi) && !valueTypeIsMapped(pi)) {

		ClassInfo typeCi = pi.typeClass();

		providerConfigConstraintCreated = true;
		String codelistId = codelistId(typeCi);

		if (!codelistsAndEnumerations.contains(typeCi)) {
		    /*
		     * Handle case of enumeration/codelist that is not encoded, unmapped, or not
		     * from the schemas selected for processing.
		     */
		    MessageContext mc = result.addWarning(this, 127, typeCi.name(), codelistId);
		    if (mc != null) {
			mc.addDetail(this, 0, typeCi.fullNameInSchema());
		    }
		}

		constraintsBuilder.codelist(codelistId);

		if (pi.categoryOfValue() == Options.ENUMERATION
			&& typeCi.matches(Ldproxy2Constants.RULE_CLS_ENUMERATION_ENUM_CONSTRAINT)) {
		    constraintsBuilder.enumValues(enumValues(typeCi));
		}

		// now create content for inclusion in service config:
		ImmutablePropertyTransformation trf = new ImmutablePropertyTransformation.Builder().codelist(codelistId)
			.build();
		addPropertyTransformationToBuildingBlockOfCollectionInServiceConfiguration(
			nowVisitedList.get(0).inClass(), propertyPath(nowVisitedList), trf,
			propertyTransformationsForBuildingBlock_FeaturesHtml_OfServiceConfigCollectionsByTopLevelClass);
	    }

	    if (providerConfigConstraintCreated) {
		constraints = Optional.of(constraintsBuilder.build());
	    }
	    propMemberDefBuilder.constraints(constraints);

	    ImmutableFeatureSchema propMemberDef = propMemberDefBuilder.name(pi.name()).label(label(pi))
		    .description(description(pi)).build();
	    propertyDefs.put(pi.name(), propMemberDef);

	    // create more service constraint content
	    if (StringUtils.isNotBlank(pi.taggedValue("ldpRemove"))) {
		String tv = pi.taggedValue("ldpRemove").trim().toUpperCase(Locale.ENGLISH);
		if (tv.equals("IN_COLLECTION") || tv.equals("ALWAYS") || tv.equals("NEVER")) {
		    ImmutablePropertyTransformation trf = new ImmutablePropertyTransformation.Builder().remove(tv)
			    .build();
		    addPropertyTransformationToBuildingBlockOfCollectionInServiceConfiguration(
			    nowVisitedList.get(0).inClass(), propertyPath(nowVisitedList), trf,
			    propertyTransformationsForBuildingBlock_FeaturesHtml_OfServiceConfigCollectionsByTopLevelClass);
		} else {
		    MessageContext mc = result.addError(this, 122, pi.name(), pi.taggedValue("ldpRemove"));
		    if (mc != null) {
			mc.addDetail(this, 1, pi.fullNameInSchema());
		    }
		}
	    }
	    if (ldpType == Type.DATE && StringUtils.isNotBlank(dateFormat)) {
		ImmutablePropertyTransformation trf = new ImmutablePropertyTransformation.Builder()
			.dateFormat(dateFormat).build();
		addPropertyTransformationToBuildingBlockOfCollectionInServiceConfiguration(
			nowVisitedList.get(0).inClass(), propertyPath(nowVisitedList), trf,
			propertyTransformationsForBuildingBlock_FeaturesHtml_OfServiceConfigCollectionsByTopLevelClass);
	    }
	    if (ldpType == Type.DATETIME && StringUtils.isNotBlank(dateTimeFormat)) {
		ImmutablePropertyTransformation trf = new ImmutablePropertyTransformation.Builder()
			.dateFormat(dateTimeFormat).build();
		addPropertyTransformationToBuildingBlockOfCollectionInServiceConfiguration(
			nowVisitedList.get(0).inClass(), propertyPath(nowVisitedList), trf,
			propertyTransformationsForBuildingBlock_FeaturesHtml_OfServiceConfigCollectionsByTopLevelClass);
	    }

	    if (enableGmlOutput) {

		ClassInfo firstCi = nowVisitedList.get(0).inClass();

		if (gmlRenameRequired(pi, gmlXmlNamespace(firstCi))) {

		    String xmlQNameToUse = gmlQName(pi);

		    ImmutablePropertyTransformation trf = new ImmutablePropertyTransformation.Builder()
			    .rename(xmlQNameToUse).build();
		    addPropertyTransformationToBuildingBlockOfCollectionInServiceConfiguration(firstCi,
			    propertyPath(nowVisitedList), trf,
			    propertyTransformationsForBuildingBlock_Gml_OfServiceConfigCollectionsByTopLevelClass);
		}

		if (gmlAsAttribute(pi)) {

		    List<String> xmlAttributes = xmlAttributes_Gml_OfServiceConfigCollectionsByTopLevelClass
			    .get(firstCi);
		    if (xmlAttributes == null) {
			xmlAttributes = new ArrayList<>();
			xmlAttributes_Gml_OfServiceConfigCollectionsByTopLevelClass.put(firstCi, xmlAttributes);
		    }
		    xmlAttributes.add(propertyPath(nowVisitedList));
		}
	    }
	}

	return propertyDefs;
    }

    private boolean gmlAsAttribute(PropertyInfo pi) {

	String originalInClassName = originalInClassName(pi);
	String originalSchemaName = originalSchemaName(pi);
	String originalPropertyName = originalPropertyName(pi);

	return xmlEncodingInfos.isXmlAttribute(originalSchemaName, originalInClassName, originalPropertyName);
    }

    private String gmlQName(PropertyInfo pi) {

	String originalInClassName = originalInClassName(pi);
	String originalSchemaName = originalSchemaName(pi);
	String originalPropertyName = originalPropertyName(pi);

	String xmlNamespaceToUse = gmlXmlNamespace(pi);

	String xmlNameToUse = originalPropertyName;
	Optional<String> xmlNameOpt = xmlEncodingInfos.getXmlName(originalSchemaName, originalInClassName,
		originalPropertyName);
	if (xmlNameOpt.isPresent() && !xmlNameOpt.get().equals(originalPropertyName)) {
	    xmlNameToUse = xmlNameOpt.get();
	}

	String nsabrToUse = gmlNsabr(xmlNamespaceToUse);

	if (nsabrToUse.equals(mainAppSchema.xmlns())) {
	    return xmlNameToUse;
	} else {
	    return nsabrToUse + ":" + xmlNameToUse;
	}
    }

    private String gmlXmlNamespace(ClassInfo ci) {

	String className = ci.name();

	PackageInfo schemaPkg = model.schemaPackage(ci);
	String schemaName = schemaPkg == null ? null : schemaPkg.name();

	Optional<String> xmlNsOpt = xmlEncodingInfos.getXmlNamespace(schemaName, className);

	if (xmlNsOpt.isPresent()) {
	    return xmlNsOpt.get();
	} else {

	    String targetNamespace = ci.pkg().targetNamespace();
	    if (StringUtils.isBlank(targetNamespace)) {

		// Note: schemaName can be null - is supported as key by Java HashMap
		if (gmlFixmeByOriginalSchemaNameMap.containsKey(schemaName)) {
		    targetNamespace = gmlFixmeByOriginalSchemaNameMap.get(schemaName);
		} else {
		    targetNamespace = gmlFixmeForSchema(schemaName);
		    result.addWarning(this, 129, schemaName == null ? "<no schema found>" : schemaName,
			    targetNamespace);
		}
	    }
	    return targetNamespace;
	}
    }

    private String gmlXmlNamespace(PropertyInfo pi) {

	String originalInClassName = originalInClassName(pi);
	String originalSchemaName = originalSchemaName(pi);
	String originalPropertyName = originalPropertyName(pi);

	String piTargetNamespace = pi.inClass().pkg().targetNamespace();
	Optional<String> xmlNsOpt = xmlEncodingInfos.getXmlNamespace(originalSchemaName,
		originalInClassName + "::" + originalPropertyName);

	String xmlNamespaceToUse = piTargetNamespace;
	if (xmlNsOpt.isPresent()) {
	    if (!xmlNsOpt.get().equals(piTargetNamespace)) {
		xmlNamespaceToUse = xmlNsOpt.get();
	    }
	} else {
	    if (!originalSchemaName.equals(pi.model().schemaPackage(pi.inClass()).name())) {
		SortedSet<PackageInfo> originalSchema = model.schemas(originalSchemaName);
		if (originalSchema.isEmpty() || StringUtils.isBlank(originalSchema.first().targetNamespace())) {
		    if (gmlFixmeByOriginalSchemaNameMap.containsKey(originalSchemaName)) {
			xmlNamespaceToUse = gmlFixmeByOriginalSchemaNameMap.get(originalSchemaName);
		    } else {
			xmlNamespaceToUse = gmlFixmeForSchema(originalSchemaName);
			result.addWarning(this, 129, originalSchemaName, xmlNamespaceToUse);
		    }
		} else {
		    xmlNamespaceToUse = originalSchema.first().targetNamespace();
		}
	    }
	}

	return xmlNamespaceToUse;
    }

    private String gmlNsabr(String xmlNamespace) {

	if (gmlNsabrByNs.containsKey(xmlNamespace)) {
	    return gmlNsabrByNs.get(xmlNamespace);
	} else {

	    String nsabrToUse = null;

	    Optional<String> nsabr = xmlEncodingInfos.findNsabr(xmlNamespace);
	    if (nsabr.isPresent()) {
		nsabrToUse = nsabr.get();
	    } else {
		boolean foundSchemaPackage = false;
		for (PackageInfo schema : model.schemas(null)) {
		    if (schema.targetNamespace().equals(xmlNamespace)) {
			foundSchemaPackage = true;
			nsabrToUse = schema.xmlns();
			break;
		    }
		}
		if (!foundSchemaPackage || StringUtils.isBlank(nsabrToUse)) {
		    String fixme = gmlNewFixme();
		    result.addWarning(this, 130, xmlNamespace, fixme);
		    nsabrToUse = fixme;
		}
	    }

	    gmlNsabrByNs.put(xmlNamespace, nsabrToUse);
	    return nsabrToUse;
	}
    }

    private String gmlFixmeForSchema(String originalSchemaName) {

	String newFixme = gmlNewFixme();
	gmlFixmeByOriginalSchemaNameMap.put(originalSchemaName, newFixme);
	return newFixme;
    }

    private String gmlNewFixme() {
	String newFixme = "fixme" + gmlFixmeCounter;
	gmlFixmeCounter++;
	return newFixme;
    }

    private String originalPropertyName(PropertyInfo pi) {
	String originalPropertyName = pi.taggedValue(Flattener.TV_ORIGINAL_PROPERTY_NAME);
	if (StringUtils.isBlank(originalPropertyName)) {
	    originalPropertyName = pi.name();
	}
	return originalPropertyName;
    }

    private String originalSchemaName(PropertyInfo pi) {
	String originalSchemaName = pi.taggedValue(Flattener.TV_ORIGINAL_SCHEMA_NAME);
	if (StringUtils.isBlank(originalSchemaName)) {
	    originalSchemaName = pi.model().schemaPackage(pi.inClass()).name();
	}
	return originalSchemaName;
    }

    private String originalInClassName(PropertyInfo pi) {
	String originalInClassName = pi.taggedValue(Flattener.TV_ORIGINAL_INCLASS_NAME);
	if (StringUtils.isBlank(originalInClassName)) {
	    originalInClassName = pi.inClass().name();
	}
	return originalInClassName;
    }

    private boolean gmlRenameRequired(PropertyInfo pi, String xmlNamespaceOfRootCollection) {

	String originalInClassName = originalInClassName(pi);
	String originalSchemaName = originalSchemaName(pi);
	String originalPropertyName = originalPropertyName(pi);

	Optional<String> xmlNsOpt = xmlEncodingInfos.getXmlNamespace(originalSchemaName,
		originalInClassName + "::" + originalPropertyName);

	boolean renameRequired = false;

	if (xmlNsOpt.isPresent()) {
	    if (!xmlNsOpt.get().equals(xmlNamespaceOfRootCollection)) {
		renameRequired = true;
	    }
	} else {

	    PackageInfo piSchema = model.schemaPackage(pi.inClass());

	    if (StringUtils.isNotBlank(piSchema.targetNamespace())
		    && !piSchema.targetNamespace().equals(xmlNamespaceOfRootCollection)) {
		renameRequired = true;
	    } else if (!originalSchemaName.equals(piSchema.name())) {
		/*
		 * since different schemas must have different target namespaces, the XML
		 * namespace for the property changes
		 */
		renameRequired = true;
	    }
	}

	if (!originalPropertyName.equals(pi.name())) {
	    renameRequired = true;
	}

	Optional<String> xmlNameOpt = xmlEncodingInfos.getXmlName(originalSchemaName, originalInClassName,
		originalPropertyName);
	if (xmlNameOpt.isPresent() && !xmlNameOpt.get().equals(originalPropertyName)) {
	    renameRequired = true;
	}

	return renameRequired;
    }

    private String sourcePathLinkLevel(PropertyInfo pi) {

	if (pi.cardinality().maxOccurs == 1) {
	    // normal databaseColumnName-mechanic is fine on link level
	    // also for the case of a reflexive property
	    return databaseColumnName(pi);
	} else {
	    // return name of PK column
	    return primaryKeyColumn;
	}
    }

    private void addPropertyTransformationToBuildingBlockOfCollectionInServiceConfiguration(ClassInfo topLevelClass,
	    String propertyPath, ImmutablePropertyTransformation trf,
	    Map<ClassInfo, SortedMap<String, List<PropertyTransformation>>> propertyTransformationsForBuildingBlockOfServiceConfigCollectionsByTopLevelClass) {

	SortedMap<String, List<PropertyTransformation>> serviceConfigTrfsByPropPath;
	if (propertyTransformationsForBuildingBlockOfServiceConfigCollectionsByTopLevelClass
		.containsKey(topLevelClass)) {
	    serviceConfigTrfsByPropPath = propertyTransformationsForBuildingBlockOfServiceConfigCollectionsByTopLevelClass
		    .get(topLevelClass);
	} else {
	    serviceConfigTrfsByPropPath = new TreeMap<>();
	    propertyTransformationsForBuildingBlockOfServiceConfigCollectionsByTopLevelClass.put(topLevelClass,
		    serviceConfigTrfsByPropPath);
	}

	if (serviceConfigTrfsByPropPath.containsKey(propertyPath)) {
	    serviceConfigTrfsByPropPath.get(propertyPath).add(trf);
	} else {
	    List<PropertyTransformation> propTransforms = new ArrayList<>();
	    propTransforms.add(trf);
	    serviceConfigTrfsByPropPath.put(propertyPath, propTransforms);
	}
    }

    private Iterable<String> enumValues(ClassInfo enumeration) {

	List<String> res = new ArrayList<>();

	for (PropertyInfo pi : enumeration.properties().values()) {
	    if (StringUtils.isNotBlank(pi.initialValue())) {
		res.add(pi.initialValue().trim());
	    } else {
		res.add(pi.name());
	    }
	}

	return res;
    }

    private boolean unsupportedCategoryOfValue(PropertyInfo pi) {

	return pi.categoryOfValue() == Options.BASICTYPE || pi.categoryOfValue() == Options.MIXIN
		|| pi.categoryOfValue() == Options.UNKNOWN;
    }

    private boolean isMappedToOrImplementedAsLink(PropertyInfo pi) {

	if (valueTypeIsMapped(pi)) {
	    return isMappedToLink(pi);
	} else {
	    return isTypeWithIdentityValueType(pi);
	}
    }

    private Optional<String> sourcePathPropertyLevel(PropertyInfo pi) {

	String typeName = pi.typeInfo().name;
	String typeId = pi.typeInfo().id;
	String encodingRule = pi.encodingRule(Ldproxy2Constants.PLATFORM);

	ProcessMapEntry pme = mapEntryParamInfos.getMapEntry(typeName, encodingRule);

	if (pme != null && !ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme, typeId)) {

	    if (pme.hasTargetType()) {

		if ("GEOMETRY".equalsIgnoreCase(pme.getTargetType())) {

		    // the target only allows and thus assumes max mult = 1
		    return Optional.of(databaseColumnName(pi));

		} else if ("LINK".equalsIgnoreCase(pme.getTargetType())) {

		    if (pi.cardinality().maxOccurs == 1) {
			return Optional.empty();
		    } else {
			return Optional.of("[" + primaryKeyColumn(pi.inClass()) + "="
				+ databaseTableName(pi.inClass(), true) + "]" + associativeTableName(pi));
		    }

		} else {

		    // value type is a simple ldproxy type
		    if (pi.cardinality().maxOccurs == 1) {

			return Optional.of(databaseColumnName(pi));

		    } else {

			String sortKeyAddition = "{sortKey=" + databaseTableName(pi.inClass(), true) + "}";
			if (pi.matches(Ldproxy2Constants.RULE_ALL_ASSOCIATIVETABLES_WITH_SEPARATE_PK_FIELD)) {
			    sortKeyAddition = "";
			}
			return Optional.of(
				"[" + primaryKeyColumn(pi.inClass()) + "=" + databaseTableName(pi.inClass(), true) + "]"
					+ associativeTableName(pi) + sortKeyAddition + "/" + databaseColumnName(pi));
		    }
		}

	    } else {
		// is checked via target configuration validator (which can be switched off)
		result.addError(this, 118, typeName);
		return Optional.of("FIXME");
	    }
	}

	ClassInfo typeCi = pi.typeClass();

	if (typeCi == null) {

	    MessageContext mc = result.addError(this, 118, typeName);
	    if (mc != null) {
		mc.addDetail(this, 1, pi.fullNameInSchema());
	    }
	    return Optional.of("FIXME");

	} else {

	    if (typeCi.category() == Options.ENUMERATION || typeCi.category() == Options.CODELIST) {

		if (pi.cardinality().maxOccurs == 1) {

		    /*
		     * Note: Addition of code list foreign key suffix is handled in method
		     * databaseColumnName(..).
		     */
		    return Optional.of(databaseColumnName(pi));

		} else {

		    String sortKeyAddition = "{sortKey=" + databaseTableName(pi.inClass(), true) + "}";
		    if (pi.matches(Ldproxy2Constants.RULE_ALL_ASSOCIATIVETABLES_WITH_SEPARATE_PK_FIELD)) {
			sortKeyAddition = "";
		    }

		    if (typeCi.matches(Ldproxy2Constants.RULE_CLS_CODELIST_BY_TABLE)) {

			return Optional.of("[" + primaryKeyColumn(pi.inClass()) + "="
				+ databaseTableName(pi.inClass(), true) + "]" + associativeTableName(pi)
				+ sortKeyAddition + "/" + databaseTableName(typeCi, true));

		    } else {

			return Optional.of(
				"[" + primaryKeyColumn(pi.inClass()) + "=" + databaseTableName(pi.inClass(), true) + "]"
					+ associativeTableName(pi) + sortKeyAddition + "/" + databaseColumnName(pi));
		    }
		}

	    } else if (typeCi.category() == Options.DATATYPE) {

		if (typeCi.matches(Ldproxy2Constants.RULE_CLS_DATATYPES_ONETOMANY_SEVERAL_TABLES)) {

		    if (pi.cardinality().maxOccurs == 1) {

			return Optional.of("[" + databaseColumnName(pi) + "=" + primaryKeyColumn(typeCi) + "]"
				+ databaseTableName(typeCi, false));
		    } else {

			return Optional.of("[" + primaryKeyColumn(pi.inClass()) + "="
				+ databaseTableName(pi.inClass(), true) + "]" + associativeTableName(pi));
		    }

		} else {

		    if (pi.cardinality().maxOccurs == 1) {

			return Optional.of("[" + databaseColumnName(pi) + "=" + primaryKeyColumn(typeCi) + "]"
				+ databaseTableName(typeCi, false));
		    } else {

			return Optional
				.of("[" + primaryKeyColumn(pi.inClass()) + "=" + databaseTableName(pi.inClass(), true)
					+ "]" + associativeTableName(pi) + "/[" + databaseTableName(typeCi, true) + "="
					+ primaryKeyColumn(typeCi) + "]" + databaseTableName(typeCi, false));
		    }
		}

	    } else {

		boolean reflexive = pi.inClass().id().equals(pi.typeInfo().id);

		if (pi.reverseProperty() != null && pi.reverseProperty().isNavigable()) {

		    // bi-directional association
		    if (pi.cardinality().maxOccurs > 1 && pi.reverseProperty().cardinality().maxOccurs > 1) {

			// n:m

			if (reflexive) {

			    return Optional.of(
				    "[" + primaryKeyColumn(pi.inClass()) + "=" + databaseTableName(pi.inClass(), false)
					    + "_" + databaseColumnNameReflexiveProperty(pi.reverseProperty(), true)
					    + "]" + associativeTableName(pi) + "/[" + databaseTableName(typeCi, false)
					    + "_" + databaseColumnNameReflexiveProperty(pi, true) + "="
					    + primaryKeyColumn(pi.inClass()) + "]" + databaseTableName(typeCi, false));

			} else {

			    return Optional.of(
				    "[" + primaryKeyColumn(pi.inClass()) + "=" + databaseTableName(pi.inClass(), true)
					    + "]" + associativeTableName(pi) + "/[" + databaseTableName(typeCi, true)
					    + "=" + primaryKeyColumn(typeCi) + "]" + databaseTableName(typeCi, false));
			}

		    } else if (pi.cardinality().maxOccurs > 1) {

			// n:1

			if (reflexive) {

			    // no need for a table join in this case
			    // case p2 from ppt image (n:1 for bi-directional reflexive association)
			    return Optional.of("[" + primaryKeyColumn(pi.inClass()) + "="
				    + databaseColumnNameReflexiveProperty(pi.reverseProperty(), false) + "]"
				    + databaseTableName(typeCi, false));

			} else {

			    // case pB from ppt image (n:1 for bi-directional association)
			    return Optional.of("[" + primaryKeyColumn(pi.inClass()) + "="
				    + databaseColumnName(pi.reverseProperty()) + "]"
				    + databaseTableName(typeCi, false));
			}

		    } else if (pi.reverseProperty().cardinality().maxOccurs > 1) {

			// n:1

			if (reflexive) {

			    // no need for a table join in this case
			    // case p1 from ppt image (n:1 for bi-directional reflexive association)
			    return Optional.of(databaseColumnNameReflexiveProperty(pi, false));

			} else {

			    // case pA from ppt image (n:1 for bi-directional association)
			    return Optional.of("[" + databaseColumnName(pi) + "=" + primaryKeyColumn(typeCi) + "]"
				    + databaseTableName(typeCi, false));
			}

		    } else {

			// 1:1

			if (reflexive) {

			    // no need for a table join in this case
			    return Optional.of(databaseColumnNameReflexiveProperty(pi, false));

			} else {

			    // max mult = 1 on both ends
			    return Optional.of("[" + databaseColumnName(pi) + "=" + primaryKeyColumn(typeCi) + "]"
				    + databaseTableName(typeCi, false));
			}
		    }

		} else {

		    // attribute or uni-directional association
		    if (pi.cardinality().maxOccurs == 1) {

			// n:1

			if (reflexive) {

			    // no need for a table join in this case
			    return Optional.of(databaseColumnNameReflexiveProperty(pi, false));

			} else {

			    return Optional.of("[" + databaseColumnName(pi) + "=" + primaryKeyColumn(typeCi) + "]"
				    + databaseTableName(typeCi, false));
			}

		    } else {

			// n:m

			if (reflexive) {

			    return Optional.of(
				    "[" + primaryKeyColumn(pi.inClass()) + "=" + databaseTableName(pi.inClass(), false)
					    + "_" + databaseColumnNameReflexiveProperty(pi.reverseProperty(), true)
					    + "]" + associativeTableName(pi) + "/[" + databaseTableName(typeCi, false)
					    + "_" + databaseColumnNameReflexiveProperty(pi, true) + "="
					    + primaryKeyColumn(pi.inClass()) + "]" + databaseTableName(typeCi, false));

			} else {

			    return Optional.of(
				    "[" + primaryKeyColumn(pi.inClass()) + "=" + databaseTableName(pi.inClass(), true)
					    + "]" + associativeTableName(pi) + "/[" + databaseTableName(typeCi, true)
					    + "=" + primaryKeyColumn(typeCi) + "]" + databaseTableName(typeCi, false));
			}
		    }
		}

	    }
	}
    }

    private String primaryKeyColumn(ClassInfo ci) {

	if (ci.matches(Ldproxy2Constants.RULE_CLS_IDENTIFIER_STEREOTYPE)) {
	    for (PropertyInfo pi : ci.properties().values()) {
		if (pi.stereotype("identifier")) {
		    return databaseColumnName(pi);
		}
	    }
	}

	return primaryKeyColumn;
    }

    private String associativeTableName(PropertyInfo pi) {

	if (StringUtils.isNotBlank(pi.taggedValue("associativeTable"))) {
	    return pi.taggedValue("associativeTable");
	} else if (pi.association() != null
		&& StringUtils.isNotBlank(pi.association().taggedValue("associativeTable"))) {
	    return pi.association().taggedValue("associativeTable");
	}

	// tag associativeTable not set or without value -> proceed

	String tableNamePi = determineTableName(pi);

	if (pi.isAttribute() || pi.reverseProperty() == null || !pi.reverseProperty().isNavigable()) {

	    return tableNamePi;

	} else {

	    // both pi and its reverseProperty are navigable

	    // choose name based on alphabetical order
	    // take into account the case of a reflexive association
	    String tableNameRevPi = determineTableName(pi.reverseProperty());

	    if (tableNamePi.compareTo(tableNameRevPi) <= 0) {
		return tableNamePi;
	    } else {
		return tableNameRevPi;
	    }
	}
    }

    private String determineTableName(PropertyInfo pi) {

	String tableName = pi.inClass().name();
	String propertyName = pi.name();
	String res = tableName + "_" + propertyName;
	return res;
    }

    private String constantValue(PropertyInfo pi) {

	String valueTypeName = pi.typeInfo().name;

	ProcessMapEntry pme = mapEntryParamInfos.getMapEntry(valueTypeName,
		pi.encodingRule(Ldproxy2Constants.PLATFORM));

	if (pme != null && !ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme, pi.typeInfo().id)) {

	    if (valueTypeName.equalsIgnoreCase("Boolean")) {

		if ("true".equalsIgnoreCase(pi.initialValue().trim())) {
		    return StringUtils.defaultIfBlank(mapEntryParamInfos.getCharacteristic(valueTypeName,
			    pi.encodingRule(Ldproxy2Constants.PLATFORM),
			    Ldproxy2Constants.ME_PARAM_INITIAL_VALUE_ENCODING,
			    Ldproxy2Constants.ME_PARAM_INITIAL_VALUE_ENCODING_CHARACT_FALSE), "true");
		} else if ("false".equalsIgnoreCase(pi.initialValue().trim())) {
		    return StringUtils.defaultIfBlank(mapEntryParamInfos.getCharacteristic(valueTypeName,
			    pi.encodingRule(Ldproxy2Constants.PLATFORM),
			    Ldproxy2Constants.ME_PARAM_INITIAL_VALUE_ENCODING,
			    Ldproxy2Constants.ME_PARAM_INITIAL_VALUE_ENCODING_CHARACT_FALSE), "false");
		}
	    }
	}

	return pi.initialValue();
    }

    private String urlTemplateForValueType(PropertyInfo pi) {

	String urlTemplate = null;

	String valueTypeName = pi.typeInfo().name;

	ProcessMapEntry pme = mapEntryParamInfos.getMapEntry(valueTypeName,
		pi.encodingRule(Ldproxy2Constants.PLATFORM));

	if (pme != null && !ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme, pi.typeInfo().id)
		&& "LINK".equalsIgnoreCase(pme.getTargetType())) {

	    urlTemplate = mapEntryParamInfos.getCharacteristic(valueTypeName,
		    pi.encodingRule(Ldproxy2Constants.PLATFORM), Ldproxy2Constants.ME_PARAM_LINK_INFOS,
		    Ldproxy2Constants.ME_PARAM_LINK_INFOS_CHARACT_URL_TEMPLATE);

	    urlTemplate = urlTemplate.replaceAll("\\(value\\)", "{{value}}").replaceAll("\\(serviceUrl\\)",
		    "{{serviceUrl}}");
	}

	if (StringUtils.isBlank(urlTemplate)) {
	    urlTemplate = "{{serviceUrl}}/collections/" + valueTypeName.toLowerCase(Locale.ENGLISH)
		    + "/items/{{value}}";
	}

	return urlTemplate;
    }

    private String propertyPath(List<PropertyInfo> propertyList) {
	return propertyList.stream().map(pi -> pi.name()).collect(Collectors.joining("."));
    }

    private boolean isMappedToLink(PropertyInfo pi) {

	return isMappedToLink(pi.typeInfo().name, pi.typeInfo().id, pi.encodingRule(Ldproxy2Constants.PLATFORM));
    }

    private boolean isMappedToLink(String typeName, String typeId, String encodingRule) {

	ProcessMapEntry pme = mapEntryParamInfos.getMapEntry(typeName, encodingRule);

	if (pme != null && !ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme, typeId) && pme.hasTargetType()
		&& "LINK".equalsIgnoreCase(pme.getTargetType())) {
	    return true;
	} else {
	    return false;
	}
    }

    private boolean valueTypeIsMapped(PropertyInfo pi) {

	return valueTypeIsMapped(pi.typeInfo().name, pi.typeInfo().id, pi.encodingRule(Ldproxy2Constants.PLATFORM));
    }

    private boolean valueTypeIsMapped(String typeName, String typeId, String encodingRule) {

	ProcessMapEntry pme = mapEntryParamInfos.getMapEntry(typeName, encodingRule);

	if (pme != null && !ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme, typeId)) {
	    return true;
	} else {
	    return false;
	}
    }

    private boolean isLdproxyGeometryType(Type t) {
	return t == Type.GEOMETRY;
    }

    private boolean isLdproxySimpleType(Type t) {

	switch (t) {
	case STRING:
	case BOOLEAN:
	case DATE:
	case DATETIME:
	case INTEGER:
	case FLOAT:
	    return true;

	default:
	    return false;
	}
    }

    private boolean isEnumerationOrCodelistValueType(PropertyInfo pi) {
	return pi.categoryOfValue() == Options.ENUMERATION || pi.categoryOfValue() == Options.CODELIST;
    }

    private boolean isTypeWithIdentityValueType(PropertyInfo pi) {
	return pi.categoryOfValue() == Options.FEATURE || pi.categoryOfValue() == Options.OBJECT;
    }

    private Type ldproxyType(PropertyInfo pi) {

	return ldproxyType(pi.typeInfo().name, pi.typeInfo().id, pi.encodingRule(Ldproxy2Constants.PLATFORM));
    }

    private Type ldproxyType(String typeName, String typeId, String encodingRule) {

	Type resType = Type.STRING;

	ProcessMapEntry pme = mapEntryParamInfos.getMapEntry(typeName, encodingRule);

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
		valueType = model.classById(typeId);
	    }
	    if (valueType == null && StringUtils.isNotBlank(typeName)) {
		valueType = model.classByName(typeName);
	    }

	    if (valueType == null) {

		// The value type was not found in the model
		result.addError(this, 113, typeName);

	    } else if (!isEncoded(valueType)) {

		result.addError(this, 114, typeName);

	    } else {

		if (valueType.category() == Options.ENUMERATION || valueType.category() == Options.CODELIST) {

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
	}

	return resType;
    }

    private Optional<SimpleFeatureGeometry> geometryType(PropertyInfo pi) {

	String typeName = pi.typeInfo().name;
	String typeId = pi.typeInfo().id;
	String encodingRule = pi.encodingRule(Ldproxy2Constants.PLATFORM);

	SimpleFeatureGeometry res = SimpleFeatureGeometry.ANY;

	ProcessMapEntry pme = mapEntryParamInfos.getMapEntry(typeName, encodingRule);

	if (pme != null && !ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme, typeId) && pme.hasTargetType()
		&& "GEOMETRY".equalsIgnoreCase(pme.getTargetType())
		&& mapEntryParamInfos.hasCharacteristic(typeName, encodingRule,
			Ldproxy2Constants.ME_PARAM_GEOMETRY_INFOS,
			Ldproxy2Constants.ME_PARAM_GEOMETRY_INFOS_CHARACT_GEOMETRY_TYPE)) {

	    String t = mapEntryParamInfos.getCharacteristic(typeName, encodingRule,
		    Ldproxy2Constants.ME_PARAM_GEOMETRY_INFOS,
		    Ldproxy2Constants.ME_PARAM_GEOMETRY_INFOS_CHARACT_GEOMETRY_TYPE);

	    res = SimpleFeatureGeometry.valueOf(t.toUpperCase(Locale.ENGLISH));

	} else {
	    // is checked via target configuration validator (which can be switched off)
	    MessageContext mc = result.addError(this, 121, pi.name(), pi.typeInfo().name);
	    if (mc != null) {
		mc.addDetail(this, 1, pi.fullNameInSchema());
	    }
	}

	return Optional.of(res);
    }

    private boolean isTrueIgnoringCase(String s) {
	return StringUtils.isNotBlank(s) && "true".equalsIgnoreCase(s.trim());
    }

    private Optional<String> featureTitleTemplate(ClassInfo ci) {

	String tv = ci.taggedValue("ldpFeatureTitleTemplate");
	if (StringUtils.isBlank(tv)) {
	    return Optional.empty();
	} else {
	    return Optional.of(tv.trim());
	}
    }

    private ImmutableCodelistData createCodelistEntity(ClassInfo ci) {

	String id = codelistId(ci);

	Optional<String> labelOpt = label(ci);
	String label;
	if (labelOpt.isEmpty()) {
	    MessageContext mc = result.addInfo(this, 102, ci.name());
	    if (mc != null) {
		mc.addDetail(this, 1, ci.fullNameInSchema());
	    }
	    label = ci.name();
	} else {
	    label = labelOpt.get();
	}

	ImmutableCodelistData icd = cfg.builder().entity().codelist().id(id).label(label)
		.sourceType(ImportType.TEMPLATES).build();

	if (!ci.properties().isEmpty()) {

	    SortedMap<String, String> entries = new TreeMap<>();

	    for (PropertyInfo pi : ci.properties().values()) {

		String code = null;
		String targetValue = null;

		if (ci.matches(Ldproxy2Constants.RULE_CLS_CODELIST_DIRECT)) {

		    targetValue = pi.name();

		    if (StringUtils.isBlank(pi.initialValue())) {
			MessageContext mc = result.addWarning(this, 100, ci.name(), pi.name());
			if (mc != null) {
			    mc.addDetail(this, 1, pi.fullNameInSchema());
			}
			code = pi.name();
		    } else {
			code = pi.initialValue();
		    }
		} else if (ci.matches(Ldproxy2Constants.RULE_CLS_CODELIST_TARGETBYTV)) {

		    if (StringUtils.isBlank(pi.initialValue())) {
			code = pi.name();
		    } else {
			code = pi.initialValue();
		    }

		    if (StringUtils.isBlank(pi.taggedValue(codeTargetTagName))) {
			MessageContext mc = result.addWarning(this, 101, ci.name(), pi.name(), codeTargetTagName);
			if (mc != null) {
			    mc.addDetail(this, 1, pi.fullNameInSchema());
			}
			targetValue = pi.name();
		    } else {
			targetValue = pi.taggedValue(codeTargetTagName).trim();
		    }
		} else {
		    code = pi.name();
		    targetValue = pi.name();
		}

		entries.put(code, targetValue);
	    }

	    icd = icd.withEntries(entries);
	}

	if (StringUtils.isNotBlank(ci.taggedValue("ldpFallbackValue"))) {
	    icd = icd.withFallback(ci.taggedValue("ldpFallbackValue").trim());
	}

	if (isUnitTest) {
	    icd = icd.withCreatedAt(Ldproxy2Constants.UNITTEST_UNIX_TIME)
		    .withLastModified(Ldproxy2Constants.UNITTEST_UNIX_TIME);
	}

	return icd;
    }

    private String databaseTableName(ClassInfo ci,
	    boolean isAssociativeTableContextAndNotReflexiveRelationshipContext) {

	String result = ci.name();

	if (isAssociativeTableContextAndNotReflexiveRelationshipContext) {
	    result = result + associativeTableColumnSuffix;
	}

	result = result.toLowerCase(Locale.ENGLISH);

	result = StringUtils.substring(result, 0, maxNameLength);

	return result;
    }

    private String databaseColumnNameReflexiveProperty(PropertyInfo pi, boolean inAssociativeTable) {

	String suffix = "";

	if (valueTypeIsTypeWithIdentity(pi)) {

	    if (inAssociativeTable) {
		suffix = suffix + associativeTableColumnSuffix;
	    } else {
		suffix = suffix + foreignKeyColumnSuffix;
	    }

	} else if (pi.categoryOfValue() == Options.DATATYPE) {
	    suffix = suffix + foreignKeyColumnSuffixDatatype;
	}

	return databaseColumnName(pi, suffix);
    }

    private String databaseColumnName(PropertyInfo pi) {

	String suffix = "";

	Type t = ldproxyType(pi);

	if (!(isLdproxySimpleType(t) || isLdproxyGeometryType(t))) {

	    if (valueTypeIsTypeWithIdentity(pi)) {
		suffix = suffix + foreignKeyColumnSuffix;
	    } else if (pi.categoryOfValue() == Options.DATATYPE) {
		suffix = suffix + foreignKeyColumnSuffixDatatype;
	    }

	} else if (pi.categoryOfValue() == Options.CODELIST
		&& model.classByIdOrName(pi.typeInfo()).matches(Ldproxy2Constants.RULE_CLS_CODELIST_BY_TABLE)) {

	    // Support SqlDdl target parameter foreignKeyColumnSuffixCodelist
	    suffix = suffix + foreignKeyColumnSuffixCodelist;
	}

	return databaseColumnName(pi, suffix);
    }

    private String databaseColumnName(PropertyInfo pi, String suffix) {

	String result = pi.name();

	result = result + suffix;

	result = result.toLowerCase(Locale.ENGLISH);

	result = StringUtils.substring(result, 0, maxNameLength);

	return result;
    }

    private boolean valueTypeIsTypeWithIdentity(PropertyInfo pi) {
	return pi.categoryOfValue() == Options.FEATURE || pi.categoryOfValue() == Options.OBJECT;
    }

    private String codelistId(ClassInfo ci) {
	return ci.name().replaceAll("\\W", "_");
    }

    private Optional<String> label(Info i) {

	if (i != null && StringUtils.isNotBlank(labelTemplate) && i.matches(Ldproxy2Constants.RULE_ALL_DOCUMENTATION)) {

	    String label = i.derivedDocumentation(labelTemplate, descriptorNoValue);

	    return StringUtils.isBlank(label) ? Optional.empty() : Optional.of(label);

	} else {
	    return Optional.empty();
	}
    }

    private Optional<String> description(Info i) {

	if (i != null && StringUtils.isNotBlank(descriptionTemplate)
		&& i.matches(Ldproxy2Constants.RULE_ALL_DOCUMENTATION)) {

	    String description = i.derivedDocumentation(descriptionTemplate, descriptorNoValue);

	    return StringUtils.isBlank(description) ? Optional.empty() : Optional.of(description);

	} else {
	    return Optional.empty();
	}
    }

    @Override
    public void reset() {

	model = null;

	initialised = false;
	diagnosticsOnly = false;
	numberOfEncodedSchemas = 0;

	isUnitTest = false;

	dbSchemaNames = new TreeSet<String>();

//	documentationTemplate = null;
//	documentationNoValue = null;

	associativeTableColumnSuffix = null; // default: value of primaryKeyColumn parameter
	cfgTemplatePath = "https://shapechange.net/resources/templates/ldproxy2/cfgTemplate.yml";
	codeTargetTagName = Ldproxy2Constants.DEFAULT_CODE_TARGET_TAG_NAME_VALUE;
	dateFormat = null; // no default value
	dateTimeFormat = null; // no default value
	descriptionTemplate = "[[definition]]";
	descriptorNoValue = "";
	forceAxisOrder = Force.NONE;
	foreignKeyColumnSuffix = "";
	foreignKeyColumnSuffixDatatype = "";
	foreignKeyColumnSuffixCodelist = "";
	labelTemplate = "[[alias]]";
	maxNameLength = 63;
	nativeTimeZone = ZoneId.systemDefault();
	objectIdentifierName = "oid";
	primaryKeyColumn = "id";
	serviceDescription = "FIXME";
	serviceLabel = "FIXME";
	srid = 4326;
	serviceConfigTemplatePathString = null;

	outputDirectory = null;
	dataDirectoryFile = null;
	mainId = null;
	mainAppSchema = null;

	enableGmlOutput = false;
	uomTvName = null;
	gmlIdPrefix = null;
	gmlSfLevel = null;
	gmlFeatureCollectionElementName = null;
	gmlFeatureMemberElementName = null;
	gmlSupportsStandardResponseParameters = false;
	xmlEncodingInfos = new XmlEncodingInfos();

	mapEntryParamInfos = null;

	typesWithIdentity = new ArrayList<>();
	codelistsAndEnumerations = new TreeSet<>();

	cfg = null;
    }

    @Override
    public void registerRulesAndRequirements(RuleRegistry r) {

	r.addRule(Ldproxy2Constants.RULE_ALL_ASSOCIATIVETABLES_WITH_SEPARATE_PK_FIELD);
	r.addRule(Ldproxy2Constants.RULE_ALL_DOCUMENTATION);
	r.addRule(Ldproxy2Constants.RULE_ALL_NOT_ENCODED);
	r.addRule(Ldproxy2Constants.RULE_ALL_SCHEMAS);
	r.addRule(Ldproxy2Constants.RULE_CLS_CODELIST_DIRECT);
	r.addRule(Ldproxy2Constants.RULE_CLS_CODELIST_TARGETBYTV);
	r.addRule(Ldproxy2Constants.RULE_CLS_CODELIST_BY_TABLE);
	r.addRule(Ldproxy2Constants.RULE_CLS_DATATYPES_ONETOMANY_SEVERAL_TABLES);
	r.addRule(Ldproxy2Constants.RULE_CLS_ENUMERATION_ENUM_CONSTRAINT);
	r.addRule(Ldproxy2Constants.RULE_CLS_IDENTIFIER_STEREOTYPE);
	r.addRule(Ldproxy2Constants.RULE_PROP_READONLY);
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

	switch (mnr) {

	case 0:
	    return "Context: class '$1$'";
	case 1:
	    return "Context: property '$1$'";

	case 3:
	    return "Context: class Ldproxy2Target";
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

	case 15:
	    return "No map entries provided via the configuration.";
	case 16:
	    return "Value '$1$' of configuration parameter $2$ does not match the regular expression: $3$. The parameter will be ignored.";
	case 17:
	    return "Type '$1$' is of a category not enabled for conversion, meaning that no ldproxy configuration items will be created to represent it.";
	case 18:
	    return "Schema '$1$' is not encoded. Thus class '$2$' (which belongs to that schema) is not encoded either.";
	case 19:
	    return "";
	case 20:
	    return "Type '$1$' is abstract. Conversion of abstract types is not supported by this target. The type will be ignored. Apply inheritance flattening (a model transformation) in order to handle abstract supertypes.";
	case 21:
	    return "Value '$1$' of configuration parameter '$2$' does not result in a valid path. Exception message is: $3$";
	case 22:
	    return "Type '$1$' has been mapped to '$2$', as defined by the configuration.";
	case 23:
	    return "";

	case 100:
	    return Ldproxy2Constants.RULE_CLS_CODELIST_DIRECT
		    + " applies to codelist/enumeration '$1$'. However, code/enum '$2$' has no initial value. The code/enum cannot be encoded as defined by the conversion rule. Using the property name as fallback for the {code} in the enum/code mapping.";
	case 101:
	    return Ldproxy2Constants.RULE_CLS_CODELIST_TARGETBYTV
		    + " applies to codelist/enumeration '$1$'. However, code/enum '$2$' does not have a non-blank value for tag '$3$'. The code/enum cannot be encoded as defined by the conversion rule. Using the property name as fallback for the {target_value} in the enum/code mapping";
	case 102:
	    return "Could not compute a label for codelist/enumeration '$1$'. A label is required for the ldproxy encoding as a codelist. Check the label template in the target configuration as well as the codelist/enumeration model, to ensure that a label can be created. Using the type name as label.";
	case 103:
	    return "Type '$1$' has one or more supertypes. Conversion of inheritance relationships is not supported by this target. The relationship will be ignored. Apply inheritance flattening (a model transformation) in order to handle inheritance relationships.";
	case 104:
	    return "Type '$1$' has <<identifier>> property '$2$', with max multiplicity greater 1. Encoding will assume a max multiplicity of exactly 1.";
	case 105:
	    return "Property '$3$' of type '$1$' is marked (via tagged value 'defaultGeometry') as default geometry property. So is property '$2$'. Multiple default geometry properties per type are not allowed. None will be marked as default geometry.";
	case 106:
	    return "Property '$2$' of type '$1$' is marked as default instant as well as as default interval (start and/or end) via according tagged values. That is an invalid combination. The property is not recognized as defining a primary temporal property.";
	case 107:
	    return "Property '$3$' of type '$1$' is marked (via stereotype 'identifier') as identifier property. So is property '$2$'. Multiple identifier properties per type are not allowed. None will be used as identifier (because no informed decision can be made).";
	case 108:
	    return "Property '$3$' of type '$1$' is marked (via tagged value 'defaultInstant') as default (temporal) instant property. So is property '$2$'. Multiple default instant properties per type are not allowed. None will be marked as default instant.";
	case 109:
	    return "Property '$2$' of type '$1$' is marked as default interval start as well as default interval end via according tagged values. That is an invalid combination. The property is not recognized as defining a primary temporal property.";
	case 110:
	    return "Property '$3$' of type '$1$' is marked (via tagged value 'defaultIntervalStart') as default (temporal) interval start property. So is property '$2$'. Multiple default interval start properties per type are not allowed. None will be marked as default interval start.";
	case 111:
	    return "Property '$3$' of type '$1$' is marked (via tagged value 'defaultIntervalEnd') as default (temporal) interval end property. So is property '$2$'. Multiple default interval end properties per type are not allowed. None will be marked as default interval end.";
	case 112:
	    return "??No target type is defined in map entry for type '$1$'. Assuming ldproxy type 'STRING'.";
	case 113:
	    return "??Ldproxy type definition for type '$1$' could not be identified. No map entry is defined for the type, and the type was not found in the model. Assuming ldproxy type 'STRING'.";
	case 114:
	    return "??Type '$1$' is marked to not be encoded. Could not identify an ldproxy type for it. Assuming ldproxy type 'STRING'.";
	case 115:
	    return "??Type '$1$' is not part of the schemas selected for processing. Could not identify an ldproxy type for it. Assuming ldproxy type 'STRING'.";
	case 116:
	    return "??Expected simple ldproxy type implementation of value type '$1$' of property '$2$' in class '$3$'. Found '$4$'. Assuming ldproxy type 'STRING'.";
	case 117:
	    return "??Circular dependency detected when encoding type '$1$'. The property path '$2$' would create a circle. The last property in that path will not be encoded.";
	case 118:
	    return "??No target type is defined in map entry for type '$1$'. Setting source path for properties with this type as value type to 'FIXME'.";
	case 119:
	    return "??Value type '$1$' of property '$2$' is neither mapped nor found in the model. Is there a typo in value type name? Has the type not been loaded (e.g. by excluding some package during model loading) or removed (e.g. through a transformation)? Setting source path for the property to 'FIXME'.";
	case 120:
	    return "??The value type '$1$' of property '$2$' (of class '$3$') is not mapped and of a category not supported by this target. The property will not be encoded. Either define a mapping for the value type or apply a model transform (e.g. flattening inheritance or flattening complex types [including unions]) to cope with this situation.";
	case 121:
	    return "??No geometry type defined via map entry for value type '$2$' of property '$1$'. Ensure that map entries are configured correctly. Proceeding with geometry type ANY.";
	case 122:
	    return "??Value of tag 'ldpRemove' on property '$1$' is invalid. Allowed values are: IN_COLLECTION, ALWAYS, NEVER (case is ignored when parsing the value). Found value '$2$'.";
	case 123:
	    return "Exception occurred while copying the template defined by target parameter '$1$' from '$2$' to '$3$'. Exception message is: '$3$'.";
	case 124:
	    return "??The value type '$1$' of property '$2$' (of class '$3$') is not mapped and either not present in the model, not correctly linked, or not encoded. The property will therefore not be encoded. Either define a mapping for the value type, or ensure that the value type is correctly linked to and that it actually is defined to be encoded by this target.";
	case 125:
	    return "Type '$1$' will be ignored, because a type with equal name (ignoring case) has already been encountered and marked for encoding by the target. The target does not support encoding of multiple types with equal name (ignoring case).";
	case 126:
	    return "The target configuration does not contain XML encoding infos.";
	case 127:
	    return "??Enumeration or code list '$1$', which is used as value type of at least one property that is encoded by the target, is either not encoded, not mapped, or not part of the schemas selected for processing. This is an issue UNLESS an ldproxy codelist with id '$2$' has already been or will be established for the desired deployment by other means (e.g. manually created).";
	case 128:
	    return "??Main application schema could not be determined (using parameter '"
		    + TargetUtil.PARAM_MAIN_APP_SCHEMA
		    + "' - if set - or by having only a single schema to process). Using '$1$'.";
	case 129:
	    return "??Could not find a schema with name '$1$' in the model, or no target namespace is defined for that schema. XML namespace information for that schema is not available. Using value '$2$' as namespace and nsabr for that schema name.";
	case 130:
	    return "??Could not find an abbreviation for XML namespace '$1$' (neither in XmlEncodingInfos, nor in the model). Using value '$2$'.";

	case 10001:
	    return "Generating ldproxy configuration items for application schema $1$.";
	case 10002:
	    return "Diagnostics-only mode. All output to files is suppressed.";
	default:
	    return "(" + Ldproxy2Target.class.getName() + ") Unknown message with number: " + mnr;
	}
    }
}
