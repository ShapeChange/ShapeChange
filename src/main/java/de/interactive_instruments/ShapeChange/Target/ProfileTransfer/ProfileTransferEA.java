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
package de.interactive_instruments.ShapeChange.Target.ProfileTransfer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.sparx.Attribute;
import org.sparx.Connector;
import org.sparx.ConnectorEnd;
import org.sparx.Element;
import org.sparx.Repository;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.TargetIdentification;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Model.EA.AssociationInfoEA;
import de.interactive_instruments.ShapeChange.Model.EA.ClassInfoEA;
import de.interactive_instruments.ShapeChange.Model.EA.EADocument;
import de.interactive_instruments.ShapeChange.Model.EA.PropertyInfoEA;
import de.interactive_instruments.ShapeChange.Profile.Profiles;
import de.interactive_instruments.ShapeChange.Target.SingleTarget;
import de.interactive_instruments.ShapeChange.Util.EAException;
import de.interactive_instruments.ShapeChange.Util.EAModelUtil;
import de.interactive_instruments.ShapeChange.Util.EATaggedValue;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class ProfileTransferEA implements SingleTarget, MessageSource {

	/**
	 * Alias: none
	 * <p>
	 * Required / Optional: optional
	 * <p>
	 * Type: Boolean
	 * <p>
	 * Default Value: <code>false</code>
	 * <p>
	 * Explanation: By default, profile information is transferred into the EA
	 * repository that has originally been loaded by ShapeChange. If this target
	 * parameter is 'true' and if the EA repository containing the input model
	 * is an EA project file (.eap), then that file is copied into the output
	 * directory and only the copy will be processed by the target.
	 * <p>
	 * Applies to Rule(s): none – default behavior
	 */
	public static final String PARAM_TRANSFER_TO_EAP_COPY = "transferToCopyOfEAP";

	/**
	 * Alias: none
	 * <p>
	 * Required / Optional: optional
	 * <p>
	 * Type: Boolean
	 * <p>
	 * Default Value: <code>false</code>
	 * <p>
	 * Explanation: By default, the profile information in the model that is
	 * stored in the EA repository is overridden by this target for relevant
	 * model elements (classes and properties that are processed; for further
	 * details, see the explanation of parameter
	 * {@value #PARAM_PROCESS_ALL_SCHEMAS}) that are also contained in the input
	 * model. If this parameter is set to <code>true</code>, the profile
	 * information will be deleted in the classes and properties of the model in
	 * the EA repository before transferring any new profile information.
	 * <p>
	 * Applies to Rule(s): none – default behavior
	 */
	public static final String PARAM_DELETE_EA_MODEL_PROFILES = "deleteEAModelProfiles";

	/**
	 * Alias: none
	 * <p>
	 * Required / Optional: optional
	 * <p>
	 * Type: String (comma separated list of values)
	 * <p>
	 * Default Value: all profiles
	 * <p>
	 * Explanation: Names of profiles to be transferred.
	 * <p>
	 * Applies to Rule(s): none – default behavior
	 */
	public static final String PARAM_PROFILES_TO_TRANSFER = "profilesToTransfer";

	/**
	 * Alias: none
	 * <p>
	 * Required / Optional: optional
	 * <p>
	 * Type: Boolean
	 * <p>
	 * Default Value: <code>false</code>
	 * <p>
	 * Explanation: By default, profiles are transferred only for classes (and
	 * their properties) from schemas that are selected for processing. If this
	 * parameter is set to <code>true</code>, profiles are transferred for
	 * classes (and their properties) from all schemas.
	 * <p>
	 * Applies to Rule(s): none – default behavior
	 */
	public static final String PARAM_PROCESS_ALL_SCHEMAS = "processAllSchemas";

	private static boolean initialised = false;
	private static boolean invalidConfiguration = false;

	private static String connectionString = null;
	private static String user = null;

	private static Model inputModel = null;
	private static EADocument eaModel = null;

	private static boolean processAllSchemas = false;
	private static boolean deleteEAModelProfiles = false;
	private static SortedSet<String> profilesToTransfer = null;

	private Options options = null;
	private ShapeChangeResult result = null;

	@Override
	public void initialise(PackageInfo pi, Model m, Options o,
			ShapeChangeResult r, boolean diagOnly)
			throws ShapeChangeAbortException {

		options = o;
		result = r;

		if (!initialised) {

			initialised = true;

			inputModel = m;

			processAllSchemas = options.parameterAsBoolean(
					ProfileTransferEA.class.getName(),
					PARAM_PROCESS_ALL_SCHEMAS, false);
			deleteEAModelProfiles = options.parameterAsBoolean(
					ProfileTransferEA.class.getName(),
					PARAM_DELETE_EA_MODEL_PROFILES, false);

			if (options.hasParameter(ProfileTransferEA.class.getName(),
					PARAM_PROFILES_TO_TRANSFER)) {
				profilesToTransfer = new TreeSet<String>(
						options.parameterAsStringList(
								ProfileTransferEA.class.getName(),
								PARAM_PROFILES_TO_TRANSFER, null, true, true));
			}

			// check that input model is an EA repository
			String inputModelType = options.parameter("inputModelType");

			if (inputModelType == null
					|| !inputModelType.equalsIgnoreCase("EA7")) {

				result.addFatalError(this, 10);
				invalidConfiguration = true;

			} else {

				String mdl = options.parameter("inputFile");

				String repoFileNameOrConnectionString = options
						.parameter("repositoryFileNameOrConnectionString");

				String username = options.parameter("username");
				String password = options.parameter("password");

				user = username == null ? "" : username;
				String pwd = password == null ? "" : password;

				boolean transferToCopyOfEAP = options.parameterAsBoolean(
						ProfileTransferEA.class.getName(),
						ProfileTransferEA.PARAM_TRANSFER_TO_EAP_COPY, false);

				/*
				 * we accept path to EAP file or repository connection string
				 * via both inputFile and repositoryFileNameOrConnectionString
				 * parameters
				 */
				String repoConnectionInfo = null;

				if (repoFileNameOrConnectionString != null
						&& repoFileNameOrConnectionString.length() > 0) {
					repoConnectionInfo = repoFileNameOrConnectionString;
				} else if (mdl != null && mdl.length() > 0) {
					repoConnectionInfo = mdl;
				} else {
					result.addFatalError(this, 11);
					invalidConfiguration = true;
				}

				if (!invalidConfiguration) {

					/*
					 * Determine if we are dealing with a file or server based
					 * repository
					 */
					if (repoConnectionInfo.contains("DBType=")
							|| repoConnectionInfo.contains("Connect=Cloud")) {

						/* We are dealing with a server based repository. */

						if (transferToCopyOfEAP) {
							result.addFatalError(this, 13);
							invalidConfiguration = true;

						} else {

							connectionString = repoConnectionInfo;
						}

					} else {

						/* We have an EAP file. Ensure that it exists */

						File repfile = new File(repoConnectionInfo);

						boolean ex = true;

						if (!repfile.exists()) {

							ex = false;
							if (!repoConnectionInfo.toLowerCase()
									.endsWith(".eap")) {
								repoConnectionInfo += ".eap";
								repfile = new File(repoConnectionInfo);
								ex = repfile.exists();
							}
						}

						if (!ex) {

							result.addFatalError(this, 14, repoConnectionInfo);
							invalidConfiguration = true;

						} else if (transferToCopyOfEAP) {

							/*
							 * EAP file shall be copied. Check that the output
							 * directory exists and can be written to.
							 */
							String outputDirectory = options.parameter(
									ProfileTransferEA.class.getName(),
									"outputDirectory");

							if (outputDirectory == null)
								outputDirectory = options
										.parameter("outputDirectory");
							if (outputDirectory == null)
								outputDirectory = ".";

							File outputDirectoryFile = new File(
									outputDirectory);
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
								invalidConfiguration = true;

							} else {

								// copy EA project file to output directory
								File inputEAP = new File(repoConnectionInfo);
								File copyEAP = new File(outputDirectoryFile,
										inputEAP.getName());

								try {
									FileUtils.copyFile(inputEAP, copyEAP,
											false);
									connectionString = copyEAP
											.getAbsolutePath();
								} catch (Exception e) {
									result.addFatalError(this, 15,
											e.getMessage());
									invalidConfiguration = true;
								}
							}

						} else {

							connectionString = repfile.getAbsolutePath();
						}
					}

					if (!invalidConfiguration) {

						eaModel = new EADocument();

						try {

							if (user.length() == 0) {

								eaModel.initialise(result, options,
										connectionString);

							} else {

								eaModel.initialise(result, options,
										repoConnectionInfo, user, pwd);
							}

						} catch (ShapeChangeAbortException e) {

							if (user.length() == 0) {
								result.addFatalError(this, 16, connectionString,
										e.getMessage());
							} else {
								result.addFatalError(this, 17, connectionString,
										user, pwd, e.getMessage());
							}

							invalidConfiguration = true;
						}
					}
				}
			}
		}
	}

	@Override
	public void process(ClassInfo ci) {
		// ignored - see writeAll
	}

	@Override
	public void write() {
		// nothing to do - see writeAll
	}

	@Override
	public int getTargetID() {
		return TargetIdentification.PROFILE_TRANSFER_EA.getId();
	}

	@Override
	public void writeAll(ShapeChangeResult r) {

		if (invalidConfiguration) {
			return;
		}

		Repository eaRepo = eaModel.getEARepository();

		if (deleteEAModelProfiles) {

			/*
			 * NOTE: We get new wrapper objects (Element, Attribute,
			 * ConnectorEnd) to avoid timeout issues, which can occur when
			 * processing large models. This is experience from using the ArcGIS
			 * workspace target.
			 */
			SortedSet<PackageInfo> eaSchemas = eaModel.schemas(null);

			for (PackageInfo eaSchema : eaSchemas) {

				SortedSet<ClassInfo> eaClasses = eaModel.classes(eaSchema);

				for (ClassInfo ci : eaClasses) {

					ClassInfoEA eaClass = (ClassInfoEA) ci;
					Element eaClassElement = eaRepo
							.GetElementByID(eaClass.getEaElementId());
					EAModelUtil.deleteTaggedValue(eaClassElement, "profiles");

					for (PropertyInfo pi : ci.properties().values()) {

						PropertyInfoEA eaProperty = (PropertyInfoEA) pi;

						if (pi.isAttribute()) {

							Attribute eaAttribute = eaRepo.GetAttributeByID(
									eaProperty.getEAAttributeId());
							EAModelUtil.deleteTaggedValue(eaAttribute,
									"profiles");

						} else {

							AssociationInfoEA eaAssociation = (AssociationInfoEA) pi
									.association();

							Connector con = eaRepo.GetConnectorByID(
									eaAssociation.getEAConnectorId());

							/*
							 * Get the relevant connector end. NOTE: Since the
							 * EA API does not provide access to connector ends
							 * via an ID, we need to use the ID prefix
							 * established by the constructor of PropertyInfoEA
							 * to identify the relevant connector end.
							 */
							ConnectorEnd end;
							if (pi.id().startsWith("S")) {
								end = con.GetClientEnd();
							} else {
								end = con.GetSupplierEnd();
							}

							EAModelUtil.deleteTaggedValue(end, "profiles");
						}
					}
				}
			}
		}

		// transfer profiles
		SortedMap<String, SortedSet<ClassInfo>> inputModelClassesBySchemaName = new TreeMap<String, SortedSet<ClassInfo>>();

		for (PackageInfo pi : inputModel.schemas(null)) {
			SortedSet<ClassInfo> classes = new TreeSet<ClassInfo>();
			for (ClassInfo ci : inputModel.classes(pi)) {
				classes.add(ci);
			}
			inputModelClassesBySchemaName.put(pi.name(), classes);
		}

		Map<String, Map<String, ClassInfoEA>> eaModelClassesByNameBySchemaName = new HashMap<String, Map<String, ClassInfoEA>>();

		for (PackageInfo pi : eaModel.schemas(null)) {

			Map<String, ClassInfoEA> eaClassesByClassName = new HashMap<String, ClassInfoEA>();
			for (ClassInfo ci : eaModel.classes(pi)) {
				eaClassesByClassName.put(ci.name(), (ClassInfoEA) ci);
			}
			eaModelClassesByNameBySchemaName.put(pi.name(),
					eaClassesByClassName);
		}

		for (Entry<String, SortedSet<ClassInfo>> inputSchemaEntry : inputModelClassesBySchemaName
				.entrySet()) {

			String schemaName = inputSchemaEntry.getKey();

			if (!eaModelClassesByNameBySchemaName.containsKey(schemaName)) {

				/*
				 * No corresponding schema package in the ea model.
				 */

			} else {

				SortedSet<ClassInfo> inputSchemaCis = inputSchemaEntry
						.getValue();
				Map<String, ClassInfoEA> eaSchemaCisByClassName = eaModelClassesByNameBySchemaName
						.get(schemaName);

				for (ClassInfo inputCi : inputSchemaCis) {

					if (!processAllSchemas
							&& !inputModel.isInSelectedSchemas(inputCi)) {
						/*
						 * Not all schemas are processed, and the input class
						 * does not belong to a schema selected for processing.
						 */

					} else if (inputCi.profiles().isEmpty()
							|| (profilesToTransfer != null && inputCi.profiles()
									.getProfiles(profilesToTransfer)
									.isEmpty())) {
						/*
						 * No profiles to transfer
						 */

					} else if (eaSchemaCisByClassName
							.containsKey(inputCi.name())) {

						ClassInfoEA eaCi = eaSchemaCisByClassName
								.get(inputCi.name());

						/*
						 * Determine which profiles shall be transferred to the
						 * class.
						 */
						Profiles ptt;
						if (profilesToTransfer == null) {
							ptt = inputCi.profiles();
						} else {
							ptt = new Profiles(inputCi.profiles()
									.getProfiles(profilesToTransfer));
						}

						/*
						 * NOTE: We currently do not merge the profiles. The
						 * 'profiles' tagged value of the model element from the
						 * EA repository is overwritten.
						 */

						// transfer profiles to the class
						EATaggedValue newClassProfiles = new EATaggedValue(
								"profiles", ptt.toString());
						Element eaClassElement = eaRepo
								.GetElementByID(eaCi.getEaElementId());
						try {
							EAModelUtil.setTaggedValue(eaClassElement,
									newClassProfiles);
						} catch (EAException e) {
							MessageContext mc = result.addError(this, 18,
									eaCi.name(), e.getMessage());
							if (mc != null) {
								mc.addDetail(this, 1, eaCi.fullNameInSchema());
							}
						}

						for (PropertyInfo inputCiPi : inputCi.properties()
								.values()) {

							PropertyInfo eaCiPi = eaCi
									.property(inputCiPi.name());

							if (eaCiPi != null) {

								PropertyInfoEA eaProperty = (PropertyInfoEA) eaCiPi;

								/*
								 * Determine which profiles shall be transferred
								 * to the property.
								 */
								Profiles pttPi;
								if (profilesToTransfer == null) {
									pttPi = inputCiPi.profiles();
								} else {
									pttPi = new Profiles(inputCiPi.profiles()
											.getProfiles(profilesToTransfer));
								}

								/*
								 * NOTE: We currently do not merge the profiles.
								 * The 'profiles' tagged value of the model
								 * element from the EA repository is
								 * overwritten.
								 */

								// transfer profiles to the property
								EATaggedValue newPropertyProfiles = new EATaggedValue(
										"profiles", pttPi.toString());

								try {
									if (eaProperty.isAttribute()) {

										Attribute eaAttribute = eaRepo
												.GetAttributeByID(eaProperty
														.getEAAttributeId());

										EAModelUtil.setTaggedValue(eaAttribute,
												newPropertyProfiles);

									} else {

										AssociationInfoEA eaAssociation = (AssociationInfoEA) eaProperty
												.association();

										Connector con = eaRepo
												.GetConnectorByID(eaAssociation
														.getEAConnectorId());

										/*
										 * Get the relevant connector end. NOTE:
										 * Since the EA API does not provide
										 * access to connector ends via an ID,
										 * we need to use the ID prefix
										 * established by the constructor of
										 * PropertyInfoEA to identify the
										 * relevant connector end.
										 */
										ConnectorEnd end;
										if (eaProperty.id().startsWith("S")) {
											end = con.GetClientEnd();
										} else {
											end = con.GetSupplierEnd();
										}

										EAModelUtil.setTaggedValue(end,
												newPropertyProfiles);
									}

								} catch (EAException e) {
									MessageContext mc = result.addError(this,
											19, eaProperty.name(),
											eaProperty.name(), e.getMessage());
									if (mc != null) {
										mc.addDetail(this, 1,
												eaProperty.fullNameInSchema());
									}
								}
							}
						}

					} else {
						/*
						 * Ok. No corresponding class in schema from EA model.
						 */
					}
				}
			}
		}

		// finally, shut down the EA repository
		if (eaModel != null) {
			eaModel.shutdown();
		}
	}

	@Override
	public void reset() {

		initialised = false;
		invalidConfiguration = false;

		connectionString = null;
		user = null;

		inputModel = null;
		eaModel = null;
		processAllSchemas = false;
		deleteEAModelProfiles = false;
		profilesToTransfer = null;
	}

	@Override
	public String message(int mnr) {

		switch (mnr) {

		case 1:
			return "Context: $1$";
		case 2:
			return "Directory named '$1$' does not exist or is not accessible.";
		case 10:
			return "The input parameter 'inputModelType' was not set or does not equal (ignoring case) 'EA7'. This target can only be executed if the input model is an EA repository.";
		case 11:
			return "Neither the input parameter 'inputFile' nor the input parameter 'repositoryFileNameOrConnectionString' are set. This target requires one of these parameters in order to connect to the EA repository.";
		case 12:
			return "The target is configured to copy the EA project file that contains the input model to the output directory, before transferring the profile infos. However, the directory named '$1$' does not exist or is not accessible. The transfer will not be executed.";
		case 13:
			return "The target is configured to copy the EA project file that contains the input model to the output directory, before transferring the profile infos. However, the EA repository is a server based repository, not an EA project file. The transfer will not be executed.";
		case 14:
			return "Enterprise Architect repository file named '$1$' not found.";
		case 15:
			return "The target is configured to copy the EA project file that contains the input model to the output directory, before transferring the profile infos. However, an exception occurred when copying the file: $1$. The transfer will not be executed.";
		case 16:
			return "Enterprise Architect repository cannot be opened. File name or connection string is: '$1$', exception message is: '$2$'";
		case 17:
			return "Enterprise Architect repository cannot be opened. File name or connection string is: '$1$', username is: '$2$', password is: '$3$', exception message is: '$4$'";
		case 18:
			return "Could not transfer profiles to class '$1$'. Exception message is: $2$.";
		case 19:
			return "Could not transfer profiles to property '$1$' in class '$2$'. Exception message is: $3$.";

		default:
			return "(" + ProfileTransferEA.class.getName()
					+ ") Unknown message with number: " + mnr;
		}

	}
}
