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
 * (c) 2002-2012 interactive instruments GmbH, Bonn, Germany
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

package de.interactive_instruments.shapechange.core.target;

import de.interactive_instruments.shapechange.core.Converter;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;

public interface SingleTarget extends Target {

	/**
	 * Instructs the target to output the processing results.
	 * <p>
	 * Will be called by the {@link Converter} after initialization and
	 * processing for all of the selected schema has been performed.
	 * 
	 * @param r tbd
	 * @see de.interactive_instruments.shapechange.core.model.Model#selectedSchemas()
	 */
	public void writeAll(ShapeChangeResult r);

	/**
	 * All relevant fields of the target will be reset, so that it is ready for
	 * processing selected schemas based upon a specific target configuration.
	 * <p>
	 * The {@link Converter} will reset SingleTargets whenever it processes a
	 * target configuration.
	 */
	public void reset();

}
