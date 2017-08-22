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
package de.interactive_instruments.ShapeChange.Target.SQL;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.SerializerFactory;

import de.interactive_instruments.ShapeChange.MapEntryParamInfos;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Multiplicity;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Target.SingleTarget;
import de.interactive_instruments.ShapeChange.Target.SQL.expressions.SdoDimArrayExpression;
import de.interactive_instruments.ShapeChange.Target.SQL.expressions.SdoDimElement;
import de.interactive_instruments.ShapeChange.Target.SQL.naming.CheckConstraintNamingStrategy;
import de.interactive_instruments.ShapeChange.Target.SQL.naming.CountSuffixUniqueNamingStrategy;
import de.interactive_instruments.ShapeChange.Target.SQL.naming.DefaultForeignKeyNamingStrategy;
import de.interactive_instruments.ShapeChange.Target.SQL.naming.DefaultNamingScheme;
import de.interactive_instruments.ShapeChange.Target.SQL.naming.DefaultOracleCheckConstraintNamingStrategy;
import de.interactive_instruments.ShapeChange.Target.SQL.naming.DefaultPostgreSQLCheckConstraintNamingStrategy;
import de.interactive_instruments.ShapeChange.Target.SQL.naming.DefaultSQLServerCheckConstraintNamingStrategy;
import de.interactive_instruments.ShapeChange.Target.SQL.naming.ForeignKeyNamingStrategy;
import de.interactive_instruments.ShapeChange.Target.SQL.naming.LowerCaseNameNormalizer;
import de.interactive_instruments.ShapeChange.Target.SQL.naming.NameNormalizer;
import de.interactive_instruments.ShapeChange.Target.SQL.naming.OracleNameNormalizer;
import de.interactive_instruments.ShapeChange.Target.SQL.naming.OracleStyleForeignKeyNamingStrategy;
import de.interactive_instruments.ShapeChange.Target.SQL.naming.PearsonHashCheckConstraintNamingStrategy;
import de.interactive_instruments.ShapeChange.Target.SQL.naming.PearsonHashForeignKeyNamingStrategy;
import de.interactive_instruments.ShapeChange.Target.SQL.naming.SQLServerNameNormalizer;
import de.interactive_instruments.ShapeChange.Target.SQL.naming.SqlNamingScheme;
import de.interactive_instruments.ShapeChange.Target.SQL.naming.UniqueNamingStrategy;
import de.interactive_instruments.ShapeChange.Target.SQL.naming.UpperCaseNameNormalizer;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Statement;

/**
 * Creates SQL DDL for an application schema.
 *
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class SqlDdl implements SingleTarget, MessageSource {

	public static final String PLATFORM = "sql";
	
	protected static Model model = null;

	private static String[] descriptorsForCodelistFromConfig = new String[] {
			"documentation" };
	protected static List<DescriptorForCodeList> descriptorsForCodelist = new ArrayList<DescriptorForCodeList>();

	protected static String codeNameColumnName = "name";
	protected static int codeNameSize = 0;

	private static boolean initialised = false;
	protected static boolean diagnosticsOnly = false;
	protected static boolean atLeastOneSchemaIsEncoded = false;
	/**
	 * NOTE: If not set via the configuration, the default applies which is
	 * {@value Options#DERIVED_DOCUMENTATION_DEFAULT_TEMPLATE}.
	 */
	protected static String documentationTemplate = null;
	/**
	 * NOTE: If not set via the configuration, the default applies which is
	 * {@value Options#DERIVED_DOCUMENTATION_DEFAULT_NOVALUE}.
	 */
	protected static String documentationNoValue = null;

	private static String outputDirectory = null;
	private static String outputFilename = null;

	protected static String codeStatusCLType;
	protected static String idColumnName;
	protected static String oneToManyReferenceColumnName;
	protected static String foreignKeyColumnSuffix;
	protected static String foreignKeyColumnSuffixDatatype;
	protected static String foreignKeyColumnDataType;
	protected static String primaryKeySpec;
	protected static String primaryKeySpecCodelist;
	protected static String nameCodeStatusCLColumn;
	protected static String nameCodeStatusNotesColumn;
	protected static int defaultSize;
	protected static int srid;
	protected static boolean createReferences = false;
	protected static boolean createDocumentation = true;
	protected static boolean createAssociativeTables = false;
	protected static boolean removeEmptyLinesInDdlOutput = false;

	/**
	 * Contains information parsed from the 'param' attributes of each map entry
	 * defined for this target.
	 */
	protected static MapEntryParamInfos mapEntryParamInfos = null;

	protected static DatabaseStrategy databaseStrategy;

	protected static SqlNamingScheme namingScheme;

	protected static List<ClassInfo> cisToProcess = new ArrayList<ClassInfo>();

	protected static SdoDimArrayExpression sdoDimArrayExpression = new SdoDimArrayExpression();

	/* ------- */
	/* Replication schema specific fields */
	protected static boolean createRepSchema = false;
	protected static Map<String, ProcessMapEntry> repSchemaMapEntryByType = new HashMap<String, ProcessMapEntry>();

	protected static String repSchemaDocumentationUnlimitedLengthCharacterDataType = null;
	protected static String repSchemaTargetNamespace = null;
	protected static String repSchemaTargetNamespaceSuffix = null;
	protected static String repSchemaTargetVersion = null;
	protected static String repSchemaTargetXmlns = null;
	protected static String repSchemaObjectIdentifierFieldType;
	protected static String repSchemaForeignKeyFieldType;
	protected static Multiplicity repSchemaMultiplicity1 = new Multiplicity(1,
			1);
	
	/* ------ */
	/*
	 * Non-static fields
	 */
	protected ShapeChangeResult result = null;
	protected Options options = null;
	
	private PackageInfo schema = null;
	private boolean schemaNotEncoded = false;
	
	@Override
	public void initialise(PackageInfo pi, Model m, Options o,
			ShapeChangeResult r, boolean diagOnly)
			throws ShapeChangeAbortException {

		schema = pi;
		model = m;
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

		if (!initialised) {
			initialised = true;

			outputDirectory = options.parameter(this.getClass().getName(),
					"outputDirectory");
			if (outputDirectory == null)
				outputDirectory = options.parameter("outputDirectory");
			if (outputDirectory == null)
				outputDirectory = options.parameter(".");

			// create output directory, if necessary
			if (!diagnosticsOnly) {

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

				outputFilename = options.parameter(this.getClass().getName(),
						"outputFilename");
				if (outputFilename == null) {
					outputFilename = schema.name();
				}
				outputFilename = outputFilename.replace("/", "_")
						.replace(" ", "_");
				
				String repSchemaOutputFilename = outputFilename + ".xsd";

				File repSchemaOutputFile = new File(outputDirectoryFile,
						repSchemaOutputFilename);

				/*
				 * check if output file already exists - if so, attempt to
				 * delete it
				 */
				exi = repSchemaOutputFile.exists();
				if (exi) {

					result.addInfo(this, 503, repSchemaOutputFilename,
							outputDirectory);

					try {
						FileUtils.forceDelete(repSchemaOutputFile);
						result.addInfo(this, 504);
					} catch (IOException e) {
						result.addInfo(null, 600, e.getMessage());
						e.printStackTrace(System.err);
					}
				}
			}

			if (pi.matches(SqlConstants.RULE_TGT_SQL_ALL_ASSOCIATIVETABLES)) {
				createAssociativeTables = true;
			}

			String databaseSystem = options.parameter(this.getClass().getName(),
					SqlConstants.PARAM_DATABASE_SYSTEM);

			NameNormalizer normalizer = null;
			ForeignKeyNamingStrategy fkNaming = null;
			CheckConstraintNamingStrategy ckNaming = null;
			UniqueNamingStrategy uniqueConstraintNaming = new CountSuffixUniqueNamingStrategy(
					result);

			// identify normalizer strategy
			if (pi.matches(
					SqlConstants.RULE_TGT_SQL_ALL_NORMALIZING_LOWER_CASE)) {
				normalizer = new LowerCaseNameNormalizer();
			} else if (pi.matches(
					SqlConstants.RULE_TGT_SQL_ALL_NORMALIZING_UPPER_CASE)) {
				normalizer = new UpperCaseNameNormalizer();
			} else if (pi.matches(
					SqlConstants.RULE_TGT_SQL_ALL_NORMALIZING_ORACLE)) {
				normalizer = new OracleNameNormalizer(result);
			} else if (pi.matches(
					SqlConstants.RULE_TGT_SQL_ALL_NORMALIZING_SQLSERVER)) {
				normalizer = new SQLServerNameNormalizer(result);
			}

			// identify foreign key naming strategy
			if (pi.matches(
					SqlConstants.RULE_TGT_SQL_ALL_FOREIGNKEY_PEARSONHASH_NAMING)) {
				fkNaming = new PearsonHashForeignKeyNamingStrategy();
			} else if (pi.matches(
					SqlConstants.RULE_TGT_SQL_ALL_FOREIGNKEY_ORACLE_NAMING_STYLE)) {
				fkNaming = new OracleStyleForeignKeyNamingStrategy(result);
			} else {
				fkNaming = new DefaultForeignKeyNamingStrategy();
			}

			// identify check constraint naming strategy
			if (pi.matches(
					SqlConstants.RULE_TGT_SQL_ALL_CHECK_CONSTRAINT_NAMING_ORACLE_DEFAULT)) {
				ckNaming = new DefaultOracleCheckConstraintNamingStrategy();
			} else if (pi.matches(
					SqlConstants.RULE_TGT_SQL_ALL_CHECK_CONSTRAINT_NAMING_PEARSONHASH)) {
				ckNaming = new PearsonHashCheckConstraintNamingStrategy();
			} else if (pi.matches(
					SqlConstants.RULE_TGT_SQL_ALL_CHECK_CONSTRAINT_NAMING_POSTGRESQL_DEFAULT)) {
				ckNaming = new DefaultPostgreSQLCheckConstraintNamingStrategy();
			} else if (pi.matches(
					SqlConstants.RULE_TGT_SQL_ALL_CHECK_CONSTRAINT_NAMING_SQLSERVER_DEFAULT)) {
				ckNaming = new DefaultSQLServerCheckConstraintNamingStrategy();
			}

			if (databaseSystem != null
					&& "oracle".equalsIgnoreCase(databaseSystem)) {

				databaseStrategy = new OracleStrategy(result);
				if (normalizer == null) {
					normalizer = new OracleNameNormalizer(result);
				}
				if (ckNaming == null) {
					ckNaming = new DefaultOracleCheckConstraintNamingStrategy();
				}

			} else if (databaseSystem != null
					&& "sqlserver".equalsIgnoreCase(databaseSystem)) {

				databaseStrategy = new SQLServerStrategy(result);
				if (normalizer == null) {
					normalizer = new SQLServerNameNormalizer(result);
				}
				if (ckNaming == null) {
					ckNaming = new DefaultSQLServerCheckConstraintNamingStrategy();
				}

			} else {

				if (databaseSystem != null
						&& !"postgresql".equalsIgnoreCase(databaseSystem)) {
					result.addError(this, 6, databaseSystem);
				}
				databaseStrategy = new PostgreSQLStrategy();
				if (normalizer == null) {
					normalizer = new LowerCaseNameNormalizer();
				}
				if (ckNaming == null) {
					ckNaming = new DefaultPostgreSQLCheckConstraintNamingStrategy();
				}
			}

			if (schema.matches(
					SqlConstants.RULE_TGT_SQL_ALL_NORMALIZING_IGNORE_CASE)) {
				normalizer.setIgnoreCaseWhenNormalizing(true);
			}

			namingScheme = new DefaultNamingScheme(result, normalizer,
					fkNaming, ckNaming, uniqueConstraintNaming);

			codeStatusCLType = options.parameterAsString(this.getClass().getName(),
					SqlConstants.PARAM_CODESTATUSCL_TYPE,
					SqlConstants.DEFAULT_CODESTATUSCL_TYPE, false, true);

			idColumnName = options.parameterAsString(this.getClass().getName(),
					SqlConstants.PARAM_ID_COLUMN_NAME,
					SqlConstants.DEFAULT_ID_COLUMN_NAME, false, true);

			oneToManyReferenceColumnName = options.parameterAsString(
					this.getClass().getName(),
					SqlConstants.PARAM_ONE_TO_MANY_REF_COLUMN_NAME,
					SqlConstants.DEFAULT_ONE_TO_MANY_REF_COLUMN_NAME, false,
					true);

			foreignKeyColumnSuffix = options.parameterAsString(
					this.getClass().getName(),
					SqlConstants.PARAM_FOREIGN_KEY_COLUMN_SUFFIX,
					SqlConstants.DEFAULT_FOREIGN_KEY_COLUMN_SUFFIX, true,
					false);

			foreignKeyColumnSuffixDatatype = options.parameterAsString(
					this.getClass().getName(),
					SqlConstants.PARAM_FOREIGN_KEY_COLUMN_SUFFIX_DATATYPE,
					SqlConstants.DEFAULT_FOREIGN_KEY_COLUMN_SUFFIX_DATATYPE,
					true, false);

			foreignKeyColumnDataType = options.parameterAsString(
					this.getClass().getName(),
					SqlConstants.PARAM_FOREIGN_KEY_COLUMN_DATA_TYPE,
					databaseStrategy.primaryKeyDataType(), false, true);

			primaryKeySpec = options.parameterAsString(
					this.getClass().getName(),
					SqlConstants.PARAM_PRIMARYKEY_SPEC,
					SqlConstants.DEFAULT_PRIMARYKEY_SPEC, true, true);

			primaryKeySpecCodelist = options.parameterAsString(
					this.getClass().getName(),
					SqlConstants.PARAM_PRIMARYKEY_SPEC_CODELIST,
					SqlConstants.DEFAULT_PRIMARYKEY_SPEC_CODELIST, true, true);

			nameCodeStatusCLColumn = options.parameterAsString(
					this.getClass().getName(),
					SqlConstants.PARAM_NAME_CODESTATUS_CL_COLUMN,
					SqlConstants.DEFAULT_NAME_CODESTATUS_CL_COLUMN, false, true);
			
			nameCodeStatusNotesColumn = options.parameterAsString(
					this.getClass().getName(),
					SqlConstants.PARAM_NAME_CODESTATUSNOTES_COLUMN,
					SqlConstants.DEFAULT_NAME_CODESTATUSNOTES_COLUMN, false, true);

			String sdoDimElement_value = options.parameterAsString(
					this.getClass().getName(),
					SqlConstants.PARAM_SDO_DIM_ELEMENTS, null, false, true);
			parseSdoDimElementValue(sdoDimElement_value);

			defaultSize = options.parameterAsInteger(this.getClass().getName(),
					SqlConstants.PARAM_SIZE, SqlConstants.DEFAULT_SIZE);

			srid = options.parameterAsInteger(this.getClass().getName(),
					SqlConstants.PARAM_SRID, SqlConstants.DEFAULT_SRID);

			createReferences = options.parameterAsBoolean(
					this.getClass().getName(),
					SqlConstants.PARAM_CREATE_REFERENCES,
					SqlConstants.DEFAULT_CREATE_REFERNCES);

			createDocumentation = options.parameterAsBoolean(
					this.getClass().getName(),
					SqlConstants.PARAM_CREATE_DOCUMENTATION,
					SqlConstants.DEFAULT_CREATE_DOCUMENTATION);

			removeEmptyLinesInDdlOutput = options.parameterAsBoolean(
					this.getClass().getName(),
					SqlConstants.PARAM_REMOVE_EMPTY_LINES_IN_DDL_OUTPUT, false);

			/*
			 * override parameter 'createDocumentation' if configured via
			 * conversion rule
			 */
			if (pi.matches(
					SqlConstants.RULE_TGT_SQL_ALL_SUPPRESS_INLINE_DOCUMENTATION)) {
				createDocumentation = false;
			}

			// change the default documentation template?
			documentationTemplate = options.parameter(this.getClass().getName(),
					SqlConstants.PARAM_DOCUMENTATION_TEMPLATE);
			documentationNoValue = options.parameter(this.getClass().getName(),
					SqlConstants.PARAM_DOCUMENTATION_NOVALUE);

			String descriptorsForCodelistByConfig = options.parameter(
					this.getClass().getName(),
					SqlConstants.PARAM_DESCRIPTORS_FOR_CODELIST);
			if (descriptorsForCodelistByConfig != null
					&& !descriptorsForCodelistByConfig.trim().isEmpty()) {
				descriptorsForCodelistFromConfig = descriptorsForCodelistByConfig
						.trim().split(",");
			}
			boolean unknownDescriptorFound = false;

			for (String tmp : descriptorsForCodelistFromConfig) {

				if (tmp.matches(SqlConstants.DESCRIPTORS_FOR_CODELIST_REGEX)) {

					// parse descriptor string
					String name = null;
					String columnName = null;
					Integer size = null;

					if (tmp.contains("(")) {

						name = tmp.substring(0, tmp.indexOf("("));
						String tmp2 = tmp.substring(tmp.indexOf("(") + 1,
								tmp.length() - 1);
						String[] metadata = tmp2.split(";");
						for (String meta : metadata) {
							String[] meta_parts = meta.split("=");
							if (meta_parts[0].equalsIgnoreCase("columnName")) {
								columnName = meta_parts[1];
							} else if (meta_parts[0].equalsIgnoreCase("size")) {
								size = new Integer(meta_parts[1]);
							}
						}

					} else {
						// no metadata defined for descriptor
						name = tmp;
					}

					if (columnName == null) {
						columnName = name;
					}

					descriptorsForCodelist.add(
							new DescriptorForCodeList(name, columnName, size));

				} else {
					unknownDescriptorFound = true;
				}
			}
			if (unknownDescriptorFound) {
				result.addWarning(this, 23, descriptorsForCodelistByConfig,
						SqlConstants.DESCRIPTORS_FOR_CODELIST_REGEX);
			}
			if (descriptorsForCodelist.isEmpty()) {
				result.addWarning(this, 24);
				descriptorsForCodelist.add(
						new DescriptorForCodeList("documentation", null, null));
			}

			/*
			 * set of parameters for naming of columns when converting code list
			 * to table
			 */
			codeNameColumnName = options.parameterAsString(
					this.getClass().getName(),
					SqlConstants.PARAM_CODE_NAME_COLUMN_NAME,
					SqlConstants.DEFAULT_CODE_NAME_COLUMN_NAME, false, true);

			codeNameSize = options.parameterAsInteger(this.getClass().getName(),
					SqlConstants.PARAM_CODE_NAME_SIZE,
					SqlConstants.DEFAULT_CODE_NAME_SIZE);

			// identify map entries defined in the target configuration
			List<ProcessMapEntry> mapEntries = options.getCurrentProcessConfig()
					.getMapEntries();

			if (mapEntries == null || mapEntries.isEmpty()) {

				/*
				 * It is unlikely but not impossible that an application schema
				 * does not make use of types that require a type mapping in
				 * order to be converted into a database schema.
				 */
				result.addWarning(this, 15);

			} else {

				/*
				 * Parse all parameter information
				 */
				mapEntryParamInfos = new MapEntryParamInfos(result, mapEntries);
			}

			// ======================================
			// Replication schema configuration
			// ======================================

			if (schema.matches(
					ReplicationSchemaConstants.RULE_TGT_SQL_ALL_REPSCHEMA)) {
				
				createRepSchema = true;

				repSchemaObjectIdentifierFieldType = options.parameterAsString(
						this.getClass().getName(),
						ReplicationSchemaConstants.PARAM_OBJECT_IDENTIFIER_FIELD_TYPE,
						ReplicationSchemaConstants.DEFAULT_OBJECT_IDENTIFIER_FIELD_TYPE,
						false, true);

				repSchemaForeignKeyFieldType = options.parameterAsString(
						this.getClass().getName(),
						ReplicationSchemaConstants.PARAM_FOREIGN_KEY_FIELD_TYPE,
						repSchemaObjectIdentifierFieldType, false, true);

				repSchemaTargetNamespaceSuffix = options.parameterAsString(
						this.getClass().getName(),
						ReplicationSchemaConstants.PARAM_TARGET_NAMESPACE_SUFFIX,
						ReplicationSchemaConstants.DEFAULT_TARGET_NAMESPACE_SUFFIX,
						false, true);
				
				String mainSchemaName = options.parameterAsString(this.getClass().getName(),
						"replicationSchemaMainAppSchema", "", false, true);
				if (StringUtils.isNotBlank(mainSchemaName)) {
					for (PackageInfo packageInfo : model.selectedSchemas()) {
						if (packageInfo.name().equals(mainSchemaName)) {
							repSchemaTargetNamespace = packageInfo.targetNamespace();
							repSchemaTargetVersion = packageInfo.version();
							repSchemaTargetXmlns = packageInfo.xmlns();
						}
					}
				} else {
					repSchemaTargetNamespace = options.parameterAsString(this.getClass().getName(),
							ReplicationSchemaConstants.PARAM_TARGET_NAMESPACE, schema.targetNamespace(), true, true);
					repSchemaTargetVersion = options.parameterAsString(this.getClass().getName(),
							ReplicationSchemaConstants.PARAM_TARGET_VERSION, schema.version(), true, true);
					repSchemaTargetXmlns = options.parameterAsString(this.getClass().getName(),
							ReplicationSchemaConstants.PARAM_TARGET_XMLNS, schema.xmlns(), true, true);
				}
				// make sure parameters are never null
				repSchemaTargetNamespace = StringUtils.defaultString(repSchemaTargetNamespace);
				repSchemaTargetVersion = StringUtils.defaultString(repSchemaTargetVersion);
				repSchemaTargetXmlns = StringUtils.defaultString(repSchemaTargetXmlns);
				
				repSchemaDocumentationUnlimitedLengthCharacterDataType = options
						.parameterAsString(this.getClass().getName(),
								ReplicationSchemaConstants.PARAM_DOCUMENTATION_UNLIMITEDLENGTHCHARACTERDATATYPE,
								ReplicationSchemaConstants.DEFAULT_DOCUMENTATION_UNLIMITEDLENGTHCHARACTERDATATYPE,
								false, true);
			}
		}
	}

	private void parseSdoDimElementValue(String sdoDimElement_value) {

		if (sdoDimElement_value != null) {

			Pattern p = Pattern.compile(SqlConstants.PATTERN_SDO_DIM_ELEMENTS);
			Matcher m = p.matcher(sdoDimElement_value.trim());

			if (!m.matches()) {
				result.addError(this, 16, sdoDimElement_value,
						SqlConstants.PATTERN_SDO_DIM_ELEMENTS);
			} else {

				String[] elements = sdoDimElement_value.trim().split("\\)");

				for (String element : elements) {

					String s = element.substring(1);
					String[] parts = s.split(",");

					SdoDimElement sde = new SdoDimElement();
					sde.setDimName(parts[0]);
					sde.setLowerBound(parts[1]);
					sde.setUpperBound(parts[2]);
					sde.setTolerance(parts[3]);

					sdoDimArrayExpression.addElement(sde);
				}
			}
		}
	}

	public void process(ClassInfo ci) {

		if (ci == null || ci.pkg() == null) {
			return;
		}

		if (!isEncoded(ci)) {
			result.addInfo(this, 8, ci.name());
			return;
		}

		result.addDebug(this, 2, ci.name());
		
		ProcessMapEntry pme = options.targetMapEntry(ci.name(),
				ci.encodingRule("sql"));

		if (pme != null) {
			result.addInfo(this, 22, ci.name(), pme.getTargetType());
			return;
		}
		
		if(schemaNotEncoded) {
			result.addInfo(this,18,schema.name(),ci.name());
			return;
		}

		if (ci.isAbstract()
				&& ci.matches(SqlConstants.RULE_TGT_SQL_ALL_EXCLUDE_ABSTRACT)) {
			return;
		}

		// Create table creation statements
		if ((ci.category() == Options.OBJECT
				&& ci.matches(SqlConstants.RULE_TGT_SQL_CLS_OBJECT_TYPES))
				|| (ci.category() == Options.FEATURE && ci
						.matches(SqlConstants.RULE_TGT_SQL_CLS_FEATURE_TYPES))
				|| (ci.category() == Options.DATATYPE && ci
						.matches(SqlConstants.RULE_TGT_SQL_CLS_DATATYPES))) {

			cisToProcess.add(ci);

		} else if (ci.category() == Options.ENUMERATION) {

			// fine - tables won't be created for enumerations

		} else if (ci.category() == Options.CODELIST) {

			if (ci.matches(SqlConstants.RULE_TGT_SQL_CLS_CODELISTS)) {

				cisToProcess.add(ci);

			} else {
				// do not create tables for code lists
			}
		} else {

			result.addInfo(this, 17, ci.name());
		}
	}

	@Override
	public void write() {
		
		// nothing to do here (this is a SingleTarget)
	}

	private String readDdl(String paramFileDdl) {

		String res = null;

		String fileDdl = options.parameterAsString(this.getClass().getName(),
				paramFileDdl, null, false, true);

		if (fileDdl != null) {
			File ddlTop = new File(fileDdl);
			if (!ddlTop.exists() || ddlTop.isDirectory() || !ddlTop.canRead()) {

				result.addError(this, 25, SqlConstants.PARAM_FILE_DDL_TOP,
						fileDdl);
			} else {
				try {
					res = FileUtils.readFileToString(ddlTop,
							StandardCharsets.UTF_8);
				} catch (IOException e) {
					result.addError(this, 26, fileDdl, e.getMessage());
				}
			}
		}

		return res;
	}

	@Override
	public String getTargetName() {
		return "SQL DDL";
	}

	public static boolean isEncoded(Info i) {

		if (i.matches(SqlConstants.RULE_TGT_SQL_ALL_NOTENCODED)
				&& i.encodingRule("sql").equalsIgnoreCase("notencoded")) {

			return false;
		} else {
			return true;
		}
	}

	@Override
	public void writeAll(ShapeChangeResult r) {
		
		this.result = r;
		this.options = r.options();

		if (diagnosticsOnly || !atLeastOneSchemaIsEncoded) {
			return;
		}

		// Build SQL statements
		SqlBuilder builder = new SqlBuilder(this, result, options, model,
				namingScheme);
		List<Statement> stmts = builder.process(cisToProcess);

		/*
		 * Create representation (DDL or replication schema) and write the
		 * results
		 */
		try {
						
			if (createRepSchema) {

				// Create replication schema
				ReplicationSchemaVisitor visitor = new ReplicationSchemaVisitor(
						this, builder);
				visitor.visit(stmts);

				Properties outputFormat = OutputPropertiesFactory
						.getDefaultMethodProperties("xml");
				outputFormat.setProperty("indent", "yes");
				outputFormat.setProperty(
						"{http://xml.apache.org/xalan}indent-amount", "2");
				outputFormat.setProperty("encoding", "UTF-8");

				String fileName = outputFilename + ".xsd";

				/*
				 * Uses OutputStreamWriter instead of FileWriter to set
				 * character encoding (see doc in Serializer.setWriter and
				 * FileWriter)
				 */

				File repXsd = new File(outputDirectory, fileName);

				try (BufferedWriter writer = new BufferedWriter(
						new OutputStreamWriter(new FileOutputStream(repXsd),
								"UTF-8"))) {

					Serializer serializer = SerializerFactory
							.getSerializer(outputFormat);
					serializer.setWriter(writer);
					serializer.asDOMSerializer()
							.serialize(visitor.getDocument());

					result.addResult(getTargetName(), outputDirectory, fileName,
							repSchemaTargetNamespace);
				}

			} else {

				// --- Create DDL
				StringBuffer sb = new StringBuffer();
				DdlVisitor visitor = new DdlVisitor(SqlConstants.CRLF,
						SqlConstants.INDENT, this);
				visitor.visit(stmts);
				sb.append(visitor.getDdl());

				// --- Write DDL to file
				String fileName = outputFilename + ".sql";

				File outputFile = new File(outputDirectory, fileName);

				try (BufferedWriter writer = new BufferedWriter(
						new OutputStreamWriter(new FileOutputStream(outputFile),
								"UTF-8"))) {

					String ddlTop = readDdl(SqlConstants.PARAM_FILE_DDL_TOP);
					if (ddlTop != null) {
						writer.write(ddlTop);
					}

					writer.write(sb.toString());

					String ddlBottom = readDdl(
							SqlConstants.PARAM_FILE_DDL_BOTTOM);
					if (ddlBottom != null) {
						writer.write(ddlBottom);
					}
				}

				if (removeEmptyLinesInDdlOutput) {

					File outputWithoutEmptyLines = new File(outputDirectory,
							fileName + ".tmp");

					try (BufferedReader originalDdlReader = new BufferedReader(
							new InputStreamReader(
									new FileInputStream(outputFile)));
							BufferedWriter ddlWithoutEmptyLinesWriter = new BufferedWriter(
									new OutputStreamWriter(
											new FileOutputStream(
													outputWithoutEmptyLines),
											"UTF-8"));) {

						String aLine = null;
						while ((aLine = originalDdlReader.readLine()) != null) {
							if (!aLine.trim().isEmpty()) {
								ddlWithoutEmptyLinesWriter.write(aLine);
								ddlWithoutEmptyLinesWriter.newLine();
							}
						}

					}

					Files.move(outputWithoutEmptyLines.toPath(),
							outputFile.toPath(),
							StandardCopyOption.REPLACE_EXISTING);
				}

				result.addResult(getTargetName(), outputDirectory, fileName,
						null);
			}

		} catch (Exception e) {

			String m = e.getMessage();
			if (m != null) {
				result.addError(m);
			}

			e.printStackTrace(System.err);
		}
	}

	@Override
	public void reset() {
		
		model = null;
		
		descriptorsForCodelistFromConfig = new String[] { "documentation" };
		descriptorsForCodelist = new ArrayList<DescriptorForCodeList>();

		codeNameColumnName = "name";
		codeNameSize = 0;

		initialised = false;
		diagnosticsOnly = false;
		atLeastOneSchemaIsEncoded = false;
		
		documentationTemplate = null;
		documentationNoValue = null;

		outputDirectory = null;
		outputFilename = null;

		codeStatusCLType = null;
		idColumnName = null;
		oneToManyReferenceColumnName = null;
		foreignKeyColumnSuffix = null;
		foreignKeyColumnSuffixDatatype = null;
		foreignKeyColumnDataType = null;
		primaryKeySpec = null;
		primaryKeySpecCodelist = null;
		nameCodeStatusCLColumn = null;
		nameCodeStatusNotesColumn = null;
		defaultSize = 0;
		srid = 0;
		createReferences = false;
		createDocumentation = true;
		createAssociativeTables = false;
		removeEmptyLinesInDdlOutput = false;

		mapEntryParamInfos = null;
		databaseStrategy = null;
		namingScheme = null;
		cisToProcess = new ArrayList<ClassInfo>();
		sdoDimArrayExpression = new SdoDimArrayExpression();

		createRepSchema = false;
		repSchemaMapEntryByType = new HashMap<String, ProcessMapEntry>();
		repSchemaDocumentationUnlimitedLengthCharacterDataType = null;
		repSchemaTargetNamespace = null;
		repSchemaTargetNamespaceSuffix = null;
		repSchemaTargetVersion = null;
		repSchemaTargetXmlns = null;
		repSchemaObjectIdentifierFieldType = null;
		repSchemaForeignKeyFieldType = null;
		repSchemaMultiplicity1 = new Multiplicity(1, 1);
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.MessageSource#message(int)
	 */
	public String message(int mnr) {

		switch (mnr) {
		case 0:
			return "Context: class SqlDdl";
//		case 1:
//			return "";
		case 2:
			return "Processing class '$1$'.";
		case 3:
			return "Directory named '$1$' does not exist or is not accessible.";
		// case 4:
		// return "Number format exception while converting the value of
		// configuration parameter '$1$' to an integer. Exception message: $2$.
		// Using $3$ as default value for '$1$'.";
		case 5:
			return "Number format exception while converting the tagged value '$1$' to an integer. Exception message: $2$. Using $3$ as default value.";
		case 6:
			return "Unknown database system '$1$'";
		case 7:
			return "Schema '$1$' is not encoded.";
		case 8:
			return "Class '$1$' is not encoded.";
		case 15:
			return "No map entries provided via the configuration.";
		case 16:
			return "Value '$1$' of configuration parameter $2$ does not match the regular expression: $3$. The parameter will be ignored.";
		case 17:
			return "Type '$1$' is of a category not enabled for conversion, meaning that no table will be created to represent it.";
		case 18:
			return "Schema '$1$' is not encoded. Thus class '$2$' (which belongs to that schema) is not encoded either.";
			
		case 22:
			return "Type '$1$' has been mapped to '$2$', as defined by the configuration.";
		case 23:
			return "At least one of the descriptor identifiers in configuration parameter '"
					+ SqlConstants.PARAM_DESCRIPTORS_FOR_CODELIST
					+ "' (parameter value is '$1$') does not match the regular expression '$2$'. Identifiers that do not match this expression will be ignored.";
		case 24:
			return "Configuration parameter '"
					+ SqlConstants.PARAM_DESCRIPTORS_FOR_CODELIST
					+ "' did not contain a well-known identifier. Using default value 'documentation'.";
		case 25:
			return "Value of configuration parameter '$1$' is '$2$'. The file does not exist, is a directory, or cannot be read.";
		case 26:
			return "Exception occurred while transferring contents of file '$1$': $2$";

		case 503:
			return "Output file '$1$' already exists in output directory ('$2$'). It will be deleted prior to processing.";
		case 504:
			return "File has been deleted.";

		default:
			return "(" + SqlDdl.class.getName()
					+ ") Unknown message with number: " + mnr;
		}
	}

}
