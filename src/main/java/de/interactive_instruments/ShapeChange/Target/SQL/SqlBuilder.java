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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.Model.AssociationInfo;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.SQL.expressions.BetweenExpression;
import de.interactive_instruments.ShapeChange.Target.SQL.expressions.ColumnExpression;
import de.interactive_instruments.ShapeChange.Target.SQL.expressions.DoubleValueExpression;
import de.interactive_instruments.ShapeChange.Target.SQL.expressions.Expression;
import de.interactive_instruments.ShapeChange.Target.SQL.expressions.ExpressionList;
import de.interactive_instruments.ShapeChange.Target.SQL.expressions.InExpression;
import de.interactive_instruments.ShapeChange.Target.SQL.expressions.IsNullExpression;
import de.interactive_instruments.ShapeChange.Target.SQL.expressions.NullValueExpression;
import de.interactive_instruments.ShapeChange.Target.SQL.expressions.OrExpression;
import de.interactive_instruments.ShapeChange.Target.SQL.expressions.StringValueExpression;
import de.interactive_instruments.ShapeChange.Target.SQL.expressions.UnquotedStringExpression;
import de.interactive_instruments.ShapeChange.Target.SQL.naming.SqlNamingScheme;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Alter;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.AlterExpression.AlterOperation;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.CheckConstraint;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Column;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.ColumnDataType;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Comment;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.ConstraintAlterExpression;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.CreateTable;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.ForeignKeyConstraint;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Insert;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.PrimaryKeyConstraint;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Statement;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Table;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.UniqueConstraint;

/**
 * Builds SQL statements for model elements.
 *
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments <dot>
 *         de)
 *
 */
public class SqlBuilder implements MessageSource {

	private static Comparator<Statement> STATEMENT_COMPARATOR = new StatementSortAlphabetic();
	private static Comparator<CreateTable> CREATE_TABLE_COMPARATOR = new CreateTableSortAlphabetic();
	private static Comparator<Column> COLUMN_DEFINITION_COMPARATOR = new ColumnSortAlphabetic();

	private ShapeChangeResult result;
	private Options options;
	private Model model;

	private Pattern pattern_find_true = Pattern.compile("true",
			Pattern.CASE_INSENSITIVE);
	private Pattern pattern_find_false = Pattern.compile("false",
			Pattern.CASE_INSENSITIVE);

	private Map<PropertyInfo, Integer> sizeByCharacterValuedProperty = new HashMap<PropertyInfo, Integer>();

	private List<Table> tables = new ArrayList<Table>();

	private List<CreateTable> createTableStatements = new ArrayList<CreateTable>();
	private List<Alter> foreignKeyConstraints = new ArrayList<Alter>();
	private List<Alter> checkConstraints = new ArrayList<Alter>();
	private List<Alter> uniqueConstraints = new ArrayList<Alter>();
	private List<Statement> geometryMetadataUpdateStatements = new ArrayList<Statement>();
	private List<Statement> geometryIndexStatements = new ArrayList<Statement>();
	private List<Insert> insertStatements = new ArrayList<Insert>();
	private List<Comment> commentStatements = new ArrayList<Comment>();

	private SqlNamingScheme namingScheme;
	private SqlDdl sqlddl;

	public SqlBuilder(SqlDdl sqlddl, ShapeChangeResult result, Options options,
			Model model, SqlNamingScheme namingScheme) {

		this.sqlddl = sqlddl;
		this.result = result;
		this.options = options;

		this.model = model;
		this.namingScheme = namingScheme;
	}

	/**
	 * NOTE: only works for attributes, NOT association roles
	 *
	 * @param pi
	 */
	private Table createAssociativeTableForAttribute(PropertyInfo pi) {

		if (!pi.isAttribute()) {
			return null;
		}

		// identify table name - using tagged value or default name
		String tableName = pi.taggedValuesAll()
				.getFirstValue(SqlConstants.TV_ASSOCIATIVETABLE);

		if (tableName == null || tableName.trim().length() == 0) {

			tableName = pi.inClass().name() + "_" + pi.name();

			result.addInfo(this, 12, pi.name(), pi.inClass().name(), tableName);
		}

		CreateTable createTable = new CreateTable();
		this.createTableStatements.add(createTable);

		Table table = map(tableName);
		createTable.setTable(table);

		table.setAssociativeTable(true);
		table.setRepresentedProperty(pi);
		// TBD: set table documentation?

		List<Column> columns = new ArrayList<Column>();
		table.setColumns(columns);

		/*
		 * Add field to reference pi.inClass
		 * 
		 * NOTE: the primary key for the table will be defined later
		 */
		String classReferenceFieldName = pi.inClass().name()
				+ SqlDdl.idColumnName;
		Column cdInClassReference = createColumn(table, null, null,
				classReferenceFieldName, SqlDdl.foreignKeyColumnDataType,
				SqlConstants.NOT_NULL_COLUMN_SPEC, false, true);
		cdInClassReference.setReferencedTable(map(pi.inClass()));
		columns.add(cdInClassReference);

		Column cdPi = null;

		if (refersToTypeRepresentedByTable(pi)) {

			String piFieldName = determineTableNameForValueType(pi)
					+ SqlDdl.idColumnName;
			String piDocumentation = pi.derivedDocumentation(
					SqlDdl.documentationTemplate, SqlDdl.documentationNoValue);

			if (pi.categoryOfValue() == Options.CODELIST && pi.inClass()
					.matches(SqlConstants.RULE_TGT_SQL_CLS_CODELISTS)) {

				if (isNumericallyValued(pi)) {

					ColumnDataType mappedType = identifyNumericType(pi);

					if (mappedType != null) {
						cdPi = createColumn(table, pi, piDocumentation,
								piFieldName, mappedType,
								SqlConstants.NOT_NULL_COLUMN_SPEC, false, true);

					} else {

						MessageContext mc = result.addError(this, 29,
								pi.typeInfo().name, pi.name());
						if (mc != null) {
							mc.addDetail(this, 2, pi.fullNameInSchema());
						}
					}
				}

				if (cdPi == null) {

					ColumnDataType fieldType = null;

					if (SqlDdl.codeNameSize < 1) {
						fieldType = SqlDdl.databaseStrategy
								.unlimitedLengthCharacterDataType();
					} else {
						fieldType = SqlDdl.databaseStrategy
								.limitedLengthCharacterDataType(
										SqlDdl.codeNameSize,
										SqlDdl.lengthQualifier);
					}

					cdPi = createColumn(table, pi, piDocumentation, piFieldName,
							fieldType, SqlConstants.NOT_NULL_COLUMN_SPEC, false,
							true);
				}

			} else {

				cdPi = createColumn(table, pi, piDocumentation, piFieldName,
						SqlDdl.foreignKeyColumnDataType,
						SqlConstants.NOT_NULL_COLUMN_SPEC, false, true);
			}

			cdPi.setReferencedTable(map(pi));

		} else {

			cdPi = createColumn(table, pi, true);
		}

		columns.add(cdPi);

		PrimaryKeyConstraint pkc = new PrimaryKeyConstraint();
		pkc.setColumns(columns);
		table.addConstraint(pkc);

		return table;
	}

	private Table map(PropertyInfo pi) {

		String tableName = determineTableNameForValueType(pi);

		return map(tableName);
	}

	/**
	 * Will create a table to represent the given class. Will also create
	 * associative tables, as applicable.
	 * 
	 * @param ci
	 */
	private Table createTables(ClassInfo ci) {
		return createTables(ci, ci.name());
	}

	/**
	 * Will create a table to represent the given class. Will also create
	 * associative tables, as applicable.
	 * 
	 * @param ci
	 */
	private Table createTables(ClassInfo ci, String tableName) {

		/*
		 * Identify all properties that will be converted to columns. Create
		 * associative tables as necessary.
		 * 
		 * NOTE: The order of the properties is defined by their sequence
		 * numbers (which is automatically provided by a TreeMap).
		 */

		List<PropertyInfo> propertyInfosForColumns = new ArrayList<PropertyInfo>();

		for (PropertyInfo pi : ci.properties().values()) {

			if (!SqlDdl.isEncoded(pi)) {

				result.addInfo(this, 15, pi.name(), pi.inClass().name());
				continue;
			}

			/*
			 * If the value type of the property is part of the schema but not
			 * enabled for conversion - or not mapped - issue a warning and
			 * continue.
			 */

			// try getting the type class by ID first, then by name
			ClassInfo typeCi = model.classById(pi.typeInfo().id);

			if (typeCi == null) {
				typeCi = model.classByName(pi.typeInfo().name);
			}

			if (typeCi != null
					&& options.targetMapEntry(pi.typeInfo().name,
							pi.encodingRule("sql")) == null
					&& model.isInSelectedSchemas(typeCi)
					&& ((typeCi.category() == Options.OBJECT && !typeCi.matches(
							SqlConstants.RULE_TGT_SQL_CLS_OBJECT_TYPES))
							|| (typeCi.category() == Options.FEATURE
									&& !typeCi.matches(
											SqlConstants.RULE_TGT_SQL_CLS_FEATURE_TYPES))
							|| (typeCi.category() == Options.DATATYPE
									&& !typeCi.matches(
											SqlConstants.RULE_TGT_SQL_CLS_DATATYPES)))) {

				result.addWarning(this, 16, pi.name(), pi.inClass().name(),
						pi.typeInfo().name);
				continue;
			}

			if (pi.isDerived() && pi
					.matches(SqlConstants.RULE_TGT_SQL_PROP_EXCLUDE_DERIVED)) {

				result.addInfo(this, 14, pi.name(), pi.inClass().name());
				continue;
			}

			if (typeCi != null && typeCi.isAbstract() && typeCi
					.matches(SqlConstants.RULE_TGT_SQL_ALL_EXCLUDE_ABSTRACT)) {
				// TBD: exclude if map entry is defined for the abstract type?
				continue;
			}

			if (pi.isAttribute()) {

				if (pi.cardinality().maxOccurs == 1) {

					if (SqlDdl.createAssociativeTables
							&& options.targetMapEntry(pi.typeInfo().name,
									pi.encodingRule("sql")) == null
							&& typeCi != null
							&& typeCi.category() == Options.DATATYPE
							&& typeCi.matches(
									SqlConstants.RULE_TGT_SQL_CLS_DATATYPES)
							&& typeCi.matches(
									SqlConstants.RULE_TGT_SQL_CLS_DATATYPES_ONETOMANY_ONETABLE)
							&& typeCi.matches(
									SqlConstants.RULE_TGT_SQL_CLS_DATATYPES_ONETOMANY_ONETABLE_IGNORE_SINGLE_VALUED_CASE)) {
						/*
						 * Ignore the attribute. Either the property type is
						 * mapped to a database specific type, or the table that
						 * will be created for the (data) type of the attribute
						 * will contain a data type owner field, which can be
						 * used to reference an entry of the table that
						 * represents the inClass() of the attribute.
						 */

					} else {
						propertyInfosForColumns.add(pi);
					}

				} else if (SqlDdl.createAssociativeTables) {

					if (options.targetMapEntry(pi.typeInfo().name,
							pi.encodingRule("sql")) == null && typeCi != null
							&& typeCi.category() == Options.DATATYPE
							&& typeCi.matches(
									SqlConstants.RULE_TGT_SQL_CLS_DATATYPES)
							&& (typeCi.matches(
									SqlConstants.RULE_TGT_SQL_CLS_DATATYPES_ONETOMANY_ONETABLE)
									|| typeCi.matches(
											SqlConstants.RULE_TGT_SQL_CLS_DATATYPES_ONETOMANY_SEVERALTABLES))) {
						/*
						 * ignore the property; it will be represented by a
						 * foreign key column in the according table (depends on
						 * the conversion rule, i.e. if one or several tables
						 * represent the data type and the one-to-many
						 * relationships to it).
						 */

					} else {
						createAssociativeTableForAttribute(pi);
					}

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
				if (tableForAssociationExists(ai)) {
					continue;
				}

				PropertyInfo revPi = pi.reverseProperty();

				/*
				 * If the inClass of the reverse property is not encoded and no
				 * map entry exists, the relationship does not exist in the SQL
				 * encoding. Log a warning and continue.
				 */
				if (!SqlDdl.isEncoded(revPi.inClass())
						&& options.targetMapEntry(revPi.inClass().name(),
								revPi.inClass().encodingRule("sql")) == null) {

					result.addWarning(this, 19, revPi.inClass().name(),
							revPi.name(), pi.inClass().name(), pi.name());
					continue;
				}

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
						 * MaxOccurs = 1 on other end -> the relationship will
						 * be represented by the foreign key field that
						 * represents the reverse property in its inClass.
						 */

						/*
						 * If a map entry exists for the inClass of the reverse
						 * property, then a foreign key field would have to be
						 * added to the table that represents that class,
						 * referencing the table that represents the inClass of
						 * pi. Log an according warning.
						 */
						if (options.targetMapEntry(revPi.inClass().name(),
								revPi.inClass().encodingRule("sql")) != null) {
							result.addWarning(this, 22, revPi.inClass().name(),
									revPi.name(), pi.inClass().name(),
									pi.name());
						}

					} else {

						/*
						 * The reverse property is not navigable or both
						 * association roles have a maximum multiplicity greater
						 * than 1 - then we have an n:m relationship
						 */

						if (SqlDdl.createAssociativeTables) {

							createAssociativeTable(ai);

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

		/*
		 * We identified all properties that will be converted to columns. Now
		 * create the table to represent ci.
		 */

		CreateTable createTable = new CreateTable();
		createTableStatements.add(createTable);

		Table table = map(tableName);
		createTable.setTable(table);

		table.setRepresentedClass(ci);
		table.setDocumentation(ci.derivedDocumentation(
				SqlDdl.documentationTemplate, SqlDdl.documentationNoValue));

		if (SqlDdl.createExplicitComments) {
			createExplicitCommentUnlessNoDocumentation(table);
		}

		List<Column> columns = new ArrayList<Column>();

		// Add object identifier column or use <<identifier>> attribute
		int countIdentifierAttributes = 0;
		for (PropertyInfo pi : propertyInfosForColumns) {
			if (pi.isAttribute() && pi.stereotype("identifier") && ci.matches(
					SqlConstants.RULE_TGT_SQL_CLS_IDENTIFIER_STEREOTYPE)) {
				countIdentifierAttributes++;
			}
		}

		if (countIdentifierAttributes == 0) {

			Column id_cd = createColumn(table, null, null, SqlDdl.idColumnName,
					SqlDdl.databaseStrategy.primaryKeyDataType(),
					SqlDdl.primaryKeySpec, true, false);
			columns.add(id_cd);
			id_cd.setObjectIdentifierColumn(true);
		}

		/*
		 * NOTE: check if countIdentifierAttributes is > 1 is performed in
		 * checkRequirements(...)
		 */

		/*
		 * Flag to keep track if an attribute with stereotype <<identifier>> has
		 * already been set as primary key; if so, subsequent occurrences of
		 * <<identifier>> attributes are ignored.
		 */
		boolean identifierSet = false;

		for (PropertyInfo pi : propertyInfosForColumns) {

			Column cd = createColumn(table, pi, false);
			columns.add(cd);

			if (!identifierSet && pi.isAttribute()
					&& pi.stereotype("identifier") && ci.matches(
							SqlConstants.RULE_TGT_SQL_CLS_IDENTIFIER_STEREOTYPE)) {

				cd.removeSpecification(SqlConstants.NOT_NULL_COLUMN_SPEC);
				cd.addSpecification(SqlDdl.primaryKeySpec);
				identifierSet = true;
			}
		}

		table.setColumns(columns);

		return table;
	}

	/**
	 * Creates a comment statement for the given table, with the documentation
	 * of the table. If the documentation is empty then no comment statement
	 * will be created.
	 * 
	 * @param table
	 */
	private void createExplicitCommentUnlessNoDocumentation(Table table) {

		if (StringUtils.isNotBlank(table.getDocumentation())) {
			commentStatements.add(new Comment(table,
					table.getDocumentation().replaceAll("\\s+", " ").trim()));
		}
	}

	/**
	 * Creates a comment statement for the given column, with the documentation
	 * of the column. If the documentation is empty then no comment statement
	 * will be created.
	 * 
	 * @param column
	 */
	private void createExplicitCommentUnlessNoDocumentation(Column column) {

		if (StringUtils.isNotBlank(column.getDocumentation())) {
			commentStatements.add(new Comment(column,
					column.getDocumentation().replaceAll("\\s+", " ").trim()));
		}
	}

	private boolean tableForAssociationExists(AssociationInfo ai) {

		for (CreateTable cat : this.createTableStatements) {

			Table t = cat.getTable();

			if (t.isAssociativeTable() && t.getRepresentedAssociation() != null
					&& t.getRepresentedAssociation() == ai) {
				return true;
			}
		}

		return false;
	}

	/**
	 * @param ai
	 */
	private Table createAssociativeTable(AssociationInfo ai) {

		// identify table name - using tagged value or default name
		String tableName = ai.taggedValuesAll()
				.getFirstValue(SqlConstants.TV_ASSOCIATIVETABLE);

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

		CreateTable createTable = new CreateTable();
		this.createTableStatements.add(createTable);

		Table table = map(tableName);
		createTable.setTable(table);

		table.setAssociativeTable(true);
		table.setRepresentedAssociation(ai);
		// TBD: set table documentation?

		List<Column> columns = new ArrayList<Column>();
		table.setColumns(columns);

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

		/*
		 * column 1 references the table that represents pi1.inClass; column 1
		 * therefore also represents pi2 (whose value type is pi1.inClass());
		 * it's the other way round for column 2
		 */

		// add field for first reference
		String name_1 = determineTableNameForType(pi1.inClass())
				+ (reflexive ? "_" + pi1.name() : "") + SqlDdl.idColumnName;
		String documentation_1 = pi2.derivedDocumentation(
				SqlDdl.documentationTemplate, SqlDdl.documentationNoValue);
		Column cd1 = createColumn(table, pi2, documentation_1, name_1,
				SqlDdl.foreignKeyColumnDataType,
				SqlConstants.NOT_NULL_COLUMN_SPEC, false, true);
		cd1.setReferencedTable(map(pi1.inClass()));
		columns.add(cd1);

		// add field for second reference
		String name_2 = determineTableNameForType(pi2.inClass())
				+ (reflexive ? "_" + pi2.name() : "") + SqlDdl.idColumnName;
		String documentation_2 = pi1.derivedDocumentation(
				SqlDdl.documentationTemplate, SqlDdl.documentationNoValue);
		Column cd2 = createColumn(table, pi1, documentation_2, name_2,
				SqlDdl.foreignKeyColumnDataType,
				SqlConstants.NOT_NULL_COLUMN_SPEC, false, true);
		cd2.setReferencedTable(map(pi2.inClass()));
		columns.add(cd2);

		PrimaryKeyConstraint pkc = new PrimaryKeyConstraint();
		pkc.setColumns(columns);
		table.addConstraint(pkc);

		return table;
	}

	private Table map(ClassInfo ci) {

		String tableName = determineTableNameForType(ci);

		return map(tableName);
	}

	/**
	 * @param ci
	 */
	private Table createTableForCodeList(ClassInfo ci) {

		CreateTable createTable = new CreateTable();
		this.createTableStatements.add(createTable);

		Table table = map(ci);
		createTable.setTable(table);

		table.setRepresentedClass(ci);
		table.setDocumentation(ci.derivedDocumentation(
				SqlDdl.documentationTemplate, SqlDdl.documentationNoValue));

		if (SqlDdl.createExplicitComments) {
			createExplicitCommentUnlessNoDocumentation(table);
		}

		// --- create the columns for codes
		List<Column> columns = new ArrayList<Column>();
		table.setColumns(columns);

		// create required column to store the code name
		String name = SqlDdl.codeNameColumnName;
		String codeNameColumnDocumentation = SqlDdl.codeNameColumnDocumentation;

		Column cd_codename = null;

		ColumnDataType codeColumnType = null;

		if (isNumericallyValued(ci)) {

			codeColumnType = identifyNumericType(ci);

			if (codeColumnType == null) {

				MessageContext mc = result.addError(this, 28, ci.name());
				if (mc != null) {
					mc.addDetail(this, 1, ci.fullNameInSchema());
				}

			} else {

				cd_codename = createColumn(table, null,
						codeNameColumnDocumentation, name, codeColumnType,
						SqlDdl.primaryKeySpecCodelist, true, false);
			}
		}

		if (cd_codename == null) {

			// store codes as text

			if (SqlDdl.codeNameSize < 1) {

				codeColumnType = SqlDdl.databaseStrategy
						.unlimitedLengthCharacterDataType();

			} else {

				codeColumnType = SqlDdl.databaseStrategy
						.limitedLengthCharacterDataType(SqlDdl.codeNameSize,
								SqlDdl.lengthQualifier);
			}

			cd_codename = createColumn(table, null, codeNameColumnDocumentation,
					name, codeColumnType, SqlDdl.primaryKeySpecCodelist, true,
					false);
		}

		columns.add(cd_codename);

		/*
		 * now add one column for each descriptor, as specified via the
		 * configuration
		 */
		for (DescriptorForCodeList descriptor : SqlDdl.descriptorsForCodelist) {

			ColumnDataType descriptor_fieldType;
			if (descriptor.getSize() == null) {
				descriptor_fieldType = SqlDdl.databaseStrategy
						.unlimitedLengthCharacterDataType();
			} else {
				descriptor_fieldType = SqlDdl.databaseStrategy
						.limitedLengthCharacterDataType(descriptor.getSize(),
								SqlDdl.lengthQualifier);
			}

			String descriptorDocumentation = descriptor.getDocumentation();

			Column cd_descriptor = createColumn(table, null,
					descriptorDocumentation, descriptor.getColumnName(),
					descriptor_fieldType, "", false, false);
			columns.add(cd_descriptor);
		}

		if (ci.matches(SqlConstants.RULE_TGT_SQL_CLS_CODELISTS_PODS)) {

			ClassInfo codeStatusCLType = model
					.classByName(SqlDdl.codeStatusCLType);

			if (ci != codeStatusCLType) {

				// add codeStatusCL column
				Column cd_codeStatusCl = createColumn(table, null,
						SqlDdl.codeStatusCLColumnDocumentation,
						SqlDdl.nameCodeStatusCLColumn,
						SqlDdl.foreignKeyColumnDataType, "", false, false);

				if (codeStatusCLType != null) {

					if (codeStatusCLType.category() == Options.ENUMERATION) {

						cd_codeStatusCl
								.setEnumerationValueType(codeStatusCLType);

						// assume textual type
						ColumnDataType codeStatusCLDataType = determineCharacterVaryingOrText(
								SqlDdl.codeStatusCLLength,
								SqlDdl.lengthQualifier);
						cd_codeStatusCl.setDataType(codeStatusCLDataType);

						// now check if the code status type is numerically
						// valued
						if (isNumericallyValued(codeStatusCLType)) {

							ColumnDataType mappedType = identifyNumericType(
									codeStatusCLType);
							if (mappedType != null) {
								cd_codeStatusCl.setDataType(mappedType);
							} else {
								result.addError(this, 31,
										codeStatusCLType.name(),
										SqlDdl.nameCodeStatusCLColumn,
										table.getName());
							}
						}

					} else if (isRepresentedByTable(codeStatusCLType)) {

						cd_codeStatusCl.setForeignKeyColumn(true);
						cd_codeStatusCl
								.setReferencedTable(map(codeStatusCLType));

					} else {
						result.addError(this, 30, SqlDdl.codeStatusCLType,
								SqlDdl.nameCodeStatusCLColumn, table.getName());
					}

				} else {
					result.addError(this, 26, SqlDdl.codeStatusCLType,
							SqlDdl.nameCodeStatusCLColumn, table.getName());
				}

				columns.add(cd_codeStatusCl);

				// add codeStatusNotes column
				Column cd_codeStatusNotes = createColumn(table, null,
						SqlDdl.codeStatusNotesColumnDocumentation,
						SqlDdl.nameCodeStatusNotesColumn,
						SqlDdl.databaseStrategy.limitedLengthCharacterDataType(
								255, SqlDdl.lengthQualifier),
						"", false, false);
				columns.add(cd_codeStatusNotes);

				// add codeSupercedes column
				Column cd_codeSupercedes = createColumn(table, null,
						SqlDdl.codeSupercedesColumnDocumentation,
						SqlDdl.nameCodeSupercedesColumn, codeColumnType, "",
						false, false);
				columns.add(cd_codeSupercedes);

			} else if (ci == codeStatusCLType) {

				table.setRepresentsCodeStatusCLType(true);
			}
		}

		return table;
	}

	/**
	 * @param ci
	 * @return If a map entry with param = {@value #ME_PARAM_TABLE} is defined
	 *         for the given class, the targetType defined by the map entry is
	 *         returned. Else if a table has already been created for the class,
	 *         its name is returned. Otherwise the name of the class is
	 *         returned.
	 */
	private String determineTableNameForType(ClassInfo ci) {

		ProcessMapEntry pme = options.targetMapEntry(ci.name(),
				ci.encodingRule("sql"));

		if (pme != null && SqlDdl.mapEntryParamInfos.hasParameter(pme,
				SqlConstants.ME_PARAM_TABLE)) {

			return pme.getTargetType();

		} else {

			for (CreateTable ct : this.createTableStatements) {
				if (ct.getTable() != null
						&& ct.getTable().representsClass(ci)) {
					return ct.getTable().getName();
				}
			}

			return ci.name();
		}
	}

	/**
	 * @param table
	 * @param pi
	 */
	private void alterTableAddCheckConstraintForEnumerationValueType(
			Column column) {

		PropertyInfo pi = column.getRepresentedProperty();

		/*
		 * ignore the constraint if a type mapping exists for the value type of
		 * pi
		 */
		ProcessMapEntry pme = options.targetMapEntry(pi.typeInfo().name,
				pi.encodingRule("sql"));

		if (pme != null) {
			return;
		}

		// look up the enumeration type
		ClassInfo enumCi = model.classById(pi.typeInfo().id);

		if (enumCi == null || enumCi.properties().size() == 0) {

			result.addError(this, 18, pi.typeInfo().name,
					pi.fullNameInSchema());
		} else {

			alterTableAddCheckConstraintForEnumerationValueType(column, enumCi);
		}
	}

	/**
	 * @param column
	 * @param propertyNameForConstraintName
	 *                                          name of the property for which
	 *                                          the check constraint is created;
	 *                                          relevant for constructing the
	 *                                          constraint name
	 * @param enumCi
	 */
	private void alterTableAddCheckConstraintForEnumerationValueType(
			Column column, ClassInfo enumCi) {

		/*
		 * ignore the constraint if a type mapping exists for the value type
		 */
		ProcessMapEntry pme = options.targetMapEntry(enumCi.name(),
				enumCi.encodingRule("sql"));

		if (pme != null) {
			return;
		}

		Table tableWithColumn = column.getInTable();

		if (enumCi.properties().size() == 0) {

			result.addError(this, 32, enumCi.name(), column.getName());
		} else {

			String constraintName = namingScheme.nameForCheckConstraint(
					SqlUtil.determineName(tableWithColumn,
							SqlDdl.constraintNameUsingShortName),
					SqlUtil.determineName(column,
							SqlDdl.constraintNameUsingShortName));

			Alter alter = new Alter();
			alter.setTable(tableWithColumn);

			ConstraintAlterExpression cae = new ConstraintAlterExpression();
			alter.setExpression(cae);

			cae.setOperation(AlterOperation.ADD);

			CheckConstraint cc = new CheckConstraint();
			cae.setConstraint(cc);

			cc.setName(constraintName);

			InExpression iexp = new InExpression();

			ColumnExpression col = new ColumnExpression(column);
			iexp.setLeftExpression(col);

			ExpressionList el = new ExpressionList();
			List<Expression> el_tmp = new ArrayList<Expression>();
			el.setExpressions(el_tmp);

			for (PropertyInfo enumPi : enumCi.properties().values()) {

				if (!SqlDdl.isEncoded(enumPi)) {
					continue;
				}

				String value = enumPi.name();
				if (enumPi.initialValue() != null) {
					value = enumPi.initialValue();
				}

				if (isNumericallyValued(enumCi)) {

					UnquotedStringExpression use = new UnquotedStringExpression(
							value);
					el_tmp.add(use);

				} else {

					// escape single quotes in the enumeration value
					value = StringUtils.replace(value, "'", "''");
					StringValueExpression sv = new StringValueExpression(value);
					el_tmp.add(sv);
				}
			}

			iexp.setRightExpressionsList(el);

			if (column.isNotNull()) {
				cc.setExpression(iexp);
			} else {
				// add null check
				IsNullExpression nullexp = new IsNullExpression();
				nullexp.setExpression(col);

				OrExpression orexp = new OrExpression(nullexp, iexp);
				cc.setExpression(orexp);
			}

			this.checkConstraints.add(alter);
		}
	}

	private boolean isNumericallyValued(Info i) {

		if (i instanceof ClassInfo || i instanceof PropertyInfo) {

			ClassInfo type = null;

			if (i instanceof PropertyInfo) {

				PropertyInfo pi = (PropertyInfo) i;
				type = model.classByIdOrName(pi.typeInfo());

			} else {
				type = (ClassInfo) i;
			}

			if (type != null && StringUtils.isNotBlank(
					type.taggedValue(SqlConstants.TV_NUMERIC_TYPE))) {
				return true;
			}
		}

		return false;
	}

	private void alterTableAddCheckConstraintToRestrictTimeOfDate(
			Column column) {

		Expression expr = SqlDdl.databaseStrategy
				.expressionForCheckConstraintToRestrictTimeOfDate(column);

		if (expr != null) {

			Table tableWithColumn = column.getInTable();

			String constraintName = namingScheme.nameForCheckConstraint(
					SqlUtil.determineName(tableWithColumn,
							SqlDdl.constraintNameUsingShortName),
					SqlUtil.determineName(column,
							SqlDdl.constraintNameUsingShortName));

			Alter alter = new Alter();
			alter.setTable(tableWithColumn);

			ConstraintAlterExpression cae = new ConstraintAlterExpression();
			alter.setExpression(cae);

			cae.setOperation(AlterOperation.ADD);

			CheckConstraint cc = new CheckConstraint();
			cae.setConstraint(cc);

			cc.setName(constraintName);

			cc.setExpression(expr);

			this.checkConstraints.add(alter);
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

		String valueTypeName = pi.typeInfo().name;
		String piEncodingRule = pi.encodingRule("sql");

		ProcessMapEntry pme = options.targetMapEntry(valueTypeName,
				piEncodingRule);

		return pme != null && SqlDdl.mapEntryParamInfos.hasParameter(
				valueTypeName, piEncodingRule, SqlConstants.ME_PARAM_GEOMETRY);
	}

	/**
	 * @param pi
	 * @return If a map entry with param = {@value SqlDdl#ME_PARAM_TABLE} is
	 *         defined for the value type of the property, the targetType
	 *         defined by the map entry is returned. Otherwise the name of the
	 *         value type is returned.
	 */
	private String determineTableNameForValueType(PropertyInfo pi) {

		String valueTypeName = pi.typeInfo().name;
		String piEncodingRule = pi.encodingRule("sql");

		ProcessMapEntry pme = options.targetMapEntry(valueTypeName,
				piEncodingRule);

		if (pme != null && SqlDdl.mapEntryParamInfos.hasParameter(valueTypeName,
				piEncodingRule, SqlConstants.ME_PARAM_TABLE)) {

			return SqlDdl.mapEntryParamInfos
					.getMapEntry(valueTypeName, piEncodingRule).getTargetType();
		} else {

			/*
			 * If no map entry with table parameter was found for the value
			 * type, try looking up the table that represents the value type
			 * first; use the type name as fallback.
			 */

			ClassInfo valueType = model.classById(pi.typeInfo().id);
			if (valueType == null) {
				valueType = model.classByName(pi.typeInfo().name);
			}

			if (valueType != null) {
				for (CreateTable ct : this.createTableStatements) {
					if (ct.getTable().representsClass(valueType)) {
						return ct.getTable().getName();
					}
				}
			}

			return pi.typeInfo().name;
		}
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
	 *         {@value SqlConstants#RULE_TGT_SQL_CLS_REFERENCES_TO_EXTERNAL_TYPES}
	 *         is enabled, then the return value is <code>true</code> - else
	 *         <code>false</code>.
	 */
	private boolean refersToTypeRepresentedByTable(PropertyInfo pi) {

		String name = pi.typeInfo().name;
		String encodingRule = pi.encodingRule("sql");

		ProcessMapEntry pme = options.targetMapEntry(name, encodingRule);

		if (pme != null) {

			if (SqlDdl.mapEntryParamInfos.hasParameter(name, encodingRule,
					SqlConstants.ME_PARAM_TABLE)) {
				return true;
			} else {
				return false;
			}

		} else {

			ClassInfo typeCi = this.model.classById(pi.typeInfo().id);

			if (typeCi != null) {
				return isRepresentedByTable(typeCi);
			} else {
				return false;
			}
		}
	}

	private boolean isRepresentedByTable(ClassInfo ci) {

		String name = ci.name();
		String encodingRule = ci.encodingRule("sql");

		ProcessMapEntry pme = options.targetMapEntry(name, encodingRule);

		if (pme != null) {

			if (SqlDdl.mapEntryParamInfos.hasParameter(name, encodingRule,
					SqlConstants.ME_PARAM_TABLE)) {
				return true;
			} else {
				return false;
			}

		} else if (ci.category() == Options.FEATURE
				|| ci.category() == Options.OBJECT
				|| ci.category() == Options.DATATYPE
				|| ci.category() == Options.CODELIST) {

			if ((ci.category() == Options.OBJECT
					&& !ci.matches(SqlConstants.RULE_TGT_SQL_CLS_OBJECT_TYPES))
					|| (ci.category() == Options.FEATURE && !ci.matches(
							SqlConstants.RULE_TGT_SQL_CLS_FEATURE_TYPES))
					|| (ci.category() == Options.DATATYPE && !ci
							.matches(SqlConstants.RULE_TGT_SQL_CLS_DATATYPES))
					|| (ci.category() == Options.CODELIST && !ci.matches(
							SqlConstants.RULE_TGT_SQL_CLS_CODELISTS))) {

				return false;

			} else {

				if (model.isInSelectedSchemas(ci) || ci.matches(
						SqlConstants.RULE_TGT_SQL_CLS_REFERENCES_TO_EXTERNAL_TYPES)) {

					return true;

					/*
					 * NOTE: if the schema uses external types, map entries
					 * should be defined. This helps avoiding confusion of types
					 * that are not processed but used in the model (e.g. from
					 * ISO packages, or application schemas that were not
					 * selected for processing). The rule to allow references to
					 * external types is a convenience mechanism.
					 */

				} else {

					return false;
				}
			}

		} else {

			return false;
		}

	}

	private Column createColumn(Table inTable, PropertyInfo representedProperty,
			String documentation, String name, ColumnDataType type,
			String columnSpecification, boolean isPrimaryKey,
			boolean isForeignKey) {

		Column column = new Column(name, representedProperty, documentation,
				inTable);

		if (SqlDdl.createExplicitComments) {
			createExplicitCommentUnlessNoDocumentation(column);
		}

		column.setDataType(type);

		if (columnSpecification != null
				&& !columnSpecification.trim().isEmpty()) {
			column.addSpecification(columnSpecification.trim());
		}

		column.setForeignKeyColumn(isForeignKey);

		return column;
	}

	/**
	 * Creates the column definition based upon the property name, its type, and
	 * a possibly defined initial value. Also adds "NOT NULL" if indicated via
	 * parameter or for all properties that can be nil/null (set via tagged
	 * value or stereotype) or which are optional.
	 *
	 * @param pi
	 * @param alwaysNotNull
	 *                          <code>true</code> if the column definition shall
	 *                          be created with NOT NULL, otherwise
	 *                          <code>false</code> (then default behavior
	 *                          applies)
	 * @return The SQL statement with the definition of the column to represent
	 *         the property
	 */
	private Column createColumn(Table inTable, PropertyInfo pi,
			boolean alwaysNotNull) {

		String name;
		boolean isForeignKeyColumn = false;
		if (refersToTypeRepresentedByTable(pi)) {
			name = pi.name() + identifyForeignKeyColumnSuffix(pi);
			isForeignKeyColumn = true;
		} else {
			name = pi.name();
		}

		String documentation = pi.derivedDocumentation(
				SqlDdl.documentationTemplate, SqlDdl.documentationNoValue);

		Column cd = new Column(name, pi, documentation, inTable);

		if (SqlDdl.createExplicitComments) {
			createExplicitCommentUnlessNoDocumentation(cd);
		}

		ColumnDataType colDataType = identifyType(pi);
		cd.setDataType(colDataType);

		if (isForeignKeyColumn) {
			cd.setForeignKeyColumn(true);
			cd.setReferencedTable(map(pi));
		}

		List<String> columnSpecStrings = new ArrayList<String>();

		String columnDefault = pi.initialValue();

		if (columnDefault != null && columnDefault.trim().length() > 0) {

			Expression defaultValue = null;

			String booleanTrue = "TRUE";
			String booleanFalse = "FALSE";
			boolean quoted = false;

			/*
			 * If the value type is a code list or enumeration, quote the
			 * default value - unless it is a numerically valued code list. This
			 * can be overridden via map entry param characteristics.
			 */
			if (pi.categoryOfValue() == Options.CODELIST
					|| pi.categoryOfValue() == Options.ENUMERATION) {

				ClassInfo valueType = pi.model().classByIdOrName(pi.typeInfo());

				if (valueType == null || !isNumericallyValued(valueType)) {
					quoted = true;
				}
			}

			/*
			 * Check map entry parameter infos for any defaultValue
			 * characteristics defined for the value type of pi.
			 */
			Map<String, String> characteristics = SqlDdl.mapEntryParamInfos
					.getCharacteristics(pi.typeInfo().name,
							pi.encodingRule("sql"),
							SqlConstants.ME_PARAM_DEFAULTVALUE);

			if (characteristics != null) {

				if (characteristics.containsKey(
						SqlConstants.ME_PARAM_DEFAULTVALUE_CHARACT_TRUE)) {
					booleanTrue = characteristics.get(
							SqlConstants.ME_PARAM_DEFAULTVALUE_CHARACT_TRUE);
				}

				if (characteristics.containsKey(
						SqlConstants.ME_PARAM_DEFAULTVALUE_CHARACT_FALSE)) {
					booleanFalse = characteristics.get(
							SqlConstants.ME_PARAM_DEFAULTVALUE_CHARACT_FALSE);
				}

				if (characteristics.containsKey(
						SqlConstants.ME_PARAM_DEFAULTVALUE_CHARACT_QUOTED)) {
					quoted = characteristics.get(
							SqlConstants.ME_PARAM_DEFAULTVALUE_CHARACT_QUOTED)
							.equalsIgnoreCase("true");
				}
			}

			if (pi.typeInfo().name.equals("Boolean")) {

				if (pattern_find_true.matcher(columnDefault).find()) {

					defaultValue = quoted
							? new StringValueExpression(booleanTrue)
							: new UnquotedStringExpression(booleanTrue);

				} else if (pattern_find_false.matcher(columnDefault).find()) {

					defaultValue = quoted
							? new StringValueExpression(booleanFalse)
							: new UnquotedStringExpression(booleanFalse);
				}
			}

			if (defaultValue == null) {

				defaultValue = quoted
						? new StringValueExpression(
								columnDefault.replaceAll("'", "''"))
						: new UnquotedStringExpression(columnDefault);
			}

			cd.setDefaultValue(defaultValue);
		}

		// ----- add constraints

		if (alwaysNotNull) {
			columnSpecStrings.add(SqlConstants.NOT_NULL_COLUMN_SPEC);
		} else {
			// set NOT NULL if required
			if (pi.implementedByNilReason() || pi.nilReasonAllowed()
					|| pi.voidable() || pi.cardinality().minOccurs < 1) {
				/*
				 * in these cases the default behavior (that the field can be
				 * NULL) is ok
				 */
			} else {
				columnSpecStrings.add(SqlConstants.NOT_NULL_COLUMN_SPEC);
			}
		}

		cd.setSpecifications(columnSpecStrings);

		return cd;
	}

	private String identifyForeignKeyColumnSuffix(PropertyInfo pi) {

		String typeName = pi.typeInfo().name;
		String piEncodingRule = pi.encodingRule("sql");

		ProcessMapEntry pme = options.targetMapEntry(typeName, piEncodingRule);

		if (pme != null && SqlDdl.mapEntryParamInfos.hasCharacteristic(typeName,
				piEncodingRule, SqlConstants.ME_PARAM_TABLE,
				SqlConstants.ME_PARAM_TABLE_CHARACT_REP_CAT)) {

			String repCat = SqlDdl.mapEntryParamInfos.getCharacteristic(
					typeName, piEncodingRule, SqlConstants.ME_PARAM_TABLE,
					SqlConstants.ME_PARAM_TABLE_CHARACT_REP_CAT);

			if (repCat != null && repCat.equalsIgnoreCase("datatype")) {
				return SqlDdl.foreignKeyColumnSuffixDatatype;
			} else if (repCat != null && repCat.equalsIgnoreCase("codelist")) {
				return SqlDdl.foreignKeyColumnSuffixCodelist;
			} else {
				return SqlDdl.foreignKeyColumnSuffix;
			}

		} else if (pi.categoryOfValue() == Options.DATATYPE) {
			return SqlDdl.foreignKeyColumnSuffixDatatype;
		} else if (pi.categoryOfValue() == Options.CODELIST) {
			return SqlDdl.foreignKeyColumnSuffixCodelist;
		} else {
			return SqlDdl.foreignKeyColumnSuffix;
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
	private ColumnDataType identifyType(PropertyInfo pi) {

		// first apply well-known mappings

		// try to get type from map entries
		ProcessMapEntry me = options.targetMapEntry(pi.typeInfo().name,
				pi.encodingRule("sql"));

		if (me != null) {

			if (SqlDdl.mapEntryParamInfos.hasParameter(me,
					SqlConstants.ME_PARAM_GEOMETRY)) {

				return new ColumnDataType(SqlDdl.databaseStrategy
						.geometryDataType(me, SqlDdl.srid));

			} else if (SqlDdl.mapEntryParamInfos.hasParameter(me,
					SqlConstants.ME_PARAM_TABLE)) {

				return SqlDdl.foreignKeyColumnDataType;

			} else {

				if (me.getTargetType()
						.startsWith(SqlConstants.MAP_TARGETTYPE_COND_PART)) {

					String conditionalCriterium = me.getTargetType().substring(
							SqlConstants.MAP_TARGETTYPE_COND_PART.length());

					if (conditionalCriterium.equalsIgnoreCase(
							SqlConstants.MAP_TARGETTYPE_COND_TEXTORCHARACTERVARYING)) {
						return determineCharacterVaryingOrText(pi);
					}

				} else if (SqlDdl.mapEntryParamInfos.hasParameter(me,
						SqlConstants.ME_PARAM_TEXTORCHARACTERVARYING)) {

					return determineCharacterVaryingOrText(pi);

				} else {

					ColumnDataType type = determineTypeFromMapEntry(me);

					// local override of precision and scale is allowed
					updatePrecisionAndScaleWithLocalInfo(type, pi);

					return type;
				}
			}
		}

		// try to identify a type mapping based upon the category of the
		// property value
		int catOfValue = pi.categoryOfValue();

		if (catOfValue == Options.ENUMERATION) {

			if (isNumericallyValued(pi)) {

				ColumnDataType mappedType = identifyNumericType(pi);
				if (mappedType != null) {
					return mappedType;
				} else {
					MessageContext mc = result.addError(this, 29,
							pi.typeInfo().name, pi.name());
					if (mc != null) {
						mc.addDetail(this, 2, pi.fullNameInSchema());
					}
				}
			}

			return determineCharacterVaryingOrText(pi);

		} else if (catOfValue == Options.OBJECT || catOfValue == Options.FEATURE
				|| catOfValue == Options.DATATYPE
				|| catOfValue == Options.CODELIST) {

			ClassInfo typeCi = this.model.classById(pi.typeInfo().id);

			if (typeCi != null) {

				if ((catOfValue == Options.OBJECT && !typeCi
						.matches(SqlConstants.RULE_TGT_SQL_CLS_OBJECT_TYPES))
						|| (catOfValue == Options.FEATURE && !typeCi.matches(
								SqlConstants.RULE_TGT_SQL_CLS_FEATURE_TYPES))
						|| (catOfValue == Options.DATATYPE && !typeCi.matches(
								SqlConstants.RULE_TGT_SQL_CLS_DATATYPES))
						|| (catOfValue == Options.CODELIST && !typeCi.matches(
								SqlConstants.RULE_TGT_SQL_CLS_CODELISTS))) {

					if (catOfValue == Options.CODELIST) {

						if (isNumericallyValued(pi)) {
							ColumnDataType mappedType = identifyNumericType(pi);
							if (mappedType != null) {
								return mappedType;
							} else {
								MessageContext mc = result.addError(this, 29,
										pi.typeInfo().name, pi.name());
								if (mc != null) {
									mc.addDetail(this, 2,
											pi.fullNameInSchema());
								}
							}
						}
					}

					/*
					 * table creation for this category is not enabled -> assign
					 * textual type
					 */
					return determineCharacterVaryingOrText(pi);

				} else {

					if (model.isInSelectedSchemas(typeCi) || typeCi.matches(
							SqlConstants.RULE_TGT_SQL_CLS_REFERENCES_TO_EXTERNAL_TYPES)) {

						if (catOfValue == Options.CODELIST) {

							if (isNumericallyValued(pi)) {
								ColumnDataType mappedType = identifyNumericType(
										pi);
								if (mappedType != null) {
									return mappedType;
								} else {
									MessageContext mc = result.addError(this,
											29, pi.typeInfo().name, pi.name());
									if (mc != null) {
										mc.addDetail(this, 2,
												pi.fullNameInSchema());
									}
								}
							}

							if (SqlDdl.codeNameSize < 1) {
								return SqlDdl.databaseStrategy
										.unlimitedLengthCharacterDataType();
							} else {
								return SqlDdl.databaseStrategy
										.limitedLengthCharacterDataType(
												SqlDdl.codeNameSize,
												SqlDdl.lengthQualifier);
							}

						} else {

							return SqlDdl.foreignKeyColumnDataType;
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

		result.addWarning(this, 21, pi.typeInfo().name);

		return new ColumnDataType("unknown");
	}

	private void updatePrecisionAndScaleWithLocalInfo(ColumnDataType type,
			Info info) {

		if (info.matches(SqlConstants.RULE_TGT_SQL_ALL_PRECISION_AND_SCALE)) {

			Integer precisionFromTV = parseTaggedValue("precision", info);
			Integer scaleFromTV = parseTaggedValue("scale", info);

			if (scaleFromTV != null && precisionFromTV == null) {

				MessageContext mc = result.addWarning(this, 27);
				if (mc != null) {
					mc.addDetail(this, 3, info.fullNameInSchema());
				}

				scaleFromTV = null;
			}

			if (precisionFromTV != null) {
				type.setPrecision(precisionFromTV);
				type.setScale(scaleFromTV);
			}
		}
	}

	private ColumnDataType determineTypeFromMapEntry(ProcessMapEntry me) {

		String dtName = me.getTargetType();

		Integer length = null;
		String lengthQualifier = null;
		Integer precision = null;
		Integer scale = null;

		if (SqlDdl.mapEntryParamInfos.hasParameter(me,
				SqlConstants.ME_PARAM_LENGTH)
				|| SqlDdl.mapEntryParamInfos.hasParameter(me,
						SqlConstants.ME_PARAM_PRECISION)) {

			Matcher lengthPrecisionScale = SqlConstants.PATTERN_ME_TARGETTYPE_LENGTH_PRECISION_SCALE
					.matcher(me.getTargetType().trim());

			if (lengthPrecisionScale.matches()) {

				dtName = lengthPrecisionScale.group(1);
				String group2 = lengthPrecisionScale.group(2);
				String group3 = lengthPrecisionScale.group(3);

				if (SqlDdl.mapEntryParamInfos.hasParameter(me,
						SqlConstants.ME_PARAM_LENGTH)) {

					/*
					 * check for single non-negative number is covered by the
					 * configuration validator
					 */

					// try {
					length = Integer.parseInt(group2);
					// } catch (NumberFormatException e) {
					// result.addError(this, 30, me.getType(),
					// me.getTargetType(), e.getMessage());
					// }
					
					lengthQualifier = determineLengthQualifierFromMapEntry(me);

				} else if (SqlDdl.mapEntryParamInfos.hasParameter(me,
						SqlConstants.ME_PARAM_PRECISION)) {

					/*
					 * check for non-negative number is covered by the
					 * configuration validator
					 */

					// try {
					precision = Integer.parseInt(group2);
					// } catch (NumberFormatException e) {
					// result.addError(this, 31, me.getType(),
					// me.getTargetType(), e.getMessage());
					// }

					if (group3 != null) {

						/*
						 * check for non-negative number is covered by the
						 * configuration validator
						 */

						// try {
						scale = Integer.parseInt(group3);

						if (scale == 0) {
							scale = null;
						}
						// } catch (NumberFormatException e) {
						// result.addError(this, 32, me.getType(),
						// me.getTargetType(), e.getMessage());
						// }
					}
				}
			}
		}

		return new ColumnDataType(dtName, precision, scale, length,
				lengthQualifier);
	}

	/**
	 * @param i
	 * @return the numeric data type identified via the tagged value
	 *         {@value SqlConstants#TV_NUMERIC_TYPE}, either on the given class
	 *         or on the value type of the given property. Precision and scale
	 *         would also be used. Can be <code>null</code> if the tagged value
	 *         does not exist or if no mapping is defined by configuration map
	 *         entries
	 */
	private ColumnDataType identifyNumericType(Info i) {

		if (i instanceof ClassInfo || i instanceof PropertyInfo) {

			ClassInfo type = null;

			if (i instanceof PropertyInfo) {

				PropertyInfo pi = (PropertyInfo) i;
				type = model.classByIdOrName(pi.typeInfo());

			} else {
				type = (ClassInfo) i;
			}

			String numericConceptualType = type
					.taggedValue(SqlConstants.TV_NUMERIC_TYPE);

			if (type != null && StringUtils.isNotBlank(numericConceptualType)) {

				String encodingRule = i.encodingRule("sql");

				ProcessMapEntry pme = options.targetMapEntry(
						numericConceptualType.trim(), encodingRule);

				if (pme != null && pme.hasTargetType()) {

					ColumnDataType colDt = determineTypeFromMapEntry(pme);

					// local override of precision and scale is allowed
					updatePrecisionAndScaleWithLocalInfo(colDt, type);

					return colDt;

				} else {
					/*
					 * TBD: log error?
					 */
				}
			}
		}

		return null;

	}

	/**
	 * @param taggedValueName
	 * @param i
	 * @return The precision parsed from the tagged value with given name on the
	 *         given info object. Can be <code>null</code> if the object does
	 *         not have such a tagged value with valid integer value.
	 */
	private Integer parseTaggedValue(String taggedValueName, Info i) {

		Integer res = null;

		String value = StringUtils.stripToNull(i.taggedValue(taggedValueName));

		if (value != null) {

			try {
				res = Integer.parseInt(value);
			} catch (NumberFormatException e) {
				MessageContext mc = result.addError(this, 6, taggedValueName,
						value);
				if (mc != null) {
					if (i instanceof ClassInfo) {
						mc.addDetail(this, 1, i.fullNameInSchema());
					} else {
						mc.addDetail(this, 2, i.fullNameInSchema());
					}
				}
			}
		}

		return res;
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
	private ColumnDataType determineCharacterVaryingOrText(PropertyInfo pi) {

		int size = getSizeForProperty(pi);

		// keep track of the result for use by the replication schema
		this.sizeByCharacterValuedProperty.put(pi, size);

		String lengthQualifier = null;

		ProcessMapEntry me = options.targetMapEntry(pi.typeInfo().name,
				pi.encodingRule("sql"));

		if (me != null) {
			lengthQualifier = determineLengthQualifierFromMapEntry(me);
		}

		return determineCharacterVaryingOrText(size,lengthQualifier);
	}
	
	private String determineLengthQualifierFromMapEntry(ProcessMapEntry me) {
		return SqlDdl.mapEntryParamInfos.getCharacteristic(
				me.getType(), me.getRule(), SqlConstants.ME_PARAM_LENGTH,
				SqlConstants.ME_PARAM_LENGTH_CHARACT_LENGTH_QUALIFIER);
	}

	private ColumnDataType determineCharacterVaryingOrText(int size,
			String lengthQualifier) {

		if (size < 1) {
			return SqlDdl.databaseStrategy.unlimitedLengthCharacterDataType();
		} else {
			return SqlDdl.databaseStrategy.limitedLengthCharacterDataType(size,
					lengthQualifier);
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

		String tvSize = pi.taggedValuesAll()
				.getFirstValue(SqlConstants.PARAM_SIZE);

		int size = SqlDdl.defaultSize;

		if (tvSize != null) {
			try {
				size = Integer.parseInt(tvSize);
			} catch (NumberFormatException e) {
				MessageContext mc = result.addWarning(this, 5,
						SqlConstants.PARAM_SIZE, e.getMessage(),
						"" + SqlDdl.defaultSize);
				mc.addDetail(this, 0);
				mc.addDetail(this, 100, pi.fullNameInSchema());
				size = SqlDdl.defaultSize;
			}
		}

		return size;
	}

	/**
	 * Generates an index creation statement for the given geometry
	 * property/column.
	 * 
	 * @param tableWithColumn
	 * @param columnForProperty
	 * @param pi
	 *                              property represented by the column
	 * @return
	 */
	private Statement generateGeometryIndex(Table tableWithColumn,
			Column columnForProperty, PropertyInfo pi) {

		Map<String, String> geometryCharacteristics = SqlDdl.mapEntryParamInfos
				.getCharacteristics(pi.typeInfo().name, pi.encodingRule("sql"),
						SqlConstants.ME_PARAM_GEOMETRY);

		String indexName = namingScheme.nameForGeometryIndex(
				SqlUtil.determineName(tableWithColumn,
						SqlDdl.indexNameUsingShortName),
				SqlUtil.determineName(columnForProperty,
						SqlDdl.indexNameUsingShortName));

		Statement result = SqlDdl.databaseStrategy.geometryIndexColumnPart(
				indexName, tableWithColumn, columnForProperty,
				geometryCharacteristics);

		return result;
	}

	public List<Statement> process(List<ClassInfo> cisToProcess) {

		checkRequirements(cisToProcess);

		// ----------------------------------------
		// Create tables ("normal" and associative)
		// ----------------------------------------
		for (ClassInfo ci : cisToProcess) {

			if (ci.category() == Options.CODELIST) {

				createTableForCodeList(ci);

			} else {

				/*
				 * Check special cases first, to see if a table for the class
				 * should really be created.
				 */

				/*
				 * Check if we have a data type that matches both
				 * RULE_TGT_SQL_CLS_DATATYPES_ONETOMANY_SEVERALTABLES and
				 * RULE_TGT_SQL_CLS_DATATYPES_ONETOMANY_SEVERALTABLES_AVOID_TABLE_FOR_DATATYPE_IF_UNUSED.
				 * Do not create a table for it, unless an attribute in the
				 * classes selected for processing exists that has the data type
				 * as type, and max cardinality 1.
				 */
				if (ci.category() == Options.DATATYPE
						&& ci.matches(SqlConstants.RULE_TGT_SQL_CLS_DATATYPES)
						&& ci.matches(
								SqlConstants.RULE_TGT_SQL_CLS_DATATYPES_ONETOMANY_SEVERALTABLES)
						&& ci.matches(
								SqlConstants.RULE_TGT_SQL_CLS_DATATYPES_ONETOMANY_SEVERALTABLES_AVOID_TABLE_FOR_DATATYPE_IF_UNUSED)) {

					boolean singleValuedCaseExists = false;

					/*
					 * Screen all cisToProcess to identify properties that have
					 * ci as value type (in a one-to-one relationship).
					 */
					outer: for (ClassInfo ci_other : cisToProcess) {

						if (ci_other != ci) {

							for (PropertyInfo pi_other : ci_other.properties()
									.values()) {

								if (pi_other.isAttribute()
										&& pi_other.cardinality().maxOccurs == 1
										&& ci.id().equals(
												pi_other.typeInfo().id)) {
									singleValuedCaseExists = true;
									break outer;
								}
							}
						}
					}

					if (singleValuedCaseExists) {
						createTables(ci);
					}

				} else {

					createTables(ci);
				}
			}
		}

		// ------------------------------------------------------
		/*
		 * Handle table creation and/or modification for
		 * rule-sql-cls-data-types-oneToMany-oneTable and
		 * rule-sql-cls-data-types-oneToMany-severalTables.
		 */
		// ------------------------------------------------------
		for (ClassInfo ci : cisToProcess) {

			if (ci.category() == Options.DATATYPE
					&& ci.matches(SqlConstants.RULE_TGT_SQL_CLS_DATATYPES)) {

				/*
				 * NOTE: rule-sql-cls-data-types-oneToMany-severalTables has
				 * higher priority than
				 * rule-sql-cls-data-types-oneToMany-oneTable
				 */

				if (ci.matches(
						SqlConstants.RULE_TGT_SQL_CLS_DATATYPES_ONETOMANY_SEVERALTABLES)) {

					/*
					 * Screen all cisToProcess to identify properties that have
					 * ci as value type (in a one-to-many relationship); then a
					 * specific table must be created for each such case.
					 */
					for (ClassInfo ci_other : cisToProcess) {

						if (ci_other != ci) {

							for (PropertyInfo pi_other : ci_other.properties()
									.values()) {

								if (pi_other.isAttribute()
										&& pi_other.cardinality().maxOccurs > 1
										&& ci.id().equals(
												pi_other.typeInfo().id)) {

									String tableName = ci_other.name() + "_"
											+ pi_other.name();

									Table table = createTables(ci, tableName);
									table.setRepresentedProperty(pi_other);

									/*
									 * Add the column that supports referencing
									 * the owner of the data type.
									 */

									String columnName = ci_other.name()
											+ SqlDdl.idColumnName;

									Column dtOwner_cd = createColumn(table,
											null, null, columnName,
											SqlDdl.foreignKeyColumnDataType,
											SqlConstants.NOT_NULL_COLUMN_SPEC,
											false, true);
									dtOwner_cd
											.setReferencedTable(map(ci_other));

									table.addColumn(dtOwner_cd);
								}
							}
						}
					}

				} else if (ci.matches(
						SqlConstants.RULE_TGT_SQL_CLS_DATATYPES_ONETOMANY_ONETABLE)) {

					/*
					 * Get the table that has already been created for the data
					 * type and add the column that supports referencing the
					 * owner of the data type.
					 */

					Table table = map(ci);

					/*
					 * Use name defined via configuration parameter, unless TV
					 * is set on the datatype.
					 */
					String columnName = SqlDdl.oneToManyReferenceColumnName;
					String tv_oneToManyReferenceColumnName = ci.taggedValue(
							SqlConstants.TV_ONE_TO_MANY_REF_COLUMN_NAME);
					if (tv_oneToManyReferenceColumnName != null
							&& tv_oneToManyReferenceColumnName.trim()
									.length() > 0) {
						columnName = tv_oneToManyReferenceColumnName.trim();
					}

					String dtOwnerRef_columnSpec = null;
					if (ci.matches(
							SqlConstants.RULE_TGT_SQL_CLS_DATATYPES_ONETOMANY_ONETABLE_IGNORE_SINGLE_VALUED_CASE)) {
						dtOwnerRef_columnSpec = SqlConstants.NOT_NULL_COLUMN_SPEC;
					}

					Column dtOwnerRef_cd = createColumn(table, null, null,
							columnName + SqlDdl.idColumnName,
							SqlDdl.foreignKeyColumnDataType,
							dtOwnerRef_columnSpec, false, true);

					table.addColumn(dtOwnerRef_cd);
				}
			}
		}

		// -----------------------------------------------------------------------
		/*
		 * Adjust data type of foreign key columns according to primary key
		 * column type of referenced table. For auto-generated ID columns that
		 * type is provided by the database strategy. For <<identifier>> columns
		 * the data type can be different.
		 */
		// -----------------------------------------------------------------------
		for (Table table : tables) {

			for (Column col : table.getColumns()) {

				Table refTable = col.getReferencedTable();

				if (refTable != null) {

					for (Column refCol : refTable.getColumns()) {

						if (refCol.isPrimaryKeyColumn()) {

							ColumnDataType refColdt = refCol.getDataType();

							/*
							 * e.g. for reference to numerically valued code
							 * list
							 */
							col.setDataType(new ColumnDataType(
									refColdt.getName(), refColdt.getPrecision(),
									refColdt.getScale(), refColdt.getLength(),
									refColdt.getLengthQualifier()));
						}
					}
				}
			}
		}

		// ----------------------------------------
		// normalize table and column names (alter
		// statements use the latter as literals)
		// ----------------------------------------
		List<Statement> statements = this.statements();
		Collections.sort(statements, STATEMENT_COMPARATOR);
		this.namingScheme.getNameNormalizer().visit(statements);

		// -------------------------------------------------
		// Create alter statements to add check constraints
		// -------------------------------------------------
		// NOTE: order should be irrelevant, since naming scheme is not involved
		for (CreateTable ct : this.createTableStatements) {

			Table t = ct.getTable();

			for (Column col : t.getColumns()) {

				PropertyInfo pi = col.getRepresentedProperty();

				if (pi != null) {

					if (pi.categoryOfValue() == Options.ENUMERATION
							&& pi.matches(
									SqlConstants.RULE_TGT_SQL_PROP_CHECK_CONSTRAINTS_FOR_ENUMERATIONS)) {

						alterTableAddCheckConstraintForEnumerationValueType(
								col);
					}

					if (pi.typeInfo().name.equalsIgnoreCase("Date")
							&& pi.matches(
									SqlConstants.RULE_TGT_SQL_PROP_CHECK_CONSTRAINT_RESTRICT_TIME_OF_DATE)) {

						alterTableAddCheckConstraintToRestrictTimeOfDate(col);
					}

					if (pi.matches(
							SqlConstants.RULE_TGT_SQL_PROP_CHECK_CONSTRAINT_FOR_RANGE)) {

						Double lowerBoundaryValue = Double.valueOf(-1000000000);
						Double upperBoundaryValue = Double.valueOf(1000000000);

						boolean foundLowerBoundary = false;
						boolean foundUpperBoundary = false;

						String TV_RANGE_MIN = "rangeMinimum";
						String TV_RANGE_MAX = "rangeMaximum";

						String rMin = pi.taggedValue(TV_RANGE_MIN);
						String rMax = pi.taggedValue(TV_RANGE_MAX);

						if (StringUtils.isNotBlank(rMin)) {

							try {
								lowerBoundaryValue = Double
										.parseDouble(rMin.trim());
								foundLowerBoundary = true;
							} catch (NumberFormatException e) {
								MessageContext mc = result.addWarning(this, 36,
										rMin.trim(), TV_RANGE_MIN);
								mc.addDetail(this, 100, pi.fullNameInSchema());
							}
						}

						if (StringUtils.isNotBlank(rMax)) {

							try {
								upperBoundaryValue = Double
										.parseDouble(rMax.trim());
								foundUpperBoundary = true;
							} catch (NumberFormatException e) {
								MessageContext mc = result.addWarning(this, 36,
										rMax.trim(), TV_RANGE_MAX);
								mc.addDetail(this, 100, pi.fullNameInSchema());
							}
						}

						if (foundLowerBoundary || foundUpperBoundary) {

							alterTableAddCheckConstraintForRange(col,
									lowerBoundaryValue, upperBoundaryValue);
						}
					}
				}

				ClassInfo enumerationValueType = col.getEnumerationValueType();
				if (enumerationValueType != null) {
					alterTableAddCheckConstraintForEnumerationValueType(col,
							enumerationValueType);
				}
			}
		}

		// -------------------------------------------------
		// Create alter statements to add unique constraints
		// -------------------------------------------------
		for (CreateTable ct : this.createTableStatements) {

			Table table = ct.getTable();

			for (Column col : table.getColumns()) {

				PropertyInfo pi = col.getRepresentedProperty();

				if (pi != null) {

					if (pi.matches(
							SqlConstants.RULE_TGT_SQL_PROP_UNIQUE_CONSTRAINTS)
							&& "true".equalsIgnoreCase(
									pi.taggedValue("sqlUnique"))) {

						if (pi.cardinality().maxOccurs > 1) {
							result.addWarning(this, 33, col.getName(),
									table.getName());
						} else {

							String constraintName = namingScheme
									.nameForUniqueConstraint(
											SqlUtil.determineName(table,
													SqlDdl.constraintNameUsingShortName),
											SqlUtil.determineName(col,
													SqlDdl.constraintNameUsingShortName));

							Alter alter = alterTableAddUniqueConstraint(table,
									constraintName, col);
							uniqueConstraints.add(alter);
						}
					}
				}
			}
		}

		// -------------------------------------------------------
		// Create alter statements to add foreign key constraints
		// -------------------------------------------------------
		// NOTE: order is important, since naming scheme is involved
		// which may adjust constraint names to make them unique
		if (SqlDdl.createReferences) {

			// Process in order of table and column names
			List<CreateTable> cts = new ArrayList<CreateTable>(
					this.createTableStatements);
			Collections.sort(cts, CREATE_TABLE_COMPARATOR);

			for (CreateTable ct : cts) {

				Table t = ct.getTable();

				List<Column> columns = new ArrayList<Column>(t.getColumns());
				Collections.sort(columns, COLUMN_DEFINITION_COMPARATOR);

				for (Column cd : columns) {

					if (cd.getReferencedTable() != null) {

						Table t_main = cd.getInTable();

						Alter alter = alterTableAddForeignKeyConstraint(t_main,
								namingScheme.nameForForeignKeyConstraint(
										SqlUtil.determineName(t_main,
												SqlDdl.constraintNameUsingShortName),
										SqlUtil.determineName(cd,
												SqlDdl.constraintNameUsingShortName),
										SqlUtil.determineName(
												cd.getReferencedTable(),
												SqlDdl.constraintNameUsingShortName)),
								cd, cd.getReferencedTable());

						foreignKeyConstraints.add(alter);
					}
				}
			}
		}

		// ----------------------------------------
		// Create geometryMetadataUpdateStatement
		// ----------------------------------------

		for (CreateTable ct : this.createTableStatements) {

			Table t = ct.getTable();

			for (Column col : t.getColumns()) {

				PropertyInfo pi = col.getRepresentedProperty();

				if (pi != null) {

					if (isGeometryTypedProperty(pi)) {

						Statement stmt = SqlDdl.databaseStrategy
								.geometryMetadataUpdateStatement(t, col,
										SqlDdl.srid);

						if (stmt != null) {
							this.geometryMetadataUpdateStatements.add(stmt);
						}
					}
				}
			}
		}

		// ----------------------------------------
		// Create geometry indexes
		// ----------------------------------------

		for (CreateTable ct : this.createTableStatements) {

			Table t = ct.getTable();

			for (Column col : t.getColumns()) {

				PropertyInfo pi = col.getRepresentedProperty();

				if (pi != null) {

					if (isGeometryTypedProperty(pi)) {

						Statement stmt = this.generateGeometryIndex(t, col, pi);

						if (stmt != null) {
							this.geometryIndexStatements.add(stmt);
						}
					}
				}
			}
		}

		// -------------------------
		// create INSERT statements
		// -------------------------
		for (CreateTable ct : this.createTableStatements) {

			Table t = ct.getTable();
			ClassInfo representedClass = t.getRepresentedClass();

			if (representedClass != null
					&& representedClass.category() == Options.CODELIST
					&& representedClass
							.matches(SqlConstants.RULE_TGT_SQL_CLS_CODELISTS)) {

				List<Insert> insertStatements = new ArrayList<Insert>();

				for (PropertyInfo codePi : representedClass.properties()
						.values()) {

					if (!SqlDdl.isEncoded(codePi)) {
						continue;
					}

					Insert ins = new Insert();
					ins.setTable(ct.getTable());
					insertStatements.add(ins);

					ins.setColumns(ct.getTable().getColumns());

					ExpressionList el = new ExpressionList();
					List<Expression> values = new ArrayList<Expression>();
					el.setExpressions(values);
					ins.setExpressionList(el);

					// now add all values
					String codeName = codePi.name();
					if (codePi.initialValue() != null) {
						codeName = codePi.initialValue();
					}
					codeName = codeName.replaceAll("'", "''");

					if (isNumericallyValued(representedClass)) {
						values.add(new UnquotedStringExpression(codeName));
					} else {
						values.add(new StringValueExpression(codeName));
					}

					for (DescriptorForCodeList descriptor : SqlDdl.descriptorsForCodelist) {

						String descName = descriptor.getDescriptorName();
						String value = null;

						if (descName.equalsIgnoreCase("name")) {

							value = codePi.name();

						} else if (descName.equalsIgnoreCase("documentation")) {

							value = codePi.derivedDocumentation(
									SqlDdl.documentationTemplate,
									SqlDdl.documentationNoValue);

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

						} else if (descName
								.equalsIgnoreCase("dataCaptureStatement")) {

							String[] dcss = codePi.dataCaptureStatements();
							if (dcss != null && dcss.length > 0) {
								value = StringUtils.join(dcss, " ");
							}

						} else if (descName.equalsIgnoreCase("primaryCode")) {

							value = codePi.primaryCode();

						} else if (descName
								.equalsIgnoreCase("globalIdentifier")) {

							value = codePi.globalIdentifier();
						}

						if (value == null) {

							values.add(new NullValueExpression());

						} else {

							String valueWithEscapedQuotes = value
									.replaceAll("'", "''");

							values.add(new StringValueExpression(
									valueWithEscapedQuotes));
						}
					}

					if (representedClass.matches(
							SqlConstants.RULE_TGT_SQL_CLS_CODELISTS_PODS)
							&& !(t.representsCodeStatusCLType())) {
						values.add(new StringValueExpression("valid"));
						values.add(new NullValueExpression());
						values.add(new NullValueExpression());
					}
				}

				this.insertStatements.addAll(insertStatements);
			}
		}

		// -------------
		// Build result
		// -------------
		List<Statement> result = this.statements();

		// normalize names
		this.namingScheme.getNameNormalizer().visit(result);

		Collections.sort(result, STATEMENT_COMPARATOR);

		return result;
	}

	private void alterTableAddCheckConstraintForRange(Column column,
			Double lowerBound, Double upperBound) {

		Table tableWithColumn = column.getInTable();

		String constraintName = namingScheme.nameForCheckConstraint(
				SqlUtil.determineName(tableWithColumn,
						SqlDdl.constraintNameUsingShortName),
				SqlUtil.determineName(column,
						SqlDdl.constraintNameUsingShortName));

		Alter alter = new Alter();
		alter.setTable(tableWithColumn);

		ConstraintAlterExpression cae = new ConstraintAlterExpression();
		alter.setExpression(cae);

		cae.setOperation(AlterOperation.ADD);

		CheckConstraint cc = new CheckConstraint();
		cae.setConstraint(cc);

		cc.setName(constraintName);

		BetweenExpression bexp = new BetweenExpression();

		ColumnExpression col = new ColumnExpression(column);
		bexp.setTestExpression(col);

		DoubleValueExpression lowerExp = new DoubleValueExpression(lowerBound);
		bexp.setBeginExpression(lowerExp);

		DoubleValueExpression upperExp = new DoubleValueExpression(upperBound);
		bexp.setEndExpression(upperExp);

		if (column.isNotNull()) {
			cc.setExpression(bexp);
		} else {
			// add null check
			IsNullExpression nullexp = new IsNullExpression();
			nullexp.setExpression(col);

			OrExpression orexp = new OrExpression(nullexp, bexp);
			cc.setExpression(orexp);
		}

		this.checkConstraints.add(alter);
	}

	private Alter alterTableAddUniqueConstraint(Table tableWithColumn,
			String uniqueConstraintIdentifier, Column column) {

		Alter alter = new Alter();
		alter.setTable(tableWithColumn);

		ConstraintAlterExpression cae = new ConstraintAlterExpression();
		alter.setExpression(cae);

		cae.setOperation(AlterOperation.ADD);

		UniqueConstraint uc = new UniqueConstraint();
		cae.setConstraint(uc);

		uc.setName(uniqueConstraintIdentifier);

		uc.addColumn(column);

		return alter;
	}

	private void checkRequirements(List<ClassInfo> cisToProcess) {

		/*
		 * TODO Checking requirements on an input model should be a common
		 * pre-processing routine for targets and transformations
		 */

		for (ClassInfo ci : cisToProcess) {

			/*
			 * If rule for using <<identifier>> stereotype on attributes is
			 * enabled, check that a type does not have more than one such
			 * attribute, and that such an attribute has max cardinality 1.
			 */
			if (ci.matches(
					SqlConstants.RULE_TGT_SQL_CLS_IDENTIFIER_STEREOTYPE)) {

				int countIdentifierAttributes = 0;

				for (PropertyInfo pi : ci.properties().values()) {

					if (pi.isAttribute() && pi.stereotype("identifier")) {

						countIdentifierAttributes++;

						if (pi.cardinality().maxOccurs > 1) {
							MessageContext mc = result.addError(this, 25,
									pi.name());
							if (mc != null) {
								mc.addDetail(this, 100, pi.fullNameInSchema());
							}
						}
					}
				}

				if (countIdentifierAttributes > 1) {

					MessageContext mc = result.addWarning(this, 24, ci.name());
					if (mc != null) {
						mc.addDetail(this, 101, ci.fullNameInSchema());
					}
				}
			}
		}
	}

	private List<Statement> statements() {

		List<Statement> stmts = new ArrayList<Statement>();

		stmts.addAll(this.createTableStatements);
		stmts.addAll(this.checkConstraints);
		stmts.addAll(this.foreignKeyConstraints);
		stmts.addAll(this.uniqueConstraints);
		stmts.addAll(this.geometryMetadataUpdateStatements);
		stmts.addAll(this.geometryIndexStatements);
		stmts.addAll(this.insertStatements);
		stmts.addAll(this.commentStatements);

		return stmts;
	}

	private Alter alterTableAddForeignKeyConstraint(Table t_main,
			String foreignKeyIdentifier, Column column, Table referenceTable) {

		Alter alter = new Alter();
		alter.setTable(t_main);

		ConstraintAlterExpression cae = new ConstraintAlterExpression();
		alter.setExpression(cae);

		cae.setOperation(AlterOperation.ADD);

		ForeignKeyConstraint fkc = new ForeignKeyConstraint(
				foreignKeyIdentifier, referenceTable);
		cae.setConstraint(fkc);

		fkc.addColumn(column);

		// set foreign key options if relevant
		PropertyInfo representedPi = column.getRepresentedProperty();
		if (representedPi != null) {

			Info relevantInfo = representedPi;

			// first check tagged values on the property itself
			String tvOnDelete = relevantInfo.taggedValue("sqlOnDelete");
			String tvOnUpdate = relevantInfo.taggedValue("sqlOnUpdate");

			/*
			 * if the property is an association role and does not define any
			 * option, check if the association defines an option
			 */
			if (!representedPi.isAttribute() && StringUtils.isBlank(tvOnDelete)
					&& StringUtils.isBlank(tvOnUpdate)) {

				relevantInfo = representedPi.association();
				tvOnDelete = relevantInfo.taggedValue("sqlOnDelete");
				tvOnUpdate = relevantInfo.taggedValue("sqlOnUpdate");
			}

			setForeignKeyOption(true, tvOnDelete, fkc, column, relevantInfo);
			setForeignKeyOption(false, tvOnUpdate, fkc, column, relevantInfo);
		}

		return alter;
	}

	/**
	 * @param isOnDelete
	 *                         <code>true</code> if the option is for the 'ON
	 *                         DELETE' clause, <code>false</code> if it is for
	 *                         the 'ON UPDATE' clause.
	 * @param optionValue
	 *                         String value for the option; can be
	 *                         <code>null</code> (then this method has no
	 *                         effect)
	 * @param fkc
	 *                         the constraint on which the option would be set
	 * @param column
	 *                         the column to which the constraint applies,
	 *                         relevant for validity checks on the option
	 * @param relevantInfo
	 *                         the model element that defines the option via
	 *                         tagged value, relevant for log messages
	 */
	private void setForeignKeyOption(boolean isOnDelete, String optionValue,
			ForeignKeyConstraint fkc, Column column, Info relevantInfo) {

		if (StringUtils.isNotBlank(optionValue)) {

			Table table = column.getInTable();

			try {

				ForeignKeyConstraint.Option o = ForeignKeyConstraint.Option
						.fromString(optionValue);

				if ((isOnDelete && SqlDdl.databaseStrategy
						.isForeignKeyOnDeleteOptionSupported(o))
						|| (!isOnDelete && SqlDdl.databaseStrategy
								.isForeignKeyOnUpdateOptionSupported(o))) {

					/*
					 * Check that the foreign key option is applicable to the
					 * given column. At the moment, this includes checking that
					 * the option is not 'SET NULL' in case that the column is
					 * 'NOT NULL'.
					 */
					boolean isValid = true;

					if (o == ForeignKeyConstraint.Option.SET_NULL
							&& column.isNotNull()) {

						isValid = false;

						MessageContext mc = result.addWarning(this, 37,
								column.getName());
						if (relevantInfo != null && mc != null) {
							mc.addDetail(this, 102, table.getName(),
									column.getName());
							mc.addDetail(this, 38,
									relevantInfo.fullNameInSchema());
						}
					}

					if (isValid) {
						if (isOnDelete) {
							fkc.setOnDelete(o);
						} else {
							fkc.setOnUpdate(o);
						}
					}

				} else {
					MessageContext mc = result.addInfo(this, 35, o.toString(),
							isOnDelete ? "sqlOnDelete" : "sqlOnUpdate",
							isOnDelete ? "ON DELETE" : "ON UPDATE");
					if (mc != null) {
						mc.addDetail(this, 102, table.getName(),
								column.getName());
						mc.addDetail(this, 38, relevantInfo.fullNameInSchema());
					}
				}

			} catch (IllegalArgumentException e) {
				MessageContext mc = result.addError(this, 34, optionValue,
						isOnDelete ? "sqlOnDelete" : "sqlOnUpdate");
				if (mc != null) {
					mc.addDetail(this, 102, table.getName(), column.getName());
					mc.addDetail(this, 38, relevantInfo.fullNameInSchema());
				}
			}
		}

	}

	/**
	 * @param pi
	 *               property, may be <code>null</code>
	 * @return the size that is applicable for the property - or
	 *         <code>null</code> if the property is not character valued or has
	 *         no specific size set
	 */
	public Integer getSizeForCharacterValuedProperty(PropertyInfo pi) {
		return pi == null ? null : sizeByCharacterValuedProperty.get(pi);
	}

	public boolean isForeignKeyField(PropertyInfo pi) {
		return refersToTypeRepresentedByTable(pi);
	}

	/**
	 * Look up the table with the given name. If no such table exists, a new one
	 * is created (this is logged on debug level) and returned.
	 * 
	 * @param tableName
	 *                      name of the table to look up, must not be
	 *                      <code>null</code>
	 * @return
	 */
	private Table map(String tableName) {

		for (Table t : this.tables) {
			if (tableName.equals(t.getName())) {
				return t;
			}
		}

		result.addDebug(this, 23, tableName);
		Table t = new Table(tableName);
		this.tables.add(t);
		return t;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.MessageSource#message(int)
	 */
	public String message(int mnr) {

		switch (mnr) {
		case 0:
			return "Context: class SqlBuilder";
		case 1:
			return "Context: class '$1$'";
		case 2:
			return "Context: property '$1$'";
		case 3:
			return "Context: '$1$'";

		case 5:
			return "Number format exception while converting the tagged value '$1$' to an integer. Exception message: $2$. Using $3$ as default value.";
		case 6:
			return "??Number format exception while converting the tagged value '$1$' with value '$2$' to an integer.";

		case 8:
			return "??Many-to-many relationship represented by association between types with identity and maximum multiplicity > 1 on all navigable ends (in this case for classes: '$1$' [context is property '$2$'] <-> '$3$' [context is property '$4$']) is only supported if creation for associative tables is enabled (via inclusion of rule "
					+ SqlConstants.RULE_TGT_SQL_ALL_ASSOCIATIVETABLES
					+ "). Because the rule is not included, the relationship will be ignored.";
		case 9:
			return "Type '$1$' of property '$2$' in class '$3$' is not part of the schema that is being processed, no map entry is defined for it, and "
					+ SqlConstants.RULE_TGT_SQL_CLS_REFERENCES_TO_EXTERNAL_TYPES
					+ " is not enabled. Please ensure that map entries are defined for external types used in the schema - or allow referencing of external types in general by enabling "
					+ SqlConstants.RULE_TGT_SQL_CLS_REFERENCES_TO_EXTERNAL_TYPES
					+ ". Assigning textual type to the property.";
		case 10:
			return "Type '$1$' of property '$2$' in class '$3$' could not be found in the model. Assigning textual type to the property.";
		case 11:
			return "Attribute '$1$' in class '$2$' has maximum multiplicity greater than one. Creation of associative tables is not enabled. The property will thus be ignored.";
		case 12:
			return "Creating associative table to represent attribute '$1$' in class '$2$'. Tagged value '"
					+ SqlConstants.TV_ASSOCIATIVETABLE
					+ "' not set on this attribute, thus using default naming pattern, which leads to table name: '$3$'.";
		case 13:
			return "Creating associative table to represent association between $1$ and $2$. Tagged value '"
					+ SqlConstants.TV_ASSOCIATIVETABLE
					+ "' not set on this association, thus using default naming pattern, which leads to table name: '$3$'.";
		case 14:
			return "??Derived property '$1$' in class '$2$' has been ignored.";
		case 15:
			return "??Property '$1$' in class '$2$' is not encoded.";
		case 16:
			return "??The type of property '$1$' in class '$2$' is '$3$'. It is contained in the schema that is being processed. However, it is of a category not enabled for conversion, meaning that no table will be created to represent the type '$3$'. The property '$1$' in class '$2$' will therefore be ignored.";
		case 17:
			return "Could not find type '$1$' in the model. It was required to identify the correct map entry that applies for this type (based upon the encoding rule that applies to the type), when trying to determine if property '$2$' (that has this type) is a geometry typed property. Proceeding with the map entry that is retrieved when using the encoding rule that applies to the property.";
		case 18:
			return "Could not find enumeration '$1$' in the model - or no enum values defined for it. Check constraint for '$2$' will not be created.";
		case 19:
			return "Class $1$ is not encoded and no map entry (that applies in the SQL encoding) is defined for it. The relationship between class $1$ (context property is $2$) and class $3$ (context property is $4$), which in the model is defined via an association, thus does not exist in the SQL encoding.";
		case 20:
			return "??More than eleven occurrences of foreign key '$1$'. Resulting schema will be ambiguous.";
		case 21:
			return "?? The type '$1$' was not found in the schema(s) selected for processing or in map entries. It will be mapped to 'unknown'.";
		case 22:
			return "An association exists between class $1$ (context property is $2$) and class $3$ (context property is $4$). The association represents a 1:n relationship, which would be encoded by adding a foreign key field to the table representing $1$. A map entry is defined for $1$. Thus, the table defined in that map entry, which represents $1$, should have a foreign key field to reference the table that represents $3$.";
		case 23:
			return "Creating table with name '$1$'";
		case 24:
			return "Multiple attributes with stereotype <<identifier>> found for class '$1$'. The first - arbitrary one - will be set as primary key.";
		case 25:
			return "Identifier attribute '$1$' has max multiplicity > 1.";
		case 26:
			return "Type '$1$' is configured to be used as conceptual type of the '$2$' column in table '$3$' (which represents a code list). However, the type could not be found in the model. The column will have the common data type for foreign keys (defined via the configuration). No specific constraints will be created for the $2$ column.";
		case 27:
			return "??Tagged value 'scale' is not blank (i.e., it is defined and not whitespace only), while tagged value 'precision' is blank. Scale cannot be defined without precision. Tagged value 'scale' will be ignored.";
		case 28:
			return "Type '$1$' is numerically valued. However, the numeric type could not be determined. Check tagged value '"
					+ SqlConstants.TV_NUMERIC_TYPE
					+ "' on the type and that an appropriate map entry (with valid target type) exists for it in the configuration.";
		case 29:
			return "Type '$1$' of property '$2$' is numerically valued. However, the numeric type could not be determined. Check tagged value '"
					+ SqlConstants.TV_NUMERIC_TYPE
					+ "' on the type and that an appropriate map entry (with valid target type) exists for it in the configuration.";
		case 30:
			return "Type '$1$' is configured to be used as conceptual type of the '$2$' column in table '$3$' (which represents a code list). However, the type is neither an enumeration nor represented by a table. The column will have the common data type for foreign keys (defined via the configuration). No specific constraints will be created for the $2$ column.";
		case 31:
			return "Type '$1$' - which is the conceptual type of '$2$' column in table '$3$' (which represents a code list) - is numerically valued. However, the numeric type could not be determined. Check tagged value '"
					+ SqlConstants.TV_NUMERIC_TYPE
					+ "' on the type and that an appropriate map entry (with valid target type) exists for it in the configuration.";
		case 32:
			return "No enum values defined for enumeration '$1$'. Check constraint for column '$2$' in table '$3$' will not be created.";
		case 33:
			return "No unique constraint is created for column '$1$' of table '$2$', since the property represented by the column is multi-valued.";
		case 34:
			return "Foreign key constraint option '$1$' defined by tagged value '$2$' is unknown. The option is ignored.";
		case 35:
			return "Foreign key constraint option '$1$' is defined by tagged value '$2$'. The database system does not support this option for clause '$3$'. The option is ignored.";
		case 36:
			return "Could not parse value '$1$' of tag '$2$' to a double value. The tagged value will be ignored.";
		case 37:
			return "Foreign key option is 'SET NULL', but column '$1$' is 'NOT NULL'. The foreign key option is ignored.";
		case 38:
			return "Model element that defines the option: '$1$'";

		case 100:
			return "Context: property '$1$'.";
		case 101:
			return "Context: class '$1$'.";
		case 102:
			return "Context: table '$1$', column '$2$'.";
		default:
			return "(" + SqlBuilder.class.getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
