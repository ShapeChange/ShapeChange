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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.shapechange.core.model.generic.reader;

import java.util.List;

import org.xml.sax.XMLReader;

import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.model.ImageMetadata;
import de.interactive_instruments.shapechange.core.model.TaggedValues;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
public abstract class AbstractGenericInfoContentHandler
		extends AbstractContentHandler {

	protected DescriptorsContentHandler descriptorsHandler = null;

	public AbstractGenericInfoContentHandler(ShapeChangeResult result,
			Options options, XMLReader reader, AbstractContentHandler parent) {
		super(result, options, reader, parent);
	}

	public abstract void setTaggedValues(TaggedValues taggedValues);

	public abstract void setDiagrams(List<ImageMetadata> diagrams);
}
