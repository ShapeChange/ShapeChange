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
 * (c) 2002-2012 interactive instruments GmbH, Bonn, Germany
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
 * TODO How to diff elements with multiple strings as value, for example
 * {@link Info#dataCaptureStatements()} and {@link Info#examples()}?
 *
 */
public class DiffElement implements Comparable<DiffElement> {

	// type of change
	public Operation change;

	public enum Operation {
		DELETE, INSERT, CHANGE;
		public String toString() {
			switch (this) {
			case DELETE:
				return "DELETE";
			case INSERT:
				return "INSERT";
			case CHANGE:
				return "CHANGE";
			}
			return "(unknown)";
		}
	}

	// the sub-element that has changed
	public ElementType subElementType;

	public enum ElementType {
		NAME, DOCUMENTATION, MULTIPLICITY, VALUETYPE, CLASS, SUPERTYPE, SUBPACKAGE, PROPERTY, ENUM, STEREOTYPE, TAG, ALIAS, DEFINITION, DESCRIPTION, PRIMARYCODE, GLOBALIDENTIFIER, LEGALBASIS, AAAMODELLART, AAAGRUNDDATENBESTAND, AAALANDNUTZUNG, AAAGUELTIGBIS, AAARETIRED;
		public String toString() {
			switch (this) {
			case NAME:
				return "NAME";
			case DOCUMENTATION:
				return "DOCUMENTATION";
			case MULTIPLICITY:
				return "MULTIPLICITY";
			case VALUETYPE:
				return "VALUETYPE";
			case CLASS:
				return "CLASS";
			case SUPERTYPE:
				return "SUPERTYPE";
			case SUBPACKAGE:
				return "SUBPACKAGE";
			case PROPERTY:
				return "PROPERTY";
			case ENUM:
				return "ENUM";
			case STEREOTYPE:
				return "STEREOTYPE";
			case TAG:
				return "TAG";
			case ALIAS:
				return "ALIAS";
			case DEFINITION:
				return "DEFINITION";
			case DESCRIPTION:
				return "DESCRIPTION";
			case LEGALBASIS:
				return "LEGALBASIS";
			case PRIMARYCODE:
				return "PRIMARYCODE";
			case GLOBALIDENTIFIER:
				return "GLOBALIDENTIFIER";
			case AAAMODELLART:
				return "AAAMODELLART";
			case AAAGRUNDDATENBESTAND:
				return "AAAGRUNDDATENBESTAND";
			case AAALANDNUTZUNG:
				return "AAALANDNUTZUNG";
			case AAAGUELTIGBIS:
				return "AAAGUELTIGBIS";
			case AAARETIRED:
				return "AAARETIRED";
			}
			return "(unknown)";
		}
	}

	// for CHANGE and String-valued Elements
	public LinkedList<Diff> diff = null;

	// for Info-valued Elements
	public Info subElement = null;

	// for TAGs
	public String tag = null;

	@Override
	public int compareTo(DiffElement o) {

		if (this.change != o.change) {
			/* compare based on change */
			return this.change.toString().compareTo(o.change.toString());

		} else {

			if (this.subElementType != o.subElementType) {

				return this.subElementType.toString()
						.compareTo(o.subElementType.toString());
			} else {

				// compare based on subElement if not null, otherwise the tag
				if (this.subElement != null && o.subElement != null) {
					return this.subElement.compareTo(o.subElement);
				} else if (this.tag != null && o.tag != null) {
					return this.tag.compareTo(o.tag);
				} else if (this.subElement != null) {
					return -1;
				} else {
					return 1;
				}
			}
		}
	}

	/**
	 * Converts the diff list using the toString() method of each Diff.
	 * 
	 * @return textual representation of the diffs available for this list; can
	 *         be empty but not <code>null</code>
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
	 * @return textual representation of the diffs available for this list; can
	 *         be empty but not <code>null</code>
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
}
