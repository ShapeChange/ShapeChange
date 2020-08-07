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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;

import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot
 *         de)
 */
public class TargetXmlSchemaConfiguration extends TargetConfiguration {

	private List<XsdMapEntry> xsdMapEntries = new ArrayList<XsdMapEntry>();

	/**
	 * map with key: propertyName#schemaName (the property name may be
	 * qualified, i.e. ClassA::propertyB; the schemaName can be empty if it is
	 * undefined); value: the XsdPropertyMapEntry
	 */
	private Map<String, XsdPropertyMapEntry> xsdPropertyMapEntries = new HashMap<>();

	private List<XmlNamespace> xmlNamespaces = new ArrayList<XmlNamespace>();

	/**
	 * Creates a TargetConfiguration.
	 * 
	 * @param className
	 *                                          The fully qualified name of the
	 *                                          class implementing the target.
	 * @param processMode
	 *                                          The execution mode of the
	 *                                          target.
	 * @param parameters
	 *                                          The target parameters.
	 *                                          <code>null</code> if no
	 *                                          parameters were declared in the
	 *                                          configuration.
	 * @param ruleSets
	 *                                          The encoding rule sets declared
	 *                                          for the target.
	 *                                          <code>null</code> if no rule
	 *                                          sets were declared in the
	 *                                          configuration.
	 * @param mapEntries
	 *                                          Will be ignored, so can be
	 *                                          <code>null</code>.
	 * @param xsdMapEntries
	 *                                          The xsd map entries for the
	 *                                          target, can be <code>null</code>
	 * @param xsdPropertyMapEntries tbd
	 * @param xmlNamespaces
	 *                                          Xml namespaces defined for this
	 *                                          target, can be <code>null</code>
	 * @param inputIds
	 *                                          Set of identifiers referencing
	 *                                          either the input model or a
	 *                                          transformer.
	 * @param advancedProcessConfigurations
	 *                                          the
	 *                                          'advancedProcessConfigurations'
	 *                                          element from the configuration
	 *                                          of the process;
	 *                                          <code>null</code> if it is not
	 *                                          set there
	 */
	public TargetXmlSchemaConfiguration(String className,
			ProcessMode processMode, Map<String, String> parameters,
			Map<String, ProcessRuleSet> ruleSets,
			List<ProcessMapEntry> mapEntries, List<XsdMapEntry> xsdMapEntries,
			Map<String, List<XsdPropertyMapEntry>> xsdPropertyMapEntries,
			List<XmlNamespace> xmlNamespaces, SortedSet<String> inputIds,
			Element advancedProcessConfigurations) {
		super(className, processMode, parameters, ruleSets, null, inputIds,
				null, advancedProcessConfigurations);

		if (CollectionUtils.isNotEmpty(xsdMapEntries)) {
			this.xsdMapEntries = xsdMapEntries;
		}

		for (String property : xsdPropertyMapEntries.keySet()) {
			for (XsdPropertyMapEntry xpme : xsdPropertyMapEntries
					.get(property)) {
				this.xsdPropertyMapEntries.put(
						property + "#"
								+ (xpme.hasSchema() ? xpme.getSchema() : ""),
						xpme);
			}
		}

		if (CollectionUtils.isNotEmpty(xmlNamespaces)) {
			this.xmlNamespaces = xmlNamespaces;
		}
	}

	/**
	 * @return The xsd map entries for the target. Can be empty but not
	 *         <code>null</code>.
	 */
	public List<XsdMapEntry> getXsdMapEntries() {
		return xsdMapEntries;
	}

	/**
	 * @return map with key: propertyName#schemaName (the property name may be
	 *         qualified, i.e. ClassA::propertyB; the schemaName can be empty if
	 *         it is undefined); value: the XsdPropertyMapEntry
	 */
	public Map<String, XsdPropertyMapEntry> getXsdPropertyMapEntries() {
		return this.xsdPropertyMapEntries;
	}

	/**
	 * @return The xsd namespaces for the target. Can be empty but not
	 *         <code>null</code>.
	 */
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

		sb.append("\txsd property map entries: ");
		if (this.xsdPropertyMapEntries == null
				|| xsdPropertyMapEntries.isEmpty()) {
			sb.append("none\r\n");
		} else {
			sb.append("\r\n");
			for (XsdPropertyMapEntry mapEntry : this.xsdPropertyMapEntries
					.values()) {

				sb.append("\t\txsd property map entry: ");
				sb.append(mapEntry.toString());
				sb.append("\r\n");
			}
		}

		return sb.toString();
	}

	/**
	 * Determine the qualified name of the given property, taking into account
	 * property map entries. If a property map entry exists that applies to the
	 * property and has a (non-empty) target element, then the value of that
	 * target element - which is a QName like string - is returned. Otherwise,
	 * the QName as defined by
	 * {@link de.interactive_instruments.ShapeChange.Model.PropertyInfo#qname()}
	 * is returned.
	 * 
	 * @param pi tbd
	 * @return the QName of the given property, cannot be <code>null</code>
	 *         target element (QName like string) of the property map entry that
	 *         applies to the given property, if one is defined in the
	 *         configuration and if it is not blank, else <code>null</code>
	 */
	public String determineQName(PropertyInfo pi) {

		XsdPropertyMapEntry xpme = getPropertyMapEntry(pi);
		if (xpme != null && xpme.hasTargetElement()) {
			return xpme.getTargetElement();
		} else {
			return pi.qname();
		}
	}

	public XsdPropertyMapEntry getPropertyMapEntry(PropertyInfo pi) {

		PackageInfo schemaOfInClass = pi.model().schemaPackage(pi.inClass());

		if (schemaOfInClass == null) {
			/*
			 * Can happen if the class is owned by an external package that is
			 * not a schema. Then pi could be a reverse property.
			 */
			return null;
		} else {
			return getPropertyMapEntry(pi.inClass().name() + "::" + pi.name(),
					schemaOfInClass.name());
		}
	}

	/**
	 * Look up the map entry for a property, given a combination of the name of
	 * the class it belongs to and its name (example: Feature1::att4), as well
	 * as the name of the schema the class belongs to.
	 * 
	 * The configuration may contain multiple XsdPropertyMapEntry elements for a
	 * specific property (P). These elements are identified by matching the
	 * given property name (scoped to a class) against the 'property' and
	 * 'schema' of that map entry. The look-up of the XsdPropertyMapEntry that
	 * applies to P is performed as follows:
	 * 
	 * <ul>
	 * <li>If a map entry has the same combination of class name, property name,
	 * and schema then that map entry is chosen (because it is most specific for
	 * P).</li>
	 * <li>Otherwise, if a map entry has the same property name and schema, but
	 * the property name is not scoped to a specific class (example: att4) then
	 * that map entry is chosen (because it provides a generic mapping for the
	 * property that is specific to its schema).</li>
	 * <li>Otherwise, if a map entry does not define any schema, but has the
	 * same combination of class name and property name, then it is chosen
	 * (because it is a slightly more specific mapping for P compared to the
	 * generic mapping).</li>
	 * <li>Otherwise, if a map entry does not define any schema, but has the
	 * same property name and is not scoped to a specific class, then it is
	 * chosen (because it is a generic mapping for P).</li>
	 * <li>Otherwise none of the map entries applies to P.</li>
	 * </ul>
	 * 
	 * @param propertyNameScopedToClass
	 *                                      name of the property to look up an
	 *                                      applicable map entry; the name has a
	 *                                      class name as prefix, separated by
	 *                                      "::" (example: Feature1::att4), may
	 *                                      NOT be <code>null</code> or empty
	 * @param schemaName
	 *                                      name of the schema to which the
	 *                                      property belongs, may NOT be
	 *                                      <code>null</code> or empty
	 * @return the map entry that applies to this property; <code>null</code> if
	 *         none is applicable
	 */
	public XsdPropertyMapEntry getPropertyMapEntry(
			String propertyNameScopedToClass, String schemaName) {

		if (StringUtils.isBlank(propertyNameScopedToClass)
				|| StringUtils.isBlank(schemaName)) {
			return null;
		}

		// do we have an ideal match?
		if (xsdPropertyMapEntries
				.containsKey(propertyNameScopedToClass + "#" + schemaName)) {
			return xsdPropertyMapEntries
					.get(propertyNameScopedToClass + "#" + schemaName);
		}

		String piName = null;

		String[] components = propertyNameScopedToClass.split("::");
		piName = components[1];

		// do we have a match upon property name and schema name?
		if (xsdPropertyMapEntries.containsKey(piName + "#" + schemaName)) {
			return xsdPropertyMapEntries.get(piName + "#" + schemaName);
		}

		// do we have a match upon property name scoped to class?
		if (xsdPropertyMapEntries
				.containsKey(propertyNameScopedToClass + "#")) {
			return xsdPropertyMapEntries.get(propertyNameScopedToClass + "#");
		}

		// finally, try looking up a map entry with the property name alone
		return xsdPropertyMapEntries.get(piName + "#");
	}

}
