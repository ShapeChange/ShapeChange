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
 * (c) 2002-2015 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core.sbvr;

/**
 * Helper class to store SBVR rule information extracted from an external
 * source.
 * 
 * @author Johannes Echterhoff
 *
 */
public class SbvrRuleInfo {

	public static final String RULE_TEXT_COLUMN_NAME = "Text";
	public static final String RULE_NAME_COLUMN_NAME = "Name";
	public static final String COMMENT_COLUMN_NAME = "Comments";
	public static final String SCHEMA_PACKAGE_COLUMN_NAME = "Schema Package";
	public static final String CLASS_COLUMN_NAME = "Class";

	private String ruleName;
	private String ruleText;
	private String comment;
	private String schemaPackageName;
	private String className;

	/**
	 * @return the text
	 */
	public String getText() {
		return ruleText;
	}

	/**
	 * @param text
	 *            the text to set
	 */
	public void setText(String text) {
		this.ruleText = text;
	}

	/**
	 * @return the comment
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * @param comment
	 *            the comment to set
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

	/**
	 * @return the namespace
	 */
	public String getSchemaPackageName() {
		return schemaPackageName;
	}

	/**
	 * @param schemaPackageName
	 *            the namespace to set
	 */
	public void setSchemaPackageName(String schemaPackageName) {
		this.schemaPackageName = schemaPackageName;
	}

	/**
	 * @return the className
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * @param className
	 *            the className to set
	 */
	public void setClassName(String className) {
		this.className = className;
	}

	/**
	 * @return the ruleName
	 */
	public String getName() {
		return ruleName;
	}

	/**
	 * @param ruleName
	 *            the ruleName to set
	 */
	public void setName(String ruleName) {
		this.ruleName = ruleName;
	}

}
