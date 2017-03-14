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
package de.interactive_instruments.ShapeChange.Target.SQL.naming;

import java.util.List;

import de.interactive_instruments.ShapeChange.Target.SQL.structure.Alter;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.AlterExpression;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Column;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.ConstraintAlterExpression;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.CreateIndex;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.CreateTable;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Insert;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Statement;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public abstract class AbstractNameNormalizer implements NameNormalizer {

	private boolean isIgnoreCaseWhenNormalizing = false;

	/**
	 * Instruct the name normalizer whether to change the case of a name while
	 * normalizing it.
	 * 
	 * @param isIgnoreCaseWhenNormalizing
	 */
	public void setIgnoreCaseWhenNormalizing(
			boolean isIgnoreCaseWhenNormalizing) {
		this.isIgnoreCaseWhenNormalizing = isIgnoreCaseWhenNormalizing;
	}

	/**
	 * @return <code>true</code> if case should be changed while normalizing a
	 *         name, else <code>false</code>
	 */
	public boolean isIgnoreCaseWhenNormalizing() {
		return isIgnoreCaseWhenNormalizing;
	}

	@Override
	public void visit(Insert insert) {

		// nothing specific to normalize here
	}

	@Override
	public void visit(CreateIndex createIndex) {

		// normalize index name
		createIndex.getIndex()
				.setName(normalize(createIndex.getIndex().getName()));
	}

	@Override
	public void visit(CreateTable createTable) {

		// normalize table name
		createTable.getTable()
				.setName(normalize(createTable.getTable().getName()));

		// normalize column names
		for (Column column : createTable.getTable().getColumns()) {
			column.setName(normalize(column.getName()));
		}
	}

	@Override
	public void visit(Alter alter) {

		// normalize constraint names
		AlterExpression expr = alter.getExpression();

		if (expr instanceof ConstraintAlterExpression) {

			ConstraintAlterExpression cae = (ConstraintAlterExpression) expr;

			if (cae.getConstraint().hasName()) {
				cae.getConstraint()
						.setName(normalize(cae.getConstraint().getName()));
			}
		}
	}

	@Override
	public void visit(List<Statement> stmts) {

		for (Statement stmt : stmts) {
			stmt.accept(this);
		}
	}

	public String normalize(String stringToNormalize) {
		return stringToNormalize.replace(".", "_").replace("-", "_");
	}
}
