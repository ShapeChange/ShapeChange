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
import de.interactive_instruments.ShapeChange.Target.Ldproxy2.provider.LdpSourcePathProvider;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class LdpCoretableSourcePathProvider implements LdpSourcePathProvider {

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

	String sourcePath = pi.name();
	if (alreadyVisitedPiList.isEmpty()
		&& (LdpInfo.isTypeWithIdentity(pi.inClass()) || pi.inClass().category() == Options.MIXIN)) {
	    sourcePath = "[JSON]properties/" + sourcePath;
	}
	Type valueType = null;
	String refType = null;
	String refUriTemplate = null;
	boolean targetsSingleValue = pi.cardinality().maxOccurs == 1;

	boolean featureRefWithMultiCaseEncountered = false;

	LdpSpecialPropertiesInfo specialPropInfos = this.target.specialPropertiesInfo(pi.inClass());

	if (pme != null && !target.ignoreMapEntryForTypeFromSchemaSelectedForProcessing(pme, typeId)) {

	    if (pme.hasTargetType()) {

		if ("GEOMETRY".equalsIgnoreCase(pme.getTargetType())) {

		    // the target only allows and thus assumes max mult = 1

		    targetsSingleValue = true;

		    // special handling for primary geometry
		    if (specialPropInfos != null && specialPropInfos.getDefaultGeometryPiOfCi() == pi) {
			sourcePath = Ldproxy2Target.coretableGeometryColumnName;
		    } else {
			// source path is just the property name
		    }

		} else if ("LINK".equalsIgnoreCase(pme.getTargetType())) {

		    // only feature ref encoding is supported in the coretable approach

		    refUriTemplate = urlTemplateForValueType(pi);
		    valueType = Ldproxy2Target.coretableIdColumnLdproxyType;

		    // source path is just the property name

		    // TODO CP - DEFINITION WIE DAS MIT URL TEMPLATE UND VARIABLER COLLECTION ID
		    // KLAPPT

//		    TODO collectionInfos wie im JSON Schema Target?
//			    mit collectionIds (Liste oder 'any') und collectionIdTypes (darf aber nur einer sein, oder sonst string nutzen?)
//			    urlTemplate um Platzhalter (collectionId) erweitern??

		} else {

		    // value type is a simple ldproxy type

		    // source path is just the property name
		}

	    } else {
		// is checked via target configuration validator (which can be switched off)
		result.addError(msgSource, 118, typeName);
		sourcePath = "FIXME";
	    }

	} else {

	    ClassInfo typeCi = pi.typeClass();

	    if (typeCi == null) {

		MessageContext mc = result.addError(msgSource, 118, typeName);
		if (mc != null) {
		    mc.addDetail(msgSource, 1, pi.fullNameInSchema());
		}
		sourcePath = "FIXME";

	    } else {

		if (typeCi.category() == Options.ENUMERATION || typeCi.category() == Options.CODELIST) {

		    // source path is just the property name

		} else if (typeCi.category() == Options.DATATYPE) {

		    // source path is just the property name

		} else {

		    // value type is type with identity

		    SortedSet<String> collectionIds = collectionIds(pi);

		    if (collectionIds.size() == 1) {

			// source path is just the property name
			refType = collectionIds.first();
			valueType = Ldproxy2Target.coretableIdColumnLdproxyType;

		    } else {

			featureRefWithMultiCaseEncountered = true;

			// TODO CP - DEFINE HOW THIS CASE IS ACTUALLY ENCODED

			// TODO DIFFERENTIATE CASES WITH/OUT URI TEMPLATE

			for (String collectionId : collectionIds) {

			    LdpSourcePathInfo spi = new LdpSourcePathInfo();

			    spi.sourcePath = sourcePath + "/featureId";
			    spi.valueType = Ldproxy2Target.coretableIdColumnLdproxyType;
			    spi.refType = collectionId;
			    spi.refUriTemplate = null;
			    spi.targetsSingleValue = targetsSingleValue;

			    spRes.addSourcePathInfo(spi);
			}
		    }
		}
	    }
	}

	if (StringUtils.isNotBlank(sourcePath) && !featureRefWithMultiCaseEncountered) {

	    LdpSourcePathInfo spi = new LdpSourcePathInfo();
	    spi.sourcePath = sourcePath;
	    spi.valueType = valueType;
	    spi.refType = refType;
	    spi.refUriTemplate = refUriTemplate;
	    spi.targetsSingleValue = targetsSingleValue;

	    spRes.addSourcePathInfo(spi);
	}

	return spRes;
    }

    public List<String> sourcePathsLinkLevelTitle(PropertyInfo pi) {

	// coretable approach only supports feature refs, not links

	List<String> result = new ArrayList<>();

	result.add("LINK_UNSUPPORTED_IN_CORETABLE_APPROACH");

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
	return "/" + Ldproxy2Target.coretableName + "{filter=" + Ldproxy2Target.coretableFeatureTypeColumnName + "='"
		+ ci.name() + "'}";
    }

    @Override
    public String defaultPrimaryKey() {
	return Ldproxy2Target.coretableIdColumnName;
    }

    @Override
    public String defaultSortKey() {
	return Ldproxy2Target.coretableIdColumnName;
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
}
