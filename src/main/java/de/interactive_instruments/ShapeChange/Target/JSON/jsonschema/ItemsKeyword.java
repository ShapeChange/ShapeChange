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
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange.Target.JSON.jsonschema;

import java.util.ArrayList;

import de.interactive_instruments.ShapeChange.Target.JSON.json.JsonArray;
import de.interactive_instruments.ShapeChange.Target.JSON.json.JsonValue;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class ItemsKeyword extends ArrayList<JsonSchema> implements JsonSchemaKeyword {

    /**
     * 
     */
    private static final long serialVersionUID = -7984111878760683780L;

    public ItemsKeyword() {
	super();
    }

    public ItemsKeyword(JsonSchema value) {
	super();
	this.add(value);
    }

    @Override
    public String name() {
	return "items";
    }

    @Override
    public JsonValue toJson(JsonSerializationContext context) {

	if (this.size() == 1) {
	    return this.get(0).toJson(context);
	} else {

	    JsonArray arr = new JsonArray();

	    for (JsonSchema schema : this) {
		arr.add(schema.toJson(context));
	    }

	    return arr;
	}
    }

}
