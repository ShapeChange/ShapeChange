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
public class SQLTest extends BasicTestSCXML {

    @Test
    public void testBasicSQL() {
	/*
	 * SQL - basic test
	 */
	multiTest("src/test/resources/sql/basic/testEA_sql.xml", new String[] { "sql" }, "testResults/sql/basic",
		"src/test/resources/sql/basic/reference");
    }

    @Test
    public void testExtensionSchema() {

	multiTest("src/test/resources/sql/extensionSchema/testEA_sql_extensionSchema.xml", new String[] { "sql" },
		"testResults/sql/extensionSchema", "src/test/resources/sql/extensionSchema/reference");
    }

    @Test
    public void testGeometryParameters() {
	/*
	 * SQL - geometry parameters test
	 */
	multiTest("src/test/resources/sql/geometryParameters/testEA_sqlGeometryParameters.xml", new String[] { "sql" },
		"testResults/sql/geometryParameters", "src/test/resources/sql/geometryParameters/reference");
    }

    @Test
    public void testAssociativeTables() {
	/*
	 * SQL - associative tables tests
	 */
	multiTest("src/test/resources/sql/associativeTables/testEA_sqlAssociativeTables.xml", new String[] { "sql" },
		"testResults/sql/associativeTables/ddl", "src/test/resources/sql/associativeTables/reference/ddl");
    }
    
    @Test
    public void testAssociativeTablesWithSeparatePkFields() {

	multiTest("src/test/resources/sql/associativeTablesWithSeparatePkFields/testEA_sql_associativeTablesWithSeparatePkFields.xml", new String[] { "sql" },
		"testResults/sql/associativeTablesWithSeparatePkFields", "src/test/resources/sql/associativeTablesWithSeparatePkFields/reference");
    }

    @Test
    public void testRuleSqlAllForeignKeyOracleNamingStyle() {
	/*
	 * SQL - foreign keys Oracle naming style
	 */
	multiTest("src/test/resources/sql/foreignKeysOracleNamingStyle/testEA_sql_foreignKeysOracleNamingStyle.xml",
		new String[] { "sql" }, "testResults/sql/foreignKeysOracleNamingStyle",
		"src/test/resources/sql/foreignKeysOracleNamingStyle/reference");
    }

    @Test
    public void testRuleSqlClsCodelists() {
	/*
	 * SQL - codelist conversion
	 */
	multiTest("src/test/resources/sql/codelists/testEA_sql_codelists.xml", new String[] { "sql" },
		"testResults/sql/codelists", "src/test/resources/sql/codelists/reference");
    }

    @Test
    public void testRuleSqlClsCodelistsPods() {
	/*
	 * SQL - codelist conversion with PODS specific rule
	 */
	multiTest("src/test/resources/sql/codelistsPods/testEA_sql_codelists_pods.xml", new String[] { "sql" },
		"testResults/sql/codelists_pods", "src/test/resources/sql/codelistsPods/reference");
    }

    @Test
    public void testRuleSqlPropCheckConstraintRestrictTimeOfDate() {

	multiTest("src/test/resources/sql/restrictTimeOfDate/testEA_sql_restrictTimeOfDate.xml", new String[] { "sql" },
		"testResults/sql/restrictTimeOfDate", "src/test/resources/sql/restrictTimeOfDate/reference");
    }

    @Test
    public void testDefaultValues() {

	multiTest("src/test/resources/sql/defaultValues/testEA_sql_defaultValues.xml", new String[] { "sql" },
		"testResults/sql/defaultValues", "src/test/resources/sql/defaultValues/reference");
    }

    @Test
    public void testDdlAndReplicationSchema() {

	multiTest("src/test/resources/sql/ddlAndReplicationSchema/testEA_sqlDdlAndRepSchema.xml",
		new String[] { "sql", "xsd" }, "testResults/sql/ddlAndRepSchema",
		"src/test/resources/sql/ddlAndReplicationSchema/reference");
    }

    @Test
    public void testDocumentationViaExplicitComments() {

	/*
	 * SQL - Documentation via explicit comments
	 */
	multiTest("src/test/resources/sql/explicitComments/testEA_sql_explicitComments.xml", new String[] { "sql" },
		"testResults/sql/explicitComments", "src/test/resources/sql/explicitComments/reference");
    }

    @Test
    public void testDataTypeOneToMany() {

	/*
	 * SQL - Conversion of one to many relationships with data types
	 */
	multiTest("src/test/resources/sql/dataTypeEncoding_oneToMany/testEA_sql_dataTypeEncoding_oneToMany.xml",
		new String[] { "sql" }, "testResults/sql/dataTypeEncoding_oneToMany",
		"src/test/resources/sql/dataTypeEncoding_oneToMany/reference");
    }

    @Test
    public void testIdentifierStereotype() {

	/*
	 * SQL - Using stereotype <<identifier>>
	 */
	multiTest("src/test/resources/sql/identifierStereotype/testEA_sql_identifierStereotype.xml",
		new String[] { "sql" }, "testResults/sql/identifierStereotype",
		"src/test/resources/sql/identifierStereotype/reference");
    }

    @Test
    public void testOutputDdlModification() {

	multiTest("src/test/resources/sql/outputDdlModification/testEA_sql_outputDdlModification.xml",
		new String[] { "sql" }, "testResults/sql/outputDdlModification",
		"src/test/resources/sql/outputDdlModification/reference");
    }

    @Test
    public void testPrecisionAndScale() {

	multiTest("src/test/resources/sql/precisionAndScale/testEA_sql_precisionAndScale.xml", new String[] { "sql" },
		"testResults/sql/precisionAndScale", "src/test/resources/sql/precisionAndScale/reference");
    }

    @Test
    public void testNumericallyValuedCodeLists() {

	multiTest("src/test/resources/sql/numericallyValuedCodeLists/testEA_sql_numericallyValuedCodeList.xml",
		new String[] { "sql" }, "testResults/sql/numericallyValuedCodeLists",
		"src/test/resources/sql/numericallyValuedCodeLists/reference");
    }

    @Test
    public void testForeignKeyColumnSuffixCodelist() {

	multiTest("src/test/resources/sql/foreignKeyColumnSuffixCodelist/testEA_sql_foreignKeyColumnSuffixCodelist.xml",
		new String[] { "sql" }, "testResults/sql/foreignKeyColumnSuffixCodelist",
		"src/test/resources/sql/foreignKeyColumnSuffixCodelist/reference");
    }

    @Test
    public void testCheckConstraintForEnumeration() {

	multiTest("src/test/resources/sql/checkConstraintForEnumeration/testEA_sql_checkConstraintForEnumeration.xml",
		new String[] { "sql" }, "testResults/sql/checkConstraintForEnumeration",
		"src/test/resources/sql/checkConstraintForEnumeration/reference");
    }

    @Test
    public void testStatementFilters() {

	multiTest("src/test/resources/sql/statementFilters/testEA_sql_statementFilters.xml", new String[] { "sql" },
		"testResults/sql/statementFilters", "src/test/resources/sql/statementFilters/reference");
    }

    @Test
    public void testRepSchemaGeometryAnnotation() {

	multiTest("src/test/resources/sql/repSchemaGeometryAnnotation/test_repSchemaGeometryAnnotation.xml",
		new String[] { "xsd" }, "testResults/sql/repSchemaGeometryAnnotation",
		"src/test/resources/sql/repSchemaGeometryAnnotation/reference");
    }
    
    @Test
    public void testReflexiveRelationshipFieldSuffix() {

	multiTest("src/test/resources/sql/reflexiveRelationshipFieldSuffix/testEA_sql_reflexiveRelationshipFieldSuffix.xml",
		new String[] { "sql" }, "testResults/sql/reflexiveRelationshipFieldSuffix/ddl",
		"src/test/resources/sql/reflexiveRelationshipFieldSuffix/reference/ddl");
    }
    
    @Test
    public void testDatabaseSchemas() {

	multiTest("src/test/resources/sql/databaseSchemas/test_sql_databaseSchemas.xml",
		new String[] { "sql" }, "testResults/sql/databaseSchemas",
		"src/test/resources/sql/databaseSchemas/reference");
    }
    
    @Test
    public void testApplyForeignKeyColumnSuffixesInAssociativeTables() {

	multiTest("src/test/resources/sql/applyForeignKeyColumnSuffixesInAssociativeTables/test_sql_applyForeignKeyColumnSuffixesInAssociativeTables.xml",
		new String[] { "sql" }, "testResults/sql/applyForeignKeyColumnSuffixesInAssociativeTables",
		"src/test/resources/sql/applyForeignKeyColumnSuffixesInAssociativeTables/reference");
    }
    
    @Test
    public void testOrderAndUniqueness() {

	multiTest("src/test/resources/sql/orderAndUniqueness/testEA_sql_orderAndUniqueness.xml",
		new String[] { "sql" }, "testResults/sql/orderAndUniqueness",
		"src/test/resources/sql/orderAndUniqueness/reference");
    }
    
    @Test
    public void testForeignKeyCheckingOptions() {

	multiTest("src/test/resources/sql/foreignKeyCheckingOptions/testEA_sql_foreignKeyCheckingOptions.xml",
		new String[] { "sql" }, "testResults/sql/foreignKeyCheckingOptions",
		"src/test/resources/sql/foreignKeyCheckingOptions/reference");
    }
    
    @Test
    public void testExplicitlyEncodePkReferencedColumnInForeignKeys() {

	multiTest("src/test/resources/sql/explicitlyEncodePkReferencedColumnInForeignKeys/testEA_sql_explicitlyEncodePkReferencedColumnInForeignKeys.xml",
		new String[] { "sql" }, "testResults/sql/explicitlyEncodePkReferencedColumnInForeignKeys",
		"src/test/resources/sql/explicitlyEncodePkReferencedColumnInForeignKeys/reference");
    }
    
    @Test
    public void testSqlEncodingInfos() {

	multiTest("src/test/resources/sql/sqlEncodingInfos/testEA_sqlEncodingInfos.xml",
		new String[] { "sql","xml" }, "testResults/sql/sqlEncodingInfos/results",
		"src/test/resources/sql/sqlEncodingInfos/reference");
    }
}
