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

	multiTest("src/test/resources/json/valueTypeOptions/test_json_schema_valueTypeOptions.xml",
		new String[] { "json", "xml" }, "testResults/json/valueTypeOptions/json_schemas",
		"src/test/resources/json/valueTypeOptions/reference/json_schemas");
    }

    @Test
    public void testJson_documentation() {

	multiTest("src/test/resources/json/documentation/test_json_schema_documentation.xml",
		new String[] { "json", "xml" }, "testResults/json/documentation/json_schemas",
		"src/test/resources/json/documentation/reference/json_schemas");
    }

    @Test
    public void testJson_memberRestrictions() {

	multiTest("src/test/resources/json/memberRestrictions/test_json_schema_memberRestrictions.xml",
		new String[] { "json", "xml" }, "testResults/json/memberRestrictions/json_schemas",
		"src/test/resources/json/memberRestrictions/reference/json_schemas");
    }

    @Test
    public void testJson_collections() {

	multiTest("src/test/resources/json/collections/test_json_schema_collections.xml",
		new String[] { "json", "xml" }, "testResults/json/collections/json_schemas",
		"src/test/resources/json/collections/reference/json_schemas");
    }

    @Test
    public void testJson_mixinsAndEntityTypeMember() {

	multiTest("src/test/resources/json/mixinsAndEntityTypeMember/test_json_schema_mixinsAndEntityTypeMember.xml",
		new String[] { "json", "xml" }, "testResults/json/mixinsAndEntityTypeMember/json_schemas",
		"src/test/resources/json/mixinsAndEntityTypeMember/reference/json_schemas");
    }

    @Test
    public void testJson_measure() {

	multiTest("src/test/resources/json/measure/test_json_schema_measure.xml", new String[] { "json", "xml" },
		"testResults/json/measure/json_schemas", "src/test/resources/json/measure/reference/json_schemas");
    }

    @Test
    public void testJson_byReferenceFormat() {

	multiTest("src/test/resources/json/byReferenceFormat/test_json_schema_byReferenceFormat.xml",
		new String[] { "json", "xml" }, "testResults/json/byReferenceFormat/json_schemas",
		"src/test/resources/json/byReferenceFormat/reference/json_schemas");
    }
    
    @Test
    public void testJson_inlineOrByReferenceTag() {

	multiTest("src/test/resources/json/inlineOrByReferenceTag/test_json_schema_inlineOrByReferenceTag.xml",
		new String[] { "json", "xml" }, "testResults/json/inlineOrByReferenceTag/json_schemas",
		"src/test/resources/json/inlineOrByReferenceTag/reference/json_schemas");
    }
    
    @Test
    public void testJson_specialProperties() {

	multiTest("src/test/resources/json/specialProperties/test_json_schema_specialProperties.xml",
		new String[] { "json", "xml" }, "testResults/json/specialProperties/json_schemas",
		"src/test/resources/json/specialProperties/reference/json_schemas");
    }
    
    @Test
    public void testJson_featureRefs() {

	multiTest("src/test/resources/json/featureRefs/test_json_schema_featureRefs.xml",
		new String[] { "json" }, "testResults/json/featureRefs/json_schemas",
		"src/test/resources/json/featureRefs/reference/json_schemas");
    }
    
    @Test
    public void testJson_jsonFgGeometry() {

	multiTest("src/test/resources/json/jsonFgGeometry/test_json_schema_jsonFgGeometry.xml",
		new String[] { "json" }, "testResults/json/jsonFgGeometry/json_schemas",
		"src/test/resources/json/jsonFgGeometry/reference/json_schemas");
    }
}
