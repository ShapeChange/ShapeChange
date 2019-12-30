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
package de.interactive_instruments.ShapeChange.Target.SQL.expressions;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
public class SdoDimElement {

	private String dimName = null;
	private String lowerBound = null;
	private String upperBound = null;
	private String tolerance = null;

	/**
	 * @return the dimName
	 */
	public String getDimName() {
		return dimName;
	}

	/**
	 * @param dimName
	 *            the dimName to set
	 */
	public void setDimName(String dimName) {
		this.dimName = dimName;
	}

	/**
	 * @return the lowerBound
	 */
	public String getLowerBound() {
		return lowerBound;
	}

	/**
	 * @param lowerBound
	 *            the lowerBound to set
	 */
	public void setLowerBound(String lowerBound) {
		this.lowerBound = lowerBound;
	}

	/**
	 * @return the upperBound
	 */
	public String getUpperBound() {
		return upperBound;
	}

	/**
	 * @param upperBound
	 *            the upperBound to set
	 */
	public void setUpperBound(String upperBound) {
		this.upperBound = upperBound;
	}

	/**
	 * @return the tolerance
	 */
	public String getTolerance() {
		return tolerance;
	}

	/**
	 * @param tolerance
	 *            the tolerance to set
	 */
	public void setTolerance(String tolerance) {
		this.tolerance = tolerance;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("MDSYS.SDO_DIM_ELEMENT('");
		sb.append(this.getDimName());
		sb.append("', ");
		sb.append(this.getLowerBound());
		sb.append(", ");
		sb.append(this.getUpperBound());
		sb.append(", ");
		sb.append(this.getTolerance());
		sb.append(")");
		return sb.toString();
	}
}
