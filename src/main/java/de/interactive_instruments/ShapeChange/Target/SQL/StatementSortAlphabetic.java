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

import java.util.Comparator;

import de.interactive_instruments.ShapeChange.Target.SQL.expressions.ExpressionList;
import de.interactive_instruments.ShapeChange.Target.SQL.expressions.ToStringExpressionVisitor;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Alter;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.AlterExpression;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.ConstraintAlterExpression;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.CreateIndex;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.CreateTable;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Index;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Insert;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.SqlConstraint;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Statement;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class StatementSortAlphabetic implements Comparator<Statement> {

	@Override
	public int compare(Statement o1, Statement o2) {

		if (o1 == o2) {
			return 0;
		}

		int typePriority_o1 = typePriority(o1);
		int typePriority_o2 = typePriority(o2);

		if (typePriority_o1 < typePriority_o2) {
			return -1;
		} else if (typePriority_o1 > typePriority_o2) {
			return +1;
		}

		// same type of statement for o1 and o2

		if (o1 instanceof CreateTable) {

			String tableName_o1 = ((CreateTable) o1).getTable().getName();
			String tableName_o2 = ((CreateTable) o2).getTable().getName();

			return tableName_o1.compareTo(tableName_o2);

		} else if (o1 instanceof Alter) {

			Alter a1 = (Alter) o1;
			Alter a2 = (Alter) o2;

			String tableName_o1 = a1.getTable().getName();
			String tableName_o2 = a2.getTable().getName();

			int compareTableName = tableName_o1.compareTo(tableName_o2);

			if (compareTableName != 0) {

				return compareTableName;

			} else {

				AlterExpression ae1 = a1.getExpression();
				AlterExpression ae2 = a2.getExpression();

				// String constraintName_ae1 = ae1.getConstraintName();
				// String constraintName_ae2 = ae2.getConstraintName();
				//
				// int compareConstraintNames =
				// constraintName_ae1.compareTo(constraintName_ae2);
				// if(compareConstraintNames != 0) {
				// return compareConstraintNames;
				// }
				if (ae1 instanceof ConstraintAlterExpression
						&& ae2 instanceof ConstraintAlterExpression) {

					ConstraintAlterExpression cae1 = (ConstraintAlterExpression) ae1;
					ConstraintAlterExpression cae2 = (ConstraintAlterExpression) ae2;

					SqlConstraint scae1 = cae1.getConstraint();
					SqlConstraint scae2 = cae2.getConstraint();

					if (scae1.hasName() && scae2.hasName()) {
						int compareIndexNames = scae1.getName()
								.compareTo(scae2.getName());
						if (compareIndexNames != 0) {
							return compareIndexNames;
						}
					}
				}

				// no additional discriminating factor right now - assume
				// equality
				return 0;
			}

		} else if (o1 instanceof CreateIndex) {

			CreateIndex ci1 = (CreateIndex) o1;
			CreateIndex ci2 = (CreateIndex) o2;

			Index ind1 = ci1.getIndex();
			Index ind2 = ci2.getIndex();

			int compareIndexName = ind1.getName().compareTo(ind2.getName());

			if (compareIndexName != 0) {

				return compareIndexName;

			} else {

				String tableName_o1 = ci1.getTable().getName();
				String tableName_o2 = ci2.getTable().getName();

				return tableName_o1.compareTo(tableName_o2);
			}

		} else if (o1 instanceof Insert) {

			Insert ins1 = (Insert) o1;
			Insert ins2 = (Insert) o2;

			String tableName_o1 = ins1.getTable().getName();
			String tableName_o2 = ins2.getTable().getName();

			int compareTableName = tableName_o1.compareTo(tableName_o2);

			if (compareTableName != 0) {

				return compareTableName;

			} else {

				// Now compare the list of values
				ExpressionList il1 = ins1.getExpressionList();
				ExpressionList il2 = ins2.getExpressionList();

				ToStringExpressionVisitor ilv1 = new ToStringExpressionVisitor();
				ToStringExpressionVisitor ilv2 = new ToStringExpressionVisitor();

				il1.accept(ilv1);
				il2.accept(ilv2);

				return ilv1.getResult().compareTo(ilv2.getResult());
			}
		}

		return 0;
	}

	private int typePriority(Statement o1) {

		if (o1 instanceof CreateTable) {
			return 10;
		} else if (o1 instanceof Alter) {
			return 20;
		} else if (o1 instanceof CreateIndex) {
			return 30;
		} else if (o1 instanceof Insert) {
			return 40;
		} else {
			return 1000;
		}
	}

}
