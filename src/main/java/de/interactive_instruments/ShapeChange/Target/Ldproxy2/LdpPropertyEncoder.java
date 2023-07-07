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
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange.Target.Ldproxy2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableSchemaConstraints;
import de.ii.xtraplatform.features.domain.SchemaBase.Role;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.Ldproxy2.LdpSqlSourcePathInfos.SourcePathInfo;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class LdpPropertyEncoder {

    protected ShapeChangeResult result;
    protected Ldproxy2Target target;
    protected MessageSource msgSource;

    protected LdpSqlProviderHelper sqlProviderHelper = new LdpSqlProviderHelper();
    protected LdpSqlSourcePathProvider sqlSourcePathProvider;

    protected LdpBuildingBlockFeaturesGmlBuilder bbFeaturesGmlBuilder;
    protected LdpBuildingBlockFeaturesHtmlBuilder bbFeaturesHtmlBuilder;

    public LdpPropertyEncoder(Ldproxy2Target target, LdpBuildingBlockFeaturesGmlBuilder gml,
	    LdpBuildingBlockFeaturesHtmlBuilder featuresHtml) {

	this.target = target;
	this.result = target.result;
	this.msgSource = target;

	this.bbFeaturesGmlBuilder = gml;
	this.bbFeaturesHtmlBuilder = featuresHtml;

	this.sqlSourcePathProvider = new LdpSqlSourcePathProvider(target);
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
	    List<PropertyInfo> alreadyVisitedPiList, PropertyEncodingContext context) {

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

	    LdpSpecialPropertiesInfo spi = new LdpSpecialPropertiesInfo(currentCi, target);

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
			.name(Ldproxy2Target.objectIdentifierName).sourcePath(Ldproxy2Target.primaryKeyColumn)
			.type(Type.INTEGER).role(Role.ID).build();
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

	    List<PropertyInfo> nowVisitedList = new ArrayList<>(alreadyVisitedPiList);
	    nowVisitedList.add(pi);

	    ImmutableFeatureSchema.Builder propMemberDefBuilder = new ImmutableFeatureSchema.Builder();

	    Type ldpType = target.ldproxyType(pi);

	    Type typeForBuilder = typeForBuilder(pi, identifierPi, pi.cardinality().maxOccurs == 1, ldpType);
	    Optional<Type> valueTypeForBuilder = valueTypeForBuilder(pi, identifierPi, pi.cardinality().maxOccurs == 1,
		    ldpType);

	    Optional<SimpleFeatureGeometry> geometryTypeForBuilder = Optional.empty();
	    if (ldpType == Type.GEOMETRY) {
		geometryTypeForBuilder = geometryType(pi);
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
//			refTypeForBuilder = Optional.of(pi.typeInfo().name.toLowerCase(Locale.ENGLISH));
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

		    ImmutableFeatureSchema.Builder titlePropBuilder = new ImmutableFeatureSchema.Builder().name("title")
			    .label(pi.typeInfo().name + "-title").type(Type.STRING);
		    List<String> titleSourcePaths = sqlSourcePathProvider.sourcePathsLinkLevelTitle(pi);
		    if (titleSourcePaths.size() == 1) {
			titlePropBuilder = titlePropBuilder.sourcePath(titleSourcePaths.get(0));
		    } else {
			titlePropBuilder = titlePropBuilder.sourcePaths(titleSourcePaths);
		    }

		    linkPropertyDefs.put("title", titlePropBuilder.build());

		    ImmutableFeatureSchema.Builder linkPropHrefBuilder = new ImmutableFeatureSchema.Builder();
		    linkPropHrefBuilder.name("href").label(pi.typeInfo().name + "-ID").type(Type.STRING)
			    .sourcePath(sqlSourcePathProvider.sourcePathLinkLevelHref(pi));
		    linkPropHrefBuilder.addAllTransformationsBuilders(new ImmutablePropertyTransformation.Builder()
			    .stringFormat(sqlSourcePathProvider.urlTemplateForValueType(pi)));
		    linkPropertyDefs.put("href", linkPropHrefBuilder.build());

		    propertyMapForBuilder = linkPropertyDefs;
		}

	    } else if (!LdpUtil.isLdproxySimpleType(ldpType) && pi.categoryOfValue() == Options.DATATYPE) {

		ClassInfo typeCi = pi.typeClass();

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

			if (typeCi.subtypesInCompleteHierarchy().size() > 0) {

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

			PropertyEncodingContext nextContext = createChildContext(context, typeCi,
				sqlProviderHelper.databaseTableName(typeCi, false));

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
	    LdpSqlSourcePathInfos sourcePathInfosForProperty = sqlSourcePathProvider.sourcePathPropertyLevel(pi,
		    alreadyVisitedPiList, context);
	    LdpSqlSourcePathInfos sourcePathInfosForBuilder = null;

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
		    if (context.isInFragment() && isEncodedWithDirectValueSourcePath(pi, context)) {
			sourcePathInfosForBuilder = sourcePathInfosForProperty;
		    } else if (!context.isInFragment() && !isEncodedWithDirectValueSourcePath(pi, context)) {
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

		ClassInfo typeCi = pi.typeClass();

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

		    /*
		     * The property is not fully defined in the fragment (that information has been
		     * determined before, when setting the value for sourcePathInfosForBuilder).
		     */

		    if (sourcePathInfosForBuilder.isSingleSourcePath()) {

			SourcePathInfo spi = sourcePathInfosForBuilder.getSourcePathInfos().get(0);

			encodeSourcePathInfosInTypeDefinitionWithFragmentsEnabled(pi, alreadyVisitedPiList,
				nowVisitedList, currentCi, spi, context, propMemberDefBuilder, ldpType);

		    } else if (sourcePathInfosForBuilder.isMultipleSourcePaths()) {

			List<ImmutableFeatureSchema> itemSchemas = new ArrayList<>();

			for (SourcePathInfo spi : sourcePathInfosForBuilder.getSourcePathInfos()) {

			    ImmutableFeatureSchema.Builder mspBuilder = new ImmutableFeatureSchema.Builder();

			    mspBuilder.name(pi.name() + "_to_" + spi.targetTable);

			    encodeSourcePathInfosInTypeDefinitionWithFragmentsEnabled(pi, alreadyVisitedPiList,
				    nowVisitedList, currentCi, spi, context, mspBuilder, ldpType);

			    // determine the correct type for the item schema

			    Type typeForMspBuilder = typeForBuilder(pi, identifierPi, spi.targetsSingleValue, ldpType);
			    Optional<Type> valueTypeForMspBuilder = valueTypeForBuilder(pi, identifierPi,
				    spi.targetsSingleValue, ldpType);

			    mspBuilder.type(typeForMspBuilder).valueType(valueTypeForMspBuilder);

			    itemSchemas.add(mspBuilder.build());
			}

			if (sourcePathInfosForBuilder.concatRequired()) {
			    propMemberDefBuilder.concat(itemSchemas);
			} else {
			    propMemberDefBuilder.coalesce(itemSchemas);
			}
		    }

		    propMemberDefBuilder.name(pi.name()).type(typeForBuilder).valueType(valueTypeForBuilder);

		    propertyDefs.put(pi.name(), propMemberDefBuilder.build());
		}

	    } else {

		/*
		 * Case of fragment encoding, or of a type definition if fragments are disabled.
		 */

		if (sourcePathInfosForBuilder != null) {

		    if (sourcePathInfosForBuilder.isSingleSourcePath()) {

			SourcePathInfo spi = sourcePathInfosForBuilder.getSourcePathInfos().get(0);
			propMemberDefBuilder.sourcePath(spi.sourcePath);

			if (pi.matches(Ldproxy2Constants.RULE_ALL_LINK_OBJECT_AS_FEATURE_REF)) {
			    addFeatureRefDetailsFromSourcePathInfo(propMemberDefBuilder, spi);
			}

		    } else {

			/*
			 * Multiple source paths found, which is unexpected for this case, but who knows
			 * if this cannot occur in the future. Thus, better log an appropriate error
			 * message.
			 */
			MessageContext mc = result.addError(msgSource, 134);
			if (mc != null) {
			    mc.addDetail(msgSource, 1, pi.fullNameInSchema());
			}
		    }
		}

		ImmutableFeatureSchema propMemberDef = propMemberDefBuilder.name(pi.name()).label(LdpInfo.label(pi))
			.description(LdpInfo.description(pi)).type(typeForBuilder).objectType(objectTypeForBuilder)
			.valueType(valueTypeForBuilder).constraints(constraints).role(propRoleForBuilder)
			.constantValue(constantValueForBuilder).geometryType(geometryTypeForBuilder)
			.unit(unitForBuilder).propertyMap(propertyMapForBuilder).build();
		/*
		 * .refType(refTypeForBuilder) .refUriTemplate(refUriTemplateForBuilder)
		 */
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

	return propertyDefs;
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
//		    valueTypeForBuilder = Optional.of(ldpType);
	    }
	} else {
	    return isEncodedAsFeatureRef(pi) ? Type.FEATURE_REF : ldpType;
	}
    }

    private void addFeatureRefDetailsFromSourcePathInfo(ImmutableFeatureSchema.Builder schemaBuilder,
	    SourcePathInfo spi) {
	if (StringUtils.isNotBlank(spi.refType)) {
	    schemaBuilder.refType(spi.refType);
	}
	if (StringUtils.isNotBlank(spi.refUriTemplate)) {
	    schemaBuilder.refUriTemplate(spi.refUriTemplate);
	}
    }

    private void encodeSourcePathInfosInTypeDefinitionWithFragmentsEnabled(PropertyInfo pi,
	    List<PropertyInfo> alreadyVisitedPiList, List<PropertyInfo> nowVisitedList, ClassInfo currentCi,
	    SourcePathInfo spi, PropertyEncodingContext context, ImmutableFeatureSchema.Builder schemaBuilder,
	    Type ldpType) {

	schemaBuilder.sourcePath(spi.sourcePath);

	if (pi.matches(Ldproxy2Constants.RULE_ALL_LINK_OBJECT_AS_FEATURE_REF)) {
	    addFeatureRefDetailsFromSourcePathInfo(schemaBuilder, spi);
	}

	if (!LdpUtil.isLdproxySimpleType(ldpType) && (pi.categoryOfValue() == Options.DATATYPE)) {

	    /*
	     * Identify actual typeCi from SqlEncodingClassInfos, through inspection of
	     * targetTable.
	     */
	    String targetTable = spi.targetTable;
	    Optional<ClassInfo> optActualTypeCi = sqlProviderHelper.actualTypeClass(targetTable, pi);

	    if (optActualTypeCi.isPresent()) {

		// original typeCi of pi is required for circular dependency check
		ClassInfo originalTypeCi = pi.typeClass();

		// detect circular dependency in the property path
		if (alreadyVisitedPiList.stream().anyMatch(vPi -> vPi.inClass() == originalTypeCi)
			|| originalTypeCi == currentCi) {

		    // the circular dependency is already reported for the fragment case

		} else {

		    ClassInfo actualTypeCi = optActualTypeCi.get();

		    PropertyEncodingContext nextContext = createChildContext(context, actualTypeCi, targetTable);

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
	    PropertyEncodingContext context) {

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

		SortedSet<ClassInfo> typeAndSubtypesSet = new TreeSet<>();
		typeAndSubtypesSet.add(typeCi);
		typeAndSubtypesSet.addAll(typeCi.subtypesInCompleteHierarchy());

		List<ClassInfo> typeAndSubtypes = typeAndSubtypesSet.stream()
			.filter(actualTypeCi -> !actualTypeCi.isAbstract() && LdpInfo.isEncoded(actualTypeCi)
				&& target.mapEntry(actualTypeCi).isEmpty())
			.collect(Collectors.toList());

		for (ClassInfo actualTypeCi : typeAndSubtypes) {

		    // detect circular dependency in the property path -
		    if (alreadyVisitedPiList.stream().anyMatch(vPi -> vPi.inClass() == actualTypeCi)
			    || actualTypeCi == currentCi) {
			/*
			 * circular paths are not supported
			 */
		    } else {

			PropertyEncodingContext nextContext = createChildContext(context, actualTypeCi,
				sqlProviderHelper.databaseTableName(actualTypeCi, false));

			propertyDefinitionsForServiceConfiguration(actualTypeCi, nowVisitedList, nextContext);

			if (bbFeaturesGmlBuilder != null) {
			    bbFeaturesGmlBuilder.register(actualTypeCi);
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
	}
    }

    private PropertyEncodingContext createChildContext(PropertyEncodingContext parentContext, ClassInfo newTypeCi,
	    String newSourceTable) {

	PropertyEncodingContext childContext = new PropertyEncodingContext();
	childContext.setInFragment(parentContext.isInFragment());
	childContext.setType(newTypeCi);
	childContext.setSourceTable(newSourceTable);
	childContext.setParentContext(parentContext);

	return childContext;
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

    private boolean isEncodedWithDirectValueSourcePath(PropertyInfo pi, PropertyEncodingContext context) {

	String typeName = pi.typeInfo().name;
	String typeId = pi.typeInfo().id;
	String encodingRule = pi.encodingRule(Ldproxy2Constants.PLATFORM);

	ProcessMapEntry pme = Ldproxy2Target.mapEntryParamInfos.getMapEntry(typeName, encodingRule);

	if (pme != null && !target.ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme, typeId)) {

	    if (pme.hasTargetType()) {

		if ("GEOMETRY".equalsIgnoreCase(pme.getTargetType())) {

		    // the target only allows and thus assumes max mult = 1
		    return true;

		} else if ("LINK".equalsIgnoreCase(pme.getTargetType())) {

		    return pi.cardinality().maxOccurs == 1;

		} else {

		    // value type is a simple ldproxy type
		    return pi.cardinality().maxOccurs == 1;
		}

	    } else {
		// is checked via target configuration validator (which can be switched off)
		result.addError(msgSource, 118, typeName);
		return true;
	    }
	}

	ClassInfo typeCi = pi.typeClass();

	if (typeCi == null) {

	    MessageContext mc = result.addError(msgSource, 118, typeName);
	    if (mc != null) {
		mc.addDetail(msgSource, 1, pi.fullNameInSchema());
	    }
	    return true;

	} else {

	    if (pi.cardinality().maxOccurs == 1
		    && (pi.categoryOfValue() == Options.ENUMERATION || pi.categoryOfValue() == Options.CODELIST)) {
		return true;
	    } else {
		return false;
	    }
	}
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
