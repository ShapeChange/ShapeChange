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
public class VariousFunctionalityTest extends BasicTestSCXML {

    @Test
    public void testNotEncoded() {

	multiTest("src/test/resources/variousFunctionality/notEncoded/testEA_notEncoded.xml",
		new String[] { "xsd", "json", "sql" }, "testResults/variousFunctionality/notEncoded",
		"src/test/resources/variousFunctionality/notEncoded/reference");
    }

    @Test
    public void testLoadingWithProhibitedStatusSetting() {

	multiTest(
		"src/test/resources/variousFunctionality/loadingWithProhibitedStatus/testEA_loadingWithProhibitedStatusSetting.xml",
		new String[] { "xsd", "html" }, "testResults/loadingWithProhibitedStatusSetting/results",
		"src/test/resources/variousFunctionality/loadingWithProhibitedStatus/reference");
    }

    @Test
    public void testBasicModelValidation_strict_fail() {

	executeAndError(
		"src/test/resources/variousFunctionality/basicModelValidation/strict_fail/testEA_basicModelValidation_strict_fail.xml",
		"It is expected that the log contains an error, informing the user about model validation issues.");
    }

    @Test
    public void testBasicModelValidation_lax() {

	multiTest("src/test/resources/variousFunctionality/basicModelValidation/lax/testEA_basicModelValidation_lax.xml",
		new String[] { "xsd" }, "testResults/variousFunctionality/basicModelValidation/lax",
		"src/test/resources/variousFunctionality/basicModelValidation/lax/reference");
    }
    
    @Test
    public void testBasicModelValidation_strict_success() {

	multiTest("src/test/resources/variousFunctionality/basicModelValidation/strict_success/testEA_basicModelValidation_strict_success.xml",
		new String[] { "xsd" }, "testResults/variousFunctionality/basicModelValidation/strict_success",
		"src/test/resources/variousFunctionality/basicModelValidation/strict_success/reference");
    }
}
