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

import com.google.common.base.Joiner;

import de.interactive_instruments.ShapeChange.Target.SQL.PostgreSQLConstants;
import de.interactive_instruments.ShapeChange.Target.SQL.expressions.SpatiaLiteCreateSpatialIndexExpression;

/**
 * Identifies statements related to spatial indexes (creation, but also
 * insertion of geometry metadata).
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot
 *         de)
 *
 */
public class SpatialIndexStatementFilter implements StatementFilter {

	@Override
	public List<Statement> filter(List<Statement> statements) {

		List<Statement> result = new ArrayList<Statement>();

		Joiner specJoiner = Joiner.on(" ").skipNulls();

		for (Statement stmt : statements) {

			if (stmt instanceof Select) {

				Select select = (Select) stmt;

				if (select.hasExpression() && select
						.getExpression() instanceof SpatiaLiteCreateSpatialIndexExpression) {
					result.add(select);
				}

			} else if (stmt instanceof Insert) {

				Insert ins = (Insert) stmt;

				if (ins.getTable().getName()
						.equalsIgnoreCase("USER_SDO_GEOM_METADATA")) {
					result.add(stmt);
				}

			} else if (stmt instanceof CreateIndex) {

				CreateIndex cIndex = (CreateIndex) stmt;
				Index index = cIndex.getIndex();

				if ((index.hasSpecs() && specJoiner.join(index.getSpecs())
						.contains("MDSYS.SPATIAL_INDEX"))
						|| "GIST".equals(index.getProperties().getProperty(
								PostgreSQLConstants.PROPERTY_METHOD))
						|| "SPATIAL".equals(index.getType())) {

					result.add(stmt);
				}
			}
		}

		return result;
	}

}
