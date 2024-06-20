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
package de.interactive_instruments.ShapeChange.Target.CDB;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
public class CDBUnit {

	protected Integer code;
	protected String symbol;
	protected String name;
	protected Set<String> aliasNames = new HashSet<String>();
	protected String description;

	public CDBUnit(Integer code, String symbol, String name,
			Set<String> aliasNames, String description) {
		super();
		this.code = code;
		this.symbol = symbol;
		this.name = name;
		if (aliasNames != null && !aliasNames.isEmpty()) {
			this.aliasNames = aliasNames;
		}
		this.description = StringUtils.stripToEmpty(description);
	}

	/**
	 * @return the code
	 */
	public Integer getCode() {
		return code;
	}

	/**
	 * @param code
	 *            the code to set
	 */
	public void setCode(Integer code) {
		this.code = code;
	}

	/**
	 * @return the symbol
	 */
	public String getSymbol() {
		return symbol;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the description; can be empty but not <code>null</code>
	 */
	public String getDescription() {
		return description;
	}

	public boolean hasCode() {
		return this.code != null;
	}

	/**
	 * @return set of alias names; can be empty but not <code>null</code>
	 */
	public Set<String> getAliasNames() {
		return this.aliasNames;
	}
}
