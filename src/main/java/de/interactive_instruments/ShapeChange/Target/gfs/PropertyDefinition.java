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
public class PropertyDefinition extends AbstractPropertyDefinition {

    protected GfsPropertyType type = null;
    protected GfsPropertySubtype subtype = null;
    protected boolean isListValuedProperty = false;
    protected int width = 0;
    protected int precision = 0;

    /**
     * @return the type
     */
    public GfsPropertyType getType() {
	return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(GfsPropertyType type) {
	this.type = type;
    }

    /**
     * @return the subtype
     */
    public GfsPropertySubtype getSubtype() {
	return subtype;
    }

    /**
     * @param subtype the subtype to set
     */
    public void setSubtype(GfsPropertySubtype subtype) {
	this.subtype = subtype;
    }

    /**
     * @return the width
     */
    public int getWidth() {
	return width;
    }

    /**
     * @param width the width to set
     */
    public void setWidth(int width) {
	this.width = width;
    }

    /**
     * @return the precision
     */
    public int getPrecision() {
	return precision;
    }

    /**
     * @param precision the precision to set
     */
    public void setPrecision(int precision) {
	this.precision = precision;
    }

    /**
     * @return the isListValuedProperty
     */
    public boolean isListValuedProperty() {
	return isListValuedProperty;
    }

    /**
     * @param isListValuedProperty the isListValuedProperty to set
     */
    public void setListValuedProperty(boolean isListValuedProperty) {
	this.isListValuedProperty = isListValuedProperty;
    }

    @Override
    public PropertyDefinition createCopy() {

	PropertyDefinition copy = new PropertyDefinition();

	copy.setName(this.getName());
	copy.setElementPath(this.getElementPath());
	copy.setNullable(this.isNullable());
	
	copy.setListValuedProperty(this.isListValuedProperty());
	copy.setPrecision(this.getPrecision());
	copy.setWidth(this.getWidth());
	copy.setSubtype(this.getSubtype());
	copy.setType(this.getType());

	return copy;
    }
}
