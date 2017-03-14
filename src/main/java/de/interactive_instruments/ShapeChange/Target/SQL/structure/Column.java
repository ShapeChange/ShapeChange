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

import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.SQL.expressions.Expression;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class Column {

	private String name = null;
	private List<String> specifications = new ArrayList<String>();
	private ColumnDataType dataType = null;
	private Expression defaultValue = null;
	private PropertyInfo representedProperty = null;
	private Table inTable = null;
	private boolean isObjectIdentifierColumn = false;

	public Column(String name, Table inTable) {
		this.name = name;
		this.inTable = inTable;
	}
	
	public Column(String name, PropertyInfo representedProperty, Table inTable) {
		this.name = name;
		this.representedProperty = representedProperty;
		this.inTable = inTable;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the constraints
	 */
	public List<String> getSpecifications() {
		return specifications;
	}

	/**
	 * @param specifications
	 *            the specifications to set
	 */
	public void setSpecifications(List<String> specifications) {
		this.specifications = specifications;
	}

	public void addSpecification(String spec) {
		this.specifications.add(spec);
	}

	/**
	 * @return the dataType
	 */
	public ColumnDataType getDataType() {
		return dataType;
	}

	/**
	 * @param dataType
	 *            the dataType to set
	 */
	public void setDataType(ColumnDataType dataType) {
		this.dataType = dataType;
	}

	public String toString() {
		return name;
	}

	/**
	 * @return the defaultValue
	 */
	public Expression getDefaultValue() {
		return defaultValue;
	}

	/**
	 * @param defaultValue the defaultValue to set
	 */
	public void setDefaultValue(Expression defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * @return the representedProperty
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

	/**
	 * @return the inTable
	 */
	public Table getInTable() {
		return inTable;
	}

	/**
	 * @param inTable the inTable to set
	 */
	public void setInTable(Table inTable) {
		this.inTable = inTable;
	}
	
	public boolean belongsToAssociativeTable(Column c) {
		
		return c.getInTable().isAssociativeTable();
	}

	public void setObjectIdentifierColumn(boolean isObjectIdentifierColumn) {
		this.isObjectIdentifierColumn = isObjectIdentifierColumn;
	}
	
	public boolean isObjectIdentifierColumn() {
		return isObjectIdentifierColumn;
	}
}
