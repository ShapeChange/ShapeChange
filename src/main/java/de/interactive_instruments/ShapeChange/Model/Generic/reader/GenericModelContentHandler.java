package de.interactive_instruments.ShapeChange.Model.Generic.reader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;

public class GenericModelContentHandler extends AbstractContentHandler {

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

		if (localName.equals("Model")) {

			if (atts != null) {
				String encoding_ = atts.getValue("encoding");
				if (encoding_ != null) {
					this.encoding = encoding_;
				}
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

			// do not throw an exception, just log a warning - the schema could
			// have been extended
			result.addWarning(null, 30800, "GenericModelContentHandler",
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
