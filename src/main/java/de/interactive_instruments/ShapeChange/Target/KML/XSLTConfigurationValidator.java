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
package de.interactive_instruments.ShapeChange.Target.KML;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.interactive_instruments.ShapeChange.AbstractConfigurationValidator;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessConfiguration;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class XSLTConfigurationValidator extends AbstractConfigurationValidator {

    protected SortedSet<String> allowedParametersWithStaticNames = new TreeSet<>(
	    Stream.of(XSLT.PARAM_BORIS_SPECIAL, XSLT.PARAM_DEFAULT_REFERENCE, XSLT.PARAM_DEFAULT_STYLE_URL,
		    XSLT.PARAM_EAST, XSLT.PARAM_GROUP_IN_FOLDER, XSLT.PARAM_HREF, XSLT.PARAM_HREF_PROP_ONLY_ID,
		    XSLT.PARAM_MAX_FOLDER_LEVEL, XSLT.PARAM_MIN_LOD_PIXELS, XSLT.PARAM_NORTH, XSLT.PARAM_NS_DEF,
		    XSLT.PARAM_PORTRAYAL_RULE_DOC, XSLT.PARAM_PROP_NAME_CASE, XSLT.PARAM_SOUTH, XSLT.PARAM_WEST,
		    XSLT.PARAM_WITH_EMPTY_PROPS, XSLT.PARAM_WITH_SCHEMA).collect(Collectors.toSet()));
    protected List<Pattern> regexForAllowedParametersWithDynamicNames = Stream
	    .of(Pattern.compile("(defaultStyleUrl|kmlName)\\(.+\\)")).collect(Collectors.toList());

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
