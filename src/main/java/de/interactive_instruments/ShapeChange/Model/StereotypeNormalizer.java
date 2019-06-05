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
 * (c) 2002-2019 interactive instruments GmbH, Bonn, Germany
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

import de.interactive_instruments.ShapeChange.Options;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments <dot>
 *         de)
 *
 */
public class StereotypeNormalizer {

	/**
	 * Normalizes the given stereotypes based upon configured stereotype
	 * aliases, and maps them to the well-known ones defined for the given
	 * {@link Info} object. If a stereotype does not map to a well-known
	 * stereotype, it will be ignored (and thus not contained in the result).
	 * 
	 * @param stereotypes
	 *                        The stereotypes to normalize and map.
	 * @param infoObject
	 *                        The object for which the stereotypes are defined.
	 *                        Needed to identify the type of object, so that
	 *                        mapping to the well-known stereotypes defined for
	 *                        that type can occur.
	 * @return Stereotypes cache with normalized and mapped stereotypes. Can be
	 *         empty but not <code>null</code>.
	 */
	public static Stereotypes normalizeAndMapToWellKnownStereotype(
			String[] stereotypes, Info infoObject) {

		Options options = infoObject.options();

		Stereotypes result = options.stereotypesFactory();

		for (String stereotype : stereotypes) {

			String st = options.normalizeStereotype(stereotype.trim());

			if (st != null) {

				String[] wellKnownStereotypes;

				if (infoObject instanceof PropertyInfo) {
					wellKnownStereotypes = Options.propertyStereotypes;
				} else if (infoObject instanceof ClassInfo) {
					wellKnownStereotypes = Options.classStereotypes;
				} else if (infoObject instanceof PackageInfo) {
					wellKnownStereotypes = Options.packageStereotypes;
				} else if (infoObject instanceof AssociationInfo) {
					wellKnownStereotypes = Options.assocStereotypes;
				} else {
					wellKnownStereotypes = new String[] {};
				}

				for (String s : wellKnownStereotypes) {
					if (st.toLowerCase().equals(s))
						result.add(s);
				}
			}
		}

		return result;
	}

}
