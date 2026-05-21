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

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Configuration information for a tagged value.
 * 
 * @author echterhoff
 * 
 */
public class TaggedValueConfigurationEntry {

    private String name = null;
    private String value = null;
    private ModelElementSelectionInfo modelElementSelectionInfo = null;

    public TaggedValueConfigurationEntry(String name, String value,
	    ModelElementSelectionInfo modelElementSelectionInfo) {
	super();
	this.name = name;
	this.value = value;
	this.modelElementSelectionInfo = modelElementSelectionInfo;
    }

    /**
     * @return the name of the tagged value
     */
    public String getName() {
	return name;
    }

    /**
     * @return the value of the tagged value, <code>null</code> if not set in the
     *         configuration
     */
    public String getValue() {
	return value;
    }

    /**
     * @return <code>true</code> if the value attribute of the tagged value was set
     *         in the configuration (even to the empty string), else
     *         <code>false</code>
     */
    public boolean hasValue() {
	return value != null;
    }

    /**
     * @return the modelElementSelectionInfo; cannot be <code>null</code>
     */
    public ModelElementSelectionInfo getModelElementSelectionInfo() {
	return modelElementSelectionInfo;
    }

    /**
     * @param modelElementSelectionInfo the modelElementSelectionInfo to set; if
     *                                  <code>null</code>, the default
     *                                  ModelElementSelectionInfo will be set, i.e.
     *                                  any model element will be selected
     */
    public void setModelElementSelectionInfo(ModelElementSelectionInfo modelElementSelectionInfo) {

	if (modelElementSelectionInfo == null) {
	    this.modelElementSelectionInfo = new ModelElementSelectionInfo();
	} else {
	    this.modelElementSelectionInfo = modelElementSelectionInfo;
	}
    }

    public static TaggedValueConfigurationEntry parse(Element taggedValueE) {

	String name = taggedValueE.getAttribute("name");
	String value = null;

	if (taggedValueE.hasAttribute("value")) {
	    value = taggedValueE.getAttribute("value");
	}

	ModelElementSelectionInfo meselect = ModelElementSelectionInfo.parse(taggedValueE);

	return new TaggedValueConfigurationEntry(name, value, meselect);
    }

    /**
     * @param trfE element that contains the 'taggedValues' element
     * @return list of configured tagged values; can be empty but not
     *         <code>null</code>
     */
    public static List<TaggedValueConfigurationEntry> parseTaggedValues(Element trfE) {

	List<TaggedValueConfigurationEntry> result = new ArrayList<TaggedValueConfigurationEntry>();

	List<Element> directTaggedValuesInTrfList = new ArrayList<>();

	/*
	 * identify taggedValues elements that are direct children of the Transformer
	 * element
	 */
	NodeList children = trfE.getChildNodes();
	if (children != null && children.getLength() != 0) {

	    for (int k = 0; k < children.getLength(); k++) {

		Node n = children.item(k);
		if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals("taggedValues")) {
		    directTaggedValuesInTrfList.add((Element) n);
		}
	    }
	}

	if (!directTaggedValuesInTrfList.isEmpty()) {

	    for (Element directTaggedValuesInTrf : directTaggedValuesInTrfList) {

		NodeList taggedValuesNl = directTaggedValuesInTrf.getElementsByTagName("TaggedValue");
		Node taggedValueN;
		Element taggedValueE;

		if (taggedValuesNl != null && taggedValuesNl.getLength() > 0) {

		    for (int k = 0; k < taggedValuesNl.getLength(); k++) {

			taggedValueN = taggedValuesNl.item(k);
			if (taggedValueN.getNodeType() == Node.ELEMENT_NODE) {

			    taggedValueE = (Element) taggedValueN;

			    TaggedValueConfigurationEntry tvce = TaggedValueConfigurationEntry.parse(taggedValueE);
			    result.add(tvce);
			}
		    }
		}
	    }
	}

	return result;
    }
}
