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
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange.FOL;

import java.util.List;


/**
 * TBD
 * 
 * @author Johannes Echterhoff
 */
public class FunctionPredicate extends Predicate {

	private String name;
	private List<Expression> args;

	public FunctionPredicate() {

	}

	public boolean evaluate() {
		return false;
	}

	@Override
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append(name);
		sb.append("(");
		if(args != null && !args.isEmpty()) {
			
			sb.append(args.get(0).toString());
			
			for(int i=1; i<args.size(); i++) {
				sb.append(",");
				sb.append(args.get(i).toString());
			}
		}
		sb.append(")");
		
		return sb.toString();
	}
}