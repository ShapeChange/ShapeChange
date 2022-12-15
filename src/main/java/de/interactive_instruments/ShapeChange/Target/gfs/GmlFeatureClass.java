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
 * (c) 2002-2022 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target.gfs;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class GmlFeatureClass {

    protected String name = null;
    protected String elementPath = null;
    protected List<GeometryPropertyDefinition> geometryPropertyDefinitions = new ArrayList<>();
    protected List<PropertyDefinition> propertyDefinitions = new ArrayList<>();
    protected String srsName = null;

    /**
     * @return the name
     */
    public String getName() {
	return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
	this.name = name;
    }

    /**
     * @return the elementPath
     */
    public String getElementPath() {
	return elementPath;
    }

    /**
     * @param elementPath the elementPath to set
     */
    public void setElementPath(String elementPath) {
	this.elementPath = elementPath;
    }

    /**
     * @return the geometryPropertyDefinitions; can be empty but not
     *         <code>null</code>
     */
    public List<GeometryPropertyDefinition> getGeometryPropertyDefinitions() {
	return geometryPropertyDefinitions;
    }

    /**
     * @param geometryPropertyDefinitions the geometryPropertyDefinitions to set
     */
    public void setGeometryPropertyDefinitions(List<GeometryPropertyDefinition> geometryPropertyDefinitions) {
	this.geometryPropertyDefinitions = geometryPropertyDefinitions == null ? new ArrayList<>()
		: geometryPropertyDefinitions;
    }

    /**
     * @return the propertyDefinitions; can be empty but not <code>null</code>
     */
    public List<PropertyDefinition> getPropertyDefinitions() {
	return propertyDefinitions;
    }

    /**
     * @param propertyDefinitions the propertyDefinitions to set
     */
    public void setPropertyDefinitions(List<PropertyDefinition> propertyDefinitions) {
	this.propertyDefinitions = propertyDefinitions == null ? new ArrayList<>() : propertyDefinitions;
    }

    /**
     * @return the srsName
     */
    public String getSrsName() {
	return srsName;
    }

    /**
     * @param srsName the srsName to set
     */
    public void setSrsName(String srsName) {
	this.srsName = srsName;
    }

}
