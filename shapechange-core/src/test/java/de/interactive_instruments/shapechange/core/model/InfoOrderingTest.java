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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact:
 * interactive instruments GmbH
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.shapechange.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link InfoOrdering}, which implements the <code>sortedOutput</code>
 * configuration parameter.
 *
 * @author ShapeChange contributors
 */
public class InfoOrderingTest {

    /**
     * A minimal {@link Info}: only id, name and tagged values are relevant to
     * ordering, so the remaining methods are left unimplemented rather than pulling
     * in a mocking framework.
     */
    private static Info info(String id, String name, Map<String, String> taggedValues) {
	return (Info) Proxy.newProxyInstance(Info.class.getClassLoader(), new Class<?>[] { Info.class },
		(proxy, method, args) -> switch (method.getName()) {
		case "id" -> id;
		case "name" -> name;
		case "taggedValue" -> taggedValues.get((String) args[0]);
		case "toString" -> name + "#" + id;
		case "equals" -> proxy == args[0];
		case "hashCode" -> System.identityHashCode(proxy);
		default -> throw new UnsupportedOperationException(method.getName());
		});
    }

    private static Info info(String id, String name) {
	return info(id, name, Map.of());
    }

    private static List<String> names(List<Info> ordered) {
	return ordered.stream().map(Info::name).toList();
    }

    @Test
    public void noValueMeansKeepTheExistingOrder() {
	assertTrue(InfoOrdering.forConfiguredValue(null).isEmpty());
	assertTrue(InfoOrdering.forConfiguredValue("false").isEmpty());
	assertTrue(InfoOrdering.forConfiguredValue("FALSE").isEmpty());
    }

    @Test
    public void trueMeansByName() {
	Info a = info("2", "alpha");
	Info b = info("1", "beta");

	for (String value : List.of("true", "TRUE", "name")) {
	    Comparator<Info> comparator = InfoOrdering.forConfiguredValue(value).orElseThrow();
	    assertTrue(comparator.compare(a, b) < 0, value);
	}
    }

    @Test
    public void idOrdersByIdNotByName() {
	Info a = info("1", "zulu");
	Info b = info("2", "alpha");

	Comparator<Info> comparator = InfoOrdering.forConfiguredValue("id").orElseThrow();

	assertTrue(comparator.compare(a, b) < 0);
    }

    @Test
    public void taggedValueOrdersByThatTagAndFallsBackToName() {
	Info tagged = info("1", "zulu", Map.of("order", "aaa"));
	Info untagged = info("2", "mike");

	Comparator<Info> comparator = InfoOrdering.forConfiguredValue("taggedValue=order").orElseThrow();

	// 'aaa' before 'mike': the untagged element is compared by its name
	assertTrue(comparator.compare(tagged, untagged) < 0);
    }

    @Test
    public void orderingIsTotalSoEqualKeysFallBackToId() {
	Info first = info("1", "same");
	Info second = info("2", "same");

	Comparator<Info> comparator = InfoOrdering.forConfiguredValue("name").orElseThrow();

	assertTrue(comparator.compare(first, second) < 0);
	assertTrue(comparator.compare(second, first) > 0);
	assertEquals(0, comparator.compare(first, first));
    }

    @Test
    public void unusableValuesAreRejected() {
	// not a method of Info
	assertThrows(IllegalArgumentException.class, () -> InfoOrdering.forConfiguredValue("notAMethod"));
	// a method of Info, but it does not return a String
	assertThrows(IllegalArgumentException.class, () -> InfoOrdering.forConfiguredValue("stereotypes"));
	// no tag named
	assertThrows(IllegalArgumentException.class, () -> InfoOrdering.forConfiguredValue("taggedValue="));
    }

    @Test
    public void orderedSortsWithoutLosingOrMutatingElements() {
	Info a = info("3", "charlie");
	Info b = info("1", "alpha");
	Info c = info("2", "bravo");
	List<Info> input = Arrays.asList(a, b, c);

	assertEquals(List.of("alpha", "bravo", "charlie"), names(InfoOrdering.ordered(input, "name")));
	assertEquals(List.of("charlie", "alpha", "bravo"), names(InfoOrdering.ordered(input, "false")));
	// the caller's collection is untouched
	assertEquals(List.of("charlie", "alpha", "bravo"), names(input));
    }

    @Test
    public void orderedKeepsEveryElementWhenKeysCollide() {
	List<Info> input = Arrays.asList(info("3", "same"), info("1", "same"), info("2", "same"));

	List<Info> ordered = InfoOrdering.ordered(input, "name");

	assertEquals(3, ordered.size(), "elements with equal keys must not be dropped");
	assertEquals(List.of("1", "2", "3"), ordered.stream().map(Info::id).toList());
	assertFalse(ordered == input);
    }
}
