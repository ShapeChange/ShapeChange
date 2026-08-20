/**
 * ShapeChange - processing application schemas for geographic information
 *
 * This file is part of ShapeChange. ShapeChange takes a ISO 19109 Application
 * Schema from a UML model and translates it into a GML Application Schema or
 * other implementation representations.
 *
 * Additional information about the software can be found at
 * http://shapechange.net/
 *
 * (c) 2002-2026 interactive instruments GmbH, Bonn, Germany
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact:
 * interactive instruments GmbH
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */
package de.interactive_instruments.shapechange.core.model;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

/**
 * Builds the {@link Comparator} described by the <code>sortedOutput</code>
 * configuration parameter.
 * <p>
 * The parameter accepts:
 * <ul>
 * <li><code>false</code> - do not sort; {@link #forConfiguredValue(String)}
 * returns an empty {@link Optional} and the caller keeps the order it already
 * has,</li>
 * <li><code>true</code> - equivalent to <code>name</code>,</li>
 * <li>the name of any no-argument method of {@link Info} that returns a
 * {@link String}, for example <code>name</code> or <code>id</code>,</li>
 * <li><code>taggedValue=&lt;tag&gt;</code> - sort by that tagged value, falling
 * back to {@link Info#name()} for elements that do not carry it.</li>
 * </ul>
 * <p>
 * The comparator is defined over {@link Info} rather than over
 * {@link ClassInfo}, so that the same configured order can be applied to every
 * kind of model element a consumer iterates - classes, packages, properties -
 * instead of each consumer reimplementing the parameter.
 * <p>
 * Ordering is <em>total</em>: elements that compare equal on the requested key
 * are ordered by {@link Info#id()}, which is unique within a model. Without
 * that tie-break the result would depend on the iteration order of the input,
 * which defeats the purpose of the parameter for consumers that serialise a
 * model and expect the same model to produce the same output.
 *
 * @author ShapeChange contributors
 */
public final class InfoOrdering {

    /** The configuration parameter this class implements. */
    public static final String PARAM_SORTED_OUTPUT = "sortedOutput";

    private static final String VALUE_FALSE = "false";
    private static final String VALUE_TRUE = "true";
    private static final String KEY_NAME = "name";
    private static final String KEY_TAGGED_VALUE = "taggedValue";
    private static final String PREFIX_TAGGED_VALUE = KEY_TAGGED_VALUE + "=";

    private InfoOrdering() {
	// utility class
    }

    /**
     * @param sortedOutputValue value of the <code>sortedOutput</code>
     *                          configuration parameter; may be
     *                          <code>null</code>
     * @return the comparator to apply, or an empty {@link Optional} if the value
     *         is <code>null</code> or <code>false</code>, meaning that the
     *         existing order is to be kept
     * @throws IllegalArgumentException if the value names neither a suitable
     *                                  method of {@link Info} nor a tagged value
     */
    public static Optional<Comparator<Info>> forConfiguredValue(String sortedOutputValue) {

	if (sortedOutputValue == null || VALUE_FALSE.equalsIgnoreCase(sortedOutputValue)) {
	    return Optional.empty();
	}

	if (sortedOutputValue.startsWith(PREFIX_TAGGED_VALUE)) {
	    String tag = sortedOutputValue.substring(PREFIX_TAGGED_VALUE.length());
	    if (StringUtils.isBlank(tag)) {
		throw new IllegalArgumentException(sortedOutputValue);
	    }
	    return Optional.of(withIdTieBreak(comparingTaggedValue(tag)));
	}

	String methodName = VALUE_TRUE.equalsIgnoreCase(sortedOutputValue) ? KEY_NAME : sortedOutputValue;

	final Method accessor;
	try {
	    accessor = Info.class.getMethod(methodName);
	} catch (NoSuchMethodException | SecurityException e) {
	    throw new IllegalArgumentException(sortedOutputValue, e);
	}
	if (!String.class.equals(accessor.getReturnType())) {
	    throw new IllegalArgumentException(sortedOutputValue);
	}

	return Optional.of(withIdTieBreak(comparingAccessor(accessor)));
    }

    /**
     * Convenience for the common case of sorting a collection in place.
     *
     * @param <T>                the kind of model element
     * @param elements           elements to order; not modified
     * @param sortedOutputValue  value of the <code>sortedOutput</code> parameter
     * @return a new list, ordered as configured, or a list in the original order
     *         if no sorting was requested
     * @throws IllegalArgumentException if the value is not a legal parameter value
     */
    public static <T extends Info> List<T> ordered(Iterable<T> elements, String sortedOutputValue) {

	List<T> result = new ArrayList<>();
	for (T element : elements) {
	    result.add(element);
	}
	forConfiguredValue(sortedOutputValue).ifPresent(result::sort);
	return result;
    }

    private static Comparator<Info> comparingAccessor(Method accessor) {
	return (i1, i2) -> {
	    try {
		return StringUtils.compare((String) accessor.invoke(i1), (String) accessor.invoke(i2));
	    } catch (ReflectiveOperationException e) {
		/*
		 * The accessor was resolved on Info and verified to return String, so an
		 * invocation failure is a defect in the model implementation rather than a
		 * configuration problem. Treat the elements as equal and let the id tie-break
		 * keep the order total.
		 */
		return 0;
	    }
	};
    }

    private static Comparator<Info> comparingTaggedValue(String tag) {
	return (i1, i2) -> StringUtils.compare(taggedValueOrName(i1, tag), taggedValueOrName(i2, tag));
    }

    private static String taggedValueOrName(Info info, String tag) {
	String value = info.taggedValue(tag);
	return StringUtils.isBlank(value) ? info.name() : value;
    }

    private static Comparator<Info> withIdTieBreak(Comparator<Info> primary) {
	return primary.thenComparing(Info::id, StringUtils::compare);
    }
}
