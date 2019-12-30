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
package de.interactive_instruments.ShapeChange.Model.Generic.reader;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Profile.ProfileIdentifier;
import de.interactive_instruments.ShapeChange.Profile.ProfileVersionIndicator;
import de.interactive_instruments.ShapeChange.Profile.Profiles;
import de.interactive_instruments.ShapeChange.Profile.VersionNumber;
import de.interactive_instruments.ShapeChange.Profile.VersionRange;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
public class ProfilesContentHandler extends AbstractContentHandler {

	public ProfilesContentHandler(ShapeChangeResult result, Options options,
			XMLReader reader, AbstractContentHandler parent) {
		super(result, options, reader, parent);
	}

	private List<ProfileIdentifier> profileIdentifiers = new ArrayList<ProfileIdentifier>();
	private String profileName = null;
	private List<VersionRange> versionRanges = null;
	private ProfileVersionIndicator versionIndicator = null;
	private SortedMap<String, String> parameters = null;

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {

		if (localName.equals("profiles")) {

			// ignore

		} else if (localName.equals("Profile")) {

			this.profileName = atts.getValue("name");
			this.versionIndicator = null;
			this.versionRanges = null;
			this.parameters = null;

		} else if (localName.equals("versionIdentifier")) {

			this.versionRanges = new ArrayList<VersionRange>();

		} else if (localName.equals("VersionRange")) {

			String begin = atts.getValue("begin");
			String end = atts.getValue("end");

			VersionNumber beginVN = new VersionNumber(begin);
			VersionNumber endVN = new VersionNumber(end);

			VersionRange vr = new VersionRange(beginVN, endVN);

			this.versionRanges.add(vr);

		} else if (localName.equals("parameter")) {

			this.parameters = new TreeMap<String, String>();

		} else if (localName.equals("ProfileParameter")) {

			String name = atts.getValue("name");
			String value = atts.getValue("value");

			this.parameters.put(name, value);

		} else {

			// do not throw an exception, just log a message - the schema could
			// have been extended
			result.addDebug(null, 30800, "ProfilesContentHandler", localName);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

		if (localName.equals("Profile")) {

			ProfileIdentifier pi = new ProfileIdentifier(this.profileName,
					this.versionIndicator, this.parameters);

			this.profileIdentifiers.add(pi);

		} else if (localName.equals("versionIdentifier")) {

			this.versionIndicator = new ProfileVersionIndicator(
					this.versionRanges);

		} else if (localName.equals("VersionRange")) {

			// ignore

		} else if (localName.equals("parameter")) {

			// ignore

		} else if (localName.equals("ProfileParameter")) {

			// ignore

		} else if (localName.equals("profiles")) {

			/*
			 * the parent has a reference to this content handler, so can invoke
			 * getProfiles()
			 */

			/*
			 * let parent know that we reached the end of the profiles element
			 * (so that for example depth can properly be tracked)
			 */
			parent.endElement(uri, localName, qName);

			// Switch handler back to parent
			reader.setContentHandler(parent);

		} else {
			// do not throw an exception, just log a message - the schema could
			// have been extended
			result.addDebug(null, 30801, "ProfilesContentHandler", localName);
		}
	}

	public Profiles getProfiles() {

		return new Profiles(this.profileIdentifiers);
	}

}
