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
package de.interactive_instruments.ShapeChange.Target.Metadata;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessConfiguration;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.RuleRegistry;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.MalformedProfileIdentifierException;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Profile.Profiles;
import de.interactive_instruments.ShapeChange.Target.SingleTarget;
import de.interactive_instruments.ShapeChange.Target.TargetOutputProcessor;
import de.interactive_instruments.ShapeChange.UI.StatusBoard;
import de.interactive_instruments.ShapeChange.Util.XMLUtil;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class ApplicationSchemaMetadata implements SingleTarget, MessageSource {

    public static final String NS = "http://shapechange.net/targets/ApplicationSchemaMetadata";

    public static int STATUS_RULE_ALL_IDENTIFY_PROFILES = 301500;
    public static int STATUS_RULE_ALL_IDENTIFY_TYPE_USAGE = 301501;
    public static int STATUS_RULE_ALL_IDENTIFY_PROPERTIES_WITH_SPECIFIC_TAGGED_VALUES = 301502;

    /* ------------------------------------------- */
    /* --- configuration parameter identifiers --- */
    /* ------------------------------------------- */

    /**
     * Required parameter for rule {@value #RULE_ALL_IDENTIFY_TYPE_USAGE}. Multiple
     * (union or data) type names are separated by commas.
     */
    public static final String PARAM_TYPES_FOR_TYPE_USAGE_IDENTIFICATION = "typesForTypeUsageIdentification";

    /* ------------------------ */
    /* --- rule identifiers --- */
    /* ------------------------ */

    /**
     * This rule identifies the names of all the profiles to which elements (classes
     * and properties) in selected schemas belong. Identified profiles are listed
     * within ProfilesMetadata/containedProfile elements.
     */
    public static final String RULE_ALL_IDENTIFY_PROFILES = "rule-asm-all-identify-profiles";

    /**
     * This rule identifies which feature and object types directly or indirectly
     * make use (as property value type) of one or more of the union or data types
     * defined by target parameter
     * {@value #PARAM_TYPES_FOR_TYPE_USAGE_IDENTIFICATION}.
     */
    public static final String RULE_ALL_IDENTIFY_TYPE_USAGE = "rule-asm-all-identifyTypeUsage";

    /**
     * This rule identifies all properties (maybe including inherited ones, if
     * {@link #PARAM_INHERITED_PROPERTIES} is true) of a class that have tagged
     * values whose name matches a regular expression defined by
     * {@link #PARAM_TAG_NAME_REGEX}. The optional parameter
     * {@link #PARAM_TAG_VALUE_REGEX} can be used to restrict the tags to only those
     * whose values match the regular expression defined by that parameter.
     */
    public static final String RULE_ALL_IDENTIFY_PROPERTIES_WITH_SPECIFIC_TAGGED_VALUES = "rule-asm-all-identifyPropertiesWithSpecificTaggedValues";
    /**
     * If true, then the output per class will not only contain the direct
     * properties of the class but also the inherited properties. Default is false.
     * Applis to {@link #RULE_ALL_IDENTIFY_PROPERTIES_WITH_SPECIFIC_TAGGED_VALUES}.
     */
    public static final String PARAM_INHERITED_PROPERTIES = "inheritedProperties";
    /**
     * Regular expression to match the name of tagged values that shall be reported
     * by {@link #RULE_ALL_IDENTIFY_PROPERTIES_WITH_SPECIFIC_TAGGED_VALUES}. The
     * parameter is required by that rule.
     */
    public static final String PARAM_TAG_NAME_REGEX = "tagNameRegex";
    /**
     * If set, then all values of a tag whose name matches
     * {@link #PARAM_TAG_NAME_REGEX} must match this regular expression, in order
     * for the tagged value to be reported by
     * {@link #RULE_ALL_IDENTIFY_PROPERTIES_WITH_SPECIFIC_TAGGED_VALUES}.
     */
    public static final String PARAM_TAG_VALUE_REGEX = "tagValueRegex";

    /* --------------------- */
    /* --- tagged values --- */
    /* --------------------- */

    // none at present

    /* -------------------- */
    /* --- other fields --- */
    /* -------------------- */

    private static boolean initialised = false;

    protected static Document document = null;
    protected static Element root = null;
    protected static Map<String, ProcessMapEntry> mapEntryByType = new HashMap<String, ProcessMapEntry>();

    private static File outputDirectoryFile;
    private static String outputDirectory;
    private static String outputFilename;

    private static boolean printed = false;
    private static boolean diagnosticsOnly = false;

    private static String schemaTargetNamespace = null;

    private ShapeChangeResult result = null;
    private PackageInfo schemaPi = null;
    private Model model = null;
    private Options options = null;

    @Override
    public void initialise(PackageInfo p, Model m, Options o, ShapeChangeResult r, boolean diagOnly)
	    throws ShapeChangeAbortException {

	schemaPi = p;
	schemaTargetNamespace = p.targetNamespace();

	model = m;
	options = o;
	result = r;
	diagnosticsOnly = diagOnly;

	result.addDebug(this, 1, schemaPi.name());

	if (!initialised) {
	    initialised = true;

	    outputDirectory = options.parameter(this.getClass().getName(), "outputDirectory");
	    if (outputDirectory == null)
		outputDirectory = options.parameter("outputDirectory");
	    if (outputDirectory == null)
		outputDirectory = options.parameter(".");

	    outputFilename = "schema_metadata.xml";

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
	    List<ProcessMapEntry> mapEntries = options.getCurrentProcessConfig().getMapEntries();

	    for (ProcessMapEntry pme : mapEntries) {
		mapEntryByType.put(pme.getType(), pme);
	    }

	    // ======================================
	    // Parse configuration parameters
	    // ======================================

	    // nothing to do at present

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

	    root = document.createElementNS(NS, "ApplicationSchemaMetadata");
	    document.appendChild(root);

	    addAttribute(root, "xmlns", NS);

	    if (options.getCurrentProcessConfig().parameterAsString(TargetOutputProcessor.PARAM_ADD_COMMENT, null,
		    false, true) == null) {
		Comment generationComment = document.createComment("Created by ShapeChange - http://shapechange.net/");
		root.appendChild(generationComment);
	    }
	}

	// create elements documenting the application schema
	Element e_name = document.createElementNS(NS,"name");
	e_name.setTextContent(schemaPi.name());
	Element e_tns = document.createElementNS(NS,"targetNamespace");
	e_tns.setTextContent(schemaPi.targetNamespace());

	Element e_as = document.createElementNS(NS, "ApplicationSchema");
	e_as.appendChild(e_name);
	e_as.appendChild(e_tns);

	Element e_schema = document.createElementNS(NS, "schema");
	e_schema.appendChild(e_as);

	root.appendChild(e_schema);

	/*
	 * Now compute relevant metadata.
	 */
	processMetadata(e_as);
    }

    protected void processMetadata(Element appSchemaElement) {

	if (schemaPi != null) {

	    if (schemaPi.matches(RULE_ALL_IDENTIFY_PROFILES)) {

		StatusBoard.getStatusBoard().statusChanged(STATUS_RULE_ALL_IDENTIFY_PROFILES);

		processProfilesMetadata(appSchemaElement);
	    }

	    if (schemaPi.matches(RULE_ALL_IDENTIFY_TYPE_USAGE)) {
		StatusBoard.getStatusBoard().statusChanged(STATUS_RULE_ALL_IDENTIFY_TYPE_USAGE);

		identifyTypeUsage(appSchemaElement);
	    }

	    if (schemaPi.matches(RULE_ALL_IDENTIFY_PROPERTIES_WITH_SPECIFIC_TAGGED_VALUES)) {
		StatusBoard.getStatusBoard()
			.statusChanged(STATUS_RULE_ALL_IDENTIFY_PROPERTIES_WITH_SPECIFIC_TAGGED_VALUES);

		identifyPropertiesWithSpecificTaggedValues(appSchemaElement);
	    }

	    // add additional rules to compute metadata here
	}

    }

    private void identifyPropertiesWithSpecificTaggedValues(Element appSchemaElement) {

	ProcessConfiguration config = options.getCurrentProcessConfig();

	Pattern tagNamePattern = null;
	try {
	    tagNamePattern = config.parameterAsRegexPattern(PARAM_TAG_NAME_REGEX, null);
	    if (tagNamePattern == null) {
		result.addError(this, 10, PARAM_TAG_NAME_REGEX,
			RULE_ALL_IDENTIFY_PROPERTIES_WITH_SPECIFIC_TAGGED_VALUES);
		return;
	    }
	} catch (PatternSyntaxException e) {
	    result.addError(this, 9, PARAM_TAG_NAME_REGEX, e.getMessage());
	}

	Pattern tagValuePattern = null;
	try {
	    tagValuePattern = config.parameterAsRegexPattern(PARAM_TAG_VALUE_REGEX, null);
	} catch (PatternSyntaxException e) {
	    result.addError(this, 9, PARAM_TAG_VALUE_REGEX, e.getMessage());
	}

	boolean inheritedProperties = config.parameterAsBoolean(PARAM_INHERITED_PROPERTIES, false);

	Element e_pwstv = document.createElementNS(NS, "PropertiesWithSpecificTaggedValues");
	appSchemaElement.appendChild(e_pwstv);

	SortedSet<ClassInfo> schemaClasses = model.classes(schemaPi);

	for (ClassInfo ci : schemaClasses) {

	    Element e_class = document.createElementNS(NS, "Class");
	    e_class.setAttribute("name", ci.name());

	    e_pwstv.appendChild(e_class);

	    Collection<PropertyInfo> properties = inheritedProperties ? (Collection<PropertyInfo>) ci.propertiesAll()
		    : ci.properties().values();
	    for (PropertyInfo pi : properties) {

		SortedMap<String, List<String>> tvs = pi.taggedValuesAll().asMap();
		SortedMap<String, List<String>> matchingTvs = new TreeMap<>();

		for (String tag : tvs.keySet()) {

		    if (tagNamePattern.matcher(tag).matches()) {

			List<String> values = tvs.get(tag);

			boolean relevantTag = true;

			if (tagValuePattern != null) {

			    if (values == null || values.isEmpty()) {
				relevantTag = false;
			    } else {
				boolean valuesOk = true;
				for (String value : values) {
				    if (!tagValuePattern.matcher(value).matches()) {
					valuesOk = false;
					break;
				    }
				}
				relevantTag = valuesOk;
			    }
			}

			if (relevantTag) {
			    matchingTvs.put(tag, values);
			}
		    }
		}

		if (!matchingTvs.isEmpty()) {

		    Element e_prop = document.createElementNS(NS, "Property");
		    e_prop.setAttribute("name", pi.name());

		    e_class.appendChild(e_prop);

		    for (Entry<String, List<String>> entry : matchingTvs.entrySet()) {

			String tag = entry.getKey();
			List<String> values = entry.getValue();

			Element e_tag = document.createElementNS(NS, "Tag");
			e_tag.setAttribute("name", tag);

			e_prop.appendChild(e_tag);

			if (values != null && !values.isEmpty()) {

			    for (String value : values) {
				Element e_val = document.createElementNS(NS, "Value");
				e_val.setTextContent(value);

				e_tag.appendChild(e_val);
			    }
			}
		    }
		}
	    }
	}
    }

    private void identifyTypeUsage(Element appSchemaElement) {

	/*
	 * 
	 */

	SortedSet<String> typesForTypeUsage = new TreeSet<>(options.parameterAsStringList(this.getClass().getName(),
		PARAM_TYPES_FOR_TYPE_USAGE_IDENTIFICATION, null, true, true));

	// NOTE: parameter value checked by configuration validator

	// identify all classes and properties that belong to the schema
	SortedSet<ClassInfo> schemaClasses = model.classes(schemaPi);

	if (schemaClasses != null) {

	    /*
	     * For a given feature or object type, get all direct and indirect properties -
	     * ignoring model routes that lead through feature or object types; also ignore
	     * enumerations and code lists. Keep track of visited types (to avoid an endless
	     * loop). Check if one of these properties has a type from the parameter.
	     */

	    SortedMap<ClassInfo, Set<String>> resultMap = new TreeMap<>();

	    for (ClassInfo ci : schemaClasses) {

		if (ci.category() == Options.FEATURE || ci.category() == Options.OBJECT) {

		    SortedSet<ClassInfo> foundDataOrUnionTypes = new TreeSet<>();

		    searchTypeUses(ci, foundDataOrUnionTypes);

		    Set<String> namesOfFoundDataOrUnionTypes = foundDataOrUnionTypes.stream().map(type -> type.name())
			    .filter(type -> typesForTypeUsage.contains(type)).collect(Collectors.toSet());

		    if (!namesOfFoundDataOrUnionTypes.isEmpty()) {

			resultMap.put(ci, namesOfFoundDataOrUnionTypes);
		    }
		}
	    }

	    // now create the ProfilesMetadata XML element
	    Element e_tum = document.createElementNS(NS, "TypesUsageMetadata");

	    if (resultMap.isEmpty()) {

		result.addInfo(this, 200, String.join(", ", typesForTypeUsage));

	    } else {

		result.addInfo(this, 201, String.join(", ", typesForTypeUsage));

		for (String type : typesForTypeUsage) {

		    SortedSet<String> classesUsingType = new TreeSet<>();

		    for (Entry<ClassInfo, Set<String>> e : resultMap.entrySet()) {

			if (e.getValue().contains(type)) {
			    classesUsingType.add(e.getKey().name());
			}
		    }

		    if (!classesUsingType.isEmpty()) {

			String classes = String.join(", ", classesUsingType);
			result.addInfo(this, 202, type, classes);
			Element e_tu = document.createElementNS(NS, "typeUsage");
			e_tu.setAttribute("type", type);
			e_tu.setAttribute("directlyOrIndirectlyUsedBy", classes);
			e_tum.appendChild(e_tu);
		    }
		}
	    }

	    Element e_m = document.createElementNS(NS, "metadata");

	    e_m.appendChild(e_tum);

	    appSchemaElement.appendChild(e_m);
	}
    }

    private void searchTypeUses(ClassInfo ci, SortedSet<ClassInfo> foundDataOrUnionTypes) {

	for (PropertyInfo pi : ci.properties().values()) {

	    ClassInfo type = model.classByIdOrName(pi.typeInfo());

	    if (type != null && !foundDataOrUnionTypes.contains(type)) {

		foundDataOrUnionTypes.add(type);

		if (type.category() == Options.DATATYPE || type.category() == Options.UNION) {
		    searchTypeUses(type, foundDataOrUnionTypes);
		}
	    }
	}
    }

    protected void processProfilesMetadata(Element appSchemaElement) {

	Set<Info> schemaElements = new TreeSet<Info>();

	// identify all classes and properties that belong to the schema
	SortedSet<ClassInfo> schemaClasses = model.classes(schemaPi);

	if (schemaClasses != null) {

	    for (ClassInfo ci : schemaClasses) {
		schemaElements.add(ci);

		for (PropertyInfo pi : ci.properties().values()) {
		    schemaElements.add(pi);
		}
	    }
	}

	// identify the profiles of all relevant schema elements
	SortedSet<String> profileNames = new TreeSet<String>();

	for (Info i : schemaElements) {

	    String[] profilesTVs = i.taggedValuesForTag("profiles");

	    for (String profilesTV : profilesTVs) {

		if (profilesTV.trim().length() > 0) {

		    try {

			Profiles piMap = Profiles.parse(profilesTV, false);

			profileNames.addAll(piMap.getProfileIdentifiersByName().keySet());

		    } catch (MalformedProfileIdentifierException e) {
			MessageContext mc = result.addWarning(null, 20201);
			if (mc != null) {
			    mc.addDetail(null, 20216, i.fullNameInSchema());
			    mc.addDetail(null, 20217, e.getMessage());
			    mc.addDetail(null, 20218, profilesTV);
			}
		    }
		}
	    }
	}

	// now create the ProfilesMetadata XML element
	Element e_pm = document.createElementNS(NS, "ProfilesMetadata");

	for (String name : profileNames) {
	    Element e_cp = document.createElementNS(NS, "containedProfile");
	    e_cp.setTextContent(name);
	    e_pm.appendChild(e_cp);
	}

	Element e_m = document.createElementNS(NS, "metadata");

	e_m.appendChild(e_pm);

	appSchemaElement.appendChild(e_m);
    }

    /**
     * Add attribute to an element
     * 
     * @param e     tbd
     * @param name  tbd
     * @param value tbd
     */
    protected void addAttribute(Element e, String name, String value) {
	Attr att = document.createAttribute(name);
	att.setValue(value);
	e.setAttributeNode(att);
    }

    @Override
    public void process(ClassInfo ci) {

	/*
	 * This target gathers metadata about an application schema as-is. As such, it
	 * can do so directly with the package provided by the initialize method.
	 * Ordering of classes is handled directly by this class.
	 * 
	 * NOTE: the Converter.STATUS_TARGET_PROCESS can therefore be ignored for this
	 * target.
	 */
    }

    @Override
    public void write() {

	// nothing to do here
    }

    @Override
    public String getTargetName() {
	return "Application Schema Metadata";
    }

    @Override
    public void writeAll(ShapeChangeResult r) {

	if (printed || diagnosticsOnly) {
	    return;
	}

	try {
	    File repXsd = new File(outputDirectoryFile, outputFilename);
	    
	    XMLUtil.writeXml(document, repXsd);

	    r.addResult(getTargetName(), outputDirectory, outputFilename, schemaTargetNamespace);

	} catch (ShapeChangeException ioe) {

	    r.addError(this, 3, outputFilename, ioe.getMessage());
	}

	printed = true;
    }

    @Override
    public void registerRulesAndRequirements(RuleRegistry r) {
	r.addRule("rule-asm-all-identify-profiles");
	r.addRule("rule-asm-all-identifyTypeUsage");
	r.addRule("rule-asm-all-identifyPropertiesWithSpecificTaggedValues");
    }

    @Override
    public String getTargetIdentifier() {
	return "asm";
    }

    @Override
    public String getDefaultEncodingRule() {
	return "*";
    }

    @Override
    public void reset() {

	initialised = false;

	schemaTargetNamespace = null;

	outputDirectoryFile = null;
	outputDirectory = null;
	outputFilename = null;

	printed = false;
	diagnosticsOnly = false;

	document = null;
	root = null;
	mapEntryByType = new HashMap<String, ProcessMapEntry>();
    }

    @Override
    public String message(int mnr) {

	switch (mnr) {
	case 0:
	    return "Context: class ApplicationSchemaMetadata";
	case 1:
	    return "Retrieving metadata for application schema '$1$'.";
	case 2:
	    return "XML Schema document with name '$1$' could not be created, invalid filename.";
	case 3:
	    return "Could not write output to file '$1$'. Exception message is: $2$.";
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
	    return "Syntax exception while compiling the regular expression defined by target parameter '$1$': '$2$'.";
	case 10:
	    return "Parameter '$1$' required by rule '$2$' was not set. The rule will be ignored.";

	case 100:
	    return "Context: property '$1$' in class '$2$'.";

	case 200:
	    return "No classes with direct or indirect use of the type(s) $1$.";
	case 201:
	    return "One or more classes with direct or indirect use of the type(s) $1$ was found.";
	case 202:
	    return "Type '$1$' directly or indirectly used by the following class(es): $2$";

	default:
	    return "(" + ApplicationSchemaMetadata.class.getName() + ") Unknown message with number: " + mnr;
	}
    }
}
