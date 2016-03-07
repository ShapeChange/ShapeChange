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
 * (c) 2002-2013 interactive instruments GmbH, Bonn, Germany
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

package de.interactive_instruments.ShapeChange;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.Vector;

import org.xml.sax.SAXException;

import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.Transformer;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel;
import de.interactive_instruments.ShapeChange.Target.DeferrableOutputWriter;
import de.interactive_instruments.ShapeChange.Target.SingleTarget;
import de.interactive_instruments.ShapeChange.Target.Target;
import de.interactive_instruments.ShapeChange.Transformation.TransformationManager;
import de.interactive_instruments.ShapeChange.UI.StatusBoard;

public class Converter {

	public static final int STATUS_TARGET_INITSTART = 201;
	public static final int STATUS_TARGET_PROCESS = 202;
	public static final int STATUS_TARGET_WRITE = 203;
	public static final int STATUS_TARGET_WRITEALL = 204;
	public static final int STATUS_TARGET_DEFERRED_WRITE = 205;
	public static final int STATUS_TRANSFORMER_PROCESS = 206;

	/** Result object. */
	protected ShapeChangeResult result = null;
	protected Options options = null;
	protected Target target = null;
	protected Set<String> processIdsToIgnore = new HashSet<String>();

	public Converter(Options o, ShapeChangeResult r) {
		options = o;
		result = r;
		target = null;
	};

	public void convert() throws ShapeChangeAbortException {

		if (options.isOnlyDeferrableOutputWrite()) {

			executeDeferrableOutputWriters();

		} else {

			// process model as usual
			Model m = getModel();

			convert(m);
		}
	}

	private void executeDeferrableOutputWriters() {

		try {

			// write outputs for any DeferrableOutputWriter
			this.executeDeferrableOutputWriters(
					options.getTargetConfigurations());

		} catch (Exception e) {
			String m = e.getMessage();
			if (m != null) {
				result.addError(m);
			}
			Exception se = e;
			if (e instanceof SAXException) {
				se = ((SAXException) e).getException();
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

	/** Convert the application schema. */
	public void convert(Model model) {

		try {
			if (model == null) {
				result.addFatalError(null, 14);
				throw new ShapeChangeAbortException();
			}

			model.loadInformationFromExternalSources();

			// Prepare and check model
			model.postprocessAfterLoadingAndValidate();

			// simply return if no schema is selected for processing
			SortedSet<? extends PackageInfo> selectedSchema = model
					.selectedSchemas();

			if (selectedSchema == null || selectedSchema.isEmpty()) {

				result.addWarning(null, 507);
				release(model);

			} else {

				// at first run any targets that directly reference the input
				// model
				this.executeTargets(model, options.getInputId(),
						options.getInputTargetConfigs());

				// now recursively execute the transformations (and associated
				// targets) defined for the input model
				this.executeTransformations(model,
						options.getInputTransformerConfigs());

				/*
				 * do this before executing deferrable output writers to free
				 * memory (important in case of very large models)
				 */
				release(model);

				// now write outputs for any DeferrableOutputWriter
				this.executeDeferrableOutputWriters(
						options.getTargetConfigurations());
			}

		} catch (Exception e) {
			String m = e.getMessage();
			if (m != null) {
				result.addError(m);
			}
			Exception se = e;
			if (e instanceof SAXException) {
				se = ((SAXException) e).getException();
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
	 * Writes the output for all DeferrableOutputWriters that are contained in
	 * the list of all targets from the ShapeChange configuration.
	 * 
	 * @param targetConfigs
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	private void executeDeferrableOutputWriters(
			List<TargetConfiguration> targetConfigs) throws Exception {

		for (TargetConfiguration tgt : targetConfigs) {

			if (tgt.getProcessMode().equals(ProcessMode.disabled))
				continue;

			String classname = tgt.getClassName();
			Class theClass = Class.forName(classname);
			boolean isDeferrableOutputWriter = false;
			for (Class intfc : theClass.getInterfaces()) {
				String in = intfc.getName();
				if (in.equals(
						"de.interactive_instruments.ShapeChange.Target.DeferrableOutputWriter")) {
					isDeferrableOutputWriter = true;
					break;
				}
			}

			if (!isDeferrableOutputWriter) {
				continue;
			}

			// reset options for this target
			options.setCurrentProcessConfig(tgt);
			options.resetFields();

			// update the outputDirectory parameter by appending the id of the
			// model provider
			String outputDirectoryForTarget = options
					.parameter(tgt.getClassName(), "outputDirectory");

			for (String modelProviderId : tgt.getInputIds()) {

				if (processIdsToIgnore.contains(modelProviderId)) {
					continue;
				}

				if (outputDirectoryForTarget != null
						&& outputDirectoryForTarget.length() > 0) {
					String outputDirectory = outputDirectoryForTarget.trim()
							+ File.separator + modelProviderId;
					options.setParameter(tgt.getClassName(), "outputDirectory",
							outputDirectory);
				}

				DeferrableOutputWriter dowTarget = (DeferrableOutputWriter) theClass
						.newInstance();

				dowTarget.initialise(options, result);

				StatusBoard.getStatusBoard()
						.statusChanged(STATUS_TARGET_DEFERRED_WRITE);
				dowTarget.writeOutput();

				dowTarget = null;

				result.addInfo(null, 500, tgt.getClassName(), modelProviderId);
			}
		}
	}

	private void executeTransformations(Model model,
			List<TransformerConfiguration> transformerConfigs)
					throws Exception {

		/*
		 * First of all count the transformers that are disabled. Then subtract
		 * this number from the length of the transformer configuration list. If
		 * the result is greater than 1 then a copy of the input model must be
		 * created before executing each transformation. Otherwise we only have
		 * one executable transformation and therefore the input model can be
		 * used as is - unless it is not an instance of GenericModel.
		 * 
		 * The information on which is the last transformer that is enabled is
		 * also relevant for releasing the original model once the
		 * transformation(s) have been executed.
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
		boolean modelCopyRequired = (transformerConfigs.size()
				- numberOfDisabledTransformers) > 1
				|| !(model instanceof GenericModel);

		for (TransformerConfiguration trf : transformerConfigs) {

			// first of all reset options for the transformer
			options.setCurrentProcessConfig(trf);
			options.resetFields();

			// execute the transformer
			GenericModel modelOutput = null;

			if (trf.getProcessMode() == ProcessMode.disabled) {

				/*
				 * because this transformation is disabled, we won't process it
				 * - and all depending transformations and targets (only for
				 * those inputs that depend upon this transformation)
				 */
				processIdsToIgnore.add(trf.getId());

				result.addInfo(null, 506, trf.getId());

			} else if (processIdsToIgnore.contains(trf.getInputId())) {
				/*
				 * the transformation that this transformation uses as input and
				 * thus depends on shall be ignored - thus also ignore this
				 * transformation in further processing
				 * 
				 * it is important to perform this check so that all
				 * dependencies are added to the ignore list which can be
				 * considered in target execution and especially execution of
				 * deferrable output writers
				 */
				processIdsToIgnore.add(trf.getId());
			}

			/*
			 * only execute the transformer if it shall not be ignored (reason
			 * being that the transformation is disabled or that one of the
			 * transformations that this transformation depends upon is disabled
			 * or did not succeed)
			 */
			if (!processIdsToIgnore.contains(trf.getId())) {

				// process the model
				try {
					result.addInfo(null, 501, trf.getId(), trf.getInputId());

					GenericModel modelInput;
					if (modelCopyRequired) {
						result.addDebug("Creating GenericModel...");
						// create generic model from model
						modelInput = new GenericModel(model);
						result.addDebug("...done.");
					} else {
						result.addDebug(
								"Creation of GenericModel is not required.");
						modelInput = (GenericModel) model;
					}

					TransformationManager trfManager = new TransformationManager();

					StatusBoard.getStatusBoard()
							.statusChanged(STATUS_TRANSFORMER_PROCESS);

					modelOutput = trfManager.process(modelInput, options, trf,
							result);

					/*
					 * Release the original model now if: 1) a GenericModel copy
					 * has been created for it (we should not shut a
					 * GenericModel down if it is being used as-is in a chain of
					 * transformations) and b) we have reached the last enabled
					 * transformation.
					 */
					if (modelCopyRequired
							&& trf.getId().equals(idOfLastEnabledTransformer)) {
						result.addDebug(
								"Releasing model created by processing step: "
										+ trf.getInputId());
						this.release(model);
					}

					result.addInfo(null, 502, trf.getId(), trf.getInputId());

				} catch (ClassCastException e) {

					processIdsToIgnore.add(trf.getId());

					result.addError(null, 505, e.getMessage(), trf.getId());

					StackTraceElement[] stes = e.getStackTrace();

					if (stes != null) {

						for (StackTraceElement ste : stes) {
							result.addDebug(ste.toString());
						}
					}
				}
			}

			// Thread.sleep(2000);

			/*
			 * Even if the transformation was not executed (because it was on
			 * the ignore list) or did not succeed, continue processing the
			 * dependent transformations and targets. Execution of dependent
			 * transformations will check if the input shall be ignored and add
			 * their IDs to the processIdsToIgnore list. The target execution
			 * also checks if the model provider ID is on the ignore list and
			 * skips execution if it is (same for the deferrable output writer
			 * execution)
			 */

			// execute all targets on the transformed model
			this.executeTargets(modelOutput, trf.getId(), trf.getTargets());

			// execute all dependent transformers on the transformed model
			this.executeTransformations(modelOutput, trf.getTransformers());

		}
	}

	@SuppressWarnings("rawtypes")
	private void executeTargets(Model model, String modelProviderId,
			List<TargetConfiguration> targetConfigs)
					throws ShapeChangeAbortException, ClassNotFoundException,
					InstantiationException, IllegalAccessException,
					NoSuchMethodException, SecurityException {

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

			// now execute the target

			// Prepare targets
			resetSingleTargets();

			// update the outputDirectory parameter by appending the id of the
			// model provider
			String outputDirectory = options.parameter(tgt.getClassName(),
					"outputDirectory");
			if (outputDirectory != null && outputDirectory.length() > 0) {
				outputDirectory = outputDirectory.trim() + File.separator
						+ modelProviderId;
				options.setParameter(tgt.getClassName(), "outputDirectory",
						outputDirectory);
			}

			SortedSet<? extends PackageInfo> selectedSchema = model
					.selectedSchemas();

			String classname = tgt.getClassName();
			String tmode = options.targetMode(classname);
			Class theClass = Class.forName(classname);
			boolean targetCalled = false;

			for (PackageInfo pi : selectedSchema) {

				if (pi == null) {
					continue;
				}

				// Only process schemas in a namespace and name that matches a
				// user-selected pattern
				if (options.skipSchema(null, pi))
					continue;

				String name = pi.name();
				String ns = pi.targetNamespace();
				result.addInfo(null, 1012, name, ns);

				if (tmode.equals(ProcessMode.disabled))
					continue;

				target = (Target) theClass.newInstance();

				if (target != null) {
					// filter additionally for target specific application
					// schema names
					if (options.skipSchema(target, pi))
						continue;

					targetCalled = true;

					result.addInfo(null, 503,
							options.nameOfTarget(target.getTargetID()),
							modelProviderId);

					StatusBoard.getStatusBoard()
							.statusChanged(STATUS_TARGET_INITSTART);
					target.initialise(pi, model, options, result,
							tmode.equals(ProcessMode.diagnosticsonly));

					StatusBoard.getStatusBoard()
							.statusChanged(STATUS_TARGET_PROCESS);

					ClassInfo[] classArr = classes(model, pi);
					for (int cidx = 0; cidx < classArr.length; cidx++) {
						ClassInfo k = classArr[cidx];
						target.process(k);
					}

					StatusBoard.getStatusBoard()
							.statusChanged(STATUS_TARGET_WRITE);
					target.write();
					/*
					 * 2016-03-05 JE: does not seem to be used by StatusReaders
					 * StatusBoard.getStatusBoard().statusChanged(0);
					 */
				}

				target = null;
			}

			// write results for targets where the results are ready only after
			// all schemas have been processed
			if (!tmode.equals(ProcessMode.disabled) && targetCalled) {
				boolean isSingleTarget = false;
				for (Class intfc : theClass.getInterfaces()) {
					String in = intfc.getName();
					if (in.equals(
							"de.interactive_instruments.ShapeChange.Target.SingleTarget")) {
						isSingleTarget = true;
						break;
					}
				}
				if (isSingleTarget) {
					SingleTarget starget = (SingleTarget) theClass
							.newInstance();

					/*
					 * announce target class-wide so that StatusReaders can
					 * inspect it ...
					 */
					target = starget;
					if (starget != null) {
						StatusBoard.getStatusBoard()
								.statusChanged(STATUS_TARGET_WRITEALL);
						starget.writeAll(result);
					}
					/*
					 * ... now we no longer need to keep track of the target
					 */
					target = null;
				}
			}
			result.addInfo(null, 504, tgt.getClassName(), modelProviderId);
		}

	}

	/**
	 * Returns the classes belonging to the package and all child packages that
	 * are in the same target namespace, sorted in alphabetical order as defined
	 * via the configuration.
	 * 
	 * @param model
	 * @param pi
	 * @return
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	@SuppressWarnings("rawtypes")
	private ClassInfo[] classes(Model model, PackageInfo pi)
			throws NoSuchMethodException, SecurityException {
		Set<ClassInfo> classes = model.classes(pi);
		// Change the order of processing if requested by sortedOutput parameter
		String sortedOpt = options.parameter(target.getClass().getName(),
				"sortedOutput");
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
				result.addError(null, 165, sortedOpt,
						target.getClass().getName());
				return classArr;
			}

			final Method compMeth = ClassInfo.class.getMethod(compField,
					(Class[]) paramTypes);
			final Object[] compMethArgs = pargs;

			Arrays.sort(classArr, new Comparator<ClassInfo>() {
				public int compare(ClassInfo ci1, ClassInfo ci2) {
					try {
						// TBD: if two classes had the same name, a
						// comparison via the id should be performed as well
						if (compMeth.getName().equals("taggedValue")) {
							String s1 = ci1
									.taggedValue((String) compMethArgs[0]);
							String s2 = ci2
									.taggedValue((String) compMethArgs[0]);
							if (s1 == null)
								s1 = ci1.name();
							if (s2 == null)
								s2 = ci2.name();
							return s1.compareTo(s2);
						} else
							return ((String) compMeth.invoke(ci1))
									.compareTo((String) compMeth.invoke(ci2));
					} catch (Exception e) {
						String m = e.getMessage();
						if (m != null)
							result.addError(m);
					}
					return 0;
				}
			});
		}
		return classArr;
	}

	/*
	 * 2013-12-16 CP: deprecated as it is unused private PackageInfo[]
	 * schemas(Model model, String appSchemaName) throws
	 * ShapeChangeAbortException { Set<PackageInfo> schemas =
	 * model.schemas(appSchemaName);
	 * 
	 * if (schemas.isEmpty()) { if (appSchemaName.equals("")) {
	 * result.addFatalError(null, 12); } else { result.addFatalError(null, 13,
	 * appSchemaName); } throw new ShapeChangeAbortException(); }
	 * 
	 * PackageInfo[] schemaArr = new PackageInfo[schemas.size()];
	 * schemas.toArray(schemaArr);
	 * 
	 * // Sort packages if the parameter "sortedSchemaOutput" is set to "true"
	 * if (options.sortedSchemaOutput || "true".equalsIgnoreCase(options
	 * .parameter("sortedSchemaOutput"))) { Arrays.sort(schemaArr, new
	 * Comparator<PackageInfo>() { public int compare(PackageInfo pi1,
	 * PackageInfo pi2) { return pi1.name().compareTo(pi2.name()); } }); }
	 * 
	 * return schemaArr; }
	 */

	/*
	 * SingleTarget store information on the class level (due to the fact that a
	 * Target instance is created for each processed schema, but for
	 * SingleTarget instances the result can only be created after all schemas
	 * have been processed), so they have to be reseted before a new conversion.
	 */
	@SuppressWarnings("rawtypes")
	private void resetSingleTargets() throws ClassNotFoundException,
			InstantiationException, IllegalAccessException {
		Vector<String> targets = options.targets();
		for (Iterator<String> j = targets.iterator(); j.hasNext();) {
			String classname = j.next();
			Class theClass = Class.forName(classname);
			target = (Target) theClass.newInstance();

			if (target != null) {
				String tmode = options.targetMode(classname);
				if (tmode.equals(ProcessMode.disabled))
					continue;

				boolean isSingleTarget = false;
				for (Class intfc : theClass.getInterfaces()) {
					String in = intfc.getName();
					if (in.equals(
							"de.interactive_instruments.ShapeChange.Target.SingleTarget")) {
						isSingleTarget = true;
						break;
					}
				}
				if (isSingleTarget) {
					SingleTarget starget = (SingleTarget) theClass
							.newInstance();
					if (starget != null) {
						starget.reset();
					}
				}
			}
		}
	}

	public int getCurrentTargetID() {
		if (target == null)
			return 0;
		else
			return target.getTargetID();
	}

	private Model getModel() throws ShapeChangeAbortException {

		String imt = options.parameter("inputModelType");

		String mdl = options.parameter("inputFile");

		String repoFileNameOrConnectionString = options
				.parameter("repositoryFileNameOrConnectionString");

		String username = options.parameter("username");
		String password = options.parameter("password");

		String user = username == null ? "" : username;
		String pwd = password == null ? "" : password;

		// Support original model type codes
		if (imt.equalsIgnoreCase("ea7"))
			imt = "de.interactive_instruments.ShapeChange.Model.EA.EADocument";
		else if (imt.equalsIgnoreCase("xmi10"))
			imt = "de.interactive_instruments.ShapeChange.Model.Xmi10.Xmi10Document";
		else if (imt.equalsIgnoreCase("gsip"))
			imt = "us.mitre.ShapeChange.Model.GSIP.GSIPDocument";

		// Transformations of the model are only supported for EA models
		if (imt.equals(
				"de.interactive_instruments.ShapeChange.Model.EA.EADocument")) {
			String transformer = options.parameter("transformer");
			if (transformer != null && transformer.length() > 0) {

				// TBD: at the moment the 'old' transformer only works with the
				// inputFile parameter
				// if (mdl == null || mdl.trim().length() == 0) {
				// throw new
				// ShapeChangeAbortException("Transformation with 'transformer'
				// specified via the according input parameter is only supported
				// for models contained in EAP file - but no inputFile was
				// provided.");
				// }

				try {
					@SuppressWarnings("rawtypes")
					Class theClass = Class.forName(transformer);
					Transformer t = (Transformer) theClass.newInstance();
					t.initialise(options, result, mdl);
					t.transform();
					t.shutdown();
				} catch (Exception e) {
					e.printStackTrace();
					throw new ShapeChangeAbortException();
				}
			}
		}

		Model m = null;

		// Get model object from reflection API
		@SuppressWarnings("rawtypes")
		Class theClass;
		try {
			theClass = Class.forName(imt);
			if (theClass == null) {
				result.addFatalError(null, 17, imt);
				throw new ShapeChangeAbortException();
			}
			m = (Model) theClass.newInstance();
			if (m != null) {

				/*
				 * we accept path to EAP file or repository connection string
				 * via both inputFile and repositoryFileNameOrConnectionString
				 * parameters
				 */
				String repoConnectionInfo;

				if (repoFileNameOrConnectionString != null
						&& repoFileNameOrConnectionString.length() > 0) {
					repoConnectionInfo = repoFileNameOrConnectionString;
				} else if (mdl != null && mdl.length() > 0) {
					repoConnectionInfo = mdl;
				} else {
					result.addFatalError(null, 24);
					throw new ShapeChangeAbortException();
				}

				if (user.length() == 0) {
					m.initialise(result, options, repoConnectionInfo);
				} else {
					m.initialise(result, options, repoConnectionInfo, user,
							pwd);
				}
			} else {
				result.addFatalError(null, 17, imt);
				throw new ShapeChangeAbortException();
			}
		} catch (ClassNotFoundException e) {
			result.addFatalError(null, 17, imt);
			throw new ShapeChangeAbortException();
		} catch (InstantiationException e) {
			result.addFatalError(null, 19, imt);
			throw new ShapeChangeAbortException();
		} catch (IllegalAccessException e) {
			result.addFatalError(null, 20, imt);
			throw new ShapeChangeAbortException();
		}
		return m;
	}

} // class ShapeChange.Converter
