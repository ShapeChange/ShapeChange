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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments <dot>
 *         de)
 *
 */
public class GenericModelContentHandler extends AbstractContentHandler {

	private static final Set<String> ELEMENTS_TO_IGNORE = new HashSet<String>(
			Arrays.asList(new String[] { "globalProfileInfos", "Profile",
					"name", "description" }));

	private List<GenericPackageContentHandler> packageContentHandlers = new ArrayList<GenericPackageContentHandler>();
	private List<GenericAssociationContentHandler> associationContentHandlers = new ArrayList<GenericAssociationContentHandler>();
	private String encoding = null;

	public GenericModelContentHandler(ShapeChangeResult result, Options options,
			XMLReader reader) {
		super(result, options, reader, null);
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {

		if (localName.length() == 0)
			throw new GenericModelReaderConfigurationException(
					"localName is empty. No Namespace support in the SAXParser?");

		if (ELEMENTS_TO_IGNORE.contains(localName)) {

			// ignore

		} else if (localName.equals("Model")) {

			if (atts != null) {

				String encoding_ = atts.getValue("encoding");
				if (encoding_ != null) {
					this.encoding = encoding_;
				}

				String scxmlProducer = atts.getValue("scxmlProducer");
				if (StringUtils.isBlank(scxmlProducer)) {
					scxmlProducer = "<not set>";
				}
				String scxmlProducerVersion = atts
						.getValue("scxmlProducerVersion");
				if (StringUtils.isBlank(scxmlProducerVersion)) {
					scxmlProducerVersion = "<not set>";
				}
				result.addDebug(null, 30804, scxmlProducer,
						scxmlProducerVersion);

			}

		} else if (localName.equals("packages")) {

			// ignore

		} else if (localName.equals("Package")) {

			GenericPackageContentHandler handler = new GenericPackageContentHandler(
					result, options, reader, this);
			this.packageContentHandlers.add(handler);
			reader.setContentHandler(handler);

		} else if (localName.equals("associations")) {

			// ignore

		} else if (localName.equals("Association")) {

			GenericAssociationContentHandler handler = new GenericAssociationContentHandler(
					result, options, reader, this);
			this.associationContentHandlers.add(handler);
			reader.setContentHandler(handler);

		} else {

			// do not throw an exception, just log a message - the schema could
			// have been extended
			result.addDebug(null, 30800, "GenericModelContentHandler",
					localName);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		// ignore
	}

	/**
	 * @return the packageContentHandlers
	 */
	public List<GenericPackageContentHandler> getPackageContentHandlers() {
		return packageContentHandlers;
	}

	/**
	 * @return the associationContentHandlers
	 */
	public List<GenericAssociationContentHandler> getAssociationContentHandlers() {
		return associationContentHandlers;
	}

	public Map<String, GenericPackageContentHandler> getAllPackageContentHandlers() {

		Map<String, GenericPackageContentHandler> result = new HashMap<String, GenericPackageContentHandler>();

		for (GenericPackageContentHandler gpch : this.packageContentHandlers) {
			gpch.visitPackageContentHandlers(result);
		}

		return result;
	}

	/**
	 * @return the value read from Model/@encoding, or <code>null</code> if the
	 *         attribute was not present
	 */
	public String getEncoding() {
		return this.encoding;
	}
}
