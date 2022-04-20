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

import java.util.Comparator;

import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;

/**
 * Used for sorting collections of DiffElement2 objects.
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class DiffElement2Comparator implements Comparator<DiffElement2> {

    @Override
    public int compare(DiffElement2 o1, DiffElement2 o2) {

	if (o1.equals(o2)) {
	    return 0;
	}

	Info o1Info = null;
	if (o1.targetInfo != null) {
	    o1Info = o1.targetInfo;
	} else if (o1.sourceInfo != null) {
	    o1Info = o1.sourceInfo;
	}

	Info o2Info = null;
	if (o2.targetInfo != null) {
	    o2Info = o2.targetInfo;
	} else if (o2.sourceInfo != null) {
	    o2Info = o2.sourceInfo;
	}

	String packagePathThis = "";
	String packagePathOther = "";

	if (o1Info != null) {

	    if (o1Info instanceof PropertyInfo) {
		packagePathThis = ((PropertyInfo) o1Info).inClass().pkg().fullNameInSchema();
	    } else if (o1Info instanceof ClassInfo) {
		packagePathThis = ((ClassInfo) o1Info).pkg().fullNameInSchema();
	    } else {
		packagePathThis = o1Info.fullNameInSchema();
	    }
	}

	if (o2Info != null)
	    if (o2Info instanceof PropertyInfo) {
		packagePathOther = ((PropertyInfo) o2Info).inClass().pkg().fullNameInSchema();
	    } else if (o2Info instanceof ClassInfo) {
		packagePathOther = ((ClassInfo) o2Info).pkg().fullNameInSchema();
	    } else {
		packagePathOther = o2Info.fullNameInSchema();
	    }

	int comparePkgPath = compareSchemaPaths(packagePathThis, packagePathOther);
	if (comparePkgPath != 0) {
	    return comparePkgPath;
	}

	if (o1Info != null && o2Info != null) {
	    /*
	     * If package path is equal, compare again using fullNameInSchema (to ensure
	     * that package contents are ordered)
	     */
	    int compare = o1Info.fullNameInSchema().compareTo(o2Info.fullNameInSchema());
	    if (compare != 0) {
		return compare;
	    }
	}

	if (o1.change != o2.change) {
	    return o1.change.toString().compareTo(o2.change.toString());
	}

	if (o1.elementChangeType != o2.elementChangeType) {
	    return o1.elementChangeType.toString().compareTo(o2.elementChangeType.toString());
	}

	if (o1.subElement != null && o2.subElement != null) {
	    int compare = o1.subElement.fullNameInSchema().compareTo(o2.subElement.fullNameInSchema());
	    if (compare != 0) {
		return compare;
	    }
	} else if (o1.subElement != null) {
	    return -1;
	} else if (o2.subElement != null) {
	    return 1;
	}

	if (o1.tag != null && o2.tag != null) {
	    int compare = o1.tag.compareTo(o2.tag);
	    if (compare != 0) {
		return compare;
	    }
	} else if (o1.tag != null) {
	    return -1;
	} else if (o2.tag != null) {
	    return 1;
	}

	if (o1.diff != null && o2.diff != null) {
	    int compare = o1.diff_from_to().compareTo(o2.diff_from_to());
	    if (compare != 0) {
		return compare;
	    }
	} else if (o1.diff != null) {
	    return -1;
	} else if (o2.diff != null) {
	    return 1;
	}

	return 0;

    }

    private int compareSchemaPaths(String pathThis, String pathOther) {

	String[] ptMembers = pathThis.split("::");
	String[] poMembers = pathOther.split("::");

	int maxLength = ptMembers.length >= poMembers.length ? ptMembers.length : poMembers.length;

	for (int i = 0; i < maxLength; i++) {
	    if (ptMembers.length - 1 < i && poMembers.length - 1 < i) {
		break;
	    } else if (ptMembers.length - 1 < i) {
		return -1;
	    } else if (poMembers.length - 1 < i) {
		return 1;
	    } else {
		int compareMembers = ptMembers[i].compareTo(poMembers[i]);
		if (compareMembers != 0) {
		    return compareMembers;
		}
	    }
	}

	return 0;
    }
}
