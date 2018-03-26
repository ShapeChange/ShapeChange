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

public class SQLDatabaseModelTest extends WindowsBasicTest {

	@Test
	public void testDatabaseModel() {

		multiTest(
				"src/test/resources/sql/databaseModel/testEA_sql_databaseModel.xml",
				new String[] { "sql", "eap" }, "testResults/sql/databaseModel",
				"src/test/resources/sql/databaseModel/reference");
	}

	@Test
	public void testDatabaseModel_mergeLinkedDocuments() {

		multiTest(
				"src/test/resources/sql/databaseModel_mergeLinkedDocuments/testEA_sql_databaseModel_mergeLinkedDocuments.xml",
				new String[] { "sql", "eap" },
				"testResults/sql/databaseModel_mergeLinkedDocuments",
				"src/test/resources/sql/databaseModel_mergeLinkedDocuments/reference");
	}

	@Test
	public void testUniqueConstraint() {

		multiTest("src/test/resources/sql/sqlUnique/testEA_sql_unique.xml",
				new String[] { "sql", "eap" }, "testResults/sql/sqlUnique",
				"src/test/resources/sql/sqlUnique/reference");
	}

	@Test
	public void testDatabaseModel_packageHierarchy_singleSchema() {

		multiTest(
				"src/test/resources/sql/databaseModel_packageHierarchy/testEA_sql_databaseModel_packageHierarchy_singleSchema.xml",
				new String[] { "sql", "eap" },
				"testResults/sql/databaseModel_packageHierarchy/singleSchema",
				"src/test/resources/sql/databaseModel_packageHierarchy/reference/singleSchema");
	}

	@Test
	public void testDatabaseModel_packageHierarchy_multipleSchemas() {

		multiTest(
				"src/test/resources/sql/databaseModel_packageHierarchy/testEA_sql_databaseModel_packageHierarchy_multipleSchemas.xml",
				new String[] { "sql", "eap" },
				"testResults/sql/databaseModel_packageHierarchy/multipleSchemas",
				"src/test/resources/sql/databaseModel_packageHierarchy/reference/multipleSchemas");
	}

	@Test
	public void testForeignKeyOptions() {

		multiTest(
				"src/test/resources/sql/foreignKeyOptions/testEA_sql_foreignKeyOptions.xml",
				new String[] { "sql", "eap" },
				"testResults/sql/foreignKeyOptions",
				"src/test/resources/sql/foreignKeyOptions/reference");
	}
}
