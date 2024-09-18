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
 * (c) 2002-2024 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core.model;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class DescriptorAndTagResolver {

    /**
     * group 0: the whole input string group 1: the string to use as separator of
     * multiple values; can be <code>null</code> group 2: the name of the tagged
     * value; cannot be <code>null</code>
     */
    public static final Pattern taggedValuePattern = Pattern.compile("TV(\\(.+?\\))?:(.+)");

    /**
     * For a given model element, look up the value(s) of a certain descriptor or
     * tag, and add them to the given list.
     * 
     * @param i                                 the model element in which to
     *                                          resolve descriptor or tag values
     * @param descriptorOrTaggedValueIdentifier string with the name of a descriptor
     *                                          (case will be ignored), or a string
     *                                          for identifying a tagged value
     *                                          (using the
     *                                          {{@link #taggedValuePattern} syntax)
     * @param language                          language to search tag values in
     * @param values                            list, to which the values found for
     *                                          the given tag or descriptor will be
     *                                          added; if no values were found, the
     *                                          list will not be modified
     * @return true, if the descriptor was recognized (tags will always be
     *         recognized)
     */
    public static boolean resolveDescriptorOrTag(Info i, String descriptorOrTaggedValueIdentifier, String language,
	    List<String> values) {

	boolean descRecognized = true;

	if (descriptorOrTaggedValueIdentifier.startsWith("TV")) {

	    Matcher m = taggedValuePattern.matcher(descriptorOrTaggedValueIdentifier);

	    /*
	     * validation of the configuration should ensure that desc matches
	     */
	    m.matches();
	    String separator = m.group(1);
	    String tv = m.group(2);

	    String[] tv_values = i.taggedValuesInLanguage(tv, language);

	    if (separator != null) {
		// exclude leading "(" and trailing ")"
		separator = separator.substring(1, separator.length() - 1);
		/*
		 * match the string separator as if it were a literal pattern
		 */
		String quoted_separator = Pattern.quote(separator);

		for (String tv_value : tv_values) {
		    String[] split = tv_value.split(quoted_separator);
		    for (String s : split) {
			if (!s.trim().isEmpty()) {
			    values.add(s.trim());
			}
		    }
		}
	    } else {
		for (String tv_value : tv_values) {
		    if (!tv_value.trim().isEmpty()) {
			values.add(tv_value.trim());
		    }
		}
	    }

	} else if (descriptorOrTaggedValueIdentifier.equalsIgnoreCase("name")) {

	    values.add(i.name());

	} else if (descriptorOrTaggedValueIdentifier.equalsIgnoreCase("alias")) {

	    String s = i.aliasName();
	    if (s != null && !s.trim().isEmpty()) {
		values.add(s);
	    }

	} else if (descriptorOrTaggedValueIdentifier.equalsIgnoreCase("documentation")) {

	    String s = i.documentation();
	    if (s != null && !s.trim().isEmpty()) {
		values.add(s);
	    }

	} else if (descriptorOrTaggedValueIdentifier.equalsIgnoreCase("definition")) {

	    String s = i.definition();
	    if (s != null && !s.trim().isEmpty()) {
		values.add(s);
	    }

	} else if (descriptorOrTaggedValueIdentifier.equalsIgnoreCase("description")) {

	    String s = i.description();
	    if (s != null && !s.trim().isEmpty()) {
		values.add(s);
	    }

	} else if (descriptorOrTaggedValueIdentifier.equalsIgnoreCase("example")) {

	    String[] s = i.examples();
	    if (s != null && s.length > 0) {
		for (String ex : s) {
		    if (ex.trim().length() > 0) {
			values.add(ex.trim());
		    }
		}
	    }

	} else if (descriptorOrTaggedValueIdentifier.equalsIgnoreCase("legalBasis")) {

	    String s = i.legalBasis();
	    if (s != null && !s.trim().isEmpty()) {
		values.add(s);
	    }

	} else if (descriptorOrTaggedValueIdentifier.equalsIgnoreCase("dataCaptureStatement")) {

	    String[] s = i.dataCaptureStatements();
	    if (s != null && s.length > 0) {
		for (String ex : s) {
		    if (ex.trim().length() > 0) {
			values.add(ex.trim());
		    }
		}
	    }

	} else if (descriptorOrTaggedValueIdentifier.equalsIgnoreCase("primaryCode")) {

	    String s = i.primaryCode();
	    if (s != null && !s.trim().isEmpty()) {
		values.add(s);
	    }

	} else if (descriptorOrTaggedValueIdentifier.equalsIgnoreCase("globalIdentifier")) {

	    String s = i.globalIdentifier();
	    if (s != null && !s.trim().isEmpty()) {
		values.add(s);
	    }

	} else {
	    /*
	     * the field in the template neither identifies a known descriptor nor a tagged
	     * value
	     */
	    descRecognized = false;
	}

	return descRecognized;
    }
}
