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
 * (c) 2002-2026 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.interactive_instruments.shapechange.core.util.XMLUtil;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class PropertyRestrictions implements MessageSource {

    SortedMap<String, List<PropertyRestriction>> propertyRestrictionsByType = new TreeMap<>();

    public boolean isEmpty() {
	return this.propertyRestrictionsByType.isEmpty();
    }

    public void add(String typeName, PropertyRestriction pr) {

	List<PropertyRestriction> prs = null;
	if (this.propertyRestrictionsByType.containsKey(typeName)) {
	    prs = this.propertyRestrictionsByType.get(typeName);
	} else {
	    prs = new ArrayList<>();
	    this.propertyRestrictionsByType.put(typeName, prs);
	}

	prs.add(pr);
    }

    public void merge(PropertyRestrictions otherPropertyRestrictions) {
	this.propertyRestrictionsByType.putAll(otherPropertyRestrictions.getPropertyRestrictionsByType());
    }

    public SortedMap<String, List<PropertyRestriction>> getPropertyRestrictionsByType() {
	return this.propertyRestrictionsByType;
    }

    /**
     * @param typeName
     * @return the list of property restrictions defined for the type; can be empty
     *         but not <code>null</code>
     */
    public List<PropertyRestriction> getPropertyRestrictions(String typeName) {
	if (this.propertyRestrictionsByType.containsKey(typeName)) {
	    return this.propertyRestrictionsByType.get(typeName);
	} else {
	    return new ArrayList<>();
	}
    }

    public Optional<PropertyRestriction> getPropertyRestriction(String typeName, String propertyName) {

	List<PropertyRestriction> prs = getPropertyRestrictions(typeName);

	return prs.stream().filter(pr -> pr.getPropertyName().equals(propertyName)).findFirst();
    }

    public void toXml(File outputFile, ShapeChangeResult result) {

	if (this.propertyRestrictionsByType.isEmpty()) {
	    result.addWarning(this, 1, outputFile.getAbsolutePath());
	    return;
	}

	try {

	    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	    dbf.setNamespaceAware(true);
	    dbf.setValidating(true);
	    DocumentBuilder db = dbf.newDocumentBuilder();

	    Document document = db.newDocument();

	    Element root = document.createElementNS(ShapeChangeConstants.SC_CONFIG_NS, "PropertyRestrictions");
	    document.appendChild(root);

	    XMLUtil.addAttribute(document, root, "xmlns", ShapeChangeConstants.SC_CONFIG_NS);

	    Element e1 = document.createElementNS(ShapeChangeConstants.SC_CONFIG_NS, "types");
	    root.appendChild(e1);

	    for (Entry<String, List<PropertyRestriction>> mapEntry : this.propertyRestrictionsByType.entrySet()) {

		Element e2 = document.createElementNS(ShapeChangeConstants.SC_CONFIG_NS, "Type");
		e1.appendChild(e2);

		XMLUtil.addAttribute(document, e2, "name", StringUtils.stripToEmpty(mapEntry.getKey()));

		for (PropertyRestriction pr : mapEntry.getValue()) {

		    Element e3 = document.createElementNS(ShapeChangeConstants.SC_CONFIG_NS, "property");
		    e2.appendChild(e3);

		    XMLUtil.addAttribute(document, e3, "name", StringUtils.stripToEmpty(pr.getPropertyName()));

		    if (pr.hasValueTypeRestrictions()) {
			XMLUtil.addAttribute(document, e3, "valueTypeRestrictions",
				StringUtils.join(pr.getValueTypeRestrictions(), ", "));
		    }
		}
	    }

	    XMLUtil.writeXml(document, outputFile);

	} catch (ShapeChangeException | ParserConfigurationException e) {

	    result.addError(this, 2, outputFile.getAbsolutePath(), e.getMessage());
	}
    }

    public static PropertyRestrictions fromXml(Element prsElmt) {

	PropertyRestrictions prs = new PropertyRestrictions();

	for (Element typeElmt : XMLUtil.getChildElements(prsElmt, "Type")) {

	    String typeName = typeElmt.getAttribute("name");

	    for (Element propertyElmt : XMLUtil.getChildElements(typeElmt, "property")) {

		String propertyName = propertyElmt.getAttribute("name");

		List<String> valueTypeRestrictions = new ArrayList<>();

		if (propertyElmt.hasAttribute("valueTypeRestrictions")) {
		    String valueTypeRestriction = propertyElmt.getAttribute("valueTypeRestrictions");
		    String[] valueTypes = StringUtils.split(valueTypeRestriction, ", ");
		    valueTypeRestrictions = Arrays.asList(valueTypes);
		}

		// more restriction facets can be added in the future

		if (!valueTypeRestrictions.isEmpty()) {
		    PropertyRestriction pr = new PropertyRestriction();
		    pr.setPropertyName(propertyName);
		    pr.setValueTypeRestrictions(valueTypeRestrictions);
		    prs.add(typeName, pr);
		}
	    }
	}

	return prs;
    }

    @Override
    public String message(int mnr) {

	return switch (mnr) {

	case 1 -> "No property restrictions to write to file '$1$'.";
	case 2 ->
	    "Encountered an exception while writing property restrictions to file '$1$'. Exception message is: $2$";

	default -> "(PropertyRestrictions.java) Unknown message with number: " + mnr;
	};

    }

}
