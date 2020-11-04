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
 * (c) 2002-2017 interactive instruments GmbH, Bonn, Germany
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
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange.Target.SQL;

import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.ShapeChange.Target.SQL.structure.CreateIndex;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Index;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.PostgreSQLAlterRole;

/**
 * Creates the DDL representation for the set of visited SQL statements for a
 * PostgreSQL DB.
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class PostgreSQLDdlVisitor extends DdlVisitor {

    public PostgreSQLDdlVisitor(String crlf, String indent, SqlDdl sqlddl) {
	super(crlf, indent, sqlddl);
    }

    @Override
    public void visit(CreateIndex createIndex) {

	// Syntax:
	// https://www.postgresql.org/docs/10/static/sql-createindex.html

	Index index = createIndex.getIndex();
	Properties indexProps = index.getProperties();

	sb.append("CREATE ");

	if (index.getType() != null) {
	    sb.append(index.getType());
	    sb.append(" ");
	}

	sb.append("INDEX ");
	sb.append(index.getName());
	sb.append(" ON ");
	sb.append(createIndex.getTable().getFullName());

	if (indexProps.containsKey(PostgreSQLConstants.PROPERTY_METHOD)) {
	    sb.append(" USING ");
	    sb.append(indexProps.getProperty(PostgreSQLConstants.PROPERTY_METHOD));
	}

	if (index.hasColumns()) {
	    sb.append(" ");
	    sb.append(SqlUtil.getStringList(index.getColumns(), true, true));
	}

	if (index.hasSpecs()) {
	    sb.append(" ");
	    sb.append(StringUtils.join(index.getSpecs(), " "));
	}

	sb.append(";");
	sb.append(crlf);
    }


    @Override
    public void visit(PostgreSQLAlterRole postgreSQLAlterRole) {

	// https://www.postgresql.org/docs/10/sql-alterrole.html
	sb.append("ALTER ROLE ");

	sb.append(postgreSQLAlterRole.getRoleSpecificationOrALL());

	sb.append(" ");
	sb.append(postgreSQLAlterRole.getSpec());

	sb.append(";");
	sb.append(crlf);
    }
}
