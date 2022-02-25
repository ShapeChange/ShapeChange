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
 * (c) 2002-2018 interactive instruments GmbH, Bonn, Germany
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

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot
 *         de)
 *
 */
@Tag("EARequired")
public class UmlModelTest extends WindowsBasicTest {
    
	@Test
	public void testBasicFunctionality() {

		multiTest("src/test/resources/uml/basic/testEA_uml_basic.xml",
				new String[] { "eap" }, "testResults/uml/basic",
				"src/test/resources/uml/basic/reference");
	}

	@Test
	public void testLinkedDocuments() {

		multiTest(
				"src/test/resources/uml/linkedDocument/testEA_uml_linkedDocument.xml",
				new String[] { "eap" }, "testResults/uml/linkedDocument",
				"src/test/resources/uml/linkedDocument/reference");
	}

	@Test
	public void testConnectorEndOwnedByClassifier() {

		multiTest(
				"src/test/resources/uml/connectorEndOwnedByClassifier/test_uml_connectorEndOwnedByClassifier.xml",
				new String[] { "eap", "xml" },
				"testResults/uml/connectorEndOwnedByClassifier/results",
				"src/test/resources/uml/connectorEndOwnedByClassifier/reference/results");
	}
		
	@Test
	public void testMergeConstraintCommentsIntoText() {

		multiTest(
				"src/test/resources/uml/mergeConstraintCommentsIntoText/test_uml_mergeConstraintCommentsIntoText.xml",
				new String[] { "eap" },
				"testResults/uml/mergeConstraintCommentsIntoText/results",
				"src/test/resources/uml/mergeConstraintCommentsIntoText/reference/results");
	}
	
	@Test
	public void testAuthorAndStatus() {

		multiTest(
				"src/test/resources/uml/authorAndStatus/test_uml_authorAndStatus.xml",
				new String[] { "eap" },
				"testResults/uml/authorAndStatus/results",
				"src/test/resources/uml/authorAndStatus/reference/results");
	}
	
	@Test
	public void testPreservePackageHierarchy() {

		multiTest(
				"src/test/resources/uml/preservePackageHierarchy/test_uml_preservePackageHierarchy.xml",
				new String[] { "eap" },
				"testResults/uml/preservePackageHierarchy",
				"src/test/resources/uml/preservePackageHierarchy/reference");
	}
	
	@Test
	public void testMdgValidation() {

		multiTest(
				"src/test/resources/uml/mdgValidation/testEA_mdg_validation.xml",
				new String[] { "eap" },
				"testResults/uml/mdgValidation",
				"src/test/resources/uml/mdgValidation/reference");
	}
}
