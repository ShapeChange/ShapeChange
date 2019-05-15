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

import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.ShapeChange.Target.SQL.structure.Alter;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.AlterExpression;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Column;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.ColumnDataType;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Comment;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.ConstraintAlterExpression;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.CreateIndex;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.CreateTable;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Index;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Insert;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.SqlConstraint;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Statement;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.StatementVisitor;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Table;

/**
 * Creates the DDL representation for the set of visited SQL statements.
 * <p>
 * NOTE: Database system specific visitors should be added as needed, to take
 * into account any database system specific syntax.
 * 
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class DdlVisitor implements StatementVisitor {

	protected StringBuffer sb = new StringBuffer();
	protected String crlf;
	protected String indent;
	protected SqlDdl sqlddl;

	public DdlVisitor(String crlf, String indent, SqlDdl sqlddl) {
		this.crlf = crlf;
		this.indent = indent;
		this.sqlddl = sqlddl;
	}

	@Override
	public void visit(Insert insert) {

		sb.append("INSERT INTO ");
		sb.append(insert.getTable().getName());

		if (insert.getColumns() != null) {
			sb.append(" ");
			sb.append(SqlUtil.getStringList(insert.getColumns(), true, true));
		}

		sb.append(" ");
		sb.append("VALUES ");

		if (insert.getExpressionList() != null) {
			sb.append(insert.getExpressionList());
		}

		sb.append(";");
		sb.append(crlf);
	}

	@Override
	public void visit(CreateIndex createIndex) {

		Index index = createIndex.getIndex();

		sb.append("CREATE ");

		if (index.getType() != null) {
			sb.append(index.getType());
			sb.append(" ");
		}

		sb.append("INDEX ");
		sb.append(index.getName());
		sb.append(" ON ");
		sb.append(createIndex.getTable().getName());

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
	public void visit(CreateTable ct) {

		Table table = ct.getTable();

		sb.append("CREATE TABLE ");
		sb.append(table.getName());

		sb.append(" (");
		sb.append(crlf);
		sb.append(crlf);

		if (table.getColumns() != null) {

			for (Iterator<Column> iter = table.getColumns().iterator(); iter
					.hasNext();) {

				Column col = iter.next();

				sb.append(indent);

				sb.append(col.getName());
				sb.append(" ");
				ColumnDataType colDataType = col.getDataType();
				sb.append(colDataType.getName());

				if (colDataType.hasPrecision()) {
					sb.append("(");
					sb.append(colDataType.getPrecision().toString());
					if (colDataType.hasScale()) {
						sb.append(",");
						sb.append(colDataType.getScale().toString());
					}
					sb.append(")");
				} else if (colDataType.hasLength()) {
					sb.append("(");
					sb.append(colDataType.getLength());
					if(colDataType.hasLengthQualifier()) {
						sb.append(" ");
						sb.append(colDataType.getLengthQualifier());
					}
					sb.append(")");
				}

				if (col.getDefaultValue() != null) {
					sb.append(" DEFAULT " + col.getDefaultValue());
				}

				if (col.getSpecifications() != null
						&& !col.getSpecifications().isEmpty()) {
					sb.append(" ");
					sb.append(StringUtils.join(col.getSpecifications(), " "));
				}

				if (iter.hasNext() || table.hasConstraints()) {
					sb.append(",");
				}

				if (SqlDdl.createDocumentation) {

					if (StringUtils.isNotBlank(col.getDocumentation())) {

						sb.append(" -- " + col.getDocumentation()
								.replaceAll("\\s+", " ").trim());
					}
				}

				sb.append(crlf);
			}
		}

		if (table.hasConstraints()) {

			for (Iterator<SqlConstraint> iter = table.getConstraints()
					.iterator(); iter.hasNext();) {

				SqlConstraint constr = iter.next();

				sb.append(indent).append(constr);

				if (iter.hasNext()) {
					sb.append(",");
				}

				sb.append(crlf);
			}
		}

		sb.append(")");
		sb.append(";");
		sb.append(crlf);
		sb.append(crlf);
	}

	@Override
	public void visit(Alter alter) {

		Table table = alter.getTable();

		sb.append("ALTER TABLE ");
		sb.append(table.getName());
		sb.append(" ");

		AlterExpression ae = alter.getExpression();

		sb.append(ae.getOperation());
		sb.append(" ");

		if (ae instanceof ConstraintAlterExpression) {

			ConstraintAlterExpression cae = (ConstraintAlterExpression) ae;
			sb.append(cae.toString());
		}

		sb.append(";");
		sb.append(crlf);
	}

	@Override
	public void visit(List<Statement> stmts) {

		if (stmts != null) {

			Statement last = null;

			for (Statement stmt : stmts) {

				/*
				 * Separate different types of statements with additional empty
				 * row.
				 */
				if (last != null && !last.getClass().getName()
						.equals(stmt.getClass().getName())) {

					sb.append(crlf);
				}

				stmt.accept(this);

				last = stmt;
			}
		}
	}

	public String getDdl() {
		return sb.toString();
	}

	@Override
	public void visit(Comment comment) {

		sb.append(comment.toString());
		sb.append(";");
		sb.append(crlf);
	}

	@Override
	public void postprocess() {
		// ignore
	}
}
