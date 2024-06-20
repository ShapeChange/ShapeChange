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
 * (c) 2002-2020 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target.JSON.jsonschema;

import java.util.Optional;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public enum JsonSchemaVersion {

    DRAFT_2020_12("2020-12", "https://json-schema.org/draft/2020-12/schema"),
    DRAFT_2019_09("2019-09", "https://json-schema.org/draft/2019-09/schema"),
    DRAFT_07("draft-07", "http://json-schema.org/draft-07/schema#"),
    OPENAPI_30("OpenApi30",null);

    private String name;
    private String schemaUri;

    JsonSchemaVersion(String name, String schemaUri) {
	this.name = name;
	this.schemaUri = schemaUri;
    }

    public String getName() {
	return this.name;
    }

    public Optional<String> getSchemaUri() {
	return Optional.ofNullable(this.schemaUri);
    }

    /**
     * @param name The name of the JSON Schema version enum to retrieve
     * @return the enum whose name is equal to, ignoring case, the given name; can
     *         be empty (if the name does not match one of the defined enum names)
     *         but not <code>null</code>
     */
    public static Optional<JsonSchemaVersion> fromString(String name) {

	for (JsonSchemaVersion v : values()) {
	    if (v.getName().equalsIgnoreCase(name)) {
		return Optional.of(v);
	    }
	}
	return Optional.empty();
    }
}
