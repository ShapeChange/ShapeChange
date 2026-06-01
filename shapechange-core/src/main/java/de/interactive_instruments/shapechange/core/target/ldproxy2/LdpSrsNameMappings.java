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
public class LdpSrsNameMappings {

    List<LdpSrsNameMapping> srsNameMappings = new ArrayList<>();

    public boolean isEmpty() {
	return this.srsNameMappings.isEmpty();
    }

    public static LdpSrsNameMappings fromXml(Element snmElmt) {

	LdpSrsNameMappings snms = new LdpSrsNameMappings();

	for (Element snm : XMLUtil.getChildElements(snmElmt, "LdproxySrsNameMapping")) {

	    int code = Integer.valueOf(snm.getAttribute("code"));
	    String forceAxisOrderString = snm.getAttribute("forceAxisOrder");
	    Force forceAxisOrder = Force.NONE;
	    if ("latlon".equalsIgnoreCase(forceAxisOrderString)) {
		forceAxisOrder = Force.LAT_LON;
	    } else if ("lonlat".equalsIgnoreCase(forceAxisOrderString)) {
		forceAxisOrder = Force.LON_LAT;
	    }
	    String value = snm.getAttribute("value");
	    OptionalInt verticalCode = snm.hasAttribute("verticalCode")
		    ? OptionalInt.of(Integer.valueOf(snm.getAttribute("verticalCode")))
		    : OptionalInt.empty();

	    snms.add(new LdpSrsNameMapping(code, forceAxisOrder, value, verticalCode));
	}

	return snms;
    }

    private void add(LdpSrsNameMapping ldpSrsNameMapping) {
	this.srsNameMappings.add(ldpSrsNameMapping);
    }

    public void merge(LdpSrsNameMappings otherLdproxySrsNameMappings) {
	this.srsNameMappings.addAll(otherLdproxySrsNameMappings.getSrsNameMappings());
    }

    public List<LdpSrsNameMapping> getSrsNameMappings() {
	return this.srsNameMappings;
    }

}
