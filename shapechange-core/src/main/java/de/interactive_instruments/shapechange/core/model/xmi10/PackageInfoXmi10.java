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

package de.interactive_instruments.shapechange.core.model.xmi10;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.interactive_instruments.shapechange.core.target.xmlschema.XsdDocument;
import de.interactive_instruments.shapechange.core.MessageSource;
import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ShapeChangeAbortException;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.model.Model;
import de.interactive_instruments.shapechange.core.model.PackageInfo;
import de.interactive_instruments.shapechange.core.model.PackageInfoImpl;

public class PackageInfoXmi10 extends PackageInfoImpl implements PackageInfo, MessageSource {

	protected Element pkg;
	protected Xmi10Document doc;
	protected String id;
	protected XsdDocument gmlASD = null;
	protected boolean processedIncludes = false;
	protected UUID uuid = null;
	protected Document defDocument = null;
	protected Document smDocument = null;

	public Model model() {
		return doc;
	}
	
	public Options options() {
		return doc.options;
	}
	
	public ShapeChangeResult result() {
		return doc.result;
	}
	
	public String id() {
		return id;
	};

	public String name() {
		String s = doc.textOfProperty(pkg, "Foundation.Core.ModelElement.name");
		if (s == null) {
			s = id();
			doc.result.addWarning(null, 100, "package", s);
		} else {
			s = s.trim();
		}
		return s;
	};

	public void validateStereotypesCache() {
		if (stereotypesCache == null) {
			stereotypesCache = doc.fStereotypes.get(id);
		}
		
		if (stereotypesCache == null)
			stereotypesCache = options().stereotypesFactory();
	};

	// Validate tagged values cache, the filtering on tagged values defined 
	// within ShapeChange has already been done during initial loading of the 
	// XMI document...
	public void validateTaggedValuesCache() {
		if (taggedValuesCache == null) {
			taggedValuesCache = doc.taggedValues(id());
		}
		
		if (taggedValuesCache == null)
			taggedValuesCache = options().taggedValueFactory(0);
	} // validateTaggedValuesCache()	

	public PackageInfo owner() {
		String s = doc.idOfProperty(pkg,
				"Foundation.Core.ModelElement.namespace");
		if (s.length() == 0) {
			s = doc.getOwnerIdAsString(pkg);
		}
		return doc.fPackages.get(s);
	};

	public SortedSet<String> supplierIds() {
		SortedSet<String> suppliers = new TreeSet<String>();
		Vector<String> depids = doc.idsOfProperty(pkg,
				"Foundation.Core.ModelElement.clientDependency");
		for (Iterator<String> i = depids.iterator(); i.hasNext();) {
			Element e = doc.getElementById(i.next());
			if (e!=null && (doc.visible(e)
					|| doc.options.eaBugFixPublicPackagesAreMarkedAsPrivate)) {
				Vector<String> pkgids = doc.idsOfProperty(e,
						"Foundation.Core.Dependency.supplier");
				for (Iterator<String> j = pkgids.iterator(); j.hasNext();) {
					String sid = j.next();
					suppliers.add(sid);
				}
			}
		}
		return suppliers;
	};

	public XsdDocument gmlApplicationSchemaDocument() {
		return gmlASD;
	};

	public PackageInfoXmi10(Xmi10Document d, Element e)
			throws ShapeChangeAbortException {
		doc = d;
		pkg = e;
		id = pkg.getAttribute("xmi.id");

		uuid = UUID.randomUUID();
		doc.fUUIDs.put(id, uuid);

		doc.result.addDebug(this, 10001, id, name(), targetNamespace());
	}

	public SortedSet<PackageInfo> containedPackages() {
		SortedSet<PackageInfo> res = new TreeSet<PackageInfo>();
		NodeList nl, nl2;
		Node n, n2;
		nl = pkg.getChildNodes();
		for (int j = 0; j < nl.getLength(); j++) {
			n = nl.item(j);
			if (n.getLocalName().equals("Foundation.Core.Namespace.ownedElement")) {
				nl2 = n.getChildNodes();
				for (int k = 0; k < nl2.getLength(); k++) {
					n2 = nl2.item(k);
					if (n2.getLocalName().equals("Model_Management.Package")) {
						Element e = (Element) n2;
						String xid = e.getAttribute("xmi.id");
						res.add(doc.fPackages.get(xid));						
					}
				}
				break;
			}
		}
		return res;
	}
	
	@Override
	public String message(int mnr) {

		switch (mnr) {

		case 10001:
		    return "The package with ID '$1$' and name '$2$' was created. Namespace: '$3$'.";
		
		default:
		    return "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
		}
	}
}
