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
public class SQLDatabaseModelTest extends WindowsBasicTest {

	@Test
	public void testDatabaseModel() {

		multiTest(
				"src/integrationtests/sql/databaseModel/testEA_sql_databaseModel.xml",
				new String[] { "sql", "qea" }, "testResults/sql/databaseModel",
				"src/integrationtests/sql/databaseModel/reference");
	}

	@Test
	public void testDatabaseModel_mergeLinkedDocuments() {

		multiTest(
				"src/integrationtests/sql/databaseModel_mergeLinkedDocuments/testEA_sql_databaseModel_mergeLinkedDocuments.xml",
				new String[] { "sql", "qea" },
				"testResults/sql/databaseModel_mergeLinkedDocuments",
				"src/integrationtests/sql/databaseModel_mergeLinkedDocuments/reference");
	}

	@Test
	public void testUniqueConstraint() {

		multiTest("src/integrationtests/sql/sqlUnique/testEA_sql_unique.xml",
				new String[] { "sql", "qea" }, "testResults/sql/sqlUnique",
				"src/integrationtests/sql/sqlUnique/reference");
	}

	@Test
	public void testDatabaseModel_packageHierarchy_singleSchema() {

		multiTest(
				"src/integrationtests/sql/databaseModel_packageHierarchy/testEA_sql_databaseModel_packageHierarchy_singleSchema.xml",
				new String[] { "sql", "qea" },
				"testResults/sql/databaseModel_packageHierarchy/singleSchema",
				"src/integrationtests/sql/databaseModel_packageHierarchy/reference/singleSchema");
	}

	@Test
	public void testDatabaseModel_packageHierarchy_multipleSchemas() {

		multiTest(
				"src/integrationtests/sql/databaseModel_packageHierarchy/testEA_sql_databaseModel_packageHierarchy_multipleSchemas.xml",
				new String[] { "sql", "qea" },
				"testResults/sql/databaseModel_packageHierarchy/multipleSchemas",
				"src/integrationtests/sql/databaseModel_packageHierarchy/reference/multipleSchemas");
	}

	@Test
	public void testForeignKeyOptions() {

		multiTest(
				"src/integrationtests/sql/foreignKeyOptions/testEA_sql_foreignKeyOptions.xml",
			new String[] {
				"sql", "qea" },
				"testResults/sql/foreignKeyOptions",
				"src/integrationtests/sql/foreignKeyOptions/reference");
	}

	@Test
	public void testConstraintNameUsingShortName() {

		multiTest(
				"src/integrationtests/sql/constraintNameUsingShortName/testEA_sql_constraintNameUsingShortName.xml",
				new String[] { "sql", "qea" },
				"testResults/sql/constraintNameUsingShortName",
				"src/integrationtests/sql/constraintNameUsingShortName/reference");
	}

	@Test
	public void testIndexNameUsingShortName() {

		multiTest(
				"src/integrationtests/sql/indexNameUsingShortName/testEA_sql_indexNameUsingShortName.xml",
				new String[] { "sql", "qea" },
				"testResults/sql/indexNameUsingShortName",
				"src/integrationtests/sql/indexNameUsingShortName/reference");
	}

	@Test
	public void testCheckConstraintForRange() {

		multiTest(
				"src/integrationtests/sql/checkConstraintForRange/testEA_sql_checkConstraintForRange.xml",
				new String[] { "sql", "qea" },
				"testResults/sql/checkConstraintForRange",
				"src/integrationtests/sql/checkConstraintForRange/reference");
	}

	@Test
	public void testRepresentTaggedValues() {

		multiTest(
				"src/integrationtests/sql/databaseModel_representTaggedValues/testEA_sql_representTaggedValues.xml",
				new String[] { "qea" }, "testResults/sql/representTaggedValues",
				"src/integrationtests/sql/databaseModel_representTaggedValues/reference");
	}
	
	@Test
	public void testLengthQualifier() {

		multiTest(
				"src/integrationtests/sql/lengthQualifier/testEA_sql_lengthQualifier.xml",
				new String[] { "sql","qea" }, "testResults/sql/lengthQualifier",
				"src/integrationtests/sql/lengthQualifier/reference");
	}
}
