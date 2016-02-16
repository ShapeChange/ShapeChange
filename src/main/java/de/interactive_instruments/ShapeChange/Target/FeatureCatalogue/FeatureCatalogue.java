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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import name.fraser.neil.plaintext.diff_match_patch;

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
import de.interactive_instruments.ShapeChange.Multiplicity;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.TargetIdentification;
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
import de.interactive_instruments.ShapeChange.ModelDiff.DiffElement;
import de.interactive_instruments.ShapeChange.ModelDiff.DiffElement.ElementType;
import de.interactive_instruments.ShapeChange.ModelDiff.Differ;
import de.interactive_instruments.ShapeChange.Target.DeferrableOutputWriter;
import de.interactive_instruments.ShapeChange.Target.SingleTarget;
import de.interactive_instruments.ShapeChange.Transformation.TransformationConstants;
import de.interactive_instruments.ShapeChange.UI.StatusBoard;
import de.interactive_instruments.ShapeChange.Util.ZipHandler;

/**
 * @author Clemens Portele (portele <at> interactive-instruments <dot> de)
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 * 
 */
public class FeatureCatalogue
		implements SingleTarget, MessageSource, DeferrableOutputWriter {

	public static final int STATUS_WRITE_PDF = 22;
	public static final int STATUS_WRITE_HTML = 23;
	public static final int STATUS_WRITE_XML = 24;
	public static final int STATUS_WRITE_RTF = 25;

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

	private static boolean initialised = false;
	private static XMLWriter writer = null;
	private static String Package = "";
	private static TreeSet<ClassInfo> additionalClasses = new TreeSet<ClassInfo>();
	private static TreeSet<ClassInfo> enumerations = new TreeSet<ClassInfo>();
	private static Model refModel = null;
	private static Boolean Inherit = false;
	private static TreeSet<PropertyInfo> exportedRoles = new TreeSet<PropertyInfo>();
	private static TreeSet<PropertyInfo> exportedProperties = new TreeSet<PropertyInfo>();
	private static String OutputFormat = "";
	private static String outputDirectory = null;
	private static String outputFilename = null;
	private static String docxTemplateFilePath = DOCX_TEMPLATE_URL;
	private static boolean error = false;
	private static boolean printed = false;
	private static String encoding = null;
	private static String xslfofileName = "pdf.xsl";
	private static String xslTransformerFactory = null;
	private static String xslhtmlfileName = "html.xsl";
	private static String xslframeHtmlFileName = "frameHtml.xsl";
	private static String cssFileName = "stylesheet.css";
	private static String xslrtffileName = "rtf.xsl";
	private static String xsldocxfileName = "docx.xsl";
	private static String xsldocxrelsfileName = "docx_rels.xsl";
	private static String xslxmlfileName = "xml.xsl";
	private static String xsltPath = "http://shapechange.net/resources/xslt";
	// private static String xsltPath = "src/main/resources/xslt";
	private static String cssPath = xsltPath;
	private static String lang = "en";
	private static String featureTerm = "Feature";
	private static String noAlphabeticSortingForProperties = "false";
	private static boolean includeVoidable = true;
	private static boolean includeTitle = true;
	private static boolean deleteXmlFile = false;

	private static boolean includeDiagrams = false;
	private static int imgIntegerIdCounter = 0;
	private static int imgIntegerIdStepwidth = 2;
	private static Set<ImageMetadata> imageSet = new HashSet<ImageMetadata>();

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

	public int getTargetID() {
		return TargetIdentification.FEATURE_CATALOGUE.getId();
	}

	public void reset() {
		initialised = false;
		writer = null;
		Package = "";
		additionalClasses.clear();
		enumerations.clear();
		Inherit = false;
		exportedRoles.clear();
		exportedProperties.clear();
		OutputFormat = "";
		outputDirectory = null;
		outputFilename = null;
		docxTemplateFilePath = DOCX_TEMPLATE_URL;
		error = false;
		printed = false;
		encoding = null;
		xslfofileName = "pdf.xsl";
		xslhtmlfileName = "html.xsl";
		xslframeHtmlFileName = "frameHtml.xsl";
		xslrtffileName = "rtf.xsl";
		xsldocxfileName = "docx.xsl";
		xsldocxrelsfileName = "docx_rels.xsl";
		xslxmlfileName = "xml.xsl";
		// xsltPath = "src/main/resources/xslt";
		xsltPath = "http://shapechange.net/resources/xslt";
		cssPath = xsltPath;
		xslTransformerFactory = null;
		lang = "en";
		noAlphabeticSortingForProperties = "false";
		hrefMappings = new TreeMap<String, URI>();
		featureTerm = "Feature";
		includeVoidable = true;
		includeTitle = true;
		deleteXmlFile = false;
		dontTransform = false;
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

				String pathToJavaExe_ = options.parameter(
						this.getClass().getName(), PARAM_JAVA_EXE_PATH);
				if (pathToJavaExe_ != null
						&& pathToJavaExe_.trim().length() > 0) {
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

					/*
					 * check path - and potentially also options - by invoking
					 * the exe
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

						StreamGobbler outputGobbler = new StreamGobbler(
								proc.getInputStream());
						StreamGobbler errorGobbler = new StreamGobbler(
								proc.getErrorStream());

						errorGobbler.start();
						outputGobbler.start();

						errorGobbler.join();
						outputGobbler.join();

						int exitVal = proc.waitFor();

						if (exitVal != 0) {
							if (errorGobbler.hasResult()) {
								MessageContext mc = result.addFatalError(this,
										21, StringUtils.join(cmds, " "),
										"" + exitVal);
								mc.addDetail(this, 27,
										errorGobbler.getResult());
							} else {
								result.addFatalError(this, 21,
										StringUtils.join(cmds, " "),
										"" + exitVal);
							}
							throw new ShapeChangeAbortException();
						}

					} catch (InterruptedException e) {
						result.addFatalError(this, 22);
						throw new ShapeChangeAbortException();
					}
				}

				encounteredAppSchemasByName = new TreeMap<String, Integer>();

				initialiseFromOptions();

				String s = null;

				refModel = getReferenceModel();
				if (refModel != null) {
					Differ differ = new Differ();
					SortedSet<PackageInfo> set = refModel.schemas(p.name());
					if (set.size() == 1) {
						TreeMap<Info, HashSet<DiffElement>> diffs = differ
								.diff(p, set.iterator().next());
						for (Entry<Info, HashSet<DiffElement>> me : diffs
								.entrySet()) {
							MessageContext mc = result
									.addInfo("Model difference - "
											+ me.getKey().fullName().replace(
													p.fullName(), p.name()));
							for (DiffElement diff : me.getValue()) {
								s = diff.change + " " + diff.subElementType;
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
					}
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
					PrintLineByLine(s, "scope");
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
				if (s != null && s.length() > 0)
					writer.dataElement("versionDate", s);
				else
					writer.dataElement("versionDate", "unknown");

				s = options.parameter(this.getClass().getName(), "producer");
				if (s != null && s.length() > 0)
					writer.dataElement("producer", s);
				else
					writer.dataElement("producer", "unknown");
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
				encounteredAppSchemasByName.put(pi.name(), new Integer(count));
			} else {
				nameForAppSchema = pi.name();
				encounteredAppSchemasByName.put(pi.name(), new Integer(1));
			}

			// now set the name of the application schema
			writer.dataElement("name", nameForAppSchema);

			String s = pi.definition();
			if (s != null && s.length() > 0) {
				PrintLineByLine(s, "definition");
			}
			s = pi.description();
			if (s != null && s.length() > 0) {
				PrintLineByLine(s, "description");
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

			for (PackageInfo pix : pi.containedPackages()) {
				if (!pix.isSchema()) {
					PrintPackage(pix);
				}
			}
		} catch (Exception e) {

			String msg = e.getMessage();
			if (msg != null) {
				result.addError(msg);
			}
			e.printStackTrace(System.err);
		}
	}

	private void appendImageInfo(List<ImageMetadata> images)
			throws SAXException {

		if (!includeDiagrams) {
			return;
		}

		Collections.sort(images, new Comparator<ImageMetadata>() {

			@Override
			public int compare(ImageMetadata o1, ImageMetadata o2) {
				return o1.getId().compareTo(o2.getId());
			}
		});

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
			imageSet.add(img);
		}

		writer.endElement("images");
	}

	private Model getReferenceModel() {
		String imt = options.parameter(this.getClass().getName(),
				"referenceModelType");
		String mdl = options.parameter(this.getClass().getName(),
				"referenceModelFile");

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

		Model m = null;

		// Get model object from reflection API
		@SuppressWarnings("rawtypes")
		Class theClass;
		try {
			theClass = Class.forName(imt);
			if (theClass == null) {
				result.addError(null, 17, imt);
				result.addError(null, 22, mdl);
				return null;
			}
			m = (Model) theClass.newInstance();
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
		} catch (InstantiationException e) {
			result.addError(null, 19, imt);
			result.addError(null, 22, mdl);
		} catch (IllegalAccessException e) {
			result.addError(null, 20, imt);
			result.addError(null, 22, mdl);
		} catch (ShapeChangeAbortException e) {
			result.addError(null, 22, mdl);
			m = null;
		}
		return m;
	}

	private void PrintDescriptors(Info i, boolean isClass) throws SAXException {
		String s;
		String[] sa;

		writer.dataElement("name", PrepareToPrint(i.name()));

		if (includeTitle && i.aliasName() != null
				&& i.aliasName().length() > 0) {
			// calling the element that holds the 'alias' value
			// TODO note that 'title' is legacy, it should be called 'alias'
			writer.dataElement("title", i.aliasName());
		}

		s = i.definition();
		if (s != null && s.length() > 0) {
			PrintLineByLine(s, "definition");
		}
		s = i.description();
		if (s != null && s.length() > 0) {
			PrintLineByLine(s, "description");
		}

		sa = i.examples();
		if (sa != null) {
			Arrays.sort(sa);
			for (String s2 : sa)
				if (s2 != null)
					PrintLineByLine(s2, "example");
		}

		s = i.legalBasis();
		if (s != null && s.length() > 0) {
			PrintLineByLine(s, "legalBasis");
		}

		sa = i.dataCaptureStatements();
		if (sa != null) {
			Arrays.sort(sa);
			for (String s2 : sa)
				if (s2 != null)
					PrintLineByLine(s2, "dataCaptureStatement");
		}

		s = i.primaryCode();
		if (s != null && s.length() > 0) {
			writer.dataElement("code", PrepareToPrint(s));
		}
	}

	// FIXME package structure
	private void PrintPackage(PackageInfo pix) throws Exception {
		if (packageInPackage(pix)) {

			writer.startElement("Package", "id", "_P" + pix.id());

			PrintDescriptors(pix, false);

			writer.emptyElement("parent", "idref", "_P" + pix.owner().id());

			if (pix.getDiagrams() != null) {
				appendImageInfo(pix.getDiagrams());
			}

			writer.endElement("Package");
		}

		for (PackageInfo pix2 : pix.containedPackages()) {
			if (!pix2.isSchema())
				PrintPackage(pix2);
		}
	}

	private void PrintLineByLine(String s, String ename) throws SAXException {
		String[] lines = s.replace("[NEWLINE]", "\n").split("\n");
		for (String line : lines) {

			String text = PrepareToPrint(line);
			text = options.internalize(text);

			writer.dataElement(ename, text);
		}
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

		int cat = ci.category();
		switch (cat) {
		case Options.OKSTRAFID:
		case Options.FEATURE:
		case Options.OBJECT:
			PrintClass(ci, true);
			break;
		case Options.MIXIN:
			if (!Inherit)
				PrintClass(ci, true);
			break;
		case Options.OKSTRAKEY:
		case Options.DATATYPE:
		case Options.UNION:
		case Options.BASICTYPE:
			PrintClass(ci, true);
			for (String t : ci.supertypes()) {
				ClassInfo cix = model.classById(t);
				if (cix != null) {
					additionalClasses.add(cix);
				}
			}
			break;
		case Options.CODELIST:
		case Options.ENUMERATION:
			// PrintValues(ci);
			break;
		}
	}

	private void PrintValues(ClassInfo ci) throws SAXException {

		for (PropertyInfo propi : ci.properties().values()) {
			if (propi == null)
				continue;
			if (!ExportValue(propi))
				continue;

			String propiid = "_A" + propi.id();
			propiid = options.internalize(propiid);

			writer.startElement("Value", "id", propiid);

			String s = propi.aliasName();
			if (s == null || s.length() == 0)
				s = propi.name();
			writer.dataElement("label", s);

			s = propi.initialValue();
			if (s == null || s.length() == 0)
				s = propi.name();

			s = options.internalize(s);

			writer.dataElement("code", PrepareToPrint(s));

			s = propi.definition();
			if (s != null && s.length() > 0) {
				PrintLineByLine(s, "definition");
			}
			s = propi.description();
			if (s != null && s.length() > 0) {
				PrintLineByLine(s, "description");
			}

			// FIXME PrintStandardElements(propi);
			writer.endElement("Value");
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

	private boolean ExportClass(ClassInfo ci, Boolean onlyProperties) {
		if (!ci.inSchema(pi) && !onlyProperties)
			return false;

		if (!packageInPackage(ci.pkg()) && !onlyProperties)
			return false;

		return ExportItem(ci);
	}

	private void PrintClass(ClassInfo ci, boolean onlyProperties) {
		if (!ExportClass(ci, onlyProperties))
			return;

		try {

			if (onlyProperties) {

				String ciid = "_C" + ci.id();
				ciid = options.internalize(ciid);

				writer.startElement("FeatureType", "id", ciid);

				PrintDescriptors(ci, true);

				if (ci.isAbstract()) {
					writer.dataElement("isAbstract", "1");
				}

				for (String t : ci.supertypes()) {
					ClassInfo cix = model.classById(t);
					if (cix != null
							&& (!Inherit || cix.category() != Options.MIXIN)) {

						String cixid = "_C" + cix.id();
						cixid = options.internalize(cixid);

						writer.dataElement("subtypeOf", cix.name(), "idref",
								cixid);
					}
				}

				PrintProperties(ci, true);
				/*
				 * TODO PrintOperations true;
				 */

				writer.emptyElement("package", "idref", "_P" + ci.pkg().id());

				switch (ci.category()) {
				case Options.FEATURE:
				case Options.OKSTRAFID:

					String text = featureTerm + " Type";
					text = options.internalize(text);

					writer.dataElement("type", text);
					break;
				case Options.OBJECT:
					writer.dataElement("type", "Object Type");
					break;
				case Options.OKSTRAKEY:
				case Options.DATATYPE:
					writer.dataElement("type", "Data Type");
					break;
				case Options.UNION:
					writer.dataElement("type", "Union Data Type");
					break;
				}

				String s;
				for (Constraint ocl : ci.constraints()) {

					writer.startElement("constraint");

					writer.dataElement("name", ocl.name());

					s = ocl.text();
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

				s = ci.taggedValue("name");
				if (s != null && s.trim().length() > 0) {
					writer.dataElement("name", PrepareToPrint(s));
				}
				writer.endElement("taggedValues");

				if (ci.getDiagrams() != null) {
					appendImageInfo(ci.getDiagrams());
				}

				writer.endElement("FeatureType");
			}

			PrintProperties(ci, false);
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

	private void PrintProperties(ClassInfo ci, boolean listOnly)
			throws SAXException {

		/*
		 * IMPORTANT: it is important that inherited properties are printed
		 * before those that directly belong to the class (ci).
		 */
		if (/* FIXME listOnly && */Inherit) {

			for (String cid : ci.supertypes()) {
				ClassInfo cix = model.classById(cid);
				if (cix != null)
					PrintProperties(cix, listOnly);
			}
		}

		for (PropertyInfo propi : ci.properties().values()) {
			if (listOnly)
				PrintPropertyRef(propi);
			else
				PrintProperty(propi);
		}
	}

	private void PrintPropertyRef(PropertyInfo propi) throws SAXException {

		if (ExportProperty(propi)) {

			String propiid = "_A" + propi.id();
			propiid = options.internalize(propiid);

			writer.emptyElement("characterizedBy", "idref", propiid);
		}
	}

	private void PrintProperty(PropertyInfo propi) throws SAXException {
		if (!ExportProperty(propi))
			return;

		if (exportedProperties.contains(propi))
			return;

		String assocId = "__FIXME";
		if (!propi.isAttribute()) {
			if (!exportedRoles.contains(propi)) {
				assocId = "__" + propi.id();
				writer.startElement("FeatureRelationship", "id", assocId);

				writer.dataElement("name", PrepareToPrint("(unbestimmt)"));

				AssociationInfo ai = propi.association();
				if (ai != null) {
					ClassInfo aci = ai.assocClass();
					if (aci != null) {

						String aciid = "_C" + aci.id();
						aciid = options.internalize(aciid);

						writer.dataElement("associationClass",
								PrepareToPrint(aci.name()), "idref", aciid);
					}
				}

				String propiid = "_A" + propi.id();
				propiid = options.internalize(propiid);

				// SAX Note: print roles first, then their details
				writer.emptyElement("roles", "idref", propiid);

				PropertyInfo propi2 = propi.reverseProperty();
				if (propi2 != null) {
					if (ExportProperty(propi2)) {

						String propi2id = "_A" + propi2.id();
						propi2id = options.internalize(propi2id);

						writer.emptyElement("roles", "idref", propi2id);
					}
				}

				writer.endElement("FeatureRelationship");

				// now print the property details
				PrintPropertyDetail(propi, assocId);
				exportedRoles.add(propi);

				if (propi2 != null) {
					if (ExportProperty(propi2)) {

						PrintPropertyDetail(propi2, assocId);
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
			PrintPropertyDetail(propi, assocId);
		}

		exportedProperties.add(propi);
	}

	private void PrintPropertyDetail(PropertyInfo propi, String assocId)
			throws SAXException {

		String propiid = "_A" + propi.id();
		propiid = options.internalize(propiid);

		if (propi.isAttribute()) {
			writer.startElement("FeatureAttribute", "id", propiid);
		} else {
			writer.startElement("RelationshipRole", "id", propiid);
		}

		PrintDescriptors(propi, false);

		String s;
		Multiplicity m = propi.cardinality();
		if (m.maxOccurs == m.minOccurs)
			s = "" + m.minOccurs;
		else if (m.maxOccurs == Integer.MAX_VALUE)
			s = "" + m.minOccurs + ".." + "*";
		else
			s = "" + m.minOccurs + ".." + m.maxOccurs;

		String cardinalityText = PrepareToPrint(s);
		cardinalityText = options.internalize(cardinalityText);

		writer.dataElement("cardinality", cardinalityText);

		if (!propi.isAttribute() && !propi.isNavigable()) {
			PrintLineByLine("false", "isNavigable");
		}

		if (propi.isDerived()) {
			PrintLineByLine("true", "isDerived");
		}

		s = propi.initialValue();
		if (propi.isAttribute() && s != null && s.length() > 0) {
			PrintLineByLine(PrepareToPrint(s), "initialValue");
		}

		writer.startElement("taggedValues");

		s = propi.taggedValue("name");
		if (s != null && s.trim().length() > 0) {
			writer.dataElement("name", PrepareToPrint(s));
		}
		String[] tags = propi.taggedValuesForTag("length");
		if (tags != null && tags.length > 0) {
			for (String tag : tags) {
				writer.dataElement("length", PrepareToPrint(tag));
			}
		}

		writer.endElement("taggedValues");

		if (includeVoidable) {
			if (propi.voidable()) {
				writer.dataElement("voidable", "true");
			} else {
				writer.dataElement("voidable", "false");
			}
		}

		Type ti = propi.typeInfo();
		if (!propi.isAttribute() && !propi.isComposition()) {
			if (ti != null) {

				AttributesImpl atts = new AttributesImpl();

				ClassInfo cix = model.classById(ti.id);
				if (cix != null) {
					String tiid = "_C" + ti.id;
					tiid = options.internalize(tiid);

					atts.addAttribute("", "idref", "", "CDATA", tiid);
				}
				atts.addAttribute("", "category", "", "CDATA",
						featureTerm.toLowerCase() + " type");
				writer.dataElement("", "FeatureTypeIncluded", "", atts,
						ti.name);
			}
			writer.emptyElement("relation", "idref", assocId);

			PropertyInfo propi2 = propi.reverseProperty();
			if (propi2 != null && ExportProperty(propi2)
					&& propi2.isNavigable()) {

				String propi2id = "_A" + propi2.id();
				propi2id = options.internalize(propi2id);

				writer.emptyElement("InverseRole", "idref", propi2id);
			}
			if (propi.isOrdered()) {
				writer.dataElement("orderIndicator", "1");
			} else {
				writer.dataElement("orderIndicator", "0");
			}

		} else {
			if (ti != null) {
				ClassInfo cix = model.classById(ti.id);

				if (cix != null) {

					int cat = cix.category();
					String cixname = cix.name();
					cixname = options.internalize(cixname);

					switch (cat) {
					case Options.CODELIST:
					case Options.ENUMERATION:

						AttributesImpl atts = new AttributesImpl();

						if (cat == Options.CODELIST) {
							atts.addAttribute("", "category", "", "CDATA",
									"code list");
						} else if (cat == Options.ENUMERATION
								&& !cixname.equals("Boolean")) {
							atts.addAttribute("", "category", "", "CDATA",
									"enumeration");
						}
						writer.dataElement("", "ValueDataType", "", atts,
								PrepareToPrint(cixname));

						if (!cixname.equals("Boolean")) {
							writer.dataElement("ValueDomainType", "1");
							for (PropertyInfo ei : cix.properties().values()) {
								if (ei != null && ExportValue(ei)) {

									String eiid = "_A" + ei.id();
									eiid = options.internalize(eiid);

									writer.emptyElement("enumeratedBy", "idref",
											eiid);
								}
							}
							// FIXME if (cix.inSchema(propi.inClass().pkg()))
							enumerations.add(cix);
						} else {
							writer.dataElement("ValueDomainType", "0");
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
						writer.dataElement("", "ValueDataType", "", atts2,
								PrepareToPrint(cixname));

						writer.dataElement("ValueDomainType", "0");
						break;
					}
				} else {
					String tiname = ti.name;
					tiname = options.internalize(tiname);

					writer.dataElement("ValueDataType", PrepareToPrint(tiname));
				}
			} else {
				writer.dataElement("ValueDataType", "(unknown)");
			}
		}

		if (propi.isAttribute()) {
			writer.endElement("FeatureAttribute");
		} else {
			writer.endElement("RelationshipRole");
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
				PrintClass(cix, false);
			}
			for (ClassInfo cix : enumerations) {
				PrintValues(cix);
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
		}

		// TODO add an option to delete the temporary file
		// File outDir = new File(outputDirectory);
		// File xmlFile = new File(outDir, xmlName);
		// xmlFile.delete();

		printed = true;
	}

	private void writePDF(String xmlName, String outfileBasename) {
		StatusBoard.getStatusBoard().statusChanged(STATUS_WRITE_PDF);

		if (!OutputFormat.toLowerCase().contains("pdf"))
			return;

		String pdffileName = outfileBasename + ".pdf";
		String mime = MimeConstants.MIME_PDF;

		if (xmlName != null && xmlName.length() > 0 && xslfofileName != null
				&& xslfofileName.length() > 0 && pdffileName != null
				&& pdffileName.length() > 0) {
			fopWrite(xmlName, xslfofileName, pdffileName, mime);
		}
	}

	private void writeHTML(String xmlName, String outfileBasename) {
		StatusBoard.getStatusBoard().statusChanged(STATUS_WRITE_HTML);

		if (!OutputFormat.toLowerCase().contains("html"))
			return;

		String htmlfileName = outfileBasename + ".html";

		if (OutputFormat.toLowerCase().contains("framehtml")) {

			transformationParameters.put("outputdir", outfileBasename);

			File outDir = new File(outputDirectory);
			File xmlFile = new File(outDir, xmlName);
			transformationParameters.put("catalogXmlPath",
					xmlFile.toURI().toString());

			if (xmlName != null && xmlName.length() > 0
					&& xslframeHtmlFileName != null
					&& xslframeHtmlFileName.length() > 0) {
				xsltWrite(xmlName, xslframeHtmlFileName, htmlfileName);
			}

			File outputDir = new File(outDir, outfileBasename);
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

		} else {

			if (xmlName != null && xmlName.length() > 0
					&& xslhtmlfileName != null && xslhtmlfileName.length() > 0
					&& htmlfileName != null && htmlfileName.length() > 0) {
				xsltWrite(xmlName, xslhtmlfileName, htmlfileName);
			}
		}
	}

	/**
	 * Transforms the contents of the temporary feature catalogue xml and
	 * inserts it into a specific place (denoted by a placeholder) of a docx
	 * template file. The result is copied into a new output file. The template
	 * file is not modified.
	 * 
	 * @param xmlName
	 *            Name of the temporary feature catalogue xml file, located in
	 *            the output directory.
	 * @param outfileBasename
	 *            Base name of the output file, without file type ending.
	 */
	private void writeDOCX(String xmlName, String outfileBasename) {
		StatusBoard.getStatusBoard().statusChanged(STATUS_WRITE_HTML);

		ZipHandler zipHandler = new ZipHandler();

		if (!OutputFormat.toLowerCase().contains("docx"))
			return;

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
			if (!styleXmlFile.canRead()) {
				result.addError(null, 301, styleXmlFile.getName(), xmlName);
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

			/*
			 * Execute the transformation.
			 */
			this.xsltWrite(indocumentxmlFile, xsldocxfileName,
					outdocumentxmlFile);

			if (includeDiagrams) {
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

				List<ImageMetadata> imageList = new ArrayList<ImageMetadata>(
						imageSet);
				Collections.sort(imageList, new Comparator<ImageMetadata>() {

					@Override
					public int compare(ImageMetadata o1, ImageMetadata o2) {
						return o1.getId().compareTo(o2.getId());
					}
				});

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

			result.addResult(getTargetID(), outputDirectory, docxfileName,
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
		StatusBoard.getStatusBoard().statusChanged(STATUS_WRITE_RTF);

		if (!OutputFormat.toLowerCase().contains("rtf"))
			return;

		String rtffileName = outfileBasename + ".rtf";

		if (xmlName != null && xmlName.length() > 0 && xslrtffileName != null
				&& xslrtffileName.length() > 0 && rtffileName != null
				&& rtffileName.length() > 0) {
			xsltWrite(xmlName, xslrtffileName, rtffileName);
		}
	}

	private void writeXML(String xmlName, String outfileBasename) {
		StatusBoard.getStatusBoard().statusChanged(STATUS_WRITE_XML);

		if (!OutputFormat.toLowerCase().contains("xml"))
			return;

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
						result.addResult(getTargetID(), outputDirectory,
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
			result.addError(null, 301, transformationSourceFile.getName(),
					transformationTargetFile.getName());
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
					Manifest mf = new JarFile(jarF).getManifest();
					String classPath = mf.getMainAttributes()
							.getValue("Class-Path");

					if (classPath != null) {

						for (String dependency : classPath.split(" ")) {
							// add path to dependency to class path entries
							File dependencyF = new File(jarDir, dependency);
							cpEntries.add(dependencyF.getPath());
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

					if (exitVal != 0) {

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

				result.addResult(getTargetID(), outputDir, "index.html", null);
			} else {
				result.addResult(getTargetID(), outputDirectory,
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
	 *            Message number
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
		this.transformationParameters.put("noAlphabeticSortingForProperties", noAlphabeticSortingForProperties);
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

		String s = options.parameter(this.getClass().getName(),
				"inheritedProperties");
		if (s != null && s.equals("true"))
			Inherit = true;

		s = options.parameter(this.getClass().getName(), "deleteXmlfile");
		if (s != null && s.equals("true"))
			deleteXmlFile = true;

		s = options.parameter(this.getClass().getName(), "package");
		if (s != null && s.length() > 0)
			Package = s;
		else
			Package = "";

		s = options.parameter(this.getClass().getName(), "outputFormat");
		if (s != null && s.length() > 0)
			OutputFormat = s;
		else
			OutputFormat = "";

		s = options.parameter(this.getClass().getName(), "featureTerm");
		if (s != null && s.length() > 0)
			featureTerm = s;

		s = options.parameter(this.getClass().getName(), "includeDiagrams");
		if (s != null && s.equals("true"))
			includeDiagrams = true;

		s = options.parameter(this.getClass().getName(), PARAM_DONT_TRANSFORM);
		if (s != null && s.equals("true"))
			dontTransform = true;

		// TBD: one could check that input has actually loaded the diagrams;
		// however, in future a transformation could create images as well

		s = options.parameter(this.getClass().getName(), "includeVoidable");
		if (s != null && s.equalsIgnoreCase("false"))
			includeVoidable = false;

		s = options.parameter(this.getClass().getName(), "includeTitle");
		if (s != null && s.equalsIgnoreCase("false"))
			includeTitle = false;

		if (model != null) {
			encoding = model.characterEncoding();
		}

		s = options.parameter(this.getClass().getName(),
				"xslTransformerFactory");
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
		
		s = options.parameter(this.getClass().getName(), "noAlphabeticSortingForProperties");
		if (s != null && s.trim().length() > 0)
			noAlphabeticSortingForProperties = s.trim();

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
	}

	/**
	 * This is the message text provision proper. It returns a message for a
	 * number.
	 * 
	 * @param mnr
	 *            Message number
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

		}
		return null;
	}
}
