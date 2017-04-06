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
package de.interactive_instruments.ShapeChange.Profile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Multiplicity;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.AssociationInfo;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class ModelProfileValidator implements MessageSource {

	private static final Splitter commaSplitter = Splitter.on(',')
			.omitEmptyStrings().trimResults();
	private static final Joiner commaJoiner = Joiner.on(", ").skipNulls();

	private Model model;
	private ShapeChangeResult result;

	// private Map<String, Profiles> profilesByClassId = new TreeMap<String,
	// Profiles>();
	// private Map<String, Profiles> profilesByPropertyId = new TreeMap<String,
	// Profiles>();

	public ModelProfileValidator(Model model, ShapeChangeResult result) {

		this.model = model;
		this.result = result;

		// HashSet<PackageInfo> packages = model.packages();
		//
		// for (PackageInfo pkg : packages) {
		//
		// for (ClassInfo ci : pkg.containedClasses()) {
		//
		// this.profilesByClassId.put(ci.id(), ci.profiles());
		//
		// for (PropertyInfo pi : ci.properties().values()) {
		//
		// this.profilesByPropertyId.put(pi.id(), pi.profiles());
		// }
		// }
		// }
	}

	/**
	 * Checks that the 'profiles' information within the model is consistent.
	 * 
	 * NOTE: The creation of a generalization relationship between model classes
	 * and versioning of this model change is currently not taken into account
	 * via the profiling mechanism, because adding the 'profiles' tagged value
	 * to generalization relationships is currently not foreseen.
	 * 
	 * Checks that the profile set of a supertype contains the profile set of
	 * its subtypes. Also checks that the profile set of a class contains the
	 * profile sets of its properties (unless explicit profile settings is
	 * enabled and the class does not belong to a profile).In both cases,
	 * undefined/empty 'profiles' are taken into acocunt: behavior is different
	 * depending upon whether or not explicit profile settings is enabled. If it
	 * is <code>true</code>, classes and properties without profile information
	 * belong to no profile. If it is <code>false</code> then classes belong to
	 * all profiles and properties inherit the profile set from their owner.
	 * 
	 * Furthermore, the consistency of the following profile parameters is
	 * checked: geometry and multiplicity.
	 * 
	 * Non-navigable properties belonging to the class are ignored.
	 * 
	 * @param isExplicitProfileSettings
	 * @param warnIfSupertypeProfilesDoNotContainSubtypeProfiles
	 */
	public void validateModelConsistency(boolean isExplicitProfileSettings,
			boolean warnIfSupertypeProfilesDoNotContainSubtypeProfiles) {

		HashSet<PackageInfo> packages = model.packages();

		/*
		 * Keep track of associations to check if they would no longer be
		 * navigable when profile parameter 'isNavigable' would be applied.
		 */
		SortedSet<AssociationInfo> associations = new TreeSet<AssociationInfo>();

		for (PackageInfo pkg : packages) {

			for (ClassInfo ci : pkg.containedClasses()) {

				// validate profile consistency on subtypes
				SortedSet<String> subtypeIds = ci.subtypes();

				for (String subtypeId : subtypeIds) {

					ClassInfo subtype = model.classById(subtypeId);

					// used for logging messages
					List<String> messages = new ArrayList<String>();

					if (!ci.profiles().contains(ci.name(), subtype.profiles(),
							subtype.name(), isExplicitProfileSettings, false,
							messages)) {

						/*
						 * This can be dangerous in case that the subtype has
						 * constraints from properties of its supertype. In such
						 * a case the constraints won't have a proper context
						 * (because the properties they are referring to may not
						 * exist in case that the supertype is omitted in a
						 * profile for which the subtype remains).
						 * 
						 * However, if all subtypes would be removed as well
						 * then it would be ok.
						 */

						if (warnIfSupertypeProfilesDoNotContainSubtypeProfiles) {

							/*
							 * as all subtypes will be removed this is not
							 * problematic - just log a warning
							 */
							result.addWarning(this, 100, ci.name(),
									subtype.name(),
									StringUtils.join(messages, " "));

						} else {

							result.addError(this, 100, ci.name(),
									subtype.name(),
									StringUtils.join(messages, " "));
						}
					}
				}

				// validate geometry profile parameter
				String geometryTV = ci.taggedValue("geometry");
				if (geometryTV != null) {

					// identify set of geometries from tagged value
					SortedSet<String> geometryTVValues = new TreeSet<String>(
							commaSplitter.splitToList(geometryTV));

					for (ProfileIdentifier profile : ci.profiles()
							.getProfileIdentifiers()) {

						if (profile.hasParameters()) {

							String geometryProfile = profile.getParameter()
									.get("geometry");

							if (geometryProfile != null) {

								SortedSet<String> geometryProfileValues = new TreeSet<String>(
										commaSplitter
												.splitToList(geometryProfile));

								/*
								 * first check that sets intersect, then check
								 * if geometry profile values are not fully
								 * contained in geometry tagged value of class
								 */
								SetView<String> geometryIntersection = Sets
										.intersection(geometryProfileValues,
												geometryTVValues);

								if (geometryIntersection.isEmpty()) {
									MessageContext mc = result.addWarning(this,
											104, profile.getName(), ci.name(),
											geometryTV, geometryProfile);
									if (mc != null) {
										mc.addDetail(this, 1, ci.fullName());
									}

								} else {

									SetView<String> geometryDiff = Sets
											.difference(geometryProfileValues,
													geometryTVValues);

									if (!geometryDiff.isEmpty()) {
										MessageContext mc = result.addWarning(
												this, 101, profile.getName(),
												ci.name(),
												commaJoiner.join(geometryDiff));
										if (mc != null) {
											mc.addDetail(this, 1,
													ci.fullName());
										}
									}
								}
							}
						}
					}
				}

				// validate profile consistency of class properties
				for (PropertyInfo pi : ci.properties().values()) {

					if (!pi.isAttribute()) {
						associations.add(pi.association());
					}

					List<String> messages = new ArrayList<String>();

					if (isExplicitProfileSettings && ci.profiles().isEmpty()) {

						// this is allowed

					} else if (!ci.profiles().contains(ci.name(), pi.profiles(),
							pi.name() + "(in class " + pi.inClass() + ")",
							isExplicitProfileSettings, true, messages)) {

						result.addWarning(null, 20204, ci.name(), pi.name(),
								StringUtils.join(messages, " "));
					}

					// validate parameters
					for (ProfileIdentifier profile : pi.profiles()
							.getProfileIdentifiers()) {

						if (profile.hasParameters()) {

							// validate multiplicity parameter
							String mult = profile.getParameter()
									.get("multiplicity");

							if (mult != null) {

								Multiplicity multProfile = new Multiplicity(
										mult);

								if (multProfile.minOccurs < pi
										.cardinality().minOccurs
										|| multProfile.maxOccurs > pi
												.cardinality().maxOccurs) {
									MessageContext mc = result.addWarning(this,
											102, profile.getName(), pi.name(),
											ci.name());
									if (mc != null) {
										mc.addDetail(this, 2, pi.fullName());
									}
								}
							}

							// validate isNavigable parameter
							String isNavigable = profile.getParameter()
									.get("isNavigable");

							if (isNavigable != null) {

								if (pi.isAttribute()) {
									MessageContext mc = result.addWarning(this,
											103, profile.getName(), pi.name(),
											ci.name());
									if (mc != null) {
										mc.addDetail(this, 2, pi.fullName());
									}
								}
							}
						}
					}
				}
			}
		}

		for (AssociationInfo ai : associations) {

			// get set of profiles from both association ends

			SortedSet<String> profileNames = new TreeSet<String>();

			profileNames.addAll(ai.end1().profiles()
					.getProfileIdentifiersByName().keySet());
			profileNames.addAll(ai.end2().profiles()
					.getProfileIdentifiersByName().keySet());

			for (String profileName : profileNames) {

				boolean end1IsNavigableInProfile = ai.end1().isNavigable();
				boolean end2IsNavigableInProfile = ai.end2().isNavigable();

				String end1IsNavigableProfileParameterValue = ai.end1()
						.profiles()
						.getProfileParameter(profileName, "isNavigable");
				String end2IsNavigableProfileParameterValue = ai.end2()
						.profiles()
						.getProfileParameter(profileName, "isNavigable");

				if (end1IsNavigableProfileParameterValue != null) {
					end1IsNavigableInProfile = Boolean
							.parseBoolean(end1IsNavigableProfileParameterValue);
				}
				if (end2IsNavigableProfileParameterValue != null) {
					end2IsNavigableInProfile = Boolean
							.parseBoolean(end2IsNavigableProfileParameterValue);
				}

				/*
				 * If both ends would no longer be navigable, log a warning.
				 */
				if (!(end1IsNavigableInProfile || end2IsNavigableInProfile)) {
					MessageContext mc = result.addWarning(this, 105,
							profileName);
					if (mc != null) {
						mc.addDetail(this, 3, ai.end1().fullName());
						mc.addDetail(this, 4, ai.end2().fullName());
					}
				}
			}
		}
	}

	public String message(int mnr) {

		switch (mnr) {
		case 1:
			return "Context: class $1$";
		case 2:
			return "Context: property $1$";
		case 3:
			return "Context: association role (end1) '$1$'";
		case 4:
			return "Context: association role (end2) '$1$'";
		case 100:
			return "The profile set of class '$1$' does not contain the profile set of its subtype '$2$': $3$.";
		case 101:
			return "The geometry values defined in the parameter of profile '$1$' are not fully contained in the set of geometries defined for the class '$2$' (via tagged value 'geometry'). The following values are missing in the geometry definition of the class: $3$.";
		case 102:
			return "The multiplicity value defined in the parameter of profile '$1$' is (at least partly) outside of the range defined by the multiplicity of property '$2$' in class '$3$'.";
		case 103:
			return "The isNavigable parameter of profile '$1$' cannot be applied to property '$2$' in class '$3$' because that property is an attribute.";
		case 104:
			return "None of the geometry values defined in the parameter of profile '$1$' is contained in the set of geometries defined for the class '$2$' (via tagged value 'geometry'). The geometry definition of the class is: $3$. The geometry definition of the profile is: $4$.";
		case 105:
			return "The isNavigable parameter of profile '$1$' on the ends of the association will result in the whole association to no longer be navigable. It is better practice to completely remove the properties from the profile, instead of using the 'isNavigable' profile parameter to do so.";

		default:
			return "(Unknown message in " + this.getClass().getName()
					+ ". Message number was: " + mnr + ")";
		}
	}
}
