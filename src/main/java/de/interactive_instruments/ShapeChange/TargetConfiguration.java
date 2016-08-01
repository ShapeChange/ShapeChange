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
 * (c) 2002-2013 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 */
public class TargetConfiguration extends ProcessConfiguration {

	private Set<String> inputIds;
	private List<Namespace> namespaces;

	/**
	 * key: namespace URI; value: Namespace object (with that namespace)
	 */
	private Map<String, Namespace> namespaceByNamespace = new HashMap<String, Namespace>();

	/**
	 * key: namespace abbreviation; value: Namespace object (with that
	 * abbreviation)
	 */
	private Map<String, Namespace> namespaceByAbbreviation = new HashMap<String, Namespace>();

	/**
	 * Creates a TargetConfiguration.
	 * 
	 * @param className
	 *            The fully qualified name of the class implementing the target.
	 * @param processMode
	 *            The execution mode of the target.
	 * @param parameters
	 *            The target parameters. <code>null</code> if no parameters were
	 *            declared in the configuration.
	 * @param ruleSets
	 *            The encoding rule sets declared for the target.
	 *            <code>null</code> if no rule sets were declared in the
	 *            configuration.
	 * @param mapEntries
	 *            The map entries for the target. <code>null</code> if no map
	 *            entries were declared in the configuration.
	 * @param inputIds
	 *            Set of identifiers referencing either the input model or a
	 *            transformer.
	 * @param namespaces
	 *            List of namespaces for the target. <code>null</code> if no
	 *            namespaces were declared in the configuration.
	 * @param advancedProcessConfigurations
	 *            the 'advancedProcessConfigurations' element from the
	 *            configuration of the process; <code>null</code> if it is not
	 *            set there
	 */
	public TargetConfiguration(String className, ProcessMode processMode,
			Map<String, String> parameters,
			Map<String, ProcessRuleSet> ruleSets,
			List<ProcessMapEntry> mapEntries, Set<String> inputIds,
			List<Namespace> namespaces, Element advancedProcessConfigurations) {

		super(className, processMode, parameters, ruleSets, mapEntries,
				advancedProcessConfigurations);
		this.inputIds = inputIds;

		this.namespaces = namespaces;

		if (namespaces != null) {
			for (Namespace ns : namespaces) {
				this.namespaceByNamespace.put(ns.getNs(), ns);
				this.namespaceByAbbreviation.put(ns.getNsabr(), ns);
			}
		}
	}

	public Set<String> getInputIds() {
		return inputIds;
	}

	public String toString() {

		StringBuffer sb = new StringBuffer();

		sb.append("TargetConfiguration:\r\n");
		sb.append("--- with process configuration:\r\n");
		sb.append(super.toString());
		sb.append("\tinputs: " + "\r\n");
		for (String s : inputIds) {
			sb.append("\t\t" + s + "\r\n");
		}
		if (namespaces != null) {
			sb.append("\tnamespaces: " + "\r\n");
			for (Namespace ns : namespaces) {

				sb.append("\t\t(" + ns.getNsabr() + "|" + ns.getNs() + "|"
						+ (ns.getLocation() != null ? ns.getLocation() : "NA")
						+ ")\r\n");
			}
		}

		return sb.toString();
	}

	/**
	 * @return the namespaces, or <code>null</code> if none are defined in the
	 *         target configuration
	 */
	public List<Namespace> getNamespaces() {
		return namespaces;
	}

	/**
	 * @param namespace
	 * @return The abbreviation for the given namespace, or <code>null</code> if
	 *         the namespace is unknown.
	 */
	public String nsabrForNamespace(String namespace) {

		if (namespaceByNamespace.containsKey(namespace)) {
			return namespaceByNamespace.get(namespace).getNsabr();
		} else {
			return null;
		}
	}

	/**
	 * @param abbreviation
	 * @return the namespace for the given abbreviation, or <code>null</code> if
	 *         the abbreviation is unknown
	 */
	public String fullNamespace(String abbreviation) {

		if (namespaceByAbbreviation.containsKey(abbreviation)) {
			return namespaceByAbbreviation.get(abbreviation).getNs();
		} else {
			return null;
		}
	}

	public boolean hasNamespaceWithAbbreviation(String abbrev) {
		return fullNamespace(abbrev) != null;
	}

	/**
	 * @param namespace
	 * @return the location defined for the namespace (in the configuration) -
	 *         or <code>null</code> if either the namespace or the location is
	 *         not defined in the configuration
	 */
	public String locationOfNamespace(String namespace) {

		if (namespaceByNamespace.containsKey(namespace)) {
			return namespaceByNamespace.get(namespace).getLocation();
		} else {
			return null;
		}
	}
}
