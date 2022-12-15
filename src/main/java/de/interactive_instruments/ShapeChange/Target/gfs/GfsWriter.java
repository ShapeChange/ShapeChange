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
 * (c) 2002-2022 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target.gfs;

import java.io.File;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.platform.commons.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.interactive_instruments.ShapeChange.ShapeChangeException;
import de.interactive_instruments.ShapeChange.Util.XMLUtil;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class GfsWriter {

    public void write(File gfsTemplateFile, List<GmlFeatureClass> featureClasses, String srsName)
	    throws ShapeChangeException {

	try {
	    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	    DocumentBuilder db = dbf.newDocumentBuilder();
	    Document document = db.newDocument();

	    Element root = document.createElement("GMLFeatureClassList");
	    document.appendChild(root);

	    for (GmlFeatureClass gfc : featureClasses) {

		Element fcElmt = document.createElement("GMLFeatureClass");
		root.appendChild(fcElmt);

		addDataElement(document, fcElmt, "Name", gfc.getName());
		addDataElement(document, fcElmt, "ElementPath", gfc.getElementPath());

		for (GeometryPropertyDefinition geomProp : gfc.getGeometryPropertyDefinitions()) {

		    Element gpdElmt = document.createElement("GeomPropertyDefn");
		    fcElmt.appendChild(gpdElmt);

		    addDataElement(document, gpdElmt, "Name", geomProp.getName());
		    addDataElement(document, gpdElmt, "ElementPath", geomProp.getElementPath());
		    addDataElement(document, gpdElmt, "Type", geomProp.getGeometryType());

		    if (!geomProp.isNullable()) {
			addDataElement(document, gpdElmt, "Nullable", "false");
		    }
		}

		if (gfc.getGeometryPropertyDefinitions().isEmpty()) {
		    addDataElement(document, fcElmt, "GeometryType", "None");
		}

		if (StringUtils.isNotBlank(srsName)) {
		    addDataElement(document, fcElmt, "SRSName", srsName);
		}

		for (PropertyDefinition prop : gfc.getPropertyDefinitions()) {

		    Element pdElmt = document.createElement("PropertyDefn");
		    fcElmt.appendChild(pdElmt);

		    addDataElement(document, pdElmt, "Name", prop.getName());
		    addDataElement(document, pdElmt, "ElementPath", prop.getElementPath());

		    String type = prop.isListValuedProperty ? prop.getType().getName() + "List"
			    : prop.getType().getName();
		    addDataElement(document, pdElmt, "Type", type);
		    if (!prop.isNullable()) {
			addDataElement(document, pdElmt, "Nullable", "false");
		    }
		    if (prop.getSubtype() != null) {
			addDataElement(document, pdElmt, "Subtype", prop.getSubtype().getName());
		    }
		    if (prop.getWidth() != 0) {
			addDataElement(document, pdElmt, "Width", "" + prop.getWidth());
		    }
		    if (prop.getPrecision() != 0) {
			addDataElement(document, pdElmt, "Precision", "" + prop.getPrecision());
		    }
		}
	    }

	    XMLUtil.writeXml(document, gfsTemplateFile);

	} catch (ParserConfigurationException e) {
	    throw new ShapeChangeException(
		    "An exception occurred while writing the gfs template file: " + e.getMessage(), e);
	}

    }

    private void addDataElement(Document document, Element parentElement, String elementName, String textContent) {
	Element e = document.createElement(elementName);
	parentElement.appendChild(e);
	e.setTextContent(textContent);
    }

    public void writeGmlRegistry(File gmlRegistryFile, File gfsTemplateFile, String nsPrefix, String nsUri,
	    List<GmlFeatureClass> featureClasses) throws ShapeChangeException {

	try {

	    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	    DocumentBuilder db = dbf.newDocumentBuilder();
	    Document document = db.newDocument();

	    Element root = document.createElement("gml_registry");
	    document.appendChild(root);

	    Element namespace = document.createElement("namespace");
	    root.appendChild(namespace);
	    namespace.setAttribute("prefix", nsPrefix);
	    namespace.setAttribute("uri", nsUri);

	    for (GmlFeatureClass gfc : featureClasses) {

		Element featureType = document.createElement("featureType");
		namespace.appendChild(featureType);
		featureType.setAttribute("elementName", gfc.getName());
		featureType.setAttribute("gfsSchemaLocation", gfsTemplateFile.getName());
	    }

	    XMLUtil.writeXml(document, gmlRegistryFile);

	} catch (ParserConfigurationException e) {
	    throw new ShapeChangeException(
		    "An exception occurred while writing the gml registry file: " + e.getMessage(), e);
	}
    }

}
