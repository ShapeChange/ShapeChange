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
 * (c) 2002-2019 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core.model;

import de.interactive_instruments.shapechange.core.ShapeChangeAbortException;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot
 *         de)
 *
 */
public interface ModelProvider {

	/**
	 * @param modelType
	 *                                           Identifies the type of the
	 *                                           model that shall be loaded
	 *                                           (e.g. "EA7", "XMI", "SCXML");
	 *                                           must not be <code>null</code>.
	 * @param repoFileNameOrConnectionString
	 *                                           Connection string to open the
	 *                                           model repository. Can be the
	 *                                           path to a model file, or some
	 *                                           model type specific connection
	 *                                           string (e.g. the connection
	 *                                           string for an EA repository
	 *                                           contained in a database server
	 *                                           or Cloud service). Must not be
	 *                                           <code>null</code>.
	 * @param username
	 *                                           Provide the username if it is
	 *                                           needed to open the model
	 *                                           repository. May be
	 *                                           <code>null</code>.
	 * @param password
	 *                                           Provide the password if it is
	 *                                           needed to open the model
	 *                                           repository. May be
	 *                                           <code>null</code>.
	 * @param isLoadingInputModel
	 *                                           <code>true</code> If the method
	 *                                           call is for loading a model
	 *                                           during the input loading phase,
	 *                                           <code>false</code> if the model
	 *                                           is loaded by a transformer or
	 *                                           target.
	 * @param inputModelTransformer
	 *                                           Qualified name of the
	 *                                           {@link de.interactive_instruments.shapechange.core.model.Transformer}
	 *                                           that shall be used to transform
	 *                                           the model. Typically only used
	 *                                           when loading the input model.
	 *                                           May therefore be
	 *                                           <code>null</code>. Note that it
	 *                                           is assumed that the transformer
	 *                                           is able to transform a model of
	 *                                           the given type.
	 * @return The model loaded from the given repository (and potentially
	 *         transformed).
	 * @throws ShapeChangeAbortException
	 *                                       If an exception occurred while
	 *                                       loading the model. The result log
	 *                                       should provide further details
	 *                                       about the exception.
	 */
	public Model getModel(String modelType,
			String repoFileNameOrConnectionString, String username,
			String password, boolean isLoadingInputModel,
			String inputModelTransformer) throws ShapeChangeAbortException;
}
