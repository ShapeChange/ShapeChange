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
 * (c) 2002-2013 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments <dot>
 *         de)
 */
public class PackageInfoConfiguration {

	/**
	 * Required
	 */
	private String packageName;
	/**
	 * Optional
	 */
	private String nsabr;
	/**
	 * Optional
	 */
	private String ns;
	/**
	 * Optional
	 */
	private String xsdDocument;
	/**
	 * Optional
	 */
	private String version;

	public PackageInfoConfiguration(String packageName, String nsabr,
			String ns, String xsdDocument, String version) {
		super();
		this.packageName = packageName;
		this.nsabr = nsabr;
		this.ns = ns;
		this.xsdDocument = xsdDocument;
		this.version = version;
	}

	public String getPackageName() {
		return packageName;
	}

	public String getNsabr() {
		return nsabr;
	}

	public String getNs() {
		return ns;
	}

	public String getXsdDocument() {
		return xsdDocument;
	}

	public String getVersion() {
		return version;
	}
}
