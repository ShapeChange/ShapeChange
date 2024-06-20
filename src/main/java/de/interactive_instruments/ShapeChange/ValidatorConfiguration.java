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
package de.interactive_instruments.ShapeChange;

import java.util.Map;

import org.w3c.dom.Element;

/**
 * Configuration of a validator.
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class ValidatorConfiguration extends ProcessConfiguration {

    /**
     * Identifier of the validator, unique within a ShapeChangeConfiguration.
     */
    private String id;

    /**
     * The validation mode of this validator.
     */
    private ValidationMode validationMode = ValidationMode.strict;

    /**
     * Creates a ValidatorConfiguration.
     * 
     * @param id                            Validator identifier
     * @param className                     The fully qualified name of the class
     *                                      implementing the validator.
     * @param processMode                   The execution mode of the validator.
     * @param parameters                    The validator parameters.
     *                                      <code>null</code> if no parameters were
     *                                      declared in the configuration.
     * @param ruleSets                      The rule sets declared for the
     *                                      validator. <code>null</code> if no rule
     *                                      sets were declared in the configuration.
     * @param validationMode                The validation mode declared for this
     *                                      validator.
     * @param advancedProcessConfigurations the 'advancedProcessConfigurations'
     *                                      element from the configuration of the
     *                                      process; <code>null</code> if it is not
     *                                      set there
     */
    public ValidatorConfiguration(String id, String className, ProcessMode processMode, Map<String, String> parameters,
	    Map<String, ProcessRuleSet> ruleSets, ValidationMode validationMode,
	    Element advancedProcessConfigurations) {
	super(className, processMode, parameters, ruleSets, null, advancedProcessConfigurations);
	this.id = id;
	this.validationMode = validationMode;
    }

    /**
     * @return The identifier of the process.
     */
    public String getId() {
	return id;
    }

    /**
     * @return The identifier of the input for this transformer, for example
     *         referencing the global model input.
     */
    public ValidationMode getValidationMode() {
	return validationMode;
    }

    public String toString() {

	StringBuffer sb = new StringBuffer();

	sb.append("ValidatorConfiguration:\r\n");
	sb.append("\tid: " + id + "\r\n");
	sb.append("\tvalidationMode: " + validationMode.toString() + "\r\n");
	sb.append("--- with process configuration:\r\n");
	sb.append(super.toString());

	return sb.toString();
    }

    public boolean hasRule(String ruleID) {

	return this.getAllRules().contains(ruleID);
    }

}
