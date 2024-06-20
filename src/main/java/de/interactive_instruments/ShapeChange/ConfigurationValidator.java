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
 * (c) 2002-2016 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange;

/**
 * A configuration validator is used to check that the configuration of a
 * transformation or target is valid. The validator can check parameters, rules,
 * map entries, etc.
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
public interface ConfigurationValidator {

	/**
	 * Validates the given configuration. Any invalidity is directly logged in
	 * the ShapeChangeResult.
	 * <p>
	 * WARNING: The validator should use the given ProcessConfiguration for
	 * retrieving parameters, map entries etc. Currently it is not safe to get
	 * this information from options, since options is not configured with that
	 * ProcessConfiguration. If the configuration contains multiple targets of
	 * the same type, this can lead to parameter values being returned from the
	 * wrong target configuration. Simply rely on the given
	 * ProcessConfiguration. That also leads to more readable code.
	 * 
	 * @param pConfig tbd
	 * @param o tbd
	 * @param r tbd
	 * @return <code>true</code> if the configuration is valid, else
	 *         <code>false</code>
	 */
	public boolean isValid(ProcessConfiguration pConfig, Options o,
			ShapeChangeResult r);
}
