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
package de.interactive_instruments.shapechange.core.target.json.jsonschema;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import de.interactive_instruments.shapechange.core.target.json.json.JsonArray;
import de.interactive_instruments.shapechange.core.target.json.json.JsonValue;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class EnumKeyword extends ArrayList<JsonValue> implements JsonSchemaKeyword {

	/**
	  
	 */
	@Serial
	private static final long serialVersionUID = 2950497991632003427L;

    public EnumKeyword() {
	super();
    }

    public EnumKeyword(List<JsonValue> values) {
	super();
	this.addAll(values);
    }

    @Override
    public String name() {
	return "enum";
    }

    @Override
    public JsonValue toJson(JsonSerializationContext context) {

	/*
	 * The value of this keyword MUST be an array. This array SHOULD have at least
	 * one element. Elements in the array SHOULD be unique.
	 */

	JsonArray arr = new JsonArray();

	arr.addAll(this);

	return arr;
    }

}
