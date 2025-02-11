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

@Tag("EARequired")
public class ProfileTransferTest extends WindowsBasicTest {

	@Test
	public void profileTransfer_test1_EARepoFromInputConfiguration() {

		execute("src/integrationtests/profileTransfer/test1/test1_profileTransfer_step1_transferToCopy.xml");

		multiTest(
				"src/integrationtests/profileTransfer/test1/test1_profileTransfer_step2_export.xml",
				new String[] { "xml" },
				"testResults/profileTransfer/test1/results/export",
				"src/integrationtests/profileTransfer/test1/reference/export");
	}
	
	@Test
	public void profileTransfer_test2_EARepoFromTargetConfiguration() {

		execute("src/integrationtests/profileTransfer/test2/test2_profileTransfer_step1_transferToCopy.xml");

		multiTest(
				"src/integrationtests/profileTransfer/test2/test2_profileTransfer_step2_export.xml",
				new String[] { "xml" },
				"testResults/profileTransfer/test2/results/export",
				"src/integrationtests/profileTransfer/test2/reference/export");
	}

	/*
	 * Uncomment and run the following to export the model.
	 */
	// @Test
	// public void profileTransfer_exportModel() {
	//
	// String exportModelConfig =
	// "src/integrationtests/profileTransfer/helper_profileTransfer.xml";
	// System.out.println("Executing " + exportModelConfig);
	// new TestInstance(exportModelConfig);
	// }
}
