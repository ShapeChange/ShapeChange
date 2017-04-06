package de.interactive_instruments.ShapeChange.Model.Generic.reader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.Model.Descriptor;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.Descriptors;
import de.interactive_instruments.ShapeChange.Model.LangString;

public class DescriptorsContentHandler extends AbstractContentHandler {

	public static final Set<String> DESCRIPTOR_FIELDS = new HashSet<String>(
			Arrays.asList(new String[] { "alias", "primaryCode",
					"globalIdentifier", "documentation", "definition",
					"description", "legalBasis", "language", "example",
					"dataCaptureStatement" }));

	private Descriptors descriptors = new Descriptors();

	private DescriptorValuesContentHandler descriptorValuesHandler = null;

	public DescriptorsContentHandler(ShapeChangeResult result, Options options,
			XMLReader reader, AbstractGenericInfoContentHandler parent) {
		super(result, options, reader, parent);
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {

		if (DESCRIPTOR_FIELDS.contains(localName)) {

			DescriptorValuesContentHandler handler = new DescriptorValuesContentHandler(
					result, options, reader, this);

			this.descriptorValuesHandler = handler;
			reader.setContentHandler(handler);

		} else if (localName.equals("descriptors")) {

			// ignore

		} else {

			// do not throw an exception, just log a warning - the schema could
			// have been extended
			result.addWarning(null, 30800, "DescriptorsContentHandler",
					localName);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

		if (localName.equals("alias")) {

			this.descriptors.put(Descriptor.ALIAS,
					this.descriptorValuesHandler.getDescriptorValues());

		} else if (localName.equals("documentation")) {

			this.descriptors.put(Descriptor.DOCUMENTATION,
					this.descriptorValuesHandler.getDescriptorValues());

		} else if (localName.equals("definition")) {

			this.descriptors.put(Descriptor.DEFINITION,
					this.descriptorValuesHandler.getDescriptorValues());

		} else if (localName.equals("description")) {

			this.descriptors.put(Descriptor.DESCRIPTION,
					this.descriptorValuesHandler.getDescriptorValues());

		} else if (localName.equals("legalBasis")) {

			this.descriptors.put(Descriptor.LEGALBASIS,
					this.descriptorValuesHandler.getDescriptorValues());

		} else if (localName.equals("primaryCode")) {

			this.descriptors.put(Descriptor.PRIMARYCODE,
					this.descriptorValuesHandler.getDescriptorValues());

		} else if (localName.equals("globalIdentifier")) {

			this.descriptors.put(Descriptor.GLOBALIDENTIFIER,
					this.descriptorValuesHandler.getDescriptorValues());

		} else if (localName.equals("language")) {

			this.descriptors.put(Descriptor.LANGUAGE,
					this.descriptorValuesHandler.getDescriptorValues());

		} else if (localName.equals("example")) {

			this.descriptors.put(Descriptor.EXAMPLE,
					this.descriptorValuesHandler.getDescriptorValues());

		} else if (localName.equals("dataCaptureStatement")) {

			this.descriptors.put(Descriptor.DATACAPTURESTATEMENT,
					this.descriptorValuesHandler.getDescriptorValues());

		} else if (localName.equals("descriptorValues")) {

			// ignore

		} else if (localName.equals("descriptors")) {

			/*
			 * the parent has a reference to this content handler, so can invoke
			 * getDescriptors()
			 */

			/*
			 * Ensure that for any descriptor that was not available in the
			 * model XML, we set an empty value list (to prevent an attempt to
			 * load descriptor values again in InfoImpl.descriptorValues()
			 */
			for (Descriptor descriptor : Descriptor.values()) {

				if (this.descriptors.has(descriptor)) {
					// fine, the descriptor has been loaded from the model XML
				} else {
					List<LangString> list = new ArrayList<LangString>();
					this.descriptors.put(descriptor, list);
				}
			}

			/*
			 * let parent know that we reached the end of the descriptorValues
			 * element (so that for example depth can properly be tracked)
			 */
			parent.endElement(uri, localName, qName);

			// Switch handler back to parent
			reader.setContentHandler(parent);

		} else {
			// do not throw an exception, just log a warning - the schema could
			// have been extended
			result.addWarning(null, 30801, "DescriptorsContentHandler",
					localName);
		}
	}

	public Descriptors getDescriptors() {
		return this.descriptors;
	}

}
