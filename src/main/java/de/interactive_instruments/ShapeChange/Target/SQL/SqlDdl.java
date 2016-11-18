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
package de.interactive_instruments.ShapeChange.Target.SQL;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import de.interactive_instruments.ShapeChange.MapEntryParamInfos;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.StructuredNumber;
import de.interactive_instruments.ShapeChange.TargetIdentification;
import de.interactive_instruments.ShapeChange.Model.AssociationInfo;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.Target;

/**
 * Creates SQL DDL for an application schema.
 *
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class SqlDdl implements Target, MessageSource {

	public static final String PLATFORM = "sql";

	/* ------------------------------------------- */
	/* --- configuration parameter identifiers --- */
	/* ------------------------------------------- */

	/**
	 * Name for the identifier column when generating table creation statements.
	 * This parameter is optional. The default is
	 * {@value #DEFAULT_ID_COLUMN_NAME}.
	 */
	public static final String PARAM_ID_COLUMN_NAME = "idColumnName";

	/**
	 * Suffix to append to the name of columns that contain foreign keys (except
	 * if it references a table that represents a data type). This parameter is
	 * optional. The default is the empty string.
	 */
	public static final String PARAM_FOREIGN_KEY_COLUMN_SUFFIX = "foreignKeyColumnSuffix";

	/**
	 * Suffix to append to the name of columns that contain foreign keys
	 * referencing tables that represent data types. This parameter is optional.
	 * The default is the empty string.
	 */
	public static final String PARAM_FOREIGN_KEY_COLUMN_SUFFIX_DATATYPE = "foreignKeyColumnSuffixDatatype";

	/**
	 * Datatype to use for foreign key fields, for example 'bigint' in case of a
	 * PostgreSQL database. The default is the primary key type defined by the
	 * database strategy.
	 */
	public static final String PARAM_FOREIGN_KEY_COLUMN_DATA_TYPE = "foreignKeyColumnDataType";

	/**
	 * Size for fields representing textual properties with limited length, to
	 * be used in case that the property represented by the field does not have
	 * a 'size' tagged value; default is {@value #DEFAULT_SIZE}
	 */
	public static final String PARAM_SIZE = "size";

	/**
	 * EPSG code of the spatial reference system to use for geometries; default
	 * is {@value #DEFAULT_SRID}
	 */
	public static final String PARAM_SRID = "srid";

	/**
	 * Flag to indicate that foreign key creation is desired (true); default is
	 * false.
	 */
	public static final String PARAM_CREATE_REFERENCES = "createReferences";

	/**
	 * Flag to indicate that model documentation shall be added to the DDL via
	 * comments; default is true.
	 */
	public static final String PARAM_CREATE_DOCUMENTATION = "createDocumentation";

	/**
	 * Identifier of the database system for which SQL DDL shall be created.
	 * Supported systems - and also relevant identifiers - are:
	 * <ul>
	 * <li>PostgreSQL</li>
	 * <li>Oracle</li>
	 * <li>SQLServer</li>
	 * </ul>
	 * The default is PostgreSQL.
	 */
	public static final String PARAM_DATABASE_SYSTEM = "databaseSystem";

	/**
	 * Optional changes to the default documentation template and the default
	 * strings for descriptors without value
	 */
	public static final String PARAM_DOCUMENTATION_TEMPLATE = "documentationTemplate";
	public static final String PARAM_DOCUMENTATION_NOVALUE = "documentationNoValue";

	/**
	 * Comma-separated list of descriptors that shall be encoded as individual
	 * columns in a table representing a code list. The descriptors are
	 * specified by their identifier ('alias', 'definition', 'description',
	 * 'example', 'legalBasis', 'dataCaptureStatement', 'primaryCode'). NOTE:
	 * 'documentation' can also be used to include documentation that is derived
	 * from descriptors using the {@value #PARAM_DOCUMENTATION_TEMPLATE} and
	 * {@value #PARAM_DOCUMENTATION_NOVALUE}. The default value for this
	 * parameter is 'documentation'.
	 * <p>
	 * Applies to {@value #RULE_TGT_SQL_CLS_CODELISTS}
	 */
	public static final String PARAM_DESCRIPTORS_FOR_CODELIST = "descriptorsForCodelist";
	private String[] descriptorsForCodelistFromConfig = new String[] {
			"documentation" };
	private List<DescriptorForCodeList> descriptorsForCodelist = new ArrayList<DescriptorForCodeList>();

	/**
	 * This parameter controls the name of the column that contains the name or
	 * - if available - the initial value of a code. Default is 'name'. NOTE:
	 * The column name will be normalized according to the rules of the chosen
	 * database system.
	 * <p>
	 * Additional columns can be defined via the configuration parameter
	 * {@value #PARAM_DESCRIPTORS_FOR_CODELIST}.
	 * <p>
	 * Applies to {@value #RULE_TGT_SQL_CLS_CODELISTS}
	 */
	public static final String PARAM_CODE_NAME_COLUMN_NAME = "codeNameColumnName";
	private String codeNameColumnName = "name";
	private String normalizedCodeNameColumnName = null;

	public static final String PARAM_CODE_NAME_SIZE = "codeNameSize";
	private Integer codeNameSize = null;

	/* ------------------------ */
	/* --- rule identifiers --- */
	/* ------------------------ */

	/**
	 * Ensures that table creation statements are generated for feature types.
	 */
	public static final String RULE_TGT_SQL_CLS_FEATURE_TYPES = "rule-sql-cls-feature-types";

	/**
	 * Ensures that table creation statements are generated for object types.
	 */
	public static final String RULE_TGT_SQL_CLS_OBJECT_TYPES = "rule-sql-cls-object-types";

	/**
	 * Ensures that CHECK constraints are created for fields representing
	 * enumeration values.
	 */
	public static final String RULE_TGT_SQL_PROP_CHECK_CONSTRAINTS_FOR_ENUMERATIONS = "rule-sql-prop-check-constraints-for-enumerations";
	/**
	 * Ensures that table creation statements are generated for complex data
	 * types.
	 */
	public static final String RULE_TGT_SQL_CLS_DATATYPES = "rule-sql-cls-data-types";

	/**
	 * Tables are generated for code lists. Insert statements are created for
	 * the codes of a code list. Properties with a code list as value type will
	 * be converted to fields with foreign key type.
	 */
	public static final String RULE_TGT_SQL_CLS_CODELISTS = "rule-sql-cls-code-lists";

	/**
	 * 
	 */
	public static final String RULE_TGT_SQL_CLS_CODELISTS_PODS = "rule-sql-cls-code-lists-pods";

	/**
	 * If this rule is enabled, then a property whose type is neither covered by
	 * a type mapping entry nor contained in the currently processed schema -
	 * but in the overall model - is still encoded as a field with a foreign key
	 * - if other rules allow table creation for this type. Otherwise the field
	 * is encoded using a textual data type.
	 */
	public static final String RULE_TGT_SQL_CLS_REFERENCES_TO_EXTERNAL_TYPES = "rule-sql-cls-references-to-external-types";

	/**
	 * This rule ensures that associative tables are created for cases in which
	 * an n:m relationship exists between types.
	 *
	 * The name of the associative table is taken from the tagged value
	 * {@value #TV_ASSOCIATIVETABLE} - which exists either on an association or
	 * an attribute. If the tagged value is not present or empty, the name is
	 * created as follows:
	 * <ul>
	 * <li>If the table represents an n:m relationship represented by an
	 * association, then:
	 * <ul>
	 * <li>for a bi-directional association: the name of the class (from both
	 * ends of the association) that is lower in alphabetical order is used,
	 * concatenated with the according property name</li>
	 * <li>for a uni-directional association: the name of the inClass of the
	 * navigable property is used, concatenated with the property name</li>
	 * </ul>
	 * </li>
	 * <li>If the table represents an n:m relationship that is caused by an
	 * attribute with max multiplicity greater than one, then the name of the
	 * class that the attribute belongs to is used, concatenated with the
	 * property name</li>
	 * </ul>
	 */
	public static final String RULE_TGT_SQL_ALL_ASSOCIATIVETABLES = "rule-sql-all-associativetables";

	/**
	 * If this rule is enabled derived properties will be ignored.
	 */
	public static final String RULE_TGT_SQL_PROP_EXCLUDE_DERIVED = "rule-sql-prop-exclude-derived";

	/**
	 * If this rule is enabled, abstract classes will be ignored by the target.
	 */
	public static final String RULE_TGT_SQL_ALL_EXCLUDE_ABSTRACT = "rule-sql-all-exclude-abstract";

	/**
	 * Under this rule, foreign key identifiers are generated as follows:
	 * <p>
	 * "fk_" + tableNameForFK + "" + targetTableNameForFK + "" + fieldNameForFK
	 * + count where:
	 * <ul>
	 * <li>tableNameForFK is the name of the table that contains the field with
	 * the foreign key, clipped to the first eight characters</li>
	 * <li>targetTableNameForFK is the name of the table that the field with
	 * foreign key references, clipped to the first eight characters</li>
	 * <li>fieldNameForFK is the name of the field that contains the foreign
	 * key, clipped to the first eight characters</li>
	 * <li>count is the number of times the foreign key identifier has been
	 * assigned; it ranges from 0-9 and can also be omitted, thus supporting
	 * eleven unambiguous uses of the foreign key identifier (NOTE: if the
	 * foreign key identifier is used more than eleven times, ShapeChange logs
	 * an error)</li>
	 * </ul>
	 */
	public static final String RULE_TGT_SQL_ALL_FOREIGNKEY_ORACLE_NAMING_STYLE = "rule-sql-all-foreign-key-oracle-naming-style";

	/* --------------------- */
	/* --- Tagged Values --- */
	/* --------------------- */

	public static final String TV_ASSOCIATIVETABLE = "associativeTable";

	/* -------------------- */
	/* --- other fields --- */
	/* -------------------- */

	/**
	 * <pre>
	 * (name|documentation|alias|definition|description|example|legalBasis|dataCaptureStatement|primaryCode)(\(((columnName|size)=\w+)(,(columnName|size)=\w+)*\))?
	 * </pre>
	 */
	public static final String DESCRIPTORS_FOR_CODELIST_REGEX = "(name|documentation|alias|definition|description|example|legalBasis|dataCaptureStatement|primaryCode)(\\(((columnName|size)=\\w+)(;(columnName|size)=\\w+)*\\))?";

	public static final String DEFAULT_ID_COLUMN_NAME = "_id";
	public static final String DEFAULT_FOREIGN_KEY_COLUMN_SUFFIX = "";
	public static final String DEFAULT_FOREIGN_KEY_COLUMN_SUFFIX_DATATYPE = "";
	public static final int DEFAULT_SIZE = 1024;
	public static final int DEFAULT_SRID = 4326;

	/**
	 * Name of the parameter to indicate (via the 'param' attribute) that a map
	 * entry contains information about a geometry type.
	 */
	public static final String ME_PARAM_GEOMETRY = "geometry";
	/**
	 * Name of the parameter to indicate (via the 'param' attribute) that the
	 * type of a map entry is represented by a table.
	 */
	public static final String ME_PARAM_TABLE = "table";
	/**
	 * A characteristic for the parameter {@value #ME_PARAM_TABLE} that gives
	 * information about the category of the conceptual type that is identified
	 * by the map entry.
	 *
	 * Recognized values are (currently there is only one):
	 * <ul>
	 * <li>datatype</li>
	 * </ul>
	 */
	public static final String ME_PARAM_TABLE_CHARACT_REP_CAT = "representedCategory";
	/**
	 * Regular expression (?i:datatype) to check that a given string is one of a
	 * list of allowed values (NOTE1: check is case-insensitive; NOTE2: at the
	 * moment there is only one valid value).
	 */
	public static final String ME_PARAM_TABLE_CHARACT_REP_CAT_VALIDATION_REGEX = "(?i:datatype)";

	public static final String MAP_TARGETTYPE_COND_PART = "cond:";
	public static final String MAP_TARGETTYPE_COND_TEXTORCHARACTERVARYING = "textOrCharacterVarying";

	public static final String CRLF = Options.CRLF;
	public static final String IDT = "\t";

	protected TreeMap<String, ProcessMapEntry> mapEntryByType = new TreeMap<String, ProcessMapEntry>();

	private Pattern pattern_find_true = Pattern.compile("true",
			Pattern.CASE_INSENSITIVE);
	private Pattern pattern_find_false = Pattern.compile("false",
			Pattern.CASE_INSENSITIVE);

	private ShapeChangeResult result = null;
	private PackageInfo schema = null;
	private Model model = null;
	private Options options = null;
	private boolean printed = false;
	private boolean diagnosticsOnly = false;
	/**
	 * NOTE: If not set via the configuration, the default applies which is
	 * {@value Options#DERIVED_DOCUMENTATION_DEFAULT_TEMPLATE}.
	 */
	private String documentationTemplate = null;
	/**
	 * NOTE: If not set via the configuration, the default applies which is
	 * {@value Options#DERIVED_DOCUMENTATION_DEFAULT_NOVALUE}.
	 */
	private String documentationNoValue = null;

	private String outputDirectory;

	private String idColumnName;
	private String foreignKeyColumnSuffix;
	private String foreignKeyColumnSuffixDatatype;
	private String foreignKeyColumnDataType;
	private int defaultSize;
	private int srid;
	private boolean createReferences = false;
	private boolean createDocumentation = true;
	private boolean createAssociativeTables = false;
	private boolean oracleNamingStyle = false;
	private Map<String, Integer> countByForeignKeyOracleStyle = new HashMap<String, Integer>();

	/**
	 * This map contains the general table definitions for each class.
	 *
	 * key: class name (not normalized)
	 *
	 * value: table creation statement according to the class (normalized
	 * property names)
	 */
	private TreeMap<String, String> tablesByClassName = new TreeMap<String, String>();

	/**
	 * This map contains the definitions for all associative tables.
	 *
	 * key: name of associative table (not normalized)
	 *
	 * value: table creation statement
	 */
	private TreeMap<String, String> tablesByAssociativeTableName = new TreeMap<String, String>();

	/**
	 * This map is used to cache information for building geometry indexes
	 *
	 * key: table name (not normalized)
	 *
	 * value: geometry properties belonging to the table
	 */
	private TreeMap<String, List<PropertyInfo>> geometryPropsByTableName = new TreeMap<String, List<PropertyInfo>>();

	/**
	 * This map is used to cache information for altering tables to add
	 * reference columns
	 *
	 * key: table name (not normalized)
	 *
	 * value: ALTER TABLE statements with full column definition of the
	 * reference
	 */
	private TreeMap<String, List<String>> referenceColumnDefinitionsByTableName = new TreeMap<String, List<String>>();

	private TreeMap<String, List<String>> checkConstraintsForPropsWithEnumValueTypeByTableName = new TreeMap<String, List<String>>();

	/**
	 * This map is used to cache INSERT statements for code list tables
	 *
	 * key: name of code list (not normalized)
	 *
	 * value: INSERT statements for codes of the code list
	 */
	private TreeMap<String, List<String>> insertStatementsByCodeListName = new TreeMap<String, List<String>>();

	private List<String> geometryMetadataUpdateStatements;
	private List<String> geometryIndexCreationStatements;

	/**
	 * Contains information parsed from the 'param' attributes of each map entry
	 * defined for this target.
	 */
	private MapEntryParamInfos mepp = null;

	private TreeMap<String, String> alterTableStatementsByClassName = new TreeMap<String, String>();

	/**
	 * Used to keep track of the associations for which an associative table
	 * creation statement has already been generated.
	 * <p>
	 * Does not have an influence on the order in which statements are written.
	 */
	private Set<AssociationInfo> associationsWithAssociativeTable = new HashSet<AssociationInfo>();

	private DatabaseStrategy databaseStrategy;

	/**
	 * @see de.interactive_instruments.ShapeChange.Target.Target#initialise(de.interactive_instruments.ShapeChange.Model.PackageInfo,
	 *      de.interactive_instruments.ShapeChange.Model.Model,
	 *      de.interactive_instruments.ShapeChange.Options,
	 *      de.interactive_instruments.ShapeChange.ShapeChangeResult, boolean)
	 */
	public void initialise(PackageInfo pi, Model m, Options o,
			ShapeChangeResult r, boolean diagOnly)
			throws ShapeChangeAbortException {

		schema = pi;
		model = m;
		options = o;
		result = r;
		diagnosticsOnly = diagOnly;

		result.addDebug(this, 1, pi.name());

		outputDirectory = options.parameter(this.getClass().getName(),
				"outputDirectory");
		if (outputDirectory == null)
			outputDirectory = options.parameter("outputDirectory");
		if (outputDirectory == null)
			outputDirectory = options.parameter(".");

		// create output directory, if necessary
		if (!this.diagnosticsOnly) {

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
				result.addFatalError(this, 3, outputDirectory);
				return;
			}
		}

		if (pi.matches(RULE_TGT_SQL_ALL_ASSOCIATIVETABLES)) {
			this.createAssociativeTables = true;
		}

		if (pi.matches(RULE_TGT_SQL_ALL_FOREIGNKEY_ORACLE_NAMING_STYLE)) {
			this.oracleNamingStyle = true;
		}

		String databaseSystem = options.parameter(this.getClass().getName(),
				PARAM_DATABASE_SYSTEM);
		if (databaseSystem == null
				|| "postgresql".equalsIgnoreCase(databaseSystem)) {
			databaseStrategy = new PostgreSQLStrategy();
		} else if ("oracle".equalsIgnoreCase(databaseSystem)) {
			databaseStrategy = new OracleStrategy(result);
		} else if ("sqlserver".equalsIgnoreCase(databaseSystem)) {
			databaseStrategy = new SQLServerStrategy(result);
		} else {
			databaseStrategy = new NullDatabaseStrategy();
			result.addFatalError(this, 6, databaseSystem);
		}

		idColumnName = options.parameter(this.getClass().getName(),
				PARAM_ID_COLUMN_NAME);
		if (idColumnName == null) {
			idColumnName = DEFAULT_ID_COLUMN_NAME;
		}

		foreignKeyColumnSuffix = options.parameter(this.getClass().getName(),
				PARAM_FOREIGN_KEY_COLUMN_SUFFIX);
		if (foreignKeyColumnSuffix == null) {
			foreignKeyColumnSuffix = DEFAULT_FOREIGN_KEY_COLUMN_SUFFIX;
		}

		foreignKeyColumnSuffixDatatype = options.parameter(
				this.getClass().getName(),
				PARAM_FOREIGN_KEY_COLUMN_SUFFIX_DATATYPE);
		if (foreignKeyColumnSuffixDatatype == null) {
			foreignKeyColumnSuffixDatatype = DEFAULT_FOREIGN_KEY_COLUMN_SUFFIX_DATATYPE;
		}

		foreignKeyColumnDataType = options.parameter(this.getClass().getName(),
				PARAM_FOREIGN_KEY_COLUMN_DATA_TYPE);
		if (foreignKeyColumnDataType == null) {
			foreignKeyColumnDataType = databaseStrategy.primaryKeyDataType();
		}

		String defaultSizeByConfig = options
				.parameter(this.getClass().getName(), PARAM_SIZE);
		if (defaultSizeByConfig == null) {
			defaultSize = DEFAULT_SIZE;
		} else {
			try {
				defaultSize = Integer.parseInt(defaultSizeByConfig);
			} catch (NumberFormatException e) {
				MessageContext mc = result.addWarning(this, 4, PARAM_SIZE,
						e.getMessage(), "" + DEFAULT_SIZE);
				mc.addDetail(this, 0);
				defaultSize = DEFAULT_SIZE;
			}
		}

		String defaultSridByConfig = options
				.parameter(this.getClass().getName(), PARAM_SRID);
		if (defaultSridByConfig == null) {
			srid = DEFAULT_SRID;
		} else {
			try {
				srid = Integer.parseInt(defaultSridByConfig);
			} catch (NumberFormatException e) {
				MessageContext mc = result.addWarning(this, 4, PARAM_SRID,
						e.getMessage(), "" + DEFAULT_SRID);
				mc.addDetail(this, 0);
				srid = DEFAULT_SRID;
			}
		}

		String createReferencesByConfig = options
				.parameter(this.getClass().getName(), PARAM_CREATE_REFERENCES);
		if (createReferencesByConfig != null) {
			createReferences = Boolean
					.parseBoolean(createReferencesByConfig.trim());
		}

		String createDocumentationByConfig = options.parameter(
				this.getClass().getName(), PARAM_CREATE_DOCUMENTATION);
		if (createDocumentationByConfig != null) {
			createDocumentation = Boolean
					.parseBoolean(createDocumentationByConfig.trim());
		}

		// change the default documentation template?
		documentationTemplate = options.parameter(this.getClass().getName(),
				PARAM_DOCUMENTATION_TEMPLATE);
		documentationNoValue = options.parameter(this.getClass().getName(),
				PARAM_DOCUMENTATION_NOVALUE);

		String descriptorsForCodelistByConfig = options.parameter(
				this.getClass().getName(), PARAM_DESCRIPTORS_FOR_CODELIST);
		if (descriptorsForCodelistByConfig != null
				&& !descriptorsForCodelistByConfig.trim().isEmpty()) {
			descriptorsForCodelistFromConfig = descriptorsForCodelistByConfig
					.trim().split(",");
		}
		boolean unknownDescriptorFound = false;

		for (String tmp : descriptorsForCodelistFromConfig) {

			if (tmp.matches(DESCRIPTORS_FOR_CODELIST_REGEX)) {

				// parse descriptor string
				String name = null;
				String normalizedColumnName = null;
				Integer size = null;

				if (tmp.contains("(")) {

					name = tmp.substring(0, tmp.indexOf("("));
					String tmp2 = tmp.substring(tmp.indexOf("(") + 1,
							tmp.length() - 1);
					String[] metadata = tmp2.split(";");
					for (String meta : metadata) {
						String[] meta_parts = meta.split("=");
						if (meta_parts[0].equalsIgnoreCase("columnName")) {
							normalizedColumnName = normalizeName(meta_parts[1]);
						} else if (meta_parts[0].equalsIgnoreCase("size")) {
							size = new Integer(meta_parts[1]);
						}
					}

				} else {
					// no metadata defined for descriptor
					name = tmp;
				}

				if (normalizedColumnName == null) {
					normalizedColumnName = normalizeName(name);
				}

				descriptorsForCodelist.add(new DescriptorForCodeList(name,
						normalizedColumnName, size));

			} else {
				unknownDescriptorFound = true;
			}
		}
		if (unknownDescriptorFound) {
			result.addWarning(this, 23, descriptorsForCodelistByConfig,
					DESCRIPTORS_FOR_CODELIST_REGEX);
		}
		if (descriptorsForCodelist.isEmpty()) {
			result.addWarning(this, 24);
			descriptorsForCodelist.add(
					new DescriptorForCodeList("documentation", null, null));
		}

		/*
		 * set of parameters for naming of columns when converting code list to
		 * table
		 */
		String codeNameColumnNameByConfig = options.parameter(
				this.getClass().getName(), PARAM_CODE_NAME_COLUMN_NAME);
		if (codeNameColumnNameByConfig != null
				&& !codeNameColumnNameByConfig.trim().isEmpty()) {
			codeNameColumnName = codeNameColumnNameByConfig.trim();
		}
		normalizedCodeNameColumnName = normalizeName(codeNameColumnName);

		String codeNameSizeByConfig = options
				.parameter(this.getClass().getName(), PARAM_CODE_NAME_SIZE);
		if (codeNameSizeByConfig != null
				&& !codeNameSizeByConfig.trim().isEmpty()) {
			codeNameSize = new Integer(codeNameSizeByConfig.trim());
		}

		// reset processed flags on all classes in the schema
		for (Iterator<ClassInfo> k = model.classes(pi).iterator(); k
				.hasNext();) {
			ClassInfo ci = k.next();
			ci.processed(getTargetID(), false);
		}

		// identify map entries defined in the target configuration
		List<ProcessMapEntry> mapEntries = options.getCurrentProcessConfig()
				.getMapEntries();

		if (mapEntries == null || mapEntries.isEmpty()) {

			/*
			 * It is unlikely but not impossible that an application schema does
			 * not make use of types that require a type mapping in order to be
			 * converted into a database schema.
			 */
			result.addWarning(this, 15);

		} else {

			/*
			 * Store map entries in map with key being the 'type'.
			 */
			for (ProcessMapEntry pme : mapEntries) {

				this.mapEntryByType.put(pme.getType(), pme);
			}

			/*
			 * Parse all parameter information
			 */
			mepp = new MapEntryParamInfos(result, mapEntries);
		}
	}

	public void process(ClassInfo ci) {

		if (ci == null || ci.pkg() == null)
			return;

		if (ci.processed(getTargetID()))
			return;

		result.addDebug(this, 2, ci.name());

		if (this.mapEntryByType.containsKey(ci.name())) {
			ProcessMapEntry pme = this.mapEntryByType.get(ci.name());

			result.addInfo(this, 22, ci.name(), pme.getTargetType());
			return;
		}

		if (ci.isAbstract() && ci.matches(RULE_TGT_SQL_ALL_EXCLUDE_ABSTRACT)) {
			return;
		}

		// Create table creation statements
		if ((ci.category() == Options.OBJECT
				&& ci.matches(RULE_TGT_SQL_CLS_OBJECT_TYPES))
				|| (ci.category() == Options.FEATURE
						&& ci.matches(RULE_TGT_SQL_CLS_FEATURE_TYPES))
				|| (ci.category() == Options.DATATYPE
						&& ci.matches(RULE_TGT_SQL_CLS_DATATYPES))) {

			generateTableCreationAndAlterStatements(ci);

			if (createReferences) {
				generateForeignKeyDefinitions(ci);
			}

		} else if (ci.category() == Options.ENUMERATION) {

			// fine - tables won't be created for enumerations

		} else if (ci.category() == Options.CODELIST) {

			if (ci.matches(RULE_TGT_SQL_CLS_CODELISTS)) {

				generateTableForCodeList(ci);

			} else {
				// do not create tables for code lists
			}
		} else {

			result.addInfo(this, 17, ci.name());
		}

		ci.processed(getTargetID(), true);
	}

	/**
	 * Creates ALTER TABLE commands for any class property that refers to
	 * another class and shall thus receive a foreign key constraint.
	 *
	 * @param ci
	 */
	private void generateForeignKeyDefinitions(ClassInfo ci) {

		for (PropertyInfo pi : ci.properties().values()) {

			if (refersToTypeRepresentedByTable(pi)) {

				if (pi.cardinality().maxOccurs == 1) {

					createForeignKeyDefinition(ci.name(), pi);

					// } else if (!pi.isAttribute()
					// && pi.reverseProperty().cardinality().maxOccurs == 1) {
					//
					// createForeignKeyDefinition(
					// pi.reverseProperty().inClass().name(),
					// pi.reverseProperty());

				} else {

					if (createAssociativeTables) {

						/*
						 * fine - foreign key constraints have been created
						 * while creating associative tables
						 */

					} else {

						/*
						 * no need to warn again here, because a warning has
						 * already been issued during table creation
						 */
						// String rel1Class = pi.inClass().name();
						// String rel2Class = pi.typeInfo().name;
						//
						// result.addWarning(this, 8, rel1Class, rel2Class,
						// pi.name());
					}
				}
			}
		}
	}

	public void createForeignKeyDefinition(String className, PropertyInfo pi) {

		String res = "ALTER TABLE " + normalizeName(className)
				+ " ADD CONSTRAINT " + getForeignKeyIdentifier(pi)
				+ " FOREIGN KEY ("
				+ normalizeName(pi.name() + identifyForeignKeyColumnSuffix(pi))
				+ ") REFERENCES " + determineTableNameForValueType(pi) + ";"
				+ CRLF;

		if (!this.referenceColumnDefinitionsByTableName
				.containsKey(className)) {

			this.referenceColumnDefinitionsByTableName.put(className,
					new ArrayList<String>());
		}

		this.referenceColumnDefinitionsByTableName.get(className).add(res);
	}

	private String identifyForeignKeyColumnSuffix(PropertyInfo pi) {

		String typeName = pi.typeInfo().name;

		ProcessMapEntry pme = this.mapEntryByType.get(typeName);

		if (pme != null && mepp.hasCharacteristic(typeName, ME_PARAM_TABLE,
				ME_PARAM_TABLE_CHARACT_REP_CAT)) {

			String repCat = mepp.getCharacteristic(typeName, ME_PARAM_TABLE,
					ME_PARAM_TABLE_CHARACT_REP_CAT);

			if (repCat != null && repCat.equalsIgnoreCase("datatype")) {
				return foreignKeyColumnSuffixDatatype;
			} else {
				return foreignKeyColumnSuffix;
			}

		} else if (pi.categoryOfValue() == Options.DATATYPE) {
			return foreignKeyColumnSuffixDatatype;
		} else {
			return foreignKeyColumnSuffix;
		}
	}

	/**
	 * @param tableName
	 *            (does not need to be normalized)
	 * @param fieldName
	 *            (does not need to be normalized)
	 * @param targetTableName
	 *            (does not need to be normalized)
	 */
	public void createForeignKeyDefinition(String tableName, String fieldName,
			String targetTableName) {

		String res = "ALTER TABLE " + normalizeName(tableName)
				+ " ADD CONSTRAINT "
				+ getForeignKeyIdentifier(tableName, fieldName, targetTableName)
				+ " FOREIGN KEY (" + normalizeName(fieldName) + ") REFERENCES "
				+ normalizeName(targetTableName) + ";" + CRLF;

		if (!this.referenceColumnDefinitionsByTableName
				.containsKey(tableName)) {

			this.referenceColumnDefinitionsByTableName.put(tableName,
					new ArrayList<String>());
		}

		this.referenceColumnDefinitionsByTableName.get(tableName).add(res);
	}

	private String getForeignKeyIdentifier(PropertyInfo pi) {

		String targetTableName = determineTableNameForValueType(pi);

		return getForeignKeyIdentifier(pi.inClass().name(), pi.name(),
				targetTableName);
	}

	/**
	 * @param tableName
	 * @param fieldName
	 * @return the normalized identifier for the foreign key
	 */
	private String getForeignKeyIdentifier(String tableName, String fieldName,
			String targetTableName) {

		/*
		 * The following is most often too long, thus we simply use the table
		 * and field name
		 */
		// String res = "fk_" + className + "_" + fieldName + "_to_"
		// + targetClassName;

		String res;

		if (oracleNamingStyle) {

			String tableNameForFK = tableName.length() > 8
					? tableName.substring(0, 8) : tableName;
			String fieldNameForFK = fieldName.length() > 8
					? fieldName.substring(0, 8) : fieldName;
			String targetTableNameForFK = targetTableName.length() > 8
					? targetTableName.substring(0, 8) : targetTableName;
			String fk = "fk_" + tableNameForFK + "_" + targetTableNameForFK
					+ "_" + fieldNameForFK;

			if (countByForeignKeyOracleStyle.containsKey(fk)) {

				Integer count = countByForeignKeyOracleStyle.get(fk);

				if (count > 9) {
					result.addError(this, 20, fk);
				}

				res = fk + count;
				countByForeignKeyOracleStyle.put(fk, new Integer(count + 1));

			} else {

				res = fk;
				countByForeignKeyOracleStyle.put(fk, new Integer(0));
			}

		} else {
			res = "fk_" + tableName + "_" + fieldName;
		}

		return normalizeName(res);
	}

	private void generateTableCreationAndAlterStatements(ClassInfo ci) {

		/*
		 * The order of the properties is defined by their sequence numbers
		 * (which is automatically provided by a TreeMap).
		 */
		SortedMap<StructuredNumber, PropertyInfo> pis = ci.properties();

		List<PropertyInfo> propertyInfosForColumns = new ArrayList<PropertyInfo>();

		for (PropertyInfo pi : pis.values()) {

			/*
			 * If the value type of the property is part of the schema but not
			 * enabled for conversion, issue a warning and continue.
			 */

			// try getting the type class by ID first, then by name
			ClassInfo typeCi = model.classById(pi.typeInfo().id);

			if (typeCi == null) {
				typeCi = model.classByName(pi.typeInfo().name);
			}

			if (typeCi != null && typeCi.inSchema(schema) && ((typeCi
					.category() == Options.OBJECT
					&& !typeCi.matches(RULE_TGT_SQL_CLS_OBJECT_TYPES))
					|| (typeCi.category() == Options.FEATURE
							&& !typeCi.matches(RULE_TGT_SQL_CLS_FEATURE_TYPES))
					|| (typeCi.category() == Options.DATATYPE
							&& !typeCi.matches(RULE_TGT_SQL_CLS_DATATYPES)))) {

				result.addWarning(this, 16, pi.name(), pi.inClass().name(),
						pi.typeInfo().name);
				continue;
			}

			if (pi.isDerived()
					&& pi.matches(RULE_TGT_SQL_PROP_EXCLUDE_DERIVED)) {

				result.addInfo(this, 14, pi.name(), ci.name());
				continue;
			}

			if (typeCi != null && typeCi.isAbstract()
					&& typeCi.matches(RULE_TGT_SQL_ALL_EXCLUDE_ABSTRACT)) {
				continue;
			}

			if (pi.isAttribute()) {

				if (pi.cardinality().maxOccurs == 1) {

					propertyInfosForColumns.add(pi);

				} else if (createAssociativeTables) {

					createAssociativeTableForAttribute(pi);

				} else {
					/*
					 * Warn that attribute with max multiplicity > 1 is not
					 * supported when creation of associative tables is not
					 * enabled.
					 */
					result.addWarning(this, 11, pi.name(), pi.inClass().name());
				}

			} else {

				// the property is an association role

				AssociationInfo ai = pi.association();

				/*
				 * if an associative table has already been created for this
				 * association, continue
				 */
				if (associationsWithAssociativeTable.contains(ai)) {
					continue;
				}

				PropertyInfo revPi = pi.reverseProperty();

				int maxOccursPi = pi.cardinality().maxOccurs;
				int maxOccursRevPi = revPi.cardinality().maxOccurs;

				/*
				 * note: pi is navigable, otherwise it wouldn't occur as
				 * property of ci
				 */

				if (maxOccursPi == 1) {

					propertyInfosForColumns.add(pi);

				} else {

					if (revPi.isNavigable() && maxOccursRevPi == 1) {

						/*
						 * MaxOccurs = 1 on both ends -> the relationship will
						 * be represented by the foreign key field that
						 * represents the reverse property in its inClass.
						 */

					} else {

						/*
						 * The reverse property is not navigable or both
						 * association roles have a maximum multiplicity greater
						 * than 1 - both situations represent an n:m
						 * relationship
						 */

						if (createAssociativeTables) {

							if (associationsWithAssociativeTable.contains(ai)) {

								/*
								 * An associative table has already been created
								 * to represent this relationship.
								 */

							} else {

								createAssociativeTable(ai);
								associationsWithAssociativeTable.add(ai);
							}

						} else {

							PropertyInfo pi1, pi2;

							if (pi.inClass().name().compareTo(pi
									.reverseProperty().inClass().name()) <= 0) {
								pi1 = pi;
								pi2 = pi.reverseProperty();
							} else {
								pi1 = pi.reverseProperty();
								pi2 = pi;
							}

							result.addWarning(this, 8, pi1.inClass().name(),
									pi1.name(), pi2.inClass().name(),
									pi2.name());
						}
					}
				}
			}
		}

		// if (propertyInfosForColumns.size() > 0) {
		generateTableCreationStatement(ci, propertyInfosForColumns);
		// }
	}

	/**
	 * @param ai
	 */
	private void createAssociativeTable(AssociationInfo ai) {

		StringBuffer sb = new StringBuffer();

		// identify table name - using tagged value or default name
		String tableName = ai.taggedValuesAll()
				.getFirstValue(TV_ASSOCIATIVETABLE);

		String tableNameEnd1InClass = determineTableNameForType(
				ai.end1().inClass());
		String tableNameEnd2InClass = determineTableNameForType(
				ai.end2().inClass());

		if (tableName == null || tableName.trim().length() == 0) {

			if (ai.end1().isNavigable() && ai.end2().isNavigable()) {

				// choose name based on alphabetical order
				if (tableNameEnd1InClass.compareTo(tableNameEnd2InClass) <= 0) {

					tableName = tableNameEnd1InClass + "_" + ai.end1().name();

				} else {

					tableName = tableNameEnd2InClass + "_" + ai.end2().name();
				}

			} else if (ai.end1().isNavigable()) {

				tableName = tableNameEnd1InClass + "_" + ai.end1().name();

			} else {
				// ai.end2 is navigable
				tableName = tableNameEnd2InClass + "_" + ai.end2().name();
			}

			result.addInfo(this, 13,
					ai.end1().inClass().name() + " (context property '"
							+ ai.end1().name() + "')",
					ai.end2().inClass().name() + " (context property '"
							+ ai.end2().name() + "')",
					tableName);
		}

		// add table creation statement
		addCRLF(sb, "CREATE TABLE " + normalizeName(tableName) + " (");
		newLine(sb);

		/*
		 * ensure that reference fields are created in lexicographical order of
		 * their inClass names
		 */
		PropertyInfo pi1, pi2;

		if (tableNameEnd1InClass.compareTo(tableNameEnd2InClass) <= 0) {
			pi1 = ai.end1();
			pi2 = ai.end2();
		} else {
			pi1 = ai.end2();
			pi2 = ai.end1();
		}

		boolean reflexive = pi1.inClass().id().equals(pi2.inClass().id());

		// add field for first reference
		String name_1 = determineTableNameForType(pi1.inClass())
				+ (reflexive ? "_" + pi1.name() : "") + idColumnName;
		String name_1_norm = normalizeName(name_1);
		indent(sb, name_1_norm + " " + foreignKeyColumnDataType + " NOT NULL,");

		// add field for second reference
		String name_2 = determineTableNameForType(pi2.inClass())
				+ (reflexive ? "_" + pi2.name() : "") + idColumnName;
		String name_2_norm = normalizeName(name_2);
		indent(sb, name_2_norm + " " + foreignKeyColumnDataType + " NOT NULL,");

		// add primary key definition
		indent(sb, "PRIMARY KEY (" + name_1_norm + "," + name_2_norm + ")");

		addCRLF(sb, ");");
		newLine(sb);

		// now store the table creation statement for later use
		this.tablesByAssociativeTableName.put(tableName, sb.toString());

		// add foreign key constraint(s)
		if (createReferences) {
			createForeignKeyDefinition(tableName, name_1,
					determineTableNameForType(pi1.inClass()));
			createForeignKeyDefinition(tableName, name_2,
					determineTableNameForType(pi2.inClass()));
		}
	}

	/**
	 * @param ci
	 * @return If a map entry with param = {@value #ME_PARAM_TABLE} is defined
	 *         for the given class, the targetType defined by the map entry is
	 *         returned. Otherwise the name of the class is returned.
	 */
	private String determineTableNameForType(ClassInfo ci) {

		ProcessMapEntry pme = this.mapEntryByType.get(ci.name());

		if (pme != null && mepp.hasParameter(ci.name(), ME_PARAM_TABLE)) {

			return pme.getTargetType();

		} else {

			return ci.name();
		}
	}

	/**
	 * NOTE: only works for attributes, NOT association roles
	 *
	 * @param pi
	 */
	private void createAssociativeTableForAttribute(PropertyInfo pi) {

		if (!pi.isAttribute()) {
			return;
		}

		StringBuffer sb = new StringBuffer();

		// identify table name - using tagged value or default name
		String tableName = pi.taggedValuesAll()
				.getFirstValue(TV_ASSOCIATIVETABLE);

		if (tableName == null || tableName.trim().length() == 0) {

			tableName = pi.inClass().name() + "_" + pi.name();

			result.addInfo(this, 12, pi.name(), pi.inClass().name(), tableName);
		}

		// add table creation statement
		addCRLF(sb, "CREATE TABLE " + normalizeName(tableName) + " (");
		newLine(sb);

		// add field to reference pi.inClass
		String classReferenceFieldName = pi.inClass().name() + idColumnName;
		String normalizedClassReferenceFieldName = normalizeName(
				classReferenceFieldName);
		indent(sb, normalizedClassReferenceFieldName + " "
				+ foreignKeyColumnDataType + " NOT NULL,");

		String normalizedPiFieldName;
		boolean createForeignKeyConstraintForPiField = false;

		if (refersToTypeRepresentedByTable(pi)) {

			normalizedPiFieldName = normalizeName(
					determineTableNameForValueType(pi) + idColumnName);

			if (pi.categoryOfValue() == Options.CODELIST
					&& pi.inClass().matches(RULE_TGT_SQL_CLS_CODELISTS)) {

				String fieldType;
				if (codeNameSize == null) {
					fieldType = databaseStrategy
							.unlimitedLengthCharacterDataType();
				} else {
					fieldType = databaseStrategy
							.limitedLengthCharacterDataType(codeNameSize);
				}
				indent(sb,
						normalizedPiFieldName + " " + fieldType + " NOT NULL,");
			} else {
				indent(sb, normalizedPiFieldName + " "
						+ foreignKeyColumnDataType + " NOT NULL,");
			}

			createForeignKeyConstraintForPiField = true;

		} else {

			normalizedPiFieldName = normalizeName(pi.name());
			indent(sb, generateColumnDefinition(pi, true, true));

			if (pi.categoryOfValue() == Options.ENUMERATION && pi.matches(
					RULE_TGT_SQL_PROP_CHECK_CONSTRAINTS_FOR_ENUMERATIONS)) {
				generateCheckConstraintForEnumerationValueType(tableName, pi);
			}

			if (isGeometryTypedProperty(pi)) {
				notePropertyForGeometryIndexCreation(pi, tableName);
			}
		}

		// add primary key definition
		indent(sb, "PRIMARY KEY (" + normalizedClassReferenceFieldName + ","
				+ normalizedPiFieldName + ")");

		addCRLF(sb, ");");
		newLine(sb);

		// now store the table creation statement for later use
		this.tablesByAssociativeTableName.put(tableName, sb.toString());

		// add foreign key constraint(s)
		if (createReferences) {
			createForeignKeyDefinition(tableName, classReferenceFieldName,
					pi.inClass().name());
			if (createForeignKeyConstraintForPiField) {
				createForeignKeyDefinition(tableName, normalizedPiFieldName,
						determineTableNameForValueType(pi));
			}
		}
	}

	private void notePropertyForGeometryIndexCreation(PropertyInfo pi,
			String tableName) {

		if (!geometryPropsByTableName.containsKey(tableName)) {
			// initialise the list for storage of geometry columns
			// for the table
			geometryPropsByTableName.put(tableName,
					new ArrayList<PropertyInfo>());
		}
		// keep track of geometry column for index creation later on
		geometryPropsByTableName.get(tableName).add(pi);
	}

	/**
	 * @param tableName
	 *            - does not need to be normalized
	 * @param pi
	 */
	private void generateCheckConstraintForEnumerationValueType(
			String tableName, PropertyInfo pi) {

		/*
		 * ignore the constraint if the enumeration which is the value type of
		 * pi is mapped to a simple type (example: usage of the 'Boolean' type
		 * from ISO 19103).
		 */
		ProcessMapEntry pme = this.mapEntryByType.get(pi.typeInfo().name);

		if (pme != null && !pme.hasParam()) {
			return;
		}

		// look up the enumeration type
		ClassInfo enumCi = model.classById(pi.typeInfo().id);

		if (enumCi == null || enumCi.properties().size() == 0) {

			result.addError(this, 18, pi.typeInfo().name,
					pi.fullNameInSchema());
		} else {

			StringBuffer sb2 = new StringBuffer();

			sb2.append("ALTER TABLE " + normalizeName(tableName)
					+ " ADD CONSTRAINT "
					+ createNameCheckConstraint(tableName, pi.name())
					+ " CHECK (" + normalizeName(pi.name()) + " IN (");

			for (PropertyInfo enumPi : enumCi.properties().values()) {
				sb2.append("'" + StringUtils.replace(enumPi.name(), "'", "''")
						+ "', "); // escape single quotes in the enumeration
									// value
			}
			sb2.delete(sb2.length() - 2, sb2.length());
			sb2.append("));" + CRLF);

			if (!this.checkConstraintsForPropsWithEnumValueTypeByTableName
					.containsKey(tableName)) {

				this.checkConstraintsForPropsWithEnumValueTypeByTableName
						.put(tableName, new ArrayList<String>());
			}

			this.checkConstraintsForPropsWithEnumValueTypeByTableName
					.get(tableName).add(sb2.toString());

		}
	}

	/**
	 * @param pi
	 * @return If a map entry with param = {@value #ME_PARAM_TABLE} is defined
	 *         for the value type of the property, the targetType defined by the
	 *         map entry is returned. Otherwise the normalized name of the value
	 *         type is returned.
	 */
	private String determineTableNameForValueType(PropertyInfo pi) {

		String valueTypeName = pi.typeInfo().name;

		ProcessMapEntry pme = this.mapEntryByType.get(valueTypeName);

		if (pme != null && mepp.hasParameter(valueTypeName, ME_PARAM_TABLE)) {

			return pme.getTargetType();

		} else {

			return normalizeName(pi.typeInfo().name);
		}
	}

	public void generateTableCreationStatement(ClassInfo ci,
			List<PropertyInfo> propertyInfosForColumns) {

		StringBuffer sb = new StringBuffer();

		addCRLF(sb, "CREATE TABLE " + normalizeName(ci.name()) + " (");
		newLine(sb);

		// create the column to store the object identifier
		String oid_field_stmt = idColumnName + " "
				+ databaseStrategy.primaryKeyDataType()
				+ " NOT NULL PRIMARY KEY";

		if (!propertyInfosForColumns.isEmpty()) {
			indent(sb, oid_field_stmt + ",");
		} else {
			indent(sb, oid_field_stmt);
		}

		for (int i = 0; i < propertyInfosForColumns.size(); i++) {

			PropertyInfo pi = propertyInfosForColumns.get(i);

			/*
			 * generate column definitions with comma until we've reached the
			 * last column
			 */
			if (i < propertyInfosForColumns.size() - 1) {
				indent(sb, generateColumnDefinition(pi, true));
			} else {
				indent(sb, generateColumnDefinition(pi, false));
			}

			if (pi.categoryOfValue() == Options.ENUMERATION && pi.matches(
					RULE_TGT_SQL_PROP_CHECK_CONSTRAINTS_FOR_ENUMERATIONS)) {
				generateCheckConstraintForEnumerationValueType(ci.name(), pi);
			}

			if (isGeometryTypedProperty(pi)) {
				notePropertyForGeometryIndexCreation(pi, ci.name());
			}
		}

		addCRLF(sb, ");");
		newLine(sb);

		// now store the table creation statement for later use
		this.tablesByClassName.put(ci.name(), sb.toString());
	}

	public void generateTableForCodeList(ClassInfo ci) {

		StringBuffer sb = new StringBuffer();

		String codeListTableName = normalizeName(ci.name());

		addCRLF(sb, "CREATE TABLE " + codeListTableName + " (");
		newLine(sb);

		// --- create the columns for codes

		// create required column to store the code name
		StringBuffer name_column_stmt = new StringBuffer();
		name_column_stmt.append(normalizedCodeNameColumnName);
		name_column_stmt.append(" ");
		if (codeNameSize == null) {
			name_column_stmt.append(
					databaseStrategy.unlimitedLengthCharacterDataType());
		} else {
			name_column_stmt.append(databaseStrategy
					.limitedLengthCharacterDataType(codeNameSize));
		}
		name_column_stmt.append(" NOT NULL");
		if (!ci.matches(RULE_TGT_SQL_CLS_CODELISTS_PODS)) {
			name_column_stmt.append(" PRIMARY KEY");
		}

		if (!descriptorsForCodelist.isEmpty()) {
			name_column_stmt.append(",");
		}
		indent(sb, name_column_stmt.toString());

		/*
		 * now add one column for each descriptor, as specified via the
		 * configuration
		 */
		for (Iterator<DescriptorForCodeList> iter = descriptorsForCodelist
				.iterator(); iter.hasNext();) {

			DescriptorForCodeList descriptor = iter.next();

			StringBuffer column_stmt = new StringBuffer();
			column_stmt.append(descriptor.getNormalizedColumnName());
			column_stmt.append(" ");
			if (descriptor.getSize() == null) {
				column_stmt.append(
						databaseStrategy.unlimitedLengthCharacterDataType());
			} else {
				column_stmt.append(databaseStrategy
						.limitedLengthCharacterDataType(descriptor.getSize()));
			}
			if (iter.hasNext() || ci.matches(RULE_TGT_SQL_CLS_CODELISTS_PODS)) {
				column_stmt.append(",");
			}

			indent(sb, column_stmt.toString());
		}

		if (ci.matches(RULE_TGT_SQL_CLS_CODELISTS_PODS)) {

			indent(sb, "ACTIVE_INDICATOR_LF CHAR(1) NULL,");
			indent(sb, "CONSTRAINT "
					+ normalizeName("CKC_AI_" + codeListTableName)
					+ " CHECK (ACTIVE_INDICATOR_LF IS NULL OR (ACTIVE_INDICATOR_LF IN ('Y','N'))),");
			indent(sb, "SOURCE_GCL VARCHAR(16) NULL,");
			indent(sb,
					"CONSTRAINT " + normalizeName("PK_" + codeListTableName)
							+ " PRIMARY KEY NONCLUSTERED ("
							+ normalizedCodeNameColumnName + ")");
		}

		addCRLF(sb, ");");
		newLine(sb);

		// now store the table creation statement for later use
		this.tablesByClassName.put(ci.name(), sb.toString());

		// -------------------------
		// create INSERT statements
		// -------------------------

		List<String> insertStatements = new ArrayList<String>();

		for (PropertyInfo codePi : ci.properties().values()) {

			StringBuffer codeSb = new StringBuffer();

			addCRLF(codeSb, "INSERT INTO " + codeListTableName);

			codeSb.append("(" + normalizedCodeNameColumnName);
			if (!descriptorsForCodelist.isEmpty()) {
				codeSb.append(", ");
			}

			for (Iterator<DescriptorForCodeList> iter = descriptorsForCodelist
					.iterator(); iter.hasNext();) {

				DescriptorForCodeList descriptor = iter.next();

				codeSb.append(descriptor.getNormalizedColumnName());
				if (iter.hasNext()
						|| ci.matches(RULE_TGT_SQL_CLS_CODELISTS_PODS)) {
					codeSb.append(", ");
				}
			}

			if (ci.matches(RULE_TGT_SQL_CLS_CODELISTS_PODS)) {
				codeSb.append("ACTIVE_INDICATOR_LF, SOURCE_GCL");
			}

			addCRLF(codeSb, ")");
			addCRLF(codeSb, "VALUES");

			String codeName = codePi.name();
			if (codePi.initialValue() != null) {
				codeName = codePi.initialValue();
			}

			codeSb.append("('" + codeName + "'");
			if (!descriptorsForCodelist.isEmpty()) {
				codeSb.append(", ");
			}

			for (Iterator<DescriptorForCodeList> iter = descriptorsForCodelist
					.iterator(); iter.hasNext();) {

				DescriptorForCodeList descriptor = iter.next();
				String descName = descriptor.getDescriptorName();
				String value = null;

				if (descName.equalsIgnoreCase("name")) {

					value = codePi.name();

				} else if (descName.equalsIgnoreCase("documentation")) {

					value = codePi.derivedDocumentation(documentationTemplate,
							documentationNoValue);

				} else if (descName.equalsIgnoreCase("alias")) {

					value = codePi.aliasName();

				} else if (descName.equalsIgnoreCase("definition")) {

					value = codePi.definition();

				} else if (descName.equalsIgnoreCase("description")) {

					value = codePi.description();

				} else if (descName.equalsIgnoreCase("example")) {

					String[] examples = codePi.examples();
					if (examples != null && examples.length > 0) {
						value = StringUtils.join(examples, " ");
					}

				} else if (descName.equalsIgnoreCase("legalBasis")) {

					value = codePi.legalBasis();

				} else if (descName.equalsIgnoreCase("dataCaptureStatement")) {

					String[] dcss = codePi.dataCaptureStatements();
					if (dcss != null && dcss.length > 0) {
						value = StringUtils.join(dcss, " ");
					}

				} else if (descName.equalsIgnoreCase("primaryCode")) {

					value = codePi.primaryCode();
				}

				if (value == null) {
					value = "NULL";
				} else {

					value = "'" + value + "'";
				}

				if (iter.hasNext()
						|| ci.matches(RULE_TGT_SQL_CLS_CODELISTS_PODS)) {
					value = value + ", ";
				}
				codeSb.append(value);
			}

			if (ci.matches(RULE_TGT_SQL_CLS_CODELISTS_PODS)) {
				codeSb.append("'Y', NULL");
			}

			addCRLF(codeSb, ");");
			newLine(codeSb);

			insertStatements.add(codeSb.toString());
		}
		this.insertStatementsByCodeListName.put(ci.name(), insertStatements);
	}

	public void createAlterTableStatementForReverseProperty(PropertyInfo pi) {

		String reversePropertyName = pi.reverseProperty().name();

		if (reversePropertyName.matches("role_(S|T)\\d*")) {
			result.addWarning(this, 7, reversePropertyName,
					pi.reverseProperty().inClass().name(), pi.inClass().name());
		}

		StringBuffer sb2 = new StringBuffer();

		sb2.append("ALTER TABLE " + normalizeName(
				determineTableNameForType(pi.reverseProperty().inClass())));
		sb2.append(
				" ADD " + generateColumnDefinition(pi.reverseProperty(), false)
						+ ";" + CRLF);

		alterTableStatementsByClassName
				.put(pi.reverseProperty().inClass().name(), sb2.toString());
	}

	/**
	 * Creates the column definition based upon the property name, its type, and
	 * a possibly defined initial value. Also adds "NOT NULL" for all properties
	 * that can be nil/null (set via tagged value or stereotype) or which are
	 * optional.
	 *
	 * @param pi
	 * @param addComma
	 *            true if a ',' shall be added at the end of the statement
	 *            (before a potential comment), else false (specifically used
	 *            for the last column in a table creation statement).
	 * @return The SQL statement with the definition of the column to represent
	 *         the property
	 */
	private String generateColumnDefinition(PropertyInfo pi, boolean addComma) {

		return generateColumnDefinition(pi, addComma, false);
	}

	/**
	 * Creates the column definition based upon the property name, its type, and
	 * a possibly defined initial value. Also adds "NOT NULL" if indicated via
	 * parameter or for all properties that can be nil/null (set via tagged
	 * value or stereotype) or which are optional.
	 *
	 * @param pi
	 * @param addComma
	 *            true if a ',' shall be added at the end of the statement
	 *            (before a potential comment), else false (specifically used
	 *            for the last column in a table creation statement).
	 * @param alwaysNotNull
	 *            <code>true</code> if the column definition shall be created
	 *            with NOT NULL, otherwise <code>false</code> (then default
	 *            behavior applies)
	 * @return The SQL statement with the definition of the column to represent
	 *         the property
	 */
	private String generateColumnDefinition(PropertyInfo pi, boolean addComma,
			boolean alwaysNotNull) {

		String columnName;
		if (refersToTypeRepresentedByTable(pi)) {
			columnName = normalizeName(
					pi.name() + identifyForeignKeyColumnSuffix(pi));
		} else {
			columnName = normalizeName(pi.name());
		}

		String columnType = identifyType(pi);
		String columnDefault = pi.initialValue();

		String result = columnName + " " + columnType;

		if (columnDefault != null && columnDefault.trim().length() > 0) {
			/*
			 * strings in UML: enclosed by " strings in SQL: enclosed by '
			 */
			String defaultValue = null;
			if (pi.typeInfo().name.equals("Boolean")) {

				if (pattern_find_true.matcher(columnDefault).find()) {

					defaultValue = databaseStrategy.convertDefaultValue(true);

				} else if (pattern_find_false.matcher(columnDefault).find()) {

					defaultValue = databaseStrategy.convertDefaultValue(false);
				}
			}

			if (defaultValue == null) {
				defaultValue = columnDefault.trim().replace("\"", "'");
			}

			result = result + " DEFAULT " + defaultValue;
		}

		// ----- add constraints

		if (alwaysNotNull) {
			result = result + " NOT NULL";
		} else {
			// set NOT NULL if required
			if (pi.implementedByNilReason() || pi.nilReasonAllowed()
					|| pi.voidable() || pi.cardinality().minOccurs < 1) {
				/*
				 * in these cases the default behavior (that the field can be
				 * NULL) is ok
				 */
			} else {
				result = result + " NOT NULL";
			}
		}

		// ----- finalize column definition
		if (addComma) {
			result = result + ",";
		}

		if (createDocumentation) {
			String s = pi.derivedDocumentation(documentationTemplate,
					documentationNoValue);
			if (s != null) {
				result = result + "   -- " + s.replaceAll("\\s+", " ");
			}
		}

		return result;
	}

	/**
	 * @param pi
	 * @return If a map entry is defined for the type, then the return value is
	 *         <code>true</code> if the entry specifies (via the parameter) a
	 *         mapping to a table, else <code>false</code> is returned.
	 *         Otherwise, if the value type of the property is a feature,
	 *         object, data type, or code list that: 1) can be found in the
	 *         model, 2) table creation for the type is allowed (defined by the
	 *         conversion rules), and 3) is in the currently processed schema OR
	 *         {@value #RULE_TGT_SQL_CLS_REFERENCES_TO_EXTERNAL_TYPES} is
	 *         enabled, then the return value is <code>true</code> - else
	 *         <code>false</code>.
	 */
	public boolean refersToTypeRepresentedByTable(PropertyInfo pi) {

		String valueTypeName = pi.typeInfo().name;

		ProcessMapEntry pme = this.mapEntryByType.get(valueTypeName);

		if (pme != null) {

			if (mepp.hasParameter(valueTypeName, ME_PARAM_TABLE)) {
				return true;
			} else {
				return false;
			}

		} else if (pi.categoryOfValue() == Options.FEATURE
				|| pi.categoryOfValue() == Options.OBJECT
				|| pi.categoryOfValue() == Options.DATATYPE
				|| pi.categoryOfValue() == Options.CODELIST) {

			ClassInfo typeCi = this.model.classById(pi.typeInfo().id);

			if (typeCi != null) {

				if ((pi.categoryOfValue() == Options.OBJECT
						&& !typeCi.matches(RULE_TGT_SQL_CLS_OBJECT_TYPES))
						|| (pi.categoryOfValue() == Options.FEATURE && !typeCi
								.matches(RULE_TGT_SQL_CLS_FEATURE_TYPES))
						|| (pi.categoryOfValue() == Options.DATATYPE
								&& !typeCi.matches(RULE_TGT_SQL_CLS_DATATYPES))
						|| (pi.categoryOfValue() == Options.CODELIST && !typeCi
								.matches(RULE_TGT_SQL_CLS_CODELISTS))) {

					return false;

				} else {

					if (typeCi.inSchema(schema) || typeCi.matches(
							RULE_TGT_SQL_CLS_REFERENCES_TO_EXTERNAL_TYPES)) {

						return true;

						/*
						 * NOTE: if the schema uses external types, map entries
						 * should be defined. This helps avoiding confusion of
						 * types that are not processed but used in the model
						 * (e.g. from ISO packages, or application schemas that
						 * were not selected for processing). The rule to allow
						 * references to external types is a convenience
						 * mechanism.
						 */

					} else {

						return false;
					}
				}

			} else {

				return false;
			}

		} else {

			return false;
		}

	}

	/**
	 * @param pi
	 * @return <code>true</code> if the value type of the given property is a
	 *         geometry type - which requires a map entry for the value type
	 *         whose param contains the {@value #ME_PARAM_GEOMETRY} parameter;
	 *         otherwise <code>false</code> is returned.
	 */
	private boolean isGeometryTypedProperty(PropertyInfo pi) {

		String piTypeName = pi.typeInfo().name;

		ProcessMapEntry me = this.mapEntryByType.get(piTypeName);

		if (me != null && mepp.hasParameter(piTypeName, ME_PARAM_GEOMETRY)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Identifies the type to use in the SQL definition of the property.
	 *
	 * At first, standard mappings (defined via the configuration) are applied.
	 * If there is no direct standard mapping, then conditional mappings based
	 * upon the category/stereotype of the type is performed: enumeration,
	 * codelist and object types are mapped to 'text' or 'character varying'. If
	 * the type is a feature, 'bigserial' is returned (actual foreign key
	 * references can be added via a separate processing step, see
	 * {@link #generateForeignKeyDefinitions(ClassInfo)}. If all else fails,
	 * 'unknown' is returned as type.
	 *
	 * @param pi
	 * @return the type to use in the SQL definition of the property
	 */
	private String identifyType(PropertyInfo pi) {

		// first apply well-known mappings
		String piTypeName = pi.typeInfo().name;

		// try to get type from map entries
		ProcessMapEntry me = this.mapEntryByType.get(piTypeName);

		if (me != null) {

			if (mepp.hasParameter(piTypeName, ME_PARAM_GEOMETRY)) {

				return databaseStrategy.geometryDataType(me, srid);

			} else if (mepp.hasParameter(piTypeName, ME_PARAM_TABLE)) {

				return foreignKeyColumnDataType;

			} else {

				if (me.getTargetType().startsWith(MAP_TARGETTYPE_COND_PART)) {

					String conditionalCriterium = me.getTargetType()
							.substring(MAP_TARGETTYPE_COND_PART.length());

					if (conditionalCriterium.equalsIgnoreCase(
							MAP_TARGETTYPE_COND_TEXTORCHARACTERVARYING)) {
						return determineCharacterVaryingOrText(pi);
					}

				} else {

					return me.getTargetType();
				}
			}
		}

		// try to identify a type mapping based upon the category of the
		// property value
		int catOfValue = pi.categoryOfValue();

		if (catOfValue == Options.ENUMERATION) {

			return determineCharacterVaryingOrText(pi);

		} else if (catOfValue == Options.OBJECT || catOfValue == Options.FEATURE
				|| catOfValue == Options.DATATYPE
				|| catOfValue == Options.CODELIST) {

			ClassInfo typeCi = this.model.classById(pi.typeInfo().id);

			if (typeCi != null) {

				if ((catOfValue == Options.OBJECT
						&& !typeCi.matches(RULE_TGT_SQL_CLS_OBJECT_TYPES))
						|| (catOfValue == Options.FEATURE && !typeCi
								.matches(RULE_TGT_SQL_CLS_FEATURE_TYPES))
						|| (catOfValue == Options.DATATYPE
								&& !typeCi.matches(RULE_TGT_SQL_CLS_DATATYPES))
						|| (catOfValue == Options.CODELIST && !typeCi
								.matches(RULE_TGT_SQL_CLS_CODELISTS))) {

					/*
					 * table creation for this category is not enabled -> assign
					 * textual type
					 */
					return determineCharacterVaryingOrText(pi);

				} else {

					if (typeCi.inSchema(schema) || typeCi.matches(
							RULE_TGT_SQL_CLS_REFERENCES_TO_EXTERNAL_TYPES)) {

						if (catOfValue == Options.CODELIST) {
							
							if (codeNameSize == null) {
								return databaseStrategy
										.unlimitedLengthCharacterDataType();
							} else {
								return databaseStrategy
										.limitedLengthCharacterDataType(codeNameSize);
							}
														
						} else {
							return foreignKeyColumnDataType;
						}

					} else {
						result.addWarning(this, 9, typeCi.name(), pi.name(),
								pi.inClass().name());
						return determineCharacterVaryingOrText(pi);
					}
				}

			} else {
				result.addWarning(this, 10, pi.typeInfo().name, pi.name(),
						pi.inClass().name());
				return determineCharacterVaryingOrText(pi);
			}

		}

		result.addWarning(this, 21, piTypeName);

		return "unknown";
	}

	/**
	 * Determines if the property should have a type that allows unlimited or
	 * limited text size. This depends upon the setting of {@value #PARAM_SIZE},
	 * locally via a tagged value or globally via a configuration parameter or
	 * the default value defined by this class ({@value #DEFAULT_SIZE}): if
	 * {@value #PARAM_SIZE} is 0 or negative, the type is for unlimited text
	 * size; otherwise it is with limited size (as determined by the size tagged
	 * value, parameter, or default).
	 *
	 * @param pi
	 * @return the data type for unlimited or limited text size, depending upon
	 *         the (local and global) settings of 'size' for the property
	 */
	private String determineCharacterVaryingOrText(PropertyInfo pi) {

		int size = getSizeForProperty(pi);

		if (size < 1) {
			return databaseStrategy.unlimitedLengthCharacterDataType();
		} else {
			return databaseStrategy.limitedLengthCharacterDataType(size);
		}
	}

	/**
	 * Determines the applicable 'size' for the given property. If the tagged
	 * value {@value #PARAM_SIZE} is set for the property, its value is
	 * returned. Otherwise the default value (given via the configuration
	 * parameter {@value #PARAM_SIZE} or as defined by this class [
	 * {@value #DEFAULT_SIZE}]) applies.
	 *
	 * @param pi
	 * @return
	 */
	private int getSizeForProperty(PropertyInfo pi) {

		String tvSize = pi.taggedValuesAll().getFirstValue(PARAM_SIZE);

		int size = defaultSize;

		if (tvSize != null) {
			try {
				size = Integer.parseInt(tvSize);
			} catch (NumberFormatException e) {
				MessageContext mc = result.addWarning(this, 5, PARAM_SIZE,
						e.getMessage(), "" + defaultSize);
				mc.addDetail(this, 0);
				mc.addDetail(this, 100, pi.name(), pi.inClass().name());
				size = defaultSize;
			}
		}

		return size;
	}

	/**
	 * Adds the carriage return and line feed defined for this class (
	 * {@value #CRLF}) to the given buffer.
	 *
	 * @param sb
	 */
	private void newLine(StringBuffer sb) {
		sb.append(CRLF);
	}

	/**
	 * Adds the given string to the given buffer, adding the carriage return and
	 * line feed defined for this class ({@value #CRLF}) at the end of the
	 * string, without preceding indentation.
	 *
	 * @param string
	 */
	private void addCRLF(StringBuffer sb, String string) {
		sb.append(string + CRLF);
	}

	/**
	 * Adds the given string to the given buffer, preceding it with the
	 * indentation value defined for this class ({@value #IDT}), and adding the
	 * carriage return and line feed defined for this class ({@value #CRLF}) at
	 * the end of the string.
	 *
	 * @param string
	 */
	private void indent(StringBuffer sb, String string) {
		sb.append(IDT + string + CRLF);
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Target.Target#write()
	 */
	public void write() {

		if (printed) {
			return;
		}
		if (diagnosticsOnly) {
			return;
		}

		generateGeometryMetadataUpdateStatements();

		// write all indexes
		generateGeometryIndexes();

		// TBD: add further indexes as required

		String fileName = schema.name().replace("/", "_").replace(" ", "_")
				+ ".sql";
		// String fileName = outputFilename + ".sql";

		// Now aggregate the output

		StringBuffer sb = new StringBuffer();

		// add table creation statements
		for (String tableCreationStatement : this.tablesByClassName.values()) {
			sb.append(tableCreationStatement);
		}
		newLine(sb);

		if (createAssociativeTables) {
			// add statements to create associative tables
			for (String tableCreationStatement : this.tablesByAssociativeTableName
					.values()) {
				sb.append(tableCreationStatement);
			}
			newLine(sb);
		}

		for (String alterTableStatement : alterTableStatementsByClassName
				.values()) {
			sb.append(alterTableStatement);
		}
		newLine(sb);

		for (List<String> checkConstrStmts : checkConstraintsForPropsWithEnumValueTypeByTableName
				.values()) {

			for (String stmt : checkConstrStmts) {
				sb.append(stmt);
			}
		}
		newLine(sb);

		// if required, add foreign key statements
		for (List<String> foreignKeys : referenceColumnDefinitionsByTableName
				.values()) {
			for (String foreignKeyStatement : foreignKeys) {
				sb.append(foreignKeyStatement);
			}
		}
		newLine(sb);

		for (String geometryMetadataUpdateStatement : geometryMetadataUpdateStatements) {
			sb.append(geometryMetadataUpdateStatement);
		}
		newLine(sb);

		// add indexes
		for (String geometryIndexCreationStatement : this.geometryIndexCreationStatements) {
			sb.append(geometryIndexCreationStatement);
		}
		newLine(sb);

		// add INSERT statements for code lists, if any
		if (!insertStatementsByCodeListName.isEmpty()) {
			for (List<String> inserts : insertStatementsByCodeListName
					.values()) {
				for (String stmt : inserts) {
					sb.append(stmt);
				}
			}
		}

		BufferedWriter writer = null;
		try {
			File file = new File(outputDirectory, fileName);
			writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(file), "UTF-8"));
			writer.write(sb.toString());
			writer.close();
			result.addResult(getTargetID(), outputDirectory, fileName, null);

			printed = true;

		} catch (Exception e) {

			String m = e.getMessage();
			if (m != null) {
				result.addError(m);
			}

			e.printStackTrace(System.err);
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

	private void generateGeometryMetadataUpdateStatements() {

		List<String> result = new ArrayList<String>();

		for (String classname : geometryPropsByTableName.keySet()) {

			for (PropertyInfo pi : geometryPropsByTableName.get(classname)) {

				String s = databaseStrategy.geometryMetadataUpdateStatement(
						normalizeName(classname), normalizeName(pi.name()),
						srid);
				if (!s.isEmpty()) {
					result.add(s + ";" + CRLF);
				}
			}
		}

		this.geometryMetadataUpdateStatements = result;
	}

	/**
	 * Generates index creation statements for all geometry properties/columns
	 * contained in {@link #geometryPropsByTableName}. The statements are stored
	 * in an internal list ({@link #geometryIndexCreationStatements}).
	 */
	private void generateGeometryIndexes() {

		List<String> result = new ArrayList<String>();

		for (String classname : geometryPropsByTableName.keySet()) {

			for (PropertyInfo pi : geometryPropsByTableName.get(classname)) {

				Map<String, String> geometryCharacteristics = null;

				if (mepp.hasParameter(pi.typeInfo().name, ME_PARAM_GEOMETRY)) {
					geometryCharacteristics = mepp.getCharacteristics(
							pi.typeInfo().name, ME_PARAM_GEOMETRY);
				}

				// TBD: UPDATE NAMING PATTERN?

				String columnName = normalizeName(pi.name());
				String indexName = normalizeName(
						"idx_" + classname + "_" + columnName);
				String tableName = normalizeName(classname);

				String s = databaseStrategy.geometryIndexColumnPart(indexName,
						tableName, columnName, geometryCharacteristics) + ";"
						+ CRLF;

				result.add(s);
			}
		}
		this.geometryIndexCreationStatements = result;
	}

	public int getTargetID() {
		return TargetIdentification.SQLDDL.getId();
	}

	/**
	 * @param name
	 * @return String with any occurrence of '.' or '-' replaced by '_'.
	 */
	private String normalizeName(String name) {

		if (name == null) {
			return null;
		} else {
			return databaseStrategy
					.normalizeName(name.replace(".", "_").replace("-", "_"));
		}
	}

	private String createNameCheckConstraint(String tableName,
			String propertyName) {
		if (tableName == null || propertyName == null) {
			return null;
		}
		return databaseStrategy.createNameCheckConstraint(
				tableName.replace(".", "_").replace("-", "_"),
				propertyName.replace(".", "_").replace("-", "_"));
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.MessageSource#message(int)
	 */
	public String message(int mnr) {

		switch (mnr) {
		case 0:
			return "Context: class SqlDdl";
		case 1:
			return "Generating SQL DDL for application schema '$1$'.";
		case 2:
			return "Processing class '$1$'.";
		case 3:
			return "Directory named '$1$' does not exist or is not accessible.";
		case 4:
			return "Number format exception while converting the value of configuration parameter '$1$' to an integer. Exception message: $2$. Using $3$ as default value for '$1$'.";
		case 5:
			return "Number format exception while converting the tagged value '$1$' to an integer. Exception message: $2$. Using $3$ as default value.";
		case 6:
			return "Unknown database system '$1$'";
		case 7:
			return "Role name '$1$', on a relation from '$2$' to '$3$', seems to be an autogenerated name.";
		case 8:
			return "??Many-to-many relationship represented by association between types with identity and maximum multiplicity > 1 on all navigable ends (in this case for classes: '$1$' [context is property '$2$'] <-> '$3$' [context is property '$4$']) is only supported if creation for associative tables is enabled (via inclusion of rule "
					+ RULE_TGT_SQL_ALL_ASSOCIATIVETABLES
					+ "). Because the rule is not included, the relationship will be ignored.";
		case 9:
			return "Type '$1$' of property '$2$' in class '$3$' is not part of the schema that is being processed, no map entry is defined for it, and "
					+ RULE_TGT_SQL_CLS_REFERENCES_TO_EXTERNAL_TYPES
					+ " is not enabled. Please ensure that map entries are defined for external types used in the schema - or allow referencing of external types in general by enabling "
					+ RULE_TGT_SQL_CLS_REFERENCES_TO_EXTERNAL_TYPES
					+ ". Assigning textual type to the property.";
		case 10:
			return "Type '$1$' of property '$2$' in class '$3$' could not be found in the model. Assigning textual type to the property.";
		case 11:
			return "Attribute '$1$' in class '$2$' has maximum multiplicity greater than one. Creation of associative tables is not enabled. The property will thus be ignored.";
		case 12:
			return "Creating associative table to represent attribute '$1$' in class '$2$'. Tagged value '"
					+ TV_ASSOCIATIVETABLE
					+ "' not set on this attribute, thus using default naming pattern, which leads to table name: '$3$'.";
		case 13:
			return "Creating associative table to represent association between $1$ and $2$. Tagged value '"
					+ TV_ASSOCIATIVETABLE
					+ "' not set on this association, thus using default naming pattern, which leads to table name: '$3$'.";
		case 14:
			return "Derived property '$1$' in class '$2$' has been ignored.";
		case 15:
			return "No map entries provided via the configuration.";
		case 16:
			return "The type of property '$1$' in class '$2$' is '$3$'. It is contained in the schema that is being processed. However, it is of a category not enabled for conversion, meaning that no table will be created to represent the type '$3$'. The property '$1$' in class '$2$' will therefore be ignored.";
		case 17:
			return "Type '$1$' is of a category not enabled for conversion, meaning that no table will be created to represent it.";
		case 18:
			return "Could not find enumeration '$1$' in the model - or no enum values defined for it. Check constraint for '$2$' will not be created.";
		case 19:
			return "";
		case 20:
			return "??More than eleven occurrences of foreign key '$1$'. Resulting schema will be ambiguous.";
		case 21:
			return "?? The type '$1$' was not found in the schema(s) selected for processing or in map entries. It will be mapped to 'unknown'.";
		case 22:
			return "Type '$1$' has been mapped to '$2$', as defined by the configuration.";
		case 23:
			return "At least one of the descriptor identifiers in configuration parameter '"
					+ PARAM_DESCRIPTORS_FOR_CODELIST
					+ "' (parameter value is '$1$') does not match the regular expression '$2$'. Identifiers that do not match this expression will be ignored.";
		case 24:
			return "Configuration parameter '" + PARAM_DESCRIPTORS_FOR_CODELIST
					+ "' did not contain a well-known identifier. Using default value 'documentation'.";
		case 100:
			return "Context: property '$1$' in class '$2$'.";
		default:
			return "(Unknown message)";
		}
	}

}
