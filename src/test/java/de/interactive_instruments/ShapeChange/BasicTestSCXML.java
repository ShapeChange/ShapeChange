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
 * (c) 2002-2019 interactive instruments GmbH, Bonn, Germany
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

import de.interactive_instruments.ShapeChange.scxmltest.SCXMLTestResourceConverter;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class BasicTestSCXML extends BasicTest {

    SCXMLTestResourceConverter scxmlConverter = new SCXMLTestResourceConverter();

    protected ShapeChangeResult executeScxml(String configPath) {

	String pathToRelevantConfig;
	try {
	    pathToRelevantConfig = scxmlConverter.updateSCXMLTestResources(configPath);
	} catch (Exception e) {
	    e.printStackTrace();
	    fail("Exception occurred while updating SCXML test resources.", e);
	    return null;
	}

	return execute(pathToRelevantConfig);
    }

    @Override
    protected void multiTest(String configPath, String[] fileFormatsToCheck, String basedirResults,
	    String basedirReference) {

	String pathToRelevantConfig;
	try {
	    pathToRelevantConfig = scxmlConverter.updateSCXMLTestResources(configPath);
	} catch (Exception e) {
	    e.printStackTrace();
	    fail("Exception occurred while updating SCXML test resources.", e);
	    return;
	}

	super.multiTest(pathToRelevantConfig, fileFormatsToCheck, basedirResults, basedirReference);
    }

    @Override
    protected void executeAndError(String config, String detailsOnExpectedError) {

	String pathToRelevantConfig;
	try {
	    pathToRelevantConfig = scxmlConverter.updateSCXMLTestResources(config);
	} catch (Exception e) {
	    e.printStackTrace();
	    fail("Exception occurred while updating SCXML test resources.", e);
	    return;
	}

	super.executeAndError(pathToRelevantConfig, detailsOnExpectedError);
    }

    @Override
    protected void jsonTest(String config, String[] jsonFileNamesWithoutExtension, String basedirResults,
	    String basedirReference) {

	String pathToRelevantConfig;
	try {
	    pathToRelevantConfig = scxmlConverter.updateSCXMLTestResources(config);
	} catch (Exception e) {
	    e.printStackTrace();
	    fail("Exception occurred while updating SCXML test resources.", e);
	    return;
	}

	super.jsonTest(pathToRelevantConfig, jsonFileNamesWithoutExtension, basedirResults, basedirReference);
    }

    @Override
    protected void docxTest(String config, String[] docxFileNamesWithoutExtension, String basedirResults,
	    String basedirReference) {

	String pathToRelevantConfig;
	try {
	    pathToRelevantConfig = scxmlConverter.updateSCXMLTestResources(config);
	} catch (Exception e) {
	    e.printStackTrace();
	    fail("Exception occurred while updating SCXML test resources.", e);
	    return;
	}

	super.docxTest(pathToRelevantConfig, docxFileNamesWithoutExtension, basedirResults, basedirReference);
    }

    @Override
    protected void htmlTest(String config, String[] htmlFileNamesWithoutExtension, String basedirResults,
	    String basedirReference) {

	String pathToRelevantConfig;
	try {
	    pathToRelevantConfig = scxmlConverter.updateSCXMLTestResources(config);
	} catch (Exception e) {
	    e.printStackTrace();
	    fail("Exception occurred while updating SCXML test resources.", e);
	    return;
	}

	super.htmlTest(pathToRelevantConfig, htmlFileNamesWithoutExtension, basedirResults, basedirReference);
    }

    @Override
    protected void yamlTest(String config, String[] yamlFileNamesWithoutExtension, String basedirResults,
	    String basedirReference) {

	String pathToRelevantConfig;
	try {
	    pathToRelevantConfig = scxmlConverter.updateSCXMLTestResources(config);
	} catch (Exception e) {
	    e.printStackTrace();
	    fail("Exception occurred while updating SCXML test resources.", e);
	    return;
	}

	super.yamlTest(pathToRelevantConfig, yamlFileNamesWithoutExtension, basedirResults, basedirReference);
    }

    @Override
    protected void rdfTest(String config, String[] rdfFileNamesWithoutExtension, String basedirResults,
	    String basedirReference) {

	String pathToRelevantConfig;
	try {
	    pathToRelevantConfig = scxmlConverter.updateSCXMLTestResources(config);
	} catch (Exception e) {
	    fail("Exception occurred while updating SCXML test resources.", e);
	    e.printStackTrace();
	    return;
	}

	super.rdfTest(pathToRelevantConfig, rdfFileNamesWithoutExtension, basedirResults, basedirReference);
    }

    protected void sqlTest(String config, String[] sqlFileNamesWithoutExtension, HashMap<String, String> replacevalues,
	    String basedirResults, String basedirReference, boolean noErrors) {

	String pathToRelevantConfig;

	try {
	    pathToRelevantConfig = scxmlConverter.updateSCXMLTestResources(config);
	} catch (Exception e) {
	    e.printStackTrace();
	    fail("Exception occurred while updating SCXML test resources.", e);
	    return;
	}

	super.sqlTest(pathToRelevantConfig, sqlFileNamesWithoutExtension, replacevalues, basedirResults,
		basedirReference, noErrors);
    }

    @Override
    protected void xsdTest(String config, String[] xsdFileNamesWithoutExtension, String[] schFileNamesWithoutExtension,
	    HashMap<String, String> replacevalues, String basedirResults, String basedirReference, boolean noErrors) {

	String pathToRelevantConfig;
	if (config == null) {
	    // so the default config shall be executed
	    pathToRelevantConfig = null;
	} else {
	    try {
		pathToRelevantConfig = scxmlConverter.updateSCXMLTestResources(config);
	    } catch (Exception e) {
		e.printStackTrace();
		fail("Exception occurred while updating SCXML test resources.", e);
		return;
	    }
	}

	super.xsdTest(pathToRelevantConfig, xsdFileNamesWithoutExtension, schFileNamesWithoutExtension, replacevalues,
		basedirResults, basedirReference, noErrors);
    }
}
