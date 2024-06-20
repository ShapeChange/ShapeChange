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
 * (c) 2002-2016 interactive instruments GmbH, Bonn, Germany
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

import java.util.SortedSet;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class StereotypeConversionParameter {

    private String wellknown;
    private SortedSet<String> subClassOf;
    private String rule;

    public StereotypeConversionParameter(String wellknown, SortedSet<String> subClassOf, String rule) {
	super();
	this.wellknown = wellknown;
	this.subClassOf = subClassOf;
	this.rule = rule;
    }

    /**
     * @return Stereotype that is well-known to ShapeChange
     */
    public String getWellknown() {
	return wellknown;
    }

    /**
     * @return IRIs of classes of which UML types with this stereotype shall be
     *         subClassOf. May be empty but not <code>null</code>. Note: the values
     *         are given as (trimmed) QName-like strings, with the namespace
     *         prefixes matching the namespace abbreviations of the namespaces
     *         declared in the configuration.
     */
    public SortedSet<String> getSubClassOf() {
	return subClassOf;
    }

    /**
     * @return The encoding rule to which this parameter applies. May be “*” to
     *         indicate that the parameter applies to all encoding rules.
     */
    public String getRule() {
	return rule;
    }

}
