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
 * (c) 2002-2024 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core.modelvalidation.basic;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import de.interactive_instruments.shapechange.core.AbstractConfigurationValidator;
import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ProcessConfiguration;
import de.interactive_instruments.shapechange.core.ShapeChangeParseException;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.ValidatorConfiguration;
import de.interactive_instruments.shapechange.core.model.DescriptorAndTagResolver;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class BasicModelValidatorConfigurationValidator extends AbstractConfigurationValidator {

//    protected SortedSet<String> allowedParametersWithStaticNames = new TreeSet<>(
//	    Stream.of().collect(Collectors.toSet()));
    protected SortedSet<String> allowedParametersWithStaticNames = new TreeSet<>();
    protected List<Pattern> regexForAllowedParametersWithDynamicNames = null;

    // will be initialized when isValid(...) is called
    private ValidatorConfiguration validatorConfig = null;

    @Override
    public boolean isValid(ProcessConfiguration pc, Options o, ShapeChangeResult scr) {

	setProcessConfiguration(pc);
	setOptions(o);
	setShapeChangeResult(scr);
	
	this.validatorConfig = (ValidatorConfiguration) config;

	boolean isValid = true;

	isValid = validateParameters(allowedParametersWithStaticNames, regexForAllowedParametersWithDynamicNames,
		config.getParameters().keySet(), result) && isValid;

	// ===== content test elements =====

	if (config.getAdvancedProcessConfigurations() != null) {

	    Element advancedProcessConfigElmt = config.getAdvancedProcessConfigurations();

	    // identify content test elements
	    try {
		List<AbstractContentTestElement> contentTestElmts = BasicModelValidator
			.parseAndValidateContentTestElements(advancedProcessConfigElmt);

		for (AbstractContentTestElement testElmt : contentTestElmts) {

		    if (testElmt instanceof DescriptorContentTestElement descriptorTestElmt) {

			String desc = descriptorTestElmt.getDescriptorOrTaggedValue();
			if (desc.startsWith("TV")) {
			    Matcher m = DescriptorAndTagResolver.taggedValuePattern.matcher(desc);
			    if (!m.matches()) {
				result.addError(this, 109, desc);
				isValid = false;
			    }
			}
		    }
		}

	    } catch (ShapeChangeParseException e) {
		isValid = false;
		result.addError(this, 112, e.getMessage());
	    }
	}

	return isValid;
    }

    @Override
    public String message(int mnr) {

	return switch (mnr) {
	case 0 -> "Context: BasicConfigurationValidator configuration element with 'id'='$1$'.";

	case 100 -> "Parameter '$1$' is set to '$2$'. This is not a valid value.";

	case 109 -> "Tagged value identification value '$1$' in @descriptorOrTaggedValue XML-attribute of DescriptorContentTest configuration element does not match regular expression TV(\\(.+?\\))?:(.+)";
	case 110 -> "";
	case 111 -> "";
	case 112 -> "Invalid content test element(s) encountered: $1$";

	default -> "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	};
    }
}
