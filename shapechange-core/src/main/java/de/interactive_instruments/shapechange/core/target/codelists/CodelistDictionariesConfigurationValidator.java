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
package de.interactive_instruments.shapechange.core.target.codelists;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.interactive_instruments.shapechange.core.AbstractConfigurationValidator;
import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ProcessConfiguration;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class CodelistDictionariesConfigurationValidator extends AbstractConfigurationValidator {

    protected SortedSet<String> allowedParametersWithStaticNames = new TreeSet<>(Stream
	    .of(CodelistDictionaries.PARAM_ADD_STYLESHEET_PROCESSING_INSTRUCTION, CodelistDictionaries.PARAM_CODELISTS,
		    CodelistDictionaries.PARAM_DEFINITION_GMLID_TEMPLATE,
		    CodelistDictionaries.PARAM_DEFINITION_GMLIDENTIFIER_TEMPLATE,
		    CodelistDictionaries.PARAM_ENUMERATIONS, CodelistDictionaries.PARAM_FIXED_IDENTIFIER_CODESPACE,
		    CodelistDictionaries.PARAM_GMLID, CodelistDictionaries.PARAM_IDENTIFIER,
		    CodelistDictionaries.PARAM_NAMES, CodelistDictionaries.PARAM_NAME_SOURCES_TO_ADD_AS_CODESPACE)
	    .collect(Collectors.toSet()));
    protected List<Pattern> regexForAllowedParametersWithDynamicNames = null;

    @Override
    public boolean isValid(ProcessConfiguration config, Options options, ShapeChangeResult result) {

	boolean isValid = true;

	allowedParametersWithStaticNames.addAll(getCommonTargetParameters());
	isValid = validateParameters(allowedParametersWithStaticNames, regexForAllowedParametersWithDynamicNames,
		config.getParameters().keySet(), result) && isValid;

	return isValid;
    }

    @Override
    public String message(int mnr) {
	switch (mnr) {

	default:
	    return "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	}
    }
}
