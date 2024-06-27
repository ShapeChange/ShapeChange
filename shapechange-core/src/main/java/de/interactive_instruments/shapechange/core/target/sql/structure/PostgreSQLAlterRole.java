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

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class PostgreSQLAlterRole implements Statement {

    private String roleSpecificationOrALL = null;
    private String spec = null;

    /**
     * @return the roleSpecificationOrALL
     */
    public String getRoleSpecificationOrALL() {
	return roleSpecificationOrALL;
    }

    /**
     * @param roleSpecificationOrALL the roleSpecificationOrALL to set
     */
    public void setRoleSpecificationOrALL(String roleSpecificationOrALL) {
	this.roleSpecificationOrALL = roleSpecificationOrALL;
    }

    /**
     * @return the spec
     */
    public String getSpec() {
	return spec;
    }

    /**
     * @param spec the spec to set
     */
    public void setSpec(String spec) {
	this.spec = spec;
    }

    @Override
    public void accept(StatementVisitor visitor) {
	visitor.visit(this);
    }
}