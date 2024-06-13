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
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange.ModelValidation.Basic;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import de.interactive_instruments.ShapeChange.AbstractConfigurationValidator;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessConfiguration;
import de.interactive_instruments.ShapeChange.ShapeChangeParseException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ValidatorConfiguration;
import de.interactive_instruments.ShapeChange.Model.DescriptorAndTagResolver;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class BasicModelValidatorConfigurationValidator extends AbstractConfigurationValidator {

//    protected SortedSet<String> allowedParametersWithStaticNames = new TreeSet<>(
//	    Stream.of().collect(Collectors.toSet()));
    protected SortedSet<String> allowedParametersWithStaticNames = new TreeSet<>();
    protected List<Pattern> regexForAllowedParametersWithDynamicNames = null;

    // these fields will be initialized when isValid(...) is called
    private ValidatorConfiguration validatorConfig = null;
    private Options options = null;
    private ShapeChangeResult result = null;

    @Override
    public boolean isValid(ProcessConfiguration config, Options options, ShapeChangeResult result) {

	this.validatorConfig = (ValidatorConfiguration) config;
	this.options = options;
	this.result = result;

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

		    if (testElmt instanceof DescriptorContentTestElement) {

			DescriptorContentTestElement descriptorTestElmt = (DescriptorContentTestElement) testElmt;

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

	switch (mnr) {
	case 0:
	    return "Context: BasicConfigurationValidator configuration element with 'id'='$1$'.";

	case 100:
	    return "Parameter '$1$' is set to '$2$'. This is not a valid value.";

	case 109:
	    return "Tagged value identification value '$1$' in @descriptorOrTaggedValue XML-attribute of DescriptorContentTest configuration element does not match regular expression TV(\\(.+?\\))?:(.+)";
	case 110:
	    return "";
	case 111:
	    return "";
	case 112:
	    return "Invalid content test element(s) encountered: $1$";

	default:
	    return "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	}
    }
}
