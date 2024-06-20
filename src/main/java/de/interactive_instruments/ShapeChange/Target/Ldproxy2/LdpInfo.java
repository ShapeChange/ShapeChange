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
package de.interactive_instruments.ShapeChange.Target.Ldproxy2;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Transformation.TaggedValues.TaggedValueTransformer;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class LdpInfo {

    public static Optional<String> description(Info i) {

	if (i != null && StringUtils.isNotBlank(Ldproxy2Target.descriptionTemplate)
		&& i.matches(Ldproxy2Constants.RULE_ALL_DOCUMENTATION)) {

	    String description = i.derivedDocumentation(Ldproxy2Target.descriptionTemplate,
		    Ldproxy2Target.descriptorNoValue);

	    return StringUtils.isBlank(description) ? Optional.empty() : Optional.of(description);

	} else {
	    return Optional.empty();
	}
    }

    public static String codelistId(ClassInfo ci) {
	return ci.name().replaceAll("\\W", "_");
    }

    public static Optional<String> label(Info i) {

	if (i != null && StringUtils.isNotBlank(Ldproxy2Target.labelTemplate)
		&& i.matches(Ldproxy2Constants.RULE_ALL_DOCUMENTATION)) {

	    String label = i.derivedDocumentation(Ldproxy2Target.labelTemplate, Ldproxy2Target.descriptorNoValue);

	    return StringUtils.isBlank(label) ? Optional.empty() : Optional.of(label);

	} else {
	    return Optional.empty();
	}
    }

    public static String originalPropertyName(PropertyInfo pi) {
	String originalPropertyName = pi.taggedValue(TaggedValueTransformer.TV_ORIG_PROPERTY_NAME);
	if (StringUtils.isBlank(originalPropertyName)) {
	    originalPropertyName = pi.name();
	}
	return originalPropertyName;
    }

    public static String originalSchemaName(PropertyInfo pi) {
	String originalSchemaName = pi.taggedValue(TaggedValueTransformer.TV_ORIG_SCHEMA_NAME);
	if (StringUtils.isBlank(originalSchemaName)) {
	    originalSchemaName = pi.model().schemaPackage(pi.inClass()).name();
	}
	return originalSchemaName;
    }

    public static String originalInClassName(PropertyInfo pi) {
	String originalInClassName = pi.taggedValue(TaggedValueTransformer.TV_ORIG_INCLASS_NAME);
	if (StringUtils.isBlank(originalInClassName)) {
	    originalInClassName = originalClassName(pi.inClass());
	}
	return originalInClassName;
    }

    public static String originalClassName(ClassInfo ci) {
	String originalClassName = ci.taggedValue(TaggedValueTransformer.TV_ORIG_CLASS_NAME);
	if (StringUtils.isBlank(originalClassName)) {
	    originalClassName = ci.name();
	}
	return originalClassName;
    }

//    public static List<ClassInfo> allSupertypes(ClassInfo ci) {
//
//	List<ClassInfo> result = new ArrayList<ClassInfo>();
//
//	for (String supertypeId : ci.supertypes()) {
//
//	    ClassInfo supertype = ci.model().classById(supertypeId);
//
//	    result.addAll(allSupertypes(supertype));
//	    result.add(supertype);
//	}
//
//	return result;
//    }

    public static List<ClassInfo> directSupertypesInOrderOfXsdEncoding(ClassInfo ci) {

	List<ClassInfo> result = new ArrayList<>();

	if (ci.category() != Options.MIXIN) {
	    result.addAll(ci.supertypeClasses());
	} else {
	    SortedSet<ClassInfo> stcis = ci.supertypeClasses();
	    ClassInfo baseClass = ci.baseClass();
	    if (baseClass != null) {
		result.add(baseClass);
		stcis.remove(baseClass);
		result.addAll(stcis);
	    } else {
		result.addAll(stcis);
	    }
	}

	return result;
    }

    public static List<PropertyInfo> allPropertiesInOrderOfXsdEncoding(ClassInfo ci) {

	List<PropertyInfo> allProps = new ArrayList<>();

	List<PropertyInfo> directProps = new ArrayList<>(ci.properties().values());

	List<PropertyInfo> supertypeProps = new ArrayList<>();

	List<ClassInfo> supertypes = LdpInfo.directSupertypesInOrderOfXsdEncoding(ci).stream()
		.filter(st -> LdpInfo.isEncoded(st)).collect(Collectors.toList());

	for (ClassInfo supertype : supertypes) {

	    for (PropertyInfo supertypeProp : allPropertiesInOrderOfXsdEncoding(supertype)) {
		/*
		 * ensure that direct property of the class is not overridden by supertype
		 * property
		 */
		if (directProps.stream().filter(ciProp -> ciProp.name().equals(supertypeProp.name())).findFirst()
			.isEmpty()) {
		    supertypeProps.add(supertypeProp);
		}
	    }
	}

	if (ci.category() == Options.MIXIN) {
	    allProps.addAll(directProps);
	    allProps.addAll(supertypeProps);
	} else {
	    allProps.addAll(supertypeProps);
	    allProps.addAll(directProps);
	}

	return allProps;
    }

    public static Optional<String> featureTitleTemplate(ClassInfo ci) {

	String tv = ci.taggedValue("ldpFeatureTitleTemplate");
	if (StringUtils.isBlank(tv)) {
	    return Optional.empty();
	} else {
	    return Optional.of(tv.trim());
	}
    }

    public static boolean isEncoded(Info i) {

	if (i.matches(Ldproxy2Constants.RULE_ALL_NOT_ENCODED)
		&& i.encodingRule(Ldproxy2Constants.PLATFORM).equalsIgnoreCase("notencoded")) {

	    return false;

	} else {

	    return true;
	}
    }

    public static boolean unsupportedCategoryOfValue(PropertyInfo pi) {

	return pi.categoryOfValue() == Options.BASICTYPE || pi.categoryOfValue() == Options.MIXIN
		|| pi.categoryOfValue() == Options.UNKNOWN;
    }

    public static boolean isEnumerationOrCodelistValueType(PropertyInfo pi) {
	return pi.categoryOfValue() == Options.ENUMERATION || pi.categoryOfValue() == Options.CODELIST;
    }

    public static boolean isTypeWithIdentity(ClassInfo ci) {
	return ci.category() == Options.FEATURE || ci.category() == Options.OBJECT;
    }

    public static boolean isTypeWithIdentityValueType(PropertyInfo pi) {
	return pi.categoryOfValue() == Options.FEATURE || pi.categoryOfValue() == Options.OBJECT;
    }

    public static boolean valueTypeHasValidLdpTitleAttributeTag(PropertyInfo pi) {

	ClassInfo typeCi = pi.typeClass();
	if (typeCi == null) {
	    return false;
	} else {
	    PropertyInfo titleAtt = getTitleAttribute(typeCi);
	    return titleAtt != null;
	}
    }

    public static boolean valueTypeHasValidLdpTypeAttributeTag(PropertyInfo pi) {

	ClassInfo typeCi = pi.typeClass();
	if (typeCi == null) {
	    return false;
	} else {
	    PropertyInfo titleAtt = getTypeAttribute(typeCi);
	    return titleAtt != null;
	}
    }

    /**
     * @param ci - tbd
     * @return the attribute of ci whose name is equal to the value of tag
     *         ldpTitleAttribute on ci
     */
    public static PropertyInfo getTitleAttribute(ClassInfo ci) {

	PropertyInfo result = null;

	String titleAttName = ci.taggedValue("ldpTitleAttribute");
	if (StringUtils.isNotBlank(titleAttName)) {
	    result = ci.property(titleAttName.trim());
	}

	return result;
    }

    /**
     * @param ci - tbd
     * @return the attribute of ci whose name is equal to the value of tag
     *         ldpTypeAttribute on ci
     */
    public static PropertyInfo getTypeAttribute(ClassInfo ci) {

	PropertyInfo result = null;

	String titleAttName = ci.taggedValue("ldpTypeAttribute");
	if (StringUtils.isNotBlank(titleAttName)) {
	    result = ci.property(titleAttName.trim());
	}

	return result;
    }

    public static Iterable<String> enumValues(ClassInfo enumeration) {

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

    public static boolean isReflexive(PropertyInfo pi) {
	return pi.inClass().id().equals(pi.typeInfo().id);
    }

    public static String configIdentifierName(Info i) {
	if (i instanceof ClassInfo) {
	    if ("none".equalsIgnoreCase(Ldproxy2Target.collectionIdFormat)) {
		return i.name();
	    } else {
		return i.name().toLowerCase(Locale.ENGLISH);
	    }
	} else {
	    return i.name().toLowerCase(Locale.ENGLISH);
	}
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
    public static SortedSet<String> collectionIds(PropertyInfo pi) {

	SortedSet<String> collectionIds = new TreeSet<>();

	if (LdpInfo.isTypeWithIdentityValueType(pi)) {

	    ClassInfo typeCi = pi.typeClass();

	    if (typeCi != null) {

		SortedSet<ClassInfo> typeSet = typeCi.subtypesInCompleteHierarchy();
		typeSet.add(typeCi);
		List<ClassInfo> relevantTypes = typeSet.stream()
			.filter(ci -> !ci.isAbstract() && LdpInfo.isEncoded(typeCi)).collect(Collectors.toList());
		for (ClassInfo ci : relevantTypes) {
		    collectionIds.add(LdpUtil.formatCollectionId(ci.name()));
		}

	    } else {
		collectionIds.add(LdpUtil.formatCollectionId(pi.typeInfo().name));
	    }
	}

	return collectionIds;
    }
}
