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
import javax.xml.transform.OutputKeys;
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

    public static final String SCRS_NS = "http://www.interactive-instruments.de/ShapeChange/Result";

    // Data
    public Document document = null;
    protected Element root = null;
    protected Element messages = null;
    protected Element resultFiles = null;
    protected Set<Element> resultElements = new HashSet<Element>();
    protected Properties outputFormat = OutputPropertiesFactory.getDefaultMethodProperties("xml");
    protected Options options = null;
    protected boolean fatalErrorReceived = false;

    protected HashSet<String> duplicateMessageCheck;

    protected static boolean printDateTime = false;
    protected static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

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
	public MessageContext(ShapeChangeResult result, String level, String mtext) {
	    this.result = result;
	    this.level = level;
	    
	    String systemErrMsgPrefix = level.startsWith("ProcessFlow") ? "PF-"+level.substring(11,12) : level.substring(0, 1);

	    System.err.println(systemErrMsgPrefix + " " + (printDateTime ? dateTime() + " " : "") + mtext);
	    message = result.document.createElementNS(SCRS_NS, level);
	    result.messages.appendChild(message);
	    message.setAttribute("message", (printDateTime ? dateTime() + " " : "") + mtext);

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
	public void addDetail(MessageSource ms, int mnr, String p1, String p2, String p3, String p4) {
	    String m = ms == null ? result.message(mnr) : ms.message(mnr);
	    addDetail(m.replace("$1$", p1).replace("$2$", p2).replace("$3$", p3).replace("$4$", p4));
	}

	public void addDetail(MessageSource ms, int mnr, String p1, String p2, String p3) {
	    String m = ms == null ? result.message(mnr) : ms.message(mnr);
	    addDetail(m.replace("$1$", p1).replace("$2$", p2).replace("$3$", p3));
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
	    dbf.setAttribute(Options.JAXP_SCHEMA_LANGUAGE, Options.W3C_XML_SCHEMA);
	    DocumentBuilder db = dbf.newDocumentBuilder();
	    document = db.newDocument();

	    root = document.createElementNS(SCRS_NS, "ShapeChangeResult");
	    document.appendChild(root);
	    root.setAttribute("resultCode", "0");
	    root.setAttribute("xmlns:r", SCRS_NS);
	    root.setAttribute("start", (new Date()).toString());

	    String version = "[dev]";
	    InputStream stream = getClass().getResourceAsStream("/sc.properties");
	    if (stream != null) {
		Properties properties = new Properties();
		properties.load(stream);
		version = properties.getProperty("sc.version");
	    }
	    root.setAttribute("version", version);

	    messages = document.createElementNS(SCRS_NS, "Messages");
	    root.appendChild(messages);

	    resultFiles = document.createElementNS(SCRS_NS, "Results");
	    root.appendChild(resultFiles);

	} catch (ParserConfigurationException e) {
	    System.err.println("Bootstrap Error: XML parser was unable to be configured.");
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
	outputFormat.setProperty("{http://xml.apache.org/xalan}indent-amount", "2");
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

    public MessageContext addDebug(MessageSource ms, int mnr, String p1, String p2, String p3, String p4) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addDebug(m.replace("$1$", p1).replace("$2$", p2).replace("$3$", p3).replace("$4$", p4));
    }

    public MessageContext addDebug(MessageSource ms, int mnr, String p1, String p2, String p3) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addDebug(m.replace("$1$", p1).replace("$2$", p2).replace("$3$", safe(p3)));
    };

    public MessageContext addDebug(MessageSource ms, int mnr, String p1, String p2) {
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
	if (document == null || !options.parameter("reportLevel").equals("DEBUG")) {
	    return null;
	}
	if (m.startsWith("??")) {
	    m = m.substring(2);
	    if (!duplicateMessageCheck.add("D " + m))
		return null;
	}
	return new MessageContext(this, "Debug", m);
    }
    
    public MessageContext addProcessFlowDebug(MessageSource ms, int mnr, String p1, String p2, String p3, String p4) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addProcessFlowDebug(m.replace("$1$", p1).replace("$2$", p2).replace("$3$", p3).replace("$4$", p4));
    }

    public MessageContext addProcessFlowDebug(MessageSource ms, int mnr, String p1, String p2, String p3) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addProcessFlowDebug(m.replace("$1$", p1).replace("$2$", p2).replace("$3$", safe(p3)));
    };

    public MessageContext addProcessFlowDebug(MessageSource ms, int mnr, String p1, String p2) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addProcessFlowDebug(m.replace("$1$", p1).replace("$2$", p2));
    };

    public MessageContext addProcessFlowDebug(MessageSource ms, int mnr, String p1) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addProcessFlowDebug(m.replace("$1$", p1));
    };

    public MessageContext addProcessFlowDebug(MessageSource ms, int mnr) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addProcessFlowDebug(m);
    };

    public MessageContext addProcessFlowDebug(String m) {
	if (document == null || !options.parameterAsString(null, "processFlowReportLevel", "INFO", false, true).equals("DEBUG")) {
	    return null;
	}
	if (m.startsWith("??")) {
	    m = m.substring(2);
	    if (!duplicateMessageCheck.add("PF-D " + m))
		return null;
	}
	return new MessageContext(this, "ProcessFlowDebug", m);
    }

    public MessageContext addInfo(MessageSource ms, int mnr, String p1, String p2, String p3, String p4) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addInfo(m.replace("$1$", p1).replace("$2$", p2).replace("$3$", p3).replace("$4$", p4));
    }

    public MessageContext addInfo(MessageSource ms, int mnr, String p1, String p2, String p3) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addInfo(m.replace("$1$", p1).replace("$2$", p2).replace("$3$", p3));
    };

    public MessageContext addInfo(MessageSource ms, int mnr, String p1, String p2) {
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

    public MessageContext addProcessFlowInfo(MessageSource ms, int mnr, String p1, String p2, String p3, String p4) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addProcessFlowInfo(m.replace("$1$", p1).replace("$2$", p2).replace("$3$", p3).replace("$4$", p4));
    }

    public MessageContext addProcessFlowInfo(MessageSource ms, int mnr, String p1, String p2, String p3) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addProcessFlowInfo(m.replace("$1$", p1).replace("$2$", p2).replace("$3$", p3));
    };

    public MessageContext addProcessFlowInfo(MessageSource ms, int mnr, String p1, String p2) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addProcessFlowInfo(m.replace("$1$", p1).replace("$2$", p2));
    };

    public MessageContext addProcessFlowInfo(MessageSource ms, int mnr, String p1) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addProcessFlowInfo(m.replace("$1$", p1));
    };

    public MessageContext addProcessFlowInfo(MessageSource ms, int mnr) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addProcessFlowInfo(m);
    };

    public MessageContext addProcessFlowInfo(String m) {
	String l = options.parameterAsString(null, "processFlowReportLevel", "INFO", false, true);
	if (document == null || !l.equals("DEBUG") && !l.equals("INFO")) {
	    return null;
	}
	if (m.startsWith("??")) {
	    m = m.substring(2);
	    if (!duplicateMessageCheck.add("PF-I " + m))
		return null;
	}
	return new MessageContext(this, "ProcessFlowInfo", m);
    }

    public MessageContext addWarning(MessageSource ms, int mnr, String p1, String p2, String p3, String p4) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addWarning(m.replace("$1$", p1).replace("$2$", p2).replace("$3$", p3).replace("$4$", p4));
    }

    public MessageContext addWarning(MessageSource ms, int mnr, String p1, String p2, String p3) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addWarning(m.replace("$1$", p1).replace("$2$", p2).replace("$3$", p3));
    };

    public MessageContext addWarning(MessageSource ms, int mnr, String p1, String p2) {
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
	if (document == null || !l.equals("DEBUG") && !l.equals("INFO") && !l.equals("WARNING")) {
	    return null;
	}
	if (m.startsWith("??")) {
	    m = m.substring(2);
	    if (!duplicateMessageCheck.add("W " + m))
		return null;
	}
	return new MessageContext(this, "Warning", m);
    }
    
    public MessageContext addProcessFlowWarning(MessageSource ms, int mnr, String p1, String p2, String p3, String p4) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addProcessFlowWarning(m.replace("$1$", p1).replace("$2$", p2).replace("$3$", p3).replace("$4$", p4));
    }

    public MessageContext addProcessFlowWarning(MessageSource ms, int mnr, String p1, String p2, String p3) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addProcessFlowWarning(m.replace("$1$", p1).replace("$2$", p2).replace("$3$", p3));
    };

    public MessageContext addProcessFlowWarning(MessageSource ms, int mnr, String p1, String p2) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addProcessFlowWarning(m.replace("$1$", p1).replace("$2$", p2));
    };

    public MessageContext addProcessFlowWarning(MessageSource ms, int mnr, String p1) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addProcessFlowWarning(m.replace("$1$", p1));
    };

    public MessageContext addProcessFlowWarning(MessageSource ms, int mnr) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addProcessFlowWarning(m);
    };

    public MessageContext addProcessFlowWarning(String m) {
	String l = options.parameterAsString(null, "processFlowReportLevel", "INFO", false, true);
	if (document == null || !l.equals("DEBUG") && !l.equals("INFO") && !l.equals("WARNING")) {
	    return null;
	}
	if (m.startsWith("??")) {
	    m = m.substring(2);
	    if (!duplicateMessageCheck.add("PF-W " + m))
		return null;
	}
	return new MessageContext(this, "ProcessFlowWarning", m);
    }

    public MessageContext addError(MessageSource ms, int mnr, String p1, String p2, String p3, String p4, String p5,
	    String p6, String p7) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addError(m.replace("$1$", p1).replace("$2$", p2).replace("$3$", p3).replace("$4$", p4).replace("$5$", p5)
		.replace("$6$", p6).replace("$7$", p7));
    }

    public MessageContext addError(MessageSource ms, int mnr, String p1, String p2, String p3, String p4) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addError(m.replace("$1$", p1).replace("$2$", p2).replace("$3$", p3).replace("$4$", p4));
    }

    public MessageContext addError(MessageSource ms, int mnr, String p1, String p2, String p3) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addError(m.replace("$1$", p1).replace("$2$", p2).replace("$3$", p3));
    };

    public MessageContext addError(MessageSource ms, int mnr, String p1, String p2) {
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
    
    public MessageContext addProcessFlowError(MessageSource ms, int mnr, String p1, String p2, String p3, String p4, String p5,
	    String p6, String p7) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addProcessFlowError(m.replace("$1$", p1).replace("$2$", p2).replace("$3$", p3).replace("$4$", p4).replace("$5$", p5)
		.replace("$6$", p6).replace("$7$", p7));
    }

    public MessageContext addProcessFlowError(MessageSource ms, int mnr, String p1, String p2, String p3, String p4) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addProcessFlowError(m.replace("$1$", p1).replace("$2$", p2).replace("$3$", p3).replace("$4$", p4));
    }

    public MessageContext addProcessFlowError(MessageSource ms, int mnr, String p1, String p2, String p3) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addProcessFlowError(m.replace("$1$", p1).replace("$2$", p2).replace("$3$", p3));
    };

    public MessageContext addProcessFlowError(MessageSource ms, int mnr, String p1, String p2) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addProcessFlowError(m.replace("$1$", p1).replace("$2$", p2));
    };

    public MessageContext addProcessFlowError(MessageSource ms, int mnr, String p1) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addProcessFlowError(m.replace("$1$", p1));
    };

    public MessageContext addProcessFlowError(MessageSource ms, int mnr) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addProcessFlowError(m);
    };

    public MessageContext addProcessFlowError(String m) {
	if (document == null) {
	    return null;
	}
	if (m.startsWith("??")) {
	    m = m.substring(2);
	    if (!duplicateMessageCheck.add("PF-E " + m))
		return null;
	}
	return new MessageContext(this, "ProcessFlowError", m);
    }

    public MessageContext addFatalError(MessageSource ms, int mnr, String p1, String p2, String p3, String p4) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addFatalError(m.replace("$1$", p1).replace("$2$", p2).replace("$3$", p3).replace("$4$", p4));
    }

    public MessageContext addFatalError(MessageSource ms, int mnr, String p1, String p2, String p3) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addFatalError(m.replace("$1$", p1).replace("$2$", p2).replace("$3$", p3));
    };

    public MessageContext addFatalError(MessageSource ms, int mnr, String p1, String p2) {
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
    
    public MessageContext addProcessFlowFatalError(MessageSource ms, int mnr, String p1, String p2, String p3, String p4) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addProcessFlowFatalError(m.replace("$1$", p1).replace("$2$", p2).replace("$3$", p3).replace("$4$", p4));
    }

    public MessageContext addProcessFlowFatalError(MessageSource ms, int mnr, String p1, String p2, String p3) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addProcessFlowFatalError(m.replace("$1$", p1).replace("$2$", p2).replace("$3$", p3));
    };

    public MessageContext addProcessFlowFatalError(MessageSource ms, int mnr, String p1, String p2) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addProcessFlowFatalError(m.replace("$1$", p1).replace("$2$", p2));
    };

    public MessageContext addProcessFlowFatalError(MessageSource ms, int mnr, String p1) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	return addProcessFlowFatalError(m.replace("$1$", p1));
    };

    public MessageContext addProcessFlowFatalError(MessageSource ms, int mnr) {
	String m = ms == null ? message(mnr) : ms.message(mnr);
	MessageContext ex = addProcessFlowFatalError(m);
	setResultCode(1);
	return ex;
    };

    public MessageContext addProcessFlowFatalError(String m) {

	fatalErrorReceived = true;

	if (document == null) {
	    return null;
	}
	if (m.startsWith("??")) {
	    m = m.substring(2);
	    if (!duplicateMessageCheck.add("PF-F " + m))
		return null;
	}
	return new MessageContext(this, "ProcessFlowFatalError", m);
    }

    public void addResult(String targetName, String dname, String fname, String scope) {
	if (document == null) {
	    return;
	}
	Element resfile = document.createElementNS(SCRS_NS, "Result");
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
     * Copies 'Result' elements with the URI of the given original file as 'href'
     * attribute, and sets the URI of the given new file as 'href' and text content
     * of these copies. 'scope' and 'target' will be kept as-is.
     * 
     * @param originalFile tbd
     * @param newFile      tbd
     */
    public void copyResultAndUpdateFileReference(File originalFile, File newFile) {

	if (document == null) {
	    return;
	}

	String originalFilePath = originalFile.toURI().toASCIIString();
	String newFilePath = newFile.toURI().toASCIIString();

	List<Element> newResultElements = new ArrayList<Element>();

	for (Element resultElementForOriginalFile : this.resultElements) {

	    if (resultElementForOriginalFile.getAttribute("href").equals(originalFilePath)) {

		Element resultElementForNewFile = document.createElementNS(SCRS_NS, "Result");

		// append the new 'Result' element after resultE
		resultFiles.insertBefore(resultElementForNewFile, resultElementForOriginalFile.getNextSibling());

		resultElementForNewFile.setAttribute("target", resultElementForOriginalFile.getAttribute("target"));

		resultElementForNewFile.setAttribute("href", newFilePath);

		if (resultElementForOriginalFile.hasAttribute("scope")) {
		    resultElementForNewFile.setAttribute("scope", resultElementForOriginalFile.getAttribute("scope"));
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

	    BufferedWriter outputXML = new BufferedWriter(
		    new OutputStreamWriter(new FileOutputStream(filename), "UTF-8"));
	    Serializer serializer = SerializerFactory.getSerializer(outputFormat);
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
		    xsltSource = new StreamSource(urlConnection.getInputStream());
		} else {
		    InputStream stream = getClass().getResourceAsStream("/xslt/result.xsl");
		    if (stream == null) {
			// get it from the file system
			File xsltFile = new File(xsltfileName);
			if (!xsltFile.canRead()) {
			    throw new Exception("Cannot read " + xsltFile.getName());
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
		BufferedWriter outputHTML = new BufferedWriter(
			new OutputStreamWriter(new FileOutputStream(outHTML), "UTF-8"));
		;

		if (xsltSource != null) {
		    Source xmlSource = new DOMSource(document);
		    Result res = new StreamResult(outputHTML);

		    TransformerFactory transFact = TransformerFactory.newInstance();
		    Transformer trans = transFact.newTransformer(xsltSource);
		    trans.setOutputProperty(OutputKeys.INDENT, "yes");
		    trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		    trans.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "2");
		    trans.transform(xmlSource, res);

		    /*
		     * Apparently, the following is necessary to close streams appropriately when
		     * running ShapeChange in a separate process that was spawned by another process
		     * (in the given case, a server application):
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
	 * NOTE: A leading ?? in a message text suppresses multiple appearance of a
	 * message in the output.
	 */
	switch (mnr) {
//	case 1:
//	    return "Unable to get a document builder factory.";
//	case 2:
//	    return "XML Parser was unable to be configured.";
//	case 3:
//	    return "Invalid XMI file.";
//	case 5:
//	    return "Exactly only element <XMI> expected.";


//	case 8:
//	    return "Exactly one element <XMI.metamodel> expected.";
	case 9: // used by supertype class
	    return "Class '$1$' is not associated with a package.";
	case 11: // used by supertype class
	    return "Could not establish a category for element '$1$', having well-known stereotype <<$2$>> and encoding rule '$3$'; setting category to 'unknown'.";
	

//	case 12:
//	    return "No application schema found.";
//	case 13:
//	    return "Application schema '$1$' not found.";
//	case 16:
//	    return "The XMI file is not associated with a DTD. The DTD is required for validating and processing the XMI file.";
	case 18: //xx
	    return "Unsupported Java version: '$1$'. Java 11 or higher required.";
	case 19: //AAATools Katalog.java
	    return "Model object could not be instantiated: '$1$'.";
	case 20: //AAATools Katalog.java
	    return "Model object could not be accessed: '$1$'.";
	case 21: // used in supertype class
	    return "??Rule name '$1$' is not valid. The rule is ignored.";
	case 22: //AAATools Katalog.java
	    return "??Reference model '$1$' could not be loaded and is ignored.";
//	case 23:
//	    return "Could not create temporary directory for ShapeChange run with read/write access at: $1$.";

	case 30: //xxxxxx
	    return "Enterprise Architect repository cannot be opened. File name or connection string is: '$2$', EA message is: '$1$'";
	case 31: //xxxx
	    return "Enterprise Architect repository file named '$1$' not found";
	case 32: //xx
	    return "Could not create directory $1$";
//	case 37:
//	    return "??Loaded SBVR rules from excel spreadsheet for schema '$1$'.";
	case 38: //xxx
	    return "Encountered First Order Logic constraint with source type '$1$', parsing of which is not supported yet. A First Order Logic expression cannot be created.";
	case 39: //xxx
	    return "Context: constraint '$1$' in class '$2$'.";

//	case 40:
//	    return "Microsoft Access Database named '$2$' cannot be opened. JDBC message is: '$1$'";
//	case 41:
//	    return "Microsoft Access Database file named '$1$' not found";
//	case 42:
//	    return "Error reading from Microsoft Access Database '$2$'.  Error message is: '$1$'";
	
	case 50: // used in StereotypeNormalizer - in static context
	    return "Element '$1$' has the following stereotype(s) in the input model: '$2$'.";
	case 51: // used in StereotypeNormalizer - in static context
	    return "Stereotype '$2$' of element '$1$' normalized to '$3$'.";
	case 52: // used in StereotypeNormalizer - in static context
	    return "Well-known stereotype '$2$' added to element '$1$'.";
	case 53: // used in StereotypeNormalizer - in static context
	    return "No well-known stereotype found for stereotype '$2$' of element '$1$'.";
	case 54: // used in StereotypeNormalizer - in static context
	    return "Element '$1$' has $2$ (well-known or additional) stereotype(s): '$3$'";
	case 56: // used in StereotypeNormalizer - in static context
	    return "No well-known stereotype found for stereotype '$2$' of element '$1$', but due to input parameter 'addStereotypes' all stereotypes are allowed. Thus stereotype '$2$' is added to element '$1$'.";
	case 57: // used in StereotypeNormalizer - in static context
	    return "No well-known stereotype found for stereotype '$2$' of element '$1$', but due to input parameter 'addStereotypes' that stereotype is allowed. Thus stereotype '$2$' is added to element '$1$'.";
	
	case 100: //xxxxxxx
	    return "??The '$1$' with ID '$2$' has no name. The ID is used instead.";
	case 101: // used by supertype class
	    return "??Application schema '$1$' with ID '$2$' is not associated with an XML Schema document. A default name is used: '$3$'.";
	case 104: // used by supertype class
	    return "??The supertypes of class '$1$' are of different categories or the stereotype of the class cannot be determined. This is not supported, the class is ignored.";
	case 105: // used by supertype class
	    return "??The restriction of UML attribute '$1$' in class '$2$' is not legal. The lower multiplicity limit is smaller than in the supertype '$3$'.";
	case 106: // used by supertype class
	    return "??The restriction of UML attribute '$1$' in class '$2$' is not legal. The upper multiplicity limit is higher than in the supertype '$3$'.";
	case 107: //xx
	    return "??The property '$1$' in class '$2$' has a sequence number that is already in use for another property ($3$) which will be overwritten.";
	case 108: // used by supertype class
	    return "??The class '$1$' is modelled as a feature or data type, but has at least one supertype of a different category. The supertype is ignored.";
	case 109: // used by supertype class
	    return "??The class '$1$' is modelled as a feature or data type, but has more than one supertype of the same kind. All but one (arbitrary) supertypes are ignored.";
	case 110: //xxxx
	    return "??A target could not be created for schema '$1$'. This target is supported only for GML versions 3.2 and later.";
	case 111: // used in Main - in static context
	    return "??Missing argument to '$1$' option.";
	case 115: // used by supertype class
	    return "??The class '$1$' is modelled as an interface, but has supertypes that are instantiable. The supertype relationships are ignored.";
	case 117: //xx
	    return "??No XML Schema type for type '$1$' is defined. Only object and data types are supported.";
	case 121: //xx
	    return "Base type '$1$' could not be mapped. Missing base type in complex type '$2$'.";
	case 125: // used by supertype class
	    return "The class '$1$' is an enumeration. Generalization relationships are not supported for these classes. All such relationships are ignored.";
	case 126: //xx
	    return "Failed to create enumeration type '$1$'.";
	case 127: // used by supertype class
	    return "The class '$1$' is a codelist. Generalization relationships are not supported for these classes. All such relationships are ignored.";
	case 131: //xxxx
	    return "??The type '$2$' of property '$1$' was not found.";
	case 132: //xx
	    return "The class '$1$' is referenced, but is not part of any schema in the model nor is it mapped to a well-known XML Schema type. The class is ignored.";
	case 133: //xxx
	    return "One or more errors encountered in OCL constraint in $3$ '$1$' : '$2$' ...";
	case 134: //xxx
	    return "Line/column(s) $1$: $2$";
	case 135: //xxx
	    return "??The type of property '$1$' was not found by id, only by name (fixed broken type definition).";
	case 136: //xx
	    return "The enumeration element with ID '$1$' in class '$2$' contains an empty string as value.";
	case 139: //xxx
	    return "Cannot add properties of type '$1' in schema definitions since the type definition is not part of the model.";
	case 140: // used by supertype class
    	    return "Unknown value for $1: $2";
    	case 142: // used by supertype class
	    return "Class '$1$' cannot be suppressed, as no direct or indirect, non-abstract supertype exists that is not suppressed.";
	case 143: // used by supertype class
	    return "Class '$1$' cannot be suppressed, as it has a non-suppressed subtype '$2$'.";
	case 144: // used by supertype class
	    return "Class '$1$' cannot be suppressed, as it add at least one property.";
	case 146: // used by supertype class
	    return "??Schema '$1$' is missing tagged value '$2$'.";
	case 147: // used by supertype class
	    return "??Package '$1$' has tagged value '$2$', but is not an application schema.";
	case 148: // used by supertype class
    	    return "??Property '$2$' of class '$1$' is not a composition, but has a data type as its value: '$3$'.";
	case 149: //xx
	    return "??Name of '$1$' '$2$' includes invalid characters.";
	case 150: // used by supertype class
	    return "??Schema '$1$' has dummy tagged value '$2$': '$3$'.";
	case 151: // used by supertype class
	    return "??Documentation of schema '$1$' is missing the separator '$2$'.";
	case 152: // used by supertype class
	    return "??Documentation of class '$1$' is missing the separator '$2$'.";
	case 153: // used by supertype class
	    return "??Documentation of property '$1$' is missing the separator '$2$'.";
	case 159: // used by supertype class
	    return "??Package '$1$' has a dependency, but is not an application schema.";
	case 160: // used by supertype class
	    return "??Package '$1$' is the supplier of a dependency to package '$2$', but is not an application schema.";
	case 161: // used by supertype classes
	    return "??$1$ with id '$2$' is referenced, but cannot be found.";
	case 162: // used by supertype class
	    return "??XML Schema document name '$1$' is used for more than one schema package.";
	case 163: // used by supertype class
	    return "??Class name '$1$' is used more than once in application schema '$2$'.";
	case 164: // used by supertype class
	    return "??Rule '$1$' is unknown, but referenced in the ShapeChange source code. This is a system error.";
	case 167: // used by supertype class
	    return "XML Schema document name '$1$' does not contain the .xsd file extension.";
	case 168: // used by supertype class
	    return "??The property '$1$' in class '$2$' has no sequence number.";
	case 171: //xx
	    return "XML Schema document with name '$1$' could not be created, invalid filename.";
	case 181: // used by supertype class
	    return "??Encoding rule $1$ is specified as default encoding rule for platform $2$ but is not configured.";

	case 200: // used by supertype class
	    return "??Tagged value '$1$' missing in class '$2$'.";
	case 201: // used by supertype class
	    return "??Tagged value '$1$' has incorrect value '$3$' in class '$2$'.";
	case 203: // used by supertype class
	    return "??Tagged value '$1$' has incorrect value '$3$' in property '$2$'.";
	case 204: // used by supertype class
	    return "??Outdated tagged value '$1$' used in property '$2$'.";

	case 301: //xxx
	    return "File '$1$' is not readable, processing of $2$ is skipped.";
	case 304: //xx
	    return "Error while transforming '$1$'. Message: $2$";
	case 307: //xx
	    return "File '$1$' is not writable, processing of $2$ is skipped.";
	
	case 400: //xxxxxxxxxx
	    return "Context: $1$ '$2$'";

	// 600 - 699 Messages known to be used by multiple targets
	case 600: //xxxxxx
	    return "File could not be deleted. Exception message: '$1$'.";
	case 601: //xxxx
	    return "Directory named '$1$' does not exist or is not accessible.";

	/*
	 * 700-799 Messages used by InfoImpl (which should not define its own message()
	 * method, since subtypes may override that method and then the messages defined
	 * in InfoImpl would not be visible - or it would be tedious to ensure that
	 * subtypes use message numbers not defined by InfoImpl and call
	 * InfoImpl.message() to see if a code is covered there.
	 */
	case 701: // used by supertype class
	    return "A single value was requested for tag '$1$', but in addition to returned value '$2$', an additional value '$3$' exists and is ignored.";
	case 702: // used by supertype class
	    return "A single value was requested for tag '$1$' in language '$2$', but in addition to returned value '$3$', an additional value '$4$' exists and is ignored.";
	case 704: // used by supertype class
	    return "??Descriptor '$1$' is a single-valued descriptor, but in addition to returned value '$2$' a value '$3$' exists and is ignored.";
	case 790: // used by supertype class
	    return "Context: class InfoImpl. Element: $1$. Name: $2$";
	case 791: // used by supertype class
	    return "Context: class InfoImpl (subtype: PropertyInfo). Name: $1$. In class: $2$";

	case 1002: // used by supertype class
	    return "Restriction of property '$1$' in class '$2$' from supertype '$3$'.";
	case 1003: //xxx
	    return "The multiplicity value of '$1$' is neither a number nor a known string. '*' is used instead.";
	case 1004: // used by supertype class
	    return "Class '$1$' has an unknown category, an object is assumed.";
	
	case 10003: // used by supertype class
	    return "Checked class '$1$', category '$2$', result '$3$'"; //x
	case 10004: // used by supertype class
	    return "Checking overloading. Class = '$1$'; current class = '$2$'.";
	case 10005: //xxx
	    return "Generating GML dictionaries with definitions for application schema '$1$'.";
	case 10006: //xx
	    return "Processing OCL constraint in class '$1$': '$2$'.";
	case 10013: //xxxxx
	    return "The '$1$' with ID '$2$' and name '$3$' was created."; //x
	case 10023: //xx
	    return "Processing local properties of class '$1$'.";
	case 10024: //xx
	    return "OCL syntax tree: '$1$'";
	case 10025: //xx
	    return "OCL comment: '$1$'";
	    
	case 20103: //xxxxxxxxxx
	    return "---------- now processing: $1$ ----------";

	// common profiling related messages

	case 20216: //xxx
	    return "Context: model element: '$1$'";
	case 20217: //xxx
	    return "Context: parsing message: '$1$'";
	case 20218: //xxx
	    return "Context: profiles string: '$1$'";

	case 30800: //xxxxxxxxxx
	    return "(Generic model element reader) Unexpected start element found by $1$: '$2$'.";
	case 30801: //xxxxxxxxxxx
	    return "(Generic model element reader) Unexpected end element found by $1$: '$2$'.";
	case 30804: //x
	    return "(Generic model element reader) SCXML producer: $1$, version: $2$";

	case 1000000: // used by supertype class
	    return "Unrecognized parameter found: '$1$'. The parameter may have no effect on processing. Did you mean '$2$'?";
	case 1000001: // used by supertype class
	    return "Unrecognized parameter found: '$1$'. The parameter may have no effect on processing.";
	    
	default:
	    return "(" + ShapeChangeResult.class.getName() + ") Unknown message with number: " + mnr;
	}
    }
}
