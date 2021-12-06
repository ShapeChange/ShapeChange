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
 * (c) 2002-2021 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target.FeatureCatalogue;

import java.io.File;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import de.interactive_instruments.ShapeChange.Model.ImageMetadata;

/**
 * Handler for reading image metadata from the feature catalogue temporary XML
 * file.
 *
 * @author Johannes Echterhoff
 */
public class ImageMetadataContentHandler implements ContentHandler {

    private SortedMap<String, ImageMetadata> images = new TreeMap<>();

    public ImageMetadataContentHandler() {
	super();
    }

    @Override
    public void setDocumentLocator(Locator locator) {
	// irrelevant
    }

    @Override
    public void startDocument() throws SAXException {
	// irrelevant
    }

    @Override
    public void endDocument() throws SAXException {
	// irrelevant
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
	// irrelevant
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
	// irrelevant
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {

	if (localName.equalsIgnoreCase("image")) {

	    String id = atts.getValue("id");

	    if (!images.containsKey(id)) {
		String name = atts.getValue("name");
		/*
		 * 2021-12-06 JE: file not needed by FC, afaics.
		 */
		File file = null;
		String relPathToFile = atts.getValue("relPath");
		int width = Integer.parseInt(atts.getValue("width"));
		int height = Integer.parseInt(atts.getValue("height"));

		ImageMetadata img = new ImageMetadata(id, name, file, relPathToFile, width, height);
		this.images.put(id, img);
	    }
	}
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
	// ignore for this simple SAX content handler
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
	// irrelevant
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
	// irrelevant
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
	// irrelevant
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
	// irrelevant
    }
    
    public List<ImageMetadata> getImages() {
	return this.images.values().stream().collect(Collectors.toList());
    }
}
