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

package de.interactive_instruments.ShapeChange.Target.XmlSchema;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.serializer.OutputPropertiesFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import de.interactive_instruments.ShapeChange.MapEntry;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.Target.Target;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.OclConstraint;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;

public class XmlSchema implements Target {

	private ShapeChangeResult result = null;
	private PackageInfo pi = null;
	private Model model = null;
	private Options options = null;
	private boolean printed = false;
	private final HashMap<String, XsdDocument> xsdMap = new HashMap<String, XsdDocument>();
	private SchematronSchema schDoc = null;
	private boolean diagnosticsOnly = false;
	private String outputDirectory;

	public void initialise(PackageInfo p, Model m, Options o,
			ShapeChangeResult r, boolean diagOnly)
			throws ShapeChangeAbortException {

		pi = p;
		model = m;
		options = o;
		result = r;
		diagnosticsOnly = diagOnly;

		if (pi.matches("rule-xsd-all-notEncoded")
				&& pi.encodingRule("xsd").equalsIgnoreCase("notencoded"))
			return;

		result.addDebug(null, 10012, pi.name());

		outputDirectory = options.parameter(this.getClass().getName(),
				"outputDirectory");
		if (outputDirectory == null)
			outputDirectory = options.parameter("outputDirectory");
		if (outputDirectory == null)
			outputDirectory = options.parameter(".");

		if (pi.matches("rule-xsd-pkg-schematron"))
			schDoc = new SchematronSchema(model, options, result, pi);

		/** Create XML Schema documents */
		createXSDs(pi, null);

		if (pi.matches("rule-xsd-pkg-dependencies")) {
			processDependecies(pi);
		}

		// create output directory, if necessary
		if (!this.diagnosticsOnly) {
			File trgdir = new File(outputDirectory);
			if (!trgdir.exists()) {
				trgdir.mkdirs();
			}
		}
	}

	public void process(ClassInfo ci) {
		if (ci == null || ci.pkg() == null)
			return;

		int cat = ci.category();
		result.addDebug(null, 10016, ci.name(), ci.encodingRule("xsd"));

		if (ci.matches("rule-xsd-all-notEncoded")
				&& ci.encodingRule("xsd").equalsIgnoreCase("notencoded"))
			return;

		PackageInfo pi = ci.pkg();
		XsdDocument xsd = xsdMap.get(pi.id());
		while (xsd == null) {
			pi = pi.owner();
			if (pi == null) {
				MessageContext mc = result.addError(null, 10, ci.name(),
						ci.pkg().name());
				if (mc != null)
					mc.addDetail(null, 400, "Class", ci.fullName());
				return;
			}
			xsd = xsdMap.get(pi.id());
		}

		ClassInfo cibase = ci.baseClass();

		if (ci.matches("rule-xsd-cls-no-base-class")) {
			cibase = null;
		}

		if (schDoc != null) {
			List<Constraint> cs = ci.constraints();

			Collections.sort(cs, new Comparator<Constraint>() {
				public int compare(Constraint ci1, Constraint ci2) {
					return ci1.name().compareTo(ci2.name());
				}
			});

			if (cs != null) {
				for (Iterator<Constraint> i = cs.iterator(); i.hasNext();) {
					Constraint c = i.next();
					if (c != null && c instanceof OclConstraint
							&& schDoc != null
							&& ((OclConstraint) c).syntaxTree() != null) {
						schDoc.addAssertion(ci, (OclConstraint) c);
					}
				}
			}
		}

		// Object element
		switch (cat) {
		case Options.ENUMERATION:
		case Options.CODELIST:
			if (ci.matches("rule-xsd-cls-enum-object-element")) {
				xsd.pObjectElement(ci, cibase);
			}
			break;
		case Options.UNION:
			if ((ci.matches("rule-xsd-cls-union-asGroup") && ci.asGroup())
					|| (ci.matches("rule-xsd-cls-union-asCharacterString")
							&& ci.asCharacterString())) {
				break;
			}
			if (ci.matches("rule-xsd-cls-union-as-group-property-type"))
				break;
			if (ci.isUnionDirect())
				break;
			// FIXME TB13TBD
			if (ci.matches("rule-xsd-cls-union-omitUnionsRepresentingTypeSets")
					&& "true".equalsIgnoreCase(
							ci.taggedValue("representsTypeSet")))
				break;
		case Options.OKSTRAFID:
		case Options.FEATURE:
		case Options.OBJECT:
		case Options.DATATYPE:
			if (ci.matches("rule-xsd-cls-no-abstract-classes")
					&& ci.isAbstract())
				break;
			if (ci.matches("rule-xsd-cls-suppress") && ci.suppressed())
				break;
			if (ci.matches("rule-xsd-cls-object-element"))
				xsd.pObjectElement(ci, cibase);
			break;
		case Options.MIXIN:
			if (ci.matches("rule-xsd-cls-mixin-classes"))
				break;
		}
		;

		// Content model
		switch (cat) {
		case Options.ENUMERATION:
			if (ci.matches("rule-xsd-cls-global-enumeration"))
				xsd.pGlobalEnumeration(ci);
			break;
		case Options.CODELIST:
			if ((ci.matches("rule-xsd-cls-codelist-asDictionary")
					&& !ci.asDictionary())
					|| (ci.matches("rule-xsd-cls-codelist-asDictionaryGml33")
							&& !ci.asDictionaryGml33()))
				xsd.pGlobalCodeList(ci);
			break;
		case Options.UNION:
			// FIXME TB13TBD
			if (ci.matches("rule-xsd-cls-union-omitUnionsRepresentingTypeSets")
					&& "true".equalsIgnoreCase(
							ci.taggedValue("representsTypeSet")))
				break;
			if ((ci.matches("rule-xsd-cls-union-asGroup") && ci.asGroup())
					|| (ci.matches("rule-xsd-cls-union-asCharacterString")
							&& ci.asCharacterString()))
				break;
			if (ci.matches("rule-xsd-cls-union-as-group-property-type")) {
				xsd.pValueTypeGroup(ci);
				break;
			}
			if (ci.isUnionDirect())
				break;
		case Options.OKSTRAFID:
		case Options.FEATURE:
		case Options.OBJECT:
		case Options.DATATYPE:
			if (ci.matches("rule-xsd-cls-adeelement")
					&& ci.stereotype("adeelement")) {
				xsd.processLocalProperties(ci, xsd.root, schDoc);
				break;
			}
			if (ci.matches("rule-xsd-cls-suppress") && ci.suppressed())
				break;
			if (ci.matches("rule-xsd-cls-no-abstract-classes")
					&& ci.isAbstract())
				break;
			if (ci.matches("rule-xsd-cls-type")) {
				Element propertyHook = xsd.pComplexType(ci, cibase, schDoc);
				if (ci.matches("rule-xsd-cls-local-properties")) {
					xsd.processLocalProperties(ci, propertyHook, schDoc);
				}
			}
			break;
		case Options.MIXIN:
			if (ci.matches("rule-xsd-cls-mixin-classes"))
				break;
		}

		// Property types
		switch (cat) {
		case Options.ENUMERATION:
		case Options.CODELIST:
			if (ci.matches("rule-xsd-cls-enum-property-type"))
				xsd.pPropertyTypes(ci);
			break;
		case Options.UNION:
			if ((ci.matches("rule-xsd-cls-union-asGroup") && ci.asGroup())
					|| (ci.matches("rule-xsd-cls-union-asCharacterString")
							&& ci.asCharacterString()))
				break;
			if (ci.matches("rule-xsd-cls-union-as-group-property-type")) {
				xsd.pPropertyTypeWithGroup(ci);
				break;
			}
			if (ci.isUnionDirect())
				break;
			// FIXME TB13TBD
			if (ci.matches("rule-xsd-cls-union-omitUnionsRepresentingTypeSets")
					&& "true".equalsIgnoreCase(
							ci.taggedValue("representsTypeSet")))
				break;
		case Options.FEATURE:
		case Options.OBJECT:
		case Options.DATATYPE:
			if (ci.matches("rule-xsd-cls-suppress") && ci.suppressed())
				break;
			if (ci.matches("rule-xsd-cls-no-abstract-classes")
					&& ci.isAbstract())
				xsd.pPropertyTypeWithSubtypes(ci);
			else if (ci.matches("rule-xsd-cls-type"))
				xsd.pPropertyTypes(ci);
			break;
		case Options.MIXIN:
			if (ci.matches("rule-xsd-cls-mixin-classes"))
				xsd.pPropertyTypeWithSubtypes(ci);
			break;
		case Options.OKSTRAKEY:
			if (ci.matches("rule-xsd-cls-okstra-schluesseltabelle"))
				xsd.pOKSTRAKEYPropertyType(ci);
			break;
		}
		;

		// Separate content model
		switch (cat) {
		case Options.UNION:
			// FIXME TB13TBD
			if (ci.matches("rule-xsd-cls-union-omitUnionsRepresentingTypeSets")
					&& "true".equalsIgnoreCase(
							ci.taggedValue("representsTypeSet")))
				break;
			if (ci.matches("rule-xsd-cls-union-asGroup") && ci.asGroup()) {
				Element propertyHook = xsd.pGroup(ci, cibase);
				xsd.processLocalProperties(ci, propertyHook, schDoc);
			}
			break;
		case Options.MIXIN:
			if (ci.matches("rule-xsd-cls-mixin-classes")
					&& ci.matches("rule-xsd-cls-mixin-classes-as-group")) {
				Element propertyHook = xsd.pGroup(ci, cibase);
				xsd.processLocalProperties(ci, propertyHook, schDoc);
			}
			break;
		case Options.BASICTYPE:
			if (ci.matches("rule-xsd-cls-basictype")
					&& !ci.matches("rule-xsd-cls-local-basictype")) {
				xsd.pGlobalBasicType(ci);
			}
			break;
		}
	}

	public void write() {
		if (printed) {
			return;
		}
		if (diagnosticsOnly) {
			return;
		}

		Properties outputFormat = OutputPropertiesFactory
				.getDefaultMethodProperties("xml");
		outputFormat.setProperty("indent", "yes");
		outputFormat.setProperty("{http://xml.apache.org/xalan}indent-amount",
				"2");
		outputFormat.setProperty("encoding", "UTF-8");

		for (Iterator<XsdDocument> i = xsdMap.values().iterator(); i
				.hasNext();) {
			XsdDocument xsd = i.next();
			if (!xsd.printed()) {
				try {
					xsd.printFile(outputFormat);
					result.addResult(getTargetName(), outputDirectory, xsd.name,
							pi.targetNamespace());
				} catch (Exception e) {
					String m = e.getMessage();
					if (m != null) {
						result.addError(m);
					}
					e.printStackTrace(System.err);
				}
			}
		}

		if (schDoc != null)
			schDoc.write(outputDirectory);

		printed = true;
	};

	/** Create XML Schema documents */
	protected boolean createXSDs(PackageInfo pi, XsdDocument xsdcurr)
			throws ShapeChangeAbortException {
		boolean res = false;

		/**
		 * Determine and if necessary create XML Schema document for this
		 * package
		 */
		XsdDocument xsd;
		String xsdDocument = pi.xsdDocument();
		if (xsdDocument != null && xsdDocument.length() > 0) {
			try {
				result.addDebug(null, 10017, xsdDocument, pi.name());
				xsd = new XsdDocument(pi, model, options, result, xsdDocument);
				res = true;
			} catch (ParserConfigurationException e) {
				result.addFatalError(null, 2);
				throw new ShapeChangeAbortException();
			}
		} else {
			xsd = xsdcurr;
			if (xsd == null) {
				result.addFatalError(null, 15, pi.name());
				// throw new ShapeChangeAbortException();
				xsdDocument = pi.name() + ".xsd";
				try {
					result.addDebug(null, 10017, xsdDocument, pi.name());
					xsd = new XsdDocument(pi, model, options, result,
							xsdDocument);
					res = true;
				} catch (ParserConfigurationException e) {
					result.addFatalError(null, 2);
					throw new ShapeChangeAbortException();
				}
			}
		}
		xsdMap.put(pi.id(), xsd);

		/**
		 * Navigate through sub packages and create and include XML Schema
		 * documents
		 */
		if (pi.matches("rule-xsd-pkg-contained-packages")) {
			try {
				SortedSet<PackageInfo> sub = pi.containedPackages();
				for (Iterator<PackageInfo> i = sub.iterator(); i.hasNext();) {
					PackageInfo pix = i.next();
					if (pix.isSchema()) {
						xsd.addImport(pix.xmlns(), pix.targetNamespace());
					} else {
						boolean created = createXSDs(pix, xsd);
						if (created) {
							xsd.addInclude(xsdMap.get(pix.id()));
							xsdMap.get(pix.id()).addInclude(xsd);
						}
					}
				}
			} catch (Exception e) {
				String m = e.getMessage();
				if (m != null) {
					result.addError(m);
				}
				Exception se = e;
				if (e instanceof SAXException) {
					se = ((SAXException) e).getException();
				}
				if (se != null) {
					se.printStackTrace(System.err);
				} else {
					e.printStackTrace(System.err);
				}
				throw new ShapeChangeAbortException();
			}
		}

		return res;
	}

	/** Process dependency relationships with other packages */
	protected void processDependecies(PackageInfo pi)
			throws ShapeChangeAbortException {
		XsdDocument xsd1 = xsdMap.get(pi.id());
		SortedSet<String> suppliers = pi.supplierIds();
		if (suppliers != null) {
			for (Iterator<String> i = suppliers.iterator(); i.hasNext();) {
				String pid = i.next();
				XsdDocument xsd2 = xsdMap.get(pid);
				if (xsd2 != null) {
					xsd1.addInclude(xsd2);
				} else {
					PackageInfo pi2 = model.packageById(pid);
					if (pi2 != null && pi2.xmlns() != null
							&& pi2.xmlns().length() > 0
							&& pi2.targetNamespace() != null
							&& pi2.targetNamespace().length() > 0)
						xsd1.addImport(pi2.xmlns(), pi2.targetNamespace());
				}
			}
		}

		/**
		 * Navigate through sub packages and import/include XML Schema documents
		 */
		try {
			SortedSet<PackageInfo> sub = pi.containedPackages();
			for (Iterator<PackageInfo> i = sub.iterator(); i.hasNext();) {
				PackageInfo pix = i.next();
				if (!pix.isSchema()) {
					processDependecies(pix);
				}
			}
		} catch (Exception e) {
			String m = e.getMessage();
			if (m != null) {
				result.addError(m);
			}
			e.printStackTrace(System.err);
			throw new ShapeChangeAbortException();
		}
	}

	@Override
	public String getTargetName() {
		return "XML Schema";
	}

	/**
	 * Find out whether the given class is represented by means of an XML
	 * element construct.
	 * 
	 * @param ci
	 *            ClassInfo of class to be inquired
	 * @return Flag returning the requested information
	 */
	static public boolean classHasObjectElement(ClassInfo ci) {
		// test, if we have a map entry to an element for this encoding rule
		MapEntry me = ci.options().elementMapEntry(ci.name(),
				ci.encodingRule("xsd"));
		if (me != null) {
			return me.p1 != null && me.p1.length() > 0;
		}

		// We don't. Before checking the stereotype we first have to look for a
		// map entry
		// that maps the class to a simple type or attribute
		me = ci.options().typeMapEntry(ci.name(), ci.encodingRule("xsd"));
		if (me != null) {
			// Yes. As we had no element map entry the type content must be
			// simple
			if (me.rule == null || !me.rule.equalsIgnoreCase("direct")) {
				// as a static function we cannot write an error message
			} else if (me.p2 == null
					|| !(me.p2.equalsIgnoreCase("complex/simple")
							|| me.p2.equalsIgnoreCase("simple/simple"))) {
				// as a static function we cannot write an error message
			} else {
				// We know this is a type with simple content
				return false;
			}
		}

		me = ci.options().attributeMapEntry(ci.name(), ci.encodingRule("xsd"));
		if (me != null) {
			// We know this is just an attribute in XML
			return false;
		}

		me = ci.options().attributeGroupMapEntry(ci.name(),
				ci.encodingRule("xsd"));
		if (me != null) {
			// We know this is just an attribute group in XML
			return false;
		}

		int cat = ci.category();
		if (ci.encodingRule("xsd").equals(Options.ISO19139_2007)) {
			return cat == Options.DATATYPE || cat == Options.UNION
					|| cat == Options.FEATURE || cat == Options.OBJECT
					|| cat == Options.ENUMERATION || cat == Options.CODELIST
					|| cat == Options.UNKNOWN;
		} else {
			return cat == Options.DATATYPE
					&& !ci.matches("rule-all-cls-aixmDatatype")
					|| cat == Options.UNION
							&& !(ci.matches("rule-xsd-cls-union-asGroup")
									&& ci.asGroup())
					|| cat == Options.FEATURE || cat == Options.OBJECT
					|| cat == Options.UNKNOWN || (cat == Options.OKSTRAFID
							&& ci.matches("rule-xsd-cls-okstra-fid"));
		}
	}

	public static boolean implementedAsXmlAttribute(PropertyInfo pi) {

		// TBD: requires specific mapping info if the inClass of pi is given by
		// a MapEntry

		boolean result = false;

		if (pi.name().equals("uom")
				&& pi.matches("rule-all-prop-uomAsAttribute")) {

			result = true;

		} else if (pi.matches("rule-xsd-prop-xsdAsAttribute")) {

			result = true;
		}

		return result;
	}

	/**
	 * Find out whether the given class is can carry an id and can hence be
	 * referenced by means of xlink:href.
	 * 
	 * @param ci
	 *            ClassInfo of class to be inquired
	 * @return Flag returning the requested information
	 */
	static public boolean classCanBeReferenced(ClassInfo ci) {
		if (classHasObjectElement(ci)) {
			int cat = ci.category();
			if (cat == Options.FEATURE)
				return true;
			if (cat == Options.OKSTRAFID)
				return true;
			if (cat == Options.OBJECT)
				return true;
			if (ci.encodingRule("xsd").equals(Options.ISO19139_2007)) {
				if (cat == Options.DATATYPE)
					return true;
				if (cat == Options.UNION)
					return true;
			}
		}
		return false;
	}
}
