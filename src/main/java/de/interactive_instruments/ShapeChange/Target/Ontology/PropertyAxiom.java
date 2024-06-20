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
 * (c) 2002-2018 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target.Ontology;

import java.util.Locale;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot
 *         de)
 *
 */
public enum PropertyAxiom {

	SUBPROPERTYOF("subPropertyOf"), EQUIVALENT("equivalent"), DISJOINT(
			"disjoint"), INVERSE("inverse"), FUNCTIONAL(
					"functional"), INVERSEFUNCTIONAL(
							"inversefunctional"), REFLEXIVE(
									"reflexive"), IRREFLEXIVE(
											"irreflexive"), SYMMETRIC(
													"symmetric"), ASYMMETRIC(
															"asymmetric"), TRANSITIVE(
																	"transitive");
	private String nameForMessages;

	PropertyAxiom(String nameForMessages) {
		this.nameForMessages = nameForMessages;
	}
	
	public String getNameForMessages() {
		return this.nameForMessages;
	}

	public static PropertyAxiom fromString(String s) {

		PropertyAxiom result = null;

		try {
			result = PropertyAxiom.valueOf(s.trim().replaceAll("[^a-zA-Z]", "")
					.toUpperCase(Locale.ENGLISH));
		} catch (IllegalArgumentException e1) {
			// given String s does not match any of the defined enum values
			System.err.println(e1);
		} catch (NullPointerException e2) {
			// given String s is null
			System.err.println(e2);
		}

		return result;
	}

}
