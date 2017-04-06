package de.interactive_instruments.ShapeChange.Model.Generic.reader;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;

public abstract class AbstractContentHandler implements ContentHandler {

	protected ShapeChangeResult result;
	protected Options options;
	protected XMLReader reader;
	protected AbstractContentHandler parent = null;

	protected StringBuffer sb;
	protected List<String> stringList = new ArrayList<String>();

	public AbstractContentHandler(ShapeChangeResult result, Options options,
			XMLReader reader, AbstractContentHandler parent) {

		this.result = result;
		this.options = options;
		this.reader = reader;
		this.parent = parent;
	}

	@Override
	public void setDocumentLocator(Locator locator) {
		// irrelevant
	}

	@Override
	public void startDocument() throws SAXException {
		// irrelevant
	}

	@Override
	public void endDocument() throws SAXException {
		// irrelevant
	}

	@Override
	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {
		// irrelevant
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		// irrelevant
	}

	@Override
	public abstract void startElement(String uri, String localName,
			String qName, Attributes atts) throws SAXException;

	@Override
	public abstract void endElement(String uri, String localName, String qName)
			throws SAXException;

	/**
	 * Appends the content of the character array to the internal string buffer.
	 * 
	 * @see org.xml.sax.ContentHandler#characters(char[], int, int)
	 */
	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {

		if (sb != null) {
			sb.append(ch, start, length);
		}
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length)
			throws SAXException {
		// irrelevant
	}

	@Override
	public void processingInstruction(String target, String data)
			throws SAXException {
		// irrelevant
	}

	@Override
	public void skippedEntity(String name) throws SAXException {
		// irrelevant
	}

	/**
	 * @param sb
	 * @return <code>true</code> if the value of the StringBuffer is 'true' or
	 *         '1', else <code>false</code>.
	 */
	protected boolean toBooleanValue(StringBuffer sb) {
		String value = sb.toString().trim();
		boolean res = value.equalsIgnoreCase("true") || value.equals("1");
		return res;
	}
	
}
