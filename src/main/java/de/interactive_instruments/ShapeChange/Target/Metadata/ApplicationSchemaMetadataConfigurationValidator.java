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
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange.Target.Metadata;

import java.util.HashSet;
import java.util.Set;

import de.interactive_instruments.ShapeChange.ConfigurationValidator;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessConfiguration;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments <dot>
 *         de)
 *
 */
public class ApplicationSchemaMetadataConfigurationValidator
		implements ConfigurationValidator, MessageSource {

	@Override
	public boolean isValid(ProcessConfiguration pConfig, Options options,
			ShapeChangeResult result) {

		boolean isValid = true;

		if (pConfig.getAllRules().contains(
				ApplicationSchemaMetadata.RULE_ALL_IDENTIFY_TYPE_USAGE)) {

			Set<String> typesForTypeUsage = new HashSet<>(
					options.parameterAsStringList(ApplicationSchemaMetadata.class.getName(),
							ApplicationSchemaMetadata.PARAM_TYPES_FOR_TYPE_USAGE_IDENTIFICATION,
							null, true, true));

			if (typesForTypeUsage.isEmpty()) {
				
				isValid = false;
				
				result.addError(this, 3,
						ApplicationSchemaMetadata.RULE_ALL_IDENTIFY_TYPE_USAGE,
						ApplicationSchemaMetadata.PARAM_TYPES_FOR_TYPE_USAGE_IDENTIFICATION);
			}
		}

		return isValid;
	}

	@Override
	public String message(int mnr) {

		switch (mnr) {

		case 1:
			return "Syntax exception while compiling the regular expression defined by target parameter '$1$': '$2$'.";
		case 2:
			return "Output directory '$1$' does not exist or is not accessible.";
		case 3:
			return "Rule '$1$' is contained in the target configuration. Parameter '$2$' required by that rule was not provided or is invalid. Provide a valid value for this parameter.";
		default:
			return "(ApplicationSchemaMetadata.java) Unknown message with number: "
					+ mnr;
		}
	}

}
