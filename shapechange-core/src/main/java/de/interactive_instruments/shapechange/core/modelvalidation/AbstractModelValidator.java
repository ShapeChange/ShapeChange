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
 * (c) 2002-2024 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core.modelvalidation;

import de.interactive_instruments.shapechange.core.MessageSource;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.ValidationMode;
import de.interactive_instruments.shapechange.core.ShapeChangeResult.MessageContext;
import de.interactive_instruments.shapechange.core.model.Info;

/**
 * Provides convenience methods for model validators.
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public abstract class AbstractModelValidator implements ModelValidator, MessageSource {

    protected void report(Info i, MessageSource ms, int mnr, ValidationMode validationMode) {

	ShapeChangeResult result = i.result();

	MessageContext mc;
	if (validationMode == ValidationMode.strict) {
	    mc = result.addError(ms, mnr);
	} else {
	    mc = result.addWarning(ms, mnr);
	}
	if (mc != null) {
	    mc.addDetail("Context: " + i.fullName());
	}
    }

    protected void report(Info i, MessageSource ms, int mnr, String p1, ValidationMode validationMode) {

	ShapeChangeResult result = i.result();

	MessageContext mc;
	if (validationMode == ValidationMode.strict) {
	    mc = result.addError(ms, mnr, p1);
	} else {
	    mc = result.addWarning(ms, mnr, p1);
	}
	if (mc != null) {
	    mc.addDetail("Context: " + i.fullName());
	}
    }

    protected void report(Info i, MessageSource ms, int mnr, String p1, String p2, ValidationMode validationMode) {

	ShapeChangeResult result = i.result();

	MessageContext mc;
	if (validationMode == ValidationMode.strict) {
	    mc = result.addError(ms, mnr, p1, p2);
	} else {
	    mc = result.addWarning(ms, mnr, p1, p2);
	}
	if (mc != null) {
	    mc.addDetail("Context: " + i.fullName());
	}
    }

    protected void report(Info i, MessageSource ms, int mnr, String p1, String p2, String p3,
	    ValidationMode validationMode) {

	ShapeChangeResult result = i.result();

	MessageContext mc;
	if (validationMode == ValidationMode.strict) {
	    mc = result.addError(ms, mnr, p1, p2, p3);
	} else {
	    mc = result.addWarning(ms, mnr, p1, p2, p3);
	}
	if (mc != null) {
	    mc.addDetail("Context: " + i.fullName());
	}
    }

    protected void report(Info i, MessageSource ms, int mnr, String p1, String p2, String p3, String p4,
	    ValidationMode validationMode) {

	ShapeChangeResult result = i.result();

	MessageContext mc;
	if (validationMode == ValidationMode.strict) {
	    mc = result.addError(ms, mnr, p1, p2, p3, p4);
	} else {
	    mc = result.addWarning(ms, mnr, p1, p2, p3, p4);
	}
	if (mc != null) {
	    mc.addDetail("Context: " + i.fullName());
	}
    }
}
