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

import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.FOL.FolExpression;
import de.interactive_instruments.ShapeChange.FOL.Variable;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.FolConstraint;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.TextConstraint;
import de.interactive_instruments.antlr.ShapeChangeAntlr.SbvrParserHelper;
import de.interactive_instruments.antlr.sbvr.SBVRLexer;
import de.interactive_instruments.antlr.sbvr.SBVRParser;

/**
 * Parses First Order Logic expressions from SBVR constraints.
 * 
 * @author Johannes Echterhoff
 *
 */
public class Sbvr2FolParser implements MessageSource {

	private ShapeChangeResult result;

	private Set<String> nouns = new TreeSet<String>();
	private Set<String> verbs = new TreeSet<String>();

	private Options options;

	private Model model;

	private SbvrParserHelper helper;
	private boolean logParsingErrorsAsInfos = false;
	private boolean hasErrors = false;

	public Sbvr2FolParser(Model m) {

		this.result = m.result();
		this.options = m.options();
		this.model = m;

		helper = SbvrUtil.createParserHelper(m);

		nouns = helper.nouns;
		verbs = helper.verbs;

		// System.out.println("----- Nouns:");
		// for (String noun : nouns) {
		// System.out.println("nouns.add(\"" + noun + "\");");
		// }
		// System.out.println();
		// System.out.println("----- Verbs:");
		// for (String verb : verbs) {
		// System.out.println("verbs.add(\"" + verb + "\");");
		// }

	}

	public Sbvr2FolParser(Model m, boolean logParsingErrorsAsInfos) {

		this(m);
		this.logParsingErrorsAsInfos = logParsingErrorsAsInfos;
	}

	/**
	 * Parsed a first order logic expression from the given constraint. If
	 * errors occur while parsing they are logged and <code>null</code> is
	 * returned.
	 * 
	 * @param con
	 * @return the first order logic expression represented by the constraint,
	 *         or <code>null</code> if errors were detected while parsing
	 */
	public FolExpression parse(FolConstraint con) {

		Variable.reset();

		SbvrParsingResult parsingResult = new SbvrParsingResult();
		parsingResult.setConstraint(con);

		ANTLRInputStream input = new ANTLRInputStream(con.text());

		// create a lexer that feeds off of input CharStream
		SBVRLexer lexer = new SBVRLexer(input);

		// create a buffer of tokens pulled from the lexer
		CommonTokenStream tokens = new CommonTokenStream(lexer);

		// create a parser that feeds off the tokens buffer
		SBVRParser parser = new SBVRParser(tokens);
		parser.helper = helper;

		/*
		 * remove ConsoleErrorListener and add our own
		 */
		parser.removeErrorListeners();
		SbvrErrorListener parsingErrorListener = new SbvrErrorListener();
		parser.addErrorListener(parsingErrorListener);

		// execute parsing, starting with rule 'sentence'
		ParseTree tree = parser.sentence();

		// get rule invocation stack for debugging
		parsingResult.setRuleInvocationStack(tree.toStringTree(parser));

		// if there were parsing errors, log them
		if (parsingErrorListener.hasErrors()) {

			parsingResult.addErrors(parsingErrorListener.getErrors());
			this.hasErrors = true;

		} else {

			// walk parse tree to apply further validation
			SbvrValidationErrorListener validationErrorListener = new SbvrValidationErrorListener(
					nouns, verbs);

			ParseTreeWalker walker = new ParseTreeWalker();
			walker.walk(validationErrorListener, tree);

			// if there were validation errors, log them
			if (validationErrorListener.hasErrors()) {

				parsingResult.addErrors(validationErrorListener.getErrors());
				this.hasErrors = true;

			} else {

				// no parsing or validation errors encountered

				// create FOL expression

				Sbvr2FolVisitor folVisitor = new Sbvr2FolVisitor(model, con);

				FolExpression folExpr = folVisitor.visit(tree);

				if (folExpr == null) {

					if (folVisitor.hasErrors()) {

						parsingResult.addErrors(folVisitor.getErrors());
						this.hasErrors = true;

					} else {

						if (logParsingErrorsAsInfos) {
							result.addInfo(this, 1);
						} else {
							result.addError(this, 1);
						}
					}

				} else {
					parsingResult.setFirstOrderLogicExpression(folExpr);
				}
			}
		}

		logParsingResult(parsingResult);

		if (parsingResult.hasFirstOrderLogicExpression()) {
			return parsingResult.getFirstOrderLogicExpression();
		} else {
			return null;
		}
	}

	private void logParsingResult(SbvrParsingResult parsingResult) {

		if (parsingResult != null) {

			TextConstraint con = parsingResult.getConstraint();

			if (parsingResult.getFirstOrderLogicExpression() != null) {
				result.addDebug(
						"SBVR constraint " + con.name() + ": " + con.text()
								+ (con.contextModelElmt() instanceof ClassInfo
										? " (on class '"
												+ con.contextModelElmt().name()
												+ "' in package '"
												+ ((ClassInfo) con
														.contextModelElmt())
																.pkg().name()
												+ ")"
										: ""));
				result.addDebug(SbvrConstants.INDENTATION_FOR_MESSAGE_DETAILS
						+ parsingResult.getFirstOrderLogicExpression()
								.toString());

			} else {

				TreeMap<String, List<SbvrErrorInfo>> errors = parsingResult
						.getErrors();

				if (!errors.isEmpty()) {

					String message = "SBVR constraint " + con.name() + ": "
							+ con.text()
							+ (con.contextModelElmt() instanceof ClassInfo
									? " (on class '"
											+ con.contextModelElmt().name()
											+ "' in package '"
											+ ((ClassInfo) con
													.contextModelElmt()).pkg()
															.name()
											+ ")"
									: "");

					if (logParsingErrorsAsInfos) {
						result.addInfo(message);
					} else {
						result.addError(message);
					}

					for (List<SbvrErrorInfo> ei : errors.values()) {
						SbvrUtil.printErrors(ei, con.text(), result,
								logParsingErrorsAsInfos);
						// printErrors(ei, con.text());
					}
				}
			}

			// rule stack is relevant for debugging
			if (parsingResult.hasRuleInvocationStack()) {
				result.addDebug(SbvrConstants.INDENTATION_FOR_MESSAGE_DETAILS
						+ "rule stack: "
						+ parsingResult.getRuleInvocationStack());
			}
		}
	}

	/**
	 * @return <code>true</code> if an error was encountered while parsing the
	 *         constraints, else <code>false</code>
	 */
	public boolean hasErrors() {
		return this.hasErrors;
	}

	@Override
	public String message(int mnr) {

		switch (mnr) {

		case 1:
			return "Translation to First Order Logic expression was not successfull but no errors were reported.";

		default:
			return "(" + Sbvr2FolParser.class.getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
