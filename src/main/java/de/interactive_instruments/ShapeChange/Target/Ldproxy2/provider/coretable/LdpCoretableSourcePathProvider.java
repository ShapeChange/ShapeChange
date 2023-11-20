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
package de.interactive_instruments.ShapeChange.Target.Ldproxy2.provider.coretable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.Ldproxy2.LdpInfo;
import de.interactive_instruments.ShapeChange.Target.Ldproxy2.LdpPropertyEncodingContext;
import de.interactive_instruments.ShapeChange.Target.Ldproxy2.LdpSourcePathInfo;
import de.interactive_instruments.ShapeChange.Target.Ldproxy2.LdpSourcePathInfos;
import de.interactive_instruments.ShapeChange.Target.Ldproxy2.LdpSpecialPropertiesInfo;
import de.interactive_instruments.ShapeChange.Target.Ldproxy2.Ldproxy2Constants;
import de.interactive_instruments.ShapeChange.Target.Ldproxy2.Ldproxy2Target;
import de.interactive_instruments.ShapeChange.Target.Ldproxy2.provider.AbstractLdpSourcePathProvider;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class LdpCoretableSourcePathProvider extends AbstractLdpSourcePathProvider {

    protected ShapeChangeResult result;
    protected Ldproxy2Target target;
    protected MessageSource msgSource;

    public LdpCoretableSourcePathProvider(Ldproxy2Target target) {

	this.target = target;
	this.result = target.result;
	this.msgSource = target;
    }

    /**
     * @param pi                   the property for which to construct the source
     *                             path on property level
     * @param alreadyVisitedPiList information about previous steps in the source
     *                             path; can be analyzed to detect special cases
     *                             (e.g. lists of data type valued properties)
     * @param context              - The context in which the property is encoded
     * @return - TBD
     */
    @Override
    public LdpSourcePathInfos sourcePathPropertyLevel(PropertyInfo pi, List<PropertyInfo> alreadyVisitedPiList,
	    LdpPropertyEncodingContext context) {

	LdpSourcePathInfos spRes = new LdpSourcePathInfos();
	spRes.setPi(pi);
	spRes.setContext(context);

	String typeName = pi.typeInfo().name;
	String typeId = pi.typeInfo().id;
	String encodingRule = pi.encodingRule(Ldproxy2Constants.PLATFORM);
	ProcessMapEntry pme = Ldproxy2Target.mapEntryParamInfos.getMapEntry(typeName, encodingRule);

	Optional<String> idSourcePath = Optional.empty();
	Optional<String> valueSourcePath = Optional.of(pi.name());
	if (alreadyVisitedPiList.isEmpty()
		&& (LdpInfo.isTypeWithIdentity(pi.inClass()) || pi.inClass().category() == Options.MIXIN)) {
	    valueSourcePath = Optional.of("[JSON]properties/" + valueSourcePath.get());
	}
	Optional<Type> idValueType = Optional.empty();
	String refType = null;
	String refUriTemplate = null;
	boolean targetsSingleValue = pi.cardinality().maxOccurs == 1;

	boolean featureRefWithMultiCaseEncountered = false;

	LdpSpecialPropertiesInfo specialPropInfos = this.target.specialPropertiesInfo(pi.inClass());

	if (pme != null && !target.ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme, typeId)) {

	    // property type is mapped

	    if (pme.hasTargetType()) {

		if ("GEOMETRY".equalsIgnoreCase(pme.getTargetType())) {

		    // the target only allows and thus assumes max mult = 1

		    targetsSingleValue = true;

		    // special handling for primary geometry
		    if (specialPropInfos != null && specialPropInfos.getDefaultGeometryPiOfCi() == pi) {
			valueSourcePath = Optional.of(Ldproxy2Target.coretableGeometryColumn);
		    } else {
			// value source path is just the property name
		    }

		} else if ("LINK".equalsIgnoreCase(pme.getTargetType())) {

		    // only feature ref encoding is supported in the coretable approach

		    refUriTemplate = urlTemplateForValueType(pi);
		    idValueType = Optional.of(Ldproxy2Target.coretableIdColumnLdproxyType);

		    idSourcePath = featureRefIdSourcePath(pi);

		    /*
		     * TODO unclear how url template and variable collection id should work in the
		     * coretable approach
		     * 
		     * TODO Handle case of inheritance for the property value type (differentiate
		     * mapped and non-mapped subtypes).
		     */

		} else {

		    // property type is mapped to a simple ldproxy type

		    // value source path is just the property name
		}

	    } else {
		// is checked via target configuration validator (which can be switched off)
		result.addError(msgSource, 118, typeName);
		valueSourcePath = Optional.of("FIXME");
	    }

	} else {

	    // property type is NOT mapped

	    ClassInfo typeCi = pi.typeClass();

	    if (typeCi == null) {

		MessageContext mc = result.addError(msgSource, 118, typeName);
		if (mc != null) {
		    mc.addDetail(msgSource, 1, pi.fullNameInSchema());
		}
		valueSourcePath = Optional.of("FIXME");

	    } else {

		if (typeCi.category() == Options.ENUMERATION || typeCi.category() == Options.CODELIST) {

		    // value source path is just the property name

		} else if (typeCi.category() == Options.DATATYPE) {

		    // value source path is just the property name

		} else {

		    // property type is type with identity

		    SortedSet<String> collectionIds = collectionIds(pi);

		    idSourcePath = featureRefIdSourcePath(pi);
		    valueSourcePath = featureRefValueSourcePath(pi);

		    idValueType = Optional.of(Ldproxy2Target.coretableIdColumnLdproxyType);

		    if (collectionIds.size() == 1) {

			refType = collectionIds.first();

		    } else {

			featureRefWithMultiCaseEncountered = true;

			// TBD: DIFFERENTIATE CASES WITH/OUT URI TEMPLATE?

			for (String collectionId : collectionIds) {

			    LdpSourcePathInfo spi = new LdpSourcePathInfo(idSourcePath, valueSourcePath, idValueType,
				    collectionId, null, targetsSingleValue);

			    spRes.addSourcePathInfo(spi);
			}
		    }
		}
	    }
	}

	if ((valueSourcePath.isPresent() || idSourcePath.isPresent()) && !featureRefWithMultiCaseEncountered) {

	    LdpSourcePathInfo spi = new LdpSourcePathInfo(idSourcePath, valueSourcePath, idValueType, refType,
		    refUriTemplate, targetsSingleValue);

	    spRes.addSourcePathInfo(spi);
	}

	return spRes;
    }

    private Optional<String> featureRefIdSourcePath(PropertyInfo pi) {

	String idSourcePath;
	if (pi.inClass().category() == Options.DATATYPE) {

	    // does not really work in coretable approach

	    // NOTE: Such a situation would actually not be compliant to ISO 19109

	    // untested attempt: pi.name() + (pi.cardinality().maxOccurs > 1 ? "/featureId"
	    // : "");

	    MessageContext mc = result.addWarning(msgSource, 136, pi.name(), pi.inClass().name());
	    if (mc != null) {
		mc.addDetail(msgSource, 1, pi.fullNameInSchema());
	    }
	    idSourcePath = "FIXME";

	} else if (Ldproxy2Target.coretableRefRelations.contains(pi.name())) {

	    idSourcePath = Ldproxy2Target.coretableRefColumn;

	} else {

	    // lookup via separate reference table

	    String refTableSourceIdColumn;
	    String refTableTargetIdColumn;
	    String relColumnName;

	    if (pi.isAttribute() || !pi.association().isBiDirectional() || pi.association().end2() == pi) {

		refTableSourceIdColumn = Ldproxy2Target.coretableIdColumn;
		refTableTargetIdColumn = Ldproxy2Target.coretableRefColumn;
		relColumnName = Ldproxy2Target.coretableRelationNameColumn;

	    } else {

		refTableTargetIdColumn = Ldproxy2Target.coretableIdColumn;
		refTableSourceIdColumn = Ldproxy2Target.coretableRefColumn;
		relColumnName = Ldproxy2Target.coretableInverseRelationNameColumn;

	    }

	    idSourcePath = "[" + Ldproxy2Target.coretableIdColumn + "=" + refTableSourceIdColumn + "]"
		    + Ldproxy2Target.coretableRelationsTable + "{filter=" + relColumnName + "='" + pi.name() + "'}/"
		    + refTableTargetIdColumn;
	}

	return Optional.ofNullable(idSourcePath);
    }

    private Optional<String> featureRefValueSourcePath(PropertyInfo pi) {

	String valueSourcePath;
	if (pi.inClass().category() == Options.DATATYPE) {

	    // does not really work in coretable approach

	    // NOTE: Such a situation would actually not be compliant to ISO 19109

	    MessageContext mc = result.addWarning(msgSource, 136, pi.name(), pi.inClass().name());
	    if (mc != null) {
		mc.addDetail(msgSource, 1, pi.fullNameInSchema());
	    }
	    valueSourcePath = "FIXME";

	} else if (Ldproxy2Target.coretableRefRelations.contains(pi.name())) {

	    valueSourcePath = "[" + Ldproxy2Target.coretableRefColumn + "=" + Ldproxy2Target.coretableIdColumn + "]"
		    + Ldproxy2Target.coretable;

	} else {

	    // lookup via separate reference table

	    String refTableSourceIdColumn;
	    String refTableTargetIdColumn;
	    String relColumnName;

	    if (pi.isAttribute() || !pi.association().isBiDirectional() || pi.association().end2() == pi) {

		refTableSourceIdColumn = Ldproxy2Target.coretableIdColumn;
		refTableTargetIdColumn = Ldproxy2Target.coretableRefColumn;
		relColumnName = Ldproxy2Target.coretableRelationNameColumn;

	    } else {

		refTableTargetIdColumn = Ldproxy2Target.coretableIdColumn;
		refTableSourceIdColumn = Ldproxy2Target.coretableRefColumn;
		relColumnName = Ldproxy2Target.coretableInverseRelationNameColumn;

	    }

	    valueSourcePath = "[" + Ldproxy2Target.coretableIdColumn + "=" + refTableSourceIdColumn + "]"
		    + Ldproxy2Target.coretableRelationsTable + "{filter=" + relColumnName + "='" + pi.name() + "'}/["
		    + refTableTargetIdColumn + "=" + Ldproxy2Target.coretableIdColumn + "]" + Ldproxy2Target.coretable;
	}

	return Optional.ofNullable(valueSourcePath);
    }

    public List<String> sourcePathsLinkLevelTitle(PropertyInfo pi) {

	List<String> result = new ArrayList<>();

	if (!target.isMappedToLink(pi) && LdpInfo.valueTypeHasValidLdpTitleAttributeTag(pi)) {

	    PropertyInfo titleAtt = LdpInfo.getTitleAttribute(pi.typeClass());
	    if (titleAtt.cardinality().minOccurs == 0) {
		/*
		 * id column of coretable shall be listed first, since the last listed
		 * sourcePaths "wins", and that should be the title attribute, if it exists
		 */
		result.add(Ldproxy2Target.coretableIdColumn);
	    }
	    result.add("[JSON]properties/" + titleAtt.name());

	} else {
	    result.add(Ldproxy2Target.coretableIdColumn);
	}

	return result;
    }

    public String sourcePathLinkLevelHref(PropertyInfo pi) {

	// coretable approach only supports feature refs, not links

	return "LINK_UNSUPPORTED_IN_CORETABLE_APPROACH";
    }

    public String urlTemplateForValueType(PropertyInfo pi) {

	String urlTemplate = null;

	String valueTypeName = pi.typeInfo().name;

	ProcessMapEntry pme = Ldproxy2Target.mapEntryParamInfos.getMapEntry(valueTypeName,
		pi.encodingRule(Ldproxy2Constants.PLATFORM));

	if (pme != null && !target.ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme, pi.typeInfo().id)
		&& "LINK".equalsIgnoreCase(pme.getTargetType())) {

	    urlTemplate = Ldproxy2Target.mapEntryParamInfos.getCharacteristic(valueTypeName,
		    pi.encodingRule(Ldproxy2Constants.PLATFORM), Ldproxy2Constants.ME_PARAM_LINK_INFOS,
		    Ldproxy2Constants.ME_PARAM_LINK_INFOS_CHARACT_URL_TEMPLATE);

	    if (urlTemplate != null) {

		// TODO MORE VARIABLES FOR CORETABLE APPROACH?
		urlTemplate = urlTemplate.replaceAll("\\(value\\)", "{{value}}").replaceAll("\\(serviceUrl\\)",
			"{{serviceUrl}}");
	    }
	}

	if (StringUtils.isBlank(urlTemplate)) {
	    urlTemplate = "{{serviceUrl}}/collections/" + valueTypeName.toLowerCase(Locale.ENGLISH)
		    + "/items/{{value}}";
	}

	return urlTemplate;
    }

    @Override
    public String sourcePathTypeLevel(ClassInfo ci) {
	return "/" + Ldproxy2Target.coretable + "{filter=" + Ldproxy2Target.coretableFeatureTypeColumn + "='"
		+ formatCollectionId(ci.name()) + "'}";
    }

    @Override
    public String defaultPrimaryKey() {
	return Ldproxy2Target.coretableIdColumn;
    }

    @Override
    public String defaultSortKey() {
	return Ldproxy2Target.coretableIdColumn;
    }

    /**
     * Identify the collection IDs for the value type of the property. The value
     * type must be a type with identity. A collection ID is returned for each case
     * in the inheritance hierarchy of the value type (starting with the value type,
     * and going down) that is encoded and not abstract.
     * 
     * @param pi Property to determine the collection IDs for
     * @return collection IDs for the value type of the property; can be empty
     *         (especially if the value type is not a type with identity) but not
     *         <code>null</code>
     */
    protected SortedSet<String> collectionIds(PropertyInfo pi) {

	SortedSet<String> collectionIds = new TreeSet<>();

	if (LdpInfo.isTypeWithIdentityValueType(pi)) {

	    ClassInfo typeCi = pi.typeClass();

	    if (typeCi != null) {

		SortedSet<ClassInfo> typeSet = typeCi.subtypesInCompleteHierarchy();
		typeSet.add(typeCi);
		List<ClassInfo> relevantTypes = typeSet.stream()
			.filter(ci -> !ci.isAbstract() && LdpInfo.isEncoded(typeCi)).collect(Collectors.toList());
		for (ClassInfo ci : relevantTypes) {
		    collectionIds.add(formatCollectionId(ci.name()));
		}

	    } else {
		collectionIds.add(formatCollectionId(pi.typeInfo().name));
	    }
	}

	return collectionIds;
    }

    protected String formatCollectionId(String id) {
	// NOTE: In future, we may have different ways to format the collection id
	return id.toLowerCase(Locale.ENGLISH);
    }

    @Override
    public boolean isEncodedWithDirectValueSourcePath(PropertyInfo pi, LdpPropertyEncodingContext context) {

	return true;
//	String typeName = pi.typeInfo().name;
//	String typeId = pi.typeInfo().id;
//	String encodingRule = pi.encodingRule(Ldproxy2Constants.PLATFORM);
//
//	ProcessMapEntry pme = Ldproxy2Target.mapEntryParamInfos.getMapEntry(typeName, encodingRule);
//
//	if (pme != null && !target.ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme, typeId)) {
//
//	    if (pme.hasTargetType()) {
//
//		if ("GEOMETRY".equalsIgnoreCase(pme.getTargetType())) {
//
//		    // the target only allows and thus assumes max mult = 1
//		    return true;
//
//		} else if ("LINK".equalsIgnoreCase(pme.getTargetType())) {
//
//		    return pi.cardinality().maxOccurs == 1;
//
//		} else {
//
//		    // value type is a simple ldproxy type
//		    return true;
//		}
//
//	    } else {
//		// is checked via target configuration validator (which can be switched off)
//		result.addError(msgSource, 118, typeName);
//		return true;
//	    }
//	}
//	
//	if(LdpInfo.isTypeWithIdentityValueType(pi)) {
//	    
//	    return collectionIds(pi).size() == 1;
//	    
//	} else {
//	    
//	    return true;
//	}
    }

    @Override
    public boolean multipleSourcePathsUnsupportedinFragments() {
	return false;
    }

    @Override
    public String sourcePathFeatureRefId(PropertyInfo pi) {
	return Ldproxy2Target.coretableIdColumn;
    }
}
