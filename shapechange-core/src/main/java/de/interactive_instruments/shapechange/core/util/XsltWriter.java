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
 * (c) 2002-2015 interactive instruments GmbH, Bonn, Germany
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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.shapechange.core.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.WWWFormCodec;

import de.interactive_instruments.shapechange.core.ShapeChangeResult;

/**
 * This class is used to support processing an XSL transformation in two ways:
 * <ol>
 * <li>within the same java process that created the transformation source file
 * (for example a temporary catalogue xml file) - usually the process in which
 * ShapeChange is running.</li>
 * <li>within a separate java process that only performs the XSL transformation
 * </li>
 * </ol>
 * 
 * If no ShapeChangeResult object is available, logging is performed as follows:
 * <ul>
 * <li>normal log messages are printed to System.out</li>
 * <li>error messages are printed to System.err</li>
 * </ul>
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
public class XsltWriter {

	/**
	 * Character set used for encoding and decoding values within key-value
	 * pairs.
	 */
	public static Charset ENCODING_CHARSET = Charset.forName("UTF-8");

	public static final String PARAM_xslTransformerFactory = "-xslTransformerFactory";
	public static final String PARAM_hrefMappings = "-hrefMappings";
	public static final String PARAM_transformationParameters = "-transformationParameters";
	public static final String PARAM_transformationSourcePath = "-transformationSourcePath";
	public static final String PARAM_xsltMainFileUri = "-xsltMainFileUri";
	public static final String PARAM_transformationTargetPath = "-transformationTargetPath";

	private String xslTransformerFactory;
	private Map<String, URI> hrefMappings;
	private Map<String, String> transformationParameters = new HashMap<String, String>();

	private ShapeChangeResult result;

	/**
	 * 
	 * 
	 * @param xslTransformerFactory
	 *            - can be <code>null</code>
	 * @param hrefMappings
	 *            - can be <code>null</code>
	 * @param transformationParameters
	 *            - can be <code>null</code>
	 * @param result
	 *            used to log exception messages; can be <code>null</code> if
	 *            the XSL transformation was invoked via the main(...) method
	 */
	public XsltWriter(String xslTransformerFactory,
			Map<String, URI> hrefMappings,
			Map<String, String> transformationParameters,
			ShapeChangeResult result) {

		this.xslTransformerFactory = xslTransformerFactory;
		this.hrefMappings = hrefMappings;
		if (transformationParameters != null) {
			this.transformationParameters = transformationParameters;
		}
		this.result = result;
	}

	/**
	 * Parameter identifiers have a leading "-". Parameter values are separated
	 * from the parameter identifier via a single space.
	 * <ul>
	 * <li>Parameter {@value #PARAM_xslTransformerFactory}: fully qualified name
	 * of the XSLT processor implementation; NOTE: this parameter may not be
	 * provided if the default implementation shall be used.</li>
	 * <li>Parameter {@value #PARAM_hrefMappings}: list of key-value pairs
	 * defining href mappings, structured using URL query syntax (i.e. using '='
	 * to separate the key from the value, using '&amp;' to separate pairs, and with
	 * URL-encoded value (with UTF-8 character encoding); NOTE: this parameter
	 * may not be provided if href mappings are not needed.</li>
	 * <li>Parameter {@value #PARAM_transformationParameters}: list of key-value
	 * pairs defining the transformation parameters, structured using URL query
	 * syntax (i.e. using '=' to separate the key from the value, using '&amp;' to
	 * separate pairs, and with URL-encoded value (with UTF-8 character
	 * encoding); NOTE: this parameter may not be provided if transformation
	 * parameters are not needed.</li>
	 * <li>Parameter {@value #PARAM_transformationSourcePath}: path to the
	 * transformation source file (may be a relative path); NOTE: this is a
	 * required parameter.</li>
	 * <li>Parameter {@value #PARAM_xsltMainFileUri}: String representation of
	 * the URI to the main XSLT file; NOTE: this is a required parameter.</li>
	 * <li>Parameter {@value #PARAM_transformationTargetPath}: path to the
	 * transformation target file (may be a relative path); NOTE: this is a
	 * required parameter.</li>
	 * </ul>
	 * @param args  tbd
	 */
	public static void main(String[] args) {

		String xslTransformerFactory = null;
		String hrefMappingsString = null;
		String transformationParametersString = null;
		String transformationSourcePath = null;
		String xsltMainFileUriString = null;
		String transformationTargetPath = null;

		// identify parameters
		String arg = null;

		for (int i = 0; i < args.length; i++) {

			arg = args[i];

			if (arg.equals(PARAM_xslTransformerFactory)) {

				if (i + 1 == args.length || args[i + 1].startsWith("-")) {
					System.err.println(
							"No value provided for invocation parameter "
									+ PARAM_xslTransformerFactory);
					return;
				} else {
					xslTransformerFactory = args[i + 1];
					i++;
				}

			} else if (arg.equals(PARAM_hrefMappings)) {

				if (i + 1 == args.length || args[i + 1].startsWith("-")) {
					System.err.println(
							"No value provided for invocation parameter "
									+ PARAM_hrefMappings);
					return;
				} else {
					hrefMappingsString = args[i + 1];
					i++;
				}

			} else if (arg.equals(PARAM_transformationParameters)) {

				if (i + 1 == args.length || args[i + 1].startsWith("-")) {
					System.err.println(
							"No value provided for invocation parameter "
									+ PARAM_transformationParameters);
					return;
				} else {
					transformationParametersString = args[i + 1];
					i++;
				}

			} else if (arg.equals(PARAM_transformationSourcePath)) {

				if (i + 1 == args.length || args[i + 1].startsWith("-")) {
					System.err.println(
							"No value provided for invocation parameter "
									+ PARAM_transformationSourcePath);
					return;
				} else {
					transformationSourcePath = args[i + 1];
					i++;
				}

			} else if (arg.equals(PARAM_transformationTargetPath)) {

				if (i + 1 == args.length || args[i + 1].startsWith("-")) {
					System.err.println(
							"No value provided for invocation parameter "
									+ PARAM_transformationTargetPath);
					return;
				} else {
					transformationTargetPath = args[i + 1];
					i++;
				}

			} else if (arg.equals(PARAM_xsltMainFileUri)) {

				if (i + 1 == args.length || args[i + 1].startsWith("-")) {
					System.err.println(
							"No value provided for invocation parameter "
									+ PARAM_xsltMainFileUri);
					return;
				} else {
					xsltMainFileUriString = args[i + 1];
					i++;
				}
			}
		}

		try {

			// parse parameter values
			Map<String, URI> hrefMappings = new HashMap<String, URI>();

			if (hrefMappingsString != null) {
				List<NameValuePair> hrefMappingsList = WWWFormCodec
						.parse(hrefMappingsString, ENCODING_CHARSET);
				for (NameValuePair nvp : hrefMappingsList) {

					hrefMappings.put(nvp.getName(), new URI(nvp.getValue()));
				}
			}

			Map<String, String> transformationParameters = new HashMap<String, String>();

			if (transformationParametersString != null) {
				List<NameValuePair> transParamList = WWWFormCodec.parse(
						transformationParametersString, ENCODING_CHARSET);
				for (NameValuePair nvp : transParamList) {
					transformationParameters.put(nvp.getName(), nvp.getValue());
				}
			}

			boolean invalidParameters = false;

			if (transformationSourcePath == null) {
				invalidParameters = true;
				System.err.println(
						"Path to transformation source file was not provided.");
			}
			if (xsltMainFileUriString == null) {
				invalidParameters = true;
				System.err.println("Path to main XSLT file was not provided.");
			}
			if (transformationTargetPath == null) {
				invalidParameters = true;
				System.err.println(
						"Path to transformation target file was not provided.");
			}

			if (!invalidParameters) {

				// set up and execute XSL transformation
				XsltWriter writer = new XsltWriter(xslTransformerFactory,
						hrefMappings, transformationParameters, null);

				File transformationSource = new File(transformationSourcePath);
				URI xsltMainFileUri = new URI(xsltMainFileUriString);
				File transformationTarget = new File(transformationTargetPath);

				writer.xsltWrite(transformationSource, xsltMainFileUri,
						transformationTarget);
			}

		} catch (Exception e) {

			String m = e.getMessage();

			if (m != null) {
				System.err.println(m);
			} else {
				System.err.println(
						"Exception occurred while processing the XSL transformation.");
			}

			e.printStackTrace(System.err);
		}
	}

	public void xsltWrite(File transformationSource, URI xsltMainFileUri,
			File transformationTarget) {

		try {

			// Set up input and output files

			InputStream stream = null;

			if (xsltMainFileUri.getScheme().startsWith("http")) {
				URL url = xsltMainFileUri.toURL();
				URLConnection urlConnection = url.openConnection();
				stream = urlConnection.getInputStream();
			} else {
				File xsl = new File(xsltMainFileUri);
				// FeatureCatalogue.java already checked that file exists
				stream = new FileInputStream(xsl);
			}

			// create an instance of TransformerFactory
			if (xslTransformerFactory != null) {
				// use TransformerFactory specified in configuration
				System.setProperty("javax.xml.transform.TransformerFactory",
						xslTransformerFactory);
			} else {
				// use TransformerFactory determined by system
			}
			TransformerFactory transFact = TransformerFactory.newInstance();

			Source xsltSource = new StreamSource(stream);
			xsltSource.setSystemId(xsltMainFileUri.toString());
			Source xmlSource = new StreamSource(transformationSource);

			/*
			 * Create StreamResult differently to avoid issues with whitespace
			 * in file path, depending upon the actual TransformerFactory
			 * implementation.
			 */
			Result res;
			if (transFact.getClass().getName().equalsIgnoreCase(
					"org.apache.xalan.processor.TransformerFactoryImpl")) {
				res = new StreamResult(transformationTarget.getPath());
			} else {
				res = new StreamResult(transformationTarget);
			}

			/*
			 * Set URI resolver for transformation, configured with standard
			 * mappings (e.g. for the localization files) and possibly other
			 * mappings.
			 */
			transFact.setURIResolver(new XsltUriResolver(hrefMappings));

			Transformer trans = transFact.newTransformer(xsltSource);

			/*
			 * Specify any standard transformation parameters (e.g. for
			 * localization).
			 */
			for (String key : transformationParameters.keySet()) {
				trans.setParameter(key, transformationParameters.get(key));
			}

			/* Execute the transformation. */
			trans.transform(xmlSource, res);

		} catch (Exception e) {

			String m = e.getMessage();
			if (m != null) {
				if (result != null) {
					result.addError(m);
				} else {
					System.err.println(m);
				}
			} else {
				String msg = "Exception occurred while processing the XSL transformation.";
				if (result != null) {
					result.addError(msg);
				} else {
					System.err.println(msg);
				}
			}
		}

	}
}
