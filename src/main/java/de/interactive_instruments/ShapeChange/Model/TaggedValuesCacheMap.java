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
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.List;
import java.util.SortedMap;
import java.util.Map.Entry;
import java.util.SortedSet;

import de.interactive_instruments.ShapeChange.Options;

/**
 * @author Clemens Portele (portele <at> interactive-instruments <dot> de)
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class TaggedValuesCacheMap extends TaggedValuesImpl {

	private SortedMap<String, List<String>> tagMap = null;

	public TaggedValuesCacheMap(Options options) {
		tagMap = new TreeMap<String, List<String>>();
		this.options = options;
	}

	public TaggedValuesCacheMap(int size, Options options) {
		tagMap = new TreeMap<String, List<String>>();
		this.options = options;
	}

	public TaggedValuesCacheMap(TaggedValues original, Options options) {

		this.options = options;

		if (original != null) {

			tagMap = original.asMap();

		} else {

			tagMap = new TreeMap<String, List<String>>();
		}
	}

	public TaggedValuesCacheMap(TaggedValues original, String tagList,
			Options options) {

		this.options = options;

		String[] tags = tagList.split(",");
		SortedSet<String> tagsAsSet = new TreeSet<String>();
		for (String tag : tags) {
			if (tag.trim().length() != 0) {
				tagsAsSet.add(tag.trim());
			}
		}

		if (original != null) {

			tagMap = original.asMap();

			// remove the entries that are not contained in the tag list

			SortedSet<String> tvsToRemove = new TreeSet<String>();

			for (String tvn : tagMap.keySet()) {
				if (!tagsAsSet.contains(tvn)) {
					tvsToRemove.add(tvn);
				}
			}

			for (String tvToRemove : tvsToRemove) {
				tagMap.remove(tvToRemove);
			}

		} else {

			tagMap = new TreeMap<String, List<String>>();
		}
	}

	@Override
	public boolean containsKey(String tag) {

		return tagMap.containsKey(tag);
	}

	@Override
	public String[] get(String tag) {

		if (!containsKey(tag))
			return null;

		List<String> list = tagMap.get(tag);
		return list != null ? list.toArray(new String[list.size()]) : null;
	}

	@Override
	public SortedSet<String> keySet() {

		if (isEmpty()) {

			return new TreeSet<String>();

		} else {

			return new TreeSet<String>(tagMap.keySet());
		}
	}

	@Override
	public void put(String tag, String value) {

		List<String> tmp = new ArrayList<String>(1);
		tmp.add(options.internalize(value));

		this.put(options.internalize(tag), tmp);
	}

	@Override
	public void put(String tag, String[] values) {

		List<String> tmp = new ArrayList<String>(Arrays.asList(values));
		this.put(tag, tmp);
	}

	@Override
	public void put(String tag, List<String> values) {

		List<String> tmp = new ArrayList<String>(values.size());

		for (String v : values) {
			tmp.add(options.internalize(v));
		}

		tagMap.put(options.internalize(tag), tmp);
	}

	@Override
	public void add(String tag, String value) {

		if (containsKey(tag)) {

			tagMap.get(tag).add(options.internalize(value));

		} else {

			List<String> values = new ArrayList<String>();
			values.add(options.internalize(value));

			tagMap.put(options.internalize(tag), values);
		}
	}

	@Override
	public SortedMap<String, String> getFirstValues() {

		SortedMap<String, String> res = new TreeMap<String, String>();

		for (Entry<String, List<String>> e : tagMap.entrySet()) {

			res.put(e.getKey(), e.getValue().get(0));
		}

		return res;
	}

	@Override
	public SortedMap<String, String> getFirstValues(String tagList) {

		SortedMap<String, String> res = new TreeMap<String, String>();

		if (tagList == null || tagList.trim().isEmpty()) {

			// nothing to add to the result

		} else {

			String[] tags = tagList.split(",");
			SortedSet<String> tagsAsSet = new TreeSet<String>();
			for (String tag : tags) {
				if (tag.trim().length() != 0) {
					tagsAsSet.add(tag.trim());
				}
			}

			for (Entry<String, List<String>> e : tagMap.entrySet()) {
				if (tagsAsSet.contains(e.getKey())) {
					res.put(e.getKey(), e.getValue().get(0));
				}
			}
		}

		return res;
	}

	@Override
	public String getFirstValue(String tag) {

		List<String> l = tagMap.get(tag);

		return l != null && l.size() > 0 ? l.get(0) : null;
	} // getFirstValue()

	@Override
	public int size() {

		return tagMap.size();
	}

	@Override
	public SortedMap<String, List<String>> asMap() {

		if (isEmpty()) {

			return new TreeMap<String, List<String>>();

		} else {

			SortedMap<String, List<String>> result = new TreeMap<String, List<String>>();

			for (Entry<String, List<String>> e : tagMap.entrySet()) {

				List<String> values = new ArrayList<String>(
						e.getValue().size());

				for (String v : e.getValue()) {
					values.add(options.internalize(v));
				}

				result.put(options.internalize(e.getKey()), values);
			}

			return result;
		}
	}

	@Override
	public boolean isEmpty() {

		return tagMap.isEmpty();
	}

	@Override
	public void remove(String tvName) {
		if (tvName != null) {
			tagMap.remove(tvName);
		}
	}

	@Override
	public void remove(Set<String> tvNames) {
		if (tvNames != null) {
			for (String tvName : tvNames) {
				tagMap.remove(tvName);
			}
		}
	}
}
