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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

/**
 * Configuration of a transformer.
 * 
 * @author echterhoff
 * 
 */
public class TransformerConfiguration extends ProcessConfiguration {

	/**
	 * Identifier of the process, unique within a ShapeChangeConfiguration.
	 */
	private String id;

	/**
	 * Identifier of the input for this transformer. Can be the identifier of
	 * the global input model.
	 */
	private String inputId;

	/**
	 * key: rule of ProcessMapEntry value: map of all ProcessMapEntries defined
	 * for the rule (key of this map: type, value: ProcessMapEntry)
	 */
	private Map<String, Map<String, ProcessMapEntry>> mapEntriesByRule = new HashMap<String, Map<String, ProcessMapEntry>>();

	/**
	 * List of tagged values defined for the transformer; <code>null</code> if
	 * none are defined.
	 */
	private List<TaggedValueConfigurationEntry> taggedValues;

	/**
	 * Creates a TransformerConfiguration.
	 * 
	 * @param id
	 *            Transformer identifier
	 * @param className
	 *            The fully qualified name of the class implementing the
	 *            process.
	 * @param processMode
	 *            The execution mode of the process.
	 * @param parameters
	 *            The process parameters. <code>null</code> if no parameters
	 *            were declared in the configuration.
	 * @param ruleSets
	 *            The rule sets declared for the process. <code>null</code> if
	 *            no rule sets were declared in the configuration.
	 * @param mapEntries
	 *            The map entries for the process. <code>null</code> if no map
	 *            entries were declared in the configuration.
	 * @param taggedValues
	 *            The tagged values defined for the transformer.
	 *            <code>null</code> if no tagged values were declared in the
	 *            configuration.
	 * @param inputId
	 *            identifier of the input for this transformer
	 * @param advancedProcessConfigurations
	 *            the 'advancedProcessConfigurations' element from the
	 *            configuration of the process; <code>null</code> if it is not
	 *            set there
	 */
	public TransformerConfiguration(String id, String className,
			ProcessMode processMode, Map<String, String> parameters,
			Map<String, ProcessRuleSet> ruleSets,
			List<ProcessMapEntry> mapEntries,
			List<TaggedValueConfigurationEntry> taggedValues, String inputId,
			Element advancedProcessConfigurations) {
		super(className, processMode, parameters, ruleSets, mapEntries,
				advancedProcessConfigurations);
		this.id = id;
		this.inputId = inputId;

		if (mapEntries != null) {
			for (ProcessMapEntry mapEntry : mapEntries) {
				if (!mapEntriesByRule.containsKey(mapEntry.getRule())) {
					mapEntriesByRule.put(mapEntry.getRule(),
							new HashMap<String, ProcessMapEntry>());
				}
				mapEntriesByRule.get(mapEntry.getRule()).put(mapEntry.getType(),
						mapEntry);
			}
		}

		this.taggedValues = taggedValues;
	}

	/**
	 * @return The identifier of the process.
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return The identifier of the input for this transformer, for example
	 *         referencing the global model input.
	 */
	public String getInputId() {
		return inputId;
	}

	public String toString() {

		StringBuffer sb = new StringBuffer();

		sb.append("TransformerConfiguration:\r\n");
		sb.append("\tid: " + id);
		sb.append("--- with process configuration:\r\n");
		sb.append(super.toString());

		return sb.toString();
	}

	public ProcessMapEntry getMappingForType(String rule, String type) {
		if (rule == null || type == null)
			return null;
		else {
			Map<String, ProcessMapEntry> pme = mapEntriesByRule.get(rule);
			if (pme == null) {
				return null;
			} else {
				return pme.get(type);
			}
		}
	}

	/**
	 * @param rule
	 *            Name of the rule that the type mapping must apply to, can be
	 *            <code>null</code> (then any type mapping with the given name
	 *            is fine)
	 * @param type
	 *            Value of the type in a ProcessMapEntry
	 * 
	 * @return
	 */
	public boolean hasMappingForType(String rule, String type) {

		if (this.mapEntriesByRule.containsKey(rule)) {
			return mapEntriesByRule.get(rule).get(type) != null;
		} else {
			return false;
		}

	}

	public boolean hasTaggedValues() {
		return taggedValues != null;
	}

	public List<TaggedValueConfigurationEntry> getTaggedValues() {
		return taggedValues;
	}

	/**
	 * @return list of targets that this transformer and all other transformers
	 *         in the tree (where this transformer is the root) have.
	 */
	public List<TargetConfiguration> getAllTargets() {

		List<TargetConfiguration> targets = new ArrayList<TargetConfiguration>();

		if (this.getTargets() != null) {
			targets.addAll(this.getTargets());
		}

		if (this.getTransformers() != null) {
			for (TransformerConfiguration trfChild : this.getTransformers()) {
				List<TargetConfiguration> targetsOftrfChild = trfChild
						.getAllTargets();
				targets.addAll(targetsOftrfChild);
			}
		}
		return targets;
	}

}
