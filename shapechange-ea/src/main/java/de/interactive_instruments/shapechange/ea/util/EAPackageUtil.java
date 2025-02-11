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
package de.interactive_instruments.shapechange.ea.util;

import java.util.Set;
import java.util.SortedMap;

import org.sparx.Collection;
import org.sparx.Connector;
import org.sparx.Element;
import org.sparx.Package;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class EAPackageUtil extends AbstractEAUtil {

    /**
     * Sets the given tagged value in the tagged values of the given package. If
     * tagged values with the same tag name already exist, they will be deleted.
     * Then the tagged value will be added.
     * 
     * @param pkg   the package in which the tagged value shall be set
     * @param name  name of the tagged value to set, must not be <code>null</code>
     * @param value value of the tagged value to set, can be <code>null</code>
     * @throws EAException tbd
     */
    public static void setTaggedValue(Package pkg, String name, String value) throws EAException {

	Element e = pkg.GetElement();

	EATaggedValue tv = new EATaggedValue(name, value);

	EAElementUtil.deleteTaggedValue(e, tv.getName());
	EAElementUtil.addTaggedValue(e, tv);
    }

    public static Package addPackage(Package owner, String newPackageName, String newPackageType) throws EAException {

	Package pkg = owner.GetPackages().AddNew(newPackageName, newPackageType);

	if (!pkg.Update()) {
	    throw new EAException(createMessage(message(101), newPackageName, owner.GetName(), pkg.GetLastError()));
	} else {
	    return pkg;
	}
    }

    /**
     * Searches in the package hierarchy with the given package at the top. First
     * checks if the given package has the given name. Otherwise, recursively
     * searches in child packages.
     * 
     * @param pkgIn Package to search in
     * @param name  Name of package to find
     * @return the package with equal to the given name; can be <code>null</code> if
     *         no such package was found
     */
    public static Package findPackage(Package pkgIn, String name) {

	if (pkgIn.GetName().equals(name)) {

	    return pkgIn;

	} else {

	    Collection<org.sparx.Package> pkgs = pkgIn.GetPackages();
	    pkgs.Refresh();

	    for (short i = 0; i < pkgs.GetCount(); i++) {

		Package resPkg = EAPackageUtil.findPackage(pkgs.GetAt(i), name);

		if (resPkg != null) {
		    return resPkg;
		}
	    }

	    return null;
	}
    }

    /**
     * Looks up the elements contained in the given package as well as the direct
     * and indirect child packages whose type equals one of the given types. Results
     * are stored in the given map.
     * 
     * @param pkg          EA package in which to search for elements; must not be
     *                     <code>null</code>
     * @param elementTypes element types to look up, must not be <code>null</code>;
     *                     see the EA API on Element.Type for the list of possible
     *                     types
     * @param resultMap    storage for found elements (key: element name, value: EA
     *                     element ID); must not be <code>null</code>
     */
    public static void lookUpElements(Package pkg, Set<String> elementTypes, SortedMap<String, Integer> resultMap) {

	Collection<Element> elmts = pkg.GetElements();
	elmts.Refresh();

	// process elements contained in the package
	for (short i = 0; i < elmts.GetCount(); i++) {

	    Element elmt = elmts.GetAt(i);

	    if (elementTypes.contains(elmt.GetType())) {
		resultMap.put(elmt.GetName(), elmt.GetElementID());
	    }
	}

	// process child packages
	Collection<org.sparx.Package> pkgs = pkg.GetPackages();
	pkgs.Refresh();

	for (short i = 0; i < pkgs.GetCount(); i++) {

	    lookUpElements(pkgs.GetAt(i), elementTypes, resultMap);
	}
    }

    public static void deleteElement(Package pkg, int elementId) {

	Collection<Element> elmts = pkg.GetElements();
	elmts.Refresh();

	for (short i = 0; i < elmts.GetCount(); i++) {
	    Element e = elmts.GetAt(i);
	    if (e.GetElementID() == elementId) {
		elmts.Delete(i);
		break;
	    }
	}

	elmts.Refresh();
    }

    public static void deleteConnector(Package pkg, int connectorId) {

	Collection<Connector> conns = pkg.GetConnectors();
	conns.Refresh();

	for (short i = 0; i < conns.GetCount(); i++) {
	    Connector conn = conns.GetAt(i);
	    if (conn.GetConnectorID() == connectorId) {
		conns.Delete(i);
		break;
	    }
	}

	conns.Refresh();
    }

    public static void createDependencyConnector(Package pkg, int targetElementId) throws EAException {

	Collection<Connector> conns = pkg.GetConnectors();
	conns.Refresh();

	Connector conn = conns.AddNew("", "Dependency");
	conn.SetClientID(pkg.GetElement().GetElementID());
	conn.SetSupplierID(targetElementId);
	if (!conn.Update()) {
	    throw new EAException(createMessage(message(102), pkg.GetName(), conn.GetLastError()));
	}

	conns.Refresh();
    }

    public static void setEAName(Package e, String name) throws EAException {

	e.SetName(name);

	if (!e.Update()) {
	    throw new EAException(createMessage(message(103), e.GetName(), e.GetLastError()));
	}
    }

    public static void setEAParentID(Package e, int parentId) throws EAException {

	e.SetParentID(parentId);

	if (!e.Update()) {
	    throw new EAException(createMessage(message(104), e.GetName(), e.GetLastError()));
	}
    }

    public static String message(int mnr) {

	switch (mnr) {

	case 101:
	    return "EA error encountered while adding child package '$1$' to EA package '$2$'. Error message is: $3$";
	case 102:
	    return "EA error encountered while adding a dependency relationship to EA package '$1$'. Error message is: $2$";
	case 103:
	    return "EA error encountered while updating 'Name' of EA package '$1$'. Error message is: $2$";
	case 104:
	    return "EA error encountered while updating 'ParentID' of EA package '$1$'. Error message is: $2$";

	default:
	    return "(" + EAPackageUtil.class.getName() + ") Unknown message with number: " + mnr;
	}
    }
}
