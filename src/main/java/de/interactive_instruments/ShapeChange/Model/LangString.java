package de.interactive_instruments.ShapeChange.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stores a string value and optional language identifier.
 * 
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class LangString {

	private static final Pattern langPattern = Pattern
			.compile("^\"(.*)\"@([a-zA-Z0-9\\-]{2,})$");

	private String value;
	private String lang;

	/**
	 * @param string
	 *            - must not be <code>null</code>
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
	 * @param strings
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
	 * @param lang
	 */
	public LangString(String value, String lang) {
		super();
		this.value = value;
		this.lang = lang;
	}

	/**
	 * @param value
	 * @param lang
	 */
	public LangString(String value) {
		super();
		this.value = value;
		this.lang = null;
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @param value
	 *            the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * @return the lang
	 */
	public String getLang() {
		return lang;
	}

	/**
	 * @param lang
	 *            the lang to set
	 */
	public void setLang(String lang) {
		this.lang = lang;
	}

	/**
	 * @return <code>true</code> if lang is not null and has a length greater
	 *         than 0.
	 */
	public boolean hasLang() {
		return this.lang != null && this.lang.length() > 0;
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
}
