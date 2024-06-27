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

import org.apache.commons.lang3.StringUtils;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot
 *         de)
 *
 */
public class ColumnDataType {

	private String name = null;
	private Integer precision = null;
	private Integer scale = null;
	private Integer length = null;
	private String lengthQualifier = null;

	public ColumnDataType(String name) {
		this.name = name;
	}

	/**
	 * @param name
	 *                            should not be <code>null</code>
	 * @param precision
	 *                            can be <code>null</code>
	 * @param scale
	 *                            can be <code>null</code>
	 * @param length
	 *                            can be <code>null</code>
	 * @param lengthQualifier
	 *                            can be <code>null</code>
	 */
	public ColumnDataType(String name, Integer precision, Integer scale,
			Integer length, String lengthQualifier) {
		this.name = name;
		this.precision = precision;
		this.scale = scale;
		this.length = length;
		this.lengthQualifier = lengthQualifier;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the precision, can be <code>null</code>
	 */
	public Integer getPrecision() {
		return precision;
	}

	public void setPrecision(Integer precision) {
		this.precision = precision;
	}

	public boolean hasPrecision() {
		return this.precision != null;
	}

	/**
	 * @return the scale, can be <code>null</code>
	 */
	public Integer getScale() {
		return scale;
	}

	public void setScale(Integer scale) {
		this.scale = scale;
	}

	public boolean hasScale() {
		return this.scale != null;
	}

	/**
	 * @return the length
	 */
	public Integer getLength() {
		return length;
	}

	public boolean hasLength() {
		return this.length != null;
	}

	public void setLengthQualifier(String lengthQualifier) {
		this.lengthQualifier = lengthQualifier;
	}

	public boolean hasLengthQualifier() {
		return StringUtils.isNotBlank(this.lengthQualifier);
	}

	/**
	 * @return stripped lengthQualifier defined for this data type; can be
	 *         <code>null</code>
	 */
	public String getLengthQualifier() {
		return StringUtils.strip(this.lengthQualifier);
	}
}
