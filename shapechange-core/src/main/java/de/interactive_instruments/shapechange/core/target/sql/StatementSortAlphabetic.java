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
package de.interactive_instruments.shapechange.core.target.sql;

import java.util.Comparator;

import de.interactive_instruments.shapechange.core.target.sql.expressions.Expression;
import de.interactive_instruments.shapechange.core.target.sql.expressions.ExpressionList;
import de.interactive_instruments.shapechange.core.target.sql.expressions.SpatiaLiteAddGeometryColumn;
import de.interactive_instruments.shapechange.core.target.sql.expressions.SpatiaLiteCreateSpatialIndexExpression;
import de.interactive_instruments.shapechange.core.target.sql.expressions.ToStringExpressionVisitor;
import de.interactive_instruments.shapechange.core.target.sql.structure.Alter;
import de.interactive_instruments.shapechange.core.target.sql.structure.AlterExpression;
import de.interactive_instruments.shapechange.core.target.sql.structure.Comment;
import de.interactive_instruments.shapechange.core.target.sql.structure.ConstraintAlterExpression;
import de.interactive_instruments.shapechange.core.target.sql.structure.CreateIndex;
import de.interactive_instruments.shapechange.core.target.sql.structure.CreateTable;
import de.interactive_instruments.shapechange.core.target.sql.structure.Index;
import de.interactive_instruments.shapechange.core.target.sql.structure.Insert;
import de.interactive_instruments.shapechange.core.target.sql.structure.SQLitePragma;
import de.interactive_instruments.shapechange.core.target.sql.structure.Select;
import de.interactive_instruments.shapechange.core.target.sql.structure.SqlConstraint;
import de.interactive_instruments.shapechange.core.target.sql.structure.Statement;
import de.interactive_instruments.shapechange.core.target.sql.structure.Table;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
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

	if (o1 instanceof SQLitePragma pragma) {

	    String p1Name = pragma.getName();
	    String p2Name = ((SQLitePragma) o2).getName();

	    return p1Name.compareTo(p2Name);

	} else if (o1 instanceof CreateTable table) {

	    if (SqlConstants.STATEMENT_SORT_USES_SCHEMA_NAME) {
		String tableName_o1 = table.getTable().getFullName();
		String tableName_o2 = ((CreateTable) o2).getTable().getFullName();
		return tableName_o1.compareTo(tableName_o2);
	    } else {
		String tableName_o1 = table.getTable().getName();
		String tableName_o2 = ((CreateTable) o2).getTable().getName();
		return tableName_o1.compareTo(tableName_o2);
	    }

	} else if (o1 instanceof Select s1) {
	    Select s2 = (Select) o2;

	    if (s1.hasExpression() && s2.hasExpression()) {

		Expression e1 = s1.getExpression();
		Expression e2 = s2.getExpression();

		if (e1 instanceof SpatiaLiteAddGeometryColumn && e2 instanceof SpatiaLiteCreateSpatialIndexExpression) {
		    return -1;
		} else if (e1 instanceof SpatiaLiteCreateSpatialIndexExpression
			&& e2 instanceof SpatiaLiteAddGeometryColumn) {
		    return 1;
		} else if (e1 instanceof SpatiaLiteCreateSpatialIndexExpression csie1
			&& e2 instanceof SpatiaLiteCreateSpatialIndexExpression csie2) {
		    int tableNameComp = csie1.getTable().getFullName().compareTo(csie2.getTable().getFullName());
		    if (tableNameComp == 0) {
			return csie1.getColumn().getName().compareTo(csie2.getColumn().getName());
		    } else {
			return tableNameComp;
		    }
		} else if (e1 instanceof SpatiaLiteAddGeometryColumn agc1 && e2 instanceof SpatiaLiteAddGeometryColumn agc2) {
		    int tableNameComp = agc1.getTable().getFullName().compareTo(agc2.getTable().getFullName());
		    if (tableNameComp == 0) {
			return agc1.getColumn().getName().compareTo(agc2.getColumn().getName());
		    } else {
			return tableNameComp;
		    }
		} else {
		    // add more expression comparisons
		    return 0;
		}

	    } else {
		return 0;
	    }

	} else if (o1 instanceof Alter a1) {
	    Alter a2 = (Alter) o2;

	    String tableName_o1;
	    String tableName_o2;

	    if (SqlConstants.STATEMENT_SORT_USES_SCHEMA_NAME) {
		tableName_o1 = a1.getTable().getFullName();
		tableName_o2 = ((Alter) o2).getTable().getFullName();
	    } else {
		tableName_o1 = a1.getTable().getName();
		tableName_o2 = ((Alter) o2).getTable().getName();
	    }

	    int compareTableName = tableName_o1.compareTo(tableName_o2);

	    if (compareTableName != 0) {

		return compareTableName;

	    } else {

		AlterExpression ae1 = a1.getExpression();
		AlterExpression ae2 = a2.getExpression();

		if (ae1 instanceof ConstraintAlterExpression cae1 && ae2 instanceof ConstraintAlterExpression cae2) {

		    SqlConstraint scae1 = cae1.getConstraint();
		    SqlConstraint scae2 = cae2.getConstraint();

		    if (scae1.hasName() && scae2.hasName()) {
			int compareIndexNames = scae1.getName().compareTo(scae2.getName());
			if (compareIndexNames != 0) {
			    return compareIndexNames;
			}
		    }
		}

		// no additional discriminating factor right now - assume
		// equality
		return 0;
	    }

	} else if (o1 instanceof CreateIndex ci1) {
	    CreateIndex ci2 = (CreateIndex) o2;

	    Index ind1 = ci1.getIndex();
	    Index ind2 = ci2.getIndex();

	    int compareIndexName = ind1.getName().compareTo(ind2.getName());

	    if (compareIndexName != 0) {

		return compareIndexName;

	    } else {

		String tableName_o1;
		String tableName_o2;
		if (SqlConstants.STATEMENT_SORT_USES_SCHEMA_NAME) {
		    tableName_o1 = ci1.getTable().getFullName();
		    tableName_o2 = ci2.getTable().getFullName();
		} else {
		    tableName_o1 = ci1.getTable().getName();
		    tableName_o2 = ci2.getTable().getName();
		}
		return tableName_o1.compareTo(tableName_o2);
	    }

	} else if (o1 instanceof Insert ins1) {
	    Insert ins2 = (Insert) o2;

	    Table tins1 = ins1.getTable();
	    Table tins2 = ins2.getTable();

	    /*
	     * Ensure that inserts for the CodeStatusCL type come first, then inserts for
	     * other types.
	     */
	    if (tins1.representsCodeStatusCLType()) {
		return -1;
	    } else if (tins2.representsCodeStatusCLType()) {
		return 1;
	    }

	    String tableName_o1;
	    String tableName_o2;
	    if (SqlConstants.STATEMENT_SORT_USES_SCHEMA_NAME) {
		tableName_o1 = tins1.getFullName();
		tableName_o2 = tins2.getFullName();
	    } else {
		tableName_o1 = tins1.getFullName();
		tableName_o2 = tins2.getFullName();
	    }

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

	} else if (o1 instanceof Comment comment1) {
	    Comment comment2 = (Comment) o2;

	    return comment1.computeTargetName().compareTo(comment2.computeTargetName());
	}

	return 0;
    }

    private int typePriority(Statement o1) {

	if (o1 instanceof SQLitePragma) {
	    return 5;
	} else if (o1 instanceof CreateTable) {
	    return 10;
	} else if (o1 instanceof Alter) {
	    return 20;
	} else if (o1 instanceof Select) {
	    /*
	     * Higher priority than Insert because SQLite spatial columns are added via
	     * SELECT AddGeometryColumn(...);
	     */
	    return 25;
	} else if (o1 instanceof Insert) {
	    return 30;
	} else if (o1 instanceof CreateIndex) {
	    return 40;
	} else if (o1 instanceof Comment) {
	    return 50;
	} else {
	    return 1000;
	}
    }

}
