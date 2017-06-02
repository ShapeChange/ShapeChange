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
 * (c) 2002-2013 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange;

import java.util.regex.Pattern;

/**
 * Configuration information for a tagged value.
 * 
 * @author echterhoff
 * 
 */
public class TaggedValueConfigurationEntry {

	public enum ModelElementType {
		ASSOCIATION, CLASS, PACKAGE, PROPERTY, ATTRIBUTE, ASSOCIATIONROLE
	};

	private String name = null;
	private String value = null;
	private ModelElementType modelElementType = null;
	private Pattern modelElementStereotypePattern = null;
	private Pattern modelElementNamePattern = null;
	private Pattern propertyValueTypeStereotypePattern = null;
	private Pattern applicationSchemaNamePattern = null;

	public TaggedValueConfigurationEntry(String name, String value,
			ModelElementType modelElementType,
			Pattern modelElementStereotypePattern,
			Pattern modelElementNamePattern,
			Pattern propertyValueTypeStereotypePattern,
			Pattern applicationSchemaNamePattern) {
		super();
		this.name = name;
		this.value = value;
		this.modelElementType = modelElementType;
		this.modelElementStereotypePattern = modelElementStereotypePattern;
		this.modelElementNamePattern = modelElementNamePattern;
		this.propertyValueTypeStereotypePattern = propertyValueTypeStereotypePattern;
		this.applicationSchemaNamePattern = applicationSchemaNamePattern;
	}

	/**
	 * @return the name of the tagged value
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the value of the tagged value, <code>null</code> if not set in
	 *         the configuration
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @return true if the value attribute of the tagged value was set in the
	 *         configuration, else false
	 */
	public boolean hasValue() {
		return value != null;
	}

	/**
	 * @return the pattern representing the regular expression of the
	 *         modelElementStereotype attribute, or <code>null</code> if this
	 *         filter criterium was not set in the configuration.
	 */
	public Pattern getModelElementStereotypePattern() {
		return modelElementStereotypePattern;
	}
	
	/**
	 * @return the pattern representing the regular expression of the
	 *         propertyValueTypeStereotype attribute, or <code>null</code> if this
	 *         filter criterium was not set in the configuration.
	 */
	public Pattern getPropertyValueTypeStereotypePattern() {
		return propertyValueTypeStereotypePattern;
	}

	/**
	 * @return the pattern representing the regular expression of the
	 *         modelElementName attribute, or <code>null</code> if this filter
	 *         criterium was not set in the configuration.
	 */
	public Pattern getModelElementNamePattern() {
		return modelElementNamePattern;
	}

	/**
	 * @return the value defined by the modelElementType attribute, or
	 *         <code>null</code> if this filter criterium was not set in the
	 *         configuration.
	 */
	public ModelElementType getModelElementType() {
		return modelElementType;
	}

	/**
	 * @return the pattern representing the regular expression of the
	 *         applicationSchemaName attribute, or <code>null</code> if this
	 *         filter criterium was not set in the configuration.
	 */
	public Pattern getApplicationSchemaNamePattern() {
		return applicationSchemaNamePattern;
	}

	/**
	 * @return true if this configuration entry has a value for the
	 *         modelElementName attribute, else false
	 */
	public boolean hasModelElementNamePattern() {
		return modelElementNamePattern != null;
	}

	/**
	 * @return true if this configuration entry has a value for the
	 *         modelElementStereotype attribute, else false
	 */
	public boolean hasModelElementStereotypePattern() {
		return modelElementStereotypePattern != null;
	}
	
	/**
	 * @return true if this configuration entry has a value for the
	 *         propertyValueTypeStereotype attribute, else false
	 */
	public boolean hasPropertyValueTypeStereotypePattern() {
		return propertyValueTypeStereotypePattern != null;
	}

	/**
	 * @return true if this configuration entry has a value for the
	 *         applicationSchemaName attribute, else false
	 */
	public boolean hasApplicationSchemaNamePattern() {
		return applicationSchemaNamePattern != null;
	}

	/**
	 * @return true if this configuration entry has a value for the
	 *         modelElementType attribute, else false
	 */
	public boolean hasModelElementType() {
		return modelElementType != null;
	}
}
