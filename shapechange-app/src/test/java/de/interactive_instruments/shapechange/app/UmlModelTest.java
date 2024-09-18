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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.shapechange.app;

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

		multiTest("src/integrationtests/uml/basic/testEA_uml_basic.xml",
				new String[] { "qea" }, "testResults/uml/basic",
				"src/integrationtests/uml/basic/reference");
	}

	@Test
	public void testLinkedDocuments() {

		multiTest(
				"src/integrationtests/uml/linkedDocument/testEA_uml_linkedDocument.xml",
				new String[] { "qea" }, "testResults/uml/linkedDocument",
				"src/integrationtests/uml/linkedDocument/reference");
	}

	@Test
	public void testConnectorEndOwnedByClassifier() {

		multiTest(
				"src/integrationtests/uml/connectorEndOwnedByClassifier/test_uml_connectorEndOwnedByClassifier.xml",
				new String[] { "qea", "xml" },
				"testResults/uml/connectorEndOwnedByClassifier/results",
				"src/integrationtests/uml/connectorEndOwnedByClassifier/reference/results");
	}
		
	@Test
	public void testMergeConstraintCommentsIntoText() {

		multiTest(
				"src/integrationtests/uml/mergeConstraintCommentsIntoText/test_uml_mergeConstraintCommentsIntoText.xml",
				new String[] { "qea" },
				"testResults/uml/mergeConstraintCommentsIntoText/results",
				"src/integrationtests/uml/mergeConstraintCommentsIntoText/reference/results");
	}
	
	@Test
	public void testAuthorAndStatus() {

		multiTest(
				"src/integrationtests/uml/authorAndStatus/test_uml_authorAndStatus.xml",
				new String[] { "qea" },
				"testResults/uml/authorAndStatus/results",
				"src/integrationtests/uml/authorAndStatus/reference/results");
	}
	
	@Test
	public void testPreservePackageHierarchy() {

		multiTest(
				"src/integrationtests/uml/preservePackageHierarchy/test_uml_preservePackageHierarchy.xml",
				new String[] { "qea" },
				"testResults/uml/preservePackageHierarchy",
				"src/integrationtests/uml/preservePackageHierarchy/reference");
	}
	
	@Test
	public void testMdgValidation() {

		multiTest(
				"src/integrationtests/uml/mdgValidation/testEA_mdg_validation.xml",
				new String[] { "qea" },
				"testResults/uml/mdgValidation",
				"src/integrationtests/uml/mdgValidation/reference");
	}
}
