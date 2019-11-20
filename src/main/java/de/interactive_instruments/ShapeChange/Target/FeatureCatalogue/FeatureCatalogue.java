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
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */

package de.interactive_instruments.ShapeChange.Target.FeatureCatalogue;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.SerializerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.Fop.FopErrorListener;
import de.interactive_instruments.ShapeChange.Fop.FopMsgHandler;
import de.interactive_instruments.ShapeChange.Model.AssociationInfo;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.ImageMetadata;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Model.TaggedValues;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel;
import de.interactive_instruments.ShapeChange.ModelDiff.DiffElement;
import de.interactive_instruments.ShapeChange.ModelDiff.DiffElement.ElementType;
import de.interactive_instruments.ShapeChange.ModelDiff.DiffElement.Operation;
import de.interactive_instruments.ShapeChange.ModelDiff.Differ;
import de.interactive_instruments.ShapeChange.Target.DeferrableOutputWriter;
import de.interactive_instruments.ShapeChange.Target.SingleTarget;
import de.interactive_instruments.ShapeChange.Transformation.TransformationConstants;
import de.interactive_instruments.ShapeChange.UI.StatusBoard;
import de.interactive_instruments.ShapeChange.Util.XMLWriter;
import de.interactive_instruments.ShapeChange.Util.XsltWriter;
import de.interactive_instruments.ShapeChange.Util.ZipHandler;
import name.fraser.neil.plaintext.diff_match_patch;

/**
 * @author Clemens Portele (portele <at> interactive-instruments <dot> de)
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments <dot>
 *         de)
 * 
 */
public class FeatureCatalogue
		implements SingleTarget, MessageSource, DeferrableOutputWriter {

	public static final int STATUS_WRITE_PDF = 22;
	public static final int STATUS_WRITE_HTML = 23;
	public static final int STATUS_WRITE_XML = 24;
	public static final int STATUS_WRITE_RTF = 25;
	public static final int STATUS_WRITE_FRAMEHTML = 26;
	public static final int STATUS_WRITE_DOCX = 27;

	/**
	 * Default URI defining the location of the localizationMessages.xml file in
	 * XSLT(s). This can be overridden via the configuration parameter
	 * 'localizationMessagesUri'.
	 */
	public static final String localizationMessagesDefaultUri = "localizationMessages.xml";
	/**
	 * Default URI defining the location of the localization.xsl file. This can
	 * be overridden via the configuration parameter 'xslLocalizationUri'.
	 */
	public static final String localizationXslDefaultUri = "localization.xsl";

	/**
	 * The string used as placeholder in the docx template. The paragraph this
	 * placeholder text belongs to will be replaced with the feature catalogue.
	 */
	public static final String DOCX_PLACEHOLDER = "ShapeChangeFeatureCatalogue";
	public static final String DOCX_TEMPLATE_URL = "http://shapechange.net/resources/templates/template.docx";

	/**
	 * Can be used to only perform the deferrable output write if necessary.
	 */
	public static final String PARAM_DONT_TRANSFORM = "dontTransform";

	/**
	 * If set to <code>false</code>, the URI of code lists (available via tagged
	 * value 'codeList' or 'vocabulary') won't be encoded as hyperlink on the
	 * name of a code list type in the feature catalogue. This can be useful for
	 * example when the overall linking to external code lists is not ready for
	 * publication yet.
	 */
	public static final String PARAM_INCLUDE_CODELIST_URI = "includeCodelistURI";

	public static final String PARAM_XSL_TRANSFORMER_FACTORY = "xslTransformerFactory";

	public static final String PARAM_OUTPUT_FORMAT = "outputFormat";

	public static final String PARAM_DOCX_STYLE = "docxStyle";

	/**
	 * Path to a java executable (usually 64bit). This parameter should be used
	 * whenever the feature catalogue to produce will be very large (hundreds of
	 * megabytes to gigabytes). Set the options for execution - especially 'Xmx'
	 * - via the parameter {@value #PARAM_JAVA_OPTIONS}.
	 */
	public static final String PARAM_JAVA_EXE_PATH = "pathToJavaExecutable";
	/**
	 * Can be used to set options - especially 'Xmx' - for the invocation of the
	 * java executable identified via the parameter
	 * {@value #PARAM_JAVA_EXE_PATH}.
	 * 
	 * NOTE: when processing documents of 100Mbytes or more, it is recommended
	 * to allocate - via the Xmx parameter - at least 5 times the size of the
	 * source document.
	 */
	public static final String PARAM_JAVA_OPTIONS = "javaOptions";
	
	/**
	 * Suffix to add to the 'id' attribute of a Value element that represents an enum, and 
	 * to the 'idref' attribute of an enumeratedBy element that refers to that Value. Necessary
	 * in order to avoid same 'id' attributes for enums that are encoded both as FeatureAttribute
	 * (if target parameter {@link #includeCodelistsAndEnumerations} is true) and as Value elements.  
	 */
	private static final String VALUE_ID_SUFFIX = "_VALUE";

	private static boolean initialised = false;
	private static XMLWriter writer = null;
	private static String Package = "";
	private static TreeSet<ClassInfo> additionalClasses = new TreeSet<ClassInfo>();
	private static TreeSet<ClassInfo> enumerations = new TreeSet<ClassInfo>();

	/*
	 * NOTE: refModel, refPackage, diffs and differ are relevant for processing
	 * classes and during write all. They are not needed when the output is
	 * actually written during writeOutput(). Therefore, they are not
	 * initialized when the converter executes deferrable output writers.
	 * 
	 */
	private static GenericModel refModel = null;
	private static PackageInfo refPackage = null;
	private static SortedMap<Info, SortedSet<DiffElement>> diffs = new TreeMap<Info, SortedSet<DiffElement>>();
	private static Differ differ = null;

	/**
	 * key: (lowercase!) full name (in schema) of the class contained as value
	 * 
	 * value: a class from the input schema
	 */
	private static Map<String, ClassInfo> inputSchemaClassesByFullNameInSchema = null;

	private static boolean inheritedConstraints = true;
	private static boolean inheritedProperties = false;
	private static TreeSet<PropertyInfo> exportedRoles = new TreeSet<PropertyInfo>();
	private static TreeSet<PropertyInfo> exportedProperties = new TreeSet<PropertyInfo>();
	private static String OutputFormat = "";
	private static String outputDirectory = null;
	private static String outputFilename = null;
	private static String docxTemplateFilePath = DOCX_TEMPLATE_URL;
	private static String docxStyle = "default";
	private static String logoFilePath = null;
	private static boolean error = false;
	private static boolean printed = false;
	private static String encoding = null;
	private static String xslfofileName = "pdf.xsl";
	private static String xslTransformerFactory = null;
	private static String xslhtmlfileName = "html.xsl";
	private static final String DEFAULT_XSL_HTML_DIFF_FILE_NAME = "html_diff.xsl";
	private static String xslframeHtmlFileName = "frameHtml.xsl";
	private static String cssFileName = "stylesheet.css";
	private static String xslrtffileName = "rtf.xsl";
	private static String xsldocxfileName = "docx.xsl";
	private static String xsldocxrelsfileName = "docx_rels.xsl";
	private static String xsldocxContentTypesFileName = "docx_contentTypes.xsl";
	private static String xslxmlfileName = "xml.xsl";
	private static String xsltPath = "http://shapechange.net/resources/xslt";
	// private static String xsltPath = "src/main/resources/xslt";
	private static String cssPath = xsltPath;
	private static String lang = "en";
	private static String featureTerm = "Feature";
	private static String noAlphabeticSortingForProperties = "false";
	private static String includeCodelistsAndEnumerations = "false";
	private static boolean includeVoidable = true;
	private static boolean includeTitle = true;
	private static boolean includeCodelistURI = true;
	private static boolean deleteXmlFile = false;
	private static String representTaggedValues = null;

	private static boolean includeDiagrams = false;
	private static int imgIntegerIdCounter = 0;
	private static int imgIntegerIdStepwidth = 2;
	private static List<ImageMetadata> imageList = new ArrayList<ImageMetadata>();

	private static boolean dontTransform = false;

	private static String pathToJavaExe = null;
	private static String javaOptions = null;

	/**
	 * This map is used to keep track of the names of the application schema
	 * that are encountered during processing. Whenever this FeatureCatalogue is
	 * initialized with a new application schema package, the name of that
	 * schema is added to the map as a key - with a 1 integer as value in case
	 * that key was not present before, otherwise increasing the existing value
	 * by one (and in that case altering the name of the application schema
	 * during print accordingly [adding "(integer_value)"]). This is used to
	 * ensure that application schema with the same name are disambiguated
	 * during print.
	 */
	private static Map<String, Integer> encounteredAppSchemasByName = null;

	private TreeMap<String, String> transformationParameters = new TreeMap<String, String>();

	private PackageInfo pi = null;
	private Model model = null;
	private Options options = null;
	private ShapeChangeResult result = null;

	// set buffer size for streams (in bytes)
	private int streamBufferSize = 8 * 1042;

	/**
	 * Contains mappings for href values specified in XSLT scripts.
	 * 
	 * This information is used by the XsltUriResolver.
	 * 
	 * Key: href value (as used in the XSLTs, in import, include or as value of
	 * the document() function) Value: absolute URI to the actual file location.
	 * 
	 */
	private TreeMap<String, URI> hrefMappings = new TreeMap<String, URI>();

	@Override
	public String getTargetName() {
		return "Feature Catalogue";
	}

	public void reset() {
		initialised = false;
		writer = null;
		Package = "";
		additionalClasses.clear();
		enumerations.clear();
		inheritedConstraints = true;
		inheritedProperties = false;
		exportedRoles.clear();
		exportedProperties.clear();
		OutputFormat = "";
		outputDirectory = null;
		outputFilename = null;
		docxTemplateFilePath = DOCX_TEMPLATE_URL;
		docxStyle = "default";
		logoFilePath = null;
		error = false;
		printed = false;
		encoding = null;
		xslfofileName = "pdf.xsl";
		xslhtmlfileName = "html.xsl";
		xslframeHtmlFileName = "frameHtml.xsl";
		xslrtffileName = "rtf.xsl";
		xsldocxfileName = "docx.xsl";
		xsldocxrelsfileName = "docx_rels.xsl";
		xsldocxContentTypesFileName = "docx_contentTypes.xsl";
		xslxmlfileName = "xml.xsl";
		// xsltPath = "src/main/resources/xslt";
		xsltPath = "http://shapechange.net/resources/xslt";
		cssPath = xsltPath;
		xslTransformerFactory = null;
		lang = "en";
		noAlphabeticSortingForProperties = "false";
		includeCodelistsAndEnumerations = "false";
		hrefMappings = new TreeMap<String, URI>();
		featureTerm = "Feature";
		includeVoidable = true;
		includeTitle = true;
		includeCodelistURI = true;
		deleteXmlFile = false;
		dontTransform = false;
		representTaggedValues = null;

		refModel = null;
		refPackage = null;
		diffs = new TreeMap<Info, SortedSet<DiffElement>>();
		differ = null;
		inputSchemaClassesByFullNameInSchema = null;
		
		includeDiagrams = false;
		imgIntegerIdCounter = 0;
		imgIntegerIdStepwidth = 2;
		imageList = new ArrayList<ImageMetadata>();
	}

	// FIXME New diagnostics-only flag is to be considered
	public void initialise(PackageInfo p, Model m, Options o,
			ShapeChangeResult r, boolean diagOnly)
			throws ShapeChangeAbortException {
		pi = p;
		model = m;
		options = o;
		result = r;

		try {

			if (!initialised) {
				initialised = true;

				encounteredAppSchemasByName = new TreeMap<String, Integer>();

				initialiseFromOptions();

				String s = null;

				Model refModel_tmp = getReferenceModel();

				if (refModel_tmp != null) {

					/*
					 * Ensure that IDs used in the reference model are unique to
					 * that model and do not get mixed up with the IDs of the
					 * input model.
					 * 
					 * REQUIREMENT for model diff: two objects with equal ID
					 * must represent the same model element. If a model element
					 * is deleted in the reference model, then a new model
					 * element in the input model must not have the same ID.
					 * 
					 * It looks like this cannot be guaranteed. Therefore we add
					 * a prefix to the IDs of the model elements in the
					 * reference model.
					 */
					refModel = new GenericModel(refModel_tmp);
					refModel_tmp.shutdown();

					refModel.addPrefixToModelElementIDs("refmodel_");
				}

				String xmlName = outputFilename + ".tmp.xml";

				// Check whether we can use the given output directory
				File outputDirectoryFile = new File(outputDirectory);
				boolean exi = outputDirectoryFile.exists();
				if (!exi) {
					outputDirectoryFile.mkdirs();
					exi = outputDirectoryFile.exists();
				}
				boolean dir = outputDirectoryFile.isDirectory();
				boolean wrt = outputDirectoryFile.canWrite();
				boolean rea = outputDirectoryFile.canRead();
				if (!exi || !dir || !wrt || !rea) {
					result.addFatalError(this, 12, outputDirectory);
					throw new ShapeChangeAbortException();
				}

				String encoding_ = encoding == null ? "UTF-8"
						: model.characterEncoding();

				OutputStream fout = new FileOutputStream(
						outputDirectory + "/" + xmlName);
				OutputStream bout = new BufferedOutputStream(fout,
						streamBufferSize);
				OutputStreamWriter outputXML = new OutputStreamWriter(bout,
						encoding_);

				writer = new XMLWriter(outputXML, encoding_);

				writer.forceNSDecl("http://www.w3.org/2001/XMLSchema-instance",
						"xsi");

				writer.startDocument();

				writer.processingInstruction("xml-stylesheet",
						"type='text/xsl' href='./html.xsl'");

				writer.comment("Feature catalogue created using ShapeChange");

				AttributesImpl atts = new AttributesImpl();
				atts.addAttribute("http://www.w3.org/2001/XMLSchema-instance",
						"noNamespaceSchemaLocation",
						"xsi:noNamespaceSchemaLocation", "CDATA", "FC.xsd");
				writer.startElement("", "FeatureCatalogue", "", atts);

				s = options.parameter(this.getClass().getName(), "name");
				if (s != null && s.length() > 0)
					writer.dataElement("name", s);
				else
					writer.dataElement("name", "unknown");

				s = options.parameter(this.getClass().getName(), "scope");

				if (s != null && s.length() > 0)
					PrintLineByLine(s, "scope", null);
				else {
					writer.dataElement("scope", "unknown");
				}

				s = options.parameter(this.getClass().getName(),
						"versionNumber");
				if (s != null && s.length() > 0)
					writer.dataElement("versionNumber", s);
				else
					writer.dataElement("versionNumber", "unknown");

				s = options.parameter(this.getClass().getName(), "versionDate");
				if (StringUtils.isNotBlank(s)) {

					if (s.trim().equalsIgnoreCase("now")) {
						/* NOTE: cannot be unit tested */
						s = ZonedDateTime.now(ZoneOffset.systemDefault())
								.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
					}
					writer.dataElement("versionDate", s);
				} else {
					writer.dataElement("versionDate", "unknown");
				}

				s = options.parameter(this.getClass().getName(), "producer");
				if (s != null && s.length() > 0)
					writer.dataElement("producer", s);
				else
					writer.dataElement("producer", "unknown");
			}

			// we need to compute the diff for each application schema
			if (refModel != null) {

				SortedSet<PackageInfo> set = refModel.schemas(p.name());

				if (set.size() == 1) {

					/*
					 * Get the full names of classes (in lower case) from the
					 * input schema so that later we can look them up by their
					 * full name (within the schema, not in the model).
					 */
					inputSchemaClassesByFullNameInSchema = new HashMap<String, ClassInfo>();
					for (ClassInfo ci : model.classes(pi)) {
						inputSchemaClassesByFullNameInSchema.put(ci
								.fullNameInSchema().toLowerCase(Locale.ENGLISH),
								ci);
					}

					// compute diffs
					differ = new Differ();
					refPackage = set.iterator().next();
					SortedMap<Info, SortedSet<DiffElement>> pi_diffs = differ
							.diff(p, refPackage);

					// merge diffs for pi with existing diffs (from other
					// schemas)
					differ.merge(diffs, pi_diffs);

					// log the diffs found for pi
					for (Entry<Info, SortedSet<DiffElement>> me : pi_diffs
							.entrySet()) {

						MessageContext mc = result.addInfo(
								"Model difference - " + me.getKey().fullName()
										.replace(p.fullName(), p.name()));

						for (DiffElement diff : me.getValue()) {
							String s = diff.change + " " + diff.subElementType;
							if (diff.subElementType == ElementType.TAG)
								s += "(" + diff.tag + ")";
							if (diff.subElement != null)
								s += " " + diff.subElement.name();
							else if (diff.diff != null)
								s += " " + (new diff_match_patch())
										.diff_prettyHtml(diff.diff);
							else
								s += " ???";
							mc.addDetail(s);
						}
					}

					/*
					 * switch to default xslt for html diff - unless the
					 * configuration explicitly names an XSLT file to use
					 */
					if (options.parameter(this.getClass().getName(),
							"xslhtmlFile") == null) {
						xslhtmlfileName = DEFAULT_XSL_HTML_DIFF_FILE_NAME;
					}

				} else {
					result.addWarning(null, 308, p.name());
					refModel = null;
				}
			}

			writer.startElement("ApplicationSchema", "id", "_P" + pi.id());

			/*
			 * Determine if app schema with same name has been encountered
			 * before, and choose name accordingly
			 */

			String nameForAppSchema = null;

			if (encounteredAppSchemasByName.containsKey(pi.name())) {
				int count = encounteredAppSchemasByName.get(pi.name())
						.intValue();
				count++;
				nameForAppSchema = pi.name() + " (" + count + ")";
				encounteredAppSchemasByName.put(pi.name(),
						Integer.valueOf(count));
			} else {
				nameForAppSchema = pi.name();
				encounteredAppSchemasByName.put(pi.name(), Integer.valueOf(1));
			}

			// now set the name of the application schema
			writer.dataElement("name", nameForAppSchema);

			String s = pi.definition();
			if (s != null && s.length() > 0) {
				PrintLineByLine(s, "definition", null);
			}
			s = pi.description();
			if (s != null && s.length() > 0) {
				PrintLineByLine(s, "description", null);
			}

			s = pi.version();
			if (s != null && s.length() > 0) {
				writer.dataElement("versionNumber", s);
			}

			writer.startElement("taggedValues");

			s = pi.taggedValue(
					TransformationConstants.TRF_TV_NAME_GENERATIONDATETIME);
			if (s != null && s.trim().length() > 0) {
				writer.dataElement(
						TransformationConstants.TRF_TV_NAME_GENERATIONDATETIME,
						PrepareToPrint(s));
			}

			writer.endElement("taggedValues");

			if (pi.getDiagrams() != null) {
				appendImageInfo(pi.getDiagrams());
			}

			writer.endElement("ApplicationSchema");

			/*
			 * Check if there are any deletions of classes or packages that are
			 * owned by the application schema package.
			 */

			if (hasDiff(pi, ElementType.SUBPACKAGE, Operation.DELETE)) {

				Set<DiffElement> pkgdiffs = getDiffs(pi, ElementType.SUBPACKAGE,
						Operation.DELETE);

				for (DiffElement diff : pkgdiffs) {

					// child package was deleted
					PrintPackage((PackageInfo) diff.subElement,
							Operation.DELETE);
				}

			}

			printContainedPackages(pi);

			/*
			 * NOTE: inserted or unchanged classes are handled in
			 * process(ClassInfo) method
			 */
			printDeletedClasses(pi);

		} catch (Exception e) {

			String msg = e.getMessage();
			if (msg != null) {
				result.addError(msg);
			}
			e.printStackTrace(System.err);
		}
	}

	private void printDeletedClasses(PackageInfo pix) {

		if (hasDiff(pix, ElementType.CLASS, Operation.DELETE)) {

			Set<DiffElement> classdiffs = getDiffs(pix, ElementType.CLASS,
					Operation.DELETE);

			for (DiffElement diff : classdiffs) {

				// child class was deleted
				ClassInfo deletedCi = (ClassInfo) diff.subElement;

				/*
				 * Print the class if it is not a code list or enumeration
				 * (because these categories are not printed).
				 */
				if (deletedCi.category() != Options.CODELIST
						&& deletedCi.category() != Options.ENUMERATION) {

					PrintClass(deletedCi, true, Operation.DELETE, pix);
				}
			}

		} else {

			/*
			 * inserted and unchanged classes are handled in process(ClassInfo)
			 * method
			 */
		}
	}

	private void appendImageInfo(List<ImageMetadata> images)
			throws SAXException {

		if (!includeDiagrams) {
			return;
		}

		writer.startElement("images");

		for (ImageMetadata img : images) {

			// TBD: at the moment this is only used by the docx transformation
			// the information could therefore be moved to a separate file
			AttributesImpl atts = new AttributesImpl();
			atts.addAttribute("", "id", "", "CDATA", img.getId());
			atts.addAttribute("", "idAsInt", "", "CDATA",
					"" + imgIntegerIdCounter);
			imgIntegerIdCounter = imgIntegerIdCounter + imgIntegerIdStepwidth;
			atts.addAttribute("", "name", "", "CDATA", img.getName());
			atts.addAttribute("", "height", "", "CDATA", "" + img.getHeight());
			atts.addAttribute("", "width", "", "CDATA", "" + img.getWidth());
			atts.addAttribute("", "relPath", "", "CDATA",
					img.getRelPathToFile());

			writer.emptyElement("image", atts);

			// also keep track of the image metadata for later use
			imageList.add(img);
		}

		writer.endElement("images");
	}

	private Model getReferenceModel() {

		String imt = options.parameter(this.getClass().getName(),
				"referenceModelType");
		String mdl = options.parameter(this.getClass().getName(),
				"referenceModelFileNameOrConnectionString");

		if (imt == null || imt.isEmpty())
			return null;

		if (mdl == null || mdl.isEmpty())
			return null;

		// Support original model type codes
		if (imt.equalsIgnoreCase("ea7"))
			imt = "de.interactive_instruments.ShapeChange.Model.EA.EADocument";
		else if (imt.equalsIgnoreCase("xmi10"))
			imt = "de.interactive_instruments.ShapeChange.Model.Xmi10.Xmi10Document";
		else if (imt.equalsIgnoreCase("gsip"))
			imt = "us.mitre.ShapeChange.Model.GSIP.GSIPDocument";
		else if (imt.equalsIgnoreCase("scxml")) {
			imt = "de.interactive_instruments.ShapeChange.Model.Generic.GenericModel";
		} else {
			result.addInfo(this, 29, imt);
		}

		Model m = null;

		// Get model object from reflection API
		Class<?> theClass;
		try {
			theClass = Class.forName(imt);
			if (theClass == null) {
				result.addError(null, 17, imt);
				result.addError(null, 22, mdl);
				return null;
			}
			m = (Model) theClass.getConstructor().newInstance();
			if (m != null) {
				m.initialise(result, options, mdl);
			} else {
				result.addError(null, 17, imt);
				result.addError(null, 22, mdl);
				return null;
			}
		} catch (ClassNotFoundException e) {
			result.addError(null, 17, imt);
			result.addError(null, 22, mdl);
		} catch (IllegalArgumentException | InstantiationException
				| InvocationTargetException | NoSuchMethodException e) {
			result.addError(null, 19, imt);
			result.addError(null, 22, mdl);
		} catch (IllegalAccessException | SecurityException e) {
			result.addError(null, 20, imt);
			result.addError(null, 22, mdl);
		} catch (ShapeChangeAbortException e) {
			result.addError(null, 22, mdl);
			m = null;
		}
		return m;
	}

	private void PrintDescriptors(Info i, boolean isClass, Operation op)
			throws SAXException {
		String s;
		String[] sa;

		s = i.name();
		s = checkDiff(s, i, ElementType.NAME);
		writer.dataElement("name", PrepareToPrint(s), op);

		s = i.aliasName();
		/*
		 * Always include the alias if a diff exists for it; otherwise only
		 * include the alias if requested via parameter and if it has a value
		 */
		if (hasDiff(i, ElementType.ALIAS)) {

			// get the diff
			Set<DiffElement> diffs = getDiffs(i, ElementType.ALIAS);
			// there can only be one change to the alias
			s = differ.diff_toString(diffs.iterator().next().diff);

			writer.dataElement("title", PrepareToPrint(s), op);

		} else {

			if (includeTitle && i.aliasName() != null
					&& i.aliasName().length() > 0) {
				// calling the element that holds the 'alias' value
				// TODO note that 'title' is legacy, it should be called 'alias'
				writer.dataElement("title", s, op);
			}
		}

		s = i.definition();
		s = checkDiff(s, i, ElementType.DEFINITION);
		if (s != null && s.length() > 0) {
			PrintLineByLine(s, "definition", op);
		}

		s = i.description();
		s = checkDiff(s, i, ElementType.DESCRIPTION);
		if (s != null && s.length() > 0) {
			PrintLineByLine(s, "description", op);
		}

		sa = i.examples();
		// TODO compute and check diffs
		if (sa != null) {
			Arrays.sort(sa);
			for (String s2 : sa)
				if (s2 != null)
					PrintLineByLine(s2, "example", null);
		}

		s = i.legalBasis();
		s = checkDiff(s, i, ElementType.LEGALBASIS);
		if (s != null && s.length() > 0) {
			PrintLineByLine(s, "legalBasis", op);
		}

		sa = i.dataCaptureStatements();
		// TODO compute and check diffs
		if (sa != null) {
			Arrays.sort(sa);
			for (String s2 : sa)
				if (s2 != null)
					PrintLineByLine(s2, "dataCaptureStatement", null);
		}

		s = i.primaryCode();
		s = checkDiff(s, i, ElementType.PRIMARYCODE);
		if (s != null && s.length() > 0) {
			writer.dataElement("code", PrepareToPrint(s), op);
		}

		s = i.globalIdentifier();
		s = checkDiff(s, i, ElementType.GLOBALIDENTIFIER);
		if (s != null && s.length() > 0) {
			writer.dataElement("globalIdentifier", PrepareToPrint(s), op);
		}
	}

	/**
	 * @param i
	 * @param type
	 * @return the diffs with the given ElementType for the given Info object,
	 *         if such diffs exist; can be empty but not <code>null</code>
	 */
	private SortedSet<DiffElement> getDiffs(Info i, ElementType type) {

		SortedSet<DiffElement> result = new TreeSet<DiffElement>();

		if (diffs != null && diffs.get(i) != null) {

			for (DiffElement diff : diffs.get(i)) {
				if (diff.subElementType == type) {
					result.add(diff);
				}
			}
		}

		return result;
	}

	/**
	 * @param i
	 * @param type
	 * @param op
	 * @return the diffs with the given ElementType and Operation for the given
	 *         Info object, if such diffs exist; can be empty but not
	 *         <code>null</code>
	 */
	private SortedSet<DiffElement> getDiffs(Info i, ElementType type,
			Operation op) {

		SortedSet<DiffElement> result = new TreeSet<DiffElement>();

		if (diffs != null && diffs.get(i) != null) {

			for (DiffElement diff : diffs.get(i)) {
				if (diff.subElementType == type && diff.change == op) {
					result.add(diff);
				}
			}
		}

		return result;
	}

	/**
	 * @param i
	 * @param type
	 * @return <code>true</code> if at least one diff with the given type exists
	 *         for the given Info object; else <code>false</code>
	 */
	private boolean hasDiff(Info i, ElementType type) {

		if (diffs != null && diffs.get(i) != null) {
			for (DiffElement diff : diffs.get(i)) {
				if (diff.subElementType == type) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Checks if a diff with the given ElementType exists for the given Info
	 * object. If so, the string representation of the diff is returned.
	 * Otherwise the given String is returned.
	 * 
	 * @param s
	 *                 original String
	 * @param i
	 *                 Info object for which a diff might exist
	 * @param type
	 *                 the type of element for which a diff shall be looked up
	 * @return the diff with the given ElementType for the given Info object, if
	 *         it exists - otherwise the original String
	 */
	private String checkDiff(String s, Info i, ElementType type) {

		if (diffs != null && diffs.get(i) != null) {
			for (DiffElement diff : diffs.get(i)) {
				if (diff.subElementType == type) {
					return differ.diff_toString(diff.diff);
				}
			}
		}

		return s;
	}

	// FIXME package structure
	private void PrintPackage(PackageInfo pix, Operation op) throws Exception {

		if (packageInPackage(pix)) {

			writer.startElement("Package", "id", "_P" + pix.id(), op);

			PrintDescriptors(pix, false, op);

			String pixOwnerId = pix.owner().id();

			/*
			 * if pix was deleted, use the id from the according diff as owner
			 * id
			 */
			Info ownerOfInputModel = getInfoWithDiff(ElementType.SUBPACKAGE,
					Operation.DELETE, pix);
			if (ownerOfInputModel != null) {
				pixOwnerId = ownerOfInputModel.id();
			}

			writer.emptyElement("parent", "idref", "_P" + pixOwnerId);

			if (pix.getDiagrams() != null) {
				appendImageInfo(pix.getDiagrams());
			}

			writer.endElement("Package");
		}

		// now handle contained packages and potentially deleted classes

		// check if package pix has been deleted
		if (op != null && op == Operation.DELETE) {

			// package has been deleted: print all its classes and packages

			for (PackageInfo delpi : pix.containedPackages()) {

				if (!delpi.isSchema()) {
					PrintPackage(delpi, Operation.DELETE);
				}
			}

			for (ClassInfo delci : pix.containedClasses()) {

				PrintClass(delci, true, Operation.DELETE, pix);
			}

		} else {

			// pix itself has not been deleted; handle its content

			// check if subpackages of pix have been deleted
			if (hasDiff(pix, ElementType.SUBPACKAGE, Operation.DELETE)) {

				Set<DiffElement> pkgdiffs = getDiffs(pix,
						ElementType.SUBPACKAGE, Operation.DELETE);

				for (DiffElement diff : pkgdiffs) {

					// child package was deleted
					PrintPackage((PackageInfo) diff.subElement,
							Operation.DELETE);
				}

			}

			printContainedPackages(pix);

			/*
			 * NOTE: inserted or unchanged classes are handled in
			 * process(ClassInfo) method
			 */
			printDeletedClasses(pix);
		}
	}

	private void printContainedPackages(PackageInfo pix) throws Exception {

		for (PackageInfo pix2 : pix.containedPackages()) {

			if (!pix2.isSchema()) {

				/*
				 * Check for diffs concerning the children of the given package
				 * (pix).
				 */
				if (hasDiff(pix, ElementType.SUBPACKAGE, Operation.INSERT,
						pix2)) {

					// child package was inserted
					PrintPackage(pix2, Operation.INSERT);

				} else {

					/*
					 * child package has not been deleted (this is checked
					 * elsewhere) or inserted
					 */
					PrintPackage(pix2, null);
				}
			}
		}
	}

	private void PrintLineByLine(String s, String ename, Operation op)
			throws SAXException {

		boolean ins = false;
		boolean del = false;

		String[] lines = s.replace("[NEWLINE]", "\n").replace("\r\n", "\n")
				.replace("\r", "\n").split("\n");

		for (String line : lines) {

			String text = PrepareToPrint(line);

			if (ins) {
				text = "[[ins]]" + line;
				ins = false;
			} else if (del) {
				text = "[[del]]" + line;
				del = false;
			}

			if (countSubstringInString(text,
					"[[ins]]") > countSubstringInString(text, "[[/ins]]")) {
				ins = true;
				text += "[[/ins]]";
			} else if (countSubstringInString(text,
					"[[del]]") > countSubstringInString(text, "[[/del]]")) {
				del = true;
				text += "[[/del]]";
			}

			text = options.internalize(text);

			writer.dataElement(ename, text, op);
		}
	}

	private int countSubstringInString(String str, String substr) {

		int count = 0;
		int idx = 0;
		while ((idx = str.indexOf(substr, idx)) != -1) {
			idx++;
			count++;
		}

		return count;
	}

	private String PrepareToPrint(String s) {
		s = s.trim();
		return s;
	}

	/** Add attribute to an element */
	protected void addAttribute(Document document, Element e, String name,
			String value) {
		Attr att = document.createAttribute(name);
		att.setValue(value);
		e.setAttributeNode(att);
	}

	protected Document createDocument() {
		Document document = null;
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			document = db.newDocument();
		} catch (ParserConfigurationException e) {
			result.addFatalError(null, 2);
			String m = e.getMessage();
			if (m != null) {
				result.addFatalError(m);
			}
			e.printStackTrace(System.err);
			System.exit(1);
		} catch (Exception e) {
			result.addFatalError(e.getMessage());
			e.printStackTrace(System.err);
			System.exit(1);
		}

		return document;
	}

	private boolean packageInPackage(PackageInfo pi) {
		if (Package.length() == 0)
			return true;
		if (pi.name().equals(Package))
			return true;
		if (pi.isSchema())
			return false;
		return packageInPackage(pi.owner());
	}

	public void process(ClassInfo ci) {
		if (error)
			return;

		if (!packageInPackage(ci.pkg()))
			return;

		// determine diff operation for ci
		Operation op = null;
		if (diffs != null && diffs.get(ci.pkg()) != null)
			for (DiffElement diff : diffs.get(ci.pkg())) {
				if (diff.subElementType == ElementType.CLASS
						&& ((ClassInfo) diff.subElement) == ci
						&& diff.change == Operation.INSERT) {
					op = Operation.INSERT;
					break;
				}
			}
		if (op == null) {
			PackageInfo pix = ci.pkg();
			while (pix != null) {
				if (diffs != null && pix.owner() != null
						&& diffs.get(pix.owner()) != null)
					for (DiffElement diff : diffs.get(pix.owner())) {
						if (diff.subElementType == ElementType.SUBPACKAGE
								&& ((PackageInfo) diff.subElement) == pix
								&& diff.change == Operation.INSERT) {
							op = Operation.INSERT;
							pix = null;
							break;
						}
					}
				if (pix != null)
					pix = pix.owner();
			}
		}

		int cat = ci.category();
		switch (cat) {
		case Options.OKSTRAFID:
		case Options.FEATURE:
		case Options.OBJECT:
			PrintClass(ci, true, op, ci.pkg());
			break;
		case Options.MIXIN:
			if (!inheritedProperties)
				PrintClass(ci, true, op, ci.pkg());
			break;
		case Options.OKSTRAKEY:
		case Options.DATATYPE:
		case Options.UNION:
		case Options.BASICTYPE:
		case Options.CODELIST:
		case Options.ENUMERATION:
			PrintClass(ci, true, op, ci.pkg());
			for (String t : ci.supertypes()) {
				ClassInfo cix = model.classById(t);
				if (cix != null) {
					additionalClasses.add(cix);
				}
			}
			break;		
		}
	}

	private void PrintValue(PropertyInfo propi, Operation op)
			throws SAXException {

		String propiid = "_A" + propi.id() + VALUE_ID_SUFFIX;
		propiid = options.internalize(propiid);

		writer.startElement("Value", "id", propiid, op);

		String s = propi.aliasName();
		s = checkDiff(s, propi, ElementType.ALIAS);
		if (s == null || s.length() == 0) {
			s = propi.name();
			s = checkDiff(s, propi, ElementType.NAME);
		}
		writer.dataElement("label", s, op);

		s = propi.initialValue();
		if (s == null || s.length() == 0)
			s = propi.name();

		s = options.internalize(s);

		writer.dataElement("code", PrepareToPrint(s), op);

		s = propi.definition();
		s = checkDiff(s, propi, ElementType.DEFINITION);
		if (s != null && s.length() > 0) {
			PrintLineByLine(s, "definition", op);
		}
		s = propi.description();
		s = checkDiff(s, propi, ElementType.DESCRIPTION);
		if (s != null && s.length() > 0) {
			PrintLineByLine(s, "description", op);
		}

		/*
		 * 2019-05-14 JE - NOTE: code lists and their codes are only partially
		 * represented in the feature catalogues by ShapeChange (typically a
		 * table of the codes).
		 */
		if (representTaggedValues != null) {
			// TBD diff tagged values
			PrintTaggedValues(propi, representTaggedValues, null, true);
		}

		writer.endElement("Value");
	}

	private void PrintValues(ClassInfo ci, Operation op) throws SAXException {

		for (PropertyInfo propi : ci.properties().values()) {
			if (propi == null)
				continue;
			if (!ExportValue(propi))
				continue;

			Operation top = op;
			if (hasDiff(ci, ElementType.ENUM, Operation.INSERT, propi)) {
				top = Operation.INSERT;
			}

			PrintValue(propi, top);
		}

		if (diffs != null && diffs.get(ci) != null) {
			for (DiffElement diff : diffs.get(ci)) {
				if (diff.subElementType == ElementType.ENUM
						&& diff.change == Operation.DELETE) {
					PrintValue((PropertyInfo) diff.subElement,
							Operation.DELETE);
				}
			}
		}
	}

	private boolean ExportItem(Info i) {
		return true;
	}

	private boolean ExportValue(PropertyInfo propi) {
		return ExportItem(propi);
	}

	private boolean ExportProperty(PropertyInfo propi) {
		if (propi.name().length() == 0)
			return false;

		/*
		 * FIXME if (!propi.isNavigable()) return false;
		 */

		return ExportItem(propi);
	}

	private boolean ExportClass(ClassInfo ci, Boolean onlyProperties,
			Operation op) {

		if (!ci.inSchema(pi) && !onlyProperties && op != Operation.DELETE)
			return false;

		if (!packageInPackage(ci.pkg()) && !onlyProperties
				&& op != Operation.DELETE)
			return false;

		return ExportItem(ci);
	}

	private void PrintClass(ClassInfo ci, boolean onlyProperties, Operation op,
			PackageInfo pix) {

		if (!ExportClass(ci, onlyProperties, op))
			return;

		try {

			if (onlyProperties) {

				String ciid = "_C" + ci.id();
				ciid = options.internalize(ciid);

				writer.startElement("FeatureType", "id", ciid, op);

				PrintDescriptors(ci, true, op);

				// NOTE: the Differ currently does not check abstractness
				if (ci.isAbstract()) {
					writer.dataElement("isAbstract", "1");
				}

				for (String t : ci.supertypes()) {

					ClassInfo cix = lookupClassById(t);

					if (cix != null) {

						String name = cix.name();

						String cixid = "_C" + cix.id();
						cixid = options.internalize(cixid);

						// check for insertion
						boolean inserted = false;
						Operation opForGeneralization = op;
						if (hasDiff(ci, ElementType.SUPERTYPE, Operation.INSERT,
								cix)) {
							name = "[[ins]]" + name + "[[/ins]]";
							inserted = true;
							opForGeneralization = Operation.INSERT;
						}

						if (inserted || !inheritedProperties
								|| cix.category() != Options.MIXIN) {

							name = options.internalize(name);

							writer.dataElement("subtypeOf", cix.name(), "idref",
									cixid, opForGeneralization);
						}
					}
				}
				// Diff: check for potential deletions of supertypes
				if (hasDiff(ci, ElementType.SUPERTYPE, Operation.DELETE)) {

					for (DiffElement diff : getDiffs(ci, ElementType.SUPERTYPE,
							Operation.DELETE)) {

						String nameOfDeletedSupertype = "[[del]]"
								+ diff.subElement.name() + "[[/del]]";

						String supertypeId = diff.subElement.id();

						/*
						 * Don't simply use the id from the diff's subElement as
						 * reference. Rather, try to look up the class in the
						 * input model.
						 */
						ClassInfo supertype = lookupClassById(supertypeId);

						String cixid = "_C" + supertype.id();
						cixid = options.internalize(cixid);

						writer.dataElement("subtypeOf", nameOfDeletedSupertype,
								"idref", cixid, Operation.DELETE);
					}
				}

				PrintProperties(ci, true, op);
				/*
				 * TODO PrintOperations true;
				 */

				writer.emptyElement("package", "idref", "_P" + pix.id());

				switch (ci.category()) {
				case Options.FEATURE:
				case Options.OKSTRAFID:

					String text = featureTerm + " Type";
					text = options.internalize(text);

					writer.dataElement("type", text, op);

					break;
				case Options.OBJECT:
					writer.dataElement("type", "Object Type", op);
					break;
				case Options.OKSTRAKEY:
				case Options.DATATYPE:
					writer.dataElement("type", "Data Type", op);
					break;
				case Options.UNION:
					writer.dataElement("type", "Union Data Type", op);
					break;
				case Options.CODELIST:
				    writer.dataElement("type", "Code List Type", op);
					break;
				case Options.ENUMERATION:
					writer.dataElement("type", "Enumeration Type", op);
					break;
				}

				String s;
				for (Constraint constraint : ci.constraints()) {

					// check if the constraint shall be encoded
					if (!inheritedConstraints
							&& isInheritedConstraint(constraint, ci)) {
						continue;
					}

					writer.startElement("constraint");

					writer.dataElement("name", constraint.name());

					s = constraint.text();
					String description = null;
					String expression = null;
					if (s != null && s.contains("/*") && s.contains("*/")) {
						String[] sa = s.split("\\*/");
						description = sa[0].replaceFirst("/\\*", "").trim();
						expression = sa[1].trim();
					} else {
						expression = s;
					}

					if (description != null && description.length() > 0) {
						writer.dataElement("description", description);
					}

					if (expression != null && expression.length() > 0) {
						writer.dataElement("expression", expression);
					}

					writer.endElement("constraint");
				}

				s = ci.taggedValue("alwaysVoid");
				if (s != null && s.length() > 0) {
					writer.startElement("constraint");
					writer.dataElement("description",
							"Properties that are always void: " + s);
					writer.endElement("constraint");
				}
				s = ci.taggedValue("neverVoid");
				if (s != null && s.length() > 0) {
					writer.startElement("constraint");
					writer.dataElement("description",
							"Properties that are never void: " + s);
					writer.endElement("constraint");
				}
				s = ci.taggedValue("appliesTo");
				if (s != null && s.length() > 0) {
					writer.startElement("constraint");
					writer.dataElement("description",
							"Applies to the following network elements: " + s);
					writer.endElement("constraint");
				}

				writer.startElement("taggedValues");

				// backwards compatibility
				s = ci.taggedValue("name");
				if (s != null && s.trim().length() > 0) {
					writer.dataElement("name", PrepareToPrint(s), op);
				}

				if (representTaggedValues != null) {
					// TODO diff tagged values
					PrintTaggedValues(ci, representTaggedValues, null, false);
				}

				writer.endElement("taggedValues");

				if (ci.getDiagrams() != null) {
					appendImageInfo(ci.getDiagrams());
				}

				writer.endElement("FeatureType");
			}

			PrintProperties(ci, false, op);
			/*
			 * TODO PrintOperations false;
			 */

		} catch (SAXException e) {

			String m = e.getMessage();
			if (m != null) {
				result.addError(m);
			}
			e.printStackTrace(System.err);
		}
	}

	/**
	 * Check if the given constraint is inherited, i.e. if one of the direct or
	 * indirect supertypes of the given class (to which the constraint belongs)
	 * has a constraint with the same name and text.
	 * 
	 * @param constraint
	 * @param ci
	 * @return <code>true</code> if the constraint is inherited, else
	 *         <code>false</code>
	 */
	private boolean isInheritedConstraint(Constraint constraint, ClassInfo ci) {

		for (ClassInfo supertype : ci.supertypesInCompleteHierarchy()) {

			for (Constraint stCon : supertype.constraints()) {
				if (stCon.name().equals(constraint.name())
						&& stCon.text().equals(constraint.text())) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * @param i
	 * @param taglist
	 * @param op
	 * @param printTaggedValuesElement
	 *                                     <code>true</code>, if the surrounding
	 *                                     &lt;taggedValues&gt; element shall be
	 *                                     added, if the Info object has at
	 *                                     least one value for the tags from the
	 *                                     list; else <code>false</code> (in
	 *                                     that case, the &lt;taggedValues&gt;
	 *                                     element will never be added and is
	 *                                     assumed to be set outside of the
	 *                                     method)
	 * @throws SAXException
	 */
	private void PrintTaggedValues(Info i, String taglist, Operation op,
			boolean printTaggedValuesElement) throws SAXException {

		TaggedValues taggedValues = i.taggedValuesForTagList(taglist);

		if (!taggedValues.isEmpty()) {

			if (printTaggedValuesElement) {
				writer.startElement("taggedValues");
			}

			// sort results alphabetically by tag name for consistent output
			TreeSet<String> tags = new TreeSet<String>(taggedValues.keySet());
			for (String tag : tags) {
				// sort values
				String[] values = taggedValues.get(tag);
				List<String> valueList = Arrays.asList(values);
				Collections.sort(valueList);

				for (String v : values) {
					if (v.trim().length() > 0) {
						writer.dataElement("taggedValue", v, "tag", tag, op);
						// writer.dataElement(tag, PrepareToPrint(v));
					}
				}
			}

			if (printTaggedValuesElement) {
				writer.endElement("taggedValues");
			}
		}
	}

	private boolean hasDiff(Info i, ElementType subElementType,
			Operation diffChange, Info diffSubelement) {

		if (diffs != null && diffs.get(i) != null) {

			for (DiffElement diff : diffs.get(i)) {

				if (diff.subElementType == subElementType
						&& diff.change == diffChange
						&& diff.subElement == diffSubelement) {
					return true;
				}
			}
		}

		return false;
	}

	private Info getInfoWithDiff(ElementType subElementType,
			Operation diffChange, Info diffSubelement) {

		if (diffs != null) {

			for (Info i : diffs.keySet()) {

				for (DiffElement diff : diffs.get(i)) {

					if (diff.subElementType == subElementType
							&& diff.change == diffChange
							&& diff.subElement == diffSubelement) {
						return i;
					}
				}
			}
		}

		return null;
	}

	private boolean hasDiff(Info i, ElementType subElementType,
			Operation diffChange) {

		if (diffs != null && diffs.get(i) != null) {

			for (DiffElement diff : diffs.get(i)) {

				if (diff.subElementType == subElementType
						&& diff.change == diffChange) {
					return true;
				}
			}
		}

		return false;
	}

	private void PrintProperties(ClassInfo ci, boolean listOnly, Operation op)
			throws SAXException {

		/*
		 * IMPORTANT: it is important that inherited properties are printed
		 * before those that directly belong to the class (ci).
		 */
		if (/* FIXME listOnly && */inheritedProperties) {

			for (String cid : ci.supertypes()) {
				ClassInfo cix = model.classById(cid);
				if (cix != null)
					PrintProperties(cix, listOnly, op);
			}
		}

		for (PropertyInfo propi : ci.properties().values()) {

			Operation top = op;
			if (hasDiff(ci, ElementType.PROPERTY, Operation.INSERT, propi)) {
				top = Operation.INSERT;
			}

			if (listOnly)
				PrintPropertyRef(propi, top);
			else
				PrintProperty(propi, top);
		}

		// also check deletions
		if (diffs != null && diffs.get(ci) != null) {
			for (DiffElement diff : diffs.get(ci)) {
				if (diff.subElementType == ElementType.PROPERTY
						&& diff.change == Operation.DELETE) {
					if (listOnly)
						PrintPropertyRef((PropertyInfo) diff.subElement,
								Operation.DELETE);
					else
						PrintProperty((PropertyInfo) diff.subElement,
								Operation.DELETE);
				}
			}
		}
	}

	private void PrintPropertyRef(PropertyInfo propi, Operation op)
			throws SAXException {

		if (ExportProperty(propi)) {

			String propiid = "_A" + propi.id();
			propiid = options.internalize(propiid);

			writer.emptyElement("characterizedBy", "idref", propiid, op);
		}
	}

	private void PrintProperty(PropertyInfo propi, Operation op)
			throws SAXException {
		if (!ExportProperty(propi))
			return;

		if (exportedProperties.contains(propi))
			return;

		String assocId = "__FIXME";
		if (!propi.isAttribute()) {

			if (!exportedRoles.contains(propi)) {
				assocId = "__" + propi.id();

				writer.startElement("FeatureRelationship", "id", assocId, op);

				writer.dataElement("name", PrepareToPrint("(unbestimmt)"));

				AssociationInfo ai = propi.association();
				if (ai != null) {
					ClassInfo aci = ai.assocClass();
					if (aci != null) {

						String aciid = "_C" + aci.id();
						aciid = options.internalize(aciid);

						writer.dataElement("associationClass",
								PrepareToPrint(aci.name()), "idref", aciid, op);
					}
				}

				String propiid = "_A" + propi.id();
				propiid = options.internalize(propiid);

				// SAX Note: print roles first, then their details
				writer.emptyElement("roles", "idref", propiid, op);

				PropertyInfo propi2 = propi.reverseProperty();
				if (propi2 != null) {
					if (ExportProperty(propi2)) {

						String propi2id = "_A" + propi2.id();
						propi2id = options.internalize(propi2id);

						writer.emptyElement("roles", "idref", propi2id, op);
					}
				}

				writer.endElement("FeatureRelationship");

				// now print the property details
				PrintPropertyDetail(propi, assocId, op);
				exportedRoles.add(propi);

				if (propi2 != null) {
					if (ExportProperty(propi2)) {

						PrintPropertyDetail(propi2, assocId, op);
					}
					exportedRoles.add(propi2);
				}

			} else {
				PropertyInfo propi2 = propi.reverseProperty();
				if (propi2 != null) {
					assocId = "__" + propi2.id();
				}
			}
		} else {
			PrintPropertyDetail(propi, assocId, op);
		}

		exportedProperties.add(propi);
	}

	private void PrintPropertyDetail(PropertyInfo propi, String assocId,
			Operation op) throws SAXException {

		String propiid = "_A" + propi.id();
		propiid = options.internalize(propiid);

		if (propi.isAttribute()) {
			writer.startElement("FeatureAttribute", "id", propiid, op);
		} else {
			writer.startElement("RelationshipRole", "id", propiid, op);
		}

		PrintDescriptors(propi, false, op);

		String s = propi.cardinality().toString();
		s = checkDiff(s, propi, ElementType.MULTIPLICITY);
		String cardinalityText = PrepareToPrint(s);
		cardinalityText = options.internalize(cardinalityText);

		writer.dataElement("cardinality", cardinalityText, op);

		if (!propi.isAttribute() && !propi.isNavigable()) {
			PrintLineByLine("false", "isNavigable", op);
		}

		if (propi.isDerived()) {
			PrintLineByLine("true", "isDerived", op);
		}

		s = propi.initialValue();
		if (propi.isAttribute() && s != null && s.length() > 0) {
			PrintLineByLine(PrepareToPrint(s), "initialValue", op);
		}

		writer.startElement("taggedValues");

		// backwards compatibility
		s = propi.taggedValue("name");
		if (s != null && s.trim().length() > 0) {
			writer.dataElement("name", PrepareToPrint(s), op);
		}
		String[] tags = propi.taggedValuesForTag("length");
		if (tags != null && tags.length > 0) {
			for (String tag : tags) {
				writer.dataElement("length", PrepareToPrint(tag), op);
			}
		}

		if (representTaggedValues != null) {
			PrintTaggedValues(propi, representTaggedValues, null, false);
		}

		writer.endElement("taggedValues");

		if (includeVoidable) {
			if (propi.voidable()) {
				writer.dataElement("voidable", "true", op);
			} else {
				writer.dataElement("voidable", "false", op);
			}
		}

		if (propi.isOrdered()) {
			writer.dataElement("orderIndicator", "1", op);
		} else {
			writer.dataElement("orderIndicator", "0", op);
		}
		if (propi.isUnique()) {
			writer.dataElement("uniquenessIndicator", "1", op);
		} else {
			writer.dataElement("uniquenessIndicator", "0", op);
		}

		Type ti = propi.typeInfo();
		if (!propi.isAttribute() && !propi.isComposition()) {
			if (ti != null) {

				AttributesImpl atts = new AttributesImpl();

				ClassInfo cix = lookupClassById(ti.id);
				if (cix != null) {
					String tiid = "_C" + cix.id();
					tiid = options.internalize(tiid);

					atts.addAttribute("", "idref", "", "CDATA", tiid);
				}
				atts.addAttribute("", "category", "", "CDATA",
						featureTerm.toLowerCase() + " type");
				addOperationToAttributes(op, atts);
				writer.dataElement("", "FeatureTypeIncluded", "", atts,
						checkDiff(ti.name, propi, ElementType.VALUETYPE));
			}
			writer.emptyElement("relation", "idref", assocId);

			PropertyInfo propi2 = propi.reverseProperty();
			if (propi2 != null && ExportProperty(propi2)
					&& propi2.isNavigable()) {

				String propi2id = "_A" + propi2.id();
				propi2id = options.internalize(propi2id);

				writer.emptyElement("InverseRole", "idref", propi2id, op);
			}

		} else {
			if (ti != null) {

				ClassInfo cix;

				if (op != Operation.DELETE) {
					cix = model.classById(ti.id);
				} else {
					cix = refModel.classById(ti.id);
				}

				if (cix != null) {

					int cat = cix.category();
					String cixname = cix.name();
					cixname = checkDiff(cixname, propi, ElementType.VALUETYPE);
					cixname = options.internalize(cixname);

					switch (cat) {
					case Options.CODELIST:
					case Options.ENUMERATION:

						AttributesImpl atts = new AttributesImpl();

						if (cat == Options.CODELIST) {
							atts.addAttribute("", "category", "", "CDATA",
									"code list");

							if (includeCodelistURI) {
								String cl = cix.taggedValue("codeList");
								if (cl == null || cl.isEmpty()) {
									cl = cix.taggedValue("vocabulary");
								}
								if (cl != null && !cl.isEmpty()) {
									atts.addAttribute("", "codeList", "",
											"CDATA", options.internalize(cl));
								}
							}

						} else if (cat == Options.ENUMERATION
								&& !cixname.equals("Boolean")) {
							atts.addAttribute("", "category", "", "CDATA",
									"enumeration");
						}
						addOperationToAttributes(op, atts);
						writer.dataElement("", "ValueDataType", "", atts,
								PrepareToPrint(cixname));

						if (!cixname.equals("Boolean")) {
							writer.dataElement("ValueDomainType", "1", op);
							for (PropertyInfo ei : cix.properties().values()) {
								if (ei != null && ExportValue(ei)) {

									String eiid = "_A" + ei.id() + VALUE_ID_SUFFIX;
									eiid = options.internalize(eiid);

									writer.emptyElement("enumeratedBy", "idref",
											eiid);
								}
							}

							if (diffs != null && diffs.get(cix) != null) {
								for (DiffElement diff : diffs.get(cix)) {
									if (diff.subElementType == ElementType.ENUM
											&& diff.change == Operation.DELETE) {

										writer.emptyElement("enumeratedBy",
												"idref",
												"_A" + ((PropertyInfo) diff.subElement)
														.id()  + VALUE_ID_SUFFIX);
									}
								}
							}

							if (op != Operation.DELETE) {
								if (cix.inSchema(propi.inClass().pkg()))
									enumerations.add(cix);
							} else {
								if (cix.inSchema(refPackage))
									enumerations.add(cix);
							}

							// FIXME if (cix.inSchema(propi.inClass().pkg()))
							// enumerations.add(cix);
						} else {
							writer.dataElement("ValueDomainType", "0", op);
						}
						break;
					default:

						String cixid = "_C" + cix.id();
						cixid = options.internalize(cixid);

						AttributesImpl atts2 = new AttributesImpl();
						atts2.addAttribute("", "idref", "", "CDATA", cixid);

						if (cat == Options.FEATURE
								|| cat == Options.OKSTRAFID) {

							String fttext = featureTerm.toLowerCase() + " type";
							fttext = options.internalize(fttext);

							atts2.addAttribute("", "category", "", "CDATA",
									fttext);

						} else if (cat == Options.DATATYPE
								|| cat == Options.OKSTRAKEY) {
							atts2.addAttribute("", "category", "", "CDATA",
									"data type");
						} else if (cat == Options.UNION) {
							atts2.addAttribute("", "category", "", "CDATA",
									"union data type");
						} else if (cat == Options.BASICTYPE) {
							atts2.addAttribute("", "category", "", "CDATA",
									"basic type");
						}
						addOperationToAttributes(op, atts2);
						writer.dataElement("", "ValueDataType", "", atts2,
								PrepareToPrint(cixname));

						writer.dataElement("ValueDomainType", "0", op);
						break;
					}
				} else {
					String tiname = ti.name;
					tiname = checkDiff(tiname, propi, ElementType.VALUETYPE);
					tiname = options.internalize(tiname);

					writer.dataElement("ValueDataType", PrepareToPrint(tiname),
							op);
				}
			} else {
				writer.dataElement("ValueDataType", "(unknown)", op);
			}
		}

		if (propi.isAttribute()) {
			writer.endElement("FeatureAttribute");
		} else {
			writer.endElement("RelationshipRole");
		}
	}

	/**
	 * Looks up a class as follows: at first we search the class in the input
	 * model (this is the usual case).
	 * 
	 * If the input model does not contain a class with the given id, then we
	 * search in the reference model (which is supplied when a schema diff shall
	 * be computed). If the class was found there, we try to look up the
	 * corresponding class in the input model via the full name (in schema). If
	 * it is found, the ClassInfo from the input model is returned; otherwise
	 * the one from the reference model.
	 * 
	 * We do all this so that references to the class point to the class that
	 * exists in the input model rather than in the reference model (NOTE: when
	 * a diff is computed, then the IDs in the reference model are made unique
	 * by prepending a prefix).
	 * 
	 * @param id
	 * @return
	 */
	private ClassInfo lookupClassById(String id) {

		ClassInfo ci = null;

		if (id != null) {

			// look up class in the input model
			ci = model.classById(id);

			if (ci == null) {

				// is it contained in the reference model?
				if (refModel != null) {

					ci = refModel.classById(id);

					if (ci != null) {

						/*
						 * If we found it in the reference model, can we
						 * identify the class in the input model by its full
						 * name?
						 */
						String fullNameInSchema = ci.fullNameInSchema();
						String fullnamelowercase = fullNameInSchema
								.toLowerCase(Locale.ENGLISH);

						if (inputSchemaClassesByFullNameInSchema
								.containsKey(fullnamelowercase)) {

							// then we'll use the class from the input model
							ci = (ClassInfo) inputSchemaClassesByFullNameInSchema
									.get(fullnamelowercase);
						}
					}
				}
			}
		}

		return ci;
	}

	private void addOperationToAttributes(Operation op, AttributesImpl atts) {

		if (atts != null && op != null) {
			atts.addAttribute("", "mode", "", "CDATA", op.toString());
		}
	}

	public void write() {
	}

	public void writeAll(ShapeChangeResult r) {
		result = r;

		/*
		 * FIXME: workaround until we've decided about the best way to provide
		 * options when writing (currently required for string internalizing).
		 * We can make it a static field or add a parameter to method writeAll.
		 */
		options = r.options();

		if (error || printed)
			return;

		try {

			for (ClassInfo cix : additionalClasses) {

				Operation top = getDiffChange(cix.pkg(), ElementType.CLASS,
						cix);
				PrintClass(cix, false, top, cix.pkg());
			}
			for (ClassInfo cix : enumerations) {
				Operation top = getDiffChange(cix.pkg(), ElementType.CLASS,
						cix);
				PrintValues(cix, top);
			}

			writer.endElement("FeatureCatalogue");
			writer.endDocument();
			writer.close();

		} catch (Exception e) {

			String m = e.getMessage();
			if (m != null) {
				result.addError(m);
			}
			e.printStackTrace(System.err);

		} finally {

			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					String m = e.getMessage();
					if (m != null) {
						result.addError(m);
					}
					e.printStackTrace(System.err);
				}
			}

			if (refModel != null) {
				refModel.shutdown();
				refModel = null;
			}
		}

		// TODO add an option to delete the temporary file
		// File outDir = new File(outputDirectory);
		// File xmlFile = new File(outDir, xmlName);
		// xmlFile.delete();

		printed = true;
	}

	/**
	 * @param i
	 * @param diffSubElementType
	 * @param diffSubElement
	 * @return if a diff exists for i with the given diffSubElementType and
	 *         diffSubElement, the according diff Operation (i.e., the kind of
	 *         change) is returned - else <code>null</code>
	 */
	private Operation getDiffChange(Info i, ElementType diffSubElementType,
			Info diffSubElement) {

		if (diffs != null && diffs.get(i) != null) {
			for (DiffElement diff : diffs.get(i)) {
				if (diff.subElementType == diffSubElementType
						&& diff.subElement == diffSubElement) {
					return diff.change;
				}
			}
		}
		return null;
	}

	private void writePDF(String xmlName, String outfileBasename) {

		if (!OutputFormat.toLowerCase().contains("pdf"))
			return;

		StatusBoard.getStatusBoard().statusChanged(STATUS_WRITE_PDF);

		String pdffileName = outfileBasename + ".pdf";
		String mime = MimeConstants.MIME_PDF;

		if (xmlName != null && xmlName.length() > 0 && xslfofileName != null
				&& xslfofileName.length() > 0 && pdffileName != null
				&& pdffileName.length() > 0) {
			fopWrite(xmlName, xslfofileName, pdffileName, mime);
		}
	}

	private void writeHTML(String xmlName, String outfileBasename) {

		if (!OutputFormat.toLowerCase().contains("html"))
			return;

		String htmlfileName = outfileBasename + ".html";

		if (OutputFormat.toLowerCase().contains("framehtml")) {

			StatusBoard.getStatusBoard().statusChanged(STATUS_WRITE_FRAMEHTML);

			transformationParameters.put("outputdir", outfileBasename);

			File outDir = new File(outputDirectory);
			File xmlFile = new File(outDir, xmlName);
			transformationParameters.put("catalogXmlPath",
					xmlFile.toURI().toString());

			// directory that will contain index.html etc.
			File outputDir = new File(outDir, outfileBasename);
			if (!outputDir.exists()) {
				try {
					FileUtils.forceMkdir(outputDir);
				} catch (IOException ioex) {
					result.addError(this, 31, outputDir.getAbsolutePath());
				}
			}

			if (logoFilePath != null) {
				String logoFileName = readAndStoreLogo(outputDir,
						outfileBasename);
				if (logoFileName != null) {
					transformationParameters.put("logoFileName", logoFileName);
				}
			}

			if (xmlName != null && xmlName.length() > 0
					&& xslframeHtmlFileName != null
					&& xslframeHtmlFileName.length() > 0) {
				xsltWrite(xmlName, xslframeHtmlFileName, htmlfileName);
			}

			File cssDestination = new File(outputDir, cssFileName);

			try {

				if (cssPath.toLowerCase().startsWith("http")) {
					URL css = new URL(cssPath + "/" + cssFileName);
					FileUtils.copyURLToFile(css, cssDestination);
				} else {
					File css = new File(cssPath + "/" + cssFileName);
					if (css.exists()) {
						FileUtils.copyFile(css, cssDestination);
					} else {
						result.addError(this, 18, css.getAbsolutePath());
						return;
					}
				}

			} catch (Exception e) {
				result.addWarning(this, 16, cssFileName, cssPath,
						outputDir.getAbsolutePath());
			}

			if (includeDiagrams) {

				/*
				 * Copy content of temporary images folder to output folder
				 */
				File tmpImgDir = options.imageTmpDir();

				try {

					FileUtils.copyDirectoryToDirectory(tmpImgDir, outputDir);

				} catch (IOException e) {
					result.addError(this, 28, tmpImgDir.getAbsolutePath(),
							outputDir.getAbsolutePath(), e.getMessage());
				}
			}

		} else {

			StatusBoard.getStatusBoard().statusChanged(STATUS_WRITE_HTML);

			File outDir = new File(outputDirectory);

			if (logoFilePath != null) {
				String logoFileName = readAndStoreLogo(outDir, outfileBasename);
				if (logoFileName != null) {
					transformationParameters.put("logoFileName", logoFileName);
				}
			}

			if (xmlName != null && xmlName.length() > 0
					&& xslhtmlfileName != null && xslhtmlfileName.length() > 0
					&& htmlfileName != null && htmlfileName.length() > 0) {
				xsltWrite(xmlName, xslhtmlfileName, htmlfileName);
			}
		}
	}

	private String readAndStoreLogo(File outputDir, String outfileBasename) {

		String logoFileName = outfileBasename + "_logo.png";
		File logoFile = new File(outputDir, logoFileName);

		try {

			BufferedImage img = null;

			if (logoFilePath.toLowerCase().startsWith("http")) {
				URL logoUrl = new URL(logoFilePath);
				img = ImageIO.read(logoUrl);
			} else {
				File localLogoFile = new File(logoFilePath);
				img = ImageIO.read(localLogoFile);
			}

			ImageIO.write(img, "png", logoFile);

			return logoFileName;

		} catch (Exception e) {
			result.addError(this, 30, logoFilePath, e.getMessage());
			return null;
		}
	}

	/**
	 * Transforms the contents of the temporary feature catalogue xml and
	 * inserts it into a specific place (denoted by a placeholder) of a docx
	 * template file. The result is copied into a new output file. The template
	 * file is not modified.
	 * 
	 * @param xmlName
	 *                            Name of the temporary feature catalogue xml
	 *                            file, located in the output directory.
	 * @param outfileBasename
	 *                            Base name of the output file, without file
	 *                            type ending.
	 */
	private void writeDOCX(String xmlName, String outfileBasename) {

		if (!OutputFormat.toLowerCase().contains("docx"))
			return;

		StatusBoard.getStatusBoard().statusChanged(STATUS_WRITE_DOCX);

		ZipHandler zipHandler = new ZipHandler();

		String docxfileName = outfileBasename + ".docx";

		try {

			// Setup directories
			File outDir = new File(outputDirectory);
			File tmpDir = new File(outDir, "tmpdocx");
			File tmpinputDir = new File(tmpDir, "input");
			File tmpoutputDir = new File(tmpDir, "output");

			// get docx template

			// create temporary file for the docx template copy
			File docxtemplate_copy = new File(tmpDir, "docxtemplatecopy.tmp");

			// populate temporary file either from remote or local URI
			if (docxTemplateFilePath.toLowerCase().startsWith("http")) {
				URL templateUrl = new URL(docxTemplateFilePath);
				FileUtils.copyURLToFile(templateUrl, docxtemplate_copy);
			} else {
				File docxtemplate = new File(docxTemplateFilePath);
				if (docxtemplate.exists()) {
					FileUtils.copyFile(docxtemplate, docxtemplate_copy);
				} else {
					result.addError(this, 19, docxtemplate.getAbsolutePath());
					return;
				}
			}

			/*
			 * Unzip the docx template to tmpinputDir and tmpoutputDir The
			 * contents of the tmpinputdir will be used as input for the
			 * transformation. The transformation result will overwrite the
			 * relevant files in the tmpoutputDir.
			 */
			zipHandler.unzip(docxtemplate_copy, tmpinputDir);
			zipHandler.unzip(docxtemplate_copy, tmpoutputDir);

			/*
			 * Get hold of the styles.xml file from which the transformation
			 * will get relevant information. The path to this file will be used
			 * as a transformation parameter.
			 */
			File styleXmlFile = new File(tmpinputDir, "word/styles.xml");
			if (!styleXmlFile.canRead()) {
				result.addError(null, 301, styleXmlFile.getName(),
						"styles.xml");
				return;
			}

			/*
			 * Get hold of the temporary feature catalog xml file. The path to
			 * this file will be used as a transformation parameter.
			 */
			File xmlFile = new File(outDir, xmlName);
			if (!xmlFile.canRead()) {
				result.addError(null, 301, xmlFile.getName(), xmlName);
				return;
			}

			/*
			 * Get hold of the input document.xml file (internal .xml file from
			 * the docxtemplate). It will be used as the source for the
			 * transformation.
			 */
			File indocumentxmlFile = new File(tmpinputDir, "word/document.xml");
			if (!indocumentxmlFile.canRead()) {
				result.addError(null, 301, indocumentxmlFile.getName(),
						"document.xml");
				return;
			}

			/*
			 * Get hold of the output document.xml file. It will be used as the
			 * transformation target.
			 */
			File outdocumentxmlFile = new File(tmpoutputDir,
					"word/document.xml");
			if (!outdocumentxmlFile.canWrite()) {
				result.addError(null, 307, outdocumentxmlFile.getName(),
						"document.xml");
				return;
			}

			/*
			 * Prepare the transformation.
			 */
			transformationParameters.put("styleXmlPath",
					styleXmlFile.toURI().toString());
			transformationParameters.put("catalogXmlPath",
					xmlFile.toURI().toString());
			transformationParameters.put("DOCX_PLACEHOLDER", DOCX_PLACEHOLDER);
			transformationParameters.put("docxStyle", docxStyle);

			/*
			 * Execute the transformation.
			 */
			this.xsltWrite(indocumentxmlFile, xsldocxfileName,
					outdocumentxmlFile);

			if (includeDiagrams && !imageList.isEmpty()) {
				/*
				 * === Process image information ===
				 */

				/*
				 * 1. Copy content of temporary images folder to output folder
				 */
				File mediaDir = new File(tmpoutputDir, "word/media");
				FileUtils.copyDirectoryToDirectory(options.imageTmpDir(),
						mediaDir);

				/*
				 * 2. Create image information file. The path to this file will
				 * be used as a transformation parameter.
				 */

				Document imgInfoDoc = createDocument();

				imgInfoDoc.appendChild(imgInfoDoc.createComment(
						"Temporary file containing image metadata"));

				Element imgInfoRoot = imgInfoDoc.createElement("images");
				imgInfoDoc.appendChild(imgInfoRoot);

				addAttribute(imgInfoDoc, imgInfoRoot, "xmlns:xsi",
						"http://www.w3.org/2001/XMLSchema-instance");

				for (ImageMetadata im : imageList) {

					Element e1 = imgInfoDoc.createElement("image");

					addAttribute(imgInfoDoc, e1, "id", im.getId());
					addAttribute(imgInfoDoc, e1, "relPath",
							im.getRelPathToFile());

					imgInfoRoot.appendChild(e1);
				}

				Properties outputFormat = OutputPropertiesFactory
						.getDefaultMethodProperties("xml");
				outputFormat.setProperty("indent", "yes");
				outputFormat.setProperty(
						"{http://xml.apache.org/xalan}indent-amount", "2");
				if (encoding != null)
					outputFormat.setProperty("encoding", encoding);

				File relsFile = new File(tmpDir, "docx_relationships.tmp.xml");

				try {

					OutputStream fout = new FileOutputStream(relsFile);
					OutputStream bout = new BufferedOutputStream(fout);
					OutputStreamWriter outputXML = new OutputStreamWriter(bout,
							outputFormat.getProperty("encoding"));

					Serializer serializer = SerializerFactory
							.getSerializer(outputFormat);
					serializer.setWriter(outputXML);
					serializer.asDOMSerializer().serialize(imgInfoDoc);
					outputXML.close();
				} catch (Exception e) {
					String m = e.getMessage();
					if (m != null) {
						result.addError(m);
					}
					e.printStackTrace(System.err);
				}

				/*
				 * 3. Apply transformation to relationships file
				 */

				/*
				 * Get hold of the input relationships file (internal file from
				 * the docx template). It will be used as the source for the
				 * transformation.
				 */

				File inRelsXmlFile = new File(tmpinputDir,
						"word/_rels/document.xml.rels");
				if (!inRelsXmlFile.canRead()) {
					result.addError(null, 301, inRelsXmlFile.getName(),
							"document.xml.rels");
					return;
				}

				/*
				 * Get hold of the output relationships file. It will be used as
				 * the transformation target.
				 */
				File outRelsXmlFile = new File(tmpoutputDir,
						"word/_rels/document.xml.rels");
				if (!outRelsXmlFile.canWrite()) {
					result.addError(null, 307, outRelsXmlFile.getName(),
							"document.xml.rels");
					return;
				}

				/*
				 * Prepare the transformation.
				 */
				transformationParameters.put("imageInfoXmlPath",
						relsFile.toURI().toString());

				/*
				 * Execute the transformation.
				 */
				this.xsltWrite(inRelsXmlFile, xsldocxrelsfileName,
						outRelsXmlFile);

				/*
				 * 4. Apply transformation to content types file
				 * 
				 * NOTE: To ensure it contains: <Default Extension="jpg"
				 * ContentType="image/jpeg"/>
				 */

				/*
				 * Get hold of the input content types file (internal file from
				 * the docx template). It will be used as the source for the
				 * transformation.
				 */

				File inContentTypesXmlFile = new File(tmpinputDir,
						"[Content_Types].xml");
				if (!inContentTypesXmlFile.canRead()) {
					result.addError(null, 301, inContentTypesXmlFile.getName(),
							"[Content_Types].xml");
					return;
				}

				/*
				 * Get hold of the output content types file. It will be used as
				 * the transformation target.
				 */
				File outContentTypesXmlFile = new File(tmpoutputDir,
						"[Content_Types].xml");
				if (!outContentTypesXmlFile.canWrite()) {
					result.addError(null, 307, outContentTypesXmlFile.getName(),
							"[Content_Types].xml");
					return;
				}

				/*
				 * Execute the transformation.
				 */
				this.xsltWrite(inContentTypesXmlFile,
						xsldocxContentTypesFileName, outContentTypesXmlFile);
			}

			/*
			 * === Create the docx result file ===
			 */

			// Get hold of the output docx file (it will be overwritten or
			// initialized).
			File outFile = new File(outDir, docxfileName);

			/*
			 * Zip the temporary output directory and copy it to the output docx
			 * file.
			 */
			zipHandler.zip(tmpoutputDir, outFile);

			/*
			 * === Delete the temporary directory ===
			 */

			try {
				FileUtils.deleteDirectory(tmpDir);
			} catch (IOException e) {
				result.addWarning(this, 20, e.getMessage());
			}

			result.addResult(getTargetName(), outputDirectory, docxfileName,
					null);

		} catch (Exception e) {
			String m = e.getMessage();
			if (m != null) {
				result.addError(m);
			}
			e.printStackTrace(System.err);
		}
	}

	private void writeRTF(String xmlName, String outfileBasename) {

		if (!OutputFormat.toLowerCase().contains("rtf"))
			return;

		StatusBoard.getStatusBoard().statusChanged(STATUS_WRITE_RTF);

		String rtffileName = outfileBasename + ".rtf";

		if (xmlName != null && xmlName.length() > 0 && xslrtffileName != null
				&& xslrtffileName.length() > 0 && rtffileName != null
				&& rtffileName.length() > 0) {
			xsltWrite(xmlName, xslrtffileName, rtffileName);
		}
	}

	private void writeXML(String xmlName, String outfileBasename) {

		if (!OutputFormat.toLowerCase().contains("xml"))
			return;

		StatusBoard.getStatusBoard().statusChanged(STATUS_WRITE_XML);

		String xmloutFileName = outfileBasename + ".xml";

		if (xmlName != null && xmlName.length() > 0 && xslxmlfileName != null
				&& xslxmlfileName.length() > 0 && xmloutFileName != null
				&& xmloutFileName.length() > 0) {
			xsltWrite(xmlName, xslxmlfileName, xmloutFileName);
		}
	}

	private void fopWrite(String xmlName, String xslfofileName,
			String outfileName, String outputMimetype) {
		Properties outputFormat = OutputPropertiesFactory
				.getDefaultMethodProperties("xml");
		outputFormat.setProperty("indent", "yes");
		outputFormat.setProperty("{http://xml.apache.org/xalan}indent-amount",
				"2");
		outputFormat.setProperty("encoding", encoding);

		// redirect FOP-logging to our system, Level 'Warning' by default
		Logger fl = Logger.getLogger("org.apache.fop");
		fl.setLevel(Level.WARNING);
		FopMsgHandler fmh = new FopMsgHandler(result, this);
		fl.addHandler(fmh);

		try {
			// configure fopFactory as desired
			FopFactory fopFactory = FopFactory.newInstance();

			FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
			// configure foUserAgent as desired

			boolean skip = false;

			// Setup directories
			File outDir = new File(outputDirectory);

			// Setup input and output files
			File xmlFile = new File(outDir, xmlName);
			File xsltFile = new File(xsltPath, xslfofileName);
			File outFile = new File(outDir, outfileName);

			if (!xmlFile.canRead()) {
				result.addError(null, 301, xmlFile.getName(), outfileName);
				skip = true;
			}
			if (!xsltFile.canRead()) {
				result.addError(null, 301, xsltFile.getName(), outfileName);
				skip = true;
			}

			if (skip == false) {
				// Setup output
				OutputStream out = null;
				try {
					out = new java.io.FileOutputStream(outFile);
					out = new java.io.BufferedOutputStream(out);
				} catch (Exception e) {
					result.addError(null, 304, outFile.getName(),
							e.getMessage());
					skip = true;
				}
				if (skip == false) {
					try {
						// Construct fop with desired output format
						Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF,
								foUserAgent, out);

						// Setup XSLT
						if (xslTransformerFactory != null) {
							// use TransformerFactory specified in configuration
							System.setProperty(
									"javax.xml.transform.TransformerFactory",
									xslTransformerFactory);
						} else {
							// use TransformerFactory determined by system
						}
						TransformerFactory factory = TransformerFactory
								.newInstance();
						Transformer transformer = factory
								.newTransformer(new StreamSource(xsltFile));

						FopErrorListener el = new FopErrorListener(
								xmlFile.getName(), result, this);
						transformer.setErrorListener(el);

						// Set the value of a <param> in the stylesheet
						transformer.setParameter("versionParam", "2.0");

						// Setup input for XSLT transformation
						Source src = new StreamSource(xmlFile);

						// Resulting SAX events (the generated FO) must be piped
						// through to FOP
						Result res = new SAXResult(fop.getDefaultHandler());

						// Start XSLT transformation and FOP processing
						transformer.transform(src, res);

					} catch (Exception e) {
						result.addError(null, 304, outfileName, e.getMessage());
						skip = true;
					} finally {
						out.close();
						result.addResult(getTargetName(), outputDirectory,
								outfileName, null);
						if (deleteXmlFile)
							xmlFile.delete();
					}
				}
			}

		} catch (Exception e) {
			String m = e.getMessage();
			if (m != null) {
				result.addError(m);
			}
			e.printStackTrace(System.err);
		}
	}

	public void xsltWrite(String xmlName, String xsltfileName,
			String outfileName) {

		// =========================================
		// ensure that the source file is available
		// =========================================

		File outDir = new File(outputDirectory);

		File transformationTargetFile = new File(outDir, outfileName);

		File transformationSourceFile = new File(outDir, xmlName);
		if (!transformationSourceFile.canRead()) {
			result.addError(null, 301,
					transformationSourceFile.getAbsolutePath(),
					transformationTargetFile.getAbsolutePath());
			return;
		}

		xsltWrite(transformationSourceFile, xsltfileName,
				transformationTargetFile);
	}

	public void xsltWrite(File transformationSource, String xsltfileName,
			File transformationTarget) {

		try {

			// ==============================
			// 1. perform additional checks
			// ==============================

			URI xsltMainFileUri = null;
			if (xsltPath.toLowerCase().startsWith("http")) {
				URL url = new URL(xsltPath + "/" + xsltfileName);
				xsltMainFileUri = url.toURI();
			} else {
				File xsl = new File(xsltPath + "/" + xsltfileName);
				if (xsl.exists()) {
					xsltMainFileUri = xsl.toURI();
				} else {
					result.addError(this, 18, xsl.getAbsolutePath());
					return;
				}
			}

			// ==============================
			// 2. perform the transformation
			// ==============================

			// determine if we need to run with a specific JRE
			if (pathToJavaExe == null) {

				// continue using current runtime environment
				XsltWriter writer = new XsltWriter(xslTransformerFactory,
						hrefMappings, transformationParameters, result);

				writer.xsltWrite(transformationSource, xsltMainFileUri,
						transformationTarget);

			} else {

				// execute with JRE from configuration

				List<String> cmds = new ArrayList<String>();

				cmds.add(pathToJavaExe);

				if (javaOptions != null) {
					cmds.add(javaOptions);
				}

				cmds.add("-cp");
				List<String> cpEntries = new ArrayList<String>();

				// determine if execution from jar or from class file
				URL writerResource = XsltWriter.class
						.getResource("XsltWriter.class");
				String writerResourceAsString = writerResource.toString();

				if (writerResourceAsString.startsWith("jar:")) {

					// execution from jar

					// get path to main ShapeChange jar file
					String jarPath = writerResourceAsString.substring(4,
							writerResourceAsString.indexOf("!"));

					URI jarUri = new URI(jarPath);

					// add path to man jar file to class path entries
					File jarF = new File(jarUri);
					cpEntries.add(jarF.getPath());

					/*
					 * Get parent directory in which ShapeChange JAR file
					 * exists, because class path entries in manifest are
					 * defined relative to it.
					 */
					File jarDir = jarF.getParentFile();

					// get manifest and the classpath entries defined by it
					try (JarFile jf = new JarFile(jarF)) {

						Manifest mf = jf.getManifest();
						String classPath = mf.getMainAttributes()
								.getValue("Class-Path");

						if (classPath != null) {

							for (String dependency : classPath.split(" ")) {
								// add path to dependency to class path entries
								File dependencyF = new File(jarDir, dependency);
								cpEntries.add(dependencyF.getPath());
							}
						}
					}

				} else {

					// execution with class files

					// get classpath entries from system class loader
					ClassLoader cl = ClassLoader.getSystemClassLoader();

					URL[] urls = ((URLClassLoader) cl).getURLs();

					for (URL url : urls) {
						File dependencyF = new File(url.getPath());
						cpEntries.add(dependencyF.getPath());
					}
				}

				String cpValue = StringUtils.join(cpEntries,
						System.getProperty("path.separator"));
				cmds.add("\"" + cpValue + "\"");

				/* add fully qualified name of XsltWriter class to command */
				cmds.add(XsltWriter.class.getName());

				// add parameter for hrefMappings (if defined)
				if (!hrefMappings.isEmpty()) {

					List<NameValuePair> hrefMappingsList = new ArrayList<NameValuePair>();
					for (Entry<String, URI> hrefM : hrefMappings.entrySet()) {
						hrefMappingsList.add(new BasicNameValuePair(
								hrefM.getKey(), hrefM.getValue().toString()));
					}
					String hrefMappingsString = URLEncodedUtils.format(
							hrefMappingsList, XsltWriter.ENCODING_CHARSET);

					/*
					 * NOTE: surrounding href mapping string with double quotes
					 * to avoid issues with using '=' inside the string when
					 * passed as parameter in invocation of java executable.
					 */
					cmds.add(XsltWriter.PARAM_hrefMappings);
					cmds.add("\"" + hrefMappingsString + "\"");
				}

				if (!transformationParameters.isEmpty()) {

					List<NameValuePair> transformationParametersList = new ArrayList<NameValuePair>();
					for (Entry<String, String> transParam : transformationParameters
							.entrySet()) {
						transformationParametersList.add(new BasicNameValuePair(
								transParam.getKey(), transParam.getValue()));
					}
					String transformationParametersString = URLEncodedUtils
							.format(transformationParametersList,
									XsltWriter.ENCODING_CHARSET);

					/*
					 * NOTE: surrounding transformation parameter string with
					 * double quotes to avoid issues with using '=' inside the
					 * string when passed as parameter in invocation of java
					 * executable.
					 */
					cmds.add(XsltWriter.PARAM_transformationParameters);
					cmds.add("\"" + transformationParametersString + "\"");
				}

				if (xslTransformerFactory != null) {
					cmds.add(XsltWriter.PARAM_xslTransformerFactory);
					cmds.add(xslTransformerFactory);
				}

				String transformationSourcePath = transformationSource
						.getPath();
				String xsltMainFileUriString = xsltMainFileUri.toString();
				String transformationTargetPath = transformationTarget
						.getPath();

				cmds.add(XsltWriter.PARAM_transformationSourcePath);
				cmds.add("\"" + transformationSourcePath + "\"");

				cmds.add(XsltWriter.PARAM_transformationTargetPath);
				cmds.add("\"" + transformationTargetPath + "\"");

				cmds.add(XsltWriter.PARAM_xsltMainFileUri);
				cmds.add("\"" + xsltMainFileUriString + "\"");

				result.addInfo(this, 26, StringUtils.join(cmds, " "));

				ProcessBuilder pb = new ProcessBuilder(cmds);

				try {
					Process proc = pb.start();

					StreamGobbler outputGobbler = new StreamGobbler(
							proc.getInputStream());
					StreamGobbler errorGobbler = new StreamGobbler(
							proc.getErrorStream());

					errorGobbler.start();
					outputGobbler.start();

					errorGobbler.join();
					outputGobbler.join();

					int exitVal = proc.waitFor();

					if (outputGobbler.hasResult()) {
						result.addInfo(this, 25, outputGobbler.getResult());
					}

					if (exitVal != 0 || errorGobbler.hasResult()) {

						// log error
						if (errorGobbler.hasResult()) {
							result.addError(this, 23, errorGobbler.getResult(),
									"" + exitVal);
						} else {
							result.addError(this, 24, "" + exitVal);
						}
					}

				} catch (InterruptedException e) {
					result.addFatalError(this, 22);
					throw new ShapeChangeAbortException();
				}
			}

			// ==============
			// 2. log result
			// ==============

			if (OutputFormat.toLowerCase().contains("docx")) {

				// nothing to do here, the writeDOCX method adds the proper
				// result

			} else if (OutputFormat.toLowerCase().contains("framehtml")) {

				String outputDir = outputDirectory + "/" + outputFilename;

				result.addResult(getTargetName(), outputDir, "index.html",
						null);
			} else {
				result.addResult(getTargetName(), outputDirectory,
						transformationTarget.getName(), null);
			}

		} catch (Exception e) {
			String m = e.getMessage();
			if (m != null) {
				result.addError(m);
			}
			e.printStackTrace(System.err);
		}
	}

	/**
	 * <p>
	 * This method returns messages belonging to the Feature Catalogue target by
	 * their message number. The organization corresponds to the logic in module
	 * ShapeChangeResult. All functions in that class, which require an message
	 * number can be redirected to the function at hand.
	 * </p>
	 * 
	 * @param mnr
	 *                Message number
	 * @return Message text, including $x$ substitution points.
	 */
	public String message(int mnr) {
		// Get the message proper and return it with an identification prefixed
		String mess = messageText(mnr);
		if (mess == null)
			return null;
		String prefix = "";
		if (mess.startsWith("??")) {
			prefix = "??";
			mess = mess.substring(2);
		}
		return prefix + "Feature Catalogue Target: " + mess;
	}

	public void writeOutput() {

		if (dontTransform) {
			// TODO log message
			return;
		}

		String xmlName = outputFilename + ".tmp.xml";

		writePDF(xmlName, outputFilename);
		writeHTML(xmlName, outputFilename);
		writeXML(xmlName, outputFilename);
		writeRTF(xmlName, outputFilename);
		writeDOCX(xmlName, outputFilename);

	}

	public void initialise(Options o, ShapeChangeResult r) {

		options = o;
		result = r;

		// String interning only used for creation of temporary XML

		initialiseFromOptions();
		initialiseTransformationParameters();

	}

	/**
	 * Set up any common parameters for the XSL transformation.
	 */
	private void initialiseTransformationParameters() {

		this.transformationParameters.put("featureTypeSynonym",
				featureTerm + " Type");
		this.transformationParameters.put("lang", lang);
		this.transformationParameters.put("noAlphabeticSortingForProperties",
				noAlphabeticSortingForProperties);
		this.transformationParameters.put("includeCodelistsAndEnumerations",
			includeCodelistsAndEnumerations);
	}

	private void initialiseFromOptions() {

		outputDirectory = options.parameter(this.getClass().getName(),
				"outputDirectory");
		if (outputDirectory == null)
			outputDirectory = options.parameter("outputDirectory");
		if (outputDirectory == null)
			outputDirectory = ".";

		outputFilename = options.parameter(this.getClass().getName(),
				"outputFilename");
		if (outputFilename == null)
			outputFilename = "FeatureCatalogue";

		docxTemplateFilePath = options.parameter(this.getClass().getName(),
				"docxTemplateFilePath");
		if (docxTemplateFilePath == null)
			docxTemplateFilePath = options.parameter("docxTemplateFilePath");
		// if no path is provided, use the directory of the default template
		if (docxTemplateFilePath == null) {
			docxTemplateFilePath = DOCX_TEMPLATE_URL;
			result.addDebug(this, 17, "docxTemplateFilePath",
					DOCX_TEMPLATE_URL);
		}

		String docxStyleParamValue = options.parameter(
				this.getClass().getName(), FeatureCatalogue.PARAM_DOCX_STYLE);
		if (docxStyleParamValue != null) {
			// configuration validation will ensure that parameter value is
			// valid
			docxStyle = docxStyleParamValue;
		}

		logoFilePath = options.parameter(this.getClass().getName(),
				"logoFilePath");

		String s = options.parameter(this.getClass().getName(),
				"inheritedConstraints");
		if (s != null && s.equalsIgnoreCase("false"))
			inheritedConstraints = false;

		s = options.parameter(this.getClass().getName(), "inheritedProperties");
		if (s != null && s.equalsIgnoreCase("true"))
			inheritedProperties = true;

		s = options.parameter(this.getClass().getName(), "deleteXmlfile");
		if (s != null && s.equalsIgnoreCase("true"))
			deleteXmlFile = true;

		s = options.parameter(this.getClass().getName(), "package");
		if (s != null && s.length() > 0)
			Package = s;
		else
			Package = "";

		s = options.parameter(this.getClass().getName(), PARAM_OUTPUT_FORMAT);
		if (s != null && s.length() > 0)
			OutputFormat = s;
		else
			OutputFormat = "";

		s = options.parameter(this.getClass().getName(), "featureTerm");
		if (s != null && s.length() > 0)
			featureTerm = s;

		s = options.parameter(this.getClass().getName(), "includeDiagrams");
		if (s != null && s.equalsIgnoreCase("true"))
			includeDiagrams = true;

		s = options.parameter(this.getClass().getName(), PARAM_DONT_TRANSFORM);
		if (s != null && s.equalsIgnoreCase("true"))
			dontTransform = true;

		s = options.parameter(this.getClass().getName(),
				PARAM_INCLUDE_CODELIST_URI);
		if (s != null && s.equalsIgnoreCase("false"))
			includeCodelistURI = false;

		representTaggedValues = options.parameter("representTaggedValues");

		// TBD: one could check that input has actually loaded the diagrams;
		// however, in future a transformation could create images as well

		s = options.parameter(this.getClass().getName(), "includeVoidable");
		if (s != null && s.equalsIgnoreCase("false"))
			includeVoidable = false;

		s = options.parameter(this.getClass().getName(), "includeAlias");
		if (s != null) {
			if (s.equalsIgnoreCase("false"))
				includeTitle = false;
		} else {
			// support for old, somewhat misleading, name for this parameter
			s = options.parameter(this.getClass().getName(), "includeTitle");
			if (s != null && s.equalsIgnoreCase("false"))
				includeTitle = false;
		}

		if (model != null) {
			encoding = model.characterEncoding();
		}

		s = options.parameter(this.getClass().getName(),
				PARAM_XSL_TRANSFORMER_FACTORY);
		if (s != null && s.length() > 0)
			xslTransformerFactory = s;

		s = options.parameter(this.getClass().getName(), "xslhtmlFile");
		if (s != null && s.length() > 0)
			xslhtmlfileName = s;

		s = options.parameter(this.getClass().getName(),
				"xslframeHtmlFileName");
		if (s != null && s.length() > 0)
			xslframeHtmlFileName = s;

		s = options.parameter(this.getClass().getName(), "xslfoFile");
		if (s != null && s.length() > 0)
			xslfofileName = s;

		s = options.parameter(this.getClass().getName(), "xslrtfFile");
		if (s != null && s.length() > 0)
			xslrtffileName = s;

		s = options.parameter(this.getClass().getName(), "xsldocxFile");
		if (s != null && s.length() > 0)
			xsldocxfileName = s;

		s = options.parameter(this.getClass().getName(), "xslxmlFile");
		if (s != null && s.length() > 0)
			xslxmlfileName = s;

		/*
		 * first check the xslt path setting(s), then anything that depends on
		 * it for example the css path defaults to the xslt path
		 */
		s = options.parameter(this.getClass().getName(), "xsltPfad");
		if (s != null && s.length() > 0)
			xsltPath = s;

		s = options.parameter(this.getClass().getName(), "xsltPath");
		if (s != null && s.length() > 0)
			xsltPath = s;

		/*
		 * check cssPath only after xslt path has been checked if no value is
		 * provided for cssPath it defaults to the xslt path
		 */
		s = options.parameter(this.getClass().getName(), "cssPath");
		if (s != null && s.length() > 0) {
			cssPath = s;
		} else {
			cssPath = xsltPath;
		}

		s = options.parameter(this.getClass().getName(), "lang");
		if (s != null && s.length() > 0)
			lang = s;

		s = options.parameter(this.getClass().getName(),
				"noAlphabeticSortingForProperties");
		if (s != null && s.equalsIgnoreCase("true"))
			noAlphabeticSortingForProperties = "true";
		
		s = options.parameter(this.getClass().getName(),
			"includeCodelistsAndEnumerations");
		if (s != null && s.equalsIgnoreCase("true"))
		    includeCodelistsAndEnumerations = "true";
		
		s = options.parameter(this.getClass().getName(), "xslLocalizationUri");
		if (s != null && s.length() > 0) {

			try {

				URI locXslUri;
				if (s.startsWith("http")) {
					locXslUri = new URI(s);
				} else {
					locXslUri = new URI(s);
					File f;
					if (!locXslUri.isAbsolute()) {
						f = new File(s);
						locXslUri = f.toURI();
					}
				}

				hrefMappings.put(localizationXslDefaultUri, locXslUri);

			} catch (URISyntaxException e) {
				result.addError(this, 15, "xslLocalizationUri", s,
						e.toString());
			}

		}

		s = options.parameter(this.getClass().getName(),
				"localizationMessagesUri");
		if (s != null && s.length() > 0) {

			try {

				URI locMsgUri;
				if (s.startsWith("http")) {
					locMsgUri = new URI(s);
				} else {
					locMsgUri = new URI(s);
					File f;
					if (!locMsgUri.isAbsolute()) {
						f = new File(s);
						locMsgUri = f.toURI();
					}
				}

				hrefMappings.put(localizationMessagesDefaultUri, locMsgUri);

			} catch (URISyntaxException e) {
				result.addError(this, 15, "localizationMessagesUri", s,
						e.toString());
			}
		}

		String pathToJavaExe_ = options.parameter(this.getClass().getName(),
				PARAM_JAVA_EXE_PATH);
		if (pathToJavaExe_ != null && pathToJavaExe_.trim().length() > 0) {
			pathToJavaExe = pathToJavaExe_.trim();
			if (!pathToJavaExe.startsWith("\"")) {
				pathToJavaExe = "\"" + pathToJavaExe;
			}
			if (!pathToJavaExe.endsWith("\"")) {
				pathToJavaExe = pathToJavaExe + "\"";
			}

			String jo_tmp = options.parameter(this.getClass().getName(),
					PARAM_JAVA_OPTIONS);
			if (jo_tmp != null && jo_tmp.trim().length() > 0) {
				javaOptions = jo_tmp.trim();
			}
		}
	}

	/**
	 * This is the message text provision proper. It returns a message for a
	 * number.
	 * 
	 * @param mnr
	 *                Message number
	 * @return Message text or null
	 */
	protected String messageText(int mnr) {
		switch (mnr) {
		case 12:
			return "Directory named '$1$' does not exist or is not accessible.";
		case 13:
			return "File '$1$' does not exist or is not accessible.";
		case 14:
			return "TBD";
		case 15:
			return "URI syntax exception for configuration parameter '$1$'. Value was: '$2$'. Using default URI stated in XSLT. Exception message: $3$";
		case 16:
			return "Could not copy stylesheet '$1$' from '$2$' to '$3$'.";
		case 17:
			return "No value provided for configuration parameter '$1$', defaulting to: '$2$'.";
		case 18:
			return "XSLT stylesheet $1$ not found.";
		case 19:
			return "DOCX template $1$ not found.";
		case 20:
			return "Could not delete temporary directory created for docx transformation; IOException message is: $1$";
		case 21:
			return "Invalid command for invocation of external java executable. Return code was: $2$. Command was: $1$";
		case 22:
			return "Interruption exception during execution of external java executable.";
		case 23:
			return "Execution of XSLT write with external java executable did not succeed (return code was '$2$'). Error message is: $1$.";
		case 24:
			return "Execution of XSLT write with external java executable did not succeed (return code was '$2$'). No error message was provided.";
		case 25:
			return "Execution of XSLT write with external java executable produced the following log message(s): $1$";
		case 26:
			return "Invoking external JRE with command: $1$";
		case 27:
			return "Message from external java executable: $1$";
		case 28:
			return "Exception occurred when copying content from temporary image directory at '$1$' to directory '$2$'. Message is: $3$.";
		case 29:
			return "Value of parameter 'referenceModelType' is '$1$'.";
		case 30:
			return "Exception occurred while trying to read and store logo file from '$1$'. Exception message is: $2$";
		case 31:
			return "Directory '$1$' could not be created.";
		}
		return null;
	}
}
