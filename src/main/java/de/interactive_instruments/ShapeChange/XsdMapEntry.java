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
 * (c) 2002-2013 interactive instruments GmbH, Bonn, Germany
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

import java.util.List;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments <dot>
 *         de)
 */
public class XsdMapEntry {

	private String type;
	private List<String> encodingRules;
	private String xmlType;
	private String xmlTypeContent;
	private String xmlTypeType;
	private String xmlTypeNilReason;
	private String xmlElement;
	private String xmlPropertyType;
	private String xmlAttribute;
	private String xmlAttributeGroup;
	private String nsabr;

	public XsdMapEntry(String type, List<String> encodingRules, String xmlType,
			String xmlTypeContent, String xmlTypeType, String xmlTypeNilReason,
			String xmlElement, String xmlPropertyType, String xmlAttribute,
			String xmlAttributeGroup, String nsabr) {
		super();
		this.type = type;
		this.encodingRules = encodingRules;
		this.xmlType = xmlType;
		this.xmlTypeContent = xmlTypeContent;
		this.xmlTypeType = xmlTypeType;
		this.xmlTypeNilReason = xmlTypeNilReason;
		this.xmlElement = xmlElement;
		this.xmlPropertyType = xmlPropertyType;
		this.xmlAttribute = xmlAttribute;
		this.xmlAttributeGroup = xmlAttributeGroup;
		this.nsabr = nsabr;
	}

	public String getType() {
		return type;
	}

	public List<String> getEncodingRules() {
		return encodingRules;
	}

	public String getXmlType() {
		return xmlType;
	}

	public String getXmlTypeContent() {
		return xmlTypeContent;
	}

	public String getXmlTypeType() {
		return xmlTypeType;
	}

	public String getXmlTypeNilReason() {
		return xmlTypeNilReason;
	}

	public String getXmlElement() {
		return xmlElement;
	}

	public String getXmlPropertyType() {
		return xmlPropertyType;
	}

	public String getXmlAttribute() {
		return xmlAttribute;
	}

	public String getXmlAttributeGroup() {
		return xmlAttributeGroup;
	}

	public String getNsabr() {
		return nsabr;
	}

}
