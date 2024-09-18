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
 * (c) 2002-2023 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core.target.json;

import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class EncodingRestrictions {

    protected boolean memberRequired = false;
    protected SortedSet<String> memberTypeRestrictions = new TreeSet<>();
    protected SortedSet<String> memberFormatRestrictions = new TreeSet<>();

    /**
     * @return the memberRequired
     */
    public boolean isMemberRequired() {
	return this.memberRequired;
    }

    /**
     * @param memberRequired the memberRequired to set
     */
    public void setMemberRequired(boolean memberRequired) {
	this.memberRequired = memberRequired;
    }

    /**
     * @return the memberTypeRestrictions; can be empty but not <code>null</code>
     */
    public SortedSet<String> getMemberTypeRestrictions() {
	return this.memberTypeRestrictions;
    }

    /**
     * @param memberTypeRestrictions the memberTypes to set
     */
    public void setMemberTypeRestrictions(SortedSet<String> memberTypeRestrictions) {
	if (memberTypeRestrictions == null) {
	    this.memberTypeRestrictions = new TreeSet<>();
	} else {
	    this.memberTypeRestrictions = memberTypeRestrictions;
	}
    }

    public void addMemberTypeRestriction(String memberTypeRestriction) {
	this.memberTypeRestrictions.add(memberTypeRestriction);
    }

    /**
     * @return the memberFormatRestrictions; can be empty but not <code>null</code>
     */
    public SortedSet<String> getMemberFormatRestrictions() {
	return memberFormatRestrictions;
    }

    /**
     * @param memberFormatRestrictions the memberFormatRestrictions to set
     */
    public void setMemberFormatRestrictions(SortedSet<String> memberFormatRestrictions) {
	if (memberFormatRestrictions == null) {
	    this.memberFormatRestrictions = new TreeSet<>();
	} else {
	    this.memberFormatRestrictions = memberFormatRestrictions;
	}
    }

    public void addMemberFormatRestriction(String memberFormatRestriction) {
	this.memberFormatRestrictions.add(memberFormatRestriction);
    }

    /**
     * @param s the string that contains the encoding restrictions
     * @return the encoding restrictions parsed from the given string;
     *         <code>null</code>, if the string is blank
     * @throws IllegalArgumentException if the format of the encoding restrictions
     *                                  is not as expected
     */
    public static EncodingRestrictions from(String s) throws IllegalArgumentException {

	if (StringUtils.isBlank(s)) {
	    throw new IllegalArgumentException("String with encoding restrictions is blank.");
	}

	EncodingRestrictions encRestriction = new EncodingRestrictions();

	String[] restrictions = s.split("\\s*;\\s*");

	for (String rtmp : restrictions) {

	    String restriction = rtmp.trim();
	    String[] rParts = restriction.split("\\s*=\\s*");

	    if (rParts.length == 0 || rParts.length > 2 || StringUtils.isBlank(rParts[0])) {
		throw new IllegalArgumentException("String with encoding restrictions contains invalid part: "
			+ StringUtils.defaultIfBlank(restriction, "<blank value>"));
	    }

	    if (rParts[0].equalsIgnoreCase("memberRequired")) {
		encRestriction.setMemberRequired(true);
	    } else if (rParts[0].equalsIgnoreCase("typeRestriction")) {
		if (rParts.length == 1 || StringUtils.isBlank(rParts[1])) {
		    throw new IllegalArgumentException("Invalid value for restriction " + rParts[0]);
		}

		for (String typeTmp : rParts[1].split("\\s*,\\s*")) {
		    String type = typeTmp.toLowerCase(Locale.ENGLISH).trim();
		    if (StringUtils.equalsAny(type, "integer", "number", "string", "boolean")) {
			encRestriction.addMemberTypeRestriction(type);
		    } else {
			throw new IllegalArgumentException(
				"Invalid value for restriction " + rParts[0] + ": " + rParts[1]);
		    }
		}
	    } else if (rParts[0].equalsIgnoreCase("formatRestriction")) {
		if (rParts.length == 1 || StringUtils.isBlank(rParts[1])) {
		    throw new IllegalArgumentException("Invalid value for restriction " + rParts[0]);
		}

		for (String formatTmp : rParts[1].split("\\s*,\\s*")) {
		    String format = formatTmp.trim();
		    if (StringUtils.isNotBlank(format)) {
			encRestriction.addMemberFormatRestriction(format);
		    } else {
			throw new IllegalArgumentException(
				"Invalid value for characteristic " + rParts[0] + ": " + rParts[1]);
		    }
		}
	    } else {
		throw new IllegalArgumentException("Unknown restriction " + restriction);
	    }
	}

	return encRestriction;
    }

}
