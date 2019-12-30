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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

import org.antlr.v4.runtime.Token;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.FOL.AndOr;
import de.interactive_instruments.ShapeChange.FOL.AndOrType;
import de.interactive_instruments.ShapeChange.FOL.BinaryComparisonPredicate;
import de.interactive_instruments.ShapeChange.FOL.ClassCall;
import de.interactive_instruments.ShapeChange.FOL.ClassLiteral;
import de.interactive_instruments.ShapeChange.FOL.EqualTo;
import de.interactive_instruments.ShapeChange.FOL.Expression;
import de.interactive_instruments.ShapeChange.FOL.FolExpression;
import de.interactive_instruments.ShapeChange.FOL.HigherOrEqualTo;
import de.interactive_instruments.ShapeChange.FOL.HigherThan;
import de.interactive_instruments.ShapeChange.FOL.IsNull;
import de.interactive_instruments.ShapeChange.FOL.IsTypeOf;
import de.interactive_instruments.ShapeChange.FOL.LowerOrEqualTo;
import de.interactive_instruments.ShapeChange.FOL.LowerThan;
import de.interactive_instruments.ShapeChange.FOL.Not;
import de.interactive_instruments.ShapeChange.FOL.Predicate;
import de.interactive_instruments.ShapeChange.FOL.PropertyCall;
import de.interactive_instruments.ShapeChange.FOL.Quantification;
import de.interactive_instruments.ShapeChange.FOL.Quantifier;
import de.interactive_instruments.ShapeChange.FOL.RealLiteral;
import de.interactive_instruments.ShapeChange.FOL.SchemaCall;
import de.interactive_instruments.ShapeChange.FOL.StringLiteral;
import de.interactive_instruments.ShapeChange.FOL.StringLiteralList;
import de.interactive_instruments.ShapeChange.FOL.Variable;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.FolConstraint;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.SBVR.SbvrErrorInfo.Category;
import de.interactive_instruments.antlr.sbvr.SBVRBaseVisitor;
import de.interactive_instruments.antlr.sbvr.SBVRParser.AndorContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.AndornotContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.AssignmentPredicateContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.AssignmentPredicateInVerbExpressionContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.AtLeast2QuantifierContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.AtLeastNQuantifierContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.AtMostNQuantifierContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.AtMostOneQuantifierContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.ComparisonPredicateContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.ConditionInSentenceUsingObligationContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.ConditionInSentenceUsingShallContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.EqualToContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.ExactlyNQuantifierContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.ExactlyOneQuantifierContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.ExistentialQuantifierContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.HigherOrEqualToContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.HigherThanContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.LowerOrEqualToContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.LowerThanContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.NameExprContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.NumberContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.NumericRangeQuantifierContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.OfTypePredicateContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.OtherThanContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.PredicateContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.PrefixedPredicateContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.QuantificationContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.QuantificationWithOptionalQuantifierInVerbExpressionContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.RelativeClauseContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.RelativeClauseExprContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.SentenceContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.SentenceUsingObligationContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.SentenceUsingShallContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.SinglePredicateContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.UniversalQuantifierContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.VerbExprContext;

/**
 * Encapsulates the logic to parse First Order Logic expressions from an SBVR
 * constraint text.
 * 
 * @author Johannes Echterhoff
 *
 */
public class Sbvr2FolVisitor extends SBVRBaseVisitor<FolExpression> {

	private Stack<Variable> scopes = new Stack<Variable>();

	private Stack<PropertyCall> verbContexts = new Stack<PropertyCall>();

	/**
	 * Used internally to provide the expression for the left hand side of a
	 * binary predicate.
	 */
	private Expression leftExpr;

	private List<SbvrErrorInfo> errors = new ArrayList<SbvrErrorInfo>();
	private Model model;
	private Set<String> namesOfAllPropertiesInSelectedSchema = new HashSet<String>();

	/**
	 * Constraint that is currently being parsed; this field is primarily used
	 * when debugging (for example to use the constraint name in breakpoint
	 * expressions).
	 */
	private FolConstraint con;

	public Sbvr2FolVisitor(Model m, FolConstraint con) {
		this.model = m;
		this.con = con;

		SortedSet<? extends PackageInfo> selectedSchemas = model
				.selectedSchemas();

		for (PackageInfo selectedSchema : selectedSchemas) {
			SortedSet<ClassInfo> classes = model.classes(selectedSchema);
			for (ClassInfo ci : classes) {
				for (PropertyInfo pi : ci.properties().values()) {
					namesOfAllPropertiesInSelectedSchema.add(pi.name());
				}
			}
		}
	}

	// @Override
	public Quantification visitSentenceUsingObligation(
			SentenceUsingObligationContext ctx) throws SbvrParsingException {

		Quantification q = visitQuantification(ctx.quantification());

		Predicate p2 = this.visitConditionInSentenceUsingObligation(ctx
				.conditionInSentenceUsingObligation());

		// update p2 if the rule is a prohibition
		if (ctx.prohibition != null) {
			Not negation = new Not();
			negation.setPredicate(p2);
			p2 = negation;
		}

		Predicate predForQuantification;

		if (q.hasCondition()) {
			// combine selection predicate 'p1' in quantification with main
			// predicate 'p2' (via implies, realized as: not(p1) or p2)
			Predicate p1 = q.getCondition();
			Not n = new Not();
			n.setPredicate(p1);

			AndOr or = new AndOr();
			or.setType(AndOrType.or);
			or.addPredicate(n);
			or.addPredicate(p2);
			predForQuantification = or;

		} else {

			predForQuantification = p2;
		}

		/*
		 * 2015-05-15: would result in not(not(A) or B), which appears to be
		 * wrong - overall condition must be negated
		 */
		// // update predicate if the rule is a prohibition
		// if (ctx.prohibition != null) {
		// Not negation = new Not();
		// negation.setPredicate(predForQuantification);
		// predForQuantification = negation;
		// }

		q.setCondition(predForQuantification);

		/*
		 * we can now remove the variable scope that was set in the
		 * quantification represented by this sentence
		 */
		this.scopes.pop();

		return q;
	}

	// @Override
	public Quantification visitSentenceUsingShall(SentenceUsingShallContext ctx)
			throws SbvrParsingException {

		Quantification q = visitQuantification(ctx.quantification());

		Predicate p2 = this.visitConditionInSentenceUsingShall(ctx
				.conditionInSentenceUsingShall());

		Predicate predForQuantification;

		if (q.hasCondition()) {
			// combine selection predicate 'p1' in quantification with main
			// predicate 'p2' (via implies, realized as: not(p1) or p2)
			Predicate p1 = q.getCondition();
			Not n = new Not();
			n.setPredicate(p1);

			AndOr or = new AndOr();
			or.setType(AndOrType.or);
			or.addPredicate(n);
			or.addPredicate(p2);
			predForQuantification = or;

		} else {

			predForQuantification = p2;
		}
		q.setCondition(predForQuantification);

		/*
		 * we can now remove the variable scope that was set in the
		 * quantification represented by this sentence
		 */
		this.scopes.pop();

		return q;
	}

	// @Override
	public Predicate visitConditionInSentenceUsingObligation(
			ConditionInSentenceUsingObligationContext ctx)
			throws SbvrParsingException {

		Predicate firstVerbExpr = this.visitVerbExpr(ctx.verbExpr(0));

		if (ctx.verbExpr().size() == 1) {

			return firstVerbExpr;

		} else {

			/*
			 * parse additional predicates, concatenated via 'and' or 'or' (but
			 * NOT both)
			 */

			/*
			 * TBD: move check that it's all 'and' or 'or' to the validation
			 * listener?
			 */
			boolean andFound = false;
			boolean orFound = false;

			for (int i = 0; i < ctx.andornot().size(); i++) {

				AndornotContext andOrNot = ctx.andornot(i);
				if (andOrNot.and != null || andOrNot.andNot != null) {
					andFound = true;
				}
				if (andOrNot.or != null || andOrNot.orNot != null) {
					orFound = true;
				}

				if (andFound && orFound) {

					SbvrErrorInfo error = new SbvrErrorInfo();
					error.setErrorCategory(Category.MIX_OF_AND_AND_OR);
					error.setErrorMessage("The combination of verb expresions must not mix 'and' and 'or'.");
					error.setMetadataFromContext(andOrNot);

					throw new SbvrParsingException(error);
				}
			}

			AndOr logicExpr = new AndOr();
			if (andFound) {
				logicExpr.setType(AndOrType.and);
			} else {
				logicExpr.setType(AndOrType.or);
			}

			logicExpr.addPredicate(firstVerbExpr);

			for (int i = 1; i < ctx.verbExpr().size(); i++) {

				Predicate pi = this.visitVerbExpr(ctx.verbExpr(i));

				AndornotContext andOrNot = ctx.andornot(i - 1);

				if (andOrNot.andNot != null || andOrNot.orNot != null) {
					Not n = new Not();
					n.setPredicate(pi);
					logicExpr.addPredicate(n);
				} else {
					logicExpr.addPredicate(pi);
				}
			}

			return logicExpr;
		}
	}

	// @Override
	public Predicate visitConditionInSentenceUsingShall(
			ConditionInSentenceUsingShallContext ctx)
			throws SbvrParsingException {

		Predicate firstVerbExpr = this.visitVerbExpr(ctx.verbExpr(0));

		if (ctx.modality(0).shallNot != null) {
			Not n = new Not();
			n.setPredicate(firstVerbExpr);
			firstVerbExpr = n;
		}

		if (ctx.verbExpr().size() == 1) {

			return firstVerbExpr;

		} else {

			/*
			 * parse additional predicates, concatenated via 'and' or 'or' (but
			 * NOT both)
			 */

			/*
			 * TBD: move check that it's all 'and' or 'or' to the validation
			 * listener?
			 */
			boolean andFound = false;
			boolean orFound = false;

			for (int i = 0; i < ctx.andor().size(); i++) {

				AndorContext andOr = ctx.andor(i);
				if (andOr.and != null) {
					andFound = true;
				}
				if (andOr.or != null) {
					orFound = true;
				}

				if (andFound && orFound) {

					SbvrErrorInfo error = new SbvrErrorInfo();
					error.setErrorCategory(Category.MIX_OF_AND_AND_OR);
					error.setErrorMessage("The combination of verb expresions must not mix 'and' and 'or'.");
					error.setMetadataFromContext(andOr);

					throw new SbvrParsingException(error);
				}
			}

			AndOr logicExpr = new AndOr();
			if (andFound) {
				logicExpr.setType(AndOrType.and);
			} else {
				logicExpr.setType(AndOrType.or);
			}

			logicExpr.addPredicate(firstVerbExpr);

			for (int i = 1; i < ctx.verbExpr().size(); i++) {

				Predicate pi = this.visitVerbExpr(ctx.verbExpr(i));

				if (ctx.modality(i).shallNot != null) {
					Not n = new Not();
					n.setPredicate(pi);
					logicExpr.addPredicate(n);
				} else {
					logicExpr.addPredicate(pi);
				}
			}

			return logicExpr;
		}
	}

	public Predicate visitVerbExpr(VerbExprContext ctx)
			throws SbvrParsingException {

		Predicate result = null;

		String verb = ctx.verb.getText();
		PropertyCall pcFromVerb = null;

		if (verb.equalsIgnoreCase("has") || verb.equalsIgnoreCase("have")) {

			this.verbContexts.push(null);

		} else {

			/*
			 * ensure that the verb uniquely identifies a property within the
			 * current scope and create a PropertyCall to be used as context
			 * later on
			 */

			// get last element in current scope
			Variable var = this.scopes.peek();

			SchemaCall scLastSeg = var.getLastSegmentInValue();

			/*
			 * now determine the association role / property that is identified
			 * by the verb (which is the name of an association that the
			 * role/property is a navigable end of)
			 */
			PropertyInfo piIdentifiedByVerb;
			ClassInfo contextForVerb;

			if (scLastSeg instanceof ClassCall) {

				ClassCall ccLastSeg = (ClassCall) scLastSeg;
				contextForVerb = ccLastSeg.getSchemaElement();

				piIdentifiedByVerb = findPropertyByAssociationNameInClassInfo(
						verb, contextForVerb);

			} else {

				PropertyCall pcLastSeg = (PropertyCall) scLastSeg;
				PropertyInfo pi = pcLastSeg.getSchemaElement();

				Type t = pi.typeInfo();
				contextForVerb = model.classById(t.id);

				if (contextForVerb == null) {

					SbvrErrorInfo error = new SbvrErrorInfo();
					error.setErrorCategory(Category.UNKNOWN_PROPERTY_TYPE);
					error.setErrorMessage("Property '"
							+ pi.name()
							+ "' in class '"
							+ pi.inClass().name()
							+ "' provides the context for the schema call represented by verb '"
							+ verb
							+ "', but the schema does not contain class '"
							+ t.name + "' which is the type of property '"
							+ pi.name() + "'.");
					error.setMetadataFromToken(ctx.verb);

					throw new SbvrParsingException(error);

				} else {

					piIdentifiedByVerb = findPropertyByAssociationNameInClassInfo(
							verb, contextForVerb);
				}
			}

			if (piIdentifiedByVerb == null) {

				boolean stillNotFound = true;

				// try to get the property via AIXM feature time slice
				if (model.options().isAIXM()
						&& contextForVerb.category() == Options.FEATURE) {

					PropertyInfo ts = contextForVerb.property("timeSlice");

					ClassInfo tsType = model.classById(ts.typeInfo().id);

					piIdentifiedByVerb = findPropertyByAssociationNameInClassInfo(
							verb, tsType);

					if (piIdentifiedByVerb != null) {

						// create timeSlice PropertyCall
						PropertyCall tsPC = new PropertyCall();
						tsPC.setNameInSbvr("timeSlice");
						tsPC.setSchemaElement(ts);

						PropertyCall pcForVerb = new PropertyCall();
						pcForVerb.setNameInSbvr(verb);
						pcForVerb.setSchemaElement(piIdentifiedByVerb);

						tsPC.setNextElement(pcForVerb);

						pcFromVerb = tsPC;
						stillNotFound = false;

					} else {
						// throw exception like in non AIXM feature case
					}
				}

				if (stillNotFound) {

					SbvrErrorInfo error = new SbvrErrorInfo();
					error.setErrorCategory(Category.VERB_UNKNOWN_IN_CONTEXT);
					error.setErrorMessage("The context for verb (association name) '"
							+ verb
							+ "' is class '"
							+ contextForVerb.name()
							+ "'; neither that class nor one of its direct or indirect supertypes has a navigable property that is the role of an association with '"
							+ verb + "' as its name.");
					error.setMetadataFromToken(ctx.verb);

					throw new SbvrParsingException(error);
				}

			} else {

				pcFromVerb = new PropertyCall();
				pcFromVerb.setNameInSbvr(verb);
				pcFromVerb.setSchemaElement(piIdentifiedByVerb);
			}

			this.verbContexts.push(pcFromVerb);
		}

		if (ctx.quantificationWithOptionalQuantifierInVerbExpression() != null) {

			result = this
					.visitQuantificationWithOptionalQuantifierInVerbExpression(ctx
							.quantificationWithOptionalQuantifierInVerbExpression());

		} else {

			/*
			 * must be assignmentPredicateInVerbExpression because there are no
			 * other alternatives
			 */
			result = this.visitAssignmentPredicateInVerbExpression(ctx
					.assignmentPredicateInVerbExpression());
		}

		/*
		 * remove the verb context that was set by this verb expression
		 */
		this.verbContexts.pop();

		// finally, return the parsed predicate
		return result;
	}

	@Override
	public Predicate visitAssignmentPredicateInVerbExpression(
			AssignmentPredicateInVerbExpressionContext ctx) {

		if (this.verbContexts.peek() != null) {

			SbvrErrorInfo error = new SbvrErrorInfo();
			error.setErrorCategory(Category.VERB_INVALID_FOR_GIVEN_PREDICATE);
			error.setErrorMessage("Verb '"
					+ verbContexts.peek().getLastElement().getNameInSbvr()
					+ "' is invalid for an assignment predicate in a verb expression; expected verb 'has' or 'have'.");
			error.setMetadataFromContext(ctx);

			throw new SbvrParsingException(error);
		}

		if (ctx.assignmentPredicate() != null) {

			Quantification q = new Quantification();

			// parse optional quantifier
			Quantifier quantifier;

			if (ctx.quantifier() != null) {

				quantifier = (Quantifier) this.visit(ctx.quantifier());

			} else {

				// default quantifier is "at least one"
				quantifier = new Quantifier();
				quantifier.setLowerBoundary(1);
			}

			q.setQuantifier(quantifier);

			/*
			 * NOTE: parsing of the assignment predicate also creates the
			 * variable and pushes it onto the stack
			 */
			Predicate p = this.visitAssignmentPredicate(ctx
					.assignmentPredicate());
			q.setCondition(p);

			// pop variable created by visitation of assignmentPredicate
			Variable var = this.scopes.pop();
			q.setVar(var);

			return q;

		} else {

			// must be ctx.assignmentAndOtherThan() != null

			/*
			 * Because this is a specific combination of predicates, we handle
			 * it here
			 */

			AndOr and = new AndOr();
			and.setType(AndOrType.and);

			IsNull in = new IsNull();

			// in this specific case, the assignment refers to the already
			// defined variable
			Variable var = this.scopes.peek();
			in.setExpr(var);

			/*
			 * We check that the value is assigned, in other words it shall not
			 * be null
			 */
			Not not = new Not();
			not.setPredicate(in);

			and.addPredicate(not);

			// now parse the "other than" part
			Expression exprRight = this.visitNameExpr(ctx
					.assignmentAndOtherThan().nameExpr());

			EqualTo et = new EqualTo();
			et.setExprLeft(var);
			et.setExprRight(exprRight);

			Not n = new Not();
			n.setPredicate(et);
			and.addPredicate(n);

			return and;
		}
	}

	/**
	 * Searches for a property that belongs to the given class or one of its
	 * (direct and indirect) supertypes and that is a role of an association
	 * with the given name.
	 * 
	 * @param name
	 *            name of an association
	 * @param ci
	 * @return the property that belongs to the given class or one of its
	 *         (direct and indirect) supertypes and that is a role of an
	 *         association with the given name; <code>null</code> if no such
	 *         property was found
	 */
	private PropertyInfo findPropertyByAssociationNameInClassInfo(String name,
			ClassInfo ci) {

		for (PropertyInfo pi : ci.properties().values()) {

			if (pi.association() != null && pi.association().name() != null
					&& pi.association().name().equals(name)) {
				return pi;
			}
		}

		for (String supertypeId : ci.supertypes()) {

			ClassInfo supertype = model.classById(supertypeId);

			if (supertype != null) {

				PropertyInfo pi = findPropertyByAssociationNameInClassInfo(
						name, supertype);
				if (pi != null) {
					return pi;
				}
			}
		}

		return null;
	}

	@Override
	public Predicate visitRelativeClauseExpr(RelativeClauseExprContext ctx)
			throws SbvrParsingException {

		Predicate p = this.visitRelativeClause(ctx.relativeClause().get(0));

		if (ctx.relativeClause().size() == 1) {

			return p;

		} else {

			// ensure that verb expression level is not higher than 1 and
			// one of the following relative clauses uses a verb expression -
			// because then the sentence would be ambiguous
			if (this.verbContexts.size() >= 1) {

				for (int i = 1; i < ctx.relativeClause().size(); i++) {

					if (ctx.relativeClause().get(i).verbExpr() != null) {

						AndorContext andor = ctx.andor(i - 1);

						SbvrErrorInfo error = new SbvrErrorInfo();
						error.setErrorCategory(Category.AMBIGUOUS_CONTEXT);
						error.setErrorMessage("The context for the and|or connected relative clause is ambiguous.");
						error.setOffendingTextStartIndex(andor.start
								.getStartIndex());
						error.setOffendingTextStopIndex(ctx.relativeClause()
								.get(i).verbExpr().stop.getStopIndex());

						throw new SbvrParsingException(error);

					}
				}
			}

			/*
			 * parse additional predicates, concatenated via 'and' or 'or' (but
			 * NOT both)
			 */

			/*
			 * TBD: move check that it's all 'and' or 'or' to the validation
			 * listener?
			 */
			boolean andFound = false;
			boolean orFound = false;

			for (int i = 0; i < ctx.andor().size(); i++) {

				AndorContext andOr = ctx.andor(i);
				if (andOr.and != null) {
					andFound = true;
				}
				if (andOr.or != null) {
					orFound = true;
				}

				if (andFound && orFound) {

					SbvrErrorInfo error = new SbvrErrorInfo();
					error.setErrorCategory(Category.MIX_OF_AND_AND_OR);
					error.setErrorMessage("The combination of relative clauses must not mix 'and' and 'or'.");
					error.setMetadataFromContext(andOr);

					throw new SbvrParsingException(error);
				}
			}

			AndOr logExpr = new AndOr();
			if (andFound) {
				logExpr.setType(AndOrType.and);
			} else {
				logExpr.setType(AndOrType.or);
			}

			logExpr.addPredicate(p);

			for (int i = 1; i < ctx.relativeClause().size(); i++) {
				Predicate pi = this.visitRelativeClause(ctx.relativeClause()
						.get(i));
				logExpr.addPredicate(pi);
			}

			return logExpr;
		}
	}

	@Override
	public Predicate visitRelativeClause(RelativeClauseContext ctx) {

		Predicate result = null;

		if (ctx.singlePredicate() != null) {

			result = this.visitSinglePredicate(ctx.singlePredicate());

		} else {

			// must be 'that' ('not') verbExpr

			Predicate p = this.visitVerbExpr(ctx.verbExpr());

			if (ctx.not != null) {
				Not not = new Not();
				not.setPredicate(p);
				result = not;
			} else {
				result = p;
			}
		}

		return result;
	}

	@Override
	public Quantification visitQuantificationWithOptionalQuantifierInVerbExpression(
			QuantificationWithOptionalQuantifierInVerbExpressionContext ctx) {

		// parse optional quantifier
		Quantifier quantifier;

		if (ctx.quantifier() != null) {

			quantifier = (Quantifier) this.visit(ctx.quantifier());

		} else {

			// default quantifier is "at least one"
			quantifier = new Quantifier();
			quantifier.setLowerBoundary(1);
		}

		Quantification q;

		if (ctx.relativeClauseExpr() != null) {

			/*
			 * parse quantification - taking into account the current verb
			 * context
			 */
			q = parseQuantification(quantifier, ctx.noun,
					this.verbContexts.peek(), ctx.relativeClauseExpr());

		} else {

			q = new Quantification();
			q.setQuantifier(quantifier);

			Variable var;
			try {

				var = this.parseVariable(ctx.noun.getText(),
						this.verbContexts.peek());
				this.scopes.push(var);
				q.setVar(var);

			} catch (SbvrParsingException e) {

				SbvrErrorInfo error = e.getError();
				error.setMetadataFromToken(ctx.noun);

				throw e;
			}

			if (ctx.predicate() != null) {

				// handled like a singlePredicate with prefixedPredicate

				leftExpr = var;
				Predicate p = this.visitPredicate(ctx.predicate());
				leftExpr = null;

				q.setCondition(p);

			} else {

				// existence test results in not(isNull(var))

				Not n = new Not();
				IsNull in = new IsNull();
				in.setExpr(var);
				n.setPredicate(in);
				q.setCondition(n);
			}
		}

		this.scopes.pop();

		return q;
	}

	@Override
	public Quantification visitQuantification(QuantificationContext ctx) {

		Quantifier quantifier = (Quantifier) this.visit(ctx.quantifier());

		return parseQuantification(quantifier, ctx.noun, null,
				ctx.relativeClauseExpr());
	}

	/**
	 * @param quantifier
	 * @param noun
	 * @param verbContext
	 *            provides the context for evaluation of the verb; can be
	 *            <code>null</code>; if not <code>null</code> the context can be
	 *            a list of SchemaCalls and a copy of it will be prepended as-is
	 *            (so without checks for validity in context) to the SchemaCall
	 *            represented by the verb - the result is the value of the
	 *            variable
	 * @param relativeClauseExprCtx
	 * @return
	 */
	private Quantification parseQuantification(Quantifier quantifier,
			Token noun, PropertyCall verbContext,
			RelativeClauseExprContext relativeClauseExprCtx) {

		Quantification q = new Quantification();

		q.setQuantifier(quantifier);

		// parse noun concept (handle concatenation)
		String n = noun.getText();

		try {

			Variable v;

			if (verbContext != null) {
				v = parseVariable(n, verbContext);
			} else {
				v = parseVariable(n);
			}

			q.setVar(v);
			scopes.push(v);

		} catch (SbvrParsingException e) {

			SbvrErrorInfo error = e.getError();
			error.setMetadataFromToken(noun);

			throw e;
		}

		// parse optional relative clause
		if (relativeClauseExprCtx != null) {

			Predicate p = this.visitRelativeClauseExpr(relativeClauseExprCtx);
			q.setCondition(p);
		}

		return q;
	}

	@Override
	public FolExpression visitSentence(SentenceContext ctx) {

		try {

			// first establish 'self' variable context
			Variable v = new Variable(Variable.SELF_VARIABLE_NAME);
			v.setNextOuterScope(null);
			v.setValue(null);
			scopes.push(v);

			if (ctx.sentenceUsingObligation() != null) {
				return visitSentenceUsingObligation(ctx
						.sentenceUsingObligation());
			} else {
				return visitSentenceUsingShall(ctx.sentenceUsingShall());
			}
		} catch (SbvrParsingException e) {

			// this is where we really catch and log the error
			this.errors.add(e.getError());
			return null;
		}
	}

	/**
	 * Parses a variable from the given concept. The top element in the stack of
	 * scopes provides the nextOuterScope of the variable (that scope can be
	 * <code>null</code>), and the given concept is parsed as the value of the
	 * variable.
	 *
	 * Does not modify the stack of scopes.
	 *
	 * @param concept
	 * @return
	 * @throws SbvrParsingException
	 *             if the concept cannot be parsed to a valid schema call
	 */
	private Variable parseVariable(String concept) throws SbvrParsingException {

		Variable v = new Variable();

		Variable nextOuterScope = scopes.isEmpty() ? null : scopes.peek();
		v.setNextOuterScope(nextOuterScope);

		String concept_ = concept.trim();

		String[] parts = concept_.split("\\.");

		// determine variable value
		SchemaCall firstCall;

		if (nextOuterScope == null || nextOuterScope.getValue() == null) {

			// first part must be a ClassCall
			firstCall = parseClassCall(parts[0]);

		} else {

			// first part must be a PropertyCall
			firstCall = parsePropertyCall(parts[0], nextOuterScope);
		}

		firstCall.setVariableContext(nextOuterScope);
		v.setValue(firstCall);

		/*
		 * now parse the remaining parts, and connect them to the previous
		 * schema call
		 */
		SchemaCall previousElement = firstCall.getLastElement();

		for (int i = 1; i < parts.length; i++) {

			SchemaCall sc = parseSchemaCall(parts[i], previousElement);

			previousElement.setNextElement(sc);
			/*
			 * parsing may have injected a PropertyCall with the result, thus we
			 * always use the last element of the parsed SchemaCall
			 */
			previousElement = sc.getLastElement();
		}

		return v;
	}

	/**
	 * @param concept
	 * @param verbContext
	 *            if <code>null</code> the method calls
	 *            {@link #parseVariable(String)} with the given concept
	 * @return
	 * @throws SbvrParsingException
	 */
	private Variable parseVariable(String concept, PropertyCall verbContext)
			throws SbvrParsingException {

		if (verbContext == null) {
			return parseVariable(concept);
		}

		SchemaCall verbContextCopy = SbvrUtil.copy(verbContext);

		Variable v = new Variable();

		Variable nextOuterScope = scopes.isEmpty() ? null : scopes.peek();
		v.setNextOuterScope(nextOuterScope);

		String concept_ = concept.trim();

		String[] parts = concept_.split("\\.");

		verbContextCopy.setVariableContext(nextOuterScope);
		v.setValue(verbContextCopy);

		/*
		 * now parse the actual concept, and connect the parts to the previous
		 * schema call
		 */
		SchemaCall previousElement = verbContextCopy.getLastElement();

		for (int i = 0; i < parts.length; i++) {

			SchemaCall sc = parseSchemaCall(parts[i], previousElement);

			previousElement.setNextElement(sc);
			/*
			 * parsing may have injected a PropertyCall with the result, thus we
			 * always use the last element of the parsed SchemaCall
			 */
			previousElement = sc.getLastElement();
		}

		return v;
	}

	/**
	 * Parses a schema call based upon the given path segment name and depending
	 * upon the previous element in the path:
	 * <ul>
	 * <li>If the previous element is a ClassCall then the path segment must
	 * identify a property of that class (can also be inherited from one of its
	 * supertypes).</li>
	 * <li>If the previous element is a PropertyCall then the path segment must
	 * identify either:
	 * <ul>
	 * <li>the value type of the property (or one of its subtypes - but in any
	 * case: a class) or</li>
	 * <li>a property of the value type (or one of its supertypes).</li>
	 * </ul>
	 * </li>
	 * </ul>
	 * 
	 * @param pathSegmentName
	 * @param previousElement
	 *            must not be <code>null</code>
	 * @return
	 * @throws SbvrParsingException
	 *             if the pathSegmentName is invalid
	 */
	private SchemaCall parseSchemaCall(String pathSegmentName,
			SchemaCall previousElement) throws SbvrParsingException {

		SchemaCall result;

		if (previousElement instanceof ClassCall) {

			ClassCall cc = (ClassCall) previousElement;

			// then the pathSegmentName must identify a property of that class
			result = parsePropertyCall(pathSegmentName, cc);

		} else {

			// the previousElement is a PropertyCall

			PropertyCall pc = (PropertyCall) previousElement;

			/*
			 * The pathSegmentName either a) identifies the value type of the
			 * property (or one of its subtypes - but in any case: a class), or
			 * b) identifies a property that belongs to the value type of the
			 * property.
			 * 
			 * In case of a) we create a ClassCall, in case of b) we create a
			 * PropertyCall.
			 * 
			 * TBD: we could prevent case a) or not allow the use of a specific
			 * subtype of the properties value type TBD: for case b) we could
			 * also insert an intermediate ClassCall
			 */

			PropertyInfo pi = pc.getSchemaElement();

			Type ti = pi.typeInfo();

			ClassInfo classContext = model.classById(ti.id);

			if (classContext == null) {

				SbvrErrorInfo error = new SbvrErrorInfo();
				error.setErrorCategory(Category.UNKNOWN_PROPERTY_TYPE);
				error.setErrorMessage("Property '" + pi.name() + "' in class '"
						+ pi.inClass().name()
						+ "' provides the scope for the schema call '"
						+ pathSegmentName
						+ "', but the schema does not contain class '"
						+ ti.name + "' which is the type of property '"
						+ pi.name() + "'.");

				throw new SbvrParsingException(error);

			} else {

				if (isKindOf(pathSegmentName, classContext)) {

					/*
					 * case a) - the pathSegmentName identifies the type of the
					 * property (or one of its subtypes - but in any case: a
					 * class) -> create a ClassCall
					 */
					result = parseClassCall(pathSegmentName);

				} else if (classContext.property(pathSegmentName) != null) {

					/*
					 * case b) - the pathSegmentName identifies a property that
					 * belongs to the value type of the property -> create a
					 * PropertyCall
					 */
					PropertyCall newpc = new PropertyCall();
					newpc.setNameInSbvr(pathSegmentName);
					result = validateSchemaElement(newpc, classContext);

				} else {

					if (model.options().isAIXM()
							&& classContext.category() == Options.FEATURE) {

						PropertyInfo ts = classContext.property("timeSlice");

						ClassInfo tsType = model.classById(ts.typeInfo().id);

						PropertyInfo piInTSType = tsType
								.property(pathSegmentName);

						if (piInTSType != null) {

							// create timeSlice PropertyCall
							PropertyCall tsPC = new PropertyCall();
							tsPC.setNameInSbvr("timeSlice");
							tsPC.setSchemaElement(ts);

							PropertyCall pcForPathSegment = new PropertyCall();
							pcForPathSegment.setNameInSbvr(pathSegmentName);
							pcForPathSegment.setSchemaElement(piInTSType);

							tsPC.setNextElement(pcForPathSegment);

							return tsPC;

						} else {
							// raise parsing exception as in non AIXM feature
							// case
						}
					}

					// raise exception
					SbvrErrorInfo error = new SbvrErrorInfo();
					error.setErrorCategory(Category.UNKNOWN_SCHEMA_CALL);
					error.setErrorMessage("The context for the schema call '"
							+ pathSegmentName
							+ "' is provided by property '"
							+ pi.name()
							+ "' (in class '"
							+ pi.inClass().name()
							+ "') which is of type '"
							+ classContext.name()
							+ "' but '"
							+ pathSegmentName
							+ "' neither identifies the value type of that property (or one of its subtypes) nor does it identify a property of that value type.");
					throw new SbvrParsingException(error);
				}
			}
		}

		return result;
	}

	/**
	 * @param className
	 * @param ci
	 * @return <code>true</code> if the given className is the name of ci or one
	 *         of its subtypes in the complete subtype hierarchy; else
	 *         <code>false</code>
	 */
	private boolean isKindOf(String className, ClassInfo ci) {

		if (className.equals(ci.name())) {

			return true;

		} else {

			if (ci.subtypes() == null || ci.subtypes().isEmpty()) {

				return false;

			} else {

				for (String subtypeId : ci.subtypes()) {

					ClassInfo subtype = model.classById(subtypeId);

					if (isKindOf(className, subtype)) {
						return true;
					}
				}

				return false;
			}
		}
	}

	/**
	 * Parses a property call from the given propertyName and the class context
	 * provided by the schemaElement of the given ClassCall. The propertyName
	 * must identify a property of the class context.
	 * 
	 * @param propertyName
	 * @param cc
	 * @return
	 * @throws SbvrParsingException
	 *             if validation of the propertyName failed
	 */
	private PropertyCall parsePropertyCall(String propertyName, ClassCall cc)
			throws SbvrParsingException {

		PropertyCall pc = new PropertyCall();

		pc.setNameInSbvr(propertyName);

		return validateSchemaElement(pc, cc.getSchemaElement());

	}

	/**
	 * Parses a property call from the given propertyName, depending upon the
	 * class context provided by the schema call that is the last segment in the
	 * value of the given scope variable.
	 * <ul>
	 * <li>If the segment is a ClassCall then its schemaElement provides the
	 * class context.</li>
	 * <li>If the segment is a PropertyCall then the value type of its
	 * schemaElement (a PropertyInfo) provides the class context.</li>
	 * </ul>
	 * The propertyName must identify a property of the class context.
	 * 
	 * @param propertyName
	 * @param scope
	 * @return
	 * @throws SbvrParsingException
	 */
	private PropertyCall parsePropertyCall(String propertyName, Variable scope)
			throws SbvrParsingException {

		PropertyCall pc = new PropertyCall();

		pc.setNameInSbvr(propertyName);

		// now identify the correct PropertyInfo from the schema

		SchemaCall previousElementFromScope = scope.getLastSegmentInValue();

		ClassInfo classContext;

		if (previousElementFromScope instanceof ClassCall) {

			ClassCall tmp = (ClassCall) previousElementFromScope;
			classContext = tmp.getSchemaElement();

		} else {

			// previousElementFromScope is a PropertyCall

			PropertyCall tmp = (PropertyCall) previousElementFromScope;
			PropertyInfo pi = tmp.getSchemaElement();
			Type ti = pi.typeInfo();
			classContext = model.classById(ti.id);

			if (classContext == null) {

				SbvrErrorInfo error = new SbvrErrorInfo();
				error.setErrorCategory(Category.UNKNOWN_PROPERTY_TYPE);
				error.setErrorMessage("Property '"
						+ pi.name()
						+ "' in class '"
						+ pi.inClass().name()
						+ "' provides the scope for the schema call via property '"
						+ propertyName
						+ "', but the schema does not contain class '"
						+ ti.name + "' which is the type of property '"
						+ pi.name() + "'.");

				throw new SbvrParsingException(error);
			}
		}

		return validateSchemaElement(pc, classContext);
	}

	/**
	 * Tries to identify the schemaElement for the given PropertyCall, by
	 * looking up the nameInSbvr of the PropertyCall in the given (ClassInfo)
	 * context. If the property is found, it is set as schemaElement in the
	 * PropertyCall - otherwise an exception is raised.
	 * 
	 * If the property cannot be found in the given context, an attempt is made
	 * to find it in the timeSlice property of the context - which can only work
	 * if we are dealing with an AIXM schema.
	 * 
	 * @param pc
	 * @param context
	 * @return the validated PropertyCall, possibly a sequence of two
	 *         PropertyCalls if a 'timeSlice' step needed to be injected
	 * @throws SbvrParsingException
	 */
	private PropertyCall validateSchemaElement(PropertyCall pc,
			ClassInfo context) throws SbvrParsingException {

		/*
		 * look up the propertyName in classContext (search includes all
		 * supertypes)
		 */
		PropertyInfo pi = context.property(pc.getNameInSbvr());

		if (pi == null) {

			if (model.options().isAIXM()
					&& context.category() == Options.FEATURE) {

				PropertyInfo ts = context.property("timeSlice");

				ClassInfo tsType = model.classById(ts.typeInfo().id);

				pi = tsType.property(pc.getNameInSbvr());

				if (pi != null) {

					pc.setSchemaElement(pi);

					// plug in timeSlice PropertyCall
					PropertyCall tsPC = new PropertyCall();
					tsPC.setNameInSbvr("timeSlice");
					tsPC.setSchemaElement(ts);

					if (pc.hasVariableContext()) {
						tsPC.setVariableContext(pc.getVariableContext());
						pc.setVariableContext(null);
					}

					tsPC.setNextElement(pc);

					return tsPC;

				} else {
					// raise parsing exception as in non AIXM feature case
				}
			}

			SbvrErrorInfo error = new SbvrErrorInfo();
			error.setErrorCategory(Category.UNKNOWN_PROPERTY);
			error.setErrorMessage("Class '"
					+ context.name()
					+ "' provides the context for the property call '"
					+ pc.getNameInSbvr()
					+ "' - however, neither the class nor its supertypes have a property with that name.");

			throw new SbvrParsingException(error);

		} else {

			pc.setSchemaElement(pi);
			return pc;
		}
	}

	/**
	 * Creates a ClassCall from a given class name. The className must be the
	 * name of a class that can be found in the model.
	 * 
	 * @param className
	 * @return
	 * @throws SbvrParsingException
	 *             if the model does not contain a class with the given name
	 */
	private ClassCall parseClassCall(String className)
			throws SbvrParsingException {

		ClassInfo ci = model.classByName(className);

		/*
		 * TBD: this would be the place to look up names from external schema
		 * via configuration files
		 */

		if (ci == null) {

			SbvrErrorInfo error = new SbvrErrorInfo();
			error.setErrorCategory(Category.UNKNOWN_CLASS);
			error.setErrorMessage("The schema does not contain a class with name '"
					+ className + "'.");

			throw new SbvrParsingException(error);

		} else {

			ClassCall cc = new ClassCall();
			cc.setNameInSbvr(className);
			cc.setSchemaElement(ci);

			return cc;
		}
	}

	@Override
	public Quantifier visitUniversalQuantifier(UniversalQuantifierContext ctx) {

		Quantifier q = new Quantifier();
		return q;
	}

	@Override
	public Quantifier visitExistentialQuantifier(
			ExistentialQuantifierContext ctx) {

		Quantifier q = new Quantifier();
		q.setLowerBoundary(1);
		return q;
	}

	@Override
	public Quantifier visitExactlyOneQuantifier(ExactlyOneQuantifierContext ctx) {

		Quantifier q = new Quantifier();
		q.setLowerBoundary(1);
		q.setUpperBoundary(1);
		return q;
	}

	@Override
	public Quantifier visitExactlyNQuantifier(ExactlyNQuantifierContext ctx) {

		Quantifier q = new Quantifier();
		Integer i = Integer.valueOf(ctx.value.getText());
		q.setLowerBoundary(i);
		q.setUpperBoundary(i);
		return q;
	}

	@Override
	public Quantifier visitNumericRangeQuantifier(
			NumericRangeQuantifierContext ctx) {

		Quantifier q = new Quantifier();
		Integer lv = Integer.valueOf(ctx.lowerValue.getText());
		Integer uv = Integer.valueOf(ctx.upperValue.getText());
		q.setLowerBoundary(lv);
		q.setUpperBoundary(uv);
		return q;
	}

	@Override
	public Quantifier visitAtLeast2Quantifier(AtLeast2QuantifierContext ctx) {

		Quantifier q = new Quantifier();
		q.setLowerBoundary(2);
		return q;
	}

	@Override
	public Quantifier visitAtLeastNQuantifier(AtLeastNQuantifierContext ctx) {

		Quantifier q = new Quantifier();
		Integer i = Integer.valueOf(ctx.value.getText());
		q.setLowerBoundary(i);
		return q;
	}

	@Override
	public Quantifier visitAtMostOneQuantifier(AtMostOneQuantifierContext ctx) {

		Quantifier q = new Quantifier();
		q.setUpperBoundary(1);
		return q;
	}

	@Override
	public Quantifier visitAtMostNQuantifier(AtMostNQuantifierContext ctx) {

		Quantifier q = new Quantifier();
		Integer i = Integer.valueOf(ctx.value.getText());
		q.setUpperBoundary(i);
		return q;
	}

	@Override
	public Predicate visitSinglePredicate(SinglePredicateContext ctx) {

		Quantification q = new Quantification();

		// parse optional quantifier
		Quantifier quantifier;

		if (ctx.quantifier() != null) {

			quantifier = (Quantifier) this.visit(ctx.quantifier());

		} else {

			// default quantifier is "at least one"
			quantifier = new Quantifier();
			quantifier.setLowerBoundary(1);
		}
		q.setQuantifier(quantifier);

		/*
		 * NOTE: parsing of the actual predicates also creates the variable and
		 * pushes it onto the stack
		 */

		Predicate p;

		if (ctx.assignmentPredicate() != null) {
			p = this.visitAssignmentPredicate(ctx.assignmentPredicate());
		} else {
			// must be prefixedPredicate
			p = this.visitPrefixedPredicate(ctx.prefixedPredicate());
		}

		q.setCondition(p);

		// pop variable created by assignmentPredicate or prefixedPredicate
		Variable var = this.scopes.pop();
		q.setVar(var);

		return q;
	}

	@Override
	public Predicate visitPrefixedPredicate(PrefixedPredicateContext ctx) {

		try {

			Variable var = this.parseVariable(ctx.noun.getText());
			this.scopes.push(var);

			leftExpr = var;
			Predicate p = this.visitPredicate(ctx.predicate());
			leftExpr = null;

			return p;

		} catch (SbvrParsingException e) {

			SbvrErrorInfo error = e.getError();
			error.setMetadataFromToken(ctx.noun);

			throw e;
		}
	}

	@Override
	public Predicate visitPredicate(PredicateContext ctx) {

		Predicate p;

		if (ctx.comparisonPredicate() != null) {

			p = this.visitComparisonPredicate(ctx.comparisonPredicate());

		} else {
			// must be ofTypePredicate
			p = this.visitOfTypePredicate(ctx.ofTypePredicate());
		}

		if (ctx.not != null) {
			Not not = new Not();
			not.setPredicate(p);
			return not;
		} else {
			return p;
		}
	}

	@Override
	public Predicate visitComparisonPredicate(ComparisonPredicateContext ctx) {

		Predicate fol = (Predicate) this.visit(ctx.comparisonKeyword());

		Expression exprRight;

		if (ctx.nameExpr() != null) {

			exprRight = this.visitNameExpr(ctx.nameExpr());

		} else {

			exprRight = this.visitNumber(ctx.number());
		}

		// set expressions

		Predicate actual = fol;

		if (fol instanceof Not) {
			actual = ((Not) fol).getPredicate();
		}

		if (actual instanceof BinaryComparisonPredicate) {

			BinaryComparisonPredicate bcp = (BinaryComparisonPredicate) actual;

			bcp.setExprLeft(this.leftExpr);
			bcp.setExprRight(exprRight);

			return fol;

		} else {

			SbvrErrorInfo error = new SbvrErrorInfo();
			error.setErrorMessage("Expected a binary comparison operator.");
			error.setErrorCategory(Category.PARSER);
			error.setMetadataFromContext(ctx);

			throw new SbvrParsingException(error);
		}
	}

	@Override
	public RealLiteral visitNumber(NumberContext ctx) {

		String text = ctx.getText();

		double v = Double.parseDouble(text);

		RealLiteral rl = new RealLiteral();

		rl.setValue(v);

		return rl;
	}

	private boolean hasPropertyNameAsFirstElement(String noun) {

		String[] parts = noun.split("\\.");

		if (namesOfAllPropertiesInSelectedSchema.contains(parts[0]))
			return true;
		else
			return false;
	}

	@Override
	public Expression visitNameExpr(NameExprContext ctx) {

		List<String> names = new ArrayList<String>();

		for (Token t : ctx.values) {
			String s = t.getText();
			// strip leading and trailing "'"
			names.add(s.substring(1, s.length() - 1));
		}

		if (names.size() == 1) {

			StringLiteral sl = new StringLiteral();
			sl.setValue(names.get(0));
			return sl;

		} else {

			StringLiteralList sll = new StringLiteralList();
			sll.setValues(names);

			return sll;
		}
	}

	@Override
	public EqualTo visitEqualTo(EqualToContext ctx) {
		return new EqualTo();
	}

	@Override
	public HigherOrEqualTo visitHigherOrEqualTo(HigherOrEqualToContext ctx) {
		return new HigherOrEqualTo();
	}

	@Override
	public HigherThan visitHigherThan(HigherThanContext ctx) {
		return new HigherThan();
	}

	@Override
	public LowerOrEqualTo visitLowerOrEqualTo(LowerOrEqualToContext ctx) {
		return new LowerOrEqualTo();
	}

	@Override
	public LowerThan visitLowerThan(LowerThanContext ctx) {
		return new LowerThan();
	}

	@Override
	public Not visitOtherThan(OtherThanContext ctx) {

		EqualTo et = new EqualTo();
		Not n = new Not();
		n.setPredicate(et);

		return n;
	}

	@Override
	public Predicate visitOfTypePredicate(OfTypePredicateContext ctx)
			throws SbvrParsingException {

		List<ClassLiteral> typeClassLiterals = new ArrayList<ClassLiteral>();

		List<NameExprContext> nameContexts = ctx.nameExpr();

		for (NameExprContext nec : nameContexts) {

			Expression e = this.visitNameExpr(nec);

			TreeSet<String> typeNames = new TreeSet<String>();

			if (e instanceof StringLiteral) {
				typeNames.add(((StringLiteral) e).getValue());
			} else if (e instanceof StringLiteralList) {
				typeNames.addAll(((StringLiteralList) e).getValues());
			} else {

				// should not happen, unless grammar was changed
				SbvrErrorInfo error = new SbvrErrorInfo();
				error.setErrorCategory(Category.PARSER);
				error.setErrorMessage("Simple name or list of names expected in 'type-of' predicate, but found expression of type '"
						+ e.getClass().getSimpleName() + "'.");
				error.setMetadataFromContext(nec);

				throw new SbvrParsingException(error);
			}

			/*
			 * convert typeNames to set of ClassLiterals, thus making sure we
			 * have types that are actually known
			 */
			for (String typeName : typeNames) {

				ClassInfo typeCi = model.classByName(typeName);

				if (typeCi != null) {

					ClassLiteral cl = new ClassLiteral();
					cl.setSchemaElement(typeCi);
					typeClassLiterals.add(cl);

				} else {

					SbvrErrorInfo error = new SbvrErrorInfo();
					error.setErrorCategory(Category.UNKNOWN_CLASS);
					error.setErrorMessage("The model does not contain a class called '"
							+ typeName + "'.");
					error.setMetadataFromContext(nec);

					throw new SbvrParsingException(error);
				}
			}
		}

		if (typeClassLiterals.size() > 1) {

			Collections.sort(typeClassLiterals, new Comparator<ClassLiteral>() {
				public int compare(ClassLiteral o1, ClassLiteral o2) {
					return o1.getSchemaElement().name()
							.compareTo(o2.getSchemaElement().name());
				};
			});

			AndOr andor = new AndOr();
			andor.setType(AndOrType.or);

			for (ClassLiteral typeClassLiteral : typeClassLiterals) {

				IsTypeOf ito = new IsTypeOf();
				ito.setExprLeft(this.leftExpr);
				ito.setExprRight(typeClassLiteral);

				andor.addPredicate(ito);
			}

			return andor;

		} else {

			IsTypeOf ito = new IsTypeOf();
			ito.setExprLeft(this.leftExpr);
			ito.setExprRight(typeClassLiterals.get(0));

			return ito;
		}
	}

	@Override
	public Not visitAssignmentPredicate(AssignmentPredicateContext ctx) {

		IsNull p = new IsNull();

		try {

			Variable var = this.parseVariable(ctx.noun.getText());
			this.scopes.push(var);
			p.setExpr(var);

		} catch (SbvrParsingException e) {

			SbvrErrorInfo error = e.getError();
			error.setMetadataFromToken(ctx.noun);

			throw e;
		}

		/*
		 * We check that the value is assigned, in other words it shall not be
		 * null
		 */
		Not not = new Not();
		not.setPredicate(p);

		return not;
	}

	/**
	 * @return the errors
	 */
	public List<SbvrErrorInfo> getErrors() {
		return errors;
	}

	public boolean hasErrors() {
		return !this.errors.isEmpty();
	}

	@Override
	protected FolExpression aggregateResult(FolExpression aggregate,
			FolExpression nextResult) {

		if (aggregate == null && nextResult == null) {
			return null;
		} else if (nextResult != null) {
			return nextResult;
		} else {
			// aggregate != null
			return aggregate;
		}
	}
}
