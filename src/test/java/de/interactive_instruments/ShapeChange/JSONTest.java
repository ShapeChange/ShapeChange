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

import org.junit.Test;

public class JSONTest extends WindowsBasicTest {

	@Test
	public void testJsonWithGeoservicesEncodingRule() {
		/*
		 * JSON encoding with geoservices encoding rule
		 */
		String[] typenamesGsr = { "FeatureType1", "FeatureType2" };
		jsonTest("src/test/resources/config/testEA_JsonGsr.xml", typenamesGsr,
				"testResults/ea/json/geoservices/INPUT",
				"src/test/resources/reference/json/geoservices");
	}

	@Test
	public void testJsonWithExtendedGeoservicesEncodingRule() {
		/*
		 * JSON encoding with extended geoservices encoding rule
		 */
		String[] typenamesGsrExtended = { "DataType", "DataType2",
				"FeatureType1", "FeatureType2", "NilUnion", "Union" };
		jsonTest("src/test/resources/config/testEA_JsonGsrExtended.xml",
				typenamesGsrExtended,
				"testResults/ea/json/geoservices_extended/INPUT",
				"src/test/resources/reference/json/geoservices_extended");
	}

}
