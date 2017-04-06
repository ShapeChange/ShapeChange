package de.interactive_instruments.ShapeChange.Model.Generic.reader;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.ImageMetadata;

public class DiagramsContentHandler extends AbstractContentHandler {

	private static final Set<String> IMAGE_METADATA_FIELDS = new HashSet<String>(
			Arrays.asList(new String[] { "id", "name", "file", "relPathToFile",
					"width", "height" }));

	private AbstractGenericInfoContentHandler parent;

	private List<ImageMetadata> diagrams = new ArrayList<ImageMetadata>();

	private String id = null;
	private String name = null;
	private String file = null;
	private String relPathToFile = null;
	private String width = null;
	private String height = null;

	public DiagramsContentHandler(ShapeChangeResult result, Options options,
			XMLReader reader, AbstractGenericInfoContentHandler parent) {
		super(result, options, reader, parent);
		this.parent = parent;
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {

		if (localName.equals("ImageMetadata")) {

			// reset fields
			this.id = null;
			this.name = null;
			this.file = null;
			this.relPathToFile = null;
			this.width = null;
			this.height = null;

		} else if (IMAGE_METADATA_FIELDS.contains(localName)) {

			sb = new StringBuffer();

		} else {

			// do not throw an exception, just log a warning - the schema could
			// have been extended
			result.addWarning(null, 30800, "DiagramsContentHandler", localName);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

		if (localName.equals("id")) {

			this.id = sb.toString();

		} else if (localName.equals("name")) {

			this.name = sb.toString();

		} else if (localName.equals("file")) {

			this.file = sb.toString();

		} else if (localName.equals("relPathToFile")) {

			this.relPathToFile = sb.toString();

		} else if (localName.equals("width")) {

			this.width = sb.toString();

		} else if (localName.equals("height")) {

			this.height = sb.toString();

		} else if (localName.equals("ImageMetadata")) {

			try {

				File f = new File(file);
				int w = Integer.parseInt(width);
				int h = Integer.parseInt(height);

				ImageMetadata im = new ImageMetadata(id, name, f, relPathToFile,
						w, h);
				this.diagrams.add(im);

			} catch (NumberFormatException e) {
				result.addError(null, 30802, id, name, e.getMessage());
			}

		} else if (localName.equals("diagrams")) {

			parent.setDiagrams(diagrams);

			// let parent know that we reached the end of the diagrams entry
			// (so that for example depth can properly be tracked)
			parent.endElement(uri, localName, qName);

			// Switch handler back to parent
			reader.setContentHandler(parent);

		} else {
			// do not throw an exception, just log a warning - the schema could
			// have been extended
			result.addWarning(null, 30801, "DiagramsContentHandler", localName);
		}
	}

}
