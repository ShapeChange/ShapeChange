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

package de.interactive_instruments.ShapeChange.Model;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.xml.serializer.utils.XMLChar;

import de.interactive_instruments.ShapeChange.MapEntry;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.Profile.Profiles;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;

public abstract class ClassInfoImpl extends InfoImpl implements ClassInfo {

	/**
	 * Category of the class according to ISO 19136 (and ShapeChange extensions)
	 */
	protected int category = Options.UNKNOWN;

	protected List<ImageMetadata> diagrams = null;
	protected File linkedDocument = null;
	protected Profiles profiles = null;

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 * 
	 * @see de.interactive_instruments.ShapeChange.Model.InfoImpl#language()
	 */
	@Override
	public String language() {
		String lang = this.taggedValue("language");

		if (lang == null || lang.isEmpty()) {
			PackageInfo pi = this.pkg();
			if (pi != null)
				return pi.language();
		} else
			return lang;

		return null;
	}

	/**
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	public String nsabr() {
		PackageInfo pi = pkg();
		if (pi != null)
			return pi.xmlns();
		return null;
	}

	/**
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	public String ns() {
		PackageInfo pi = pkg();
		if (pi != null)
			return pi.targetNamespace();
		return null;
	}

	/**
	 * Return the encoding rule relevant on the class, given the platform
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	public String encodingRule(String platform) {
		String s = taggedValue(platform + "EncodingRule");
		if (s == null || s.isEmpty()
				|| options().ignoreEncodingRuleTaggedValues()) {
			PackageInfo pi = pkg();
			if (pi != null)
				s = pi.encodingRule(platform);
			else {
				s = super.encodingRule(platform);
			}
		}
		if (s != null)
			s = s.toLowerCase().trim();
		return s;
	};

	// Standard ISO 19136 tagged value: If a class with the stereotype
	// <<Type>> has a canonical XML Schema encoding (e.g. from XML Schema) the
	// XML Schema typename corresponding to the data type shall be given as the
	// value of the tagged value "xmlSchemaType".
	// NOTE Canonical encodings may be preferred to structured encodings that
	// follow the standard UML-to-GML
	// encoding rules in some cases, for example where a compact structure based
	// on "simpleContent" is already well known within the application domain.
	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public String xmlSchemaType() {
		return taggedValue("xmlSchemaType");
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public boolean includeByValuePropertyType() {
		String s = taggedValue("byValuePropertyType");
		if (s != null && s.toLowerCase().equals("true"))
			return true;
		return false;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public boolean includePropertyType() {
		String s = taggedValue("noPropertyType");
		if (s != null && s.toLowerCase().equals("true"))
			return false;
		return true;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public boolean isCollection() {
		String s = taggedValue("isCollection");
		if (s != null && s.toLowerCase().equals("true")) {
			return true;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public boolean asDictionary() {
		if (category() != Options.CODELIST)
			return false;
		String s = taggedValue("asDictionary");
		if (s != null && s.toLowerCase().equals("true")) {
			return true;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public boolean asDictionaryGml33() {
		if (category() != Options.CODELIST)
			return false;
		String s = taggedValue("asDictionary");
		if (s != null && s.toLowerCase().equals("false")) {
			return false;
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public boolean asGroup() {
		String s = taggedValue("gmlAsGroup");
		if (s != null && s.toLowerCase().equals("true")) {
			return !this.isUnionDirect();
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public boolean asCharacterString() {
		String s = taggedValue("gmlAsCharacterString");
		if (s != null && s.toLowerCase().equals("true")) {
			return !this.isUnionDirect();
		}
		return false;
	}

	private boolean isMixin() {
		if (matches("rule-xsd-cls-mixin-classes")) {
			String tv = taggedValue("gmlMixin");
			if (tv != null && tv.equalsIgnoreCase("true"))
				return true;
		}
		return false;
	}

	private boolean isGMLMixinSetToFalse() {
		if (matches("rule-xsd-cls-mixin-classes")) {
			String tv = taggedValue("gmlMixin");
			if (tv != null && tv.equalsIgnoreCase("false"))
				return true;
		}
		return false;
	}

	/**
	 * Establish category. This auxiliary function determines the category of
	 * the class (being a FeatureType, a Codelist, a Union etc) from its
	 * stereotype. It is an error, if a class carries a stereotype not
	 * recognized by ShapeChange.
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	public void establishCategory() throws ShapeChangeAbortException {
		if (stereotype("enumeration")) {
			category = Options.ENUMERATION;
		} else if (stereotype("codelist")) {
			category = Options.CODELIST;
		} else if (stereotype("datatype")) {
			category = Options.DATATYPE;
			if (isMixin())
				category = Options.MIXIN;
		} else if (stereotype("union")) {
			category = Options.UNION;
		} else if (stereotype("featuretype")) {
			category = Options.FEATURE;
			if (isMixin())
				category = Options.MIXIN;
		} else if (stereotype("")) {
			category = Options.OBJECT;
			if (isMixin())
				category = Options.MIXIN;
		} else if (stereotype("type")) {
			category = Options.OBJECT;
			if ((isMixin()
					|| (isAbstract() && matches("rule-xsd-cls-mixin-classes"))
							&& !isGMLMixinSetToFalse()))
				category = Options.MIXIN;
		} else if (stereotype("interface")
				&& matches("rule-xsd-cls-mixin-classes")) {
			category = Options.MIXIN;
		} else if (stereotype("basictype")
				&& matches("rule-xsd-cls-basictype")) {
			category = Options.BASICTYPE;
		} else if (stereotype("adeelement")
				&& matches("rule-xsd-cls-adeelement")) {
			category = Options.FEATURE;
		} else if (stereotype("featureconcept")) {
			category = Options.FEATURECONCEPT;
		} else if (stereotype("attributeconcept")) {
			category = Options.ATTRIBUTECONCEPT;
		} else if (stereotype("roleconcept")) {
			category = Options.ROLECONCEPT;
		} else if (stereotype("valueconcept")) {
			category = Options.VALUECONCEPT;
		} else if (stereotype("schluesseltabelle")
				&& matches("rule-xsd-cls-okstra-schluesseltabelle")) {
			category = Options.OKSTRAKEY;
		} else if (stereotype("fachid") && matches("rule-xsd-cls-okstra-fid")) {
			category = Options.OKSTRAFID;
		} else if (stereotype("aixmextension") && options().isAIXM()) {
			category = Options.AIXMEXTENSION;
		} else {
			result().addInfo(null, 11, name(),
					stereotypes().toString().replace("[", "").replace("]", ""),
					encodingRule("xsd"));
			category = Options.UNKNOWN;
		}
	}

	@Override
	public int category() {
		return category;
	}

	/**
	 * Fix the category of a class, which from its stereotypes alone has not
	 * been assigned UNKONWN. The correction is applied by fetching the missing
	 * category from one of the supertypes.
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	public void fixIfCategoryIsUnknown() {
		if (category == Options.UNKNOWN && supertypes() != null) {
			if (isAbstract() && checkSupertypes(Options.MIXIN)) {
				category = Options.MIXIN;
			} else if (checkSupertypes(Options.OBJECT)) {
				category = Options.OBJECT;
			} else if (checkSupertypes(Options.FEATURE)) {
				category = Options.FEATURE;
			} else if (checkSupertypes(Options.DATATYPE)) {
				category = Options.DATATYPE;
			} else {
				result().addWarning(null, 104, name());
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public boolean inSchema(PackageInfo pi) {
		String ns1 = null;
		String ns2 = null;
		PackageInfo pi2 = this.pkg();
		if (pi2 != null)
			ns1 = pi2.targetNamespace();
		if (pi != null)
			ns2 = pi.targetNamespace();
		if (ns1 == null || ns2 == null) {
			return false;
		}
		if (!ns1.equals(ns2)) {
			return false;
		}
		return true;
	}

	// TODO If a class is in no package or is not associated with a namespace,
	// report an error?
	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public String qname() {
		PackageInfo pi = pkg();
		if (pi != null)
			return pi.xmlns() + ":" + name();
		return name();
	}

	@Override
	public SortedSet<PropertyInfo> propertiesAll() {

		SortedSet<PropertyInfo> allProps = new TreeSet<PropertyInfo>();

		allProps.addAll(this.properties().values());

		for (String supertypeId : this.supertypes()) {
			ClassInfo supertype = this.model().classById(supertypeId);
			SortedSet<PropertyInfo> allSupertypeProps = supertype
					.propertiesAll();
			allProps.addAll(allSupertypeProps);
		}

		return allProps;
	}

	@Override
	public boolean checkSupertypes(int cat) {
		SortedSet<String> ts = supertypes();
		if (ts == null) {
			result().addDebug(null, 10003, name(), "" + cat, "TRUE");
			return true;
		}
		boolean res = true;
		for (Iterator<String> i = ts.iterator(); i.hasNext();) {
			ClassInfo cix = model().classById(i.next());
			if (cix != null) {
				if (cix.category() == Options.UNKNOWN) {
					res = res && cix.checkSupertypes(cat);
				} else if (cix.category() == Options.MIXIN) {
				} else if (cix.category() != cat) {
					res = false;
				}
				if (!res) {
					result().addDebug(null, 10003, name(), "" + cat, "FALSE");
					return res;
				}
			}
		}
		if (res)
			result().addDebug(null, 10003, name(), "" + cat, "TRUE");
		else
			result().addDebug(null, 10003, name(), "" + cat, "FALSE");
		return res;
	}

	private void checkForBasicType(ClassInfoImpl cibase, String rule) {
		if (category() == Options.BASICTYPE) {
			if (cibase != this)
				cibase.category = Options.BASICTYPE;
			return;
		} else {
			MapEntry me = options().baseMapEntry(name(), rule);
			if (me != null) {
				if (!me.p2.equals("complex/complex")) {
					result().addDebug(
							qname() + " (" + category + ") is a basic type.");
					if (cibase != this)
						cibase.category = Options.BASICTYPE;
					return;
				}
			}
		}

		for (String cid : supertypes()) {
			ClassInfo cix = model().classById(cid);
			if (cix != null)
				((ClassInfoImpl) cix).checkForBasicType(cibase, rule);
		}
	}

	/*
	 * identify types that should be treated as "basic types" with a canonical
	 * implementation. Either they carry a basictype stereotype or a supertype
	 * is identified as a basic type through a map entry
	 */
	private void identifyBasicTypes() {
		if ((category == Options.DATATYPE || category == Options.OBJECT
				|| category == Options.MIXIN))
			checkForBasicType(this, encodingRule("xsd"));
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public boolean isSubtype(ClassInfo ci) {
		if (this == ci)
			return true;
		SortedSet<String> idsuper = supertypes();
		for (String sid : idsuper) {
			ClassInfo sci = model().classById(sid);
			if (sci == null)
				continue;
			return sci.isSubtype(ci);
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public boolean isKindOf(String supertype) {
		SortedSet<String> st = supertypes();
		if (st != null) {
			for (Iterator<String> i = st.iterator(); i.hasNext();) {
				ClassInfo sti = model().classById(i.next());
				if (sti.name().equals(supertype))
					return true;
				if (sti.isKindOf(supertype))
					return true;
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public boolean suppressed() {
		String supval = taggedValue("suppress");
		if (supval != null && supval.equalsIgnoreCase("true"))
			return true;
		if (stereotype("adeelement"))
			return true;
		return false;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public ClassInfo unsuppressedSupertype(boolean permitAbstract) {
		for (ClassInfo sci = this; sci != null; sci = sci.baseClass()) {
			if (sci.suppressed())
				continue;
			if (!permitAbstract && sci.isAbstract())
				continue;
			return sci;
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public boolean hasConstraint(String name) {
		List<Constraint> vc = constraints();
		for (Constraint c : vc) {
			if (c.name().equals(name))
				return true;
		}
		return false;
	}

	@Override
	public String fullName() {
		return pkg().fullName() + "::" + name();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public String fullNameInSchema() {
		return pkg().fullNameInSchema() + "::" + name();
	}

	private void checkOverloading(ClassInfo cicurr) {

		if (cicurr == null) {
			return;
		}

		result().addDebug(null, 10004, name(), cicurr.name());

		if (cicurr != this) {
			for (Iterator<PropertyInfo> i = cicurr.properties().values()
					.iterator(); i.hasNext();) {
				PropertyInfo aicurr = i.next();
				for (Iterator<PropertyInfo> j = properties().values()
						.iterator(); j.hasNext();) {
					PropertyInfo ai = j.next();
					if (aicurr != null && ai != null
							&& ai.name().equals(aicurr.name())) {
						if (ai.cardinality().minOccurs < aicurr
								.cardinality().minOccurs) {
							MessageContext mc = result().addError(null, 105,
									ai.name(), name(), cicurr.name());
							if (mc != null)
								mc.addDetail(null, 400, "Package",
										pkg().fullName());
						}
						if (ai.cardinality().maxOccurs > aicurr
								.cardinality().maxOccurs) {
							MessageContext mc = result().addError(null, 106,
									ai.name(), name(), cicurr.name());
							if (mc != null)
								mc.addDetail(null, 400, "Package",
										pkg().fullName());
						}
						if (ai instanceof PropertyInfoImpl) {
							((PropertyInfoImpl) ai).restriction = true;
						}
						result().addInfo(null, 1002, ai.name(), name(),
								cicurr.name());
					}
				}
			}
		}

		SortedSet<String> st = cicurr.supertypes();
		if (st != null) {
			for (Iterator<String> i = st.iterator(); i.hasNext();) {
				checkOverloading(model().classById(i.next()));
			}
		}
	}

	/**
	 * Postprocess the class to execute any actions that require that the
	 * complete model has been loaded. Validate the class against all applicable
	 * requirements and recommendations.
	 */
	public void postprocessAfterLoadingAndValidate() {
		if (postprocessed)
			return;

		super.postprocessAfterLoadingAndValidate();

		String s, s2;

		if (pkg() == null) {
			MessageContext mc = result().addError(null, 9, name());
			if (mc != null)
				mc.addDetail(null, 400, "Class", fullName());
		}

		/*
		 * Handle classes of UNKNOWN category
		 */
		if (matches("rule-xsd-cls-unknown-as-object")) {
			if (category == Options.UNKNOWN) {
				category = Options.OBJECT;
				MessageContext mc = result().addInfo(null, 1004, name());
				if (mc != null)
					mc.addDetail(null, 400, "Class", fullName());
			}
		}

		fixIfCategoryIsUnknown();

		identifyBasicTypes();

		/*
		 * identify restrictions
		 */
		if (category() == Options.FEATURE || category() == Options.OBJECT
				|| category() == Options.DATATYPE
				|| category() == Options.UNION && !asGroup()) {
			checkOverloading(this);
		}

		if (matches("req-xsd-cls-ncname")) {
			s = name();
			if (!XMLChar.isValidNCName(s)) {
				MessageContext mc = result().addError(null, 149, "class", s);
				if (mc != null)
					mc.addDetail(null, 400, "Class", fullName());
			}
		}

		if (matches("req-xsd-cls-mixin-supertypes")
				&& !matches("req-xsd-cls-mixin-supertypes-overrule")) {
			if (category() == Options.MIXIN) {
				if (!checkSupertypes(Options.MIXIN)) {
					MessageContext mc = result().addError(null, 115, name());
					if (mc != null)
						mc.addDetail(null, 400, "Class", fullName());
				}
			}
		}

		if (suppressed()) {
			if (matches("req-xsd-cls-suppress-supertype")) {
				if (unsuppressedSupertype(false) == null
						&& !stereotype("adeelement")) {
					MessageContext mc = result().addError(null, 142, name());
					if (mc != null)
						mc.addDetail(null, 400, "Class", fullName());
				}
			}
			if (matches("req-xsd-cls-suppress-subtype")) {
				boolean found = false;
				ClassInfo cix = null;
				for (String s0 : subtypes()) {
					cix = model().classById(s0);
					if (cix != null && !cix.suppressed()) {
						found = true;
						break;
					}
				}
				if (found) {
					MessageContext mc = result().addError(null, 143, name(),
							cix.name());
					if (mc != null)
						mc.addDetail(null, 400, "Class", fullName());
				}
			}
			if (matches("req-xsd-cls-suppress-no-properties")) {
				if (!(properties().isEmpty()) && !stereotype("adeelement")) {
					MessageContext mc = result().addError(null, 144, name());
					if (mc != null)
						mc.addDetail(null, 400, "Class", fullName());
				}
			}
		}

		switch (category()) {
		case Options.ENUMERATION:
			if (matches("req-xsd-cls-enum-no-supertypes")) {
				SortedSet<String> sc = supertypes();
				if (sc != null && sc.size() > 0) {
					MessageContext mc = result().addError(null, 125, name());
					if (mc != null)
						mc.addDetail(null, 400, "Class", fullName());
				}
			}
			break;
		case Options.CODELIST:
			if (matches("req-xsd-cls-codelist-no-supertypes")) {
				SortedSet<String> sc = supertypes();
				if (sc != null && sc.size() > 0) {
					MessageContext mc = result().addError(null, 127, name());
					if (mc != null)
						mc.addDetail(null, 400, "Class", fullName());
				}
			}
			if (matches("req-xsd-cls-codelist-asDictionary-true")) {
				s = taggedValue("asDictionary");
				if (s == null || s.isEmpty()) {
					MessageContext mc = result().addWarning(null, 200,
							"asDictionary", name());
					if (mc != null)
						mc.addDetail(null, 400, "Class", fullName());
				} else if (!s.equalsIgnoreCase("true")) {
					MessageContext mc = result().addError(null, 201,
							"asDictionary", name(), s);
					if (mc != null)
						mc.addDetail(null, 400, "Class", fullName());
				}
			}
			s = taggedValue("vocabulary");
			s2 = taggedValue("extensibility");
			if (matches("req-xsd-cls-codelist-extensibility-values")) {
				if (s2 != null && !s2.isEmpty() && !s2.equalsIgnoreCase("any")
						&& !s2.equalsIgnoreCase("narrower")) {
					MessageContext mc = result().addError(null, 201,
							"extensibility", name(), s2);
					if (mc != null)
						mc.addDetail(null, 400, "Class", fullName());
				}
			}
			if (matches("req-xsd-cls-codelist-extensibility-vocabulary")) {
				if (s2 != null && s2.equalsIgnoreCase("narrower")
						&& (s == null || s.isEmpty())) {
					MessageContext mc = result().addError(null, 201,
							"extensibility", name(), s2);
					if (mc != null)
						mc.addDetail(null, 400, "Class", fullName());
				}
			}
			break;
		case Options.DATATYPE:
		case Options.UNION:
			if (matches("req-xsd-cls-datatype-noPropertyType")) {
				s = taggedValue("noPropertyType");
				if (s == null || s.isEmpty()) {
					MessageContext mc = result().addWarning(null, 200,
							"noPropertyType", name());
					if (mc != null)
						mc.addDetail(null, 400, "Class", fullName());
				} else if (!s.equalsIgnoreCase("false")) {
					MessageContext mc = result().addError(null, 201,
							"noPropertyType", name(), s);
					if (mc != null)
						mc.addDetail(null, 400, "Class", fullName());
				}
			}
			break;
		case Options.MIXIN:
		case Options.OKSTRAFID:
		case Options.FEATURE:
		case Options.OBJECT:
			if (matches("req-xsd-cls-objecttype-noPropertyType")) {
				s = taggedValue("noPropertyType");
				if (s == null || s.isEmpty()) {
					MessageContext mc = result().addWarning(null, 200,
							"noPropertyType", name());
					if (mc != null)
						mc.addDetail(null, 400, "Class", fullName());
				} else if (!s.equalsIgnoreCase("false")) {
					MessageContext mc = result().addError(null, 201,
							"noPropertyType", name(), s);
					if (mc != null)
						mc.addDetail(null, 400, "Class", fullName());
				}
			}
			if (matches("req-xsd-cls-objecttype-byValuePropertyType")) {
				s = taggedValue("byValuePropertyType");
				if (s == null || s.isEmpty()) {
					MessageContext mc = result().addWarning(null, 200,
							"byValuePropertyType", name());
					if (mc != null)
						mc.addDetail(null, 400, "Class", fullName());
				} else if (!s.equalsIgnoreCase("false")) {
					MessageContext mc = result().addError(null, 201,
							"byValuePropertyType", name(), s);
					if (mc != null)
						mc.addDetail(null, 400, "Class", fullName());
				}
			}
			break;
		}
		;

		if (matches("req-xsd-cls-generalization-consistent")
				&& (category() == Options.DATATYPE
						|| category() == Options.FEATURE
						|| category() == Options.OBJECT)) {
			int count = 0;
			for (String cid : supertypes()) {
				ClassInfo ci = model().classById(cid);
				if (ci == null) {
					MessageContext mc = result().addError(null, 161, "Class",
							cid);
					if (mc != null)
						mc.addDetail(null, 400, "Class", fullName());
				} else {
					int cat = ci.category();
					if (cat == category()) {
						count++;
						if (count == 2) {
							if (this.model().isInSelectedSchemas(this)) {
								MessageContext mc = result().addError(null, 109,
										name());
								if (mc != null)
									mc.addDetail(null, 400, "Class",
											fullName());
							} else {
								/*
								 * 2015-07-17 JE: So this is a class that
								 * violates multiple inheritance rules. However,
								 * it is outside the selected schemas. We could
								 * log a debug, info, or even warning message.
								 * However, we should not raise an error because
								 * creation of a complete GenericModel that also
								 * copies ISO classes would raise an error which
								 * would cause a unit test to fail.
								 */
							}
						}
					} else if (cat == Options.MIXIN
							&& matches("rule-xsd-cls-mixin-classes")) {
						// nothing to do
					} else {

						if (this.model().isInSelectedSchemas(this)) {
							MessageContext mc = result().addError(null, 108,
									name());
							if (mc != null)
								mc.addDetail(null, 400, "Class", fullName());
						} else {
							/*
							 * 2015-07-17 JE: So this is a class that violates
							 * multiple inheritance rules. However, it is
							 * outside the selected schemas. We could log a
							 * debug, info, or even warning message. However, we
							 * should not raise an error because creation of a
							 * complete GenericModel that also copies ISO
							 * classes would raise an error which would cause a
							 * unit test to fail.
							 */
						}
					}
				}
			}
		}

		if (matches("req-all-all-documentation")) {
			s = documentation();
			if (!s.contains(options().nameSeparator())) {
				MessageContext mc = result().addError(null, 152, name(),
						options().nameSeparator());
				if (mc != null)
					mc.addDetail(null, 400, "Class", fullName());
			}
			if (!s.contains(options().definitionSeparator())) {
				MessageContext mc = result().addError(null, 152, name(),
						options().definitionSeparator());
				if (mc != null)
					mc.addDetail(null, 400, "Class", fullName());
			}
		}

		postprocessed = true;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public boolean isUnionDirect() {
		return category() == Options.UNION
				&& matches("rule-xsd-cls-union-direct") && hasNilReason()
				&& properties().size() == 2;
	}

	@Override
	public List<ImageMetadata> getDiagrams() {
		return diagrams;
	}

	@Override
	public void setDiagrams(List<ImageMetadata> diagrams) {
		this.diagrams = diagrams;
	}

	@Override
	public File getLinkedDocument() {
		return linkedDocument;
	}

	@Override
	public void setLinkedDocument(File linkedDocument) {
		this.linkedDocument = linkedDocument;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public boolean hasNilReason() {

		for (PropertyInfo pi : properties().values()) {
			if (pi.implementedByNilReason())
				return true;
		}

		return false;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public SortedSet<ClassInfo> subtypesInCompleteHierarchy() {

		SortedSet<ClassInfo> result = new TreeSet<ClassInfo>();

		for (String subtypeId : this.subtypes()) {

			ClassInfo subtype = model().classById(subtypeId);

			result.add(subtype);
			result.addAll(subtype.subtypesInCompleteHierarchy());
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public SortedSet<ClassInfo> supertypesInCompleteHierarchy() {

		SortedSet<ClassInfo> result = new TreeSet<ClassInfo>();

		for (String supertypeId : this.supertypes()) {

			ClassInfo supertype = model().classById(supertypeId);

			result.add(supertype);
			result.addAll(supertype.supertypesInCompleteHierarchy());
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public Profiles profiles() {

		if (this.profiles == null) {

			// attempt to parse from profiles tagged value
			String profilesTV = this
					.taggedValue(Profiles.PROFILES_TAGGED_VALUE);

			if (profilesTV == null || profilesTV.trim().length() == 0) {

				// No specific profiles declared, which is valid.
				this.profiles = new Profiles();

			} else {

				try {

					Profiles tmp = Profiles.parse(profilesTV, false);

					this.profiles = tmp;

				} catch (MalformedProfileIdentifierException e) {

					MessageContext mc = result().addWarning(null, 20201);
					if (mc != null) {
						mc.addDetail(null, 20216, fullNameInSchema());
						mc.addDetail(null, 20217, e.getMessage());
						mc.addDetail(null, 20218, profilesTV);
					}
					this.profiles = new Profiles();
				}
			}
		}

		return this.profiles;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 */
	@Override
	public PropertyInfo ownedProperty(String name) {

		for (PropertyInfo pi : properties().values()) {

			if (pi.name().equals(name)) {

				return pi;
			}
		}

		return null;
	}
}
