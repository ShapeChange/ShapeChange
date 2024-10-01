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
 * (c) 2002-2024 interactive instruments GmbH, Bonn, Germany
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

package de.interactive_instruments.shapechange.ea.util.modelhelper;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.sparx.Collection;
import org.sparx.Element;
import org.sparx.Package;
import org.sparx.Repository;

/**
 * Helper class for storing information about the contents of an enterprise
 * architect repository. The content representation uses IDs of model elements,
 * rather than their actual EA API objects.
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class EARepository {

    public static final String NO_NAME = "NONAME";

    private Set<EAPackage> packages = new HashSet<>();
    private Set<EAElement> elements = new HashSet<>();

    public EARepository(Repository rep) {

	for (Package p : rep.GetModels()) {
	    readPackage(p, "");
	}
    }

    private void readPackage(Package pkg, String pathToPackage) {

	String name = pkg.GetName().trim();
	String fullName = pathToPackage + name;
	String pathToOwnedElements = fullName + "::";

	if (!pathToPackage.isEmpty()) {

	    int pkgElmtId = pkg.GetElement().GetElementID();

	    EAPackage eaPkg = new EAPackage(name, fullName, pkgElmtId, pkg.GetPackageID());
	    this.packages.add(eaPkg);

	    Collection<Element> c = pkg.GetElements();
	    c.Refresh();
	    for (Element elmt : c) {
		EAElement eaElmt = new EAElement(elmt, pathToOwnedElements);
		this.elements.add(eaElmt);
	    }
	}

	Collection<Package> childPackages = pkg.GetPackages();
	childPackages.Refresh();
	for (Package cp : childPackages) {
	    readPackage(cp, pathToOwnedElements);
	}

    }

    /**
     * @param pkgIn EA package for which to look up its (direct and indirect)
     *              children
     * @return key: child package full name; value: the child package
     */
    public SortedMap<String, EAPackage> childrenAll(EAPackage pkgIn) {

	String pkgInFullName = pkgIn.getFullName();

	SortedMap<String, EAPackage> res = new TreeMap<>();

	for (EAPackage eaPkg : packages) {
	    if (eaPkg.getFullName().startsWith(pkgInFullName) && !eaPkg.getFullName().equals(pkgInFullName)) {
		res.put(eaPkg.getFullName(), eaPkg);
	    }
	}

	return res;
    }

    /**
     * Creates a map of all EA packages that are direct or indirect children of
     * pkgIn, as well as pkgIn itself.
     * 
     * @param pkgIn EA package for which to generate the map
     * @return key: package full name; value: the package
     */
    public SortedMap<String, EAPackage> all(EAPackage pkgIn) {

	SortedMap<String, EAPackage> res = childrenAll(pkgIn);

	res.put(pkgIn.getFullName(), pkgIn);

	return res;
    }

    /**
     * @param pkg EA package for which to look up its (directly and indirectly
     *            contained) elements
     * @return key: element ID; value: the element
     */
    public SortedMap<Integer, EAElement> elementsAll(EAPackage pkg) {

	String pkgFullName = pkg.getFullName();

	SortedMap<Integer, EAElement> res = new TreeMap<>();

	for (EAElement elmt : elements) {
	    if (elmt.getFullName().startsWith(pkgFullName)) {
		res.put(elmt.getElementId(), elmt);
	    }
	}

	return res;
    }

    public Optional<EAPackage> lookupPackage(String pkgFullName) {
	return packages.stream().filter(eaPkg -> eaPkg.getFullName().equals(pkgFullName)).findFirst();
    }

    public Optional<EAPackage> lookupPackageByElementId(int pkgElementId) {
	return packages.stream().filter(eaPkg -> eaPkg.getPkgElementId() == pkgElementId).findFirst();
    }

    public Optional<EAElement> lookupElement(int elementId) {
	return elements.stream().filter(eaElmt -> eaElmt.getElementId() == (elementId)).findFirst();
    }

    /**
     * @param elementId element ID of an EA Element or Package (Element)
     * @return the full name of the element, if found
     */
    public Optional<String> getFullName(int elementId) {
	Optional<EAElement> elmtOpt = elements.stream().filter(elmt -> elmt.getElementId() == elementId).findFirst();
	if (elmtOpt.isPresent()) {
	    return Optional.of(elmtOpt.get().getFullName());
	} else {
	    Optional<EAPackage> pktOpt = packages.stream().filter(pkg -> pkg.getPkgElementId() == elementId)
		    .findFirst();
	    if (pktOpt.isPresent()) {
		return Optional.of(pktOpt.get().getFullName());
	    }
	}
	return Optional.empty();
    }

    /**
     * @param elmts elements to remove (does not apply to packages)
     */
    public void deleteElements(List<EAElement> elmts) {
	this.elements.removeAll(elmts);
    }

    /**
     * @param elementName simple name of the element (not its full name)
     * @return can be empty but not <code>null</code>
     */
    public List<EAElement> lookupElementByName(String elementName) {
	return this.elements.stream().filter(elmt -> elmt.getName().equals(elementName)).collect(Collectors.toList());
    }

}
