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
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */

package de.interactive_instruments.ShapeChange.Model;

/**
 * The interface TextConstraint handles objects, which contain a constraint,
 * other than an OCL constraint, which in the view of ShapeChange's model
 * objects is just simple text. There may be a conspiracy between the model
 * source and target code generators, which gives meaning to TextConstraints.
 */
public interface TextConstraint extends Constraint {

	/**
	 * @return the 'type' of the text based constraint. Principally, types are
	 *         chosen in conspiracy between the model source and target code
	 *         generator to describe the nature of the text-based constraint
	 *         contained in the implementing object. The value of type must not
	 *         be equal to "OCL".
	 */
	public String type();
}
