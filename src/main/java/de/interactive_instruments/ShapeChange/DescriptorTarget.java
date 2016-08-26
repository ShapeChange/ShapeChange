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
 * (c) 2002-2016 interactive instruments GmbH, Bonn, Germany
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

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class DescriptorTarget {

	public enum Format {
		STRING, IRI, LANG_STRING
	}

	public enum NoValueBehavior {
		INGORE, POPULATE_ONCE
	}

	public enum MultiValueBehavior {
		CONNECT_IN_SINGLE_TARGET, SPLIT_TO_MULTIPLE_TARGETS
	}
	
	public enum AppliesTo {
		ONTOLOGY, CLASS, CONCEPT_SCHEME, PROPERTY, ALL
	}

	private AppliesTo appliesTo;
	private String target;
	private String template;
	private Format format;
	private NoValueBehavior noValueBehavior;
	private String noValueText;
	private MultiValueBehavior multiValueBehavior;
	private String multiValueConnectorToken;

	/**
	 * @param appliesTo
	 * @param target
	 * @param template
	 * @param format
	 * @param noValueBehavior
	 * @param noValueText
	 * @param multiValueBehavior
	 * @param multiValueConnectorToken
	 */
	public DescriptorTarget(AppliesTo appliesTo, String target, String template,
			Format format, NoValueBehavior noValueBehavior, String noValueText,
			MultiValueBehavior multiValueBehavior,
			String multiValueConnectorToken) {
		super();
		this.appliesTo = appliesTo;
		this.target = target;
		this.template = template;
		this.format = format;
		this.noValueBehavior = noValueBehavior;
		this.noValueText = noValueText;
		this.multiValueBehavior = multiValueBehavior;
		this.multiValueConnectorToken = multiValueConnectorToken;
	}

	/**
	 * @return describes to which model elements the descriptor applies
	 */
	public AppliesTo getAppliesTo() {
		return appliesTo;
	}

	/**
	 * @return IRI of an RDF property that will be added with the resource
	 *         representing the model element as subject. Note: the value is
	 *         given as a (trimmed) QName-like string, with the namespace prefix
	 *         matching the namespace abbreviation of a namespace declared in
	 *         the configuration. The value is determined by the template
	 *         attribute, with the value format being defined by the format
	 *         attribute.
	 */
	public String getTarget() {
		return target;
	}

	/**
	 * @return Text template where an occurrence of the field
	 *         "[[descriptor-ID]]" is replaced with the value(s) of that
	 *         descriptor. The IDs of supported descriptors are listed in the
	 *         table above. An occurrence of the field "[[TV:name]]" is replaced
	 *         with the value(s) of the tag with the given name.
	 */
	public String getTemplate() {
		return template;
	}

	/**
	 * @return Defines the format of the property value:
	 *         <ul>
	 *         <li>langString: language tagged string; the configuration
	 *         parameter "language" (in the ontology target configuration)
	 *         provides the value of the language tag</li>
	 *         <li>string: string without language tag</li>
	 *         <li>IRI: the value is the IRI of a resource</li>
	 *         </ul>
	 * 
	 */
	public Format getFormat() {
		return format;
	}

	/**
	 * @return Determines the behavior in case that no value is available for
	 *         any of the fields contained in the template:
	 *         <ul>
	 *         <li>ignore: No target property is created.</li>
	 *         <li>populateOnce: A single target property is created, with the
	 *         noValueText being used for all fields.</li>
	 *         </ul>
	 * 
	 */
	public NoValueBehavior getNoValueBehavior() {
		return noValueBehavior;
	}

	/**
	 * @return If a descriptor used in a template has no value, then this
	 *         information item provides the text to use instead (e.g. "N/A" or
	 *         "FIXME").
	 */
	public String getNoValueText() {
		return noValueText;
	}

	/**
	 * @return Specifies the behavior how descriptors with multiple values shall
	 *         be encoded:
	 *         <ul>
	 *         <li>connectInSingleTarget: Multiple values of a descriptor
	 *         contained in the template are combined in a single target
	 *         property value, using the multiValueConnectorToken to combine
	 *         them.</li>
	 *         <li>splitToMultipleTargets: Multiple values for one or more
	 *         descriptors result in multiple target properties, one for each
	 *         value-combination of multi-valued descriptors (resulting in a
	 *         permutation of the values of each descriptor contained in the
	 *         template).</li>
	 *         </ul>
	 */
	public MultiValueBehavior getMultiValueBehavior() {
		return multiValueBehavior;
	}

	/**
	 * @return If a descriptor used in a template has multiple values, and the
	 *         multiValueBehavior of the descriptor target is set to
	 *         connectInSingleTarget, then the values are concatenated to a
	 *         single string value using this token as connector between two
	 *         values.
	 */
	public String getMultiValueConnectorToken() {
		return multiValueConnectorToken;
	}

}
