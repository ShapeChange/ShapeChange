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
import java.util.Set;

import org.antlr.v4.runtime.Token;

import de.interactive_instruments.ShapeChange.SBVR.SbvrErrorInfo.Category;
import de.interactive_instruments.antlr.sbvr.SBVRBaseListener;
import de.interactive_instruments.antlr.sbvr.SBVRParser.ModalityContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.PrefixedPredicateContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.QuantificationContext;

/**
 * Performs basic validation checks:
 * 
 * <ul>
 * <li>ensure that modality is not ill-defined (no use of "should")</li>
 * <li><i>more TBD</i></li>
 * </ul>
 * 
 * @author Johannes Echterhoff
 *
 */
public class SbvrValidationErrorListener extends SBVRBaseListener {

	private Set<String> nouns;
	private Set<String> verbs;

	private List<SbvrErrorInfo> errors = new ArrayList<SbvrErrorInfo>();

	public SbvrValidationErrorListener(Set<String> nouns, Set<String> verbs) {

		this.nouns = nouns;
		this.verbs = verbs;
	}

	@Override
	public void enterPrefixedPredicate(PrefixedPredicateContext ctx) {

		/*
		 * TODO this simple noun check is usually done automatically using the
		 * SbvrParserHelper; a more thorough test could be performed here
		 */
		if (ctx.noun != null) {
			validateNounConcept(ctx.noun);
		}
	}

	@Override
	public void enterQuantification(QuantificationContext ctx) {
		/*
		 * TODO this simple noun check is usually done automatically using the
		 * SbvrParserHelper; a more thorough test could be performed here
		 */
		validateNounConcept(ctx.noun);
	}

	public boolean validateNounConcept(Token t) {

		if (t == null) {
			String msg = "Unknown noun concept encountered.";
			addErrorMessage(Category.NOT_A_NOUN_CONCEPT, msg);
			return false;
		}

		String concept = t.getText();

		if (!isNoun(concept)) {

			String msg = concept + " is not a known noun concept.";
			addErrorMessage(Category.NOT_A_NOUN_CONCEPT, msg, t);
			return false;
		}

		return true;
	}

	private void addErrorMessage(Category category, String msg) {

		SbvrErrorInfo err = new SbvrErrorInfo();

		err.setErrorCategory(category);

		err.setErrorMessage(msg);

		this.errors.add(err);
	}

	@Override
	public void enterModality(ModalityContext ctx) {

		Token shouldToken = ctx.should;

		if (shouldToken != null) {
			String msg = "'should' is not an actual requirement and therefore not allowed.";
			addErrorMessage(Category.ILL_DEFINED_MODALITY, msg, shouldToken);
		}

		Token shouldNotToken = ctx.shouldNot;

		if (shouldNotToken != null) {
			String msg = "'should not' is not an actual requirement and therefore not allowed.";
			addErrorMessage(Category.ILL_DEFINED_MODALITY, msg, shouldNotToken);
		}
	}

	public boolean hasErrors() {
		return !this.errors.isEmpty();
	}

	public List<SbvrErrorInfo> getErrors() {
		return errors;
	}

	private void addErrorMessage(Category category, String msg, Token t) {

		SbvrErrorInfo err = new SbvrErrorInfo();

		err.setErrorCategory(category);

		err.setErrorMessage(msg);

		err.setMetadataFromToken(t);

		this.errors.add(err);
	}

	@Override
	public void enterVerbExpr(
			de.interactive_instruments.antlr.sbvr.SBVRParser.VerbExprContext ctx) {

		Token verbToken = ctx.verb;

		if (verbToken != null) {

			String concept = verbToken.getText();
			/*
			 * TODO this simple noun check is usually done automatically using the
			 * SbvrParserHelper; a more thorough test could be performed here
			 */
			if (!isVerb(concept)) {
				String msg = concept + " is not a known verb concept.";
				addErrorMessage(Category.NOT_A_VERB_CONCEPT, msg, verbToken);
			}
		}
	}

	public boolean isNoun(String token) {

		boolean result = true;

		if (token.contains(".")) {

			String[] parts = token.split("\\.");

			for (String part : parts) {

				if (part.length() > 0 && !nouns.contains(part)) {
					result = false;
				}
			}

		} else {

			if (!nouns.contains(token)) {
				result = false;
			}
		}

		return result;
	}

	public boolean isVerb(String token) {

		if (!verbs.contains(token)) {
			return false;
		} else {
			return true;
		}
	}
}
