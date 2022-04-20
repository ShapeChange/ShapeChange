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
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */

package de.interactive_instruments.ShapeChange.ModelDiff;

import java.util.LinkedList;

import de.interactive_instruments.ShapeChange.Model.Info;
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

	SELF("SELF", false), NAME("NAME", false), DOCUMENTATION("DOCUMENTATION", false),
	MULTIPLICITY("MULTIPLICITY", false), VALUETYPE("VALUETYPE", false), INITIALVALUE("INITIALVALUE", false),
	CLASS("CLASS", false), SUPERTYPE("SUPERTYPE", false), SUBPACKAGE("SUBPACKAGE", false),
	PROPERTY("PROPERTY", false), ENUM("ENUM", false), STEREOTYPE("STEREOTYPE", true), TAG("TAG", true),
	ALIAS("ALIAS", false), DEFINITION("DEFINITION", false), DESCRIPTION("DESCRIPTION", false),
	PRIMARYCODE("PRIMARYCODE", false), GLOBALIDENTIFIER("GLOBALIDENTIFIER", false), LEGALBASIS("LEGALBASIS", false),
	DATACAPTURESTATEMENT("DATACAPTURESTATEMENT", true), EXAMPLE("EXAMPLE", true), LANGUAGE("LANGUAGE", false)
//	, AAAMODELLART("AAAMODELLART", true), AAAGRUNDDATENBESTAND("AAAGRUNDDATENBESTAND", true),
//	AAALANDNUTZUNG("AAALANDNUTZUNG", false), AAARETIRED("AAARETIRED", false)
	;

	private String name;
	private boolean isDiffForPotentiallyMultipleStringValues;

	ElementChangeType(String name, boolean isDiffForPotentiallyMultipleStringValues) {
	    this.name = name;
	    this.isDiffForPotentiallyMultipleStringValues = isDiffForPotentiallyMultipleStringValues;
	}

	public String toString() {
	    return this.name;
	}

	public boolean isDiffForPotentiallyMultipleStringValues() {
	    return this.isDiffForPotentiallyMultipleStringValues;
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
