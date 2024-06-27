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
 * (c) 2002-2022 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core.target.gfs;

import java.util.Comparator;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public abstract class AbstractPropertyDefinition {

    public static final Comparator<AbstractPropertyDefinition> PROP_NAME_COMPARATOR = new Comparator<>() {
	@Override
	public int compare(AbstractPropertyDefinition o1, AbstractPropertyDefinition o2) {
	    return o1.getName().compareTo(o2.getName());
	}
    };

    protected String name = "";
    protected String elementPath = "";
    protected boolean nullable = true;

    /**
     * @return the name; can be empty but not <code>null</code>
     */
    public String getName() {
	return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
	this.name = StringUtils.stripToEmpty(name);
    }

    /**
     * @return the elementPath; can be empty but not <code>null</code>
     */
    public String getElementPath() {
	return elementPath;
    }

    /**
     * @param elementPath the elementPath to set
     */
    public void setElementPath(String elementPath) {
	this.elementPath = StringUtils.stripToEmpty(elementPath);
    }

    /**
     * @return the nullable
     */
    public boolean isNullable() {
	return nullable;
    }

    /**
     * @param nullable the nullable to set
     */
    public void setNullable(boolean nullable) {
	this.nullable = nullable;
    }

    public abstract AbstractPropertyDefinition createCopy();
}
