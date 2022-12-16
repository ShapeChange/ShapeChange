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
 * (c) 2002-2022 interactive instruments GmbH, Bonn, Germany
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

package de.interactive_instruments.ShapeChange.Target.gfs;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import de.interactive_instruments.ShapeChange.MapEntryParamInfos;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.RuleRegistry;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.Target;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class GfsTemplateTarget implements Target, MessageSource {

    protected Model model = null;
    private boolean initialised = false;
    protected boolean diagnosticsOnly = false;

    protected boolean alwaysEncodeDataTypeName = false;
    protected String choiceForInlineOrByReference = "byReference";
    protected String gmlCodeListEncodingVersion = "3.2";
    protected String propertyNameSeparator = "_";
    protected String srsName = null;
    protected String xmlAttributeNameSeparator = "_"; // default: value of propertyNameSeparator parameter
    protected SortedSet<String> xmlAttributesToEncode = new TreeSet<>();

    protected boolean isUnitTest = false;

    protected String outputDirectory = null;

    /**
     * Contains information parsed from the 'param' attributes of each map entry
     * defined for this target.
     */
    protected MapEntryParamInfos mapEntryParamInfos = null;

    protected ShapeChangeResult result = null;
    protected Options options = null;

    private PackageInfo schema = null;
    private boolean schemaNotEncoded = false;

    private List<GmlFeatureClass> gmlFeatureClasses = new ArrayList<>();

    @Override
    public void initialise(PackageInfo pi, Model m, Options o, ShapeChangeResult r, boolean diagOnly)
	    throws ShapeChangeAbortException {

	schema = pi;
	model = m;
	options = o;
	result = r;
	diagnosticsOnly = diagOnly;

	if (!isEncoded(schema)) {

	    schemaNotEncoded = true;
	    result.addInfo(this, 7, schema.name());
	    return;
	}

	if (!initialised) {

	    initialised = true;

	    outputDirectory = options.parameter(this.getClass().getName(), "outputDirectory");
	    if (outputDirectory == null)
		outputDirectory = options.parameter("outputDirectory");
	    if (outputDirectory == null)
		outputDirectory = options.parameter(".");

	    isUnitTest = options.parameterAsBoolean(this.getClass().getName(), "_unitTestOverride", false);

	    alwaysEncodeDataTypeName = options.parameterAsBoolean(this.getClass().getName(),
		    GfsTemplateConstants.PARAM_ALWAYS_ENCODE_DATA_TYPE_NAME, false);

	    choiceForInlineOrByReference = options.parameterAsString(this.getClass().getName(),
		    GfsTemplateConstants.PARAM_CHOICE_FOR_INLINE_OR_BY_REFERENCE, "byReference", false, true);

	    gmlCodeListEncodingVersion = options.parameterAsString(this.getClass().getName(),
		    GfsTemplateConstants.PARAM_GML_CODE_LIST_ENCODING_VERSION, "3.2", false, true);

	    propertyNameSeparator = options.parameterAsString(this.getClass().getName(),
		    GfsTemplateConstants.PARAM_PROPERTY_NAME_SEPARATOR, "_", false, true);

	    srsName = options.parameterAsString(this.getClass().getName(), GfsTemplateConstants.PARAM_SRS_NAME, null,
		    false, true);

	    // MUST BE PARSED AFTER propertyNameSeparator, because that is used as default
	    xmlAttributeNameSeparator = options.parameterAsString(this.getClass().getName(),
		    GfsTemplateConstants.PARAM_XML_ATTRIBUTE_NAME_SEPARATOR, propertyNameSeparator, false, true);

	    List<String> xmlAttributesToEncodeTmp = options.parameterAsStringList(this.getClass().getName(),
		    GfsTemplateConstants.PARAM_XML_ATTRIBUTES_TO_ENCODE, new String[] { "href", "uom" }, true, true);
	    xmlAttributesToEncode.addAll(xmlAttributesToEncodeTmp);

	    // identify map entries defined in the target configuration
	    List<ProcessMapEntry> mapEntries = options.getCurrentProcessConfig().getMapEntries();

	    if (mapEntries.isEmpty()) {

		/*
		 * It is unlikely but not impossible that an application schema does not make
		 * use of types that require a type mapping.
		 */
		result.addWarning(this, 15);
		mapEntryParamInfos = new MapEntryParamInfos(result, null);

	    } else {

		/*
		 * Parse all parameter information
		 */
		mapEntryParamInfos = new MapEntryParamInfos(result, mapEntries);
	    }

	    File outputDirectoryFile = new File(outputDirectory);

	    // create output directory, if necessary
	    if (!diagnosticsOnly) {

		// Check whether we can use the output directory
		boolean exi = outputDirectoryFile.exists();
		if (!exi) {
		    outputDirectoryFile.mkdirs();
		    exi = outputDirectoryFile.exists();
		}
		boolean dir = outputDirectoryFile.isDirectory();
		boolean wrt = outputDirectoryFile.canWrite();
		boolean rea = outputDirectoryFile.canRead();
		if (!exi || !dir || !wrt || !rea) {
		    result.addFatalError(this, 5, outputDirectory);
		    throw new ShapeChangeAbortException();
		}

	    } else {
		result.addInfo(this, 10002);
	    }
	}

	/*
	 * Required to be performed for each application schema
	 */
	result.addDebug(this, 10001, pi.name());
    }

    public static boolean isEncoded(Info i) {

	if (i.matches(GfsTemplateConstants.RULE_ALL_NOT_ENCODED)
		&& i.encodingRule(GfsTemplateConstants.PLATFORM).equalsIgnoreCase("notencoded")) {

	    return false;

	} else {

	    return true;
	}
    }

    @Override
    public void process(ClassInfo ci) {

	if (ci == null || ci.pkg() == null) {
	    return;
	}

	if (schemaNotEncoded) {
	    result.addInfo(this, 18, schema.name(), ci.name());
	    return;
	}

	if (!isEncoded(ci)) {
	    result.addInfo(this, 8, ci.name());
	    return;
	}

	Optional<ProcessMapEntry> pme = mapEntry(ci);

	if (pme.isPresent()) {
	    result.addInfo(this, 14, ci.name(), pme.get().getTargetType());
	    return;
	}

	result.addDebug(this, 4, ci.name());

	if (ci.isAbstract() || ci.category() == Options.MIXIN) {
	    // ignore
	    return;
	}

	if (ci.category() == Options.DATATYPE || ci.category() == Options.UNION || ci.category() == Options.ENUMERATION
		|| ci.category() == Options.CODELIST) {

	    // ignore here - will be encoded as needed

	} else if (ci.category() == Options.OBJECT || ci.category() == Options.FEATURE) {

	    // Create the layer definition
	    GmlFeatureClass gfc = createFeatureClass(ci);
	    gmlFeatureClasses.add(gfc);

	} else {

	    // NOTE: conversion of unions and mixins not supported

	    result.addInfo(this, 17, ci.name());
	}
    }

    /**
     * Look up the map entry defined for a class. It is not guaranteed that such a
     * map entry exists.
     * 
     * @param ci the class for which to look up a map entry
     * @return an {@link Optional} with the map entry defined for the given class,
     *         under the ldp2 encoding rule that applies to the class
     */
    public Optional<ProcessMapEntry> mapEntry(ClassInfo ci) {

	return Optional.ofNullable(options.targetMapEntry(ci.name(), ci.encodingRule(GfsTemplateConstants.PLATFORM)));
    }

    @Override
    public void write() {

	if (diagnosticsOnly) {
	    return;
	}

	// Sort list of classes and the properties of each class
	Collections.sort(gmlFeatureClasses, new Comparator<GmlFeatureClass>() {
	    @Override
	    public int compare(GmlFeatureClass o1, GmlFeatureClass o2) {
		return o1.getName().compareTo(o2.getName());
	    }
	});
	for (GmlFeatureClass gfc : gmlFeatureClasses) {
	    Collections.sort(gfc.getGeometryPropertyDefinitions(), AbstractPropertyDefinition.PROP_NAME_COMPARATOR);
	    Collections.sort(gfc.getPropertyDefinitions(), AbstractPropertyDefinition.PROP_NAME_COMPARATOR);
	}

	File outputDirectoryFile = new File(outputDirectory);

	String schemaBaseName = schema.name().trim().replaceAll("\\W", "_");

	String gfsFileName = schemaBaseName + ".gfs";
	File gfsFile = new File(outputDirectoryFile, gfsFileName);
	String registryFileName = schemaBaseName + "_gml_registry.xml";
	File registryFile = new File(outputDirectoryFile, registryFileName);

	// write both the .gfs and the gml_registry.xml files
	GfsWriter writer = new GfsWriter();
	try {
	    writer.write(gfsFile, gmlFeatureClasses, srsName);
	    result.addResult(getTargetName(), outputDirectory, gfsFileName, null);
	    writer.writeGmlRegistry(registryFile, gfsFile, schema.xmlns(), schema.targetNamespace(), gmlFeatureClasses);
	    result.addResult(getTargetName(), outputDirectory, registryFileName, null);
	} catch (ShapeChangeException e) {
	    result.addError(this, 101, e.getMessage());
	    result.addDebug(this, 9, ExceptionUtils.getStackTrace(e));
	}
    }

    private GmlFeatureClass createFeatureClass(ClassInfo ci) {

	GmlFeatureClass gfc = new GmlFeatureClass();

	gfc.setName(ci.name());
	gfc.setElementPath(ci.name());

	if (srsName != null) {
	    gfc.setSrsName(srsName);
	}

	List<AbstractPropertyDefinition> allProperties = createPropertyDefinitions(ci);

	List<GeometryPropertyDefinition> geometryPropertyDefinitions = allProperties.stream()
		.filter(p -> p instanceof GeometryPropertyDefinition).map(p -> (GeometryPropertyDefinition) p)
		.collect(Collectors.toList());
	gfc.setGeometryPropertyDefinitions(geometryPropertyDefinitions);

	List<PropertyDefinition> propertyDefinitions = allProperties.stream()
		.filter(p -> p instanceof PropertyDefinition).map(p -> (PropertyDefinition) p)
		.collect(Collectors.toList());
	gfc.setPropertyDefinitions(propertyDefinitions);

	return gfc;
    }

    private List<AbstractPropertyDefinition> createPropertyDefinitions(ClassInfo ci) {

	// identify property paths
	List<List<PropertyInfo>> pathsToEndProperties = new ArrayList<>();
	for (PropertyInfo pi : ci.propertiesAll()) {
	    if (!isEncoded(pi)) {
		continue;
	    }
	    List<PropertyInfo> propList = new ArrayList<>();
	    propList.add(pi);
	    identifyPropertyPaths(pi, propList, pathsToEndProperties);
	}

	// create property definition(s) for each path
	List<AbstractPropertyDefinition> res = new ArrayList<>();

	for (List<PropertyInfo> path : pathsToEndProperties) {

	    PropertyInfo endPi = path.get(path.size() - 1);

	    AbstractPropertyDefinition baseDef;

	    Optional<String> geometryType = geometryType(endPi);

	    boolean maxMultiplicityGreaterOne = path.stream().anyMatch(prop -> prop.cardinality().maxOccurs > 1);

	    if (geometryType.isPresent()) {

		GeometryPropertyDefinition geomPropDef = new GeometryPropertyDefinition();
		geomPropDef.setGeometryType(geometryType.get());
		baseDef = geomPropDef;

		if (maxMultiplicityGreaterOne) {
		    result.addInfo(this, 108, toString(path), geometryType.get());
		}

	    } else {

		PropertyDefinition propDef = new PropertyDefinition();

		propDef.setListValuedProperty(maxMultiplicityGreaterOne);

		GfsPropertyType propertyType = gfsPropertyType(endPi);
		propDef.setType(propertyType);

		Optional<GfsPropertySubtype> propertySubtype = gfsPropertySubtype(endPi);
		if (propertySubtype.isPresent() && propertySubtype.get().getParentType() == propertyType
			&& (!maxMultiplicityGreaterOne || propertySubtype.get().isSupportedInListType())) {
		    propDef.setSubtype(propertySubtype.get());
		}

		// precision, width
		if (!maxMultiplicityGreaterOne && endPi.matches(GfsTemplateConstants.RULE_PROP_WIDTH)
			&& StringUtils.isNotBlank(endPi.taggedValue("gfsWidth"))
			&& propertyType != GfsPropertyType.FEATURE_PROPERTY
			&& (!propertySubtype.isPresent() || !(propertySubtype.get() == GfsPropertySubtype.BOOLEAN
				|| propertySubtype.get() == GfsPropertySubtype.DATE
				|| propertySubtype.get() == GfsPropertySubtype.DATETIME
				|| propertySubtype.get() == GfsPropertySubtype.TIME))) {
		    try {
			Integer width = Integer.parseInt(endPi.taggedValue("gfsWidth"));
			propDef.setWidth(width);
		    } catch (NumberFormatException e) {
			MessageContext mc = result.addError(this, 107, endPi.name(), endPi.inClass().name(), "gfsWidth",
				endPi.taggedValue("gfsWidth"));
			if (mc != null) {
			    mc.addDetail(this, 1, endPi.fullName());
			}
		    }
		}

		if (!maxMultiplicityGreaterOne && endPi.matches(GfsTemplateConstants.RULE_PROP_PRECISION)
			&& StringUtils.isNotBlank(endPi.taggedValue("gfsPrecision"))
			&& propertyType == GfsPropertyType.REAL) {
		    try {
			Integer precision = Integer.parseInt(endPi.taggedValue("gfsPrecision"));
			propDef.setPrecision(precision);
		    } catch (NumberFormatException e) {
			MessageContext mc = result.addError(this, 107, endPi.name(), endPi.inClass().name(),
				"gfsPrecision", endPi.taggedValue("gfsPrecision"));
			if (mc != null) {
			    mc.addDetail(this, 1, endPi.fullName());
			}
		    }
		}

		baseDef = propDef;
	    }

	    List<AbstractPropertyDefinition> resForBaseDef = new ArrayList<>();

	    /*
	     * set name and element path - create copies if needed (for XML attributes, and
	     * properties of datatype supertypes)
	     */
	    createPropertyDefinitionsFromBaseDefinition(baseDef, 0, path, resForBaseDef);

	    res.addAll(resForBaseDef);
	}

	return res;
    }

    private void createPropertyDefinitionsFromBaseDefinition(AbstractPropertyDefinition def, int pathIndex,
	    List<PropertyInfo> path, List<AbstractPropertyDefinition> resForBaseDef) {

	String defName = def.getName();
	String defElementPath = def.getElementPath();

	if (pathIndex == path.size()) {

	    /*
	     * end of the path has been reached (actually, we are now at an index that is
	     * just outside the range in the path list)
	     */

	    PropertyInfo endPi = path.get(path.size() - 1);

	    /*
	     * Handle case of end property with value type being a type with identity that
	     * is not mapped to one of the simple types or a geometry type (but
	     * FeatureProperty instead).
	     */
	    if ((endPi.categoryOfValue() == Options.FEATURE || endPi.categoryOfValue() == Options.OBJECT)
		    && def instanceof PropertyDefinition
		    && ((PropertyDefinition) def).getType() == GfsPropertyType.FEATURE_PROPERTY) {

		String inlineOrByReferenceTV = StringUtils.stripToNull(endPi.taggedValue("inlineOrByReference"));

		String effectiveInlineOrByReferenceValue;

		if ("inline".equalsIgnoreCase(inlineOrByReferenceTV)) {
		    effectiveInlineOrByReferenceValue = "inline";
		} else if ("byReference".equalsIgnoreCase(inlineOrByReferenceTV)) {
		    effectiveInlineOrByReferenceValue = "byReference";
		} else {
		    // TV is null, or has value 'inlineOrByReference' or an unrecognized value
		    effectiveInlineOrByReferenceValue = choiceForInlineOrByReference;
		}

		if ("inline".equalsIgnoreCase(effectiveInlineOrByReferenceValue)) {

		    if (endPi.matches(GfsTemplateConstants.RULE_PROP_INLINE_ENCODING_USES_HREF_SUFFIX)) {
			/*
			 * the values are encoded inline, and rule-gfs-prop-inlineEncodingUsesHrefSuffix
			 * applies - so add the '_href' suffix to the name
			 */
			def.setName(defName + "_href");
		    }

		    resForBaseDef.add(def);

		} else {

		    // effectiveInlineOrByReferenceValue is byReference

		    /*
		     * the values are encoded by reference, the element path must target @xlink:href
		     */
		    if (xmlAttributesToEncode.contains("href")) {

			def.setName(defName + "_href");
			def.setElementPath(defElementPath + "@href");
			PropertyDefinition propDef = (PropertyDefinition) def;
			setXmlAttributeFieldValues(propDef, propDef.getType());
			resForBaseDef.add(def);

		    } else {
			// the property is ignored, as configured
			result.addInfo(this, 104, toString(path));
		    }
		}

	    } else if (endPi.categoryOfValue() == Options.CODELIST && "3.3".equals(gmlCodeListEncodingVersion)
		    && endPi.typeClass() != null
		    && !"false".equalsIgnoreCase(endPi.typeClass().taggedValue("asDictionary"))) {

		if (xmlAttributesToEncode.contains("href")) {

		    def.setElementPath(defElementPath + "@href");
		    setXmlAttributeFieldValues((PropertyDefinition) def, GfsPropertyType.STRING);

		    resForBaseDef.add(def);

		} else {

		    // the property is ignored, as configured
		    result.addInfo(this, 103, toString(path));
		}

	    } else {

		// handle case of measure typed property (with XML attribute @uom)
		if (isGmlMeasureTypedProperty(endPi)) {

		    if (xmlAttributesToEncode.contains("uom")) {

			AbstractPropertyDefinition defCopy = def.createCopy();
			defCopy.setName(defName + xmlAttributeNameSeparator + "uom");
			defCopy.setElementPath(defElementPath + "@uom");
			setXmlAttributeFieldValues((PropertyDefinition) defCopy, GfsPropertyType.STRING);

			resForBaseDef.add(defCopy);

		    } else {

			// the property is ignored, as configured
			result.addInfo(this, 102, toString(path));
		    }
		}

		resForBaseDef.add(def);
	    }

	} else {

	    PropertyInfo pi = path.get(pathIndex);

	    String piName = pi.name();

	    if (pi.inClass().category() == Options.DATATYPE || pi.inClass().category() == Options.UNION) {

		if (!pi.inClass().subtypes().isEmpty() || multipleOccurrencesOfPropertyInInheritanceHierarchy(pi)) {

		    /*
		     * need to fan out according to names of non-abstract classes in inheritance
		     * hierarchy of the inClass, including that class (especially for the inClass is
		     * not a superclass but an encoded property with same name occurs in another
		     * class of the inheritance hierarchy)
		     */
		    List<String> nonAbstractClassNames = new ArrayList<>();
		    if (!pi.inClass().isAbstract()) {
			nonAbstractClassNames.add(pi.inClass().name());
		    }
		    for (ClassInfo subtype : pi.inClass().subtypesInCompleteHierarchy()) {
			if (!subtype.isAbstract()) {
			    nonAbstractClassNames.add(subtype.name());
			}
		    }

		    for (String className : nonAbstractClassNames) {

			AbstractPropertyDefinition defCopy = def.createCopy();

			String newDefName = defName + propertyNameSeparator + className + propertyNameSeparator
				+ piName;
			defCopy.setName(newDefName);

			String newDefElementPath = defElementPath + "|" + className + "|" + piName;
			defCopy.setElementPath(newDefElementPath);

			createPropertyDefinitionsFromBaseDefinition(defCopy, pathIndex + 1, path, resForBaseDef);
		    }

		} else {

		    String inClassName = pi.inClass().name();

		    String inClassNamePart = "";
		    if (alwaysEncodeDataTypeName && pi.inClass().category() == Options.DATATYPE) {
			inClassNamePart = propertyNameSeparator + inClassName;
		    }

		    String newDefName = defName + inClassNamePart + propertyNameSeparator + piName;
		    def.setName(newDefName);

		    String newDefElementPath = defElementPath + "|" + inClassName + "|" + piName;
		    def.setElementPath(newDefElementPath);

		    createPropertyDefinitionsFromBaseDefinition(def, pathIndex + 1, path, resForBaseDef);
		}

	    } else {

		/*
		 * the property is owned by a type with identity - which are always at the start
		 * of the path
		 */

		def.setName(piName);
		def.setElementPath(piName);

		createPropertyDefinitionsFromBaseDefinition(def, pathIndex + 1, path, resForBaseDef);
	    }
	}
    }

    /**
     * @param pi
     * @return <code>true</code>, if a property with same name as pi occurs in the
     *         inheritance hierarchy of pi's inClass, and is encoded, else
     *         <code>false</code>
     */
    private boolean multipleOccurrencesOfPropertyInInheritanceHierarchy(PropertyInfo pi) {

	Optional<ClassInfo> topSupertypeOpt = pi.inClass().supertypesInCompleteHierarchy().stream()
		.filter(superCi -> superCi.supertypes().isEmpty()).findAny();

	if (topSupertypeOpt.isEmpty()) {
	    return false;
	} else {
	    int countPropsWithPiName = 0;
	    ClassInfo topSupertype = topSupertypeOpt.get();
	    Set<ClassInfo> allClasses = new HashSet<>();
	    allClasses.addAll(topSupertype.subtypesInCompleteHierarchy());
	    allClasses.add(topSupertype);
	    for (ClassInfo ci : allClasses) {
		for (PropertyInfo pix : ci.properties().values()) {
		    if (pix.name().equals(pi.name())) {
			countPropsWithPiName++;
		    }
		}
	    }
	    return countPropsWithPiName > 1;
	}
    }

    private void setXmlAttributeFieldValues(PropertyDefinition pd, GfsPropertyType propertyType) {

	pd.setType(propertyType);
	pd.setPrecision(0);
	pd.setWidth(0);
	pd.setSubtype(null);
    }

    private Optional<GfsPropertySubtype> gfsPropertySubtype(PropertyInfo pi) {

	String typeName = pi.typeInfo().name;
	String encodingRule = pi.encodingRule(GfsTemplateConstants.PLATFORM);

	String subtype = mapEntryParamInfos.getCharacteristic(typeName, encodingRule,
		GfsTemplateConstants.ME_PARAM_TYPE_DETAILS, GfsTemplateConstants.ME_PARAM_TYPE_DETAILS_CHARACT_SUBTYPE);

	if (StringUtils.isNotBlank(subtype)) {
	    return GfsPropertySubtype.fromString(subtype);
	} else {
	    return Optional.empty();
	}
    }

    /**
     * Used to create the list of property lists. Each property list in
     * pathsToEndProperties represents the path to an "end" property, i.e., one that
     * either is mapped, has a type with identity, enumeration or code list as type.
     * 
     * Circles within a property list are avoided.
     * 
     * @param pi                      property to consider
     * @param propListUpToIncludingPi a list of property infos that lead to pi; MUST
     *                                include pi
     * @param pathsToEndProperties    list in which to add the propListForPi if it
     *                                is an "end" property
     */
    private void identifyPropertyPaths(PropertyInfo pi, List<PropertyInfo> propListUpToIncludingPi,
	    List<List<PropertyInfo>> pathsToEndProperties) {

	if (propListUpToIncludingPi.get(propListUpToIncludingPi.size() - 1) != pi) {
	    throw new IllegalArgumentException(
		    "Wrong use of method identifyPropertyPaths(..) - argument propListUpToIncludingPi does not contain argument pi as last element");
	}

	if (!isEncoded(pi)) {
	    return;
	}

	// ensure that the value type of pi is encoded
	if (pi.typeClass() != null && !isEncoded(pi.typeClass())) {
	    MessageContext mc = result.addWarning(this, 100, pi.typeInfo().name, pi.name());
	    if (mc != null) {
		mc.addDetail(this, 1, pi.fullNameInSchema());
	    }
	}

	if (valueTypeIsMapped(pi) || pi.categoryOfValue() == Options.FEATURE || pi.categoryOfValue() == Options.OBJECT
		|| pi.categoryOfValue() == Options.ENUMERATION || pi.categoryOfValue() == Options.CODELIST) {

	    pathsToEndProperties.add(propListUpToIncludingPi);

	} else if (pi.categoryOfValue() == Options.DATATYPE || pi.categoryOfValue() == Options.UNION) {

	    // fan out, but watch out for and avoid circles in paths!
	    String typeCiName = pi.typeInfo().name;

	    // detect circle
	    if (StringUtils.isNotBlank(typeCiName)
		    && propListUpToIncludingPi.stream().limit(propListUpToIncludingPi.size() - 1)
			    .anyMatch(prop -> prop.typeInfo().name.equals(typeCiName))) {

		result.addWarning(this, 111, toString(propListUpToIncludingPi), typeCiName);
	    }

	    ClassInfo typeCi = pi.typeClass();

	    if (typeCi == null) {

		result.addError(this, 105, typeCiName, toString(propListUpToIncludingPi));

	    } else {

		fanOutPropertyPath(typeCi, propListUpToIncludingPi, pathsToEndProperties);

		/*
		 * also handle the case of typeCi being a <<dataType>> that is a supertype ...
		 * then we must recognize the properties of subtypes as well
		 */
		if (typeCi.category() == Options.DATATYPE && !typeCi.subtypes().isEmpty()) {

		    for (ClassInfo typeCiSubtype : typeCi.subtypesInCompleteHierarchy()) {

			/*
			 * handle the rare case that a subtype is abstract and a leaf class (does not
			 * make much sense, but still ...)
			 */
			if (typeCiSubtype.isAbstract() && typeCiSubtype.subtypes().isEmpty()) {

			    result.addWarning(this, 106, typeCi.name(), toString(propListUpToIncludingPi),
				    typeCiSubtype.name());

			} else {

			    fanOutPropertyPath(typeCiSubtype, propListUpToIncludingPi, pathsToEndProperties);
			}
		    }
		}
	    }

	} else {

	    // case unsupported
	    result.addError(this, 110, pi.typeInfo().name, toString(propListUpToIncludingPi));
	}
    }

    private void fanOutPropertyPath(ClassInfo typeCi, List<PropertyInfo> propListUpToIncludingPi,
	    List<List<PropertyInfo>> pathsToEndProperties) {

	for (PropertyInfo typePi : typeCi.properties().values()) {

	    /*
	     * ignore the property if it is overriding/restricting a supertype property - or
	     * if the property is not encoded
	     */
	    if (typePi.isRestriction() || !isEncoded(typePi)) {
		continue;
	    }

	    List<PropertyInfo> newPath = new ArrayList<>();
	    newPath.addAll(propListUpToIncludingPi);
	    newPath.add(typePi);

	    identifyPropertyPaths(typePi, newPath, pathsToEndProperties);
	}
    }

    private String toString(List<PropertyInfo> propPath) {

	StringBuffer sb = new StringBuffer();
	sb.append(propPath.get(0).inClass().name());
	for (PropertyInfo pi : propPath) {
	    sb.append(".");
	    sb.append(pi.name());
	}
	return sb.toString();
    }

    private boolean valueTypeIsMapped(PropertyInfo pi) {

	return valueTypeIsMapped(pi.typeInfo().name, pi.typeInfo().id, pi.encodingRule(GfsTemplateConstants.PLATFORM));
    }

    private boolean valueTypeIsMapped(String typeName, String typeId, String encodingRule) {

	ProcessMapEntry pme = mapEntryParamInfos.getMapEntry(typeName, encodingRule);

	if (pme != null) {
	    return true;
	} else {
	    return false;
	}
    }

    private boolean isGmlMeasureTypedProperty(PropertyInfo pi) {

	String typeName = pi.typeInfo().name;
	String encodingRule = pi.encodingRule(GfsTemplateConstants.PLATFORM);

	String gmlMeasureTypeCharacteristicValue = mapEntryParamInfos.getCharacteristic(typeName, encodingRule,
		GfsTemplateConstants.ME_PARAM_TYPE_DETAILS,
		GfsTemplateConstants.ME_PARAM_TYPE_DETAILS_CHARACT_GMLMEASURETYPE);

	return "true".equalsIgnoreCase(gmlMeasureTypeCharacteristicValue);
    }

    private GfsPropertyType gfsPropertyType(PropertyInfo pi) {

	return gfsPropertyType(pi.typeInfo().name, pi.typeInfo().id, pi.encodingRule(GfsTemplateConstants.PLATFORM));
    }

    private GfsPropertyType gfsPropertyType(String typeName, String typeId, String encodingRule) {

	ProcessMapEntry pme = mapEntryParamInfos.getMapEntry(typeName, encodingRule);

	GfsPropertyType resType = GfsPropertyType.STRING;

	if (pme != null) {

	    if (pme.hasTargetType()) {

		Optional<GfsPropertyType> typeFromTargetType = GfsPropertyType.fromString(pme.getTargetType());
		if (typeFromTargetType.isPresent()) {
		    resType = typeFromTargetType.get();
		} else {
		    result.addError(this, 109, pme.getTargetType());
		}

	    } else {
		// is checked via target configuration validator (which can be switched off)
		result.addError(this, 112, typeName);
	    }

	} else {

	    ClassInfo valueType = null;
	    if (StringUtils.isNotBlank(typeId)) {
		valueType = model.classById(typeId);
	    }
	    if (valueType == null && StringUtils.isNotBlank(typeName)) {
		valueType = model.classByName(typeName);
	    }

	    if (valueType == null) {

		// The value type was not found in the model
		result.addError(this, 113, typeName);

	    } else if (!isEncoded(valueType)) {

		result.addError(this, 114, typeName);

	    } else if (valueType.category() == Options.OBJECT || valueType.category() == Options.FEATURE) {
		resType = GfsPropertyType.FEATURE_PROPERTY;
	    }
	}

	return resType;
    }

    private Optional<String> geometryType(PropertyInfo pi) {

	String typeName = pi.typeInfo().name;
	String encodingRule = pi.encodingRule(GfsTemplateConstants.PLATFORM);

	ProcessMapEntry pme = options.targetMapEntry(typeName, encodingRule);

	if (pme != null && GfsPropertyType.fromString(pme.getTargetType()).isEmpty()) {
	    return Optional.of(pme.getTargetType());
	} else {
	    return Optional.empty();
	}
    }

    @Override
    public void registerRulesAndRequirements(RuleRegistry r) {

	r.addRule(GfsTemplateConstants.RULE_ALL_NOT_ENCODED);
	r.addRule(GfsTemplateConstants.RULE_PROP_INLINE_ENCODING_USES_HREF_SUFFIX);
	r.addRule(GfsTemplateConstants.RULE_PROP_PRECISION);
	r.addRule(GfsTemplateConstants.RULE_PROP_WIDTH);
    }

    @Override
    public String getDefaultEncodingRule() {
	return "*";
    }

    @Override
    public String getTargetName() {
	return "gfs template";
    }

    @Override
    public String getTargetIdentifier() {
	return "gfs";
    }

    @Override
    public String message(int mnr) {

	switch (mnr) {

	case 0:
	    return "Context: class '$1$'";
	case 1:
	    return "Context: property '$1$'";

	case 3:
	    return "Context: class GfsTemplateTarget";
	case 4:
	    return "Processing class '$1$'.";
	case 5:
	    return "Directory named '$1$' does not exist or is not accessible.";
	case 6:
	    return "System error: Exception encountered. Message is: '$1$'";
	case 7:
	    return "Schema '$1$' is not encoded.";
	case 8:
	    return "Class '$1$' is not encoded.";
	case 9:
	    return "Stack trace is: $1$";

	case 14:
	    return "Type '$1$' has been mapped to '$2$', as defined by the configuration.";
	case 15:
	    return "No map entries provided via the configuration.";
//	case 16:
//	    return "Value '$1$' of configuration parameter $2$ does not match the regular expression: $3$. The parameter will be ignored.";
	case 17:
	    return "Type '$1$' is of a category not enabled for conversion, meaning that no gfs template items will be created to represent it.";
	case 18:
	    return "Schema '$1$' is not encoded. Thus class '$2$' (which belongs to that schema) is not encoded either.";

	case 100:
	    return "??Value type '$1$' of property '$2$' is not encoded. The property would be used in one or more property paths, but must be ignored, since the value type is not encoded. Check if type '$1$' and all properties that use it as value type shall really not be encoded. If that is the case, this warning can be ignored.";
	case 101:
	    return "Could not write the output file(s) (stack trace is available on log level debug): $1$";
	case 102:
	    return "??The property at the end of the property path '$1$' has a value type that is mapped with 'typeDetails' parameter and 'gmlMeasureType' characteristic set to 'true'. However, target parameter 'xmlAttributesToEncode' does not include 'uom'. Therefore, no additional property definition for the 'uom' XML attribute is created for this path.";
	case 103:
	    return "??The property at the end of the property path '$1$' has a code list as value type, which has tagged value 'asDictionary' not set to 'false'. In addition, target parameter "
		    + GfsTemplateConstants.PARAM_GML_CODE_LIST_ENCODING_VERSION
		    + " has value 3.3. The code value should therefore be encoded in GML using an xlink:href XML attribute. However, target parameter 'xmlAttributesToEncode' does not include 'href'. Therefore, no property definition is created for this path.";
	case 104:
	    return "??The property at the end of the property path '$1$' has a type with identity as value type which is implemented as a 'FeatureProperty'. The value is (expected to be) encoded by-reference, in GML using an xlink:href attribute. However, target parameter 'xmlAttributesToEncode' does not include 'href'. Therefore, no property definition is created for this path.";
	case 105:
	    return "??Type '$1$', which is the value type of the last property in property path '$2$', is not mapped, but was also not found in the model. The property path is ignored.";
	case 106:
	    return "??Type '$1$', which is the value type of the last property in property path '$2$', has subtype '$3$'. That subtype is an abstract leaf class. No property paths will be created for the properties of that subtype.";
	case 107:
	    return "Property '$1$' of type '$2$' has tag '$3$' with invalid integer value ('$4$'). The tag is ignored.";
	case 108:
	    return "??Property path '$1$' - with end property having a geometry type that is implemented as gfs geometry type '$2$' - leads to a multiplicity thas is greater than one, which cannot be represented (and thus max multiplicity = 1 is used instead). This can be significant if the geometry type is not a multi-geometry.";
	case 109:
	    return "??Map entry with targetType '$1$' is invalid, because the targetType is not one of the recognized gfs property types. Using 'String' instead.";
	case 110:
	    return "??Type '$1$', which is the value type of the last property in property path '$2$', is of a category that is not supported by the path creation logic. The property path is ignored.";
	case 111:
	    return "??Property path '$1$' would lead to a property circle, because the value type of the last property in the path (the value type being '$2$') is already used as value type in one of the previous path properties. The property path is therefore ignored.";
	case 112:
	    return "??No target type is defined in map entry for type '$1$'. Assuming type 'String'.";
	case 113:
	    return "??Type definition for type '$1$' could not be identified. No map entry is defined for the type, and the type was not found in the model. Assuming type 'String'.";
	case 114:
	    return "??Type '$1$' is marked to not be encoded. Could not identify a type for it. Assuming type 'String'.";

	case 10001:
	    return "Generating gfs template items for application schema $1$.";
	case 10002:
	    return "Diagnostics-only mode. All output to files is suppressed.";
	default:
	    return "(" + GfsTemplateTarget.class.getName() + ") Unknown message with number: " + mnr;
	}
    }
}
