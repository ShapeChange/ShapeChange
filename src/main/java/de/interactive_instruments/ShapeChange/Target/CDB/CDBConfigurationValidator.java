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
 * (c) 2002-2017 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target.CDB;

import java.util.SortedMap;
import java.util.TreeMap;

import de.interactive_instruments.ShapeChange.ConfigurationValidator;
import de.interactive_instruments.ShapeChange.MapEntryParamInfos;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessConfiguration;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
public class CDBConfigurationValidator
		implements ConfigurationValidator, MessageSource {

	@Override
	public boolean isValid(ProcessConfiguration config, Options options,
			ShapeChangeResult result) {

		boolean isValid = true;

		SortedMap<String, ProcessMapEntry> mapEntryByType = new TreeMap<String, ProcessMapEntry>();

		for (ProcessMapEntry pme : config.getMapEntries()) {
			mapEntryByType.put(pme.getType(), pme);
		}

		// general validation of map entry parameters
		MapEntryParamInfos mepis = new MapEntryParamInfos(result,
				mapEntryByType.values());

		isValid = isValid && mepis.isValid();

		/*
		 * check that all map entries have a targetType and that it is either
		 * 'Text', 'Numeric', or 'Boolean'
		 */
		for (ProcessMapEntry pme : mapEntryByType.values()) {

			if (!(pme.hasTargetType()
					|| pme.getTargetType().equalsIgnoreCase("Text")
					|| pme.getTargetType().equalsIgnoreCase("Numeric")
					|| pme.getTargetType().equalsIgnoreCase("Boolean"))) {
				result.addError(this, 100, pme.getType(), pme.getRule());
				isValid = false;
			}
		}

		return isValid;
	}

	@Override
	public String message(int mnr) {

		switch (mnr) {

		// 100-199 messages related to map entries
		case 100:
			return "The target type of map entry with type '$1$' and rule '$2$' is invalid. It must be one of (ignoring case): Text, Numeric, Boolean.";

		default:
			return "(" + this.getClass().getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
