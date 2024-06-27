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
 * (c) 2002-2020 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core.util;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class ValueTypeOptions {
    
    protected SortedMap<String, SortedSet<String>> valueTypeOptionsByPropName = new TreeMap<>();
    protected SortedSet<String> associationClassRoles = new TreeSet<>();
    
    public ValueTypeOptions() {}
    
    public ValueTypeOptions(String vto_tag_value) {
		
	if (StringUtils.isNotBlank(vto_tag_value)) {
	    
	    String[] propValueTypeOptions = StringUtils.split(vto_tag_value, ";");
	    
	    for (String propValueTypeOption : propValueTypeOptions) {
		
		String[] optionFacets = StringUtils.split(propValueTypeOption, "=");
		
		String propertyNamePart = optionFacets[0].trim();
		String propertyName = null;
		
		if(propertyNamePart.contains("(")) {
		    int bracketStartIndex = propertyNamePart.indexOf("(");
		    propertyName = propertyNamePart.substring(0, bracketStartIndex);
		    String qualifier = propertyNamePart.substring(bracketStartIndex+1, propertyNamePart.length()-1);
		    if("associationClassRole".equalsIgnoreCase(qualifier)) {
			associationClassRoles.add(propertyName);
		    }
		} else {
		    propertyName = propertyNamePart;
		}
		
		SortedSet<String> valueTypes = Arrays.stream(optionFacets[1].split(",")).map(s -> s.trim())
			.collect(Collectors.toCollection(TreeSet::new));
		
		valueTypeOptionsByPropName.put(propertyName, valueTypes);
	    }
	}	
    }
    
    public void setAssociationClassRole(String propertyName) {
	associationClassRoles.add(propertyName);
    }
    
    public boolean isAssociationClassRole(String propertyName) {
	return associationClassRoles.contains(propertyName);
    }
    
    public boolean hasValueTypeOptions(String propertyName) {
	return valueTypeOptionsByPropName.containsKey(propertyName);
    }

    public SortedSet<String> getValueTypeOptions(String propertyName) {
	return valueTypeOptionsByPropName.get(propertyName);
    }
    
    public Set<String> getPropertiesWithValueTypeOptions() {
	return valueTypeOptionsByPropName.keySet();
    }
    
    public boolean isEmpty() {
	return this.valueTypeOptionsByPropName.isEmpty();
    }
    
    public String toString() {
	
	StringBuffer sb = new StringBuffer();
	
	for(Entry<String,SortedSet<String>> e : valueTypeOptionsByPropName.entrySet()) {
	    
	    String propertyName = e.getKey();
	    SortedSet<String> valueTypeOptions = e.getValue();
	    boolean isAssociationClassRole = associationClassRoles.contains(propertyName);
	    
	    sb.append(propertyName);
	    if(isAssociationClassRole) {
		sb.append("(").append("associationClassRole").append(")");
	    }
	    sb.append("=");
	    sb.append(String.join(",", valueTypeOptions));
	    sb.append(";");
	}
	
	sb.deleteCharAt(sb.length()-1);
	
	return sb.toString();
    }
    
    public void add(String propertyName, boolean isAssociationClassRole, SortedSet<String> valueTypeOptions) {
	this.valueTypeOptionsByPropName.put(propertyName,valueTypeOptions);
	if(isAssociationClassRole) {
	    this.associationClassRoles.add(propertyName);
	}	
    }

    public void remove(String propertyName) {
	this.associationClassRoles.remove(propertyName);
	this.valueTypeOptionsByPropName.remove(propertyName);	
    }
}
