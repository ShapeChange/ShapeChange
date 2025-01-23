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
package de.interactive_instruments.shapechange.ea.target.sql.databasemodel;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.sparx.Repository;

import de.interactive_instruments.shapechange.core.MessageSource;
import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.target.sql.DatabaseModelConstants;
import de.interactive_instruments.shapechange.core.target.sql.DatabaseModelWriter;
import de.interactive_instruments.shapechange.core.target.sql.SqlDdl;
import de.interactive_instruments.shapechange.core.target.sql.structure.Statement;
import de.interactive_instruments.shapechange.ea.util.EAException;
import de.interactive_instruments.shapechange.ea.util.EARepositoryUtil;

/**
 * Creates a database model for an application schema.
 *
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class DatabaseModelWriterEA implements DatabaseModelWriter, MessageSource {

    @Override
    public void write(SqlDdl sqlDdl, List<Statement> stmts, ShapeChangeResult result) throws Exception {
	
	Options options = result.options();
	
	String fileNameDM = sqlDdl.outputFilename() + ".qea";
	    File eaRepo = new File(sqlDdl.outputDirectory(), fileNameDM);

	    String eaRepoFilePathByConfig;
	    if (options.hasParameter(SqlDdl.class.getName(),
		    DatabaseModelConstants.PARAM_DATAMODEL_EA_REPOSITORY_PATH)) {
		eaRepoFilePathByConfig = options.parameter(SqlDdl.class.getName(),
			DatabaseModelConstants.PARAM_DATAMODEL_EA_REPOSITORY_PATH);
	    } else {
		eaRepoFilePathByConfig = options.parameter(SqlDdl.class.getName(),
			DatabaseModelConstants.PARAM_DATAMODEL_EAP_PATH);
	    }

	    if (eaRepoFilePathByConfig != null) {

		if (eaRepoFilePathByConfig.toLowerCase().startsWith("http")) {

		    // copy ea repo file from remote URI
		    try {
			URL eaRepoUrl = URI.create(eaRepoFilePathByConfig).toURL();
			FileUtils.copyURLToFile(eaRepoUrl, eaRepo);
			result.addInfo(this, 30, eaRepoFilePathByConfig, eaRepo.getAbsolutePath());
		    } catch (MalformedURLException e1) {
			result.addError(this, 28, eaRepoFilePathByConfig);
		    } catch (IOException e2) {
			result.addFatalError(this, 29, e2.getMessage());
		    }

		} else {

		    result.addInfo(this, 31, eaRepoFilePathByConfig);
		    eaRepo = new File(eaRepoFilePathByConfig);

		    /*
		     * In case that the EA repo file does not exist yet,
		     * EARepositoryUtil.openRepository() also takes care of creating the necessary
		     * directory structure, so no need to do this here.
		     */
		}
	    }

	    Repository repository = EARepositoryUtil.openRepository(eaRepo, true);

	    EARepositoryUtil.setEABatchAppend(repository, true);
	    EARepositoryUtil.setEAEnableUIUpdates(repository, false);

	    try {
		DatabaseModelVisitor dmVisitor = new DatabaseModelVisitor(sqlDdl, repository, result);
		dmVisitor.initialize();

		dmVisitor.visit(stmts);

		dmVisitor.postprocess();

		result.addResult(sqlDdl.getTargetName(), sqlDdl.outputDirectory(), fileNameDM, null);

	    } catch (EAException e) {
		result.addError(this, 27, e.getMessage());
	    } catch (NullPointerException npe) {
		if (npe.getMessage() != null) {
		    result.addError(this, 27, npe.getMessage());
		}
		npe.printStackTrace(System.err);
	    } finally {
		EARepositoryUtil.closeRepository(repository);
		repository = null;
	    }
    }
    
    @Override
    public String message(int mnr) {

	switch (mnr) {
	
	case 27:
	    return "Exception occurred while creating database model. Exception message is: $1$";
	case 28:
	    return "URL '$1$' provided for configuration parameter "
		    + DatabaseModelConstants.PARAM_DATAMODEL_EA_REPOSITORY_PATH
		    + " is malformed. The data model will be created in a new EA repository within the output directory.";
	case 29:
	    return "Exception encountered while copying the data model EA repository file defined by configuration parameter "
		    + DatabaseModelConstants.PARAM_DATAMODEL_EA_REPOSITORY_PATH
		    + " to the output directory. The data model will be created in a new EA repository within the output directory.";
	case 30:
	    return "Copied EA repository file for creation of the data model from URL '$1$' to '$2$'.";
	case 31:
	    return "Using local EA repository file '$1$' for creation of the data model.";
	
	default:
	    return "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	}
    }

}
