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
public class CoretableNavigableRole implements Comparable<CoretableNavigableRole> {

    public enum RelDirection {
	forward, inverse
    }

    private ClassInfo sourceFeatureType;
    private PropertyInfo navigableRole;
    private ClassInfo targetFeatureType;
    private String appSchema;
    private String version;
    private RelDirection relDirection;
    private boolean isExistentiallyDependentPart;

    public ClassInfo getSourceFeatureType() {
	return sourceFeatureType;
    }

    public void setSourceFeatureType(ClassInfo sourceFeatureType) {
	this.sourceFeatureType = sourceFeatureType;
    }

    public PropertyInfo getNavigableRole() {
	return navigableRole;
    }

    public void setNavigableRole(PropertyInfo navigableRole) {
	this.navigableRole = navigableRole;
    }

    public ClassInfo getTargetFeatureType() {
	return targetFeatureType;
    }

    public void setTargetFeatureType(ClassInfo targetFeatureType) {
	this.targetFeatureType = targetFeatureType;
    }

    public String getAppSchema() {
	return appSchema;
    }

    public void setAppSchema(String appSchema) {
	this.appSchema = appSchema;
    }

    public String getVersion() {
	return version;
    }

    public void setVersion(String version) {
	this.version = version;
    }

    public RelDirection getRelDirection() {
	return relDirection;
    }

    public void setRelDirection(RelDirection relDirection) {
	this.relDirection = relDirection;
    }

    public boolean isExistentiallyDependentPart() {
	return isExistentiallyDependentPart;
    }

    public void setExistentiallyDependentPart(boolean isExistentiallyDependentPart) {
	this.isExistentiallyDependentPart = isExistentiallyDependentPart;
    }

    @Override
    public String toString() {
	return "CoretableNavigableRole [sourceFeatureType=" + sourceFeatureType + ", navigableRole=" + navigableRole
		+ ", targetFeatureType=" + targetFeatureType + ", appSchema=" + appSchema + ", version=" + version
		+ ", relDirection=" + relDirection + ", isExistentiallyDependentPart=" + isExistentiallyDependentPart
		+ "]";
    }

    @Override
    public int compareTo(CoretableNavigableRole o) {

	if (this.getAppSchema().compareTo(o.getAppSchema()) != 0) {
	    return this.getAppSchema().compareTo(o.getAppSchema());

	} else if (this.getVersion().compareTo(o.getVersion()) != 0) {
	    return this.getVersion().compareTo(o.getVersion());

	} else if (this.getSourceFeatureType().fullName().compareTo(o.getSourceFeatureType().fullName()) != 0) {
	    return this.getSourceFeatureType().fullName().compareTo(o.getSourceFeatureType().fullName());

	} else if (this.getNavigableRole().fullName().compareTo(o.getNavigableRole().fullName()) != 0) {
	    return this.getNavigableRole().fullName().compareTo(o.getNavigableRole().fullName());

	} else if (this.getTargetFeatureType().fullName().compareTo(o.getTargetFeatureType().fullName()) != 0) {
	    return this.getTargetFeatureType().fullName().compareTo(o.getTargetFeatureType().fullName());

	} else if (Boolean.compare(this.isExistentiallyDependentPart(), o.isExistentiallyDependentPart()) != 0) {
	    return Boolean.compare(this.isExistentiallyDependentPart(), o.isExistentiallyDependentPart());

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
	return Objects.hash(appSchema, isExistentiallyDependentPart, navigableRole, relDirection, sourceFeatureType,
		targetFeatureType, version);
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	CoretableNavigableRole other = (CoretableNavigableRole) obj;
	return Objects.equals(appSchema, other.appSchema)
		&& isExistentiallyDependentPart == other.isExistentiallyDependentPart
		&& Objects.equals(navigableRole, other.navigableRole) && relDirection == other.relDirection
		&& Objects.equals(sourceFeatureType, other.sourceFeatureType)
		&& Objects.equals(targetFeatureType, other.targetFeatureType) && Objects.equals(version, other.version);
    }

}
