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
public class RdfPropertyMapEntry {

	private String property;
	private String schema;
	private String target;
	private String range;
	private String rule;

	/**
	 * @param property
	 * @param schema
	 * @param target
	 * @param range
	 * @param rule
	 */
	public RdfPropertyMapEntry(String property, String schema, String target,
			String range, String rule) {
		super();
		this.property = property;
		this.schema = schema;
		this.target = target;
		this.range = range;
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
	 *         multiple schemas are being processed. Can be <code>null</code>.
	 */
	public String getSchema() {
		return schema;
	}

	public boolean hasSchema() {
		return schema != null;
	}

	/**
	 * @return IRI of the RDF/OWL property to which the UML property shall be
	 *         mapped (e.g. "ex1:propZ"). Note: the value is given as a
	 *         (trimmed) QName-like string, with the namespace prefix matching
	 *         the namespace abbreviation of a namespace declared in the
	 *         configuration.
	 */
	public String getTarget() {
		return target;
	}

	/**
	 * @return Range to use in class expressions involving the target (RDF/OWL)
	 *         property. Note: the value is expected to be given as a (trimmed)
	 *         QName-like string, with the namespace prefix matching the
	 *         namespace abbreviation of a namespace declared in the
	 *         configuration. Can be <code>null</code>.
	 */
	public String getRange() {
		return range;
	}

	public boolean hasRange() {
		return range != null;
	}

	/**
	 * @return The encoding rule to which this mapping applies. May be “*” to
	 *         indicate that the mapping applies to all encoding rules.
	 */
	public String getRule() {
		return rule;
	}
}
