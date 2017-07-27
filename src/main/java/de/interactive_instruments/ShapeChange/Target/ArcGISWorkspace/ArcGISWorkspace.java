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
package de.interactive_instruments.ShapeChange.Target.ArcGISWorkspace;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.sparx.Attribute;
import org.sparx.Collection;
import org.sparx.Connector;
import org.sparx.ConnectorEnd;
import org.sparx.Element;
import org.sparx.Repository;
import org.sparx.Package;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Multiplicity;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessConfiguration;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.Model.AssociationInfo;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.SingleTarget;
import de.interactive_instruments.ShapeChange.Util.EAException;
import de.interactive_instruments.ShapeChange.Util.EAModelUtil;
import de.interactive_instruments.ShapeChange.Util.EATaggedValue;

/**
 * @author Johannes Echterhoff
 *
 */
public class ArcGISWorkspace implements SingleTarget, MessageSource {

	/* ------------------------------------------------------ */
	/* --- Rules to modify or extend the default behavior --- */
	/* ------------------------------------------------------ */

	/**
	 * If this rule is enabled, the initial value for a <<DomainCodedValue>>,
	 * which is an attribute of a <<CodedValueDomain>> that results from
	 * conversion of enumerations and code lists from the application schema, is
	 * taken from the alias of the respective enums and codes, rather than from
	 * the initial value defined in the application schema.
	 */
	public static final String RULE_ENUM_INITIAL_VALUE_BY_ALIAS = "rule-arcgis-prop-initialValueByAlias";

	/**
	 * If a feature type has the tagged value 'HasZ' set to 'true', and the
	 * feature type is converted to an ArcGIS feature class (Point, Polyline,
	 * etc.), then with this rule enabled the ArcGIS feature class will have the
	 * tagged value 'HasZ' set to 'true' (default is 'false').
	 */
	public static final String RULE_CLS_HASZ = "rule-arcgis-cls-hasZ";

	/**
	 * If a feature type has the tagged value 'HasM' set to 'true', and the
	 * feature type is converted to an ArcGIS feature class (Point, Polyline,
	 * etc.), then with this rule enabled the ArcGIS feature class will have the
	 * tagged value 'HasM' set to 'true' (default is 'false').
	 */
	public static final String RULE_CLS_HASM = "rule-arcgis-cls-hasM";

	/**
	 * Identifies range domains for class properties based upon the tagged
	 * values 'rangeMinimum' and 'rangeMaximum'. Each boundary is inclusive. If
	 * one of the tagged value is not provided, the default value for that
	 * boundary is used. If both tagged values are empty, a range domain is not
	 * created. This rule overrides the range domain parsed from an OCL
	 * constraint, if the tagged values also specify a range domain for that
	 * property.
	 */
	public static final String RULE_CLS_RANGE_DOMAIN_FROM_TAGGED_VALUES = "rule-arcgis-cls-rangeDomainFromTaggedValues";

	/**
	 * If this rule is enabled, ShapeChange will use the value of the tagged
	 * value 'size' (must be an integer) to populate the ‘length’ tagged value
	 * of the &lt;&lt;field&gt;&gt; that will represent the property in the
	 * ArcGIS model. NOTE: Only applies to properties that are implemented as
	 * fields with type esriFieldTypeString. If the value is 0 or empty,
	 * unlimited length is assumed - unless an OCL constraint exists that
	 * restricts the length for the property. That also means that this rule has
	 * precedence over an OCL constraint: if the tagged value 'size' has an
	 * integer value > 1, then this value will be used as the length in the
	 * &lt;&lt;field&gt;&gt;.
	 */
	public static final String RULE_PROP_LENGTH_FROM_TAGGED_VALUE = "rule-arcgis-prop-lengthFromTaggedValue";

	/**
	 * If this rule is enabled, then - for properties with a code list or
	 * enumeration as value type - ShapeChange will use the value of the tagged
	 * value 'size' (must be an integer) to populate the ‘length’ tagged value
	 * of the &lt;&lt;field&gt;&gt; that will represent the property in the
	 * ArcGIS model. This rule has higher priority than
	 * {@value #RULE_PROP_LENGTH_FROM_CODES_OR_ENUMS_OF_VALUE_TYPE}. If none of
	 * these rules apply, the length will be set to 0.
	 */
	public static final String RULE_PROP_LENGTH_FROM_TAGGED_VALUE_FOR_CODELIST_OR_ENUMERATION_VALUE_TYPE = "rule-arcgis-prop-lengthFromTaggedValueForCodelistOrEnumerationValueType";

	/**
	 * If this rule is enabled then the length of a property that has a code
	 * list or enumeration as value type is computed as the maximum name length
	 * from the codes/enums of the value type (if codes/enums are defined by
	 * that type). This rule has lower priority than
	 * {@value #RULE_PROP_LENGTH_FROM_TAGGED_VALUE_FOR_CODELIST_OR_ENUMERATION_VALUE_TYPE}
	 * If none of these rules apply, the length will be set to 0.
	 */
	public static final String RULE_PROP_LENGTH_FROM_CODES_OR_ENUMS_OF_VALUE_TYPE = "rule-arcgis-prop-lengthFromCodesOrEnumsOfValueType";

	public static final String RULE_PROP_INITIAL_VALUE = "rule-arcgis-prop-initialValue";

	public static final String RULE_PROP_PRECISION = "rule-arcgis-prop-precision";

	public static final String RULE_PROP_SCALE = "rule-arcgis-prop-scale";

	public static final String RULE_PROP_ISNULLABLE = "rule-arcgis-prop-isNullable";

	/**
	 * If this rule is enabled, then the base name of a relationship class will
	 * be constructed from the short names of the source and target class,
	 * combined by an underscore. The short name of a class is given via the
	 * tagged value specified by parameter
	 * {@value #PARAM_SHORT_NAME_BY_TAGGED_VALUE}. If no short name is
	 * specified, the original class name will be used as fallback. Note that
	 * the base name can be subject to additional modifications (such as
	 * normalization, addition of suffix to make the name unique, and clipping
	 * in case that the name exceeds the allowed length).
	 */
	public static final String RULE_ALL_RELCLASSNAME_BY_TAGGEDVALUE_OF_CLASSES = "rule-arcgis-all-relationshipClassNameByTaggedValueOfClasses";

	/* ------------------------------------------- */
	/* --- configuration parameter identifiers --- */
	/* ------------------------------------------- */

	/* --- parameters required for / available in default behavior --- */
	/**
	 * Optional (defaults to 255) - Default length to set in the 'length' tagged
	 * value of <<field>>s that have a textual value, in case that there is no
	 * OCL constraint that defines the length.
	 */
	public static final String PARAM_LENGTH_TAGGED_VALUE_DEFAULT = "defaultLength";

	/**
	 * Optional (defaults to 'size') - Name of the tagged value that is used to
	 * determine the length of a &lt;&lt;field&gt;&gt; that represents a
	 * property under {@value #RULE_PROP_LENGTH_FROM_TAGGED_VALUE}.
	 */
	public static final String PARAM_NAME_OF_TV_TO_DETERMINE_FIELD_LENGTH = "nameOfTaggedValueToDetermineFieldLength";

	/**
	 * Optional (defaults to 0.01) - Delta to add to / subtract from a range
	 * limit in case that the lower and/or upper boundary comparison operator is
	 * not inclusive.
	 */
	public static final String PARAM_VALUE_RANGE_DELTA = "valueRangeExcludedBoundaryDelta";
	/**
	 * Optional (default is the current run directory) - The path to the folder
	 * in which the resulting ArcGIS workspace (UML) model will be created.
	 */
	public static final String PARAM_OUTPUT_DIR = "outputDirectory";
	/**
	 * Optional (defaults to "ArcGISWorkspace.eap") The name of the output file.
	 * ShapeChange will append the file extension '.eap' as suffix if the file
	 * name does not already contain it.
	 */
	public static final String PARAM_OUTPUT_FILENAME = "outputFilename";
	/**
	 * Optional (defaults to
	 * "http://shapechange.net/resources/templates/ArcGISWorkspace_template.eap"
	 * ) - Path to the ArcGIS workspace UML model template file (can be local or
	 * an online resource).
	 */
	public static final String PARAM_WORKSPACE_TEMPLATE = "workspaceTemplate";
	public static final String WORKSPACE_TEMPLATE_URL = "http://shapechange.net/resources/templates/ArcGISWorkspace_template.eap";
	/**
	 * Optional changes to the default documentation template and the default
	 * strings for descriptors without value
	 */
	public static final String PARAM_DOCUMENTATION_TEMPLATE = "documentationTemplate";
	public static final String PARAM_DOCUMENTATION_NOVALUE = "documentationNoValue";

	public static final String PARAM_MAX_NAME_LENGTH = "maxNameLength";

	/**
	 * Name of the tagged value that provides the short name for a model
	 * element, when used in constructing specific names of the ArcGIS
	 * workspace. Default is 'shortName'.
	 */
	public static final String PARAM_SHORT_NAME_BY_TAGGED_VALUE = "shortNameByTaggedValue";

	/* --------------------------------------------------------------- */
	/* --- Constants for elements of the ArcGIS workspace template --- */
	/* --------------------------------------------------------------- */

	public static final String TEMPLATE_PKG_FEATURES_NAME = "Features";
	public static final String TEMPLATE_PKG_DOMAINS_NAME = "Domains";
	public static final String TEMPLATE_PKG_TABLES_NAME = "Tables";
	public static final String TEMPLATE_PKG_ASSOCIATION_CLASSES_NAME = "Association Classes";

	/* ------------------------------------ */
	/* --- ArcGIS Workspace Stereotypes --- */
	/* ------------------------------------ */

	public static final String STEREOTYPE_RELATIONSHIP_CLASS = "RelationshipClass";

	public static final String STEREOTYPE_DOMAIN_CODED_VALUE = "DomainCodedValue";

	/* ----------------------- */
	/* --- Other constants --- */
	/* ----------------------- */

	public static final int DEFAULT_MAX_NAME_LENGTH = 30;

	public static final double NUM_RANGE_DELTA = 0.01;

	public static final Double DEFAULT_NUM_RANGE_MIN_LOWER_BOUNDARY = new Double(
			-1000000000);
	public static final Double DEFAULT_NUM_RANGE_MAX_UPPER_BOUNDARY = new Double(
			1000000000);

	public static final int LENGTH_TAGGED_VALUE_DEFAULT = 255;

	public static final String ILLEGAL_NAME_CHARACTERS_DETECTION_REGEX = "\\W";

	/* -------------------- */
	/* --- enumerations --- */
	/* -------------------- */

	public enum ArcGISGeometryType {
		POINT("Point"), MULTIPOINT("Multipoint"), POLYLINE("Polyline"), POLYGON(
				"Polygon"), UNKNOWN("Unknown"), NONE("None");

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
	 * Pattern to parse length constraints for property values, i.e. the
	 * property name and maximum length.
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
	 * <li>group 1: comparison operator for lower boundary (either '>' or '>=')
	 * </li>
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
	 * <li>group 1: comparison operator for upper boundary (either '<' or '<=')
	 * </li>
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
	private static String workspaceTemplateFilePath = WORKSPACE_TEMPLATE_URL;

	private static int maxNameLength = DEFAULT_MAX_NAME_LENGTH;
	private static int lengthTaggedValueDefault = LENGTH_TAGGED_VALUE_DEFAULT;

	private static double numRangeDelta = NUM_RANGE_DELTA;

	private static Set<String> esriTypesSuitedForRangeConstraint = new HashSet<String>();

	private static String outputDirectory = null;
	private static File outputDirectoryFile = null;
	private static String documentationTemplate = null;
	private static String documentationNoValue = null;

	private static Repository rep = null;

	private static Set<ClassInfo> ignoredCis = new HashSet<ClassInfo>();

	private static Map<ClassInfo, ArcGISGeometryType> geometryTypeCache = new HashMap<ClassInfo, ArcGISGeometryType>();

	private static Map<ClassInfo, Integer> elementIdByClassInfo = new HashMap<ClassInfo, Integer>();
	private static Map<ClassInfo, String> elementNameByClassInfo = new HashMap<ClassInfo, String>();

	private static Map<ClassInfo, String> objectIdAttributeGUIDByClass = new HashMap<ClassInfo, String>();
	private static Map<ClassInfo, ClassInfo> generalisations = new HashMap<ClassInfo, ClassInfo>();
	private static Set<AssociationInfo> associations = new HashSet<AssociationInfo>();

	/**
	 * key: name that would usually be assigned to a relationship class; value:
	 * counter for the number of occurrences of this particular name (assume 0
	 * if a name is not contained as key yet)
	 */
	private static Map<String, Integer> counterByRelationshipClassName = new HashMap<String, Integer>();

	/**
	 * key: class info object; value: map that keeps track of property names
	 * used [key: name that would usually be assigned to the property; value:
	 * counter for the number of occurrences of this particular name (assume 0
	 * if a name is not contained as key yet)]
	 */
	private static Map<ClassInfo, Map<String, Integer>> counterByPropertyNameByClass = new HashMap<ClassInfo, Map<String, Integer>>();

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
	 * &lt;&lt;FeatureDataset&gt;&gt; package where all feature types with
	 * supported ArcGIS geometry are stored (in package itself or sub-packages
	 * according to package hierarchy in the application schema).
	 */
	protected static Integer featuresPkgId;

	/**
	 * Package where all feature types without geometry and object types are
	 * stored (in package itself or sub-packages according to package hierarchy
	 * in the application schema).
	 */
	protected static Integer tablesPkgId;

	/**
	 * Package where all association classes used to represent n:m relationships
	 * are stored.
	 */
	protected static Integer assocClassesPkgId;

	/**
	 * Package where all code lists and enumerations are stored (in package
	 * itself or sub-packages according to package hierarchy in the application
	 * schema).
	 */
	protected static Integer domainsPkgId;

	/**
	 * key: workspace sub package; value: {key: application schema package;
	 * value: corresponding EA package within the workspace sub package}
	 */
	protected static Map<Integer, Map<PackageInfo, Integer>> eaPkgIdByModelPkg_byWorkspaceSubPkgId = new HashMap<Integer, Map<PackageInfo, Integer>>();

	/**
	 * TODO: value of 'rule' attribute is currently ignored
	 * 
	 * key: 'type' attribute value of map entry defined for the target; value:
	 * according map entry
	 */
	protected static Map<String, ProcessMapEntry> processMapEntries = null;

	protected static Map<String, Integer> lengthMappingByTypeName = new HashMap<String, Integer>();
	protected static Map<String, Integer> precisionMappingByTypeName = new HashMap<String, Integer>();
	protected static Map<String, Integer> scaleMappingByTypeName = new HashMap<String, Integer>();

	/**
	 * Contains information about the maximum length of a property value
	 * (usually of a textual type).
	 * 
	 * key: {class name}_{property name}; value: the max length of the property
	 * value
	 */
	protected static Map<String, Integer> lengthByClassPropName = new HashMap<String, Integer>();

	/**
	 * Contains information about the numeric ranges defined for specific class
	 * properties via OCL constraints.
	 * 
	 * key: class; value: map with [key: property name; value: the numeric range
	 * for the property]
	 */
	protected static Map<ClassInfo, Map<String, NumericRangeConstraintMetadata>> numericRangeConstraintByPropNameByClassName = new HashMap<ClassInfo, Map<String, NumericRangeConstraintMetadata>>();

	protected static String nameOfTVToDetermineFieldLength = "size";

	private static String absolutePathOfOutputEAPFile;

	private static String shortNameByTaggedValue;

	/**
	 * key: name of the class element; value: the range domain element
	 */
	private static Map<String, Integer> numericRangeElementIdsByClassName = new HashMap<String, Integer>();

	// TODO Unit Test

	public void initialise(PackageInfo p, Model m, Options o,
			ShapeChangeResult r, boolean diagOnly)
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
			outputDirectory = options.parameter(this.getClass().getName(),
					PARAM_OUTPUT_DIR);
			if (outputDirectory == null)
				outputDirectory = options.parameter("outputDirectory");
			if (outputDirectory == null)
				outputDirectory = options.parameter(".");

			String outputFilename = options.parameter(this.getClass().getName(),
					"outputFilename");
			if (outputFilename == null) {
				outputFilename = p.name();
			}
			outputFilename = outputFilename.replace("/", "_").replace(" ", "_")
					+ ".eap";

			// parse default length parameter
			String defaultLengthParamValue = options.parameter(
					this.getClass().getName(),
					PARAM_LENGTH_TAGGED_VALUE_DEFAULT);
			if (defaultLengthParamValue != null) {

				try {

					int length = Integer.parseInt(defaultLengthParamValue);
					lengthTaggedValueDefault = length;

				} catch (NumberFormatException e) {

					result.addError(this, 13, PARAM_LENGTH_TAGGED_VALUE_DEFAULT,
							"" + LENGTH_TAGGED_VALUE_DEFAULT);
				}
			}

			// parse max name length parameter
			String maxNameLengthParamValue = options.parameter(
					this.getClass().getName(), PARAM_MAX_NAME_LENGTH);
			if (maxNameLengthParamValue != null) {
				try {
					int maxNameLengthTmp = Integer
							.parseInt(maxNameLengthParamValue);
					maxNameLength = maxNameLengthTmp;
				} catch (NumberFormatException e) {
					result.addError(this, 13, PARAM_MAX_NAME_LENGTH,
							"" + DEFAULT_MAX_NAME_LENGTH);
				}
			}

			// check parameter with name of the tagged value that determines the
			// field length
			String nameOfTVToDetermineFieldLengthParamValue = options.parameter(
					this.getClass().getName(),
					PARAM_NAME_OF_TV_TO_DETERMINE_FIELD_LENGTH);
			if (nameOfTVToDetermineFieldLengthParamValue != null
					&& nameOfTVToDetermineFieldLengthParamValue.trim()
							.length() > 0) {
				nameOfTVToDetermineFieldLength = nameOfTVToDetermineFieldLengthParamValue
						.trim();
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

			workspaceTemplateFilePath = options.parameter(
					this.getClass().getName(), PARAM_WORKSPACE_TEMPLATE);

			if (workspaceTemplateFilePath == null) {
				workspaceTemplateFilePath = options
						.parameter(PARAM_WORKSPACE_TEMPLATE);
			}
			// if no path is provided, use the directory of the default template
			if (workspaceTemplateFilePath == null) {
				workspaceTemplateFilePath = WORKSPACE_TEMPLATE_URL;
				result.addInfo(this, 9, PARAM_WORKSPACE_TEMPLATE,
						WORKSPACE_TEMPLATE_URL);
			}

			// copy template file either from remote or local URI
			if (workspaceTemplateFilePath.toLowerCase().startsWith("http")) {
				try {
					URL templateUrl = new URL(workspaceTemplateFilePath);
					FileUtils.copyURLToFile(templateUrl, outputFile);
				} catch (MalformedURLException e1) {
					result.addFatalError(this, 6, workspaceTemplateFilePath,
							e1.getMessage());
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
					result.addFatalError(this, 7,
							workspacetemplate.getAbsolutePath());
					throw new ShapeChangeAbortException();
				}
			}

			// connect to EA repository in outputFile
			absolutePathOfOutputEAPFile = outputFile.getAbsolutePath();

			rep = new Repository();

			if (!rep.OpenFile(absolutePathOfOutputEAPFile)) {
				String errormsg = rep.GetLastError();
				r.addError(null, 30, errormsg, outputFilename);
				rep = null;
				throw new ShapeChangeAbortException();
			}

			// get template packages
			rep.RefreshModelView(0);

			Collection<Package> c = rep.GetModels();
			Package root = c.GetAt((short) 0);

			Collection<Package> modelPkgs = root.GetPackages();
			if (modelPkgs.GetCount() == 0 || !modelPkgs.GetAt((short) 0)
					.GetStereotypeEx().equalsIgnoreCase("ArcGIS")) {
				result.addError(this, 9);
				throw new ShapeChangeAbortException();
			} else {
				Package workspacePkg = modelPkgs.GetAt((short) 0);
				workspacePkgId = workspacePkg.GetPackageID();

				// eaPkgByModelPkg.put(appSchemaPkg, workspacePkg);
			}

			Integer features = EAModelUtil.getEAChildPackageByName(rep,
					workspacePkgId, TEMPLATE_PKG_FEATURES_NAME);
			if (features == null) {
				result.addError(this, 102, TEMPLATE_PKG_FEATURES_NAME);
				throw new ShapeChangeAbortException();
			} else {
				featuresPkgId = features;
				eaPkgIdByModelPkg_byWorkspaceSubPkgId.put(featuresPkgId,
						new HashMap<PackageInfo, Integer>());
			}

			Integer domains = EAModelUtil.getEAChildPackageByName(rep,
					workspacePkgId, TEMPLATE_PKG_DOMAINS_NAME);
			if (domains == null) {
				result.addError(this, 102, TEMPLATE_PKG_DOMAINS_NAME);
				throw new ShapeChangeAbortException();
			} else {
				domainsPkgId = domains;
				eaPkgIdByModelPkg_byWorkspaceSubPkgId.put(domainsPkgId,
						new HashMap<PackageInfo, Integer>());
			}

			Integer tables = EAModelUtil.getEAChildPackageByName(rep,
					workspacePkgId, TEMPLATE_PKG_TABLES_NAME);
			if (tables == null) {
				result.addError(this, 102, TEMPLATE_PKG_TABLES_NAME);
				throw new ShapeChangeAbortException();
			} else {
				tablesPkgId = tables;
				eaPkgIdByModelPkg_byWorkspaceSubPkgId.put(tablesPkgId,
						new HashMap<PackageInfo, Integer>());
			}

			Integer assocClasses = EAModelUtil.getEAChildPackageByName(rep,
					workspacePkgId, TEMPLATE_PKG_ASSOCIATION_CLASSES_NAME);
			if (assocClasses == null) {
				result.addError(this, 102,
						TEMPLATE_PKG_ASSOCIATION_CLASSES_NAME);
				throw new ShapeChangeAbortException();
			} else {
				assocClassesPkgId = assocClasses;
				eaPkgIdByModelPkg_byWorkspaceSubPkgId.put(assocClassesPkgId,
						new HashMap<PackageInfo, Integer>());
			}

			ProcessConfiguration pc = o.getCurrentProcessConfig();

			// parse map entries
			List<ProcessMapEntry> mapEntries = pc.getMapEntries();

			Map<String, ProcessMapEntry> mes = new HashMap<String, ProcessMapEntry>();

			for (ProcessMapEntry pme : mapEntries) {
				/*
				 * TODO ignores value of 'rule' attribute in map entry, so if
				 * there were map entries for different rules with same 'type'
				 * attribute value, this needs to be updated
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
			String numRangeDeltaParamValue = options.parameter(
					this.getClass().getName(), PARAM_VALUE_RANGE_DELTA);
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
					PARAM_DOCUMENTATION_TEMPLATE);
			documentationNoValue = options.parameter(this.getClass().getName(),
					PARAM_DOCUMENTATION_NOVALUE);

			// parse
			shortNameByTaggedValue = options.parameterAsString(
					this.getClass().getName(), PARAM_SHORT_NAME_BY_TAGGED_VALUE,
					"shortName", false, true);

		}
	}

	public void process(ClassInfo ci) {

		/*
		 * if a map entry provides a mapping for this class to an esri field
		 * type, we can ignore it
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

			if (cat == Options.OBJECT || (cat == Options.FEATURE
					&& determineArcGISGeometryType(ci)
							.equals(ArcGISGeometryType.NONE))) {

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
						 * log warning that first geometry property will be
						 * used, the rest ignored
						 */
						result.addWarning(this, 211, ci.name());
					}

					parseNumericRangeConstraints(ci);
					createFeatureClass(ci);
				}

			} else if (cat == Options.ENUMERATION) {

				createCodedValueDomain(ci);

			} else if (cat == Options.CODELIST) {

				createCodedValueDomain(ci);

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

	private void parseNumericRangeConstraints(ClassInfo ci) {

		if (ci.constraints() != null) {

			for (Constraint cons : ci.constraints()) {

				String ocl = cons.text();

				boolean lowerBoundaryInclusive = true;
				boolean upperBoundaryInclusive = true;
				Double lowerBoundaryValue = DEFAULT_NUM_RANGE_MIN_LOWER_BOUNDARY;
				Double upperBoundaryValue = DEFAULT_NUM_RANGE_MAX_UPPER_BOUNDARY;

				Matcher matcher = numRangeConstraintLowerBoundaryPattern
						.matcher(ocl);

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
						result.addWarning(this, 231, lbValue, ci.name(),
								cons.name(), ocl);
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
						result.addWarning(this, 232, ubValue, ci.name(),
								cons.name(), ocl);
					}

				} else {
					// no problem - OCL constraint can be of other
					// constraint type
				}

				if (foundLowerBoundary || foundUpperBoundary) {

					matcher = numRangeConstraintPropertyNamePattern
							.matcher(ocl);

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
									|| (pi.aliasName() != null && pi.aliasName()
											.startsWith(propertyName))) {
								propertyFound = true;
								break;
							}
						}

						if (!propertyFound) {
							result.addWarning(this, 229, propertyName,
									ci.name(), ocl);
						}

						// keep track of the numeric range information

						NumericRangeConstraintMetadata nrcm = new NumericRangeConstraintMetadata(
								lowerBoundaryValue, upperBoundaryValue,
								lowerBoundaryInclusive, upperBoundaryInclusive);

						Map<String, NumericRangeConstraintMetadata> map;

						if (numericRangeConstraintByPropNameByClassName
								.containsKey(ci)) {
							// fine, we don't need to initialize the map for the
							// class
							map = numericRangeConstraintByPropNameByClassName
									.get(ci);
						} else {

							map = new HashMap<String, NumericRangeConstraintMetadata>();

							numericRangeConstraintByPropNameByClassName.put(ci,
									map);
						}

						map.put(propertyName, nrcm);

					} else {

						/*
						 * this is unexpected, as we expect to be able to
						 * determine the property name for a numeric range
						 * constraint (which we appear to have detected, because
						 * the regular expression to detect lower/upper boundary
						 * found something
						 */

						result.addWarning(this, 228, ci.name(), ocl);
					}
				}
			}
		}

		if (ci.matches(RULE_CLS_RANGE_DOMAIN_FROM_TAGGED_VALUES)) {

			for (PropertyInfo pi : ci.properties().values()) {

				Double lowerBoundaryValue = DEFAULT_NUM_RANGE_MIN_LOWER_BOUNDARY;
				Double upperBoundaryValue = DEFAULT_NUM_RANGE_MAX_UPPER_BOUNDARY;

				boolean foundLowerBoundary = false;
				boolean foundUpperBoundary = false;

				String TV_RANGE_MIN = "rangeMinimum";
				String TV_RANGE_MAX = "rangeMaximum";

				String rMin = pi.taggedValue(TV_RANGE_MIN);
				String rMax = pi.taggedValue(TV_RANGE_MAX);

				if (rMin != null && rMin.trim().length() > 0) {

					try {
						lowerBoundaryValue = Double.parseDouble(rMin.trim());
						foundLowerBoundary = true;
					} catch (NumberFormatException e) {
						MessageContext mc = result.addWarning(this, 241,
								rMin.trim(), TV_RANGE_MIN);
						mc.addDetail(this, 20001, pi.fullNameInSchema());
					}
				}

				if (rMax != null && rMax.trim().length() > 0) {

					try {
						upperBoundaryValue = Double.parseDouble(rMax.trim());
						foundUpperBoundary = true;
					} catch (NumberFormatException e) {
						MessageContext mc = result.addWarning(this, 242,
								rMax.trim(), TV_RANGE_MAX);
						mc.addDetail(this, 20001, pi.fullNameInSchema());
					}
				}

				if (foundLowerBoundary || foundUpperBoundary) {

					// keep track of the numeric range information

					NumericRangeConstraintMetadata nrcm = new NumericRangeConstraintMetadata(
							lowerBoundaryValue, upperBoundaryValue, true, true);

					Map<String, NumericRangeConstraintMetadata> map;

					if (numericRangeConstraintByPropNameByClassName
							.containsKey(ci)) {
						// fine, we don't need to initialize the map for the
						// class
						map = numericRangeConstraintByPropNameByClassName
								.get(ci);
					} else {

						map = new HashMap<String, NumericRangeConstraintMetadata>();

						numericRangeConstraintByPropNameByClassName.put(ci,
								map);
					}

					map.put(pi.name(), nrcm);
				}
			}
		}
	}

	private void parseLengthInfoFromOCLConstraints(ClassInfo ci) {

		if ((ci.category() == Options.FEATURE || ci.category() == Options.OBJECT
				|| ci.category() == Options.GMLOBJECT)
				&& ci.constraints() != null) {

			for (Constraint cons : ci.constraints()) {

				String ocl = cons.text();

				Matcher matcher = lengthConstraintPattern.matcher(ocl);

				boolean found = matcher.find();

				if (found) {

					String propertyName = matcher.group(1);
					String length = matcher.group(2);

					Integer i = new Integer(length);

					lengthByClassPropName.put(ci.name() + "_" + propertyName,
							i);

				} else {
					// no problem - OCL constraint can be of other
					// constraint type
				}
			}
		}
	}

	private void createCodedValueDomain(ClassInfo ci) throws EAException {

		int eaPkgId = establishEAPackageHierarchy(ci, domainsPkgId);

		// create class element
		String name = normalizeName(ci.name());

		if (exceedsMaxLength(name)) {
			this.result.addWarning(this, 205, name, ci.name(), ci.name(),
					"" + maxNameLength);
			name = clipToMaxLength(name);
		}

		Element e = EAModelUtil.createEAClass(rep, name, eaPkgId);

		// store mapping between ClassInfo and EA Element
		elementIdByClassInfo.put(ci, e.GetElementID());
		elementNameByClassInfo.put(ci, name);

		// set alias, notes, abstractness
		setCommonItems(ci, e);

		EAModelUtil.setEAStereotype(e, "CodedValueDomain");

		String documentation = ci.derivedDocumentation(documentationTemplate,
				"<no description available>");

		EAModelUtil.setTaggedValue(e,
				new EATaggedValue("Description", documentation, true));

		// identify field type for the coded value domain
		String fieldType = "esriFieldTypeString";
		String fieldTypeTV = ci.taggedValue("fieldType");
		if (fieldTypeTV != null && fieldTypeTV.trim().length() > 0) {
			fieldType = fieldTypeTV.trim();
		}

		// create required properties: FieldType, MergePolicy, SplitPolicy
		EAModelUtil.createEAAttribute(e, "FieldType", null, null, null, null,
				false, false, fieldType, new Multiplicity(1, 1),
				"esriFieldType", null);

		EAModelUtil.createEAAttribute(e, "MergePolicy", null, null, null, null,
				false, false, "esriMPTDefaultValue", new Multiplicity(1, 1),
				"esriMergePolicyType", null);

		EAModelUtil.createEAAttribute(e, "SplitPolicy", null, null, null, null,
				false, false, "esriSPTDuplicate", new Multiplicity(1, 1),
				"esriSplitPolicyType", null);

		// for each enum/code of the class, create an according property
		if (ci.properties() != null) {

			Set<String> enumStereotype = new HashSet<String>();
			enumStereotype.add(STEREOTYPE_DOMAIN_CODED_VALUE);

			for (PropertyInfo pi : ci.properties().values()) {

				String initialValue;

				if (pi.matches(RULE_ENUM_INITIAL_VALUE_BY_ALIAS)) {
					initialValue = pi.aliasName();
				} else {
					initialValue = pi.initialValue();
				}

				if (initialValue == null || initialValue.trim().length() == 0) {
					initialValue = pi.name();
				}

				EAModelUtil.createEAAttribute(e, pi.name(), null,
						pi.derivedDocumentation(documentationTemplate,
								documentationNoValue),
						enumStereotype, null, false, false, initialValue,
						new Multiplicity(1, 1), null, null);
			}
		}
	}

	private Element createRangeDomain(String rangeDomainName,
			String rangeDomainDocumentation, String esriFieldType,
			ClassInfo rangeDomainSource) throws EAException {

		int eaPkgId = establishEAPackageHierarchy(rangeDomainSource,
				domainsPkgId);

		// create class element
		String name = rangeDomainName;

		// TODO - think of naming scheme for range domains
		// String name = normalizeName(rangeDomainName);
		//
		// if (exceedsMaxLength(name)) {
		// this.result.addWarning(this, 205, name, rangeDomainName,
		// rangeDomainName, ""
		// + MAX_NAME_LENGTH);
		// name = clipToMaxLength(name);
		// }

		Element e = EAModelUtil.createEAClass(rep, name, eaPkgId);

		// store mapping between class name and EA Element
		numericRangeElementIdsByClassName.put(rangeDomainName,
				e.GetElementID());

		// TBD: set alias or notes?

		EAModelUtil.setEAStereotype(e, "RangeDomain");

		EAModelUtil.setTaggedValue(e,
				new EATaggedValue("Description",
						rangeDomainDocumentation == null
								? "<no description available>"
								: rangeDomainDocumentation,
						true));

		// create required properties: FieldType, MergePolicy, SplitPolicy
		EAModelUtil.createEAAttribute(e, "FieldType", null, null, null, null,
				false, false, esriFieldType, new Multiplicity(1, 1),
				"esriFieldType", null);

		EAModelUtil.createEAAttribute(e, "MergePolicy", null, null, null, null,
				false, false, "esriMPTDefaultValue", new Multiplicity(1, 1),
				"esriMergePolicyType", null);

		EAModelUtil.createEAAttribute(e, "SplitPolicy", null, null, null, null,
				false, false, "esriSPTDuplicate", new Multiplicity(1, 1),
				"esriSplitPolicyType", null);

		return e;
	}

	private void createFeatureClass(ClassInfo ci) throws EAException {

		int eaPkgId = establishEAPackageHierarchy(ci, featuresPkgId);

		// create class element
		String name = normalizeName(ci.name());

		if (exceedsMaxLength(name)) {
			this.result.addWarning(this, 205, name, ci.name(), ci.name(),
					"" + maxNameLength);
			name = clipToMaxLength(name);
		}

		Element e = EAModelUtil.createEAClass(rep, name, eaPkgId);

		// store mapping between ClassInfo and EA Element
		elementIdByClassInfo.put(ci, e.GetElementID());
		elementNameByClassInfo.put(ci, name);

		// set alias, notes, abstractness
		setCommonItems(ci, e);

		if (!ci.isAbstract()) {

			// identify stereotype to use
			ArcGISGeometryType geomType = determineArcGISGeometryType(ci);
			EAModelUtil.setEAStereotype(e, geomType.stereotypeValue());

			// create ArcGIS System Fields

			Attribute objectIdField = createSystemFieldOBJECTID(e);
			objectIdAttributeGUIDByClass.put(ci,
					objectIdField.GetAttributeGUID());

			Attribute shapeField = createSystemFieldShape(e);

			Attribute shapeAreaField = null;
			Attribute shapeLengthField = null;

			if (geomType.equals(ArcGISGeometryType.POLYLINE)
					|| geomType.equals(ArcGISGeometryType.POLYGON)) {

				shapeLengthField = createSystemFieldShapeLength(e);

				if (geomType.equals(ArcGISGeometryType.POLYGON)) {

					shapeAreaField = createSystemFieldShapeArea(e);
				}
			}

			createSystemFieldOBJECTIDIDX(e);
			createSystemFieldShapeIDX(e);

			// now set tagged values for feature class

			List<EATaggedValue> tvs = new ArrayList<EATaggedValue>();

			if (geomType.equals(ArcGISGeometryType.POINT)) {
				tvs.add(new EATaggedValue("AncillaryRole", "esriNCARNone"));
			}

			if (geomType.equals(ArcGISGeometryType.POLYGON)) {
				tvs.add(new EATaggedValue("AreaFieldName",
						shapeAreaField.GetAttributeGUID()));
			} else {
				tvs.add(new EATaggedValue("AreaFieldName", ""));
			}

			tvs.add(new EATaggedValue("CanVersion", "false"));
			tvs.add(new EATaggedValue("DSID", ""));
			tvs.add(new EATaggedValue("FeatureType", "esriFTSimple"));
			tvs.add(new EATaggedValue("GlobalIDFieldName", ""));

			String hasM = "false";
			if (ci.matches(RULE_CLS_HASM)) {
				String hasMFromTV = ci.taggedValue("HasM");
				if (hasMFromTV != null
						&& hasMFromTV.trim().equalsIgnoreCase("true")) {
					hasM = "true";
				}
			}
			tvs.add(new EATaggedValue("HasM", hasM));

			tvs.add(new EATaggedValue("HasSpatialIndex", "true"));

			String hasZ = "false";
			if (ci.matches(RULE_CLS_HASZ)) {
				String hasZFromTV = ci.taggedValue("HasZ");
				if (hasZFromTV != null
						&& hasZFromTV.trim().equalsIgnoreCase("true")) {
					hasZ = "true";
				}
			}
			tvs.add(new EATaggedValue("HasZ", hasZ));

			if (geomType.equals(ArcGISGeometryType.POLYGON)
					|| geomType.equals(ArcGISGeometryType.POLYLINE)) {
				tvs.add(new EATaggedValue("LengthFieldName",
						shapeLengthField.GetAttributeGUID()));
			} else {
				tvs.add(new EATaggedValue("LengthFieldName", ""));
			}

			tvs.add(new EATaggedValue("Metadata", "", true));
			tvs.add(new EATaggedValue("ModelName", ""));
			tvs.add(new EATaggedValue("OIDFieldName",
					objectIdField.GetAttributeGUID()));
			tvs.add(new EATaggedValue("RasterFieldName", ""));
			tvs.add(new EATaggedValue("ShapeFieldName",
					shapeField.GetAttributeGUID()));
			tvs.add(new EATaggedValue("SpatialReference", ""));
			tvs.add(new EATaggedValue("Versioned", "false"));

			EAModelUtil.setTaggedValues(e, tvs);
		}

		// properties cannot be processed now, because we do not necessarily
		// have EA Elements for all classes in the model yet; thus the type of a
		// property (with value type being a <<type>> or <<featureType>> cannot
		// be set using the GUID of the element - and thus the class would not
		// be linked to correctly

		// keep track of generalizations
		identifyGeneralisationRelationships(ci);
	}

	private Attribute createSystemFieldShapeIDX(Element e) throws EAException {

		List<EATaggedValue> tvs = new ArrayList<EATaggedValue>();

		tvs.add(new EATaggedValue("IsUnique", "true"));
		tvs.add(new EATaggedValue("IsAscending", "true"));

		Set<String> stereotypes = new HashSet<String>();
		stereotypes.add("SpatialIndex");

		return EAModelUtil.createEAAttribute(e, "Shape_IDX", null, null,
				stereotypes, tvs, false, false, null, new Multiplicity(1, 1),
				"", null);
	}

	private Attribute createSystemFieldOBJECTIDIDX(Element e)
			throws EAException {

		List<EATaggedValue> tvs = new ArrayList<EATaggedValue>();

		tvs.add(new EATaggedValue("IsUnique", "true"));
		tvs.add(new EATaggedValue("IsAscending", "true"));
		tvs.add(new EATaggedValue("Fields", ""));

		Set<String> stereotypes = new HashSet<String>();
		stereotypes.add("AttributeIndex");

		return EAModelUtil.createEAAttribute(e, "OBJECTID_IDX", null, null,
				stereotypes, tvs, false, false, null, new Multiplicity(1, 1),
				"", null);
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

		Set<String> stereotypes = new HashSet<String>();
		stereotypes.add("RequiredField");

		return EAModelUtil.createEAAttribute(e, "Shape_Area", null, null,
				stereotypes, tvs, false, false, null, new Multiplicity(1, 1),
				"esriFieldTypeDouble", null);
	}

	private Attribute createSystemFieldShapeLength(Element e)
			throws EAException {

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

		Set<String> stereotypes = new HashSet<String>();
		stereotypes.add("RequiredField");

		return EAModelUtil.createEAAttribute(e, "Shape_Length", null, null,
				stereotypes, tvs, false, false, null, new Multiplicity(1, 1),
				"esriFieldTypeDouble", null);
	}

	private Attribute createSystemFieldShape(Element e) throws EAException {

		List<EATaggedValue> tvs = new ArrayList<EATaggedValue>();

		tvs.add(new EATaggedValue("DomainFixed", "true"));
		tvs.add(new EATaggedValue("Editable", "true"));
		tvs.add(new EATaggedValue(
				"GeometryDef", "AvgNumPoints=0;"
						+ System.getProperty("line.separator") + "GridSize0=0;",
				true));
		tvs.add(new EATaggedValue("IsNullable", "true"));
		tvs.add(new EATaggedValue("Length", "0"));
		tvs.add(new EATaggedValue("ModelName", ""));
		tvs.add(new EATaggedValue("Precision", "0"));
		tvs.add(new EATaggedValue("RasterDef", "", true));
		tvs.add(new EATaggedValue("Required", "true"));
		tvs.add(new EATaggedValue("Scale", "0"));

		Set<String> stereotypes = new HashSet<String>();
		stereotypes.add("RequiredField");

		return EAModelUtil.createEAAttribute(e, "Shape", null, null,
				stereotypes, tvs, false, false, null, new Multiplicity(1, 1),
				"esriFieldTypeGeometry", null);
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

		Set<String> stereotypes = new HashSet<String>();
		stereotypes.add("RequiredField");

		return EAModelUtil.createEAAttribute(e, "OBJECTID", null, null,
				stereotypes, tvs, false, false, null, new Multiplicity(1, 1),
				"esriFieldTypeOID", null);
	}

	private void createObjectClass(ClassInfo ci) throws EAException {

		int eaPkgId = establishEAPackageHierarchy(ci, tablesPkgId);

		// create class element
		String name = normalizeName(ci.name());

		if (exceedsMaxLength(name)) {
			this.result.addWarning(this, 205, name, ci.name(), ci.name(),
					"" + maxNameLength);
			name = clipToMaxLength(name);
		}

		Element e = EAModelUtil.createEAClass(rep, name, eaPkgId,
				"ArcGIS::ObjectClass");

		// store mapping between ClassInfo and EA Element
		elementIdByClassInfo.put(ci, e.GetElementID());
		elementNameByClassInfo.put(ci, name);

		// set alias, notes, abstractness
		setCommonItems(ci, e);

		EAModelUtil.setEAStereotype(e, "ObjectClass");

		Attribute objectIdField = createSystemFieldOBJECTID(e);
		objectIdAttributeGUIDByClass.put(ci, objectIdField.GetAttributeGUID());

		createSystemFieldOBJECTIDIDX(e);

		// now set tagged values for feature class
		List<EATaggedValue> tvs = new ArrayList<EATaggedValue>();

		tvs.add(new EATaggedValue("CanVersion", "false"));
		tvs.add(new EATaggedValue("DSID", ""));
		tvs.add(new EATaggedValue("GlobalIDFieldName", ""));
		tvs.add(new EATaggedValue("Metadata", "", true));
		tvs.add(new EATaggedValue("ModelName", ""));
		tvs.add(new EATaggedValue("OIDFieldName",
				objectIdField.GetAttributeGUID()));
		tvs.add(new EATaggedValue("RasterFieldName", ""));
		tvs.add(new EATaggedValue("Versioned", "false"));

		EAModelUtil.setTaggedValues(e, tvs);

		// properties cannot be processed now, because we do not necessarily
		// have EA Elements for all classes in the model yet; thus the type of a
		// property (with value type being a <<type>> or <<featureType>> cannot
		// be set using the GUID of the element - and thus the class would not
		// be linked to correctly

		// keep track of generalizations
		identifyGeneralisationRelationships(ci);

	}

	private void identifyGeneralisationRelationships(ClassInfo ci) {

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

		String result = name.replaceAll(ILLEGAL_NAME_CHARACTERS_DETECTION_REGEX,
				"_");

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

	// private String applyLengthRestriction(String name, String propertyName,
	// ClassInfo ci) {
	//
	// String result = name;
	//
	// if (exceedsMaxLength(name)) {
	//
	// this.result.addWarning(this, 205, name, propertyName, ci.name(), ""
	// + MAX_NAME_LENGTH);
	// result = clipToMaxLength(name);
	// }
	//
	// return result;
	// }

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
	 * Sets abstractness, alias and documentation.
	 * 
	 * @param ci
	 * @param e
	 */
	private void setCommonItems(ClassInfo ci, Element e) {

		if (ci.isAbstract()) {
			try {
				EAModelUtil.setEAAbstract(true, e);
			} catch (EAException exc) {
				result.addError(this, 204, ci.name(), exc.getMessage());
			}
		}

		if (ci.aliasName() != null) {
			try {

				String aliasName = normalizeAlias(ci.aliasName(), ci);
				EAModelUtil.setEAAlias(aliasName, e);
			} catch (EAException exc) {
				result.addError(this, 204, ci.name(), exc.getMessage());
			}
		}

		String s = ci.derivedDocumentation(documentationTemplate,
				documentationNoValue);
		if (s != null) {
			try {
				EAModelUtil.setEANotes(s, e);
			} catch (EAException exc) {
				result.addError(this, 204, ci.name(), exc.getMessage());
			}
		}
	}

	private int establishEAPackageHierarchy(ClassInfo ci,
			int mainWorkspaceSubPkgId) throws EAException {

		/*
		 * Get path up to the application schema package. Include the
		 * application schema if the number of schemas selected for processing
		 * is greater than 1, since then we want/need to include the separation
		 * by application schema.
		 */
		Deque<PackageInfo> pathToAppSchemaAsStack = new ArrayDeque<PackageInfo>();

		if (numberOfSchemasSelectedForProcessing > 1) {

			PackageInfo pkg = ci.pkg();
			PackageInfo lastPkg = null;

			while (pkg != null && (lastPkg == null
					|| pkg.targetNamespace() == lastPkg.targetNamespace())) {

				pathToAppSchemaAsStack.addFirst(pkg);
				lastPkg = pkg;
				pkg = pkg.owner();
			}

		} else if (!ci.pkg().isSchema()) {

			PackageInfo pkg = ci.pkg();

			while (pkg != null && !pkg.isSchema()) {

				pathToAppSchemaAsStack.addFirst(pkg);

				pkg = pkg.owner();
			}
		}

		if (pathToAppSchemaAsStack.isEmpty()) {

			/*
			 * Class shall be created in main workspace sub-package; typically
			 * the case for a single application schema being selected for
			 * processing and the class being situated in the app schema
			 * package.
			 */
			return mainWorkspaceSubPkgId;

		} else {

			// walk down the path, create packages as needed

			Map<PackageInfo, Integer> eaPkgIdByModelPkg = eaPkgIdByModelPkg_byWorkspaceSubPkgId
					.get(mainWorkspaceSubPkgId);

			Integer eaParentPkgId = mainWorkspaceSubPkgId;
			Integer eaPkgId = null;

			while (!pathToAppSchemaAsStack.isEmpty()) {

				PackageInfo pi = pathToAppSchemaAsStack.removeFirst();

				if (eaPkgIdByModelPkg.containsKey(pi)) {

					eaPkgId = eaPkgIdByModelPkg.get(pi);

				} else {

					// create the EA package
					eaPkgId = EAModelUtil.createEAPackage(rep, pi,
							eaParentPkgId);
					eaPkgIdByModelPkg.put(pi, eaPkgId);
				}

				eaParentPkgId = eaPkgId;
			}

			return eaPkgId;
		}
	}

	/**
	 * Determines the ArcGIS geometry type to use for the given class. It is
	 * computed based upon the first occurrence of a property whose type name
	 * starts with 'GM_'. The geometry type is then determined as follows:
	 * <ul>
	 * <li>GM_Point -&gt; {@link ArcGISGeometryType.point}</li>
	 * <li>GM_MultiPoint -&gt; {@link ArcGISGeometryType.multipoint}</li>
	 * <li>GM_Curve or GM_MultiCurve -&gt; {@link ArcGISGeometryType.polyline}
	 * </li>
	 * <li>GM_Surface or GM_MultiSurface -&gt;
	 * {@link ArcGISGeometryType.polygon}</li>
	 * <li>other geometry type -&gt; {@link ArcGISGeometryType.unknown}</li>
	 * </ul>
	 * If no geometry type is found, {@link ArcGISGeometryType.none} is
	 * returned.
	 * 
	 * @param ci
	 * @return
	 */
	private ArcGISGeometryType determineArcGISGeometryType(ClassInfo ci) {

		if (geometryTypeCache.containsKey(ci)) {
			return geometryTypeCache.get(ci);
		}

		Map<String, PropertyInfo> allPis = getAllPisForClassInfo(ci);

		ArcGISGeometryType result = ArcGISGeometryType.NONE;

		// loop through properties - in order determined by sequence numbers
		for (PropertyInfo pi : allPis.values()) {

			Type t = pi.typeInfo();

			if (t.name.startsWith("GM_")) {

				if (t.name.equalsIgnoreCase("GM_Point")) {
					result = ArcGISGeometryType.POINT;
					break;
				} else if (t.name.equalsIgnoreCase("GM_MultiPoint")) {
					result = ArcGISGeometryType.MULTIPOINT;
					break;
				} else if (t.name.equalsIgnoreCase("GM_Curve")
						|| t.name.equalsIgnoreCase("GM_MultiCurve")) {
					result = ArcGISGeometryType.POLYLINE;
					break;
				} else if (t.name.equalsIgnoreCase("GM_Surface")
						|| t.name.equalsIgnoreCase("GM_MultiSurface")) {
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

		/*
		 * determine full set of properties relevant for this class, so also
		 * from all supertypes (ignoring those properties that are specialized
		 * in subtypes)
		 */
		Map<String, PropertyInfo> allPis = getAllPisForClassInfo(ci);

		boolean geomPropFound = false;

		for (PropertyInfo pi : allPis.values()) {

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

	private Map<String, PropertyInfo> getAllPisForClassInfo(ClassInfo ci) {

		// set to keep track of supertypes that have already been visited, in
		// case that there is a shared supertype in the supertype hierarchy
		Set<String> idsOfVisitedSupertypes = new HashSet<String>();

		Map<String, PropertyInfo> pis = computeAllPis(ci,
				idsOfVisitedSupertypes);

		return pis;
	}

	private Map<String, PropertyInfo> computeAllPis(ClassInfo ci,
			Set<String> idsOfVisitedSupertypes) {

		Map<String, PropertyInfo> result = new HashMap<String, PropertyInfo>();

		// first look up properties in all supertypes
		if (ci.supertypes() != null) {

			for (String supertypeId : ci.supertypes()) {

				if (idsOfVisitedSupertypes.contains(supertypeId)) {
					continue;
				} else {
					idsOfVisitedSupertypes.add(supertypeId);
				}

				ClassInfo supertype = model.classById(supertypeId);

				if (supertype != null) {

					// look up properties from supertype
					Map<String, PropertyInfo> supertypePis = computeAllPis(
							supertype, idsOfVisitedSupertypes);

					for (PropertyInfo supertypePi : supertypePis.values()) {
						result.put(supertypePi.name(), supertypePi);
					}
				}
			}

		}

		// now add properties of the given class, overriding those from
		// supertypes
		if (ci.properties() != null) {

			for (PropertyInfo pi : ci.properties().values()) {
				// overrides property if one with same name already existed
				result.put(pi.name(), pi);
			}
		}

		return result;
	}

	@Override
	public void write() {

		// nothing to do here (SingleTarget)
	}

	/**
	 * Creates a relationship class association including association class
	 * between the class that the given property is in (which represents the
	 * source class), and the class that is the type of the property (which is
	 * the target). The source class name will be used to create the foreign key
	 * that refers to an instance of that class.
	 * <p>
	 * Used to create a many to many relationship to represent an attribute from
	 * the application schema that has category of value object or feature type
	 * and max cardinality > 1.
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

			this.createManyToManyRelationshipClass(ciSource, ciTarget,
					toLowerCamelCase(ciSource.name()), pi.name());
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

		return new BigDecimal(d.toString()).stripTrailingZeros()
				.toPlainString();
	}

	/**
	 * Creates a relationship class including association class between the
	 * classes that the given properties are in.
	 * <p>
	 * Used to create a many to many relationship to represent an association
	 * from the application schema.
	 * 
	 * @param pi1
	 * @param pi2
	 */
	private void createManyToManyRelationshipClass(PropertyInfo pi1,
			PropertyInfo pi2) {

		ClassInfo ci1 = pi1.inClass();
		ClassInfo ci2 = pi2.inClass();

		this.createManyToManyRelationshipClass(ci1, ci2, pi2.name(),
				pi1.name());
	}

	private void createManyToManyRelationshipClass(ClassInfo source,
			ClassInfo target, String roleNameSource, String roleNameTarget) {

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
		Set<ClassInfo> sources, targets;

		if (source.isAbstract()) {
			sources = computeListOfAllNonAbstractSubtypes(source);
		} else {
			sources = new HashSet<ClassInfo>();
			sources.add(source);
		}

		if (target.isAbstract()) {
			targets = computeListOfAllNonAbstractSubtypes(target);
		} else {
			targets = new HashSet<ClassInfo>();
			targets.add(target);
		}

		/*
		 * create association(s) with association class(es) between source and
		 * target class(es)
		 */
		for (ClassInfo source_ : sources) {

			int sourceElementId_ = elementIdByClassInfo.get(source_);
			Element sourceElmt = rep.GetElementByID(sourceElementId_);

			for (ClassInfo target_ : targets) {

				int targetElementId_ = elementIdByClassInfo.get(target_);
				Element targetElmt = rep.GetElementByID(targetElementId_);

				if (sourceElmt == null) {
					result.addError(this, 239, source.name(), target.name(),
							source_.name(), target_.name());
					continue;
				}
				if (targetElmt == null) {
					result.addError(this, 239, source.name(), target.name(),
							target_.name(), source_.name());
					continue;
				}

				try {

					/*
					 * create association class (must be specific for each
					 * association, because an association class can only belong
					 * to a single association)
					 */

					String relClassName = computeRelationshipClassName(source_,
							target_);

					String assocClassName = normalizeName(relClassName);

					if (exceedsMaxLength(assocClassName)) {
						this.result.addWarning(this, 226, assocClassName,
								"" + maxNameLength);
						assocClassName = clipToMaxLength(assocClassName);
					}

					Element assocClass = EAModelUtil.createEAClass(rep,
							assocClassName, assocClassesPkgId);

					Attribute foreignKeyFieldSrc = null;
					Attribute foreignKeyFieldTgt = null;
					Attribute ridField = null;

					// create RID field
					try {

						ridField = createField(assocClass, "RID", "", "",
								"esriFieldTypeOID", "0", "0", "0", null, null,
								true);

					} catch (EAException e) {
						result.addError(this, 10003, "RID", assocClassName,
								e.getMessage());
						return;
					}

					/*
					 * create foreign key fields in assocClass for source and
					 * target
					 */

					String fkSrcName = roleNameSource + "ID";

					try {

						fkSrcName = normalizeName(fkSrcName);

						if (exceedsMaxLength(fkSrcName)) {
							this.result.addWarning(this, 227, fkSrcName,
									"" + maxNameLength);
							fkSrcName = clipToMaxLength(fkSrcName);
						}

						foreignKeyFieldSrc = createField(assocClass, fkSrcName,
								"", "", "esriFieldTypeInteger", "0", "9", "0",
								null, null, true);

					} catch (EAException e) {
						result.addError(this, 10003, fkSrcName, assocClassName,
								e.getMessage());
						return;
					}

					String fkTgtName = roleNameTarget + "ID";

					try {

						fkTgtName = normalizeName(fkTgtName);

						if (exceedsMaxLength(fkTgtName)) {
							this.result.addWarning(this, 227, fkTgtName,
									"" + maxNameLength);
							fkTgtName = clipToMaxLength(fkTgtName);
						}

						foreignKeyFieldTgt = createField(assocClass, fkTgtName,
								"", "", "esriFieldTypeInteger", "0", "9", "0",
								null, null, true);

					} catch (EAException e) {
						result.addError(this, 10003, fkTgtName, assocClassName,
								e.getMessage());
						return;
					}

					// set stereotype and tagged values on association class

					EAModelUtil.setEAStereotype(assocClass,
							STEREOTYPE_RELATIONSHIP_CLASS);

					List<EATaggedValue> tvs = new ArrayList<EATaggedValue>();

					tvs.add(new EATaggedValue("CanVersion", "false"));
					tvs.add(new EATaggedValue("CatalogPath", ""));
					tvs.add(new EATaggedValue("ClassKey",
							"esriRelClassKeyUndefined"));
					tvs.add(new EATaggedValue("DSID", "-1"));
					tvs.add(new EATaggedValue("DatasetType",
							"esriDTRelationshipClass"));
					tvs.add(new EATaggedValue("DestinationForeignKey",
							foreignKeyFieldTgt.GetAttributeGUID()));
					tvs.add(new EATaggedValue("DestinationPrimaryKey",
							objectIdAttributeGUIDByClass.get(target_)));
					tvs.add(new EATaggedValue("GlobalIDFieldName", ""));
					tvs.add(new EATaggedValue("IsAttachmentRelationship",
							"false"));
					tvs.add(new EATaggedValue("IsComposite", "false"));
					tvs.add(new EATaggedValue("IsReflexive", "false"));
					tvs.add(new EATaggedValue("KeyType",
							"esriRelKeyTypeSingle"));
					tvs.add(new EATaggedValue("Metadata", "", true));
					tvs.add(new EATaggedValue("MetadataRetrieved", "false"));
					tvs.add(new EATaggedValue("ModelName", ""));
					tvs.add(new EATaggedValue("Notification",
							"useUMLConnectorDirection"));
					tvs.add(new EATaggedValue("OIDFieldName",
							ridField.GetAttributeGUID()));
					tvs.add(new EATaggedValue("OriginForeignKey",
							foreignKeyFieldSrc.GetAttributeGUID()));
					tvs.add(new EATaggedValue("OriginPrimaryKey",
							objectIdAttributeGUIDByClass.get(source_)));
					tvs.add(new EATaggedValue("RasterFieldName", ""));
					tvs.add(new EATaggedValue("Versioned", "false"));

					/*
					 * create <<RelationshipClass>> association
					 */

					Connector con = EAModelUtil.createEAAssociation(sourceElmt,
							targetElmt);

					/*
					 * NOTE: the connector has the same name as the association
					 * class - and that has already been created, so we can
					 * reuse the name here
					 */
					EAModelUtil.setEAName(con, assocClassName);
					EAModelUtil.setEAStereotype(con,
							STEREOTYPE_RELATIONSHIP_CLASS);

					ConnectorEnd clientEnd = con.GetClientEnd();
					ConnectorEnd supplierEnd = con.GetSupplierEnd();

					/*
					 * set cardinality and name on connector ends
					 */

					// TODO ensure that property name is unique (approach with
					// counters could be improved)
					String sourcePropName = toLowerCamelCase(roleNameSource);
					sourcePropName = this.checkPropertyName(target_,
							sourcePropName);

					String targetPropName = toLowerCamelCase(roleNameTarget);
					targetPropName = this.checkPropertyName(source_,
							targetPropName);

					EAModelUtil.setEARole(clientEnd,
							toLowerCamelCase(sourcePropName));

					EAModelUtil.setEARole(supplierEnd,
							toLowerCamelCase(targetPropName));

					// EAModelUtil.setEARole(clientEnd,
					// toLowerCamelCase(roleNameSource));
					// EAModelUtil.setEARole(supplierEnd,
					// toLowerCamelCase(roleNameTarget));

					EAModelUtil.setEACardinality(clientEnd, "0..*");
					EAModelUtil.setEACardinality(supplierEnd, "0..*");

					/*
					 * set tagged values on association - they are the same as
					 * for the association class
					 */

					EAModelUtil.setTaggedValues(con, tvs);

					EAModelUtil.setEAAssociationClass(con, assocClass);

				} catch (EAException e) {

					result.addError(this, 221, source_.name(), target_.name(),
							e.getMessage());
				}
			}

		}
	}

	private String computeRelationshipClassName(ClassInfo source,
			ClassInfo target) {

		String sourceName = source.name();
		String targetName = target.name();

		if (source.matches(RULE_ALL_RELCLASSNAME_BY_TAGGEDVALUE_OF_CLASSES)) {

			if (source.taggedValue(shortNameByTaggedValue) != null) {
				sourceName = source.taggedValue(shortNameByTaggedValue);
			}
			if (target.taggedValue(shortNameByTaggedValue) != null) {
				targetName = target.taggedValue(shortNameByTaggedValue);
			}
		}

		String relClassName = sourceName + "_" + targetName;

		relClassName = this.checkRelationshipClassName(relClassName);

		return relClassName;
	}

	/**
	 * Creates a <<RelationshipClass>> association between the class that the
	 * given property is in and the the class that is the type of the property.
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
			result.addError(this, 218, pi.name(), pi.inClass().name(),
					typeInfo.name);

		} else {

			this.createOneToManyRelationshipClass(ci, typeCi,
					toLowerCamelCase(ci.name()), pi.name(), Integer.MAX_VALUE,
					false);
		}
	}

	private void createOneToManyRelationshipClass(AssociationInfo ai) {

		ClassInfo ci1 = ai.end2().inClass();
		ClassInfo ci2 = ai.end1().inClass();

		String roleNameOnCi1 = ai.end1().name();
		String roleNameOnCi2 = ai.end2().name();

		int maxOccursOnCi1 = ai.end1().cardinality().maxOccurs;

		this.createOneToManyRelationshipClass(ci1, ci2, roleNameOnCi1,
				roleNameOnCi2, maxOccursOnCi1, ai.end1().isNavigable());
	}

	/**
	 * The relationship association will be created with the source being the
	 * class where a navigable role with maxOccurs 1 ends. The other class will
	 * be the target. Source multiplicity for the association will be set to 1,
	 * target multiplicity to 0..*. The target class will get the foreign key ID
	 * field that is used to point to the source.
	 * 
	 * @param ci1
	 *            first class
	 * @param ci2
	 *            second class
	 * @param roleNameOnCi1
	 *            role name that belongs to the first class
	 * @param roleNameOnCi2
	 *            role name that belongs to the second class
	 * @param maxOccursOnCi1
	 *            maximum multiplicity that belongs to the first class
	 * @param isNavigableOnCi1
	 *            whether the role that belongs to the first class is navigable
	 */
	private void createOneToManyRelationshipClass(ClassInfo ci1, ClassInfo ci2,
			String roleNameOnCi1, String roleNameOnCi2, int maxOccursOnCi1,
			boolean isNavigableOnCi1) {

		ClassInfo source, target;
		String roleNameSource, roleNameTarget;

		if (maxOccursOnCi1 <= 1 && isNavigableOnCi1) {
			// use ci1 as source
			source = ci1;
			target = ci2;
			roleNameSource = roleNameOnCi1;
			roleNameTarget = roleNameOnCi2;
		} else {
			// use ci2 as source
			source = ci2;
			target = ci1;
			roleNameSource = roleNameOnCi2;
			roleNameTarget = roleNameOnCi1;
		}

		// create foreign key field in target class
		int eaElementId = elementIdByClassInfo.get(target);
		Element eaClassTarget = rep.GetElementByID(eaElementId);
		// Element eaClassTarget = this.classes.get(target);
		if (eaClassTarget == null) {
			result.addError(this, 237, source.name(), target.name(),
					target.name());
			return;
		}

		Attribute foreignKeyField = null;

		try {
			String name = normalizeName(roleNameSource + "ID");

			if (exceedsMaxLength(name)) {
				this.result.addWarning(this, 205, name, roleNameSource,
						target.name(), "" + maxNameLength);
				name = clipToMaxLength(name);

			}

			foreignKeyField = createField(eaClassTarget, name, "", "",
					"esriFieldTypeInteger", "0", "9", "0", null, null, true);

		} catch (EAException e) {
			result.addError(this, 10003, roleNameTarget, target.name(),
					e.getMessage());
			return;
		}

		/*
		 * execute behavior based upon abstractness of the types
		 */
		Set<ClassInfo> sources, targets;

		if (source.isAbstract()) {
			sources = computeListOfAllNonAbstractSubtypes(source);
		} else {
			sources = new HashSet<ClassInfo>();
			sources.add(source);
		}

		if (target.isAbstract()) {
			targets = computeListOfAllNonAbstractSubtypes(target);
		} else {
			targets = new HashSet<ClassInfo>();
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
					result.addError(this, 238, source.name(), target.name(),
							source_.name(), target_.name());
					continue;
				}
				if (targetElmt == null) {
					result.addError(this, 238, source.name(), target.name(),
							target_.name(), source_.name());
					continue;
				}

				try {

					Connector con = EAModelUtil.createEAAssociation(sourceElmt,
							targetElmt);

					String relClassName = computeRelationshipClassName(source_,
							target_);

					if (exceedsMaxLength(relClassName)) {
						this.result.addWarning(this, 234, relClassName,
								"" + maxNameLength);
						relClassName = clipToMaxLength(relClassName);
					}

					EAModelUtil.setEAName(con, relClassName);
					EAModelUtil.setEAStereotype(con,
							STEREOTYPE_RELATIONSHIP_CLASS);

					ConnectorEnd clientEnd = con.GetClientEnd();
					ConnectorEnd supplierEnd = con.GetSupplierEnd();

					// set cardinality and name on connector
					// ends

					// TODO ensure that property name is unique (approach with
					// counters could be improved)
					String sourcePropName = toLowerCamelCase(roleNameSource);
					sourcePropName = this.checkPropertyName(target_,
							sourcePropName);

					String targetPropName = toLowerCamelCase(roleNameTarget);
					targetPropName = this.checkPropertyName(source_,
							targetPropName);

					EAModelUtil.setEARole(clientEnd,
							toLowerCamelCase(sourcePropName));

					EAModelUtil.setEARole(supplierEnd,
							toLowerCamelCase(targetPropName));

					// EAModelUtil.setEARole(clientEnd,
					// toLowerCamelCase(roleNameSource));
					// EAModelUtil.setEARole(supplierEnd,
					// toLowerCamelCase(roleNameTarget));
					// EAModelUtil.setEARole(clientEnd, roleNameSource);
					// EAModelUtil.setEARole(supplierEnd, roleNameTarget);

					EAModelUtil.setEACardinality(clientEnd, "1");
					EAModelUtil.setEACardinality(supplierEnd, "0..*");

					// set tagged values on association
					List<EATaggedValue> tvs = new ArrayList<EATaggedValue>();

					tvs.add(new EATaggedValue("KeyType",
							"esriRelKeyTypeSingle"));
					tvs.add(new EATaggedValue("ClassKey",
							"esriRelClassKeyUndefined"));
					tvs.add(new EATaggedValue("OriginPrimaryKey",
							objectIdAttributeGUIDByClass.get(source_)));
					tvs.add(new EATaggedValue("OriginForeignKey",
							foreignKeyField.GetAttributeGUID()));
					tvs.add(new EATaggedValue("DestinationPrimaryKey", ""));
					tvs.add(new EATaggedValue("DestinationForeignKey", ""));
					tvs.add(new EATaggedValue("IsComposite", "false"));
					tvs.add(new EATaggedValue("IsReflexive", "false"));
					tvs.add(new EATaggedValue("DatasetType",
							"esriDTRelationshipClass"));
					tvs.add(new EATaggedValue("OIDFieldName", ""));
					tvs.add(new EATaggedValue("DSID", "-1"));
					tvs.add(new EATaggedValue("ModelName", ""));
					tvs.add(new EATaggedValue("GlobalIDFieldName", ""));
					tvs.add(new EATaggedValue("CatalogPath", ""));
					tvs.add(new EATaggedValue("RasterFieldName", ""));
					tvs.add(new EATaggedValue("Versioned", "false"));
					tvs.add(new EATaggedValue("CanVersion", "false"));
					tvs.add(new EATaggedValue("MetadataRetrieved", "false"));
					tvs.add(new EATaggedValue("Metadata", "", true));
					tvs.add(new EATaggedValue("Notification",
							"useUMLConnectorDirection"));
					tvs.add(new EATaggedValue("IsAttachmentRelationship",
							"false"));

					EAModelUtil.setTaggedValues(con, tvs);

				} catch (EAException e) {

					result.addError(this, 221, source_.name(), target_.name(),
							e.getMessage());
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
			Map<String, Integer> tmp = new HashMap<String, Integer>();

			tmp.put(propertyName, 1);

			counterByPropertyNameByClass.put(ci, tmp);

			return propertyName;
		}
	}

	private Set<ClassInfo> computeListOfAllNonAbstractSubtypes(ClassInfo ci) {

		Set<ClassInfo> allNonAbstractSubtypes = new HashSet<ClassInfo>();

		if (ci.subtypes() != null) {

			for (String subtypeId : ci.subtypes()) {

				ClassInfo subtype = model.classById(subtypeId);

				if (subtype == null) {

					result.addWarning(this, 219, subtypeId, ci.name());
					continue;
				}

				if (model.isInSelectedSchemas(subtype)) {

					if (subtype.isAbstract()) {

						// ignore abstract subtypes

					} else {
						allNonAbstractSubtypes.add(subtype);
					}

					allNonAbstractSubtypes.addAll(
							computeListOfAllNonAbstractSubtypes(subtype));

				} else {

					result.addWarning(this, 220, subtype.name(), ci.name());
				}
			}
		}

		return allNonAbstractSubtypes;
	}

	private String toLowerCamelCase(String s) {

		if (s == null || s.length() == 0) {

			return s;

		} else {

			String first = s.substring(0, 1);
			String lower = first.toLowerCase(Locale.ENGLISH);
			return lower + s.substring(1, s.length());
		}
	}

	private Integer computeScale(PropertyInfo pi, String valueTypeName) {

		String nameOfScaleTV = "scale";
		String scaleTV = pi.taggedValue(nameOfScaleTV);

		if (pi.matches(RULE_PROP_SCALE) && scaleTV != null
				&& scaleTV.trim().length() > 0) {

			try {
				Integer scale = Integer.parseInt(scaleTV.trim());
				return scale;
			} catch (NumberFormatException e) {
				MessageContext mc = result.addWarning(this, 243, scaleTV.trim(),
						nameOfScaleTV);
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

	private Integer computePrecision(PropertyInfo pi, String valueTypeName) {

		String nameOfPrecisionTV = "precision";
		String precisionTV = pi.taggedValue(nameOfPrecisionTV);

		if (pi.matches(RULE_PROP_PRECISION) && precisionTV != null
				&& precisionTV.trim().length() > 0) {

			try {
				Integer prec = Integer.parseInt(precisionTV.trim());
				return prec;
			} catch (NumberFormatException e) {
				MessageContext mc = result.addWarning(this, 243,
						precisionTV.trim(), nameOfPrecisionTV);
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

		if (pi.matches(RULE_PROP_ISNULLABLE)) {

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

			if (pi.matches(RULE_PROP_LENGTH_FROM_TAGGED_VALUE)) {

				String tv = pi.taggedValue(nameOfTVToDetermineFieldLength);

				if (tv != null && tv.trim().length() > 0) {

					try {
						Integer value = Integer.parseInt(tv.trim());
						if (value > 0) {
							return value;
						}
					} catch (NumberFormatException e) {
						MessageContext mc = result.addWarning(this, 243,
								tv.trim(), nameOfTVToDetermineFieldLength);
						mc.addDetail(this, 20001, pi.fullNameInSchema());
					}
				}
			}

			/*
			 * see if length is defined by OCL constraint contained in pi's
			 * class; this information has been computed when all class infos
			 * have been processed
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

	private int computeLengthForCodelistOrEnumerationValueType(
			PropertyInfo pi) {

		if (pi.matches(
				RULE_PROP_LENGTH_FROM_TAGGED_VALUE_FOR_CODELIST_OR_ENUMERATION_VALUE_TYPE)) {

			String tv = pi.taggedValue(nameOfTVToDetermineFieldLength);

			if (tv != null && tv.trim().length() > 0) {

				try {
					Integer value = Integer.parseInt(tv.trim());
					if (value > 0) {
						return value;
					}
				} catch (NumberFormatException e) {
					MessageContext mc = result.addWarning(this, 243, tv.trim(),
							nameOfTVToDetermineFieldLength);
					mc.addDetail(this, 20001, pi.fullNameInSchema());
				}
			}
		}

		if (pi.matches(RULE_PROP_LENGTH_FROM_CODES_OR_ENUMS_OF_VALUE_TYPE)) {

			ClassInfo typeCi = model.classById(pi.typeInfo().id);

			if (typeCi == null) {

				MessageContext mc = result.addWarning(this, 244,
						pi.typeInfo().name, pi.name());
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
		 * default length for property with code list or enumeration as value
		 * type is 0
		 */
		return 0;
	}

	private Attribute createField(Element e, String name, String alias,
			String documentation, String eaType, String tvLength,
			String tvPrecision, String tvScale, Integer eaClassifierId,
			String initialValue, boolean isNullable) throws EAException {

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

		Set<String> stereotypes = new HashSet<String>();
		stereotypes.add("Field");

		return EAModelUtil.createEAAttribute(e, name, alias, documentation,
				stereotypes, tvs, false, false, initialValue,
				new Multiplicity(1, 1), eaType, eaClassifierId);
	}

	@Override
	public String getTargetName() {
		return "ArcGIS Workspace";
	}

	public void reset() {

		initialised = false;
		model = null;
		numberOfSchemasSelectedForProcessing = 0;
		workspaceTemplateFilePath = WORKSPACE_TEMPLATE_URL;
		maxNameLength = DEFAULT_MAX_NAME_LENGTH;
		lengthTaggedValueDefault = LENGTH_TAGGED_VALUE_DEFAULT;
		numRangeDelta = NUM_RANGE_DELTA;
		esriTypesSuitedForRangeConstraint = new HashSet<String>();
		outputDirectory = null;
		outputDirectoryFile = null;
		documentationTemplate = null;
		documentationNoValue = null;
		rep = null;
		ignoredCis = new HashSet<ClassInfo>();
		geometryTypeCache = new HashMap<ClassInfo, ArcGISGeometryType>();
		elementIdByClassInfo = new HashMap<ClassInfo, Integer>();
		elementNameByClassInfo = new HashMap<ClassInfo, String>();
		objectIdAttributeGUIDByClass = new HashMap<ClassInfo, String>();
		generalisations = new HashMap<ClassInfo, ClassInfo>();
		associations = new HashSet<AssociationInfo>();
		counterByRelationshipClassName = new HashMap<String, Integer>();
		counterByPropertyNameByClass = new HashMap<ClassInfo, Map<String, Integer>>();
		workspacePkgId = null;
		featuresPkgId = null;
		tablesPkgId = null;
		assocClassesPkgId = null;
		domainsPkgId = null;
		eaPkgIdByModelPkg_byWorkspaceSubPkgId = new HashMap<Integer, Map<PackageInfo, Integer>>();
		processMapEntries = null;
		lengthMappingByTypeName = new HashMap<String, Integer>();
		precisionMappingByTypeName = new HashMap<String, Integer>();
		scaleMappingByTypeName = new HashMap<String, Integer>();
		lengthByClassPropName = new HashMap<String, Integer>();
		numericRangeConstraintByPropNameByClassName = new HashMap<ClassInfo, Map<String, NumericRangeConstraintMetadata>>();
		nameOfTVToDetermineFieldLength = "size";
		absolutePathOfOutputEAPFile = null;
		shortNameByTaggedValue = null;
		numericRangeElementIdsByClassName = new HashMap<String, Integer>();
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.MessageSource#message(int)
	 */
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
			return "URL '$1$' provided for configuration parameter "
					+ PARAM_WORKSPACE_TEMPLATE
					+ " is malformed. Execution will be aborted. Exception message is: '$2$'.";
		case 7:
			return "EAP with ArcGIS workspace template at '$1$' does not exist or cannot be read. Check the value of the configuration parameter '"
					+ PARAM_WORKSPACE_TEMPLATE
					+ "' and ensure that: a) it contains the path to the template file and b) the file can be read by ShapeChange.";
		case 8:
			return "Exception encountered when copying ArcGIS workspace template EAP file to output destination. Message is: $1$.";
		case 9:
			return "No value provided for configuration parameter '$1$', defaulting to: '$2$'.";
		case 10:
			return "Encountered package '$1$' (child of package '$2$') which is an application schema. The package will be ignored.";
		case 11:
			return "Target configuration map entry for type '$1$' does not have a target type. The map entry will be ignored.";
		case 12:
			return "Value of configuration parameter '"
					+ PARAM_VALUE_RANGE_DELTA
					+ "' could not be parsed as a double value. The default value of "
					+ NUM_RANGE_DELTA + " will be used for processing.";
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
			return "Encoding for reflexive association (found on class '$1$') is not defined. The association will be ignored.";
		case 236:
			return "Encoding for reflexive relationship (found on class '$1$', for property '$2$') is not defined. The property will be ignored.";
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

		// 10001-10100: EA exceptions
		case 10001:
			return "EA exception encountered: $1$";
		case 10002:
			return "EA exception encountered while creating generalization relationship between classes '$1$' and '$2$': $3$";
		case 10003:
			return "EA exception encountered while creating <<Field>> attribute for property '$1$' in class '$2$'. The property will be ignored. Error message: $3$";

		// 20001 - 20100: message context
		case 20001:
			return "Property: $1$";

		default:
			return "(" + ArcGISWorkspace.class.getName()
					+ ") Unknown message with number: " + mnr;
		}
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

				try {
					EAModelUtil.createEAGeneralization(rep,
							elementIdByClassInfo.get(ci1), c1Name,
							elementIdByClassInfo.get(ci2), c2Name);
				} catch (EAException e) {
					result.addWarning(this, 10002, c1Name, c2Name,
							e.getMessage());
				}
			}
		}

		// process properties of all classes
		for (ClassInfo ci : elementIdByClassInfo.keySet()) {

			if (ci.category() == Options.ENUMERATION
					|| ci.category() == Options.CODELIST) {
				/* enumerations and codelists have already been fully created */
				continue;
			}

			int eaElementId = elementIdByClassInfo.get(ci);
			Element eaClass = rep.GetElementByID(eaElementId);

			for (PropertyInfo pi : ci.properties().values()) {

				if (!pi.isAttribute()) {
					// keep track of the association for later
					associations.add(pi.association());
					continue;
				}

				String initialValue = null;
				if (pi.matches(RULE_PROP_INITIAL_VALUE)) {
					initialValue = pi.initialValue();
				}

				Type typeInfo = pi.typeInfo();
				String mappedTypeName = typeInfo.name;

				// omit reflexive relationships
				if (typeInfo.id.equals(ci.id())) {
					result.addWarning(this, 236, ci.name(), pi.name());
					continue;
				}

				if (pi.cardinality().maxOccurs > 1
						&& !(pi.categoryOfValue() == Options.FEATURE
								|| pi.categoryOfValue() == Options.OBJECT
								|| pi.categoryOfValue() == Options.GMLOBJECT)) {

					/*
					 * multiplicity must have been flattened, at least for
					 * actual attributes - unless the category of value for the
					 * property is object or feature type (then establish a one
					 * to many or many to many relationship - which is covered
					 * later on in the decision tree)
					 */
					result.addWarning(this, 212, pi.name(), ci.name());
					continue;
				}

				String normalizedPiName = normalizeName(pi.name());

				if (exceedsMaxLength(normalizedPiName)) {
					this.result.addWarning(this, 205, normalizedPiName,
							pi.name(), ci.name(), "" + maxNameLength);
					normalizedPiName = clipToMaxLength(normalizedPiName);
				}

				String normalizedPiAlias = pi.aliasName() == null ? null
						: normalizeAlias(pi.aliasName(), ci);

				ClassInfo typeCi = model.classById(typeInfo.id);

				/*
				 * first, determine if a type mapping is available - if so,
				 * apply it
				 */
				if (processMapEntries.containsKey(typeInfo.name)) {

					ProcessMapEntry pme = processMapEntries.get(typeInfo.name);

					// during initialization we ensured that the map entry
					// has a target type, so no need to check again here

					/*
					 * now it depends: is the target type one that supports
					 * numeric range constraints, and is such a constraint
					 * actually defined for the property?
					 */

					Element numericRange = null;

					if (esriTypesSuitedForRangeConstraint
							.contains(pme.getTargetType())
							&& numericRangeConstraintByPropNameByClassName
									.containsKey(ci)) {

						/*
						 * determine if pi has a name or alias that starts with
						 * a property name in the range constraint map for ci.
						 * We must check via a 'startsWith' instead of equals
						 * because flattening can change the name of a property;
						 * a prominent example is where a numeric property can
						 * have a single or interval value (then you'd have
						 * three properties instead of one after flattening).
						 * Name flattening may have switched the name of a model
						 * element with its alias, so we have to check both
						 * fields.
						 */
						Set<String> originalPropertyNames = numericRangeConstraintByPropNameByClassName
								.get(ci).keySet();

						for (String origPropName : originalPropertyNames) {

							if (pi.name().startsWith(origPropName)
									|| (pi.aliasName() != null && pi.aliasName()
											.startsWith(origPropName))) {

								// apply range constraint!

								// check if a range domain already exists - if
								// so, reuse it
								String rangeDomainName = ci.name() + "_"
										+ origPropName + "_NumRange";

								if (numericRangeElementIdsByClassName
										.containsKey(rangeDomainName)) {

									int numRangeElementId = numericRangeElementIdsByClassName
											.get(rangeDomainName);
									numericRange = rep
											.GetElementByID(numRangeElementId);

								} else {

									// create range domain element

									NumericRangeConstraintMetadata nrcm = numericRangeConstraintByPropNameByClassName
											.get(ci).get(origPropName);

									double minValue = DEFAULT_NUM_RANGE_MIN_LOWER_BOUNDARY;
									double maxValue = DEFAULT_NUM_RANGE_MAX_UPPER_BOUNDARY;

									if (nrcm.hasLowerBoundaryValue()) {
										if (nrcm.isLowerBoundaryInclusive()) {
											minValue = nrcm
													.getLowerBoundaryValue();
										} else {
											minValue = nrcm
													.getLowerBoundaryValue()
													+ numRangeDelta;
										}
									}

									if (nrcm.hasUpperBoundaryValue()) {
										if (nrcm.isUpperBoundaryInclusive()) {
											maxValue = nrcm
													.getUpperBoundaryValue();
										} else {
											maxValue = nrcm
													.getUpperBoundaryValue()
													- numRangeDelta;
										}
									}

									try {

										// TODO parse documentation from range
										// constraint
										Element rd = this.createRangeDomain(
												rangeDomainName, null,
												pme.getTargetType(), ci);

										// create min and max fields
										EAModelUtil.createEAAttribute(rd,
												"MinValue", null, null, null,
												null, false, false,
												doubleToString(minValue),
												new Multiplicity(1, 1), null,
												null);
										EAModelUtil.createEAAttribute(rd,
												"MaxValue", null, null, null,
												null, false, false,
												doubleToString(maxValue),
												new Multiplicity(1, 1), null,
												null);

										numericRange = rd;

									} catch (EAException e) {

										// log an error, but proceed
										result.addError(this, 230,
												rangeDomainName,
												e.getMessage());
										numericRange = null;
									}

								}

								break;
							}
						}
					}

					if (numericRange != null) {

						try {
							String valueType = numericRange.GetName();
							createField(eaClass, normalizedPiName,
									normalizedPiAlias,
									pi.derivedDocumentation(
											documentationTemplate,
											documentationNoValue),
									valueType, "0",
									"" + computePrecision(pi, valueType),
									"" + computeScale(pi, valueType),
									numericRange.GetElementID(), initialValue,
									computeIsNullable(pi));

						} catch (EAException e) {
							result.addError(this, 10003, pi.name(), ci.name(),
									e.getMessage());
						}

					} else {

						/*
						 * try to find the target type in the model (for
						 * updating the type info of pi)
						 */
						ClassInfo targetTypeCi = model
								.classByName(pme.getTargetType());

						mappedTypeName = pme.getTargetType();

						/*
						 * now try to find the target type in the class map - if
						 * it is available there we can set the classifier ID
						 * correctly; otherwise we simply don't set the
						 * classifier ID
						 */
						String eaTargetType;
						Integer eaTargetClassifierId = null;

						if (targetTypeCi == null || !elementIdByClassInfo
								.containsKey(targetTypeCi)) {
							// it's alright to not have an actual class for the
							// target type defined in the map entry
							eaTargetType = pme.getTargetType();

						} else {

							int eaTargetTypeElementId = elementIdByClassInfo
									.get(targetTypeCi);
							Element eaTargetTypeClass = rep
									.GetElementByID(eaTargetTypeElementId);

							eaTargetType = eaTargetTypeClass.GetName();
							eaTargetClassifierId = eaTargetTypeClass
									.GetElementID();
						}

						try {
							createField(eaClass, normalizedPiName,
									normalizedPiAlias,
									pi.derivedDocumentation(
											documentationTemplate,
											documentationNoValue),
									eaTargetType,
									"" + computeLength(pi, mappedTypeName),
									"" + computePrecision(pi, mappedTypeName),
									"" + computeScale(pi, mappedTypeName),
									eaTargetClassifierId, initialValue,
									computeIsNullable(pi));

						} catch (EAException e) {
							result.addError(this, 10003, pi.name(), ci.name(),
									e.getMessage());
						}
					}

				} else if (typeInfo.name.startsWith("GM_")) {

					/*
					 * ignore geometry typed properties - the geometry is
					 * implicit for an ArcGIS feature class (<<Point>> etc).
					 */
					result.addDebug(this, 213, pi.name(), ci.name());

				} else if (typeCi != null
						&& !model.isInSelectedSchemas(typeCi)) {

					/*
					 * type does not belong to application schema - ignore the
					 * property
					 */
					result.addWarning(this, 222, typeInfo.name, pi.name(),
							ci.name());

				} else if (pi.categoryOfValue() == Options.FEATURE
						|| pi.categoryOfValue() == Options.OBJECT
						|| pi.categoryOfValue() == Options.GMLOBJECT) {

					if (pi.cardinality().maxOccurs > 1) {

						createManyToManyRelationshipClass(pi);

					} else {

						createOneToManyRelationshipClass(pi);
					}

				} else if (pi.categoryOfValue() == Options.ENUMERATION
						|| pi.categoryOfValue() == Options.CODELIST) {

					String eaType;
					Integer eaClassifierId = null;

					if (typeCi == null
							|| !elementIdByClassInfo.containsKey(typeCi)) {
						result.addWarning(this, 216, typeInfo.name, pi.name(),
								ci.name());
						eaType = clipToMaxLength(typeInfo.name);
					} else {
						int eaTypeElementId = elementIdByClassInfo.get(typeCi);
						Element eaTypeClass = rep
								.GetElementByID(eaTypeElementId);
						eaType = eaTypeClass.GetName();
						eaClassifierId = eaTypeClass.GetElementID();
					}

					try {
						createField(eaClass, normalizedPiName,
								normalizedPiAlias,
								pi.derivedDocumentation(documentationTemplate,
										documentationNoValue),
								eaType,
								"" + computeLengthForCodelistOrEnumerationValueType(
										pi),
								"0", "0", eaClassifierId, initialValue,
								computeIsNullable(pi));
					} catch (EAException e) {
						result.addError(this, 10003, pi.name(), ci.name(),
								e.getMessage());
					}

				} else if (pi.categoryOfValue() == Options.UNION) {

					// unions aren't supported
					result.addWarning(this, 214, pi.name(), ci.name());

				} else if (pi.categoryOfValue() == Options.DATATYPE) {

					// data types aren't supported
					result.addWarning(this, 215, pi.name(), ci.name());

				} else {

					// This case is unexpected
					result.addError(this, 217, pi.name(),
							"" + pi.categoryOfValue(), ci.name());

				}
			}
		}

		// process associations
		for (AssociationInfo ai : associations) {

			// upper bound of either end can be > 1

			/*
			 * 2014-11-26 NOTE: flattening of multiplicity normally dissolves
			 * associations where one end has max cardinality > 1; this behavior
			 * can be suppressed for associations between feature and object
			 * types via a configuration parameter
			 */

			PropertyInfo end1 = ai.end1();
			int end1CiCat = end1.inClass().category();
			PropertyInfo end2 = ai.end2();
			int end2CiCat = end2.inClass().category();

			// omit reflexive relationships
			if (end1.inClass().id().equals(end2.inClass().id())) {
				result.addWarning(this, 235, end1.inClass().name());
				continue;
			}

			/*
			 * only process associations between feature and object types,
			 * ignore those where one end is a union, datatype, enumeration,
			 * codelist etc.
			 */
			if (!(end1CiCat == Options.FEATURE || end1CiCat == Options.GMLOBJECT
					|| end1CiCat == Options.OBJECT)
					&& !(end2CiCat == Options.FEATURE
							|| end2CiCat == Options.GMLOBJECT
							|| end2CiCat == Options.OBJECT)) {

				// we only process associations between feature and object types
				result.addDebug(this, 223, end1.inClass().name(),
						end2.inClass().name());
				continue;
			}

			if (!(model.isInSelectedSchemas(end1.inClass())
					&& model.isInSelectedSchemas(end2.inClass()))) {

				// we only process associations where both ends are in the app
				// schema
				result.addDebug(this, 224, end1.inClass().name(),
						end2.inClass().name());
				continue;

			}

			/*
			 * Alright, now we should have an association between two feature
			 * and/or object types that are both part of schemas selected for
			 * processing
			 */

			// differentiate between one to many and many to many
			if ((end1.isNavigable() && end1.cardinality().maxOccurs == 1)
					|| (end2.isNavigable()
							&& end2.cardinality().maxOccurs == 1)) {

				createOneToManyRelationshipClass(ai);

			} else {

				createManyToManyRelationshipClass(end1, end2);
			}
		}

		// 2015-06-25 JE: compact() no longer supported in EA v12 API
		// rep.Compact();
		rep.CloseFile();
		rep.Exit();
	}
}
