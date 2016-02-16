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
 * Representation of a ProcessMapEntry, that can be part of a
 * ShapeChangeConfiguration.
 * 
 * @author echterhoff
 * 
 */
public class ProcessMapEntry {

	/**
	 * Value of the 'type' attribute in a map entry.
	 */
	protected String type;
	/**
	 * Value of the 'rule' attribute in a map entry.
	 */
	protected String rule;
	/**
	 * Value of the 'targetType' attribute in a map entry; <code>null</code> if
	 * it was not set.
	 */
	protected String targetType = null;
	/**
	 * Value of the 'param' attribute in a map entry; <code>null</code> if it
	 * was not set.
	 */
	protected String param = null;

	/**
	 * @param type
	 *            Value of the 'type' attribute in a map entry.
	 * @param rule
	 *            Value of the 'rule' attribute in a map entry.
	 */
	public ProcessMapEntry(String type, String rule) {
		this(type, rule, null, null);
	}

	/**
	 * @param type
	 *            Value of the 'type' attribute in a map entry.
	 * @param rule
	 *            Value of the 'rule' attribute in a map entry.
	 * @param targetType
	 *            Value of the 'targetType' attribute in a map entry;
	 *            <code>null</code> if it was not set.
	 */
	public ProcessMapEntry(String type, String rule, String targetType) {
		this(type, rule, targetType, null);
	}

	/**
	 * @param type
	 *            Value of the 'type' attribute in a map entry.
	 * @param rule
	 *            Value of the 'rule' attribute in a map entry.
	 * @param targetType
	 *            Value of the 'targetType' attribute in a map entry;
	 *            <code>null</code> if it was not set.
	 * @param param
	 *            Value of the 'param' attribute in a map entry;
	 *            <code>null</code> if it was not set.
	 */
	public ProcessMapEntry(String type, String rule, String targetType,
			String param) {
		super();
		this.type = type;
		this.rule = rule;
		this.targetType = targetType;
		this.param = param;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return the rule
	 */
	public String getRule() {
		return rule;
	}

	/**
	 * @return the targetType
	 */
	public String getTargetType() {
		return targetType;
	}

	/**
	 * @return the param
	 */
	public String getParam() {
		return param;
	}

	public boolean hasTargetType() {
		return (this.targetType != null && this.targetType.trim().length() > 0);
	}

	public boolean hasParam() {
		return (this.param != null && this.param.trim().length() > 0);
	}

}
