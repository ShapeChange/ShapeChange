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
 * (c) 2002-2016 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Transformation.Flattening;

import java.util.Set;

import de.interactive_instruments.ShapeChange.ConfigurationValidator;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessConfiguration;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.TransformerConfiguration;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class FlattenerConfigurationValidator
		implements ConfigurationValidator, MessageSource {

	@Override
	public boolean isValid(ProcessConfiguration pConfig, Options options,
			ShapeChangeResult result) {

		boolean isValid = true;

		/*
		 * NOTE: No type check for the configuration is performed, since a
		 * mismatch would be a system error
		 */
		TransformerConfiguration trfConfig = (TransformerConfiguration) pConfig;

		String id = trfConfig.getId();

		Set<String> rules = trfConfig.getAllRules();

		// validate configuration for rule-trf-all-removeType
		if (rules.contains(Flattener.RULE_TRF_ALL_REMOVETYPE)) {

			if (!trfConfig.hasParameter(Flattener.PARAM_REMOVE_TYPE)
					|| trfConfig.getParameterValue(Flattener.PARAM_REMOVE_TYPE)
							.trim().length() == 0) {

				MessageContext mc = result.addWarning(this, 100);
				if (mc != null)
					mc.addDetail(this, 0, id);
			}
		}

		return isValid;
	}

	@Override
	public String message(int mnr) {

		switch (mnr) {
		case 0:
			return "Context: Transformer configuration element with 'id'='$1$'.";
		case 100:
			return "Parameter '" + Flattener.PARAM_REMOVE_TYPE
					+ "' is required for the execution of '"
					+ Flattener.RULE_TRF_ALL_REMOVETYPE
					+ "'. The configuration does not contain this parameter with a non-empty string.";

		default:
			return "(" + FlattenerConfigurationValidator.class.getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
