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

import de.interactive_instruments.ShapeChange.Target.Target;

/**
 * For adding and finding conversion as well as encoding rules.
 * 
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments <dot>
 *         de)
 *
 */
public class RuleRegistry {

    /** Hash table for schema requirements and conversion rules */
    protected HashSet<String> fAllRules = new HashSet<String>();
    protected HashSet<String> fRulesInEncRule = new HashSet<String>();

    /** Hash table for encoding rule extensions */
    protected HashMap<String, String> fExtendsEncRule = new HashMap<String, String>();
    protected TargetRegistry targetRegistry;

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

	loadRulesAndRequirements();
    }

    public void addRule(String rule) {
	fAllRules.add(rule.toLowerCase());
    }

    public boolean hasRule(String rule) {
	return fAllRules.contains(rule.toLowerCase());
    }

    public void addRule(String rule, String encRule) {
	fRulesInEncRule.add(rule.toLowerCase() + "#" + encRule.toLowerCase());
    }

    public boolean hasRule(String rule, String encRule) {
	boolean res = false;
	while (!res && encRule != null) {
	    res = fRulesInEncRule.contains(rule.toLowerCase() + "#" + encRule.toLowerCase());
	    encRule = extendsEncRule(encRule);
	}
	return res;
    }

    /**
     * Identify if the given encRule is or extends (directly or indirectly) the
     * given baseRule. When comparing encoding rule names, case is ignored.
     * 
     * @param encRule
     * @param baseRule
     * @return <code>true</code> if encRule is or extends (directly or indirectly)
     *         baseRule, else <code>false</code>
     */
    public boolean matchesEncRule(String encRule, String baseRule) {
	while (encRule != null) {
	    if (encRule.equalsIgnoreCase(baseRule))
		return true;
	    encRule = extendsEncRule(encRule);
	}
	return false;
    }

    public void addExtendsEncRule(String rule1, String rule2) {
	fExtendsEncRule.put(rule1.toLowerCase(), rule2.toLowerCase());
    }

    public String extendsEncRule(String rule1) {
	return fExtendsEncRule.get(rule1.toLowerCase());
    }

    public boolean encRuleExists(String encRule) {
	if ("*".equals(encRule)) {
	    return true;
	} else {
	    return fExtendsEncRule.containsKey(encRule);
	}
    }

}
