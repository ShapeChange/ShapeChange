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
 * (c) 2002-2026 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core.target.ldproxy2;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import org.w3c.dom.Element;

import de.ii.xtraplatform.crs.domain.EpsgCrs.Force;
import de.interactive_instruments.shapechange.core.util.XMLUtil;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class LdpUomMappings {

    List<LdpUomMapping> uomMappings = new ArrayList<>();

    public boolean isEmpty() {
	return this.uomMappings.isEmpty();
    }

    public static LdpUomMappings fromXml(Element uommElmt) {

	LdpUomMappings uomms = new LdpUomMappings();

	for (Element uomm : XMLUtil.getChildElements(uommElmt, "LdproxyUomMapping")) {

	    String uom = uomm.getAttribute("uom");
	    String value = uomm.getAttribute("value");

	    uomms.add(new LdpUomMapping(uom, value));
	}

	return uomms;
    }

    private void add(LdpUomMapping ldpUomMapping) {
	this.uomMappings.add(ldpUomMapping);
    }

    public void merge(LdpUomMappings otherLdproxyUomMappings) {
	this.uomMappings.addAll(otherLdproxyUomMappings.getUomMappings());
    }

    public List<LdpUomMapping> getUomMappings() {
	return this.uomMappings;
    }

}
