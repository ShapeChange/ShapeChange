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
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */

package de.interactive_instruments.ShapeChange.ModelDiff;

import java.util.LinkedList;

import de.interactive_instruments.ShapeChange.Model.Info;

import name.fraser.neil.plaintext.diff_match_patch.Diff;

public class DiffElement {

	// type of change
	public Operation change;
	
	public enum Operation {
		DELETE, INSERT, CHANGE;
		public String toString() {
			switch (this) {
			case DELETE: return "DELETE";
			case INSERT: return "INSERT";
			case CHANGE: return "CHANGE";
			}
			return "(unknown)";
		}
	}

	// the sub-element that has changed
	public ElementType subElementType;
	
	public enum ElementType {
		NAME, DOCUMENTATION, MULTIPLICITY, VALUETYPE, CLASS, SUPERTYPE, SUBPACKAGE, PROPERTY, ENUM, STEREOTYPE, TAG, ALIAS, DEFINITION, DESCRIPTION, AAAMODELLART, AAAGRUNDDATENBESTAND;
		public String toString() {
			switch (this) {
			case NAME: return "NAME";
			case DOCUMENTATION: return "DOCUMENTATION";
			case MULTIPLICITY: return "MULTIPLICITY";
			case VALUETYPE: return "VALUETYPE";
			case CLASS: return "CLASS";
			case SUPERTYPE: return "SUPERTYPE";
			case SUBPACKAGE: return "SUBPACKAGE";
			case PROPERTY: return "PROPERTY";
			case ENUM: return "ENUM";
			case STEREOTYPE: return "STEREOTYPE";
			case TAG: return "TAG";
			case ALIAS: return "ALIAS";
			case DEFINITION: return "DEFINITION";
			case DESCRIPTION: return "DESCRIPTION";
			case AAAMODELLART: return "AAAMODELLART";
			case AAAGRUNDDATENBESTAND: return "AAAGRUNDDATENBESTAND";
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
}
