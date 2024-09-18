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
 * (c) 2002-2015 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core.fol;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Johannes Echterhoff
 */
public class AndOr extends LogicalPredicate {

	private List<Predicate> predicateList;
	private AndOrType type;

	public AndOr() {

	}

	/**
	 * @return the predicateList
	 */
	public List<Predicate> getPredicateList() {
		return predicateList;
	}

	/**
	 * @param p
	 *            the new predicate
	 * @return true if the predicate list changed as a result of the call
	 */
	public boolean addPredicate(Predicate p) {

		if (predicateList == null) {
			predicateList = new ArrayList<Predicate>();
		}

		return predicateList.add(p);
	}

	/**
	 * @param predicateList
	 *            the predicateList to set
	 */
	public void setPredicateList(List<Predicate> predicateList) {
		this.predicateList = predicateList;
	}

	/**
	 * @return the type
	 */
	public AndOrType getType() {
		return type;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(AndOrType type) {
		this.type = type;
	}

	@Override
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("(");
		sb.append(predicateList.get(0).toString());
		
		for(int i=1; i<predicateList.size(); i++) {
			sb.append(") ");
			sb.append(type.toString());
			sb.append(" (");
			sb.append(predicateList.get(i).toString());
		}
		sb.append(")");
		
		return sb.toString();
	}	
}