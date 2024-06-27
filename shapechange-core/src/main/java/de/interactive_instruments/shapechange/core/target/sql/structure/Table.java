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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.shapechange.core.target.sql.structure;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.shapechange.core.model.AssociationInfo;
import de.interactive_instruments.shapechange.core.model.ClassInfo;
import de.interactive_instruments.shapechange.core.model.PropertyInfo;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class Table {

    private String schemaName = null;
    private String name = null;
    private String documentation = null;

    private List<Column> columns = new ArrayList<Column>();
    private List<SqlConstraint> constraints = new ArrayList<SqlConstraint>();

    private boolean isAssociativeTable = false;
    private ClassInfo representedClass = null;
    private AssociationInfo representedAssociation = null;
    private PropertyInfo representedProperty = null;
    private boolean representsCodeStatusCLType = false;
    private boolean isUsageSpecific = false;

    public Table(String schemaName, String tableName) {
	this.schemaName = schemaName;
	this.name = tableName;
    }

    /**
     * @return can be empty (but not null) or contain 1 or more columns that
     *         consitute the PK of the table
     */
    public List<Column> getPrimaryKeyColumns() {

	for (SqlConstraint constr : getConstraints()) {
	    if (constr instanceof PrimaryKeyConstraint) {
		return constr.getColumns();
	    }
	}

	List<Column> res = new ArrayList<>();
	
	for (Column col : getColumns()) {
	    if (col.isPrimaryKeyColumn()) {
		res.add(col);
	    }
	}

	return res;
    }

    /**
     * @return the schema name
     */
    public String getSchemaName() {
	return schemaName;
    }

    /**
     * @return the name
     */
    public String getName() {
	return name;
    }

    /**
     * @param schemaName the schema name to set
     */
    public void setSchemaName(String schemaName) {
	this.schemaName = schemaName;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
	this.name = name;
    }

    /**
     * @return the columns of this table; can be empty but not <code>null</code>
     */
    public List<Column> getColumns() {
	return columns;
    }

    /**
     * @param columns the columns to set
     */
    public void setColumns(List<Column> columns) {
	if (columns != null) {
	    this.columns = columns;
	} else {
	    this.columns = new ArrayList<Column>();
	}
    }

    public void removeColumns(List<Column> columnsToRemove) {
	if (columnsToRemove != null) {
	    for (Column ctr : columnsToRemove) {
		this.columns.remove(ctr);
	    }
	}
    }

    /**
     * @return the constraints defined for this table; can be empty but not
     *         <code>null</code>
     */
    public List<SqlConstraint> getConstraints() {
	return constraints;
    }

    /**
     * @param constraints the constraints to set
     */
    public void setConstraints(List<SqlConstraint> constraints) {
	if (constraints == null) {
	    this.constraints = new ArrayList<SqlConstraint>();
	} else {
	    this.constraints = constraints;
	}
    }

    public void addConstraint(SqlConstraint constraint) {
	this.constraints.add(constraint);
    }

    public void addConstraints(List<SqlConstraint> constraints) {
	this.constraints.addAll(constraints);
    }

    public String toString() {
	return name;
    }

    public boolean hasConstraints() {
	return !constraints.isEmpty();
    }

    /**
     * @return the isAssociativeTable
     */
    public boolean isAssociativeTable() {
	return isAssociativeTable;
    }

    /**
     * @param isAssociativeTable the isAssociativeTable to set
     */
    public void setAssociativeTable(boolean isAssociativeTable) {
	this.isAssociativeTable = isAssociativeTable;
    }

    /**
     * @return the representedClass; can be <code>null</code>
     */
    public ClassInfo getRepresentedClass() {
	return representedClass;
    }

    /**
     * @param representedClass the representedClass to set
     */
    public void setRepresentedClass(ClassInfo representedClass) {
	this.representedClass = representedClass;
    }

    /**
     * @return the representedAssociation; can be <code>null</code>
     */
    public AssociationInfo getRepresentedAssociation() {
	return representedAssociation;
    }

    /**
     * @param representedAssociation the representedAssociation to set
     */
    public void setRepresentedAssociation(AssociationInfo representedAssociation) {
	this.representedAssociation = representedAssociation;
    }

    /**
     * @return the representedProperty; can be <code>null</code>
     */
    public PropertyInfo getRepresentedProperty() {
	return representedProperty;
    }

    /**
     * @param representedProperty the representedProperty to set
     */
    public void setRepresentedProperty(PropertyInfo representedProperty) {
	this.representedProperty = representedProperty;
    }

    public boolean representsClass(ClassInfo ci) {
	return (ci != null && this.representedClass != null && ci == this.representedClass);
    }

    /**
     * @return the global ID to represent the table, may be <code>null</code> if no
     *         such ID could be determined
     */
    public String getGlobalId() {

	String globalId = null;

	if (representedClass != null) {

	    globalId = representedClass.globalIdentifier();

	} else if (representedProperty != null && representedProperty.inClass().globalIdentifier() != null
		&& representedProperty.globalIdentifier() != null) {

	    globalId = representedProperty.inClass().globalIdentifier() + "." + representedProperty.globalIdentifier();

	} else if (representedAssociation != null) {

	    globalId = representedAssociation.globalIdentifier();
	}

	return globalId;
    }

    public void addColumn(Column column) {

	this.columns.add(column);
    }

    public boolean representsCodeStatusCLType() {
	return representsCodeStatusCLType;
    }

    public void setRepresentsCodeStatusCLType(boolean representsCodeStatusCLType) {
	this.representsCodeStatusCLType = representsCodeStatusCLType;
    }

    public String getDocumentation() {
	return this.documentation;
    }

    public void setDocumentation(String documentation) {
	this.documentation = documentation;
    }

    public boolean hasSchemaName() {
	return StringUtils.isNotBlank(this.schemaName);
    }

    public String getFullName() {

	if (hasSchemaName()) {
	    return schemaName + "." + name;
	} else {
	    return name;
	}
    }

    /**
     * @return <code>true</code> if this table has been created to encode the value
     *         type of a specific property, <code>false</code> if this table has
     *         been created to encode the value type in general (so can be used as
     *         target of a 1:1 relationship)
     */
    public boolean isUsageSpecificTable() {
	return this.isUsageSpecific;
    }

    /**
     * @param isUsageSpecific <code>true</code> if this table has been created to
     *                        encode the value type of a specific property,
     *                        <code>false</code> if this table has been created to
     *                        encode the value type in general (so can be used as
     *                        target of a 1:1 relationship)
     */
    public void setUsageSpecificTable(boolean isUsageSpecific) {
	this.isUsageSpecific = isUsageSpecific;
    }
}
