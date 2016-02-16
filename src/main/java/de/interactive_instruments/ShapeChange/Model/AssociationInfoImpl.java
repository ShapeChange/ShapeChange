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

public abstract class AssociationInfoImpl extends InfoImpl implements AssociationInfo {

	/** Return the encoding rule relevant on the association, given the platform */
	public String encodingRule(String platform) {
		String s = taggedValue(platform + "EncodingRule");
		if (s == null || s.isEmpty() || options().ignoreEncodingRuleTaggedValues()) {
			ClassInfo aci = assocClass();
			if (aci!=null)
				s = aci.encodingRule(platform);
			else {
				s = super.encodingRule(platform);
			}
		}
		if (s!=null)
			s = s.toLowerCase().trim();
		return s;
	};
	
	public String language() {
		String lang = this.taggedValue("language");

		if (lang==null || lang.isEmpty()) {
			ClassInfo ci = this.assocClass();
			if (ci!=null)
				return ci.language();
		} else
			return lang;
		
		// associations without association classes are not part of a package, so we do 
		// not have an application schema context
		return null;
	}
	
	
	/*
	 * Full qualified UML name
	 * @see de.interactive_instruments.ShapeChange.Model.Info#fullName()
	 */
	public String fullName() {
		return name();
	}
	
	
	/* (non-Javadoc)
	 * @see de.interactive_instruments.ShapeChange.Model.Info#fullNameInSchema()
	 */
	public String fullNameInSchema() {
		return name();
	}
	
	/*
	 * Validate the association against all applicable requirements and recommendations
	 */
	public void postprocessAfterLoadingAndValidate() {
		if (postprocessed)
			return;

		super.postprocessAfterLoadingAndValidate();

		postprocessed = true;
	}	
	
	/* (non-Javadoc)
	 * @see de.interactive_instruments.ShapeChange.Model.AssociationInfo#globalId()
	 */
	public String globalId() {
		return null;
	}
}
