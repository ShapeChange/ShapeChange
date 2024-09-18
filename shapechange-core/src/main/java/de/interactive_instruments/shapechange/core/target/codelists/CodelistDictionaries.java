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

package de.interactive_instruments.shapechange.core.target.codelists;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ProcessingInstruction;

import de.interactive_instruments.shapechange.core.MessageSource;
import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.RuleRegistry;
import de.interactive_instruments.shapechange.core.ShapeChangeAbortException;
import de.interactive_instruments.shapechange.core.ShapeChangeException;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.model.ClassInfo;
import de.interactive_instruments.shapechange.core.model.Info;
import de.interactive_instruments.shapechange.core.model.Model;
import de.interactive_instruments.shapechange.core.model.PackageInfo;
import de.interactive_instruments.shapechange.core.model.PropertyInfo;
import de.interactive_instruments.shapechange.core.target.Target;
import de.interactive_instruments.shapechange.core.util.XMLUtil;

/**
 * @author Stefan Olk
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class CodelistDictionaries implements Target, MessageSource {

    public static final String PARAM_IDENTIFIER = "identifier";
    public static final String PARAM_NAMES = "names";
    public static final String PARAM_GMLID = "gmlid";
    public static final String PARAM_ENUMERATIONS = "enumerations";
    public static final String PARAM_CODELISTS = "codelists";
    public static final String PARAM_FIXED_IDENTIFIER_CODESPACE = "fixedIdentifierCodeSpace";
    public static final String PARAM_NAME_SOURCES_TO_ADD_AS_CODESPACE = "nameSourcesToAddAsCodeSpace";
    public static final String PARAM_DEFINITION_GMLID_TEMPLATE = "definitionGmlIdTemplate";
    public static final String PARAM_DEFINITION_GMLIDENTIFIER_TEMPLATE = "definitionGmlIdentifierTemplate";
    public static final String PARAM_ADD_STYLESHEET_PROCESSING_INSTRUCTION = "addStylesheetProcessingInstruction";

    private PackageInfo pi = null;
    private Model model = null;
    private Options options = null;
    private ShapeChangeResult result = null;

    private boolean diagnosticsOnly = false;
    private boolean printed = false;

    private boolean enums = false;
    private boolean codelists = true;
    private boolean addStylesheetProcessingInstruction = true;

    private String definitionGmlIdTemplate = null;
    private String definitionGmlIdentifierTemplate = null;
    private String fixedIdentifierCodeSpace = null;
    private String gmlid = "id";
    private List<String> identifiers = null;
    private List<String> names = null;
    private Set<String> nameSourcesToAddAsCodeSpace = null;

    private final HashMap<String, Document> documentMap = new HashMap<String, Document>();

    private String documentationTemplate = null;
    private String documentationNoValue = null;

    public void initialise(PackageInfo p, Model m, Options o, ShapeChangeResult r, boolean diagOnly)
	    throws ShapeChangeAbortException {

	pi = p;
	model = m;
	options = o;
	result = r;
	diagnosticsOnly = diagOnly;

	enums = options.parameterAsBoolean(this.getClass().getName(), PARAM_ENUMERATIONS, false);
	codelists = options.parameterAsBoolean(this.getClass().getName(), PARAM_CODELISTS, true);
	addStylesheetProcessingInstruction = options.parameterAsBoolean(this.getClass().getName(),
		PARAM_ADD_STYLESHEET_PROCESSING_INSTRUCTION, true);

	identifiers = options.parameterAsStringList(this.getClass().getName(), PARAM_IDENTIFIER,
		new String[] { "name" }, true, true);
	names = options.parameterAsStringList(this.getClass().getName(), PARAM_NAMES,
		new String[] { "alias", "initialValue" }, true, true);
	nameSourcesToAddAsCodeSpace = new HashSet<>(options.parameterAsStringList(this.getClass().getName(),
		PARAM_NAME_SOURCES_TO_ADD_AS_CODESPACE, null, true, true));

	gmlid = options.parameterAsString(this.getClass().getName(), PARAM_GMLID, "id", false, true);
	fixedIdentifierCodeSpace = options.parameterAsString(this.getClass().getName(),
		PARAM_FIXED_IDENTIFIER_CODESPACE, null, false, true);

	documentationTemplate = options.parameter(this.getClass().getName(), "documentationTemplate");
	documentationNoValue = options.parameter(this.getClass().getName(), "documentationNoValue");

	definitionGmlIdTemplate = options.parameterAsString(this.getClass().getName(), PARAM_DEFINITION_GMLID_TEMPLATE,
		null, false, true);
	definitionGmlIdentifierTemplate = options.parameterAsString(this.getClass().getName(),
		PARAM_DEFINITION_GMLIDENTIFIER_TEMPLATE, null, false, true);
    }

    public void process(ClassInfo ci) {

	int cat = ci.category();
	if (cat != Options.CODELIST && (!enums || cat != Options.ENUMERATION)) {
	    return;
	} else if (cat == Options.CODELIST && (!codelists || ci.asDictionary() == false)) {
	    result.addInfo(this, 101, ci.name());
	    return;
	}

	try {

	    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	    DocumentBuilder db = dbf.newDocumentBuilder();
	    Document cDocument = db.newDocument();

	    if (addStylesheetProcessingInstruction) {
		ProcessingInstruction proci = null;
		if (options.gmlVersion.equals("3.2")) {
		    proci = cDocument.createProcessingInstruction("xml-stylesheet",
			    "type='text/xsl' href='./CodelistDictionary-v32.xsl'");
		} else if (options.gmlVersion.equals("3.1")) {
		    proci = cDocument.createProcessingInstruction("xml-stylesheet",
			    "type='text/xsl' href='./CodelistDictionary-v31.xsl'");
		}
		if (proci != null) {
		    cDocument.appendChild(proci);
		}
	    }

	    Element ec = cDocument.createElementNS(options.GML_NS, "Dictionary");
	    cDocument.appendChild(ec);

	    XMLUtil.addAttribute(cDocument, ec, "xmlns", options.GML_NS);
	    XMLUtil.addAttribute(cDocument, ec, "xmlns:gml", options.GML_NS);
	    XMLUtil.addAttribute(cDocument, ec, "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
	    XMLUtil.addAttribute(cDocument, ec, "xsi:schemaLocation",
		    options.GML_NS + " " + options.schemaLocationOfNamespace(options.GML_NS));
	    XMLUtil.addAttribute(cDocument, ec, "gml:id", ci.name());

	    documentMap.put(ci.id(), cDocument);

	    String documentation = ci.derivedDocumentation(documentationTemplate, documentationNoValue);
	    if (StringUtils.isNotBlank(documentation)) {
		Element e1 = cDocument.createElementNS(options.GML_NS, "description");
		e1.appendChild(cDocument.createTextNode(documentation));
		ec.appendChild(e1);
	    }

	    for (String identifierSource : identifiers) {

		String identifierValue = getValue(ci, identifierSource);

		if (StringUtils.isNotBlank(identifierValue)) {

		    if (options.gmlVersion.equals("3.2")) {

			Element e1 = cDocument.createElementNS(options.GML_NS, "identifier");
			String codeSpace = fixedIdentifierCodeSpace;
			if (StringUtils.isBlank(codeSpace)) {
			    codeSpace = ci.pkg().targetNamespace();
			}
			XMLUtil.addAttribute(cDocument, e1, "codeSpace", codeSpace);
			e1.appendChild(cDocument.createTextNode(identifierValue));
			ec.appendChild(e1);

		    } else if (options.gmlVersion.equals("3.1")) {

			Element e1 = cDocument.createElementNS(options.GML_NS, "name");
			e1.appendChild(cDocument.createTextNode(identifierValue));
			ec.appendChild(e1);
		    }

		    break;
		}
	    }

	    for (String nameSource : names) {

		String nameValue = getValue(ci, nameSource);

		if (StringUtils.isNotBlank(nameValue)) {

		    Element e1 = cDocument.createElementNS(options.GML_NS, "name");
		    e1.appendChild(cDocument.createTextNode(nameValue));
		    ec.appendChild(e1);

		    if (nameSourcesToAddAsCodeSpace.contains(nameSource)) {
			XMLUtil.addAttribute(cDocument, e1, "codeSpace", StringUtils.strip(nameSource, "@"));
		    }
		}
	    }

	    /*
	     * Add properties from ci as well as all its supertypes in the complete
	     * supertype hierarchy.
	     */

	    SortedMap<String, Element> entryElmtsByGmlId = new TreeMap<>();

	    for (PropertyInfo propi : ci.propertiesAll()) {

		Element entryElmt = createEntry(cDocument, ci, propi, true);

		if (entryElmt != null) {
		    String gmlid = ((Element) entryElmt.getElementsByTagName("Definition").item(0))
			    .getAttribute("gml:id");
		    entryElmtsByGmlId.put(gmlid, entryElmt);
		}
	    }

	    for (Element defElmt : entryElmtsByGmlId.values()) {
		ec.appendChild(defElmt);
	    }

	} catch (ParserConfigurationException e) {

	    result.addFatalError(null, 2);
	    String m = e.getMessage();
	    if (m != null) {
		result.addFatalError(m);
	    }
	    e.printStackTrace(System.err);

	} catch (Exception e) {

	    String m = e.getMessage();
	    if (m != null) {
		result.addFatalError(m);
	    }
	    e.printStackTrace(System.err);
	}

    }

    private Element createEntry(Document lDocument, ClassInfo ci, PropertyInfo propi, boolean local) {

	Element e = lDocument.createElementNS(options.GML_NS, "dictionaryEntry");
	Element e3 = lDocument.createElementNS(options.GML_NS, "Definition");

	String gmlIdValue = applyDefinitionIdsTemplate(propi, definitionGmlIdTemplate, getValue(propi, gmlid), "FIXME");

	if (StringUtils.isNotBlank(gmlIdValue)) {
	    if (!gmlIdValue.matches("^[a-zA-Z_].*")) {
		gmlIdValue = "_" + gmlIdValue;
	    }
	} else {
	    gmlIdValue = "_" + propi.id();
	}

	XMLUtil.addAttribute(lDocument, e3, "gml:id", gmlIdValue);
	e.appendChild(e3);

	Element e2;
	String defDescription = propi.derivedDocumentation(documentationTemplate, documentationNoValue);
	if (StringUtils.isNotBlank(defDescription)) {
	    e2 = lDocument.createElementNS(options.GML_NS, "description");
	    e2.appendChild(lDocument.createTextNode(defDescription));
	    e3.appendChild(e2);
	}

	if (options.gmlVersion.equals("3.2")) {
	    e2 = lDocument.createElementNS(options.GML_NS, "identifier");
	} else {
	    e2 = lDocument.createElementNS(options.GML_NS, "name");
	}

	String codeSpace = fixedIdentifierCodeSpace;
	if (StringUtils.isBlank(codeSpace))
	    codeSpace = ci.taggedValue("codeList");
	if (StringUtils.isBlank(codeSpace))
	    codeSpace = ci.taggedValue("infoURL");
	if (StringUtils.isBlank(codeSpace))
	    codeSpace = ci.pkg().targetNamespace() + "/" + ci.name();

	XMLUtil.addAttribute(lDocument, e2, "codeSpace", codeSpace);

	String defaultIdentifierValue = "FIXME";
	for (String identifierSource : identifiers) {
	    String identifierValue = getValue(propi, identifierSource);
	    if (StringUtils.isNotBlank(identifierValue)) {
		defaultIdentifierValue = identifierValue;
		break;
	    }
	}

	String gmlIdentifierValue = applyDefinitionIdsTemplate(propi, definitionGmlIdentifierTemplate,
		defaultIdentifierValue, "FIXME");

	e2.appendChild(lDocument.createTextNode(gmlIdentifierValue));
	e3.appendChild(e2);

	for (String nameSource : names) {

	    String nameValue = getValue(propi, nameSource);

	    if (StringUtils.isNotBlank(nameValue)) {
		Element e1 = lDocument.createElementNS(options.GML_NS, "name");
		e1.appendChild(lDocument.createTextNode(nameValue));
		e3.appendChild(e1);

		if (nameSourcesToAddAsCodeSpace.contains(nameSource)) {
		    XMLUtil.addAttribute(lDocument, e1, "codeSpace", StringUtils.strip(nameSource, "@"));
		}
	    }
	}

	return e;
    }

    private String applyDefinitionIdsTemplate(PropertyInfo propi, String templateIn, String defaultValue,
	    String noValueValue) {

	String result = null;

	if (StringUtils.isNotBlank(templateIn)) {

	    String template = templateIn;
	    template = template.replaceAll("\\[\\[initialValue\\]\\]",
		    StringUtils.defaultIfBlank(StringUtils.stripToNull(propi.initialValue()), noValueValue));
	    template = template.replaceAll("\\[\\[className\\]\\]",
		    StringUtils.defaultIfBlank(propi.inClass().name(), noValueValue));

	    result = propi.derivedDocumentation(template, noValueValue);

	}

	if (StringUtils.isBlank(result)) {
	    result = defaultValue;
	}

	return result;
    }

    /**
     * @param i      the model element from which to retrieve a specific value
     * @param source one of: name, alias, id, initialValue, @{tagged value name}
     * @return the value of the source; can be blank (<code>null</code> or empty)
     */
    private String getValue(Info i, String source) {

	String s = null;

	if (source.equalsIgnoreCase("name")) {
	    s = i.name();
	} else if (source.equalsIgnoreCase("alias")) {
	    s = i.aliasName();
	} else if (source.equalsIgnoreCase("id")) {
	    s = i.id();
	} else if (source.equalsIgnoreCase("initialValue") && i instanceof PropertyInfo) {
	    s = ((PropertyInfo) i).initialValue();
	} else if (source.startsWith("@")) {
	    s = i.taggedValue(source.substring(1));
	}

	return s;
    }

    public void write() {
	if (printed) {
	    return;
	}
	if (diagnosticsOnly) {
	    return;
	}

	try {
	    for (Iterator<ClassInfo> i = model.classes(pi).iterator(); i.hasNext();) {
		ClassInfo ci = i.next();
		Document cDocument = documentMap.get(ci.id());
		if (cDocument != null) {
		    String dir = options.parameter(this.getClass().getName(), "outputDirectory");
		    if (dir == null)
			dir = options.parameter("outputDirectory");
		    if (dir == null)
			dir = options.parameter(".");

		    File outDir = new File(dir);
		    if (!outDir.exists())
			outDir.mkdirs();

		    XMLUtil.writeXml(cDocument, new File(dir, ci.name() + ".xml"));
		    result.addResult(getTargetName(), dir, ci.name() + ".xml", ci.qname());
		}
	    }
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
	return "Code List Dictionary";
    }

    @Override
    public String getTargetIdentifier() {
	return "cld";
    }

    @Override
    public String getDefaultEncodingRule() {
	return "*";
    }

    @Override
    public String message(int mnr) {

	switch (mnr) {

	case 101:
	    return "Code list '$1$' is not configured to be encoded as a dictionary. Either parameter '"
		    + PARAM_CODELISTS
		    + "' is set to false, or tagged value asDictionary on the code list is false. It will be ignored.";

	default:
	    return "(" + CodelistDictionaries.class.getName() + ") Unknown message with number: " + mnr;
	}
    }
}
