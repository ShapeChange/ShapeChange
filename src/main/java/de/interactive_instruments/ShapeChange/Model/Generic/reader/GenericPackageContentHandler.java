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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.ImageMetadata;
import de.interactive_instruments.ShapeChange.Model.Stereotypes;
import de.interactive_instruments.ShapeChange.Model.TaggedValues;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericClassInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericPackageInfo;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class GenericPackageContentHandler
		extends AbstractGenericInfoContentHandler {

	private static final Set<String> SIMPLE_PACKAGE_FIELDS = new HashSet<String>(
			Arrays.asList(new String[] { }));

	private static final Set<String> DEPRECATED_SIMPLE_PACKAGE_FIELDS = new HashSet<String>(
			Arrays.asList(new String[] { "isAppSchema", "isSchema", "targetNamespace", "xmlns",
					"xsdDocument", "version" }));

	private boolean isInPackages = false;

	private GenericPackageInfo genPi;

	private List<GenericPackageContentHandler> childPackageContentHandlers = new ArrayList<GenericPackageContentHandler>();
	private List<GenericClassContentHandler> classContentHandlers = new ArrayList<GenericClassContentHandler>();

	public GenericPackageContentHandler(ShapeChangeResult result,
			Options options, XMLReader reader, AbstractContentHandler parent) {
		super(result, options, reader, parent);

		this.genPi = new GenericPackageInfo();
		this.genPi.setResult(result);
		this.genPi.setOptions(options);
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {

		if (GenericModelReaderConstants.SIMPLE_INFO_FIELDS
				.contains(localName)) {

			sb = new StringBuffer();

		} else if (localName.equals("descriptors")) {

			DescriptorsContentHandler handler = new DescriptorsContentHandler(
					result, options, reader, this);

			super.descriptorsHandler = handler;
			reader.setContentHandler(handler);

		} else if (localName.equals("taggedValues")) {

			reader.setContentHandler(new TaggedValuesContentHandler(result,
					options, reader, this));

		} else if (localName.equals("stereotypes")) {

			reader.setContentHandler(new StringListContentHandler(result,
					options, reader, this));

		} else if (localName.equals("diagrams")) {

			reader.setContentHandler(
					new DiagramsContentHandler(result, options, reader, this));

		} else if (SIMPLE_PACKAGE_FIELDS.contains(localName)) {

			sb = new StringBuffer();

		} else if (DEPRECATED_SIMPLE_PACKAGE_FIELDS.contains(localName)) {

			// ignore

		} else if (localName.equals("supplierIds")) {

			reader.setContentHandler(new StringListContentHandler(result,
					options, reader, this));

		} else if (localName.equals("packages")) {

			isInPackages = true;

		} else if (localName.equals("Package")) {

			if (isInPackages) {
				GenericPackageContentHandler handler = new GenericPackageContentHandler(
						result, options, reader, this);
				this.childPackageContentHandlers.add(handler);
				reader.setContentHandler(handler);
			}

		} else if (localName.equals("classes")) {

			// ignore

		} else if (localName.equals("Class")) {

			GenericClassContentHandler handler = new GenericClassContentHandler(
					result, options, reader, this);
			this.classContentHandlers.add(handler);
			reader.setContentHandler(handler);

		} else {

			// do not throw an exception, just log a warning - the schema could
			// have been extended
			result.addWarning(null, 30800, "GenericPackageContentHandler",
					localName);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

		if (localName.equals("id")) {

			this.genPi.setId(sb.toString());

			// String id = sb.toString();
			// // strip "_PKG" prefix added by ModelExport
			// id = id.substring(4);
			// this.genPi.setId(id);

		} else if (localName.equals("name")) {

			this.genPi.setName(sb.toString());

		} else if (localName.equals("stereotypes")) {

			Stereotypes stereotypesCache = options.stereotypesFactory();
			for (String stereotype : this.stringList) {
				stereotypesCache.add(stereotype);
			}
			this.genPi.setStereotypes(stereotypesCache);

		} else if (localName.equals("descriptors")) {

			/*
			 * ignore - we have a reference to the DescriptorsContentHandler
			 */

		} else if (localName.equals("taggedValues")) {

			/*
			 * ignore - TaggedValuesContentHandler calls
			 * this.setTaggedValues(...)
			 */

		} else if (localName.equals("diagrams")) {

			/*
			 * ignore - DiagramsContentHandler calls this.setDiagrams(...)
			 */

		} else if (localName.equals("supplierIds")) {

			this.genPi.setSupplierIds(new TreeSet<String>(this.stringList));

		} else if (localName.equals("packages")) {

			isInPackages = false;

		} else if (localName.equals("classes")) {

			// ignore

		} else if (localName.equals("Class")) {

			// ignore

		} else if (localName.equals("Package")) {

			if (!isInPackages) {

				// set descriptors in genPi
				this.genPi.setDescriptors(descriptorsHandler.getDescriptors());

				// set contained packages
				SortedSet<GenericPackageInfo> children = new TreeSet<GenericPackageInfo>();
				for (GenericPackageContentHandler gpch : this.childPackageContentHandlers) {
					children.add(gpch.getGenericPackage());
				}
				this.genPi.setContainedPackages(children);

				// set contained classes
				SortedSet<GenericClassInfo> classes = new TreeSet<GenericClassInfo>();
				for (GenericClassContentHandler gcch : this.classContentHandlers) {
					classes.add(gcch.getGenericClass());
				}
				this.genPi.setClasses(classes);

				// let parent know that we reached the end of the Package entry
				// (so that for example depth can properly be tracked)
				parent.endElement(uri, localName, qName);

				// Switch handler back to parent
				reader.setContentHandler(parent);
			}

		} else if (DEPRECATED_SIMPLE_PACKAGE_FIELDS.contains(localName)) {

			// ignore

		} else {
			// do not throw an exception, just log a warning - the schema could
			// have been extended
			result.addWarning(null, 30801, "GenericPackageContentHandler",
					localName);
		}
	}

	/**
	 * @return the genPi
	 */
	public GenericPackageInfo getGenericPackage() {
		return genPi;
	}

	@Override
	public void setTaggedValues(TaggedValues taggedValues) {

		this.genPi.setTaggedValues(taggedValues, false);
	}

	@Override
	public void setDiagrams(List<ImageMetadata> diagrams) {

		this.genPi.setDiagrams(diagrams);
	}

	/**
	 * @return the content handlers for the child packages; can be empty but not
	 *         <code>null</code>
	 */
	public List<GenericPackageContentHandler> getChildPackageContentHandlers() {
		return childPackageContentHandlers;
	}

	/**
	 * @return the classContentHandlers
	 */
	public List<GenericClassContentHandler> getClassContentHandlers() {
		return classContentHandlers;
	}

	public void visitPackageContentHandlers(
			Map<String, GenericPackageContentHandler> resultMap) {

		resultMap.put(this.genPi.id(), this);

		for (GenericPackageContentHandler gpch : this.childPackageContentHandlers) {

			gpch.visitPackageContentHandlers(resultMap);
		}
	}
}
