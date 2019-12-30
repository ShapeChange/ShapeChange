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
 * (c) 2002-2017 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target.CDB;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
public class CDBCategory {

	protected String code = null;
	protected String label = null;
	protected SortedMap<String, CDBSubcategory> subcategoriesByCode = new TreeMap<String, CDBSubcategory>();
	protected CDBSubcategory defaultSubcategory = null;

	public CDBCategory() {
		super();
	}

	public CDBCategory(String code, String label) {
		super();
		this.code = code;
		this.label = label;
	}

	/**
	 * @return the code
	 */
	public String getCode() {
		return code;
	}

	/**
	 * @param code
	 *            the code to set
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @param label
	 *            the label to set
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * Retrieve the default subcategory of this category. If it did not exist
	 * yet, it will be created and added to the set of subcategories.
	 * 
	 * @return the default subcategory of this category
	 */
	public CDBSubcategory getDefaultSubcategory() {

		if (defaultSubcategory == null) {
			defaultSubcategory = new CDBSubcategory("Default", "Default");
			this.subcategoriesByCode.put(defaultSubcategory.getCode(),
					defaultSubcategory);
		}

		return defaultSubcategory;
	}

	/**
	 * @param subcatcode tbd
	 * @return <code>true</code> if a subcategory with the given code exists,
	 *         else <code>false</code>
	 */
	public boolean hasSubcategory(String subcatcode) {
		return this.subcategoriesByCode.containsKey(subcatcode);
	}

	/**
	 * @param subcatcode tbd
	 * @return the subcategory with the given code; can be <code>null</code> if
	 *         no such subcategory exists
	 */
	public CDBSubcategory getSubcategory(String subcatcode) {
		return this.subcategoriesByCode.get(subcatcode);
	}

	/**
	 * Adds the given subcategory to this category
	 * 
	 * @param subcat tbd
	 */
	public void add(CDBSubcategory subcat) {
		this.subcategoriesByCode.put(subcat.getCode(), subcat);
	}

	/**
	 * @return the subcategories assigned to this category, can be empty but not
	 *         <code>null</code>
	 */
	public SortedMap<String, CDBSubcategory> getSubcategoriesByCode() {
		return this.subcategoriesByCode;
	}

}
