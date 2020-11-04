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
package de.interactive_instruments.ShapeChange.Target.FeatureCatalogue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.ShapeChange.AbstractConfigurationValidator;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessConfiguration;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.TargetConfiguration;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class FeatureCatalogueConfigurationValidator extends AbstractConfigurationValidator {

    protected SortedSet<String> allowedParametersWithStaticNames = new TreeSet<>(Stream.of(FeatureCatalogue.PARAM_CSS_PATH, FeatureCatalogue.PARAM_DELETE_XML_FILE, FeatureCatalogue.PARAM_DOCX_STYLE,
		FeatureCatalogue.PARAM_DOCX_TEMPLATE_FILE_PATH, FeatureCatalogue.PARAM_DONT_TRANSFORM, FeatureCatalogue.PARAM_FEATURE_TERM, FeatureCatalogue.PARAM_INCLUDE_ALIAS,
		FeatureCatalogue.PARAM_INCLUDE_CODELIST_URI, FeatureCatalogue.PARAM_INCLUDE_CODELISTS_AND_ENUMERATIONS, FeatureCatalogue.PARAM_INCLUDE_DIAGRAMS,
		FeatureCatalogue.PARAM_INCLUDE_TITLE, FeatureCatalogue.PARAM_INCLUDE_VOIDABLE, FeatureCatalogue.PARAM_INHERITED_CONSTRAINTS, FeatureCatalogue.PARAM_INHERITED_PROPERTIES,
		FeatureCatalogue.PARAM_JAVA_EXE_PATH, FeatureCatalogue.PARAM_JAVA_OPTIONS, FeatureCatalogue.PARAM_LANG, FeatureCatalogue.PARAM_LOCALIZATION_MESSAGES_URI,
		FeatureCatalogue.PARAM_LOGO_FILE_PATH, FeatureCatalogue.PARAM_NAME, FeatureCatalogue.PARAM_NO_ALPHABETIC_SORT_OF_PROPS, FeatureCatalogue.PARAM_OUTPUT_FORMAT, FeatureCatalogue.PARAM_PACKAGE,
		FeatureCatalogue.PARAM_PRODUCER, FeatureCatalogue.PARAM_REFERENCE_MODEL_FILENAME_OR_CONSTRING, FeatureCatalogue.PARAM_REFERENCE_MODEL_TYPE, FeatureCatalogue.PARAM_SCOPE,
		FeatureCatalogue.PARAM_VERSION_DATE, FeatureCatalogue.PARAM_VERSION_NUMBER, FeatureCatalogue.PARAM_XSL_DOCX_FILE, FeatureCatalogue.PARAM_XSL_FO_FILE,
		FeatureCatalogue.PARAM_XSL_FRAME_HTML_FILENAME, FeatureCatalogue.PARAM_XSL_HTML_FILE, FeatureCatalogue.PARAM_XSL_LOCALIZATION_URI, FeatureCatalogue.PARAM_XSL_RTF_FILE,
		FeatureCatalogue.PARAM_XSL_TRANSFORMER_FACTORY, FeatureCatalogue.PARAM_XSL_XML_FILE, FeatureCatalogue.PARAM_XSLT_PATH, FeatureCatalogue.PARAM_XSLT_PFAD)
		.collect(Collectors.toSet()));
    protected List<Pattern> regexForAllowedParametersWithDynamicNames = null;
    
    private TargetConfiguration tgtConfig = null;
    private Options options = null;
    private ShapeChangeResult result = null;
    private String inputs = null;

    @Override
    public boolean isValid(ProcessConfiguration pConfig, Options options, ShapeChangeResult result) {

	/*
	 * NOTE: No type check for the configuration is performed, since a mismatch
	 * would be a system error
	 */
	this.tgtConfig = (TargetConfiguration) pConfig;
	this.options = options;
	this.result = result;

	inputs = StringUtils.join(tgtConfig.getInputIds(), ", ");

	boolean isValid = true;
	
	allowedParametersWithStaticNames.addAll(getCommonTargetParameters());
	isValid = validateParameters(allowedParametersWithStaticNames, regexForAllowedParametersWithDynamicNames,
		pConfig.getParameters().keySet(), result) && isValid;

	// check parameter types
	isValid &= checkIsBooleanValueIfSet(FeatureCatalogue.PARAM_DONT_TRANSFORM);
	isValid &= checkIsBooleanValueIfSet("deleteXmlfile");
	isValid &= checkIsBooleanValueIfSet("includeAlias");
	isValid &= checkIsBooleanValueIfSet("includeCodelistURI");
	isValid &= checkIsBooleanValueIfSet("includeDiagrams");
	isValid &= checkIsBooleanValueIfSet("includeTitle");
	isValid &= checkIsBooleanValueIfSet("includeVoidable");
	isValid &= checkIsBooleanValueIfSet("inheritedConstraints");
	isValid &= checkIsBooleanValueIfSet("inheritedProperties");
	isValid &= checkIsBooleanValueIfSet("noAlphabeticSortingForProperties");

	/*
	 * Check that parameter docxStyle, if set, has known values
	 */
	String docxStyle = tgtConfig.getParameterValue(FeatureCatalogue.PARAM_DOCX_STYLE);
	if (docxStyle != null && !("default".equals(docxStyle) || "custom1".equals(docxStyle))) {

	    MessageContext mc = result.addError(this, 5, FeatureCatalogue.PARAM_DOCX_STYLE, docxStyle);
	    if (mc != null) {
		mc.addDetail(this, 0, inputs);
	    }

	    isValid = false;
	}

	/*
	 * Check that the configured XSL transformer factory is available
	 */
	String xslTransformerFactory = tgtConfig.getParameterValue(FeatureCatalogue.PARAM_XSL_TRANSFORMER_FACTORY);

	if (xslTransformerFactory != null) {

	    try {
		System.setProperty("javax.xml.transform.TransformerFactory", xslTransformerFactory);
		@SuppressWarnings("unused")
		TransformerFactory factory = TransformerFactory.newInstance();

	    } catch (TransformerFactoryConfigurationError e) {
		isValid = false;
		MessageContext mc = result.addError(this, 100, xslTransformerFactory);
		if (mc != null) {
		    mc.addDetail(this, 0, inputs);
		    mc.addDetail(this, 1, FeatureCatalogue.PARAM_XSL_TRANSFORMER_FACTORY);
		}

	    }
	}

	/*
	 * Check that parameter 'outputFormat' is set.
	 */
	String outputFormat = tgtConfig.getParameterValue(FeatureCatalogue.PARAM_OUTPUT_FORMAT);

	if (outputFormat == null) {

	    isValid = false;

	    MessageContext mc = result.addError(this, 101);
	    if (mc != null) {
		mc.addDetail(this, 0, inputs);
		mc.addDetail(this, 1, FeatureCatalogue.PARAM_OUTPUT_FORMAT);
	    }

	} else {

	    /*
	     * Check that if format is DOCX or FRAMEHTML, then parameter
	     * 'xslTransformerFactory' is set or Saxon is used as TransformerFactory
	     * implementation
	     */
	    if ((outputFormat.toLowerCase().contains("docx") || outputFormat.toLowerCase().contains("framehtml"))
		    && xslTransformerFactory == null) {

		try {
		    TransformerFactory factory = TransformerFactory.newInstance();

		    if (factory.getClass().getName().equalsIgnoreCase("net.sf.saxon.TransformerFactoryImpl")) {
			// fine - this is an XSLT 2.0 processor
		    } else {
			isValid = false;

			MessageContext mc = result.addError(this, 102);
			if (mc != null) {
			    mc.addDetail(this, 0, inputs);
			    mc.addDetail(this, 1, FeatureCatalogue.PARAM_XSL_TRANSFORMER_FACTORY);
			}
		    }

		} catch (TransformerFactoryConfigurationError e) {
		    isValid = false;
		    MessageContext mc = result.addError(this, 100, xslTransformerFactory);
		    if (mc != null) {
			mc.addDetail(this, 0, inputs);
			mc.addDetail(this, 1, FeatureCatalogue.PARAM_XSL_TRANSFORMER_FACTORY);
		    }
		}
	    }
	}

	String pathToJavaExe_ = pConfig.getParameterValue(FeatureCatalogue.PARAM_JAVA_EXE_PATH);
	if (pathToJavaExe_ != null && pathToJavaExe_.trim().length() > 0) {
	    String pathToJavaExe = pathToJavaExe_.trim();
	    String javaOptions = null;
	    if (!pathToJavaExe.startsWith("\"")) {
		pathToJavaExe = "\"" + pathToJavaExe;
	    }
	    if (!pathToJavaExe.endsWith("\"")) {
		pathToJavaExe = pathToJavaExe + "\"";
	    }

	    String jo_tmp = pConfig.getParameterValue(FeatureCatalogue.PARAM_JAVA_OPTIONS);
	    if (jo_tmp != null && jo_tmp.trim().length() > 0) {
		javaOptions = jo_tmp.trim();
	    }

	    /*
	     * check path - and potentially also options - by invoking the exe
	     */
	    List<String> cmds = new ArrayList<String>();
	    cmds.add(pathToJavaExe);
	    if (javaOptions != null) {
		cmds.add(javaOptions);
	    }
	    cmds.add("-version");

	    ProcessBuilder pb = new ProcessBuilder(cmds);

	    try {
		Process proc = pb.start();

		StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream());
		StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream());

		errorGobbler.start();
		outputGobbler.start();

		errorGobbler.join();
		outputGobbler.join();

		int exitVal = proc.waitFor();

		if (exitVal != 0) {
		    if (errorGobbler.hasResult()) {
			MessageContext mc = result.addFatalError(this, 102, StringUtils.join(cmds, " "), "" + exitVal);
			mc.addDetail(this, 4, errorGobbler.getResult());
		    } else {
			result.addFatalError(this, 102, StringUtils.join(cmds, " "), "" + exitVal);
		    }
		    isValid = false;
		}

	    } catch (InterruptedException e) {
		MessageContext mc = result.addFatalError(this, 104);
		if (mc != null) {
		    mc.addDetail(this, 2, pathToJavaExe);
		    mc.addDetail(this, 3, javaOptions != null ? javaOptions : "<none>");
		}
		isValid = false;
	    } catch (IOException e) {
		MessageContext mc = result.addFatalError(this, 105);
		if (mc != null) {
		    mc.addDetail(this, 2, pathToJavaExe);
		    mc.addDetail(this, 3, javaOptions != null ? javaOptions : "<none>");
		}
		isValid = false;
	    }
	}

	return isValid;
    }

    /**
     * Checks if the configuration of the target has a parameter with given name. If
     * it is, and if its value is neither 'true' nor 'false' (ignoring case), then
     * the check will fail and an error will be logged. Otherwise the check will
     * succeed.
     * 
     * @param parameterName
     * @return <code>true</code> if either the parameter is not set in the
     *         configuration of the target, or if its value is 'true' or 'false'
     *         (ignoring case). Otherwise, <code>false</code> is returned.
     */
    private boolean checkIsBooleanValueIfSet(String parameterName) {

	String paramValue = tgtConfig.getParameterValue(parameterName);

	if (paramValue == null || paramValue.equalsIgnoreCase("true") || paramValue.equalsIgnoreCase("false")) {

	    return true;

	} else {

	    MessageContext mc = result.addError(this, 106, parameterName, paramValue);
	    if (mc != null) {
		mc.addDetail(this, 0, inputs);
	    }

	    return false;
	}
    }

    @Override
    public String message(int mnr) {

	switch (mnr) {
	case 0:
	    return "Context: FeatureCatalogue target configuration element with 'inputs'='$1$'.";
	case 1:
	    return "For further details, see the documentation of parameter '$1$' on http://shapechange.net/targets/feature-catalogue/";
	case 2:
	    return FeatureCatalogue.PARAM_JAVA_EXE_PATH + " is: $1$";
	case 3:
	    return FeatureCatalogue.PARAM_JAVA_OPTIONS + " is: $1$";
	case 4:
	    return "Message from external java executable: $1$";
	case 5:
	    return "Value of parameter '$1$' is invalid. Found: $2$";

	case 100:
	    return "Parameter '" + FeatureCatalogue.PARAM_XSL_TRANSFORMER_FACTORY
		    + "' is set to '$1$'. A Transformer with this factory could not be instantiated. Make the implementation of the transformer factory available on the classpath.";
	case 101:
	    return "The required parameter '" + FeatureCatalogue.PARAM_OUTPUT_FORMAT
		    + "' was not found in the configuration.";
	case 102:
	    return "Parameter '" + FeatureCatalogue.PARAM_OUTPUT_FORMAT
		    + "' contains 'DOCX' and/or 'FRAMEHTML'. These formats require an XSLT 2.0 processor, which should be set via the configuration parameter '"
		    + FeatureCatalogue.PARAM_XSL_TRANSFORMER_FACTORY
		    + "'. That parameter was not found, and the default TransformerFactory implementation is not 'net.sf.saxon.TransformerFactoryImpl' (which is known to be an XSLT 2.0 processor); ensure that the parameter is configured correctly.";
	case 103:
	    return "Invalid command for invocation of external java executable. Return code was: $2$. Command was: $1$";
	case 104:
	    return "InterruptionException while testing alternative java executable to perform the XSL transformation. Message is: $1$";
	case 105:
	    return "IOException while testing alternative java executable to perform the XSL transformation. Message is: $1$";
	case 106:
	    return "Value of parameter '$1$' is not a recognized boolean value. The value must either be equal to (ignoring case) 'true' or to 'false'. Given value is: $2$.";

	default:
	    return "(" + FeatureCatalogue.class.getName() + ") Unknown message with number: " + mnr;
	}
    }
}
