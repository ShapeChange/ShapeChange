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
 * (c) 2002-2022 interactive instruments GmbH, Bonn, Germany
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

package de.interactive_instruments.ShapeChange.Target.gfs;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class GfsTemplateConstants {

    public static final String PLATFORM = "gfs";

    public static final String ME_PARAM_TYPE_DETAILS = "typeDetails";
    public static final String ME_PARAM_TYPE_DETAILS_CHARACT_SUBTYPE = "subtype";
    public static final String ME_PARAM_TYPE_DETAILS_CHARACT_GMLMEASURETYPE = "gmlMeasureType";

    public static final String PARAM_ALWAYS_ENCODE_DATA_TYPE_NAME = "alwaysEncodeDataTypeName";
    public static final String PARAM_CHOICE_FOR_INLINE_OR_BY_REFERENCE = "choiceForInlineOrByReference";
    public static final String PARAM_GML_CODE_LIST_ENCODING_VERSION = "gmlCodeListEncodingVersion";
    public static final String PARAM_PROPERTY_NAME_SEPARATOR = "propertyNameSeparator";
    public static final String PARAM_SORT_PROPERTIES_BY_NAME = "sortPropertiesByName";    
    public static final String PARAM_SRS_NAME = "srsName";
    public static final String PARAM_XML_ATTRIBUTE_NAME_SEPARATOR = "xmlAttributeNameSeparator";
    public static final String PARAM_XML_ATTRIBUTES_TO_ENCODE = "xmlAttributesToEncode";
    
    public static final String RULE_ALL_NOT_ENCODED = "rule-gfs-all-notEncoded";
    public static final String RULE_PROP_INLINE_ENCODING_USES_HREF_SUFFIX = "rule-gfs-prop-inlineEncodingUsesHrefSuffix";
    public static final String RULE_PROP_PRECISION = "rule-gfs-prop-precision";
    public static final String RULE_PROP_WIDTH = "rule-gfs-prop-width";

    public static final String[] GFS_SUBTYPES = new String[] { "Boolean", "Date", "Datetime", "Float", "Integer64", "Short",
	    "Time" };
    public static final String GFS_TYPE_REGEX = "(String|Real|Integer|FeatureProperty)|((-)?\\d+)|(((Multi)?(Point|LineString|Polygon|Curve|Surface)|GeometryCollection|CircularString|CurvePolygon|Triangle|PolyhedralSurface|TIN)Z?M?)";
	
}
