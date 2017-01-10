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
		multiTest(
				"src/test/resources/config/testEA_sqlGeometryParameters.xml",
				new String[] { "sql" },
				"testResults/sql/geometryParameters",
				"src/test/resources/reference/sql/geometryParameters");
	}

	@Test
	public void testAssociativeTables() {
		/*
		 * SQL - associative tables tests
		 */
		multiTest(
				"src/test/resources/config/testEA_sqlAssociativeTables.xml",
				new String[] { "sql" }, "testResults/sql/associativeTables",
				"src/test/resources/reference/sql/associativeTables");
	}

}
