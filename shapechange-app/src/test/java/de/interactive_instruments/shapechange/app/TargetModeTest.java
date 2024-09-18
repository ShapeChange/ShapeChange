package de.interactive_instruments.shapechange.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ProcessMode;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;

@Tag("SCXML")
public class TargetModeTest extends BasicTestSCXML {

    @Test
    public void test() {
	ShapeChangeResult result = executeScxml("src/integrationtests/xmi/targetMode/testXMI_targetMode.xml");
	assertEquals(ProcessMode.enabled, result.options().targetMode(Options.TargetXmlSchemaClass),
		"Target must be configured as enabled if the target is configured multiple times and one or more of its occurrences are enabled");
    }

}
