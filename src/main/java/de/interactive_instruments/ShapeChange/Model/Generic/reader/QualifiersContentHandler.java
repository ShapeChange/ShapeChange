package de.interactive_instruments.ShapeChange.Model.Generic.reader;

import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.Qualifier;

public class QualifiersContentHandler extends AbstractContentHandler {

	private Vector<Qualifier> qualifiers = new Vector<Qualifier>();
	
	protected GenericPropertyContentHandler parent;
	
	private String name = null;
	private String type = null;

	public QualifiersContentHandler(ShapeChangeResult result, Options options,
			XMLReader reader, GenericPropertyContentHandler parent) {
		
		super(result, options, reader, parent);

		this.parent = parent;
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {

		if (localName.equals("Qualifier")) {

			// reset qualifier fields
			this.name = null;
			this.type = null;

		} else if (localName.equals("name")) {

			this.sb = new StringBuffer();

		} else if (localName.equals("type")) {

			this.sb = new StringBuffer();
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

		if (localName.equals("name")) {

			this.name = sb.toString();

		} else if (localName.equals("type")) {

			this.type = sb.toString();

		} else if (localName.equals("Qualifier")) {

			Qualifier qualifier = new Qualifier();
			qualifier.name = this.name;
			qualifier.type = this.type;
			
			this.qualifiers.addElement(qualifier);

		} else if (localName.equals("qualifiers")) {
			
			parent.setQualifiers(this.qualifiers);
			
			parent.endElement(uri, localName, qName);

			// Switch handler back to parent
			reader.setContentHandler(parent);
		}
	}
}
