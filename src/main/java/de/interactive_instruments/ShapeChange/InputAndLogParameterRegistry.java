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
package de.interactive_instruments.ShapeChange;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import de.interactive_instruments.ShapeChange.Util.ShapeChangeClassFinder;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class InputAndLogParameterRegistry {

    protected SortedSet<String> allowedInputParametersWithStaticNames = new TreeSet<>();
    protected List<Pattern> regexesForAllowedInputParametersWithDynamicNames = new ArrayList<>();
    protected SortedSet<String> allowedLogParametersWithStaticNames = new TreeSet<>();
    protected List<Pattern> regexesForAllowedLogParametersWithDynamicNames = new ArrayList<>();

    public InputAndLogParameterRegistry() throws ShapeChangeAbortException {

	List<Class<?>> inputAndLogParameterProviders = ShapeChangeClassFinder
		.findClassesImplementing("de.interactive_instruments.ShapeChange.InputAndLogParameterProvider");

	for (Class<?> ppCls : inputAndLogParameterProviders) {

	    try {
		InputAndLogParameterProvider pp = (InputAndLogParameterProvider) ppCls.getConstructor().newInstance();

		SortedSet<String> ips = pp.allowedInputParametersWithStaticNames();
		if (ips != null) {
		    allowedInputParametersWithStaticNames.addAll(ips);
		}

		List<Pattern> ipRegexes = pp.regexesForAllowedInputParametersWithDynamicNames();
		if (ipRegexes != null) {
		    regexesForAllowedInputParametersWithDynamicNames.addAll(ipRegexes);
		}

		SortedSet<String> lps = pp.allowedLogParametersWithStaticNames();
		if (lps != null) {
		    allowedLogParametersWithStaticNames.addAll(lps);
		}

		List<Pattern> lpRegexes = pp.regexesForAllowedLogParametersWithDynamicNames();
		if (lpRegexes != null) {
		    regexesForAllowedLogParametersWithDynamicNames.addAll(lpRegexes);
		}

	    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
		    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
		System.err.println("Exception occurred while loading InputAndLogParameterProvider class "
			+ ppCls.getName()
			+ " and attempting to gather information about (additional) allowed input and log parameters.");
		e.printStackTrace();
	    }
	}
    }

    /**
     * @return the allowedInputParametersWithStaticNames
     */
    public SortedSet<String> getAllowedInputParametersWithStaticNames() {
	return allowedInputParametersWithStaticNames;
    }

    /**
     * @return the regexesForAllowedInputParametersWithDynamicNames
     */
    public List<Pattern> getRegexesForAllowedInputParametersWithDynamicNames() {
	return regexesForAllowedInputParametersWithDynamicNames;
    }

    /**
     * @return the allowedLogParametersWithStaticNames
     */
    public SortedSet<String> getAllowedLogParametersWithStaticNames() {
	return allowedLogParametersWithStaticNames;
    }

    /**
     * @return the regexesForAllowedLogParametersWithDynamicNames
     */
    public List<Pattern> getRegexesForAllowedLogParametersWithDynamicNames() {
	return regexesForAllowedLogParametersWithDynamicNames;
    }
}
