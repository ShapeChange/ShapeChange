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
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Joiner;

import de.interactive_instruments.ShapeChange.Target.TargetOutputProcessor;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class BasicConfigurationValidator extends AbstractConfigurationValidator {

    /**
     * Validates the 'input'-section of the ShapeChange configuration. Any
     * invalidity is directly logged in the ShapeChangeResult.
     * 
     * @param pConfig will be ignored by this class, can therefore be
     *                <code>null</code>
     * @param options tbd
     * @param result  tbd
     * @return <code>true</code> if the configuration is valid, else
     *         <code>false</code>
     */
    @Override
    public boolean isValid(ProcessConfiguration pConfig, Options options, ShapeChangeResult result) {

	boolean isValid = true;

	/*
	 * ====== Check input parameters ======
	 */
	result.addInfo(this, 3, "input");
	isValid = validateParameters(
		options.getInputAndLogParameterRegistry().getAllowedInputParametersWithStaticNames(),
		options.getInputAndLogParameterRegistry().getRegexesForAllowedInputParametersWithDynamicNames(),
		options.inputConfig.getParameters().keySet(), result) && isValid;
	result.addInfo(this, 4, "input");

	/*
	 * ====== Check log parameters ======
	 */
	result.addInfo(this, 3, "log");
	isValid = validateParameters(options.getInputAndLogParameterRegistry().getAllowedLogParametersWithStaticNames(),
		options.getInputAndLogParameterRegistry().getRegexesForAllowedLogParametersWithDynamicNames(),
		options.logParameters.keySet(), result) && isValid;
	result.addInfo(this, 4, "log");

	
	String imt = options.parameter("inputModelType");

	if (imt == null) {
	    result.addProcessFlowError(null, 26);
	    isValid = false;
	}

	/*
	 * If the input type is EA7 and we are not only executing deferrable output
	 * writers, check that we are running on 32bit Java in a windows environment.
	 * 
	 * NOTE: Apparently, it is not trivial to detect if a java program is executed
	 * with 32bit or 64bit JRE. The web has many suggestions on how to do it. Here,
	 * we make the assumption that in order to execute Enterprise Architect (EA),
	 * the JRE must be run in Windows. Under that assumption, the java system
	 * property 'os.arch' apparently is always 'x86' if the program is run with a
	 * 32bit JRE.
	 */
	if (imt.equalsIgnoreCase("EA7") && !options.isOnlyDeferrableOutputWrite()) {

	    /*
	     * 2020-08-10 JE: Since v15, Enterprise Architect has a 64bit dll, so can be run
	     * with 64bit Java. Therefore, this check is no longer required.
	     */
//			boolean isWindows = SystemUtils.IS_OS_WINDOWS;
//			String osArch = SystemUtils.OS_ARCH;
//
//			if (!isWindows) {
//				result.addProcessFlowError(this, 1);
//				isValid = false;
//			} else if (!osArch.equalsIgnoreCase("x86")) {
//				result.addProcessFlowError(this, 2, osArch);
//				isValid = false;
//			}
	}

	/* === check output processing parameters === */
	List<TargetConfiguration> targetConfigs = options.getTargetConfigurations();

	for (TargetConfiguration conf : targetConfigs) {

	    boolean applyXslt = conf.parameterAsBoolean(TargetOutputProcessor.PARAM_APPLY_XSLT, false);

	    if (applyXslt) {

		Joiner joiner = Joiner.on(", ");

		String pathToXsltDirectory = conf.parameterAsString(TargetOutputProcessor.PARAM_PATH_TO_XSLT_DIRECTORY,
			".", false, true);
		String xsltFileName = conf.parameterAsString(TargetOutputProcessor.PARAM_XSLT_FILENAME, null, false,
			true);

		if (xsltFileName == null) {

		    result.addProcessFlowError(this, 102, conf.getClassName(), joiner.join(conf.getInputIds()));
		    isValid = false;

		} else if (pathToXsltDirectory.toLowerCase().startsWith("http")) {

		    String urlString = pathToXsltDirectory + "/" + xsltFileName;
		    try {
			URL url = new URL(urlString);
			url.toURI();
		    } catch (URISyntaxException | MalformedURLException e) {
			result.addProcessFlowError(this, 100, conf.getClassName(), joiner.join(conf.getInputIds()),
				urlString, e.getMessage());
			isValid = false;
		    }

		} else {

		    File xsl = new File(pathToXsltDirectory + "/" + xsltFileName);
		    if (!xsl.exists()) {
			result.addProcessFlowError(this, 101, conf.getClassName(), joiner.join(conf.getInputIds()),
				xsl.getAbsolutePath());
			isValid = false;
		    }
		}
	    }
	}

	/* === Validate descriptor sources === */
	SortedMap<String, String> descriptorSources = options.getInputConfig().getDescriptorSources();
	for (Entry<String, String> entry : descriptorSources.entrySet()) {

	    /*
	     * Value is either 'sc:extract#sometoken' or 'tag#sometag'
	     */
	    String[] components = StringUtils.splitPreserveAllTokens(entry.getValue(), "#");
	    if (components[0].equalsIgnoreCase("sc:extract") && components[1].length() == 0) {

		result.addProcessFlowError(this, 200, entry.getKey());
		isValid = false;

	    } else if (components[0].equalsIgnoreCase("tag") && components[1].length() == 0) {

		result.addProcessFlowError(this, 201, entry.getKey());
		isValid = false;
	    }
	}

	/*
	 * ====== Check XmlSchema target configuration ======
	 */
	for (TargetConfiguration conf : targetConfigs) {

	    if (conf instanceof TargetXmlSchemaConfiguration) {

		TargetXmlSchemaConfiguration xsdConf = (TargetXmlSchemaConfiguration) conf;

		List<XsdMapEntry> xsdMapEntries = xsdConf.getXsdMapEntries();

		for (XsdMapEntry xsdme : xsdMapEntries) {

		    if (xsdme.getXmlElementHasSimpleContent() != null && !xsdme.hasXmlElement()) {
			result.addProcessFlowError(this, 300, xsdme.getType(),
				String.join(" ", xsdme.getEncodingRules()));
			isValid = false;
		    }
		}

		for (XsdPropertyMapEntry xpme : xsdConf.getXsdPropertyMapEntries().values()) {

		    /*
		     * Ensure that the targetElement contains a QName like string.
		     */
		    if (xpme.hasTargetElement()) {

			String targetElement = xpme.getTargetElement();

			if (StringUtils.countMatches(targetElement, ":") != 1) {

			    result.addProcessFlowError(this, 301, xpme.toString());
			    isValid = false;

			} else {

			    String nsabr = targetElement.split(":")[0];
			    if (options.fullNamespace(nsabr) == null) {
				result.addProcessFlowError(this, 302, xpme.toString(), nsabr);
				isValid = false;
			    }
			}
		    }
		}
	    }
	}

	return isValid;
    }

    @Override
    public String message(int mnr) {

	switch (mnr) {
	case 0:
	    return "Context: class BasicConfigurationValidator";
	case 1:
	    return "The input parameter 'inputModelType' is set to 'EA7'. When loading an Enterprise Architect model, ShapeChange must be executed in Windows OS. ShapeChange detected that it is run in a different OS.";
	case 2:
	    return "The input parameter 'inputModelType' is set to 'EA7'. When loading an Enterprise Architect model, ShapeChange must be executed in Windows OS with a 32bit JRE. ShapeChange detected that it is not executed with a 32bit JRE. The value of system property 'os.arch' is: '$1$'.";
	case 3:
	    return "Validating $1$ parameters.";
	case 4:
	    return "Validation of $1$ parameters completed.";

	// 100-199: Validation of output processing parameters
	case 100:
	    return "XSL transformation of output files is requested via configuration parameter '"
		    + TargetOutputProcessor.PARAM_APPLY_XSLT
		    + "' for target with class name '$1$' and input(s) '$2$'. The URL that results from concatenating the path to the XSL directory and the XSLT file name - '$3$' - could not be converted to a URI. Exception message is: '$4$'.";
	case 101:
	    return "XSL transformation of output files is requested via configuration parameter '"
		    + TargetOutputProcessor.PARAM_APPLY_XSLT
		    + "' for target with class name '$1$' and input(s) '$2$'. No XSL file was found at location '$3$'.";
	case 102:
	    return "XSL transformation of output files is requested via configuration parameter '"
		    + TargetOutputProcessor.PARAM_APPLY_XSLT
		    + "' for target with class name '$1$' and input(s) '$2$'. Required parameter '"
		    + TargetOutputProcessor.PARAM_APPLY_XSLT
		    + "' was not configured (or does not contain a non-empty value).";

	// 200-299: Validation of descriptor sources
	case 200:
	    return "Source for descriptor '$1$' is 'sc:extract', but required token is not provided.";
	case 201:
	    return "Source for descriptor '$1$' is 'tag', but required tag is not provided.";

	// 300-399: Validation of XsdMapEntries and XsdPropertyMapEntries
	case 300:
	    return "XsdMapEntry with @type '$1$' and @xsdEncodingRule '$2$' is invalid because @xmlElementHasSimpleContent is set but @xmlElement has no value.";
	case 301:
	    return "XsdPropertyMapEntry '$1$' is invalid because the @targetElement is not a QName like string.";
	case 302:
	    return "XsdPropertyMapEntry '$1$' is invalid because the configuration does not contain a namespace definition with the namespace prefix '$2$', which is used by the @targetElement.";
	default:
	    return "(" + BasicConfigurationValidator.class.getName() + ") Unknown message with number: " + mnr;
	}
    }
}
