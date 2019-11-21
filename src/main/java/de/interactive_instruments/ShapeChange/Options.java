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
 * (c) 2002-2013 interactive instruments GmbH, Bonn, Germany
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

package de.interactive_instruments.ShapeChange;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.base.Splitter;

import de.interactive_instruments.ShapeChange.AIXMSchemaInfos.AIXMSchemaInfo;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.Stereotypes;
import de.interactive_instruments.ShapeChange.Model.StereotypesCacheSet;
import de.interactive_instruments.ShapeChange.Model.TaggedValueNormalizer;
import de.interactive_instruments.ShapeChange.Model.TaggedValues;
import de.interactive_instruments.ShapeChange.Model.TaggedValuesCacheArray;
import de.interactive_instruments.ShapeChange.Model.TaggedValuesCacheMap;
import de.interactive_instruments.ShapeChange.Target.FeatureCatalogue.FeatureCatalogue;
import de.interactive_instruments.ShapeChange.Target.Ontology.GeneralDataProperty;
import de.interactive_instruments.ShapeChange.Target.Ontology.GeneralObjectProperty;
import de.interactive_instruments.ShapeChange.Target.Ontology.RdfGeneralProperty;
import de.interactive_instruments.ShapeChange.Util.XMLUtil;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments <dot>
 *         de)
 *
 */
public class Options {

	//
	// Constants
	//

	/** Parser feature ids. */
	public static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
	public static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
	public static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
	public static final String W3C_RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static final String W3C_RDFS = "http://www.w3.org/2000/01/rdf-schema#";
	public static final String W3C_OWL = "http://www.w3.org/2002/07/owl#";
	public static final String W3C_OWL2XML = "http://www.w3.org/2006/12/owl2-xml#";
	public static final String DC = "http://purl.org/dc/elements/1.1/";
	public static final String OGC_GEOSPARQL = "http://www.opengis.net/geosparql#";
	public static final String W3C_SKOS = "http://www.w3.org/2004/02/skos/core#";

	public static final String SCRS_NS = "http://www.interactive-instruments.de/ShapeChange/Result";
	public static final String SCAI_NS = "http://www.interactive-instruments.de/ShapeChange/AppInfo";
	public static final String SCHEMATRON_NS = "http://purl.oclc.org/dsdl/schematron";
	public static final String DGIWGSP_NSABR = "gmldgiwgsp";
	public static final String DGIWGSP_NS = "http://www.dgiwg.org/gml/3.2/profiles/spatial/1.0/";
	public static final String GMLSF_NSABR = "gmlsf";
	public static final String GMLSF_NS = "http://www.opengis.net/gmlsf/2.0";

	// TODO move these to each target
	public static final String DEF_NS = "http://www.interactive-instruments.de/ShapeChange/Definitions/0.5";
	public static final String XTRASERVER_NS = "http://www.interactive-instruments.de/namespaces/XtraServer";
	public static final String GDBDOC_NS = "http://www.interactive-instruments.de/namespaces/GdbDoc";

	/* Target Java Classes */
	/**
	 *
	 */
	public static final String TargetXmlSchemaClass = "de.interactive_instruments.ShapeChange.Target.XmlSchema.XmlSchema";
	public static final String TargetFOL2SchematronClass = "de.interactive_instruments.ShapeChange.Target.FOL2Schematron.FOL2Schematron";
	public static final String TargetJsonSchemaClass = "de.interactive_instruments.ShapeChange.Target.JSON.JsonSchema";
	public static final String TargetFeatureCatalogueClass = "de.interactive_instruments.ShapeChange.Target.FeatureCatalogue.FeatureCatalogue";
	public static final String TargetOWLISO19150Class = "de.interactive_instruments.ShapeChange.Target.Ontology.OWLISO19150";
	public static final String TargetRDFClass = "de.interactive_instruments.ShapeChange.Target.Ontology.RDF";
	public static final String TargetSQLClass = "de.interactive_instruments.ShapeChange.Target.SQL.SqlDdl";
	public static final String TargetArcGISWorkspaceClass = "de.interactive_instruments.ShapeChange.Target.ArcGISWorkspace.ArcGISWorkspace";
	public static final String TargetReplicationSchemaClass = "de.interactive_instruments.ShapeChange.Target.ReplicationSchema.ReplicationXmlSchema";
	public static final String TargetApplicationSchemaMetadata = "de.interactive_instruments.ShapeChange.Target.Metadata.ApplicationSchemaMetadata";
	public static final String TargetModelExport = "de.interactive_instruments.ShapeChange.Target.ModelExport.ModelExport";
	public static final String TargetProfileTransferEA = "de.interactive_instruments.ShapeChange.Target.ProfileTransfer.ProfileTransferEA";
	public static final String TargetCDB = "de.interactive_instruments.ShapeChange.Target.CDB.CDB";
	public static final String TargetCodeListDictionariesML = "de.interactive_instruments.ShapeChange.Target.Codelists.CodelistDictionariesML";
	public static final String TargetLdproxy = "de.interactive_instruments.ShapeChange.Target.Ldproxy.Config";

	/** XML Schema encoding rules */
	public static final String ISO19136_2007 = "iso19136_2007".toLowerCase();
	public static final String ISO19139_2007 = "iso19139_2007".toLowerCase();
	public static final String ISO19136_2007_SHAPECHANGE_1_0 = "iso19136_2007_ShapeChange_1.0_Extensions"
			.toLowerCase();
	public static final String ISO19136_2007_INSPIRE = "iso19136_2007_INSPIRE_Extensions"
			.toLowerCase();
	public static final String ISO19136_2007_NO_GML = "iso19136_2007_NoGmlBaseTypes"
			.toLowerCase();
	public static final String ISO19150_2014 = "iso19150_2014".toLowerCase();
	public static final String SQL = "sql";
	public static final String SWECOMMON2 = "ogcSweCommon2".toLowerCase();
	public static final String NOT_ENCODED = "notEncoded".toLowerCase();
	public static final String GCSR_ENC = "gcsr".toLowerCase();

	/** Well known stereotypes */
	public static final Set<String> classStereotypes = Stream.of( "codelist", "enumeration",
			"datatype", "featuretype", "type", "basictype", "interface",
			"union", "abstract", "fachid", "schluesseltabelle", "adeelement",
			"featureconcept", "attributeconcept", "valueconcept", "roleconcept",
			"aixmextension", "retired" )
			    .collect(Collectors.toSet());
	public static final Set<String> assocStereotypes = Stream.of( "disjoint", "retired" )
	    .collect(Collectors.toSet());

	public static final Set<String> propertyStereotypes = Stream.of( "voidable",
			"identifier", "version", "property", "estimated", "enum",
			"retired", "propertymetadata" )
		    .collect(Collectors.toSet());
	public static final Set<String> packageStereotypes = Stream.of( "application schema",
			"schema", "bundle", "leaf", "retired" )
			    .collect(Collectors.toSet());
	public static final Set<String> depStereotypes = Stream.of( "import", "include",
			"retired" )
	    .collect(Collectors.toSet());

	/** Carriage Return and Line Feed characters. */
	public static final String CRLF = "\r\n";

	/** SQL targets. TODO move the relevant targets */
	public static final int NONE = 0;
	// public static final int SFSQL = 1;
	public static final int ORACLE10 = 2;
	public static final int POSTGIS = 3;
	public static final int GDB = 4;

	/** Global vs local declarations. */
	public static final int GLOBAL = 1;
	public static final int LOCAL = 2;

	/** Class categories. */
	public static final int UNKNOWN = -1;
	public static final int FEATURE = 1;
	public static final int CODELIST = 2;
	public static final int ENUMERATION = 3;
	public static final int MIXIN = 4;
	public static final int DATATYPE = 5;
	public static final int OBJECT = 6;
	public static final int GMLOBJECT = 6; // for backwards compatibility
	public static final int BASICTYPE = 7;
	public static final int UNION = 8;
	// 2013-10-14: UNIONDIRECT has been retired, use UNION and
	// ClassInfo.isUnionDirect() instead
	// public static final int UNIONDIRECT = 9;
	public static final int OKSTRAKEY = 11;
	public static final int OKSTRAFID = 12;
	public static final int FEATURECONCEPT = 13;
	public static final int ATTRIBUTECONCEPT = 14;
	public static final int VALUECONCEPT = 15;
	public static final int AIXMEXTENSION = 16;
	public static final int ROLECONCEPT = 17;

	/* These constants are used when loading diagrams from the input model */
	public static final String ELEMENT_NAME_KEY_FOR_DIAGRAM_MATCHING = "NAME";
	public static final String IMAGE_INCLUSION_CLASS_REGEX = "NAME";
	public static final String IMAGE_INCLUSION_PACKAGE_REGEX = "NAME";

	/** Constants of the ShapeChangeConfiguration */
	public static final String INPUTELEMENTID = "INPUT";

	/**
	 * Defines the name of the configuration parameter via which a
	 * comma-separated list of package names to exclude from the model loading
	 * can be provided.
	 */
	public static final String PARAM_INPUT_EXCLUDED_PACKAGES = "excludedPackages";

	/**
	 * Defines the name of the input parameter that provides the location of an
	 * excel file with constraints (currently only SBVR rules are supported)
	 * that shall be loaded before postprocessing the input model.
	 */
	public static final String PARAM_CONSTRAINT_EXCEL_FILE = "constraintExcelFile";

	public static final String PARAM_IGNORE_ENCODING_RULE_TVS = "ignoreEncodingRuleTaggedValues";
	public static final String PARAM_ONLY_DEFERRABLE_OUTPUT_WRITE = "onlyDeferrableOutputWrite";
	public static final String PARAM_USE_STRING_INTERNING = "useStringInterning";
	public static final String PARAM_LANGUAGE = "language"; // TODO document

	/**
	 * Alias: none
	 * <p>
	 * Required / Optional: optional
	 * <p>
	 * Type: String (comma separated list of values)
	 * <p>
	 * Default Value: notValid, retired, superseded
	 * <p>
	 * Explanation: Comma separated list of values that, if one of them is being
	 * set as the 'status' tagged value of a class, will lead to the class not
	 * being loaded.
	 * <p>
	 * Applies to Rule(s): none – default behavior
	 */
	public static final String PARAM_PROHIBIT_LOADING_CLASSES_WITH_STATUS_TV = "prohibitLoadingClassesWithStatusTaggedValue";
	public static final String[] DEFAULT_FOR_PROHIBIT_LOADING_CLASSES_WITH_STATUS_TV = new String[] {
			"notValid", "retired", "superseded" };
	private boolean prohibitedStatusValuesWhenLoadingClasses_accessed = false;
	protected Set<String> prohibitedStatusValuesWhenLoadingClasses = null;

	/**
	 * If 'true', semantic validation of the ShapeChange configuration will not
	 * be performed.
	 */
	public static final String PARAM_SKIP_SEMANTIC_VALIDATION_OF_CONFIG = "skipSemanticValidationOfShapeChangeConfiguration";

	/**
	 * If set to “array”, ShapeChange will use a memory optimized implementation
	 * of tagged values when processing the model.Use this option when
	 * processing very large models. ShapeChange can process 100+MB sized models
	 * without problem. However, if processing involves many transformations and
	 * target derivations you may hit a memory limit, which is determined by the
	 * maximum amount of memory you can assign to the Java process in which
	 * ShapeChange is running. On Windows machines that were used for
	 * development, that limit was near 1.1GB.
	 */
	public static final String PARAM_TAGGED_VALUE_IMPL = "taggedValueImplementation";

	public static final String PARAM_DONT_CONSTRUCT_ASSOCIATION_NAMES = "dontConstructAssociationNames";

	/**
	 * Set this input parameter to <code>true</code> if constraints shall only
	 * be loaded for classes and properties from the schemas selected for
	 * processing (and ignoring all constraints from other packages).
	 * <p>
	 * Don't make use of this parameter if one of the classes from the selected
	 * schema packages extends another class from an external package (e.g. an
	 * ISO package) and needs to inherit constraints from that class!
	 * <p>
	 * This parameter is primarily a convenience mechanism to avoid loading and
	 * parsing constraints from external packages (especially ISO packages) that
	 * are irrelevant for processing. So on the one hand this can speed up model
	 * loading. On the other hand, it can prevent messages about constraints
	 * that were parsed from the elements of an external package from appearing
	 * in the log.
	 */
	public static final String PARAM_LOAD_CONSTRAINT_FOR_SEL_SCHEMAS_ONLY = "loadConstraintsForSelectedSchemasOnly";

	// Application schema defaults (namespace and version)
	public String xmlNamespaceDefault = "FIXME";
	public String xmlNamespaceAbbreviationDefault = "FIXME";
	public String appSchemaVersion = "unknown";

	// Name of the configuration file
	public String configFile = null;

	/** GML core namespaces */
	public String GML_NS = "http://www.opengis.net/gml/3.2";
	public static String GMLEXR_NS = "http://www.opengis.net/gml/3.3/exr";

	// Default target version of GML
	// Note: the use of ISO/TS 19139 requires the use of GML 3.2
	// This parameter is deprecated and should not be used
	// TODO remove usages from external targets
	public String gmlVersion = "3.2";

	/**
	 * If set to true, schemas (from the model) will be processed in some stable
	 * (not random) order. To change the processing order of the classes in a
	 * specific Target use the targetParameter "sortedOutput" in the
	 * configuration file.
	 */
	public boolean sortedSchemaOutput = false;

	// Bugfixes:

	// Fix for bugs in Rose
	public boolean roseBugFixDuplicateGlobalDataTypes = true;

	// Fix for bugs in EA
	public boolean eaBugFixWrongID = true;
	public boolean eaBugFixPublicPackagesAreMarkedAsPrivate = true;
	public boolean eaIncludeExtentsions = true;

	// Fix for bugs in ArgoUML
	public boolean argoBugFixMissingDOCTYPE = false;

	// Temporary directory for ShapeChange run
	public static final String DEFAULT_TMP_DIR_PATH = "temp";
	public static final String TMP_DIR_PATH_PARAM = "tmpDirectory";
	protected File tmpDir = null;

	/**
	 * Map of targets to generate from the input model. Keys are the names of
	 * the Java classes which must implement the Target interface and values are
	 * the requested processing modes.
	 * 
	 * @see ProcessMode
	 */
	protected HashMap<String, ProcessMode> fTargets = new HashMap<String, ProcessMode>();

	/** Hash table for additional parameters */
	protected HashMap<String, String> fParameters = new HashMap<String, String>();

	/**
	 * Hash table for string replacements. Must not be reset, because
	 * replacement information is not contained in the configuration itself!
	 */
	protected HashMap<String, String> fReplace = new HashMap<String, String>();

	/** Hash table for type and element mappings */
	protected HashMap<String, MapEntry> fTypeMap = new HashMap<String, MapEntry>();

	/**
	 * Key: type + "#" + xsdEncodingRule
	 * <p>
	 * Value: MapEntry with:
	 * <ul>
	 * <li>rule: ("direct")</li>
	 * <li>p1: xmlType</li>
	 * <li>p2: xmlTypeType+"/"+xmlTypeContent</li>
	 * </ul>
	 */
	protected HashMap<String, MapEntry> fBaseMap = new HashMap<String, MapEntry>();

	/**
	 * Key: type + "#" + xsdEncodingRule
	 * <p>
	 * Value: MapEntry with:
	 * <ul>
	 * <li>rule: ("direct")</li>
	 * <li>p1: xmlElement</li>
	 * </ul>
	 */
	protected HashMap<String, MapEntry> fElementMap = new HashMap<String, MapEntry>();
	protected HashMap<String, MapEntry> fAttributeMap = new HashMap<String, MapEntry>();
	protected HashMap<String, MapEntry> fAttributeGroupMap = new HashMap<String, MapEntry>();

	/**
	 * Key: type + "#" + xsdEncodingRule
	 * <p>
	 * Value: boolean value of XsdMapEntry/@xmlReferenceable, or
	 * <code>null</code> if that XML attribute was not set
	 * </ul>
	 */
	protected Map<String, Boolean> fXmlReferenceableMap = new HashMap<>();

	/**
	 * Key: type + "#" + xsdEncodingRule
	 * <p>
	 * Value: boolean value of XsdMapEntry/@xmlElementHasSimpleContent, or
	 * <code>null</code> if that XML attribute was not set
	 * </ul>
	 */
	protected Map<String, Boolean> fXmlElementHasSimpleContentMap = new HashMap<>();

	/**
	 * Map entries for a non-XML Schema target
	 * 
	 * key: {type attribute value}#{rule attribute value}
	 * 
	 * value: map entry with this combination of type and rule
	 * 
	 * NOTE: separation by class is not necessary, since the map is reset when
	 * the converter is about to process a target (which also leads to loading
	 * the specific configuration of that target)
	 */
	protected Map<String, ProcessMapEntry> targetMapEntryByTypeRuleKey = new HashMap<String, ProcessMapEntry>();

	/** Hash table for all stereotype and tag aliases */
	protected HashMap<String, String> fStereotypeAliases = new HashMap<String, String>();
	protected HashMap<String, String> fTagAliases = new HashMap<String, String>();

	/** Hash table for all descriptor sources */
	protected HashMap<String, String> fDescriptorSources = new HashMap<String, String>();

	public static final String DERIVED_DOCUMENTATION_DEFAULT_TEMPLATE = "[[definition]]";
	public static final String DERIVED_DOCUMENTATION_DEFAULT_NOVALUE = "";

	public static final String LF = System.getProperty("line.separator");
	public static final String DERIVED_DOCUMENTATION_INSPIRE_TEMPLATE = "-- Name --"
			+ LF + "[[alias]]" + LF + LF + "-- Definition --" + LF
			+ "[[definition]]" + LF + LF + "-- Description --" + LF
			+ "[[description]]";

	/**
	 * Hash table for external namespaces
	 * <p>
	 * key: namespace abbreviation / prefix; value: MapEntry (arg1: namespace,
	 * arg2: location)
	 */
	protected HashMap<String, MapEntry> fNamespaces = new HashMap<String, MapEntry>();

	/**
	 * Hash table for packages
	 * <p>
	 * key: package name <br/>
	 * value: MapEntry with:
	 * <ul>
	 * <li>rule: namespace URL of the schema (value of XML attribute 'ns' of
	 * PackageInfo configuration element)</li>
	 * <li>p1: namespace abbreviation (value of XML attribute 'nsabr' of
	 * PackageInfo configuration element)</li>
	 * <li>p2: desired filename of the output XML Schema document (value of XML
	 * attribute 'xsdDocument' of PackageInfo configuration element)</li>
	 * <li>p3: version of the schema (value of XML attribute 'version' of
	 * PackageInfo configuration element)</li>
	 * </ul>
	 */
	protected HashMap<String, MapEntry> fPackages = new HashMap<String, MapEntry>();

	/**
	 * Hash table for schema locations
	 * <p>
	 * key: namespace, value: location
	 */
	protected HashMap<String, String> fSchemaLocations = new HashMap<String, String>();

	/** Hash table for schema requirements and conversion rules */
	protected HashSet<String> fAllRules = new HashSet<String>();
	protected HashSet<String> fRulesInEncRule = new HashSet<String>();

	/** Hash table for encoding rule extensions */
	protected HashMap<String, String> fExtendsEncRule = new HashMap<String, String>();

	/** documentation separators */
	protected String extractSeparator = null;
	protected String definitionSeparator = null;
	protected String descriptionSeparator = null;
	protected String nameSeparator = null;

	public String extractSeparator() {
		if (extractSeparator == null)
			extractSeparator = parameter("extractSeparator");
		return extractSeparator;
	}

	public String definitionSeparator() {
		if (definitionSeparator == null)
			definitionSeparator = parameter("definitionSeparator");
		return definitionSeparator;
	}

	public String descriptionSeparator() {
		if (descriptionSeparator == null)
			descriptionSeparator = parameter("descriptionSeparator");
		return descriptionSeparator;
	}

	public String nameSeparator() {
		if (nameSeparator == null)
			nameSeparator = parameter("nameSeparator");
		return nameSeparator;
	}

	// determines, if unset class descriptors are inherited from a superclass of
	// the same name
	boolean getDescriptorsFromSupertypesInitialised = false;
	boolean getDescriptorsFromSupertypes = false;

	public boolean getDescriptorsFromSupertypes() {
		if (!getDescriptorsFromSupertypesInitialised) {
			getDescriptorsFromSupertypesInitialised = true;
			String s = parameter("inheritDocumentation");
			// still support deprecated parameter in the FeatureCatalogue target
			// for backward compatibility
			if (s == null || !s.equals("true"))
				s = parameter(FeatureCatalogue.class.getName(),
						"inheritDocumentation");
			if (s != null && s.equals("true"))
				getDescriptorsFromSupertypes = true;
		}
		return getDescriptorsFromSupertypes;
	}

	// determines, if unset package, class or property descriptors are inherited
	// from a dependency
	boolean getDescriptorsFromDependencyInitialised = false;
	boolean getDescriptorsFromDependency = true;

	public boolean getDescriptorsFromDepedency() {
		if (!getDescriptorsFromDependencyInitialised) {
			getDescriptorsFromDependencyInitialised = true;
			String s = parameter("documentationFromDependency");
			if (s != null && s.equals("false"))
				getDescriptorsFromDependency = false;
		}
		return getDescriptorsFromDependency;
	}

	// /**
	// * List of transformer configurations that directly reference the input
	// * element.
	// */
	// protected List<TransformerConfiguration> transformerConfigurations =
	// null;
	//
	// /**
	// * List of all target configurations that directly reference the input
	// * element.
	// */
	// protected List<TargetConfiguration> inputTargetConfigurations = null;

	/**
	 * True, if constraints shall be created for properties, else false.
	 */
	protected boolean constraintCreationForProperties = true;

	/**
	 * True, if xxxEncodingRule tagged values shall be ignored (because the
	 * model is wrong and needs cleanup), else false.
	 */
	protected boolean ignoreEncodingRuleTaggedValues = false;

	protected boolean useStringInterning = false;
	protected boolean dontConstructAssociationNames = false;
	protected boolean allowAllTags = false;
	protected boolean allowAllStereotypes = false;
	protected Set<String> addedStereotypes = new HashSet<>();
	protected String language = "en";

	/**
	 * Set of class stereotypes for which constraints shall be created; null if
	 * constraints for all classes shall be created.
	 */
	protected HashSet<Integer> classTypesToCreateConstraintsFor = null;

	protected InputConfiguration inputConfig = null;
	protected Map<String, String> dialogParameters = null;
	protected Map<String, String> logParameters = null;
	protected ProcessConfiguration currentProcessConfig = null;
	protected List<TargetConfiguration> inputTargetConfigs = new ArrayList<TargetConfiguration>();
	protected List<TransformerConfiguration> inputTransformerConfigs = new ArrayList<TransformerConfiguration>();
	private String inputId = null;

	private List<TargetConfiguration> targetConfigs = null;
	/**
	 * key: 'id' assigned to the transformer; value: the transformer
	 * configuration
	 */
	private Map<String, TransformerConfiguration> transformerConfigs = null;

	protected File imageTmpDir = null;
	protected File linkedDocTmpDir = null;

	protected TaggedValueNormalizer tvNormalizer = null;

	private Map<String, AIXMSchemaInfo> schemaInfos;

	/**
	 * @return True, if constraints shall be created for properties, else false.
	 */
	public boolean isConstraintCreationForProperties() {
		return this.constraintCreationForProperties;
	}

	/**
	 * Determines if AIXM schema are being processed, which require special
	 * treatment (due to the AIXM extension mechanism and because AIXM feature
	 * types are dynamic features).
	 *
	 * @return <code>true</code> if the input configuration element has
	 *         parameter 'isAIXM' with value 'true' (ignoring case), else
	 *         <code>false</code>.
	 */
	public boolean isAIXM() {
		return this.parameter("isAIXM") != null
				&& this.parameter("isAIXM").equalsIgnoreCase("true");
	}

	public boolean isLoadConstraintsForSelectedSchemasOnly() {

		return this
				.parameter(PARAM_LOAD_CONSTRAINT_FOR_SEL_SCHEMAS_ONLY) != null
				&& this.parameter(PARAM_LOAD_CONSTRAINT_FOR_SEL_SCHEMAS_ONLY)
						.equalsIgnoreCase("true");
	}

	/**
	 * Determines if only deferrable output writing shall be executed. This can
	 * be used to transform the temporary XML with feature catalogue information
	 * in a separate ShapeChange run. That run does no longer need to read the
	 * UML model and can thus be executed using 64bit Java, which supports
	 * bigger heap sizes that may be required to transform huge XML files.
	 *
	 * @return <code>true</code> if the input configuration element has
	 *         parameter {@value #PARAM_ONLY_DEFERRABLE_OUTPUT_WRITE} with value
	 *         'true' (ignoring case), else <code>false</code>.
	 */
	public boolean isOnlyDeferrableOutputWrite() {
		return this.parameter(PARAM_ONLY_DEFERRABLE_OUTPUT_WRITE) != null
				&& this.parameter(PARAM_ONLY_DEFERRABLE_OUTPUT_WRITE)
						.equalsIgnoreCase("true");
	}

	/**
	 * @return True, if xxxEncodingRule tagged values shall be ignored (because
	 *         the model is wrong and needs cleanup), else false.
	 */
	public boolean ignoreEncodingRuleTaggedValues() {
		return this.ignoreEncodingRuleTaggedValues;
	}

	public boolean isClassTypeToCreateConstraintsFor(int classCategory) {
		if (this.classTypesToCreateConstraintsFor == null)
			return true;
		else
			return this.classTypesToCreateConstraintsFor
					.contains(Integer.valueOf(classCategory));
	}

	/** A map entry. */
	protected void addTypeMapEntry(String k1, String k2, String s1, String s2) {
		fTypeMap.put(k1 + "#" + k2, new MapEntry(s1, s2));
	}

	protected void addTypeMapEntry(String k1, String k2, String s1, String s2,
			String s3) {
		fTypeMap.put(k1 + "#" + k2, new MapEntry(s1, s2, s3));
	}

	protected void addTypeMapEntry(String k1, String k2, String s1, String s2,
			String s3, String s4) {
		fTypeMap.put(k1 + "#" + k2, new MapEntry(s1, s2, s3, s4));
	}

	public MapEntry typeMapEntry(String k1, String k2) {
		String rule = k2;
		MapEntry me = null;
		while (me == null && rule != null) {
			me = fTypeMap.get(k1 + "#" + rule);
			rule = extendsEncRule(rule);
		}
		return me;
	}

	public void addTargetTypeMapEntry(ProcessMapEntry pme) {

		targetMapEntryByTypeRuleKey.put(pme.getType() + "#" + pme.getRule(),
				pme);
	}

	public ProcessMapEntry targetMapEntry(String type, String rule) {

		ProcessMapEntry pme = null;

		while (pme == null && rule != null) {
			pme = targetMapEntryByTypeRuleKey.get(type + "#" + rule);
			rule = extendsEncRule(rule);
		}

		return pme;
	}

	/**
	 * Adds a new MapEntry to fBaseMap.
	 * <p>
	 * The key is k1 + "#" + k2.
	 * <p>
	 * The value is a MapEntry with:
	 * <ul>
	 * <li>rule: s1</li>
	 * <li>p1: s2</li>
	 * <li>p2: s3</li>
	 * </ul>
	 *
	 * @param k1
	 *               type
	 * @param k2
	 *               xsdEncodingRule
	 * @param s1
	 *               ("direct")
	 * @param s2
	 *               xmlType
	 * @param s3
	 *               xmlTypeType+"/"+xmlTypeContent
	 */
	protected void addBaseMapEntry(String k1, String k2, String s1, String s2,
			String s3) {
		fBaseMap.put(k1 + "#" + k2, new MapEntry(s1, s2, s3));
	}

	/**
	 * Tries to find a MapEntry that defines the mapping of a type to its
	 * xmlType, based upon the given encoding rule or any rules it extends.
	 *
	 * @param k1
	 *               type name
	 * @param k2
	 *               encoding rule name
	 * @return MapEntry with rule=("direct"), p1=xmlType and
	 *         p2=xmlTypeType+"/"+xmlTypeContent
	 */
	public MapEntry baseMapEntry(String k1, String k2) {
		String rule = k2;
		MapEntry me = null;
		while (me == null && rule != null) {
			me = fBaseMap.get(k1 + "#" + rule);
			rule = extendsEncRule(rule);
		}
		return me;
	}

	/**
	 * Tries to find a MapEntry that defines the mapping of a type to its
	 * xmlType, with given xmlTypeType and xmlTypeContent, based upon the given
	 * encoding rule or any rules it extends.
	 *
	 * @param k1
	 *                           type name
	 * @param k2
	 *                           encoding rule name
	 * @param xmlTypeType
	 *                           Identifies, if the xmlType is “simple” or
	 *                           “complex”.
	 * @param xmlTypeContent
	 *                           Identifies, if the content of the xmlType is
	 *                           “simple” or “complex”.
	 * @return MapEntry with rule=("direct"), p1=xmlType and
	 *         p2=xmlTypeType+"/"+xmlTypeContent
	 */
	public MapEntry baseMapEntry(String k1, String k2, String xmlTypeType,
			String xmlTypeContent) {
		String rule = k2;
		MapEntry me = null;
		while (me == null && rule != null) {
			MapEntry mex = fBaseMap.get(k1 + "#" + rule);
			if (mex != null && mex.p2
					.equalsIgnoreCase(xmlTypeType + "/" + xmlTypeContent)) {
				me = mex;
			}
			rule = extendsEncRule(rule);
		}
		return me;
	}

	protected void addElementMapEntry(String k1, String k2, String s1,
			String s2) {
		fElementMap.put(k1 + "#" + k2, new MapEntry(s1, s2));
	}

	protected void addElementMapEntry(String k1, String k2, String s1,
			String s2, String s3) {
		fElementMap.put(k1 + "#" + k2, new MapEntry(s1, s2, s3));
	}

	/**
	 * Tries to find a MapEntry that defines the mapping of a type to its
	 * xmlElement, based upon the given encoding rule or any rules it extends.
	 *
	 * @param k1
	 *               type name
	 * @param k2
	 *               encoding rule name
	 * @return MapEntry with rule=("direct"), p1=xmlElement - or
	 *         <code>null</code> if no such map entry was found
	 */
	public MapEntry elementMapEntry(String k1, String k2) {
		String rule = k2;
		MapEntry me = null;
		while (me == null && rule != null) {
			me = fElementMap.get(k1 + "#" + rule);
			rule = extendsEncRule(rule);
		}
		return me;
	}

	protected void addAttributeMapEntry(String k1, String k2, String s1) {
		fAttributeMap.put(k1 + "#" + k2, new MapEntry(s1));
	}

	public MapEntry attributeMapEntry(String k1, String k2) {
		String rule = k2;
		MapEntry me = null;
		while (me == null && rule != null) {
			me = fAttributeMap.get(k1 + "#" + rule);
			rule = extendsEncRule(rule);
		}
		return me;
	}

	protected void addAttributeGroupMapEntry(String k1, String k2, String s1) {
		fAttributeGroupMap.put(k1 + "#" + k2, new MapEntry(s1));
	}

	public MapEntry attributeGroupMapEntry(String k1, String k2) {
		String rule = k2;
		MapEntry me = null;
		while (me == null && rule != null) {
			me = fAttributeGroupMap.get(k1 + "#" + rule);
			rule = extendsEncRule(rule);
		}
		return me;
	}

	protected void addTarget(String k1, ProcessMode k2) {
		fTargets.put(k1, k2);
	}

	public Vector<String> targets() {
		Vector<String> res = new Vector<String>();
		for (String t : fTargets.keySet()) {
			res.add(t);
		}
		return res;
	}

	public ProcessMode targetMode(String targetClassName) {
		ProcessMode processMode;
		if (targetClassName == null) {
			processMode = ProcessMode.disabled;
		} else if (fTargets.get(targetClassName) == null) {
			processMode = ProcessMode.disabled;
		} else {
			processMode = fTargets.get(targetClassName);
		}
		return processMode;
	}

	public ProcessMode setTargetMode(String tn, ProcessMode mode) {
		return fTargets.put(tn, mode);
	}

	/**
	 * @param k1
	 * @return the value of the parameter with the given name, or
	 *         <code>null</code> if the parameter does not exist
	 */
	public String parameter(String k1) {
		return fParameters.get(k1);
	}

	/**
	 * @param t
	 *               class name; can be <code>null</code> to search for an input
	 *               parameter
	 * @param k1
	 *               parameter name
	 * @return the parameter value, or <code>null</code> if no value was found
	 */
	public String parameter(String t, String k1) {
		if (t == null) {
			return fParameters.get(k1);
		} else {
			return fParameters.get(t + "::" + k1);
		}
	}

	/**
	 * @param className
	 *                          Fully qualified name of the target class for
	 *                          which the existence of the parameter with given
	 *                          name shall be determined; can be
	 *                          <code>null</code> to check the existence of an
	 *                          input parameter
	 * @param parameterName
	 *                          name of the parameter to check
	 * @return <code>true</code> if the parameter exists (i.e., is set in the
	 *         configuration), else <code>false</code>
	 */
	public boolean hasParameter(String className, String parameterName) {

		String key;
		if (className == null) {
			key = parameterName;
		} else {
			key = className + "::" + parameterName;
		}

		return this.fParameters.containsKey(key);
	}

	/**
	 * @param className
	 *                                            Fully qualified name of the
	 *                                            target class for which the
	 *                                            values of the parameter with
	 *                                            given name shall be searched;
	 *                                            can be <code>null</code> to
	 *                                            search for an input parameter
	 * @param parameterName
	 *                                            name of the parameter to
	 *                                            retrieve the value from
	 * @param defaultValue
	 *                                            value that will be returned if
	 *                                            no valid value was found;
	 *                                            NOTE: <code>null</code> is NOT
	 *                                            converted
	 * @param allowNonEmptyTrimmedStringValue
	 *                                            <code>true</code> if the
	 *                                            parameter value may be empty
	 *                                            if it was trimmed, else
	 *                                            <code>false</code>
	 * @param trimValue
	 *                                            <code>true</code> if leading
	 *                                            and trailing whitespace shall
	 *                                            be removed from the parameter
	 *                                            value
	 * @return Value retrieved from this parameter, or the default value if the
	 *         parameter was not set or did not contain a valid value; can be
	 *         <code>null</code>
	 */
	public String parameterAsString(String className, String parameterName,
			String defaultValue, boolean allowNonEmptyTrimmedStringValue,
			boolean trimValue) {

		String result = this.parameter(className, parameterName);

		if (result == null || (result.trim().isEmpty()
				&& !allowNonEmptyTrimmedStringValue)) {

			result = defaultValue;

		} else if (trimValue) {
			result = result.trim();
		}

		return result;
	}

	/**
	 * @param className
	 *                             Fully qualified name of the target class for
	 *                             which the values of the parameter with given
	 *                             name shall be searched; can be
	 *                             <code>null</code> to search for an input
	 *                             parameter
	 * @param parameterName
	 *                             name of the parameter to retrieve the comma
	 *                             separated values from
	 * @param defaultValues
	 *                             values that will be returned if no values
	 *                             were found; <code>null</code> is converted to
	 *                             an empty list of strings
	 * @param omitEmptyStrings
	 *                             <code>true</code> if values may NOT be empty
	 *                             if they were trimmed, else <code>false</code>
	 * @param trimResults
	 *                             <code>true</code> if leading and trailing
	 *                             whitespace shall be removed from a value
	 * @return List of values (originally separated by commas) retrieved from
	 *         this parameter, or the default values if the parameter was not
	 *         set or did not contain valid values; can be empty but not
	 *         <code>null</code>
	 */
	public List<String> parameterAsStringList(String className,
			String parameterName, String[] defaultValues,
			boolean omitEmptyStrings, boolean trimResults) {

		List<String> defaultValuesList = defaultValues == null
				? new ArrayList<String>()
				: Arrays.asList(defaultValues);

		String paramValue = this.parameter(className, parameterName);

		if (paramValue == null) {

			return defaultValuesList;

		} else {

			Splitter splitter = Splitter.on(',');

			if (omitEmptyStrings) {
				splitter = splitter.omitEmptyStrings();
			}
			if (trimResults) {
				splitter = splitter.trimResults();
			}

			List<String> result = splitter.splitToList(paramValue);

			if (result.isEmpty()) {

				return defaultValuesList;

			} else {

				return new ArrayList<String>(result);
			}
		}
	}

	/**
	 * @param className
	 *                          Fully qualified name of the target class for
	 *                          which the value of the parameter with given name
	 *                          shall be searched; can be <code>null</code> to
	 *                          search for an input parameter
	 * @param parameterName
	 *                          name of the parameter to retrieve the value from
	 * @param defaultValue
	 *                          value that will be returned if no valid value
	 *                          was found (i.e., if the parameter value could
	 *                          not be parsed to an int)
	 * @return Value retrieved from this parameter, or the default value if the
	 *         parameter was not set or did not contain a valid value.
	 */
	public int parameterAsInteger(String className, String parameterName,
			int defaultValue) {

		int res;

		String valueByConfig = this.parameter(className, parameterName);

		if (valueByConfig == null) {

			res = defaultValue;

		} else {

			try {
				res = Integer.parseInt(valueByConfig);

			} catch (NumberFormatException e) {

				/*
				 * Options does not have a ShapeChangeResult - check validity of
				 * configuration parameters through target specific validation
				 * routine
				 */
				res = defaultValue;
			}
		}

		return res;
	}

	/**
	 * @param className
	 *                          Fully qualified name of the target class for
	 *                          which the boolean value of the parameter with
	 *                          given name shall be searched; can be
	 *                          <code>null</code> to search for an input
	 *                          parameter
	 * @param parameterName
	 *                          name of the parameter to retrieve the boolean
	 *                          value from
	 * @param defaultValue
	 *                          value that will be returned if no valid value
	 *                          was found (i.e., if the parameter value could
	 *                          not be parsed to a boolean)
	 * @return Value retrieved from this parameter (<code>true</code> if the
	 *         parameter was set and is equal, ignoring case, to the string
	 *         "true"), or the default value if the parameter was not set or did
	 *         not contain a valid value.
	 */
	public boolean parameterAsBoolean(String className, String parameterName,
			boolean defaultValue) {

		boolean res;

		String valueByConfig = this.parameter(className, parameterName);

		if (valueByConfig == null) {
			res = defaultValue;
		} else {
			res = Boolean.parseBoolean(valueByConfig.trim());
		}

		return res;
	}

	/** This returns the names of all parms whose names match a regex pattern */
	public String[] parameterNamesByRegex(String t, String regex) {
		HashSet<String> pnames = new HashSet<String>();
		int lt2 = t.length() + 2;
		for (Entry<String, String> e : fParameters.entrySet()) {
			String key = e.getKey();
			if (key.startsWith(t + "::")) {
				if (Pattern.matches(regex, key.substring(lt2)))
					pnames.add(key.substring(lt2));
			}
		}
		return pnames.toArray(new String[0]);
	}

	public void setParameter(String k1, String s1) {
		String s = replaceValue(s1);
		if (s != null)
			fParameters.put(k1, s);
		else
			fParameters.put(k1, s1);
	}

	public void setParameter(String t, String k1, String s1) {
		String s = replaceValue(s1);
		if (s != null)
			fParameters.put(t + "::" + k1, s);
		else
			fParameters.put(t + "::" + k1, s1);
	}

	public String replaceValue(String k1) {
		return fReplace.get(k1);
	}

	public void setReplaceValue(String k1, String s1) {
		fReplace.put(k1, s1);
	}

	/**
	 * Adds a stereotype alias mapping.
	 *
	 * @param alias
	 *                      - the stereotype alias (in lower case)
	 * @param wellknown
	 *                      - the wellknown stereotype (in lower case) to which
	 *                      the alias maps
	 */
	protected void addStereotypeAlias(String alias, String wellknown) {
		fStereotypeAliases.put(alias, wellknown);
	}

	/**
	 * Retrieves the wellknown stereotype to which the given alias maps, or
	 * <code>null</code> if no such mapping exists. The alias will automatically
	 * be converte to lower case to look up the mapping (the according key
	 * values in the stereotype map have also been converted to lower case).
	 *
	 * @param alias
	 *                  stereotype for which a mapping to a wellknown stereotype
	 *                  is being looked up
	 * @return the wellknown stereotype to which the alias maps, or
	 *         <code>null</code> if no such mapping exists
	 */
	public String stereotypeAlias(String alias) {
		return fStereotypeAliases.get(alias.toLowerCase());
	}

	/**
	 * Adds a tag alias mapping.
	 * 
	 * @param alias
	 *                      - the tag alias (will be converted to lower case)
	 * @param wellknown
	 *                      - the tag to which the alias maps
	 */
	protected void addTagAlias(String alias, String wellknown) {
		fTagAliases.put(alias.toLowerCase(Locale.ENGLISH), wellknown);
	}

	/**
	 * Retrieves the wellknown tag to which the given alias maps, or
	 * <code>null</code> if no such mapping exists. The alias will automatically
	 * be converted to lower case to look up the mapping.
	 * 
	 * @param alias
	 *                  tag for which a mapping to a wellknown tag is being
	 *                  looked up
	 * @return the wellknown tag to which the alias maps, or <code>null</code>
	 *         if no such mapping exists
	 */
	public String tagAlias(String alias) {
		return fTagAliases.get(alias.toLowerCase(Locale.ENGLISH));
	}

	/**
	 * Adds a descriptor-source mapping.
	 * 
	 * @param descriptor
	 *                       - the descriptor (in lower case)
	 * @param source
	 *                       - the source (in lower case)
	 */
	protected void addDescriptorSource(String descriptor, String source) {
		fDescriptorSources.put(descriptor, source);
	}

	/**
	 * Retrieves the wellknown stereotype to which the given alias maps, or
	 * <code>null</code> if no such mapping exists. The alias will automatically
	 * be converte to lower case to look up the mapping (the according key
	 * values in the stereotype map have also been converted to lower case).
	 * 
	 * @param descriptor
	 *                       - the descriptor (in lower case)
	 * @return the source where the descriptor is represented in the model or
	 *         <code>null</code> if no such mapping exists
	 */
	public String descriptorSource(String descriptor) {
		return fDescriptorSources.get(descriptor.toLowerCase());
	}

	/**
	 * @param k1
	 *               namespace abbreviation / prefix
	 * @param s1
	 *               namespace
	 * @param s2
	 *               location
	 */
	protected void addNamespace(String k1, String s1, String s2) {
		fNamespaces.put(k1, new MapEntry(s1, s2));
	}

	protected void addRule(String rule) {
		fAllRules.add(rule.toLowerCase());
	}

	public boolean hasRule(String rule) {
		return fAllRules.contains(rule.toLowerCase());
	}

	protected void addRule(String rule, String encRule) {
		fRulesInEncRule.add(rule.toLowerCase() + "#" + encRule.toLowerCase());
	}

	public boolean hasRule(String rule, String encRule) {
		boolean res = false;
		while (!res && encRule != null) {
			res = fRulesInEncRule
					.contains(rule.toLowerCase() + "#" + encRule.toLowerCase());
			encRule = extendsEncRule(encRule);
		}
		return res;
	}

	/**
	 * Identify if the given encRule is or extends (directly or indirectly) the
	 * given baseRule. When comparing encoding rule names, case is ignored.
	 * 
	 * @param encRule
	 * @param baseRule
	 * @return <code>true</code> if encRule is or extends (directly or
	 *         indirectly) baseRule, else <code>false</code>
	 */
	public boolean matchesEncRule(String encRule, String baseRule) {
		while (encRule != null) {
			if (encRule.equalsIgnoreCase(baseRule))
				return true;
			encRule = extendsEncRule(encRule);
		}
		return false;
	}

	protected void addExtendsEncRule(String rule1, String rule2) {
		fExtendsEncRule.put(rule1.toLowerCase(), rule2.toLowerCase());
	}

	protected String extendsEncRule(String rule1) {
		return fExtendsEncRule.get(rule1.toLowerCase());
	}

	public boolean encRuleExists(String encRule) {
		if ("*".equals(encRule)) {
			return true;
		} else {
			return fExtendsEncRule.containsKey(encRule);
		}
	}

	protected void addPackage(String k1, String s1, String s2, String s3,
			String s4) {
		fPackages.put(k1, new MapEntry(s1, s2, s3, s4));
	}

	/**
	 * @param k1
	 *               namespace
	 * @param s1
	 *               location
	 */
	public void addSchemaLocation(String k1, String s1) {
		// This will overwrite any previously existing value for the given key
		// k1. Order of schema location additions thus is important.
		fSchemaLocations.put(k1, s1);
	}

	/**
	 * @param k1
	 *               nsabr (namespace abbreviation / prefix)
	 * @return the MapEntry belonging to the nsabr (with namespace as 'rule' and
	 *         location as 'p1') - or <code>null</code> if the nsabr is unknown
	 */
	protected MapEntry namespace(String k1) {
		MapEntry me = fNamespaces.get(k1);
		return me;
	}

	/**
	 * Gets the namespace abbreviation / prefix for a given namespace as
	 * declared via the configuration, or <code>null</code> if the configuration
	 * does not contain information about the namespace.
	 *
	 * @param ns
	 * @return
	 */
	public String nsabrForNamespace(String ns) {

		for (String nsabr : fNamespaces.keySet()) {
			MapEntry me = fNamespaces.get(nsabr);
			if (me.rule.equals(ns)) {
				return nsabr;
			}
		}
		return null;
	}

	/**
	 * @param k1
	 *               nsabr (namespace abbreviation / prefix)
	 * @return the full namespace, can be <code>null</code> if none was found
	 *         for the given nsabr
	 */
	public String fullNamespace(String k1) {
		MapEntry me = fNamespaces.get(k1);
		if (me != null) {
			return me.rule;
		}
		return null;
	}

	public String nsOfPackage(String k1) {
		MapEntry me = fPackages.get(k1);
		if (me != null) {
			return me.rule;
		}
		return null;
	}

	/**
	 * @param packageName
	 *                        name of a package
	 * @return namespace abbreviation (nsabr) defined by a package configuration
	 *         entry for a package with given name; can be <code>null</code> if
	 *         no such entry exists
	 */
	public String nsabrOfPackage(String packageName) {
		MapEntry me = fPackages.get(packageName);
		if (me != null) {
			return me.p1;
		}
		return null;
	}

	public String xsdOfPackage(String k1) {
		MapEntry me = fPackages.get(k1);
		if (me != null) {
			return me.p2;
		}
		return null;
	}

	public String versionOfPackage(String k1) {
		MapEntry me = fPackages.get(k1);
		if (me != null) {
			return me.p3;
		}
		return null;
	}

	/**
	 * @param k1
	 *               - namespace
	 * @return schema location, if defined, else <code>null</code>
	 */
	public String schemaLocationOfNamespace(String k1) {
		String loc = fSchemaLocations.get(k1);
		/*
		 * note, schema location may be omitted / null; example: DGIWG spatial
		 * profile is not available online
		 */
		return loc;
	}

	public String categoryName(int category) {

		switch (category) {

		case Options.AIXMEXTENSION:
			return "aixmextension";
		case Options.UNKNOWN:
			return "unknown";
		case Options.FEATURE:
			return "feature";
		case Options.CODELIST:
			return "codelist";
		case Options.ENUMERATION:
			return "enumeration";
		case Options.MIXIN:
			return "mixin";
		case Options.DATATYPE:
			return "datatype";
		case Options.OBJECT:
			return "object";
		case Options.BASICTYPE:
			return "basic type";
		case Options.UNION:
			return "union";
		case Options.OKSTRAKEY:
			return "okstra key";
		case Options.OKSTRAFID:
			return "okstra fid";
		case Options.FEATURECONCEPT:
			return "feature concept";
		case Options.ATTRIBUTECONCEPT:
			return "attribute concept";
		case Options.VALUECONCEPT:
			return "value concept";
		case Options.ROLECONCEPT:
			return "role concept";
		default:
			return "unknown category";
		}
	}

	public void loadConfiguration() throws ShapeChangeAbortException {

		InputStream configStream = null;

		if (configFile == null) {

			// load minimal configuration, if no configuration file has been
			// provided
			String minConfigFile = "/config/minimal.xml";
			configStream = getClass().getResourceAsStream(minConfigFile);
			if (configStream == null) {
				minConfigFile = "src/main/resources" + minConfigFile;
				File file = new File(minConfigFile);
				if (file.exists())
					try {
						configStream = new FileInputStream(file);
					} catch (FileNotFoundException e1) {
						throw new ShapeChangeAbortException(
								"Minimal configuration file not found: "
										+ minConfigFile);
					}
				else {
					URL url;
					String configURL = "http://shapechange.net/resources/config/minimal.xml";
					try {
						url = new URL(configURL);
						configStream = url.openStream();
					} catch (MalformedURLException e) {
						throw new ShapeChangeAbortException(
								"Minimal configuration file not accessible from: "
										+ configURL + " (malformed URL)");
					} catch (IOException e) {
						throw new ShapeChangeAbortException(
								"Minimal configuration file not accessible from: "
										+ configURL + " (IO error)");
					}
				}
			}
		} else {
			File file = new File(configFile);
			if (file == null || !file.exists()) {
				try {
					configStream = (new URL(configFile)).openStream();
				} catch (MalformedURLException e) {
					throw new ShapeChangeAbortException(
							"No configuration file found at " + configFile
									+ " (malformed URL)");
				} catch (IOException e) {
					throw new ShapeChangeAbortException(
							"No configuration file found at " + configFile
									+ " (IO exception)");
				}
			} else {
				try {
					configStream = new FileInputStream(file);
				} catch (FileNotFoundException e) {
					throw new ShapeChangeAbortException(
							"No configuration file found at " + configFile);
				}
			}
			if (configStream == null) {
				throw new ShapeChangeAbortException(
						"No configuration file found at " + configFile);
			}
		}

		DocumentBuilder builder = null;
		ShapeChangeErrorHandler handler = null;
		try {
			System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
					"org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setNamespaceAware(true);
			factory.setValidating(true);
			factory.setFeature(
					"http://apache.org/xml/features/validation/schema", true);
			factory.setIgnoringElementContentWhitespace(true);
			factory.setIgnoringComments(true);
			factory.setXIncludeAware(true);
			factory.setFeature(
					"http://apache.org/xml/features/xinclude/fixup-base-uris",
					false);
			builder = factory.newDocumentBuilder();
			handler = new ShapeChangeErrorHandler();
			builder.setErrorHandler(handler);
		} catch (FactoryConfigurationError e) {
			throw new ShapeChangeAbortException(
					"Unable to get a document builder factory.");
		} catch (ParserConfigurationException e) {
			throw new ShapeChangeAbortException(
					"XML Parser was unable to be configured.");
		}

		// parse file
		try {
			Document document = builder.parse(configStream);
			if (handler.errorsFound()) {
				throw new ShapeChangeAbortException(
						"Invalid configuration file.");
			}

			/*
			 * 2018-01-31 JE: uncomment the following to print the configuration
			 * as loaded by ShapeChange, with xincludes resolved.
			 */
			// try {
			// TransformerFactory tf = TransformerFactory.newInstance();
			// Transformer transformer = tf.newTransformer();
			// transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,
			// "no");
			// transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			// transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			// transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			// transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount",
			// "4");
			//
			// transformer.transform(new DOMSource(document),
			// new StreamResult(new OutputStreamWriter(System.out, "UTF-8")));
			// } catch (Exception e) {
			// e.printStackTrace();
			// }

			// parse input element specific content
			NodeList nl = document.getElementsByTagName("input");
			Element inputElement = (Element) nl.item(0);
			if (inputElement.hasAttribute("id")) {
				inputId = inputElement.getAttribute("id").trim();
				if (inputId.length() == 0) {
					inputId = null;
				}
			} else {
				inputId = Options.INPUTELEMENTID;
			}

			Map<String, String> inputParameters = new HashMap<String, String>();
			nl = inputElement.getElementsByTagName("parameter");
			for (int j = 0; j < nl.getLength(); j++) {
				Element e = (Element) nl.item(j);
				String key = e.getAttribute("name");
				String val = e.getAttribute("value");
				inputParameters.put(key, val);
			}

			Map<String, String> stereotypeAliases = new HashMap<String, String>();
			nl = inputElement.getElementsByTagName("StereotypeAlias");
			for (int j = 0; j < nl.getLength(); j++) {
				Element e = (Element) nl.item(j);
				String key = e.getAttribute("alias");
				String val = e.getAttribute("wellknown");

				// case shall be ignored
				key = key.toLowerCase();
				val = val.toLowerCase();

				stereotypeAliases.put(key, val);
			}

			Map<String, String> tagAliases = new HashMap<String, String>();
			nl = inputElement.getElementsByTagName("TagAlias");
			for (int j = 0; j < nl.getLength(); j++) {
				Element e = (Element) nl.item(j);
				String key = e.getAttribute("alias");
				String val = e.getAttribute("wellknown");

				// case not to be ignored for tagged values at the moment
				// key = key.toLowerCase();
				// val = val.toLowerCase();

				tagAliases.put(key, val);
			}

			Map<String, String> descriptorSources = new HashMap<String, String>();
			nl = inputElement.getElementsByTagName("DescriptorSource");
			for (int j = 0; j < nl.getLength(); j++) {
				Element e = (Element) nl.item(j);
				String key = e.getAttribute("descriptor");
				String val = e.getAttribute("source");

				// case shall be ignored for descriptor and source
				key = key.toLowerCase();
				val = val.toLowerCase();

				if (val.equals("sc:extract")) {
					String s = e.getAttribute("token");
					val += "#" + (s == null ? "" : s);
				} else if (val.equals("tag")) {
					String s = e.getAttribute("tag");
					val += "#" + (s == null ? "" : s);
				}

				descriptorSources.put(key, val);
			}

			Map<String, PackageInfoConfiguration> packageInfos = parsePackageInfos(
					inputElement);

			this.inputConfig = new InputConfiguration(inputId, inputParameters,
					stereotypeAliases, tagAliases, descriptorSources,
					packageInfos);

			// parse dialog specific parameters
			nl = document.getElementsByTagName("dialog");
			if (nl != null && nl.getLength() != 0) {
				for (int k = 0; k < nl.getLength(); k++) {
					Node node = nl.item(k);
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						Element dialogElement = (Element) node;
						this.dialogParameters = parseParameters(dialogElement,
								"parameter");
					}
				}
			}

			// parse log specific parameters
			nl = document.getElementsByTagName("log");
			if (nl != null && nl.getLength() != 0) {
				for (int k = 0; k < nl.getLength(); k++) {
					Node node = nl.item(k);
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						Element logElement = (Element) node;
						this.logParameters = parseParameters(logElement,
								"parameter");
					}
				}
			}

			// add standard rules, just so that configured rules can be
			// validated
			addStandardRules();

			// Load transformer configurations (if any are provided in the
			// configuration file)
			this.transformerConfigs = parseTransformerConfigurations(document);

			// Load target configurations
			this.targetConfigs = parseTargetConfigurations(document);

			this.resetFields();

			// TBD discuss if there's a better way to support rule and
			// requirements matching for the input model
			// TBD apparently the matching requires all
			// applicable encoding rules to be known up front
			for (TargetConfiguration tgtConfig : targetConfigs) {

				// System.out.println(tgtConfig);
				String className = tgtConfig.getClassName();
				ProcessMode mode = tgtConfig.getProcessMode();

				// set targets and their mode; if a target occurs multiple
				// times, keep the enabled one(s)
				if (!this.fTargets.containsKey(className)) {
					addTarget(className, mode);

				} else {
					if (this.fTargets.get(className)
							.equals(ProcessMode.disabled)) {
						// set targets and their mode; if a target occurs
						// multiple times, keep the enabled one(s)
						addTarget(className, mode);
					}
				}

				/*
				 * ensure that we have all the rules from all non-disabled
				 * targets we repeat this for the same target (if it is not
				 * disabled) to ensure that we get the union of all encoding
				 * rules
				 */
				if (!tgtConfig.getProcessMode().equals(ProcessMode.disabled)) {
					for (ProcessRuleSet prs : tgtConfig.getRuleSets()
							.values()) {
						String nam = prs.getName();
						String ext = prs.getExtendedRuleSetName();
						addExtendsEncRule(nam, ext);

						if (prs.hasAdditionalRules()) {
							for (String rule : prs.getAdditionalRules()) {
								addRule(rule, nam);
							}
						}
					}
				}

				/*
				 * looks like we also need parameters like defaultEncodingRule
				 * !!! IF THERE ARE DIFFERENT DEFAULT ENCODING RULES FOR
				 * DIFFERENT TARGETS (WITH SAME CLASS) THIS WONT WORK!!!
				 */
				for (String paramName : tgtConfig.getParameters().keySet()) {
					setParameter(className, paramName,
							tgtConfig.getParameters().get(paramName));
				}

				// in order for the input model load not to produce warnings,
				// we also need to load the map entries
				if (tgtConfig instanceof TargetXmlSchemaConfiguration) {

					TargetXmlSchemaConfiguration config = (TargetXmlSchemaConfiguration) tgtConfig;

					// add xml schema namespace information
					for (XmlNamespace xns : config.getXmlNamespaces()) {
						addNamespace(xns.getNsabr(), xns.getNs(),
								xns.getLocation());
						// if (xns.getLocation() != null) {
						addSchemaLocation(xns.getNs(), xns.getLocation());
						// }
					}

					// add xsd map entries
					addXsdMapEntries(config.getXsdMapEntries());

				} else {

					// add map entries for Target (no need to do this for
					// transformers)
					for (ProcessMapEntry pme : tgtConfig.getMapEntries()) {
						addTargetTypeMapEntry(pme);
					}
				}
			}

			// create "tree"
			for (TargetConfiguration tgtConfig : targetConfigs) {
				for (String inputIdref : tgtConfig.getInputIds()) {
					if (inputIdref.equals(getInputId())) {
						this.inputTargetConfigs.add(tgtConfig);
					} else {
						this.transformerConfigs.get(inputIdref)
								.addTarget(tgtConfig);
					}
				}
			}

			for (TransformerConfiguration trfConfig : this.transformerConfigs
					.values()) {
				String inputIdref = trfConfig.getInputId();
				if (inputIdref.equals(getInputId())) {
					this.inputTransformerConfigs.add(trfConfig);
				} else {
					this.transformerConfigs.get(inputIdref)
							.addTransformer(trfConfig);
				}
			}

			// Determine constraint creation handling parameters:
			String classTypesToCreateConstraintsFor = parameter(
					"classTypesToCreateConstraintsFor");
			if (classTypesToCreateConstraintsFor != null) {
				classTypesToCreateConstraintsFor = classTypesToCreateConstraintsFor
						.trim();
				if (classTypesToCreateConstraintsFor.length() > 0) {
					String[] stereotypes = classTypesToCreateConstraintsFor
							.split("\\W*,\\W*");
					this.classTypesToCreateConstraintsFor = new HashSet<Integer>();
					for (String stereotype : stereotypes) {
						String sForCons = stereotype.toLowerCase();
						if (sForCons.equals("enumeration")) {
							this.classTypesToCreateConstraintsFor
									.add(Integer.valueOf(ENUMERATION));
						} else if (sForCons.equals("codelist")) {
							this.classTypesToCreateConstraintsFor
									.add(Integer.valueOf(CODELIST));
						} else if (sForCons.equals("schluesseltabelle")) {
							this.classTypesToCreateConstraintsFor
									.add(Integer.valueOf(OKSTRAKEY));
						} else if (sForCons.equals("fachid")) {
							this.classTypesToCreateConstraintsFor
									.add(Integer.valueOf(OKSTRAFID));
						} else if (sForCons.equals("datatype")) {
							this.classTypesToCreateConstraintsFor
									.add(Integer.valueOf(DATATYPE));
						} else if (sForCons.equals("union")) {
							this.classTypesToCreateConstraintsFor
									.add(Integer.valueOf(UNION));
						} else if (sForCons.equals("featureconcept")) {
							this.classTypesToCreateConstraintsFor
									.add(Integer.valueOf(FEATURECONCEPT));
						} else if (sForCons.equals("attributeconcept")) {
							this.classTypesToCreateConstraintsFor
									.add(Integer.valueOf(ATTRIBUTECONCEPT));
						} else if (sForCons.equals("roleconcept")) {
							this.classTypesToCreateConstraintsFor
									.add(Integer.valueOf(ROLECONCEPT));
						} else if (sForCons.equals("valueconcept")) {
							this.classTypesToCreateConstraintsFor
									.add(Integer.valueOf(VALUECONCEPT));
						} else if (sForCons.equals("interface")) {
							this.classTypesToCreateConstraintsFor
									.add(Integer.valueOf(MIXIN));
						} else if (sForCons.equals("basictype")) {
							this.classTypesToCreateConstraintsFor
									.add(Integer.valueOf(BASICTYPE));
						} else if (sForCons.equals("adeelement")) {
							this.classTypesToCreateConstraintsFor
									.add(Integer.valueOf(FEATURE));
						} else if (sForCons.equals("featuretype")) {
							this.classTypesToCreateConstraintsFor
									.add(Integer.valueOf(FEATURE));
						} else if (sForCons.equals("type")) {
							this.classTypesToCreateConstraintsFor
									.add(Integer.valueOf(OBJECT));
						} else {
							this.classTypesToCreateConstraintsFor
									.add(Integer.valueOf(UNKNOWN));
						}
					}
				}
			}

			String constraintCreationForProperties = parameter(
					"constraintCreationForProperties");
			if (constraintCreationForProperties != null) {
				if (constraintCreationForProperties.trim()
						.equalsIgnoreCase("false")) {
					this.constraintCreationForProperties = false;
				}
			}

			String ignoreEncodingRuleTaggedValues = parameter(
					PARAM_IGNORE_ENCODING_RULE_TVS);

			if (ignoreEncodingRuleTaggedValues != null) {
				if (ignoreEncodingRuleTaggedValues.trim()
						.equalsIgnoreCase("true")) {
					this.ignoreEncodingRuleTaggedValues = true;
				}
			}

			String useStringInterning_value = parameter(
					PARAM_USE_STRING_INTERNING);

			if (useStringInterning_value != null && useStringInterning_value
					.trim().equalsIgnoreCase("true")) {
				this.useStringInterning = true;
			}

			String dontConstructAssociationNames_value = parameter(
					PARAM_DONT_CONSTRUCT_ASSOCIATION_NAMES);

			if (dontConstructAssociationNames_value != null
					&& dontConstructAssociationNames_value.trim()
							.equalsIgnoreCase("true")) {
				this.dontConstructAssociationNames = true;
			}

			String addTaggedValues_value = parameter("addTaggedValues");

			if (addTaggedValues_value != null
					&& addTaggedValues_value.trim().equals("*")) {
				this.allowAllTags = true;
			}
			
			String addStereotypes_value = parameter("addStereotypes");

			if (StringUtils.isNotBlank(addStereotypes_value)) {

			    if(addStereotypes_value.trim().equals("*")) {
				this.allowAllStereotypes = true;
			    } else {
				String[] as = StringUtils.split(addStereotypes_value, ",");
				for(String s : as) {
				    if(StringUtils.stripToNull(s) != null) {
					this.addedStereotypes.add(s.trim());
				    }
				}
			    }
			}
			

			String language_value = inputConfig.getParameters()
					.get(PARAM_LANGUAGE);

			if (language_value != null && !language_value.trim().isEmpty()) {
				this.language = language_value.trim().toLowerCase();
			}

		} catch (SAXException e) {
			String m = e.getMessage();
			if (m != null) {
				throw new ShapeChangeAbortException(
						"Error while loading configuration file: "
								+ System.getProperty("line.separator") + m);
			} else {
				e.printStackTrace(System.err);
				throw new ShapeChangeAbortException(
						"Error while loading configuration file: "
								+ System.getProperty("line.separator")
								+ System.err);
			}
		} catch (IOException e) {
			String m = e.getMessage();
			if (m != null) {
				throw new ShapeChangeAbortException(
						"Error while loading configuration file: "
								+ System.getProperty("line.separator") + m);
			} else {
				e.printStackTrace(System.err);
				throw new ShapeChangeAbortException(
						"Error while loading configuration file: "
								+ System.getProperty("line.separator")
								+ System.err);
			}
		}

		MapEntry nsme = namespace("gml");
		if (nsme != null) {
			GML_NS = nsme.rule;
		}
	}

	private void addXmlElementHasSimpleContent(String type,
			String xsdEncodingRule, Boolean xmlElementHasSimpleContent) {

		fXmlElementHasSimpleContentMap.put(type + "#" + xsdEncodingRule,
				xmlElementHasSimpleContent);

	}

	/**
	 * @param type
	 * @param xsdEncodingRule
	 * @return the boolean value of XsdMapEntry/@xmlElementHasSimpleContent, for
	 *         the map entry with the given type and encoding rule (or one of
	 *         the rules it extends); can be <code>null</code> if that attribute
	 *         was not set in the map entry or no map entry exists for the type
	 *         in the given encoding rule (or one of the rules it extends)
	 */
	public Boolean xmlElementHasSimpleContent(String type,
			String xsdEncodingRule) {
		String rule = xsdEncodingRule;
		Boolean val = null;
		while (val == null && rule != null) {
			val = fXmlElementHasSimpleContentMap.get(type + "#" + rule);
			rule = extendsEncRule(rule);
		}
		return val;
	}

	/**
	 * @param type
	 * @param xsdEncodingRule
	 * @return the boolean value of XsdMapEntry/@xmlReferenceable, for the map
	 *         entry with the given type and encoding rule (or one of the rules
	 *         it extends); can be <code>null</code> if that attribute was not
	 *         set in the map entry or no map entry exists for the type in the
	 *         given encoding rule (or one of the rules it extends)
	 */
	private void addXmlReferenceableMapEntry(String type,
			String xsdEncodingRule, Boolean xmlReferenceable) {

		fXmlReferenceableMap.put(type + "#" + xsdEncodingRule,
				xmlReferenceable);
	}

	public Boolean xmlReferenceable(String type, String xsdEncodingRule) {
		String rule = xsdEncodingRule;
		Boolean val = null;
		while (val == null && rule != null) {
			val = fXmlReferenceableMap.get(type + "#" + rule);
			rule = extendsEncRule(rule);
		}
		return val;
	}

	/**
	 *
	 */
	public void resetFields() {

		// reset fields

		fParameters = new HashMap<String, String>();
		fTypeMap = new HashMap<String, MapEntry>();
		fBaseMap = new HashMap<String, MapEntry>();
		fElementMap = new HashMap<String, MapEntry>();
		fAttributeMap = new HashMap<String, MapEntry>();
		fAttributeGroupMap = new HashMap<String, MapEntry>();
		targetMapEntryByTypeRuleKey = new HashMap<String, ProcessMapEntry>();
		fStereotypeAliases = new HashMap<String, String>();
		fTagAliases = new HashMap<String, String>();
		fDescriptorSources = new HashMap<String, String>();
		fNamespaces = new HashMap<String, MapEntry>();
		fPackages = new HashMap<String, MapEntry>();
		fSchemaLocations = new HashMap<String, String>();
		fAllRules = new HashSet<String>();
		fExtendsEncRule = new HashMap<String, String>();
		fRulesInEncRule = new HashSet<String>();

		// repopulate fields

		// set standard parameters first
		setStandardParameters();

		addStandardRules();

		// add all parameters from input
		for (String key : inputConfig.getParameters().keySet()) {
			setParameter(key, inputConfig.getParameters().get(key));
		}

		// add all stereotype aliases
		for (String key : inputConfig.getStereotypeAliases().keySet()) {
			addStereotypeAlias(key,
					inputConfig.getStereotypeAliases().get(key));
		}

		// add all tag aliases
		for (String key : inputConfig.getTagAliases().keySet()) {
			addTagAlias(key, inputConfig.getTagAliases().get(key));
		}

		// add all descriptor sources
		for (String key : inputConfig.getDescriptorSources().keySet()) {
			addDescriptorSource(key,
					inputConfig.getDescriptorSources().get(key));
		}

		// add all package infos
		for (PackageInfoConfiguration pic : inputConfig.getPackageInfos()
				.values()) {
			addPackage(pic.getPackageName(), pic.getNs(), pic.getNsabr(),
					pic.getXsdDocument(), pic.getVersion());
			// ensure that the package information is also added to the
			// schema location map
			addSchemaLocation(pic.getNs(), pic.getXsdDocument());
		}

		// add all dialog parameters
		if (dialogParameters != null) {
			for (String key : dialogParameters.keySet()) {
				setParameter(key, dialogParameters.get(key));
			}
		}

		// add all log parameters
		if (logParameters != null) {
			for (String key : logParameters.keySet()) {
				setParameter(key, logParameters.get(key));
			}
		}

		/*
		 * FIXME 2019-07-02 JE: Now we need to overwrite standard settings like
		 * defaultEncodingRule and rule sets defined by XmlSchema targets -
		 * unless the current process config is an XmlSchema target. That is
		 * important for rule matching throughout the processing chain of
		 * ShapeChange. This kind of target specific behavior is undesirable,
		 * but too intertwined with the current code in order to be easily
		 * fixed. Refactoring the code will probably lead to a new major version
		 * of ShapeChange. That version could also be written in such a way that
		 * new transformations and targets can be added without code changes to
		 * the core of ShapeChange.
		 */
		int countXmlSchemaConfigs = 0;
		for (TargetConfiguration tgtConfig : targetConfigs) {

			if (!tgtConfig.getProcessMode().equals(ProcessMode.disabled)
					&& tgtConfig instanceof TargetXmlSchemaConfiguration
					&& !(currentProcessConfig != null
							&& currentProcessConfig instanceof TargetXmlSchemaConfiguration)) {

				countXmlSchemaConfigs++;
				if (countXmlSchemaConfigs == 2) {

					/*
					 * FIXME develop/use common logging mechanism that is
					 * available to all ShapeChange code
					 */
					System.out.println(
							"Warning: Multiple non-disabled XmlSchema targets "
									+ "found in the ShapeChange configuration. If these "
									+ "targets have different encoding rules or different"
									+ " values for the target parameter defaultEncodingRule,"
									+ " that may lead to unexpected results.");
				}

				/*
				 * Ensure that we have all the rules defined by the XmlSchema
				 * target.
				 */
				for (ProcessRuleSet prs : tgtConfig.getRuleSets().values()) {
					String nam = prs.getName();
					String ext = prs.getExtendedRuleSetName();
					addExtendsEncRule(nam, ext);

					if (prs.hasAdditionalRules()) {
						for (String rule : prs.getAdditionalRules()) {
							addRule(rule, nam);
						}
					}
				}

				/*
				 * Also load parameter defaultEncodingRule, if set in the
				 * XmlSchema target configuration.
				 */
				if (tgtConfig.hasParameter("defaultEncodingRule")) {
					setParameter(tgtConfig.getClassName(),
							"defaultEncodingRule", tgtConfig.getParameters()
									.get("defaultEncodingRule"));
				}
			}
		}

		if (currentProcessConfig != null) {

			this.gmlVersion = currentProcessConfig.getGmlVersion();

			// no need to set all the fields for transformers, because they
			// retrieve them directly

			// initialise fields for common targets and xml schema targets
			if (currentProcessConfig instanceof TargetConfiguration) {

				/*
				 * add all parameters from current process configuration (only
				 * required for targets)
				 */
				for (String name : currentProcessConfig.getParameters()
						.keySet()) {
					setParameter(currentProcessConfig.getClassName(), name,
							currentProcessConfig.getParameters().get(name));
				}

				// add encoding rules
				for (ProcessRuleSet prs : currentProcessConfig.getRuleSets()
						.values()) {
					String nam = prs.getName();
					String ext = prs.getExtendedRuleSetName();
					addExtendsEncRule(nam, ext);
					if (prs.hasAdditionalRules()) {
						for (String rule : prs.getAdditionalRules()) {
							addRule(rule, nam);
						}
					}
				}
			}

			if (currentProcessConfig.getClassName()
					.equals(TargetFOL2SchematronClass)
					&& currentProcessConfig.getParameters()
							.get("defaultEncodingRule") != null) {
				/*
				 * The execution of the FOL2Schematron target only generates
				 * Schematron, not XML Schema. We want the derivation of
				 * Schematron code to take into account the default encoding
				 * rule that we configured in the FOL2Schematron target. Because
				 * Schematron generation depends on how the XML Schema of model
				 * elements looks like, we must take the XSD encoding into
				 * account. We have a standard value for the XML Schema default
				 * encoding rule, and encoding rule checks for 'xsd' depend upon
				 * that specific parameter. However, the parameter is bound to
				 * the XMLSchema target class name. Thus if we have a
				 * defaultEncodingRule set in the FOL2Schematron target we must
				 * overwrite that parameter for the XML Schema target.
				 */
				this.setParameter(TargetXmlSchemaClass, "defaultEncodingRule",
						currentProcessConfig.getParameters()
								.get("defaultEncodingRule"));
			}

			if (currentProcessConfig instanceof TargetXmlSchemaConfiguration) {

				TargetXmlSchemaConfiguration config = (TargetXmlSchemaConfiguration) currentProcessConfig;

				// add xml schema namespace information
				for (XmlNamespace xns : config.getXmlNamespaces()) {
					addNamespace(xns.getNsabr(), xns.getNs(),
							xns.getLocation());
					// if (xns.getLocation() != null) {
					addSchemaLocation(xns.getNs(), xns.getLocation());
					// }
				}

				// add xsd map entries
				addXsdMapEntries(config.getXsdMapEntries());

			} else {

				// add map entries for Target (no need to do this for
				// transformers)
				for (ProcessMapEntry pme : currentProcessConfig
						.getMapEntries()) {
					addTargetTypeMapEntry(pme);
				}

				// TODO store namespace info
			}
		}

		MapEntry nsme = namespace("gml");
		if (nsme != null) {
			GML_NS = nsme.rule;
		}

	}

	private void addXsdMapEntries(List<XsdMapEntry> xsdMapEntries) {

		for (XsdMapEntry xsdme : xsdMapEntries) {

			for (String xsdEncodingRule : xsdme.getEncodingRules()) {

				String type = xsdme.getType();
				String xmlPropertyType = xsdme.getXmlPropertyType();
				String xmlElement = xsdme.getXmlElement();
				String xmlTypeContent = xsdme.getXmlTypeContent();
				String xmlTypeNilReason = xsdme.getXmlTypeNilReason();
				String xmlType = xsdme.getXmlType();
				String xmlTypeType = xsdme.getXmlTypeType();
				String xmlAttribute = xsdme.getXmlAttribute();
				String xmlAttributeGroup = xsdme.getXmlAttributeGroup();
				Boolean xmlReferenceable = xsdme.getXmlReferenceable();
				Boolean xmlElementHasSimpleContent = xsdme
						.getXmlElementHasSimpleContent();

				if (xmlPropertyType != null) {
					if (xmlPropertyType.equals("_P_") && xmlElement != null) {
						addTypeMapEntry(type, xsdEncodingRule, "propertyType",
								xmlElement);
					} else if (xmlPropertyType.equals("_MP_")
							&& xmlElement != null) {
						addTypeMapEntry(type, xsdEncodingRule,
								"metadataPropertyType", xmlElement);
					} else {
						addTypeMapEntry(type, xsdEncodingRule, "direct",
								xmlPropertyType,
								xmlTypeType + "/" + xmlTypeContent,
								xmlTypeNilReason);
					}
				}
				if (xmlElement != null) {
					addElementMapEntry(type, xsdEncodingRule, "direct",
							xmlElement);
				}
				if (xmlType != null) {
					addBaseMapEntry(type, xsdEncodingRule, "direct", xmlType,
							xmlTypeType + "/" + xmlTypeContent);
				}
				if (xmlAttribute != null) {
					addAttributeMapEntry(type, xsdEncodingRule, xmlAttribute);
				}
				if (xmlAttributeGroup != null) {
					addAttributeGroupMapEntry(type, xsdEncodingRule,
							xmlAttributeGroup);
				}
				if (xmlReferenceable != null) {
					addXmlReferenceableMapEntry(type, xsdEncodingRule,
							xmlReferenceable);
				}
				if (xmlElementHasSimpleContent != null) {
					addXmlElementHasSimpleContent(type, xsdEncodingRule,
							xmlElementHasSimpleContent);
				}
			}
		}
	}

	private void setStandardParameters() {
		setParameter("reportLevel", "INFO");
		setParameter("xsltFile", "src/main/resources/xslt/result.xsl");
		setParameter("appSchemaName", "");
		setParameter("appSchemaNameRegex", "");
		setParameter("appSchemaNamespaceRegex", "");
		setParameter("publicOnly", "true");
		setParameter("inputFile",
				"http://shapechange.net/resources/test/test.xmi");
		setParameter("inputModelType", "XMI10");
		setParameter("logFile", "log.xml");
		setParameter("representTaggedValues", "");
		setParameter("addTaggedValues", "");
		setParameter("extractSeparator", "--IMPROBABLE--DUMMY--SEPARATOR--");
		setParameter("definitionSeparator", "-- Definition --");
		setParameter("descriptionSeparator", "-- Description --");
		setParameter("nameSeparator", "-- Name --");
		setParameter("outputDirectory", SystemUtils.getUserDir().getPath());
		setParameter("sortedSchemaOutput", "true");
		setParameter("sortedOutput", "true");
		setParameter("oclConstraintTypeRegex", "(OCL|Invariant)");
		setParameter("folConstraintTypeRegex", "(SBVR)");
		setParameter(Options.TargetXmlSchemaClass, "defaultEncodingRule",
				Options.ISO19136_2007);
		setParameter(Options.TargetXmlSchemaClass, "gmlVersion", "3.2");
		setParameter(Options.TargetOWLISO19150Class, "defaultEncodingRule",
				Options.ISO19150_2014);
		setParameter(Options.TargetSQLClass, "defaultEncodingRule",
				Options.SQL);
	}

	private Map<String, PackageInfoConfiguration> parsePackageInfos(
			Element inputElement) {

		Map<String, PackageInfoConfiguration> result = new HashMap<String, PackageInfoConfiguration>();

		NodeList nl = inputElement.getElementsByTagName("PackageInfo");

		for (int j = 0; j < nl.getLength(); j++) {

			Element e = (Element) nl.item(j);

			String name = e.getAttribute("packageName");
			String nsabr = e.getAttribute("nsabr");
			String ns = e.getAttribute("ns");
			String xsdDocument = e.getAttribute("xsdDocument");
			String version = e.getAttribute("version");

			PackageInfoConfiguration pic = new PackageInfoConfiguration(name,
					nsabr, ns, xsdDocument, version);

			result.put(name, pic);
		}

		return result;
	}

	/**
	 * @param configurationDocument
	 * @return list with target configurations, can be empty but not
	 *         <code>null</code>
	 * @throws ShapeChangeAbortException
	 */
	private List<TargetConfiguration> parseTargetConfigurations(
			Document configurationDocument) throws ShapeChangeAbortException {

		List<TargetConfiguration> tgtConfigs = new ArrayList<TargetConfiguration>();

		NodeList tgtsNl = configurationDocument.getElementsByTagName("targets");

		for (int i = 0; i < tgtsNl.getLength(); i++) {
			Node tgtsN = tgtsNl.item(i);
			NodeList tgtNl = tgtsN.getChildNodes();

			// look for all Target/TargetXmlSchema elements in the "targets"
			// Node
			for (int j = 0; j < tgtNl.getLength(); j++) {
				Node tgtN = tgtNl.item(j);

				if (tgtN.getNodeType() == Node.ELEMENT_NODE) {
					Element tgtE = (Element) tgtN;

					// parse content of target element

					// get name of config element for decision which type of
					// TargetConfiguration to instantiate later on
					String tgtType = tgtE.getNodeName();

					// get class name
					String tgtConfigName = tgtE.getAttribute("class");

					// get mode
					ProcessMode tgtMode = this.parseMode(tgtE);

					Map<String, String> processParameters = parseParameters(
							tgtE, "targetParameter");

					// now look up all ProcessRuleSet elements, if there are
					// any
					Map<String, ProcessRuleSet> processRuleSets = parseRuleSets(
							tgtE, "EncodingRule", true);

					// get the target inputs - can be null, then set it to
					// global input element
					Set<String> tgtConfigInputs;
					if (tgtE.hasAttribute("inputs")) {
						String[] inputs = tgtE.getAttribute("inputs")
								.split("\\s");
						tgtConfigInputs = new HashSet<String>(
								Arrays.asList(inputs));
					} else {
						tgtConfigInputs = new HashSet<String>();
						tgtConfigInputs.add(getInputId());
					}

					Element advancedProcessConfigurations = parseAdvancedProcessConfigurations(
							tgtE);

					TargetConfiguration tgtConfig;
					if (tgtType.equals("Target")) {
						// now look up all ProcessMapEntry elements, if there
						// are any
						List<ProcessMapEntry> processMapEntries = parseProcessMapEntries(
								tgtE, "MapEntry");

						// also parse namespaces, if there are any
						List<Namespace> namespaces = parseNamespaces(tgtE);

						// create target config and add it to list
						tgtConfig = new TargetConfiguration(tgtConfigName,
								tgtMode, processParameters, processRuleSets,
								processMapEntries, tgtConfigInputs, namespaces,
								advancedProcessConfigurations);

					} else if (tgtType.equals("TargetOwl")) {

						Map<String, List<RdfTypeMapEntry>> rdfTypeMapEntries = parseRdfTypeMapEntries(
								tgtE);
						Map<String, List<RdfPropertyMapEntry>> rdfPropertyMapEntries = parseRdfPropertyMapEntries(
								tgtE);
						SortedMap<String, List<StereotypeConversionParameter>> stereotypeConversionParameters = parseStereotypeConversionParameters(
								tgtE);
						Map<String, List<TypeConversionParameter>> typeConversionParameters = parseTypeConversionParameters(
								tgtE);
						Map<String, List<PropertyConversionParameter>> propertyConversionParameters = parsePropertyConversionParameters(
								tgtE);
						List<DescriptorTarget> descriptorTargets = parseDescriptorTargets(
								tgtE);
						Map<ConstraintMapping.ConstraintType, ConstraintMapping> constraintMappings = parseConstraintMappings(
								tgtE);

						List<RdfGeneralProperty> generalProperties = parseGeneralProperties(
								tgtE);

						List<Namespace> namespaces = parseNamespaces(tgtE);

						// create target owl config and add it to list
						TargetOwlConfiguration owlConfig = new TargetOwlConfiguration(
								tgtConfigName, tgtMode, processParameters,
								processRuleSets, tgtConfigInputs, namespaces,
								advancedProcessConfigurations,
								rdfTypeMapEntries, rdfPropertyMapEntries,
								stereotypeConversionParameters,
								typeConversionParameters,
								propertyConversionParameters, descriptorTargets,
								constraintMappings, generalProperties);

						owlConfig.validate();

						tgtConfig = owlConfig;

					} else {
						// We're dealing with a TargetXmlSchema element

						List<XsdMapEntry> xsdMapEntries = parseXsdMapEntries(
								tgtE);

						Map<String, List<XsdPropertyMapEntry>> xsdPropertyMapEntries = parseXsdPropertyMapEntries(
								tgtE);

						List<XmlNamespace> xmlNamespaces = parseXmlNamespaces(
								tgtE);

						tgtConfig = new TargetXmlSchemaConfiguration(
								tgtConfigName, tgtMode, processParameters,
								processRuleSets, null, xsdMapEntries,
								xsdPropertyMapEntries, xmlNamespaces,
								tgtConfigInputs, advancedProcessConfigurations);
					}
					tgtConfigs.add(tgtConfig);
				}
			}
		}
		return tgtConfigs;
	}

	/**
	 * @param targetElement
	 * @return map (can be empty but not <code>null</code>), with key: property
	 *         name (optionally scoped to a class), and value: list of map
	 *         entries for properties with that name (which can be more than
	 *         one, if schemas are different)
	 */
	private Map<String, List<XsdPropertyMapEntry>> parseXsdPropertyMapEntries(
			Element targetElement) {

		NodeList xpmeNl = targetElement
				.getElementsByTagName("XsdPropertyMapEntry");
		Node xpmeN;
		Element xpmeE;

		Map<String, List<XsdPropertyMapEntry>> result = new TreeMap<>();

		if (xpmeNl != null && xpmeNl.getLength() != 0) {

			for (int k = 0; k < xpmeNl.getLength(); k++) {

				xpmeN = xpmeNl.item(k);
				if (xpmeN.getNodeType() == Node.ELEMENT_NODE) {

					xpmeE = (Element) xpmeN;

					String property = xpmeE.getAttribute("property").trim();

					String schema = xpmeE.hasAttribute("schema")
							? xpmeE.getAttribute("schema").trim()
							: null;

					String target = xpmeE.hasAttribute("targetElement")
							? xpmeE.getAttribute("targetElement").trim()
							: null;
					if (StringUtils.isBlank(target)) {
						target = null;
					}

					XsdPropertyMapEntry xpme = new XsdPropertyMapEntry(property,
							schema, target);

					List<XsdPropertyMapEntry> list;
					if (result.containsKey(property)) {
						list = result.get(property);
					} else {
						list = new ArrayList<XsdPropertyMapEntry>();
						result.put(property, list);
					}
					list.add(xpme);
				}
			}
		}

		return result;
	}

	private List<RdfGeneralProperty> parseGeneralProperties(
			Element targetElement) {

		List<RdfGeneralProperty> result = new ArrayList<>();

		List<Element> gopEs = XMLUtil.getChildElements(targetElement,
				"GeneralObjectProperty");

		for (Element gopE : gopEs) {
			GeneralObjectProperty gop = new GeneralObjectProperty(gopE);
			result.add(gop);
		}

		List<Element> gdpEs = XMLUtil.getChildElements(targetElement,
				"GeneralDataProperty");

		for (Element gdpE : gdpEs) {
			GeneralDataProperty gdp = new GeneralDataProperty(gdpE);
			result.add(gdp);
		}

		return result;
	}

	private Element parseAdvancedProcessConfigurations(Element processElement) {

		NodeList apcNl = processElement
				.getElementsByTagName("advancedProcessConfigurations");
		Node apcN;
		Element apcE;

		if (apcNl != null && apcNl.getLength() != 0) {

			for (int k = 0; k < apcNl.getLength(); k++) {

				apcN = apcNl.item(k);
				if (apcN.getNodeType() == Node.ELEMENT_NODE) {

					apcE = (Element) apcN;

					/*
					 * there can only be one 'advancedProcessConfigurations'
					 * element
					 */
					return apcE;
				}
			}
		}

		// no 'advancedProcessConfigurations' element found
		return null;
	}

	/**
	 * @param targetElement
	 * @return map (can be empty but not <code>null</code>), with key:
	 *         identifier of wellknown stereotype, and value: list of conversion
	 *         parameters with that identifier as 'wellknown'
	 */
	private SortedMap<String, List<StereotypeConversionParameter>> parseStereotypeConversionParameters(
			Element targetElement) {

		NodeList scpNl = targetElement
				.getElementsByTagName("StereotypeConversionParameter");
		Node scpN;
		Element scpE;

		SortedMap<String, List<StereotypeConversionParameter>> result = new TreeMap<String, List<StereotypeConversionParameter>>();

		if (scpNl != null && scpNl.getLength() != 0) {

			for (int k = 0; k < scpNl.getLength(); k++) {

				scpN = scpNl.item(k);
				if (scpN.getNodeType() == Node.ELEMENT_NODE) {

					scpE = (Element) scpN;

					String wellknown = scpE.getAttribute("wellknown")
							.toLowerCase();

					String subClassOf_tmp = scpE.getAttribute("subClassOf")
							.trim();
					Set<String> subClassOf = new TreeSet<String>(
							Arrays.asList(StringUtils.split(subClassOf_tmp)));

					String rule = scpE.hasAttribute("rule")
							? scpE.getAttribute("rule").trim()
							: "*";

					StereotypeConversionParameter scp = new StereotypeConversionParameter(
							wellknown, subClassOf, rule);

					List<StereotypeConversionParameter> list;
					if (result.containsKey(wellknown)) {
						list = result.get(wellknown);
					} else {
						list = new ArrayList<StereotypeConversionParameter>();
						result.put(wellknown, list);
					}
					list.add(scp);
				}
			}
		}

		return result;
	}

	/**
	 * @param targetElement
	 * @return map, with key: type name, and value: list of conversion
	 *         parameters for types with that name (which can be more than one,
	 *         if schemas are different)
	 */
	private Map<String, List<TypeConversionParameter>> parseTypeConversionParameters(
			Element targetElement) {

		NodeList tcpNl = targetElement
				.getElementsByTagName("TypeConversionParameter");
		Node tcpN;
		Element tcpE;

		Map<String, List<TypeConversionParameter>> result = new TreeMap<String, List<TypeConversionParameter>>();

		if (tcpNl != null && tcpNl.getLength() != 0) {

			for (int k = 0; k < tcpNl.getLength(); k++) {

				tcpN = tcpNl.item(k);
				if (tcpN.getNodeType() == Node.ELEMENT_NODE) {

					tcpE = (Element) tcpN;

					String type = tcpE.getAttribute("type");

					String schema = tcpE.hasAttribute("schema")
							? tcpE.getAttribute("schema")
							: null;

					Set<String> subClassOf = new TreeSet<String>(
							Arrays.asList(StringUtils
									.split(tcpE.getAttribute("subClassOf"))));

					String rule = tcpE.hasAttribute("rule")
							? tcpE.getAttribute("rule")
							: "*";

					TypeConversionParameter tcp = new TypeConversionParameter(
							type, schema, subClassOf, rule);

					List<TypeConversionParameter> list;
					if (result.containsKey(type)) {
						list = result.get(type);
					} else {
						list = new ArrayList<TypeConversionParameter>();
						result.put(type, list);
					}
					list.add(tcp);
				}
			}
		}

		return result;
	}

	/**
	 * @param targetElement
	 * @return map (can be empty but not <code>null</code>), with key: property
	 *         name (optionally scoped to a class) and value: list of conversion
	 *         parameters for types with that name (which can be more than one,
	 *         if schemas are different)
	 */
	private Map<String, List<PropertyConversionParameter>> parsePropertyConversionParameters(
			Element targetElement) {

		NodeList pcpNl = targetElement
				.getElementsByTagName("PropertyConversionParameter");
		Node pcpN;
		Element pcpE;

		Map<String, List<PropertyConversionParameter>> result = new TreeMap<String, List<PropertyConversionParameter>>();

		if (pcpNl != null && pcpNl.getLength() != 0) {

			for (int k = 0; k < pcpNl.getLength(); k++) {

				pcpN = pcpNl.item(k);
				if (pcpN.getNodeType() == Node.ELEMENT_NODE) {

					pcpE = (Element) pcpN;

					String property = pcpE.getAttribute("property").trim();

					String schema = pcpE.hasAttribute("schema")
							? pcpE.getAttribute("schema").trim()
							: null;

					boolean global = pcpE.hasAttribute("global")
							? javax.xml.bind.DatatypeConverter
									.parseBoolean(pcpE.getAttribute("global"))
							: false;

					String subPropertyOf_tmp = pcpE
							.hasAttribute("subPropertyOf")
									? pcpE.getAttribute("subPropertyOf").trim()
									: null;
					Set<String> subPropertyOf = null;
					if (subPropertyOf_tmp != null) {
						subPropertyOf = new TreeSet<String>(Arrays
								.asList(StringUtils.split(subPropertyOf_tmp)));
					}

					String target = pcpE.hasAttribute("target")
							? pcpE.getAttribute("target").trim()
							: null;

					String targetSchema = pcpE.hasAttribute("targetSchema")
							? pcpE.getAttribute("targetSchema").trim()
							: null;

					String rule = pcpE.hasAttribute("rule")
							? pcpE.getAttribute("rule").trim()
							: "*";

					PropertyConversionParameter pcp = new PropertyConversionParameter(
							property, schema, global, subPropertyOf, target,
							targetSchema, rule);

					List<PropertyConversionParameter> list;
					if (result.containsKey(property)) {
						list = result.get(property);
					} else {
						list = new ArrayList<PropertyConversionParameter>();
						result.put(property, list);
					}
					list.add(pcp);
				}
			}
		}

		return result;
	}

	/**
	 * @param targetElement
	 * @return map (can be empty but not <code>null</code>), with key: type
	 *         name, and value: list of map entries for types with that name
	 *         (which can be more than one, if schemas are different)
	 */
	private Map<String, List<RdfTypeMapEntry>> parseRdfTypeMapEntries(
			Element targetElement) {

		NodeList rtmeNl = targetElement.getElementsByTagName("RdfTypeMapEntry");
		Node rtmeN;
		Element rtmeE;

		Map<String, List<RdfTypeMapEntry>> result = new TreeMap<String, List<RdfTypeMapEntry>>();

		if (rtmeNl != null && rtmeNl.getLength() != 0) {

			for (int k = 0; k < rtmeNl.getLength(); k++) {

				rtmeN = rtmeNl.item(k);
				if (rtmeN.getNodeType() == Node.ELEMENT_NODE) {

					rtmeE = (Element) rtmeN;

					String type = rtmeE.getAttribute("type").trim();

					String schema = rtmeE.hasAttribute("schema")
							? rtmeE.getAttribute("schema").trim()
							: null;

					String target = rtmeE.getAttribute("target").trim();

					RdfTypeMapEntry.TargetType targetType = RdfTypeMapEntry.TargetType.CLASS;
					if (rtmeE.hasAttribute("targetType")) {
						String tmp = rtmeE.getAttribute("targetType");
						if (tmp.trim().equalsIgnoreCase("class")) {
							targetType = RdfTypeMapEntry.TargetType.CLASS;
						} else {
							targetType = RdfTypeMapEntry.TargetType.DATATYPE;
						}
					}

					String rule = rtmeE.hasAttribute("rule")
							? rtmeE.getAttribute("rule").trim()
							: "*";

					RdfTypeMapEntry rtme = new RdfTypeMapEntry(type, schema,
							target, targetType, rule);

					List<RdfTypeMapEntry> list;
					if (result.containsKey(type)) {
						list = result.get(type);
					} else {
						list = new ArrayList<RdfTypeMapEntry>();
						result.put(type, list);
					}
					list.add(rtme);
				}
			}
		}

		return result;
	}

	/**
	 * @param targetElement
	 * @return map (can be empty but not <code>null</code>), with key: property
	 *         name (optionally scoped to a class), and value: list of map
	 *         entries for properties with that name (which can be more than
	 *         one, if schemas are different)
	 */
	private Map<String, List<RdfPropertyMapEntry>> parseRdfPropertyMapEntries(
			Element targetElement) {

		NodeList rpmeNl = targetElement
				.getElementsByTagName("RdfPropertyMapEntry");
		Node rpmeN;
		Element rpmeE;

		Map<String, List<RdfPropertyMapEntry>> result = new TreeMap<String, List<RdfPropertyMapEntry>>();

		if (rpmeNl != null && rpmeNl.getLength() != 0) {

			for (int k = 0; k < rpmeNl.getLength(); k++) {

				rpmeN = rpmeNl.item(k);
				if (rpmeN.getNodeType() == Node.ELEMENT_NODE) {

					rpmeE = (Element) rpmeN;

					String property = rpmeE.getAttribute("property").trim();

					String schema = rpmeE.hasAttribute("schema")
							? rpmeE.getAttribute("schema").trim()
							: null;

					String target = rpmeE.hasAttribute("target")
							? rpmeE.getAttribute("target").trim()
							: null;
					if (target != null && target.isEmpty()) {
						target = null;
					}

					String range = rpmeE.hasAttribute("range")
							? rpmeE.getAttribute("range").trim()
							: null;

					String rule = rpmeE.hasAttribute("rule")
							? rpmeE.getAttribute("rule").trim()
							: "*";

					RdfPropertyMapEntry rpme = new RdfPropertyMapEntry(property,
							schema, target, range, rule);

					List<RdfPropertyMapEntry> list;
					if (result.containsKey(property)) {
						list = result.get(property);
					} else {
						list = new ArrayList<RdfPropertyMapEntry>();
						result.put(property, list);
					}
					list.add(rpme);
				}
			}
		}

		return result;
	}

	/**
	 * @param targetElement
	 * @return map (can be empty but not <code>null</code>), with key:
	 *         constraint type, and value: mapping defined for the constraint
	 *         type
	 */
	private Map<ConstraintMapping.ConstraintType, ConstraintMapping> parseConstraintMappings(
			Element targetElement) {

		NodeList cmNl = targetElement.getElementsByTagName("ConstraintMapping");
		Node cmN;
		Element cmE;

		Map<ConstraintMapping.ConstraintType, ConstraintMapping> result = new HashMap<ConstraintMapping.ConstraintType, ConstraintMapping>();

		if (cmNl != null && cmNl.getLength() != 0) {

			for (int k = 0; k < cmNl.getLength(); k++) {

				cmN = cmNl.item(k);
				if (cmN.getNodeType() == Node.ELEMENT_NODE) {

					cmE = (Element) cmN;

					ConstraintMapping.ConstraintType constraintType;
					String tmp = cmE.getAttribute("constraintType");
					if (tmp.trim().equalsIgnoreCase("Text")) {
						constraintType = ConstraintMapping.ConstraintType.TEXT;
					} else if (tmp.trim().equalsIgnoreCase("FOL")) {
						constraintType = ConstraintMapping.ConstraintType.FOL;
					} else {
						constraintType = ConstraintMapping.ConstraintType.OCL;
					}

					String target = cmE.hasAttribute("target")
							? cmE.getAttribute("target").trim()
							: "iso19150-2:constraint";

					String template = cmE.getAttribute("template");

					String noValue = cmE.hasAttribute("noValue")
							? cmE.getAttribute("noValue")
							: " ";

					String mvct = cmE.hasAttribute("multiValueConnectorToken")
							? cmE.getAttribute("multiValueConnectorToken")
							: " ";

					ConstraintMapping.Format format = ConstraintMapping.Format.STRING;
					if (cmE.hasAttribute("format")) {
						String tmp2 = cmE.getAttribute("format");
						if (tmp2.trim().equalsIgnoreCase("string")) {
							format = ConstraintMapping.Format.STRING;
						} else {
							format = ConstraintMapping.Format.LANG_STRING;
						}
					}

					ConstraintMapping cm = new ConstraintMapping(constraintType,
							target, template, noValue, mvct, format);

					/*
					 * NOTE: constraints defined in the XSD for TargetOwl ensure
					 * that ConstraintMappings have unique constraintType
					 */
					result.put(constraintType, cm);
				}
			}
		}

		return result;
	}

	/**
	 * @param targetElement
	 * @return list of descriptor targets (can be empty but not
	 *         <code>null</code>)
	 */
	private List<DescriptorTarget> parseDescriptorTargets(
			Element targetElement) {

		NodeList dtNl = targetElement.getElementsByTagName("DescriptorTarget");
		Node dtN;
		Element dtE;

		List<DescriptorTarget> result = new ArrayList<DescriptorTarget>();

		if (dtNl != null && dtNl.getLength() != 0) {

			for (int k = 0; k < dtNl.getLength(); k++) {

				dtN = dtNl.item(k);
				if (dtN.getNodeType() == Node.ELEMENT_NODE) {

					dtE = (Element) dtN;

					String target = dtE.getAttribute("target").trim();

					String template = dtE.getAttribute("template");

					DescriptorTarget.Format format = DescriptorTarget.Format.LANG_STRING;
					if (dtE.hasAttribute("format")) {
						String tmp = dtE.getAttribute("format");
						if (tmp.trim().equalsIgnoreCase("string")) {
							format = DescriptorTarget.Format.STRING;
						} else if (tmp.trim().equalsIgnoreCase("IRI")) {
							format = DescriptorTarget.Format.IRI;
						} else {
							format = DescriptorTarget.Format.LANG_STRING;
						}
					}

					DescriptorTarget.NoValueBehavior noValueBehavior = DescriptorTarget.NoValueBehavior.INGORE;
					if (dtE.hasAttribute("noValueBehavior")) {
						String tmp = dtE.getAttribute("noValueBehavior");
						if (tmp.trim().equalsIgnoreCase("populateOnce")) {
							noValueBehavior = DescriptorTarget.NoValueBehavior.POPULATE_ONCE;
						} else {
							noValueBehavior = DescriptorTarget.NoValueBehavior.INGORE;
						}
					}

					String noValueText = dtE.hasAttribute("noValueText")
							? dtE.getAttribute("noValueText")
							: "";

					DescriptorTarget.MultiValueBehavior multiValueBehavior = DescriptorTarget.MultiValueBehavior.CONNECT_IN_SINGLE_TARGET;
					if (dtE.hasAttribute("multiValueBehavior")) {
						String tmp = dtE.getAttribute("multiValueBehavior");
						if (tmp.trim()
								.equalsIgnoreCase("splitToMultipleTargets")) {
							multiValueBehavior = DescriptorTarget.MultiValueBehavior.SPLIT_TO_MULTIPLE_TARGETS;
						} else {
							multiValueBehavior = DescriptorTarget.MultiValueBehavior.CONNECT_IN_SINGLE_TARGET;
						}
					}

					DescriptorTarget.AppliesTo appliesTo = DescriptorTarget.AppliesTo.ALL;
					if (dtE.hasAttribute("appliesTo")) {
						String tmp = dtE.getAttribute("appliesTo");
						if (tmp.trim().equalsIgnoreCase("ontology")) {
							appliesTo = DescriptorTarget.AppliesTo.ONTOLOGY;
						} else if (tmp.trim().equalsIgnoreCase("class")) {
							appliesTo = DescriptorTarget.AppliesTo.CLASS;
						} else if (tmp.trim()
								.equalsIgnoreCase("conceptscheme")) {
							appliesTo = DescriptorTarget.AppliesTo.CONCEPT_SCHEME;
						} else if (tmp.trim().equalsIgnoreCase("property")) {
							appliesTo = DescriptorTarget.AppliesTo.PROPERTY;
						} else {
							appliesTo = DescriptorTarget.AppliesTo.ALL;
						}
					}

					String mvct = dtE.hasAttribute("multiValueConnectorToken")
							? dtE.getAttribute("multiValueConnectorToken")
							: " ";

					DescriptorTarget dt = new DescriptorTarget(appliesTo,
							target, template, format, noValueBehavior,
							noValueText, multiValueBehavior, mvct);

					result.add(dt);
				}
			}
		}

		return result;
	}

	private List<Namespace> parseNamespaces(Element targetElement) {

		List<Namespace> result = new ArrayList<Namespace>();

		NodeList namespacesNl = targetElement.getElementsByTagName("Namespace");
		Node namespaceN;
		Element namespaceE;

		if (namespacesNl != null && namespacesNl.getLength() != 0) {

			for (int k = 0; k < namespacesNl.getLength(); k++) {

				namespaceN = namespacesNl.item(k);
				if (namespaceN.getNodeType() == Node.ELEMENT_NODE) {

					namespaceE = (Element) namespaceN;

					String nsabr = namespaceE.getAttribute("nsabr");
					String ns = namespaceE.getAttribute("ns");
					String location = namespaceE.hasAttribute("location")
							? location = namespaceE.getAttribute("location")
							: null;

					result.add(new Namespace(nsabr, ns, location));
				}
			}
		}
		return result;
	}

	private Map<String, String> parseParameters(Element processElement,
			String parameterElementTagName) throws ShapeChangeAbortException {

		Map<String, String> result = new HashMap<String, String>();
		NodeList parametersNl = processElement
				.getElementsByTagName(parameterElementTagName);
		Node parameterN;
		Element parameterE;

		if (parametersNl != null && parametersNl.getLength() != 0) {

			for (int k = 0; k < parametersNl.getLength(); k++) {

				parameterN = parametersNl.item(k);
				if (parameterN.getNodeType() == Node.ELEMENT_NODE) {

					parameterE = (Element) parameterN;

					result.put(parameterE.getAttribute("name"),
							parameterE.getAttribute("value"));
				}

			}
		}

		String s = result.get("gmlVersion");
		if (s != null) {
			if (s.equals("3.3") || s.equals("3.2") || s.equals("3.1")
					|| s.equals("2.1"))
				gmlVersion = s;
			else {
				throw new ShapeChangeAbortException(
						"Unknown value for gmlVersion: " + s);
			}
		}

		return result;
	}

	private List<XmlNamespace> parseXmlNamespaces(
			Element targetXmlSchemaElement) {

		List<XmlNamespace> result = new ArrayList<XmlNamespace>();

		NodeList xmlNamespacesNl = targetXmlSchemaElement
				.getElementsByTagName("XmlNamespace");
		Node xmlNamespaceN;
		Element xmlNamespaceE;

		if (xmlNamespacesNl != null && xmlNamespacesNl.getLength() != 0) {

			for (int k = 0; k < xmlNamespacesNl.getLength(); k++) {

				xmlNamespaceN = xmlNamespacesNl.item(k);
				if (xmlNamespaceN.getNodeType() == Node.ELEMENT_NODE) {

					xmlNamespaceE = (Element) xmlNamespaceN;

					String nsabr = xmlNamespaceE.getAttribute("nsabr");
					String ns = xmlNamespaceE.getAttribute("ns");
					String location = xmlNamespaceE.hasAttribute("location")
							? location = xmlNamespaceE.getAttribute("location")
							: null;
					String packageName = xmlNamespaceE
							.hasAttribute("packageName")
									? xmlNamespaceE.getAttribute("packageName")
									: null;

					result.add(
							new XmlNamespace(nsabr, ns, location, packageName));
				}
			}
		}
		return result;
	}

	/**
	 * Parses the given TargetXmlSchema element and returns a map of all
	 * XsdMapEntries it contains.
	 *
	 * @param targetXmlSchemaElement
	 *                                   The TargetXmlSchema element from the
	 *                                   ShapeChangeConfiguration
	 * @return map of the XsdMapEntries contained in the TargetXmlSchema element
	 *         (key: XsdMapEntry type)
	 */
	private List<XsdMapEntry> parseXsdMapEntries(
			Element targetXmlSchemaElement) {

		List<XsdMapEntry> result = new ArrayList<XsdMapEntry>();

		NodeList xsdMapEntriesNl = targetXmlSchemaElement
				.getElementsByTagName("XsdMapEntry");
		Node xsdMapEntryN;
		Element xsdMapEntryE;

		if (xsdMapEntriesNl != null && xsdMapEntriesNl.getLength() != 0) {

			for (int k = 0; k < xsdMapEntriesNl.getLength(); k++) {

				xsdMapEntryN = xsdMapEntriesNl.item(k);
				if (xsdMapEntryN.getNodeType() == Node.ELEMENT_NODE) {

					xsdMapEntryE = (Element) xsdMapEntryN;

					String type = xsdMapEntryE.getAttribute("type");

					List<String> encodingRules = new ArrayList<String>();
					String encodingRulesValue = xsdMapEntryE
							.getAttribute("xsdEncodingRules");
					if (encodingRulesValue != null
							&& encodingRulesValue.trim().length() > 0) {
						encodingRulesValue = encodingRulesValue.toLowerCase();
						encodingRules = new ArrayList<String>(
								Arrays.asList(encodingRulesValue.split("\\s")));
					}

					String xmlType = xsdMapEntryE.hasAttribute("xmlType")
							? xsdMapEntryE.getAttribute("xmlType")
							: null;

					String xmlTypeContent = "";
					if (xsdMapEntryE.hasAttribute("xmlTypeContent")) {
						xmlTypeContent = xsdMapEntryE
								.getAttribute("xmlTypeContent").trim();
					}
					if (xmlTypeContent.length() == 0) {
						xmlTypeContent = "complex";
					}

					String xmlTypeType = "";
					if (xsdMapEntryE.hasAttribute("xmlTypeType")) {
						xmlTypeType = xsdMapEntryE.getAttribute("xmlTypeType")
								.trim();
					}
					if (xmlTypeType.length() == 0) {
						xmlTypeType = "complex";
					}

					String xmlTypeNilReason = "";
					if (xsdMapEntryE.hasAttribute("xmlTypeNilReason")) {
						xmlTypeNilReason = xsdMapEntryE
								.getAttribute("xmlTypeNilReason").trim();
					}
					if (xmlTypeNilReason.length() == 0) {
						if (xmlTypeType.equals("simple"))
							xmlTypeNilReason = "false";
						else
							xmlTypeNilReason = "true";
					}

					String xmlElement = xsdMapEntryE.hasAttribute("xmlElement")
							? xsdMapEntryE.getAttribute("xmlElement")
							: null;

					String xmlPropertyType = xsdMapEntryE
							.hasAttribute("xmlPropertyType")
									? xsdMapEntryE
											.getAttribute("xmlPropertyType")
									: null;

					String xmlAttribute = xsdMapEntryE
							.hasAttribute("xmlAttribute")
									? xsdMapEntryE.getAttribute("xmlAttribute")
									: null;

					String xmlAttributeGroup = xsdMapEntryE
							.hasAttribute("xmlAttributeGroup")
									? xsdMapEntryE
											.getAttribute("xmlAttributeGroup")
									: null;

					String xmlReferenceable = xsdMapEntryE
							.hasAttribute("xmlReferenceable")
									? xsdMapEntryE
											.getAttribute("xmlReferenceable")
									: null;

					String xmlElementHasSimpleContent = xsdMapEntryE
							.hasAttribute("xmlElementHasSimpleContent")
									? xsdMapEntryE.getAttribute(
											"xmlElementHasSimpleContent")
									: null;

					String nsabr = xsdMapEntryE.hasAttribute("nsabr")
							? xsdMapEntryE.getAttribute("nsabr")
							: null;

					result.add(new XsdMapEntry(type, encodingRules, xmlType,
							xmlTypeContent, xmlTypeType, xmlTypeNilReason,
							xmlElement, xmlPropertyType, xmlAttribute,
							xmlAttributeGroup, xmlReferenceable,
							xmlElementHasSimpleContent, nsabr));
				}
			}
		}
		return result;
	}

	/**
	 * Parses the given process element and returns a map of all map entries it
	 * contains.
	 *
	 * @param processElement
	 *                           A Transformer or Target element from the
	 *                           ShapeChangeConfiguration
	 * @param string
	 * @return map of the map entries contained in the process element (key:
	 *         ProcessMapEntry type)
	 */
	private List<ProcessMapEntry> parseProcessMapEntries(Element processElement,
			String mapEntryElementTagName) {

		List<ProcessMapEntry> result = new ArrayList<ProcessMapEntry>();

		NodeList processMapEntriesNl = processElement
				.getElementsByTagName(mapEntryElementTagName);
		Node processMapEntryN;
		Element processMapEntryE;

		if (processMapEntriesNl != null
				&& processMapEntriesNl.getLength() != 0) {

			for (int k = 0; k < processMapEntriesNl.getLength(); k++) {

				processMapEntryN = processMapEntriesNl.item(k);
				if (processMapEntryN.getNodeType() == Node.ELEMENT_NODE) {

					processMapEntryE = (Element) processMapEntryN;

					String type = processMapEntryE.getAttribute("type");
					String rule = processMapEntryE.getAttribute("rule");
					String targetType = processMapEntryE
							.hasAttribute("targetType")
									? processMapEntryE
											.getAttribute("targetType")
									: null;
					String param = processMapEntryE.hasAttribute("param")
							? processMapEntryE.getAttribute("param")
							: null;

					result.add(
							new ProcessMapEntry(type, rule, targetType, param));
				}
			}
		}
		return result;
	}

	/**
	 * Parses all transformer configuration aspects available in the given
	 * ShapeChangeConfiguration document.
	 *
	 * @param configurationDocument
	 * @return map with key: 'id' assigned to the transformer, value: the
	 *         transformer configuration; can be empty but not <code>null</code>
	 * @throws ShapeChangeAbortException
	 *
	 */
	private Map<String, TransformerConfiguration> parseTransformerConfigurations(
			Document configurationDocument) throws ShapeChangeAbortException {

		Map<String, TransformerConfiguration> trfConfigs = new HashMap<String, TransformerConfiguration>();

		NodeList trfsNl = configurationDocument
				.getElementsByTagName("transformers");

		for (int i = 0; i < trfsNl.getLength(); i++) {
			Node trfsN = trfsNl.item(i);
			NodeList trfNl = trfsN.getChildNodes();

			// look for all Transformer elements in the "transformers" Node
			for (int j = 0; j < trfNl.getLength(); j++) {
				Node trfN = trfNl.item(j);

				if (trfN.getNodeType() == Node.ELEMENT_NODE) {
					Element trfE = (Element) trfN;

					// parse content of Transformer element

					// get transformer id
					String trfConfigId = trfE.getAttribute("id");

					// get transformer class name
					String trfConfigName = trfE.getAttribute("class");

					// get transformer mode
					ProcessMode trfMode = parseMode(trfE);

					Map<String, String> processParameters = parseParameters(
							trfE, "ProcessParameter");

					// now look up all ProcessRuleSet elements, if there are
					// any
					Map<String, ProcessRuleSet> processRuleSets = parseRuleSets(
							trfE, "ProcessRuleSet", false);

					// now look up all ProcessMapEntry elements, if there
					// are any
					List<ProcessMapEntry> processMapEntries = parseProcessMapEntries(
							trfE, "ProcessMapEntry");

					// parse tagged values, if any are defined
					List<TaggedValueConfigurationEntry> taggedValues = parseTaggedValues(
							trfE);

					// get the transformer input - can be null, then set it to
					// global input element
					String trfConfigInput;
					if (trfE.hasAttribute("input")) {
						trfConfigInput = trfE.getAttribute("input");
					} else {
						trfConfigInput = getInputId();
					}

					if (trfConfigInput.equals(trfConfigId)) {
						throw new ShapeChangeAbortException(
								"Attributes input and id are equal in a transformer configuration element (class: "
										+ trfConfigName
										+ ") which is not allowed.");
					}

					Element advancedProcessConfigurations = parseAdvancedProcessConfigurations(
							trfE);

					// create transformer config and add it to list
					TransformerConfiguration trfConfig = new TransformerConfiguration(
							trfConfigId, trfConfigName, trfMode,
							processParameters, processRuleSets,
							processMapEntries, taggedValues, trfConfigInput,
							advancedProcessConfigurations);

					trfConfigs.put(trfConfig.getId(), trfConfig);
				}
			}
		}
		return trfConfigs;

	}

	/**
	 * @param trfE
	 *                 "Transformer" element from the configuration
	 * @return list of tagged values defined for the transformer or
	 *         <code>null</code> if no tagged values are defined for it.
	 */
	private List<TaggedValueConfigurationEntry> parseTaggedValues(
			Element trfE) {

		List<TaggedValueConfigurationEntry> result = new ArrayList<TaggedValueConfigurationEntry>();

		Element directTaggedValuesInTrf = null;

		/*
		 * identify taggedValues element that is direct child of the Transformer
		 * element - can be null
		 */
		NodeList children = trfE.getChildNodes();
		if (children != null && children.getLength() != 0) {

			for (int k = 0; k < children.getLength(); k++) {

				Node n = children.item(k);
				if (n.getNodeType() == Node.ELEMENT_NODE
						&& n.getNodeName().equals("taggedValues")) {
					directTaggedValuesInTrf = (Element) n;
					break;
				}
			}
		}

		if (directTaggedValuesInTrf != null) {

			NodeList taggedValuesNl = directTaggedValuesInTrf
					.getElementsByTagName("TaggedValue");
			Node taggedValueN;
			Element taggedValueE;

			if (taggedValuesNl != null && taggedValuesNl.getLength() > 0) {

				for (int k = 0; k < taggedValuesNl.getLength(); k++) {

					taggedValueN = taggedValuesNl.item(k);
					if (taggedValueN.getNodeType() == Node.ELEMENT_NODE) {

						taggedValueE = (Element) taggedValueN;

						TaggedValueConfigurationEntry tvce = TaggedValueConfigurationEntry
								.parse(taggedValueE);
						result.add(tvce);
					}
				}
			}
		}

		if (result.isEmpty())
			return null;
		else
			return result;
	}

	private ProcessMode parseMode(Element processElement) {

		ProcessMode result = ProcessMode.enabled;

		if (processElement.hasAttribute("mode")) {
			String mode = processElement.getAttribute("mode");
			if (mode.equalsIgnoreCase("diagnostics-only")) {
				mode = "diagnosticsonly";
			}
			result = ProcessMode.fromString(mode);
		}

		return result;
	}

	private Map<String, ProcessRuleSet> parseRuleSets(Element processElement,
			String ruleSetElementTagName, boolean checkRuleExistence)
			throws ShapeChangeAbortException {

		Map<String, ProcessRuleSet> result = new HashMap<String, ProcessRuleSet>();

		NodeList processRuleSetsNl = processElement
				.getElementsByTagName(ruleSetElementTagName);
		Node processRuleSetN;
		Element processRuleSetE;
		Set<String> ruleSetRules;

		if (processRuleSetsNl != null && processRuleSetsNl.getLength() != 0) {

			for (int k = 0; k < processRuleSetsNl.getLength(); k++) {

				processRuleSetN = processRuleSetsNl.item(k);

				if (processRuleSetN.getNodeType() == Node.ELEMENT_NODE) {

					// parse rule set
					processRuleSetE = (Element) processRuleSetN;

					// parse name and extends
					String processRuleSetName = processRuleSetE
							.getAttribute("name");
					// String processRuleSetName = processRuleSetE
					// .getAttribute("name").toLowerCase();
					String extendedRuleName = processRuleSetE
							.getAttribute("extends");
					if (extendedRuleName != null
							&& extendedRuleName.trim().length() == 0) {
						extendedRuleName = "*";
					}
					// extendedRuleName = extendedRuleName.toLowerCase();

					// parse additional rules for rule set, if
					// there are any
					ruleSetRules = null;
					NodeList processRuleSetRulesNl = processRuleSetE
							.getElementsByTagName("rule");
					if (processRuleSetRulesNl != null
							&& processRuleSetRulesNl.getLength() != 0) {

						for (int l = 0; l < processRuleSetRulesNl
								.getLength(); l++) {
							Node processRuleSetRuleN = processRuleSetRulesNl
									.item(l);
							if (processRuleSetRuleN
									.getNodeType() == Node.ELEMENT_NODE) {
								Element processRuleSetRuleE = (Element) processRuleSetRuleN;
								if (ruleSetRules == null)
									ruleSetRules = new HashSet<String>();
								String ruleName = processRuleSetRuleE
										.getAttribute("name");

								// we do not need to check rules for
								// transformers here
								if (checkRuleExistence) {
									if (hasRule(ruleName))
										ruleSetRules.add(ruleName);
									else
										System.err.println(
												"Warning while loading configuration file: Rule '"
														+ ruleName
														+ "' is unknown, but referenced from the configuration. This is only a problem, if "
														+ "you know that the conversion rule is used in an encoding rule of your configuration.");
								} else {
									ruleSetRules.add(ruleName);
								}
							}
						}
					}

					result.put(processRuleSetName,
							new ProcessRuleSet(processRuleSetName,
									extendedRuleName, ruleSetRules));

				}
			}
		}
		return result;
	}

	private void addStandardRules() {

		/*
		 * FIXME: Targets should be able to register rules with options. By
		 * requiring that Options knows all rules up front, one cannot add a new
		 * target dynamically (e.g. via an additional library on the classpath)
		 * and make use of the Info.matches(String) function.
		 */

		/*
		 * mandatory rules
		 */
		addRule("req-xsd-pkg-xsdDocument-unique");
		addRule("req-xsd-cls-name-unique");
		addRule("req-xsd-cls-ncname");
		addRule("req-xsd-prop-data-type");
		addRule("req-xsd-prop-value-type-exists");
		addRule("req-xsd-prop-ncname");
		addRule("rule-xsd-pkg-contained-packages");
		addRule("rule-xsd-pkg-dependencies");
		addRule("rule-xsd-cls-union-as-choice");
		addRule("rule-xsd-cls-unknown-as-object");
		addRule("rule-xsd-cls-sequence");
		addRule("rule-xsd-cls-object-element");
		addRule("rule-xsd-cls-type");
		addRule("rule-xsd-cls-property-type");
		addRule("rule-xsd-cls-local-properties");
		/*
		 * Associate these with a core encoding rule
		 */
		addRule("req-xsd-pkg-xsdDocument-unique", "*");
		addRule("req-xsd-cls-name-unique", "*");
		addRule("req-xsd-cls-ncname", "*");
		addRule("req-xsd-prop-data-type", "*");
		addRule("req-xsd-prop-value-type-exists", "*");
		addRule("req-xsd-prop-ncname", "*");
		addRule("rule-xsd-pkg-contained-packages", "*");
		addRule("rule-xsd-pkg-dependencies", "*");
		addRule("rule-xsd-cls-unknown-as-object", "*");
		addRule("rule-xsd-cls-object-element", "*");
		addRule("rule-xsd-cls-type", "*");
		addRule("rule-xsd-cls-property-type", "*");
		addRule("rule-xsd-cls-local-properties", "*");
		addRule("rule-xsd-cls-union-as-choice", "*");
		addRule("rule-xsd-cls-sequence", "*");
		/*
		 * GML 3.2 / ISO 19136:2007 rules
		 */
		addRule("req-xsd-cls-generalization-consistent");
		addRule("rule-xsd-all-naming-gml");
		addRule("rule-xsd-cls-global-enumeration");
		addRule("rule-xsd-cls-codelist-asDictionary");
		addRule("rule-xsd-cls-noPropertyType");
		addRule("rule-xsd-cls-byValuePropertyType");
		addRule("rule-xsd-cls-standard-gml-property-types");
		addRule("rule-xsd-pkg-gmlProfileSchema");
		addRule("rule-xsd-prop-defaultCodeSpace");
		addRule("rule-xsd-prop-inlineOrByReference");
		addRule("rule-xsd-prop-reverseProperty");
		addRule("rule-xsd-prop-targetElement");
		/*
		 * add the iso19136_2007 encoding rule and extend the core encoding rule
		 */
		addExtendsEncRule("iso19136_2007", "*");
		addRule("req-xsd-cls-generalization-consistent", "iso19136_2007");
		addRule("rule-xsd-all-naming-gml", "iso19136_2007");
		addRule("rule-xsd-cls-global-enumeration", "iso19136_2007");
		addRule("rule-xsd-cls-codelist-asDictionary", "iso19136_2007");
		addRule("rule-xsd-cls-standard-gml-property-types", "iso19136_2007");
		addRule("rule-xsd-cls-noPropertyType", "iso19136_2007");
		addRule("rule-xsd-cls-byValuePropertyType", "iso19136_2007");
		addRule("rule-xsd-pkg-gmlProfileSchema", "iso19136_2007");
		addRule("rule-xsd-prop-targetElement", "iso19136_2007");
		addRule("rule-xsd-prop-reverseProperty", "iso19136_2007");
		addRule("rule-xsd-prop-defaultCodeSpace", "iso19136_2007");
		addRule("rule-xsd-prop-inlineOrByReference", "iso19136_2007");
		/*
		 * additional GML 3.3 rules
		 */
		addRule("rule-xsd-cls-codelist-asDictionaryGml33");
		addRule("rule-xsd-rel-association-classes");
		/*
		 * add the gml33 encoding rule and extend the core encoding rule
		 */
		addExtendsEncRule("gml33", "*");
		addRule("req-xsd-cls-generalization-consistent", "gml33");
		addRule("rule-xsd-all-naming-gml", "gml33");
		addRule("rule-xsd-cls-global-enumeration", "gml33");
		addRule("rule-xsd-cls-codelist-asDictionaryGml33", "gml33");
		addRule("rule-xsd-cls-standard-gml-property-types", "gml33");
		addRule("rule-xsd-cls-noPropertyType", "gml33");
		addRule("rule-xsd-cls-byValuePropertyType", "gml33");
		addRule("rule-xsd-pkg-gmlProfileSchema", "gml33");
		addRule("rule-xsd-prop-targetElement", "gml33");
		addRule("rule-xsd-prop-reverseProperty", "gml33");
		addRule("rule-xsd-prop-defaultCodeSpace", "gml33");
		addRule("rule-xsd-prop-inlineOrByReference", "gml33");
		addRule("rule-xsd-rel-association-classes", "gml33");
		/*
		 * ISO/TS 19139:2007 rules
		 */
		addRule("rule-xsd-all-naming-19139");
		addRule("rule-xsd-cls-standard-19139-isoType");
		addRule("rule-xsd-cls-standard-19139-property-types");
		addRule("rule-xsd-cls-enum-object-element");
		addRule("rule-xsd-cls-enum-property-type");

		/*
		 * add the iso19139_2007 encoding rule and extend the core encoding rule
		 */
		addExtendsEncRule("iso19139_2007", "*");
		addRule("rule-xsd-cls-enum-object-element", "iso19139_2007");
		addRule("rule-xsd-cls-enum-property-type", "iso19139_2007");
		addRule("rule-xsd-cls-global-enumeration", "iso19139_2007");
		addRule("rule-xsd-cls-standard-19139-property-types", "iso19139_2007");
		addRule("rule-xsd-all-naming-19139", "iso19139_2007");
		/*
		 * SWE Common Data Model 2.0 rules
		 */
		addRule("rule-xsd-all-naming-swe");
		addRule("rule-xsd-prop-xsdAsAttribute");
		addRule("rule-xsd-prop-soft-typed");
		addRule("rule-xsd-cls-union-as-group-property-type");
		addRule("rule-xsd-cls-standard-swe-property-types");
		addRule("rule-xsd-prop-initialValue");
		/*
		 * add the ogcSweCommon2 encoding rule and extend the core encoding rule
		 */
		addExtendsEncRule("ogcSweCommon2", "*");
		addRule("req-xsd-cls-generalization-consistent", "ogcSweCommon2");
		addRule("rule-xsd-all-naming-swe", "ogcSweCommon2");
		addRule("rule-xsd-cls-global-enumeration", "ogcSweCommon2");
		addRule("rule-xsd-cls-codelist-asDictionary", "ogcSweCommon2");
		addRule("rule-xsd-cls-standard-swe-property-types", "ogcSweCommon2");
		addRule("rule-xsd-cls-noPropertyType", "ogcSweCommon2");
		addRule("rule-xsd-cls-byValuePropertyType", "ogcSweCommon2");
		addRule("rule-xsd-pkg-gmlProfileSchema", "ogcSweCommon2");
		addRule("rule-xsd-prop-targetElement", "ogcSweCommon2");
		addRule("rule-xsd-prop-reverseProperty", "ogcSweCommon2");
		addRule("rule-xsd-prop-defaultCodeSpace", "ogcSweCommon2");
		addRule("rule-xsd-prop-inlineOrByReference", "ogcSweCommon2");
		addRule("rule-xsd-prop-xsdAsAttribute", "ogcSweCommon2");
		addRule("rule-xsd-prop-soft-typed", "ogcSweCommon2");
		addRule("rule-xsd-cls-union-as-group-property-type", "ogcSweCommon2");
		addRule("rule-xsd-prop-initialValue", "ogcSweCommon2");

		/*
		 * additional GML 2.1 rules
		 */
		addRule("rule-xsd-all-gml21");
		addRule("rule-xsd-cls-codelist-anonymous-xlink");
		/*
		 * add the gml21 encoding rule and extend the core encoding rule
		 */
		addExtendsEncRule("gml21", "iso19136_2007");
		addRule("rule-xsd-all-gml21", "gml21");
		addRule("rule-xsd-cls-codelist-anonymous-xlink", "gml21");

		/*
		 * addExtendsEncRule("gcsr","*");
		 * addRule("req-xsd-cls-mixin-supertypes","gcsr");
		 * addRule("req-xsd-cls-codelist-no-supertypes","gcsr");
		 * addRule("rule-xsd-cls-union-asCharacterString","gcsr");
		 * addRule("rule-xsd-cls-union-asGroup","gcsr");
		 * addRule("rule-xsd-cls-enum-supertypes","gcsr");
		 * addRule("rule-xsd-cls-enum-subtypes","gcsr");
		 * addRule("rule-xsd-cls-basictype","gcsr");
		 * addRule("rule-xsd-cls-union-direct","gcsr");
		 * addRule("rule-xsd-cls-codelist-constraints","gcsr");
		 * addRule("rule-xsd-cls-mixin-classes-as-group","gcsr");
		 * addRule("rule-xsd-cls-mixin-classes","gcsr");
		 * addRule("rule-xsd-prop-exclude-derived","gcsr");
		 * addRule("rule-xsd-prop-length-size-pattern","gcsr");
		 * addRule("rule-xsd-prop-xsdAsAttribute","gcsr");
		 * addRule("rule-xsd-prop-nillable","gcsr");
		 * addRule("rule-xsd-prop-nilReasonAllowed","gcsr");
		 * addRule("rule-xsd-prop-initialValue","gcsr");
		 * addRule("rule-xsd-prop-att-map-entry","gcsr");
		 * addRule("rule-xsd-pkg-schematron","gcsr");
		 * addRule("rule-xsd-all-tagged-values","gcsr");
		 */

		/*
		 * non-standard extensions - requirements
		 */
		addRule("req-all-all-documentation");
		addRule("req-all-prop-sequenceNumber");
		addRule("req-xsd-pkg-targetNamespace");
		addRule("req-xsd-pkg-xmlns");
		addRule("req-xsd-pkg-namespace-schema-only");
		addRule("rec-xsd-pkg-version");
		addRule("req-xsd-pkg-xsdDocument");
		addRule("req-xsd-pkg-dependencies");
		addRule("req-xsd-cls-codelist-asDictionary-true");
		addRule("req-xsd-cls-codelist-extensibility-values");
		addRule("req-xsd-cls-codelist-extensibility-vocabulary");
		addRule("req-xsd-cls-codelist-no-supertypes");
		addRule("req-xsd-cls-datatype-noPropertyType");
		addRule("req-xsd-cls-enum-no-supertypes");
		addRule("req-xsd-cls-mixin-supertypes");
		addRule("req-xsd-cls-mixin-supertypes-overrule");
		addRule("req-xsd-cls-objecttype-byValuePropertyType");
		addRule("req-xsd-cls-objecttype-noPropertyType");
		addRule("req-xsd-cls-suppress-no-properties");
		addRule("req-xsd-cls-suppress-subtype");
		addRule("req-xsd-cls-suppress-supertype");
		addRule("req-xsd-prop-codelist-obligation");
		/*
		 * non-standard extensions - conversion rules
		 */
		addRule("rule-xsd-all-descriptorAnnotation");
		addRule("rule-xsd-all-globalIdentifierAnnotation");
		addRule("rule-xsd-all-notEncoded");
		addRule("rule-xsd-all-propertyAssertion-ignoreProhibited");
		addRule("rule-xsd-cls-adeelement");
		addRule("rule-xsd-cls-basictype");
		addRule("rule-xsd-cls-codelist-constraints");
		addRule("rule-xsd-cls-codelist-constraints2");
		addRule("rule-xsd-cls-codelist-constraints-codeAbsenceInModelAllowed");
		addRule("rule-xsd-cls-codelist-gmlsf");
		addRule("rule-xsd-cls-enum-subtypes");
		addRule("rule-xsd-cls-enum-supertypes");
		addRule("rule-xsd-cls-mixin-classes-as-group");
		addRule("rule-xsd-cls-mixin-classes");
		addRule("rule-xsd-cls-mixin-classes-non-mixin-supertypes");
		addRule("rule-xsd-cls-no-abstract-classes");
		addRule("rule-xsd-cls-no-base-class");
		addRule("rule-xsd-cls-no-gml-types");
		addRule("rule-xsd-cls-okstra-fid");
		addRule("rule-xsd-cls-okstra-lifecycle");
		addRule("rule-xsd-cls-okstra-schluesseltabelle");
		addRule("rule-xsd-cls-suppress");
		addRule("rule-xsd-cls-union-asCharacterString");
		addRule("rule-xsd-cls-union-asGroup");
		addRule("rule-xsd-cls-union-direct");
		addRule("rule-xsd-cls-union-direct-optionality");
		addRule("rule-xsd-cls-union-omitUnionsRepresentingFeatureTypeSets");
		addRule("rule-xsd-prop-att-map-entry");
		addRule("rule-xsd-prop-constrainingFacets");
		addRule("rule-xsd-prop-exclude-derived");
		addRule("rule-xsd-prop-length-size-pattern");
		addRule("rule-xsd-prop-featureType-gmlsf-byReference");
		addRule("rule-xsd-prop-metadata");
		addRule("rule-xsd-prop-metadata-gmlsf-byReference");
		addRule("rule-xsd-prop-nillable");
		addRule("rule-xsd-prop-nilReasonAllowed");
		addRule("rule-xsd-prop-gmlArrayProperty");
		addRule("rule-xsd-prop-gmlListProperty");
		addRule("rule-xsd-prop-qualified-associations");
		addRule("rule-xsd-prop-targetCodeListURI");
		addRule("rule-xsd-all-no-documentation");
		addRule("rule-xsd-cls-local-enumeration");
		addRule("rule-xsd-cls-local-basictype");
		addRule("rule-xsd-pkg-dgiwgsp");
		addRule("rule-xsd-pkg-gmlsf");
		addRule("rule-xsd-pkg-schematron");
		addRule("rule-xsd-all-tagged-values");
		addRule("rule-xsd-cls-adehook");

		// AIXM specific rules
		addRule("rule-all-cls-aixmDatatype");
		// further rules of a more general nature
		addRule("rule-all-prop-uomAsAttribute");

		/*
		 * JSON encoding rules
		 */
		addRule("rule-json-all-notEncoded");

		addExtendsEncRule("geoservices", "*");
		addExtendsEncRule("geoservices_extended", "*");

		/*
		 * RDF encoding rules
		 */
		addRule("rule-rdf-prop-parent");

		/*
		 * SQL encoding rules
		 */

		addRule("rule-sql-all-associativetables");
		addRule("rule-sql-all-check-constraint-naming-oracle-default");
		addRule("rule-sql-all-check-constraint-naming-pearsonhash");
		addRule("rule-sql-all-check-constraint-naming-postgresql-default");
		addRule("rule-sql-all-check-constraint-naming-sqlserver-default");
		addRule("rule-sql-all-constraintNameUsingShortName");
		addRule("rule-sql-all-databaseModel");
		addRule("rule-sql-all-documentationViaExplicitCommentStatements");
		addRule("rule-sql-all-exclude-abstract");
		addRule("rule-sql-all-foreign-key-oracle-naming-style");
		addRule("rule-sql-all-foreign-key-pearsonhash-naming");
		addRule("rule-sql-all-foreign-key-default-naming");
		addRule("rule-sql-all-indexNameUsingShortName");
		addRule("rule-sql-all-normalizing-ignore-case");
		addRule("rule-sql-all-normalizing-lower-case");
		addRule("rule-sql-all-normalizing-oracle");
		addRule("rule-sql-all-normalizing-sqlserver");
		addRule("rule-sql-all-normalizing-upper-case");
		addRule("rule-sql-all-notEncoded");
		addRule("rule-sql-all-precisionAndScale");
		addRule("rule-sql-all-representTaggedValues");
		addRule("rule-sql-all-suppressDocumentationViaInlineComments");
		addRule("rule-sql-all-unique-naming-count-suffix");

		addRule("rule-sql-cls-code-lists");
		addRule("rule-sql-cls-code-lists-pods");
		addRule("rule-sql-cls-data-types");
		addRule("rule-sql-cls-data-types-oneToMany-oneTable");
		addRule("rule-sql-cls-data-types-oneToMany-oneTable-ignoreSingleValuedCase");
		addRule("rule-sql-cls-data-types-oneToMany-severalTables");
		addRule("rule-sql-cls-data-types-oneToMany-severalTables-avoidTableForDatatypeIfUnused");
		addRule("rule-sql-cls-feature-types");
		addRule("rule-sql-cls-identifierStereotype");
		addRule("rule-sql-cls-object-types");
		addRule("rule-sql-cls-references-to-external-types");

		addRule("rule-sql-prop-check-constraint-for-range");
		addRule("rule-sql-prop-check-constraints-for-enumerations");
		addRule("rule-sql-prop-check-constraint-restrictTimeOfDate");
		addRule("rule-sql-prop-exclude-derived");
		addRule("rule-sql-prop-uniqueConstraints");

		addRule("rule-sql-all-replicationSchema");
		addRule("rule-sql-prop-replicationSchema-documentation-fieldWithUnlimitedLengthCharacterDataType");
		addRule("rule-sql-prop-replicationSchema-geometryAnnotation");
		addRule("rule-sql-prop-replicationSchema-maxLength-from-size");
		addRule("rule-sql-prop-replicationSchema-nillable");
		addRule("rule-sql-prop-replicationSchema-optional");

		// declare rule sets
		addExtendsEncRule(SQL, "*");
		addRule("rule-sql-cls-feature-types", SQL);

		/*
		 * OWL encoding rules
		 */

		// declare optional rules
		addRule("rule-owl-all-constraints-byConstraintMapping");
		addRule("rule-owl-all-constraints-humanReadableTextOnly");

		addRule("rule-owl-pkg-importISO191502Base");
		addRule("rule-owl-pkg-dctSourceTitle");
		addRule("rule-owl-pkg-ontologyName-appendVersion");
		addRule("rule-owl-pkg-ontologyName-byTaggedValue");
		addRule("rule-owl-pkg-ontologyName-code");
		addRule("rule-owl-pkg-ontologyName-withPath");
		addRule("rule-owl-pkg-ontologyName-iso191502");
		addRule("rule-owl-pkg-versionInfo");
		addRule("rule-owl-pkg-versionIRI");
		addRule("rule-owl-pkg-versionIRI-avoid-duplicate-version");
		addRule("rule-owl-pkg-singleOntologyPerSchema");

		addRule("rule-owl-cls-codelist-external");
		addRule("rule-owl-cls-codelist-19150-2");
		addRule("rule-owl-cls-codelist-19150-2-conceptSchemeSubclass");
		addRule("rule-owl-cls-codelist-19150-2-differentIndividuals");
		addRule("rule-owl-cls-codelist-19150-2-owlClassInDifferentNamespace");
		addRule("rule-owl-cls-codelist-19150-2-objectOneOfForEnumeration");
		addRule("rule-owl-cls-codelist-19150-2-skos-collection");
		addRule("rule-owl-cls-disjoint-classes");
		addRule("rule-owl-cls-iso191502Enumeration");

		addRule("rule-owl-cls-encode-featuretypes");
		addRule("rule-owl-cls-encode-basictypes");
		addRule("rule-owl-cls-encode-datatypes");
		addRule("rule-owl-cls-encode-mixintypes");
		addRule("rule-owl-cls-encode-objecttypes");
		addRule("rule-owl-cls-enumerationAsCodelist");
		addRule("rule-owl-cls-generalization");
		addRule("rule-owl-cls-iso191502IsAbstract");
		addRule("rule-owl-cls-union");
		addRule("rule-owl-cls-unionSets");

		addRule("rule-owl-prop-code-broader-byBroaderListedValue");
		addRule("rule-owl-prop-external-reference");
		addRule("rule-owl-prop-general");
		addRule("rule-owl-prop-globalScopeAttributes");
		addRule("rule-owl-prop-globalScopeByConversionParameter");
		addRule("rule-owl-prop-globalScopeByUniquePropertyName");
		addRule("rule-owl-prop-inverseOf");
		addRule("rule-owl-prop-iso191502Aggregation");
		addRule("rule-owl-prop-iso191502AssociationName");
		addRule("rule-owl-prop-iso191502-naming");
		addRule("rule-owl-prop-labelFromLocalName");
		addRule("rule-owl-prop-localScopeAll");
		addRule("rule-owl-prop-mapping-compare-specifications");
		addRule("rule-owl-prop-multiplicityAsQualifiedCardinalityRestriction");
		addRule("rule-owl-prop-multiplicityAsUnqualifiedCardinalityRestriction");
		addRule("rule-owl-prop-propertyEnrichment");
		addRule("rule-owl-prop-range-global");
		addRule("rule-owl-prop-range-local-withUniversalQuantification");
		addRule("rule-owl-prop-voidable-as-minCardinality0");

		/*
		 * ArcGIS workspace encoding rules
		 */
		addRule("rule-arcgis-all-postprocess-removeUnusedCodedValueDomains");
		addRule("rule-arcgis-all-precision");
		addRule("rule-arcgis-all-relationshipClassNameByTaggedValueOfClasses");
		addRule("rule-arcgis-all-representTaggedValues");
		addRule("rule-arcgis-all-scale");
		addRule("rule-arcgis-all-subtypes");
		addRule("rule-arcgis-cls-hasM");
		addRule("rule-arcgis-cls-hasZ");
		addRule("rule-arcgis-cls-identifierStereotype");
		addRule("rule-arcgis-cls-rangeDomainFromTaggedValues");
		addRule("rule-arcgis-prop-attIndex");
		addRule("rule-arcgis-prop-initialValue");
		addRule("rule-arcgis-prop-initialValueByAlias");
		addRule("rule-arcgis-prop-isNullable");
		addRule("rule-arcgis-prop-lengthFromCodesOrEnumsOfValueType");
		addRule("rule-arcgis-prop-lengthFromTaggedValue");
		addRule("rule-arcgis-prop-lengthFromTaggedValueForCodelistOrEnumerationValueType");
		addRule("rule-arcgis-prop-precision"); // deprecated
		addRule("rule-arcgis-prop-reflexiveRelationshipAsField");
		addRule("rule-arcgis-prop-scale"); // deprecated

		/*
		 * Replication schema encoding rules
		 */
		addRule("rule-rep-prop-optional");
		addRule("rule-rep-prop-exclude-derived");
		addRule("rule-rep-cls-generate-objectidentifier");
		addRule("rule-rep-prop-maxLength-from-size");

		/*
		 * Application schema metadata rules
		 */
		addRule("rule-asm-all-identify-profiles");
		addRule("rule-asm-all-identifyTypeUsage");
		addRule("rule-asm-all-identifyPropertiesWithSpecificTaggedValues");

		/*
		 * Model export conversion rules
		 */
		addRule("rule-exp-all-omitDescriptors");
		addRule("rule-exp-all-omitExistingProfiles");
		addRule("rule-exp-all-restrictExistingProfiles");
		addRule("rule-exp-pkg-allPackagesAreEditable");
		addRule("rule-exp-prop-suppressIsNavigable");

		/*
		 * CDB conversion rules
		 */
		addRule("rule-cdb-all-notEncoded");
		addRule("rule-cdb-all-valueTypeTextForUnionRepresentingFeatureSet");

		/*
		 * CodeListDictionariesML conversion rules
		 */
		addRule("rule-cldml-prop-codeListAndCodeNameAsGmlId");

		/*
		 * ldproxy Configuration conversion rules
		 */
		addRule("rule-ldp-cls-non-abstract-feature-types-as-collection");
		addRule("rule-ldp-cls-table-per-feature-type");
		addRule("rule-ldp-cls-id-field");
		addRule("rule-ldp-cls-generate-codelist");
		addRule("rule-ldp-all-names-in-lowercase");
		addRule("rule-ldp-all-names-max-length");
		addRule("rule-ldp-prop-all-datatype-relations-as-n-to-m-relations");
		addRule("rule-ldp-prop-all-featuretype-relations-as-n-to-m-relations");
		addRule("rule-ldp-prop-multiple-single-values-as-1-to-n-relations");
		addRule("rule-ldp-prop-separate-geometry-table");
		addRule("rule-ldp-prop-all-codelist-values-as-strings");
		addRule("rule-ldp-cls-oneo-metadata");
	}

	/**
	 * Normalize a stereotype fetched from the model.
	 * 
	 * @return The well-known stereotype, for which the given stereotype is
	 *         defined as an alias (via stereotype alias configuration
	 *         elements), or the given stereotype (if it is not configured as a
	 *         stereotype alias).
	 */
	public String normalizeStereotype(String stereotype) {
		// Map stereotype alias to well-known stereotype
		String s = stereotypeAlias(stereotype.trim());
		if (s != null)
			return s;
		return stereotype;
	};

	/**
	 * Normalize a tag fetched from the model.
	 * 
	 * @return the mapping for the tag, or the given tag itself if no mapping is
	 *         configured
	 */
	public String normalizeTag(String tag) {
		// Map tag alias to well-known tag
		String s = tagAlias(tag.trim());
		if (s != null)
			return s;
		return tag;
	};

	/**
	 * Determines if the given package should not be processed (i.e., should be
	 * skipped), based upon the configuration parameters 'appSchemaName',
	 * 'appSchemaNameRegex', and 'appSchemaNamespaceRegex'. If the current
	 * process configuration contains one of these parameters, they are used.
	 * Otherwise, the parameters from the input configuration are used. If the
	 * package does not match one of the defined parameters, the result will be
	 * <code>true</code>.
	 * 
	 * @param pi
	 *               package to check
	 * @return <code>true</code> if the given package shall be skipped, else
	 *         <code>false</code>
	 */
	public boolean skipSchema(PackageInfo pi) {

		String name = pi.name();
		String ns = pi.targetNamespace();

		String appSchemaName = null;
		String appSchemaNameRegex = null;
		String appSchemaNamespaceRegex = null;

		/*
		 * If the current process configuration defines selection parameters,
		 * use these criteria. Otherwise, use the selection parameters defined
		 * by the input configuration.
		 */
		if (currentProcessConfig != null
				&& (currentProcessConfig.hasParameter("appSchemaName")
						|| currentProcessConfig
								.hasParameter("appSchemaNameRegex")
						|| currentProcessConfig
								.hasParameter("appSchemaNamespaceRegex"))) {

			appSchemaName = currentProcessConfig
					.getParameterValue("appSchemaName");
			appSchemaNameRegex = currentProcessConfig
					.getParameterValue("appSchemaNameRegex");
			appSchemaNamespaceRegex = currentProcessConfig
					.getParameterValue("appSchemaNamespaceRegex");

		} else {

			/*
			 * if the current process configuration does not provide selection
			 * parameters, use the input parameters
			 */
			appSchemaName = parameter("appSchemaName");
			appSchemaNameRegex = parameter("appSchemaNameRegex");
			appSchemaNamespaceRegex = parameter("appSchemaNamespaceRegex");
		}

		/*
		 * only process schemas with a given name
		 */
		if (StringUtils.isNotBlank(appSchemaName)
				&& !appSchemaName.equals(name))
			return true;

		/*
		 * only process schemas with a name that matches a user-selected pattern
		 */
		if (StringUtils.isNotBlank(appSchemaNameRegex)
				&& !name.matches(appSchemaNameRegex))
			return true;

		/*
		 * only process schemas in a namespace that matches a user-selected
		 * pattern
		 */
		if (StringUtils.isNotBlank(appSchemaNamespaceRegex)
				&& !ns.matches(appSchemaNamespaceRegex))
			return true;

		return false;
	}

	public String targetClassName(String rule) {
		String[] ra = rule.toLowerCase().split("-", 4);
		if (ra.length != 4)
			return null;

		if (ra[1].equals("xsd"))
			return Options.TargetXmlSchemaClass;
		else if (ra[1].equals("json"))
			return Options.TargetJsonSchemaClass;
		else if (ra[1].equals("fc"))
			return Options.TargetFeatureCatalogueClass;
		else if (ra[1].equals("rdf"))
			return Options.TargetRDFClass;
		else if (ra[1].equals("sql"))
			return Options.TargetSQLClass;
		else if (ra[1].equals("owl"))
			return Options.TargetOWLISO19150Class;
		else if (ra[1].equals("arcgis"))
			return Options.TargetArcGISWorkspaceClass;
		else if (ra[1].equals("rep"))
			return Options.TargetReplicationSchemaClass;
		else if (ra[1].equals("asm"))
			return Options.TargetApplicationSchemaMetadata;
		else if (ra[1].equals("exp"))
			return Options.TargetModelExport;
		else if (ra[1].equals("cdb"))
			return Options.TargetCDB;
		else if (ra[1].equals("cldml"))
			return Options.TargetCodeListDictionariesML;
		else if (ra[1].equals("ldp"))
			return Options.TargetLdproxy;

		return null;
	}

	public Options() {
		setStandardParameters();
	}

	/**
	 * @return the inputTargetConfigs
	 */
	public List<TargetConfiguration> getInputTargetConfigs() {
		return inputTargetConfigs;
	}

	/**
	 * @return the inputTransformerConfigs
	 */
	public List<TransformerConfiguration> getInputTransformerConfigs() {
		return inputTransformerConfigs;
	}

	/**
	 * @param currentProcessConfig
	 *                                 the currentProcessConfig to set
	 */
	public void setCurrentProcessConfig(
			ProcessConfiguration currentProcessConfig) {
		this.currentProcessConfig = currentProcessConfig;
	}

	/**
	 * @return the configuration of the process that is currently being
	 *         executed; can be <code>null</code> during the input loading phase
	 */
	public ProcessConfiguration getCurrentProcessConfig() {
		return currentProcessConfig;
	}

	public List<TargetConfiguration> getTargetConfigurations() {
		return this.targetConfigs;
	}

	public Map<String, TransformerConfiguration> getTransformerConfigs() {
		return this.transformerConfigs;
	}

	/**
	 * @return the value of the 'id' attribute of the 'input' configuration
	 *         element (or the default value as defined by the
	 *         {@link #INPUTELEMENTID} field), if the attribute was not set in
	 *         the configuration.
	 */
	public String getInputId() {
		return inputId;
	}

	/**
	 * @return the temporary directory for the ShapeChange run; will be created
	 *         if it does not already exist
	 */
	public File getTmpDir() {

		if (tmpDir == null) {

			String tmpDirPath = this.parameter(TMP_DIR_PATH_PARAM);
			if (tmpDirPath == null) {
				tmpDirPath = DEFAULT_TMP_DIR_PATH;
			}

			tmpDir = new File(tmpDirPath);
		}

		if (!tmpDir.exists()) {
			try {
				FileUtils.forceMkdir(tmpDir);
			} catch (IOException e) {
				e.printStackTrace(System.err);
			}
		}

		return tmpDir;
	}

	/**
	 * @return the directory in which images (i.e. diagrams) from the input
	 *         model can be stored; if it did not exist yet, it will be created
	 */
	public File imageTmpDir() {

		if (imageTmpDir == null) {

			imageTmpDir = new File(getTmpDir(), "images");
		}

		if (!imageTmpDir.exists()) {
			try {
				FileUtils.forceMkdir(imageTmpDir);
			} catch (IOException e) {
				e.printStackTrace(System.err);
			}
		}

		return imageTmpDir;
	}

	/**
	 * @return the directory in which linked documents from the input model can
	 *         be stored; if it did not exist yet, it will be created
	 */
	public File linkedDocumentsTmpDir() {

		if (linkedDocTmpDir == null) {

			linkedDocTmpDir = new File(getTmpDir(), "linkedDocuments");
		}

		if (!linkedDocTmpDir.exists()) {
			try {
				FileUtils.forceMkdir(linkedDocTmpDir);
			} catch (IOException e) {
				e.printStackTrace(System.err);
			}
		}

		return linkedDocTmpDir;
	}

	/**
	 * @return the excludedPackages for the model (may be empty but not
	 *         <code>null</code>).
	 */
	public Set<String> getExcludedPackages() {

		Set<String> excludedPackages = new HashSet<String>();

		String value = parameter(PARAM_INPUT_EXCLUDED_PACKAGES);

		if (value != null) {

			value = value.trim();

			if (value.length() > 0) {

				String[] values = value.split(",");

				for (String v : values) {
					excludedPackages.add(v.trim());
				}
			}
		}

		return excludedPackages;
	}

	/**
	 * Store AIXM schema infos for global use.
	 *
	 * @param schemaInfos
	 */
	public void setAIXMSchemaInfos(Map<String, AIXMSchemaInfo> schemaInfos) {
		this.schemaInfos = schemaInfos;
	}

	/**
	 * Retrieve AIXM schema infos that have been stored previously.
	 *
	 * @return map with schema infos (key: info object id; value: AIXMSchemaInfo
	 *         for the object)
	 */
	public Map<String, AIXMSchemaInfo> getAIXMSchemaInfos() {
		return schemaInfos;
	}

	/**
	 * 
	 * @return RFC 5646 language code of the primary language to use in targets
	 *         and transformers
	 */
	public String language() {

		return this.language;
	}

	public boolean useStringInterning() {

		return this.useStringInterning;
	}

	public boolean dontConstructAssociationNames() {
		return this.dontConstructAssociationNames;
	}

	public boolean allowAllTags() {
		return this.allowAllTags;
	}
	
	public boolean allowAllStereotypes() {
		return this.allowAllStereotypes;
	}
	
	public Set<String> addedStereotypes() {
	    return this.addedStereotypes;
	}

	/**
	 * Depending upon whether or not string interning shall be used during
	 * processing, this method interns the given string. <code>null</code> is
	 * simply returned.
	 *
	 * @param string
	 * @return
	 */
	public String internalize(String string) {

		if (string == null) {
			return null;
		} else if (useStringInterning) {
			return string.intern();
		} else {
			return string;
		}
	}

	/**
	 * Depending upon whether or not string interning shall be used during
	 * processing, this method interns the given string array or an internized
	 * copy.
	 *
	 * @param array
	 * @return
	 */
	public String[] internalize(String[] array) {
		if (useStringInterning) {
			String[] result = new String[array.length];
			int i = 0;
			for (String s : array)
				result[i++] = s.intern();
			return result;
		} else {
			return array;
		}
	}

	/**
	 * Depending on the tagged value implementation we want (map: default;
	 * array: better memory footprint for large models) this returns the one
	 * selected in the configuration.
	 */
	private boolean useTaggedValuesArray() {
		String tvImpl = parameter(PARAM_TAGGED_VALUE_IMPL);
		return (tvImpl != null && tvImpl.equalsIgnoreCase("array"));
	}

	public TaggedValues taggedValueFactory() {
		TaggedValues result;
		if (useTaggedValuesArray()) {
			result = new TaggedValuesCacheArray(this);
		} else {
			result = new TaggedValuesCacheMap(this);
		}
		return result;
	}

	public TaggedValues taggedValueFactory(int size) {
		TaggedValues result;
		if (useTaggedValuesArray()) {
			result = size < 0 ? new TaggedValuesCacheArray(this)
					: new TaggedValuesCacheArray(size, this);
		} else {
			result = size < 0 ? new TaggedValuesCacheMap(this)
					: new TaggedValuesCacheMap(size, this);
		}
		return result;
	}

	/**
	 * @param original
	 * @param tagList
	 * @return can be empty but not <code>null</code>
	 */
	public TaggedValues taggedValueFactory(TaggedValues original,
			String tagList) {
		TaggedValues result;
		if (useTaggedValuesArray()) {
			result = new TaggedValuesCacheArray(original, tagList, this);
		} else {
			result = new TaggedValuesCacheMap(original, tagList, this);
		}
		return result;
	}

	public TaggedValueNormalizer taggedValueNormalizer() {

		if (this.tvNormalizer == null) {
			this.tvNormalizer = new TaggedValueNormalizer(this);
		}

		return this.tvNormalizer;
	}

	/**
	 * @param original
	 * @return can be empty but not <code>null</code>
	 */
	public TaggedValues taggedValueFactory(TaggedValues original) {
		TaggedValues result;
		if (useTaggedValuesArray()) {
			result = new TaggedValuesCacheArray(original, this);
		} else {
			result = new TaggedValuesCacheMap(original, this);
		}
		return result;
	}

	public Stereotypes stereotypesFactory() {
		return new StereotypesCacheSet(this);
	}

	public Stereotypes stereotypesFactory(Stereotypes stereotypes) {
		return new StereotypesCacheSet(stereotypes, this);
	}

	/**
	 * @return values that, if one of them is being set as the 'status' tagged
	 *         value of a class, will lead to the class not being loaded; can be
	 *         empty but not <code>null</code>
	 */
	public Set<String> prohibitedStatusValuesWhenLoadingClasses() {

		if (!prohibitedStatusValuesWhenLoadingClasses_accessed) {

			prohibitedStatusValuesWhenLoadingClasses_accessed = true;

			if (this.hasParameter(null,
					PARAM_PROHIBIT_LOADING_CLASSES_WITH_STATUS_TV)) {

				prohibitedStatusValuesWhenLoadingClasses = new HashSet<String>(
						this.parameterAsStringList(null,
								PARAM_PROHIBIT_LOADING_CLASSES_WITH_STATUS_TV,
								DEFAULT_FOR_PROHIBIT_LOADING_CLASSES_WITH_STATUS_TV,
								true, true));
			} else {

				prohibitedStatusValuesWhenLoadingClasses = new HashSet<String>();
			}
		}

		return prohibitedStatusValuesWhenLoadingClasses;
	}

	public InputConfiguration getInputConfig() {
		return this.inputConfig;
	}
}
