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
 * (c) 2002-2016 interactive instruments GmbH, Bonn, Germany
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.ontology.UnionClass;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFList;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.reasoner.ValidityReport;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.OWL2;
import com.hp.hpl.jena.vocabulary.RDFS;

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
 * @author Clemens Portele
 * 
 */
public class OntologyModel implements OntologyDocument, MessageSource {

	protected OntModel ontmodel = ModelFactory
			.createOntologyModel(OntModelSpec.OWL_MEM);
	protected Ontology ontology = null;

	// ontology for classes and properties used in map entries
	protected static com.hp.hpl.jena.rdf.model.Model refmodel = ModelFactory
			.createDefaultModel();

	protected Options options = null;
	public ShapeChangeResult result = null;

	protected Model model = null;
	protected PackageInfo mpackage = null;

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
	protected Map<String, OwlProperty> properties = new HashMap<String, OwlProperty>();

	class OwlProperty {
		protected PropertyInfo pi;
		protected OntProperty p;

		public OwlProperty(PropertyInfo pi, OntProperty p) {
			this.pi = pi;
			this.p = p;
		}
	}

	public OntologyModel(PackageInfo pi, Model m, Options o,
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

		this.name = computeOntologyName();

		if (mpackage
				.matches(OWLISO19150.RULE_OWL_PKG_SINGLE_ONTOLOGY_PER_SCHEMA)
				&& mpackage.matches(
						OWLISO19150.RULE_OWL_PKG_ONTOLOGY_NAME_APP_SCHEMA_CODE)) {
			this.fileName = pi.xmlns() + ".ttl";
			this.path = "";
		} else {
			this.fileName = normalizedName(pi) + ".ttl";
			this.path = computePath(pi);
		}

		/*
		 * As per ISO 19150-2package:rdfNamespace; '#' is the default for the
		 * separator - this can be changed via configuration parameter
		 * OWLISO19150#PARAM_RDF_NAMESPACE_SEPARATOR.
		 */
		this.rdfNamespace = name + owliso19150.getRdfNamespaceSeparator();

		this.backPath = computeBackPath(pi);

		setupModel();
	}

	/**
	 * If the configuration parameter {@link OWLISO19150#PARAM_URIBASE} is set
	 * its value is used as URIbase. Otherwise the targetNamespace of the
	 * package is used as URIbase.
	 * 
	 * @return
	 */
	public String computeUriBase() {

		if (owliso19150.getUriBase() != null) {
			return owliso19150.getUriBase();
		} else {
			return mpackage.targetNamespace();
		}
	}

	/**
	 * Determines the ontology name of a given package. The name is constructed
	 * following the ISO 19150-2owl:ontologyName requirement.
	 * 
	 * The ontologyName is defined via the following rules, in descending
	 * priority:
	 * <ul>
	 * <li>If {@link OWLISO19150#RULE_OWL_PKG_ONTOLOGY_NAME_WITH_PATH} is set
	 * and an according tagged value is set for the package its value is used.
	 * </li>
	 * <li>If {@link OWLISO19150#RULE_OWL_PKG_SINGLE_ONTOLOGY_PER_SCHEMA} and
	 * {@link OWLISO19150#RULE_OWL_PKG_APP_SCHEMA_CODE} are both in effect, the
	 * namespace abbreviation defined for an application schema package is used
	 * for constructing the ontology name (appended to URIbase with "/" as
	 * separator).<br>
	 * NOTE: the filename for that ontology will then also be constructed using
	 * the namespace abbreviation – instead of the package name (which would be
	 * normalized according to 19150-2package:ontologyName).</li>
	 * <li>If the encoding rule
	 * {@link OWLISO19150#RULE_OWL_PKG_PATH_IN_ONTOLOGY_NAME} is in effect, then
	 * the umlPackageName (that is appended to URIbase with "/" as separator) is
	 * constructed using the path of the package to the upmost owner that is in
	 * the same targetNamespace - using a combination of "/" and normalized
	 * package names for all parent packages in the same target namespace</li>
	 * <li>Otherwise just the normalized package name is appended to URIbase
	 * (with "/" as separator), as per 19150-2package:ontologyName.</li>
	 * </ul>
	 * 
	 * @param pi
	 * @return
	 */
	public String computeOntologyName() {

		if (mpackage.matches(
				OWLISO19150.RULE_OWL_PKG_ONTOLOGY_NAME_BY_TAGGED_VALUE)) {

			String ontologyNameTVName = owliso19150
					.getOntologyNameTaggedValue();

			String ontologyName = mpackage.taggedValue(ontologyNameTVName);

			if (ontologyName != null) {
				return ontologyName;
			} else {
				result.addWarning(this, 26,
						OWLISO19150.RULE_OWL_PKG_ONTOLOGY_NAME_BY_TAGGED_VALUE,
						ontologyNameTVName, mpackage.fullNameInSchema());
			}
		}

		String uriBase = computeUriBase();

		String path;

		if (mpackage
				.matches(OWLISO19150.RULE_OWL_PKG_SINGLE_ONTOLOGY_PER_SCHEMA)
				&& mpackage.matches(
						OWLISO19150.RULE_OWL_PKG_ONTOLOGY_NAME_APP_SCHEMA_CODE)) {

			path = "/" + mpackage.xmlns();

		} else if (mpackage
				.matches(OWLISO19150.RULE_OWL_PKG_ONTOLOGY_NAME_WITH_PATH)) {

			path = computePath(mpackage);

		} else {

			// default behavior - as defined by 19150-2package:ontologyName

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
		 * According to http://en.wikipedia.org/wiki/Regular_expression#
		 * POSIX_basic_and_extended the POSIX [:punct:] character class has the
		 * following ASCII punctuation characters:
		 * [][!"#$%&'()*+,./:;<=>?@\^_`{|}~-]
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
	 * 19150-2package:name and 19150-2package:ontologyName.
	 * 
	 * @param pi
	 * @return
	 */
	public static String normalizedName(PackageInfo pi) {

		String result = pi.name();

		// no space characters
		result = result.replaceAll(" ", "");

		/*
		 * dash and underscore characters allowed
		 * 
		 * other punctuation characters are replaced by underscore characters
		 * 
		 * According to http://en.wikipedia.org/wiki/Regular_expression#
		 * POSIX_basic_and_extended the POSIX [:punct:] character class has the
		 * following ASCII punctuation characters:
		 * [][!"#$%&'()*+,./:;<=>?@\^_`{|}~-]
		 * 
		 * In Java this character class can be used in regular expressions via
		 * \p{Punct}. We can omit specific characters in a regular expression
		 * that uses this character class (to keep dash and underscore).
		 */
		result = result.replaceAll("[\\p{Punct}&&[^-_]]", "_");

		// lower case
		result = result.toLowerCase(Locale.ENGLISH);

		/*
		 * only the semantic part of the package name is represented -> this
		 * should be the case as we do not get the fully qualified package name
		 * via PackageInfo#name()
		 */

		return result;
	}

	public void setupModel() {

		this.ontmodel.setNsPrefix("rdf", OWLISO19150.RDF_NS_W3C_RDF);
		this.ontmodel.setNsPrefix("rdfs", OWLISO19150.RDF_NS_W3C_RDFS);
		this.ontmodel.setNsPrefix("owl", OWLISO19150.RDF_NS_W3C_OWL);
		this.ontmodel.setNsPrefix("skos", OWLISO19150.RDF_NS_W3C_SKOS);
		this.ontmodel.setNsPrefix("dc", OWLISO19150.RDF_NS_DC);
		this.ontmodel.setNsPrefix("dct", OWLISO19150.RDF_NS_DCT);
		this.ontmodel.setNsPrefix("xsd", OWLISO19150.RDF_NS_W3C_XML_SCHEMA);
		this.ontmodel.setNsPrefix(OWLISO19150.PREFIX_ISO_19150_2,
				OWLISO19150.RDF_NS_ISO_19150_2);
		this.ontmodel.setNsPrefix(prefix, rdfNamespace);

		ontology = ontmodel.createOntology(name);

		/*
		 * rdfs:label - According to 19150-2package:package it should be the
		 * "full name of the corresponding UML PACKAGE". We interpret this as
		 * the full local name. According to 19150-2app:documentation-ontology
		 * it should be the "human-readable" title of the application schema",
		 * which should be the same.
		 * 
		 * TODO: rather than the package name the designator/alias, if present,
		 * could work just as well - this should be controlled via the
		 * descriptorTarget.
		 */
		ontology.addLabel(mpackage.name(), owliso19150.getLanguage());

		if (mpackage.matches(OWLISO19150.RULE_OWL_PKG_DCT_SOURCE_TITLE)) {
			ontology.addProperty(DCTerms.source,
					owliso19150.computeSource(mpackage),
					owliso19150.getLanguage());
		}

		// TODO use configuration from descriptorTarget
		String documentation = "";
		String def = mpackage.definition();
		if (def != null)
			documentation = def + "\n";
		String desc = mpackage.description();
		if (desc != null)
			documentation += desc;
		documentation = documentation.trim();
		if (documentation.length() > 0) {
			ontology.addComment(documentation, owliso19150.getLanguage());
		}

		// add version information
		ontology.addVersionInfo(mpackage.version());

		if (mpackage.matches(OWLISO19150.RULE_OWL_PKG_VERSION_IRI)
				&& mpackage.version() != null
				&& !mpackage.version().trim().isEmpty()) {

			// TODO - ensure that resulting URI is properly escaped

			ontology.addProperty(ResourceFactory
					.createProperty(OWLISO19150.RDF_NS_W3C_OWL, "versionIRI"),
					name + "/" + mpackage.version());
		}

		if (mpackage.matches(OWLISO19150.RULE_OWL_PKG_IMPORT_191502BASE)) {
			addImport(OWLISO19150.RDF_NS_ISO_19150_2,
					config.locationOfNamespace(OWLISO19150.RDF_NS_ISO_19150_2));
		}

		// TODO add information IRI using rdfs:isDefinedBy
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

	public void finalizeDocument() {

		for (ClassInfo ci : this.classInfos) {
			createClass(ci);
		}

		// ensure that no further classes are added once finalized
		finalized = true;
	}

	private Resource createClass(ClassInfo ci) {
		Resource r = null;

		// otherwise we have to create the resource for the class...
		int cat = ci.category();

		switch (cat) {
		case Options.FEATURE:
			r = addFeature(ci);
			break;
		case Options.OBJECT:
			r = addInterface(ci);
			break;
		case Options.DATATYPE:
			r = addDatatype(ci);
			break;
		case Options.UNION:
			r = addUnion(ci);
			break;
		case Options.CODELIST:
			r = addCodelist(ci);
			break;
		case Options.ENUMERATION:
			r = addEnumeration(ci);
			break;
		default:
			MessageContext mc = result.addError(this, 5, "" + cat);
			mc.addDetail(this, 30000, ci.fullName());
		}
		return r;
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

	public Resource addFeature(ClassInfo ci) {
		return addClassDefinition(ci);
	}

	public Resource addClassDefinition(ClassInfo ci) {

		if (finalized) {
			this.result.addWarning(this, 3, ci.name());
			return null;
		}

		// create the Class <OWL>
		OntClass c = ontmodel
				.createClass(computeReference(getPrefix(), normalizedName(ci)));

		setLabelsAndAnnotations(c, ci);

		if (ci.matches(OWLISO19150.RULE_OWL_ALL_CONSTRAINTS)) {
			addConstraintDeclarations(c, ci);
		}

		if (ci.isAbstract()
				&& ci.matches(OWLISO19150.RULE_OWL_CLS_19150_2_ISABSTRACT)) {
			c.addLiteral(ISO19150_2.isAbstract,
					ontmodel.createTypedLiteral(true));
		}

		// assign stereotype information
		// TODO - replace with generic subClassOf mapping mechanism
		addStereotypeInfo(c, ci);

		// determine if this is a subclass of one or more specific types
		if ((ci.baseClass() != null
				|| (ci.supertypes() != null && !ci.supertypes().isEmpty()))
				&& ci.matches(OWLISO19150.RULE_OWL_CLS_GENERALIZATION)) {

			String baseClassId = null;

			if (ci.baseClass() != null) {

				ClassInfo supertype = ci.baseClass();

				baseClassId = supertype.id();

				mapAndAddSuperClass(ci, c, supertype);
			}

			if (ci.supertypes() != null && !ci.supertypes().isEmpty()) {

				// for now create one subClassOf for each supertype
				for (String ciId : ci.supertypes()) {

					if (baseClassId != null && baseClassId.equals(ciId)) {
						continue;
					}

					ClassInfo supertype = model.classById(ciId);

					mapAndAddSuperClass(ci, c, supertype);
				}
			}
		}

		// now add the properties
		SortedMap<StructuredNumber, PropertyInfo> pis = ci.properties();

		if (pis != null) {
			for (PropertyInfo pi : pis.values()) {
				if (pi.isNavigable()) {
					Property p = addPropertyDeclaration(pi);

					// establish multiplicity and all-values-from restrictions
					if (p != null) {
						addMultiplicity(c, pi, p);
						// add no all-values-from for mapped properties
						if (p.getModel() == ontmodel)
							addAllValuesFrom(c, pi, p);
					}
				}
			}
		}

		return c;
	}

	private void mapAndAddSuperClass(ClassInfo ci, OntClass c,
			ClassInfo supertype) {

		Resource mappedResource = mapResource(supertype, true);

		if (mappedResource == null) {
			MessageContext mc = result.addError(this, 6, supertype.name(),
					ci.name());
			mc.addDetail(this, 10000, ci.fullName());
			c.addComment(
					"This class has a supertype for which no RDF representation is known: "
							+ supertype.name(),
					"en");
		} else {
			c.addSuperClass(mappedResource);
		}
	}

	private void setLabelsAndAnnotations(OntResource c, Info i) {

		// TODO - replace with descriptorTargets implementation

		c.addProperty(SKOS.prefLabel, prefLabel(i), owliso19150.getLanguage());
		c.addProperty(SKOS.notation, i.name(), XSDDatatype.XSDNCName);
		String s = i.definition();
		if (s != null && !s.trim().isEmpty())
			c.addProperty(SKOS.definition, s, owliso19150.getLanguage());
		s = i.description();
		if (s != null && !s.trim().isEmpty())
			c.addProperty(SKOS.scopeNote, s, owliso19150.getLanguage());
		if (!i.matches(OWLISO19150.RULE_OWL_ALL_SUPPRESS_DC_SOURCE)) {
			s = owliso19150.computeSource(i);
			if (s != null && !s.trim().isEmpty())
				c.addProperty(DC.source, s, owliso19150.getLanguage());
		}
	}

	private void addStereotypeInfo(OntResource c, ClassInfo ci) {

		/*
		 * TODO - copy/adapt this as a general mechanism for adding subClassOf -
		 * and maybe subPropertyOf - relationships; identify class or property
		 * by its name (full name if scoped to class) and allow selection via
		 * stereotype; allow multiple subXxxOf per schema component; plan is to
		 * realize this using customized map entries (specific to OWL target,
		 * just like XML map entries are specific to the XSD target)
		 */

		Map<String, String> stereotypeMappings = owliso19150.getConfig()
				.getStereotypeMappings();
		if (stereotypeMappings == null) {
			// initialize map so that following tests are simpler
			stereotypeMappings = new HashMap<String, String>();
		}

		int cat = ci.category();

		switch (cat) {
		case Options.FEATURE:

			boolean featureFlag = false;

			if (ci.matches(OWLISO19150.RULE_OWL_CLS_GEOSPARQL_FEATURES)) {
				addImport(OWLISO19150.RDF_NS_OGC_GEOSPARQL, config
						.locationOfNamespace(OWLISO19150.RDF_NS_OGC_GEOSPARQL));
				c.asClass().addSuperClass(ontmodel.createResource(
						OWLISO19150.RDF_NS_OGC_GEOSPARQL + "Feature"));
				featureFlag = true;
			}

			// REMOVED IN ISO 19150-2 FINAL
			// if (ci.matches(OWLISO19150.RULE_OWL_CLS_19150_2_FEATURES)) {
			// /*
			// * import for 19150-2 base is automatically added in
			// * createDocument()
			// */
			// c.asClass()
			// .addSuperClass(ontmodel.createResource(
			// computeReference(OWLISO19150.PREFIX_ISO_19150_2,
			// "FeatureType")));
			//
			// addImport(OWLISO19150.RDF_NS_ISO_GFM,
			// config.locationOfNamespace(OWLISO19150.RDF_NS_ISO_GFM));
			// c.asClass().addSuperClass(ontmodel.createResource(
			// OWLISO19150.RDF_NS_ISO_GFM + "AnyFeature"));
			// featureFlag = true;
			// }

			if (stereotypeMappings.containsKey("featuretype")) {
				String mappingForFeatureType = stereotypeMappings
						.get("featuretype");
				if (mappingForFeatureType != null) {
					c.asClass().addSuperClass(
							ontmodel.createResource(mappingForFeatureType));
					featureFlag = true;
				}
			}
			if (!featureFlag) {
				result.addWarning(this, 8, ci.name(), "featuretype");
			}
			break;

		case Options.OBJECT:
			if (stereotypeMappings.containsKey("type")) {
				c.asClass().addSuperClass(ontmodel
						.createResource(stereotypeMappings.get("type")));
			}
			break;

		case Options.DATATYPE:
			if (stereotypeMappings.containsKey("datatype")) {
				c.asClass().addSuperClass(ontmodel
						.createResource(stereotypeMappings.get("datatype")));
			}

			break;
		case Options.CODELIST:
			if (stereotypeMappings.containsKey("codelist")) {
				c.asClass().addSuperClass(ontmodel
						.createResource(stereotypeMappings.get("codelist")));
			}
			break;

		case Options.UNION:
			if (stereotypeMappings.containsKey("union")) {
				c.asClass().addSuperClass(ontmodel
						.createResource(stereotypeMappings.get("union")));
			}
			break;

		case Options.ENUMERATION:
			if (stereotypeMappings.containsKey("enumeration")) {
				c.asClass().addSuperClass(ontmodel
						.createResource(stereotypeMappings.get("enumeration")));
			}

			// /*
			// * TODO Comment 15 by Australia: The classification of a resource
			// as
			// * an ‘enumeration’ is implicit. So it is not necessary to add the
			// * isEnumeration annotation.
			// */
			// removed in final version of ISO 19150-2
			// c.addLiteral(ISO19150_2.isEnumeration,
			// ontmodel.createTypedLiteral(true));
			break;

		default:
			result.addWarning(this, 9, ci.name(), ci.stereotypes().toString());
		}
	}

	protected void addImport(String rdfns, String uri) {

		if (rdfns == null || rdfns.equals(this.rdfNamespace)
				|| rdfns.equals(getCodeNamespace())) {
			return;
		}

		if (uri != null) {
			if (ontmodel.hasLoadedImport(uri)) {
				return;
			}

			ontology.addImport(ontmodel.createResource(uri));
		}

		// determine prefix for rdf namespace
		String prefix = owliso19150.computePrefixForRdfNamespace(rdfns);

		if (prefix != null) {
			String s = ontmodel.getNsPrefixURI(prefix);
			if (s == null)
				ontmodel.setNsPrefix(prefix, rdfns);
			else if (!s.equals(rdfns))
				result.addError(this, 11, getName(), prefix, rdfns, s);
		} else {
			result.addWarning(this, 4, rdfns);
		}
	}

	protected void addMultiplicity(OntClass cls, PropertyInfo pi, Property p) {

		Multiplicity m = pi.cardinality();

		if (pi.voidable() && pi.matches(
				OWLISO19150.RULE_OWL_PROP_VOIDABLE_AS_MINCARDINALITY0)) {
			// FIXME - this changes the multiplicity in the input model, which
			// is dangerous if it is processed by other targets as well (because
			// a model would be shared by multiple targets)!
			m.minOccurs = 0;
		}

		if (m.minOccurs == m.maxOccurs && !pi.matches(
				OWLISO19150.RULE_OWL_PROP_SUPPRESS_CARDINALITY_RESTRICTIONS)) {
			OntClass restriction = ontmodel.createCardinalityRestriction(null,
					p, m.minOccurs);
			cls.addSuperClass(restriction);
		} else {
			// set min cardinality if required
			if (m.minOccurs == 0 || pi.matches(
					OWLISO19150.RULE_OWL_PROP_SUPPRESS_CARDINALITY_RESTRICTIONS)) {
				// simply omit min cardinality to represent this case
			} else {
				OntClass restriction = ontmodel
						.createMinCardinalityRestriction(null, p, m.minOccurs);
				cls.addSuperClass(restriction);
			}

			// set max cardinality if required
			if (m.maxOccurs == Integer.MAX_VALUE || pi.matches(
					OWLISO19150.RULE_OWL_PROP_SUPPRESS_CARDINALITY_RESTRICTIONS)) {
				// simply omit max cardinality to represent this case
			} else {
				OntClass restriction = ontmodel
						.createMaxCardinalityRestriction(null, p, m.maxOccurs);
				cls.addSuperClass(restriction);
			}
		}
	}

	protected void addAllValuesFrom(OntClass cls, PropertyInfo pi, Property p) {
		// get referenced resource
		Resource r = p.getPropertyResourceValue(RDFS.range);
		// add allValuesFrom, in case the suppressing conversion rule is active
		// do it only for cases with unionOf values or no range
		if (r == null || r.hasProperty(OWL.unionOf) || !pi.matches(
				OWLISO19150.RULE_OWL_PROP_SUPPRESS_ALLVALUESFROM_RESTRICTIONS)) {
			OntClass restriction = ontmodel.createAllValuesFromRestriction(null,
					p, mapTypeResource(pi));
			cls.addSuperClass(restriction);
		}
	}

	private boolean isDatatypeProperty(PropertyInfo pi) {
		boolean result;
		ProcessMapEntry pme = config.getMapEntry(pi.typeInfo().name);
		if (pme == null) {
			int cat = pi.categoryOfValue();
			result = (cat == Options.ENUMERATION);
		} else {
			result = (pme.hasParam()
					&& pme.getParam().equalsIgnoreCase("datatype"));
		}
		return result;
	}

	private Property addPropertyDeclaration(PropertyInfo pi) {

		if (!pi.isNavigable()) {
			return null;
		}

		Property mappedProperty = mapProperty(pi);
		if (mappedProperty != null) {
			if (mappedProperty.getURI()
					.equals(computeReference("sc", "null"))) {
				MessageContext mc = result.addInfo(this, 20, pi.name());
				if (mc != null)
					mc.addDetail(this, 10001, pi.inClass().fullName(),
							pi.name());
				return null;
			} else {
				MessageContext mc = result.addInfo(this, 21, pi.name(),
						mappedProperty.getURI());
				if (mc != null)
					mc.addDetail(this, 10001, pi.inClass().fullName(),
							pi.name());
				return mappedProperty;
			}
		}

		boolean isGlobalProperty = false;
		if (globalPropertyNames.contains("*")
				|| globalPropertyNames.contains(pi.name())) {
			isGlobalProperty = true;
		}

		String propAbout;
		if (isGlobalProperty) {
			propAbout = computeReference(getPrefix(), normalizedName(pi));
		} else {
			propAbout = computeReference(getPrefix(),
					normalizedName(pi.inClass()) + "." + normalizedName(pi));
		}

		if (properties.containsKey(propAbout)) {
			// a property with this id has already been declared - do not add it
			// again,
			// but verify the specifications are consistent
			OwlProperty p0 = properties.get(propAbout);
			if (isDatatypeProperty(pi) != isDatatypeProperty(p0.pi)) {
				MessageContext mc = result.addError(this, 23, propAbout,
						pi.typeInfo().name, p0.pi.typeInfo().name);
				mc.addDetail(this, 10001, pi.inClass().fullName(), pi.name());
				mc.addDetail(this, 10001, p0.pi.inClass().fullName(),
						p0.pi.name());
			} else if (!pi.typeInfo().name
					.equalsIgnoreCase(p0.pi.typeInfo().name)) {
				if (isDatatypeProperty(pi)) {
					MessageContext mc = result.addError(this, 16, propAbout,
							pi.typeInfo().name, p0.pi.typeInfo().name);
					mc.addDetail(this, 10001, pi.inClass().fullName(),
							pi.name());
					mc.addDetail(this, 10001, p0.pi.inClass().fullName(),
							p0.pi.name());
					p0.p.removeAll(RDFS.range);
				} else {
					Resource range = mapTypeResource(pi);
					Statement rangeStatement = p0.p.getProperty(RDFS.range);
					Resource object = rangeStatement.getObject().asResource();
					if (object.hasProperty(OWL.unionOf)) {
						UnionClass unionOf = object.as(UnionClass.class);
						unionOf.addOperand(range);
					} else {
						RDFNode[] nodes1 = { object, range };
						RDFList list = ontmodel.createList(nodes1);
						UnionClass unionOf = ontmodel.createUnionClass(null,
								list);
						rangeStatement.changeObject(unionOf);
					}
					MessageContext mc = result.addError(this, 25, propAbout,
							pi.typeInfo().name, p0.pi.typeInfo().name);
					mc.addDetail(this, 10001, pi.inClass().fullName(),
							pi.name());
					mc.addDetail(this, 10001, p0.pi.inClass().fullName(),
							p0.pi.name());
				}
			}
			String s1 = pi.definition();
			if (s1 == null)
				s1 = "";
			String s2 = p0.pi.definition();
			if (s2 == null)
				s2 = "";
			if (!s1.equalsIgnoreCase(s2)) {
				p0.p.addProperty(SKOS.definition, s1,
						owliso19150.getLanguage());
				MessageContext mc = result.addWarning(this, 17, propAbout, s1,
						s2);
				mc.addDetail(this, 10001, pi.inClass().fullName(), pi.name());
				mc.addDetail(this, 10001, p0.pi.inClass().fullName(),
						p0.pi.name());
			}
			s1 = pi.description();
			if (s1 == null)
				s1 = "";
			s2 = p0.pi.description();
			if (s2 == null)
				s2 = "";
			if (!s1.equalsIgnoreCase(s2)) {
				p0.p.addProperty(SKOS.scopeNote, s1, owliso19150.getLanguage());
				MessageContext mc = result.addWarning(this, 18, propAbout, s1,
						s2);
				mc.addDetail(this, 10001, pi.inClass().fullName(), pi.name());
				mc.addDetail(this, 10001, p0.pi.inClass().fullName(),
						p0.pi.name());
			}
			s1 = prefLabel(pi);
			s2 = prefLabel(p0.pi);
			if (!s1.equalsIgnoreCase(s2)) {
				p0.p.addProperty(SKOS.altLabel, s1, owliso19150.getLanguage());
				MessageContext mc = result.addWarning(this, 19, propAbout, s1,
						s2);
				mc.addDetail(this, 10001, pi.inClass().fullName(), pi.name());
				mc.addDetail(this, 10001, p0.pi.inClass().fullName(),
						p0.pi.name());
			}

			// return the existing property
			return p0.p;
		}

		OntProperty p;

		// Determine if this is a DatatypeProperty or ObjectProperty
		if (isDatatypeProperty(pi)) {

			// we have a datatype
			DatatypeProperty dp = ontmodel.createDatatypeProperty(propAbout);
			p = dp.asProperty();

		} else {

			// we have an object type
			ObjectProperty op = ontmodel.createObjectProperty(propAbout);
			p = op.asProperty();

		}

		setLabelsAndAnnotations(p, pi);

		if (pi.matches(OWLISO19150.RULE_OWL_ALL_CONSTRAINTS)) {
			addConstraintDeclarations(p, pi);
		}

		// no domain should be declared for a global property
		if (!isGlobalProperty) {
			p.addDomain(ontmodel.createResource(computeReference(getPrefix(),
					normalizedName(pi.inClass()))));
		}

		Resource range = mapTypeResource(pi);
		p.addRange(range);
		if (range == OWL2.Class)
			p.addComment(
					"The range is a type for which no RDF representation is known: "
							+ pi.typeInfo().name,
					"en");

		AssociationInfo ai = pi.association();

		if (ai != null) {

			if (ai.assocClass() != null) {
				// TODO
				result.addError(this, 15, ai.assocClass().name());
			}

			// ensure that owl:inverseOf is set only once
			if (!owliso19150.getProcessedAssociations().contains(ai)) {

				// check if we need to set owl:inverseOf

				PropertyInfo revPi = pi.reverseProperty();

				if (revPi != null && revPi.isNavigable()
						&& pi.matches(OWLISO19150.RULE_OWL_PROP_INVERSEOF)) {

					owliso19150.getProcessedAssociations().add(ai);

					// TODO inverseOf currently only supported for associations
					// within a single schema
					if (pi.inClass().pkg().schemaId()
							.equals(revPi.inClass().pkg().schemaId())) {
						Property ip = addPropertyDeclaration(revPi);
						if (ip != null)
							p.addInverseOf(ip);
						else {
							result.addError(this, 13, propAbout);
						}
					}

				}
			}

			// add association name if it exists
			String aiName = ai.name();
			if (aiName != null && aiName.length() > 0 && pi.matches(
					OWLISO19150.RULE_OWL_PROP_ISO191502_ASSOCIATION_NAME)) {
				p.addProperty(ISO19150_2.associationName, aiName);
			}

			if ((pi.isComposition() || pi.isAggregation()) && pi
					.matches(OWLISO19150.RULE_OWL_PROP_ISO191502_AGGREGATION)) {
				if (pi.isComposition()) {
					p.addProperty(ISO19150_2.aggregationType,
							"partOfCompositeAggregation");
				} else if (pi.isAggregation()) {
					p.addProperty(ISO19150_2.aggregationType,
							"partOfSharedAggregation");
				} else {
					// no special aggregation to document
				}
			}
		}

		// remember property
		properties.put(propAbout, new OwlProperty(pi, p));

		return p;
	}

	private Resource mapResource(String qname) {
		String[] qnamePars = qname.split(":");
		String prefix = qnamePars[0];
		String resourceName = qnamePars[1];

		// identify rdf namespace based upon prefix and standard namespaces
		String rdfNs = config.fullNamespace(prefix);
		String location = config.locationOfNamespace(rdfNs);

		String uri = rdfNs + resourceName;
		Resource r = refmodel.getResource(uri);
		if (r == null)
			r = refmodel.createResource(uri);

		// also add import for the namespace
		addImport(rdfNs, location);

		// return correct element definition
		return r;
	}

	protected Resource mapResource(ClassInfo ci, boolean processMapEntry) {

		if (ci == null)
			return null;

		if (processMapEntry) {
			ProcessMapEntry pme = config.getMapEntry(ci.name());
			if (pme != null) {
				Resource r = mapResource(pme.getTargetType());
				MessageContext mc = result.addInfo(this, 22, ci.name(),
						r.getURI());
				if (mc != null) {
					mc.addDetail(this, 10000, ci.fullName());
				}
				return r;
			}
		}

		// lookup the ontology to which the class belongs
		OntologyDocument od = owliso19150.computeRelevantOntology(ci);

		if (od == null) {
			// FIXME this.result.addError(this, 7, ci.name());
			return null;
		}

		String rdfNs = od.getRdfNamespace();
		String location = od.getName();

		if (ci.category() == Options.CODELIST) {
			if (ci.matches(OWLISO19150.RULE_OWL_CLS_CODELIST_EXTERNAL)) {
				String uri = ci.taggedValue("codeList");
				if (uri == null || uri.isEmpty())
					uri = ci.taggedValue("vocabulary");
				if (uri != null && (uri.startsWith("http://")
						|| uri.startsWith("https://"))) {
					result.addInfo(this, 24, ci.name(), uri);
					Resource r = refmodel.getResource(uri);
					if (r == null)
						r = refmodel.createResource(uri);
					return r;
				} else {
					result.addInfo(this, 24, ci.name(), "owl:Class");
					return OWL2.Class;
				}
			} else {
				rdfNs = od.getCodeNamespace();
				location = null;
			}
		}

		// add import for the namespace and declare prefix
		addImport(rdfNs, location);

		// return resource
		return od.getResource(ci);
	}

	protected Resource mapTypeResource(PropertyInfo pi) {

		Type ti = pi.typeInfo();

		if (ti == null)
			return null;

		ProcessMapEntry pme = config.getMapEntry(ti.name);
		if (pme != null)
			return mapResource(pme.getTargetType());

		ClassInfo ci = model.classById(ti.id);

		if (ci == null) {
			// in case that the model references the type by name only
			ci = model.classByName(ti.name);
		}

		if (ci == null) {
			MessageContext mc = this.result.addError(this, 7, ti.name);
			if (mc != null) {
				mc.addDetail(this, 10001, pi.inClass().fullName(), pi.name());
			}
			return OWL2.Class;
		}

		Resource r = mapResource(ci, false);
		if (r == null) {
			MessageContext mc = this.result.addError(this, 7, ti.name);
			if (mc != null) {
				mc.addDetail(this, 10001, pi.inClass().fullName(), pi.name());
			}
			return OWL2.Class;
		}

		return r;
	}

	protected Property mapProperty(PropertyInfo pi) {

		if (pi == null)
			return null;

		// look for map entry based on the name of the property
		ProcessMapEntry pme = config.getMapEntry(pi.name());
		if (pme == null || !pme.hasParam()
				|| !pme.getParam().equalsIgnoreCase("property")) {
			// look for map entry based on the name of the range
			pme = config.getMapEntry(pi.typeInfo().name);
			if (pme == null || !pme.hasParam() || !pme.getParam()
					.equalsIgnoreCase("propertyByValueType")) {
				return null;
			}
		}

		// get QName
		String qname = pme.getTargetType();

		if (qname.isEmpty()) {
			// property to be dropped
			qname = "sc:null";
		}

		String[] qnamePars = qname.split(":");
		String prefix = qnamePars[0];
		String refName = qnamePars[1];

		// identify rdf namespace based upon prefix and standard namespaces
		String rdfNs = config.fullNamespace(prefix);
		String location = config.locationOfNamespace(rdfNs);

		// also add import for the namespace
		if (!qname.equalsIgnoreCase("sc:null"))
			addImport(rdfNs, location);

		// return property, create if needed
		String propAbout = computeReference(prefix, refName);
		Property p = refmodel.getProperty(propAbout);
		if (p == null)
			p = refmodel.createProperty(propAbout);
		return p;

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
		 * According to http://en.wikipedia.org/wiki/Regular_expression#
		 * POSIX_basic_and_extended the POSIX [:punct:] character class has the
		 * following ASCII punctuation characters:
		 * [][!"#$%&'()*+,./:;<=>?@\^_`{|}~-]
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

	public String getFileName() {
		return fileName;
	}

	private String prefLabel(Info i) {
		String alias = i.aliasName();
		if (alias != null && alias.length() > 0) {
			return alias;
		} else {
			return i.name();
		}
	}

	public Resource addInterface(ClassInfo ci) {
		return addClassDefinition(ci);
	}

	public Resource addDatatype(ClassInfo ci) {
		return addClassDefinition(ci);
	}

	public Resource addUnion(ClassInfo ci) {

		if (finalized) {
			this.result.addWarning(this, 3, ci.name());
			return null;
		}

		// create the Class <OWL>
		OntClass c = ontmodel
				.createClass(computeReference(getPrefix(), normalizedName(ci)));

		setLabelsAndAnnotations(c, ci);

		if (ci.matches(OWLISO19150.RULE_OWL_ALL_CONSTRAINTS)) {
			addConstraintDeclarations(c, ci);
		}

		// assign stereotype information
		addStereotypeInfo(c, ci);

		// TODO implement
		result.addError(this, 14, ci.name());

		return c;
	}

	private void addConstraintDeclarations(Resource r, ClassInfo ci) {
		List<Constraint> cons = ci.constraints();
		if (cons != null && !cons.isEmpty()) {
			for (Constraint c : cons) {
				r.addProperty(ISO19150_2.constraint,
						c.name() + ": " + c.text());
			}
		}
	}

	private void addConstraintDeclarations(Resource r, PropertyInfo pi) {
		List<Constraint> cons = pi.constraints();
		if (cons != null && !cons.isEmpty()) {
			for (Constraint c : cons) {
				r.addProperty(ISO19150_2.constraint, c.text());
			}
		}
	}

	public Resource addCodelist(ClassInfo ci) {

		if (finalized) {
			this.result.addWarning(this, 3, ci.name());
			return null;
		}

		// code lists are usually managed outside of the application schema. If
		// this
		// schema conversion rule is active, we skip this classifier
		if (ci.matches(OWLISO19150.RULE_OWL_CLS_CODELIST_EXTERNAL))
			return null;

		String classURI = computeReference(getPrefix(), normalizedName(ci));
		String schemeURI = getCodeNamespace() + normalizedName(ci);

		// create the Class <OWL>
		OntClass c = ontmodel.createClass(classURI);

		setLabelsAndAnnotations(c, ci);

		if (ci.matches(OWLISO19150.RULE_OWL_ALL_CONSTRAINTS)) {
			addConstraintDeclarations(c, ci);
		}

		// assign stereotype information
		addStereotypeInfo(c, ci);

		c.addSuperClass(ontmodel
				.createResource(OWLISO19150.RDF_NS_W3C_SKOS + "Concept"));

		// create ConceptScheme <SKOS>
		OntResource cs = ontmodel.createOntResource(schemeURI);
		cs.addRDFType(ontmodel
				.createResource(OWLISO19150.RDF_NS_W3C_SKOS + "ConceptScheme"));
		setLabelsAndAnnotations(cs, ci);
		cs.addProperty(DCTerms.isFormatOf, classURI);

		// now add the individual concept definitions
		SortedMap<StructuredNumber, PropertyInfo> clPis = ci.properties();

		if (clPis != null && !clPis.isEmpty()) {

			for (PropertyInfo pi : clPis.values()) {
				OntResource clv = ontmodel
						.createOntResource(schemeURI + "/" + pi.name());
				clv.addRDFType(ontmodel.createResource(
						OWLISO19150.RDF_NS_W3C_SKOS + "Concept"));
				setLabelsAndAnnotations(clv, pi);
				clv.addProperty(SKOS.inScheme, schemeURI);

				if (pi.matches(OWLISO19150.RULE_OWL_ALL_CONSTRAINTS)) {
					addConstraintDeclarations(clv, pi);
				}
			}
		}
		return c;
	}

	private String computeReference(String prefix, String name) {

		String rdfns = ontmodel.getNsPrefixURI(prefix);
		return rdfns + name;
	}

	public Resource addEnumeration(ClassInfo ci) {

		if (finalized) {
			this.result.addWarning(this, 3, ci.name());
			return null;
		}

		// create the Datatype <RDFS>
		OntResource e = ontmodel.createOntResource(
				computeReference(getPrefix(), normalizedName(ci)));
		e.addRDFType(ontmodel
				.createResource(OWLISO19150.RDF_NS_W3C_RDFS + "Datatype"));
		setLabelsAndAnnotations(e, ci);

		if (ci.matches(OWLISO19150.RULE_OWL_ALL_CONSTRAINTS)) {
			addConstraintDeclarations(e, ci);
		}

		// assign stereotype information
		addStereotypeInfo(e, ci);

		SortedMap<StructuredNumber, PropertyInfo> enumPis = ci.properties();

		if (enumPis != null && !enumPis.isEmpty()) {
			/*
			 * Comment 5 Canada: <owl:oneOf> list should be embeded in
			 * <owl:equivalentClass> and <rdfs:Datatype> elements
			 */
			RDFList enums = ontmodel.createList();
			for (PropertyInfo pi : enumPis.values()) {
				Literal en = ontmodel.createLiteral(pi.name());
				enums = enums.cons(en);
			}
			e.addProperty(OWL2.oneOf, enums);
		}
		return e;
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

	public OntModel getOntologyModel() {
		return ontmodel;
	}

	public void addClass(ClassInfo ci) {
		this.classInfos.add(ci);
	}

	private void validate() {
		ValidityReport r = ontmodel.validate();
		if (r != null && !r.isValid())
			result.addError(this, 12, name, r.toString());
	}

	@Override
	public void print(String outputDirectory, ShapeChangeResult r) {
		validate();

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
			OutputStreamWriter outputWriter = new OutputStreamWriter(bout,
					"UTF-8");

			ontmodel.write(outputWriter, "TTL");
//			JenaJSONLD.init();
//			ontmodel.write(outputWriter, "JSON-LD");
			// ontmodel.write(outputWriter ,"RDF/XML-ABBREV");
			r.addResult(owliso19150.getTargetID(), outDirForOntology,
					getFileName(), getName());

		} catch (Exception e) {
			r.addError(this, 5, fname);
			e.printStackTrace(System.err);
		}
	}

	public Resource getResource(ClassInfo ci) {
		Resource r = ontmodel
				.getResource(computeReference(getPrefix(), normalizedName(ci)));
		return r;
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
			return "Unsupported class category encountered while processing the stereotype '$2$' of class '$1$'.";
		case 10:
			return "Duplicate property mapping encountered in union (for type named '$1$').";
		case 11:
			return "In ontology '$1$' the prefix '$2$' is used for multiple URIs: '$3$' and '$4$'.";
		case 12:
			return "Ontology '$1$' is not valid: $2$";
		case 13:
			return "Inverse property of '$1$' not found. The owl:inverseOf property has not been added.";
		case 14:
			return "Union data types are not implemented. Only placeholder implemented for '$1$'.";
		case 15:
			return "Association classes are not implemented. Association class '$1$' not represented in the ontology.";
		case 16:
			return "Property '$1$' has inconsistent ranges: '$2$' and '$3$'. The rdfs:range property has been dropped.";
		case 17:
			return "Property '$1$' may have inconsistent definitions: '$2$' and '$3$'. Please clean up the RDF vocabulary.";
		case 18:
			return "Property '$1$' may have inconsistent descriptions: '$2$' and '$3$'. Please clean up the RDF vocabulary.";
		case 19:
			return "Property '$1$' has inconsistent labels: '$2$' and '$3$'. Please clean up the RDF vocabulary.";
		case 20:
			return "Property '$1$' has been dropped as specified in the configuration.";
		case 21:
			return "Property '$1$' has been mapped to '$2$' as specified in the configuration.";
		case 22:
			return "Class '$1$' has been mapped to '$2$' as specified in the configuration.";
		case 23:
			return "Property '$1$' has inconsistent ranges: '$2$' and '$3$'. One is a datatype and one a class.";
		case 24:
			return "Code list '$1$' is managed separately and the range is represented by the class '$2$'.";
		case 25:
			return "Property '$1$' has inconsistent ranges: '$2$' and '$3$'. A union has been created. Please review the RDF vocabulary.";
		case 26:
			return "Rule $1$ is in effect, but tagged value '$2$' was not found. Ignoring the rule for computing the ontology name of package '$3$'.";
		case 10000:
			return "--- Context - class: '$1$'";
		case 10001:
			return "--- Context - class: '$1$', property: '$2$'";
		case 20000:
			return "Writing ontology to file at $1$";

		}
		return null;
	}
}