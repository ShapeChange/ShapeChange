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
 * (c) 2002-2026 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class PropertyRestriction {

    protected String propertyName = null;
    protected List<String> valueTypeRestrictions = new ArrayList<>();

    /**
     * @return the propertyName
     */
    public String getPropertyName() {
	return propertyName;
    }

    /**
     * @return the valueTypeRestrictions
     */
    public List<String> getValueTypeRestrictions() {
	return valueTypeRestrictions;
    }

    /**
     * @param propertyName the propertyName to set
     */
    public void setPropertyName(String propertyName) {
	this.propertyName = propertyName;
    }

    /**
     * @param valueTypeRestrictions the valueTypeRestrictions to set
     */
    public void setValueTypeRestrictions(List<String> valueTypeRestrictions) {
	if (valueTypeRestrictions == null) {
	    this.valueTypeRestrictions = new ArrayList<>();
	} else {
	    this.valueTypeRestrictions = valueTypeRestrictions;
	}
    }

    public boolean hasValueTypeRestrictions() {
	return !this.valueTypeRestrictions.isEmpty();
    }

}
