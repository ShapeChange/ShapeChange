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

package de.interactive_instruments.ShapeChange.Target;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;

public interface Target {

	/**
	 * Allows a target to perform the necessary initialization routines before
	 * processing.
	 * <p>
	 * Will be called by the {@link Converter} for each selected schema (see
	 * {@link Model#selectedSchemas()} and {@link PackageInfo#isSchema()}).
	 * 
	 * @param pi
	 *            a schema from the model selected via the configuration (see
	 *            {@link Model#selectedSchemas()}) - not necessarily always an
	 *            application schema
	 * @param m
	 * @param o
	 * @param r
	 * @param diagOnly
	 * @throws ShapeChangeAbortException
	 * @see Model#selectedSchemas()
	 * @see PackageInfo#isSchema()
	 */
	public void initialise(PackageInfo pi, Model m, Options o,
			ShapeChangeResult r, boolean diagOnly)
			throws ShapeChangeAbortException;

	/**
	 * The converter will call this method for each class belonging to the
	 * package given during initialization (see {@link #initialise}).
	 * <p>
	 * NOTE: will be called not only for the classes directly contained in the
	 * package, but also all sub-packages belonging to the same targetNamespace!
	 * 
	 * @param ci
	 */
	public void process(ClassInfo ci);

	public void write();

	public int getTargetID();

};