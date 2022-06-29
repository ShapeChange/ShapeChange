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
 * (c) 2002-2019 interactive instruments GmbH, Bonn, Germany
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
public class GeoPackageTest extends BasicTestSCXML {

    @Test
    public void test_GeoPackage_basic() {

	executeScxml("src/test/resources/gpkg/basic/test_gpkg_basic.xml");

	/*
	 * TODO: implement GeoPackage diff
	 * 
	 * Schema diff via https://www.schemacrawler.com/ ?
	 * https://github.com/schemacrawler/SchemaCrawler/blob/master/schemacrawler-diff/src/test/java/schemacrawler/integration/test/DiffTest.java
	 * 
	 * Unclear if schema diff via schemacrawler would be complete
	 * 
	 * In addition, we'd need to diff the actual data, too
	 */
//	multiTest("src/test/resources/gpkg/basic/test_gpkg_basic.xml", new String[] { "gpkg" },
//		"testResults/gpkg/basic/results", "src/test/resources/gpkg/basic/reference/results");
    }
    
    @Test
    public void test_GeoPackage_basic_2srs() {

	executeScxml("src/test/resources/gpkg/basic/test_gpkg_basic_2srs.xml");

    }

}
