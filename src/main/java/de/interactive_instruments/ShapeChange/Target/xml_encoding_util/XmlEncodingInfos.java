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
package de.interactive_instruments.ShapeChange.Target.xml_encoding_util;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.ShapeChangeException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.XmlNamespace;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Util.XMLUtil;

/**
 * Supports writing a file with XML encoding infos by targets, and reading such
 * infos from an XML element (contained in the advanced process configuration).
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class XmlEncodingInfos implements MessageSource {

    public static final String SC_CONFIG_NS = "http://www.interactive-instruments.de/ShapeChange/Configuration/1.1";

    protected SortedSet<ModelElementXmlEncoding> modelElementEncodings = new TreeSet<>();
    protected SortedSet<XmlNamespace> xmlNamespaces = new TreeSet<>(XmlNamespace.comparator);

    public SortedSet<ModelElementXmlEncoding> getModelElementEncodings() {
	return modelElementEncodings;
    }

    public SortedSet<XmlNamespace> getXmlNamespaces() {
	return xmlNamespaces;
    }

    public void add(ModelElementXmlEncoding mexe) {
	modelElementEncodings.add(mexe);
    }

    public void add(XmlNamespace xn) {
	xmlNamespaces.add(xn);
    }

    public void addModelElementXmlEncodings(Collection<ModelElementXmlEncoding> mexes) {
	modelElementEncodings.addAll(mexes);
    }

    public void addXmlNamespaces(Collection<XmlNamespace> xns) {
	xmlNamespaces.addAll(xns);
    }

    public Optional<ModelElementXmlEncoding> getXmlEncodingInfo(String schemaName, String modelElementName) {

	for (ModelElementXmlEncoding mexe : this.modelElementEncodings) {
	    if ((StringUtils.isBlank(schemaName) || mexe.getApplicationSchemaName().equals(schemaName))
		    && mexe.getModelElementName().equals(modelElementName)) {
		return Optional.of(mexe);
	    }
	}

	return Optional.empty();
    }

    public Optional<ModelElementXmlEncoding> getXmlEncodingInfo(String schemaName, ClassInfo ci) {
	return getXmlEncodingInfo(schemaName, ci.name());
    }

    public Optional<ModelElementXmlEncoding> getXmlEncodingInfo(String schemaName, PropertyInfo pi) {
	return getXmlEncodingInfo(schemaName, pi.inClass().name() + "::" + pi.name());
    }

    public Optional<String> findNsabr(String xmlNamespace) {

	return this.xmlNamespaces.stream().filter(xns -> xns.getNs().equals(xmlNamespace)).map(xns -> xns.getNsabr())
		.findFirst();
    }

    public boolean isXmlAttribute(String schemaName, String className, String propertyName) {

	final String propertyModelElementName = className + "::" + propertyName;

	return this.modelElementEncodings.stream()
		.filter(mexe -> (StringUtils.isBlank(schemaName) || mexe.getApplicationSchemaName().equals(schemaName))
			&& mexe.getModelElementName().equals(propertyModelElementName))
		.map(mexe -> mexe.isXmlAttribute()).findFirst().orElse(false);
    }

    public void add(ClassInfo ci, String xmlName, String xmlNamespace) {
	PackageInfo schemaPkg = ci.model().schemaPackage(ci);
	add(new ModelElementXmlEncoding(ci.name(), schemaPkg == null ? null : schemaPkg.name(), xmlName, xmlNamespace,
		false));
    }

    public void add(PropertyInfo pi, String xmlName, String xmlNamespace, boolean isXmlAttribute) {
	PackageInfo schemaPkg = pi.model().schemaPackage(pi.inClass());
	add(new ModelElementXmlEncoding(pi.inClass().name() + "::" + pi.name(),
		schemaPkg == null ? null : schemaPkg.name(), xmlName, xmlNamespace, isXmlAttribute));
    }

    public void toXml(File outputFile, ShapeChangeResult result) {

	if (modelElementEncodings.isEmpty() && xmlNamespaces.isEmpty()) {
	    result.addWarning(this, 1, outputFile.getAbsolutePath());
	    return;
	}

	try {

	    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	    dbf.setNamespaceAware(true);
	    dbf.setValidating(true);
	    DocumentBuilder db = dbf.newDocumentBuilder();

	    Document document = db.newDocument();

	    Element root = document.createElementNS(SC_CONFIG_NS, "XmlEncodingInfos");
	    document.appendChild(root);

	    XMLUtil.addAttribute(document, root, "xmlns", SC_CONFIG_NS);

	    // sort list of model element encodings
	    List<ModelElementXmlEncoding> sortedModelElementEncodings = modelElementEncodings.stream()
		    .sorted(ModelElementXmlEncoding.comparator).collect(Collectors.toList());

	    if (!sortedModelElementEncodings.isEmpty()) {

		Element e1 = document.createElementNS(SC_CONFIG_NS, "modelElementEncodings");
		root.appendChild(e1);

		for (ModelElementXmlEncoding mexe : sortedModelElementEncodings) {

		    Element e2 = document.createElementNS(SC_CONFIG_NS, "ModelElementXmlEncoding");
		    e1.appendChild(e2);

		    if (mexe.getApplicationSchemaName() != null) {
			XMLUtil.addAttribute(document, e2, "applicationSchemaName", mexe.getApplicationSchemaName());
		    }
		    XMLUtil.addAttribute(document, e2, "modelElementName", mexe.getModelElementName());
		    XMLUtil.addAttribute(document, e2, "xmlName", mexe.getXmlName());
		    XMLUtil.addAttribute(document, e2, "xmlNamespace", mexe.getXmlNamespace());
		    if (mexe.isXmlAttribute()) {
			XMLUtil.addAttribute(document, e2, "xmlAttribute", "true");
		    }
		}
	    }

	    if (!xmlNamespaces.isEmpty()) {

		Element e1 = document.createElementNS(SC_CONFIG_NS, "namespaces");
		root.appendChild(e1);

		for (XmlNamespace xn : xmlNamespaces) {

		    Element e2 = document.createElementNS(SC_CONFIG_NS, "XmlNamespace");
		    e1.appendChild(e2);

		    XMLUtil.addAttribute(document, e2, "nsabr", xn.getNsabr());
		    XMLUtil.addAttribute(document, e2, "ns", xn.getNs());
		    if (xn.hasLocation()) {
			XMLUtil.addAttribute(document, e2, "location", xn.getLocation());
		    }
		    if (xn.hasPackageName()) {
			XMLUtil.addAttribute(document, e2, "packageName", xn.getPackageName());
		    }
		}
	    }
	    
	    XMLUtil.writeXml(document, outputFile);

	} catch (ShapeChangeException | ParserConfigurationException e) {

	    result.addError(this, 2, outputFile.getAbsolutePath(), e.getMessage());
	}
    }

    public static XmlEncodingInfos fromXml(Element xeiElmt) {

	XmlEncodingInfos xei = new XmlEncodingInfos();

	for (Element mexe : XMLUtil.getChildElements(xeiElmt, "ModelElementXmlEncoding")) {

	    String modelElementName = mexe.getAttribute("modelElementName");
	    String applicationSchemaName = StringUtils.stripToNull(mexe.getAttribute("applicationSchemaName"));
	    String xmlName = mexe.getAttribute("xmlName");
	    String xmlNamespace = mexe.getAttribute("xmlNamespace");
	    Boolean isXmlAttribute = mexe.hasAttribute("xmlAttribute")
		    ? XMLUtil.parseBoolean(mexe.getAttribute("xmlAttribute"))
		    : false;

	    xei.add(new ModelElementXmlEncoding(modelElementName, applicationSchemaName, xmlName, xmlNamespace,
		    isXmlAttribute));
	}

	for (Element xn : XMLUtil.getChildElements(xeiElmt, "XmlNamespace")) {

	    String nsabr = xn.getAttribute("nsabr");
	    String ns = xn.getAttribute("ns");
	    String location = xn.hasAttribute("location") ? xn.getAttribute("location") : null;
	    String packageName = xn.hasAttribute("packageName") ? xn.getAttribute("packageName") : null;

	    xei.add(new XmlNamespace(nsabr, ns, location, packageName));
	}

	return xei;
    }

    public void addNamespace(String namespaceAbbreviation, String namespace, String location) {

	XmlNamespace nsObj = new XmlNamespace(namespaceAbbreviation, namespace, location, null);

	this.xmlNamespaces.add(nsObj);
    }

    public Optional<String> getXmlQName(String schemaName, String inClassName, String propertyName) {

	Optional<ModelElementXmlEncoding> mexeOpt = this.getXmlEncodingInfo(schemaName,
		inClassName + "::" + propertyName);

	if (mexeOpt.isPresent()) {
	    Optional<String> nsabr = this.findNsabr(mexeOpt.get().getXmlNamespace());
	    if (nsabr.isPresent()) {
		return Optional.of(nsabr.get() + ":" + mexeOpt.get().getXmlName());
	    }
	}

	return Optional.empty();
    }

    public Optional<String> getXmlNamespace(String schemaName, String modelElementName) {

	Optional<ModelElementXmlEncoding> mexeOpt = this.getXmlEncodingInfo(schemaName, modelElementName);

	if (mexeOpt.isPresent()) {
	    return Optional.of(mexeOpt.get().getXmlNamespace());
	} else {
	    // try to look the namespace up by XmlNamespace with packageName
	    return getXmlNamespace(schemaName);
	}
    }

    public Optional<String> getXmlName(String schemaName, String inClassName, String propertyName) {

	Optional<ModelElementXmlEncoding> mexeOpt = this.getXmlEncodingInfo(schemaName,
		inClassName + "::" + propertyName);

	if (mexeOpt.isPresent()) {
	    return Optional.of(mexeOpt.get().getXmlName());
	}

	return Optional.empty();
    }

    public Optional<String> getXmlNamespace(String schemaName) {

	if (StringUtils.isBlank(schemaName)) {
	    return Optional.empty();
	}

	for (XmlNamespace xns : this.xmlNamespaces) {
	    if (xns.hasPackageName() && xns.getPackageName().equals(schemaName)) {
		return Optional.of(xns.getNs());
	    }
	}

	return Optional.empty();
    }

    public void merge(XmlEncodingInfos otherXmlEncodingInfos) {
	this.modelElementEncodings.addAll(otherXmlEncodingInfos.getModelElementEncodings());

	Set<String> thisNamespaces = this.xmlNamespaces.stream().map(xns -> xns.getNs()).collect(Collectors.toSet());

	for (XmlNamespace otherXns : otherXmlEncodingInfos.getXmlNamespaces()) {
	    if (!thisNamespaces.contains(otherXns.getNs())) {
		thisNamespaces.add(otherXns.getNs());
		this.add(otherXns);
	    }
	}
    }

    @Override
    public String message(int mnr) {

	switch (mnr) {

	case 1:
	    return "No XML encoding infos to write to file '$1$'.";
	case 2:
	    return "Encountered an exception while writing XML encoding infos to file '$1$'. Exception message is: $2$";

	default:
	    return "(MapEntries.java) Unknown message with number: " + mnr;
	}

    }

}
