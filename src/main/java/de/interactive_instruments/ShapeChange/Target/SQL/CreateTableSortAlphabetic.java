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
package de.interactive_instruments.ShapeChange.Target.SQL;

import java.util.Comparator;

import de.interactive_instruments.ShapeChange.Target.SQL.structure.CreateTable;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class CreateTableSortAlphabetic implements Comparator<CreateTable> {

    @Override
    public int compare(CreateTable o1, CreateTable o2) {

	if (SqlConstants.STATEMENT_SORT_USES_SCHEMA_NAME) {
	    return o1.getTable().getFullName().compareTo(o2.getTable().getFullName());
	} else {
	    return o1.getTable().getName().compareTo(o2.getTable().getName());
	}
    }

}
