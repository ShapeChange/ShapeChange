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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.SerializerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.interactive_instruments.ShapeChange.MapEntry;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.RuleRegistry;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.Target;

/**
 * UML to RDF/OWL (based on OWS-8 encoding rule)
 */
public class RDF implements Target, MessageSource {

    public static final String PARAM_LANGUAGE = "language";
    public static final String PARAM_TV_FOR_TITLE = "taggedValueForTitle";
    public static final String PARAM_TV_FOR_CODE = "taggedValueForCode";
    public static final String PARAM_CODELIST_ONLY = "codeListOnly";
    public static final String PARAM_ID_REPLACE_PATTERN = "idReplacePattern";
    public static final String PARAM_ID_REPLACE_CHAR = "idReplaceChar";

    public static final String W3C_RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    public static final String W3C_RDFS = "http://www.w3.org/2000/01/rdf-schema#";
    public static final String W3C_OWL = "http://www.w3.org/2002/07/owl#";
    public static final String W3C_OWL2XML = "http://www.w3.org/2006/12/owl2-xml#";
    public static final String DC = "http://purl.org/dc/elements/1.1/";
    public static final String OGC_GEOSPARQL = "http://www.opengis.net/geosparql#";
    public static final String W3C_SKOS = "http://www.w3.org/2004/02/skos/core#";

    private HashSet<PropertyInfo> exportedRoles = new HashSet<PropertyInfo>();
    private HashSet<PropertyInfo> exportedProperties = new HashSet<PropertyInfo>();
    private boolean error = false;
    private boolean printed = false;

    private String language = "en";
    private String outputDirectory = null;
    private Element root = null;
    private String encoding = null;
    private PackageInfo pi = null;
    private Model model = null;
    private Options options = null;
    private ShapeChangeResult result = null;
    private Document document = null;
    private String ns = null;
    private String taggedValueForTitle = null;
    private String taggedValueForCode = null;
    private String idReplacePattern = null;
    private String idReplaceChar = "-";
    private boolean codeListOnly = false;

    @Override
    public String getTargetName() {
	return "RDF";
    }

    // FIXME New diagnostics-only flag is to be considered
    public void initialise(PackageInfo p, Model m, Options o, ShapeChangeResult r, boolean diagOnly)
	    throws ShapeChangeAbortException {
	pi = p;
	model = m;
	options = o;
	result = r;

	outputDirectory = options.parameter(this.getClass().getName(), "outputDirectory");
	if (outputDirectory == null)
	    outputDirectory = options.parameter("outputDirectory");
	if (outputDirectory == null)
	    outputDirectory = ".";

	language = options.parameter(this.getClass().getName(), PARAM_LANGUAGE);
	if (language == null)
	    language = "en";

	taggedValueForTitle = options.parameter(this.getClass().getName(), PARAM_TV_FOR_TITLE);

	taggedValueForCode = options.parameter(this.getClass().getName(), PARAM_TV_FOR_CODE);

	encoding = model.characterEncoding();
	if (encoding == null)
	    encoding = "UTF-8";

	String s = options.parameter(this.getClass().getName(), PARAM_CODELIST_ONLY);
	if (s != null && s.equalsIgnoreCase("true"))
	    codeListOnly = true;

	s = options.parameter(this.getClass().getName(), PARAM_ID_REPLACE_PATTERN);
	if (s != null && !s.isEmpty())
	    idReplacePattern = s;

	s = options.parameter(this.getClass().getName(), PARAM_ID_REPLACE_CHAR);
	if (s != null && !s.isEmpty())
	    idReplaceChar = s;

	document = createDocument();

	/*
	 * ProcessingInstruction proci; proci =
	 * document.createProcessingInstruction("xml-stylesheet",
	 * "type='text/xsl' href='./rdf.xsl'"); document.appendChild(proci);
	 * 
	 */
	document.appendChild(document.createComment("Created using ShapeChange - http://shapechange.net/"));

	root = document.createElementNS(W3C_RDF, "RDF");
	document.appendChild(root);
	addAttribute(document, root, "xmlns:rdf", W3C_RDF);
	addAttribute(document, root, "xmlns:owl", W3C_OWL);
	addAttribute(document, root, "xmlns:skos", W3C_SKOS);

	if (!codeListOnly) {
	    addAttribute(document, root, "xmlns:rdfs", W3C_RDFS);
	    addAttribute(document, root, "xmlns:owl2xml", W3C_OWL2XML);
	    addAttribute(document, root, "xmlns:dc", DC);
	    addAttribute(document, root, "xmlns:xsd", Options.W3C_XML_SCHEMA + "#");
	}

	addAttribute(document, root, "xml:base", pi.targetNamespace());

	ns = pi.targetNamespace() + "#";
	addAttribute(document, root, "xmlns:" + pi.xmlns(), ns);

	if (!codeListOnly) {
	    Element e0 = document.createElementNS(W3C_OWL, "Ontology");
	    root.appendChild(e0);
	    addAttribute(document, e0, "rdf:about", "");

	    Element e1 = document.createElementNS(W3C_OWL, "imports");
	    addAttribute(document, e1, "rdf:resource", OGC_GEOSPARQL);
	    e0.appendChild(e1);

	    e0 = document.createElementNS(W3C_OWL, "AnnotationProperty");
	    root.appendChild(e0);
	    addAttribute(document, e0, "rdf:about", DC + "source");

	    e0 = document.createElementNS(W3C_OWL, "AnnotationProperty");
	    root.appendChild(e0);
	    addAttribute(document, e0, "rdf:about", DC + "description");

	    e0 = document.createElementNS(W3C_OWL, "AnnotationProperty");
	    root.appendChild(e0);
	    addAttribute(document, e0, "rdf:about", W3C_SKOS + "historyNote");

	    e0 = document.createElementNS(W3C_OWL, "AnnotationProperty");
	    root.appendChild(e0);
	    addAttribute(document, e0, "rdf:about", W3C_SKOS + "prefLabel");

	    e0 = document.createElementNS(W3C_OWL, "AnnotationProperty");
	    root.appendChild(e0);
	    addAttribute(document, e0, "rdf:about", W3C_SKOS + "definition");

	    e0 = document.createElementNS(W3C_OWL, "ObjectProperty");
	    root.appendChild(e0);
	    addAttribute(document, e0, "rdf:about", OGC_GEOSPARQL + "defaultGeometry");

	    e0 = document.createElementNS(W3C_OWL, "ObjectProperty");
	    root.appendChild(e0);
	    addAttribute(document, e0, "rdf:about", W3C_SKOS + "inScheme");

	    e0 = document.createElementNS(W3C_OWL, "Class");
	    root.appendChild(e0);
	    addAttribute(document, e0, "rdf:about", OGC_GEOSPARQL + "Feature");

	    e0 = document.createElementNS(W3C_OWL, "Class");
	    root.appendChild(e0);
	    addAttribute(document, e0, "rdf:about", OGC_GEOSPARQL + "Point");

	    e0 = document.createElementNS(W3C_OWL, "Class");
	    root.appendChild(e0);
	    addAttribute(document, e0, "rdf:about", OGC_GEOSPARQL + "Curve");

	    e0 = document.createElementNS(W3C_OWL, "Class");
	    root.appendChild(e0);
	    addAttribute(document, e0, "rdf:about", OGC_GEOSPARQL + "Surface");

	    e0 = document.createElementNS(W3C_OWL, "Class");
	    root.appendChild(e0);
	    addAttribute(document, e0, "rdf:about", OGC_GEOSPARQL + "Geometry");

	    e0 = document.createElementNS(W3C_OWL, "Class");
	    root.appendChild(e0);
	    addAttribute(document, e0, "rdf:about", W3C_OWL + "Thing");

	    e0 = document.createElementNS(W3C_OWL, "Class");
	    root.appendChild(e0);
	    addAttribute(document, e0, "rdf:about", W3C_OWL + "Concept");

	    e0 = document.createElementNS(W3C_OWL, "Class");
	    root.appendChild(e0);
	    addAttribute(document, e0, "rdf:about", W3C_OWL + "ConceptScheme");

	    e0 = document.createElementNS(W3C_RDF, "Description");
	    root.appendChild(e0);
	    addAttribute(document, e0, "rdf:about", pi.targetNamespace());

	    s = documentation(pi);
	    if (s != null && s.length() > 0) {
		e1 = document.createElementNS(DC, "description");
		addAttribute(document, e1, "rdf:datatype", Options.W3C_XML_SCHEMA + "#string");
		e0.appendChild(e1);
		e1.setTextContent(s);
	    }
	}
    }

    private String documentation(Info i) {
	String s = getDef(i);
	if (s != null && s.length() > 0) {
	    String s2 = getDesc(pi);
	    if (s2 != null && s2.length() > 0) {
		s = s + "\n\n" + s2;
	    }
	    s = s.replace("\r", "").trim();
	}
	return s;
    }

    private String PrepareToPrint(String s) {
	if (s != null) {
	    s = s.trim();
	}
	return s;
    }

    /**
     * Add attribute to an element
     * 
     * @param document tbd
     * @param e        tbd
     * @param name     tbd
     * @param value    tbd
     */
    protected void addAttribute(Document document, Element e, String name, String value) {
	Attr att = document.createAttribute(name);
	att.setValue(value);
	e.setAttributeNode(att);
    }

    protected Document createDocument() {
	Document document = null;
	try {
	    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	    DocumentBuilder db = dbf.newDocumentBuilder();
	    document = db.newDocument();
	} catch (ParserConfigurationException e) {
	    result.addFatalError(null, 2);
	    String m = e.getMessage();
	    if (m != null) {
		result.addFatalError(m);
	    }
	    e.printStackTrace(System.err);
	    System.exit(1);
	} catch (Exception e) {
	    result.addFatalError(e.getMessage());
	    e.printStackTrace(System.err);
	    System.exit(1);
	}

	return document;
    }

    public void process(ClassInfo ci) {
	if (error)
	    return;

	int cat = ci.category();
	switch (cat) {
	case Options.FEATURE:
	case Options.OBJECT:
	case Options.DATATYPE:
	case Options.UNION:
	case Options.BASICTYPE:
	    if (!codeListOnly)
		PrintClass(ci);
	    break;
	case Options.CODELIST:
	case Options.ENUMERATION:
	    PrintValues(ci);
	    break;
	}
    }

    private String uri(String v) {
	try {
	    URI uri = null;
	    if (!v.contains("#"))
		uri = new URI(null, v, null);
	    else {
		String[] sa = v.split("#", 2);
		if (idReplacePattern != null)
		    sa[1] = sa[1].replaceAll(idReplacePattern, idReplaceChar);
		uri = new URI(null, sa[0], sa[1]);
	    }
	    v = uri.toString();
	    if (v.startsWith(ns))
		v = v.replace(ns, "#");
	} catch (URISyntaxException e) {
	    result.addWarning(this, 1001, v);
	}
	return v;
    }

    private void PrintValues(ClassInfo ci) {

	Element e0, e1;

	e0 = document.createElementNS(W3C_SKOS, "ConceptScheme");
	root.appendChild(e0);
	addAttribute(document, e0, "rdf:about", uri(ns + ci.name()));

	e1 = document.createElementNS(W3C_RDF, "type");
	e0.appendChild(e1);
	addAttribute(document, e1, "rdf:resource", W3C_OWL + "Thing");

	String s = ci.taggedValue("name");
	if (s == null)
	    s = ci.taggedValue("title");
	if (s != null) {
	    e1 = document.createElementNS(W3C_SKOS, "prefLabel");
	    e0.appendChild(e1);
	    addAttribute(document, e1, "rdf:datatype", Options.W3C_XML_SCHEMA + "#string");
	    addAttribute(document, e1, "xml:lang", language);
	    e1.setTextContent(s);
	}

	s = documentation(ci);
	if (s != null && s.length() > 0) {
	    e1 = document.createElementNS(W3C_SKOS, "definition");
	    addAttribute(document, e1, "rdf:datatype", Options.W3C_XML_SCHEMA + "#string");
	    addAttribute(document, e1, "xml:lang", language);
	    e0.appendChild(e1);
	    e1.setTextContent(s);
	}

	for (PropertyInfo propi : ci.properties().values()) {
	    if (propi == null)
		continue;
	    if (!ExportValue(propi))
		continue;

	    String label = null;
	    if (taggedValueForTitle != null)
		label = propi.taggedValue(taggedValueForTitle);
	    if (label == null)
		label = PrepareToPrint(propi.name());

	    String code = propi.initialValue();
	    if (code == null || code.length() == 0)
		code = propi.name();
	    code = PrepareToPrint(code);

	    e0 = document.createElementNS(W3C_SKOS, "Concept");
	    root.appendChild(e0);
	    addAttribute(document, e0, "rdf:about", uri(ns + ci.name() + "_" + code));

	    e1 = document.createElementNS(W3C_RDF, "type");
	    e0.appendChild(e1);
	    addAttribute(document, e1, "rdf:resource", W3C_OWL + "Thing");

	    e1 = document.createElementNS(W3C_SKOS, "prefLabel");
	    e0.appendChild(e1);
	    addAttribute(document, e1, "rdf:datatype", Options.W3C_XML_SCHEMA + "#string");
	    addAttribute(document, e1, "xml:lang", language);
	    e1.setTextContent(label);

	    if (!label.equalsIgnoreCase(code)) {
		e1 = document.createElementNS(W3C_SKOS, "hiddenLabel");
		e0.appendChild(e1);
		addAttribute(document, e1, "rdf:datatype", Options.W3C_XML_SCHEMA + "#string");
		e1.setTextContent(code);
	    }

	    if (taggedValueForCode != null) {
		s = propi.taggedValue(taggedValueForCode);
		if (s != null) {
		    e1 = document.createElementNS(W3C_SKOS, "altLabel");
		    e0.appendChild(e1);
		    addAttribute(document, e1, "rdf:datatype", Options.W3C_XML_SCHEMA + "#string");
		    addAttribute(document, e1, "xml:lang", language);
		    e1.setTextContent(s);
		}
	    }

	    if (propi.matches("rule-rdf-prop-parent")) {
		s = propi.taggedValue("parent");
		if (s != null) {
		    String s1 = ci.taggedValue("parent");
		    if (s1 != null) {
			String[] sap = s.split(";");
			String[] sac = s1.split(";");
			for (String sc : sac) {
			    ClassInfo cip = model.classByName(sc.trim());
			    if (cip != null) {
				for (String sp : sap) {
				    sp = sp.trim();
				    String codep = null;
				    for (PropertyInfo propip : cip.properties().values()) {
					if (propip == null)
					    continue;
					if (propip.name().equalsIgnoreCase(sp)) {
					    codep = propip.initialValue();
					    if (codep == null || codep.length() == 0)
						codep = propip.name();
					    codep = PrepareToPrint(codep);
					    break;
					}
				    }
				    if (codep != null) {
					e1 = document.createElementNS(W3C_SKOS, "broader");
					e0.appendChild(e1);
					String nsp = cip.pkg().targetNamespace() + "#";
					addAttribute(document, e1, "rdf:resource", uri(nsp + cip.name() + "_" + codep));
				    }
				}
			    }
			}
		    }
		}
	    }

	    s = documentation(propi);
	    if (s != null && s.length() > 0) {
		e1 = document.createElementNS(W3C_SKOS, "definition");
		addAttribute(document, e1, "rdf:datatype", Options.W3C_XML_SCHEMA + "#string");
		addAttribute(document, e1, "xml:lang", language);
		e0.appendChild(e1);
		e1.setTextContent(s);
	    }

	    e1 = document.createElementNS(W3C_SKOS, "inScheme");
	    e0.appendChild(e1);
	    addAttribute(document, e1, "rdf:resource", uri(ns + ci.name()));
	}
    }

    private boolean ExportItem(Info i) {
	return true;
    }

    private boolean ExportValue(PropertyInfo propi) {
	return ExportItem(propi);
    }

    private boolean ExportProperty(PropertyInfo propi) {
	if (propi.name().length() == 0)
	    return false;

	return ExportItem(propi);
    }

    private boolean ExportClass(ClassInfo ci) {
	if (!ci.inSchema(pi))
	    return false;

	return ExportItem(ci);
    }

    private String getDef(Info i) {
	return PrepareToPrint(i.definition());
    }

    private String getDesc(Info i) {
	if (i.description() == null)
	    return null;
	return PrepareToPrint(i.description());
    }

    private boolean InheritsFrom(ClassInfo ci, String nam) {
	for (String t : ci.supertypes()) {
	    ClassInfo cix = model.classById(t);
	    if (cix.name().equals(nam))
		return true;
	    if (InheritsFrom(cix, nam))
		return true;
	}
	return false;
    }

    private String DetermineDefaultGeometry(ClassInfo ci) {
	/*
	 * FIXME String s = null; for (String t : ci.supertypes()) { ClassInfo cix =
	 * model.classById(t); s = DetermineDefaultGeometry(cix); if (s!=null) return s;
	 * }
	 */
	for (PropertyInfo propi : ci.properties().values()) {
	    Type ti = propi.typeInfo();
	    /*
	     * FIXME String s0 = ci.name(); String s1 = propi.name(); String s2 = ti.name;
	     */

	    if (ti.name.equals("GM_Point"))
		return "Point";
	    else if (ti.name.equals("GM_Curve"))
		return "Curve";
	    else if (ti.name.equals("GM_Surface"))
		return "Surface";
	    else if (ti.name.startsWith("GM_"))
		return "Geometry";
	    else if (ti.name.equals("GeometryInfo"))
		return "Geometry";
	    else {
		ClassInfo cix = model.classById(ti.id);
		if (cix != null) {
		    if (InheritsFrom(ci, "GM_Point"))
			return "Point";
		    else if (InheritsFrom(ci, "GM_Curve"))
			return "Curve";
		    else if (InheritsFrom(ci, "GM_Surface"))
			return "Surface";
		    else if (InheritsFrom(ci, "GM_Object"))
			return "Geometry";
		    else if (InheritsFrom(ci, "GM_Primitive"))
			return "Geometry";
		}
	    }
	}
	return null;
    }

    private void PrintClass(ClassInfo ci) {
	if (!ExportClass(ci))
	    return;

	Element e0, e1, e2, e3;

	e0 = document.createElementNS(W3C_OWL, "Class");
	root.appendChild(e0);
	addAttribute(document, e0, "rdf:about", uri(ns + ci.name()));

	boolean feature = false;
	for (String t : ci.supertypes()) {
	    ClassInfo cix = model.classById(t);
	    e1 = document.createElementNS(W3C_RDFS, "subClassOf");
	    e0.appendChild(e1);
	    addAttribute(document, e1, "rdf:resource", uri(cix.pkg().targetNamespace() + "#" + cix.name()));
	    if (cix.category() == Options.FEATURE)
		feature = true;
	}

	if (ci.category() == Options.FEATURE && !feature) {
	    e1 = document.createElementNS(W3C_RDFS, "subClassOf");
	    e0.appendChild(e1);
	    addAttribute(document, e1, "rdf:resource", OGC_GEOSPARQL + "Feature");
	}

	String g = DetermineDefaultGeometry(ci);
	if (g != null) {
	    e1 = document.createElementNS(W3C_RDFS, "subClassOf");
	    e0.appendChild(e1);
	    e2 = document.createElementNS(W3C_OWL, "Restriction");
	    e1.appendChild(e2);
	    e3 = document.createElementNS(W3C_OWL, "onProperty");
	    addAttribute(document, e3, "rdf:resource", OGC_GEOSPARQL + "defaultGeometry");
	    e2.appendChild(e3);
	    e3 = document.createElementNS(W3C_OWL, "allValuesFrom");
	    addAttribute(document, e3, "rdf:resource", OGC_GEOSPARQL + g);
	    e2.appendChild(e3);
	}

	String s;
	if (taggedValueForTitle != null) {
	    s = ci.taggedValue(taggedValueForTitle);
	    if (s != null) {
		e1 = document.createElementNS(W3C_RDFS, "label");
		e0.appendChild(e1);
		addAttribute(document, e1, "xml:lang", language);
		e1.setTextContent(s);
	    }
	}

	s = documentation(ci);
	if (s != null && s.length() > 0) {
	    e1 = document.createElementNS(DC, "description");
	    addAttribute(document, e1, "rdf:datatype", Options.W3C_XML_SCHEMA + "#string");
	    e0.appendChild(e1);
	    e1.setTextContent(s);
	}

	/*
	 * e1 = document.createElementNS(DC,"source");
	 * addAttribute(document,e1,"rdf:datatype",Options.W3C_XML_SCHEMA+ "#anyURI");
	 * e0.appendChild(e1); e1.setTextContent(
	 * "TODO - add reference to concept dictionary or feature catalogue?");
	 */

	for (PropertyInfo propi : ci.properties().values()) {
	    PrintProperty(propi);
	}
    }

    private void PrintProperty(PropertyInfo propi) {
	if (!ExportProperty(propi))
	    return;

	if (exportedProperties.contains(propi))
	    return;

	String assocId = "__FIXME";
	if (!propi.isAttribute()) {
	    if (!exportedRoles.contains(propi)) {
		assocId = "__" + propi.id();
		PrintPropertyDetail(propi, assocId);
		exportedRoles.add(propi);
		PropertyInfo propi2 = propi.reverseProperty();
		if (propi2 != null) {
		    if (ExportProperty(propi2)) {
			PrintPropertyDetail(propi2, assocId);
		    }
		    exportedRoles.add(propi2);
		}
	    } else {
		PropertyInfo propi2 = propi.reverseProperty();
		if (propi2 != null) {
		    assocId = "__" + propi2.id();
		}
	    }
	} else {
	    PrintPropertyDetail(propi, assocId);
	}

	exportedProperties.add(propi);
    }

    private void PrintPropertyDetail(PropertyInfo propi, String assocId) {

	Element e0, e1;
	String cn = propi.inClass().name();

	e0 = document.createElementNS(W3C_OWL, "DatatypeProperty");
	root.appendChild(e0);
	addAttribute(document, e0, "rdf:about", uri(PrepareToPrint(ns + cn + "." + propi.name())));

	String s = documentation(propi);
	if (s != null && s.length() > 0) {
	    e1 = document.createElementNS(DC, "description");
	    addAttribute(document, e1, "rdf:datatype", Options.W3C_XML_SCHEMA + "#string");
	    e0.appendChild(e1);
	    e1.setTextContent(s);
	}

	/*
	 * e1 = document.createElementNS(DC,"source");
	 * addAttribute(document,e1,"rdf:datatype",Options.W3C_XML_SCHEMA+ "#anyURI");
	 * e0.appendChild(e1); e1.setTextContent(
	 * "TODO - add reference to concept dictionary or feature catalogue?");
	 */

	if (taggedValueForTitle != null) {
	    s = propi.taggedValue(taggedValueForTitle);
	    if (s != null) {
		e1 = document.createElementNS(W3C_RDFS, "label");
		e0.appendChild(e1);
		addAttribute(document, e1, "xml:lang", language);
		e1.setTextContent(s);
	    }
	}

	e1 = document.createElementNS(W3C_RDFS, "domain");
	addAttribute(document, e1, "rdf:resource", uri(ns + cn));
	e0.appendChild(e1);

	Type ti = propi.typeInfo();
	if (!propi.isAttribute()) {
	    if (ti != null) {
		ClassInfo cix = model.classById(ti.id);
		if (cix != null) {
		    e1 = document.createElementNS(W3C_RDFS, "range");
		    addAttribute(document, e1, "rdf:resource", uri(cix.pkg().targetNamespace() + "#" + cix.name()));
		    e0.appendChild(e1);
		}
	    }
	} else {
	    if (ti != null) {
		ClassInfo cix = model.classById(ti.id);
		if (cix != null) {
		    int cat = cix.category();
		    switch (cat) {
		    case Options.CODELIST:
		    case Options.ENUMERATION:
			e1 = document.createElementNS(W3C_RDFS, "range");
			addAttribute(document, e1, "rdf:resource", W3C_SKOS + "Concept");
			e0.appendChild(e1);
			e1 = document.createElementNS(W3C_SKOS, "definition");
			addAttribute(document, e1, "rdf:resource", uri(ns + cix.name()));
			e0.appendChild(e1);
			break;
		    default:
			s = mapTypeByName(ti.name);
			if (s == null)
			    s = uri(cix.pkg().targetNamespace() + "#" + cix.name());
			e1 = document.createElementNS(W3C_RDFS, "range");
			addAttribute(document, e1, "rdf:resource", s);
			e0.appendChild(e1);
			break;
		    }
		} else {
		    s = mapTypeByName(ti.name);
		    if (s != null) {
			e1 = document.createElementNS(W3C_RDFS, "range");
			addAttribute(document, e1, "rdf:resource", s);
			e0.appendChild(e1);
		    }
		}
	    }
	}
    }

    private String mapTypeByName(String nam) {
	String s = null;
	MapEntry me = options.baseMapEntry(nam, "iso19136_2007");
	if (me != null) {
	    if (!me.p1.equals("")) {
		s = me.p1;
		int idx = s.indexOf(":");
		if (idx > 0) {
		    String nsabr = s.substring(0, idx);
		    s = uri(s.replace(nsabr + ":", options.fullNamespace(nsabr) + "#"));
		} else {
		    s = Options.W3C_XML_SCHEMA + "#" + s;
		}
	    }
	}
	return s;
    }

    public void write() {
	if (error || printed)
	    return;

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
	    return;
	}

	Properties outputFormat = OutputPropertiesFactory.getDefaultMethodProperties("xml");
	outputFormat.setProperty("indent", "yes");
	outputFormat.setProperty("{http://xml.apache.org/xalan}indent-amount", "2");
	outputFormat.setProperty("encoding", encoding);

	String fileName = pi.name().replace("/", "_").replace(" ", "_") + ".rdf";
	try {
	    OutputStream fout = new FileOutputStream(outputDirectory + "/" + fileName);
	    OutputStream bout = new BufferedOutputStream(fout);
	    OutputStreamWriter outputXML = new OutputStreamWriter(bout, outputFormat.getProperty("encoding"));

	    Serializer serializer = SerializerFactory.getSerializer(outputFormat);
	    serializer.setWriter(outputXML);
	    serializer.asDOMSerializer().serialize(document);
	    outputXML.close();
	    result.addResult(getTargetName(), outputDirectory, fileName, pi.targetNamespace() + "#");
	} catch (Exception e) {
	    String m = e.getMessage();
	    if (m != null) {
		result.addError(m);
	    }
	    e.printStackTrace(System.err);
	}

	printed = true;
    }

    @Override
    public void registerRulesAndRequirements(RuleRegistry r) {

	r.addRule("rule-rdf-prop-parent");
    }

    @Override
    public String getDefaultEncodingRule() {
	return "*";
    }

    @Override
    public String getTargetIdentifier() {
	return "rdf";
    }

    /**
     * This is the message text provision proper. It returns a message for a number.
     * 
     * @param mnr Message number
     * @return Message text or null
     */
    protected String messageText(int mnr) {
	switch (mnr) {
	case 12:
	    return "Directory named '$1$' does not exist or is not accessible.";
	case 1001:
	    return "URI '$1$' is invalid, but could not be converted to a valid URI.";

	}
	return null;
    }

    /**
     * <p>
     * This method returns messages belonging to the RDF target by their message
     * number. The organization corresponds to the logic in module
     * ShapeChangeResult. All functions in that class, which require an message
     * number can be redirected to the function at hand.
     * </p>
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
}
