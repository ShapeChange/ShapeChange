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
package de.interactive_instruments.ShapeChange.Target.SQL;

import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import de.interactive_instruments.ShapeChange.ConfigurationValidator;
import de.interactive_instruments.ShapeChange.MapEntryParamInfos;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessConfiguration;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class SqlDdlConfigurationValidator
		implements ConfigurationValidator, MessageSource {

	@Override
	public boolean isValid(ProcessConfiguration config, Options options,
			ShapeChangeResult result) {

		boolean isValid = true;

		TreeMap<String, ProcessMapEntry> mapEntryByType = new TreeMap<String, ProcessMapEntry>();
		for (ProcessMapEntry pme : config.getMapEntries()) {
			mapEntryByType.put(pme.getType(), pme);
		}

		MapEntryParamInfos mepp = new MapEntryParamInfos(result,
				config.getMapEntries());
		if (!mepp.isValid()) {
			isValid = false;
		}

		DatabaseStrategy databaseStrategy;
		String databaseSystem = options.parameter(SqlDdl.class.getName(),
				SqlDdl.PARAM_DATABASE_SYSTEM);
		if (databaseSystem == null
				|| "postgresql".equalsIgnoreCase(databaseSystem)) {
			databaseStrategy = new PostgreSQLStrategy();
		} else if ("oracle".equalsIgnoreCase(databaseSystem)) {
			databaseStrategy = new OracleStrategy(result);
		} else if ("sqlserver".equalsIgnoreCase(databaseSystem)) {
			databaseStrategy = new SQLServerStrategy(result);
		} else {
			databaseStrategy = new NullDatabaseStrategy();
			result.addError(this, 100, databaseSystem);
		}

		// first up validation of common parameters
		if (mapEntryByType != null) {

			for (String type : mapEntryByType.keySet()) {

				if (mepp.hasCharacteristic(type, SqlDdl.ME_PARAM_TABLE,
						SqlDdl.ME_PARAM_TABLE_CHARACT_REP_CAT)) {

					String representedCategory = mepp.getCharacteristic(type,
							SqlDdl.ME_PARAM_TABLE,
							SqlDdl.ME_PARAM_TABLE_CHARACT_REP_CAT);

					if (representedCategory == null) {

						isValid = false;
						result.addError(this, 101, type,
								SqlDdl.ME_PARAM_TABLE_CHARACT_REP_CAT,
								SqlDdl.ME_PARAM_TABLE);

					} else if (!representedCategory.matches(
							SqlDdl.ME_PARAM_TABLE_CHARACT_REP_CAT_VALIDATION_REGEX)) {

						isValid = false;
						result.addError(this, 102, type,
								SqlDdl.ME_PARAM_TABLE_CHARACT_REP_CAT,
								SqlDdl.ME_PARAM_TABLE,
								SqlDdl.ME_PARAM_TABLE_CHARACT_REP_CAT_VALIDATION_REGEX);
					}
				}
			}
		}

		// then the database strategy specific parameters
		if (!databaseStrategy.validate(mapEntryByType, mepp)) {
			isValid = false;
		}

		isValid = checkDescriptorsForCodeList(config,options,result);


		return isValid;
	}

	private boolean checkDescriptorsForCodeList(ProcessConfiguration config,
			Options options, ShapeChangeResult result) {
		
		boolean isValid = true;

		String descriptorsForCodelistByConfig = options.parameter(
				SqlDdl.class.getName(), SqlDdl.PARAM_DESCRIPTORS_FOR_CODELIST);
		String[] descriptorsForCodelistFromConfig = new String[] {
				"documentation" };
		SortedSet<String> descriptorsForCodelist = new TreeSet<String>();

		if (descriptorsForCodelistByConfig != null) {

			if (descriptorsForCodelistByConfig.trim().isEmpty()) {
				descriptorsForCodelistFromConfig = new String[0];
			} else {
				descriptorsForCodelistFromConfig = descriptorsForCodelistByConfig
						.trim().split(",");
			}
		}
		boolean unknownDescriptorFound = false;
		String descriptorRegex = "documentation|alias|definition|description|example|legalBasis|dataCaptureStatement|primaryCode";
		for (String tmp : descriptorsForCodelistFromConfig) {

			if (tmp.matches(descriptorRegex)) {
				descriptorsForCodelist.add(tmp);
			} else {
				unknownDescriptorFound = true;
			}
		}
		if (unknownDescriptorFound) {
			result.addError(this, 2, descriptorsForCodelistByConfig,
					descriptorRegex);
			isValid = false;
		}
		if (descriptorsForCodelist.isEmpty()) {
			result.addError(this, 3);
			isValid = false;
			// irrelevant here: descriptorsForCodelist.add("documentation");
		}
		
		return isValid;
	}

	@Override
	public String message(int mnr) {

		switch (mnr) {
		case 0:
			return "Context: SqlDdl target configuration element with 'inputs'='$1$'.";
		case 1:
			return "For further details, see the documentation of parameter '$1$' on http://shapechange.net/targets/sql-ddl/";
		case 2:
			return "At least one of the identifiers in configuration parameter '"
					+ SqlDdl.PARAM_DESCRIPTORS_FOR_CODELIST
					+ "' (parameter value is '$1$') does not match the regular expression '$2$'. Correct the parameter value.";
		case 3:
			return "Configuration parameter '"
					+ SqlDdl.PARAM_DESCRIPTORS_FOR_CODELIST
					+ "' did not contain a well-known identifier. Use well-known identifiers or omit the parameter.";
		case 100:
			return "Parameter '" + SqlDdl.PARAM_DATABASE_SYSTEM
					+ "' is set to '$1$'. This is not a valid value.";
		case 101:
			return "Invalid map entry for type '$1$': no value is provided for the characteristic '$2$' of parameter '$3$'.";
		case 102:
			return "Invalid map entry for type '$1$': value provided for characteristic '$2$' of parameter '$3$' is invalid. Check that the value matches the regular expression: $4$.";

		default:
			return "(Unknown message)";
		}
	}
}
