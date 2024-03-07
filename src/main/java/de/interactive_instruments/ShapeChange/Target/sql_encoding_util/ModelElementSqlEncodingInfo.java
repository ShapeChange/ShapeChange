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
 * (c) 2002-2023 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target.sql_encoding_util;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public abstract class ModelElementSqlEncodingInfo {

    protected String schemaName;
    protected String originalSchemaName;
    /**
     * @return the schemaName
     */
    public String getSchemaName() {
        return schemaName;
    }
    /**
     * @param schemaName the schemaName to set
     */
    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }
    /**
     * @return the originalSchemaName
     */
    public String getOriginalSchemaName() {
        return originalSchemaName;
    }
    /**
     * @param originalSchemaName the originalSchemaName to set
     */
    public void setOriginalSchemaName(String originalSchemaName) {
        this.originalSchemaName = originalSchemaName;
    }
    
    public boolean hasOriginalSchemaName() {
	return StringUtils.isNotBlank(this.originalSchemaName);
    }
    
    @Override
    public int hashCode() {
	return Objects.hash(originalSchemaName, schemaName);
    }
    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	ModelElementSqlEncodingInfo other = (ModelElementSqlEncodingInfo) obj;
	return Objects.equals(originalSchemaName, other.originalSchemaName)
		&& Objects.equals(schemaName, other.schemaName);
    }
}
