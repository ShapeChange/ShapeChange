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
package de.interactive_instruments.ShapeChange.Transformation.Profiling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments <dot>
 *         de)
 */
public class ProfileIdentifierMap {

	private Map<String, ProfileIdentifier> profileIdentifiersByName;
	private String ownerName;

	private ProfileIdentifierMap(
			Map<String, ProfileIdentifier> profileIdentifiers, String ownerName) {
		this.profileIdentifiersByName = profileIdentifiers;
		this.ownerName = ownerName;
	}

	/**
	 * @param profilesString
	 * @param pattern
	 * @param ownerName
	 *            name of the model element that owns the profilesString
	 * @return
	 * @throws MalformedProfileIdentifierException
	 */
	public static ProfileIdentifierMap parse(String profilesString,
			ProfileIdentifier.IdentifierPattern pattern, String ownerName)
			throws MalformedProfileIdentifierException {

		String[] profileIdentifierStrings = profilesString.split(",");
		Map<String, ProfileIdentifier> profileIdentifiersByName = new HashMap<String, ProfileIdentifier>();

		Set<String> duplicateProfileIdentifierNames = new HashSet<String>();

		for (String profileIdentifierString : profileIdentifierStrings) {

			ProfileIdentifier profileIdentifier = ProfileIdentifier.parse(
					profileIdentifierString.trim(), pattern, ownerName);

			if (profileIdentifiersByName.containsKey(profileIdentifier
					.getName())) {

				duplicateProfileIdentifierNames
						.add(profileIdentifier.getName());

				// TBD: if desired, the version information could also be merged
			} else {
				profileIdentifiersByName.put(profileIdentifier.getName(),
						profileIdentifier);
			}
		}

		if (!duplicateProfileIdentifierNames.isEmpty()) {

			throw new MalformedProfileIdentifierException(
					"Duplicate profile name(s) encountered (in profile set owned by '"
							+ ownerName
							+ "': "
							+ StringUtils.join(duplicateProfileIdentifierNames,
									","));
		} else {
			return new ProfileIdentifierMap(profileIdentifiersByName, ownerName);
		}
	}

	public Map<String, ProfileIdentifier> getProfileIdentifiersByName() {
		return profileIdentifiersByName;
	}

	/**
	 * @return name of the model element that owns the profile map information
	 */
	public String getOwnerName() {
		return ownerName;
	}

	/**
	 * @param other, shall not be <code>null</code>
	 * @param messages
	 *            List to store the reason(s) why this map does not contain the
	 *            other map; can be null
	 * @return
	 */
	public boolean contains(ProfileIdentifierMap other, List<String> messages) {

		// identify which profiles from the other map are not contained in
		// this map
		Set<String> difference = new HashSet<String>(other
				.getProfileIdentifiersByName().keySet());
		difference.removeAll(profileIdentifiersByName.keySet());

		if (!difference.isEmpty()) {
			// we found identifiers in the other map that are not contained in
			// this map
			if (messages != null) {
				String s = StringUtils.join(difference, " ");
				messages.add("The profile set owned by '"
						+ this.getOwnerName()
						+ "' does not contain the following profiles owned by '"
						+ other.getOwnerName() + "': " + s);
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
				profileIdentifiersByName.values());

		for (int i = 0; i < identifiers.size() - 1; i++) {
			ProfileIdentifier pi = identifiers.get(i);
			sb.append(pi + ",");
		}
		sb.append(identifiers.get(identifiers.size() - 1));

		return sb.toString();
	}

}
