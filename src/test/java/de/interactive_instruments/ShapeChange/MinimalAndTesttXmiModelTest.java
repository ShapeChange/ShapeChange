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

import java.io.InputStream;
import java.net.URL;

import org.junit.jupiter.api.Test;

public class MinimalAndTesttXmiModelTest extends BasicTest {

	@Test
	public void testMinimalAndTestXmiModels() {
		/*
		 * Invoke without parameters, if we are connected
		 */
		try {
			URL url = new URL(
					"http://shapechange.net/resources/config/minimal.xml");
			InputStream configStream = url.openStream();
			if (configStream != null) {
				xsdTest(null, new String[] { "test" }, null, ".",
						"src/test/resources/reference/xsd");
			}
		} catch (Exception e) {
		}

		/*
		 * Process the XMI 1.0 test model
		 */
		xsdTest("src/test/resources/config/testXMI.xml",
				new String[] { "test" }, null, "testResults/xmi/INPUT",
				"src/test/resources/reference/xsd");
	}

}
