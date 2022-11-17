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

package de.interactive_instruments.ShapeChange.Target.KML;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.docx4j.org.apache.xpath.XPathAPI;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.RuleRegistry;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.Target;
import de.interactive_instruments.ShapeChange.Util.XMLUtil;

public class XSLT implements Target {

    public static final String PARAM_WITH_SCHEMA = "withSchema";
    public static final String PARAM_WITH_EMPTY_PROPS = "withEmptyProps";
    public static final String PARAM_MAX_FOLDER_LEVEL = "maxFolderLevel";
    public static final String PARAM_NORTH = "north";
    public static final String PARAM_SOUTH = "south";
    public static final String PARAM_EAST = "east";
    public static final String PARAM_WEST = "west";
    public static final String PARAM_HREF = "href";
    public static final String PARAM_MIN_LOD_PIXELS = "minLodPixels";
    public static final String PARAM_NS_DEF = "nsDef";
    public static final String PARAM_BORIS_SPECIAL = "borisSpecial";
    public static final String PARAM_PORTRAYAL_RULE_DOC = "portrayalRuleDocument";
    public static final String PARAM_DEFAULT_REFERENCE = "defaultReference";
    public static final String PARAM_DEFAULT_STYLE_URL = "defaultStyleUrl";
    public static final String PARAM_GROUP_IN_FOLDER = "groupInFolder";
    public static final String PARAM_PROP_NAME_CASE = "propNameCase";
    public static final String PARAM_HREF_PROP_ONLY_ID = "hrefPropOnlyId";

    private static final String NS_XSL = "http://www.w3.org/1999/XSL/Transform";
    private static final String NS_FN = "http://www.w3.org/2005/xpath-functions";
    private static final String NS_KML = "http://www.opengis.net/kml/2.2";

    private PackageInfo pi = null;
    private Model model = null;
    private Options options = null;
    private ShapeChangeResult result = null;
    private boolean printed = false;
    private Document document = null;
    private Document kmlDocument = null;
    private Document ruledoc = null;
    private Element root = null;
    private Element kmldoc = null;
    private Comment schemaAnchor = null;
    private Comment callAnchor = null;
    private String outputDirectory = null;
    private String documentationTemplate = null;
    private String documentationNoValue = null;
    private int maxLevel = 0;
    private boolean withSchema = true;
    private boolean withEmptyProps = false;
    private HashMap<PackageInfo, Element> folderMapElement = new HashMap<PackageInfo, Element>();
    private HashMap<PackageInfo, String> folderMapFeatureTypes = new HashMap<PackageInfo, String>();
    private String north = "90.0";
    private String south = "-90.0";
    private String west = "-180.0";
    private String east = "-180.0";
    private String minLod = "2048";
    private String href = "http://services.interactive-instruments.de/ows7-kml/kml2/wfskml_regions.php";

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
	    outputDirectory = options.parameter(".");

	String s = options.parameter(this.getClass().getName(), PARAM_WITH_SCHEMA);
	if (s != null && s.equals("false"))
	    withSchema = false;

	s = options.parameter(this.getClass().getName(), PARAM_WITH_EMPTY_PROPS);
	if (s != null && s.equals("true"))
	    withEmptyProps = true;

	s = options.parameter(this.getClass().getName(), PARAM_MAX_FOLDER_LEVEL);
	if (s != null && s.length() > 0) {
	    maxLevel = Integer.parseInt(s);
	}

	s = options.parameter(this.getClass().getName(), PARAM_NORTH);
	if (s != null && s.length() > 0) {
	    north = s.trim();
	}

	s = options.parameter(this.getClass().getName(), PARAM_SOUTH);
	if (s != null && s.length() > 0) {
	    south = s.trim();
	}

	s = options.parameter(this.getClass().getName(), PARAM_EAST);
	if (s != null && s.length() > 0) {
	    east = s.trim();
	}

	s = options.parameter(this.getClass().getName(), PARAM_WEST);
	if (s != null && s.length() > 0) {
	    west = s.trim();
	}

	s = options.parameter(this.getClass().getName(), PARAM_HREF);
	if (s != null && s.length() > 0) {
	    href = s.trim();
	}

	s = options.parameter(this.getClass().getName(), PARAM_MIN_LOD_PIXELS);
	if (s != null && s.length() > 0) {
	    minLod = s.trim();
	}

	// change the default documentation template?
	documentationTemplate = options.parameter(this.getClass().getName(), "documentationTemplate");
	documentationNoValue = options.parameter(this.getClass().getName(), "documentationNoValue");

	result.addDebug(null, 10005, pi.name());

	document = createDocument();

	root = document.createElementNS(NS_XSL, "stylesheet");
	document.appendChild(root);
	addAttribute(document, root, "version", "2.0");
	addAttribute(document, root, "xmlns:gml", options.GML_NS);
	addAttribute(document, root, "xmlns:xsl", NS_XSL);
	addAttribute(document, root, "xmlns:kml", NS_KML);
	addAttribute(document, root, "xmlns:fn", NS_FN);
	addAttribute(document, root, "xmlns:xlink", options.fullNamespace("xlink"));
	addAttribute(document, root, "xmlns:xs", "http://www.w3.org/2001/XMLSchema");
	addAttribute(document, root, "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
	s = options.parameter(this.getClass().getName(), PARAM_NS_DEF);
	if (s != null && s.trim().length() > 0) {
	    String[] sl = s.split(" ");
	    for (String ns : sl) {
		String[] nsdef = ns.split("=");
		if (nsdef.length == 2 && nsdef[0] != null && nsdef[0].trim().length() > 0 && nsdef[1] != null
			&& nsdef[1].trim().length() > 0)
		    addAttribute(document, root, nsdef[0], nsdef[1]);
	    }
	}

	addAttribute(document, root, "xmlns:" + pi.xmlns(), pi.targetNamespace());

	Element e0, e1, e2, e4, e5;

	e1 = document.createElementNS(NS_XSL, "output");
	root.appendChild(e1);
	addAttribute(document, e1, "method", "xml");
	addAttribute(document, e1, "indent", "yes");

	s = options.parameter(this.getClass().getName(), PARAM_BORIS_SPECIAL);
	if (s != null && s.equalsIgnoreCase("true")) {
	    e1 = document.createElementNS(NS_XSL, "param");
	    root.appendChild(e1);
	    addAttribute(document, e1, "name", "dir");
	    addAttribute(document, e1, "as", "xs:string");
	    addAttribute(document, e1, "required", "yes");

	    e1 = document.createElementNS(NS_XSL, "variable");
	    root.appendChild(e1);
	    addAttribute(document, e1, "name", "umretab_pfad");
	    addAttribute(document, e1, "select", "'umretabs'");
	}

	e1 = document.createElementNS(NS_XSL, "template");
	root.appendChild(e1);
	addAttribute(document, e1, "match", "/");

	s = options.parameter(this.getClass().getName(), PARAM_BORIS_SPECIAL);
	if (s != null && s.equalsIgnoreCase("true")) {
	    e2 = document.createElementNS(NS_XSL, "variable");
	    e1.appendChild(e2);
	    addAttribute(document, e2, "name", "fname_prae");
	    addAttribute(document, e2, "select", "'boriskmlout_'");

	    e2 = document.createElementNS(NS_XSL, "variable");
	    e1.appendChild(e2);
	    addAttribute(document, e2, "name", "stage");
	    addAttribute(document, e2, "select", "//boris:BR_Bodenrichtwert[1]/boris:stichtag");

	    e2 = document.createElementNS(NS_XSL, "variable");
	    e1.appendChild(e2);
	    addAttribute(document, e2, "name", "stag");
	    addAttribute(document, e2, "select", "$stage[1]");

	    e2 = document.createElementNS(NS_XSL, "variable");
	    e1.appendChild(e2);
	    addAttribute(document, e2, "name", "jahr");
	    addAttribute(document, e2, "select", "substring(format-date($stag,'[Y]', 'en', 'AD', 'UK'),3)");

	    e2 = document.createElementNS(NS_XSL, "variable");
	    e1.appendChild(e2);
	    addAttribute(document, e2, "name", "fname");
	    addAttribute(document, e2, "select", "concat($dir, '/', $fname_prae, $jahr, '.kml')");

	    e2 = document.createElementNS(NS_XSL, "result-document");
	    e1.appendChild(e2);
	    addAttribute(document, e2, "href", "{$fname}");

	    e4 = document.createElementNS(NS_XSL, "call-template");
	    e2.appendChild(e4);
	    addAttribute(document, e4, "name", "alle");

	    e2 = document.createElementNS(NS_XSL, "for-each");
	    e1.appendChild(e2);
	    addAttribute(document, e2, "select", "//boris:BR_Umrechnungstabelle");

	    e4 = document.createElementNS(NS_XSL, "result-document");
	    e2.appendChild(e4);
	    addAttribute(document, e4, "href", "{$dir}/{$umretab_pfad}/{@gml:id}.htm");

	    e5 = document.createElementNS(NS_XSL, "value-of");
	    e4.appendChild(e5);
	    addAttribute(document, e5, "select", "boris:umrechnungstabelleDokument");

	    e1 = document.createElementNS(NS_XSL, "template");
	    root.appendChild(e1);
	    addAttribute(document, e1, "name", "alle");
	}

	e2 = document.createElementNS(NS_KML, "kml");
	e1.appendChild(e2);

	kmldoc = document.createElementNS(NS_KML, "Document");
	e2.appendChild(kmldoc);

	e4 = document.createElementNS(NS_KML, "open");
	e4.appendChild(document.createTextNode("0"));
	kmldoc.appendChild(e4);

	schemaAnchor = document.createComment("Call styling template for each feature type");
	kmldoc.appendChild(schemaAnchor);

	callAnchor = document.createComment("Styling templates for each feature type");
	kmldoc.appendChild(callAnchor);

	s = options.parameter(this.getClass().getName(), PARAM_PORTRAYAL_RULE_DOC);
	if (s != null && !s.isEmpty()) {
	    DocumentBuilder builder = null;
	    try {
		System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
			"org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setValidating(false);
		factory.setFeature("http://apache.org/xml/features/validation/schema", false);
		factory.setIgnoringElementContentWhitespace(true);
		factory.setIgnoringComments(true);
		factory.setXIncludeAware(true);
		factory.setFeature("http://apache.org/xml/features/xinclude/fixup-base-uris", false);
		builder = factory.newDocumentBuilder();
		ruledoc = builder.parse(s);
	    } catch (Exception e) {
		System.err.println("Error with portrayal rules document.");
	    }
	}

	kmlDocument = createDocument();

	e0 = kmlDocument.createElementNS(NS_KML, "kml");
	kmlDocument.appendChild(e0);

	e1 = kmlDocument.createElementNS(NS_KML, "Document");
	e0.appendChild(e1);

	e2 = kmlDocument.createElementNS(NS_KML, "name");
	s = pi.taggedValue("name");
	if (StringUtils.isNotBlank(s))
	    e2.appendChild(kmlDocument.createTextNode(s.trim()));
	else
	    e2.appendChild(kmlDocument.createTextNode(pi.name()));
	e1.appendChild(e2);

	e2 = kmlDocument.createElementNS(NS_KML, "visibility");
	e2.appendChild(kmlDocument.createTextNode("1"));
	e1.appendChild(e2);

	e2 = kmlDocument.createElementNS(NS_KML, "open");
	e2.appendChild(kmlDocument.createTextNode("1"));
	e1.appendChild(e2);

	e2 = kmlDocument.createElementNS(NS_KML, "description");
	s = pi.derivedDocumentation(documentationTemplate, documentationNoValue);
	if (s != null && s.length() > 0)
	    e2.appendChild(kmlDocument.createTextNode(s.trim()));
	e1.appendChild(e2);

	e2 = kmlDocument.createElementNS(NS_KML, "Snippet");
	addAttribute(kmlDocument, e2, "maxLines", "0");
	e1.appendChild(e2);

	if (maxLevel == 0) {
	    addNetworkLink(pi, e1);
	} else {
	    try {
		for (PackageInfo sub : pi.containedPackages()) {
		    createFolders(sub, e1, 1);
		}
	    } catch (Exception e) {
		result.addError("Cannot access sub-packages of package " + pi.name() + " during KML generation.");
		e.printStackTrace();
	    }
	}
    }

    /**
     * Create folder structure
     * 
     * @param pix   tbd
     * @param e     tbd
     * @param level tbd
     * @return tbd
     * @throws Exception tbd
     */
    protected boolean createFolders(PackageInfo pix, Element e, int level) throws Exception {
	String sss = pix.name();
	result.addDebug("!!! " + sss + " - " + level);

	if (level > maxLevel)
	    return true;

	/**
	 * Create folder element for this package
	 */
	Element e1, e2;

	e1 = kmlDocument.createElementNS(NS_KML, "Folder");

	e2 = kmlDocument.createElementNS(NS_KML, "name");
	String s = pix.taggedValue("name");
	if (StringUtils.isNotBlank(s))
	    e2.appendChild(kmlDocument.createTextNode(s.trim()));
	else
	    e2.appendChild(kmlDocument.createTextNode(pix.name()));
	e1.appendChild(e2);

	e2 = kmlDocument.createElementNS(NS_KML, "visibility");
	e2.appendChild(kmlDocument.createTextNode("1"));
	e1.appendChild(e2);

	e2 = kmlDocument.createElementNS(NS_KML, "open");
	e2.appendChild(kmlDocument.createTextNode("0"));
	e1.appendChild(e2);

	e2 = kmlDocument.createElementNS(NS_KML, "description");
	s = pi.derivedDocumentation(documentationTemplate, documentationNoValue);
	if (s != null && s.length() > 0)
	    e2.appendChild(kmlDocument.createTextNode(s.trim()));
	e1.appendChild(e2);

	e2 = kmlDocument.createElementNS(NS_KML, "Snippet");
	addAttribute(kmlDocument, e2, "maxLines", "0");
	e1.appendChild(e2);

	boolean empty = true;
	SortedSet<PackageInfo> subs = pix.containedPackages();
	if (subs.size() == 0 || level >= maxLevel) {
	    for (ClassInfo cix : model.classes(pix)) {
		if (cix.pkg() != pix)
		    continue;
		String ssss = cix.name();
		result.addDebug("!!! " + ssss);
		if (testProcess(cix)) {
		    empty = false;
		    break;
		}
	    }
	    if (!empty)
		addNetworkLink(pix, e1);

	} else {
	    /**
	     * Navigate through sub packages
	     */
	    for (PackageInfo sub : subs) {
		boolean empty2 = createFolders(sub, e1, level + 1);
		empty = (empty && empty2);
	    }
	}
	if (!empty)
	    e.appendChild(e1);

	result.addDebug("--- " + sss + " - " + empty);
	return empty;
    }

    private void addNetworkLink(PackageInfo pix, Element e1) {
	Element e3, e4, e5, e6;

	e3 = kmlDocument.createElementNS(NS_KML, "NetworkLink");
	e1.appendChild(e3);

	e4 = kmlDocument.createElementNS(NS_KML, "name");
	e4.appendChild(kmlDocument.createTextNode("Tiles"));
	e3.appendChild(e4);

	e4 = kmlDocument.createElementNS(NS_KML, "visibility");
	e4.appendChild(kmlDocument.createTextNode("1"));
	e3.appendChild(e4);

	e4 = kmlDocument.createElementNS(NS_KML, "open");
	e4.appendChild(kmlDocument.createTextNode("0"));
	e3.appendChild(e4);

	e4 = kmlDocument.createElementNS(NS_KML, "refreshVisibility");
	e4.appendChild(kmlDocument.createTextNode("0"));
	e3.appendChild(e4);

	e4 = kmlDocument.createElementNS(NS_KML, "flyToView");
	e4.appendChild(kmlDocument.createTextNode("0"));
	e3.appendChild(e4);

	e4 = kmlDocument.createElementNS(NS_KML, "Region");
	e3.appendChild(e4);

	e5 = kmlDocument.createElementNS(NS_KML, "LatLonAltBox");
	e4.appendChild(e5);

	e6 = kmlDocument.createElementNS(NS_KML, "north");
	e6.appendChild(kmlDocument.createTextNode(north));
	e5.appendChild(e6);

	e6 = kmlDocument.createElementNS(NS_KML, "south");
	e6.appendChild(kmlDocument.createTextNode(south));
	e5.appendChild(e6);

	e6 = kmlDocument.createElementNS(NS_KML, "east");
	e6.appendChild(kmlDocument.createTextNode(east));
	e5.appendChild(e6);

	e6 = kmlDocument.createElementNS(NS_KML, "west");
	e6.appendChild(kmlDocument.createTextNode(west));
	e5.appendChild(e6);

	e5 = kmlDocument.createElementNS(NS_KML, "Lod");
	e4.appendChild(e5);

	e6 = kmlDocument.createElementNS(NS_KML, "minLodPixels");
	e6.appendChild(kmlDocument.createTextNode(minLod));
	e5.appendChild(e6);

	e6 = kmlDocument.createElementNS(NS_KML, "maxLodPixels");
	e6.appendChild(kmlDocument.createTextNode("-1"));
	e5.appendChild(e6);

	e4 = kmlDocument.createElementNS(NS_KML, "Link");
	e3.appendChild(e4);

	e5 = kmlDocument.createElementNS(NS_KML, "href");
	e5.appendChild(kmlDocument.createTextNode(href));
	e4.appendChild(e5);

	e5 = kmlDocument.createElementNS(NS_KML, "viewRefreshMode");
	e5.appendChild(kmlDocument.createTextNode("onRegion"));
	e4.appendChild(e5);

	e5 = kmlDocument.createElementNS(NS_KML, "viewFormat");
	e5.appendChild(kmlDocument.createTextNode(
		"BBOX=[bboxWest],[bboxSouth],[bboxEast],[bboxNorth]&LOOKAT=[lookatLon],[lookatLat],[lookatRange],[lookatTilt],[lookatHeading]&CAMERA=[cameraLon],[cameraLat],[cameraAlt]&LOOKATTERR=[lookatTerrainLon],[lookatTerrainLat],[lookatTerrainAlt]&VIEW=[horizFov],[vertFov],[horizPixels],[vertPixels],[terrainEnabled]"));
	e4.appendChild(e5);

	e5 = kmlDocument.createElementNS(NS_KML, "httpQuery");
	e4.appendChild(e5);

	folderMapElement.put(pix, e5);
	folderMapFeatureTypes.put(pix, "");
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

    private String kmlReference(ClassInfo ci) {
	String s = ci.taggedValue("kmlReference");
	if (StringUtils.isBlank(s)) {
	    s = options.parameter(this.getClass().getName(), PARAM_DEFAULT_REFERENCE);
	}
	if (StringUtils.isNotBlank(s)) {
	    return s.trim();
	}
	return null;
    }

    private String kmlStyleUrl(ClassInfo ci) {
	String s = ci.taggedValue("kmlStyleUrl");
	if (StringUtils.isNotBlank(s)) {
	    return s.trim();
	}
	return kmlStyleUrl(ci, ci.pkg());
    }

    private String defaultKmlStyleUrl(ClassInfo ci) {
	String s = options.parameter(this.getClass().getName(), "defaultStyleUrl(" + ci.name() + ")");
	if (s != null && s.length() > 0) {
	    return s.trim();
	}
	s = options.parameter(this.getClass().getName(), PARAM_DEFAULT_STYLE_URL);
	if (s != null && s.length() > 0) {
	    return s.trim();
	}
	return null;
    }

    private String kmlStyleUrl(ClassInfo ci, PackageInfo pi) {
	if (pi == null)
	    return defaultKmlStyleUrl(ci);

	String s = pi.taggedValue("kmlStyleUrl");
	if (StringUtils.isNotBlank(s)) {
	    return s.trim();
	}
	if (pi == null || pi.isAppSchema() || pi.owner() == null) {
	    return defaultKmlStyleUrl(ci);
	}
	return kmlStyleUrl(ci, pi.owner());
    }

    private CDATASection cdataType(ClassInfo ci) {
	CDATASection cd = null;
	String s = kmlReference(ci);
	String sn = stringType(ci);
	String s2 = ci.definition().trim().replace("'", "").replace("\"", "");
	if (s != null && s2 != null) {
	    cd = document.createCDATASection(
		    "<a href='" + s + "' title='" + s2 + "'><big><b><i>" + sn + "</i></b></big></a>");
	} else if (s != null) {
	    cd = document.createCDATASection("<a href='" + s + "'><big><b><i>" + sn + "</i></b></big></a>");
	} else if (s2 != null) {
	    cd = document.createCDATASection("<div title='" + s2 + "'><big><b><i>" + sn + "</i></b></big></div>");
	} else
	    cd = document.createCDATASection("<big><b><i>" + sn + "</i></b></big>");
	return cd;
    }

    private String stringType(ClassInfo ci) {
	String s = ci.taggedValue("name");
	if (StringUtils.isNotBlank(s))
	    return s.trim();
	return ci.name();
    }

    private String stringProperty(PropertyInfo propi) {
	String s = propi.taggedValue("name");
	if (StringUtils.isNotBlank(s))
	    return s.trim();
	return propi.name();
    }

    private boolean testProcess(ClassInfo ci) {
	int cat = ci.category();
	if (cat != Options.FEATURE) {
	    return false;
	}
	if (ci.isAbstract())
	    return false;

	// TODO add option for a white or black list of feature types to process

	return true;
    }

    public void process(ClassInfo ci) {
	if (!testProcess(ci))
	    return;

	Element schema, schemaData, e0, e1, e2, e3, e4, e5, e6, e7, e8;

	if (withSchema) {
	    e0 = document.createElementNS(NS_XSL, "if");
	    addAttribute(document, e0, "test", "//" + ci.pkg().xmlns() + ":" + ci.name());
	    kmldoc.insertBefore(e0, schemaAnchor);

	    schema = document.createElementNS(NS_KML, "Schema");
	    addAttribute(document, schema, "id", ci.name() + "Schema");
	    e0.appendChild(schema);

	    e1 = document.createElementNS(NS_KML, "SimpleField");
	    addAttribute(document, e1, "type", "string");
	    addAttribute(document, e1, "name", "type");
	    schema.appendChild(e1);

	    e2 = document.createElementNS(NS_KML, "displayName");
	    addAttribute(document, e1, "type", "string");
	    CDATASection cd = cdataType(ci);
	    e2.appendChild(cd);
	    e1.appendChild(e2);
	} else
	    schema = null;

	if (options.parameter(this.getClass().getName(), PARAM_GROUP_IN_FOLDER) != null
		&& options.parameter(this.getClass().getName(), PARAM_GROUP_IN_FOLDER).equalsIgnoreCase("true")) {
	    e3 = document.createElementNS(NS_XSL, "if");
	    addAttribute(document, e3, "test", "//" + ci.pkg().xmlns() + ":" + ci.name());
	    kmldoc.insertBefore(e3, callAnchor);

	    e4 = document.createElementNS(NS_KML, "Folder");
	    e3.appendChild(e4);

	    e5 = document.createElementNS(NS_KML, "name");
	    e5.appendChild(document.createTextNode(ci.name()));
	    e4.appendChild(e5);

	    e1 = document.createElementNS(NS_XSL, "for-each");
	    addAttribute(document, e1, "select", "//" + ci.pkg().xmlns() + ":" + ci.name());
	    e4.appendChild(e1);
	} else {
	    e1 = document.createElementNS(NS_XSL, "for-each");
	    addAttribute(document, e1, "select", "//" + ci.pkg().xmlns() + ":" + ci.name());
	    kmldoc.insertBefore(e1, callAnchor);
	}

	e2 = document.createElementNS(NS_XSL, "call-template");
	addAttribute(document, e2, "name", ci.name());
	e1.appendChild(e2);

	e1 = document.createElementNS(NS_XSL, "template");
	root.appendChild(e1);
	addAttribute(document, e1, "name", ci.name());

	e2 = document.createElementNS(NS_KML, "Placemark");
	e1.appendChild(e2);

	e3 = document.createElementNS(NS_XSL, "attribute");
	e2.appendChild(e3);
	addAttribute(document, e3, "name", "id");

	e4 = document.createElementNS(NS_XSL, "value-of");
	e3.appendChild(e4);
	addAttribute(document, e4, "select", "@gml:id");

	e3 = document.createElementNS(NS_XSL, "choose");
	e2.appendChild(e3);

	String s = getTaggedProperty(ci, "kmlName");
	if (s != null) {
	    e4 = document.createElementNS(NS_XSL, "when");
	    e3.appendChild(e4);
	    addAttribute(document, e4, "test", s);
	    e5 = document.createElementNS(NS_KML, "name");
	    e4.appendChild(e5);
	    e6 = document.createElementNS(NS_XSL, "value-of");
	    e5.appendChild(e6);
	    addAttribute(document, e6, "select", "./" + s + "[1]");
	}

	e4 = document.createElementNS(NS_XSL, "when");
	e3.appendChild(e4);
	addAttribute(document, e4, "test", "gml:name");
	e5 = document.createElementNS(NS_KML, "name");
	e4.appendChild(e5);
	e6 = document.createElementNS(NS_XSL, "value-of");
	e5.appendChild(e6);
	addAttribute(document, e6, "select", "./gml:name[1]");

	e4 = document.createElementNS(NS_XSL, "otherwise");
	e3.appendChild(e4);
	e5 = document.createElementNS(NS_KML, "name");
	e4.appendChild(e5);
	s = ci.taggedValue("name");
	if (StringUtils.isNotBlank(s)) {
	    e5.appendChild(document.createTextNode(s));
	} else {
	    e6 = document.createElementNS(NS_XSL, "value-of");
	    e5.appendChild(e6);
	    addAttribute(document, e6, "select", "@gml:id");
	}

	e3 = document.createElementNS(NS_KML, "visibility");
	e2.appendChild(e3);
	e3.appendChild(document.createTextNode("1"));

	s = getTaggedProperty(ci, "kmlTimeStamp");
	if (s != null) {
	    e4 = document.createElementNS(NS_XSL, "if");
	    e2.appendChild(e4);
	    addAttribute(document, e4, "test", s);
	    e5 = document.createElementNS(NS_KML, "TimeStamp");
	    e4.appendChild(e5);
	    e6 = document.createElementNS(NS_KML, "when");
	    e5.appendChild(e6);
	    e7 = document.createElementNS(NS_XSL, "value-of");
	    e6.appendChild(e7);
	    addAttribute(document, e7, "select", "./" + s + "[1]");
	} else {
	    s = getTaggedProperty(ci, "kmlTimeSpanBegin");
	    String s2 = getTaggedProperty(ci, "kmlTimeSpanEnd");
	    if (s != null && s2 != null) {
		e4 = document.createElementNS(NS_XSL, "if");
		e2.appendChild(e4);
		addAttribute(document, e4, "test", s + " or " + s2);
		e5 = document.createElementNS(NS_KML, "TimeSpan");
		e4.appendChild(e5);
		e6 = document.createElementNS(NS_XSL, "if");
		e5.appendChild(e6);
		addAttribute(document, e6, "test", s);
		e7 = document.createElementNS(NS_KML, "begin");
		e6.appendChild(e7);
		e8 = document.createElementNS(NS_XSL, "value-of");
		e7.appendChild(e8);
		addAttribute(document, e8, "select", "./" + s + "[1]");
		e6 = document.createElementNS(NS_XSL, "if");
		e5.appendChild(e6);
		addAttribute(document, e6, "test", s2);
		e7 = document.createElementNS(NS_KML, "end");
		e6.appendChild(e7);
		e8 = document.createElementNS(NS_XSL, "value-of");
		e7.appendChild(e8);
		addAttribute(document, e8, "select", "./" + s2 + "[1]");
	    } else if (s != null) {
		e4 = document.createElementNS(NS_XSL, "if");
		e2.appendChild(e4);
		addAttribute(document, e4, "test", s);
		e5 = document.createElementNS(NS_KML, "TimeSpan");
		e4.appendChild(e5);
		e6 = document.createElementNS(NS_KML, "begin");
		e5.appendChild(e6);
		e7 = document.createElementNS(NS_XSL, "value-of");
		e6.appendChild(e7);
		addAttribute(document, e7, "select", "./" + s + "[1]");
	    } else if (s2 != null) {
		e4 = document.createElementNS(NS_XSL, "if");
		e2.appendChild(e4);
		addAttribute(document, e4, "test", s2);
		e5 = document.createElementNS(NS_KML, "TimeSpan");
		e4.appendChild(e5);
		e6 = document.createElementNS(NS_KML, "end");
		e5.appendChild(e6);
		e7 = document.createElementNS(NS_XSL, "value-of");
		e6.appendChild(e7);
		addAttribute(document, e7, "select", "./" + s2 + "[1]");
	    }
	}

	if (ruledoc != null) {
	    e4 = null;
	    String def = null;
	    try {		
		NodeList styles = XPathAPI.selectNodeList(ruledoc,
			"/*/se:FeatureTypeStyle[se:FeatureTypeName=\"" + ci.qname() + "\"]/se:Rule",
			ruledoc.getDocumentElement());
		for (int i = styles.getLength() - 1; i >= 0; i--) {
		    Node style = styles.item(i);		    
		    NodeList filters = XPathAPI.selectNodeList(style, "ogc:Filter", ruledoc.getDocumentElement());
		    if (filters.getLength() > 0) {
			Node filter = filters.item(0);
			String test = "";
			NodeList preds = filter.getChildNodes();
			for (int j = 0; j < preds.getLength(); j++) {
			    Node pred = preds.item(j);
			    test += getString(pred);
			}
			if (e4 == null) {
			    e4 = document.createElementNS(NS_XSL, "choose");
			    e2.appendChild(e4);
			}
			e5 = document.createElementNS(NS_XSL, "when");
			e4.appendChild(e5);
			addAttribute(document, e5, "test", test);			
			NodeList symbols = XPathAPI.selectNodeList(style, "se:OnlineResource/@xlink:href",
				ruledoc.getDocumentElement());
			if (symbols.getLength() > 0) {
			    String sym = symbols.item(0).getTextContent();
			    String id = (sym.split("Symbol="))[1].trim();
			    e6 = document.createElementNS(NS_KML, "styleUrl");
			    e5.appendChild(e6);
			    e6.appendChild(document.createTextNode(sym.replace("&", "%26")
				    + "%26Encoding=application/vnd.google-earth.kml+xml#" + id));
			}
		    } else {			
			NodeList symbols = XPathAPI.selectNodeList(style, "se:OnlineResource/@xlink:href",
				ruledoc.getDocumentElement());
			if (symbols.getLength() > 0) {
			    String sym = symbols.item(0).getTextContent();
			    String id = (sym.split("Symbol="))[1].trim();
			    def = sym.replace("&", "%26") + "%26Encoding=application/vnd.google-earth.kml+xml#" + id;
			}
		    }
		}
	    } catch (TransformerException e) {
		result.addError(
			"Xpath error while processing portrayal rule document in target KML: " + e.getMessage());
	    }
	    if (def != null) {
		if (e4 != null) {
		    e5 = document.createElementNS(NS_XSL, "otherwise");
		    e4.appendChild(e5);
		} else
		    e5 = e2;
		e6 = document.createElementNS(NS_KML, "styleUrl");
		e5.appendChild(e6);
		e6.appendChild(document.createTextNode(def));
	    }
	} else {
	    s = kmlStyleUrl(ci);
	    if (s != null) {
		e3 = document.createElementNS(NS_KML, "styleUrl");
		e2.appendChild(e3);
		e3.appendChild(document.createTextNode(s));
	    }
	}

	e3 = document.createElementNS(NS_KML, "ExtendedData");
	e2.appendChild(e3);

	if (withSchema) {
	    schemaData = document.createElementNS(NS_KML, "SchemaData");
	    e3.appendChild(schemaData);
	    addAttribute(document, schemaData, "schemaUrl", "#" + ci.name() + "Schema");

	    e4 = document.createElementNS(NS_KML, "SimpleData");
	    schemaData.appendChild(e4);
	    addAttribute(document, e4, "name", "type");
	} else {
	    schemaData = e3;
	    e3 = document.createElementNS(NS_KML, "Data");
	    schemaData.appendChild(e3);
	    addAttribute(document, e3, "name", stringType(ci));

	    e4 = document.createElementNS(NS_KML, "displayName");
	    CDATASection cd = cdataType(ci);
	    e4.appendChild(cd);
	    e3.appendChild(e4);

	    e4 = document.createElementNS(NS_KML, "value");
	    e3.appendChild(e4);
	}

	e5 = document.createElementNS(NS_XSL, "choose");
	e4.appendChild(e5);

	e6 = document.createElementNS(NS_XSL, "when");
	e5.appendChild(e6);
	addAttribute(document, e6, "test", "gml:description");
	e7 = document.createElementNS(NS_XSL, "value-of");
	e6.appendChild(e7);
	addAttribute(document, e7, "select", "./gml:description");

	e6 = document.createElementNS(NS_XSL, "otherwise");
	e5.appendChild(e6);
	s = pi.derivedDocumentation(documentationTemplate, documentationNoValue);
	if (s != null && s.length() > 0)
	    e6.appendChild(document.createTextNode(s));

	e3 = document.createElementNS(NS_XSL, "apply-templates");
	e2.appendChild(e3);
	addAttribute(document, e3, "select", "*/gml:Polygon|*/gml:LineString|*/gml:Point");

	createAllPropertyDefinitions(pi, ci, true, schema, schemaData);

	/*
	 * add reference in KML root file
	 */
	PackageInfo pix = getFolderPackage(ci);
	if (pix != null) {
	    s = folderMapFeatureTypes.get(pix);
	    if (s != null) {
		if (s.length() == 0) {
		    folderMapFeatureTypes.put(pix, ci.qname());
		} else {
		    folderMapFeatureTypes.put(pix, s + "," + ci.qname());
		}
	    }
	}
    }

    private String getString(Node pred) {
	String test = "";

	if (pred.getLocalName().equals("Or") || pred.getLocalName().equals("And")
		|| pred.getLocalName().equals("Not")) {
	    NodeList preds2 = pred.getChildNodes();
	    test += " (";
	    for (int k = 0; k < preds2.getLength(); k++) {
		Node pred2 = preds2.item(k);
		if (k > 0) {
		    if (pred.getLocalName().equals("Or"))
			test += " or ";
		    else if (pred.getLocalName().equals("And"))
			test += " and ";
		}
		if (k == 0) {
		    if (pred.getLocalName().equals("Not"))
			test += "not ";
		}
		test += getString(pred2);
	    }
	    test += ") ";
	} else if (pred.getLocalName().startsWith("PropertyIs")) {
	    NodeList preds2 = pred.getChildNodes();
	    for (int k = 0; k < preds2.getLength(); k++) {
		Node pred2 = preds2.item(k);
		String s = getString(pred2);
		if (k > 0) {
		    if (pred.getLocalName().equals("PropertyIsEqualTo"))
			test += " = ";
		    else if (pred.getLocalName().equals("PropertyIsNotEqualTo"))
			test += " != ";
		    else if (pred.getLocalName().equals("PropertyIsLessThan")) {
			test += " &lt; ";
			s = s.replace("'", "");
		    } else if (pred.getLocalName().equals("PropertyIsLessThanOrEqualTo")) {
			test += " &lt;= ";
			s = s.replace("'", "");
		    } else if (pred.getLocalName().equals("PropertyIsGreaterThan")) {
			test += " &gt; ";
			s = s.replace("'", "");
		    } else if (pred.getLocalName().equals("PropertyIsGreaterThanOrEqualTo")) {
			test += " &gt;= ";
			s = s.replace("'", "");
		    } else
			test += " !!!" + pred.getLocalName() + " ";
		}
		test += s;
	    }
	} else if (pred.getLocalName().equals("PropertyName")) {
	    test += pred.getTextContent();
	} else if (pred.getLocalName().equals("Literal")) {
	    test += "'" + pred.getTextContent() + "'";
	} else {
	    test += " !!!" + pred.getLocalName() + " ";
	}

	return test;
    }

    private PackageInfo getFolderPackage(ClassInfo ci) {
	PackageInfo pi = ci.pkg();
	if (pi == null)
	    return null;
	if (folderMapElement.containsKey(pi))
	    return pi;
	return getFolderPackage(pi.owner());
    }

    private PackageInfo getFolderPackage(PackageInfo pi) {
	if (pi == null)
	    return null;
	if (folderMapElement.containsKey(pi))
	    return pi;
	return getFolderPackage(pi.owner());
    }

    private String getTaggedProperty(ClassInfo ci, String tag) {
	if (ci == null)
	    return null;
	if (ci.pkg() == null) {
	    result.addError(null, 139, ci.name());
	    return null;
	}

	String s;
	for (Iterator<PropertyInfo> j = ci.properties().values().iterator(); j.hasNext();) {
	    PropertyInfo propi = j.next();
	    s = propi.taggedValue(tag);
	    if (StringUtils.equalsIgnoreCase(s,"true"))
		return propi.qname();

	    // TODO temporary settings
	    if (tag.equals("kmlName")) {
		s = options.parameter(this.getClass().getName(), "kmlName(" + propi.name() + ")");
		if (s != null && s.toLowerCase().equals("true")) {
		    return propi.qname();
		}
	    }
	    // if (tag.equals("kmlName") && propi.taggedValue(tag)!=null &&
	    // propi.taggedValue(tag).length()>0)// && propi.qname().equals("cp:label"))
	    // return propi.qname();
	    // if (tag.equals("kmlName") && propi.qname().equals("cp:label"))
	    // return propi.qname();
	    // if (tag.equals("kmlName") && propi.name().equals("name"))
	    // return propi.qname();
	    // if (tag.equals("kmlTimeSpanBegin") &&
	    // propi.name().equals("beginLifespanVersion"))
	    // return propi.qname();
	    // if (tag.equals("kmlTimeSpanEnd") &&
	    // propi.name().equals("endLifespanVersion"))
	    // return propi.qname();
	}
	SortedSet<String> st = ci.supertypes();
	if (st != null) {
	    for (Iterator<String> i = st.iterator(); i.hasNext();) {
		s = getTaggedProperty(model.classById(i.next()), tag);
		if (s != null)
		    return s;
	    }
	}

	return null;
    }

    private int createAllPropertyDefinitions(PackageInfo asi, ClassInfo ci, boolean local, Element schema,
	    Element schemaData) {
	if (ci == null)
	    return 0;
	if (ci.pkg() == null) {
	    result.addError(null, 139, ci.name());
	    return 0;
	}

	int ct = 0;

	SortedSet<String> st = ci.supertypes();
	if (st != null) {
	    for (Iterator<String> i = st.iterator(); i.hasNext();) {
		ct += createAllPropertyDefinitions(asi, model.classById(i.next()), false, schema, schemaData);
	    }
	}
	for (Iterator<PropertyInfo> j = ci.properties().values().iterator(); j.hasNext();) {
	    PropertyInfo propi = j.next();
	    ct += createPropertyDefinition(asi, ci, propi, local, schema, schemaData);
	}

	return ct;
    }

    private CDATASection cdataProperty(ClassInfo ci, PropertyInfo propi) {
	String s = kmlReference(ci);
	String s2 = propi.definition().trim().replace("'", "").replace("\"", "");
	String s3 = "<b><i>" + stringProperty(propi) + "</i></b>";
	CDATASection cd = null;
	if (s != null && s2 != null) {
	    cd = document.createCDATASection("<a href='" + s + "' title='" + s2 + "'>" + s3 + "</a>");
	} else if (s != null) {
	    cd = document.createCDATASection("<a href='" + s + "'>" + s3 + "</a>");
	} else if (s2 != null) {
	    cd = document.createCDATASection("<div title='" + s2 + "'>" + s3 + "</div>");
	}
	return cd;
    }

    private int createPropertyDefinition(PackageInfo asi, ClassInfo ci, PropertyInfo propi, boolean local,
	    Element schema, Element schemaData) {
	if (!propi.isNavigable())
	    return 0;
	if (propi.isRestriction())
	    return 0;

	ClassInfo vi = model.classById(propi.typeInfo().id);
	if (vi == null)
	    return 0;
	String t = vi.name();
	int cat = vi.category();
	if (t.startsWith("GM_"))
	    return 0;

	Element e0, e1, e2, e3, e4, e5, e6;
	CDATASection cd;

	if (withSchema) {
	    e1 = document.createElementNS(NS_KML, "SimpleField");
	    addAttribute(document, e1, "type", "string");
	    addAttribute(document, e1, "name", propi.name());
	    schema.appendChild(e1);

	    e2 = document.createElementNS(NS_KML, "displayName");
	    e1.appendChild(e2);
	    addAttribute(document, e1, "type", "string");

	    cd = cdataProperty(ci, propi);
	    if (cd != null)
		e2.appendChild(cd);
	}

	e1 = document.createElementNS(NS_XSL, "if");
	if (!withEmptyProps) {
	    schemaData.appendChild(e1);
	    addAttribute(document, e1, "test",
		    "(count(" + propi.qname() + ") - count(" + propi.qname() + "[@xsi:nil='true']))>0");
	}

	if (withSchema) {
	    e2 = document.createElementNS(NS_KML, "SimpleData");
	    if (!withEmptyProps) {
		e1.appendChild(e2);
	    } else {
		schemaData.appendChild(e2);
	    }
	    if (options.parameter(this.getClass().getName(), PARAM_PROP_NAME_CASE) != null && options
		    .parameter(this.getClass().getName(), PARAM_PROP_NAME_CASE).equalsIgnoreCase("capitalized")) {
		addAttribute(document, e2, "name",
			propi.name().substring(0, 1).toUpperCase().concat(propi.name().substring(1)));
	    } else {
		// standard: use property name as-is
		addAttribute(document, e2, "name", propi.name());
	    }

	} else {
	    e0 = document.createElementNS(NS_KML, "Data");
	    if (!withEmptyProps) {
		e1.appendChild(e0);
	    } else {
		schemaData.appendChild(e0);
	    }
	    String sn = stringProperty(propi);
	    if (options.parameter(this.getClass().getName(), PARAM_PROP_NAME_CASE) != null && options
		    .parameter(this.getClass().getName(), PARAM_PROP_NAME_CASE).equalsIgnoreCase("capitalized")) {
		addAttribute(document, e0, "name", sn.substring(0, 1).toUpperCase().concat(sn.substring(1)));
	    } else {
		// standard: use property name as-is
		addAttribute(document, e0, "name", sn);
	    }

	    e2 = document.createElementNS(NS_KML, "displayName");
	    e0.appendChild(e2);
	    cd = cdataProperty(ci, propi);
	    if (cd != null)
		e2.appendChild(cd);

	    e2 = document.createElementNS(NS_KML, "value");
	    e0.appendChild(e2);
	}

	e3 = document.createElementNS(NS_XSL, "for-each");
	e2.appendChild(e3);
	addAttribute(document, e3, "select", propi.qname());

	e4 = document.createElementNS(NS_XSL, "if");
	e3.appendChild(e4);
	addAttribute(document, e4, "test", "position()>1");

	cd = document.createCDATASection("<hr/>");
	e4.appendChild(cd);

	if (t.equals("CharacterString") || t.equals("Integer") || t.equals("Real") || t.equals("Number")
		|| t.equals("Character")) {
	    e4 = document.createElementNS(NS_XSL, "value-of");
	    e3.appendChild(e4);
	    addAttribute(document, e4, "select", ".");
	} else if (t.equals("Date")) {
	    e4 = document.createElementNS(NS_XSL, "value-of");
	    e3.appendChild(e4);
	    addAttribute(document, e4, "select", "format-date(., '[D]-[M]-[Y]', 'en', 'AD', 'UK')");
	} else if (t.equals("Time")) {
	    e4 = document.createElementNS(NS_XSL, "value-of");
	    e3.appendChild(e4);
	    addAttribute(document, e4, "select", "format-time(.,'[H01]:[m01]:[s01]', 'en', 'AD', 'UK')");
	} else if (t.equals("DateTime")) {
	    e4 = document.createElementNS(NS_XSL, "value-of");
	    e3.appendChild(e4);
	    addAttribute(document, e4, "select",
		    "format-dateTime(.,'[D]-[M]-[Y] at [H01]:[m01]:[s01]', 'en', 'AD', 'UK')");
	} else if (t.equals("Measure") || t.equals("Length") || t.equals("Distance") || t.equals("Area")
		|| t.equals("Volume")) {
	    e4 = document.createElementNS(NS_XSL, "value-of");
	    e3.appendChild(e4);
	    addAttribute(document, e4, "select", ".");
	    e4 = document.createElementNS(NS_XSL, "text");
	    e3.appendChild(e4);
	    e4.appendChild(document.createTextNode(" "));
	    e4 = document.createElementNS(NS_XSL, "value-of");
	    e3.appendChild(e4);
	    addAttribute(document, e4, "select", "@uom");
	} else if (cat == Options.CODELIST) {
	    e4 = document.createElementNS(NS_XSL, "choose");
	    e3.appendChild(e4);

	    e5 = document.createElementNS(NS_XSL, "when");
	    e4.appendChild(e5);
	    addAttribute(document, e5, "test", "@codeSpace");
	    cd = document.createCDATASection("<a href='");
	    e5.appendChild(cd);
	    e6 = document.createElementNS(NS_XSL, "value-of");
	    e5.appendChild(e6);
	    addAttribute(document, e6, "select", "@codeSpace");
	    cd = document.createCDATASection("'>");
	    e5.appendChild(cd);
	    e6 = document.createElementNS(NS_XSL, "value-of");
	    e5.appendChild(e6);
	    addAttribute(document, e6, "select", ".");
	    cd = document.createCDATASection("</a>");
	    e5.appendChild(cd);

	    e5 = document.createElementNS(NS_XSL, "otherwise");
	    e4.appendChild(e5);
	    e6 = document.createElementNS(NS_XSL, "value-of");
	    e5.appendChild(e6);
	    addAttribute(document, e6, "select", ".");
	} else if (cat == Options.ENUMERATION) {
	    e4 = document.createElementNS(NS_XSL, "value-of");
	    e3.appendChild(e4);
	    addAttribute(document, e4, "select", ".");
	} else if (cat == Options.FEATURE) {
	    e4 = document.createElementNS(NS_XSL, "choose");
	    e3.appendChild(e4);

	    if (options.parameter(this.getClass().getName(), PARAM_HREF_PROP_ONLY_ID) != null
		    && options.parameter(this.getClass().getName(), PARAM_HREF_PROP_ONLY_ID).equalsIgnoreCase("true")) {
		e5 = document.createElementNS(NS_XSL, "when");
		e4.appendChild(e5);
		addAttribute(document, e5, "test", "@xlink:href");
		e6 = document.createElementNS(NS_XSL, "value-of");
		e5.appendChild(e6);
		addAttribute(document, e6, "select", "reverse(tokenize(@xlink:href,':'))[1]");
	    } else {
		e5 = document.createElementNS(NS_XSL, "when");
		e4.appendChild(e5);
		addAttribute(document, e5, "test", "@xlink:href");
		cd = document.createCDATASection("<a href='");
		e5.appendChild(cd);
		e6 = document.createElementNS(NS_XSL, "value-of");
		e5.appendChild(e6);
		addAttribute(document, e6, "select", "@xlink:href");
		cd = document.createCDATASection("'>Placemark (");
		e5.appendChild(cd);
		e6 = document.createElementNS(NS_XSL, "value-of");
		e5.appendChild(e6);
		addAttribute(document, e6, "select", "@xlink:href");
		cd = document.createCDATASection(")</a>");
		e5.appendChild(cd);
	    }

	    e5 = document.createElementNS(NS_XSL, "otherwise");
	    e4.appendChild(e5);
	    cd = document.createCDATASection("Placemark");
	    e5.appendChild(cd);
	} else if (cat == Options.DATATYPE) {
	    e4 = document.createElementNS(NS_XSL, "call-template");
	    e3.appendChild(e4);
	    addAttribute(document, e4, "name", "DataType");
	} else {
	    e4 = document.createElementNS(NS_XSL, "value-of");
	    e3.appendChild(e4);
	    addAttribute(document, e4, "select", ".");
	    result.addError("Unsupported type in KML encoding rule: " + t);
	}

	return 1;
    }

    public void write() {
	if (printed) {
	    return;
	}

	Element e0, e1, e2, e3, e4, e5, e6, e7;

	e1 = document.createElementNS(NS_XSL, "template");
	addAttribute(document, e1, "name", "DataType");
	root.appendChild(e1);

	CDATASection cd;
	cd = document.createCDATASection("<table>");
	e1.appendChild(cd);
	e7 = document.createElementNS(NS_XSL, "for-each");
	e1.appendChild(e7);
	addAttribute(document, e7, "select", "*/*");
	e2 = document.createElementNS(NS_XSL, "if");
	e7.appendChild(e2);
	addAttribute(document, e2, "test", "count(@xsi:nil)=0 or @xsi:nil!='true'");
	cd = document.createCDATASection("<tr><td><small><i>");
	e2.appendChild(cd);
	e3 = document.createElementNS(NS_XSL, "value-of");
	e2.appendChild(e3);
	if (options.parameter(this.getClass().getName(), PARAM_PROP_NAME_CASE) != null
		&& options.parameter(this.getClass().getName(), PARAM_PROP_NAME_CASE).equalsIgnoreCase("capitalized")) {
	    addAttribute(document, e3, "select",
		    "concat(upper-case(substring(local-name(.),1,1)),substring(local-name(.),2))");
	} else {
	    addAttribute(document, e3, "select", "local-name(.)");
	}
	cd = document.createCDATASection("</i></small></td><td>");
	e2.appendChild(cd);

	e3 = document.createElementNS(NS_XSL, "choose");
	e2.appendChild(e3);
	e4 = document.createElementNS(NS_XSL, "when");
	e3.appendChild(e4);
	addAttribute(document, e4, "test", "*/*");
	e5 = document.createElementNS(NS_XSL, "call-template");
	e4.appendChild(e5);
	addAttribute(document, e5, "name", "DataType");

	e4 = document.createElementNS(NS_XSL, "otherwise");
	e3.appendChild(e4);
	e5 = document.createElementNS(NS_XSL, "value-of");
	e4.appendChild(e5);
	addAttribute(document, e5, "select", ".");
	cd = document.createCDATASection("</td></tr>");
	e2.appendChild(cd);
	cd = document.createCDATASection("</table>");
	e1.appendChild(cd);
	// TODO: nesting

	e0 = document.createElementNS(NS_XSL, "template");
	addAttribute(document, e0, "match", "gml:Polygon");
	root.appendChild(e0);

	e1 = document.createElementNS(NS_KML, "MultiGeometry");
	e0.appendChild(e1);

	e2 = document.createElementNS(NS_KML, "Point");
	e1.appendChild(e2);

	e3 = document.createElementNS(NS_KML, "coordinates");
	e2.appendChild(e3);

	e4 = document.createElementNS(NS_XSL, "variable");
	e3.appendChild(e4);
	addAttribute(document, e4, "name", "ord");
	addAttribute(document, e4, "select", "fn:tokenize(gml:exterior/gml:LinearRing/gml:posList, ' ', 'm')");

	e4 = document.createElementNS(NS_XSL, "variable");
	e3.appendChild(e4);
	addAttribute(document, e4, "name", "lat");
	addAttribute(document, e4, "as", "xs:double*");
	e5 = document.createElementNS(NS_XSL, "for-each");
	e4.appendChild(e5);
	addAttribute(document, e5, "select", "$ord");
	e6 = document.createElementNS(NS_XSL, "if");
	e5.appendChild(e6);
	addAttribute(document, e6, "test", "position() mod 2 = 1");
	e7 = document.createElementNS(NS_XSL, "sequence");
	e6.appendChild(e7);
	addAttribute(document, e7, "select", "xs:double(.)");

	e4 = document.createElementNS(NS_XSL, "variable");
	e3.appendChild(e4);
	addAttribute(document, e4, "name", "long");
	addAttribute(document, e4, "as", "xs:double*");
	e5 = document.createElementNS(NS_XSL, "for-each");
	e4.appendChild(e5);
	addAttribute(document, e5, "select", "$ord");
	e6 = document.createElementNS(NS_XSL, "if");
	e5.appendChild(e6);
	addAttribute(document, e6, "test", "position() mod 2 = 0");
	e7 = document.createElementNS(NS_XSL, "sequence");
	e6.appendChild(e7);
	addAttribute(document, e7, "select", "xs:double(.)");

	e4 = document.createElementNS(NS_XSL, "value-of");
	e3.appendChild(e4);
	addAttribute(document, e4, "select", "avg($long)");
	e4 = document.createElementNS(NS_XSL, "text");
	e3.appendChild(e4);
	e4.appendChild(document.createTextNode(","));
	e4 = document.createElementNS(NS_XSL, "value-of");
	e3.appendChild(e4);
	addAttribute(document, e4, "select", "avg($lat)");

	e2 = document.createElementNS(NS_KML, "Polygon");
	e1.appendChild(e2);

	e3 = document.createElementNS(NS_KML, "tessellate");
	e2.appendChild(e3);
	e3.appendChild(document.createTextNode("1"));

	e3 = document.createElementNS(NS_KML, "altitudeMode");
	e2.appendChild(e3);
	e3.appendChild(document.createTextNode("clampToGround"));

	e3 = document.createElementNS(NS_KML, "outerBoundaryIs");
	e2.appendChild(e3);

	e4 = document.createElementNS(NS_XSL, "apply-templates");
	e3.appendChild(e4);
	addAttribute(document, e4, "select", "gml:exterior/gml:LinearRing");

	e3 = document.createElementNS(NS_XSL, "for-each");
	e2.appendChild(e3);
	addAttribute(document, e3, "select", "gml:interior");

	e4 = document.createElementNS(NS_KML, "innerBoundaryIs");
	e3.appendChild(e4);

	e5 = document.createElementNS(NS_XSL, "apply-templates");
	e4.appendChild(e5);
	addAttribute(document, e5, "select", "gml:LinearRing");

	e1 = document.createElementNS(NS_XSL, "template");
	addAttribute(document, e1, "match", "gml:LinearRing");
	root.appendChild(e1);

	e2 = document.createElementNS(NS_KML, "LinearRing");
	e1.appendChild(e2);

	e3 = document.createElementNS(NS_XSL, "apply-templates");
	e2.appendChild(e3);
	addAttribute(document, e3, "select", "gml:posList");

	e0 = document.createElementNS(NS_XSL, "template");
	addAttribute(document, e0, "match", "gml:LineString");
	root.appendChild(e0);

	e1 = document.createElementNS(NS_KML, "MultiGeometry");
	e0.appendChild(e1);

	e2 = document.createElementNS(NS_KML, "Point");
	e1.appendChild(e2);

	e3 = document.createElementNS(NS_KML, "coordinates");
	e2.appendChild(e3);

	e4 = document.createElementNS(NS_XSL, "variable");
	e3.appendChild(e4);
	addAttribute(document, e4, "name", "ord");
	addAttribute(document, e4, "select", "fn:tokenize(gml:posList, ' ', 'm')");

	e4 = document.createElementNS(NS_XSL, "variable");
	e3.appendChild(e4);
	addAttribute(document, e4, "name", "lat");
	addAttribute(document, e4, "as", "xs:double*");
	e5 = document.createElementNS(NS_XSL, "for-each");
	e4.appendChild(e5);
	addAttribute(document, e5, "select", "$ord");
	e6 = document.createElementNS(NS_XSL, "if");
	e5.appendChild(e6);
	addAttribute(document, e6, "test", "position() mod 2 = 1");
	e7 = document.createElementNS(NS_XSL, "sequence");
	e6.appendChild(e7);
	addAttribute(document, e7, "select", "xs:double(.)");

	e4 = document.createElementNS(NS_XSL, "variable");
	e3.appendChild(e4);
	addAttribute(document, e4, "name", "long");
	addAttribute(document, e4, "as", "xs:double*");
	e5 = document.createElementNS(NS_XSL, "for-each");
	e4.appendChild(e5);
	addAttribute(document, e5, "select", "$ord");
	e6 = document.createElementNS(NS_XSL, "if");
	e5.appendChild(e6);
	addAttribute(document, e6, "test", "position() mod 2 = 0");
	e7 = document.createElementNS(NS_XSL, "sequence");
	e6.appendChild(e7);
	addAttribute(document, e7, "select", "xs:double(.)");

	e4 = document.createElementNS(NS_XSL, "value-of");
	e3.appendChild(e4);
	addAttribute(document, e4, "select", "avg($long)");
	e4 = document.createElementNS(NS_XSL, "text");
	e3.appendChild(e4);
	e4.appendChild(document.createTextNode(","));
	e4 = document.createElementNS(NS_XSL, "value-of");
	e3.appendChild(e4);
	addAttribute(document, e4, "select", "avg($lat)");

	e2 = document.createElementNS(NS_KML, "LineString");
	e1.appendChild(e2);

	e3 = document.createElementNS(NS_KML, "tessellate");
	e2.appendChild(e3);
	e3.appendChild(document.createTextNode("1"));

	e3 = document.createElementNS(NS_KML, "altitudeMode");
	e2.appendChild(e3);
	e3.appendChild(document.createTextNode("clampToGround"));

	e3 = document.createElementNS(NS_XSL, "apply-templates");
	e2.appendChild(e3);
	addAttribute(document, e3, "select", "gml:posList");

	e0 = document.createElementNS(NS_XSL, "template");
	addAttribute(document, e0, "match", "gml:Point");
	root.appendChild(e0);

	e1 = document.createElementNS(NS_KML, "Point");
	e0.appendChild(e1);

	e2 = document.createElementNS(NS_KML, "altitudeMode");
	e1.appendChild(e2);
	e2.appendChild(document.createTextNode("clampToGround"));

	e2 = document.createElementNS(NS_XSL, "apply-templates");
	e1.appendChild(e2);
	addAttribute(document, e2, "select", "gml:pos");

	e1 = document.createElementNS(NS_XSL, "template");
	addAttribute(document, e1, "match", "gml:posList|gml:pos");
	root.appendChild(e1);

	e2 = document.createElementNS(NS_KML, "coordinates");
	e1.appendChild(e2);

	e3 = document.createElementNS(NS_XSL, "variable");
	e2.appendChild(e3);
	addAttribute(document, e3, "name", "ord");
	addAttribute(document, e3, "select", "fn:tokenize(., ' ', 'm')");

	e3 = document.createElementNS(NS_XSL, "for-each");
	e2.appendChild(e3);
	addAttribute(document, e3, "select", "$ord");

	e4 = document.createElementNS(NS_XSL, "choose");
	e3.appendChild(e4);

	e5 = document.createElementNS(NS_XSL, "when");
	e4.appendChild(e5);
	addAttribute(document, e5, "test", "position() mod 2 = 1");
	e6 = document.createElementNS(NS_XSL, "variable");
	e5.appendChild(e6);
	addAttribute(document, e6, "name", "pos");
	addAttribute(document, e6, "select", "fn:number(position()+1)");
	e6 = document.createElementNS(NS_XSL, "value-of");
	e5.appendChild(e6);
	addAttribute(document, e6, "select", "$ord[$pos]");
	e6 = document.createElementNS(NS_XSL, "text");
	e5.appendChild(e6);
	e6.appendChild(document.createTextNode(","));
	e6 = document.createElementNS(NS_XSL, "value-of");
	e5.appendChild(e6);
	addAttribute(document, e6, "select", ".");

	e5 = document.createElementNS(NS_XSL, "otherwise");
	e4.appendChild(e5);
	e6 = document.createElementNS(NS_XSL, "if");
	e5.appendChild(e6);
	addAttribute(document, e6, "test", "not(position()=last())");
	e7 = document.createElementNS(NS_XSL, "text");
	e6.appendChild(e7);
	e7.appendChild(document.createTextNode(" "));

	for (PackageInfo pix : folderMapElement.keySet()) {
	    e1 = folderMapElement.get(pix);
	    String s = folderMapFeatureTypes.get(pix);
	    if (e1 != null && s != null) {
		e1.appendChild(kmlDocument.createTextNode(
			"CLIENTINFO=[clientVersion];[kmlVersion];[clientName];[language]&TYPENAME=" + s));
	    }
	}

	try {
	    File file = new File(outputDirectory + "/" + pi.name() + "_gml2kml.xsl");
	    XMLUtil.writeXml(document, file);
	    result.addResult(getTargetName(), outputDirectory, pi.name() + "_gml2kml.xsl", pi.targetNamespace());

	    file = new File(outputDirectory + "/" + pi.name() + "_root.kml");
	    XMLUtil.writeXml(document, file);	    
	    result.addResult(getTargetName(), outputDirectory, pi.name() + "_root.kml", pi.targetNamespace());
	} catch (ShapeChangeException e) {
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
	// no rules or requirements defined for this target, thus nothing to do
    }

    @Override
    public String getTargetName() {
	return "KML XSLT";
    }

    @Override
    public String getTargetIdentifier() {
	return "kml";
    }

    @Override
    public String getDefaultEncodingRule() {
	return "*";
    }
}
