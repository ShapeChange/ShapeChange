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
 * (c) 2013 interactive instruments GmbH, Bonn, Germany
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.SerializerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.Target.SingleTarget;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.ModelDiff.DiffElement;
import de.interactive_instruments.ShapeChange.ModelDiff.Differ;
import de.interactive_instruments.ShapeChange.ModelDiff.DiffElement.ElementType;
import de.interactive_instruments.ShapeChange.ModelDiff.DiffElement.Operation;

public class CodelistRegister implements SingleTarget {

	private Model model = null;
	private Options options = null;
	private ShapeChangeResult result = null;
	private boolean diagnosticsOnly = false;
	private static boolean printed = false;
	private static boolean enums = true;
	private static boolean initialised = false;
	private static String baseURI = "http://example.com/fixme/";
	private static String identifier = "initialValue,name";
	private static String label = "name";
	private static String documentation = "notes";
	private static String profiles = null;
	private static String minprofiles = null;
	private static String version = null;
	private static String language = "en";
	private static String regTitle = "FIXME";
	private static String author = "ShapeChange";
	private static String regDescription = "FIXME";
	private static Document rootDocument = null;
	private static Element root = null;
	private static String datetimePub = null;	
	private static String datetimeUpd = null;	
	private static String xsltPath = "src/main/resources/xslt";
	private static String xslTransformerFactory = null;
	private static String outputDirectory = ".";
	private static TreeMap<String, Document> documentMap = new TreeMap<String, Document>();
	private static SortedMap<Info,SortedSet<DiffElement>> diffs = null;
	private static Differ differ = null;
	private static Model refModel = null;
	private static PackageInfo refPackage = null;
	private static String refVersion = null;
	private static String xslhtmlfileName = "clr-html.xsl";
	private static String xslskosfileName = "clr-skos.xsl";
	private static String xslgmlfileName = "clr-gml.xsl";

	private static final String ATOM_NS = "http://www.w3.org/2005/Atom";
	private static final boolean html = true;
	private static final boolean gml = true;
	private static final boolean skos = true;
	
	public void initialise(PackageInfo p, Model m, Options o,
			ShapeChangeResult r, boolean diagOnly) throws ShapeChangeAbortException {
		model = m;
		options = o;
		result = r;
		diagnosticsOnly = diagOnly;

		if (!initialised) {
			initialised = true;
		
			String s = options.parameter(this.getClass().getName(),"enumerations");
			if (s!=null && s.equalsIgnoreCase("false"))
				enums = false;
			
			s = options.parameter(this.getClass().getName(),"baseURI");
			if (s!=null && !s.isEmpty())
				baseURI = s;
			
			s = options.parameter(this.getClass().getName(),"config");
			if (s!=null && !s.isEmpty()) {
				if (s.equalsIgnoreCase("geoinfodok")) {
					identifier = "initialValue,name";
					label = "name";
					documentation = "notes";
					profiles = "@AAA:Modellart";
					minprofiles = "@AAA:Grunddatenbestand";
					language = "de";
				} else if (s.equalsIgnoreCase("geoinfohok")) {
					identifier = "alias,name";
					label = "name";
					documentation = "@name,definition,description";
					profiles = "@profiles";
					minprofiles = null;
					language = "en";
				}
			}
			
			s = options.parameter(this.getClass().getName(),"identifier");
			if (s!=null && !s.isEmpty())
				identifier = s;
			
			s = options.parameter(this.getClass().getName(),"label");
			if (s!=null && !s.isEmpty())
				label = s;
			
			s = options.parameter(this.getClass().getName(),"documentation");
			if (s!=null && !s.isEmpty())
				documentation = s;
			
			s = options.parameter(this.getClass().getName(),"title");
			if (s!=null && !s.isEmpty())
				regTitle = s;
			
			s = options.parameter(this.getClass().getName(),"description");
			if (s!=null && !s.isEmpty())
				regDescription = s;

		    TimeZone tz = TimeZone.getTimeZone("UTC");
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
			df.setTimeZone(tz);
			datetimePub = df.format(new Date());	
			datetimeUpd = datetimePub;	

			s = options.parameter(this.getClass().getName(),"datetimePublished");
			if (s!=null && !s.isEmpty())
				datetimePub = s;
			
			s = options.parameter(this.getClass().getName(),"datetimeUpdated");
			if (s!=null && !s.isEmpty())
				datetimeUpd = s;
			
			s = options.parameter(this.getClass().getName(),"author");
			if (s!=null && !s.isEmpty())
				author = s;

			s = options.parameter(this.getClass().getName(),"xsltPath");
			if (s!=null && s.length()>0)
				xsltPath = s;

			s = options.parameter(this.getClass().getName(),"xslTransformerFactory");
			if (s!=null && s.length()>0)
				xslTransformerFactory = s;
			
			s = options.parameter(this.getClass().getName(),"xslhtmlFile");
			if (s!=null && s.length()>0)
				xslhtmlfileName = s;
						
			s = options.parameter(this.getClass().getName(),"xslgmlFile");
			if (s!=null && s.length()>0)
				xslgmlfileName = s;
						
			s = options.parameter(this.getClass().getName(),"xslskosFile");
			if (s!=null && s.length()>0)
				xslskosfileName = s;
						
			s = options.parameter(this.getClass().getName(), "outputDirectory");
			if (s==null)
				s = options.parameter("outputDirectory");
			if (s!=null && s.length()>0)
				outputDirectory = s;

			version = p.version();
			
			refModel = getReferenceModel();
			if (refModel!=null) {
				SortedSet<PackageInfo> set = refModel.schemas(p.name());
				if (set.size()==1) {
					differ = new Differ(true,new String[0]);
					refPackage = set.iterator().next();
					refVersion = refPackage.version();
					diffs = differ.diff(p, refPackage);
					for (Entry<Info,SortedSet<DiffElement>> me : diffs.entrySet()) {
						MessageContext mc = result.addInfo("Model difference - "+me.getKey().fullName().replace(p.fullName(),p.name()));
						if (mc!=null) {
							for (DiffElement diff : me.getValue()) {
								s = diff.change+" "+diff.subElementType;
								if (diff.subElementType==ElementType.TAG)
									s += "("+diff.tag+")";
								if (diff.subElement!=null) {
									s += " "+diff.subElement.name(); 
									if (diff.subElementType==ElementType.CLASS || diff.subElementType==ElementType.SUBPACKAGE || diff.subElementType==ElementType.PROPERTY) {
										String s2 = diff.subElement.taggedValue("AAA:Kennung"); 
										if (s2!=null && !s2.isEmpty()) 
											s += " ("+s2+")";
									} else if (diff.subElementType==ElementType.ENUM) {
										String s2 = ((PropertyInfo)diff.subElement).initialValue(); 
										if (s2!=null && !s2.isEmpty()) 
											s += " ("+s2+")";
									}
								} else if (diff.diff!=null)
									s += " "+ differ.diff_toString(diff.diff).replace("[[/ins]][[ins]]", "").replace("[[/del]][[del]]", "").replace("[[ins]][[/ins]]", "").replace("[[del]][[/del]]", "");
								else
									s += " ???";
								mc.addDetail(s);
							}
						}
					}
				}
			}
			
			rootDocument = createDocument();
			
			documentMap.put(baseURI, rootDocument);
	
			root = rootDocument.createElementNS(ATOM_NS, "feed");
			rootDocument.appendChild(root);

			Element e1 = rootDocument.createElementNS(ATOM_NS, "title");
			root.appendChild(e1);
			e1.appendChild(rootDocument.createTextNode(regTitle));
			
			if (regDescription!=null) {
				e1 = rootDocument.createElementNS(ATOM_NS, "subtitle");
				e1.appendChild(rootDocument.createTextNode(regDescription));
				root.appendChild(e1);
			}
			
			e1 = rootDocument.createElementNS(ATOM_NS, "link");
			root.appendChild(e1);
			addAttribute(rootDocument, e1, "href", baseURI+"/index.atom");
			addAttribute(rootDocument, e1, "rel", "self");
			addAttribute(rootDocument, e1, "type", "application/atom+xml");
			
			if (html) {
				e1 = rootDocument.createElementNS(ATOM_NS, "link");
				root.appendChild(e1);
				addAttribute(rootDocument, e1, "href", baseURI+"/index.html");
				addAttribute(rootDocument, e1, "rel", "alternate");
				addAttribute(rootDocument, e1, "type", "text/html");
			}
			
			if (gml) {
				e1 = rootDocument.createElementNS(ATOM_NS, "link");
				root.appendChild(e1);
				addAttribute(rootDocument, e1, "href", baseURI+"/index.gml");
				addAttribute(rootDocument, e1, "rel", "alternate");
				addAttribute(rootDocument, e1, "type", "application/gml+xml;version=3.2");
			}
			
			/* TODO Decide whether a SKOS makes any sense on level 0
			if (skos) {
				e1 = rootDocument.createElementNS(ATOM_NS, "link");
				root.appendChild(e1);
				addAttribute(rootDocument, e1, "href", baseURI+"/index.rdf");
				addAttribute(rootDocument, e1, "rel", "alternate");
				addAttribute(rootDocument, e1, "type", "application/rdf+xml");
			}
			*/	
			
			e1 = rootDocument.createElementNS(ATOM_NS, "published");
			root.appendChild(e1);
			e1.appendChild(rootDocument.createTextNode(datetimePub));

			e1 = rootDocument.createElementNS(ATOM_NS, "updated");
			root.appendChild(e1);
			e1.appendChild(rootDocument.createTextNode(datetimeUpd));

			e1 = rootDocument.createElementNS(ATOM_NS, "author");
			root.appendChild(e1);
			Element e2 = rootDocument.createElementNS(ATOM_NS, "name");
			e1.appendChild(e2);
			e2.appendChild(rootDocument.createTextNode(author));
			
			e1 = rootDocument.createElementNS(ATOM_NS, "id");
			root.appendChild(e1);
			e1.appendChild(rootDocument.createTextNode(baseURI));
		}
	}

	private Model getReferenceModel() {
		String imt = options.parameter(this.getClass().getName(),"referenceModelType");
		String mdl = options.parameter(this.getClass().getName(),"referenceModelFile");
		
		if (imt==null || mdl==null || imt.isEmpty() || mdl.isEmpty())
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
			if (theClass==null) {
				result.addError(null, 17, imt); 
				result.addError(null, 22, mdl); 
				return null;
			}
			m = (Model)theClass.newInstance();
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
		if (cat != Options.CODELIST && (!enums || cat != Options.ENUMERATION))
				return;

		Operation op = null;
		if (diffs!=null && diffs.get(ci.pkg())!=null)
			for (DiffElement diff : diffs.get(ci.pkg())) {
				if (diff.subElementType==ElementType.CLASS && ((ClassInfo)diff.subElement)==ci && diff.change==Operation.INSERT) {
					op=Operation.INSERT;
					break;
				} else if (diff.subElementType==ElementType.CLASS && ((ClassInfo)diff.subElement)==ci && diff.change==Operation.DELETE) {
					op=Operation.DELETE;
					break;
				}
			}
		if (refModel!=null && op==null) {
			PackageInfo pix = ci.pkg();
			while (pix!=null) {
				if (diffs!=null && diffs.get(pix.owner())!=null)
					for (DiffElement diff : diffs.get(pix.owner())) {
						if (diff.subElementType==ElementType.SUBPACKAGE && ((PackageInfo)diff.subElement)==pix && diff.change==Operation.INSERT) {
							op=Operation.INSERT;
							pix=null;
							break;
						}
						if (diff.subElementType==ElementType.SUBPACKAGE && ((PackageInfo)diff.subElement)==pix && diff.change==Operation.DELETE) {
							op=Operation.DELETE;
							pix=null;
							break;
						}
					}
				if (pix!=null)
					pix = pix.owner();
			}
		}
				
		Document cDocument = createDocument();

		String id = ci.id();
		String[] sa = identifier.split(",");
		for (String s0 : sa) {
			String s = getValue(ci,s0);
			if (s!=null && !s.isEmpty()) {
				id = s;
				break;
			}
		}
		String uri = baseURI+"/"+id.replace("/", "-");
		
		documentMap.put(uri, cDocument);
		
		String theLabel = getValue(ci,label);
		String doc = "";
		sa = documentation.split(",");
		for (String s0 : sa) {
			String s = getValue(ci,s0);
			if (s!=null && !s.isEmpty()) {
				doc += "\n\n"+s;
			}
		}
		doc = doc.trim();
		
		if (op==null && diffs!=null && diffs.get(ci)!=null)
			for (DiffElement diff : diffs.get(ci)) {
				if (diff.subElementType==ElementType.DOCUMENTATION || diff.subElementType==ElementType.NAME || diff.subElementType==ElementType.TAG || diff.subElementType==ElementType.ALIAS) {
					op = Operation.CHANGE;
					break;
				}
			}
		
		processClass(rootDocument, root, ci, false, uri, theLabel, doc, op);
		
		Element cRoot = processClass(cDocument, null, ci, true, uri, theLabel, doc, op);
		processProperties(cDocument, cRoot, ci, uri, (op==Operation.CHANGE?null:op));
	}
	
	private Element processClass(Document theDocument, Element baseRoot, ClassInfo ci, boolean feed, String uri, String label, String doc, Operation op) {
		Element theRoot = theDocument.createElementNS(ATOM_NS, feed? "feed" : "entry");
		if (baseRoot==null) {
			theDocument.appendChild(theRoot);
		} else {
			baseRoot.appendChild(theRoot);
		}

		Element e1 = theDocument.createElementNS(ATOM_NS, "title");
		theRoot.appendChild(e1);
		e1.appendChild(theDocument.createTextNode(label));
		
		if (!doc.isEmpty()) {
			if (feed) {
				e1 = theDocument.createElementNS(ATOM_NS,"subtitle");
			} else {
				e1 = theDocument.createElementNS(ATOM_NS,"summary");
			}
			e1.appendChild(theDocument.createTextNode(doc.trim()));
			theRoot.appendChild(e1);
		}
		
		e1 = theDocument.createElementNS(ATOM_NS, "link");
		theRoot.appendChild(e1);
		addAttribute(theDocument, e1, "href", uri+"/index.atom");
		addAttribute(theDocument, e1, "rel", feed ? "self" : "alternate");
		addAttribute(theDocument, e1, "type", "application/atom+xml");
		
		if (html) {
			e1 = theDocument.createElementNS(ATOM_NS, "link");
			theRoot.appendChild(e1);
			addAttribute(theDocument, e1, "href", uri+"/index.html");
			addAttribute(theDocument, e1, "rel", "alternate");
			addAttribute(theDocument, e1, "type", "text/html");
		}
		
		if (gml) {
			e1 = theDocument.createElementNS(ATOM_NS, "link");
			theRoot.appendChild(e1);
			addAttribute(theDocument, e1, "href", uri+"/index.gml");
			addAttribute(theDocument, e1, "rel", "alternate");
			addAttribute(theDocument, e1, "type", "application/gml+xml;version=3.2");
		}
		
		if (skos) {
			e1 = theDocument.createElementNS(ATOM_NS, "link");
			theRoot.appendChild(e1);
			addAttribute(theDocument, e1, "href", uri+"/index.rdf");
			addAttribute(theDocument, e1, "rel", "alternate");
			addAttribute(theDocument, e1, "type", "application/rdf+xml");
		}
		
		e1 = theDocument.createElementNS(ATOM_NS, "published");
		theRoot.appendChild(e1);
		if (op==null || op==Operation.DELETE || op==Operation.CHANGE)
			e1.appendChild(theDocument.createTextNode(datetimePub));
		else 
			e1.appendChild(theDocument.createTextNode(datetimeUpd));
		
		
		e1 = theDocument.createElementNS(ATOM_NS, "updated");
		theRoot.appendChild(e1);
		if (refModel!=null && op==null)
			e1.appendChild(theDocument.createTextNode(datetimePub));
		else 
			e1.appendChild(theDocument.createTextNode(datetimeUpd));

		e1 = theDocument.createElementNS(ATOM_NS, "author");
		theRoot.appendChild(e1);
		Element e2 = theDocument.createElementNS(ATOM_NS, "name");
		e1.appendChild(e2);
		e2.appendChild(theDocument.createTextNode(author));
		
		e1 = theDocument.createElementNS(ATOM_NS, "id");
		theRoot.appendChild(e1);
		e1.appendChild(theDocument.createTextNode(uri));			

		if (refVersion!=null && (op==null || op==Operation.DELETE || op==Operation.CHANGE)) {
			e1 = theDocument.createElementNS(ATOM_NS, "category");
			theRoot.appendChild(e1);
			addAttribute(theDocument, e1, "term", refVersion);
			addAttribute(theDocument, e1, "scheme", baseURI+"/version");
			addAttribute(theDocument, e1, "label", refVersion);
		} 				
		if (version!=null && (op==null || op==Operation.INSERT || op==Operation.CHANGE)) {
			e1 = theDocument.createElementNS(ATOM_NS, "category");
			theRoot.appendChild(e1);
			addAttribute(theDocument, e1, "term", version);
			addAttribute(theDocument, e1, "scheme", baseURI+"/version");
			addAttribute(theDocument, e1, "label", version);
		}

		if (op==Operation.DELETE) {
			e1 = theDocument.createElementNS(ATOM_NS, "category");
			theRoot.appendChild(e1);
			addAttribute(theDocument, e1, "term", "retired");
			addAttribute(theDocument, e1, "scheme", baseURI+"/status");
			addAttribute(theDocument, e1, "label", "Zurückgezogen");
		} else if (op==Operation.INSERT) {
			e1 = theDocument.createElementNS(ATOM_NS, "category");
			theRoot.appendChild(e1);
			addAttribute(theDocument, e1, "term", "valid");
			addAttribute(theDocument, e1, "scheme", baseURI+"/status");
			addAttribute(theDocument, e1, "label", "Gültig");
		} else if (op==Operation.CHANGE) {
			e1 = theDocument.createElementNS(ATOM_NS, "category");
			theRoot.appendChild(e1);
			addAttribute(theDocument, e1, "term", "valid");
			addAttribute(theDocument, e1, "scheme", baseURI+"/status");
			addAttribute(theDocument, e1, "label", "Gültig");
		} else if (op==null) {
			e1 = theDocument.createElementNS(ATOM_NS, "category");
			theRoot.appendChild(e1);
			addAttribute(theDocument, e1, "term", "valid");
			addAttribute(theDocument, e1, "scheme", baseURI+"/status");
			addAttribute(theDocument, e1, "label", "Gültig");
		}		
		
		int cat = ci.category();
		if (cat == Options.CODELIST) {
			e1 = theDocument.createElementNS(ATOM_NS, "category");
			theRoot.appendChild(e1);
			addAttribute(theDocument, e1, "term", "codelist");
			addAttribute(theDocument, e1, "scheme", baseURI+"/type");
			addAttribute(theDocument, e1, "label", "Codeliste");			
		} else if (cat == Options.ENUMERATION) {
			e1 = theDocument.createElementNS(ATOM_NS, "category");
			theRoot.appendChild(e1);
			addAttribute(theDocument, e1, "term", "enumeration");
			addAttribute(theDocument, e1, "scheme", baseURI+"/type");
			addAttribute(theDocument, e1, "label", "Enumeration");			
		}

		if (profiles!=null) {
			String malist = getValue(ci,profiles);
			if (malist!=null) {
				malist = malist.trim();
				if (malist.length()>0) {
					for (String ma : malist.split(",")) {
						ma = ma.trim();
						e1 = theDocument.createElementNS(ATOM_NS, "category");
						theRoot.appendChild(e1);
						addAttribute(theDocument, e1, "term", ma);
						addAttribute(theDocument, e1, "scheme", baseURI+"/profile");
						addAttribute(theDocument, e1, "label", ma);
					}
				}
			} else {
				e1 = theDocument.createElementNS(ATOM_NS, "category");
				theRoot.appendChild(e1);
				addAttribute(theDocument, e1, "term", "alle");
				addAttribute(theDocument, e1, "scheme", baseURI+"/profile");
				addAttribute(theDocument, e1, "label", "alle");
			}
			
			if (minprofiles!=null) {
				malist = getValue(ci,minprofiles);
				if (malist!=null) {
					malist = malist.trim();
					if (malist.length()>0) {
						for (String ma : malist.split(",")) {
							ma = ma.trim();
							e1 = theDocument.createElementNS(ATOM_NS, "category");
							theRoot.appendChild(e1);
							addAttribute(theDocument, e1, "term", ma);
							addAttribute(theDocument, e1, "scheme", baseURI+"/mandatoryToCapture");
							addAttribute(theDocument, e1, "label", ma);
						}
					}
				}
			}
		}
		
		
		return theRoot;
	}

	private void processProperties(Document cDocument, Element cRoot, ClassInfo ci, String uri, Operation op) {
		for (Iterator<PropertyInfo> j = ci.properties().values().iterator(); j.hasNext();) {			
			PropertyInfo propi = j.next();
			
			Operation top = op;
			if (diffs!=null && diffs.get(ci)!=null)
				for (DiffElement diff : diffs.get(ci)) {
					if (diff.subElementType==ElementType.ENUM && ((PropertyInfo)diff.subElement)==propi && diff.change==Operation.INSERT) {
						top = Operation.INSERT;
						break;
					}
				}			
			if (top==null && diffs!=null && diffs.get(propi)!=null)
				for (DiffElement diff : diffs.get(propi)) {
					if (diff.subElementType==ElementType.DOCUMENTATION || diff.subElementType==ElementType.NAME || diff.subElementType==ElementType.TAG || diff.subElementType==ElementType.ALIAS) {
						top = Operation.CHANGE;
						break;
					}
				}			
			
			processProperty(cDocument, cRoot, ci, propi, uri, top);
		}		
		
		if (diffs!=null && diffs.get(ci)!=null)
			for (DiffElement diff : diffs.get(ci)) {
				if (diff.subElementType==ElementType.ENUM && diff.change==Operation.DELETE) {
					processProperty(cDocument, cRoot, ci, (PropertyInfo)diff.subElement, uri, Operation.DELETE);
					for (PropertyInfo propi : ci.properties().values()) {
						if ((PropertyInfo)diff.subElement==propi) {
							// FIXME processProperty(cDocument, cRoot, ci, propi, uri, Operation.DELETE);
						}
					}
				}
			}			
		

		SortedSet<String> ts = ci.supertypes();
		for (Iterator<String> i = ts.iterator(); i.hasNext();) {
			ClassInfo cix = model.classById(i.next());
			if (cix != null) {
				processProperties(cDocument, cRoot, cix, uri, op);
			}
		}
		
	}
	
	private void processProperty(Document cDocument, Element cRoot, ClassInfo ci, PropertyInfo propi, String uri, Operation op) {
		String id = propi.id();
		String[] sa = identifier.split(",");
		for (String s0 : sa) {
			String s = getValue(propi,s0);
			if (s!=null && !s.isEmpty()) {
				id = s;
				break;
			}
		}
		String valuri = uri+"/"+id.replace("/", "-");
		String theLabel = getValue(ci,label) + "/" + getValue(propi,label);
		String doc = "";
		sa = documentation.split(",");
		for (String s0 : sa) {
			String s = getValue(propi,s0);
			if (s!=null && !s.isEmpty()) {
				doc += "\n\n"+s;
			}
		}
		doc = doc.trim();

		if (op==Operation.CHANGE) {
			Element e1 = createEntry(cDocument, ci, propi, valuri, oldText(theLabel), oldText(doc), false, op, null, valuri+"-1");
			if (e1!=null) {
				cRoot.appendChild(e1);
				Document vDocument = createDocument();
				e1 = createEntry(vDocument, ci, propi, valuri, oldText(theLabel), oldText(doc), true, op, null, valuri+"-1");
				vDocument.appendChild(e1);

				documentMap.put(valuri, vDocument);
			}
			e1 = createEntry(cDocument, ci, propi, valuri+"-1", newText(theLabel), newText(doc), false, op, valuri, null);
			if (e1!=null) {
				cRoot.appendChild(e1);
				Document vDocument = createDocument();
				e1 = createEntry(vDocument, ci, propi, valuri+"-1", newText(theLabel), newText(doc), true, op, valuri, null);
				vDocument.appendChild(e1);

				documentMap.put(valuri+"-1", vDocument);
			}
			
		} else {		
			Element e1 = createEntry(cDocument, ci, propi, valuri, theLabel, doc, false, op, null, null);
			if (e1!=null) {
				cRoot.appendChild(e1);
				Document vDocument = createDocument();
				e1 = createEntry(vDocument, ci, propi, valuri, theLabel, doc, true, op, null, null);
				vDocument.appendChild(e1);
	
				documentMap.put(valuri, vDocument);
			}
		}				
	}

	private Element createEntry(Document cDocument, ClassInfo ci, PropertyInfo propi, String uri, String thelabel, String doc, boolean self, Operation op, String predecessor, String successor) {

		Element cRoot = cDocument.createElementNS(ATOM_NS, "entry");

		Element e1 = cDocument.createElementNS(ATOM_NS, "title");
		cRoot.appendChild(e1);
		e1.appendChild(cDocument.createTextNode(thelabel));
		
		if (!doc.isEmpty()) {
			e1 = cDocument.createElementNS(ATOM_NS,"summary");
			e1.appendChild(cDocument.createTextNode(doc.trim()));
			cRoot.appendChild(e1);
		}
		
		e1 = cDocument.createElementNS(ATOM_NS, "link");
		cRoot.appendChild(e1);
		addAttribute(cDocument, e1, "href", uri+".atom");
		addAttribute(cDocument, e1, "rel", self ? "self" : "alternate");
		addAttribute(cDocument, e1, "type", "application/atom+xml");
		
		if (html) {
			e1 = cDocument.createElementNS(ATOM_NS, "link");
			cRoot.appendChild(e1);
			addAttribute(cDocument, e1, "href", uri+".html");
			addAttribute(cDocument, e1, "rel", "alternate");
			addAttribute(cDocument, e1, "type", "text/html");
		}
		
		if (gml) {
			e1 = cDocument.createElementNS(ATOM_NS, "link");
			cRoot.appendChild(e1);
			addAttribute(cDocument, e1, "href", uri+".gml");
			addAttribute(cDocument, e1, "rel", "alternate");
			addAttribute(cDocument, e1, "type", "application/gml+xml;version=3.2");
		}
		
		if (skos) {
			e1 = cDocument.createElementNS(ATOM_NS, "link");
			cRoot.appendChild(e1);
			addAttribute(cDocument, e1, "href", uri+".rdf");
			addAttribute(cDocument, e1, "rel", "alternate");
			addAttribute(cDocument, e1, "type", "application/rdf+xml");
		}
		
		if (predecessor!=null) {
			e1 = cDocument.createElementNS(ATOM_NS, "link");
			cRoot.appendChild(e1);
			addAttribute(cDocument, e1, "href", predecessor);
			addAttribute(cDocument, e1, "rel", "predecessor");
		}
		
		if (successor!=null) {
			e1 = cDocument.createElementNS(ATOM_NS, "link");
			cRoot.appendChild(e1);
			addAttribute(cDocument, e1, "href", successor);
			addAttribute(cDocument, e1, "rel", "successor");
		}

		e1 = cDocument.createElementNS(ATOM_NS, "published");
		cRoot.appendChild(e1);
		if (op==null || op==Operation.DELETE || (op==Operation.CHANGE && successor!=null))
			e1.appendChild(cDocument.createTextNode(datetimePub));
		else 
			e1.appendChild(cDocument.createTextNode(datetimeUpd));
				
		e1 = cDocument.createElementNS(ATOM_NS, "updated");
		cRoot.appendChild(e1);
		if (refModel!=null && op==null)
			e1.appendChild(cDocument.createTextNode(datetimePub));
		else 
			e1.appendChild(cDocument.createTextNode(datetimeUpd));

		e1 = cDocument.createElementNS(ATOM_NS, "author");
		cRoot.appendChild(e1);
		Element e2 = cDocument.createElementNS(ATOM_NS, "name");
		e1.appendChild(e2);
		e2.appendChild(cDocument.createTextNode(author));
		
		e1 = cDocument.createElementNS(ATOM_NS, "id");
		cRoot.appendChild(e1);
		e1.appendChild(cDocument.createTextNode(uri));		

		if (refVersion!=null && (op==null || op==Operation.DELETE || (op==Operation.CHANGE && successor!=null))) {
			e1 = cDocument.createElementNS(ATOM_NS, "category");
			cRoot.appendChild(e1);
			addAttribute(cDocument, e1, "term", refVersion);
			addAttribute(cDocument, e1, "scheme", baseURI+"/version");
			addAttribute(cDocument, e1, "label", refVersion);
		} 
				
		if (version!=null && (op==null || op==Operation.INSERT || (op==Operation.CHANGE && predecessor!=null))) {
			e1 = cDocument.createElementNS(ATOM_NS, "category");
			cRoot.appendChild(e1);
			addAttribute(cDocument, e1, "term", version);
			addAttribute(cDocument, e1, "scheme", baseURI+"/version");
			addAttribute(cDocument, e1, "label", version);
		}

		if (op==Operation.DELETE) {
			e1 = cDocument.createElementNS(ATOM_NS, "category");
			cRoot.appendChild(e1);
			addAttribute(cDocument, e1, "term", "retired");
			addAttribute(cDocument, e1, "scheme", baseURI+"/status");
			addAttribute(cDocument, e1, "label", "Zurückgezogen");
		} else if (op==Operation.INSERT) {
			e1 = cDocument.createElementNS(ATOM_NS, "category");
			cRoot.appendChild(e1);
			addAttribute(cDocument, e1, "term", "valid");
			addAttribute(cDocument, e1, "scheme", baseURI+"/status");
			addAttribute(cDocument, e1, "label", "Gültig");
		} else if (op==Operation.CHANGE && successor!=null) {
			e1 = cDocument.createElementNS(ATOM_NS, "category");
			cRoot.appendChild(e1);
			addAttribute(cDocument, e1, "term", "superseded");
			addAttribute(cDocument, e1, "scheme", baseURI+"/status");
			addAttribute(cDocument, e1, "label", "Veraltet");
		} else if (op==Operation.CHANGE && predecessor!=null) {
			e1 = cDocument.createElementNS(ATOM_NS, "category");
			cRoot.appendChild(e1);
			addAttribute(cDocument, e1, "term", "valid");
			addAttribute(cDocument, e1, "scheme", baseURI+"/status");
			addAttribute(cDocument, e1, "label", "Gültig");
		} else if (op==null) {
			e1 = cDocument.createElementNS(ATOM_NS, "category");
			cRoot.appendChild(e1);
			addAttribute(cDocument, e1, "term", "valid");
			addAttribute(cDocument, e1, "scheme", baseURI+"/status");
			addAttribute(cDocument, e1, "label", "Gültig");
		}		
		
		if (profiles!=null) {
			String malist = getValue(propi,profiles);
			if (malist!=null) {
				malist = malist.trim();
				if (malist.length()>0) {
					for (String ma : malist.split(",")) {
						ma = ma.trim();
						e1 = cDocument.createElementNS(ATOM_NS, "category");
						cRoot.appendChild(e1);
						addAttribute(cDocument, e1, "term", ma);
						addAttribute(cDocument, e1, "scheme", baseURI+"/profile");
						addAttribute(cDocument, e1, "label", ma);
					}
				}
			} else {
				e1 = cDocument.createElementNS(ATOM_NS, "category");
				cRoot.appendChild(e1);
				addAttribute(cDocument, e1, "term", "alle");
				addAttribute(cDocument, e1, "scheme", baseURI+"/profile");
				addAttribute(cDocument, e1, "label", "alle");
			}
	
			if (minprofiles!=null) {
				malist = getValue(propi,minprofiles);
				if (malist!=null) {
					malist = malist.trim();
					if (malist.length()>0) {
						for (String ma : malist.split(",")) {
							ma = ma.trim();
							e1 = cDocument.createElementNS(ATOM_NS, "category");
							cRoot.appendChild(e1);
							addAttribute(cDocument, e1, "term", ma);
							addAttribute(cDocument, e1, "scheme", baseURI+"/mandatoryToCapture");
							addAttribute(cDocument, e1, "label", ma);
						}
					}
				}
			}
		}
		
		return cRoot;
	}
	
	private String oldText(String s) {
		s = s.replaceAll("\\[\\[ins\\]\\].+?(?=\\[\\[/ins\\]\\])\\[\\[/ins\\]\\]", "");
		s = s.replaceAll("\\[\\[del\\]\\]", "");
		s = s.replaceAll("\\[\\[/del\\]\\]", "");
		return s;
	}

	private String newText(String s) {
		s = s.replaceAll("\\[\\[del\\]\\].+?(?=\\[\\[/del\\]\\])\\[\\[/del\\]\\]", "");
		s = s.replaceAll("\\[\\[ins\\]\\]", "");
		s = s.replaceAll("\\[\\[/ins\\]\\]", "");
		return s;
	}

	private String getValue(Info i, String source) {
		String s = null;

		if (source.equalsIgnoreCase("name")) {
			s = i.name();
			if (diffs!=null && diffs.get(i)!=null)
				for (DiffElement diff : diffs.get(i)) {
					if (diff.subElementType==ElementType.NAME) {
						s = differ.diff_toString(diff.diff);
						break;
					}
				}
		} else if (source.equalsIgnoreCase("alias")) {
			s = i.aliasName();
			if (diffs!=null && diffs.get(i)!=null)
				for (DiffElement diff : diffs.get(i)) {
					if (diff.subElementType==ElementType.ALIAS) {
						s = differ.diff_toString(diff.diff);
						break;
					}
				}
		} else if (source.equalsIgnoreCase("id")) {
			s = i.id();
		} else if (source.equalsIgnoreCase("notes")) {
			s = i.documentation();
			if (diffs!=null && diffs.get(i)!=null)
				for (DiffElement diff : diffs.get(i)) {
					if (diff.subElementType==ElementType.DOCUMENTATION) {
						s = differ.diff_toString(diff.diff);
						break;
					}
				}
		} else if (source.equalsIgnoreCase("description")) {
			s = i.description();
			if (diffs!=null && diffs.get(i)!=null)
				for (DiffElement diff : diffs.get(i)) {
					if (diff.subElementType==ElementType.DESCRIPTION) {
						s = differ.diff_toString(diff.diff);
						break;
					}
				}
		} else if (source.equalsIgnoreCase("definition")) {
			s = i.definition();
			if (diffs!=null && diffs.get(i)!=null)
				for (DiffElement diff : diffs.get(i)) {
					if (diff.subElementType==ElementType.DEFINITION) {
						s = differ.diff_toString(diff.diff);
						break;
					}
				}
		} else if (source.equalsIgnoreCase("initialValue") && i instanceof PropertyInfo) {
			s = ((PropertyInfo)i).initialValue();
			if (diffs!=null && diffs.get(i)!=null)
				for (DiffElement diff : diffs.get(i)) {
					if (diff.subElementType==ElementType.ENUM) {
						s = differ.diff_toString(diff.diff);
						break;
					}
				}
		} else if (source.startsWith("@")) {
			s = i.taggedValue(source.substring(1));
			if (diffs!=null && diffs.get(i)!=null)
				for (DiffElement diff : diffs.get(i)) {
					if (diff.subElementType==ElementType.TAG && diff.tag.equalsIgnoreCase(source.substring(1))) {
						s = differ.diff_toString(diff.diff);
						break;
					}
				}
		}
			
		if (s!=null && s.isEmpty())
			s = null;
		
		return s;
	}

	public void write() {
	}
	
	public void writeAll(ShapeChangeResult r) {
		result = r;

		if (printed) {
			return;
		}
		if (diagnosticsOnly) {
			return;
		}

		try {
			Properties outputFormat = OutputPropertiesFactory.getDefaultMethodProperties("xml");
			outputFormat.setProperty("indent", "yes");
			outputFormat.setProperty("{http://xml.apache.org/xalan}indent-amount", "2");
			outputFormat.setProperty("encoding", "UTF-8");

	        Serializer serializer = SerializerFactory.getSerializer(outputFormat);
	        
	        for (Entry<String, Document> mapentry : documentMap.entrySet()) {
				Document cDocument = mapentry.getValue();
				if (cDocument != null) {
					String dir = outputDirectory;

		            File outDir = new File(dir);
		            if(!outDir.exists())
		            	outDir.mkdirs();

		            String path = mapentry.getKey().replace(baseURI, "");

		            int i = 0;
		            String fname = "index";
		            for (String step : path.split("/")) {
		            	if (step.trim().isEmpty())
		            		continue;
		            	if (i==0) {
			            	dir += "/"+step;
				            outDir = new File(dir);
				            if(!outDir.exists())
				            	outDir.mkdirs();
		            	} else if (i==1) {
			            	fname = step;
		            	}
		            	i++;
		            }
		            
					OutputStream fout= new FileOutputStream(dir + "/" + fname + ".atom");
			        OutputStreamWriter outputXML = new OutputStreamWriter(fout, outputFormat.getProperty("encoding"));
					serializer.setWriter(outputXML);
					serializer.asDOMSerializer().serialize(cDocument);
					outputXML.close();
					result.addResult(getTargetName(), dir, fname + ".atom", path);
					
					if (html && xslhtmlfileName!=null)
						xsltWrite(dir, fname+".atom", "/"+xslhtmlfileName, fname+".html", i);
					if (skos && i>0 && xslskosfileName!=null)
						xsltWrite(dir, fname+".atom", "/"+xslskosfileName, fname+".rdf", i);
					if( gml && xslgmlfileName!=null)
						xsltWrite(dir, fname+".atom", "/"+xslgmlfileName, fname+".gml", i);

					fout= new FileOutputStream(dir + "/" + fname + ".var");
			        OutputStreamWriter outputVAR = new OutputStreamWriter(fout);
			        outputVAR.write("URI: "+fname+"\n\n");
			        outputVAR.write("URI: "+fname+".atom\n");
			        outputVAR.write("Content-type: application/atom+xml\n\n");
			        if (gml) {
				        outputVAR.write("URI: "+fname+".gml\n");
				        outputVAR.write("Content-type: application/gml+xml\n\n");
			        }
					if (html) {
				        outputVAR.write("URI: "+fname+".html\n");
				        outputVAR.write("Content-type: text/html\n\n");
					}
					if (skos && i>0) {
				        outputVAR.write("URI: "+fname+".rdf\n");
				        outputVAR.write("Content-type: application/rdf+xml\n\n");
					}
					outputVAR.close();
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
		return "Codelist register";
	}

	public void reset() {
		printed = false;
		enums = true;
		initialised = false;
		baseURI = "http://example.com/fixme/";
		identifier = "initialValue,name";
		label = "name";
		documentation = "notes";
		profiles = null;
		minprofiles = null;
		language = "en";
		regTitle = "FIXME";
		author = "ShapeChange";
		regDescription = null;
		documentMap = new TreeMap<String, Document>();
		rootDocument = null;
		root = null;
		datetimePub = null;	
		datetimeUpd = null;	
		outputDirectory = ".";
		xslhtmlfileName = "clr-html.xsl";
		xslskosfileName = "clr-skos.xsl";
		xslgmlfileName = "clr-gml.xsl";
	}
	
   public void xsltWrite(String outputDirectory, String xmlName, String xsltfileName, String outfileName, int level){
	   try{
			// Setup directories
            File outDir = new File(outputDirectory);

            // Setup input and output files
            File xmlFile = new File(outDir, xmlName);	        
           	if(!xmlFile.canRead()){
            	result.addError(null, 301, xmlFile.getName(), outfileName);
            	return;
            }

           	File outFile = new File(outDir, outfileName);

           	InputStream stream = null;
           	if (xsltPath.toLowerCase().startsWith("http")) {
           		URL url = new URL(xsltPath+"/"+xsltfileName);
           		URLConnection urlConnection = url.openConnection();
           		stream = urlConnection.getInputStream();
           	} else {
           		File xsl = new File(xsltPath+"/"+xsltfileName);
           		if (xsl.exists())
           			stream = new FileInputStream(xsl);
           		else {
           			result.addError("XSLT stylesheet "+xsl.getAbsolutePath()+" not found.");
           			return;
           		}
           	}
    		    		
		    Source xsltSource = new StreamSource(stream);		    
		    Source xmlSource = new StreamSource(xmlFile);
		    Result res = new StreamResult(outFile);
		 
		    // create an instance of TransformerFactory		   
			if (xslTransformerFactory != null) {
				// use TransformerFactory specified in configuration
				System.setProperty("javax.xml.transform.TransformerFactory", xslTransformerFactory);
			} else {
				// use TransformerFactory determined by system
			}
			TransformerFactory transFact = TransformerFactory.newInstance();
		    Transformer trans = transFact.newTransformer(xsltSource);
		    trans.setParameter("baseuri", baseURI);
		    trans.setParameter("level", ""+level);
		    trans.setParameter("language", language);
		    trans.transform(xmlSource, res);
		    
			result.addResult(getTargetName(), outputDirectory, outfileName, null);

	   } catch (Exception e) {
			String m = e.getMessage();
			if (m != null) {
				result.addError(m);
			}
	       e.printStackTrace(System.err);
	   }	
		    
	}	
}
