package de.interactive_instruments.ShapeChange;

import org.junit.Test;

public class ReplicationXMLSchemaTest extends WindowsBasicTest {
	
	@Test
	public void testReplicationSchema() {
		/*
		 * Replication schema target
		 */
		multiTest("src/test/resources/config/testEA_repSchema.xml",
				new String[] { "xsd" }, "testResults/repSchema/repXsd",
				"src/test/resources/reference/xsd/replicationSchema");
	}

}
