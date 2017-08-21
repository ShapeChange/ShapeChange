package de.interactive_instruments.ShapeChange;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TargetModeTest extends BasicTest {

	@Test
	public void test() {
		ShapeChangeResult result = execute("src/test/resources/config/testXMI_targetMode.xml");
		assertEquals("Target must be configured as enabled if the target is configured multiple times and one or more of its occurrences are enabled", ProcessMode.enabled, result.options().targetMode(Options.TargetXmlSchemaClass));
	}

}
