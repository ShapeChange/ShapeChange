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
package de.interactive_instruments.shapechange.core;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.shapechange.core.model.Model;
import de.interactive_instruments.shapechange.core.model.ModelProvider;
import de.interactive_instruments.shapechange.core.model.Transformer;

/**
 * Default implementation for loading models. Recognized model types are EA7,
 * XMI, and SCXML (as well as GCSR, for backwards compatibility).
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class DefaultModelProvider implements ModelProvider, MessageSource {

    private ShapeChangeResult result;
    private Options options;

    public DefaultModelProvider(ShapeChangeResult result, Options options) {
	this.result = result;
	this.options = options;
    }

    @Override
    public Model getModel(String modelType, String repoFileNameOrConnectionString, String username, String password,
	    boolean isLoadingInputModel, String inputModelTransformer) throws ShapeChangeAbortException {

	if (StringUtils.isBlank(repoFileNameOrConnectionString)) {
	    result.addFatalError(this, 24);
	    throw new ShapeChangeAbortException();
	}

	String user = username == null ? "" : username;
	String pwd = password == null ? "" : password;

	// Support original model type codes
	if (modelType == null) {
	    result.addFatalError(this, 26);
	    throw new ShapeChangeAbortException();
	} else if (modelType.equalsIgnoreCase("ea7")) {
	    modelType = "de.interactive_instruments.shapechange.ea.model.EADocument";
	} else if (modelType.equalsIgnoreCase("xmi10")) {
	    modelType = "de.interactive_instruments.shapechange.core.model.xmi10.Xmi10Document";
	} else if (modelType.equalsIgnoreCase("gcsr")) {
	    modelType = "gov.nga.ShapeChange.Model.GCSR.GCSRModel";
	} else if (modelType.equalsIgnoreCase("scxml")) {
	    modelType = "de.interactive_instruments.shapechange.core.model.generic.GenericModel";
	} else {
	    result.addInfo(this, 27, modelType);
	}

	if (isLoadingInputModel && StringUtils.isNotBlank(inputModelTransformer)) {

	    try {
		Class<?> theClass = Class.forName(inputModelTransformer);
		Transformer t = (Transformer) theClass.getConstructor().newInstance();
		t.initialise(options, result, repoFileNameOrConnectionString);
		t.transform();
		t.shutdown();
	    } catch (Exception e) {
		e.printStackTrace();
		throw new ShapeChangeAbortException();
	    }
	}

	Model m = null;

	if (isLoadingInputModel && options.isSkipModelLoadingIfProcessingIsOnlyInputTransformations()) {    
	    result.addProcessFlowInfo(this,28);
	} else {

	    // Get model object from reflection API
	    Class<?> theClass;

	    try {

		theClass = Class.forName(modelType);
		if (theClass == null) {
		    result.addFatalError(this, 17, modelType);
		    throw new ShapeChangeAbortException();
		}

		m = (Model) theClass.getConstructor().newInstance();

		if (m != null) {

		    if (user.length() == 0) {
			m.initialise(result, options, repoFileNameOrConnectionString);
		    } else {
			m.initialise(result, options, repoFileNameOrConnectionString, user, pwd);
		    }

		    m.loadInformationFromExternalSources(isLoadingInputModel);

		    // Prepare and check model
		    m.postprocessAfterLoadingAndValidate();

		} else {
		    result.addFatalError(this, 17, modelType);
		    throw new ShapeChangeAbortException();
		}

	    } catch (ClassNotFoundException e) {
		result.addFatalError(this, 17, modelType);
		throw new ShapeChangeAbortException();
	    } catch (IllegalArgumentException | InstantiationException | InvocationTargetException
		    | NoSuchMethodException e) {
		result.addFatalError(this, 19, modelType);
		throw new ShapeChangeAbortException();
	    } catch (IllegalAccessException e) {
		result.addFatalError(this, 20, modelType);
		throw new ShapeChangeAbortException();
	    }
	}

	return m;
    }

    @Override
    public String message(int mnr) {

	/*
	 * NOTE: A leading ?? in a message text suppresses multiple appearance of a
	 * message in the output.
	 */
	switch (mnr) {

	case 17:
	    return "Unknown model type: '$1$'.";
	case 19:
	    return "Model object could not be instantiated: '$1$'.";
	case 20:
	    return "Model object could not be accessed: '$1$'.";
	case 24:
	    return "Repository filename or connection string was not provided. Cannot connect to a repository.";
	case 26:
	    return "Model type not provided.";
	case 27:
	    return "Using custom model implementation: '$1$'.";
	case 28:
	    return "Loading of input model is skipped. No model transformations or targets are configured.";
	    
	default:
	    return "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	}
    }

}
