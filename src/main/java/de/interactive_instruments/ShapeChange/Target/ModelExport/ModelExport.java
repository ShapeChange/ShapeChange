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
 * (c) 2002-2017 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target.ModelExport;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.Model.Descriptor;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.Model.AssociationInfo;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.Constraint.ModelElmtContextType;
import de.interactive_instruments.ShapeChange.Model.LangString;
import de.interactive_instruments.ShapeChange.Model.Descriptors;
import de.interactive_instruments.ShapeChange.Model.FolConstraint;
import de.interactive_instruments.ShapeChange.Model.ImageMetadata;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.OclConstraint;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Model.Qualifier;
import de.interactive_instruments.ShapeChange.Model.Stereotypes;
import de.interactive_instruments.ShapeChange.Model.TaggedValues;
import de.interactive_instruments.ShapeChange.Model.TextConstraint;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel;
import de.interactive_instruments.ShapeChange.Profile.ProfileUtil;
import de.interactive_instruments.ShapeChange.Profile.ProfileIdentifier;
import de.interactive_instruments.ShapeChange.Profile.Profiles;
import de.interactive_instruments.ShapeChange.Profile.VersionRange;
import de.interactive_instruments.ShapeChange.Target.SingleTarget;
import de.interactive_instruments.ShapeChange.Util.XMLWriter;
import de.interactive_instruments.ShapeChange.Util.ZipHandler;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments <dot>
 *         de)
 *
 */
public class ModelExport implements SingleTarget, MessageSource {

	public static final String NS = "http://shapechange.net/model";

	private static boolean initialised = false;

	private static String outputDirectory = null;
	private static String outputFilename = null;
	private static String encoding = null;
	private static XMLWriter writer = null;
	private static File outputXmlFile = null;

	private static Model model = null;
	private static SortedSet<PackageInfo> allSelectedSchemaPackages = null;

	private static Set<String> profilesToExport = null;
	private static boolean omitExistingProfiles = false;
	private static Pattern ignoreTaggedValuesPattern = null;
	private static boolean exportProfilesFromWholeModel = false;
	private static boolean zipOutput = false;
	private static String schemaLocation = ModelExportConstants.DEFAULT_SCHEMA_LOCATION;

	private Options options = null;
	private ShapeChangeResult result = null;

	private SortedSet<AssociationInfo> associations = new TreeSet<AssociationInfo>();

	// set buffer size for streams (in bytes)
	private int streamBufferSize = 8 * 1042;

	@Override
	public void initialise(PackageInfo p, Model m, Options o,
			ShapeChangeResult r, boolean diagOnly)
			throws ShapeChangeAbortException {

		options = o;
		result = r;

		try {

			if (!initialised) {

				initialised = true;

				model = m;

				allSelectedSchemaPackages = model
						.allPackagesFromSelectedSchemas();

				outputDirectory = options.parameter(ModelExport.class.getName(),
						"outputDirectory");
				if (outputDirectory == null)
					outputDirectory = options.parameter("outputDirectory");
				if (outputDirectory == null)
					outputDirectory = ".";

				outputFilename = options.parameter(ModelExport.class.getName(),
						"outputFilename");
				if (outputFilename == null)
					outputFilename = "ModelExport";

				encoding = m.characterEncoding();

				String xmlName = outputFilename + ".xml";

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
					result.addFatalError(this, 12, outputDirectory);
					throw new ShapeChangeAbortException();
				}

				String encoding_ = encoding == null ? "UTF-8"
						: m.characterEncoding();
				// String encoding_ = "UTF-8";

				outputXmlFile = new File(outputDirectory + "/" + xmlName);

				OutputStream fout = new FileOutputStream(outputXmlFile);
				OutputStream bout = new BufferedOutputStream(fout,
						streamBufferSize);
				OutputStreamWriter outputXML = new OutputStreamWriter(bout,
						encoding_);

				writer = new XMLWriter(outputXML, encoding_);

				if (p.matches(
						ModelExportConstants.RULE_TGT_EXP_ALL_RESTRICT_EXISTING_PROFILES)) {
					profilesToExport = new HashSet<String>(options
							.parameterAsStringList(ModelExport.class.getName(),
									ModelExportConstants.PARAM_PROFILES_TO_EXPORT,
									null, true, true));
				}

				omitExistingProfiles = p.matches(
						ModelExportConstants.RULE_TGT_EXP_ALL_OMIT_EXISTING_PROFILES);

				try {
					ignoreTaggedValuesPattern = Pattern.compile(options
							.parameterAsString(ModelExport.class.getName(),
									ModelExportConstants.PARAM_IGNORE_TAGGED_VALUES_REGEX,
									ModelExportConstants.DEFAULT_IGNORE_TAGGED_VALUES_REGEX,
									true, false));
				} catch (PatternSyntaxException e) {
					result.addError(this, 11,
							ModelExportConstants.PARAM_IGNORE_TAGGED_VALUES_REGEX,
							e.getMessage());
					ignoreTaggedValuesPattern = Pattern.compile(
							ModelExportConstants.DEFAULT_IGNORE_TAGGED_VALUES_REGEX);
				}

				exportProfilesFromWholeModel = options.parameterAsBoolean(
						ModelExport.class.getName(),
						ModelExportConstants.PARAM_EXPORT_PROFILES_FROM_WHOLE_MODEL,
						false);

				zipOutput = options.parameterAsBoolean(
						ModelExport.class.getName(),
						ModelExportConstants.PARAM_ZIP_OUTPUT, false);

				schemaLocation = options.parameterAsString(
						ModelExport.class.getName(),
						ModelExportConstants.PARAM_SCHEMA_LOCATION,
						ModelExportConstants.DEFAULT_SCHEMA_LOCATION, false,
						true);

				boolean profilesInModelSetExplicitly = options
						.parameterAsBoolean(ModelExport.class.getName(),
								ModelExportConstants.PARAM_MODEL_EXPLICIT_PROFILES,
								true);

				if (!omitExistingProfiles && !profilesInModelSetExplicitly) {

					// We need to convert the profile definitions in the model
					SortedSet<String> profilesForClassesWithoutExplicitProfiles = null;

					if (options.hasParameter(ModelExport.class.getName(),
							ModelExportConstants.PARAM_PROFILES_FOR_CLASSES_WITHOUT_EXPLICIT_PROFILES)) {

						profilesForClassesWithoutExplicitProfiles = new TreeSet<String>(
								options.parameterAsStringList(
										ModelExport.class.getName(),
										ModelExportConstants.PARAM_PROFILES_FOR_CLASSES_WITHOUT_EXPLICIT_PROFILES,
										null, true, true));

					} else {

						// gather the names of profiles from the model
						profilesForClassesWithoutExplicitProfiles = ProfileUtil
								.findNamesOfAllProfiles(m,
										exportProfilesFromWholeModel);
					}

					if (!profilesForClassesWithoutExplicitProfiles.isEmpty()) {

						Profiles profilesForClassesBelongingToAllProfiles = new Profiles();
						for (String profileName : profilesForClassesWithoutExplicitProfiles) {
							profilesForClassesBelongingToAllProfiles
									.put(profileName);
						}

						/*
						 * Convert model to one with explicit profile
						 * definitions and set it as the model to process by
						 * this target
						 */
						GenericModel genModel = new GenericModel(m);
						ProfileUtil.convertToExplicitProfileDefinitions(
								genModel,
								profilesForClassesBelongingToAllProfiles, null,
								exportProfilesFromWholeModel);

						/*
						 * Postprocessing and validation of the generic model
						 * should not be necessary. For example, constraints do
						 * not need to be parsed again, since the export does
						 * not require parsed statements, just the constraint
						 * text (as well as name etc).
						 */
						// genModel.postprocessAfterLoadingAndValidate();
						model = genModel;

					} else {
						/*
						 * If no profiles are available to set for classes and
						 * properties that belong to all profiles, we can just
						 * as well pretend that the input model uses explicit
						 * profile settings
						 */
					}
				}
			}

		} catch (Exception e) {

			String msg = e.getMessage();
			if (msg != null) {
				result.addError(msg);
			}
			e.printStackTrace(System.err);
		}
	}

	@Override
	public void reset() {

		initialised = false;

		writer = null;

		model = null;
		allSelectedSchemaPackages = null;

		outputDirectory = null;
		outputFilename = null;
		outputXmlFile = null;

		encoding = null;

		profilesToExport = null;
		omitExistingProfiles = false;
		ignoreTaggedValuesPattern = null;
		exportProfilesFromWholeModel = false;
		zipOutput = false;
		schemaLocation = ModelExportConstants.DEFAULT_SCHEMA_LOCATION;
	}

	@Override
	public void process(ClassInfo ci) {
		// nothing to do here, since we'll get all classes from the schemas
	}

	@Override
	public void write() {
		// nothing to do here, since this is a SingleTarget
	}

	@Override
	public String getTargetName() {
		return "Model Export";
	}

	@Override
	public void writeAll(ShapeChangeResult r) {

		result = r;
		options = r.options();

		try {
			writer.forceNSDecl("http://www.w3.org/2001/XMLSchema-instance",
					"xsi");
			writer.forceNSDecl(NS, "sc");
			writer.startDocument();

			AttributesImpl atts = new AttributesImpl();
			atts.addAttribute("http://www.w3.org/2001/XMLSchema-instance",
					"schemaLocation", "xsi:schemaLocation", "CDATA",
					NS + " " + schemaLocation);
			atts.addAttribute("", "encoding", "", "string",
					model.characterEncoding());
			writer.startElement(NS, "Model", "", atts);

			SortedSet<PackageInfo> packagesToPrint = new TreeSet<PackageInfo>();

			for (PackageInfo pi : model.packages()) {
				if (pi.owner() == null) {
					packagesToPrint.add(pi);
				}
			}

			writer.startElement(NS, "packages");
			for (PackageInfo pi : packagesToPrint) {
				printPackage(pi);
			}
			writer.endElement(NS, "packages");

			/*
			 * finally, print all associations; navigable roles have been
			 * printed as part of the classes they are in, non-navigable roles
			 * need to be printed within the association
			 */
			if (!associations.isEmpty()) {
				writer.startElement(NS, "associations");
				for (AssociationInfo ai : associations) {
					printAssociation(ai);
				}
				writer.endElement(NS, "associations");
			}

			writer.endElement(NS, "Model");
			writer.endDocument();
			writer.close();

			if (zipOutput) {

				File outputZipFile = new File(
						outputDirectory + "/" + outputFilename + ".zip");
				ZipHandler.zipFile(outputXmlFile, outputZipFile);
			}

		} catch (Exception e) {

			String m = e.getMessage();
			if (m != null) {
				result.addError(m);
			}
			e.printStackTrace(System.err);

		} finally {

			// close writer
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					String m = e.getMessage();
					if (m != null) {
						result.addError(m);
					}
					e.printStackTrace(System.err);
				}
			}

			// release model - do NOT close it here
			model = null;
		}
	}

	private void printAssociation(AssociationInfo ai) throws Exception {

		writer.startElement(NS, "Association");

		printInfoFields(ai);

		if (ai.assocClass() != null) {
			writer.dataElement(NS, "assocClassId", ai.assocClass().id());
		}

		printAssociationRole(ai.end1(), "end1");
		printAssociationRole(ai.end2(), "end2");

		writer.endElement(NS, "Association");
	}

	private void printAssociationRole(PropertyInfo pi, String tag)
			throws SAXException {

		if (!pi.isAttribute()) {

			if (pi.isNavigable()) {
				// then it has already been encoded as part of a class
				AttributesImpl atts = new AttributesImpl();
				atts.addAttribute("", "ref", "", "string", pi.id());
				writer.emptyElement(NS, tag, "", atts);
			} else {
				writer.startElement(NS, tag);
				printProperty(pi, true);
				writer.endElement(NS, tag);
			}
		}
	}

	private void printPackage(PackageInfo pi) throws Exception {

		if (pi.matches(ModelExportConstants.RULE_TGT_EXP_PKG_ALL_EDITABLE)
				|| allSelectedSchemaPackages.contains(pi)) {
			writer.startElement(NS, "Package");
		} else {
			writer.startElement(NS, "Package", "editable", "false");
		}

		printInfoFields(pi);

		if (pi.supplierIds() != null && !pi.supplierIds().isEmpty()) {

			writer.startElement(NS, "supplierIds");

			for (String supplierId : pi.supplierIds()) {
				writer.dataElement(NS, "SupplierId", supplierId);
			}

			writer.endElement(NS, "supplierIds");
		}

		if (pi.containedClasses() != null && !pi.containedClasses().isEmpty()) {
			writer.startElement(NS, "classes");
			for (ClassInfo ci : pi.containedClasses()) {
				printClass(ci);
			}
			writer.endElement(NS, "classes");
		}

		if (pi.containedPackages() != null
				&& !pi.containedPackages().isEmpty()) {

			writer.startElement(NS, "packages");

			for (PackageInfo pi2 : pi.containedPackages()) {
				printPackage(pi2);
			}

			writer.endElement(NS, "packages");
		}

		printDiagrams(pi.getDiagrams());

		writer.endElement(NS, "Package");

	}

	/**
	 * Creates an element with the given name, containing the given boolean as
	 * value ('true' or 'false') - if and only if the given boolean does not
	 * have the same value as the given default value.
	 * 
	 * @param elementName
	 * @param value
	 * @param defaultValue
	 * @throws SAXException
	 */
	private void printDataElement(String elementName, boolean value,
			boolean defaultValue) throws SAXException {

		// java logical XOR operator is: ^
		if (value ^ defaultValue) {
			printDataElement(elementName, "" + value);
		}
	}

	/**
	 * Creates an element with the given name, containing the given string as
	 * value - if and only if the given string is not <code>null</code>, has a
	 * length greater than 0, and does not equal (ignoring case!) the given
	 * default value.
	 * 
	 * @param elementName
	 * @param value
	 * @param defaultValue
	 * @throws SAXException
	 */
	private void printDataElement(String elementName, String value,
			String defaultValue) throws SAXException {

		if (value != null && value.length() > 0
				&& !value.equalsIgnoreCase(defaultValue)) {
			writer.dataElement(NS, elementName, value);
		}
	}

	private void printDiagrams(List<ImageMetadata> diagrams)
			throws SAXException {

		if (diagrams != null && !diagrams.isEmpty()) {

			writer.startElement(NS, "diagrams");

			for (ImageMetadata im : diagrams) {

				writer.startElement(NS, "ImageMetadata");
				/*
				 * NOTE: image metadata IDs have prefix "img", so no need to add
				 * a prefix to prevent a number as first character of the id
				 */
				writer.dataElement(NS, "id", im.getId());
				writer.dataElement(NS, "name", im.getName());
				// TBD: system independent path?
				writer.dataElement(NS, "file", im.getFile().getAbsolutePath());
				writer.dataElement(NS, "relPathToFile", im.getRelPathToFile());
				writer.dataElement(NS, "width", "" + im.getWidth());
				writer.dataElement(NS, "height", "" + im.getHeight());
				writer.endElement(NS, "ImageMetadata");
			}

			writer.endElement(NS, "diagrams");
		}
	}

	private void printClass(ClassInfo ci) throws Exception {

		writer.startElement(NS, "Class");

		printInfoFields(ci);

		printDiagrams(ci.getDiagrams());

		printDataElement("isAbstract", ci.isAbstract(), false);
		printDataElement("isLeaf", ci.isLeaf(), false);

		if (ci.isAssocClass() != null) {
			printDataElement("associationId", ci.isAssocClass().id());
		}
		if (ci.baseClass() != null) {
			printDataElement("baseClassId", ci.baseClass().id());
		}

		if (exportProfilesFromWholeModel
				|| allSelectedSchemaPackages.contains(ci.pkg())) {
			printProfiles(ci.profiles());
		}

		if (!ci.supertypes().isEmpty()) {

			writer.startElement(NS, "supertypes");

			for (String sid : ci.supertypes()) {
				writer.dataElement(NS, "SupertypeId", sid);
			}

			writer.endElement(NS, "supertypes");
		}

		if (!ci.subtypes().isEmpty()) {

			writer.startElement(NS, "subtypes");

			for (String sid : ci.subtypes()) {
				writer.dataElement(NS, "SubtypeId", sid);
			}

			writer.endElement(NS, "subtypes");
		}

		printConstraints(ci.constraints());

		if (!ci.properties().isEmpty() && ci.properties().values().stream()
				.anyMatch(property -> property.isNavigable())) {

			writer.startElement(NS, "properties");
			for (PropertyInfo pi : ci.properties().values()) {
				/*
				 * 20180907 JE: ClassInfo.properties() should only return
				 * navigable properties. However, we've had the case that this
				 * contract was not fulfilled by an external model
				 * implementation. Therefore, we enforce the contract here.
				 */
				if (pi.isNavigable()) {
					printProperty(pi, false);
				}
			}
			writer.endElement(NS, "properties");
		}

		// TODO print operations?

		writer.endElement(NS, "Class");
	}

	private void printProfiles(Profiles profiles) throws SAXException {

		if (!omitExistingProfiles && !profiles.isEmpty()) {

			/*
			 * Determine which profiles to export; first create copy of the set
			 * of profile identifier names from the given set of profiles so
			 * that we can safely apply the retainAll operation on it.
			 */
			Set<String> ptexp = new HashSet<String>(
					profiles.getProfileIdentifiersByName().keySet());
			if (profilesToExport != null) {
				ptexp.retainAll(profilesToExport);
			}

			if (!ptexp.isEmpty()) {

				writer.startElement(NS, "profiles");

				for (ProfileIdentifier pi : profiles
						.getProfileIdentifiersByName().values()) {

					if (!ptexp.contains(pi.getName())) {
						/*
						 * this particular profile is not a member of the set of
						 * profiles to export
						 */
						continue;
					}

					AttributesImpl atts = new AttributesImpl();
					atts.addAttribute("", "name", "", "string", pi.getName());

					if (pi.hasVersionIndicator() || pi.hasParameters()) {

						writer.startElement(NS, "Profile", "", atts);

						if (pi.hasVersionIndicator()) {

							writer.startElement(NS, "versionIdentifier");

							for (VersionRange versionRange : pi
									.getVersionIndicator().getVersionInfos()) {

								AttributesImpl attsVersionRange = new AttributesImpl();
								attsVersionRange.addAttribute("", "begin", "",
										"string",
										versionRange.getBegin().toString());
								attsVersionRange.addAttribute("", "end", "",
										"string",
										versionRange.getEnd().toString());

								writer.emptyElement(NS, "VersionRange", "",
										attsVersionRange);
							}

							writer.endElement(NS, "versionIdentifier");
						}

						if (pi.hasParameters()) {

							writer.startElement(NS, "parameter");

							for (Entry<String, String> parameterEntry : pi
									.getParameter().entrySet()) {

								AttributesImpl attsParameter = new AttributesImpl();
								attsParameter.addAttribute("", "name", "",
										"string", parameterEntry.getKey());
								if (parameterEntry.getValue() != null) {
									attsParameter.addAttribute("", "value", "",
											"string",
											parameterEntry.getValue());
								}

								writer.emptyElement(NS, "ProfileParameter", "",
										attsParameter);
							}

							writer.endElement(NS, "parameter");
						}

						writer.endElement(NS, "Profile");

					} else {

						writer.emptyElement(NS, "Profile", "", atts);
					}
				}

				writer.endElement(NS, "profiles");
			}

		}
	}

	private void printConstraints(List<Constraint> constraints)
			throws SAXException {

		if (constraints != null && !constraints.isEmpty()) {

			writer.startElement(NS, "constraints");

			for (Constraint con : constraints) {

				String elementName;
				String type = null;
				String sourceType = null;

				if (con instanceof FolConstraint) {

					elementName = "FolConstraint";
					sourceType = ((FolConstraint) con).sourceType();

				} else if (con instanceof OclConstraint) {

					elementName = "OclConstraint";

				} else {

					elementName = "TextConstraint";
					type = ((TextConstraint) con).type();
				}

				writer.startElement(NS, elementName);

				printDataElement("name", con.name());
				printDataElement("status", con.status());
				printDataElement("text", con.text());

				String contextModelElmtId;
				String contextModelElmtType;
				if (con.contextModelElmtType() == ModelElmtContextType.ATTRIBUTE) {
					contextModelElmtType = "ATTRIBUTE";
					contextModelElmtId = con.contextModelElmt().id();
				} else {
					contextModelElmtType = "CLASS";
					contextModelElmtId = con.contextModelElmt().id();
				}
				printDataElement("contextModelElementId", contextModelElmtId);
				printDataElement("contextModelElementType",
						contextModelElmtType, "CLASS");

				if (type != null) {
					printDataElement("type", type);
				}
				printDataElement("sourceType", sourceType);

				writer.endElement(NS, elementName);

				// comments and expressions will be created when validating
				// the constraint
			}

			writer.endElement(NS, "constraints");
		}
	}

	private void printProperty(PropertyInfo pi, boolean printInClass)
			throws SAXException {

		writer.startElement(NS, "Property");

		printInfoFields(pi);

		if (exportProfilesFromWholeModel
				|| allSelectedSchemaPackages.contains(pi.inClass().pkg())) {
			printProfiles(pi.profiles());
		}

		if (!(pi.cardinality().minOccurs == 1
				&& pi.cardinality().maxOccurs == 1)) {
			printDataElement("cardinality", pi.cardinality().toString());
		}

		printDataElement("isNavigable", pi.isNavigable(), true);
		printDataElement("sequenceNumber", pi.sequenceNumber().getString());

		Type ti = pi.typeInfo();

		if (ti != null) {

			ClassInfo ci = pi.model().classById(ti.id);
			if (ci != null) {
				printDataElement("typeId", ti.id);
			}
			if (StringUtils.isNotBlank(ti.name)) {
				printDataElement("typeName", ti.name);
			}
		}

		printDataElement("isDerived", pi.isDerived(), false);
		printDataElement("isReadOnly", pi.isReadOnly(), false);
		printDataElement("isAttribute", pi.isAttribute(), true);

		if (pi.isOrdered() && pi.matches(
				ModelExportConstants.RULE_TGT_EXP_PROP_SUPPRESS_MEANINGLESS_CODE_ENUM_CHARACTERISTICS)) {
			result.addDebug(this, 13, "isOrdered", "true",
					pi.fullNameInSchema());
		} else {
			printDataElement("isOrdered", pi.isOrdered(), false);
		}

		if (!pi.isUnique() && pi.matches(
				ModelExportConstants.RULE_TGT_EXP_PROP_SUPPRESS_MEANINGLESS_CODE_ENUM_CHARACTERISTICS)) {
			result.addDebug(this, 13, "isUnique", "false",
					pi.fullNameInSchema());
		} else {
			printDataElement("isUnique", pi.isUnique(), true);
		}

		if (pi.isComposition() && pi.matches(
				ModelExportConstants.RULE_TGT_EXP_PROP_SUPPRESS_MEANINGLESS_CODE_ENUM_CHARACTERISTICS)) {
			result.addDebug(this, 13, "isComposition", "true",
					pi.fullNameInSchema());
		} else {
			printDataElement("isComposition", pi.isComposition(), false);
		}
		if (pi.isAggregation() && pi.matches(
				ModelExportConstants.RULE_TGT_EXP_PROP_SUPPRESS_MEANINGLESS_CODE_ENUM_CHARACTERISTICS)) {
			result.addDebug(this, 13, "isAggregation", "true",
					pi.fullNameInSchema());
		} else {
			printDataElement("isAggregation", pi.isAggregation(), false);
		}

		printDataElement("initialValue", pi.initialValue());
		printDataElement("inlineOrByReference", pi.inlineOrByReference(),
				"inlineOrByReference");

		if (pi.qualifiers() != null && !pi.qualifiers().isEmpty()) {
			writer.startElement(NS, "qualifiers");
			for (Qualifier qualifier : pi.qualifiers()) {
				writer.startElement(NS, "Qualifier");
				printDataElement("name", qualifier.name);
				printDataElement("type", qualifier.type);
				writer.endElement(NS, "Qualifier");
			}
			writer.endElement(NS, "qualifiers");
		}

		if (printInClass && pi.inClass() != null) {
			printDataElement("inClassId", pi.inClass().id());
		}

		if (pi.reverseProperty() != null) {
			printDataElement("reversePropertyId", pi.reverseProperty().id());
		}

		if (pi.association() != null) {
			if (pi.isNavigable()) {
				/*
				 * only keep track of the association for navigable role;
				 * non-navigable role will be printed when printing the
				 * association
				 */
				associations.add(pi.association());
			}
			printDataElement("associationId", pi.association().id());
		}

		printConstraints(pi.constraints());

		writer.endElement(NS, "Property");
	}

	private void printInfoFields(Info i) throws SAXException {

		printDataElement("name", i.name());
		writer.dataElement(NS, "id", i.id());

		Stereotypes st = i.stereotypes();
		if (st != null && !st.isEmpty()) {
			writer.startElement(NS, "stereotypes");
			for (String s : st.asArray()) {
				writer.dataElement(NS, "Stereotype", s);
			}
			writer.endElement(NS, "stereotypes");
		}

		// print descriptors - if not empty
		Descriptors descriptors = i.descriptors();
		if (!descriptors.isEmpty()) {
			writer.startElement(NS, "descriptors");
			printDescriptorElement(Descriptor.ALIAS, descriptors);
			printDescriptorElement(Descriptor.PRIMARYCODE, descriptors);
			printDescriptorElement(Descriptor.GLOBALIDENTIFIER, descriptors);
			printDescriptorElement(Descriptor.DEFINITION, descriptors);
			printDescriptorElement(Descriptor.DESCRIPTION, descriptors);
			/*
			 * TBD: We could add a parameter to list the descriptors that shall
			 * be exported
			 */
			// printDescriptorElement(Descriptor.DOCUMENTATION, descriptors);
			printDescriptorElement(Descriptor.LEGALBASIS, descriptors);
			printDescriptorElement(Descriptor.LANGUAGE, descriptors);
			printDescriptorElement(Descriptor.EXAMPLE, descriptors);
			printDescriptorElement(Descriptor.DATACAPTURESTATEMENT,
					descriptors);
			writer.endElement(NS, "descriptors");
		}

		TaggedValues tvs = i.taggedValuesAll();

		// identify set of tagged values to export
		TaggedValues tvsToExport = options.taggedValueFactory();
		for (String tagName : tvs.keySet()) {
			if (!ignoreTaggedValuesPattern.matcher(tagName).matches()) {
				tvsToExport.put(tagName, tvs.get(tagName));
			}
		}

		if (!tvsToExport.isEmpty()) {

			writer.startElement(NS, "taggedValues");

			for (String tagName : tvsToExport.keySet()) {

				String[] values = tvsToExport.get(tagName);
				if (values != null && values.length > 0) {
					writer.startElement(NS, "TaggedValue");
					writer.dataElement(NS, "name", tagName);
					if (values.length == 1
							&& (values[0] == null || values[0].length() == 0)) {
						// ignore empty value
					} else {
						writer.startElement(NS, "values");
						for (String value : values) {
							writer.dataElement(NS, "Value", value);
						}
						writer.endElement(NS, "values");
					}
					writer.endElement(NS, "TaggedValue");
				}
			}

			writer.endElement(NS, "taggedValues");
		}
	}

	private void printDescriptorElement(Descriptor descriptor,
			Descriptors descriptors) throws SAXException {

		List<LangString> descriptorValues_tmp = descriptors.values(descriptor);

		/*
		 * Ignore values that only contain whitespace. Empty values would lead
		 * to validation errors. This is especially relevant for descriptors
		 * that have the empty string as value by default, even if no source
		 * defines it; an example is the descriptor 'definition'.
		 */
		List<LangString> descriptorValues = new ArrayList<LangString>();

		for (LangString dv : descriptorValues_tmp) {
			if (dv.getValue().trim().length() > 0) {
				descriptorValues.add(dv);
			}
		}

		if (!descriptorValues.isEmpty()) {

			writer.startElement(NS, descriptor.getName());
			writer.startElement(NS, "descriptorValues");

			for (LangString dv : descriptorValues) {

				if (dv.hasLang()) {

					printDataElement("DescriptorValue", dv.getValue(), "lang",
							dv.getLang());

				} else {

					printDataElement("DescriptorValue", dv.getValue());
				}
			}

			writer.endElement(NS, "descriptorValues");
			writer.endElement(NS, descriptor.getName());
		}
	}

	/**
	 * Creates an element with the given name, containing the given string as
	 * value - if and only if the given string is not <code>null</code> and has
	 * a length greater than 0.
	 * 
	 * @param elementName
	 * @param s
	 * @throws SAXException
	 */
	private void printDataElement(String elementName, String s)
			throws SAXException {

		if (s != null && s.length() > 0) {
			writer.dataElement(NS, elementName, s);
		}
	}

	/**
	 * Creates an element with the given name, containing the given string as
	 * value and having an attribute with given name and value - if and only if
	 * the given string is not <code>null</code> and has a length greater than
	 * 0.
	 * 
	 * @param elementName
	 * @param elementContent
	 * @param attributeName
	 * @param attributeContent
	 * @throws SAXException
	 */
	private void printDataElement(String elementName, String elementContent,
			String attributeName, String attributeValue) throws SAXException {

		if (elementContent != null && elementContent.length() > 0) {
			writer.dataElement(NS, elementName, elementContent, attributeName,
					attributeValue);
		}
	}

	@Override
	public String message(int mnr) {

		switch (mnr) {

		case 11:
			return "Syntax exception while compiling the regular expression defined by target parameter '$1$': '$2$'. The default will be used.";
		case 12:
			return "Directory named '$1$' does not exist or is not accessible.";
		case 13:
			return "Suppressing semantically meaningless characteristic '$1$' (with value '$2$') of code/enum '$3$'.";

		default:
			return "(ModelExport.java) Unknown message with number: " + mnr;
		}

	}
}
