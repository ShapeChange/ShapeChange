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

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class CDBFeature {

	protected String code = null;
	protected String label = null;
	protected String conceptDefinition = null;
	protected String origin = "";
	protected ClassInfo classInfo = null;

	public CDBFeature(ClassInfo ci) {

		classInfo = ci;

		code = StringUtils.isNotBlank(ci.primaryCode())
				? ci.primaryCode().trim() : ci.name();
		label = StringUtils.isNotBlank(ci.aliasName()) ? ci.aliasName().trim()
				: ci.name();

		conceptDefinition = ci.definition();

		if (StringUtils.isNotBlank(ci.description())) {
			conceptDefinition = conceptDefinition + " [descr] "
					+ ci.description().trim();
		}

		PackageInfo schema = ci.pkg().rootPackage();
		if (schema != null) {
			origin = schema.name();
			if (schema.version() != null) {
				origin = origin + " " + schema.version();
			}
		}
	}

	/**
	 * @return the code, cannot be empty
	 */
	public String getCode() {
		return code;
	}

	/**
	 * @return the label, cannot be empty
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @return the conceptDefinition, can be empty not <code>null</code>
	 */
	public String getConceptDefinition() {
		return conceptDefinition;
	}

	/**
	 * @return the origin, can be empty not <code>null</code>
	 */
	public String getOrigin() {
		return origin;
	}

	public ClassInfo classInfo() {
		return classInfo;
	}
}
