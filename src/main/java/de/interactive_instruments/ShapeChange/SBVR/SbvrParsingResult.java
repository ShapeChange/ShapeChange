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
package de.interactive_instruments.ShapeChange.SBVR;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import de.interactive_instruments.ShapeChange.FOL.FolExpression;
import de.interactive_instruments.ShapeChange.Model.FolConstraint;

/**
 * @author Johannes Echterhoff
 *
 */
public class SbvrParsingResult {

	/**
	 * key: error category; value: error information
	 */
	private TreeMap<String, List<SbvrErrorInfo>> errorsByCategory = new TreeMap<String, List<SbvrErrorInfo>>();
	private FolConstraint con;
	private String ruleInvocationStackForHappyCase;
	private FolExpression folExpr;

	/**
	 * @return the errors
	 */
	public TreeMap<String, List<SbvrErrorInfo>> getErrors() {
		return errorsByCategory;
	}

	/**
	 * @param errorsByCategory
	 *            the errors to set
	 */
	public void setErrors(TreeMap<String, List<SbvrErrorInfo>> errorsByCategory) {
		this.errorsByCategory = errorsByCategory;
	}

	public void addError(SbvrErrorInfo error) {

		if (errorsByCategory == null) {
			errorsByCategory = new TreeMap<String, List<SbvrErrorInfo>>();
		}

		List<SbvrErrorInfo> errors = errorsByCategory.get(error
				.getErrorCategory().getName());

		if (errors == null) {
			errors = new ArrayList<SbvrErrorInfo>();
			errorsByCategory.put(error.getErrorCategory().getName(), errors);
		}

		errors.add(error);
	}

	
	public void addErrors(List<SbvrErrorInfo> errors) {

		if (errors != null) {

			for (SbvrErrorInfo error : errors) {
				this.addError(error);
			}
		}
	}

	public boolean hasRuleInvocationStack() {
		return ruleInvocationStackForHappyCase != null;
	}

	public void setRuleInvocationStack(String stack) {

		if (stack != null && stack.trim().length() > 0) {
			ruleInvocationStackForHappyCase = stack.trim();
		}
	}
	
	public String getRuleInvocationStack() {
		return ruleInvocationStackForHappyCase;
	}

	/**
	 * @return the con
	 */
	public FolConstraint getConstraint() {
		return con;
	}

	/**
	 * @param con the con to set
	 */
	public void setConstraint(FolConstraint con) {
		this.con = con;
	}

	public FolExpression getFirstOrderLogicExpression() {
		return this.folExpr;
	}
	
	public void setFirstOrderLogicExpression(FolExpression folExpr) {
		this.folExpr = folExpr;
	}

	public boolean hasFirstOrderLogicExpression() {
		return this.folExpr != null;
	}

}
