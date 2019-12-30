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
package de.interactive_instruments.ShapeChange.Target;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.xml.serializer.OutputPropertiesFactory;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.TargetConfiguration;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Util.XsltWriter;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
public class TargetOutputProcessor implements MessageSource {

	/**
	 * If set to "true", an XSL transformation will be applied to output files
	 * created by the target, with one of the following file extensions: xml,
	 * xsd, rdf, owl, sch, trix, html, sql, ddl, ttl, nt, trig, nq. Default is
	 * "false".
	 */
	public static final String PARAM_APPLY_XSLT = "processOutput_applyXslt";
	/**
	 * Define the suffix to append to base names of files (i.e. the file name
	 * without file extension) created by applying an XSL transformation to
	 * output files. Default is {@value #DEFAULT_XSLT_OUTPUT_SUFFIX}. The suffix
	 * is required to identify the intended type of output files. For example,
	 * if an XML Schema file is transformed, the result could be an XML Schema
	 * but also an XML file. If the suffix is set to the same file extension as
	 * the input file of the transformation (e.g. ".xsd" when processing an XML
	 * Schema file), the input file will be overwritten.
	 */
	public static final String PARAM_XSLT_OUTPUT_SUFFIX = "processOutput_xslt_outputSuffix";
	public static final String DEFAULT_XSLT_OUTPUT_SUFFIX = "_transformed.xml";
	/**
	 * If set to "true", the input file of an XSL transformation (that is
	 * applied to output files created by a target) will be deleted - unless the
	 * result file of the transformation has overwritten the input file. Default
	 * is "false".
	 */
	public static final String PARAM_XSLT_DELETE_INPUT_AFTER_TRANSFORM = "processOutput_xslt_deleteInputFilesAfterTransformation";
	/**
	 * Path to the directory that contains the XSL transformation file(s).
	 * Default is "." (i.e. the current run directory). Can be an HTTP URL.
	 */
	public static final String PARAM_PATH_TO_XSLT_DIRECTORY = "processOutput_pathToXsltDirectory";
	/**
	 * Name of the XSL transformation file to apply. The file must be contained
	 * in the directory identified by parameter
	 * {@value #PARAM_PATH_TO_XSLT_DIRECTORY}.
	 */
	public static final String PARAM_XSLT_FILENAME = "processOutput_xsltFileName";
	/**
	 * Identifies the XSLT processor implementation, to be used for processing
	 * output files (see parameter {@value #PARAM_APPLY_XSLT}). The parameter is
	 * optional.
	 * 
	 * In order to process XSLT transformations with version 2.0 or higher, this
	 * parameter should point to the implementation of an XSLT processor that is
	 * capable of processing such XSLTs, for example Saxon-HE (home edition;
	 * open source):
	 * <ul>
	 * <li>net.sf.saxon.TransformerFactoryImpl (download the Saxon-HE jar and
	 * copy it to the lib folder of your ShapeChange distribution; note: name
	 * the file ‘Saxon-HE-9.5.1-1.jar’ - or explicitly include the jar in your
	 * classpath, using the '-cp' command when invoking java to execute
	 * ShapeChange)</li>
	 * </ul>
	 */
	public static final String PARAM_XSL_TRANSFORMER_FACTORY = "xslTransformerFactory";

	/**
	 * If set to "true", a comment will be added to output files (with one of
	 * the following file extensions: xml, xsd, rdf, owl, sch, trix, sql, ddl,
	 * ttl, nt, trig, nq). The comment is given via parameter
	 * {@value #PARAM_COMMENT}.
	 */
	public static final String PARAM_ADD_COMMENT = "processOutput_addComment";
	/**
	 * Comment to add to the content of output files (see parameter
	 * {@value #PARAM_ADD_COMMENT}). If no value is provided for this
	 * parameter, the default value applies (i.e., the comment will be
	 * auto-generated). The default value is generated as follows: last modified
	 * date of the output file, formatted according to yyyyMMdd, followed by the
	 * schema version if appropriate (target is a) either not a SingleTarget, or
	 * a SingleTarget and only a single schema is processed by ShapeChange, and
	 * b) the target is not a DeferrableOutputWriter (like the FeatureCatalogue
	 * target), and a link to shapechange.net. Example: <i>Created by
	 * ShapeChange on 20170818 from application schema version 1.1 -
	 * http://shapechange.net/</i>.
	 */
	public static final String PARAM_COMMENT = "processOutput_comment";

	/**
	 * If set to "true", the name of output files will be augmented with a
	 * prefix. The prefix is given via parameter {@value #PARAM_FILENAME_PREFIX}
	 * .
	 */
	public static final String PARAM_MODIFY_FILE_NAME = "processOutput_modifyFileName";
	/**
	 * Prefix to add to the name of output files (see parameter
	 * {@value #PARAM_MODIFY_FILE_NAME}). If no value is provided for this
	 * parameter, the default value applies (i.e., the prefix will be
	 * auto-generated). The default value is generated as follows: last modified
	 * date of the output file, formatted according to yyyyMMdd, followed by the
	 * schema version if appropriate (target is a) either not a SingleTarget, or
	 * a SingleTarget and only a single schema is processed by ShapeChange, and
	 * b) the target is not a DeferrableOutputWriter (like the FeatureCatalogue
	 * target), and a link to shapechange.net. Example:
	 * <i>20170818.v1.1.s1.xsd</i>.
	 */
	public static final String PARAM_FILENAME_PREFIX = "processOutput_fileNamePrefix";

	ShapeChangeResult result;

	public TargetOutputProcessor(ShapeChangeResult result) {
		this.result = result;
	}

	/**
	 * @param outputFiles
	 *            list of output files created by a target; typically does not
	 *            include temporary files that may have been created upon
	 *            writeAll by targets that are deferrable output writers (like
	 *            the tmp.xml from the feature catalogue target)
	 * @param tgt
	 *            configuration of the target that created the output
	 * @param schema
	 *            schema from which the output has been derived; may be
	 *            <code>null</code> if the output has been derived from multiple
	 *            schemas, or if the target is a deferrable output writer
	 */
	public void process(List<File> outputFiles, TargetConfiguration tgt,
			PackageInfo schema) {

		boolean modifyFileName = tgt.parameterAsBoolean(PARAM_MODIFY_FILE_NAME,
				false);
		boolean addComment = tgt.parameterAsBoolean(PARAM_ADD_COMMENT, false);
		boolean applyXslt = tgt.parameterAsBoolean(PARAM_APPLY_XSLT, false);

		// check if processing is required
		if (!(applyXslt || addComment || modifyFileName)) {
			// no specific output processing requested
			return;
		} else if (outputFiles == null || outputFiles.isEmpty()) {
			// no output files
			result.addInfo(this, 105);
		}

		result.addInfo(this, 100);

		// TBD: add parameter from which date format is parsed?
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

		List<File> filesToProcess = outputFiles;

		if (modifyFileName) {

			result.addInfo(this, 102);

			List<File> modifiedFiles = new ArrayList<File>();

			for (File file : filesToProcess) {

				Date lastModified = new Date(file.lastModified());
				String defaultFileNamePrefix = dateFormat.format(lastModified);

				String fileNamePrefix = tgt.parameterAsString(
						PARAM_FILENAME_PREFIX, defaultFileNamePrefix, false,
						true);

				StringBuilder newFileName = new StringBuilder();

				newFileName.append(fileNamePrefix);
				newFileName.append(".");

				if (schema != null && schema.version() != null) {
					newFileName.append("v");
					newFileName.append(schema.version());
					newFileName.append(".");
				}

				newFileName.append(file.getName());

				Path from = file.toPath();
				Path to = from.getParent().resolve(newFileName.toString());

				try {

					Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
					modifiedFiles.add(to.toFile());

					// update the result entry of the log
					result.updateResult(from.toFile(), to.toFile());

				} catch (IOException e) {
					result.addError(this, 10, from.toAbsolutePath().toString(),
							to.toAbsolutePath().toString(), e.getMessage());
					modifiedFiles.add(file);
				}
			}

			filesToProcess = modifiedFiles;
		}

		if (addComment) {

			result.addInfo(this, 103);

			for (File file : filesToProcess) {

				Date lastModified = new Date(file.lastModified());

				String defaultComment = "Created by ShapeChange on ";
				defaultComment += dateFormat.format(lastModified);
				if (schema != null
						&& StringUtils.isNotBlank(schema.version())) {
					defaultComment += " from application schema version ";
					defaultComment += schema.version();
				}
				defaultComment += " - http://shapechange.net/";

				String comment = tgt.parameterAsString(PARAM_COMMENT,
						defaultComment, false, true);

				String fileExtension = FilenameUtils
						.getExtension(file.getName());

				if (fileExtension.matches("(?i)(xml|xsd|rdf|owl|sch|trix)")) {

					addCommentToXmlFile(file, comment);

				} else if (fileExtension.matches("(?i)(sql|ddl)")) {

					addCommentToTextFile(file, "-- " + comment);

				} else if (fileExtension.matches("(?i)(ttl|nt|trig|nq)")) {

					addCommentToTextFile(file, "# " + comment);
				}
			}
		}

		if (applyXslt) {

			result.addInfo(this, 104);

			for (File file : filesToProcess) {

				/*
				 * only apply the transformation to supported file types: xml
				 * and text based files
				 */
				String fileExtension = FilenameUtils
						.getExtension(file.getName());
				if (!fileExtension.matches(
						"(?i)(xml|xsd|rdf|owl|sch|trix|html|sql|ddl|ttl|nt|trig|nq)")) {
					continue;
				}

				/*
				 * NOTE: validation of the following two parameters is done by
				 * the BasicConfigurationValidator
				 */
				String pathToXsltDirectory = tgt.parameterAsString(
						PARAM_PATH_TO_XSLT_DIRECTORY, ".", false, true);
				String xsltFileName = tgt.parameterAsString(PARAM_XSLT_FILENAME,
						null, false, true);

				String xslTransformerFactory = tgt.parameterAsString(
						PARAM_XSL_TRANSFORMER_FACTORY, null, false, true);

				String outputSuffix = tgt.parameterAsString(
						PARAM_XSLT_OUTPUT_SUFFIX, DEFAULT_XSLT_OUTPUT_SUFFIX,
						false, true);

				boolean deleteInputAfterTransform = tgt.parameterAsBoolean(
						PARAM_XSLT_DELETE_INPUT_AFTER_TRANSFORM, false);

				File transformationTargetFile = new File(file.getParentFile(),
						FilenameUtils.getBaseName(file.getName())
								+ outputSuffix);

				try {

					URI xsltMainFileUri = null;

					if (pathToXsltDirectory.toLowerCase().startsWith("http")) {

						URL url = new URL(
								pathToXsltDirectory + "/" + xsltFileName);
						xsltMainFileUri = url.toURI();

					} else {

						File xsl = new File(
								pathToXsltDirectory + "/" + xsltFileName);
						if (xsl.exists()) {
							xsltMainFileUri = xsl.toURI();
						} else {
							result.addError(this, 18, xsl.getAbsolutePath());
						}
					}

					XsltWriter writer = new XsltWriter(xslTransformerFactory,
							null, null, result);

					writer.xsltWrite(file, xsltMainFileUri,
							transformationTargetFile);

					if (!file.toPath()
							.equals(transformationTargetFile.toPath())) {

						if (deleteInputAfterTransform) {

							FileUtils.deleteQuietly(file);

							// update the result entry of the log
							result.updateResult(file, transformationTargetFile);

						} else {

							/*
							 * Create a new result entry for the new transformed
							 * file
							 */
							result.copyResultAndUpdateFileReference(file,
									transformationTargetFile);
						}
					}

				} catch (Exception e) {
					String m = e.getMessage();
					if (m != null) {
						result.addError(m);
					}
				}
			}
		}

		result.addInfo(this, 101);
	}

	public void addCommentToTextFile(File txtFile, String comment) {

		File directory = txtFile.getParentFile();
		File tmpFile = new File(directory, txtFile.getName() + ".tmp");

		try (BufferedWriter out = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(tmpFile), "UTF8"));
				BufferedReader in = new BufferedReader(
						new FileReader(txtFile))) {

			out.write(comment);
			out.newLine();

			String line = null;
			while ((line = in.readLine()) != null) {
				out.write(line);
				out.newLine();
			}

		} catch (IOException e) {
			result.addError(this, 15, txtFile.getAbsolutePath(),
					e.getMessage());
		}

		// move tmpFile to txtFile
		if (tmpFile.exists()) {
			try {
				Files.move(tmpFile.toPath(), txtFile.toPath(),
						StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				result.addError(this, 10, tmpFile.getAbsolutePath(),
						txtFile.getAbsolutePath(), e.getMessage());
			}
		}
	}

	public void addCommentToXmlFile(File xmlFile, String comment) {

		File directory = xmlFile.getParentFile();
		File tmpFile = new File(directory, xmlFile.getName() + ".tmp");

		try {

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			dbFactory.setNamespaceAware(true);
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(xmlFile);

			Comment com = doc.createComment(comment);

			Element root = doc.getDocumentElement();
			root.insertBefore(com, root.getFirstChild());

			Properties outputFormat = OutputPropertiesFactory
					.getDefaultMethodProperties("xml");
			outputFormat.setProperty("indent", "yes");
			outputFormat.setProperty(
					"{http://xml.apache.org/xalan}indent-amount", "2");
			outputFormat.setProperty("encoding", "UTF-8");

			Transformer transformer = TransformerFactory.newInstance()
					.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(
					"{http://xml.apache.org/xslt}indent-amount", "2");

			Result output = new StreamResult(tmpFile);
			Source input = new DOMSource(doc);

			transformer.transform(input, output);

		} catch (ParserConfigurationException | SAXException | IOException
				| TransformerFactoryConfigurationError
				| TransformerException e) {
			result.addError(this, 20, xmlFile.getAbsolutePath(),
					e.getMessage());
		}

		// move tmpFile to xmlFile
		if (tmpFile.exists()) {

			try {
				Files.move(tmpFile.toPath(), xmlFile.toPath(),
						StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				result.addError(this, 10, tmpFile.getAbsolutePath(),
						xmlFile.getAbsolutePath(), e.getMessage());
			}
		}
	}

	@Override
	public String message(int mnr) {

		/*
		 * NOTE: A leading ?? in a message text suppresses multiple appearance
		 * of a message in the output.
		 */
		switch (mnr) {

		case 10:
			return "Exception occurred while moving file from '$1$' to '$2$'. Exception message is: '$3$'.";
		case 15:
			return "Exception occurred while writing comment to text file located at '$1$'. Exception message is: '$2$'.";
		case 18:
			return "XSLT stylesheet $1$ not found.";
		case 20:
			return "Exception occurred while writing comment to XML file located at '$1$'. Exception message is: '$2$'.";

		case 100:
			return "---------- Processing output: START ----------";
		case 101:
			return "---------- Processing output: COMPLETE ----------";
		case 102:
			return "--- Modifying file names ...";
		case 103:
			return "--- Adding comments ...";
		case 104:
			return "--- Applying XSL transformation ...";
		case 105:
			return "---------- Processing output: no output files produced by target ----------";

		default:
			return "(" + this.getClass().getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
