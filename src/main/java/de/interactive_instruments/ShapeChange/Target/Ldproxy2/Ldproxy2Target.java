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
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import de.ii.ldproxy.cfg.LdproxyCfg;
import de.ii.ldproxy.ogcapi.domain.ImmutableOgcApiDataV2;
import de.ii.ldproxy.ogcapi.html.domain.ImmutableHtmlConfiguration;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableFeatureProviderSqlData;
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
import de.interactive_instruments.ShapeChange.Target.TargetUtil;

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
    protected static String forceAxisOrder = "NONE";
    protected static String foreignKeyColumnSuffix = "";
    protected static String foreignKeyColumnSuffixDatatype = "";
    protected static String labelTemplate = "[[alias]]";
    protected static int maxNameLength = 63;
    protected static String nativeTimeZone = ZoneId.systemDefault().toString();
    protected static String objectIdentifierName = "oid";
    protected static String primaryKeyColumn = "id";
    protected static String serviceApiTemplatePath = null; // default TODO
    protected static String serviceDescription = "FIXME";
    protected static String serviceLabel = "FIXME";
    protected static String serviceMetadataTemplatePath = null; // default TODO
    protected static int srid = 4326;

    protected static boolean isUnitTest = false;

    protected static String outputDirectory = null;
    protected static String providerFilename = null;
    protected static String serviceFilename = null;

    /**
     * Contains information parsed from the 'param' attributes of each map entry
     * defined for this target.
     */
    protected static MapEntryParamInfos mapEntryParamInfos = null;

//    protected static SortedMap<PackageInfo, JsonSchemaDocument> jsDocsByPkg = new TreeMap<>();
//    protected static Map<ClassInfo, JsonSchemaDocument> jsDocsByCi = new HashMap<>();

    protected static LdproxyCfg cfg = null;
    protected static ImmutableOgcApiDataV2 serviceConfig = null;
    protected static ImmutableFeatureProviderSqlData providerConfig = null;

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
	}

	if (!initialised) {

	    initialised = true;

	    outputDirectory = options.parameter(this.getClass().getName(), "outputDirectory");
	    if (outputDirectory == null)
		outputDirectory = options.parameter("outputDirectory");
	    if (outputDirectory == null)
		outputDirectory = options.parameter(".");

	    if (mainAppSchema == null) {
		providerFilename = schema.name();
	    } else {
		providerFilename = mainAppSchema.name();
	    }
	    providerFilename = providerFilename.replaceAll("\\W", "_");
	    serviceFilename = providerFilename;

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

	    // TODO enum with allowed values? if so, apply configuration check
	    forceAxisOrder = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_FORCE_AXIS_ORDER, "NONE", false, true);

	    foreignKeyColumnSuffix = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_FK_COLUMN_SUFFIX, "", false, true);

	    foreignKeyColumnSuffixDatatype = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_FK_COLUMN_SUFFIX_DATATYPE, "", false, true);

	    labelTemplate = options.parameterAsString(this.getClass().getName(), Ldproxy2Constants.PARAM_LABEL_TEMPLATE,
		    "[[alias]]", false, true);

	    maxNameLength = options.parameterAsInteger(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_MAX_NAME_LENGTH, 63);

	    nativeTimeZone = options.parameterAsString(this.getClass().getName(),
		    Ldproxy2Constants.PARAM_NATIVE_TIME_ZONE, ZoneId.systemDefault().toString(), false, true);

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

	    // create a new api extension
	    // builders for all extension types can be created using
	    // cfg.builder().ogcApiExtension()
	    ImmutableHtmlConfiguration html = cfg.builder().ogcApiExtension().html().footerText("TEST").build();

	    serviceConfig = cfg.builder().entity().api().id("test").label("Test")
		    .addExtensions(html).build();
	    if (isUnitTest) {
		serviceConfig = serviceConfig.withCreatedAt(Ldproxy2Constants.UNITTEST_UNIX_TIME)
			.withLastModified(Ldproxy2Constants.UNITTEST_UNIX_TIME);
	    }

	    providerConfig = cfg.builder().entity().provider().id("test").providerType("FEATURE")
		    .featureProviderType("SQL").build();
	    if (isUnitTest) {
		providerConfig = providerConfig.withCreatedAt(Ldproxy2Constants.UNITTEST_UNIX_TIME)
			.withLastModified(Ldproxy2Constants.UNITTEST_UNIX_TIME);
	    }
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

	/*
	 * This target could support basic types, but does not do so yet. The JSON
	 * Schema target is an example of how basic types can be identified in the model
	 * (through determination if ci inherits (directly or indirectly) from a type
	 * that is mapped to a simple ldproxy type; if so, ci is a basic type.
	 */

	if (ci.category() == Options.OBJECT || ci.category() == Options.FEATURE || ci.category() == Options.DATATYPE
		|| ci.category() == Options.ENUMERATION || ci.category() == Options.CODELIST) {

//	    Path dataDirectory = Path.of("/home/zahnen/cfgapp");
//	    
//	    LdproxyCfg cfg = new LdproxyCfg(dataDirectory);
//	    
//	    Builder providerBuilder = cfg.builder().entity().provider();
	    // TODO
//	    registerClass(ci, null);
	    System.out.println("processing " + ci.name());

	} else {
	    // conversion of unions and mixins not supported
	    result.addInfo(this, 17, ci.name());
	}
    }

    /**
     * @param pme    map entry that would apply for the type with given ID
     * @param typeId ID of the type to check
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
     *         under the JSON Schema encoding rule that applies to the class
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
		// TODO write all codelist entities
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
    }

    @Override
    public void reset() {

	model = null;

	initialised = false;
	diagnosticsOnly = false;
	numberOfEncodedSchemas = 0;

	isUnitTest = false;

	documentationTemplate = null;
	documentationNoValue = null;

	associativeTableColumnSuffix = null; // default: value of primaryKeyColumn parameter
	cfgTemplatePath = null; // default TODO
	dateFormat = null; // no default value
	dateTimeFormat = null; // no default value
	descriptionTemplate = "[[definition]]";
	descriptorNoValue = "";
	forceAxisOrder = "NONE";
	foreignKeyColumnSuffix = "";
	foreignKeyColumnSuffixDatatype = "";
	labelTemplate = "[[alias]]";
	maxNameLength = 63;
	nativeTimeZone = ZoneId.systemDefault().toString();
	objectIdentifierName = "oid";
	primaryKeyColumn = "id";
	serviceApiTemplatePath = null; // default TODO
	serviceDescription = "FIXME";
	serviceLabel = "FIXME";
	serviceMetadataTemplatePath = null; // default TODO
	srid = 4326;

	outputDirectory = null;
	providerFilename = null;
	serviceFilename = null;

	mapEntryParamInfos = null;

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
	    return "";
	case 20:
	    return "";
	case 21:
	    return "";
	case 22:
	    return "Type '$1$' has been mapped to '$2$', as defined by the configuration.";
	case 23:
	    return "";

	case 101:
	    return "";
	case 102:
	    return "";
	case 103:
	    return "";

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
