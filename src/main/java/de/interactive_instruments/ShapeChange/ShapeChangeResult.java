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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FileUtils;
import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.SerializerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** The result is xxx as an XML file. */
public class ShapeChangeResult {
	// Data
	protected Document document = null;
	protected Element root = null;
	protected Element messages = null;
	protected Element resultFiles = null;
	protected Set<Element> resultElements = new HashSet<Element>();
	protected Properties outputFormat = OutputPropertiesFactory
			.getDefaultMethodProperties("xml");
	protected Options options = null;
	protected boolean fatalErrorReceived = false;

	protected HashSet<String> duplicateMessageCheck;

	protected static boolean printDateTime = false;
	protected static DateFormat dateFormat = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss.SSS");

	// MessageContext objects are returned when emitting messages via
	// addXxxxx() functions. The object holds the generated DOM node and the
	// necessary context to add message details later on. In most of all
	// cases the returned MessageContext object can be safely ignored.
	public static class MessageContext {
		protected ShapeChangeResult result;
		protected String level;
		protected Element message;

		// Ctor: No message output
		public MessageContext(ShapeChangeResult result, String level) {
			this.result = result;
			this.level = level;
			this.message = null;
		}

		// Ctor: Create message in log file
		public MessageContext(ShapeChangeResult result, String level,
				String mtext) {
			this.result = result;
			this.level = level;
			System.err.println(level.substring(0, 1) + " "
					+ (printDateTime ? dateTime() + " " : "") + mtext);
			message = result.document.createElementNS(Options.SCRS_NS, level);
			result.messages.appendChild(message);
			message.setAttribute("message",
					(printDateTime ? dateTime() + " " : "") + mtext);

		}

		// Function to add details to an existing message
		public void addDetail(String mtext) {
			System.err.println(level.substring(0, 1) + " ... " + mtext);
			if (message != null) {
				Element detail = result.document.createElement("Detail");
				detail.setAttribute("message", mtext);
				message.appendChild(detail);
			}
		}

		// Functions analogous to the standard message handlers
		public void addDetail(MessageSource ms, int mnr, String p1, String p2,
				String p3, String p4) {
			String m = ms == null ? result.message(mnr) : ms.message(mnr);
			addDetail(m.replace("$1$", p1).replace("$2$", p2).replace("$3$", p3)
					.replace("$4$", p4));
		}

		public void addDetail(MessageSource ms, int mnr, String p1, String p2,
				String p3) {
			String m = ms == null ? result.message(mnr) : ms.message(mnr);
			addDetail(
					m.replace("$1$", p1).replace("$2$", p2).replace("$3$", p3));
		}

		public void addDetail(MessageSource ms, int mnr, String p1, String p2) {
			String m = ms == null ? result.message(mnr) : ms.message(mnr);
			addDetail(m.replace("$1$", p1).replace("$2$", p2));
		}

		public void addDetail(MessageSource ms, int mnr, String p1) {
			String m = ms == null ? result.message(mnr) : ms.message(mnr);
			addDetail(m.replace("$1$", p1));
		}

		public void addDetail(MessageSource ms, int mnr) {
			String m = ms == null ? result.message(mnr) : ms.message(mnr);
			addDetail(m);
		}
	}

	// Methods
	public ShapeChangeResult(Options o) {
		init();

		options = o;
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			dbf.setValidating(true);
			dbf.setAttribute(Options.JAXP_SCHEMA_LANGUAGE,
					Options.W3C_XML_SCHEMA);
			DocumentBuilder db = dbf.newDocumentBuilder();
			document = db.newDocument();

			root = document.createElementNS(Options.SCRS_NS,
					"ShapeChangeResult");
			document.appendChild(root);
			root.setAttribute("resultCode", "0");
			root.setAttribute("xmlns:r", Options.SCRS_NS);
			root.setAttribute("start", (new Date()).toString());

			String version = "[dev]";
			InputStream stream = getClass()
					.getResourceAsStream("/sc.properties");
			if (stream != null) {
				Properties properties = new Properties();
				properties.load(stream);
				version = properties.getProperty("sc.version");
			}
			root.setAttribute("version", version);

			messages = document.createElementNS(Options.SCRS_NS, "Messages");
			root.appendChild(messages);

			resultFiles = document.createElementNS(Options.SCRS_NS, "Results");
			root.appendChild(resultFiles);

		} catch (ParserConfigurationException e) {
			System.err.println(
					"Bootstrap Error: XML parser was unable to be configured.");
			String m = e.getMessage();
			if (m != null) {
				System.err.println(m);
			}
			e.printStackTrace(System.err);
			System.exit(1);
		} catch (Exception e) {
			System.err.println("Bootstrap Error: " + e.getMessage());
			e.printStackTrace(System.err);
			System.exit(1);
		}

		outputFormat.setProperty("encoding", "UTF-8");
		outputFormat.setProperty("indent", "yes");
		outputFormat.setProperty("{http://xml.apache.org/xalan}indent-amount",
				"2");
	}

	public void init() {
		duplicateMessageCheck = new HashSet<String>(50);
	}

	public boolean isFatalErrorReceived() {
		return fatalErrorReceived;
	}

	private String safe(String s) {
		if (s == null) {
			return "<null>";
		} else {
			return s;
		}
	};

	public MessageContext addDebug(MessageSource ms, int mnr, String p1,
			String p2, String p3, String p4) {
		String m = ms == null ? message(mnr) : ms.message(mnr);
		return addDebug(m.replace("$1$", p1).replace("$2$", p2)
				.replace("$3$", p3).replace("$4$", p4));
	}

	public MessageContext addDebug(MessageSource ms, int mnr, String p1,
			String p2, String p3) {
		String m = ms == null ? message(mnr) : ms.message(mnr);
		return addDebug(m.replace("$1$", p1).replace("$2$", p2).replace("$3$",
				safe(p3)));
	};

	public MessageContext addDebug(MessageSource ms, int mnr, String p1,
			String p2) {
		String m = ms == null ? message(mnr) : ms.message(mnr);
		return addDebug(m.replace("$1$", p1).replace("$2$", p2));
	};

	public MessageContext addDebug(MessageSource ms, int mnr, String p1) {
		String m = ms == null ? message(mnr) : ms.message(mnr);
		return addDebug(m.replace("$1$", p1));
	};

	public MessageContext addDebug(MessageSource ms, int mnr) {
		String m = ms == null ? message(mnr) : ms.message(mnr);
		return addDebug(m);
	};

	public MessageContext addDebug(String m) {
		if (document == null
				|| !options.parameter("reportLevel").equals("DEBUG")) {
			return null;
		}
		if (m.startsWith("??")) {
			m = m.substring(2);
			if (!duplicateMessageCheck.add("D " + m))
				return null;
		}
		return new MessageContext(this, "Debug", m);
	}

	public MessageContext addInfo(MessageSource ms, int mnr, String p1,
			String p2, String p3, String p4) {
		String m = ms == null ? message(mnr) : ms.message(mnr);
		return addInfo(m.replace("$1$", p1).replace("$2$", p2)
				.replace("$3$", p3).replace("$4$", p4));
	}

	public MessageContext addInfo(MessageSource ms, int mnr, String p1,
			String p2, String p3) {
		String m = ms == null ? message(mnr) : ms.message(mnr);
		return addInfo(
				m.replace("$1$", p1).replace("$2$", p2).replace("$3$", p3));
	};

	public MessageContext addInfo(MessageSource ms, int mnr, String p1,
			String p2) {
		String m = ms == null ? message(mnr) : ms.message(mnr);
		return addInfo(m.replace("$1$", p1).replace("$2$", p2));
	};

	public MessageContext addInfo(MessageSource ms, int mnr, String p1) {
		String m = ms == null ? message(mnr) : ms.message(mnr);
		return addInfo(m.replace("$1$", p1));
	};

	public MessageContext addInfo(MessageSource ms, int mnr) {
		String m = ms == null ? message(mnr) : ms.message(mnr);
		return addInfo(m);
	};

	public MessageContext addInfo(String m) {
		String l = options.parameter("reportLevel");
		if (document == null || !l.equals("DEBUG") && !l.equals("INFO")) {
			return null;
		}
		if (m.startsWith("??")) {
			m = m.substring(2);
			if (!duplicateMessageCheck.add("I " + m))
				return null;
		}
		return new MessageContext(this, "Info", m);
	}

	public MessageContext addWarning(MessageSource ms, int mnr, String p1,
			String p2, String p3, String p4) {
		String m = ms == null ? message(mnr) : ms.message(mnr);
		return addWarning(m.replace("$1$", p1).replace("$2$", p2)
				.replace("$3$", p3).replace("$4$", p4));
	}

	public MessageContext addWarning(MessageSource ms, int mnr, String p1,
			String p2, String p3) {
		String m = ms == null ? message(mnr) : ms.message(mnr);
		return addWarning(
				m.replace("$1$", p1).replace("$2$", p2).replace("$3$", p3));
	};

	public MessageContext addWarning(MessageSource ms, int mnr, String p1,
			String p2) {
		String m = ms == null ? message(mnr) : ms.message(mnr);
		return addWarning(m.replace("$1$", p1).replace("$2$", p2));
	};

	public MessageContext addWarning(MessageSource ms, int mnr, String p1) {
		String m = ms == null ? message(mnr) : ms.message(mnr);
		return addWarning(m.replace("$1$", p1));
	};

	public MessageContext addWarning(MessageSource ms, int mnr) {
		String m = ms == null ? message(mnr) : ms.message(mnr);
		return addWarning(m);
	};

	public MessageContext addWarning(String m) {
		String l = options.parameter("reportLevel");
		if (document == null || !l.equals("DEBUG") && !l.equals("INFO")
				&& !l.equals("WARNING")) {
			return null;
		}
		if (m.startsWith("??")) {
			m = m.substring(2);
			if (!duplicateMessageCheck.add("W " + m))
				return null;
		}
		return new MessageContext(this, "Warning", m);
	}

	public MessageContext addError(MessageSource ms, int mnr, String p1,
			String p2, String p3, String p4, String p5, String p6, String p7) {
		String m = ms == null ? message(mnr) : ms.message(mnr);
		return addError(m.replace("$1$", p1).replace("$2$", p2)
				.replace("$3$", p3).replace("$4$", p4).replace("$5$", p5)
				.replace("$6$", p6).replace("$7$", p7));
	}

	public MessageContext addError(MessageSource ms, int mnr, String p1,
			String p2, String p3, String p4) {
		String m = ms == null ? message(mnr) : ms.message(mnr);
		return addError(m.replace("$1$", p1).replace("$2$", p2)
				.replace("$3$", p3).replace("$4$", p4));
	}

	public MessageContext addError(MessageSource ms, int mnr, String p1,
			String p2, String p3) {
		String m = ms == null ? message(mnr) : ms.message(mnr);
		return addError(
				m.replace("$1$", p1).replace("$2$", p2).replace("$3$", p3));
	};

	public MessageContext addError(MessageSource ms, int mnr, String p1,
			String p2) {
		String m = ms == null ? message(mnr) : ms.message(mnr);
		return addError(m.replace("$1$", p1).replace("$2$", p2));
	};

	public MessageContext addError(MessageSource ms, int mnr, String p1) {
		String m = ms == null ? message(mnr) : ms.message(mnr);
		return addError(m.replace("$1$", p1));
	};

	public MessageContext addError(MessageSource ms, int mnr) {
		String m = ms == null ? message(mnr) : ms.message(mnr);
		return addError(m);
	};

	public MessageContext addError(String m) {
		if (document == null) {
			return null;
		}
		if (m.startsWith("??")) {
			m = m.substring(2);
			if (!duplicateMessageCheck.add("E " + m))
				return null;
		}
		return new MessageContext(this, "Error", m);
	}

	public MessageContext addFatalError(MessageSource ms, int mnr, String p1,
			String p2, String p3, String p4) {
		String m = ms == null ? message(mnr) : ms.message(mnr);
		return addFatalError(m.replace("$1$", p1).replace("$2$", p2)
				.replace("$3$", p3).replace("$4$", p4));
	}

	public MessageContext addFatalError(MessageSource ms, int mnr, String p1,
			String p2, String p3) {
		String m = ms == null ? message(mnr) : ms.message(mnr);
		return addFatalError(
				m.replace("$1$", p1).replace("$2$", p2).replace("$3$", p3));
	};

	public MessageContext addFatalError(MessageSource ms, int mnr, String p1,
			String p2) {
		String m = ms == null ? message(mnr) : ms.message(mnr);
		return addFatalError(m.replace("$1$", p1).replace("$2$", p2));
	};

	public MessageContext addFatalError(MessageSource ms, int mnr, String p1) {
		String m = ms == null ? message(mnr) : ms.message(mnr);
		return addFatalError(m.replace("$1$", p1));
	};

	public MessageContext addFatalError(MessageSource ms, int mnr) {
		String m = ms == null ? message(mnr) : ms.message(mnr);
		MessageContext ex = addFatalError(m);
		setResultCode(1);
		return ex;
	};

	public MessageContext addFatalError(String m) {

		fatalErrorReceived = true;

		if (document == null) {
			return null;
		}
		if (m.startsWith("??")) {
			m = m.substring(2);
			if (!duplicateMessageCheck.add("F " + m))
				return null;
		}
		return new MessageContext(this, "FatalError", m);
	}

	public void addResult(String targetName, String dname, String fname,
			String scope) {
		if (document == null) {
			return;
		}
		Element resfile = document.createElementNS(Options.SCRS_NS, "Result");
		resultFiles.appendChild(resfile);
		resfile.setAttribute("target", targetName);
		File file = new File(dname + "/" + fname);
		String path = file.toURI().toASCIIString();
		resfile.setAttribute("href", path);
		if (scope != null)
			resfile.setAttribute("scope", scope);
		resfile.appendChild(document.createTextNode(fname));

		resultElements.add(resfile);
	}

	public void updateResult(File originalFile, File newFile) {

		String originalFilePath = originalFile.toURI().toASCIIString();
		String newFilePath = newFile.toURI().toASCIIString();

		for (Element resultE : this.resultElements) {

			if (resultE.getAttribute("href").equals(originalFilePath)) {

				resultE.setAttribute("href", newFilePath);
				resultE.setTextContent(newFile.getName());
			}
		}
	}

	/**
	 * Copies 'Result' elements with the URI of the given original file as
	 * 'href' attribute, and sets the URI of the given new file as 'href' and
	 * text content of these copies. 'scope' and 'target' will be kept as-is.
	 * 
	 * @param originalFile
	 * @param newFile
	 */
	public void copyResultAndUpdateFileReference(File originalFile,
			File newFile) {

		if (document == null) {
			return;
		}

		String originalFilePath = originalFile.toURI().toASCIIString();
		String newFilePath = newFile.toURI().toASCIIString();

		List<Element> newResultElements = new ArrayList<Element>();

		for (Element resultElementForOriginalFile : this.resultElements) {

			if (resultElementForOriginalFile.getAttribute("href")
					.equals(originalFilePath)) {

				Element resultElementForNewFile = document
						.createElementNS(Options.SCRS_NS, "Result");

				// append the new 'Result' element after resultE
				resultFiles.insertBefore(resultElementForNewFile,
						resultElementForOriginalFile.getNextSibling());

				resultElementForNewFile.setAttribute("target",
						resultElementForOriginalFile.getAttribute("target"));
				
				resultElementForNewFile.setAttribute("href", newFilePath);
				
				if (resultElementForOriginalFile.hasAttribute("scope")) {
					resultElementForNewFile.setAttribute("scope",
							resultElementForOriginalFile.getAttribute("scope"));
				}
				
				resultElementForNewFile.setTextContent(newFile.getName());

				newResultElements.add(resultElementForNewFile);
			}
		}

		resultElements.addAll(newResultElements);

	}

	public void setResultCode(int rc) {
		if (document == null) {
			return;
		}
		root.setAttribute("resultCode", Integer.toString(rc, 10));
	}

	public Options options() {
		return options;
	}

	public void toFile(String filename) {
		if (document == null) {
			return;
		}
		try {
			root.setAttribute("config", options.configFile);
			root.setAttribute("end", (new Date()).toString());

			// check that directory exists and create it if necessary
			File f = new File(filename);
			f = f.getParentFile();
			if (f != null && !f.exists()) {
				FileUtils.forceMkdir(f);
			}

			BufferedWriter outputXML = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(filename), "UTF-8"));
			Serializer serializer = SerializerFactory
					.getSerializer(outputFormat);
			serializer.setWriter(outputXML);
			serializer.asDOMSerializer().serialize(document);
			outputXML.close();

			String xsltfileName = options.parameter("xsltFile");
			if (xsltfileName != null && !xsltfileName.isEmpty()) {

				StreamSource xsltSource;
				if (xsltfileName.toLowerCase().startsWith("http")) {
					// get xslt via URL
					URL url = new URL(xsltfileName);
					URLConnection urlConnection = url.openConnection();
					xsltSource = new StreamSource(
							urlConnection.getInputStream());
				} else {
					InputStream stream = getClass()
							.getResourceAsStream("/xslt/result.xsl");
					if (stream == null) {
						// get it from the file system
						File xsltFile = new File(xsltfileName);
						if (!xsltFile.canRead()) {
							throw new Exception(
									"Cannot read " + xsltFile.getName());
						}
						xsltSource = new StreamSource(xsltFile);
					} else {
						// get it from the JAR file
						xsltSource = new StreamSource(stream);
					}
				}

				File outHTML = new File(filename.replace(".xml", ".html"));
				if (outHTML.exists())
					outHTML.delete();
				BufferedWriter outputHTML = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(outHTML), "UTF-8"));;

				if (xsltSource != null) {
					Source xmlSource = new DOMSource(document);
					Result res = new StreamResult(outputHTML);

					TransformerFactory transFact = TransformerFactory
							.newInstance();
					Transformer trans = transFact.newTransformer(xsltSource);
					trans.transform(xmlSource, res);

					/*
					 * Apparently, the following is necessary to close streams
					 * appropriately when running ShapeChange in a separate
					 * process that was spawned by another process (in the given
					 * case, a server application):
					 */
					xsltSource.getInputStream().close();
					outputHTML.close();
				}
			}

		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
	}

	public Document asDOM() {
		return document;
	}

	protected static String dateTime() {

		Date date = new Date();
		return dateFormat.format(date);
	}

	public String message(int mnr) {

		/*
		 * NOTE: A leading ?? in a message text suppresses multiple appearance
		 * of a message in the output.
		 */
		switch (mnr) {
		case 1:
			return "Unable to get a document builder factory.";
		case 2:
			return "XML Parser was unable to be configured.";
		case 3:
			return "Invalid XMI file.";
		case 4:
			return "XMI version must be 1.0, found: '$1$'.";
		case 5:
			return "Exactly only element <XMI> expected.";
		case 6:
			return "Metamodel must be UML, found: '$1$'.";
		case 7:
			return "The UML version must be 1.3, found: '$1$'.";
		case 8:
			return "Exactly one element <XMI.metamodel> expected.";
		case 9:
			return "Class '$1$' is not associated with a package.";
		case 10:
			return "Class '$1$' in package '$2$' is not associated with an XSD document.";
		case 11:
			return "Could not establish a category for element '$1$', having well-known stereotype <<$2$>> and encoding rule '$3$'; setting category to 'unknown'.";
		case 12:
			return "No application schema found.";
		case 13:
			return "Application schema '$1$' not found.";
		case 15:
			return "Package '$1$' not associated with any XML Schema document. Set tagged value 'xsdDocument' on the according schema package. Alternatively, if a PackageInfo element is used in the input configuration of ShapeChange to mark that package as an application schema, set the XML attribute 'xsdDocument'. Package '$1$' will be associated with XML Schema document '$2$'.";
		case 16:
			return "The XMI file is not associated with a DTD. The DTD is required for validating and processing the XMI file.";
		case 17:
			return "Unknown input model type: '$1$'.";
		case 18:
			return "Unsupported Java version: '$1$'. Java 1.8 or higher required.";
		case 19:
			return "Model object could not be instantiated: '$1$'.";
		case 20:
			return "Model object could not be accessed: '$1$'.";
		case 21:
			return "??Rule name '$1$' is not valid. The rule is ignored.";
		case 22:
			return "??Reference model '$1$' could not be loaded and is ignored.";
		case 23:
			return "Could not create temporary directory for ShapeChange run with read/write access at: $1$.";

		case 30:
			return "Enterprise Architect repository cannot be opened. File name or connection string is: '$2$', EA message is: '$1$'";
		case 31:
			return "Enterprise Architect repository file named '$1$' not found";
		case 32:
			return "Could not create directory $1$";
		case 33:
			return "Could not read diagram from temporary image directory (diagram name: $1$, in package: $2$); this diagram will be ignored.";
		case 34:
			return "Could not delete directory $1$";
		case 35:
			return "Enterprise Architect repository cannot be opened. File name or connection string is: '$2$', username is: '$3$', password is: '$4$', EA message is: '$1$'";
		case 36:
			return "??The excel spreadsheet with SBVR rules was not found at file location '$1$'.";
		case 37:
			return "??Loaded SBVR rules from excel spreadsheet for schema '$1$'.";
		case 38:
			return "Encountered First Order Logic constraint with source type '$1$', parsing of which is not supported yet. A First Order Logic expression cannot be created.";
		case 39:
			return "Context: constraint '$1$' in class '$2$'.";

		case 40:
			return "Microsoft Access Database named '$2$' cannot be opened. JDBC message is: '$1$'";
		case 41:
			return "Microsoft Access Database file named '$1$' not found";
		case 42:
			return "Error reading from Microsoft Access Database '$2$'.  Error message is: '$1$'";
		case 43:
			return "Connecting to $1$";
		case 44:
			return "Connected to $1$";
		case 45:
			return "Starting reading $1$";
		case 46:
			return "Finished reading $1$";
			
		
		case 50:
			return "Element '$1$' has the following stereotype(s) in the input model: '$2$'.";
		case 51:
			return "Stereotype '$2$' of element '$1$' normalized to '$3$'.";
		case 52:
			return "Well-known stereotype '$2$' added to element '$1$'.";
		case 53:
			return "No well-known stereotype found for stereotype '$2$' of element '$1$', ignoring it.";
		case 54:
			return "Element '$1$' has $2$ well-known stereotype(s): '$3$'";
		case 55:
			return "After taking into account data types and enumerations modelled without the use of stereotypes is element '$1$' treated as having $2$ well-known stereotype(s): '$3$'";

		case 100:
			return "??The '$1$' with ID '$2$' has no name. The ID is used instead.";
		case 101:
			return "??Application schema '$1$' with ID '$2$' is not associated with an XML Schema document. A default name is used: '$3$'.";
		case 102:
			return "";
		case 103:
			return "??The association with name '$1$' and ID '$2$' does not have 2 connections: $3$ connections. All Roles will be ignored.";
		case 104:
			return "??The supertypes of class '$1$' are of different categories or the stereotype of the class cannot be determined. This is not supported, the class is ignored.";
		case 105:
			return "??The restriction of UML attribute '$1$' in class '$2$' is not legal. The lower multiplicity limit is smaller than in the supertype '$3$'.";
		case 106:
			return "??The restriction of UML attribute '$1$' in class '$2$' is not legal. The upper multiplicity limit is higher than in the supertype '$3$'.";
		case 107:
			return "??The property '$1$' in class '$2$' has a sequence number that is already in use for another property ($3$) which will be overwritten.";
		case 108:
			return "??The class '$1$' is modelled as a feature or data type, but has at least one supertype of a different category. The supertype is ignored.";
		case 109:
			return "??The class '$1$' is modelled as a feature or data type, but has more than one supertype of the same kind. All but one (arbitrary) supertypes are ignored.";
		case 110:
			return "??A target could not be created for schema '$1$'. This target is supported only for GML versions 3.2 and later.";
		case 111:
			return "??Missing argument to '$1$' option.";
		case 115:
			return "??The class '$1$' is modelled as an interface, but has supertypes that are instantiable. The supertype relationships are ignored.";
		case 116:
			return "Target object element(s) missing in property type for property '$1$'.";
		case 117:
			return "??No XML Schema type for type '$1$' is defined. Only object and data types are supported.";
		case 119:
			return "No element for type '$1$' is defined. Only object and data types are represented by elements.";
		case 121:
			return "Base type '$1$' could not be mapped. Missing base type in complex type '$2$'.";
		case 122:
			return "The type with the name '$1$' has no tagged value 'base' or valid supertype and cannot be mapped to a basic type.";
		case 123:
			return "The type with the name '$1$' has no ID and cannot be mapped to a basic type.";
		case 124:
			return "Failed to create basic type '$1$'.";
		case 125:
			return "The class '$1$' is an enumeration. Generalization relationships are not supported for these classes. All such relationships are ignored.";
		case 126:
			return "Failed to create enumeration type '$1$'.";
		case 127:
			return "The class '$1$' is a codelist. Generalization relationships are not supported for these classes. All such relationships are ignored.";
		case 128:
			return "The property '$1$' cannot be assigned a type as it is mapped to an XML attribute, but the type is not a simple type.";
		case 129:
			return "Union '$1$' as the value type of '$2$' could not be mapped as it does not contain the expected number of exactly one property to be encoded in the application schema.";
		case 130:
			return "No type can be provided for the property '$1$'.";
		case 131:
			return "??The type '$2$' of property '$1$' was not found.";
		case 132:
			return "The class '$1$' is referenced, but is not part of any schema in the model nor is it mapped to a well-known XML Schema type. The class is ignored.";
		case 133:
			return "One or more errors encountered in OCL constraint in $3$ '$1$' : '$2$' ...";
		case 134:
			return "Line/column(s) $1$: $2$";
		case 135:
			return "??The type of property '$1$' was not found by id, only by name (fixed broken type definition).";
		case 136:
			return "The enumeration element with ID '$1$' in class '$2$' contains an empty string as value.";
		case 137:
			return "Property with id '$1' and name '$2' has no type.";
		case 138:
			return "Property with id '$1' and name '$2' has a type with no name.";
		case 139:
			return "Cannot add properties of type '$1' in schema definitions since the type definition is not part of the model.";
		case 140:
			return "Unknown value for $1: $2";
		case 141:
			return "The class '$1$' referenced from class '$2$' is not part of any package nor is it mapped to a well-known XML Schema type. The class is ignored.";
		case 142:
			return "Class '$1$' cannot be suppressed, as no direct or indirect, non-abstract supertype exists that is not suppressed.";
		case 143:
			return "Class '$1$' cannot be suppressed, as it has a non-suppressed subtype '$2$'.";
		case 144:
			return "Class '$1$' cannot be suppressed, as it add at least one property.";
		case 145:
			return "ADE class '$1$' cannot be suppressed, as it has no supertype.";
		case 146:
			return "??Schema '$1$' is missing tagged value '$2$'.";
		case 147:
			return "??Package '$1$' has tagged value '$2$', but is not an application schema.";
		case 148:
			return "??Property '$2$' of class '$1$' is not a composition, but has a data type as its value: '$3$'.";
		case 149:
			return "??Name of '$1$' '$2$' includes invalid characters.";
		case 150:
			return "??Schema '$1$' has dummy tagged value '$2$': '$3$'.";
		case 151:
			return "??Documentation of schema '$1$' is missing the separator '$2$'.";
		case 152:
			return "??Documentation of class '$1$' is missing the separator '$2$'.";
		case 153:
			return "??Documentation of property '$1$' is missing the separator '$2$'.";
		case 154:
			return "??No rule to name the '$1$' of class '$2$' is configured. Please check the current configuration.";
		case 155:
			return "??No rule for a choice/sequence/all container for class '$1$' is configured, sequence is used. Please check the current configuration.";
		case 156:
			return "??Failed to create enumeration type '$1$EnumerationType'.";
		case 157:
			return "??Class of property '$1$' cannot be determined. The property is ignored.";
		case 158:
			return "??MapEntry contains empty mapping target. Verify the configuration and look for 'fixme:fixme' in the created schemas.";
		case 159:
			return "??Package '$1$' has a dependency, but is not an application schema.";
		case 160:
			return "??Package '$1$' is the supplier of a dependency to package '$2$', but is not an application schema.";
		case 161:
			return "??$1$ with id '$2$' is referenced, but cannot be found.";
		case 162:
			return "??XML Schema document name '$1$' is used for more than one schema package.";
		case 163:
			return "??Class name '$1$' is used more than once in application schema '$2$'.";
		case 164:
			return "??Rule '$1$' is unknown, but referenced in the ShapeChange source code. This is a system error.";
		case 166:
			return "Class '$1$' cannot be mapped to an object element and is not included in the mapping of class '$2$'.";
		case 167:
			return "XML Schema document name '$1$' does not contain the .xsd file extension.";
		case 168:
			return "??The property '$1$' in class '$2$' has no sequence number.";
		case 169:
			return "The property '$1$' cannot be assigned a type as it is mapped to an XML Schema list attribute, but the type '$2$' is not a simple type.";
		case 170:
			return "??The property '$1$' cannot be made an array propery as it is not restricted to inline content. Set 'inlineOrByReference' to 'inline' on the property.";
		case 171:
			return "XML Schema document with name '$1$' could not be created, invalid filename.";
		case 172:
			return "??The property '$1$' cannot be made an array propery as the type map does not specify an XML element for type '$2$'.";
		case 173:
			return "??The property '$1$' cannot be made an array propery as the type '$2$' is not represented by an object element in XML.";
		case 174:
			return "??MapEntry contains mapping target '$1$' from unknown schema. Verify the configuration and look for 'fixme:fixme' in the created schemas.";
		case 175:
			return "??'$1$' is a complex type with simple content which cannot be used in a qualifier. 'string' is used instead.";
		case 176:
			return "??A qualifier has no type. 'string' is used instead.";
		case 177:
			return "??A qualifier has type '$1$' which could not be identified unambiguously in model. 'string' is used instead.";
		case 178:
			return "??'$1$' is a data type and cannot be used in a qualifier. 'string' is used instead.";
		case 179:
			return "??'$1$' is a type of an unsupported category for a qualifier. 'string' is used instead.";
		case 180:
			return "Could not find a map entry for the value type '$1$' of property '$2$' or the value type itself (in the model). Thus, constraining facets could not be created.";
		case 181:
			return "??Encoding rule $1$ is specified as default encoding rule for platform $2$ but is not configured.";

		case 200:
			return "??Tagged value '$1$' missing in class '$2$'.";
		case 201:
			return "??Tagged value '$1$' has incorrect value '$3$' in class '$2$'.";
		case 202:
			return "??Tagged value '$1$' missing in property '$2$'.";
		case 203:
			return "??Tagged value '$1$' has incorrect value '$3$' in property '$2$'.";
		case 204:
			return "??Outdated tagged value '$1$' used in property '$2$'.";

		// FeatureCatalogue related messages (TBC)
		case 301:
			return "File '$1$' is not readable, processing of $2$ is skipped.";
		case 303:
			return "Warning while transforming '$1$'. Message: $2$";
		case 304:
			return "Error while transforming '$1$'. Message: $2$";
		case 305:
			return "Fatal error while transforming '$1$'. Message: $2$";
		case 306:
			return "XSLFO-File '$1$' does not exist, PDF generation is skipped.";
		case 307:
			return "File '$1$' is not writable, processing of $2$ is skipped.";
		case 308:
			return "No schema with name '$1$' found in the reference model. Consequently, no diff was performed.";

		case 400:
			return "Context: $1$ '$2$'";

		// 600 - 699 Messages known to be used by multiple targets
		case 600:
			return "File could not be deleted. Exception message: '$1$'.";
		case 601:
			return "Directory named '$1$' does not exist or is not accessible.";

		/*
		 * 700-799 Messages used by InfoImpl (which should not define its own
		 * message() method, since subtypes may override that method and then
		 * the messages defined in InfoImpl would not be visible - or it would
		 * be tedious to ensure that subtypes use message numbers not defined by
		 * InfoImpl and call InfoImpl.message() to see if a code is covered
		 * there.
		 */
		case 701:
			return "A single value was requested for tag '$1$', but in addition to returned value '$2$', an additional value '$3$' exists and is ignored.";
		case 702:
			return "A single value was requested for tag '$1$' in language '$2$', but in addition to returned value '$3$', an additional value '$4$' exists and is ignored.";
		case 703:
			return "Multiple values were requested for descriptor '$1$', but the source '$2$' specified in the configuration only supports single values. No values have been returned.";
		case 704:
			return "??Descriptor '$1$' is a single-valued descriptor, but in addition to returned value '$2$' a value '$3$' exists and is ignored.";
		case 790:
			return "Context: class InfoImpl. Element: $1$. Name: $2$";
		case 791:
			return "Context: class InfoImpl (subtype: PropertyInfo). Name: $1$. In class: $2$";

		case 1000:
			return "Testing UML version 1.4.";
		case 1001:
			return "Class '$1$' with ID '$2$' cannot be identified as being part of any package. The package is probably ignored, for example, because it carries an unsupported stereotype. The ID of the missing package is: '$3$'";
		case 1002:
			return "Restriction of property '$1$' in class '$2$' from supertype '$3$'.";
		case 1003:
			return "The multiplicity value of '$1$' is neither a number nor a known string. '*' is used instead.";
		case 1004:
			return "Class '$1$' has an unknown category, an object is assumed.";
		case 1005:
			return "Stereotype <<$1$>> not supported for UML model elements of type '$2$'.";
		case 1006:
			return "The $1$ '$2$' will be ignored.";
		case 1007:
			return "The discriminator for the UML generalization with ID '$1$' is not blank. This genralization is ignored.";
		case 1008:
			return "Property '$1$' with type of ID '$2$' has hidden labels: '$3$'";
		case 1009:
			return "The property '$1$' is tagged as a metadata property. This is only possible for properties with complex content.";
		case 1010:
			return "Support for nilReason attributes was requested in property '$1$'. This is not possible for properties which have a local $2$ as their value.";
		case 1011:
			return "The constraint '$1$' cannot be associated with a modeling object (ID '$2$').";

		case 10000:
			return "Added tagged value '$1$' for element with ID '$2$' with value: '$3$'.";
		case 10001:
			return "The package with ID '$1$' and name '$2$' was created. Namespace: '$3$'.";
		case 10002:
			return "The association end with name '$1$' is the reverse property to '$2$'.";
		case 10003:
			return "Checked class '$1$', category '$2$', result '$3$'";
		case 10004:
			return "Checking overloading. Class = '$1$'; current class = '$2$'.";
		case 10005:
			return "Generating GML dictionaries with definitions for application schema '$1$'.";
		case 10006:
			return "Processing OCL constraint in class '$1$': '$2$'.";
		case 10007:
			return "Constraint is of type '$1$'.";
		case 10008:
			return "Target type: '$1$'.";
		case 10009:
			return "Property: '$1$'.";
		case 10010:
			return "Value condition: '$1$' '$2$' '$3$'";
		case 10011:
			return "The operation with ID '$1$' and name '$2$' has the following parameter: '$3$'";
		case 10012:
			return "Generating XML Schema for application schema '$1$'.";
		case 10013:
			return "The '$1$' with ID '$2$' and name '$3$' was created.";
		case 10014:
			return "Processing class '$1$'.";
		case 10015:
			return "Class '$1$' is a service.";
		case 10016:
			return "Processing class '$1$', rule '$2$'.";
		case 10017:
			return "Creating XSD document '$1$' for package '$2$'.";
		case 10018:
			return "Rose Bug Fix for Duplicate Global Data Types: DataType '$1$' replaced by '$2$'.";
		case 10019:
			return "Added stereotype '$1$' for element with ID '$2$'.";
		case 10020:
			return "Application schema found, package name: '$1$'";
		case 10021:
			return "Import to namespace '$1$' added.";
		case 10022:
			return "Found: '$1$'";
		case 10023:
			return "Processing local properties of class '$1$'.";
		case 10024:
			return "OCL syntax tree: '$1$'";
		case 10025:
			return "OCL comment: '$1$'";

		case 20103:
			return "---------- now processing: $1$ ----------";
			
		// common profiling related messages 
		case 20201:
			return "Profile identifier is not well-formed.";
		case 20206:
			return "Error parsing component of '$1$' configuration parameter: $2$";		
		case 20216:
			return "Context: model element: '$1$'";
		case 20217:
			return "Context: parsing message: '$1$'";
		case 20218:
			return "Context: profiles string: '$1$'";
		case 20221:
			return "Value of configuration parameter '$1$' does not match one of the defined values (was: '$2$').";

		case 30800:
			return "(Generic model element reader) Unexpected start element found by $1$: '$2$'.";
		case 30801:
			return "(Generic model element reader) Unexpected end element found by $1$: '$2$'.";
		case 30802:
			return "(Generic model element reader) NumberFormatException while parsing content of ImageMetadata element with id '$1$' and name '$2$'. Message is: $3$.";
		case 30803:
			return "(Generic model element reader) Exception occurred while reading the model XML. Message is: $3$.";

		default:
			return "(" + ShapeChangeResult.class.getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
