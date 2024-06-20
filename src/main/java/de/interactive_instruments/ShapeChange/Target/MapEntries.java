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
 * (c) 2002-2020 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.ProcessMapEntry;
import de.interactive_instruments.ShapeChange.ShapeChangeException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Util.XMLUtil;

/**
 * Supports writing a map entries file by targets.
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class MapEntries implements MessageSource {

    public static final String SC_CONFIG_NS = "http://www.interactive-instruments.de/ShapeChange/Configuration/1.1";

    protected List<ProcessMapEntry> mapEntries = new ArrayList<>();

    public void add(ProcessMapEntry me) {
	mapEntries.add(me);
    }

    public void add(Collection<ProcessMapEntry> mes) {
	mapEntries.addAll(mes);
    }

    public void toXml(File outputFile, ShapeChangeResult result) {

	if (mapEntries.isEmpty()) {
	    result.addWarning(this, 1, outputFile.getAbsolutePath());
	    return;
	}

	try {

	    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	    dbf.setNamespaceAware(true);
	    dbf.setValidating(true);
	    DocumentBuilder db = dbf.newDocumentBuilder();

	    Document document = db.newDocument();

	    Element root = document.createElementNS(SC_CONFIG_NS, "mapEntries");
	    document.appendChild(root);

	    XMLUtil.addAttribute(document, root, "xmlns", SC_CONFIG_NS);

	    // sort list of map entries
	    List<ProcessMapEntry> sortedMapEntries = mapEntries.stream()
		    .sorted((me1, me2) -> me1.getType().compareTo(me2.getType())).collect(Collectors.toList());

	    for (ProcessMapEntry pme : sortedMapEntries) {

		Element e1 = document.createElementNS(SC_CONFIG_NS, "MapEntry");
		root.appendChild(e1);

		XMLUtil.addAttribute(document, e1, "type", pme.getType());
		XMLUtil.addAttribute(document, e1, "rule", pme.getRule());
		if (pme.hasTargetType()) {
		    XMLUtil.addAttribute(document, e1, "targetType", pme.getTargetType());
		}
		if (pme.hasParam()) {
		    XMLUtil.addAttribute(document, e1, "param", pme.getParam());
		}
	    }
	    
	    XMLUtil.writeXml(document, outputFile);

	} catch (ShapeChangeException | ParserConfigurationException e) {

	    result.addError(this, 2, outputFile.getAbsolutePath(), e.getMessage());
	}
    }

    @Override
    public String message(int mnr) {

	switch (mnr) {

	case 1:
	    return "No map entries to write to file '$1$'.";
	case 2:
	    return "Encountered an exception while writing map entries to file '$1$'. Exception message is: $2$";

	default:
	    return "(MapEntries.java) Unknown message with number: " + mnr;
	}

    }
}
