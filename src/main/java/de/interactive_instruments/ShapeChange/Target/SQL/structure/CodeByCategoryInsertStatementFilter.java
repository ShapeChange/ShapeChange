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
package de.interactive_instruments.ShapeChange.Target.SQL.structure;

import java.util.ArrayList;
import java.util.List;

import de.interactive_instruments.ShapeChange.Model.ClassInfo;

/**
 * Identifies Insert statements where the class that is represented by the table
 * to which data inserted has tagged value 'codelistType' with a value that is
 * equal to the category with which this statement filter is constructed.
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
public class CodeByCategoryInsertStatementFilter implements StatementFilter {

	protected String category;

	public CodeByCategoryInsertStatementFilter(String category) {
		this.category = category;
	}

	@Override
	public List<Statement> filter(List<Statement> statements) {

		List<Statement> result = new ArrayList<Statement>();

		for (Statement stmt : statements) {

			if (stmt instanceof Insert) {

				Insert ins = (Insert) stmt;
				ClassInfo repCi = ins.getTable().getRepresentedClass();

				if (category.equals(repCi.taggedValue("codelistType"))) {

					result.add(stmt);
				}
			}
		}

		return result;
	}

}
