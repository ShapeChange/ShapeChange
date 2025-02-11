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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.shapechange.core.target.diff;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.shapechange.core.DefaultModelProvider;
import de.interactive_instruments.shapechange.core.MapEntryParamInfos;
import de.interactive_instruments.shapechange.core.MessageSource;
import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ProcessConfiguration;
import de.interactive_instruments.shapechange.core.ProcessMapEntry;
import de.interactive_instruments.shapechange.core.RuleRegistry;
import de.interactive_instruments.shapechange.core.ShapeChangeAbortException;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.model.ClassInfo;
import de.interactive_instruments.shapechange.core.model.Model;
import de.interactive_instruments.shapechange.core.model.PackageInfo;
import de.interactive_instruments.shapechange.core.model.generic.GenericModel;
import de.interactive_instruments.shapechange.core.model.writer.ModelDiffWriter;
import de.interactive_instruments.shapechange.core.target.SingleTarget;
import de.interactive_instruments.shapechange.core.modeldiff.DiffElement2;
import de.interactive_instruments.shapechange.core.modeldiff.DiffElement2Comparator;
import de.interactive_instruments.shapechange.core.modeldiff.Differ2;
import de.interactive_instruments.shapechange.core.modeldiff.DiffElement2.ElementChangeType;

/**
 * Computes differences between schemas selected for processing in the input and
 * a reference model. The latter is used as 'source' for the comparison, while
 * the former is used as 'target'.
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class DiffTarget implements SingleTarget, MessageSource {

    private static final String SCHEMA_URL = "http://shapechange.net/resources/schema/ShapeChangeExportedModel.xsd";

    protected static boolean initialised = false;

    protected static SortedSet<PackageInfo> schemasToProcess = new TreeSet<>();

    protected static GenericModel refModel = null;
    protected static Set<ElementChangeType> relevantDiffElementTypes = new HashSet<>();
    protected static MapEntryParamInfos mapEntryParamInfos = null;
    protected static List<ProcessMapEntry> mapEntries = null;

    protected static Pattern tagPattern = null;
    protected static Pattern tagsToSplitPattern = null;
    protected static boolean includeModelData = false;
    protected static boolean printModelElementPaths = true;
    protected static boolean aaaModel = false;
    protected static Set<String> relevantModellarten = null;

    protected static File outputDirectoryFile;
    protected static String outputDirectory;
    protected static String outputFilename;

    protected static boolean printed = false;
    protected static boolean diagnosticsOnly = false;

    protected ShapeChangeResult result = null;
    protected static GenericModel model = null;
    protected Options options = null;

    @Override
    public void initialise(PackageInfo p, Model m, Options o, ShapeChangeResult r, boolean diagOnly)
	    throws ShapeChangeAbortException {

	schemasToProcess.add(p);

	options = o;
	result = r;
	diagnosticsOnly = diagOnly;

	if (!initialised) {
	    initialised = true;

	    ProcessConfiguration config = options.getCurrentProcessConfig();

	    mapEntries = config.getMapEntries();

	    /*
	     * identify map entries defined in the target configuration covering relocations
	     * and name changes of packages, classes, and their properties in source and
	     * target model
	     */
	    List<ProcessMapEntry> mapEntries = config.getMapEntries();
	    if (mapEntries.isEmpty()) {
		result.addDebug(this, 15);
		mapEntryParamInfos = new MapEntryParamInfos(result, null);
	    } else {
		/*
		 * Parse all parameter information
		 */
		mapEntryParamInfos = new MapEntryParamInfos(result, mapEntries);
	    }

	    try {
		tagPattern = config.parameterAsRegexPattern(DiffTargetConstants.PARAM_TAG_PATTERN,
			DiffTargetConstants.DEFAULT_TAG_PATTERN);
	    } catch (PatternSyntaxException e) {
		/*
		 * the parameter is checked by the configuration validator, so this exception
		 * should only occur if validation of the configuration was disabled
		 */
		result.addError(this, 9, DiffTargetConstants.PARAM_TAG_PATTERN, e.getMessage());
	    }

	    try {
		tagsToSplitPattern = config.parameterAsRegexPattern(DiffTargetConstants.PARAM_TAGS_TO_SPLIT, null);
	    } catch (PatternSyntaxException e) {
		/*
		 * the parameter is checked by the configuration validator, so this exception
		 * should only occur if validation of the configuration was disabled
		 */
		result.addError(this, 9, DiffTargetConstants.PARAM_TAGS_TO_SPLIT, e.getMessage());
	    }

	    includeModelData = config.parameterAsBoolean(DiffTargetConstants.PARAM_INCLUDE_MODEL_DATA, false);
	    printModelElementPaths = config.parameterAsBoolean(DiffTargetConstants.PARAM_PRINT_MODEL_ELEMENT_PATHS,
		    true);
	    aaaModel = config.parameterAsBoolean(DiffTargetConstants.PARAM_AAA_MODEL, false);
	    List<String> relMaList = config.parameterAsStringList(DiffTargetConstants.PARAM_RELEVANTE_MODELLARTEN, null,
		    true, true);
	    relevantModellarten = new HashSet<>(relMaList);

	    if (!(m instanceof GenericModel)) {
		model = new GenericModel(m);
	    } else {
		model = (GenericModel) m;
	    }

	    String imt = config.parameterAsString(DiffTargetConstants.PARAM_REFERENCE_MODEL_TYPE, null, false, true);
	    String mdl = config.parameterAsString(DiffTargetConstants.PARAM_REFERENCE_MODEL_FILENAME_OR_CONSTRING, null,
		    false, true);

	    String user = config.parameterAsString(DiffTargetConstants.PARAM_REFERENCE_MODEL_USER, null, false, true);
	    String pwd = config.parameterAsString(DiffTargetConstants.PARAM_REFERENCE_MODEL_PWD, null, false, true);

	    if (StringUtils.isNotBlank(imt) && StringUtils.isNotBlank(mdl)) {

		result.addInfo(this, 109);

		DefaultModelProvider mp = new DefaultModelProvider(result, options);
		Model refModel_tmp = mp.getModel(imt, mdl, user, pwd, false, null);

		/*
		 * Ensure that IDs used in the reference model are unique to that model and do
		 * not get mixed up with the IDs of the input model.
		 * 
		 * REQUIREMENT for model diff: two objects with equal ID must represent the same
		 * model element. If a model element is deleted in the reference model, then a
		 * new model element in the input model must not have the same ID.
		 * 
		 * It looks like this cannot be guaranteed. Therefore we add a prefix to the IDs
		 * of the model elements in the reference model.
		 */
		refModel = new GenericModel(refModel_tmp);
		refModel_tmp.shutdown();

		refModel.addPrefixToModelElementIDs("refmodel_");

		result.addInfo(this, 110);

	    } else {
		result.addError(this, 107);
	    }

	    List<String> diffElementTypeNames = config
		    .parameterAsStringList(DiffTargetConstants.PARAM_DIFF_ELEMENT_TYPES, null, true, true);

	    if (diffElementTypeNames.isEmpty()) {
		for (ElementChangeType ect : ElementChangeType.values()) {
		    relevantDiffElementTypes.add(ect);
		}
	    } else {
		for (String detn : diffElementTypeNames) {

		    try {

			ElementChangeType ect = ElementChangeType.valueOf(detn.toUpperCase(Locale.ENGLISH));
			relevantDiffElementTypes.add(ect);

		    } catch (IllegalArgumentException e) {
			/*
			 * Reporting of illegal value elements is done by configuration validator; we
			 * simply ignore illegal values here.
			 */
		    }
		}
	    }

	    outputDirectory = options.parameter(this.getClass().getName(), "outputDirectory");
	    if (outputDirectory == null)
		outputDirectory = options.parameter("outputDirectory");
	    if (outputDirectory == null)
		outputDirectory = options.parameter(".");

	    outputFilename = "modeldiff.xml";

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

		result.addDebug(this, 3, outputFilename, outputDirectory);

		try {
		    FileUtils.forceDelete(outputFile);
		    result.addDebug(this, 4);
		} catch (IOException e) {
		    result.addInfo(null, 600, e.getMessage());
		    e.printStackTrace(System.err);
		}
	    }
	}
    }

    @Override
    public void process(ClassInfo ci) {

	/*
	 * nothing to do here; the target compares whole schemas
	 */
    }

    @Override
    public void write() {

	// nothing to do here
    }

    @Override
    public String getTargetName() {
	return "Model Diff Target";
    }

    @Override
    public void writeAll(ShapeChangeResult r) {

	if (printed || diagnosticsOnly || refModel == null) {
	    return;
	}

	result = r;
	options = r.options();

	List<DiffElement2> allDiffs = new ArrayList<>();

	for (PackageInfo inputSchema : schemasToProcess) {

	    String nameForSchemaSearch = inputSchema.name();

	    /*
	     * A map entry may define a name change for the schema package, with @type = ref
	     * model schema name, and @targetType = input model schema name. Since we
	     * iterate over the schemas to process as defined in the input model, we search
	     * for map entries by their @targetType - which would have to equal the name of
	     * the schema from the input model.
	     * 
	     * NOTE: This does not take into account encoding rules - which are not defined
	     * for this target yet, anyway.
	     */
	    for (ProcessMapEntry pme : mapEntries) {
		if (StringUtils.isNotBlank(pme.getTargetType())
			&& pme.getTargetType().trim().equals(inputSchema.name().trim())) {
		    /*
		     * Map entries have ref model names / paths in @type, and input model names /
		     * paths in @targetType. Thus, in order to get the name for lookup in the ref
		     * model, we take the value of @type.
		     */
		    nameForSchemaSearch = pme.getType().trim();
		}
	    }

	    SortedSet<PackageInfo> set = refModel.schemas(nameForSchemaSearch);

	    if (set.size() == 1) {

		PackageInfo refSchema = set.getFirst();

		// compute diffs
		Differ2 differ;
		if (aaaModel) {
		    differ = new Differ2(relevantModellarten, mapEntryParamInfos, tagPattern, tagsToSplitPattern);
		} else {
		    differ = new Differ2(mapEntryParamInfos, tagPattern, tagsToSplitPattern);
		}
		List<DiffElement2> diffs = differ.diffSchemas(refSchema, inputSchema);

		// filter to relevant diff elements, then merge into allDiffs
		List<DiffElement2> relevantDiffs = diffs.parallelStream()
			.filter(diff -> relevantDiffElementTypes.contains(diff.elementChangeType))
			.collect(Collectors.toList());

		allDiffs.addAll(relevantDiffs);

	    } else {
		result.addWarning(this, 104, inputSchema.name());

		/*
		 * TBD - assume deleted? ... check the other way as well, i.e packages in
		 * refModel which may fit the criteria for schema selection but have no
		 * equivalent in the input model?
		 */
	    }
	}

	allDiffs.sort(new DiffElement2Comparator());

	File res = new File(outputDirectoryFile, outputFilename);

	ModelDiffWriter dw = new ModelDiffWriter(options, result, "UTF-8", res, true, SCHEMA_URL, true, null, allDiffs);

	dw.write(refModel, model, includeModelData, printModelElementPaths);

	printed = true;
    }

    @Override
    public void registerRulesAndRequirements(RuleRegistry r) {
//	r.addRule("rule-modeldiff-");
    }

    @Override
    public String getTargetIdentifier() {
	return "modeldiff";
    }

    @Override
    public String getDefaultEncodingRule() {
	return "*";
    }

    @Override
    public void reset() {

	initialised = false;

	model = null;
	refModel = null;
	relevantDiffElementTypes = new HashSet<ElementChangeType>();
	mapEntryParamInfos = null;
	mapEntries = null;
	tagPattern = null;
	tagsToSplitPattern = null;

	includeModelData = false;
	printModelElementPaths = true;
	aaaModel = false;
	relevantModellarten = null;

	schemasToProcess = new TreeSet<>();

	outputDirectoryFile = null;
	outputDirectory = null;
	outputFilename = null;

	printed = false;
	diagnosticsOnly = false;
    }

    @Override
    public String message(int mnr) {

	switch (mnr) {
	case 0:
	    return "Context: class DiffTarget";
	case 1:
	    return "";
	case 2:
	    return "XML Schema document with name '$1$' could not be created, invalid filename.";
	case 3:
	    return "Output file '$1$' already exists in directory '$2$'. Attempting to delete it...";
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
	    return "Syntax exception while compiling the regular expression defined by target parameter '$1$': '$2$'.";
	case 10:
	    return "Parameter '$1$' required by rule '$2$' was not set. The rule will be ignored.";
	case 15:
	    return "No map entries provided via the configuration.";

	case 100:
	    return "Context: property '$1$' in class '$2$'.";

	case 104:
	    return "Schema from input model with name '$1$' has no equivalent package in the loaded model. Consequently, no diff was performed.";
	case 107:
	    return "At least one of the required parameters " + DiffTargetConstants.PARAM_REFERENCE_MODEL_TYPE + " and "
		    + DiffTargetConstants.PARAM_REFERENCE_MODEL_FILENAME_OR_CONSTRING
		    + " is missing or has a blank value. Cannot load the reference model. No diff will be created.";
	case 108:
	    return "Model difference - $1$";
	case 109:
	    return "START reading the reference model. Note that following log messages - until END of reading is indicated - are generated by the model loading process. Warnings and errors are related to the reference model.";
	case 110:
	    return "END reading the reference model.";

	default:
	    return "(" + DiffTarget.class.getName() + ") Unknown message with number: " + mnr;
	}
    }
}
