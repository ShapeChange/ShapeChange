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
package de.interactive_instruments.shapechange.core.target.sql.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class DropSchema implements Statement {

    private boolean ifExists = false;
    private SortedSet<String> schemaNames = null;
    private List<String> specs = new ArrayList<String>();

    public DropSchema(SortedSet<String> schemaNames) {
	this.schemaNames = schemaNames;
    }
    
    /**
     * @return the schemaNames
     */
    public SortedSet<String> getSchemaNames() {
	return schemaNames;
    }

    /**
     * @param schemaNames the schemaNames to set
     */
    public void setSchemaNames(SortedSet<String> schemaNames) {
	this.schemaNames = schemaNames;
    }

    /**
     * @return the specs
     */
    public List<String> getSpecs() {
	return specs;
    }

    /**
     * @param specs the specs to set
     */
    public void setSpecs(List<String> specs) {
	this.specs = specs;
    }

    public void addSpec(String spec) {
	this.specs.add(spec);
    }
    
    public boolean hasSpecs() {
	return !this.specs.isEmpty();
    }

    @Override
    public void accept(StatementVisitor visitor) {
	visitor.visit(this);
    }


    /**
     * @return the ifExists
     */
    public boolean isIfExists() {
        return ifExists;
    }


    /**
     * @param ifExists the ifExists to set
     */
    public void setIfExists(boolean ifExists) {
        this.ifExists = ifExists;
    }

}
