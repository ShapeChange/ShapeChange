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
package de.interactive_instruments.ShapeChange;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("SCXML")
public class ApplicationSchemaMetadataTest extends BasicTestSCXML {

	@Test
	public void testApplicationSchemaMetadata_identifyProfiles() {
		/*
		 * Test derivation of application schema metadata.
		 */
		multiTest("src/test/resources/appSchemaMetadata/identifyProfiles/testEA_appSchemaMetadata_identifyProfiles.xml",
				new String[] { "xml" }, "testResults/appSchemaMetadata/identifyProfiles/INPUT",
				"src/test/resources/appSchemaMetadata/identifyProfiles/reference/INPUT");
	}

	@Test
	public void testApplicationSchemaMetadata_identifyTypeUsage() {
		/*
		 * Test derivation of application schema metadata.
		 */
		multiTest("src/test/resources/appSchemaMetadata/identifyTypeUsage/testEA_appSchemaMetadata_identifyTypeUsage.xml",
				new String[] { "xml" }, "testResults/appSchemaMetadata/identifyTypeUsage/INPUT",
				"src/test/resources/appSchemaMetadata/identifyTypeUsage/reference/INPUT");
	}
	
	@Test
	public void testApplicationSchemaMetadata_identifyPropertiesWithSpecificTaggedValues() {
		/*
		 * Test derivation of application schema metadata.
		 */
		multiTest("src/test/resources/appSchemaMetadata/identifyPropertiesWithSpecificTaggedValues/testEA_appSchemaMetadata_identifyPropertiesWithSpecificTaggedValues.xml",
				new String[] { "xml" }, "testResults/appSchemaMetadata/identifyPropertiesWithSpecificTaggedValues/results",
				"src/test/resources/appSchemaMetadata/identifyPropertiesWithSpecificTaggedValues/reference/results");
	}
}
