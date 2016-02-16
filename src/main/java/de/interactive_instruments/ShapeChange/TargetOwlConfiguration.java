package de.interactive_instruments.ShapeChange;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;

public class TargetOwlConfiguration extends TargetConfiguration {

	private Map<String, String> stereotypeMappings;

	/**
	 * Creates a TargetOwlConfiguration.
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
	 * @param stereotypeMappings
	 *            Maps wellknown stereotypes to ontology types that represent
	 *            these stereotypes. The key is the wellknown stereotype in
	 *            lower case.
	 * @param advancedProcessConfigurations
	 *            the 'advancedProcessConfigurations' element from the
	 *            configuration of the process; <code>null</code> if it is not
	 *            set there
	 */
	public TargetOwlConfiguration(String className, ProcessMode processMode,
			Map<String, String> parameters,
			Map<String, ProcessRuleSet> ruleSets,
			List<ProcessMapEntry> mapEntries, Set<String> inputIds,
			List<Namespace> namespaces, Map<String, String> stereotypeMappings,
			Element advancedProcessConfigurations) {

		super(className, processMode, parameters, ruleSets, mapEntries,
				inputIds, namespaces, advancedProcessConfigurations);

		this.stereotypeMappings = stereotypeMappings;

	}

	/**
	 * @return the stereotypeMappings
	 */
	public Map<String, String> getStereotypeMappings() {
		return stereotypeMappings;
	}

}
