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
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange.Target.JSON.config;

import de.interactive_instruments.ShapeChange.ModelElementSelectionInfo;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class AbstractJsonSchemaAnnotationElement {

    private String annotation;
    private boolean isArrayValue = false;

    private NoValueBehavior noValueBehavior = NoValueBehavior.IGNORE;
    private String noValueValue = "";

    private ModelElementSelectionInfo selectionInfo = null;

    public AbstractJsonSchemaAnnotationElement(String annotation) {
	this.annotation = annotation;
    }

    /**
     * @return the isArrayValue
     */
    public boolean isArrayValue() {
	return isArrayValue;
    }

    /**
     * @param isArrayValue the isArrayValue to set
     */
    public void setArrayValue(boolean isArrayValue) {
	this.isArrayValue = isArrayValue;
    }

    /**
     * @return the annotation
     */
    public String getAnnotation() {
	return annotation;
    }

    /**
     * @return the noValueBehavior
     */
    public NoValueBehavior getNoValueBehavior() {
	return noValueBehavior;
    }

    /**
     * @param noValueBehavior the noValueBehavior to set
     */
    public void setNoValueBehavior(NoValueBehavior noValueBehavior) {
	this.noValueBehavior = noValueBehavior;
    }

    /**
     * @return the noValueValue
     */
    public String getNoValueValue() {
	return noValueValue;
    }

    /**
     * @param noValueValue the noValueValue to set
     */
    public void setNoValueValue(String noValueValue) {
	this.noValueValue = noValueValue;
    }

    public void setModelElementSelectionInfo(ModelElementSelectionInfo selectionInfo) {
	this.selectionInfo = selectionInfo;
    }
    
    public ModelElementSelectionInfo getModelElementSelectionInfo() {
	return this.selectionInfo;
    }
}
