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
 * (c) 2002-2014 interactive instruments GmbH, Bonn, Germany
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

package de.interactive_instruments.shapechange.core.target.ontology;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.RDFWriterI;
import org.apache.jena.reasoner.ValidityReport;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

import de.interactive_instruments.shapechange.core.MessageSource;
import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ProcessConfiguration;
import de.interactive_instruments.shapechange.core.ProcessRuleSet;
import de.interactive_instruments.shapechange.core.PropertyConversionParameter;
import de.interactive_instruments.shapechange.core.RuleRegistry;
import de.interactive_instruments.shapechange.core.ShapeChangeAbortException;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.TargetOwlConfiguration;
import de.interactive_instruments.shapechange.core.ShapeChangeResult.MessageContext;
import de.interactive_instruments.shapechange.core.model.ClassInfo;
import de.interactive_instruments.shapechange.core.model.Info;
import de.interactive_instruments.shapechange.core.model.Model;
import de.interactive_instruments.shapechange.core.model.PackageInfo;
import de.interactive_instruments.shapechange.core.model.PropertyInfo;
import de.interactive_instruments.shapechange.core.target.SingleTarget;
import de.interactive_instruments.shapechange.core.target.TargetUtil;

/**
 * UML to RDF/OWL/SKOS (based on ISO 19150-2)
 * 
 * @author Johannes Echterhoff, Clemens Portele
 * 
 */
public class OWLISO19150 implements SingleTarget, MessageSource {

    public static final String RDF_NS_W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema#";
    public static final String RDF_NS_W3C_RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    public static final String RDF_NS_W3C_RDFS = "http://www.w3.org/2000/01/rdf-schema#";
    public static final String RDF_NS_W3C_OWL = "http://www.w3.org/2002/07/owl#";
    public static final String RDF_NS_DC = "http://purl.org/dc/elements/1.1/";
    public static final String RDF_NS_DCT = "http://purl.org/dc/terms/";
    public static final String RDF_NS_W3C_SKOS = "http://www.w3.org/2004/02/skos/core#";
    public static final String RDF_NS_ISO_19150_2 = "http://def.isotc211.org/iso19150/-2/2012/base#";
    public static final String RDF_NS_OGC_GEOSPARQL = "http://www.opengis.net/ont/geosparql#";
    public static final String RDF_NS_ISO_GFM = "http://def.isotc211.org/iso19109/2013/GeneralFeatureModel#";

    public static final String PREFIX_ISO_19150_2 = "iso19150-2";

    public static final String NS_XMLNS = "http://www.w3.org/2000/xmlns/";
    public static final String NS_QNAME_ERROR = "http://example.org/qname/error/";
    public static final String NS_QNAME_ERROR_PREFIX = "qnerr";

    /**
     * NOTE: this tagged value is usually set by ShapeChange internally; if it is
     * explicitly set in the model for whatever reason, then it needs to be made
     * known to ShapeChange via the "addTaggedValues" or "representTaggedValues"
     * configuration parameters.
     */
    public static final String TV_UNION_SET = "SC_UNION_SET";
    public static final String TV_SKOS_CONCEPT_SCHEME_SUBCLASS_NAME = "skosConceptSchemeSubclassName";
    public static final String TV_BROADER_LISTED_VALUE = "broaderListedValue";

    /**
     * If this rule is enabled, ontologies will be created for selected schema, but
     * not for all of their child packages.
     */
    public static final String RULE_OWL_PKG_SINGLE_ONTOLOGY_PER_SCHEMA = "rule-owl-pkg-singleOntologyPerSchema";

    public static final String RULE_OWL_PKG_ONTOLOGY_NAME_BY_TAGGED_VALUE = "rule-owl-pkg-ontologyName-byTaggedValue";

    /**
     * If this rule is enabled, ontology names will be constructed using the path of
     * packages (usually from a leaf package to its main schema package). This rule
     * changes ontology names only if the rule
     * {@value #RULE_OWL_PKG_SINGLE_ONTOLOGY_PER_SCHEMA} is not in effect (because
     * otherwise child packages will not be considered).
     */
    public static final String RULE_OWL_PKG_ONTOLOGY_NAME_WITH_PATH = "rule-owl-pkg-ontologyName-withPath";

    public static final String RULE_OWL_PKG_ONTOLOGY_NAME_CODE = "rule-owl-pkg-ontologyName-code";

    public static final String RULE_OWL_PKG_ONTOLOGY_NAME_ISO191502 = "rule-owl-pkg-ontologyName-iso191502";

    public static final String RULE_OWL_PKG_ONTOLOGY_NAME_APPEND_VERSION = "rule-owl-pkg-ontologyName-appendVersion";

    /**
     * If {@value #RULE_OWL_PKG_VERSION_IRI} and
     * {@value #RULE_OWL_PKG_ONTOLOGY_NAME_APPEND_VERSION} are both enabled, and the
     * package has version information, then this rule prevents the version to be
     * appended again to the versionIRI (because it will already be added to the RDF
     * namespace).
     */
    public static final String RULE_OWL_PKG_VERSION_IRI_AVOID_DUPLICATE_VERSION = "rule-owl-pkg-versionIRI-avoid-duplicate-version";

    /**
     * If this rule is included and a package that is converted into an ontology has
     * version information, then the versionIRI of the ontology is constructed as
     * follows: 'rdfNamespace' + 'version'.
     */
    public static final String RULE_OWL_PKG_VERSION_IRI = "rule-owl-pkg-versionIRI";

    public static final String RULE_OWL_PKG_VERSION_INFO = "rule-owl-pkg-versionInfo";

    public static final String RULE_OWL_ALL_CONSTRAINTS_HUMAN_READABLE_TEXT_ONLY = "rule-owl-all-constraints-humanReadableTextOnly";
    public static final String RULE_OWL_ALL_CONSTRAINTS_BY_CONSTRAINT_MAPPING = "rule-owl-all-constraints-byConstraintMapping";

    public static final String RULE_OWL_CLS_19150_2_ISABSTRACT = "rule-owl-cls-iso191502IsAbstract";

    public static final String RULE_OWL_PROP_PROPERTYENRICHMENT = "rule-owl-prop-propertyEnrichment";

    /**
     * If this rule is included, the base ontology defined by ISO 19150-2 with IRI
     * http://def.isotc211.org/iso19150-2/2012/base# is imported by each ontology.
     */
    public static final String RULE_OWL_PKG_IMPORT_191502BASE = "rule-owl-pkg-importISO191502Base";

    public static final String RULE_OWL_PROP_GENERAL = "rule-owl-prop-general";

    public static final String RULE_OWL_PROP_RANGE_LOCAL_UNIVERSAL_QUANTIFICATION = "rule-owl-prop-range-local-withUniversalQuantification";
    public static final String RULE_OWL_PROP_RANGE_GLOBAL = "rule-owl-prop-range-global";

    public static final String RULE_OWL_PROP_GLOBAL_SCOPE_ATTRIBUTES = "rule-owl-prop-globalScopeAttributes";
    public static final String RULE_OWL_PROP_LOCAL_SCOPE_ALL = "rule-owl-prop-localScopeAll";
    public static final String RULE_OWL_PROP_GLOBAL_SCOPE_BY_CONVERSION_PARAMETER = "rule-owl-prop-globalScopeByConversionParameter";
    public static final String RULE_OWL_PROP_GLOBAL_SCOPE_BY_UNIQUE_PROPERTY_NAME = "rule-owl-prop-globalScopeByUniquePropertyName";

    public static final String RULE_OWL_PROP_MULTIPLICITY_QUALIFIED_RESTRICTION = "rule-owl-prop-multiplicityAsQualifiedCardinalityRestriction";
    public static final String RULE_OWL_PROP_MULTIPLICITY_UNQUALIFIED_RESTRICTION = "rule-owl-prop-multiplicityAsUnqualifiedCardinalityRestriction";

    /**
     * If this rule is included, minCardinality is set to 0 for voidable properties
     */
    public static final String RULE_OWL_PROP_VOIDABLE_AS_MINCARDINALITY0 = "rule-owl-prop-voidable-as-minCardinality0";

    public static final String RULE_OWL_PROP_MAPPING_COMPARE_SPECIFICATIONS = "rule-owl-prop-mapping-compare-specifications";

    public static final String RULE_OWL_PROP_ISO191502_ASSOCIATION_NAME = "rule-owl-prop-iso191502AssociationName";

    public static final String RULE_OWL_PROP_INVERSEOF = "rule-owl-prop-inverseOf";

    public static final String RULE_OWL_PROP_ISO191502_AGGREGATION = "rule-owl-prop-iso191502Aggregation";

    public static final String RULE_OWL_PROP_ISO191502_NAMING = "rule-owl-prop-iso191502-naming";

    public static final String RULE_OWL_PROP_EXTERNAL_REFERENCE = "rule-owl-prop-external-reference";

    public static final String RULE_OWL_PKG_DCT_SOURCE_TITLE = "rule-owl-pkg-dctSourceTitle";
    /**
     * If this conversion rule is enabled, then a code list that has the vocabulary
     * or codeList tagged value is not represented as part of the OWL ontology
     * derived from the application schema.
     */
    public static final String RULE_OWL_CLS_CODELIST_EXTERNAL = "rule-owl-cls-codelist-external";
    public static final String RULE_OWL_CLS_CODELIST_191502 = "rule-owl-cls-codelist-19150-2";
    public static final String RULE_OWL_CLS_CODELIST_191502_CONCEPTSCHEMESUBCLASS = "rule-owl-cls-codelist-19150-2-conceptSchemeSubclass";
    public static final String RULE_OWL_CLS_CODELIST_191502_DIFFERENTINDIVIDUALS = "rule-owl-cls-codelist-19150-2-differentIndividuals";
    public static final String RULE_OWL_CLS_CODELIST_191502_CLASSINDIFFERENTNAMESPACE = "rule-owl-cls-codelist-19150-2-owlClassInDifferentNamespace";
    public static final String RULE_OWL_CLS_CODELIST_191502_SKOS_COLLECTION = "rule-owl-cls-codelist-19150-2-skos-collection";
    public static final String RULE_OWL_CLS_CODELIST_191502_OBJECTONEOFFORENUMERATION = "rule-owl-cls-codelist-19150-2-objectOneOfForEnumeration";
    public static final String RULE_OWL_PROP_CODE_BROADER_BY_BROADER_LISTED_VALUE = "rule-owl-prop-code-broader-byBroaderListedValue";

    public static final String RULE_OWL_CLS_ENUMERATION_AS_CODELIST = "rule-owl-cls-enumerationAsCodelist";

    public static final String RULE_OWL_CLS_ISO191502_ENUMERATION = "rule-owl-cls-iso191502Enumeration";

    public static final String RULE_OWL_CLS_GENERALIZATION = "rule-owl-cls-generalization";

    public static final String RULE_OWL_CLS_DISJOINT_CLASSES = "rule-owl-cls-disjoint-classes";

    public static final String RULE_OWL_CLS_ENCODE_FEATURETYPES = "rule-owl-cls-encode-featuretypes";
    public static final String RULE_OWL_CLS_ENCODE_OBJECTTYPES = "rule-owl-cls-encode-objecttypes";
    public static final String RULE_OWL_CLS_ENCODE_MIXINTYPES = "rule-owl-cls-encode-mixintypes";
    public static final String RULE_OWL_CLS_ENCODE_DATATYPES = "rule-owl-cls-encode-datatypes";
    public static final String RULE_OWL_CLS_ENCODE_BASICTYPES = "rule-owl-cls-encode-basictypes";

    public static final String RULE_OWL_CLS_UNION = "rule-owl-cls-union";
    /**
     * Adds class expression defining union semantics for those properties of a
     * class that have the same value for the tag {@value #TV_UNION_SET}.
     */
    public static final String RULE_OWL_CLS_UNION_SETS = "rule-owl-cls-unionSets";

    /**
     * 
     */
    public static final String PARAM_SOURCE = "source";

    public static final String PARAM_DEFAULT_TYPE_IMPL = "defaultTypeImplementation";

    public static final String PARAM_CODE_NAMESPACE = "codeNamespace";
    public static final String PARAM_CODE_NAMESPACE_FOR_ENUMERATIONS = "codeNamespaceForEnumerations";
    public static final String PARAM_CODE_LIST_OWL_CLASS_NAMESPACE = "codeListOwlClassNamespace";
    public static final String PARAM_CODE_LIST_OWL_CLASS_NAMESPACE_FOR_ENUMERATIONS = "codeListOwlClassNamespaceForEnumerations";

    public static final String PARAM_GENERAL_PROPERTY_NSABR = "generalPropertyNamespaceAbbreviation";
    /**
     * 
     */
    public static final String PARAM_LANGUAGE = "language";

    public static final String PARAM_OUTPUTFORMAT = "outputFormat";

    /**
     * 
     */
    public static final String PARAM_SOURCE_TAGGED_VALUE_NAME = "sourceTaggedValueName";

    public static final String PARAM_SKOS_CONCEPT_SCHEME_SUFFIX = "skosConceptSchemeSuffix";
    public static final String PARAM_SKOS_CONCEPT_SCHEME_SUBCLASS_NAME_SUFFIX = "skosConceptSchemeSubclassSuffix";

    /**
     * Defines the global URIbase for construction of the ontologyName (see
     * 19150-2package:ontologyName). If this parameter is not configured or empty,
     * the target namespace of the application schema is used as URIbase.
     */
    public static final String PARAM_URIBASE = "URIbase";

    /**
     * Define the value of the 'blockRules' property of the Apache Jena RDF writer
     * when using RDF/XML as output format. For further details on this property,
     * see the <a href=
     * "https://jena.apache.org/documentation/io/rdfxml_howto.html#advanced-rdfxml-output">
     * advanced RDF/XML output options of Apache Jena</a>. The parameter is
     * optional. It defaults to {@value #DEFAULT_RDFXMLWRITER_BLOCKRULES}.
     */
    public static final String PARAM_RDFXMLWRITER_BLOCKRULES = "rdfXmlWriterBlockRules";
    public static final String DEFAULT_RDFXMLWRITER_BLOCKRULES = "idAttr,propertyAttr";

    /**
     * Per 19150-2package:rdfNamespace, the default separator that is appended to
     * the 'ontologyName' when creating the 'rdfNamespace' is '#'. If this parameter
     * is included in the configuration, a different separator can be used (e.g.
     * '/'), even the empty string.
     */
    public static final String PARAM_RDF_NAMESPACE_SEPARATOR = "rdfNamespaceSeparator";

    /**
     * Name of the parameter whose value provides the name of the tagged value
     * which, if present on a package, defines the ontologyName (see
     * 19150-2owl:ontologyName) of the package.
     * <p>
     * The ontologyName is defined via the following rules, in descending priority:
     * <ul>
     * <li>If the configuration parameter
     * {@value #PARAM_ONTOLOGYNAME_TAGGED_VALUE_NAME} is set and an according tagged
     * value is set for the package its value is used.</li>
     * <li>If the configuration parameter {@value #PARAM_URIBASE} is set its value
     * is used for constructing the ontologyName as per 19150-2owl:ontologyName</li>
     * <li>Otherwise the targetNamespace of the package is used as URIbase</li>
     * </ul>
     */
    public static final String PARAM_ONTOLOGYNAME_TAGGED_VALUE_NAME = "ontologyNameTaggedValue";

    public static final String PARAM_ONTOLOGYNAME_CODE_NAME = "ontologyNameCode";

    /**
     * Provide a QName that identifies an RDF(S) or OWL property to use for encoding
     * an external reference, which is defined by tagged values 'codeList' and
     * 'vocabulary' on the value type of a UML property. The tagged value will be
     * encoded as IRI, the range of the chosen RDF(S)/OWL property should support
     * this. For further details, see {@value #RULE_OWL_PROP_EXTERNAL_REFERENCE}.
     * Default is 'rdfs:seeAlso'.
     */
    public static final String PARAM_PROP_EXTERNAL_REFERENCE_TARGET_PROPERTY = "propExternalReference_targetProperty";

    public static final String PARAM_SUPPRESS_MESSAGES_FOR_UNSUPPORTED_CLASS_CATEGORY = "suppressMessagesForUnsupportedCategoryOfClasses";

    /**
     * key: a package, value: the according ontology object
     */
    protected static SortedMap<PackageInfo, OntologyModel> ontologyByPiMap = new TreeMap<PackageInfo, OntologyModel>();

    /**
     * key: rdf namespace of an ontology document; value: the ontology document
     */
    protected static SortedMap<String, OntologyModel> ontologyByRdfNs = new TreeMap<String, OntologyModel>();

    protected static SortedMap<ClassInfo, OntologyModel> ontologyByCi = new TreeMap<ClassInfo, OntologyModel>();

    /**
     * key: xml prefix value, value: current counter (used to establish unique xml
     * prefixes for referencing elements from the same target namespace [which might
     * be contained in different ontologies])
     */
    protected static SortedMap<String, Integer> counterByXmlprefix = new TreeMap<String, Integer>();

    /**
     * key: namespace abbreviation / prefix; value: RDF namespace
     */
    protected static SortedMap<String, String> rdfNsByPrefix = new TreeMap<String, String>();

    protected static String codeNamespace = null;
    protected static String codeNamespaceForEnumerations = null;
    protected static String codeListOwlClassNamespace = null;
    protected static String codeListOwlClassNamespaceForEnumerations = null;
    protected static String prefixCodeNamespace = "c";
    protected static String prefixCodeNamespaceForEnumerations = "e";
    protected static String prefixCodeListOwlClassNamespace = "cc";
    protected static String prefixCodeListOwlClassNamespaceForEnumerations = "ce";
    protected static String propExternalReference_targetProperty = "rdfs:seeAlso";

    private static boolean error = false;
    private static boolean printed = false;

    private static String outputDirectory = null;
    private Options options = null;

    private static TargetOwlConfiguration config = null;

    private static ShapeChangeResult result = null;
    private static Model model = null;
    private static PackageInfo mainAppSchema = null;
    private static String mainAppSchemaNamespace = null;

    private static String skosConceptSchemeSuffix = "";
    private static String skosConceptSchemeSubclassSuffix = "";
    private static String source = null;
    private static String sourceTaggedValue = null;
    private static String uriBase = null;
    private static String rdfXmlWriterBlockRules = null;
    private static String rdfNamespaceSeparator = "#";
    private static String language = "en";
    private static String outputFormat = "TURTLE";
    private static RDFFormat rdfFormat = RDFFormat.TURTLE;
    private static String fileNameExtension = ".ttl";
    private static String ontologyNameTaggedValue = "ontologyName";
    private static String ontologyNameCode = null;
    private static String defaultTypeImplementation = null;
    private static boolean suppressMessagesForUnsupportedCategoryOfClasses = false;
    private static String generalPropertyNamespaceAbbreviation = null;

    /**
     * Will be populated when writeAll is called.
     * 
     * key: property-name '#' schema-name
     */
    private static SortedMap<String, OntologyModel> ontologyByPropertyConversionTargetReference = null;

    private static SortedMap<String, List<OntProperty>> subPropertyByURIOfSuperProperty = new TreeMap<>();

    @Override
    public String getTargetName() {
	return "ISO 19150-2 OWL Ontology";
    }

    // TBD: New diagnostics-only flag is to be considered
    public void initialise(PackageInfo p, Model m, Options o, ShapeChangeResult r, boolean diagOnly)
	    throws ShapeChangeAbortException {

	options = o;
	result = r;
	model = m;
	mainAppSchema = TargetUtil.findMainSchemaForSingleTargets(model.selectedSchemas(), o, r);

	ProcessConfiguration tmp = o.getCurrentProcessConfig();

	if (tmp instanceof TargetOwlConfiguration) {
	    config = (TargetOwlConfiguration) tmp;
	} else {
	    result.addError(this, 6);
	    throw new ShapeChangeAbortException();
	}

	/*
	 * We need to retrieve the output directory this way - rather than directly from
	 * the configuration - because the converter may modify the output directory
	 * location.
	 */
	outputDirectory = options.parameter(this.getClass().getName(), "outputDirectory");
	if (outputDirectory == null)
	    outputDirectory = options.parameter("outputDirectory");
	if (outputDirectory == null)
	    outputDirectory = ".";

	String outputFormatFromConfig = config.getParameterValue(PARAM_OUTPUTFORMAT);
	if (outputFormatFromConfig != null) {
	    outputFormat = outputFormatFromConfig;
	}

	if (outputFormat.equalsIgnoreCase("NTRIPLES")) {
	    rdfFormat = RDFFormat.NTRIPLES;
	    fileNameExtension = ".nt";
	} else if (outputFormat.equalsIgnoreCase("RDFXML")) {
	    rdfFormat = RDFFormat.RDFXML;
	    fileNameExtension = ".rdf";
	} else if (outputFormat.equalsIgnoreCase("JSONLD")) {
	    rdfFormat = RDFFormat.JSONLD;
	    fileNameExtension = ".jsonld";
	} else if (outputFormat.equalsIgnoreCase("RDFJSON")) {
	    rdfFormat = RDFFormat.RDFJSON;
	    fileNameExtension = ".rj";
	} else if (outputFormat.equalsIgnoreCase("TRIG")) {
	    rdfFormat = RDFFormat.TRIG;
	    fileNameExtension = ".trig";
	} else if (outputFormat.equalsIgnoreCase("NQUADS")) {
	    rdfFormat = RDFFormat.NQUADS;
	    fileNameExtension = ".nq";
	} else if (outputFormat.equalsIgnoreCase("TRIX")) {
	    rdfFormat = RDFFormat.TRIX;
	    fileNameExtension = ".trix";
	} else if (outputFormat.equalsIgnoreCase("RDFTHRFIT")) {
	    rdfFormat = RDFFormat.RDF_THRIFT;
	    fileNameExtension = ".trdf";
	} else {
	    // default is turtle
	    rdfFormat = RDFFormat.TURTLE;
	    fileNameExtension = ".ttl";
	}

	String sourceFromConfig = config.getParameterValue(PARAM_SOURCE);
	if (sourceFromConfig != null) {
	    source = sourceFromConfig;
	}

	generalPropertyNamespaceAbbreviation = config.getParameterValue(PARAM_GENERAL_PROPERTY_NSABR);
	/*
	 * NOTE: The configuration validator cheks that the target configuration
	 * contains a namespace definition with matching abbreviation, if a value is set
	 * for the parameter.
	 */

	String skosConceptSchemeSuffixFromConfig = config.getParameterValue(PARAM_SKOS_CONCEPT_SCHEME_SUFFIX);
	if (skosConceptSchemeSuffixFromConfig != null) {
	    skosConceptSchemeSuffix = skosConceptSchemeSuffixFromConfig;
	}

	String skosConceptSchemeSubclassSuffixFromConfig = config
		.getParameterValue(PARAM_SKOS_CONCEPT_SCHEME_SUBCLASS_NAME_SUFFIX);
	if (skosConceptSchemeSubclassSuffixFromConfig != null) {
	    skosConceptSchemeSubclassSuffix = skosConceptSchemeSubclassSuffixFromConfig;
	}

	String sourceTaggedValueNameFromConfig = config.getParameterValue(PARAM_SOURCE_TAGGED_VALUE_NAME);
	if (sourceTaggedValueNameFromConfig != null) {
	    sourceTaggedValue = sourceTaggedValueNameFromConfig;
	}

	String uriBaseFromConfig = config.getParameterValue(PARAM_URIBASE);
	if (uriBaseFromConfig != null) {
	    uriBase = uriBaseFromConfig;
	}

	rdfXmlWriterBlockRules = options.parameterAsString(this.getClass().getName(), PARAM_RDFXMLWRITER_BLOCKRULES,
		DEFAULT_RDFXMLWRITER_BLOCKRULES, false, true);

	String rdfNamespaceSeparatorFromConfig = config.getParameterValue(PARAM_RDF_NAMESPACE_SEPARATOR);
	if (rdfNamespaceSeparatorFromConfig != null) {
	    rdfNamespaceSeparator = rdfNamespaceSeparatorFromConfig.trim();
	}

	String langFromConfig = config.getParameterValue(PARAM_LANGUAGE);
	if (langFromConfig != null) {
	    language = langFromConfig;
	}

	String ontologyNameTaggedValueNameFromConfig = config.getParameterValue(PARAM_ONTOLOGYNAME_TAGGED_VALUE_NAME);
	if (ontologyNameTaggedValueNameFromConfig != null) {
	    ontologyNameTaggedValue = ontologyNameTaggedValueNameFromConfig;
	}

	String ontologyNameCodeFromConfig = config.getParameterValue(PARAM_ONTOLOGYNAME_CODE_NAME);
	if (ontologyNameCodeFromConfig != null) {
	    ontologyNameCode = ontologyNameCodeFromConfig;
	}

	String defaultTypeImplementation_tmp = config.getParameterValue(PARAM_DEFAULT_TYPE_IMPL);
	if (defaultTypeImplementation_tmp != null && defaultTypeImplementation_tmp.contains(":")) {
	    defaultTypeImplementation = defaultTypeImplementation_tmp;
	}

	String codeNamespace_tmp = config.getParameterValue(PARAM_CODE_NAMESPACE);
	if (codeNamespace_tmp != null && codeNamespace_tmp.trim().length() > 0) {
	    codeNamespace = codeNamespace_tmp.trim();
	}

	String codeNamespaceForEnumerations_tmp = config.getParameterValue(PARAM_CODE_NAMESPACE_FOR_ENUMERATIONS);
	if (codeNamespaceForEnumerations_tmp != null && codeNamespaceForEnumerations_tmp.trim().length() > 0) {
	    codeNamespaceForEnumerations = codeNamespaceForEnumerations_tmp.trim();
	}

	String codeListOwlClassNamespace_tmp = config.getParameterValue(PARAM_CODE_LIST_OWL_CLASS_NAMESPACE);
	if (codeListOwlClassNamespace_tmp != null && codeListOwlClassNamespace_tmp.trim().length() > 0) {
	    codeListOwlClassNamespace = codeListOwlClassNamespace_tmp.trim();
	}

	String codeListOwlClassNamespaceForEnumerations_tmp = config
		.getParameterValue(PARAM_CODE_LIST_OWL_CLASS_NAMESPACE_FOR_ENUMERATIONS);
	if (codeListOwlClassNamespaceForEnumerations_tmp != null
		&& codeListOwlClassNamespaceForEnumerations_tmp.trim().length() > 0) {
	    codeListOwlClassNamespaceForEnumerations = codeListOwlClassNamespaceForEnumerations_tmp.trim();
	}

	String propExternalReference_targetProperty_tmp = config
		.getParameterValue(PARAM_PROP_EXTERNAL_REFERENCE_TARGET_PROPERTY);
	if (propExternalReference_targetProperty_tmp != null
		&& propExternalReference_targetProperty_tmp.contains(":")) {
	    propExternalReference_targetProperty = propExternalReference_targetProperty_tmp.trim();
	}

	suppressMessagesForUnsupportedCategoryOfClasses = options.parameterAsBoolean(this.getClass().getName(),
		PARAM_SUPPRESS_MESSAGES_FOR_UNSUPPORTED_CLASS_CATEGORY, false);

	/*
	 * Initialize an ontology for the package and - unless stated otherwise via a
	 * rule - for the sub-packages in the same target namespace. Also create
	 * ontologies for code lists (classes - if in separate namespace - and their
	 * individuals).
	 */

	// create new ontology / ontologies
	String xmlPrefixOfMainSchema = p.xmlns();

	OntologyModel od = new OntologyModel(p, m, o, r, xmlPrefixOfMainSchema, this);

	if (ontologyByRdfNs.containsKey(od.getRdfNamespace())) {
	    /*
	     * can happen if multiple schemas are merged into a single ontology by setting a
	     * common URI base; we re-use the existing OntologyModel
	     */
	    od = ontologyByRdfNs.get(od.getRdfNamespace());
	}

	ontologyByPiMap.put(p, od);
	ontologyByRdfNs.put(od.getRdfNamespace(), od);

	if (mainAppSchema != null && mainAppSchema == p) {
	    mainAppSchemaNamespace = od.getRdfNamespace();
	}

	if (p.matches(RULE_OWL_PKG_SINGLE_ONTOLOGY_PER_SCHEMA)) {

	    SortedSet<ClassInfo> ontclasses = new TreeSet<ClassInfo>();
	    ontclasses.addAll(model.classes(p));
	    createAdditionalOntologyModels(ontclasses, od.getPrefix(), od.getRdfNamespace(), od.getName(), od.getPath(),
		    od.getFileName(), p.version());

	} else {

	    // DEFAULT BEHAVIOR

	    /*
	     * Create ontologies for all sub packages that are in the same target namespace.
	     * 
	     * Future work: exclude packages that contain no classes
	     */
	    SortedSet<PackageInfo> subPkgsInSameTNS = subPackagesInSameTNS(p);

	    int counter = 1;
	    for (PackageInfo subPi : subPkgsInSameTNS) {
		String xmlprefix = xmlPrefixOfMainSchema + counter;
		counter++;
		OntologyModel odSub = new OntologyModel(subPi, m, o, r, xmlprefix, this);

		ontologyByPiMap.put(subPi, odSub);
		ontologyByRdfNs.put(odSub.getRdfNamespace(), odSub);

		// if the package contains a code list or enumeration, create
		// additional OntologyModels as necessary
		SortedSet<ClassInfo> ontclasses = new TreeSet<ClassInfo>();
		ontclasses.addAll(subPi.containedClasses());
		createAdditionalOntologyModels(ontclasses, odSub.getPrefix(), odSub.getRdfNamespace(), odSub.getName(),
			odSub.getPath(), odSub.getFileName(), subPi.version());

	    }
	}
    }

    private void createAdditionalOntologyModels(SortedSet<ClassInfo> classes, String basePrefix, String baseRdfns,
	    String baseName, String path, String baseFileName, String version) throws ShapeChangeAbortException {

	for (ClassInfo ci : classes) {

	    if (isEncodedAsCodeList(ci)) {

		/*
		 * We have a code list / enumeration that is not mapped and shall be encoded
		 * under rule-owl-cls-codelist-19150-2
		 */

		/*
		 * Create an OntologyModel for the individuals derived from this class, if such
		 * a model doesn't already exist.
		 */
		String prefixOntIndividuals, nameOntIndividuals, pathOntIndividuals, fileNameOntIndividuals;
		if (ci.category() == Options.ENUMERATION && codeNamespaceForEnumerations != null) {
		    prefixOntIndividuals = prefixCodeNamespaceForEnumerations;
		    nameOntIndividuals = codeNamespaceForEnumerations;
		    pathOntIndividuals = "";
		    fileNameOntIndividuals = "enums";
		} else if (codeNamespace != null) {
		    prefixOntIndividuals = prefixCodeNamespace;
		    nameOntIndividuals = codeNamespace;
		    pathOntIndividuals = "";
		    fileNameOntIndividuals = "codes";
		} else {
		    prefixOntIndividuals = basePrefix + "code";
		    nameOntIndividuals = baseName + "/" + "code";
		    pathOntIndividuals = path;
		    fileNameOntIndividuals = baseFileName + "_codes";
		}

		String rdfnsOntIndividuals = nameOntIndividuals + getRdfNamespaceSeparator();

		/*
		 * We have to check if the ontology exists because use of the codeNamespace
		 * /codeNamespaceForEnumeration will result in a single, global ontology model
		 * for code list / enumeration individuals, and this method may be called
		 * multiple times (for the code lists / enumerations in different packages).
		 */
		if (!ontologyByRdfNs.containsKey(rdfnsOntIndividuals)) {
		    OntologyModel om = new OntologyModel(model, options, result, prefixOntIndividuals,
			    rdfnsOntIndividuals, nameOntIndividuals, pathOntIndividuals, fileNameOntIndividuals, this);
		    ontologyByRdfNs.put(rdfnsOntIndividuals, om);
		}

		/*
		 * determine if the owl:Class of the code list / enumeration shall be in a
		 * different namespace
		 */
		if (ci.matches(OWLISO19150.RULE_OWL_CLS_CODELIST_191502_CLASSINDIFFERENTNAMESPACE)) {

		    if (codeListOwlClassNamespace == null && codeListOwlClassNamespaceForEnumerations == null) {
			// rule does not have any effect
			result.addWarning(this, 9);
		    }

		    if (ci.category() == Options.ENUMERATION && codeListOwlClassNamespaceForEnumerations != null) {
			/*
			 * Create OntologyModel for enumerations if it does not exist yet.
			 * 
			 * NOTE: If the 'codeListOwlClassNamespaceForEnumerations' is equal to the
			 * 'codeNamespaceForEnumerations' then the ontology already exists.
			 */
			String rdfns = codeListOwlClassNamespaceForEnumerations + getRdfNamespaceSeparator();
			if (!ontologyByRdfNs.containsKey(rdfns)) {
			    OntologyModel om = new OntologyModel(model, options, result,
				    prefixCodeListOwlClassNamespaceForEnumerations, rdfns,
				    codeListOwlClassNamespaceForEnumerations, "", "enumerations", this);
			    ontologyByRdfNs.put(rdfns, om);
			}

		    } else if (codeListOwlClassNamespace != null) {

			/*
			 * Create OntologyModel for code lists if it does not exist yet. If
			 * 'codeListOwlClassNamespaceForEnumerations' is null then an enumeration will
			 * be added here as well.
			 * 
			 * NOTE: If the 'codeListOwlClassNamespace' is equal to the 'codeNamespace' then
			 * the ontology already exists.
			 */

			String rdfns = codeListOwlClassNamespace + getRdfNamespaceSeparator();
			if (!ontologyByRdfNs.containsKey(rdfns)) {
			    OntologyModel om = new OntologyModel(model, options, result,
				    prefixCodeListOwlClassNamespace, rdfns, codeListOwlClassNamespace, "", "code_lists",
				    this);
			    ontologyByRdfNs.put(rdfns, om);
			}
		    }

		} else {
		    /*
		     * the class itself will be added to the OntologyModel to which its package
		     * belongs
		     */
		}
	    }
	}

    }

    /**
     * Recursively searches in all sub packages of the given package, to identify
     * each sub package that is in the same target namespace as the given package.
     * 
     * @param pi
     * @return set of all descendant packages that are in the same target namespace
     *         as pi
     */
    private SortedSet<PackageInfo> subPackagesInSameTNS(PackageInfo pi) {

	SortedSet<PackageInfo> result = new TreeSet<PackageInfo>();

	SortedSet<PackageInfo> childPis = pi.containedPackages();

	if (childPis != null) {

	    for (PackageInfo childPi : childPis) {
		if (childPi.targetNamespace().equals(pi.targetNamespace())) {
		    result.add(childPi);
		    result.addAll(subPackagesInSameTNS(childPi));
		} else {
		    // the child package is in a different schema /
		    // targetNamespace
		}
	    }
	}

	return result;
    }

    public void process(ClassInfo ci) {

	if (error)
	    return;

	// determine ontology to which the class should be added
	OntologyModel om = computeRelevantOntology(ci);

	if (om == null) {

	    // ontology document could not be found
	    MessageContext mc = result.addError(this, 2, ci.name());
	    if (mc != null) {
		mc.addDetail(this, 10000, ci.fullName());
	    }

	} else {

	    ontologyByCi.put(ci, om);
	    om.addClass(ci);
	}
    }

    /**
     * @param ci a code list or enumeration
     * @return tbd
     */
    public OntologyModel computeRelevantOntologyForIndividuals(ClassInfo ci) {

	PackageInfo pCi = ci.pkg();
	PackageInfo relevantPi;

	if (pCi.matches(RULE_OWL_PKG_SINGLE_ONTOLOGY_PER_SCHEMA)) {

	    relevantPi = model.schemaPackage(ci);

	} else {
	    /*
	     * follow default ISO 19150-2 behavior, which is to have an ontology for each
	     * package
	     */
	    relevantPi = pCi;
	}

	OntologyModel om = ontologyByPiMap.get(relevantPi);

	if (isEncodedAsCodeList(ci)) {

	    /*
	     * We have a code list / enumeration that is not mapped and shall be encoded
	     * under rule-owl-cls-codelist-19150-2
	     */

	    String nameOntIndividuals;
	    if (ci.category() == Options.ENUMERATION && codeNamespaceForEnumerations != null) {
		nameOntIndividuals = codeNamespaceForEnumerations;
	    } else if (codeNamespace != null) {
		nameOntIndividuals = codeNamespace;
	    } else {
		nameOntIndividuals = om.getName() + "/" + "code";
	    }

	    String rdfnsOntIndividuals = nameOntIndividuals + getRdfNamespaceSeparator();

	    return ontologyByRdfNs.get(rdfnsOntIndividuals);
	} else {
	    // for a code list or enumeration that is encoded as a code list,
	    // there must be an OntologyModel for the individuals
	    return null;
	}
    }

    /**
     * @param prefix tbd
     * @return the ontology document that has the given prefix, or <code>null</code>
     *         if no such ontology could be found
     */
    public OntologyModel computeRelevantOntology(String prefix) {

	for (OntologyModel om : ontologyByRdfNs.values()) {
	    if (om.getPrefix().equals(prefix)) {
		return om;
	    }
	}

	return null;
    }

    /**
     * Identifies the ontology document to which the given class belongs. Usually
     * this is the ontology that represents the package the class is in, but that
     * also depends upon the configured rules.
     * 
     * @param ci tbd
     * @return the ontology document for the class, or <code>null</code> if no
     *         ontology could be found for the class
     */
    public OntologyModel computeRelevantOntology(ClassInfo ci) {

	if (ontologyByCi.containsKey(ci)) {

	    return ontologyByCi.get(ci);

	} else {

	    PackageInfo pCi = ci.pkg();
	    PackageInfo relevantPi;

	    if (pCi.matches(RULE_OWL_PKG_SINGLE_ONTOLOGY_PER_SCHEMA)) {

		relevantPi = model.schemaPackage(ci);

		if (relevantPi == null) {
		    /*
		     * can happen if, for example, the inClass of a reverse property is in a schema
		     * that was not selected for processing
		     */
		    return null;
		}

	    } else {
		/*
		 * follow default ISO 19150-2 behavior, which is to have an ontology for each
		 * package
		 */
		relevantPi = pCi;
	    }

	    OntologyModel om = ontologyByPiMap.get(relevantPi);

	    if (isEncodedAsCodeList(ci)) {

		/*
		 * We have a code list / enumeration that is not mapped and shall be encoded
		 * under rule-owl-cls-codelist-19150-2
		 */

		/*
		 * determine if the owl:Class of the code list / enumeration shall be in a
		 * different namespace
		 */
		if (ci.matches(OWLISO19150.RULE_OWL_CLS_CODELIST_191502_CLASSINDIFFERENTNAMESPACE)) {

		    if (ci.category() == Options.ENUMERATION && codeListOwlClassNamespaceForEnumerations != null) {

			String rdfns = codeListOwlClassNamespaceForEnumerations + getRdfNamespaceSeparator();
			om = ontologyByRdfNs.get(rdfns);

		    } else if (codeListOwlClassNamespace != null) {

			String rdfns = codeListOwlClassNamespace + getRdfNamespaceSeparator();
			om = ontologyByRdfNs.get(rdfns);

		    } else {
			/*
			 * Rule does not have any effect - the class will be encoded in the
			 * OntologyModel that applies to the package (which we already retrieved)
			 */
		    }

		} else {
		    /*
		     * The class itself will be added to the OntologyModel that applies to its
		     * package.
		     */
		}
	    }

	    return om;
	}
    }

    /**
     * @return <code>true</code> if RULE_OWL_CLS_CODELIST_191502 is enabled, ci is
     *         either an unmapped code list or an unmapped enumeration for which
     *         RULE_OWL_CLS_ENUMERATION_AS_CODELIST applies, and ci is not encoded
     *         under RULE_OWL_CLS_CODELIST_EXTERNAL; else <code>false</code>
     */
    private boolean isEncodedAsCodeList(ClassInfo ci) {

	/*
	 * NOTE: the check to see if ci is encoded under RULE_OWL_CLS_CODELIST_EXTERNAL
	 * must take place after the check that ci is an unmapped enumeration that is
	 * encoded under RULE_OWL_CLS_ENUMERATION_AS_CODELIST.
	 */

	boolean result = ci.matches(RULE_OWL_CLS_CODELIST_191502)

		&& ((ci.category() == Options.CODELIST && config.getTypeMapEntry(ci) == null)
			|| (ci.category() == Options.ENUMERATION && config.getTypeMapEntry(ci) == null
				&& ci.matches(RULE_OWL_CLS_ENUMERATION_AS_CODELIST)))

		&& !(ci.matches(RULE_OWL_CLS_CODELIST_EXTERNAL)
			&& !ci.taggedValuesForTagList("codeList,vocabulary").isEmpty());

	return result;
    }

    /**
     * Searches for the ontology that contains the property that is identified by
     * the 'target' and 'targetSchema' fields of the given conversion parameter.
     * 
     * @param pcp tbd
     * @return the ontology that contains the global property to which the
     *         conversion parameter maps; can be <code>null</code> if no applicable
     *         ontology was found
     */
    public OntologyModel computeRelevantOntologyForTargetMapping(PropertyConversionParameter pcp) {

	if (pcp == null || !pcp.hasTarget()) {
	    return null;
	}

	return ontologyByPropertyConversionTargetReference.get(pcp.getTarget() + "#" + pcp.getTargetSchema());
    }

    public void write() {
	// ignore - this is a SingleTarget
    }

    public void writeAll(ShapeChangeResult r) {

	if (error || printed)
	    return;

	/*
	 * identify properties to encode as global ones (only for feature types,
	 * interfaces, datatypes and basictypes)
	 */

	ontologyByPropertyConversionTargetReference = new TreeMap<String, OntologyModel>();

	outer: for (PackageInfo schema : model.selectedSchemas()) {

	    for (ClassInfo ci : model.classes(schema)) {

		if (ci.category() == Options.FEATURE || ci.category() == Options.OBJECT
			|| ci.category() == Options.DATATYPE || ci.category() == Options.MIXIN
			|| ci.category() == Options.BASICTYPE) {

		    for (PropertyInfo prop : ci.properties().values()) {

			if (!prop.matches(RULE_OWL_PROP_GLOBAL_SCOPE_BY_CONVERSION_PARAMETER)
				|| !prop.matches(RULE_OWL_PROP_GENERAL)) {
			    // no need to look further
			    break outer;
			}

			PropertyConversionParameter pcp = config.getPropertyConversionParameter(prop);
			if (pcp != null && pcp.isGlobal() && !pcp.hasTarget()) {

			    OntologyModel om = computeRelevantOntology(prop.inClass());
			    ontologyByPropertyConversionTargetReference.put(pcp.getProperty() + "#" + pcp.getSchema(),
				    om);
			}
		    }
		}
	    }
	}

	for (OntologyModel om : ontologyByRdfNs.values()) {
	    om.createClasses();
	}

	addGeneralProperties();

	for (OntologyModel om : ontologyByRdfNs.values()) {
	    om.createProperties();
	}

	for (OntologyModel om : ontologyByRdfNs.values()) {
	    om.createAdditionalClassDetails();
	}

	for (OntologyModel om : ontologyByRdfNs.values()) {
	    om.createAdditionalPropertyDetails();
	}

	for (OntologyModel om : ontologyByRdfNs.values()) {
	    om.addGeneralPropertyDomainByUnionOfSubPropertyDomains();
	}

	// output ontologies in folder hierarchy as determined by their
	// path - using normalized package names

	for (OntologyModel om : ontologyByRdfNs.values()) {

	    print(om, outputDirectory, r);
	}

	printed = true;
    }

    private void addGeneralProperties() {

	for (RdfGeneralProperty gp : this.getConfig().getGeneralProperties()) {

	    String nsabr = gp.getNamespaceAbbreviation();
	    String namespace = null;

	    if (nsabr == null) {

		if (StringUtils.isNotBlank(generalPropertyNamespaceAbbreviation)) {

		    nsabr = generalPropertyNamespaceAbbreviation;
		    /*
		     * Validation of the configuration already ensured that a namespace with that
		     * abbreviation is configured.
		     */
		    namespace = config.fullNamespace(nsabr);

		} else {

		    if (StringUtils.isBlank(mainAppSchemaNamespace)) {
			result.addError(this, 100, gp.getName());
		    } else {
			namespace = mainAppSchemaNamespace;
		    }
		}

	    } else {

		/*
		 * Check available ontology models as well as namespaces from configuration
		 */

		if (config.hasNamespaceWithAbbreviation(nsabr)) {
		    namespace = config.fullNamespace(nsabr);
		} else {
		    for (OntologyModel om : ontologyByRdfNs.values()) {
			if (om.getPrefix().equals(nsabr)) {
			    namespace = om.getRdfNamespace();
			    break;
			}
		    }
		    if (namespace == null) {
			result.addError(this, 101, gp.getName(), nsabr);
		    }
		}
	    }

	    if (namespace != null) {

		// store the namespace for later use
		gp.setNamespace(namespace);

		// Find or create the ontology to add the property
		OntologyModel om = null;

		if (ontologyByRdfNs.containsKey(namespace)) {

		    om = ontologyByRdfNs.get(namespace);

		} else {

		    // create a new ontology model

		    String ontologyName = StringUtils.endsWithAny(namespace, new String[] { "#", "/" })
			    ? namespace.substring(0, namespace.length() - 1)
			    : namespace;
		    String path = "";
		    String location = config.locationOfNamespace(namespace);
		    String fileName;

		    if (StringUtils.isNotBlank(location)) {
			fileName = FilenameUtils.getBaseName(location);
		    } else {
			fileName = namespace.replaceAll("[^a-zA-Z]", "_");
			fileName = fileName.replaceAll("_+", "_");
		    }

		    try {
			om = new OntologyModel(model, options, result, nsabr, namespace, ontologyName, path, fileName,
				this);
			ontologyByRdfNs.put(namespace, om);

		    } catch (ShapeChangeAbortException e) {
			result.addError(this, 102, gp.getName(), e.getMessage());
			continue;
		    }
		}

		// now add the general property to the model
		om.createGeneralProperty(nsabr, namespace, gp);
	    }
	}

    }

    public void print(OntologyModel om, String outputDirectory, ShapeChangeResult r) {

	OntModel ontmodel = om.getOntologyModel();
	String ontName = om.getName();
	String path = om.getPath();
	String filenameWithoutExtension = om.getFileName();

	ValidityReport report = ontmodel.validate();
	if (report != null && !report.isValid())
	    result.addError(this, 7, ontName, r.toString());

	String outDirForOntology = outputDirectory + path;

	// Check if we can use the output directory
	File outputDirectoryFile = new File(outDirForOntology);

	boolean exi = outputDirectoryFile.exists();
	if (!exi) {
	    outputDirectoryFile.mkdirs();
	    exi = outputDirectoryFile.exists();
	}
	boolean dir = outputDirectoryFile.isDirectory();
	boolean wrt = outputDirectoryFile.canWrite();
	boolean rea = outputDirectoryFile.canRead();

	if (!exi || !dir || !wrt || !rea) {
	    r.addFatalError(this, 8, ontName, outDirForOntology);
	    return;
	}

	/*
	 * Uses OutputStreamWriter instead of FileWriter to set character encoding
	 */

	String filename = filenameWithoutExtension + fileNameExtension;

	String fname = outDirForOntology + "/" + filename;

	File outFile = new File(outputDirectoryFile, filename);

	try (OutputStream fout = new FileOutputStream(outFile); OutputStream bout = new BufferedOutputStream(fout)) {

	    String canpath = new File(fname).getCanonicalPath();
	    r.addDebug(this, 20000, ontName, canpath);

	    if (rdfFormat.equals(RDFFormat.RDFXML)) {

		RDFWriterI writer = ontmodel.getWriter("RDF/XML");
		writer.setProperty("xmlbase", om.getName());
		writer.setProperty("blockRules", rdfXmlWriterBlockRules);
		writer.setProperty("relativeURIs", "");
		writer.write(ontmodel, bout, null);

	    } else {

		RDFDataMgr.write(bout, ontmodel, rdfFormat);
	    }

	    r.addResult(getTargetName(), outDirForOntology, filename, ontName);

	} catch (Exception e) {
	    r.addError(this, 5, fname);
	    e.printStackTrace(System.err);
	}
    }

    public void reset() {

	OWLISO19150.ontologyByPiMap = new TreeMap<PackageInfo, OntologyModel>();
	OWLISO19150.ontologyByRdfNs = new TreeMap<String, OntologyModel>();
	OWLISO19150.ontologyByCi = new TreeMap<ClassInfo, OntologyModel>();
	OWLISO19150.counterByXmlprefix = new TreeMap<String, Integer>();
	OWLISO19150.rdfNsByPrefix = new TreeMap<String, String>();

	OWLISO19150.codeNamespace = null;
	OWLISO19150.codeNamespaceForEnumerations = null;
	OWLISO19150.codeListOwlClassNamespace = null;
	OWLISO19150.codeListOwlClassNamespaceForEnumerations = null;
	OWLISO19150.prefixCodeNamespace = "c";
	OWLISO19150.prefixCodeNamespaceForEnumerations = "e";
	OWLISO19150.prefixCodeListOwlClassNamespace = "cc";
	OWLISO19150.prefixCodeListOwlClassNamespaceForEnumerations = "ce";
	OWLISO19150.propExternalReference_targetProperty = "rdfs:seeAlso";

	OWLISO19150.error = false;
	OWLISO19150.printed = false;

	OWLISO19150.outputDirectory = null;

	OWLISO19150.config = null;

	OWLISO19150.result = null;
	OWLISO19150.model = null;
	OWLISO19150.mainAppSchema = null;
	OWLISO19150.mainAppSchemaNamespace = null;

	OWLISO19150.skosConceptSchemeSuffix = "";
	OWLISO19150.skosConceptSchemeSubclassSuffix = "";
	OWLISO19150.source = null;
	OWLISO19150.sourceTaggedValue = null;
	OWLISO19150.uriBase = null;
	OWLISO19150.rdfXmlWriterBlockRules = null;
	OWLISO19150.rdfNamespaceSeparator = "#";
	OWLISO19150.language = "en";
	OWLISO19150.outputFormat = "TURTLE";
	OWLISO19150.rdfFormat = RDFFormat.TURTLE;
	OWLISO19150.fileNameExtension = ".ttl";
	OWLISO19150.ontologyNameTaggedValue = "ontologyName";
	OWLISO19150.ontologyNameCode = null;
	OWLISO19150.defaultTypeImplementation = null;
	OWLISO19150.suppressMessagesForUnsupportedCategoryOfClasses = false;

	OWLISO19150.ontologyByPropertyConversionTargetReference = null;

	OWLISO19150.generalPropertyNamespaceAbbreviation = null;

	OWLISO19150.subPropertyByURIOfSuperProperty = new TreeMap<>();
    }

    /**
     * Computes the value for the dct:source that qualifies an ontology element. The
     * value is computed according to the following instructions, in descending
     * order:
     * <ul>
     * <li>if the configuration parameter {@value #PARAM_SOURCE_TAGGED_VALUE_NAME}
     * is set and the info object has this tagged value, its value is used</li>
     * <li>if the configuration parameter {@value #PARAM_SOURCE} is set then its
     * value is used</li>
     * <li>otherwise "FIXME" is returned</li>
     * </ul>
     * 
     * @param i tbd
     * @return tbd
     */
    public String computeSource(Info i) {

	if (sourceTaggedValue != null) {

	    String sourceTV = i.taggedValue(sourceTaggedValue);

	    if (StringUtils.isNotBlank(sourceTV)) {
		return sourceTV;
	    }
	}

	if (source != null) {
	    return source;
	}

	return "FIXME";
    }

    /**
     * @return the suffix defined by the configuration, or the empty string
     */
    public String getSkosConceptSchemeSuffix() {
	return skosConceptSchemeSuffix;
    }

    /**
     * @return the suffix defined by the configuration, or the empty string
     */
    public String getSkosConceptSchemeSubclassSuffix() {
	return skosConceptSchemeSubclassSuffix;
    }

    /**
     * @param rdfns tbd
     * @return the abbreviation/prefix belonging to the given rdf namespace, or
     *         <code>null</code> if no such prefix was found.
     */
    public String computePrefixForRdfNamespace(String rdfns) {

	if (NS_QNAME_ERROR.equals(rdfns)) {
	    return NS_QNAME_ERROR_PREFIX;
	}

	// try to identify via namespace configuration info
	String nsabr = config.nsabrForNamespace(rdfns);

	if (nsabr == null) {

	    // try to find namespace via local ontologies
	    if (ontologyByRdfNs.containsKey(rdfns)) {

		nsabr = ontologyByRdfNs.get(rdfns).getPrefix();
	    }
	}

	return nsabr;
    }

    /**
     * @return the config
     */
    public TargetOwlConfiguration getConfig() {
	return config;
    }

    /**
     * @return the uriBase
     */
    public String getUriBase() {
	return uriBase;
    }

    public boolean isSuppressMessagesForUnsupportedCategoryOfClasses() {
	return suppressMessagesForUnsupportedCategoryOfClasses;
    }

    /**
     * @return the rdfNamespaceSeparator
     */
    public String getRdfNamespaceSeparator() {
	return rdfNamespaceSeparator;
    }

    /**
     * @return the language
     */
    public String getLanguage() {
	return language;
    }

    public String getOutputFormat() {
	return outputFormat;
    }

    public String getOutputFileNameExtension() {
	return fileNameExtension;
    }

    public RDFFormat getRDFFormat() {
	return rdfFormat;
    }

    /**
     * @return the ontologyNameTaggedValue
     */
    public String getOntologyNameTaggedValue() {
	return ontologyNameTaggedValue;
    }

    /**
     * @return value of parameter {@value #PARAM_ONTOLOGYNAME_CODE_NAME} if set,
     *         else <code>null</code>
     */
    public String getOntologyNameCodeParameterValue() {
	return ontologyNameCode;
    }

    /**
     * @return QName identifying the resource to use as default implementation for
     *         schema types
     */
    public String getDefaultTypeImplementation() {
	return defaultTypeImplementation;
    }

    /**
     * @return QName identifying the property to use for encoding an external
     *         reference defined by tagged values 'codeList' and 'vocabulary' on the
     *         value type of a property
     */
    public String getPropExternalReferenceTargetProperty() {
	return propExternalReference_targetProperty;
    }

    /**
     * @return the codeNamespace
     */
    public static String getCodeNamespace() {
	return codeNamespace;
    }

    /**
     * @return the codeNamespaceForEnumerations
     */
    public static String getCodeNamespaceForEnumerations() {
	return codeNamespaceForEnumerations;
    }

    /**
     * @return the codeListOwlClassNamespace
     */
    public static String getCodeListOwlClassNamespace() {
	return codeListOwlClassNamespace;
    }

    /**
     * @return the codeListOwlClassNamespaceForEnumerations
     */
    public static String getCodeListOwlClassNamespaceForEnumerations() {
	return codeListOwlClassNamespaceForEnumerations;
    }

    /**
     * This method returns messages belonging to the target by their message number.
     * The organization corresponds to the logic in module ShapeChangeResult. All
     * functions in that class, which require a message number can be redirected to
     * the function at hand.
     * 
     * @param mnr Message number
     * @return Message text, including $x$ substitution points.
     */
    public String message(int mnr) {
	// Get the message proper and return it with an identification prefixed
	String mess = messageText(mnr);
	if (mess == null)
	    return null;
	String prefix = "";
	if (mess.startsWith("??")) {
	    prefix = "??";
	    mess = mess.substring(2);
	}
	return prefix + "OWL ISO 19150 Target: " + mess;
    }

    public void registerSubPropertyOfRelationship(String superPropertyURI, OntProperty subProperty) {

	List<OntProperty> subProps;
	if (subPropertyByURIOfSuperProperty.containsKey(superPropertyURI)) {
	    subProps = subPropertyByURIOfSuperProperty.get(superPropertyURI);
	} else {
	    subProps = new ArrayList<>();
	    subPropertyByURIOfSuperProperty.put(superPropertyURI, subProps);
	}

	if (!subProps.contains(subProperty)) {
	    subProps.add(subProperty);
	}
    }

    /**
     * 
     * @param propertyURI tbd
     * @return A map with the direct and indirect sub properties registered for the
     *         property with given URI, sorted by their URI; can be empty but not
     *         <code>null</code>.
     */
    public SortedMap<String, OntProperty> getAllSubproperties(String propertyURI) {

	SortedMap<String, OntProperty> result = new TreeMap<>();

	/*
	 * Note: We need to prevent an endless loop (caused by a sub property A of
	 * property B also being - maybe indirectly - a super property of B).
	 */

	Set<String> visitedSuperProperties = new HashSet<String>();
	Set<OntProperty> subProperties = getAllSubproperties(propertyURI, visitedSuperProperties);

	for (OntProperty subProp : subProperties) {
	    result.put(subProp.getURI(), subProp);
	}

	return result;
    }

    /**
     * @param propertyURI
     * @param visitedSuperProperties set to keep track of the URIs of super
     *                               properties that have been visited
     * @return A set of the direct and indirect sub properties registered for the
     *         property with given URI; can be empty but not <code>null</code>.
     */
    private Set<OntProperty> getAllSubproperties(String propertyURI, Set<String> visitedSuperProperties) {

	Set<OntProperty> subProperties = new HashSet<OntProperty>();

	if (!visitedSuperProperties.contains(propertyURI)) {

	    visitedSuperProperties.add(propertyURI);

	    List<OntProperty> directSubProperties = subPropertyByURIOfSuperProperty.get(propertyURI);

	    if (directSubProperties != null) {

		subProperties.addAll(directSubProperties);

		// drill down
		for (OntProperty subProp : directSubProperties) {
		    subProperties.addAll(getAllSubproperties(subProp.getURI(), visitedSuperProperties));
		}
	    }
	}

	return subProperties;
    }

    @Override
    public String getTargetIdentifier() {
	return "owl";
    }

    @Override
    public String getDefaultEncodingRule() {
	return "iso19150_2014";
    }

    @Override
    public void registerRulesAndRequirements(RuleRegistry r) {

	// declare default encoding rule
	ProcessRuleSet iso19150_2014Prs = new ProcessRuleSet("iso19150_2014","*");
	r.addRuleSet(iso19150_2014Prs);

	// declare optional rules
	r.addRule("rule-owl-all-constraints-byConstraintMapping");
	r.addRule("rule-owl-all-constraints-humanReadableTextOnly");

	r.addRule("rule-owl-pkg-importISO191502Base");
	r.addRule("rule-owl-pkg-dctSourceTitle");
	r.addRule("rule-owl-pkg-ontologyName-appendVersion");
	r.addRule("rule-owl-pkg-ontologyName-byTaggedValue");
	r.addRule("rule-owl-pkg-ontologyName-code");
	r.addRule("rule-owl-pkg-ontologyName-withPath");
	r.addRule("rule-owl-pkg-ontologyName-iso191502");
	r.addRule("rule-owl-pkg-versionInfo");
	r.addRule("rule-owl-pkg-versionIRI");
	r.addRule("rule-owl-pkg-versionIRI-avoid-duplicate-version");
	r.addRule("rule-owl-pkg-singleOntologyPerSchema");

	r.addRule("rule-owl-cls-codelist-external");
	r.addRule("rule-owl-cls-codelist-19150-2");
	r.addRule("rule-owl-cls-codelist-19150-2-conceptSchemeSubclass");
	r.addRule("rule-owl-cls-codelist-19150-2-differentIndividuals");
	r.addRule("rule-owl-cls-codelist-19150-2-owlClassInDifferentNamespace");
	r.addRule("rule-owl-cls-codelist-19150-2-objectOneOfForEnumeration");
	r.addRule("rule-owl-cls-codelist-19150-2-skos-collection");
	r.addRule("rule-owl-cls-disjoint-classes");
	r.addRule("rule-owl-cls-iso191502Enumeration");

	r.addRule("rule-owl-cls-encode-featuretypes");
	r.addRule("rule-owl-cls-encode-basictypes");
	r.addRule("rule-owl-cls-encode-datatypes");
	r.addRule("rule-owl-cls-encode-mixintypes");
	r.addRule("rule-owl-cls-encode-objecttypes");
	r.addRule("rule-owl-cls-enumerationAsCodelist");
	r.addRule("rule-owl-cls-generalization");
	r.addRule("rule-owl-cls-iso191502IsAbstract");
	r.addRule("rule-owl-cls-union");
	r.addRule("rule-owl-cls-unionSets");

	r.addRule("rule-owl-prop-code-broader-byBroaderListedValue");
	r.addRule("rule-owl-prop-external-reference");
	r.addRule("rule-owl-prop-general");
	r.addRule("rule-owl-prop-globalScopeAttributes");
	r.addRule("rule-owl-prop-globalScopeByConversionParameter");
	r.addRule("rule-owl-prop-globalScopeByUniquePropertyName");
	r.addRule("rule-owl-prop-inverseOf");
	r.addRule("rule-owl-prop-iso191502Aggregation");
	r.addRule("rule-owl-prop-iso191502AssociationName");
	r.addRule("rule-owl-prop-iso191502-naming");
	r.addRule("rule-owl-prop-labelFromLocalName");
	r.addRule("rule-owl-prop-localScopeAll");
	r.addRule("rule-owl-prop-mapping-compare-specifications");
	r.addRule("rule-owl-prop-multiplicityAsQualifiedCardinalityRestriction");
	r.addRule("rule-owl-prop-multiplicityAsUnqualifiedCardinalityRestriction");
	r.addRule("rule-owl-prop-propertyEnrichment");
	r.addRule("rule-owl-prop-range-global");
	r.addRule("rule-owl-prop-range-local-withUniversalQuantification");
	r.addRule("rule-owl-prop-voidable-as-minCardinality0");
    }

    /**
     * This is the message text provision proper. It returns a message for a number.
     * 
     * @param mnr Message number
     * @return Message text or null
     */
    protected String messageText(int mnr) {

	switch (mnr) {
	case 1:
	    return "Could not find an ontology document for package '$1$', which was determined to be the relevant one for class '$2$'.";
	case 2:
	    return "Rule '" + RULE_OWL_PKG_SINGLE_ONTOLOGY_PER_SCHEMA
		    + "' is in effect, but no schema package was found for class '$1$'.";
	case 3:
	    return "Unsupported class category ($1$).";
	case 4:
	    return "Output directory is not accessible.";
	case 5:
	    return "Ontology document with name '$1$' could not be created.";
	case 6:
	    return "Target configuration type is incorrect. Expected a TargetOwl(Configuration).";
	case 7:
	    return "Ontology '$1$' is not valid: $2$";
	case 8:
	    return "Cannot print ontology '$1$' in directory '$2$'.";
	case 9:
	    return "??" + RULE_OWL_CLS_CODELIST_191502_CLASSINDIFFERENTNAMESPACE
		    + " is enabled, but neither the configuration parameter '" + PARAM_CODE_LIST_OWL_CLASS_NAMESPACE
		    + "' nor the parameter '" + PARAM_CODE_LIST_OWL_CLASS_NAMESPACE_FOR_ENUMERATIONS
		    + "' are set to a specific value. The rule does not have any effect.";
	case 10:
	    return "??";

	case 100:
	    return "No namespace abbreviation is defined for general property '$1$'. Parameter '"
		    + PARAM_GENERAL_PROPERTY_NSABR
		    + "' is not set, and a main schema that would define the namespace of the property could not be identified. The property will be ignored.";
	case 101:
	    return "Namespace abbreviation for general property '$1$' is '$2$'. The configuration does not define a namespace with that abbreviation, and none of the ontologies created by the target has that abbreviation, either. The general property will be ignored.";
	case 102:
	    return "Exception encountered while creating new ontology model for general property '$1$'. The property will be ignored. Exception message is: $2$";

	case 10000:
	    return "--- Context - class: '$1$'";
	case 20000:
	    return "Writing ontology '$1$' to file '$2$'.";
	}
	return null;
    }
}
