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

import java.util.Set;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class PropertyConversionParameter {

	private String property;
	private String schema;
	private boolean global;
	private Set<String> subPropertyOf;
	private String target;
	private String targetSchema;
	private String rule;

	/**
	 * @param property
	 * @param schema
	 * @param global
	 * @param subPropertyOf
	 * @param target
	 * @param targetSchema
	 * @param rule
	 */
	public PropertyConversionParameter(String property, String schema,
			boolean global, Set<String> subPropertyOf, String target,
			String targetSchema, String rule) {
		super();
		this.property = property;
		this.schema = schema;
		this.global = global;
		this.subPropertyOf = subPropertyOf;
		this.target = target;
		this.targetSchema = targetSchema;
		this.rule = rule;
	}

	/**
	 * @return (Trimmed) Name of a UML property, optionally scoped to a class
	 *         from the application schema (example: FeatureX::propertyY).
	 */
	public String getProperty() {
		return property;
	}

	/**
	 * @return The (trimmed) name of the application schema package to which the
	 *         UML property belongs. Used to avoid ambiguity in case that
	 *         multiple schemas are being processed.
	 */
	public String getSchema() {
		return schema;
	}

	public boolean hasSchema() {
		return schema != null;
	}

	/**
	 * @return Specifies if the UML property shall be encoded as a global
	 *         property
	 */
	public boolean isGlobal() {
		return global;
	}

	/**
	 * @return IRIs of RDF/OWL properties of which the RDF/OWL implementation of
	 *         the UML property shall be a subPropertyOf. Note: the values are
	 *         given as (trimmed) QName-like strings, with the namespace
	 *         prefixes matching the namespace abbreviations of the namespaces
	 *         declared in the configuration. Can be <code>null</code> - check
	 *         with hasSubPropertyOf()
	 */
	public Set<String> getSubPropertyOf() {
		return subPropertyOf;
	}

	public boolean hasSubPropertyOf() {
		return subPropertyOf != null && !subPropertyOf.isEmpty();
	}

	/**
	 * @return (Trimmed) Name of the target UML property, whose RDF/OWL
	 *         implementation will be used to implement this property.
	 */
	public String getTarget() {
		return target;
	}

	public boolean hasTarget() {
		return target != null;
	}

	/**
	 * @return The (trimmed) name of the application schema package to which the
	 *         target property belongs. Used to avoid ambiguity in case that
	 *         multiple schemas are being processed.
	 */
	public String getTargetSchema() {
		return targetSchema;
	}

	public boolean hasTargetSchema() {
		return targetSchema != null;
	}

	/**
	 * @return The encoding rule to which this parameter applies. May be “*” to
	 *         indicate that the parameter applies to all encoding rules.
	 */
	public String getRule() {
		return rule;
	}
}
