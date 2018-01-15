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
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */

package de.interactive_instruments.ShapeChange.Model;

import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

import de.interactive_instruments.ShapeChange.Options;

/**
 * NOTE: implementing classes should ensure that String interning is performed
 * for both the tag name and the value(s) while storing a tagged value in the
 * internal data structures (see {@link Options#internalize(String)}).
 * 
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public interface TaggedValues {

	/**
	 * @return the number of tagged values stored by this collection
	 */
	public int size();

	/**
	 * @return <code>true</code> if this collection is empty, else
	 *         <code>false</code>
	 */
	public boolean isEmpty();

	/**
	 * @param tag
	 * @return <code>true</code> if this collection contains value(s) for the
	 *         given tag, else <code>false</code>
	 */
	public boolean containsKey(String tag);

	/**
	 * @param tag
	 * @return An array with the values for the given tag, can be empty (but not
	 *         <code>null</code>) if this collection does not contain values for
	 *         the tag.
	 */
	public String[] get(String tag);

	/**
	 * NOTE: Implements deprecated access to tagged values. Use
	 * {@link Info#taggedValuesMult(String tagList)} instead.
	 * 
	 * @param tag
	 * @return The first of the list of values of the given tag that is stored
	 *         in this collection, <code>null</code> if this collection does not
	 *         contain value(s) for the given tag.
	 */
	public String getFirstValue(String tag);

	/**
	 * @return A map with the tagged values (key: tagged value id/name; value:
	 *         list with actual value(s)), can be empty but not
	 *         <code>null</code>.
	 *         <p>
	 *         NOTE 1: The resulting map is a deep copy of the tagged values
	 *         stored in this collection, thus modifications to the map won't be
	 *         reflected in this collection.
	 *         <p>
	 *         NOTE 2: if string interning is enabled (automatically checked and
	 *         performed through use of {@link Options#internalize(String)})
	 *         keys and string values of the resulting map have been
	 *         internalized.
	 */
	public SortedMap<String, List<String>> asMap();

	/**
	 * @return A set with the names of the tagged values stored in this
	 *         collection. Can be empty if this collection is empty but cannot
	 *         be <code>null</code>.
	 *         <p>
	 *         NOTE: The resulting set is a copy of the tagged value names
	 *         stored in this collection, thus modifications to the set won't be
	 *         reflected in this collection.
	 */
	public SortedSet<String> keySet();

	/**
	 * Puts the given tag and its value into this collection. Any value(s)
	 * previously stored for this tag will be replaced by the given value.
	 * 
	 * @param tag
	 * @param value
	 *            The value for the tag. NOTE: A <code>null</code> value is
	 *            automatically converted to the empty string
	 */
	public void put(String tag, String value);

	/**
	 * Puts the given tag and its values into this collection. Any value(s)
	 * previously stored for this tag will be replaced by the given values.
	 * 
	 * @param tag
	 * @param values
	 *            The values for the tag. NOTE: A <code>null</code> value is
	 *            automatically converted to the empty string
	 */
	public void put(String tag, String[] values);

	/**
	 * Puts the given tag and its values into this collection. Any value(s)
	 * previously stored for this tag will be replaced by the given values.
	 * 
	 * @param tag
	 * @param values
	 *            The values for the tag. NOTE: A <code>null</code> value is
	 *            automatically converted to the empty string
	 */
	public void put(String tag, List<String> values);

	/**
	 * Put all given tagged values into this collection. Any value(s) previously
	 * stored for one of these tags will be replaced with the given values.
	 * 
	 * @param other
	 *            can be empty or <code>null</code>
	 */
	public void putAll(TaggedValues other);

	/**
	 * Adds the given tag and its value to this collection. If the collection
	 * already contains values for this tag, the new value is added at the end
	 * of the list of existing values.
	 * 
	 * @param tag
	 * @param value
	 *            The value for the tag. NOTE: A <code>null</code> value is
	 *            automatically converted to the empty string
	 */
	public void add(String tag, String value);

	/**
	 * NOTE: Implements deprecated access to tagged values. Use
	 * {@link Info#taggedValuesMult()} instead.
	 * 
	 * @return A map with the first of the list of values for each tag stored in
	 *         this collection. The resulting map can be empty (if this
	 *         collection is empty) but not <code>null</code>.
	 * 
	 */
	public SortedMap<String, String> getFirstValues();

	/**
	 * NOTE: Implements deprecated access to tagged values. Use
	 * {@link Info#taggedValuesMult(String tagList)} instead.
	 * 
	 * @param tagList
	 *            comma-separated list of tags
	 * @return A map with the first of the list of values for each of the given
	 *         tags that is stored in this collection. If the given tagList is
	 *         <code>null</code> or empty, an empty map will be returned. The
	 *         resulting map can be empty but not <code>null</code>.
	 * 
	 */
	public SortedMap<String, String> getFirstValues(String tagList);

	/**
	 * Removes the tagged value with given name from this collection.
	 * 
	 * @param tvName
	 */
	public void remove(String tvName);

	/**
	 * Removes the set of tagged values with given names from this collection.
	 * 
	 * @param tvNames
	 */
	public void remove(Set<String> tvNames);

	public Options options();
}
