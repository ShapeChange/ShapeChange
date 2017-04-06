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

import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import de.interactive_instruments.ShapeChange.Multiplicity;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.StructuredNumber;
import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.Model.AssociationInfo;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.Descriptor;
import de.interactive_instruments.ShapeChange.Model.LangString;
import de.interactive_instruments.ShapeChange.Model.Descriptors;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfoImpl;
import de.interactive_instruments.ShapeChange.Model.EA.ClassInfoEA;
import de.interactive_instruments.ShapeChange.Model.EA.EADocument;

public class PropertyInfoXmi10 extends PropertyInfoImpl
		implements PropertyInfo {
	// Data
	protected Element prp = null;
	protected Xmi10Document doc = null;
	protected String id = null;
	protected boolean attribute = true;
	protected AssociationInfoXmi10 associationInfo = null;
	protected ClassInfo classInfo = null;

	protected StructuredNumber sequenceNumber = null;
	protected PropertyInfo reverseProperty = null;

	/**
	 * Flag used to prevent duplicate retrieval/computation of the alias of this
	 * class.
	 */
	protected boolean aliasAccessed = false;

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
	};

	public String name() {
		String s = doc.textOfProperty(prp, "Foundation.Core.ModelElement.name");

		if (s != null) {
			s = s.trim();
			if (s.startsWith("/")) {
				s = s.substring(1);
			}
			if (attribute && s.indexOf("[") > 0 && s.endsWith("]")) {
				String[] parts = s.split("\\[");
				s = parts[0].trim();
			}
		} else {
			if (classInfo == null || (classInfo.category() != Options.CODELIST
					&& classInfo.category() != Options.ENUMERATION)) {
				if (isNavigable()) {
					doc.result.addWarning(null, 100, "property", id);
				}
				s = id;
			} else {
				doc.result.addWarning(null, 136, id, classInfo.name());
			}
		}

		return s;
	};

	// @Override
	// public Descriptors aliasNameAll() {
	// // Obtain alias name from default implementation
	// Descriptors a = super.aliasNameAll();
	// // If not present, and if we are an attribute, read the "style" tagged
	// // value, which is supposed to carry the contents of the alias field.
	// if (a.isEmpty() && isAttribute()) {
	// a = new Descriptors(new LangString(
	// options().internalize(taggedValue("style"))));
	// }
	// return a;
	// }

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

	public boolean isDerived() {
		String s = doc.textOfProperty(prp, "Foundation.Core.ModelElement.name");
		if (s != null) {
			s = s.trim();
			if (s.startsWith("/")) {
				return true;
			}
		}
		return false;
	};

	public boolean isAttribute() {
		return attribute;
	};

	public Type typeInfo() {
		Type ti = new Type();

		if (attribute) {
			ti.id = doc
					.idOfProperty(prp, "Foundation.Core.StructuralFeature.type")
					.trim();
		} else {
			ti.id = doc.idOfProperty(prp, "Foundation.Core.AssociationEnd.type")
					.trim();
		}

		Element e = doc.getElementById(ti.id);
		if (e != null) {
			ti.name = doc.textOfProperty(e,
					"Foundation.Core.ModelElement.name");
			if (ti.name == null) {
				return ti;
			} else {
				ti.name = ti.name.trim();
			}
		}

		if (ti.id == null) {
			doc.result.addError(null, 137, id(), name());
			return ti;
		}
		if (ti.name == null) {
			doc.result.addError(null, 138, id(), name());
			return ti;
		}

		if (attribute && ti.name.indexOf("[") > 0
				&& (ti.name.endsWith("]") || ti.name.endsWith("}"))) {
			String[] parts = ti.name.split("\\[");
			ti.name = parts[0].trim();
		}
		if (attribute && ti.name.indexOf("{") > 0 && ti.name.endsWith("}")) {
			String[] parts = ti.name.split("\\{");
			ti.name = parts[0].trim();
		}
		return ti;
	};

	public Multiplicity cardinality() {
		Multiplicity m = new Multiplicity();

		Element e1 = doc.elementOfProperty(prp,
				"Foundation.Core.StructuralFeature.multiplicity");
		if (e1 == null) {
			e1 = doc.elementOfProperty(prp,
					"Foundation.Core.AssociationEnd.multiplicity");
		}
		if (e1 != null) {
			Element e2 = doc.elementOfProperty(e1,
					"Foundation.Data_Types.Multiplicity.range");
			if (e2 != null) {
				Integer lower = new Integer(doc.textOfProperty(e2,
						"Foundation.Data_Types.MultiplicityRange.lower"));
				m.minOccurs = lower.intValue();

				String upperval = doc.textOfProperty(e2,
						"Foundation.Data_Types.MultiplicityRange.upper");
				if (upperval.equals("*")) {
					m.maxOccurs = Integer.MAX_VALUE;
				} else {
					Integer upper = new Integer(upperval);
					m.maxOccurs = upper.intValue();
					if (m.maxOccurs == -1) {
						m.maxOccurs = Integer.MAX_VALUE;
					}
				}
			}
		}

		// Rose is not capable of specifiying the multiplicity of attributes.
		// This has to be encoded as part of the name. The following code
		// extracts
		// the multiplicty info from the name and reduces the attribute name to
		// its intended value.
		String name = doc.textOfProperty(prp,
				"Foundation.Core.ModelElement.name");
		if (attribute && name.indexOf("[") > 0 && name.endsWith("]")) {
			String[] parts = name.split("\\[");
			String multiplicityRanges = parts[1].substring(0,
					parts[1].length() - 1);
			String[] ranges = multiplicityRanges.split(",");
			int minv = Integer.MAX_VALUE;
			int maxv = Integer.MIN_VALUE;
			int lower;
			int upper;
			for (int i = 0; i < ranges.length; i++) {
				if (ranges[i].indexOf("..") > 0) {
					String[] minmax = ranges[i].split("\\.\\.", 2);
					lower = Integer.parseInt(minmax[0]);
					if (minmax[1].equals("*") || minmax[1].equals("n")
							|| minmax[1].length() == 0) {
						upper = Integer.MAX_VALUE;
					} else {
						try {
							upper = Integer.parseInt(minmax[1]);
						} catch (NumberFormatException e) {
							doc.result.addWarning(null, 1003, minmax[1]);
							upper = Integer.MAX_VALUE;
						}
					}
				} else {
					if (ranges[i].length() == 0 || ranges[i].equals("*")
							|| ranges[i].equals("n")) {
						lower = 0;
						upper = Integer.MAX_VALUE;
					} else {
						try {
							lower = Integer.parseInt(ranges[i]);
							upper = lower;
						} catch (NumberFormatException e) {
							doc.result.addWarning(null, 1003, ranges[i]);
							lower = 0;
							upper = Integer.MAX_VALUE;
						}
					}
				}
				if (lower < minv && lower >= 0) {
					minv = lower;
				}
				if (upper < 0) {
					maxv = Integer.MAX_VALUE;
				}
				if (upper > maxv) {
					maxv = upper;
				}
			}
			m.minOccurs = minv;
			m.maxOccurs = maxv;
		}

		if (attribute) {
			Multiplicity mx = doc.fClassesRoseHiddenCardinality
					.get(typeInfo().id);
			if (mx != null) {
				m = mx;
			}
		}

		return m;
	};

	public boolean isNavigable() {
		if (attribute) {
			return true;
		} else {
			return doc.attributeOfProperty(prp,
					"Foundation.Core.AssociationEnd.isNavigable", "xmi.value")
					.equals("true");
		}
	};

	public boolean isOrdered() {
		if (attribute) {
			return false;
		} else {
			return doc.attributeOfProperty(prp,
					"Foundation.Core.AssociationEnd.ordering", "xmi.value")
					.equals("ordered");
		}
	};

	public boolean isUnique() {
		// does not seem to be implemented in XMI 1.0
		return true;
	};

	public boolean isComposition() {
		if (attribute)
			return true;

		/* XMI tests on associations disabled due to issue with EA XMI */
		return false;
	};

	public boolean isAggregation() {
		if (attribute)
			return true;

		/* XMI tests on associations disabled due to issue with EA XMI */
		return false;
	};

	public String inlineOrByReference() {
		String s = doc.taggedValue(id, "inlineOrByReference");
		if (s == null) {
			s = "";
		}

		// If still not set, find out from model
		if (s.length() == 0)
			s = super.inlineOrByReferenceFromEncodingRule();

		return s.toLowerCase();
	};

	public String defaultCodeSpace() {
		String s = doc.taggedValue(id, "defaultCodeSpace");
		if (s == null) {
			s = "";
		}
		return s;
	};

	public boolean voidable() {
		if (stereotype("voidable")) {
			return true;
		}
		return false;
	};

	public void validateStereotypesCache() {
		if (stereotypesCache == null) {
			stereotypesCache = doc.fStereotypes.get(id);
		}

		if (stereotypesCache == null)
			stereotypesCache = options().stereotypesFactory();
	};

	public StructuredNumber sequenceNumber() {
		return sequenceNumber;
	};

	public String initialValue() {
		String initialValue = null;
		if (attribute) {
			NodeList nl = prp.getElementsByTagName(
					"Foundation.Core.Attribute.initialValue");
			if (nl.getLength() == 1) {
				Element e = (Element) nl.item(0);
				nl = e.getElementsByTagName("Foundation.Data_Types.Expression");
				if (nl.getLength() == 1) {
					e = (Element) nl.item(0);
					initialValue = doc.textOfProperty(e,
							"Foundation.Data_Types.Expression.body");
					if (initialValue != null) {
						initialValue = initialValue.trim();
						if (initialValue.toLowerCase().equals("true")) {
							initialValue = "true";
						} else if (initialValue.toLowerCase().equals("false")) {
							initialValue = "false";
						}
					}
				}
			}
		}
		return initialValue;
	};

	public PropertyInfo reverseProperty() {
		return reverseProperty;
	};

	public PropertyInfoXmi10(Xmi10Document d, Element e,
			AssociationInfoXmi10 ai) throws ShapeChangeAbortException {
		doc = d;
		prp = e;
		attribute = (ai == null);
		associationInfo = ai;
		id = prp.getAttribute("xmi.id");
		String s = doc.taggedValue(id, "sequenceNumber");
		if (s != null) {
			sequenceNumber = new StructuredNumber(s);
		} else {
			sequenceNumber = attribute
					? new StructuredNumber(
							getNextNumberForAttributeWithoutExplicitSequenceNumber())
					: new StructuredNumber(
							getNextNumberForAssociationRoleWithoutExplicitSequenceNumber());
		}
		doc.result.addDebug(null, 10013, "property", id, name());
	}

	public ClassInfo inClass() {
		return classInfo;
	}

	public void inClass(ClassInfo ci) {
		classInfo = ci;
	}

	public Vector<Constraint> constraints() {
		return new Vector<Constraint>();
	}

	public AssociationInfo association() {
		return associationInfo;
	}

	@Override
	protected List<LangString> descriptorValues(Descriptor descriptor) {

		// get default first
		List<LangString> ls = super.descriptorValues(descriptor);

		if (ls.isEmpty()) {

			if (!aliasAccessed && descriptor == Descriptor.ALIAS) {

				aliasAccessed = true;

				if (isAttribute()) {

					String a = taggedValue("style");

					if (a != null && !a.isEmpty()) {
						ls.add(new LangString(options().internalize(a)));
						this.descriptors().put(descriptor, ls);
					}
				}
			}
		}

		return ls;
	}
}