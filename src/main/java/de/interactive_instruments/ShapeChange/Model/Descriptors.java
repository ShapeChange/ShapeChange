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
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange.Model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Cache for descriptors.
 * 
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class Descriptors {

	/**
	 * This internal class provides optimized storage of 'simple' descriptors,
	 * i.e. those with only a single value. These descriptors store the value in
	 * a separate field, while descriptors with multiple values use an array
	 * list (that is trimmed to size). The optimization is due to the fact that
	 * descriptors with multiple values occur significantly less often than
	 * those with a single value.
	 * 
	 * The optimization can significantly reduce memory consumption when
	 * processing large models.
	 * 
	 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
	 *         <dot> de)
	 *
	 */
	private class DescriptorValue {

		private LangString singleValue = null;
		private ArrayList<LangString> multipleValues = null;

		DescriptorValue(LangString value) {

			if (value != null) {
				this.singleValue = value;
			}
		}

		DescriptorValue(List<LangString> values) {

			if (values != null && !values.isEmpty()) {
				if (values.size() == 1) {
					this.singleValue = values.get(0);
				} else {
					this.multipleValues = new ArrayList<LangString>();
					this.multipleValues.addAll(values);
					this.multipleValues.trimToSize();
				}
			}
		}

		/**
		 * @return can be empty but not <code>null</code>
		 */
		List<LangString> getValues() {

			List<LangString> result = new ArrayList<LangString>();

			if (singleValue != null) {
				result.add(singleValue);
			} else if (multipleValues != null) {
				result.addAll(multipleValues);
			}

			return result;
		}

		void appendSuffix(String s) {

			if (s != null) {
				if (singleValue != null) {
					singleValue.appendSuffix(s);
				} else if (multipleValues != null) {
					for (LangString val : multipleValues) {
						val.appendSuffix(s);
					}
				}
			}
		}
	}

	/**
	 * Key: descriptor Value: object with values for the descriptor (may have
	 * language tag, also multiple languages) - can be <code>null</code> if no
	 * values are available for the descriptor (using null will have slightly
	 * improved memory footprint, which can be significant for large models that
	 * are flattened)
	 */
	private EnumMap<Descriptor, DescriptorValue> descriptorValues = new EnumMap<Descriptor, DescriptorValue>(
			Descriptor.class);

	/**
	 * Constructor for empty object (to avoid having to deal with
	 * <code>null</code>).
	 * 
	 * @param options
	 */
	public Descriptors() {
	}

	public boolean has(Descriptor descriptor) {
		return this.descriptorValues.containsKey(descriptor);
	}

	/**
	 * @param descriptor
	 * @return List with values for the descriptor; can be empty but not null;
	 */
	public List<LangString> values(Descriptor descriptor) {

		List<LangString> result = new ArrayList<LangString>();

		DescriptorValue dv = this.descriptorValues.get(descriptor);
		if (dv != null) {
			result.addAll(dv.getValues());
		}

		return result;
	}

	public DescriptorValue get(Descriptor descriptor) {

		return this.descriptorValues.get(descriptor);
	}

	public void put(Descriptor descriptor, List<LangString> descriptorValues) {

		if (descriptorValues.isEmpty()) {
			this.descriptorValues.put(descriptor, null);
		} else {
			DescriptorValue dv = new DescriptorValue(descriptorValues);
			this.descriptorValues.put(descriptor, dv);
		}
	}

	public void put(Descriptor descriptor, String stringValue) {

		if (stringValue == null) {
			this.descriptorValues.put(descriptor, null);
		} else {
			DescriptorValue dv = new DescriptorValue(
					new LangString(stringValue));
			this.descriptorValues.put(descriptor, dv);
		}
	}

	public void put(Descriptor descriptor, String[] stringValues) {

		if (stringValues == null || stringValues.length == 0) {

			this.descriptorValues.put(descriptor, null);

		} else {

			List<LangString> values = new ArrayList<LangString>();

			for (String s : stringValues) {
				values.add(new LangString(s));
			}

			this.put(descriptor, values);
		}
	}

	public void put(Descriptor descriptor, LangString ls) {

		if (ls == null) {
			this.descriptorValues.put(descriptor, null);
		} else {
			DescriptorValue dv = new DescriptorValue(ls);
			this.descriptorValues.put(descriptor, dv);
		}
	}

	public void putCopy(Descriptor descriptor, List<LangString> values) {

		if (values == null || values.isEmpty()) {
			this.descriptorValues.put(descriptor, null);
		} else {
			List<LangString> copy = new ArrayList<LangString>();

			for (LangString ls : values) {
				copy.add(new LangString(ls.getValue(), ls.getLang()));
			}

			DescriptorValue dv = new DescriptorValue(copy);

			this.descriptorValues.put(descriptor, dv);
		}
	}

	public void putCopy(Descriptor[] descriptorsToCopy,
			Descriptors originalDescriptors) {

		for (Descriptor descriptor : descriptorsToCopy) {
			this.putCopy(descriptor, originalDescriptors.values(descriptor));
		}

	}

	public void remove(Descriptor descriptor) {
		this.descriptorValues.remove(descriptor);
	}

	public Descriptors createCopy() {

		Descriptors copy = new Descriptors();

		for (Entry<Descriptor, Descriptors.DescriptorValue> entry : this.descriptorValues
				.entrySet()) {

			List<LangString> valueCopy = new ArrayList<LangString>();

			if (entry.getValue() != null) {
				for (LangString ls : entry.getValue().getValues()) {
					valueCopy.add(new LangString(ls.getValue(), ls.getLang()));
				}
			}

			copy.put(entry.getKey(), valueCopy);
		}

		return copy;
	}

	/**
	 * @param descriptor
	 * @return a deep copy of the values stored for the given descriptor; can be
	 *         empty but not <code>null</code>
	 */
	public List<LangString> createValueCopy(Descriptor descriptor) {

		List<LangString> valueCopy = new ArrayList<LangString>();

		for (LangString ls : this.values(descriptor)) {
			valueCopy.add(new LangString(ls.getValue(), ls.getLang()));
		}

		return valueCopy;
	}

	/**
	 * Appends suffixes for descriptors that a) are available as key in the
	 * given map and for which b) values are stored in this collection. The
	 * value pairs of the given map contain the separator and the suffix to be
	 * used when appending to a descriptor value.
	 * 
	 * @param separatorAndSuffixByDescriptor
	 *            Map (can be empty or null) with key: Descriptor; value: pair
	 *            of first the separator to use, then the suffix
	 */
	public void appendSuffix(
			EnumMap<Descriptor, Pair<String, String>> separatorAndSuffixByDescriptor,
			boolean addDescriptorIfMissing) {

		if (separatorAndSuffixByDescriptor != null) {

			for (Descriptor descriptor : separatorAndSuffixByDescriptor
					.keySet()) {

				Pair<String, String> separatorAndSuffix = separatorAndSuffixByDescriptor
						.get(descriptor);

				if (addDescriptorIfMissing && (!this.descriptorValues
						.containsKey(descriptor)
						|| this.descriptorValues.get(descriptor) == null)) {

					this.put(descriptor, separatorAndSuffix.getRight());

				} else if (this.descriptorValues != null
						&& this.descriptorValues.containsKey(descriptor)) {

					DescriptorValue dv = this.descriptorValues.get(descriptor);

					if (dv != null) {
						dv.appendSuffix(separatorAndSuffix.getLeft()
								+ separatorAndSuffix.getRight());
					}
				}
			}
		}
	}

	public static List<LangString> merge(Descriptor descriptor, Info info1,
			Info info2, EnumMap<Descriptor, String> separatorByDescriptor,
			String defaultSeparator) {

		String separator = defaultSeparator;
		if (separatorByDescriptor.containsKey(descriptor)) {
			separator = separatorByDescriptor.get(descriptor);
		}

		return merge(descriptor, info1, info2, separator);
	}

	public static List<LangString> merge(Descriptor descriptor, Info info1,
			Info info2, String separator) {

		Set<LangString> result = new HashSet<LangString>();

		List<LangString> info1DVs = info1.descriptors()
				.createValueCopy(descriptor);
		List<LangString> info2DVs = info2.descriptors()
				.createValueCopy(descriptor);

		Set<LangString> unmatched_info2DVs = new HashSet<LangString>(info2DVs);

		if (descriptor.isSingleValued()) {

			/*
			 * Merge matching values; just keep the other ones
			 */
			for (LangString i1val : info1DVs) {

				String d1 = StringUtils.stripToNull(i1val.getValue());

				/*
				 * Keep track if a value in info2DVs has the same lang as i1val.
				 */
				boolean matchFound = false;

				for (LangString i2val : info2DVs) {

					String d2 = StringUtils.stripToNull(i2val.getValue());

					/*
					 * Both i1val and i2val must either have no long, or both
					 * have the same lang.
					 */
					if ((!i1val.hasLang() && !i2val.hasLang())
							|| (i1val.hasLang() && i2val.hasLang()
									&& i1val.getLang().equalsIgnoreCase(
											i2val.getLang()))) {

						matchFound = true;

						if (d1 == null && d2 == null) {
							/*
							 * No actual value for this particular language id.
							 * Nothing to merge.
							 */
							result.add(i1val);
						} else if (d1 == null) {
							/*
							 * No actual value for i1val. Nothing to merge.
							 */
							result.add(i2val);
						} else if (d2 == null) {
							/*
							 * No actual value for i2val. Nothing to merge.
							 */
							result.add(i1val);
						} else {
							LangString merge = new LangString(StringUtils
									.join(new String[] { d1, d2 }, separator));
							result.add(merge);
						}

						/*
						 * i2val no longer matters in any case
						 */
						unmatched_info2DVs.remove(i2val);
					}
				}

				/*
				 * No value in info2DVs had the same lang as i1val. Add i1val to
				 * the result.
				 */
				if (!matchFound) {
					result.add(i1val);
				}
			}

			/*
			 * Add the remaining values from info2, the ones that have not been
			 * matched.
			 */
			for (LangString i2val : unmatched_info2DVs) {
				result.add(i2val);
			}

		} else {

			/*
			 * Simply add all values to the result.
			 */
			result.addAll(info1DVs);
			result.addAll(info2DVs);
		}

		return new ArrayList<LangString>(result);
	}
}
