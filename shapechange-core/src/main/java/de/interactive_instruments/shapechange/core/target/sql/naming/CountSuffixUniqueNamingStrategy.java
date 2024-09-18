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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.shapechange.core.target.sql.naming;

import java.util.HashSet;
import java.util.Set;

import de.interactive_instruments.shapechange.core.MessageSource;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
public class CountSuffixUniqueNamingStrategy
		implements UniqueNamingStrategy, MessageSource {

	private Set<String> names = new HashSet<String>();
	private ShapeChangeResult result;

	public CountSuffixUniqueNamingStrategy(ShapeChangeResult result) {
		this.result = result;
	}

	@Override
	public String makeUnique(String proposedName) {

		String newProposedName = proposedName;

		/*
		 * make name unique by adding a number to it and testing again for
		 * uniqueness
		 */
		int suffix = 0;
		String suffixAsString = String.valueOf(suffix);

		while (names.contains(newProposedName) && suffix <= 9999
				&& suffixAsString.length() <= newProposedName.length() - 1) {

			newProposedName = newProposedName.substring(0,
					newProposedName.length() - suffixAsString.length())
					+ suffixAsString;

			suffix++;
			suffixAsString = String.valueOf(suffix);
		}

		// final check
		if (names.contains(newProposedName)) {

			result.addError(this, 1, proposedName, newProposedName);
			return proposedName;

		} else {

			names.add(newProposedName);
			return newProposedName;
		}
	}

	@Override
	public String message(int mnr) {
		switch (mnr) {
		case 1:
			return "Could not make name '$1$' unique. Last possible name '$2$' is already taken.";
		default:
			return "(" + CountSuffixUniqueNamingStrategy.class.getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
