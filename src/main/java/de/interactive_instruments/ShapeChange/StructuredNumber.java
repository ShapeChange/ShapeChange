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
 * This class represents structured integers of the form x.y.z..., where the
 * number of components is determined at object creation time.
 */
public class StructuredNumber implements Comparable<StructuredNumber> {

    /** The component array */
    public int[] components = null;

    /**
     * Out-of-string ctor. This converts strings of the form x.y.z. Note that this
     * covers also simple integers coded as strings.
     * 
     * @param sn string of the form x.y.z
     */
    public StructuredNumber(String sn) {
	int[] newComponents = componentsFromString(sn);
	this.components = newComponents;	
    }

    /**
     * Out-of-integer ctor. Make a StructuredNumber from from one single integer.
     * 
     * @param n integer value
     */
    public StructuredNumber(int n) {
	components = new int[1];
	components[0] = n;
    }

    /**
     * Comparison method. Comparison proceeds from front to back, while there are
     * components existing in both comparands. When the common part turns out to be
     * equal, the longer comparand will be regarded the one with the higher value.
     * Only comparands of equal length can compare with equal result.
     * 
     * @param sn other structured number to compare with
     * 
     */
    public int compareTo(StructuredNumber sn) {

	// Determine lengths
	int lth = components.length;
	int lsn = sn.components.length;
	int len = lth <= lsn ? lth : lsn;

	// Loop over common part
	for (int i = 0; i < len; i++) {

	    // Compare component
	    if (components[i] < sn.components[i]) {
		return -1;
	    } else if (components[i] > sn.components[i]) {
		return 1;
	    } else {
		/*
		 * this component is equal, compare the next one or compare their length
		 */
	    }
	}

	// Common part is equal, decide on lengths
	if (lth < lsn) {
	    return -1;
	} else if (lth > lsn) {
	    return 1;
	} else {
	    return 0;
	}
    }

    public String getString() {
	StringBuffer sb = new StringBuffer();
	for (int i = 0; i < components.length - 1; i++) {
	    sb.append(components[i] + ".");
	}
	sb.append(components[components.length - 1]);
	return sb.toString();
    }

    /**
     * Creates a copy of this StructuredNumber, appending the given number. For
     * example: if this StructuredNumber represents "1.1.1" and "3" is given as
     * parameter, the result is a new StructuredNumber representing "1.1.1.3".
     * 
     * @param number the integer to append
     * @return a copy of this StructuredNumber, with the given number appended to
     *         it.
     */
    public StructuredNumber createCopyWithSuffix(int number) {

	return new StructuredNumber(this.getString() + "." + number);
    }

    public StructuredNumber createCopy() {
	return new StructuredNumber(this.getString());
    }

    /**
     * Checks if this StructuredNumber is equal to the given one. This is true if
     * the StructuredNumbers have the same components (length and content).
     * 
     * @param sn tbd
     * @return tbd
     */
    public boolean equals(StructuredNumber sn) {
	if (sn == null)
	    return false;
	else if (this.components.length != sn.components.length)
	    return false;
	else {
	    boolean result = true;
	    for (int i = 0; i < components.length; i++) {
		if (this.components[i] != sn.components[i]) {
		    result = false;
		    break;
		}
	    }
	    return result;
	}
    }

    public String toString() {
	return this.getString();
    }
    
    public void with(String sn) {
	int[] newComponents = componentsFromString(sn);
	this.components = newComponents;
    }

    private int[] componentsFromString(String sn) {

	String[] cmp = sn.split("\\.");
	int[] newComponents = new int[cmp.length];
	for (int i = 0; i < cmp.length; i++) {
	    try {
		newComponents[i] = Integer.parseInt(cmp[i]);
	    } catch (NumberFormatException e) {
		newComponents[i] = 0;
	    }
	}
	return newComponents;
    }
}
