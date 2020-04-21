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
 * (c) 2002-2020 interactive instruments GmbH, Bonn, Germany
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

package de.interactive_instruments.ShapeChange.Target.OpenApi;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class OpenApiConstants {

    public static final String PLATFORM = "openapi";

    public static final String CC_OGCAPI_FEATURES_1_1_CORE = "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/core";
    public static final String CC_OGCAPI_FEATURES_1_1_GEOJSON = "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/geojson";
    public static final String CC_OGCAPI_FEATURES_1_1_HTML = "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/html";
    public static final String CC_OGCAPI_FEATURES_2_1_CRS = "http://www.opengis.net/spec/ogcapi-features-2/1.0/conf/crs";

    public static final String PARAM_BASE_TEMPLATE = "baseTemplate";
    public static final String PARAM_COLLECTIONS = "collections";
    public static final String PARAM_JSON_SCHEMAS_BASE_LOCATION = "jsonSchemasBaseLocation";
    public static final String PARAM_JSON_SCHEMA_VERSION = "jsonSchemaVersion";
    
    public static final String RULE_ALL_EXPLICIT_COLLECTIONS = "rule-openapi-all-explicit-collections";
    public static final String RULE_CLS_INSTANTIABLE_FEATURE_TYPES = "rule-openapi-cls-instantiable-feature-types";
    public static final String RULE_CLS_TOP_LEVEL_FEATURE_TYPES = "rule-openapi-cls-top-level-feature-types";
}
