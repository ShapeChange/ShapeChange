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
 * (c) 2002-2020 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.ea.target.uml;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.sparx.CreateModelType;
import org.sparx.Repository;

import de.interactive_instruments.shapechange.ea.util.EARepositoryUtil;
import de.interactive_instruments.shapechange.core.AbstractConfigurationValidator;
import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ProcessConfiguration;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.TargetConfiguration;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class UmlModelConfigurationValidator extends AbstractConfigurationValidator {

    protected SortedSet<String> allowedParametersWithStaticNames = new TreeSet<>(Stream
	    .of(UmlModelConstants.PARAM_EA_AUTHOR, UmlModelConstants.PARAM_EA_STATUS,
		    UmlModelConstants.PARAM_EA_TEMPLATE, UmlModelConstants.PARAM_EAP_TEMPLATE,
		    UmlModelConstants.PARAM_IGNORE_TAGGED_VALUES_REGEX,
		    UmlModelConstants.PARAM_INCLUDE_ASSOCIATIONEND_OWNERSHIP,
		    UmlModelConstants.PARAM_MERGE_CONSTRAINT_COMMENTS_INTO_TEXT, UmlModelConstants.PARAM_MODEL_FILENAME,
		    UmlModelConstants.PARAM_OMIT_OUTPUT_PACKAGE, UmlModelConstants.PARAM_OMIT_OUTPUT_PACKAGE_DATETIME,
		    UmlModelConstants.PARAM_OUTPUT_DIR, UmlModelConstants.PARAM_OUTPUT_PACKAGE_NAME,
		    UmlModelConstants.PARAM_PRESERVE_PACKAGE_HIERARCHY, UmlModelConstants.PARAM_SYNCH_STEREOTYPES)
	    .collect(Collectors.toSet()));
    protected List<Pattern> regexForAllowedParametersWithDynamicNames = null;

    @Override
    public boolean isValid(ProcessConfiguration configIn, Options options, ShapeChangeResult result) {

	TargetConfiguration config = (TargetConfiguration) configIn;

	boolean isValid = true;

	allowedParametersWithStaticNames.addAll(getCommonTargetParameters());
	isValid = validateParameters(allowedParametersWithStaticNames, regexForAllowedParametersWithDynamicNames,
		config.getParameters().keySet(), result) && isValid;

	String outputDirectoryBase = config.getParameterValue(UmlModelConstants.PARAM_OUTPUT_DIR);
	if (outputDirectoryBase == null)
	    outputDirectoryBase = options.parameter("outputDirectory");
	if (outputDirectoryBase == null)
	    outputDirectoryBase = ".";

	SortedSet<String> modelProviderIds = config.getInputIds();

	for (String modelProviderId : modelProviderIds) {

	    String outputDirectory = outputDirectoryBase.trim() + File.separator + modelProviderId;

	    String outputFilename = config.parameterAsString(UmlModelConstants.PARAM_MODEL_FILENAME,
		    "ShapeChangeExport.qea", false, true);

	    /*
	     * Make sure repository file exists
	     */
	    java.io.File repfile = null;

	    java.io.File outDir = new java.io.File(outputDirectory);
	    if (!outDir.exists()) {
		try {
		    FileUtils.forceMkdir(outDir);
		} catch (IOException e) {
		    String errormsg = e.getMessage();
		    result.addError(this, 32, errormsg, outputDirectory);
		    isValid = false;
		    continue;
		}
	    }

	    repfile = new java.io.File(outDir, outputFilename);

	    boolean ex = true;
	    boolean created = false;

	    Repository rep = new Repository();

	    if (!repfile.exists()) {
		ex = false;
		if (!outputFilename.toLowerCase().endsWith(".qea")) {
		    outputFilename += ".qea";
		    repfile = new java.io.File(outputFilename);
		    ex = repfile.exists();
		}
	    }

	    String absname = repfile.getAbsolutePath();

	    if (!ex) {

		/*
		 * Either copy EA repository template, or create new repository.
		 */
		
		String eaTemplateFilePath;
		if (config.hasParameter(UmlModelConstants.PARAM_EA_TEMPLATE)) {
		    eaTemplateFilePath = config.getParameterValue(UmlModelConstants.PARAM_EA_TEMPLATE);
		} else {
		    eaTemplateFilePath = config.getParameterValue(UmlModelConstants.PARAM_EAP_TEMPLATE);
		}

		if (eaTemplateFilePath != null) {

		    // copy template file either from remote or local URI
		    if (eaTemplateFilePath.toLowerCase().startsWith("http")) {
			try {
			    URL templateUrl = URI.create(eaTemplateFilePath).toURL();
			    FileUtils.copyURLToFile(templateUrl, repfile);
			    created = true;
			} catch (MalformedURLException e1) {
			    result.addError(this, 51, eaTemplateFilePath, e1.getMessage());
			    isValid = false;
			} catch (IOException e2) {
			    result.addError(this, 53, e2.getMessage());
			    isValid = false;
			}
		    } else {
			File eatemplate = new File(eaTemplateFilePath);
			if (eatemplate.exists()) {
			    try {
				FileUtils.copyFile(eatemplate, repfile);
				created = true;
			    } catch (IOException e) {
				result.addError(this, 53, e.getMessage());
				isValid = false;
			    }
			} else {
			    result.addError(this, 52, eatemplate.getAbsolutePath());
			    isValid = false;
			}
		    }

		} else {

		    if (!rep.CreateModel(CreateModelType.cmEAPFromBase, absname, 0)) {
			result.addError(null, 31, absname);
			rep = null;
			isValid = false;
		    } else {
			created = true;
		    }
		}
	    }

	    /*
	     * Checks that rely on the output repository file to exist or to have been
	     * created (either from a template or as new file).
	     */
	    if (ex || created) {

		/** Connect to EA Repository */
		if (!rep.OpenFile(absname)) {
		    String errormsg = rep.GetLastError();
		    result.addError(null, 30, errormsg, outputFilename);
		    rep = null;
		    isValid = false;
		} else {

		    isValid = isValid & validateMdg(rep, config, options, result);
		}
	    }

	    EARepositoryUtil.closeRepository(rep);
	}

	return isValid;
    }

    private boolean validateMdg(Repository rep, ProcessConfiguration config, Options options,
	    ShapeChangeResult result) {

	List<String> profiles = config.getMapEntries().stream().filter(me -> me.hasTargetType())
		.map(me -> me.getTargetType()).filter(tt -> tt.indexOf("::") >= 0).map(tt -> tt.split("::")[0])
		.distinct().sorted().collect(Collectors.toList());

	if (!profiles.isEmpty()) {

	    List<String> unavailableMdes = new ArrayList<>();

	    for (String profile : profiles) {
		if (!rep.IsTechnologyLoaded(profile)) {
		    unavailableMdes.add(profile);
		}
	    }

	    if (!unavailableMdes.isEmpty()) {
		result.addWarning(this, 100, StringUtils.join(unavailableMdes, ", "));
	    }
	}

	return true;
    }

    @Override
    public String message(int mnr) {
	switch (mnr) {

	case 32:
	    return "Could not create output directory at $1$. Tests with an actual output repository will thus not be performed.";
	case 51:
	    return "URL '$1$' provided for configuration parameter " + UmlModelConstants.PARAM_EA_TEMPLATE
		    + " is malformed. Exception message is: '$2$'.";
	case 52:
	    return "EA repository template at '$1$' does not exist or cannot be read. Check the value of the configuration parameter '"
		    + UmlModelConstants.PARAM_EA_TEMPLATE
		    + "' and ensure that: a) it contains the path to the template file and b) the file can be read by ShapeChange.";
	case 53:
	    return "Exception encountered when copying EA repository template file to output destination. Message is: $1$.";

	case 100:
	    return "The target configuration contains map entries with qualified stereotypes as target types. The following profiles from these map entries are not loaded: $1$. That is only an issue if the actual encoding attempts to use map entries with these profiles.";

	default:
	    return "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	}
    }
}
