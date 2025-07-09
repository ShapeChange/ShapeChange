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
 * (c) 2002-2025 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core.target.ldproxy2.storedquery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;

import de.interactive_instruments.shapechange.core.MessageSource;
import de.interactive_instruments.shapechange.core.util.XMLUtil;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class LdproxyStoredQueryDefinitions implements MessageSource {

    protected SortedMap<String, LdproxyStoredQuery> storedQueriesById = new TreeMap<>();

    public SortedMap<String, LdproxyStoredQuery> getStoredQueryDefinitions() {
	return this.storedQueriesById;
    }

    public boolean isEmpty() {
	return this.storedQueriesById.isEmpty();
    }

    public void add(LdproxyStoredQuery ldpsq) {
	this.storedQueriesById.put(ldpsq.getId(), ldpsq);
    }

    public void addLdproxyStoredQueries(Collection<LdproxyStoredQuery> ldpsqd) {
	for (LdproxyStoredQuery lpdsq : ldpsqd) {
	    this.add(lpdsq);
	}
    }

    public static List<LdproxyStoredQuery> fromXml(Element ldproxyStoredQueryDefinitionsElmt) {

	List<LdproxyStoredQuery> lsqs = new ArrayList<>();

	for (Element lsqElmt : XMLUtil.getChildElements(ldproxyStoredQueryDefinitionsElmt, "LdproxyStoredQuery")) {

	    LdproxyStoredQuery sq = new LdproxyStoredQuery();

	    List<PropertyEqualToCollectionQuery> petQueries = new ArrayList<>();
	    for (Element petQueryElmt : XMLUtil.getChildElements(lsqElmt, "PropertyEqualToCollectionQuery")) {
		PropertyEqualToCollectionQuery petQuery = new PropertyEqualToCollectionQuery();
		petQuery.setProperty(XMLUtil.getTrimmedTextContentOfFirstElement(petQueryElmt, "property"));
		petQuery.setParameter(XMLUtil.getTrimmedTextContentOfFirstElement(petQueryElmt, "parameter"));
		petQueries.add(petQuery);
	    }
	    sq.setQueryDefinitions(petQueries);

	    sq.setId(lsqElmt.getAttribute("id"));

	    sq.setTitle(XMLUtil.getTrimmedTextContentOfFirstElement(lsqElmt, "title"));
	    sq.setDescription(XMLUtil.getTrimmedTextContentOfFirstElement(lsqElmt, "description"));
	    String limitString = XMLUtil.getTrimmedTextContentOfFirstElement(lsqElmt, "limit");
	    if (StringUtils.isNotBlank(limitString)) {
		sq.setLimit(Integer.valueOf(limitString));
	    }
	    sq.setCrs(XMLUtil.getTrimmedTextContentOfFirstElement(lsqElmt, "crs"));

	    lsqs.add(sq);
	}

	return lsqs;
    }

    @Override
    public String message(int mnr) {

	switch (mnr) {

	case 1:
	    return "";
	case 2:
	    return "";

	default:
	    return "(LdproxyStoredQueryDefinitions.java) Unknown message with number: " + mnr;
	}

    }
}
