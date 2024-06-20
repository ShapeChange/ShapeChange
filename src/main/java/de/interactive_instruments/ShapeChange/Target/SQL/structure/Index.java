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
package de.interactive_instruments.ShapeChange.Target.SQL.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
public class Index {

	private String name = null;
	private String type = null;
	private List<String> specs = new ArrayList<String>();
	private List<Column> columns = new ArrayList<Column>();
	private Properties properties = new Properties();

	public Index() {

	}

	public Index(String name) {
		this.name = name;
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
	 * @return the type, can be <code>null</code>
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return the specs
	 */
	public List<String> getSpecs() {
		return specs;
	}

	/**
	 * @param specs
	 *            the specs to set
	 */
	public void setSpecs(List<String> specs) {
		this.specs = specs;
	}

	public void addSpec(String spec) {
		this.specs.add(spec);
	}

	/**
	 * @return the columns
	 */
	public List<Column> getColumns() {
		return columns;
	}

	public boolean hasColumns() {
		return columns != null && !columns.isEmpty();
	}

	/**
	 * @param columns
	 *            the columns to set
	 */
	public void setColumns(List<Column> columns) {
		this.columns = columns;
	}

	public void addColumn(Column column) {
		this.columns.add(column);
	}

	public boolean hasSpecs() {
		return specs != null && !specs.isEmpty();
	}

	/**
	 * @return the properties; can be empty but not <code>null</code>
	 */
	public Properties getProperties() {
		return properties;
	}

}
