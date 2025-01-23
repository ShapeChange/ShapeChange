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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.shapechange.core.target.sql.naming;

import java.util.List;

import de.interactive_instruments.shapechange.core.target.sql.structure.Alter;
import de.interactive_instruments.shapechange.core.target.sql.structure.AlterExpression;
import de.interactive_instruments.shapechange.core.target.sql.structure.Column;
import de.interactive_instruments.shapechange.core.target.sql.structure.Comment;
import de.interactive_instruments.shapechange.core.target.sql.structure.ConstraintAlterExpression;
import de.interactive_instruments.shapechange.core.target.sql.structure.CreateIndex;
import de.interactive_instruments.shapechange.core.target.sql.structure.CreateSchema;
import de.interactive_instruments.shapechange.core.target.sql.structure.CreateTable;
import de.interactive_instruments.shapechange.core.target.sql.structure.DropSchema;
import de.interactive_instruments.shapechange.core.target.sql.structure.Insert;
import de.interactive_instruments.shapechange.core.target.sql.structure.PostgreSQLAlterRole;
import de.interactive_instruments.shapechange.core.target.sql.structure.SQLitePragma;
import de.interactive_instruments.shapechange.core.target.sql.structure.Select;
import de.interactive_instruments.shapechange.core.target.sql.structure.Statement;
import de.interactive_instruments.shapechange.core.target.sql.structure.Table;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public abstract class AbstractNameNormalizer implements NameNormalizer {

    private boolean isIgnoreCaseWhenNormalizing = false;

    /**
     * Instruct the name normalizer whether to change the case of a name while
     * normalizing it.
     * 
     * @param isIgnoreCaseWhenNormalizing tbd
     */
    public void setIgnoreCaseWhenNormalizing(boolean isIgnoreCaseWhenNormalizing) {
	this.isIgnoreCaseWhenNormalizing = isIgnoreCaseWhenNormalizing;
    }

    /**
     * @return <code>true</code> if case should be changed while normalizing a name,
     *         else <code>false</code>
     */
    public boolean isIgnoreCaseWhenNormalizing() {
	return isIgnoreCaseWhenNormalizing;
    }

    @Override
    public void visit(Insert insert) {

	// nothing specific to normalize here
    }

    @Override
    public void visit(Select select) {

	// nothing specific to normalize here
    }

    @Override
    public void visit(CreateIndex createIndex) {

	// normalize index name
	createIndex.getIndex().setName(normalize(createIndex.getIndex().getName()));
    }

    @Override
    public void visit(CreateTable createTable) {

	// normalize table name
	Table table = createTable.getTable();
	table.setName(normalize(table.getName()));

	// normalize schema name, if set
	if (table.hasSchemaName()) {
	    table.setSchemaName(normalize(table.getSchemaName()));
	}

	// normalize column names
	for (Column column : createTable.getTable().getColumns()) {
	    column.setName(normalize(column.getName()));
	}
    }

    @Override
    public void visit(Alter alter) {

	// normalize constraint names
	AlterExpression expr = alter.getExpression();

	if (expr instanceof ConstraintAlterExpression cae) {

	    if (cae.getConstraint().hasName()) {
		cae.getConstraint().setName(normalize(cae.getConstraint().getName()));
	    }
	}
    }

    @Override
    public void visit(List<Statement> stmts) {

	for (Statement stmt : stmts) {
	    stmt.accept(this);
	}
    }

    @Override
    public void visit(Comment comment) {
	// ignore
    }

    @Override
    public void postprocess() {
	// ignore
    }

    @Override
    public void visit(SQLitePragma pragma) {
	// ignore
    }

    @Override
    public void visit(PostgreSQLAlterRole postgreSQLAlterRole) {
	// ignore
    }

    @Override
    public void visit(DropSchema dropSchema) {
	// ignore
    }

    @Override
    public void visit(CreateSchema createSchema) {
	createSchema.setSchemaName(normalize(createSchema.getSchemaName()));
    }

    public String normalize(String stringToNormalize) {
	return stringToNormalize.replace(".", "_").replace("-", "_");
    }
}
