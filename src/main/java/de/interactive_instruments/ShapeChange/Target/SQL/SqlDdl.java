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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
import de.interactive_instruments.ShapeChange.TargetIdentification;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Target.Target;
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
public class SqlDdl implements Target, MessageSource {

	public static final String PLATFORM = "sql";

	private String[] descriptorsForCodelistFromConfig = new String[] {
			"documentation" };
	private List<DescriptorForCodeList> descriptorsForCodelist = new ArrayList<DescriptorForCodeList>();

	private String codeNameColumnName = "name";
	private int codeNameSize = 0;

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
	protected String oneToManyReferenceColumnName;
	private String foreignKeyColumnSuffix;
	private String foreignKeyColumnSuffixDatatype;
	private String foreignKeyColumnDataType;
	private String primaryKeyColumnSpec;
	protected String nameActiveIndicatorLFColumn;
	protected String nameSourceGCLColumn;
	private int defaultSize;
	private int srid;
	private boolean createReferences = false;
	private boolean createDocumentation = true;
	private boolean createAssociativeTables = false;

	/**
	 * Contains information parsed from the 'param' attributes of each map entry
	 * defined for this target.
	 */
	private MapEntryParamInfos mepp = null;

	private DatabaseStrategy databaseStrategy;

	private SqlNamingScheme namingScheme;

	private List<ClassInfo> cisToProcess = new ArrayList<ClassInfo>();

	private SdoDimArrayExpression sdae = new SdoDimArrayExpression();

	/* ------- */
	/* Replication schema specific fields */
	protected String repSchemaOutputFilename;
	protected Map<String, ProcessMapEntry> repSchemaMapEntryByType = new HashMap<String, ProcessMapEntry>();

	protected String repSchemaDocumentationUnlimitedLengthCharacterDataType = null;
	protected String repSchemaTargetNamespace = null;
	protected String repSchemaObjectIdentifierFieldType;
	protected String repSchemaForeignKeyFieldType;
	protected Multiplicity repSchemaMultiplicity1 = new Multiplicity(1, 1);
	protected String repSchemaTargetNamespaceSuffix;

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

			result.addInfo(this, 7, schema.name());
			return;
		}

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

			repSchemaOutputFilename = schema.name().replace("/", "_")
					.replace(" ", "_") + ".xsd";

			File repSchemaOutputFile = new File(outputDirectoryFile,
					repSchemaOutputFilename);

			// check if output file already exists - if so, attempt to delete it
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
			this.createAssociativeTables = true;
		}

		String databaseSystem = options.parameter(this.getClass().getName(),
				SqlConstants.PARAM_DATABASE_SYSTEM);

		NameNormalizer normalizer = null;
		ForeignKeyNamingStrategy fkNaming = null;
		CheckConstraintNamingStrategy ckNaming = null;
		UniqueNamingStrategy uniqueConstraintNaming = new CountSuffixUniqueNamingStrategy(
				result);

		// identify normalizer strategy
		if (pi.matches(SqlConstants.RULE_TGT_SQL_ALL_NORMALIZING_LOWER_CASE)) {
			normalizer = new LowerCaseNameNormalizer();
		} else if (pi.matches(
				SqlConstants.RULE_TGT_SQL_ALL_NORMALIZING_UPPER_CASE)) {
			normalizer = new UpperCaseNameNormalizer();
		} else if (pi
				.matches(SqlConstants.RULE_TGT_SQL_ALL_NORMALIZING_ORACLE)) {
			normalizer = new OracleNameNormalizer(result);
		} else if (pi
				.matches(SqlConstants.RULE_TGT_SQL_ALL_NORMALIZING_SQLSERVER)) {
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

			databaseStrategy = new OracleStrategy(result, this);
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

		this.namingScheme = new DefaultNamingScheme(result, normalizer,
				fkNaming, ckNaming, uniqueConstraintNaming);

		idColumnName = options.parameterAsString(this.getClass().getName(),
				SqlConstants.PARAM_ID_COLUMN_NAME,
				SqlConstants.DEFAULT_ID_COLUMN_NAME, false, true);

		oneToManyReferenceColumnName = options.parameterAsString(
				this.getClass().getName(),
				SqlConstants.PARAM_ONE_TO_MANY_REF_COLUMN_NAME,
				SqlConstants.DEFAULT_ONE_TO_MANY_REF_COLUMN_NAME, false, true);

		foreignKeyColumnSuffix = options.parameterAsString(
				this.getClass().getName(),
				SqlConstants.PARAM_FOREIGN_KEY_COLUMN_SUFFIX,
				SqlConstants.DEFAULT_FOREIGN_KEY_COLUMN_SUFFIX, true, false);

		foreignKeyColumnSuffixDatatype = options.parameterAsString(
				this.getClass().getName(),
				SqlConstants.PARAM_FOREIGN_KEY_COLUMN_SUFFIX_DATATYPE,
				SqlConstants.DEFAULT_FOREIGN_KEY_COLUMN_SUFFIX_DATATYPE, true,
				false);

		foreignKeyColumnDataType = options.parameterAsString(
				this.getClass().getName(),
				SqlConstants.PARAM_FOREIGN_KEY_COLUMN_DATA_TYPE,
				databaseStrategy.primaryKeyDataType(), false, true);

		primaryKeyColumnSpec = options.parameterAsString(
				this.getClass().getName(),
				SqlConstants.PARAM_PRIMARYKEY_COLUMNSPEC,
				SqlConstants.DEFAULT_PRIMARYKEY_COLUMNSPEC, true, true);

		nameActiveIndicatorLFColumn = options.parameterAsString(
				this.getClass().getName(),
				SqlConstants.PARAM_NAME_ACTIVE_INDICATOR_LF_COLUMN,
				SqlConstants.DEFAULT_NAME_ACTIVE_INDICATOR_LF_COLUMN, false,
				true);

		nameSourceGCLColumn = options.parameterAsString(
				this.getClass().getName(),
				SqlConstants.PARAM_NAME_SOURCE_GCL_COLUMN,
				SqlConstants.DEFAULT_NAME_SOURCE_GCL_COLUMN, false, true);

		String sdoDimElement_value = options.parameterAsString(
				this.getClass().getName(), SqlConstants.PARAM_SDO_DIM_ELEMENTS,
				null, false, true);
		parseSdoDimElementValue(sdoDimElement_value);

		defaultSize = options.parameterAsInteger(this.getClass().getName(),
				SqlConstants.PARAM_SIZE, SqlConstants.DEFAULT_SIZE);

		srid = options.parameterAsInteger(this.getClass().getName(),
				SqlConstants.PARAM_SRID, SqlConstants.DEFAULT_SRID);

		createReferences = options.parameterAsBoolean(this.getClass().getName(),
				SqlConstants.PARAM_CREATE_REFERENCES,
				SqlConstants.DEFAULT_CREATE_REFERNCES);

		createDocumentation = options.parameterAsBoolean(
				this.getClass().getName(),
				SqlConstants.PARAM_CREATE_DOCUMENTATION,
				SqlConstants.DEFAULT_CREATE_DOCUMENTATION);
		/*
		 * override parameter 'createDocumentation' if configured via conversion
		 * rule
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

				descriptorsForCodelist
						.add(new DescriptorForCodeList(name, columnName, size));

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
		 * set of parameters for naming of columns when converting code list to
		 * table
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
			 * It is unlikely but not impossible that an application schema does
			 * not make use of types that require a type mapping in order to be
			 * converted into a database schema.
			 */
			result.addWarning(this, 15);

		} else {

			/*
			 * Parse all parameter information
			 */
			mepp = new MapEntryParamInfos(result, mapEntries);
		}

		// ======================================
		// Replication schema configuration
		// ======================================

		if (schema.matches(
				ReplicationSchemaConstants.RULE_TGT_SQL_ALL_REPSCHEMA)) {

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

			repSchemaDocumentationUnlimitedLengthCharacterDataType = options
					.parameterAsString(this.getClass().getName(),
							ReplicationSchemaConstants.PARAM_DOCUMENTATION_UNLIMITEDLENGTHCHARACTERDATATYPE,
							ReplicationSchemaConstants.DEFAULT_DOCUMENTATION_UNLIMITEDLENGTHCHARACTERDATATYPE,
							false, true);
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

					sdae.addElement(sde);
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

	/**
	 * @see de.interactive_instruments.ShapeChange.Target.Target#write()
	 */
	public void write() {

		if (printed || diagnosticsOnly || !isEncoded(schema)) {
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
		BufferedWriter writer = null;
		try {

			if (schema.matches(
					ReplicationSchemaConstants.RULE_TGT_SQL_ALL_REPSCHEMA)) {

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

				String fileName = schema.name().replace("/", "_").replace(" ",
						"_") + ".xsd";

				/*
				 * Uses OutputStreamWriter instead of FileWriter to set
				 * character encoding (see doc in Serializer.setWriter and
				 * FileWriter)
				 */

				File repXsd = new File(outputDirectory, fileName);

				writer = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(repXsd), "UTF-8"));

				Serializer serializer = SerializerFactory
						.getSerializer(outputFormat);
				serializer.setWriter(writer);
				serializer.asDOMSerializer().serialize(visitor.getDocument());

				// writer.close();

				result.addResult(getTargetID(), outputDirectory, fileName,
						schema.targetNamespace());

				printed = true;

			} else {

				// Create DDL
				StringBuffer sb = new StringBuffer();
				DdlVisitor visitor = new DdlVisitor(SqlConstants.CRLF,
						SqlConstants.INDENT, this);
				visitor.visit(stmts);
				sb.append(visitor.getDdl());

				// Write DDL to file
				String fileName = schema.name().replace("/", "_").replace(" ",
						"_") + ".sql";

				File file = new File(outputDirectory, fileName);
				writer = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(file), "UTF-8"));
				writer.write(sb.toString());
				// writer.close();
				result.addResult(getTargetID(), outputDirectory, fileName,
						null);

				printed = true;
			}

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

	public int getTargetID() {
		return TargetIdentification.SQLDDL.getId();
	}

	public String getForeignKeyColumnDataType() {
		return foreignKeyColumnDataType;
	}

	/**
	 * @return the idColumnName
	 */
	public String getIdColumnName() {
		return idColumnName;
	}

	/**
	 * @return the mepp
	 */
	public MapEntryParamInfos getMapEntryParamInfos() {
		return mepp;
	}

	/**
	 * @return the schema
	 */
	public PackageInfo getSchema() {
		return schema;
	}

	/**
	 * @return the codeNameSize
	 */
	public int getCodeNameSize() {
		return codeNameSize;
	}

	/**
	 * @return the databaseStrategy
	 */
	public DatabaseStrategy getDatabaseStrategy() {
		return databaseStrategy;
	}

	/**
	 * @return the defaultSize
	 */
	public int getDefaultSize() {
		return defaultSize;
	}

	/**
	 * @return the srid
	 */
	public int getSrid() {
		return srid;
	}

	/**
	 * @return the foreignKeyColumnSuffix
	 */
	public String getForeignKeyColumnSuffix() {
		return foreignKeyColumnSuffix;
	}

	/**
	 * @return the foreignKeyColumnSuffixDatatype
	 */
	public String getForeignKeyColumnSuffixDatatype() {
		return foreignKeyColumnSuffixDatatype;
	}

	/**
	 * @return the createReferences
	 */
	public boolean isCreateReferences() {
		return createReferences;
	}

	/**
	 * @return the createAssociativeTables
	 */
	public boolean isCreateAssociativeTables() {
		return createAssociativeTables;
	}

	/**
	 * @return the codeNameColumnName
	 */
	public String getCodeNameColumnName() {
		return codeNameColumnName;
	}

	/**
	 * @return the descriptorsForCodelist
	 */
	public List<DescriptorForCodeList> getDescriptorsForCodelist() {
		return descriptorsForCodelist;
	}

	/**
	 * @return the documentationTemplate
	 */
	public String getDocumentationTemplate() {
		return documentationTemplate;
	}

	/**
	 * @return the documentationNoValue
	 */
	public String getDocumentationNoValue() {
		return documentationNoValue;
	}

	/**
	 * @return the createDocumentation
	 */
	public boolean isCreateDocumentation() {
		return createDocumentation;
	}

	public static boolean isEncoded(Info i) {

		if (i.matches(SqlConstants.RULE_TGT_SQL_ALL_NOTENCODED)
				&& i.encodingRule("sql").equalsIgnoreCase("notencoded")) {

			return false;
		} else {
			return true;
		}
	}

	/**
	 * @return the repSchemaTargetNamespaceSuffix
	 */
	public String getRepSchemaTargetNamespaceSuffix() {
		return repSchemaTargetNamespaceSuffix;
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

		case 503:
			return "Output file '$1$' already exists in output directory ('$2$'). It will be deleted prior to processing.";
		case 504:
			return "File has been deleted.";

		default:
			return "(" + SqlDdl.class.getName()
					+ ") Unknown message with number: " + mnr;
		}
	}

	/**
	 * @return the options
	 */
	public Options getOptions() {
		return options;
	}

	/**
	 * @return the result
	 */
	public ShapeChangeResult getResult() {
		return result;
	}

	/**
	 * @return the repSchemaObjectIdentifierFieldType
	 */
	public String getRepSchemaObjectIdentifierFieldType() {
		return repSchemaObjectIdentifierFieldType;
	}

	/**
	 * @return the repSchemaForeignKeyFieldType
	 */
	public String getRepSchemaForeignKeyFieldType() {
		return repSchemaForeignKeyFieldType;
	}

	/**
	 * @return the repSchemaDocumentationUnlimitedLengthCharacterDataType
	 */
	public String getRepSchemaDocumentationUnlimitedLengthCharacterDataType() {
		return repSchemaDocumentationUnlimitedLengthCharacterDataType;
	}

	/**
	 * @return the primaryKeyColumnSpec
	 */
	public String getPrimaryKeyColumnSpec() {
		return primaryKeyColumnSpec;
	}

	public SdoDimArrayExpression getSdoDimArrayExpression() {
		return sdae;
	}
}
