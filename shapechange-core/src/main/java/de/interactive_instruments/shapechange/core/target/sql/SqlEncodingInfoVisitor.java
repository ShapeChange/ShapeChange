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
 * (c) 2002-2023 interactive instruments GmbH, Bonn, Germany
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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.shapechange.core.target.sql;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.shapechange.core.target.sql.structure.Alter;
import de.interactive_instruments.shapechange.core.target.sql.structure.Column;
import de.interactive_instruments.shapechange.core.target.sql.structure.ColumnDataType;
import de.interactive_instruments.shapechange.core.target.sql.structure.Comment;
import de.interactive_instruments.shapechange.core.target.sql.structure.CreateIndex;
import de.interactive_instruments.shapechange.core.target.sql.structure.CreateSchema;
import de.interactive_instruments.shapechange.core.target.sql.structure.CreateTable;
import de.interactive_instruments.shapechange.core.target.sql.structure.DropSchema;
import de.interactive_instruments.shapechange.core.target.sql.structure.Insert;
import de.interactive_instruments.shapechange.core.target.sql.structure.PostgreSQLAlterRole;
import de.interactive_instruments.shapechange.core.target.sql.structure.SQLitePragma;
import de.interactive_instruments.shapechange.core.target.sql.structure.Select;
import de.interactive_instruments.shapechange.core.target.sql.structure.Statement;
import de.interactive_instruments.shapechange.core.target.sql.structure.StatementVisitor;
import de.interactive_instruments.shapechange.core.target.sql.structure.Table;
import de.interactive_instruments.shapechange.core.target.sql_encoding_util.SqlClassEncodingInfo;
import de.interactive_instruments.shapechange.core.target.sql_encoding_util.SqlEncodingInfos;
import de.interactive_instruments.shapechange.core.target.sql_encoding_util.SqlPropertyEncodingInfo;
import de.interactive_instruments.shapechange.core.transformation.taggedvalues.TaggedValueTransformer;
import de.interactive_instruments.shapechange.core.MessageSource;
import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.model.ClassInfo;
import de.interactive_instruments.shapechange.core.model.Info;
import de.interactive_instruments.shapechange.core.model.Model;
import de.interactive_instruments.shapechange.core.model.PackageInfo;
import de.interactive_instruments.shapechange.core.model.PropertyInfo;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class SqlEncodingInfoVisitor implements StatementVisitor, MessageSource {

    protected Options options;
    protected ShapeChangeResult result;

    protected Model model;

    protected SqlEncodingInfos sei = new SqlEncodingInfos();

    protected List<Table> tables = new ArrayList<>();

    public SqlEncodingInfoVisitor(SqlDdl sqlddl) {

	this.options = sqlddl.options;
	this.result = sqlddl.result;

	this.model = SqlDdl.model;
    }

    @Override
    public void visit(Insert insert) {
	// ignore
    }

    @Override
    public void visit(CreateTable createTable) {
	tables.add(createTable.getTable());
    }

    private String originalSchemaName(Info i) {
	return i.taggedValue(TaggedValueTransformer.TV_ORIG_SCHEMA_NAME);
    }

    private String originalClassName(ClassInfo ci) {
	return ci.taggedValue(TaggedValueTransformer.TV_ORIG_CLASS_NAME);
    }

    private String originalPropertyName(PropertyInfo pi) {
	return pi.taggedValue(TaggedValueTransformer.TV_ORIG_PROPERTY_NAME);
    }

    private String originalPropertyValueType(PropertyInfo pi) {
	return pi.taggedValue(TaggedValueTransformer.TV_ORIG_PROPERTY_VALUETYPE);
    }

    private String originalInClassName(PropertyInfo pi) {
	return pi.taggedValue(TaggedValueTransformer.TV_ORIG_INCLASS_NAME);
    }

    private String originalPropertyMultiplicity(PropertyInfo pi) {
	return pi.taggedValue(TaggedValueTransformer.TV_ORIG_PROPERTY_MULTIPLICITY);
    }

    private String schemaName(PropertyInfo pi) {
	return schemaName(pi.inClass());
    }

    private String schemaName(ClassInfo ci) {

	PackageInfo piSchema = model.schemaPackage(ci);
	if (piSchema != null) {
	    return piSchema.name();
	} else {
	    return null;
	}
    }

    @Override
    public void visit(CreateIndex createIndex) {
	// ignore
    }

    @Override
    public void visit(Alter alter) {
	/*
	 * Ignore. Foreign key info is provided via Column.getReferencedTable(), which
	 * can be inspected while evaluating CreateTable statements.
	 */
    }

    @Override
    public void visit(Comment comment) {
	// ignore
    }

    @Override
    public void visit(List<Statement> stmts) {

	if (stmts != null) {

	    for (Statement stmt : stmts) {

		stmt.accept(this);
	    }
	}
    }

    @Override
    public void visit(Select select) {
	// ignore
    }

    @Override
    public void visit(SQLitePragma sqLitePragma) {
	// ignore
    }

    @Override
    public void postprocess() {

	// 2023-06-22 JE: useful for debugging (understanding the different table cases)
//	System.out.println("table,isAssociativeTable,isUsageSpecific,repAsso,repClass,repProp");

	for (Table table : tables) {

	    String tableName = table.getName();
	    String schemaName = table.getSchemaName();

	    // 2023-06-22 JE: useful for debugging (understanding the different table cases)
//	    System.out.println(tableName + "," + table.isAssociativeTable() + "," + table.isUsageSpecificTable() + ","
//		    + (table.getRepresentedAssociation() != null) + "," + (table.getRepresentedClass() != null) + ","
//		    + (table.getRepresentedProperty() != null));

	    if (table.getRepresentedAssociation() != null) {

		/*
		 * The table represents an association with maxOccurs > 0 on both ends, i.e., an
		 * n:m relationship.
		 */

		Column c1 = null;
		Column c2 = null;

		for (Column column : table.getColumns()) {

		    // determine the relevant columns, ignoring any PK column

		    if (column.getRepresentedProperty() != null) {
			if (c1 == null) {
			    c1 = column;
			} else {
			    c2 = column;
			}
		    }
		}

		PropertyInfo pi1 = c1.getRepresentedProperty();
		Table refTable1 = c1.getReferencedTable();
		Optional<String> c1IdValueTypeOpt = ldproxyType(c1.getDataType());

		PropertyInfo pi2 = c2.getRepresentedProperty();
		Table refTable2 = c2.getReferencedTable();
		Optional<String> c2IdValueTypeOpt = ldproxyType(c2.getDataType());

		if (isRelevantProperty(pi1)) {

		    SqlPropertyEncodingInfo spei1 = createSqlPropertyEncodingInfoStub(pi1);

		    spei1.setSourceTable(refTable2.getName());
		    spei1.setSourceTableSchema(refTable2.getSchemaName());
		    spei1.setTargetTable(refTable1.getName());
		    spei1.setTargetTableSchema(refTable1.getSchemaName());
		    spei1.setValueSourcePath(
			    "[" + primaryKeyColumnName(refTable2) + "=" + c2.getName() + "]" + tableName + "/["
				    + c1.getName() + "=" + primaryKeyColumnName(refTable1) + "]" + refTable1.getName());
		    String idSourcePath = "[" + primaryKeyColumnName(refTable2) + "=" + c2.getName() + "]" + tableName
			    + "/" + c1.getName();
		    spei1.setIdInfos(idSourcePath, c1IdValueTypeOpt);

		    sei.add(spei1);
		}

		if (isRelevantProperty(pi2)) {

		    SqlPropertyEncodingInfo spei2 = createSqlPropertyEncodingInfoStub(pi2);

		    spei2.setSourceTable(refTable1.getName());
		    spei2.setSourceTableSchema(refTable1.getSchemaName());
		    spei2.setTargetTable(refTable2.getName());
		    spei2.setTargetTableSchema(refTable2.getSchemaName());
		    spei2.setValueSourcePath(
			    "[" + primaryKeyColumnName(refTable1) + "=" + c1.getName() + "]" + tableName + "/["
				    + c2.getName() + "=" + primaryKeyColumnName(refTable2) + "]" + refTable2.getName());
		    String idSourcePath = "[" + primaryKeyColumnName(refTable1) + "=" + c1.getName() + "]" + tableName
			    + "/" + c2.getName();
		    spei2.setIdInfos(idSourcePath, c2IdValueTypeOpt);

		    sei.add(spei2);
		}

	    } else if (table.getRepresentedClass() != null && table.getRepresentedProperty() != null) {

		/*
		 * The table is a usage specific table for encoding the data type values for a
		 * specific property.
		 */

		ClassInfo repCi = table.getRepresentedClass();
		SqlClassEncodingInfo scei = createSqlClassEncodingInfoStub(repCi);
		scei.setTable(tableName);
		scei.setDatabaseSchema(schemaName);
		sei.add(scei);

		PropertyInfo tableRepPi = table.getRepresentedProperty();

		/*
		 * First, identify the column that references the parent table (i.e., the column
		 * whose represented property is equal to the one of the table).
		 */
		Column parentTableRefCol = null;

		for (Column col : table.getColumns()) {
		    PropertyInfo repPi = col.getRepresentedProperty();
		    if (repPi != null && isRelevantProperty(repPi) && repPi == tableRepPi) {
			parentTableRefCol = col;
			break;
		    }
		}

		Table parentTable = parentTableRefCol.getReferencedTable();

		for (Column col : table.getColumns()) {

		    PropertyInfo repPi = col.getRepresentedProperty();

		    if (repPi != null && isRelevantProperty(repPi)) {

			SqlPropertyEncodingInfo spei = createSqlPropertyEncodingInfoStub(repPi);

			if (repPi == tableRepPi) {

			    // this is the column that references the parent table

			    // we create encoding info for the data type valued property

			    spei.setSourceTable(parentTable.getName());
			    spei.setSourceTableSchema(parentTable.getSchemaName());

			    spei.setTargetTable(tableName);
			    spei.setTargetTableSchema(schemaName);

			    spei.setValueSourcePath(
				    "[" + primaryKeyColumnName(parentTable) + "=" + col.getName() + "]" + tableName);
			    String idSourcePath = "[" + primaryKeyColumnName(parentTable) + "=" + col.getName() + "]"
				    + tableName + "/" + primaryKeyColumnName(table);
			    Optional<String> idValueTypeOpt = ldproxyTypeFromPrimaryKeyColumn(table);
			    spei.setIdInfos(idSourcePath, idValueTypeOpt);

			} else {

			    // create encoding info for a property owned by the data type itself

			    spei.setSourceTable(tableName);
			    spei.setSourceTableSchema(schemaName);

			    if (col.isForeignKeyColumn() && repPi.categoryOfValue() != Options.CODELIST) {

				Table referencedTable = col.getReferencedTable();

				spei.setTargetTable(referencedTable.getName());
				spei.setTargetTableSchema(referencedTable.getSchemaName());

				spei.setValueSourcePath("[" + col.getName() + "="
					+ primaryKeyColumnName(referencedTable) + "]" + referencedTable.getName());
				spei.setIdInfos(col.getName(), ldproxyType(col.getDataType()));

				/*
				 * check the case of n:1 relationship encoded by this property, and a relevant
				 * reverse property
				 */
				if (repPi.reverseProperty() != null) {

				    handleReversePropertyInNtoOneRelationship(repPi.reverseProperty(), referencedTable,
					    table, col);
				}

			    } else {
				spei.setValueSourcePath(col.getName());
			    }
			}

			sei.add(spei);
		    }
		}

	    } else if (table.getRepresentedClass() != null && table.getRepresentedProperty() == null) {

		/*
		 * The table represents a feature type, an object type, a data type
		 * (non-usage-specific case), or a code list.
		 */

		ClassInfo repCi = table.getRepresentedClass();
		SqlClassEncodingInfo scei = createSqlClassEncodingInfoStub(repCi);
		scei.setTable(tableName);
		scei.setDatabaseSchema(schemaName);
		sei.add(scei);

		/*
		 * For the sql property encoding infos, we are only interested in tables
		 * representing feature , object, or data types.
		 */

		if (repCi.category() == Options.FEATURE || repCi.category() != Options.OBJECT
			|| repCi.category() == Options.DATATYPE) {

		    for (Column col : table.getColumns()) {

			PropertyInfo repPi = col.getRepresentedProperty();

			if (repPi != null && isRelevantProperty(repPi)) {

			    SqlPropertyEncodingInfo spei = createSqlPropertyEncodingInfoStub(repPi);

			    spei.setSourceTable(tableName);
			    spei.setSourceTableSchema(schemaName);

			    if (col.isForeignKeyColumn() && repPi.categoryOfValue() != Options.CODELIST) {

				Table referencedTable = col.getReferencedTable();

				spei.setTargetTable(referencedTable.getName());
				spei.setTargetTableSchema(referencedTable.getSchemaName());

				spei.setValueSourcePath("[" + col.getName() + "="
					+ primaryKeyColumnName(referencedTable) + "]" + referencedTable.getName());
				spei.setIdInfos(col.getName(),ldproxyType(col.getDataType()));

				/*
				 * check the case of n:1 relationship encoded by this property, and a relevant
				 * reverse property
				 */
				if (repPi.reverseProperty() != null) {

				    handleReversePropertyInNtoOneRelationship(repPi.reverseProperty(), referencedTable,
					    table, col);
				}

			    } else {
				// property with simple value
				spei.setValueSourcePath(col.getName());
			    }

			    sei.add(spei);
			}
		    }
		}

	    } else if (table.getRepresentedProperty() != null && table.getRepresentedClass() == null) {

		/*
		 * The table is an associative table for an attribute with max mult greater 1,
		 * to encode a set of property values, where the value type maps to a simple
		 * type (not a table), is implemented as a table (data type or code list), or is
		 * a type that is mapped to a table. Whenever the value type is encoded as a
		 * separate table, the current 'table' (variable in this code) encodes an n:m
		 * relationship.
		 */

		PropertyInfo tableRepPi = table.getRepresentedProperty();

		/*
		 * Identify the column whose represented property is equal to the one of the
		 * table, as well as the column that references the parent table (i.e., the
		 * table that encodes the objects which own the attribute and its values).
		 */
		Column repPiCol = null;
		Column parentTableRefCol = null;

		for (Column col : table.getColumns()) {
		    if (col.getRepresentedProperty() == tableRepPi) {
			repPiCol = col;
		    }
		    if (col.isForeignKeyColumn() && col.getRepresentedProperty() != tableRepPi) {
			parentTableRefCol = col;
		    }
		}

		Table parentTable = parentTableRefCol.getReferencedTable();

		String sortKeyAddition = tableRepPi
			.matches(SqlConstants.RULE_TGT_SQL_ALL_ASSOCIATIVETABLES_WITH_SEPARATE_PK_FIELD) ? ""
				: "{sortKey=" + parentTableRefCol.getName() + "}";

		SqlPropertyEncodingInfo spei = createSqlPropertyEncodingInfoStub(tableRepPi);

		/*
		 * Determine if we have an n:m table.
		 */
		boolean isNtoMTable = repPiCol.getReferencedTable() != null;

		if (isNtoMTable) {

		    Table targetTable = repPiCol.getReferencedTable();

		    spei.setSourceTable(parentTable.getName());
		    spei.setSourceTableSchema(parentTable.getSchemaName());

		    if (tableRepPi.categoryOfValue() == Options.CODELIST) {

			/*
			 * Here we have the code value directly as value of the repPiCol column. That is
			 * what we actually want for the application (especially the ldproxy target).
			 * The code list table only encodes additional metadata for code values. If that
			 * is needed for particular applications, we can extend the behavior. For now,
			 * though, accessing the code list table is not necessary. That is why we do not
			 * encode values for the 'targetTable' in the SqlPropertyEncodingInfo.
			 */

			spei.setValueSourcePath(
				"[" + primaryKeyColumnName(parentTable) + "=" + parentTableRefCol.getName() + "]"
					+ tableName + sortKeyAddition + "/" + repPiCol.getName());

		    } else {

			spei.setTargetTable(targetTable.getName());
			spei.setTargetTableSchema(targetTable.getSchemaName());

			spei.setValueSourcePath("[" + primaryKeyColumnName(parentTable) + "="
				+ parentTableRefCol.getName() + "]" + tableName + "/[" + repPiCol.getName() + "="
				+ primaryKeyColumnName(targetTable) + "]" + targetTable.getName());
			String idSourcePath = "[" + primaryKeyColumnName(parentTable) + "=" + parentTableRefCol.getName()
				+ "]" + tableName + "/" + repPiCol.getName();
			spei.setIdInfos(idSourcePath, ldproxyType(repPiCol.getDataType()));
		    }

		} else {

		    spei.setSourceTable(parentTable.getName());
		    spei.setSourceTableSchema(parentTable.getSchemaName());

		    spei.setValueSourcePath("[" + primaryKeyColumnName(parentTable) + "=" + parentTableRefCol.getName()
			    + "]" + tableName + sortKeyAddition + "/" + repPiCol.getName());
		}

		sei.add(spei);

	    } else {
		result.addFatalError(this, 100, table.getFullName());
	    }
	}
    }

    /**
     * Handle the case of adding encoding infos for the n-case in a n:1
     * relationship, with the relationship being modeled as an association (where
     * one navigable end has max mult equal 1, and the other end is either
     * non-navigable, or is navigable and has max mult greater 1). The SQL encoding
     * then typically encodes the 1-case, i.e. the association role with max mult
     * equal 1, as a foreign key column. The reverse property, if relevant (encoded,
     * navigable, max mult greater 1), is then not explicitly represented in the
     * database schema, and thus needs to be handled here.
     * 
     * @param reverseProperty        Property that is the reverse in an association
     *                               that represents a n:1 relationship, with the
     *                               reverse property being the n-case.
     * @param sourceTable            Table that represents the class which owns the
     *                               reverse property.
     * @param targetTable            Table that represents the class which is the
     *                               value type of the reverse property.
     * @param colWithFkInTargetTable Column in the target table that represents the
     *                               other end of the association (in which the
     *                               reverse property is one end), and contains the
     *                               foreign key to the target table.
     */
    private void handleReversePropertyInNtoOneRelationship(PropertyInfo reverseProperty, Table sourceTable,
	    Table targetTable, Column colWithFkInTargetTable) {

	if (reverseProperty.cardinality().maxOccurs > 1 && isRelevantProperty(reverseProperty)) {

	    /*
	     * Here we have the case of an n:1 relationship, where the reverse property is
	     * not encoded as a column, and represents the n-case. Since the reverse
	     * property is a relevant one, we create encoding infos for it, with the
	     * appropriate source path.
	     */

	    SqlPropertyEncodingInfo speiRevPi = createSqlPropertyEncodingInfoStub(reverseProperty);

	    speiRevPi.setSourceTable(sourceTable.getName());
	    speiRevPi.setSourceTableSchema(sourceTable.getSchemaName());

	    speiRevPi.setTargetTable(targetTable.getName());
	    speiRevPi.setTargetTableSchema(targetTable.getSchemaName());

	    speiRevPi.setValueSourcePath("[" + primaryKeyColumnName(sourceTable) + "="
		    + colWithFkInTargetTable.getName() + "]" + targetTable.getName());
	    String idSourcePath = "[" + primaryKeyColumnName(sourceTable) + "=" + colWithFkInTargetTable.getName()
		    + "]" + targetTable.getName() + "/" + primaryKeyColumnName(targetTable);
	    Optional<String> idValueTypeOpt = ldproxyTypeFromPrimaryKeyColumn(targetTable);
	    speiRevPi.setIdInfos(idSourcePath, idValueTypeOpt);

	    sei.add(speiRevPi);
	}
    }

    private SqlClassEncodingInfo createSqlClassEncodingInfoStub(ClassInfo ci) {

	SqlClassEncodingInfo scei = new SqlClassEncodingInfo();

	scei.setSchemaName(schemaName(ci));
	scei.setOriginalSchemaName(originalSchemaName(ci));

	scei.setClassName(ci.name());
	scei.setOriginalClassName(originalClassName(ci));

	return scei;
    }

    /**
     * @param pi the property to create a SqlPropertyEncodingInfo object for
     * @return a SqlPropertyEncodingInfo object with schema information taken from
     *         the property itself (sql encoding info is not populated - that needs
     *         to be done separately)
     */
    private SqlPropertyEncodingInfo createSqlPropertyEncodingInfoStub(PropertyInfo pi) {

	SqlPropertyEncodingInfo spei = new SqlPropertyEncodingInfo();

	spei.setSchemaName(schemaName(pi));
	spei.setOriginalSchemaName(originalSchemaName(pi));
	spei.setPropertyName(pi.name());
	spei.setOriginalPropertyName(originalPropertyName(pi));
	spei.setInClassName(pi.inClass().name());
	spei.setOriginalInClassName(originalInClassName(pi));
	spei.setPropertyValueType(pi.typeInfo().name);
	spei.setOriginalPropertyValueType(originalPropertyValueType(pi));
	spei.setPropertyMultiplicity(pi.cardinality().toString());
	spei.setOriginalPropertyMultiplicity(originalPropertyMultiplicity(pi));

	return spei;
    }

    /**
     * @param pi The property to inspect, if it is relevant for SqlEncodingInfos
     * @return <code>true</code>, if the property is encoded and navigable, else
     *         <code>false</code>
     */
    private boolean isRelevantProperty(PropertyInfo pi) {
	return pi.isNavigable() && SqlDdl.isEncoded(pi);
    }

    /**
     * Identifies the primary key column(s) of a given table.
     * 
     * @param t the table for which to identify primary key columns
     * 
     * @return The name of the primary key column of the given table (should be a
     *         non-associative table)
     */
    private String primaryKeyColumnName(Table t) {

	List<Column> pkColumns = t.getPrimaryKeyColumns();

	if (pkColumns.size() == 1) {

	    return pkColumns.getFirst().getName();

	} else {

	    /*
	     * we are looking for a single primary key column; the case of multiple PKs
	     * should not occur with the current encoding logic of the SqlDdl target - at
	     * least not for non-associative tables.
	     */

	    // finally, if we have not identified any primary key column
	    String fallback = "FIXME_NO_UNIQUE_PK_FOUND";
	    result.addWarning(this, 101, t.getFullName(), fallback);
	    return fallback;
	}
    }

    @Override
    public void visit(CreateSchema createSchema) {
	// ignore
    }

    @Override
    public void visit(DropSchema dropSchema) {
	// ignore
    }

    @Override
    public void visit(PostgreSQLAlterRole postgreSQLAlterRole) {
	// ignore
    }

    public SqlEncodingInfos getSqlEncodingInfos() {
	return sei;
    }

    public Optional<String> ldproxyType(ColumnDataType dataType) {
	if (dataType != null) {
	    return ldproxyTypeFromDataType(dataType.getName());
	} else {
	    return Optional.empty();
	}
    }

    public Optional<String> ldproxyTypeFromDataType(String dataTypeName) {
	if (StringUtils.equalsAnyIgnoreCase(dataTypeName, "int", "integer", "bigserial", "smallint", "bigint",
		"shortinteger", "longinteger")) {
	    return Optional.of("integer");
	} else {
	    // assume string then
	    return Optional.of("string");
	}
    }

    public Optional<String> ldproxyTypeFromPrimaryKeyColumn(Table table) {

	if (table != null) {

	    List<Column> pkCols = table.getPrimaryKeyColumns();

	    if (pkCols.size() == 1) {
		return ldproxyType(pkCols.getFirst().getDataType());
	    } else {
		// undefined - return empty optional
	    }
	}

	return Optional.empty();
    }

    @Override
    public String message(int mnr) {

	switch (mnr) {
	case 1:
	    return "";
	case 2:
	    return "";
	case 3:
	    return "";
	case 4:
	    return "";
	case 100:
	    return "Table '$1$' represents a case that is not recognized by the SqlEncodingInfoVisitor. This is a system error. Please contact the ShapeChange developers.";
	case 101:
	    return "??Could not determine primary key column(s) for table '$1$'. Using '$2$' as fallback. If the table is defined by a map entry, do provide information about the primary key columns in the map entry.";

	default:
	    return "(" + SqlEncodingInfoVisitor.class.getName() + ") Unknown message with number: " + mnr;
	}
    }
}
