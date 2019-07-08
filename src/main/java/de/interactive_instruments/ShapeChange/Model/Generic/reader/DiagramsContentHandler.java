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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.ImageMetadata;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments <dot>
 *         de)
 *
 */
public class DiagramsContentHandler extends AbstractContentHandler {

	private static final Set<String> IMAGE_METADATA_FIELDS = new HashSet<String>(
			Arrays.asList(new String[] { "id", "name", "relPathToFile", "width",
					"height" }));

	private static final Set<String> DEPRECATED_FIELDS = new HashSet<String>(
			Arrays.asList(new String[] { "file" }));

	private AbstractGenericInfoContentHandler parent;

	private List<ImageMetadata> diagrams = new ArrayList<ImageMetadata>();

	private String id = null;
	private String name = null;
	private String relPathToFile = null;
	private String width = null;
	private String height = null;

	public DiagramsContentHandler(ShapeChangeResult result, Options options,
			XMLReader reader, AbstractGenericInfoContentHandler parent) {
		super(result, options, reader, parent);
		this.parent = parent;
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {

		if (DEPRECATED_FIELDS.contains(localName)) {

			// ignore

		} else if (localName.equals("ImageMetadata")) {

			// reset fields
			this.id = null;
			this.name = null;
			this.relPathToFile = null;
			this.width = null;
			this.height = null;

		} else if (IMAGE_METADATA_FIELDS.contains(localName)) {

			sb = new StringBuffer();

		} else {

			// do not throw an exception, just log a message - the schema could
			// have been extended
			result.addDebug(null, 30800, "DiagramsContentHandler", localName);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

		if (localName.equals("id")) {

			this.id = sb.toString();

		} else if (localName.equals("name")) {

			this.name = sb.toString();

		} else if (DEPRECATED_FIELDS.contains(localName)) {

			// ignore

		} else if (localName.equals("relPathToFile")) {

			this.relPathToFile = sb.toString();

		} else if (localName.equals("width")) {

			this.width = sb.toString();

		} else if (localName.equals("height")) {

			this.height = sb.toString();

		} else if (localName.equals("ImageMetadata")) {

			try {

				File f = new File(options.imageTmpDir(), relPathToFile);
				int w = Integer.parseInt(width);
				int h = Integer.parseInt(height);

				ImageMetadata im = new ImageMetadata(id, name, f, relPathToFile,
						w, h);
				this.diagrams.add(im);

			} catch (NumberFormatException e) {
				result.addError(null, 30802, id, name, e.getMessage());
			}

		} else if (localName.equals("diagrams")) {

			if ("true".equalsIgnoreCase(options.parameter("loadDiagrams"))) {

				boolean sortDiagramsByName = options.parameterAsBoolean(null,
						"sortDiagramsByName", true);
				if (sortDiagramsByName) {
					Collections.sort(diagrams, new Comparator<ImageMetadata>() {
						@Override
						public int compare(ImageMetadata o1, ImageMetadata o2) {
							return o1.getName().compareTo(o2.getName());
						}
					});
				}

				parent.setDiagrams(diagrams);
			}

			// let parent know that we reached the end of the diagrams entry
			// (so that for example depth can properly be tracked)
			parent.endElement(uri, localName, qName);

			// Switch handler back to parent
			reader.setContentHandler(parent);

		} else {
			// do not throw an exception, just log a message - the schema could
			// have been extended
			result.addDebug(null, 30801, "DiagramsContentHandler", localName);
		}
	}

}
