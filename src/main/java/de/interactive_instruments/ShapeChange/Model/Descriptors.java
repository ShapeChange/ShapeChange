package de.interactive_instruments.ShapeChange.Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Descriptors {

	// private LangString singleValue = null;
	//
	// /**
	// * Used to store multiple string values and their (optional) language
	// * identifiers.
	// *
	// * <ul>
	// * <li>[0][] contains an array with the string values</li>
	// * <li>[1][] contains an array with the language identifier of each string
	// * value (can be null if the value has no language identifier)</li>
	// * </ul>
	// *
	// * The lengths of [0][] and [1][] are equal. The optimization lies in the
	// * fact that only two arrays need to be stored, not as many as there are
	// * language tagged string values. For processing of very large models this
	// * can reduce the overhead in memory consumption considerably.
	// */
	// private String[][] values = null;

	/**
	 * Key: descriptor Value: list with values for the descriptor (may have
	 * language tag, also multiple languages) - can be <code>null</code> if no
	 * values are available for the descriptor (using null will have slightly
	 * improved memory footprint, which can be significant for large models that
	 * are flattened)
	 */
	private Map<Descriptor, List<LangString>> descriptorValues = new HashMap<Descriptor, List<LangString>>();

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

		List<LangString> result = this.descriptorValues.get(descriptor);

		if (result == null) {
			result = new ArrayList<LangString>();
		}

		return result;
	}

	public void put(Descriptor descriptor, List<LangString> descriptorValues) {

		if (descriptorValues == null || descriptorValues.isEmpty()) {
			this.descriptorValues.put(descriptor, null);
		} else {
			this.descriptorValues.put(descriptor, descriptorValues);
		}
	}

	public void put(Descriptor descriptor, String stringValue) {

		if (stringValue == null) {
			this.descriptorValues.put(descriptor, null);
		} else {
			List<LangString> values = new ArrayList<LangString>();
			values.add(new LangString(stringValue));
			this.descriptorValues.put(descriptor, values);
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
			List<LangString> val = new ArrayList<LangString>();
			val.add(ls);
			this.descriptorValues.put(descriptor, val);
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

			this.descriptorValues.put(descriptor, copy);
		}
	}

	public void putCopy(Descriptor[] descriptorsToCopy,
			Descriptors originalDescriptors) {

		for (Descriptor descriptor : descriptorsToCopy) {
			this.putCopy(descriptor, originalDescriptors.values(descriptor));
		}

	}

	public Descriptors createCopy() {

		Descriptors copy = new Descriptors();

		for (Entry<Descriptor, List<LangString>> entry : this.descriptorValues
				.entrySet()) {

			List<LangString> valueCopy = new ArrayList<LangString>();

			if (entry.getValue() != null) {
				for (LangString ls : entry.getValue()) {
					valueCopy.add(new LangString(ls.getValue(), ls.getLang()));
				}
			}

			copy.put(entry.getKey(), valueCopy);
		}

		return copy;
	}
}
