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
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange.Target.sql_encoding_util;

import java.io.File;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.ShapeChangeException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.Ldproxy2.PropertyEncodingContext;
import de.interactive_instruments.ShapeChange.Util.XMLUtil;

/**
 * Supports writing a file with SQL encoding infos by targets, and reading such
 * infos from an XML element (contained in the advanced process configuration).
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class SqlEncodingInfos implements MessageSource {

    public static final String SC_CONFIG_NS = "http://www.interactive-instruments.de/ShapeChange/Configuration/1.1";

    protected SortedSet<SqlClassEncodingInfo> classInfos = new TreeSet<>();
    protected SortedSet<SqlPropertyEncodingInfo> propertyInfos = new TreeSet<>();

    public SortedSet<SqlClassEncodingInfo> getSqlClassEncodingInfos() {
	return classInfos;
    }

    public SortedSet<SqlPropertyEncodingInfo> getSqlPropertyEncodingInfos() {
	return propertyInfos;
    }

    public boolean isEmpty() {
	return this.classInfos.isEmpty() && this.propertyInfos.isEmpty();
    }

    public void add(SqlClassEncodingInfo scei) {
	classInfos.add(scei);
    }

    public void add(SqlPropertyEncodingInfo spei) {
	propertyInfos.add(spei);
    }

    public void addSqlClassEncodingInfos(Collection<SqlClassEncodingInfo> sceis) {
	classInfos.addAll(sceis);
    }

    public void addSqlPropertyEncodingInfos(Collection<SqlPropertyEncodingInfo> speis) {
	propertyInfos.addAll(speis);
    }

    /**
     * Look for SqlPropertyEncodingInfos that have same sourceTable as defined by
     * the context, and that have originalPropertyName (if not set, then
     * propertyName) equal to that of pi, and originalInClassName (if not set, then
     * inClassName) equal to that of pi, and originalSchemaName (if not set, then
     * schemaName) equal to that of pi.
     * 
     * @param pi      - tbd
     * @param context - tbd
     * @return - tbd
     */
    public SortedSet<SqlPropertyEncodingInfo> getPropertyEncodingInfos(PropertyInfo pi,
	    PropertyEncodingContext context) {

	SortedSet<SqlPropertyEncodingInfo> result = new TreeSet<>();

	for (SqlPropertyEncodingInfo sei : this.propertyInfos) {

	    String sourceTable = sei.getSourceTable();

	    if (sourceTable.equals(context.getSourceTable())) {

		String name = sei.hasOriginalPropertyName() ? sei.getOriginalPropertyName() : sei.getPropertyName();
		String schema = sei.hasOriginalSchemaName() ? sei.getOriginalSchemaName() : sei.getSchemaName();
		String inClass = sei.hasOriginalInClassName() ? sei.getOriginalInClassName() : sei.getInClassName();

		String piSchema = pi.model().schemaPackage(pi.inClass()).name();

		if (pi.name().equals(name) && piSchema.equals(schema) && pi.inClass().name().equals(inClass)) {
		    result.add(sei);
		}
	    }
	}

	return result;
    }

    public boolean hasClassEncodingInfo(ClassInfo ci) {

	for (SqlClassEncodingInfo sei : this.classInfos) {

	    String name = sei.hasOriginalClassName() ? sei.getOriginalClassName() : sei.getClassName();
	    String schema = sei.hasOriginalSchemaName() ? sei.getOriginalSchemaName() : sei.getSchemaName();

	    String ciSchema = ci.model().schemaPackage(ci).name();

	    if (ci.name().equals(name) && ciSchema.equals(schema)) {
		return true;
	    }
	}

	return false;
    }

    public boolean hasClassEncodingInfoForTable(String targetTableName) {

	for (SqlClassEncodingInfo sei : this.classInfos) {
	    if (sei.getTable().equalsIgnoreCase(targetTableName)) {
		return true;
	    }
	}
	return false;
    }

    public SqlClassEncodingInfo getClassEncodingInfoForTable(String targetTableName) {

	for (SqlClassEncodingInfo sei : this.classInfos) {
	    if (sei.getTable().equalsIgnoreCase(targetTableName)) {
		return sei;
	    }
	}
	return null;
    }

    public SqlClassEncodingInfo getClassEncodingInfo(ClassInfo ci) {

	for (SqlClassEncodingInfo sei : this.classInfos) {

	    String name = sei.hasOriginalClassName() ? sei.getOriginalClassName() : sei.getClassName();
	    String schema = sei.hasOriginalSchemaName() ? sei.getOriginalSchemaName() : sei.getSchemaName();

	    String ciSchema = ci.model().schemaPackage(ci).name();

	    if (ci.name().equals(name) && ciSchema.equals(schema)) {
		return sei;
	    }
	}

	return null;
    }

    public void toXml(File outputFile, ShapeChangeResult result) {

	if (classInfos.isEmpty() && propertyInfos.isEmpty()) {
	    result.addWarning(this, 1, outputFile.getAbsolutePath());
	    return;
	}

	try {

	    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	    dbf.setNamespaceAware(true);
	    dbf.setValidating(true);
	    DocumentBuilder db = dbf.newDocumentBuilder();

	    Document document = db.newDocument();

	    Element root = document.createElementNS(SC_CONFIG_NS, "SqlEncodingInfos");
	    document.appendChild(root);

	    XMLUtil.addAttribute(document, root, "xmlns", SC_CONFIG_NS);

	    Element e1 = document.createElementNS(SC_CONFIG_NS, "modelElementEncodings");
	    root.appendChild(e1);

	    for (SqlClassEncodingInfo sei : classInfos) {

		Element e2 = document.createElementNS(SC_CONFIG_NS, "SqlClassEncodingInfo");
		e1.appendChild(e2);

		XMLUtil.addAttribute(document, e2, "schemaName", StringUtils.stripToEmpty(sei.getSchemaName()));
		if (sei.hasOriginalSchemaName() && !sei.getOriginalSchemaName().equals(sei.getSchemaName())) {
		    XMLUtil.addAttribute(document, e2, "originalSchemaName", sei.getOriginalSchemaName());
		}
		XMLUtil.addAttribute(document, e2, "className", StringUtils.stripToEmpty(sei.getClassName()));
		if (sei.hasOriginalClassName() && !sei.getOriginalClassName().equals(sei.getClassName())) {
		    XMLUtil.addAttribute(document, e2, "originalClassName", sei.getOriginalClassName());
		}
		XMLUtil.addAttribute(document, e2, "tableName", StringUtils.stripToEmpty(sei.getTable()));
		if (sei.hasDatabaseSchema()) {
		    XMLUtil.addAttribute(document, e2, "databaseSchema", sei.getDatabaseSchema());
		}

	    }

	    for (SqlPropertyEncodingInfo sei : propertyInfos) {

		Element e2 = document.createElementNS(SC_CONFIG_NS, "SqlPropertyEncodingInfo");
		e1.appendChild(e2);

		XMLUtil.addAttribute(document, e2, "schemaName", StringUtils.stripToEmpty(sei.getSchemaName()));
		if (sei.hasOriginalSchemaName() && !sei.getOriginalSchemaName().equals(sei.getSchemaName())) {
		    XMLUtil.addAttribute(document, e2, "originalSchemaName", sei.getOriginalSchemaName());
		}
		XMLUtil.addAttribute(document, e2, "propertyName", StringUtils.stripToEmpty(sei.getPropertyName()));
		if (sei.hasOriginalPropertyName() && !sei.getOriginalPropertyName().equals(sei.getPropertyName())) {
		    XMLUtil.addAttribute(document, e2, "originalPropertyName", sei.getOriginalPropertyName());
		}
		XMLUtil.addAttribute(document, e2, "inClassName", StringUtils.stripToEmpty(sei.getInClassName()));
		if (sei.hasOriginalInClassName() && !sei.getOriginalInClassName().equals(sei.getInClassName())) {
		    XMLUtil.addAttribute(document, e2, "originalInClassName", sei.getOriginalInClassName());
		}
		XMLUtil.addAttribute(document, e2, "propertyValueType",
			StringUtils.stripToEmpty(sei.getPropertyValueType()));
		if (sei.hasOriginalPropertyValueType()
			&& !sei.getOriginalPropertyValueType().equals(sei.getPropertyValueType())) {
		    XMLUtil.addAttribute(document, e2, "originalPropertyValueType", sei.getOriginalPropertyValueType());
		}
		XMLUtil.addAttribute(document, e2, "propertyMultiplicity",
			StringUtils.stripToEmpty(sei.getPropertyMultiplicity()));
		if (sei.hasOriginalPropertyMultiplicity()
			&& !sei.getOriginalPropertyMultiplicity().equals(sei.getPropertyMultiplicity())) {
		    XMLUtil.addAttribute(document, e2, "originalPropertyMultiplicity",
			    sei.getOriginalPropertyMultiplicity());
		}
		XMLUtil.addAttribute(document, e2, "sourceTable", StringUtils.stripToEmpty(sei.getSourceTable()));
		if (sei.hasSourceTableSchema()) {
		    XMLUtil.addAttribute(document, e2, "sourceTableSchema", sei.getSourceTableSchema());
		}
		XMLUtil.addAttribute(document, e2, "valueSourcePath",
			StringUtils.stripToEmpty(sei.getValueSourcePath()));
		if (sei.hasIdSourcePath()) {
		    XMLUtil.addAttribute(document, e2, "idSourcePath", StringUtils.stripToEmpty(sei.getIdSourcePath()));
		}
		if (sei.hasTargetTable()) {
		    XMLUtil.addAttribute(document, e2, "targetTable", StringUtils.stripToEmpty(sei.getTargetTable()));
		}
		if (sei.hasTargetTableSchema()) {
		    XMLUtil.addAttribute(document, e2, "targetTableSchema", sei.getTargetTableSchema());
		}
	    }

	    XMLUtil.writeXml(document, outputFile);

	} catch (ShapeChangeException | ParserConfigurationException e) {

	    result.addError(this, 2, outputFile.getAbsolutePath(), e.getMessage());
	}
    }

    public static SqlEncodingInfos fromXml(Element seiElmt) {

	SqlEncodingInfos sei = new SqlEncodingInfos();

	for (Element seie : XMLUtil.getChildElements(seiElmt, "SqlClassEncodingInfo")) {

	    SqlClassEncodingInfo scei = new SqlClassEncodingInfo();

	    scei.setSchemaName(seie.getAttribute("schemaName"));
	    scei.setOriginalSchemaName(StringUtils.stripToNull(seie.getAttribute("originalSchemaName")));

	    scei.setClassName(seie.getAttribute("className"));
	    scei.setOriginalClassName(StringUtils.stripToNull(seie.getAttribute("originalClassName")));

	    scei.setTable(seie.getAttribute("tableName"));
	    scei.setDatabaseSchema(StringUtils.stripToNull(seie.getAttribute("databaseSchema")));

	    sei.add(scei);
	}

	for (Element seie : XMLUtil.getChildElements(seiElmt, "SqlPropertyEncodingInfo")) {

	    SqlPropertyEncodingInfo spei = new SqlPropertyEncodingInfo();

	    spei.setSchemaName(seie.getAttribute("schemaName"));
	    spei.setOriginalSchemaName(StringUtils.stripToNull(seie.getAttribute("originalSchemaName")));

	    spei.setPropertyName(seie.getAttribute("propertyName"));
	    spei.setOriginalPropertyName(StringUtils.stripToNull(seie.getAttribute("originalPropertyName")));

	    spei.setInClassName(seie.getAttribute("inClassName"));
	    spei.setOriginalInClassName(StringUtils.stripToNull(seie.getAttribute("originalInClassName")));

	    spei.setPropertyValueType(seie.getAttribute("propertyValueType"));
	    spei.setOriginalPropertyValueType(StringUtils.stripToNull(seie.getAttribute("originalPropertyValueType")));

	    spei.setPropertyMultiplicity(seie.getAttribute("propertyMultiplicity"));
	    spei.setOriginalPropertyMultiplicity(
		    StringUtils.stripToNull(seie.getAttribute("originalPropertyMultiplicity")));

	    spei.setSourceTable(seie.getAttribute("sourceTable"));
	    spei.setSourceTableSchema(StringUtils.stripToNull(seie.getAttribute("sourceTableSchema")));

	    spei.setValueSourcePath(seie.getAttribute("valueSourcePath"));
	    spei.setIdSourcePath(StringUtils.stripToNull(seie.getAttribute("idSourcePath")));

	    spei.setTargetTable(seie.getAttribute("targetTable"));
	    spei.setTargetTableSchema(StringUtils.stripToNull(seie.getAttribute("targetTableSchema")));

	    sei.add(spei);
	}

	return sei;
    }

    public void merge(SqlEncodingInfos otherSqlEncodingInfos) {
	this.classInfos.addAll(otherSqlEncodingInfos.getSqlClassEncodingInfos());
	this.propertyInfos.addAll(otherSqlEncodingInfos.getSqlPropertyEncodingInfos());
    }

    @Override
    public String message(int mnr) {

	switch (mnr) {

	case 1:
	    return "No SQL encoding infos to write to file '$1$'.";
	case 2:
	    return "Encountered an exception while writing SQL encoding infos to file '$1$'. Exception message is: $2$";

	default:
	    return "(SqlEncodingInfos.java) Unknown message with number: " + mnr;
	}

    }

}
