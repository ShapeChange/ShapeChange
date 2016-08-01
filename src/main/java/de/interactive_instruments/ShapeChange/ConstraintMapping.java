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
public class ConstraintMapping {

	public enum ConstraintType {
		OCL, FOL, TEXT
	}

	private ConstraintType constraintType;
	private String target;
	private String template;
	private String noValue;
	private String multiValueConnectorToken;

	/**
	 * @param constraintType
	 * @param target
	 * @param template
	 * @param noValue
	 * @param multiValueConnectorToken
	 */
	public ConstraintMapping(ConstraintType constraintType, String target,
			String template, String noValue, String multiValueConnectorToken) {
		super();
		this.constraintType = constraintType;
		this.target = target;
		this.template = template;
		this.noValue = noValue;
		this.multiValueConnectorToken = multiValueConnectorToken;
	}

	/**
	 * @return Identifies the type of constraint for which the mapping is
	 *         defined.
	 */
	public ConstraintType getConstraintType() {
		return constraintType;
	}

	/**
	 * @return IRI of an RDF property or OWL annotation property that will be
	 *         used to represent the constraint. The subject is the OWL class or
	 *         property representing the UML class or property on which the
	 *         constraint is defined (i.e., its context model element), and the
	 *         object is a language tagged string. The string content is
	 *         determined by the template. The configuration parameter
	 *         "language" (in the ontology target configuration) provides the
	 *         value of the language tag. Note: the value is given as a
	 *         (trimmed) QName-like string, with the namespace prefix matching
	 *         the namespace abbreviation of a namespace declared in the
	 *         configuration.
	 */
	public String getTarget() {
		return target;
	}

	/**
	 * @return Text template where an occurrence of the field
	 *         "[[constraint property ID]]" is replaced with the value of that
	 *         property.
	 */
	public String getTemplate() {
		return template;
	}

	/**
	 * @return If a constraint property used in a template has no value, then
	 *         this information item provides the text to use instead (e.g.,
	 *         "N/A").
	 */
	public String getNoValue() {
		return noValue;
	}

	/**
	 * @return If a constraint property used in a template has multiple values,
	 *         they are concatenated to a single string value using this token
	 *         as connector between two values.
	 */
	public String getMultiValueConnectorToken() {
		return multiValueConnectorToken;
	}
}
