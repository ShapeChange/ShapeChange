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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.shapechange.app;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("SCXML")
public class TaggedValueTransformerTest extends BasicTestSCXML {

	@Test
	public void testTaggedValueInheritance() {
		/*
		 * 
		 */
		multiTest(
				"src/integrationtests/taggedValueTransformer/taggedValueInheritance/testEA_taggedValueInheritance.xml",
				new String[] { "xml" }, "testResults/taggedValueTransformer/taggedValueInheritance/export",
				"src/integrationtests/taggedValueTransformer/taggedValueInheritance/reference/export");
	}
	
	@Test
	public void testCreateOriginalSchemaInformationTags() {
		/*
		 * 
		 */
		multiTest(
				"src/integrationtests/taggedValueTransformer/createOriginalSchemaInfoTags/test_createOriginalSchemaInfoTags.xml",
				new String[] { "xsd" }, "testResults/taggedValueTransformer/createOriginalSchemaInfoTags/xsd",
				"src/integrationtests/taggedValueTransformer/createOriginalSchemaInfoTags/reference/xsd");
	}
	
	@Test
	public void testCreatePropertyValueTypeInfoTag() {
		/*
		 * 
		 */
		multiTest(
				"src/integrationtests/taggedValueTransformer/createPropertyValueTypeInfoTag/test_createPropertyValueTypeInfoTag.xml",
				new String[] { "xsd" }, "testResults/taggedValueTransformer/createPropertyValueTypeInfoTag/xsd",
				"src/integrationtests/taggedValueTransformer/createPropertyValueTypeInfoTag/reference/xsd");
	}
}
