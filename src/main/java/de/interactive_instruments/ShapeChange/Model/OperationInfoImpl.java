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
 * (c) 2002-2012 interactive instruments GmbH, Bonn, Germany
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

package de.interactive_instruments.ShapeChange.Model;

public abstract class OperationInfoImpl extends InfoImpl implements OperationInfo {

	/* (non-Javadoc)
	 * @see de.interactive_instruments.ShapeChange.Model.Info#fullName()
	 */
	public String fullName() {
		return name();
	}
	
	public final String language() {
		String lang = this.taggedValue("language");

		if (lang!=null && !lang.isEmpty())
			return lang;
		
		return null;
	}

	
	/* (non-Javadoc)
	 * @see de.interactive_instruments.ShapeChange.Model.Info#fullName()
	 */
	public String fullNameInSchema() {
		return name();
	}

	/*
	 * Validate the operation against all applicable requirements and recommendations
	 */
	public void postprocessAfterLoadingAndValidate() {
		if (postprocessed)
			return;

		super.postprocessAfterLoadingAndValidate();

		postprocessed = true;
	}
}
