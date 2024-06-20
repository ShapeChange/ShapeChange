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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange.Target.CDB;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
public class CDBSubcategory {

	protected String code = null;
	protected String label = null;
	protected SortedMap<String, CDBFeature> featuresByCode = new TreeMap<String, CDBFeature>();

	public CDBSubcategory(String code, String label) {
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
	 * @return the features
	 */
	public SortedMap<String, CDBFeature> getFeatures() {
		return featuresByCode;
	}

	public boolean hasFeature(String featureCode) {
		return featuresByCode.containsKey(featureCode);
	}

	/**
	 * Adds the given feature to this subcategory
	 * 
	 * @param feature tbd
	 */
	public void add(CDBFeature feature) {
		this.featuresByCode.put(feature.getCode(), feature);
	}

	public CDBFeature getFeature(String featureCode) {
		return this.featuresByCode.get(featureCode);
	}
}
