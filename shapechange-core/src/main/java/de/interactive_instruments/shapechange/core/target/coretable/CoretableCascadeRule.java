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
 * (c) 2002-2026 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core.target.coretable;

import java.util.Objects;

import de.interactive_instruments.shapechange.core.model.ClassInfo;
import de.interactive_instruments.shapechange.core.model.PropertyInfo;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class CoretableCascadeRule implements Comparable<CoretableCascadeRule> {

    public enum RelDirection {
	forward, inverse
    }

    private ClassInfo wholeFeatureType;
    private PropertyInfo wholeOwnedRole;
    private ClassInfo partFeatureType;
    private String appSchema;
    private String version;
    private RelDirection relDirection;

    /**
     * @return the wholeFeatureType
     */
    public ClassInfo getWholeFeatureType() {
	return wholeFeatureType;
    }

    /**
     * @param wholeFeatureType the wholeFeatureType to set
     */
    public void setWholeFeatureType(ClassInfo wholeFeatureType) {
	this.wholeFeatureType = wholeFeatureType;
    }

    /**
     * @return the wholeOwnedRole
     */
    public PropertyInfo getWholeOwnedRole() {
	return wholeOwnedRole;
    }

    /**
     * @param wholeOwnedRole the wholeOwnedRole to set
     */
    public void setWholeOwnedRole(PropertyInfo wholeOwnedRole) {
	this.wholeOwnedRole = wholeOwnedRole;
    }

    /**
     * @return the partFeatureType
     */
    public ClassInfo getPartFeatureType() {
	return partFeatureType;
    }

    /**
     * @param partFeatureType the partFeatureType to set
     */
    public void setPartFeatureType(ClassInfo partFeatureType) {
	this.partFeatureType = partFeatureType;
    }

    /**
     * @return the appSchema
     */
    public String getAppSchema() {
	return appSchema;
    }

    /**
     * @param appSchema the appSchema to set
     */
    public void setAppSchema(String appSchema) {
	this.appSchema = appSchema;
    }

    /**
     * @return the version
     */
    public String getVersion() {
	return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(String version) {
	this.version = version;
    }

    /**
     * @return the relDirection
     */
    public RelDirection getRelDirection() {
	return relDirection;
    }

    /**
     * @param relDirection the relDirection to set
     */
    public void setRelDirection(RelDirection relDirection) {
	this.relDirection = relDirection;
    }

    @Override
    public String toString() {
	return "CoretableCascadeRule [wholeFeatureType=" + wholeFeatureType + ", wholeOwnedRole=" + wholeOwnedRole
		+ ", partFeatureType=" + partFeatureType + ", appSchema=" + appSchema + ", version=" + version
		+ ", relDirection=" + relDirection + "]";
    }

    @Override
    public int compareTo(CoretableCascadeRule o) {

	if (this.getAppSchema().compareTo(o.getAppSchema()) != 0) {
	    return this.getAppSchema().compareTo(o.getAppSchema());

	} else if (this.getVersion().compareTo(o.getVersion()) != 0) {
	    return this.getVersion().compareTo(o.getVersion());

	} else if (this.getWholeFeatureType().fullName().compareTo(o.getWholeFeatureType().fullName()) != 0) {
	    return this.getWholeFeatureType().fullName().compareTo(o.getWholeFeatureType().fullName());

	} else if (this.getWholeOwnedRole().fullName().compareTo(o.getWholeOwnedRole().fullName()) != 0) {
	    return this.getWholeOwnedRole().fullName().compareTo(o.getWholeOwnedRole().fullName());

	} else if (this.getPartFeatureType().fullName().compareTo(o.getPartFeatureType().fullName()) != 0) {
	    return this.getPartFeatureType().fullName().compareTo(o.getPartFeatureType().fullName());

	} else {

	    if (this.getRelDirection() == o.getRelDirection()) {
		return 0;
	    } else if (this.getRelDirection() == RelDirection.forward) {
		return -1;
	    } else {
		return 1;
	    }
	}
    }

    @Override
    public int hashCode() {
	return Objects.hash(appSchema, partFeatureType, relDirection, version, wholeFeatureType, wholeOwnedRole);
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	CoretableCascadeRule other = (CoretableCascadeRule) obj;
	return Objects.equals(appSchema, other.appSchema) && Objects.equals(partFeatureType, other.partFeatureType)
		&& relDirection == other.relDirection && Objects.equals(version, other.version)
		&& Objects.equals(wholeFeatureType, other.wholeFeatureType)
		&& Objects.equals(wholeOwnedRole, other.wholeOwnedRole);
    }

}
