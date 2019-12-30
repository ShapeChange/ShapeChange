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
import java.util.List;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.SerializerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ProcessingInstruction;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.RuleRegistry;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.Target;

/**
 * @author Stefan Olk
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
public class CodelistDictionaries implements Target, MessageSource {

	public static final String PARAM_IDENTIFIER = "identifier";
	public static final String PARAM_NAMES = "names";
	public static final String PARAM_GMLID = "gmlid";
	public static final String PARAM_ENUMERATIONS = "enumerations";

	private PackageInfo pi = null;
	private Model model = null;
	private Options options = null;
	private ShapeChangeResult result = null;

	private boolean diagnosticsOnly = false;
	private boolean printed = false;

	private boolean enums = false;
	private String gmlid = "id";
	private List<String> identifiers = null;
	private List<String> names = null;

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

		enums = options.parameterAsBoolean(this.getClass().getName(),
				PARAM_ENUMERATIONS, false);
		identifiers = options.parameterAsStringList(this.getClass().getName(),
				PARAM_IDENTIFIER, new String[] { "name" }, true, true);
		names = options.parameterAsStringList(this.getClass().getName(),
				PARAM_NAMES, new String[] { "alias", "initialValue" }, true,
				true);
		gmlid = options.parameterAsString(this.getClass().getName(),
				PARAM_GMLID, "id", false, false);

		documentationTemplate = options.parameter(this.getClass().getName(),
				"documentationTemplate");
		documentationNoValue = options.parameter(this.getClass().getName(),
				"documentationNoValue");
	}

	/**
	 * Add attribute to an element
	 * @param document  tbd
	 * @param e  tbd
	 * @param name  tbd
	 * @param value  tbd
	 */
	protected void addAttribute(Document document, Element e, String name,
			String value) {

		Attr att = document.createAttribute(name);
		att.setValue(value);
		e.setAttributeNode(att);
	}

	public void process(ClassInfo ci) {

		int cat = ci.category();
		if (cat != Options.CODELIST && (!enums || cat != Options.ENUMERATION)) {
			return;
		} else if (cat == Options.CODELIST && ci.asDictionary() == false) {
			result.addInfo(this, 101, ci.name());
			return;
		}

		try {

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document cDocument = db.newDocument();

			ProcessingInstruction proci = null;
			if (options.gmlVersion.equals("3.2")) {
				proci = cDocument.createProcessingInstruction("xml-stylesheet",
						"type='text/xsl' href='./CodelistDictionary-v32.xsl'");
			} else if (options.gmlVersion.equals("3.1")) {
				proci = cDocument.createProcessingInstruction("xml-stylesheet",
						"type='text/xsl' href='./CodelistDictionary-v31.xsl'");
			}
			if (proci != null) {
				cDocument.appendChild(proci);
			}

			Element ec = cDocument.createElementNS(options.GML_NS,
					"gml:Dictionary");
			cDocument.appendChild(ec);

			addAttribute(cDocument, ec, "xmlns:gml", options.GML_NS);
			addAttribute(cDocument, ec, "xmlns:xsi",
					"http://www.w3.org/2001/XMLSchema-instance");
			addAttribute(cDocument, ec, "xsi:schemaLocation", options.GML_NS
					+ " " + options.schemaLocationOfNamespace(options.GML_NS));
			addAttribute(cDocument, ec, "gml:id", ci.name());

			documentMap.put(ci.id(), cDocument);

			String documentation = ci.derivedDocumentation(
					documentationTemplate, documentationNoValue);
			if (StringUtils.isNotBlank(documentation)) {
				Element e1 = cDocument.createElementNS(options.GML_NS,
						"gml:description");
				e1.appendChild(cDocument.createTextNode(documentation));
				ec.appendChild(e1);
			}

			for (String identifierSource : identifiers) {

				String identifierValue = getValue(ci, identifierSource);

				if (StringUtils.isNotBlank(identifierValue)) {

					if (options.gmlVersion.equals("3.2")) {

						Element e1 = cDocument.createElementNS(options.GML_NS,
								"gml:identifier");
						addAttribute(cDocument, e1, "codeSpace",
								ci.pkg().targetNamespace());
						e1.appendChild(
								cDocument.createTextNode(identifierValue));
						ec.appendChild(e1);

					} else if (options.gmlVersion.equals("3.1")) {

						Element e1 = cDocument.createElementNS(options.GML_NS,
								"gml:name");
						e1.appendChild(
								cDocument.createTextNode(identifierValue));
						ec.appendChild(e1);
					}

					break;
				}
			}

			for (String nameSource : names) {

				String nameValue = getValue(ci, nameSource);

				if (StringUtils.isNotBlank(nameValue)) {

					Element e1 = cDocument.createElementNS(options.GML_NS,
							"gml:name");
					e1.appendChild(cDocument.createTextNode(nameValue));
					ec.appendChild(e1);
				}
			}

			/*
			 * Add properties from ci as well as all its supertypes in the
			 * complete supertype hierarchy.
			 */

			SortedMap<String, Element> entryElmtsByGmlId = new TreeMap<>();

			for (PropertyInfo propi : ci.propertiesAll()) {

				Element entryElmt = createEntry(cDocument, ci, propi, true);

				if (entryElmt != null) {
					String gmlid = ((Element) entryElmt
							.getElementsByTagName("gml:Definition").item(0))
									.getAttribute("gml:id");
					entryElmtsByGmlId.put(gmlid, entryElmt);
				}
			}

			for (Element defElmt : entryElmtsByGmlId.values()) {
				ec.appendChild(defElmt);
			}

		} catch (ParserConfigurationException e) {

			result.addFatalError(null, 2);
			String m = e.getMessage();
			if (m != null) {
				result.addFatalError(m);
			}
			e.printStackTrace(System.err);

		} catch (Exception e) {

			String m = e.getMessage();
			if (m != null) {
				result.addFatalError(m);
			}
			e.printStackTrace(System.err);
		}

	}

	private Element createEntry(Document lDocument, ClassInfo ci,
			PropertyInfo propi, boolean local) {

		Element e = lDocument.createElementNS(options.GML_NS,
				"gml:dictionaryEntry");
		Element e3 = lDocument.createElementNS(options.GML_NS,
				"gml:Definition");

		String gmlIdValue = getValue(propi, gmlid);

		if (StringUtils.isNotBlank(gmlIdValue)) {
			if (!gmlIdValue.matches("^[a-zA-Z_].*")) {
				gmlIdValue = "_" + gmlIdValue;
			}
		} else {
			gmlIdValue = "_" + propi.id();
		}

		addAttribute(lDocument, e3, "gml:id", gmlIdValue);
		e.appendChild(e3);

		Element e2;
		String defDescription = propi.derivedDocumentation(
				documentationTemplate, documentationNoValue);
		if (StringUtils.isNotBlank(defDescription)) {
			e2 = lDocument.createElementNS(options.GML_NS, "gml:description");
			e2.appendChild(lDocument.createTextNode(defDescription));
			e3.appendChild(e2);
		}

		if (options.gmlVersion.equals("3.2")) {
			e2 = lDocument.createElementNS(options.GML_NS, "gml:identifier");
		} else {
			e2 = lDocument.createElementNS(options.GML_NS, "gml:name");
		}

		String codeSpace = ci.taggedValue("codeList");
		if (codeSpace == null)
			codeSpace = ci.taggedValue("infoURL");
		if (codeSpace == null)
			codeSpace = ci.pkg().targetNamespace() + "/" + ci.name();

		addAttribute(lDocument, e2, "codeSpace", codeSpace);

		for (String identifierSource : identifiers) {

			String identifierValue = getValue(propi, identifierSource);

			if (StringUtils.isNotBlank(identifierValue)) {
				e2.appendChild(lDocument.createTextNode(identifierValue));
				e3.appendChild(e2);
				break;
			}
		}

		for (String nameSource : names) {

			String nameValue = getValue(propi, nameSource);

			if (StringUtils.isNotBlank(nameValue)) {
				Element e1 = lDocument.createElementNS(options.GML_NS,
						"gml:name");
				e1.appendChild(lDocument.createTextNode(nameValue));
				e3.appendChild(e1);
			}
		}

		return e;
	}

	/**
	 * @param i
	 *            the model element from which to retrieve a specific value
	 * @param source
	 *            one of: name, alias, id, initialValue, @{tagged value name}
	 * @return the value of the source; can be blank (<code>null</code> or
	 *         empty)
	 */
	private String getValue(Info i, String source) {

		String s = null;

		if (source.equalsIgnoreCase("name")) {
			s = i.name();
		} else if (source.equalsIgnoreCase("alias")) {
			s = i.aliasName();
		} else if (source.equalsIgnoreCase("id")) {
			s = i.id();
		} else if (source.equalsIgnoreCase("initialValue")
				&& i instanceof PropertyInfo) {
			s = ((PropertyInfo) i).initialValue();
		} else if (source.startsWith("@")) {
			s = i.taggedValue(source.substring(1));
		}

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
	public void registerRulesAndRequirements(RuleRegistry r) {
	 // no rules or requirements defined for this target, thus nothing to do
	}

	@Override
	public String getTargetName() {
		return "Code List Dictionary";
	}
	
	@Override
	public String getTargetIdentifier() {
	    return "cld";
	}
	
	@Override
	public String getDefaultEncodingRule() {
		return "*";
	}

	@Override
	public String message(int mnr) {

		switch (mnr) {

		case 101:
			return "Code list '$1$' is not configured to be encoded as a dictionary. It will be ignored.";

		default:
			return "(" + CodelistDictionaries.class.getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
