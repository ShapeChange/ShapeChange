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
 * (c) 2002-2012 interactive instruments GmbH, Bonn, Germany
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

package de.interactive_instruments.shapechange.core.target.xmlschema;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.interactive_instruments.shapechange.core.util.XMLUtil;
import de.interactive_instruments.shapechange.core.MapEntry;
import de.interactive_instruments.shapechange.core.MessageSource;
import de.interactive_instruments.shapechange.core.Multiplicity;
import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ShapeChangeAbortException;
import de.interactive_instruments.shapechange.core.ShapeChangeException;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.TargetXmlSchemaConfiguration;
import de.interactive_instruments.shapechange.core.Type;
import de.interactive_instruments.shapechange.core.XsdPropertyMapEntry;
import de.interactive_instruments.shapechange.core.ShapeChangeResult.MessageContext;
import de.interactive_instruments.shapechange.core.model.AssociationInfo;
import de.interactive_instruments.shapechange.core.model.ClassInfo;
import de.interactive_instruments.shapechange.core.model.Descriptor;
import de.interactive_instruments.shapechange.core.model.Descriptors;
import de.interactive_instruments.shapechange.core.model.Info;
import de.interactive_instruments.shapechange.core.model.LangString;
import de.interactive_instruments.shapechange.core.model.Model;
import de.interactive_instruments.shapechange.core.model.PackageInfo;
import de.interactive_instruments.shapechange.core.model.PropertyInfo;
import de.interactive_instruments.shapechange.core.model.Qualifier;
import de.interactive_instruments.shapechange.core.model.TaggedValues;
import de.interactive_instruments.shapechange.core.target.TargetOutputProcessor;
import de.interactive_instruments.shapechange.core.target.xml_encoding_util.XmlEncodingInfos;

public class XsdDocument implements MessageSource {

    public static final String DGIWGSP_NSABR = "gmldgiwgsp";
    public static final String DGIWGSP_NS = "http://www.dgiwg.org/gml/3.2/profiles/spatial/1.0/";

    public static final String GMLSF_NSABR = "gmlsf";
    public static final String GMLSF_NS = "http://www.opengis.net/gmlsf/2.0";

    protected Document document = null;
    protected Element root = null;
    protected Element rootAnnotation = null;
    protected Options options = null;
    protected ShapeChangeResult result = null;
    protected TargetXmlSchemaConfiguration config = null;
    protected Model model = null;
    protected String name = null;
    protected Vector<String> includes = new Vector<String>();
    protected Vector<String> imports = new Vector<String>();
    protected boolean printed = false;
    protected String targetNamespace = null;
    protected String targetNamespaceAbbreviation = null;
    protected String outputDirectory;
    protected String documentationTemplate = null;
    protected String documentationNoValue = null;
    protected String okstraKeyValuePropertyType;
    protected String okstraKeyValueBaseType;
    protected String okstraObjectRefType;
    protected String okstraPrefix;
    protected String okstra;
    protected String codeListRepresentationTVFallback;
    protected EnumSet<Descriptor> descriptorsToRepresent = EnumSet.noneOf(Descriptor.class);
    protected SchematronSchema schDoc;

    protected boolean writeXmlEncodingInfos = false;
    protected XmlEncodingInfos encodingInfos = new XmlEncodingInfos();

    public XsdDocument(PackageInfo pi, Model m, Options o, ShapeChangeResult r, TargetXmlSchemaConfiguration c,
	    String n, SchematronSchema schDoc) throws ShapeChangeAbortException, ParserConfigurationException {
	options = o;
	result = r;
	model = m;
	config = c;
	name = n;
	this.schDoc = schDoc;

	outputDirectory = options.parameter(Options.TargetXmlSchemaClass, "outputDirectory");
	if (outputDirectory == null)
	    outputDirectory = options.parameter("outputDirectory");
	if (outputDirectory == null)
	    outputDirectory = options.parameter(".");

	// change the default documentation template?
	documentationTemplate = options.parameter(Options.TargetXmlSchemaClass, "documentationTemplate");
	documentationNoValue = options.parameter(Options.TargetXmlSchemaClass, "documentationNoValue");

	codeListRepresentationTVFallback = options.parameterAsString(XmlSchema.class.getName(),
		XmlSchemaConstants.PARAM_DEFAULT_CODELIST_REPRESENTATION, null, false, true);

	List<String> namesOfDescriptorsToRepresent = options.parameterAsStringList(Options.TargetXmlSchemaClass,
		XmlSchemaConstants.PARAM_REPRESENT_DESCRIPTORS, null, true, true);

	for (String descriptorName : namesOfDescriptorsToRepresent) {
	    /*
	     * NOTE: The configuration validator ensures that the parameter only contains
	     * known descriptors.
	     */
	    Descriptor desc = Descriptor.valueOf(descriptorName.toUpperCase(Locale.ENGLISH));
	    descriptorsToRepresent.add(desc);
	}

	String s = options.parameter(Options.TargetXmlSchemaClass,
		XmlSchemaConstants.PARAM_OKSTRA_KEY_VALUE_PROPERTY_TYPE);
	if (s != null)
	    okstraKeyValuePropertyType = s;
	else
	    okstraKeyValuePropertyType = "okstra-basis:KeyValuePropertyType";

	s = options.parameter(Options.TargetXmlSchemaClass, XmlSchemaConstants.PARAM_OKSTRA_KEY_VALUE_BASE_TYPE);
	if (s != null)
	    okstraKeyValueBaseType = s;
	else
	    okstraKeyValueBaseType = "gml:AbstractFeatureType";

	s = options.parameter(Options.TargetXmlSchemaClass, XmlSchemaConstants.PARAM_OKSTRA_OBJECT_REF_TYPE);
	if (s != null)
	    okstraObjectRefType = s;
	else
	    okstraObjectRefType = "okstra-basis:ObjectRefType";

	okstraPrefix = (okstraKeyValuePropertyType.split(":"))[0];

	s = options.parameter(Options.TargetXmlSchemaClass, XmlSchemaConstants.PARAM_OKSTRA);
	if (s != null)
	    okstra = s;
	else
	    okstra = "false";

	writeXmlEncodingInfos = options.getCurrentProcessConfig()
		.parameterAsBoolean(XmlSchemaConstants.PARAM_WRITE_XML_ENCODING_INFOS, false);

	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	dbf.setNamespaceAware(true);
	dbf.setValidating(true);
	dbf.setAttribute(Options.JAXP_SCHEMA_LANGUAGE, Options.W3C_XML_SCHEMA);
	DocumentBuilder db = dbf.newDocumentBuilder();
	document = db.newDocument();

	root = document.createElementNS(Options.W3C_XML_SCHEMA, "schema");
	document.appendChild(root);

	addAttribute(root, "xmlns", Options.W3C_XML_SCHEMA);
	addAttribute(root, "elementFormDefault", "qualified");

	addAttribute(root, "version", pi.version());
	targetNamespace = pi.targetNamespace();
	targetNamespaceAbbreviation = pi.xmlns();
	addAttribute(root, "targetNamespace", targetNamespace);
	addAttribute(root, "xmlns:" + pi.xmlns(), targetNamespace);

	rootAnnotation = addStandardAnnotation(root, pi);

	if (options.getCurrentProcessConfig().parameterAsString(TargetOutputProcessor.PARAM_ADD_COMMENT, null, false,
		true) == null) {
	    addCreationComment(root);
	}

    }

    /**
     * @return the Schematron document that applies to this XSD document; can be
     *         <code>null</code> and can be the common Schematron document (if
     *         Schematron segmentation was not configured)
     */
    public SchematronSchema getSchematronDocument() {
	return this.schDoc;
    }

    /**
     * Add attribute to an element
     * 
     * @param e     tbd
     * @param name  tbd
     * @param value tbd
     */
    protected void addAttribute(Element e, String name, String value) {
	try {
	    Attr att = document.createAttribute(name);
	    att.setValue(value);
	    e.setAttributeNode(att);
	} catch (DOMException ex) {
	    result.addError(this, 181, name, value, StringUtils.isNotBlank(ex.getMessage()) ? ex.getMessage() : "<NA>");
	}
    }

    /**
     * Add a comment
     * 
     * @param e tbd
     * @return tbd
     */
    protected Comment addCreationComment(Element e) {
	Comment e1 = document.createComment("XML Schema document created by ShapeChange - http://shapechange.net/");
	e.appendChild(e1);
	return e1;
    }

    /**
     * Add documentation and annotation to an element
     * 
     * @param e    tbd
     * @param info tbd
     * 
     * @return the annotation element, if one was created, else <code>null</code>
     */
    protected Element addStandardAnnotation(Element e, Info info) {

	// documentation
	Element e1 = null;
	String txt;
	if (!info.matches("rule-xsd-all-no-documentation")) {
	    if (documentationTemplate != null) {
		/*
		 * Only use the derived documentation, if a template has been specified
		 * explicitly, otherwise use the old style for backwards compatibility
		 */
		txt = info.derivedDocumentation(documentationTemplate, documentationNoValue);
	    } else {
		/*
		 * Use the fixed documentation template from v2.0.1 and earlier.
		 */
		txt = info.definition();
		if (txt != null) {
		    // trim documentation and normalize line breaks
		    txt = txt.trim();
		    txt = txt.replaceAll("\r\n", "\n");
		    txt = txt.replaceAll("\r", "\n");
		    if (!txt.isEmpty()) {
			String s = info.description();
			if (s != null && !s.isEmpty()) {
			    s = s.trim();
			    s = s.replaceAll("\r\n", "\n");
			    s = s.replaceAll("\r", "\n");
			    txt += "\nNOTE " + s;
			}
			s = info.aliasName();
			if (s != null && !s.trim().isEmpty()) {
			    s = s.trim();
			    s = s.replaceAll("\r\n", "\n");
			    s = s.replaceAll("\r", "\n");
			    txt = s + ": " + txt;
			}
		    }
		}
	    }
	    if (txt != null && !txt.trim().isEmpty()) {
		e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "documentation");
		txt = options.internalize(txt);
		e1.appendChild(document.createTextNode(txt));
	    }
	}

	Element e2 = null;
	if (info instanceof PackageInfo) {
	    PackageInfo pi = (PackageInfo) info;
	    if (info.matches("rule-xsd-pkg-gmlProfileSchema")
		    && options.GML_NS.equals("http://www.opengis.net/gml/3.2")) {
		String profile = pi.gmlProfileSchema();
		if (profile != null && !profile.trim().isEmpty()) {
		    if (e2 == null)
			e2 = document.createElementNS(Options.W3C_XML_SCHEMA, "appinfo");
		    Element e0 = document.createElementNS(options.GML_NS, "gmlProfileSchema");
		    e2.appendChild(e0);
		    e0.appendChild(document.createTextNode(profile));
		}
	    }

	    if (info.matches("rule-xsd-pkg-dgiwgsp")) {

		String dgiwgComplianceLevel = pi.taggedValue("dgiwgComplianceLevel");
		String dgiwgGMLProfileSchema = pi.taggedValue("dgiwgGMLProfileSchema");

		if (StringUtils.isNotBlank(dgiwgComplianceLevel) && StringUtils.isNotBlank(dgiwgGMLProfileSchema)) {
		    // if (e2==null)
		    e2 = document.createElementNS(Options.W3C_XML_SCHEMA, "appinfo");
		    addAttribute(e2, "source", options.schemaLocationOfNamespace(DGIWGSP_NS));
		    Element e0 = document.createElementNS(DGIWGSP_NS, "ComplianceLevel");
		    e2.appendChild(e0);
		    e0.appendChild(document.createTextNode(dgiwgComplianceLevel));
		    e0 = document.createElementNS(DGIWGSP_NS, "GMLProfileSchema");
		    e2.appendChild(e0);
		    e0.appendChild(document.createTextNode(dgiwgGMLProfileSchema));

		    addImport(DGIWGSP_NSABR, DGIWGSP_NS);
		}
	    }

	    if (info.matches("rule-xsd-pkg-gmlsf")) {

		String gmlsfComplianceLevel = pi.taggedValue("gmlsfComplianceLevel");

		if (StringUtils.isNotBlank(gmlsfComplianceLevel)) {

		    e2 = document.createElementNS(Options.W3C_XML_SCHEMA, "appinfo");
		    addAttribute(e2, "source", options.schemaLocationOfNamespace(GMLSF_NS));
		    Element e0 = document.createElementNS(GMLSF_NS, "ComplianceLevel");
		    e2.appendChild(e0);
		    e0.appendChild(document.createTextNode(gmlsfComplianceLevel));

		    addImport(GMLSF_NSABR, GMLSF_NS);
		}
	    }
	}

	if ((info instanceof PropertyInfo || info instanceof ClassInfo)
		&& StringUtils.isNotBlank(info.globalIdentifier())
		&& info.matches("rule-xsd-all-globalIdentifierAnnotation")) {

	    if (e2 == null)
		e2 = document.createElementNS(Options.W3C_XML_SCHEMA, "appinfo");
	    Element e3 = document.createElementNS(Options.SCAI_NS, Options.SCAI_NS_PREFIX + ":globalIdentifier");
	    e2.appendChild(e3);
	    e3.appendChild(document.createTextNode(info.globalIdentifier()));
	    addImport(Options.SCAI_NS_PREFIX, Options.SCAI_NS);
	}

	if (info instanceof PropertyInfo) {
	    PropertyInfo propi = (PropertyInfo) info;
	    ClassInfo ci = model.classById(propi.typeInfo().id);
	    if (ci != null) {
		if (info.matches("rule-xsd-prop-targetElement")
			&& options.GML_NS.equals("http://www.opengis.net/gml/3.2")
			&& (propi.inlineOrByReference().equals("byreference")
				|| (ci.category() == Options.OKSTRAFID) && ci.matches("rule-xsd-cls-okstra-fid"))) {
		    // Only add targetElement, if this is not a code list
		    if (ci.category() != Options.CODELIST && classHasObjectElement(ci)) {
			if (e2 == null)
			    e2 = document.createElementNS(Options.W3C_XML_SCHEMA, "appinfo");

			String s = mapElement(ci);
			if (s == null) {
			    s = ci.qname();
			}

			Element e3 = document.createElementNS(options.GML_NS, "targetElement");
			e2.appendChild(e3);
			e3.appendChild(document.createTextNode(s));
		    }
		}
		if (propi.inClass().matches("rule-xsd-cls-codelist-asDictionaryGml33")
			&& (ci.asDictionary() || ci.asDictionaryGml33())) {
		    if (e2 == null)
			e2 = document.createElementNS(Options.W3C_XML_SCHEMA, "appinfo");
		    Element e3 = document.createElementNS(Options.GMLEXR_NS, "targetCodeList");
		    e2.appendChild(e3);
		    e3.appendChild(document.createTextNode(ci.name()));
		    addImport("gmlexr", Options.GMLEXR_NS);
		}
		if (ci.category() == Options.CODELIST && propi.matches("rule-xsd-prop-targetCodeListURI")) {

		    String codeListURI = StringUtils.stripToNull(ci.taggedValue("codeList"));
		    if (codeListURI == null) {
			codeListURI = StringUtils.stripToNull(ci.taggedValue("vocabulary"));
		    }

		    if (codeListURI != null) {

			if (e2 == null)
			    e2 = document.createElementNS(Options.W3C_XML_SCHEMA, "appinfo");
			Element e3 = document.createElementNS(Options.SCAI_NS,
				Options.SCAI_NS_PREFIX + ":targetCodeListURI");
			e2.appendChild(e3);
			e3.appendChild(document.createTextNode(codeListURI));
			addImport(Options.SCAI_NS_PREFIX, Options.SCAI_NS);
		    }
		}
		if (ci.category() == Options.CODELIST && propi.matches("rule-xsd-prop-referencedCodeList")) {

		    String annotationName = options.parameterAsString(XmlSchema.class.getName(),
			    XmlSchemaConstants.PARAM_REF_CODELIST_ANNOTATION_NAME, "referencedCodeList", false, true);
		    String annotationNamespace = options.parameterAsString(XmlSchema.class.getName(),
			    XmlSchemaConstants.PARAM_REF_CODELIST_ANNOTATION_NAMESPACE, Options.W3C_XML_SCHEMA, false,
			    true);
		    String annotationNamespacePrefix = options.nsabrForNamespace(annotationNamespace);

		    if (e2 == null) {
			e2 = document.createElementNS(Options.W3C_XML_SCHEMA, "appinfo");
		    }

		    String annotationQName;
		    if (StringUtils.isNotBlank(annotationNamespacePrefix)
			    && !annotationNamespace.equals(Options.W3C_XML_SCHEMA)) {
			annotationQName = annotationNamespacePrefix + ":" + annotationName;
			addImport(annotationNamespacePrefix, annotationNamespace);
		    } else {
			annotationQName = annotationName;
		    }

		    Element e3 = document.createElementNS(annotationNamespace, annotationQName);
		    e2.appendChild(e3);
		    e3.appendChild(document.createTextNode(ci.qname()));

		    String codelistNsabr = ci.pkg().xmlns();
		    String codelistNs = ci.pkg().targetNamespace();
		    if (StringUtils.isNotBlank(codelistNsabr) && StringUtils.isNotBlank(codelistNs)) {
			addImport(codelistNsabr, codelistNs);
		    }
		}
	    }
	    if (info.matches("rule-xsd-prop-reverseProperty") && options.GML_NS.equals("http://www.opengis.net/gml/3.2")
		    && propi.reverseProperty() != null && propi.reverseProperty().isNavigable()) {
		if (e2 == null)
		    e2 = document.createElementNS(Options.W3C_XML_SCHEMA, "appinfo");
		Element e3 = document.createElementNS(options.GML_NS, "reversePropertyName");
		e2.appendChild(e3);
		e3.appendChild(document.createTextNode(config.determineQName(propi.reverseProperty())));
	    }
	    if (info.matches("rule-xsd-prop-defaultCodeSpace") && !propi.defaultCodeSpace().isEmpty()
		    && options.GML_NS.equals("http://www.opengis.net/gml/3.2")) {
		if (e2 == null)
		    e2 = document.createElementNS(Options.W3C_XML_SCHEMA, "appinfo");
		Element e3 = document.createElementNS(options.GML_NS, "defaultCodeSpace");
		e2.appendChild(e3);
		e3.appendChild(document.createTextNode(propi.defaultCodeSpace()));
	    }
	}

	if (info.matches("rule-xsd-all-tagged-values")) {

	    TaggedValues taggedValues = info.taggedValuesForTagList(options.parameter("representTaggedValues"));

	    if (!taggedValues.isEmpty()) {

		// sort results alphabetically to support unit testing

		// sort by tag name
		TreeSet<String> tags = new TreeSet<String>(taggedValues.keySet());

		for (String tag : tags) {

		    // sort values
		    String[] values = taggedValues.get(tag);
		    List<String> valueList = Arrays.asList(values);
		    Collections.sort(valueList);

		    // add appinfo elements
		    for (String v : values) {
			if (v.trim().length() > 0) {
			    if (e2 == null)
				e2 = document.createElementNS(Options.W3C_XML_SCHEMA, "appinfo");
			    Element e3 = document.createElementNS(Options.SCAI_NS,
				    Options.SCAI_NS_PREFIX + ":taggedValue");
			    addAttribute(e3, "tag", tag);
			    e3.appendChild(document.createTextNode(v));
			    e2.appendChild(e3);
			    addImport(Options.SCAI_NS_PREFIX, Options.SCAI_NS);
			}
		    }
		}
	    }
	}

	if (info.matches("rule-xsd-all-descriptorAnnotation") && !descriptorsToRepresent.isEmpty()) {

	    Descriptors descriptors = info.descriptors();

	    if (!descriptors.isEmpty()) {

		// sort results alphabetically to support unit testing

		for (Descriptor descToRep : descriptorsToRepresent) {

		    if (!descriptors.has(descToRep)) {
			continue;
		    }

		    List<LangString> descVals = descriptors.values(descToRep);
		    Collections.sort(descVals);

		    // add appinfo elements
		    for (LangString ls : descVals) {

			if (ls.hasValue()) {

			    if (e2 == null) {
				e2 = document.createElementNS(Options.W3C_XML_SCHEMA, "appinfo");
			    }

			    Element e3 = document.createElementNS(Options.SCAI_NS,
				    Options.SCAI_NS_PREFIX + ":descriptor");
			    addAttribute(e3, "name", descToRep.getName());
			    if (ls.hasLang()) {
				addAttribute(e3, "lang", ls.getLang());
			    }
			    e3.appendChild(document.createTextNode(ls.getValue()));
			    e2.appendChild(e3);
			    addImport(Options.SCAI_NS_PREFIX, Options.SCAI_NS);
			}
		    }
		}
	    }
	}

	if (e1 != null || e2 != null) {
	    Element e0 = document.createElementNS(Options.W3C_XML_SCHEMA, "annotation");
	    if (e1 != null)
		e0.appendChild(e1);
	    if (e2 != null)
		e0.appendChild(e2);
	    e.appendChild(e0);
	    return e0;
	}

	return null;
    }

    private boolean classHasObjectType(ClassInfo ci) {
	int cat = ci.category();
	return cat == Options.DATATYPE || cat == Options.UNION && !ci.asGroup() || cat == Options.FEATURE
		|| cat == Options.OBJECT || (cat == Options.OKSTRAFID && ci.matches("rule-xsd-cls-okstra-fid"));
    }

    private boolean classHasObjectElement(ClassInfo ci) {
	int cat = ci.category();
	if (ci.matches("rule-xsd-cls-enum-object-element") && (cat == Options.ENUMERATION || cat == Options.CODELIST))
	    return true;
	return classHasObjectType(ci);
    }

    private boolean classHasIdentity(ClassInfo ci) {
	int cat = ci.category();
	return cat == Options.MIXIN || cat == Options.FEATURE || cat == Options.OBJECT
		|| (cat == Options.OKSTRAFID && ci.matches("rule-xsd-cls-okstra-fid"))
		|| ((cat == Options.DATATYPE || cat == Options.UNION)
			&& ci.matches("rule-xsd-cls-standard-swe-property-types"));
    }

    /**
     * Verify QName and add import of namespace
     */
    private String addImport(String qname) {
	result.addDebug(this, 10022, qname);
	String s = qname.trim();
	if (s.isEmpty())
	    return null;

	int idx = s.indexOf(":");
	if (idx > 0) {
	    String nsabr = s.substring(0, idx);
	    addImport(nsabr, options.fullNamespace(nsabr));
	}
	return s;
    }

    /**
     * Map a base type of a class to a predefined representation in GML, ISO/TS
     * 19139, etc.
     * 
     * @param ci tbd
     * @return tbd
     */
    protected String mapBaseType(ClassInfo ci) {
	String s = null;
	MapEntry me = options.baseMapEntry(ci.name(), ci.encodingRule("xsd"));
	if (me == null) {
	    if (classHasObjectType(ci)) {
		s = typeName(ci, true);
	    } else {
		MessageContext mc = result.addError(null, 117, ci.name());
		if (mc != null)
		    mc.addDetail(null, 400, "Class", ci.fullName());
	    }
	} else {
	    s = addImport(me.p1);
	}
	return s;
    }

    /**
     * Map an element to a predefined representation in GML, ISO/TS 19139, etc.
     * 
     * @param ci tbd
     * @return tbd
     */
    protected String mapElement(ClassInfo ci) {
	if (ci == null)
	    return null;
	String s = null;
	MapEntry me = options.elementMapEntry(ci.name(), ci.encodingRule("xsd"));
	if (me == null) {
	    if (classHasObjectElement(ci)) {
		s = elementName(ci, true);
	    } else {
		MessageContext mc = result.addError(this, 119, ci.name());
		if (mc != null)
		    mc.addDetail(null, 400, "Class", ci.fullName());
	    }
	} else {
	    s = addImport(me.p1);
	}
	return s;
    }

    private String elementName(ClassInfo ci, boolean qualified) {
	if (ci == null) {
	    // nothing to do;
	} else if (ci.pkg() == null || ci.pkg().xmlns() == null) {
	    MessageContext mc = result.addError(null, 132, ci.name());
	    if (mc != null)
		mc.addDetail(null, 400, "Class", ci.fullName());
	} else if (ci.matches("rule-xsd-all-naming-19139")) {
	    if (ci.isAbstract())
		return (qualified ? ci.pkg().xmlns() + ":" : "") + "Abstract" + ci.name();
	    else
		return (qualified ? ci.qname() : ci.name());
	} else if (ci.matches("rule-xsd-all-naming-gml") || ci.matches("rule-xsd-all-naming-swe")) {
	    return (qualified ? ci.qname() : ci.name());
	} else {
	    MessageContext mc = result.addError(this, 154, "object element", ci.name());
	    if (mc != null)
		mc.addDetail(null, 400, "Class", ci.fullName());
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
	} else if (ci.matches("rule-xsd-all-naming-19139")) {
	    if (ci.category() == Options.CODELIST) {
		return (qualified ? "gco:CodeListValue_Type" : "CodeListValue_Type");
	    } else if (ci.isAbstract()) {
		return options
			.internalize((qualified ? ci.pkg().xmlns() + ":" : "") + "Abstract" + ci.name() + "_Type");
	    } else {
		return options.internalize((qualified ? ci.qname() : ci.name()) + "_Type");
	    }
	} else if (ci.matches("rule-xsd-all-naming-gml") || ci.matches("rule-xsd-all-naming-swe")) {
	    return options.internalize(
		    (qualified ? ci.qname() : ci.name()) + (ci.name().endsWith("Property") ? "_" : "") + "Type");
	} else {
	    MessageContext mc = result.addError(this, 154, "type", ci.name());
	    if (mc != null)
		mc.addDetail(null, 400, "Class", ci.fullName());
	}
	return null;
    }

    private String propertyTypeName(ClassInfo ci, boolean qualified) {
	if (ci == null) {
	    // nothing to do;
	} else if (ci.pkg() == null || ci.pkg().xmlns() == null) {
	    MessageContext mc = result.addError(null, 132, ci.name());
	    if (mc != null)
		mc.addDetail(null, 400, "Class", ci.fullName());
	} else if (ci.matches("rule-xsd-all-naming-19139")) {
	    if (ci.isAbstract()) {
		return (qualified ? ci.pkg().xmlns() + ":" : "") + "Abstract" + ci.name() + "_PropertyType";
	    } else {
		return (qualified ? ci.qname() : ci.name()) + "_PropertyType";
	    }
	} else if (ci.matches("rule-xsd-all-naming-gml") || ci.matches("rule-xsd-all-naming-swe")) {
	    String propertyTypeName = (qualified ? ci.qname() : ci.name()) + (ci.name().endsWith("Property") ? "_" : "")
		    + "PropertyType";
	    propertyTypeName = options.internalize(propertyTypeName);
	    return propertyTypeName;
	} else {
	    MessageContext mc = result.addError(this, 154, "property type", ci.name());
	    if (mc != null)
		mc.addDetail(null, 400, "Class", ci.fullName());
	}
	return null;
    }

    private String defaultSubstitutionGroup(ClassInfo ci) {
	int cat = ci.category();
	if (ci.matches("rule-xsd-cls-no-gml-types")) {
	    return null;
	} else if (ci.matches("rule-xsd-all-naming-gml")) {
	    addImport("gml", options.fullNamespace("gml"));
	    if (cat == Options.OBJECT) {
		if (options.GML_NS.equals("http://www.opengis.net/gml"))
		    if (ci.matches("rule-xsd-all-gml21")) {
			return "gml:_Feature";
		    } else {
			return "gml:_GML";
		    }
		else if (options.GML_NS.equals("http://www.opengis.net/gml/3.2"))
		    return "gml:AbstractGML";
	    } else if (cat == Options.DATATYPE || cat == Options.UNION) {
		if (options.GML_NS.equals("http://www.opengis.net/gml/3.2"))
		    return "gml:AbstractObject";
		else if (options.GML_NS.equals("http://www.opengis.net/gml")) {

		    /*
		     * GML 2.1 and 3.1 have the same namespace. With a special rule we can let
		     * ShapeChange know that a GML 2.1 encoding is desired. GML 2.1 does not define
		     * a common base type for datatypes/unions (3.1 does so with gml:_Object). In a
		     * GML 2.1 encoding we do not assign the class to a substitution group.
		     */
		    if (ci.matches("rule-xsd-all-gml21")) {
			return null;
		    } else {
			return "gml:_Object";
		    }
		}
	    } else if (cat == Options.FEATURE) {
		if (options.GML_NS.equals("http://www.opengis.net/gml"))
		    return "gml:_Feature";
		else if (options.GML_NS.equals("http://www.opengis.net/gml/3.2"))
		    return "gml:AbstractFeature";
	    }
	} else if (ci.matches("rule-xsd-all-naming-swe")) {
	    addImport("swe", options.fullNamespace("swe"));
	    if (cat == Options.OBJECT) {
		return "swe:AbstractSWE";
	    }
	} else if (ci.matches("rule-xsd-all-naming-19139") && cat != Options.UNION) {
	    addImport("gco", options.fullNamespace("gco"));
	    if (cat == Options.CODELIST || cat == Options.ENUMERATION) {
		return "gco:CharacterString";
	    } else {
		return "gco:AbstractObject";
	    }
	}
	return null;
    }

    private String defaultBaseType(ClassInfo ci) {
	int cat = ci.category();
	if (ci.matches("rule-xsd-cls-no-gml-types")) {
	    return null;
	} else if (ci.matches("rule-xsd-all-naming-gml")) {
	    addImport("gml", options.fullNamespace("gml"));
	    if (cat == Options.OBJECT) {
		if (options.GML_NS.equals("http://www.opengis.net/gml"))
		    if (ci.matches("rule-xsd-all-gml21")) {
			return "gml:AbstractFeatureType";
		    } else {
			return "gml:AbstractGMLType";
		    }
		else if (options.GML_NS.equals("http://www.opengis.net/gml/3.2"))
		    return "gml:AbstractGMLType";
	    } else if (cat == Options.FEATURE) {
		if (options.GML_NS.equals("http://www.opengis.net/gml"))
		    return "gml:AbstractFeatureType";
		else if (options.GML_NS.equals("http://www.opengis.net/gml/3.2"))
		    return "gml:AbstractFeatureType";
	    }
	} else if (ci.matches("rule-xsd-all-naming-swe")) {
	    addImport("swe", options.fullNamespace("swe"));
	    if (cat == Options.OBJECT) {
		return "swe:AbstractSWEType";
	    }
	} else if (ci.matches("rule-xsd-all-naming-19139") && cat != Options.UNION) {
	    addImport("gco", options.fullNamespace("gco"));
	    return "gco:AbstractObject_Type";
	}
	return null;
    }

    public XmlEncodingInfos getXmlEncodingInfos() {

	String targetNamespaceLocation = options.schemaLocationOfNamespace(targetNamespace);
	this.encodingInfos.addNamespace(targetNamespaceAbbreviation, targetNamespace, targetNamespaceLocation);

	Set<String> usedXmlNamespaces = this.encodingInfos.getModelElementEncodings().stream()
		.map(mexe -> mexe.getXmlNamespace()).collect(Collectors.toSet());

	for (String namespace : usedXmlNamespaces) {
	    if (namespace.equals(targetNamespace)) {
		continue;
	    }
	    String location = options.schemaLocationOfNamespace(namespace);
	    String nsabr = options.nsabrForNamespace(namespace);
	    this.encodingInfos.addNamespace(nsabr, namespace, location);
	}
//	for (String namespace : this.imports) {
//	    String location = options.schemaLocationOfNamespace(namespace);
//	    String nsabr = options.nsabrForNamespace(namespace);
//	    this.encodingInfos.addNamespace(nsabr, namespace, location);
//	}

	return this.encodingInfos;
    }

    /**
     * Create global element for an object / data type instance
     * 
     * @param ci     tbd
     * @param cibase tbd
     */
    public void pObjectElement(ClassInfo ci, ClassInfo cibase) {

	if (ci.pkg() == null || ci.pkg().xmlns() == null) {
	    MessageContext mc = result.addError(null, 132, ci.name());
	    if (mc != null)
		mc.addDetail(null, 400, "Class", ci.fullName());
	    return;
	}

	Element e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "element");
	document.getDocumentElement().appendChild(e4);
	addAttribute(e4, "name", elementName(ci, false));
	addAttribute(e4, "type", typeName(ci, true));

	if (writeXmlEncodingInfos) {
	    encodingInfos.add(ci, elementName(ci, false), this.targetNamespace);
	}

	if (ci.isAbstract())
	    addAttribute(e4, "abstract", "true");

	if (cibase == null) {
	    String s = defaultSubstitutionGroup(ci);
	    if (s != null)
		addAttribute(e4, "substitutionGroup", s);
	} else {
	    if (classHasObjectElement(cibase)) {
		String s = mapElement(cibase);
		if (s != null) {
		    addAttribute(e4, "substitutionGroup", s);
		    addImport(cibase.pkg().xmlns(), cibase.pkg().targetNamespace());
		}
	    }
	}

	addStandardAnnotation(e4, ci);
    };

    /**
     * complexType name='[ci.name()+"Type"]' abstract='[ci.isAbstract()]'
     * 
     * @param ci     tbd
     * @param cibase tbd
     * @param schDoc tbd
     * @return tbd
     */
    public Element pComplexType(ClassInfo ci, ClassInfo cibase, SchematronSchema schDoc) {

	int cat = ci.category();

	Element e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "complexType");
	document.getDocumentElement().appendChild(e1);
	addAttribute(e1, "name", typeName(ci, false));

	if (ci.isAbstract())
	    addAttribute(e1, "abstract", "true");

	Element e3;
	if (cibase == null && (cat == Options.DATATYPE || cat == Options.UNION)
		&& ci.matches("rule-xsd-all-naming-gml")) {
	    e3 = e1;
	} else {
	    String s = null;
	    if (cibase == null) {
		s = defaultBaseType(ci);
	    } else {
		s = mapBaseType(cibase);
		if (s == null) {
		    result.addError(this, 158);
		    s = "fixme:fixme";
		}
		addImport(cibase.pkg().xmlns(), cibase.pkg().targetNamespace());
	    }
	    if (s != null) {
		Element e2 = document.createElementNS(Options.W3C_XML_SCHEMA, "complexContent");
		e1.appendChild(e2);
		e3 = document.createElementNS(Options.W3C_XML_SCHEMA, "extension");
		e2.appendChild(e3);
		addAttribute(e3, "base", s);
	    } else if (cibase != null) {
		MessageContext mc = result.addError(null, 121, (cibase == null ? "<unknown>" : cibase.name()),
			typeName(ci, false));
		if (mc != null)
		    mc.addDetail(null, 400, "Class", (ci == null ? "<unknown>" : ci.fullName()));
		e3 = e1;
	    } else
		e3 = e1;
	}

	Element ret = null;
	if (cat == Options.UNION && ci.matches("rule-xsd-cls-union-as-choice")) {
	    /*
	     * 2018-08-02 JE: If the union is a subtype of another union, then we want to
	     * avoid adding an empty choice, which would occur if the ci union did not
	     * define any property itself. Otherwise, an empty choice would lead to an
	     * unsolvable situation and validation would never succeed.
	     * 
	     * A test showed that if the subtype union defines one or more attributes and a
	     * choice for these options is created, then an XML validator expects that one
	     * element from the choice of the supertype union is encoded in XML, followed by
	     * an element from the choice of the subtype union. That wouldn't make sense
	     * from a modeling perspective (only one option shall be encoded).
	     */
	    if (cibase == null && ci.properties().size() > 0) {
		/*
		 * The union defines attributes and has no supertype. So create a <choice/>.
		 */
		ret = document.createElementNS(Options.W3C_XML_SCHEMA, "choice");

	    } else if (ci.properties().size() > 0 && cibase.properties().size() > 0) {
		/*
		 * Both supertype and subtype union define attributes. This must not be encoded
		 * with an additional <choice/> for the subtype union. Report an error.
		 */
		MessageContext mc = result.addError(this, 2000, ci.name(), cibase.name());
		if (mc != null) {
		    mc.addDetail(null, 400, "Class", ci.fullName());
		}
	    } else {
		/*
		 * We have a subtype union that does not define any attributes; just ignore the
		 * choice.
		 */
	    }

	} else if (ci.matches("rule-xsd-cls-sequence")) {
	    ret = document.createElementNS(Options.W3C_XML_SCHEMA, "sequence");
	    if (ci.matches("rule-xsd-cls-mixin-classes")) {
		if (ci.matches("rule-xsd-cls-mixin-classes-as-group"))
		    addGroupReferences(ci, ret, false);
		else
		    addMixinProperties(ci, ret, schDoc);
	    }
	} else {
	    MessageContext mc = result.addError(this, 155, ci.name());
	    if (mc != null)
		mc.addDetail(null, 400, "Class", ci.fullName());
	    ret = document.createElementNS(Options.W3C_XML_SCHEMA, "sequence");
	}
	if (ret != null) {
	    e3.appendChild(ret);
	}

	if (ci.baseClass() != null && ci.matches("rule-xsd-cls-standard-19139-isoType")) {

	    ClassInfo baseType = ci.baseClass();

	    /*
	     * Check if the base type is mapped to one of the ISO 19139 namespaces. If so,
	     * identify the element name to which the base type is mapped, and add
	     * a @gco:isoType to the content.
	     */
	    MapEntry me = options.elementMapEntry(baseType.name(), baseType.encodingRule("xsd"));

	    if (me != null) {

		String xmlElement = me.p1;

		// determine the namespace
		String s = xmlElement.trim();
		if (!s.isEmpty()) {

		    int idx = s.indexOf(":");
		    if (idx > 0) {
			String nsabr = s.substring(0, idx);
			String ns = options.fullNamespace(nsabr);

			/*
			 * NOTE: ns == null can occur if the map entry is defined for one of the types
			 * from the application schemas in the model, and no namespace mapping is
			 * defined in the XML Schema target for the targetNamespace of the application
			 * schema of the type.
			 */
			if (ns == null) {

			    /*
			     * Get the target namespace of the schema to which the base type belongs.
			     */
			    PackageInfo pkg = ci.model().schemaPackage(baseType);

			    if (pkg != null && pkg.xmlns().equalsIgnoreCase(nsabr)) {
				ns = pkg.targetNamespace();
			    }
			}

			if (ns != null && (ns.equals("http://www.isotc211.org/2005/gco")
				|| ns.equals("http://www.isotc211.org/2005/gmd")
				|| ns.equals("http://www.isotc211.org/2005/gmx")
				|| ns.equals("http://www.isotc211.org/2005/gsr")
				|| ns.equals("http://www.isotc211.org/2005/gss")
				|| ns.equals("http://www.isotc211.org/2005/gts"))) {

			    String metadataBaseTypeElementName = s.substring(idx + 1);

			    if (metadataBaseTypeElementName != null) {

				addImport("gco", options.fullNamespace("gco"));

				Element e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "attribute");
				e3.appendChild(e4);

				addAttribute(e4, "ref", "gco:isoType");
				addAttribute(e4, "use", "required");
				addAttribute(e4, "fixed", metadataBaseTypeElementName);
			    }
			}
		    }
		}
	    }
	}

	return ret;
    };

    public void pValueTypeGroup(ClassInfo ci) {
	Element e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "group");
	document.getDocumentElement().appendChild(e1);
	addAttribute(e1, "name", ci.name());
	addStandardAnnotation(e1, ci);
	Element e2 = document.createElementNS(Options.W3C_XML_SCHEMA, "choice");
	e1.appendChild(e2);

	for (PropertyInfo propi : ci.properties().values()) {
	    ClassInfo vci = model.classById(propi.typeInfo().id);
	    if (vci != null) {
		if (classHasObjectElement(vci)) {
		    String s = mapElement(vci);
		    if (s != null) {
			Element e3 = document.createElementNS(Options.W3C_XML_SCHEMA, "element");
			e2.appendChild(e3);
			addAttribute(e3, "ref", s);
			addImport(vci.pkg().xmlns(), vci.pkg().targetNamespace());
		    } else {
			MessageContext mc = result.addError(this, 166, vci.name(), ci.name());
			if (mc != null)
			    mc.addDetail(null, 400, "Property", propi.fullName());
		    }
		} else {
		    MessageContext mc = result.addError(this, 166, vci.name(), ci.name());
		    if (mc != null)
			mc.addDetail(null, 400, "Property", propi.fullName());
		}
	    } else {
		MessageContext mc = result.addError(null, 131, propi.name(), propi.typeInfo().name);
		if (mc != null)
		    mc.addDetail(null, 400, "Property", propi.fullName());
	    }
	}
    };

    /**
     * group name='[ci.name()+"Group"]'
     * 
     * @param ci     tbd
     * @param cibase tbd
     * @return tbd
     */
    public Element pGroup(ClassInfo ci, ClassInfo cibase) {
	Element e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "group");
	document.getDocumentElement().appendChild(e1);
	addAttribute(e1, "name", ci.name() + "Group");
	addStandardAnnotation(e1, ci);

	Element ret = null;
	if (ci.category() == Options.UNION && ci.matches("rule-xsd-cls-union-as-choice")) {
	    ret = document.createElementNS(Options.W3C_XML_SCHEMA, "choice");
	} else if (ci.category() == Options.UNION && ci.matches("rule-xsd-cls-union-as-group-property-type")) {
	    ret = document.createElementNS(Options.W3C_XML_SCHEMA, "choice");
	} else if (ci.matches("rule-xsd-cls-sequence")) {
	    ret = document.createElementNS(Options.W3C_XML_SCHEMA, "sequence");
	    if (ci.matches("rule-xsd-cls-mixin-classes") && ci.matches("rule-xsd-cls-mixin-classes-as-group")) {
		addGroupReferences(ci, ret, true);
	    }
	} else {
	    MessageContext mc = result.addError(this, 155, ci.name());
	    if (mc != null)
		mc.addDetail(null, 400, "Class", ci.fullName());
	    ret = document.createElementNS(Options.W3C_XML_SCHEMA, "sequence");
	}
	e1.appendChild(ret);

	return ret;
    };

    private void addGroupReferences(ClassInfo ci, Element e, boolean recursive) {

	SortedSet<String> st = ci.supertypes();
	if (st != null && ci.matches("rule-xsd-cls-mixin-classes")
		&& ci.matches("rule-xsd-cls-mixin-classes-as-group")) {
	    for (String sid : st) {
		ClassInfo cix = model.classById(sid);
		if (cix != null && (cix.category() == Options.MIXIN
			|| (cix.matches("rule-xsd-cls-no-abstract-classes") && cix.isAbstract()))) {
		    Element eg = document.createElementNS(Options.W3C_XML_SCHEMA, "group");
		    e.appendChild(eg);
		    addAttribute(eg, "ref", cix.qname() + "Group");
		    if (recursive)
			addGroupReferences(cix, e, true);
		} else if (cix != null && cix.category() != Options.MIXIN
			&& ci.matches("rule-xsd-cls-mixin-classes-non-mixin-supertypes") && recursive) {
		    processLocalProperties(cix, e, null);
		    if (recursive)
			addGroupReferences(cix, e, true);
		}
	    }
	}
    };

    private void addMixinProperties(ClassInfo ci, Element e, SchematronSchema schDoc) {

	SortedSet<String> st = ci.supertypes();
	if (st != null && ci.matches("rule-xsd-cls-mixin-classes")
		&& !ci.matches("rule-xsd-cls-mixin-classes-as-group")) {
	    for (String sid : st) {
		ClassInfo cix = model.classById(sid);
		if (cix != null && (cix.category() == Options.MIXIN
			|| (cix.matches("rule-xsd-cls-no-abstract-classes") && cix.isAbstract()))) {
		    processLocalProperties(cix, e, schDoc);
		    addMixinProperties(cix, e, schDoc);
		}
	    }
	}
    };

    /**
     * Walk down the subtype tree to find the first instantiable types and list
     * their element.
     */
    private void addElements(Element e, HashSet<ClassInfo> v) {
	ClassInfo[] clArr = new ClassInfo[v.size()];
	v.toArray(clArr);
	Arrays.sort(clArr, new Comparator<ClassInfo>() {
	    public int compare(ClassInfo ci1, ClassInfo ci2) {
		return ci1.name().compareTo(ci2.name());
	    }
	});
	for (int cidx = 0; cidx < clArr.length; cidx++) {
	    ClassInfo ci = clArr[cidx];
	    String s = mapElement(ci);
	    if (s != null) {
		Element e3 = document.createElementNS(Options.W3C_XML_SCHEMA, "element");
		e.appendChild(e3);
		addAttribute(e3, "ref", s);
		addImport(ci.pkg().xmlns(), ci.pkg().targetNamespace());
	    }
	}
    }

    /**
     * Walk down the subtype tree to find the first instantiable types
     */
    private HashSet<ClassInfo> subtypes(ClassInfo ci) {
	HashSet<ClassInfo> res = new HashSet<ClassInfo>();
	if (ci.subtypes() != null) {
	    for (String subtypeId : ci.subtypes()) {
		ClassInfo cix = model.classById(subtypeId);
		if (cix != null && (cix.category() == Options.MIXIN
			|| (cix.matches("rule-xsd-cls-no-abstract-classes") && cix.isAbstract()))) {
		    res.addAll(subtypes(cix));
		} else {
		    res.add(cix);
		}
	    }
	}
	return res;
    }

    /**
     * Walk down the subtype tree to find the first instantiable types for any mixin
     * subtype
     * 
     * @return can be empty but not <code>null</code>
     */
    private HashSet<ClassInfo> subtypesOfMixins(ClassInfo ci, boolean inMixin) {
	HashSet<ClassInfo> res = new HashSet<ClassInfo>();
	if (ci.subtypes() != null) {
	    for (String subtypeId : ci.subtypes()) {
		ClassInfo cix = model.classById(subtypeId);
		if (cix != null && (cix.category() == Options.MIXIN
			|| (cix.matches("rule-xsd-cls-no-abstract-classes") && cix.isAbstract()))) {
		    res.addAll(subtypesOfMixins(cix, true));
		} else {
		    if (inMixin)
			res.add(cix);
		    else
			res.addAll(subtypesOfMixins(cix, false));
		}
	    }
	}
	return res;
    }

    public void pPropertyTypeWithSubtypes(ClassInfo ci) {

	if (ci.matches("rule-xsd-cls-noPropertyType") && !ci.includePropertyType())
	    return;

	Element e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "complexType");
	document.getDocumentElement().appendChild(e1);
	addAttribute(e1, "name", propertyTypeName(ci, false));
	Element e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "choice");
	e1.appendChild(e4);
	addAttribute(e4, "minOccurs", "0");
	addElements(e4, subtypes(ci));
	e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "attributeGroup");
	e1.appendChild(e4);

	if (ci.matches("rule-xsd-cls-no-gml-types")) {
	    addAttribute(e4, "ref", "xlink:simpleAttrs");
	    addImport("xlink", options.fullNamespace("xlink"));
	} else {
	    addAttribute(e4, "ref", "gml:AssociationAttributeGroup");
	    addImport("gml", options.fullNamespace("gml"));
	    if (options.GML_NS.equals("http://www.opengis.net/gml/3.2")) {
		e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "attributeGroup");
		e1.appendChild(e4);
		addAttribute(e4, "ref", "gml:OwnershipAttributeGroup");
	    }
	}
    }

    public void pPropertyTypes(ClassInfo ci) {

	if (ci.matches("rule-xsd-cls-noPropertyType") && !ci.includePropertyType())
	    return;

	boolean rnobase = ci.matches("rule-xsd-cls-no-gml-types");
	boolean rgml = ci.matches("rule-xsd-cls-standard-gml-property-types");
	boolean r19139 = ci.matches("rule-xsd-cls-standard-19139-property-types");
	boolean rswe = ci.matches("rule-xsd-cls-standard-swe-property-types");

	HashSet<ClassInfo> instatiableMixinSubclasses = null;
	if (ci.matches("rule-xsd-cls-mixin-classes-non-mixin-supertypes")) {
	    instatiableMixinSubclasses = subtypesOfMixins(ci, false);
	}

	Element e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "complexType");
	document.getDocumentElement().appendChild(e1);
	addAttribute(e1, "name", propertyTypeName(ci, false));
	Element e4;
	if (instatiableMixinSubclasses != null && !instatiableMixinSubclasses.isEmpty()) {
	    e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "choice");
	} else {
	    e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "sequence");
	}
	e1.appendChild(e4);
	Element e3 = document.createElementNS(Options.W3C_XML_SCHEMA, "element");
	e4.appendChild(e3);
	String s = elementName(ci, true);
	if (s != null)
	    addAttribute(e3, "ref", s);
	if (instatiableMixinSubclasses != null && !instatiableMixinSubclasses.isEmpty()) {
	    addElements(e4, instatiableMixinSubclasses);
	}
	if (rgml && !rnobase) {
	    if (classHasIdentity(ci)) {
		addAttribute(e4, "minOccurs", "0");
		e3 = document.createElementNS(Options.W3C_XML_SCHEMA, "attributeGroup");
		e1.appendChild(e3);
		addAttribute(e3, "ref", "gml:AssociationAttributeGroup");
		addImport("gml", options.fullNamespace("gml"));
		if (options.GML_NS.equals("http://www.opengis.net/gml/3.2")) {
		    e3 = document.createElementNS(Options.W3C_XML_SCHEMA, "attributeGroup");
		    e1.appendChild(e3);
		    addAttribute(e3, "ref", "gml:OwnershipAttributeGroup");
		}
	    }
	} else if (rswe) {
	    if (classHasIdentity(ci)) {
		addAttribute(e4, "minOccurs", "0");
		e3 = document.createElementNS(Options.W3C_XML_SCHEMA, "attributeGroup");
		e1.appendChild(e3);
		addAttribute(e3, "ref", "swe:AssociationAttributeGroup");
		addImport("swe", options.fullNamespace("swe"));
	    }
	} else if (r19139) {
	    addAttribute(e4, "minOccurs", "0");
	    int cat = ci.category();
	    if (cat != Options.CODELIST && cat != Options.ENUMERATION && cat != Options.UNION) {
		e3 = document.createElementNS(Options.W3C_XML_SCHEMA, "attributeGroup");
		addAttribute(e3, "ref", "gco:ObjectReference");
		e1.appendChild(e3);
	    }
	    addImport("gco", options.fullNamespace("gco"));
	    e3 = document.createElementNS(Options.W3C_XML_SCHEMA, "attribute");
	    e1.appendChild(e3);
	    addAttribute(e3, "ref", "gco:nilReason");
	} else {
	    addAttribute(e4, "minOccurs", "0");
	    e3 = document.createElementNS(Options.W3C_XML_SCHEMA, "attributeGroup");
	    e1.appendChild(e3);
	    addAttribute(e3, "ref", "xlink:simpleAttrs");
	    addImport("xlink", options.fullNamespace("xlink"));
	}

	if (ci.matches("rule-xsd-cls-byValuePropertyType")) {
	    if ((classHasIdentity(ci)) && ci.includeByValuePropertyType()) {
		e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "complexType");
		document.getDocumentElement().appendChild(e1);
		addAttribute(e1, "name", ci.name() + "PropertyByValueType");
		e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "sequence");
		e1.appendChild(e4);
		e3 = document.createElementNS(Options.W3C_XML_SCHEMA, "element");
		e4.appendChild(e3);
		if (ci.pkg() == null || ci.pkg().xmlns() == null) {
		    MessageContext mc = result.addError(null, 132, ci.name());
		    if (mc != null)
			mc.addDetail(null, 400, "Class", ci.fullName());
		} else
		    addAttribute(e3, "ref", ci.qname());
	    }
	}
    };

    public void pPropertyTypeWithGroup(ClassInfo ci) {

	Element e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "complexType");
	document.getDocumentElement().appendChild(e1);
	addAttribute(e1, "name", propertyTypeName(ci, false));
	Element e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "sequence");
	e1.appendChild(e4);
	addAttribute(e4, "minOccurs", "0");
	addElements(e4, subtypes(ci));
	Element e5 = document.createElementNS(Options.W3C_XML_SCHEMA, "group");
	e4.appendChild(e5);
	addAttribute(e5, "ref", ci.qname());

	addAttribute(e4, "minOccurs", "0");
	e5 = document.createElementNS(Options.W3C_XML_SCHEMA, "attributeGroup");
	e1.appendChild(e5);
	addAttribute(e5, "ref", "swe:AssociationAttributeGroup");
	addImport("swe", options.fullNamespace("swe"));
    }

    public void pOKSTRAKEYPropertyType(ClassInfo ci) {

	Element e1, e2, e3, e4, e5, e6, e7, e8, e9, e10;
	e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "complexType");
	document.getDocumentElement().appendChild(e1);
	addAttribute(e1, "name", ci.name() + (ci.name().endsWith("Property") ? "_" : "") + "PropertyType");
	e2 = document.createElementNS(Options.W3C_XML_SCHEMA, "complexContent");
	e1.appendChild(e2);
	e3 = document.createElementNS(Options.W3C_XML_SCHEMA, "extension");
	addAttribute(e3, "base", okstraKeyValuePropertyType);
	addImport(okstraPrefix, options.fullNamespace(okstraPrefix));
	e2.appendChild(e3);
	e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "sequence");
	e3.appendChild(e4);
	e5 = document.createElementNS(Options.W3C_XML_SCHEMA, "element");
	addAttribute(e5, "ref", ci.qname());
	addAttribute(e5, "minOccurs", "0");
	e4.appendChild(e5);

	e5 = document.createElementNS(Options.W3C_XML_SCHEMA, "element");
	addAttribute(e5, "name", ci.name());
	document.getDocumentElement().appendChild(e5);
	e6 = document.createElementNS(Options.W3C_XML_SCHEMA, "complexType");
	e5.appendChild(e6);
	e7 = document.createElementNS(Options.W3C_XML_SCHEMA, "complexContent");
	e6.appendChild(e7);
	e8 = document.createElementNS(Options.W3C_XML_SCHEMA, "extension");
	addAttribute(e8, "base", okstraKeyValueBaseType);
	e7.appendChild(e8);
	e9 = document.createElementNS(Options.W3C_XML_SCHEMA, "sequence");
	e8.appendChild(e9);
	e10 = document.createElementNS(Options.W3C_XML_SCHEMA, "element");
	e9.appendChild(e10);
	addAttribute(e10, "name", "Kennung");
	addAttribute(e10, "type", "string");
	e10 = document.createElementNS(Options.W3C_XML_SCHEMA, "element");
	e9.appendChild(e10);
	addAttribute(e10, "name", "Langtext");
	addAttribute(e10, "type", "string");
    };

    /** create anonymous basic type */
    private Element pAnonymousBasicType(ClassInfo ci) {

	Element e1 = null;
	String id = ci.id();
	if (id != null) {

	    if (ci.matches("rule-xsd-cls-basictype-list") && ci.properties().size() == 1
		    && ci.properties().values().iterator().next().cardinality().maxOccurs > 1) {

		// encode as a list-based simple type
		e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "simpleType");
		addStandardAnnotation(e1, ci);

		PropertyInfo pi = ci.properties().values().iterator().next();
		ClassInfo typeCi = model.classByIdOrName(pi.typeInfo());

		String itemType = null;
		if (typeCi != null) {
		    MapEntry me = options.baseMapEntry(typeCi.name(), ci.encodingRule("xsd"));
		    if (me != null) {
			itemType = me.p1;
			addImport(me.p1);
		    }
		    if (itemType == null) {
			itemType = typeCi.qname() + "Type";
			addImport(typeCi.pkg().xmlns(), typeCi.pkg().targetNamespace());
		    }
		} else {
		    MapEntry me = options.baseMapEntry(pi.typeInfo().name, ci.encodingRule("xsd"));
		    if (me != null) {
			itemType = me.p1;
			addImport(me.p1);
		    } else {
			itemType = "FIXME";
			MessageContext mc = result.addError(this, 2001, pi.typeInfo().name, pi.name(), ci.name());
			if (mc != null)
			    mc.addDetail(null, 400, "Class", ci.fullName());
		    }
		}

		Element eList = document.createElementNS(Options.W3C_XML_SCHEMA, "list");
		addAttribute(eList, "itemType", itemType);

		if (pi.cardinality().minOccurs == 0 && pi.cardinality().maxOccurs == Integer.MAX_VALUE) {

		    // no length restriction
		    e1.appendChild(eList);

		} else {

		    // some length restriction
		    Element eRestrict = document.createElementNS(Options.W3C_XML_SCHEMA, "restriction");
		    e1.appendChild(eRestrict);

		    Element eSimpleTypeChild = document.createElementNS(Options.W3C_XML_SCHEMA, "simpleType");
		    eSimpleTypeChild.appendChild(eList);
		    eRestrict.appendChild(eSimpleTypeChild);

		    if (pi.cardinality().minOccurs == pi.cardinality().maxOccurs) {

			int length = pi.cardinality().minOccurs;
			Element eLength = document.createElementNS(Options.W3C_XML_SCHEMA, "length");
			addAttribute(eLength, "value", "" + length);
			eRestrict.appendChild(eLength);

		    } else {

			if (pi.cardinality().minOccurs > 0) {
			    Element eMinLength = document.createElementNS(Options.W3C_XML_SCHEMA, "minLength");
			    addAttribute(eMinLength, "value", "" + pi.cardinality().minOccurs);
			    eRestrict.appendChild(eMinLength);
			}

			if (pi.cardinality().maxOccurs != Integer.MAX_VALUE) {
			    Element eMaxLength = document.createElementNS(Options.W3C_XML_SCHEMA, "maxLength");
			    addAttribute(eMaxLength, "value", "" + pi.cardinality().maxOccurs);
			    eRestrict.appendChild(eMaxLength);
			}
		    }
		}

	    } else {

		String base = ci.taggedValue("base");
		String length = ci.taggedValue("length");
		if (StringUtils.isBlank(length))
		    length = ci.taggedValue("maxLength");
		String pattern = ci.taggedValue("pattern");
		String min = ci.taggedValue("rangeMinimum");
		String max = ci.taggedValue("rangeMaximum");
		String typecontent = "simple/simple";

		/*
		 * baseType is the simple type that is the foundation of the basic type
		 * implementation; it is either defined directly, via the tagged value "base" of
		 * the type, or indirectly, by a map entry in the supertypes (direct and
		 * indirect) that maps to a simple type with simple content.
		 */
		String baseType = null;

		if (StringUtils.isBlank(base)) {

		    /*
		     * Identify base and type content from the direct supertypes; this is important
		     * for correct declaration of the basic type
		     */
		    if (ci.supertypes() != null) {
			for (String supertypeId : ci.supertypes()) {
			    ClassInfo cix = model.classById(supertypeId);
			    if (cix != null) {
				MapEntry me = options.baseMapEntry(cix.name(), ci.encodingRule("xsd"));
				if (me != null) {
				    base = me.p1;
				    typecontent = me.p2;
				}
				if (base == null) {
				    base = cix.qname() + "Type";
				}
			    }
			}
		    }

		    /*
		     * Identify base type that has xmlTypeType="simple" and xmlTypeContent="simple"
		     */
		    MapEntry me = findBaseMapEntryInSupertypes(ci, ci.encodingRule("xsd"), "simple", "simple");

		    if (me != null) {
			baseType = me.p1;
		    }
		}

		if (baseType == null) {
		    baseType = base;
		}

		if (StringUtils.isNotBlank(base)) {

		    Element e3;
		    Element e4;
		    if (typecontent.equals("complex/simple")) {
			e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "complexType");
			addStandardAnnotation(e1, ci);
			e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "simpleContent");
			e1.appendChild(e4);
			e3 = document.createElementNS(Options.W3C_XML_SCHEMA, "restriction");
			e4.appendChild(e3);
		    } else if (typecontent.equals("simple/simple")) {
			e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "simpleType");
			addStandardAnnotation(e1, ci);
			e3 = document.createElementNS(Options.W3C_XML_SCHEMA, "restriction");
			e1.appendChild(e3);
		    } else {
			e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "complexType");
			addStandardAnnotation(e1, ci);
			e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "complexContent");
			e1.appendChild(e4);
			e3 = document.createElementNS(Options.W3C_XML_SCHEMA, "extension");
			e4.appendChild(e3);
		    }
		    addAttribute(e3, "base", base);
		    if (facetSupported("totalDigits", baseType) && StringUtils.isNotBlank(length)) {
			Element e5 = document.createElementNS(Options.W3C_XML_SCHEMA, "totalDigits");
			e3.appendChild(e5);
			addAttribute(e5, "value", length);
		    }
		    if (facetSupported("maxLength", baseType) && StringUtils.isNotBlank(length)) {
			Element e5 = document.createElementNS(Options.W3C_XML_SCHEMA, "maxLength");
			e3.appendChild(e5);
			addAttribute(e5, "value", length);
		    }
		    if (facetSupported("pattern", baseType) && StringUtils.isNotBlank(pattern)) {
			Element e5 = document.createElementNS(Options.W3C_XML_SCHEMA, "pattern");
			e3.appendChild(e5);
			addAttribute(e5, "value", pattern);
		    }
		    if (facetSupported("minInclusive", baseType) && StringUtils.isNotBlank(min)) {
			Element e5 = document.createElementNS(Options.W3C_XML_SCHEMA, "minInclusive");
			e3.appendChild(e5);
			addAttribute(e5, "value", min);
		    }
		    if (facetSupported("maxInclusive", baseType) && StringUtils.isNotBlank(max)) {
			Element e5 = document.createElementNS(Options.W3C_XML_SCHEMA, "maxInclusive");
			e3.appendChild(e5);
			addAttribute(e5, "value", max);
		    }
		} else {
		    MessageContext mc = result.addError(this, 122, ci.name());
		    if (mc != null)
			mc.addDetail(null, 400, "Class", ci.fullName());
		}
	    }
	} else {
	    MessageContext mc = result.addError(this, 123, ci.name());
	    if (mc != null)
		mc.addDetail(null, 400, "Class", ci.fullName());
	}

	return e1;
    }

    /**
     * Search for a MapEntry that provides the base of the given class, with the
     * given type and content definition. The map entry must be defined for one of
     * the direct or indirect supertypes of the class. NOTE: a tagged value 'base'
     * that may be defined on the supertype is currently ignored.
     * 
     * @param ci
     * @param encodingRule
     * @param xmlTypeType
     * @param xmlTypeContent
     * @return the base MapEntry that matches the search criteria, or
     *         <code>null</code> if none was found
     */
    private MapEntry findBaseMapEntryInSupertypes(ClassInfo ci, String encodingRule, String xmlTypeType,
	    String xmlTypeContent) {

	MapEntry me = null;

	if (ci.supertypes() != null) {

	    for (String supertypeId : ci.supertypes()) {

		ClassInfo cix = model.classById(supertypeId);

		if (cix != null) {
		    MapEntry mex = options.baseMapEntry(cix.name(), encodingRule, xmlTypeType, xmlTypeContent);
		    if (mex == null) {
			// search in supertypes of supertype
			mex = findBaseMapEntryInSupertypes(cix, encodingRule, xmlTypeType, xmlTypeContent);
		    }
		    if (mex != null) {
			me = mex;
		    }
		}
	    }
	}

	return me;
    }

    private boolean facetSupported(String facet, String base) {
	if (facet.equals("minInclusive") || facet.equals("maxInclusive") || facet.equals("minExclusive")
		|| facet.equals("maxExclusive")) {
	    if (base.equals("integer"))
		return true;
	    else if (base.equals("positiveInteger"))
		return true;
	    else if (base.equals("negativeInteger"))
		return true;
	    else if (base.equals("nonNegativeInteger"))
		return true;
	    else if (base.equals("nonPositiveInteger"))
		return true;
	    else if (base.equals("long"))
		return true;
	    else if (base.equals("unsignedLong"))
		return true;
	    else if (base.equals("int"))
		return true;
	    else if (base.equals("unsignedInt"))
		return true;
	    else if (base.equals("short"))
		return true;
	    else if (base.equals("unsignedShort"))
		return true;
	    else if (base.equals("byte"))
		return true;
	    else if (base.equals("unsignedByte"))
		return true;
	    else if (base.equals("decimal"))
		return true;
	    else if (base.equals("float"))
		return true;
	    else if (base.equals("double"))
		return true;
	    else if (base.equals("duration"))
		return true;
	    else if (base.equals("dateTime"))
		return true;
	    else if (base.equals("date"))
		return true;
	    else if (base.equals("time"))
		return true;
	    else if (base.equals("gYear"))
		return true;
	    else if (base.equals("gYearMonth"))
		return true;
	    else if (base.equals("gMonth"))
		return true;
	    else if (base.equals("gMonthDay"))
		return true;
	    else if (base.equals("gDay"))
		return true;
	}
	if (facet.equals("totalDigits")) {
	    if (base.equals("integer"))
		return true;
	    else if (base.equals("positiveInteger"))
		return true;
	    else if (base.equals("negativeInteger"))
		return true;
	    else if (base.equals("nonNegativeInteger"))
		return true;
	    else if (base.equals("nonPositiveInteger"))
		return true;
	    else if (base.equals("long"))
		return true;
	    else if (base.equals("unsignedLong"))
		return true;
	    else if (base.equals("int"))
		return true;
	    else if (base.equals("unsignedInt"))
		return true;
	    else if (base.equals("short"))
		return true;
	    else if (base.equals("unsignedShort"))
		return true;
	    else if (base.equals("byte"))
		return true;
	    else if (base.equals("unsignedByte"))
		return true;
	    else if (base.equals("decimal"))
		return true;
	}
	if (facet.equals("maxLength")) {
	    if (base.equals("string"))
		return true;
	    else if (base.equals("normalizedString"))
		return true;
	    else if (base.equals("token"))
		return true;
	    else if (base.equals("base64Binary"))
		return true;
	    else if (base.equals("hexBinary"))
		return true;
	    else if (base.equals("Name"))
		return true;
	    else if (base.equals("QName"))
		return true;
	    else if (base.equals("NCName"))
		return true;
	    else if (base.equals("anyURI"))
		return true;
	    else if (base.equals("language"))
		return true;
	    else if (base.equals("ID"))
		return true;
	    else if (base.equals("IDREF"))
		return true;
	    else if (base.equals("IDREFS"))
		return true;
	    else if (base.equals("ENTITY"))
		return true;
	    else if (base.equals("ENTITIES"))
		return true;
	    else if (base.equals("NOTATION"))
		return true;
	    else if (base.equals("NMTOKEN"))
		return true;
	    else if (base.equals("NMTOKENS"))
		return true;
	}
	if (facet.equals("pattern"))
	    return true;
	// if we have a non-XSD base type we cannot check if the facet is
	// appropriate
	if (base.contains(":"))
	    return true;
	return false;
    }

    public void pGlobalBasicType(ClassInfo ci) {
	Element e1 = pAnonymousBasicType(ci);
	if (e1 != null) {
	    document.getDocumentElement().appendChild(e1);
	    addAttribute(e1, "name", typeName(ci, false));
	} else {
	    MessageContext mc = result.addError(this, 124, typeName(ci, false));
	    if (mc != null)
		mc.addDetail(null, 400, "Class", ci.fullName());
	}
    }

    /** map to enumeration type */
    private Element pAnonymousEnumeration(ClassInfo ci) {
	Element e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "simpleType");
	addStandardAnnotation(e1, ci);
	Element e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "restriction");
	e1.appendChild(e4);
	addAttribute(e4, "base", "string");

	LinkedList<ClassInfo> classes = new LinkedList<ClassInfo>();
	classes.add(ci);

	LinkedList<ClassInfo> tasks = new LinkedList<ClassInfo>();
	if (ci.matches("rule-xsd-cls-enum-supertypes")) { // FIXME still used by
							  // anyone?
	    // First add superclasses, which are enums or codelists
	    tasks.add(ci);
	    while (tasks.size() > 0) {
		ClassInfo cci = tasks.removeFirst();
		SortedSet<String> sids = cci.supertypes();
		if (sids != null) {
		    for (String id : sids) {
			ClassInfo nci = model.classById(id);
			if (nci != null) {
			    int cat = nci.category();
			    if (cat == Options.ENUMERATION || cat == Options.CODELIST) {
				tasks.addLast(nci);
				classes.addFirst(nci);
			    }
			}
		    }
		}
	    }
	}
	if (ci.matches("rule-xsd-cls-enum-subtypes")) { // FIXME still used by
							// anyone?
	    // Now follow the enum/codelist subclasses
	    tasks.add(ci);
	    while (tasks.size() > 0) {
		ClassInfo cci = tasks.removeFirst();
		SortedSet<String> sids = cci.subtypes();
		if (sids != null) {
		    for (String id : sids) {
			ClassInfo nci = model.classById(id);
			if (nci != null) {
			    int cat = nci.category();
			    if (cat == Options.ENUMERATION || cat == Options.CODELIST) {
				tasks.addLast(nci);
				classes.addLast(nci);
			    }
			}
		    }
		}
	    }
	}

	// Now loop over the list of classes and collect all names
	for (ClassInfo cci : classes) {
	    for (PropertyInfo atti : cci.properties().values()) {
		Element e3 = document.createElementNS(Options.W3C_XML_SCHEMA, "enumeration");
		e4.appendChild(e3);
		String val = atti.name();
		if (atti.initialValue() != null) {
		    val = atti.initialValue();
		}
		addAttribute(e3, "value", val);
		addStandardAnnotation(e3, atti);
	    }
	}

	return e1;
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
    };

    /**
     * Create code list encoding according to standard GML 3.2 encoding rule (union
     * between enumeration and other-pattern)
     * 
     * @param ci the code list class
     */
    public void pGlobalCodeList(ClassInfo ci) {
	Element e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "simpleType");
	document.getDocumentElement().appendChild(e1);
	addAttribute(e1, "name", typeName(ci, false));

	addStandardAnnotation(e1, ci);

	Element e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "union");
	e1.appendChild(e4);
	addAttribute(e4, "memberTypes", ci.qname() + "EnumerationType " + ci.qname() + "OtherType");

	// Here is where it happens :-) ...
	e1 = pAnonymousEnumeration(ci);
	if (e1 != null) {
	    document.getDocumentElement().appendChild(e1);
	    addAttribute(e1, "name", ci.name() + "EnumerationType");
	} else {
	    MessageContext mc = result.addError(this, 156, ci.name());
	    if (mc != null)
		mc.addDetail(null, 400, "Class", ci.fullName());
	}

	e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "simpleType");
	document.getDocumentElement().appendChild(e1);
	addAttribute(e1, "name", ci.name() + "OtherType");
	e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "restriction");
	e1.appendChild(e4);
	addAttribute(e4, "base", "string");
	Element e3 = document.createElementNS(Options.W3C_XML_SCHEMA, "pattern");
	e4.appendChild(e3);
	addAttribute(e3, "value", "other: \\w{2,}");
    };

    private boolean includeProperty(PropertyInfo pi) {

	XsdPropertyMapEntry pme = config.getPropertyMapEntry(pi);

	return pi.isNavigable() && !pi.isRestriction()
		&& (!pi.matches("rule-xsd-prop-exclude-derived") || !pi.isDerived())
		&& !(pi.matches("rule-xsd-all-notEncoded") && pi.encodingRule("xsd").equalsIgnoreCase("notencoded"))
		&& (pme == null || pme.hasTargetElement());
    }

    /**
     * Process a class property. "true" is returned, if the property is an
     * aggregation/composition, "false" otherwise
     * 
     * @param ci               tbd
     * @param pi               tbd
     * @param sequenceOrChoice tbd
     * @param m                tbd
     * @param schDoc           tbd
     * @return tbd
     */
    public boolean processLocalProperty(ClassInfo ci, PropertyInfo pi, Element sequenceOrChoice, Multiplicity m,
	    SchematronSchema schDoc) {

	if (includeProperty(pi)) {
	    Element property = addProperty(ci, pi, m, schDoc);
	    if (property.getLocalName().equals("attribute") || property.getLocalName().equals("attributeGroup")) {
		sequenceOrChoice.getParentNode().appendChild(property);
	    } else {
		sequenceOrChoice.appendChild(property);
	    }
	    return (pi.isAggregation() || pi.isComposition());
	}
	return false;
    }

    /**
     * Process all properties that are added in this class. "true" is returned if a
     * single property is an aggregation/composition, "false" otherwise
     * 
     * @param ci               tbd
     * @param sequenceOrChoice tbd
     * @param schDoc           tbd
     * @return tbd
     */
    public boolean processLocalProperties(ClassInfo ci, Element sequenceOrChoice, SchematronSchema schDoc) {

	int res = 0;
	result.addDebug(null, 10023, ci.name());

	for (PropertyInfo pi : ci.properties().values()) {
	    if (processLocalProperty(ci, pi, sequenceOrChoice, null, schDoc))
		res++;
	}

	AssociationInfo ai = ci.isAssocClass();
	if (ai != null && ai.matches("rule-xsd-rel-association-classes")) {
	    Multiplicity m = new Multiplicity();
	    if (processLocalProperty(ci, ai.end1(), sequenceOrChoice, m, schDoc))
		res++;
	    if (processLocalProperty(ci, ai.end2(), sequenceOrChoice, m, schDoc))
		res++;
	}

	if (ci.matches("rule-xsd-cls-adehook")) {

	    // Name of the ADE hook
	    String elementName = "AbstractGenericApplicationPropertyOf" + ci.name();

	    // add property
	    Element e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "element");
	    addAttribute(e1, "ref", elementName);
	    addMinMaxOccurs(e1, new Multiplicity("0..*"));
	    sequenceOrChoice.appendChild(e1);

	    // add element
	    Element e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "element");
	    document.getDocumentElement().appendChild(e4);
	    addAttribute(e4, "name", elementName);
	    addAttribute(e4, "type", "anyType");
	    addAttribute(e4, "abstract", "true");

	    if (writeXmlEncodingInfos) {
		encodingInfos.add(ci, elementName, this.targetNamespace);
	    }
	}

	return res == 1;
    }

    private void addMinMaxOccurs(Element e, Multiplicity m) {

	if (m.minOccurs != 1) {

	    String minOccursTxt = ((Integer) m.minOccurs).toString();
	    minOccursTxt = options.internalize(minOccursTxt);
	    addAttribute(e, "minOccurs", minOccursTxt);
	}

	if (m.maxOccurs == Integer.MAX_VALUE) {

	    addAttribute(e, "maxOccurs", "unbounded");

	} else if (m.maxOccurs != 1) {

	    String maxOccursTxt = ((Integer) m.maxOccurs).toString();
	    maxOccursTxt = options.internalize(maxOccursTxt);
	    addAttribute(e, "maxOccurs", maxOccursTxt);
	}
    };

    /**
     * Process a single property.
     * 
     * @param cibase a class
     * @param pi     property of cibase
     * @param m      multiplicity of the property, can be <code>null</code>
     * @param schDoc tbd
     * @return tbd
     */
    protected Element addProperty(ClassInfo cibase, PropertyInfo pi, Multiplicity m, SchematronSchema schDoc) {

	Element e1;

	/*
	 * Map the property, if an according map entry exists.
	 * 
	 * NOTE: That the map entry defines omission of the property is checked in
	 * method includeProperty(PropertyInfo).
	 */
	XsdPropertyMapEntry pme = config.getPropertyMapEntry(pi);
	if (pme != null) {
	    e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "element");
	    addStandardAnnotation(e1, pi);
	    addAttribute(e1, "ref", addImport(pme.getTargetElement()));

	    if (writeXmlEncodingInfos) {

		String targetQName = pme.getTargetElement().trim();
		if (targetQName.contains(":")) {
		    String[] parts = targetQName.split(":");
		    String nsabr = parts[0];
		    String localName = parts[1];
		    String xmlNamespace = options.fullNamespace(nsabr);
		    if (xmlNamespace == null) {
			if (nsabr.equals(targetNamespaceAbbreviation)) {
			    xmlNamespace = targetNamespace;
			} else {
			    xmlNamespace = "FIXME";
			}
		    }
		    encodingInfos.add(pi, localName, xmlNamespace, false);
		} else {
		    encodingInfos.add(pi, targetQName, this.targetNamespace, false);
		}
	    }

	    addMinMaxOccurs(e1, pi.cardinality());
	    return e1;
	}

	boolean inAssocClass = true;
	if (m == null) {
	    m = pi.cardinality();
	    inAssocClass = false;
	}

	ClassInfo typeCi = model.classById(pi.typeInfo().id);

	String asAtt = pi.taggedValue("xsdAsAttribute");
	if (StringUtils.isBlank(asAtt))
	    asAtt = pi.taggedValue("asXMLAttribute");
	String asAttRef = "";
	String asAttGroupRef = "";
	if (typeCi != null) {
	    MapEntry me = options.attributeMapEntry(typeCi.name(), cibase.encodingRule("xsd"));
	    if (me != null) {
		asAttRef = me.rule;
	    }
	    me = options.attributeGroupMapEntry(typeCi.name(), cibase.encodingRule("xsd"));
	    if (me != null) {
		asAttGroupRef = me.rule;
	    }
	}

	// First take care of the special cases

	if (typeCi != null && typeCi.category() == Options.UNION && typeCi.asGroup()
		&& typeCi.matches("rule-xsd-cls-union-asGroup")) {
	    e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "group");
	    addStandardAnnotation(e1, pi);
	    if (typeCi.pkg() == null || typeCi.pkg().xmlns() == null) {
		MessageContext mc = result.addError(null, 132, typeCi.name());
		if (mc != null)
		    mc.addDetail(null, 400, "Class", typeCi.fullName());
	    } else {
		addAttribute(e1, "ref", typeCi.qname() + "Group");
		addImport(typeCi.pkg().xmlns(), typeCi.pkg().targetNamespace());
	    }
	    addMinMaxOccurs(e1, m);
	} else if (!asAttRef.equals("")) {
	    e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "attribute");
	    addAttribute(e1, "ref", asAttRef);
	    int idx = asAttRef.indexOf(":");
	    if (idx > 0) {
		String nsabr = asAttRef.substring(0, idx);
		String attName = asAttRef.substring(idx + 1);
		String ns = options.fullNamespace(nsabr);
		addImport(nsabr, ns);
		if (writeXmlEncodingInfos) {
		    encodingInfos.add(pi, attName, ns, true);
		}
	    } else {
		if (writeXmlEncodingInfos) {
		    encodingInfos.add(pi, asAttRef, this.targetNamespace, true);
		}
	    }
	    if (m.minOccurs == 1)
		addAttribute(e1, "use", "required");
	} else if (!asAttGroupRef.equals("")) {
	    e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "attributeGroup");
	    addAttribute(e1, "ref", asAttGroupRef);
	    int idx = asAttGroupRef.indexOf(":");
	    if (idx > 0) {
		String nsabr = asAttGroupRef.substring(0, idx);
		addImport(nsabr, options.fullNamespace(nsabr));
	    }
	    // TODO JE - If encoding infos shall be written, Warn that we could not define
	    // specific infos for pi
	} else if (pi.matches("rule-xsd-prop-xsdAsAttribute") && "true".equalsIgnoreCase(asAtt)
		&& (m.maxOccurs == 1 || asList(pi)) && !inAssocClass) {
	    e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "attribute");
	    addStandardAnnotation(e1, pi);
	    addAttribute(e1, "name", pi.name());

	    if (writeXmlEncodingInfos) {
		encodingInfos.add(pi, pi.name(), this.targetNamespace, true);
	    }

	    if (m.minOccurs == 1)
		addAttribute(e1, "use", "required");
	    mapPropertyType(cibase, pi, e1, false, schDoc);

	    if (pi.initialValue() != null && !pi.initialValue().isEmpty() && pi.matches("rule-xsd-prop-initialValue")) {
		if (pi.isReadOnly()) {
		    addAttribute(e1, "fixed", stripQuotes(pi.initialValue()));
		} else {
		    addAttribute(e1, "default", stripQuotes(pi.initialValue()));

		    if (e1.hasAttribute("use")) {
			// if @default is present, the @use attribute must have
			// the value optional (default)
			e1.removeAttribute("use");
		    }
		}
	    }
	} else if (cibase.matches("rule-xsd-cls-suppress") && cibase.stereotype("adeelement")) {
	    e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "element");
	    addStandardAnnotation(e1, pi);
	    addAttribute(e1, "name", pi.name());

	    if (writeXmlEncodingInfos) {
		encodingInfos.add(pi, pi.name(), this.targetNamespace, false);
	    }

	    if (typeCi != null && typeCi.asCharacterString())
		addAttribute(e1, "type", "string");
	    else
		mapPropertyType(cibase, pi, e1, inAssocClass, schDoc);
	    ClassInfo cis = cibase.unsuppressedSupertype(true);
	    if (cis != null) {
		String s = mapElement(cis);
		if (s != null) {
		    addAttribute(e1, "substitutionGroup",
			    s.replace(":_", ":").replace(":", ":_GenericApplicationPropertyOf"));
		    addImport(cis.pkg().xmlns(), cis.pkg().targetNamespace());
		}
	    } else {
		MessageContext mc = result.addError(this, 145, cibase.name());
		if (mc != null)
		    mc.addDetail(null, 400, "Class", cibase.fullName());
	    }

	} else if (typeCi != null && typeCi.category() == Options.CODELIST
		&& typeCi.matches("rule-xsd-cls-codelist-gmlsf")) {

	    e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "element");
	    addStandardAnnotation(e1, pi);
	    addAttribute(e1, "name", pi.name());

	    if (writeXmlEncodingInfos) {
		encodingInfos.add(pi, pi.name(), this.targetNamespace, false);
	    }

	    Element e2 = document.createElementNS(Options.W3C_XML_SCHEMA, "complexType");
	    e1.appendChild(e2);

	    Element simpleContent = document.createElementNS(Options.W3C_XML_SCHEMA, "simpleContent");
	    e2.appendChild(simpleContent);

	    Element restriction = document.createElementNS(Options.W3C_XML_SCHEMA, "restriction");
	    addAttribute(restriction, "base", "gml:CodeType");
	    simpleContent.appendChild(restriction);

	    String codeListUri = typeCi.taggedValue("codeList");
	    if (StringUtils.isBlank(codeListUri)) {
		codeListUri = typeCi.taggedValue("vocabulary");
	    }

	    if (StringUtils.isNotBlank(codeListUri)) {
		Element attribute = document.createElementNS(Options.W3C_XML_SCHEMA, "attribute");
		addAttribute(attribute, "name", "codeSpace");
		addAttribute(attribute, "type", "anyURI");
		addAttribute(attribute, "fixed", codeListUri.trim());
		restriction.appendChild(attribute);
	    }
	    addMinMaxOccurs(e1, m);
	    addImport("gml", options.fullNamespace("gml"));

	} else if (pi.categoryOfValue() == Options.FEATURE
		&& pi.matches("rule-xsd-prop-featureType-gmlsf-byReference")) {

	    e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "element");
	    addStandardAnnotation(e1, pi);
	    addAttribute(e1, "name", pi.name());

	    if (writeXmlEncodingInfos) {
		encodingInfos.add(pi, pi.name(), this.targetNamespace, false);
	    }

	    addAttribute(e1, "type", "gml:ReferenceType");
	    addMinMaxOccurs(e1, m);
	    addImport("gml", options.fullNamespace("gml"));

	} else {

	    e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "element");
	    addStandardAnnotation(e1, pi);
	    addAttribute(e1, "name", pi.name());

	    if (writeXmlEncodingInfos) {
		encodingInfos.add(pi, pi.name(), this.targetNamespace, false);
	    }

	    if ((pi.nilReasonAllowed() && pi.matches("rule-xsd-prop-nilReasonAllowed"))
		    || (pi.voidable() && pi.matches("rule-xsd-prop-nillable"))) {
		addAttribute(e1, "nillable", "true");
	    }

	    if (pi.initialValue() != null && !pi.initialValue().isEmpty() && pi.matches("rule-xsd-prop-initialValue")) {
		if (pi.isReadOnly()) {
		    addAttribute(e1, "fixed", stripQuotes(pi.initialValue()));
		} else {
		    addAttribute(e1, "default", stripQuotes(pi.initialValue()));
		}
	    }

	    boolean multiplicityAlreadySet = false;
	    if (typeCi != null && typeCi.asCharacterString() && typeCi.matches("rule-xsd-cls-union-asCharacterString"))
		addAttribute(e1, "type", "string");
	    else
		multiplicityAlreadySet = mapPropertyType(cibase, pi, e1, inAssocClass, schDoc);

	    if (!multiplicityAlreadySet) {
		if (typeCi != null && typeCi.isKindOf("historisches_Objekt")
			&& typeCi.matches("rule-xsd-cls-okstra-lifecycle")) {
		    m.maxOccurs = Integer.MAX_VALUE;
		}
		addMinMaxOccurs(e1, m);
	    }

	    /*
	     * 20120702 [js] Add restrictions given in the EA model using TaggedValues
	     * 'length' and 'size' / 'pattern'
	     */
	    if (pi.matches("rule-xsd-prop-length-size-pattern") && (StringUtils.isNotBlank(pi.taggedValue("length"))
		    || (StringUtils.isNotBlank(pi.taggedValue("size"))
			    && StringUtils.isNotBlank(pi.taggedValue("pattern"))))) {

		Element simpleType = document.createElementNS(Options.W3C_XML_SCHEMA, "simpleType");
		e1.appendChild(simpleType);

		Element restriction = document.createElementNS(Options.W3C_XML_SCHEMA, "restriction");
		addAttribute(restriction, "base", "string");
		e1.removeAttribute("type");
		simpleType.appendChild(restriction);

		Element concreteRestriction = null;
		// maxLength
		if (StringUtils.isNotBlank(pi.taggedValue("length"))) {
		    concreteRestriction = document.createElementNS(Options.W3C_XML_SCHEMA, "maxLength");
		    addAttribute(concreteRestriction, "value", pi.taggedValue("length"));
		    // pattern
		} else if (StringUtils.isNotBlank(pi.taggedValue("size"))
			&& StringUtils.isNotBlank(pi.taggedValue("pattern"))) {
		    concreteRestriction = document.createElementNS(Options.W3C_XML_SCHEMA, "pattern");
		    addAttribute(concreteRestriction, "value",
			    pi.taggedValue("pattern") + "{" + pi.taggedValue("size") + "}");
		}
		if (concreteRestriction != null)
		    restriction.appendChild(concreteRestriction);

	    }

	    if (pi.matches("rule-xsd-prop-constrainingFacets") && pi.categoryOfValue() != Options.CODELIST
		    && pi.categoryOfValue() != Options.ENUMERATION) {

		String length = pi.taggedValue("length");
		if (StringUtils.isBlank(length)) {
		    length = pi.taggedValue("maxLength");
		}
		if (StringUtils.isBlank(length)) {
		    length = pi.taggedValue("size");
		}
		String pattern = pi.taggedValue("pattern");
		String min = pi.taggedValue("rangeMinimum");
		String max = pi.taggedValue("rangeMaximum");
		String typecontent = "simple/simple";

		if (StringUtils.isNotBlank(length) || StringUtils.isNotBlank(pattern) || StringUtils.isNotBlank(min)
			|| StringUtils.isNotBlank(max)) {

		    /*
		     * baseType is the simple type that is the foundation of the restriction. It is
		     * defined by a map entry for the value type or a map entry in the supertypes
		     * (direct and indirect) of the value type that maps to a simple type with
		     * simple content.
		     */
		    String baseType = null;
		    String base = null;

		    /*
		     * Identify base and type content from the value type; this is important for
		     * correct declaration
		     */
		    MapEntry me = options.baseMapEntry(pi.typeInfo().name, pi.encodingRule("xsd"));
		    if (me != null) {
			base = me.p1;
			baseType = me.p1;
			typecontent = me.p2;
		    }
		    if (base == null && typeCi != null) {
			base = typeCi.qname() + "Type";
		    }

		    if (typeCi != null) {

			/*
			 * Identify base type of value type that has xmlTypeType="simple" (otherwise
			 * ="complex") and xmlTypeContent="simple"
			 */
			MapEntry me2 = findBaseMapEntryInSupertypes(typeCi, pi.encodingRule("xsd"), "simple", "simple");
			if (me2 == null) {
			    me2 = findBaseMapEntryInSupertypes(typeCi, pi.encodingRule("xsd"), "complex", "simple");
			}
			if (me2 != null) {
			    baseType = me2.p1;
			}
		    }

		    /*
		     * We do NOT use the base as fallback if baseType is null, since this would lead
		     * to always adding facets (since the check if a facet is supported by the
		     * baseType returns true if a qname is given as baseType).
		     */

		    if (base != null && baseType != null) {

			e1.removeAttribute("type");

			Element e2; // complexType or simpleType
			Element e3; // restriction or extension
			Element e4; // complexContent or simpleContent
			if (typecontent.equals("complex/simple")) {
			    e2 = document.createElementNS(Options.W3C_XML_SCHEMA, "complexType");
			    // addStandardAnnotation(e2, ci);
			    e1.appendChild(e2);
			    e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "simpleContent");
			    e2.appendChild(e4);
			    e3 = document.createElementNS(Options.W3C_XML_SCHEMA, "restriction");
			    e4.appendChild(e3);
			} else if (typecontent.equals("simple/simple")) {
			    e2 = document.createElementNS(Options.W3C_XML_SCHEMA, "simpleType");
			    e1.appendChild(e2);
			    // addStandardAnnotation(e2, ci);
			    e3 = document.createElementNS(Options.W3C_XML_SCHEMA, "restriction");
			    e2.appendChild(e3);
			} else {
			    e2 = document.createElementNS(Options.W3C_XML_SCHEMA, "complexType");
			    e1.appendChild(e2);
			    // addStandardAnnotation(e2, ci);
			    e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "complexContent");
			    e2.appendChild(e4);
			    e3 = document.createElementNS(Options.W3C_XML_SCHEMA, "extension");
			    e4.appendChild(e3);
			}
			addAttribute(e3, "base", base);
			if (facetSupported("totalDigits", baseType) && StringUtils.isNotBlank(length)) {
			    Element e5 = document.createElementNS(Options.W3C_XML_SCHEMA, "totalDigits");
			    e3.appendChild(e5);
			    addAttribute(e5, "value", length);
			}
			if (facetSupported("maxLength", baseType) && StringUtils.isNotBlank(length)) {
			    Element e5 = document.createElementNS(Options.W3C_XML_SCHEMA, "maxLength");
			    e3.appendChild(e5);
			    addAttribute(e5, "value", length);
			}
			if (facetSupported("pattern", baseType) && StringUtils.isNotBlank(pattern)) {
			    Element e5 = document.createElementNS(Options.W3C_XML_SCHEMA, "pattern");
			    e3.appendChild(e5);
			    addAttribute(e5, "value", pattern);
			}
			if (facetSupported("minInclusive", baseType) && StringUtils.isNotBlank(min)) {
			    Element e5 = document.createElementNS(Options.W3C_XML_SCHEMA, "minInclusive");
			    e3.appendChild(e5);
			    addAttribute(e5, "value", min);
			}
			if (facetSupported("maxInclusive", baseType) && StringUtils.isNotBlank(max)) {
			    Element e5 = document.createElementNS(Options.W3C_XML_SCHEMA, "maxInclusive");
			    e3.appendChild(e5);
			    addAttribute(e5, "value", max);
			}

		    } else {

			MessageContext mc = result.addError(this, 180, typeCi.name(), pi.name());
			if (mc != null)
			    mc.addDetail(null, 400, "Property", pi.fullName());
		    }
		}
	    }
	}

	return e1;
    }

    private boolean asArray(PropertyInfo propi) {
	boolean asArray = false;
	if (propi.matches("rule-xsd-prop-gmlArrayProperty")) {
	    String s = propi.taggedValue("gmlArrayProperty");
	    asArray = "true".equalsIgnoreCase(s);
	}
	if (asArray) {
	    if (!propi.matches("rule-xsd-prop-inlineOrByReference") || !propi.inlineOrByReference().equals("inline")) {
		asArray = false;
		MessageContext mc = result.addError(this, 170, propi.inClass().name() + "." + propi.name());
		if (mc != null)
		    mc.addDetail(null, 400, "Property", propi.fullName());
	    }
	}
	return asArray;
    }

    private boolean asList(PropertyInfo propi) {
	boolean asList = false;
	if (propi.matches("rule-xsd-prop-gmlListProperty")) {
	    String s = propi.taggedValue("gmlListProperty");
	    asList = "true".equalsIgnoreCase(s);
	}
	return asList;
    }

    private List<Qualifier> qualifiers(PropertyInfo propi) {
	if (propi.matches("rule-xsd-prop-qualified-associations")) {
	    if (!propi.isAttribute() && propi.isNavigable()) {
		PropertyInfo revpi = propi.reverseProperty();
		if (revpi != null) {
		    List<Qualifier> qv = revpi.qualifiers();
		    if (qv != null && qv.size() > 0)
			return qv;
		}
	    }
	}
	return null;
    }

    private boolean processQualifiers(Element e, PropertyInfo propi) {
	List<Qualifier> qv = qualifiers(propi);
	if (qv != null) {
	    for (Qualifier q : qv)
		processQualifier(e, propi, q);
	    return true;
	}
	return false;
    }

    private void processQualifier(Element e, PropertyInfo propi, Qualifier q) {
	Element e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "attribute");
	e.appendChild(e1);
	addAttribute(e1, "name", q.name);

	if (q.type == null) {
	    // qualifier without type, use string
	    MessageContext mc = result.addError(null, 176);
	    if (mc != null)
		mc.addDetail(null, 400, "Property", propi.fullName());
	    addAttribute(e1, "type", "string");
	} else {
	    MapEntry me = options.typeMapEntry(q.type, propi.encodingRule("xsd"));
	    if (me != null) {
		// we have a mapping to a property type
		if (me.p2.equalsIgnoreCase("simple/simple")) {
		    String propertyTypeName = addImport(me.p1);
		    if (propertyTypeName == null) {
			MessageContext mc = result.addError(this, 174, me.p1);
			if (mc != null)
			    mc.addDetail(null, 400, "Property", propi.fullName());
			propertyTypeName = "fixme:fixme";
		    }
		    addAttribute(e1, "type", propertyTypeName);
		} else if (me.p2.equalsIgnoreCase("complex/complex")) {
		    // complex value, use URI to reference it
		    addAttribute(e1, "type", "anyURI");
		} else {
		    MessageContext mc = result.addError(this, 175, me.p1);
		    if (mc != null)
			mc.addDetail(null, 400, "Property", propi.fullName());
		    addAttribute(e1, "type", "string");
		}
	    } else {
		// we have to derive the property type from the model
		ClassInfo ci = model.classByName(q.type);
		if (ci != null) {
		    if (ci.category() == Options.ENUMERATION) {
			addAttribute(e1, "type", ci.qname() + "Type");
			addImport(ci.qname());
		    } else if (ci.category() == Options.CODELIST || ci.category() == Options.FEATURE
			    || ci.category() == Options.GMLOBJECT || ci.category() == Options.MIXIN) {
			// use URI to reference it
			addAttribute(e1, "type", "anyURI");
		    } else if (ci.category() == Options.DATATYPE || ci.category() == Options.UNION) {
			MessageContext mc = result.addError(this, 178, ci.qname());
			if (mc != null)
			    mc.addDetail(null, 400, "Property", propi.fullName());
			addAttribute(e1, "type", "string");
		    } else {
			MessageContext mc = result.addError(this, 179, ci.qname());
			if (mc != null)
			    mc.addDetail(null, 400, "Property", propi.fullName());
			addAttribute(e1, "type", "string");
		    }
		} else {
		    MessageContext mc = result.addError(this, 177, q.type);
		    if (mc != null)
			mc.addDetail(null, 400, "Property", propi.fullName());
		    addAttribute(e1, "type", "string");
		}
	    }
	}
    }

    /**
     * Set the type for a property element.
     * 
     * @param cibase       class that owns the property
     * @param propi        the property
     * @param e            property element
     * @param inAssocClass flag is set, if the class is an association class
     * @param schDoc       Schematron schema, optional
     * @return true, if the multiplicity of the property element has already been
     *         set, false otherwise
     */
    protected boolean mapPropertyType(ClassInfo cibase, PropertyInfo propi, Element e, boolean inAssocClass,
	    SchematronSchema schDoc) {

	String pName = cibase.name() + "." + propi.name();
	boolean isAttribute = e.getLocalName().equals("attribute");
	Element e1 = null;

	/*
	 * First look into the mapping tables for entries that map the type to a
	 * pre-defined element, type, attribute or attributeGroup
	 */
	Type ti = propi.typeInfo();
	MapEntry me = options.typeMapEntry(ti.name, cibase.encodingRule("xsd"));

	boolean multiplicityAlreadySet = false;

	boolean addMetadata = false;
	if (propi.propertyMetadata() && propi.matches("rule-xsd-prop-metadata")) {
	    addMetadata = true;
	}

	/*
	 * Generate code list checks for properties where the property type is
	 * restricted (e.g. via OCL constraint) to a code list. This kind of restriction
	 * can be used in ISO metadata profiles.
	 */
	if (StringUtils.isNotBlank(propi.taggedValue("codeListRestriction"))) {
	    String typeName = propi.taggedValue("codeListRestriction");
	    ClassInfo codeList = propi.model().classByName(typeName);
	    addAssertionForCodelistUri(cibase, propi, codeList, schDoc);
	}

	if (me != null) {
	    /*
	     * So we have a mapping to a pre-defined element or type
	     */
	    if (me.rule.equals("direct")) {

		/*
		 * "direct", i.e. we usually have the property type in me.p1, the type/content
		 * type in me.p2 and in me.p3 whether the property type has a nilReason or name
		 * attribute. However, we may also have the case where we have a mapping to an
		 * attribute or an attributeGroup, so check this first.
		 */
		boolean addNilReason = (me.p3 != null && me.p3.equals("false")
			&& (propi.nilReasonAllowed() || (propi.voidable() && propi.matches("rule-xsd-prop-nillable"))));

		boolean softtyped = false;
		if (propi.matches("rule-xsd-prop-soft-typed")) {
		    String s = propi.taggedValue("soft-typed");
		    softtyped = "true".equalsIgnoreCase(s);
		}

		boolean asList = asList(propi);

		if (propi.matches("rule-xsd-prop-att-map-entry") && !isAttribute) {
		    MapEntry mea = options.attributeMapEntry(ti.name, cibase.encodingRule("xsd"));
		    String asAttRef = (mea != null ? mea.rule : "");
		    MapEntry meag = options.attributeGroupMapEntry(ti.name, cibase.encodingRule("xsd"));
		    String asAttGroupRef = (meag != null ? meag.rule : "");

		    if (!asAttRef.equals("") || !asAttGroupRef.equals("")) {
			/*
			 * The property type has no element content, only attributes / attributeGroups:
			 * add an anonymous type with the attributes
			 */
			e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "complexType");
			e.appendChild(e1);

			/*
			 * add nilReason, if needed
			 */
			if (addNilReason) {
			    Element e2 = document.createElementNS(Options.W3C_XML_SCHEMA, "attribute");
			    e1.appendChild(e2);
			    addNilReason(e2);
			}
			if (addMetadata) {
			    addMetadata(e1);
			}
			if (softtyped) {
			    Element e2 = document.createElementNS(Options.W3C_XML_SCHEMA, "attribute");
			    e1.appendChild(e2);
			    addName(e2);
			}

			/*
			 * add attribute
			 */
			if (!asAttRef.equals("")) {
			    Element e2 = document.createElementNS(Options.W3C_XML_SCHEMA, "attribute");
			    addAttribute(e2, "ref", asAttRef);
			    int ix = asAttRef.indexOf(":");
			    if (ix > 0) {
				String nsabr = asAttRef.substring(0, ix);
				addImport(nsabr, options.fullNamespace(nsabr));
			    }
			    e1.appendChild(e2);
			}

			/*
			 * add attribute group
			 */
			if (!asAttGroupRef.equals("")) {
			    Element e2 = document.createElementNS(Options.W3C_XML_SCHEMA, "attributeGroup");
			    addAttribute(e2, "ref", asAttGroupRef);
			    int ix = asAttGroupRef.indexOf(":");
			    if (ix > 0) {
				String nsabr = asAttGroupRef.substring(0, ix);
				addImport(nsabr, options.fullNamespace(nsabr));
			    }
			    e1.appendChild(e2);
			}
			return false;
		    }
		}

		String propertyTypeName = addImport(me.p1);
		if (propertyTypeName == null) {
		    result.addError(this, 174, me.p1);
		    propertyTypeName = "fixme:fixme";
		}

		/*
		 * if the property "element" is an attribute or if an xs:list should be used,
		 * ensure that the type is simple
		 */
		if (isAttribute) {
		    boolean simpleType = me.p2 != null && me.p2.equals("simple/simple");
		    if (!simpleType || addNilReason || addMetadata || softtyped) {
			MessageContext mc = result.addError(this, 128, pName);
			if (mc != null)
			    mc.addDetail(null, 400, "Property", propi.fullName());
			/*
			 * If it is just the nilReason part, we can skip it
			 */
			if (!simpleType)
			    return false;

			addNilReason = false;
			addMetadata = false;
			softtyped = false;
		    }
		}
		if (asList) {
		    boolean simpleType = me.p2 != null && me.p2.equals("simple/simple");
		    if (!simpleType) {
			MessageContext mc = result.addError(this, 169, pName, me.p1);
			if (mc != null)
			    mc.addDetail(null, 400, "Property", propi.fullName());
			return false;
		    }
		}

		boolean asArray = asArray(propi);
		String asArrayTargetElement = null;
		if (asArray) {
		    MapEntry me2 = options.elementMapEntry(ti.name, cibase.encodingRule("xsd"));
		    if (me2 == null) {
			asArray = false;
			MessageContext mc = result.addError(this, 172, pName, ti.name);
			if (mc != null)
			    mc.addDetail(null, 400, "Property", propi.fullName());
		    } else
			asArrayTargetElement = me2.p1;
		}

		boolean withQualifiers = qualifiers(propi) != null && !isAttribute;

		// FIXME 2014-01-21 JE: does this correctly take into account
		// the inlineOrByReference setting?
		if (!addNilReason && !addMetadata && !softtyped && !asArray && !asList && !withQualifiers) {
		    /*
		     * We have a property type and no need to add a nilReason, metadata, or name
		     * attribute: simply add the type name
		     */
		    addAttribute(e, "type", propertyTypeName);
		    return false;

		} else if (asList) {
		    /*
		     * We need to construct an anonymous simple property type with a xs:list. We
		     * also honour multiplicity restrictions...
		     */
		    e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "simpleType");
		    e.appendChild(e1);
		    Multiplicity m = propi.cardinality();
		    if (m.minOccurs == 0 && m.maxOccurs == Integer.MAX_VALUE) {
			Element e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "list");
			e1.appendChild(e4);
			addAttribute(e4, "itemType", me.p1);
		    } else {
			Element e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "restriction");
			e1.appendChild(e4);
			Element e5 = document.createElementNS(Options.W3C_XML_SCHEMA, "simpleType");
			e4.appendChild(e5);
			Element e6 = document.createElementNS(Options.W3C_XML_SCHEMA, "list");
			e5.appendChild(e6);
			addAttribute(e6, "itemType", me.p1);
			if (m.minOccurs > 0) {
			    e6 = document.createElementNS(Options.W3C_XML_SCHEMA, "minLength");
			    e4.appendChild(e6);
			    String minOccTxt = "" + m.minOccurs;
			    minOccTxt = options.internalize(minOccTxt);
			    addAttribute(e6, "value", minOccTxt);
			}
			if (m.maxOccurs != Integer.MAX_VALUE) {
			    e6 = document.createElementNS(Options.W3C_XML_SCHEMA, "maxLength");
			    e4.appendChild(e6);
			    String maxOccTxt = "" + m.maxOccurs;
			    maxOccTxt = options.internalize(maxOccTxt);
			    addAttribute(e6, "value", maxOccTxt);
			}
		    }

		    // if 0..? also allow that the property element or attribute
		    // is missing
		    multiplicityAlreadySet = true;
		    if (isAttribute) {
			if (propi.cardinality().minOccurs > 0)
			    addAttribute(e, "required", "true");
		    } else {
			if (propi.cardinality().minOccurs == 0)
			    addAttribute(e, "minOccurs", "0");
		    }

		} else if (asArray) {
		    /*
		     * We need to construct an anonymous array property type
		     */
		    multiplicityAlreadySet = addAnonymousPropertyType(e, propi, null, asArrayTargetElement, null,
			    false);

		    // if 0..? also allow that the property element is missing
		    multiplicityAlreadySet = true;
		    if (propi.cardinality().minOccurs == 0)
			addAttribute(e, "minOccurs", "0");

		} else {
		    /*
		     * We have a property type, but still need to add a nilReason, name or qualifier
		     * attribute
		     */
		    e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "complexType");
		    e.appendChild(e1);
		    Element e4;
		    if (me.p2 == null || me.p2.equals("complex/complex"))
			e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "complexContent");
		    else
			e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "simpleContent");
		    e1.appendChild(e4);
		    Element e3 = document.createElementNS(Options.W3C_XML_SCHEMA, "extension");
		    e4.appendChild(e3);
		    addAttribute(e3, "base", propertyTypeName);
		    if (addNilReason) {
			Element e2 = document.createElementNS(Options.W3C_XML_SCHEMA, "attribute");
			e3.appendChild(e2);
			addNilReason(e2);
		    }
		    if (addMetadata) {
			addMetadata(e3);
		    }
		    if (softtyped) {
			Element e2 = document.createElementNS(Options.W3C_XML_SCHEMA, "attribute");
			e3.appendChild(e2);
			addName(e2);
		    }
		    if (withQualifiers) {
			if (processQualifiers(e3, propi)) {
			    addAttribute(e, "minOccurs", "0");
			    addAttribute(e, "maxOccurs", "unbounded");
			    multiplicityAlreadySet = true;
			}
		    }
		}

	    } else if (me.rule.equals("propertyType")) {
		multiplicityAlreadySet = addAnonymousPropertyType(e, propi, null, me.p1, null, false);

	    } else if (me.rule.equals("metadataPropertyType")) {
		multiplicityAlreadySet = addAnonymousPropertyType(e, propi, null, me.p1, null, true);
	    }
	    return multiplicityAlreadySet;
	}

	/*
	 * If we end up here, we have no map entry in the configuration, so we look at
	 * the value type to determine the proper property type. So first get the class
	 * of the value type.
	 */

	ClassInfo ci = model.classById(ti.id);
	if (ci == null) {
	    // try to get by-name if link by-id is broken
	    ci = model.classByName(ti.name);
	    if (ci != null) {
		MessageContext mc = result.addError(null, 135, pName);
		if (mc != null)
		    mc.addDetail(null, 400, "Property", propi.fullName());
	    }
	}

	ClassInfo associationClass = null;
	if (!propi.isAttribute() && !inAssocClass) {
	    AssociationInfo ai = propi.association();
	    if (ai != null && ai.matches("rule-xsd-rel-association-classes"))
		associationClass = ai.assocClass();
	    if (associationClass != null)
		ci = associationClass;
	}

	if (ci == null) {
	    MessageContext mc = result.addError(null, 131, pName, ti.name);
	    if (mc != null)
		mc.addDetail(null, 400, "Property", propi.fullName());
	    return false;
	}

	/*
	 * Now look at the different special cases
	 */

	if (ci.isUnionDirect() && ci.matches("rule-xsd-cls-union-direct")) {

	    for (PropertyInfo pi2 : ci.properties().values()) {

		if (pi2.isNavigable() && !pi2.isRestriction() && !pi2.implementedByNilReason()) {

		    if (ci.hasNilReason())
			pi2.nilReasonAllowed(true);
		    addAttribute(e, "nillable", "true");
		    mapPropertyType(cibase, pi2, e, false, schDoc);

		    // determine the multiplicity to use, based upon
		    // the information from both properties (propi & pi2)
		    // Multiplicity m2 = pi2.cardinality();

		    int pi2MinOccurs = pi2.cardinality().minOccurs;
		    int propiMinOccurs = propi.cardinality().minOccurs;

		    int minOccurs = (propiMinOccurs < pi2MinOccurs) ? propiMinOccurs : pi2MinOccurs;

		    int pi2MaxOccurs = pi2.cardinality().maxOccurs;
		    int propiMaxOccurs = propi.cardinality().maxOccurs;

		    int maxOccurs = 0;

		    if (pi2MaxOccurs == Integer.MAX_VALUE || propiMaxOccurs == Integer.MAX_VALUE) {
			maxOccurs = Integer.MAX_VALUE;
		    } else {
			maxOccurs = pi2MaxOccurs * propiMaxOccurs;
		    }
		    // m2.minOccurs = minOccurs;
		    // m2.maxOccurs = maxOccurs;
		    Multiplicity m = new Multiplicity(minOccurs, maxOccurs);
		    addMinMaxOccurs(e, m);

		    return true;
		}
	    }
	    MessageContext mc = result.addError(this, 129, ci.name(), pName);
	    if (mc != null)
		mc.addDetail(null, 400, "Class", ci.fullName());
	}

	boolean asArray = asArray(propi);
	if (asArray && !classHasObjectElement(ci)) {
	    asArray = false;
	    MessageContext mc = result.addError(this, 173, pName, ci.name());
	    if (mc != null)
		mc.addDetail(null, 400, "Property", propi.fullName());
	}

	if (ci.category() == Options.UNION && ci.matches("rule-xsd-cls-union-omitUnionsRepresentingFeatureTypeSets")
		&& "true".equalsIgnoreCase(ci.taggedValue("representsFeatureTypeSet"))) {

	    if (addMetadata) {
		Element e1x = document.createElementNS(Options.W3C_XML_SCHEMA, "complexType");
		e.appendChild(e1x);
		Element e4x = document.createElementNS(Options.W3C_XML_SCHEMA, "complexContent");
		e1x.appendChild(e4x);
		Element e3x = document.createElementNS(Options.W3C_XML_SCHEMA, "extension");
		e4x.appendChild(e3x);
		addAttribute(e3x, "base", "gml:ReferenceType");
		addMetadata(e3x);
	    } else {
		addAttribute(e, "type", "gml:ReferenceType");
	    }
	    addImport("gml", options.fullNamespace("gml"));

	    if (propi.matches("rule-xsd-prop-targetElement")
		    && options.GML_NS.equals("http://www.opengis.net/gml/3.2")) {

		Element annotationElement;
		NodeList annotationElements = e.getElementsByTagName("annotation");
		if (annotationElements != null && annotationElements.getLength() != 0) {
		    annotationElement = (Element) annotationElements.item(0);
		} else {
		    annotationElement = document.createElementNS(Options.W3C_XML_SCHEMA, "annotation");
		    e.appendChild(annotationElement);
		}

		Element appinfoElement = document.createElementNS(Options.W3C_XML_SCHEMA, "appinfo");
		annotationElement.appendChild(appinfoElement);

		/*
		 * get all properties from the union, recursively drill down to get all options
		 * from unions that represent feature type sets
		 */
		List<ClassInfo> representedFeatures = new ArrayList<ClassInfo>(findAllRepresentedFeatures(ci));
		Collections.sort(representedFeatures, new Comparator<ClassInfo>() {

		    @Override
		    public int compare(ClassInfo o1, ClassInfo o2) {
			return o1.name().compareTo(o2.name());
		    }
		});

		for (ClassInfo representedFeatureType : representedFeatures) {

		    String s = mapElement(representedFeatureType);
		    if (s == null) {
			s = representedFeatureType.qname();
		    }

		    Element targetElement = document.createElementNS(options.GML_NS, "gml:targetElement");
		    appinfoElement.appendChild(targetElement);
		    targetElement.appendChild(document.createTextNode(s));
		}
	    }

	} else if (ci.category() == Options.OKSTRAKEY && ci.matches("rule-xsd-cls-okstra-schluesseltabelle")) {
	    addAttribute(e, "type", propertyTypeName(ci, true));
	    addImport(ci.pkg().xmlns(), ci.pkg().targetNamespace());

	} else if (ci.category() == Options.OKSTRAFID && ci.matches("rule-xsd-cls-okstra-fid")) {
	    addAttribute(e, "type", okstraObjectRefType);
	    addImport(okstraPrefix, options.fullNamespace(okstraPrefix));

	} else if (classHasObjectElement(ci)) {

	    if (ci.pkg() == null || ci.pkg().xmlns() == null) {
		MessageContext mc = result.addError(this, 141, ci.name(), propi.inClass().name());
		if (mc != null)
		    mc.addDetail(null, 400, "Class", ci.fullName());

	    } else if (ci.matches("rule-xsd-cls-standard-19139-property-types")) {

		String type = propertyTypeName(ci, true);

		if (addMetadata) {
		    Element e1x = document.createElementNS(Options.W3C_XML_SCHEMA, "complexType");
		    e.appendChild(e1x);
		    Element e4x = document.createElementNS(Options.W3C_XML_SCHEMA, "complexContent");
		    e1x.appendChild(e4x);
		    Element e3x = document.createElementNS(Options.W3C_XML_SCHEMA, "extension");
		    e4x.appendChild(e3x);
		    addAttribute(e3x, "base", type);
		    addMetadata(e3x);
		} else {
		    addAttribute(e, "type", type);
		}

		addImport(ci.pkg().xmlns(), ci.pkg().targetNamespace());
		if (ci.category() == Options.CODELIST) {
		    addAssertionForCodelistUri(cibase, propi, ci, schDoc);
		}

	    } else if (ci.matches("rule-xsd-cls-standard-gml-property-types")
		    || ci.matches("rule-xsd-cls-standard-swe-property-types")) {
		boolean embedPropertyType = false;
		boolean addImport = true;

		if (propi.matches("rule-xsd-prop-inlineOrByReference")
			&& propi.inlineOrByReference().equals("byreference")) {
		    /*
		     * For by-reference we never use the standard property type
		     */
		    embedPropertyType = true;
		    if (!propi.matches("rule-xsd-prop-targetElement")) {
			addImport = false;
		    }
		} else if (propi.matches("rule-xsd-prop-inlineOrByReference")
			&& propi.inlineOrByReference().equals("inline")) {
		    /*
		     * For inline there are a number of cases that we need to distinguish:
		     * 
		     * 1. GML 3.2 and later and a property type which derives from
		     * AbstractMemberType or AbstractMetadataPropertyType
		     */
		    if (ci.matches("rule-xsd-cls-standard-gml-property-types"))
			embedPropertyType = embedPropertyType || options.GML_NS.equals("http://www.opengis.net/gml/3.2")
				&& (propi.isAggregation() || propi.isComposition() || propi.isMetadata())
				&& (propi.categoryOfValue() == Options.GMLOBJECT
					|| propi.categoryOfValue() == Options.FEATURE);
		    /*
		     * 2. the property type needs to include a nilReason attribute
		     */
		    if (ci.matches("rule-xsd-cls-standard-gml-property-types"))
			embedPropertyType = embedPropertyType || propi.nilReasonAllowed()
				|| (propi.voidable() && propi.matches("rule-xsd-prop-nillable"));
		    /*
		     * 3. the property type is an array property
		     */
		    if (ci.matches("rule-xsd-cls-standard-gml-property-types")) {
			embedPropertyType = embedPropertyType || asArray;
		    }
		    /*
		     * 4. the value type has identity but no by-value-property-type or no identity
		     * and no regular property type
		     */
		    embedPropertyType = embedPropertyType || (classHasIdentity(ci) && !ci.includeByValuePropertyType())
			    || (!classHasIdentity(ci) && !ci.includePropertyType());
		    /*
		     * 5. the property has qualifiers
		     */
		    embedPropertyType = embedPropertyType || qualifiers(propi) != null;

		    /*
		     * 6. the property has metadata
		     */
		    embedPropertyType = embedPropertyType || addMetadata;

		    if (!embedPropertyType) {
			if (classHasIdentity(ci))
			    addAttribute(e, "type", ci.qname() + "PropertyByValueType");
			else
			    addAttribute(e, "type", propertyTypeName(ci, true));
		    }

		} else {
		    /*
		     * For the standard case there are a number of cases that we need to
		     * distinguish, too:
		     * 
		     * 1. GML 3.2 and later and a property type which derives from
		     * AbstractMemberType or AbstractMetadataPropertyType
		     */
		    if (ci.matches("rule-xsd-cls-standard-gml-property-types"))
			embedPropertyType = embedPropertyType || options.GML_NS.equals("http://www.opengis.net/gml/3.2")
				&& (propi.isAggregation() || propi.isComposition() || propi.isMetadata())
				&& (propi.categoryOfValue() == Options.GMLOBJECT
					|| propi.categoryOfValue() == Options.FEATURE);
		    /*
		     * 2. GML 3.1 and earlier and the property type needs to include a nilReason
		     * attribute
		     */
		    if (ci.matches("rule-xsd-cls-standard-gml-property-types"))
			embedPropertyType = embedPropertyType
				|| (options.GML_NS.equals("http://www.opengis.net/gml") && (propi.nilReasonAllowed()
					|| (propi.voidable()) && propi.matches("rule-xsd-prop-nillable")));
		    /*
		     * 3. GML 3.2 and later and the property type needs to include a nilReason
		     * attribute
		     */
		    if (ci.matches("rule-xsd-cls-standard-gml-property-types"))
			embedPropertyType = embedPropertyType
				|| (options.GML_NS.equals("http://www.opengis.net/gml/3.2")
					&& (propi.nilReasonAllowed()
						|| (propi.voidable() && propi.matches("rule-xsd-prop-nillable")))
					&& !classHasIdentity(ci));
		    /*
		     * 4. the value type has no regular property type
		     */
		    embedPropertyType = embedPropertyType || !ci.includePropertyType();
		    /*
		     * 5. the property is soft-typed (SWE Common)
		     */
		    if (ci.matches("rule-xsd-cls-standard-swe-property-types"))
			embedPropertyType = embedPropertyType || (propi.matches("rule-xsd-prop-soft-typed")
				&& "true".equalsIgnoreCase(propi.taggedValue("soft-typed")));
		    /*
		     * 6. the property has qualifiers
		     */
		    embedPropertyType = embedPropertyType || qualifiers(propi) != null;

		    /*
		     * 7. the property has metadata
		     */
		    embedPropertyType = embedPropertyType || addMetadata;

		    if (!embedPropertyType)
			addAttribute(e, "type", propertyTypeName(ci, true));
		}

		if (embedPropertyType)
		    multiplicityAlreadySet = addAnonymousPropertyType(e, propi, associationClass, ci.qname(), null,
			    false);

		if (asArray) {
		    // if 0..? also allow that the property element is missing
		    multiplicityAlreadySet = true;
		    if (propi.cardinality().minOccurs == 0)
			addAttribute(e, "minOccurs", "0");
		}

		if (addImport) {
		    addImport(ci.pkg().xmlns(), ci.pkg().targetNamespace());
		}
	    }

	} else if ((ci.matches("rule-xsd-cls-mixin-classes") && ci.category() == Options.MIXIN)
		|| (ci.matches("rule-xsd-cls-no-abstract-classes") && ci.isAbstract())) {

	    multiplicityAlreadySet = addAnonymousPropertyType(e, propi, null, null, subtypes(ci), false);

	} else if (ci.matches("rule-xsd-cls-standard-gml-property-types")
		|| ci.matches("rule-xsd-cls-standard-swe-property-types")) {

	    if (ci.category() == Options.CODELIST && ci.matches("rule-xsd-cls-codelist-anonymous-xlink")) {

		// Case to handle code lists, primarily for GML 2.1 encodings of
		// properties that are of a CodeList type
		e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "complexType");
		e.appendChild(e1);
		Element e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "sequence");
		e1.appendChild(e4);
		e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "attributeGroup");
		e1.appendChild(e4);
		addAttribute(e4, "ref", "xlink:simpleAttrs");
		addImport("xlink", options.fullNamespace("xlink"));

	    } else if (ci.category() == Options.CODELIST
		    && ((ci.matches("rule-xsd-cls-codelist-asDictionary") && ci.asDictionary())
			    || (ci.matches("rule-xsd-cls-codelist-asDictionaryGml33") && ci.asDictionaryGml33()))) {

		/*
		 * So the code list is represented by a dictionary and not actually encoded as
		 * an XML Schema type.
		 * 
		 * Encode the property with a reference type, depending on the encoding rule
		 * that applies to the property (NOT the code list).
		 */

		if (propi.inClass().matches("rule-xsd-cls-codelist-asDictionaryGml33")) {

		    /*
		     * The GML 3.3 encoding rule applies to the property.
		     */

		    if (addMetadata) {
			Element e1x = document.createElementNS(Options.W3C_XML_SCHEMA, "complexType");
			e.appendChild(e1x);
			Element e4x = document.createElementNS(Options.W3C_XML_SCHEMA, "complexContent");
			e1x.appendChild(e4x);
			Element e3x = document.createElementNS(Options.W3C_XML_SCHEMA, "extension");
			e4x.appendChild(e3x);
			addAttribute(e3x, "base", "gml:ReferenceType");
			addMetadata(e3x);
		    } else {
			addAttribute(e, "type", "gml:ReferenceType");
		    }
		    addAssertionForCodelistUri(cibase, propi, ci, schDoc);

		} else if (propi.inClass().matches("rule-xsd-cls-standard-swe-property-types")) {

		    /*
		     * SWE Common encoding applies to the property.
		     */

		    if (addMetadata) {
			Element e1x = document.createElementNS(Options.W3C_XML_SCHEMA, "complexType");
			e.appendChild(e1x);
			Element e4x = document.createElementNS(Options.W3C_XML_SCHEMA, "complexContent");
			e1x.appendChild(e4x);
			Element e3x = document.createElementNS(Options.W3C_XML_SCHEMA, "extension");
			e4x.appendChild(e3x);
			addAttribute(e3x, "base", "swe:ReferenceType");
			addMetadata(e3x);
		    } else {
			addAttribute(e, "type", "swe:ReferenceType");
		    }
		    addAssertionForCodelistUri(cibase, propi, ci, schDoc);

		} else {

		    /*
		     * The property is neither GML 3.3 nor SWE Common encoded. Likely GML 3.2 (could
		     * also be a prior GML version). Use gml:CodeType as type.
		     */

		    if (!propi.nilReasonAllowed() && !(propi.voidable() && propi.matches("rule-xsd-prop-nillable"))
			    && !addMetadata) {
			addAttribute(e, "type", "gml:CodeType");
		    } else {
			e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "complexType");
			e.appendChild(e1);
			Element e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "simpleContent");
			e1.appendChild(e4);
			Element e3 = document.createElementNS(Options.W3C_XML_SCHEMA, "extension");
			e4.appendChild(e3);
			addAttribute(e3, "base", "gml:CodeType");

			if (propi.nilReasonAllowed() || (propi.voidable() && propi.matches("rule-xsd-prop-nillable"))) {
			    Element e2 = document.createElementNS(Options.W3C_XML_SCHEMA, "attribute");
			    e3.appendChild(e2);
			    addNilReason(e2);
			}

			if (addMetadata) {
			    addMetadata(e3);
			}
		    }

		    addAssertionForCodelistUri(cibase, propi, ci, schDoc);
		}

		if (propi.isMetadata()) {
		    MessageContext mc = result.addWarning(this, 1009, pName);
		    if (mc != null)
			mc.addDetail(null, 400, "Property", propi.fullName());
		}

		if (ci.matches("rule-xsd-cls-standard-swe-property-types"))
		    addImport("swe", options.fullNamespace("swe"));
		else
		    addImport("gml", options.fullNamespace("gml"));

	    } else if (ci.category() == Options.CODELIST
		    || (ci.category() == Options.ENUMERATION && !ci.matches("rule-xsd-cls-local-enumeration"))
		    || (ci.matches("rule-xsd-cls-basictype") && ci.category() == Options.BASICTYPE
			    && !ci.matches("rule-xsd-cls-local-basictype"))) {

		if (ci.pkg() == null || ci.pkg().xmlns() == null) {
		    MessageContext mc = result.addError(this, 141, ci.name(), propi.inClass().name());
		    if (mc != null)
			mc.addDetail(null, 400, "Class", ci.fullName());
		} else {

		    if (!propi.nilReasonAllowed() && !(propi.voidable() && propi.matches("rule-xsd-prop-nillable"))
			    && !addMetadata) {
			addAttribute(e, "type", options.internalize(ci.qname() + "Type"));
		    } else {

			e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "complexType");
			e.appendChild(e1);
			Element e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "simpleContent");
			e1.appendChild(e4);
			Element e3 = document.createElementNS(Options.W3C_XML_SCHEMA, "extension");
			e4.appendChild(e3);
			addAttribute(e3, "base", options.internalize(ci.qname() + "Type"));

			if (propi.nilReasonAllowed() || (propi.voidable() && propi.matches("rule-xsd-prop-nillable"))) {
			    Element e2 = document.createElementNS(Options.W3C_XML_SCHEMA, "attribute");
			    e3.appendChild(e2);
			    addNilReason(e2);
			}

			if (addMetadata) {
			    addMetadata(e3);
			}
		    }

		    addImport(ci.pkg().xmlns(), ci.pkg().targetNamespace());
		}

		if (propi.isMetadata()) {
		    MessageContext mc = result.addWarning(this, 1009, pName);
		    if (mc != null)
			mc.addDetail(null, 400, "Property", propi.fullName());
		}
	    } else if (ci.category() == Options.ENUMERATION && ci.matches("rule-xsd-cls-local-enumeration")) {
		e1 = pAnonymousEnumeration(ci);
		e.appendChild(e1);
		if (propi.nilReasonAllowed() || (propi.voidable() && propi.matches("rule-xsd-prop-nillable"))) {
		    MessageContext mc = result.addWarning(this, 1010, pName, "enumeration");
		    if (mc != null)
			mc.addDetail(null, 400, "Property", propi.fullName());
		}
		if (propi.isMetadata()) {
		    MessageContext mc = result.addWarning(this, 1009, pName);
		    if (mc != null)
			mc.addDetail(null, 400, "Property", propi.fullName());
		}
	    } else if (ci.matches("rule-xsd-cls-basictype") && ci.category() == Options.BASICTYPE
		    && ci.matches("rule-xsd-cls-local-basictype")) {
		e1 = pAnonymousBasicType(ci);
		e.appendChild(e1);
		if (propi.nilReasonAllowed() || (propi.voidable() && propi.matches("rule-xsd-prop-nillable"))) {
		    MessageContext mc = result.addWarning(this, 1010, pName, "basic type");
		    if (mc != null)
			mc.addDetail(null, 400, "Property", propi.fullName());
		}
		if (propi.isMetadata()) {
		    MessageContext mc = result.addWarning(this, 1009, pName);
		    if (mc != null)
			mc.addDetail(null, 400, "Property", propi.fullName());
		}
	    } else {
		MessageContext mc = result.addError(this, 130, pName);
		if (mc != null)
		    mc.addDetail(null, 400, "Property", propi.fullName());
	    }
	} else {
	    MessageContext mc = result.addError(this, 130, pName);
	    if (mc != null)
		mc.addDetail(null, 400, "Property", propi.fullName());
	}
	return multiplicityAlreadySet;
    }

    /**
     * *
     * 
     * @param ci
     * @return The set of feature types represented by the given union (with tagged
     *         value "representsFeatureTypeSet=true"), as well as indirect unions
     *         that may be implied by this union; can be empty but not
     *         <code>null</code>
     */
    private SortedSet<ClassInfo> findAllRepresentedFeatures(ClassInfo ci) {

	SortedSet<ClassInfo> result = new TreeSet<ClassInfo>();

	if (ci != null && ci.category() == Options.UNION
		&& "true".equalsIgnoreCase(ci.taggedValue("representsFeatureTypeSet"))) {

	    for (PropertyInfo option : ci.propertiesAll()) {

		ClassInfo type = model.classById(option.typeInfo().id);

		if (type == null) {
		    type = model.classByName(option.typeInfo().name);
		}

		if (type != null) {

		    if (type.category() == Options.UNION
			    && "true".equalsIgnoreCase(ci.taggedValue("representsFeatureTypeSet"))) {

			result.addAll(findAllRepresentedFeatures(type));

		    } else if (type.category() == Options.FEATURE) {

			result.add(type);
		    }
		}
	    }
	}

	return result;
    }

    /**
     * @param cibase class that owns the property
     * @param propi  the property
     * @param typeCi a code list type; typically the (value) type of the property,
     *               but could also be a code list type that was defined via an
     *               additional restriction (tagged value - potentially derived from
     *               an OCL constraint)
     * @param schDoc
     */
    private void addAssertionForCodelistUri(ClassInfo cibase, PropertyInfo propi, ClassInfo typeCi,
	    SchematronSchema schDoc) {

	if (schDoc != null && cibase.matches("rule-xsd-cls-codelist-constraints")) {

	    // add assertion for code list URI
	    XpathFragment xpath;

	    String s = typeCi.taggedValue("codeList");
	    if (StringUtils.isBlank(s))
		s = typeCi.taggedValue("vocabulary");

	    if (typeCi.matches("rule-xsd-cls-standard-19139-property-types")) {

		if (StringUtils.isNotBlank(s)) {
		    xpath = new XpathFragment(0, propi.qname() + "/*/@codeList='" + s + "'");
		    schDoc.addAssertion(cibase, xpath, "Code list is '" + s + "'");
		}

		// assert the existence of the code list
		String s2 = schDoc.determineCodeListValuePattern(typeCi, "{codeList}/{value}");
		s2 = "concat('" + s2.replace("{codeList}", s).replace("{value}", "',*/@codeListValue)");
		xpath = new XpathFragment(0,
			"(not contains('" + s2 + "', '#') and document('" + s2 + "')) or (contains('" + s2
				+ "', '#') and document(substring-before('" + s2 + "','#'))/id(substring-after('" + s2
				+ "','#')))");
		schDoc.addAssertion(cibase, xpath, "Code list value exists");

		// assert that the remote resource has the correct element based
		// on its representation
		s = typeCi.taggedValue("codeListRepresentation");

		if (StringUtils.isBlank(s) || s.equalsIgnoreCase("application/gml+xml;version=3.2")) {

		    xpath = new XpathFragment(0, "(not contains('" + s2 + "', '#') and document('" + s2
			    + "')/gml:Definition) or (contains('" + s2 + "', '#') and document(substring-before('" + s2
			    + "','#'))/id(substring-after('" + s2
			    + "','#'))[local-name()='Definiton' and namespace-uri()='http://www.opengis.net/gml/3.2'])");
		    schDoc.addAssertion(cibase, xpath, "Code list dictionary is represented using GML 3.2");

		} else if (s.equalsIgnoreCase("application/rdf+xml")) {

		    xpath = new XpathFragment(0, "(not contains('" + s2 + "', '#') and document('" + s2
			    + "')/skos:Concept) or (contains('" + s2 + "', '#') and document(substring-before('" + s2
			    + "','#'))/id(substring-after('" + s2
			    + "','#'))[local-name()='Concept' and namespace-uri()='http://www.w3.org/2004/02/skos/core#'])");
		    schDoc.addAssertion(cibase, xpath, "Code list dictionary is represented using SKOS");
		}

	    } else if (propi.inClass().matches("rule-xsd-cls-codelist-asDictionaryGml33")
		    && (typeCi.asDictionary() || typeCi.asDictionaryGml33())) {

		/*
		 * The property is encoded using the GML 3.3 encoding rule, and the value is a
		 * code list that is encoded as dictionary.
		 */

		if (s != null && !s.isEmpty()) {
		    xpath = new XpathFragment(0, "starts-with(" + propi.qname() + "/@xlink:href,'" + s + "')");
		    schDoc.addAssertion(cibase, xpath, "Code list value URI starts with '" + s + "'");
		}

		// assert the existence of the code list
		xpath = new XpathFragment(0,
			"(not contains(" + propi.qname() + "/@xlink:href, '#') and document(" + propi.qname()
				+ "/@xlink:href)) or (contains(" + propi.qname()
				+ "/@xlink:href, '#') and document(substring-before(" + propi.qname()
				+ "/@xlink:href,'#'))/id(substring-after(" + propi.qname() + "/@xlink:href,'#')))");
		schDoc.addAssertion(cibase, xpath, "Code list value exists");

		// assert that the remote resource has the correct element based
		// on its representation
		s = typeCi.taggedValue("codeListRepresentation");

		if (StringUtils.isBlank(s) || s.equalsIgnoreCase("application/gml+xml;version=3.2")) {
		    xpath = new XpathFragment(0, "(not contains(" + propi.qname() + "/@xlink:href, '#') and document("
			    + propi.qname() + "/@xlink:href)/gml:Definition) or (contains(" + propi.qname()
			    + "/@xlink:href, '#') and document(substring-before(" + propi.qname()
			    + "/@xlink:href,'#'))/id(substring-after(" + propi.qname()
			    + "/@xlink:href,'#'))[local-name()='Definiton' and namespace-uri()='http://www.opengis.net/gml/3.2'])");
		    schDoc.addAssertion(cibase, xpath, "Code list dictionary is represented using GML 3.2");

		} else if (s.equalsIgnoreCase("application/rdf+xml")) {

		    /*
		     * FIXME 2018-01-15 JE: This XPath expression will probably be insufficient, due
		     * to the different ways RDFdata can be encoded in RDF/XML. The RDF encoding may
		     * not provide a target for the id() function. But what's more important, the
		     * RDF encoding may not have 'Concept' elements. Instead, it could have
		     * rdf:Description elements (with @about being the identifier of the code list
		     * value) with nested rdf:type
		     * having @rdf:resource=http://www.w3.org/2004/02/skos/ core# Concept. The
		     * development of a better XPath expression requires further analysis and
		     * testing.
		     */

		    xpath = new XpathFragment(0, "(not contains(" + propi.qname() + "/@xlink:href, '#') and document("
			    + propi.qname() + "/@xlink:href)/skos:Concept) or (contains(" + propi.qname()
			    + "/@xlink:href, '#') and document(substring-before(" + propi.qname()
			    + "/@xlink:href,'#'))/id(substring-after(" + propi.qname()
			    + "/@xlink:href,'#'))[local-name()='Concept' and namespace-uri()='http://www.w3.org/2004/02/skos/core#'])");
		    schDoc.addAssertion(cibase, xpath, "Code list dictionary is represented using SKOS");
		}

	    } else if (propi.inClass().matches("rule-xsd-cls-codelist-asDictionary")
		    && (typeCi.asDictionary() || typeCi.asDictionaryGml33())) {

		/*
		 * The property is encoded using the GML 3.2 encoding rule, and the value type
		 * is a code list that is encoded as dictionary.
		 */

		if (s != null && !s.isEmpty()) {

		    xpath = new XpathFragment(0, propi.qname() + "/@codeSpace='" + s + "'");
		    schDoc.addAssertion(cibase, xpath, "Code space is '" + s + "'");

		    // assert the existence of the code list
		    String s2 = schDoc.determineCodeListValuePattern(typeCi, "{codeList}/{value}");
		    s2 = "concat('" + s2.replace("{codeList}", s).replace("{value}", "',.)");
		    xpath = new XpathFragment(0,
			    "(not contains('" + s2 + "', '#') and document('" + s2 + "')) or (contains('" + s2
				    + "', '#') and document(substring-before('" + s2 + "','#'))/id(substring-after('"
				    + s2 + "','#')))");
		    schDoc.addAssertion(cibase, xpath, "Code list value exists");

		    // assert that the remote resource has the correct element
		    // based on its representation
		    s = typeCi.taggedValue("codeListRepresentation");
		    if (StringUtils.isBlank(s) || s.equalsIgnoreCase("application/gml+xml;version=3.2")) {
			xpath = new XpathFragment(0, "(not contains('" + s2 + "', '#') and document('" + s2
				+ "')/gml:Definition) or (contains('" + s2 + "', '#') and document(substring-before('"
				+ s2 + "','#'))/id(substring-after('" + s2
				+ "','#'))[local-name()='Definiton' and namespace-uri()='http://www.opengis.net/gml/3.2'])");
			schDoc.addAssertion(cibase, xpath, "Code list dictionary is represented using GML 3.2");
		    } else if (s.equalsIgnoreCase("application/rdf+xml")) {
			xpath = new XpathFragment(0, "(not contains('" + s2 + "', '#') and document('" + s2
				+ "')/skos:Concept) or (contains('" + s2 + "', '#') and document(substring-before('"
				+ s2 + "','#'))/id(substring-after('" + s2
				+ "','#'))[local-name()='Concept' and namespace-uri()='http://www.w3.org/2004/02/skos/core#'])");
			schDoc.addAssertion(cibase, xpath, "Code list dictionary is represented using SKOS");
		    }
		}
	    }

	} else if (schDoc != null && cibase.matches("rule-xsd-cls-codelist-constraints2")) {

	    if (propi.matches("rule-xsd-all-propertyAssertion-ignoreProhibited")
		    && "true".equalsIgnoreCase(propi.taggedValue("prohibitedInProfile"))) {
		/*
		 * The property is prohibited in the profile schema. Do not create code list
		 * checks for this property.
		 */
		return;
	    }

	    /*
	     * 2018-01-17 JE: rule-xsd-cls-codelist-constraints2 represents a revision of
	     * rule-xsd-cls-codelist-constraints to make use of XPath 2.0 in the xslt2 query
	     * binding for Schematron (which has been standardized with ISO 19757-3:2016).
	     */

	    /*
	     * 2018-01-15 JE: Note that using a function call (for example 'id(...)' - as
	     * used in rule-xsd-cls-codelist-constraints) within a location step is not
	     * supported by (the syntax of) XPath 1.0. Also see comments from Dimitre
	     * Novatchev and Michael Kay on
	     * https://stackoverflow.com/questions/333249/how-to-apply-the-
	     * xpath-function-substring-after and
	     * https://www.oxygenxml.com/archives/xsl-list/200603/msg00610. html. However,
	     * XPath 2.0 supports this.
	     * 
	     * Furthermore, we would like to make use of XPath 2.0 features such as
	     * for-loops and quantifications with variable declarations (to avoid repeated
	     * computations, and to support predicates on elements of an external code list
	     * document that are based on values (e.g. code list values) of the original
	     * document.
	     * 
	     * Thus, the queryBinding of the schematron schema must be set to (at least)
	     * xslt2.
	     */
	    schDoc.setQueryBinding("xslt2");

	    String clTV = typeCi.taggedValue("codeList");
	    if (StringUtils.isBlank(clTV)) {
		clTV = typeCi.taggedValue("vocabulary");
	    }

	    if (StringUtils.isBlank(clTV)) {
		result.addWarning(this, 1001, typeCi.name());
	    }

	    String clRepTV = typeCi.taggedValue("codeListRepresentation");
	    if (StringUtils.isBlank(clRepTV) && codeListRepresentationTVFallback != null) {
		clRepTV = codeListRepresentationTVFallback;
	    }

	    /*
	     * NOTE: application/x.iso19139+xml is a preliminary identifier for an XML
	     * document that contains an ISO 19139 code list dictionary (either a
	     * gmx:CodeListDictionary or a gmx:ML_CodeListDictionary).
	     */
	    if (StringUtils.isNotBlank(clRepTV) && !(clRepTV.equalsIgnoreCase("application/gml+xml;version=3.2")
		    || clRepTV.equalsIgnoreCase("application/x.iso19139+xml"))) {
		result.addWarning(this, 1000, clRepTV, typeCi.name());
	    }

	    if (typeCi.matches("rule-xsd-cls-standard-19139-property-types")) {

		/*
		 * ISO 19139:2007 defines the meaning of @codeList as: "The codeList attribute
		 * contains a URL that references a codeList definition within a registry or a
		 * codeList catalogue." In other words, @codeList references the code list
		 * element (not an element representing a code). The meaning of @codeListValue
		 * is: "The codeListValue attribute carries the identifier of the codeList value
		 * definition."
		 * 
		 * So there's essentially no reason to combine @codeList and @codeListValue
		 * based upon some codeListValuePattern.
		 * 
		 * Since @codeList references a code list dictionary (at the moment this
		 * conversion rule only supports a GML or ISO 19139 code list dictionary as the
		 * representation of a code list), it already includes the fragment identifier
		 * to identify the dictionary, if necessary - for example if the dictionary is
		 * part of a catalogue (like a gmx:CT_CodelistCatalogue). That is sufficient to
		 * retrieve the dictionary element.
		 * 
		 * The value of @codeListValue can then be used to search the Definition element
		 * that has @codeListValue as identifier element.
		 * 
		 * @codeSpace should be irrelevant, since @codeListValue is independent of the
		 * code space.
		 */
		String codeListExpr = "*/@codeList";
		String codeListValueExpr = "*/@codeListValue";

		// Assert that the code list is used.
		if (StringUtils.isNotBlank(clTV)) {

		    XpathFragment xpathCodeListUse = new XpathFragment(0,
			    "every $codeListUrl in " + codeListExpr + " satisfies " + codeListExpr + "='" + clTV + "'",
			    XpathType.BOOLEAN);
		    schDoc.addAssertionForExplicitProperty(cibase, propi, true, xpathCodeListUse,
			    "Code list shall be '" + clTV + "'");
		}

		/*
		 * Assert the existence of the code list.
		 */
		if (StringUtils.isBlank(clRepTV) || clRepTV.equalsIgnoreCase("application/gml+xml;version=3.2")
			|| clRepTV.equalsIgnoreCase("application/x.iso19139+xml")) {

		    schDoc.registerNamespace("gml");
		    XpathFragment xpathCodeListExists = new XpathFragment(0, "every $codeListUrl in " + codeListExpr
			    + " satisfies " + "(not(contains($codeListUrl, '#')) and doc-available($codeListUrl)) "
			    + "or (contains($codeListUrl, '#') and doc-available(substring-before($codeListUrl,'#')) and boolean("
			    + "for $codelistDoc in doc(substring-before($codeListUrl,'#')) "
			    + "return exists($codelistDoc//*[@gml:id = substring-after($codeListUrl,'#')])))",
			    XpathType.BOOLEAN);

		    schDoc.addAssertionForExplicitProperty(cibase, propi, true, xpathCodeListExists,
			    "Code list shall exist");
		}

		/*
		 * Assert that the remote resource has the correct element based on its
		 * representation.
		 */
		if (StringUtils.isBlank(clRepTV) || clRepTV.equalsIgnoreCase("application/gml+xml;version=3.2")) {

		    schDoc.registerNamespace("gml");
		    XpathFragment xpathCodeListCorrectRepresentation = new XpathFragment(0, "every $codeListUrl in "
			    + codeListExpr + " satisfies "
			    + "(not(contains($codeListUrl, '#')) and doc-available($codeListUrl) and doc($codeListUrl)/gml:Dictionary) "
			    + "or (contains($codeListUrl, '#') and doc-available(substring-before($codeListUrl,'#')) and boolean("
			    + "for $codelistDoc in doc(substring-before($codeListUrl,'#')) "
			    + "return exists($codelistDoc//gml:Dictionary[@gml:id = substring-after($codeListUrl,'#')])))",
			    XpathType.BOOLEAN);

		    schDoc.addAssertionForExplicitProperty(cibase, propi, true, xpathCodeListCorrectRepresentation,
			    "Code list dictionary shall be represented using a GML 3.2 Dictionary");

		} else if (clRepTV.equalsIgnoreCase("application/x.iso19139+xml")) {

		    schDoc.registerNamespace("gml");
		    schDoc.registerNamespace("gmx");

		    XpathFragment xpathCodeListCorrectRepresentation = new XpathFragment(0, "every $codeListUrl in "
			    + codeListExpr + " satisfies "
			    + "(not(contains($codeListUrl, '#')) and doc-available($codeListUrl) and doc($codeListUrl)/*[namespace-uri() = 'http://www.isotc211.org/2005/gmx' and (local-name() = 'ML_CodeListDictionary' or local-name() = 'CodeListDictionary')]) "
			    + "or (contains($codeListUrl, '#') and doc-available(substring-before($codeListUrl,'#')) and boolean("
			    + "for $codelistDoc in doc(substring-before($codeListUrl,'#')) "
			    + "return exists($codelistDoc//*[namespace-uri() = 'http://www.isotc211.org/2005/gmx' and (local-name() = 'ML_CodeListDictionary' or local-name() = 'CodeListDictionary') and @gml:id = substring-after($codeListUrl,'#')])))",
			    XpathType.BOOLEAN);

		    schDoc.addAssertionForExplicitProperty(cibase, propi, true, xpathCodeListCorrectRepresentation,
			    "Code list dictionary shall be represented using an ISO 19139 CodeListDictionary");
		}

		/*
		 * Assert the existence of the code list value.
		 */
		XpathFragment xpathCodeListValueExists = null;

		if (StringUtils.isBlank(clRepTV) || clRepTV.equalsIgnoreCase("application/gml+xml;version=3.2")) {

		    schDoc.registerNamespace("gml");
		    xpathCodeListValueExists = new XpathFragment(0, "every $codeListUrl in " + codeListExpr + ", "
			    + "$codeListValue in " + codeListValueExpr + " satisfies "
			    + "(not(contains($codeListUrl, '#')) and doc-available($codeListUrl) and "
			    + "doc($codeListUrl)/*/gml:dictionaryEntry/gml:Definition[gml:identifier = $codeListValue]) "
			    + "or (contains($codeListUrl, '#') and doc-available(substring-before($codeListUrl,'#')) and "
			    + "doc(substring-before($codeListUrl,'#'))//*[@gml:id = substring-after($codeListUrl,'#')]"
			    + "/gml:dictionaryEntry/gml:Definition[gml:identifier = $codeListValue])",
			    XpathType.BOOLEAN);

		} else if (clRepTV.equalsIgnoreCase("application/x.iso19139+xml")) {

		    schDoc.registerNamespace("gml");
		    schDoc.registerNamespace("gmx");

		    xpathCodeListValueExists = new XpathFragment(0, "every $codeListUrl in " + codeListExpr + ", "
			    + "$codeListValue in " + codeListValueExpr + " satisfies "
			    + "(not(contains($codeListUrl, '#')) and doc-available($codeListUrl) and "
			    + "doc($codeListUrl)/*/gmx:codeEntry/*[namespace-uri() = 'http://www.isotc211.org/2005/gmx' and (local-name() = 'ML_CodeDefinition' or local-name() = 'CodeDefinition') and gml:identifier = $codeListValue]) "
			    + "or (contains($codeListUrl, '#') and doc-available(substring-before($codeListUrl,'#')) and "
			    + "doc(substring-before($codeListUrl,'#'))//*[@gml:id = substring-after($codeListUrl,'#')]"
			    + "/gmx:codeEntry/*[namespace-uri() = 'http://www.isotc211.org/2005/gmx' and (local-name() = 'ML_CodeDefinition' or local-name() = 'CodeDefinition') and gml:identifier = $codeListValue])",
			    XpathType.BOOLEAN);
		}

		if (xpathCodeListValueExists != null) {
		    schDoc.addAssertionForExplicitProperty(cibase, propi, true, xpathCodeListValueExists,
			    "Code list value shall exist");
		}

	    } else if (propi.inClass().matches("rule-xsd-cls-codelist-asDictionaryGml33")
		    && (typeCi.asDictionary() || typeCi.asDictionaryGml33())) {

		/*
		 * NOTE: The property is encoded according to the GML 3.3 encoding rule, and the
		 * value type is a code list that is encoded as dictionary, therefore propi is
		 * encoded with type gml:ReferenceType.
		 * 
		 * The xlink:href contains the URI for the dictionary item, i.e. the XML element
		 * that represents the code. If the URI does not contain a '#', the referenced
		 * resource only represents the dictionary item. Otherwise, the referenced
		 * resource contains the dictionary item but the item must be looked up by its
		 * id (that lookup depends on the representation; it can but does not need to be
		 * gml:id).
		 */

		String clRefExp = "@xlink:href";

		if (StringUtils.isNotBlank(clTV)) {

		    schDoc.registerNamespace("xlink");
		    XpathFragment xpathCLValUriStartsWith = new XpathFragment(0, "every $clValueUri in " + clRefExp
			    + " satisfies " + "starts-with(" + clRefExp + ",'" + clTV + "')", XpathType.BOOLEAN);
		    schDoc.addAssertionForExplicitProperty(cibase, propi, true, xpathCLValUriStartsWith,
			    "Code list value URI shall start with '" + clTV + "'");
		}

		/*
		 * Assert the existence of the code list value.
		 */
		if (StringUtils.isBlank(clRepTV) || clRepTV.equalsIgnoreCase("application/gml+xml;version=3.2")
			|| clRepTV.equalsIgnoreCase("application/x.iso19139+xml")) {

		    schDoc.registerNamespace("gml");
		    schDoc.registerNamespace("xlink");

		    XpathFragment xpathCLValueExists = new XpathFragment(0, "every $clValueUri in " + clRefExp
			    + " satisfies " + "(not(contains($clValueUri, '#')) and doc-available($clValueUri)) "
			    + "or (contains($clValueUri, '#') and doc-available(substring-before($clValueUri,'#')) and boolean("
			    + "for $codelistDoc in doc(substring-before($clValueUri,'#')) "
			    + "return exists($codelistDoc//*[@gml:id = substring-after($clValueUri,'#')])))",
			    XpathType.BOOLEAN);

		    schDoc.addAssertionForExplicitProperty(cibase, propi, true, xpathCLValueExists,
			    "Code list value shall exist");
		}

		/*
		 * Assert that the remote resource has the correct element based on its
		 * representation.
		 */
		if (StringUtils.isBlank(clRepTV) || clRepTV.equalsIgnoreCase("application/gml+xml;version=3.2")) {

		    schDoc.registerNamespace("gml");
		    schDoc.registerNamespace("xlink");
		    XpathFragment xpathCLValueCorrectRepresentation = new XpathFragment(0, "every $clValueUri in "
			    + clRefExp + " satisfies "
			    + "(not(contains($clValueUri, '#')) and doc-available($clValueUri) and doc($clValueUri)/gml:Definition) "
			    + "or (contains($clValueUri, '#') and doc-available(substring-before($clValueUri,'#')) and boolean("
			    + "for $codelistDoc in doc(substring-before($clValueUri,'#')) "
			    + "return exists($codelistDoc//gml:Definition[@gml:id = substring-after($clValueUri,'#')])))",
			    XpathType.BOOLEAN);

		    schDoc.addAssertionForExplicitProperty(cibase, propi, true, xpathCLValueCorrectRepresentation,
			    "Code list value shall be represented using GML 3.2 (gml:Definition)");

		} else if (clRepTV.equalsIgnoreCase("application/x.iso19139+xml")) {

		    schDoc.registerNamespace("gml");
		    schDoc.registerNamespace("gmx");
		    schDoc.registerNamespace("xlink");

		    XpathFragment xpathCLValueCorrectRepresentation = new XpathFragment(0, "every $clValueUri in "
			    + clRefExp + " satisfies "
			    + "(not(contains($clValueUri, '#')) and doc-available($clValueUri) and doc($clValueUri)/*[namespace-uri() = 'http://www.isotc211.org/2005/gmx' and (local-name() = 'ML_CodeDefinition' or local-name() = 'CodeDefinition')]) "
			    + "or (contains($clValueUri, '#') and doc-available(substring-before($clValueUri,'#')) and boolean("
			    + "for $codelistDoc in doc(substring-before($clValueUri,'#')) "
			    + "return exists($codelistDoc//*[namespace-uri() = 'http://www.isotc211.org/2005/gmx' and (local-name() = 'ML_CodeDefinition' or local-name() = 'CodeDefinition') and @gml:id = substring-after($clValueUri,'#')])))",
			    XpathType.BOOLEAN);

		    schDoc.addAssertionForExplicitProperty(cibase, propi, true, xpathCLValueCorrectRepresentation,
			    "Code list dictionary item shall be represented using an ISO 19139 CodeDefinition");
		}

	    } else if (propi.inClass().matches("rule-xsd-cls-codelist-asDictionary")
		    && (typeCi.asDictionary() || typeCi.asDictionaryGml33())) {

		/*
		 * NOTE: The property is encoded according to the GML 3.2 encoding rule, and the
		 * value type is a code list that is encoded as dictionary, therefore propi is
		 * encoded with type gml:CodeType.
		 * 
		 * The element value contains the code value, while the optional @codeSpace
		 * contains the URI to the dictionary. This is quite similar to the ISO 19139
		 * based encoding of ci (however, the optional @codeSpace requires specific
		 * checks).
		 */

		if (StringUtils.isNotBlank(clTV)) {

		    String clRefExp = "if (@codeSpace) then @codeSpace else '" + clTV + "'";

		    // Assert that @codeSpace, if set, is the code list
		    XpathFragment xpathCodeListUse = new XpathFragment(0,
			    "if (not(@codeSpace)) then true() else " + "@codeSpace='" + clTV + "'");
		    schDoc.addAssertionForExplicitProperty(cibase, propi, true, xpathCodeListUse,
			    "Code space, if set, shall be '" + clTV + "'");

		    /*
		     * Assert the existence of the code list.
		     */
		    if (StringUtils.isBlank(clRepTV) || clRepTV.equalsIgnoreCase("application/gml+xml;version=3.2")
			    || clRepTV.equalsIgnoreCase("application/x.iso19139+xml")) {

			schDoc.registerNamespace("gml");
			XpathFragment xpathCodeListExists = new XpathFragment(0, "every $codeListUrl in " + clRefExp
				+ " satisfies " + "(not(contains($codeListUrl, '#')) and doc-available($codeListUrl)) "
				+ "or (contains($codeListUrl, '#') and doc-available(substring-before($codeListUrl,'#')) and boolean("
				+ "for $codelistDoc in doc(substring-before($codeListUrl,'#')) "
				+ "return exists($codelistDoc//*[@gml:id = substring-after($codeListUrl,'#')])))",
				XpathType.BOOLEAN);

			schDoc.addAssertionForExplicitProperty(cibase, propi, true, xpathCodeListExists,
				"Code list shall exist");
		    }

		    /*
		     * Assert that the remote resource has the correct element based on its
		     * representation.
		     */
		    if (StringUtils.isBlank(clRepTV) || clRepTV.equalsIgnoreCase("application/gml+xml;version=3.2")) {

			schDoc.registerNamespace("gml");
			XpathFragment xpathCodeListCorrectRepresentation = new XpathFragment(0, "every $codeListUrl in "
				+ clRefExp + " satisfies "
				+ "(not(contains($codeListUrl, '#')) and doc-available($codeListUrl) and doc($codeListUrl)/gml:Dictionary) "
				+ "or (contains($codeListUrl, '#') and doc-available(substring-before($codeListUrl,'#')) and boolean("
				+ "for $codelistDoc in doc(substring-before($codeListUrl,'#')) "
				+ "return exists($codelistDoc//gml:Dictionary[@gml:id = substring-after($codeListUrl,'#')])))",
				XpathType.BOOLEAN);

			schDoc.addAssertionForExplicitProperty(cibase, propi, true, xpathCodeListCorrectRepresentation,
				"Code list dictionary shall be represented using a GML 3.2 Dictionary");

		    } else if (clRepTV.equalsIgnoreCase("application/x.iso19139+xml")) {

			schDoc.registerNamespace("gml");
			schDoc.registerNamespace("gmx");

			XpathFragment xpathCodeListCorrectRepresentation = new XpathFragment(0, "every $codeListUrl in "
				+ clRefExp + " satisfies "
				+ "(not(contains($codeListUrl, '#')) and doc-available($codeListUrl) and doc($codeListUrl)/*[namespace-uri() = 'http://www.isotc211.org/2005/gmx' and (local-name() = 'ML_CodeListDictionary' or local-name() = 'CodeListDictionary')]) "
				+ "or (contains($codeListUrl, '#') and doc-available(substring-before($codeListUrl,'#')) and boolean("
				+ "for $codelistDoc in doc(substring-before($codeListUrl,'#')) "
				+ "return exists($codelistDoc//*[namespace-uri() = 'http://www.isotc211.org/2005/gmx' and (local-name() = 'ML_CodeListDictionary' or local-name() = 'CodeListDictionary') and @gml:id = substring-after($codeListUrl,'#')])))",
				XpathType.BOOLEAN);

			schDoc.addAssertionForExplicitProperty(cibase, propi, true, xpathCodeListCorrectRepresentation,
				"Code list dictionary shall be represented using an ISO 19139 CodeListDictionary");
		    }

		    /*
		     * Assert the existence of the code list value.
		     */
		    XpathFragment xpathCodeListValueExists = null;

		    if (StringUtils.isBlank(clRepTV) || clRepTV.equalsIgnoreCase("application/gml+xml;version=3.2")) {

			schDoc.registerNamespace("gml");
			xpathCodeListValueExists = new XpathFragment(0, "every " + "$codeListUrl in " + clRefExp + ", "
				+ "$codeListValue in ./text()" + " satisfies"
				+ "(not(contains($codeListUrl, '#')) and doc-available($codeListUrl) and "
				+ "doc($codeListUrl)/*/gml:dictionaryEntry/gml:Definition[gml:identifier = $codeListValue]) "
				+ "or (contains($codeListUrl, '#') and doc-available(substring-before($codeListUrl,'#')) and "
				+ "doc(substring-before($codeListUrl,'#'))//*[@gml:id = substring-after($codeListUrl,'#')]"
				+ "/gml:dictionaryEntry/gml:Definition[gml:identifier = $codeListValue])",
				XpathType.BOOLEAN);

		    } else if (clRepTV.equalsIgnoreCase("application/x.iso19139+xml")) {

			schDoc.registerNamespace("gml");
			schDoc.registerNamespace("gmx");

			xpathCodeListValueExists = new XpathFragment(0, "every " + "$codeListUrl in " + clRefExp + ", "
				+ "$codeListValue in ./text()" + " satisfies"
				+ "(not(contains($codeListUrl, '#')) and doc-available($codeListUrl) and "
				+ "doc($codeListUrl)/*/gmx:codeEntry/*[namespace-uri() = 'http://www.isotc211.org/2005/gmx' and (local-name() = 'ML_CodeDefinition' or local-name() = 'CodeDefinition') and gml:identifier = $codeListValue]) "
				+ "or (contains($codeListUrl, '#') and doc-available(substring-before($codeListUrl,'#')) and "
				+ "doc(substring-before($codeListUrl,'#'))//*[@gml:id = substring-after($codeListUrl,'#')]"
				+ "/gmx:codeEntry/*[namespace-uri() = 'http://www.isotc211.org/2005/gmx' and (local-name() = 'ML_CodeDefinition' or local-name() = 'CodeDefinition') and gml:identifier = $codeListValue])",
				XpathType.BOOLEAN);
		    }

		    if (xpathCodeListValueExists != null) {
			schDoc.addAssertionForExplicitProperty(cibase, propi, true, xpathCodeListValueExists,
				"Code list value shall exist");
		    }
		}
	    }
	}
    }

    private void addNilReason(Element e) {
	addAttribute(e, "name", "nilReason");
	if (options.GML_NS.equals("http://www.opengis.net/gml/3.2"))
	    addAttribute(e, "type", "gml:NilReasonType");
	else if (options.GML_NS.equals("http://www.opengis.net/gml"))
	    addAttribute(e, "type", "gml:NullType");
	addImport("gml", options.fullNamespace("gml"));
    }

    private void addName(Element e) {
	addAttribute(e, "name", "name");
	addAttribute(e, "type", "NCName");
	addAttribute(e, "use", "required");
    }

    /** Create a embedded property type */
    private boolean addAnonymousPropertyType(Element e, PropertyInfo propi, ClassInfo targetAssociationClass,
	    String targetElement, HashSet<ClassInfo> types, boolean valueIsMetadata) {

	ClassInfo cibase = propi.inClass();
	if (cibase == null) {
	    MessageContext mc = result.addError(this, 157, propi.name());
	    if (mc != null)
		mc.addDetail(null, 400, "Property", propi.fullName());
	    return false;
	}

	boolean addMetadata = false;
	if (propi.propertyMetadata() && propi.matches("rule-xsd-prop-metadata")) {
	    addMetadata = true;
	}

	// First address cases where no property type is necessary or only a
	// very simple one

	if ((propi.isMetadata() || valueIsMetadata) && propi.matches("rule-xsd-prop-metadata-gmlsf-byReference")) {
	    addAttribute(e, "type", "gml:ReferenceType");
	    addImport("gml", options.fullNamespace("gml"));
	    return false;
	}

	if (propi.matches("rule-xsd-prop-inlineOrByReference") && propi.inlineOrByReference().equals("byreference")) {

	    if (cibase.matches("rule-xsd-cls-no-gml-types")) {
		Element e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "complexType");
		e.appendChild(e1);
		Element e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "sequence");
		e1.appendChild(e4);
		e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "attributeGroup");
		e1.appendChild(e4);
		addAttribute(e4, "ref", "xlink:simpleAttrs");
		addImport("xlink", options.fullNamespace("xlink"));
		return false;
	    }

	    if (cibase.matches("rule-xsd-cls-standard-swe-property-types")) {
		addAttribute(e, "type", "swe:ReferenceType");
		addImport("swe", options.fullNamespace("swe"));
		return false;
	    }

	    boolean multiplicityAlreadySet = false;
	    if (options.GML_NS.equals("http://www.opengis.net/gml")) {

		boolean addNilReason = false;

		if (propi.nilReasonAllowed() || (propi.voidable()) && propi.matches("rule-xsd-prop-nillable")) {
		    addNilReason = true;
		}

		if (cibase.matches("rule-xsd-all-gml21")) {
		    Element e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "complexType");
		    e.appendChild(e1);
		    Element e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "sequence");
		    e1.appendChild(e4);
		    e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "attributeGroup");
		    e1.appendChild(e4);
		    addAttribute(e4, "ref", "gml:AssociationAttributeGroup");

		    if (addNilReason) {
			Element e5 = document.createElementNS(Options.W3C_XML_SCHEMA, "attribute");
			addAttribute(e5, "name", "nilReason");
			addAttribute(e5, "type", "gml:NullType");
			e1.appendChild(e5);
		    }
		    if (addMetadata) {
			addMetadata(e1);
		    }
		} else if (qualifiers(propi) != null || addNilReason || addMetadata) {
		    Element e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "complexType");
		    e.appendChild(e1);
		    Element e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "complexContent");
		    e1.appendChild(e4);
		    Element e3 = document.createElementNS(Options.W3C_XML_SCHEMA, "extension");
		    e4.appendChild(e3);
		    addAttribute(e3, "base", "gml:ReferenceType");

		    if (processQualifiers(e3, propi)) {
			addAttribute(e, "minOccurs", "0");
			addAttribute(e, "maxOccurs", "unbounded");
			multiplicityAlreadySet = true;
		    }

		    if (addNilReason) {
			Element e5 = document.createElementNS(Options.W3C_XML_SCHEMA, "attribute");
			addAttribute(e5, "name", "nilReason");
			addAttribute(e5, "type", "gml:NullType");
			e3.appendChild(e5);
		    }
		    if (addMetadata) {
			addMetadata(e3);
		    }
		} else {
		    addAttribute(e, "type", "gml:ReferenceType");
		}

		addImport("gml", options.fullNamespace("gml"));
		return multiplicityAlreadySet;
	    }

	    if (options.GML_NS.equals("http://www.opengis.net/gml/3.2")) {
		if (propi.isMetadata() || valueIsMetadata) {
		    // nothing to do, special type construction necessary
		} else if (propi.isAggregation() || propi.isComposition()) {
		    // nothing to do, special type construction necessary
		} else if (qualifiers(propi) != null || addMetadata) {
		    Element e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "complexType");
		    e.appendChild(e1);
		    Element e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "complexContent");
		    e1.appendChild(e4);
		    Element e3 = document.createElementNS(Options.W3C_XML_SCHEMA, "extension");
		    e4.appendChild(e3);
		    addAttribute(e3, "base", "gml:ReferenceType");

		    if (processQualifiers(e3, propi)) {
			addAttribute(e, "minOccurs", "0");
			addAttribute(e, "maxOccurs", "unbounded");
			multiplicityAlreadySet = true;
		    }
		    if (addMetadata) {
			addMetadata(e3);
		    }
		    return multiplicityAlreadySet;
		} else {
		    addAttribute(e, "type", "gml:ReferenceType");
		    addImport("gml", options.fullNamespace("gml"));
		    return multiplicityAlreadySet;
		}
	    }
	}

	// We need to create a type

	Element type = document.createElementNS(Options.W3C_XML_SCHEMA, "complexType");
	e.appendChild(type);

	Element typeOrExtension;
	if ((propi.isMetadata() || valueIsMetadata) && options.GML_NS.equals("http://www.opengis.net/gml/3.2")
		&& cibase.matches("rule-xsd-cls-standard-gml-property-types")) {
	    Element e2 = document.createElementNS(Options.W3C_XML_SCHEMA, "complexContent");
	    type.appendChild(e2);
	    typeOrExtension = document.createElementNS(Options.W3C_XML_SCHEMA, "extension");
	    e2.appendChild(typeOrExtension);
	    addAttribute(typeOrExtension, "base", "gml:AbstractMetadataPropertyType");
	} else if ((propi.isAggregation() || propi.isComposition())
		&& (propi.categoryOfValue() == Options.GMLOBJECT || propi.categoryOfValue() == Options.FEATURE)
		&& options.GML_NS.equals("http://www.opengis.net/gml/3.2")
		&& cibase.matches("rule-xsd-cls-standard-gml-property-types")
		&& !cibase.matches("rule-xsd-cls-no-gml-types")) {
	    Element e2 = document.createElementNS(Options.W3C_XML_SCHEMA, "complexContent");
	    type.appendChild(e2);
	    typeOrExtension = document.createElementNS(Options.W3C_XML_SCHEMA, "extension");
	    e2.appendChild(typeOrExtension);
	    if (propi.categoryOfValue() == Options.FEATURE)
		addAttribute(typeOrExtension, "base", "gml:AbstractFeatureMemberType");
	    else
		addAttribute(typeOrExtension, "base", "gml:AbstractMemberType");
	} else {
	    typeOrExtension = type;
	}

	HashSet<ClassInfo> instatiableMixinSubclasses = null;
	ClassInfo instantiableMixinSubclassesTypeCi = targetAssociationClass != null ? targetAssociationClass
		: model.classByIdOrName(propi.typeInfo());
	if (instantiableMixinSubclassesTypeCi != null
		&& instantiableMixinSubclassesTypeCi.matches("rule-xsd-cls-mixin-classes-non-mixin-supertypes")) {
	    instatiableMixinSubclasses = subtypesOfMixins(instantiableMixinSubclassesTypeCi, false);
	}

	Element sequenceOrChoice;
	if (propi.matches("rule-xsd-prop-inlineOrByReference") && propi.inlineOrByReference().equals("byreference")) {
	    sequenceOrChoice = document.createElementNS(Options.W3C_XML_SCHEMA, "sequence");
	    typeOrExtension.appendChild(sequenceOrChoice);

	} else if (instatiableMixinSubclasses != null && !instatiableMixinSubclasses.isEmpty()) {

	    sequenceOrChoice = document.createElementNS(Options.W3C_XML_SCHEMA, "choice");
	    typeOrExtension.appendChild(sequenceOrChoice);

	    Element e5 = document.createElementNS(Options.W3C_XML_SCHEMA, "element");
	    sequenceOrChoice.appendChild(e5);
	    String s = elementName(instantiableMixinSubclassesTypeCi, true);
	    if (s != null) {
		addAttribute(e5, "ref", s);
	    }
	    addElements(sequenceOrChoice, instatiableMixinSubclasses);

	} else if (types != null) {
	    sequenceOrChoice = document.createElementNS(Options.W3C_XML_SCHEMA, "choice");
	    typeOrExtension.appendChild(sequenceOrChoice);
	    addElements(sequenceOrChoice, types);

	} else if (targetElement != null) {
	    sequenceOrChoice = document.createElementNS(Options.W3C_XML_SCHEMA, "sequence");
	    typeOrExtension.appendChild(sequenceOrChoice);
	    Element e5 = document.createElementNS(Options.W3C_XML_SCHEMA, "element");
	    sequenceOrChoice.appendChild(e5);
	    addAttribute(e5, "ref", targetElement);
	    int idx = targetElement.indexOf(":");
	    if (idx > 0) {
		String nsabr = targetElement.substring(0, idx);
		addImport(nsabr, options.fullNamespace(nsabr));
	    }

	} else {
	    MessageContext mc = result.addError(this, 116, propi.name());
	    if (mc != null)
		mc.addDetail(null, 400, "Property", propi.fullName());
	    return false;
	}

	boolean nilReasonAdded = false;
	ClassInfo cit = model.classById(propi.typeInfo().id);

	boolean multiplicityAlreadySet = false;
	if (asArray(propi)) {
	    addMinMaxOccurs(sequenceOrChoice, propi.cardinality());
	} else if (((cit == null || classHasIdentity(cit)) && propi.matches("rule-xsd-prop-inlineOrByReference")
		&& !propi.inlineOrByReference().equals("inline"))
		|| (!propi.matches("rule-xsd-prop-inlineOrByReference") && cit != null && classHasIdentity(cit))) {
	    if (!propi.inlineOrByReference().equals("byreference"))
		addAttribute(sequenceOrChoice, "minOccurs", "0");

	    Element e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "attributeGroup");
	    typeOrExtension.appendChild(e4);

	    if (cibase.matches("rule-xsd-cls-no-gml-types")) {
		addAttribute(e4, "ref", "xlink:simpleAttrs");
	    } else if (cibase.matches("rule-xsd-cls-standard-swe-property-types")) {
		addAttribute(e4, "ref", "swe:AssociationAttributeGroup");
	    } else {
		addAttribute(e4, "ref", "gml:AssociationAttributeGroup");
		if (options.GML_NS.equals("http://www.opengis.net/gml/3.2"))
		    nilReasonAdded = true;
	    }
	}

	if (!(propi.isMetadata() || valueIsMetadata) && !propi.isAggregation() && !propi.isComposition()
		&& options.GML_NS.equals("http://www.opengis.net/gml/3.2") && (cit != null && classHasIdentity(cit))
		&& cibase.matches("rule-xsd-cls-standard-gml-property-types")) {
	    Element e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "attributeGroup");
	    typeOrExtension.appendChild(e4);
	    addAttribute(e4, "ref", "gml:OwnershipAttributeGroup");
	}

	if ((propi.nilReasonAllowed() || (propi.voidable()) && propi.matches("rule-xsd-prop-nillable"))
		&& !nilReasonAdded) {
	    Element e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "attribute");
	    typeOrExtension.appendChild(e4);
	    addAttribute(e4, "name", "nilReason");
	    if (options.GML_NS.equals("http://www.opengis.net/gml/3.2")) {
		addAttribute(e4, "type", "gml:NilReasonType");
	    } else if (options.GML_NS.equals("http://www.opengis.net/gml")) {
		addAttribute(e4, "type", "gml:NullType");
	    }
	}

	if (propi.matches("rule-xsd-prop-soft-typed")) {
	    String s = propi.taggedValue("soft-typed");
	    if ("true".equalsIgnoreCase(s)) {
		Element e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "attribute");
		typeOrExtension.appendChild(e4);
		addName(e4);
	    }
	}

	if (processQualifiers(typeOrExtension, propi)) {
	    addAttribute(e, "minOccurs", "0");
	    addAttribute(e, "maxOccurs", "unbounded");
	    multiplicityAlreadySet = true;
	}

	if (addMetadata) {
	    addMetadata(typeOrExtension);
	}

	if (cibase.matches("rule-xsd-cls-no-gml-types")) {
	    addImport("xlink", options.fullNamespace("xlink"));
	} else if (cibase.matches("rule-xsd-cls-standard-swe-property-types")) {
	    addImport("swe", options.fullNamespace("swe"));
	} else {
	    addImport("gml", options.fullNamespace("gml"));
	}
	return multiplicityAlreadySet;
    }

    private void addMetadata(Element e) {
	Element m = document.createElementNS(Options.W3C_XML_SCHEMA, "attribute");
	e.appendChild(m);
	addAttribute(m, "name", "metadata");
	addAttribute(m, "type", "anyURI");
    }

    /** Strip quotes. */
    private String stripQuotes(String s) {
	StringBuffer str = new StringBuffer();

	int len = s != null ? s.length() : 0;
	for (int i = 0; i < len; i++) {
	    char ch = s.charAt(i);
	    switch (ch) {
	    case '"': {
		break;
	    }
	    default: {
		str.append(ch);
	    }
	    }
	}

	return str.toString();

    } // stripQuotes(String):String

    public void addInclude(XsdDocument xsd) {
	if (xsd == this) {
	    return;
	}
	boolean found = false;
	for (String f : includes) {
	    if (f.equals(xsd.name)) {
		found = true;
		break;
	    }
	}
	if (!found) {
	    includes.add(xsd.name);
	}
    }

    public void addImport(String nsabr, String ns) {
	if (ns == null || ns.equals(targetNamespace)) {
	    return;
	}

	boolean found = false;
	for (String nsx : imports) {
	    if (nsx.equals(ns)) {
		found = true;
		break;
	    }
	}
	if (!found) {
	    imports.add(ns);
	    result.addDebug(this, 10021, ns);
	    if (nsabr != null) {
		addAttribute(root, "xmlns:" + nsabr, ns);
	    }
	}
    }

    /**
     * Dump XML Schema file
     * 
     * @throws Exception tbd
     */
    public void printFile() throws Exception {
	if (printed) {
	    return;
	}

	Node anchor = rootAnnotation;

	Element e;
	Collections.sort(includes);
	for (String sl : includes) {
	    e = document.createElementNS(Options.W3C_XML_SCHEMA, "include");
	    addAttribute(e, "schemaLocation", sl);

	    if (anchor == null) {
		root.insertBefore(e, root.getFirstChild());
	    } else {
		root.insertBefore(e, anchor.getNextSibling());
	    }
	    anchor = e;
	}

	String loc;
	Collections.sort(imports);
	for (String s : imports) {
	    e = document.createElementNS(Options.W3C_XML_SCHEMA, "import");
	    addAttribute(e, "namespace", s);
	    loc = options.schemaLocationOfNamespace(s);
	    if (loc != null) {
		addAttribute(e, "schemaLocation", loc);
	    }

	    if (anchor == null) {
		root.insertBefore(e, root.getFirstChild());
	    } else {
		root.insertBefore(e, anchor.getNextSibling());
	    }
	    anchor = e;
	}

	// Check whether we can use the given output directory
	File outputDirectoryFile = new File(outputDirectory);
	boolean exi = outputDirectoryFile.exists();
	if (!exi) {
	    outputDirectoryFile.mkdirs();
	    exi = outputDirectoryFile.exists();
	}
	boolean dir = outputDirectoryFile.isDirectory();
	boolean wrt = outputDirectoryFile.canWrite();
	boolean rea = outputDirectoryFile.canRead();
	if (!exi || !dir || !wrt || !rea) {
	    result.addFatalError(this, 12, outputDirectory);
	    throw new ShapeChangeAbortException();
	}

	try {
	    String fname = outputDirectory + "/" + name;
	    new File(fname).getCanonicalPath();

	    XMLUtil.writeXml(document, new File(fname));
	} catch (ShapeChangeException ioe) {
	    result.addError(null, 171, name);
	}

	printed = true;
    }

    public boolean printed() {
	return printed;
    }

    /**
     * This method returns messages belonging to the XML Schema target by their
     * message number. The organization corresponds to the logic in module
     * ShapeChangeResult. All functions in that class, which require an message
     * number can be redirected to the function at hand.
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
	return prefix + "XML Schema Target: " + mess;
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
	    return "Context: class '$1$'";
	case 2:
	    return "Context: property '$1$'";

	case 12:
	    return "Directory named '$1$' does not exist or is not accessible.";

	case 116:
	    return "Target object element(s) missing in property type for property '$1$'.";
	case 119:
	    return "No element for type '$1$' is defined. Only object and data types are represented by elements.";
	case 122:
	    return "The type with the name '$1$' has no tagged value 'base' or valid supertype and cannot be mapped to a basic type.";
	case 123:
	    return "The type with the name '$1$' has no ID and cannot be mapped to a basic type.";
	case 124:
	    return "Failed to create basic type '$1$'.";
	case 128:
	    return "The property '$1$' cannot be assigned a type as it is mapped to an XML attribute, but the type is not a simple type.";
	case 129:
	    return "Union '$1$' as the value type of '$2$' could not be mapped as it does not contain the expected number of exactly one property to be encoded in the application schema.";
	case 130:
	    return "No type can be provided for the property '$1$'.";
	case 141:
	    return "The class '$1$' referenced from class '$2$' is not part of any package nor is it mapped to a well-known XML Schema type. The class is ignored.";
	case 145:
	    return "ADE class '$1$' cannot be suppressed, as it has no supertype.";
	case 154:
	    return "??No rule to name the '$1$' of class '$2$' is configured. Please check the current configuration.";
	case 155:
	    return "??No rule for a choice/sequence/all container for class '$1$' is configured, sequence is used. Please check the current configuration.";
	case 156:
	    return "??Failed to create enumeration type '$1$EnumerationType'.";
	case 157:
	    return "??Class of property '$1$' cannot be determined. The property is ignored.";
	case 158:
	    return "??MapEntry contains empty mapping target. Verify the configuration and look for 'fixme:fixme' in the created schemas.";
	case 166:
	    return "Class '$1$' cannot be mapped to an object element and is not included in the mapping of class '$2$'.";
	case 169:
	    return "The property '$1$' cannot be assigned a type as it is mapped to an XML Schema list attribute, but the type '$2$' is not a simple type.";
	case 170:
	    return "??The property '$1$' cannot be made an array propery as it is not restricted to inline content. Set 'inlineOrByReference' to 'inline' on the property.";
	case 172:
	    return "??The property '$1$' cannot be made an array propery as the type map does not specify an XML element for type '$2$'.";
	case 173:
	    return "??The property '$1$' cannot be made an array propery as the type '$2$' is not represented by an object element in XML.";
	case 174:
	    return "??MapEntry contains mapping target '$1$' from unknown schema. Verify the configuration and look for 'fixme:fixme' in the created schemas.";
	case 175:
	    return "??'$1$' is a complex type with simple content which cannot be used in a qualifier. 'string' is used instead.";
	case 177:
	    return "??A qualifier has type '$1$' which could not be identified unambiguously in model. 'string' is used instead.";
	case 178:
	    return "??'$1$' is a data type and cannot be used in a qualifier. 'string' is used instead.";
	case 179:
	    return "??'$1$' is a type of an unsupported category for a qualifier. 'string' is used instead.";
	case 180:
	    return "Could not find a map entry for the value type '$1$' of property '$2$' or the value type itself (in the model). Thus, constraining facets could not be created.";
	case 181:
	    return "Encountered DOMException, setting XML attribute '$1$' to value '$2$'. Exception message is: $3$";

	case 1009:
	    return "The property '$1$' is tagged as a metadata property. This is only possible for properties with complex content.";
	case 1010:
	    return "Support for nilReason attributes was requested in property '$1$'. This is not possible for properties which have a local $2$ as their value.";

	/*
	 * 1000 - 1999: Schematron assertions (for code lists, etc.)
	 */
	case 1000:
	    return "??Representation '$1$' of code list '$2$' is not recognized. No representation specific schematron assertions will be created for this code list.";
	case 1001:
	    return "??Code list URI of code list '$1$' is undefined. Schematron assertions that require the presence of this URI will not be created for the code list.";

	/*
	 * 2000-2999: Additional messages defined directly by this class (not
	 * ShapeChangeResult)
	 */
	case 2000:
	    return "Union '$1$' is subtype of union '$2$'. Both have attributes. This would lead to an XML Schema that requires choices of both unions to be encoded via two XML elements. This is contrary to the modeling intent of a union (where a single element would be encoded). The attributes of the subtype will be ignored. It is recommended that you review and revise your model. One situation where it is ok to have a subtype union is where the subtype does not have any attribute and only defines OCL constraints to restrict the options from the supertype union.";
	case 2001:
	    return "Could not find QName for value type '$1$' of property '$2$' in basic type '$3$'. Using 'FIXME' instead.";

	case 10021:
	    return "Import to namespace '$1$' added.";
	case 10022:
	    return "Found: '$1$'";

	default:
	    return "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	}
    }
};