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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import de.interactive_instruments.ShapeChange.Model.MalformedProfileIdentifierException;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 */
public class Profiles {

	/**
	 * Name of the tagged value that contains the profile information.
	 */
	public static final String PROFILES_TAGGED_VALUE = "profiles";

	/**
	 * Regular expression to validate the structure of (a comma-separated list
	 * of) profiles given via the configuration. Each profile identifier must
	 * have an identifier, and may have a single version number but no
	 * parameters.
	 */
	public static final Pattern PATTERN_VALIDATE_PROFILES_FROM_CONFIG = Pattern
			.compile(
					"(\\w|-)+(\\[[0-9]+(\\.[0-9]+)*\\])?(,(\\w|-)+(\\[[0-9]+(\\.[0-9]+)*\\])?)*");

	/**
	 * Regular expression to validate the structure of a profiles tagged value.
	 * Profiles are separated by comma. Each profile has an identifier and may
	 * have a version indicator as well as parameters.
	 */
	public static final Pattern PATTERN_VALIDATE_PROFILES_TAGGED_VALUE = Pattern
			.compile(
					"([\\w|-]+)(?:\\[([-;\\.0-9]+)\\])?(?:\\((.*?)\\))?(,([\\w|-]+)(?:\\[([-;\\.0-9]+)\\])?(?:\\((.*?)\\))?)*");

	/**
	 * Pattern to find a string that defines a single profile. Group 1 contains
	 * the profile name, groups 2 and 3 are optional: group 2 would contain the
	 * version indicator, group 3 the profile parameters.
	 */
	public static final Pattern PATTERN_PARSE_PROFILE = Pattern
			.compile("([\\w|-]+)(?:\\[([\\.0-9-;]+)\\])?(?:\\((.*?)\\))?");

	/**
	 * Regex to validate the parameters of a profile (the parameters string must
	 * match this expression).
	 */
	public static final Pattern PATTERN_VALIDATE_PROFILE_PARAMETERS = Pattern
			.compile("\\w+(\\[.*?\\])?(,\\w+(\\[.*?\\])?)*");

	/**
	 * Pattern to find a string defining a parameter entry of a profile. Group 1
	 * contains the parameter name, group 2 contains the optional value (and can
	 * therefore be <code>null</code>).
	 */
	public static final Pattern PATTERN_PARSE_PROFILE_PARAMETERS = Pattern
			.compile("(\\w+)(?:\\[(.*?)\\])?");

	private SortedMap<String, ProfileIdentifier> profileIdentifiersByName = null;
	// private String ownerName;

	public Profiles(SortedMap<String, ProfileIdentifier> profileIdentifiers) {
		if (profileIdentifiers == null || profileIdentifiers.isEmpty()) {
			this.profileIdentifiersByName = null;
		} else {
			this.profileIdentifiersByName = profileIdentifiers;
		}
	}

	public Profiles(Collection<ProfileIdentifier> profileIdentifiers) {
		if (profileIdentifiers == null || profileIdentifiers.isEmpty()) {
			this.profileIdentifiersByName = null;
		} else {
			this.profileIdentifiersByName = new TreeMap<String, ProfileIdentifier>();
			for (ProfileIdentifier pi : profileIdentifiers) {
				this.profileIdentifiersByName.put(pi.getName(), pi);
			}
		}
	}
	

	/**
	 * Adds the given profile to this set of profiles. If the profiles
	 * previously contained a profile with the same name, the old profile is
	 * replaced by the given one.
	 * 
	 * @param profile
	 *            the profile to add to this set of profiles
	 * @return The previous profile, if this set already contained a profile
	 *         with the same name as the given profile, or null if there was no
	 *         such profile.
	 */
	public ProfileIdentifier put(ProfileIdentifier profile) {

		if (this.profileIdentifiersByName == null) {
			this.profileIdentifiersByName = new TreeMap<String, ProfileIdentifier>();
		}

		return this.profileIdentifiersByName.put(profile.getName(), profile);
	}

	/**
	 * Adds a new profile with the given name to this set of profiles. If the
	 * profiles previously contained a profile with the same name, the old
	 * profile is replaced by the given one.
	 * 
	 * @param profileName
	 *            the name of the new profile to add to this set of profiles
	 * @return The previous profile, if this set already contained a profile
	 *         with the same name as the given one, or null if there was no such
	 *         profile.
	 */
	public ProfileIdentifier put(String profileName) {

		if (this.profileIdentifiersByName == null) {
			this.profileIdentifiersByName = new TreeMap<String, ProfileIdentifier>();
		}

		ProfileIdentifier pi = new ProfileIdentifier(profileName, null, null);
		return this.profileIdentifiersByName.put(pi.getName(), pi);
	}

	public Profiles() {

	}

	/**
	 * @param profilesString
	 * @param isProfilesFromConfig
	 *            <code>true</code> if the profilesString is from the
	 *            configuration, <code>false</code> if it is from the tagged
	 *            value 'profiles
	 * @param ownerName
	 *            name of the model element that owns the profilesString
	 * @return
	 * @throws MalformedProfileIdentifierException
	 */
	public static Profiles parse(String profilesString,
			boolean isProfilesFromConfig
	// , String ownerName
	) throws MalformedProfileIdentifierException {

		Pattern validationPattern = isProfilesFromConfig
				? PATTERN_VALIDATE_PROFILES_FROM_CONFIG
				: PATTERN_VALIDATE_PROFILES_TAGGED_VALUE;

		// validate profiles string
		Matcher profilesValidator = validationPattern.matcher(profilesString);

		if (!profilesValidator.matches()) {
			throw new MalformedProfileIdentifierException(
					"Profiles value does not match regular expression "
							+ validationPattern.pattern());
		}

		// now find occurrences of profiles
		SortedMap<String, ProfileIdentifier> profileIdentifiersByName = new TreeMap<String, ProfileIdentifier>();

		SortedSet<String> duplicateProfileIdentifierNames = new TreeSet<String>();

		Matcher profilesParser = PATTERN_PARSE_PROFILE.matcher(profilesString);

		while (profilesParser.find()) {

			String profileName = profilesParser.group(1);

			if (profileIdentifiersByName.containsKey(profileName)) {

				duplicateProfileIdentifierNames.add(profileName);

				// TBD: if desired, the version information could also be merged

			} else {

				// NOTE: versionIndicator and parameters can be null
				String versionIndicatorString = profilesParser.group(2);
				String parametersString = profilesParser.group(3);

				// parse version indicator
				ProfileVersionIndicator versionIndicator = null;

				if (versionIndicatorString != null) {
					versionIndicator = ProfileVersionIndicator
							.parse(versionIndicatorString, profileName);
				}
				// else {
				// String versionValue = "0-" + Integer.MAX_VALUE;
				// versionIndicator = ProfileVersionIndicator
				// .parse(versionValue, profileName);
				// }

				// parse parameter
				SortedMap<String, String> parameters = null;

				if (parametersString != null) {

					Matcher parametersValidator = PATTERN_VALIDATE_PROFILE_PARAMETERS
							.matcher(parametersString);

					if (!parametersValidator.matches()) {
						throw new MalformedProfileIdentifierException(
								"The parameters part of profile '" + profileName
										+ "' does not match regular expression "
										+ parametersValidator.pattern()
										+ " The given parameters value was: "
										+ parametersString);
					}

					parameters = new TreeMap<String, String>();

					Matcher parameterParser = PATTERN_PARSE_PROFILE_PARAMETERS
							.matcher(parametersString);

					while (parameterParser.find()) {

						String parameterName = parameterParser.group(1);
						// NOTE: the value can be null
						String parameterValue = parameterParser.group(2);
						parameters.put(parameterName, parameterValue);
					}
				}

				ProfileIdentifier profileIdentifier = new ProfileIdentifier(
						profileName, versionIndicator, parameters);

				profileIdentifiersByName.put(profileName, profileIdentifier);
			}

		}

		if (!duplicateProfileIdentifierNames.isEmpty()) {

			throw new MalformedProfileIdentifierException(
					"Duplicate profile name(s) encountered: " + StringUtils
							.join(duplicateProfileIdentifierNames, ","));
		} else {
			return new Profiles(profileIdentifiersByName);
		}
	}

	/**
	 * @return can be empty but not <code>null</code>
	 */
	public SortedMap<String, ProfileIdentifier> getProfileIdentifiersByName() {
		if (this.profileIdentifiersByName == null) {
			return new TreeMap<String, ProfileIdentifier>();
		} else {
			return profileIdentifiersByName;
		}
	}

	/**
	 * @return A new list referencing the available profile identifiers
	 *         (supports modification of individual profile identifiers, but not
	 *         of the set of profile identifiers contained by this object); can
	 *         be empty but not <code>null</code>.
	 */
	public SortedSet<ProfileIdentifier> getProfileIdentifiers() {

		SortedSet<ProfileIdentifier> result = new TreeSet<ProfileIdentifier>();

		if (this.profileIdentifiersByName != null) {
			result.addAll(this.profileIdentifiersByName.values());
		}

		return result;
	}
	//
	// /**
	// * @return name of the model element that owns the profile map information
	// */
	// public String getOwnerName() {
	// return ownerName;
	// }

	/**
	 * @param other,
	 *            shall not be <code>null</code>
	 * @param messages
	 *            List to store the reason(s) why this map does not contain the
	 *            other map; can be null
	 * @return
	 */
	public boolean contains(Profiles other, List<String> messages) {

		// identify which profiles from the other map are not contained in
		// this map
		Set<String> difference = new HashSet<String>(
				other.getProfileIdentifiersByName().keySet());
		difference.removeAll(profileIdentifiersByName.keySet());

		if (!difference.isEmpty()) {
			// we found identifiers in the other map that are not contained in
			// this map
			if (messages != null) {
				String s = StringUtils.join(difference, " ");
				messages.add(
						"These profiles do not contain the following other profiles: "
								+ s);
			}
			return false;
		}

		boolean result = true;

		// Now that we know that at least the names of the other profile
		// identifiers are contained in this map, also ensure that the
		// versions stated for other identifiers are contained in the
		// identifiers of this map.
		for (String key : other.getProfileIdentifiersByName().keySet()) {

			ProfileIdentifier pIdThis = this.profileIdentifiersByName.get(key);
			ProfileIdentifier pIdOther = other.getProfileIdentifiersByName()
					.get(key);

			if (!pIdThis.contains(pIdOther, messages)) {
				result = false;
				// we do not break here to identify and log all possible issues
				// with the identifiers
			}
		}
		return result;

	}

	public String toString() {

		StringBuffer sb = new StringBuffer();

		List<ProfileIdentifier> identifiers = new ArrayList<ProfileIdentifier>(
				this.getProfileIdentifiersByName().values());

		for (int i = 0; i < identifiers.size() - 1; i++) {
			ProfileIdentifier pi = identifiers.get(i);
			sb.append(pi + ",");
		}
		sb.append(identifiers.get(identifiers.size() - 1));

		return sb.toString();
	}

	public Profiles createCopy() {

		SortedMap<String, ProfileIdentifier> copy = new TreeMap<String, ProfileIdentifier>();

		for (Entry<String, ProfileIdentifier> entry : this
				.getProfileIdentifiersByName().entrySet()) {

			ProfileIdentifier piCopy = entry.getValue().createCopy();
			copy.put(entry.getKey(), piCopy);
		}

		return new Profiles(copy);
	}

	public boolean isEmpty() {

		return this.profileIdentifiersByName == null
				|| this.profileIdentifiersByName.isEmpty();
	}

	/**
	 * Checks if this set of profiles contains another set of profiles.
	 * <p>
	 * Takes into account whether or not the rule for explicit profile settings
	 * is enabled, which matters in case that a model element has no profile
	 * information.
	 * <p>
	 * Useful for:
	 * <ul>
	 * <li>Checking if the profile information for a property is contained in
	 * the profile set of its class (profileInheritance: true).</li>
	 * <li>Checking if the profiles of the subtypes of a class are contained in
	 * that classes profiles (profileInheritance: false).</li>
	 * <li>Checking if the target profiles provided via the configuration are
	 * contained in the profile information of a class (profileInheritance:
	 * false).</li>
	 * </ul>
	 * 
	 * Profile inheritance: if the other profiles are empty but these profiles
	 * are not, the other profiles/owner inherit these profiles. This is
	 * irrelevant if explicit profile settings is enabled.
	 * 
	 * @param ownerName
	 *            Name of the owner of these profiles
	 * @param otherProfiles
	 *            profile map that is contained in these profiles (or not)
	 * @param otherOwnerName
	 *            Name of the owner of the other profiles
	 * @param isExplicitProfileSettings
	 *            <code>true</code> if profiles are explicitly stated (a lack of
	 *            profile assignments for a model element thus means that the
	 *            element belongs to no profile, and would thus be removed by
	 *            the profiler), else <code>false</code> (in that case, a lack
	 *            of profile assignments for a model element would mean that it
	 *            belongs to all profiles)
	 * @param profileInheritance
	 *            true if profile inheritance shall be applied, else false
	 *            (irrelevant if rule for explicit profile settings is enabled)
	 * @param messages
	 *            used to log the reason(s) why these profiles do not contain
	 *            the other profiles; can be <code>null</code>
	 * @return <code>true</code> if these profiles contain the other profiles
	 */
	public boolean contains(String ownerName, Profiles otherProfiles,
			String otherOwnerName, boolean isExplicitProfileSettings,
			boolean profileInheritance, List<String> messages) {

		if (this.isEmpty() && otherProfiles.isEmpty()) {

			return true;

		} else if (this.isEmpty() && !otherProfiles.isEmpty()) {

			if (isExplicitProfileSettings) {

				/*
				 * This is empty while other profiles are not; the empty set
				 * does not contain a non-empty set:
				 */
				return false;

			} else {

				/*
				 * This is unlimited and thus contains other profiles (which are
				 * limited):
				 */
				return true;
			}

		} else if (!this.isEmpty() && otherProfiles.isEmpty()) {

			if (isExplicitProfileSettings) {

				/*
				 * Other profiles is empty while these profiles are not; a
				 * non-empty set always contains the empty set
				 */
				return true;

			} else {

				/*
				 * Now it depends if profile inheritance shall be applied or not
				 * This is the case for properties but not for subtypes, for
				 * example.
				 */
				if (profileInheritance) {
					// Ok, other profiles inherit these profiles:
					return true;
				} else {
					/*
					 * As profile inheritance is false, other profiles are
					 * unlimited while these profiles are not, therefore these
					 * profiles doe not contain the other profiles:
					 */
					if (messages != null) {
						messages.add("The profiles owned by '" + ownerName
								+ "' do not contain the profiles owned by '"
								+ otherOwnerName
								+ "' because the latter does not inherit the profiles from the former, and because the latter has an unlimited profile set while the former does not.");
					}
					return false;
				}
			}

		} else {
			// Both profiles are limited, thus compare their contents
			return this.contains(otherProfiles, messages);
		}
	}

	/**
	 * @return number of contained profiles
	 */
	public int size() {
		if (this.profileIdentifiersByName == null) {
			return 0;
		} else {
			return this.profileIdentifiersByName.size();
		}
	}

	/**
	 * @param index
	 *            0-based
	 * @return profile identifier at given index, can be <code>null</code> if
	 *         the set of profiles is empty or if the index is not in range of
	 *         the set of profiles
	 */
	public ProfileIdentifier get(int index) {

		if (isEmpty()) {
			return null;
		} else {
			int i = 0;
			for (ProfileIdentifier pi : this.profileIdentifiersByName
					.values()) {
				if (i == index) {
					return pi;
				}
			}

			// index is not in range of the set of profiles
			return null;
		}
	}

	/**
	 * @param profileName
	 * @return the profile identifier with given name stored in this set of
	 *         profiles; can be <code>null</code> if no profile with that name
	 *         exists in this set
	 */
	public ProfileIdentifier getProfile(String profileName) {
		if (this.profileIdentifiersByName == null) {
			return null;
		} else {
			return this.profileIdentifiersByName.get(profileName);
		}
	}

	/**
	 * @param profileNames
	 * @return the set of profile identifiers whose names are contained in the
	 *         given set; can be empty (especially if the given set is
	 *         <code>null</code> or empty) but not <code>null</code>
	 */
	public SortedSet<ProfileIdentifier> getProfiles(Set<String> profileNames) {

		SortedSet<ProfileIdentifier> result = new TreeSet<ProfileIdentifier>();

		if (profileNames != null && this.profileIdentifiersByName != null) {

			for (Entry<String, ProfileIdentifier> profileEntry : this.profileIdentifiersByName
					.entrySet()) {

				if (profileNames.contains(profileEntry.getKey())) {
					result.add(profileEntry.getValue());
				}
			}
		}

		return result;
	}

	/**
	 * @param profileName
	 * @param parameterName
	 * @return the value of the profile parameter with given name, stored for
	 *         the profile with given name; can be <code>null</code> if the
	 *         profile does not exist, or the profile does not have any
	 *         parameters, or the profile does not define that parameter, or
	 *         that parameter has no value
	 */
	public String getProfileParameter(String profileName,
			String parameterName) {

		if (profileName != null && parameterName != null) {

			ProfileIdentifier profile = this.getProfile(profileName);

			if (profile != null && profile.hasParameters()) {

				return profile.getParameter().get(parameterName);
			}
		}

		return null;
	}

	public void put(Set<ProfileIdentifier> profiles) {
		
		for(ProfileIdentifier profile : profiles) {
			this.put(profile);
		}		
	}
}
