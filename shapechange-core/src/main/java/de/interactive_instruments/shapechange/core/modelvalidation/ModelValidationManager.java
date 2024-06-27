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
package de.interactive_instruments.shapechange.core.modelvalidation;

import java.util.List;
import java.util.Objects;

import de.interactive_instruments.shapechange.core.MessageSource;
import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ProcessMode;
import de.interactive_instruments.shapechange.core.ShapeChangeAbortException;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.ValidationMode;
import de.interactive_instruments.shapechange.core.ValidatorConfiguration;
import de.interactive_instruments.shapechange.core.model.Model;

/**
 * Manages the validation of a model, taking into account all model validators
 * defined for a given transformer or target.
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 */
public class ModelValidationManager implements MessageSource {

    /**
     * Validates the given model, executing all non-disabled validators with given
     * IDs. The model will only be considered invalid, if the validation result for
     * one of the enabled validators is false and the validation mode for that
     * validator is strict.
     * 
     * @param model        the model to check (provides access to ShapeChangeResult
     *                     and Options objects)
     * @param validatorIds Identifiers of the model validators that shall be
     *                     executed.
     * @return <code>true</code>, if the model is considered valid, else
     *         <code>false</code>
     * @throws ShapeChangeAbortException tbd
     */
    public boolean isValid(Model model, List<String> validatorIds) throws ShapeChangeAbortException {

	Options options = model.options();
	ShapeChangeResult result = model.result();

	boolean overallValidationResult = true;

	for (String validatorId : validatorIds) {

	    ValidatorConfiguration vConfig = options.getValidatorConfigs().get(validatorId);

	    /*
	     * XML Schema validation of the ShapeChange configuration ensures that a
	     * validator configuration is present for each configured validator.
	     */

	    if (vConfig.getProcessMode() == ProcessMode.disabled) {
		continue;
	    }

	    result.addInfo(this, 104, validatorId);

	    Class<?> theClass;
	    ModelValidator validator;

	    try {
		theClass = Class.forName(vConfig.getClassName());
		validator = (ModelValidator) theClass.getConstructor().newInstance();

	    } catch (Exception e) {
		throw new ShapeChangeAbortException("Could not load model validator class '" + vConfig.getClassName()
			+ ". Exception message is: " + Objects.toString(e.getMessage(), "<null>"));
	    }

	    // execute actual validation
	    boolean isValid = validator.isValid(model, vConfig);

	    /*
	     * Only consider overall validation failed if the actual validation result is
	     * fail, process mode is enabled (not just diagnostics only), and validation
	     * mode is strict (not lax).
	     */
	    if (!isValid && vConfig.getProcessMode() == ProcessMode.enabled
		    && vConfig.getValidationMode() == ValidationMode.strict) {
		overallValidationResult = false;
	    }
	}

	return overallValidationResult;
    }

    @Override
    public String message(int mnr) {

	/*
	 * NOTE: A leading ?? in a message text suppresses multiple appearance of a
	 * message in the output.
	 */
	switch (mnr) {

	case 100:
	    return "";
	case 101:
	    return "";
	case 102:
	    return "";
	case 103:
	    return "";
	case 104:
	    return "---------- Executing model validator '$1$' ----------";

	default:
	    return "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	}
    }
}
