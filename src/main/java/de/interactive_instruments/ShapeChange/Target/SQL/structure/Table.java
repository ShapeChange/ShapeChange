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
package de.interactive_instruments.ShapeChange.Target.SQL.structure;

import java.util.ArrayList;
import java.util.List;

import de.interactive_instruments.ShapeChange.Model.AssociationInfo;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class Table {

	private String name = null;
	private List<Column> columns = new ArrayList<Column>();
	private List<SqlConstraint> constraints = new ArrayList<SqlConstraint>();

	private boolean isAssociativeTable = false;
	private ClassInfo representedClass = null;
	private AssociationInfo representedAssociation = null;
	private PropertyInfo representedProperty = null;
	private boolean representsCodeStatusCLType = false;

	public Table(String tableName) {
		this.name = tableName;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
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
	 * @param columns
	 *            the columns to set
	 */
	public void setColumns(List<Column> columns) {
		if (columns != null) {
			this.columns = columns;
		} else {
			this.columns = new ArrayList<Column>();
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
	 * @param constraints
	 *            the constraints to set
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
	 * @param isAssociativeTable
	 *            the isAssociativeTable to set
	 */
	public void setAssociativeTable(boolean isAssociativeTable) {
		this.isAssociativeTable = isAssociativeTable;
	}

	/**
	 * @return the representedClass
	 */
	public ClassInfo getRepresentedClass() {
		return representedClass;
	}

	/**
	 * @param representedClass
	 *            the representedClass to set
	 */
	public void setRepresentedClass(ClassInfo representedClass) {
		this.representedClass = representedClass;
	}

	/**
	 * @return the representedAssociation
	 */
	public AssociationInfo getRepresentedAssociation() {
		return representedAssociation;
	}

	/**
	 * @param representedAssociation
	 *            the representedAssociation to set
	 */
	public void setRepresentedAssociation(
			AssociationInfo representedAssociation) {
		this.representedAssociation = representedAssociation;
	}

	/**
	 * @return the representedProperty
	 */
	public PropertyInfo getRepresentedProperty() {
		return representedProperty;
	}

	/**
	 * @param representedProperty
	 *            the representedProperty to set
	 */
	public void setRepresentedProperty(PropertyInfo representedProperty) {
		this.representedProperty = representedProperty;
	}

	public boolean representsClass(ClassInfo ci) {
		return (ci != null && this.representedClass != null
				&& ci == this.representedClass);
	}

	/**
	 * @return the global ID to represent the table, may be <code>null</code> if
	 *         no such ID could be determined
	 */
	public String getGlobalId() {

		String globalId = null;

		if (representedClass != null) {

			globalId = representedClass.globalIdentifier();

		} else if (representedProperty != null
				&& representedProperty.inClass().globalIdentifier() != null
				&& representedProperty.globalIdentifier() != null) {

			globalId = representedProperty.inClass().globalIdentifier() + "."
					+ representedProperty.globalIdentifier();

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

	public void setRepresentsCodeStatusCLType(
			boolean representsCodeStatusCLType) {
		this.representsCodeStatusCLType = representsCodeStatusCLType;
	}
}
