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

			// do not throw an exception, just log a warning - the schema could
			// have been extended
			result.addWarning(null, 30800, "ProfilesContentHandler", localName);
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

			this.versionIndicator = new ProfileVersionIndicator(this.versionRanges);

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
			// do not throw an exception, just log a warning - the schema could
			// have been extended
			result.addWarning(null, 30801, "ProfilesContentHandler", localName);
		}
	}
	
	public Profiles getProfiles() {
		
		return new Profiles(this.profileIdentifiers);
	}

}
