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
 * (c) 2002-2019 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import de.interactive_instruments.shapechange.core.target.Target;
import de.interactive_instruments.shapechange.core.util.ShapeChangeClassFinder;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class TargetRegistry {

    protected List<Class<?>> targetClasses;

    protected Map<String, String> targetClassNameByIdentifier = new HashMap<>();
    protected Map<String, String> targetDefaultEncodingRuleByIdentifier = new HashMap<>();
    protected SortedSet<String> targetIdentifiers = new TreeSet<>();

    public TargetRegistry() throws ShapeChangeAbortException {

	targetClasses = ShapeChangeClassFinder
		.findClassesImplementing("de.interactive_instruments.shapechange.core.target.Target");

	for (Class<?> tc : targetClasses) {

	    try {
		Target target = (Target) tc.getConstructor().newInstance();

		// gather target identifiers
		String targetIdentifier = target.getTargetIdentifier();
		if (targetClassNameByIdentifier.containsKey(targetIdentifier)) {
		    throw new ShapeChangeAbortException("Duplicate target identifier '" + targetIdentifier
			    + "'. Found for targets " + targetClassNameByIdentifier.get(targetIdentifier)
			    + " and target " + tc.getName()
			    + ". The latter would overwrite the former in the target registry, which can lead to unexpected results. "
			    + "Execution will stop. Contact the target implementers to ensure that "
			    + "the identifiers of the targets are unique.");
		}
		targetIdentifiers.add(targetIdentifier);
		targetClassNameByIdentifier.put(targetIdentifier, tc.getName());
		String targetDefaultEncodingRule = target.getDefaultEncodingRule();
		if (targetDefaultEncodingRule == null) {
		    targetDefaultEncodingRule = "*";
		}
		targetDefaultEncodingRuleByIdentifier.put(targetIdentifier, targetDefaultEncodingRule);

	    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
		    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
		System.err.println("Exception occurred while loading target class " + tc.getName()
			+ " and attempting to gather target identifiers.");
		e.printStackTrace();
	    }
	}
    }

    public List<Class<?>> getTargetClasses() {
	return this.targetClasses;
    }

    /**
     * @param targetIdentifier tbd
     * @return the fully qualified class name of the target with the given
     *         identifier; can be <code>null</code> if no such target was found
     */
    public String targetClassName(String targetIdentifier) {
	return targetClassNameByIdentifier.get(targetIdentifier);
    }

    /**
     * @param targetIdentifier tbd
     * @return the default encoding rule defined for the target with the given
     *         identifier; can be <code>null</code> if no such target was found
     */
    public String targetDefaultEncodingRule(String targetIdentifier) {
	return targetDefaultEncodingRuleByIdentifier.get(targetIdentifier);
    }

    /**
     * @return the set of identifiers of available (on the classpath) Target
     *         implementations; can be empty (though unlikely) but not
     *         <code>null</code>
     */
    public SortedSet<String> getTargetIdentifiers() {
	return this.targetIdentifiers;
    }

}
