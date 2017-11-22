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
 * (c) 2002-2015 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target.ReplicationSchema;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.SerializerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Multiplicity;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.Target.Target;
import de.interactive_instruments.ShapeChange.Target.TargetOutputProcessor;
import de.interactive_instruments.ShapeChange.Transformation.Flattening.Flattener;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class ReplicationXmlSchema implements Target, MessageSource {

	/* ------------------------------------------- */
	/* --- configuration parameter identifiers --- */
	/* ------------------------------------------- */

	/**
	 * Name of the field that contains the identifier of the object for which a
	 * data entity contains information. This parameter is optional. The default
	 * is {@value #DEFAULT_OBJECT_IDENTIFIER_FIELD_NAME}.
	 */
	public static final String PARAM_OBJECT_IDENTIFIER_FIELD_NAME = "objectIdentifierFieldName";

	/**
	 * Supports setting a suffix that will be appended to the target namespace
	 * of the replication schema that is produced by the target. This parameter
	 * is optional.
	 */
	public static final String PARAM_TARGET_NAMESPACE_SUFFIX = "targetNamespaceSuffix";

	/**
	 * Supports setting a suffix that will be appended to the name of properties
	 * that reference feature types. This parameter is optional.
	 */
	public static final String PARAM_SUFFIX_FOR_PROPERTIES_WITH_FEATURE_VALUE_TYPE = "suffixForPropertiesWithFeatureValueType";

	/**
	 * Supports setting a suffix that will be appended to the name of properties
	 * that reference object types. This parameter is optional.
	 */
	public static final String PARAM_SUFFIX_FOR_PROPERTIES_WITH_OBJECT_VALUE_TYPE = "suffixForPropertiesWithObjectValueType";

	/**
	 * Size for elements representing textual properties with limited length, to
	 * be used in case that the property represented by the element does not
	 * have a 'size' tagged value; by default an element with textual type does
	 * not have a size/length restriction.
	 */
	public static final String PARAM_SIZE = "size";

	/* ------------------------ */
	/* --- rule identifiers --- */
	/* ------------------------ */

	/**
	 * If this rule is enabled, an object identifier will be added to feature
	 * and object types. The element will be created as first content element
	 * inside of a data entity, with XML Schema type xs:string. The element will
	 * be required and not nillable. The name of the element can be configured
	 * via parameter {@value #PARAM_OBJECT_IDENTIFIER_FIELD_NAME}.
	 */
	public static final String RULE_TGT_REP_CLS_GENERATE_OBJECTIDENTIFIER = "rule-rep-cls-generate-objectidentifier";

	/**
	 * If this rule is enabled derived properties will be ignored.
	 */
	public static final String RULE_TGT_REP_PROP_EXCLUDE_DERIVED = "rule-rep-prop-exclude-derived";

	/**
	 * If this rule is enabled all elements that represent properties from the
	 * conceptual schema will have minOccurs=0. This does not apply to elements
	 * that were generated by the target, for example object identifier
	 * elements.
	 */
	public static final String RULE_TGT_REP_PROP_OPTIONAL = "rule-rep-prop-optional";

	/**
	 * If this rule is enabled then properties with specific value types receive
	 * a length restriction. The types are identified by map entries: a map
	 * entry must identify the value type in its 'type' attribute and have a
	 * 'param' attribute with value 'maxLengthFromSize'. Whenever a property has
	 * an according value type its maxLength is determined by the setting of the
	 * {@value TV_SIZE} tagged value on the property or the global target
	 * parameter {@value PARAM_SIZE}. The tagged value takes precedence over the
	 * target parameter. If neither tagged value nor target parameter are set,
	 * no maxLength restriction is created.
	 */
	public static final String RULE_TGT_REP_PROP_MAXLENGTHFROMSIZE = "rule-rep-prop-maxLength-from-size";

	/* --------------------- */
	/* --- tagged values --- */
	/* --------------------- */

	/**
	 * Name of the tagged value via which properties with textual type can be
	 * restricted in length. If
	 */
	public static final String TV_SIZE = "size";

	/* -------------------- */
	/* --- other fields --- */
	/* -------------------- */

	public static final String DEFAULT_OBJECT_IDENTIFIER_FIELD_NAME = "id";
	public static final String MAP_ENTRY_PARAM_MAXLENGTHFROMSIZE = "maxLengthFromSize";

	private ShapeChangeResult result = null;
	private PackageInfo schemaPi = null;
	private Model model = null;
	private Options options = null;
	private boolean printed = false;
	private boolean diagnosticsOnly = false;
	private File outputDirectoryFile;
	private String outputDirectory;
	private String outputFilename;

	protected Document document = null;
	protected Element root = null;
	protected Map<String, ProcessMapEntry> mapEntryByType = new HashMap<String, ProcessMapEntry>();

	protected String targetNamespace = null;
	protected String objectIdentifierFieldName;
	protected String suffixForPropWithFeatureValueType;
	protected String suffixForPropWithObjectValueType;
	protected Multiplicity multiplicity1 = new Multiplicity(1, 1);
	protected Integer defaultSize = null;
	protected String targetNamespaceSuffix;

	private TreeSet<PackageInfo> packagesForImport = new TreeSet<PackageInfo>(
			new Comparator<PackageInfo>() {

				@Override
				public int compare(PackageInfo pi1, PackageInfo pi2) {
					return pi1.name().compareTo(pi2.name());
				}
			});

	@Override
	public void initialise(PackageInfo p, Model m, Options o,
			ShapeChangeResult r, boolean diagOnly)
			throws ShapeChangeAbortException {

		schemaPi = p;
		model = m;
		options = o;
		result = r;
		diagnosticsOnly = diagOnly;

		result.addDebug(this, 1, schemaPi.name());

		outputDirectory = options.parameter(this.getClass().getName(),
				"outputDirectory");
		if (outputDirectory == null)
			outputDirectory = options.parameter("outputDirectory");
		if (outputDirectory == null)
			outputDirectory = options.parameter(".");

		outputFilename = schemaPi.name().replace("/", "_").replace(" ", "_")
				+ ".xsd";

		// Check if we can use the output directory; create it if it
		// does not exist
		outputDirectoryFile = new File(outputDirectory);
		boolean exi = outputDirectoryFile.exists();
		if (!exi) {
			try {
				FileUtils.forceMkdir(outputDirectoryFile);
			} catch (IOException e) {
				result.addError(null, 600, e.getMessage());
				e.printStackTrace(System.err);
			}
			exi = outputDirectoryFile.exists();
		}
		boolean dir = outputDirectoryFile.isDirectory();
		boolean wrt = outputDirectoryFile.canWrite();
		boolean rea = outputDirectoryFile.canRead();
		if (!exi || !dir || !wrt || !rea) {
			result.addFatalError(null, 601, outputDirectory);
			throw new ShapeChangeAbortException();
		}

		File outputFile = new File(outputDirectoryFile, outputFilename);

		// check if output file already exists - if so, attempt to delete it
		exi = outputFile.exists();
		if (exi) {

			result.addInfo(this, 3, outputFilename, outputDirectory);

			try {
				FileUtils.forceDelete(outputFile);
				result.addInfo(this, 4);
			} catch (IOException e) {
				result.addInfo(null, 600, e.getMessage());
				e.printStackTrace(System.err);
			}
		}

		// identify map entries defined in the target configuration
		List<ProcessMapEntry> mapEntries = options.getCurrentProcessConfig()
				.getMapEntries();

		if (mapEntries == null || mapEntries.isEmpty()) {

			result.addFatalError(this, 9);
			throw new ShapeChangeAbortException();

		} else {

			for (ProcessMapEntry pme : mapEntries) {
				this.mapEntryByType.put(pme.getType(), pme);
			}
		}

		// ======================================
		// Parse configuration parameters
		// ======================================

		objectIdentifierFieldName = options.parameter(this.getClass().getName(),
				PARAM_OBJECT_IDENTIFIER_FIELD_NAME);
		if (objectIdentifierFieldName == null
				|| objectIdentifierFieldName.trim().length() == 0) {
			objectIdentifierFieldName = DEFAULT_OBJECT_IDENTIFIER_FIELD_NAME;
		}

		suffixForPropWithFeatureValueType = options.parameter(
				this.getClass().getName(),
				PARAM_SUFFIX_FOR_PROPERTIES_WITH_FEATURE_VALUE_TYPE);
		if (suffixForPropWithFeatureValueType == null
				|| suffixForPropWithFeatureValueType.trim().length() == 0) {
			suffixForPropWithFeatureValueType = "";
		}

		suffixForPropWithObjectValueType = options.parameter(
				this.getClass().getName(),
				PARAM_SUFFIX_FOR_PROPERTIES_WITH_OBJECT_VALUE_TYPE);
		if (suffixForPropWithObjectValueType == null
				|| suffixForPropWithObjectValueType.trim().length() == 0) {
			suffixForPropWithObjectValueType = "";
		}

		targetNamespaceSuffix = options.parameter(this.getClass().getName(),
				PARAM_TARGET_NAMESPACE_SUFFIX);
		if (targetNamespaceSuffix == null
				|| objectIdentifierFieldName.trim().length() == 0) {
			targetNamespaceSuffix = "";
		}

		String defaultSizeByConfig = options
				.parameter(this.getClass().getName(), PARAM_SIZE);
		if (defaultSizeByConfig != null) {
			try {
				defaultSize = Integer.parseInt(defaultSizeByConfig);
			} catch (NumberFormatException e) {
				MessageContext mc = result.addWarning(this, 8, PARAM_SIZE,
						e.getMessage());
				mc.addDetail(this, 0);
			}
		}

		// ======================================
		// Set up the document and create root
		// ======================================

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		dbf.setValidating(true);
		dbf.setAttribute(Options.JAXP_SCHEMA_LANGUAGE, Options.W3C_XML_SCHEMA);
		DocumentBuilder db;
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			result.addFatalError(null, 2);
			throw new ShapeChangeAbortException();
		}
		document = db.newDocument();

		root = document.createElementNS(Options.W3C_XML_SCHEMA, "schema");
		document.appendChild(root);

		addAttribute(root, "xmlns", Options.W3C_XML_SCHEMA);
		addAttribute(root, "elementFormDefault", "qualified");

		addAttribute(root, "version", schemaPi.version());
		targetNamespace = schemaPi.targetNamespace() + targetNamespaceSuffix;
		addAttribute(root, "targetNamespace", targetNamespace);
		addAttribute(root, "xmlns:" + schemaPi.xmlns(), targetNamespace);

		if (options.getCurrentProcessConfig().parameterAsString(
				TargetOutputProcessor.PARAM_ADD_COMMENT, null, false,
				true) == null) {
			Comment generationComment = document.createComment(
					"XML Schema document created by ShapeChange - http://shapechange.net/");
			root.appendChild(generationComment);
		}
	}

	/** Add attribute to an element */
	protected void addAttribute(Element e, String name, String value) {
		Attr att = document.createAttribute(name);
		att.setValue(value);
		e.setAttributeNode(att);
	}

	@Override
	public void process(ClassInfo ci) {

		if (ci == null || ci.pkg() == null)
			return;

		result.addDebug(this, 6, ci.name());

		int cat = ci.category();

		// report unsupported categories
		switch (cat) {
		case Options.UNION:
			result.addWarning(this, 7, ci.name(), "union");
			break;
		case Options.DATATYPE:
			result.addWarning(this, 7, ci.name(), "datatype");
			break;
		case Options.MIXIN:
			result.addWarning(this, 7, ci.name(), "mixin");
			break;
		}

		// Object element
		switch (cat) {
		case Options.ENUMERATION:
		case Options.CODELIST:
			// object elements are not created for enumerations and codelists
			break;
		case Options.FEATURE:
		case Options.OBJECT:
			pObjectElement(ci);
			break;
		}

		// Content model
		switch (cat) {
		case Options.ENUMERATION:
			pGlobalEnumeration(ci);
			break;
		case Options.FEATURE:
		case Options.OBJECT:
			Element propertyHook = pComplexType(ci);
			processLocalProperties(ci, propertyHook);
			break;
		}
	}

	/**
	 * Process all properties that are added in this class.
	 */
	public void processLocalProperties(ClassInfo ci, Element sequenceOrChoice) {

		result.addDebug(null, 10023, ci.name());

		/*
		 * If we have a feature or object type and object identifiers shall be
		 * created for them, do so.
		 */
		if ((ci.category() == Options.FEATURE
				|| ci.category() == Options.OBJECT)
				&& ci.matches(RULE_TGT_REP_CLS_GENERATE_OBJECTIDENTIFIER)) {

			Element eOID = document.createElementNS(Options.W3C_XML_SCHEMA,
					"element");
			addAttribute(eOID, "name", this.objectIdentifierFieldName);
			addAttribute(eOID, "type", "string");
			addMinMaxOccurs(eOID, this.multiplicity1);
			sequenceOrChoice.appendChild(eOID);
		}

		/*
		 * Now process the properties of the class.
		 */
		for (PropertyInfo pi : ci.properties().values()) {
			processLocalProperty(pi, sequenceOrChoice);
		}
	}

	/**
	 * Process a class property.
	 *
	 * @param pi
	 *            property to process
	 * @param sequenceOrChoice
	 *            element to which the property element shall be appended
	 * @return
	 */
	public void processLocalProperty(PropertyInfo pi,
			Element sequenceOrChoice) {

		if (includeProperty(pi)) {

			Element piElement = addProperty(pi);

			if (piElement.getLocalName().equals("attribute")
					|| piElement.getLocalName().equals("attributeGroup")) {

				sequenceOrChoice.getParentNode().appendChild(piElement);

			} else {

				sequenceOrChoice.appendChild(piElement);
			}
		}
	}

	/** Process a single property. */
	protected Element addProperty(PropertyInfo pi) {

		ClassInfo inClass = pi.inClass();

		Element e1 = document.createElementNS(Options.W3C_XML_SCHEMA,
				"element");
		addGlobalIdentifierAnnotation(e1, pi);

		String piName = pi.name();
		if (pi.categoryOfValue() == Options.FEATURE) {
			piName = pi.name() + suffixForPropWithFeatureValueType;
		} else if (pi.categoryOfValue() == Options.OBJECT) {
			piName = pi.name() + suffixForPropWithObjectValueType;
		}
		addAttribute(e1, "name", piName);

		mapPropertyType(pi, e1);
		addMinMaxOccurs(e1, pi);

		// add maxLength restriction if applicable
		ProcessMapEntry pme = mapEntryByType.get(pi.typeInfo().name);
		if (pme != null && pme.hasParam() && pme.getParam().trim()
				.equals(MAP_ENTRY_PARAM_MAXLENGTHFROMSIZE)) {

			int size = -1;

			if (pi.taggedValue(TV_SIZE) != null
					&& pi.taggedValue(TV_SIZE).trim().length() > 0) {
				try {
					size = Integer.parseInt(pi.taggedValue(TV_SIZE).trim());
				} catch (NumberFormatException e) {
					result.addWarning(this, 14, TV_SIZE, pi.name(),
							inClass.name(), e.getMessage());
				}
			}

			if (size <= 0 && this.defaultSize != null) {
				size = this.defaultSize;
			}

			if (size > 0) {

				Element simpleType = document
						.createElementNS(Options.W3C_XML_SCHEMA, "simpleType");
				e1.appendChild(simpleType);

				Element restriction = document
						.createElementNS(Options.W3C_XML_SCHEMA, "restriction");
				addAttribute(restriction, "base", "string");
				e1.removeAttribute("type");
				simpleType.appendChild(restriction);

				Element concreteRestriction = document
						.createElementNS(Options.W3C_XML_SCHEMA, "maxLength");
				addAttribute(concreteRestriction, "value", "" + size);
				restriction.appendChild(concreteRestriction);
			}
		}

		return e1;
	}

	/**
	 * Set the type for a property element.
	 *
	 * @param propi
	 *            the property
	 * @param e
	 *            property element
	 */
	protected void mapPropertyType(PropertyInfo propi, Element e) {

		ClassInfo ci = propi.inClass();

		String pName = ci.name() + "." + propi.name();

		/*
		 * First look into the mapping tables for entries that map the type to a
		 * pre-defined simple type
		 */
		Type ti = propi.typeInfo();
		ProcessMapEntry pme = this.mapEntryByType.get(ti.name);

		if (pme != null) {

			/*
			 * So we have a mapping to a simple type
			 */

			String targetType;

			if (pme.hasTargetType()) {

				targetType = pme.getTargetType();

			} else {

				targetType = "fixme:fixme";
				result.addError(this, 11, ti.name, targetType);
			}

			addAttribute(e, "type", targetType);
			return;
		}

		/*
		 * If we end up here, we have no map entry in the configuration, so we
		 * look at the value type to determine the proper property type. So
		 * first get the class of the value type.
		 */

		ClassInfo typeCi = model.classById(ti.id);

		if (typeCi == null) {

			// try to get by-name if link by-id is broken
			typeCi = model.classByName(ti.name);

			if (typeCi != null) {
				MessageContext mc = result.addError(null, 135, pName);
				if (mc != null)
					mc.addDetail(null, 400, "Property",
							propi.fullNameInSchema());
			}
		}

		if (typeCi == null) {

			MessageContext mc = result.addError(null, 131, pName, ti.name);
			if (mc != null)
				mc.addDetail(null, 400, "Property", propi.fullNameInSchema());

		} else {

			if (classHasObjectType(typeCi)) {

				/*
				 * In the replication schema this is encoded as an element with
				 * type string
				 */
				addAttribute(e, "type", "string");

			} else {

				if (typeCi.category() == Options.CODELIST) {

					addAttribute(e, "type", "string");

				} else if (typeCi.category() == Options.ENUMERATION) {

					addAttribute(e, "type",
							options.internalize(typeCi.qname() + "Type"));

					// enumerations may reside in another namespace, they are
					// usually not flattened
					if (!typeCi.pkg().targetNamespace()
							.equals(schemaPi.targetNamespace())) {
						packagesForImport.add(typeCi.pkg());
					}

				} else {

					addAttribute(e, "type", "fixme:fixme");
					result.addError(this, 13, pName, "fixme:fixme");
				}
			}
		}
	}

	private void addMinMaxOccurs(Element e, PropertyInfo pi) {

		Multiplicity m = pi.cardinality();

		if (pi.matches(RULE_TGT_REP_PROP_OPTIONAL)) {

			addMinMaxOccurs(e, new Multiplicity(0, m.maxOccurs));

		} else {

			addMinMaxOccurs(e, m);
		}
	}

	private void addMinMaxOccurs(Element e, Multiplicity m) {

		if (m.minOccurs != 1) {
			addAttribute(e, "minOccurs",
					options.internalize(((Integer) m.minOccurs).toString()));
		}

		// determine maxOccurs
		if (m.maxOccurs == Integer.MAX_VALUE) {

			addAttribute(e, "maxOccurs", "unbounded");

		} else if (m.maxOccurs != 1) {

			String maxOccursTxt = options
					.internalize(((Integer) m.maxOccurs).toString());
			addAttribute(e, "maxOccurs", maxOccursTxt);
		}
	}

	private boolean includeProperty(PropertyInfo pi) {
		return pi.isNavigable() && !(pi.isDerived()
				&& pi.matches(RULE_TGT_REP_PROP_EXCLUDE_DERIVED));
	}

	public Element pComplexType(ClassInfo ci) {

		Element e1 = document.createElementNS(Options.W3C_XML_SCHEMA,
				"complexType");
		document.getDocumentElement().appendChild(e1);
		addAttribute(e1, "name", typeName(ci, false));

		if (ci.isAbstract())
			addAttribute(e1, "abstract", "true");

		Element e3;

		// String s = null;
		// if (cibase != null) {
		// s = mapBaseType(cibase);
		// if (s == null) {
		// result.addError(null, 158);
		// s = "fixme:fixme";
		// }
		// }
		// if (s != null) {
		// Element e2 = document.createElementNS(Options.W3C_XML_SCHEMA,
		// "complexContent");
		// e1.appendChild(e2);
		// e3 = document.createElementNS(Options.W3C_XML_SCHEMA, "extension");
		// e2.appendChild(e3);
		// addAttribute(e3, "base", s);
		// } else if (cibase != null) {
		// MessageContext mc = result.addError(null, 121,
		// (cibase == null ? "<unknown>" : cibase.name()),
		// typeName(ci, false));
		// if (mc != null)
		// mc.addDetail(null, 400, "Class",
		// (ci == null ? "<unknown>" : ci.fullName()));
		// e3 = e1;
		// } else
		e3 = e1;

		Element ret = document.createElementNS(Options.W3C_XML_SCHEMA,
				"sequence");
		e3.appendChild(ret);

		return ret;
	}

	/**
	 * Map a base type of a class to a predefined representation.
	 */
	protected String mapBaseType(ClassInfo ci) {

		String s = null;

		if (classHasObjectType(ci)) {
			s = typeName(ci, true);
		} else {
			MessageContext mc = result.addError(null, 117, ci.name());
			if (mc != null)
				mc.addDetail(null, 400, "Class", ci.fullName());
		}

		return s;
	}

	public void pGlobalEnumeration(ClassInfo ci) {

		Element e1 = pAnonymousEnumeration(ci);

		if (e1 != null) {

			document.getDocumentElement().appendChild(e1);
			addAttribute(e1, "name", typeName(ci, false));

		} else {

			MessageContext mc = result.addError(null, 126, typeName(ci, false));
			if (mc != null)
				mc.addDetail(null, 400, "Class", ci.fullName());
		}
	}

	/** map to enumeration type */
	private Element pAnonymousEnumeration(ClassInfo ci) {

		Element e1 = document.createElementNS(Options.W3C_XML_SCHEMA,
				"simpleType");
		Element e4 = document.createElementNS(Options.W3C_XML_SCHEMA,
				"restriction");
		e1.appendChild(e4);
		addAttribute(e4, "base", "string");

		for (PropertyInfo atti : ci.properties().values()) {

			Element e3 = document.createElementNS(Options.W3C_XML_SCHEMA,
					"enumeration");
			e4.appendChild(e3);
			String val = atti.name();
			if (atti.initialValue() != null) {
				val = atti.initialValue();
			}
			addAttribute(e3, "value", val);
			// TODO hevan: check with KMD if this could be useful or not
			// (answer: not needed but no problem if present)
			// addGlobalIdentifierAnnotation(e3, atti);
		}

		return e1;
	}

	/**
	 * Create global element for a feature / object type
	 */
	public void pObjectElement(ClassInfo ci) {

		if (ci.pkg() == null || ci.pkg().xmlns() == null) {

			MessageContext mc = result.addError(null, 132, ci.name());
			if (mc != null)
				mc.addDetail(null, 400, "Class", ci.fullName());
			return;
		}

		Element e4 = document.createElementNS(Options.W3C_XML_SCHEMA,
				"element");
		document.getDocumentElement().appendChild(e4);
		addAttribute(e4, "name", elementName(ci, false));
		addAttribute(e4, "type", typeName(ci, true));
		addGlobalIdentifierAnnotation(e4, ci);

		if (ci.isAbstract()) {
			addAttribute(e4, "abstract", "true");
		}

		/*
		 * NOTE: in a replication schema data entities are not in the
		 * substitution group of GML types and inheritance is ignored (must have
		 * been flattened beforehand). Thus inheritance structures are ignored
		 * by the replication schema.
		 */
		if (ci.baseClass() != null) {
			result.addWarning(this, 15, ci.name(), ci.baseClass().name());
		}
	}

	private boolean classHasObjectType(ClassInfo ci) {
		int cat = ci.category();
		return cat == Options.FEATURE || cat == Options.OBJECT;
	}

	/**
	 * Add standard annotations for data entities and fields. Standard
	 * annotation consists of:
	 * <ul>
	 * <li>the global identifier (if it exists) for feature and object types as
	 * well as properties</li>
	 * </ul>
	 */
	protected void addGlobalIdentifierAnnotation(Element e, Info info) {

		Element eAppInfo = null;

		if (info instanceof ClassInfo) {

			ClassInfo ci = (ClassInfo) info;

			if (ci.globalIdentifier() != null
					&& (ci.category() == Options.FEATURE
							|| ci.category() == Options.OBJECT)) {

				if (eAppInfo == null)
					eAppInfo = document.createElementNS(Options.W3C_XML_SCHEMA,
							"appinfo");
				eAppInfo.appendChild(
						document.createTextNode(ci.globalIdentifier()));
			}

		} else if (info instanceof PropertyInfo) {

			PropertyInfo pi = (PropertyInfo) info;

			if (pi.globalIdentifier() != null) {

				if (eAppInfo == null)
					eAppInfo = document.createElementNS(Options.W3C_XML_SCHEMA,
							"appinfo");
				eAppInfo.appendChild(
						document.createTextNode(pi.globalIdentifier()));
			}
		}

		if (eAppInfo != null) {

			Element e0 = document.createElementNS(Options.W3C_XML_SCHEMA,
					"annotation");
			e0.appendChild(eAppInfo);
			e.appendChild(e0);
		}
	}

	private String elementName(ClassInfo ci, boolean qualified) {

		if (ci == null) {

			// nothing to do;

		} else if (ci.pkg() == null || ci.pkg().xmlns() == null) {

			MessageContext mc = result.addError(null, 132, ci.name());
			if (mc != null)
				mc.addDetail(null, 400, "Class", ci.fullNameInSchema());

		} else {

			return (qualified ? ci.qname() : ci.name());
		}

		return null;
	}

	private String typeName(ClassInfo ci, boolean qualified) {

		if (ci == null) {

			// nothing to do;

		} else if (ci.pkg() == null || ci.pkg().xmlns() == null) {

			MessageContext mc = result.addError(null, 132, ci.name());
			if (mc != null)
				mc.addDetail(null, 400, "Class", ci.fullName());

		} else {

			return (qualified ? ci.qname() : ci.name()) + "Type";
		}

		return null;
	}

	@Override
	public void write() {

		if (printed || diagnosticsOnly) {
			return;
		}

		addImports();

		Properties outputFormat = OutputPropertiesFactory
				.getDefaultMethodProperties("xml");
		outputFormat.setProperty("indent", "yes");
		outputFormat.setProperty("{http://xml.apache.org/xalan}indent-amount",
				"2");
		outputFormat.setProperty("encoding", "UTF-8");

		/*
		 * Uses OutputStreamWriter instead of FileWriter to set character
		 * encoding (see doc in Serializer.setWriter and FileWriter)
		 */
		try {
			File repXsd = new File(outputDirectoryFile, outputFilename);

			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(repXsd), "UTF-8"));

			Serializer serializer = SerializerFactory
					.getSerializer(outputFormat);
			serializer.setWriter(writer);
			serializer.asDOMSerializer().serialize(document);

			writer.close();

			result.addResult(getTargetName(), outputDirectory, outputFilename,
					schemaPi.targetNamespace());

		} catch (IOException ioe) {

			result.addError(null, 171, outputFilename);
		}

		printed = true;
	}

	/**
	 * Add &lt;import&gt; tags as the first content in the &lt;schema&gt; tag.
	 */
	private void addImports() {

		Node anchor = null;

		Element importElement;
		for (PackageInfo packageInfo : packagesForImport) {
			addAttribute(root, "xmlns:" + packageInfo.xmlns(),
					packageInfo.targetNamespace() + targetNamespaceSuffix);
			importElement = document.createElementNS(Options.W3C_XML_SCHEMA,
					"import");
			addAttribute(importElement, "namespace",
					packageInfo.targetNamespace() + targetNamespaceSuffix);

			if (anchor == null) {
				root.insertBefore(importElement, root.getFirstChild());
			} else {
				root.insertBefore(importElement, anchor.getNextSibling());
			}
			anchor = importElement;
		}
	}

	@Override
	public String getTargetName() {
		return "Replication XML Schema";
	}

	@Override
	public String message(int mnr) {

		switch (mnr) {
		case 0:
			return "Context: class ReplicationSchema";
		case 1:
			return "Generating replication schema for application schema '$1$'.";
		case 2:
			return ""; // unused (moved to ShapeChangeResult)
		case 3:
			return "Output file '$1$' already exists in output directory ('$2$'). It will be deleted prior to processing.";
		case 4:
			return "File has been deleted.";
		case 5:
			return ""; // unused (moved to ShapeChangeResult)
		case 6:
			return "Processing class '$1$'.";
		case 7:
			return "Class '$1$' is a $2$ which is not supported by this target. The class will be ignored.";
		case 8:
			return "Number format exception while converting the value of configuration parameter '$1$' to an integer. Exception message: $2$. The parameter will be ignored.";
		case 9:
			return "No map entries provided via the configuration. Target cannot be executed.";
		case 10:
			return "No map entry defined for type '$1$'. Cannot map this type.";
		case 11:
			return "No target type defined in type mapping for type '$1$'. Using '$2$' instead.";
		case 12:
			return "No map entry defined for type '$1$'. Using 'string' as target type.";
		case 13:
			return "Property '$1$' has an unsupported value type. Using '$2$' as target type.";
		case 14:
			return "Number format exception while converting tagged value '$1$' on property '$2$' of class '$3$' to an integer. Exception message: $4$. Global size setting for target will be used if available.";
		case 15:
			return "Class '$1$' has supertype '$2$'. Inheritance is not supported by this target. Apply "
					+ Flattener.RULE_TRF_CLS_FLATTEN_INHERITANCE
					+ " of the Flattener transformer before executing this target.";

		case 100:
			return "Context: property '$1$' in class '$2$'.";
		default:
			return "(" + ReplicationXmlSchema.class.getName()
					+ ") Unknown message with number: " + mnr;
		}
	}

}
