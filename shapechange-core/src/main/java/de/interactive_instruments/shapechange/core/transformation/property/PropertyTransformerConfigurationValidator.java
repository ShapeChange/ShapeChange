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
 * (c) 2002-2026 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core.transformation.property;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import de.interactive_instruments.shapechange.core.AbstractConfigurationValidator;
import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ProcessConfiguration;
import de.interactive_instruments.shapechange.core.ShapeChangeParseException;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.TransformerConfiguration;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class PropertyTransformerConfigurationValidator extends AbstractConfigurationValidator {

    protected SortedSet<String> allowedParametersWithStaticNames = new TreeSet<>(
//	    Stream.of().collect(Collectors.toSet())
    );
    protected List<Pattern> regexForAllowedParametersWithDynamicNames = null;

    @Override
    public boolean isValid(ProcessConfiguration pc, Options o, ShapeChangeResult scr) {

	setProcessConfiguration(pc);
	setOptions(o);
	setShapeChangeResult(scr);

	TransformerConfiguration trfConfig = (TransformerConfiguration) config;

	boolean isValid = true;

	allowedParametersWithStaticNames.addAll(getCommonTransformerParameters());
	isValid = validateParameters(allowedParametersWithStaticNames, regexForAllowedParametersWithDynamicNames,
		config.getParameters().keySet(), result) && isValid;

	if (config.getAdvancedProcessConfigurations() != null) {

	    Element advancedProcessConfigElmt = config.getAdvancedProcessConfigurations();

	    // identify annotation elements
	    try {
		List<PropertyTransformationElement> transformationElmts = PropertyTransformationParser
			.parseAndValidatePropertyTransformationElements(advancedProcessConfigElmt);

		for (PropertyTransformationElement pte : transformationElmts) {

		    if (!pte.getRule().equalsIgnoreCase(PropertyTransformerConstants.RULE_PROP_DELETE)) {
			result.addError(this, 100, pte.getRule());
			isValid = false;
		    }
		}
	    } catch (ShapeChangeParseException e) {
		isValid = false;
		result.addError(this, 101, e.getMessage());
	    }
	}

	return isValid;
    }

    @Override
    public String message(int mnr) {

	return switch (mnr) {
	case 0 -> "Context: property '$1$'.";
	case 1 -> "Context: class '$1$'.";
	case 2 -> "Context: association class '$1$'.";
	case 3 ->
	    "Context: association between class '$1$' (with property '$2$') and class '$3$' (with property '$4$')";

	case 10 -> "Syntax exception for regular expression '$1$' of parameter '$2$'. Message is: $3$.";

	case 100 ->
	    "Value '$1$' in @rule of PropertyTransformation configuration element does not match one of the rule identifiers supported by the PropertyTransformer.";
	case 101 -> "Invalid property transformation element(s) encountered: $1$";

	default -> "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	};
    }
}
