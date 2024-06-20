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
 * (c) 2002-2014 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Util.ea;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import de.interactive_instruments.ShapeChange.Model.TaggedValues;

/**
 * @author Johannes Echterhoff
 *
 */
public class EATaggedValue {

	protected String name;
	protected String fqName = "";
	protected List<String> values;

	/**
	 * If set to <code>true</code>, the values shall be encoded using
	 * &lt;memo&gt; fields, regardless of the actual length of each value.
	 */
	protected boolean createAsMemoField = false;

	/**
	 * @param name tbd
	 * @param value
	 *            can be <code>null</code> (then the list of values would be
	 *            empty)
	 */
	public EATaggedValue(String name, String value) {

		this(name, value, false);
	}

	/**
	 * @param name tbd
	 * @param fqName tbd
	 * @param value
	 *            can be <code>null</code> (then the list of values would be
	 *            empty)
	 */
	public EATaggedValue(String name, String fqName, String value) {

		this(name, fqName, value, false);
	}

	/**
	 * @param name tbd
	 * @param value
	 *            can be <code>null</code> (then the list of values would be
	 *            empty)
	 * @param createAsMemoField
	 *            set to <code>true</code> if the value shall be encoded using a
	 *            &lt;memo&gt; field, regardless of the value length
	 */
	public EATaggedValue(String name, String value, boolean createAsMemoField) {

		this(name, null, value, createAsMemoField);
	}

	/**
	 * @param name tbd
	 * @param fqName tbd
	 * @param value
	 *            can be <code>null</code> (then the list of values would be
	 *            empty)
	 * @param createAsMemoField
	 *            set to <code>true</code> if the value shall be encoded using a
	 *            &lt;memo&gt; field, regardless of the value length
	 */
	public EATaggedValue(String name, String fqName, String value,
			boolean createAsMemoField) {

		super();

		this.name = name;
		this.fqName = fqName == null ? "" : fqName;

		this.values = new ArrayList<String>();
		if (value != null) {
			this.values.add(value);
		}

		this.createAsMemoField = createAsMemoField;
	}

	/**
	 * @param name tbd
	 * @param values
	 *            can be <code>null</code> (then the list of values would be
	 *            empty)
	 */
	public EATaggedValue(String name, List<String> values) {

		this(name, null, values);
	}

	/**
	 * @param name tbd
	 * @param fqName tbd
	 * @param values
	 *            can be <code>null</code> (then the list of values would be
	 *            empty)
	 */
	public EATaggedValue(String name, String fqName, List<String> values) {

		super();

		this.name = name;
		this.fqName = fqName == null ? "" : fqName;

		if (values != null) {
			this.values = values;
		} else {
			this.values = new ArrayList<String>();
		}
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the FQName, can be empty but not <code>null</code>
	 */
	public String getFQName() {
		return fqName;
	}

	/**
	 * @return the values; can be empty but not <code>null</code>
	 */
	public List<String> getValues() {
		return values;
	}

	public boolean createAsMemoField() {
		return createAsMemoField;
	}

	public void addValue(String value) {
		this.values.add(value);
	}

	public static List<EATaggedValue> fromTaggedValues(
			TaggedValues taggedValues) {

		List<EATaggedValue> result = new ArrayList<EATaggedValue>();

		for (Entry<String, List<String>> e : taggedValues.asMap().entrySet()) {

			result.add(new EATaggedValue(e.getKey(), e.getValue()));
		}

		return result;
	}

}
