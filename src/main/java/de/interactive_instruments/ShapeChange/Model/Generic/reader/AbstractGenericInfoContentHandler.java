package de.interactive_instruments.ShapeChange.Model.Generic.reader;

import java.util.List;

import org.xml.sax.XMLReader;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.ImageMetadata;
import de.interactive_instruments.ShapeChange.Model.TaggedValues;

public abstract class AbstractGenericInfoContentHandler extends AbstractContentHandler {

	protected DescriptorsContentHandler descriptorsHandler = null;
	
	public AbstractGenericInfoContentHandler(ShapeChangeResult result,
			Options options, XMLReader reader, AbstractContentHandler parent) {
		super(result, options, reader, parent);
	}
	
	public abstract void setTaggedValues(TaggedValues taggedValues);
	public abstract void setDiagrams(List<ImageMetadata> diagrams);			
}
