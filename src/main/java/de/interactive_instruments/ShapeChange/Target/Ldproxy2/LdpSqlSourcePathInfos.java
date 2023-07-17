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
package de.interactive_instruments.ShapeChange.Target.Ldproxy2;

import java.util.ArrayList;
import java.util.List;

import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class LdpSqlSourcePathInfos {

    public class SourcePathInfo {
	String sourcePath;
	Type valueType;
	String refType;
	String refUriTemplate;
	String targetTable;
	boolean targetsSingleValue;
	
	
    }
    
    PropertyInfo pi;
    PropertyEncodingContext context;
    
    private List<SourcePathInfo> spis = new ArrayList<>();

    public LdpSqlSourcePathInfos() {
    }

    public boolean isEmpty() {
	return spis.isEmpty();
    }

    public List<SourcePathInfo> getSourcePathInfos() {
	return this.spis;
    }

    public boolean isSingleSourcePath() {
	return this.spis.size() == 1;
    }

    public boolean isMultipleSourcePaths() {
	return this.spis.size() > 1;
    }

    public SourcePathInfo addSourcePathInfo(String sourcePath, Type valueType, String refType, String refUriTemplate, String targetTable, boolean targetsSingleValue) {
	SourcePathInfo spi = new SourcePathInfo();
	spi.sourcePath = sourcePath;
	spi.valueType = valueType;
	spi.refType = refType;
	spi.refUriTemplate = refUriTemplate;
	spi.targetTable = targetTable;
	spi.targetsSingleValue = targetsSingleValue;
	this.spis.add(spi);
	return spi;
    }
    
    public boolean concatRequired() {
	return isMultipleSourcePaths() && pi.cardinality().maxOccurs > 1;
    }

    public boolean coalesceRequired() {
	return isMultipleSourcePaths() && pi.cardinality().maxOccurs == 1;
    }
}
