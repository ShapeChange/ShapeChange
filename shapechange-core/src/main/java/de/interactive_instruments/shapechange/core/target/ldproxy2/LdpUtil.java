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

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.interactive_instruments.shapechange.core.model.ClassInfo;
import de.interactive_instruments.shapechange.core.model.PropertyInfo;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class LdpUtil {

    public static boolean isTrueIgnoringCase(String s) {
	return StringUtils.isNotBlank(s) && "true".equalsIgnoreCase(s.trim());
    }

    public static boolean isLdproxySimpleType(Type t) {

	switch (t) {
	case STRING:
	case BOOLEAN:
	case DATE:
	case DATETIME:
	case INTEGER:
	case FLOAT:
	    return true;

	default:
	    return false;
	}
    }

    public static boolean isLdproxyGeometryType(Type t) {
	return t == Type.GEOMETRY;
    }

    public static String fragmentRef(String specialObjectOrDataTypeName) {
	return "#/fragments/" + specialObjectOrDataTypeName.toLowerCase(Locale.ENGLISH);
    }

    public static String fragmentRef(ClassInfo ci) {
	return "#/fragments/" + LdpInfo.configIdentifierName(ci);
    }

    public static String formatCollectionId(String id) {
	if ("none".equalsIgnoreCase(Ldproxy2Target.collectionIdFormat)) {
	    return id;
	} else {
	    return id.toLowerCase(Locale.ENGLISH);
	}
    }

    public static String queryableId(PropertyInfo pi) {
	return pi.name() + queryableSuffix(pi);
    }

    public static String queryableSuffix(PropertyInfo pi) {

	if (pi.isAttribute()) {

	    return "";

	} else {

	    // pi is association role

	    if (pi.matches(Ldproxy2Constants.RULE_ALL_LINK_OBJECT_AS_FEATURE_REF)) {

		return "";

		/*
		 * 2024-06-13 JE: Differentiation of queryables for id and title disabled.
		 * Queryable thus far only defined for id, which is the default. We will wait
		 * until the WG for OGC API Features has discussed the matter, and maybe
		 * extended the queryable mechanics.
		 */

//		// a feature ref (object) always has an id property
//		queryableProperties.add(pi.name() + ".id");
//		
//		/*
//		 * a feature ref (object) MAY have a title property with an actual title value
//		 * (not just repeating the id)
//		 */
//		if (LdpInfo.valueTypeHasValidLdpTitleAttributeTag(pi)) {
//		    queryableProperties.add(pi.name() + ".title");
//		}

	    } else {

		// assume link encoding

		// not useful to filter on href value
//		queryableProperties.add(pi.name()+".href");

		/*
		 * but a link object always has a title (which is either the object id or an
		 * actual title value)
		 */
		return ".title";
	    }
	}
    }
}
