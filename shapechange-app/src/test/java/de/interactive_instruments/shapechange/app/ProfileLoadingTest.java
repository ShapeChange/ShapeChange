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
public class ProfileLoadingTest extends BasicTestSCXML {

	@Test
	public void profileLoading_explicitProfiles() {

		multiTest(
				"src/integrationtests/profileLoading/explicitProfiles/testEA_profileLoading_explicitProfiles.xml",
				new String[] { "xml" },
				"testResults/profileLoading/explicitProfiles/results",
				"src/integrationtests/profileLoading/explicitProfiles/reference/results");
	}
	
	@Test
	public void profileLoading_nonExplicitProfiles() {

		multiTest(
				"src/integrationtests/profileLoading/nonExplicitProfiles/testEA_profileLoading_nonExplicitProfiles.xml",
				new String[] { "xml" },
				"testResults/profileLoading/nonExplicitProfiles/results",
				"src/integrationtests/profileLoading/nonExplicitProfiles/reference/results");
	}
	
	/*
	 * Uncomment the following to derive model3.xml if the test model to
	 * perform the model diff with had changed.
	 */
	// @Test
	// public void profileLoading_exportDiffModel() {
	//
	// String exportDiffModelConfig =
	// "src/integrationtests/profileLoading/explicitProfiles/helper_profileLoading_explicitProfiles_exportDiffModel.xml";
	// System.out.println("Executing " + exportDiffModelConfig);
	// new TestInstance(exportDiffModelConfig);
	// }

}
