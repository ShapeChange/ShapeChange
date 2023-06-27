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

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class LdpPropertyEncoder {

    protected ShapeChangeResult result;
    protected Ldproxy2Target target;
    protected MessageSource msgSource;

    protected LdpSqlProviderHelper sqlProviderHelper = new LdpSqlProviderHelper();

    protected LdpBuildingBlockFeaturesGmlBuilder bbFeaturesGmlBuilder;
    protected LdpBuildingBlockFeaturesHtmlBuilder bbFeaturesHtmlBuilder;

    public LdpPropertyEncoder(Ldproxy2Target target, LdpBuildingBlockFeaturesGmlBuilder gml,
	    LdpBuildingBlockFeaturesHtmlBuilder featuresHtml) {

	this.target = target;
	this.result = target.result;
	this.msgSource = target;

	this.bbFeaturesGmlBuilder = gml;
	this.bbFeaturesHtmlBuilder = featuresHtml;
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

	    for (PropertyInfo pi : currentCi.properties().values()) {

		if (!LdpInfo.isEncoded(pi)) {
		    continue;
		}

		/*
		 * NOTE: No need to differentiate between fragment encoding and non-fragment
		 * encoding here, because in this for-loop only special properties are
		 * determined. The exception is at the end, where an identifier property is
		 * actually added - that shall only occur in fragment encoding mode, if a
		 * fragment is encoded.
		 */

		if (!valueTypeIsMapped(pi)) {

		    if (pi.typeClass() == null || !LdpInfo.isEncoded(pi.typeClass())) {
			MessageContext mc = result.addError(msgSource, 124, pi.typeInfo().name, pi.name(),
				pi.inClass().name());
			if (mc != null) {
			    mc.addDetail(msgSource, 1, pi.fullNameInSchema());
			}
			continue;
		    } else if (LdpInfo.unsupportedCategoryOfValue(pi)) {
			MessageContext mc = result.addError(msgSource, 120, pi.typeInfo().name, pi.name(),
				pi.inClass().name());
			if (mc != null) {
			    mc.addDetail(msgSource, 1, pi.fullNameInSchema());
			}
			continue;
		    }
		}

		if (pi.stereotype("identifier")
			&& currentCi.matches(Ldproxy2Constants.RULE_CLS_IDENTIFIER_STEREOTYPE)) {

		    if (!multipleIdentifierPisEncountered) {
			identifierPi = pi;
			if (pi.cardinality().maxOccurs > 1) {
			    MessageContext mc = result.addWarning(msgSource, 104, currentCi.name(), pi.name());
			    if (mc != null) {
				mc.addDetail(msgSource, 1, pi.fullNameInSchema());
			    }
			}
		    } else {
			multipleIdentifierPisEncountered = true;
			MessageContext mc = result.addError(msgSource, 107, pi.inClass().name(), pi.name(),
				identifierPi.name());
			if (mc != null) {
			    mc.addDetail(msgSource, 1, pi.fullNameInSchema());
			}
		    }

		}

		if (LdpUtil.isTrueIgnoringCase(pi.taggedValue("defaultGeometry"))) {
		    if (!multipleDefaultGeometriesEncountered) {
			defaultGeometryPi = pi;
		    } else {
			multipleDefaultGeometriesEncountered = true;
			MessageContext mc = result.addError(msgSource, 105, pi.inClass().name(), pi.name(),
				defaultGeometryPi.name());
			if (mc != null) {
			    mc.addDetail(msgSource, 1, pi.fullNameInSchema());
			}
		    }
		}

		boolean isDefaultInstant = LdpUtil.isTrueIgnoringCase(pi.taggedValue("defaultInstant"));
		boolean isDefaultIntervalStart = LdpUtil.isTrueIgnoringCase(pi.taggedValue("defaultIntervalStart"));
		boolean isDefaultIntervalEnd = LdpUtil.isTrueIgnoringCase(pi.taggedValue("defaultIntervalEnd"));

		if (isDefaultInstant && (isDefaultIntervalStart || isDefaultIntervalEnd)) {

		    MessageContext mc = result.addError(msgSource, 106, pi.inClass().name(), pi.name());
		    if (mc != null) {
			mc.addDetail(msgSource, 1, pi.fullNameInSchema());
		    }

		} else if (isDefaultInstant) {

		    if (!multipleDefaultInstantsEncountered) {
			defaultInstantPi = pi;
		    } else {
			multipleDefaultInstantsEncountered = true;
			MessageContext mc = result.addError(msgSource, 108, pi.inClass().name(), pi.name(),
				defaultInstantPi.name());
			if (mc != null) {
			    mc.addDetail(msgSource, 1, pi.fullNameInSchema());
			}
		    }

		} else {

		    if (isDefaultIntervalStart && isDefaultIntervalEnd) {

			MessageContext mc = result.addError(msgSource, 109, pi.inClass().name(), pi.name());
			if (mc != null) {
			    mc.addDetail(msgSource, 1, pi.fullNameInSchema());
			}

		    } else if (isDefaultIntervalStart) {

			if (!multipleDefaultIntervalStartsEncountered) {
			    defaultIntervalStartPi = pi;
			} else {
			    multipleDefaultIntervalStartsEncountered = true;
			    MessageContext mc = result.addError(msgSource, 110, pi.inClass().name(), pi.name(),
				    defaultIntervalStartPi.name());
			    if (mc != null) {
				mc.addDetail(msgSource, 1, pi.fullNameInSchema());
			    }
			}

		    } else if (isDefaultIntervalEnd) {
			if (!multipleDefaultIntervalEndsEncountered) {
			    defaultIntervalEndPi = pi;
			} else {
			    multipleDefaultIntervalEndsEncountered = true;
			    MessageContext mc = result.addError(msgSource, 111, pi.inClass().name(), pi.name(),
				    defaultIntervalEndPi.name());
			    if (mc != null) {
				mc.addDetail(msgSource, 1, pi.fullNameInSchema());
			    }
			}
		    }
		}

	    }

	    if ((identifierPi == null || multipleIdentifierPisEncountered)
		    && (!Ldproxy2Target.enableFragments || context.isInFragment())) {

		ImmutableFeatureSchema identifierMemberDef = new ImmutableFeatureSchema.Builder()
			.name(Ldproxy2Target.objectIdentifierName).sourcePath(Ldproxy2Target.primaryKeyColumn)
			.type(Type.INTEGER).role(Role.ID).build();
		propertyDefs.put(identifierMemberDef.getName(), identifierMemberDef);
	    }
	}

	for (PropertyInfo pi : currentCi.properties().values()) {

	    if (!LdpInfo.isEncoded(pi)) {
		continue;
	    }

	    if (!valueTypeIsMapped(pi)) {

		if (pi.typeClass() == null || !LdpInfo.isEncoded(pi.typeClass())) {
		    MessageContext mc = result.addError(msgSource, 124, pi.typeInfo().name, pi.name(),
			    pi.inClass().name());
		    if (mc != null) {
			mc.addDetail(msgSource, 1, pi.fullNameInSchema());
		    }
		    continue;
		} else if (LdpInfo.unsupportedCategoryOfValue(pi)) {
		    MessageContext mc = result.addError(msgSource, 120, pi.typeInfo().name, pi.name(),
			    pi.inClass().name());
		    if (mc != null) {
			mc.addDetail(msgSource, 1, pi.fullNameInSchema());
		    }
		    continue;
		}
	    }

	    List<PropertyInfo> nowVisitedList = new ArrayList<>(alreadyVisitedPiList);
	    nowVisitedList.add(pi);

	    ImmutableFeatureSchema.Builder propMemberDefBuilder = new ImmutableFeatureSchema.Builder();

	    Type ldpType = target.ldproxyType(pi);

	    Type typeForBuilder;
	    Optional<Type> valueTypeForBuilder = Optional.empty();
	    if (pi.cardinality().maxOccurs > 1) {
		if (ldpType == Type.OBJECT) {
		    typeForBuilder = Type.OBJECT_ARRAY;
		} else if (ldpType == Type.GEOMETRY || pi == identifierPi) {
		    // no array for geometry and identifier properties
		    typeForBuilder = ldpType;
		} else {
		    typeForBuilder = Type.VALUE_ARRAY;
		    valueTypeForBuilder = Optional.of(ldpType);
		}
	    } else {
		typeForBuilder = ldpType;
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

	    Optional<String> sourcePathProperty = sourcePathPropertyLevel(pi, alreadyVisitedPiList);

	    boolean ignoreSourcePathOnPropertyLevel = false;

	    Optional<String> objectTypeForBuilder = Optional.empty();
	    LinkedHashMap<String, FeatureSchema> propertyMapForBuilder = new LinkedHashMap<>();

	    // handle embedded cases (datatype or link properties)
	    if (isMappedToOrImplementedAsLink(pi)) {

		// IMPORTANT: The object type must be 'Link', NOT 'LINK'!
		objectTypeForBuilder = Optional.of("Link");

		ignoreSourcePathOnPropertyLevel = omitSourcePathOnPropertyLevelForLinkObjectProperty(pi);

		LinkedHashMap<String, FeatureSchema> linkPropertyDefs = new LinkedHashMap<>();

		ImmutableFeatureSchema.Builder titlePropBuilder = new ImmutableFeatureSchema.Builder().name("title")
			.label(pi.typeInfo().name + "-title").type(Type.STRING);
		List<String> titleSourcePaths = sourcePathsLinkLevelTitle(pi);
		if (titleSourcePaths.size() == 1) {
		    titlePropBuilder = titlePropBuilder.sourcePath(titleSourcePaths.get(0));
		} else {
		    titlePropBuilder = titlePropBuilder.sourcePaths(titleSourcePaths);
		}

		linkPropertyDefs.put("title", titlePropBuilder.build());

		ImmutableFeatureSchema.Builder linkPropHrefBuilder = new ImmutableFeatureSchema.Builder();
		linkPropHrefBuilder.name("href").label(pi.typeInfo().name + "-ID").type(Type.STRING)
			.sourcePath(sourcePathLinkLevelHref(pi));
		linkPropHrefBuilder.addAllTransformationsBuilders(
			new ImmutablePropertyTransformation.Builder().stringFormat(urlTemplateForValueType(pi)));
		linkPropertyDefs.put("href", linkPropHrefBuilder.build());

		propertyMapForBuilder = linkPropertyDefs;

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

		    LinkedHashMap<String, FeatureSchema> datatypePropertyDefs = propertyDefinitions(typeCi,
			    nowVisitedList, context);
		    propertyMapForBuilder = datatypePropertyDefs;

		    objectTypeForBuilder = Optional.of(LdpInfo.originalClassName(typeCi));

		    if (bbFeaturesGmlBuilder != null) {
			bbFeaturesGmlBuilder.register(typeCi);
		    }
		}
	    }

	    Optional<String> constantValueForBuilder = Optional.empty();
	    if (StringUtils.isNotBlank(pi.initialValue()) && pi.isReadOnly()
		    && pi.matches(Ldproxy2Constants.RULE_PROP_READONLY)) {

		constantValueForBuilder = Optional.of(constantValue(pi));

	    } else if (!ignoreSourcePathOnPropertyLevel) {

		/*
		 * Encode the source path if either a) fragment encoding is disabled, or b) we
		 * are in a fragment definition, and the source path is for a direct value
		 * access.
		 */

		if (!Ldproxy2Target.enableFragments
			|| (context.isInFragment() && isEncodedWithDirectValueSourcePath(pi, context))) {
		    propMemberDefBuilder.sourcePath(sourcePathProperty);
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
	    if (LdpInfo.isEnumerationOrCodelistValueType(pi) && !valueTypeIsMapped(pi)) {

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
		    constraintsBuilder.enumValues(enumValues(typeCi));
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

		if (!isEncodedWithDirectValueSourcePath(pi, context)) {

		    /*
		     * So fragment encoding is enabled, we are in a type definition, and the
		     * property is not fully defined in the fragment.
		     */

		    ImmutableFeatureSchema propMemberDef = propMemberDefBuilder.name(pi.name()).type(typeForBuilder)
			    .valueType(valueTypeForBuilder).build();
		    propertyDefs.put(pi.name(), propMemberDef);
		}

	    } else {

		ImmutableFeatureSchema propMemberDef = propMemberDefBuilder.name(pi.name()).label(LdpInfo.label(pi))
			.description(LdpInfo.description(pi)).type(typeForBuilder).objectType(objectTypeForBuilder)
			.valueType(valueTypeForBuilder).constraints(constraints).role(propRole)
			.constantValue(constantValueForBuilder).geometryType(geometryTypeForBuilder)
			.unit(unitForBuilder).propertyMap(propertyMapForBuilder).build();
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

    private Iterable<String> enumValues(ClassInfo enumeration) {

	List<String> res = new ArrayList<>();

	for (PropertyInfo pi : enumeration.properties().values()) {
	    if (StringUtils.isNotBlank(pi.initialValue())) {
		res.add(pi.initialValue().trim());
	    } else {
		res.add(pi.name());
	    }
	}

	return res;
    }

    private String propertyPath(List<PropertyInfo> propertyList) {
	return propertyList.stream().map(pi -> pi.name()).collect(Collectors.joining("."));
    }

    private List<String> sourcePathsLinkLevelTitle(PropertyInfo pi) {

	List<String> result = new ArrayList<>();

	if (!isMappedToLink(pi) && valueTypeHasValidLdpTitleAttributeTag(pi)) {

	    PropertyInfo titleAtt = getTitleAttribute(pi.typeClass());
	    if (titleAtt.cardinality().minOccurs == 0) {
		/*
		 * PK field shall be listed first, since the last listed sourcePaths "wins", and
		 * that should be the title attribute, if it exists
		 */
		result.add(Ldproxy2Target.primaryKeyColumn);
	    }
	    result.add(databaseColumnName(titleAtt));

	} else {

	    if (pi.cardinality().maxOccurs == 1) {
		result.add(databaseColumnName(pi));
	    } else {
		result.add(Ldproxy2Target.primaryKeyColumn);
	    }
	}

	return result;
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

    private String urlTemplateForValueType(PropertyInfo pi) {

	String urlTemplate = null;

	String valueTypeName = pi.typeInfo().name;

	ProcessMapEntry pme = Ldproxy2Target.mapEntryParamInfos.getMapEntry(valueTypeName,
		pi.encodingRule(Ldproxy2Constants.PLATFORM));

	if (pme != null && !target.ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme, pi.typeInfo().id)
		&& "LINK".equalsIgnoreCase(pme.getTargetType())) {

	    urlTemplate = Ldproxy2Target.mapEntryParamInfos.getCharacteristic(valueTypeName,
		    pi.encodingRule(Ldproxy2Constants.PLATFORM), Ldproxy2Constants.ME_PARAM_LINK_INFOS,
		    Ldproxy2Constants.ME_PARAM_LINK_INFOS_CHARACT_URL_TEMPLATE);

	    urlTemplate = urlTemplate.replaceAll("\\(value\\)", "{{value}}").replaceAll("\\(serviceUrl\\)",
		    "{{serviceUrl}}");
	}

	if (StringUtils.isBlank(urlTemplate)) {
	    urlTemplate = "{{serviceUrl}}/collections/" + valueTypeName.toLowerCase(Locale.ENGLISH)
		    + "/items/{{value}}";
	}

	return urlTemplate;
    }

    private String sourcePathLinkLevelHref(PropertyInfo pi) {

	if (pi.cardinality().maxOccurs == 1) {

	    if (!isMappedToLink(pi) && valueTypeHasValidLdpTitleAttributeTag(pi)) {
		return Ldproxy2Target.primaryKeyColumn;
	    } else {
		return databaseColumnName(pi);
	    }
	} else {
	    return Ldproxy2Target.primaryKeyColumn;
	}

    }

    /**
     * @param pi
     * @return <code>true</code>, if the maximum multiplicity of pi is 1 and either
     *         the value type of pi is mapped to LINK or the value type does NOT
     *         have a valid value for tag ldpTitleAttribute; otherwise
     *         <code>false</code>
     */
    private boolean omitSourcePathOnPropertyLevelForLinkObjectProperty(PropertyInfo pi) {

	return (pi.cardinality().maxOccurs == 1 && (isMappedToLink(pi) || !valueTypeHasValidLdpTitleAttributeTag(pi)));
    }

    private boolean valueTypeHasValidLdpTitleAttributeTag(PropertyInfo pi) {

	ClassInfo typeCi = pi.typeClass();
	if (typeCi == null) {
	    return false;
	} else {
	    PropertyInfo titleAtt = getTitleAttribute(typeCi);
	    return titleAtt != null;
	}
    }

    /**
     * @param ci - tbd
     * @return the attribute of ci whose name is equal to the value of tag
     *         ldpTitleAttribute on ci
     */
    private PropertyInfo getTitleAttribute(ClassInfo ci) {

	PropertyInfo result = null;

	String titleAttName = ci.taggedValue("ldpTitleAttribute");
	if (StringUtils.isNotBlank(titleAttName)) {
	    result = ci.property(titleAttName.trim());
	}

	return result;
    }

    private boolean valueTypeIsMapped(PropertyInfo pi) {

	return valueTypeIsMapped(pi.typeInfo().name, pi.typeInfo().id, pi.encodingRule(Ldproxy2Constants.PLATFORM));
    }

    private boolean valueTypeIsMapped(String typeName, String typeId, String encodingRule) {

	ProcessMapEntry pme = Ldproxy2Target.mapEntryParamInfos.getMapEntry(typeName, encodingRule);

	if (pme != null && !target.ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme, typeId)) {
	    return true;
	} else {
	    return false;
	}
    }

    private boolean isMappedToOrImplementedAsLink(PropertyInfo pi) {

	if (valueTypeIsMapped(pi)) {
	    return isMappedToLink(pi);
	} else {
	    return LdpInfo.isTypeWithIdentityValueType(pi);
	}
    }

    private boolean isMappedToLink(PropertyInfo pi) {

	return isMappedToLink(pi.typeInfo().name, pi.typeInfo().id, pi.encodingRule(Ldproxy2Constants.PLATFORM));
    }

    private boolean isMappedToLink(String typeName, String typeId, String encodingRule) {

	ProcessMapEntry pme = Ldproxy2Target.mapEntryParamInfos.getMapEntry(typeName, encodingRule);

	if (pme != null && !target.ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme, typeId)
		&& pme.hasTargetType() && "LINK".equalsIgnoreCase(pme.getTargetType())) {
	    return true;
	} else {
	    return false;
	}
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

    /**
     * @param pi                   the property for which to construct the source
     *                             path on property level
     * @param alreadyVisitedPiList information about previous steps in the source
     *                             path; can be analyzed to detect special cases
     *                             (e.g. lists of data type valued properties)
     * @return
     */
    private Optional<String> sourcePathPropertyLevel(PropertyInfo pi, List<PropertyInfo> alreadyVisitedPiList) {

	String typeName = pi.typeInfo().name;
	String typeId = pi.typeInfo().id;
	String encodingRule = pi.encodingRule(Ldproxy2Constants.PLATFORM);

	ProcessMapEntry pme = Ldproxy2Target.mapEntryParamInfos.getMapEntry(typeName, encodingRule);

	if (pme != null && !target.ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme, typeId)) {

	    if (pme.hasTargetType()) {

		if ("GEOMETRY".equalsIgnoreCase(pme.getTargetType())) {

		    // the target only allows and thus assumes max mult = 1
		    return Optional.of(databaseColumnName(pi));

		} else if ("LINK".equalsIgnoreCase(pme.getTargetType())) {

		    if (pi.cardinality().maxOccurs == 1) {
			return Optional.empty();
		    } else {
			return Optional.of("[" + primaryKeyColumn(pi.inClass()) + "="
				+ sqlProviderHelper.databaseTableName(pi.inClass(), true) + "]"
				+ associativeTableName(pi, alreadyVisitedPiList));
		    }

		} else {

		    // value type is a simple ldproxy type
		    if (pi.cardinality().maxOccurs == 1) {

			return Optional.of(databaseColumnName(pi));

		    } else {

			String sortKeyAddition = "{sortKey=" + sqlProviderHelper.databaseTableName(pi.inClass(), true)
				+ "}";
			if (pi.matches(Ldproxy2Constants.RULE_ALL_ASSOCIATIVETABLES_WITH_SEPARATE_PK_FIELD)) {
			    sortKeyAddition = "";
			}
			return Optional.of("[" + primaryKeyColumn(pi.inClass()) + "="
				+ sqlProviderHelper.databaseTableName(pi.inClass(), true) + "]"
				+ associativeTableName(pi, alreadyVisitedPiList) + sortKeyAddition + "/"
				+ databaseColumnName(pi));
		    }
		}

	    } else {
		// is checked via target configuration validator (which can be switched off)
		result.addError(msgSource, 118, typeName);
		return Optional.of("FIXME");
	    }
	}

	ClassInfo typeCi = pi.typeClass();

	if (typeCi == null) {

	    MessageContext mc = result.addError(msgSource, 118, typeName);
	    if (mc != null) {
		mc.addDetail(msgSource, 1, pi.fullNameInSchema());
	    }
	    return Optional.of("FIXME");

	} else {

	    if (typeCi.category() == Options.ENUMERATION || typeCi.category() == Options.CODELIST) {

		if (pi.cardinality().maxOccurs == 1) {

		    /*
		     * Note: Addition of code list foreign key suffix is handled in method
		     * databaseColumnName(..).
		     */
		    return Optional.of(databaseColumnName(pi));

		} else {

		    String sortKeyAddition = "{sortKey=" + sqlProviderHelper.databaseTableName(pi.inClass(), true)
			    + "}";
		    if (pi.matches(Ldproxy2Constants.RULE_ALL_ASSOCIATIVETABLES_WITH_SEPARATE_PK_FIELD)) {
			sortKeyAddition = "";
		    }

		    String path1 = "[" + primaryKeyColumn(pi.inClass()) + "="
			    + sqlProviderHelper.databaseTableName(pi.inClass(), true) + "]"
			    + associativeTableName(pi, alreadyVisitedPiList) + sortKeyAddition + "/";
		    String path2;
		    if (typeCi.category() == Options.CODELIST
			    && typeCi.matches(Ldproxy2Constants.RULE_CLS_CODELIST_BY_TABLE)) {
			path2 = sqlProviderHelper.databaseTableName(typeCi, true);
		    } else {
			path2 = databaseColumnName(pi);
		    }

		    String path = path1 + path2;
		    return Optional.of(path);
		}

	    } else if (typeCi.category() == Options.DATATYPE) {

		if (typeCi.matches(Ldproxy2Constants.RULE_CLS_DATATYPES_ONETOMANY_SEVERAL_TABLES)) {

		    return Optional.of("[" + primaryKeyColumn(pi.inClass()) + "="
			    + sqlProviderHelper.databaseTableName(pi.inClass(), true) + "]"
			    + associativeTableName(pi, alreadyVisitedPiList));

		} else {

		    if (pi.cardinality().maxOccurs == 1) {

			return Optional.of("[" + databaseColumnName(pi) + "=" + primaryKeyColumn(typeCi) + "]"
				+ sqlProviderHelper.databaseTableName(typeCi, false));
		    } else {

			return Optional.of("[" + primaryKeyColumn(pi.inClass()) + "="
				+ sqlProviderHelper.databaseTableName(pi.inClass(), true) + "]"
				+ associativeTableName(pi, alreadyVisitedPiList) + "/["
				+ sqlProviderHelper.databaseTableName(typeCi, true) + "=" + primaryKeyColumn(typeCi)
				+ "]" + sqlProviderHelper.databaseTableName(typeCi, false));
		    }
		}

	    } else {

		if (pi.reverseProperty() != null && pi.reverseProperty().isNavigable()) {

		    // bi-directional association
		    if (pi.cardinality().maxOccurs > 1 && pi.reverseProperty().cardinality().maxOccurs > 1) {

			// n:m

			if (isReflexive(pi)) {

			    return Optional.of("[" + primaryKeyColumn(pi.inClass()) + "="
				    + sqlProviderHelper.databaseTableName(pi.inClass(), false) + "_"
				    + databaseColumnNameReflexiveProperty(pi.reverseProperty(), true) + "]"
				    + associativeTableName(pi, alreadyVisitedPiList) + "/["
				    + sqlProviderHelper.databaseTableName(typeCi, false) + "_"
				    + databaseColumnNameReflexiveProperty(pi, true) + "="
				    + primaryKeyColumn(pi.inClass()) + "]"
				    + sqlProviderHelper.databaseTableName(typeCi, false));

			} else {

			    return Optional.of("[" + primaryKeyColumn(pi.inClass()) + "="
				    + sqlProviderHelper.databaseTableName(pi.inClass(), true) + "]"
				    + associativeTableName(pi, alreadyVisitedPiList) + "/["
				    + sqlProviderHelper.databaseTableName(typeCi, true) + "=" + primaryKeyColumn(typeCi)
				    + "]" + sqlProviderHelper.databaseTableName(typeCi, false));
			}

		    } else if (pi.cardinality().maxOccurs > 1) {

			// n:1

			if (isReflexive(pi)) {

			    // no need for a table join in this case
			    // case p2 from ppt image (n:1 for bi-directional reflexive association)
			    return Optional.of("[" + primaryKeyColumn(pi.inClass()) + "="
				    + databaseColumnNameReflexiveProperty(pi.reverseProperty(), false) + "]"
				    + sqlProviderHelper.databaseTableName(typeCi, false));

			} else {

			    // case pB from ppt image (n:1 for bi-directional association)
			    return Optional.of("[" + primaryKeyColumn(pi.inClass()) + "="
				    + databaseColumnName(pi.reverseProperty()) + "]"
				    + sqlProviderHelper.databaseTableName(typeCi, false));
			}

		    } else if (pi.reverseProperty().cardinality().maxOccurs > 1) {

			// n:1

			if (isReflexive(pi)) {

			    // no need for a table join in this case
			    // case p1 from ppt image (n:1 for bi-directional reflexive association)
			    return Optional.of(databaseColumnNameReflexiveProperty(pi, false));

			} else {

			    // case pA from ppt image (n:1 for bi-directional association)
			    return Optional.of("[" + databaseColumnName(pi) + "=" + primaryKeyColumn(typeCi) + "]"
				    + sqlProviderHelper.databaseTableName(typeCi, false));
			}

		    } else {

			// 1:1

			if (isReflexive(pi)) {

			    // no need for a table join in this case
			    return Optional.of(databaseColumnNameReflexiveProperty(pi, false));

			} else {

			    // max mult = 1 on both ends
			    return Optional.of("[" + databaseColumnName(pi) + "=" + primaryKeyColumn(typeCi) + "]"
				    + sqlProviderHelper.databaseTableName(typeCi, false));
			}
		    }

		} else {

		    // attribute or uni-directional association
		    if (pi.cardinality().maxOccurs == 1) {

			// n:1

			if (isReflexive(pi)) {

			    // no need for a table join in this case
			    return Optional.of(databaseColumnNameReflexiveProperty(pi, false));

			} else {

			    return Optional.of("[" + databaseColumnName(pi) + "=" + primaryKeyColumn(typeCi) + "]"
				    + sqlProviderHelper.databaseTableName(typeCi, false));
			}

		    } else {

			// n:m

			if (isReflexive(pi)) {

			    return Optional.of("[" + primaryKeyColumn(pi.inClass()) + "="
				    + sqlProviderHelper.databaseTableName(pi.inClass(), false) + "_"
				    + databaseColumnNameReflexiveProperty(pi.reverseProperty(), true) + "]"
				    + associativeTableName(pi, alreadyVisitedPiList) + "/["
				    + sqlProviderHelper.databaseTableName(typeCi, false) + "_"
				    + databaseColumnNameReflexiveProperty(pi, true) + "="
				    + primaryKeyColumn(pi.inClass()) + "]"
				    + sqlProviderHelper.databaseTableName(typeCi, false));

			} else {

			    return Optional.of("[" + primaryKeyColumn(pi.inClass()) + "="
				    + sqlProviderHelper.databaseTableName(pi.inClass(), true) + "]"
				    + associativeTableName(pi, alreadyVisitedPiList) + "/["
				    + sqlProviderHelper.databaseTableName(typeCi, true) + "=" + primaryKeyColumn(typeCi)
				    + "]" + sqlProviderHelper.databaseTableName(typeCi, false));
			}
		    }
		}
	    }
	}
    }

    private String databaseColumnName(PropertyInfo pi) {

	String suffix = "";

	Type t = target.ldproxyType(pi);

	if (!(LdpUtil.isLdproxySimpleType(t) || LdpUtil.isLdproxyGeometryType(t))) {

	    if (LdpInfo.valueTypeIsTypeWithIdentity(pi)) {
		suffix = suffix + Ldproxy2Target.foreignKeyColumnSuffix;
	    } else if (pi.categoryOfValue() == Options.DATATYPE) {
		suffix = suffix + Ldproxy2Target.foreignKeyColumnSuffixDatatype;
	    }

	} else if (pi.categoryOfValue() == Options.CODELIST && Ldproxy2Target.model.classByIdOrName(pi.typeInfo())
		.matches(Ldproxy2Constants.RULE_CLS_CODELIST_BY_TABLE)) {

	    // Support SqlDdl target parameter foreignKeyColumnSuffixCodelist
	    suffix = suffix + Ldproxy2Target.foreignKeyColumnSuffixCodelist;
	}

	return databaseColumnName(pi, suffix);
    }

    private String databaseColumnName(PropertyInfo pi, String suffix) {

	String result = pi.name();

	result = result + suffix;

	result = result.toLowerCase(Locale.ENGLISH);

	result = StringUtils.substring(result, 0, Ldproxy2Target.maxNameLength);

	return result;
    }

    private String primaryKeyColumn(ClassInfo ci) {

	if (ci.matches(Ldproxy2Constants.RULE_CLS_IDENTIFIER_STEREOTYPE)) {
	    for (PropertyInfo pi : ci.properties().values()) {
		if (pi.stereotype("identifier")) {
		    return databaseColumnName(pi);
		}
	    }
	}

	return Ldproxy2Target.primaryKeyColumn;
    }

    private boolean isReflexive(PropertyInfo pi) {
	return pi.inClass().id().equals(pi.typeInfo().id);
    }

    private String databaseColumnNameReflexiveProperty(PropertyInfo pi, boolean inAssociativeTable) {

	String suffix = "";

	if (LdpInfo.valueTypeIsTypeWithIdentity(pi)) {

	    if (inAssociativeTable) {
		suffix = suffix + Ldproxy2Target.associativeTableColumnSuffix;
	    } else {
		suffix = suffix + Ldproxy2Target.foreignKeyColumnSuffix;
	    }

	} else if (pi.categoryOfValue() == Options.DATATYPE) {
	    suffix = suffix + Ldproxy2Target.foreignKeyColumnSuffixDatatype;
	}

	return databaseColumnName(pi, suffix);
    }

    private String associativeTableName(PropertyInfo pi, List<PropertyInfo> alreadyVisitedPiList) {

	String result = null;

	/*
	 * Check case of usage specific data type table first. We need to create table
	 * names as are created by the SqlDdl target for
	 * rule-sql-cls-data-types-oneToMany-severalTables. That is the case if the
	 * owner of pi is a data type that matches that rule.
	 */
	if (isEncodedInUsageSpecificDataTypeTable(pi)) {

	    /*
	     * We need to follow the list of already visited properties from the end along
	     * all properties owned by complex data types in order to construct the table
	     * name.
	     */
	    String suffix = "_" + pi.name();
	    String tableName = null;

	    for (int i = alreadyVisitedPiList.size() - 1; i >= 0; i--) {

		PropertyInfo previousPi = alreadyVisitedPiList.get(i);
		ClassInfo prevPiOwnerCi = previousPi.inClass();

		/*
		 * We also gather the name of the first property (looked at from the end of the
		 * list of already visited properties) which is not owned by a complex data
		 * type. That is why the suffix modification is not part of the following
		 * if-else-test.
		 */
		suffix = "_" + previousPi.name() + suffix;

		if (prevPiOwnerCi != null && prevPiOwnerCi.category() == Options.DATATYPE
			&& Ldproxy2Target.model.isInSelectedSchemas(prevPiOwnerCi)
			&& target.mapEntry(prevPiOwnerCi).isEmpty()
			&& prevPiOwnerCi.matches(Ldproxy2Constants.RULE_CLS_DATATYPES_ONETOMANY_SEVERAL_TABLES)) {
		    /*
		     * As long as the owner of the currently visited property is a complex data type
		     * that matches the criteria for creation of several tables, we continue
		     * iterating through the list of previous properties.
		     */
		} else {
		    tableName = previousPi.inClass().name();
		    break;
		}
	    }

	    result = tableName + suffix;

	} else {

	    if (StringUtils.isNotBlank(pi.taggedValue("associativeTable"))) {
		result = pi.taggedValue("associativeTable");
	    } else if (pi.association() != null
		    && StringUtils.isNotBlank(pi.association().taggedValue("associativeTable"))) {
		result = pi.association().taggedValue("associativeTable");
	    } else {

		// tag associativeTable not set or without value -> proceed

		String tableNamePi = determineTableName(pi);

		if (pi.isAttribute() || pi.reverseProperty() == null || !pi.reverseProperty().isNavigable()) {

		    result = tableNamePi;

		} else {

		    // both pi and its reverseProperty are navigable

		    // choose name based on alphabetical order
		    // take into account the case of a reflexive association
		    String tableNameRevPi = determineTableName(pi.reverseProperty());

		    if (tableNamePi.compareTo(tableNameRevPi) <= 0) {
			result = tableNamePi;
		    } else {
			result = tableNameRevPi;
		    }
		}
	    }
	}

	result = StringUtils.substring(result, 0, Ldproxy2Target.maxNameLength);
	return result;
    }

    private boolean isEncodedInUsageSpecificDataTypeTable(PropertyInfo pi) {

	ClassInfo piOwnerCi = pi.inClass();
	return (piOwnerCi != null && piOwnerCi.category() == Options.DATATYPE
		&& Ldproxy2Target.model.isInSelectedSchemas(piOwnerCi) && target.mapEntry(piOwnerCi).isEmpty()
		&& piOwnerCi.matches(Ldproxy2Constants.RULE_CLS_DATATYPES_ONETOMANY_SEVERAL_TABLES));
    }

    private String determineTableName(PropertyInfo pi) {

	String tableName = pi.inClass().name();
	String propertyName = pi.name();
	String res = tableName + "_" + propertyName;
	return res;
    }

}
