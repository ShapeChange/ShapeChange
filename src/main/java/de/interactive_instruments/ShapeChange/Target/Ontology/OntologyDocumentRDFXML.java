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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.hp.hpl.jena.rdf.model.Resource;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Multiplicity;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.StructuredNumber;
import de.interactive_instruments.ShapeChange.TargetOwlConfiguration;
import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.Model.AssociationInfo;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;

/**
 * @author Johannes Echterhoff
 * 
 */
public class OntologyDocumentRDFXML implements OntologyDocument, MessageSource {

	protected Document document;
	protected Element root;
	protected Element ontology;
	protected Comment hook;

	protected Options options = null;
	public ShapeChangeResult result = null;

	protected Model model = null;
	protected PackageInfo mpackage = null;

	/**
	 * Contains locations for relevant ontologies that need to be imported
	 */
	protected Set<String> imports = new HashSet<String>();

	/**
	 * Contains information on namespaces that should be declared in the
	 * ontology root element
	 * <p>
	 * key: nsabr; value: rdf namespace
	 */
	protected Map<String, String> declaredRdfNamespacesByNsabr = new HashMap<String, String>();
	/**
	 * Contains information on namespaces that are used internally
	 * <p>
	 * key: nsabr; value: rdf namespace
	 */
	protected Map<String, String> usedRdfNamespacesByNsabr = new HashMap<String, String>();

	/**
	 * key: PropertyInfo.name(); value: 19150-2owl:propertyName (rdfNamespace +
	 * propertyLocalName)
	 */
	protected Map<String, String> globalPropertyRangeValueByPropertyName = new HashMap<String, String>();

	protected boolean printed = false;

	protected String targetNamespace = null;

	protected String name;
	protected String fileName;
	protected String rdfNamespace;
	protected String prefix;
	protected OWLISO19150 owliso19150;
	protected boolean finalized = false;
	protected String path;
	protected String backPath;
	protected List<ClassInfo> classInfos = new ArrayList<ClassInfo>();
	protected TargetOwlConfiguration config;
	protected Set<String> globalPropertyNames;

	public OntologyDocumentRDFXML(PackageInfo pi, Model m, Options o,
			ShapeChangeResult r, String xmlprefix, OWLISO19150 owliso19150)
			throws ShapeChangeAbortException {

		this.options = o;
		this.result = r;
		this.model = m;
		this.mpackage = pi;
		this.prefix = xmlprefix;
		this.owliso19150 = owliso19150;
		this.config = this.owliso19150.getConfig();
		this.globalPropertyNames = OWLISO19150.getGlobalPropertyNames();

		this.name = computeName();
		this.fileName = normalizedName(pi) + ".rdf";

		// as per ISO 19150-2owl:rdfNamespace
		// TBD Comment by Great Britain: "/" should also be allowed
		this.rdfNamespace = name + "#";

		this.path = computePath(pi);
		this.backPath = computeBackPath(pi);

		try {

			this.createDocument();

		} catch (ParserConfigurationException e) {

			result.addError(this, 1, pi.name());
			e.printStackTrace(System.err);
			throw new ShapeChangeAbortException();
		}
	}

	/**
	 * Determines the ontology name of a given package. The name is constructed
	 * following the ISO 19150-2owl:ontologyName requirement.
	 * 
	 * The ontologyName is defined via the following rules, in descending
	 * priority:
	 * <ul>
	 * <li>If the configuration parameter
	 * {@link OWLISO19150#PARAM_ONTOLOGYNAME_TAGGED_VALUE_NAME} is set and an
	 * according tagged value is set for the package its value is used.</li>
	 * <li>If the configuration parameter {@link OWLISO19150#PARAM_URIBASE} is
	 * set its value is used for constructing the ontologyName</li>
	 * <li>Otherwise the targetNamespace of the package is used as URIbase</li>
	 * </ul>
	 * If URIbase is used and the encoding rule
	 * {@link OWLISO19150#RULE_OWL_PKG_PATH_IN_ONTOLOGY_NAME} is in effect, then
	 * the umlPackageName is constructed using the path of the package to the
	 * upmost owner that is in the same targetNamespace - using a combination of
	 * "/" and normalized package names for all parent packages in the same
	 * target namespace; otherwise just the normalized package name is appended
	 * to URIbase as per 19150-2owl:ontologyName.
	 * 
	 * @param pi
	 * @return
	 */
	public String computeName() {

		if (owliso19150.getOntologyNameTaggedValue() != null) {
			String ontologyName = mpackage.taggedValue(owliso19150
					.getOntologyNameTaggedValue());
			if (ontologyName != null) {
				return ontologyName;
			}
		}

		String uriBase;

		if (owliso19150.getUriBase() != null) {
			uriBase = owliso19150.getUriBase();
		} else {
			uriBase = mpackage.targetNamespace();
		}

		String path;

		if (mpackage.matches(OWLISO19150.RULE_OWL_PKG_ONTOLOGY_NAME_WITH_PATH)) {
			path = computePath(mpackage);
		} else {
			path = "/" + normalizedName(mpackage);
		}

		return uriBase + path;
	}

	public static String normalizedName(ClassInfo ci) {

		// ISO 19150-2owl:className (part 2)
		// =================================

		String result = ci.name();

		// no space characters
		result = result.replaceAll(" ", "");

		/*
		 * dash and underscore characters are kept
		 * 
		 * other punctuation characters are replaced by underscore characters
		 * 
		 * According to
		 * http://en.wikipedia.org/wiki/Regular_expression#POSIX_basic_and_extended
		 * the POSIX [:punct:] character class has the following ASCII
		 * punctuation characters: [][!"#$%&'()*+,./:;<=>?@\^_`{|}~-]
		 * 
		 * In Java this character class can be used in regular expressions via
		 * \p{Punct}. We can omit specific characters in a regular expression
		 * that uses this character class (to keep dash and underscore).
		 */
		result = result.replaceAll("[\\p{Punct}&&[^-_]]", "_");

		// upper camel case
		char[] characters = result.toCharArray();
		String firstChar = String.valueOf(characters[0]);
		firstChar = firstChar.toUpperCase();
		characters[0] = firstChar.charAt(0);
		result = String.valueOf(characters);

		return result;
	}

	/**
	 * Provides the path (of packages) that leads from the main schema package
	 * down to this package. The main schema package is the topmost parent/owner
	 * package that has the same target namespace as the given one. Each segment
	 * of the path is preceded by "/", followed by the normalized package name.
	 * 
	 * @param pi
	 * @return
	 * @see #normalizedName(PackageInfo)
	 */
	public static String computePath(PackageInfo pi) {

		String result = "/" + normalizedName(pi);

		if (pi.owner() != null) {

			String ownerTargetNamespace = pi.owner().targetNamespace();

			if (ownerTargetNamespace != null) {

				if (ownerTargetNamespace.equals(pi.targetNamespace())) {
					String parentPath = computePath(pi.owner());
					result = parentPath + result;
				}
			} else {
				// this could be the case for model packages that only provide
				// structure
			}
		}

		return result;
	}

	public static String computeBackPath(PackageInfo pi) {

		String result = "../";

		if (pi.owner() != null) {

			String ownerTargetNamespace = pi.owner().targetNamespace();

			if (ownerTargetNamespace != null) {

				if (ownerTargetNamespace.equals(pi.targetNamespace())) {
					result = result + computeBackPath(pi.owner());
				}
			} else {
				// this could be the case for model packages that only provide
				// structure
			}
		}

		return result;
	}

	/**
	 * Normalizes the name of a package according to the rules in ISO
	 * 19150-2owl:ontologyName.
	 * 
	 * @param pi
	 * @return
	 */
	public static String normalizedName(PackageInfo pi) {

		String result = pi.name();

		// no space characters
		result = result.replaceAll(" ", "");

		/*
		 * dash and underscore characters are kept
		 * 
		 * other punctuation characters are replaced by underscore characters
		 * 
		 * According to
		 * http://en.wikipedia.org/wiki/Regular_expression#POSIX_basic_and_extended
		 * the POSIX [:punct:] character class has the following ASCII
		 * punctuation characters: [][!"#$%&'()*+,./:;<=>?@\^_`{|}~-]
		 * 
		 * In Java this character class can be used in regular expressions via
		 * \p{Punct}. We can omit specific characters in a regular expression
		 * that uses this character class (to keep dash and underscore).
		 */
		result = result.replaceAll("[\\p{Punct}&&[^-_]]", "_");

		// upper camel case
		// TBD lower camel case requested by Australia
		char[] characters = result.toCharArray();
		String firstChar = String.valueOf(characters[0]);
		firstChar = firstChar.toUpperCase();
		characters[0] = firstChar.charAt(0);
		result = String.valueOf(characters);

		/*
		 * only the semantic part of the package name is represented -> this
		 * should be the case as we do not get the fully qualified package name
		 * via PackageInfo#name()
		 */

		return result;
	}

	public void createDocument() throws ParserConfigurationException {

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		dbf.setValidating(true);
		dbf.setAttribute(Options.JAXP_SCHEMA_LANGUAGE, Options.W3C_XML_SCHEMA);
		DocumentBuilder db = dbf.newDocumentBuilder();
		document = db.newDocument();

		root = document.createElementNS(OWLISO19150.RDF_NS_W3C_RDF, "rdf:RDF");
		document.appendChild(root);

		this.declaredRdfNamespacesByNsabr
				.put("rdf", OWLISO19150.RDF_NS_W3C_RDF);
		this.declaredRdfNamespacesByNsabr.put("rdfs",
				OWLISO19150.RDF_NS_W3C_RDFS);
		this.declaredRdfNamespacesByNsabr
				.put("owl", OWLISO19150.RDF_NS_W3C_OWL);
		this.declaredRdfNamespacesByNsabr.put("skos",
				OWLISO19150.RDF_NS_W3C_SKOS);
		this.declaredRdfNamespacesByNsabr.put("dc", OWLISO19150.RDF_NS_DC);
		this.declaredRdfNamespacesByNsabr.put(OWLISO19150.PREFIX_ISO_19150_2,
				OWLISO19150.RDF_NS_ISO_19150_2);
		this.declaredRdfNamespacesByNsabr.put(prefix, rdfNamespace);

		this.usedRdfNamespacesByNsabr.put("rdf", OWLISO19150.RDF_NS_W3C_RDF);
		this.usedRdfNamespacesByNsabr.put("rdfs", OWLISO19150.RDF_NS_W3C_RDFS);
		this.usedRdfNamespacesByNsabr.put("owl", OWLISO19150.RDF_NS_W3C_OWL);
		this.usedRdfNamespacesByNsabr.put("skos", OWLISO19150.RDF_NS_W3C_SKOS);
		this.usedRdfNamespacesByNsabr.put("dc", OWLISO19150.RDF_NS_DC);
		this.usedRdfNamespacesByNsabr.put("xsd",
				OWLISO19150.RDF_NS_W3C_XML_SCHEMA);
		this.usedRdfNamespacesByNsabr.put(OWLISO19150.PREFIX_ISO_19150_2,
				OWLISO19150.RDF_NS_ISO_19150_2);
		this.usedRdfNamespacesByNsabr.put(prefix, rdfNamespace);

		ontology = document.createElementNS(OWLISO19150.RDF_NS_W3C_OWL,
				"owl:Ontology");
		root.appendChild(ontology);

		// use ontology URI for rdf:about
		addAttribute(ontology, "rdf:about", name);

		/*
		 * ISO 19150-2 says "full name of the corresponding UML PACKAGE" -
		 * we interpret this as the full local name
		 */
		addRdfsLabel(ontology, mpackage.name());

		addDcSource(ontology, this.owliso19150.computeSource(mpackage));

		String documentation = (mpackage.definition() + "\n" + mpackage.description()).trim();
		if (documentation != null && documentation.length() > 0) {
			addRdfsComment(ontology, documentation);
		}

		String version = mpackage.version();
		addOwlVersionInfo(ontology, version);

		// add standard import(s)
		addImport(OWLISO19150.PREFIX_ISO_19150_2,
				OWLISO19150.RDF_NS_ISO_19150_2, config.locationOfNamespace(OWLISO19150.RDF_NS_ISO_19150_2));
	}

	private void addOwlVersionInfo(Element e, String value) {

		Element vi = document.createElementNS(OWLISO19150.RDF_NS_W3C_OWL,
				"owl:versionInfo");
		e.appendChild(vi);

		vi.setTextContent(value);
	}

	private void addDcSource(Element e, Info i) {

		String value = owliso19150.computeSource(i);

		addDcSource(e, value);
	}

	private void addDcSource(Element e, String value) {

		Element s = document
				.createElementNS(OWLISO19150.RDF_NS_DC, "dc:source");
		e.appendChild(s);

		s.setTextContent(value);
	}

	private void addRdfsLabel(Element e, String value) {

		Element label = document.createElementNS(OWLISO19150.RDF_NS_W3C_RDFS,
				"rdfs:label");
		e.appendChild(label);

		label.setTextContent(value);
	}

	private void addRdfsComment(Element e, String value) {

		Element comment = document.createElementNS(OWLISO19150.RDF_NS_W3C_RDFS,
				"rdfs:comment");
		e.appendChild(comment);

		comment.setTextContent(value);
	}

	/**
	 * Logs an import statement for the given namespace, if it is not the same
	 * as the one of this ontology document (or its code namespace), and if the
	 * location is not null.
	 * 
	 * Also adds the namespace declaration (including retrieval of the
	 * abbreviation/prefix).
	 * <p>
	 * Note: the information will be added to the document when
	 * {@link #finalizeDocument()} is called.
	 * 
	 * @param rdfns
	 * @param loc
	 */
	protected void addImport(String rdfns, String loc) {

		if (rdfns == null || rdfns.equals(this.rdfNamespace)
				|| rdfns.equals(getCodeNamespace())) {
			return;
		}

		if (loc != null) {

			this.imports.add(loc);
		}

		// determine prefix for rdf namespace
		String prefix = owliso19150.computePrefixForRdfNamespace(rdfns);

		if (prefix != null) {
			this.usedRdfNamespacesByNsabr.put(prefix, rdfns);
		} else {
			result.addWarning(this, 4, rdfns);
		}
	}

	/**
	 * Logs an import statement for the given namespace, if it is not the same
	 * as the one of this ontology document (or its code namespace), and if the
	 * location is not null.
	 * <p>
	 * Also adds the namespace declaration.
	 * <p>
	 * Note: the information will be added to the document when
	 * {@link #finalizeDocument()} is called.
	 * 
	 * @param rdfns
	 * @param loc
	 */
	protected void addImport(String prefix, String rdfns, String loc) {

		if (rdfns == null || rdfns.equals(this.rdfNamespace)
				|| rdfns.equals(getCodeNamespace())) {
			return;
		}

		if (loc != null) {

			this.imports.add(loc);
		}

		if (prefix != null) {

			this.usedRdfNamespacesByNsabr.put(prefix, rdfns);
		}
	}

	/** Add attribute to an element */
	protected void addAttribute(Element e, String name, String value) {
		try {
			Attr att = document.createAttribute(name);
			att.setValue(value);
			e.setAttributeNode(att);
		} catch (DOMException exc) {
			System.out.println(name);
			exc.printStackTrace();
		}
	}

	/**
	 * <p>
	 * This method returns messages belonging to the XML Schema target by their
	 * message number. The organization corresponds to the logic in module
	 * ShapeChangeResult. All functions in that class, which require an message
	 * number can be redirected to the function at hand.
	 * </p>
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
		return prefix + "Ontology Target: " + mess;
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
			return "ParserConfigurationException when creating document for package '$1$'.";
		case 2:
			return "Could not find a type mapping and also no ontology document to map class '$1$'. Using owl:Class.";
		case 3:
			return "Ontology document has already been finalized. Cannot add class '$1$'";
		case 4:
			return "Could not add namespace declaration info for (rdf) namespace '$1$' because no abbreviation/prefix was found for it.";
		case 5:
			return "Unsupported class category ($1$).";
		case 6:
			return "Could not identify a mapping for the supertype '$1$' of class '$2$'.";
		case 7:
			return "Could not find a type mapping and also no class within the model to map class '$1$'. Using owl:Class.";
		case 8:
			return "No stereotype mapping defined for class '$1$' (a $2$).";
		case 9:
			return "Unsupported class category encountered while defining the stereotype of class '$1$'.";
		case 10:
			return "Duplicate property mapping encountered in union (for type named '$1$').";
		case 10000:
			return "--- Context - class: '$1$'";
		case 10001:
			return "--- Context - class: '$1$', property: '$2$'";
		case 20000:
			return "Writing ontology to file at $1$";

		}
		return null;
	}

	public void finalizeDocument() {

		for (ClassInfo ci : this.classInfos) {

			int cat = ci.category();

			switch (cat) {
			case Options.FEATURE:
				addFeature(ci);
				break;
			case Options.OBJECT:
				addInterface(ci);
				break;
			case Options.DATATYPE:
				addDatatype(ci);
				break;
			case Options.UNION:
				addUnion(ci);
				break;
			case Options.CODELIST:
				addCodelist(ci);
				break;
			case Options.ENUMERATION:
				addEnumeration(ci);
				break;
			default:
				MessageContext mc = result.addError(this, 5, "" + cat);
				mc.addDetail(this, 30000, ci.fullName());
			}
		}

		// add xmlns declarations
		for (String nsabr : declaredRdfNamespacesByNsabr.keySet()) {

			addAttribute(root, "xmlns:" + nsabr,
					declaredRdfNamespacesByNsabr.get(nsabr));
		}

		// add imports
		for (String location : imports) {

			Element e = document.createElementNS(OWLISO19150.RDF_NS_W3C_OWL,
					"owl:imports");

			ontology.appendChild(e);

			addAttribute(e, "rdf:resource", location);
		}

		// ensure that no further classes are added once finalized
		finalized = true;
	}

	/**
	 * @return the ontology name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the rdfNamespace
	 */
	public String getRdfNamespace() {
		return rdfNamespace;
	}

	/**
	 * @return the rdfNamespace
	 */
	public String getCodeNamespace() {

		return getName() + "/code/";
	}

	public void addFeature(ClassInfo ci) {

		addClassDefinition(ci);
	}

	public void addClassDefinition(ClassInfo ci) {

		if (finalized) {
			this.result.addWarning(this, 3, ci.name());
			return;
		}

		// create the Class <OWL>
		Element c = document.createElementNS(OWLISO19150.RDF_NS_W3C_OWL,
				"owl:Class");
		root.appendChild(c);

		addAttribute(c, "rdf:about",
				computeReference(getPrefix(), normalizedName(ci)));

		addSkosPrefLabel(c, ci);
		addSkosDefinition(c, ci.definition());
		addSkosScopeNote(c, ci.description());
		addDcSource(c, ci);

		if (ci.matches(OWLISO19150.RULE_OWL_ALL_CONSTRAINTS)) {
			addConstraintDeclarations(c, ci);
		}

		if (ci.isAbstract()) {
			addIso19150_2IsAbstract(c, true);
		}

		// assign stereotype information
		addStereotypeInfo(c, ci);

		// determine if this is a subclass of one or more specific types
		if (ci.baseClass() != null
				|| (ci.supertypes() != null && !ci.supertypes().isEmpty())) {

			String baseClassId = null;

			if (ci.baseClass() != null) {

				ClassInfo supertype = ci.baseClass();

				baseClassId = supertype.id();

				String mapping = mapElement(supertype);

				if (mapping == null) {

					MessageContext mc = result.addError(this, 6,
							supertype.name(), ci.name());
					mc.addDetail(this, 10000, ci.fullName());

				} else {

					addSubclassOf(c, mapping);
				}
			}

			if (ci.supertypes() != null && !ci.supertypes().isEmpty()) {

				// for now create one subClassOf for each supertype
				for (String ciId : ci.supertypes()) {

					if (baseClassId != null && baseClassId.equals(ciId)) {
						continue;
					}

					ClassInfo supertype = model.classById(ciId);

					String mapping = mapElement(supertype);

					if (mapping == null) {

						MessageContext mc = result.addError(this, 6,
								supertype.name(), ci.name());
						mc.addDetail(this, 10000, ci.fullName());

					} else {

						addSubclassOf(c, mapping);
					}
				}
			}
		}

		// now add the properties
		SortedMap<StructuredNumber, PropertyInfo> pis = ci.properties();

		if (pis != null) {

			for (PropertyInfo pi : pis.values()) {

				if (pi.isNavigable()) {

					addPropertyDeclaration(pi);

					// establish multiplicity
					addMultiplicity(c, pi);
				}
			}
		}
	}

	private void addStereotypeInfo(Element c, ClassInfo ci) {

		Map<String, String> stereotypeMappings = owliso19150.getConfig()
				.getStereotypeMappings();
		if (stereotypeMappings == null) {
			// initialize map so that following tests are simpler
			stereotypeMappings = new HashMap<String, String>();
		}

		int cat = ci.category();

		switch (cat) {
		case Options.FEATURE:

			String geosparqlFeatureType = null;

			if (ci.matches(OWLISO19150.RULE_OWL_CLS_GEOSPARQL_FEATURES)) {

				addImport(OWLISO19150.RDF_NS_OGC_GEOSPARQL);

				geosparqlFeatureType = OWLISO19150.RDF_NS_OGC_GEOSPARQL
						+ "Feature";
				addSubclassOf(c, geosparqlFeatureType);
			}

			String iso19150FeatureType = null;
			if (ci.matches(OWLISO19150.RULE_OWL_CLS_19150_2_FEATURES)) {

				/*
				 * import for 19150-2 base is automatically added in
				 * createDocument()
				 */

				iso19150FeatureType = computeReference(
						OWLISO19150.PREFIX_ISO_19150_2, "FeatureType");
				addSubclassOf(c, iso19150FeatureType);

				/*
				 * TBD: AnyFeature missing in 19150-2 base ontology (found in
				 * Annex D)
				 */
				addSubclassOf(
						c,
						computeReference(OWLISO19150.PREFIX_ISO_19150_2,
								"AnyFeature"));
			}

			if (stereotypeMappings.containsKey("featuretype")) {
				String mappingForFeatureType = stereotypeMappings
						.get("featuretype");
				if ((iso19150FeatureType == null && geosparqlFeatureType == null)
						|| !iso19150FeatureType.equals(mappingForFeatureType)) {
					addSubclassOf(c, mappingForFeatureType);
				}
			} else if (iso19150FeatureType == null
					&& geosparqlFeatureType == null) {
				result.addWarning(this, 8, ci.name(), "featuretype");
			}

			break;

		case Options.OBJECT:

			if (stereotypeMappings.containsKey("type")) {
				addSubclassOf(c, stereotypeMappings.get("type"));
			} else {
				result.addWarning(this, 8, ci.name(), "type");
			}

			break;
		case Options.DATATYPE:

			if (stereotypeMappings.containsKey("datatype")) {
				addSubclassOf(c, stereotypeMappings.get("datatype"));
			} else {
				result.addWarning(this, 8, ci.name(), "datatype");
			}

			break;
		case Options.CODELIST:

			if (stereotypeMappings.containsKey("codelist")) {
				addSubclassOf(c, stereotypeMappings.get("codelist"));
			} else {
				result.addWarning(this, 8, ci.name(), "codelist");
			}

			break;
		case Options.UNION:

			if (stereotypeMappings.containsKey("union")) {
				addSubclassOf(c, stereotypeMappings.get("union"));
			} else {
				result.addWarning(this, 8, ci.name(), "union");
			}

			break;
		case Options.ENUMERATION:

			if (stereotypeMappings.containsKey("enumeration")) {
				addSubclassOf(c, stereotypeMappings.get("enumeration"));
			} else {
				result.addWarning(this, 8, ci.name(), "enumeration");
			}

			break;
		default:
			result.addWarning(this, 9, ci.name());
		}
	}

	protected void addImport(String rdfns) {

		if (rdfns == null || rdfns.equals(this.rdfNamespace)
				|| rdfns.equals(getCodeNamespace())) {
			return;
		}

		String loc = config.locationOfNamespace(rdfns);

		if (loc != null) {

			this.imports.add(loc);
		}

		// determine prefix for rdf namespace
		String prefix = owliso19150.computePrefixForRdfNamespace(rdfns);

		if (prefix != null) {
			this.usedRdfNamespacesByNsabr.put(prefix, rdfns);
		} else {
			result.addWarning(this, 4, rdfns);
		}
	}

	protected void addMultiplicity(Element classE, PropertyInfo pi) {

		String piName;

		if (globalPropertyNames.contains(pi.name())) {

			// the global property should have already been declared (before
			// multiplicity is added to it)

			piName = globalPropertyRangeValueByPropertyName.get(pi.name());
		} else {
			piName = computeReference(getPrefix(), normalizedName(pi.inClass())
					+ "." + normalizedName(pi));
		}

		Multiplicity m = pi.cardinality();

		// set min cardinality if required
		if (m.minOccurs == 0 || pi.matches(OWLISO19150.RULE_OWL_PROP_SUPPRESS_CARDINALITY_RESTRICTIONS) || (pi.voidable() && pi.matches(OWLISO19150.RULE_OWL_PROP_VOIDABLE_AS_MINCARDINALITY0))) {

			// simply omit min cardinality to represent this case

		} else {
			Element minOccE = document.createElementNS(OWLISO19150.RDF_NS_W3C_RDFS,
					"rdfs:subClassOf");
			classE.appendChild(minOccE);
	
			Element rMinOcc = document.createElementNS(OWLISO19150.RDF_NS_W3C_OWL,
					"owl:Restriction");
			minOccE.appendChild(rMinOcc);
	
			Element onPropMinOcc = document.createElementNS(
					OWLISO19150.RDF_NS_W3C_OWL, "owl:onProperty");
			rMinOcc.appendChild(onPropMinOcc);
	
			addAttribute(onPropMinOcc, "rdf:resource", piName);
	
			Element minCard = document.createElementNS(OWLISO19150.RDF_NS_W3C_OWL,
					"owl:minCardinality");
			rMinOcc.appendChild(minCard);
	
			addAttribute(minCard, "rdf:datatype",
					computeReference("xsd", "nonNegativeInteger"));
	
			minCard.setTextContent("" + m.minOccurs);
		}
		
		// set max cardinality if required
		if (m.maxOccurs == Integer.MAX_VALUE || pi.matches(OWLISO19150.RULE_OWL_PROP_SUPPRESS_CARDINALITY_RESTRICTIONS)) {

			// simply omit max cardinality to represent this case

		} else {

			Element maxOccE = document.createElementNS(
					OWLISO19150.RDF_NS_W3C_RDFS, "rdfs:subClassOf");
			classE.appendChild(maxOccE);

			Element rMaxOcc = document.createElementNS(
					OWLISO19150.RDF_NS_W3C_OWL, "owl:Restriction");
			maxOccE.appendChild(rMaxOcc);

			Element onPropMaxOcc = document.createElementNS(
					OWLISO19150.RDF_NS_W3C_OWL, "owl:onProperty");
			rMaxOcc.appendChild(onPropMaxOcc);

			addAttribute(onPropMaxOcc, "rdf:resource", piName);

			Element maxCard = document.createElementNS(
					OWLISO19150.RDF_NS_W3C_OWL, "owl:maxCardinality");
			rMaxOcc.appendChild(maxCard);

			addAttribute(maxCard, "rdf:datatype",
					computeReference("xsd", "nonNegativeInteger"));

			maxCard.setTextContent("" + m.maxOccurs);
		}

		// set all values information
		Element avE = document.createElementNS(OWLISO19150.RDF_NS_W3C_RDFS,
				"rdfs:subClassOf");
		classE.appendChild(avE);

		Element rAvOcc = document.createElementNS(OWLISO19150.RDF_NS_W3C_OWL,
				"owl:Restriction");
		avE.appendChild(rAvOcc);

		Element onPropAv = document.createElementNS(OWLISO19150.RDF_NS_W3C_OWL,
				"owl:onProperty");
		rAvOcc.appendChild(onPropAv);

		addAttribute(onPropAv, "rdf:resource", piName);

		Element av = document.createElementNS(OWLISO19150.RDF_NS_W3C_OWL,
				"owl:allValuesFrom");
		rAvOcc.appendChild(av);

		addAttribute(av, "rdf:resource", mapElement(pi.typeInfo()));

	}

	private void addPropertyDeclaration(PropertyInfo pi) {

		if (!pi.isNavigable()) {
			return;
		}

		boolean isGlobalProperty = false;
		if (globalPropertyNames.contains(pi.name())) {

			isGlobalProperty = true;

			if (globalPropertyRangeValueByPropertyName.containsKey(pi.name())) {
				// the global property has already been added - do not add it
				// again
				return;
			}
		}

		// Determine if this is a DatatypeProperty or ObjectProperty

		ProcessMapEntry pme = config.getMapEntry(pi.typeInfo().name);

		Element pE;
		String rangeValue;

		if (pme != null && pme.hasParam()
				&& pme.getParam().equalsIgnoreCase("simpleType")) {

			// we have a mapping to a simple type
			pE = document.createElementNS(OWLISO19150.RDF_NS_W3C_OWL,
					"owl:DatatypeProperty");
			String qname = pme.getTargetType();
			String[] parts = qname.split(":");
			rangeValue = computeReference(parts[0], parts[1]);

		} else {

			// we have an object type
			pE = document.createElementNS(OWLISO19150.RDF_NS_W3C_OWL,
					"owl:ObjectProperty");

			rangeValue = mapElement(pi.typeInfo());
		}

		root.appendChild(pE);

		String propAbout;
		if (isGlobalProperty) {
			propAbout = computeReference(getPrefix(), normalizedName(pi));
			this.globalPropertyRangeValueByPropertyName.put(pi.name(),
					propAbout);
		} else {
			propAbout = computeReference(getPrefix(),
					normalizedName(pi.inClass()) + "." + normalizedName(pi));
		}

		addAttribute(pE, "rdf:about", propAbout);

		addSkosPrefLabel(pE, pi);

		addSkosDefinition(pE, pi.definition());
		addSkosScopeNote(pE, pi.description());

		if (pi.matches(OWLISO19150.RULE_OWL_ALL_CONSTRAINTS)) {
			addConstraintDeclarations(pE, pi);
		}

		addDcSource(pE, pi);

		// TODO definitely TBD if the domain should be declared for a global
		// property
		if (!isGlobalProperty) {
			addDomain(pE,
					computeReference(getPrefix(), normalizedName(pi.inClass())));
		}

		addRdfsRange(pE, rangeValue);

		AssociationInfo ai = pi.association();

		if (ai != null) {

			// ensure that owl:inverseOf is set only once
			if (!owliso19150.getProcessedAssociations().contains(ai)) {

				// check if we need to set owl:inverseOf

				PropertyInfo revPi = pi.reverseProperty();

				if (revPi != null && revPi.isNavigable()) {

					String ioValue = mapElement(revPi.inClass()) + "."
							+ normalizedName(revPi);
					addOwlInverseOf(pE, ioValue);

					owliso19150.getProcessedAssociations().add(ai);
				}
			}

			// add association name if it exists
			String aiName = ai.name();
			if (aiName != null && aiName.length() > 0) {
				addAssociationName(pE, aiName);
			}

			if (pi.isComposition()) {
				addIso19150_2AggregationType(pE, "partOfCompositeAggregation");
			} else if (pi.isAggregation()) {
				addIso19150_2AggregationType(pE, "partOfSharedAggregation");
			} else {
				// no special aggregation to document
			}
		}
	}

	private void addConstraintDeclarations(Element e, PropertyInfo pi) {

		List<Constraint> cons = pi.constraints();

		if (cons != null && !cons.isEmpty()) {

			for (Constraint c : cons) {

				Element cE = document.createElementNS(
						OWLISO19150.RDF_NS_ISO_19150_2,
						OWLISO19150.PREFIX_ISO_19150_2 + ":constraint");
				e.appendChild(cE);

				cE.setTextContent(c.text());
			}
		}
	}

	protected String mapElement(Type ti) {

		if (ti == null)
			return null;

		ProcessMapEntry pme = config.getMapEntry(ti.name);

		String elementName;
		String prefix;
		String rdfNs;
		String location;

		if (pme == null) {

			ClassInfo ci = model.classById(ti.id);

			if (ci == null) {
				// in case that the model references the type by name only
				ci = model.classByName(ti.name);
			}

			if (ci == null) {
				this.result.addError(this, 7, ti.name);
				return computeReference("owl", "Class");
			}

			// lookup the ontology to which the class belongs
			OntologyDocument od = owliso19150.computeRelevantOntology(ci);

			if (od == null) {

				MessageContext mc = this.result.addError(this, 2, ci.name());
				if (mc != null) {
					mc.addDetail(this, 10000, ci.fullName());
				}
				return computeReference("owl", "Class");

			} else {

				prefix = od.getPrefix();
				rdfNs = od.getRdfNamespace();

				elementName = normalizedName(ci);

				/*
				 * construct relative location to the other ontology document,
				 * removing the trailing "/" from the back path of this ontology
				 * document, and adding the path of the other ontology (which
				 * begins with "/").
				 */
				location = computeRelativeLocation(this, od);
			}

		} else {

			// get QName
			String qname = pme.getTargetType();

			String[] qnamePars = qname.split(":");
			prefix = qnamePars[0];
			elementName = qnamePars[1];

			// identify rdf namespace based upon prefix and standard namespaces
			rdfNs = config.fullNamespace(prefix);
			location = config.locationOfNamespace(rdfNs);
		}

		// also add import for the namespace
		addImport(rdfNs, location);

		// return correct element definition
		return computeReference(prefix, elementName);
	}

	private void addIso19150_2AggregationType(Element e, String value) {

		Element at = document.createElementNS(OWLISO19150.RDF_NS_ISO_19150_2,
				OWLISO19150.PREFIX_ISO_19150_2 + ":aggregationType");
		e.appendChild(at);

		at.setTextContent(value);
	}

	private void addOwlInverseOf(Element e, String value) {

		Element io = document.createElementNS(OWLISO19150.RDF_NS_W3C_OWL,
				"owl:inverseOf");
		e.appendChild(io);

		addAttribute(io, "rdf:resource", value);
	}

	private void addAssociationName(Element e, String value) {

		Element an = document.createElementNS(OWLISO19150.RDF_NS_ISO_19150_2,
				OWLISO19150.PREFIX_ISO_19150_2 + ":associationName");
		e.appendChild(an);

		an.setTextContent(value);
	}

	private void addRdfsRange(Element e, String value) {

		Element r = document.createElementNS(OWLISO19150.RDF_NS_W3C_RDFS,
				"rdfs:range");
		e.appendChild(r);

		addAttribute(r, "rdf:resource", value);
	}

	private void addDomain(Element e, String value) {

		Element d = document.createElementNS(OWLISO19150.RDF_NS_W3C_RDFS,
				"rdfs:domain");
		e.appendChild(d);

		addAttribute(d, "rdf:resource", value);
	}

	public static String normalizedName(PropertyInfo pi) {

		// ISO 19150-2owl:propertyName (part 2)
		// =================================

		String result = pi.name();

		// no space characters
		result = result.replaceAll(" ", "");

		/*
		 * dash and underscore characters are kept
		 * 
		 * other punctuation characters are replaced by underscore characters
		 * 
		 * According to
		 * http://en.wikipedia.org/wiki/Regular_expression#POSIX_basic_and_extended
		 * the POSIX [:punct:] character class has the following ASCII
		 * punctuation characters: [][!"#$%&'()*+,./:;<=>?@\^_`{|}~-]
		 * 
		 * In Java this character class can be used in regular expressions via
		 * \p{Punct}. We can omit specific characters in a regular expression
		 * that uses this character class (to keep dash and underscore).
		 */
		result = result.replaceAll("[\\p{Punct}&&[^-_]]", "_");

		// lower camel case
		char[] characters = result.toCharArray();
		String firstChar = String.valueOf(characters[0]);
		firstChar = firstChar.toLowerCase();
		characters[0] = firstChar.charAt(0);
		result = String.valueOf(characters);

		return result;
	}

	private void addSubclassOf(Element e, String value) {

		Element sc = document.createElementNS(OWLISO19150.RDF_NS_W3C_RDFS,
				"rdfs:subClassOf");
		e.appendChild(sc);

		addAttribute(sc, "rdf:resource", value);
	}

	protected String mapElement(ClassInfo ci) {

		if (ci == null)
			return null;

		ProcessMapEntry pme = config.getMapEntry(ci.name());

		String elementName;
		String prefix;
		String rdfNs;
		String location;

		if (pme == null) {

			// lookup the ontology to which the class belongs
			OntologyDocument od = owliso19150.computeRelevantOntology(ci);

			if (od == null) {

				MessageContext mc = this.result.addError(this, 2, ci.name());
				if (mc != null) {
					mc.addDetail(this, 10000, ci.fullName());
				}
				return computeReference("owl", "Class");

			} else {

				if (ci.category() == Options.CODELIST) {

					prefix = od.getPrefixForCode();
					rdfNs = od.getCodeNamespace();

				} else {

					prefix = od.getPrefix();
					rdfNs = od.getRdfNamespace();
				}

				elementName = normalizedName(ci);

				/*
				 * construct relative location to the other ontology document,
				 * removing the trailing "/" from the back path of this ontology
				 * document, and adding the path of the other ontology (which
				 * begins with "/").
				 */
				location = computeRelativeLocation(this, od);
			}

		} else {

			// get QName
			String qname = pme.getTargetType();

			String[] qnamePars = qname.split(":");
			prefix = qnamePars[0];
			elementName = qnamePars[1];

			// identify rdf namespace based upon prefix and standard namespaces
			rdfNs = config.fullNamespace(prefix);
			location = config.locationOfNamespace(rdfNs);
		}

		// also add import for the namespace
		addImport(rdfNs, location);

		// return correct element definition
		return computeReference(prefix, elementName);
	}

	/**
	 * Computes the relative path/location from a given ontology document to
	 * another ontology document.
	 * 
	 * @param from
	 * @param to
	 * @return
	 */
	public static String computeRelativeLocation(OntologyDocument from,
			OntologyDocument to) {

		String fromBackPath = from.getBackPath();

		String result = fromBackPath.substring(0, fromBackPath.length() - 1)
				+ to.getPath() + "/" + to.getFileName();

		return result;
	}

	public String getFileName() {

		return fileName;
	}

	private void addIso19150_2IsAbstract(Element e, boolean value) {

		Element a = document.createElementNS(OWLISO19150.RDF_NS_ISO_19150_2,
				"iso19150-2:isAbstract");
		e.appendChild(a);

		a.setTextContent(value ? "true" : "false");

		addAttribute(a, "rdf:datatype", computeReference("xsd", "boolean"));
	}

	private void addIso19150_2IsEnumeration(Element e, boolean value) {

		Element enumerationE = document.createElementNS(
				OWLISO19150.RDF_NS_ISO_19150_2, "iso19150-2:isEnumeration");
		e.appendChild(enumerationE);

		enumerationE.setTextContent(value ? "true" : "false");

		addAttribute(enumerationE, "rdf:datatype",
				computeReference("xsd", "boolean"));
	}

	private void addSkosDefinition(Element e, String value) {

		// only add if we have a non-empty string 
		if (value == null || value.isEmpty())
			return;
		
		Element d = document.createElementNS(OWLISO19150.RDF_NS_W3C_SKOS,
				"skos:definition");
		e.appendChild(d);

		d.setTextContent(value);
	}

	private void addSkosScopeNote(Element e, String value) {

		// only add if we have a non-empty string 
		if (value == null || value.isEmpty())
			return;
		
		Element sn = document.createElementNS(OWLISO19150.RDF_NS_W3C_SKOS,
				"skos:scopeNote");
		e.appendChild(sn);

		sn.setTextContent(value);
	}

	private void addSkosPrefLabel(Element e, Info i) {

		Element pl = document.createElementNS(OWLISO19150.RDF_NS_W3C_SKOS,
				"skos:prefLabel");
		e.appendChild(pl);

		String alias = i.aliasName();

		if (alias != null && alias.length() > 0) {
			pl.setTextContent(alias);
		} else {
			pl.setTextContent(i.name());
		}
	}

	public void addInterface(ClassInfo ci) {

		addClassDefinition(ci);
	}

	public void addDatatype(ClassInfo ci) {

		addClassDefinition(ci);
	}

	public void addUnion(ClassInfo ci) {

		if (finalized) {
			this.result.addWarning(this, 3, ci.name());
			return;
		}

		// create the Class <OWL>
		Element c = document.createElementNS(OWLISO19150.RDF_NS_W3C_OWL,
				"owl:Class");
		root.appendChild(c);

		addAttribute(c, "rdf:about",
				computeReference(getPrefix(), normalizedName(ci)));

		addSubclassOf(c,
				computeReference(OWLISO19150.PREFIX_ISO_19150_2, "Union"));

		if (ci.matches(OWLISO19150.RULE_OWL_ALL_CONSTRAINTS)) {
			addConstraintDeclarations(c, ci);
		}

		SortedMap<StructuredNumber, PropertyInfo> uPis = ci.properties();

		if (uPis != null && !uPis.isEmpty()) {

			Element unionOf = document.createElementNS(
					OWLISO19150.RDF_NS_W3C_OWL, "owl:unionOf");
			c.appendChild(unionOf);
			addAttribute(unionOf, "rdf:parseType", "Collection");

			/*
			 * Remember which mappings have already been added, to ensure that
			 * there are no duplicates.
			 */
			Set<String> mappedTypesInUnion = new HashSet<String>();

			for (PropertyInfo pi : uPis.values()) {

				String mappingForType = mapElement(pi.typeInfo());

				if (mappedTypesInUnion.contains(mappingForType)) {

					MessageContext mc = result.addWarning(this, 10,
							pi.typeInfo().name);
					mc.addDetail(this, 10001, ci.name(), pi.name());

				} else {

					Element entry = document.createElementNS(
							OWLISO19150.RDF_NS_W3C_OWL, "owl:Class");
					unionOf.appendChild(entry);

					addAttribute(entry, "rdf:resource", mappingForType);
				}
			}
		}
	}

	private void addConstraintDeclarations(Element e, ClassInfo ci) {

		List<Constraint> cons = ci.constraints();

		if (cons != null && !cons.isEmpty()) {

			for (Constraint c : cons) {

				Element cE = document.createElementNS(
						OWLISO19150.RDF_NS_ISO_19150_2,
						OWLISO19150.PREFIX_ISO_19150_2 + ":constraint");
				e.appendChild(cE);

				cE.setTextContent(c.text());
			}
		}
	}

	public void addCodelist(ClassInfo ci) {

		if (finalized) {
			this.result.addWarning(this, 3, ci.name());
			return;
		}

		this.declaredRdfNamespacesByNsabr.put("dct", OWLISO19150.RDF_NS_DCT);
		this.usedRdfNamespacesByNsabr.put("dct", OWLISO19150.RDF_NS_DCT);

		String aboutValue_normal = computeReference(getPrefix(),
				normalizedName(ci));
		String aboutValue_code = getCodeNamespace() + normalizedName(ci);

		// create the Class <OWL>
		Element c = document.createElementNS(OWLISO19150.RDF_NS_W3C_OWL,
				"owl:Class");
		root.appendChild(c);

		addAttribute(c, "rdf:about", aboutValue_normal);

		addSkosPrefLabel(c, ci);
		addSkosDefinition(c, ci.definition());
		addSkosScopeNote(c, ci.description());

		addDcSource(c, ci);

		if (ci.matches(OWLISO19150.RULE_OWL_ALL_CONSTRAINTS)) {
			addConstraintDeclarations(c, ci);
		}

		// assign stereotype information
		addStereotypeInfo(c, ci);

		addSubclassOf(c, computeReference("skos", "Concept"));

		// create ConceptScheme <SKOS>
		Element cs = document.createElementNS(OWLISO19150.RDF_NS_W3C_SKOS,
				"skos:ConceptScheme");
		root.appendChild(cs);

		addAttribute(cs, "rdf:about", aboutValue_code);

		addSkosPrefLabel(cs, ci);
		addSkosDefinition(cs, ci.definition());
		addSkosScopeNote(cs, ci.description());
		addDcSource(cs, ci);

		addDctIsFormatOf(cs, aboutValue_normal);

		// now add the individual concept definitions
		SortedMap<StructuredNumber, PropertyInfo> clPis = ci.properties();

		if (clPis != null && !clPis.isEmpty()) {

			for (PropertyInfo pi : clPis.values()) {

				Element concept = document.createElementNS(getRdfNamespace(), getPrefix() + ":" + normalizedName(ci));
				root.appendChild(concept);

				String conceptSchemeRef = getCodeNamespace()
						+ normalizedName(ci);

				addAttribute(concept, "rdf:about",
						conceptSchemeRef + "/" + pi.name());

				Element rdfType = document.createElementNS(
						OWLISO19150.RDF_NS_W3C_RDF, "rdf:type");
				concept.appendChild(rdfType);
				addAttribute(rdfType, "rdf:resource", computeReference("skos", "Concept"));

				addSkosPrefLabel(concept, pi);
				addSkosInScheme(concept, conceptSchemeRef);
				addDcSource(concept, pi);

				if (pi.matches(OWLISO19150.RULE_OWL_ALL_CONSTRAINTS)) {
					addConstraintDeclarations(concept, pi);
				}
			}
		}
	}

	private String computeReference(String prefix, String name) {

		String rdfns = this.usedRdfNamespacesByNsabr.get(prefix);
		return rdfns + name;
	}

	private void addSkosInScheme(Element e, String value) {

		Element is = document.createElementNS(OWLISO19150.RDF_NS_W3C_SKOS,
				"skos:inScheme");
		e.appendChild(is);

		addAttribute(is, "rdf:resource", value);
	}

	private void addDctIsFormatOf(Element e, String value) {

		Element ifo = document.createElementNS(OWLISO19150.RDF_NS_DCT,
				"dct:isFormatOf");
		e.appendChild(ifo);

		addAttribute(ifo, "rdf:resource", value);
	}

	public void addEnumeration(ClassInfo ci) {

		if (finalized) {
			this.result.addWarning(this, 3, ci.name());
			return;
		}

		// create the Datatype <RDFS>
		Element c = document.createElementNS(OWLISO19150.RDF_NS_W3C_RDFS,
				"rdfs:Datatype");
		root.appendChild(c);

		addAttribute(c, "rdf:about",
				computeReference(getPrefix(), normalizedName(ci)));

		addSkosPrefLabel(c, ci);
		addSkosDefinition(c, ci.definition());
		addSkosScopeNote(c, ci.description());
		addDcSource(c, ci);

		if (ci.matches(OWLISO19150.RULE_OWL_ALL_CONSTRAINTS)) {
			addConstraintDeclarations(c, ci);
		}

		// assign stereotype information
		addStereotypeInfo(c, ci);

		/*
		 * TBD Comment 15 by Australia: The classification of a resource as an
		 * ‘enumeration’ is implicit. So it is not necessary to add the
		 * isEnumeration annotation.
		 */
		addIso19150_2IsEnumeration(c, true);

		SortedMap<StructuredNumber, PropertyInfo> enumPis = ci.properties();

		if (enumPis != null && !enumPis.isEmpty()) {

			/*
			 * Comment 5 Canada: <owl:oneOf> list should be embeded in
			 * <owl:equivalentClass> and <rdfs:Datatype> elements
			 */
			Element oneOf = document.createElementNS(
					OWLISO19150.RDF_NS_W3C_OWL, "owl:oneOf");
			c.appendChild(oneOf);

			Element parent = oneOf;

			Element list, first, rest;

			for (PropertyInfo pi : enumPis.values()) {

				list = document.createElementNS(OWLISO19150.RDF_NS_W3C_RDF,
						"rdf:List");
				parent.appendChild(list);

				first = document.createElementNS(OWLISO19150.RDF_NS_W3C_RDF,
						"rdf:first");
				list.appendChild(first);
				addAttribute(first, "rdf:datatype",
						computeReference("xsd", "string"));

				String enumValue = pi.name();
				first.setTextContent(enumValue);

				rest = document.createElementNS(OWLISO19150.RDF_NS_W3C_RDF,
						"rdf:rest");
				list.appendChild(rest);

				parent = rest;
			}
			/*
			 * now 'parent' contains the last 'rest' element of the list - set
			 * it to nil to mark the end of the list
			 */
			addAttribute(parent, "rdf:resource", computeReference("rdf", "nil"));
		}
	}

	/**
	 * @return the prefix
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * @return the prefix defined for the ontology, with 'code' appended.
	 */
	public String getPrefixForCode() {
		return getPrefix() + "code";
	}

	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @return the backPath
	 */
	public String getBackPath() {
		return backPath;
	}

	/**
	 * @return the mpackage
	 */
	public PackageInfo getPackage() {
		return mpackage;
	}

	public Document getDocument() {
		return document;
	}

	public void addClass(ClassInfo ci) {

		this.classInfos.add(ci);
	}
	
	public void print(String outputDirectory, ShapeChangeResult r) {

		String outDirForOntology = outputDirectory + getPath();

		// Check whether we can use the output directory
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
			r.addFatalError(this, 12, outDirForOntology);
			return;
		}

		/*
		 * Uses OutputStreamWriter instead of FileWriter to set character
		 * encoding
		 */

		String fname = outDirForOntology + "/" + getFileName();

		File outFile = new File(outputDirectoryFile, getFileName());

		try {

			String path = new File(fname).getCanonicalPath();
			r.addDebug(this, 20000, path);

			OutputStream fout = new FileOutputStream(outFile);
			OutputStream bout = new BufferedOutputStream(fout);
			OutputStreamWriter outputXML = new OutputStreamWriter(bout, "UTF-8");

			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer();

			DOMSource source = new DOMSource(getDocument());
			StreamResult result = new StreamResult(outputXML);

			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(
					"{http://xml.apache.org/xslt}indent-amount", "" + 2);
			transformer.transform(source, result);

			r.addResult(owliso19150.getTargetID(), outDirForOntology, getFileName(), getRdfNamespace());

		} catch (Exception e) {
			r.addError(this, 5, fname);
			e.printStackTrace(System.err);
		}
	}

	@Override
	public Resource getResource(ClassInfo ci) {
		// TODO Auto-generated method stub
		return null;
	}	
}