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

package de.interactive_instruments.ShapeChange.ModelValidation;

import de.interactive_instruments.ShapeChange.Process;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ValidatorConfiguration;
import de.interactive_instruments.ShapeChange.Model.Model;

/**
 * Defines the operation(s) common to all model validators.
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 */
public interface ModelValidator extends Process {

    /**
     * Validate the model.
     * 
     * @param m               The model to process (provides access to
     *                        ShapeChangeResult and Options objects)
     * @param validatorConfig tbd
     * @return <code>true</code>, if the model passed validation as defined for this
     *         validator in the given configuration, else <code>false</code>.
     *         Ignores the process mode and validation mode settings in the
     *         validator configuration.
     * @throws ShapeChangeAbortException tbd
     */
    public boolean isValid(Model m, ValidatorConfiguration validatorConfig) throws ShapeChangeAbortException;
}