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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.shapechange.core.target.gfs;

import java.util.Optional;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public enum GfsPropertySubtype {

    BOOLEAN("Boolean", GfsPropertyType.STRING, true), DATE("Date", GfsPropertyType.STRING, false),
    TIME("Time", GfsPropertyType.STRING, false), DATETIME("Datetime", GfsPropertyType.STRING, false),
    FLOAT("Float", GfsPropertyType.REAL, false), SHORT("Float", GfsPropertyType.INTEGER, false),
    INTEGER64("Integer64", GfsPropertyType.INTEGER, true);

    private final String name;
    private final GfsPropertyType parentType;
    private final boolean supportedInListType;

    GfsPropertySubtype(String name, GfsPropertyType parentType, boolean supportedInListType) {
	this.name = name;
	this.parentType = parentType;
	this.supportedInListType = supportedInListType;
    }

    public String getName() {
	return this.name;
    }

    /**
     * @return the parent type
     */
    public GfsPropertyType getParentType() {
	return parentType;
    }

    /**
     * @return <code>true</code>, if the subtype is supported in the list variant of
     *         its parent type, else <code>false</code>
     */
    public boolean isSupportedInListType() {
	return supportedInListType;
    }

    /**
     * @param name The name of the gfs subtype enum to retrieve
     * @return the enum whose name is equal to, ignoring case, the given name; can
     *         be empty (if the name does not match one of the defined enum names)
     *         but not <code>null</code>
     */
    public static Optional<GfsPropertySubtype> fromString(String name) {

	for (GfsPropertySubtype v : values()) {
	    if (v.getName().equalsIgnoreCase(name)) {
		return Optional.of(v);
	    }
	}
	return Optional.empty();
    }

}
