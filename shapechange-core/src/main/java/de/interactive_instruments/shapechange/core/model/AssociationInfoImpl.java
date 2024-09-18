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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */

package de.interactive_instruments.shapechange.core.model;

import org.apache.commons.lang3.StringUtils;

public abstract class AssociationInfoImpl extends InfoImpl
		implements AssociationInfo {

	protected String name = null;

	/**
	 * Return the encoding rule relevant on the association, given the platform
	 */
	public String encodingRule(String platform) {
		String s = taggedValue(platform + "EncodingRule");
		if (s == null || s.isEmpty()
				|| options().ignoreEncodingRuleTaggedValues()) {
			ClassInfo aci = assocClass();
			if (aci != null)
				s = aci.encodingRule(platform);
			else {
				s = super.encodingRule(platform);
			}
		}
		if (s != null)
			s = s.toLowerCase().trim();
		return s;
	};

	/**
	 * {@inheritDoc}
	 * <p>
	 * WARNING: This method is intended to be "final", but not actually declared
	 * as such. A depending project can thus extend the method, if absolutely
	 * necessary.
	 * 
	 * @see de.interactive_instruments.shapechange.core.model.InfoImpl#language()
	 */
	@Override
	public String language() {
		String lang = this.taggedValue("language");

		if (StringUtils.isBlank(lang)) {
			ClassInfo ci = this.assocClass();
			if (ci != null)
				return ci.language();
		} else
			return lang;

		// associations without association classes are not part of a package,
		// so we do
		// not have an application schema context
		return null;
	}

	@Override
	public String name() {
		/*
		 * 2016-07-26 JE: The association name should not always automatically
		 * be constructed. For rule-owl-prop-iso191502Aggregation we would get
		 * association names that are not in the model. I've added a new input
		 * parameter to control the behavior.
		 */
		if (StringUtils.isBlank(name)) {
			if (options().dontConstructAssociationNames()) {
				name = "";
			} else {
				if (end2() != null && end2().inClass() != null)
					name = end2().inClass().name() + "_";
				else
					name = "end2_";
				// name = "roles[0]_";
				if (end1() != null && end1().inClass() != null)
					name = name + end1().inClass().name();
				else
					name = name + "end1";
				// name = name + "roles[1]";
			}
		}

		return name;
	}

	@Override
	public String fullName() {
		return name();
	}

	@Override
	public String fullNameInSchema() {
		return name();
	}
	
	@Override
	public final boolean isReflexive() {
	    return this.end1().inClass() == this.end2().inClass();
	}
	
	@Override
	public final boolean isBiDirectional() {
	    return this.end1().isNavigable() && this.end2().isNavigable();
	}

	@Override
	public void postprocessAfterLoadingAndValidate() {
		if (postprocessed)
			return;

		super.postprocessAfterLoadingAndValidate();

		postprocessed = true;
	}
}
