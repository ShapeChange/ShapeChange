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
package de.interactive_instruments.ShapeChange.Target.SQL;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.interactive_instruments.ShapeChange.MapEntryParamInfos;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;

public class OracleStrategy implements DatabaseStrategy, MessageSource {

	public static final String GEOM_PARAM_LAYER_GTYPE = "layer_gtype";
	/**
	 * Regular expression
	 * (?i:(POINT|LINE|POLYGON|COLLECTION|MULTIPOINT|MULTILINE|MULTIPOLYGON)) to
	 * check that a given string is one of a list of allowed values (NOTE: check
	 * is case-insensitive).
	 */
	public static final String GEOM_PARAM_LAYER_GTYPE_VALIDATION_REGEX = "(?i:(POINT|LINE|POLYGON|COLLECTION|MULTIPOINT|MULTILINE|MULTIPOLYGON))";

	private DatabaseObjectNamingScheme namingScheme;
	private ShapeChangeResult result;

	public OracleStrategy(DatabaseObjectNamingScheme namingScheme, ShapeChangeResult result) {
		this.namingScheme = namingScheme;
		this.result = result;
	}

	@Override
	public String primaryKeyDataType() {
		return "INTEGER";
	}

	@Override
	public String geometryDataType(ProcessMapEntry me, int srid) {
		return me.getTargetType();
	}

	@Override
	public String unlimitedLengthCharacterDataType() {
		return "CLOB";
	}

	@Override
	public String limitedLengthCharacterDataType(int size) {
		return "VARCHAR2(" + size + ")";
	}

	@Override
	public String geometryIndexColumnPart(String columnname,
			Map<String, String> geometryCharacteristics) {

		String res = " (" + columnname + ") INDEXTYPE IS MDSYS.SPATIAL_INDEX";

		if (geometryCharacteristics != null && geometryCharacteristics
				.containsKey(GEOM_PARAM_LAYER_GTYPE)) {

			String layergtype = geometryCharacteristics
					.get(GEOM_PARAM_LAYER_GTYPE);

			if (layergtype != null) {
				res = res + " PARAMETERS('layer_gtype="
						+ geometryCharacteristics.get(GEOM_PARAM_LAYER_GTYPE)
						+ "')";
			} else {

				/*
				 * Missing characteristic value should have been reported during
				 * validation of the map entry parameter infos.
				 */
			}
		}

		return res;
	}

	@Override
	public String geometryMetadataUpdateStatement(String normalizedClassName,
			String columnname, int srid) {
		String s = "INSERT INTO USER_SDO_GEOM_METADATA (TABLE_NAME, COLUMN_NAME, DIMINFO, SRID) VALUES ('"
				+ normalizedClassName.toUpperCase(Locale.ENGLISH) + "', '"
				+ columnname.toUpperCase(Locale.ENGLISH)
				+ "', MDSYS.SDO_DIM_ARRAY(FIXME)" + ", " + srid + ")";
		return s;
	}

	@Override
	public String normalizeName(String name) {
		return namingScheme.normalizeName(name);
	}

	/**
	 * Constraints in Oracle are in their own namespace and do also have the maximum length of 30.
	 * 
	 */
	@Override
	public String createNameCheckConstraint(String tableName, String propertyName, Set<String> allConstraintNames) {
		return namingScheme.createNameCheckConstraint(tableName, propertyName, allConstraintNames);
	}

	@Override
	public void validate(Map<String, ProcessMapEntry> mapEntryByType,
			MapEntryParamInfos mepp) {

		if (mapEntryByType != null) {

			for (String type : mapEntryByType.keySet()) {

				// ensure that layer_gtype has a value and that it is one of the
				// allowed ones
				if (mepp.hasCharacteristic(type, SqlDdl.ME_PARAM_GEOMETRY,
						GEOM_PARAM_LAYER_GTYPE)) {

					String layergtype = mepp.getCharacteristic(type,
							SqlDdl.ME_PARAM_GEOMETRY, GEOM_PARAM_LAYER_GTYPE);

					if (layergtype == null) {

						result.addError(this, 3, type, GEOM_PARAM_LAYER_GTYPE,
								SqlDdl.ME_PARAM_GEOMETRY);

					} else if (!layergtype
							.matches(GEOM_PARAM_LAYER_GTYPE_VALIDATION_REGEX)) {

						result.addError(this, 4, type, GEOM_PARAM_LAYER_GTYPE,
								SqlDdl.ME_PARAM_GEOMETRY,
								GEOM_PARAM_LAYER_GTYPE_VALIDATION_REGEX);
					} else {
						// fine - no further tests at this point in time
					}
				}
			}
		}
	}
	
	@Override
	public String createNameForeignKey(String tableName, String targetTableName, String fieldName, Set<String> allConstraintNames) {
		return namingScheme.createNameForeignKey(tableName, targetTableName, fieldName, allConstraintNames);
	}


	@Override
	public String message(int mnr) {
		switch (mnr) {
		case 0:
			return "Context: class OracleStrategy";
		case 3:
			return "Invalid map entry for type '$1$': no value is provided for the characteristic '$2$' of parameter '$3$'.";
		case 4:
			return "Invalid map entry for type '$1$': value provided for characteristic '$2$' of parameter '$3$' is invalid. Check that the value matches the regular expression: $4$.";
		default:
			return "(Unknown message)";
		}
	}

}
