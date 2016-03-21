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
 * (c) 2002-2016 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target.Metadata;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.SerializerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.TargetIdentification;
import de.interactive_instruments.ShapeChange.Target.SingleTarget;
import de.interactive_instruments.ShapeChange.Transformation.Profiling.ProfileIdentifierMap;
import de.interactive_instruments.ShapeChange.Transformation.Profiling.MalformedProfileIdentifierException;
import de.interactive_instruments.ShapeChange.Transformation.Profiling.ProfileIdentifier.IdentifierPattern;
import de.interactive_instruments.ShapeChange.UI.StatusBoard;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class ApplicationSchemaMetadata implements SingleTarget, MessageSource {

	public static final String NS = "http://shapechange.net/targets/ApplicationSchemaMetadata";

	public static int STATUS_RULE_ALL_IDENTIFY_PROFILES = 301500;

	/* ------------------------------------------- */
	/* --- configuration parameter identifiers --- */
	/* ------------------------------------------- */

	// none at present

	/* ------------------------ */
	/* --- rule identifiers --- */
	/* ------------------------ */

	/**
	 * This rule identifies the names of all the profiles to which elements
	 * (classes and properties) in selected schemas belong. Identified profiles
	 * are listed within ProfilesMetadata/containedProfile elements.
	 */
	public static final String RULE_ALL_IDENTIFY_PROFILES = "rule-asm-all-identify-profiles";

	/* --------------------- */
	/* --- tagged values --- */
	/* --------------------- */

	// none at present

	/* -------------------- */
	/* --- other fields --- */
	/* -------------------- */

	private static boolean initialised = false;

	protected static Document document = null;
	protected static Element root = null;
	protected static Map<String, ProcessMapEntry> mapEntryByType = new HashMap<String, ProcessMapEntry>();

	private static Comment hook;

	private static File outputDirectoryFile;
	private static String outputDirectory;
	private static String outputFilename;

	private static boolean printed = false;
	private static boolean diagnosticsOnly = false;

	private static String schemaTargetNamespace = null;

	private ShapeChangeResult result = null;
	private PackageInfo schemaPi = null;
	private Model model = null;
	private Options options = null;

	@Override
	public void initialise(PackageInfo p, Model m, Options o,
			ShapeChangeResult r, boolean diagOnly)
					throws ShapeChangeAbortException {

		schemaPi = p;
		schemaTargetNamespace = p.targetNamespace();

		model = m;
		options = o;
		result = r;
		diagnosticsOnly = diagOnly;

		result.addDebug(this, 1, schemaPi.name());

		if (!initialised) {
			initialised = true;

			outputDirectory = options.parameter(this.getClass().getName(),
					"outputDirectory");
			if (outputDirectory == null)
				outputDirectory = options.parameter("outputDirectory");
			if (outputDirectory == null)
				outputDirectory = options.parameter(".");

			outputFilename = "schema_metadata.xml";

			// Check if we can use the output directory; create it if it
			// does not exist
			outputDirectoryFile = new File(outputDirectory);
			boolean exi = outputDirectoryFile.exists();
			if (!exi) {
				try {
					FileUtils.forceMkdir(outputDirectoryFile);
				} catch (IOException e) {
					result.addError(null, 600, e.getMessage());
					e.printStackTrace(System.err);
				}
				exi = outputDirectoryFile.exists();
			}
			boolean dir = outputDirectoryFile.isDirectory();
			boolean wrt = outputDirectoryFile.canWrite();
			boolean rea = outputDirectoryFile.canRead();
			if (!exi || !dir || !wrt || !rea) {
				result.addFatalError(null, 601, outputDirectory);
				throw new ShapeChangeAbortException();
			}

			File outputFile = new File(outputDirectoryFile, outputFilename);

			// check if output file already exists - if so, attempt to delete it
			exi = outputFile.exists();
			if (exi) {

				result.addInfo(this, 3, outputFilename, outputDirectory);

				try {
					FileUtils.forceDelete(outputFile);
					result.addInfo(this, 4);
				} catch (IOException e) {
					result.addInfo(null, 600, e.getMessage());
					e.printStackTrace(System.err);
				}
			}

			// identify map entries defined in the target configuration
			List<ProcessMapEntry> mapEntries = options.getCurrentProcessConfig()
					.getMapEntries();

			if (mapEntries != null) {

				for (ProcessMapEntry pme : mapEntries) {
					mapEntryByType.put(pme.getType(), pme);
				}
			}

			// reset processed flags on all classes in the schema
			for (Iterator<ClassInfo> k = model.classes(schemaPi).iterator(); k
					.hasNext();) {
				ClassInfo ci = k.next();
				ci.processed(getTargetID(), false);
			}

			// ======================================
			// Parse configuration parameters
			// ======================================

			// nothing to do at present

			// ======================================
			// Set up the document and create root
			// ======================================

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			dbf.setValidating(true);
			dbf.setAttribute(Options.JAXP_SCHEMA_LANGUAGE,
					Options.W3C_XML_SCHEMA);
			DocumentBuilder db;
			try {
				db = dbf.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				result.addFatalError(null, 2);
				throw new ShapeChangeAbortException();
			}
			document = db.newDocument();

			root = document.createElementNS(NS, "ApplicationSchemaMetadata");
			document.appendChild(root);

			addAttribute(root, "xmlns", NS);

			hook = document.createComment(
					"Created by ShapeChange - http://shapechange.net/");
			root.appendChild(hook);
		}

		// create elements documenting the application schema
		Element e_name = document.createElement("name");
		e_name.setTextContent(schemaPi.name());
		Element e_tns = document.createElement("targetNamespace");
		e_tns.setTextContent(schemaPi.targetNamespace());

		Element e_as = document.createElement("ApplicationSchema");
		e_as.appendChild(e_name);
		e_as.appendChild(e_tns);

		Element e_schema = document.createElement("schema");
		e_schema.appendChild(e_as);

		root.appendChild(e_schema);

		/*
		 * Now compute relevant metadata.
		 */
		processMetadata(e_as);
	}

	protected void processMetadata(Element appSchemaElement) {

		if (schemaPi != null) {

			if (schemaPi.matches(RULE_ALL_IDENTIFY_PROFILES)) {

				StatusBoard.getStatusBoard()
						.statusChanged(STATUS_RULE_ALL_IDENTIFY_PROFILES);

				processProfilesMetadata(appSchemaElement);
			}

			// add additional rules to compute metadata here
		}

	}

	protected void processProfilesMetadata(Element appSchemaElement) {

		Set<Info> schemaElements = new TreeSet<Info>();
		
		// identify all classes and properties that belong to the schema
		SortedSet<ClassInfo> schemaClasses = model.classes(schemaPi);

		if (schemaClasses != null) {

			for (ClassInfo ci : schemaClasses) {
				schemaElements.add(ci);

				for (PropertyInfo pi : ci.properties().values()) {
					schemaElements.add(pi);
				}
			}
		}

		// identify the profiles of all relevant schema elements
		SortedSet<String> profileNames = new TreeSet<String>();

		for (Info i : schemaElements) {

			String[] profilesTVs = i.taggedValuesForTag("profiles");

			for (String profilesTV : profilesTVs) {

				if (profilesTV.trim().length() > 0) {

					try {

						ProfileIdentifierMap piMap = ProfileIdentifierMap.parse(
								profilesTV, IdentifierPattern.loose, i.name());

						profileNames.addAll(
								piMap.getProfileIdentifiersByName().keySet());

					} catch (MalformedProfileIdentifierException e) {
						result.addWarning(this, 9, profilesTV,
								i.fullNameInSchema(), e.getMessage());
					}
				}
			}
		}

		// now create the ProfilesMetadata XML element
		Element e_pm = document.createElement("ProfilesMetadata");

		for (String name : profileNames) {
			Element e_cp = document.createElement("containedProfile");
			e_cp.setTextContent(name);
			e_pm.appendChild(e_cp);
		}

		Element e_m = document.createElement("metadata");

		e_m.appendChild(e_pm);

		appSchemaElement.appendChild(e_m);
	}

	/** Add attribute to an element */
	protected void addAttribute(Element e, String name, String value) {
		Attr att = document.createAttribute(name);
		att.setValue(value);
		e.setAttributeNode(att);
	}

	@Override
	public void process(ClassInfo ci) {

		/*
		 * This target gathers metadata about an application schema as-is. As
		 * such, it can do so directly with the package provided by the
		 * initialize method. Ordering of classes is handled directly by this
		 * class.
		 * 
		 * NOTE: the Converter.STATUS_TARGET_PROCESS can therefore be ignored
		 * for this target.
		 */
	}

	@Override
	public void write() {

		// nothing to do here
	}

	@Override
	public int getTargetID() {
		return TargetIdentification.APP_SCHEMA_METADATA.getId();
	}

	@Override
	public String message(int mnr) {

		switch (mnr) {
		case 0:
			return "Context: class ApplicationSchemaMetadata";
		case 1:
			return "Retrieving metadata for application schema '$1$'.";
		case 2:
			return "XML Schema document with name '$1$' could not be created, invalid filename.";
		case 3:
			return "Could not write output to file '$1$'. Exception message is: $2$.";
		case 4:
			return "File has been deleted.";
		case 5:
			return ""; // unused (moved to ShapeChangeResult)
		case 6:
			return "Processing class '$1$'.";
		case 7:
			return "Class '$1$' is a $2$ which is not supported by this target. The class will be ignored.";
		case 8:
			return "Number format exception while converting the value of configuration parameter '$1$' to an integer. Exception message: $2$. The parameter will be ignored.";
		case 9:
			return "The profile identifier '$1$' in model element '$2$' is not well-formed: $3$";

		case 100:
			return "Context: property '$1$' in class '$2$'.";
		default:
			return "(Unknown message)";
		}
	}

	@Override
	public void writeAll(ShapeChangeResult r) {

		if (printed || diagnosticsOnly) {
			return;
		}

		Properties outputFormat = OutputPropertiesFactory
				.getDefaultMethodProperties("xml");
		outputFormat.setProperty("indent", "yes");
		outputFormat.setProperty("{http://xml.apache.org/xalan}indent-amount",
				"2");
		outputFormat.setProperty("encoding", "UTF-8");

		/*
		 * Uses OutputStreamWriter instead of FileWriter to set character
		 * encoding (see doc in Serializer.setWriter and FileWriter)
		 */
		try {
			File repXsd = new File(outputDirectoryFile, outputFilename);

			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(repXsd), "UTF-8"));

			Serializer serializer = SerializerFactory
					.getSerializer(outputFormat);
			serializer.setWriter(writer);
			serializer.asDOMSerializer().serialize(document);

			writer.close();

			r.addResult(getTargetID(), outputDirectory, outputFilename,
					schemaTargetNamespace);

		} catch (IOException ioe) {

			r.addError(this, 3, outputFilename, ioe.getMessage());
		}

		printed = true;
	}

	@Override
	public void reset() {

		initialised = false;

		schemaTargetNamespace = null;

		outputDirectoryFile = null;
		outputDirectory = null;
		outputFilename = null;

		printed = false;
		diagnosticsOnly = false;

		document = null;
		root = null;
		hook = null;
		mapEntryByType = new HashMap<String, ProcessMapEntry>();
	}

}
