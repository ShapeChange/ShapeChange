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
 * (c) 2002-2017 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.ea.util;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot
 *         de)
 *
 */
public enum EASupportedDBMS {

	ORACLE("Oracle", "dm_oracle.xml", "Oracle"), POSTGRESQL("PostgreSQL",
			"dm_postgresql.xml", "PostgreSQL"), SQLSERVER2012("SQL Server 2012",
					"dm_sqlserver2012.xml", "SQLServer2012"), SQLITE("SQLite",
							"dm_sqlite.xml", "SQLite");

	private String gentype;
	private String dmPatternFileName;
	private String dmPatternPackageName;

	EASupportedDBMS(String gentype, String dmPatternFileName,
			String dmPatternPackageName) {
		this.gentype = gentype;
		this.dmPatternFileName = dmPatternFileName;
		this.dmPatternPackageName = dmPatternPackageName;
	}

	public String getGenType() {
		return gentype;
	}

	public String getDmPatternFileName() {
		return dmPatternFileName;
	}

	/**
	 * @return the name of the &lt;&lt;DataModel&gt;&gt; package in the pattern.
	 */
	public String getDmPatternPackageName() {
		return dmPatternPackageName;
	}
}
