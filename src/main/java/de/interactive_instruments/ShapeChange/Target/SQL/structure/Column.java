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
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.SQL.SqlConstants;
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
	private Table inTable = null;

	private PropertyInfo representedProperty = null;
	private ClassInfo enumerationValueType = null;

	private String documentation = null;

	private Table referencedTable = null;
	private boolean isObjectIdentifierColumn = false;
	private boolean isForeignKeyColumn = false;

	public Column(String name, String documentation, Table inTable) {
		this.name = name;
		this.documentation = documentation;
		this.inTable = inTable;
	}

	public Column(String name, PropertyInfo representedProperty, String documentation,
			Table inTable) {
		this.name = name;
		this.representedProperty = representedProperty;	
		this.documentation = documentation;
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
	 * @return the specification of this column; can be empty but not
	 *         <code>null</code>
	 */
	public List<String> getSpecifications() {
		return specifications;
	}

	/**
	 * @param specifications
	 *            the specifications to set
	 */
	public void setSpecifications(List<String> specifications) {
		if (specifications != null) {
			this.specifications = specifications;
		} else {
			this.specifications = new ArrayList<String>();
		}
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
	 * @return the defaultValue, can be <code>null</code>
	 */
	public Expression getDefaultValue() {
		return defaultValue;
	}

	public boolean hasDefaultValue() {
		return defaultValue != null;
	}

	/**
	 * @param defaultValue
	 *            the defaultValue to set
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
	 * @param representedProperty
	 *            the representedProperty to set
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
	 * @param inTable
	 *            the inTable to set
	 */
	public void setInTable(Table inTable) {
		this.inTable = inTable;
	}

	public void setObjectIdentifierColumn(boolean isObjectIdentifierColumn) {
		this.isObjectIdentifierColumn = isObjectIdentifierColumn;
	}

	public boolean isObjectIdentifierColumn() {
		return isObjectIdentifierColumn;
	}

	/**
	 * @return <code>true</code> if this column is intended to store a foreign
	 *         key, otherwise <code>false</code> NOTE: Even if this object does
	 *         not reference a specific table, it may still be intended to be
	 *         used as foreign key (to one or more tables, see
	 *         {@link SqlConstants.RULE_TGT_SQL_CLS_DATATYPES_ONETOMANY_ONETABLE}
	 *         . That a column is a foreign key is of interest when creating a
	 *         replication schema.
	 */
	public boolean isForeignKeyColumn() {
		return isForeignKeyColumn;
	}

	/**
	 * @return <code>true</code> if this column contains a 'PRIMARY KEY'
	 *         specification (case is ignored), else <code>false</code>
	 */
	public boolean isPrimaryKeyColumn() {

		return hasSpecificationIgnoringCase("primary key");
	}

	/**
	 * @return <code>true</code> if the column has a specification containing
	 *         "NOT NULL" (ignoring case), else <code>false</code>.
	 */
	public boolean isNotNull() {
		String columnSpec = getSpecifications() == null ? ""
				: StringUtils.join(getSpecifications(), " ");
		return columnSpec.toLowerCase(Locale.ENGLISH).contains("not null");
	}

	public void setForeignKeyColumn(boolean isForeignKeyColumn) {
		this.isForeignKeyColumn = isForeignKeyColumn;
	}

	/**
	 * @return The table that is referenced by this column (which can be
	 *         represented by a foreign key constraint); can be
	 *         <code>null</code>
	 */
	public Table getReferencedTable() {
		return this.referencedTable;
	}

	public void setReferencedTable(Table refTable) {
		this.referencedTable = refTable;
	}

	public void removeSpecification(String specIn) {
		List<String> result = new ArrayList<String>();
		for (String s : this.specifications) {
			if (!s.equalsIgnoreCase(specIn)) {
				result.add(s);
			}
		}
		this.specifications = result;
	}

	public boolean hasSpecificationIgnoringCase(String specIn) {

		String specInLower = specIn.toLowerCase(Locale.ENGLISH);

		for (String specification : this.specifications) {
			String specificationLower = specification
					.toLowerCase(Locale.ENGLISH);
			if (specificationLower.contains(specInLower)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * @return the enumeration that is the value type of the property
	 *         represented by this column; can be <code>null</code>
	 */
	public ClassInfo getEnumerationValueType() {
		return enumerationValueType;
	}

	public void setEnumerationValueType(ClassInfo enumerationValueType) {
		this.enumerationValueType = enumerationValueType;
	}

	/**
	 * @return Documentation of this column. Can be <code>null</code>.
	 */
	public String getDocumentation() {
		return documentation;
	}

	/**
	 * @param documentation
	 *            Documentation of this column. Can be <code>null</code>.
	 */
	public void setDocumentation(String documentation) {
		this.documentation = documentation;
	}
}
