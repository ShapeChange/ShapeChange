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
 * (c) 2002-2026 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core.target.coretable;

import java.util.SortedSet;
import java.util.TreeSet;

import de.interactive_instruments.shapechange.core.MessageSource;
import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.RuleRegistry;
import de.interactive_instruments.shapechange.core.ShapeChangeAbortException;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.model.ClassInfo;
import de.interactive_instruments.shapechange.core.model.Info;
import de.interactive_instruments.shapechange.core.model.Model;
import de.interactive_instruments.shapechange.core.model.PackageInfo;
import de.interactive_instruments.shapechange.core.target.SingleTarget;
import de.interactive_instruments.shapechange.core.target.TargetUtil;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 */
public class Coretable implements SingleTarget, MessageSource {

    private ShapeChangeResult result = null;
    private Options options = null;

    private static PackageInfo mainAppSchema = null;

    private static String outputDirectory = null;
    private static String outputFilename = null;

    private static boolean initialised = false;

    private static boolean generateCascadeRules = false;
    private static boolean createCascadeRuleGraphImage = false;
    private static String dbSchemaName = "public";

    private static SortedSet<ClassInfo> featureObjectAndMixinTypes = new TreeSet<>();

    @Override
    public void initialise(PackageInfo pi, Model model, Options o, ShapeChangeResult r, boolean diagOnly)
	    throws ShapeChangeAbortException {

	options = o;
	result = r;

	if (!initialised) {
	    initialised = true;

	    outputDirectory = options.parameter(this.getClass().getName(), "outputDirectory");
	    if (outputDirectory == null)
		outputDirectory = options.parameter("outputDirectory");
	    if (outputDirectory == null)
		outputDirectory = ".";

	    outputFilename = options.parameter(this.getClass().getName(), "outputFilename");
	    if (outputFilename == null)
		outputFilename = "coretable";

	    mainAppSchema = TargetUtil.findMainSchemaForSingleTargets(model.selectedSchemas(), o, r);
	    if (mainAppSchema == null) {
		result.addWarning(this, 128, pi.name());
		mainAppSchema = pi;
	    }

	    generateCascadeRules = options.parameterAsBoolean(this.getClass().getName(),
		    CoretableConstants.PARAM_GENERATE_CASCADE_RULES, false);
	    
	    createCascadeRuleGraphImage = options.parameterAsBoolean(this.getClass().getName(),
		    CoretableConstants.PARAM_CREATE_CASCADE_RULE_GRAPH_IMAGE, false);

	    dbSchemaName = options.parameterAsString(this.getClass().getName(), CoretableConstants.PARAM_DB_SCHEMA_NAME,
		    "public", false, true);
	}
    }

    @Override
    public void process(ClassInfo ci) {

	if (!isEncoded(ci)) {
	    result.addInfo(this, 8, ci.name());
	    return;
	}

	int cat = ci.category();

	if (cat == Options.FEATURE || cat == Options.OBJECT || cat == Options.MIXIN) {
	    featureObjectAndMixinTypes.add(ci);
	}
    }

    public static boolean isEncoded(Info i) {

	if (i.matches(CoretableConstants.RULE_ALL_NOT_ENCODED)
		&& i.encodingRule("coretable").equalsIgnoreCase("notencoded")) {

	    return false;

	} else {

	    return true;
	}
    }

    @Override
    public void write() {
	// irrelevant for this SingleTarget
    }

    @Override
    public String getTargetName() {
	return "Coretable";
    }

    @Override
    public void writeAll(ShapeChangeResult r) {

	result = r;

	if (generateCascadeRules) {

	    String appSchema = mainAppSchema.name();
	    String appSchemaVersion = mainAppSchema.version();

	    CoretableCascadeRuleWriter ccrWriter = new CoretableCascadeRuleWriter(result, dbSchemaName);
	    ccrWriter.computeCascadeRules(featureObjectAndMixinTypes, appSchema, appSchemaVersion);
	    ccrWriter.write(outputDirectory, outputFilename, getTargetName(), createCascadeRuleGraphImage);
	}
    }

    @Override
    public void registerRulesAndRequirements(RuleRegistry r) {

	r.addRule("rule-coretable-all-notEncoded");
    }

    @Override
    public String getDefaultEncodingRule() {
	return "*";
    }

    @Override
    public String getTargetIdentifier() {
	return "coretable";
    }

    @Override
    public void reset() {

	mainAppSchema = null;
	
	initialised = false;
	outputDirectory = null;
	outputFilename = null;

	generateCascadeRules = false;
	createCascadeRuleGraphImage = false;
	dbSchemaName = "public";
	
	featureObjectAndMixinTypes = new TreeSet<>();
    }

    @Override
    public String message(int mnr) {

	return switch (mnr) {
	case 0 -> "Context: class Coretable";
	case 8 -> "Class '$1$' is not encoded.";
	case 128 ->
	    "??Main application schema could not be determined (using parameter '" + TargetUtil.PARAM_MAIN_APP_SCHEMA
		    + "' - if set - or by having only a single schema to process). Using '$1$'.";

	default -> "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	};
    }
}
