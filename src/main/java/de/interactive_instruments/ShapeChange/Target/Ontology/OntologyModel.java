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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;

import de.interactive_instruments.ShapeChange.DescriptorTarget;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Multiplicity;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.PropertyConversionParameter;
import de.interactive_instruments.ShapeChange.RdfPropertyMapEntry;
import de.interactive_instruments.ShapeChange.RdfTypeMapEntry;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.StructuredNumber;
import de.interactive_instruments.ShapeChange.TargetOwlConfiguration;
import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.TypeConversionParameter;
import de.interactive_instruments.ShapeChange.ConstraintMapping;
import de.interactive_instruments.ShapeChange.ConstraintMapping.ConstraintType;
import de.interactive_instruments.ShapeChange.Model.AssociationInfo;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.FolConstraint;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.OclConstraint;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.StereotypeConversionParameter;

/**
 * @author Clemens Portele, Johannes Echterhoff
 * 
 */
public class OntologyModel implements MessageSource {

	private OntModel ontmodel = ModelFactory
			.createOntologyModel(OntModelSpec.OWL_MEM);
	private Ontology ontology = null;
	private OntModel refmodel = ModelFactory
			.createOntologyModel(OntModelSpec.OWL_MEM);

	private Options options = null;
	public ShapeChangeResult result = null;

	private Model model = null;
	private PackageInfo mpackage = null;

	/**
	 * group 0: the whole input string group 1: the string to use as separator
	 * of multiple values; can be <code>null</code> group 2: the name of the
	 * tagged value; cannot be <code>null</code>
	 */
	private Pattern descriptorTargetTaggedValuePattern = Pattern
			.compile("TV(\\(.+?\\))?:(.+)");

	private String name;
	private String fileName;
	private String rdfNamespace;
	private String prefix;
	private OWLISO19150 owliso19150;
	private boolean finalized = false;
	private String path;

	private SortedSet<ClassInfo> classInfos = new TreeSet<ClassInfo>();
	private Resource defaultTypeImplementation;

	private TargetOwlConfiguration config;

	/**
	 * key: ontology name of property (so a URI); value: OwlProperty (a pair of
	 * Jena Property and PropertyInfo)
	 */
	private SortedMap<String, OwlProperty> properties = new TreeMap<String, OwlProperty>();

	/**
	 * Map to keep track of the RDF implementation of a ClassInfo. Can be a
	 * specific class or datatype but also the 'defaultTypeImplementation'
	 * (which defaults to OWL Class). This information is necessary whenever the
	 * RDF/OWL implementation of a ClassInfo needs to be known (especially when
	 * mapping a ClassInfo).
	 */
	protected SortedMap<ClassInfo, Resource> resourceByClassInfo = new TreeMap<ClassInfo, Resource>();

	/**
	 * Map to keep track of the ClassInfos that were converted to OWL classes.
	 */
	private SortedMap<ClassInfo, OntClass> ontClassByClassInfo = new TreeMap<ClassInfo, OntClass>();

	private SortedMap<PropertyInfo, OntProperty> ontPropertyByPropertyInfo = new TreeMap<PropertyInfo, OntProperty>();

	private SortedMap<PropertyInfo, Resource> rangeByPropertyInfo = new TreeMap<PropertyInfo, Resource>();

	private ConstraintMapping defaultConstraintMapping = new ConstraintMapping(
			null, "iso19150-2:constraint", "[[name]]: [[text]]", "", " ",
			ConstraintMapping.Format.STRING);

	private SortedSet<String> uniquePropertyNames = new TreeSet<String>();

	class OwlProperty {
		protected PropertyInfo pi;
		protected OntProperty p;

		public OwlProperty(PropertyInfo pi, OntProperty p) {
			this.pi = pi;
			this.p = p;
		}
	}

	private OntologyModel(Model m, Options o, ShapeChangeResult r,
			String xmlprefix, OWLISO19150 owliso19150) {

		this.model = m;
		this.options = o;
		this.result = r;
		this.prefix = xmlprefix;
		this.owliso19150 = owliso19150;
		this.config = this.owliso19150.getConfig();

		this.ontmodel.setNsPrefix("rdf", OWLISO19150.RDF_NS_W3C_RDF);
		this.ontmodel.setNsPrefix("rdfs", OWLISO19150.RDF_NS_W3C_RDFS);
		this.ontmodel.setNsPrefix("owl", OWLISO19150.RDF_NS_W3C_OWL);
		this.ontmodel.setNsPrefix("xsd", OWLISO19150.RDF_NS_W3C_XML_SCHEMA);

		if (owliso19150.getDefaultTypeImplementation() == null) {
			defaultTypeImplementation = OWL.Class;
		} else {
			defaultTypeImplementation = mapClass(
					owliso19150.getDefaultTypeImplementation());
		}
	}

	public OntologyModel(PackageInfo pi, Model m, Options o,
			ShapeChangeResult r, String xmlprefix, OWLISO19150 owliso19150)
			throws ShapeChangeAbortException {

		this(m, o, r, xmlprefix, owliso19150);

		this.mpackage = pi;

		String ontologyName = null;

		String uriBase;
		if (owliso19150.getUriBase() != null) {
			uriBase = owliso19150.getUriBase();
		} else {
			uriBase = mpackage.targetNamespace();
		}

		String path;

		if (mpackage.matches(
				OWLISO19150.RULE_OWL_PKG_ONTOLOGY_NAME_BY_TAGGED_VALUE)) {

			String ontologyNameTVName = owliso19150
					.getOntologyNameTaggedValue();

			ontologyName = mpackage.taggedValue(ontologyNameTVName);

			if (ontologyName == null) {
				result.addWarning(this, 26,
						OWLISO19150.RULE_OWL_PKG_ONTOLOGY_NAME_BY_TAGGED_VALUE,
						ontologyNameTVName, mpackage.fullNameInSchema());
			}
		}

		if (ontologyName == null) {

			if (mpackage.matches(
					OWLISO19150.RULE_OWL_PKG_SINGLE_ONTOLOGY_PER_SCHEMA)
					&& mpackage.matches(
							OWLISO19150.RULE_OWL_PKG_ONTOLOGY_NAME_CODE)) {

				String code = computeOntologyNameCode();

				path = "/" + code;

			} else if (mpackage.matches(
					OWLISO19150.RULE_OWL_PKG_ONTOLOGY_NAME_WITH_PATH)) {

				path = computePath(mpackage);

			} else {

				// default behavior - as defined by 19150-2package:ontologyName

				if (!mpackage.matches(
						OWLISO19150.RULE_OWL_PKG_ONTOLOGY_NAME_ISO191502)) {
					result.addWarning(this, 27, mpackage.fullNameInSchema());
				}

				path = "/" + normalizedName(mpackage);
			}

			ontologyName = uriBase + path;
		}

		if (mpackage.matches(
				OWLISO19150.RULE_OWL_PKG_ONTOLOGY_NAME_APPEND_VERSION)) {

			String version = mpackage.version();

			if (version != null && version.trim().length() > 0) {
				ontologyName = ontologyName + "/" + version.trim();
			}
		}
		this.name = ontologyName;

		if (mpackage
				.matches(OWLISO19150.RULE_OWL_PKG_SINGLE_ONTOLOGY_PER_SCHEMA)
				&& mpackage
						.matches(OWLISO19150.RULE_OWL_PKG_ONTOLOGY_NAME_CODE)) {
			this.fileName = computeOntologyNameCode();
			this.path = "";
		} else {
			this.fileName = normalizedName(pi);
			this.path = computePath(pi);
		}

		/*
		 * identify properties that have a unique name amongst the properties of
		 * classes that belong to the ontology
		 */
		SortedSet<ClassInfo> relevantClasses;
		if (mpackage
				.matches(OWLISO19150.RULE_OWL_PKG_SINGLE_ONTOLOGY_PER_SCHEMA)) {
			relevantClasses = model.classes(mpackage);
		} else {
			relevantClasses = mpackage.containedClasses();
		}
		SortedSet<String> encounteredPropertyNames = new TreeSet<String>();
		for (ClassInfo ci : relevantClasses) {
			for (PropertyInfo prop : ci.properties().values()) {
				if (encounteredPropertyNames.contains(prop.name())) {
					uniquePropertyNames.remove(prop.name());
				} else {
					encounteredPropertyNames.add(prop.name());
					uniquePropertyNames.add(prop.name());
				}
			}
		}

		/*
		 * As per ISO 19150-2package:rdfNamespace; '#' is the default for the
		 * separator - this can be changed via configuration parameter
		 * OWLISO19150#PARAM_RDF_NAMESPACE_SEPARATOR.
		 */
		this.rdfNamespace = name + owliso19150.getRdfNamespaceSeparator();

		this.ontmodel.setNsPrefix(prefix, rdfNamespace);

		ontology = ontmodel.createOntology(name);

		if (mpackage.matches(OWLISO19150.RULE_OWL_PKG_VERSION_INFO)
				&& mpackage.version() != null
				&& !mpackage.version().trim().isEmpty()) {
			ontology.addVersionInfo(mpackage.version());
		}

		if (mpackage.matches(OWLISO19150.RULE_OWL_PKG_VERSION_IRI)
				&& mpackage.version() != null
				&& !mpackage.version().trim().isEmpty()) {

			if (mpackage.matches(
					OWLISO19150.RULE_OWL_PKG_ONTOLOGY_NAME_APPEND_VERSION)
					&& mpackage.matches(
							OWLISO19150.RULE_OWL_PKG_VERSION_IRI_AVOID_DUPLICATE_VERSION)) {
				ontology.addProperty(OWL2.versionIRI, name);
			} else {
				ontology.addProperty(OWL2.versionIRI,
						name + "/" + mpackage.version());
			}
		}

		if (mpackage.matches(OWLISO19150.RULE_OWL_PKG_IMPORT_191502BASE)) {
			addImport(OWLISO19150.RDF_NS_ISO_19150_2,
					config.locationOfNamespace(OWLISO19150.RDF_NS_ISO_19150_2));
		}

		if (mpackage.matches(OWLISO19150.RULE_OWL_PKG_DCT_SOURCE_TITLE)) {
			ontmodel.setNsPrefix("dct", OWLISO19150.RDF_NS_DCT);
			ontology.addProperty(DCTerms.source,
					owliso19150.computeSource(mpackage),
					owliso19150.getLanguage());
		}

		applyDescriptorTargets(ontology, mpackage,
				DescriptorTarget.AppliesTo.ONTOLOGY);
	}

	/**
	 * This constructor is used to create an ontology model for codes
	 * (individuals, maybe also classes). Note that the namespace separator in
	 * this case is always '/'.
	 * 
	 * @param m
	 * @param o
	 * @param r
	 * @param prefix
	 * @param rdfns
	 * @param name
	 * @param path
	 * @param fileName
	 * @param owliso19150
	 * @throws ShapeChangeAbortException
	 */
	public OntologyModel(Model m, Options o, ShapeChangeResult r, String prefix,
			String rdfns, String name, String path, String fileName,
			OWLISO19150 owliso19150) throws ShapeChangeAbortException {

		this(m, o, r, prefix, owliso19150);

		this.name = name;
		this.path = path;
		this.fileName = fileName;

		this.rdfNamespace = rdfns;

		this.ontmodel.setNsPrefix(prefix, rdfNamespace);

		ontology = ontmodel.createOntology(name);
	}

	private String computeOntologyNameCode() {

		String onc = owliso19150.getOntologyNameCodeParameterValue();
		String xmlns = mpackage.xmlns();

		if (onc != null && onc.trim().length() > 0) {
			return onc;
		} else if (xmlns != null && xmlns.trim().length() > 0) {
			return mpackage.xmlns();
		} else {
			return "FIXME";
		}
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

	public void createClasses() {

		for (ClassInfo ci : this.classInfos) {

			// determine if a mapping exists for the class
			RdfTypeMapEntry rtme = config.getTypeMapEntry(ci);

			if (rtme != null) {
				/*
				 * Then this class shall be mapped - it shall not be encoded in
				 * the ontology.
				 */
				Resource r = mapClass(rtme.getTarget());
				this.resourceByClassInfo.put(ci, r);

			} else {

				int cat = ci.category();

				switch (cat) {
				case Options.FEATURE:
					if (ci.matches(
							OWLISO19150.RULE_OWL_CLS_ENCODE_FEATURETYPES)) {
						addClassDefinition(ci);
					}
					break;
				case Options.OBJECT:
					if (ci.matches(
							OWLISO19150.RULE_OWL_CLS_ENCODE_OBJECTTYPES)) {
						addClassDefinition(ci);
					}
					break;
				case Options.MIXIN:
					if (ci.matches(
							OWLISO19150.RULE_OWL_CLS_ENCODE_MIXINTYPES)) {
						addClassDefinition(ci);
					}
					break;
				case Options.BASICTYPE:
					if (ci.matches(
							OWLISO19150.RULE_OWL_CLS_ENCODE_BASICTYPES)) {
						addClassDefinition(ci);
					}
					break;
				case Options.DATATYPE:
					if (ci.matches(OWLISO19150.RULE_OWL_CLS_ENCODE_DATATYPES)) {
						addClassDefinition(ci);
					}
					break;
				case Options.UNION:
					if (ci.matches(OWLISO19150.RULE_OWL_CLS_UNION)) {
						addClassDefinition(ci);
					}
					break;
				case Options.ENUMERATION:
					if (!ci.matches(
							OWLISO19150.RULE_OWL_CLS_ENUMERATION_AS_CODELIST)
							&& !ci.matches(
									OWLISO19150.RULE_OWL_CLS_ISO191502_ENUMERATION)) {
						this.resourceByClassInfo.put(ci,
								defaultTypeImplementation);

						break;
					} else if (ci.matches(
							OWLISO19150.RULE_OWL_CLS_ISO191502_ENUMERATION)) {

						if (ci.matches(
								OWLISO19150.RULE_OWL_CLS_ENUMERATION_AS_CODELIST)) {
							// rules are mutually exclusive
							result.addWarning(this, 29,
									OWLISO19150.RULE_OWL_CLS_ENUMERATION_AS_CODELIST,
									OWLISO19150.RULE_OWL_CLS_ISO191502_ENUMERATION);
						}
						addEnumeration(ci);
						break;
					} else {
						/*
						 * Matches RULE_OWL_CLS_ENUMERATION_AS_CODELIST
						 * 
						 * just continue with the instructions from the next
						 * case (Options.CODELIST)
						 */
					}
				case Options.CODELIST:
					addCodelist(ci);
					break;
				default:
					this.resourceByClassInfo.put(ci, defaultTypeImplementation);
					MessageContext mc = result.addError(this, 5, "" + cat);
					if (mc != null) {
						mc.addDetail(this, 10000, ci.fullName());
					}
				}
			}
		}
	}

	public void createProperties() {

		for (ClassInfo ci : this.ontClassByClassInfo.keySet()) {

			int cat = ci.category();

			switch (cat) {
			case Options.FEATURE:
			case Options.OBJECT:
			case Options.MIXIN:
			case Options.DATATYPE:
			case Options.BASICTYPE:
			case Options.UNION:
				createNormalProperties(ci);
				break;
			case Options.CODELIST:
				/*
				 * code list properties are fully encoded when adding the code
				 * list class
				 */
				break;
			case Options.ENUMERATION:
				/*
				 * enumeration is either encoded as an RDFS datatype (which is
				 * fully covered when adding the enumeration class), treated as
				 * code list, or not encoded at all
				 */
				break;
			default:
				// already covered in createClasses() method
			}

		}
	}

	public void createNormalProperties(ClassInfo ci) {

		SortedMap<StructuredNumber, PropertyInfo> pis = ci.properties();

		for (PropertyInfo pi : pis.values()) {

			if (!pi.matches(OWLISO19150.RULE_OWL_PROP_GENERAL)) {
				// properties shall not be encoded
				break;
			}

			if (pi.isNavigable()) {

				OntProperty p = addPropertyDeclaration(pi);
				this.ontPropertyByPropertyInfo.put(pi, p);

				Resource range = computeRange(pi);
				this.rangeByPropertyInfo.put(pi, range);
			}
		}
	}

	private Resource computeRange(PropertyInfo pi) {

		Resource range = null;

		// the range may have already been computed for the property
		if (this.rangeByPropertyInfo.containsKey(pi)) {
			return this.rangeByPropertyInfo.get(pi);
		}

		/*
		 * identify the range to use for this property in class expressions
		 * (such as quantified cardinality restrictions and universal
		 * quantifications)
		 */
		RdfPropertyMapEntry rpme = config.getPropertyMapEntry(pi);

		if (rpme != null) {

			if (rpme.hasRange()) {
				range = mapClass(rpme.getRange());
			} else {
				/*
				 * so this property is mapped, but without a specific range
				 * being declared - that's fine
				 */
				return null;
			}

		} else {

			PropertyConversionParameter pcp = config
					.getPropertyConversionParameter(pi);

			if (pcp != null && pcp.hasTarget()) {

				// use range of global property to which this property is mapped

				PropertyInfo globalProp = getGlobalProperty(pcp);

				if (globalProp == null) {

					/*
					 * log warning that we could not find the global property in
					 * selected schemas
					 */
					MessageContext mc = result.addError(this, 36,
							pcp.getTarget(), pcp.getTargetSchema());
					if (mc != null) {
						mc.addDetail(this, 10001, pi.fullName());
					}

					range = mapType(pi);

				} else {

					range = mapType(globalProp);
				}

			} else {

				String propAbout = computePropertyName(pi);

				if (properties.containsKey(propAbout)) {
					/*
					 * a property with this id has already been declared - the
					 * given property may have been mapped to it
					 */
					OwlProperty p0 = properties.get(propAbout);

					/*
					 * compute the range from the PropertyInfo that belongs to
					 * the OWL property that has already been declared
					 */
					range = mapType(p0.pi);

				} else {

					// use type of the property
					range = mapType(pi);
				}
			}
		}

		if (range == null) {
			// use default range
			range = defaultTypeImplementation;
		}

		this.rangeByPropertyInfo.put(pi, range);

		return range;
	}

	/**
	 * @param pcp
	 * @return the PropertyInfo that matches the 'target' and 'targetSchema'
	 *         specification from the given PropertyConversionParameter, or
	 *         <code>null</code> if no such property could be found
	 */
	public PropertyInfo getGlobalProperty(PropertyConversionParameter pcp) {

		SortedSet<PackageInfo> schemas = model.schemas(pcp.getTargetSchema());

		if (schemas != null && schemas.size() == 1) {

			String[] qnameParts = pcp.getTarget().split("::");
			PackageInfo schema = schemas.first();
			SortedSet<ClassInfo> schemaCis = model.classes(schema);
			for (ClassInfo schemaCi : schemaCis) {
				if (schemaCi.name().equals(qnameParts[0])) {
					for (PropertyInfo prop : schemaCi.properties().values()) {
						if (prop.name().equals(qnameParts[1])) {
							return prop;
						}
					}
				}
			}
		}

		return null;
	}

	public void createAdditionalClassDetails() {

		for (ClassInfo ci : this.ontClassByClassInfo.keySet()) {

			OntClass c = this.ontClassByClassInfo.get(ci);

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

			if (ci.subtypes() != null && !ci.subtypes().isEmpty()
					&& ci.subtypes().size() > 1
					&& ci.matches(OWLISO19150.RULE_OWL_CLS_DISJOINT_CLASSES)) {

				SortedMap<String, Resource> subtypeResources = new TreeMap<String, Resource>();

				for (String ciId : ci.subtypes()) {

					ClassInfo subtype = model.classById(ciId);

					Resource mappedResource = map(subtype);

					if (mappedResource == null) {
						MessageContext mc = result.addError(this, 28,
								subtype.name(), ci.name());
						if (mc != null) {
							mc.addDetail(this, 10000, ci.fullName());
						}
					} else {

						subtypeResources.put(mappedResource.getURI(),
								mappedResource);
					}
				}

				if (subtypeResources.size() == 2) {

					Iterator<Resource> it = subtypeResources.values()
							.iterator();
					Resource r1 = it.next();
					Resource r2 = it.next();
					r1.addProperty(OWL2.disjointWith, r2);

				} else {

					OntClass adw = ontmodel.createClass();
					adw.setRDFType(OWL2.AllDisjointClasses);
					adw.addProperty(OWL2.members, ontmodel
							.createList(subtypeResources.values().iterator()));
				}
			}

			if (ci.category() != Options.ENUMERATION
					&& ci.category() != Options.CODELIST) {

				SortedMap<StructuredNumber, PropertyInfo> pis = ci.properties();

				for (PropertyInfo pi : pis.values()) {

					if (!pi.matches(OWLISO19150.RULE_OWL_PROP_GENERAL)) {
						// properties shall not be encoded
						break;
					}

					if (pi.isNavigable()) {

						OntProperty p = this.ontPropertyByPropertyInfo.get(pi);

						addMultiplicity(c, pi, p);

						addAllValuesFrom(c, pi, p);
					}
				}
			}

			addUnionSemantics(ci, c);
		}
	}

	/**
	 * Adds one or more class expressions to define union semantics for sets of
	 * properties of the class.
	 * <p>
	 * If the class is a 'union' then all properties belong to one set.
	 * Otherwise, all properties with the same value for the tag
	 * {@value OWLISO19150#UNION_SET_TAG_NAME} are members of a set. This can
	 * lead to multiple sets.
	 * 
	 * @param ci
	 */
	protected void addUnionSemantics(ClassInfo ci, OntClass c) {

		SortedMap<StructuredNumber, PropertyInfo> pis = ci.properties();

		SortedMap<String, SortedSet<PropertyInfo>> sets = new TreeMap<String, SortedSet<PropertyInfo>>();
		String unionSetId = "INTERNAL_UNION_SET_IDENTIFIER";

		for (PropertyInfo pi : pis.values()) {

			if (!pi.isNavigable()) {

				continue;

			} else if (!pi.matches(OWLISO19150.RULE_OWL_PROP_GENERAL)) {

				// properties shall not be encoded
				break;

			} else if (ci.category() == Options.UNION
					&& ci.matches(OWLISO19150.RULE_OWL_CLS_UNION)) {

				SortedSet<PropertyInfo> set;

				if (sets.containsKey(unionSetId)) {
					set = sets.get(unionSetId);
				} else {
					set = new TreeSet<PropertyInfo>();
					sets.put(unionSetId, set);
				}

				set.add(pi);

			} else {

				String tag = pi.taggedValue(OWLISO19150.TV_UNION_SET);

				if (tag != null && pi.inClass()
						.matches(OWLISO19150.RULE_OWL_CLS_UNION_SETS)) {
					SortedSet<PropertyInfo> set;

					if (sets.containsKey(tag)) {
						set = sets.get(tag);
					} else {
						set = new TreeSet<PropertyInfo>();
						sets.put(tag, set);
					}

					set.add(pi);
				}
			}
		}

		for (SortedSet<PropertyInfo> set : sets.values()) {

			if (set.size() == 1) {
				// no need to create a class expression
			} else {

				List<OntClass> intersections = new ArrayList<OntClass>();

				for (PropertyInfo pi_main : set) {

					List<OntClass> cardinalityRestrictions = new ArrayList<OntClass>();

					for (PropertyInfo pi_x : set) {

						OntProperty p = this.ontPropertyByPropertyInfo
								.get(pi_x);

						OntClass restriction;

						if (pi_x == pi_main) {

							int lower = pi_main.cardinality().minOccurs;
							int upper = pi_main.cardinality().maxOccurs;

							Resource range = this.rangeByPropertyInfo
									.get(pi_main);

							if (pi_main.matches(
									OWLISO19150.RULE_OWL_PROP_MULTIPLICITY_UNQUALIFIED_RESTRICTION)
									|| (range == null && pi_main.matches(
											OWLISO19150.RULE_OWL_PROP_MULTIPLICITY_QUALIFIED_RESTRICTION))) {

								if (lower == upper) {

									restriction = createCardinalityRestriction(
											p, lower);

								} else if (upper == Integer.MAX_VALUE) {

									restriction = createMinCardinalityRestriction(
											p, lower);

								} else if (lower == 0) {

									restriction = createMaxCardinalityRestriction(
											p, upper);

								} else {

									// lower >= 1 and 1<=upper<*
									// we need to represent this case in the
									// intersection as an intersection of
									// restrictions

									OntClass restriction_lower = createMinCardinalityRestriction(
											p, lower);
									OntClass restriction_upper = createMaxCardinalityRestriction(
											p, upper);

									List<OntClass> pi_main_restrictions = new ArrayList<OntClass>();
									pi_main_restrictions.add(restriction_lower);
									pi_main_restrictions.add(restriction_upper);

									restriction = this.ontmodel
											.createIntersectionClass(null,
													this.ontmodel.createList(
															pi_main_restrictions
																	.iterator()));
								}

							} else if (pi_main.matches(
									OWLISO19150.RULE_OWL_PROP_MULTIPLICITY_QUALIFIED_RESTRICTION)) {

								if (lower == upper) {

									restriction = createQCardinalityRestriction(
											p, lower, range);

								} else if (upper == Integer.MAX_VALUE) {

									restriction = createQMinCardinalityRestriction(
											p, lower, range);

								} else if (lower == 0) {

									restriction = createQMaxCardinalityRestriction(
											p, upper, range);

								} else {

									// lower >= 1 and 1<=upper<*
									// we need to represent this case in the
									// intersection as an intersection of
									// restrictions

									OntClass restriction_lower = createQMinCardinalityRestriction(
											p, lower, range);
									OntClass restriction_upper = createQMaxCardinalityRestriction(
											p, upper, range);

									List<OntClass> pi_main_restrictions = new ArrayList<OntClass>();
									pi_main_restrictions.add(restriction_lower);
									pi_main_restrictions.add(restriction_upper);

									restriction = this.ontmodel
											.createIntersectionClass(null,
													this.ontmodel.createList(
															pi_main_restrictions
																	.iterator()));
								}

							} else {

								/*
								 * multiplicity is otherwise not encoded; just
								 * create a simple minimum cardinality
								 * restriction to cover this property in the
								 * intersection of restrictions for the union
								 * properties
								 */
								restriction = createMinCardinalityRestriction(p,
										0);
							}

						} else {
							restriction = createCardinalityRestriction(p, 0);
						}

						cardinalityRestrictions.add(restriction);
					}

					OntClass intersectionRestriction = this.ontmodel
							.createIntersectionClass(null,
									this.ontmodel
											.createList(cardinalityRestrictions
													.iterator()));
					intersections.add(intersectionRestriction);
				}

				OntClass unionOfRestriction = this.ontmodel.createUnionClass(
						null,
						this.ontmodel.createList(intersections.iterator()));
				c.addSuperClass(unionOfRestriction);
			}
		}
	}

	public OntClass createQCardinalityRestriction(OntProperty p,
			int cardinality, Resource range) {
		/*
		 * NOTE: We cannot use the create(Min|Max)CardinalityQRestriction(...)
		 * methods from the Jena API, because they throw the following
		 * exception: org.apache.jena.ontology.ProfileException: Attempted to
		 * use language construct CARDINALITY_Q that is not supported in the
		 * current language profile: OWL Full
		 * 
		 * So, we use a workaround to create the qualified cardinality
		 * restriction. For further details, see
		 * http://stackoverflow.com/questions/20562107/how-to-add-qualified-
		 * cardinality-in-jena
		 */

		OntClass restriction = this.ontmodel.createCardinalityRestriction(null,
				p, cardinality);
		restriction.removeAll(OWL.cardinality);
		Literal cardAsNonNegativeInteger = ontmodel.createTypedLiteral(
				cardinality,
				"http://www.w3.org/2001/XMLSchema#nonNegativeInteger");
		restriction.addLiteral(OWL2.qualifiedCardinality,
				cardAsNonNegativeInteger);
		restriction.addProperty(OWL2.onClass, range);

		return restriction;
	}

	public OntClass createQMinCardinalityRestriction(OntProperty p,
			int cardinality, Resource range) {
		/*
		 * NOTE: We cannot use the create(Min|Max)CardinalityQRestriction(...)
		 * methods from the Jena API, because they throw the following
		 * exception: org.apache.jena.ontology.ProfileException: Attempted to
		 * use language construct CARDINALITY_Q that is not supported in the
		 * current language profile: OWL Full
		 * 
		 * So, we use a workaround to create the qualified cardinality
		 * restriction. For further details, see
		 * http://stackoverflow.com/questions/20562107/how-to-add-qualified-
		 * cardinality-in-jena
		 */

		OntClass restriction = this.ontmodel
				.createMinCardinalityRestriction(null, p, cardinality);
		restriction.removeAll(OWL.minCardinality);
		Literal cardAsNonNegativeInteger = ontmodel.createTypedLiteral(
				cardinality,
				"http://www.w3.org/2001/XMLSchema#nonNegativeInteger");
		restriction.addLiteral(OWL2.minQualifiedCardinality,
				cardAsNonNegativeInteger);
		restriction.addProperty(OWL2.onClass, range);

		return restriction;
	}

	public OntClass createQMaxCardinalityRestriction(OntProperty p,
			int cardinality, Resource range) {
		/*
		 * NOTE: We cannot use the create(Min|Max)CardinalityQRestriction(...)
		 * methods from the Jena API, because they throw the following
		 * exception: org.apache.jena.ontology.ProfileException: Attempted to
		 * use language construct CARDINALITY_Q that is not supported in the
		 * current language profile: OWL Full
		 * 
		 * So, we use a workaround to create the qualified cardinality
		 * restriction. For further details, see
		 * http://stackoverflow.com/questions/20562107/how-to-add-qualified-
		 * cardinality-in-jena
		 */

		OntClass restriction = this.ontmodel
				.createMaxCardinalityRestriction(null, p, cardinality);
		restriction.removeAll(OWL.maxCardinality);
		Literal cardAsNonNegativeInteger = ontmodel.createTypedLiteral(
				cardinality,
				"http://www.w3.org/2001/XMLSchema#nonNegativeInteger");
		restriction.addLiteral(OWL2.maxQualifiedCardinality,
				cardAsNonNegativeInteger);
		restriction.addProperty(OWL2.onClass, range);

		return restriction;
	}

	public OntClass createCardinalityRestriction(OntProperty p,
			int cardinality) {

		/*
		 * NOTE: the create(Min|Max)CardinalityRestriction(...) methods from
		 * Jena - or the turtle serialization - encodes the cardinality as an
		 * xsd:int, which should be an xsd:nonNegativeInteger according to
		 * https://www.w3.org/TR/owl2-mapping-to-rdf/
		 * 
		 * That's why we create and set the typed literal that represents the
		 * cardinality ourselves.
		 */

		OntClass restriction = this.ontmodel.createCardinalityRestriction(null,
				p, cardinality);
		restriction.removeAll(OWL.cardinality);
		Literal cardAsNonNegativeInteger = ontmodel.createTypedLiteral(
				cardinality,
				"http://www.w3.org/2001/XMLSchema#nonNegativeInteger");
		restriction.addLiteral(OWL.cardinality, cardAsNonNegativeInteger);
		return restriction;
	}

	public OntClass createMinCardinalityRestriction(OntProperty p,
			int cardinality) {

		/*
		 * NOTE: the create(Min|Max)CardinalityRestriction(...) methods from
		 * Jena - or the turtle serialization - encodes the cardinality as an
		 * xsd:int, which should be an xsd:nonNegativeInteger according to
		 * https://www.w3.org/TR/owl2-mapping-to-rdf/
		 * 
		 * That's why we create and set the typed literal that represents the
		 * cardinality ourselves.
		 */

		OntClass restriction = this.ontmodel
				.createMinCardinalityRestriction(null, p, cardinality);
		restriction.removeAll(OWL.minCardinality);
		Literal cardAsNonNegativeInteger = ontmodel.createTypedLiteral(
				cardinality,
				"http://www.w3.org/2001/XMLSchema#nonNegativeInteger");
		restriction.addLiteral(OWL.minCardinality, cardAsNonNegativeInteger);
		return restriction;
	}

	public OntClass createMaxCardinalityRestriction(OntProperty p,
			int cardinality) {

		/*
		 * NOTE: the create(Min|Max)CardinalityRestriction(...) methods from
		 * Jena - or the turtle serialization - encodes the cardinality as an
		 * xsd:int, which should be an xsd:nonNegativeInteger according to
		 * https://www.w3.org/TR/owl2-mapping-to-rdf/
		 * 
		 * That's why we create and set the typed literal that represents the
		 * cardinality ourselves.
		 */

		OntClass restriction = this.ontmodel
				.createMaxCardinalityRestriction(null, p, cardinality);
		restriction.removeAll(OWL.maxCardinality);
		Literal cardAsNonNegativeInteger = ontmodel.createTypedLiteral(
				cardinality,
				"http://www.w3.org/2001/XMLSchema#nonNegativeInteger");
		restriction.addLiteral(OWL.maxCardinality, cardAsNonNegativeInteger);
		return restriction;
	}

	public void createAdditionalPropertyDetails() {

		outer: for (ClassInfo ci : this.ontClassByClassInfo.keySet()) {

			SortedMap<StructuredNumber, PropertyInfo> pis = ci.properties();

			for (PropertyInfo pi : pis.values()) {

				if (!pi.matches(OWLISO19150.RULE_OWL_PROP_GENERAL)) {
					// properties shall not be encoded
					break outer;
				}

				if (pi.isNavigable()) {

					OntProperty p = this.ontPropertyByPropertyInfo.get(pi);

					AssociationInfo ai = pi.association();

					if (ai != null) {

						if (ai.assocClass() != null) {
							result.addError(this, 15, ai.assocClass().name());
						}

						PropertyInfo revPi = pi.reverseProperty();

						if (revPi != null && revPi.isNavigable() && pi
								.matches(OWLISO19150.RULE_OWL_PROP_INVERSEOF)) {

							OntologyModel om = owliso19150
									.computeRelevantOntology(revPi.inClass());

							if (om != null) {
								OntProperty ip;

								if (om == this) {
									ip = this.ontPropertyByPropertyInfo
											.get(revPi);
								} else {
									ip = om.getOntProperty(revPi);
								}

								if (ip != null)
									p.addInverseOf(ip);
								else {
									result.addError(this, 13, p.getURI());
								}
							} else {
								/*
								 * The schema to which revPi belongs is not
								 * selected for processing; we cannot create a
								 * property in the reference model to use as
								 * inverseOf because we do not know the correct
								 * ontology name of the reverse property.
								 */
							}
						}

						String aiName = ai.name();
						if (aiName != null && aiName.length() > 0 && pi.matches(
								OWLISO19150.RULE_OWL_PROP_ISO191502_ASSOCIATION_NAME)) {
							this.ontmodel.setNsPrefix(
									OWLISO19150.PREFIX_ISO_19150_2,
									OWLISO19150.RDF_NS_ISO_19150_2);
							p.addProperty(ISO19150_2.associationName, aiName);
						}

						if ((pi.isComposition() || pi.isAggregation())
								&& pi.matches(
										OWLISO19150.RULE_OWL_PROP_ISO191502_AGGREGATION)) {
							if (pi.isComposition()) {
								this.ontmodel.setNsPrefix(
										OWLISO19150.PREFIX_ISO_19150_2,
										OWLISO19150.RDF_NS_ISO_19150_2);
								p.addProperty(ISO19150_2.aggregationType,
										"partOfCompositeAggregation");
							} else if (pi.isAggregation()) {
								this.ontmodel.setNsPrefix(
										OWLISO19150.PREFIX_ISO_19150_2,
										OWLISO19150.RDF_NS_ISO_19150_2);
								p.addProperty(ISO19150_2.aggregationType,
										"partOfSharedAggregation");
							} else {
								// no special aggregation to document
							}
						}
					}
				}
			}
		}
	}

	public void finalizeDocument() {

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

	public OntClass addClassDefinition(ClassInfo ci) {

		if (finalized) {
			this.result.addWarning(this, 3, ci.name());
			return null;
		}

		// create the Class <OWL>
		OntClass c = ontmodel
				.createClass(computeReference(getPrefix(), normalizedName(ci)));

		this.ontClassByClassInfo.put(ci, c);
		this.resourceByClassInfo.put(ci, c);

		applyDescriptorTargets(c, ci, DescriptorTarget.AppliesTo.CLASS);

		if (ci.isAbstract()
				&& ci.matches(OWLISO19150.RULE_OWL_CLS_19150_2_ISABSTRACT)) {
			this.ontmodel.setNsPrefix(OWLISO19150.PREFIX_ISO_19150_2,
					OWLISO19150.RDF_NS_ISO_19150_2);
			c.addLiteral(ISO19150_2.isAbstract,
					ontmodel.createTypedLiteral(true));
		}

		addCustomSubClassOf(c, ci);
		addConstraintDeclarations(c, ci);

		// NOTE: properties are created once all classes have been created

		return c;
	}

	private void mapAndAddSuperClass(ClassInfo ci, OntClass c,
			ClassInfo supertype) {

		Resource mappedResource = map(supertype);

		if (mappedResource == null) {
			MessageContext mc = result.addError(this, 6, supertype.name(),
					ci.name());
			if (mc != null) {
				mc.addDetail(this, 10000, ci.fullName());
			}

		} else {
			c.addSuperClass(mappedResource);
		}
	}

	private void applyDescriptorTargets(OntResource c, Info i,
			DescriptorTarget.AppliesTo appliesTo) {

		for (DescriptorTarget dt : this.config.getDescriptorTargets()) {

			if (!dt.getAppliesTo().equals(DescriptorTarget.AppliesTo.ALL)
					&& !appliesTo.equals(DescriptorTarget.AppliesTo.ALL)
					&& (((i instanceof PackageInfo) && !dt.getAppliesTo()
							.equals(DescriptorTarget.AppliesTo.ONTOLOGY))
							|| ((i instanceof ClassInfo) && !((dt.getAppliesTo()
									.equals(DescriptorTarget.AppliesTo.CLASS)
									&& appliesTo
											.equals(DescriptorTarget.AppliesTo.CLASS))
									|| (dt.getAppliesTo()
											.equals(DescriptorTarget.AppliesTo.CONCEPT_SCHEME)
											&& appliesTo
													.equals(DescriptorTarget.AppliesTo.CONCEPT_SCHEME))))
							|| ((i instanceof PropertyInfo)
									&& !dt.getAppliesTo().equals(
											DescriptorTarget.AppliesTo.PROPERTY)))) {
				// Descriptor target does not apply to the Info object
				continue;
			}

			String template = dt.getTemplate();

			String doc = template;

			Pattern pattern = Pattern.compile("\\[\\[([^\\[].*?)\\]\\]");
			// TV(\(.+?\))?:(.+) -> "TV(\\(.+?\\))?:(.+)"
			Matcher matcher = pattern.matcher(template);

			List<StringBuilder> builders = new ArrayList<StringBuilder>();
			builders.add(new StringBuilder());

			boolean noValuesForFieldsInTemplate = true;

			int index = 0;
			while (matcher.find()) {

				String desc = matcher.group(1).trim();

				/*
				 * identify the descriptor or tagged value from the field and
				 * get value(s)
				 */
				List<String> values = new ArrayList<String>();
				boolean descRecognized = true;

				if (desc.startsWith("TV")) {

					Matcher m = descriptorTargetTaggedValuePattern
							.matcher(desc);

					/*
					 * validation of the TargetOwlConfiguration already ensured
					 * that desc matches
					 */
					m.matches();
					String separator = m.group(1);
					String tv = m.group(2);

					String[] tv_values = i.taggedValuesInLanguage(tv,
							options.language());

					if (separator != null) {
						// exclude leading "(" and trailing ")"
						separator = separator.substring(1,
								separator.length() - 1);
						// match the string separator as if it were a literal
						// pattern
						String quoted_separator = Pattern.quote(separator);

						for (String tv_value : tv_values) {
							String[] split = tv_value.split(quoted_separator);
							for (String s : split) {
								if (!s.trim().isEmpty()) {
									values.add(s.trim());
								}
							}
						}
					} else {
						for (String tv_value : tv_values) {
							if (!tv_value.trim().isEmpty()) {
								values.add(tv_value.trim());
							}
						}
					}

				} else if (desc.equalsIgnoreCase("name")) {

					values.add(i.name());

				} else if (desc.equalsIgnoreCase("alias")) {

					String s = i.aliasName();
					if (s != null && !s.trim().isEmpty()) {
						values.add(s);
					}

				} else if (desc.equalsIgnoreCase("definition")) {

					String s = i.definition();
					if (s != null && !s.trim().isEmpty()) {
						values.add(s);
					}

				} else if (desc.equalsIgnoreCase("description")) {

					String s = i.description();
					if (s != null && !s.trim().isEmpty()) {
						values.add(s);
					}

				} else if (desc.equalsIgnoreCase("example")) {

					String[] s = i.examples();
					if (s != null && s.length > 0) {
						for (String ex : s) {
							if (ex.trim().length() > 0) {
								values.add(ex.trim());
							}
						}
					}

				} else if (desc.equalsIgnoreCase("legalBasis")) {

					String s = i.legalBasis();
					if (s != null && !s.trim().isEmpty()) {
						values.add(s);
					}

				} else if (desc.equalsIgnoreCase("dataCaptureStatement")) {

					String[] s = i.dataCaptureStatements();
					if (s != null && s.length > 0) {
						for (String ex : s) {
							if (ex.trim().length() > 0) {
								values.add(ex.trim());
							}
						}
					}

				} else if (desc.equalsIgnoreCase("primaryCode")) {

					String s = i.primaryCode();
					if (s != null && !s.trim().isEmpty()) {
						values.add(s);
					}

				} else {
					/*
					 * the field in the template neither identifies a known
					 * descriptor nor a tagged value
					 */
					descRecognized = false;
				}

				// append the text from the template up until the current find
				for (StringBuilder b : builders) {
					b.append(doc.substring(index, matcher.start()));
				}

				if (descRecognized) {

					if (values.isEmpty()) {
						values.add(dt.getNoValueText());
					} else {
						noValuesForFieldsInTemplate = false;
					}

					if (values.size() == 1) {

						for (StringBuilder b : builders) {
							b.append(values.get(0));
						}

					} else {

						if (dt.getMultiValueBehavior() == DescriptorTarget.MultiValueBehavior.CONNECT_IN_SINGLE_TARGET) {

							String connectedValues = StringUtils.join(values,
									dt.getMultiValueConnectorToken());

							for (StringBuilder b : builders) {
								b.append(connectedValues);
							}

						} else {

							// we shall split to multiple targets
							List<StringBuilder> newBuilders = new ArrayList<StringBuilder>();

							for (String val : values) {
								for (StringBuilder b : builders) {
									StringBuilder newBuilder = new StringBuilder(
											b);
									newBuilder.append(val);
									newBuilders.add(newBuilder);
								}
							}

							builders = newBuilders;
						}
					}

				} else {
					// template field not recognized - put it back in
					for (StringBuilder b : builders) {
						b.append(matcher.group(0));
					}
				}

				index = matcher.end();
			}

			// append any remaining text from the template
			for (StringBuilder b : builders) {
				b.append(doc.substring(index, doc.length()));
			}

			if (noValuesForFieldsInTemplate && dt
					.getNoValueBehavior() == DescriptorTarget.NoValueBehavior.INGORE) {
				// we don't create any property from this descriptor target

			} else {

				String target = dt.getTarget();
				addNamespaceDeclaration(target);

				String propertyIRI = computeReference(target);

				Property prop = ontmodel.createProperty(propertyIRI);

				for (StringBuilder sb : builders) {

					if (dt.getFormat() == DescriptorTarget.Format.STRING) {
						c.addProperty(prop, sb.toString());
					} else if (dt
							.getFormat() == DescriptorTarget.Format.LANG_STRING) {
						c.addProperty(prop, sb.toString(),
								owliso19150.getLanguage());
					} else {
						// format is IRI
						Resource r = ontmodel.createResource(sb.toString());
						c.addProperty(prop, r);
					}
				}
			}
		}
	}

	private void addCustomSubClassOf(OntResource c, ClassInfo ci) {

		// apply stereotype conversion parameters

		int cat = ci.category();

		String catID;

		switch (cat) {
		case Options.FEATURE:
			catID = "featuretype";
			break;
		case Options.MIXIN:
		case Options.OBJECT:
			catID = "type";
			break;
		case Options.BASICTYPE:
			catID = "basictype";
			break;
		case Options.DATATYPE:
			catID = "datatype";
			break;
		case Options.CODELIST:
			catID = "codelist";
			break;
		case Options.UNION:
			catID = "union";
			break;
		case Options.ENUMERATION:
			catID = "enumeration";
			break;

		default:
			result.addWarning(this, 9, ci.name(), ci.stereotypes().toString());
			return;
		}

		SortedMap<String, List<StereotypeConversionParameter>> stereotypeMappings = owliso19150
				.getConfig().getStereotypeConversionParameters();

		if (stereotypeMappings.containsKey(catID)) {

			List<StereotypeConversionParameter> scps = stereotypeMappings
					.get(catID);

			for (StereotypeConversionParameter scp : scps) {
				for (String subClassOf : scp.getSubClassOf()) {
					Resource mappedResource = mapResource(subClassOf);
					c.asClass().addSuperClass(mappedResource);
				}
			}
		}

		// apply type conversion parameters

		TypeConversionParameter tcp = owliso19150.getConfig()
				.getTypeConversionParameter(ci);

		if (tcp != null) {
			for (String subClassOf : tcp.getSubClassOf()) {
				Resource mappedResource = mapResource(subClassOf);
				c.asClass().addSuperClass(mappedResource);
			}
		}
	}

	public void addNamespaceDeclaration(String qname) {

		if (qname == null || !qname.contains(":")
				|| qname.substring(0, qname.indexOf(":")).length() == 0) {
			return;
		}

		String prefix = qname.substring(0, qname.indexOf(":"));

		if (config.hasNamespaceWithAbbreviation(prefix)) {

			String ns = config.fullNamespace(prefix);
			// add namespace declaration
			String s = ontmodel.getNsPrefixURI(prefix);
			if (s == null)
				ontmodel.setNsPrefix(prefix, ns);
			else if (!s.equals(ns))
				result.addError(this, 11, getName(), prefix, ns, s);

		} else {

			result.addError(this, 39, prefix);
		}
	}

	/**
	 * @param rdfns
	 *            full namespace of the ontology to import
	 * @param uri
	 *            location of the ontology to import, can be <code>null</code>
	 *            to indicate that the location is unknown (in that case, an
	 *            import is not created)
	 */
	public void addImport(String rdfns, String uri) {

		if (rdfns == null || rdfns.equals(this.rdfNamespace)) {
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

	protected void addMultiplicity(OntClass cls, PropertyInfo pi,
			OntProperty p) {

		if ((pi.inClass().category() == Options.UNION
				&& pi.inClass().matches(OWLISO19150.RULE_OWL_CLS_UNION))
				|| (pi.taggedValue(OWLISO19150.TV_UNION_SET) != null
						&& pi.inClass().matches(
								OWLISO19150.RULE_OWL_CLS_UNION_SETS))) {
			/*
			 * A class expression to specify union semantics for this property
			 * will be created. The expression specifies the multiplicity, so do
			 * not encode it here.
			 */
			return;
		}

		Multiplicity m = pi.cardinality();
		int lower = m.minOccurs;
		int upper = m.maxOccurs;

		if (pi.voidable() && pi.matches(
				OWLISO19150.RULE_OWL_PROP_VOIDABLE_AS_MINCARDINALITY0)) {
			lower = 0;
		}

		Resource range = this.rangeByPropertyInfo.get(pi);

		if (pi.matches(
				OWLISO19150.RULE_OWL_PROP_MULTIPLICITY_UNQUALIFIED_RESTRICTION)
				|| (range == null && pi.matches(
						OWLISO19150.RULE_OWL_PROP_MULTIPLICITY_QUALIFIED_RESTRICTION))) {

			boolean restrictionCreated = false;

			if (lower == upper) {

				OntClass restriction = createCardinalityRestriction(p, lower);

				cls.addSuperClass(restriction);
				restrictionCreated = true;

			} else {

				// set min cardinality if required
				if (lower == 0) {

					// simply omit min cardinality to represent this case

				} else {

					OntClass restriction = createMinCardinalityRestriction(p,
							lower);

					cls.addSuperClass(restriction);
					restrictionCreated = true;
				}

				// set max cardinality if required
				if (upper == Integer.MAX_VALUE) {

					// simply omit max cardinality to represent this case

				} else {

					OntClass restriction = createMaxCardinalityRestriction(p,
							upper);

					cls.addSuperClass(restriction);
					restrictionCreated = true;
				}
			}

			if (restrictionCreated && range == null && pi.matches(
					OWLISO19150.RULE_OWL_PROP_MULTIPLICITY_QUALIFIED_RESTRICTION)) {
				MessageContext mc = result.addInfo(this, 33);
				if (mc != null) {
					mc.addDetail(this, 10001, pi.fullName());
				}
			}

		} else if (pi.matches(
				OWLISO19150.RULE_OWL_PROP_MULTIPLICITY_QUALIFIED_RESTRICTION)) {

			if (lower == upper) {

				OntClass restriction = createQCardinalityRestriction(p, lower,
						range);
				cls.addSuperClass(restriction);

			} else {

				// set min cardinality if required
				if (lower == 0) {

					// simply omit min cardinality to represent this case

				} else {

					OntClass restriction = createQMinCardinalityRestriction(p,
							lower, range);
					cls.addSuperClass(restriction);
				}

				// set max cardinality if required
				if (upper == Integer.MAX_VALUE) {

					// simply omit max cardinality to represent this case

				} else {

					OntClass restriction = createQMaxCardinalityRestriction(p,
							upper, range);
					cls.addSuperClass(restriction);
				}
			}
		}
	}

	/**
	 * @param cls
	 *            class for which a all-values-from restriction is created
	 * @param pi
	 *            property
	 * @param p
	 *            ontology representation of the property
	 */
	protected void addAllValuesFrom(OntClass cls, PropertyInfo pi, Property p) {

		if (pi.matches(
				OWLISO19150.RULE_OWL_PROP_RANGE_LOCAL_UNIVERSAL_QUANTIFICATION)) {

			Resource range = this.rangeByPropertyInfo.get(pi);

			if (range != null) {

				OntClass restriction = ontmodel
						.createAllValuesFromRestriction(null, p, range);
				cls.addSuperClass(restriction);

			} else {
				/*
				 * The restriction is not created because the range is unknown
				 */
				MessageContext mc = result.addWarning(this, 35, pi.name());
				if (mc != null) {
					mc.addDetail(this, 10001, pi.fullName());
				}
			}
		}
	}

	private boolean isDatatypeProperty(PropertyInfo pi) {

		RdfTypeMapEntry rtme = config.getTypeMapEntryByTypeInfo(pi);

		if (rtme == null) {
			int cat = pi.categoryOfValue();

			if (cat == Options.ENUMERATION) {
				ClassInfo enumeration = model.classById(pi.typeInfo().id);
				if (enumeration == null
						|| this.ontClassByClassInfo.containsKey(enumeration)) {
					return false;
				} else {
					return true;
				}
			} else {
				return false;
			}
		} else {
			return rtme.getTargetType() == RdfTypeMapEntry.TargetType.DATATYPE;
		}
	}

	private OntProperty addPropertyDeclaration(PropertyInfo pi) {

		if (!pi.isNavigable()) {
			return null;
		}

		OntProperty mappedProperty = mapProperty(pi);

		if (mappedProperty != null) {
			if (mappedProperty.getURI()
					.equals(computeReference("sc", "null"))) {
				MessageContext mc = result.addInfo(this, 20, pi.name());
				if (mc != null)
					mc.addDetail(this, 10001, pi.fullName());
				return null;
			} else {
				MessageContext mc = result.addDebug(this, 21, pi.name(),
						mappedProperty.getURI());
				if (mc != null) {
					mc.addDetail(this, 10001, pi.fullName());
				}
				return mappedProperty;
			}
		} else if (pi.isAttribute() && pi
				.matches(OWLISO19150.RULE_OWL_PROP_GLOBAL_SCOPE_ATTRIBUTES)) {
			/*
			 * fine - we only check this here because
			 * rule-owl-prop-globalScopeAttributes has higher priority than
			 * rule-owl-prop-globalScopeByConversionParameter; pi will be
			 * handled later on
			 */
		} else if (pi.matches(
				OWLISO19150.RULE_OWL_PROP_GLOBAL_SCOPE_BY_CONVERSION_PARAMETER)) {

			PropertyConversionParameter pcp = config
					.getPropertyConversionParameter(pi);

			if (pcp != null && pcp.hasTarget()) {
				/*
				 * The property shall be mapped to a global one. Identify the
				 * relevant ontology and create a reference.
				 */
				OntologyModel ontologyWithGlobalProp = owliso19150
						.computeRelevantOntologyForTargetMapping(pcp);

				if (ontologyWithGlobalProp == null) {

					MessageContext mc = result.addError(this, 31,
							pcp.getTarget(), pcp.getTargetSchema());
					if (mc != null) {
						mc.addDetail(this, 10001, pi.fullName());
					}

				} else {

					addImport(ontologyWithGlobalProp.getRdfNamespace(), null);

					String propAbout = computeReference(
							ontologyWithGlobalProp.getPrefix(),
							pcp.getTarget().split("::")[1]);
					OntModel ontModel = ontologyWithGlobalProp
							.getOntologyModel();
					OntProperty p = ontModel.getOntProperty(propAbout);

					if (p == null)
						p = ontModel.createOntProperty(propAbout);

					MessageContext mc = result.addInfo(this, 34, pi.name(),
							p.getURI());
					if (mc != null) {
						mc.addDetail(this, 10001, pi.fullName());
					}

					if (pi.matches(
							OWLISO19150.RULE_OWL_PROP_MAPPING_COMPARE_SPECIFICATIONS)) {

						PropertyInfo globalProp = getGlobalProperty(pcp);

						if (globalProp != null) {
							comparePropertySpecifications(pi, globalProp);
						} else {
							MessageContext mc2 = result.addError(this, 25);
							if (mc2 != null) {
								mc2.addDetail(this, 10003, pi.fullName());
								mc2.addDetail(this, 10004, pi.fullName());
							}
						}
					}

					return p;
				}
			}
		}

		String propAbout = computePropertyName(pi);

		if (properties.containsKey(propAbout)) {
			/*
			 * a property with this id has already been declared - do not add it
			 * again
			 */
			OwlProperty p0 = properties.get(propAbout);

			if (pi.matches(
					OWLISO19150.RULE_OWL_PROP_MAPPING_COMPARE_SPECIFICATIONS)) {
				comparePropertySpecifications(pi, p0.pi);
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

		applyDescriptorTargets(p, pi, DescriptorTarget.AppliesTo.PROPERTY);
		addConstraintDeclarations(p, pi);
		addCustomSubPropertyOf(p, pi);

		// domain should be declared only for a locally scoped property
		if (!isGlobalProperty(pi)) {
			p.addDomain(ontmodel.createResource(computeReference(getPrefix(),
					normalizedName(pi.inClass()))));
		}

		// if range shall be globally defined, add it now
		Resource range = computeRange(pi);
		if (pi.matches(OWLISO19150.RULE_OWL_PROP_RANGE_GLOBAL)
				&& range != null) {
			p.addRange(range);
		}

		// remember property
		properties.put(propAbout, new OwlProperty(pi, p));

		return p;
	}

	private String computePropertyName(PropertyInfo pi) {

		boolean isGlobalProperty = isGlobalProperty(pi);

		String result;
		if (isGlobalProperty) {
			result = computeReference(getPrefix(), normalizedName(pi));
		} else {
			result = computeReference(getPrefix(),
					normalizedName(pi.inClass()) + "." + normalizedName(pi));
		}

		return result;
	}

	/**
	 * @param pi1
	 *            property 1 (that is usually mapped to or implemented by pi2)
	 * @param propAbout
	 *            ontology name of pi1
	 * @param pi2
	 *            property 2
	 */
	private void comparePropertySpecifications(PropertyInfo pi1,
			PropertyInfo pi2) {

		if (isDatatypeProperty(pi1) != isDatatypeProperty(pi2)) {

			MessageContext mc = result.addWarning(this, 23, pi1.typeInfo().name,
					pi2.typeInfo().name);
			if (mc != null) {
				mc.addDetail(this, 10003, pi1.fullName());
				mc.addDetail(this, 10004, pi2.fullName());
			}

		} else if (!pi1.typeInfo().name.equalsIgnoreCase(pi2.typeInfo().name)) {

			MessageContext mc = result.addWarning(this, 16, pi1.typeInfo().name,
					pi2.typeInfo().name);
			if (mc != null) {
				mc.addDetail(this, 10003, pi1.fullName());
				mc.addDetail(this, 10004, pi2.fullName());
			}
		}

		String s1 = pi1.definition();
		if (s1 == null)
			s1 = "";
		String s2 = pi2.definition();
		if (s2 == null)
			s2 = "";
		if (!s1.equalsIgnoreCase(s2)) {
			MessageContext mc = result.addWarning(this, 17, s1, s2);
			if (mc != null) {
				mc.addDetail(this, 10003, pi1.fullName());
				mc.addDetail(this, 10004, pi2.fullName());
			}
		}

		s1 = pi1.description();
		if (s1 == null)
			s1 = "";
		s2 = pi2.description();
		if (s2 == null)
			s2 = "";
		if (!s1.equalsIgnoreCase(s2)) {
			MessageContext mc = result.addWarning(this, 18, s1, s2);
			if (mc != null) {
				mc.addDetail(this, 10003, pi1.fullName());
				mc.addDetail(this, 10004, pi2.fullName());
			}
		}

		s1 = pi1.aliasName();
		if (s1 == null)
			s1 = "";
		s2 = pi2.aliasName();
		if (s2 == null)
			s2 = "";
		if (!s1.equalsIgnoreCase(s2)) {
			MessageContext mc = result.addWarning(this, 19, s1, s2);
			if (mc != null) {
				mc.addDetail(this, 10003, pi1.fullName());
				mc.addDetail(this, 10004, pi2.fullName());
			}
		}
	}

	private void addCustomSubPropertyOf(OntProperty p, PropertyInfo pi) {

		PropertyConversionParameter pcp = config
				.getPropertyConversionParameter(pi);
		if (pcp != null && pcp.hasSubPropertyOf()) {

			for (String spo : pcp.getSubPropertyOf()) {

				Property mapping = mapProperty(spo);
				p.asProperty().addSuperProperty(mapping);
			}
		}
	}

	private boolean isGlobalProperty(PropertyInfo pi) {

		if (pi.matches(OWLISO19150.RULE_OWL_PROP_LOCAL_SCOPE_ALL)) {
			return false;
		}

		if (pi.isAttribute() && pi
				.matches(OWLISO19150.RULE_OWL_PROP_GLOBAL_SCOPE_ATTRIBUTES)) {
			return true;
		}

		if (pi.matches(
				OWLISO19150.RULE_OWL_PROP_GLOBAL_SCOPE_BY_UNIQUE_PROPERTY_NAME)
				&& this.uniquePropertyNames.contains(pi.name())) {
			return true;
		}

		if (pi.matches(
				OWLISO19150.RULE_OWL_PROP_GLOBAL_SCOPE_BY_CONVERSION_PARAMETER)) {

			PropertyConversionParameter pcp = config
					.getPropertyConversionParameter(pi);
			if (pcp != null && pcp.isGlobal() && !pcp.hasTarget()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Retrieves a resource identified by the given QName from the reference
	 * model. The resource is created if necessary. Also creates an import for
	 * the resource.
	 * 
	 * @param qname
	 *            identifies a resource
	 * @return resource identified by the qname
	 */
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

	/**
	 * Creates an ontology class (in the internal {@link #refmodel}), with
	 * namespace identified by looking up the prefix of the QName in the
	 * configuration of namespaces. Also creates an import of that namespace.
	 * 
	 * @param qname
	 *            identifies a class
	 * @return ontology class identified by the qname
	 */
	private Resource mapClass(String qname) {

		String[] qnamePars = qname.split(":");
		String prefix = qnamePars[0];
		String resourceName = qnamePars[1];

		// identify rdf namespace based upon prefix and standard namespaces
		String rdfNs = config.fullNamespace(prefix);
		String location = config.locationOfNamespace(rdfNs);

		String uri = rdfNs + resourceName;

		OntClass c = refmodel.getOntClass(uri);
		if (c == null)
			c = refmodel.createClass(uri);

		// also add import for the namespace
		addImport(rdfNs, location);

		// return correct element definition
		return c;
	}

	/**
	 * Creates an ontology property (in the internal {@link #refmodel} - unless
	 * it already exists in the refmodel), with namespace identified by looking
	 * up the prefix of the QName in the configuration of namespaces. Also
	 * creates an import of that namespace.
	 * 
	 * @param qname
	 *            identifies a property
	 * @return property identified by the qname
	 */
	private Property mapProperty(String qname) {

		String[] qnamePars = qname.split(":");
		String prefix = qnamePars[0];
		String propertyName = qnamePars[1];

		// identify rdf namespace based upon prefix and standard namespaces
		String rdfNs = config.fullNamespace(prefix);
		String location = config.locationOfNamespace(rdfNs);

		String uri = rdfNs + propertyName;
		Property p = refmodel.getProperty(uri);
		if (p == null)
			p = refmodel.createProperty(uri);

		// also add import for the namespace
		addImport(rdfNs, location);

		// return correct element definition
		return p;
	}

	/**
	 * Maps the given class to the RDF resource that represents it. If an
	 * RdfTypeMapEntry exists for the class, it is used. Otherwise, the RDF
	 * implementation of the class is looked up in one of the ontology models
	 * that were created from the schemas selected for processing. If no map
	 * entry exists and the class is not part of the schemas selected for
	 * processing, an error will be logged and <code>null</code> returned.
	 * 
	 * @param ci
	 * @param processMapEntry
	 * @return the resource that implements the class, or <code>null</code>
	 *         (then no RdfTypeMapEntry is defined for the class and the class
	 *         is not part of the schemas selected for processing)
	 */
	protected Resource map(ClassInfo ci) {

		RdfTypeMapEntry rtme = config.getTypeMapEntry(ci);

		if (rtme != null) {
			Resource r = mapClass(rtme.getTarget());
			MessageContext mc = result.addDebug(this, 22, ci.name(),
					r.getURI());
			if (mc != null) {
				mc.addDetail(this, 10000, ci.fullName());
			}
			return r;
		}

		// lookup the ontology to which the class belongs
		OntologyModel om = owliso19150.computeRelevantOntology(ci);

		if (om == null) {

			/*
			 * No RdfMapEntry is defined for the type and the type is not
			 * contained in the schemas selected for processing
			 */
			MessageContext mc = this.result.addError(this, 14, ci.name());
			if (mc != null) {
				mc.addDetail(this, 10000, ci.fullNameInSchema());
			}
			return null;
		}

		String rdfNs = om.getRdfNamespace();
		String location = om.getName();

		// add import for the namespace and declare prefix
		addImport(rdfNs, location);

		return om.getResource(ci);
	}

	/**
	 * Returns a class representing the type of the property.
	 * RdfPropertyMapEntries are not considered.
	 * 
	 * @param pi
	 * @return
	 */
	protected Resource mapType(PropertyInfo pi) {

		RdfTypeMapEntry rtme = config.getTypeMapEntryByTypeInfo(pi);

		if (rtme != null) {
			return mapClass(rtme.getTarget());
		}

		// no mapping available - use actual type
		Type ti = pi.typeInfo();

		ClassInfo ci = model.classById(ti.id);

		if (ci == null) {
			// in case that the model references the type by name only
			ci = model.classByName(ti.name);
		}

		if (ci == null) {
			MessageContext mc = this.result.addError(this, 37, ti.name);
			if (mc != null) {
				mc.addDetail(this, 10001, pi.fullName());
			}
			return null;
		}

		Resource r = map(ci);

		if (r == null) {
			MessageContext mc = this.result.addError(this, 7, ti.name);
			if (mc != null) {
				mc.addDetail(this, 10001, pi.fullName());
			}
			return null;
		}

		return r;
	}

	/**
	 * NOTE: also imports the namespace of a mapped property
	 * 
	 * @param pi
	 * @return The RDF/OWL property implementation to which the given
	 *         PropertyInfo is mapped, or <code>null</code> if there is no
	 *         mapping. The mapping can be defined via an RdfPropertyMapEntry or
	 *         through a PropertyConversionParameter (a global property to which
	 *         the given property is mapped will automatically be created in the
	 *         correct OntologyModel if it does not already exist there).
	 */
	protected OntProperty mapProperty(PropertyInfo pi) {

		if (pi == null)
			return null;

		RdfPropertyMapEntry pme = config.getPropertyMapEntry(pi);

		if (pme != null) {
			// The property shall be mapped via RdfPropertyMapEntry

			// get QName
			String qname;

			if (pme.hasTarget()) {
				qname = pme.getTarget();
			} else {
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
			if (!qname.equalsIgnoreCase("sc:null")) {
				addImport(rdfNs, location);
			}

			// return property, create if needed
			String propAbout = computeReference(prefix, refName);
			OntProperty p = refmodel.getOntProperty(propAbout);
			if (p == null)
				p = refmodel.createOntProperty(propAbout);
			return p;
		}

		// the property is not mapped
		return null;
	}

	public static String normalizedName(PropertyInfo pi) {
		return normalizedPropertyName(pi.name());
	}

	public static String normalizedPropertyName(String name) {

		// NOTE: using RULE_OWL_PROP_ISO191502_NAMING behavior as default

		// ISO 19150-2owl:propertyName (part 2)
		// =================================

		String result = name;

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

	private void addConstraintDeclarations(OntResource r, Info i) {

		List<Constraint> cons;

		if (i instanceof ClassInfo) {
			cons = ((ClassInfo) i).constraints();
		} else if (i instanceof PropertyInfo) {
			cons = ((PropertyInfo) i).constraints();
		} else {
			return;
		}

		if (cons.isEmpty())
			return;

		if (i.matches(
				OWLISO19150.RULE_OWL_ALL_CONSTRAINTS_HUMAN_READABLE_TEXT_ONLY)) {

			if (i.matches(
					OWLISO19150.RULE_OWL_ALL_CONSTRAINTS_BY_CONSTRAINT_MAPPING)) {
				result.addWarning(this, 29,
						OWLISO19150.RULE_OWL_ALL_CONSTRAINTS_BY_CONSTRAINT_MAPPING,
						OWLISO19150.RULE_OWL_ALL_CONSTRAINTS_HUMAN_READABLE_TEXT_ONLY);
			}

			for (Constraint c : cons) {

				String text;

				if (c instanceof OclConstraint) {
					OclConstraint oclCon = (OclConstraint) c;
					String[] comments = oclCon.comments();
					comments = stripOclCommentMarkup(comments);
					text = StringUtils.join(comments, " ");
				} else {
					text = c.text();
				}
				r.addProperty(ISO19150_2.constraint, c.name() + ": " + text);
			}

		} else if (i.matches(
				OWLISO19150.RULE_OWL_ALL_CONSTRAINTS_BY_CONSTRAINT_MAPPING)) {

			for (Constraint c : cons) {

				ConstraintMapping cm = null;

				if (c instanceof OclConstraint) {

					if (!config.hasConstraintMapping(ConstraintType.OCL)) {
						result.addWarning(this, 30, "OCL");
						cm = defaultConstraintMapping;
					} else {
						cm = config.getConstraintMapping(ConstraintType.OCL);
					}

				} else if (c instanceof FolConstraint) {

					if (!config.hasConstraintMapping(ConstraintType.FOL)) {
						result.addWarning(this, 30, "FOL");
						cm = defaultConstraintMapping;
					} else {
						cm = config.getConstraintMapping(ConstraintType.FOL);
					}

				} else {
					// TextConstraint
					if (!config.hasConstraintMapping(ConstraintType.TEXT)) {
						result.addWarning(this, 30, "TEXT");
						cm = defaultConstraintMapping;
					} else {
						cm = config.getConstraintMapping(ConstraintType.TEXT);
					}
				}

				String template = cm.getTemplate();

				String doc = template;

				Pattern pattern = Pattern.compile("\\[\\[([^\\[].*?)\\]\\]");
				Matcher matcher = pattern.matcher(template);

				StringBuilder builder = new StringBuilder();

				int index = 0;
				while (matcher.find()) {

					String desc = matcher.group(1).trim();

					/*
					 * identify the constraint property from the field and get
					 * value(s)
					 */
					List<String> values = new ArrayList<String>();
					boolean descRecognized = true;

					if (desc.equalsIgnoreCase("name")) {

						values.add(c.name());

					} else if (desc.equalsIgnoreCase("text")) {

						String s = c.text();
						if (s != null && !s.trim().isEmpty()) {
							values.add(s);
						}

					} else if (desc.equalsIgnoreCase("status")) {

						String s = c.status();
						if (s != null && !s.trim().isEmpty()) {
							values.add(s);
						}

					} else if (desc.equalsIgnoreCase("comment")) {

						if (c instanceof OclConstraint) {
							String[] s = ((OclConstraint) c).comments();
							s = stripOclCommentMarkup(s);
							if (s != null && s.length > 0) {
								for (String ex : s) {
									if (ex.trim().length() > 0) {
										values.add(ex.trim());
									}
								}
							}
						}

					} else {
						/*
						 * the field in the template does not identify a known
						 * constraint property
						 */
						descRecognized = false;
					}

					/*
					 * append the text from the template up until the current
					 * find
					 */
					builder.append(doc.substring(index, matcher.start()));

					if (descRecognized) {

						if (values.isEmpty()) {
							values.add(cm.getNoValue());
						}

						if (values.size() == 1) {

							builder.append(values.get(0));

						} else {

							String connectedValues = StringUtils.join(values,
									cm.getMultiValueConnectorToken());

							builder.append(connectedValues);
						}

					} else {
						// template field not recognized - put it back in
						builder.append(matcher.group(0));
					}

					index = matcher.end();
				}

				// append any remaining text from the template
				builder.append(doc.substring(index, doc.length()));

				String target = cm.getTarget();
				addNamespaceDeclaration(target);

				String propertyIRI = computeReference(target);

				/*
				 * ensure that we import the ISO 19150-2 base ontology if we are
				 * using the default template
				 */
				if (cm == this.defaultConstraintMapping) {
					addImport(OWLISO19150.RDF_NS_ISO_19150_2,
							config.locationOfNamespace(
									OWLISO19150.RDF_NS_ISO_19150_2));
				}

				Property prop = ontmodel.createProperty(propertyIRI);

				if (cm.getFormat() == ConstraintMapping.Format.LANG_STRING) {
					r.addProperty(prop, builder.toString(),
							owliso19150.getLanguage());
				} else {
					// cm.getFormat() == ConstraintMapping.Format.STRING
					r.addProperty(prop, builder.toString());
				}
			}
		}
	}

	/**
	 * Removes any leading or trailing java comment markup; also trims the
	 * resulting string(s).
	 * 
	 * @param comments
	 * @return
	 */
	private String[] stripOclCommentMarkup(String[] comments) {

		if (comments == null || comments.length == 0) {
			return new String[] {};
		} else {
			List<String> res = new ArrayList<String>();
			for (String c : comments) {
				String tmp = c.trim();
				if (tmp.startsWith("/*")) {
					tmp = tmp.substring(2);
				}
				if (tmp.endsWith("*/")) {
					tmp = tmp.substring(0, tmp.length() - 2);
				}
				tmp = tmp.trim();
				res.add(tmp);
			}

			return res.toArray(new String[res.size()]);
		}
	}

	/**
	 * @param ci
	 */
	/**
	 * @param ci
	 */
	public void addCodelist(ClassInfo ci) {

		if (finalized) {

			this.result.addWarning(this, 3, ci.name());

		} else if (ci.matches(OWLISO19150.RULE_OWL_CLS_CODELIST_EXTERNAL) && !ci
				.taggedValuesForTagList("codeList,vocabulary").isEmpty()) {

			/*
			 * the class has tagged value 'codeList' or 'vocabulary' and shall
			 * be encoded under RULE_OWL_CLS_CODELIST_EXTERNAL
			 */
			this.resourceByClassInfo.put(ci, defaultTypeImplementation);

		} else if (ci.matches(OWLISO19150.RULE_OWL_CLS_CODELIST_191502)) {

			this.ontmodel.setNsPrefix("skos", OWLISO19150.RDF_NS_W3C_SKOS);

			String classURI = computeReference(getPrefix(), normalizedName(ci));

			/*
			 * create the Class <OWL>; we are already in the correct
			 * OntologyModel (the logic is in OWLISO19150.java)
			 */
			OntClass c = ontmodel.createClass(classURI);
			this.resourceByClassInfo.put(ci, c);

			applyDescriptorTargets(c, ci, DescriptorTarget.AppliesTo.CLASS);
			addConstraintDeclarations(c, ci);
			c.addSuperClass(SKOS.Concept);
			addCustomSubClassOf(c, ci);

			// now create the <OWL> individuals
			OntologyModel ontForIndividuals = owliso19150
					.computeRelevantOntologyForIndividuals(ci);
			OntModel ontmodelIndi = ontForIndividuals.getOntologyModel();
			ontmodelIndi.setNsPrefix("skos", OWLISO19150.RDF_NS_W3C_SKOS);
			ontmodelIndi.setNsPrefix("dct", OWLISO19150.RDF_NS_DCT);

			// create ConceptScheme <SKOS>
			String schemeURI = ontForIndividuals.getRdfNamespace()
					+ normalizedName(ci)
					+ owliso19150.getSkosConceptSchemeSuffix();

			Individual cs;

			if (ci.matches(
					OWLISO19150.RULE_OWL_CLS_CODELIST_191502_CONCEPTSCHEMESUBCLASS)) {

				String css_name;
				if (ci.taggedValuesForTag(
						OWLISO19150.TV_SKOS_CONCEPT_SCHEME_SUBCLASS_NAME).length > 0) {
					css_name = ci.taggedValuesForTag(
							OWLISO19150.TV_SKOS_CONCEPT_SCHEME_SUBCLASS_NAME)[0];
				} else {
					css_name = ontForIndividuals.getRdfNamespace()
							+ normalizedName(ci)
							+ owliso19150.getSkosConceptSchemeSubclassSuffix();
				}
				OntClass css = ontmodelIndi.createClass(css_name);
				css.addSuperClass(SKOS.ConceptScheme);

				cs = ontmodelIndi.createIndividual(schemeURI, css);

			} else {

				cs = ontmodelIndi.createIndividual(schemeURI,
						SKOS.ConceptScheme);
			}

			cs.addProperty(DCTerms.isFormatOf, c);
			applyDescriptorTargets(cs, ci,
					DescriptorTarget.AppliesTo.CONCEPT_SCHEME);

			// now add the individual concept definitions
			SortedMap<StructuredNumber, PropertyInfo> clPis = ci.properties();

			SortedMap<String, Individual> codesByUri = new TreeMap<String, Individual>();

			SortedMap<String, Individual> codesByPropertyName = new TreeMap<String, Individual>();
			// note: value may be null to indicate that no broader listed value
			// is defined for a property
			SortedMap<String, String> broaderListedValueByPropertyName = new TreeMap<String, String>();

			String indiBaseURI = ontForIndividuals.getRdfNamespace()
					+ normalizedName(ci);

			for (PropertyInfo pi : clPis.values()) {

				String clvUri = indiBaseURI + "/" + pi.name();
				ontmodelIndi.setNsPrefix(this.prefix, this.rdfNamespace);
				Individual clv = ontmodelIndi.createIndividual(clvUri, c);

				codesByUri.put(clvUri, clv);

				if (pi.matches(
						OWLISO19150.RULE_OWL_PROP_CODE_BROADER_BY_BROADER_LISTED_VALUE)) {
					codesByPropertyName.put(pi.name(), clv);
					String broaderListedValue = pi
							.taggedValue(OWLISO19150.TV_BROADER_LISTED_VALUE);
					broaderListedValueByPropertyName.put(pi.name(),
							broaderListedValue);
				}

				applyDescriptorTargets(clv, pi,
						DescriptorTarget.AppliesTo.PROPERTY);
				clv.addProperty(SKOS.inScheme, cs);
			}

			if (!broaderListedValueByPropertyName.isEmpty()) {

				/*
				 * RULE_OWL_PROP_CODE_BROADER_BY_BROADER_LISTED_VALUE is enabled
				 */
				for (Entry<String, String> e : broaderListedValueByPropertyName
						.entrySet()) {

					String piName = e.getKey();
					String broaderListedValue = e.getValue();

					Individual indi = codesByPropertyName.get(piName);

					if (broaderListedValue == null) {

						indi.addProperty(SKOS.topConceptOf, cs);

					} else {

						if (codesByPropertyName
								.containsKey(broaderListedValue)) {

							Individual broader = codesByPropertyName
									.get(broaderListedValue);
							indi.addProperty(SKOS.broader, broader);

						} else {

							MessageContext mc = result.addWarning(this, 38);
							if (mc != null) {
								mc.addDetail(this, 10001,
										ci.property(piName).fullNameInSchema());
							}
							indi.addProperty(SKOS.topConceptOf, cs);
						}
					}
				}
			}

			if (ci.matches(
					OWLISO19150.RULE_OWL_CLS_CODELIST_191502_SKOS_COLLECTION)) {

				String collectionURI = schemeURI + "Collection";

				Individual collection = ontmodelIndi
						.createIndividual(collectionURI, SKOS.Collection);

				for (Individual code : codesByUri.values()) {
					collection.addProperty(SKOS.member, code);
				}
			}

			if (ci.category() == Options.ENUMERATION
					&& !ci.properties().isEmpty() && ci.matches(
							OWLISO19150.RULE_OWL_CLS_CODELIST_191502_OBJECTONEOFFORENUMERATION)) {

				c.addProperty(OWL2.oneOf,
						ontmodel.createList(codesByUri.values().iterator()));
			}

			if (!ci.properties().isEmpty() && ci.matches(
					OWLISO19150.RULE_OWL_CLS_CODELIST_191502_DIFFERENTINDIVIDUALS)) {

				ontmodelIndi.createAllDifferent(ontmodelIndi
						.createList(codesByUri.values().iterator()));
			}

		} else {

			// none of the code list encoding rules matches
			this.resourceByClassInfo.put(ci, defaultTypeImplementation);
		}
	}

	private String computeReference(String prefix, String name) {

		String rdfns = ontmodel.getNsPrefixURI(prefix);
		return rdfns + name;
	}

	private String computeReference(String qname) {

		String[] parts = qname.split(":");

		String rdfns = ontmodel.getNsPrefixURI(parts[0]);
		return rdfns + parts[1];
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

		this.resourceByClassInfo.put(ci, e);

		applyDescriptorTargets(e, ci, DescriptorTarget.AppliesTo.CLASS);
		addConstraintDeclarations(e, ci);

		// assign stereotype information
		addCustomSubClassOf(e, ci);

		SortedMap<StructuredNumber, PropertyInfo> enumPis = ci.properties();

		if (!enumPis.isEmpty()) {

			List<Literal> enums = new ArrayList<Literal>();
			for (PropertyInfo pi : enumPis.values()) {
				Literal en = ontmodel.createLiteral(pi.name());
				enums.add(en);
			}
			e.addProperty(OWL2.oneOf, ontmodel.createList(enums.iterator()));
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

	public OntModel getOntologyModel() {
		return ontmodel;
	}

	public void addClass(ClassInfo ci) {

		this.classInfos.add(ci);
	}

	public Resource getResource(ClassInfo ci) {
		return this.resourceByClassInfo.get(ci);
	}

	public OntProperty getOntProperty(PropertyInfo pi) {
		OntProperty p = this.ontPropertyByPropertyInfo.get(pi);
		return p;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.MessageSource#message(int)
	 */
	public String message(int mnr) {

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
			return "Could not find a type mapping and also no class within the model to map class '$1$'.";
		case 8:
			return "No stereotype mapping defined for class '$1$' (a $2$).";
		case 9:
			return "Unsupported class category encountered while processing the stereotype '$2$' of class '$1$'.";
		case 10:
			return "Duplicate property mapping encountered in union (for type named '$1$').";
		case 11:
			return "In ontology '$1$' the prefix '$2$' is used for multiple URIs: '$3$' and '$4$'.";
		case 12:
			return "";
		case 13:
			return "Inverse property of '$1$' not found. The owl:inverseOf property has not been added.";
		case 14:
			return "??Type '$1$' is not covered by an RdfTypeMapEntry and also not contained in one of the schemas selected for processing. Cannot map or create the class.";
		case 15:
			return "??Association classes are not supported by this target. Association class '$1$' not represented in the ontology. Use the AssociationClassMapper transformation to convert association classes before executing the ontology target.";
		case 16:
			return "Property mapping with potentially inconsistent ranges. Type of property 1 is '$1$', while that of property 2 (to which 1 is mapped) is '$2$'.";
		case 17:
			return "Property mapping with potentially inconsistent definitions. Definition of property 1 is '$1$', while that of property 2 (to which 1 is mapped) is '$2$'.";
		case 18:
			return "Property mapping with potentially inconsistent descriptions. Description of property 1 is '$1$', while that of property 2 (to which 1 is mapped) is '$2$'.";
		case 19:
			return "Property mapping with potentially inconsistent alias names. Alias of property 1 is '$1$', while that of property 2 (to which 1 is mapped) is '$2$'.";
		case 20:
			return "Property '$1$' has been dropped as specified in the configuration.";
		case 21:
			return "Property '$1$' has been mapped to '$2$' as specified in the configuration.";
		case 22:
			return "??Class '$1$' has been mapped to '$2$' as specified in the configuration.";
		case 23:
			return "Property mapping with inconsistent ranges: '$2$' and '$3$'. One is a datatype and one a class.";
		case 24:
			return "Code list '$1$' is managed separately and the range is represented by the class '$2$'.";
		case 25:
			return "Cannot compare property specifications, because property 2 was not found in the model.";
		case 26:
			return "Rule $1$ is in effect, but tagged value '$2$' was not found. Ignoring the rule for computing the ontology name of package '$3$'.";
		case 27:
			return "The encoding rule does not contain a specific rule for creating the ontology name. Using "
					+ OWLISO19150.RULE_OWL_PKG_ONTOLOGY_NAME_ISO191502
					+ " to construct the ontology name for '$1$'.";
		case 28:
			return "Could not identify a mapping for the subtype '$1$' of class '$2$'. Cannot create a disjoint classes axiom for this subtype.";
		case 29:
			return "??The encoding rule contains both '$1$' and '$2$', which are mutually exclusive. Using '$2$'.";
		case 30:
			return "??No constraint mapping is defined for constraints of type '$1$'. Using defaults (template='[[name]]: [[text]]', noValue='', multiValueConnectorToken=' ').";
		case 31:
			return "Property shall be mapped to global property '$1$' in schema '$2$', but no applicable ontology was found. The property cannot be mapped.";
		case 32:
			return "Universal quantification not created for property, because an RdfPropertyMapEntry is defined for it and the map entry does not declare a specific range.";
		case 33:
			return "Qualified cardinality restrictions cannot be created for property, because no specific range is known. Using a unqualified cardinality restrictions instead.";
		case 34:
			return "Property '$1$' is mapped to global property '$2$' as specified in the configuration.";
		case 35:
			return "Range for property is undefined. This is ok if the property is mapped to an RDF/OWL property with global range declaration. Universal quantification is not created for this property.";
		case 36:
			return "Property shall be mapped to global property '$1$' in schema '$2$', but this global property could not be found. The range will be computed based upon the given property.";
		case 37:
			return "??No RdfTypeMapEntry is defined for the value type '$1$'. Also, the value type was not found in the model. Cannot map the value type.";
		case 38:
			return "Property has tagged value 'broaderListedValue' which does not identify another property of the class the property is in. Setting skos:topConceptOf for this property.";
		case 39:
			return "??No namespace configured for namespace abbreviation '$1$'. Cannot create an import and namespace declaration.";

		case 10000:
			return "--- Context - Class: $1$";
		case 10001:
			return "--- Context - Property: $1$";
		case 10003:
			return "--- Context - Property 1: $1$";
		case 10004:
			return "--- Context - Property 2: $1$";

		default:
			return "(Unknown message)";
		}
	}

}