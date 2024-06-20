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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot
 *         de)
 */
public class XsdPropertyMapEntry {

	private String property;
	private String schema;
	private String targetElement;

	public XsdPropertyMapEntry(String property, String schema,
			String targetElement) {
		super();
		this.property = property;
		this.schema = StringUtils.isBlank(schema) ? null : schema;
		this.targetElement = StringUtils.isBlank(targetElement) ? null
				: targetElement;
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

	public boolean hasTargetElement() {
		return targetElement != null;
	}

	/**
	 * @return XML Element to which the UML property shall be mapped (e.g.
	 *         “ex:elementX). Can be <code>null</code> if the property shall not
	 *         be encoded. Note: the value is given as a (trimmed) QName-like
	 *         string, with the namespace prefix matching the namespace
	 *         abbreviation of a namespace declared in the configuration.
	 */
	public String getTargetElement() {
		return targetElement;
	}

	@Override
	public String toString() {
		return "(@property: " + property + " | @schema: "
				+ (hasSchema() ? schema
						: "<none>") + " | @targetElement: "
								+ (hasTargetElement() ? targetElement
										: "<none>" + ")");
	}

}
