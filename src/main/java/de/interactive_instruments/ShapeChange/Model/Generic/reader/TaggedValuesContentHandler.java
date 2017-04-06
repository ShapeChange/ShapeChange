package de.interactive_instruments.ShapeChange.Model.Generic.reader;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.TaggedValues;

public class TaggedValuesContentHandler extends AbstractContentHandler {

	private TaggedValues taggedValues;
	private String name;
	
	protected AbstractGenericInfoContentHandler parent;

	public TaggedValuesContentHandler(ShapeChangeResult result, Options options,
			XMLReader reader, AbstractGenericInfoContentHandler parent) {
		super(result, options, reader, parent);

		this.parent = parent;
		this.taggedValues = options.taggedValueFactory();
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {

		if (localName.equals("TaggedValue")) {

			/*
			 * ensure that string list for values is reset for each occurrence
			 * of a 'TaggedValue'
			 */
			this.stringList = new ArrayList<String>();

		} else if (localName.equals("name")) {

			this.sb = new StringBuffer();

		} else if (localName.equals("values")) {

			reader.setContentHandler(new StringListContentHandler(result,
					options, reader, this));
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

		if (localName.equals("name")) {

			this.name = sb.toString();

		} else if (localName.equals("values")) {

			// ignore

		} else if (localName.equals("TaggedValue")) {

			if (!this.stringList.isEmpty()) {

				taggedValues.put(this.name, this.stringList);

			} else {

				taggedValues.add(this.name, "");
			}

		} else if (localName.equals("taggedValues")) {

			parent.setTaggedValues(taggedValues);
			
			parent.endElement(uri, localName, qName);

			// Switch handler back to parent
			reader.setContentHandler(parent);
		}
	}
}
