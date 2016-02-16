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
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 */
public class TargetXmlSchemaConfiguration extends TargetConfiguration {

	private List<XsdMapEntry> xsdMapEntries;
	private List<XmlNamespace> xmlNamespaces;

	/**
	 * Creates a TargetConfiguration.
	 * 
	 * @param className
	 *            The fully qualified name of the class implementing the target.
	 * @param processMode
	 *            The execution mode of the target.
	 * @param parameters
	 *            The target parameters. <code>null</code> if no parameters were
	 *            declared in the configuration.
	 * @param ruleSets
	 *            The encoding rule sets declared for the target.
	 *            <code>null</code> if no rule sets were declared in the
	 *            configuration.
	 * @param mapEntries
	 *            Will be ignored, so can be <code>null</code>.
	 * @param xsdMapEntries
	 *            The xsd map entries for the target
	 * @param xmlNamespaces
	 *            Xml namespaces defined for this target
	 * @param inputIds
	 *            Set of identifiers referencing either the input model or a
	 *            transformer.
	 * @param advancedProcessConfigurations
	 *            the 'advancedProcessConfigurations' element from the
	 *            configuration of the process; <code>null</code> if it is not
	 *            set there
	 */
	public TargetXmlSchemaConfiguration(String className,
			ProcessMode processMode, Map<String, String> parameters,
			Map<String, ProcessRuleSet> ruleSets,
			List<ProcessMapEntry> mapEntries, List<XsdMapEntry> xsdMapEntries,
			List<XmlNamespace> xmlNamespaces, Set<String> inputIds,
			Element advancedProcessConfigurations) {
		super(className, processMode, parameters, ruleSets, null, inputIds,
				null, advancedProcessConfigurations);
		this.xsdMapEntries = xsdMapEntries;
		this.xmlNamespaces = xmlNamespaces;
	}

	/**
	 * @return The xsd map entries for the target. <code>null</code> if no map
	 *         entries were declared in the configuration.
	 */
	public List<XsdMapEntry> getXsdMapEntries() {
		return xsdMapEntries;
	}

	public List<XmlNamespace> getXmlNamespaces() {
		return xmlNamespaces;
	}

	public String toString() {

		StringBuffer sb = new StringBuffer();

		sb.append("TargetXmlSchemaConfiguration:\r\n");
		sb.append("--- with target configuration:\r\n");
		sb.append(super.toString());
		sb.append("\txml namespaces: " + "\r\n");

		if (this.xmlNamespaces == null || xmlNamespaces.isEmpty()) {
			sb.append("none\r\n");
		} else {
			for (XmlNamespace xmlns : xmlNamespaces) {
				sb.append("\t\txml namespace (" + xmlns.getNsabr() + "|"
						+ xmlns.getNs() + "|" + xmlns.getLocation() + "|");

				if (xmlns.getPackageName() != null) {
					sb.append(xmlns.getPackageName());
				} else {
					sb.append("no package name");
				}
				sb.append(")\r\n");

			}
		}

		sb.append("\txsd map entries: ");
		if (this.xsdMapEntries == null || xsdMapEntries.isEmpty()) {
			sb.append("none\r\n");
		} else {
			sb.append("\r\n");
			for (XsdMapEntry mapEntry : xsdMapEntries) {

				sb.append("\t\txsd map entry: (");
				sb.append(mapEntry.getType() + "|");
				if (mapEntry.getEncodingRules() == null
						|| mapEntry.getEncodingRules().isEmpty()) {
					sb.append("<no encoding rules>|");
				} else {
					for (String encRule : mapEntry.getEncodingRules()) {
						sb.append(encRule + " ");
					}
					sb.deleteCharAt(sb.length() - 1);
					sb.append("|");
				}

				if (mapEntry.getXmlType() == null) {
					sb.append("<no type>|");
				} else {
					sb.append(mapEntry.getXmlType() + "|");
				}
				if (mapEntry.getXmlTypeContent() == null) {
					sb.append("<no type content>|");
				} else {
					sb.append(mapEntry.getXmlTypeContent() + "|");
				}
				if (mapEntry.getXmlTypeType() == null) {
					sb.append("<no type type>|");
				} else {
					sb.append(mapEntry.getXmlTypeType() + "|");
				}
				if (mapEntry.getXmlTypeNilReason() == null) {
					sb.append("<no nil reason>|");
				} else {
					sb.append(mapEntry.getXmlTypeNilReason() + "|");
				}
				if (mapEntry.getXmlElement() == null) {
					sb.append("<no element>|");
				} else {
					sb.append(mapEntry.getXmlElement() + "|");
				}
				if (mapEntry.getXmlPropertyType() == null) {
					sb.append("<no property type>|");
				} else {
					sb.append(mapEntry.getXmlPropertyType() + "|");
				}
				if (mapEntry.getXmlAttribute() == null) {
					sb.append("<no attribute>|");
				} else {
					sb.append(mapEntry.getXmlAttribute() + "|");
				}
				if (mapEntry.getXmlAttributeGroup() == null) {
					sb.append("<no attribute group>|");
				} else {
					sb.append(mapEntry.getXmlAttributeGroup() + "|");
				}
				if (mapEntry.getNsabr() == null) {
					sb.append("<no nsabr>");
				} else {
					sb.append(mapEntry.getNsabr() + "");
				}
				sb.append(")\r\n");
			}
		}

		return sb.toString();
	}

}
