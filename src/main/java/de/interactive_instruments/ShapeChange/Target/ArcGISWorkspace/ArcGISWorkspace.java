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
 * (c) 2002-2018 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target.ArcGISWorkspace;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.sparx.Attribute;
import org.sparx.Collection;
import org.sparx.Connector;
import org.sparx.ConnectorEnd;
import org.sparx.Element;
import org.sparx.Package;
import org.sparx.Repository;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Multiplicity;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessConfiguration;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.RuleRegistry;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.Model.AssociationInfo;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.SingleTarget;
import de.interactive_instruments.ShapeChange.Util.ArcGISUtil;
import de.interactive_instruments.ShapeChange.Util.ea.EAAttributeUtil;
import de.interactive_instruments.ShapeChange.Util.ea.EAConnectorEndUtil;
import de.interactive_instruments.ShapeChange.Util.ea.EAConnectorUtil;
import de.interactive_instruments.ShapeChange.Util.ea.EAElementUtil;
import de.interactive_instruments.ShapeChange.Util.ea.EAException;
import de.interactive_instruments.ShapeChange.Util.ea.EAPackageUtil;
import de.interactive_instruments.ShapeChange.Util.ea.EARepositoryUtil;
import de.interactive_instruments.ShapeChange.Util.ea.EATaggedValue;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class ArcGISWorkspace implements SingleTarget, MessageSource {

    private static String REGEX_TO_SPLIT_BY_COMMA_WITH_ESCAPING = "(?<!(?:^|[^\\\\])(?:\\\\\\\\){0,5}\\\\),";
    private static String REGEX_TO_SPLIT_BY_COLON_WITH_ESCAPING = "(?<!(?:^|[^\\\\])(?:\\\\\\\\){0,5}\\\\):";

    /* -------------------- */
    /* --- enumerations --- */
    /* -------------------- */

    public enum ArcGISGeometryType {
	POINT("ArcGIS::Point"), MULTIPOINT("ArcGIS::Multipoint"), POLYLINE("ArcGIS::Polyline"),
	POLYGON("ArcGIS::Polygon"), UNKNOWN("Unknown"), NONE("None");

	private String stereotype;

	ArcGISGeometryType(String stereotype) {
	    this.stereotype = stereotype;
	}

	public String stereotypeValue() {
	    return stereotype;
	}
    }

    /* -------------------- */
    /* --- other fields --- */
    /* -------------------- */

    /**
     * Pattern to parse length constraints for property values, i.e. the property
     * name and maximum length.
     * <p>
     * (?:self\.)?(\w+)[\.\w+]*\.size\(\)\D*(\d+)
     * 
     * <p>
     * <ul>
     * <li>group 1: property name</li>
     * <li>group 2: max length value</li>
     * </ul>
     */
    private static final Pattern lengthConstraintPattern = Pattern
	    .compile("(?:self\\.)?(\\w+)[\\.\\w+]*\\.size\\(\\)\\D*(\\d+)");

    /**
     * Pattern to parse the lower boundary information from a numeric range
     * constraint.
     * 
     * <pre>
     * \.value\s*(?=>)(.*?)\s*([\+-]?[\.|\d]+)
     * </pre>
     * <ul>
     * <li>group 1: comparison operator for lower boundary (either '>' or '>=')</li>
     * <li>group 2: comparison value (e.g. '0', '30.3', '.045')</li>
     * </ul>
     */
    private static final Pattern numRangeConstraintLowerBoundaryPattern = Pattern
	    .compile("\\.value\\s*(?=>)(.*?)\\s*([\\+-]?[\\.|\\d]+)");

    /**
     * Pattern to parse the upper boundary information from a numeric range
     * constraint.
     * 
     * <pre>
     * \.value\s*(?=<)(.*?)\s*([\+-]?[\.|\d]+)
     * </pre>
     * <ul>
     * <li>group 1: comparison operator for upper boundary (either '<' or '<=')</li>
     * <li>group 2: comparison value (e.g. '0', '30.3', '.045')</li>
     * </ul>
     */
    private static final Pattern numRangeConstraintUpperBoundaryPattern = Pattern
	    .compile("\\.value\\s*(?=<)(.*?)\\s*([\\+-]?[\\.|\\d]+)");

    /**
     * Pattern to parse the property name from a numeric range constraint.
     * <p>
     * (?:self\.|\s)?(\w+)\.[\w\.]*?value(?:[,\s])
     * 
     * <p>
     * <ul>
     * <li>group 1: name of the property; WARNING: the name is expected to be
     * contained in the first find() - don't use subsequent finds, because they
     * could yield the name of interval properties (e.g. 'vs', 'vil', 'viu' -
     * specifically relevant for HOK or DGIM models)</li>
     * </ul>
     */
    private static final Pattern numRangeConstraintPropertyNamePattern = Pattern
	    .compile("(?:self\\.|\\s)?(\\w+)\\.[\\w\\.]*?value(?:[,\\s])");

    private static boolean initialised = false;
    private static String workspaceTemplateFilePath = ArcGISWorkspaceConstants.WORKSPACE_TEMPLATE_URL;

    private static int maxNameLength = ArcGISWorkspaceConstants.DEFAULT_MAX_NAME_LENGTH;
    private static int lengthTaggedValueDefault = ArcGISWorkspaceConstants.LENGTH_TAGGED_VALUE_DEFAULT;

    private static double numRangeDelta = ArcGISWorkspaceConstants.NUM_RANGE_DELTA;

    private static Set<String> esriTypesSuitedForRangeConstraint = new TreeSet<String>();

    private static String outputDirectory = null;
    private static File outputDirectoryFile = null;
    private static String documentationTemplate = null;
    private static String documentationNoValue = null;

    protected static String author = null;
    protected static String status = null;

    private static Repository rep = null;

    private static SortedSet<ClassInfo> ignoredCis = new TreeSet<ClassInfo>();

    private static SortedMap<ClassInfo, ArcGISGeometryType> geometryTypeCache = new TreeMap<ClassInfo, ArcGISGeometryType>();

    private static SortedMap<ClassInfo, Integer> elementIdByClassInfo = new TreeMap<ClassInfo, Integer>();
    private static SortedMap<ClassInfo, String> elementNameByClassInfo = new TreeMap<ClassInfo, String>();

    private static SortedSet<Integer> elementIdsOfUnusedCodedValueDomain = new TreeSet<>();

    private static SortedMap<ClassInfo, String> objectIdAttributeGUIDByClass = new TreeMap<ClassInfo, String>();
    private static SortedMap<ClassInfo, String> identifierAttributeGUIDByClass = new TreeMap<ClassInfo, String>();

    private static SortedMap<ClassInfo, ClassInfo> generalisations = new TreeMap<ClassInfo, ClassInfo>();
    private static SortedSet<AssociationInfo> associations = new TreeSet<AssociationInfo>();

    /**
     * Set of explicit ArcGIS subtypes, for establishing generalization
     * relationships with stereotype ArcGIS::Subtype during writeAll.
     */
    private static Set<ClassInfo> arcgisSubtypes = new HashSet<>();
    private static Map<ClassInfo, Map<String, Integer>> subtypeElementIdBySubtypeNameByParent = new TreeMap<>();
    /**
     * Key: 1) parent class (ClassInfo), 2) property of parent class (PropertyInfo)
     * <p>
     * Value: SortedMap with key being the subtype name and value being the EA
     * element ID of the subtype specific coded value domain to be used as type of
     * the field that represents the property in the subtype
     */
    @SuppressWarnings("rawtypes")
    private static MultiKeyMap subtypeCodedValueDomainEAIDByMultiKey = new MultiKeyMap();

    /**
     * key: name that would usually be assigned to a relationship class; value:
     * counter for the number of occurrences of this particular name (assume 0 if a
     * name is not contained as key yet)
     */
    private static SortedMap<String, Integer> counterByRelationshipClassName = new TreeMap<String, Integer>();

    /**
     * key: class info object; value: map that keeps track of property names used
     * [key: name that would usually be assigned to the property; value: counter for
     * the number of occurrences of this particular name (assume 0 if a name is not
     * contained as key yet)]
     */
    private static SortedMap<ClassInfo, SortedMap<String, Integer>> counterByPropertyNameByClass = new TreeMap<ClassInfo, SortedMap<String, Integer>>();

    private static Model model = null;

    private Options options = null;
    private ShapeChangeResult result = null;

    protected static int numberOfSchemasSelectedForProcessing = 0;

    /**
     * &lt;&lt;ArcGIS&gt;&gt; workspace package that represents the application
     * schema package.
     */
    protected static Integer workspacePkgId;

    /**
     * &lt;&lt;FeatureDataset&gt;&gt; package where all feature types with supported
     * ArcGIS geometry are stored (in package itself or sub-packages according to
     * package hierarchy in the application schema).
     */
    protected static Integer featuresPkgId;

    /**
     * Package where all feature types without geometry and object types are stored
     * (in package itself or sub-packages according to package hierarchy in the
     * application schema).
     */
    protected static Integer tablesPkgId;

    /**
     * Package where all association classes used to represent n:m relationships are
     * stored.
     */
    protected static Integer assocClassesPkgId;

    /**
     * Package where all code lists and enumerations are stored (in package itself
     * or sub-packages according to package hierarchy in the application schema).
     */
    protected static Integer domainsPkgId;

    /**
     * key: workspace sub package; value: {key: application schema package; value:
     * corresponding EA package within the workspace sub package}
     */
    protected static SortedMap<Integer, SortedMap<PackageInfo, Integer>> eaPkgIdByModelPkg_byWorkspaceSubPkgId = new TreeMap<Integer, SortedMap<PackageInfo, Integer>>();

    /**
     * NOTE: value of 'rule' attribute is currently ignored
     * <p>
     * key: 'type' attribute value of map entry defined for the target; value:
     * according map entry
     */
    protected static SortedMap<String, ProcessMapEntry> processMapEntries = null;

    protected static SortedMap<String, Integer> lengthMappingByTypeName = new TreeMap<String, Integer>();
    protected static SortedMap<String, Integer> precisionMappingByTypeName = new TreeMap<String, Integer>();
    protected static SortedMap<String, Integer> scaleMappingByTypeName = new TreeMap<String, Integer>();

    /**
     * Contains information about the maximum length of a property value (usually of
     * a textual type).
     * 
     * key: {class name}_{property name}; value: the max length of the property
     * value
     */
    protected static SortedMap<String, Integer> lengthByClassPropName = new TreeMap<String, Integer>();

    /**
     * Contains information about the numeric ranges defined for specific class
     * properties via OCL constraints.
     * 
     * key: class; value: map with [key: property name; value: the numeric range for
     * the property]
     */
    protected static SortedMap<ClassInfo, SortedMap<String, NumericRangeConstraintMetadata>> numericRangeConstraintByPropNameByClassName = new TreeMap<ClassInfo, SortedMap<String, NumericRangeConstraintMetadata>>();

    protected static String nameOfTVToDetermineFieldLength = "size";

    private static String absolutePathOfOutputEaRepositoryFile;

    private static String shortNameByTaggedValue;

    private static boolean keepCaseOfRolename = false;

    private static String foreignKeySuffix;
    private static String reflexiveRelationshipAttributeSuffix;

    protected static boolean representTaggedValues = false;
    protected static SortedSet<String> taggedValuesToRepresent = null;

    /**
     * key: name of the class element; value: the range domain element
     */
    private static SortedMap<String, Integer> numericRangeElementIdsByClassName = new TreeMap<String, Integer>();

    public void initialise(PackageInfo p, Model m, Options o, ShapeChangeResult r, boolean diagOnly)
	    throws ShapeChangeAbortException {

	options = o;
	result = r;

	if (!initialised) {
	    initialised = true;

	    model = m;

	    numberOfSchemasSelectedForProcessing = m.selectedSchemas().size();

	    // initialize mappings
	    lengthMappingByTypeName.put("esriFieldTypeInteger", 0);
	    lengthMappingByTypeName.put("esriFieldTypeDouble", 0);
	    lengthMappingByTypeName.put("esriFieldTypeDate", 0);

	    precisionMappingByTypeName.put("esriFieldTypeString", 0);
	    precisionMappingByTypeName.put("esriFieldTypeInteger", 9);
	    precisionMappingByTypeName.put("esriFieldTypeDouble", 10);
	    precisionMappingByTypeName.put("esriFieldTypeDate", 0);

	    scaleMappingByTypeName.put("esriFieldTypeString", 0);
	    scaleMappingByTypeName.put("esriFieldTypeInteger", 0);
	    scaleMappingByTypeName.put("esriFieldTypeDouble", 6);
	    scaleMappingByTypeName.put("esriFieldTypeDate", 0);

	    // get output location
	    outputDirectory = options.parameter(this.getClass().getName(), ArcGISWorkspaceConstants.PARAM_OUTPUT_DIR);
	    if (outputDirectory == null)
		outputDirectory = options.parameter("outputDirectory");
	    if (outputDirectory == null)
		outputDirectory = options.parameter(".");

	    String outputFilename = options.parameter(this.getClass().getName(),
		    ArcGISWorkspaceConstants.PARAM_OUTPUT_FILENAME);
	    if (outputFilename == null) {
		outputFilename = p.name();
	    }
	    outputFilename = outputFilename.replace("/", "_").replace(" ", "_") + ".qea";

	    author = options.parameterAsString(this.getClass().getName(), ArcGISWorkspaceConstants.PARAM_EA_AUTHOR,
		    null, false, true);

	    status = options.parameterAsString(this.getClass().getName(), ArcGISWorkspaceConstants.PARAM_EA_STATUS,
		    null, false, true);

	    // parse default length parameter
	    String defaultLengthParamValue = options.parameter(this.getClass().getName(),
		    ArcGISWorkspaceConstants.PARAM_LENGTH_TAGGED_VALUE_DEFAULT);
	    if (defaultLengthParamValue != null) {

		try {

		    int length = Integer.parseInt(defaultLengthParamValue);
		    lengthTaggedValueDefault = length;

		} catch (NumberFormatException e) {

		    result.addError(this, 13, ArcGISWorkspaceConstants.PARAM_LENGTH_TAGGED_VALUE_DEFAULT,
			    "" + ArcGISWorkspaceConstants.LENGTH_TAGGED_VALUE_DEFAULT);
		}
	    }

	    // parse max name length parameter
	    String maxNameLengthParamValue = options.parameter(this.getClass().getName(),
		    ArcGISWorkspaceConstants.PARAM_MAX_NAME_LENGTH);
	    if (maxNameLengthParamValue != null) {
		try {
		    int maxNameLengthTmp = Integer.parseInt(maxNameLengthParamValue);
		    maxNameLength = maxNameLengthTmp;
		} catch (NumberFormatException e) {
		    result.addError(this, 13, ArcGISWorkspaceConstants.PARAM_MAX_NAME_LENGTH,
			    "" + ArcGISWorkspaceConstants.DEFAULT_MAX_NAME_LENGTH);
		}
	    }

	    // check parameter with name of the tagged value that determines the
	    // field length
	    String nameOfTVToDetermineFieldLengthParamValue = options.parameter(this.getClass().getName(),
		    ArcGISWorkspaceConstants.PARAM_NAME_OF_TV_TO_DETERMINE_FIELD_LENGTH);
	    if (nameOfTVToDetermineFieldLengthParamValue != null
		    && nameOfTVToDetermineFieldLengthParamValue.trim().length() > 0) {
		nameOfTVToDetermineFieldLength = nameOfTVToDetermineFieldLengthParamValue.trim();
	    }

	    // Check if we can use the output directory; create it if it
	    // does not exist
	    outputDirectoryFile = new File(outputDirectory);
	    boolean exi = outputDirectoryFile.exists();
	    if (!exi) {
		try {
		    FileUtils.forceMkdir(outputDirectoryFile);
		} catch (IOException e) {
		    result.addError(this, 5, e.getMessage());
		    e.printStackTrace(System.err);
		}
		exi = outputDirectoryFile.exists();
	    }
	    boolean dir = outputDirectoryFile.isDirectory();
	    boolean wrt = outputDirectoryFile.canWrite();
	    boolean rea = outputDirectoryFile.canRead();
	    if (!exi || !dir || !wrt || !rea) {
		result.addFatalError(this, 1, outputDirectory);
		throw new ShapeChangeAbortException();
	    }

	    File outputFile = new File(outputDirectoryFile, outputFilename);

	    // check if output file already exists - if so, attempt to delete it
	    exi = outputFile.exists();
	    if (exi) {

		result.addInfo(this, 2, outputFilename, outputDirectory);

		try {
		    FileUtils.forceDelete(outputFile);
		    result.addInfo(this, 3);
		} catch (IOException e) {
		    result.addInfo(this, 4, e.getMessage());
		    e.printStackTrace(System.err);
		}
	    }

	    // read workspace template

	    workspaceTemplateFilePath = options.parameter(this.getClass().getName(),
		    ArcGISWorkspaceConstants.PARAM_WORKSPACE_TEMPLATE);

	    if (workspaceTemplateFilePath == null) {
		workspaceTemplateFilePath = options.parameter(ArcGISWorkspaceConstants.PARAM_WORKSPACE_TEMPLATE);
	    }
	    // if no path is provided, use the directory of the default template
	    if (workspaceTemplateFilePath == null) {
		workspaceTemplateFilePath = ArcGISWorkspaceConstants.WORKSPACE_TEMPLATE_URL;
		result.addInfo(this, 9, ArcGISWorkspaceConstants.PARAM_WORKSPACE_TEMPLATE,
			ArcGISWorkspaceConstants.WORKSPACE_TEMPLATE_URL);
	    }

	    // copy template file either from remote or local URI
	    if (workspaceTemplateFilePath.toLowerCase().startsWith("http")) {
		try {
		    URL templateUrl = new URL(workspaceTemplateFilePath);
		    FileUtils.copyURLToFile(templateUrl, outputFile);
		} catch (MalformedURLException e1) {
		    result.addFatalError(this, 6, workspaceTemplateFilePath, e1.getMessage());
		    throw new ShapeChangeAbortException();
		} catch (IOException e2) {
		    result.addFatalError(this, 8, e2.getMessage());
		    throw new ShapeChangeAbortException();
		}
	    } else {
		File workspacetemplate = new File(workspaceTemplateFilePath);
		if (workspacetemplate.exists()) {
		    try {
			FileUtils.copyFile(workspacetemplate, outputFile);
		    } catch (IOException e) {
			result.addFatalError(this, 8, e.getMessage());
			throw new ShapeChangeAbortException();
		    }
		} else {
		    result.addFatalError(this, 7, workspacetemplate.getAbsolutePath());
		    throw new ShapeChangeAbortException();
		}
	    }

	    // connect to EA repository in outputFile
	    absolutePathOfOutputEaRepositoryFile = outputFile.getAbsolutePath();

	    rep = new Repository();

	    if (!rep.OpenFile(absolutePathOfOutputEaRepositoryFile)) {
		String errormsg = rep.GetLastError();
		r.addError(null, 30, errormsg, outputFilename);
		rep = null;
		throw new ShapeChangeAbortException();
	    }

	    EARepositoryUtil.setEABatchAppend(rep, true);
	    EARepositoryUtil.setEAEnableUIUpdates(rep, false);

	    // get template packages
	    rep.RefreshModelView(0);

	    Collection<Package> c = rep.GetModels();
	    Package root = c.GetAt((short) 0);

	    Collection<Package> modelPkgs = root.GetPackages();
	    if (modelPkgs.GetCount() == 0 || !modelPkgs.GetAt((short) 0).GetStereotypeEx().equalsIgnoreCase("ArcGIS")) {
		result.addError(this, 9);
		throw new ShapeChangeAbortException();
	    } else {
		Package workspacePkg = modelPkgs.GetAt((short) 0);
		workspacePkgId = workspacePkg.GetPackageID();

		// eaPkgByModelPkg.put(appSchemaPkg, workspacePkg);
	    }

	    Integer features = EARepositoryUtil.getEAChildPackageByName(rep, workspacePkgId,
		    ArcGISWorkspaceConstants.TEMPLATE_PKG_FEATURES_NAME);
	    if (features == null) {
		result.addError(this, 102, ArcGISWorkspaceConstants.TEMPLATE_PKG_FEATURES_NAME);
		throw new ShapeChangeAbortException();
	    } else {
		featuresPkgId = features;
		eaPkgIdByModelPkg_byWorkspaceSubPkgId.put(featuresPkgId, new TreeMap<PackageInfo, Integer>());
	    }

	    Integer domains = EARepositoryUtil.getEAChildPackageByName(rep, workspacePkgId,
		    ArcGISWorkspaceConstants.TEMPLATE_PKG_DOMAINS_NAME);
	    if (domains == null) {
		result.addError(this, 102, ArcGISWorkspaceConstants.TEMPLATE_PKG_DOMAINS_NAME);
		throw new ShapeChangeAbortException();
	    } else {
		domainsPkgId = domains;
		eaPkgIdByModelPkg_byWorkspaceSubPkgId.put(domainsPkgId, new TreeMap<PackageInfo, Integer>());
	    }

	    Integer tables = EARepositoryUtil.getEAChildPackageByName(rep, workspacePkgId,
		    ArcGISWorkspaceConstants.TEMPLATE_PKG_TABLES_NAME);
	    if (tables == null) {
		result.addError(this, 102, ArcGISWorkspaceConstants.TEMPLATE_PKG_TABLES_NAME);
		throw new ShapeChangeAbortException();
	    } else {
		tablesPkgId = tables;
		eaPkgIdByModelPkg_byWorkspaceSubPkgId.put(tablesPkgId, new TreeMap<PackageInfo, Integer>());
	    }

	    Integer assocClasses = EARepositoryUtil.getEAChildPackageByName(rep, workspacePkgId,
		    ArcGISWorkspaceConstants.TEMPLATE_PKG_ASSOCIATION_CLASSES_NAME);
	    if (assocClasses == null) {
		result.addError(this, 102, ArcGISWorkspaceConstants.TEMPLATE_PKG_ASSOCIATION_CLASSES_NAME);
		throw new ShapeChangeAbortException();
	    } else {
		assocClassesPkgId = assocClasses;
		eaPkgIdByModelPkg_byWorkspaceSubPkgId.put(assocClassesPkgId, new TreeMap<PackageInfo, Integer>());
	    }

	    ProcessConfiguration pc = o.getCurrentProcessConfig();

	    // parse map entries
	    List<ProcessMapEntry> mapEntries = pc.getMapEntries();

	    SortedMap<String, ProcessMapEntry> mes = new TreeMap<String, ProcessMapEntry>();

	    for (ProcessMapEntry pme : mapEntries) {
		/*
		 * NOTE: ignores value of 'rule' attribute in map entry, so if there were map
		 * entries for different rules with same 'type' attribute value, this needs to
		 * be updated
		 */
		if (pme.hasTargetType()) {
		    mes.put(pme.getType(), pme);
		} else {
		    result.addError(this, 11, pme.getType());
		}
	    }

	    processMapEntries = mes;

	    // initialize set with esri types suited for numeric range
	    // constraints
	    esriTypesSuitedForRangeConstraint.add("esriFieldTypeInteger");
	    esriTypesSuitedForRangeConstraint.add("esriFieldTypeDouble");

	    // parse numeric range delta parameter
	    String numRangeDeltaParamValue = options.parameter(this.getClass().getName(),
		    ArcGISWorkspaceConstants.PARAM_VALUE_RANGE_DELTA);
	    if (numRangeDeltaParamValue != null) {

		try {

		    double delta = Double.parseDouble(numRangeDeltaParamValue);
		    numRangeDelta = delta;

		} catch (NumberFormatException e) {

		    result.addError(this, 12);
		}
	    }

	    // change the default documentation template?
	    documentationTemplate = options.parameter(this.getClass().getName(),
		    ArcGISWorkspaceConstants.PARAM_DOCUMENTATION_TEMPLATE);
	    documentationNoValue = options.parameter(this.getClass().getName(),
		    ArcGISWorkspaceConstants.PARAM_DOCUMENTATION_NOVALUE);

	    // parse
	    shortNameByTaggedValue = options.parameterAsString(this.getClass().getName(),
		    ArcGISWorkspaceConstants.PARAM_SHORT_NAME_BY_TAGGED_VALUE, "shortName", false, true);

	    keepCaseOfRolename = options.parameterAsBoolean(this.getClass().getName(),
		    ArcGISWorkspaceConstants.PARAM_KEEP_CASE_OF_ROLENAME, false);

	    foreignKeySuffix = options.parameterAsString(this.getClass().getName(),
		    ArcGISWorkspaceConstants.PARAM_FOREIGN_KEY_SUFFIX, "ID", false, true);

	    reflexiveRelationshipAttributeSuffix = options.parameterAsString(this.getClass().getName(),
		    ArcGISWorkspaceConstants.PARAM_REFLEXIVE_REL_FIELD_SUFFIX, "", false, true);

	    List<String> tvsToRepresent = options.parameterAsStringList(null, "representTaggedValues", null, true,
		    true);
	    taggedValuesToRepresent = new TreeSet<>(tvsToRepresent);

	    representTaggedValues = p.matches(ArcGISWorkspaceConstants.RULE_ALL_REPRESENT_TAGGED_VALUES);
	}
    }

    public void process(ClassInfo ci) {

	/*
	 * if a map entry provides a mapping for this class to an esri field type, we
	 * can ignore it
	 */
	if (processMapEntries.containsKey(ci.name())) {

	    ProcessMapEntry pme = processMapEntries.get(ci.name());

	    result.addInfo(this, 240, ci.name(), pme.getTargetType());
	    ignoredCis.add(ci);
	    return;
	}

	// some preprocessing: determine length info from OCL constraints
	parseLengthInfoFromOCLConstraints(ci);

	try {

	    int cat = ci.category();

	    if (isExplicitlyModeledArcGISSubtype(ci)) {

		createExplicitlyModeledArcGISSubtype(ci);

	    } else if (cat == Options.OBJECT
		    || (cat == Options.FEATURE && determineArcGISGeometryType(ci).equals(ArcGISGeometryType.NONE))) {

		parseNumericRangeConstraints(ci);
		createObjectClass(ci);

	    } else if (cat == Options.FEATURE) {

		// create class according to geometry type of feature

		ArcGISGeometryType geomType = determineArcGISGeometryType(ci);

		if (geomType.equals(ArcGISGeometryType.UNKNOWN)) {

		    result.addWarning(this, 210, ci.name());

		    // keep track of ignored classes for better debugging
		    ignoredCis.add(ci);

		} else {

		    if (hasMultipleGeometryProperties(ci)) {

			/*
			 * log warning that first geometry property will be used, the rest ignored
			 */
			result.addWarning(this, 211, ci.name());
		    }

		    parseNumericRangeConstraints(ci);
		    createFeatureClass(ci);
		}

	    } else if (cat == Options.ENUMERATION) {

		if (isSubtypeSetDefinition(ci)) {
		    result.addInfo(this, 254, ci.name());
		} else {
		    createCodedValueDomain(ci);
		}

	    } else if (cat == Options.CODELIST) {

		if (isSubtypeSetDefinition(ci)) {
		    result.addInfo(this, 255, ci.name());
		} else {
		    createCodedValueDomain(ci);
		}

	    } else if (cat == Options.UNION) {

		result.addWarning(this, 209, cat + " (union)", ci.name());

		// keep track of ignored classes for better debugging
		ignoredCis.add(ci);

	    } else if (cat == Options.DATATYPE) {

		result.addWarning(this, 209, cat + " (dataType)", ci.name());

		// keep track of ignored classes for better debugging
		ignoredCis.add(ci);

	    } else if (cat == Options.MIXIN) {

		result.addWarning(this, 209, cat + " (mixin)", ci.name());

		// keep track of ignored classes for better debugging
		ignoredCis.add(ci);

	    } else {

		result.addWarning(this, 209, "" + cat, ci.name());

		// keep track of ignored classes for better debugging
		ignoredCis.add(ci);
	    }
	} catch (EAException e) {
	    result.addError(this, 201, e.getMessage());
	    ignoredCis.add(ci);
	}
    }

    /**
     * @param ci
     * @return <code>true</code> if the given class is the type of a property in the
     *         schemas selected for processing that has tagged value
     *         'arcgisDefaultSubtype' with non-empty value; else <code>false</code>.
     */
    private boolean isSubtypeSetDefinition(ClassInfo ci) {

	if (!ci.matches(ArcGISWorkspaceConstants.RULE_ALL_SUBTYPES)) {
	    return false;
	}

	SortedSet<? extends PropertyInfo> pis = ci.model().selectedSchemaProperties();

	for (PropertyInfo pi : pis) {

	    if ((pi.typeInfo().id.equals(ci.id()) || pi.typeInfo().name.equals(ci.name()))
		    && StringUtils.isNotBlank(pi.taggedValue("arcgisDefaultSubtype"))) {
		return true;
	    }
	}

	return false;
    }

    private void createExplicitlyModeledArcGISSubtype(ClassInfo ci) throws EAException {

	ClassInfo parent = ci.baseClass();

	int eaPkgId;
	if (parent.category() == Options.FEATURE) {
	    eaPkgId = EARepositoryUtil.establishEAPackageHierarchy(parent, featuresPkgId,
		    eaPkgIdByModelPkg_byWorkspaceSubPkgId, rep, numberOfSchemasSelectedForProcessing, author, status);
	} else {
	    eaPkgId = EARepositoryUtil.establishEAPackageHierarchy(ci, tablesPkgId,
		    eaPkgIdByModelPkg_byWorkspaceSubPkgId, rep, numberOfSchemasSelectedForProcessing, author, status);
	}

	// create class element

	/*
	 * 2018-08-16: do NOT normalize or clip subtype name!
	 */

	Element eaElement = EARepositoryUtil.createEAClass(rep, ci.name(), eaPkgId);

	// store mapping between ClassInfo and EA Element
	elementIdByClassInfo.put(ci, eaElement.GetElementID());
	elementNameByClassInfo.put(ci, ci.name());

	setAuthorAndStatus(eaElement);

	// set alias, notes, abstractness
	setCommonItems(ci, eaElement);

	/*
	 * set stereotype of EA element to <<Subtype>> (with correct MDG ID:
	 * ArcGIS::Subtype)
	 */
	EAElementUtil.setEAStereotypeEx(eaElement, "ArcGIS::Subtype");

	/*
	 * create the SubtypeCode tagged value (from tagged value arcgisSubtypeCode)
	 */
	String arcgisSubtypeCode = ci.taggedValue("arcgisSubtypeCode");
	int subtypeCode = -1;
	if (StringUtils.isBlank(arcgisSubtypeCode)) {
	    MessageContext mc = result.addError(this, 252, ci.name());
	    if (mc != null) {
		mc.addDetail(this, -1, ci.fullNameInSchema());
	    }
	} else {
	    try {
		subtypeCode = Integer.parseInt(arcgisSubtypeCode);
	    } catch (Exception e) {
		MessageContext mc = result.addError(this, 253, ci.name(), arcgisSubtypeCode);
		if (mc != null) {
		    mc.addDetail(this, -1, ci.fullNameInSchema());
		}
	    }
	}
	EAElementUtil.updateTaggedValue(eaElement, "SubtypeCode", "" + subtypeCode, false);

	/*
	 * note the subtype for creation of generalization with correct stereotype
	 */
	arcgisSubtypes.add(ci);

	/*
	 * properties cannot be processed now, because we do not necessarily have EA
	 * Elements for all classes in the model yet; thus the type of a property cannot
	 * be set using the GUID of the element - and thus the class would not be linked
	 * to correctly
	 */

	// keep track of generalizations
	identifyGeneralisationRelationships(ci);
    }

    private void setAuthorAndStatus(Element e) throws EAException {

	if (StringUtils.isNotBlank(author)) {
	    EAElementUtil.setEAAuthor(e, author);
	}

	if (StringUtils.isNotBlank(status)) {
	    EAElementUtil.setEAStatus(e, status);
	}
    }

    private void parseNumericRangeConstraints(ClassInfo ci) {

	for (Constraint cons : ci.constraints()) {

	    String ocl = cons.text();

	    boolean lowerBoundaryInclusive = true;
	    boolean upperBoundaryInclusive = true;
	    Double lowerBoundaryValue = ArcGISWorkspaceConstants.DEFAULT_NUM_RANGE_MIN_LOWER_BOUNDARY;
	    Double upperBoundaryValue = ArcGISWorkspaceConstants.DEFAULT_NUM_RANGE_MAX_UPPER_BOUNDARY;

	    Matcher matcher = numRangeConstraintLowerBoundaryPattern.matcher(ocl);

	    boolean foundLowerBoundary = matcher.find();

	    if (foundLowerBoundary) {

		String lbOperator = matcher.group(1);
		String lbValue = matcher.group(2);

		if (lbOperator.equals(">")) {
		    lowerBoundaryInclusive = false;
		}

		try {
		    lowerBoundaryValue = Double.parseDouble(lbValue);
		} catch (NumberFormatException e) {
		    result.addWarning(this, 231, lbValue, ci.name(), cons.name(), ocl);
		}

	    } else {
		// no problem - OCL constraint can be of other
		// constraint type
	    }

	    matcher = numRangeConstraintUpperBoundaryPattern.matcher(ocl);

	    boolean foundUpperBoundary = matcher.find();

	    if (foundUpperBoundary) {

		String ubOperator = matcher.group(1);
		String ubValue = matcher.group(2);

		if (ubOperator.equals("<")) {
		    upperBoundaryInclusive = false;
		}

		try {
		    upperBoundaryValue = Double.parseDouble(ubValue);
		} catch (NumberFormatException e) {
		    result.addWarning(this, 232, ubValue, ci.name(), cons.name(), ocl);
		}

	    } else {
		// no problem - OCL constraint can be of other
		// constraint type
	    }

	    if (foundLowerBoundary || foundUpperBoundary) {

		matcher = numRangeConstraintPropertyNamePattern.matcher(ocl);

		boolean foundPropertyName = matcher.find();

		if (foundPropertyName) {

		    String propertyName = matcher.group(1);

		    // check that detected property actually exists in the
		    // class; NOTE: because flattening can have modified the
		    // property names (merging them with other names, or
		    // replacing them with the alias) we must use a specific
		    // search

		    boolean propertyFound = false;

		    for (PropertyInfo pi : ci.properties().values()) {

			if (pi.name().startsWith(propertyName)
				|| (pi.aliasName() != null && pi.aliasName().startsWith(propertyName))) {
			    propertyFound = true;
			    break;
			}
		    }

		    if (!propertyFound) {
			result.addWarning(this, 229, propertyName, ci.name(), ocl);
		    }

		    // keep track of the numeric range information

		    NumericRangeConstraintMetadata nrcm = new NumericRangeConstraintMetadata(lowerBoundaryValue,
			    upperBoundaryValue, lowerBoundaryInclusive, upperBoundaryInclusive);

		    SortedMap<String, NumericRangeConstraintMetadata> map;

		    if (numericRangeConstraintByPropNameByClassName.containsKey(ci)) {
			// fine, we don't need to initialize the map for the
			// class
			map = numericRangeConstraintByPropNameByClassName.get(ci);
		    } else {

			map = new TreeMap<String, NumericRangeConstraintMetadata>();

			numericRangeConstraintByPropNameByClassName.put(ci, map);
		    }

		    map.put(propertyName, nrcm);

		} else {

		    /*
		     * this is unexpected, as we expect to be able to determine the property name
		     * for a numeric range constraint (which we appear to have detected, because the
		     * regular expression to detect lower/upper boundary found something
		     */

		    result.addWarning(this, 228, ci.name(), ocl);
		}
	    }
	}

	if (ci.matches(ArcGISWorkspaceConstants.RULE_CLS_RANGE_DOMAIN_FROM_TAGGED_VALUES)) {

	    for (PropertyInfo pi : ci.properties().values()) {

		Double lowerBoundaryValue = ArcGISWorkspaceConstants.DEFAULT_NUM_RANGE_MIN_LOWER_BOUNDARY;
		Double upperBoundaryValue = ArcGISWorkspaceConstants.DEFAULT_NUM_RANGE_MAX_UPPER_BOUNDARY;

		boolean foundLowerBoundary = false;
		boolean foundUpperBoundary = false;

		String TV_RANGE_MIN = "rangeMinimum";
		String TV_RANGE_MAX = "rangeMaximum";

		String rMin = pi.taggedValue(TV_RANGE_MIN);
		String rMax = pi.taggedValue(TV_RANGE_MAX);

		if (StringUtils.isNotBlank(rMin)) {

		    try {
			lowerBoundaryValue = Double.parseDouble(rMin.trim());
			foundLowerBoundary = true;
		    } catch (NumberFormatException e) {
			MessageContext mc = result.addWarning(this, 241, rMin.trim(), TV_RANGE_MIN);
			mc.addDetail(this, 20001, pi.fullNameInSchema());
		    }
		}

		if (StringUtils.isNotBlank(rMax)) {

		    try {
			upperBoundaryValue = Double.parseDouble(rMax.trim());
			foundUpperBoundary = true;
		    } catch (NumberFormatException e) {
			MessageContext mc = result.addWarning(this, 242, rMax.trim(), TV_RANGE_MAX);
			mc.addDetail(this, 20001, pi.fullNameInSchema());
		    }
		}

		if (foundLowerBoundary || foundUpperBoundary) {

		    // keep track of the numeric range information

		    NumericRangeConstraintMetadata nrcm = new NumericRangeConstraintMetadata(lowerBoundaryValue,
			    upperBoundaryValue, true, true);

		    SortedMap<String, NumericRangeConstraintMetadata> map;

		    if (numericRangeConstraintByPropNameByClassName.containsKey(ci)) {
			// fine, we don't need to initialize the map for the
			// class
			map = numericRangeConstraintByPropNameByClassName.get(ci);
		    } else {

			map = new TreeMap<String, NumericRangeConstraintMetadata>();

			numericRangeConstraintByPropNameByClassName.put(ci, map);
		    }

		    map.put(pi.name(), nrcm);
		}
	    }
	}
    }

    private void parseLengthInfoFromOCLConstraints(ClassInfo ci) {

	if ((ci.category() == Options.FEATURE || ci.category() == Options.OBJECT
		|| ci.category() == Options.GMLOBJECT)) {

	    for (Constraint cons : ci.constraints()) {

		String ocl = cons.text();

		Matcher matcher = lengthConstraintPattern.matcher(ocl);

		boolean found = matcher.find();

		if (found) {

		    String propertyName = matcher.group(1);
		    String length = matcher.group(2);

		    Integer i = Integer.valueOf(length);

		    lengthByClassPropName.put(ci.name() + "_" + propertyName, i);

		} else {
		    // no problem - OCL constraint can be of other
		    // constraint type
		}
	    }
	}
    }

    private Element createCodedValueDomain(ClassInfo ci) throws EAException {

	int eaPkgId = EARepositoryUtil.establishEAPackageHierarchy(ci, domainsPkgId,
		eaPkgIdByModelPkg_byWorkspaceSubPkgId, rep, numberOfSchemasSelectedForProcessing, author, status);

	// create class element
	String name = normalizeName(ci.name());

	if (exceedsMaxLength(name)) {
	    this.result.addWarning(this, 205, name, ci.name(), ci.name(), "" + maxNameLength);
	    name = clipToMaxLength(name);
	}

	Element e = EARepositoryUtil.createEAClass(rep, name, eaPkgId);

	// store mapping between ClassInfo and EA Element
	int elementID = e.GetElementID();
	elementIdByClassInfo.put(ci, elementID);
	elementNameByClassInfo.put(ci, name);

	/*
	 * Keep track of element IDs of coded value domains, so that unused ones can be
	 * removed during postprocessing.
	 */
	elementIdsOfUnusedCodedValueDomain.add(elementID);

	setAuthorAndStatus(e);

	// set alias, notes, abstractness
	setCommonItems(ci, e);

	EAElementUtil.setEAStereotypeEx(e, "ArcGIS::CodedValueDomain");

	String documentation = ci.derivedDocumentation(documentationTemplate, "<no description available>");

	EAElementUtil.setTaggedValue(e, new EATaggedValue("Description", documentation, true));

	String fieldType = determineFieldTypeForCodedValueDomain(ci);

	createRequiredFieldsOfCodedValueDomain(e, fieldType);

	// for each enum/code of the class, create an according property
	if (ci.properties() != null) {

	    SortedSet<String> enumStereotype = new TreeSet<String>();
	    enumStereotype.add(ArcGISWorkspaceConstants.STEREOTYPE_DOMAIN_CODED_VALUE);

	    for (PropertyInfo pi : ci.properties().values()) {

		String initialValue = determineInitialValueForDomainCodedValue(pi);

		Attribute eaAtt = EAElementUtil.createEAAttribute(e, pi.name(), null,
			pi.derivedDocumentation(documentationTemplate, documentationNoValue), enumStereotype, null,
			false, false, false, initialValue, false, new Multiplicity(1, 1), null, null);

		addTaggedValuesToRepresent(eaAtt, pi);
	    }
	}

	return e;
    }

    private Element createSubtypeSpecificCodedValueDomain(String subtypeName, ClassInfo codeListOrEnumeration,
	    SortedSet<PropertyInfo> codes) throws EAException {

	int eaPkgId = EARepositoryUtil.establishEAPackageHierarchy(codeListOrEnumeration, domainsPkgId,
		eaPkgIdByModelPkg_byWorkspaceSubPkgId, rep, numberOfSchemasSelectedForProcessing, author, status);

	String fullName = codeListOrEnumeration.name() + subtypeName;

	// create class element
	String name = normalizeName(fullName);

	if (exceedsMaxLength(name)) {
	    this.result.addWarning(this, 205, name, fullName, subtypeName, "" + maxNameLength);
	    name = clipToMaxLength(name);
	}

	Element e = EARepositoryUtil.createEAClass(rep, name, eaPkgId);

	setAuthorAndStatus(e);

	// set alias, notes, abstractness
	// setCommonItems(codeListOrEnumeration, e);

	EAElementUtil.setEAStereotypeEx(e, "ArcGIS::CodedValueDomain");

	// String documentation =
	// codeListOrEnumeration.derivedDocumentation(documentationTemplate,
	// "<no description available>");

	// EAElementUtil.setTaggedValue(e,
	// new EATaggedValue("Description", documentation, true));

	String fieldType = determineFieldTypeForCodedValueDomain(codeListOrEnumeration);

	createRequiredFieldsOfCodedValueDomain(e, fieldType);

	// for each enum/code for the subtype, create an according property

	SortedSet<String> enumStereotype = new TreeSet<String>();
	enumStereotype.add(ArcGISWorkspaceConstants.STEREOTYPE_DOMAIN_CODED_VALUE);

	for (PropertyInfo pi : codes) {

	    String initialValue = determineInitialValueForDomainCodedValue(pi);

	    Attribute eaAtt = EAElementUtil.createEAAttribute(e, pi.name(), null,
		    pi.derivedDocumentation(documentationTemplate, documentationNoValue), enumStereotype, null, false,
		    false, false, initialValue, false, new Multiplicity(1, 1), null, null);

	    addTaggedValuesToRepresent(eaAtt, pi);
	}

	return e;
    }

    private String determineInitialValueForDomainCodedValue(PropertyInfo pi) {
	String initialValue;
	if (pi.matches(ArcGISWorkspaceConstants.RULE_ENUM_INITIAL_VALUE_BY_ALIAS)) {
	    initialValue = pi.aliasName();
	} else {
	    initialValue = pi.initialValue();
	}

	if (StringUtils.isBlank(initialValue)) {
	    initialValue = pi.name();
	}
	return initialValue;
    }

    private void createRequiredFieldsOfCodedValueDomain(Element eaElement, String fieldType) throws EAException {

	EAElementUtil.createEAAttribute(eaElement, "FieldType", null, null, null, null, false, false, false, fieldType,
		false, new Multiplicity(1, 1), "esriFieldType", null);

	EAElementUtil.createEAAttribute(eaElement, "MergePolicy", null, null, null, null, false, false, false,
		"esriMPTDefaultValue", false, new Multiplicity(1, 1), "esriMergePolicyType", null);

	EAElementUtil.createEAAttribute(eaElement, "SplitPolicy", null, null, null, null, false, false, false,
		"esriSPTDuplicate", false, new Multiplicity(1, 1), "esriSplitPolicyType", null);
    }

    private String determineFieldTypeForCodedValueDomain(ClassInfo codeListOrEnumeration) {

	String fieldType = "esriFieldTypeString";

	if (isNumericallyValued(codeListOrEnumeration)) {
	    String numericFieldConceptualType = codeListOrEnumeration
		    .taggedValue(ArcGISWorkspaceConstants.TV_NUMERIC_TYPE).trim();
	    if (processMapEntries.containsKey(numericFieldConceptualType)) {
		// use target type defined by map entry for the conceptual type
		fieldType = processMapEntries.get(numericFieldConceptualType).getTargetType();
	    } else {
		// log error and keep fieldType as is
		MessageContext mc = result.addError(this, 245, numericFieldConceptualType);
		if (mc != null) {
		    mc.addDetail(this, -1, codeListOrEnumeration.fullNameInSchema());
		}
	    }
	}
	String fieldTypeTV = codeListOrEnumeration.taggedValue(ArcGISWorkspaceConstants.TV_FIELD_TYPE);
	if (StringUtils.isNotBlank(fieldTypeTV)) {
	    fieldType = fieldTypeTV.trim();
	}

	return fieldType;
    }

    private Element createRangeDomain(String rangeDomainName, String rangeDomainDocumentation, String esriFieldType,
	    ClassInfo rangeDomainSource) throws EAException {

	int eaPkgId = EARepositoryUtil.establishEAPackageHierarchy(rangeDomainSource, domainsPkgId,
		eaPkgIdByModelPkg_byWorkspaceSubPkgId, rep, numberOfSchemasSelectedForProcessing, author, status);

	// create class element
	String name = rangeDomainName;

	// TBD - think of naming scheme for range domains
	// String name = normalizeName(rangeDomainName);
	//
	// if (exceedsMaxLength(name)) {
	// this.result.addWarning(this, 205, name, rangeDomainName,
	// rangeDomainName, ""
	// + MAX_NAME_LENGTH);
	// name = clipToMaxLength(name);
	// }

	Element e = EARepositoryUtil.createEAClass(rep, name, eaPkgId);

	// store mapping between class name and EA Element
	numericRangeElementIdsByClassName.put(rangeDomainName, e.GetElementID());

	setAuthorAndStatus(e);

	// TBD: set alias or notes?

	EAElementUtil.setEAStereotypeEx(e, "ArcGIS::RangeDomain");

	EAElementUtil.setTaggedValue(e, new EATaggedValue("Description",
		rangeDomainDocumentation == null ? "<no description available>" : rangeDomainDocumentation, true));

	// create required properties: FieldType, MergePolicy, SplitPolicy
	EAElementUtil.createEAAttribute(e, "FieldType", null, null, null, null, false, false, false, esriFieldType,
		false, new Multiplicity(1, 1), "esriFieldType", null);

	EAElementUtil.createEAAttribute(e, "MergePolicy", null, null, null, null, false, false, false,
		"esriMPTDefaultValue", false, new Multiplicity(1, 1), "esriMergePolicyType", null);

	EAElementUtil.createEAAttribute(e, "SplitPolicy", null, null, null, null, false, false, false,
		"esriSPTDuplicate", false, new Multiplicity(1, 1), "esriSplitPolicyType", null);

	return e;
    }

    private void createFeatureClass(ClassInfo ci) throws EAException {

	checkRequirements(ci);

	int eaPkgId = EARepositoryUtil.establishEAPackageHierarchy(ci, featuresPkgId,
		eaPkgIdByModelPkg_byWorkspaceSubPkgId, rep, numberOfSchemasSelectedForProcessing, author, status);

	// create class element
	String name = normalizeName(ci.name());

	if (exceedsMaxLength(name)) {
	    this.result.addWarning(this, 205, name, ci.name(), ci.name(), "" + maxNameLength);
	    name = clipToMaxLength(name);
	}

	Element e = EARepositoryUtil.createEAClass(rep, name, eaPkgId);

	// store mapping between ClassInfo and EA Element
	elementIdByClassInfo.put(ci, e.GetElementID());
	elementNameByClassInfo.put(ci, name);

	setAuthorAndStatus(e);

	// set alias, notes, abstractness
	setCommonItems(ci, e);

	if (!ci.isAbstract()) {

	    // identify stereotype to use
	    ArcGISGeometryType geomType = determineArcGISGeometryType(ci);
	    EAElementUtil.setEAStereotypeEx(e, geomType.stereotypeValue());

	    // create ArcGIS System Fields

	    Attribute objectIdField = createSystemFieldOBJECTID(e);
	    objectIdAttributeGUIDByClass.put(ci, objectIdField.GetAttributeGUID());

	    Attribute shapeField = createSystemFieldShape(e);

	    Attribute shapeAreaField = null;
	    Attribute shapeLengthField = null;

	    if (geomType.equals(ArcGISGeometryType.POLYLINE) || geomType.equals(ArcGISGeometryType.POLYGON)) {

		shapeLengthField = createSystemFieldShapeLength(e);

		if (geomType.equals(ArcGISGeometryType.POLYGON)) {

		    shapeAreaField = createSystemFieldShapeArea(e);
		}
	    }

	    createSystemFieldOBJECTIDIDX(e);
	    createSystemFieldShapeIDX(e);

	    // now update tagged values for feature class

	    if (geomType.equals(ArcGISGeometryType.POINT)) {
		EAElementUtil.updateTaggedValue(e, "AncillaryRole", "esriNCARNone", false);
	    }

	    if (geomType.equals(ArcGISGeometryType.POLYGON)) {
		EAElementUtil.updateTaggedValue(e, "AreaFieldName", shapeAreaField.GetAttributeGUID(), false);
	    } else {
		EAElementUtil.updateTaggedValue(e, "AreaFieldName", "", false);
	    }

	    EAElementUtil.updateTaggedValue(e, "CanVersion", "false", false);
	    EAElementUtil.updateTaggedValue(e, "DSID", "", false);
	    EAElementUtil.updateTaggedValue(e, "FeatureType", "esriFTSimple", false);
	    EAElementUtil.updateTaggedValue(e, "GlobalIDFieldName", "", false);

	    String hasM = "false";
	    if (ci.matches(ArcGISWorkspaceConstants.RULE_CLS_HASM)) {
		String hasMFromTV = ci.taggedValue("HasM");
		if (StringUtils.isNotBlank(hasMFromTV) && hasMFromTV.trim().equalsIgnoreCase("true")) {
		    hasM = "true";
		}
	    }
	    EAElementUtil.updateTaggedValue(e, "HasM", hasM, false);

	    EAElementUtil.updateTaggedValue(e, "HasSpatialIndex", "true", false);

	    String hasZ = "false";
	    if (ci.matches(ArcGISWorkspaceConstants.RULE_CLS_HASZ)) {
		String hasZFromTV = ci.taggedValue("HasZ");
		if (StringUtils.isNotBlank(hasZFromTV) && hasZFromTV.trim().equalsIgnoreCase("true")) {
		    hasZ = "true";
		}
	    }
	    EAElementUtil.updateTaggedValue(e, "HasZ", hasZ, false);

	    if (geomType.equals(ArcGISGeometryType.POLYGON) || geomType.equals(ArcGISGeometryType.POLYLINE)) {
		EAElementUtil.updateTaggedValue(e, "LengthFieldName", shapeLengthField.GetAttributeGUID(), false);
	    } else {
		EAElementUtil.updateTaggedValue(e, "LengthFieldName", "", false);
	    }

	    EAElementUtil.updateTaggedValue(e, "Metadata", "", true);
	    EAElementUtil.updateTaggedValue(e, "ModelName", "", false);
	    EAElementUtil.updateTaggedValue(e, "OIDFieldName", objectIdField.GetAttributeGUID(), false);
	    EAElementUtil.updateTaggedValue(e, "RasterFieldName", "", false);
	    EAElementUtil.updateTaggedValue(e, "ShapeFieldName", shapeField.GetAttributeGUID(), false);
	    EAElementUtil.updateTaggedValue(e, "SpatialReference", "", false);
	    EAElementUtil.updateTaggedValue(e, "Versioned", "false", false);
	}

	// properties cannot be processed now, because we do not necessarily
	// have EA Elements for all classes in the model yet; thus the type of a
	// property (with value type being a <<type>> or <<featureType>> cannot
	// be set using the GUID of the element - and thus the class would not
	// be linked to correctly

	// keep track of generalizations
	identifyGeneralisationRelationships(ci);

	handleArcGISSubtypesDefinedByFieldType(ci, eaPkgId, e);
    }

    private boolean isExplicitlyModeledArcGISSubtype(ClassInfo ci) {

	if (ci.matches(ArcGISWorkspaceConstants.RULE_ALL_SUBTYPES) && ArcGISUtil.isArcGISSubtype(ci)) {
	    return true;
	} else {
	    return false;
	}
    }

    @SuppressWarnings("unchecked")
    private void handleArcGISSubtypesDefinedByFieldType(ClassInfo parent, int eaPkgId, Element parentEAElement)
	    throws EAException {

	if (!parent.matches(ArcGISWorkspaceConstants.RULE_ALL_SUBTYPES)) {
	    return;
	}

	// determine if one of the properties defines a subtype set
	ClassInfo subtypeSetCi = null;
	String arcgisDefaultSubtypeTV = null;

	for (PropertyInfo pi : parent.propertiesAll()) {

	    arcgisDefaultSubtypeTV = pi.taggedValue("arcgisDefaultSubtype");
	    if (StringUtils.isNotBlank(arcgisDefaultSubtypeTV)) {
		ClassInfo typeCi = parent.model().classByIdOrName(pi.typeInfo());
		if (typeCi != null
			&& (typeCi.category() == Options.ENUMERATION || typeCi.category() == Options.CODELIST)) {
		    subtypeSetCi = typeCi;
		    break;
		}
	    }
	}

	if (subtypeSetCi == null) {
	    return;
	}

	// just check that the defaultSubtype is an integer
	try {
	    Integer.parseInt(arcgisDefaultSubtypeTV.trim());
	} catch (Exception e) {
	    MessageContext mc = result.addError(this, 256, arcgisDefaultSubtypeTV.trim());
	    if (mc != null) {
		mc.addDetail(this, -1, parent.fullNameInSchema());
	    }
	}

	SortedMap<String, Integer> subtypeCodeBySubtypeName = new TreeMap<>();

	if (subtypeSetCi.properties().isEmpty()) {
	    result.addError(this, 257, subtypeSetCi.name());
	    return;
	} else {

	    for (PropertyInfo pi : subtypeSetCi.properties().values()) {
		String subtypeCodeTV = pi.taggedValue("arcgisSubtypeCode");
		int subtypeCode = -1;
		if (StringUtils.isBlank(subtypeCodeTV)) {
		    MessageContext mc = result.addError(this, 258, pi.name(), subtypeSetCi.name());
		    if (mc != null) {
			mc.addDetail(this, -2, pi.fullNameInSchema());
		    }
		    continue;
		} else {
		    try {
			subtypeCode = Integer.parseInt(subtypeCodeTV);
		    } catch (Exception e) {
			MessageContext mc = result.addError(this, 259, pi.name(), subtypeSetCi.name(), subtypeCodeTV);
			if (mc != null) {
			    mc.addDetail(this, -2, pi.fullNameInSchema());
			}
			continue;
		    }
		}

		/*
		 * Check for duplicate code. Duplicate subtype name should already be identified
		 * while loading the model.
		 */
		if (subtypeCodeBySubtypeName.containsValue(subtypeCode)) {
		    MessageContext mc = result.addError(this, 260, "" + subtypeCode, subtypeSetCi.name(), pi.name());
		    if (mc != null) {
			mc.addDetail(this, -1, subtypeSetCi.fullNameInSchema());
		    }
		} else {
		    subtypeCodeBySubtypeName.put(pi.name(), subtypeCode);
		}
	    }
	}

	// create EA elements for the subtypes
	for (Entry<String, Integer> stDef : subtypeCodeBySubtypeName.entrySet()) {

	    String subtypeName = stDef.getKey();
	    int subtypeCode = stDef.getValue();

	    // create class element

	    /*
	     * NOTE: no normalization or clipping of subtype name
	     */

	    Element subtypeEAElement = EARepositoryUtil.createEAClass(rep, subtypeName, eaPkgId);

	    setAuthorAndStatus(subtypeEAElement);

	    // store mapping for subtype element id
	    int subtypeEAElementID = subtypeEAElement.GetElementID();
	    Map<String, Integer> subtypeElementIdBySubtypeName = null;
	    if (subtypeElementIdBySubtypeNameByParent.containsKey(parent)) {
		subtypeElementIdBySubtypeName = subtypeElementIdBySubtypeNameByParent.get(parent);
	    } else {
		subtypeElementIdBySubtypeName = new TreeMap<>();
		subtypeElementIdBySubtypeNameByParent.put(parent, subtypeElementIdBySubtypeName);
	    }
	    subtypeElementIdBySubtypeName.put(subtypeName, subtypeEAElementID);

	    /*
	     * set stereotype of subtype EA element to <<Subtype>> (with correct MDG ID:
	     * ArcGIS::Subtype)
	     */
	    EAElementUtil.setEAStereotypeEx(subtypeEAElement, "ArcGIS::Subtype");

	    /*
	     * create the SubtypeCode tagged value
	     */
	    EAElementUtil.updateTaggedValue(subtypeEAElement, "SubtypeCode", "" + subtypeCode, false);

	    /* Create the <<Subtype>> generalization relationship */
	    String c1Name = subtypeName;
	    String c2Name = elementNameByClassInfo.get(parent);

	    try {
		Connector con = EARepositoryUtil.createEAGeneralization(rep, subtypeEAElementID, c1Name,
			elementIdByClassInfo.get(parent), c2Name);
		EAConnectorUtil.setEAStereotypeEx(con, "ArcGIS::Subtype");
	    } catch (EAException e) {
		result.addWarning(this, 10006, c1Name, c2Name, e.getMessage());
	    }
	}

	/*
	 * determine if specific coded value domains are needed - if so, create them
	 */
	for (PropertyInfo parentPi : parent.properties().values()) {

	    if (parentPi.categoryOfValue() == Options.CODELIST || parentPi.categoryOfValue() == Options.ENUMERATION) {

		// get the codelist/enumeration
		ClassInfo clOrEnum = parent.model().classByIdOrName(parentPi.typeInfo());

		if (clOrEnum == null) {
		    MessageContext mc = result.addError(this, 261, parent.name(), parentPi.name(),
			    parentPi.typeInfo().name);
		    if (mc != null) {
			mc.addDetail(this, -2, parentPi.fullNameInSchema());
		    }
		    continue;
		}

		// identify which codes/enums specifically apply to which
		// subtypes
		SortedMap<String, SortedSet<PropertyInfo>> codesBySubtypeName = new TreeMap<>();

		for (PropertyInfo codePi : clOrEnum.properties().values()) {

		    String arcgisUsedBySubtypes = codePi.taggedValue("arcgisUsedBySubtypes");

		    if (StringUtils.isNotBlank(arcgisUsedBySubtypes)) {

			String[] subtypesThatUseTheCode_escaped = arcgisUsedBySubtypes
				.split(REGEX_TO_SPLIT_BY_COMMA_WITH_ESCAPING);

			for (String s : subtypesThatUseTheCode_escaped) {
			    if (StringUtils.isNotBlank(s)) {
				/*
				 * Unescape ',' and '\'.
				 */
				String unescapedSubtypeName = s.replace("\\,", ",").replace("\\\\", "\\");

				SortedSet<PropertyInfo> codesOfSubtype = null;
				if (codesBySubtypeName.containsKey(unescapedSubtypeName)) {
				    codesOfSubtype = codesBySubtypeName.get(unescapedSubtypeName);
				} else {
				    codesOfSubtype = new TreeSet<PropertyInfo>();
				    codesBySubtypeName.put(unescapedSubtypeName, codesOfSubtype);
				}
				codesOfSubtype.add(codePi);
			    }
			}
		    }
		}

		/*
		 * Create coded value domains as necessary
		 */
		for (Entry<String, SortedSet<PropertyInfo>> e : codesBySubtypeName.entrySet()) {

		    String subtypeName = e.getKey();

		    Element eaElementOfSubtypeSpecificCodedValueDomain = createSubtypeSpecificCodedValueDomain(
			    subtypeName, clOrEnum, e.getValue());

		    String documentation = clOrEnum.derivedDocumentation(documentationTemplate,
			    "<no description available>");

		    EAElementUtil.setTaggedValue(eaElementOfSubtypeSpecificCodedValueDomain,
			    new EATaggedValue("Description", subtypeName + ": " + documentation, true));

		    SortedMap<String, Integer> subtypeSpecificCodedValueDomainEAIDBySubtypeName = null;
		    if (subtypeCodedValueDomainEAIDByMultiKey.containsKey(parent, parentPi)) {
			subtypeSpecificCodedValueDomainEAIDBySubtypeName = (SortedMap<String, Integer>) subtypeCodedValueDomainEAIDByMultiKey
				.get(parent, parentPi);
		    } else {
			subtypeSpecificCodedValueDomainEAIDBySubtypeName = new TreeMap<>();
			subtypeCodedValueDomainEAIDByMultiKey.put(parent, parentPi,
				subtypeSpecificCodedValueDomainEAIDBySubtypeName);
		    }

		    subtypeSpecificCodedValueDomainEAIDBySubtypeName.put(subtypeName,
			    eaElementOfSubtypeSpecificCodedValueDomain.GetElementID());
		}
	    }
	}

	/*
	 * NOTE: since some coded value domain may not be specifically created for the
	 * subtype, but instead be encoded from a code list or enumeration, and used by
	 * the subtype (e.g. with specific default value), we cannot create the fields
	 * of subtypes here.
	 */

    }

    private Attribute createSystemFieldShapeIDX(Element e) throws EAException {

	List<EATaggedValue> tvs = new ArrayList<EATaggedValue>();

	tvs.add(new EATaggedValue("IsUnique", "true"));
	tvs.add(new EATaggedValue("IsAscending", "true"));

	SortedSet<String> stereotypes = new TreeSet<String>();
	stereotypes.add("SpatialIndex");

	return EAElementUtil.createEAAttribute(e, "Shape_IDX", null, null, stereotypes, tvs, false, false, false, null,
		false, new Multiplicity(1, 1), "", null);
    }

    private Attribute createSystemFieldOBJECTIDIDX(Element e) throws EAException {

	List<EATaggedValue> tvs = new ArrayList<EATaggedValue>();

	tvs.add(new EATaggedValue("IsUnique", "true"));
	tvs.add(new EATaggedValue("IsAscending", "true"));
	tvs.add(new EATaggedValue("Fields", ""));

	SortedSet<String> stereotypes = new TreeSet<String>();
	stereotypes.add("AttributeIndex");

	return EAElementUtil.createEAAttribute(e, "OBJECTID_IDX", null, null, stereotypes, tvs, false, false, false,
		null, false, new Multiplicity(1, 1), "", null);
    }

    private Attribute createSystemFieldShapeArea(Element e) throws EAException {

	List<EATaggedValue> tvs = new ArrayList<EATaggedValue>();

	tvs.add(new EATaggedValue("DomainFixed", "false"));
	tvs.add(new EATaggedValue("Editable", "false"));
	tvs.add(new EATaggedValue("GeometryDef", "", true));
	tvs.add(new EATaggedValue("IsNullable", "true"));
	tvs.add(new EATaggedValue("Length", "8"));
	tvs.add(new EATaggedValue("ModelName", ""));
	tvs.add(new EATaggedValue("Precision", "0"));
	tvs.add(new EATaggedValue("RasterDef", "", true));
	tvs.add(new EATaggedValue("Required", "true"));
	tvs.add(new EATaggedValue("Scale", "0"));

	SortedSet<String> stereotypes = new TreeSet<String>();
	stereotypes.add("RequiredField");

	return EAElementUtil.createEAAttribute(e, "Shape_Area", null, null, stereotypes, tvs, false, false, false, null,
		false, new Multiplicity(1, 1), "esriFieldTypeDouble", null);
    }

    private Attribute createSystemFieldShapeLength(Element e) throws EAException {

	List<EATaggedValue> tvs = new ArrayList<EATaggedValue>();

	tvs.add(new EATaggedValue("DomainFixed", "false"));
	tvs.add(new EATaggedValue("Editable", "false"));
	tvs.add(new EATaggedValue("GeometryDef", "", true));
	tvs.add(new EATaggedValue("IsNullable", "true"));
	tvs.add(new EATaggedValue("Length", "8"));
	tvs.add(new EATaggedValue("ModelName", ""));
	tvs.add(new EATaggedValue("Precision", "0"));
	tvs.add(new EATaggedValue("RasterDef", "", true));
	tvs.add(new EATaggedValue("Required", "true"));
	tvs.add(new EATaggedValue("Scale", "0"));

	SortedSet<String> stereotypes = new TreeSet<String>();
	stereotypes.add("RequiredField");

	return EAElementUtil.createEAAttribute(e, "Shape_Length", null, null, stereotypes, tvs, false, false, false,
		null, false, new Multiplicity(1, 1), "esriFieldTypeDouble", null);
    }

    private Attribute createSystemFieldShape(Element e) throws EAException {

	List<EATaggedValue> tvs = new ArrayList<EATaggedValue>();

	tvs.add(new EATaggedValue("DomainFixed", "true"));
	tvs.add(new EATaggedValue("Editable", "true"));
	tvs.add(new EATaggedValue("GeometryDef",
		"AvgNumPoints=0;" + System.getProperty("line.separator") + "GridSize0=0;", true));
	tvs.add(new EATaggedValue("IsNullable", "true"));
	tvs.add(new EATaggedValue("Length", "0"));
	tvs.add(new EATaggedValue("ModelName", ""));
	tvs.add(new EATaggedValue("Precision", "0"));
	tvs.add(new EATaggedValue("RasterDef", "", true));
	tvs.add(new EATaggedValue("Required", "true"));
	tvs.add(new EATaggedValue("Scale", "0"));

	SortedSet<String> stereotypes = new TreeSet<String>();
	stereotypes.add("RequiredField");

	return EAElementUtil.createEAAttribute(e, "Shape", null, null, stereotypes, tvs, false, false, false, null,
		false, new Multiplicity(1, 1), "esriFieldTypeGeometry", null);
    }

    private Attribute createSystemFieldOBJECTID(Element e) throws EAException {

	List<EATaggedValue> tvs = new ArrayList<EATaggedValue>();

	tvs.add(new EATaggedValue("DomainFixed", "true"));
	tvs.add(new EATaggedValue("Editable", "false"));
	tvs.add(new EATaggedValue("GeometryDef", "", true));
	tvs.add(new EATaggedValue("IsNullable", "false"));
	tvs.add(new EATaggedValue("Length", "4"));
	tvs.add(new EATaggedValue("ModelName", "OBJECTID"));
	tvs.add(new EATaggedValue("Precision", ""));
	tvs.add(new EATaggedValue("RasterDef", "", true));
	tvs.add(new EATaggedValue("Required", "true"));
	tvs.add(new EATaggedValue("Scale", "0"));

	SortedSet<String> stereotypes = new TreeSet<String>();
	stereotypes.add("RequiredField");

	return EAElementUtil.createEAAttribute(e, "OBJECTID", null, null, stereotypes, tvs, false, false, false, null,
		false, new Multiplicity(1, 1), "esriFieldTypeOID", null);
    }

    private void createObjectClass(ClassInfo ci) throws EAException {

	checkRequirements(ci);

	int eaPkgId = EARepositoryUtil.establishEAPackageHierarchy(ci, tablesPkgId,
		eaPkgIdByModelPkg_byWorkspaceSubPkgId, rep, numberOfSchemasSelectedForProcessing, author, status);

	// create class element
	String name = normalizeName(ci.name());

	if (exceedsMaxLength(name)) {
	    this.result.addWarning(this, 205, name, ci.name(), ci.name(), "" + maxNameLength);
	    name = clipToMaxLength(name);
	}

	Element e = EARepositoryUtil.createEAClass(rep, name, eaPkgId);

	// store mapping between ClassInfo and EA Element
	elementIdByClassInfo.put(ci, e.GetElementID());
	elementNameByClassInfo.put(ci, name);

	setAuthorAndStatus(e);

	// set alias, notes, abstractness
	setCommonItems(ci, e);

	EAElementUtil.setEAStereotypeEx(e, "ArcGIS::ObjectClass");

	Attribute objectIdField = createSystemFieldOBJECTID(e);
	objectIdAttributeGUIDByClass.put(ci, objectIdField.GetAttributeGUID());

	createSystemFieldOBJECTIDIDX(e);

	// now update tagged values for feature class

	EAElementUtil.updateTaggedValue(e, "CanVersion", "false", false);
	EAElementUtil.updateTaggedValue(e, "DSID", "", false);
	EAElementUtil.updateTaggedValue(e, "GlobalIDFieldName", "", false);
	EAElementUtil.updateTaggedValue(e, "Metadata", "", true);
	EAElementUtil.updateTaggedValue(e, "ModelName", "", false);
	EAElementUtil.updateTaggedValue(e, "OIDFieldName", objectIdField.GetAttributeGUID(), false);
	EAElementUtil.updateTaggedValue(e, "RasterFieldName", "", false);
	EAElementUtil.updateTaggedValue(e, "Versioned", "false", false);

	// properties cannot be processed now, because we do not necessarily
	// have EA Elements for all classes in the model yet; thus the type of a
	// property (with value type being a <<type>> or <<featureType>> cannot
	// be set using the GUID of the element - and thus the class would not
	// be linked to correctly

	// keep track of generalizations
	identifyGeneralisationRelationships(ci);

	handleArcGISSubtypesDefinedByFieldType(ci, eaPkgId, e);
    }

    private void checkRequirements(ClassInfo ci) {

	/*
	 * If rule for using <<identifier>> stereotype on attributes is enabled, check
	 * that a type does not have more than one such attribute, and that such an
	 * attribute has max cardinality 1.
	 */
	if (ci.matches(ArcGISWorkspaceConstants.RULE_CLS_IDENTIFIER_STEREOTYPE)) {

	    int countIdentifierAttributes = 0;

	    for (PropertyInfo pi : ci.properties().values()) {

		if (pi.isAttribute() && pi.stereotype("identifier")) {

		    countIdentifierAttributes++;

		    if (pi.cardinality().maxOccurs > 1) {
			MessageContext mc = result.addError(this, 247, pi.name());
			if (mc != null) {
			    mc.addDetail(this, -2, pi.fullNameInSchema());
			}
		    }
		}
	    }

	    if (countIdentifierAttributes > 1) {

		MessageContext mc = result.addWarning(this, 246, ci.name());
		if (mc != null) {
		    mc.addDetail(this, -1, ci.fullNameInSchema());
		}

	    } else if (countIdentifierAttributes == 0) {

		MessageContext mc = result.addWarning(this, 248, ci.name());
		if (mc != null) {
		    mc.addDetail(this, -1, ci.fullNameInSchema());
		}
	    }
	}
    }

    private void identifyGeneralisationRelationships(ClassInfo ci) {

	/*
	 * The target currently only supports one generalization relationship per
	 * subtype (the key of the generalisations map is the subtype). Thus, if we
	 * encounter a class that has more than one supertype, this is an error (since
	 * the result would be unexpected).
	 */
	if (ci.supertypes().size() > 1) {
	    MessageContext mc = result.addError(this, 268, ci.name());
	    if (mc != null) {
		mc.addDetail(this, -1, ci.fullNameInSchema());
	    }
	}

	for (String tid : ci.supertypes()) {

	    ClassInfo cix = model.classById(tid);

	    if (cix == null) {
		result.addError(this, 206, ci.name(), tid);
	    } else {
		generalisations.put(ci, cix);
	    }
	}

	for (String tid : ci.subtypes()) {

	    ClassInfo cix = model.classById(tid);

	    if (cix == null) {
		result.addError(this, 202, ci.name(), tid);
	    } else {
		generalisations.put(cix, ci);
	    }
	}
    }

    private String normalizeName(String name) {

	String result = name.replaceAll(ArcGISWorkspaceConstants.ILLEGAL_NAME_CHARACTERS_DETECTION_REGEX, "_");

	return result;
    }

    private String normalizeAlias(String alias, ClassInfo ci) {

	// TBD: figure out the restrictions on aliases

	// String result =
	// alias.replaceAll(ILLEGAL_ALIAS_CHARACTERS_DETECTION_REGEX, "_");
	String result = alias;

	// if (exceedsMaxLength(result)) {
	//
	// this.result.addWarning(this, 205, result, alias, ci.name(), ""
	// + MAX_ALIAS_LENGTH);
	// result = clipToMaxLength(result);
	// }

	return result;
    }

    private boolean exceedsMaxLength(String s) {

	if (s == null) {

	    return false;

	} else if (s.length() <= maxNameLength) {

	    return false;

	} else {

	    return true;
	}
    }

    private String clipToMaxLength(String s) {

	if (s == null) {

	    return null;

	} else if (s.length() <= maxNameLength) {

	    return s;

	} else {

	    return s.substring(0, maxNameLength);
	}
    }

    /**
     * Sets abstractness, alias, documentation, and linked documentation.
     * 
     * @param ci
     * @param e
     */
    private void setCommonItems(ClassInfo ci, Element e) {

	if (ci.isAbstract()) {
	    try {
		EAElementUtil.setEAAbstract(e, true);
	    } catch (EAException exc) {
		result.addError(this, 204, ci.name(), exc.getMessage());
	    }
	}

	if (ci.aliasName() != null) {
	    try {

		String aliasName = normalizeAlias(ci.aliasName(), ci);
		EAElementUtil.setEAAlias(e, aliasName);
	    } catch (EAException exc) {
		result.addError(this, 204, ci.name(), exc.getMessage());
	    }
	}

	String s = ci.derivedDocumentation(documentationTemplate, documentationNoValue);
	if (s != null) {
	    try {
		EAElementUtil.setEANotes(e, s);
	    } catch (EAException exc) {
		result.addError(this, 204, ci.name(), exc.getMessage());
	    }
	}

	if (ci.getLinkedDocument() != null) {
	    e.LoadLinkedDocument(ci.getLinkedDocument().getAbsolutePath());
	}

	try {
	    addTaggedValuesToRepresent(e, ci);
	} catch (EAException exc) {
	    result.addError(this, 250, ci.name(), exc.getMessage());
	}
    }

    /**
     * Determines the ArcGIS geometry type to use for the given class. It is
     * computed based upon the first occurrence of a property whose type name starts
     * with 'GM_'. The geometry type is then determined as follows:
     * <ul>
     * <li>GM_Point -&gt; {@link ArcGISGeometryType#POINT}</li>
     * <li>GM_MultiPoint -&gt; {@link ArcGISGeometryType#MULTIPOINT}</li>
     * <li>GM_Curve or GM_MultiCurve -&gt; {@link ArcGISGeometryType#POLYLINE}</li>
     * <li>GM_Surface or GM_MultiSurface -&gt;
     * {@link ArcGISGeometryType#POLYGON}</li>
     * <li>other geometry type -&gt; {@link ArcGISGeometryType#UNKNOWN}</li>
     * </ul>
     * If no geometry type is found, {@link ArcGISGeometryType#NONE} is returned.
     * 
     * @param ci
     * @return
     */
    private ArcGISGeometryType determineArcGISGeometryType(ClassInfo ci) {

	if (geometryTypeCache.containsKey(ci)) {
	    return geometryTypeCache.get(ci);
	}

	SortedSet<PropertyInfo> allPis = ci.propertiesAll();

	ArcGISGeometryType result = ArcGISGeometryType.NONE;

	// loop through properties
	for (PropertyInfo pi : allPis) {

	    Type t = pi.typeInfo();

	    if (t.name.startsWith("GM_")) {

		if (t.name.equalsIgnoreCase("GM_Point")) {
		    result = ArcGISGeometryType.POINT;
		    break;
		} else if (t.name.equalsIgnoreCase("GM_MultiPoint")) {
		    result = ArcGISGeometryType.MULTIPOINT;
		    break;
		} else if (t.name.equalsIgnoreCase("GM_Curve") || t.name.equalsIgnoreCase("GM_MultiCurve")) {
		    result = ArcGISGeometryType.POLYLINE;
		    break;
		} else if (t.name.equalsIgnoreCase("GM_Surface") || t.name.equalsIgnoreCase("GM_MultiSurface")) {
		    result = ArcGISGeometryType.POLYGON;
		    break;
		} else {
		    // unsupported geometry type
		    result = ArcGISGeometryType.UNKNOWN;
		    break;
		}
	    }
	}

	// cache the result for later use
	geometryTypeCache.put(ci, result);

	return result;
    }

    private boolean hasMultipleGeometryProperties(ClassInfo ci) {

	// /*
	// * determine full set of properties relevant for this class, so also
	// * from all supertypes (ignoring those properties that are specialized
	// * in subtypes)
	// */
	// Map<String, PropertyInfo> allPis = getAllPisForClassInfo(ci);

	SortedSet<PropertyInfo> allPis = ci.propertiesAll();

	boolean geomPropFound = false;

	for (PropertyInfo pi : allPis) {

	    Type t = pi.typeInfo();

	    if (t.name.startsWith("GM_")) {

		if (geomPropFound) {

		    // geometry property alread encountered in one of previous
		    // loops
		    return true;

		} else {

		    geomPropFound = true;
		}
	    }
	}

	return false;
    }

    @Override
    public void write() {

	// nothing to do here (SingleTarget)
    }

    /**
     * Creates a relationship class association including association class between
     * the class that the given property is in (which represents the source class),
     * and the class that is the type of the property (which is the target). The
     * source class name will be used to create the foreign key that refers to an
     * instance of that class.
     * <p>
     * Used to create a many to many relationship to represent an attribute from the
     * application schema that has category of value object or feature type and max
     * cardinality > 1.
     * 
     * @param pi
     */
    private void createManyToManyRelationshipClass(PropertyInfo pi) {

	ClassInfo ciSource = pi.inClass();

	Type ti = pi.typeInfo();
	ClassInfo ciTarget = model.classById(ti.id);

	if (ciTarget == null) {

	    result.addWarning(this, 233, ciSource.name(), pi.name(), ti.name);

	} else {

	    this.createManyToManyRelationshipClass(ciSource, ciTarget, toLowerCamelCase(ciSource.name()), pi.name(),
		    null, pi);
	}
    }

    private String doubleToString(Double d) {

	// see http://stackoverflow.com/a/38873693/3469138
	if (d == null)
	    return null;
	if (d.isNaN() || d.isInfinite())
	    return d.toString();

	// pre java 8, a value of 0 would yield "0.0" below
	if (d.doubleValue() == 0)
	    return "0";

	return new BigDecimal(d.toString()).stripTrailingZeros().toPlainString();
    }

    /**
     * Creates a relationship class including association class between the classes
     * that the given properties are in.
     * <p>
     * Used to create a many to many relationship to represent an association from
     * the application schema.
     * 
     * @param pi1
     * @param pi2
     */
    private void createManyToManyRelationshipClass(PropertyInfo pi1, PropertyInfo pi2) {

	ClassInfo ci1 = pi1.inClass();
	ClassInfo ci2 = pi2.inClass();

	this.createManyToManyRelationshipClass(ci1, ci2, pi2.name(), pi1.name(), pi2, pi1);
    }

    private void createManyToManyRelationshipClass(ClassInfo source, ClassInfo target, String roleNameSource,
	    String roleNameTarget, PropertyInfo sourceRole, PropertyInfo targetRole) {

	int sourceElementId = elementIdByClassInfo.get(source);
	Element eaClassSource = rep.GetElementByID(sourceElementId);
	int targetElementId = elementIdByClassInfo.get(target);
	Element eaClassTarget = rep.GetElementByID(targetElementId);

	if (eaClassSource == null || eaClassTarget == null) {

	    result.addError(this, 225, source.name(), target.name());
	    return;
	}

	/*
	 * execute behavior based upon abstractness of the types
	 */
	SortedSet<ClassInfo> sources, targets;

	if (source.isAbstract()) {
	    sources = computeListOfAllNonAbstractSubtypes(source);
	} else {
	    sources = new TreeSet<ClassInfo>();
	    sources.add(source);
	}

	if (target.isAbstract()) {
	    targets = computeListOfAllNonAbstractSubtypes(target);
	} else {
	    targets = new TreeSet<ClassInfo>();
	    targets.add(target);
	}

	/*
	 * create association(s) with association class(es) between source and target
	 * class(es)
	 */
	for (ClassInfo source_ : sources) {

	    int sourceElementId_ = elementIdByClassInfo.get(source_);
	    Element sourceElmt = rep.GetElementByID(sourceElementId_);

	    for (ClassInfo target_ : targets) {

		int targetElementId_ = elementIdByClassInfo.get(target_);
		Element targetElmt = rep.GetElementByID(targetElementId_);

		if (sourceElmt == null) {
		    result.addError(this, 239, source.name(), target.name(), source_.name(), target_.name());
		    continue;
		}
		if (targetElmt == null) {
		    result.addError(this, 239, source.name(), target.name(), target_.name(), source_.name());
		    continue;
		}

		try {

		    /*
		     * create association class (must be specific for each association, because an
		     * association class can only belong to a single association)
		     */

		    String relClassName = computeRelationshipClassName(source_, target_);

		    String assocClassName = normalizeName(relClassName);

		    if (exceedsMaxLength(assocClassName)) {
			this.result.addWarning(this, 226, assocClassName, "" + maxNameLength);
			assocClassName = clipToMaxLength(assocClassName);
		    }

		    Element assocClass = EARepositoryUtil.createEAClass(rep, assocClassName, assocClassesPkgId);

		    setAuthorAndStatus(assocClass);

		    Attribute foreignKeyFieldSrc = null;
		    Attribute foreignKeyFieldTgt = null;
		    Attribute ridField = null;

		    // create RID field
		    try {

			ridField = createField(assocClass, "RID", "", "", "esriFieldTypeOID", "0", "0", "0", null, null,
				true);

		    } catch (EAException e) {
			result.addError(this, 10003, "RID", assocClassName, e.getMessage());
			return;
		    }

		    /*
		     * create foreign key fields in assocClass to reference source and target
		     */

		    String srcAlias = "";
		    if(sourceRole != null && StringUtils.isNotBlank(sourceRole.aliasName())) {
			srcAlias = sourceRole.aliasName();
		    }
		    String fkSrcName = roleNameSource + foreignKeySuffix;
		    fkSrcName = normalizeName(fkSrcName);

		    if (exceedsMaxLength(fkSrcName)) {
			this.result.addWarning(this, 227, fkSrcName, "" + maxNameLength);
			fkSrcName = clipToMaxLength(fkSrcName);
		    }

		    try {

			/*
			 * in an association class, this field references a source object
			 */
			foreignKeyFieldSrc = createForeignKeyField(assocClass, fkSrcName, srcAlias, "", source_);

			if (sourceRole != null) {
			    addTaggedValuesToRepresent(foreignKeyFieldSrc, sourceRole);
			}

		    } catch (EAException e) {
			result.addError(this, 10003, fkSrcName, assocClassName, e.getMessage());
			return;
		    }

		    String tgtAlias = "";
		    if(targetRole != null && StringUtils.isNotBlank(targetRole.aliasName())) {
			tgtAlias = targetRole.aliasName();
		    }
		    String fkTgtName = roleNameTarget + foreignKeySuffix;
		    fkTgtName = normalizeName(fkTgtName);

		    if (exceedsMaxLength(fkTgtName)) {
			this.result.addWarning(this, 227, fkTgtName, "" + maxNameLength);
			fkTgtName = clipToMaxLength(fkTgtName);
		    }

		    try {

			/*
			 * in an association class, this field references a target object
			 */
			foreignKeyFieldTgt = createForeignKeyField(assocClass, fkTgtName, tgtAlias, "", target_);

			if (targetRole != null) {
			    addTaggedValuesToRepresent(foreignKeyFieldTgt, targetRole);
			}

		    } catch (EAException e) {
			result.addError(this, 10003, fkTgtName, assocClassName, e.getMessage());
			return;
		    }

		    // set stereotype and tagged values on association class

		    EAElementUtil.setEAStereotypeEx(assocClass, ArcGISWorkspaceConstants.STEREOTYPE_RELATIONSHIP_CLASS);

		    EAElementUtil.updateTaggedValue(assocClass, "CanVersion", "false", false);
		    EAElementUtil.updateTaggedValue(assocClass, "CatalogPath", "", false);
		    EAElementUtil.updateTaggedValue(assocClass, "ClassKey", "esriRelClassKeyUndefined", false);
		    EAElementUtil.updateTaggedValue(assocClass, "DSID", "-1", false);
		    EAElementUtil.updateTaggedValue(assocClass, "DatasetType", "esriDTRelationshipClass", false);
		    EAElementUtil.updateTaggedValue(assocClass, "DestinationForeignKey",
			    foreignKeyFieldTgt.GetAttributeGUID(), false);
		    EAElementUtil.updateTaggedValue(assocClass, "DestinationPrimaryKey",
			    determinePrimaryKeyGUID(target_), false);
		    EAElementUtil.updateTaggedValue(assocClass, "GlobalIDFieldName", "", false);
		    EAElementUtil.updateTaggedValue(assocClass, "IsAttachmentRelationship", "false", false);
		    EAElementUtil.updateTaggedValue(assocClass, "IsComposite", "false", false);
		    EAElementUtil.updateTaggedValue(assocClass, "IsReflexive", "false", false);
		    EAElementUtil.updateTaggedValue(assocClass, "KeyType", "esriRelKeyTypeSingle", false);
		    EAElementUtil.updateTaggedValue(assocClass, "Metadata", "", true);
		    EAElementUtil.updateTaggedValue(assocClass, "MetadataRetrieved", "false", false);
		    EAElementUtil.updateTaggedValue(assocClass, "ModelName", "", false);
		    EAElementUtil.updateTaggedValue(assocClass, "Notification", "useUMLConnectorDirection", false);
		    EAElementUtil.updateTaggedValue(assocClass, "OIDFieldName", ridField.GetAttributeGUID(), false);
		    EAElementUtil.updateTaggedValue(assocClass, "OriginForeignKey",
			    foreignKeyFieldSrc.GetAttributeGUID(), false);
		    EAElementUtil.updateTaggedValue(assocClass, "OriginPrimaryKey", determinePrimaryKeyGUID(source_),
			    false);
		    EAElementUtil.updateTaggedValue(assocClass, "RasterFieldName", "", false);
		    EAElementUtil.updateTaggedValue(assocClass, "Versioned", "false", false);

		    /*
		     * create <<RelationshipClass>> association
		     */

		    Connector con = EAElementUtil.createEAAssociation(sourceElmt, targetElmt);

		    /*
		     * NOTE: the connector has the same name as the association class - and that has
		     * already been created, so we can reuse the name here
		     */
		    EAConnectorUtil.setEAName(con, assocClassName);
		    EAConnectorUtil.setEAStereotypeEx(con, ArcGISWorkspaceConstants.STEREOTYPE_RELATIONSHIP_CLASS);

		    ConnectorEnd clientEnd = con.GetClientEnd();
		    ConnectorEnd supplierEnd = con.GetSupplierEnd();

		    AssociationInfo assocForRepresentingTVs = null;

		    if (sourceRole != null) {
			addTaggedValuesToRepresent(clientEnd, sourceRole);
			if (!sourceRole.isAttribute()) {
			    assocForRepresentingTVs = sourceRole.association();
			}
		    }
		    if (targetRole != null) {
			addTaggedValuesToRepresent(supplierEnd, targetRole);
			if (!targetRole.isAttribute()) {
			    assocForRepresentingTVs = targetRole.association();
			}
		    }
		    if (assocForRepresentingTVs != null) {
			addTaggedValuesToRepresent(con, assocForRepresentingTVs);
			addTaggedValuesToRepresent(assocClass, assocForRepresentingTVs);
		    }

		    /*
		     * set cardinality and name on connector ends
		     */

		    // TODO ensure that property name is unique (approach with
		    // counters could be improved)
		    String sourcePropName = toLowerCamelCase(roleNameSource);
		    sourcePropName = this.checkPropertyName(target_, sourcePropName);

		    String targetPropName = toLowerCamelCase(roleNameTarget);
		    targetPropName = this.checkPropertyName(source_, targetPropName);

		    EAConnectorEndUtil.setEARole(clientEnd, toLowerCamelCase(sourcePropName));

		    EAConnectorEndUtil.setEARole(supplierEnd, toLowerCamelCase(targetPropName));

		    EAConnectorEndUtil.setEACardinality(clientEnd, "0..*");
		    EAConnectorEndUtil.setEACardinality(supplierEnd, "0..*");

		    /*
		     * update tagged values on association - they are the same as for the
		     * association class
		     */

		    EAConnectorUtil.updateTaggedValue(con, "CanVersion", "false", false);
		    EAConnectorUtil.updateTaggedValue(con, "CatalogPath", "", false);
		    EAConnectorUtil.updateTaggedValue(con, "ClassKey", "esriRelClassKeyUndefined", false);
		    EAConnectorUtil.updateTaggedValue(con, "DSID", "-1", false);
		    EAConnectorUtil.updateTaggedValue(con, "DatasetType", "esriDTRelationshipClass", false);
		    EAConnectorUtil.updateTaggedValue(con, "DestinationForeignKey",
			    foreignKeyFieldTgt.GetAttributeGUID(), false);
		    EAConnectorUtil.updateTaggedValue(con, "DestinationPrimaryKey", determinePrimaryKeyGUID(target_),
			    false);
		    EAConnectorUtil.updateTaggedValue(con, "GlobalIDFieldName", "", false);
		    EAConnectorUtil.updateTaggedValue(con, "IsAttachmentRelationship", "false", false);
		    EAConnectorUtil.updateTaggedValue(con, "IsComposite", "false", false);
		    EAConnectorUtil.updateTaggedValue(con, "IsReflexive", "false", false);
		    EAConnectorUtil.updateTaggedValue(con, "KeyType", "esriRelKeyTypeSingle", false);
		    EAConnectorUtil.updateTaggedValue(con, "Metadata", "", true);
		    EAConnectorUtil.updateTaggedValue(con, "MetadataRetrieved", "false", false);
		    EAConnectorUtil.updateTaggedValue(con, "ModelName", "", false);
		    EAConnectorUtil.updateTaggedValue(con, "Notification", "useUMLConnectorDirection", false);
		    EAConnectorUtil.updateTaggedValue(con, "OIDFieldName", ridField.GetAttributeGUID(), false);
		    EAConnectorUtil.updateTaggedValue(con, "OriginForeignKey", foreignKeyFieldSrc.GetAttributeGUID(),
			    false);
		    EAConnectorUtil.updateTaggedValue(con, "OriginPrimaryKey", determinePrimaryKeyGUID(source_), false);
		    EAConnectorUtil.updateTaggedValue(con, "RasterFieldName", "", false);
		    EAConnectorUtil.updateTaggedValue(con, "Versioned", "false", false);

		    EAConnectorUtil.setEAAssociationClass(con, assocClass);

		} catch (EAException e) {

		    result.addError(this, 221, source_.name(), target_.name(), e.getMessage());
		}
	    }

	}
    }

    /**
     * Identify the GUID of the primary key field for the given class.
     * 
     * @param ci
     * @return the GUID of an attribute with stereotype 'identifier' - if
     *         {@value ArcGISWorkspaceConstants#RULE_CLS_IDENTIFIER_STEREOTYPE} is
     *         enabled and such an attribute exists - or the GUID of the OBJECTID
     *         system field.
     */
    private String determinePrimaryKeyGUID(ClassInfo ci) {

	if (identifierAttributeGUIDByClass.containsKey(ci)) {

	    return identifierAttributeGUIDByClass.get(ci);

	} else {

	    return objectIdAttributeGUIDByClass.get(ci);
	}
    }

    private String computeRelationshipClassName(ClassInfo source, ClassInfo target) {

	String sourceName = source.name();
	String targetName = target.name();

	if (source.matches(ArcGISWorkspaceConstants.RULE_ALL_RELCLASSNAME_BY_TAGGEDVALUE_OF_CLASSES)) {

	    if (StringUtils.isNotBlank(source.taggedValue(shortNameByTaggedValue))) {
		sourceName = source.taggedValue(shortNameByTaggedValue);
	    }
	    if (StringUtils.isNotBlank(target.taggedValue(shortNameByTaggedValue))) {
		targetName = target.taggedValue(shortNameByTaggedValue);
	    }
	}

	String relClassName = sourceName + "_" + targetName;

	relClassName = this.checkRelationshipClassName(relClassName);

	return relClassName;
    }

    /**
     * Creates a <<RelationshipClass>> association between the class that the given
     * property is in and the the class that is the type of the property.
     * 
     * @param pi
     */
    private void createOneToManyRelationshipClass(PropertyInfo pi) {

	ClassInfo ci = pi.inClass();

	Type typeInfo = pi.typeInfo();

	ClassInfo typeCi = model.classById(typeInfo.id);

	/*
	 * create <<RelationshipClass>> association to that type
	 */

	if (typeCi == null || !elementIdByClassInfo.containsKey(typeCi)) {

	    // we cannot establish the connection
	    result.addError(this, 218, pi.name(), pi.inClass().name(), typeInfo.name);

	} else {

	    this.createOneToManyRelationshipClass(ci, typeCi, toLowerCamelCase(ci.name()), pi.name(), Integer.MAX_VALUE,
		    false, null, pi);
	}
    }

    private void createOneToManyRelationshipClass(AssociationInfo ai) {

	ClassInfo ci1 = ai.end2().inClass();
	ClassInfo ci2 = ai.end1().inClass();

	String roleNameOnCi1 = ai.end1().name();
	String roleNameOnCi2 = ai.end2().name();

	int maxOccursOnCi1 = ai.end1().cardinality().maxOccurs;

	this.createOneToManyRelationshipClass(ci1, ci2, roleNameOnCi1, roleNameOnCi2, maxOccursOnCi1,
		ai.end1().isNavigable(), ai.end1(), ai.end2());
    }

    /**
     * The relationship association will be created with the source being the class
     * where a navigable role with maxOccurs 1 ends. The other class will be the
     * target. Source multiplicity for the association will be set to 1, target
     * multiplicity to 0..*. The target class will get the foreign key ID field that
     * is used to point to the source.
     * 
     * @param ci1              first class
     * @param ci2              second class
     * @param roleNameOnCi1    role name that belongs to the first class
     * @param roleNameOnCi2    role name that belongs to the second class
     * @param maxOccursOnCi1   maximum multiplicity that belongs to the first class
     * @param isNavigableOnCi1 whether the role that belongs to the first class is
     *                         navigable
     */
    private void createOneToManyRelationshipClass(ClassInfo ci1, ClassInfo ci2, String roleNameOnCi1,
	    String roleNameOnCi2, int maxOccursOnCi1, boolean isNavigableOnCi1, PropertyInfo roleOnCi1,
	    PropertyInfo roleOnCi2) {

	ClassInfo source, target;
	String roleNameSource, roleNameTarget;
	PropertyInfo roleSource, roleTarget;

	if (maxOccursOnCi1 <= 1 && isNavigableOnCi1) {
	    // use ci1 as source
	    source = ci1;
	    target = ci2;
	    roleNameSource = roleNameOnCi1;
	    roleNameTarget = roleNameOnCi2;
	    roleSource = roleOnCi1;
	    roleTarget = roleOnCi2;
	} else {
	    // use ci2 as source
	    source = ci2;
	    target = ci1;
	    roleNameSource = roleNameOnCi2;
	    roleNameTarget = roleNameOnCi1;
	    roleSource = roleOnCi2;
	    roleTarget = roleOnCi1;
	}

	// create foreign key field in target class
	int eaElementId = elementIdByClassInfo.get(target);
	Element eaClassTarget = rep.GetElementByID(eaElementId);
	if (eaClassTarget == null) {
	    result.addError(this, 237, source.name(), target.name(), target.name());
	    return;
	}

	String name = normalizeName(roleNameSource + foreignKeySuffix);
	String alias = "";
	if(roleSource != null && StringUtils.isNotBlank(roleSource.aliasName())) {
	    alias = roleSource.aliasName();
	}
	
	if (exceedsMaxLength(name)) {
	    this.result.addWarning(this, 205, name, roleNameSource, target.name(), "" + maxNameLength);
	    name = clipToMaxLength(name);

	}

	Attribute foreignKeyField;
	try {
	    // the foreign key field is used to reference a source object
	    foreignKeyField = createForeignKeyField(eaClassTarget, name, alias, "", source);
	    if (roleSource != null) {
		addTaggedValuesToRepresent(foreignKeyField, roleSource);
	    }

	} catch (EAException e) {
	    result.addError(this, 10003, roleNameTarget, target.name(), e.getMessage());
	    return;
	}

	/*
	 * execute behavior based upon abstractness of the types
	 */
	SortedSet<ClassInfo> sources, targets;

	if (source.isAbstract()) {
	    sources = computeListOfAllNonAbstractSubtypes(source);
	} else {
	    sources = new TreeSet<ClassInfo>();
	    sources.add(source);
	}

	if (target.isAbstract()) {
	    targets = computeListOfAllNonAbstractSubtypes(target);
	} else {
	    targets = new TreeSet<ClassInfo>();
	    targets.add(target);
	}

	// create association between source and target
	// class(es)

	// String normalizedSourceName = normalizeName(roleNameSource);
	//
	// if (exceedsMaxLength(normalizedSourceName)) {
	// this.result.addWarning(this, 205, normalizedSourceName,
	// roleNameSource, source.name(), "" + MAX_NAME_LENGTH);
	// normalizedSourceName = clipToMaxLength(normalizedSourceName);
	// }

	for (ClassInfo source_ : sources) {

	    int sourceElementId_ = elementIdByClassInfo.get(source_);
	    Element sourceElmt = rep.GetElementByID(sourceElementId_);

	    for (ClassInfo target_ : targets) {

		int targetElementId_ = elementIdByClassInfo.get(target_);
		Element targetElmt = rep.GetElementByID(targetElementId_);

		if (sourceElmt == null) {
		    result.addError(this, 238, source.name(), target.name(), source_.name(), target_.name());
		    continue;
		}
		if (targetElmt == null) {
		    result.addError(this, 238, source.name(), target.name(), target_.name(), source_.name());
		    continue;
		}

		try {

		    Connector con = EAElementUtil.createEAAssociation(sourceElmt, targetElmt);

		    String relClassName = computeRelationshipClassName(source_, target_);

		    if (exceedsMaxLength(relClassName)) {
			this.result.addWarning(this, 234, relClassName, "" + maxNameLength);
			relClassName = clipToMaxLength(relClassName);
		    }

		    EAConnectorUtil.setEAName(con, relClassName);
		    EAConnectorUtil.setEAStereotypeEx(con, ArcGISWorkspaceConstants.STEREOTYPE_RELATIONSHIP_CLASS);

		    ConnectorEnd clientEnd = con.GetClientEnd();
		    ConnectorEnd supplierEnd = con.GetSupplierEnd();

		    AssociationInfo assocForRepresentingTVs = null;

		    if (roleSource != null) {
			addTaggedValuesToRepresent(clientEnd, roleSource);
			if (!roleSource.isAttribute()) {
			    assocForRepresentingTVs = roleSource.association();
			}
		    }
		    if (roleTarget != null) {
			addTaggedValuesToRepresent(supplierEnd, roleTarget);
			if (!roleTarget.isAttribute()) {
			    assocForRepresentingTVs = roleTarget.association();
			}
		    }
		    if (assocForRepresentingTVs != null) {
			addTaggedValuesToRepresent(con, assocForRepresentingTVs);
		    }

		    // set cardinality and name on connector
		    // ends

		    // TODO ensure that property name is unique (approach with
		    // counters could be improved)
		    String sourcePropName = toLowerCamelCase(roleNameSource);
		    sourcePropName = this.checkPropertyName(target_, sourcePropName);

		    String targetPropName = toLowerCamelCase(roleNameTarget);
		    targetPropName = this.checkPropertyName(source_, targetPropName);

		    EAConnectorEndUtil.setEARole(clientEnd, toLowerCamelCase(sourcePropName));

		    EAConnectorEndUtil.setEARole(supplierEnd, toLowerCamelCase(targetPropName));

		    EAConnectorEndUtil.setEACardinality(clientEnd, "1");
		    EAConnectorEndUtil.setEACardinality(supplierEnd, "0..*");

		    // update tagged values on association

		    EAConnectorUtil.updateTaggedValue(con, "KeyType", "esriRelKeyTypeSingle", false);
		    EAConnectorUtil.updateTaggedValue(con, "ClassKey", "esriRelClassKeyUndefined", false);
		    EAConnectorUtil.updateTaggedValue(con, "OriginPrimaryKey", determinePrimaryKeyGUID(source_), false);
		    EAConnectorUtil.updateTaggedValue(con, "OriginForeignKey", foreignKeyField.GetAttributeGUID(),
			    false);
		    EAConnectorUtil.updateTaggedValue(con, "DestinationPrimaryKey", "", false);
		    EAConnectorUtil.updateTaggedValue(con, "DestinationForeignKey", "", false);
		    EAConnectorUtil.updateTaggedValue(con, "IsComposite", "false", false);
		    EAConnectorUtil.updateTaggedValue(con, "IsReflexive", "false", false);
		    EAConnectorUtil.updateTaggedValue(con, "DatasetType", "esriDTRelationshipClass", false);
		    EAConnectorUtil.updateTaggedValue(con, "OIDFieldName", "", false);
		    EAConnectorUtil.updateTaggedValue(con, "DSID", "-1", false);
		    EAConnectorUtil.updateTaggedValue(con, "ModelName", "", false);
		    EAConnectorUtil.updateTaggedValue(con, "GlobalIDFieldName", "", false);
		    EAConnectorUtil.updateTaggedValue(con, "CatalogPath", "", false);
		    EAConnectorUtil.updateTaggedValue(con, "RasterFieldName", "", false);
		    EAConnectorUtil.updateTaggedValue(con, "Versioned", "false", false);
		    EAConnectorUtil.updateTaggedValue(con, "CanVersion", "false", false);
		    EAConnectorUtil.updateTaggedValue(con, "MetadataRetrieved", "false", false);
		    EAConnectorUtil.updateTaggedValue(con, "Metadata", "", true);
		    EAConnectorUtil.updateTaggedValue(con, "Notification", "useUMLConnectorDirection", false);
		    EAConnectorUtil.updateTaggedValue(con, "IsAttachmentRelationship", "false", false);

		} catch (EAException e) {

		    result.addError(this, 221, source_.name(), target_.name(), e.getMessage());
		}
	    }
	}

    }

    private String checkRelationshipClassName(String relClassName) {

	if (counterByRelationshipClassName.containsKey(relClassName)) {

	    Integer count = counterByRelationshipClassName.get(relClassName);

	    // so now we have an additional relationship class with that name
	    int newCount = count.intValue() + 1;

	    // append new count to the relationship class name
	    String updatedName = relClassName + "_" + newCount;

	    // log new count
	    counterByRelationshipClassName.put(relClassName, newCount);

	    return updatedName;

	} else {

	    // log that we now have a relationship class with the given name
	    counterByRelationshipClassName.put(relClassName, 1);

	    return relClassName;
	}
    }

    private String checkPropertyName(ClassInfo ci, String propertyName) {

	if (counterByPropertyNameByClass.containsKey(ci)) {

	    Map<String, Integer> tmp = counterByPropertyNameByClass.get(ci);

	    if (tmp.containsKey(propertyName)) {

		Integer count = tmp.get(propertyName);

		// so now we have an additional property with that name
		int newCount = count.intValue() + 1;

		// append new count to the property name
		String updatedName = propertyName + "_" + newCount;

		// log new count
		tmp.put(propertyName, newCount);

		return updatedName;

	    } else {

		tmp.put(propertyName, 1);

		return propertyName;
	    }

	} else {

	    // log that we now have a property with the given name
	    SortedMap<String, Integer> tmp = new TreeMap<String, Integer>();

	    tmp.put(propertyName, 1);

	    counterByPropertyNameByClass.put(ci, tmp);

	    return propertyName;
	}
    }

    /**
     * Computes the list of the (direct and indirect) non abstract subtypes of the
     * given class. NOTE: Ignores explicitly modelled subtypes if
     * {@value ArcGISWorkspaceConstants#RULE_ALL_SUBTYPES} is enabled.
     * 
     * @param ci
     * @return
     */
    private SortedSet<ClassInfo> computeListOfAllNonAbstractSubtypes(ClassInfo ci) {

	SortedSet<ClassInfo> allNonAbstractSubtypes = new TreeSet<ClassInfo>();

	if (ci.subtypes() != null) {

	    for (String subtypeId : ci.subtypes()) {

		ClassInfo subtype = model.classById(subtypeId);

		if (subtype == null) {

		    result.addWarning(this, 219, subtypeId, ci.name());
		    continue;
		}

		if (isExplicitlyModeledArcGISSubtype(subtype)) {
		    // ignore explicitly modelled ArcGIS subtype
		}

		if (model.isInSelectedSchemas(subtype)) {

		    if (subtype.isAbstract()) {

			// ignore abstract subtypes

		    } else {
			allNonAbstractSubtypes.add(subtype);
		    }

		    allNonAbstractSubtypes.addAll(computeListOfAllNonAbstractSubtypes(subtype));

		} else {

		    result.addWarning(this, 220, subtype.name(), ci.name());
		}
	    }
	}

	return allNonAbstractSubtypes;
    }

    private String toLowerCamelCase(String s) {

	if (keepCaseOfRolename || s == null || s.length() == 0) {

	    return s;

	} else {

	    String first = s.substring(0, 1);
	    String lower = first.toLowerCase(Locale.ENGLISH);
	    return lower + s.substring(1, s.length());
	}
    }

    private boolean isNumericallyValued(ClassInfo ci) {
	return StringUtils.isNotBlank(ci.taggedValue(ArcGISWorkspaceConstants.TV_NUMERIC_TYPE));
    }

    private Integer computeScale(Info pi, String valueTypeName) {

	String nameOfScaleTV = "scale";
	String scaleTV = pi.taggedValue(nameOfScaleTV);

	if ((pi.matches(ArcGISWorkspaceConstants.RULE_ALL_SCALE)
		|| pi.matches(ArcGISWorkspaceConstants.RULE_PROP_SCALE)) && StringUtils.isNotBlank(scaleTV)) {

	    try {
		Integer scale = Integer.parseInt(scaleTV.trim());
		return scale;
	    } catch (NumberFormatException e) {
		MessageContext mc = result.addWarning(this, 243, scaleTV.trim(), nameOfScaleTV);
		mc.addDetail(this, 20001, pi.fullNameInSchema());
	    }
	}

	// if the property type is known, use the known value
	if (scaleMappingByTypeName.containsKey(valueTypeName)) {

	    return scaleMappingByTypeName.get(valueTypeName);

	} else {

	    // use default value
	    return 0;
	}
    }

    private Integer computePrecision(Info pi, String valueTypeName) {

	String nameOfPrecisionTV = "precision";
	String precisionTV = pi.taggedValue(nameOfPrecisionTV);

	if ((pi.matches(ArcGISWorkspaceConstants.RULE_ALL_PRECISION)
		|| pi.matches(ArcGISWorkspaceConstants.RULE_PROP_PRECISION)) && StringUtils.isNotBlank(precisionTV)) {

	    try {
		Integer prec = Integer.parseInt(precisionTV.trim());
		return prec;
	    } catch (NumberFormatException e) {
		MessageContext mc = result.addWarning(this, 243, precisionTV.trim(), nameOfPrecisionTV);
		mc.addDetail(this, 20001, pi.fullNameInSchema());
	    }
	}

	// if the property type is known, use the known value
	if (precisionMappingByTypeName.containsKey(valueTypeName)) {

	    return precisionMappingByTypeName.get(valueTypeName);

	} else {

	    // use default value
	    return 0;
	}
    }

    private boolean computeIsNullable(PropertyInfo pi) {

	if (pi.matches(ArcGISWorkspaceConstants.RULE_PROP_ISNULLABLE)) {

	    if (pi.voidable() || pi.cardinality().minOccurs < 1) {
		return true;
	    } else {
		return false;
	    }

	} else {
	    return true;
	}
    }

    private int computeLength(PropertyInfo pi, String valueTypeName) {

	// if the property type is known, use the known value
	if (lengthMappingByTypeName.containsKey(valueTypeName)) {

	    return lengthMappingByTypeName.get(valueTypeName);

	} else {

	    if (pi.matches(ArcGISWorkspaceConstants.RULE_PROP_LENGTH_FROM_TAGGED_VALUE)) {

		String tv = pi.taggedValue(nameOfTVToDetermineFieldLength);

		if (StringUtils.isNotBlank(tv)) {

		    try {
			Integer value = Integer.parseInt(tv.trim());
			if (value >= 0) {
			    return value;
			}
		    } catch (NumberFormatException e) {
			MessageContext mc = result.addWarning(this, 243, tv.trim(), nameOfTVToDetermineFieldLength);
			mc.addDetail(this, 20001, pi.fullNameInSchema());
		    }
		}
	    }

	    /*
	     * see if length is defined by OCL constraint contained in pi's class; this
	     * information has been computed when all class infos have been processed
	     */
	    ClassInfo ci = pi.inClass();

	    Integer length = null;

	    length = lengthByClassPropName.get(ci.name() + "_" + pi.name());

	    if (length == null) {

		// no length constraint is available, use default value
		return lengthTaggedValueDefault;

	    } else {

		return length;
	    }
	}
    }

    private int computeLengthForCodelistOrEnumerationValueType(PropertyInfo pi) {

	if (pi.matches(
		ArcGISWorkspaceConstants.RULE_PROP_LENGTH_FROM_TAGGED_VALUE_FOR_CODELIST_OR_ENUMERATION_VALUE_TYPE)) {

	    String tv = pi.taggedValue(nameOfTVToDetermineFieldLength);

	    if (StringUtils.isNotBlank(tv)) {

		try {
		    Integer value = Integer.parseInt(tv.trim());
		    if (value >= 0) {
			return value;
		    }
		} catch (NumberFormatException e) {
		    MessageContext mc = result.addWarning(this, 243, tv.trim(), nameOfTVToDetermineFieldLength);
		    mc.addDetail(this, 20001, pi.fullNameInSchema());
		}
	    }
	}

	if (pi.matches(ArcGISWorkspaceConstants.RULE_PROP_LENGTH_FROM_CODES_OR_ENUMS_OF_VALUE_TYPE)) {

	    ClassInfo typeCi = model.classById(pi.typeInfo().id);

	    if (typeCi == null) {

		MessageContext mc = result.addWarning(this, 244, pi.typeInfo().name, pi.name());
		mc.addDetail(this, 20001, pi.fullNameInSchema());

	    } else {

		if (!typeCi.properties().isEmpty()) {

		    int maxLength = 0;
		    for (PropertyInfo pix : typeCi.properties().values()) {
			if (pix.name().length() > maxLength) {
			    maxLength = pix.name().length();
			}
		    }
		    return maxLength;
		}
	    }
	}

	/*
	 * default length for property with code list or enumeration as value type is 0
	 */
	return 0;
    }

    private Attribute createField(Element e, String name, String alias, String documentation, String eaType,
	    String tvLength, String tvPrecision, String tvScale, Integer eaClassifierId, String initialValue,
	    boolean isNullable) throws EAException {

	return createField(e, name, alias, documentation, eaType, tvLength, tvPrecision, tvScale, eaClassifierId,
		initialValue, isNullable, "Field");
    }

    private Attribute createField(Element e, String name, String alias, String documentation, String eaType,
	    String tvLength, String tvPrecision, String tvScale, Integer eaClassifierId, String initialValue,
	    boolean isNullable, String stereotype) throws EAException {

	List<EATaggedValue> tvs = new ArrayList<EATaggedValue>();

	tvs.add(new EATaggedValue("DomainFixed", "false"));
	tvs.add(new EATaggedValue("Editable", "true"));
	tvs.add(new EATaggedValue("GeometryDef", "", true));
	tvs.add(new EATaggedValue("IsNullable", isNullable ? "true" : "false"));
	tvs.add(new EATaggedValue("Length", tvLength));
	tvs.add(new EATaggedValue("ModelName", ""));
	tvs.add(new EATaggedValue("Precision", tvPrecision));
	tvs.add(new EATaggedValue("RasterDef", "", true));
	tvs.add(new EATaggedValue("Required", "false"));
	tvs.add(new EATaggedValue("Scale", tvScale));

	SortedSet<String> stereotypes = new TreeSet<String>();
	stereotypes.add(stereotype);

	if (eaClassifierId != null) {
	    /*
	     * Remove eaClassifierId in elementIdOfCodedValueDomain - so that at the end of
	     * processing, the set elementIdOfCodedValueDomain only contains IDs of the
	     * coded value domains that are not used in the model.
	     */
	    elementIdsOfUnusedCodedValueDomain.remove(eaClassifierId);
	}

	return EAElementUtil.createEAAttribute(e, name, alias, documentation, stereotypes, tvs, false, false, false,
		initialValue, false, new Multiplicity(1, 1), eaType, eaClassifierId);
    }

    @Override
    public String getTargetName() {
	return "ArcGIS Workspace";
    }

    @SuppressWarnings("rawtypes")
    public void reset() {

	initialised = false;
	model = null;
	numberOfSchemasSelectedForProcessing = 0;
	workspaceTemplateFilePath = ArcGISWorkspaceConstants.WORKSPACE_TEMPLATE_URL;
	maxNameLength = ArcGISWorkspaceConstants.DEFAULT_MAX_NAME_LENGTH;
	lengthTaggedValueDefault = ArcGISWorkspaceConstants.LENGTH_TAGGED_VALUE_DEFAULT;
	numRangeDelta = ArcGISWorkspaceConstants.NUM_RANGE_DELTA;
	esriTypesSuitedForRangeConstraint = new TreeSet<String>();
	outputDirectory = null;
	outputDirectoryFile = null;
	documentationTemplate = null;
	documentationNoValue = null;
	rep = null;
	ignoredCis = new TreeSet<ClassInfo>();
	geometryTypeCache = new TreeMap<ClassInfo, ArcGISGeometryType>();
	elementIdByClassInfo = new TreeMap<ClassInfo, Integer>();
	elementNameByClassInfo = new TreeMap<ClassInfo, String>();
	elementIdsOfUnusedCodedValueDomain = new TreeSet<>();
	objectIdAttributeGUIDByClass = new TreeMap<ClassInfo, String>();
	identifierAttributeGUIDByClass = new TreeMap<ClassInfo, String>();
	generalisations = new TreeMap<ClassInfo, ClassInfo>();
	arcgisSubtypes = new HashSet<ClassInfo>();
	subtypeElementIdBySubtypeNameByParent = new TreeMap<>();
	subtypeCodedValueDomainEAIDByMultiKey = new MultiKeyMap();
	associations = new TreeSet<AssociationInfo>();
	counterByRelationshipClassName = new TreeMap<String, Integer>();
	counterByPropertyNameByClass = new TreeMap<ClassInfo, SortedMap<String, Integer>>();
	workspacePkgId = null;
	featuresPkgId = null;
	tablesPkgId = null;
	assocClassesPkgId = null;
	domainsPkgId = null;
	eaPkgIdByModelPkg_byWorkspaceSubPkgId = new TreeMap<Integer, SortedMap<PackageInfo, Integer>>();
	processMapEntries = null;
	lengthMappingByTypeName = new TreeMap<String, Integer>();
	precisionMappingByTypeName = new TreeMap<String, Integer>();
	scaleMappingByTypeName = new TreeMap<String, Integer>();
	lengthByClassPropName = new TreeMap<String, Integer>();
	numericRangeConstraintByPropNameByClassName = new TreeMap<ClassInfo, SortedMap<String, NumericRangeConstraintMetadata>>();
	nameOfTVToDetermineFieldLength = "size";
	absolutePathOfOutputEaRepositoryFile = null;
	shortNameByTaggedValue = null;
	keepCaseOfRolename = false;
	foreignKeySuffix = "ID";
	reflexiveRelationshipAttributeSuffix = "";
	numericRangeElementIdsByClassName = new TreeMap<String, Integer>();
	taggedValuesToRepresent = null;
	representTaggedValues = false;
	author = null;
	status = null;
    }

    @Override
    public void writeAll(ShapeChangeResult r) {

	if (rep == null)
	    return; // repository not initialised

	this.result = r;
	this.options = r.options();

	// establish generalization relationships
	for (Entry<ClassInfo, ClassInfo> entry : generalisations.entrySet()) {

	    ClassInfo ci1 = entry.getKey();
	    ClassInfo ci2 = entry.getValue();

	    if (!elementIdByClassInfo.containsKey(ci1))
		result.addWarning(this, 207, ci1.name(), ci2.name());
	    else if (!elementIdByClassInfo.containsKey(ci2))
		result.addWarning(this, 208, ci1.name(), ci2.name());
	    else {

		String c1Name = elementNameByClassInfo.get(ci1);
		String c2Name = elementNameByClassInfo.get(ci2);

		String generalizationStereotype = "";
		if (arcgisSubtypes.contains(ci1)) {
		    generalizationStereotype = "ArcGIS::Subtype";
		}

		try {
		    Connector con = EARepositoryUtil.createEAGeneralization(rep, elementIdByClassInfo.get(ci1), c1Name,
			    elementIdByClassInfo.get(ci2), c2Name);
		    if (StringUtils.isNotBlank(generalizationStereotype)) {
			EAConnectorUtil.setEAStereotypeEx(con, generalizationStereotype);
		    }
		} catch (EAException e) {
		    result.addWarning(this, 10002, c1Name, c2Name, e.getMessage());
		}
	    }
	}

	List<PropertyInfo> pisToCreateRelationshipClassesWith = new ArrayList<PropertyInfo>();

	/*
	 * Process properties of all classes. Do not create relationship classes in this
	 * loop, just keep track of the relevant properties. The reason is that
	 * <<identifier>> attributes may be significant. Such attributes need to be
	 * created in this loop. Afterwards, they can be used when creating relationship
	 * classes.
	 */
	for (ClassInfo ci : elementIdByClassInfo.keySet()) {

	    if (ci.category() == Options.ENUMERATION || ci.category() == Options.CODELIST) {
		/* enumerations and codelists have already been fully created */
		continue;
	    }

	    int eaElementId = elementIdByClassInfo.get(ci);
	    Element eaClass = rep.GetElementByID(eaElementId);

	    for (PropertyInfo pi : ci.properties().values()) {

		/*
		 * Check properties on an explicitly modelled ArcGIS subtype.
		 */
		if (isExplicitlyModeledArcGISSubtype(ci)) {

		    ClassInfo supertype = ci.baseClass();

		    /*
		     * Ignore geometry properties.
		     */
		    Type t = pi.typeInfo();
		    if (t.name.startsWith("GM_")) {
			MessageContext mc = result.addWarning(this, 266, ci.name(), supertype.name(), pi.name(),
				t.name);
			if (mc != null) {
			    mc.addDetail(this, -2, pi.fullNameInSchema());
			}
			continue;
		    }

		    /*
		     * Ignore properties that the supertype does not define.
		     */
		    if (supertype.property(pi.name()) == null) {
			MessageContext mc = result.addWarning(this, 265, ci.name(), supertype.name(), pi.name());
			if (mc != null) {
			    mc.addDetail(this, -2, pi.fullNameInSchema());
			}
			continue;
		    }
		}

		Attribute eaAtt = null;

		if (!pi.isAttribute()) {
		    // keep track of the association for later
		    associations.add(pi.association());
		    continue;
		}

		String initialValue = null;
		if (pi.matches(ArcGISWorkspaceConstants.RULE_PROP_INITIAL_VALUE)) {
		    initialValue = pi.initialValue();
		}

		Type typeInfo = pi.typeInfo();
		String mappedTypeName = typeInfo.name;

		// handle reflexive relationships
		if (typeInfo.id.equals(ci.id())) {

		    if (pi.matches(ArcGISWorkspaceConstants.RULE_PROP_REFLEXIVE_AS_FIELD)) {

			try {
			    createFieldForReflexiveRelationshipProperty(eaClass, pi);
			} catch (EAException e) {
			    result.addError(this, 10005, pi.name(), ci.name(), e.getMessage());
			}

		    } else {
			result.addWarning(this, 236, ci.name(), pi.name());
		    }

		    continue;
		}

		/*
		 * NOTE: This check is performed after the one for reflexive relationships,
		 * since RULE_PROP_REFLEXIVE_AS_FIELD supports creation of a field for a
		 * reflexive relationship property, even if that property has max cardinality >
		 * 1.
		 */
		if (pi.cardinality().maxOccurs > 1 && !(pi.categoryOfValue() == Options.FEATURE
			|| pi.categoryOfValue() == Options.OBJECT || pi.categoryOfValue() == Options.GMLOBJECT)) {

		    /*
		     * multiplicity must have been flattened, at least for actual attributes -
		     * unless the category of value for the property is object or feature type (then
		     * establish a one to many or many to many relationship - which is covered later
		     * on in the decision tree)
		     */
		    result.addWarning(this, 212, pi.name(), ci.name());
		    continue;
		}

		ClassInfo typeCi = model.classByIdOrName(typeInfo);

		String normalizedPiName = normalizeName(pi.name());

		if (exceedsMaxLength(normalizedPiName)) {
		    this.result.addWarning(this, 205, normalizedPiName, pi.name(), ci.name(), "" + maxNameLength);
		    normalizedPiName = clipToMaxLength(normalizedPiName);
		}

		String normalizedPiAlias = pi.aliasName() == null ? null : normalizeAlias(pi.aliasName(), ci);

		/*
		 * first, determine if a type mapping is available - if so, apply it
		 */
		if (processMapEntries.containsKey(typeInfo.name)) {

		    ProcessMapEntry pme = processMapEntries.get(typeInfo.name);

		    // during initialization we ensured that the map entry
		    // has a target type, so no need to check again here

		    /*
		     * now it depends: is the target type one that supports numeric range
		     * constraints, and is such a constraint actually defined for the property?
		     */

		    Element numericRange = null;

		    if (esriTypesSuitedForRangeConstraint.contains(pme.getTargetType())
			    && numericRangeConstraintByPropNameByClassName.containsKey(ci)) {
			numericRange = determineNumericRange(ci, pi, pme);
		    }

		    if (numericRange != null) {

			try {
			    String valueType = numericRange.GetName();
			    eaAtt = createField(eaClass, normalizedPiName, normalizedPiAlias,
				    pi.derivedDocumentation(documentationTemplate, documentationNoValue), valueType,
				    "0", "" + computePrecision(pi, valueType), "" + computeScale(pi, valueType),
				    numericRange.GetElementID(), initialValue, computeIsNullable(pi));

			} catch (EAException e) {
			    result.addError(this, 10003, pi.name(), ci.name(), e.getMessage());
			}

		    } else {

			/*
			 * try to find the target type in the model (for updating the type info of pi)
			 */
			ClassInfo targetTypeCi = model.classByName(pme.getTargetType());

			mappedTypeName = pme.getTargetType();

			/*
			 * now try to find the target type in the class map - if it is available there
			 * we can set the classifier ID correctly; otherwise we simply don't set the
			 * classifier ID
			 */
			String eaTargetType;
			Integer eaTargetClassifierId = null;

			if (targetTypeCi == null || !elementIdByClassInfo.containsKey(targetTypeCi)) {
			    // it's alright to not have an actual class for the
			    // target type defined in the map entry
			    eaTargetType = pme.getTargetType();

			} else {

			    int eaTargetTypeElementId = elementIdByClassInfo.get(targetTypeCi);
			    Element eaTargetTypeClass = rep.GetElementByID(eaTargetTypeElementId);

			    eaTargetType = eaTargetTypeClass.GetName();
			    eaTargetClassifierId = eaTargetTypeClass.GetElementID();
			}

			try {

			    if (pi.matches(ArcGISWorkspaceConstants.RULE_ALL_SUBTYPES)
				    && StringUtils.isNotBlank(pi.taggedValue("arcgisDefaultSubtype"))) {

				if (!pi.typeInfo().name.equalsIgnoreCase("Integer")) {
				    MessageContext mc = result.addWarning(this, 267, ci.name(), pi.name(),
					    pi.typeInfo().name);
				    if (mc != null) {
					mc.addDetail(this, -2, pi.fullNameInSchema());
				    }
				}

				eaAtt = createField(eaClass, normalizedPiName, normalizedPiAlias,
					pi.derivedDocumentation(documentationTemplate, documentationNoValue),
					"esriFieldTypeInteger", "0", "9", "0", null,
					pi.taggedValue("arcgisDefaultSubtype").trim(), computeIsNullable(pi),
					"ArcGIS::SubtypeField");

			    } else {

				String derivedDocumentation = pi.derivedDocumentation(documentationTemplate,
					documentationNoValue);
				String length = "" + computeLength(pi, mappedTypeName);
				String precision = "" + computePrecision(pi, mappedTypeName);
				String scale = "" + computeScale(pi, mappedTypeName);
				boolean isNullable = computeIsNullable(pi);

				eaAtt = createField(eaClass, normalizedPiName, normalizedPiAlias, derivedDocumentation,
					eaTargetType, length, precision, scale, eaTargetClassifierId, initialValue,
					isNullable);

				/*
				 * Create fields in ArcGIS subtypes if necessary. That is only the case here if
				 * the class is the parent of ArcGIS subtypes modelled via a property with a
				 * codelist or enumeration that defines a set of subtypes AND tagged value
				 * 'arcgisSubtypeInitialValue' of the current property is not empty. In that
				 * case, subtype specific initial values are used.
				 */
				String arcgisSubtypeInitialValues = pi.taggedValue("arcgisSubtypeInitialValues");
				if (subtypeElementIdBySubtypeNameByParent.containsKey(ci)
					&& StringUtils.isNotBlank(arcgisSubtypeInitialValues)) {

				    Map<String, Integer> subtypeEAIDBySubtypeName = subtypeElementIdBySubtypeNameByParent
					    .get(ci);

				    Map<String, String> initialValueBySubtypeName = parseArcGISSubtypeInitialValues(
					    arcgisSubtypeInitialValues, pi);

				    if (!initialValueBySubtypeName.isEmpty()) {

					for (String subtypeName : initialValueBySubtypeName.keySet()) {

					    if (!subtypeEAIDBySubtypeName.containsKey(subtypeName)) {

						MessageContext mc = result.addError(this, 264, pi.name(), subtypeName,
							ci.name());
						if (mc != null) {
						    mc.addDetail(this, -2, pi.fullNameInSchema());
						}
						continue;
					    }

					    int subtypeEAElementID = subtypeEAIDBySubtypeName.get(subtypeName);
					    Element subtypeEAElement = rep.GetElementByID(subtypeEAElementID);

					    String subtypeInitialValue = initialValueBySubtypeName.get(subtypeName);

					    Attribute subtypeEAAtt = createField(subtypeEAElement, normalizedPiName,
						    normalizedPiAlias, derivedDocumentation, eaTargetType, length,
						    precision, scale, eaTargetClassifierId, subtypeInitialValue,
						    isNullable);

					    try {
						addTaggedValuesToRepresent(subtypeEAAtt, pi);
					    } catch (EAException e) {
						result.addError(this, 251, pi.name(), e.getMessage());
					    }

					}
				    }
				}
			    }

			    if (ci.matches(ArcGISWorkspaceConstants.RULE_CLS_IDENTIFIER_STEREOTYPE)
				    && pi.stereotype("identifier") && !identifierAttributeGUIDByClass.containsKey(ci)) {

				identifierAttributeGUIDByClass.put(ci, eaAtt.GetAttributeGUID());
			    }

			} catch (EAException e) {
			    result.addError(this, 10003, pi.name(), ci.name(), e.getMessage());
			}
		    }

		} else if (typeInfo.name.startsWith("GM_")) {

		    /*
		     * ignore geometry typed properties - the geometry is implicit for an ArcGIS
		     * feature class (<<Point>> etc).
		     */
		    result.addDebug(this, 213, pi.name(), ci.name());

		} else if (typeCi != null && !model.isInSelectedSchemas(typeCi)) {

		    /*
		     * type does not belong to application schema - ignore the property
		     */
		    result.addWarning(this, 222, typeInfo.name, pi.name(), ci.name());

		} else if (pi.categoryOfValue() == Options.FEATURE || pi.categoryOfValue() == Options.OBJECT
			|| pi.categoryOfValue() == Options.GMLOBJECT) {

		    pisToCreateRelationshipClassesWith.add(pi);

		} else if (pi.categoryOfValue() == Options.ENUMERATION || pi.categoryOfValue() == Options.CODELIST) {

		    if (pi.matches(ArcGISWorkspaceConstants.RULE_ALL_SUBTYPES)
			    && StringUtils.isNotBlank(pi.taggedValue("arcgisDefaultSubtype"))) {

			// this is the property that defines the subtype set

			try {
			    eaAtt = createField(eaClass, normalizedPiName, normalizedPiAlias,
				    pi.derivedDocumentation(documentationTemplate, documentationNoValue),
				    "esriFieldTypeInteger", "0", "9", "0", null,
				    pi.taggedValue("arcgisDefaultSubtype").trim(), computeIsNullable(pi),
				    "ArcGIS::SubtypeField");
			} catch (EAException e) {
			    result.addError(this, 10003, pi.name(), ci.name(), e.getMessage());
			}

		    } else {

			String eaType;
			Integer eaClassifierId = null;

			if (typeCi == null || !elementIdByClassInfo.containsKey(typeCi)) {
			    result.addWarning(this, 216, typeInfo.name, pi.name(), ci.name());
			    eaType = clipToMaxLength(typeInfo.name);
			} else {
			    int eaTypeElementId = elementIdByClassInfo.get(typeCi);
			    Element eaTypeClass = rep.GetElementByID(eaTypeElementId);
			    eaType = eaTypeClass.GetName();
			    eaClassifierId = eaTypeClass.GetElementID();
			}

			try {

			    int length = computeLengthForCodelistOrEnumerationValueType(pi);
			    Integer precision = computePrecision(pi, pi.typeInfo().name);
			    Integer scale = computeScale(pi, pi.typeInfo().name);

			    if (typeCi != null && isNumericallyValued(typeCi)) {

				/*
				 * NOTE: We don't check here that a mapping exists for the numeric conceptual
				 * type specified by the numeric type tagged value of typeCi. If it cannot be
				 * mapped then an error will be logged when constructing the coded value domain
				 * for typeCi. Another check here should therefore not be necessary.
				 */

				if (precision <= 0 && scale <= 0) {
				    /*
				     * Precision and scale on property override the same settings on a numerically
				     * valued code list / enumeration. Only if precision and scale are not set (<=
				     * 0), try to get a useful value from the code list / enumeration itself.
				     */
				    precision = computePrecision(typeCi, typeCi.name());
				    scale = computeScale(typeCi, typeCi.name());
				}

				length = 0;
			    }

			    /*
			     * Create fields in ArcGIS subtypes if necessary. That is the case if ci is the
			     * parent of ArcGIS subtypes modelled via a property with a codelist or
			     * enumeration that defines a set of subtypes AND a subtype specific coded value
			     * domain shall be used as type. Tagged value 'arcgisSubtypeInitialValue' of the
			     * current property may also play a role, regarding the initial value, if not
			     * empty.
			     * 
			     * If one of the two situations applies for this property, then subtype specific
			     * fields need to be created - and the type of the field in the parent class
			     * (that represents pi) shall be the esri field type of the coded value domain.
			     */

			    String eaTypeForNonSubtypeField = eaType;
			    Integer eaClassifierIdForNonSubtypeField = eaClassifierId;
			    boolean isNullable = computeIsNullable(pi);
			    String derivedDocumentation = pi.derivedDocumentation(documentationTemplate,
				    documentationNoValue);

			    // first, check if there's any subtype at all
			    if (subtypeElementIdBySubtypeNameByParent.containsKey(ci)) {

				String arcgisSubtypeInitialValues = pi.taggedValue("arcgisSubtypeInitialValues");

				@SuppressWarnings("unchecked")
				SortedMap<String, Integer> subtypeSpecificCodedValueDomainEAIDBySubtypeName = (SortedMap<String, Integer>) subtypeCodedValueDomainEAIDByMultiKey
					.get(ci, pi);

				if (StringUtils.isNotBlank(arcgisSubtypeInitialValues)
					|| (subtypeSpecificCodedValueDomainEAIDBySubtypeName != null
						&& !subtypeSpecificCodedValueDomainEAIDBySubtypeName.isEmpty())) {

				    /*
				     * Alright, so we have subtype specific codelist/enumeration use.
				     * 
				     * Modify the ea type and classifier of the non subtype field.
				     */
				    eaTypeForNonSubtypeField = determineFieldTypeForCodedValueDomain(typeCi);
				    eaClassifierIdForNonSubtypeField = null;

				    Map<String, String> initialValueBySubtypeName = parseArcGISSubtypeInitialValues(
					    arcgisSubtypeInitialValues, pi);

				    Map<String, Integer> subtypeEAIDBySubtypeName = subtypeElementIdBySubtypeNameByParent
					    .get(ci);

				    // walk through all established subtypes
				    for (Entry<String, Integer> stEntry : subtypeEAIDBySubtypeName.entrySet()) {

					String subtypeName = stEntry.getKey();
					int subtypeEAElementID = stEntry.getValue();
					Element subtypeEAElement = rep.GetElementByID(subtypeEAElementID);

					String subtypeInitialValue = initialValueBySubtypeName.get(subtypeName);

					String eaTypeForSubtypeField = eaType;
					Integer eaClassifierIdForSubtypeField = eaClassifierId;

					if (subtypeSpecificCodedValueDomainEAIDBySubtypeName != null
						&& subtypeSpecificCodedValueDomainEAIDBySubtypeName
							.containsKey(subtypeName)) {
					    int subtypeSpecificCodedValueDomainEAID = subtypeSpecificCodedValueDomainEAIDBySubtypeName
						    .get(subtypeName);
					    Element subtypeSpecificCodedValueDomain = rep
						    .GetElementByID(subtypeSpecificCodedValueDomainEAID);
					    eaTypeForSubtypeField = subtypeSpecificCodedValueDomain.GetName();
					    eaClassifierIdForSubtypeField = subtypeSpecificCodedValueDomainEAID;
					}

					Attribute subtypeEAAtt = createField(subtypeEAElement, normalizedPiName,
						normalizedPiAlias, derivedDocumentation, eaTypeForSubtypeField,
						"" + length, "" + precision, "" + scale, eaClassifierIdForSubtypeField,
						subtypeInitialValue, isNullable);

					try {
					    addTaggedValuesToRepresent(subtypeEAAtt, pi);
					} catch (EAException e) {
					    result.addError(this, 251, pi.name(), e.getMessage());
					}
				    }
				}
			    }

			    eaAtt = createField(eaClass, normalizedPiName, normalizedPiAlias, derivedDocumentation,
				    eaTypeForNonSubtypeField, "" + length, "" + precision, "" + scale,
				    eaClassifierIdForNonSubtypeField, initialValue, isNullable);

			} catch (EAException e) {
			    result.addError(this, 10003, pi.name(), ci.name(), e.getMessage());
			}
		    }

		} else if (pi.categoryOfValue() == Options.UNION) {

		    // unions aren't supported
		    result.addWarning(this, 214, pi.name(), ci.name());

		} else if (pi.categoryOfValue() == Options.DATATYPE) {

		    // data types aren't supported
		    result.addWarning(this, 215, pi.name(), ci.name());

		} else {

		    // This case is unexpected
		    result.addError(this, 217, pi.name(), "" + pi.categoryOfValue(), ci.name());

		}

		if (eaAtt != null) {

		    try {
			addTaggedValuesToRepresent(eaAtt, pi);
		    } catch (EAException e) {
			result.addError(this, 251, pi.name(), e.getMessage());
		    }

		    // determine if an attribute index shall be created
		    if (pi.matches(ArcGISWorkspaceConstants.RULE_PROP_ATTINDEX)
			    && "true".equalsIgnoreCase(pi.taggedValue("sqlUnique"))) {

			createAttributeIndex(eaClass, ci, eaAtt, pi);
		    }
		}
	    }
	}

	// process properties to create relationship classes
	for (PropertyInfo pi : pisToCreateRelationshipClassesWith) {

	    if (pi.cardinality().maxOccurs > 1) {

		createManyToManyRelationshipClass(pi);

	    } else {

		createOneToManyRelationshipClass(pi);
	    }
	}

	// process associations
	for (AssociationInfo ai : associations) {

	    // upper bound of either end can be > 1

	    /*
	     * 2014-11-26 NOTE: flattening of multiplicity normally dissolves associations
	     * where one end has max cardinality > 1; this behavior can be suppressed for
	     * associations between feature and object types via a configuration parameter
	     */

	    PropertyInfo end1 = ai.end1();
	    int end1CiCat = end1.inClass().category();
	    PropertyInfo end2 = ai.end2();
	    int end2CiCat = end2.inClass().category();

	    /*
	     * only process associations between feature and object types, ignore those
	     * where one end is a union, datatype, enumeration, codelist etc.
	     */
	    if (!(end1CiCat == Options.FEATURE || end1CiCat == Options.GMLOBJECT || end1CiCat == Options.OBJECT)
		    && !(end2CiCat == Options.FEATURE || end2CiCat == Options.GMLOBJECT
			    || end2CiCat == Options.OBJECT)) {

		// we only process associations between feature and object types
		result.addDebug(this, 223, end1.inClass().name(), end2.inClass().name());
		continue;
	    }

	    // handle reflexive relationships
	    if (end1.inClass().id().equals(end2.inClass().id())) {

		if (end1.matches(ArcGISWorkspaceConstants.RULE_PROP_REFLEXIVE_AS_FIELD)) {

		    ClassInfo ci = end1.inClass();

		    int eaElementId = elementIdByClassInfo.get(ci);
		    Element eaClass = rep.GetElementByID(eaElementId);

		    if (end1.isNavigable()) {
			try {
			    createFieldForReflexiveRelationshipProperty(eaClass, end1);
			} catch (EAException e) {
			    result.addError(this, 10005, end1.name(), ci.name(), e.getMessage());
			}
		    }

		    if (end2.isNavigable()) {
			try {
			    createFieldForReflexiveRelationshipProperty(eaClass, end2);
			} catch (EAException e) {
			    result.addError(this, 10005, end2.name(), ci.name(), e.getMessage());
			}
		    }

		} else {
		    result.addWarning(this, 235, end1.inClass().name());
		}

		continue;
	    }

	    if (!(model.isInSelectedSchemas(end1.inClass()) && model.isInSelectedSchemas(end2.inClass()))) {

		// we only process associations where both ends are in the app
		// schema
		result.addDebug(this, 224, end1.inClass().name(), end2.inClass().name());
		continue;

	    }

	    /*
	     * Alright, now we should have an association between two feature and/or object
	     * types that are both part of schemas selected for processing
	     */

	    // differentiate between one to many and many to many
	    if ((end1.isNavigable() && end1.cardinality().maxOccurs == 1)
		    || (end2.isNavigable() && end2.cardinality().maxOccurs == 1)) {

		createOneToManyRelationshipClass(ai);

	    } else {

		createManyToManyRelationshipClass(end1, end2);
	    }
	}

	postprocess();

	EARepositoryUtil.closeRepository(rep);
    }

    private void postprocess() {

	result.addInfo(this, 30000);

	if (!elementIdByClassInfo.isEmpty() && elementIdByClassInfo.keySet().iterator().next()
		.matches(ArcGISWorkspaceConstants.RULE_ALL_POSTPROCESS_REMOVE_UNUSED_CODED_VALUE_DOMAINS)) {
	    result.addInfo(this, 30001);

	    for (Integer elementIDOfUnusedCodedValueDomain : elementIdsOfUnusedCodedValueDomain) {
		Element elmtToRemove = rep.GetElementByID(elementIDOfUnusedCodedValueDomain);
		int pkgId = elmtToRemove.GetPackageID();
		Package pkg = rep.GetPackageByID(pkgId);
		result.addInfo(this, 272, elmtToRemove.GetName());
		EAPackageUtil.deleteElement(pkg, elementIDOfUnusedCodedValueDomain);
	    }
	}

	if (!elementIdByClassInfo.isEmpty() && elementIdByClassInfo.keySet().iterator().next()
		.matches(ArcGISWorkspaceConstants.RULE_ALL_SUBTYPES)) {

	    result.addInfo(this, 30002);

	    /*
	     * For each class that has an ArcGIS subtype, check all its fields to determine
	     * the maximum length, scale, precision, and whether it is nullable from its
	     * subtypes.
	     */
	    for (ClassInfo ci : elementIdByClassInfo.keySet()) {

		if (ArcGISUtil.hasArcGISSubtype(ci)) {

		    Element parentElement = rep.GetElementByID(elementIdByClassInfo.get(ci));

		    /*
		     * Get map of all subtypes and their EA elements.
		     */
		    SortedMap<String, Element> subtypeElementByName = new TreeMap<>();
		    for (String subtypeId : ci.subtypes()) {

			ClassInfo subtype = ci.model().classById(subtypeId);
			Element subtypeElement = rep.GetElementByID(elementIdByClassInfo.get(subtype));
			subtypeElementByName.put(subtype.name(), subtypeElement);
		    }

		    Collection<Attribute> parentAtts = parentElement.GetAttributes();
		    for (short i = 0; i < parentAtts.GetCount(); i++) {

			Attribute parentAtt = parentAtts.GetAt(i);
			String parentAttName = parentAtt.GetName();

			if (parentAtt.GetStereotype().equals("Field")) {

			    SortedMap<String, EATaggedValue> parentAttTVs = EAAttributeUtil
				    .getEATaggedValuesWithCombinedKeys(parentAtt);

			    String tvKey;

			    int parentLength = 0;
			    tvKey = "Length#ArcGIS::Field::Length";
			    if (parentAttTVs.containsKey(tvKey)) {
				parentLength = Integer.parseInt(parentAttTVs.get(tvKey).getValues().get(0));
			    }

			    int parentPrecision = 0;
			    tvKey = "Precision#ArcGIS::Field::Precision";
			    if (parentAttTVs.containsKey(tvKey)) {
				parentPrecision = Integer.parseInt(parentAttTVs.get(tvKey).getValues().get(0));
			    }

			    int parentScale = 0;
			    tvKey = "Scale#ArcGIS::Field::Scale";
			    if (parentAttTVs.containsKey(tvKey)) {
				parentScale = Integer.parseInt(parentAttTVs.get(tvKey).getValues().get(0));
			    }

			    boolean parentIsNullable = false;
			    tvKey = "IsNullable#ArcGIS::Field::IsNullable";
			    if (parentAttTVs.containsKey(tvKey)) {
				parentIsNullable = Boolean.parseBoolean(parentAttTVs.get(tvKey).getValues().get(0));
			    }

			    boolean lengthChange = false;
			    boolean scaleChange = false;
			    boolean precisionChange = false;
			    boolean isNullableChange = false;

			    int maxLengthFromSubtypes = 0;
			    int maxPrecisionFromSubtypes = 0;
			    int maxScaleFromSubtypes = 0;
			    boolean isNullableFromSubtypes = false;

			    /*
			     * Determine if the corresponding (same name) fields in the subtypes - which may
			     * or may not exist - have different length, scale, precision, or isNullable.
			     * Also determine the maximum value and if isNullable should be true.
			     */
			    for (Entry<String, Element> e : subtypeElementByName.entrySet()) {

				// String subtypeName = e.getKey();
				Element subtypeElement = e.getValue();

				Attribute subtypeAtt = EAElementUtil.getAttributeByName(subtypeElement, parentAttName);

				if (subtypeAtt != null) {

				    SortedMap<String, EATaggedValue> subtypeAttTVs = EAAttributeUtil
					    .getEATaggedValuesWithCombinedKeys(subtypeAtt);

				    tvKey = "Length#ArcGIS::Field::Length";
				    if (subtypeAttTVs.containsKey(tvKey)) {
					int subtypeLength = Integer
						.parseInt(subtypeAttTVs.get(tvKey).getValues().get(0));
					if (parentLength != subtypeLength) {
					    lengthChange = true;
					    if (subtypeLength > maxLengthFromSubtypes) {
						maxLengthFromSubtypes = subtypeLength;
					    }
					}
				    }

				    tvKey = "Precision#ArcGIS::Field::Precision";
				    if (subtypeAttTVs.containsKey(tvKey)) {
					int subtypePrecision = Integer
						.parseInt(subtypeAttTVs.get(tvKey).getValues().get(0));
					if (parentPrecision != subtypePrecision) {
					    precisionChange = true;
					    if (subtypePrecision > maxPrecisionFromSubtypes) {
						maxPrecisionFromSubtypes = subtypePrecision;
					    }
					}
				    }

				    tvKey = "Scale#ArcGIS::Field::Scale";
				    if (subtypeAttTVs.containsKey(tvKey)) {
					int subtypeScale = Integer
						.parseInt(subtypeAttTVs.get(tvKey).getValues().get(0));
					if (parentScale != subtypeScale) {
					    scaleChange = true;
					    if (subtypeScale > maxScaleFromSubtypes) {
						maxScaleFromSubtypes = subtypeScale;
					    }
					}
				    }

				    tvKey = "IsNullable#ArcGIS::Field::IsNullable";
				    if (subtypeAttTVs.containsKey(tvKey)) {
					boolean subtypeIsNullable = Boolean
						.parseBoolean(subtypeAttTVs.get(tvKey).getValues().get(0));
					if (parentIsNullable != subtypeIsNullable) {
					    isNullableChange = true;
					    if (subtypeIsNullable) {
						isNullableFromSubtypes = true;
					    }
					}
				    }
				}
			    }

			    String parentName = ci.name();
			    String fieldToUpdate = parentAttName;
			    String tagToUpdate = "";
			    String fqNameOfTag = "";
			    String newValue = "";

			    try {

				if (lengthChange) {

				    tagToUpdate = "Length";
				    fqNameOfTag = "ArcGIS::Field::Length";

				    if (maxLengthFromSubtypes == 0 && parentLength > 0 && parentLength < 255) {
					newValue = "" + parentLength;
				    } else {
					newValue = "" + maxLengthFromSubtypes;
				    }

				    updateFieldInArcGISParentAndSubtypes(fieldToUpdate, tagToUpdate, fqNameOfTag,
					    newValue, parentName, parentAtt, subtypeElementByName);
				}

				if (precisionChange) {

				    tagToUpdate = "Precision";
				    fqNameOfTag = "ArcGIS::Field::Precision";
				    newValue = "" + maxPrecisionFromSubtypes;

				    updateFieldInArcGISParentAndSubtypes(fieldToUpdate, tagToUpdate, fqNameOfTag,
					    newValue, parentName, parentAtt, subtypeElementByName);
				}

				if (scaleChange) {

				    tagToUpdate = "Scale";
				    fqNameOfTag = "ArcGIS::Field::Scale";
				    newValue = "" + maxScaleFromSubtypes;

				    updateFieldInArcGISParentAndSubtypes(fieldToUpdate, tagToUpdate, fqNameOfTag,
					    newValue, parentName, parentAtt, subtypeElementByName);
				}

				if (isNullableChange) {

				    tagToUpdate = "IsNullable";
				    fqNameOfTag = "ArcGIS::Field::IsNullable";
				    newValue = isNullableFromSubtypes ? "true" : "false";

				    updateFieldInArcGISParentAndSubtypes(fieldToUpdate, tagToUpdate, fqNameOfTag,
					    newValue, parentName, parentAtt, subtypeElementByName);
				}

			    } catch (EAException e1) {
				result.addError(this, 10007, tagToUpdate, fieldToUpdate, parentName, e1.getMessage());
			    }
			}
		    }
		}
	    }
	}
    }

    private void updateFieldInArcGISParentAndSubtypes(String fieldToUpdate, String tagToUpdate, String fqNameOfTag,
	    String newValue, String parentName, Attribute parentAtt, SortedMap<String, Element> subtypeElementByName)
	    throws EAException {

	result.addInfo(this, 270, parentName, tagToUpdate, fieldToUpdate);

	String parentAttTagValue = EAAttributeUtil.taggedValue(parentAtt, tagToUpdate);
	if (!newValue.equals(parentAttTagValue)) {
	    result.addInfo(this, 271, parentName, tagToUpdate, fieldToUpdate, newValue);
	    EAAttributeUtil.updateTaggedValue(parentAtt, fqNameOfTag, newValue, false);
	}

	updateFieldTaggedValueOfArcGISSubtypes(subtypeElementByName, fieldToUpdate, tagToUpdate, fqNameOfTag, newValue);
    }

    private void updateFieldTaggedValueOfArcGISSubtypes(SortedMap<String, Element> subtypeElementByName, String attName,
	    String tvName, String fqNameOfTag, String tvValue) throws EAException {

	for (Entry<String, Element> e : subtypeElementByName.entrySet()) {

	    Element subtypeElement = e.getValue();

	    Attribute subtypeAtt = EAElementUtil.getAttributeByName(subtypeElement, attName);

	    if (subtypeAtt != null) {

		String subtypeAttTagValue = EAAttributeUtil.taggedValue(subtypeAtt, tvName);

		if (!tvValue.equals(subtypeAttTagValue)) {

		    String subtypeName = e.getKey();

		    result.addInfo(this, 269, subtypeName, attName, tvName, tvValue);

		    EAAttributeUtil.updateTaggedValue(subtypeAtt, fqNameOfTag, tvValue, false);
		}
	    }
	}
    }

    /**
     * @param arcgisSubtypeInitialValues can be <code>null</code>
     * @return map with subtype name as key and initial value as value; can be empty
     *         but not null;
     */
    private SortedMap<String, String> parseArcGISSubtypeInitialValues(String arcgisSubtypeInitialValues,
	    PropertyInfo pi) {

	SortedMap<String, String> initialValuesBySubtypeName = new TreeMap<>();

	String[] subtypeInitialValueDefinitions_escaped = arcgisSubtypeInitialValues == null ? new String[] {}
		: arcgisSubtypeInitialValues.split(REGEX_TO_SPLIT_BY_COMMA_WITH_ESCAPING);

	for (String s : subtypeInitialValueDefinitions_escaped) {

	    if (StringUtils.isNotBlank(s)) {

		String[] subtypeInitialValueDefinitionParts_escaped = s.split(REGEX_TO_SPLIT_BY_COLON_WITH_ESCAPING);

		if (subtypeInitialValueDefinitionParts_escaped.length != 2) {
		    MessageContext mc = result.addError(this, 263, pi.name());
		    if (mc != null) {
			mc.addDetail(this, -2, pi.fullNameInSchema());
		    }
		    return new TreeMap<String, String>();
		}

		String subtypeName_escaped = subtypeInitialValueDefinitionParts_escaped[0];
		String subtypeInitialValue_escaped = subtypeInitialValueDefinitionParts_escaped[1];

		if (StringUtils.isBlank(subtypeName_escaped) || StringUtils.isBlank(subtypeInitialValue_escaped)) {

		    MessageContext mc = result.addError(this, 263, pi.name());
		    if (mc != null) {
			mc.addDetail(this, -2, pi.fullNameInSchema());
		    }
		    return new TreeMap<String, String>();
		}

		/*
		 * Unescape ',' as well as ':' and '\'.
		 */
		String unescapedSubtypeName = subtypeName_escaped.replace("\\,", ",").replace("\\:", ":")
			.replace("\\\\", "\\");
		String unescapedInitialValue = subtypeInitialValue_escaped.replace("\\,", ",").replace("\\:", ":")
			.replace("\\\\", "\\");
		initialValuesBySubtypeName.put(unescapedSubtypeName, unescapedInitialValue);
	    }
	}

	return initialValuesBySubtypeName;
    }

    private Element determineNumericRange(ClassInfo ci, PropertyInfo pi, ProcessMapEntry pme) {

	Element numericRange = null;

	/*
	 * determine if pi has a name or alias that starts with a property name in the
	 * range constraint map for ci. We must check via a 'startsWith' instead of
	 * equals because flattening can change the name of a property; a prominent
	 * example is where a numeric property can have a single or interval value (then
	 * you'd have three properties instead of one after flattening). Name flattening
	 * may have switched the name of a model element with its alias, so we have to
	 * check both fields.
	 */
	Set<String> originalPropertyNames = numericRangeConstraintByPropNameByClassName.get(ci).keySet();

	for (String origPropName : originalPropertyNames) {

	    if (pi.name().startsWith(origPropName)
		    || (pi.aliasName() != null && pi.aliasName().startsWith(origPropName))) {

		// apply range constraint!

		// check if a range domain already exists - if
		// so, reuse it
		String rangeDomainName = ci.name() + "_" + origPropName + "_NumRange";

		if (numericRangeElementIdsByClassName.containsKey(rangeDomainName)) {

		    int numRangeElementId = numericRangeElementIdsByClassName.get(rangeDomainName);
		    numericRange = rep.GetElementByID(numRangeElementId);

		} else {

		    // create range domain element

		    NumericRangeConstraintMetadata nrcm = numericRangeConstraintByPropNameByClassName.get(ci)
			    .get(origPropName);

		    double minValue = ArcGISWorkspaceConstants.DEFAULT_NUM_RANGE_MIN_LOWER_BOUNDARY;
		    double maxValue = ArcGISWorkspaceConstants.DEFAULT_NUM_RANGE_MAX_UPPER_BOUNDARY;

		    if (nrcm.hasLowerBoundaryValue()) {
			if (nrcm.isLowerBoundaryInclusive()) {
			    minValue = nrcm.getLowerBoundaryValue();
			} else {
			    minValue = nrcm.getLowerBoundaryValue() + numRangeDelta;
			}
		    }

		    if (nrcm.hasUpperBoundaryValue()) {
			if (nrcm.isUpperBoundaryInclusive()) {
			    maxValue = nrcm.getUpperBoundaryValue();
			} else {
			    maxValue = nrcm.getUpperBoundaryValue() - numRangeDelta;
			}
		    }

		    try {

			// TODO parse documentation from range
			// constraint
			Element rd = this.createRangeDomain(rangeDomainName, null, pme.getTargetType(), ci);

			// create min and max fields
			EAElementUtil.createEAAttribute(rd, "MinValue", null, null, null, null, false, false, false,
				doubleToString(minValue), false, new Multiplicity(1, 1), null, null);
			EAElementUtil.createEAAttribute(rd, "MaxValue", null, null, null, null, false, false, false,
				doubleToString(maxValue), false, new Multiplicity(1, 1), null, null);

			numericRange = rd;

		    } catch (EAException e) {

			// log an error, but proceed
			result.addError(this, 230, rangeDomainName, e.getMessage());
			numericRange = null;
		    }

		}

		break;
	    }
	}

	return numericRange;
    }

    /**
     * If any specific tagged values shall be represented, and the info object has
     * such tagged values, they will be added.
     *
     * @param eaAtt
     * @param i
     * @throws EAException
     */
    private void addTaggedValuesToRepresent(Attribute eaAtt, Info i) throws EAException {

	if (ArcGISWorkspace.representTaggedValues) {

	    for (String tvToRepresent : ArcGISWorkspace.taggedValuesToRepresent) {
		String normalizedTag = options.normalizeTag(tvToRepresent);
		String[] values = i.taggedValuesForTag(normalizedTag);
		if (values.length > 0) {
		    EAAttributeUtil.setTaggedValue(eaAtt, new EATaggedValue(tvToRepresent, Arrays.asList(values)));
		}
	    }
	}
    }

    /**
     * If any specific tagged values shall be represented, and the info object has
     * such tagged values, they will be added.
     *
     * @param ce
     * @param i
     * @throws EAException
     */
    private void addTaggedValuesToRepresent(ConnectorEnd ce, Info i) throws EAException {

	if (ArcGISWorkspace.representTaggedValues) {

	    for (String tvToRepresent : ArcGISWorkspace.taggedValuesToRepresent) {
		String normalizedTag = options.normalizeTag(tvToRepresent);
		String[] values = i.taggedValuesForTag(normalizedTag);
		if (values.length > 0) {
		    EAConnectorEndUtil.setTaggedValue(ce, new EATaggedValue(tvToRepresent, Arrays.asList(values)));
		}
	    }
	}
    }

    /**
     * If any specific tagged values shall be represented, and the info object has
     * such tagged values, they will be added.
     *
     * @param con
     * @param i
     * @throws EAException
     */
    private void addTaggedValuesToRepresent(Connector con, Info i) throws EAException {

	if (ArcGISWorkspace.representTaggedValues) {

	    for (String tvToRepresent : ArcGISWorkspace.taggedValuesToRepresent) {
		String normalizedTag = options.normalizeTag(tvToRepresent);
		String[] values = i.taggedValuesForTag(normalizedTag);
		if (values.length > 0) {
		    EAConnectorUtil.setTaggedValue(con, new EATaggedValue(tvToRepresent, Arrays.asList(values)));
		}
	    }
	}
    }

    /**
     * If any specific tagged values shall be represented, and the info object has
     * such tagged values, they will be added.
     *
     * @param e
     * @param i
     * @throws EAException
     */
    private void addTaggedValuesToRepresent(Element e, Info i) throws EAException {

	if (ArcGISWorkspace.representTaggedValues) {

	    for (String tvToRepresent : ArcGISWorkspace.taggedValuesToRepresent) {
		String normalizedTag = options.normalizeTag(tvToRepresent);
		String[] values = i.taggedValuesForTag(normalizedTag);
		if (values.length > 0) {
		    EAElementUtil.setTaggedValue(e, new EATaggedValue(tvToRepresent, Arrays.asList(values)));
		}
	    }
	}
    }

    private Attribute createFieldForReflexiveRelationshipProperty(Element eaClass, PropertyInfo pi) throws EAException {

	ClassInfo ci = pi.inClass();

	if (pi.cardinality().maxOccurs > 1) {
	    result.addWarning(this, 249, pi.name(), ci.name());
	}

	String normalizedName = normalizeName(pi.name() + reflexiveRelationshipAttributeSuffix);

	if (exceedsMaxLength(normalizedName)) {
	    this.result.addWarning(this, 205, normalizedName, pi.name(), ci.name(), "" + maxNameLength);
	    normalizedName = clipToMaxLength(normalizedName);
	}

	String normalizedAlias = pi.aliasName() == null ? null : normalizeAlias(pi.aliasName(), ci);

	String documentation = pi.derivedDocumentation(documentationTemplate, documentationNoValue);

	return createForeignKeyField(eaClass, normalizedName, normalizedAlias, documentation, ci);
    }

    private Attribute createForeignKeyField(Element eaClass, String normalizedName, String normalizedAlias,
	    String documentation, ClassInfo targetType) throws EAException {

	String primaryKeyAttGUID = determinePrimaryKeyGUID(targetType);
	Attribute primaryKeyAtt = rep.GetAttributeByGuid(primaryKeyAttGUID);

	String primaryKeyType = primaryKeyAtt.GetType();

	String length = StringUtils.stripToEmpty(EAAttributeUtil.taggedValue(primaryKeyAtt, "Length"));
	String precision = StringUtils.stripToEmpty(EAAttributeUtil.taggedValue(primaryKeyAtt, "Precision"));
	String scale = StringUtils.stripToEmpty(EAAttributeUtil.taggedValue(primaryKeyAtt, "Scale"));

	String refType;
	if (primaryKeyType.equalsIgnoreCase("esriFieldTypeOID")) {

	    /*
	     * We do not want to create a new field with type ObjectID. We want to refer to
	     * a row that has a specific ObjectID. Since an ObjectID is an integer, we use
	     * esriFieldTypeInteger, with according length, precision, and scale.
	     */
	    refType = "esriFieldTypeInteger";
	    length = "0";
	    precision = "9";
	    scale = "0";

	} else if (primaryKeyType.equalsIgnoreCase("esriFieldTypeGlobalID")) {

	    /*
	     * We do not want to create a new field with type GlobalID. We want to refer to
	     * a row that has a specific GlobalID. Since a GlobalID is a GUID, we use
	     * esriFieldTypeGUID.
	     */
	    refType = "esriFieldTypeGUID";

	} else {

	    refType = primaryKeyType;
	}

	return createField(eaClass, normalizedName, normalizedAlias, documentation, refType, length, precision, scale,
		null, null, true);
    }

    private void createAttributeIndex(Element eaClass, ClassInfo ci, Attribute eaAtt, PropertyInfo pi) {

	try {
	    List<EATaggedValue> tvs = new ArrayList<EATaggedValue>();

	    tvs.add(new EATaggedValue("IsUnique", "ArcGIS::AttributeIndex::IsUnique", "true"));
	    tvs.add(new EATaggedValue("IsAscending", "ArcGIS::AttributeIndex::IsAscending", "true"));
	    tvs.add(new EATaggedValue("Fields", "ArcGIS::AttributeIndex::Fields", eaAtt.GetAttributeGUID()));

	    SortedSet<String> stereotypes = new TreeSet<String>();
	    stereotypes.add("ArcGIS::AttributeIndex");

	    EAElementUtil.createEAAttribute(eaClass, eaClass.GetName() + "_" + eaAtt.GetName() + "_IDX", null, null,
		    stereotypes, tvs, false, false, false, null, false, new Multiplicity(1, 1), "", null);

	} catch (EAException e) {
	    result.addError(this, 10004, pi.name(), ci.name(), e.getMessage());
	}
    }

    @Override
    public void registerRulesAndRequirements(RuleRegistry r) {
	r.addRule("rule-arcgis-all-postprocess-removeUnusedCodedValueDomains");
	r.addRule("rule-arcgis-all-precision");
	r.addRule("rule-arcgis-all-relationshipClassNameByTaggedValueOfClasses");
	r.addRule("rule-arcgis-all-representTaggedValues");
	r.addRule("rule-arcgis-all-scale");
	r.addRule("rule-arcgis-all-subtypes");
	r.addRule("rule-arcgis-cls-hasM");
	r.addRule("rule-arcgis-cls-hasZ");
	r.addRule("rule-arcgis-cls-identifierStereotype");
	r.addRule("rule-arcgis-cls-rangeDomainFromTaggedValues");
	r.addRule("rule-arcgis-prop-attIndex");
	r.addRule("rule-arcgis-prop-initialValue");
	r.addRule("rule-arcgis-prop-initialValueByAlias");
	r.addRule("rule-arcgis-prop-isNullable");
	r.addRule("rule-arcgis-prop-lengthFromCodesOrEnumsOfValueType");
	r.addRule("rule-arcgis-prop-lengthFromTaggedValue");
	r.addRule("rule-arcgis-prop-lengthFromTaggedValueForCodelistOrEnumerationValueType");
	r.addRule("rule-arcgis-prop-precision"); // deprecated
	r.addRule("rule-arcgis-prop-reflexiveRelationshipAsField");
	r.addRule("rule-arcgis-prop-scale"); // deprecated
    }

    @Override
    public String getTargetIdentifier() {
	return "arcgis";
    }

    @Override
    public String getDefaultEncodingRule() {
	return "*";
    }

    @Override
    public String message(int mnr) {

	/**
	 * Number ranges defined as follows:
	 * <ul>
	 * <li>1-100: Initialization related messages</li>
	 * <li>101-200: ArcGIS workspace template related messages</li>
	 * <li>201-300: other messages</li>
	 * <li>10001-10100: EA exceptions
	 * </ul>
	 */

	switch (mnr) {

	case -2:
	    return "Context: property '$1$'";
	case -1:
	    return "Context: class '$1$'";
	case 0:
	    return "Context: class ArcGISWorkspace";

	// 1-100: Initialization related messages
	case 1:
	    return "Directory named '$1$' does not exist or is not accessible.";
	case 2:
	    return "Output file '$1$' already exists in output directory ('$2$'). It will be deleted prior to processing.";
	case 3:
	    return "File has been deleted.";
	case 4:
	    return "File could not be deleted. Exception message: '$1$'.";
	case 5:
	    return "Could not create output directory. Exception message: '$1$'.";
	case 6:
	    return "URL '$1$' provided for configuration parameter " + ArcGISWorkspaceConstants.PARAM_WORKSPACE_TEMPLATE
		    + " is malformed. Execution will be aborted. Exception message is: '$2$'.";
	case 7:
	    return "EA repository file with ArcGIS workspace template at '$1$' does not exist or cannot be read. Check the value of the configuration parameter '"
		    + ArcGISWorkspaceConstants.PARAM_WORKSPACE_TEMPLATE
		    + "' and ensure that: a) it contains the path to the template file and b) the file can be read by ShapeChange.";
	case 8:
	    return "Exception encountered when copying ArcGIS workspace template EA repository file to output destination. Message is: $1$.";
	case 9:
	    return "No value provided for configuration parameter '$1$', defaulting to: '$2$'.";
	case 10:
	    return "Encountered package '$1$' (child of package '$2$') which is an application schema. The package will be ignored.";
	case 11:
	    return "Target configuration map entry for type '$1$' does not have a target type. The map entry will be ignored.";
	case 12:
	    return "Value of configuration parameter '" + ArcGISWorkspaceConstants.PARAM_VALUE_RANGE_DELTA
		    + "' could not be parsed as a double value. The default value of "
		    + ArcGISWorkspaceConstants.NUM_RANGE_DELTA + " will be used for processing.";
	case 13:
	    return "Value of configuration parameter '$1$' could not be parsed as an integer value. The default value of '$2$' will be used for processing.";

	// 101-200: ArcGIS workspace template related messages
	case 101:
	    return "Invalid ArcGIS workspace template: expected package with stereotype <<ArcGIS>> as child of root package.";
	case 102:
	    return "Invalid ArcGIS workspace template: could not find required package '$1$'.";

	// 201-300: other messages
	case 201:
	    return "Class '$1$' will be ignored.";
	case 202:
	    // see also 206
	    return "Unknown subtype encountered for class '$1$' - ID of subtype is '$2$'.";
	case 203:
	    return "Different ArcGIS geometry types encountered in supertypes of class '$1$'. Geometry type will be determined based upon first occurrence of supertype with geometry type that is not 'unknown' (or 'unknown' will be used if no such supertype exists).";
	case 204:
	    return "Could not set abstract on class '$1$'. Exception message is: '$2$'.";
	case 205:
	    return "Length of normalized name '$1$' (original name is '$2$') of property in class '$3$' or of the class itself exceeds maximum length restriction (which is $4$ characters). The name will be clipped to fit the maximum length.";
	case 206:
	    // see also 202
	    return "Unknown supertype encountered for class '$1$' - ID of supertype is '$2$'.";
	case 207:
	    return "Generalisation '$1$' - '$2$' not set because the first class is not part of the target model.";
	case 208:
	    return "Generalisation '$1$' - '$2$' not set because the second class is not part of the target model.";
	case 209:
	    return "Processing class with category $1$ is not supported. Class '$2$' will be ignored.";
	case 210:
	    return "Class '$1$' has geometry property of unknown type. The class will be ignored.";
	case 211:
	    return "Class '$1$' has multiple geometry properties. All but one will be ignored.";
	case 212:
	    return "Property '$1$' of class '$2$' has max occurrence > 1. The property will be ignored.";
	case 213:
	    return "Property '$1$' of class '$2$' is of a geometry type. The property will be ignored.";
	case 214:
	    return "Property '$1$' of class '$2$' is of a <<union>> type. The property will be ignored.";
	case 215:
	    return "Property '$1$' of class '$2$' is of a <<dataType>> type. The property will be ignored.";
	case 216:
	    return "Class '$1$' is the type of property '$2$' (from class '$3$') but was not found in the model. A proper link to '$1$' cannot be set.";
	case 217:
	    return "Unrecognized case of property conversion. Context is property '$1$' (category of value is '$2$') in class '$3$'.";
	case 218:
	    return "Cannot establish a <<RelationshipClass>> association for property '$1$' in class '$2$' to class '$3$' (which is the type of the property) because that class is not part of the application schema.";
	case 219:
	    return "Subtype with id '$1$' of type '$2$' not found in the model. Cannot create a <<RelationshipClass>> association for this subtype.";
	case 220:
	    return "Subtype '$1$' of type '$2$' is not part of the application schema. Cannot create a <<RelationshipClass>> association for this subtype.";
	case 221:
	    return "Could not properly establish <<RelationshipClass>> association between classes '$1$' and '$2$' due to an EA exception. Error message is: $3$";
	case 222:
	    return "Type '$1$' of property '$2$' in class '$3$' does not belong to the application schema. The property will be ignored.";
	case 223:
	    return "Association between classes '$1$' and '$2$' will be ignored because at least one of the two classes is not a feature or object type.";
	case 224:
	    return "Association between classes '$1$' and '$2$' will be ignored because at least one of the two classes is not contained in the application schema.";
	case 225:
	    return "Cannot create association between classes '$1$' and '$2$' because at least one of them has not been established in the ArcGIS workspace (the reason could be that the class is not part of the application schema).";
	case 226:
	    return "Length of normalized name '$1$' for new association class (as well as the according relationship class) exceeds maximum length restriction (which is $2$ characters). The name will be clipped to fit the maximum length.";
	case 227:
	    return "Length of normalized name '$1$' for foreign key field in new association class exceeds maximum length restriction (which is $2$ characters). The name will be clipped to fit the maximum length.";
	case 228:
	    return "Detected numeric range constraint in class '$1$' but could not find the property name in it. OCL is: $2$";
	case 229:
	    return "Detected numeric range constraint for property named '$1$' in class '$2$' but could not actually find the property in that class. The property name has been parsed from the OCL text, which is: $3$";
	case 230:
	    return "Could not create <<RangeDomain>> with name '$1$' due to an EA exception: $2$";
	case 231:
	    return "Could not parse lower boundary value '$1$' in numeric range constraint to a double value. Class that contains the constraint is: '$2$'. Constraint name is: '$3$'. OCL is: $4$.";
	case 232:
	    return "Could not parse upper boundary value '$1$' in numeric range constraint to a double value. Class that contains the constraint is: '$2$'. Constraint name is: '$3$'. OCL is: $4$.";
	case 233:
	    return "Association between class '$1$' (which is the inClass for property '$2$') and class '$3$' (which is the type of the property) will be ignored because class '$3$' is not contained in the application schema.";
	case 234:
	    return "Length of normalized name '$1$' for new relationship class exceeds maximum length restriction (which is $2$ characters). The name will be clipped to fit the maximum length.";
	case 235:
	    return "No conversion rule is configured to handle the reflexive association found on class '$1$'. The association will be ignored.";
	case 236:
	    return "No conversion rule is configured to handle the reflexive relationship found on class '$1$', property '$2$'. The property will be ignored.";
	case 237:
	    return "Cannot create one to many relationship between classes '$1$' and '$2$' because class '$3$' has not been established in the ArcGIS workspace (the reason could be that the class is not part of the application schema).";
	case 238:
	    return "One to many relationship between classes '$1$' and '$2$' is incomplete. Could not create relationship between '$3$' and '$4$' because class '$3$' has not been established in the ArcGIS workspace (the reason could be that the class is not part of the application schema).";
	case 239:
	    return "Many to many relationship between classes '$1$' and '$2$' is incomplete. Could not create relationship between '$3$' and '$4$' because class '$3$' has not been established in the ArcGIS workspace (the reason could be that the class is not part of the application schema).";
	case 240:
	    return "Type '$1$' has been mapped to '$2$', as defined by the configuration.";
	case 241:
	    return "Could not parse lower boundary value '$1$' in tagged value '$2$' to a double value. The tagged value will be ignored.";
	case 242:
	    return "Could not parse upper boundary value '$1$' in tagged value '$2$' to a double value. The tagged value will be ignored.";
	case 243:
	    return "Could not parse value '$1$' of tagged value '$2$' to an integer value. The tagged value will be ignored.";
	case 244:
	    return "Could not find the code list or enumeration that is the value type '$1$' of property '$2$' in the model. The length can therefore not be computed from the codes/enums.";
	case 245:
	    return "Tagged value '" + ArcGISWorkspaceConstants.TV_NUMERIC_TYPE
		    + "' is not blank. It has value '$1$'. No map entry was found with this value as type. The tagged value will be ignored.";
	case 246:
	    return "Multiple attributes with stereotype <<identifier>> found for class '$1$'. The first - arbitrary one - will be used as primary key in relationship classes.";
	case 247:
	    return "Identifier attribute '$1$' has max multiplicity > 1.";
	case 248:
	    return "Class '$1$' does not have an <<identifier>> attribute.";
	case 249:
	    return "Reflexive relationship property '$1$' of class '$2$' has max cardinality > 1. The <<Field>> that is created for the property will only support representation of a single relationship.";
	case 250:
	    return "Could not add tagged values to represent on class '$1$'. Exception message is: '$2$'.";
	case 251:
	    return "Could not add tagged values to represent on property '$1$'. Exception message is: '$2$'.";
	case 252:
	    return "Class '$1$' is an ArcGIS Subtype. Therefore, it must define tagged value 'arcgisSubtypeCode' with an integer that uniquely identifies the subtype amongst the other children of its parent. The tagged value is undefined or empty. Using subtype code -1.";
	case 253:
	    return "Class '$1$' is an ArcGIS Subtype. Value '$2$' of tag 'arcgisSubtypeCode' cannot be parsed to an integer. Using subtype code -1.";
	case 254:
	    return "Enumeration '$1$' defines a set of subtypes. This enumeration will not be encoded as a coded value domain.";
	case 255:
	    return "Code list '$1$' defines a set of subtypes. This code list will not be encoded as a coded value domain.";
	case 256:
	    return "Value '$1$' of tag 'arcgisDefaultSubtype' cannot be parsed to an integer. Using subtype code -1.";
	case 257:
	    return "Class '$1$' is supposed to define a set of subtypes, but actually has no properties.";
	case 258:
	    return "Property '$1$' of class '$2$' defines an ArcGIS Subtype. Therefore, it must have tagged value 'arcgisSubtypeCode' with an integer that uniquely identifies the subtype. The tagged value is undefined or empty. Ignoring this subtype.";
	case 259:
	    return "Property '$1$' of class '$2$' defines an ArcGIS Subtype. Value '$3$' of tag 'arcgisSubtypeCode' cannot be parsed to an integer. Ignoring this subtype.";
	case 260:
	    return "Duplicate subtype code '$1$' detected in properties of class '$2$' which defines a set of ArcGIS Subtypes. Subtype '$3$' will be ignored.";
	case 261:
	    return "Class '$1$' is the parent of ArcGIS subtypes. Its property '$2$' has a codelist or enumeration as type, whose name is '$3$'. That type was not found in the model. Consequently, it is not possible to check if the codes/enums only apply to specific subtypes.";
	case 262:
	    return "Length of normalized name '$1$' (full name would be '$2$') of coded value domain for subtype '$3$' exceeds maximum length restriction (which is $4$ characters). The name will be clipped to fit the maximum length.";
	case 263:
	    return "??Invalid format of tagged value 'arcgisSubtypeInitialValues' of property '$1$'. The tagged value will be ignored. Ensure that the tagged value contains a comma-separated list of key-value pairs (with subtype name as key, initial value as value, and colon as separator).";
	case 264:
	    return "??Tagged value 'arcgisSubtypeInitialValues' of property '$1$' contains subtype '$2$', which was not found in the set of subtypes defined for class '$3$'. The subtype will be ignored.";
	case 265:
	    return "Class '$1$' is an explicitly modelled ArcGIS subtype of class '$2$'. '$1$' defines property '$3$', but '$2$' does not. This is not allowed. An ArcGIS subtype may only restrict the properties of its supertype. The property will be ignored.";
	case 266:
	    return "Class '$1$' is an explicitly modelled ArcGIS subtype of class '$2$'. '$1$' defines property '$3$', which has a geometry type ('$4$'). This is not allowed. An ArcGIS subtype may not redefine the geometry type of its supertype. The property will be ignored.";
	case 267:
	    return "Class '$1$' is the supertype of a set of explicitly modelled ArcGIS subtypes. Its property '$2$' has non-empty tagged value 'arcgisDefaultSubtype'. However, the type of that property is '$3$' instead of 'Integer'. Integer will be used as type.";
	case 268:
	    return "Class '$1$' has more than one supertype. The target only supports one generalization relationship per subtype.";
	case 269:
	    return "--- Subtype '$1$': setting tag '$2$' of field '$3$' to value '$4$'.";
	case 270:
	    return "Change of '$2$' required for field '$3$' of ArcGIS parent '$1$' and/or (one or more of) its subtypes.";
	case 271:
	    return "--- Parent '$1$': setting tag '$2$' of field '$3$' to value '$4$'.";
	case 272:
	    return "Removing coded value domain '$1$'.";

	// 10001-10100: EA exceptions
	case 10001:
	    return "EA exception encountered: $1$";
	case 10002:
	    return "EA exception encountered while creating generalization relationship between classes '$1$' and '$2$': $3$";
	case 10003:
	    return "EA exception encountered while creating <<Field>> attribute for property '$1$' in class '$2$'. The property will be ignored. Error message: $3$";
	case 10004:
	    return "EA exception encountered while creating <<AttributeIndex>> attribute for property '$1$' in class '$2$'. Error message: $3$";
	case 10005:
	    return "EA exception encountered while creating <<Field>> attribute for reflexive relationship property '$1$' in class '$2$'. The property will be ignored. Error message: $3$";
	case 10006:
	    return "EA exception encountered while creating generalization relationship between class '$1$' and its subtype '$2$': $3$";
	case 10007:
	    return "EA exception encountered while updating tag '$1$' in field '$2$' of ArcGIS parent '$3$' and its subtypes. Error message: $4$";

	// 20001 - 20100: message context
	case 20001:
	    return "Property: $1$";

	case 30000:
	    return "=============== Postprocessing ===============";
	case 30001:
	    return "---------- Removing unused coded value domains  ----------";
	case 30002:
	    return "---------- Aligning field length, scale, precision, isNullable of ArcGIS parent and subtypes  ----------";

	default:
	    return "(" + ArcGISWorkspace.class.getName() + ") Unknown message with number: " + mnr;
	}
    }
}
