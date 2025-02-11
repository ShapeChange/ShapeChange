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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.shapechange.core.target.ldproxy2;

import java.util.Optional;

import de.ii.xtraplatform.features.domain.SchemaBase.Type;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class LdpSourcePathInfo {

    protected Optional<String> idSourcePath;
    protected Optional<String> valueSourcePath;
    protected Optional<Type> idValueType;
    protected String refType;
    protected String refUriTemplate;
    protected boolean targetsSingleValue;

    public LdpSourcePathInfo(Optional<String> idSourcePath, Optional<String> valueSourcePath,
	    Optional<Type> idValueType, String refType, String refUriTemplate, boolean targetsSingleValue) {
	this.idSourcePath = idSourcePath;
	this.valueSourcePath = valueSourcePath;
	this.idValueType = idValueType;
	this.refType = refType;
	this.refUriTemplate = refUriTemplate;
	this.targetsSingleValue = targetsSingleValue;
    }

    /**
     * @return the idValueType
     */
    public Optional<Type> getIdValueType() {
	return idValueType;
    }

    /**
     * @return the refType
     */
    public String getRefType() {
	return refType;
    }

    /**
     * @return the refUriTemplate
     */
    public String getRefUriTemplate() {
	return refUriTemplate;
    }

    /**
     * @return <code>true</code>, if the property for which this object holds source
     *         path information has at most a single value; else <code>false</code>
     */
    public boolean isTargetsSingleValue() {
	return targetsSingleValue;
    }

    /**
     * @return the idSourcePath; can be empty for cases of properties with simple or
     *         geometry type
     */
    public Optional<String> getIdSourcePath() {
	return idSourcePath;
    }

    /**
     * @return the valueSourcePath; can be empty for cases of links/refs to external
     *         resources
     */
    public Optional<String> getValueSourcePath() {
	return valueSourcePath;
    }

}
