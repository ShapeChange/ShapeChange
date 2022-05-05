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

import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.platform.commons.util.StringUtils;
import org.w3c.dom.Element;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Multiplicity;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeIgnoreClassException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.StructuredNumber;
import de.interactive_instruments.ShapeChange.Model.AssociationInfo;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.ClassInfoImpl;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.OperationInfo;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;

/** Information about an UML class. */
public class ClassInfoXmi10 extends ClassInfoImpl implements ClassInfo, MessageSource {

	// Data
	protected String id;
	protected Element cla;
	protected Xmi10Document doc;
	protected UUID uuid;

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
		String s = doc.textOfProperty(cla, "Foundation.Core.ModelElement.name");
		if (s == null) {
			s = id();
			doc.result.addWarning(null, 100, "class", s);
		} else {
			s = s.trim();
		}
		/* Why needed in class names? Required for Rational Rose XMI! */
		if (s.indexOf("[") > 0 && (s.endsWith("]") || s.endsWith("}"))) {
			String[] parts = s.split("\\[");
			s = parts[0].trim();
		}
		if (s.indexOf("{") > 0 && s.endsWith("}")) {
			String[] parts = s.split("\\{");
			s = parts[0].trim();
		}
		return s;
	};

	protected Multiplicity roseHiddenCardinality() {
		String s = doc.textOfProperty(cla, "Foundation.Core.ModelElement.name");
		if (s == null) {
			return null;
		}
		s = s.trim();
		if (s.indexOf("[") > 0 && s.indexOf("]") > 0) {
			String[] parts = s.split("\\[");
			s = parts[1].trim();
			String[] parts2 = s.split("\\]");
			s = parts2[0].trim();
			return doc.cardinalityFromString(s);
		} else {
			return null;
		}
	};

	protected String roseHiddenLabels() {
		String s = doc.textOfProperty(cla, "Foundation.Core.ModelElement.name");
		if (s == null) {
			return null;
		}
		s = s.trim();
		if (s.indexOf("{") > 0 && s.endsWith("}")) {
			String[] parts = s.split("\\{");
			s = parts[1].substring(0, parts[1].length() - 1).trim();
			return s;
		} else {
			return null;
		}
	};

	public boolean isAbstract() {
		return doc.attributeOfProperty(cla,
				"Foundation.Core.GeneralizableElement.isAbstract", "xmi.value")
				.equals("true");
	};

	public boolean isLeaf() {
		return doc.attributeOfProperty(cla,
				"Foundation.Core.GeneralizableElement.isLeaf", "xmi.value")
				.equals("true");
	};

	public PackageInfo pkg() {
		String propId = doc.idOfProperty(cla,
				"Foundation.Core.ModelElement.namespace");
		if (propId.length() == 0) {
			propId = doc.getOwnerIdAsString(cla);
		}
		PackageInfo pkg = doc.fPackages.get(propId);
		if (pkg == null) {
			doc.result.addInfo(this, 1001, name(), id, propId);
		}
		return pkg;
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

	public SortedSet<String> supertypes() {
		SortedSet<String> res = doc.fSupertypes.get(id);
		if (res == null)
			res = new TreeSet<String>();
		return res;
	};

	public SortedSet<String> subtypes() {
		SortedSet<String> res = doc.fSubtypes.get(id);
		if (res == null)
			res = new TreeSet<String>();
		return res;
	};

	protected TreeMap<StructuredNumber, PropertyInfo> properties = null;

	public TreeMap<StructuredNumber, PropertyInfo> properties() {
		return properties;
	}

	public PropertyInfo property(String name) {
		for (Iterator<PropertyInfo> j = properties.values().iterator(); j
				.hasNext();) {
			PropertyInfo pi = j.next();
			if (pi != null && pi.name().equals(name)) {
				return pi;
			}
		}
		SortedSet<String> st = supertypes();
		if (st != null) {
			for (Iterator<String> j = st.iterator(); j.hasNext();) {
				ClassInfo ci = doc.classById(j.next());
				if (ci != null) {
					PropertyInfo pi = ci.property(name);
					if (pi != null)
						return pi;
				}
			}
		}
		return null;
	}

	public TreeMap<Integer, OperationInfo> operations;

	public ClassInfoXmi10(Xmi10Document d, Element e)
			throws ShapeChangeAbortException, ShapeChangeIgnoreClassException {
		cla = e;
		doc = d;
		id = cla.getAttribute("xmi.id");
		uuid = UUID.randomUUID();

		if (cla.getNodeName().equals("Foundation.Core.DataType")) {
			if (doc.isOwnerOfEnumeration(cla)) {
				category = Options.ENUMERATION; // required for uml14xmi10
			} else {
				category = Options.DATATYPE;
			}
		} else if (cla.getNodeName().equals("Foundation.Core.Interface")) {
			category = Options.MIXIN;
		} 
//		else if (cla.getNodeName().equals("Foundation.Core.Class")) {
//			establishCategory();
//		} else {
//			establishCategory();
//		}

		properties = new TreeMap<StructuredNumber, PropertyInfo>();

		// get attributes
		Vector<String> attids = doc.idsOfProperty(cla,
				"Foundation.Core.Classifier.feature");
		attids.addAll(
				doc.idsOfProperty(cla, "Foundation.Core.Enumeration.literal"));
		for (Iterator<String> i = attids.iterator(); i.hasNext();) {
			Element att = doc.getElementById(i.next());
			if (!att.getNodeName().equals("Foundation.Core.Attribute")
					&& !att.getNodeName()
							.equals("Foundation.Core.EnumerationLiteral")) {
				continue;
			}
			if (!doc.visible(att)) {
				continue;
			}
			if (!doc.notAReference(att)) {
				continue;
			}
			PropertyInfoXmi10 atti = new PropertyInfoXmi10(doc, att, null);
			atti.inClass(this);
			PropertyInfo piTemp = properties.get(atti.sequenceNumber());
			if (piTemp != null) {
				doc.result.addError(null, 107, atti.name(), name(),
						piTemp.name());
			}
			properties.put(atti.sequenceNumber(), atti);
		}

		// add all navigable association ends
		Vector<PropertyInfo> roles = doc.fRoles.get(id);
		if (roles != null) {
			for (Iterator<PropertyInfo> i = roles.iterator(); i.hasNext();) {
				PropertyInfo ri = i.next();
				((PropertyInfoXmi10) ri).inClass(this);
				PropertyInfo piTemp = properties.get(ri.sequenceNumber());
				if (piTemp != null) {
					doc.result.addError(null, 107, ri.name(), name(),
							piTemp.name());
				}
				properties.put(ri.sequenceNumber(), ri);
			}
		}

		// get operations
		Vector<String> opids = doc.idsOfProperty(cla,
				"Foundation.Core.Classifier.feature");
		operations = new TreeMap<Integer, OperationInfo>();
		// Ensure that there are methods before continuing
		if (opids != null && opids.size() > 0) {
			int count=0;
			for (Iterator<String> i = opids.iterator(); i.hasNext();) {
				Element e1 = doc.getElementById(i.next());
				if (!e1.getNodeName().equals("Foundation.Core.Operation")) {
					continue;
				}
				if (!doc.visible(e1)) {
					continue;
				}
				if (!doc.notAReference(e1)) {
					continue;
				}
				OperationInfo opi = new OperationInfoXmi10(doc, e1);
				operations.put(++count, opi);
			}
		}

		doc.result.addDebug(null, 10013, "class (" + category + ")", id,
				name());
	}

	private boolean createdConstraints = false;

	private final Vector<Constraint> constraints = new Vector<Constraint>();

	public Vector<Constraint> directConstraints() {
	    
		if (createdConstraints) {
			return constraints;
		}
		
		// Constraints from selected schemas only?
		if (doc.options.isLoadConstraintsForSelectedSchemasOnly()
				&& !doc.isInSelectedSchemas(this)) {
			createdConstraints = true;
			return constraints;
		}
		
		// Fetching constraints from tagged value named 'oclExpressions' ...
		HashMap<String, OclConstraintXmi10> namefilter = new HashMap<String, OclConstraintXmi10>();
		String tv = doc.taggedValue(id, "oclExpressions");
		if (StringUtils.isNotBlank(tv)) {
			// Find first inv [name] : pattern
			Pattern pat = Pattern.compile("inv\\s*\\w*\\s*:");
			Matcher mat = pat.matcher(tv);
			if (mat.find()) {
				// At least one invariant contained. Start at 0
				int i1 = 0;
				do {
					// Get position of next one or end
					int i2 = tv.length();
					if (mat.find())
						i2 = mat.start();
					// Forget the rest of the string
					String c = tv.substring(0, i2);
					// Everything before the keyword is replaced by space
					c = c.substring(0, i1).replaceAll("\\S+", " ")
							+ c.substring(i1);
					// Compile. If o.k., store away ...
					OclConstraintXmi10 ocl = new OclConstraintXmi10(doc, this,
							c);
					if (ocl.syntaxTree() != null)
						constraints.add(ocl);
					// If the constraint has a name, add it to the filter which
					// blocks inheritance of constraints
					String conam = ocl.name();
					if (conam != null && conam.length() > 0)
						namefilter.put(conam, ocl);
					// Switch to next constraint in tagged value
					i1 = i2;
				} while (i1 != tv.length());
			}
		}
		
		createdConstraints = true;
		return constraints;
	}

	public OperationInfo operation(String name, String[] types) {
		// TODO Auto-generated method stub
		return null;
	}

	public AssociationInfo isAssocClass() {
		// TODO not implemeted
		return null;
	}

	@Override
	public String message(int mnr) {

		switch (mnr) {

		case 1001:
		    return "Class '$1$' with ID '$2$' cannot be identified as being part of any package. The package is probably ignored, for example, because it carries an unsupported stereotype. The ID of the missing package is: '$3$'";
		
		default:
		    return "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
		}
	}
}
