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
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments <dot>
 *         de)
 */
public class ProfileVersionIndicator {

	private List<VersionRange> versionInfos = null;

	public ProfileVersionIndicator(List<VersionRange> versionInfos) {
		this.versionInfos = versionInfos;
	}

	/**
	 * @param versionInformation
	 * @param identifierName
	 *            name of the profile identifier this version indicator belongs
	 *            to
	 * @param ownerName
	 *            name of the model element that the profile identifier belongs
	 *            to
	 * @return
	 * @throws MalformedProfileIdentifierException
	 */
	public static ProfileVersionIndicator parse(String versionInformation,
			String identifierName, String ownerName)
			throws MalformedProfileIdentifierException {

		String[] versionValues = versionInformation.split(";");
		List<VersionRange> versionInfosTmp = new ArrayList<VersionRange>();

		for (String versionValue : versionValues) {
			if (versionValue.contains("-")) {
				// construct a VersionRange
				VersionNumber begin = null;
				VersionNumber end = null;
				if (versionValue.indexOf("-") == 0) {
					begin = new VersionNumber(0);
					end = new VersionNumber(versionValue.substring(1,
							versionValue.length()));
				} else if (versionValue.indexOf("-") == versionValue.length() - 1) {
					begin = new VersionNumber(versionValue.substring(0,
							versionValue.length() - 1));
					end = new VersionNumber(Integer.MAX_VALUE);
				} else {
					begin = new VersionNumber(versionValue.substring(0,
							versionValue.indexOf("-")));
					end = new VersionNumber(versionValue.substring(
							versionValue.indexOf("-") + 1,
							versionValue.length()));

					// check that start is lower than end
					// start being greater than or equal to end is invalid
					if (begin.compareTo(end) >= 0) {
						throw new MalformedProfileIdentifierException(
								"Begin of version range '"
										+ versionValue
										+ "' is not lower than its end (in profile '"
										+ identifierName + "' owned by '"
										+ ownerName + "').");
					}
				}
				VersionRange vr = new VersionRange(begin, end);
				versionInfosTmp.add(vr);
			} else {
				// translate the given version number into a version range
				VersionNumber begin = new VersionNumber(versionValue);
				VersionNumber end = begin.copyForVersionRangeEnd();
				VersionRange vr = new VersionRange(begin, end);
				versionInfosTmp.add(vr);
			}
		}

		// merge version ranges that are not before each other (so they are not
		// disjoint)
		ProfileVersionIndicator pVIndicator = null;

		if (versionInfosTmp.size() == 0) {
			throw new MalformedProfileIdentifierException(
					"Version information '" + versionInformation
							+ "' could not be parsed (in profile '"
							+ identifierName + "' owned by '" + ownerName
							+ "').");
		} else if (versionInfosTmp.size() == 1) {
			pVIndicator = new ProfileVersionIndicator(versionInfosTmp);
		} else {

			// merge version ranges
			List<VersionRange> versionInfos = new ArrayList<VersionRange>();

			while (!versionInfosTmp.isEmpty()) {

				VersionRange tmp = versionInfosTmp.get(0);
				versionInfosTmp.remove(0);

				boolean mergeOccurred;

				/*
				 * Merge the tmp version range with one of the other ranges from
				 * the list. If no merge occurred, this procedure is complete
				 * for the tmp version range.
				 */
				do {
					mergeOccurred = false;

					for (int i = 0; i < versionInfosTmp.size(); i++) {

						VersionRange tmp2 = versionInfosTmp.get(i);

						if (tmp2.before(tmp) || tmp2.after(tmp)) {
							// nothing to do
						} else {
							// create the union of tmp and tmp2
							mergeOccurred = true;
							tmp = tmp.union(tmp2);
							versionInfosTmp.remove(i);
						}
					}
				} while (mergeOccurred);

				versionInfos.add(tmp);

				if (versionInfosTmp.size() == 1) {
					versionInfos.add(versionInfosTmp.remove(0));
				}
			}

			pVIndicator = new ProfileVersionIndicator(versionInfos);
		}

		return pVIndicator;
	}

	public List<VersionRange> getVersionInfos() {
		return versionInfos;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < versionInfos.size(); i++) {
			sb.append(versionInfos.get(i));
			if (i < versionInfos.size() - 1) {
				sb.append(";");
			}
		}
		return sb.toString();
	}

	/**
	 * @param other
	 * @param sb
	 *            the comma separated list of version ranges from the other
	 *            indicator that are not contained in this indicator will be
	 *            added to this buffer (unless the buffer is null)
	 * @return
	 */
	public boolean contains(ProfileVersionIndicator other, StringBuffer sb) {

		// TBD: this double for-loop could be improved with a clever tree
		// implementation that automatically sorts the version ranges and
		// provides log(n) access to the respective range that contains a given
		// version number (e.g. begin/end from other) or directly checks if it
		// contains the other version range.

		boolean doesContain = true;
		ArrayList<String> messages = new ArrayList<String>();

		for (VersionRange vrOther : other.getVersionInfos()) {

			boolean vrOtherIsContainedByThis = false;

			for (VersionRange vrThis : this.versionInfos) {
				if (vrThis.containsNonStrict(vrOther)) {
					vrOtherIsContainedByThis = true;
					break;
				}
			}
			if (!vrOtherIsContainedByThis) {
				messages.add("(" + vrOther.toString() + ")");
				doesContain = false;
			}
		}

		if (doesContain) {
			return true;
		} else {
			if (sb != null) {
				String s = StringUtils.join(messages, ",");
				sb.append(s);
			}
			return false;
		}
	}

}
