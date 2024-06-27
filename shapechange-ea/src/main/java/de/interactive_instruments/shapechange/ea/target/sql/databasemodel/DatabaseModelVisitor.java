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
 * (c) 2002-2024 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.ea.target.sql.databasemodel;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.sparx.Attribute;
import org.sparx.Collection;
import org.sparx.Connector;
import org.sparx.ConnectorEnd;
import org.sparx.Element;
import org.sparx.Method;
import org.sparx.Package;
import org.sparx.Parameter;
import org.sparx.Project;
import org.sparx.Repository;

import de.interactive_instruments.shapechange.core.target.sql.expressions.SpatiaLiteCreateSpatialIndexExpression;
import de.interactive_instruments.shapechange.core.target.sql.structure.Alter;
import de.interactive_instruments.shapechange.core.target.sql.structure.AlterExpression;
import de.interactive_instruments.shapechange.core.target.sql.structure.CheckConstraint;
import de.interactive_instruments.shapechange.core.target.sql.structure.Column;
import de.interactive_instruments.shapechange.core.target.sql.structure.ColumnDataType;
import de.interactive_instruments.shapechange.core.target.sql.structure.Comment;
import de.interactive_instruments.shapechange.core.target.sql.structure.ConstraintAlterExpression;
import de.interactive_instruments.shapechange.core.target.sql.structure.CreateIndex;
import de.interactive_instruments.shapechange.core.target.sql.structure.CreateSchema;
import de.interactive_instruments.shapechange.core.target.sql.structure.CreateTable;
import de.interactive_instruments.shapechange.core.target.sql.structure.DropSchema;
import de.interactive_instruments.shapechange.core.target.sql.structure.ForeignKeyConstraint;
import de.interactive_instruments.shapechange.core.target.sql.structure.ForeignKeyConstraint.ReferentialAction;
import de.interactive_instruments.shapechange.core.target.sql.structure.Index;
import de.interactive_instruments.shapechange.core.target.sql.structure.Insert;
import de.interactive_instruments.shapechange.core.target.sql.structure.PostgreSQLAlterRole;
import de.interactive_instruments.shapechange.core.target.sql.structure.PrimaryKeyConstraint;
import de.interactive_instruments.shapechange.core.target.sql.structure.SQLitePragma;
import de.interactive_instruments.shapechange.core.target.sql.structure.Select;
import de.interactive_instruments.shapechange.core.target.sql.structure.SqlConstraint;
import de.interactive_instruments.shapechange.core.target.sql.structure.Statement;
import de.interactive_instruments.shapechange.core.target.sql.structure.StatementVisitor;
import de.interactive_instruments.shapechange.core.target.sql.structure.Table;
import de.interactive_instruments.shapechange.core.target.sql.structure.UniqueConstraint;
import de.interactive_instruments.shapechange.core.MessageSource;
import de.interactive_instruments.shapechange.core.Multiplicity;
import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ProcessMapEntry;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.model.ClassInfo;
import de.interactive_instruments.shapechange.core.model.Info;
import de.interactive_instruments.shapechange.core.model.Model;
import de.interactive_instruments.shapechange.core.model.PackageInfo;
import de.interactive_instruments.shapechange.core.target.sql.DatabaseModelConstants;
import de.interactive_instruments.shapechange.core.target.sql.OracleStrategy;
import de.interactive_instruments.shapechange.core.target.sql.SQLServerStrategy;
import de.interactive_instruments.shapechange.core.target.sql.SQLiteStrategy;
import de.interactive_instruments.shapechange.core.target.sql.SqlDdl;
import de.interactive_instruments.shapechange.core.target.sql.SqlUtil;
import de.interactive_instruments.shapechange.ea.util.EAAttributeUtil;
import de.interactive_instruments.shapechange.ea.util.EAConnectorEndUtil;
import de.interactive_instruments.shapechange.ea.util.EAConnectorUtil;
import de.interactive_instruments.shapechange.ea.util.EADirection;
import de.interactive_instruments.shapechange.ea.util.EAElementUtil;
import de.interactive_instruments.shapechange.ea.util.EAException;
import de.interactive_instruments.shapechange.ea.util.EAMethodUtil;
import de.interactive_instruments.shapechange.ea.util.EANavigable;
import de.interactive_instruments.shapechange.ea.util.EAPackageUtil;
import de.interactive_instruments.shapechange.ea.util.EAParameterUtil;
import de.interactive_instruments.shapechange.ea.util.EARepositoryUtil;
import de.interactive_instruments.shapechange.ea.util.EASupportedDBMS;
import de.interactive_instruments.shapechange.ea.util.EATaggedValue;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class DatabaseModelVisitor implements StatementVisitor, MessageSource {

    private SqlDdl sqlddl;

    protected Options options;
    protected ShapeChangeResult result;

    protected Model model;
    protected Repository repository;

    /**
     * The database owner, if specified via the configuration. Otherwise
     * <code>null</code>.
     */
    protected String dbOwner;
    /**
     * The database version, if specified via the configuration. Otherwise
     * <code>null</code>.
     */
    protected String dbVersion;
    /**
     * The tablespace, if specified via the configuration. Otherwise
     * <code>null</code>.
     */
    protected String tablespace;

    protected String author = null;
    protected String status = null;

    protected EASupportedDBMS eadbms;

    protected boolean establishPackageHierarchy = false;

    protected Integer tablesPkgID = null;

    /**
     * key: database model sub package; value: {key: application schema package;
     * value: corresponding EA package within the database model sub package}
     */
    protected SortedMap<Integer, SortedMap<PackageInfo, Integer>> eaPkgIdByModelPkg_byDatabaseModelSubPkgId = new TreeMap<Integer, SortedMap<PackageInfo, Integer>>();

    protected Map<Column, Integer> eaAttributeIDByColumn = new HashMap<>();
    protected Map<Table, Integer> eaElementIDByTable = new HashMap<>();

    protected Map<String, Integer> spatialIndexCounterByTableName = new HashMap<>();

    public DatabaseModelVisitor(SqlDdl sqlddl, Repository repository, ShapeChangeResult result) {

	this.sqlddl = sqlddl;

	this.result = result;
	this.options = result.options();
	
	this.model = sqlddl.model();
	this.repository = repository;

	dbOwner = options.parameterAsString(sqlddl.getClass().getName(), DatabaseModelConstants.PARAM_DB_OWNER, null,
		false, true);

	dbVersion = options.parameterAsString(sqlddl.getClass().getName(), DatabaseModelConstants.PARAM_DB_VERSION,
		null, false, true);

	author = options.parameterAsString(sqlddl.getClass().getName(), DatabaseModelConstants.PARAM_EA_AUTHOR, null,
		false, true);

	status = options.parameterAsString(sqlddl.getClass().getName(), DatabaseModelConstants.PARAM_EA_STATUS, null,
		false, true);

	tablespace = options.parameterAsString(sqlddl.getClass().getName(), DatabaseModelConstants.PARAM_TABLESPACE,
		null, false, true);

	if (sqlddl.databaseStrategy() instanceof OracleStrategy) {
	    eadbms = EASupportedDBMS.ORACLE;
	} else if (sqlddl.databaseStrategy() instanceof SQLServerStrategy) {
	    eadbms = EASupportedDBMS.SQLSERVER2012;
	} else if (sqlddl.databaseStrategy() instanceof SQLiteStrategy) {
	    eadbms = EASupportedDBMS.SQLITE;
	} else {
	    eadbms = EASupportedDBMS.POSTGRESQL;
	}

	establishPackageHierarchy = options.parameterAsBoolean(sqlddl.getClass().getName(),
		DatabaseModelConstants.PARAM_ESTABLISH_PACKAGE_HIERARCHY, false);
    }

    public void initialize() throws Exception {

	// Get first root package
	repository.RefreshModelView(0);

	Collection<Package> c = repository.GetModels();
	Package root = c.GetAt((short) 0);

	boolean deletePreexistingPackage = options.parameterAsBoolean(SqlDdl.class.getName(),
		DatabaseModelConstants.PARAM_DELETE_PREEXISTING_DATAMODEL_PACKAGE, false);

	if (deletePreexistingPackage) {
	    /*
	     * Check if <<DataModel>> package with name from pattern already exists - if so,
	     * delete it.
	     */
	    Integer dmPkgID = EARepositoryUtil.getEAChildPackageByName(repository, root.GetPackageID(),
		    eadbms.getDmPatternPackageName());

	    if (dmPkgID != null) {
		result.addInfo(this, 100, eadbms.getDmPatternPackageName());
		EARepositoryUtil.deletePackage(repository, dmPkgID);
	    }
	}

	/*
	 * Get XMI template
	 */
	String dmPatternDir = options.parameterAsString(sqlddl.getClass().getName(),
		DatabaseModelConstants.PARAM_DM_PATTERN_PATH, "http://shapechange.net/resources/dataModelPatterns",
		false, true);

	File dmPatternFile;
	String dmPatternFilePath = dmPatternDir + "/" + eadbms.getDmPatternFileName();

	if (dmPatternDir.toLowerCase().startsWith("http")) {
	    // create temporary local copy of XMI template
	    try {
		URL url = new URL(dmPatternFilePath);
		dmPatternFile = File.createTempFile(eadbms.getDmPatternFileName(), null);
		dmPatternFile.deleteOnExit();
		FileUtils.copyURLToFile(url, dmPatternFile);
	    } catch (IOException e) {
		result.addError(this, 102, dmPatternFilePath, e.getMessage());
		throw new Exception();
	    }
	} else {
	    dmPatternFile = new File(dmPatternFilePath);
	    if (!dmPatternFile.exists()) {
		result.addError(this, 101, dmPatternFile.getAbsolutePath());
		throw new Exception();
	    }
	}

	/*
	 * Import XMI template into "Model" package
	 */

	Project project = repository.GetProjectInterface();

	String modelPkgGUIDAsXML = project.GUIDtoXML(root.GetPackageGUID());

	/*
	 * By setting the last parameter to 1 (instead of 0) we strip the GUIDs. That
	 * way, the data model pattern is imported, even if a package with the same name
	 * already exists.
	 * 
	 * TBD: Alternatively, we could delete a pre-existing package.
	 */
	String dmPkgGUIDOrError = project.ImportPackageXMI(modelPkgGUIDAsXML, dmPatternFile.getAbsolutePath(), 1, 1);

	if (StringUtils.isEmpty(dmPkgGUIDOrError) || !dmPkgGUIDOrError.startsWith("{")) {

	    result.addError(this, 103, dmPatternFilePath, dmPkgGUIDOrError);

	} else {

	    Integer dbPkg = EARepositoryUtil.getEAChildPackageByStereotype(repository,
		    repository.GetPackageByGuid(dmPkgGUIDOrError).GetPackageID(), "Database");

	    if (dbOwner != null) {
		EAPackageUtil.setTaggedValue(repository.GetPackageByID(dbPkg), "DefaultOwner", dbOwner);
	    }

	    /*
	     * No null check because the XMI template contains a <<Database>> package
	     */
	    tablesPkgID = EARepositoryUtil.getEAChildPackageByName(repository, dbPkg, "Tables");
	    eaPkgIdByModelPkg_byDatabaseModelSubPkgId.put(tablesPkgID, new TreeMap<PackageInfo, Integer>());
	}
    }

    @Override
    public void visit(Insert insert) {
	// ignore

    }

    @Override
    public void visit(CreateTable createTable) {

	Table table = createTable.getTable();

	try {

	    int eaPkgID = tablesPkgID;
	    if (establishPackageHierarchy && table.getRepresentedClass() != null) {
		eaPkgID = EARepositoryUtil.establishEAPackageHierarchy(table.getRepresentedClass(), tablesPkgID,
			eaPkgIdByModelPkg_byDatabaseModelSubPkgId, repository, sqlddl.numberOfEncodedSchemas(), author,
			status);
	    }
	    Element tableElmt = EARepositoryUtil.createEAClass(repository, table.getName(), eaPkgID);

	    setCommonElementFields(tableElmt);

	    this.eaElementIDByTable.put(table, tableElmt.GetElementID());

	    // Load linked document, if it exists
	    ClassInfo representedClass = table.getRepresentedClass();
	    if (representedClass != null && representedClass.getLinkedDocument() != null) {
		EAElementUtil.loadLinkedDocument(tableElmt, representedClass.getLinkedDocument().getAbsolutePath());
	    }

	    /*
	     * NOTE: The stereotype must be set before any tagged values associated with
	     * that stereotype, since EA will initialize those tagged values for us and the
	     * EA API does not appear to provide a way to create them ourselves
	     * programmatically.
	     */
	    EAElementUtil.setEAStereotypeEx(tableElmt, "EAUML::table");

	    EAElementUtil.setEAGenType(tableElmt, eadbms.getGenType());

	    if (dbOwner != null) {
		EAElementUtil.updateTaggedValue(tableElmt, "EAUML::table::Owner", dbOwner, false);
	    }
	    if (dbVersion != null) {
		EAElementUtil.updateTaggedValue(tableElmt, "EAUML::table::DBVersion", dbVersion, false);
	    }
	    if (tablespace != null) {
		EAElementUtil.updateTaggedValue(tableElmt, "EAUML::table::Tablespace", tablespace, false);
	    }

	    // Determine if any specific tagged values shall be represented
	    if (sqlddl.representTaggedValues()) {

		Info representedInfo = null;
		if (table.getRepresentedClass() != null) {
		    representedInfo = table.getRepresentedClass();
		} else if (table.getRepresentedAssociation() != null) {
		    representedInfo = table.getRepresentedAssociation();
		}

		if (representedInfo != null) {
		    for (String tvToRepresent : sqlddl.taggedValuesToRepresent()) {
			String normalizedTag = options.normalizeTag(tvToRepresent);
			String[] values = representedInfo.taggedValuesForTag(normalizedTag);
			if (values.length > 0) {
			    EAElementUtil.setTaggedValue(tableElmt,
				    new EATaggedValue(tvToRepresent, Arrays.asList(values)));
			}
		    }
		}
	    }

	    String tableDocumentation = table.getDocumentation();

	    if (StringUtils.isBlank(tableDocumentation)) {

		if (table.getRepresentedClass() != null) {
		    tableDocumentation = table.getRepresentedClass().derivedDocumentation(sqlddl.documentationTemplate(),
			    sqlddl.documentationNoValue());
		} else if (table.getRepresentedAssociation() != null) {
		    tableDocumentation = table.getRepresentedAssociation()
			    .derivedDocumentation(sqlddl.documentationTemplate(), sqlddl.documentationNoValue());
		}
	    }
	    if (tableDocumentation != null) {
		EAElementUtil.setEANotes(tableElmt, tableDocumentation);
	    }

	    List<Column> primaryKeyColumns = new ArrayList<Column>();

	    Set<String> columnStereotypes = new HashSet<String>();
	    columnStereotypes.add("EAUML::column");

	    for (Column col : table.getColumns()) {

		String columnDocumentation = col.getDocumentation();

		if (StringUtils.isBlank(columnDocumentation)) {

		    if (col.getRepresentedProperty() != null) {
			columnDocumentation = col.getRepresentedProperty()
				.derivedDocumentation(sqlddl.documentationTemplate(), sqlddl.documentationNoValue());
		    }
		}

		Attribute att = EAElementUtil.createEAAttribute(tableElmt, col.getName(), null, columnDocumentation,
			columnStereotypes, null, false, false, false,
			col.hasDefaultValue() ? col.getDefaultValue().toString() : null, false, new Multiplicity(1, 1),
			mapDataType(col), null);

		this.eaAttributeIDByColumn.put(col, att.GetAttributeID());

		if (col.isNotNull()) {
		    EAAttributeUtil.setEAAllowDuplicates(att, true);
		}

		ColumnDataType coldt = col.getDataType();

		int precision = coldt.hasPrecision() ? coldt.getPrecision() : 0;
		int scale = coldt.hasScale() ? coldt.getScale() : 0;

		EAAttributeUtil.setEAPrecision(att, precision);
		EAAttributeUtil.setEAScale(att, scale);
		if (coldt.hasLength()) {
		    EAAttributeUtil.setEALength(att, "" + coldt.getLength());

		    if (coldt.hasLengthQualifier()) {
			EAAttributeUtil.setTaggedValue(att, "LengthType", coldt.getLengthQualifier());
		    }
		}

		if (col.isForeignKeyColumn()) {
		    EAAttributeUtil.setEAIsCollection(att, true);
		}

		if (col.isPrimaryKeyColumn()) {
		    primaryKeyColumns.add(col);
		    /*
		     * actually setting the attribute as primary key will be done later on, when we
		     * have all primary key columns (also from primary key constraints)
		     */
		}

		// Determine if any specific tagged values shall be represented
		if (sqlddl.representTaggedValues() && col.getRepresentedProperty() != null) {

		    for (String tvToRepresent : sqlddl.taggedValuesToRepresent()) {
			String normalizedTag = options.normalizeTag(tvToRepresent);
			String[] values = col.getRepresentedProperty().taggedValuesForTag(normalizedTag);
			if (values.length > 0) {
			    EAAttributeUtil.setTaggedValue(att,
				    new EATaggedValue(tvToRepresent, Arrays.asList(values)));
			}
		    }
		}
	    }

	    /*
	     * Now investigate the table constraints to identify any primary key constraint.
	     */
	    for (SqlConstraint constr : table.getConstraints()) {

		if (constr instanceof PrimaryKeyConstraint) {
		    PrimaryKeyConstraint pkCon = (PrimaryKeyConstraint) constr;
		    primaryKeyColumns.addAll(pkCon.getColumns());
		}
	    }

	    // Create primary key 'operation'
	    Method m = EAElementUtil.createEAMethod(tableElmt,
		    "PK_" + SqlUtil.determineName(table, sqlddl.constraintNameUsingShortName()));
	    EAMethodUtil.setEAStereotypeEx(m, "EAUML::PK");

	    Collections.reverse(primaryKeyColumns);

	    for (Column pkCol : primaryKeyColumns) {

		Parameter param = EAMethodUtil.createEAParameter(m, pkCol.getName());

		EAParameterUtil.setEAType(param, mapDataType(pkCol));

		// also set attributes as primary keys
		Integer attributeID = this.eaAttributeIDByColumn.get(pkCol);
		EAAttributeUtil.setEAIsOrdered(repository.GetAttributeByID(attributeID), true);
	    }

	} catch (EAException e) {
	    result.addError(this, 104, table.getName(), e.getMessage());
	}
    }

    private void setCommonElementFields(Element e) throws EAException {

	if (author != null) {
	    EAElementUtil.setEAAuthor(e, author);
	}

	if (status != null) {
	    EAElementUtil.setEAStatus(e, status);
	}
    }

    @Override
    public void visit(CreateIndex createIndex) {

	Table table = createIndex.getTable();

	Element tableElmt = repository.GetElementByID(this.eaElementIDByTable.get(table));

	Index index = createIndex.getIndex();

	try {

	    /* Create index 'operation' */
	    Method m = EAElementUtil.createEAMethod(tableElmt, index.getName());
	    EAMethodUtil.setEAStereotypeEx(m, "EAUML::index");

	    List<Column> indexColumns = index.getColumns();

	    for (Column indexCol : indexColumns) {

		Parameter param = EAMethodUtil.createEAParameter(m, indexCol.getName());

		EAParameterUtil.setEAType(param, mapDataType(indexCol));
	    }

	    // TBD Is there a way to set index specs, for example via tagged
	    // values?

	} catch (EAException e) {
	    result.addError(this, 109, index.getName(), table.getName(), e.getMessage());
	}

    }

    @Override
    public void visit(Alter alter) {

	Table table = alter.getTable();

	AlterExpression expr = alter.getExpression();

	if (expr instanceof ConstraintAlterExpression) {

	    ConstraintAlterExpression conAltExpr = (ConstraintAlterExpression) expr;

	    // For now we assume that the operation is always 'ADD'

	    SqlConstraint constr = conAltExpr.getConstraint();

	    if (constr instanceof CheckConstraint) {

		CheckConstraint checkCon = (CheckConstraint) constr;

		try {
		    Element tableElmt = repository.GetElementByID(this.eaElementIDByTable.get(table));

		    // Create check constraint 'operation'
		    Method m = EAElementUtil.createEAMethod(tableElmt, constr.getName());
		    EAMethodUtil.setEAStereotypeEx(m, "EAUML::check");
		    EAMethodUtil.setEACode(m, checkCon.getExpression().toString());

		} catch (EAException e) {
		    result.addError(this, 105, constr.getName(), table.getName(), e.getMessage());
		}

	    } else if (constr instanceof ForeignKeyConstraint) {

		ForeignKeyConstraint fkCon = (ForeignKeyConstraint) constr;

		try {
		    Element tableElmt = repository.GetElementByID(this.eaElementIDByTable.get(table));

		    /* Create foreign key constraint 'operation' */
		    Method m = EAElementUtil.createEAMethod(tableElmt, constr.getName());
		    EAMethodUtil.setEAStereotypeEx(m, "EAUML::FK");

		    List<Column> foreignKeyColumns = fkCon.getColumns();

		    Collections.reverse(foreignKeyColumns);

		    for (Column fkCol : foreignKeyColumns) {

			Parameter param = EAMethodUtil.createEAParameter(m, fkCol.getName());

			EAParameterUtil.setEAType(param, mapDataType(fkCol));

			// attributes have already been set as foreign keys
		    }

		    // also set tagged values
		    List<EATaggedValue> methodTVs = new ArrayList<EATaggedValue>();
		    String onDeleteValue = translateForeignKeyReferentialAction(fkCon.getOnDelete());
		    EATaggedValue tvDelete = new EATaggedValue("Delete", onDeleteValue);
		    methodTVs.add(tvDelete);

		    if (eadbms == EASupportedDBMS.ORACLE) {

			// Oracle only has on delete
			EATaggedValue tvProperty = new EATaggedValue("property", "Delete " + onDeleteValue + "=1;");
			methodTVs.add(tvProperty);

		    } else {

			String onUpdateValue = translateForeignKeyReferentialAction(fkCon.getOnUpdate());
			EATaggedValue tvUpdate = new EATaggedValue("Update", onUpdateValue);
			methodTVs.add(tvUpdate);

			EATaggedValue tvProperty = new EATaggedValue("property",
				"Delete " + onDeleteValue + "=1;Update " + onUpdateValue + "=1;");
			methodTVs.add(tvProperty);
		    }

		    /*
		     * NOTE: According to the documentation
		     * (https://sparxsystems.com/enterprise_architect_user_guide/15.2/model_domains/
		     * constraints2.html and
		     * https://sparxsystems.com/enterprise_architect_user_guide/15.2/model_domains/
		     * foreignkeys.html), EA database models do not reflect constraint checking
		     * options (deferrable and initial constraint mode).
		     */

		    EAMethodUtil.setTaggedValues(m, methodTVs);

		    /*
		     * Create directed association for foreign key
		     */
		    Integer refTableEaElmtID = this.eaElementIDByTable.get(fkCon.getReferenceTable());
		    if (refTableEaElmtID == null) {
			result.addError(this, 111, constr.getName(), table.getName(),
				fkCon.getReferenceTable().getName());
			return;
		    }
		    Element referenceTableElmt = repository.GetElementByID(refTableEaElmtID);
		    Method pkMethodOfReferenceTableElmt = EAElementUtil.getEAMethodWithStereotypeEx(referenceTableElmt,
			    "PK");

		    if (pkMethodOfReferenceTableElmt == null) {
			result.addError(this, 108, constr.getName(), table.getName(),
				fkCon.getReferenceTable().getName());
			return;
		    }

		    String pkMethodName = pkMethodOfReferenceTableElmt.GetName();
		    Parameter pkColParam = EAMethodUtil.getFirstParameter(pkMethodOfReferenceTableElmt);
		    String conName = "(" + foreignKeyColumns.get(0).getName() + " = " + pkColParam.GetName() + ")";

		    Connector con = EAElementUtil.createEAAssociation(tableElmt, referenceTableElmt);

		    EAConnectorUtil.setEAStereotypeEx(con, "EAUML::FK");
		    EAConnectorUtil.setEAName(con, conName);

		    ConnectorEnd clientEnd = con.GetClientEnd();
		    EAConnectorEndUtil.setEARole(clientEnd, fkCon.getName());
		    EAConnectorEndUtil.setEACardinality(clientEnd, "0..*");
		    EAConnectorEndUtil.setEANavigable(clientEnd, EANavigable.NONNAVIGABLE);
		    EAConnectorEndUtil.setEAContainment(clientEnd, "Unspecified");

		    ConnectorEnd supplierEnd = con.GetSupplierEnd();
		    EAConnectorEndUtil.setEARole(supplierEnd, pkMethodName);
		    EAConnectorEndUtil.setEACardinality(supplierEnd, "1");
		    EAConnectorEndUtil.setEANavigable(supplierEnd, EANavigable.NAVIGABLE);
		    EAConnectorEndUtil.setEAContainment(supplierEnd, "Unspecified");

		    // Apparently the StyleEx needs to be set as well (noticed
		    // during development; apparently this is relevant for the
		    // joining)
		    EAConnectorUtil.setEAStyleEx(con, "FKINFO=SRC=" + fkCon.getName() + ":DST=" + pkMethodName + ":;");

		    EAConnectorUtil.setEADirection(con, EADirection.SOURCE_DESTINATION);

		} catch (EAException e) {
		    result.addError(this, 106, constr.getName(), table.getName(), e.getMessage());
		}

	    } else if (constr instanceof UniqueConstraint) {

		UniqueConstraint ukCon = (UniqueConstraint) constr;

		try {
		    Element tableElmt = repository.GetElementByID(this.eaElementIDByTable.get(table));

		    /* Create unique constraint 'operation' */
		    Method m = EAElementUtil.createEAMethod(tableElmt, constr.getName());
		    EAMethodUtil.setEAStereotypeEx(m, "EAUML::unique");

		    List<Column> uniqueColumns = ukCon.getColumns();

		    Collections.reverse(uniqueColumns);

		    for (Column ukCol : uniqueColumns) {

			Parameter param = EAMethodUtil.createEAParameter(m, ukCol.getName());

			EAParameterUtil.setEAType(param, mapDataType(ukCol));
		    }

		} catch (EAException e) {
		    result.addError(this, 110, constr.getName(), table.getName(), e.getMessage());
		}
	    }
	}
    }

    /**
     * @param o
     * @return The string used by EA to represent the given foreign key constraint
     *         referential action. If the action is <code>null</code>, 'No Action'
     *         is returned.
     */
    private String translateForeignKeyReferentialAction(ForeignKeyConstraint.ReferentialAction o) {

	if (o == ReferentialAction.CASCADE) {
	    return "Cascade";
	} else if (o == ReferentialAction.RESTRICT) {
	    return "Restrict";
	} else if (o == ReferentialAction.SET_DEFAULT) {
	    return "Set Default";
	} else if (o == ReferentialAction.SET_NULL) {
	    return "Set Null";
	} else {
	    // also if o is null
	    return "No Action";
	}
    }

    private String mapDataType(Column col) {

	ColumnDataType coldt = col.getDataType();
	ProcessMapEntry pme = options.targetMapEntry(coldt.getName(), "*");
	String typeName = pme != null ? pme.getTargetType() : coldt.getName();

	return typeName;
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
    public void visit(CreateSchema createSchema) {
	// ignore
    }

    @Override
    public void visit(DropSchema dropSchema) {
	// ignore
    }

    @Override
    public void postprocess() {

	/*
	 * Sort table operations/methods to facilitate comparison with database models
	 * that were imported from actual databases.
	 */

	for (Integer eaElmtID : this.eaElementIDByTable.values()) {

	    Element element = repository.GetElementByID(eaElmtID);

	    Collection<Method> eaMethods = element.GetMethods();

	    List<Method> methods = new ArrayList<Method>();

	    for (short i = 0; i < eaMethods.GetCount(); i++) {
		Method method = eaMethods.GetAt(i);
		methods.add(method);
	    }

	    Collections.sort(methods, new Comparator<Method>() {

		@Override
		public int compare(Method m1, Method m2) {

		    String m1Stereo = m1.GetStereotype();
		    String m2Stereo = m2.GetStereotype();

		    String m1Name = m1.GetName();
		    String m2Name = m2.GetName();

		    if (m1Stereo.equalsIgnoreCase(m2Stereo)) {
			return m1Name.compareTo(m2Name);
		    } else if (m1Stereo.endsWith("PK")) {
			return -1;
		    } else if (m2Stereo.endsWith("PK")) {
			return 1;
		    } else if (m1Stereo.endsWith("check")) {
			return -1;
		    } else if (m2Stereo.endsWith("check")) {
			return 1;
		    } else if (m1Stereo.endsWith("index")) {
			return -1;
		    } else if (m2Stereo.endsWith("index")) {
			return 1;
		    } else if (m1Stereo.endsWith("unique")) {
			return -1;
		    } else if (m2Stereo.endsWith("unique")) {
			return 1;
		    } else {
			return m1Name.compareTo(m2Name);
		    }
		}
	    });

	    /*
	     * now set the positions of the methods according to their order in the sorted
	     * list
	     */
	    try {

		for (int i = 0; i < methods.size(); i++) {
		    Method method = methods.get(i);
		    EAMethodUtil.setEAPos(method, i);
		}

	    } catch (EAException e) {
		result.addError(this, 107, element.GetName(), e.getMessage());
	    }
	}
    }

    @Override
    public void visit(Select select) {

	if (select.getExpression() != null
		&& select.getExpression() instanceof SpatiaLiteCreateSpatialIndexExpression) {

	    SpatiaLiteCreateSpatialIndexExpression expr = (SpatiaLiteCreateSpatialIndexExpression) select
		    .getExpression();

	    Table table = expr.getTable();
	    Column indexCol = expr.getColumn();

	    Element tableElmt = repository.GetElementByID(this.eaElementIDByTable.get(table));

	    int indexCounter;
	    if (spatialIndexCounterByTableName.containsKey(table.getName())) {
		indexCounter = spatialIndexCounterByTableName.get(table.getName());
	    } else {
		indexCounter = 0;
	    }
	    indexCounter++;
	    spatialIndexCounterByTableName.put(table.getName(), indexCounter);

	    String indexName = "spatialIndex" + indexCounter;

	    try {

		/* Create index 'operation' */
		Method m = EAElementUtil.createEAMethod(tableElmt, indexName);
		EAMethodUtil.setEAStereotypeEx(m, "EAUML::index");

		Parameter param = EAMethodUtil.createEAParameter(m, indexCol.getName());

		EAParameterUtil.setEAType(param, mapDataType(indexCol));

	    } catch (EAException e) {
		result.addError(this, 112, indexName, table.getName(), indexCol.getName(), e.getMessage());
	    }
	}
    }

    @Override
    public void visit(SQLitePragma sqLitePragma) {
	// ignore
    }

    @Override
    public void visit(PostgreSQLAlterRole postgreSQLAlterRole) {
	// ignore
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
	    return "Package with name '$1$' already exists. It will be deleted.";
	case 101:
	    return "Required configuration file '$1$' does not exist.";
	case 102:
	    return "Could not create temporary local copy of XMI file with data model pattern (at '$1$'). Exception message is: $2$";
	case 103:
	    return "Importing data model pattern from '$1$' was not successful. Error message is: $2$";
	case 104:
	    return "Exception encountered while creating table '$1$'. Exception message: '$2$'";
	case 105:
	    return "Exception encountered while creating check constraint '$1$' (operation) on table '$2$'. Exception message: '$3$'";
	case 106:
	    return "Exception encountered while creating foreign key constraint '$1$' on table '$2$'. Exception message: '$3$'";
	case 107:
	    return "??Postprocessing - Exception encountered while updating position of methods in table '$1$'. Exception message: '$2$'";
	case 108:
	    return "Could not create foreign key constraint '$1$' on table '$2$' because no primary key method was found on reference table '$3$'.";
	case 109:
	    return "Exception encountered while creating index '$1$' on table '$2$'. Exception message: '$3$'";
	case 110:
	    return "Exception encountered while creating unique constraint '$1$' on table '$2$'. Exception message: '$3$'";
	case 111:
	    return "Could not create foreign key constraint '$1$' on table '$2$' because no ElementID was found for referenced table '$3$'. Does that table belong to a schema that was not selected for processing?";
	case 112:
	    return "Exception encountered while creating spatial index '$1$' on table '$2$' (column '$3$') from SpatiaLite CreateSpatialIndex. Exception message: '$4$'";

	default:
	    return "(" + DatabaseModelVisitor.class.getName() + ") Unknown message with number: " + mnr;
	}
    }
}
