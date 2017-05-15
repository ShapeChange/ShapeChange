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

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class Comment implements Statement {

	private Table table = null;
	private Column column = null;
	private String text;

	/**
	 * @param table
	 * @param text
	 *            - shall not contain single quotes that have been escaped with
	 *            an additional single quote
	 */
	public Comment(Table table, String text) {
		super();

		this.table = table;
		this.text = text;
	}

	/**
	 * @param column
	 * @param text
	 *            - shall not contain single quotes that have been escaped with
	 *            an additional single quote
	 */
	public Comment(Column column, String text) {
		super();

		this.column = column;
		this.text = text;
	}

	@Override
	public void accept(StatementVisitor visitor) {
		visitor.visit(this);
	}

	/**
	 * @return the table
	 */
	public Table getTable() {
		return table;
	}

	/**
	 * @return the column
	 */
	public Column getColumn() {
		return column;
	}

	/**
	 * @return the text
	 */
	public String getText() {
		return text;
	}

	public String computeTargetName() {
		if (column == null) {
			return this.table.getName();
		} else {
			return this.column.getInTable().getName() + "."
					+ this.column.getName();
		}
	}

	public String toString() {

		StringBuilder sb = new StringBuilder();

		sb.append("COMMENT ON ");
		sb.append(column == null ? "TABLE " : "COLUMN ");
		sb.append(computeTargetName());
		sb.append(" IS '");
		sb.append(text.replaceAll("'", "''"));
		sb.append("'");

		return sb.toString();
	}
}
