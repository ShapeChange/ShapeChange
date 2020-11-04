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

import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.TargetOutputProcessor;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Alter;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Column;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.CreateIndex;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.CreateSchema;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.CreateTable;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.DropSchema;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Insert;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.PostgreSQLAlterRole;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.SQLitePragma;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Select;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Statement;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.StatementVisitor;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Table;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.Type;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class ReplicationSchemaVisitor implements StatementVisitor, MessageSource {

    protected SqlDdl sqlddl;
    protected SqlBuilder sqlbuilder;

    protected Options options;
    protected ShapeChangeResult result;

    protected Model model;
    protected SortedSet<ClassInfo> enumerationsInSchema = new TreeSet<ClassInfo>(new Comparator<ClassInfo>() {

	@Override
	public int compare(ClassInfo ci1, ClassInfo ci2) {
	    return ci1.name().compareTo(ci2.name());
	}
    });

    protected Document document;
    protected Element root;
    protected String targetNamespace;

    private TreeSet<PackageInfo> repSchemaPackagesForImport = new TreeSet<PackageInfo>(new Comparator<PackageInfo>() {

	@Override
	public int compare(PackageInfo pi1, PackageInfo pi2) {
	    return pi1.name().compareTo(pi2.name());
	}
    });

    public ReplicationSchemaVisitor(SqlDdl sqlddl, SqlBuilder sqlbuilder) throws ShapeChangeAbortException {

	this.sqlddl = sqlddl;
	this.sqlbuilder = sqlbuilder;

	this.options = sqlddl.options;
	this.result = sqlddl.result;

	this.model = SqlDdl.model;

	// Identify the enumerations contained in the schema
	for (ClassInfo ci : model.selectedSchemaClasses()) {
	    if (ci.category() == Options.ENUMERATION) {
		this.enumerationsInSchema.add(ci);
	    }
	}

	// ======================================
	// Set up the document and create root
	// ======================================

	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	dbf.setNamespaceAware(true);
	dbf.setValidating(true);
	dbf.setAttribute(Options.JAXP_SCHEMA_LANGUAGE, Options.W3C_XML_SCHEMA);

	DocumentBuilder db;
	try {
	    db = dbf.newDocumentBuilder();
	} catch (ParserConfigurationException e) {
	    result.addFatalError(null, 2);
	    throw new ShapeChangeAbortException();
	}

	document = db.newDocument();

	root = document.createElementNS(Options.W3C_XML_SCHEMA, "schema");
	document.appendChild(root);

	addAttribute(root, "xmlns", Options.W3C_XML_SCHEMA);
	addAttribute(root, "elementFormDefault", "qualified");

	addAttribute(root, "version", SqlDdl.repSchemaTargetVersion);
	targetNamespace = SqlDdl.repSchemaTargetNamespace + SqlDdl.repSchemaTargetNamespaceSuffix;
	addAttribute(root, "targetNamespace", targetNamespace);
	addAttribute(root, "xmlns:" + SqlDdl.repSchemaTargetXmlns, targetNamespace);

	if (options.getCurrentProcessConfig().parameterAsString(TargetOutputProcessor.PARAM_ADD_COMMENT, null, false,
		true) == null) {
	    Comment generationComment = document
		    .createComment("XML Schema document created by ShapeChange - http://shapechange.net/");
	    root.appendChild(generationComment);
	}
    }

    @Override
    public void visit(Insert insert) {
	// ignore

    }

    @Override
    public void visit(CreateIndex createIndex) {
	// ignore

    }

    @Override
    public void visit(CreateTable createTable) {

	Table table = createTable.getTable();

	// ignore tables that represent code lists
	if (!table.isAssociativeTable() && table.getRepresentedClass().category() == Options.CODELIST) {
	    return;
	}

	// ---------------------------------------------
	// Create global element to represent the table
	Element globalElement = document.createElementNS(Options.W3C_XML_SCHEMA, "element");
	document.getDocumentElement().appendChild(globalElement);

	addAttribute(globalElement, "name", table.getName());
	addAttribute(globalElement, "type", SqlDdl.repSchemaTargetXmlns + ":" + table.getName() + "Type");

	// -------------------------------------------
	// Add global identifier annotation for table
	String globalIdForGlobalElement = table.getGlobalId();

	Element annotationGlobalElement = addAnnotation(globalElement);
	addGlobalId(annotationGlobalElement, globalIdForGlobalElement);

	// -----------------------------------------------------------
	// Create global complex type to represent the table contents
	Element complexTypeElement = document.createElementNS(Options.W3C_XML_SCHEMA, "complexType");
	document.getDocumentElement().appendChild(complexTypeElement);

	addAttribute(complexTypeElement, "name", table.getName() + "Type");

	Element sequence = document.createElementNS(Options.W3C_XML_SCHEMA, "sequence");
	complexTypeElement.appendChild(sequence);

	for (Column column : table.getColumns()) {

	    /*
	     * NOTE: this can be null (e.g. in case of object identifier columns)
	     */
	    PropertyInfo propForColumn = column.getRepresentedProperty();

	    Integer sizeForFieldWithCharacterDataType = null;
	    if (propForColumn != null) {
		String taggedValueSize = propForColumn.taggedValuesAll().getFirstValue(SqlConstants.PARAM_SIZE);
		if (StringUtils.isNotBlank(taggedValueSize)) {
		    try {
			sizeForFieldWithCharacterDataType = Integer.parseInt(taggedValueSize);
		    } catch (NumberFormatException e) {
			result.addError(this, 4, taggedValueSize, e.getMessage());
		    }
		}
	    }

	    Element columnDefinitionElement = document.createElementNS(Options.W3C_XML_SCHEMA, "element");
	    sequence.appendChild(columnDefinitionElement);

	    addAttribute(columnDefinitionElement, "name", column.getName());

	    /*
	     * Identify the type of the element
	     */
	    String type;

	    if (column.isObjectIdentifierColumn()) {

		type = SqlDdl.repSchemaObjectIdentifierFieldType;

	    } else if (propForColumn != null) {

		Type ti = propForColumn.typeInfo();
		ProcessMapEntry pme = options.targetMapEntry(ti.name, propForColumn.encodingRule("sql"));

		if (pme != null) {

		    if (pme.hasTargetType()) {

			type = pme.getTargetType();

		    } else {

			type = "fixme:fixme";
			result.addError(this, 2, ti.name, type);
		    }

		} else {

		    if (propForColumn.categoryOfValue() == Options.ENUMERATION) {

			// get enumeration class
			ClassInfo enumeration = getValueType(propForColumn);

			/*
			 * Enumerations may reside in another namespace, they are usually not flattened
			 */
			if (model.isInSelectedSchemas(enumeration)) {
			    type = SqlDdl.repSchemaTargetXmlns + ":" + enumeration.name() + "Type";
			} else {
			    type = enumeration.qname() + "Type";
			    repSchemaPackagesForImport.add(enumeration.pkg());
			}

		    } else if (propForColumn.categoryOfValue() == Options.CODELIST) {

			type = "string";

		    } else {

			type = SqlDdl.repSchemaForeignKeyFieldType;
		    }
		}

	    } else if (column.isForeignKeyColumn()) {

		type = SqlDdl.repSchemaForeignKeyFieldType;

	    } else {

		type = "fixme:fixme";

		result.addError(this, 3, column.getName(), table.getName(), type);
	    }

	    addAttribute(columnDefinitionElement, "type", type);

	    if (propForColumn != null) {

		// Handle minOccurs
		if (!table.isAssociativeTable() && (propForColumn.cardinality().minOccurs == 0
			|| propForColumn.matches(ReplicationSchemaConstants.RULE_TGT_SQL_PROP_REPSCHEMA_OPTIONAL))) {

		    addAttribute(columnDefinitionElement, "minOccurs", "0");
		}

		// Handle nillable
		if (propForColumn.matches(ReplicationSchemaConstants.RULE_TGT_PROP_REPSCHEMA_NILLABLE)
			&& !column.isNotNull()) {

		    addAttribute(columnDefinitionElement, "nillable", "true");
		}
	    }

	    /*
	     * Determine and add annotation:
	     * 
	     * - global identifier for column, if the column represents a property from the
	     * model (it can also be an identifier column)
	     * 
	     * - documentation, if required
	     * 
	     * - geometry type and srid, if required
	     */
	    String globalIdForColumnElement = null;
	    String documentationForColumnElement = null;
	    String geometryType = null;

	    if (propForColumn != null) {

		globalIdForColumnElement = propForColumn.globalIdentifier();

		if (sizeForFieldWithCharacterDataType != null && sizeForFieldWithCharacterDataType < 1
			&& propForColumn.matches(
				ReplicationSchemaConstants.RULE_TGT_SQL_PROP_REPSCHEMA_DOCUMENTATION_UNLIMITEDLENGTHCHARACTERDATATYPE)) {
		    documentationForColumnElement = SqlDdl.repSchemaDocumentationUnlimitedLengthCharacterDataType;
		}

		if (propForColumn.typeInfo().name.startsWith("GM_") && propForColumn
			.matches(ReplicationSchemaConstants.RULE_TGT_SQL_PROP_REPSCHEMA_GEOMETRY_ANNOTATION)) {
		    geometryType = propForColumn.typeInfo().name;
		}
	    }

	    if (globalIdForColumnElement != null || documentationForColumnElement != null || geometryType != null) {

		Element annotationColumnElement = addAnnotation(columnDefinitionElement);

		addGlobalId(annotationColumnElement, globalIdForColumnElement);

		addDocumentation(annotationColumnElement, documentationForColumnElement);

		addGeometryAnnotation(annotationColumnElement, geometryType);
	    }

	    // add maxLength restriction if applicable
	    if (propForColumn != null && sizeForFieldWithCharacterDataType != null
		    && sizeForFieldWithCharacterDataType > 0
		    && propForColumn.matches(ReplicationSchemaConstants.RULE_TGT_SQL_PROP_REPSCHEMA_MAXLENGTHFROMSIZE)
		    && propForColumn.categoryOfValue() != Options.ENUMERATION) {

		Element simpleType = document.createElementNS(Options.W3C_XML_SCHEMA, "simpleType");
		columnDefinitionElement.appendChild(simpleType);

		Element restriction = document.createElementNS(Options.W3C_XML_SCHEMA, "restriction");
		simpleType.appendChild(restriction);

		// use identified type in restriction
		columnDefinitionElement.removeAttribute("type");
		addAttribute(restriction, "base", type);

		Element concreteRestriction = document.createElementNS(Options.W3C_XML_SCHEMA, "maxLength");
		addAttribute(concreteRestriction, "value", "" + sizeForFieldWithCharacterDataType);
		restriction.appendChild(concreteRestriction);
	    }
	}

    }

    /**
     * Adds an 'appinfo' element to the given annotation element, with two children:
     * an element with the given geometry type and an element with the configured
     * srid.
     * 
     * @param annotation   an 'annotation' element, must not be <code>null</code>
     * @param geometryType the geometry type to add, may be <code>null</code> (in
     *                     that case, no 'appinfo' is added)
     */
    private void addGeometryAnnotation(Element annotation, String geometryType) {

	if (geometryType != null) {

	    Element eAppInfo = document.createElementNS(Options.W3C_XML_SCHEMA, "appinfo");

	    Element eGeomType = document.createElementNS(Options.SCAI_NS, "geometryType");
	    eGeomType.appendChild(document.createTextNode(geometryType));
	    eAppInfo.appendChild(eGeomType);

	    Element eSrid = document.createElementNS(Options.SCAI_NS, "srid");
	    eSrid.appendChild(document.createTextNode("" + SqlDdl.srid));
	    eAppInfo.appendChild(eSrid);

	    annotation.appendChild(eAppInfo);
	}
    }

    /**
     * Gets the ClassInfo object that is the value type of the property.
     * 
     * @param pi
     * @return the ClassInfo that is the value type of the given property, may be
     *         <code>null</code> if it could not be found in the model
     */
    private ClassInfo getValueType(PropertyInfo pi) {

	Type ti = pi.typeInfo();

	ClassInfo typeCi = pi.model().classById(ti.id);

	if (typeCi == null) {

	    // try to get by-name if link by-id is broken
	    typeCi = pi.model().classByName(ti.name);

	    if (typeCi != null) {
		MessageContext mc = result.addError(null, 135, pi.name());
		if (mc != null)
		    mc.addDetail(null, 400, "Property", pi.fullNameInSchema());
	    }
	}

	if (typeCi == null) {

	    MessageContext mc = result.addError(null, 131, pi.name(), ti.name);
	    if (mc != null)
		mc.addDetail(null, 400, "Property", pi.fullNameInSchema());

	}

	return typeCi;
    }

    /**
     * Adds an 'appinfo' element with the global ID to the given annotation element.
     * 
     * @param annotation - an 'annotation' element, must not be <code>null</code>
     * @param globalId   - the globalId to add, may be <code>null</code> (in that
     *                   case, no 'appinfo' is added)
     */
    private void addGlobalId(Element annotation, String globalId) {

	if (globalId != null) {

	    Element eAppInfo = document.createElementNS(Options.W3C_XML_SCHEMA, "appinfo");
	    eAppInfo.appendChild(document.createTextNode(globalId));

	    annotation.appendChild(eAppInfo);
	}
    }

    /**
     * Adds a 'documentation' element to the given annotation element.
     * 
     * @param annotation    - an 'annotation' element, must not be <code>null</code>
     * @param documentation - the documentation to add, may be <code>null</code> (in
     *                      that case, no 'documentation' is added)
     */
    private void addDocumentation(Element annotation, String documentation) {

	if (documentation != null) {

	    Element eDoc = document.createElementNS(Options.W3C_XML_SCHEMA, "documentation");
	    eDoc.appendChild(document.createTextNode(documentation));

	    annotation.appendChild(eDoc);
	}
    }

    /**
     * Adds an 'annotation' element as child to the given element.
     * 
     * @param e
     * @return the new 'annotation' element
     */
    private Element addAnnotation(Element e) {

	Element res = document.createElementNS(Options.W3C_XML_SCHEMA, "annotation");
	e.appendChild(res);
	return res;
    }

    /**
     * Adds a global simpleType declaration representing the given enumeration to
     * the document.
     * 
     * @param ci must be an enumeration, otherwise this method does not add anything
     */
    private void addGlobalEnumeration(ClassInfo ci) {

	if (ci.category() == Options.ENUMERATION) {

	    Element e1 = document.createElementNS(Options.W3C_XML_SCHEMA, "simpleType");
	    Element e4 = document.createElementNS(Options.W3C_XML_SCHEMA, "restriction");

	    e1.appendChild(e4);
	    addAttribute(e4, "base", "string");

	    for (PropertyInfo atti : ci.properties().values()) {

		Element e3 = document.createElementNS(Options.W3C_XML_SCHEMA, "enumeration");
		e4.appendChild(e3);
		String val = atti.name();
		if (atti.initialValue() != null) {
		    val = atti.initialValue();
		}
		addAttribute(e3, "value", val);
	    }

	    document.getDocumentElement().appendChild(e1);
	    addAttribute(e1, "name", ci.name() + "Type");
	}
    }

    @Override
    public void visit(Alter alter) {
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

    /**
     * Add attribute to an element
     * 
     * @param e     tbd
     * @param name  tbd
     * @param value tbd
     */
    protected void addAttribute(Element e, String name, String value) {
	Attr att = document.createAttribute(name);
	att.setValue(value);
	e.setAttributeNode(att);
    }

    /**
     * @return the document that was created by this visitor
     */
    public Document getDocument() {

	addImports();

	// Generate identified enumerations
	for (ClassInfo enumeration : enumerationsInSchema) {
	    addGlobalEnumeration(enumeration);
	}

	return document;
    }

    /**
     * Add &lt;import&gt; tags as the first content in the &lt;schema&gt; tag.
     */
    private void addImports() {

	Node anchor = null;

	for (PackageInfo packageInfo : repSchemaPackagesForImport) {

	    addAttribute(root, "xmlns:" + packageInfo.xmlns(),
		    packageInfo.targetNamespace() + SqlDdl.repSchemaTargetNamespaceSuffix);

	    Element importElement = document.createElementNS(Options.W3C_XML_SCHEMA, "import");

	    addAttribute(importElement, "namespace",
		    packageInfo.targetNamespace() + SqlDdl.repSchemaTargetNamespaceSuffix);

	    if (anchor == null) {
		root.insertBefore(importElement, root.getFirstChild());
	    } else {
		root.insertBefore(importElement, anchor.getNextSibling());
	    }
	    anchor = importElement;
	}
    }

    @Override
    public void visit(de.interactive_instruments.ShapeChange.Target.SQL.structure.Comment comment) {
	// ignore
    }

    @Override
    public void visit(SQLitePragma sqLitePragma) {
	// ignore
    }

    @Override
    public void postprocess() {
	// ignore
    }

    @Override
    public void visit(Select select) {
	//
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

    @Override
    public String message(int mnr) {

	switch (mnr) {
	case 1:
	    return "Could not find enumeration '$1$' in the model.";
	case 2:
	    return "??No target type defined in map entry with type '$1$'. Using '$2$' instead.";
	case 3:
	    return "Column '$1$' in table '$2$' neither represents a specific property nor an object identifier. Using '$3$' as type.";
	case 4:
	    return "Could not parse tagged value size (value = $1$, error message = $2$)";
	case 100:
	    return "Context: $1$";
	default:
	    return "(" + ReplicationSchemaVisitor.class.getName() + ") Unknown message with number: " + mnr;
	}
    }
}
