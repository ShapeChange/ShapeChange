package de.interactive_instruments.ShapeChange;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("SCXML")
public class TargetModeTest extends BasicTestSCXML {

    @Test
    public void test() {
	ShapeChangeResult result = executeScxml("src/test/resources/xmi/targetMode/testXMI_targetMode.xml");
	assertEquals(ProcessMode.enabled, result.options().targetMode(Options.TargetXmlSchemaClass),
		"Target must be configured as enabled if the target is configured multiple times and one or more of its occurrences are enabled");
    }

}
