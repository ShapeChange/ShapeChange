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
 * (c) 2002-2022 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Model.Writer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.Model.AssociationInfo;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.Descriptor;
import de.interactive_instruments.ShapeChange.Model.Descriptors;
import de.interactive_instruments.ShapeChange.Model.FolConstraint;
import de.interactive_instruments.ShapeChange.Model.ImageMetadata;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.LangString;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.OclConstraint;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Model.Qualifier;
import de.interactive_instruments.ShapeChange.Model.Stereotypes;
import de.interactive_instruments.ShapeChange.Model.TaggedValues;
import de.interactive_instruments.ShapeChange.Model.TextConstraint;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel;
import de.interactive_instruments.ShapeChange.Profile.ProfileIdentifier;
import de.interactive_instruments.ShapeChange.Profile.ProfileUtil;
import de.interactive_instruments.ShapeChange.Profile.Profiles;
import de.interactive_instruments.ShapeChange.Profile.VersionRange;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.Target.ModelExport.ModelExportConstants;
import de.interactive_instruments.ShapeChange.Util.XMLWriter;
import de.interactive_instruments.ShapeChange.Util.ZipHandler;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class ModelWriter extends AbstractModelWriter {

    protected boolean allPackagesAreEditable = false;
    private SortedSet<PackageInfo> allSelectedSchemaPackages = null;

    protected Set<String> profilesToExport = null;
    protected boolean omitExistingProfiles = false;
    protected Pattern ignoreTaggedValuesPattern = null;
    protected boolean exportProfilesFromWholeModel = false;
    protected boolean includeConstraintDescriptions = false;
    protected boolean suppressCodeAndEnumCharacteristicsWithoutSemanticMeaning = false;

    protected boolean profilesInModelSetExplicitly;
    protected SortedSet<String> defaultProfilesForClassesWithoutExplicitProfiles;

    public ModelWriter(Options o, ShapeChangeResult r, String encoding, File outputXmlFile,
	    Set<String> profilesToExport, boolean omitExistingProfiles, Pattern ignoreTaggedValuesPattern,
	    boolean exportProfilesFromWholeModel, boolean includeConstraintDescriptions,
	    boolean suppressCodeAndEnumCharacteristicsWithoutSemanticMeaning, boolean zipOutput, String schemaLocation,
	    boolean profilesInModelSetExplicitly, SortedSet<String> defaultProfilesForClassesWithoutExplicitProfiles,
	    boolean allPackagesAreEditable) {

	super(o, r, encoding, outputXmlFile, zipOutput, schemaLocation);

	this.profilesToExport = profilesToExport;
	this.omitExistingProfiles = omitExistingProfiles;
	this.ignoreTaggedValuesPattern = ignoreTaggedValuesPattern;
	this.exportProfilesFromWholeModel = exportProfilesFromWholeModel;
	this.includeConstraintDescriptions = includeConstraintDescriptions;
	this.suppressCodeAndEnumCharacteristicsWithoutSemanticMeaning = suppressCodeAndEnumCharacteristicsWithoutSemanticMeaning;

	this.profilesInModelSetExplicitly = profilesInModelSetExplicitly;
	this.defaultProfilesForClassesWithoutExplicitProfiles = defaultProfilesForClassesWithoutExplicitProfiles;
	this.allPackagesAreEditable = allPackagesAreEditable;

	try {

	    OutputStream fout = new FileOutputStream(outputXmlFile);
	    OutputStream bout = new BufferedOutputStream(fout, streamBufferSize);
	    OutputStreamWriter outputXML = new OutputStreamWriter(bout, this.encoding);

	    writer = new XMLWriter(outputXML, this.encoding);

	} catch (Exception e) {

	    // TODO

	    String msg = e.getMessage();
	    if (msg != null) {
		result.addError(msg);
	    }
	    e.printStackTrace(System.err);
	}
    }

    public ModelWriter(XMLWriter writer, Options o, ShapeChangeResult r, Set<String> profilesToExport,
	    boolean omitExistingProfiles, Pattern ignoreTaggedValuesPattern, boolean exportProfilesFromWholeModel,
	    boolean includeConstraintDescriptions, boolean suppressCodeAndEnumCharacteristicsWithoutSemanticMeaning,
	    boolean profilesInModelSetExplicitly, SortedSet<String> defaultProfilesForClassesWithoutExplicitProfiles,
	    boolean allPackagesAreEditable) {

	super(o, r);

	this.writer = writer;
	
	this.profilesToExport = profilesToExport;
	this.omitExistingProfiles = omitExistingProfiles;
	this.ignoreTaggedValuesPattern = ignoreTaggedValuesPattern;
	this.exportProfilesFromWholeModel = exportProfilesFromWholeModel;
	this.includeConstraintDescriptions = includeConstraintDescriptions;
	this.suppressCodeAndEnumCharacteristicsWithoutSemanticMeaning = suppressCodeAndEnumCharacteristicsWithoutSemanticMeaning;
	this.profilesInModelSetExplicitly = profilesInModelSetExplicitly;
	this.defaultProfilesForClassesWithoutExplicitProfiles = defaultProfilesForClassesWithoutExplicitProfiles;
	this.allPackagesAreEditable = allPackagesAreEditable;
    }

    protected void printAssociation(AssociationInfo ai) throws Exception {

	if (ai != null && ai.end1() != null && ai.end2() != null) {

	    writer.startElement(NS, "Association");

	    printInfoFields(ai);

	    if (ai.assocClass() != null) {
		writer.dataElement(NS, "assocClassId", ai.assocClass().id());
	    }

	    printAssociationRole(ai.end1(), "end1");
	    printAssociationRole(ai.end2(), "end2");

	    writer.endElement(NS, "Association");
	}
    }

    protected void printAssociationRole(PropertyInfo pi, String tag) throws SAXException {

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

    protected void printPackage(PackageInfo pi) throws Exception {

	if (allPackagesAreEditable || allSelectedSchemaPackages.contains(pi)) {
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

	if (pi.containedPackages() != null && !pi.containedPackages().isEmpty()) {

	    writer.startElement(NS, "packages");

	    for (PackageInfo pi2 : pi.containedPackages()) {
		printPackage(pi2);
	    }

	    writer.endElement(NS, "packages");
	}

	printDiagrams(pi.getDiagrams());

	writer.endElement(NS, "Package");

    }

    protected void printClass(ClassInfo ci) throws Exception {

	writer.startElement(NS, "Class");

	printInfoFields(ci);

	printDiagrams(ci.getDiagrams());

	printDataElement("isAbstract", ci.isAbstract(), false);
	printDataElement("isLeaf", ci.isLeaf(), false);

	if (ci.isAssocClass() != null) {
	    printDataElement("associationId", ci.isAssocClass().id());
	}
	if (ci.getLinkedDocument() != null) {
	    File linkedDoc = ci.getLinkedDocument();
	    File linkedDocsDir = options.linkedDocumentsTmpDir();
	    String relativePath = linkedDocsDir.toPath().relativize(linkedDoc.toPath()).toString();
	    printDataElement("linkedDocument", relativePath);
	}

	if (exportProfilesFromWholeModel || allSelectedSchemaPackages.contains(ci.pkg())) {
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

	printConstraints(ci.directConstraints());

	if (!ci.properties().isEmpty()
		&& ci.properties().values().stream().anyMatch(property -> property.isNavigable())) {

	    writer.startElement(NS, "properties");
	    for (PropertyInfo pi : ci.properties().values()) {
		/*
		 * 20180907 JE: ClassInfo.properties() should only return navigable properties.
		 * However, we've had the case that this contract was not fulfilled by an
		 * external model implementation. Therefore, we enforce the contract here.
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

    protected void printProfiles(Profiles profiles) throws SAXException {

	if (!omitExistingProfiles && !profiles.isEmpty()) {

	    /*
	     * Determine which profiles to export; first create copy of the set of profile
	     * identifier names from the given set of profiles so that we can safely apply
	     * the retainAll operation on it.
	     */
	    Set<String> ptexp = new HashSet<String>(profiles.getProfileIdentifiersByName().keySet());
	    if (profilesToExport != null) {
		ptexp.retainAll(profilesToExport);
	    }

	    if (!ptexp.isEmpty()) {

		writer.startElement(NS, "profiles");

		for (ProfileIdentifier pi : profiles.getProfileIdentifiersByName().values()) {

		    if (!ptexp.contains(pi.getName())) {
			/*
			 * this particular profile is not a member of the set of profiles to export
			 */
			continue;
		    }

		    AttributesImpl atts = new AttributesImpl();
		    atts.addAttribute("", "name", "", "string", pi.getName());

		    if (pi.hasVersionIndicator() || pi.hasParameters()) {

			writer.startElement(NS, "Profile", "", atts);

			if (pi.hasVersionIndicator()) {

			    writer.startElement(NS, "versionIdentifier");

			    for (VersionRange versionRange : pi.getVersionIndicator().getVersionInfos()) {

				AttributesImpl attsVersionRange = new AttributesImpl();
				attsVersionRange.addAttribute("", "begin", "", "string",
					versionRange.getBegin().toString());
				attsVersionRange.addAttribute("", "end", "", "string",
					versionRange.getEnd().toString());

				writer.emptyElement(NS, "VersionRange", "", attsVersionRange);
			    }

			    writer.endElement(NS, "versionIdentifier");
			}

			if (pi.hasParameters()) {

			    writer.startElement(NS, "parameter");

			    for (Entry<String, String> parameterEntry : pi.getParameter().entrySet()) {

				AttributesImpl attsParameter = new AttributesImpl();
				attsParameter.addAttribute("", "name", "", "string", parameterEntry.getKey());
				if (parameterEntry.getValue() != null) {
				    attsParameter.addAttribute("", "value", "", "string", parameterEntry.getValue());
				}

				writer.emptyElement(NS, "ProfileParameter", "", attsParameter);
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

    protected void printConstraints(List<Constraint> constraints) throws SAXException {

	if (constraints != null && !constraints.isEmpty()) {

	    writer.startElement(NS, "constraints");

	    for (Constraint con : constraints) {

		String elementName;
		String type = null;
		String sourceType = null;
		String[] comments = null;

		if (con instanceof FolConstraint) {

		    elementName = "FolConstraint";
		    sourceType = ((FolConstraint) con).sourceType();
		    comments = ((FolConstraint) con).comments();

		} else if (con instanceof OclConstraint) {

		    elementName = "OclConstraint";
		    comments = ((OclConstraint) con).comments();

		} else {

		    elementName = "TextConstraint";
		    type = ((TextConstraint) con).type();
		}

		writer.startElement(NS, elementName);

		printDataElement("name", con.name());
		printDataElement("status", con.status());
		printDataElement("text", con.text());

		printDataElement("type", type);
		printDataElement("sourceType", sourceType);
		if (includeConstraintDescriptions) {
		    printDataElements("description", comments);
		}

		writer.endElement(NS, elementName);

		// comments and expressions will be created when validating
		// the constraint
	    }

	    writer.endElement(NS, "constraints");
	}
    }

    protected void printProperty(PropertyInfo pi, boolean printInClass) throws SAXException {

	writer.startElement(NS, "Property");

	printInfoFields(pi);

	if (exportProfilesFromWholeModel || allSelectedSchemaPackages.contains(pi.inClass().pkg())) {
	    printProfiles(pi.profiles());
	}

	if (!(pi.cardinality().minOccurs == 1 && pi.cardinality().maxOccurs == 1)) {
	    printDataElement("cardinality", pi.cardinality().toString());
	}

	if (!pi.matches(ModelExportConstants.RULE_TGT_EXP_PROP_SUPPRESS_ISNAVIGABLE)) {
	    printDataElement("isNavigable", pi.isNavigable(), true);
	}

	if (pi.sequenceNumber() == null) {
	    MessageContext mc = this.result.addError(null, 30901, pi.name());
	    if (mc != null) {
		mc.addDetail(null, 0, pi.fullName());
	    }
	    printDataElement("sequenceNumber", "0");
	} else {
	    printDataElement("sequenceNumber", pi.sequenceNumber().getString());
	}

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

	if (suppressCodeAndEnumCharacteristicsWithoutSemanticMeaning
		&& (pi.inClass().category() == Options.ENUMERATION || pi.inClass().category() == Options.CODELIST)) {

	    if (pi.isOrdered()) {
		result.addDebug(null, 30900, "isOrdered", "true", pi.fullNameInSchema());
	    }
	    if (!pi.isUnique()) {
		result.addDebug(null, 30900, "isUnique", "false", pi.fullNameInSchema());
	    }
	    if (pi.isComposition()) {
		result.addDebug(null, 30900, "isComposition", "true", pi.fullNameInSchema());
	    }
	    if (pi.isAggregation()) {
		result.addDebug(null, 30900, "isAggregation", "true", pi.fullNameInSchema());
	    }
	    if (pi.isOwned()) {
		result.addDebug(null, 30900, "isOwned", "true", pi.fullNameInSchema());
	    }
	} else {
	    printDataElement("isOrdered", pi.isOrdered(), false);
	    printDataElement("isUnique", pi.isUnique(), true);
	    printDataElement("isComposition", pi.isComposition(), false);
	    printDataElement("isAggregation", pi.isAggregation(), false);
	    printDataElement("isOwned", pi.isOwned(), false);
	}

	printDataElement("initialValue", pi.initialValue());
	printDataElement("inlineOrByReference", pi.inlineOrByReference(), "inlineOrByReference");

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

	if (pi.association() != null) {
	    printDataElement("associationId", pi.association().id());
	}

	printConstraints(pi.constraints());

	writer.endElement(NS, "Property");
    }

    protected void printInfoFields(Info i) throws SAXException {

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
	if (!descriptors.isEmpty() && !i.matches(ModelExportConstants.RULE_TGT_EXP_ALL_OMIT_DESCRIPTORS)) {
	    writer.startElement(NS, "descriptors");
	    printDescriptorElement(Descriptor.ALIAS, descriptors);
	    printDescriptorElement(Descriptor.PRIMARYCODE, descriptors);
	    printDescriptorElement(Descriptor.GLOBALIDENTIFIER, descriptors);
	    printDescriptorElement(Descriptor.DEFINITION, descriptors);
	    printDescriptorElement(Descriptor.DESCRIPTION, descriptors);
	    /*
	     * TBD: We could add a parameter to list the descriptors that shall be exported
	     */
	    printDescriptorElement(Descriptor.DOCUMENTATION, descriptors);
	    printDescriptorElement(Descriptor.LEGALBASIS, descriptors);
	    printDescriptorElement(Descriptor.LANGUAGE, descriptors);
	    printDescriptorElement(Descriptor.EXAMPLE, descriptors);
	    printDescriptorElement(Descriptor.DATACAPTURESTATEMENT, descriptors);
	    writer.endElement(NS, "descriptors");
	}

	TaggedValues tvs = i.taggedValuesAll();

	// identify set of tagged values to export
	TaggedValues tvsToExport = options.taggedValueFactory();
	for (String tagName : tvs.keySet()) {
	    if (ignoreTaggedValuesPattern == null || !ignoreTaggedValuesPattern.matcher(tagName).matches()) {
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
		    if (values.length == 1 && (values[0] == null || values[0].length() == 0)) {
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

    protected void printDescriptorElement(Descriptor descriptor, Descriptors descriptors) throws SAXException {

	List<LangString> descriptorValues_tmp = descriptors.values(descriptor);

	/*
	 * Ignore values that only contain whitespace. Empty values would lead to
	 * validation errors. This is especially relevant for descriptors that have the
	 * empty string as value by default, even if no source defines it; an example is
	 * the descriptor 'definition'.
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

		    printDataElement("DescriptorValue", dv.getValue(), "lang", dv.getLang());

		} else {

		    printDataElement("DescriptorValue", dv.getValue());
		}
	    }

	    writer.endElement(NS, "descriptorValues");
	    writer.endElement(NS, descriptor.getName());
	}
    }

    protected void printDiagrams(List<ImageMetadata> diagrams) throws SAXException {

	if (diagrams != null && !diagrams.isEmpty()) {

	    writer.startElement(NS, "diagrams");

	    for (ImageMetadata im : diagrams) {

		writer.startElement(NS, "ImageMetadata");
		/*
		 * NOTE: image metadata IDs have prefix "img", so no need to add a prefix to
		 * prevent a number as first character of the id
		 */
		writer.dataElement(NS, "id", im.getId());
		writer.dataElement(NS, "name", im.getName());
		writer.dataElement(NS, "relPathToFile", im.getRelPathToFile());
		writer.dataElement(NS, "width", "" + im.getWidth());
		writer.dataElement(NS, "height", "" + im.getHeight());		
		if(im.getDocumentation() != null) {
		    writer.dataElement(NS,"documentation",im.getDocumentation());
		}
		writer.endElement(NS, "ImageMetadata");
	    }

	    writer.endElement(NS, "diagrams");
	}
    }

    public void printModel(Model modelIn, AttributesImpl atts) throws Exception {

	Model model = processProfileInfos(modelIn);

	allSelectedSchemaPackages = model.allPackagesFromSelectedSchemas();

	atts.addAttribute("", "encoding", "", "string", model.characterEncoding());

	String scversion = "[dev]";
	String scunittesting = System.getProperty("scunittesting");
	if ("true".equalsIgnoreCase(scunittesting)) {
	    scversion = "unittest";
	} else {
	    InputStream stream = getClass().getResourceAsStream("/sc.properties");
	    if (stream != null) {
		Properties properties = new Properties();
		properties.load(stream);
		scversion = properties.getProperty("sc.version");
	    }
	}
	atts.addAttribute("", "scxmlProducer", "", "string", "ShapeChange");
	atts.addAttribute("", "scxmlProducerVersion", "", "string", scversion);

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
	 * finally, print all associations; navigable roles have been printed as part of
	 * the classes they are in, non-navigable roles need to be printed within the
	 * association
	 */
	SortedSet<AssociationInfo> associations = model.associations();
	if (!associations.isEmpty()) {
	    writer.startElement(NS, "associations");
	    for (AssociationInfo ai : associations) {
		printAssociation(ai);
	    }
	    writer.endElement(NS, "associations");
	}

	writer.endElement(NS, "Model");
    }

    protected Model processProfileInfos(Model modelIn) {

	Model resultModel = modelIn;

	if (!omitExistingProfiles && !profilesInModelSetExplicitly) {

	    // We need to convert the profile definitions in the model
	    SortedSet<String> profilesForClassesWithoutExplicitProfiles = null;

	    if (defaultProfilesForClassesWithoutExplicitProfiles != null && !defaultProfilesForClassesWithoutExplicitProfiles.isEmpty()) {

		profilesForClassesWithoutExplicitProfiles = defaultProfilesForClassesWithoutExplicitProfiles;

	    } else {

		// gather the names of profiles from the model
		profilesForClassesWithoutExplicitProfiles = ProfileUtil.findNamesOfAllProfiles(modelIn,
			exportProfilesFromWholeModel);
	    }

	    if (!profilesForClassesWithoutExplicitProfiles.isEmpty()) {

		Profiles profilesForClassesBelongingToAllProfiles = new Profiles();
		for (String profileName : profilesForClassesWithoutExplicitProfiles) {
		    profilesForClassesBelongingToAllProfiles.put(profileName);
		}

		/*
		 * Convert model to one with explicit profile definitions
		 */
		GenericModel genModel = new GenericModel(modelIn);
		ProfileUtil.convertToExplicitProfileDefinitions(genModel, profilesForClassesBelongingToAllProfiles,
			null, exportProfilesFromWholeModel);

		/*
		 * Postprocessing and validation of the generic model should not be necessary.
		 * For example, constraints do not need to be parsed again, since the export to
		 * XML does not require parsed statements, just the constraint text (as well as
		 * name etc).
		 */
		// genModel.postprocessAfterLoadingAndValidate();
		resultModel = genModel;

	    } else {
		/*
		 * If no profiles are available to set for classes and properties that belong to
		 * all profiles, we can just as well pretend that the input model uses explicit
		 * profile settings
		 */
	    }
	}

	return resultModel;
    }

    public void write(Model model) {

	try {
	    writer.forceNSDecl("http://www.w3.org/2001/XMLSchema-instance", "xsi");
	    writer.forceNSDecl(NS, "sc");
	    writer.startDocument();

	    AttributesImpl atts = new AttributesImpl();
	    atts.addAttribute("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation", "xsi:schemaLocation",
		    "CDATA", NS + " " + schemaLocation);

	    printModel(model, atts);

	    writer.endDocument();
	    writer.close();

	    if (zipOutput) {

		File outputZipFile = new File(outputXmlFile.getParentFile(), FilenameUtils.getBaseName(outputXmlFile.getName()) + ".zip");
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
	}
    }
}
