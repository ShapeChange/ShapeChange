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
package de.interactive_instruments.shapechange.core.target.ldproxy2.provider;

import java.util.List;

import de.interactive_instruments.shapechange.core.model.ClassInfo;
import de.interactive_instruments.shapechange.core.model.PropertyInfo;
import de.interactive_instruments.shapechange.core.target.ldproxy2.LdpPropertyEncodingContext;
import de.interactive_instruments.shapechange.core.target.ldproxy2.LdpSourcePathInfos;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public interface LdpSourcePathProvider {

    public String sourcePathLinkLevelHref(PropertyInfo pi);

    /**
     * @param pi                   the property for which to construct the source
     *                             path on property level
     * @param alreadyVisitedPiList information about previous steps in the source
     *                             path; can be analyzed to detect special cases
     *                             (e.g. lists of data type valued properties)
     * @param context              - The context in which the property is encoded
     * @return - TBD
     */
    public LdpSourcePathInfos sourcePathPropertyLevel(PropertyInfo pi, List<PropertyInfo> alreadyVisitedPiList,
	    LdpPropertyEncodingContext context);

    /**
     * @param pi The property for which to retrieve source paths for the title
     *           property
     * @return List of applicable source paths, where the first available value
     *         should win
     */
    public List<String> sourcePathsLinkLevelTitle(PropertyInfo pi);

    public String urlTemplateForValueType(PropertyInfo pi);

    public String sourcePathTypeLevel(ClassInfo ci);

    public String defaultPrimaryKey();

    public String defaultSortKey();

    public boolean isEncodedWithDirectValueSourcePath(PropertyInfo pi, LdpPropertyEncodingContext context);

    public boolean multipleSourcePathsUnsupportedinFragments();

    public String sourcePathForDataTypeMemberOfGenericValueType();

    public String sourcePathForValueMemberOfGenericValueType(String valuePropertyName, String suffix);

    public String sourcePathFeatureRefId(PropertyInfo pi);

    public String objectIdentifierSourcePath();
}
