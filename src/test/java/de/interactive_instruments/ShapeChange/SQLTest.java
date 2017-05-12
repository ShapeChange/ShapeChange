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

import org.junit.Test;

public class SQLTest extends WindowsBasicTest {

	@Test
	public void testBasicSQL() {
		/*
		 * SQL - basic text
		 */
		multiTest("src/test/resources/config/testEA_sql.xml",
				new String[] { "sql" }, "testResults/sql/basic",
				"src/test/resources/reference/sql/basic");
	}

	@Test
	public void testGeometryParameters() {
		/*
		 * SQL - geometry parameters test
		 */
		multiTest("src/test/resources/config/testEA_sqlGeometryParameters.xml",
				new String[] { "sql" }, "testResults/sql/geometryParameters",
				"src/test/resources/reference/sql/geometryParameters");
	}

	@Test
	public void testAssociativeTables() {
		/*
		 * SQL - associative tables tests
		 */
		multiTest("src/test/resources/config/testEA_sqlAssociativeTables.xml",
				new String[] { "sql" }, "testResults/sql/associativeTables",
				"src/test/resources/reference/sql/associativeTables");
	}

	@Test
	public void testRuleSqlAllForeignKeyOracleNamingStyle() {
		/*
		 * SQL - foreign keys Oracle naming style
		 */
		multiTest(
				"src/test/resources/config/testEA_sql_foreignKeysOracleNamingStyle.xml",
				new String[] { "sql" },
				"testResults/sql/foreignKeysOracleNamingStyle",
				"src/test/resources/reference/sql/foreignKeysOracleNamingStyle");
	}

	@Test
	public void testRuleSqlClsCodelists() {
		/*
		 * SQL - codelist conversion
		 */
		multiTest("src/test/resources/config/testEA_sql_codelists.xml",
				new String[] { "sql" }, "testResults/sql/codelists",
				"src/test/resources/reference/sql/codelists");
		
		/*
		 * SQL - codelist conversion with PODS specific rule
		 */
		multiTest("src/test/resources/config/testEA_sql_codelists_pods.xml",
				new String[] { "sql" }, "testResults/sql/codelists_pods",
				"src/test/resources/reference/sql/codelists_pods");
	}
	
	@Test
	public void testRuleSqlPropCheckConstraintRestrictTimeOfDate() {
		
		multiTest(
				"src/test/resources/config/testEA_sql_restrictTimeOfDate.xml",
				new String[] { "sql" },
				"testResults/sql/restrictTimeOfDate",
				"src/test/resources/reference/sql/restrictTimeOfDate");
	}
	
	@Test
	public void testDefaultValues() {
		
		multiTest(
				"src/test/resources/config/testEA_sql_defaultValues.xml",
				new String[] { "sql" },
				"testResults/sql/defaultValues",
				"src/test/resources/reference/sql/defaultValues");
	}
	
	@Test
	public void testDdlAndReplicationSchema() {
		
		multiTest(
				"src/test/resources/config/testEA_sqlDdlAndRepSchema.xml",
				new String[] { "sql","xsd" },
				"testResults/sql/ddlAndRepSchema",
				"src/test/resources/reference/sql/ddlAndRepSchema");
	}

}
