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
 * (c) 2002-2012 interactive instruments GmbH, Bonn, Germany
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

package de.interactive_instruments.ShapeChange.Transformation;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.Process;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.TransformerConfiguration;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel;

/**
 * Defines the operation(s) common to all actual transformers.
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot
 *         de)
 */
public interface Transformer extends Process {

	/**
	 * Processes the model.
	 * 
	 * The given model is changed by the Transformer. NOTE: The
	 * TransformationManager ensures that this can be done without side-effects.
	 * 
	 * @param m
	 *            The model to process.
	 * @param o
	 *            Options to control the process execution.
	 * @param trfConfig  tbd
	 * @param r
	 *            Logging target.
	 * @throws ShapeChangeAbortException tbd
	 */
	public void process(GenericModel m, Options o,
			TransformerConfiguration trfConfig, ShapeChangeResult r)
			throws ShapeChangeAbortException;
}