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

package de.interactive_instruments.ShapeChange.Fop;

import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Target.Target;

//FOP
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class FopMsgHandler extends Handler {

	private ShapeChangeResult result = null;
	@SuppressWarnings("unused")
	private Target xfBase = null;

	public FopMsgHandler(ShapeChangeResult r, Target x) {
		result = r;
		xfBase = x;
	};

	@Override
	public void close() throws SecurityException {
	}
	
	@Override
	public void flush() {
	}
	
	@Override
	public void publish(LogRecord record) {
		if(record.getLevel()==Level.INFO){
			result.addInfo("[Apache FOP message] " + record.getMessage());
		}
		if(record.getLevel()==Level.WARNING){
			result.addWarning("[Apache FOP message] " + record.getMessage());
		}
		if(record.getLevel()==Level.SEVERE){
			result.addError("[Apache FOP message] " + record.getMessage());
		}
	}
		
}
