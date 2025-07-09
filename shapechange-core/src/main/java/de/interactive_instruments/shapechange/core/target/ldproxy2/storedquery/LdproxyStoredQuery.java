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

import java.util.List;
import java.util.Optional;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class LdproxyStoredQuery {

    protected String id;
    protected String title;
    protected String description;
    protected Integer limit;
    protected String crs;

    protected List<PropertyEqualToCollectionQuery> queryDefinitions;

    /**
     * @return the id
     */
    public String getId() {
	return id;
    }

    /**
     * @return the title
     */
    public Optional<String> getTitle() {
	return Optional.ofNullable(title);
    }

    /**
     * @return the description
     */
    public Optional<String> getDescription() {
	return Optional.ofNullable(description);
    }

    /**
     * @return the limit
     */
    public Optional<Integer> getLimit() {
	return Optional.ofNullable(limit);
    }

    /**
     * @return the crs
     */
    public Optional<String> getCrs() {
	return Optional.ofNullable(crs);
    }

    /**
     * @return the queryDefinitions
     */
    public List<PropertyEqualToCollectionQuery> getQueryDefinitions() {
	return queryDefinitions;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
	this.id = id;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
	this.title = title;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
	this.description = description;
    }

    /**
     * @param limit the limit to set
     */
    public void setLimit(int limit) {
	this.limit = Integer.valueOf(limit);
    }

    /**
     * @param crs the crs to set
     */
    public void setCrs(String crs) {
	this.crs = crs;
    }

    /**
     * @param queryDefinitions the queryDefinitions to set
     */
    public void setQueryDefinitions(List<PropertyEqualToCollectionQuery> queryDefinitions) {
	this.queryDefinitions = queryDefinitions;
    }

}
