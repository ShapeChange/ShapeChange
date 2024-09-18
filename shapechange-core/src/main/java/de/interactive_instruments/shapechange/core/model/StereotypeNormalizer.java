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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.shapechange.core.model;

import java.util.HashSet;
import java.util.Set;

import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot
 *         de)
 *
 */
public class StereotypeNormalizer {

    /**
     * Normalizes the given stereotypes based upon configured stereotype aliases,
     * and maps them to the well-known ones defined for the given {@link Info}
     * object. If a stereotype does not map to a well-known stereotype, it will be
     * ignored (and thus not contained in the result).
     * 
     * @param stereotypes The stereotypes to normalize and map.
     * @param infoObject  The object for which the stereotypes are defined. Needed
     *                    to identify the type of object, so that mapping to the
     *                    well-known stereotypes defined for that type can occur.
     * @return Stereotypes cache with normalized and mapped stereotypes. Can be
     *         empty but not <code>null</code>.
     */
    public static Stereotypes normalizeAndMapToWellKnownStereotype(String[] stereotypes, Info infoObject) {

	Options options = infoObject.options();
	ShapeChangeResult result = infoObject.result();

	result.addDebug(null, 50, infoObject.name(), String.join(", ", stereotypes));

	Stereotypes resultingStereotypes = options.stereotypesFactory();

	for (String stereotype : stereotypes) {

	    String normalizedStereotype = options.normalizeStereotype(stereotype.trim());

	    result.addDebug(null, 51, infoObject.name(), stereotype, normalizedStereotype);

	    Set<String> wellKnownStereotypes;

	    if (infoObject instanceof PropertyInfo) {
		wellKnownStereotypes = Options.propertyStereotypes;
	    } else if (infoObject instanceof ClassInfo) {
		wellKnownStereotypes = Options.classStereotypes;
	    } else if (infoObject instanceof PackageInfo) {
		wellKnownStereotypes = Options.packageStereotypes;
	    } else if (infoObject instanceof AssociationInfo) {
		wellKnownStereotypes = Options.assocStereotypes;
	    } else {
		wellKnownStereotypes = new HashSet<>();
	    }

	    String s = normalizedStereotype.toLowerCase();
	    if (wellKnownStereotypes.contains(s)) {
		resultingStereotypes.add(s);
		result.addDebug(null, 52, infoObject.name(), s);
	    } else {
		
		if (options.allowAllStereotypes()) {
		    resultingStereotypes.add(normalizedStereotype);
		    result.addDebug(null, 56, infoObject.name(), normalizedStereotype);
		} else if (options.addedStereotypes().contains(normalizedStereotype)) {
		    resultingStereotypes.add(normalizedStereotype);
		    result.addDebug(null, 57, infoObject.name(), normalizedStereotype);
		} else {
		    result.addDebug(null, 53, infoObject.name(), normalizedStereotype);
		}
	    }
	}

	result.addDebug(null, 54, infoObject.name(), Integer.toString(resultingStereotypes.size()),
		resultingStereotypes.toString());

	return resultingStereotypes;
    }

}
