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
 * (c) 2002-2012 interactive instruments GmbH, Bonn, Germany
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

package de.interactive_instruments.ShapeChange.UI;

import java.util.ArrayList;
import java.util.Iterator;
import de.interactive_instruments.ShapeChange.UI.StatusReader;

public class StatusBoard {

	private static StatusBoard statusBoard = null;
	
	private ArrayList<StatusReader> register = null;

	public static StatusBoard getStatusBoard(){
		if(statusBoard==null)
			statusBoard = new StatusBoard();
		return statusBoard;
	}

	private StatusBoard(){
		register = new ArrayList<StatusReader>();
	}
	
	public void registerStatusReader(StatusReader r){
		register.add(r);
	}
	
	public void statusChanged(int status){
		
		for (@SuppressWarnings("rawtypes")
		Iterator iter = register.iterator(); iter.hasNext();) {
			((StatusReader) iter.next()).statusChanged(status); 
		}
	}
	
	
	
};