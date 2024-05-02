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
 * (c) 2002-2022 interactive instruments GmbH, Bonn, Germany
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
public class Ldproxy2Test extends BasicTestSCXML {

	@Test
	public void test_ldproxy2_basic() {
		
		multiTest("src/test/resources/ldproxy2/basic/test_ldproxy2.xml",
			new String[] { "yaml", "yml" }, "testResults/ldproxy2/basic/results",
			"src/test/resources/ldproxy2/basic/reference/results");
	}
	
	@Test
	public void test_ldproxy2_titleAttribute() {
		
		multiTest("src/test/resources/ldproxy2/titleAttribute/test_ldproxy2.xml",
			new String[] { "yaml", "yml" }, "testResults/ldproxy2/titleAttribute/results",
			"src/test/resources/ldproxy2/titleAttribute/reference/results");
	}
	
	@Test
	public void test_ldproxy2_queryables() {
		
		multiTest("src/test/resources/ldproxy2/queryables/test_ldproxy2.xml",
			new String[] { "yaml", "yml" }, "testResults/ldproxy2/queryables/results",
			"src/test/resources/ldproxy2/queryables/reference/results");
	}
	
	@Test
	public void test_ldproxy2_foreignKeyColumnSuffixCodelist() {
		
		multiTest("src/test/resources/ldproxy2/foreignKeyColumnSuffixCodelist/test_foreignKeyColumnSuffixCodelist.xml",
			new String[] { "yaml", "yml" }, "testResults/ldproxy2/foreignKeyColumnSuffixCodelist/results",
			"src/test/resources/ldproxy2/foreignKeyColumnSuffixCodelist/reference/results");
	}
	
	@Test
	public void test_ldproxy2_associativeTablesWithSeparatePkField() {
		
		multiTest("src/test/resources/ldproxy2/associativeTablesWithSeparatePkField/test_associativeTablesWithSeparatePkField.xml",
			new String[] { "yaml", "yml", "sql" }, "testResults/ldproxy2/associativeTablesWithSeparatePkField/results",
			"src/test/resources/ldproxy2/associativeTablesWithSeparatePkField/reference/results");
	}
	
	@Test
	public void test_ldproxy2_reflexiveRelation() {
		
		multiTest("src/test/resources/ldproxy2/reflexiveRelation/test_reflexiveRelation.xml",
			new String[] { "yaml", "yml", "sql" }, "testResults/ldproxy2/reflexiveRelation/results",
			"src/test/resources/ldproxy2/reflexiveRelation/reference/results");
	}
	
	@Test
	public void test_ldproxy2_gmlOutput() {
		
		multiTest("src/test/resources/ldproxy2/gmlOutput/test_ldproxy2_gmlOutput.xml",
			new String[] { "yaml", "yml" }, "testResults/ldproxy2/gmlOutput/results",
			"src/test/resources/ldproxy2/gmlOutput/reference/results");
	}
	
	@Test
	public void test_ldproxy2_complexDatatypes() {
		
		multiTest("src/test/resources/ldproxy2/complexDatatypes/test_ldproxy2_complexDatatypes.xml",
			new String[] { "yaml", "yml", "sql" }, "testResults/ldproxy2/complexDatatypes",
			"src/test/resources/ldproxy2/complexDatatypes/reference/results");
	}
	
	@Test
	public void test_ldproxy2_labelTemplate() {
		
		multiTest("src/test/resources/ldproxy2/labelTemplate/test_ldproxy2.xml",
			new String[] { "yaml", "yml", "sql" }, "testResults/ldproxy2/labelTemplate/results",
			"src/test/resources/ldproxy2/labelTemplate/reference/results");
	}
	
	@Test
	public void test_ldproxy2_fragmentsConcatCoalesce() {
		
		multiTest("src/test/resources/ldproxy2/fragmentsConcatCoalesce/test_ldproxy2_fragmentsConcatCoalesce.xml",
			new String[] { "yaml", "yml", "xsd", "xml" }, "testResults/ldproxy2/fragmentsConcatCoalesce/results",
			"src/test/resources/ldproxy2/fragmentsConcatCoalesce/reference");
	}
	
	@Test
	public void test_ldproxy2_coretable() {
		
		multiTest("src/test/resources/ldproxy2/coretable/test_ldproxy2_coretable.xml",
			new String[] { "yaml", "yml" }, "testResults/ldproxy2/coretable/results",
			"src/test/resources/ldproxy2/coretable/reference/results");
	}
	
	@Test
	public void test_ldproxy2_linearizeCurves() {
		
		multiTest("src/test/resources/ldproxy2/linearizeCurves/test_ldproxy2LinearizeCurves.xml",
			new String[] { "yaml", "yml" }, "testResults/ldproxy2/linearizeCurves/results",
			"src/test/resources/ldproxy2/linearizeCurves/reference/results");
	}
	
	@Test
	public void test_ldproxy2_coretableVersion() {
		
		multiTest("src/test/resources/ldproxy2/coretableVersion/test_ldproxy2_coretableVersion.xml",
			new String[] { "yaml", "yml" }, "testResults/ldproxy2/coretableVersion/results",
			"src/test/resources/ldproxy2/coretableVersion/reference/results");
	}
}
