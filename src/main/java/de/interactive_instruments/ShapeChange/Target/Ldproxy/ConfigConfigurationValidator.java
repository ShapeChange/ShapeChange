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
package de.interactive_instruments.ShapeChange.Target.Ldproxy;

import java.io.File;

import de.interactive_instruments.ShapeChange.ConfigurationValidator;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessConfiguration;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;

/**
 * @author Clemens Portele (portele <at> interactive-instruments <dot> de)
 *
 */
public class ConfigConfigurationValidator
		implements ConfigurationValidator, MessageSource {

	@Override
	public boolean isValid(ProcessConfiguration pConfig, Options options,
			ShapeChangeResult result) {

		boolean isValid = true;

		// ensure that output directory exists
		String outputDirectory = pConfig.getParameterValue("outputDirectory");
		if (outputDirectory == null)
			outputDirectory = options.parameter("outputDirectory");
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
			isValid = false;
			result.addError(this, 2, outputDirectory);
		}

		// validate PARAM_SERVICE_ID
		if (pConfig.hasParameter(ConfigConstants.PARAM_SERVICE_ID)) {

			String sid = options.parameter(Config.class.getName(), ConfigConstants.PARAM_SERVICE_ID);
			
			if (!sid.matches("^[a-zA-Z_][a-zA-Z0-9_]+$")) {
				result.addError(this, 1, sid);
				isValid = false;
			}
		}

		// validate PARAM_MAX_LENGTH
		if (pConfig.hasParameter(ConfigConstants.PARAM_MAX_LENGTH)) {

			String s = options.parameter(Config.class.getName(), ConfigConstants.PARAM_MAX_LENGTH);
			
			try { new Integer(s); }
			catch (Exception e) {
				result.addError(this, 3, ConfigConstants.PARAM_MAX_LENGTH, s);
				isValid = false;
			}
		}

		return isValid;
	}

	@Override
	public String message(int mnr) {

		switch (mnr) {

		case 1:
			return "The id of a service must be alphanumeric (underscore is allowed). The first character must be a letter or an underscore. Found: '$1$'.";
		case 2:
			return "Output directory '$1$' does not exist or is not accessible.";
		case 3:
			return "The parameter '$1$' must be an integer. Found: '$2$'.";
		default:
			return "(ModelExport.java) Unknown message with number: " + mnr;
		}
	}
}
