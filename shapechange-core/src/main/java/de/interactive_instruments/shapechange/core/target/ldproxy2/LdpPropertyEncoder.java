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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema.Builder;
import de.ii.xtraplatform.features.domain.ImmutableSchemaConstraints;
import de.ii.xtraplatform.features.domain.SchemaBase.Role;
import de.ii.xtraplatform.features.domain.SchemaBase.Scope;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import de.interactive_instruments.shapechange.core.target.ldproxy2.provider.LdpProvider;
import de.interactive_instruments.shapechange.core.target.ldproxy2.provider.LdpSourcePathProvider;
import de.interactive_instruments.shapechange.core.target.ldproxy2.service.LdpBuildingBlockFeaturesGeoJsonBuilder;
import de.interactive_instruments.shapechange.core.target.ldproxy2.service.LdpBuildingBlockFeaturesGmlBuilder;
import de.interactive_instruments.shapechange.core.target.ldproxy2.service.LdpBuildingBlockFeaturesHtmlBuilder;
import de.interactive_instruments.shapechange.core.target.ldproxy2.service.LdpBuildingBlockFeaturesJsonFgBuilder;
import de.interactive_instruments.shapechange.core.util.GenericValueTypeUtil;
import de.interactive_instruments.shapechange.core.MessageSource;
import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ProcessMapEntry;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.ShapeChangeResult.MessageContext;
import de.interactive_instruments.shapechange.core.model.ClassInfo;
import de.interactive_instruments.shapechange.core.model.PropertyInfo;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class LdpPropertyEncoder {

    protected ShapeChangeResult result;
    protected Ldproxy2Target target;
    protected MessageSource msgSource;

    protected LdpBuildingBlockFeaturesGmlBuilder bbFeaturesGmlBuilder;
    protected LdpBuildingBlockFeaturesHtmlBuilder bbFeaturesHtmlBuilder;
    protected LdpBuildingBlockFeaturesGeoJsonBuilder bbFeaturesGeoJsonBuilder;
    protected LdpBuildingBlockFeaturesJsonFgBuilder bbFeaturesJsonFgBuilder;

    protected LdpProvider ldpProvider;
    protected LdpSourcePathProvider sourcePathProvider;

    public LdpPropertyEncoder(Ldproxy2Target target, LdpBuildingBlockFeaturesGmlBuilder gml,
	    LdpBuildingBlockFeaturesHtmlBuilder featuresHtml,
	    LdpBuildingBlockFeaturesGeoJsonBuilder featuresGeoJsonBuilder,
	    LdpBuildingBlockFeaturesJsonFgBuilder featuresJsonFgBuilder, LdpProvider ldpProvider,
	    LdpSourcePathProvider sourcePathProvider) {

	this.target = target;
	this.result = target.result;
	this.msgSource = target;

	this.bbFeaturesGmlBuilder = gml;
	this.bbFeaturesHtmlBuilder = featuresHtml;
	this.bbFeaturesGeoJsonBuilder = featuresGeoJsonBuilder;
	this.bbFeaturesJsonFgBuilder = featuresJsonFgBuilder;

	this.ldpProvider = ldpProvider;
	this.sourcePathProvider = sourcePathProvider;
    }

    /**
     * NOTE: The method calls itself recursively.
     * 
     * @param currentCi            - tbd
     * @param alreadyVisitedPiList - tbd
     * @param context              provides additional information about the context
     *                             in which property definitions shall be encoded
     * @return the schema definition
     */
    public LinkedHashMap<String, FeatureSchema> propertyDefinitions(ClassInfo currentCi,
	    List<PropertyInfo> alreadyVisitedPiList, LdpPropertyEncodingContext context) {

	/*
	 * NOTE: This method cannot easily be split into cases with fragments enabled
	 * (differentiating between type definition and fragment encoding, maybe also
	 * context type being a data type), and fragments disabled. The reason is that
	 * without fragments enabled, full type definitions must be encoded, with
	 * content that is otherwise encoded in fragments. If we were to split the
	 * method into one for the case of fragments enabled, and one without, we would
	 * have to duplicate a lot of code. It seems more efficient to differentiate the
	 * relevant cases clearly within this method itself.
	 */

	LinkedHashMap<String, FeatureSchema> propertyDefs = new LinkedHashMap<>();

	/*
	 * DETERMINE SPECIAL PROPERTIES - ONLY RELEVANT FOR TOP-LEVEL CLASS
	 */

	PropertyInfo identifierPi = null;
	boolean multipleIdentifierPisEncountered = false;
	PropertyInfo defaultGeometryPi = null;
	boolean multipleDefaultGeometriesEncountered = false;
	PropertyInfo defaultInstantPi = null;
	boolean multipleDefaultInstantsEncountered = false;
	PropertyInfo defaultIntervalStartPi = null;
	boolean multipleDefaultIntervalStartsEncountered = false;
	PropertyInfo defaultIntervalEndPi = null;
	boolean multipleDefaultIntervalEndsEncountered = false;

	if (alreadyVisitedPiList.isEmpty()) {

	    LdpSpecialPropertiesInfo spi = target.specialPropertiesInfo(currentCi);

	    identifierPi = spi.getIdentifierPiOfCi();
	    multipleIdentifierPisEncountered = spi.isMultipleIdentifierPisEncountered();

	    defaultGeometryPi = spi.getDefaultGeometryPiOfCi();
	    multipleDefaultGeometriesEncountered = spi.isMultipleDefaultGeometriesEncountered();

	    defaultInstantPi = spi.getDefaultInstantPiOfCi();
	    multipleDefaultInstantsEncountered = spi.isMultipleDefaultInstantsEncountered();

	    defaultIntervalStartPi = spi.getDefaultIntervalStartPiOfCi();
	    multipleDefaultIntervalStartsEncountered = spi.isMultipleDefaultIntervalStartsEncountered();

	    defaultIntervalEndPi = spi.getDefaultIntervalEndPiOfCi();
	    multipleDefaultIntervalEndsEncountered = spi.isMultipleDefaultIntervalEndsEncountered();

	    /*
	     * Conditional encoding of an additional identifier property
	     */
	    if (spi.isEncodeAdditionalIdentifierProp() && (!Ldproxy2Target.enableFragments || context.isInFragment())) {

		ImmutableFeatureSchema identifierMemberDef = new ImmutableFeatureSchema.Builder()
			.name(Ldproxy2Target.objectIdentifierName)
			.sourcePath(sourcePathProvider.objectIdentifierSourcePath())
			.type(ldpProvider.objectIdentifierType()).role(Role.ID).build();
		propertyDefs.put(identifierMemberDef.getName(), identifierMemberDef);
	    }
	}

	/*
	 * If fragment encoding is enabled and we are in a type definition, encode all
	 * properties (from direct and indirect supertypes, taking into account the
	 * order of the xsd encoding, and property overrides). Otherwise, only encode
	 * the properties that are owned by the class.
	 */
	Collection<PropertyInfo> propsToProcess = (Ldproxy2Target.enableFragments && !context.isInFragment())
		? LdpInfo.allPropertiesInOrderOfXsdEncoding(currentCi)
		: currentCi.properties().values();

	for (PropertyInfo pi : propsToProcess) {

	    if (!LdpInfo.isEncoded(pi) || target.isIgnored(pi)) {
		continue;
	    }

	    ClassInfo typeCi = pi.typeClass();

	    List<PropertyInfo> nowVisitedList = new ArrayList<>(alreadyVisitedPiList);
	    nowVisitedList.add(pi);

	    ImmutableFeatureSchema.Builder propMemberDefBuilder = new ImmutableFeatureSchema.Builder();

	    Type ldpType = target.ldproxyType(pi);

	    Type typeForBuilder = typeForBuilder(pi, identifierPi, pi.cardinality().maxOccurs == 1, ldpType);
	    Optional<Type> valueTypeForBuilder = valueTypeForBuilder(pi, identifierPi, pi.cardinality().maxOccurs == 1,
		    ldpType);

	    Optional<SimpleFeatureGeometry> geometryTypeForBuilder = Optional.empty();
	    Optional<Boolean> linearizeCurvesOpt = Optional.empty();
	    if (ldpType == Type.GEOMETRY) {
		geometryTypeForBuilder = geometryType(pi);
		if (Ldproxy2Target.linearizeCurves && geometryTypeForBuilder.isPresent()) {
		    SimpleFeatureGeometry sfg = geometryTypeForBuilder.get();
		    if (sfg != SimpleFeatureGeometry.POINT && sfg != SimpleFeatureGeometry.MULTI_POINT
			    && sfg != SimpleFeatureGeometry.NONE) {
			linearizeCurvesOpt = Optional.of(true);
		    }
		}
	    }

	    Optional<String> unitForBuilder = Optional.empty();
	    if ((ldpType == Type.FLOAT || ldpType == Type.INTEGER) && StringUtils.isNotBlank(Ldproxy2Target.uomTvName)
		    && StringUtils.isNotBlank(pi.taggedValue(Ldproxy2Target.uomTvName))) {
		unitForBuilder = Optional.of(pi.taggedValue(Ldproxy2Target.uomTvName).trim());
	    }

	    Optional<Role> propRoleForBuilder;

	    if (identifierPi != null && !multipleIdentifierPisEncountered && pi == identifierPi) {
		propRoleForBuilder = Optional.of(Role.ID);
	    } else if (defaultGeometryPi != null && !multipleDefaultGeometriesEncountered && pi == defaultGeometryPi) {
		propRoleForBuilder = Optional.of(Role.PRIMARY_GEOMETRY);
	    } else if (defaultInstantPi != null && !multipleDefaultInstantsEncountered && pi == defaultInstantPi) {
		propRoleForBuilder = Optional.of(Role.PRIMARY_INSTANT);
	    } else if (defaultIntervalStartPi != null && !multipleDefaultIntervalStartsEncountered
		    && pi == defaultIntervalStartPi) {
		propRoleForBuilder = Optional.of(Role.PRIMARY_INTERVAL_START);
	    } else if (defaultIntervalEndPi != null && !multipleDefaultIntervalEndsEncountered
		    && pi == defaultIntervalEndPi) {
		propRoleForBuilder = Optional.of(Role.PRIMARY_INTERVAL_END);
	    } else {
		propRoleForBuilder = Optional.empty();
	    }

	    boolean ignoreSourcePathOnPropertyLevel = false;

	    Optional<String> objectTypeForBuilder = Optional.empty();
//	    Optional<String> refTypeForBuilder = Optional.empty();
//	    Optional<String> refUriTemplateForBuilder = Optional.empty();

	    LinkedHashMap<String, FeatureSchema> propertyMapForBuilder = new LinkedHashMap<>();

	    // handle embedded cases (datatype or link properties)
	    if (target.isMappedToOrImplementedAsLink(pi)) {

		if (pi.matches(Ldproxy2Constants.RULE_ALL_LINK_OBJECT_AS_FEATURE_REF)) {

//		    ClassInfo typeCi = pi.typeClass();

//		    if (!target.valueTypeIsMapped(pi) && typeCi != null && target.isProcessedType(typeCi)) {
//			// the value type must be a type with identity
//			refTypeForBuilder = Optional.of(LdpUtil.formatCollectionId(pi.typeInfo().name));
//		    } else {
//			// the value type is mapped
//			refUriTemplateForBuilder = Optional.of(sqlSourcePathProvider.urlTemplateForValueType(pi));
//		    }

//		    objectTypeForBuilder = typeCi != null ? Optional.of(LdpInfo.originalClassName(typeCi)) : Optional.of(pi.typeInfo().name);

		} else {

		    // IMPORTANT: The object type must be 'Link', NOT 'LINK'!
		    objectTypeForBuilder = Optional.of("Link");

		    ignoreSourcePathOnPropertyLevel = omitSourcePathOnPropertyLevelForLinkObjectProperty(pi);

		    LinkedHashMap<String, FeatureSchema> linkPropertyDefs = new LinkedHashMap<>();

		    ImmutableFeatureSchema titleSubProp = createTitlePropertyForLinkOrFeatureRef(pi);
		    linkPropertyDefs.put("title", titleSubProp);

		    ImmutableFeatureSchema linkPropHref = createHrefPropertyForLink(pi);
		    linkPropertyDefs.put("href", linkPropHref);

		    propertyMapForBuilder = linkPropertyDefs;
		}

	    } else if (!LdpUtil.isLdproxySimpleType(ldpType) && pi.categoryOfValue() == Options.DATATYPE) {

		/*
		 * detect circular dependency in the property path - circular paths are not
		 * supported
		 */
		if (alreadyVisitedPiList.stream().anyMatch(vPi -> vPi.inClass() == typeCi) || typeCi == currentCi) {

		    ClassInfo topType = alreadyVisitedPiList.get(0).inClass();

		    MessageContext mc = result.addError(msgSource, 117, topType.name(), propertyPath(nowVisitedList));
		    if (mc != null) {
			mc.addDetail(msgSource, 0, topType.fullNameInSchema());
		    }

		    continue;

		} else {

		    if (Ldproxy2Target.enableFragments && context.isInFragment()) {

			if (!ldpProvider.isDatatypeWithSubtypesEncodedInFragmentWithSingularSchemaAndObjectType()
				&& typeCi.subtypesInCompleteHierarchy().size() > 0) {

			    /*
			     * typeCi has subclasses; thus, we cannot identify one particular objectType.
			     * Furthermore, we cannot reference a particular schema.
			     */

			} else {

			    objectTypeForBuilder = Optional.of(LdpInfo.originalClassName(typeCi));
			    if (!target.isMappedToLink(typeCi)) {
				propMemberDefBuilder.schema(LdpUtil.fragmentRef(typeCi));
			    }
			}

		    } else {

			/*
			 * Case of datatype valued property within a type definition. We must drill down
			 * recursively.
			 */

			LdpPropertyEncodingContext nextContext = ldpProvider.createChildContext(context, typeCi);

			LinkedHashMap<String, FeatureSchema> datatypePropertyDefs = propertyDefinitions(typeCi,
				nowVisitedList, nextContext);
			propertyMapForBuilder = datatypePropertyDefs;

			objectTypeForBuilder = Optional.of(LdpInfo.originalClassName(typeCi));
		    }

//		    if (bbFeaturesGmlBuilder != null) {
//			bbFeaturesGmlBuilder.register(typeCi);
//		    }
		}
	    }

	    Optional<String> constantValueForBuilder = Optional.empty();

	    /*
	     * Determine source path information for pi. Take into account the current
	     * context. The result may contain multiple source paths.
	     */
	    LdpSourcePathInfos sourcePathInfosForProperty = sourcePathProvider.sourcePathPropertyLevel(pi,
		    alreadyVisitedPiList, context);
	    LdpSourcePathInfos sourcePathInfosForBuilder = null;

	    if (StringUtils.isNotBlank(pi.initialValue()) && pi.isReadOnly()
		    && pi.matches(Ldproxy2Constants.RULE_PROP_READONLY)) {

		constantValueForBuilder = Optional.of(constantValue(pi));

	    } else if (!ignoreSourcePathOnPropertyLevel) {
		/*
		 * Encode the source path only in certain circumstances.
		 */
		if (!Ldproxy2Target.enableFragments) {
		    // no fragment encoding - we are in a type definition
		    sourcePathInfosForBuilder = sourcePathInfosForProperty;
		} else {
		    // fragment encoding enabled
		    if (context.isInFragment() && sourcePathProvider.isEncodedWithDirectValueSourcePath(pi, context)) {
			sourcePathInfosForBuilder = sourcePathInfosForProperty;
		    } else if (!context.isInFragment()
			    && !sourcePathProvider.isEncodedWithDirectValueSourcePath(pi, context)) {
			sourcePathInfosForBuilder = sourcePathInfosForProperty;
		    }
		}
	    }

	    // Generate constraints
	    Optional<ImmutableSchemaConstraints> constraints = Optional.empty();
	    boolean providerConfigConstraintCreated = false;
	    ImmutableSchemaConstraints.Builder constraintsBuilder = new ImmutableSchemaConstraints.Builder();
	    if (pi.cardinality().minOccurs != 0 && !pi.voidable()) {

		providerConfigConstraintCreated = true;
		constraintsBuilder.required(true);
	    }
	    if (pi.cardinality().maxOccurs > 1 && !(pi == identifierPi || pi == defaultGeometryPi)
		    && ldpType != Type.GEOMETRY) {
		if (pi.voidable()) {
		    providerConfigConstraintCreated = true;
		    constraintsBuilder.minOccurrence(0);
		} else {
		    providerConfigConstraintCreated = true;
		    constraintsBuilder.minOccurrence(pi.cardinality().minOccurs);
		}
		if (pi.cardinality().maxOccurs != Integer.MAX_VALUE) {
		    providerConfigConstraintCreated = true;
		    constraintsBuilder.maxOccurrence(pi.cardinality().maxOccurs);
		}
	    }
	    if (LdpInfo.isEnumerationOrCodelistValueType(pi) && !target.valueTypeIsMapped(pi)) {

		providerConfigConstraintCreated = true;
		String codelistId = LdpInfo.codelistId(typeCi);

		if (!Ldproxy2Target.codelistsAndEnumerations.contains(typeCi)) {
		    /*
		     * Handle case of enumeration/codelist that is not encoded, unmapped, or not
		     * from the schemas selected for processing.
		     */
		    MessageContext mc = result.addWarning(msgSource, 127, typeCi.name(), codelistId);
		    if (mc != null) {
			mc.addDetail(msgSource, 0, typeCi.fullNameInSchema());
		    }
		}

		constraintsBuilder.codelist(codelistId);

		if (pi.categoryOfValue() == Options.ENUMERATION
			&& typeCi.matches(Ldproxy2Constants.RULE_CLS_ENUMERATION_ENUM_CONSTRAINT)) {
		    constraintsBuilder.enumValues(LdpInfo.enumValues(typeCi));
		}

//		if (!Ldproxy2Target.enableFragments || context.isInFragment()) {
//		    // Create content for inclusion in service config:
//		    ImmutablePropertyTransformation trf = new ImmutablePropertyTransformation.Builder()
//			    .codelist(codelistId).build();
//		    bbFeaturesHtmlBuilder.addPropertyTransformationToBuildingBlockOfCollectionInServiceConfiguration(
//			    nowVisitedList.get(0).inClass(), propertyPath(nowVisitedList), trf);
//		}
	    }

	    if (providerConfigConstraintCreated) {
		constraints = Optional.of(constraintsBuilder.build());
	    }

	    /*
	     * Fragment encoding and what is encoded within a type definition are different
	     * things. Within fragments - if enabled at all - we encode the structure of the
	     * represented type. The only exception is the sourcePath: if it is direct path
	     * (i.e., a column within the source table of the context type), then the path
	     * can be encoded in the fragment definition; otherwise, it needs to be encoded
	     * within the type definition. If fragments are not enabled, we encode
	     * everything in the type definition, as usual.
	     */

	    if (Ldproxy2Target.enableFragments && !context.isInFragment()) {

		/*
		 * Fragment encoding is enabled and we are in a type definition.
		 */

		if (sourcePathInfosForBuilder != null) {

		    if (sourcePathInfosForBuilder.isMultipleSourcePaths() && sourcePathInfosForBuilder.allWithRefType()
			    && pi.matches(Ldproxy2Constants.RULE_ALL_LINK_OBJECT_AS_FEATURE_REF)
			    && (pi.matches(Ldproxy2Constants.RULE_ALL_CORETABLE)
				    || LdpInfo.valueTypeHasValidLdpTypeAttributeTag(pi))
			    && sourcePathInfosForBuilder.commonValueSourcePath().isPresent()) {

			addDetailsForFeatureRefWithMultipleRefTypesAndCommonValueSourcePath(pi, propMemberDefBuilder,
				sourcePathInfosForBuilder, propertyMapForBuilder, typeCi, nowVisitedList, context);

		    } else {

			/*
			 * The property is not fully defined in the fragment (that information has been
			 * determined before, when setting the value for sourcePathInfosForBuilder).
			 */

			if (sourcePathInfosForBuilder.isSingleSourcePath()) {

			    LdpSourcePathInfo spi = sourcePathInfosForBuilder.getSourcePathInfos().get(0);

			    encodeSourcePathInfosInTypeDefinitionWithFragmentsEnabled(pi, typeCi, alreadyVisitedPiList,
				    nowVisitedList, currentCi, spi, context, propMemberDefBuilder, ldpType,
				    propertyMapForBuilder);

			} else if (sourcePathInfosForBuilder.isMultipleSourcePaths()) {

			    List<ImmutableFeatureSchema> itemSchemas = new ArrayList<>();

			    int index = 0;
			    for (LdpSourcePathInfo spi : sourcePathInfosForBuilder.getSourcePathInfos()) {

				index++;

				ImmutableFeatureSchema.Builder mspBuilder = new ImmutableFeatureSchema.Builder();
				LinkedHashMap<String, FeatureSchema> propertyMapForMspBuilder = new LinkedHashMap<>();

				mspBuilder.name(pi.name() + "_" + index);

				encodeSourcePathInfosInTypeDefinitionWithFragmentsEnabled(pi, typeCi,
					alreadyVisitedPiList, nowVisitedList, currentCi, spi, context, mspBuilder,
					ldpType, propertyMapForMspBuilder);

				// determine the correct type for the item schema

				Type typeForMspBuilder = typeForBuilder(pi, identifierPi, spi.isTargetsSingleValue(),
					ldpType);
				Optional<Type> valueTypeForMspBuilder = valueTypeForBuilder(pi, identifierPi,
					spi.isTargetsSingleValue(), ldpType);

				mspBuilder.type(typeForMspBuilder);

				if (!propertyMapForMspBuilder.isEmpty()) {
				    mspBuilder.propertyMap(propertyMapForMspBuilder);
				}

				/*
				 * NOTE: If the property is encoded as a feature reference, the value type is
				 * already set before (together with refType or refUriTemplate, in
				 * encodeSourcePathInfosInTypeDefinitionWithFragmentsEnabled (which calls
				 * addFeatureRefDetails).
				 */
				if (!isEncodedAsFeatureRef(pi)) {
				    mspBuilder.valueType(valueTypeForMspBuilder);
				}

				itemSchemas.add(mspBuilder.build());
			    }

			    /*
			     * Reset the 'properties' for this case (type definition), since the actual
			     * details are contained in the concat/coalesce members.
			     */
			    propertyMapForBuilder = new LinkedHashMap<>();

			    if (sourcePathInfosForBuilder.concatRequired()) {
				propMemberDefBuilder.concat(itemSchemas);
			    } else {
				propMemberDefBuilder.coalesce(itemSchemas);
			    }
			}
		    }

		    /*
		     * NOTE: If the property is encoded as a feature reference, the value type is
		     * already set before (together with refType or refUriTemplate, in
		     * encodeSourcePathInfosInTypeDefinitionWithFragmentsEnabled (which calls
		     * addFeatureRefDetails).
		     */
		    if (!isEncodedAsFeatureRef(pi)) {
			propMemberDefBuilder.valueType(valueTypeForBuilder);
		    }

		    ImmutableFeatureSchema propMemberDef = propMemberDefBuilder.name(pi.name()).type(typeForBuilder)
			    .propertyMap(propertyMapForBuilder).build();

		    propertyDefs.put(pi.name(), propMemberDef);
		}

	    } else {

		/*
		 * Case of fragment encoding, or of a type definition if fragments are disabled.
		 */

		if (sourcePathInfosForBuilder != null) {

		    if (sourcePathInfosForBuilder.isMultipleSourcePaths() && sourcePathInfosForBuilder.allWithRefType()
			    && pi.matches(Ldproxy2Constants.RULE_ALL_LINK_OBJECT_AS_FEATURE_REF)
			    && (pi.matches(Ldproxy2Constants.RULE_ALL_CORETABLE)
				    || LdpInfo.valueTypeHasValidLdpTypeAttributeTag(pi))
			    && sourcePathInfosForBuilder.commonValueSourcePath().isPresent()) {

			addDetailsForFeatureRefWithMultipleRefTypesAndCommonValueSourcePath(pi, propMemberDefBuilder,
				sourcePathInfosForBuilder, propertyMapForBuilder, typeCi, nowVisitedList, context);

		    } else {

			if (sourcePathInfosForBuilder.isSingleSourcePath()) {

			    LdpSourcePathInfo spi = sourcePathInfosForBuilder.getSourcePathInfos().get(0);
			    propMemberDefBuilder.sourcePath(applicableSourcePath(pi, spi));

			    if (pi.matches(Ldproxy2Constants.RULE_ALL_LINK_OBJECT_AS_FEATURE_REF)) {
				addFeatureRefDetailsFromSourcePathInfo(propMemberDefBuilder, pi, spi,
					propertyMapForBuilder);
			    }

			} else {

			    if (Ldproxy2Target.enableFragments && context.isInFragment()
				    && sourcePathProvider.multipleSourcePathsUnsupportedinFragments()) {
				/*
				 * Multiple source paths found, which is unexpected for this case, but who knows
				 * if this cannot occur in the future. Thus, better log an appropriate error
				 * message.
				 */
				MessageContext mc = result.addError(msgSource, 134);
				if (mc != null) {
				    mc.addDetail(msgSource, 1, pi.fullNameInSchema());
				}

			    } else {

				List<ImmutableFeatureSchema> itemSchemas = new ArrayList<>();

				int index = 0;
				for (LdpSourcePathInfo spi : sourcePathInfosForBuilder.getSourcePathInfos()) {

				    index++;

				    ImmutableFeatureSchema.Builder mspBuilder = new ImmutableFeatureSchema.Builder();

				    mspBuilder.name(pi.name() + "_" + index);

				    mspBuilder.sourcePath(applicableSourcePath(pi, spi));

				    if (pi.matches(Ldproxy2Constants.RULE_ALL_LINK_OBJECT_AS_FEATURE_REF)) {
					addFeatureRefDetailsFromSourcePathInfo(mspBuilder, pi, spi,
						propertyMapForBuilder);
				    }

				    // determine the correct type for the item schema

				    Type typeForMspBuilder = typeForBuilder(pi, identifierPi,
					    spi.isTargetsSingleValue(), ldpType);
				    Optional<Type> valueTypeForMspBuilder = valueTypeForBuilder(pi, identifierPi,
					    spi.isTargetsSingleValue(), ldpType);

				    mspBuilder.type(typeForMspBuilder);

				    /*
				     * NOTE: If the property is encoded as a feature reference, the value type is
				     * already set before (together with refType or refUriTemplate).
				     */
				    if (!isEncodedAsFeatureRef(pi)) {
					mspBuilder.valueType(valueTypeForMspBuilder);
				    }

				    itemSchemas.add(mspBuilder.build());
				}

				if (sourcePathInfosForBuilder.concatRequired()) {
				    propMemberDefBuilder.concat(itemSchemas);
				} else {
				    propMemberDefBuilder.coalesce(itemSchemas);
				}
			    }
			}
		    }
		}

		/*
		 * NOTE: If the property is encoded as a feature reference, the value type is
		 * already set before (together with refType or refUriTemplate, in
		 * addFeatureRefDetails).
		 */
		if (!isEncodedAsFeatureRef(pi)) {
		    propMemberDefBuilder.valueType(valueTypeForBuilder);
		}

		if (StringUtils.isNotBlank(pi.taggedValue("ldpExcludedScopes"))) {
		    for (String s : pi.taggedValue("ldpExcludedScopes").split(",")) {
			if (!s.isBlank()) {
			    try {
				Scope schemaScope = Scope.valueOf(s.trim().toUpperCase(Locale.ENGLISH));
				propMemberDefBuilder.addExcludedScopes(schemaScope);
			    } catch (Exception e) {
				MessageContext mc = result.addError(msgSource, 136, pi.inClass().name(), pi.name(),
					s.trim());
				if (mc != null) {
				    mc.addDetail(msgSource, 1, pi.fullNameInSchema());
				}
			    }
			}
		    }
		}

		ImmutableFeatureSchema propMemberDef = propMemberDefBuilder.name(pi.name()).label(LdpInfo.label(pi))
			.description(LdpInfo.description(pi)).type(typeForBuilder).objectType(objectTypeForBuilder)
			.constraints(constraints).role(propRoleForBuilder).constantValue(constantValueForBuilder)
			.geometryType(geometryTypeForBuilder).linearizeCurves(linearizeCurvesOpt).unit(unitForBuilder)
			.propertyMap(propertyMapForBuilder).build();

		propertyDefs.put(pi.name(), propMemberDef);

//		// create more service constraint content
//		if (StringUtils.isNotBlank(pi.taggedValue("ldpRemove"))) {
//		    String tv = pi.taggedValue("ldpRemove").trim().toUpperCase(Locale.ENGLISH);
//		    if (tv.equals("IN_COLLECTION") || tv.equals("ALWAYS") || tv.equals("NEVER")) {
//			ImmutablePropertyTransformation trf = new ImmutablePropertyTransformation.Builder().remove(tv)
//				.build();
//			bbFeaturesHtmlBuilder
//				.addPropertyTransformationToBuildingBlockOfCollectionInServiceConfiguration(
//					nowVisitedList.get(0).inClass(), propertyPath(nowVisitedList), trf);
//		    } else {
//			MessageContext mc = result.addError(msgSource, 122, pi.name(), pi.taggedValue("ldpRemove"));
//			if (mc != null) {
//			    mc.addDetail(msgSource, 1, pi.fullNameInSchema());
//			}
//		    }
//		}
//		if (ldpType == Type.DATE && StringUtils.isNotBlank(Ldproxy2Target.dateFormat)) {
//		    ImmutablePropertyTransformation trf = new ImmutablePropertyTransformation.Builder()
//			    .dateFormat(Ldproxy2Target.dateFormat).build();
//		    bbFeaturesHtmlBuilder.addPropertyTransformationToBuildingBlockOfCollectionInServiceConfiguration(
//			    nowVisitedList.get(0).inClass(), propertyPath(nowVisitedList), trf);
//		}
//		if (ldpType == Type.DATETIME && StringUtils.isNotBlank(Ldproxy2Target.dateTimeFormat)) {
//		    ImmutablePropertyTransformation trf = new ImmutablePropertyTransformation.Builder()
//			    .dateFormat(Ldproxy2Target.dateTimeFormat).build();
//		    bbFeaturesHtmlBuilder.addPropertyTransformationToBuildingBlockOfCollectionInServiceConfiguration(
//			    nowVisitedList.get(0).inClass(), propertyPath(nowVisitedList), trf);
//		}
//
//		if (bbFeaturesGmlBuilder != null) {
//
//		    ClassInfo firstCi = nowVisitedList.get(0).inClass();
//
//		    bbFeaturesGmlBuilder.register(pi, firstCi, propertyPath(nowVisitedList));
//		}
	    }
	}

	/*
	 * =============
	 * 
	 * Handle generic value types
	 * 
	 * =============
	 */
	if (target.isGenericValueType(currentCi)) {
	    handleGenericValueType(currentCi, propertyDefs);
	}

	return propertyDefs;
    }

    private void addDetailsForFeatureRefWithMultipleRefTypesAndCommonValueSourcePath(PropertyInfo pi,
	    Builder propMemberDefBuilder, LdpSourcePathInfos sourcePathInfosForBuilder,
	    LinkedHashMap<String, FeatureSchema> propertyMapForBuilder, ClassInfo typeCi,
	    List<PropertyInfo> nowVisitedList, LdpPropertyEncodingContext context) {

	propMemberDefBuilder.sourcePath(sourcePathInfosForBuilder.commonValueSourcePath().get());

	ImmutableFeatureSchema idSubProp = createIdPropertyForFeatureRef(pi, sourcePathInfosForBuilder.spis.get(0));
	propertyMapForBuilder.put("id", idSubProp);

	if (LdpInfo.valueTypeHasValidLdpTitleAttributeTag(pi)) {

	    ImmutableFeatureSchema titleSubProp = createTitlePropertyForLinkOrFeatureRef(pi);
	    propertyMapForBuilder.put("title", titleSubProp);
	}

	addTypeDetailsToFeatureRefPropertyWithCommonSourcePath(pi, typeCi, nowVisitedList, context,
		sourcePathInfosForBuilder, propertyMapForBuilder);
    }

    private ImmutableFeatureSchema createIdPropertyForFeatureRef(PropertyInfo pi, LdpSourcePathInfo spi) {

	ImmutableSchemaConstraints.Builder constraintsBuilder = new ImmutableSchemaConstraints.Builder();
	constraintsBuilder.required(true);

	ImmutableFeatureSchema.Builder idPropBuilder = new ImmutableFeatureSchema.Builder();

	idPropBuilder.name("id").type(ldpProvider.idValueTypeForFeatureRef(pi, spi))
		.sourcePath(sourcePathProvider.sourcePathFeatureRefId(pi)).constraints(constraintsBuilder.build());

	return idPropBuilder.build();
    }

    private ImmutableFeatureSchema createHrefPropertyForLink(PropertyInfo pi) {

	ImmutableFeatureSchema.Builder linkPropHrefBuilder = new ImmutableFeatureSchema.Builder();
	linkPropHrefBuilder.name("href").label(pi.typeInfo().name + "-ID").type(Type.STRING)
		.sourcePath(sourcePathProvider.sourcePathLinkLevelHref(pi));
	linkPropHrefBuilder.addAllTransformationsBuilders(new ImmutablePropertyTransformation.Builder()
		.stringFormat(sourcePathProvider.urlTemplateForValueType(pi)));

	return linkPropHrefBuilder.build();
    }

    private ImmutableFeatureSchema createTitlePropertyForLinkOrFeatureRef(PropertyInfo pi) {

	ImmutableFeatureSchema.Builder titlePropBuilder = new ImmutableFeatureSchema.Builder().name("title")
		.type(Type.STRING).label(pi.typeInfo().name + "-title");
	List<String> titleSourcePaths = sourcePathProvider.sourcePathsLinkLevelTitle(pi);

	/*
	 * 2024-02-07 [JE] multiple source paths no longer works in the feature ref
	 * encoding (for link objects, it does).
	 */
	if (pi.matches(Ldproxy2Constants.RULE_ALL_LINK_OBJECT_AS_FEATURE_REF)) {
	    List<String> newTitleSourcePaths = new ArrayList<>();
	    newTitleSourcePaths.add(titleSourcePaths.get(0));
	    titleSourcePaths = newTitleSourcePaths;
	}

	if (titleSourcePaths.size() == 1) {
	    titlePropBuilder = titlePropBuilder.sourcePath(titleSourcePaths.get(0));
	} else {
	    List<ImmutableFeatureSchema> titleCoalesceSchemas = new ArrayList<>();
	    int index = 0;
	    for (String titleSourcePath : titleSourcePaths) {
		index++;
		ImmutableFeatureSchema tcs = (new ImmutableFeatureSchema.Builder()).name("titleSourcePath" + index)
			.sourcePath(titleSourcePath).build();
		titleCoalesceSchemas.add(tcs);
	    }
	    titlePropBuilder = titlePropBuilder.coalesce(titleCoalesceSchemas);
	}

	return titlePropBuilder.build();
    }

    /**
     * Adds a number of information items to a feature-ref property, to which a
     * common source path applies. Adds a type sub-property. That property either
     * has a constant value, if only a single ref type is involved. Otherwise, i.e.,
     * multiple ref types apply, no refType member is created, and the type
     * sub-property has an enum constraint which lists the ref types. In that case,
     * and if either coretable conversion applies, or the value type of the
     * feature-ref-property has a valid type attribute, also a source is set for the
     * type sub-property (which is used to retrieve the actual type of a given
     * object value for the feature-ref).
     * 
     * @param pi                        the feature-ref property
     * @param typeCi                    the value type of the feature-ref property
     * @param nowVisitedList            the list of visited properties (including
     *                                  the feature-ref property)
     * @param context                   the property encoding context for the
     *                                  feature-ref property
     * @param sourcePathInfosForBuilder source path infos for the feature-ref
     *                                  property
     * @param propertyMapForBuilder     property map for the feature-ref property
     *                                  (to be added as the 'properties' member of
     *                                  that property)
     */
    private void addTypeDetailsToFeatureRefPropertyWithCommonSourcePath(PropertyInfo pi, ClassInfo typeCi,
	    List<PropertyInfo> nowVisitedList, LdpPropertyEncodingContext context,
	    LdpSourcePathInfos sourcePathInfosForBuilder, LinkedHashMap<String, FeatureSchema> propertyMapForBuilder) {

	ImmutableFeatureSchema.Builder typePropertyDefBuilder = new ImmutableFeatureSchema.Builder();
	typePropertyDefBuilder.name("type").type(Type.STRING);

	List<LdpSourcePathInfo> spis = sourcePathInfosForBuilder.getSourcePathInfos();

	if (spis.size() == 1) {
	    String singularRefType = spis.get(0).getRefType();
	    typePropertyDefBuilder.constantValue(singularRefType);
	} else {

	    if (pi.matches(Ldproxy2Constants.RULE_ALL_CORETABLE)) {
		typePropertyDefBuilder.sourcePath(Ldproxy2Target.coretableFeatureTypeColumn);
	    } else {
		PropertyInfo typeAttribute = LdpInfo.getTypeAttribute(typeCi);
//		    LdpPropertyEncodingContext typeCiContext = ldpProvider.createInitialPropertyEncodingContext(
//			    typeCi, !Ldproxy2Target.enableFragments || !context.isInFragment());
//		    LdpSourcePathInfos typeAttributeSpis = sourcePathProvider.sourcePathPropertyLevel(
//			    typeAttribute, new ArrayList<PropertyInfo>(), typeCiContext);
		LdpSourcePathInfos typeAttributeSpis = sourcePathProvider.sourcePathPropertyLevel(typeAttribute,
			nowVisitedList, ldpProvider.createChildContext(context, typeCi));

		if (typeAttributeSpis.commonValueSourcePath().isPresent()) {
		    typePropertyDefBuilder.sourcePath(typeAttributeSpis.commonValueSourcePath().get());
		}
	    }

	    typePropertyDefBuilder.constraintsBuilder().required(true)
		    .addAllEnumValues(spis.stream().map(spi -> spi.getRefType()).sorted().collect(Collectors.toList()));

	}
	propertyMapForBuilder.put("type", typePropertyDefBuilder.build());
    }

    private void handleGenericValueType(ClassInfo ci, LinkedHashMap<String, FeatureSchema> propertyDefs) {

	// determine common attribute in subtypes
	Optional<String> valuePropNameOpt = GenericValueTypeUtil.commonValuePropertyOfSubtypes(ci);

	if (valuePropNameOpt.isEmpty()) {
	    result.addError(msgSource, 135, ci.name());
	} else {

	    String valuePropName = valuePropNameOpt.get();

	    // create 'dataType' and common value properties
	    SortedMap<String, ClassInfo> subtypesByName = new TreeMap<>();
	    for (ClassInfo subtype : ci.subtypesInCompleteHierarchy()) {
		subtypesByName.put(subtype.name(), subtype);
	    }

	    ImmutableFeatureSchema.Builder dataTypeMemberDef = new ImmutableFeatureSchema.Builder().name("dataType")
		    .sourcePath(sourcePathProvider.sourcePathForDataTypeMemberOfGenericValueType()).type(Type.STRING)
		    .label("data type");
	    ImmutableSchemaConstraints.Builder dataTypeMemberConstraintsBuilder = new ImmutableSchemaConstraints.Builder();
	    dataTypeMemberConstraintsBuilder.required(true);
	    dataTypeMemberConstraintsBuilder.enumValues(subtypesByName.keySet());
	    dataTypeMemberDef.constraints(dataTypeMemberConstraintsBuilder.build());
	    propertyDefs.put("dataType", dataTypeMemberDef.build());

	    ImmutableFeatureSchema.Builder valueMemberDef = new ImmutableFeatureSchema.Builder().name(valuePropName)
		    .type(Type.VALUE).label(valuePropName);

	    List<FeatureSchema> coalesceItems = new ArrayList<>();
	    for (Entry<String, ClassInfo> e : subtypesByName.entrySet()) {
		ClassInfo subtype = e.getValue();
		PropertyInfo valueProp = subtype.property(valuePropName);
		String suffix = StringUtils.defaultIfBlank(valueProp.taggedValue("ldpGenericValueTypeSuffix"),
			subtype.name());
		Type ldpType = target.ldproxyType(valueProp);
		ImmutableFeatureSchema.Builder item = new ImmutableFeatureSchema.Builder()
			.name("case_" + subtype.name())
			.sourcePath(
				sourcePathProvider.sourcePathForValueMemberOfGenericValueType(valuePropName, suffix))
			.valueType(ldpType);
		coalesceItems.add(item.build());
	    }
	    valueMemberDef.coalesce(coalesceItems);
	    propertyDefs.put(valuePropName, valueMemberDef.build());
	}
    }

    private Optional<Type> valueTypeForBuilder(PropertyInfo pi, PropertyInfo identifierPi, boolean isSingleValued,
	    Type ldpType) {

	if (!isSingleValued && !(ldpType == Type.OBJECT || ldpType == Type.GEOMETRY || pi == identifierPi)) {
	    return Optional.of(ldpType);
	} else {
	    return Optional.empty();
	}
    }

    private Type typeForBuilder(PropertyInfo pi, PropertyInfo identifierPi, boolean isSingleValued, Type ldpType) {

	if (!isSingleValued) {
	    if (ldpType == Type.OBJECT) {
		return isEncodedAsFeatureRef(pi) ? Type.FEATURE_REF_ARRAY : Type.OBJECT_ARRAY;
	    } else if (ldpType == Type.GEOMETRY || pi == identifierPi) {
		// no array for geometry and identifier properties
		return ldpType;
	    } else {
		return Type.VALUE_ARRAY;
	    }
	} else {
	    return isEncodedAsFeatureRef(pi) ? Type.FEATURE_REF : ldpType;
	}
    }

    private void addFeatureRefDetailsFromSourcePathInfo(ImmutableFeatureSchema.Builder schemaBuilder, PropertyInfo pi,
	    LdpSourcePathInfo spi, LinkedHashMap<String, FeatureSchema> propertyMapForBuilder) {

	if (StringUtils.isNotBlank(spi.getRefType()) || StringUtils.isNotBlank(spi.getRefUriTemplate())) {

	    boolean ignoreIdValueType = false;

	    if (StringUtils.isNotBlank(spi.getRefType())) {
		schemaBuilder.refType(spi.getRefType());

		if (LdpInfo.valueTypeHasValidLdpTitleAttributeTag(pi)) {

		    /*
		     * In this case, where id and title properties are explicitly set for the
		     * feature ref, its source path will result in the actual object, not just the
		     * id. So ignore / do not encode the id value type.
		     */
		    ignoreIdValueType = true;

		    ImmutableFeatureSchema idSubProp = createIdPropertyForFeatureRef(pi, spi);
		    propertyMapForBuilder.put("id", idSubProp);

		    ImmutableFeatureSchema titleSubProp = createTitlePropertyForLinkOrFeatureRef(pi);
		    propertyMapForBuilder.put("title", titleSubProp);
		}
	    }

	    if (StringUtils.isNotBlank(spi.getRefUriTemplate())) {
		schemaBuilder.refUriTemplate(spi.getRefUriTemplate());
	    }

	    if (!ignoreIdValueType) {
		Type idValueType = ldpProvider.idValueTypeForFeatureRef(pi, spi);
		if (idValueType != null) {
		    schemaBuilder.valueType(idValueType);
		}
	    }
	}
    }

    private void encodeSourcePathInfosInTypeDefinitionWithFragmentsEnabled(PropertyInfo pi, ClassInfo typeCi,
	    List<PropertyInfo> alreadyVisitedPiList, List<PropertyInfo> nowVisitedList, ClassInfo currentCi,
	    LdpSourcePathInfo spi, LdpPropertyEncodingContext context, ImmutableFeatureSchema.Builder schemaBuilder,
	    Type ldpType, LinkedHashMap<String, FeatureSchema> propertyMapForBuilder) {

	schemaBuilder.sourcePath(applicableSourcePath(pi, spi));

	if (pi.matches(Ldproxy2Constants.RULE_ALL_LINK_OBJECT_AS_FEATURE_REF)) {
	    addFeatureRefDetailsFromSourcePathInfo(schemaBuilder, pi, spi, propertyMapForBuilder);
	}

	if (!LdpUtil.isLdproxySimpleType(ldpType) && (pi.categoryOfValue() == Options.DATATYPE)) {

	    Optional<ClassInfo> optActualTypeCi = ldpProvider.actualTypeClass(spi, pi);

	    if (optActualTypeCi.isPresent()) {

		// original typeCi of pi is required for circular dependency check
		ClassInfo originalTypeCi = pi.typeClass();

		// detect circular dependency in the property path
		if (alreadyVisitedPiList.stream().anyMatch(vPi -> vPi.inClass() == originalTypeCi)
			|| originalTypeCi == currentCi) {

		    // the circular dependency is already reported for the fragment case

		} else {

		    ClassInfo actualTypeCi = optActualTypeCi.get();

		    LdpPropertyEncodingContext nextContext = ldpProvider.createChildContext(context, actualTypeCi, spi);

		    LinkedHashMap<String, FeatureSchema> datatypePropertyDefs = propertyDefinitions(actualTypeCi,
			    nowVisitedList, nextContext);

		    schemaBuilder.propertyMap(datatypePropertyDefs);

		    schemaBuilder.objectType(Optional.of(LdpInfo.originalClassName(actualTypeCi)));
		    if (!target.isMappedToLink(actualTypeCi)) {
			schemaBuilder.schema(LdpUtil.fragmentRef(actualTypeCi));
		    }

//				if (bbFeaturesGmlBuilder != null) {
//				    bbFeaturesGmlBuilder.register(actualTypeCi);
//				}
		}
	    }
	}
    }

    private Optional<String> applicableSourcePath(PropertyInfo pi, LdpSourcePathInfo spi) {
	return (spi.getIdSourcePath().isPresent() && !isFeatureRefWithTitle(pi)) ? spi.getIdSourcePath()
		: spi.getValueSourcePath();
    }

    private boolean isFeatureRefWithTitle(PropertyInfo pi) {
	return LdpInfo.isTypeWithIdentityValueType(pi)
		&& pi.matches(Ldproxy2Constants.RULE_ALL_LINK_OBJECT_AS_FEATURE_REF)
		&& LdpInfo.valueTypeHasValidLdpTitleAttributeTag(pi);
    }

    private boolean isEncodedAsFeatureRef(PropertyInfo pi) {
	return target.isMappedToOrImplementedAsLink(pi)
		&& pi.matches(Ldproxy2Constants.RULE_ALL_LINK_OBJECT_AS_FEATURE_REF);
    }

    /**
     * NOTE: The method calls itself recursively.
     * 
     * @param currentCi            - tbd
     * @param alreadyVisitedPiList - tbd
     * @param context              provides additional information about the context
     *                             in which property definitions shall be encoded
     */
    public void propertyDefinitionsForServiceConfiguration(ClassInfo currentCi, List<PropertyInfo> alreadyVisitedPiList,
	    LdpPropertyEncodingContext context) {

	ClassInfo typeDefinitionCi = context.getTopParentContext().getType();

	/*
	 * If fragment encoding is enabled and we are in a type definition, encode all
	 * properties (from direct and indirect supertypes, taking into account the
	 * order of the xsd encoding, and property overrides).
	 */
	Collection<PropertyInfo> propsToProcess = (Ldproxy2Target.enableFragments && !context.isInFragment()
		&& currentCi.category() != Options.DATATYPE) ? LdpInfo.allPropertiesInOrderOfXsdEncoding(currentCi)
			: currentCi.properties().values();

	for (PropertyInfo pi : propsToProcess) {

	    if (!LdpInfo.isEncoded(pi) || target.isIgnored(pi)) {
		continue;
	    }

	    List<PropertyInfo> nowVisitedList = new ArrayList<>(alreadyVisitedPiList);
	    nowVisitedList.add(pi);

	    Type ldpType = target.ldproxyType(pi);

	    // handle embedded cases (datatype or link properties)
	    if (target.isMappedToOrImplementedAsLink(pi)) {

		// can be ignored when encoding the service configuration

//		if (pi.matches(Ldproxy2Constants.RULE_ALL_LINK_OBJECT_AS_FEATURE_REF)) {
//		    // feature ref case
//		} else {
//		    // link object case
//		}

	    } else if (!LdpUtil.isLdproxySimpleType(ldpType) && pi.categoryOfValue() == Options.DATATYPE) {

		ClassInfo typeCi = pi.typeClass();

		if (!target.isGenericValueType(typeCi)) {

		    SortedSet<ClassInfo> typeAndSubtypesSet = new TreeSet<>();
		    typeAndSubtypesSet.add(typeCi);
		    typeAndSubtypesSet.addAll(typeCi.subtypesInCompleteHierarchy());

		    List<ClassInfo> typeAndSubtypes = typeAndSubtypesSet
			    .stream().filter(actualTypeCi -> !actualTypeCi.isAbstract()
				    && LdpInfo.isEncoded(actualTypeCi) && target.mapEntry(actualTypeCi).isEmpty())
			    .collect(Collectors.toList());

		    for (ClassInfo actualTypeCi : typeAndSubtypes) {

			// detect circular dependency in the property path -
			if (alreadyVisitedPiList.stream().anyMatch(vPi -> vPi.inClass() == actualTypeCi)
				|| actualTypeCi == currentCi) {
			    /*
			     * circular paths are not supported
			     */
			} else {

			    LdpPropertyEncodingContext nextContext = ldpProvider.createChildContext(context,
				    actualTypeCi);

			    propertyDefinitionsForServiceConfiguration(actualTypeCi, nowVisitedList, nextContext);

			    if (bbFeaturesGmlBuilder != null) {
				bbFeaturesGmlBuilder.register(actualTypeCi);
			    }
			}
		    }
		}
	    }

	    if (LdpInfo.isEnumerationOrCodelistValueType(pi) && !target.valueTypeIsMapped(pi)) {

		ClassInfo typeCi = pi.typeClass();

		String codelistId = LdpInfo.codelistId(typeCi);

		if (!Ldproxy2Target.codelistsAndEnumerations.contains(typeCi)) {
		    /*
		     * Handle case of enumeration/codelist that is not encoded, unmapped, or not
		     * from the schemas selected for processing.
		     */
		    MessageContext mc = result.addWarning(msgSource, 127, typeCi.name(), codelistId);
		    if (mc != null) {
			mc.addDetail(msgSource, 0, typeCi.fullNameInSchema());
		    }
		}

		// Create content for inclusion in service config:
		ImmutablePropertyTransformation trf = new ImmutablePropertyTransformation.Builder().codelist(codelistId)
			.build();
		bbFeaturesHtmlBuilder.addPropertyTransformationToBuildingBlockOfCollectionInServiceConfiguration(
			typeDefinitionCi, propertyPath(nowVisitedList), trf);
	    }

	    // create more service constraint content
	    if (StringUtils.isNotBlank(pi.taggedValue("ldpRemove"))) {
		String tv = pi.taggedValue("ldpRemove").trim().toUpperCase(Locale.ENGLISH);
		if (tv.equals("IN_COLLECTION") || tv.equals("ALWAYS") || tv.equals("NEVER")) {
		    ImmutablePropertyTransformation trf = new ImmutablePropertyTransformation.Builder().remove(tv)
			    .build();
		    bbFeaturesHtmlBuilder.addPropertyTransformationToBuildingBlockOfCollectionInServiceConfiguration(
			    typeDefinitionCi, propertyPath(nowVisitedList), trf);
		} else {
		    MessageContext mc = result.addError(msgSource, 122, pi.name(), pi.taggedValue("ldpRemove"));
		    if (mc != null) {
			mc.addDetail(msgSource, 1, pi.fullNameInSchema());
		    }
		}
	    }
	    if (ldpType == Type.DATE && StringUtils.isNotBlank(Ldproxy2Target.dateFormat)) {
		ImmutablePropertyTransformation trf = new ImmutablePropertyTransformation.Builder()
			.dateFormat(Ldproxy2Target.dateFormat).build();
		bbFeaturesHtmlBuilder.addPropertyTransformationToBuildingBlockOfCollectionInServiceConfiguration(
			typeDefinitionCi, propertyPath(nowVisitedList), trf);
	    }
	    if (ldpType == Type.DATETIME && StringUtils.isNotBlank(Ldproxy2Target.dateTimeFormat)) {
		ImmutablePropertyTransformation trf = new ImmutablePropertyTransformation.Builder()
			.dateFormat(Ldproxy2Target.dateTimeFormat).build();
		bbFeaturesHtmlBuilder.addPropertyTransformationToBuildingBlockOfCollectionInServiceConfiguration(
			typeDefinitionCi, propertyPath(nowVisitedList), trf);
	    }

	    if (bbFeaturesGmlBuilder != null) {
		bbFeaturesGmlBuilder.register(pi, typeDefinitionCi, propertyPath(nowVisitedList));
	    }

	    if (bbFeaturesGeoJsonBuilder != null) {

		ClassInfo typeCi = pi.typeClass();

		if (typeCi != null) {
		    if (target.isGenericValueType(typeCi)) {
			ImmutablePropertyTransformation trf = new ImmutablePropertyTransformation.Builder()
				.remove("ALWAYS").build();
			bbFeaturesGeoJsonBuilder
				.addPropertyTransformationToBuildingBlockOfCollectionInServiceConfiguration(
					typeDefinitionCi, propertyPath(nowVisitedList) + ".dataType", trf);
		    }
		}
	    }

	    if (bbFeaturesJsonFgBuilder != null) {

		ClassInfo typeCi = pi.typeClass();

		if (typeCi != null) {
		    if (target.isGenericValueType(typeCi)) {
			ImmutablePropertyTransformation trf = new ImmutablePropertyTransformation.Builder()
				.remove("ALWAYS").build();
			bbFeaturesJsonFgBuilder
				.addPropertyTransformationToBuildingBlockOfCollectionInServiceConfiguration(
					typeDefinitionCi, propertyPath(nowVisitedList) + ".dataType", trf);
		    }
		}
	    }
	}
    }

    private String propertyPath(List<PropertyInfo> propertyList) {
	return propertyList.stream().map(pi -> pi.name()).collect(Collectors.joining("."));
    }

    private String constantValue(PropertyInfo pi) {

	String valueTypeName = pi.typeInfo().name;

	ProcessMapEntry pme = Ldproxy2Target.mapEntryParamInfos.getMapEntry(valueTypeName,
		pi.encodingRule(Ldproxy2Constants.PLATFORM));

	if (pme != null && !target.ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme, pi.typeInfo().id)) {

	    if (valueTypeName.equalsIgnoreCase("Boolean")) {

		if ("true".equalsIgnoreCase(pi.initialValue().trim())) {
		    return StringUtils.defaultIfBlank(Ldproxy2Target.mapEntryParamInfos.getCharacteristic(valueTypeName,
			    pi.encodingRule(Ldproxy2Constants.PLATFORM),
			    Ldproxy2Constants.ME_PARAM_INITIAL_VALUE_ENCODING,
			    Ldproxy2Constants.ME_PARAM_INITIAL_VALUE_ENCODING_CHARACT_FALSE), "true");
		} else if ("false".equalsIgnoreCase(pi.initialValue().trim())) {
		    return StringUtils.defaultIfBlank(Ldproxy2Target.mapEntryParamInfos.getCharacteristic(valueTypeName,
			    pi.encodingRule(Ldproxy2Constants.PLATFORM),
			    Ldproxy2Constants.ME_PARAM_INITIAL_VALUE_ENCODING,
			    Ldproxy2Constants.ME_PARAM_INITIAL_VALUE_ENCODING_CHARACT_FALSE), "false");
		}
	    }
	}

	return pi.initialValue();
    }

    /**
     * @param pi
     * @return <code>true</code>, if the maximum multiplicity of pi is 1 and either
     *         the value type of pi is mapped to LINK or the value type does NOT
     *         have a valid value for tag ldpTitleAttribute; otherwise
     *         <code>false</code>
     */
    private boolean omitSourcePathOnPropertyLevelForLinkObjectProperty(PropertyInfo pi) {

	return (pi.cardinality().maxOccurs == 1
		&& (target.isMappedToLink(pi) || !LdpInfo.valueTypeHasValidLdpTitleAttributeTag(pi)));
    }

    private Optional<SimpleFeatureGeometry> geometryType(PropertyInfo pi) {

	String typeName = pi.typeInfo().name;
	String typeId = pi.typeInfo().id;
	String encodingRule = pi.encodingRule(Ldproxy2Constants.PLATFORM);

	SimpleFeatureGeometry res = SimpleFeatureGeometry.ANY;

	ProcessMapEntry pme = Ldproxy2Target.mapEntryParamInfos.getMapEntry(typeName, encodingRule);

	if (pme != null && !target.ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme, typeId)
		&& pme.hasTargetType() && "GEOMETRY".equalsIgnoreCase(pme.getTargetType())
		&& Ldproxy2Target.mapEntryParamInfos.hasCharacteristic(typeName, encodingRule,
			Ldproxy2Constants.ME_PARAM_GEOMETRY_INFOS,
			Ldproxy2Constants.ME_PARAM_GEOMETRY_INFOS_CHARACT_GEOMETRY_TYPE)) {

	    String t = Ldproxy2Target.mapEntryParamInfos.getCharacteristic(typeName, encodingRule,
		    Ldproxy2Constants.ME_PARAM_GEOMETRY_INFOS,
		    Ldproxy2Constants.ME_PARAM_GEOMETRY_INFOS_CHARACT_GEOMETRY_TYPE);

	    res = SimpleFeatureGeometry.valueOf(t.toUpperCase(Locale.ENGLISH));

	} else {
	    // is checked via target configuration validator (which can be switched off)
	    MessageContext mc = result.addError(msgSource, 121, pi.name(), pi.typeInfo().name);
	    if (mc != null) {
		mc.addDetail(msgSource, 1, pi.fullNameInSchema());
	    }
	}

	return Optional.of(res);
    }

}
