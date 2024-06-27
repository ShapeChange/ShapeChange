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
package de.interactive_instruments.shapechange.core;

import java.util.Comparator;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 */
public class XmlNamespace {

    public static final Comparator<XmlNamespace> comparator = Comparator
	    .comparing(XmlNamespace::getNsabr, Comparator.nullsFirst(Comparator.naturalOrder()))
	    .thenComparing(XmlNamespace::getNs, Comparator.nullsFirst(Comparator.naturalOrder()))
	    .thenComparing(XmlNamespace::getLocation, Comparator.nullsFirst(Comparator.naturalOrder()))
	    .thenComparing(XmlNamespace::getPackageName, Comparator.nullsFirst(Comparator.naturalOrder()));

    private String ns;
    private String location;
    private String packageName;

    private String nsabr;

    public XmlNamespace(String nsabr, String ns, String location, String packageName) {
	super();
	this.nsabr = nsabr;
	this.ns = ns;
	this.location = location;
	this.packageName = packageName;
    }

    public String getNsabr() {
	return nsabr;
    }

    public String getNs() {
	return ns;
    }

    public String getLocation() {
	return location;
    }
    
    public boolean hasLocation() {
	return StringUtils.isNotBlank(location);
    }

    public String getPackageName() {
	return packageName;
    }
    
    public boolean hasPackageName() {
	return StringUtils.isNotBlank(packageName);
    }

}
