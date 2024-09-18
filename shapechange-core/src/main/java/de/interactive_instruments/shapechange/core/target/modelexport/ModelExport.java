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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.shapechange.core.target.modelexport;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import de.interactive_instruments.shapechange.core.MessageSource;
import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.RuleRegistry;
import de.interactive_instruments.shapechange.core.ShapeChangeAbortException;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.model.ClassInfo;
import de.interactive_instruments.shapechange.core.model.Model;
import de.interactive_instruments.shapechange.core.model.PackageInfo;
import de.interactive_instruments.shapechange.core.model.writer.ModelWriter;
import de.interactive_instruments.shapechange.core.target.SingleTarget;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class ModelExport implements SingleTarget, MessageSource {

    public static final String NS = "http://shapechange.net/model";

    private static boolean initialised = false;

    private static String outputDirectory = null;
    private static String outputFilename = null;
    private static String encoding = null;
    private static File outputXmlFile = null;

    private static Model model = null;

    private static Set<String> profilesToExport = null;
    private static boolean omitExistingProfiles = false;
    private static boolean allPackagesAreEditable = false;
    private static boolean profilesInModelSetExplicitly = true;
    private static Pattern ignoreTaggedValuesPattern = null;
    private static boolean exportProfilesFromWholeModel = false;
    private static boolean includeConstraintDescriptions = false;
    private static boolean suppressCodeAndEnumCharacteristicsWithoutSemanticMeaning = false;
    private static boolean zipOutput = false;
    private static String schemaLocation = ModelExportConstants.DEFAULT_SCHEMA_LOCATION;
    private static SortedSet<String> defaultProfilesForClassesWithoutExplicitProfiles = null;

    private Options options = null;
    private ShapeChangeResult result = null;

    @Override
    public void initialise(PackageInfo p, Model m, Options o, ShapeChangeResult r, boolean diagOnly)
	    throws ShapeChangeAbortException {

	options = o;
	result = r;

	try {

	    if (!initialised) {

		initialised = true;

		model = m;

		outputDirectory = options.parameter(ModelExport.class.getName(), "outputDirectory");
		if (outputDirectory == null)
		    outputDirectory = options.parameter("outputDirectory");
		if (outputDirectory == null)
		    outputDirectory = ".";

		outputFilename = options.parameter(ModelExport.class.getName(), "outputFilename");
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

		encoding = encoding == null ? "UTF-8" : m.characterEncoding();
		// String encoding_ = "UTF-8";

		outputXmlFile = new File(outputDirectory + "/" + xmlName);

		if (p.matches(ModelExportConstants.RULE_TGT_EXP_ALL_RESTRICT_EXISTING_PROFILES)) {
		    profilesToExport = new HashSet<String>(options.parameterAsStringList(ModelExport.class.getName(),
			    ModelExportConstants.PARAM_PROFILES_TO_EXPORT, null, true, true));
		}

		omitExistingProfiles = p.matches(ModelExportConstants.RULE_TGT_EXP_ALL_OMIT_EXISTING_PROFILES);

		allPackagesAreEditable = p.matches(ModelExportConstants.RULE_TGT_EXP_PKG_ALL_EDITABLE);

		try {
		    ignoreTaggedValuesPattern = Pattern.compile(options.parameterAsString(ModelExport.class.getName(),
			    ModelExportConstants.PARAM_IGNORE_TAGGED_VALUES_REGEX,
			    ModelExportConstants.DEFAULT_IGNORE_TAGGED_VALUES_REGEX, true, false));
		} catch (PatternSyntaxException e) {
		    result.addError(this, 11, ModelExportConstants.PARAM_IGNORE_TAGGED_VALUES_REGEX, e.getMessage());
		    ignoreTaggedValuesPattern = Pattern
			    .compile(ModelExportConstants.DEFAULT_IGNORE_TAGGED_VALUES_REGEX);
		}

		exportProfilesFromWholeModel = options.parameterAsBoolean(ModelExport.class.getName(),
			ModelExportConstants.PARAM_EXPORT_PROFILES_FROM_WHOLE_MODEL, false);

		includeConstraintDescriptions = options.parameterAsBoolean(ModelExport.class.getName(),
			ModelExportConstants.PARAM_INCLUDE_CONSTRAINT_DESCRIPTIONS, false);

		suppressCodeAndEnumCharacteristicsWithoutSemanticMeaning = options.parameterAsBoolean(
			ModelExport.class.getName(),
			ModelExportConstants.PARAM_SUPPRESS_MEANINGLESS_CODE_ENUM_CHARACTERISTICS, false);

		zipOutput = options.parameterAsBoolean(ModelExport.class.getName(),
			ModelExportConstants.PARAM_ZIP_OUTPUT, false);

		schemaLocation = options.parameterAsString(ModelExport.class.getName(),
			ModelExportConstants.PARAM_SCHEMA_LOCATION, ModelExportConstants.DEFAULT_SCHEMA_LOCATION, false,
			true);

		profilesInModelSetExplicitly = options.parameterAsBoolean(ModelExport.class.getName(),
			ModelExportConstants.PARAM_MODEL_EXPLICIT_PROFILES, true);

		defaultProfilesForClassesWithoutExplicitProfiles = new TreeSet<String>(options.parameterAsStringList(
			ModelExport.class.getName(),
			ModelExportConstants.PARAM_PROFILES_FOR_CLASSES_WITHOUT_EXPLICIT_PROFILES, null, true, true));
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

	model = null;

	outputDirectory = null;
	outputFilename = null;
	outputXmlFile = null;

	encoding = null;

	profilesToExport = null;
	omitExistingProfiles = false;
	allPackagesAreEditable = false;
	profilesInModelSetExplicitly = true;
	ignoreTaggedValuesPattern = null;
	exportProfilesFromWholeModel = false;
	includeConstraintDescriptions = false;
	suppressCodeAndEnumCharacteristicsWithoutSemanticMeaning = false;
	zipOutput = false;
	schemaLocation = ModelExportConstants.DEFAULT_SCHEMA_LOCATION;
	defaultProfilesForClassesWithoutExplicitProfiles = null;
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

	ModelWriter modelWriter = new ModelWriter(options, result, encoding, outputXmlFile, profilesToExport,
		omitExistingProfiles, ignoreTaggedValuesPattern, exportProfilesFromWholeModel,
		includeConstraintDescriptions, suppressCodeAndEnumCharacteristicsWithoutSemanticMeaning, zipOutput,
		schemaLocation, profilesInModelSetExplicitly, defaultProfilesForClassesWithoutExplicitProfiles,
		allPackagesAreEditable);

	modelWriter.write(model);

	// release model - do NOT close it here
	model = null;
    }

    @Override
    public void registerRulesAndRequirements(RuleRegistry r) {
	r.addRule("rule-exp-all-omitDescriptors");
	r.addRule("rule-exp-all-omitExistingProfiles");
	r.addRule("rule-exp-all-restrictExistingProfiles");
	r.addRule("rule-exp-pkg-allPackagesAreEditable");
	r.addRule("rule-exp-prop-suppressIsNavigable");
    }

    @Override
    public String getTargetIdentifier() {
	return "exp";
    }

    @Override
    public String getDefaultEncodingRule() {
	return "*";
    }

    @Override
    public String message(int mnr) {

	switch (mnr) {

	case 0:
	    return "Context: property '$1$'.";
	case 1:
	    return "Context: class '$1$'.";
	case 2:
	    return "Context: association class '$1$'.";
	case 3:
	    return "Context: association between class '$1$' (with property '$2$') and class '$3$' (with property '$4$')";

	case 11:
	    return "Syntax exception while compiling the regular expression defined by target parameter '$1$': '$2$'. The default will be used.";
	case 12:
	    return "Directory named '$1$' does not exist or is not accessible.";
	case 13:
	    return "Suppressing semantically meaningless characteristic '$1$' (with value '$2$') of code/enum '$3$'.";

	case 100:
	    return "Sequence number is undefined for property '$1$'. Using '0'.";

	default:
	    return "(ModelExport.java) Unknown message with number: " + mnr;
	}

    }
}
