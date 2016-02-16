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
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange.SBVR;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

/**
 * @author Johannes Echterhoff
 *
 */
public class SbvrErrorInfo {

	public enum Category {

		/**
		 * 
		 */
		UNKNOWN("UNKNOWN"),
		/**
		 * 
		 */
		SYNTAX_ERROR("SYNTAX"),
		/**
		 * 
		 */
		CLASS_NAME_INSIDE_PATH("RV-1-class-name-inside-path"),
		/**
		 * 
		 */
		MODEL_EXTERNAL_PROPERTY_WITHOUT_MAPPING("RV-2-model-external-property-without-mapping"),
		/**
		 * 
		 */
		NO_XPATH_NOTATION("RV-3-no-xpath-notation"),
		/**
		 * 
		 */
		NOT_A_NOUN_CONCEPT("RV-4-not-a-noun"),
		/**
		 * 
		 */
		NOT_A_VERB_CONCEPT("RV-5-not-a-verb"),
		/**
		 * 
		 */
		ILL_DEFINED_MODALITY("RV-6-ill-defined-modality"),
		/**
		 * 
		 */
		UNKNOWN_CLASS("RV-7-unknown-class"),
		/**
		 * 
		 */
		UNKNOWN_PROPERTY_TYPE("RV-8-unknown-property-type"),
		/**
		 * 
		 */
		UNKNOWN_PROPERTY("RV-9-unkown-property"),
		/**
		 * 
		 */
		UNKNOWN_SCHEMA_CALL("RV-10-unknown-schema-call"),
		/**
		 * 
		 */
		MIX_OF_AND_AND_OR("RV-11-mix-of-and-and-or"),
		/**
		 * 
		 */
		VERB_UNKNOWN_IN_CONTEXT("RV-12-verb-unknown-in-context"),
		/**
		 * 
		 */
		VERB_INVALID_FOR_GIVEN_PREDICATE(
				"RV-13-verb-invalid-for-given-predicate"),
		/**
		 * 
		 */
		PARSER("PARSER"),
		/**
		 * 
		 */
		AMBIGUOUS_CONTEXT("RV-14-ambiguous-context");

		private String name;

		Category(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

	}

	private Category errorCategory = Category.UNKNOWN;
	private String errorMessage;
	private int offendingTextStartIndex = -1;
	private int offendingTextStopIndex = -1;
	private String ruleInvocationStack;

	/**
	 * @return the errorMessage
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * @param errorMessage
	 *            the errorMessage to set
	 */
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	/**
	 * @return the offendingTextStartIndex
	 */
	public int getOffendingTextStartIndex() {
		return offendingTextStartIndex;
	}

	/**
	 * @param offendingTextStartIndex
	 *            the offendingTextStartIndex to set
	 */
	public void setOffendingTextStartIndex(int offendingTokenStartIndex) {
		this.offendingTextStartIndex = offendingTokenStartIndex;
	}

	/**
	 * @return the offendingTextStopIndex
	 */
	public int getOffendingTextStopIndex() {
		return offendingTextStopIndex;
	}

	/**
	 * @param offendingTextStopIndex
	 *            the offendingTextStopIndex to set
	 */
	public void setOffendingTextStopIndex(int offendingTokenStopIndex) {
		this.offendingTextStopIndex = offendingTokenStopIndex;
	}

	/**
	 * @return the ruleInvocationStack, can be <code>null</code>
	 */
	public String getRuleInvocationStack() {
		return ruleInvocationStack;
	}

	/**
	 * @param ruleInvocationStack
	 *            the ruleInvocationStack to set
	 */
	public void setRuleInvocationStack(String ruleInvocationStack) {
		this.ruleInvocationStack = ruleInvocationStack;
	}

	public boolean hasRuleInvocationStack() {
		return this.ruleInvocationStack != null;
	}

	/**
	 * @return the errorCategory
	 */
	public Category getErrorCategory() {
		return errorCategory;
	}

	/**
	 * @param errorCategory
	 *            the errorCategory to set
	 */
	public void setErrorCategory(Category errorCategory) {
		this.errorCategory = errorCategory;
	}

	public void setMetadataFromToken(Token token) {

		this.offendingTextStartIndex = token.getStartIndex();
		this.offendingTextStopIndex = token.getStopIndex();
	}

	public boolean hasOffendingTextInfo() {
		return this.offendingTextStartIndex >= 0
				&& this.offendingTextStopIndex >= 0;
	}

	public void setMetadataFromContext(ParserRuleContext ctx) {

		Token start = ctx.start;
		Token stop = ctx.stop;

		this.offendingTextStartIndex = start.getStartIndex();
		this.offendingTextStopIndex = stop.getStopIndex();
	}
}
