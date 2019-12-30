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
 * (c) 2002-2018 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target.Ontology;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;

import de.interactive_instruments.ShapeChange.Util.XMLUtil;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot
 *         de)
 *
 */
public abstract class RdfGeneralProperty {

	private String namespaceAbbreviation;
	private String name;

	/**
	 * The namespace is computed by ShapeChange
	 */
	private String namespace = null;
	private String domain;
	private boolean domainByUnionOfSubPropertyDomains;
	private String range;
	private SortedSet<String> equivalentProperty;
	private SortedSet<String> disjointProperty;
	private SortedSet<String> subPropertyOf;
	private SortedMap<String, List<RdfPropertyValue>> additionalPropertiesByQName = new TreeMap<>();

	public RdfGeneralProperty(Element gpE) {

		this.namespaceAbbreviation = XMLUtil.getTextContentOfFirstElement(gpE,
				"namespaceAbbreviation");

		this.name = XMLUtil.getTextContentOfFirstElement(gpE, "name");

		this.domain = XMLUtil.getTextContentOfFirstElement(gpE, "domain");

		this.domainByUnionOfSubPropertyDomains = XMLUtil
				.getTextContentOfFirstElement(gpE,
						"domainByUnionOfSubPropertyDomains") != null ? true
								: false;

		this.range = XMLUtil.getTextContentOfFirstElement(gpE, "range");

		this.equivalentProperty = new TreeSet<>(XMLUtil
				.getTextContentOfChildElements(gpE, "equivalentProperty"));

		this.disjointProperty = new TreeSet<>(
				XMLUtil.getTextContentOfChildElements(gpE, "disjointProperty"));

		this.subPropertyOf = new TreeSet<>(
				XMLUtil.getTextContentOfChildElements(gpE, "subPropertyOf"));

		List<Element> additionalProps = XMLUtil.getChildElements(gpE,
				"additionalProperty");

		for (Element ap : additionalProps) {

			String propertyQName = XMLUtil.getTextContentOfFirstElement(ap,
					"property");

			List<RdfPropertyValue> values = new ArrayList<>();

			List<Element> valueEs = XMLUtil.getChildElements(ap, "value");

			for (Element valueE : valueEs) {
				String text = StringUtils.stripToEmpty(valueE.getTextContent());
				String lang = StringUtils
						.stripToNull(valueE.getAttribute("lang"));
				boolean isIRI = XMLUtil
						.parseBoolean(valueE.getAttribute("isIRI"));
				RdfPropertyValue rpv = new RdfPropertyValue(text, lang, isIRI);
				values.add(rpv);
			}

			this.additionalPropertiesByQName.put(propertyQName, values);
		}

	}

	/**
	 * @return the namespaceAbbreviation; can be <code>null</code>
	 */
	public String getNamespaceAbbreviation() {
		return namespaceAbbreviation;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the domain; can be <code>null</code>
	 */
	public String getDomain() {
		return domain;
	}

	/**
	 * @return the domainByUnionOfSubPropertyDomains be null
	 */
	public boolean isDomainByUnionOfSubPropertyDomains() {
		return domainByUnionOfSubPropertyDomains;
	}

	/**
	 * @return the range; can be <code>null</code>
	 */
	public String getRange() {
		return range;
	}

	/**
	 * @return the set of equivalent properties; can be empty but not
	 *         <code>null</code>
	 */
	public SortedSet<String> getEquivalentProperty() {
		return equivalentProperty;
	}

	/**
	 * @return the set of disjoint properties; can be empty but not
	 *         <code>null</code>
	 */
	public SortedSet<String> getDisjointProperty() {
		return disjointProperty;
	}

	/**
	 * @return the set of properties of which this property is a subPropertyOf;
	 *         can be empty but not <code>null</code>
	 */
	public SortedSet<String> getSubPropertyOf() {
		return subPropertyOf;
	}

	/**
	 * @return the additional properties defined for this property (key is the
	 *         QName); can be empty but not <code>null</code>
	 */
	public SortedMap<String, List<RdfPropertyValue>> getAdditionalProperties() {
		return additionalPropertiesByQName;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getNamespace() {
		return this.namespace;
	}

	/**
	 * @return concatenation of namespace and name
	 */
	public String getURI() {
		return this.namespace + this.name;
	}

	public boolean hasRange() {
		return this.range != null;
	}

	public boolean hasDomain() {
		return this.domain != null;
	}
}
