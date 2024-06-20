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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange.Target.CDB;

import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.ShapeChange.MapEntryParamInfos;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
public class CDBAttribute {

	public enum Type {
		TEXT, NUMERIC, BOOLEAN
	};

	public enum Format {
		FLOATINGPOINT, INTEGER
	};

	protected int code;
	protected String symbol;
	protected String name;
	protected String description;
	protected Type type;
	protected Format format = null;
	protected CDBRange range = null;
	protected String length = null;
	protected CDBUnit unit = null;
	protected PropertyInfo propertyInfo;

	public CDBAttribute(PropertyInfo pi,
			MapEntryParamInfos mapEntryParamInfos) {
		super();

		propertyInfo = pi;
		String piEncodingRule = pi.encodingRule("cdb");
		Options options = pi.options();

		symbol = pi.name();
		
		name = StringUtils.defaultIfBlank(pi.aliasName(), pi.name()).trim();

		description = pi.definition();

		if (StringUtils.isNotBlank(pi.description())) {
			description = description + " [descr] " + pi.description().trim();
		}

		/*
		 * Try to find mapping for the property type. If one exists, use its
		 * target type as type, and see if it defines a specific format.
		 * Otherwise, use 'Text' as type and keep null as format.
		 * 
		 * NOTE: The check that the property type actually exists in the model
		 * and is a feature type, enumeration, or code list is performed outside
		 * of this class.
		 */
		type = Type.TEXT;
		ProcessMapEntry pme = options.targetMapEntry(pi.typeInfo().name,
				piEncodingRule);

		if (pme != null) {

			// identify type
			type = Type
					.valueOf(pme.getTargetType().toUpperCase(Locale.ENGLISH));

			// identify format
			if (mapEntryParamInfos.hasParameter(pi.typeInfo().name,
					piEncodingRule, CDB.MAPENTRY_PARAM_NUMERIC_FORMAT)) {

				Map<String, String> characteristics = mapEntryParamInfos
						.getCharacteristics(pi.typeInfo().name, piEncodingRule,
								CDB.MAPENTRY_PARAM_NUMERIC_FORMAT);

				if (characteristics != null) {

					for (String characteristic : characteristics.keySet()) {

						if (characteristic.equalsIgnoreCase("Floating-Point")
								|| characteristic
										.equalsIgnoreCase("FloatingPoint")) {

							format = Format.FLOATINGPOINT;
							break;

						} else if (characteristic.equalsIgnoreCase("Integer")) {

							format = Format.INTEGER;
							break;
						}
					}
				}
			}
		}

		// identify range
		String rangeMin = StringUtils
				.stripToNull(pi.taggedValue("rangeMinimum"));
		String rangeMax = StringUtils
				.stripToNull(pi.taggedValue("rangeMaximum"));

		if (rangeMin != null || rangeMax != null) {

			range = new CDBRange(null, rangeMin, rangeMax);
		}

		length = StringUtils.stripToNull(pi.taggedValue("length"));
	}

	/**
	 * @return the code
	 */
	public int getCode() {
		return code;
	}

	/**
	 * @return the symbol, cannot be <code>null</code>
	 */
	public String getSymbol() {
		return symbol;
	}

	/**
	 * @return the name, cannot be <code>null</code>
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the description, cannot be <code>null</code>
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @return the value unit, can be <code>null</code>
	 */
	public CDBUnit getUnit() {
		return unit;
	}

	/**
	 * @param code
	 *            the code to set
	 */
	public void setCode(int code) {
		this.code = code;
	}

	/**
	 * @param unit
	 *            the unit to set
	 */
	public void setUnit(CDBUnit unit) {
		this.unit = unit;
	}

	/**
	 * @return the value type, cannot be <code>null</code>
	 */
	public Type getType() {
		return type;
	}

	/**
	 * @return the value format, can be <code>null</code>
	 */
	public Format getFormat() {
		return format;
	}

	/**
	 * @return the value range, can be <code>null</code>
	 */
	public CDBRange getRange() {
		return range;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(Type type) {
		this.type = type;
	}

	/**
	 * @return the PropertyInfo that this attribute represents
	 */
	public PropertyInfo propertyInfo() {
		return this.propertyInfo;
	}

	/**
	 * @return <code>true</code> if a specific format is defined for this
	 *         attribute, else <code>false</code>
	 */
	public boolean hasFormat() {
		return this.format != null;
	}

	/**
	 * @return <code>true</code> if a specific range is defined for this
	 *         attribute, else <code>false</code>
	 */
	public boolean hasRange() {
		return this.range != null;
	}

	/**
	 * @return the value length, can be <code>null</code>
	 */
	public String getLength() {
		return this.length;
	}

	/**
	 * @return <code>true</code> if a specific length is defined for this
	 *         attribute, else <code>false</code>
	 */
	public boolean hasLength() {
		return this.length != null;
	}

	/**
	 * @return <code>true</code> if a specific unit is defined for this
	 *         attribute, else <code>false</code>
	 */
	public boolean hasUnit() {
		return this.unit != null;
	}
}
