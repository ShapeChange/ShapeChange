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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.w3c.dom.Element;

import com.google.common.base.Splitter;

/**
 * Configuration information for a process.
 * 
 * @author echterhoff
 * 
 */
public class ProcessConfiguration {

	/**
	 * The fully qualified name of the class implementing the process.
	 */
	private String className;
	/**
	 * The execution mode of the process.
	 */
	private ProcessMode processMode = ProcessMode.enabled;
	/**
	 * The process parameters. <code>null</code> if no parameters were declared
	 * in the configuration.
	 */
	private Map<String, String> parameters = null;
	/**
	 * The rule sets declared for the process. <code>null</code> if no rule sets
	 * were declared in the configuration.
	 */
	private Map<String, ProcessRuleSet> ruleSets = null;

	/**
	 * A list of the type map entries for the process. Can be empty but not
	 * <code>null</code>.
	 */
	private List<ProcessMapEntry> mapEntries;
	/**
	 * key; type; value: MapEntry object with that type
	 */
	private Map<String, ProcessMapEntry> mapEntryByType = new HashMap<String, ProcessMapEntry>();

	/**
	 * The 'advancedProcessConfigurations' XML element from the process
	 * configuration. <code>null</code> if it was not set there. Enables a
	 * process to check if relevant advanced configuration information is
	 * available, and to parse it.
	 */
	private Element advancedProcessConfigurations;

	/**
	 * The gml version that applies to the process. Default is 3.2.
	 */
	private String gmlVersion = "3.2";

	private List<TransformerConfiguration> transformers = new ArrayList<TransformerConfiguration>();
	private List<TargetConfiguration> targets = new ArrayList<TargetConfiguration>();

	/**
	 * Creates a ProcessConfiguration.
	 * 
	 * @param className
	 *                        The fully qualified name of the class implementing
	 *                        the process.
	 * @param processMode
	 *                        The execution mode of the process.
	 * @param parameters
	 *                        The process parameters. <code>null</code> if no
	 *                        parameters were declared in the configuration.
	 * @param ruleSets
	 *                        The rule sets declared for the process.
	 *                        <code>null</code> if no rule sets were declared in
	 *                        the configuration.
	 * @param mapEntries
	 *                        The map entries for the process. Can be
	 *                        <code>null</code> if no map entries were declared
	 *                        in the configuration.
	 * @param advancedProcessConfigurations tbd
	 */
	public ProcessConfiguration(String className, ProcessMode processMode,
			Map<String, String> parameters,
			Map<String, ProcessRuleSet> ruleSets,
			List<ProcessMapEntry> mapEntries,
			Element advancedProcessConfigurations) {
		super();
		this.className = className;
		this.processMode = processMode;
		this.parameters = parameters;
		this.ruleSets = ruleSets;
		this.advancedProcessConfigurations = advancedProcessConfigurations;

		if (mapEntries == null) {
			this.mapEntries = new ArrayList<>();
		} else {
			this.mapEntries = mapEntries;
			for (ProcessMapEntry pme : mapEntries) {
				this.mapEntryByType.put(pme.type, pme);
			}
		}

		if (parameters.containsKey("gmlVersion")) {
			this.gmlVersion = parameters.get("gmlVersion");
		}
	}

	/**
	 * @return The fully qualified name of the class implementing the process.
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * @return The execution mode of the process.
	 */
	public ProcessMode getProcessMode() {
		return processMode;
	}

	/**
	 * @return The process parameters. <code>null</code> if no parameters were
	 *         declared in the configuration.
	 */
	public Map<String, String> getParameters() {
		return parameters;
	}

	/**
	 * Gets the value of the parameter with given name.
	 * 
	 * @param parameterName
	 *                          Name of the parameter to get the value for.
	 * @return The value of the process parameter with given name.
	 *         <code>null</code> if no such parameter was declared in the
	 *         configuration.
	 */
	public String getParameterValue(String parameterName) {
		return parameters.get(parameterName);
	}

	public boolean parameterAsBoolean(String parameterName,
			boolean defaultValue) {

		boolean res;

		String valueByConfig = this.parameters.get(parameterName);

		if (valueByConfig == null) {
			res = defaultValue;
		} else {
			res = Boolean.parseBoolean(valueByConfig.trim());
		}

		return res;
	}

	/**
	 * @param parameterName
	 *                                            name of the parameter to
	 *                                            retrieve the value from
	 * @param defaultValue
	 *                                            value that will be returned if
	 *                                            no valid parameter value was
	 *                                            found; can be
	 *                                            <code>null</code>
	 * @param allowNonEmptyTrimmedStringValue
	 *                                            <code>true</code> if the
	 *                                            parameter value may be empty,
	 *                                            if trimmed, else
	 *                                            <code>false</code>
	 * @param trimValue
	 *                                            <code>true</code> if leading
	 *                                            and trailing whitespace shall
	 *                                            be removed from the parameter
	 *                                            value before returning it
	 * @return the parameter value, or the given default value (which can be
	 *         <code>null</code>) if the parameter value does not fulfill the
	 *         criteria
	 */
	public String parameterAsString(String parameterName, String defaultValue,
			boolean allowNonEmptyTrimmedStringValue, boolean trimValue) {

		String result = this.parameters == null ? null
				: this.parameters.get(parameterName);

		if (result == null || (result.trim().isEmpty()
				&& !allowNonEmptyTrimmedStringValue)) {
			result = defaultValue;
		} else if (trimValue) {
			result = result.trim();
		}

		return result;
	}

	/**
	 * @param parameterName
	 *                                  name of the parameter to retrieve the
	 *                                  regular expression from
	 * @param parameterDefaultValue
	 *                                  regular expression that will be used if
	 *                                  the parameter was not found; can be
	 *                                  <code>null</code>
	 * @return the compiled regular expression, if a regular expression was
	 *         provided (either via the parameter or via the default value)
	 * @throws PatternSyntaxException
	 *                                    If the regular expression could not be
	 *                                    compiled
	 */
	public Pattern parameterAsRegexPattern(String parameterName,
			String parameterDefaultValue) throws PatternSyntaxException {

		String regexText = this.parameters == null ? null
				: this.parameters.get(parameterName);

		if (regexText == null) {
			regexText = parameterDefaultValue;
		}

		if (regexText == null) {
			return null;
		} else {
			return Pattern.compile(regexText);
		}
	}

	/**
	 * @param parameterName
	 *                             name of the parameter to retrieve the comma
	 *                             separated values from
	 * @param defaultValues
	 *                             values that will be returned if no values
	 *                             were found; <code>null</code> is converted to
	 *                             an empty list of strings
	 * @param omitEmptyStrings
	 *                             <code>true</code> if values may NOT be empty
	 *                             if they were trimmed, else <code>false</code>
	 * @param trimResults
	 *                             <code>true</code> if leading and trailing
	 *                             whitespace shall be removed from a value
	 * @return list of values (originally separated by commas) retrieved from
	 *         this parameter, or the default values if the parameter was not
	 *         set or did not contain valid values; can be empty but not
	 *         <code>null</code>
	 */
	public List<String> parameterAsStringList(String parameterName,
			String[] defaultValues, boolean omitEmptyStrings,
			boolean trimResults) {

		return parameterAsStringList(parameterName, defaultValues,
				omitEmptyStrings, trimResults, ",");
	}

	/**
	 * @param parameterName
	 *                             name of the parameter to retrieve the
	 *                             separated values from
	 * @param defaultValues
	 *                             values that will be returned if no values
	 *                             were found; <code>null</code> is converted to
	 *                             an empty list of strings
	 * @param omitEmptyStrings
	 *                             <code>true</code> if values may NOT be empty
	 *                             if they were trimmed, else <code>false</code>
	 * @param trimResults
	 *                             <code>true</code> if leading and trailing
	 *                             whitespace shall be removed from a value
	 * @param separator
	 *                             the literal, nonempty string to recognize as
	 *                             a separator
	 * @return list of values (originally separated via the given separator)
	 *         retrieved from this parameter, or the default values if the
	 *         parameter was not set or did not contain valid values; can be
	 *         empty but not <code>null</code>
	 */
	public List<String> parameterAsStringList(String parameterName,
			String[] defaultValues, boolean omitEmptyStrings,
			boolean trimResults, String separator) {

		List<String> defaultValuesList = defaultValues == null
				? new ArrayList<String>()
				: Arrays.asList(defaultValues);

		String paramValue = this.parameters.get(parameterName);

		if (paramValue == null) {

			return defaultValuesList;

		} else {

			Splitter splitter = Splitter.on(separator);

			if (omitEmptyStrings) {
				splitter = splitter.omitEmptyStrings();
			}
			if (trimResults) {
				splitter = splitter.trimResults();
			}

			List<String> result = splitter.splitToList(paramValue);

			if (result.isEmpty()) {

				return defaultValuesList;

			} else {

				return new ArrayList<String>(result);
			}
		}
	}

	/**
	 * Gets the list of values for a parameter with given name. The parameter
	 * value is a comma-separated list of values, which is returned.
	 * 
	 * @param parameterName
	 *                          Name of the parameter to get the value list for.
	 * @return The list of values computed from the process parameter with given
	 *         name. <code>null</code> if no such parameter was declared in the
	 *         configuration.
	 */
	public String[] getListParameterValue(String parameterName) {
		String p = parameters.get(parameterName);
		if (p == null)
			return null;
		else {
			String[] values = p.split(",");
			for (int i = 0; i < values.length; i++) {
				values[i] = values[i].trim();
			}
			return values;
		}
	}

	/**
	 * @return The rule sets declared for the process. <code>null</code> if no
	 *         rule sets were declared in the configuration.
	 */
	public Map<String, ProcessRuleSet> getRuleSets() {
		return ruleSets;
	}

	/**
	 * @return a set of all rules contained in the rule sets of this process;
	 *         can be empty but not <code>null</code>
	 */
	public Set<String> getAllRules() {
		Set<String> rules = new HashSet<String>();
		if (ruleSets != null && !ruleSets.isEmpty()) {
			for (ProcessRuleSet ruleSet : ruleSets.values()) {
				if (ruleSet.getAdditionalRules() != null) {
					rules.addAll(ruleSet.getAdditionalRules());
				}
			}
		}
		return rules;
	}

	/**
	 * @return The map entries for the process. Can be empty but not
	 *         <code>null</code>.
	 */
	public List<ProcessMapEntry> getMapEntries() {
		return this.mapEntries;
	}

	public boolean hasParameter(String paramName) {
		if (paramName == null || paramName.trim().length() == 0) {
			return false;
		} else {
			return this.parameters.containsKey(paramName.trim());
		}
	}

	/**
	 * @return the 'advancedProcessConfigurations' element from the
	 *         configuration of the process; <code>null</code> if it is not set
	 *         there
	 */
	public Element getAdvancedProcessConfigurations() {
		return this.advancedProcessConfigurations;
	}

	public String getGmlVersion() {
		return this.gmlVersion;
	}

	public String toString() {

		StringBuffer sb = new StringBuffer();

		sb.append("ProcessConfiguration:\r\n");

		sb.append("\tclass name: " + this.className + "\r\n");

		sb.append("\tprocess mode " + this.processMode + "\r\n");

		sb.append("\tparameters: ");
		if (this.parameters == null || parameters.isEmpty()) {
			sb.append("none\r\n");
		} else {
			sb.append("\r\n");
			for (String key : parameters.keySet()) {
				sb.append(
						"\t\t(" + key + " | " + parameters.get(key) + ")\r\n");
			}
		}

		sb.append("\trule sets: ");
		if (this.ruleSets == null || ruleSets.isEmpty()) {
			sb.append("none\r\n");
		} else {
			sb.append("\r\n");
			for (String key : ruleSets.keySet()) {
				String ext = ruleSets.get(key).getExtendedRuleSetName();
				if (ext == null)
					ext = "<none>";
				sb.append("\t\t(name: " + ruleSets.get(key).getName()
						+ " | extends: " + ext + " | rules: ");
				Set<String> rules = ruleSets.get(key).getAdditionalRules();
				if (rules == null || rules.isEmpty()) {
					sb.append("<none>");
				} else {
					Iterator<String> rulesIterator = rules.iterator();
					sb.append(rulesIterator.next());
					while (rulesIterator.hasNext()) {
						sb.append("," + rulesIterator.next());
					}
				}
				sb.append(")\r\n");

			}
		}

		sb.append("\tmap entries: ");
		if (this.mapEntries.isEmpty()) {
			sb.append("none\r\n");
		} else {
			sb.append("\r\n");
			for (ProcessMapEntry mapEntry : mapEntries) {

				sb.append("\t\tmap entry:\r\n");
				sb.append("\t\t\ttype: '" + mapEntry.getType() + "'\r\n");
				sb.append("\t\t\trule: '" + mapEntry.getRule() + "'\r\n");

				if (mapEntry.getTargetType() == null) {
					sb.append("\t\t\ttargetType: <none>\r\n");
				} else {
					sb.append("\t\t\ttargetType: '" + mapEntry.getTargetType()
							+ "'\r\n");
				}

				if (mapEntry.getParam() == null) {
					sb.append("\t\t\tparam: <none>\r\n");
				} else {
					sb.append("\t\t\tparam: '" + mapEntry.getParam() + "'\r\n");
				}
			}
		}

		return sb.toString();

	}

	/**
	 * @return the transformers
	 */
	public List<TransformerConfiguration> getTransformers() {
		return transformers;
	}

	/**
	 * @param transformers
	 *                         the transformers to set
	 */
	public void setTransformers(List<TransformerConfiguration> transformers) {
		this.transformers = transformers;
	}

	public void addTransformer(TransformerConfiguration transformerConfig) {
		this.transformers.add(transformerConfig);

	}

	/**
	 * @return the list of direct targets associated to this process
	 */
	public List<TargetConfiguration> getTargets() {
		return targets;
	}

	/**
	 * @param targets
	 *                    the targets to set
	 */
	public void setTargets(List<TargetConfiguration> targets) {
		this.targets = targets;
	}

	public void addTarget(TargetConfiguration targetConfig) {
		this.targets.add(targetConfig);

	}

	/**
	 * @param type
	 *                 the map entry with the given type, or <code>null</code>
	 *                 if none was found.
	 * @return tbd
	 */
	public ProcessMapEntry getMapEntry(String type) {
		return this.mapEntryByType.get(type);
	}

}
