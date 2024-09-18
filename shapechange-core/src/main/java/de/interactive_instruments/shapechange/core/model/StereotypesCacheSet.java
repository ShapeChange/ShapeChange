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
package de.interactive_instruments.shapechange.core.model;

import java.util.SortedSet;
import java.util.TreeSet;

import de.interactive_instruments.shapechange.core.Options;

/**
 * Stores a collection of stereotypes making use of sets.
 * 
 * NOTE: this implementation is optimized to handle the cases in which the
 * collection has a single or no member. This can save a significant amount of
 * memory when processing large models.
 * 
 * @author Clemens Portele (portele at interactive-instruments dot de)
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
public class StereotypesCacheSet extends StereotypesImpl {

	/**
	 * Optimization:
	 * <ul>
	 * <li>If the cache only contains a single stereotype, this field is used.
	 * <li>If the cache contains more than one stereotype 'stSet' is used and
	 * this field is <code>null</code>.</li>
	 * <li>If the cache does not contain any stereotype, this field is
	 * <code>null</code>.</li>
	 * </ul>
	 */
	private String st = null;

	/**
	 * Optimization:
	 * <ul>
	 * <li>If the cache contains more than one stereotype, this field is used.
	 * </li>
	 * <li>If the cache contains only a single stereotype, the field 'st' is
	 * used and this field is <code>null</code>.</li>
	 * <li>If the cache does not contain any stereotype, this field is
	 * <code>null</code>.</li>
	 * </ul>
	 */
	private SortedSet<String> stSet = null;

	public StereotypesCacheSet(Options options) {

		this.options = options;
	}

	public StereotypesCacheSet(Stereotypes stereotypes, Options options) {

		this.options = options;

		if (stereotypes != null && !stereotypes.isEmpty()) {

			for (String s : stereotypes.asArray()) {
				this.add(s);
			}
		}
	}

	@Override
	public int size() {
		if (st == null && stSet == null) {
			return 0;
		} else if (st != null) {
			return 1;
		} else {
			return stSet.size();
		}
	}

	@Override
	public boolean contains(String stereotype) {

		if (st != null) {
			return st.equals(stereotype);
		} else if (stSet != null) {
			return stSet.contains(stereotype);
		} else {
			return false;
		}
	}

	@Override
	public String[] asArray() {

		String[] result;

		if (st != null) {
			result = new String[1];
			result[0] = st;
		} else if (stSet != null) {
			result = stSet.toArray(new String[stSet.size()]);
		} else {
			return new String[0];
		}

		return result;
	}
	
	@Override
	public SortedSet<String> asSet() {
		
		SortedSet<String> result = new TreeSet<String>();
		
		if (st != null) {
			result.add(st);
		} else if (stSet != null) {
			result.addAll(stSet);
		} 

		return result;
	}

	@Override
	public void add(String stereotype) {

		if (stereotype == null || stereotype.trim().length() == 0) {

			// given stereotype is not an actual one
			return;

		} else if (st == null && stSet == null) {

			// cache does not contain a stereotype yet
			st = options.internalize(stereotype);

		} else {

			// cache contains at least one stereotype

			if (st != null) {

				/*
				 * Cache currently contains exactly one stereotype - move it
				 * from st to stSet.
				 */

				stSet = new TreeSet<String>();
				stSet.add(options.internalize(st));

				// ensure that st is no longer used
				st = null;

			} else {

				// stSet has already been initialized
			}

			// finally, add the new stereotype
			stSet.add(options.internalize(stereotype));
		}
	}

	@Override
	public void remove(String stereotype) {

		if (stereotype == null || stereotype.trim().length() == 0) {

			// nothing to do

		} else if (st == null && stSet == null) {

			// nothing to do

		} else {

			// cache contains at least one stereotype

			if (st != null) {

				/*
				 * Cache currently contains exactly one stereotype.
				 */

				if (st.equals(stereotype)) {
					st = null;
				}

			} else {

				// stSet has been initialized - multiple stereotypes exist

				if (stSet.remove(stereotype)) {

					/*
					 * the stereotype existed in stSet - determine if only one
					 * stereotype remains
					 */
					if(stSet.size() == 1) {
						st = stSet.first();
						stSet = null;
					}
				}
			}
		}
	}

	@Override
	public String getOne() {

		if (st != null) {
			return st;
		} else if (stSet != null && !stSet.isEmpty()) {
			return stSet.iterator().next();
		} else {
			return "";
		}
	}
}
