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
 * (c) 2002-2018 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target.ArcGISWorkspace;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.sparx.Repository;

import de.interactive_instruments.ShapeChange.ConfigurationValidator;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessConfiguration;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Util.ea.EARepositoryUtil;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class ArcGISWorkspaceConfigurationValidator
		implements ConfigurationValidator, MessageSource {

	@Override
	public boolean isValid(ProcessConfiguration config, Options options,
			ShapeChangeResult result) {

		boolean isValid = true;

		/*
		 * ========== Validate the EA environment ==========
		 * 
		 * NOTE Requires opening an actual EA repository.
		 */

		// Check if we can use the output directory; create it if it
		// does not exist

		// get output location
		String outputDirectory = config.parameterAsString("outputDirectory",
				".", false, true);

		String outputFilename = config.parameterAsString(
				ArcGISWorkspaceConstants.PARAM_OUTPUT_FILENAME,
				"ArcGISWorkspaceConfigurationValidator", false, true);

		outputFilename = outputFilename.replace("/", "_").replace(" ", "_")
				+ ".eap";

		File outputDirectoryFile = new File(outputDirectory);
		boolean exi = outputDirectoryFile.exists();
		if (!exi) {
			try {
				FileUtils.forceMkdir(outputDirectoryFile);
			} catch (IOException e) {
				isValid = false;
				result.addError(this, 5, e.getMessage());
				e.printStackTrace(System.err);
			}
			exi = outputDirectoryFile.exists();
		}
		boolean dir = outputDirectoryFile.isDirectory();
		boolean wrt = outputDirectoryFile.canWrite();
		boolean rea = outputDirectoryFile.canRead();
		if (!exi || !dir || !wrt || !rea) {
			isValid = false;
			result.addError(this, 1, outputDirectory);
		}

		File outputFile = new File(outputDirectoryFile, outputFilename);

		/*
		 * Check if file already exists - if so, attempt to delete it. Note that
		 * this file will be deleted after the validation.
		 */
		exi = outputFile.exists();
		if (exi) {
			try {
				FileUtils.forceDelete(outputFile);
			} catch (IOException e) {
				// ignore for configuration validation
			}
		}

		if (isValid) {
			// read workspace template

			String workspaceTemplateFilePath = config.parameterAsString(
					ArcGISWorkspaceConstants.PARAM_WORKSPACE_TEMPLATE, null,
					false, true);

			// if no path is provided, use the directory of the default template
			if (workspaceTemplateFilePath == null) {
				workspaceTemplateFilePath = ArcGISWorkspaceConstants.WORKSPACE_TEMPLATE_URL;
			}

			// copy template file either from remote or local URI
			if (workspaceTemplateFilePath.toLowerCase().startsWith("http")) {

				try {
					URL templateUrl = new URL(workspaceTemplateFilePath);
					FileUtils.copyURLToFile(templateUrl, outputFile);
				} catch (MalformedURLException e1) {
					isValid = false;
					result.addError(this, 6, workspaceTemplateFilePath,
							e1.getMessage());
				} catch (IOException e2) {
					isValid = false;
					result.addError(this, 8, e2.getMessage());
				}

			} else {

				File workspacetemplate = new File(workspaceTemplateFilePath);

				if (workspacetemplate.exists()) {
					try {
						FileUtils.copyFile(workspacetemplate, outputFile);
					} catch (IOException e) {
						isValid = false;
						result.addError(this, 8, e.getMessage());
					}
				} else {
					isValid = false;
					result.addError(this, 7,
							workspacetemplate.getAbsolutePath());
				}
			}
		}

		if (isValid) {

			// connect to EA repository in outputFile
			String absolutePathOfOutputEAPFile = outputFile.getAbsolutePath();

			Repository rep = new Repository();

			if (!rep.OpenFile(absolutePathOfOutputEAPFile)) {

				String errormsg = rep.GetLastError();
				result.addError(null, 30, errormsg, outputFilename);
				rep = null;
				isValid = false;

			} else {

				/*
				 * Use the repository for validation of the EA environment
				 */

				if (!rep.IsTechnologyEnabled("ArcGIS")) {
					isValid = false;
					result.addError(this, 100);
				}

				// Close the repository
				EARepositoryUtil.closeRepository(rep);
				rep = null;
			}
		}

		/*
		 * Delete the repository file that was used for validation of the EA
		 * environment
		 */
		if (outputFile.exists()) {
			outputFile.delete();
		}

		return isValid;
	}

	@Override
	public String message(int mnr) {

		switch (mnr) {

		case 1:
			return "Directory named '$1$' does not exist or is not accessible.";
		case 5:
			return "Could not create output directory. Exception message: '$1$'.";
		case 6:
			return "URL '$1$' provided for configuration parameter "
					+ ArcGISWorkspaceConstants.PARAM_WORKSPACE_TEMPLATE
					+ " is malformed. Exception message is: '$2$'.";
		case 7:
			return "EAP with ArcGIS workspace template at '$1$' does not exist or cannot be read. Check the value of the configuration parameter '"
					+ ArcGISWorkspaceConstants.PARAM_WORKSPACE_TEMPLATE
					+ "' and ensure that: a) it contains the path to the template file and b) the file can be read by ShapeChange.";
		case 8:
			return "Exception encountered when copying ArcGIS workspace template EAP file to output destination. Message is: $1$.";

		// 100-199 MDG related messages
		case 100:
			return "The MDG Technology 'ArcGIS' is not enabled in your EA environment. Writing an ArcGIS workspace requires that this technology is enabled. Enable this technology in your EA environment before executing ShapeChange.";

		default:
			return "(" + this.getClass().getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
