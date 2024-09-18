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
 * (c) 2002-2014 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.ea.target.arcgisworkspace;

/**
 * @author Johannes Echterhoff
 *
 */
public class NumericRangeConstraintMetadata {

	private Double lowerBoundaryValue;
	private Double upperBoundaryValue;

	private boolean lowerBoundaryInclusive;
	private boolean upperBoundaryInclusive;

	public NumericRangeConstraintMetadata(Double lowerBoundaryValue,
			Double upperBoundaryValue, boolean lowerBoundaryInclusive,
			boolean upperBoundaryInclusive) {
		super();
		this.lowerBoundaryValue = lowerBoundaryValue;
		this.upperBoundaryValue = upperBoundaryValue;
		this.lowerBoundaryInclusive = lowerBoundaryInclusive;
		this.upperBoundaryInclusive = upperBoundaryInclusive;
	}

	/**
	 * @return the lowerBoundaryValue
	 */
	public Double getLowerBoundaryValue() {
		return lowerBoundaryValue;
	}

	/**
	 * @return the upperBoundaryValue
	 */
	public Double getUpperBoundaryValue() {
		return upperBoundaryValue;
	}

	/**
	 * @return the lowerBoundaryInclusive
	 */
	public boolean isLowerBoundaryInclusive() {
		return lowerBoundaryInclusive;
	}

	/**
	 * @return the upperBoundaryInclusive
	 */
	public boolean isUpperBoundaryInclusive() {
		return upperBoundaryInclusive;
	}

	public boolean hasLowerBoundaryValue() {		
		return lowerBoundaryValue != null;
	}

	public boolean hasUpperBoundaryValue() {		
		return upperBoundaryValue != null;
	}
}
