package de.interactive_instruments.ShapeChange.Model.Generic.reader;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.LangString;
import de.interactive_instruments.ShapeChange.Model.Descriptors;

public class DescriptorValuesContentHandler extends AbstractContentHandler {

	private List<LangString> descriptorValues = new ArrayList<LangString>();

	private String value = null;
	private String lang = null;

	public DescriptorValuesContentHandler(ShapeChangeResult result,
			Options options, XMLReader reader, AbstractContentHandler parent) {
		super(result, options, reader, parent);
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {

		if (localName.equals("descriptorValues")) {

			// ignore

		} else if (localName.equals("DescriptorValue")) {
						
			if(atts != null) {
				this.lang = atts.getValue("lang");
			} else {
				this.lang = null;
			}

			sb = new StringBuffer();			
			this.value = null;
//			this.lang = null;

		}
//		else if (localName.equals("value")) {
//
//			sb = new StringBuffer();
//
//		}
//		else if (localName.equals("lang")) {
//
//			sb = new StringBuffer();
//
//		} 
		else {

			// do not throw an exception, just log a warning - the schema could
			// have been extended
			result.addWarning(null, 30800, "DescriptorValuesContentHandler",
					localName);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

		if (localName.equals("descriptorValues")) {

			/*
			 * the parent has a reference to this content handler so can invoke
			 * getDescriptors()
			 */

			/*
			 * let parent know that we reached the end of the descriptorValues
			 * element (so that for example depth can properly be tracked)
			 */
			parent.endElement(uri, localName, qName);

			// Switch handler back to parent
			reader.setContentHandler(parent);

		} else if (localName.equals("DescriptorValue")) {

			this.value = sb.toString();
			
			LangString dv = new LangString(
					options.internalize(this.value),
					options.internalize(this.lang));
			this.descriptorValues.add(dv);

		}
//		else if (localName.equals("value")) {
//
//			this.value = sb.toString();
//
//		} else if (localName.equals("lang")) {
//
//			this.lang = sb.toString();
//
//		}
		else {

			// do not throw an exception, just log a warning - the schema could
			// have been extended
			result.addWarning(null, 30801, "DescriptorValuesContentHandler",
					localName);
		}
	}

	public List<LangString> getDescriptorValues() {
		return this.descriptorValues;
	}

}
