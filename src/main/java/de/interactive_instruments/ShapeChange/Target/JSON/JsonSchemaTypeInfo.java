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
 * (c) 2002-2020 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target.JSON;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.ShapeChange.Target.JSON.jsonschema.JsonSchemaType;

/**
 * Provides information about the JSON Schema type to which the value type of a
 * UML property is converted. The information is used to define the type
 * definition for the property, but can also be used for other conversion steps,
 * such as defining a default value.
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class JsonSchemaTypeInfo {
    
    protected String ref = null;
    protected JsonSchemaType simpleType = null;
    protected String format = null;
    protected boolean isGeometry = false;
    
    /**
     * @return the ref
     */
    public String getRef() {
        return ref;
    }
    /**
     * @param ref the ref to set
     */
    public void setRef(String ref) {
        this.ref = ref;
    }
    /**
     * @return the simpleType
     */
    public JsonSchemaType getSimpleType() {
        return simpleType;
    }
    /**
     * @param simpleType the simpleType to set
     */
    public void setSimpleType(JsonSchemaType simpleType) {
        this.simpleType = simpleType;
    }
    
    public boolean isReference() {
	return this.ref != null;
    }
    
    public boolean isSimpleType() {
	return this.simpleType != null;
    }
    /**
     * @return the format
     */
    public String getFormat() {
        return format;
    }
    /**
     * @param format the format to set
     */
    public void setFormat(String format) {
        this.format = format;
    }
    
    public boolean hasFormat() {
	return StringUtils.isNotBlank(this.format);
    }
    /**
     * @return the isGeometry
     */
    public boolean isGeometry() {
        return isGeometry;
    }
    /**
     * @param isGeometry the isGeometry to set
     */
    public void setGeometry(boolean isGeometry) {
        this.isGeometry = isGeometry;
    }

}
