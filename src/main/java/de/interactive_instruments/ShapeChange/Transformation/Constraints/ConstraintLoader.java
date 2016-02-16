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
 * (c) 2002-2015 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Transformation.Constraints;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.TransformerConfiguration;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel;
import de.interactive_instruments.ShapeChange.SBVR.SbvrRuleLoader;
import de.interactive_instruments.ShapeChange.Transformation.Transformer;

/**
 * Loads constraints from an external source and adds them to the model.
 * <p>
 * At the moment only SBVR constraints are supported.
 * 
 * @author Johannes Echterhoff
 *
 */
public class ConstraintLoader implements Transformer {

	public static final String PARAM_CONSTRAINT_EXCEL_FILE = "constraintExcelFile";

	@Override
	public void process(GenericModel m, Options o,
			TransformerConfiguration trfConfig, ShapeChangeResult r)
			throws ShapeChangeAbortException {

		// load SBVR constraint info from excel file
		String sbvrFileLocation = trfConfig
				.getParameterValue(PARAM_CONSTRAINT_EXCEL_FILE);

		/*
		 * if no sbvr file is provided, the loader will simply not contain any
		 * sbvr rules
		 */
		SbvrRuleLoader sbvrLoader = new SbvrRuleLoader(sbvrFileLocation, o, r,
				m);

		for (PackageInfo pi : m.selectedSchemas()) {

			sbvrLoader.loadSBVRRulesAsConstraints(pi);
		}
	}
}
