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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */

package de.interactive_instruments.shapechange.core.fop;

import de.interactive_instruments.shapechange.core.target.Target;
import de.interactive_instruments.shapechange.core.MessageSource;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;

//FOP
import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

public class FopErrorListener implements ErrorListener, MessageSource {
 
	@SuppressWarnings("unused")
	private Target baseClass = null;
	private String filename = null;
	private ShapeChangeResult result = null;
	
        public FopErrorListener(String name, ShapeChangeResult res, Target bc) {
        	filename = name;
        	result = res;
        	baseClass = bc;
        }

        public void warning(TransformerException arg0) throws TransformerException { 
                String warn = arg0.getMessage();
                result.addWarning(this, 303, filename, warn);
        }

        public void error(TransformerException arg0) throws TransformerException {
                String err = arg0.getMessage();
                result.addError(null, 304, filename, err);
        }

        public void fatalError(TransformerException arg0)
                        throws TransformerException { 
                String fatal = arg0.getMessage();
                result.addError(this, 305, filename, fatal);
        }
        
        @Override
        public String message(int mnr) {

        	switch (mnr) {

        	case 303:
        	    return "Warning while transforming '$1$'. Message: $2$";
        	case 305:
        	    return "Fatal error while transforming '$1$'. Message: $2$";
        	
        	default:
        	    return "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
        	}
        }
}
