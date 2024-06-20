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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange;

import java.util.Objects;
import java.util.SortedSet;

import org.apache.commons.lang3.StringUtils;

/**
 * Represents a set of rules. This set may be accompanied by a set of
 * identifiers for optional rules. It may also extend another set of rules.
 * 
 * @author echterhoff
 * 
 */
public class ProcessRuleSet {

    /**
     * Name of this rule set.
     */
    private String name;
    /**
     * Name of the rule set that this rule set extends. Optional
     */
    private String extendedRuleSetName = null;
    /**
     * Set of identifiers for the additional, otherwise optional rules, that are
     * declared for this rule set.
     */
    private SortedSet<String> additionalRules = null;

    /**
     * @param name            Name of this rule set.
     * @param additionalRules Set of identifiers for the additional, otherwise
     *                        optional rules, that are declared for this rule set.
     */
    public ProcessRuleSet(String name, SortedSet<String> additionalRules) {
	this.name = name;
	this.additionalRules = additionalRules;
    }

    /**
     * @param name                Name of this rule set.
     * @param extendedRuleSetName Name of the rule set that this rule set extends.
     *                            May be null if there is no such extension.
     */
    public ProcessRuleSet(String name, String extendedRuleSetName) {
	this.name = name;
	this.extendedRuleSetName = extendedRuleSetName;
    }

    /**
     * @param name                Name of this rule set.
     * @param extendedRuleSetName Name of the rule set that this rule set extends.
     *                            May be null if there is no such extension.
     * @param additionalRules     Set of identifiers for the additional, otherwise
     *                            optional rules, that are declared for this rule
     *                            set.
     */
    public ProcessRuleSet(String name, String extendedRuleSetName, SortedSet<String> additionalRules) {
	this.name = name;
	this.extendedRuleSetName = extendedRuleSetName;
	this.additionalRules = additionalRules;
    }

    /**
     * @return Name of this rule set.
     */
    public String getName() {
	return name;
    }

    /**
     * @return Name of the rule set that is extended by this set of rules; null if
     *         there is no such extension.
     */
    public String getExtendedRuleSetName() {
	return extendedRuleSetName;
    }

    /**
     * @return Set of identifiers for the additional, otherwise optional rules, that
     *         are declared for this rule set.
     */
    public SortedSet<String> getAdditionalRules() {
	return additionalRules;
    }

    public boolean hasAdditionalRules() {
	return additionalRules != null && !additionalRules.isEmpty();
    }

    @Override
    public int hashCode() {
	return Objects.hash(additionalRules, extendedRuleSetName, name);
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	ProcessRuleSet other = (ProcessRuleSet) obj;
	return Objects.equals(additionalRules, other.additionalRules)
		&& Objects.equals(extendedRuleSetName, other.extendedRuleSetName) && Objects.equals(name, other.name);
    }

    @Override
    public String toString() {
	return "ProcessRuleSet [name=" + name + ", extends="
		+ (extendedRuleSetName == null ? "<none>" : extendedRuleSetName) + ", rules="
		+ (additionalRules == null ? "<none>" : StringUtils.join(additionalRules, ", ")) + "]";
    }

}
