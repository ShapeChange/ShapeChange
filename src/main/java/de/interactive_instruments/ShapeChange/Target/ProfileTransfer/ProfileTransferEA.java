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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.sparx.Attribute;
import org.sparx.Connector;
import org.sparx.ConnectorEnd;
import org.sparx.Element;
import org.sparx.Package;
import org.sparx.Repository;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.MalformedProfileIdentifierException;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Profile.Profiles;
import de.interactive_instruments.ShapeChange.Target.SingleTarget;
import de.interactive_instruments.ShapeChange.Util.ea.EAAttributeUtil;
import de.interactive_instruments.ShapeChange.Util.ea.EAConnectorEndUtil;
import de.interactive_instruments.ShapeChange.Util.ea.EAElementUtil;
import de.interactive_instruments.ShapeChange.Util.ea.EAException;

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
	 * Explanation: By default, profiles that are transferred are merged with
	 * existing profiles of a model element in the EA repository. Merging means
	 * that only the profiles with names that match one of the names of profiles
	 * to be transferred will be overwritten - all other profiles of the model
	 * element will be kept.
	 * <p>
	 * If this parameter is set to <code>true</code>, any previously existing
	 * profiles of a model element from the EA repository that is eligible for
	 * profile transfer (see {@value #PARAM_PROCESS_ALL_SCHEMAS}) are deleted
	 * before profiles are transferred to it (even if no corresponding model
	 * elements could be found in the input model).
	 * <p>
	 * Applies to Rule(s): none – default behavior
	 */
	public static final String PARAM_DELETE_EXISTING_PROFILES = "deleteExistingProfiles";

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
	 * Explanation: By default, profiles are transferred only for non-prohibited
	 * classes (and their properties) from schemas that are selected for
	 * processing. If this parameter is set to <code>true</code>, profiles are
	 * transferred for non-prohibited classes (and their properties) from all
	 * schemas. For details on non-prohibited classes, see the explanation of
	 * input parameter
	 * {@value de.interactive_instruments.ShapeChange.Options#PARAM_PROHIBIT_LOADING_CLASSES_WITH_STATUS_TV}
	 * .
	 * <p>
	 * Applies to Rule(s): none – default behavior
	 */
	public static final String PARAM_PROCESS_ALL_SCHEMAS = "processAllSchemas";

	/**
	 * Alias: none
	 * <p>
	 * Required / Optional: optional
	 * <p>
	 * Type: String
	 * <p>
	 * Default Value: defaults to the value of the input parameters 'inputFile'
	 * and 'repositoryFileNameOrConnectionString' (the former has higher
	 * priority than the latter)
	 * <p>
	 * Explanation: If this parameter is set in the target configuration, it
	 * provides the connection info to the EA repository in which profiles shall
	 * be transferred. If the parameter is not set, the target will transfer the
	 * profiles into the model that is defined in the input configuration of
	 * ShapeChange. For further details on this parameter, see the explanation
	 * for the input parameter with this name (but keep in mind that the target
	 * will assume / requires that the connection to an EA repository is given).
	 * <p>
	 * Applies to Rule(s): none – default behavior
	 */
	public static final String PARAM_REPO_CONNECTION_STRING = "repositoryFileNameOrConnectionString";

	/**
	 * Alias: none
	 * <p>
	 * Required / Optional: optional
	 * <p>
	 * Type: String
	 * <p>
	 * Default Value: none
	 * <p>
	 * Explanation: If the target parameter
	 * 'repositoryFileNameOrConnectionString' is set, and the connection
	 * requires a username and password, set the username with this target
	 * parameter. NOTE: If the parameter 'repositoryFileNameOrConnectionString'
	 * is not set in the target configuration, the target will fully rely on the
	 * information provided in the input configuration. In other words, then
	 * there is no need to set the parameter 'username' in the target
	 * configuration.
	 * <p>
	 * Applies to Rule(s): none – default behavior
	 */
	public static final String PARAM_USER = "username";

	/**
	 * Alias: none
	 * <p>
	 * Required / Optional: optional
	 * <p>
	 * Type: String
	 * <p>
	 * Default Value: none
	 * <p>
	 * Explanation: If the target parameter
	 * 'repositoryFileNameOrConnectionString' is set, and the connection
	 * requires a username and password, set the password with this target
	 * parameter. NOTE: If the parameter 'repositoryFileNameOrConnectionString'
	 * is not set in the target configuration, the target will fully rely on the
	 * information provided in the input configuration. In other words, then
	 * there is no need to set the parameter 'password' in the target
	 * configuration.
	 * <p>
	 * Applies to Rule(s): none – default behavior
	 */
	public static final String PARAM_PWD = "password";

	private static boolean initialised = false;
	private static boolean invalidConfiguration = false;

	private static String connectionString = null;

	private static Model inputModel = null;
	/**
	 * key: schema name
	 * <p>
	 * value: map with class as value and the key being the name of that class
	 */
	private static Map<String, Map<String, ClassInfo>> inputModelClassesByClassNameBySchemaName = null;
	private static Repository eaRepo = null;

	private static boolean processAllSchemas = false;
	private static boolean deleteExistingProfiles = false;
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
			deleteExistingProfiles = options.parameterAsBoolean(
					ProfileTransferEA.class.getName(),
					PARAM_DELETE_EXISTING_PROFILES, false);

			if (options.hasParameter(ProfileTransferEA.class.getName(),
					PARAM_PROFILES_TO_TRANSFER)) {
				profilesToTransfer = new TreeSet<String>(
						options.parameterAsStringList(
								ProfileTransferEA.class.getName(),
								PARAM_PROFILES_TO_TRANSFER, null, true, true));
			}

			/*
			 * Retrieve EA repo connection info either directly from the target,
			 * if PARAM_REPO_CONNECTION_STRING is set, or from the input
			 * configuration.
			 */
			String repoConnectionInfo = null;
			String username = null;
			String password = null;

			if (options.hasParameter(ProfileTransferEA.class.getName(),
					PARAM_REPO_CONNECTION_STRING)) {

				result.addInfo(this, 29);

				repoConnectionInfo = options.parameterAsString(
						ProfileTransferEA.class.getName(),
						PARAM_REPO_CONNECTION_STRING, null, false, true);

				if (repoConnectionInfo == null) {

					result.addError(this, 31);
					invalidConfiguration = true;

				} else {

					username = options.parameter(
							ProfileTransferEA.class.getName(), PARAM_USER);
					password = options.parameter(
							ProfileTransferEA.class.getName(), PARAM_PWD);
				}

			} else {

				result.addInfo(this, 30);

				// check that input model is an EA repository
				String inputModelType = options.parameter("inputModelType");

				if (inputModelType == null
						|| !inputModelType.equalsIgnoreCase("EA7")) {

					result.addError(this, 10);
					invalidConfiguration = true;

				} else {

					String mdl = options.parameter("inputFile");

					String repoFileNameOrConnectionString = options
							.parameter(PARAM_REPO_CONNECTION_STRING);

					username = options.parameter(PARAM_USER);
					password = options.parameter(PARAM_PWD);

					if (repoFileNameOrConnectionString != null
							&& repoFileNameOrConnectionString.length() > 0) {
						repoConnectionInfo = repoFileNameOrConnectionString;
					} else if (mdl != null && mdl.length() > 0) {
						repoConnectionInfo = mdl;
					} else {
						result.addError(this, 11);
						invalidConfiguration = true;
					}
				}
			}

			if (!invalidConfiguration) {

				username = username == null ? "" : username;
				password = password == null ? "" : password;

				boolean transferToCopyOfEAP = options.parameterAsBoolean(
						ProfileTransferEA.class.getName(),
						ProfileTransferEA.PARAM_TRANSFER_TO_EAP_COPY, false);

				/*
				 * Determine if we are dealing with a file or server based
				 * repository
				 */
				if (repoConnectionInfo.contains("DBType=")
						|| repoConnectionInfo.contains("Connect=Cloud")) {

					/* We are dealing with a server based repository. */

					if (transferToCopyOfEAP) {
						result.addError(this, 13);
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

						result.addError(this, 14, repoConnectionInfo);
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

							result.addError(this, 12, outputDirectory);
							invalidConfiguration = true;

						} else {

							// copy EA project file to output directory
							File inputEAP = new File(repoConnectionInfo);
							File copyEAP = new File(outputDirectoryFile,
									inputEAP.getName());

							try {
								FileUtils.copyFile(inputEAP, copyEAP, false);
								connectionString = copyEAP.getAbsolutePath();
							} catch (Exception e) {
								result.addError(this, 15, e.getMessage());
								invalidConfiguration = true;
							}
						}

					} else {

						connectionString = repfile.getAbsolutePath();
					}
				}

				if (!invalidConfiguration) {

					eaRepo = new Repository();

					if (username.length() == 0) {

						if (!eaRepo.OpenFile(connectionString)) {
							String errormsg = eaRepo.GetLastError();
							result.addError(this, 16, connectionString,
									errormsg);
							invalidConfiguration = true;
						}

					} else {

						if (!eaRepo.OpenFile2(connectionString, username,
								password)) {
							String errormsg = eaRepo.GetLastError();
							result.addError(this, 17, connectionString,
									username, password, errormsg);
							invalidConfiguration = true;
						}
					}
				}

				if (!invalidConfiguration) {

					// identify schemas and their classes in input model
					inputModelClassesByClassNameBySchemaName = new HashMap<String, Map<String, ClassInfo>>();

					for (PackageInfo inputModelPkg : inputModel.schemas(null)) {
						Map<String, ClassInfo> classesByName = new HashMap<String, ClassInfo>();
						for (ClassInfo ci : inputModel.classes(inputModelPkg)) {
							classesByName.put(ci.name(), ci);
						}
						inputModelClassesByClassNameBySchemaName
								.put(inputModelPkg.name(), classesByName);
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
	public String getTargetName() {
		return "Profile Transfer EA";
	}

	@Override
	public void writeAll(ShapeChangeResult r) {

		if (invalidConfiguration) {
			return;
		}

		result = r;
		options = r.options();

		// Walk through models in EA repository
		org.sparx.Collection<org.sparx.Package> eaModelPackages = eaRepo
				.GetModels();
		for (org.sparx.Package eaModelPkg : eaModelPackages) {

			processProfiles(eaModelPkg, null, null);
		}

		// shut down the EA repository
		if (eaRepo != null) {
			eaRepo.CloseFile();
			eaRepo.Exit();
			eaRepo = null;
		}
	}

	/**
	 * @param eaModelPkg
	 * @param parentSchemaName
	 *            name of the schema to which the parent of this package
	 *            belongs; can be <code>null</code>
	 * @param parentSchemaTargetNamespace
	 *            target namespace of the schema to which the parent of this
	 *            schema belongs; can be <code>null</code>
	 */
	private void processProfiles(Package eaModelPkg, String parentSchemaName,
			String parentSchemaTargetNamespace) {

		String pkgName = eaModelPkg.GetName();
		Element pkgElement = eaModelPkg.GetElement();

		String targetNamespace = identifyTargetNamespace(pkgName, pkgElement);

		/*
		 * Identify applicable schema name and target namespace. NOTE: Both may
		 * be null if the package is not (in) a schema.
		 */
		String schemaName;
		String schemaTargetNamespace;

		if (targetNamespace != null
				&& (parentSchemaTargetNamespace == null || !targetNamespace
						.equalsIgnoreCase(parentSchemaTargetNamespace))) {

			// this is a different schema
			schemaName = pkgName;
			schemaTargetNamespace = targetNamespace;

		} else {

			// same schema as parent package, if that belongs to a schema
			schemaName = parentSchemaName;
			schemaTargetNamespace = parentSchemaTargetNamespace;
		}

		if (schemaTargetNamespace != null && (processAllSchemas
				|| !skipSchema(schemaName, schemaTargetNamespace))) {

			boolean hasCorrespondingInputSchema = inputModelClassesByClassNameBySchemaName
					.containsKey(schemaName);

			if (!hasCorrespondingInputSchema) {
				result.addWarning(this, 20, schemaName, schemaTargetNamespace);
			}

			for (org.sparx.Element classElmt : eaModelPkg.GetElements()) {

				String type = classElmt.GetType();
				int classElementId = classElmt.GetElementID();

				if (!type.equals("DataType") && !type.equals("Class")
						&& !type.equals("Interface")
						&& !type.equals("Enumeration")) {
					continue;
				}

				String statusTaggedValue = EAElementUtil.taggedValue(classElmt,
						"status");

				/*
				 * prevent loading of classes that have tagged value 'status'
				 * with prohibited value
				 */
				if (statusTaggedValue != null
						&& options.prohibitedStatusValuesWhenLoadingClasses()
								.contains(statusTaggedValue)) {
					continue;
				}

				/*
				 * Delete the profiles tagged value of the class and its
				 * properties if so configured. NOTE: This is independent of
				 * whether or not a corresponding class can be found in the
				 * input model.
				 */
				if (deleteExistingProfiles) {

					EAElementUtil.deleteTaggedValue(classElmt, "profiles");

					for (Attribute att : classElmt.GetAttributes()) {
						EAAttributeUtil.deleteTaggedValue(att, "profiles");
					}

					for (Connector con : classElmt.GetConnectors()) {

						String conType = con.GetType();
						if (!conType.equalsIgnoreCase("Association")
								&& !conType.equalsIgnoreCase("Aggregation")) {
							continue;
						}

						/*
						 * Delete profiles tagged value only for the roles that
						 * belong to the current class.
						 */
						if (con.GetClientID() == classElementId
								&& isNavigableEnd(con, con.GetSupplierEnd())) {
							EAConnectorEndUtil.deleteTaggedValue(
									con.GetSupplierEnd(), "profiles");
						}

						if (con.GetSupplierID() == classElementId
								&& isNavigableEnd(con, con.GetClientEnd())) {
							EAConnectorEndUtil.deleteTaggedValue(
									con.GetClientEnd(), "profiles");
						}
					}
				}

				if (hasCorrespondingInputSchema) {

					Map<String, ClassInfo> inputModelSchemaCisByCiName = inputModelClassesByClassNameBySchemaName
							.get(schemaName);

					// transfer class profiles
					String className = classElmt.GetName().trim();
					result.addInfo(this, 3, className);

					// ensure that input model contains this class as well
					if (!inputModelSchemaCisByCiName.containsKey(className)) {

						result.addWarning(this, 21, className, schemaName,
								schemaTargetNamespace);

					} else {

						Profiles newProfilesCi = new Profiles();
						if (!deleteExistingProfiles) {
							String profilesTV = EAElementUtil
									.taggedValue(classElmt, "profiles");
							if (profilesTV != null
									&& profilesTV.trim().length() > 0) {
								try {
									newProfilesCi = Profiles
											.parse(profilesTV.trim(), false);
								} catch (MalformedProfileIdentifierException e) {
									result.addError(this, 22, className,
											schemaName, e.getMessage());
								}
							}
						}

						ClassInfo inputModelCi = inputModelSchemaCisByCiName
								.get(className);

						// merge profiles
						newProfilesCi.put(computeProfilesToTransfer(
								inputModelCi.profiles()));

						try {
							EAElementUtil.setTaggedValue(classElmt, "profiles",
									newProfilesCi.toString());
						} catch (EAException e) {
							result.addError(this, 23, className, schemaName,
									e.getMessage());
						}

						// transfer attribute profiles
						for (Attribute att : classElmt.GetAttributes()) {

							String attName = att.GetName();

							PropertyInfo inputModelPi = inputModelCi
									.ownedProperty(attName);

							if (inputModelPi == null) {
								result.addWarning(this, 24, className,
										schemaName, schemaTargetNamespace,
										attName);
							} else {

								Profiles newProfilesPi = new Profiles();
								if (!deleteExistingProfiles) {
									String profilesTV = EAAttributeUtil
											.taggedValue(att, "profiles");
									if (profilesTV != null
											&& profilesTV.trim().length() > 0) {
										try {
											newProfilesPi = Profiles.parse(
													profilesTV.trim(), false);
										} catch (MalformedProfileIdentifierException e) {
											result.addError(this, 25, className,
													attName, schemaName,
													e.getMessage());
										}
									}
								}

								// merge profiles
								newProfilesPi.put(computeProfilesToTransfer(
										inputModelPi.profiles()));

								try {
									EAAttributeUtil.setTaggedValue(att,
											"profiles",
											newProfilesPi.toString());
								} catch (EAException e) {
									result.addError(this, 26, className,
											attName, schemaName,
											e.getMessage());
								}
							}
						}

						// transfer association role profiles
						for (Connector con : classElmt.GetConnectors()) {

							String conType = con.GetType();
							if (!conType.equalsIgnoreCase("Association")
									&& !conType
											.equalsIgnoreCase("Aggregation")) {
								continue;
							}

							if (con.GetClientID() == classElementId
									&& isNavigableEnd(con,
											con.GetSupplierEnd())) {

								transferProfileForAssociationRole(
										con.GetSupplierEnd(), inputModelCi,
										schemaName, schemaTargetNamespace);
							}

							if (con.GetSupplierID() == classElementId
									&& isNavigableEnd(con,
											con.GetClientEnd())) {

								transferProfileForAssociationRole(
										con.GetClientEnd(), inputModelCi,
										schemaName, schemaTargetNamespace);
							}
						}
					}
				}
			}
		}

		// drill down into sub packages
		org.sparx.Collection<org.sparx.Package> subPackages = eaModelPkg
				.GetPackages();
		for (org.sparx.Package subPkg : subPackages) {

			processProfiles(subPkg, schemaName, schemaTargetNamespace);
		}
	}

	private void transferProfileForAssociationRole(ConnectorEnd end,
			ClassInfo inputModelCi, String schemaName,
			String schemaTargetNamespace) {

		String roleName = end.GetRole();

		PropertyInfo inputModelPi = inputModelCi.ownedProperty(roleName);

		if (inputModelPi == null) {
			result.addWarning(this, 24, inputModelCi.name(), schemaName,
					schemaTargetNamespace, roleName);
		} else {

			Profiles newProfilesPi = new Profiles();
			if (!deleteExistingProfiles) {
				String profilesTV = EAConnectorEndUtil.taggedValue(end,
						"profiles");
				if (profilesTV != null && profilesTV.trim().length() > 0) {
					try {
						newProfilesPi = Profiles.parse(profilesTV.trim(),
								false);
					} catch (MalformedProfileIdentifierException e) {
						result.addError(this, 27, inputModelCi.name(), roleName,
								schemaName, e.getMessage());
					}
				}
			}

			// merge profiles
			newProfilesPi
					.put(computeProfilesToTransfer(inputModelPi.profiles()));

			try {
				EAConnectorEndUtil.setTaggedValue(end, "profiles",
						newProfilesPi.toString());
			} catch (EAException e) {
				result.addError(this, 28, inputModelCi.name(), roleName,
						schemaName, e.getMessage());
			}
		}
	}

	private boolean isNavigableEnd(Connector eaConnector,
			ConnectorEnd eaConnectorEnd) {

		// First get navigability from role
		boolean nav = eaConnectorEnd.GetIsNavigable();

		// If not explicitly set, also accept unspecified navigability,
		// if present in both directions.
		if (!nav) {
			int navigability = 0;
			String dirText = eaConnector.GetDirection();
			if (dirText.equals("Source -> Destination")) {
				navigability = 1;
			} else if (dirText.equals("Destination -> Source")) {
				navigability = -1;
			}
			nav = navigability == 0;
		}

		if (nav) {

			// AssociationEnds with unknown stereotypes are skipped
			String sn = eaConnectorEnd.GetStereotype();
			if (sn != null)
				sn = options.normalizeStereotype(sn);
			if (sn != null && sn.length() > 0) {
				boolean found = false;
				for (String st : Options.propertyStereotypes) {
					if (sn.equals(st)) {
						found = true;
						break;
					}
				}
				if (!found) {
					nav = false;
				}
			}

			// navigable only with a name
			String roleName = eaConnectorEnd.GetRole();
			if (roleName == null || roleName.length() == 0) {
				nav = false;
			}
		}

		return nav;
	}

	private Profiles computeProfilesToTransfer(
			Profiles profilesOfModelElement) {

		Profiles ptt;

		if (profilesToTransfer == null) {
			ptt = profilesOfModelElement;
		} else {
			ptt = new Profiles(
					profilesOfModelElement.getProfiles(profilesToTransfer));
		}

		return ptt;
	}

	/*
	 * Only process schemas in a namespace and name that matches a user-selected
	 * pattern
	 */
	private boolean skipSchema(String schemaName, String targetNamespace) {

		// only process schemas with a given name
		String schemaFilter;
		schemaFilter = options.parameter("appSchemaName");

		if (schemaFilter != null && schemaFilter.length() > 0
				&& !schemaFilter.equals(schemaName))
			return true;

		// only process schemas with a name that matches a user-selected pattern
		String appSchemaNameRegex;
		appSchemaNameRegex = options.parameter("appSchemaNameRegex");

		if (appSchemaNameRegex != null && appSchemaNameRegex != null
				&& appSchemaNameRegex.length() > 0
				&& !schemaName.matches(appSchemaNameRegex))
			return true;

		// only process schemas in a namespace that matches a user-selected
		// pattern
		String appSchemaNamespaceRegex;
		appSchemaNamespaceRegex = options.parameter("appSchemaNamespaceRegex");

		if (appSchemaNamespaceRegex != null
				&& appSchemaNamespaceRegex.length() > 0
				&& !targetNamespace.matches(appSchemaNamespaceRegex))
			return true;

		return false;
	}

	/**
	 * @param pkgName
	 *            can be <code>null</code>
	 * @param pkgElmt
	 *            can be <code>null</code>
	 * @return target namespace of the package (from tagged value
	 *         'targetNamespace' or 'xmlNamespace', or from explicit package
	 *         configuration); can be <code>null</code> if no target namespace
	 *         was found
	 */
	private String identifyTargetNamespace(String pkgName, Element pkgElmt) {

		String targetNamespace = null;

		if (pkgElmt != null) {

			List<String> tagNames = new ArrayList<String>();
			tagNames.add("targetNamespace");
			tagNames.add("xmlNamespace");

			org.sparx.Collection<org.sparx.TaggedValue> tvs = pkgElmt
					.GetTaggedValues();

			if (tvs != null) {

				for (org.sparx.TaggedValue tv : tvs) {

					String tvName = tv.GetName();

					if ("targetNamespace".equals(tvName)) {
						targetNamespace = tv.GetValue();
						break;
					} else if ("xmlNamespace".equals(tvName)) {
						targetNamespace = tv.GetValue();
						/*
						 * do not break here, since we look at the remaining
						 * tagged values to see if one is a 'targetNamespace',
						 * which would be preferred
						 */
					}
				}
			}
		}

		if (targetNamespace == null) {
			targetNamespace = options.nsOfPackage(pkgName);
		}

		return targetNamespace;
	}

	@Override
	public void reset() {

		initialised = false;
		invalidConfiguration = false;

		connectionString = null;

		inputModel = null;
		inputModelClassesByClassNameBySchemaName = null;
		eaRepo = null;

		processAllSchemas = false;
		deleteExistingProfiles = false;
		profilesToTransfer = null;
	}

	@Override
	public String message(int mnr) {

		switch (mnr) {

		case 1:
			return "Context: $1$";
		case 2:
			return "Directory named '$1$' does not exist or is not accessible.";
		case 3:
			return "Transferring profiles for class '$1$'.";
		case 10:
			return "The input parameter 'inputModelType' was not set or does not equal (ignoring case) 'EA7'. This target can only be executed if the model to transfer profiles to is an EA repository.";
		case 11:
			return "Neither the input parameter 'inputFile' nor the input parameter 'repositoryFileNameOrConnectionString' are set. This target requires one of these parameters in order to connect to the EA repository.";
		case 12:
			return "The target is configured to copy the EA project file to the output directory, before transferring the profile infos. However, the directory named '$1$' does not exist or is not accessible. The transfer will not be executed.";
		case 13:
			return "The target is configured to copy the EA project file to the output directory, before transferring the profile infos. However, the EA repository is a server based repository, not an EA project file. The transfer will not be executed.";
		case 14:
			return "Enterprise Architect repository file named '$1$' not found.";
		case 15:
			return "The target is configured to copy the EA project file to the output directory, before transferring the profile infos. However, an exception occurred when copying the file: $1$. The transfer will not be executed.";
		case 16:
			return "Enterprise Architect repository cannot be opened. File name or connection string is: '$1$', exception message is: '$2$'";
		case 17:
			return "Enterprise Architect repository cannot be opened. File name or connection string is: '$1$', username is: '$2$', password is: '$3$', exception message is: '$4$'";
		case 18:
			return "Could not transfer profiles to class '$1$'. Exception message is: $2$.";
		case 19:
			return "Could not transfer profiles to property '$1$' in class '$2$'. Exception message is: $3$.";
		case 20:
			return "??The target EA repository contains the schema '$1$' with namespace '$2$' for which profiles shall be transferred. However, the input model does not contain a schema with that name. Consequently, no profiles can be transferred for this schema.";
		case 21:
			return "The target EA repository contains class '$1$' (in schema '$2$' with namespace '$3$') for which profiles shall be transferred. However, the according schema in the input model does not contain a class with that name. Consequently, no profiles can be transferred for the class.";
		case 22:
			return "Profiles tagged value of class '$1$' in schema '$2$' of the target EA repository is malformed: $3$. Existing profiles will be overwritten by profiles from corresponding input model element.";
		case 23:
			return "Unexpected exception occurred while transferring the profiles of class '$1$' in schema '$2$' of the target EA repository: $3$.";
		case 24:
			return "The target EA repository contains class '$1$' (in schema '$2$' with namespace '$3$') with property '$4$' for which profiles shall be transferred. However, the according class in the input model does not contain a property with that name. Consequently, no profiles can be transferred for the property.";
		case 25:
			return "Profiles tagged value of attribute '$1$.$2$' in schema '$3$' of the target EA repository is malformed: $4$. Existing profiles will be overwritten by profiles from corresponding input model element.";
		case 26:
			return "Unexpected exception occurred while transferring the profiles of attribute '$1$.$2$' in schema '$3$' of the target EA repository: $4$.";
		case 27:
			return "Profiles tagged value of association role '$1$.$2$' in schema '$3$' of the target EA repository is malformed: $4$. Existing profiles will be overwritten by profiles from corresponding input model element.";
		case 28:
			return "Unexpected exception occurred while transferring the profiles of association role '$1$.$2$' in schema '$3$' of the target EA repository: $4$.";
		case 29:
			return "Using EA repository connection info provided by target configuration.";
		case 30:
			return "Using EA repository connection info provided by input configuration.";
		case 31:
			return "Parameter '" + PARAM_REPO_CONNECTION_STRING
					+ "' is set in the configuration of this target, but it does not contain a valid value. Provide such a value or remove the target parameter in order for the target to look up the EA repository connection info in the input configuration.";

		default:
			return "(" + ProfileTransferEA.class.getName()
					+ ") Unknown message with number: " + mnr;
		}

	}
}
