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
package de.interactive_instruments.shapechange.core.modelvalidation.gfm;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import de.interactive_instruments.shapechange.core.AbstractConfigurationValidator;
import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ProcessConfiguration;
import de.interactive_instruments.shapechange.core.ProcessRuleSet;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.ValidatorConfiguration;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class GeneralFeatureModelValidatorConfigurationValidator extends AbstractConfigurationValidator {

    protected SortedSet<String> allowedParametersWithStaticNames = new TreeSet<>();
    protected List<Pattern> regexForAllowedParametersWithDynamicNames = null;

    // these fields will be initialized when isValid(...) is called
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

	Map<String, ProcessRuleSet> ruleSets = validatorConfig.getRuleSets();

	// get the set of all rules defined for the validator
	SortedSet<String> rules = new TreeSet<String>();
	if (!ruleSets.isEmpty()) {
	    for (ProcessRuleSet ruleSet : ruleSets.values()) {
		if (ruleSet.getAdditionalRules() != null) {
		    rules.addAll(ruleSet.getAdditionalRules());
		}
	    }
	}

	rules.remove(GeneralFeatureModelValidatorConstants.RULE_ASSOCIATION_ONLY_BETWEEN_FEATURETYPES);
	rules.remove(GeneralFeatureModelValidatorConstants.RULE_DATATYPE_NO_PROP_WITH_FEATURE_VALUETYPE);
	rules.remove(GeneralFeatureModelValidatorConstants.RULE_NO_ASSOCIATION_CLASS);
	rules.remove(GeneralFeatureModelValidatorConstants.RULE_NO_PROP_WITH_DATATYPE_SUPERTYPE_VALUETYPE);

	if (!rules.isEmpty()) {
	    for (String r : rules) {
		result.addError(this, 101, r);
	    }
	    isValid = false;
	}

	return isValid;
    }

    @Override
    public String message(int mnr) {

	return switch (mnr) {
	case 0 -> "";

	case 100 -> "Parameter '$1$' is set to '$2$'. This is not a valid value.";

	case 101 -> "Configured valdiator rule '$1$' is unknown. Check for any spelling mistakes and typos.";

	default -> "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	};
    }
}
