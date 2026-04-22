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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;

import de.interactive_instruments.shapechange.core.MessageSource;
import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ProcessRuleSet;
import de.interactive_instruments.shapechange.core.ShapeChangeAbortException;
import de.interactive_instruments.shapechange.core.ShapeChangeParseException;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.TransformerConfiguration;
import de.interactive_instruments.shapechange.core.model.generic.GenericModel;
import de.interactive_instruments.shapechange.core.model.generic.GenericPropertyInfo;
import de.interactive_instruments.shapechange.core.transformation.Transformer;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class PropertyTransformer implements Transformer, MessageSource {

    private GenericModel genModel = null;
    private Options options = null;
    private ShapeChangeResult result = null;

    @Override
    public void process(GenericModel genModel, Options options, TransformerConfiguration trfConfig,
	    ShapeChangeResult result) throws ShapeChangeAbortException {

	this.genModel = genModel;
	this.options = options;
	this.result = result;

	Map<String, ProcessRuleSet> ruleSets = trfConfig.getRuleSets();

	// get the set of all rules defined for the transformation
	Set<String> rules = new HashSet<String>();
	if (!ruleSets.isEmpty()) {
	    for (ProcessRuleSet ruleSet : ruleSets.values()) {
		if (ruleSet.getAdditionalRules() != null) {
		    rules.addAll(ruleSet.getAdditionalRules());
		}
	    }
	}

	/*
	 * because there are no mandatory - in other words default - rules for this
	 * transformer simply return the model if no rules are defined in the rule sets
	 * (which the schema allows)
	 */
	if (rules.isEmpty())
	    return;

	List<PropertyTransformationElement> transformationElmts = new ArrayList<>();

	if (options.getCurrentProcessConfig().getAdvancedProcessConfigurations() == null) {

	    result.addDebug(this, 12);

	} else {

	    Element advancedProcessConfigElmt = options.getCurrentProcessConfig().getAdvancedProcessConfigurations();

	    // identify property transformation elements
	    try {
		transformationElmts = PropertyTransformationParser
			.parseAndValidatePropertyTransformationElements(advancedProcessConfigElmt);
	    } catch (ShapeChangeParseException e) {
		result.addError(this, 104, e.getMessage());
	    }
	}

	// apply pre-processing (nothing to do right now)

	// execute rules

	if (rules.contains(PropertyTransformerConstants.RULE_PROP_DELETE)) {
	    result.addProcessFlowInfo(null, 20103, PropertyTransformerConstants.RULE_PROP_DELETE);
	    applyRulePropertyDelete(trfConfig,
		    transformationElmts.stream().filter(
			    pte -> pte.getRule().equalsIgnoreCase(PropertyTransformerConstants.RULE_PROP_DELETE))
			    .toList());
	}

	// apply post-processing (nothing to do right now)
    }

    private void applyRulePropertyDelete(TransformerConfiguration trfConfig,
	    List<PropertyTransformationElement> propertyTransformationsForDeleteRule) {

	if (propertyTransformationsForDeleteRule == null || propertyTransformationsForDeleteRule.isEmpty()) {

	    result.addWarning(this, 100);

	} else {

	    List<GenericPropertyInfo> propsToDelete = new ArrayList<>();

	    for (GenericPropertyInfo genPi : genModel.selectedSchemaProperties()) {

		for (PropertyTransformationElement pte : propertyTransformationsForDeleteRule) {
		    if (pte.getModelElementSelectionInfo().matches(genPi)) {
			propsToDelete.add(genPi);
		    }
		}
	    }

	    for (GenericPropertyInfo genPi : propsToDelete) {
		genModel.remove(genPi, true);
	    }
	}

    }

    @Override
    public String message(int mnr) {

	return switch (mnr) {
	case 0 -> "Context: property '$1$'.";
	case 1 -> "Context: class '$1$'.";
	case 2 -> "Context: association class '$1$'.";
	case 3 ->
	    "Context: association between class '$1$' (with property '$2$') and class '$3$' (with property '$4$')";
	case 4 -> "Context: supertype '$1$'";
	case 5 -> "Context: subtype '$1$'";

	case 10 ->
	    "Syntax exception for regular expression '$1$' of parameter '$2$'. Message is: $3$. $4$ will not have any effect.";
	case 12 ->
	    "The property transformer configuration does not contain an advanced process configuration element with definitions of property transformations.";

	case 100 ->
	    "No property transformation(s) present for configured " + PropertyTransformerConstants.RULE_PROP_DELETE;

	case 104 -> "Invalid property transformation(s) encountered (they will be ignored): $1$";

	default -> "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	};
    }
}
