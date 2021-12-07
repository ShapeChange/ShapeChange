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
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import de.interactive_instruments.ShapeChange.Target.Target;

/**
 * For adding and finding requirements / conversion rules as well as encoding
 * rules.
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class RuleRegistry {

    /**
     * Hash table for all schema requirements and conversion rules defined by all
     * targets.
     */
    protected HashSet<String> fAllRules = new HashSet<String>();

    /**
     * Hash table for requirements and conversion rules, with the encoding rule they
     * are associated with. Values are of the form
     * {requirementOrConversionRuleIdentifier}#{encodingRuleIdentifier}.
     */
    protected HashSet<String> fRulesInEncRule = new HashSet<String>();

    /**
     * Hash table for encoding rule extensions; key: encoding rule identifier (in
     * lower case), value: identifier of extended encoding rule (lower case;
     * <code>null</code> if no extension exists for the encoding rule whose
     * identifier is given as the key of the map entry)
     */
    protected HashMap<String, String> fExtendsEncRule = new HashMap<String, String>();

    protected TargetRegistry targetRegistry;

    /**
     * Used for consistency checks.
     * 
     * key: (encoding) rule name (lower case), value: the rule set definition
     */
    protected Map<String, ProcessRuleSet> ruleSetsByRuleName = new HashMap<>();
    protected SortedSet<String> ruleRegistrationErrors = new TreeSet<>();
    
    public RuleRegistry(TargetRegistry targetRegistry) {

	this.targetRegistry = targetRegistry;

	loadRulesAndRequirements();
    }

    /**
     * 
     */
    protected void loadRulesAndRequirements() {

	for (Class<?> tc : targetRegistry.getTargetClasses()) {

	    try {
		Target target = (Target) tc.getConstructor().newInstance();
		target.registerRulesAndRequirements(this);
	    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
		    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
		System.err.println("Exception occurred while loading target class " + tc.getName()
			+ " and attempting to call registerRulesAndRequirements(..).");
		e.printStackTrace();
	    }
	}
    }

    public void reset() {

	this.fAllRules = new HashSet<String>();
	this.fExtendsEncRule = new HashMap<String, String>();
	this.fRulesInEncRule = new HashSet<String>();

	this.ruleRegistrationErrors = new TreeSet<>();
	this.ruleSetsByRuleName = new HashMap<>();

	loadRulesAndRequirements();
    }

    public void addRuleSet(ProcessRuleSet prs) {

	String encodingRuleIdentifier = prs.getName();

	if (this.ruleSetsByRuleName.containsKey(encodingRuleIdentifier.toLowerCase())) {

	    ProcessRuleSet existingRuleSet = this.ruleSetsByRuleName.get(encodingRuleIdentifier.toLowerCase());

	    if (prs.equals(existingRuleSet)) {
		// fine - duplicate encoding rule definition, possibly via xincludes in
		// configuration
	    } else {
		String msg = "Encountered another definition of encoding rule " + encodingRuleIdentifier
			+ " (ignoring case in name comparison) which is different to the already established definition."
			+ " The additional definition will be ignored. Rename it if you really want it to apply."
			+ " The already established definition is (may be hard-coded in the ShapeChange target): "
			+ existingRuleSet.toString() + " The additional definition is: " + prs.toString();
		this.ruleRegistrationErrors.add(msg);
	    }

	} else {

	    this.ruleSetsByRuleName.put(encodingRuleIdentifier.toLowerCase(), prs);

	    String extendedEncodingRuleIdentifier = prs.getExtendedRuleSetName();
	    this.addExtendsEncRule(encodingRuleIdentifier, extendedEncodingRuleIdentifier);

	    if (prs.hasAdditionalRules()) {
		for (String requirementOrConversionRuleIdentifier : prs.getAdditionalRules()) {
		    this.addRule(requirementOrConversionRuleIdentifier, encodingRuleIdentifier);
		}
	    }
	}
    }

    /**
     * Register a requirement or rule defined by a target, for subsequent use in
     * rule set definitions.
     * 
     * @param requirementOrConversionRuleIdentifier tbd
     */
    public void addRule(String requirementOrConversionRuleIdentifier) {
	fAllRules.add(requirementOrConversionRuleIdentifier.toLowerCase());
    }

    /**
     * @param requirementOrConversionRuleIdentifier tbd
     * @return <code>true</code> if the requirement or conversion rule is known to
     *         ShapeChange (i.e., defined by at least one of its targets), else
     *         <code>false</code>
     */
    public boolean hasRule(String requirementOrConversionRuleIdentifier) {
	return fAllRules.contains(requirementOrConversionRuleIdentifier.toLowerCase());
    }

    private void addRule(String requirementOrConversionRuleIdentifier, String encodingRuleIdentifier) {
	fRulesInEncRule
		.add(requirementOrConversionRuleIdentifier.toLowerCase() + "#" + encodingRuleIdentifier.toLowerCase());
    }

    /**
     * @param requirementOrConversionRuleIdentifier tbd
     * @param encodingRuleIdentifier                tbd
     * @return <code>true</code> if the requirement or conversion rule is contained
     *         in the given encoding rule (thereby also resolving directly or
     *         indirectly extended encoding rules).
     */
    public boolean hasRule(String requirementOrConversionRuleIdentifier, String encodingRuleIdentifier) {
	boolean res = false;
	while (!res && encodingRuleIdentifier != null) {
	    res = fRulesInEncRule.contains(
		    requirementOrConversionRuleIdentifier.toLowerCase() + "#" + encodingRuleIdentifier.toLowerCase());
	    encodingRuleIdentifier = extendsEncRule(encodingRuleIdentifier);
	}
	return res;
    }

    /**
     * Identify if the given encoding rule is or extends (directly or indirectly)
     * the given base encoding rule. When comparing encoding rule names, case is
     * ignored.
     * 
     * @param encodingRuleIdentifier     tbd
     * @param baseEncodingRuleIdentifier tbd
     * @return <code>true</code> if encodingRuleIdentifier is or extends (directly
     *         or indirectly) baseEncodingRuleIdentifier, else <code>false</code>
     */
    public boolean matchesEncRule(String encodingRuleIdentifier, String baseEncodingRuleIdentifier) {
	while (encodingRuleIdentifier != null) {
	    if (encodingRuleIdentifier.equalsIgnoreCase(baseEncodingRuleIdentifier))
		return true;
	    encodingRuleIdentifier = extendsEncRule(encodingRuleIdentifier);
	}
	return false;
    }

    /**
     * @param encodingRuleIdentifier         tbd
     * @param extendedEncodingRuleIdentifier may be <code>null</code> if the
     *                                       encoding rule does not extend another
     *                                       encoding rule
     */
    private void addExtendsEncRule(String encodingRuleIdentifier, String extendedEncodingRuleIdentifier) {
	fExtendsEncRule.put(encodingRuleIdentifier.toLowerCase(),
		extendedEncodingRuleIdentifier == null ? null : extendedEncodingRuleIdentifier.toLowerCase());
    }

    /**
     * @param encodingRuleIdentifier tbd
     * @return the identifier of the encoding rule that is extended by the given
     *         encoding rule; can be <code>null</code> no such extension exists
     */
    public String extendsEncRule(String encodingRuleIdentifier) {
	return fExtendsEncRule.get(encodingRuleIdentifier.toLowerCase());
    }

    /**
     * @param encodingRuleIdentifier tbd
     * @return <code>true</code> if the given identifier belongs to one of the
     *         encoding rules (made) known to ShapeChange (by targets and via the
     *         configuration) or if the identifier value is '*', else
     *         <code>false</code>
     */
    public boolean encRuleExists(String encodingRuleIdentifier) {
	if ("*".equals(encodingRuleIdentifier)) {
	    return true;
	} else {
	    return fExtendsEncRule.containsKey(encodingRuleIdentifier.toLowerCase());
	}
    }

    /**
     * @return the set of errors encountered while registering rules; can be empty
     *         but not <code>null</code>
     */
    public SortedSet<String> getRuleRegistrationErrors() {
	return this.ruleRegistrationErrors;
    }

}
