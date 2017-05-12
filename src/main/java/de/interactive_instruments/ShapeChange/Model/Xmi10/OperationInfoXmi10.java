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

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.w3c.dom.Element;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.LangString;
import de.interactive_instruments.ShapeChange.Model.Descriptor;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.OperationInfo;
import de.interactive_instruments.ShapeChange.Model.OperationInfoImpl;

public class OperationInfoXmi10 extends OperationInfoImpl
		implements OperationInfo {
	// Data
	protected Element op = null;
	protected Xmi10Document doc = null;
	protected String id = null;

	/**
	 * Flag used to prevent duplicate retrieval/computation of the documentation
	 * of this class.
	 */
	protected boolean documentationAccessed = false;

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

	public String id() {
		return id;
	};

	public String name() {
		String s = doc.textOfProperty(op, "Foundation.Core.ModelElement.name");
		if (s != null) {
			s = s.trim();
		} else {
			doc.result.addWarning(null, 100, "operation", id);
			s = id;
		}
		return s;
	};

	// @Override
	// public Descriptors documentationAll() {
	// String s = doc.taggedValue(id, "documentation");
	// if (s == null) {
	// s = doc.taggedValue(id, "description");
	// if (s == null) {
	// s = "";
	// }
	// }
	//
	// return new Descriptors(new LangString(options().internalize(s)));
	// };

	@Override
	protected List<LangString> descriptorValues(Descriptor descriptor) {

		// get default first
		List<LangString> ls = super.descriptorValues(descriptor);

		if (ls.isEmpty()) {

			if (!documentationAccessed
					&& descriptor == Descriptor.DOCUMENTATION) {

				documentationAccessed = true;

				String s = doc.taggedValue(id, "documentation");
				if (s == null) {
					s = doc.taggedValue(id, "description");
				}

				if (s != null) {
					ls.add(new LangString(options().internalize(s)));
					this.descriptors().put(descriptor, ls);
				}
			}
		}

		return ls;
	}

	public int parameterCount() {
		Vector<String> pids = doc.idsOfProperty(op,
				"Foundation.Core.BehavioralFeature.parameter");
		int parameterCount = 0;
		for (Iterator<String> i = pids.iterator(); i.hasNext();) {
			Element e = doc.getElementById(i.next());
			if (!e.getNodeName().equals("Foundation.Core.Parameter")) {
				continue;
			}
			if (!doc.visible(e)) {
				continue;
			}
			if (!doc.notAReference(e)) {
				continue;
			}
			parameterCount++;
		}
		return parameterCount;
	};

	private boolean returnParameter(Element e) {
		if (e == null) {
			return false;
		}
		if (!"return".equals(doc.attributeOfProperty(e,
				"Foundation.Core.Parameter.kind", "xmi.value"))) {
			return false;
		}
		return true;
	};

	public TreeMap<Integer, String> parameterNames() {
		// get parameters
		Vector<String> pids = doc.idsOfProperty(op,
				"Foundation.Core.BehavioralFeature.parameter");
		TreeMap<Integer, String> parameterNames = new TreeMap<Integer, String>();
		int parameterCount = 0;
		for (Iterator<String> i = pids.iterator(); i.hasNext();) {
			Element e = doc.getElementById(i.next());
			if (!e.getNodeName().equals("Foundation.Core.Parameter")) {
				continue;
			}
			if (!doc.visible(e)) {
				continue;
			}
			if (!doc.notAReference(e)) {
				continue;
			}
			String s = doc.textOfProperty(e,
					"Foundation.Core.ModelElement.name");
			if (returnParameter(e)) {
				s = "__RETURN__";
			}
			parameterNames.put(++parameterCount, s.trim());
			doc.result.addDebug(null, 10011, id, name(), s);
		}
		return parameterNames;
	};

	public TreeMap<Integer, String> parameterTypes() {
		Vector<String> pids = doc.idsOfProperty(op,
				"Foundation.Core.BehavioralFeature.parameter");
		TreeMap<Integer, String> parameterTypes = new TreeMap<Integer, String>();
		int parameterCount = 0;
		for (Iterator<String> i = pids.iterator(); i.hasNext();) {
			Element e = doc.getElementById(i.next());
			if (!e.getNodeName().equals("Foundation.Core.Parameter")) {
				continue;
			}
			if (!doc.visible(e)) {
				continue;
			}
			if (!doc.notAReference(e)) {
				continue;
			}
			String s = doc.idOfProperty(e, "Foundation.Core.Parameter.type");
			parameterTypes.put(++parameterCount, s.trim());
		}
		return parameterTypes;
	};

	public OperationInfoXmi10(Xmi10Document d, Element e)
			throws ShapeChangeAbortException {
		doc = d;
		op = e;
		id = op.getAttribute("xmi.id");
		doc.result.addDebug(null, 10013, "operation", id, name());
	}
}