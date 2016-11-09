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
 * (c) 2002-2016 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Transformation.Flattening;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Multiplicity;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.TransformerConfiguration;
import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericAssociationInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericClassInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel.PropertyCopyDuplicatBehaviorIndicator;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericPropertyInfo;
import de.interactive_instruments.ShapeChange.Transformation.Transformer;

/**
 * Maps an association class into a semantically equivalent class and set of
 * associations, as defined by the OGC GML 3.3 standard.
 * 
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class AssociationClassMapper implements Transformer, MessageSource {

	/* ------------------------------------------- */
	/* --- configuration parameter identifiers --- */
	/* ------------------------------------------- */
	// none at present

	/* ------------------------ */
	/* --- rule identifiers --- */
	/* ------------------------ */
	// none at present

	/* -------------------- */
	/* --- other fields --- */
	/* -------------------- */
	// none at present

	@Override
	public void process(GenericModel m, Options o,
			TransformerConfiguration trfConfig, ShapeChangeResult r)
			throws ShapeChangeAbortException {

		SortedSet<GenericAssociationInfo> associations = m
				.selectedSchemaAssociations();

		String idSuffix = "_associationClassMapping";

		for (GenericAssociationInfo association : associations) {

			/*
			 * NOTE: all casts should be safe because the generic model is a
			 * complete copy of the model
			 */

			GenericPropertyInfo end1 = (GenericPropertyInfo) association.end1();
			GenericPropertyInfo end2 = (GenericPropertyInfo) association.end2();

			GenericClassInfo assocCi = (GenericClassInfo) association
					.assocClass();

			if (assocCi != null) {

				/*
				 * Delete the association class relationship, i.e. the
				 * relationships between the ClassInfo and AssociationInfo. We
				 * keep the ClassInfo as-is.
				 */
				assocCi.setAssocInfo(null);
				association.setAssocClass(null);

				// The association is no longer needed
				m.remove(association);

				PropertyInfo navigableRole = null;
				PropertyInfo other = null;

				if (end1.isNavigable()) {
					navigableRole = end1;
					other = end2;
				} else {
					navigableRole = end2;
					other = end1;
				}

				List<GenericPropertyInfo> newPropsForAssociationClass = new ArrayList<GenericPropertyInfo>();

				// role2_1[a..b] | role4_3[e..f]
				GenericPropertyInfo nav_a = m.createCopy(navigableRole,
						navigableRole.id() + idSuffix + "_a");
				m.register(nav_a);
				((GenericClassInfo) navigableRole.inClass()).addProperty(nav_a,
						PropertyCopyDuplicatBehaviorIndicator.OVERWRITE);

				Type ti_nav_a = new Type();
				ti_nav_a.id = assocCi.id();
				ti_nav_a.name = assocCi.name();
				nav_a.setTypeInfo(ti_nav_a);

				// <unnamed> | role3_4[1]
				GenericPropertyInfo other_a = m.createCopy(other,
						other.id() + idSuffix + "_a");
				other_a.setInClass(assocCi);
				
				if (other_a.isNavigable()) {
					other_a.setCardinality(new Multiplicity());
					/*
					 * Make the new property known to the model and its inClass
					 * only if it is navigable.
					 */
					m.register(other_a);
					newPropsForAssociationClass.add(other_a);
				}

				// Feature1->F12 | Feature3-F34
				GenericAssociationInfo ai_a = m.createCopy(association,
						association.id() + idSuffix + "_a");
				ai_a.setAssocClass(null);
				m.addAssociation(ai_a);

				nav_a.setAssociation(ai_a);
				other_a.setAssociation(ai_a);
				
				nav_a.setReverseProperty(other_a);
				other_a.setReverseProperty(nav_a);

				if (ai_a.end1() == navigableRole) {
					ai_a.setEnd1(nav_a);
					ai_a.setEnd2(other_a);
				} else {
					ai_a.setEnd1(other_a);
					ai_a.setEnd2(nav_a);
				}

				// role2_1[1] | role4_3[1]
				GenericPropertyInfo nav_b = m.createCopy(navigableRole,
						navigableRole.id() + idSuffix + "_b");
				m.register(nav_b);
				newPropsForAssociationClass.add(nav_b);
				nav_b.setInClass(assocCi);
				nav_b.setCardinality(new Multiplicity());

				// <unnamed> | role3_4[c..d]
				GenericPropertyInfo other_b = m.createCopy(other,
						other.id() + idSuffix + "_b");
				if (other_b.isNavigable()) {
					/*
					 * Make the new property known to the model and its inClass
					 * only if it is navigable.
					 */
					m.register(other_b);
					((GenericClassInfo) other.inClass()).addProperty(other_b,
							PropertyCopyDuplicatBehaviorIndicator.OVERWRITE);
				}

				Type ti_other_b = new Type();
				ti_other_b.id = assocCi.id();
				ti_other_b.name = assocCi.name();
				other_b.setTypeInfo(ti_other_b);

				// F12->Feature2 | F34-Feature4
				GenericAssociationInfo ai_b = m.createCopy(association,
						association.id() + idSuffix + "_b");
				ai_b.setAssocClass(null);
				m.addAssociation(ai_b);

				nav_b.setAssociation(ai_b);
				other_b.setAssociation(ai_b);
				
				nav_b.setReverseProperty(other_b);
				other_b.setReverseProperty(nav_b);

				if (ai_b.end1() == navigableRole) {
					ai_b.setEnd1(nav_b);
					ai_b.setEnd2(other_b);
				} else {
					ai_b.setEnd1(other_b);
					ai_b.setEnd2(nav_b);
				}

				/*
				 * We want to place the new role(s) to the association class in
				 * a predictable way. On the one hand, the new role(s) should be
				 * placed behind all other properties that the association class
				 * may have. On the other hand, if two roles are added, then
				 * they are first sorted by their name first, and - if the names
				 * are equal (NOT ignoring case) by their id.
				 */
				Collections.sort(newPropsForAssociationClass,
						new Comparator<GenericPropertyInfo>() {
							public int compare(GenericPropertyInfo o1,
									GenericPropertyInfo o2) {
								int tmp = o1.name().compareTo(o2.name());
								if (tmp == 0)
									return o1.compareTo(o2);
								else
									return tmp;
							}
						});
				assocCi.addPropertiesAtBottom(newPropsForAssociationClass,
						PropertyCopyDuplicatBehaviorIndicator.OVERWRITE);
			}
		}
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.MessageSource#message(int)
	 */
	public String message(int mnr) {

		/**
		 * Number ranges defined as follows:
		 * <ul>
		 * <li>1-100: Initialization related messages</li>
		 * <li>101-200: Transformation related messages</li>
		 * <li>201-300: Other messages</li>
		 * <li>10001-10100: Exceptions
		 * </ul>
		 */

		switch (mnr) {

		case 0:
			return "Context: class AssociationClassMapper";

		// 1-100: Initialization related messages

		// 101-200: Transformation related messages
		case 101:
			return "";
		case 102:
			return "Association role: '$1$'";

		// 10001-10100: Exceptions

		default:
			return "(Unknown message)";
		}
	}
}
