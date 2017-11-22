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

package de.interactive_instruments.ShapeChange.Target.Codelists;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.SortedSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.SerializerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ProcessingInstruction;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Target.Target;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;

public class CodelistDictionaries implements Target {

	private PackageInfo pi = null;
	private Model model = null;
	private Options options = null;
	private ShapeChangeResult result = null;
	private boolean diagnosticsOnly = false;
	private boolean printed = false;
	private boolean enums = false;
	private String gmlid = "id";
	private String identifier = "name";
	private String names = "alias,initialValue";
	private final HashMap<String, Document> documentMap = new HashMap<String, Document>();
	private String documentationTemplate = null;
	private String documentationNoValue = null;

	public void initialise(PackageInfo p, Model m, Options o,
			ShapeChangeResult r, boolean diagOnly)
			throws ShapeChangeAbortException {
		pi = p;
		model = m;
		options = o;
		result = r;
		diagnosticsOnly = diagOnly;
		String s = options.parameter(this.getClass().getName(), "enumerations");
		if (s != null && s.equalsIgnoreCase("true"))
			enums = true;

		s = options.parameter(this.getClass().getName(), "identifier");
		if (s != null && !s.isEmpty())
			identifier = s;

		s = options.parameter(this.getClass().getName(), "names");
		if (s != null && !s.isEmpty())
			names = s;

		s = options.parameter(this.getClass().getName(), "gmlid"); // TODO unit
																	// test and
																	// document,
																	// req test
																	// uniqueness,
																	// req test
																	// gmlid
																	// value
		if (s != null && !s.isEmpty())
			gmlid = s;

		// change the default documentation template?
		documentationTemplate = options.parameter(this.getClass().getName(),
				"documentationTemplate");
		documentationNoValue = options.parameter(this.getClass().getName(),
				"documentationNoValue");
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

	public void process(ClassInfo ci) {
		int cat = ci.category();
		if (cat != Options.CODELIST || ci.asDictionary() == false) {
			if (!enums || cat != Options.ENUMERATION)
				return;
		}

		Document cDocument = createDocument();

		ProcessingInstruction proci = null;
		if (options.gmlVersion.equals("3.2"))
			proci = cDocument.createProcessingInstruction("xml-stylesheet",
					"type='text/xsl' href='./CodelistDictionary-v32.xsl'");
		else if (options.gmlVersion.equals("3.1"))
			proci = cDocument.createProcessingInstruction("xml-stylesheet",
					"type='text/xsl' href='./CodelistDictionary-v31.xsl'");
		if (proci != null)
			cDocument.appendChild(proci);

		Element ec = cDocument.createElementNS(options.GML_NS, "Dictionary");
		cDocument.appendChild(ec);

		addAttribute(cDocument, ec, "xmlns:gml", options.GML_NS);
		addAttribute(cDocument, ec, "xmlns:xsi",
				"http://www.w3.org/2001/XMLSchema-instance");
		addAttribute(cDocument, ec, "xsi:schemaLocation", options.GML_NS + " "
				+ options.schemaLocationOfNamespace(options.GML_NS));
		addAttribute(cDocument, ec, "gml:id", ci.name());

		documentMap.put(ci.id(), cDocument);

		String s = ci.derivedDocumentation(documentationTemplate,
				documentationNoValue);
		if (s != null && !s.isEmpty()) {
			Element e1 = cDocument.createElementNS(options.GML_NS,
					"description");
			e1.appendChild(cDocument.createTextNode(s));
			ec.appendChild(e1);
		}

		String[] sa = identifier.split(",");
		for (String s0 : sa) {
			s = getValue(ci, s0.trim());
			if (s != null && !s.isEmpty()) {
				if (options.gmlVersion.equals("3.2")) {
					Element e1 = cDocument.createElementNS(options.GML_NS,
							"identifier");
					addAttribute(cDocument, e1, "codeSpace",
							ci.pkg().targetNamespace());
					e1.appendChild(cDocument.createTextNode(s));
					ec.appendChild(e1);
				} else if (options.gmlVersion.equals("3.1")) {
					Element e1 = cDocument.createElementNS(options.GML_NS,
							"name");
					e1.appendChild(cDocument.createTextNode(s));
					ec.appendChild(e1);
				}
				break;
			}
		}

		sa = names.split(",");
		for (String s0 : sa) {
			s = getValue(ci, s0);
			if (s != null && !s.isEmpty()) {
				Element e1 = cDocument.createElementNS(options.GML_NS, "name");
				e1.appendChild(cDocument.createTextNode(s));
				ec.appendChild(e1);
			}
		}

		for (Iterator<PropertyInfo> j = ci.properties().values().iterator(); j
				.hasNext();) {
			PropertyInfo propi = j.next();
			Element e1 = createEntry(cDocument, ci, propi, true);
			if (e1 != null)
				ec.appendChild(e1);
		}

		SortedSet<String> ts = ci.supertypes();
		for (Iterator<String> i = ts.iterator(); i.hasNext();) {
			ClassInfo cix = model.classById(i.next());
			if (cix != null) {
				for (Iterator<PropertyInfo> j = cix.properties().values()
						.iterator(); j.hasNext();) {
					PropertyInfo propi = j.next();
					Element e1 = createEntry(cDocument, ci, propi, true);
					if (e1 != null)
						ec.appendChild(e1);
				}
			}
		}

	}

	private Element createEntry(Document lDocument, ClassInfo ci,
			PropertyInfo propi, boolean local) {

		Element e = lDocument.createElementNS(options.GML_NS,
				"dictionaryEntry");
		Element e3 = lDocument.createElementNS(options.GML_NS, "Definition");
		String s = getValue(propi, gmlid);
		if (s != null && !s.isEmpty()) {
			if (!s.matches("^[a-zA-Z_]"))
				s = "_" + s;
		} else {
			s = "_" + propi.id();
		}
		addAttribute(lDocument, e3, "gml:id", "_" + propi.id());
		e.appendChild(e3);

		Element e2;
		s = propi.derivedDocumentation(documentationTemplate,
				documentationNoValue);
		if (s != null && !s.isEmpty()) {
			e2 = lDocument.createElementNS(options.GML_NS, "description");
			e2.appendChild(lDocument.createTextNode(s));
			e3.appendChild(e2);
		}

		if (options.gmlVersion.equals("3.2")) {
			e2 = lDocument.createElementNS(options.GML_NS, "identifier");
		} else {
			e2 = lDocument.createElementNS(options.GML_NS, "name");
		}

		String codeSpace = ci.taggedValue("codeList");
		if (codeSpace == null)
			codeSpace = ci.taggedValue("infoURL");
		if (codeSpace == null)
			codeSpace = ci.pkg().targetNamespace() + "/" + ci.name();
		addAttribute(lDocument, e2, "codeSpace", codeSpace);

		String[] sa = identifier.split(",");
		for (String s0 : sa) {
			s = getValue(propi, s0.trim());
			if (s != null && !s.isEmpty()) {
				e2.appendChild(lDocument.createTextNode(s));
				e3.appendChild(e2);
				break;
			}
		}

		sa = names.split(",");
		for (String s0 : sa) {
			s = getValue(propi, s0);
			if (s != null && !s.isEmpty()) {
				Element e1 = lDocument.createElementNS(options.GML_NS, "name");
				e1.appendChild(lDocument.createTextNode(s));
				e3.appendChild(e1);
			}
		}

		return e;
	}

	private String getValue(Info i, String source) {
		String s = null;

		if (source.equalsIgnoreCase("name"))
			s = i.name();
		else if (source.equalsIgnoreCase("alias"))
			s = i.aliasName();
		else if (source.equalsIgnoreCase("id"))
			s = i.id();
		else if (source.equalsIgnoreCase("initialValue")
				&& i instanceof PropertyInfo)
			s = ((PropertyInfo) i).initialValue();
		else if (source.startsWith("@"))
			s = i.taggedValue(source.substring(1));

		if (s != null && s.isEmpty())
			s = null;

		return s;
	}

	public void write() {
		if (printed) {
			return;
		}
		if (diagnosticsOnly) {
			return;
		}

		try {
			Properties outputFormat = OutputPropertiesFactory
					.getDefaultMethodProperties("xml");
			outputFormat.setProperty("indent", "yes");
			outputFormat.setProperty(
					"{http://xml.apache.org/xalan}indent-amount", "2");
			outputFormat.setProperty("encoding", "UTF-8");

			Serializer serializer = SerializerFactory
					.getSerializer(outputFormat);
			for (Iterator<ClassInfo> i = model.classes(pi).iterator(); i
					.hasNext();) {
				ClassInfo ci = i.next();
				Document cDocument = documentMap.get(ci.id());
				if (cDocument != null) {
					String dir = options.parameter(this.getClass().getName(),
							"outputDirectory");
					if (dir == null)
						dir = options.parameter("outputDirectory");
					if (dir == null)
						dir = options.parameter(".");

					File outDir = new File(dir);
					if (!outDir.exists())
						outDir.mkdirs();

					/*
					 * SO: Used OutputStreamWriter instead of FileWriter to set
					 * character encoding (see doc in Serializer.setWriter and
					 * FileWriter)
					 */
					OutputStream fout = new FileOutputStream(
							dir + "/" + ci.name() + ".xml");
					OutputStreamWriter outputXML = new OutputStreamWriter(fout,
							outputFormat.getProperty("encoding"));
					serializer.setWriter(outputXML);
					serializer.asDOMSerializer().serialize(cDocument);
					outputXML.close();
					result.addResult(getTargetName(), dir, ci.name() + ".xml",
							ci.qname());
				}
			}
		} catch (Exception e) {
			String m = e.getMessage();
			if (m != null) {
				result.addError(m);
			}
			e.printStackTrace(System.err);
		}

		printed = true;
	}

	@Override
	public String getTargetName() {
		return "Code List Dictionary";
	}
}
