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
package de.interactive_instruments.ShapeChange.Model.Generic.reader;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot
 *         de)
 *
 */
public class GenericModelErrorHandler implements ErrorHandler {

	private List<String> warnings = new ArrayList<>();
	private List<String> errors = new ArrayList<>();

	public boolean hasWarnings() {
		return !warnings.isEmpty();
	}

	public boolean hasErrors() {
		return !errors.isEmpty();
	}

	/**
	 * @return list of encountered warnings; can be empty but not
	 *         <code>null</code>
	 */
	public List<String> warnings() {
		return warnings;
	}

	/**
	 * @return list of encountered errors; can be empty but not
	 *         <code>null</code>
	 */
	public List<String> errors() {
		return errors;
	}

	@Override
	public void warning(SAXParseException spe) throws SAXException {
		warnings.add(this.getParseExceptionInfo(spe));

	}

	@Override
	public void error(SAXParseException spe) throws SAXException {
		errors.add(this.getParseExceptionInfo(spe));
	}

	@Override
	public void fatalError(SAXParseException spe) throws SAXException {
		String message = "Fatal Error: " + getParseExceptionInfo(spe);
		throw new SAXException(message);
	}

	private String getParseExceptionInfo(SAXParseException spe) {

		String systemId = spe.getSystemId();

		if (systemId == null) {
			systemId = "null";
		}

		String info = systemId + " (" + spe.getLineNumber() + ":"
				+ spe.getColumnNumber() + ") " + spe.getMessage();

		return info;
	}

}
