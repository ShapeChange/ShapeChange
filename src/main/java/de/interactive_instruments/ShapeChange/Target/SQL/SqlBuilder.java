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
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.AssociationInfo;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.SQL.expressions.ColumnExpression;
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
import de.interactive_instruments.ShapeChange.Target.SQL.structure.ConstraintAlterExpression;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.CreateTable;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.ForeignKeyConstraint;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Insert;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.PrimaryKeyConstraint;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Statement;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Table;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;

/**
 * Builds SQL statements for model elements.
 *
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class SqlBuilder implements MessageSource {

	private static Comparator<Statement> STATEMENT_COMPARATOR = new StatementSortAlphabetic();
	private static Comparator<CreateTable> CREATE_TABLE_COMPARATOR = new CreateTableSortAlphabetic();
	private static Comparator<Column> COLUMN_DEFINITION_COMPARATOR = new ColumnSortAlphabetic();

	private SqlDdl sqlddl;
	private ShapeChangeResult result;
	private Options options;
	private Model model;

	private Pattern pattern_find_true = Pattern.compile("true",
			Pattern.CASE_INSENSITIVE);
	private Pattern pattern_find_false = Pattern.compile("false",
			Pattern.CASE_INSENSITIVE);

	private Map<PropertyInfo, Integer> sizeByCharacterValuedProperty = new HashMap<PropertyInfo, Integer>();

	/**
	 * NOTE: Used to store the relationship between a column in an associative
	 * table and the 'normal' table the field references.
	 * <p>
	 * key: A column
	 * <p>
	 * value: Either the ClassInfo that is represented by the table the column
	 * references, or it is the PropertyInfo represented by the column.
	 */
	private Map<Column, Info> classOrPropertyInfoByColumnInAssociativeTable = new HashMap<Column, Info>();

	/**
	 * Required for adding PODS specific columns to tables that represent code
	 * lists.
	 * 
	 * Key: table that represents a code list Key: column that represents the
	 * code name in that table
	 */
	private Map<CreateTable, Column> codeNameColumnByCreateTable = new HashMap<CreateTable, Column>();

	private List<CreateTable> createTableStatements = new ArrayList<CreateTable>();
	private List<Alter> foreignKeyConstraints = new ArrayList<Alter>();
	private List<Alter> checkConstraints = new ArrayList<Alter>();
	private List<Statement> geometryMetadataUpdateStatements = new ArrayList<Statement>();
	private List<Statement> geometryIndexStatements = new ArrayList<Statement>();
	private List<Insert> insertStatements = new ArrayList<Insert>();

	private SqlNamingScheme namingScheme;

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
	private void createAssociativeTableForAttribute(PropertyInfo pi) {

		if (!pi.isAttribute()) {
			return;
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

		Table table = new Table(tableName);
		createTable.setTable(table);

		table.setAssociativeTable(true);
		table.setRepresentedProperty(pi);

		List<Column> Columns = new ArrayList<Column>();
		table.setColumns(Columns);

		/*
		 * Add field to reference pi.inClass
		 * 
		 * NOTE: the primary key for the table will be defined later
		 */
		String classReferenceFieldName = pi.inClass().name()
				+ sqlddl.getIdColumnName();
		Column cdInClassReference = createColumn(table, null,
				classReferenceFieldName, sqlddl.getForeignKeyColumnDataType(),
				"NOT NULL", false);
		Columns.add(cdInClassReference);

		classOrPropertyInfoByColumnInAssociativeTable.put(cdInClassReference,
				pi.inClass());

		Column cdPi;

		if (refersToTypeRepresentedByTable(pi)) {

			String piFieldName = determineTableNameForValueType(pi)
					+ sqlddl.getIdColumnName();

			String fieldType;
			if (pi.categoryOfValue() == Options.CODELIST && pi.inClass()
					.matches(SqlConstants.RULE_TGT_SQL_CLS_CODELISTS)) {

				if (sqlddl.getCodeNameSize() < 1) {
					fieldType = sqlddl.getDatabaseStrategy()
							.unlimitedLengthCharacterDataType();
				} else {
					fieldType = sqlddl.getDatabaseStrategy()
							.limitedLengthCharacterDataType(
									sqlddl.getCodeNameSize());
				}

			} else {

				fieldType = sqlddl.getForeignKeyColumnDataType();
			}

			cdPi = createColumn(table, pi, piFieldName, fieldType, "NOT NULL",
					false);
			this.classOrPropertyInfoByColumnInAssociativeTable.put(cdPi, pi);

		} else {

			cdPi = createColumn(table, pi, true);
		}

		Columns.add(cdPi);
	}

	/**
	 * Will create a table to represent the given class. Will also create
	 * associative tables, as applicable.
	 * 
	 * @param ci
	 */
	private void createTables(ClassInfo ci) {

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
					&& typeCi.inSchema(sqlddl.getSchema())
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
				continue;
			}

			/*
			 * TODO Ignore property if it is a data type and the TBD rule to
			 * ensure one-to-many relationship for data types is enabled. In
			 * such a case, each table created for the data type (for each case
			 * in which a class has an attribute with that data type as value
			 * type) contains one (or more) field(s) to reference a table that
			 * represents a type (feature type, but also other data type) in
			 * which the data type is used as value type of a property.
			 */

			if (pi.isAttribute()) {

				if (pi.cardinality().maxOccurs == 1) {

					propertyInfosForColumns.add(pi);

				} else if (sqlddl.isCreateAssociativeTables()) {

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
				if (tableForAssociationExists(ai)) {
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

					if (revPi.isNavigable() && SqlDdl.isEncoded(revPi)
							&& maxOccursRevPi == 1) {

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

						if (sqlddl.isCreateAssociativeTables()) {

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

		Table table = new Table(ci.name());
		createTable.setTable(table);

		table.setRepresentedClass(ci);

		List<Column> Columns = new ArrayList<Column>();

		// Add object identifier column
		Column id_cd = createColumn(table, null, sqlddl.getIdColumnName(),
				sqlddl.getDatabaseStrategy().primaryKeyDataType(),
				sqlddl.getPrimaryKeyColumnSpec(), true);
		Columns.add(id_cd);
		id_cd.setObjectIdentifierColumn(true);

		for (PropertyInfo pi : propertyInfosForColumns) {

			Column cd = createColumn(table, pi, false);
			Columns.add(cd);
		}

		table.setColumns(Columns);
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
	private void createAssociativeTable(AssociationInfo ai) {

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

		Table table = new Table(tableName);
		createTable.setTable(table);

		table.setAssociativeTable(true);
		table.setRepresentedAssociation(ai);

		List<Column> Columns = new ArrayList<Column>();
		table.setColumns(Columns);

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
				+ (reflexive ? "_" + pi1.name() : "")
				+ sqlddl.getIdColumnName();
		Column cd1 = createColumn(table, pi2, name_1,
				sqlddl.getForeignKeyColumnDataType(), "NOT NULL", false);
		Columns.add(cd1);

		// add field for second reference
		String name_2 = determineTableNameForType(pi2.inClass())
				+ (reflexive ? "_" + pi2.name() : "")
				+ sqlddl.getIdColumnName();
		Column cd2 = createColumn(table, pi1, name_2,
				sqlddl.getForeignKeyColumnDataType(), "NOT NULL", false);
		Columns.add(cd2);

		this.classOrPropertyInfoByColumnInAssociativeTable.put(cd1,
				pi1.inClass());
		this.classOrPropertyInfoByColumnInAssociativeTable.put(cd2,
				pi2.inClass());
	}

	/**
	 * @param ci
	 */
	private void createTableForCodeList(ClassInfo ci) {

		CreateTable createTable = new CreateTable();
		this.createTableStatements.add(createTable);

		Table table = new Table(ci.name());
		createTable.setTable(table);

		table.setRepresentedClass(ci);

		// --- create the columns for codes
		List<Column> Columns = new ArrayList<Column>();
		table.setColumns(Columns);

		// create required column to store the code name
		String name = sqlddl.getCodeNameColumnName();
		String fieldType;

		if (sqlddl.getCodeNameSize() < 1) {
			fieldType = sqlddl.getDatabaseStrategy()
					.unlimitedLengthCharacterDataType();
		} else {
			fieldType = sqlddl.getDatabaseStrategy()
					.limitedLengthCharacterDataType(sqlddl.getCodeNameSize());
		}

		Column cd_codename = createColumn(table, null, name, fieldType,
				"NOT NULL",
				!ci.matches(SqlConstants.RULE_TGT_SQL_CLS_CODELISTS_PODS));
		Columns.add(cd_codename);

		// keep track of code name column to add PODS specifics later on
		if (ci.matches(SqlConstants.RULE_TGT_SQL_CLS_CODELISTS_PODS)) {
			codeNameColumnByCreateTable.put(createTable, cd_codename);
		}

		/*
		 * now add one column for each descriptor, as specified via the
		 * configuration
		 */
		for (DescriptorForCodeList descriptor : sqlddl
				.getDescriptorsForCodelist()) {

			String descriptor_fieldType;
			if (descriptor.getSize() == null) {
				descriptor_fieldType = sqlddl.getDatabaseStrategy()
						.unlimitedLengthCharacterDataType();
			} else {
				descriptor_fieldType = sqlddl.getDatabaseStrategy()
						.limitedLengthCharacterDataType(descriptor.getSize());
			}

			Column cd_descriptor = createColumn(table, null,
					descriptor.getColumnName(), descriptor_fieldType, "",
					false);
			Columns.add(cd_descriptor);
		}
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

		if (pme != null && sqlddl.getMapEntryParamInfos().hasParameter(pme,
				SqlConstants.ME_PARAM_TABLE)) {

			return pme.getTargetType();

		} else {

			for (CreateTable ct : this.createTableStatements) {
				if (ct.getTable().representsClass(ci)) {
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
			Column column, PropertyInfo pi) {

		/*
		 * ignore the constraint if a type mapping exists for the value type of
		 * pi
		 */
		ProcessMapEntry pme = options.targetMapEntry(pi.typeInfo().name,
				pi.encodingRule("sql"));

		if (pme != null) {
			return;
		}

		Table tableWithColumn = column.getInTable();

		// look up the enumeration type
		ClassInfo enumCi = model.classById(pi.typeInfo().id);

		if (enumCi == null || enumCi.properties().size() == 0) {

			result.addError(this, 18, pi.typeInfo().name,
					pi.fullNameInSchema());
		} else {

			String constraintName = namingScheme.nameForCheckConstraint(
					tableWithColumn.getName(), pi.name());

			Alter alter = new Alter();
			alter.setTable(tableWithColumn);

			ConstraintAlterExpression cae = new ConstraintAlterExpression();
			alter.setExpression(cae);

			cae.setOperation(AlterOperation.ADD);

			CheckConstraint cc = new CheckConstraint();
			cae.setConstraint(cc);

			cc.setName(constraintName);

			InExpression iexp = new InExpression();
			cc.setExpression(iexp);

			ColumnExpression col = new ColumnExpression(column);
			iexp.setLeftExpression(col);

			ExpressionList el = new ExpressionList();
			List<Expression> el_tmp = new ArrayList<Expression>();
			el.setExpressions(el_tmp);

			for (PropertyInfo enumPi : enumCi.properties().values()) {

				if (!SqlDdl.isEncoded(pi)) {
					continue;
				}

				String value = enumPi.name();
				if (enumPi.initialValue() != null) {
					value = enumPi.initialValue();
				}

				// escape single quotes in the enumeration value
				value = StringUtils.replace(value, "'", "''");

				StringValueExpression sv = new StringValueExpression(value);

				el_tmp.add(sv);
			}

			iexp.setRightExpressionsList(el);

			this.checkConstraints.add(alter);
		}
	}

	private void alterTableAddCheckConstraintToRestrictTimeOfDate(Column column,
			PropertyInfo pi) {

		Expression expr = sqlddl.getDatabaseStrategy()
				.expressionForCheckConstraintToRestrictTimeOfDate(pi, column);

		if (expr != null) {

			Table tableWithColumn = column.getInTable();

			String constraintName = namingScheme.nameForCheckConstraint(
					tableWithColumn.getName(), pi.name());

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

		return pme != null && sqlddl.getMapEntryParamInfos().hasParameter(
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

		if (pme != null && sqlddl.getMapEntryParamInfos().hasParameter(
				valueTypeName, piEncodingRule, SqlConstants.ME_PARAM_TABLE)) {

			return sqlddl.getMapEntryParamInfos()
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

		String valueTypeName = pi.typeInfo().name;
		String piEncodingRule = pi.encodingRule("sql");

		ProcessMapEntry pme = options.targetMapEntry(valueTypeName,
				piEncodingRule);

		if (pme != null) {

			if (sqlddl.getMapEntryParamInfos().hasParameter(valueTypeName,
					piEncodingRule, SqlConstants.ME_PARAM_TABLE)) {
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

				if ((pi.categoryOfValue() == Options.OBJECT && !typeCi
						.matches(SqlConstants.RULE_TGT_SQL_CLS_OBJECT_TYPES))
						|| (pi.categoryOfValue() == Options.FEATURE
								&& !typeCi.matches(
										SqlConstants.RULE_TGT_SQL_CLS_FEATURE_TYPES))
						|| (pi.categoryOfValue() == Options.DATATYPE
								&& !typeCi.matches(
										SqlConstants.RULE_TGT_SQL_CLS_DATATYPES))
						|| (pi.categoryOfValue() == Options.CODELIST
								&& !typeCi.matches(
										SqlConstants.RULE_TGT_SQL_CLS_CODELISTS))) {

					return false;

				} else {

					if (typeCi.inSchema(sqlddl.getSchema()) || typeCi.matches(
							SqlConstants.RULE_TGT_SQL_CLS_REFERENCES_TO_EXTERNAL_TYPES)) {

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

	private Column createColumn(Table inTable, PropertyInfo representedProperty,
			String name, String type, String columnSpecification,
			boolean isPrimaryKey) {

		Column column = new Column(name, representedProperty, inTable);

		ColumnDataType colDataType = new ColumnDataType(type);
		column.setDataType(colDataType);

		List<String> columnSpecStrings = new ArrayList<String>();

		if (columnSpecification != null
				&& !columnSpecification.trim().isEmpty()) {
			columnSpecStrings.add(columnSpecification.trim());
		}
		if (isPrimaryKey) {
			columnSpecStrings.add("PRIMARY KEY");
		}
		column.setSpecifications(columnSpecStrings);

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
	 *            <code>true</code> if the column definition shall be created
	 *            with NOT NULL, otherwise <code>false</code> (then default
	 *            behavior applies)
	 * @return The SQL statement with the definition of the column to represent
	 *         the property
	 */
	private Column createColumn(Table inTable, PropertyInfo pi,
			boolean alwaysNotNull) {

		String name;
		if (refersToTypeRepresentedByTable(pi)) {
			name = pi.name() + identifyForeignKeyColumnSuffix(pi);
		} else {
			name = pi.name();
		}
		Column cd = new Column(name, pi, inTable);

		String type = identifyType(pi);
		ColumnDataType colDataType = new ColumnDataType(type);
		cd.setDataType(colDataType);

		List<String> columnSpecStrings = new ArrayList<String>();

		String columnDefault = pi.initialValue();

		if (columnDefault != null && columnDefault.trim().length() > 0) {

			Expression defaultValue = null;

			String booleanTrue = "TRUE";
			String booleanFalse = "FALSE";
			boolean quoted = false;

			/*
			 * Check map entry parameter infos for any defaultValue
			 * characteristics defined for the value type of pi.
			 */
			Map<String, String> characteristics = sqlddl.getMapEntryParamInfos()
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
					quoted = characteristics
							.get(SqlConstants.ME_PARAM_DEFAULTVALUE_CHARACT_QUOTED)
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

				defaultValue = quoted ? new StringValueExpression(columnDefault)
						: new UnquotedStringExpression(columnDefault);
			}

			cd.setDefaultValue(defaultValue);
		}

		// ----- add constraints

		if (alwaysNotNull) {
			columnSpecStrings.add("NOT NULL");
		} else {
			// set NOT NULL if required
			if (pi.implementedByNilReason() || pi.nilReasonAllowed()
					|| pi.voidable() || pi.cardinality().minOccurs < 1) {
				/*
				 * in these cases the default behavior (that the field can be
				 * NULL) is ok
				 */
			} else {
				columnSpecStrings.add("NOT NULL");
			}
		}

		cd.setSpecifications(columnSpecStrings);

		return cd;
	}

	private String identifyForeignKeyColumnSuffix(PropertyInfo pi) {

		String typeName = pi.typeInfo().name;
		String piEncodingRule = pi.encodingRule("sql");

		ProcessMapEntry pme = options.targetMapEntry(typeName, piEncodingRule);

		if (pme != null && sqlddl.getMapEntryParamInfos().hasCharacteristic(
				typeName, piEncodingRule, SqlConstants.ME_PARAM_TABLE,
				SqlConstants.ME_PARAM_TABLE_CHARACT_REP_CAT)) {

			String repCat = sqlddl.getMapEntryParamInfos().getCharacteristic(
					typeName, piEncodingRule, SqlConstants.ME_PARAM_TABLE,
					SqlConstants.ME_PARAM_TABLE_CHARACT_REP_CAT);

			if (repCat != null && repCat.equalsIgnoreCase("datatype")) {
				return sqlddl.getForeignKeyColumnSuffixDatatype();
			} else {
				return sqlddl.getForeignKeyColumnSuffix();
			}

		} else if (pi.categoryOfValue() == Options.DATATYPE) {
			return sqlddl.getForeignKeyColumnSuffixDatatype();
		} else {
			return sqlddl.getForeignKeyColumnSuffix();
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

		// try to get type from map entries
		ProcessMapEntry me = options.targetMapEntry(pi.typeInfo().name,
				pi.encodingRule("sql"));

		if (me != null) {

			if (sqlddl.getMapEntryParamInfos().hasParameter(me,
					SqlConstants.ME_PARAM_GEOMETRY)) {

				return sqlddl.getDatabaseStrategy().geometryDataType(me,
						sqlddl.getSrid());

			} else if (sqlddl.getMapEntryParamInfos().hasParameter(me,
					SqlConstants.ME_PARAM_TABLE)) {

				return sqlddl.getForeignKeyColumnDataType();

			} else {

				if (me.getTargetType()
						.startsWith(SqlConstants.MAP_TARGETTYPE_COND_PART)) {

					String conditionalCriterium = me.getTargetType().substring(
							SqlConstants.MAP_TARGETTYPE_COND_PART.length());

					if (conditionalCriterium.equalsIgnoreCase(
							SqlConstants.MAP_TARGETTYPE_COND_TEXTORCHARACTERVARYING)) {
						return determineCharacterVaryingOrText(pi);
					}

				} else if (sqlddl.getMapEntryParamInfos().hasParameter(me,
						SqlConstants.ME_PARAM_TEXTORCHARACTERVARYING)) {

					return determineCharacterVaryingOrText(pi);

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

				if ((catOfValue == Options.OBJECT && !typeCi
						.matches(SqlConstants.RULE_TGT_SQL_CLS_OBJECT_TYPES))
						|| (catOfValue == Options.FEATURE && !typeCi.matches(
								SqlConstants.RULE_TGT_SQL_CLS_FEATURE_TYPES))
						|| (catOfValue == Options.DATATYPE && !typeCi.matches(
								SqlConstants.RULE_TGT_SQL_CLS_DATATYPES))
						|| (catOfValue == Options.CODELIST && !typeCi.matches(
								SqlConstants.RULE_TGT_SQL_CLS_CODELISTS))) {

					/*
					 * table creation for this category is not enabled -> assign
					 * textual type
					 */
					return determineCharacterVaryingOrText(pi);

				} else {

					if (typeCi.inSchema(sqlddl.getSchema()) || typeCi.matches(
							SqlConstants.RULE_TGT_SQL_CLS_REFERENCES_TO_EXTERNAL_TYPES)) {

						if (catOfValue == Options.CODELIST) {

							if (sqlddl.getCodeNameSize() < 1) {
								return sqlddl.getDatabaseStrategy()
										.unlimitedLengthCharacterDataType();
							} else {
								return sqlddl.getDatabaseStrategy()
										.limitedLengthCharacterDataType(
												sqlddl.getCodeNameSize());
							}

						} else {

							return sqlddl.getForeignKeyColumnDataType();
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

		// keep track of the result for use by the replication schema
		this.sizeByCharacterValuedProperty.put(pi, size);

		/*
		 * TODO let database strategies create actual data type with field for
		 * length? if we want to use a common textual data type (rather than
		 * replicating the database system specific types) we would need
		 * database system specific DDL writers (which would be a good thing to
		 * have); then the replication schema target could read the length
		 * limitiation of a textual data type directly from that data type, and
		 * the SqlBuilder would no longer need to keep track of size for
		 * character valued properties
		 */

		if (size < 1) {
			return sqlddl.getDatabaseStrategy()
					.unlimitedLengthCharacterDataType();
		} else {
			return sqlddl.getDatabaseStrategy()
					.limitedLengthCharacterDataType(size);
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

		int size = sqlddl.getDefaultSize();

		if (tvSize != null) {
			try {
				size = Integer.parseInt(tvSize);
			} catch (NumberFormatException e) {
				MessageContext mc = result.addWarning(this, 5,
						SqlConstants.PARAM_SIZE, e.getMessage(),
						"" + sqlddl.getDefaultSize());
				mc.addDetail(this, 0);
				mc.addDetail(this, 100, pi.name(), pi.inClass().name());
				size = sqlddl.getDefaultSize();
			}
		}

		return size;
	}

	/**
	 * Generates index creation statements for all geometry properties/columns
	 * contained in {@link #geometryPropsByTableName}. The statements are stored
	 * in an internal list ({@link #geometryIndexCreationStatements}).
	 */
	private Statement generateGeometryIndex(Table tableWithColumn,
			Column columnForProperty, PropertyInfo pi) {

		Map<String, String> geometryCharacteristics = sqlddl
				.getMapEntryParamInfos().getCharacteristics(pi.typeInfo().name,
						pi.encodingRule("sql"), SqlConstants.ME_PARAM_GEOMETRY);

		// TBD: UPDATE NAMING PATTERN?

		String indexName = "idx_" + tableWithColumn.getName() + "_"
				+ columnForProperty.getName();

		Statement result = sqlddl.getDatabaseStrategy().geometryIndexColumnPart(
				indexName, tableWithColumn, columnForProperty,
				geometryCharacteristics);

		return result;
	}

	public List<Statement> process(List<ClassInfo> cisToProcess) {

		// ----------------------------------------
		// Create tables ("normal" and associative)
		// ----------------------------------------
		for (ClassInfo ci : cisToProcess) {

			if (ci.category() == Options.CODELIST) {

				createTableForCodeList(ci);

			} else {

				/*
				 * TODO determine if ci is a data type; if it is and TBD rule to
				 * ensure one-to-many relationships for data types is enabled,
				 * screen all cisToProcess to identify properties that have ci
				 * as value type; then a specific table must be created for each
				 * such case.
				 */

				createTables(ci);
			}
		}

		// ----------------------------------------
		// normalize table and column names (alter
		// statements use the latter as literals)
		// ----------------------------------------
		List<Statement> statements = this.statements();
		Collections.sort(statements, STATEMENT_COMPARATOR);
		this.namingScheme.getNameNormalizer().visit(statements);

		// ----------------------------------------------------
		// Add PODS specifics to tables representing code lists
		// ----------------------------------------------------
		// NOTE: order of processing is irrelevant since we just add
		for (Entry<CreateTable, Column> entry : codeNameColumnByCreateTable
				.entrySet()) {

			addPodsSpecificsToCodelistTable(entry.getKey(), entry.getValue());
		}

		// ------------------------------------------
		// Create primary keys for associative tables
		// ------------------------------------------
		// NOTE: order of processing is irrelevant since we just add
		for (CreateTable ct : this.createTableStatements) {

			Table t = ct.getTable();

			if (t.isAssociativeTable()) {

				PrimaryKeyConstraint pkc = new PrimaryKeyConstraint();
				pkc.setColumns(t.getColumns());

				ct.getTable().addConstraint(pkc);
			}
		}

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

						alterTableAddCheckConstraintForEnumerationValueType(col,
								pi);
					}

					if (pi.typeInfo().name.equalsIgnoreCase("Date")
							&& pi.matches(
									SqlConstants.RULE_TGT_SQL_PROP_CHECK_CONSTRAINT_RESTRICT_TIME_OF_DATE)) {

						alterTableAddCheckConstraintToRestrictTimeOfDate(col,
								pi);
					}
				}
			}
		}

		// -------------------------------------------------------
		// Create alter statements to add foreign key constraints
		// -------------------------------------------------------
		// NOTE: order is important, since naming scheme is involved
		// which may adjust constraint names to make them unique
		if (sqlddl.isCreateReferences()) {

			// Process in order of table and column names
			List<CreateTable> cts = new ArrayList<CreateTable>(
					this.createTableStatements);
			Collections.sort(cts, CREATE_TABLE_COMPARATOR);

			for (CreateTable ct : cts) {

				Table t = ct.getTable();

				List<Column> columns = new ArrayList<Column>(t.getColumns());
				Collections.sort(columns, COLUMN_DEFINITION_COMPARATOR);

				if (t.isAssociativeTable()) {

					/*
					 * Create foreign keys for fields in associative tables that
					 * reference the primary key of 'normal' tables.
					 */

					for (Column cd : columns) {

						if (this.classOrPropertyInfoByColumnInAssociativeTable
								.containsKey(cd)) {

							Info info = classOrPropertyInfoByColumnInAssociativeTable
									.get(cd);

							Table t_owner = cd.getInTable();

							String t_foreign_tablename;

							if (info instanceof ClassInfo) {

								ClassInfo ci = (ClassInfo) info;

								t_foreign_tablename = determineTableNameForType(
										ci);

							} else if (info instanceof PropertyInfo) {

								PropertyInfo pi = (PropertyInfo) info;

								t_foreign_tablename = determineTableNameForValueType(
										pi);

							} else {
								/*
								 * Should not happen, since we only store Class-
								 * and PropertyInfos in
								 * classOrPropertyInfoByColumnInAssociativeTable
								 */
								t_foreign_tablename = null;
							}

							Alter alter = alterTableAddForeignKeyConstraint(
									t_owner,
									namingScheme.nameForForeignKeyConstraint(
											t_owner.getName(), cd.getName(),
											t_foreign_tablename),
									cd, new Table(t_foreign_tablename));

							foreignKeyConstraints.add(alter);
						}
					}

				} else {

					for (Column cd : columns) {

						PropertyInfo pi = cd.getRepresentedProperty();

						if (pi == null) {
							continue;
						}

						if (refersToTypeRepresentedByTable(pi)) {

							Table t_main = cd.getInTable();

							String t_foreign_tablename;

							String valueTypeName = pi.typeInfo().name;
							String piEncodingRule = pi.encodingRule("sql");

							ProcessMapEntry pme = options.targetMapEntry(
									valueTypeName, piEncodingRule);

							if (pme != null && sqlddl.getMapEntryParamInfos()
									.hasParameter(pme,
											SqlConstants.ME_PARAM_TABLE)) {

								t_foreign_tablename = pme.getTargetType();

							} else {

								t_foreign_tablename = determineTableNameForValueType(
										pi);
							}

							String targetTableName = determineTableNameForValueType(
									pi);

							Alter alter = alterTableAddForeignKeyConstraint(
									t_main,
									namingScheme.nameForForeignKeyConstraint(
											pi.inClass().name(), pi.name(),
											targetTableName),
									cd, new Table(t_foreign_tablename));

							foreignKeyConstraints.add(alter);
						}
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

						Statement stmt = sqlddl.getDatabaseStrategy()
								.geometryMetadataUpdateStatement(t, col,
										sqlddl.getSrid());

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

					values.add(new StringValueExpression(codeName));

					for (DescriptorForCodeList descriptor : sqlddl
							.getDescriptorsForCodelist()) {

						String descName = descriptor.getDescriptorName();
						String value = null;

						if (descName.equalsIgnoreCase("name")) {

							value = codePi.name();

						} else if (descName.equalsIgnoreCase("documentation")) {

							value = codePi.derivedDocumentation(
									sqlddl.getDocumentationTemplate(),
									sqlddl.getDocumentationNoValue());

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

							values.add(new StringValueExpression(value));
						}
					}

					if (representedClass.matches(
							SqlConstants.RULE_TGT_SQL_CLS_CODELISTS_PODS)) {
						values.add(new StringValueExpression("Y"));
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

		Collections.sort(result, STATEMENT_COMPARATOR);

		// normalize names
		this.namingScheme.getNameNormalizer().visit(result);

		return result;
	}

	private List<Statement> statements() {

		List<Statement> stmts = new ArrayList<Statement>();

		stmts.addAll(this.createTableStatements);
		stmts.addAll(this.checkConstraints);
		stmts.addAll(this.foreignKeyConstraints);
		stmts.addAll(this.geometryMetadataUpdateStatements);
		stmts.addAll(this.geometryIndexStatements);
		stmts.addAll(this.insertStatements);

		return stmts;
	}

	private void addPodsSpecificsToCodelistTable(CreateTable ct, Column cd) {

		Table table = ct.getTable();
		List<Column> columns = table.getColumns();

		// add column "ACTIVE_INDICATOR_LF CHAR(1) NULL"
		Column cd_activeIndicatorLF = new Column("ACTIVE_INDICATOR_LF", table);
		ColumnDataType cd_activeIndicatorLFDataType = new ColumnDataType(
				"CHAR(1)");
		cd_activeIndicatorLF.setDataType(cd_activeIndicatorLFDataType);

		cd_activeIndicatorLF.addSpecification("NULL");

		columns.add(cd_activeIndicatorLF);

		/*
		 * add "CONSTRAINT CKC_AI_" + ct.getTable().getName() +
		 * " CHECK (ACTIVE_INDICATOR_LF IS NULL OR (ACTIVE_INDICATOR_LF IN ('Y','N'))),"
		 */
		ColumnExpression colExp = new ColumnExpression(cd_activeIndicatorLF);

		IsNullExpression isnExp = new IsNullExpression();
		isnExp.setExpression(colExp);

		InExpression inExp = new InExpression();
		inExp.setLeftExpression(colExp);
		ExpressionList expList = new ExpressionList();
		List<Expression> values = new ArrayList<Expression>();
		values.add(new StringValueExpression("Y"));
		values.add(new StringValueExpression("N"));
		expList.setExpressions(values);
		inExp.setRightExpressionsList(expList);

		OrExpression orExp = new OrExpression(isnExp, inExp);

		CheckConstraint ckc = new CheckConstraint(
				"CKC_AI_" + ct.getTable().getName(), orExp);
		ct.getTable().addConstraint(ckc);

		// add column "SOURCE_GCL VARCHAR(16) NULL"
		Column cd_sourceGcl = new Column("SOURCE_GCL", table);
		ColumnDataType cd_sourceGclDataType = new ColumnDataType("VARCHAR(16)");
		cd_sourceGcl.setDataType(cd_sourceGclDataType);
		cd_sourceGcl.addSpecification("NULL");

		columns.add(cd_sourceGcl);

		/*
		 * add "CONSTRAINT PK_" + ct.getTable().getName() +
		 * " PRIMARY KEY NONCLUSTERED (" + cd.getName() + ")"
		 */
		String indexSpec = "NONCLUSTERED";
		if (sqlddl.getDatabaseStrategy() instanceof PostgreSQLStrategy) {
			/*
			 * PostgreSQL doesn't have the concept of clustered indexes at all.
			 * Instead, all tables are heap tables and all indexes are
			 * non-clustered indexes.
			 * 
			 * TODO This might be a call for a database system specific DDL
			 * creator (unlike the common creator that currently exists). A
			 * specific creator for PostgreSQL would simply ignore the
			 * "nonclustered" flag in an index specification (which would then
			 * have to become a member of an additional class). Specific
			 * creators could also take into account specific syntax
			 * requirements. We could solve this using a common creator that
			 * uses the toString methods of SQL objects to write them, and
			 * overwrite behavior in database system specific creators (that
			 * extend the common creator) as necessary.
			 */
			indexSpec = null;
		}
		PrimaryKeyConstraint pkc = new PrimaryKeyConstraint(
				"PK_" + ct.getTable().getName(), cd, indexSpec);
		ct.getTable().addConstraint(pkc);
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

		return alter;
	}

	/**
	 * @param pi
	 *            property, may be <code>null</code>
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
	 * @see de.interactive_instruments.ShapeChange.MessageSource#message(int)
	 */
	public String message(int mnr) {

		switch (mnr) {
		case 0:
			return "Context: class SqlBuilder";

		case 5:
			return "Number format exception while converting the tagged value '$1$' to an integer. Exception message: $2$. Using $3$ as default value.";

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

		case 20:
			return "??More than eleven occurrences of foreign key '$1$'. Resulting schema will be ambiguous.";
		case 21:
			return "?? The type '$1$' was not found in the schema(s) selected for processing or in map entries. It will be mapped to 'unknown'.";

		case 100:
			return "Context: property '$1$' in class '$2$'.";
		default:
			return "(" + SqlBuilder.class.getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
