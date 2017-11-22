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

package de.interactive_instruments.ShapeChange.Target.Definitions;

import java.io.File;
import java.io.FileWriter;
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

import de.interactive_instruments.ShapeChange.Multiplicity;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Target.Target;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;

public class Definitions implements Target {

	private PackageInfo pi = null;
	private Model model = null;
	private Options options = null;
	private ShapeChangeResult result = null;
	private boolean printed = false;
	private Document document = null;
	private final HashMap<String, Document> documentMap = new HashMap<String, Document>();
	private Element root = null;
	private String outputDirectory = null;
	private boolean schema = true;
	private boolean instanceView = false;
	private String namingAuthority = null;
	private boolean diagnosticsOnly = false;
	private boolean aborted = false;
	private String documentationTemplate = null;
	private String documentationNoValue = null;

	public void initialise(PackageInfo p, Model m, Options o,
			ShapeChangeResult r, boolean diagOnly) throws ShapeChangeAbortException {
		pi = p;
		model = m;
		options = o;
		result = r;
		diagnosticsOnly = diagOnly;
		
		outputDirectory = options.parameter(this.getClass().getName(),"outputDirectory");
		if (outputDirectory==null)
			outputDirectory = options.parameter("outputDirectory");
		if (outputDirectory==null)
			outputDirectory = options.parameter(".");

		if (options.parameter(this.getClass().getName(),"style").equals("TYPE"))
			schema = false;

		if (options.parameter(this.getClass().getName(),"instanceView").equals("true"))
			instanceView = true;
		
		namingAuthority = options.parameter(this.getClass().getName(),"namingAuthority");
		if (namingAuthority==null)
			namingAuthority = "UNKNOWN";
		
		if (!options.gmlVersion.equals("3.2")) {
			result.addError(null,110, pi.name());
			aborted = true;
			return;
		}
		
		// change the default documentation template?
		documentationTemplate = options.parameter(this.getClass().getName(), "documentationTemplate");
		documentationNoValue = options.parameter(this.getClass().getName(), "documentationNoValue");

		result.addDebug(null,10005, pi.name());

		document = createDocument();

		ProcessingInstruction proci;
		proci = document.createProcessingInstruction("xml-stylesheet",
				"type='text/xsl' href='./ShapeChangeDefinitions.xsl'");
		document.appendChild(proci);

		root = document.createElementNS(Options.DEF_NS,
				"ApplicationSchemaDefinition");
		document.appendChild(root);
		addAttribute(document, root, "gml:id", "_");
		addAttribute(document, root, "xmlns:gml", options.GML_NS);
		addAttribute(document, root, "xmlns:def", Options.DEF_NS);
		addAttribute(document, root, "xmlns:xlink",
				"http://www.w3.org/1999/xlink");
		addAttribute(document, root, "xmlns:xsi",
				"http://www.w3.org/2001/XMLSchema-instance");
		addAttribute(document, root, "xsi:schemaLocation", Options.DEF_NS
				+ " ShapeChangeDefinitions.xsd");
		if (instanceView)
			addAttribute(document, root, "instanceView", "true");

		Element e1;

		String s = pi.derivedDocumentation(documentationTemplate, documentationNoValue);
		if (s != null && !s.isEmpty()) {
			e1 = document.createElementNS(options.GML_NS, "description");
			e1.appendChild(document.createTextNode(s));
			root.appendChild(e1);
		}
		e1 = document.createElementNS(options.GML_NS, "identifier");
		e1.appendChild(document
				.createTextNode("urn:x-shapechange:def:applicationSchema:"
						+ namingAuthority + "::" + pi.xmlns() + ":"
						+ pi.version()));
		addAttribute(document, e1, "codeSpace", "http://www.interactive-instruments.de/ShapeChange");
		root.appendChild(e1);
		e1 = document.createElementNS(options.GML_NS, "name");
		e1.appendChild(document.createTextNode(pi.name()));
		root.appendChild(e1);
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
		if (aborted)
			return;
		
		int cat = ci.category();
		if (cat != Options.FEATURE && cat != Options.OBJECT && cat != Options.MIXIN
				&& cat != Options.DATATYPE && cat != Options.BASICTYPE
				&& cat != Options.UNION) {
			return;
		}
		if (instanceView && ci.isAbstract())
			return;			

		String t = "featureType";
		if (cat == Options.OBJECT) {
			t = "objectType";
		} else if (cat == Options.DATATYPE) {
			t = "dataType";
		} else if (cat == Options.MIXIN) {
			t = "mixinType";
		} else if (cat == Options.BASICTYPE) {
			t = "basicType";
		} else if (cat == Options.UNION) {
			t = "unionType";
		}

		if (schema) {

			Element e1 = document.createElementNS(options.GML_NS,
					"dictionaryEntry");
			root.appendChild(e1);
			Element e2 = createTypeDefinition(pi, document, ci, t);
			e1.appendChild(e2);

		} else {

			Document cDocument = createDocument();

			ProcessingInstruction proci;
			proci = cDocument.createProcessingInstruction("xml-stylesheet",
					"type='text/xsl' href='./ShapeChangeDefinitionsType.xsl'");
			cDocument.appendChild(proci);

			Element ec = createTypeDefinition(pi, cDocument, ci, t);
			cDocument.appendChild(ec);
			addAttribute(cDocument, ec, "xmlns:gml", options.GML_NS);
			addAttribute(cDocument, ec, "xmlns:def", Options.DEF_NS);
			addAttribute(cDocument, ec, "xmlns:xlink",
					"http://www.w3.org/1999/xlink");
			addAttribute(cDocument, ec, "xmlns:xsi",
					"http://www.w3.org/2001/XMLSchema-instance");
			addAttribute(cDocument, ec, "xsi:schemaLocation", Options.DEF_NS
					+ " ShapeChangeDefinitions.xsd");

			documentMap.put(ci.id(), cDocument);

			Element e1 = document.createElementNS(options.GML_NS,
					"dictionaryEntry");
			addAttribute(document, e1, "xlink:href", "urn:x-shapechange:def:" + t + ":"
					+ namingAuthority + "::" + pi.xmlns() + ":"
					+ pi.version() + ":" + ci.name());
			root.appendChild(e1);
		}
	}

	private Element createTypeDefinition(PackageInfo asi, Document lDocument,
			ClassInfo ci, String t) {

		Element e1 = lDocument
				.createElementNS(Options.DEF_NS, "TypeDefinition");
		addAttribute(lDocument, e1, "gml:id", ci.id());

		Element e2;

		String s = ci.derivedDocumentation(documentationTemplate, documentationNoValue);
		if (s != null && !s.isEmpty()) {
			e2 = lDocument.createElementNS(options.GML_NS, "description");
			e2.appendChild(lDocument.createTextNode(s));
			e1.appendChild(e2);
		}

		e2 = lDocument.createElementNS(options.GML_NS, "identifier");
		e2.appendChild(lDocument.createTextNode("urn:x-shapechange:def:" + t + ":"
				+ namingAuthority + "::" + ci.pkg().xmlns() + ":"
				+ ci.pkg().version() + ":" + ci.name()));
		addAttribute(lDocument, e2, "codeSpace", Options.DEF_NS);
		e1.appendChild(e2);

		s = ci.taggedValue("Title");
		if (s != null) {
			e2 = lDocument.createElementNS(options.GML_NS, "name");
			addAttribute(lDocument, e2, "codeSpace", Options.DEF_NS + "/title");
			e2.appendChild(lDocument.createTextNode(s));
			e1.appendChild(e2);
		}

		s = ci.taggedValue("primaryCode");
		if (s != null) {
			e2 = lDocument.createElementNS(options.GML_NS, "name");
			addAttribute(lDocument, e2, "codeSpace", Options.DEF_NS
					+ "/primaryCode");
			e2.appendChild(lDocument.createTextNode(s));
			e1.appendChild(e2);
		}

		s = ci.taggedValue("secondaryCode");
		if (s != null) {
			e2 = lDocument.createElementNS(options.GML_NS, "name");
			addAttribute(lDocument, e2, "codeSpace", Options.DEF_NS
					+ "/secondaryCode");
			e2.appendChild(lDocument.createTextNode(s));
			e1.appendChild(e2);
		}

		e2 = lDocument.createElementNS(options.GML_NS, "name");
		addAttribute(lDocument, e2, "codeSpace", Options.DEF_NS + "/name");
		e2.appendChild(lDocument.createTextNode(ci.name()));
		e1.appendChild(e2);

		if (instanceView) {
			createAllPropertyDefinitions(asi, lDocument, ci, e1, true);			
		} else {
			for (Iterator<PropertyInfo> j = ci.properties().values().iterator(); j.hasNext();) {
				PropertyInfo propi = j.next();
				e2 = createPropertyDefinition(asi, lDocument, ci, propi, true);
				if (e2!=null)
					e1.appendChild(e2);
			}
		}
		
		e2 = lDocument.createElementNS(Options.DEF_NS, "classification");
		e2.appendChild(lDocument.createTextNode(t));
		e1.appendChild(e2);

		if (!instanceView && ci.supertypes() != null) {
			for (Iterator<String> k = ci.supertypes().iterator(); k.hasNext();) {
				ClassInfo ei = model.classById(k.next());
				if (ei==null)
					continue;
				int cate = ei.category();
				if (ei.pkg() != null
					&& ei.inSchema(asi)
					&& (cate == Options.FEATURE
						|| cate == Options.OBJECT || cate == Options.MIXIN
						|| cate == Options.DATATYPE || cate == Options.BASICTYPE)) {
					e2 = lDocument.createElementNS(Options.DEF_NS,
							"supertypeRef");
					String t0 = "featureType";
					if (cate == Options.OBJECT) {
						t0 = "objectType";
					} else if (cate == Options.MIXIN) {
						t0 = "mixinType";
					} else if (cate == Options.DATATYPE) {
						t0 = "dataType";
					} else if (cate == Options.BASICTYPE) {
						t0 = "basicType";
					}
					addAttribute(lDocument, e2, "xlink:href", "urn:x-shapechange:def:"
							+ t0 + ":" + namingAuthority + "::"
							+ ei.pkg().xmlns() + ":" + ei.pkg().version() + ":"
							+ ei.name());
				} else {
					e2 = lDocument.createElementNS(Options.DEF_NS,
							"supertypeName");
					e2.appendChild(lDocument.createTextNode(ei.name()));
				}
				e1.appendChild(e2);
			}
		}

		String[] tags = options.parameter("representTaggedValues").split("\\,");
		for (int i = 0; i < tags.length; i++) {
			s = ci.taggedValue(tags[i].trim());
			if (s != null && !tags[i].trim().equals("primaryCode")
					&& !tags[i].trim().equals("secondaryCode")) {
				e2 = lDocument.createElementNS(Options.DEF_NS, "taggedValue");
				addAttribute(lDocument, e2, "tag", tags[i].trim());
				e2.appendChild(lDocument.createTextNode(s));
				e1.appendChild(e2);
			}
		}

		return e1;
	}

	private Element createPropertyDefinition(PackageInfo asi, Document lDocument,
			ClassInfo ci, PropertyInfo propi, boolean local) {
		if (!propi.isNavigable())
			return null;
		if (propi.isRestriction())
			return null;

		Element e = lDocument.createElementNS(options.GML_NS,
				"dictionaryEntry");
		Element e3 = lDocument.createElementNS(Options.DEF_NS,
				"PropertyDefinition");
		addAttribute(lDocument, e3, "gml:id", propi.id());
		e.appendChild(e3);

		Element e2;
		String s = propi.derivedDocumentation(documentationTemplate, documentationNoValue);
		if (s != null && !s.isEmpty()) {
			e2 = lDocument.createElementNS(options.GML_NS,
					"description");
			e2.appendChild(lDocument.createTextNode(s));
			e3.appendChild(e2);
		}

		e2 = lDocument.createElementNS(options.GML_NS, "identifier");
		e2.appendChild(lDocument
				.createTextNode("urn:x-shapechange:def:propertyType:"
						+ namingAuthority + "::"
						+ ci.pkg().xmlns() + ":" + ci.pkg().version()
						+ ":" + ci.name() + ":" + propi.name()));
		addAttribute(lDocument, e2, "codeSpace", Options.DEF_NS);
		e3.appendChild(e2);

		s = propi.taggedValue("Title");
		if (s != null) {
			e2 = lDocument.createElementNS(options.GML_NS, "name");
			addAttribute(lDocument, e2, "codeSpace", Options.DEF_NS
					+ "/title");
			e2.appendChild(lDocument.createTextNode(s));
			e3.appendChild(e2);
		}

		s = propi.taggedValue("primaryCode");
		if (s != null) {
			e2 = lDocument.createElementNS(options.GML_NS, "name");
			addAttribute(lDocument, e2, "codeSpace", Options.DEF_NS
					+ "/primaryCode");
			e2.appendChild(lDocument.createTextNode(s));
			e3.appendChild(e2);
		}

		s = propi.taggedValue("secondaryCode");
		if (s != null) {
			e2 = lDocument.createElementNS(options.GML_NS, "name");
			addAttribute(lDocument, e2, "codeSpace", Options.DEF_NS
					+ "/secondaryCode");
			e2.appendChild(lDocument.createTextNode(s));
			e3.appendChild(e2);
		}

		e2 = lDocument.createElementNS(options.GML_NS, "name");
		addAttribute(lDocument, e2, "codeSpace", Options.DEF_NS
				+ "/name");
		e2.appendChild(lDocument.createTextNode(propi.name()));
		e3.appendChild(e2);

		ClassInfo ei = model.classById(propi.typeInfo().id);

		int cate = Options.UNKNOWN;
		if (ei!=null)
			cate = ei.category();
		
		if (ei != null 
				&& ei.pkg() != null
				&& (cate == Options.ENUMERATION || cate == Options.CODELIST)) {

			for (Iterator<PropertyInfo> k = ei.properties().values()
					.iterator(); k.hasNext();) {
				PropertyInfo vi = k.next();

				e2 = lDocument.createElementNS(options.GML_NS,
						"dictionaryEntry");
				e3.appendChild(e2);

				Element e4 = lDocument.createElementNS(Options.DEF_NS,
						"ListedValueDefinition");
				addAttribute(lDocument, e4, "gml:id", propi.id() + "_"
						+ vi.id());
				e2.appendChild(e4);

				s = vi.derivedDocumentation(documentationTemplate, documentationNoValue);
				if (s != null && !s.isEmpty()) {
					e2 = lDocument.createElementNS(options.GML_NS,
							"description");
					e2.appendChild(lDocument.createTextNode(s));
					e4.appendChild(e2);
				}
				e2 = lDocument.createElementNS(options.GML_NS,
						"identifier");
				if (vi.initialValue() != null) {
					e2
							.appendChild(lDocument
									.createTextNode("urn:x-shapechange:def:propertyType:"
											+ namingAuthority
											+ "::"
											+ ci.pkg().xmlns()
											+ ":"
											+ ci.pkg().version()
											+ ":"
											+ ci.name()
											+ ":"
											+ propi.name()
											+ ":"
											+ vi.initialValue()));
				} else {
					e2
							.appendChild(lDocument
									.createTextNode("urn:x-shapechange:def:propertyType:"
											+ namingAuthority
											+ "::"
											+ ci.pkg().xmlns()
											+ ":"
											+ ci.pkg().version()
											+ ":"
											+ ci.name()
											+ ":"
											+ propi.name()
											+ ":"
											+ vi.name()));
				}
				addAttribute(lDocument, e2, "codeSpace", "http://www.interactive-instruments.de/ShapeChange");
				e4.appendChild(e2);
				e2 = lDocument.createElementNS(options.GML_NS, "name");
				e2.appendChild(lDocument.createTextNode(vi.name()));
				e4.appendChild(e2);
			}
		}

		Multiplicity m = propi.cardinality();
		e2 = lDocument.createElementNS(Options.DEF_NS, "cardinality");
		if (m.minOccurs == m.maxOccurs) {
			e2.appendChild(lDocument.createTextNode(m.minOccurs + ""));
		} else if (m.maxOccurs == Integer.MAX_VALUE
				|| m.maxOccurs == -1) {
			e2.appendChild(lDocument
					.createTextNode(m.minOccurs + "..*"));
		} else {
			e2.appendChild(lDocument.createTextNode(m.minOccurs + ".."
					+ m.maxOccurs));
		}
		e3.appendChild(e2);

		if (ei != null
				&& ei.pkg() != null
				&& ei.inSchema(asi)
				&& (cate == Options.FEATURE
						|| cate == Options.OBJECT
						|| cate == Options.MIXIN
						|| cate == Options.DATATYPE
						|| cate == Options.BASICTYPE
						|| cate == Options.UNION)) {
			e2 = lDocument.createElementNS(Options.DEF_NS,
					"valueTypeRef");
			String t0 = "featureType";
			if (cate == Options.OBJECT) {
				t0 = "objectType";
			} else if (cate == Options.MIXIN) {
				t0 = "mixinType";
			} else if (cate == Options.BASICTYPE) {
				t0 = "basicType";
			} else if (cate == Options.DATATYPE) {
				t0 = "dataType";
			} else if (cate == Options.UNION) {
				t0 = "unionType";
			}
			addAttribute(lDocument, e2, "xlink:href", "urn:x-shapechange:def:"
					+ t0 + ":" + namingAuthority + "::"
					+ ei.pkg().xmlns() + ":" + ei.pkg().version() + ":"
					+ ei.name());
		} else {
			e2 = lDocument.createElementNS(Options.DEF_NS,
					"valueTypeName");
			e2.appendChild(lDocument
					.createTextNode(propi.typeInfo().name));
		}
		e3.appendChild(e2);

		e2 = lDocument.createElementNS(Options.DEF_NS, "type");
		if (propi.isAttribute()) {
			e2.appendChild(lDocument.createTextNode("attribute"));
		} else {
			e2.appendChild(lDocument.createTextNode("associationRole"));
		}
		e3.appendChild(e2);

		return e;
	}

	private void createAllPropertyDefinitions(PackageInfo asi, Document lDocument,
			ClassInfo ci, Element e1, boolean local) {
		if (ci==null)
			return;
		if (ci.pkg()==null) {
			result.addError(null, 139, ci.name());
		}

		SortedSet<String> st = ci.supertypes();
		if (st != null) {
			for (Iterator<String> i = st.iterator(); i.hasNext();) {
				createAllPropertyDefinitions(asi, lDocument, model.classById(i.next()), e1, false);
			}
		}
		for (Iterator<PropertyInfo> j = ci.properties().values().iterator(); j.hasNext();) {
			PropertyInfo propi = j.next();
			Element e2 = createPropertyDefinition(asi, lDocument, ci, propi, local);
			if (e2!=null)
				e1.appendChild(e2);
		}		
	}
	
	public void write() {
		if (printed) {
			return;
		}
		
		if (diagnosticsOnly)
			return;

		if (aborted)
			return;

		Properties outputFormat = OutputPropertiesFactory
				.getDefaultMethodProperties("xml");
		outputFormat.setProperty("indent", "yes");
		outputFormat.setProperty("{http://xml.apache.org/xalan}indent-amount",
				"2");
		outputFormat.setProperty("encoding", "UTF-8");

		try {
			File file = new File(outputDirectory + "/index." + pi.xmlns()
					+ ".definitions.xml");
			FileWriter outputXML = new FileWriter(file);
			Serializer serializer = SerializerFactory
					.getSerializer(outputFormat);
			serializer.setWriter(outputXML);
			serializer.asDOMSerializer().serialize(document);
			outputXML.close();
			result.addResult(getTargetName(), outputDirectory, "index." + pi.xmlns() + ".definitions.xml", pi.targetNamespace());

			if (!schema) {
				for (Iterator<ClassInfo> i = model.classes(pi).iterator(); i.hasNext();) {
					ClassInfo ci = i.next();
					Document cDocument = documentMap.get(ci.id());
					if (cDocument != null) {
						outputXML = new FileWriter(outputDirectory + "/" + ci.name() + ".definitions.xml");
						serializer.setWriter(outputXML);
						serializer.asDOMSerializer().serialize(cDocument);
						outputXML.close();
						result.addResult(getTargetName(), outputDirectory, ci.name() + ".definitions.xml", ci.qname());
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

		printed = true;
	}

	@Override
	public String getTargetName(){
		return "Definitions";
	}
}
