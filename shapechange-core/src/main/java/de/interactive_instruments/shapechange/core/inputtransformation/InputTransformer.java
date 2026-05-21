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

package de.interactive_instruments.shapechange.core.inputtransformation;

import de.interactive_instruments.shapechange.core.InputTransformerConfiguration;
import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.Process;
import de.interactive_instruments.shapechange.core.ShapeChangeAbortException;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;

/**
 * Defines the operation(s) common to all input transformers.
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 */
public interface InputTransformer extends Process {

    /**
     * Initialise the input transformer.
     * 
     * @param o                              Options to control the process
     *                                       execution.
     * @param itrfConfig                     Specific configuration for this input
     *                                       transformer.
     * @param r                              Logging target.
     * @param repoFileNameOrConnectionString how to access the model
     * @throws ShapeChangeAbortException tbd
     */
    public void initialise(Options o, InputTransformerConfiguration itrfConfig, ShapeChangeResult r,
	    String repoFileNameOrConnectionString) throws ShapeChangeAbortException;

    /**
     * Routine to free or close any resources or connections used by the
     * transformer.
     */
    public void shutdown();

    /**
     * Transform the input model.
     * 
     * @throws ShapeChangeAbortException tbd
     */
    public void transform() throws ShapeChangeAbortException;

    /**
     * Check if the transformer supports the model source, identified via the given
     * model type (NOTE: identifier comparison must ignore case). Examples of model
     * type identifiers are 'EA7' and 'SCXML'.
     * 
     * @param modelType ShapeChange identifier for a model
     * @return <code>true</code>, if the transformer recognizes the given model type
     *         identifier, and is able to work with an according model source; else
     *         <code>false</code>
     */
    public boolean isApplicableToInputModelType(String modelType);
}