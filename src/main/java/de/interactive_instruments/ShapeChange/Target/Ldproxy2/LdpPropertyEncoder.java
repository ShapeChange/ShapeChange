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
	 * TODO - Should we split the method into cases with fragments enabled
	 * (differentiating between type definition and fragment encoding, maybe also
	 * context type being a data type), and fragments disabled?
	 */
	Collection<PropertyInfo> propsToProcess = (Ldproxy2Target.enableFragments && !context.isInFragment()
		&& context.getType().category() == Options.DATATYPE) ? LdpInfo.propertiesAllInOrder(currentCi)
			: currentCi.properties().values();
//	Collection<PropertyInfo> propsToProcess = currentCi.properties().values();

	for (PropertyInfo pi : propsToProcess) {

	    if (!LdpInfo.isEncoded(pi) || target.isIgnored(pi)) {
		continue;
	    }

	    List<PropertyInfo> nowVisitedList = new ArrayList<>(alreadyVisitedPiList);
	    nowVisitedList.add(pi);

	    ImmutableFeatureSchema.Builder propMemberDefBuilder = new ImmutableFeatureSchema.Builder();

	    Type ldpType = target.ldproxyType(pi);

	    Type typeForBuilder;
	    Optional<Type> valueTypeForBuilder = Optional.empty();

	    if (pi.cardinality().maxOccurs > 1) {
		if (ldpType == Type.OBJECT) {
		    // TODO - JUST FOR TESTING FEATURE_REF
		    typeForBuilder = (target.isMappedToOrImplementedAsLink(pi)
			    && pi.matches(Ldproxy2Constants.RULE_ALL_LINK_OBJECT_AS_FEATURE_REF))
				    ? Type.FEATURE_REF_ARRAY
				    : Type.OBJECT_ARRAY;
		} else if (ldpType == Type.GEOMETRY || pi == identifierPi) {
		    // no array for geometry and identifier properties
		    typeForBuilder = ldpType;
		} else {
		    typeForBuilder = Type.VALUE_ARRAY;
		    valueTypeForBuilder = Optional.of(ldpType);
		}
	    } else {
		// TODO - JUST FOR TESTING FEATURE_REF
		typeForBuilder = (target.isMappedToOrImplementedAsLink(pi)
			&& pi.matches(Ldproxy2Constants.RULE_ALL_LINK_OBJECT_AS_FEATURE_REF)) ? Type.FEATURE_REF
				: ldpType;
	    }

	    Optional<SimpleFeatureGeometry> geometryTypeForBuilder = Optional.empty();
	    if (ldpType == Type.GEOMETRY) {
		geometryTypeForBuilder = geometryType(pi);
	    }

	    Optional<String> unitForBuilder = Optional.empty();
	    if ((ldpType == Type.FLOAT || ldpType == Type.INTEGER) && StringUtils.isNotBlank(Ldproxy2Target.uomTvName)
		    && StringUtils.isNotBlank(pi.taggedValue(Ldproxy2Target.uomTvName))) {
		unitForBuilder = Optional.of(pi.taggedValue(Ldproxy2Target.uomTvName).trim());
	    }

	    Optional<Role> propRole;

	    if (identifierPi != null && !multipleIdentifierPisEncountered && pi == identifierPi) {
		propRole = Optional.of(Role.ID);
	    } else if (defaultGeometryPi != null && !multipleDefaultGeometriesEncountered && pi == defaultGeometryPi) {
		propRole = Optional.of(Role.PRIMARY_GEOMETRY);
	    } else if (defaultInstantPi != null && !multipleDefaultInstantsEncountered && pi == defaultInstantPi) {
		propRole = Optional.of(Role.PRIMARY_INSTANT);
	    } else if (defaultIntervalStartPi != null && !multipleDefaultIntervalStartsEncountered
		    && pi == defaultIntervalStartPi) {
		propRole = Optional.of(Role.PRIMARY_INTERVAL_START);
	    } else if (defaultIntervalEndPi != null && !multipleDefaultIntervalEndsEncountered
		    && pi == defaultIntervalEndPi) {
		propRole = Optional.of(Role.PRIMARY_INTERVAL_END);
	    } else {
		propRole = Optional.empty();
	    }

	    LdpSqlSourcePathInfos sourcePathInfosForProperty = sqlSourcePathProvider.sourcePathPropertyLevel(pi,
		    alreadyVisitedPiList, context);

	    boolean ignoreSourcePathOnPropertyLevel = false;

	    Optional<String> objectTypeForBuilder = Optional.empty();
	    Optional<String> refTypeForBuilder = Optional.empty();
	    Optional<String> refUriTemplateForBuilder = Optional.empty();

	    LinkedHashMap<String, FeatureSchema> propertyMapForBuilder = new LinkedHashMap<>();

	    // handle embedded cases (datatype or link properties)
	    if (target.isMappedToOrImplementedAsLink(pi)) {

		if (pi.matches(Ldproxy2Constants.RULE_ALL_LINK_OBJECT_AS_FEATURE_REF)) {

		    ClassInfo typeCi = pi.typeClass();

		    if (!target.valueTypeIsMapped(pi) && typeCi != null && target.isProcessedType(typeCi)) {
			// the value type must be a type with identity
			refTypeForBuilder = Optional.of(pi.typeInfo().name.toLowerCase(Locale.ENGLISH));
		    } else {
			// the value type is mapped
			refUriTemplateForBuilder = Optional.of(sqlSourcePathProvider.urlTemplateForValueType(pi));
		    }
		    
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

	    } else if (!LdpUtil.isLdproxySimpleType(ldpType)
		    && (pi.categoryOfValue() == Options.DATATYPE /* || pi.categoryOfValue() == Options.UNION */)) {

		/*
		 * 2022-08-25 JE: Handling of unions just like data types deactivated. For the
		 * time being, we keep the approach with type flattening.
		 */

		ClassInfo typeCi = pi.typeClass();

		// detect circular dependency in the property path
		if (alreadyVisitedPiList.stream().anyMatch(vPi -> vPi.inClass() == typeCi) || typeCi == currentCi) {

		    ClassInfo topType = alreadyVisitedPiList.get(0).inClass();

		    MessageContext mc = result.addError(msgSource, 117, topType.name(), propertyPath(nowVisitedList));
		    if (mc != null) {
			mc.addDetail(msgSource, 0, topType.fullNameInSchema());
		    }

		    continue;

		} else {

		    PropertyEncodingContext nextContext = new PropertyEncodingContext();
		    nextContext.setInFragment(context.isInFragment());
		    nextContext.setType(typeCi);
		    nextContext.setSourceTable(sqlProviderHelper.databaseTableName(typeCi, false));
		    nextContext.setParentContext(context);

		    LinkedHashMap<String, FeatureSchema> datatypePropertyDefs = propertyDefinitions(typeCi,
			    nowVisitedList, nextContext);
		    propertyMapForBuilder = datatypePropertyDefs;

		    objectTypeForBuilder = Optional.of(LdpInfo.originalClassName(typeCi));

		    if (bbFeaturesGmlBuilder != null) {
			bbFeaturesGmlBuilder.register(typeCi);
		    }
		}
	    }

	    Optional<String> constantValueForBuilder = Optional.empty();
	    LdpSqlSourcePathInfos sourcePathInfosForBuilder = null;

	    if (StringUtils.isNotBlank(pi.initialValue()) && pi.isReadOnly()
		    && pi.matches(Ldproxy2Constants.RULE_PROP_READONLY)) {

		constantValueForBuilder = Optional.of(constantValue(pi));

	    } else if (!ignoreSourcePathOnPropertyLevel) {

		/*
		 * Encode the source path if either a) fragment encoding is disabled, or b) we
		 * are in a fragment definition, and the source path is for a direct value
		 * access.
		 */

		if (!Ldproxy2Target.enableFragments) {
		    sourcePathInfosForBuilder = sourcePathInfosForProperty;

		    // TODO compute single sourcePath string

		} else if (context.isInFragment()) {
		    if (isEncodedWithDirectValueSourcePath(pi, context)) {

			// TODO compute single sourcePath string
			sourcePathInfosForBuilder = sourcePathInfosForProperty;
		    }
		} else {
		    // in type definition
		    if (!isEncodedWithDirectValueSourcePath(pi, context)) {

			// TODO compute single sourcePath string, or concat/coalesce construct (just get
			// the according schema object)

//			Problem: source path aus SqlEncodingInfos bestimmen; 
//			ggfs. muss concat/coalesce gebildet werden - wie funktioniert das mit Link-Kodierung -> sourcePath und refType oder refUriTemplate angeben
			sourcePathInfosForBuilder = sourcePathInfosForProperty;
		    }
		}

	    }

	    

	    // Generate constraints
	    Optional<ImmutableSchemaConstraints> constraints = Optional.empty();
	    boolean providerConfigConstraintCreated = false;
	    ImmutableSchemaConstraints.Builder constraintsBuilder = new ImmutableSchemaConstraints.Builder();
	    if (pi.cardinality().minOccurs != 0 && !pi.voidable()/* && pi.inClass().category() != Options.UNION */) {
		/*
		 * 2022-08-25 JE: Handling of unions just like data types deactivated. For the
		 * time being, we keep the approach with type flattening.
		 */
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

		if (!Ldproxy2Target.enableFragments || context.isInFragment()) {
		    // Create content for inclusion in service config:
		    ImmutablePropertyTransformation trf = new ImmutablePropertyTransformation.Builder()
			    .codelist(codelistId).build();
		    bbFeaturesHtmlBuilder.addPropertyTransformationToBuildingBlockOfCollectionInServiceConfiguration(
			    nowVisitedList.get(0).inClass(), propertyPath(nowVisitedList), trf);
		}
	    }

	    if (providerConfigConstraintCreated) {
		constraints = Optional.of(constraintsBuilder.build());
	    }

	    /*
	     * Fragment encoding and what is encoded within a type definition are different
	     * things. Within fragments - if enabled at all - we encode the structure of the
	     * represented type. The only exception is the sourcePath, in case that it is
	     * context specific. If it is, then the context specific sourcePath needs to be
	     * encoded in the type definition. Otherwise, we encode the sourcePath in the
	     * fragment. If fragments are not enabled, we encode everything in the type
	     * definition, as usual.
	     */

	    if (Ldproxy2Target.enableFragments && !context.isInFragment()) {

		if (!ignoreSourcePathOnPropertyLevel && !isEncodedWithDirectValueSourcePath(pi, context)) {

		    /*
		     * So fragment encoding is enabled, we are in a type definition, and the
		     * property is not fully defined in the fragment.
		     */

		    if (sourcePathInfosForBuilder != null) {

			if (sourcePathInfosForBuilder.isSingleSourcePath()) {

			    SourcePathInfo spi = sourcePathInfosForBuilder.getSourcePathInfos().get(0);
			    propMemberDefBuilder.sourcePath(spi.sourcePath);

			    if (pi.matches(Ldproxy2Constants.RULE_ALL_LINK_OBJECT_AS_FEATURE_REF)) {
				if (StringUtils.isNotBlank(spi.refType)) {
				    propMemberDefBuilder.refType(spi.refType);
				}
				if (StringUtils.isNotBlank(spi.refUriTemplate)) {
				    propMemberDefBuilder.refUriTemplate(spi.refUriTemplate);
				}
			    }

			    // TODO - also consider data type case
			    if (!LdpUtil.isLdproxySimpleType(ldpType) && (pi
				    .categoryOfValue() == Options.DATATYPE /*
									    * || pi.categoryOfValue() == Options.UNION
									    */)) {

				/*
				 * 2022-08-25 JE: Handling of unions just like data types deactivated. For the
				 * time being, we keep the approach with type flattening.
				 */

				// TODO - identify actual typeCi from SqlEncodingClassInfos, through inspection
				// of targetTable

				String targetTable = spi.targetTable;

				Optional<ClassInfo> optActualTypeCi = sqlProviderHelper.actualTypeClass(targetTable,
					pi);

				if (optActualTypeCi.isPresent()) {

				    // original typeCi of pi is required for circular dependency check
				    ClassInfo originalTypeCi = pi.typeClass();

				    // detect circular dependency in the property path
				    if (alreadyVisitedPiList.stream().anyMatch(vPi -> vPi.inClass() == originalTypeCi)
					    || originalTypeCi == currentCi) {

					// already reported for fragment

//					ClassInfo topType = alreadyVisitedPiList.get(0).inClass();
//
//					MessageContext mc = result.addError(msgSource, 117, topType.name(),
//						propertyPath(nowVisitedList));
//					if (mc != null) {
//					    mc.addDetail(msgSource, 0, topType.fullNameInSchema());
//					}
//
//					continue;

				    } else {

					ClassInfo actualTypeCi = optActualTypeCi.get();

					// TODO - encode schema fragment for actual typeCi

					PropertyEncodingContext nextContext = new PropertyEncodingContext();
					nextContext.setInFragment(context.isInFragment());
					nextContext.setType(actualTypeCi);
					nextContext.setSourceTable(targetTable);
					nextContext.setParentContext(context);

					// TODO does the current approach support data type inheritance, when it
					// gets to actually checking and encoding properties? ... would we not have
					// to reference multiple schema fragments, and thus use a merge?

					LinkedHashMap<String, FeatureSchema> datatypePropertyDefs = propertyDefinitions(
						actualTypeCi, nowVisitedList, nextContext);

//					    ImmutableFeatureSchema.Builder mspPropMemberDefBuilder = new ImmutableFeatureSchema.Builder();
					propMemberDefBuilder.propertyMap(datatypePropertyDefs);

					propMemberDefBuilder
						.objectType(Optional.of(LdpInfo.originalClassName(actualTypeCi)));

					if (bbFeaturesGmlBuilder != null) {
					    bbFeaturesGmlBuilder.register(actualTypeCi);
					}
				    }
				}
			    }

			} else if (sourcePathInfosForBuilder.isMultipleSourcePaths()) {

			    /*
			     * use concat or coalesce build the according schema, with items one by one; for
			     * each item, drill down where necessary (for data type)
			     */

			    List<ImmutableFeatureSchema> itemSchemas = new ArrayList<>();

//			    int index = 0;
			    for (SourcePathInfo spi : sourcePathInfosForBuilder.getSourcePathInfos()) {

				ImmutableFeatureSchema.Builder mspBuilder = new ImmutableFeatureSchema.Builder();

//				mspBuilder.name(pi.name() + index++);
				mspBuilder.name(pi.name() + "_to_"+spi.targetTable);

				mspBuilder.sourcePath(spi.sourcePath);

				if (pi.matches(Ldproxy2Constants.RULE_ALL_LINK_OBJECT_AS_FEATURE_REF)) {
				    if (StringUtils.isNotBlank(spi.refType)) {
					mspBuilder.refType(spi.refType);
				    }
				    if (StringUtils.isNotBlank(spi.refUriTemplate)) {
					mspBuilder.refUriTemplate(spi.refUriTemplate);
				    }
				}

				if (!LdpUtil.isLdproxySimpleType(ldpType)
					&& (pi.categoryOfValue() == Options.DATATYPE /*
										      * || pi.categoryOfValue() ==
										      * Options.UNION
										      */)) {

				    /*
				     * 2022-08-25 JE: Handling of unions just like data types deactivated. For the
				     * time being, we keep the approach with type flattening.
				     */

				    // TODO - identify actual typeCi from SqlEncodingClassInfos, through inspection
				    // of targetTable

				    String targetTable = spi.targetTable;

				    Optional<ClassInfo> optActualTypeCi = sqlProviderHelper.actualTypeClass(targetTable,
					    pi);

				    if (optActualTypeCi.isPresent()) {

					// original typeCi of pi is required for circular dependency check
					ClassInfo originalTypeCi = pi.typeClass();

					// detect circular dependency in the property path
					if (alreadyVisitedPiList.stream()
						.anyMatch(vPi -> vPi.inClass() == originalTypeCi)
						|| originalTypeCi == currentCi) {

					    // already reported for fragment

//					ClassInfo topType = alreadyVisitedPiList.get(0).inClass();
//
//					MessageContext mc = result.addError(msgSource, 117, topType.name(),
//						propertyPath(nowVisitedList));
//					if (mc != null) {
//					    mc.addDetail(msgSource, 0, topType.fullNameInSchema());
//					}
//
//					continue;

					} else {

					    ClassInfo actualTypeCi = optActualTypeCi.get();

					    // TODO - encode schema fragment for actual typeCi

					    PropertyEncodingContext nextContext = new PropertyEncodingContext();
					    nextContext.setInFragment(context.isInFragment());
					    nextContext.setType(actualTypeCi);
					    nextContext.setSourceTable(targetTable);
					    nextContext.setParentContext(context);

					    // TODO does the current approach support data type inheritance, when it
					    // gets to actually checking and encoding properties? ... would we not have
					    // to reference multiple schema fragments, and thus use a merge?

					    LinkedHashMap<String, FeatureSchema> datatypePropertyDefs = propertyDefinitions(
						    actualTypeCi, nowVisitedList, nextContext);

//					    ImmutableFeatureSchema.Builder mspPropMemberDefBuilder = new ImmutableFeatureSchema.Builder();
					    mspBuilder.propertyMap(datatypePropertyDefs);

					    mspBuilder.objectType(Optional.of(LdpInfo.originalClassName(actualTypeCi)));

					    if (bbFeaturesGmlBuilder != null) {
						bbFeaturesGmlBuilder.register(actualTypeCi);
					    }
					}
				    }
				}

				itemSchemas.add(mspBuilder.build());
			    }

			    if (sourcePathInfosForBuilder.concatRequired()) {
				propMemberDefBuilder.concat(itemSchemas);
			    } else {
				// coalesce
				propMemberDefBuilder.coalesce(itemSchemas);
			    }
			}
		    }

		    ImmutableFeatureSchema propMemberDef = propMemberDefBuilder.name(pi.name()).type(typeForBuilder)
			    .valueType(valueTypeForBuilder).build();
		    propertyDefs.put(pi.name(), propMemberDef);
		}

	    } else {

		if (sourcePathInfosForBuilder != null) {

		    if (sourcePathInfosForBuilder.isSingleSourcePath()) {
			SourcePathInfo spi = sourcePathInfosForBuilder.getSourcePathInfos().get(0);
			propMemberDefBuilder.sourcePath(spi.sourcePath);

			if (pi.matches(Ldproxy2Constants.RULE_ALL_LINK_OBJECT_AS_FEATURE_REF)) {
			    if (StringUtils.isNotBlank(spi.refType)) {
				propMemberDefBuilder.refType(spi.refType);
			    }
			    if (StringUtils.isNotBlank(spi.refUriTemplate)) {
				propMemberDefBuilder.refUriTemplate(spi.refUriTemplate);
			    }
			}

		    } else {

			System.out.println("Multiple Source Paths in fragment detected, but not encoded!");

		    }
		}

		ImmutableFeatureSchema propMemberDef = propMemberDefBuilder.name(pi.name()).label(LdpInfo.label(pi))
			.description(LdpInfo.description(pi)).type(typeForBuilder).objectType(objectTypeForBuilder)
			.valueType(valueTypeForBuilder).constraints(constraints).role(propRole)
			.constantValue(constantValueForBuilder).geometryType(geometryTypeForBuilder)
			.unit(unitForBuilder).propertyMap(propertyMapForBuilder)
			/*
			 * .refType(refTypeForBuilder) .refUriTemplate(refUriTemplateForBuilder)
			 */.build();
		propertyDefs.put(pi.name(), propMemberDef);

		// create more service constraint content
		if (StringUtils.isNotBlank(pi.taggedValue("ldpRemove"))) {
		    String tv = pi.taggedValue("ldpRemove").trim().toUpperCase(Locale.ENGLISH);
		    if (tv.equals("IN_COLLECTION") || tv.equals("ALWAYS") || tv.equals("NEVER")) {
			ImmutablePropertyTransformation trf = new ImmutablePropertyTransformation.Builder().remove(tv)
				.build();
			bbFeaturesHtmlBuilder
				.addPropertyTransformationToBuildingBlockOfCollectionInServiceConfiguration(
					nowVisitedList.get(0).inClass(), propertyPath(nowVisitedList), trf);
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
			    nowVisitedList.get(0).inClass(), propertyPath(nowVisitedList), trf);
		}
		if (ldpType == Type.DATETIME && StringUtils.isNotBlank(Ldproxy2Target.dateTimeFormat)) {
		    ImmutablePropertyTransformation trf = new ImmutablePropertyTransformation.Builder()
			    .dateFormat(Ldproxy2Target.dateTimeFormat).build();
		    bbFeaturesHtmlBuilder.addPropertyTransformationToBuildingBlockOfCollectionInServiceConfiguration(
			    nowVisitedList.get(0).inClass(), propertyPath(nowVisitedList), trf);
		}

		if (bbFeaturesGmlBuilder != null) {

		    ClassInfo firstCi = nowVisitedList.get(0).inClass();

		    bbFeaturesGmlBuilder.register(pi, firstCi, propertyPath(nowVisitedList));
		}
	    }
	}

	return propertyDefs;
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

	/*
	 * TODO we need to identify the following cases (that result in context specific
	 * source paths):
	 * 
	 * * (Supertype) property with simple value type and max occurs > 1. It results
	 * in a single source path - for the associative table that is specific to the
	 * context type.
	 * 
	 * * Property with complex value type that is a supertype. Then we need to
	 * identify how many context specific source paths we actually have. It may be
	 * just a single one (depends on sql encoding infos, as well as the type
	 * hierarchy if there are no sql encoding infos [in that case, it depends on how
	 * many concrete types are in the hierarchy]). The property multiplicity is
	 * irrelevant in that case, because even with max mult 1, multiple cases - for
	 * the type and its subtypes - may be relevant.
	 * 
	 * * Property with data type as value type, which is either a) encoded in a
	 * usage specific table (one-to-many-several-tables) or b) requires an
	 * associative table (due to max mult > 1; the associative table is then
	 * specific to the context type).
	 * 
	 * If we have identified more than one context specific source path, it is a
	 * case for concat/coalesce. Otherwise, we can use the direct sourcePath member
	 * within the type definition.
	 * 
	 * NOTE: According to AZ, concat/coalesce does support data types! The content
	 * of these members is just a schema, as can be used in any type definition. It
	 * should thus be possible to refer to fragment definitions - or to use the full
	 * schema directly, for cases in which context specific data type encoding
	 * applies.
	 * 
	 */

//	WEITERMACHEN

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

	}

//	else if (!sqlEncodingInfos.isEmpty()) {
//
//	    // TODO check sql encoding infos: if such infos are available at all, see how
//	    // many apply to the property
//
//	    // TODO the sql encoding infos need to be evaluated in the current context
//	    // (sourceTable that applies to the given property)
//
//	    Set<SqlPropertyEncodingInfo> propEncInfos = sqlEncodingInfos.getPropertyEncodingInfos(pi, context);
//
//	    /*
//	     * TODO we need to differentiate:
//	     * 
//	     * * fragment definition encoding case ->
//	     * 
//	     * ** context type is always equal to pi.inClass -> if the fragment is for a
//	     * supertype, then actual type definitions for subtypes must define the source
//	     * paths for the supertype properties
//	     * 
//	     * * type definition encoding case ->
//	     * 
//	     * ** context type != pi.inClass
//	     * 
//	     * ** context type == pi.inClass the context type may be different to
//	     * pi.inClass()
//	     */
//
//	    /*
//	     * If we have multiple property encodings for the property within the current
//	     * context, we definitely have a context specific source path.
//	     */
//	    return propEncInfos.size() > 1;
//
//	} 
	else {

//	    SortedSet<ClassInfo> allTypeCiSubtypes = typeCi.subtypesInCompleteHierarchy();
//	    allTypeCiSubtypes.add(typeCi);
//	    List<ClassInfo> nonAbstractTypeCis = allTypeCiSubtypes.stream().filter(tci -> !tci.isAbstract())
//		    .collect(Collectors.toList());

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
