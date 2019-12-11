package de.interactive_instruments.ShapeChange;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TargetModeTest extends BasicTest {

	@Test
	public void test() {
		ShapeChangeResult result = execute("src/test/resources/config/testXMI_targetMode.xml");
		assertEquals(ProcessMode.enabled, result.options().targetMode(Options.TargetXmlSchemaClass), "Target must be configured as enabled if the target is configured multiple times and one or more of its occurrences are enabled");
	}

}
