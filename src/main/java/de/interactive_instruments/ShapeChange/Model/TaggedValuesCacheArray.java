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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import de.interactive_instruments.ShapeChange.Options;

/**
 * Cache for tagged values. It provides optimized storage of 'simple' tagged
 * values, i.e. those with only a single value. These tagged values are stored
 * in an array based structure, while those with multiple values are stored in a
 * map. The optimization is due to the fact that tagged values with multiple
 * values occur significantly less often than those with a single value.
 * 
 * The optimization can significantly reduce memory consumption when processing
 * large models.
 * 
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 * @author Clemens Portele (portele <at> interactive-instruments <dot> de)
 *
 * 
 */
public class TaggedValuesCacheArray extends TaggedValuesImpl {

	/**
	 * Contains tagged values that have only one value. Can be <code>null</code>
	 * if no such tagged values are stored in this collection.
	 * 
	 * <ul>
	 * <li>[0][] contains an array with the tagged value ids/names</li>
	 * <li>[1][] contains an array with the actual values</li>
	 * </ul>
	 * 
	 * The lengths of [0][] and [1][] are equal. The optimization lies in the
	 * fact that only two arrays need to be stored, not as many as there are
	 * tagged values. For processing of very large models this can reduce the
	 * overhead in memory consumption by 100+ megabytes.
	 */
	private String[][] simpleTags = null;

	/**
	 * Contains tagged values that have more than one value.
	 * 
	 * <ul>
	 * <li>key: tagged value id/name</li>
	 * <li>value: value(s) of the tagged value</li>
	 * </ul>
	 */
	private SortedMap<String, List<String>> complexTags = null;

	public TaggedValuesCacheArray(Options options) {

		this.options = options;
	}

	public TaggedValuesCacheArray(int size, Options options) {

		this.options = options;

		/*
		 * 2015-10-08 JE: we cannot initialize the fields 'simpleTags' and
		 * 'complexTags' here because we don't know anything about the tagged
		 * values to be stored yet.
		 */
	}

	public TaggedValuesCacheArray(TaggedValues original, Options options) {

		this.options = options;

		if (original == null || original.isEmpty()) {

			// no need to initialize the fields simpleTags and complexTags

		} else {

			/*
			 * create deep copy of information from original tagged values and
			 * use it to initialize the fields
			 */
			SortedMap<String, List<String>> tvsDeepCopy = original.asMap();

			initializeFields(tvsDeepCopy);
		}
	}

	public TaggedValuesCacheArray(TaggedValues original, String tagList,
			Options options) {

		this.options = options;

		if (original == null || original.isEmpty()) {

			// no need to initialize the fields simpleTags and complexTags

		} else {

			// identify tags from tagList
			String[] tags = tagList.split(",");
			SortedSet<String> tagsAsSet = new TreeSet<String>();
			for (String tag : tags) {
				if (tag.trim().length() != 0) {
					tagsAsSet.add(tag.trim());
				}
			}

			// create deep copy of information from original tagged values
			SortedMap<String, List<String>> tvsDeepCopy = original.asMap();

			/*
			 * remove all tagged values from deep copy that are not contained in
			 * the tag list
			 */
			SortedSet<String> tvsNotInTagList = new TreeSet<String>();

			for (String tvName : tvsDeepCopy.keySet()) {

				if (!tagsAsSet.contains(tvName)) {
					tvsNotInTagList.add(tvName);
				}
			}

			for (String tvToRemove : tvsNotInTagList) {
				tvsDeepCopy.remove(tvToRemove);
			}

			// initialize with remaining map entries
			initializeFields(tvsDeepCopy);
		}
	}

	/**
	 * Field {@link #simpleTags} is initialized if tvsDeepCopy contains tagged
	 * values with single value. Field {@link #complexTags} is initialized if
	 * there are tagged values with multiple values.
	 * 
	 * @param tvsDeepCopy
	 */
	private void initializeFields(SortedMap<String, List<String>> tvsDeepCopy) {

		if (tvsDeepCopy.isEmpty()) {

			// do not initialize the fields

		} else {

			// identify tags with single value
			SortedSet<String> tvsWithSingleValue = new TreeSet<String>();

			for (Entry<String, List<String>> e : tvsDeepCopy.entrySet()) {

				if (e.getValue().size() == 1) {
					tvsWithSingleValue.add(e.getKey());
				}
			}

			if (!tvsWithSingleValue.isEmpty()) {

				// store tags with single value in simpleTags
				this.simpleTags = new String[2][tvsWithSingleValue.size()];
				int i = 0;
				for (String tv : tvsWithSingleValue) {
					this.simpleTags[0][i] = tv;
					this.simpleTags[1][i] = tvsDeepCopy.get(tv).get(0);
					i++;
				}

				// remove tagged values with single value from deep copy
				for (String tv : tvsWithSingleValue) {
					tvsDeepCopy.remove(tv);
				}
			}

			// if deep copy is not empty, use it as value for complexTags field
			if (!tvsDeepCopy.isEmpty()) {
				this.complexTags = tvsDeepCopy;
			}
		}
	}

	@Override
	public boolean containsKey(String tag) {

		// search in complexTags first, then in simple tags
		if (complexTags != null && complexTags.containsKey(tag)) {

			return true;

		} else if (simpleTags != null) {

			for (String tvName : simpleTags[0]) {
				if (tvName.equals(tag)) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public String[] get(String tag) {

		// search in complexTags first, then in simple tags
		if (complexTags != null && complexTags.containsKey(tag)) {

			List<String> list = complexTags.get(tag);
			return list.toArray(new String[list.size()]);

		} else if (simpleTags != null) {

			int i = 0;
			for (String tvName : simpleTags[0]) {
				if (tvName.equals(tag)) {
					return new String[] { simpleTags[1][i] };
				}
				i++;
			}

		}

		return new String[]{};
	}

	@Override
	public SortedSet<String> keySet() {

		SortedSet<String> result = new TreeSet<String>();
		
		if (!isEmpty()) {

			if (complexTags != null) {
				result.addAll(complexTags.keySet());
			}

			if (simpleTags != null) {
				for (int i = 0; i < simpleTags[0].length; i++) {
					result.add(simpleTags[0][i]);
				}
			}
		}
		
		return result;
	}

	@Override
	public void put(String tag, String[] values) {

		if (tag == null) {
			// nothing to do
		} else {

			List<String> tmp = new ArrayList<String>(Arrays.asList(values));
			put(tag, tmp);
		}
	}

	@Override
	public void put(String tag, List<String> values) {

		if (tag == null) {
			// nothing to do
		} else {

			if (values.size() == 1) {

				// delegate to other method
				put(tag, values.get(0));

			} else {

				// delegate removal of tag
				remove(tag);

				// prepare addition of multi-valued tagged value
				if (complexTags == null) {
					complexTags = new TreeMap<String, List<String>>();
				}

				// ensure that values are internalized
				List<String> tmp = new ArrayList<String>(values.size());
				for (String value : values) {
					tmp.add(options.internalize(value));
				}

				// add multi-valued tagged value
				complexTags.put(options.internalize(tag), tmp);
			}
		}
	}

	@Override
	public void add(String tag, String value) {

		if (tag == null) {
			// nothing to do
		} else {

			if (complexTags != null && complexTags.containsKey(tag)) {

				// add new value to existing multi-valued tagged value
				complexTags.get(tag).add(options.internalize(value));

			} else if (simpleTags != null) {

				List<String> keys = new ArrayList<String>(
						Arrays.asList(simpleTags[0]));
				List<String> vals = new ArrayList<String>(
						Arrays.asList(simpleTags[1]));

				// check if tag is already stored
				int i = keys.indexOf(tag);

				if (i > -1) {

					/*
					 * tagged value already exists with one value - move it from
					 * simpleTags to complexTags, adding the new value
					 */
					List<String> multiValue = new ArrayList<String>();
					multiValue.add(options.internalize(vals.get(i)));
					multiValue.add(options.internalize(value));

					if (complexTags == null) {
						complexTags = new TreeMap<String, List<String>>();
					}
					complexTags.put(options.internalize(tag), multiValue);

					// remove tagged value from simpleTags
					keys.remove(i);
					vals.remove(i);

					if (keys.isEmpty()) {
						simpleTags = null;
					} else {
						simpleTags[0] = keys.toArray(new String[keys.size()]);
						simpleTags[1] = vals.toArray(new String[vals.size()]);
					}

				} else {

					// add the new tagged value to simpleTags
					keys.add(options.internalize(tag));
					vals.add(options.internalize(value));

					simpleTags[0] = keys.toArray(new String[keys.size()]);
					simpleTags[1] = vals.toArray(new String[vals.size()]);
				}

			} else {

				/*
				 * add new tagged value to simpleTags
				 */
				simpleTags = new String[2][1];
				simpleTags[0][0] = options.internalize(tag);
				simpleTags[1][0] = options.internalize(value);
			}
		}
	}

	@Override
	public SortedMap<String, String> getFirstValues() {

		SortedMap<String, String> res = new TreeMap<String, String>();

		if (complexTags != null) {
			for (Entry<String, List<String>> e : complexTags.entrySet()) {

				res.put(e.getKey(), e.getValue().get(0));
			}
		}

		if (simpleTags != null) {
			for (int i = 0; i < simpleTags[0].length; i++) {

				res.put(simpleTags[0][i], simpleTags[1][i]);
			}
		}

		return res;
	}

	@Override
	public SortedMap<String, String> getFirstValues(String tagList) {

		SortedMap<String, String> res = new TreeMap<String, String>();

		if (tagList == null || tagList.trim().isEmpty()) {

			// nothing to add to the result

		} else {

			/*
			 * Return a map with the first of the list of values for each of the
			 * given tags that is stored in this collection.
			 */

			String[] tags = tagList.split(",");
			Set<String> tagsAsSet = new TreeSet<String>();
			for (String tag : tags) {
				if (tag.trim().length() != 0) {
					tagsAsSet.add(tag.trim());
				}
			}

			if (complexTags != null) {
				for (Entry<String, List<String>> e : complexTags.entrySet()) {
					if (tagsAsSet.contains(e.getKey())) {
						res.put(e.getKey(), e.getValue().get(0));
					}
				}
			}

			if (simpleTags != null) {
				for (int i = 0; i < simpleTags[0].length; i++) {
					if (tagsAsSet.contains(simpleTags[0][i])) {
						res.put(simpleTags[0][i], simpleTags[1][i]);
					}
				}
			}

		}

		return res;
	}

	@Override
	public String getFirstValue(String tag) {

		if (tag == null) {

			return null;

		} else if (complexTags != null && complexTags.containsKey(tag)) {

			return complexTags.get(tag).get(0);

		} else if (simpleTags != null) {

			List<String> keys = new ArrayList<String>(
					Arrays.asList(simpleTags[0]));

			int i = keys.indexOf(tag);

			if (i > -1) {

				return simpleTags[1][i];

			} else {
				// not found in simpleTags
				return null;
			}

		} else {

			return null;
		}
	}

	@Override
	public int size() {
		int countComplexTags = complexTags == null ? 0 : complexTags.size();
		int countSimpleTags = simpleTags == null ? 0 : simpleTags[0].length;

		return countComplexTags + countSimpleTags;
	}

	@Override
	public boolean isEmpty() {
		if (size() == 0) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public SortedMap<String, List<String>> asMap() {

		if (isEmpty()) {

			return new TreeMap<String, List<String>>();

		} else {

			SortedMap<String, List<String>> result = new TreeMap<String, List<String>>();

			if (complexTags != null) {
				for (Entry<String, List<String>> e : complexTags.entrySet()) {

					List<String> values = new ArrayList<String>(
							e.getValue().size());

					for (String v : e.getValue()) {
						values.add(options.internalize(v));
					}

					result.put(options.internalize(e.getKey()), values);
				}
			}

			if (simpleTags != null) {

				for (int i = 0; i < simpleTags[0].length; i++) {

					List<String> values = new ArrayList<String>(1);
					values.add(options.internalize(simpleTags[1][i]));

					result.put(options.internalize(simpleTags[0][i]), values);
				}
			}

			return result;
		}
	}

	@Override
	public void put(String tag, String value) {

		if (tag == null) {
			// nothing to do
		} else {

			if (complexTags != null && complexTags.containsKey(tag)) {

				// remove the multi-valued tagged value
				complexTags.remove(tag);
				if (complexTags.isEmpty()) {
					complexTags = null;
				}

				// add tagged value to simpleTags
				add(tag, value);

			} else {

				if (simpleTags != null) {

					// check if tag is already stored
					int i = Arrays.asList(simpleTags[0]).indexOf(tag);

					if (i > -1) {

						// tag already stored, simply replace the value
						simpleTags[1][i] = options.internalize(value);

					} else {

						// add the new tagged value
						add(tag, value);
					}

				} else {

					// tag is definitely new - add it
					add(tag, value);
				}
			}
		}
	}

	@Override
	public void remove(String tvName) {

		if (tvName == null) {
			// nothing to do
		} else {

			if (complexTags != null && complexTags.containsKey(tvName)) {

				complexTags.remove(tvName);
				if (complexTags.isEmpty()) {
					complexTags = null;
				}

			} else if (simpleTags != null) {

				// identify index where tv is stored

				List<String> keys = new ArrayList<String>(
						Arrays.asList(simpleTags[0]));
				List<String> vals = new ArrayList<String>(
						Arrays.asList(simpleTags[1]));

				int i = keys.indexOf(tvName);

				if (i > -1) {

					// tag was found at index i
					keys.remove(i);
					vals.remove(i);

					if (keys.isEmpty()) {
						simpleTags = null;
					} else {
						simpleTags[0] = keys.toArray(new String[keys.size()]);
						simpleTags[1] = vals.toArray(new String[vals.size()]);
					}

				} else {
					// tag was not found - nothing to remove
				}

			} else {
				// the cache definitely does not contain the given tag
			}
		}
	}

	@Override
	public void remove(Set<String> tvNames) {

		if (tvNames == null || tvNames.isEmpty()) {
			// nothing to do
		} else {

			// search and remove in complexTags
			if (complexTags != null) {

				for (String tvName : tvNames) {
					if (complexTags.containsKey(tvName)) {
						complexTags.remove(tvName);
					}
				}
				if (complexTags.isEmpty()) {
					complexTags = null;
				}

			}

			// also search and remove in simpleTags
			if (simpleTags != null) {

				// use lists as intermediate data structure
				List<String> keys = new ArrayList<String>(
						Arrays.asList(simpleTags[0]));
				List<String> vals = new ArrayList<String>(
						Arrays.asList(simpleTags[1]));

				boolean tvRemoved = false;

				for (String tvName : tvNames) {

					// identify index where tv is stored
					int i = keys.indexOf(tvName);

					if (i > -1) {

						keys.remove(i);
						vals.remove(i);
						tvRemoved = true;

					} else {
						// tv not stored, continue
					}
				}

				if (tvRemoved) {

					// changes occurred - update simpleTags

					if (keys.isEmpty()) {
						simpleTags = null;
					} else {
						simpleTags[0] = keys.toArray(new String[keys.size()]);
						simpleTags[1] = vals.toArray(new String[vals.size()]);
					}
				}
			}
		}
	}
}
