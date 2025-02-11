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

package de.interactive_instruments.shapechange.core;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

import de.interactive_instruments.shapechange.core.target.DeferrableOutputWriter;
import de.interactive_instruments.shapechange.core.target.SingleTarget;
import de.interactive_instruments.shapechange.core.target.Target;
import de.interactive_instruments.shapechange.core.target.TargetOutputProcessor;
import de.interactive_instruments.shapechange.core.target.TargetUtil;
import de.interactive_instruments.shapechange.core.transformation.TransformationManager;
import de.interactive_instruments.shapechange.core.ui.StatusBoard;
import de.interactive_instruments.shapechange.core.ShapeChangeResult.MessageContext;
import de.interactive_instruments.shapechange.core.model.ClassInfo;
import de.interactive_instruments.shapechange.core.model.Model;
import de.interactive_instruments.shapechange.core.model.PackageInfo;
import de.interactive_instruments.shapechange.core.model.generic.GenericModel;
import de.interactive_instruments.shapechange.core.modelvalidation.ModelValidationManager;

public class Converter implements MessageSource {

    public static final int STATUS_TARGET_INITSTART = 201;
    public static final int STATUS_TARGET_PROCESS = 202;
    public static final int STATUS_TARGET_WRITE = 203;
    public static final int STATUS_TARGET_WRITEALL = 204;
    public static final int STATUS_TARGET_DEFERRED_WRITE = 205;
    public static final int STATUS_TRANSFORMER_PROCESS = 206;
    public static final int STATUS_VALIDATION_START = 207;

    /** Result object. */
    protected ShapeChangeResult result = null;
    protected Options options = null;
    protected Target target = null;
    protected Set<String> processIdsToIgnore = new HashSet<String>();
    protected TargetOutputProcessor outputProcessor = null;
    private PackageInfo mainSchemaForSingleTargets;

    public Converter(Options o, ShapeChangeResult r) {
	options = o;
	result = r;
	target = null;
	outputProcessor = new TargetOutputProcessor(r);
    }

    public void convert() throws ShapeChangeAbortException {

	/*
	 * Semantic validation of the ShapeChange configuration
	 * 
	 * NOTE: Validation of the configuration is managed by the Converter because it
	 * is an essential pre-processing step before executing the conversion
	 */
	boolean skipSemanticValidation = false;
	if (StringUtils.equalsIgnoreCase(options.parameter(Options.PARAM_SKIP_SEMANTIC_VALIDATION_OF_CONFIG), "true")) {
	    skipSemanticValidation = true;
	}

	/*
	 * 2021-09-22 JE: Skipping the semantic validation check is an issue, because in
	 * cases where the validation actually ensures that regular expressions are
	 * compiled, the resulting Pattern objects would be missing. Applies to
	 * ModelElementSelectionInfo.java especially.
	 */
	if (skipSemanticValidation) {

	    result.addProcessFlowInfo(this, 512);

	} else {

	    /*
	     * 2016-09-16 JE TBD: should we surround the validateConfiguration() call with a
	     * try-catch to catch any Exception that might be thrown? That could create a
	     * warning or error message in the log.
	     */
	    result.addProcessFlowInfo(this, 510);
	    boolean isValidConfig = validateConfiguration();
	    result.addProcessFlowInfo(this, 511);
	    if (!isValidConfig) {
		MessageContext mc = result.addProcessFlowError(this, 509);
		if (mc != null)
		    mc.addDetail(this, 513);
		return;
	    }
	}

	if (options.isOnlyDeferrableOutputWrite()) {

	    executeAllDeferrableOutputWriters();

	} else {

	    // process model as usual

	    String imt = options.parameter("inputModelType");

	    String mdl = options.parameter("inputFile");
	    String repoFileNameOrConnectionString = options.parameter("repositoryFileNameOrConnectionString");

	    String connection = StringUtils.isNotBlank(repoFileNameOrConnectionString) ? repoFileNameOrConnectionString
		    : mdl;

	    String username = options.parameter("username");
	    String password = options.parameter("password");

	    String transformer = options.parameter("transformer");

	    DefaultModelProvider mp = new DefaultModelProvider(result, options);
	    Model m = mp.getModel(imt, connection, username, password, true, transformer);

	    convert(m);
	}
    }

    private boolean validateConfiguration() throws ShapeChangeAbortException {

	/*
	 * 2017-12-13 JE: In order to ensure that configuration validators can grab
	 * target parameters from options, like the actual target code would, options
	 * needs to be configured with the appropriate target configuration and reset.
	 * However, we also want to ensure that the state of options after the
	 * validation is as it would be without validation. To do so, I tried resetting
	 * options and simply loading the configuration again. However, some of the unit
	 * tests failed then, and I could not figure out why (in a reasonable amount of
	 * time). If the configuration validators get parameters directly from the
	 * process configuration that is given to them, that shouldn't be a problem. So,
	 * for the time being, we do not reset options or reload the configuration.
	 */

	// // ensure initial state of options for validation
	// options.setCurrentProcessConfig(null);
	// options.resetFields();
	// options.loadConfiguration();

	// perform basic validation, especially input parameters and
	// configuration elements
	BasicConfigurationValidator bcv = new BasicConfigurationValidator();
	boolean isValid = bcv.isValid(null, options, result);

	// validate enabled transformer and target configurations
	List<ProcessConfiguration> processConfigs = new ArrayList<ProcessConfiguration>();

	processConfigs.addAll(options.getTransformerConfigs().values());
	processConfigs.addAll(options.getTargetConfigurations());
	processConfigs.addAll(options.getValidatorConfigs().values());

	for (ProcessConfiguration pConfig : processConfigs) {

	    if (pConfig.getProcessMode() == ProcessMode.disabled) {

		// we do not validate disabled processes

	    } else {

		// /*
		// * Reset options for this process, so that parameters for
		// * targets are available.
		// */
		// options.setCurrentProcessConfig(pConfig);
		// options.resetFields();

		try {

		    Class<?> theClass = Class.forName(pConfig.getClassName() + "ConfigurationValidator");

		    ConfigurationValidator validator = (ConfigurationValidator) theClass.getConstructor().newInstance();

		    if (pConfig instanceof TransformerConfiguration tconfig) {
			result.addProcessFlowInfo(this, 514, tconfig.getId());

		    } else if (pConfig instanceof ValidatorConfiguration vconfig) {
			result.addProcessFlowInfo(this, 518, vconfig.getId());

		    } else {

			// /*
			// * NOTE 1: We do not need to reset single targets,
			// since
			// * targets have not been executed yet.
			// *
			// * NOTE 2: We cannot update the outputDirectory
			// * parameter with the id of the model provider, since
			// * validation is general, not in the context of a
			// * specific transformation
			// */

			TargetConfiguration tconfig = (TargetConfiguration) pConfig;
			result.addProcessFlowInfo(this, 515, tconfig.getClassName(),
				StringUtils.join(tconfig.getInputIds(), " "));
		    }

		    isValid = isValid & validator.isValid(pConfig, options, result);

		} catch (ClassNotFoundException e) {

		    // that's fine - a ConfigurationValidator is not
		    // required for a Transformer/Target/Validator

		} catch (Exception e) {

		    result.addProcessFlowWarning(this, 508, pConfig.getClassName(),
			    StringUtils.defaultIfBlank(e.getMessage(), "<not available>"));
		}
	    }
	}

	// // ensure initial state of options for execution
	// options.setCurrentProcessConfig(null);
	// options.resetFields();
	// options.loadConfiguration();

	return isValid;
    }

    private void executeAllDeferrableOutputWriters() {

	try {

	    // write outputs for any DeferrableOutputWriter
	    this.executeDeferrableOutputWriters(options.getTargetConfigurations());

	} catch (Exception e) {

	    MessageContext mc = result.addProcessFlowError(null, 6, e.getClass().getName());
	    if (mc != null) {
		mc.addDetail(StringUtils.defaultIfBlank(e.getMessage(), "<no exception message available>"));
	    }

	    Exception se = e;
	    if (e instanceof SAXException exception) {
		se = exception.getException();
	    }
	    if (se != null) {
		se.printStackTrace(System.err);
	    } else {
		e.printStackTrace(System.err);
	    }
	} finally {
	    result.toFile(options.parameter("logFile"));
	    target = null;
	}
    }

    public void convert(Model model) {

	try {

	    if (model == null && options.isSkipModelLoadingIfProcessingIsOnlyInputTransformations()) {
		/*
		 * no conversion since there are no transformations and targets and thus the
		 * model was not loaded
		 */
	    } else if (model == null) {
		result.addProcessFlowFatalError(this, 14);
		throw new ShapeChangeAbortException();
	    } else {

		// simply return if no schema is selected for processing
		SortedSet<? extends PackageInfo> selectedSchema = model.selectedSchemas();

		if (selectedSchema == null || selectedSchema.isEmpty()) {

		    result.addProcessFlowWarning(this, 507);
		    release(model);

		} else {

		    // first, run any targets that directly reference the input
		    // model
		    this.executeTargets(model, options.getInputId(), options.getInputTargetConfigs());

		    // now recursively execute the transformations (and associated
		    // targets) defined for the input model
		    this.executeTransformations(model, options.getInputTransformerConfigs());

		    /*
		     * do this before executing deferrable output writers to free memory (important
		     * in case of very large models)
		     */
		    release(model);

		    // now write outputs for any DeferrableOutputWriter
		    this.executeDeferrableOutputWriters(options.getTargetConfigurations());
		}
	    }

	} catch (Exception e) {

	    MessageContext mc = result.addProcessFlowError(null, 6, e.getClass().getName());
	    if (mc != null) {
		mc.addDetail(StringUtils.defaultIfBlank(e.getMessage(), "<no exception message available>"));
	    }

	    Exception se = e;
	    if (e instanceof SAXException exception) {
		se = exception.getException();
	    }
	    if (se != null) {
		se.printStackTrace(System.err);
	    } else {
		e.printStackTrace(System.err);
	    }
	} finally {
	    result.toFile(options.parameter("logFile"));
	    target = null;
	}
    }; // convert()

    /**
     * Shuts down the given model and sets it to <code>null</code>.
     * 
     * @param model
     */
    private void release(Model model) {
	if (model != null)
	    model.shutdown();

	model = null;
    }

    /**
     * Writes the output for all DeferrableOutputWriters that are contained in the
     * list of all targets from the ShapeChange configuration.
     * 
     * @param targetConfigs
     * @throws Exception
     */
    private void executeDeferrableOutputWriters(List<TargetConfiguration> targetConfigs) throws Exception {

	// Prepare targets
	resetAllSingleTargets();

	for (TargetConfiguration tgt : targetConfigs) {

	    if (tgt.getProcessMode().equals(ProcessMode.disabled))
		continue;

	    String classname = tgt.getClassName();
	    Class<?> theClass = Class.forName(classname);

	    if (!isDeferrableOutputWriter(theClass)) {
		continue;
	    }

	    // reset options for this target
	    options.setCurrentProcessConfig(tgt);
	    options.resetFields();

	    /*
	     * update the outputDirectory parameter by appending the id of the model
	     * provider
	     */
	    String outputDirectoryForTarget = options.parameter(tgt.getClassName(), "outputDirectory");

	    for (String modelProviderId : tgt.getInputIds()) {

		if (processIdsToIgnore.contains(modelProviderId)) {
		    continue;
		}

		/*
		 * === configure actual output directory ===
		 */
		String outputDirectory = outputDirectoryForTarget;

		if (StringUtils.isNotBlank(outputDirectoryForTarget)) {
		    outputDirectory = outputDirectoryForTarget.trim() + File.separator + modelProviderId;
		    options.setParameter(tgt.getClassName(), "outputDirectory", outputDirectory);
		}

		/*
		 * set up monitoring of output directory, to identify output files (i.e. new and
		 * modified files)
		 */
		OutputFileAlterationListener faListener = new OutputFileAlterationListener();
		FileAlterationObserver outputObserver = setupOutputFileAlterationObserver(outputDirectory, faListener,
			tgt);

		/*
		 * === initialise deferrable output writer and write output ===
		 */
		DeferrableOutputWriter dowTarget = (DeferrableOutputWriter) theClass.getConstructor().newInstance();

		dowTarget.initialise(options, result);

		StatusBoard.getStatusBoard().statusChanged(STATUS_TARGET_DEFERRED_WRITE);
		dowTarget.writeOutput();

		dowTarget = null;

		/*
		 * === process output files ===
		 */
		if (outputObserver != null) {
		    outputObserver.checkAndNotify();

		    List<File> newOutputFiles = faListener.getNewOutputFiles();

		    outputProcessor.process(newOutputFiles, tgt, null);

		    try {
			outputObserver.destroy();
		    } catch (Exception e) {
			// ignore
		    }
		}

		result.addProcessFlowInfo(this, 500, tgt.getClassName(), modelProviderId);
	    }
	}
    }

    /**
     * @param outputDirectory directory to observe, if <code>null</code> then "." is
     *                        assumed
     * @param listener
     * @param tgt
     * @return observer for the given output directory, can be <code>null</code> if
     *         an error occurred while setting up the observer (in that case, the
     *         error is logged)
     */
    private FileAlterationObserver setupOutputFileAlterationObserver(String outputDirectory,
	    OutputFileAlterationListener listener, TargetConfiguration tgt) {

	String outputDir = outputDirectory != null ? outputDirectory : ".";
	File outputDirectoryFile = new File(outputDir);
	if (!outputDirectoryFile.exists()) {
	    try {
		FileUtils.forceMkdir(outputDirectoryFile);
	    } catch (IOException e) {
		result.addProcessFlowError(this, 516, outputDir);
		return null;
	    }
	}

	try {
	    // 20170818 JE: File filters could be added here as well
	    FileAlterationObserver observer = new FileAlterationObserver(outputDirectoryFile);

	    observer.initialize();
	    observer.addListener(listener);
	    return observer;

	} catch (Exception e) {

	    result.addProcessFlowError(this, 517, outputDir);
	    return null;
	}
    }

    private void executeTransformations(Model model, List<TransformerConfiguration> transformerConfigs)
	    throws Exception {

	/*
	 * First of all count the transformers that are disabled. Then subtract this
	 * number from the length of the transformer configuration list. If the result
	 * is greater than 1 then a copy of the input model must be created before
	 * executing each transformation. Otherwise we only have one executable
	 * transformation and therefore the input model can be used as is - unless it is
	 * not an instance of GenericModel.
	 * 
	 * The information on which is the last transformer that is enabled is also
	 * relevant for releasing the original model once the transformation(s) have
	 * been executed.
	 */
	int numberOfDisabledTransformers = 0;
	String idOfLastEnabledTransformer = "UNKNOWN";
	for (TransformerConfiguration trf : transformerConfigs) {
	    if (trf.getProcessMode() == ProcessMode.disabled) {
		numberOfDisabledTransformers++;
	    } else {
		idOfLastEnabledTransformer = trf.getId();
	    }
	}
	boolean modelCopyRequired = (transformerConfigs.size() - numberOfDisabledTransformers) > 1
		|| !(model instanceof GenericModel);

	for (TransformerConfiguration trf : transformerConfigs) {

	    options.setCurrentProcessConfig(trf);

	    // execute the transformer
	    GenericModel modelOutput = null;

	    if (trf.getProcessMode() == ProcessMode.disabled) {

		/*
		 * because this transformation is disabled, we won't process it - and all
		 * depending transformations and targets (only for those inputs that depend upon
		 * this transformation)
		 */
		processIdsToIgnore.add(trf.getId());

		result.addProcessFlowInfo(this, 506, trf.getId());

	    } else if (processIdsToIgnore.contains(trf.getInputId())) {
		/*
		 * the transformation that this transformation uses as input and thus depends on
		 * shall be ignored - thus also ignore this transformation in further processing
		 * 
		 * it is important to perform this check so that all dependencies are added to
		 * the ignore list which can be considered in target execution and especially
		 * execution of deferrable output writers
		 */
		processIdsToIgnore.add(trf.getId());

	    } else if (!isValidModel(model, trf.getValidatorIds(), trf.getId(), trf.getInputId())) {
		/*
		 * the model that this transformation uses as input did not pass validation -
		 * thus ignore this transformation in further processing
		 */
		processIdsToIgnore.add(trf.getId());
	    }

	    /*
	     * only execute the transformer if it shall not be ignored (reason being that
	     * the transformation is disabled, that one of the transformations that this
	     * transformation depends upon is disabled or did not succeed, or that the input
	     * model did not pass validation)
	     */
	    if (!processIdsToIgnore.contains(trf.getId())) {

		// process the model
		try {
		    result.addProcessFlowInfo(this, 501, trf.getId(), trf.getInputId());

		    GenericModel modelInput;
		    if (modelCopyRequired) {
			result.addProcessFlowDebug("Creating GenericModel...");
			// create generic model from model
			modelInput = new GenericModel(model);
			result.addProcessFlowDebug("...done.");
		    } else {
			result.addProcessFlowDebug("Creation of GenericModel is not required.");
			modelInput = (GenericModel) model;
		    }

		    // FIXME
		    /*
		     * 2017-06-20 JE: For the execution of the first transformers, we need to have
		     * the rules from all targets in Options.java. The reason is that some checks
		     * are performed that try to match on specific target conversion rules. An
		     * example is method ClassInfoEA.baseClass(), which at some point may need to
		     * match on "rule-xsd-cls-mixin-classes-non-mixin-supertypes". When creating a
		     * copy from an EA model, it would be problematic if the rules configured by the
		     * targets were no longer known. Thus, we reset the fields of Options.java only
		     * after the GenericModel copy has been created. Eventually, what we really want
		     * is to avoid dependencies on target conversion rules when loading a model.
		     * Target specific model checks should be performed separately. Refactoring them
		     * into target specific model validation classes may be the way to go.
		     */
		    options.resetFields();

		    TransformationManager trfManager = new TransformationManager();

		    StatusBoard.getStatusBoard().statusChanged(STATUS_TRANSFORMER_PROCESS);

		    modelOutput = trfManager.process(modelInput, options, trf, result);

		    /*
		     * Release the original model now if: 1) a GenericModel copy has been created
		     * for it (we should not shut a GenericModel down if it is being used as-is in a
		     * chain of transformations) and b) we have reached the last enabled
		     * transformation.
		     */
		    if (modelCopyRequired && trf.getId().equals(idOfLastEnabledTransformer)) {
			result.addProcessFlowDebug("Releasing model created by processing step: " + trf.getInputId());
			this.release(model);
		    }

		    result.addProcessFlowInfo(this, 502, trf.getId(), trf.getInputId());

		} catch (ClassCastException e) {

		    processIdsToIgnore.add(trf.getId());

		    result.addProcessFlowError(this, 505, e.getMessage(), trf.getId());

		    StackTraceElement[] stes = e.getStackTrace();

		    if (stes != null) {

			for (StackTraceElement ste : stes) {
			    result.addProcessFlowDebug(ste.toString());
			}
		    }
		}
	    }

	    // Thread.sleep(2000);

	    /*
	     * Even if the transformation was not executed (because it was on the ignore
	     * list) or did not succeed, continue processing the dependent transformations
	     * and targets. Execution of dependent transformations will check if the input
	     * shall be ignored and add their IDs to the processIdsToIgnore list. The target
	     * execution also checks if the model provider ID is on the ignore list and
	     * skips execution if it is (same for the deferrable output writer execution)
	     */

	    // execute all targets on the transformed model
	    this.executeTargets(modelOutput, trf.getId(), trf.getTargets());

	    // execute all dependent transformers on the transformed model
	    this.executeTransformations(modelOutput, trf.getTransformers());

	}
    }

    private boolean isValidModel(Model model, List<String> validatorIds, String idOrClassName, String modelProviderId)
	    throws ShapeChangeAbortException {

	/*
	 * Do not validate the model if no validators are configured, or if all
	 * validators are disabled.
	 */
	if (validatorIds == null || validatorIds.isEmpty() || validatorIds.stream()
		.allMatch(id -> options.getValidatorConfigs().get(id).getProcessMode() == ProcessMode.disabled)) {
	    return true;
	}

	StatusBoard.getStatusBoard().statusChanged(STATUS_VALIDATION_START);

	result.addProcessFlowInfo(this, 519, modelProviderId, idOrClassName);

	ModelValidationManager validationManager = new ModelValidationManager();
	boolean validationSucceeded = validationManager.isValid(model, validatorIds);

	result.addProcessFlowInfo(this, 520, modelProviderId, idOrClassName);

	return validationSucceeded;
    }

    private void executeTargets(Model model, String modelProviderId, List<TargetConfiguration> targetConfigs)
	    throws ShapeChangeAbortException, ClassNotFoundException, InstantiationException, IllegalAccessException,
	    NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException {

	if (processIdsToIgnore.contains(modelProviderId)) {
	    // do not execute this target
	    return;
	}

	for (TargetConfiguration tgt : targetConfigs) {

	    if (tgt.getProcessMode().equals(ProcessMode.disabled))
		continue;

	    // reset options for this target
	    options.setCurrentProcessConfig(tgt);
	    options.resetFields();

	    if (!isValidModel(model, tgt.getValidatorIds(), tgt.getClassName(), modelProviderId)) {
		continue;
	    }

	    // now execute the target

	    // Prepare targets
	    resetAllSingleTargets();

	    // update the outputDirectory parameter by appending the id of the
	    // model provider
	    String outputDirectory = options.parameter(tgt.getClassName(), "outputDirectory");
	    if (outputDirectory != null && outputDirectory.length() > 0) {
		outputDirectory = outputDirectory.trim() + File.separator + modelProviderId;
		options.setParameter(tgt.getClassName(), "outputDirectory", outputDirectory);
	    }

	    OutputFileAlterationListener faListener = new OutputFileAlterationListener();
	    FileAlterationObserver outputObserver = setupOutputFileAlterationObserver(outputDirectory, faListener, tgt);

	    SortedSet<? extends PackageInfo> selectedSchemas = model.selectedSchemas();

	    mainSchemaForSingleTargets = TargetUtil.findMainSchemaForSingleTargets(selectedSchemas, options, result);

	    String classname = tgt.getClassName();
	    ProcessMode tmode = options.targetMode(classname);
	    Class<?> theClass = Class.forName(classname);
	    boolean targetCalled = false;

	    for (PackageInfo pi : selectedSchemas) {

		if (pi == null) {
		    continue;
		}

		/*
		 * Only process schemas in a namespace and name that matches a user-selected
		 * pattern. Selection parameters are defined either by the target or the input
		 * configuration.
		 */
		if (options.skipSchema(pi))
		    continue;

		String name = pi.name();
		String ns = pi.targetNamespace();
		if (ns == null) {
		    ns = "(no namespace)";
		}
		result.addProcessFlowInfo(this, 1012, name, ns);

		if (tmode.equals(ProcessMode.disabled))
		    continue;

		target = (Target) theClass.getConstructor().newInstance();

		if (target != null) {

		    targetCalled = true;

		    result.addProcessFlowInfo(this, 503, target.getTargetName(), modelProviderId);

		    StatusBoard.getStatusBoard().statusChanged(STATUS_TARGET_INITSTART);
		    target.initialise(pi, model, options, result, tmode.equals(ProcessMode.diagnosticsonly));

		    StatusBoard.getStatusBoard().statusChanged(STATUS_TARGET_PROCESS);

		    ClassInfo[] classArr = getClasses(model, pi);
		    for (int cidx = 0; cidx < classArr.length; cidx++) {
			ClassInfo k = classArr[cidx];
			target.process(k);
		    }

		    StatusBoard.getStatusBoard().statusChanged(STATUS_TARGET_WRITE);
		    target.write();
		    /*
		     * 2016-03-05 JE: does not seem to be used by StatusReaders
		     * StatusBoard.getStatusBoard().statusChanged(0);
		     */

		    /*
		     * === process output files, if applicable ===
		     */
		    if (outputObserver != null) {

			if (isSingleTarget(theClass) || isDeferrableOutputWriter(theClass)) {

			    /*
			     * postprocessing of output files from single targets and deferrable output
			     * writers not handled here
			     */

			} else {

			    /*
			     * Postprocess output files
			     */

			    outputObserver.checkAndNotify();

			    List<File> newOutputFiles = faListener.getNewOutputFiles();

			    outputProcessor.process(newOutputFiles, tgt, pi);
			}

			/*
			 * re-initialize the observer, i.e. let the observer get all files that now
			 * exist in the output directory and use these files as baseline for next
			 * comparison
			 */
			try {
			    outputObserver.initialize();
			} catch (Exception e) {
			    result.addProcessFlowError(this, 517, outputDirectory);
			}
		    }
		}

		target = null;
	    }

	    // write results for targets where the results are ready only after
	    // all schemas have been processed
	    if (!tmode.equals(ProcessMode.disabled) && targetCalled) {

		if (isSingleTarget(theClass)) {

		    SingleTarget starget = (SingleTarget) theClass.getConstructor().newInstance();

		    /*
		     * announce target class-wide so that StatusReaders can inspect it ...
		     */
		    target = starget;
		    if (starget != null) {

			StatusBoard.getStatusBoard().statusChanged(STATUS_TARGET_WRITEALL);

			starget.writeAll(result);

			/*
			 * === process output files, if applicable ===
			 */
			if (outputObserver != null) {
			    if (isDeferrableOutputWriter(theClass)) {

				/*
				 * Processing of output files from deferrable output writers not handled here
				 */

			    } else {

				/*
				 * Process output files
				 */

				outputObserver.checkAndNotify();

				List<File> newOutputFiles = faListener.getNewOutputFiles();

				outputProcessor.process(newOutputFiles, tgt, mainSchemaForSingleTargets);

				/*
				 * re-initialize the observer, i.e. let the observer get all files that now
				 * exist in the output directory and use these files as baseline for next
				 * comparison
				 */
				try {
				    outputObserver.initialize();
				} catch (Exception e) {
				    result.addProcessFlowError(this, 517, outputDirectory);
				}
			    }
			}
		    }
		    /*
		     * ... now we no longer need to keep track of the target
		     */
		    target = null;
		}
	    }

	    if (outputObserver != null) {

		try {
		    outputObserver.destroy();
		} catch (Exception e) {
		    // ignore
		}
	    }

	    result.addProcessFlowInfo(this, 504, tgt.getClassName(), modelProviderId);
	}
    }

    /**
     * Returns the classes belonging to the package and all child packages that are
     * in the same target namespace, sorted in alphabetical order as defined via the
     * configuration.
     * 
     * @param model
     * @param pi
     * @return
     * @throws NoSuchMethodException
     * @throws SecurityException
     */
    @SuppressWarnings("rawtypes")
    private ClassInfo[] getClasses(Model model, PackageInfo pi) throws NoSuchMethodException, SecurityException {
	Set<ClassInfo> classes = model.classes(pi);
	// Change the order of processing if requested by sortedOutput parameter
	String sortedOpt = options.parameter(target.getClass().getName(), "sortedOutput");
	if (sortedOpt == null)
	    sortedOpt = options.parameter("sortedOutput");

	ClassInfo[] classArr = new ClassInfo[classes.size()];
	classes.toArray(classArr);

	if (sortedOpt != null && !"false".equalsIgnoreCase(sortedOpt)) {
	    // TreeSet is not applicable here (ClassInfo is not comparable) =>
	    // use an array.

	    String compField;
	    Class[] paramTypes = null;
	    Object[] pargs = null;

	    // Make it simple and allow all methods. "true" means "name".
	    if ("true".equalsIgnoreCase(sortedOpt)) {
		compField = "name";
	    } else if (sortedOpt.startsWith("taggedValue=")) {
		compField = "taggedValue";
		String parg = sortedOpt.split("=")[1];
		paramTypes = new Class[] { String.class };
		pargs = new Object[] { parg };
	    } else
		compField = sortedOpt;

	    // test
	    try {
		ClassInfo.class.getMethod(compField, (Class[]) paramTypes);
	    } catch (NoSuchMethodException e) {
		result.addProcessFlowError(this, 165, sortedOpt, target.getClass().getName());
		return classArr;
	    }

	    final Method compMeth = ClassInfo.class.getMethod(compField, (Class[]) paramTypes);
	    final Object[] compMethArgs = pargs;

	    Arrays.sort(classArr, new Comparator<ClassInfo>() {
		public int compare(ClassInfo ci1, ClassInfo ci2) {
		    try {
			// TBD: if two classes had the same name, a
			// comparison via the id should be performed as well
			if (compMeth.getName().equals("taggedValue")) {
			    String s1 = ci1.taggedValue((String) compMethArgs[0]);
			    String s2 = ci2.taggedValue((String) compMethArgs[0]);
			    if (StringUtils.isBlank(s1))
				s1 = ci1.name();
			    if (StringUtils.isBlank(s2))
				s2 = ci2.name();
			    return s1.compareTo(s2);
			} else
			    return ((String) compMeth.invoke(ci1)).compareTo((String) compMeth.invoke(ci2));
		    } catch (Exception e) {
			String m = e.getMessage();
			if (m != null)
			    result.addProcessFlowError(m);
		    }
		    return 0;
		}
	    });
	}
	return classArr;
    }

    /*
     * SingleTarget store information on the class level (due to the fact that a
     * Target instance is created for each processed schema, but for SingleTarget
     * instances the result can only be created after all schemas have been
     * processed), so they have to be reseted before a new conversion.
     */
    private void resetAllSingleTargets() throws ClassNotFoundException, InstantiationException, IllegalAccessException,
	    IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
	Vector<String> targets = options.targets();
	for (Iterator<String> j = targets.iterator(); j.hasNext();) {
	    String classname = j.next();
	    Class<?> theClass = Class.forName(classname);
	    target = (Target) theClass.getConstructor().newInstance();

	    if (target != null) {
		ProcessMode tmode = options.targetMode(classname);
		if (tmode.equals(ProcessMode.disabled))
		    continue;

		if (isSingleTarget(theClass)) {
		    SingleTarget starget = (SingleTarget) theClass.getConstructor().newInstance();
		    if (starget != null) {
			starget.reset();
		    }
		}
	    }
	}
    }

    @SuppressWarnings("rawtypes")
    private boolean isSingleTarget(Class theClass) {

	for (Class intfc : theClass.getInterfaces()) {
	    String in = intfc.getName();
	    if (in.equals("de.interactive_instruments.shapechange.core.target.SingleTarget")) {
		return true;
	    }
	}
	return false;
    }

    @SuppressWarnings("rawtypes")
    private boolean isDeferrableOutputWriter(Class theClass) {

	for (Class intfc : theClass.getInterfaces()) {
	    String in = intfc.getName();
	    if (in.equals("de.interactive_instruments.shapechange.core.target.DeferrableOutputWriter")) {
		return true;
	    }
	}

	return false;
    }

    @Override
    public String message(int mnr) {

	/*
	 * NOTE: A leading ?? in a message text suppresses multiple appearance of a
	 * message in the output.
	 */
	switch (mnr) {

	case 14:
	    return "No model has been loaded to convert.";
	case 165:
	    return "Value '$1$' is not allowed for targetParameter 'sortedOutput' in Target '$2$'. Try 'true' (=name), 'name', 'id', 'taggedValue=value' or 'false' (no sorting). 'false' is used.";

	case 500:
	    return "Executed deferred output write for target class '$1$' for input ID: '$2$'.";
	case 501:
	    return "Now processing transformation '$1$' for input ID: '$2$'.";
	case 502:
	    return "Performed transformation for transformer ID '$1$' for input ID: '$2$'.\n-------------------------------------------------";
	case 503:
	    return "Now processing target '$1$' for input '$2$'.";
	case 504:
	    return "Executed target class '$1$' for input ID: '$2$'.\n-------------------------------------------------";
	case 505:
	    return "Internal class cast exception encountered - message: $1$ (full exception information is only logged for log level debug). Processing of transformation with ID '$2$' did not succeed. All transformations and targets that depend on this transformation will not be executed.";
	case 506:
	    return "Transformation with ID '$1$' is disabled (via the configuration). All transformations and targets that depend on this transformation will not be executed.";
	case 507:
	    return "None of the packages contained in the model is a schema selected for processing. Make sure that the schema you want to process are configured to be a schema (via the 'targetNamespace' tagged value or via a PackageInfo element in the configuration) and also selected for processing (if you use one of the input parameters appSchemaName, appSchemaNameRegex, appSchemaNamespaceRegex, ensure that they include the schema). Execution will stop now.";
	case 508:
	    return "??The ConfigurationValidator for transformer, target, or validator class '$1$' was found but could not be loaded. Exception message is: $2$";
	case 509:
	    return "The semantic validation of the ShapeChange configuration detected one or more errors. Examine the log for further details. Execution will stop now.";
	case 510:
	    return "---------- Semantic validation of ShapeChange configuration: START ----------";
	case 511:
	    return "---------- Semantic validation of ShapeChange configuration: COMPLETE ----------";
	case 512:
	    return "---------- Semantic validation of ShapeChange configuration: SKIPPED ----------";
	case 513:
	    return "NOTE: The semantic validation can be skipped by setting the input configuration parameter '"
		    + Options.PARAM_SKIP_SEMANTIC_VALIDATION_OF_CONFIG + "' to 'true'.";
	case 514:
	    return "--- Validating transformer with @id '$1$' ...";
	case 515:
	    return "--- Validating target with @class '$1$' and @inputs '$2$' ...";
	case 516:
	    return "Could not create output directory '$1$' and thus could not set up file observer to identify output files that are created in this directory by targets. Processing of output files in this directory will not be performed.";
	case 517:
	    return "Could not initialize file observer for output directory '$1$'. The file observer would be used to identify output files that are created in this directory by targets. Processing of output files in this directory will not be performed.";
	case 518:
	    return "--- Validating model validator with @id '$1$' ...";
	case 519:
	    return "Now validating input model '$1$' of transformation/target with id/class '$2$'.";
	case 520:
	    return "Validated input model '$1$' of transformation/target with id/class '$2$'.\n-------------------------------------------------";

	case 1012:
	    return "Application schema found, package name: '$1$', target namespace: '$2$'";

	default:
	    return "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	}
    }
}
