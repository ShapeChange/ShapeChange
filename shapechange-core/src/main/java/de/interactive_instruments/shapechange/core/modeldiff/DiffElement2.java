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
 * (c) 2002-2022 interactive instruments GmbH, Bonn, Germany
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

package de.interactive_instruments.shapechange.core.modeldiff;

import java.util.LinkedList;

import de.interactive_instruments.shapechange.core.model.Info;
import name.fraser.neil.plaintext.diff_match_patch.Diff;

/**
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class DiffElement2 {

    public enum Operation {

	DELETE("DELETE"), INSERT("INSERT"), CHANGE("CHANGE");

	private String name;

	Operation(String name) {
	    this.name = name;
	}

	public String toString() {
	    return this.name;
	}
    }

    public enum ElementChangeType {

	SELF("SELF", false, false), NAME("NAME", false, false), DOCUMENTATION("DOCUMENTATION", false, false),
	MULTIPLICITY("MULTIPLICITY", false, false), VALUETYPE("VALUETYPE", false, false),
	INITIALVALUE("INITIALVALUE", false, false), CLASS("CLASS", false, false), SUPERTYPE("SUPERTYPE", false, false),
	SUBPACKAGE("SUBPACKAGE", false, false), PROPERTY("PROPERTY", false, false), ENUM("ENUM", false, false),
	STEREOTYPE("STEREOTYPE", true, true), TAG("TAG", true, false), ALIAS("ALIAS", false, false),
	DEFINITION("DEFINITION", false, false), DESCRIPTION("DESCRIPTION", false, false),
	PRIMARYCODE("PRIMARYCODE", false, false), GLOBALIDENTIFIER("GLOBALIDENTIFIER", false, false),
	LEGALBASIS("LEGALBASIS", false, false), DATACAPTURESTATEMENT("DATACAPTURESTATEMENT", true, false),
	EXAMPLE("EXAMPLE", true, false), LANGUAGE("LANGUAGE", false, false);

	private String name;
	private boolean isDiffForPotentiallyMultipleStringValues;
	private boolean isDiffIgnoringCase;

	ElementChangeType(String name, boolean isDiffForPotentiallyMultipleStringValues, boolean isDiffIgnoringCase) {
	    this.name = name;
	    this.isDiffForPotentiallyMultipleStringValues = isDiffForPotentiallyMultipleStringValues;
	    this.isDiffIgnoringCase = isDiffIgnoringCase;
	}

	public String toString() {
	    return this.name;
	}

	public boolean isDiffForPotentiallyMultipleStringValues() {
	    return this.isDiffForPotentiallyMultipleStringValues;
	}
	
	public boolean isDiffIgnoringCase() {
	    return this.isDiffIgnoringCase;
	}
    }

    public Operation change;

    public ElementChangeType elementChangeType;

    public LinkedList<Diff> diff = null;

    public Info sourceInfo = null;
    public Info targetInfo = null;
    public Info subElement = null;

    public String tag = null;

    /**
     * @param change            - identifies the change operation
     * @param elementChangeType - identifies the type of the element change
     * @param diff              - for CHANGE and String-valued Elements; can be
     *                          <code>null</code>
     * @param sourceInfo        - reference to source model element; can be
     *                          <code>null</code> (sourceInfo and/or targetInfo must
     *                          be set)
     * @param targetInfo        - reference to target model element; can be
     *                          <code>null</code> (sourceInfo and/or targetInfo must
     *                          be set)
     * @param tag               - tag name if elementChangeType=TAG; can be
     *                          <code>null</code>
     * @param subElement        - for Info-valued Elements; can be <code>null</code>
     */
    public DiffElement2(Operation change, ElementChangeType elementChangeType, LinkedList<Diff> diff, Info sourceInfo,
	    Info targetInfo, String tag, Info subElement) {
	super();
	this.change = change;
	this.elementChangeType = elementChangeType;
	this.diff = diff;
	this.sourceInfo = sourceInfo;
	this.targetInfo = targetInfo;
	this.tag = tag;
	this.subElement = subElement;
    }

    /**
     * Converts the diff list using the toString() method of each Diff.
     * 
     * @return textual representation of the diffs available for this list; can be
     *         empty but not <code>null</code>
     */
    public String diff_toString() {

	StringBuilder result = new StringBuilder();

	if (this.diff != null) {

	    for (Diff aDiff : diff) {

		switch (aDiff.operation) {
		case INSERT:
		    result.append(aDiff.toString());
		    break;
		case DELETE:
		    result.append(aDiff.toString());
		    break;
		case EQUAL:
		    result.append(aDiff.text);
		    break;
		}
	    }
	}

	return result.toString();
    }

    /**
     * Converts the diff list to a human readable text. The result shows the
     * reference text (FROM) and the input text (TO).
     * 
     * @return textual representation of the diffs available for this list; can be
     *         empty but not <code>null</code>
     */
    public String diff_from_to() {

	StringBuilder result = new StringBuilder();

	if (this.diff != null) {

	    StringBuilder from = new StringBuilder();

	    StringBuilder to = new StringBuilder();

	    for (Diff aDiff : diff) {

		switch (aDiff.operation) {
		case INSERT:
		    to.append(aDiff.text);
		    break;
		case DELETE:
		    from.append(aDiff.text);
		    break;
		case EQUAL:
		    from.append(aDiff.text);
		    to.append(aDiff.text);
		    break;
		}
	    }

	    result.append("FROM: ");
	    result.append(from);
	    result.append(" TO: ");
	    result.append(to);
	}

	return result.toString();
    }

    public String diff_from() {

	StringBuilder result = new StringBuilder();

	if (this.diff != null) {

	    for (Diff aDiff : diff) {

		switch (aDiff.operation) {
		case INSERT:
		    // nothing to do (insert irrelevant for from)
		    break;
		case DELETE:
		    result.append(aDiff.text);
		    break;
		case EQUAL:
		    result.append(aDiff.text);
		    break;
		}
	    }
	}

	return result.toString();
    }

    public String diff_to() {

	StringBuilder result = new StringBuilder();

	if (this.diff != null) {

	    for (Diff aDiff : diff) {

		switch (aDiff.operation) {
		case INSERT:
		    result.append(aDiff.text);
		    break;
		case DELETE:
		    // nothing to do (delete irrelevant for to)
		    break;
		case EQUAL:
		    result.append(aDiff.text);
		    break;
		}
	    }
	}

	return result.toString();
    }
}
