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
 * (c) 2002-2015 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Model;

import java.util.SortedSet;

import de.interactive_instruments.ShapeChange.Options;

public interface Stereotypes {

	/**
	 * @return the number of members contained in this collection of stereotypes
	 */
	public int size();

	/**
	 * @return <code>true</code> if this collection of stereotypes is empty,
	 *         else <code>false</code>
	 */
	public boolean isEmpty();

	/**
	 * @param stereotype tbd
	 * @return <code>true</code> if this collection of stereotypes contains the
	 *         given stereotype, else <code>false</code>
	 */
	public boolean contains(String stereotype);

	/**
	 * @return the content of this stereotype collection; can be empty if this
	 *         collection is empty but not <code>null</code>
	 */
	public String[] asArray();

	/**
	 * @return the content of this stereotype collection as a set; can be empty
	 *         if this collection is empty but not <code>null</code>
	 */
	public SortedSet<String> asSet();

	/**
	 * Adds the given stereotype to this collection of stereotypes
	 * 
	 * NOTE: Also internalizes the given String.
	 * 
	 * @param stereotype tbd
	 */
	public void add(String stereotype);

	/**
	 * Removes the given stereotype from this collection of stereotypes.
	 * 
	 * @param stereotype tbd
	 */
	public void remove(String stereotype);

	/**
	 * @return the ShapeChange options
	 */
	public Options options();

	/**
	 * @return the empty string if this collection of stereotypes is empty,
	 *         otherwise (any) one of the members in the collection
	 */
	public String getOne();

	/**
	 * @return the empty string if the list of stereotypes is empty, otherwise a
	 *         comma-separated list of the stereotypes
	 */
	public String toString();
}
