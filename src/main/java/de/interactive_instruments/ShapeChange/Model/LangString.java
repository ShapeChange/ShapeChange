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
package de.interactive_instruments.ShapeChange.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stores a string value and optional language identifier.
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot
 *         de)
 *
 */
public class LangString implements Comparable<LangString> {

	private static final Pattern langPattern = Pattern
			.compile("^\"(.*)\"@([a-zA-Z0-9\\-]{2,})$");

	private String value = null;
	private String lang = null;

	/**
	 * @param string
	 *                   - must not be <code>null</code>
	 * @return the LangString parsed from the given string
	 */
	public static LangString parse(String string) {

		Matcher m = langPattern.matcher(string);

		if (m.matches()) {
			String text = m.group(1);
			String lang = m.group(2);
			return new LangString(text, lang);
		} else {
			return new LangString(string);
		}
	}

	/**
	 * @param strings tbd
	 * @return list of parsed language tagged strings, can be empty but not
	 *         <code>null</code>
	 */
	public static List<LangString> parse(String[] strings) {

		List<LangString> result = new ArrayList<LangString>();

		if (strings != null && strings.length != 0) {

			for (String s : strings) {

				if (s != null) {

					result.add(parse(s));
				}
			}
		}

		return result;
	}

	/**
	 * @param value
	 *                  can be <code>null</code>
	 * @param lang
	 *                  can be <code>null</code>
	 */
	public LangString(String value, String lang) {
		super();
		if (value != null) {
			this.value = value.intern();
		}
		if (lang != null) {
			this.lang = lang.intern();
		}
	}

	/**
	 * @param value
	 *                  can be <code>null</code>
	 */
	public LangString(String value) {
		super();
		if (value != null) {
			this.value = value.intern();
		}
	}

	/**
	 * @return the value, can be <code>null</code>
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @param value
	 *                  the value to set
	 */
	public void setValue(String value) {
		if (value != null) {
			this.value = value.intern();
		} else {
			this.value = value;
		}
	}

	/**
	 * @return the lang
	 */
	public String getLang() {
		return lang;
	}

	/**
	 * @param lang
	 *                 the lang to set
	 */
	public void setLang(String lang) {
		if (lang != null) {
			this.lang = lang.intern();
		} else {
			this.lang = lang;
		}
	}

	/**
	 * @return <code>true</code> if lang is not <code>null</code> and has a
	 *         length greater than 0.
	 */
	public boolean hasLang() {
		return this.lang != null && this.lang.length() > 0;
	}

	/**
	 * @return <code>true</code> if value is not <code>null</code> and has a
	 *         length greater than 0.
	 */
	public boolean hasValue() {
		return this.getValue() != null && this.getValue().length() > 0;
	}

	public String toString() {
		StringBuffer result = new StringBuffer();
		if (hasLang()) {
			result.append("\"");
		}
		result.append(this.value);
		if (hasLang()) {
			result.append("\"@");
			result.append(this.lang);
		}
		return result.toString();
	}

	public void appendSuffix(String s) {
		this.value += s;
	}

	/**
	 * Sorts first by comparing the language value, then by comparing the actual
	 * value.
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(LangString other) {

		if (!this.hasLang() && other.hasLang()) {
			return -1;
		} else if (this.hasLang() && !other.hasLang()) {
			return 1;
		} else if (this.hasLang() && other.hasLang()
				&& this.getLang().compareTo(other.getLang()) != 0) {
			return this.getLang().compareTo(other.getLang());
		} else {
			// lang is equal
			if (!this.hasValue() && !other.hasValue()) {
				return 0;
			} else if (!this.hasValue() && other.hasValue()) {
				return -1;
			} else if (this.hasValue() && !other.hasValue()) {
				return 1;
			} else {
				return this.getValue().compareTo(other.getValue());
			}
		}
	}
}
