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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange.Target.xml_encoding_util;

import java.util.Comparator;
import java.util.Objects;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class ModelElementXmlEncoding implements Comparable<ModelElementXmlEncoding> {

    public static final Comparator<ModelElementXmlEncoding> comparator = Comparator
	    .comparing(ModelElementXmlEncoding::getApplicationSchemaName,
		    Comparator.nullsFirst(Comparator.naturalOrder()))
	    .thenComparing(ModelElementXmlEncoding::getModelElementName,
		    Comparator.nullsFirst(Comparator.naturalOrder()))
	    .thenComparing(ModelElementXmlEncoding::getXmlName, Comparator.nullsFirst(Comparator.naturalOrder()))
	    .thenComparing(ModelElementXmlEncoding::getXmlNamespace, Comparator.nullsFirst(Comparator.naturalOrder()))
	    .thenComparing(ModelElementXmlEncoding::isXmlAttribute, Comparator.nullsFirst(Comparator.naturalOrder()));

    protected String modelElementName;
    protected String applicationSchemaName;
    protected String xmlName;
    protected String xmlNamespace;
    protected Boolean isXmlAttribute;

    /**
     * @param modelElementName      - tbd
     * @param applicationSchemaName - tbd
     * @param xmlName              - tbd
     * @param xmlNamespace              - tbd
     * @param isXmlAttribute        - tbd
     */

    public ModelElementXmlEncoding(String modelElementName, String applicationSchemaName, String xmlName, String xmlNamespace,
	    Boolean isXmlAttribute) {
	super();
	this.modelElementName = modelElementName;
	this.applicationSchemaName = applicationSchemaName;
	this.xmlName = xmlName;
	this.xmlNamespace = xmlNamespace;
	this.isXmlAttribute = isXmlAttribute;
    }

    /**
     * @return the modelElementName
     */
    public String getModelElementName() {
	return modelElementName;
    }

    /**
     * @return the applicationSchemaName
     */
    public String getApplicationSchemaName() {
	return applicationSchemaName;
    }

    /**
     * @return the xmlName
     */
    public String getXmlName() {
	return xmlName;
    }
    
    /**
     * @return the xmlNamespace
     */
    public String getXmlNamespace() {
	return xmlNamespace;
    }

    /**
     * @return the xmlAttribute
     */
    public Boolean isXmlAttribute() {
	return isXmlAttribute;
    }

    @Override
    public int compareTo(ModelElementXmlEncoding o) {
	if(this == o) {
	    return 0;
	} else {
	    return comparator.compare(this, o);
	}
    }

    @Override
    public int hashCode() {
	return Objects.hash(applicationSchemaName, isXmlAttribute, modelElementName, xmlName, xmlNamespace);
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	ModelElementXmlEncoding other = (ModelElementXmlEncoding) obj;
	return Objects.equals(applicationSchemaName, other.applicationSchemaName)
		&& Objects.equals(isXmlAttribute, other.isXmlAttribute)
		&& Objects.equals(modelElementName, other.modelElementName) && Objects.equals(xmlName, other.xmlName)
		&& Objects.equals(xmlNamespace, other.xmlNamespace);
    }
    
}
