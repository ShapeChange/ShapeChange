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
 * (c) 2002-2012 interactive instruments GmbH, Bonn, Germany
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

import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashMap;
import java.util.Map.Entry;

import javax.xml.transform.TransformerException;

import org.apache.xpath.XPathAPI;
import org.w3c.dom.NodeList;

/**
 * Instance of ShapeChange to be used in test cases
 */
public class TestInstance {

	Options options = null;
	ShapeChangeResult result = null;

	/**
	 * Create and execute the ShapeChange instance
	 * 
	 * @param config
	 *            URI/Path of the configuration file
	 */
	public TestInstance(String config) {
		init(config, null);
	}

	public TestInstance(String config, HashMap<String, String> replacevalues) {
		init(config, replacevalues);
	}

	private void init(String config, HashMap<String, String> replacevalues) {
		try {
			options = new Options();
			result = new ShapeChangeResult(options);

			String javaVersion = System.getProperty("java.version");
			String[] components = javaVersion.split("\\.");
			int major = Integer.parseInt(components[0]);
			int minor = Integer.parseInt(components[1]);
			if (major == 1 && minor < 8) {
				result.addError(null, 18, javaVersion);
				System.exit(1);
			}

			if (replacevalues != null)
				for (Entry<String, String> me : replacevalues.entrySet()) {
					options.setReplaceValue(me.getKey(), me.getValue());
				}

			Converter converter = new Converter(options, result);
			options.configFile = config;
			options.loadConfiguration();
			converter.convert();
		} catch (ShapeChangeAbortException e) {
			result.addFatalError(e.getMessage());
			fail("ShapeChange encountered an abort error.");
		} catch (Exception e) {
			if(e.getMessage() != null) {
				result.addFatalError(e.getMessage());
			}
			e.printStackTrace();
			fail("ShapeChange encountered an unknown error.");
		}
	}

	/**
	 * Checks, if errors were reported during execution
	 * 
	 * @return true, if no error was logged in the log file
	 */
	protected boolean noError() {

		// if there is no result file, there is an error
		if (result == null)
			return false;

		try {
			NodeList nodes = XPathAPI.selectNodeList(result.document,
					"//r:Error|//r:FatalError",
					result.document.getDocumentElement());
			if (nodes.getLength() > 0)
				return false;
		} catch (TransformerException e) {
			fail("An error occured processing an Xpath expression on the log file.");
			e.printStackTrace();
			return false;
		}

		return true;
	}
}
