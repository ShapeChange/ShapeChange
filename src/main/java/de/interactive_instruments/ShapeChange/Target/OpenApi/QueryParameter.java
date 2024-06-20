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
package de.interactive_instruments.ShapeChange.Target.OpenApi;

import jakarta.json.JsonObject;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class QueryParameter {

    protected String name = null;
    protected JsonObject overlay = null;
    protected OpenApiDefinitionProcessingPhase phase = OpenApiDefinitionProcessingPhase.FINALIZATION;

    /**
     * @return the name
     */
    public String getName() {
	return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
	this.name = name;
    }

    /**
     * @return the overlay
     */
    public JsonObject getOverlay() {
	return overlay;
    }

    /**
     * @param overlay the overlay to set
     */
    public void setOverlay(JsonObject overlay) {
	this.overlay = overlay;
    }

    /**
     * @return the phase
     */
    public OpenApiDefinitionProcessingPhase getPhase() {
	return phase;
    }

    /**
     * @param phase the phase to set
     */
    public void setPhase(OpenApiDefinitionProcessingPhase phase) {
	this.phase = phase;
    }

}
