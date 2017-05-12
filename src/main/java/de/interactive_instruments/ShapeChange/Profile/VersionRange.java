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

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 */
public class VersionRange {

	VersionNumber begin = null;
	VersionNumber end = null;

	public VersionRange(VersionNumber begin, VersionNumber end) {
		this.begin = begin;
		this.end = end;
	}

	/**
	 * Uses the same expression as ISO 19108 (see TM_RelativePosition for time
	 * periods): Before - self.end.position < other.begin.position
	 * 
	 * @param other
	 * @return true if this version range is before the other, else false
	 */
	public boolean before(VersionRange other) {
		return this.end.compareTo(other.begin) < 0;
	}

	/**
	 * Uses the same expression as ISO 19108 (see TM_RelativePosition for time
	 * periods): Meets - self.end.position = other.begin.position
	 * 
	 * @param other
	 * @return true if this version range meets the other, else false
	 */
	public boolean meets(VersionRange other) {
		return this.end.compareTo(other.begin) == 0;
	}

	/**
	 * Uses the same expression as ISO 19108 (see TM_RelativePosition for time
	 * periods): Overlaps - self.begin.position < other.begin.position AND
	 * self.end.position > other.begin.position AND self.end.position <
	 * other.end.position
	 * 
	 * @param other
	 * @return true if this version range overlaps the other, else false
	 */
	public boolean overlaps(VersionRange other) {
		return this.begin.compareTo(other.begin) < 0
				&& this.end.compareTo(other.begin) > 0
				&& this.end.compareTo(other.end) < 0;
	}

	/**
	 * Uses the same expression as ISO 19108 (see TM_RelativePosition for time
	 * periods): Begins - self.begin.position = other.begin.position AND
	 * self.end.position < other.end.position
	 * 
	 * @param other
	 * @return true if this version range begins the other, else false
	 */
	public boolean begins(VersionRange other) {
		return this.begin.compareTo(other.begin) == 0
				&& this.end.compareTo(other.end) < 0;
	}

	/**
	 * Uses the same expression as ISO 19108 (see TM_RelativePosition for time
	 * periods): BegunBy - self.begin.position = other.begin.position AND
	 * self.end.position > other.end.position
	 * 
	 * @param other
	 * @return true if this version range is begun by the other, else false
	 */
	public boolean begunBy(VersionRange other) {
		return this.begin.compareTo(other.begin) == 0
				&& this.end.compareTo(other.end) > 0;
	}

	/**
	 * Uses the same expression as ISO 19108 (see TM_RelativePosition for time
	 * periods): During - self.begin.position > other.begin.position AND
	 * self.end.position < other.end.position
	 * 
	 * @param other
	 * @return true if this version range is during the other, else false
	 */
	public boolean during(VersionRange other) {
		return this.begin.compareTo(other.begin) > 0
				&& this.end.compareTo(other.end) < 0;
	}

	/**
	 * Uses the same expression as ISO 19108 (see TM_RelativePosition for time
	 * periods): Contains - self.begin.position < other.begin.position AND
	 * self.end.position > other.end.position
	 * 
	 * @param other
	 * @return true if this version range contains the other, else false
	 */
	public boolean contains(VersionRange other) {
		return this.begin.compareTo(other.begin) < 0
				&& this.end.compareTo(other.end) > 0;
	}

	/**
	 * OR-combination of begunBy, contains, equals, endedBy: self.begin.position
	 * <= other.begin.position AND self.end.position >= other.end.position
	 * 
	 * @param other
	 * @return true if this version range contains the other (in a non-strict
	 *         way, see method description), else false
	 */
	public boolean containsNonStrict(VersionRange other) {
		return this.begin.compareTo(other.begin) <= 0
				&& this.end.compareTo(other.end) >= 0;
	}

	/**
	 * Uses the same expression as ISO 19108 (see TM_RelativePosition for time
	 * periods): Equals - self.begin.position = other.begin.position AND
	 * self.end.position = other.end.position
	 * 
	 * @param other
	 * @return true if this version range equals the other, else false
	 */
	public boolean equals(VersionRange other) {
		return this.begin.compareTo(other.begin) == 0
				&& this.end.compareTo(other.end) == 0;
	}

	/**
	 * Uses the same expression as ISO 19108 (see TM_RelativePosition for time
	 * periods): OverlappedBy - self.begin.position > other.begin.position AND
	 * self.begin.position < other.end.position AND self.end.position >
	 * other.end.position
	 * 
	 * @param other
	 * @return true if this version range is overlapped by the other, else false
	 */
	public boolean overlappedBy(VersionRange other) {
		return this.begin.compareTo(other.begin) > 0
				&& this.begin.compareTo(other.end) < 0
				&& this.end.compareTo(other.end) > 0;
	}

	/**
	 * Uses the same expression as ISO 19108 (see TM_RelativePosition for time
	 * periods): Ends - self.begin.position > other.begin.position AND
	 * self.end.position = other.end.position
	 * 
	 * @param other
	 * @return true if this version range ends the other, else false
	 */
	public boolean ends(VersionRange other) {
		return this.begin.compareTo(other.begin) > 0
				&& this.end.compareTo(other.end) == 0;
	}

	/**
	 * Uses the same expression as ISO 19108 (see TM_RelativePosition for time
	 * periods): EndedBy - self.begin.position < other.begin.position AND
	 * self.end.position = other.end.position
	 * 
	 * @param other
	 * @return true if this version range is ended by the other, else false
	 */
	public boolean endedBy(VersionRange other) {
		return this.begin.compareTo(other.begin) < 0
				&& this.end.compareTo(other.end) == 0;
	}

	/**
	 * Uses the same expression as ISO 19108 (see TM_RelativePosition for time
	 * periods): MetBy - self.begin.position = other.end.position
	 * 
	 * @param other
	 * @return true if this version range is met by the other, else false
	 */
	public boolean metBy(VersionRange other) {
		return this.begin.compareTo(other.end) == 0;
	}

	/**
	 * Uses the same expression as ISO 19108 (see TM_RelativePosition for time
	 * periods): After - self.begin.position > other.end.position
	 * 
	 * @param other
	 * @return true if this version range is after the other, else false
	 */
	public boolean after(VersionRange other) {
		return this.begin.compareTo(other.end) > 0;
	}

	/**
	 * Creates a new VersionRange that is the union of both this range and the
	 * other, iff this range is not before and not after the other.
	 * 
	 * @param other
	 * @return the union of this and the other range or null if one is before
	 *         the other
	 */
	public VersionRange union(VersionRange other) {
		if (this.before(other) || this.after(other)) {
			return null;
		} else {
			VersionNumber begin_tmp, end_tmp;
			if (this.begin.compareTo(other.begin) < 0) {
				begin_tmp = this.begin;
			} else if (this.begin.compareTo(other.begin) > 0) {
				begin_tmp = other.begin;
			} else {
				// both begins are equal; however, in case the version numbers
				// have different lengths, we want the shorter one (because it
				// is more general)
				if (this.begin.components.length <= other.begin.components.length) {
					begin_tmp = this.begin;
				} else {
					begin_tmp = other.begin;
				}
			}
			if (this.end.compareTo(other.end) > 0) {
				end_tmp = this.end;
			} else if (this.end.compareTo(other.end) < 0) {
				end_tmp = other.end;
			} else {
				// both ends are equal; however, in case the version numbers
				// have different lengths, we want the shorter one (because it
				// is more general)
				if (this.end.components.length <= other.end.components.length) {
					end_tmp = this.end;
				} else {
					end_tmp = other.end;
				}
			}
			return new VersionRange(begin_tmp, end_tmp);
		}
	}

	public String toString() {
		return begin + "-" + end;
	}

	public VersionRange createCopy() {

		VersionNumber beginCopy = this.begin.createCopy();
		VersionNumber endCopy = this.end.createCopy();

		return new VersionRange(beginCopy, endCopy);
	}

	/**
	 * @return the begin
	 */
	public VersionNumber getBegin() {
		return begin;
	}

	/**
	 * @return the end
	 */
	public VersionNumber getEnd() {
		return end;
	}

}
