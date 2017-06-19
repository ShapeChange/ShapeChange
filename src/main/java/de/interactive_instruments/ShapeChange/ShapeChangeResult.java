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

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;

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
	protected Properties outputFormat = OutputPropertiesFactory
			.getDefaultMethodProperties("xml");
	protected Options options = null;

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

			FileWriter outputXML = new FileWriter(filename);
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
				FileWriter outputHTML = new FileWriter(outHTML);

				if (xsltSource != null) {
					Source xmlSource = new DOMSource(document);
					Result res = new StreamResult(outputHTML);

					TransformerFactory transFact = TransformerFactory
							.newInstance();
					Transformer trans = transFact.newTransformer(xsltSource);
					trans.transform(xmlSource, res);
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
			return "Stereotype <<$2$>> of class '$1$' is not an allowed value in encoding rule '$3$' and is ignored.";
		case 12:
			return "No application schema found.";
		case 13:
			return "Application schema '$1$' not found.";
		case 14:
			return "No model has been loaded to convert.";
		case 15:
			return "Package '$1$' not associated with any XML Schema document.";
		case 16:
			return "The XMI file is not associated with a DTD. The DTD is required for validating and processing the XMI file.";
		case 17:
			return "Unknown input model type: '$1$'.";
		case 18:
			return "Unsupported Java version: '$1$'. Java 1.6 or higher required.";
		case 19:
			return "Model object could not be instaniated: '$1$'.";
		case 20:
			return "Model object could not be accessed: '$1$'.";
		case 21:
			return "??Rule name '$1$' is not valid. The rule is ignored.";
		case 22:
			return "??Reference model '$1$' could not be loaded and is ignored.";
		case 23:
			return "Could not create temporary directory for ShapeChange run with read/write access at: $1$.";
		case 24:
			return "Neither 'inputFile' nor 'repositoryFileNameOrConnectionString' parameter set in configuration. Cannot connect to a repository.";
		case 25:
			return "Model repository file named '$1$' not found";
		case 26:
			return "Required input parameter 'inputModelType' not found in the configuration.";
		case 27:
			return "Value of input parameter 'inputModelType' (which defines the model implementation) is '$1$'.";

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
		case 165:
			return "Value '$1$' is not allowed for targetParameter 'sortedOutput' in Target '$2$'. Try 'true' (=name), 'name', 'id', 'taggedValue=value' or 'false' (no sorting). 'false' is used.";
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

		// 500-599 Converter messages (some messages used by Converter are
		// contained in other number ranges)
		case 500:
			return "(Converter.java) Executed deferred output write for target class '$1$' for input ID: '$2$'.";
		case 501:
			return "(Converter.java) Now processing transformation '$1$' for input ID: '$2$'.";
		case 502:
			return "(Converter.java) Performed transformation for transformer ID '$1$' for input ID: '$2$'.\n-------------------------------------------------";
		case 503:
			return "(Converter.java) Now processing target '$1$' for input '$2$'.";
		case 504:
			return "(Converter.java) Executed target class '$1$' for input ID: '$2$'.\n-------------------------------------------------";
		case 505:
			return "(Converter.java) Internal class cast exception encountered - message: $1$ (full exception information is only logged for log level debug). Processing of transformation with ID '$2$' did not succeed. All transformations and targets that depend on this transformation will not be executed.";
		case 506:
			return "(Converter.java) Transformation with ID '$1$' is disabled (via the configuration). All transformations and targets that depend on this transformation will not be executed.";
		case 507:
			return "(Converter.java) None of the packages contained in the model is a schema selected for processing. Make sure that the schema you want to process are configured to be a schema (via the 'targetNamespace' tagged value or via a PackageInfo element in the configuration) and also selected for processing (if you use one of the input parameters appSchemaName, appSchemaNameRegex, appSchemaNamespaceRegex, ensure that they include the schema). Execution will stop now.";
		case 508:
			return "??The ConfigurationValidator for transformer or target class '$1$' was found but could not be loaded. Exception message is: $2$";
		case 509:
			return "The semantic validation of the ShapeChange configuration detected one or more errors. Examine the log for further details. Execution will stop now.";
		case 510:
			return "---------- Semantic validation of ShapeChange configuration: START ----------";
		case 511:
			return "---------- Semantic validation of ShapeChange configuration: COMPLETE ----------";
		case 512:
			return "---------- Semantic validation of ShapeChange configuration: SKIPPED ----------";
		case 513:
			return "NOTE: The semantic validation can be skipped by setting the input configuration parameter '"
					+ Options.PARAM_SKIP_SEMANTIC_VALIDATION_OF_CONFIG
					+ "' to 'true'.";
		case 514:
			return "--- Validating transformer with @id '$1$' ...";
		case 515:
			return "--- Validating target with @class '$1$' and @inputs '$2$' ...";

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
			return "Descriptor '$1$' is a single-valued descriptor, but in addition to returned value '$2$' a value '$3$' exists and is ignored.";
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
		case 1012:
			return "Application schema found, package name: '$1$', target namespace: '$2$'";

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

		/* Transformation related messages */
		// (20000-20099) Messages used in multiple transformer classes
		// (20100-20199) TransformationManager messages
		// (20200-20299) Profiler messages
		// (20300-20399) Flattener messages
		// (20400-20499)

		case 20001:
			return "No non-empty string value provided for configuration parameter '$1$'. Execution of '$2$' aborted.";
		case 20002:
			return "Configuration parameter '$1$' required for execution of '$2$' was not provided. Execution of '$2$' aborted.";
		case 20003:
			return "Syntax exception for regular expression value of configuration parameter '$1$' (required for execution of '$2$'). Regular expression value was: $3$. Exception message: $4$. Execution of '$2$' aborted.";

		case 20100:
			return "Could not find application schema for Info type '$1$'";
		case 20101:
			return "Class type of Info object '$1$' not recognized by logic to determine the name of its application schema";
		case 20102:
			return "Value of configuration parameter '$1$' after parsing is '$2$'.";
		case 20103:
			return "---------- now processing: $1$ ----------";
		case 20104:
			return "No associations between feature and feature / object types found in schema '$1$'.";
		case 20105:
			return "Association exists between '$1$' and '$2$'.";
		case 20106:
			return "Association name is '$1$'.";
		case 20107:
			return "Navigable via property '$1$' of class '$2$'.";
		case 20108:
			return "$1$ associations between feature and feature / object types found in schema '$2$'.";
		case 20109:
			return "---------- TransformationManager postprocessing: validating constraints ----------";
		case 20110:
			return "The constraint '$1$' on '$2$' will be converted into a simple TextConstraint.";
		case 20111:
			return "The constraint '$1$' on '$2$' was not recognized as a constraint to be validated.";

		case 20201:
			return "Profile identifier is not well-formed.";
		case 20202:
			return "<UNUSED_20202>";
		case 20203:
			return "The profile set of class '$1$' does not contain the profile set of its subtype '$2$': $3$";
		case 20204:
			return "The profile set of class '$1$' does not contain the profile set of its property '$2$': $3$";
		case 20205:
			return "The application schema package '$1$' is completely empty after profiling.";
		case 20206:
			return "Error parsing component of '$1$' configuration parameter: $2$";
		case 20207:
			return "Removing constraint '$1$' from class '$2$' because the constraint targets a property that is missing in the class or its supertypes (to highest level)";
		case 20208:
			return "System Error: Constraint '$1$' in Class '$2$' not of type 'GenericText/OclConstraint'.";
		case 20209:
			return "$1$";
		case 20210:
			return "GenericPropertyInfo '$1$' is the context model element of the constraint named '$2$'. The property does no longer exist in the model after profiling, thus the constraint is removed.";
		case 20211:
			return "GenericClassInfo '$1$' is the context model element of the constraint named '$2$'. The class does no longer exist in the model after profiling, thus the constraint is removed.";
		case 20212:
			return "Unrecognized constraint context model element type: '$1$'.";
		case 20213:
			return "Unrecognized constraint type: '$1$'.";
		case 20214:
			return "The profile set of class '$1$' does not contain the profile set of its subtype '$2$': $3$. Because of the chosen transformation rule(s), '$1$' and all its subtypes will be removed, so that the profile mismatch between super- and subtype does not lead to model inconsistencies.";
		case 20215:
			return "??Class '$1$' - which is a subtype of '$2$' - is not an instance of GenericClassInfo (likely reason: it belongs to a package that is not part of the schema selected for processing). It (and its possibly existing subtypes) won't be removed from the model (which should be ok, given that it is (likely) not part of the selected schema destined for final processing in target(s)).";
		case 20216:
			return "Context: model element: '$1$'";
		case 20217:
			return "Context: parsing message: '$1$'";
		case 20218:
			return "Context: profiles string: '$1$'";
		case 20219:
			return "Error parsing transformation parameter '$1$': '$2$'. Assuming no profiles as value for the parameter. This may lead to unexpected results.";
		case 20220:
			return "Value of configuration parameter '$1$' does not match one of the defined values (was: '$2$'). Using default value.";
		case 20221:
			return "Value of configuration parameter '$1$' does not match one of the defined values (was: '$2$').";

		case 20301:
			return "(Flattener.java) The type '$2$' of property '$1$' was not found.";
		case 20302:
			return "(Flattener.java) The type '$1$' to replace type '$2$' was not found. Replacing type without changing the id.";
		case 20303:
			return "(Flattener.java) The ClassInfo for type '$1$' was not found in the model.";
		case 20304:
			return "(Flattener.java) maxOccurs parameter configured to be '$1$' - using default value 3";
		case 20305:
			return "(Flattener.java) maxOccurs tagged value for property '$1$' in class '$2$' was set to '$3$' - using global value: '$4$'";
		case 20306:
			return "(Flattener.java) No type information given via configuration parameter 'enforceOptionality'. Rule will not be executed.";
		case 20307:
			return "(Flattener.java) applyRulePropUnionDirectOptionality encountered unknown content model of Union-Direct type for type '$1$'.";
		case 20308:
			return "(Flattener.java) Context: $1$ '$2$'";
		case 20309:
			return "(Flattener.java) Cannot apply rule for flattening name if no value is provided via the configuration parameter '$1$'.";
		case 20310:
			return "(Flattener.java) Invalid pattern encountered for configuration parameter '$1$': $2$";
		case 20311:
			return "(Flattener.java) When creating copy of the subtype hierarchy for '$1$', subtype with id '$2$' either was not found in the model or is not an instance of GenericClassInfo (likely reason: it belongs to a package that is not part of the schema selected for processing). A copy won't be created for this subtype.";
		case 20312:
			return "(Flattener.java) Class '$1$' is not an instance of GenericClassInfo (likely reason: it belongs to a package that is not part of the schema selected for processing). Cannot reliably update subtype info for this class (removing class '$2$' as subtype, and adding its geometry specific copies).";
		case 20313:
			return "(Flattener.java) Class '$1$' has a geometry property. The following supertypes also have one: $2$. Flattening of homogeneous geometries with subtypes is enabled. This only works if all subtypes of a type with geometry do not have a geometry property themselves. The class '$1$' will not be fanned out based upon its own geometry typed properties.";
		case 20314:
			return "(Flattener.java) Could not find supertype with id '$1$' for class with name '$2$' in the model.";
		case 20315:
			return "(Flattener.java) Cannot properly update type of property named '$1$' to the union type named '$2$'.";
		case 20316:
			return "(Flattener.java) Class '$1$' has a geometry property. The following supertypes have a different set of restrictions regarding allowed geometry types: $2$. Flattening of homogeneous geometries with subtypes is enabled. This is a potential inconsistency (potential because the map entries defined for the flattening also influence how a feature type with geometry properties is fanned out).";
		case 20317:
			return "(Flattener.java) ========== $1$ phase ==========";
		case 20318:
			return "(Flattener.java) Model does not contain class '$1$' which is the target type to which the type of property '$2$' (from class '$3$') shall be mapped. Setting type.id of property to UNKNOWN.";
		case 20319:
			return "??(Flattener.java) Class '$1$' - which is a subtype of '$2$' - is not an instance of GenericClassInfo (likely reason: it belongs to a package that is not part of the schema selected for processing). The contents of '$2$' won't be copied to '$1$', which should be fine because '$1$' is not part of a schema selected for processing.";
		case 20320:
			return "??(Flattener.java) Class '$1$' - which is a subtype of '$2$' - is not an instance of GenericClassInfo (likely reason: it belongs to a package that is not part of the schema selected for processing). It (and its possibly existing subtypes) won't be added to the list of subtypes for class '$2$'. $3$";
		case 20321:
			return "??(Flattener.java) Class '$1$' is not an instance of GenericClassInfo (likely reason: it belongs to a package that is not part of the schema selected for processing). Thus it cannot be removed from the model.";
		case 20322:
			return "(Flattener.java) Class '$1$' is not an instance of GenericClassInfo (likely reason: it belongs to a package that is not part of the schema selected for processing). Cannot reliably update subtype info for this class (updating the id for subtype '$2$' in $1$'s subtype list from '$3$' to that of its copy, which has id '$4$').";
		case 20323:
			return "(Flattener.java) Class '$1$' - which is an enumeration - is not an instance of GenericClassInfo (likely reason: it belongs to a package that is not part of the schema selected for processing). Cannot add ONINA enums to the enumeration.";
		case 20324:
			return "(Flattener.java) No type information given via configuration parameter 'removeType'. Rule will not be executed.";
		case 20325:
			return "??(Flattener.java) isFlatTarget tagged value setting(s) will lead to removal of whole association (with one end being property '$1$' in class '$2$' - the other end being property '$3$' in class '$4$').";
		case 20326:
			return "(Flattener.java) --- Found cycle:";
		case 20327:
			return "(Flattener.java)    Class '$1$' -> class '$2$' (via properties: $3$)";
		case 20328:
			return "(Flattener.java) --- No cycles found.";
		case 20329:
			return "(Flattener.java) ---------- Checking for reflexive relationships and cyles in types to process (for type flattening) ----------";
		case 20330:
			return "(Flattener.java) --- Reflexive relationship detected for class '$1$' (via properties: $2$).";
		case 20331:
			return "(Flattener.java) --- No reflexive relationships detected.";
		case 20332:
			return "(Flattener.java) The Flattener configuration lists type '$1$' for removal but could not find it in the model.";
		case 20333:
			return "??(Flattener.java) Homogeneous geometry rule would update the association between classes '$1$' and '$2$' but cannot do so because class '$3$' belongs to a schema that has not been selected for processing. The association won't be updated and will thus eventually be removed.";
		case 20334:
			return "??(Flattener.java) Creating a copy of an association to connect classes '$1$' and '$2$'. The original association has an association class. Copying the association class is currently not supported. The association copy will therefore not have an association class.";
		case 20335:
			return "??(Flattener.java) The map for geometry type specific copies of '$1$' is empty.";
		case 20336:
			return "??(Flattener.java) Inheritance rule would create subtype specific copies of the association between classes '$1$' and '$2$' but cannot do so because class '$3$' belongs to a schema that has not been selected for processing. Copies of the association won't be created.";
		case 20337:
			return "??(Flattener.java) The list of subtypes of superclass '$1$' is empty.";
		case 20338:
			return "??(Flattener.java) Ignoring reflexive relationship that would be caused by property '$1$' in class '$2$'. The property will simply be removed.";
		case 20339:
			return "??(Flattener.java) No 'value' property found in <<union>> '$1$'. ONINA processing/modelling rules expect that a XxxReason <<union>> class has a 'value' property.";
		case 20340:
			return "??(Flattener.java) The type of property '$1$' in class '$2$' shall be set to the type '$3$'. That type cannot be found in the model. Setting the category of value of the property to 'unknown'.";
		case 20341:
			return "(Flattener.java) Rule '$1$' is enabled but the transformer configuration does not contain parameter '$2$' with a valid integer value greater than 1. Behavior for '$1$' will be ignored.";
		case 20342:
			return "(Flattener.java) Multiplicity flattening would usually dissolve the bi-directional association between class '$1$' (property '$2$') and class '$3$' (property '$4$'). Because the rule is to keep all bi-directional associations, the association will not be dissolved and multiplicity flattening won't be applied to it.";
		case 20343:
			return "(Flattener.java) Parameter '$1$' is required for the execution of '$2$' but has not been provided. The rule will not be applied.";
		case 20344:
			return "(Flattener.java) '$1$' matches regex '$2$', provided in parameter '$3$'";
		case 20345:
			return "(Flattener.java) '$1$' does not match regex '$2$', provided in parameter '$3$'";

		/* Generic Model related messages */
		// (30000-30099) Messages used in multiple generic model classes
		// (30100-30199) GenericAssociationInfo messages
		// (30200-30299) GenericClassInfo messages
		// (30300-30399) GenericModel messages
		// (30400-30499) GenericOclConstraint messages
		// (30500-30599) GenericPackageInfo messages
		// (30600-30699) GenericPropertyInfo messages
		// (30700-30799) GenericTextConstraint messages
		// (30800-30899) Messages from generic model element reader
		case 30200:
			return "(GenericModel.java) Duplicate property encountered. Property with name '$1$' already exists in class '$2$'. Because the duplicate property behavior is set to 'ADD' the duplicate will nevertheless be added, resulting in two properties with the same name in the class.";
		case 30201:
			return "(GenericModel.java) Duplicate property encountered. Property with name '$1$' already exists in class '$2$'. Because the duplicate property behavior is set to 'IGNORE' the duplicate will be ignored and the existing property kept. The isRestriction setting of the existing property will not be changed.";
		case 30202:
			return "(GenericModel.java) Duplicate property encountered. Property with name '$1$' already exists in class '$2$'. Because the duplicate property behavior is set to 'IGNORE_UNRESTRICT' the duplicate will be ignored and the existing property kept. In case that the existing property is a restriction, it is set to not being a restriction.";
		case 30203:
			return "(GenericModel.java) Duplicate property encountered. Property with name '$1$' already exists in class '$2$'. Because the duplicate property behavior is set to 'OVERWRITE' the duplicate/new property will overwrite the existing one.";
		case 30300:
			return "(GenericModel.java) Constraint '$1$' in Class '$2$' not of type 'GenericText/OclConstraint'.";
		case 30301:
			return "(GenericModel.java) $1$";
		case 30302:
			return "(GenericModel.java) Could not find GenericPropertyInfo to update context info with for GenericTextConstraint named '$1$'. - Context model element name is '$2$'.";
		case 30303:
			return "(GenericModel.java) Could not find GenericClassInfo to update context info with for GenericTextConstraint named '$1$'. - Context model element name is '$2$'.";
		case 30304:
			return "(GenericModel.java) Unrecognized constraint context model element type: '$1$'";
		case 30305:
			return "(GenericModel.java) Could not find GenericPropertyInfo to update context info with for GenericOclConstraint named '$1$'. - Context model element name is '$2$'.";
		case 30306:
			return "(GenericModel.java) Could not find GenericPropertyInfo to update context info with for GenericOclConstraint named '$1$'. - Context model element name is '$2$'. - Context class name is '$3$'.";
		case 30307:
			return "(GenericModel.java) Could not find GenericClassInfo to update context info with for GenericOclConstraint named '$1$'. - Context model element name is '$2$'.";
		case 30308:
			return "(GenericModel.java) Could not find GenericClassInfo to update context info with for GenericOclConstraint named '$1$'. - Context model element name is '$2$'. - Context class name is '$3$'.";
		case 30309:
			return "(GenericModel.java) Unrecognized constraint type: '$1$'.";
		case 30310:
			return "(GenericModel.java) Package '$1$' is not of type 'GenericPackageInfo'. Cannot add class '$2$'";
		case 30311:
			return "(GenericModel.java) Class '$1$' is not of type 'GenericClassInfo' (was trying to add new property '$2$').";
		case 30312:
			return "(GenericModel.java) Property $1$.$2$ is not of type 'GenericPropertyInfo' (most likely because the property belongs to a class that is not part of the selected schema). Cannot remove the property.";
		case 30313:
			return "(GenericModel.java) Class '$1$' is not of type 'GenericClassInfo' (was trying to remove property '$2$')";
		case 30314:
			return "(GenericModel.java) Class with id '$1$' not found. Cannot remove subtype '$2$'";
		case 30315:
			return "(GenericModel.java) Association class '$1$' not of type GenericClassInfo. Cannot remove it.";
		case 30316:
			return "(GenericModel.java) Class with name '$1$' and id '$2$' is not of type 'GenericClassInfo'.";
		case 30317:
			return "(GenericModel.java) Property with name '$1$' and id '$2$' is not of type 'GenericPropertyInfo'.";
		case 30318:
			return "(GenericModel.java) Property with id '$1$' (name: '$2$', in class: '$3$') already exists in the generic model (details about that property [property name / in class name]: $4$). The property will be ignored (not added to its class).";
		case 30319:
			return "(GenericModel.java) GenericPropertyInfo that should be used to represent the property '$1$' is not in the same class (property in class is '$2$', generic property in class is '$3$'). The property will not be added to class '$2$'.";
		case 30320:
			return "(GenericModel.java) PropertyInfo with sequenceNumber '$1$' in class '$2$' is null. The property will be ignored.";
		case 30321:
			return "(GenericModel.java) No GenericPropertyInfo found that represents property '$1$' in class '$2$'. The property will be ignored.";
		case 30322:
			return "(GenericModel.java) Class with name '$1$' and id '$2$' is not of type 'GenericClassInfo'. Cannot remove it from the model.";
		case 30323:
			return "(GenericModel.java) Subtype of '$1$' with name '$2$' found in the model but it is not a GenericClassInfo (likely because it does not belong to a schema selected for processing). Cannot remove the supertype relationship to '$1$' in the subtype '$2$'.";
		case 30324:
			return "(GenericModel.java) Subtype of '$1$' with id '$2$' not found in the model. Cannot remove the supertype relationship to '$1$' in the subtype.";
		case 30325:
			return "(GenericModel.java) Could not find GenericPropertyInfo to update context info with for GenericFolConstraint named '$1$'. - Context model element name is '$2$'.";
		case 30326:
			return "(GenericModel.java) Could not find GenericClassInfo to update context info with for GenericFolConstraint named '$1$'. - Context model element name is '$2$'.";

		case 30327:
			return "(Generic model) The zip file at '$1$' contains more than one entry. Only the entry '$2$' will be loaded. Other entries will be ignored.";
		case 30328:
			return "(Generic model) The zip file at '$1$' does not contain any entry. The model will be empty.";

		case 30500:
			return "(GenericPackageInfo.java) Child package '$1$' of package '$2$' is not an application schema but also not an instance of GenericPackageInfo. Cannot set the target namespace on '$3$'.";

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
