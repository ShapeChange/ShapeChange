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
 * (c) 2002-2014 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target.AppConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.SerializerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.RuleRegistry;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.Target.Target;

/**
 * Creates an application configuration file.
 * 
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 * 
 */
public class AppConfiguration implements Target, MessageSource {

	public static final String TARGET_IDENTIFIER = "appcfg";
	public static final String NS = "http://www.interactive-instruments.de/ShapeChange/AppConfiguration/0.1";

	/* ------------------------------------------- */
	/* --- configuration parameter identifiers --- */
	/* ------------------------------------------- */

	public static final String PARAM_ID_COLUMN_NAME = "idColumnName";
	public static final String PARAM_SIZE = "size";

	/* ------------------------ */
	/* --- rule identifiers --- */
	/* ------------------------ */

	// TBD

	/* -------------------- */
	/* --- other fields --- */
	/* -------------------- */

	public static final String DEFAULT_ID_COLUMN_NAME = "_id";
	public static final int DEFAULT_SIZE = 1024;
	public static final String REFERENCE_NAME = "reference";

	private ShapeChangeResult result = null;
	private Model model = null;
	private Options options = null;
	private boolean printed = false;
	private boolean diagnosticsOnly = false;

	private String outputDirectory;

	private String idColumnName;
	private int defaultSize;

	/**
	 * key: class name; value: configuration document for the class
	 */
	private final HashMap<String, Document> documentMap = new HashMap<String, Document>();

	/**
	 * @see de.interactive_instruments.ShapeChange.Target.Target#initialise(de.interactive_instruments.ShapeChange.Model.PackageInfo,
	 *      de.interactive_instruments.ShapeChange.Model.Model,
	 *      de.interactive_instruments.ShapeChange.Options,
	 *      de.interactive_instruments.ShapeChange.ShapeChangeResult, boolean)
	 */
	public void initialise(PackageInfo pi, Model m, Options o,
			ShapeChangeResult r, boolean diagOnly)
			throws ShapeChangeAbortException {

		model = m;
		options = o;
		result = r;
		diagnosticsOnly = diagOnly;

		result.addDebug(this, 1, pi.name());

		outputDirectory = options.parameter(this.getClass().getName(),
				"outputDirectory");
		if (outputDirectory == null)
			outputDirectory = options.parameter("outputDirectory");
		if (outputDirectory == null)
			outputDirectory = options.parameter(".");

		// create output directory, if necessary
		if (!this.diagnosticsOnly) {

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
				result.addFatalError(this, 3, outputDirectory);
				return;
			}
		}

		idColumnName = options.parameter(this.getClass().getName(),
				PARAM_ID_COLUMN_NAME);
		if (idColumnName == null) {
			idColumnName = DEFAULT_ID_COLUMN_NAME;
		}

		String defaultSizeByConfig = options
				.parameter(this.getClass().getName(), PARAM_SIZE);
		if (defaultSizeByConfig == null) {
			defaultSize = DEFAULT_SIZE;
		} else {
			try {
				defaultSize = Integer.parseInt(defaultSizeByConfig);
			} catch (NumberFormatException e) {
				MessageContext mc = result.addWarning(this, 4, PARAM_SIZE,
						e.getMessage(), "" + DEFAULT_SIZE);
				mc.addDetail(this, 0);
				defaultSize = DEFAULT_SIZE;
			}
		}
	}

	public void process(ClassInfo ci) {

		int cat = ci.category();

		if (cat != Options.FEATURE) {
			return;
		}

		Document doc = createDocument();

		// processing instruction not necessary at the moment

		Element root = createTypeElement(doc, ci);

		doc.appendChild(root);

		addAttribute(doc, root, "xmlns:def", NS);
		addAttribute(doc, root, "xmlns:xlink", "http://www.w3.org/1999/xlink");
		addAttribute(doc, root, "xmlns:xsi",
				"http://www.w3.org/2001/XMLSchema-instance");

		documentMap.put(ci.name(), doc);

	}

	private Element createTypeElement(Document doc, ClassInfo ci) {

		Element root = doc.createElementNS(NS, "Objektart");

		addAttribute(doc, root, "name", normalizeName(ci.name()));

		String alias = ci.aliasName();
		if (alias == null) {
			alias = "";
		}
		addAttribute(doc, root, "alias", alias);

		// create element to represent the key/identifier
		Element pE = doc.createElementNS(NS, "Attribut");

		addAttribute(doc, pE, "name", idColumnName);
		addAttribute(doc, pE, "alias", "");
		addAttribute(doc, pE, "type", "key");

		root.appendChild(pE);

		// now create elements for all navigable class properties
		if (ci.properties() != null && !ci.properties().isEmpty()) {

			for (PropertyInfo pi : ci.properties().values()) {

				if (!pi.isNavigable()) {
					continue;
				}

				pE = doc.createElementNS(NS, "Attribut");
				root.appendChild(pE);

				addAttribute(doc, pE, "name", normalizeName(pi.name()));
				alias = pi.aliasName();
				if (alias == null) {
					alias = "";
				}
				addAttribute(doc, pE, "alias", alias);

				String type = identifyType(pi);
				addAttribute(doc, pE, "type", type);

				if (type.equals(REFERENCE_NAME)) {
					addAttribute(doc, pE, "ref",
							normalizeName(pi.typeInfo().name));
				}

				if (type.equals("text")) {
					addAttribute(doc, pE, "size", "" + getSizeForProperty(pi));
				}

				if (pi.isReadOnly()) {
					addAttribute(doc, pE, "readOnly", "true");
				}

				if (pi.initialValue() != null) {
					addAttribute(doc, pE, "default", pi.initialValue());
				}

				// add "form" specifics
				if (type.equals("text") || type.equals("boolean")
						|| pi.categoryOfValue() == Options.ENUMERATION) {

					String form = "text";

					if (type.equals("text")
							&& pi.taggedValue("formrows") != null) {

						form = "textarea";

					} else if (type.equals("boolean")) {

						form = "checkbox";

					} else if (pi.categoryOfValue() == Options.ENUMERATION) {

						form = "select";

						ClassInfo enumeration = model
								.classById(pi.typeInfo().id);

						if (enumeration.properties() != null
								&& !enumeration.properties().isEmpty()) {

							for (PropertyInfo piEnum : enumeration.properties()
									.values()) {

								Element enumE = doc.createElementNS(NS, "Wert");
								pE.appendChild(enumE);

								addAttribute(doc, enumE, "name",
										normalizeName(piEnum.name()));

								alias = piEnum.aliasName();
								if (alias == null) {
									alias = "";
								}
								addAttribute(doc, enumE, "alias", alias);
							}
						}
					}

					addAttribute(doc, pE, "form", form);

					if (form.equals("text") || form.equals("textarea")) {
						if (pi.taggedValue("formcols") != null) {
							addAttribute(doc, pE, "formcols",
									pi.taggedValue("formcols").trim());
						}
					}
					if (form.equals("textarea")) {
						if (pi.taggedValue("formrows") != null) {
							addAttribute(doc, pE, "formrows",
									pi.taggedValue("formrows").trim());
						}
					}
				}

				if (pi.taggedValue("validate") != null) {
					addAttribute(doc, pE, "validate",
							pi.taggedValue("validate").trim());
				}

				if (pi.taggedValue("Reiter") != null) {
					addAttribute(doc, pE, "tab",
							pi.taggedValue("Reiter").trim());
				}
			}
		}

		return root;
	}

	/**
	 * Determines the applicable 'size' for the given property. If the tagged
	 * value {@value #PARAM_SIZE} is set for the property, its value is
	 * returned. Otherwise the default value (given via the configuration
	 * parameter {@value #PARAM_SIZE} or as defined by this class [
	 * {@value #DEFAULT_SIZE}]) applies.
	 * 
	 * @param pi
	 * @return
	 */
	private int getSizeForProperty(PropertyInfo pi) {

		String tvSize = pi.taggedValue(PARAM_SIZE);

		int size = defaultSize;

		if (tvSize != null) {
			try {
				size = Integer.parseInt(tvSize);
			} catch (NumberFormatException e) {
				MessageContext mc = result.addWarning(this, 5, PARAM_SIZE,
						e.getMessage(), "" + defaultSize);
				mc.addDetail(this, 0);
				mc.addDetail(this, 100, pi.name(), pi.inClass().name());
				size = defaultSize;
			}
		}

		return size;
	}

	/**
	 * Identifies the type to use for the property.
	 * 
	 * At first, standard mappings (defined via the configuration) are applied.
	 * If there is no direct standard mapping, then a mapping based upon the
	 * category/stereotype of the property type is performed: enumeration,
	 * codelist and object types are mapped to 'text'. If the type is a feature,
	 * 'reference' is returned. If all else fails, 'unknown' is returned as
	 * type.
	 * 
	 * @param pi
	 * @return the type to use for the property
	 */
	private String identifyType(PropertyInfo pi) {

		// first apply well-known mappings
		String piTypeName = pi.typeInfo().name;

		// try to get type from map entries
		// TBD: we use 'sql' as platform code because it is one of the platforms
		// registered in the implementation of encodingRule()
		ProcessMapEntry me = options.targetMapEntry(piTypeName,
				pi.encodingRule("sql"));

		if (me != null && me.hasTargetType()) {
			return me.getTargetType();
		}

		// try to identify a type mapping based upon the category of the
		// property value
		int catOfValue = pi.categoryOfValue();

		if (catOfValue == Options.ENUMERATION || catOfValue == Options.CODELIST
				|| catOfValue == Options.OBJECT) {

			return "text";

		} else if (catOfValue == Options.FEATURE) {

			return REFERENCE_NAME;
		}

		return "unknown";
	}

	/** Add attribute to an element */
	protected void addAttribute(Document document, Element e, String name,
			String value) {
		Attr att = document.createAttribute(name);
		att.setValue(value);
		e.setAttributeNode(att);
	}

	protected Document createDocument() {

		// TBD: move to common TargetHelper?

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

	/**
	 * @param name
	 * @return String with any occurrence of '.' or '-' replaced by '_'.
	 */
	private String normalizeName(String name) {

		if (name == null) {
			return null;
		} else {
			return name.replace(".", "_").replace("-", "_");
		}
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Target.Target#write()
	 */
	public void write() {

		if (printed) {
			return;
		}
		if (diagnosticsOnly) {
			return;
		}

		Properties outputFormat = OutputPropertiesFactory
				.getDefaultMethodProperties("xml");
		outputFormat.setProperty("indent", "yes");
		outputFormat.setProperty("{http://xml.apache.org/xalan}indent-amount",
				"2");
		outputFormat.setProperty("encoding", "UTF-8");

		try {

			FileWriter outputXML;
			Serializer serializer = SerializerFactory
					.getSerializer(outputFormat);

			for (String className : documentMap.keySet()) {

				Document doc = documentMap.get(className);

				if (doc != null) {
					outputXML = new FileWriter(
							outputDirectory + "/" + className + ".xml");
					serializer.setWriter(outputXML);
					serializer.asDOMSerializer().serialize(doc);
					outputXML.close();
					result.addResult(getTargetName(), outputDirectory,
							className + ".xml", null);
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
		return "App Configuration";
	}
	
	@Override
	public String getDefaultEncodingRule() {
		return "*";
	}

	@Override
	public String getTargetIdentifier() {
	    return TARGET_IDENTIFIER;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.MessageSource#message(int)
	 */
	public String message(int mnr) {

		switch (mnr) {
		case 0:
			return "Context: class AppConfiguration";
		case 1:
			return "Generating app configuration for application schema '$1$'.";
		case 2:
			return "Processing class '$1$'.";
		case 3:
			return "Directory named '$1$' does not exist or is not accessible.";
		case 4:
			return "Number format exception while converting the value of configuration parameter '$1$' to an integer. Exception message: $2$. Using $3$ as default value for '$1$'.";
		case 5:
			return "Number format exception while converting the tagged value '$1$' to an integer. Exception message: $2$. Using $3$ as default value.";
		case 100:
			return "Context: property '$1$' in class '$2$'.";
		default:
			return "(" + AppConfiguration.class.getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
