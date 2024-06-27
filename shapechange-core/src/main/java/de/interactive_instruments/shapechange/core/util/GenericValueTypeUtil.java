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
package de.interactive_instruments.shapechange.core.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

import de.interactive_instruments.shapechange.core.model.ClassInfo;
import de.interactive_instruments.shapechange.core.model.PropertyInfo;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class GenericValueTypeUtil {

    public static boolean isSubtypeOfGenericValueType(ClassInfo ci, SortedSet<String> namesOfGenericValueTypes) {

	if (namesOfGenericValueTypes.isEmpty()) {
	    return false;
	}

	boolean aSupertypeIsAGenericValueType = false;

	for (ClassInfo supertype : ci.supertypeClasses()) {
	    if (namesOfGenericValueTypes.contains(supertype.name())) {
		aSupertypeIsAGenericValueType = true;
		break;
	    } else {
		aSupertypeIsAGenericValueType = isSubtypeOfGenericValueType(supertype, namesOfGenericValueTypes);
	    }
	}

	return aSupertypeIsAGenericValueType;
    }

    public static Optional<String> commonValuePropertyOfSubtypes(ClassInfo genericValueType) {

	List<Set<String>> propNameSets = new ArrayList<>();

	for (ClassInfo subtype : genericValueType.subtypesInCompleteHierarchy()) {
	    Set<String> propNames = new HashSet<>();
	    for (PropertyInfo subPi : subtype.properties().values()) {
		propNames.add(subPi.name());
	    }
	    propNameSets.add(propNames);
	}

	if (propNameSets.isEmpty()) {
	    return Optional.empty();
	} else {

	    Set<String> intersection = propNameSets.get(0);

	    for (int i = 1; i < propNameSets.size(); i++) {
		Set<String> nextSet = propNameSets.get(i);
		intersection.retainAll(nextSet);
	    }

	    if (intersection.size() == 1) {
		return Optional.of(intersection.iterator().next());
	    } else {
		return Optional.empty();
	    }
	}
    }
}
