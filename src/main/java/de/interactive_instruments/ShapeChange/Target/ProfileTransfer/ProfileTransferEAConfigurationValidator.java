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

import de.interactive_instruments.ShapeChange.ConfigurationValidator;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessConfiguration;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class ProfileTransferEAConfigurationValidator
		implements ConfigurationValidator, MessageSource {

	@Override
	public boolean isValid(ProcessConfiguration config, Options options,
			ShapeChangeResult result) {

		boolean isValid = true;

		// check that input model is an EA repository
		String inputModelType = options.parameterAsString(null,
				"inputModelType", null, false, true);

		if (inputModelType == null || !inputModelType.equalsIgnoreCase("EA7")) {

			result.addFatalError(this, 10);
			isValid = false;

		} else {

			String mdl = options.parameter("inputFile");

			String repoFileNameOrConnectionString = options
					.parameter("repositoryFileNameOrConnectionString");

			boolean transferToCopyOfEAP = options.parameterAsBoolean(
					ProfileTransferEA.class.getName(),
					ProfileTransferEA.PARAM_TRANSFER_TO_EAP_COPY, false);

			/*
			 * we accept path to EAP file or repository connection string via
			 * both inputFile and repositoryFileNameOrConnectionString
			 * parameters
			 */
			String repoConnectionInfo = null;

			if (repoFileNameOrConnectionString != null
					&& repoFileNameOrConnectionString.length() > 0) {
				repoConnectionInfo = repoFileNameOrConnectionString;
			} else if (mdl != null && mdl.length() > 0) {
				repoConnectionInfo = mdl;
			} else {
				result.addError(this, 11);
				isValid = false;
			}

			if (isValid) {

				/*
				 * Determine if we are dealing with a file or server based
				 * repository
				 */
				if (repoConnectionInfo.contains("DBType=")
						|| repoConnectionInfo.contains("Connect=Cloud")) {

					/* We are dealing with a server based repository. */

					if (transferToCopyOfEAP) {
						result.addError(this, 13);
						isValid = false;
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
						isValid = false;

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
							isValid = false;

						}
					}
				}
			}
		}

		return isValid;
	}

	@Override
	public String message(int mnr) {

		switch (mnr) {

		case 1:
			return "";
		case 2:
			return "Output directory '$1$' does not exist or is not accessible.";
		case 3:
			return "";
		case 10:
			return "The input parameter 'inputModelType' was not set or does not equal (ignoring case) 'EA7'. This target can only be executed if the input model is an EA repository.";
		case 11:
			return "Neither the input parameter 'inputFile' nor the input parameter 'repositoryFileNameOrConnectionString' are set. This target requires one of these parameters in order to connect to the EA repository.";
		case 12:
			return "The target is configured to copy the EA project file that contains the input model to the output directory, before transferring the profile infos. However, the directory named '$1$' does not exist or is not accessible. The transfer would not be executed.";
		case 13:
			return "The target is configured to copy the EA project file that contains the input model to the output directory, before transferring the profile infos. However, the EA repository is a server based repository, not an EA project file. The transfer would not be executed.";
		case 14:
			return "Enterprise Architect repository file named '$1$' not found.";

		default:
			return "(" + ProfileTransferEA.class.getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
