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

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.commons.lang.StringUtils;

import de.interactive_instruments.ShapeChange.ConfigurationValidator;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessConfiguration;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.TargetConfiguration;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class FeatureCatalogueConfigurationValidator implements ConfigurationValidator, MessageSource {

	@Override
	public boolean isValid(ProcessConfiguration pConfig, Options options, ShapeChangeResult result) {

		boolean isValid = true;

		/*
		 * NOTE: No type check for the configuration is performed, since a
		 * mismatch would be a system error
		 */
		TargetConfiguration tgtConfig = (TargetConfiguration) pConfig;

		String inputs = StringUtils.join(tgtConfig.getInputIds(), ", ");

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
			 * 'xslTransformerFactory' is set or Saxon is used as
			 * TransformerFactory implementation
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

		String pathToJavaExe_ = options.parameter(this.getClass().getName(), FeatureCatalogue.PARAM_JAVA_EXE_PATH);
		if (pathToJavaExe_ != null && pathToJavaExe_.trim().length() > 0) {
			String pathToJavaExe = pathToJavaExe_.trim();
			String javaOptions = null;
			if (!pathToJavaExe.startsWith("\"")) {
				pathToJavaExe = "\"" + pathToJavaExe;
			}
			if (!pathToJavaExe.endsWith("\"")) {
				pathToJavaExe = pathToJavaExe + "\"";
			}

			String jo_tmp = options.parameter(this.getClass().getName(), FeatureCatalogue.PARAM_JAVA_OPTIONS);
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

		default:
			return "(" + FeatureCatalogue.class.getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
