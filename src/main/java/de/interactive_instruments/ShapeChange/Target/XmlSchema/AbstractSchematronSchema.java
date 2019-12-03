/**
 * ShapeChange - processing application schemas for geographic information
 *
 * <p>This file is part of ShapeChange. ShapeChange takes a ISO 19109 Application Schema from a UML
 * model and translates it into a GML Application Schema or other implementation representations.
 *
 * <p>Additional information about the software can be found at http://shapechange.net/
 *
 * <p>(c) 2002-2019 interactive instruments GmbH, Bonn, Germany
 *
 * <p>This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * <p>You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 *
 * <p>Contact: interactive instruments GmbH Trierer Strasse 70-72 53115 Bonn Germany
 */
package de.interactive_instruments.ShapeChange.Target.XmlSchema;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.SerializerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import de.interactive_instruments.ShapeChange.MapEntry;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.TargetXmlSchemaConfiguration;
import de.interactive_instruments.ShapeChange.XsdPropertyMapEntry;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;

/**
 * Common class for Schematron Schema creation.
 * 
 * @author Reinhard Erstling
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments <dot>
 *         de)
 *
 */
public abstract class AbstractSchematronSchema implements SchematronSchema, MessageSource {

    public static class RuleCreationStatus {
	public XpathFragment lastPathStatus;
	public Element ruleElement;
	public Element firstAssertElement = null;
	HashSet<String> letVarsAlreadyOutput = new HashSet<String>();
    }

    protected boolean addIdKey = false;

    /** key: rule context, value: rule creation status */
    HashMap<String, RuleCreationStatus> ruleCreationStatusMap = new HashMap<String, RuleCreationStatus>();

    Model model;
    Options options;
    TargetXmlSchemaConfiguration config;
    ShapeChangeResult result;
    PackageInfo pi;
    Document document;
    Element pattern;
    Element root;
    Text schematronTitleHook;
    boolean schematronTitleExtended = false;

    boolean printed = false;
    boolean assertion = false;

    String currentOclConstraintName = null;
    ClassInfo currentOclConstraintClass = null;

    HashSet<String> namespaces = new HashSet<String>();

    // Prefix and postfix for xlink:href references
    String alpha = "#";
    String beta = "";

    // Options for the interpretation of suppressed types
    boolean trojanSuppressedType = false;

    String classname = null;

    public static class ExtensionFunctionTemplate {
	public String nsPrefix;
	public String namespace;
	public String function;

	public ExtensionFunctionTemplate(String nsp, String ns, String fct) {
	    nsPrefix = nsp;
	    namespace = ns;
	    function = fct;
	}
    }

    HashMap<String, ExtensionFunctionTemplate> extensionFunctions = new HashMap<String, ExtensionFunctionTemplate>();

    /**
     * Ctor
     *
     * @param mdl Model object
     * @param o   Options object
     * @param r   Result object
     * @param p   PackageInfo object
     */
    public AbstractSchematronSchema(Model mdl, Options o, ShapeChangeResult r, PackageInfo p) {

	model = mdl;
	pi = p;
	options = o;
	result = r;
	document = null;
	classname = XmlSchema.class.getName();

	config = (TargetXmlSchemaConfiguration) options.getCurrentProcessConfig();

	// Get prefix and postfix of xlink:href references
	String s = options.parameter(classname, "schematronXlinkHrefPrefix");
	if (s != null)
	    alpha = s;
	s = options.parameter(classname, "schematronXlinkHrefPostfix");
	if (s != null)
	    beta = s;

	// Get option value for interpretation of "suppressed types"
	s = options.parameter(classname, "suppressedTypeInterpretation");
	if (s != null) {
	    if (s.equals("strictUML"))
		trojanSuppressedType = false;
	    else if (s.equals("trojanType"))
		trojanSuppressedType = true;
	    else
		result.addError(null, 140, "suppressedTypeInterpretation", s);
	}

	// Read extension function templates
	String pats = "^schematronExtension\\.(\\w+?)\\.function";
	String[] extdecls = options.parameterNamesByRegex(classname, pats);
	Pattern pat = Pattern.compile(pats);
	for (String ext : extdecls) {
	    // Pick the function name from the parameter key
	    Matcher mat = pat.matcher(ext);
	    mat.matches();
	    String fctname = mat.group(1);
	    // Obtain the function pattern
	    String fcts = options.parameter(classname, ext);
	    // Obtain associated namespace parameter or a default
	    String nss = options.parameter(classname, "schematronExtension." + fctname + ".namespace");
	    if (nss == null || nss.length() == 0) {
		nss = "java:java";
	    }
	    // Split namespace and prefix
	    int col = nss.indexOf(":");
	    String nspx = "java";
	    String ns = nss;
	    if (col >= 0) {
		nspx = nss.substring(0, col);
		ns = nss.substring(col + 1);
	    }
	    // Record an extension template
	    extensionFunctions.put(fctname, new ExtensionFunctionTemplate(nspx, ns, fcts));
	}

	// Create the Schematron document
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

	// Create a root and attach the Schematron and application schema
	// namespace definitions.
	root = document.createElementNS(Options.SCHEMATRON_NS, "schema");
	document.appendChild(root);
	addAttribute(document, root, "xmlns:sch", Options.SCHEMATRON_NS);

	// Add a title element to document the schema the rules belong to
	Element e1 = document.createElementNS(Options.SCHEMATRON_NS, "title");
	root.appendChild(e1);
	schematronTitleHook = document.createTextNode("Schematron constraints for schema '" + pi.name() + "'");
	e1.appendChild(schematronTitleHook);

	// Add a namespace declaration for Schematron
	e1 = document.createElementNS(Options.SCHEMATRON_NS, "ns");
	root.appendChild(e1);
	namespaces.add("sch");
	addAttribute(document, e1, "prefix", "sch");
	addAttribute(document, e1, "uri", Options.SCHEMATRON_NS);

	// Add a namespace declaration for the package
	e1 = document.createElementNS(Options.SCHEMATRON_NS, "ns");
	root.appendChild(e1);
	namespaces.add(pi.xmlns());
	addAttribute(document, e1, "prefix", pi.xmlns());
	addAttribute(document, e1, "uri", pi.targetNamespace());

	// Finally add the <pattern> element. It is to hold all the rules
	// to be generated
	pattern = document.createElementNS(Options.SCHEMATRON_NS, "pattern");
	root.appendChild(pattern);
    }

    /** Add attribute to an element */
    protected void addAttribute(Document document, Element e, String name, String value) {
	Attr att = document.createAttribute(name);
	att.setValue(value);
	e.setAttributeNode(att);
    }
    
    @Override
    public void addAssertion(ClassInfo ci, XpathFragment xpath, String text) {
	addAssertion(ci,false,xpath,text);
    }

    @Override
    public void addAssertion(ClassInfo ci, boolean addToSubtypesInSelectedSchemas, XpathFragment xpath, String text) {

	// Drop abstract classes
	if (ci.isAbstract())
	    return;

	/*
	 * Create an assertion. Find out if a rule for the required context already
	 * exists.
	 */
	String ruleContext = ci.qname();
	RuleCreationStatus rulecs = ruleCreationStatusMap.get(ruleContext);
	String asserttext;
	if (rulecs == null) {
	    // First time we encounter this feature type: Create a <rule>
	    Element rule = document.createElementNS(Options.SCHEMATRON_NS, "rule");
	    pattern.appendChild(rule);
	    addAttribute(document, rule, "context", ruleContext);
	    // Initialize the necessary DOM hooks and info
	    rulecs = new RuleCreationStatus();
	    rulecs.ruleElement = rule;
	    // Initialize accumulation of the result fragments
	    rulecs.lastPathStatus = xpath;
	    asserttext = xpath.fragment;
	    // Store away
	    ruleCreationStatusMap.put(ruleContext, rulecs);
	} else {
	    // Second time: We need to merge the result fragment
	    asserttext = rulecs.lastPathStatus.merge(xpath);
	}

	// Add the let-assignments, which are new for this assert
	if (xpath.lets != null)
	    for (Entry<String, String> l : rulecs.lastPathStatus.lets.entrySet()) {
		if (rulecs.letVarsAlreadyOutput.contains(l.getKey()))
		    continue;
		Element let = document.createElementNS(Options.SCHEMATRON_NS, "let");
		rulecs.ruleElement.insertBefore(let, rulecs.firstAssertElement);
		addAttribute(document, let, "name", l.getKey());
		addAttribute(document, let, "value", l.getValue());
		rulecs.letVarsAlreadyOutput.add(l.getKey());
	    }

	// Add the assertion
	Element ass = document.createElementNS(Options.SCHEMATRON_NS, "assert");
	rulecs.ruleElement.appendChild(ass);
	if (rulecs.firstAssertElement == null)
	    rulecs.firstAssertElement = ass;
	addAttribute(document, ass, "test", asserttext);
	ass.appendChild(document.createTextNode(text));

	// Memorize we have output at least one rule
	assertion = true;
	
	if (addToSubtypesInSelectedSchemas) {

	    for (String subtypeId : ci.subtypes()) {

		ClassInfo subtype = model.classById(subtypeId);

		if (model.isInSelectedSchemas(subtype)) {

		    addAssertion(subtype, true, xpath, text);

		    if (!schematronTitleExtended
			    && !subtype.pkg().targetNamespace().equalsIgnoreCase(ci.pkg().targetNamespace())) {
			schematronTitleHook
				.setTextContent(schematronTitleHook.getTextContent() + " and dependent schema(s)");
			schematronTitleExtended = true;
		    }
		}
	    }
	}
    }

    @Override
    public void addAssertionForExplicitProperty(ClassInfo cib, PropertyInfo pi, boolean addToSubtypesInSelectedSchemas,
	    XpathFragment xpath, String text) {

	ClassInfo ci = cib != null ? cib : pi.inClass();

	/*
	 * Do not add assertion for abstract and suppressed classes.
	 *
	 * Also, if the class or the property is prohibited in the profile schema, do
	 * not create any checks for the property and this class.
	 */
	if (!ci.isAbstract() && !ci.suppressed()
		&& !(ci.matches("rule-xsd-all-propertyAssertion-ignoreProhibited")
			&& "true".equalsIgnoreCase(ci.taggedValue("prohibitedInProfile")))
		&& !(pi.matches("rule-xsd-all-propertyAssertion-ignoreProhibited")
			&& "true".equalsIgnoreCase(pi.taggedValue("prohibitedInProfile")))) {

	    registerNamespace(ci);
	    registerNamespace(pi.inClass());

	    /*
	     * Create an assertion. Find out if a rule for the required context already
	     * exists.
	     *
	     * TBD: add nil check to context?
	     */
	    String ruleContext = ci.qname() + "/" + getAndRegisterXmlName(pi);
	    RuleCreationStatus rulecs = ruleCreationStatusMap.get(ruleContext);
	    String asserttext;

	    if (rulecs == null) {

		// First time we encounter this context: Create a <rule>
		Element rule = document.createElementNS(Options.SCHEMATRON_NS, "rule");
		pattern.appendChild(rule);
		addAttribute(document, rule, "context", ruleContext);

		// Initialize the necessary DOM hooks and info
		rulecs = new RuleCreationStatus();
		rulecs.ruleElement = rule;

		// Initialize accumulation of the result fragments
		rulecs.lastPathStatus = xpath;
		asserttext = xpath.fragment;

		// Store away
		ruleCreationStatusMap.put(ruleContext, rulecs);

	    } else {

		// Second time: We need to merge the result fragment
		asserttext = rulecs.lastPathStatus.merge(xpath);
	    }

	    // Add the let-assignments, which are new for this assert
	    if (xpath.lets != null) {

		for (Entry<String, String> l : rulecs.lastPathStatus.lets.entrySet()) {

		    if (rulecs.letVarsAlreadyOutput.contains(l.getKey())) {
			continue;
		    }

		    Element let = document.createElementNS(Options.SCHEMATRON_NS, "let");
		    rulecs.ruleElement.insertBefore(let, rulecs.firstAssertElement);
		    addAttribute(document, let, "name", l.getKey());
		    addAttribute(document, let, "value", l.getValue());
		    rulecs.letVarsAlreadyOutput.add(l.getKey());
		}
	    }

	    // Add the assertion
	    Element ass = document.createElementNS(Options.SCHEMATRON_NS, "assert");
	    rulecs.ruleElement.appendChild(ass);
	    if (rulecs.firstAssertElement == null) {
		rulecs.firstAssertElement = ass;
	    }
	    addAttribute(document, ass, "test", asserttext);
	    ass.appendChild(document.createTextNode(text));

	    // Memorize we have output at least one rule
	    assertion = true;
	}

	if (addToSubtypesInSelectedSchemas) {

	    for (String subtypeId : ci.subtypes()) {

		ClassInfo subtype = model.classById(subtypeId);

		if (model.isInSelectedSchemas(subtype)) {

		    addAssertionForExplicitProperty(subtype, pi, true, xpath, text);

		    if (!schematronTitleExtended
			    && !subtype.pkg().targetNamespace().equalsIgnoreCase(ci.pkg().targetNamespace())) {
			schematronTitleHook
				.setTextContent(schematronTitleHook.getTextContent() + " and dependent schema(s)");
			schematronTitleExtended = true;
		    }
		}
	    }
	}
    }

    @Override
    public String getAndRegisterXmlName(PropertyInfo pi) {

	XsdPropertyMapEntry xpme = config.getPropertyMapEntry(pi);

	if (xpme != null && xpme.hasTargetElement()) {

	    String qname = xpme.getTargetElement();
	    String nspref = qname.split(":")[0];
	    registerNamespace(nspref);
	    return qname;

	} else {

	    String nspref = pi.inClass().pkg().xmlns();
	    String proper = nspref + ":" + pi.name();
	    registerNamespace(nspref, pi.inClass());
	    return proper;
	}
    }

    @Override
    public String getAndRegisterXmlName(ClassInfo ci) {
	String nspref = null;
	String fulnam = null;
	MapEntry me = ci.options().elementMapEntry(ci.name(), ci.encodingRule("xsd"));
	if (me != null) {
	    if (me.p1 == null || me.p1.length() == 0)
		return null;
	    fulnam = me.p1;
	    String[] parts = fulnam.split(":");
	    if (parts.length > 1)
		nspref = parts[0];
	    registerNamespace(nspref);
	} else {
	    nspref = ci.pkg().xmlns();
	    fulnam = nspref + ":" + ci.name();
	    registerNamespace(nspref, ci);
	}
	return fulnam;
    }

    @Override
    public void registerNamespace(String xmlns, String ns) {
	if (!namespaces.contains(xmlns)) {
	    Element e = document.createElementNS(Options.SCHEMATRON_NS, "ns");
	    addAttribute(document, e, "prefix", xmlns);
	    if (ns == null)
		ns = "FIXME";
	    addAttribute(document, e, "uri", ns);
	    root.insertBefore(e, pattern);
	    namespaces.add(xmlns);
	    if (ns.equals("http://www.w3.org/2005/xpath-functions")) {
		setQueryBinding("xslt2");
	    }
	}
    }

    @Override
    public void registerNamespace(String xmlns) {
	if (!namespaces.contains(xmlns)) {
	    String ns = options.fullNamespace(xmlns);
	    registerNamespace(xmlns, ns);
	}
    }

    @Override
    public void registerNamespace(String xmlns, ClassInfo ci) {
	if (!namespaces.contains(xmlns)) {
	    String ns = ci.pkg().targetNamespace();
	    if (ns == null || ns.length() == 0)
		registerNamespace(xmlns);
	    else {
		registerNamespace(xmlns, ns);
	    }
	}
    }

    @Override
    public void registerNamespace(ClassInfo ci) {
	String xmlns = ci.pkg().xmlns();
	if (!namespaces.contains(xmlns)) {
	    String ns = ci.pkg().targetNamespace();
	    if (ns == null || ns.length() == 0)
		registerNamespace(xmlns);
	    else {
		registerNamespace(xmlns, ns);
	    }
	}
    }

    @Override
    public String determineCodeListValuePattern(ClassInfo codelist, String defaultPattern) {

	String vp = codelist.taggedValue("codeListValuePattern");

	if (StringUtils.isBlank(vp)) {

	    vp = options.parameterAsString(XmlSchema.class.getName(), "defaultCodeListValuePattern", defaultPattern,
		    false, true);
	}

	return vp;
    }

    public void addIdKey() {
	addIdKey = true;
    }

    /**
     * This function serializes the generated Schematron schema to the given
     * directory. The document name is derived from the name of the GML application
     * schema. Serialization takes place only if at least one rule has been
     * generated.
     *
     * @param outputDirectory
     */
    public void write(String outputDirectory) {

	if (printed || !assertion) {
	    return;
	}

	if (addIdKey) {

	    addAttribute(document, root, "xmlns:xsl", "http://www.w3.org/1999/XSL/Transform");

	    Element e = document.createElementNS("http://www.w3.org/1999/XSL/Transform", "xsl:key");
	    addAttribute(document, e, "name", "idKey");
	    addAttribute(document, e, "match", "*[@*:id]");
	    addAttribute(document, e, "use", "@*:id");
	    root.insertBefore(e, pattern);
	}

	// identify file name
	String schematronFilename = options.parameterAsString(XmlSchema.class.getName(), "schematronFileNameTemplate",
		"[[SCHEMA_XSD_BASENAME]].xsd_SchematronSchema.xml", false, true);
	String schemaXsdDocument = pi.xsdDocument();
	String schemaXsdBaseName = FilenameUtils.getBaseName(schemaXsdDocument);
	schematronFilename = schematronFilename.replaceAll("\\[\\[SCHEMA_XSD_BASENAME\\]\\]", schemaXsdBaseName);

	// Choose serialization parameters
	Properties outputFormat = OutputPropertiesFactory.getDefaultMethodProperties("xml");
	outputFormat.setProperty("indent", "yes");
	outputFormat.setProperty("{http://xml.apache.org/xalan}indent-amount", "2");
	// outputFormat.setProperty("encoding", model.characterEncoding());
	outputFormat.setProperty("encoding", "UTF-8");

	// Do the actual writing
	try {
	    OutputStream fout = new FileOutputStream(outputDirectory + "/" + schematronFilename);
	    OutputStreamWriter outputXML = new OutputStreamWriter(fout, outputFormat.getProperty("encoding"));
	    Serializer serializer = SerializerFactory.getSerializer(outputFormat);
	    serializer.setWriter(outputXML);
	    serializer.asDOMSerializer().serialize(document);
	    outputXML.close();
	} catch (Exception e) {
	    String m = e.getMessage();
	    if (m != null) {
		result.addError(m);
	    }
	    e.printStackTrace(System.err);
	}

	// Indicate we did it to avoid doing it again
	printed = true;
    }
}
