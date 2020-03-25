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
 * (c) 2002-2020 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Transformation.TypeConversion;

import de.interactive_instruments.ShapeChange.ConfigurationValidator;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessConfiguration;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.TransformerConfiguration;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class TypeConverterConfigurationValidator implements ConfigurationValidator, MessageSource {

    @Override
    public boolean isValid(ProcessConfiguration config, Options options, ShapeChangeResult result) {

	TransformerConfiguration trfConfig = (TransformerConfiguration) config;

	boolean isValid = true;

	if (trfConfig.hasParameter(TypeConverter.PARAM_METADATA_PROPERTY_INLINEORBYREFERENCE)) {

	    String mdpInlineOrByRef = config.parameterAsString(
		    TypeConverter.PARAM_METADATA_PROPERTY_INLINEORBYREFERENCE, "inlineOrByReference", false, true);
	    if (!("inlineOrByReference".equalsIgnoreCase(mdpInlineOrByRef)
		    || "byReference".equalsIgnoreCase(mdpInlineOrByRef)
		    || "inline".equalsIgnoreCase(mdpInlineOrByRef))) {

		result.addError(this, 101, mdpInlineOrByRef, TypeConverter.PARAM_METADATA_PROPERTY_INLINEORBYREFERENCE,
			"inline, byReference, inlineOrByReference");
	    }
	}

	return isValid;
    }

    @Override
    public String message(int mnr) {

	switch (mnr) {
	case 0:
	    return "Context: property '$1$'.";
	case 1:
	    return "Context: class '$1$'.";
	case 2:
	    return "Context: association class '$1$'.";
	case 3:
	    return "Context: association between class '$1$' (with property '$2$') and class '$3$' (with property '$4$')";

	case 10:
	    return "Syntax exception for regular expression '$1$' of parameter '$2$'. Message is: $3$.";

	// Validation messages
	case 100:
	    return "Parameter '$1$' is required for rule '$2$' but no actual value was found in the configuration.";
	case 101:
	    return "Value '$1$' of parameter '$2$' is not allowed. It must be one of: $3$";

	default:
	    return "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	}
    }
}