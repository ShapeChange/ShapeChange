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

import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("SCXML")
public class MinimalAndTesttXmiModelTest extends BasicTestSCXML {

    @Test
    public void testMinimalAndTestXmiModels() {
	/*
	 * Invoke without parameters, if we are connected
	 * 
	 * NOTE: Input model type of minimal configuration is XMI
	 */
	try {
	    URL url = URI.create("http://shapechange.net/resources/config/minimal.xml").toURL();
	    InputStream configStream = url.openStream();
	    if (configStream != null) {
		xsdTest(null, new String[] { "test" }, null, ".", "src/integrationtests/xmi/basic/reference");
	    }
	} catch (Exception e) {
	}

	/*
	 * Process the XMI 1.0 test model
	 */
	xsdTest("src/integrationtests/xmi/basic/testXMI.xml", new String[] { "test" }, null,
		"testResults/xmi/basic/INPUT", "src/integrationtests/xmi/basic/reference");
    }

}
