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
 * (c) 2002-2019 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target.GeoPackage;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments <dot>
 *         de)
 *
 */
public class GeoPackageConstants {

    /* ------------------ */
    /* --- Parameters --- */
    /* ------------------ */

    /**
     * Optional changes to the default documentation template and the default
     * strings for descriptors without value
     */
    public static final String PARAM_DOCUMENTATION_TEMPLATE = "documentationTemplate";
    public static final String PARAM_DOCUMENTATION_NOVALUE = "documentationNoValue";

    /**
     * ID (assigned by the organization from parameter
     * {@link #PARAM_SRS_ORGANIZATION}) of the spatial reference system to use for
     * definition of geometry columns; default is '4326'.
     */
    public static final String PARAM_ORGANIZATION_COORD_SYS_ID = "organizationCoordSysId";
    /**
     * Name of the organization that assigned the ID of the spatial reference system
     * (see parameter {@link #PARAM_SRS_ID}); default is 'EPSG'.
     */
    public static final String PARAM_SRS_ORGANIZATION = "srsOrganization";

    /**
     * Name for the identifier column when generating tables. This parameter is
     * optional. The default is {@value #DEFAULT_ID_COLUMN_NAME}.
     */
    public static final String PARAM_ID_COLUMN_NAME = "idColumnName";
    public static final String DEFAULT_ID_COLUMN_NAME = "_id";

    /* ------------------------ */
    /* --- Conversion rules --- */
    /* ------------------------ */

    public static final String RULE_TGT_GPKG_CLS_OBJECTTYPE = "rule-gpkg-cls-objecttype";

    public static final String RULE_TGT_GPKG_ALL_NOTENCODED = "rule-gpkg-all-notEncoded";

    /**
     * Enables use of stereotype 'identifier' on class attributes. If an attribute
     * with that stereotype belongs to a class, then the column to represent that
     * attribute will be used as primary key (and no extra identifier column will be
     * generated).
     */
    public static final String RULE_TGT_GPKG_CLS_IDENTIFIER_STEREOTYPE = "rule-gpkg-cls-identifierStereotype";

}
