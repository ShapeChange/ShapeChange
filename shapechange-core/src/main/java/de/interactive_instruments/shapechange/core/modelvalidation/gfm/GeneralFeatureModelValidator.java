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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ProcessRuleSet;
import de.interactive_instruments.shapechange.core.ShapeChangeAbortException;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.ValidatorConfiguration;
import de.interactive_instruments.shapechange.core.model.AssociationInfo;
import de.interactive_instruments.shapechange.core.model.ClassInfo;
import de.interactive_instruments.shapechange.core.model.Model;
import de.interactive_instruments.shapechange.core.model.PropertyInfo;
import de.interactive_instruments.shapechange.core.modelvalidation.AbstractModelValidator;

/**
 * Checks model requirements that originate from the General Feature Model.
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class GeneralFeatureModelValidator extends AbstractModelValidator {

    @Override
    public boolean isValid(Model model, ValidatorConfiguration validatorConfig) throws ShapeChangeAbortException {

	boolean modelIsValid = true;

	Map<String, ProcessRuleSet> ruleSets = validatorConfig.getRuleSets();

	// get the set of all rules defined for the validator
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
	 * validator simply return the result if no rules are defined in the rule sets
	 * (which the schema allows)
	 */
	if (rules.isEmpty())
	    return modelIsValid;

	// apply pre-processing (nothing to do right now)

	// execute rules

	ShapeChangeResult scr = model.result();

	if (rules.contains(GeneralFeatureModelValidatorConstants.RULE_NO_ASSOCIATION_CLASS)) {
	    scr.addProcessFlowInfo(null, 20103, GeneralFeatureModelValidatorConstants.RULE_NO_ASSOCIATION_CLASS);
	    modelIsValid = noAssociationClass(model, validatorConfig) && modelIsValid;
	}

	if (rules.contains(GeneralFeatureModelValidatorConstants.RULE_ASSOCIATION_ONLY_BETWEEN_FEATURETYPES)) {
	    scr.addProcessFlowInfo(null, 20103,
		    GeneralFeatureModelValidatorConstants.RULE_ASSOCIATION_ONLY_BETWEEN_FEATURETYPES);
	    modelIsValid = associationOnlyBetweenFeatureTypes(model, validatorConfig) && modelIsValid;
	}

	if (rules.contains(GeneralFeatureModelValidatorConstants.RULE_DATATYPE_NO_PROP_WITH_FEATURE_VALUETYPE)) {
	    scr.addProcessFlowInfo(null, 20103,
		    GeneralFeatureModelValidatorConstants.RULE_DATATYPE_NO_PROP_WITH_FEATURE_VALUETYPE);
	    modelIsValid = dataTypeNoPropertyWithFeatureValueType(model, validatorConfig) && modelIsValid;
	}

	if (rules.contains(GeneralFeatureModelValidatorConstants.RULE_NO_PROP_WITH_DATATYPE_SUPERTYPE_VALUETYPE)) {
	    scr.addProcessFlowInfo(null, 20103,
		    GeneralFeatureModelValidatorConstants.RULE_NO_PROP_WITH_DATATYPE_SUPERTYPE_VALUETYPE);
	    modelIsValid = noPropertyWithDataTypeSupertypeAsValueType(model, validatorConfig) && modelIsValid;
	}

	// further validation tasks to be added as needed in the future

	return modelIsValid;
    }

    private boolean associationOnlyBetweenFeatureTypes(Model model, ValidatorConfiguration validatorConfig) {

	boolean result = true;

	for (AssociationInfo ai : model.selectedSchemaAssociations()) {

	    /*
	     * Check that an association is only modeled between two feature types.
	     */
	    if ((ai.end1().inClass() != null && model.isInSelectedSchemas(ai.end1().inClass())
		    && ai.end1().inClass().category() != Options.FEATURE)
		    || (ai.end2().inClass() != null && model.isInSelectedSchemas(ai.end2().inClass())
			    && ai.end2().inClass().category() != Options.FEATURE)) {

		this.report(ai, this, 102, ai.end1().inClass().name(), ai.end1().name(), ai.end2().inClass().name(),
			ai.end2().name(), validatorConfig.getValidationMode());
		result = false;
	    }
	}

	return result;
    }

    private boolean noAssociationClass(Model model, ValidatorConfiguration validatorConfig) {

	boolean result = true;

	for (ClassInfo ci : model.selectedSchemaClasses()) {
	    if (ci.isAssocClass() != null) {
		this.report(ci, this, 101, ci.name(), validatorConfig.getValidationMode());
		result = false;
	    }
	}

	return result;
    }

    private boolean dataTypeNoPropertyWithFeatureValueType(Model model, ValidatorConfiguration validatorConfig) {

	boolean result = true;

	for (ClassInfo ci : model.selectedSchemaClasses()) {

	    /*
	     * Check that a data type does not have a property with a feature type as type.
	     */
	    if (ci.category() == Options.DATATYPE) {

		for (PropertyInfo pi : ci.properties().values()) {
		    if (pi.categoryOfValue() == Options.FEATURE) {
			this.report(pi, this, 100, ci.name(), pi.name(), validatorConfig.getValidationMode());
			result = false;
		    }
		}
	    }
	}

	return result;
    }

    private boolean noPropertyWithDataTypeSupertypeAsValueType(Model model, ValidatorConfiguration validatorConfig) {

	boolean result = true;

	for (ClassInfo ci : model.selectedSchemaClasses()) {

	    for (PropertyInfo pi : ci.properties().values()) {
		ClassInfo typeCi = pi.typeClass();
		if (pi.categoryOfValue() == Options.DATATYPE && typeCi != null && !typeCi.subtypes().isEmpty()
			&& model.isInSelectedSchemas(typeCi)) {
		    this.report(pi, this, 103, ci.name(), pi.name(), validatorConfig.getValidationMode());
		    result = false;
		}
	    }
	}

	return result;
    }

    @Override
    public String message(int mnr) {

	return switch (mnr) {

	case 0 -> "Context: class '$1$'";
	case 1 -> "Context: property '$1$'";
	case 2 -> "Context: $1$";

	case 100 -> "Data type '$1$', property '$2$' shall not have a feature type as value type.";
	case 101 -> "Class '$1$' shall not be an association class.";
	case 102 ->
	    "Association ($1$.$2$ - $3$.$4$) within schemas selected for processing shall only be modeled between two feature types.";
	case 103 -> "Data type '$1$', property '$2$' shall not use a data type that is a supertype as value type.";

	default -> "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	};
    }
}
