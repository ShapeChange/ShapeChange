package de.interactive_instruments.ShapeChange;

import java.io.InputStream;
import java.net.URL;

import org.junit.Test;

public class MinimalAndTesttXmiModelTest extends BasicTest {

	@Test
	public void testMinimalAndTestXmiModels() {
		/*
		 * Invoke without parameters, if we are connected
		 */
		try {
			URL url = new URL(
					"http://shapechange.net/resources/config/minimal.xml");
			InputStream configStream = url.openStream();
			if (configStream != null) {
				xsdTest(null, new String[] { "test" }, null, ".",
						"src/test/resources/reference/xsd");
			}
		} catch (Exception e) {
		}
	
		/*
		 * Process the XMI 1.0 test model
		 */
		xsdTest("src/test/resources/config/testXMI.xml", new String[] { "test" }, null,
				"testResults/xmi/INPUT", "src/test/resources/reference/xsd");
	}

}
