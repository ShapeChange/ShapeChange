package de.interactive_instruments.ShapeChange.Model.Generic.reader;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;

public class StringListContentHandler extends AbstractContentHandler {

	private int depth = 0;

	public StringListContentHandler(ShapeChangeResult result, Options options,
			XMLReader reader, AbstractContentHandler parent) {
		super(result, options, reader, parent);
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {

		depth++;
		sb = new StringBuffer();
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		
		depth--;
		
		if(depth == 0) {
			
			this.stringList.add(sb.toString());
			
		} else {
			
			// assume depth < 0 so we reached the end of the list
			
			parent.stringList = this.stringList;
			parent.endElement(uri, localName, qName);

			// Switch handler back to parent
			reader.setContentHandler(parent);
		}
	}

}
