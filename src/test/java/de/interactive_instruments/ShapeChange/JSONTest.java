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
 * (c) 2002-2017 interactive instruments GmbH, Bonn, Germany
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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("SCXML")
public class JSONTest extends BasicTestSCXML {

    @Test
    public void testJson_basic() {

	multiTest("src/test/resources/json/basic/test_json_schema_basic.xml", new String[] { "json", "xml" },
		"testResults/json/basic/json_schemas", "src/test/resources/json/basic/reference/json_schemas");
    }
    
    @Test
    public void testJson_valueTypeOptions() {

	multiTest("src/test/resources/json/valueTypeOptions/test_json_schema_valueTypeOptions.xml", new String[] { "json", "xml" },
		"testResults/json/valueTypeOptions/json_schemas", "src/test/resources/json/valueTypeOptions/reference/json_schemas");
    }
    
    @Test
    public void testJson_documentation() {

	multiTest("src/test/resources/json/documentation/test_json_schema_documentation.xml", new String[] { "json" },
		"testResults/json/documentation/json_schemas", "src/test/resources/json/documentation/reference/json_schemas");
    }
}
