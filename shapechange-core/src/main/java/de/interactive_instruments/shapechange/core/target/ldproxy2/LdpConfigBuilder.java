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
package de.interactive_instruments.shapechange.core.target.ldproxy2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import de.ii.ldproxy.cfg.LdproxyCfgWriter;
import de.ii.ogcapi.codelists.domain.ImmutableCodelistsConfiguration;
import de.ii.ogcapi.collections.queryables.domain.ImmutableQueryablesConfiguration;
import de.ii.ogcapi.crs.domain.ImmutableCrsConfiguration;
import de.ii.ogcapi.features.geojson.domain.ImmutableGeoJsonConfiguration;
import de.ii.ogcapi.features.gml.domain.ImmutableGmlConfiguration;
import de.ii.ogcapi.features.html.domain.ImmutableFeaturesHtmlConfiguration;
import de.ii.ogcapi.features.jsonfg.domain.ImmutableJsonFgConfiguration;
import de.ii.ogcapi.filter.domain.ImmutableFilterConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.ImmutableFeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.ImmutableOgcApiDataV2;
import de.ii.ogcapi.resources.domain.ImmutableResourcesConfiguration;
import de.ii.xtraplatform.codelists.domain.Codelist.ImportType;
import de.ii.xtraplatform.codelists.domain.ImmutableCodelist;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.ImmutableEpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutablePartialObjectSchema;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.sql.domain.ConnectionInfoSql.Dialect;
import de.ii.xtraplatform.features.sql.domain.ImmutableConnectionInfoSql;
import de.ii.xtraplatform.features.sql.domain.ImmutableFeatureProviderSqlData;
import de.ii.xtraplatform.features.sql.domain.ImmutableQueryGeneratorSettings;
import de.ii.xtraplatform.features.sql.domain.ImmutableSqlPathDefaults;
import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.ShapeChangeResult.MessageContext;
import de.interactive_instruments.shapechange.core.model.ClassInfo;
import de.interactive_instruments.shapechange.core.model.PropertyInfo;
import de.interactive_instruments.shapechange.core.target.ldproxy2.provider.LdpProvider;
import de.interactive_instruments.shapechange.core.target.ldproxy2.provider.LdpSourcePathProvider;
import de.interactive_instruments.shapechange.core.target.ldproxy2.service.LdpBuildingBlockFeaturesGeoJsonBuilder;
import de.interactive_instruments.shapechange.core.target.ldproxy2.service.LdpBuildingBlockFeaturesGmlBuilder;
import de.interactive_instruments.shapechange.core.target.ldproxy2.service.LdpBuildingBlockFeaturesHtmlBuilder;
import de.interactive_instruments.shapechange.core.target.ldproxy2.service.LdpBuildingBlockFeaturesJsonFgBuilder;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class LdpConfigBuilder {

    protected ShapeChangeResult result;
    protected Ldproxy2Target target;

    protected List<ClassInfo> objectFeatureMixinAndDataTypes;
    protected SortedSet<ClassInfo> codelistsAndEnumerations;

    protected LdproxyCfgWriter cfg;
    protected ImmutableOgcApiDataV2 serviceConfig = null;
    protected ImmutableFeatureProviderSqlData providerConfig = null;
    protected SortedMap<String, ImmutableCodelist> codelistById = new TreeMap<>();

    protected LdpBuildingBlockFeaturesGmlBuilder bbFeaturesGmlBuilder;
    protected LdpBuildingBlockFeaturesHtmlBuilder bbFeaturesHtmlBuilder;
    protected LdpBuildingBlockFeaturesGeoJsonBuilder bbFeaturesGeoJsonBuilder;
    protected LdpBuildingBlockFeaturesJsonFgBuilder bbFeaturesJsonFgBuilder;

    protected LdpPropertyEncoder propertyEncoder;

    protected LdpProvider ldpProvider;
    protected LdpSourcePathProvider ldpSourcePathProvider;

    protected SortedMap<ClassInfo, SortedSet<String>> queryablePropertiesByCollectionCi = new TreeMap<>();

    protected SortedMap<String, ImmutableFeatureSchema> additionalFragments = new TreeMap<>();

    public LdpConfigBuilder(Ldproxy2Target target, LdproxyCfgWriter cfg,
	    LdpBuildingBlockFeaturesGmlBuilder buldingBlockFeaturesGmlBuilder,
	    LdpBuildingBlockFeaturesGeoJsonBuilder buldingBlockGeoJsonBuilder,
	    LdpBuildingBlockFeaturesJsonFgBuilder buldingBlockJsonFgBuilder,
	    List<ClassInfo> objectFeatureMixinAndDataTypes, SortedSet<ClassInfo> codelistsAndEnumerations,
	    LdpProvider ldpProvider, LdpSourcePathProvider ldpSourcePathProvider) {

	this.target = target;
	this.result = target.result;

	this.cfg = cfg;
	this.bbFeaturesGmlBuilder = buldingBlockFeaturesGmlBuilder;
	this.bbFeaturesHtmlBuilder = new LdpBuildingBlockFeaturesHtmlBuilder();
	this.bbFeaturesGeoJsonBuilder = buldingBlockGeoJsonBuilder;
	this.bbFeaturesJsonFgBuilder = buldingBlockJsonFgBuilder;

	this.ldpProvider = ldpProvider;
	this.ldpSourcePathProvider = ldpSourcePathProvider;

	this.propertyEncoder = new LdpPropertyEncoder(this, this.target, this.bbFeaturesGmlBuilder,
		this.bbFeaturesHtmlBuilder, this.bbFeaturesGeoJsonBuilder, this.bbFeaturesJsonFgBuilder,
		this.ldpProvider, this.ldpSourcePathProvider, this.queryablePropertiesByCollectionCi);

	this.objectFeatureMixinAndDataTypes = objectFeatureMixinAndDataTypes;
	this.codelistsAndEnumerations = codelistsAndEnumerations;

    }

    public void process() {

	/*
	 * ===================================
	 * 
	 * BUILD CODELIST, as well as TYPE and FRAGMENT DEFINITIONS
	 * 
	 * ===================================
	 */

	for (ClassInfo ci : codelistsAndEnumerations) {
	    createCodelist(ci);
	}

	SortedMap<String, FeatureSchema> providerFragmentDefinitions = new TreeMap<>();

	if (Ldproxy2Target.enableFragments) {

	    /*
	     * CREATE FRAGMENTS
	     */

	    for (ClassInfo ci : objectFeatureMixinAndDataTypes) {

		String fragmentName = LdpInfo.configIdentifierName(ci);

		ImmutableFeatureSchema.Builder fragmentBuilder = new ImmutableFeatureSchema.Builder().type(Type.OBJECT)
			.name(fragmentName).objectType(LdpInfo.originalClassName(ci))
			.description(LdpInfo.description(ci));

		if (ci.category() != Options.DATATYPE && ci.category() != Options.UNION) {
		    fragmentBuilder.label(LdpInfo.label(ci));
		}

		LdpPropertyEncodingContext pec = ldpProvider.createInitialPropertyEncodingContext(ci, false);

		LinkedHashMap<String, FeatureSchema> ciPropertyDefs = propertyEncoder.propertyDefinitions(ci,
			new ArrayList<PropertyInfo>(), pec);

		/*
		 * Determine the supertypes of ci, in correct order, only keeping those that are
		 * encoded.
		 */
		List<ClassInfo> supertypes = LdpInfo.directSupertypesInOrderOfXsdEncoding(ci).stream()
			.filter(st -> LdpInfo.isEncoded(st)).collect(Collectors.toList());

		if (supertypes.isEmpty()) {

		    fragmentBuilder.propertyMap(ciPropertyDefs);

		} else if (supertypes.size() == 1) {

		    /*
		     * since we only have a single supertype schema, just reference that
		     */
		    fragmentBuilder.schema(LdpUtil.fragmentRef(supertypes.get(0)));

		    if (!ciPropertyDefs.isEmpty()) {
			fragmentBuilder.propertyMap(ciPropertyDefs);
		    }

		} else {

		    ArrayList<ImmutablePartialObjectSchema> partialObjectSchemas = new ArrayList<>();

		    for (ClassInfo cix : supertypes) {
			ImmutablePartialObjectSchema cixPartialObjectSchema = new ImmutablePartialObjectSchema.Builder()
				.schema(LdpUtil.fragmentRef(cix)).build();
			partialObjectSchemas.add(cixPartialObjectSchema);
		    }

		    // add the ci properties in the right place as well
		    ImmutablePartialObjectSchema ciPartialObjectSchema = new ImmutablePartialObjectSchema.Builder()
			    .propertyMap(ciPropertyDefs).build();
		    if (ci.category() == Options.MIXIN) {
			/*
			 * Add the properties of ci mixin first (because that is what is done by the
			 * XmlSchema target, when encoding a mixin that has supertypes.
			 */
			partialObjectSchemas.add(0, ciPartialObjectSchema);
		    } else {
			/*
			 * If ci is not a mixin, add the ci properties at the end.
			 */
			partialObjectSchemas.add(ciPartialObjectSchema);
		    }

		    fragmentBuilder.merge(partialObjectSchemas);
		}

		providerFragmentDefinitions.put(fragmentName, fragmentBuilder.build());

		if (bbFeaturesGmlBuilder != null) {
		    // note: we also track namespaces for mixins
		    bbFeaturesGmlBuilder.register(ci);
		}
	    }

	    /*
	     * In special cases, additional fragment definitions need to be created.
	     */
	    for (Entry<String, ImmutableFeatureSchema> e : this.additionalFragments.entrySet()) {
		providerFragmentDefinitions.put(e.getKey(), e.getValue());
	    }
	}

	/*
	 * CREATE TYPE DEFINITIONS
	 */

	SortedMap<String, FeatureSchema> providerTypeDefinitions = new TreeMap<>();
	SortedMap<String, FeatureTypeConfigurationOgcApi> serviceCollectionDefinitions = new TreeMap<>();

	for (ClassInfo ci : objectFeatureMixinAndDataTypes) {

	    if (ci.category() == Options.MIXIN || Ldproxy2Target.isDatatypeOrUnionEncodedLikeDatatype(ci)
		    || ci.isAbstract()) {
		continue;
	    }

	    String typeDefName = LdpInfo.configIdentifierName(ci);

	    /*
	     * Create provider config entry
	     */

	    ImmutableFeatureSchema.Builder typeDefBuilder = new ImmutableFeatureSchema.Builder().type(Type.OBJECT)
		    .name(typeDefName).sourcePath(ldpSourcePathProvider.sourcePathTypeLevel(ci))
		    .description(LdpInfo.description(ci));

	    if (ci.category() != Options.DATATYPE && ci.category() != Options.UNION) {
		typeDefBuilder.label(LdpInfo.label(ci));
	    }

	    LdpPropertyEncodingContext pec = ldpProvider.createInitialPropertyEncodingContext(ci, true);

	    if (Ldproxy2Target.enableFragments) {

//		if (ci.supertypes().isEmpty()) {

		/*
		 * The fragment for ci has merge statements, thus aggregating all relevant
		 * schema fragments for direct and indirect supertypes. When encoding properties
		 * in the type definition, all properties from direct and indirect supertypes
		 * must be considered and encoded.
		 */
		LinkedHashMap<String, FeatureSchema> ciPropertyDefs = propertyEncoder.propertyDefinitions(ci,
			new ArrayList<PropertyInfo>(), pec);

		typeDefBuilder.schema(LdpUtil.fragmentRef(ci)).propertyMap(ciPropertyDefs);

//		} else {
//
//		    List<ClassInfo> cis = LdpInfo.directSupertypesInOrderOfXsdEncoding(ci);
//		    cis.add(ci);
//
//		    List<ImmutablePartialObjectSchema> partialObjectSchemas = new ArrayList<>();
//
//		    for (ClassInfo cix : cis) {
//
//			ImmutablePartialObjectSchema.Builder cixPartialObjectSchemaBuilder = new ImmutablePartialObjectSchema.Builder()
//				.schema(fragmentRef(cix));
//			LinkedHashMap<String, FeatureSchema> cixPropertyDefs = propertyEncoder.propertyDefinitions(cix,
//				new ArrayList<PropertyInfo>(), pec);
//			cixPartialObjectSchemaBuilder.propertyMap(cixPropertyDefs);
//			partialObjectSchemas.add(cixPartialObjectSchemaBuilder.build());
//		    }
//
//		    typeDefBuilder.merge(partialObjectSchemas);
//		}

	    } else {

		LinkedHashMap<String, FeatureSchema> propertyDefs = propertyEncoder.propertyDefinitions(ci,
			new ArrayList<PropertyInfo>(), pec);

		typeDefBuilder.objectType(LdpInfo.originalClassName(ci)).propertyMap(propertyDefs);
	    }

	    propertyEncoder.propertyDefinitionsForServiceConfiguration(ci, new ArrayList<PropertyInfo>(), pec);

	    if (bbFeaturesGmlBuilder != null) {
		bbFeaturesGmlBuilder.register(ci);
	    }

	    ImmutableFeatureSchema typeDef = typeDefBuilder.build();
	    providerTypeDefinitions.put(typeDefName, typeDef);

	    /*
	     * Create service config entry (must be done after provider config entry
	     * creation, in order for relevant transformation infos to be available for
	     * inclusion in the service config)
	     */

	    List<ExtensionConfiguration> extensionConfigurations = new ArrayList<>();

	    if (ci.matches(Ldproxy2Constants.RULE_ALL_QUERYABLES)) {

		SortedSet<String> queryables = new TreeSet<>(Ldproxy2Target.queryablesFromConfig);

		SortedSet<String> queryableProperties = new TreeSet<>();

		// add properties identified via tagged values
		if (queryablePropertiesByCollectionCi.containsKey(ci)) {
		    queryableProperties.addAll(queryablePropertiesByCollectionCi.get(ci));
		}

		for (PropertyInfo pi : ci.propertiesAll()) {
		    /*
		     * This would also be the place to check if the property itself has a tagged
		     * value that marks the property as queryable
		     */
		    if (queryables.contains(pi.name()) || queryables.contains(LdpInfo.originalPropertyName(pi))) {
			String queryableId = LdpUtil.queryableId(pi);
			queryableProperties.add(queryableId);
		    }
		}

		if (!queryableProperties.isEmpty()) {

		    ImmutableQueryablesConfiguration.Builder qBuilder = cfg.builder().ogcApiExtension().queryables();
		    qBuilder.included(queryableProperties);
		    extensionConfigurations.add(qBuilder.build());
		}
	    }

	    if (bbFeaturesHtmlBuilder.hasInputForServiceCollection(ci)) {
		ImmutableFeaturesHtmlConfiguration featuresHtmlConfig = bbFeaturesHtmlBuilder
			.createConfigurationForServiceCollection(cfg, ci);
		extensionConfigurations.add(featuresHtmlConfig);
	    }

	    if (bbFeaturesGmlBuilder != null && bbFeaturesGmlBuilder.hasInputForServiceCollection(ci)) {
		ImmutableGmlConfiguration gmlConfig = bbFeaturesGmlBuilder
			.createGmlConfigurationForServiceCollection(cfg, ci);
		extensionConfigurations.add(gmlConfig);
	    }

	    if (bbFeaturesGeoJsonBuilder != null && bbFeaturesGeoJsonBuilder.hasInputForServiceCollection(ci)) {
		ImmutableGeoJsonConfiguration geoJsonConfig = bbFeaturesGeoJsonBuilder
			.createConfigurationForServiceCollection(cfg, ci);
		extensionConfigurations.add(geoJsonConfig);
	    }

	    if (bbFeaturesJsonFgBuilder != null && bbFeaturesJsonFgBuilder.hasInputForServiceCollection(ci)) {
		ImmutableJsonFgConfiguration jsonFgConfig = bbFeaturesJsonFgBuilder
			.createConfigurationForServiceCollection(cfg, ci);
		extensionConfigurations.add(jsonFgConfig);
	    }

	    ImmutableFeatureTypeConfigurationOgcApi serviceCollDef = new ImmutableFeatureTypeConfigurationOgcApi.Builder()
		    .id(typeDefName).label(typeDefName).addAllExtensions(extensionConfigurations).build();

	    serviceCollectionDefinitions.put(typeDefName, serviceCollDef);
	}

	/*
	 * ================================
	 * 
	 * CREATE COMMON GML CONFIGURATION
	 * 
	 * ================================
	 */

	ImmutableGmlConfiguration.Builder gmlBuilder = null;

	ImmutableResourcesConfiguration.Builder resourcesBuilder = null;

	if (bbFeaturesGmlBuilder != null) {
	    bbFeaturesGmlBuilder.createServiceConfigurationBuilders(cfg);
	    gmlBuilder = bbFeaturesGmlBuilder.getServiceConfigGmlConfigurationBuilder();
	    resourcesBuilder = bbFeaturesGmlBuilder.getServiceConfigResourcesConfigurationBuilder();
	}

	ImmutableQueryablesConfiguration.Builder queryablesBuilder = null;
	if (Ldproxy2Target.mainAppSchema.matches(Ldproxy2Constants.RULE_ALL_QUERYABLES)) {
	    queryablesBuilder = cfg.builder().ogcApiExtension().queryables();
	    queryablesBuilder.enabled(true);
	}

	ImmutableCodelistsConfiguration.Builder codelistsBuilder = null;
	if (Ldproxy2Target.enableCodelists) {
	    codelistsBuilder = cfg.builder().ogcApiExtension().codelists();
	    codelistsBuilder.enabled(true);
	}

	ImmutableCrsConfiguration.Builder crsBuilder = null;
	if (Ldproxy2Target.enableCrs) {
	    crsBuilder = cfg.builder().ogcApiExtension().crs();
	    crsBuilder.enabled(true);

	    Ldproxy2Target.additionalCrs.sort((EpsgCrs e1, EpsgCrs e2) -> Integer.compare(e1.getCode(), e2.getCode()));

	    crsBuilder.addAllAdditionalCrs(Ldproxy2Target.additionalCrs);
	}

	ImmutableGeoJsonConfiguration.Builder geoJsonBuilder = null;
	if (Ldproxy2Target.enableFeaturesGeoJson) {
	    geoJsonBuilder = cfg.builder().ogcApiExtension().geoJson();
	    geoJsonBuilder.enabled(true);
	}

	ImmutableJsonFgConfiguration.Builder jsonFgBuilder = null;
	if (Ldproxy2Target.enableFeaturesJsonFg) {
	    jsonFgBuilder = cfg.builder().ogcApiExtension().jsonFg();
	    jsonFgBuilder.enabled(true);

	    if (Ldproxy2Target.jsonFgCoordRefSys != null) {
		jsonFgBuilder.coordRefSys(Ldproxy2Target.jsonFgCoordRefSys);
	    }
	    if (!Ldproxy2Target.jsonFgFeatureType.isEmpty()) {
		jsonFgBuilder.featureType(Ldproxy2Target.jsonFgFeatureType);
	    }
	    if (!Ldproxy2Target.jsonFgIncludeInGeoJson.isEmpty()) {
		jsonFgBuilder.includeInGeoJson(Ldproxy2Target.jsonFgIncludeInGeoJson);
	    }
	}

	ImmutableFilterConfiguration.Builder filterBuilder = null;
	if (Ldproxy2Target.enableFilter) {
	    filterBuilder = cfg.builder().ogcApiExtension().filter();
	    filterBuilder.enabled(true);
	}

	/*
	 * ================================
	 * 
	 * BUILD THE SERVICE CONFIGURATION
	 * 
	 * ================================
	 */

	List<ExtensionConfiguration> generalExtensionConfigurations = new ArrayList<>();
	if (resourcesBuilder != null) {
	    generalExtensionConfigurations.add(resourcesBuilder.build());
	}
	if (gmlBuilder != null) {
	    generalExtensionConfigurations.add(gmlBuilder.build());
	}
	if (queryablesBuilder != null) {
	    generalExtensionConfigurations.add(queryablesBuilder.build());
	}
	if (codelistsBuilder != null) {
	    generalExtensionConfigurations.add(codelistsBuilder.build());
	}
	if (crsBuilder != null) {
	    generalExtensionConfigurations.add(crsBuilder.build());
	}
	if (geoJsonBuilder != null) {
	    generalExtensionConfigurations.add(geoJsonBuilder.build());
	}
	if (jsonFgBuilder != null) {
	    generalExtensionConfigurations.add(jsonFgBuilder.build());
	}
	if (filterBuilder != null) {
	    generalExtensionConfigurations.add(filterBuilder.build());
	}

	serviceConfig = cfg.builder().entity().api().id(Ldproxy2Target.mainId).entityStorageVersion(2)
		.label(Ldproxy2Target.serviceLabel).description(Ldproxy2Target.serviceDescription)
		.serviceType("OGC_API").addAllExtensions(generalExtensionConfigurations)
		.collections(serviceCollectionDefinitions).build();

	/*
	 * =================================
	 * 
	 * BUILD THE PROVIDER CONFIGURATION
	 * 
	 * =================================
	 */

	ImmutableConnectionInfoSql connectionInfo = cfg.builder().entity().provider().connectionInfoBuilder()
		.dialect(Dialect.PGIS.name()).database("FIXME").host("FIXME").user("FIXME")
		.password("FIXME-base64-encoded").schemas(Ldproxy2Target.dbSchemaNames).build();

	ImmutableSqlPathDefaults sourcePathDefaults = cfg.builder().entity().provider().sourcePathDefaultsBuilder()
		.primaryKey(ldpSourcePathProvider.defaultPrimaryKey()).sortKey(ldpSourcePathProvider.defaultSortKey())
		.build();

	ImmutableQueryGeneratorSettings queryGeneration = cfg.builder().entity().provider().queryGenerationBuilder()
		.computeNumberMatched(true).build();

	ImmutableEpsgCrs nativeCrs = cfg.builder().entity().provider().nativeCrsBuilder().code(Ldproxy2Target.srid)
		.forceAxisOrder(Ldproxy2Target.forceAxisOrder).build();

	ImmutableFeatureProviderSqlData.Builder providerConfigBuilder = cfg.builder().entity().provider()
		.id(Ldproxy2Target.mainId).connectionInfo(connectionInfo).sourcePathDefaults(sourcePathDefaults)
		.queryGeneration(queryGeneration).nativeCrs(nativeCrs).types(providerTypeDefinitions)
		.fragments(providerFragmentDefinitions);

	if (Ldproxy2Target.nativeTimeZone != null) {
	    providerConfigBuilder.nativeTimeZone(Ldproxy2Target.nativeTimeZone);
	}

	if (StringUtils.isNotBlank(Ldproxy2Target.providerConfigLabelTemplate)) {
	    providerConfigBuilder.labelTemplate(Ldproxy2Target.providerConfigLabelTemplate);
	}

	providerConfig = providerConfigBuilder.build();
    }

    public ImmutableFeatureProviderSqlData getProviderConfig() {
	return this.providerConfig;
    }

    public ImmutableOgcApiDataV2 getServiceConfig() {
	return this.serviceConfig;
    }

    public SortedMap<String, ImmutableCodelist> getCodeListMap() {
	return this.codelistById;
    }

    private void createCodelist(ClassInfo ci) {

	String id = LdpInfo.codelistId(ci);

	Optional<String> labelOpt = LdpInfo.label(ci);
	String label;
	if (labelOpt.isEmpty()) {
	    MessageContext mc = result.addInfo(target, 102, ci.name());
	    if (mc != null) {
		mc.addDetail(target, 1, ci.fullNameInSchema());
	    }
	    label = ci.name();
	} else {
	    label = labelOpt.get();
	}

	ImmutableCodelist ic = cfg.builder().value().codelist().label(label).sourceType(ImportType.TEMPLATES)
		.description(LdpInfo.description(ci)).build();

	if (!ci.properties().isEmpty()) {

	    SortedMap<String, String> entries = new TreeMap<>();

	    for (PropertyInfo pi : ci.properties().values()) {

		String code = null;
		String targetValue = null;

		if (ci.matches(Ldproxy2Constants.RULE_CLS_CODELIST_DIRECT)) {

		    targetValue = pi.name();

		    if (StringUtils.isBlank(pi.initialValue())) {
			MessageContext mc = result.addWarning(target, 100, ci.name(), pi.name());
			if (mc != null) {
			    mc.addDetail(target, 1, pi.fullNameInSchema());
			}
			code = pi.name();
		    } else {
			code = pi.initialValue();
		    }
		} else if (ci.matches(Ldproxy2Constants.RULE_CLS_CODELIST_TARGETBYTV)) {

		    if (StringUtils.isBlank(pi.initialValue())) {
			code = pi.name();
		    } else {
			code = pi.initialValue();
		    }

		    if (StringUtils.isBlank(pi.taggedValue(Ldproxy2Target.codeTargetTagName))) {
			MessageContext mc = result.addWarning(target, 101, ci.name(), pi.name(),
				Ldproxy2Target.codeTargetTagName);
			if (mc != null) {
			    mc.addDetail(target, 1, pi.fullNameInSchema());
			}
			targetValue = pi.name();
		    } else {
			targetValue = pi.taggedValue(Ldproxy2Target.codeTargetTagName).trim();
		    }
		} else {
		    code = pi.name();
		    targetValue = pi.name();
		}

		if (ci.matches(Ldproxy2Constants.RULE_CLS_CODELIST_APPEND_CODE)) {
		    targetValue = targetValue + " (" + code + ")";
		}

		entries.put(code, targetValue);
	    }

	    ic = ic.withEntries(entries);
	}

	if (StringUtils.isNotBlank(ci.taggedValue("ldpFallbackValue"))) {
	    ic = ic.withFallback(ci.taggedValue("ldpFallbackValue").trim());
	}

	this.codelistById.put(id, ic);
    }

    public void createAdditionalFragment(String fragmentName, ImmutableFeatureSchema fragment) {
	this.additionalFragments.put(fragmentName, fragment);
    }

}
