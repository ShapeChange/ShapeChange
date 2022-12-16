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
public class GfsTemplateTest extends BasicTestSCXML {

	@Test
	public void testBasic() {
		multiTest("src/test/resources/gfs/basic/test_gfs_basic.xml",
				new String[] { "xsd", "xml","gfs" }, "testResults/gfs/basic/results",
				"src/test/resources/gfs/basic/reference/results");
	}
	
	@Test
	public void testCodelistGml33() {
		multiTest("src/test/resources/gfs/codelistGml33/test_gfs_codelistGml33.xml",
				new String[] { "xsd", "xml","gfs" }, "testResults/gfs/codelistGml33/results",
				"src/test/resources/gfs/codelistGml33/reference/results");
	}
	
	@Test
	public void testDataTypeSubtypePropsWithSameName() {
		multiTest("src/test/resources/gfs/dataTypeSubtypePropsWithSameName/test_gfs_dataTypeSubtypePropsWithSameName.xml",
				new String[] { "xsd", "xml","gfs" }, "testResults/gfs/dataTypeSubtypePropsWithSameName/results",
				"src/test/resources/gfs/dataTypeSubtypePropsWithSameName/reference/results");
	}
}
