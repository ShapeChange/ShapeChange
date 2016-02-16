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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments <dot>
 *         de)
 */
public class ProfileIdentifier {

	private String name;
	private ProfileVersionIndicator versionIndicator;
	private String ownerName;

	/**
	 * Contains the regular expression that defines the structure of a profile
	 * identifier, which usually is part of a (comma-separated) list and
	 * provided for model elements (such as classes and properties).
	 */
	public static final Pattern profileIdentifierPatternLoose = Pattern
			.compile("(\\w|-)+(\\[(-[0-9]+(\\.[0-9]+)*|[0-9]+(\\.[0-9]+)*-[0-9]+(\\.[0-9]+)*|[0-9]+(\\.[0-9]+)*-|[0-9]+(\\.[0-9]+)*)(;(-[0-9]+(\\.[0-9]+)*|[0-9]+(\\.[0-9]+)*-[0-9]+(\\.[0-9]+)*|[0-9]+(\\.[0-9]+)*-|[0-9]+(\\.[0-9]+)*))*\\])?");
	/**
	 * Contains the regular expression that allows only a single version number
	 * to be given for a profile identifier.
	 */
	public static final Pattern profileIdentifierPatternStrict = Pattern
			.compile("(\\w|-)+(\\[[0-9]+(\\.[0-9]+)*\\])?");

	public enum IdentifierPattern {
		loose, strict
	}

	private ProfileIdentifier(String name,
			ProfileVersionIndicator versionIndicator, String ownerName) {

		this.name = name;
		this.versionIndicator = versionIndicator;
		this.ownerName = ownerName;

	}

	/**
	 * Parses the given identifier and creates a ProfileIdentifier from it.
	 * 
	 * @param identifier
	 *            the profile identifier content to parse
	 * @param pattern
	 *            one of the ProfileIdentifierPattern enumerations or null in
	 *            case that the identifier shall not be checked against regular
	 *            expressions
	 * @return a validated profile identifier
	 * @throws MalformedProfileIdentifierException
	 *             If the given identifier was not well-formed.
	 */
	public static ProfileIdentifier parse(String identifier,
			IdentifierPattern pattern, String ownerName)
			throws MalformedProfileIdentifierException {

		if (pattern != null) {
			// Check that identifier matches regular expression.
			Matcher matcher = null;
			if (pattern.equals(IdentifierPattern.strict)) {
				matcher = profileIdentifierPatternStrict.matcher(identifier);
			} else if (pattern.equals(IdentifierPattern.loose)) {
				matcher = profileIdentifierPatternLoose.matcher(identifier);
			}

			if (!matcher.matches()) {
				throw new MalformedProfileIdentifierException("Profile '"
						+ identifier + "' [owned by " + ownerName
						+ "] does not match regular expression "
						+ matcher.pattern().pattern());
			}
		}

		boolean hasVersionIndicator = identifier.contains("[");
		ProfileVersionIndicator versionIndicator = null;
		String name = null;

		if (hasVersionIndicator) {
			String[] tmp = identifier.split("\\[");
			name = tmp[0];
			String versionValue = tmp[1].substring(0, tmp[1].length() - 1);
			versionIndicator = ProfileVersionIndicator.parse(versionValue,
					name, ownerName);
		} else {
			name = identifier;
			String versionValue = "0-" + Integer.MAX_VALUE;
			versionIndicator = ProfileVersionIndicator.parse(versionValue,
					name, ownerName);
		}

		return new ProfileIdentifier(name, versionIndicator, ownerName);
	}

	public String getName() {
		return name;
	}

	/**
	 * @return name of the model element that owns the profile identifier information
	 */
	public String getOwnerName() {
		return ownerName;
	}

	public ProfileVersionIndicator getVersionIndicator() {
		return versionIndicator;
	}

	public String toString() {
		return name + "[" + versionIndicator + "]";
	}

	/**
	 * @param other
	 * @param messages
	 *            List to store the reason(s) why this identifier does not
	 *            contain the other identifier; can be null
	 * @return
	 */
	public boolean contains(ProfileIdentifier other, List<String> messages) {

		if (!this.name.equalsIgnoreCase(other.getName())) {
			if (messages != null) {
				messages.add("Profile names do not match (this [owned by '"
						+ this.getOwnerName() + "']: " + this.getName()
						+ ", other [owned by '" + other.getOwnerName() + "']: "
						+ other.getName() + ").");
			}
			return false;
		} else {
			// Check the version indicator
			boolean doesContain = true;
			StringBuffer sb = null;

			if (messages != null) {
				sb = new StringBuffer();
				doesContain = this.versionIndicator.contains(
						other.getVersionIndicator(), sb);
			} else {
				doesContain = this.versionIndicator.contains(
						other.getVersionIndicator(), null);
			}
			if (!doesContain) {
				if (messages != null) {
					messages.add("The following version ranges from profile '"
							+ other.getName() + "' (owned by '"
							+ other.getOwnerName()
							+ "') are not contained in profile '"
							+ this.getName() + "' (owned by '"
							+ this.getOwnerName() + "'): " + sb + ".");
				}
				return false;
			} else {
				return true;
			}
		}
	}

}
