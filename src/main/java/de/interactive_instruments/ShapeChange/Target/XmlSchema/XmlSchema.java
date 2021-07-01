/**
a * ShapeChange - processing application schemas for geographic information
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
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.xml.serializer.OutputPropertiesFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import de.interactive_instruments.ShapeChange.MapEntry;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.RuleRegistry;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.TargetXmlSchemaConfiguration;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.OclConstraint;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.Target;

public class XmlSchema implements Target, MessageSource {

    private ShapeChangeResult result = null;
    private PackageInfo pi = null;
    private Model model = null;
    private Options options = null;
    private boolean printed = false;
    private final HashMap<String, XsdDocument> xsdMap = new HashMap<String, XsdDocument>();
//    protected SchematronSchema schDoc = null;
    protected SchematronSchema commonSchDoc = null;
    private boolean diagnosticsOnly = false;
    private String outputDirectory;
    protected ClassInfo defaultVoidReasonType = null;
    private TargetXmlSchemaConfiguration config;
    protected String explicitSchematronQueryBinding;
    protected boolean schematronSegmentation = false;

    public void initialise(PackageInfo p, Model m, Options o, ShapeChangeResult r, boolean diagOnly)
	    throws ShapeChangeAbortException {

	pi = p;
	model = m;
	options = o;
	result = r;
	diagnosticsOnly = diagOnly;

	if (pi.matches("rule-xsd-all-notEncoded") && pi.encodingRule("xsd").equalsIgnoreCase("notencoded"))
	    return;

	result.addDebug(this, 10012, pi.name());

	config = (TargetXmlSchemaConfiguration) o.getCurrentProcessConfig();

	outputDirectory = options.parameter(this.getClass().getName(), "outputDirectory");
	if (outputDirectory == null)
	    outputDirectory = options.parameter("outputDirectory");
	if (outputDirectory == null)
	    outputDirectory = options.parameter(".");

	String voidReasonType = options.parameterAsString(this.getClass().getName(),
		XmlSchemaConstants.PARAM_DEFAULT_VOID_REASON_TYPE, null, false, true);
	if (StringUtils.isBlank(voidReasonType)) {
	    defaultVoidReasonType = null;
	} else {
	    defaultVoidReasonType = findClassByFullNameInSchemaInModelOrByNameInSchema(voidReasonType);
	    if (defaultVoidReasonType == null) {
		result.addInfo(this, 2012, voidReasonType);
	    }
	}

	explicitSchematronQueryBinding = options.parameterAsString(this.getClass().getName(),
		XmlSchemaConstants.PARAM_SCH_QUERY_BINDING, null, false, true);

	schematronSegmentation = options.parameterAsBoolean(this.getClass().getName(),
		XmlSchemaConstants.PARAM_SEGMENT_SCH, false);

	if (pi.matches("rule-xsd-pkg-schematron") && !schematronSegmentation) {

	    String schemaXsdDocument = pi.xsdDocument();
	    String schemaXsdBaseName = FilenameUtils.getBaseName(schemaXsdDocument);

	    if ("xslt2".equalsIgnoreCase(explicitSchematronQueryBinding)) {
		commonSchDoc = new SchematronSchemaXslt2(model, options, result, pi, schemaXsdBaseName,
			schematronSegmentation);
	    } else {
		commonSchDoc = new SchematronSchemaOld(model, options, result, pi, schemaXsdBaseName,
			schematronSegmentation);
	    }
	}

	/** Create XML Schema documents */
	createXSDs(pi, null);

	if (pi.matches("rule-xsd-pkg-dependencies")) {
	    processDependecies(pi);
	}

	if (StringUtils.isNotBlank(pi.taggedValue("xsdForcedImports"))) {
	    String[] nsabrForcedImports = pi.taggedValue("xsdForcedImports").split("\\s*,\\s*");
	    XsdDocument xsd = xsdMap.get(pi.id());
	    for (String nsabr : nsabrForcedImports) {
		String ns = options.fullNamespace(nsabr);
		if (StringUtils.isBlank(ns)) {
		    result.addError(this, 1001, nsabr, pi.name());
		} else {
		    xsd.addImport(nsabr, ns);
		}
	    }
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
	result.addDebug(this, 10016, ci.name(), ci.encodingRule("xsd"));

	if (ci.matches("rule-xsd-all-notEncoded") && ci.encodingRule("xsd").equalsIgnoreCase("notencoded"))
	    return;

	PackageInfo pi = ci.pkg();
	XsdDocument xsd = xsdMap.get(pi.id());
	while (xsd == null) {
	    pi = pi.owner();
	    if (pi == null) {
		MessageContext mc = result.addError(this, 10, ci.name(), ci.pkg().name());
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

	SchematronSchema schDoc = xsd.getSchematronDocument();
	if (schDoc != null) {

	    List<Constraint> cs = ci.constraints();

	    Collections.sort(cs, new Comparator<Constraint>() {
		public int compare(Constraint ci1, Constraint ci2) {
		    return ci1.name().compareTo(ci2.name());
		}
	    });

	    for (Constraint c : cs) {
		if (c != null && c instanceof OclConstraint && schDoc != null
			&& ((OclConstraint) c).syntaxTree() != null) {

		    schDoc.addAssertion(ci, (OclConstraint) c);
		}
	    }

	    for (PropertyInfo propi : ci.properties().values()) {

		// first create value or nil reason assertions, if necessary
		if (!(ci.category() == Options.CODELIST || ci.category() == Options.ENUMERATION)
			&& ((propi.nilReasonAllowed() && propi.matches("rule-xsd-prop-nilReasonAllowed"))
				|| (propi.voidable() && propi.matches("rule-xsd-prop-nillable")))) {

		    if (propi.matches("rule-xsd-prop-valueOrNilReason-constraints")) {
			addAssertionForValueOrNilReason(ci, propi, model.classByIdOrName(propi.typeInfo()), schDoc);
		    }

		    if (propi.matches("rule-xsd-prop-nilReason-constraints")) {

			String voidReasonType = propi.taggedValue("voidReasonType");
			ClassInfo directVoidReasonType;
			if (StringUtils.isBlank(voidReasonType)) {
			    directVoidReasonType = null;
			} else {
			    directVoidReasonType = findClassByFullNameInSchemaInModelOrByNameInSchema(voidReasonType);
			    if (directVoidReasonType == null) {
				MessageContext mc = result.addInfo(this, 2013, propi.name(), propi.inClass().name(),
					voidReasonType);
				if (mc != null) {
				    mc.addDetail(this, 0, propi.fullName());
				}
			    }
			}

			ClassInfo vrt = directVoidReasonType;
			if (vrt == null) {
			    vrt = defaultVoidReasonType;
			}

			if (vrt != null) {

			    if (vrt.category() == Options.ENUMERATION && vrt.properties().size() > 0) {
				addAssertionForNilReasonCheck(ci, propi, vrt, schDoc);
			    } else {
				MessageContext mc = result.addWarning(this, 2009, propi.name(), vrt.name());
				if (mc != null) {
				    mc.addDetail(this, 0, propi.fullNameInSchema());
				}
			    }

			} else {
			    MessageContext mc = result.addWarning(this, 2010, propi.name());
			    if (mc != null) {
				mc.addDetail(this, 0, propi.fullNameInSchema());
			    }
			}
		    }
		}

		// now handle constraints defined on the property
		List<Constraint> propiCs = propi.constraints();

		Collections.sort(propiCs, new Comparator<Constraint>() {
		    public int compare(Constraint ci1, Constraint ci2) {
			return ci1.name().compareTo(ci2.name());
		    }
		});

		for (Constraint c : propiCs) {
		    if (c != null && c instanceof OclConstraint && schDoc != null
			    && ((OclConstraint) c).syntaxTree() != null) {
			schDoc.addAssertionForPropertyConstraint((OclConstraint) c, null, true);
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
		    || (ci.matches("rule-xsd-cls-union-asCharacterString") && ci.asCharacterString())) {
		break;
	    }
	    if (ci.matches("rule-xsd-cls-union-as-group-property-type"))
		break;
	    if (ci.isUnionDirect())
		break;
	    if (ci.matches("rule-xsd-cls-union-omitUnionsRepresentingFeatureTypeSets")
		    && "true".equalsIgnoreCase(ci.taggedValue("representsFeatureTypeSet")))
		break;
	case Options.OKSTRAFID:
	case Options.FEATURE:
	case Options.OBJECT:
	case Options.DATATYPE:
	    if (ci.matches("rule-xsd-cls-no-abstract-classes") && ci.isAbstract())
		break;
	    if (ci.matches("rule-xsd-cls-suppress") && ci.suppressed())
		break;
	    if (ci.matches("rule-xsd-cls-object-element")) {
		xsd.pObjectElement(ci, cibase);
	    }
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
	    if ((ci.matches("rule-xsd-cls-codelist-asDictionary") && !ci.asDictionary())
		    || (ci.matches("rule-xsd-cls-codelist-asDictionaryGml33") && !ci.asDictionaryGml33()))
		xsd.pGlobalCodeList(ci);
	    break;
	case Options.UNION:
	    if (ci.matches("rule-xsd-cls-union-omitUnionsRepresentingFeatureTypeSets")
		    && "true".equalsIgnoreCase(ci.taggedValue("representsFeatureTypeSet")))
		break;
	    if ((ci.matches("rule-xsd-cls-union-asGroup") && ci.asGroup())
		    || (ci.matches("rule-xsd-cls-union-asCharacterString") && ci.asCharacterString()))
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
	    if (ci.matches("rule-xsd-cls-adeelement") && ci.stereotype("adeelement")) {
		xsd.processLocalProperties(ci, xsd.root, schDoc);
		break;
	    }
	    if (ci.matches("rule-xsd-cls-suppress") && ci.suppressed())
		break;
	    if (ci.matches("rule-xsd-cls-no-abstract-classes") && ci.isAbstract())
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
		    || (ci.matches("rule-xsd-cls-union-asCharacterString") && ci.asCharacterString()))
		break;
	    if (ci.matches("rule-xsd-cls-union-as-group-property-type")) {
		xsd.pPropertyTypeWithGroup(ci);
		break;
	    }
	    if (ci.isUnionDirect())
		break;
	    if (ci.matches("rule-xsd-cls-union-omitUnionsRepresentingFeatureTypeSets")
		    && "true".equalsIgnoreCase(ci.taggedValue("representsFeatureTypeSet")))
		break;
	case Options.FEATURE:
	case Options.OBJECT:
	case Options.DATATYPE:
	    if (ci.matches("rule-xsd-cls-suppress") && ci.suppressed())
		break;
	    if (ci.matches("rule-xsd-cls-no-abstract-classes") && ci.isAbstract())
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
	    if (ci.matches("rule-xsd-cls-union-omitUnionsRepresentingFeatureTypeSets")
		    && "true".equalsIgnoreCase(ci.taggedValue("representsFeatureTypeSet")))
		break;
	    if (ci.matches("rule-xsd-cls-union-asGroup") && ci.asGroup()) {
		Element propertyHook = xsd.pGroup(ci, cibase);
		xsd.processLocalProperties(ci, propertyHook, schDoc);
	    }
	    break;
	case Options.MIXIN:
	    if (ci.matches("rule-xsd-cls-mixin-classes") && ci.matches("rule-xsd-cls-mixin-classes-as-group")) {
		Element propertyHook = xsd.pGroup(ci, cibase);
		xsd.processLocalProperties(ci, propertyHook, schDoc);
	    }
	    break;
	case Options.BASICTYPE:
	    if (ci.matches("rule-xsd-cls-basictype") && !ci.matches("rule-xsd-cls-local-basictype")) {
		xsd.pGlobalBasicType(ci);
	    }
	    break;
	}
    }

    /**
     * Looks up a class, given its name or full name in schema. If the class name
     * contains '::', then it is assumed to represent a full name of the class
     * within its schema. The class will then be looked up by this name within the
     * model. Otherwise, the name is assumed to be the name of a class within the
     * schema that is currently processed. Therefore, the class will be searched
     * within the schema.
     * 
     * @param className either the simple class name, or the full name of a class
     *                  within its schema
     * @return the class, if found, else <code>null</code>
     */
    ClassInfo findClassByFullNameInSchemaInModelOrByNameInSchema(String className) {

	if (className.contains("::")) {
	    return model.classByFullNameInSchema(className);
	} else {
	    return model.classes(pi).stream().filter(schemaCi -> schemaCi.name().equalsIgnoreCase(className))
		    .findFirst().orElse(null);
	}
    }

    /**
     * @param cibase class that owns the property
     * @param propi  the property
     * @param schDoc
     */
    private void addAssertionForNilReasonCheck(ClassInfo cibase, PropertyInfo propi, ClassInfo voidReasonType,
	    SchematronSchema schDoc) {

	// First, check cases in which no schematron assertion will be created.

	// check if the property itself is encoded as a simple attribute
	if (pi.matches("rule-xsd-prop-xsdAsAttribute")) {
	    MessageContext mc = result.addDebug(this, 2006, propi.name(), propi.inClass().name());
	    if (mc != null) {
		mc.addDetail(this, 0, propi.fullName());
	    }
	    return;
	}

	// check if cibase is encoded as an attribute group or as a string
	if (cibase.category() == Options.UNION && ((cibase.matches("rule-xsd-cls-union-asCharacterString")
		&& "true".equalsIgnoreCase(cibase.taggedValue("gmlAsCharacterString")))
		|| (cibase.matches("rule-xsd-cls-union-asGroup")
			&& "true".equalsIgnoreCase(cibase.taggedValue("gmlAsGroup"))))) {
	    MessageContext mc = result.addDebug(this, 2007, propi.name(), propi.inClass().name());
	    if (mc != null) {
		mc.addDetail(this, 0, propi.fullName());
	    }
	    return;
	}

	// check if cibase is a mixin encoded as an attribute group
	if (cibase.category() == Options.MIXIN && cibase.matches("rule-xsd-cls-mixin-classes-as-group")) {
	    MessageContext mc = result.addDebug(this, 2008, propi.name(), propi.inClass().name());
	    if (mc != null) {
		mc.addDetail(this, 0, propi.fullName());
	    }
	    return;
	}

	schDoc.setQueryBinding("xslt2");

	String enumSequence = voidReasonType.properties().values().stream().map(enumPi -> "'" + enumPi.name() + "'")
		.sorted().collect(Collectors.joining(", "));

	String xpath = "not(@*:nilReason) or @*:nilReason = (" + enumSequence + ")";

	XpathFragment xpathNilReasonCheck = new XpathFragment(0, xpath, XpathType.BOOLEAN);

	String assertionText = "If a nil reason is given for property " + propi.name()
		+ ", then it needs to be one of: " + enumSequence;

	schDoc.addAssertionForExplicitProperty(cibase, propi, true, xpathNilReasonCheck, assertionText);

    }

    /**
     * @param cibase class that owns the property
     * @param propi  the property
     * @param typeCi value type of the property
     * @param schDoc
     */
    private void addAssertionForValueOrNilReason(ClassInfo cibase, PropertyInfo propi, ClassInfo typeCi,
	    SchematronSchema schDoc) {

	// First, check cases in which no schematron assertion will be created.

	// check if the property itself is encoded as a simple attribute
	if (propi.matches("rule-xsd-prop-xsdAsAttribute")) {
	    MessageContext mc = result.addDebug(this, 2002, propi.name(), propi.inClass().name());
	    if (mc != null) {
		mc.addDetail(this, 0, propi.fullName());
	    }
	    return;
	}

	// check if the value type of the property is encoded as an attribute (group)
	MapEntry mea = options.attributeMapEntry(propi.typeInfo().name, cibase.encodingRule("xsd"));
	MapEntry meag = options.attributeGroupMapEntry(propi.typeInfo().name, cibase.encodingRule("xsd"));
	boolean unionAsGroup = typeCi != null && typeCi.matches("rule-xsd-cls-union-asGroup");
	if (mea != null || meag != null || unionAsGroup) {
	    MessageContext mc = result.addDebug(this, 2003, propi.name(), propi.inClass().name());
	    if (mc != null) {
		mc.addDetail(this, 0, propi.fullName());
	    }
	    return;
	}

	// check if cibase is encoded as an attribute group or as a string
	if (cibase.category() == Options.UNION && ((cibase.matches("rule-xsd-cls-union-asCharacterString")
		&& "true".equalsIgnoreCase(cibase.taggedValue("gmlAsCharacterString")))
		|| (cibase.matches("rule-xsd-cls-union-asGroup")
			&& "true".equalsIgnoreCase(cibase.taggedValue("gmlAsGroup"))))) {
	    MessageContext mc = result.addDebug(this, 2004, propi.name(), propi.inClass().name());
	    if (mc != null) {
		mc.addDetail(this, 0, propi.fullName());
	    }
	    return;
	}

	// check if cibase is a mixin encoded as an attribute group
	if (cibase.category() == Options.MIXIN && cibase.matches("rule-xsd-cls-mixin-classes-as-group")) {
	    MessageContext mc = result.addDebug(this, 2005, propi.name(), propi.inClass().name());
	    if (mc != null) {
		mc.addDetail(this, 0, propi.fullName());
	    }
	    return;
	}

	boolean valueIsSimpleType = isSimpleType(typeCi, propi.typeInfo().name, propi.encodingRule("xsd"));

	schDoc.setQueryBinding("xslt2");

	/*
	 * Ensure that if there are elements representing the property, then either
	 * there is only a single such element that is nil, has a nilReason, and no
	 * value - or all of these elements are not nil, do not have nilReason
	 * attributes, and have values.
	 * 
	 * Take into account different value type categories and encodings.
	 */

	String piElementXPath = propi.qname();

	String piValueXPath = null;

	if (propi.matches("rule-xsd-prop-metadata-gmlsf-byReference")
		&& "true".equalsIgnoreCase(propi.taggedValue("isMetadata"))) {

	    piValueXPath = piElementXPath + "/@xlink:href";
	    schDoc.registerNamespace("xlink");

	} else {

	    if (typeCi != null) {

		if (typeCi.category() == Options.CODELIST) {

		    if (typeCi.matches("rule-xsd-all-naming-19139")) {

			piValueXPath = piElementXPath + "/*";

		    } else if (propi.inClass().matches("rule-xsd-cls-codelist-asDictionaryGml33")
			    && typeCi.asDictionaryGml33()) {

			piValueXPath = piElementXPath + "/@xlink:href";
			schDoc.registerNamespace("xlink");
		    } else {

			piValueXPath = piElementXPath + "/text()";
		    }

		} else if (typeCi.category() == Options.ENUMERATION) {

		    if (typeCi.matches("rule-xsd-all-naming-19139")) {

			piValueXPath = piElementXPath + "/*";

		    } else {

			piValueXPath = piElementXPath + "/text()";
		    }

		} else if (typeCi.category() == Options.UNION) {

		    if (typeCi.matches("rule-xsd-cls-union-omitUnionsRepresentingFeatureTypeSets")
			    && "true".equalsIgnoreCase(typeCi.taggedValue("representsFeatureTypeSet"))) {

			piValueXPath = piElementXPath + "/@xlink:href";
			schDoc.registerNamespace("xlink");

		    } else {

			// same for GML and ISO 19139 encoding
			piValueXPath = piElementXPath + "/*";
		    }

		} else if (typeCi.category() == Options.BASICTYPE) {

		    piValueXPath = piElementXPath + "/text()";

		} else if (typeCi.category() == Options.DATATYPE) {

		    piValueXPath = piElementXPath + "/*";

		} else if (typeCi.category() == Options.FEATURE) {

		    if (typeCi.matches("rule-xsd-prop-featureType-gmlsf-byReference")) {

			piValueXPath = piElementXPath + "/@xlink:href";
			schDoc.registerNamespace("xlink");

		    } else if (typeCi.matches("rule-xsd-all-naming-19139")) {

			piValueXPath = piElementXPath + "/(* | @xlink:href)";
			schDoc.registerNamespace("xlink");

		    } else {

			if ("inline".equalsIgnoreCase(propi.inlineOrByReference())) {
			    piValueXPath = piElementXPath + "/*";
			} else if ("byreference".equalsIgnoreCase(propi.inlineOrByReference())) {
			    piValueXPath = piElementXPath + "/@xlink:href";
			    schDoc.registerNamespace("xlink");
			} else {
			    piValueXPath = piElementXPath + "/(* | @xlink:href)";
			    schDoc.registerNamespace("xlink");
			}
		    }

		} else {

		    if (typeCi.matches("rule-xsd-all-naming-19139")) {

			if (!classCanBeReferenced(typeCi)) {
			    piValueXPath = piElementXPath + "/*";
			} else {
			    piValueXPath = piElementXPath + "/(* | @xlink:href)";
			    schDoc.registerNamespace("xlink");
			}

		    } else {

			if (valueIsSimpleType) {
			    piValueXPath = piElementXPath + "/text()";
			} else if ("inline".equalsIgnoreCase(propi.inlineOrByReference())) {
			    piValueXPath = piElementXPath + "/*";
			} else if ("byreference".equalsIgnoreCase(propi.inlineOrByReference())) {
			    piValueXPath = piElementXPath + "/@xlink:href";
			    schDoc.registerNamespace("xlink");
			} else {
			    piValueXPath = piElementXPath + "/(* | @xlink:href)";
			    schDoc.registerNamespace("xlink");
			}
		    }
		}

	    } else {

		if (valueIsSimpleType) {
		    piValueXPath = piElementXPath + "/text()";
		} else if ("inline".equalsIgnoreCase(propi.inlineOrByReference())
			|| !options.xmlReferenceable(propi.typeInfo().name, propi.encodingRule("xsd"))) {
		    piValueXPath = piElementXPath + "/*";
		} else if ("byreference".equalsIgnoreCase(propi.inlineOrByReference())) {
		    piValueXPath = piElementXPath + "/@xlink:href";
		    schDoc.registerNamespace("xlink");
		} else {
		    piValueXPath = piElementXPath + "/(* | @xlink:href)";
		    schDoc.registerNamespace("xlink");
		}
	    }
	}

	schDoc.registerNamespace("xsi");

	String xpath = "not(" + piElementXPath + ") or " + "(count(" + piElementXPath
		+ "[@xsi:nil='true' and @*:nilReason]) eq 1 and not(" + piValueXPath + ")) or (count(" + piElementXPath
		+ "[@xsi:nil='true' or @*:nilReason]) eq 0 and count(" + piValueXPath + ") eq count(" + piElementXPath
		+ "))";

	XpathFragment xpathValueOrNilReason = new XpathFragment(0, xpath, XpathType.BOOLEAN);

	String assertionText = "If there are elements for property " + propi.name()
		+ ", then either there is only a single such element that is nil, has a nilReason, and no value - or all of these elements are not nil, do not have nilReason attributes, and have values";

	schDoc.addAssertion(cibase, true, xpathValueOrNilReason, assertionText);
    }

    public boolean isSimpleType(ClassInfo ci, String ciName, String propiEncodingRule) {

	boolean isSimple = false;

	if (ci != null) {
	    Boolean indicatorSimpleType = XmlSchema.indicatorForObjectElementWithSimpleContent(ci);
	    isSimple = !XmlSchema.classHasObjectElement(ci) || (indicatorSimpleType != null && indicatorSimpleType);
	} else {
	    String tname = ciName;
	    MapEntry me = options.typeMapEntry(tname, propiEncodingRule);
	    if (me != null)
		isSimple = me.p2.equalsIgnoreCase("simple/simple") || me.p2.equalsIgnoreCase("complex/simple");
	}

	return isSimple;

    }

    public void write() {

	if (printed) {
	    return;
	}
	if (diagnosticsOnly) {
	    return;
	}

	boolean skipXmlSchemaOutput = options.parameterAsBoolean(this.getClass().getName(),
		XmlSchemaConstants.PARAM_SKIP_XML_SCHEMA_OUTPUT, false);

	if (skipXmlSchemaOutput) {

	    result.addInfo(this, 1000);

	} else {

	    Properties outputFormat = OutputPropertiesFactory.getDefaultMethodProperties("xml");
	    outputFormat.setProperty("indent", "yes");
	    outputFormat.setProperty("{http://xml.apache.org/xalan}indent-amount", "2");
	    outputFormat.setProperty("encoding", "UTF-8");

	    for (XsdDocument xsd : xsdMap.values()) {

		if (!xsd.printed()) {
		    try {
			xsd.printFile(outputFormat);
			result.addResult(getTargetName(), outputDirectory, xsd.name, pi.targetNamespace());
		    } catch (Exception e) {
			String m = e.getMessage();
			if (m != null) {
			    result.addError(m);
			}
			e.printStackTrace(System.err);
		    }
		}
	    }
	}

	/*
	 * Now on to writing Schematron. Gather all unique SchematronSchema instances.
	 * Note that multiple XsdDocuments may share a common SchematronSchema, if
	 * Schematron segmentation is not enabled. That is why we first create a set of
	 * the SchematronSchemas referenced by the XsdDocuments.
	 */
	Set<SchematronSchema> schDocs = new HashSet<>();

	for (XsdDocument xsd : xsdMap.values()) {
	    if (xsd.getSchematronDocument() != null) {
		schDocs.add(xsd.getSchematronDocument());
	    }
	}

	for (SchematronSchema schDoc : schDocs) {

	    /*
	     * Only if the SchematronSchema contains rules will we write it to disc.
	     */
	    if (schDoc.hasRules()) {
		schDoc.write(outputDirectory);
	    } else {
		result.addDebug(this, 2011, schDoc.getFileName());
	    }
	}

	printed = true;
    }

    /**
     * Create XML Schema documents
     * 
     * @param pi      tbd
     * @param xsdcurr tbd
     * @return tbd
     * @throws ShapeChangeAbortException tbd
     */
    protected boolean createXSDs(PackageInfo pi, XsdDocument xsdcurr) throws ShapeChangeAbortException {
	boolean res = false;

	/**
	 * Determine and if necessary create XML Schema document for this package
	 */
	XsdDocument xsd;
	String xsdDocument = pi.xsdDocument();

	if (xsdDocument != null && xsdDocument.length() > 0) {
	    try {
		result.addDebug(this, 10017, xsdDocument, pi.name());

		xsd = new XsdDocument(pi, model, options, result, config, xsdDocument,
			determineSchematronSchemaForXsdDocument(xsdDocument));
		res = true;
	    } catch (ParserConfigurationException e) {
		result.addFatalError(null, 2);
		throw new ShapeChangeAbortException();
	    }
	} else {
	    xsd = xsdcurr;
	    if (xsd == null) {
		xsdDocument = pi.name() + ".xsd";
		result.addWarning(this, 15, pi.name(), xsdDocument);
		try {
		    result.addDebug(this, 10017, xsdDocument, pi.name());
		    xsd = new XsdDocument(pi, model, options, result, config, xsdDocument,
			    determineSchematronSchemaForXsdDocument(xsdDocument));
		    res = true;
		} catch (ParserConfigurationException e) {
		    result.addFatalError(null, 2);
		    throw new ShapeChangeAbortException();
		}
	    }
	}
	xsdMap.put(pi.id(), xsd);

	/**
	 * Navigate through sub packages and create and include XML Schema documents
	 */
	if (pi.matches("rule-xsd-pkg-contained-packages")) {
	    try {
		for (PackageInfo pix : pi.containedPackages()) {
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

    /**
     * Determines - and thereby maybe creates - the SchematronSchema that applies to
     * the XsdDocument whose filename is given via parameter. If Schematron creation
     * is enabled (via rule-xsd-pkg-schematron) but segmentation of Schematron
     * corresponding to segmentation of xsdDocument is not enabled, the result will
     * be the common SchematronSchema (created during initialization of the target).
     * If Schematron creation as well as segmentation is enabled, a new
     * SchematronSchema object will be created and returned, using the given
     * filename as basis for the Schematron file. If Schematron creation is not
     * enabled, this method will return null.
     * 
     * @param xsdDocumentFilename the file name of the XsdDocument for which the
     *                            SchematronSchema object shall be determined; must
     *                            not be <code>null</code>; can include the file
     *                            extension (.xsd)
     * @return the SchematronSchema to use for the XsdDocument with given filename
     */
    private SchematronSchema determineSchematronSchemaForXsdDocument(String xsdDocumentFilename) {

	// NOTE: Will be null if Schematron creation is not enabled.
	SchematronSchema schDoc = commonSchDoc;

	if (pi.matches("rule-xsd-pkg-schematron") && schematronSegmentation) {

	    String schemaXsdBaseName = FilenameUtils.getBaseName(xsdDocumentFilename);

	    if ("xslt2".equalsIgnoreCase(explicitSchematronQueryBinding)) {
		schDoc = new SchematronSchemaXslt2(model, options, result, pi, schemaXsdBaseName,
			schematronSegmentation);
	    } else {
		schDoc = new SchematronSchemaOld(model, options, result, pi, schemaXsdBaseName, schematronSegmentation);
	    }
	}

	return schDoc;
    }

    /**
     * Process dependency relationships with other packages
     * 
     * @param pi tbd
     * @throws ShapeChangeAbortException tbd
     */
    protected void processDependecies(PackageInfo pi) throws ShapeChangeAbortException {
	XsdDocument xsd1 = xsdMap.get(pi.id());
	for (String pid : pi.supplierIds()) {
	    XsdDocument xsd2 = xsdMap.get(pid);
	    if (xsd2 != null) {
		xsd1.addInclude(xsd2);
	    } else {
		PackageInfo pi2 = model.packageById(pid);
		if (pi2 != null && pi2.xmlns() != null && pi2.xmlns().length() > 0 && pi2.targetNamespace() != null
			&& pi2.targetNamespace().length() > 0)
		    xsd1.addImport(pi2.xmlns(), pi2.targetNamespace());
	    }
	}

	/**
	 * Navigate through sub packages and import/include XML Schema documents
	 */
	try {
	    for (PackageInfo pix : pi.containedPackages()) {
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

    @Override
    public String getTargetIdentifier() {
	return "xsd";
    }

    /**
     * Tries to identify if the object element that represents ci has simple
     * content. If ci is a code list encoded following ISO 19139:2007, the result
     * will be <code>true</code>. Otherwise, if an XsdMapEntry exists with attribute
     * xmlElementHasSimpleType=true the result will be <code>true</code>. If
     * xmlElementHasSimpleType=false, the result will be <code>false</code>. If no
     * map entry exists, or the map entry does not contain the attribute, the result
     * will be undetermined (i.e., <code>null</code>).
     * 
     * @param ci tbd
     * @return A Boolean object that indicates if the object element that represents
     *         ci has simple content. If the result is <code>null</code>, that
     *         information is undetermined.
     */
    public static Boolean indicatorForObjectElementWithSimpleContent(ClassInfo ci) {

	if (ci.category() == Options.CODELIST && ci.matches("rule-xsd-cls-standard-19139-property-types")) {

	    return true;

	} else {

	    return ci.options().xmlElementHasSimpleContent(ci.name(), ci.encodingRule("xsd"));
	}
    }

    /**
     * Find out whether the given class is represented by means of an XML element
     * construct.
     * 
     * @param ci ClassInfo of class to be inquired
     * @return Flag returning the requested information
     */
    static public boolean classHasObjectElement(ClassInfo ci) {

	// test, if we have a map entry to an element for this encoding rule
	MapEntry me = ci.options().elementMapEntry(ci.name(), ci.encodingRule("xsd"));
	if (me != null) {
	    return me.p1 != null && me.p1.length() > 0;
	}

	/*
	 * We don't. Before checking the stereotype we first have to look for a map
	 * entry that maps the class to a simple type or attribute
	 */
	me = ci.options().typeMapEntry(ci.name(), ci.encodingRule("xsd"));
	if (me != null) {
	    // Yes. As we had no element map entry the type content must be
	    // simple
	    if (me.rule == null || !me.rule.equalsIgnoreCase("direct")) {
		// as a static function we cannot write an error message
	    } else if (me.p2 == null
		    || !(me.p2.equalsIgnoreCase("complex/simple") || me.p2.equalsIgnoreCase("simple/simple"))) {
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

	me = ci.options().attributeGroupMapEntry(ci.name(), ci.encodingRule("xsd"));
	if (me != null) {
	    // We know this is just an attribute group in XML
	    return false;
	}

	int cat = ci.category();
	if (ci.matches("rule-xsd-all-naming-19139")) {
	    return cat == Options.DATATYPE || cat == Options.UNION || cat == Options.FEATURE || cat == Options.OBJECT
		    || cat == Options.ENUMERATION || cat == Options.CODELIST || cat == Options.UNKNOWN;
	} else {
	    return cat == Options.DATATYPE && !ci.matches("rule-all-cls-aixmDatatype")
		    || cat == Options.UNION && !(ci.matches("rule-xsd-cls-union-asGroup") && ci.asGroup())
		    || cat == Options.FEATURE || cat == Options.OBJECT || cat == Options.UNKNOWN
		    || (cat == Options.OKSTRAFID && ci.matches("rule-xsd-cls-okstra-fid"));
	}
    }

    public static boolean implementedAsXmlAttribute(PropertyInfo pi) {

	// TBD: requires specific mapping info if the inClass of pi is given by
	// a MapEntry

	boolean result = false;

	if (pi.name().equals("uom") && pi.matches("rule-all-prop-uomAsAttribute")) {

	    result = true;

	} else if (pi.matches("rule-xsd-prop-xsdAsAttribute")) {

	    result = true;
	}

	return result;
    }

    /**
     * Find out whether the given class can carry an id and can hence be referenced
     * by means of xlink:href.
     * 
     * @param ci ClassInfo of class to be inquired
     * @return Flag returning the requested information
     */
    static public boolean classCanBeReferenced(ClassInfo ci) {

	/*
	 * For the GML encoding, checking if the type has an object element and is a
	 * feature, okstrafid, or object is fine. In these cases we expect a gml:id on
	 * the element. For the default encoding of ISO 19139 encoding, this is also ok
	 * - also including union and datatype. For ISO 19139 referenceable types we
	 * expect an 'id' attribute, provided by extending gco:AbstractObject.
	 * 
	 * However, we need to take into account the special case encodings of ISO
	 * 19139. They are often (or always?) not referenceable, as is the case for
	 * gco:CharacterString.
	 * 
	 * The XML attribute @xmReferenceable on an XsdMapEntry is used to tell
	 * ShapeChange that the mapping target definitely cannot be referenced. The
	 * attribute has a boolean value. If the attribute is not present, the usual
	 * assumptions (outlined before) apply.
	 */

	Boolean xmlElementCanBeReferenced = ci.options().xmlReferenceable(ci.name(), ci.encodingRule("xsd"));

	if (classHasObjectElement(ci) && (xmlElementCanBeReferenced == null || xmlElementCanBeReferenced)) {

	    int cat = ci.category();

	    if (cat == Options.FEATURE)
		return true;
	    if (cat == Options.OKSTRAFID)
		return true;
	    if (cat == Options.OBJECT)
		return true;

	    if (ci.matches("rule-xsd-all-naming-19139")) {
		/*
		 * 2018-01-18 JE: If the encoding rule of ISO 19139:2007 was extended, the
		 * following check would not work. That's why I replaced it with
		 * ci.matches("rule-xsd-all-naming-19139").
		 */
		// if (ci.encodingRule("xsd").equals(Options.ISO19139_2007)) {
		if (cat == Options.DATATYPE)
		    return true;
		if (cat == Options.UNION)
		    return true;
	    }
	}
	return false;
    }

    @Override
    public void registerRulesAndRequirements(RuleRegistry r) {

	/*
	 * mandatory rules
	 */
	r.addRule("req-xsd-pkg-xsdDocument-unique");
	r.addRule("req-xsd-cls-name-unique");
	r.addRule("req-xsd-cls-ncname");
	r.addRule("req-xsd-prop-data-type");
	r.addRule("req-xsd-prop-value-type-exists");
	r.addRule("req-xsd-prop-ncname");
	r.addRule("rule-xsd-pkg-contained-packages");
	r.addRule("rule-xsd-pkg-dependencies");
	r.addRule("rule-xsd-cls-union-as-choice");
	r.addRule("rule-xsd-cls-unknown-as-object");
	r.addRule("rule-xsd-cls-sequence");
	r.addRule("rule-xsd-cls-object-element");
	r.addRule("rule-xsd-cls-type");
	r.addRule("rule-xsd-cls-property-type");
	r.addRule("rule-xsd-cls-local-properties");
	/*
	 * Associate these with a core encoding rule
	 */
	r.addRule("req-xsd-pkg-xsdDocument-unique", "*");
	r.addRule("req-xsd-cls-name-unique", "*");
	r.addRule("req-xsd-cls-ncname", "*");
	r.addRule("req-xsd-prop-data-type", "*");
	r.addRule("req-xsd-prop-value-type-exists", "*");
	r.addRule("req-xsd-prop-ncname", "*");
	r.addRule("rule-xsd-pkg-contained-packages", "*");
	r.addRule("rule-xsd-pkg-dependencies", "*");
	r.addRule("rule-xsd-cls-unknown-as-object", "*");
	r.addRule("rule-xsd-cls-object-element", "*");
	r.addRule("rule-xsd-cls-type", "*");
	r.addRule("rule-xsd-cls-property-type", "*");
	r.addRule("rule-xsd-cls-local-properties", "*");
	r.addRule("rule-xsd-cls-union-as-choice", "*");
	r.addRule("rule-xsd-cls-sequence", "*");
	/*
	 * GML 3.2 / ISO 19136:2007 rules
	 */
	r.addRule("req-xsd-cls-generalization-consistent");
	r.addRule("rule-xsd-all-naming-gml");
	r.addRule("rule-xsd-cls-global-enumeration");
	r.addRule("rule-xsd-cls-codelist-asDictionary");
	r.addRule("rule-xsd-cls-noPropertyType");
	r.addRule("rule-xsd-cls-byValuePropertyType");
	r.addRule("rule-xsd-cls-standard-gml-property-types");
	r.addRule("rule-xsd-pkg-gmlProfileSchema");
	r.addRule("rule-xsd-prop-defaultCodeSpace");
	r.addRule("rule-xsd-prop-inlineOrByReference");
	r.addRule("rule-xsd-prop-reverseProperty");
	r.addRule("rule-xsd-prop-targetElement");
	/*
	 * add the iso19136_2007 encoding rule and extend the core encoding rule
	 */
	r.addExtendsEncRule("iso19136_2007", "*");
	r.addRule("req-xsd-cls-generalization-consistent", "iso19136_2007");
	r.addRule("rule-xsd-all-naming-gml", "iso19136_2007");
	r.addRule("rule-xsd-cls-global-enumeration", "iso19136_2007");
	r.addRule("rule-xsd-cls-codelist-asDictionary", "iso19136_2007");
	r.addRule("rule-xsd-cls-standard-gml-property-types", "iso19136_2007");
	r.addRule("rule-xsd-cls-noPropertyType", "iso19136_2007");
	r.addRule("rule-xsd-cls-byValuePropertyType", "iso19136_2007");
	r.addRule("rule-xsd-pkg-gmlProfileSchema", "iso19136_2007");
	r.addRule("rule-xsd-prop-targetElement", "iso19136_2007");
	r.addRule("rule-xsd-prop-reverseProperty", "iso19136_2007");
	r.addRule("rule-xsd-prop-defaultCodeSpace", "iso19136_2007");
	r.addRule("rule-xsd-prop-inlineOrByReference", "iso19136_2007");
	/*
	 * additional GML 3.3 rules
	 */
	r.addRule("rule-xsd-cls-codelist-asDictionaryGml33");
	r.addRule("rule-xsd-rel-association-classes");
	/*
	 * add the gml33 encoding rule and extend the core encoding rule
	 */
	r.addExtendsEncRule("gml33", "*");
	r.addRule("req-xsd-cls-generalization-consistent", "gml33");
	r.addRule("rule-xsd-all-naming-gml", "gml33");
	r.addRule("rule-xsd-cls-global-enumeration", "gml33");
	r.addRule("rule-xsd-cls-codelist-asDictionaryGml33", "gml33");
	r.addRule("rule-xsd-cls-standard-gml-property-types", "gml33");
	r.addRule("rule-xsd-cls-noPropertyType", "gml33");
	r.addRule("rule-xsd-cls-byValuePropertyType", "gml33");
	r.addRule("rule-xsd-pkg-gmlProfileSchema", "gml33");
	r.addRule("rule-xsd-prop-targetElement", "gml33");
	r.addRule("rule-xsd-prop-reverseProperty", "gml33");
	r.addRule("rule-xsd-prop-defaultCodeSpace", "gml33");
	r.addRule("rule-xsd-prop-inlineOrByReference", "gml33");
	r.addRule("rule-xsd-rel-association-classes", "gml33");
	/*
	 * ISO/TS 19139:2007 rules
	 */
	r.addRule("rule-xsd-all-naming-19139");
	r.addRule("rule-xsd-cls-standard-19139-isoType");
	r.addRule("rule-xsd-cls-standard-19139-property-types");
	r.addRule("rule-xsd-cls-enum-object-element");
	r.addRule("rule-xsd-cls-enum-property-type");

	/*
	 * add the iso19139_2007 encoding rule and extend the core encoding rule
	 */
	r.addExtendsEncRule("iso19139_2007", "*");
	r.addRule("rule-xsd-cls-enum-object-element", "iso19139_2007");
	r.addRule("rule-xsd-cls-enum-property-type", "iso19139_2007");
	r.addRule("rule-xsd-cls-global-enumeration", "iso19139_2007");
	r.addRule("rule-xsd-cls-standard-19139-property-types", "iso19139_2007");
	r.addRule("rule-xsd-all-naming-19139", "iso19139_2007");
	/*
	 * SWE Common Data Model 2.0 rules
	 */
	r.addRule("rule-xsd-all-naming-swe");
	r.addRule("rule-xsd-prop-xsdAsAttribute");
	r.addRule("rule-xsd-prop-soft-typed");
	r.addRule("rule-xsd-cls-union-as-group-property-type");
	r.addRule("rule-xsd-cls-standard-swe-property-types");
	r.addRule("rule-xsd-prop-initialValue");
	/*
	 * add the ogcSweCommon2 encoding rule and extend the core encoding rule
	 */
	r.addExtendsEncRule("ogcSweCommon2", "*");
	r.addRule("req-xsd-cls-generalization-consistent", "ogcSweCommon2");
	r.addRule("rule-xsd-all-naming-swe", "ogcSweCommon2");
	r.addRule("rule-xsd-cls-global-enumeration", "ogcSweCommon2");
	r.addRule("rule-xsd-cls-codelist-asDictionary", "ogcSweCommon2");
	r.addRule("rule-xsd-cls-standard-swe-property-types", "ogcSweCommon2");
	r.addRule("rule-xsd-cls-noPropertyType", "ogcSweCommon2");
	r.addRule("rule-xsd-cls-byValuePropertyType", "ogcSweCommon2");
	r.addRule("rule-xsd-pkg-gmlProfileSchema", "ogcSweCommon2");
	r.addRule("rule-xsd-prop-targetElement", "ogcSweCommon2");
	r.addRule("rule-xsd-prop-reverseProperty", "ogcSweCommon2");
	r.addRule("rule-xsd-prop-defaultCodeSpace", "ogcSweCommon2");
	r.addRule("rule-xsd-prop-inlineOrByReference", "ogcSweCommon2");
	r.addRule("rule-xsd-prop-xsdAsAttribute", "ogcSweCommon2");
	r.addRule("rule-xsd-prop-soft-typed", "ogcSweCommon2");
	r.addRule("rule-xsd-cls-union-as-group-property-type", "ogcSweCommon2");
	r.addRule("rule-xsd-prop-initialValue", "ogcSweCommon2");

	/*
	 * additional GML 2.1 rules
	 */
	r.addRule("rule-xsd-all-gml21");
	r.addRule("rule-xsd-cls-codelist-anonymous-xlink");
	/*
	 * add the gml21 encoding rule and extend the core encoding rule
	 */
	r.addExtendsEncRule("gml21", "iso19136_2007");
	r.addRule("rule-xsd-all-gml21", "gml21");
	r.addRule("rule-xsd-cls-codelist-anonymous-xlink", "gml21");

	/*
	 * non-standard extensions - requirements
	 */
	r.addRule("req-all-all-documentation");
	r.addRule("req-all-prop-sequenceNumber");
	r.addRule("req-xsd-pkg-targetNamespace");
	r.addRule("req-xsd-pkg-xmlns");
	r.addRule("req-xsd-pkg-namespace-schema-only");
	r.addRule("rec-xsd-pkg-version");
	r.addRule("req-xsd-pkg-xsdDocument");
	r.addRule("req-xsd-pkg-dependencies");
	r.addRule("req-xsd-cls-codelist-asDictionary-true");
	r.addRule("req-xsd-cls-codelist-extensibility-values");
	r.addRule("req-xsd-cls-codelist-extensibility-vocabulary");
	r.addRule("req-xsd-cls-codelist-no-supertypes");
	r.addRule("req-xsd-cls-datatype-noPropertyType");
	r.addRule("req-xsd-cls-enum-no-supertypes");
	r.addRule("req-xsd-cls-mixin-supertypes");
	r.addRule("req-xsd-cls-mixin-supertypes-overrule");
	r.addRule("req-xsd-cls-objecttype-byValuePropertyType");
	r.addRule("req-xsd-cls-objecttype-noPropertyType");
	r.addRule("req-xsd-cls-suppress-no-properties");
	r.addRule("req-xsd-cls-suppress-subtype");
	r.addRule("req-xsd-cls-suppress-supertype");
	r.addRule("req-xsd-prop-codelist-obligation");
	/*
	 * non-standard extensions - conversion rules
	 */
	r.addRule("rule-xsd-all-descriptorAnnotation");
	r.addRule("rule-xsd-all-globalIdentifierAnnotation");
	r.addRule("rule-xsd-all-notEncoded");
	r.addRule("rule-xsd-all-propertyAssertion-ignoreProhibited");
	r.addRule("rule-xsd-cls-adeelement");
	r.addRule("rule-xsd-cls-basictype");
	r.addRule("rule-xsd-cls-basictype-list");	
	r.addRule("rule-xsd-cls-codelist-constraints");
	r.addRule("rule-xsd-cls-codelist-constraints2");
	r.addRule("rule-xsd-cls-codelist-constraints-codeAbsenceInModelAllowed");
	r.addRule("rule-xsd-cls-codelist-gmlsf");
	r.addRule("rule-xsd-cls-enum-subtypes");
	r.addRule("rule-xsd-cls-enum-supertypes");
	r.addRule("rule-xsd-cls-mixin-classes-as-group");
	r.addRule("rule-xsd-cls-mixin-classes");
	r.addRule("rule-xsd-cls-mixin-classes-non-mixin-supertypes");
	r.addRule("rule-xsd-cls-no-abstract-classes");
	r.addRule("rule-xsd-cls-no-base-class");
	r.addRule("rule-xsd-cls-no-gml-types");
	r.addRule("rule-xsd-cls-okstra-fid");
	r.addRule("rule-xsd-cls-okstra-lifecycle");
	r.addRule("rule-xsd-cls-okstra-schluesseltabelle");
	r.addRule("rule-xsd-cls-suppress");
	r.addRule("rule-xsd-cls-union-asCharacterString");
	r.addRule("rule-xsd-cls-union-asGroup");
	r.addRule("rule-xsd-cls-union-direct");
	r.addRule("rule-xsd-cls-union-direct-optionality");
	r.addRule("rule-xsd-cls-union-omitUnionsRepresentingFeatureTypeSets");
	r.addRule("rule-xsd-prop-att-map-entry");
	r.addRule("rule-xsd-prop-constrainingFacets");
	r.addRule("rule-xsd-prop-exclude-derived");
	r.addRule("rule-xsd-prop-length-size-pattern");
	r.addRule("rule-xsd-prop-featureType-gmlsf-byReference");
	r.addRule("rule-xsd-prop-metadata");
	r.addRule("rule-xsd-prop-metadata-gmlsf-byReference");
	r.addRule("rule-xsd-prop-nillable");
	r.addRule("rule-xsd-prop-nilReasonAllowed");
	r.addRule("rule-xsd-prop-nilReason-constraints");
	r.addRule("rule-xsd-prop-gmlArrayProperty");
	r.addRule("rule-xsd-prop-gmlListProperty");
	r.addRule("rule-xsd-prop-qualified-associations");
	r.addRule("rule-xsd-prop-targetCodeListURI");
	r.addRule("rule-xsd-prop-valueOrNilReason-constraints");
	r.addRule("rule-xsd-all-no-documentation");
	r.addRule("rule-xsd-cls-local-enumeration");
	r.addRule("rule-xsd-cls-local-basictype");
	r.addRule("rule-xsd-pkg-dgiwgsp");
	r.addRule("rule-xsd-pkg-gmlsf");
	r.addRule("rule-xsd-pkg-schematron");
	r.addRule("rule-xsd-all-tagged-values");
	r.addRule("rule-xsd-cls-adehook");

	// AIXM specific rules
	r.addRule("rule-all-cls-aixmDatatype");
	// further rules of a more general nature
	r.addRule("rule-all-prop-uomAsAttribute");
    }

    @Override
    public String getDefaultEncodingRule() {
	return "iso19136_2007";
    }

    @Override
    public String message(int mnr) {

	switch (mnr) {
	case 0:
	    return "Context: property '$1$'.";
	case 1:
	    return "Context: class '$1$'.";
	case 2:
	    return "Context: association class '$1$'.";
	case 3:
	    return "Context: association between class '$1$' (with property '$2$') and class '$3$' (with property '$4$')";
	case 10:
	    return "Class '$1$' in package '$2$' is not associated with an XSD document.";
	case 15:
	    return "Package '$1$' not associated with any XML Schema document. Set tagged value 'xsdDocument' on the according schema package. Alternatively, if a PackageInfo element is used in the input configuration of ShapeChange to mark that package as an application schema, set the XML attribute 'xsdDocument'. Package '$1$' will be associated with XML Schema document '$2$'.";

	case 1000:
	    return "Skipping XML Schema output, as configured.";
	case 1001:
	    return "No namespace was found for namespace abbreviation '$1$', configured via tagged value 'xsdForcedImports' on schema package '$2$'. No import will be created for this namespace abbreviation.";

	/*
	 * 2000 - 2999: Schematron assertions (for value or nilReason etc.)
	 */
	case 2002:
	    return "??Property '$1$' of class '$2$' is encoded as an attribute. A schematron assertion to check for value or nilReason will not be created.";
	case 2003:
	    return "??The value type of property '$1$' of class '$2$' is encoded as an attribute (group). A schematron assertion to check for value or nilReason will not be created.";
	case 2004:
	    return "??Union '$1$' that has property '$2$' is encoded as an attribute group or as a CharacterString. A schematron assertion to check for value or nilReason will not be created for the property.";
	case 2005:
	    return "??Mixin '$1$' that has property '$2$' is encoded as an attribute group. A schematron assertion to check for value or nilReason will not be created for the property.";
	case 2006:
	    return "??Property '$1$' of class '$2$' is encoded as an attribute. A schematron assertion to check nilReason values will not be created.";
	case 2007:
	    return "??Union '$1$' that has property '$2$' is encoded as an attribute group or as a CharacterString. A schematron assertion to check nilReason values will not be created for the property.";
	case 2008:
	    return "??Mixin '$1$' that has property '$2$' is encoded as an attribute group. A schematron assertion to check nilReason values will not be created for the property.";
	case 2009:
	    return "voidReasonType defined for property '$1$' (directly via tagged value or indirectly via parameter) is '$2$'. This type is not an enumeration or does not define any enum. An assertion to check @nilReason for this property will NOT be created.";
	case 2010:
	    return "No voidReasonType defined or found for property '$1$' (directly via tagged value or indirectly via parameter). An assertion to check @nilReason for this property will NOT be created.";
	case 2011:
	    return "Schematron schema '$1$' will not be written, because it does not contain any schematron rule (with assertion(s)).";
	case 2012:
	    return "defaultVoidReasonType defined by the according target parameter is: '$1$'. The type could not be found in the model, using the rules defined for the parameter. Accordingly, a default void reason type is not set.";
	case 2013:
	    return "voidReasonType defined for property '$1$' of class '$2$' (via tagged value 'voidReasonType') is: '$3$'. The type could not be found in the model, using the rules defined for finding the void reason type (defined by the tagged value). The tagged value will be ignored.";
	
	case 10012:
	    return "Generating XML Schema for application schema '$1$'.";
	case 10016:
	    return "Processing class '$1$', rule '$2$'.";
	case 10017:
	    return "Creating XSD document '$1$' for package '$2$'.";
	
	default:
	    return "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	}
    }
}
