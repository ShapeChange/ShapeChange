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
 * (c) 2002-2023 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core.target.ldproxy2.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import de.ii.ldproxy.cfg.LdproxyCfgWriter;
import de.ii.ogcapi.features.gml.domain.GmlConfiguration.SrsNameStyle;
import de.ii.ogcapi.features.gml.domain.GmlConfiguration.UomStyle;
import de.ii.ogcapi.features.gml.domain.ImmutableGmlConfiguration;
import de.ii.ogcapi.features.gml.domain.ImmutableGmlIdentifier;
import de.ii.ogcapi.features.gml.domain.ImmutableSrsNameMapping;
import de.ii.ogcapi.features.gml.domain.ImmutableUomMapping;
import de.ii.ogcapi.features.gml.domain.ImmutableVariableName;
import de.ii.xtraplatform.crs.domain.ImmutableEpsgCrs;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.interactive_instruments.shapechange.core.MessageSource;
import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.XmlNamespace;
import de.interactive_instruments.shapechange.core.model.ClassInfo;
import de.interactive_instruments.shapechange.core.model.Model;
import de.interactive_instruments.shapechange.core.model.PackageInfo;
import de.interactive_instruments.shapechange.core.model.PropertyInfo;
import de.interactive_instruments.shapechange.core.target.ldproxy2.LdpGidEncoder;
import de.interactive_instruments.shapechange.core.target.ldproxy2.LdpInfo;
import de.interactive_instruments.shapechange.core.target.ldproxy2.LdpSrsNameMapping;
import de.interactive_instruments.shapechange.core.target.ldproxy2.LdpSrsNameMappings;
import de.interactive_instruments.shapechange.core.target.ldproxy2.LdpUomMapping;
import de.interactive_instruments.shapechange.core.target.ldproxy2.LdpUomMappings;
import de.interactive_instruments.shapechange.core.target.ldproxy2.Ldproxy2Constants;
import de.interactive_instruments.shapechange.core.target.ldproxy2.Ldproxy2Target;
import de.interactive_instruments.shapechange.core.target.xml_encoding_util.XmlEncodingInfos;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class LdpBuildingBlockFeaturesGmlBuilder extends LdpBuildingBlockBuilder {

    protected ShapeChangeResult result;
    protected MessageSource msgSource;

    protected PackageInfo mainAppSchema;
    protected Model model;

    protected Ldproxy2Target target;

    protected SortedMap<String, String> gmlNsabrByNs = new TreeMap<>();
    protected SortedMap<String, String> gmlObjectTypeNamespacesMap = new TreeMap<>();

    protected Map<ClassInfo, List<String>> xmlAttributes_Gml_OfServiceConfigCollectionsByTopLevelClass = new HashMap<>();
    protected Map<ClassInfo, Map<String, String>> codelistPropertiesByTopLevelClass = new HashMap<>();
    protected Map<String, String> globalCodelistProperties = new HashMap<>();
    protected Map<ClassInfo, Map<String, List<String>>> valueWrapsByTopLevelClass = new HashMap<>();
    protected Map<ClassInfo, SortedSet<String>> objectTypeSuffixedPropertiesByTopLevelClass = new HashMap<>();
    
    protected Map<String, String> gmlFixmeByOriginalSchemaNameMap = new HashMap<>();
    protected int gmlFixmeCounter = 1;

    protected String gmlIdPrefix = null;
    protected boolean gmlIdOnGeometries = false;
    protected Integer gmlSfLevel = null;
    protected String gmlFeatureCollectionElementName = null;
    protected String gmlFeatureMemberElementName = null;
    protected String gmlFeatureRefTemplate = null;
    protected boolean gmlSupportsStandardResponseParameters = false;
    protected boolean gmlUseAlias = false;
    protected boolean gmlUseSurfaceAndCurve = false;
    protected String gmlIdentifierCodeSpace = null;
    protected String gmlIdentifierValueTemplate = null;
    protected boolean appendTemporalSuffixToGmlId = false;
    protected String gmlCodelistUriTemplate = null;
    protected boolean gmlIdentifyCodelistProperties = false;

    protected SortedMap<String, ClassInfo> genericValueTypes = new TreeMap<>();

    protected XmlEncodingInfos xmlEncodingInfos = null;
    protected LdpSrsNameMappings srsNameMappings = null;
    protected LdpUomMappings uomMappings = null;

    protected LdpGidEncoder gidEncoder = null;
    protected boolean isResourcesConfigRequired = false;

    public boolean isResourcesConfigRequired() {
	return this.isResourcesConfigRequired;
    }

    public LdpBuildingBlockFeaturesGmlBuilder(ShapeChangeResult result, Ldproxy2Target target,
	    PackageInfo mainAppSchema, Model model, String gmlIdPrefix, boolean gmlIdOnGeometries, int gmlSfLevel,
	    String gmlFeatureCollectionElementName, String gmlFeatureMemberElementName,
	    boolean gmlSupportsStandardResponseParameters, boolean gmlUseAlias, boolean gmlUseSurfaceAndCurve,
	    String gmlFeatureRefTemplate, String gmlIdentifierCodeSpace, String gmlIdentifierValueTemplate,
	    boolean appendTemporalSuffixToGmlId, String gmlCodelistUriTemplate, boolean gmlIdentifyCodelistProperties,
	    XmlEncodingInfos xmlEncodingInfos, LdpSrsNameMappings srsNameMappings, LdpUomMappings uomMappings) {

	super();

	this.result = result;
	this.msgSource = target;
	this.target = target;

	this.mainAppSchema = mainAppSchema;
	this.model = model;

	this.gmlIdPrefix = gmlIdPrefix;
	this.gmlIdOnGeometries = gmlIdOnGeometries;
	this.gmlSfLevel = gmlSfLevel;
	this.gmlFeatureCollectionElementName = gmlFeatureCollectionElementName;
	this.gmlFeatureMemberElementName = gmlFeatureMemberElementName;
	this.gmlSupportsStandardResponseParameters = gmlSupportsStandardResponseParameters;
	this.gmlUseAlias = gmlUseAlias;
	this.gmlUseSurfaceAndCurve = gmlUseSurfaceAndCurve;
	this.gmlFeatureRefTemplate = gmlFeatureRefTemplate;
	this.gmlIdentifierCodeSpace = gmlIdentifierCodeSpace;
	this.gmlIdentifierValueTemplate = gmlIdentifierValueTemplate;
	this.appendTemporalSuffixToGmlId = appendTemporalSuffixToGmlId;
	this.gmlCodelistUriTemplate = gmlCodelistUriTemplate;
	this.gmlIdentifyCodelistProperties = gmlIdentifyCodelistProperties;

	this.xmlEncodingInfos = xmlEncodingInfos;
	this.srsNameMappings = srsNameMappings;
	this.uomMappings = uomMappings;

	this.addNsabrByNs(mainAppSchema.targetNamespace(), mainAppSchema.xmlns());
    }

    public boolean gmlAsAttribute(PropertyInfo pi) {

	String originalInClassName = LdpInfo.originalInClassName(pi);
	String originalSchemaName = LdpInfo.originalSchemaName(pi);
	String originalPropertyName = LdpInfo.originalPropertyName(pi);

	return xmlEncodingInfos.isXmlAttribute(originalSchemaName, originalInClassName, originalPropertyName);
    }

    public String gmlQName(PropertyInfo pi) {

	String originalInClassName = LdpInfo.originalInClassName(pi);
	String originalSchemaName = LdpInfo.originalSchemaName(pi);
	String originalPropertyName = LdpInfo.originalPropertyName(pi);

	String xmlNamespaceToUse = gmlXmlNamespace(pi);

	String xmlNameToUse = originalPropertyName;
	Optional<String> xmlNameOpt = xmlEncodingInfos.getXmlName(originalSchemaName, originalInClassName,
		originalPropertyName);
	if (xmlNameOpt.isPresent() && !xmlNameOpt.get().equals(originalPropertyName)) {
	    xmlNameToUse = xmlNameOpt.get();
	}

	String nsabrToUse = gmlNsabr(xmlNamespaceToUse);

	if (nsabrToUse.equals(mainAppSchema.xmlns())) {
	    return xmlNameToUse;
	} else {
	    return nsabrToUse + ":" + xmlNameToUse;
	}
    }

    public String gmlXmlNamespace(ClassInfo ci) {

	String className = LdpInfo.originalClassName(ci);

	PackageInfo schemaPkg = model.schemaPackage(ci);
	String schemaName = schemaPkg == null ? null : schemaPkg.name();

	Optional<String> xmlNsOpt = xmlEncodingInfos.getXmlNamespace(schemaName, className);

	if (xmlNsOpt.isPresent()) {
	    return xmlNsOpt.get();
	} else {

	    String targetNamespace = ci.pkg().targetNamespace();
	    if (StringUtils.isBlank(targetNamespace)) {

		// Note: schemaName can be null - is supported as key by Java HashMap
		if (gmlFixmeByOriginalSchemaNameMap.containsKey(schemaName)) {
		    targetNamespace = gmlFixmeByOriginalSchemaNameMap.get(schemaName);
		} else {
		    targetNamespace = gmlFixmeForSchema(schemaName);
		    result.addWarning(msgSource, 129, schemaName == null ? "<no schema found>" : schemaName,
			    targetNamespace);
		}
	    }
	    return targetNamespace;
	}
    }

    public String gmlXmlNamespace(PropertyInfo pi) {

	String originalInClassName = LdpInfo.originalInClassName(pi);
	String originalSchemaName = LdpInfo.originalSchemaName(pi);
	String originalPropertyName = LdpInfo.originalPropertyName(pi);

	String piTargetNamespace = pi.inClass().pkg().targetNamespace();
	Optional<String> xmlNsOpt = xmlEncodingInfos.getXmlNamespace(originalSchemaName,
		originalInClassName + "::" + originalPropertyName);

	String xmlNamespaceToUse = piTargetNamespace;
	if (xmlNsOpt.isPresent()) {
	    if (!xmlNsOpt.get().equals(piTargetNamespace)) {
		xmlNamespaceToUse = xmlNsOpt.get();
	    }
	} else {
	    if (!originalSchemaName.equals(pi.model().schemaPackage(pi.inClass()).name())) {
		SortedSet<PackageInfo> originalSchema = model.schemas(originalSchemaName);
		if (originalSchema.isEmpty() || StringUtils.isBlank(originalSchema.getFirst().targetNamespace())) {
		    if (gmlFixmeByOriginalSchemaNameMap.containsKey(originalSchemaName)) {
			xmlNamespaceToUse = gmlFixmeByOriginalSchemaNameMap.get(originalSchemaName);
		    } else {
			xmlNamespaceToUse = gmlFixmeForSchema(originalSchemaName);
			result.addWarning(msgSource, 129, originalSchemaName, xmlNamespaceToUse);
		    }
		} else {
		    xmlNamespaceToUse = originalSchema.getFirst().targetNamespace();
		}
	    }
	}

	return xmlNamespaceToUse;
    }

    public String gmlNsabr(String xmlNamespace) {
	return gmlNsabr(xmlNamespace, null);
    }

    public String gmlNsabr(String xmlNamespace, String defaultNamespaceAbbreviation) {

	if (gmlNsabrByNs.containsKey(xmlNamespace)) {
	    return gmlNsabrByNs.get(xmlNamespace);
	} else {

	    String nsabrToUse = null;

	    Optional<String> nsabr = xmlEncodingInfos.findNsabr(xmlNamespace);
	    if (nsabr.isPresent()) {
		nsabrToUse = nsabr.get();
	    } else {
		boolean foundSchemaPackage = false;
		for (PackageInfo schema : model.schemas(null)) {
		    if (schema.targetNamespace().equals(xmlNamespace)) {
			foundSchemaPackage = true;
			nsabrToUse = schema.xmlns();
			break;
		    }
		}
		if (!foundSchemaPackage || StringUtils.isBlank(nsabrToUse)) {

		    if (StringUtils.isBlank(defaultNamespaceAbbreviation)) {
			String fixme = gmlNewFixme();
			result.addWarning(msgSource, 130, xmlNamespace, fixme);
			nsabrToUse = fixme;
		    } else {
			nsabrToUse = defaultNamespaceAbbreviation;
		    }
		}
	    }

	    gmlNsabrByNs.put(xmlNamespace, nsabrToUse);
	    return nsabrToUse;
	}
    }

    public String gmlFixmeForSchema(String originalSchemaName) {

	String newFixme = gmlNewFixme();
	gmlFixmeByOriginalSchemaNameMap.put(originalSchemaName, newFixme);
	return newFixme;
    }

    public String gmlNewFixme() {
	String newFixme = "fixme" + gmlFixmeCounter;
	gmlFixmeCounter++;
	return newFixme;
    }

    public boolean gmlRenameRequired(PropertyInfo pi, String xmlNamespaceOfRootCollection) {

	String originalInClassName = LdpInfo.originalInClassName(pi);
	String originalSchemaName = LdpInfo.originalSchemaName(pi);
	String originalPropertyName = LdpInfo.originalPropertyName(pi);

	Optional<String> xmlNsOpt = xmlEncodingInfos.getXmlNamespace(originalSchemaName,
		originalInClassName + "::" + originalPropertyName);

	boolean renameRequired = false;

	if (xmlNsOpt.isPresent()) {
	    if (!xmlNsOpt.get().equals(xmlNamespaceOfRootCollection)) {
		renameRequired = true;
	    }
	} else {

	    PackageInfo piSchema = model.schemaPackage(pi.inClass());

	    if (StringUtils.isNotBlank(piSchema.targetNamespace())
		    && !piSchema.targetNamespace().equals(xmlNamespaceOfRootCollection)) {
		renameRequired = true;
	    } else if (!originalSchemaName.equals(piSchema.name())) {
		/*
		 * since different schemas must have different target namespaces, the XML
		 * namespace for the property changes
		 */
		renameRequired = true;
	    }
	}

	if (!originalPropertyName.equals(pi.name())) {
	    renameRequired = true;
	}

	Optional<String> xmlNameOpt = xmlEncodingInfos.getXmlName(originalSchemaName, originalInClassName,
		originalPropertyName);
	if (xmlNameOpt.isPresent() && !xmlNameOpt.get().equals(originalPropertyName)) {
	    renameRequired = true;
	}

	/*
	 * If aliases are used, do not create rename transformations.
	 */
	return renameRequired && !gmlUseAlias;
    }

    public void addNsabrByNs(String ns, String nsabr) {
	this.gmlNsabrByNs.put(ns, nsabr);
    }

    public void register(ClassInfo ci) {

	String nsabr = gmlNsabr(gmlXmlNamespace(ci));
	if (!nsabr.equals(Ldproxy2Target.mainAppSchema.xmlns())) {
	    gmlObjectTypeNamespacesMap.put(LdpInfo.originalClassName(ci), nsabr);
	}

	if (target.isGenericValueType(ci)) {
	    genericValueTypes.put(ci.name(), ci);
	}
    }

    public void register(String className, String nsabr) {
	gmlObjectTypeNamespacesMap.put(className, nsabr);
    }

    @Override
    public ImmutableGmlConfiguration getServiceConfiguration(LdproxyCfgWriter cfg) {

	ImmutableGmlConfiguration.Builder builder = cfg.builder().ogcApiExtension().gml();
	builder.enabled(true);

	if (!super.propertyTransformationsForGlobalBuildingBlockOfServiceConfig.isEmpty()) {
	    builder.transformations(propertyTransformationsForGlobalBuildingBlockOfServiceConfig);
	}

	if (!globalCodelistProperties.isEmpty()) {
	    builder.codelistProperties(globalCodelistProperties);
	}

	SortedMap<String, String> appNamespaces = new TreeMap<>();

	for (Entry<String, String> e : gmlNsabrByNs.entrySet()) {
	    appNamespaces.put(e.getValue(), e.getKey());
	}
	builder.putAllApplicationNamespaces(appNamespaces);

	builder.defaultNamespace(Ldproxy2Target.mainAppSchema.xmlns());

	String schemaLocationForMainAppSchema = xmlEncodingInfos.getXmlNamespaces().stream()
		.filter(xn -> xn.getNs().equals(mainAppSchema.targetNamespace()) && xn.hasLocation())
		.map(xn -> xn.getLocation()).findFirst().orElse(null);
	if (StringUtils.isBlank(schemaLocationForMainAppSchema)) {
	    // Assume that the XSD for the main schema is hosted as local resource
	    this.isResourcesConfigRequired = true;
	    schemaLocationForMainAppSchema = "{{serviceUrl}}/resources/" + Ldproxy2Target.mainAppSchema.xsdDocument();
	    if (!schemaLocationForMainAppSchema.toLowerCase(Locale.ENGLISH).endsWith(".xsd")) {
		schemaLocationForMainAppSchema += ".xsd";
	    }
	}

	SortedMap<String, String> schemaLocations = new TreeMap<>();
	schemaLocations.put(Ldproxy2Target.mainAppSchema.xmlns(), schemaLocationForMainAppSchema);
	for (XmlNamespace xns : xmlEncodingInfos.getXmlNamespaces()) {
	    if (xns.hasLocation() && gmlNsabrByNs.containsKey(xns.getNs())) {
		schemaLocations.put(xns.getNsabr(), xns.getLocation());
	    }
	}

	builder.putAllSchemaLocations(schemaLocations);

	builder.objectTypeNamespaces(gmlObjectTypeNamespacesMap);

	if (StringUtils.isNotBlank(gmlIdPrefix)) {
	    builder.gmlIdPrefix(gmlIdPrefix);
	}
	if (gmlIdOnGeometries) {
	    builder.gmlIdOnGeometries(true);
	}
	if (gmlSfLevel != null && gmlSfLevel != -1) {
	    builder.gmlSfLevel(gmlSfLevel);
	}
	if (StringUtils.isNotBlank(gmlFeatureCollectionElementName)) {
	    builder.featureCollectionElementName(gmlFeatureCollectionElementName);
	}
	if (StringUtils.isNotBlank(gmlFeatureMemberElementName)) {
	    builder.featureMemberElementName(gmlFeatureMemberElementName);
	}
	if (StringUtils.isNotBlank(gmlFeatureRefTemplate)) {
	    builder.featureRefTemplate(gmlFeatureRefTemplate);
	}
	if (StringUtils.isNotBlank(gmlCodelistUriTemplate)) {
	    builder.codelistUriTemplate(gmlCodelistUriTemplate);
	}
	if (StringUtils.isNotBlank(gmlIdentifierCodeSpace)) {
	    ImmutableGmlIdentifier.Builder gi = new ImmutableGmlIdentifier.Builder();
	    gi.codeSpace(gmlIdentifierCodeSpace);
	    if (StringUtils.isNotBlank(gmlIdentifierValueTemplate)) {
		gi.valueTemplate(gmlIdentifierValueTemplate);
	    }
	    builder.gmlIdentifier(gi.build());
	}

	builder.supportsStandardResponseParameters(gmlSupportsStandardResponseParameters);
	if (gmlUseAlias) {
	    builder.useAlias(gmlUseAlias);
	}
	builder.useSurfaceAndCurve(gmlUseSurfaceAndCurve);
	if (appendTemporalSuffixToGmlId) {
	    builder.appendTemporalSuffixToGmlId(appendTemporalSuffixToGmlId);
	}

	if (!genericValueTypes.isEmpty()) {
	    for (ClassInfo gvt : genericValueTypes.values()) {
		ImmutableVariableName.Builder ivn = new ImmutableVariableName.Builder();
		ivn.property("dataType");
		SortedMap<String, String> mapping = new TreeMap<>();
		for (ClassInfo subtype : gvt.subtypesInCompleteHierarchy()) {
		    String nsabr = gmlNsabr(subtype.pkg().targetNamespace());
		    mapping.put(subtype.name(), nsabr + ":" + subtype.name());
		}
		ivn.mapping(mapping);
		builder.putVariableObjectElementNames(gvt.name(), ivn.build());
	    }
	}

	if (this.srsNameMappings != null && !this.srsNameMappings.isEmpty()) {
	    builder.srsNameStyle(SrsNameStyle.TEMPLATE);
	    for (LdpSrsNameMapping snm : this.srsNameMappings.getSrsNameMappings()) {

		ImmutableEpsgCrs crs = new ImmutableEpsgCrs.Builder().code(snm.getCode())
			.forceAxisOrder(snm.getForceAxisOrder()).verticalCode(snm.getVerticalCode()).build();
		ImmutableSrsNameMapping isnm = new ImmutableSrsNameMapping.Builder().crs(crs).value(snm.getValue())
			.build();
		builder.addSrsNameMappings(isnm);
	    }
	}

	if (this.uomMappings != null && !this.uomMappings.isEmpty()) {
	    builder.uomStyle(UomStyle.TEMPLATE);
	    for (LdpUomMapping uom : this.uomMappings.getUomMappings()) {

		ImmutableUomMapping iuom = new ImmutableUomMapping.Builder().uom(uom.getUom()).value(uom.getValue())
			.build();
		builder.addUomMappings(iuom);
	    }
	}

	return builder.build();
    }

    @Override
    public ImmutableGmlConfiguration getConfigurationForServiceCollection(LdproxyCfgWriter cfg, ClassInfo ci) {

	ImmutableGmlConfiguration.Builder builder = cfg.builder().ogcApiExtension().gml();

	if (super.propertyTransformationsForBuildingBlockOfServiceConfigCollectionsByTopLevelClass.containsKey(ci)) {
	    SortedMap<String, List<PropertyTransformation>> gmlPropertyTransformations = super.propertyTransformationsForBuildingBlockOfServiceConfigCollectionsByTopLevelClass
		    .get(ci);
	    builder.transformations(gmlPropertyTransformations);
	}
	if (xmlAttributes_Gml_OfServiceConfigCollectionsByTopLevelClass.containsKey(ci)) {
	    List<String> xmlAttributeCases = xmlAttributes_Gml_OfServiceConfigCollectionsByTopLevelClass.get(ci);
	    builder.addAllXmlAttributes(xmlAttributeCases);
	}

	if (gmlIdentifyCodelistProperties && codelistPropertiesByTopLevelClass.containsKey(ci)) {
	    builder.codelistProperties(codelistPropertiesByTopLevelClass.get(ci));
	}

	if (valueWrapsByTopLevelClass.containsKey(ci)) {
	    builder.valueWrap(valueWrapsByTopLevelClass.get(ci));
	}
	
	if(objectTypeSuffixedPropertiesByTopLevelClass.containsKey(ci)) {
	    builder.objectTypeSuffixedProperties(objectTypeSuffixedPropertiesByTopLevelClass.get(ci));
	}

	return builder.build();
    }

    public void register(PropertyInfo pi, ClassInfo topLevelClass, String propertyPath) {

	if (gmlRenameRequired(pi, gmlXmlNamespace(topLevelClass))) {

	    String xmlQNameToUse = gmlQName(pi);

	    ImmutablePropertyTransformation trf = new ImmutablePropertyTransformation.Builder().rename(xmlQNameToUse)
		    .build();
	    addPropertyTransformationToBuildingBlockOfCollectionInServiceConfiguration(topLevelClass, pi, propertyPath,
		    trf);
	}

	if (gmlAsAttribute(pi)) {

	    List<String> xmlAttributes = xmlAttributes_Gml_OfServiceConfigCollectionsByTopLevelClass.get(topLevelClass);
	    if (xmlAttributes == null) {
		xmlAttributes = new ArrayList<>();
		xmlAttributes_Gml_OfServiceConfigCollectionsByTopLevelClass.put(topLevelClass, xmlAttributes);
	    }
	    xmlAttributes.add(propertyPath);
	}

	if (pi.categoryOfValue() == Options.CODELIST) {

	    Map<String, String> cpMap;

	    if (isGlobalPropertyDefinitions(pi)) {
		cpMap = this.globalCodelistProperties;
	    } else {
		if (this.codelistPropertiesByTopLevelClass.containsKey(topLevelClass)) {
		    cpMap = this.codelistPropertiesByTopLevelClass.get(topLevelClass);
		} else {
		    cpMap = new TreeMap<>();
		    this.codelistPropertiesByTopLevelClass.put(topLevelClass, cpMap);
		}
	    }

	    cpMap.put(propertyPath, pi.typeInfo().name);
	}
	
	if(LdpInfo.isObjectTypeSuffixedProperty(pi)) {
	    
	    SortedSet<String> objectTypeSuffixedProperties = objectTypeSuffixedPropertiesByTopLevelClass.get(topLevelClass);
	    if (objectTypeSuffixedProperties == null) {
		objectTypeSuffixedProperties = new TreeSet<>();
		objectTypeSuffixedPropertiesByTopLevelClass.put(topLevelClass, objectTypeSuffixedProperties);
	    }
	    objectTypeSuffixedProperties.add(propertyPath);
	}

	if (pi.matches(Ldproxy2Constants.RULE_ALL_GEOINFODOK)) {

	    if (Strings.CI.equalsAny(pi.typeInfo().name, "LI_Lineage", "DQ_RelativeInternalPositionalAccuracy",
		    "DQ_AbsoluteExternalPositionalAccuracy")) {
		this.gidEncoder.addBuildingBlockGmlDetails(pi, topLevelClass, propertyPath, this);
	    }
	}
    }

    @Override
    public boolean hasInputForServiceCollection(ClassInfo ci) {

	return super.propertyTransformationsForBuildingBlockOfServiceConfigCollectionsByTopLevelClass.containsKey(ci)
		|| xmlAttributes_Gml_OfServiceConfigCollectionsByTopLevelClass.containsKey(ci)
		|| (gmlIdentifyCodelistProperties && codelistPropertiesByTopLevelClass.containsKey(ci))
		|| valueWrapsByTopLevelClass.containsKey(ci);
    }

    public void setGidEncoder(LdpGidEncoder gidEncoder) {
	this.gidEncoder = gidEncoder;
    }

    public void gmlValueWrap(ClassInfo topLevelClass, Map<String, List<String>> valueWrapsIn) {

	Map<String, List<String>> valueWraps;

	if (this.valueWrapsByTopLevelClass.containsKey(topLevelClass)) {
	    valueWraps = this.valueWrapsByTopLevelClass.get(topLevelClass);
	} else {
	    valueWraps = new TreeMap<>();
	    this.valueWrapsByTopLevelClass.put(topLevelClass, valueWraps);
	}

	if (!valueWrapsIn.isEmpty()) {
	    valueWraps.putAll(valueWrapsIn);
	}
    }
}
