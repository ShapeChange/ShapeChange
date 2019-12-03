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

package de.interactive_instruments.ShapeChange.Model.Xmi10;

import java.util.Vector;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.AssociationInfo;
import de.interactive_instruments.ShapeChange.Model.AssociationInfoImpl;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;

/** Information about an UML association. */
public class AssociationInfoXmi10 extends AssociationInfoImpl implements AssociationInfo {
	// Data
	protected Element ass = null;
	protected Xmi10Document doc = null;
	protected String id = null;
	protected PropertyInfoXmi10 end1 = null;
	protected PropertyInfoXmi10 end2 = null;

	// Methods
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
	}

	public void validateStereotypesCache() {
		if (stereotypesCache == null) {
			stereotypesCache = doc.fStereotypes.get(id);
		}
		
		if (stereotypesCache == null)
			stereotypesCache = options().stereotypesFactory();
	}

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
	
	public PropertyInfo end1() {
		return end1;
	};

	public PropertyInfo end2() {
		return end2;
	};

	public AssociationInfoXmi10(Xmi10Document d, Element e)
			throws ShapeChangeAbortException {
		doc = d;
		ass = e;
		id = ass.getAttribute("xmi.id");
		name = doc.textOfProperty(ass, "Foundation.Core.ModelElement.name");

		boolean vis1;
		boolean vis2;
		NodeList nl = ass
				.getElementsByTagName("Foundation.Core.AssociationEnd");
		if (nl.getLength() == 2) {
			vis1 = doc.visible((Element) nl.item(0));
			end1 = new PropertyInfoXmi10(doc, (Element) nl.item(0), this);
			vis2 = doc.visible((Element) nl.item(1));
			end2 = new PropertyInfoXmi10(doc, (Element) nl.item(1), this);

			if (vis2 && end2.isNavigable() && end2.name() != null) {
				if (doc.fRoles.containsKey(end1.typeInfo().id)) {
					doc.fRoles.get(end1.typeInfo().id).add(end2);
				} else {
					Vector<PropertyInfo> l1 = new Vector<PropertyInfo>();
					l1.add(end2);
					doc.fRoles.put(end1.typeInfo().id, l1);
				}
				doc.result.addDebug(null,10002,end2.name(), end1.name());
			}

			if (vis1 && end1.isNavigable() && end1.name() != null) {
				if (doc.fRoles.containsKey(end2.typeInfo().id)) {
					doc.fRoles.get(end2.typeInfo().id).add(end1);
				} else {
					Vector<PropertyInfo> l1 = new Vector<PropertyInfo>();
					l1.add(end1);
					doc.fRoles.put(end2.typeInfo().id, l1);
				}
				doc.result.addDebug(null,10002,end1.name(), end2.name());
			}
		} else if (!ass.hasAttribute("xmi.idref")) {
			doc.result.addError(null,103,name(),id, ""+nl.getLength());
		}		
		doc.result.addDebug(null,10013,"association",id, name());
	}

	public ClassInfo assocClass() {
		// TODO not supported for XMI 1.0 models
		return null;
	}

}
