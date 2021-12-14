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
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */

package de.interactive_instruments.ShapeChange.Model.Xmi10;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Multiplicity;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeErrorHandler;
import de.interactive_instruments.ShapeChange.ShapeChangeIgnoreClassException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.AssociationInfo;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Descriptor;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.ModelImpl;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Model.Stereotypes;
import de.interactive_instruments.ShapeChange.Model.TaggedValues;

public class Xmi10Document extends ModelImpl implements Model, MessageSource {

    public Document document = null;
    protected String dtd = "";
    public ShapeChangeResult result = null;
    public Options options = null;

    /** Hash table for all relevant tagged values */
    protected SortedMap<String, TaggedValues> fTaggedValues = new TreeMap<String, TaggedValues>();

    /** Hash table for all relevant stereotypes */
    protected SortedMap<String, Stereotypes> fStereotypes = new TreeMap<String, Stereotypes>();

    /** Hash table for all relevant supertypes of a feature or data type */
    protected SortedMap<String, SortedSet<String>> fSupertypes = new TreeMap<String, SortedSet<String>>();

    /** Hash table for all relevant subtypes of a pure abstract class */
    protected SortedMap<String, SortedSet<String>> fSubtypes = new TreeMap<String, SortedSet<String>>();

    /** Hash table for all potential types */
    protected SortedMap<String, Element> fTypes = new TreeMap<String, Element>();

    /** Hash table for all root schema packages */
    protected SortedMap<String, Element> fSchemas = new TreeMap<String, Element>();

    /** Hash table for associations */
    protected SortedMap<String, AssociationInfo> fAssociations = new TreeMap<String, AssociationInfo>();
    protected SortedMap<String, Vector<PropertyInfo>> fRoles = new TreeMap<String, Vector<PropertyInfo>>();

    /** Hash table for all relevant packages */
    protected SortedMap<String, PackageInfo> fPackages = new TreeMap<String, PackageInfo>();

    /** Hash table for all classes and classnames */
    protected SortedMap<String, ClassInfo> fClasses = new TreeMap<String, ClassInfo>();
    protected SortedMap<String, ClassInfo> fClassnames = new TreeMap<String, ClassInfo>();
    protected SortedMap<String, Multiplicity> fClassesRoseHiddenCardinality = new TreeMap<String, Multiplicity>();
    public SortedMap<String, String> fClassesRoseHiddenLabels = new TreeMap<String, String>();

    /** Hash table for all package uuids */
    protected HashMap<String, UUID> fUUIDs = new HashMap<String, UUID>();

    /** Return options and configuration object. */
    public Options options() {
	return options;
    } // options()

    /** Return result object for error reporting. */
    public ShapeChangeResult result() {
	return result;
    } // result()

    /** Load the application schema(s). */
    public void initialise(ShapeChangeResult r, Options o, String xmifile) throws ShapeChangeAbortException {

	options = o;
	result = r;
	open(xmifile);

	/** verify XMI and UML version */
	verify();

	/** remove unused parts from DOM before processing */
	cleanupDOM();

	/** preprocess XMI elements to speed up processing performance */
	initTaggedValuesMap();
	initStereotypesMap();
	initSubSupertypesMap();
	initTypesMap();
	initAssociations();
	initPackages();
	initClasses();

	SortedSet<PackageInfo> schemas = schemas("");
	for (Iterator<PackageInfo> i = schemas.iterator(); i.hasNext();) {
	    PackageInfo pi = i.next();
	    options.addSchemaLocation(pi.targetNamespace(), pi.xsdDocument());
	}
    } // Xmi10Document()

    public void open(String xmlfile) throws ShapeChangeAbortException {

	DocumentBuilder builder = null;
	ShapeChangeErrorHandler errorHandler = null;
	try {
	    System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
		    "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    factory.setNamespaceAware(true);
	    factory.setValidating(false);
	    factory.setIgnoringElementContentWhitespace(true);
	    factory.setIgnoringComments(true);
	    builder = factory.newDocumentBuilder();
	    errorHandler = new ShapeChangeErrorHandler();
	    builder.setErrorHandler(errorHandler);
	} catch (FactoryConfigurationError e) {
	    result.addFatalError(null, 1);
	    throw new ShapeChangeAbortException();
	} catch (ParserConfigurationException e) {
	    result.addFatalError(null, 2);
	    throw new ShapeChangeAbortException();
	}

	// parse file
	try {
	    // consider pre- and postprocessing files with a configurable XSLT
	    // script
	    document = builder.parse(xmlfile);

	    if (errorHandler.errorsFound()) {
		result.addFatalError(null, 3);
		throw new ShapeChangeAbortException();
	    }
	} catch (SAXException e) {
	    String m = e.getMessage();
	    if (m != null) {
		result.addError(m);
	    } else {
		e.printStackTrace(System.err);
	    }
	} catch (IOException e) {
	    String m = e.getMessage();
	    if (m != null) {
		result.addError(m);
	    } else {
		e.printStackTrace(System.err);
	    }
	} catch (Exception e) {
	    String m = e.getMessage();
	    if (m != null) {
		result.addError(m);
	    }
	    Exception se = e;
	    if (e instanceof SAXException) {
		se = ((SAXException) e).getException();
	    }
	    if (se != null) {
		se.printStackTrace(System.err);
	    } else {
		e.printStackTrace(System.err);
	    }
	}

	DocumentType dt = document.getDoctype();
	dtd = dt == null ? "" : dt.getName();

    } // open()

    public String characterEncoding() {
	String xmlCharacterEncoding = document.getXmlEncoding();
	if (xmlCharacterEncoding == null) {
	    xmlCharacterEncoding = "UTF-8";
	}
	return xmlCharacterEncoding;
    } // characterEncoding()

    /**
     * get value of text child node
     * 
     * @param n tbd
     * @return tbd
     */
    protected String textValue(Node n) {
	NodeList nl = n.getChildNodes();
	Node n2;
	for (int i = 0; i < nl.getLength(); i++) {
	    n2 = nl.item(i);
	    if (n2.getNodeType() == Node.TEXT_NODE) {
		return n2.getNodeValue();
	    }
	}
	return null;
    }

    /**
     * Verify XMI version
     * 
     * @throws ShapeChangeAbortException tbd
     */
    protected void verify() throws ShapeChangeAbortException {
	if (document.getDoctype() == null) {
	    result.addFatalError(null, 16);
	    throw new ShapeChangeAbortException();
	}
	NodeList nl = document.getElementsByTagName("XMI");
	if (nl.getLength() == 1) {
	    Element e = (Element) nl.item(0);
	    String s = e.getAttribute("xmi.version");
	    if (!s.equals("1.0")) {
		result.addFatalError(this, 4, s);
		throw new ShapeChangeAbortException();
	    }
	} else {
	    result.addFatalError(null, 5);
	    throw new ShapeChangeAbortException();
	}

	nl = document.getElementsByTagName("XMI.metamodel");
	if (nl.getLength() == 1) {
	    Element e = (Element) nl.item(0);
	    String s = e.getAttribute("xmi.name");
	    if (!s.equals("UML")) {
		result.addFatalError(this, 6, s);
		throw new ShapeChangeAbortException();
	    }
	    s = e.getAttribute("xmi.version");
	    if (s.equals("1.4")) {
		result.addWarning(null, 1000);
	    } else if (!s.equals("1.3")) {
		result.addFatalError(this, 7, s);
		throw new ShapeChangeAbortException();
	    }
	} else {
	    result.addFatalError(null, 8);
	    throw new ShapeChangeAbortException();
	}
    }

    /**
     * Delete parts of the DOM not relevant for the conversion, i.e. the
     * &lt;XMI.extensions&gt; and the &lt;XMI.difference&gt; elements.
     */
    protected void cleanupDOM() {
	if (options.eaIncludeExtentsions) {
	    return;
	}

	try {
	    Node n;
	    Node p;

	    // remove <XMI.extensions> elements
	    NodeList elements = document.getElementsByTagName("XMI.extensions");
	    for (int j = 0; j < elements.getLength(); j++) {
		n = elements.item(j);
		p = n.getParentNode();
		n = p.removeChild(n);
	    }

	    // remove <XMI.difference> elements
	    elements = document.getElementsByTagName("XMI.difference");
	    for (int j = 0; j < elements.getLength(); j++) {
		n = elements.item(j);
		p = n.getParentNode();
		n = p.removeChild(n);
	    }

	    document.normalize();
	} catch (Exception e) {
	    String m = e.getMessage();
	    if (m != null) {
		result.addError(m);
	    }
	    e.printStackTrace(System.err);
	}
    } // CleanupDOM(document)

    /**
     * get value of idref reference
     * 
     * @param n tbd
     * @return tbd
     */
    protected String idrefValue(Node n) {
	NodeList nl = n.getChildNodes();
	Node n2;
	for (int i = 0; i < nl.getLength(); i++) {
	    n2 = nl.item(i);
	    if (n2.getNodeType() == Node.ELEMENT_NODE) {
		return ((Element) n2).getAttribute("xmi.idref");
	    }
	}
	return "";
    }

    /**
     * Get element representing a property.
     * 
     * @param id tbd
     * @return tbd
     */
    protected Element getElementById(String id) {
	return document.getElementById(id);
    }

    /**
     * Get id (or idref) of an element.
     * 
     * @param elmt  tbd
     * @param child tbd
     * @return tbd
     */
    protected String idOfProperty(Element elmt, String child) {

	NodeList nl1 = elmt.getChildNodes();
	for (int j = 0; j < nl1.getLength(); j++) {
	    Node n1 = nl1.item(j);
	    if (n1.getNodeType() != Node.ELEMENT_NODE) {
		continue;
	    }
	    if (n1.getNodeName().equals(child)) {
		Node n2 = n1.getFirstChild();
		if (n2 == null) {
		    continue;
		}
		if (n2.getNodeType() != Node.ELEMENT_NODE) {
		    continue;
		}
		String id = ((Element) n2).getAttribute("xmi.id");
		if (id.equals("")) {
		    id = ((Element) n2).getAttribute("xmi.idref");
		}
		return id;
	    }
	}
	return "";
    }

    /**
     * Get element representing a property.
     * 
     * @param elmt  tbd
     * @param child tbd
     * @return tbd
     */
    protected Element elementOfProperty(Element elmt, String child) {
	NodeList nl1 = elmt.getChildNodes();
	for (int j = 0; j < nl1.getLength(); j++) {
	    Node n1 = nl1.item(j);
	    if (n1.getNodeType() != Node.ELEMENT_NODE) {
		continue;
	    }
	    if (n1.getNodeName().equals(child)) {
		Node n2 = n1.getFirstChild();
		if (n2 == null) {
		    continue;
		}
		if (n2.getNodeType() != Node.ELEMENT_NODE) {
		    continue;
		}
		String id = ((Element) n2).getAttribute("xmi.idref");
		if (id.equals("")) {
		    return (Element) n2;
		} else {
		    return document.getElementById(id);
		}
	    }
	}
	return null;
    }

    /**
     * Get ids (or idrefs) of an element.
     * 
     * @param elmt  tbd
     * @param child tbd
     * @return tbd
     */
    protected Vector<String> idsOfProperty(Element elmt, String child) {
	Vector<String> ids = new Vector<String>();
	NodeList nl1 = elmt.getChildNodes();
	for (int j = 0; j < nl1.getLength(); j++) {
	    Node n1 = nl1.item(j);
	    if (n1.getNodeType() != Node.ELEMENT_NODE) {
		continue;
	    }
	    String name1 = n1.getNodeName();
	    if (name1.equals(child)) {
		NodeList nl2 = n1.getChildNodes();
		for (int k = 0; k < nl2.getLength(); k++) {
		    Node n2 = nl2.item(k);
		    if (n2.getNodeType() != Node.ELEMENT_NODE) {
			continue;
		    }
		    String id = ((Element) n2).getAttribute("xmi.id");
		    if (id.equals("")) {
			id = ((Element) n2).getAttribute("xmi.idref");
		    }
		    ids.add(id);
		}
	    } else if (name1.equals("Foundation.Core.Namespace.ownedElement")) {
		Node n2 = n1.getFirstChild();
		if (n2 != null) {
		    String name2 = n2.getNodeName();
		    if (name2.equals("Foundation.Core.Primitive")) {
			ids.addAll(idsOfProperty((Element) n2, child));
		    }
		    if (name2.equals("Foundation.Core.Enumeration")) {
			ids.addAll(idsOfProperty((Element) n2, child));
		    }
		}
	    }
	}
	return ids;
    }

    /**
     * Get value of text node.
     * 
     * @param e        tbd
     * @param property tbd
     * @return tbd
     */
    protected String textOfProperty(Element e, String property) {
	Node n1;
	Node n2;
	String nam;
	NodeList nl = e.getChildNodes();
	for (int j = 0; j < nl.getLength(); j++) {
	    n1 = nl.item(j);
	    if (n1.getNodeType() != Node.ELEMENT_NODE) {
		continue;
	    }
	    nam = n1.getNodeName();
	    if (nam.equals(property)) {
		return textValue(n1);
	    } else if (nam.equals("Foundation.Core.Namespace.ownedElement")) {
		n2 = n1.getFirstChild();
		nam = n2.getNodeName();
		if (nam.equals("Foundation.Core.Primitive")) {
		    return textOfProperty((Element) n2, property);
		}
		if (nam.equals("Foundation.Core.Enumeration")) {
		    return textOfProperty((Element) n2, property);
		}
	    }
	}
	return null;
    }

    /**
     * Get value of attribute node.
     * 
     * @param elmt  tbd
     * @param child tbd
     * @param att   tbd
     * @return tbd
     */
    protected String attributeOfProperty(Element elmt, String child, String att) {
	NodeList nl1 = elmt.getChildNodes();
	for (int j = 0; j < nl1.getLength(); j++) {
	    Node n1 = nl1.item(j);
	    if (n1.getNodeType() != Node.ELEMENT_NODE) {
		continue;
	    }
	    String name1 = n1.getNodeName();
	    if (name1.equals(child)) {
		return ((Element) n1).getAttribute(att);
	    } else if (name1.equals("Foundation.Core.Namespace.ownedElement")) {
		Node n2 = n1.getFirstChild();
		String name2 = n2.getNodeName();
		if (name2.equals("Foundation.Core.Primitive")) {
		    return attributeOfProperty((Element) n2, child, att);
		}
		if (name2.equals("Foundation.Core.Enumeration")) {
		    return attributeOfProperty((Element) n2, child, att);
		}
	    }
	}
	return "";
    }

    /**
     * Get first child element.
     * 
     * @param elmt tbd
     * @return tbd
     */
    protected Element firstChildElement(Element elmt) {
	NodeList nl1 = elmt.getChildNodes();
	for (int j = 0; j < nl1.getLength(); j++) {
	    Node n1 = nl1.item(j);
	    if (n1.getNodeType() != Node.ELEMENT_NODE) {
		continue;
	    }
	    return (Element) n1;
	}
	return null;
    }

    /**
     * Check visibility of an UML model element for the mapping.
     * 
     * @param e tbd
     * @return tbd
     */
    protected boolean visible(Element e) {
	if (e == null) {
	    return false;
	}
	if (options.parameter("publicOnly").equals("true")
		&& !"public".equals(attributeOfProperty(e, "Foundation.Core.ModelElement.visibility", "xmi.value"))
		&& !"".equals(attributeOfProperty(e, "Foundation.Core.ModelElement.visibility", "xmi.value"))) {
	    return false;
	}
	Stereotypes stereotypes = fStereotypes.get(e.getAttribute("xmi.id"));
	if (stereotypes != null && stereotypes.contains("_ShapeChangeIgnore_")) {
	    return false;
	}
	return true;
    }

    public String taggedValue(String idref, String tag) {
	String res = null;
	TaggedValues tvs = fTaggedValues.get(idref);
	if (tvs == null && options.eaBugFixWrongID) {
	    /*
	     * The following is a bugfix for Enterprise Architect which sometimes creates
	     * references with the wrong prefix in the idref value
	     */
	    if (idref.startsWith("EAID")) {
		tvs = fTaggedValues.get("EAPK" + idref.substring(4));
	    } else if (idref.startsWith("EAPK")) {
		tvs = fTaggedValues.get("EAID" + idref.substring(4));
	    }
	}

	if (tvs != null && tvs.containsKey(tag))
	    res = tvs.getFirstValue(tag);

	return res;
    }

    /**
     * @param idref identifies a model element
     * @return the tagged values for the model element identified by idref
     */
    public TaggedValues taggedValues(String idref) {
	TaggedValues tvs = fTaggedValues.get(idref);
	if (tvs == null && options.eaBugFixWrongID) {
	    /*
	     * The following is a bugfix for Enterprise Architect which sometimes creates
	     * references with the wrong prefix in the idref value
	     */
	    if (idref.startsWith("EAID")) {
		tvs = fTaggedValues.get("EAPK" + idref.substring(4));
	    } else if (idref.startsWith("EAPK")) {
		tvs = fTaggedValues.get("EAID" + idref.substring(4));
	    }
	}
	return tvs;
    }

    /**
     * Some applications use ownedElement to attach stereotypes
     * 
     * @param e tbd
     * @return tbd
     */
    protected Vector<String> getOwnerId(Element e) {
	Element owner = (Element) e.getParentNode().getParentNode();
	String id = owner.getAttribute("xmi.id");
	Vector<String> v = new Vector<String>();
	if (id != null) {
	    v.add(id);
	}
	return v;
    }

    /**
     * Get the id of the containing package of a class
     * 
     * @param e tbd
     * @return tbd
     */
    protected String getOwnerIdAsString(Element e) {
	Element owner = (Element) e.getParentNode().getParentNode();
	String id = owner.getAttribute("xmi.id");
	if (id == null) {
	    id = "";
	}
	return id;
    }

    /**
     * Verify that an element is a model element and not just a reference to one.
     * 
     * @param e tbd
     * @return tbd
     */
    protected boolean notAReference(Element e) {
	if (e.getAttribute("xmi.id").equals("")) {
	    return false;
	}
	return true;
    }

    /**
     * Needed for MagicDraw XMI 1.0 / UML 1.4 export
     * 
     * @param elmt tbd
     * @return tbd
     */
    protected boolean isOwnerOfEnumeration(Element elmt) {
	Node n1 = elmt.getFirstChild();
	String name1 = n1.getNodeName();
	if (name1.equals("Foundation.Core.Namespace.ownedElement")) {
	    Node n2 = n1.getFirstChild();
	    String name2 = n2.getNodeName();
	    if (name2.equals("Foundation.Core.Enumeration")) {
		return true;
	    }
	}
	return false;
    }

    /** Initialize map for well-known tagged values */
    protected void initTaggedValuesMap() {

	Node n1;
	Node n2;
	NodeList nl1;
	NodeList nl2;

	nl1 = document.getElementsByTagName("Foundation.Extension_Mechanisms.TaggedValue");
	for (int j = 0; j < nl1.getLength(); j++) {
	    n1 = nl1.item(j);
	    nl2 = n1.getChildNodes();
	    String tag = "";
	    String value = "";
	    String id = "";
	    for (int k = 0; k < nl2.getLength(); k++) {
		n2 = nl2.item(k);
		if (n2.getNodeName() == "Foundation.Extension_Mechanisms.TaggedValue.tag"
			|| n2.getNodeName() == "Foundation.Core.ModelElement.name") {
		    tag = textValue(n2);
		}
		if (n2.getNodeName() == "Foundation.Extension_Mechanisms.TaggedValue.value"
			|| n2.getNodeName() == "Foundation.Extension_Mechanisms.TaggedValue.dataValue") {
		    value = textValue(n2);
		}
		if (n2.getNodeName() == "Foundation.Extension_Mechanisms.TaggedValue.modelElement") {
		    id = idrefValue(n2);
		}
	    }

	    if (value == null || value.equals("")) {
		continue;
	    }

	    if (id.equals("")) {
		Element parent = (Element) n1.getParentNode().getParentNode();
		id = parent.getAttribute("xmi.id");
	    }

	    String t = options().taggedValueNormalizer().normalizeTaggedValue(tag);
	    if (t != null) {
		TaggedValues tvs;
		if (fTaggedValues.containsKey(id)) {
		    tvs = fTaggedValues.get(id);
		} else {
		    tvs = options().taggedValueFactory();
		    fTaggedValues.put(id, tvs);
		}
		tvs.add(t, value);

		result.addDebug(this, 10000, t, id, value);
	    }
	}
    } // initTaggedValuesMap

    /** Initialize map of well-known stereotypes */
    protected void initStereotypesMap() {

	Element n1;
	NodeList nl1;

	nl1 = document.getElementsByTagName("Foundation.Extension_Mechanisms.Stereotype");
	for (int j = 0; j < nl1.getLength(); j++) {
	    n1 = (Element) nl1.item(j);

	    String id = n1.getAttribute("xmi.id");
	    if (id.equals("")) {
		continue;
	    }

	    String name = textOfProperty(n1, "Foundation.Core.ModelElement.name");
	    String baseClass = textOfProperty(n1, "Foundation.Extension_Mechanisms.Stereotype.baseClass");

	    // map stereotype alias to well-known stereotype
	    String s = options.stereotypeAlias(name);
	    if (s != null) {
		name = s;
	    }
	    if (name.equals("")) {
		continue;
	    }

	    boolean found = false;

	    if (baseClass.equals("Class") && Options.classStereotypes.contains(name.toLowerCase())) {

		Vector<String> ids = idsOfProperty(n1, "Foundation.Extension_Mechanisms.Stereotype.extendedElement");
		if (ids.size() == 0) {
		    ids = getOwnerId(n1);
		}
		for (Iterator<String> k = ids.iterator(); k.hasNext();) {
		    s = k.next();
		    Stereotypes st = options.stereotypesFactory();
		    st.add(options.internalize(options.normalizeStereotype(name)));
		    fStereotypes.put(s, st);
		    result.addDebug(this, 10019, name, s);
		}
		found = true;
	    }

	    if (baseClass.equals("Association") && Options.assocStereotypes.contains(name.toLowerCase())) {

		Vector<String> ids = idsOfProperty(n1, "Foundation.Extension_Mechanisms.Stereotype.extendedElement");
		if (ids.size() == 0) {
		    ids = getOwnerId(n1);
		}
		for (Iterator<String> k = ids.iterator(); k.hasNext();) {
		    s = k.next();
		    Stereotypes st = options.stereotypesFactory();
		    st.add(options.internalize(options.normalizeStereotype(name)));
		    fStereotypes.put(s, st);
		    result.addDebug(this, 10019, name, s);
		}
		found = true;
	    }

	    if ((baseClass.equals("Package") || baseClass.equals("ClassifierRole"))
		    && Options.packageStereotypes.contains(name.toLowerCase())) {

		Vector<String> ids = idsOfProperty(n1, "Foundation.Extension_Mechanisms.Stereotype.extendedElement");
		if (ids.size() == 0) {
		    ids = getOwnerId(n1);
		}
		for (Iterator<String> k = ids.iterator(); k.hasNext();) {
		    s = k.next();
		    Stereotypes st = options.stereotypesFactory();
		    st.add(options.internalize(options.normalizeStereotype(name)));
		    fStereotypes.put(s, st);
		    result.addDebug(this, 10019, name, s);
		    Element e1 = document.getElementById(s);
		    if (e1 != null) {
			String ename = textOfProperty(e1, "Foundation.Core.ModelElement.name");
			result.addDebug(this, 10020, ename);
			fSchemas.put(ename, e1);
		    }
		}
		found = true;
	    }

	    if (baseClass.equals("Dependency") && Options.depStereotypes.contains(name.toLowerCase())) {

		Vector<String> ids = idsOfProperty(n1, "Foundation.Extension_Mechanisms.Stereotype.extendedElement");
		if (ids.size() == 0) {
		    ids = getOwnerId(n1);
		}
		for (Iterator<String> k = ids.iterator(); k.hasNext();) {
		    s = k.next();
		    Stereotypes st = options.stereotypesFactory();
		    st.add(options.internalize(options.normalizeStereotype(name)));
		    fStereotypes.put(s, st);
		    result.addDebug(this, 10019, name, s);
		}
		found = true;
	    }

	    if ((baseClass.equals("Attribute") || baseClass.equals("AssociationEnd"))
		    && Options.propertyStereotypes.contains(name.toLowerCase())) {

		Vector<String> ids = idsOfProperty(n1, "Foundation.Extension_Mechanisms.Stereotype.extendedElement");
		if (ids.size() == 0) {
		    ids = getOwnerId(n1);
		}
		for (Iterator<String> k = ids.iterator(); k.hasNext();) {
		    s = k.next();
		    Stereotypes st = options.stereotypesFactory();
		    st.add(options.internalize(options.normalizeStereotype(name)));
		    fStereotypes.put(s, st);
		    result.addDebug(this, 10019, name, s);
		}
		found = true;
	    }

	    if (!found) {
		result.addWarning(this, 1005, name, baseClass);
		Vector<String> ids = idsOfProperty(n1, "Foundation.Extension_Mechanisms.Stereotype.extendedElement");
		for (Iterator<String> i = ids.iterator(); i.hasNext();) {
		    id = i.next();
		    Stereotypes st = options.stereotypesFactory();
		    st.add(options.internalize(options.normalizeStereotype("_ShapeChangeIgnore_")));
		    fStereotypes.put(id, st);
		    Element e1 = document.getElementById(id);
		    if (e1 != null) {
			name = textOfProperty(e1, "Foundation.Core.ModelElement.name");
		    } else {
			name = "";
		    }
		    if (name == null || name.equals("")) {
			name = "(unknown)";
		    }
		    result.addWarning(this, 1006, baseClass, name);
		}
	    }
	}
    }

    /**
     * Initialize maps for supertypes of a feature or data type and subtypes of a
     * pure abstract class
     */
    protected void initSubSupertypesMap() {
	NodeList nl1 = document.getElementsByTagName("Foundation.Core.Generalization");
	for (int j = 0; j < nl1.getLength(); j++) {
	    Element n1 = (Element) nl1.item(j);
	    String id = n1.getAttribute("xmi.id");
	    if (!visible(n1)) {
		continue;
	    }
	    String disc = textOfProperty(n1, "Foundation.Core.Generalization.discriminator");
	    if (disc != null && !disc.equals("")) {
		result.addWarning(this, 1007, id);
		continue;
	    }
	    Vector<String> childs = idsOfProperty(n1, "Foundation.Core.Generalization.child");
	    Vector<String> parents = idsOfProperty(n1, "Foundation.Core.Generalization.parent");
	    for (Iterator<String> idx = childs.iterator(); idx.hasNext();) {
		for (Iterator<String> jdx = parents.iterator(); jdx.hasNext();) {
		    String child = idx.next();
		    String parent = jdx.next();
		    if (fSubtypes.containsKey(parent)) {
			((Set<String>) fSubtypes.get(parent)).add(child);
		    } else {
			SortedSet<String> s2 = new TreeSet<String>();
			s2.add(child);
			fSubtypes.put(parent, s2);
		    }
		    if (fSupertypes.containsKey(child)) {
			((Set<String>) fSupertypes.get(child)).add(parent);
		    } else {
			SortedSet<String> s1 = new TreeSet<String>();
			s1.add(parent);
			fSupertypes.put(child, s1);
		    }
		}
	    }
	}
    }

    /**
     * Initialize map of types
     * 
     * @throws ShapeChangeAbortException tbd
     */
    protected void initTypesMap() throws ShapeChangeAbortException {
	String tname;
	Element e;
	NodeList nl = document.getElementsByTagName("Foundation.Core.DataType");
	for (int j = 0; j < nl.getLength(); j++) {
	    e = (Element) nl.item(j);
	    tname = textOfProperty(e, "Foundation.Core.ModelElement.name");
	    if (tname != null) {
		fTypes.put(tname.trim(), e);
	    }
	}
	nl = document.getElementsByTagName("Foundation.Core.Class");
	for (int j = 0; j < nl.getLength(); j++) {
	    e = (Element) nl.item(j);
	    tname = textOfProperty(e, "Foundation.Core.ModelElement.name");
	    if (tname != null) {
		fTypes.put(tname.trim(), e);
	    }
	}
    }

    /**
     * Initialize map of asscoiations
     * 
     * @throws ShapeChangeAbortException tbd
     */
    protected void initAssociations() throws ShapeChangeAbortException {
	NodeList nl1 = document.getElementsByTagName("Foundation.Core.Association");
	for (int j = 0; j < nl1.getLength(); j++) {
	    AssociationInfoXmi10 ai = new AssociationInfoXmi10(this, (Element) nl1.item(j));
	    if (ai != null) {
		fAssociations.put(ai.id(), ai);
	    }
	}
    }

    private void addPackageElements(NodeList nl) throws ShapeChangeAbortException {
	for (int j = 0; j < nl.getLength(); j++) {
	    Element e = (Element) nl.item(j);
	    if ((visible(e) || options.eaBugFixPublicPackagesAreMarkedAsPrivate) && notAReference(e)) {
		PackageInfoXmi10 pi = new PackageInfoXmi10(this, e);
		fPackages.put(pi.id(), pi);
	    }
	}
    }

    /**
     * Process all packages in the application schema
     * 
     * @throws ShapeChangeAbortException tbd
     */
    protected void initPackages() throws ShapeChangeAbortException {
	Element root = document.getDocumentElement();
	addPackageElements(root.getElementsByTagName("Model_Management.Model"));
	addPackageElements(root.getElementsByTagName("Model_Management.Package"));
    }

    private void addClassElements(NodeList nl) throws ShapeChangeAbortException {
	for (int j = 0; j < nl.getLength(); j++) {
	    Element e = (Element) nl.item(j);
	    if (visible(e) && notAReference(e)) {
		try {
		    ClassInfoXmi10 ci = new ClassInfoXmi10(this, e);
		    fClasses.put(ci.id(), ci);
		    Multiplicity rm = ci.roseHiddenCardinality();
		    if (rm != null) {
			fClassesRoseHiddenCardinality.put(ci.id(), rm);
		    }
		    String rl = ci.roseHiddenLabels();
		    if (rl != null) {
			fClassesRoseHiddenLabels.put(ci.id(), rl);
		    }
		    // Fix for bug in Rose
		    if (options.roseBugFixDuplicateGlobalDataTypes) {
			ClassInfo ci2 = fClassnames.get(ci.name());
			if (ci2 != null) {
			    if (ci.id().startsWith("G.")) {
				fClasses.put(ci.id(), ci2);
				result.addDebug(this, 10018, ci.name() + " (" + ci.id() + ")",
					ci2.name() + " (" + ci2.id() + ")");
			    }
			    if (ci2.id().startsWith("G.")) {
				fClasses.put(ci2.id(), ci);
				fClassnames.put(ci.name(), ci);
				result.addDebug(this, 10018, ci2.name() + " (" + ci2.id() + ")",
					ci.name() + " (" + ci.id() + ")");
			    }
			} else {
			    fClassnames.put(ci.name(), ci);
			}
		    }
		} catch (ShapeChangeIgnoreClassException ice) {
		    // do nothing
		}
	    }
	}
    }

    /**
     * Process all classes and generate the XML Schema "code"
     * 
     * @throws ShapeChangeAbortException tbd
     */
    protected void initClasses() throws ShapeChangeAbortException {
	result.addDebug("Processing Classes...");

	// First generate all the basic class info in the full model
	Element root = document.getDocumentElement();
	addClassElements(root.getElementsByTagName("Foundation.Core.Class"));
	addClassElements(root.getElementsByTagName("Foundation.Core.Interface"));
	addClassElements(root.getElementsByTagName("Foundation.Core.DataType"));
	if (options.eaIncludeExtentsions) {
	    addClassElements(root.getElementsByTagName("Foundation.Core.ModelElement"));
	}
    }

    public PackageInfo packageById(String id) {
	return fPackages.get(id);
    }

    public ClassInfo classById(String id) {
	return fClasses.get(id);
    }

    public ClassInfo classByName(String nam) {
	return fClassnames.get(nam);
    }

    @Override
    public SortedSet<ClassInfo> classes() {

	SortedSet<ClassInfo> result = new TreeSet<>();

	result.addAll(this.fClasses.values());

	return result;
    }

    public SortedSet<ClassInfo> classes(PackageInfo pi) {

	SortedSet<ClassInfo> res = new TreeSet<ClassInfo>();
	for (Iterator<Map.Entry<String, ClassInfo>> i = fClasses.entrySet().iterator(); i.hasNext();) {
	    Map.Entry<String, ClassInfo> entry = i.next();
//			if (options.roseBugFixDuplicateGlobalDataTypes) {
//				String ky = entry.getKey();
//				if (ky.startsWith("G.")) {
//					continue;
//				}
//			}
	    ClassInfo ci = entry.getValue();
	    if (ci != null) {
		if (pi == null) {
		    res.add(ci);
		} else if (ci.inSchema(pi)) {
		    res.add(ci);
		}
	    }
	}
	return res;
    }

    protected Multiplicity cardinalityFromString(String multiplicityRanges) {
	String[] ranges = multiplicityRanges.split(",");
	int minv = Integer.MAX_VALUE;
	int maxv = Integer.MIN_VALUE;
	int lower;
	int upper;
	for (int i = 0; i < ranges.length; i++) {
	    if (ranges[i].indexOf("..") > 0) {
		String[] minmax = ranges[i].split("\\.\\.", 2);
		lower = Integer.parseInt(minmax[0]);
		if (minmax[1].equals("*") || minmax[1].equals("n") || minmax[1].length() == 0) {
		    upper = Integer.MAX_VALUE;
		} else {
		    try {
			upper = Integer.parseInt(minmax[1]);
		    } catch (NumberFormatException e) {
			result.addWarning(null, 1003, minmax[1]);
			upper = Integer.MAX_VALUE;
		    }
		}
	    } else {
		if (ranges[i].length() == 0 || ranges[i].equals("*") || ranges[i].equals("n")) {
		    lower = 0;
		    upper = Integer.MAX_VALUE;
		} else {
		    try {
			lower = Integer.parseInt(ranges[i]);
			upper = lower;
		    } catch (NumberFormatException e) {
			result.addWarning(null, 1003, ranges[i]);
			lower = 0;
			upper = Integer.MAX_VALUE;
		    }
		}
	    }
	    if (lower < minv && lower >= 0) {
		minv = lower;
	    }
	    if (upper < 0) {
		maxv = Integer.MAX_VALUE;
	    }
	    if (upper > maxv) {
		maxv = upper;
	    }
	}
	Multiplicity m = new Multiplicity();
	m.minOccurs = minv;
	m.maxOccurs = maxv;
	return m;
    }

    public void shutdown() {
	// nothing to be done for XMI files
    }

    @Override
    public SortedSet<PackageInfo> packages() {
	SortedSet<PackageInfo> allPackages = new TreeSet<PackageInfo>();
	for (PackageInfo pi : fPackages.values()) {
	    allPackages.add(pi);
	}
	return allPackages;
    }

    @Override
    public SortedSet<AssociationInfo> associations() {
	return fAssociations.isEmpty() ? new TreeSet<>() : new TreeSet<AssociationInfo>(fAssociations.values());
    }

    @Override
    public String descriptorSource(Descriptor descriptor) {

	String source = options().descriptorSource(descriptor.getName());

	// if nothing has been configured, use defaults
	if (source == null) {

	    if (descriptor == Descriptor.DOCUMENTATION)
		source = "tag#documentation;description";
	    else if (descriptor == Descriptor.ALIAS)
		source = "tag#alias";
	    else if (descriptor == Descriptor.GLOBALIDENTIFIER)
		source = "tag#globalIdentifier";
	    else if (descriptor == Descriptor.DEFINITION)
		source = "sc:extract#PROLOG";
	    else if (descriptor == Descriptor.DESCRIPTION)
		source = "none";
	    else
		source = "tag#" + descriptor;
	}

	return source;
    }

    @Override
    public String message(int mnr) {

	switch (mnr) {

	case 4:
	    return "XMI version must be 1.0, found: '$1$'.";
	case 6:
	    return "Metamodel must be UML, found: '$1$'.";
	case 7:
	    return "The UML version must be 1.3, found: '$1$'.";
	    
	case 1005:
	    return "Stereotype <<$1$>> not supported for UML model elements of type '$2$'.";
	case 1006:
	    return "The $1$ '$2$' will be ignored.";
	case 1007:
	    return "The discriminator for the UML generalization with ID '$1$' is not blank. This genralization is ignored.";
	
	case 10000:
	    return "Added tagged value '$1$' for element with ID '$2$' with value: '$3$'.";
	case 10018:
	    return "Rose Bug Fix for Duplicate Global Data Types: DataType '$1$' replaced by '$2$'.";
	case 10019:
	    return "Added stereotype '$1$' for element with ID '$2$'.";
	case 10020:
	    return "Application schema found, package name: '$1$'";
	
	default:
	    return "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	}
    }

};