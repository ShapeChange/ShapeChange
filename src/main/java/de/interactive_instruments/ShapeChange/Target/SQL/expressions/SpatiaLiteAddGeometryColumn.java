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
 * (c) 2002-2019 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target.SQL.expressions;

import de.interactive_instruments.ShapeChange.Target.SQL.structure.Column;
import de.interactive_instruments.ShapeChange.Target.SQL.structure.Table;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot
 *         de)
 *
 */
public class SpatiaLiteAddGeometryColumn implements Expression {

	protected Table table;
	protected Column column;
	protected int srid;
	protected String geometryDimension;

	public SpatiaLiteAddGeometryColumn(Table table, Column column, int srid,
			String geometryDimension) {

		this.table = table;
		this.column = column;
		this.srid = srid;
		this.geometryDimension = geometryDimension;
	}

	@Override
	public void accept(ExpressionVisitor visitor) {
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
	 * @return the srid
	 */
	public int getSrid() {
		return srid;
	}

	/**
	 * @return the geometryDimension, can be <code>null</code>
	 */
	public String getGeometryDimension() {
		return geometryDimension;
	}

	public String toString() {

		StringBuffer sb = new StringBuffer();

		sb.append("AddGeometryColumn('");
		sb.append(this.table.getName());
		sb.append("', '");
		sb.append(this.column.getName());
		sb.append("', ");
		sb.append(srid);
		sb.append(", '");
		sb.append(this.column.getDataType().getName());
		sb.append("'");
		if(this.geometryDimension != null) {
			try {
				int dim = Integer.parseInt(geometryDimension);
				sb.append(", ");
				sb.append(dim);
			} catch(NumberFormatException e) {
				sb.append(", '");
				sb.append(this.geometryDimension);
				sb.append("'");
			}			
		}
		if(this.column.isNotNull()) {
			sb.append(", -1");
		} else {
			sb.append(", 0");
		}
		
		sb.append(")");

		return sb.toString();
	}
}
