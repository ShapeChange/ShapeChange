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
 * (c) 2002-2012 interactive instruments GmbH, Bonn, Germany
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

package de.interactive_instruments.ShapeChange;

/**
 * Represents minimum and maximum validity. Default is "1..1". Unbounded multiplicity is represented by Integer.MAX_VALUE.
 */
public class Multiplicity {
	public int minOccurs = 1;
	public int maxOccurs = 1;

	/**
	 * Creates multiplicity with minOccurs = 1 and maxOccurs = 1
	 */
	public Multiplicity() {
	}
	
	public Multiplicity(int minOccurs, int maxOccurs) {
		this.minOccurs = minOccurs;
		this.maxOccurs = maxOccurs;
	}
	
	public Multiplicity(String multiplicityRanges) {
		String[] ranges = multiplicityRanges.split(",");
		int minv = Integer.MAX_VALUE;
		int maxv = Integer.MIN_VALUE;
		int lower;
		int upper;
		for (int i = 0; i < ranges.length; i++) {
			if (ranges[i].indexOf("..") > 0) {
				String[] minmax = ranges[i].split("\\.\\.", 2);
				lower = Multiplicity.boundFromString(minmax[0],0);
				upper = Multiplicity.boundFromString(minmax[1],Integer.MAX_VALUE);
			} else {
				if (ranges[i].length() == 0 || ranges[i].equals("*") || ranges[i].equals("n")) {
					lower = 0;
					upper = Integer.MAX_VALUE;
				} else {
					lower = Integer.parseInt(ranges[i]);
					upper = lower;
				}
			}
			if (lower < minv && lower >= 0) {
				minv = lower;
			}
			if (upper < 0) {
				maxv = Integer.MAX_VALUE;
			}
			if (upper > maxv) {
				maxv = upper;
			}
		}
		minOccurs = minv;
		maxOccurs = maxv;		
	}
	
	public String toString() {
		return minOccurs + (maxOccurs==minOccurs ? "" : ".." + (maxOccurs==Integer.MAX_VALUE ? "*" : maxOccurs));
	}	

	static public int boundFromString(String bound, int def) throws NumberFormatException {
		int res;
		if(bound.length()==0)
			res = def;
		else if(bound.equals("*") || bound.equals("n"))
			res = Integer.MAX_VALUE;
		else {
			res = Integer.parseInt(bound);
		}
		return res;
	}
}
