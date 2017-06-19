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
package de.interactive_instruments.ShapeChange;

import org.apache.commons.lang.SystemUtils;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class BasicConfigurationValidator implements MessageSource {

	/**
	 * Validates the 'input'-section of the ShapeChange configuration. Any
	 * invalidity is directly logged in the ShapeChangeResult.
	 * 
	 * @param options
	 * @param result
	 * @return <code>true</code> if the configuration is valid, else
	 *         <code>false</code>
	 */
	public boolean isValid(Options options, ShapeChangeResult result) {

		boolean isValid = true;

		String imt = options.parameter("inputModelType");

		if(imt == null) {
			result.addError(null,26);
			isValid = false;
		}
		
		/*
		 * If the input type is EA7 and we are not only executing deferrable
		 * output writers, check that we are running on 32bit Java in a windows
		 * environment.
		 * 
		 * NOTE: Apparently, it is not trivial to detect if a java program is
		 * executed with 32bit or 64bit JRE. The web has many suggestions on how
		 * to do it. Here, we make the assumption that in order to execute
		 * Enterprise Architect (EA), the JRE must be run in Windows. Under that
		 * assumption, the java system property 'os.arch' apparently is always
		 * 'x86' if the program is run with a 32bit JRE.
		 */
		if (imt.equalsIgnoreCase("EA7")
				&& !options.isOnlyDeferrableOutputWrite()) {

			boolean isWindows = SystemUtils.IS_OS_WINDOWS;
			String osArch = SystemUtils.OS_ARCH;

			if (!isWindows) {
				result.addError(this, 1);
				isValid = false;
			} else if (!osArch.equalsIgnoreCase("x86")) {
				result.addError(this, 2, osArch);
				isValid = false;
			}
		} 
		
		return isValid;
	}

	@Override
	public String message(int mnr) {

		switch (mnr) {
		case 0:
			return "Context: class BasicConfigurationValidator";
		case 1:
			return "The input parameter 'inputModelType' is set to 'EA7'. When loading an Enterprise Architect model, ShapeChange must be executed in Windows OS. ShapeChange detected that it is run in a different OS.";
		case 2:
			return "The input parameter 'inputModelType' is set to 'EA7'. When loading an Enterprise Architect model, ShapeChange must be executed in Windows OS with a 32bit JRE. ShapeChange detected that it is not executed with a 32bit JRE. The value of system property 'os.arch' is: '$1$'.";
		default:
			return "(" + BasicConfigurationValidator.class.getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
