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
 * (c) 2002-2017 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target.CDB;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.base.Joiner;

import de.interactive_instruments.ShapeChange.MapEntryParamInfos;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.RuleRegistry;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.SingleTarget;
import de.interactive_instruments.ShapeChange.Util.XMLUtil;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class CDB implements SingleTarget, MessageSource {

    public static final String NS_FDD = "http://www.opengis.net/cdb/1.0/Feature_Data_Dictionary";
    public static final String NS_AD = "http://www.opengis.net/cdb/1.0/Vector_Attributes";
    public static final String NSABR = "cdb";

    public static final String MAPENTRY_PARAM_NUMERIC_FORMAT = "numericFormat";

    public static final String MAPENTRY_PARAM_NUMERIC_FORMAT_FLOATINGPOINT = "Floating-Point";
    public static final String MAPENTRY_PARAM_NUMERIC_FORMAT_INTEGER = "Integer";

    private static final String FEATURE_DICTIONARY_FILENAME_SUFFIX = "_Feature_Data_Dictionary.xml";
    private static final String ATTRIBUTES_DICTIONARY_FILENAME_SUFFIX = "_Attributes.xml";

    /* ------------------------------------------- */
    /* --- configuration parameter identifiers --- */
    /* ------------------------------------------- */

    public static final String PARAM_VERSION = "version";
    public static final String PARAM_UNITS_TO_IGNORE = "unitsToIgnore";

    /* ------------------------ */
    /* --- rule identifiers --- */
    /* ------------------------ */

    public static final String RULE_TGT_CDB_ALL_NOTENCODED = "rule-cdb-all-notEncoded";
    public static final String RULE_TGT_CDB_ALL_VALUETYPETEXT_FOR_UNION_REPRESENTING_FEATURESET = "rule-cdb-all-valueTypeTextForUnionRepresentingFeatureSet";

    /* --------------------- */
    /* --- tagged values --- */
    /* --------------------- */

    // none at present

    /* -------------------- */
    /* --- other fields --- */
    /* -------------------- */

    protected static boolean initialised = false;
    protected static boolean atLeastOneSchemaIsEncoded = false;

    protected static MapEntryParamInfos mapEntryParamInfos = null;

    protected static SortedSet<ClassInfo> featureTypes = new TreeSet<ClassInfo>();
    protected static Map<String, CDBUnit> unitDefsFromConfigByName = new HashMap<String, CDBUnit>();
    protected static Set<String> unitsToIgnore = new HashSet<String>();
    protected static SortedSet<String> schemaVersions = new TreeSet<String>();

    private static File outputDirectoryFile;
    private static String outputDirectory;
    private static String outputFilename;

    private static boolean diagnosticsOnly = false;

    private boolean schemaNotEncoded = false;

    private ShapeChangeResult result = null;
    private PackageInfo schema = null;
    private Options options = null;

    @Override
    public void initialise(PackageInfo p, Model m, Options o, ShapeChangeResult r, boolean diagOnly)
	    throws ShapeChangeAbortException {

	schema = p;
	options = o;
	result = r;
	diagnosticsOnly = diagOnly;

	if (!isEncoded(schema)) {

	    schemaNotEncoded = true;
	    result.addInfo(this, 7, schema.name());
	    return;
	} else {
	    atLeastOneSchemaIsEncoded = true;
	}

	result.addDebug(this, 1, schema.name());

	if (!initialised) {

	    initialised = true;

	    outputDirectory = options.parameter(this.getClass().getName(), "outputDirectory");
	    if (outputDirectory == null)
		outputDirectory = options.parameter("outputDirectory");
	    if (outputDirectory == null)
		outputDirectory = options.parameter(".");

	    outputFilename = options.getCurrentProcessConfig().parameterAsString("outputFilename", "ApplicationSchema",
		    false, true);

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

	    deleteFileIfExists(outputDirectoryFile, outputFilename + FEATURE_DICTIONARY_FILENAME_SUFFIX);
	    deleteFileIfExists(outputDirectoryFile, outputFilename + ATTRIBUTES_DICTIONARY_FILENAME_SUFFIX);

	    // identify map entries defined in the target configuration
	    List<ProcessMapEntry> mapEntries = options.getCurrentProcessConfig().getMapEntries();

	    if (mapEntries.isEmpty()) {

		/*
		 * It is unlikely but not impossible that an application schema does not make
		 * use of types that require a type mapping in order to be converted into a CDB
		 * dictionary.
		 */
		result.addWarning(this, 15);

	    } else {

		/*
		 * Parse all parameter information
		 */
		mapEntryParamInfos = new MapEntryParamInfos(result, mapEntries);
	    }

	    // identify units to ignore
	    unitsToIgnore.addAll(
		    options.getCurrentProcessConfig().parameterAsStringList(PARAM_UNITS_TO_IGNORE, null, true, true));

	    // parse Unit definitions
	    if (options.getCurrentProcessConfig().getAdvancedProcessConfigurations() == null) {

		result.addInfo(this, 12);

	    } else {

		Element advancedProcessConfigElmt = options.getCurrentProcessConfig()
			.getAdvancedProcessConfigurations();

		// identify CDBUnitDefinition elements
		List<Element> unitDefEs = new ArrayList<Element>();

		NodeList udNl = advancedProcessConfigElmt.getElementsByTagName("CDBUnitDefinition");

		if (udNl != null && udNl.getLength() != 0) {
		    for (int k = 0; k < udNl.getLength(); k++) {
			Node n = udNl.item(k);
			if (n.getNodeType() == Node.ELEMENT_NODE) {

			    unitDefEs.add((Element) n);
			}
		    }
		}

		for (int i = 0; i < unitDefEs.size(); i++) {

		    String indexForMsg = "" + (i + 1);

		    Element unitDefE = unitDefEs.get(i);

		    // parse code - can be empty
		    Integer code = null;
		    if (unitDefE.hasAttribute("code")) {

			String codeS = unitDefE.getAttribute("code");
			try {
			    int tmp = Integer.parseInt(codeS);
			    code = tmp;
			} catch (NumberFormatException e) {
			    result.addError(this, 16, indexForMsg, codeS);
			    continue;
			}
		    }

		    // parse name - must not be empty
		    Element nameE = XMLUtil.getFirstElement(unitDefE, "name");
		    String name = StringUtils.stripToEmpty(nameE.getTextContent());
		    if (name.isEmpty()) {
			result.addError(this, 13, "name", indexForMsg);
			continue;
		    }

		    /*
		     * parse aliasNames - optional, but cannot be empty if provided
		     */
		    Set<String> aliasNames = new HashSet<String>();
		    List<Element> aliasNameElmts = new ArrayList<Element>();
		    NodeList anNl = unitDefE.getElementsByTagName("alias");

		    if (anNl != null && anNl.getLength() != 0) {
			for (int k = 0; k < anNl.getLength(); k++) {
			    Node n = anNl.item(k);
			    if (n.getNodeType() == Node.ELEMENT_NODE) {

				aliasNameElmts.add((Element) n);
			    }
			}
		    }

		    for (int anIndex = 0; anIndex < aliasNameElmts.size(); anIndex++) {
			String aliasName = StringUtils.stripToEmpty(aliasNameElmts.get(anIndex).getTextContent());
			if (aliasName.isEmpty()) {
			    result.addError(this, 13, "alias", indexForMsg);
			    continue;
			} else {
			    aliasNames.add(aliasName);
			}
		    }

		    // parse symbol - must not be empty
		    String symbol = StringUtils.stripToEmpty(unitDefE.getAttribute("symbol"));
		    if (symbol.isEmpty()) {
			result.addError(this, 17, indexForMsg);
			continue;
		    }

		    // parse description - can be empty
		    Element descrE = XMLUtil.getFirstElement(unitDefE, "description");
		    String description = null;
		    if (descrE != null) {
			description = StringUtils.stripToEmpty(descrE.getTextContent());
		    }

		    CDBUnit unit = new CDBUnit(code, symbol, name, aliasNames, description);
		    unitDefsFromConfigByName.put(name, unit);
		    for (String aliasName : aliasNames) {
			unitDefsFromConfigByName.put(aliasName, unit);
		    }
		}
	    }
	}
    }

    private void deleteFileIfExists(File directoryFile, String filename) {

	File file = new File(directoryFile, filename);

	// check if file already exists - if so, attempt to delete it
	boolean exi = file.exists();

	if (exi) {

	    result.addInfo(this, 3, file.getAbsolutePath());

	    try {
		FileUtils.forceDelete(file);
		result.addInfo(this, 4);
	    } catch (IOException e) {
		result.addInfo(null, 600, e.getMessage());
	    }
	}
    }

    @Override
    public String getTargetName() {
	return "CDB";
    }

    @Override
    public void process(ClassInfo ci) {
	/*
	 * Gather all feature types that shall be processed
	 */
	if (ci == null || ci.pkg() == null) {
	    return;
	}

	if (!isEncoded(ci)) {
	    result.addInfo(this, 8, ci.name());
	    return;
	}

	result.addDebug(this, 6, ci.name());

	if (schemaNotEncoded) {
	    result.addInfo(this, 9, schema.name(), ci.name());
	    return;
	}

	if (ci.category() == Options.FEATURE) {

	    featureTypes.add(ci);

	    // also keep track of schema version
	    schemaVersions.add(schema.version());

	} else if (ci.category() == Options.UNION
		&& ci.matches(RULE_TGT_CDB_ALL_VALUETYPETEXT_FOR_UNION_REPRESENTING_FEATURESET)
		&& "true".equalsIgnoreCase(ci.taggedValue("representsFeatureTypeSet"))) {
	    // fine

	} else {

	    result.addDebug(this, 10, ci.name());
	}
    }

    /**
     * Add attribute to an element
     * 
     * @param e     tbd
     * @param name  tbd
     * @param value tbd
     */
    protected void addAttribute(Element e, String name, String value) {

	Document document = e.getOwnerDocument();

	if (document == null) {
	    e.setAttribute(name, value);
	} else {
	    Attr att = document.createAttribute(name);
	    att.setValue(value);
	    e.setAttributeNode(att);
	}
    }

    public static boolean isEncoded(Info i) {

	if (i.matches(RULE_TGT_CDB_ALL_NOTENCODED) && i.encodingRule("cdb").equalsIgnoreCase("notencoded")) {

	    return false;
	} else {
	    return true;
	}
    }

    @Override
    public void write() {
	// nothing to do here
    }

    @Override
    public void writeAll(ShapeChangeResult result) {

	this.result = result;
	this.options = result.options();

	if (diagnosticsOnly || !atLeastOneSchemaIsEncoded) {
	    return;
	}

	if (featureTypes.isEmpty()) {
	    result.addInfo(this, 11);
	}

	// determine version
	String defaultVersion = Joiner.on(",").skipNulls().join(schemaVersions);
	String version = options.parameterAsString(this.getClass().getName(), PARAM_VERSION, defaultVersion, false,
		true);

	// create dictionaries
	createFeatureDataDictionary(version);
	createAttributesDictionary(version);
    }

    /**
     * @param version
     */
    /**
     * @param version
     */
    private void createAttributesDictionary(String version) {

	// === Create attributes dictionary structure from feature types

	/*
	 * key: symbol of attribute, value: attribute
	 */
	SortedMap<String, CDBAttribute> attributesBySymbol = new TreeMap<String, CDBAttribute>();
	SortedMap<String, CDBUnit> usedUnitsByName = new TreeMap<String, CDBUnit>();

	for (ClassInfo ci : featureTypes) {

	    /*
	     * TBD: use ci.propertiesAll()? to take properties from supertypes into account?
	     * We assume here that inheritance has been flattened.
	     */
	    for (PropertyInfo pi : ci.properties().values()) {

		String piEncodingRule = pi.encodingRule("cdb");
		ProcessMapEntry pme = options.targetMapEntry(pi.typeInfo().name, piEncodingRule);
		/*
		 * Determine if the property has a type that can be represented. Either a map
		 * entry exists for the property type, or the type is available in the model and
		 * is a feature type, enumeration, or code list (which are mapped to type
		 * 'Text').
		 */
		if (pme != null) {
		    // fine
		} else {
		    ClassInfo typeCi = pi.model().classById(pi.typeInfo().id);
		    if (typeCi == null) {
			typeCi = pi.model().classByName(pi.typeInfo().name);
		    }
		    if (typeCi != null && (typeCi.category() == Options.FEATURE
			    || typeCi.category() == Options.ENUMERATION || typeCi.category() == Options.CODELIST
			    || (typeCi.category() == Options.UNION
				    && typeCi.matches(RULE_TGT_CDB_ALL_VALUETYPETEXT_FOR_UNION_REPRESENTING_FEATURESET)
				    && "true".equalsIgnoreCase(typeCi.taggedValue("representsFeatureTypeSet"))))) {
			// fine
		    } else {
			/*
			 * Property type could not be determined or is not one of the supported ones
			 */
			result.addError(this, 14, pi.typeInfo().name);

			continue;
		    }
		}

		CDBAttribute attribute = new CDBAttribute(pi, mapEntryParamInfos);

		/*
		 * Before adding the attribute, see if one with same code already exists. If so,
		 * perform a semantic check and ignore the current attribute.
		 */
		if (attributesBySymbol.containsKey(attribute.getSymbol())) {

		    /*
		     * semantic comparison (description and value [type, format, range, length])
		     */

		    CDBAttribute exAtt = attributesBySymbol.get(attribute.getSymbol());

		    String exAttMin = exAtt.hasRange() ? StringUtils.stripToEmpty(exAtt.getRange().getMin()) : "";
		    String exAttMax = exAtt.hasRange() ? StringUtils.stripToEmpty(exAtt.getRange().getMax()) : "";

		    String attMin = attribute.hasRange() ? StringUtils.stripToEmpty(attribute.getRange().getMin()) : "";
		    String attMax = attribute.hasRange() ? StringUtils.stripToEmpty(attribute.getRange().getMax()) : "";

		    if (!attribute.getDescription().equalsIgnoreCase(exAtt.getDescription())
			    || attribute.getType() != exAtt.getType() || attribute.getFormat() != exAtt.getFormat()
			    || !StringUtils.equals(attribute.getLength(), exAtt.getLength()) || !exAttMin.equals(attMin)
			    || !exAttMax.equals(attMax)) {

			MessageContext mc = result.addWarning(this, 157);

			if (mc != null) {

			    mc.addDetail(this, 150, exAtt.propertyInfo().fullNameInSchema());
			    mc.addDetail(this, 151, attribute.propertyInfo().fullNameInSchema());

			    if (!attribute.getDescription().equalsIgnoreCase(exAtt.getDescription())) {

				mc.addDetail(this, 152, exAtt.getDescription(), attribute.getDescription());

			    }

			    if (attribute.getType() != exAtt.getType()) {
				mc.addDetail(this, 153, "" + exAtt.getType(), "" + attribute.getType());
			    }

			    if (attribute.getFormat() != exAtt.getFormat()) {

				String exAttFormat = exAtt.hasFormat() ? "" + exAtt.getFormat() : "<none>";
				String attFormat = attribute.hasFormat() ? "" + attribute.getFormat() : "<none>";

				mc.addDetail(this, 154, exAttFormat, attFormat);
			    }

			    if (!StringUtils.equals(attribute.getLength(), exAtt.getLength())) {

				String exAttLength = StringUtils.defaultIfBlank(exAtt.getLength(), "<none>");
				String attLength = StringUtils.defaultIfBlank(attribute.getLength(), "<none>");

				mc.addDetail(this, 155, exAttLength, attLength);
			    }

			    if (!exAttMin.equals(attMin) || !exAttMax.equals(attMax)) {

				mc.addDetail(this, 156, exAttMin, exAttMax, attMin, attMax);
			    }
			}
		    }

		} else {

		    attributesBySymbol.put(attribute.getSymbol(), attribute);

		    // determine unit
		    String uom = StringUtils.stripToNull(pi.taggedValue("recommendedMeasure"));

		    if (uom != null && !unitsToIgnore.contains(uom)) {

			if (unitDefsFromConfigByName.containsKey(uom)) {

			    CDBUnit unit = unitDefsFromConfigByName.get(uom);
			    attribute.setUnit(unit);
			    usedUnitsByName.put(uom, unit);

			} else {

			    MessageContext mc = result.addError(this, 200, uom, pi.name());
			    if (mc != null) {
				mc.addDetail(this, 1, pi.fullNameInSchema());
			    }
			}
		    }
		}
	    }
	}

	// assign codes to attributes and units
	int attCounter = 0;
	for (CDBAttribute att : attributesBySymbol.values()) {
	    attCounter++;
	    att.setCode(attCounter);
	}

	// determine highest number of existing codes
	int unitCounter = 0;
	for (CDBUnit unit : usedUnitsByName.values()) {
	    if (unit.hasCode() && unit.getCode() > unitCounter) {
		unitCounter = unit.getCode();
	    }
	}
	for (CDBUnit unit : usedUnitsByName.values()) {
	    if (!unit.hasCode()) {
		unitCounter++;
		unit.setCode(unitCounter);
	    }
	}

	// === Create XML representation of attributes dictionary
	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	dbf.setNamespaceAware(true);
	dbf.setValidating(true);
	dbf.setAttribute(Options.JAXP_SCHEMA_LANGUAGE, Options.W3C_XML_SCHEMA);

	try {

	    DocumentBuilder db = dbf.newDocumentBuilder();
	    Document document = db.newDocument();

	    Element root = document.createElementNS(NS_AD, "Vector_Attributes");
	    document.appendChild(root);

	    addAttribute(root, "xmlns", NS_AD);
	    addAttribute(root, "version", version);

	    Element attsElmt = addElement(NS_AD, "Attributes", root);

	    for (CDBAttribute att : attributesBySymbol.values()) {

		Element attElmt = addElement(NS_AD, "Attribute", attsElmt);
		addAttribute(attElmt, "code", "" + att.getCode());
		addAttribute(attElmt, "symbol", att.getSymbol());

		addElement(NS_AD, "Name", attElmt, att.getName());
		addElement(NS_AD, "Description", attElmt, att.getDescription());

		Element levelElmt = addElement(NS_AD, "Level", attElmt);
		addElement(NS_AD, "Instance", levelElmt, "Preferred");
		addElement(NS_AD, "Class", levelElmt, "Not Supported");
		addElement(NS_AD, "Extended", levelElmt, "Not Supported");

		Element valueElmt = addElement(NS_AD, "Value", attElmt);

		Element valueTypeElmt = addElement(NS_AD, "Type", valueElmt);
		if (att.getType() == CDBAttribute.Type.NUMERIC) {
		    valueTypeElmt.setTextContent("Numeric");
		} else if (att.getType() == CDBAttribute.Type.BOOLEAN) {
		    valueTypeElmt.setTextContent("Boolean");
		} else {
		    valueTypeElmt.setTextContent("Text");
		}

		if (att.hasFormat()) {
		    Element valueFormatElmt = addElement(NS_AD, "Format", valueElmt);
		    if (att.getFormat() == CDBAttribute.Format.INTEGER) {
			valueFormatElmt.setTextContent("Integer");
		    } else {
			valueFormatElmt.setTextContent("Floating-Point");
		    }
		}

		if (att.hasRange()) {
		    Element valueRangeElmt = addElement(NS_AD, "Range", valueElmt);
		    if (att.getRange().getMin() != null) {
			addElement(NS_AD, "Min", valueRangeElmt, att.getRange().getMin());
		    }
		    if (att.getRange().getMax() != null) {
			addElement(NS_AD, "Max", valueRangeElmt, att.getRange().getMax());
		    }
		}

		if (att.hasLength()) {
		    addElement(NS_AD, "length", valueElmt, att.getLength());
		}

		if (att.hasUnit()) {
		    addElement(NS_AD, "Unit", valueElmt, "" + att.getUnit().getCode());
		}
	    }

	    if (!usedUnitsByName.isEmpty()) {

		Element unitsElmt = addElement(NS_AD, "Units", root);

		for (CDBUnit unit : usedUnitsByName.values()) {

		    Element unitElmt = addElement(NS_AD, "Unit", unitsElmt);
		    addAttribute(unitElmt, "code", "" + unit.getCode());
		    addAttribute(unitElmt, "symbol", unit.getSymbol());

		    addElement(NS_AD, "Name", unitElmt, unit.getName());
		    addElement(NS_AD, "Description", unitElmt, unit.getDescription());
		}
	    }

	    print(document, ATTRIBUTES_DICTIONARY_FILENAME_SUFFIX);

	} catch (ParserConfigurationException e) {
	    result.addError(null, 2);
	}
    }

    private void createFeatureDataDictionary(String version) {

	// === Create feature data dictionary structure from feature types

	SortedMap<String, CDBCategory> categoriesByCode = new TreeMap<String, CDBCategory>();
	CDBCategory defaultCategory = null;

	for (ClassInfo ci : featureTypes) {

	    // identify category and subcategory
	    PackageInfo pkg = ci.pkg();
	    PackageInfo parentPkg = pkg.owner();

	    CDBCategory cat;
	    CDBSubcategory subcat;

	    if (pkg.isSchema()) {

		/*
		 * add feature to default category and its default subcategory
		 */

		if (defaultCategory == null) {
		    defaultCategory = new CDBCategory("Default", "Default");
		    categoriesByCode.put(defaultCategory.getCode(), defaultCategory);
		}

		cat = defaultCategory;
		subcat = defaultCategory.getDefaultSubcategory();

	    } else if (parentPkg.isSchema()) {

		/*
		 * add feature to category that represents 'pkg' and its default subcategory
		 */
		String catcode = getCode(pkg);

		if (categoriesByCode.containsKey(catcode)) {
		    cat = categoriesByCode.get(catcode);
		} else {
		    String catlabel = getLabel(pkg);
		    cat = new CDBCategory(catcode, catlabel);
		    categoriesByCode.put(cat.getCode(), cat);
		}

		subcat = cat.getDefaultSubcategory();

	    } else {

		/*
		 * add feature to category that represents 'parentPkg' and subcategory that
		 * represents 'pkg'
		 */
		String catcode = getCode(parentPkg);

		if (categoriesByCode.containsKey(catcode)) {
		    cat = categoriesByCode.get(catcode);
		} else {
		    String catlabel = getLabel(parentPkg);
		    cat = new CDBCategory(catcode, catlabel);
		    categoriesByCode.put(cat.getCode(), cat);
		}

		String subcatcode = getCode(pkg);

		if (cat.hasSubcategory(subcatcode)) {
		    subcat = cat.getSubcategory(subcatcode);
		} else {
		    String subcatlabel = getLabel(pkg);
		    subcat = new CDBSubcategory(subcatcode, subcatlabel);
		    cat.add(subcat);
		}
	    }

	    CDBFeature feature = new CDBFeature(ci);

	    if (subcat.hasFeature(feature.getCode())) {

		// semantic comparison (label and concept_definition)

		CDBFeature existingFeature = subcat.getFeature(feature.getCode());

		if (!feature.getLabel().equalsIgnoreCase(existingFeature.getLabel())) {
		    MessageContext mc = result.addWarning(this, 100, existingFeature.getLabel(), feature.getLabel());
		    if (mc != null) {
			mc.addDetail(this, 102, existingFeature.classInfo().fullNameInSchema());
			mc.addDetail(this, 103, feature.classInfo().fullNameInSchema());
		    }
		}
		if (!feature.getConceptDefinition().equalsIgnoreCase(existingFeature.getConceptDefinition())) {
		    MessageContext mc = result.addWarning(this, 101, existingFeature.getConceptDefinition(),
			    feature.getConceptDefinition());
		    if (mc != null) {
			mc.addDetail(this, 102, existingFeature.classInfo().fullNameInSchema());
			mc.addDetail(this, 103, feature.classInfo().fullNameInSchema());
		    }
		}

	    } else {
		subcat.add(feature);
	    }
	}

	// === Create XML representation of feature data dictionary

	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	dbf.setNamespaceAware(true);
	dbf.setValidating(true);
	dbf.setAttribute(Options.JAXP_SCHEMA_LANGUAGE, Options.W3C_XML_SCHEMA);

	try {

	    DocumentBuilder db = dbf.newDocumentBuilder();
	    Document document = db.newDocument();

	    Element root = document.createElementNS(NS_FDD, "Feature_Data_Dictionary");
	    document.appendChild(root);

	    addAttribute(root, "xmlns", NS_FDD);
	    addAttribute(root, "version", version);

	    for (CDBCategory cat : categoriesByCode.values()) {

		Element catElmt = addElement(NS_FDD, "Category", root);

		addAttribute(catElmt, "code", cat.getCode());

		addElement(NS_FDD, "Label", catElmt, cat.getLabel());

		for (CDBSubcategory subcat : cat.getSubcategoriesByCode().values()) {

		    Element subcatElmt = addElement(NS_FDD, "Subcategory", catElmt);

		    addAttribute(subcatElmt, "code", subcat.getCode());

		    addElement(NS_FDD, "Label", subcatElmt, subcat.getLabel());

		    for (CDBFeature feature : subcat.getFeatures().values()) {

			Element featureElmt = addElement(NS_FDD, "Feature_Type", subcatElmt);

			addAttribute(featureElmt, "code", feature.getCode());

			addElement(NS_FDD, "Label", featureElmt, feature.getLabel());

			Element subCodeElmt = addElement(NS_FDD, "Subcode", featureElmt);
			addAttribute(subCodeElmt, "code", "000");

			addElement(NS_FDD, "Label", subCodeElmt, feature.getLabel());

			addElement(NS_FDD, "Concept_Definition", subCodeElmt, feature.getConceptDefinition());

			// add empty element
			addElement(NS_FDD, "Recommended_Dataset_Component", subCodeElmt);

			addElement(NS_FDD, "Origin", subCodeElmt, feature.getOrigin());
		    }
		}
	    }

	    print(document, FEATURE_DICTIONARY_FILENAME_SUFFIX);

	} catch (ParserConfigurationException e) {
	    result.addError(null, 2);
	}
    }

    protected Element addElement(String namespace, String elementName, Element parent) {

	Document document = parent.getOwnerDocument();

	Element el = document.createElementNS(namespace, elementName);

	parent.appendChild(el);

	return el;
    }

    protected Element addElement(String namespace, String elementName, Element parent, String textValue) {

	Element el = addElement(namespace, elementName, parent);
	el.setTextContent(textValue);
	return el;
    }

    private String getCode(Info i) {

	if (StringUtils.isNotBlank(i.primaryCode())) {
	    return i.primaryCode().trim();
	} else {
	    return i.name();
	}
    }

    private String getLabel(Info i) {

	if (StringUtils.isNotBlank(i.aliasName())) {
	    return i.aliasName().trim();
	} else {
	    return i.name();
	}
    }

    private void print(Document document, String filenameSuffix) {

	String outFileName = outputFilename + filenameSuffix;

	File res = new File(outputDirectoryFile, outFileName);

	try {	    
	    XMLUtil.writeXml(document, res);

	    result.addResult(getTargetName(), outputDirectory, outFileName, null);

	} catch (ShapeChangeException e) {

	    result.addError(this, 5, outFileName, e.getMessage());
	}
    }

    @Override
    public void registerRulesAndRequirements(RuleRegistry r) {
	r.addRule("rule-cdb-all-notEncoded");
	r.addRule("rule-cdb-all-valueTypeTextForUnionRepresentingFeatureSet");
    }

    @Override
    public String getTargetIdentifier() {
	return "cdb";
    }

    @Override
    public String getDefaultEncodingRule() {
	return "*";
    }

    @Override
    public void reset() {

	initialised = false;

	outputDirectoryFile = null;
	outputDirectory = null;
	outputFilename = null;

	diagnosticsOnly = false;
	atLeastOneSchemaIsEncoded = false;

	mapEntryParamInfos = null;
	featureTypes = new TreeSet<ClassInfo>();
	schemaVersions = new TreeSet<String>();
	unitDefsFromConfigByName = new HashMap<String, CDBUnit>();
	unitsToIgnore = new HashSet<String>();
    }

    @Override
    public String message(int mnr) {

	switch (mnr) {
	case 0:
	    return "Context: class '$1$'";
	case 1:
	    return "Context: property '$1$'";

	case 2:
	    return "XML document with name '$1$' could not be created, invalid filename.";
	case 3:
	    return "File '$1$' already exists. Attempting to delete it...";
	case 4:
	    return "File has been deleted.";
	case 5:
	    return "Exception occurred while writing document to file '$1$'. Exception message is: $2$"; 
	case 6:
	    return "Processing class '$1$'.";
	case 7:
	    return "Schema '$1$' is not encoded.";
	case 8:
	    return "Class '$1$' is not encoded.";
	case 9:
	    return "Schema '$1$' is not encoded. Thus class '$2$' (which belongs to that schema) is not encoded either.";
	case 10:
	    return "Type '$1$' is ignored.";
	case 11:
	    return "No relevant feature type found. Consequently, no dictionaries are created.";
	case 12:
	    return "The target configuration does not contain an advanced process configuration element with CDB unit definitions. This is fine, unless relevant properties define units of measure.";
	case 13:
	    return "'$1$' element of $2$ CDBUnitDefinition element is empty which is not allowed. CDBUnitDefinition will be ignored.";
	case 14:
	    return "??No map entry exists for type '$1$'. Furthermore, the type could either not be found in the model or is not one of the types that are automatically mapped to 'Text'. Properties with this type will be ignored.";
	case 15:
	    return "No map entries provided via the configuration.";
	case 16:
	    return "'code' attribute of $1$ CDBUnitDefinition element cannot be parsed to an integer. Code value is '$2$'. CDBUnitDefinition will be ignored.";
	case 17:
	    return "'symbol' attribute of $1$ CDBUnitDefinition element is empty which is not allowed. CDBUnitDefinition will be ignored.";

	// 100-149 messages for semantic comparisons of features
	case 100:
	    return "Feature mapping with potentially inconsistent labels. Label of existing feature is '$1$', while that of the other feature (which will be ignored) is '$2$'.";
	case 101:
	    return "Feature mapping with potentially inconsistent concept definition. Definition of existing feature is '$1$', while that of the other feature (which will be ignored) is '$2$'.";
	case 102:
	    return "Context: existing feature '$1$'";
	case 103:
	    return "Context: other feature '$1$'";

	// 150-199 messages for semantic comparisons of attributes
	case 150:
	    return "Context: existing property '$1$'";
	case 151:
	    return "Context: other property '$1$'";
	case 152:
	    return "Property mapping with potentially inconsistent description. Description of existing property representation is '$1$', while that of the other property representation (which will be ignored) is '$2$'.";
	case 153:
	    return "Property mapping with potentially inconsistent value type. Type of existing property representation is '$1$', while that of the other property representation (which will be ignored) is '$2$'.";
	case 154:
	    return "Property mapping with potentially inconsistent value format. Format of existing property representation is '$1$', while that of the other property representation (which will be ignored) is '$2$'.";
	case 155:
	    return "Property mapping with potentially inconsistent value length. Length of existing property representation is '$1$', while that of the other property representation (which will be ignored) is '$2$'.";
	case 156:
	    return "Property mapping with potentially inconsistent value range. Range of existing property representation is [$1$-$2$], while that of the other property representation (which will be ignored) is [$3$-$4$].";
	case 157:
	    return "Property mapping with potential inconsistency.";

	// 200-299 messages related to units of measure
	case 200:
	    return "No CDBUnitDefinition found for recommended measure '$1$' of property '$2$'.";

	default:
	    return "(" + CDB.class.getName() + ") Unknown message with number: " + mnr;
	}
    }
}
