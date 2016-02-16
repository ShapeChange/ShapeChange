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
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */

package de.interactive_instruments.ShapeChange.Target.Ontology;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessConfiguration;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.TargetIdentification;
import de.interactive_instruments.ShapeChange.TargetOwlConfiguration;
import de.interactive_instruments.ShapeChange.Model.AssociationInfo;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Target.SingleTarget;

/**
 * @author Johannes Echterhoff
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
	public static final String RDF_NS_ISO_19150_2 = "http://def.isotc211.org/iso19150-2/2012/base#";
	public static final String RDF_NS_OGC_GEOSPARQL = "http://www.opengis.net/ont/geosparql#";
	public static final String RDF_NS_ISO_GFM = "http://def.isotc211.org/iso19109/2013/GeneralFeatureModel#";
	
	public static final String PREFIX_ISO_19150_2 = "iso19150-2";

	public static final String NS_XMLNS = "http://www.w3.org/2000/xmlns/";

	/**
	 * If this rule is enabled, ontologies will be created for selected schema,
	 * but not for all of their child packages.
	 */
	public static final String RULE_OWL_PKG_SINGLE_ONTOLOGY_PER_SCHEMA = "rule-owl-pkg-singleOntologyPerSchema";
	/**
	 * If this rule is enabled, ontology names will be constructed using the
	 * path of packages (usually from a leaf package to its main schema
	 * package). This rule changes ontology names only if the rule
	 * {@value #RULE_OWL_PKG_SINGLE_ONTOLOGY_PER_SCHEMA} is not in effect
	 * (because otherwise child packages will not be considered).
	 */
	public static final String RULE_OWL_PKG_PATH_IN_ONTOLOGY_NAME = "rule-owl-pkg-pathInOntologyName";

	/**
	 * If this rule is included, the target will create constraint definitions.
	 * Constraints on properties (not for union properties) and classes are
	 * supported.
	 */
	public static final String RULE_OWL_ALL_CONSTRAINTS = "rule-owl-all-constraints";

	/**
	 * If this rule is included, each feature type definition gets a subClassOf
	 * declaration to the GeoSPARQL defined FeatureType class.
	 */
	public static final String RULE_OWL_CLS_GEOSPARQL_FEATURES = "rule-owl-cls-geosparql-features";

	/**
	 * If this rule is included, each feature type definition gets a subClassOf
	 * declaration to the ISO 19150-2 defined FeatureType class (which defines
	 * the according stereotype) as well as AnyFeature.
	 */
	public static final String RULE_OWL_CLS_19150_2_FEATURES = "rule-owl-cls-19150-2-features";

	/**
	 * If this rule is included, allValuesFrom restrictions are not included in
	 * the ontology
	 */
	public static final String RULE_OWL_PROP_SUPPRESS_ALLVALUESFROM_RESTRICTIONS = "rule-owl-prop-suppress-allValuesFrom-restrictions";

	/**
	 * If this rule is included, cardinality restrictions are not included in
	 * the ontology
	 */
	public static final String RULE_OWL_PROP_SUPPRESS_CARDINALITY_RESTRICTIONS = "rule-owl-prop-suppress-cardinality-restrictions";

	/**
	 * If this rule is included, minCardinality is set to 0 for voidable properties
	 */
	public static final String RULE_OWL_PROP_VOIDABLE_AS_MINCARDINALITY0 = "rule-owl-prop-voidable-as-minCardinality0";
	
	/**
	 * If this rule is included, association names are not included in
	 * the ontology
	 */
	public static final String RULE_OWL_PROP_SUPPRESS_ASSOCIATION_NAMES = "rule-owl-prop-suppress-asociation-names";

	/**
	 * If this rule is included, dc:source in not included except on the ontology subject
	 */
	public static final String RULE_OWL_ALL_SUPPRESS_DC_SOURCE = "rule-owl-all-suppress-dc-source";

	/**
	 * If this rule is included, code lists are not represented as part of the RDF vocabulary and where available
	 * the vocabulary or codelist tagged value is used for the rdfs:range. If not set, owl:Class is used.
	 */
	public static final String RULE_OWL_CLS_CODELIST_EXTERNAL = "rule-owl-cls-codelist-external";

	public static final String RULE_OWL_PKG_APP_SCHEMA_CODE = "rule-owl-pkg-app-schema-code";
	
	public static final String PARAM_VERSIONINFO = "versionInfo";
	/**
	 * 
	 */
	public static final String PARAM_SOURCE = "source";
	/**
	 * 
	 */
	public static final String PARAM_LANGUAGE = "language";
	/**
	 * 
	 */
	public static final String PARAM_SOURCE_TAGGED_VALUE_NAME = "sourceTaggedValueName";

	/**
	 * Defines the global URIbase for construction of the ontologyName (see
	 * 19150-2owl:ontologyName).
	 * <p>
	 * The ontologyName is defined via the following rules, in descending
	 * priority:
	 * <ul>
	 * <li>If the configuration parameter
	 * {@value #PARAM_ONTOLOGYNAME_TAGGED_VALUE_NAME} is set and an according
	 * tagged value is set for the package its value is used.</li>
	 * <li>If the configuration parameter {@value #PARAM_URIBASE} is set its
	 * value is used for constructing the ontologyName as per
	 * 19150-2owl:ontologyName</li>
	 * <li>Otherwise the targetNamespace of the package is used as URIbase</li>
	 * </ul>
	 */
	public static String PARAM_URIBASE = "URIbase";

	/**
	 * Name of the parameter whose value provides the name of the tagged value
	 * which, if present on a package, defines the ontologyName (see
	 * 19150-2owl:ontologyName) of the package.
	 * <p>
	 * The ontologyName is defined via the following rules, in descending
	 * priority:
	 * <ul>
	 * <li>If the configuration parameter
	 * {@value #PARAM_ONTOLOGYNAME_TAGGED_VALUE_NAME} is set and an according
	 * tagged value is set for the package its value is used.</li>
	 * <li>If the configuration parameter {@value #PARAM_URIBASE} is set its
	 * value is used for constructing the ontologyName as per
	 * 19150-2owl:ontologyName</li>
	 * <li>Otherwise the targetNamespace of the package is used as URIbase</li>
	 * </ul>
	 */
	public static String PARAM_ONTOLOGYNAME_TAGGED_VALUE_NAME = "ontologyName_TaggedValue_Name";

	public static String PARAM_GLOBALPROPERTIES = "globalProperties";

	/**
	 * key: a package, value: the according ontology object
	 */
	protected static Map<PackageInfo, OntologyDocument> ontologyByPiMap = new HashMap<PackageInfo, OntologyDocument>();

	/**
	 * key: rdf namespace of an ontology document; value: the ontology document
	 */
	protected static Map<String, OntologyDocument> ontologyByRdfNs = new HashMap<String, OntologyDocument>();
	/**
	 * key: code namespace of an ontology document; value: the ontology document
	 */
	protected static Map<String, OntologyDocument> ontologyByCodeNs = new HashMap<String, OntologyDocument>();

	/**
	 * key: targetNamespace, value: main schema package
	 */
	protected static Map<String, PackageInfo> schemaByTargetNamespace = new HashMap<String, PackageInfo>();

	/**
	 * key: xml prefix value, value: current counter (used to establish unique
	 * xml prefixes for referencing elements from the same target namespace
	 * [which might be contained in different ontologies])
	 */
	protected static Map<String, Integer> counterByXmlprefix = new HashMap<String, Integer>();

	/**
	 * key: namespace abbreviation / prefix; value: RDF namespace
	 */
	protected static Map<String, String> rdfNsByPrefix = new HashMap<String, String>();

	protected static Set<AssociationInfo> processedAssociations = new HashSet<AssociationInfo>();

	private static boolean error = false;
	private static boolean printed = false;

	private static String outputDirectory = null;
	private Options options = null;

	private TargetOwlConfiguration config = null;

	private static ShapeChangeResult result = null;

	private static String versionInfo = "FIXME";
	private static String source = null;
	private static String sourceTaggedValue = null;
	private static String uriBase = null;
	private static String language = "en";
	private static String ontologyNameTaggedValue = null;
	private static Set<String> globalPropertyNames = new HashSet<String>();

	public int getTargetID() {
		return TargetIdentification.OWLISO19150.getId();
	}

	// TBD: New diagnostics-only flag is to be considered
	public void initialise(PackageInfo p, Model m, Options o,
			ShapeChangeResult r, boolean diagOnly)
			throws ShapeChangeAbortException {

		options = o;
		result = r;

		ProcessConfiguration tmp = o.getCurrentProcessConfig();

		if (tmp instanceof TargetOwlConfiguration) {
			config = (TargetOwlConfiguration) tmp;
		} else {
			result.addError(this, 6);
			throw new ShapeChangeAbortException();
		}

		// we need to retrieve the output directory this way because the
		// converter may modify the output directory location
		outputDirectory = options.parameter(this.getClass().getName(),
				"outputDirectory");
		if (outputDirectory == null)
			outputDirectory = options.parameter("outputDirectory");
		if (outputDirectory == null)
			outputDirectory = ".";

		String versionInfoFromConfig = config
				.getParameterValue(PARAM_VERSIONINFO);
		if (versionInfoFromConfig != null) {
			versionInfo = versionInfoFromConfig;
		}

		String sourceFromConfig = config.getParameterValue(PARAM_SOURCE);
		if (sourceFromConfig != null) {
			source = sourceFromConfig;
		}

		String sourceTaggedValueNameFromConfig = config
				.getParameterValue(PARAM_SOURCE_TAGGED_VALUE_NAME);
		if (sourceTaggedValueNameFromConfig != null) {
			sourceTaggedValue = sourceTaggedValueNameFromConfig;
		}

		String uriBaseFromConfig = config.getParameterValue(PARAM_URIBASE);
		if (uriBaseFromConfig != null) {
			uriBase = uriBaseFromConfig;
		}

		String langFromConfig = config.getParameterValue(PARAM_LANGUAGE);
		if (langFromConfig != null) {
			language = langFromConfig;
		}

		String ontologyNameTaggedValueNameFromConfig = config
				.getParameterValue(PARAM_ONTOLOGYNAME_TAGGED_VALUE_NAME);
		if (ontologyNameTaggedValueNameFromConfig != null) {
			ontologyNameTaggedValue = ontologyNameTaggedValueNameFromConfig;
		}

		String[] globalPropertyNamesFromConfig = config.getParameterValues(PARAM_GLOBALPROPERTIES);
		if (globalPropertyNamesFromConfig != null) {
			for (String s : globalPropertyNamesFromConfig) {
				globalPropertyNames.add(s);
			}
		}

		// initialize an ontology for the package and - unless stated
		// otherwise via a rule - for the sub-packages in the same target
		// namespace

		// create new ontology / ontologies
		String xmlPrefixOfMainSchema = p.xmlns();

		// OntologyDocument od = new OntologyDocumentRDFXML(p, m, o, r, xmlPrefixOfMainSchema, this);
		OntologyDocument od = new OntologyModel(p, m, o, r, xmlPrefixOfMainSchema, this);
		
		ontologyByPiMap.put(p, od);
		ontologyByRdfNs.put(od.getRdfNamespace(), od);
		ontologyByCodeNs.put(od.getCodeNamespace(), od);

		if (!p.matches(RULE_OWL_PKG_SINGLE_ONTOLOGY_PER_SCHEMA)) {

			/*
			 * also create ontologies for all sub packages that are in the same
			 * target namespace
			 * 
			 * Future work: exclude packages that contain no classes
			 */
			Set<PackageInfo> subPkgsInSameTNS = subPackagesInSameTNS(p);

			int counter = 1;
			for (PackageInfo subPi : subPkgsInSameTNS) {
				String xmlprefix = xmlPrefixOfMainSchema + counter;
				counter++;
				OntologyDocument odSub = new OntologyModel(subPi, m, o, r, xmlprefix, this);
				ontologyByPiMap.put(subPi, odSub);

				ontologyByRdfNs.put(odSub.getRdfNamespace(), odSub);
				ontologyByCodeNs.put(odSub.getCodeNamespace(), odSub);
			}
		}

		/*
		 * keep track of main schema packages so that we can look them up by
		 * targetNamespace
		 */
		schemaByTargetNamespace.put(p.targetNamespace(), p);

	}

	/**
	 * Recursively searches in all sub packages of the given package, to
	 * identify each sub package that is in the same target namespace as the
	 * given package.
	 * 
	 * @param pi
	 * @param result
	 *            set of all descendant packages that are in the same target
	 *            namespace as pi
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
		OntologyDocument od = computeRelevantOntology(ci);

		if (od == null) {
			// ontology document could not be found
			MessageContext mc = result.addError(this, 2, ci.name());
			if (mc != null) {
				mc.addDetail(this, 10000, ci.fullName());
			}
		} else {
			/*
			 * remember class assignment to ontology for complete processing
			 * once all schema/ontologies are known (as this is a SingleTarget)
			 */
			od.addClass(ci);
		}
	}

	/**
	 * Identifies the ontology document to which the given class belongs.
	 * Usually this is the ontology that represents the package the class is in,
	 * but that also depends upon the configured rules.
	 * 
	 * @param ci
	 * @return the ontology document for the class, or <code>null</code> if no
	 *         ontology could be found for the class
	 */
	public OntologyDocument computeRelevantOntology(ClassInfo ci) {

		PackageInfo pCi = ci.pkg();
		PackageInfo relevantPi;

		if (pCi.matches(RULE_OWL_PKG_SINGLE_ONTOLOGY_PER_SCHEMA)) {

			relevantPi = schemaByTargetNamespace.get(pCi.targetNamespace());

			if (relevantPi == null) {
				// can happen if the class is in a package from an unselected
				// schema
				return null;
			}

		} else {
			// follow default ISO 19150-2 behavior, which is to have an ontology
			// for each package
			relevantPi = pCi;
		}

		OntologyDocument od = ontologyByPiMap.get(relevantPi);

		if (od == null) {
			// can happen if the class is in a package from an unselected
			// schema
			return null;

		} else {

			return od;
		}
	}

	public void write() {
		// ignore - this is a SingleTarget
	}

	/**
	 * This is the message text provision proper. It returns a message for a
	 * number.
	 * 
	 * @param mnr
	 *            Message number
	 * @return Message text or null
	 */
	protected String messageText(int mnr) {

		switch (mnr) {
		case 1:
			return "Could not find an ontology document for package '$1$', which was determined to be the relevant one for class '$2$'.";
		case 2:
			return "Rule '"
					+ RULE_OWL_PKG_SINGLE_ONTOLOGY_PER_SCHEMA
					+ "' is in effect, but no schema package was found for class '$1$'.";
		case 3:
			return "Unsupported class category ($1$).";
		case 4:
			return "Output directory is not accessible.";
		case 5:
			return "Ontology document with name '$1$' could not be created.";
		case 6:
			return "Target configuration type is incorrect. Expected a TargetOwl(Configuration).";
		case 10000:
			return "--- Context - class: '$1$'";
		}
		return null;
	}

	/**
	 * This method returns messages belonging to the target by their message
	 * number. The organization corresponds to the logic in module
	 * ShapeChangeResult. All functions in that class, which require a message
	 * number can be redirected to the function at hand.
	 * 
	 * @param mnr
	 *            Message number
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

	public void writeAll(ShapeChangeResult r) {

		if (error || printed)
			return;

		// output ontologies in folder hierarchy as determined by their
		// path - using normalized package names

		for (OntologyDocument od : ontologyByPiMap.values()) {

			od.finalizeDocument();

			od.print(outputDirectory, r);
		}

		printed = true;

	}

	public void reset() {

		OWLISO19150.counterByXmlprefix = new HashMap<String, Integer>();
		OWLISO19150.error = false;
		OWLISO19150.ontologyByCodeNs = new HashMap<String, OntologyDocument>();
		OWLISO19150.ontologyByPiMap = new HashMap<PackageInfo, OntologyDocument>();
		OWLISO19150.ontologyByRdfNs = new HashMap<String, OntologyDocument>();
		OWLISO19150.ontologyNameTaggedValue = null;
		OWLISO19150.outputDirectory = null;
		OWLISO19150.printed = false;
		OWLISO19150.processedAssociations = new HashSet<AssociationInfo>();
		OWLISO19150.rdfNsByPrefix = new HashMap<String, String>();
		OWLISO19150.result = null;
		OWLISO19150.schemaByTargetNamespace = new HashMap<String, PackageInfo>();
		OWLISO19150.source = null;
		OWLISO19150.sourceTaggedValue = null;
		OWLISO19150.uriBase = null;
		OWLISO19150.versionInfo = null;
		OWLISO19150.globalPropertyNames = new HashSet<String>();
	}

	/**
	 * @return the versionInfo
	 */
	public String getVersionInfo() {
		return versionInfo;
	}

	/**
	 * Computes the value for the dc:source that qualifies an ontology element.
	 * The value is computed according to the following instructions, in
	 * descending order:
	 * <ul>
	 * <li>if the configuration parameter
	 * {@value #PARAM_SOURCE_TAGGED_VALUE_NAME} is set and the info object has
	 * this tagged value, its value is used</li>
	 * <li>if the configuration parameter {@value #PARAM_SOURCE} is set then its
	 * value is used</li>
	 * <li>otherwise "FIXME" is returned</li>
	 * </ul>
	 * 
	 * @param i
	 * @return
	 */
	public String computeSource(Info i) {

		if (sourceTaggedValue != null) {

			String sourceTV = i.taggedValue(sourceTaggedValue);

			if (sourceTV != null) {
				return sourceTV;
			}

		} else if (source != null) {
			return source;
		}

		return "FIXME";
	}

	/**
	 * @param rdfns
	 * @return the abbreviation/prefix belonging to the given rdf namespace, or
	 *         <code>null</code> if no such prefix was found.
	 */
	public String computePrefixForRdfNamespace(String rdfns) {

		// try to identify via namespace configuration info
		String nsabr = config.nsabrForNamespace(rdfns);

		if (nsabr == null) {

			// try to find namespace via local ontologies
			if (ontologyByRdfNs.containsKey(rdfns)) {

				nsabr = ontologyByRdfNs.get(rdfns).getPrefix();
			}

			if (ontologyByCodeNs.containsKey(rdfns)) {

				nsabr = ontologyByCodeNs.get(rdfns).getPrefixForCode();
			}
		}

		return nsabr;
	}

	public Set<AssociationInfo> getProcessedAssociations() {
		return processedAssociations;
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

	/**
	 * @return the language
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * @return the ontologyNameTaggedValue
	 */
	public String getOntologyNameTaggedValue() {
		return ontologyNameTaggedValue;
	}

	/**
	 * @return the globalPropertyNames
	 */
	public static Set<String> getGlobalPropertyNames() {
		return globalPropertyNames;
	}
}
