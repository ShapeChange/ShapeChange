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
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.ldproxy.ogcapi.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableFeatureTypeConfigurationOgcApi;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiDataV2;
import de.ii.ldproxy.ogcapi.features.html.domain.ImmutableFeaturesHtmlConfiguration;
import de.ii.ldproxy.ogcapi.html.domain.ImmutableHtmlConfiguration;
import de.ii.xtraplatform.crs.domain.ImmutableEpsgCrs;
import de.ii.xtraplatform.codelists.domain.CodelistData.IMPORT_TYPE;
import de.ii.xtraplatform.codelists.domain.ImmutableCodelistData;
import de.ii.xtraplatform.crs.domain.EpsgCrs.Force;
import de.ii.xtraplatform.feature.provider.sql.domain.ConnectionInfoSql.Dialect;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableSchemaConstraints;
import de.ii.xtraplatform.features.domain.SchemaBase.Role;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableConnectionInfoSql;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableFeatureProviderSqlData;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableQueryGeneratorSettings;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableSqlPathDefaults;
import de.interactive_instruments.ShapeChange.MapEntryParamInfos;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.RuleRegistry;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.SingleTarget;
import de.interactive_instruments.ShapeChange.Target.TargetUtil;
import de.interactive_instruments.ShapeChange.Target.JSON.JsonSchemaConstants;
import de.interactive_instruments.ShapeChange.Target.JSON.JsonSchemaDocument;
import de.interactive_instruments.ShapeChange.Target.JSON.JsonSchemaTarget;
import de.interactive_instruments.ShapeChange.Target.JSON.JsonSchemaTypeInfo;
import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.JsonSchemaVersion;
import de.interactive_instruments.ShapeChange.Target.SQL.SqlDdl;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class Ldproxy2Target implements SingleTarget, MessageSource {

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

    protected static String associativeTableColumnSuffix = null; // default: value of primaryKeyColumn parameter
    protected static String cfgTemplatePath = null; // default TODO
    protected static String dateFormat = null; // no default value
    protected static String dateTimeFormat = null; // no default value
    protected static String descriptionTemplate = "[[definition]]";
    protected static String descriptorNoValue = "";
    protected static Force forceAxisOrder = Force.NONE;
    protected static String foreignKeyColumnSuffix = "";
    protected static String foreignKeyColumnSuffixDatatype = "";
    protected static String labelTemplate = "[[alias]]";
    protected static int maxNameLength = 63;
    protected static ZoneId nativeTimeZone = ZoneId.systemDefault();
    protected static String objectIdentifierName = "oid";
    protected static String primaryKeyColumn = "id";
    protected static String serviceApiTemplatePath = null; // default TODO
    protected static String serviceDescription = "FIXME";
    protected static String serviceLabel = "FIXME";
    protected static String serviceMetadataTemplatePath = null; // default TODO
    protected static int srid = 4326;

    protected static SortedSet<String> dbSchemaNames = new TreeSet<String>();

    protected static boolean isUnitTest = false;

    protected static String outputDirectory = null;
    protected static String mainId = null;

    /**
     * Contains information parsed from the 'param' attributes of each map entry
     * defined for this target.
     */
    protected static MapEntryParamInfos mapEntryParamInfos = null;

//    protected static SortedMap<PackageInfo, JsonSchemaDocument> jsDocsByPkg = new TreeMap<>();
//    protected static Map<ClassInfo, JsonSchemaDocument> jsDocsByCi = new HashMap<>();

    protected static List<ClassInfo> typesWithIdentity = new ArrayList<>();
    protected static List<ClassInfo> codelistsAndEnumerations = new ArrayList<>();

    protected static LdproxyCfg cfg = null;

    /* ------ */
    /*
     * Non-static fields
     */
    protected ShapeChangeResult result = null;
    protected Options options = null;

    private PackageInfo schema = null;
    private boolean schemaNotEncoded = false;

    private PackageInfo mainAppSchema = null;

    @Override
    public void initialise(PackageInfo pi, Model m, Options o, ShapeChangeResult r, boolean diagOnly)
	    throws ShapeChangeAbortException {

	schema = pi;
	model = m;
	options = o;
	result = r;
	mainAppSchema = TargetUtil.findMainSchemaForSingleTargets(model.selectedSchemas(), o, r);
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

	    if (mainAppSchema == null) {
		mainId = schema.name();
	    } else {
		mainId = mainAppSchema.name();
	    }
	    mainId = mainId.replaceAll("\\W", "_").toLowerCase(Locale.ENGLISH);

	    isUnitTest = options.parameterAsBoolean(this.getClass().getName(), "_unitTestOverride", false);

	    // change the default documentation template?
	    documentationTemplate = options.parameter(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_DOCUMENTATION_TEMPLATE);
	    documentationNoValue = options.parameter(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_DOCUMENTATION_NOVALUE);

	    cfgTemplatePath = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_CFG_TEMPLATE_PATH, "FIXME", false, true);

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

	    serviceApiTemplatePath = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_SERVICE_API_TEMPLATE_PATH, "FIXME", false, true);

	    serviceDescription = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_SERVICE_DESCRIPTION, "FIXME", false, true);

	    serviceLabel = options.parameterAsString(this.getClass().getName(), Ldproxy2Constants.PARAM_SERVICE_LABEL,
		    "FIXME", false, true);

	    serviceMetadataTemplatePath = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_SERVICE_METADATA_TEMPLATE_PATH, "FIXME", false, true);

	    srid = options.parameterAsInteger(this.getClass().getName(), Ldproxy2Constants.PARAM_SRID, 4326);

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

	    File configDirectoryFile = new File(outputDirectory, "data");
	    Path configDirectoryPath = configDirectoryFile.toPath();
	    cfg = new LdproxyCfg(configDirectoryPath);
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

	if (ci.category() == Options.DATATYPE) {

	    // ignore here - will be encoded as needed

	} else if (ci.category() == Options.OBJECT || ci.category() == Options.FEATURE) {

	    typesWithIdentity.add(ci);

	} else if (ci.category() == Options.ENUMERATION || ci.category() == Options.CODELIST) {

	    if (ci.matches(Ldproxy2Constants.RULE_CLS_CODELIST_DIRECT)
		    || ci.matches(Ldproxy2Constants.RULE_CLS_CODELIST_TARGETBYTV)) {
		codelistsAndEnumerations.add(ci);
	    } else {
		result.addInfo(this, 19, ci.name());
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

	// TODO create the actual ldproxy configuration objects

	if (!diagnosticsOnly) {

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

		Optional<String> featureTitleTemplate = computeFeatureTitleTemplate(ci);

		ImmutableFeaturesHtmlConfiguration fhtml = cfg.builder().ogcApiExtension().featuresHtml()
			.featureTitleTemplate(featureTitleTemplate).build();

		SortedMap<String, FeatureSchema> propertyDefs = computePropertyDefinitions(ci,
			new ArrayList<PropertyInfo>());

		ImmutableFeatureSchema typeDef = new ImmutableFeatureSchema.Builder().type(Type.OBJECT)
			.name(typeDefName).objectType(ci.name()).label(computeLabel(ci))
			.sourcePath(computeDatabaseTableName(ci, false)).description(computeDescription(ci))
			.propertyMap(propertyDefs).build();

		ImmutableFeatureTypeConfigurationOgcApi serviceCollDef = new ImmutableFeatureTypeConfigurationOgcApi.Builder()
			.id(typeDefName).label(typeDefName).addExtensions(fhtml).build();

		providerTypeDefinitions.put(typeDefName, typeDef);
		serviceCollectionDefinitions.put(typeDefName, serviceCollDef);
	    }

	    /*
	     * ================================
	     * 
	     * BUILD THE SERVICE CONFIGURATION
	     * 
	     * ================================
	     */

//	    ImmutableHtmlConfiguration html = cfg.builder().ogcApiExtension().html().footerText("TEST").build();

	    ImmutableOgcApiDataV2 serviceConfig = cfg.builder().entity().api().id(mainId).entityStorageVersion(2)
		    .label(serviceLabel).description(serviceDescription).serviceType("OGC_API")
		    .collections(serviceCollectionDefinitions).build();

	    // FIXME: Integrate metadata and api template

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

	    // FIXME: API must not write all time zone details - just its name

	    if (isUnitTest) {
		providerConfig = providerConfig.withCreatedAt(Ldproxy2Constants.UNITTEST_UNIX_TIME)
			.withLastModified(Ldproxy2Constants.UNITTEST_UNIX_TIME);
	    }

	    // TODO create the output directories and write the ldproxy configuration files
//	    File entitiesDir = new File(outputDirectory, "data/store/entities");
//	    if (!entitiesDir.exists()) {
//		entitiesDir.mkdirs();
//	    }
//	    File codelistsDir = new File(entitiesDir, "codelists");
//	    if (!codelistsDir.exists()) {
//		codelistsDir.mkdir();
//	    }
//	    File providersDir = new File(entitiesDir, "providers");
//	    if (!providersDir.exists()) {
//		providersDir.mkdir();
//	    }
//	    File servicesDir = new File(entitiesDir, "services");
//	    if (!servicesDir.exists()) {
//		servicesDir.mkdir();
//	    }

	    // write api
	    try {
		cfg.writeEntity(serviceConfig);
		cfg.writeEntity(providerConfig);
		for (ImmutableCodelistData icd : codelists) {
		    cfg.writeEntity(icd);
		}
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}

    }

    private SortedMap<String, FeatureSchema> computePropertyDefinitions(ClassInfo currentCi,
	    List<PropertyInfo> visitedPiList) {

	SortedMap<String, FeatureSchema> propertyDefs = new TreeMap<>();

	/*
	 * DETERMINE SPECIAL PROPERTIES - ONLY FOR TOP-LEVEL CLASS
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

	if (visitedPiList.isEmpty()) {

	    for (PropertyInfo pi : currentCi.properties().values()) {

		if (pi.matches(Ldproxy2Constants.RULE_ALL_NOT_ENCODED)) {
		    continue;
		}

		if (pi.stereotype("identifier")
			&& currentCi.matches(Ldproxy2Constants.RULE_CLS_IDENTIFIER_STEREOTYPE)) {

		    if (!multipleIdentifierPisEncountered) {
			identifierPi = pi;
			if (pi.cardinality().maxOccurs > 1) {
			    MessageContext mc = result.addError(this, 104, currentCi.name(), pi.name());
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
				    defaultInstantPi.name());
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
				    defaultInstantPi.name());
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

	    if (pi.matches(Ldproxy2Constants.RULE_ALL_NOT_ENCODED)) {
		continue;
	    }

	    ImmutableFeatureSchema.Builder propMemberDefBuilder = new ImmutableFeatureSchema.Builder();

	    Type ldpType = determineLdproxyType(pi);

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

	    // FIXME - must take multiplicity into account - write method sourcePath(pi) ...
	    String sourcePath = computeDatabaseColumnName(pi, false);
	    boolean ignoreSourcePathOnPropertyLevel = false;

	    // handle embedded cases (datatype or link properties)
	    if (isMappedToLink(pi) || isTypeWithIdentityValueType(pi)) {

		propMemberDefBuilder.objectType("LINK");

		ignoreSourcePathOnPropertyLevel = true;

		SortedMap<String, FeatureSchema> linkPropertyDefs = new TreeMap<>();

		linkPropertyDefs.put("title", new ImmutableFeatureSchema.Builder().name("title")
			.label(pi.typeInfo().name + "-ID").type(Type.STRING).sourcePath(sourcePath).build());

		ImmutableFeatureSchema.Builder linkPropHrefBuilder = new ImmutableFeatureSchema.Builder();
		linkPropHrefBuilder.name("href").label(pi.typeInfo().name + "-ID").type(Type.STRING)
			.sourcePath(sourcePath);
		linkPropHrefBuilder.addAllTransformationsBuilders(new ImmutablePropertyTransformation.Builder()
			.stringFormat(determineUrlTemplateForValueType(pi)));
		linkPropertyDefs.put("href", linkPropHrefBuilder.build());

		propMemberDefBuilder.propertyMap(linkPropertyDefs);

	    } else if (pi.categoryOfValue() == Options.DATATYPE) {

		ClassInfo typeCi = pi.typeClass();

		List<PropertyInfo> newVisitedList = new ArrayList<>(visitedPiList);
		newVisitedList.add(pi);

		// detect circular dependency in the property path
		if (visitedPiList.stream().anyMatch(vPi -> vPi.inClass() == typeCi) || typeCi == currentCi) {

		    ClassInfo topType = visitedPiList.get(0).inClass();

		    MessageContext mc = result.addError(this, 117, topType.name(), propertyPath(newVisitedList));
		    if (mc != null) {
			mc.addDetail(this, 0, topType.fullNameInSchema());
		    }

		    continue;

		} else {

		    SortedMap<String, FeatureSchema> datatypePropertyDefs = computePropertyDefinitions(typeCi,
			    newVisitedList);
		    propMemberDefBuilder.propertyMap(datatypePropertyDefs);
		    propMemberDefBuilder.objectType(typeCi.name());
		}
	    }

	    if (StringUtils.isNotBlank(pi.initialValue()) && pi.isReadOnly()
		    && pi.matches(Ldproxy2Constants.RULE_PROP_READONLY)) {

		propMemberDefBuilder.constantValue(computeConstantValue(pi));

	    } else if (!ignoreSourcePathOnPropertyLevel) {

		propMemberDefBuilder.sourcePath(sourcePath);
	    }

	    // Generate constraints - from multiplicity and value type
	    Optional<ImmutableSchemaConstraints> constraints = Optional.empty();
	    boolean constraintCreated = false;
	    ImmutableSchemaConstraints.Builder constraintsBuilder = new ImmutableSchemaConstraints.Builder();
	    if (pi.cardinality().minOccurs != 0 && !pi.voidable()) {
		constraintCreated = true;
		constraintsBuilder.required(true);
	    }
	    if (pi.cardinality().maxOccurs > 1 && !(pi == identifierPi || pi == defaultGeometryPi)
		    && ldpType != Type.GEOMETRY) {
		if (pi.voidable()) {
		    constraintCreated = true;
		    constraintsBuilder.minOccurrence(0);
		} else {
		    constraintCreated = true;
		    constraintsBuilder.minOccurrence(pi.cardinality().minOccurs);
		}
		if (pi.cardinality().maxOccurs != Integer.MAX_VALUE) {
		    constraintCreated = true;
		    constraintsBuilder.maxOccurrence(pi.cardinality().maxOccurs);
		}
	    }
	    if (isEnumerationOrCodelistValueType(pi)) {
		constraintCreated = true;
		constraintsBuilder.codelist(codelistId(pi.typeClass()));
	    }
	    if (constraintCreated) {
		constraints = Optional.of(constraintsBuilder.build());
	    }
	    propMemberDefBuilder.constraints(constraints);

	    ImmutableFeatureSchema propMemberDef = propMemberDefBuilder.name(pi.name()).label(computeLabel(pi))
		    .description(computeDescription(pi)).build();
	    propertyDefs.put(pi.name(), propMemberDef);
	}

	return propertyDefs;
    }

    private String computeConstantValue(PropertyInfo pi) {

	String valueTypeName = pi.typeInfo().name;

	ProcessMapEntry pme = mapEntryParamInfos.getMapEntry(valueTypeName,
		pi.encodingRule(Ldproxy2Constants.PLATFORM));
			
	if (pme != null && !ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme, pi.typeInfo().id)) {
	    
	    if(valueTypeName.equalsIgnoreCase("Boolean")) {
		
		if("true".equalsIgnoreCase(pi.initialValue().trim())) {
		    return StringUtils.defaultIfBlank(mapEntryParamInfos.getCharacteristic(valueTypeName,
			    pi.encodingRule(Ldproxy2Constants.PLATFORM), Ldproxy2Constants.ME_PARAM_INITIAL_VALUE_ENCODING,
			    Ldproxy2Constants.ME_PARAM_INITIAL_VALUE_ENCODING_CHARACT_FALSE),"true");
		} else if("false".equalsIgnoreCase(pi.initialValue().trim())) {
		    return StringUtils.defaultIfBlank(mapEntryParamInfos.getCharacteristic(valueTypeName,
			    pi.encodingRule(Ldproxy2Constants.PLATFORM), Ldproxy2Constants.ME_PARAM_INITIAL_VALUE_ENCODING,
			    Ldproxy2Constants.ME_PARAM_INITIAL_VALUE_ENCODING_CHARACT_FALSE),"false");
		}
	    }
	}

	return pi.initialValue();
    }

    private String determineUrlTemplateForValueType(PropertyInfo pi) {

	String urlTemplate = null;

	String valueTypeName = pi.typeInfo().name;

	ProcessMapEntry pme = mapEntryParamInfos.getMapEntry(valueTypeName,
		pi.encodingRule(Ldproxy2Constants.PLATFORM));

	if (pme != null && !ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme, pi.typeInfo().id)
		&& "LINK".equalsIgnoreCase(pme.getTargetType())) {

	    urlTemplate = mapEntryParamInfos.getCharacteristic(valueTypeName,
		    pi.encodingRule(Ldproxy2Constants.PLATFORM), Ldproxy2Constants.ME_PARAM_LINK_INFOS,
		    Ldproxy2Constants.ME_PARAM_LINK_INFOS_CHARACT_URL_TEMPLATE);
	}

	if (StringUtils.isBlank(urlTemplate)) {
	    urlTemplate = "{{serviceUrl}}/collections/" + valueTypeName + "/items/{{value}}";
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

    private boolean isSimpleLdproxyType(Type t) {

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

    private Type determineTypeForValueType(PropertyInfo pi) {
	return determineTypeForValueType(pi, pi.cardinality().maxOccurs);
    }

    private Type determineTypeForValueType(PropertyInfo pi, int assumedMaxMultiplicity) {

	return determineTypeForValueType(pi.typeInfo().name, pi.typeInfo().id,
		pi.encodingRule(Ldproxy2Constants.PLATFORM), assumedMaxMultiplicity);
    }

    private Type determineTypeForValueType(String typeName, String typeId, String encodingRule,
	    int assumedMaxMultiplicity) {

	Type resType = determineLdproxyType(typeName, typeId, encodingRule);

	if (assumedMaxMultiplicity > 1) {
	    if (resType == Type.OBJECT) {
		resType = Type.OBJECT_ARRAY;
	    } else if (resType == Type.GEOMETRY) {
		// ignore -> no array for geometry properties
	    } else {
		resType = Type.VALUE_ARRAY;
	    }
	}

	return resType;
    }

    private Type determineLdproxyType(PropertyInfo pi) {

	return determineLdproxyType(pi.typeInfo().name, pi.typeInfo().id, pi.encodingRule(Ldproxy2Constants.PLATFORM));
    }

    private Type determineLdproxyType(String typeName, String typeId, String encodingRule) {

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
			resType = determineLdproxyType(valueType.taggedValue("numericType"), null,
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

    private boolean isTrueIgnoringCase(String s) {
	return StringUtils.isNotBlank(s) && "true".equalsIgnoreCase(s.trim());
    }

    private Optional<String> computeFeatureTitleTemplate(ClassInfo ci) {

	String tv = ci.taggedValue("ldpFeatureTitleTemplate");
	if (StringUtils.isBlank(tv)) {
	    return Optional.empty();
	} else {
	    return Optional.of(tv.trim());
	}
    }

    private ImmutableCodelistData createCodelistEntity(ClassInfo ci) {

	String id = codelistId(ci);

	Optional<String> labelOpt = computeLabel(ci);
	String label;
	if (labelOpt.isEmpty()) {
	    MessageContext mc = result.addError(this, 102, ci.name());
	    if (mc != null) {
		mc.addDetail(this, 1, ci.fullNameInSchema());
	    }
	    label = ci.name();
	} else {
	    label = labelOpt.get();
	}

	ImmutableCodelistData icd = cfg.builder().entity().codelist().id(id).label(label)
		.sourceType(IMPORT_TYPE.TEMPLATES).build();

	if (!ci.properties().isEmpty()) {

	    SortedMap<String, String> entries = new TreeMap<>();

	    for (PropertyInfo pi : ci.properties().values()) {

		String code = null;
		String targetValue = null;

		if (ci.matches(Ldproxy2Constants.RULE_CLS_CODELIST_DIRECT)) {
		    if (StringUtils.isBlank(pi.initialValue())) {
			MessageContext mc = result.addError(this, 100, ci.name(), pi.name());
			if (mc != null) {
			    mc.addDetail(this, 1, pi.fullNameInSchema());
			}
			continue;
		    } else {
			code = pi.initialValue();
			targetValue = pi.name();
		    }
		} else if (ci.matches(Ldproxy2Constants.RULE_CLS_CODELIST_TARGETBYTV)) {

		    if (StringUtils.isBlank(pi.initialValue())) {
			code = pi.name();
		    } else {
			code = pi.initialValue();
		    }

		    if (StringUtils.isBlank(pi.taggedValue("ldpCodeTargetValue"))) {
			MessageContext mc = result.addError(this, 101, ci.name(), pi.name());
			if (mc != null) {
			    mc.addDetail(this, 1, pi.fullNameInSchema());
			}
			continue;
		    } else {
			targetValue = pi.taggedValue("ldpCodeTargetValue").trim();
		    }
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

    private String computeDatabaseTableName(ClassInfo ci, boolean isAssociativeTableContext) {

	String result = ci.name();

	if (isAssociativeTableContext) {
	    result = result + associativeTableColumnSuffix;
	}

	result = result.toLowerCase(Locale.ENGLISH);

	result = StringUtils.substring(result, 0, maxNameLength);

	return result;
    }

    private String computeDatabaseColumnName(PropertyInfo pi, boolean isAssociativeTableContext) {

	String result = pi.name();

	if (isAssociativeTableContext) {
	    result = result + associativeTableColumnSuffix;
	} else {
	    if (valueTypeIsTypeWithIdentity(pi)) {
		result = result + foreignKeyColumnSuffix;
	    } else if (pi.categoryOfValue() == Options.DATATYPE) {
		result = result + foreignKeyColumnSuffixDatatype;
	    }
	}

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

    private Optional<String> computeLabel(Info i) {

	if (i != null && labelTemplate != null && i.matches(Ldproxy2Constants.RULE_ALL_DOCUMENTATION)) {

	    return Optional.of(i.derivedDocumentation(labelTemplate, descriptorNoValue));

	} else {
	    return Optional.empty();
	}
    }

    private Optional<String> computeDescription(Info i) {

	if (i != null && descriptionTemplate != null && i.matches(Ldproxy2Constants.RULE_ALL_DOCUMENTATION)) {

	    return Optional.of(i.derivedDocumentation(descriptionTemplate, descriptorNoValue));

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

	documentationTemplate = null;
	documentationNoValue = null;

	associativeTableColumnSuffix = null; // default: value of primaryKeyColumn parameter
	cfgTemplatePath = null; // default TODO
	dateFormat = null; // no default value
	dateTimeFormat = null; // no default value
	descriptionTemplate = "[[definition]]";
	descriptorNoValue = "";
	forceAxisOrder = Force.NONE;
	foreignKeyColumnSuffix = "";
	foreignKeyColumnSuffixDatatype = "";
	labelTemplate = "[[alias]]";
	maxNameLength = 63;
	nativeTimeZone = ZoneId.systemDefault();
	objectIdentifierName = "oid";
	primaryKeyColumn = "id";
	serviceApiTemplatePath = null; // default TODO
	serviceDescription = "FIXME";
	serviceLabel = "FIXME";
	serviceMetadataTemplatePath = null; // default TODO
	srid = 4326;

	outputDirectory = null;
	mainId = null;

	mapEntryParamInfos = null;

	typesWithIdentity = new ArrayList<>();
	codelistsAndEnumerations = new ArrayList<>();

	cfg = null;
    }

    @Override
    public void registerRulesAndRequirements(RuleRegistry r) {

	r.addRule(Ldproxy2Constants.RULE_ALL_DOCUMENTATION);
	r.addRule(Ldproxy2Constants.RULE_ALL_NOT_ENCODED);
	r.addRule(Ldproxy2Constants.RULE_ALL_SCHEMAS);
	r.addRule(Ldproxy2Constants.RULE_CLS_CODELIST_DIRECT);
	r.addRule(Ldproxy2Constants.RULE_CLS_CODELIST_TARGETBYTV);
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

	case 10: // TODO used?
	    return "Configuration parameter '$1$' has invalid value '$2$'. Using value '$3$' instead.";

	case 15:
	    return "No map entries provided via the configuration.";
	case 16:
	    return "Value '$1$' of configuration parameter $2$ does not match the regular expression: $3$. The parameter will be ignored.";
	case 17:
	    return "Type '$1$' is of a category not enabled for conversion, meaning that no ldproxy configuration items will be created to represent it.";
	case 18:
	    return "Schema '$1$' is not encoded. Thus class '$2$' (which belongs to that schema) is not encoded either.";
	case 19:
	    return "No rule for the conversion to an ldproxy codelist is defined. Codelist/enumeration '$1$' will be ignored.";
	case 20:
	    return "Type '$1$' is abstract. Conversion of abstract types is not supported by this target. The type will be ignored. Apply inheritance flattening (a model transformation) in order to handle abstract supertypes.";
	case 21:
	    return "";
	case 22:
	    return "Type '$1$' has been mapped to '$2$', as defined by the configuration.";
	case 23:
	    return "";

	case 100:
	    return Ldproxy2Constants.RULE_CLS_CODELIST_DIRECT
		    + " applies to codelist/enumeration '$1$'. However, code/enum '$2$' has no initial value. The code/enum cannot be encoded as defined by the conversion rule.";
	case 101:
	    return Ldproxy2Constants.RULE_CLS_CODELIST_TARGETBYTV
		    + " applies to codelist/enumeration '$1$'. However, code/enum '$2$' does not have a non-blank value for tag 'ldpCodeTargetValue'. The code/enum cannot be encoded as defined by the conversion rule.";
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

	case 503: // TODO used?
	    return "Output file '$1$' already exists in output directory ('$2$'). It will be deleted prior to processing.";
	case 504: // TODO used?
	    return "File has been deleted.";

	case 10001:
	    return "Generating ldproxy configuration items for application schema $1$.";
	case 10002:
	    return "Diagnostics-only mode. All output to files is suppressed.";
	default:
	    return "(" + Ldproxy2Target.class.getName() + ") Unknown message with number: " + mnr;
	}
    }
}
