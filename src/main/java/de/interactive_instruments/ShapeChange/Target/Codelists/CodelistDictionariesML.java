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
import java.util.HashSet;
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

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Target.Target;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;

/**
 * 
 * Creation of ISO 19139 codelist dictionaries. 
 * If multilingual information is avaiable we create an ML_CodeListDictionary / ML_CodeDefinition,
 * otherwise CodeListDictionary / CodeDefinition.
 * The way how the (multilingual) information is stored in the model is specific to each application,
 * so here is only a general structure of the dictionary creation, you have to overwrite create-methods
 * according to your model.
 * 
 * @author olk
 *
 */
public class CodelistDictionariesML implements Target {

	protected final String dlma = "["; 
	protected final String dlme = "]"; 
	protected final String defaultLang = "de"; 
	
	protected final String GML_NSABBR = "gml";
	protected final String GMX_NSABBR = "gmx";
	protected final String GMD_NSABBR = "gmd";
	protected final String GCO_NSABBR = "gco";
	protected final String XLINK_NSABBR = "xlink";

	protected String sLangs = null;
	protected String[] langs = null;

	protected ShapeChangeResult result = null;

	private PackageInfo pi = null;
	private Model model = null;
	protected Options options = null;
	private boolean diagnosticsOnly = false;
	private boolean printed = false;
	private final HashMap<String, Document> documentMap = new HashMap<String, Document>();
	private String documentationTemplate = null;
	private String documentationNoValue = null;

	private boolean newlineOmit = true;
	/**
	 * Ctor
	 */
	public CodelistDictionariesML(){}
	

	public void initialise(PackageInfo p, Model m, Options o,
			ShapeChangeResult r, boolean diagOnly) throws ShapeChangeAbortException {
		pi = p;
		model = m;
		options = o;
		result = r;
		diagnosticsOnly = diagOnly;

		String pm = options.parameter(this.getClass().getName(),"noNewlineOmit");
		if(pm!=null && pm.equalsIgnoreCase("true"))
			newlineOmit = false;
		else
			newlineOmit = true;

		sLangs = options.parameter(this.getClass().getName(),"languages");
		if(sLangs!=null)
			langs = sLangs.split(" ");

		// change the default documentation template?
		documentationTemplate = options.parameter(this.getClass().getName(), "documentationTemplate");
		documentationNoValue = options.parameter(this.getClass().getName(), "documentationNoValue");
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
			return;
		}
		
		// filter by package name
		String tvn = options.parameter(this.getClass().getName(), "clPackageName");
		if(tvn!=null && tvn.length()>0 && (!tvn.equalsIgnoreCase(ci.pkg().name()))){
			return;
		}

		Document cDocument = createDocument();

/*
		ProcessingInstruction proci = null;
		if (options.gmlVersion.equals("3.2"))
			proci = cDocument.createProcessingInstruction("xml-stylesheet", "type='text/xsl' href='./CodelistDictionary-v32.xsl'");
		else if (options.gmlVersion.equals("3.1"))
			proci = cDocument.createProcessingInstruction("xml-stylesheet", "type='text/xsl' href='./CodelistDictionary-v31.xsl'");
		if (proci!=null)	
			cDocument.appendChild(proci);
*/

		createDictionary(ci, cDocument);
	}
	
	private void createDictionary(ClassInfo ci, Document cDocument){
		
		Element et = null;

		// first create entry for alternative languages in cl-definition if possible,
		// so we can decide if we create a multi lingual dictionary or not
		boolean altLangFound = false;
		HashSet<Element> altExprs = new HashSet<Element>();
		for(String lang : langs){
			if(lang.equalsIgnoreCase(defaultLang)){
				continue;
			}
			
			Element eai = cDocument.createElement(GMX_NSABBR+":ClAlternativeExpression");
			Element ea = cDocument.createElement(GMX_NSABBR+":alternativeExpression");
			addAttribute(cDocument, eai, GML_NSABBR+":id", ci.name()+"_"+lang);
			addAttribute(cDocument, eai, "codeSpace", createClCodeSpaceContent(ci));
			
			// description
			Element et1 = createCodelistDescription(lang, ci, cDocument);
			if(et1!=null)
				eai.appendChild(et1);

			// name / identifier
			Element et2 = createCodelistIdentifier(lang, ci, cDocument);
			if(et2!=null)
				eai.appendChild(et2);

			// name
			Element et3 = createCodelistName(lang, ci, cDocument);
			if(et3!=null)
				eai.appendChild(et3);
			
			if((et1!=null||et3!=null)&&et2!=null){
				// locale
				et = cDocument.createElement(GMX_NSABBR+":locale");
				String localeVal = options.parameter(this.getClass().getName(),"localeRef_"+lang);
				if(localeVal==null || localeVal.trim().length()==0){
					localeVal = "FIXME_MISSING_LOCALE_REF";
					result.addError("Configuration entry for locale reference is missing for language '" + lang + "' in class '" + ci.name() + "'");
				}
				addAttribute(cDocument, et, "xlink:href", localeVal);
				eai.appendChild(et);
				ea.appendChild(eai);
				altExprs.add(ea);

				altLangFound = true;
			}
		}
		
		// if an alternative expression is found we create an multi linugual cl, 
		// otherwise a non-ml codelist dictionary
		Element ec = null;
		if(altLangFound){
			ec = createMlDocumentNamespace(ci.name(), cDocument);
		}
		else{
			ec = createDocumentNamespace(ci.name(), cDocument);
		}


		// create codelist dictionary
		documentMap.put(ci.id(), cDocument);

		// - dictionary description
		et = createCodelistDescription(defaultLang, ci, cDocument);
		if(et!=null)
			ec.appendChild(et);

		// - dictionary name / identifier
		et = createCodelistIdentifier(defaultLang, ci, cDocument);
		if(et!=null)
			ec.appendChild(et);

		// - dictionary name
		et = createCodelistName(defaultLang, ci, cDocument);
		if(et!=null)
			ec.appendChild(et);
		
		// - dictionary properties / codes
		createDictionaryProperties(ec, ci, cDocument);
	
		// Alternative languages for cl dictionary - if we found at least one (in first step of dict creation),
		// then we add it here.
		for(Element ea : altExprs){
			ec.appendChild(ea);
		}
	}

	protected void createDictionaryProperties(Element ec, ClassInfo ci, Document cDocument){
		for (Iterator<PropertyInfo> j = ci.properties().values().iterator(); j.hasNext();) {			
			PropertyInfo propi = j.next();
			Element e1 = createEntry(cDocument, ci, propi, true);
			if (e1!=null)
				ec.appendChild(e1);
		}		
		SortedSet<String> ts = ci.supertypes();
		for (Iterator<String> i = ts.iterator(); i.hasNext();) {
			ClassInfo cix = model.classById(i.next());
			if (cix != null) {
				for (Iterator<PropertyInfo> j = cix.properties().values().iterator(); j.hasNext();) {			
					PropertyInfo propi = j.next();
					Element e1 = createEntry(cDocument, ci, propi, true);
					if (e1!=null)
						ec.appendChild(e1);
				}		
			}
		}
	}

	protected Element createDocumentNamespace(String gmlid, Document cDocument){
		Element ec = cDocument.createElementNS(options.fullNamespace(GMX_NSABBR), GMX_NSABBR+":CodeListDictionary");
		cDocument.appendChild(ec);

		addDocumentNamespaceAttributes(ec, cDocument);	
		addAttribute(cDocument, ec, "gml:id", gmlid);
		addDocumentSchemaLocAttribute(ec, cDocument);
		return ec;
	}

	protected Element createMlDocumentNamespace(String gmlid, Document cDocument){
		Element ec = cDocument.createElementNS(options.fullNamespace(GMX_NSABBR), GMX_NSABBR+":ML_CodeListDictionary");
		cDocument.appendChild(ec);

		addDocumentNamespaceAttributes(ec, cDocument);	
		addAttribute(cDocument, ec, "gml:id", gmlid);
		addDocumentSchemaLocAttribute(ec, cDocument);
		return ec;
	}

	protected void addDocumentNamespaceAttributes(Element ec, Document cDocument){
		addAttribute(cDocument, ec, "xmlns:gml", options.GML_NS);
		addAttribute(cDocument, ec, "xmlns:gmx", options.fullNamespace(GMX_NSABBR));
		addAttribute(cDocument, ec, "xmlns:gmd", options.fullNamespace(GMD_NSABBR));
		addAttribute(cDocument, ec, "xmlns:gco", options.fullNamespace(GCO_NSABBR));
		addAttribute(cDocument, ec, "xmlns:xlink", options.fullNamespace(XLINK_NSABBR));
		addAttribute(cDocument, ec, "xmlns:xsi",
					"http://www.w3.org/2001/XMLSchema-instance");
	}
	
	protected void addDocumentSchemaLocAttribute(Element ec, Document cDocument){
		addAttribute(cDocument, ec, "xsi:schemaLocation", options.fullNamespace(GMX_NSABBR)
					+ " " + options.schemaLocationOfNamespace(options.fullNamespace(GMX_NSABBR)));		
	}
	
	protected Element createCodelistDescription(String lang, ClassInfo ci, Document cDocument){
		Element e = null;
		String s = ci.derivedDocumentation(documentationTemplate, documentationNoValue);
		if (s != null && !s.isEmpty()) {
			e = cDocument.createElementNS(options.GML_NS, "description");
			e.appendChild(cDocument.createTextNode(omitNewlineChar(s)));
		}
		return e;
	}

	protected Element createCodelistIdentifier(String lang, ClassInfo ci, Document cDocument){
		Element e = null;
		if (options.gmlVersion.equals("3.2")) {
			e = cDocument.createElement(GML_NSABBR+":identifier");
			addAttribute(cDocument, e, "codeSpace", createClCodeSpaceContent(ci));
			e.appendChild(cDocument.createTextNode(ci.name()));
		} else if (options.gmlVersion.equals("3.1")) {
			e = cDocument.createElement(GML_NSABBR+":name");
			e.appendChild(cDocument.createTextNode(ci.name()));
		}
		return e;
	}
	
	protected Element createCodelistName(String lang, ClassInfo ci, Document cDocument){
		Element e = null;
		e = cDocument.createElement(GML_NSABBR+":name");
		e.appendChild(cDocument.createTextNode(ci.name()));
		return e;
	}
	
	protected Element createEntry(Document lDocument,
			ClassInfo ci, PropertyInfo propi, boolean local) {

		HashSet<Element> eas = createEntryAlternLangCode(lDocument, ci, propi);
		
		Element e = lDocument.createElement(GMX_NSABBR+":codeEntry");
		
		// entry definition object
		Element eDef = createEntryDefinitionContent(lDocument, ci, propi, eas);
		if(eDef!=null)
			e.appendChild(eDef);
		
		return e;
	}

	/**
	 * 
	 * @param lDocument
	 * @param ci
	 * @param propi
	 * @return HashSet of alternative language elements, if no alt. lang. exists: null
	 */
	protected HashSet<Element> createEntryAlternLangCode(Document lDocument,
			ClassInfo ci, PropertyInfo propi) {

		HashSet<Element> eas = null; 
		Element et;
		
		for(String lang : langs){
			if(lang.equalsIgnoreCase(defaultLang)){
				continue;
			}
			
			Element ea = lDocument.createElement(GMX_NSABBR+":alternativeExpression");
			Element eai = lDocument.createElement(GMX_NSABBR+":CodeAlternativeExpression");
			addAttribute(lDocument, eai, GML_NSABBR+":id", createEntryIdentifier(lang, ci, propi));
			addAttribute(lDocument, eai, "codeSpace", createCodesCodeSpaceContent(ci, propi));
			ea.appendChild(eai);
			
			// entry description
			Element et1 = createEntryDescription(lang, lDocument, ci, propi);
			if(et1!=null)
				eai.appendChild(et1);
	
			// entry identifier
			Element et2 = createEntryIdentifier(lang, lDocument, ci, propi);
			if(et2!=null)
				eai.appendChild(et2);
	
			// entry name
			Element et3 = createEntryName(lang, lDocument, ci, propi);
			if(et3!=null)
				eai.appendChild(et3);
			
			if((et1!=null||et3!=null)&&et2!=null){
				// locale
				et = lDocument.createElement(GMX_NSABBR+":locale");
				addAttribute(lDocument, et, "xlink:href", options.parameter(this.getClass().getName(),"localeRef_"+lang));
				eai.appendChild(et);

				if(eas==null)
					eas = new HashSet<Element>();
				eas.add(ea);
			}
		}

		return eas;
	}
	protected Element createEntryDefinitionContent(Document lDocument,
			ClassInfo ci, PropertyInfo propi, HashSet<Element> eas) {

		// entry definition object
		Element eDef = createEntryDefinition(defaultLang, lDocument, ci, propi, eas!=null);
		if(eDef!=null){
			// entry description
			Element et = createEntryDescription(defaultLang, lDocument, ci, propi);
			if(et!=null)
				eDef.appendChild(et);

			// entry identifier
			et = createEntryIdentifier(defaultLang, lDocument, ci, propi);
			if(et!=null)
				eDef.appendChild(et);

			// entry name
			et = createEntryName(defaultLang, lDocument, ci, propi);
			if(et!=null)
				eDef.appendChild(et);

			// alternative languages
			if(eas!=null){
				for(Element ea : eas){
					eDef.appendChild(ea);
				}
			}
		}
		
		return eDef;
	}

	protected Element createEntryDefinition(String lang, 
			Document lDocument, ClassInfo ci, PropertyInfo propi,
			boolean altEntryLangsExist){
		
		Element e;
		if(altEntryLangsExist)
			e = lDocument.createElement(GMX_NSABBR+":ML_CodeDefinition");
		else
			e = lDocument.createElement(GMX_NSABBR+":CodeDefinition");
		addAttribute(lDocument, e, "gml:id", createEntryIdentifier(lang, ci, propi));
		return e;
	}
	
	protected Element createEntryDescription(String lang, 
			Document lDocument, ClassInfo ci, PropertyInfo propi){
		Element e = null;
		String s = propi.derivedDocumentation(documentationTemplate, documentationNoValue);
		if (s != null && !s.isEmpty()) {
			e = lDocument.createElementNS(options.GML_NS,
					"description");
			e.appendChild(lDocument.createTextNode(omitNewlineChar(s)));
		}
		return e;
	}

	protected Element createEntryIdentifier(String lang, 
			Document lDocument, ClassInfo ci, PropertyInfo propi){
		Element e = null;
		if (options.gmlVersion.equals("3.2")) {
			e = lDocument.createElement(GML_NSABBR+":identifier");
		} else {
			e = lDocument.createElement(GML_NSABBR+":name");
		}
		
		addAttribute(lDocument, e, "codeSpace", createClCodeSpaceContent(ci));

		if (propi.initialValue()!=null) {
			e.appendChild(lDocument.createTextNode(propi.initialValue()));
		} else {
			e.appendChild(lDocument.createTextNode(propi.name()));
		}

		return e;
	}

	protected Element createEntryName(String lang, 
			Document lDocument, ClassInfo ci, PropertyInfo propi){
		Element e = null;
		e = lDocument.createElementNS(options.GML_NS,"name");
		e.appendChild(lDocument.createTextNode(propi.name()));
		return e;
	}
	
	protected String createClCodeSpaceContent(ClassInfo ci){
		String codeSpace = ci.taggedValue("infoURL");
		if (codeSpace==null || codeSpace.trim().length()==0){
			codeSpace = options.parameter(this.getClass().getName(),"infoURL");
			if (codeSpace==null || codeSpace.trim().length()==0)
				codeSpace = ci.pkg().targetNamespace();
			codeSpace += "/" + ci.name();
		}
		return codeSpace;
	}
	
	protected String createCodesCodeSpaceContent(ClassInfo ci, PropertyInfo propi){
		String codeSpace = ci.taggedValue("infoURL");
		if (codeSpace==null || codeSpace.trim().length()==0){
			codeSpace = options.parameter(this.getClass().getName(),"infoURL");
			if (codeSpace==null || codeSpace.trim().length()==0)
				codeSpace = ci.pkg().targetNamespace();
			codeSpace += "/" + ci.name();
		}
		return codeSpace;
	}
	
	
	protected String createEntryIdentifier(String lang, ClassInfo ci, PropertyInfo propi){
		
		String id = "_" + propi.id();
		if(!lang.equalsIgnoreCase(defaultLang))
			id += "_" + lang;
		return id;
	}
	
	protected String omitNewlineChar(String in){
		if (newlineOmit && in!=null)
			return in.replaceAll("\r\n", " ");
		else
			return in;
	}
	
	public void write() {
		if (printed) {
			return;
		}
		if (diagnosticsOnly) {
			return;
		}

		try {
			Properties outputFormat = OutputPropertiesFactory.getDefaultMethodProperties("xml");
			outputFormat.setProperty("indent", "yes");
			outputFormat.setProperty("{http://xml.apache.org/xalan}indent-amount",
					"2");
			outputFormat.setProperty("encoding", "UTF-8");

	        Serializer serializer = SerializerFactory.getSerializer(outputFormat);
			for (Iterator<ClassInfo> i = model.classes(pi).iterator(); i.hasNext();) {
				ClassInfo ci = i.next();
				Document cDocument = documentMap.get(ci.id());
				if (cDocument != null) {
					String dir = options.parameter(this.getClass().getName(),"outputDirectory");
					if (dir==null)
						dir = options.parameter("outputDirectory");
					if (dir==null)
						dir = options.parameter(".");

		            File outDir = new File(dir);
		            if(!outDir.exists())
		            	outDir.mkdirs();
		            
					/*
					 * SO: Used OutputStreamWriter instead of FileWriter to set character encoding
					 * (see doc in Serializer.setWriter and FileWriter) 
					 */
					OutputStream fout= new FileOutputStream(dir + "/" + ci.name() + ".xml");
			        OutputStreamWriter outputXML = new OutputStreamWriter(fout, outputFormat.getProperty("encoding"));
					serializer.setWriter(outputXML);
					serializer.asDOMSerializer().serialize(cDocument);
					outputXML.close();
					result.addResult(getTargetName(), dir, ci.name()+".xml", ci.qname());
					
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
		return "Code List Dictionary";
	}
}
