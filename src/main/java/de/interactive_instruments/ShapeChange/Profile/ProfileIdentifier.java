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
package de.interactive_instruments.ShapeChange.Profile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.google.common.base.Joiner;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 */
public class ProfileIdentifier implements Comparable<ProfileIdentifier> {

	private static final Joiner commaJoiner = Joiner.on(",").skipNulls();

	private String name;
	private ProfileVersionIndicator versionIndicator;

	/**
	 * NOTE: this map can be <code>null</code> if no parameter is defined for
	 * the profile
	 * 
	 * Key: parameter name Value: parameter value, can be <code>null</code> if
	 * no value is present
	 */
	private SortedMap<String, String> parameters;

	/**
	 * @param name tbd
	 * @param versionIndicator
	 *            can be <code>null</code>
	 * @param parameters
	 *            parameters defined for the profile; can be <code>null</code>
	 *            if no parameters are defined
	 */
	public ProfileIdentifier(String name,
			ProfileVersionIndicator versionIndicator,
			SortedMap<String, String> parameters) {

		this.name = name;
		this.versionIndicator = versionIndicator;

		if (parameters == null || parameters.isEmpty()) {
			this.parameters = null;
		} else {
			this.parameters = parameters;
		}
	}

	/**
	 * @return parameter map (can be empty but not null) with key: parameter
	 *         name, value: parameter value, can be <code>null</code> if no
	 *         value is present
	 */
	public SortedMap<String, String> getParameter() {

		if (this.parameters == null) {
			return new TreeMap<String, String>();
		} else {
			return this.parameters;
		}
	}

	public String getName() {
		return name;
	}

	/**
	 * @return the version indicator for this profile; can be <code>null</code>
	 *         if no version was specified (meaning that the profile identifier
	 *         applies to all profiles)
	 */
	public ProfileVersionIndicator getVersionIndicator() {
		return versionIndicator;
	}

	public String toString() {

		StringBuffer sb = new StringBuffer();

		sb.append(name);
		if (this.versionIndicator != null) {
			sb.append("[" + versionIndicator.toString() + "]");
		}
		if (this.parameters != null && !this.parameters.isEmpty()) {

			sb.append("(");

			List<String> parameterValues = new ArrayList<String>();

			for (Entry<String, String> entry : this.parameters.entrySet()) {

				if (entry.getValue() != null) {
					parameterValues
							.add(entry.getKey() + "[" + entry.getValue() + "]");
				} else {
					parameterValues.add(entry.getKey());
				}
			}

			sb.append(commaJoiner.join(parameterValues));

			sb.append(")");
		}

		return sb.toString();
	}

	/**
	 * @param other tbd
	 * @param messages
	 *            List to store the reason(s) why this identifier does not
	 *            contain the other identifier; can be null
	 * @return tbd
	 */
	public boolean contains(ProfileIdentifier other, List<String> messages) {

		if (!this.name.equalsIgnoreCase(other.getName())) {

			if (messages != null) {
				messages.add(
						"Profile names do not match (this: " + this.getName()
								+ ", other: " + other.getName() + ").");
			}
			return false;

		} else {

			// Check the version indicator
			boolean doesContain = true;

			/*
			 * the versionIndicator of a ProfileIdentifier can be null, if so
			 * use the maximum range for the comparison
			 */
			ProfileVersionIndicator thisPVI = this.hasVersionIndicator()
					? this.versionIndicator
					: ProfileVersionIndicator.MAX_RANGE_VERSION_INDICATOR;

			ProfileVersionIndicator otherPVI = other.hasVersionIndicator()
					? other.getVersionIndicator()
					: ProfileVersionIndicator.MAX_RANGE_VERSION_INDICATOR;

			StringBuffer sb = null;

			if (messages != null) {
				sb = new StringBuffer();
				doesContain = thisPVI.contains(otherPVI, sb);
			} else {
				doesContain = thisPVI.contains(otherPVI, null);
			}
			if (!doesContain) {
				if (messages != null) {
					messages.add(
							"The following version ranges from the other profile '"
									+ other.getName()
									+ "' are not contained in this profile '"
									+ this.getName() + "': " + sb + ".");
				}
				return false;
			} else {
				return true;
			}
		}
	}

	public ProfileIdentifier createCopy() {

		SortedMap<String, String> parameterCopy = new TreeMap<String, String>();
		for (Entry<String, String> parameterEntry : this.getParameter()
				.entrySet()) {
			parameterCopy.put(parameterEntry.getKey(),
					parameterEntry.getValue());
		}

		ProfileVersionIndicator pviCopy = null;
		if (this.versionIndicator != null) {
			pviCopy = this.versionIndicator.createCopy();
		}

		return new ProfileIdentifier(this.name, pviCopy, parameterCopy);
	}

	public boolean hasVersionIndicator() {
		return this.versionIndicator != null;
	}

	/**
	 * @return <code>true</code> if one or more parameters are defined for the
	 *         profile, else <code>false</code>
	 */
	public boolean hasParameters() {
		return this.parameters != null && !this.parameters.isEmpty();
	}

	/**
	 * NOTE: Comparison is based on the textual representation of the profile
	 * identifiers.
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object other) {

		if (other == null) {
			return false;
		} else if (other == this) {
			return true;
		} else {
			return this.toString().equals(other.toString());
		}
	}

	/**
	 * NOTE: Comparison is based on the textual representation of the profile
	 * identifiers.
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(ProfileIdentifier other) {

		if (other == null) {
			throw new NullPointerException();
		} else {
			return this.toString().compareTo(other.toString());
		}
	}

}
