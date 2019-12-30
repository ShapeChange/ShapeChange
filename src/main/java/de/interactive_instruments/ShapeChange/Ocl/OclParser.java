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
 * (c) 2002-2012 interactive instruments GmbH, Bonn, Germany
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

package de.interactive_instruments.ShapeChange.Ocl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Ocl.OclNode.Declaration;

/**
 * The OclParser object implements a parser and intermediate code generator for
 * a subset of OCL. The latter is roughly characterized by the following traits
 * <ul>
 * <li>Full support of OCL built-in datatypes except Collections and Tuples.
 * <li>Sets are only implicitly supported as sets of built-in or UML class types
 * via the -&gt; operator.
 * <li>Only a small set of built-in operations and iterators is provided:
 * <i>allInstances(), size(), isEmpty(), notEmpty(), substring(), concat(),
 * oclIsKindOf(), exists()</i>
 * <li>All logical operators, arithmetic, relational.
 * <li>Access to all model attributes and roles.
 * <li>Core set of ISO19103 classes, such as CharacterString, mapped to
 * equivalent built-in types.
 * <li>Additional built-in type Date with constructor Date() for current date
 * and Date(iso8601:String) for a given date. Relational operators are in effect
 * for Dates.
 * <li>Supported condition types are <i>inv:</i>, <i>derive:</i>, <i>init:</i>.
 * Both latter need a Property context.
 * </ul>
 * The OclParser object is created by means of a no-argument constructor and can
 * then repeatedly be used to translate OCL expressions by invoking the
 * <i>parseOcl()</i> function. In case of a successful parse the resulting
 * syntax tree is returned as a OclNode.Expression object, null if the parse was
 * not successful. Various methods allow to inquire the number of generated
 * diagnostics, the diagnostics themselves, the contents of all OCL comments
 * detected, and debug output representing syntax tree representations.
 * <br><br>
 * The set of built-in operations recognized by the OclParser can be extended by
 * means of the method pair <i>addOperation(...)</i> and
 * <i>removeOperation(...).</i>
 * <br><br>
 * The OclParser works in two phases:
 * <ul>
 * <li>In the first phase a syntax tree of so-called TempNodes is created which
 * reflects the OCL-defined operator binding hierarchy. Access to the model is
 * not required in this phase.
 * <li>The second phase is only invoked if the first one succeeds without
 * errors. It connects the TempNode tree to the model and transforms the tree to
 * a structure reflecting the paradigm of Variables, PropertyCalls and Literals
 * implied by the OCL semantics. See the documentation of OclNode for further
 * explanations.
 * </ul>
 * 
 * The output of the second phase is used in ShapeChange Targets to generate OCL
 * equivalents in various target bindings.
 * <br><br>
 * OclParser objects are not designed for concurrent use. Each thread will
 * require its own OclParser object.
 * 
 * @version 0.1
 * @author Reinhard Erstling (c) interactive instruments GmbH, Bonn, Germany
 */
public class OclParser {

	// The Lexer object used by the OclParser
	private Lexer inLex = null;

	// The MessageCollection to output diagnostics into
	private MessageCollection messages = null;

	// List of Comment strings encountered in the parse
	private ArrayList<String> comments = null;

	// Environment variable declarations
	ArrayList<OclNode.Declaration> environmentDeclarations = new ArrayList<OclNode.Declaration>();

	// Intermediate TempNode tree
	TempNode.Expression tempNodeTree = null;

	// Final OclNode tree
	OclNode.Expression oclNodeTree = null;

	// Additional operations added by addOperation function
	static class AddedOperationSignature {
		String name;
		OclNode.DataType objType;
		OclNode.DataType retType;
		int noOfArgs;

		AddedOperationSignature(String n, OclNode.DataType ot,
				OclNode.DataType rt, int na) {
			name = n;
			objType = ot;
			retType = rt;
			noOfArgs = na;
		}
	}

	ArrayList<AddedOperationSignature> additionalOperations = new ArrayList<AddedOperationSignature>();

	// The following auxiliary definitions are for a unified treatment of all
	// infix operations of OCL. Each statically constructed InfixType object
	// stands for one level in the OCL operator binding hierarchy.

	static private class InfixType {
		EnumSet<Token.Type> operators;
		InfixType nextStronger;

		InfixType(InfixType nextStronger, EnumSet<Token.Type> operators) {
			this.nextStronger = nextStronger;
			this.operators = operators;
		}

		InfixType nextStronger() {
			return this.nextStronger;
		}

		boolean includesOperator(Token tok) {
			return this.operators.contains(tok.getType());
		}
	}

	// Multiplication, addition, comparisons, identity, logical, implies ...
	static InfixType mulType = new InfixType(null,
			EnumSet.of(Token.Type.ASTERISK, Token.Type.SLASH));
	static InfixType addType = new InfixType(mulType,
			EnumSet.of(Token.Type.PLUS, Token.Type.MINUS));
	static InfixType cmpType = new InfixType(addType,
			EnumSet.of(Token.Type.LESS, Token.Type.LESS_EQUAL,
					Token.Type.GREATER, Token.Type.GREATER_EQUAL));
	static InfixType idtType = new InfixType(cmpType,
			EnumSet.of(Token.Type.EQUAL, Token.Type.NOT_EQUAL));
	static InfixType logType = new InfixType(idtType,
			EnumSet.of(Token.Type.AND, Token.Type.OR, Token.Type.XOR));
	static InfixType impType = new InfixType(logType,
			EnumSet.of(Token.Type.IMPLIES));

	// References (.|->) ...
	static InfixType refType = new InfixType(null,
			EnumSet.of(Token.Type.DOT, Token.Type.ARROW));

	/**
	 * The OclParser object is created. It can be serially reused to do parsing
	 * operations. Multiple threads require multiple Parsers.
	 */
	public OclParser() {
	}

	/**
	 * Perform a full parse of an OCL expression addressed by a Readable. The
	 * output is returned as an OclNode.OclExpression.
	 * <br><br>
	 * Parsing starts at the condition type keyword, such as <i>inv:</i>,
	 * <i>derive:</i>, etc.
	 * 
	 * 
	 * @param inOcl
	 *                  Readable to fetch the OCL input from.
	 * @param ctx
	 *                  Object context in UML model, mostly ClassInfo
	 * @return OclNode.OclExpression object being the starting point of a syntax
	 *         tree. In case of error this is null.
	 */
	public OclNode.Expression parseOcl(Readable inOcl, Info ctx) {

		// Perform the required initializations
		inLex = new Lexer(inOcl);
		messages = new MessageCollection();
		comments = new ArrayList<String>(1);
		tempNodeTree = null;
		oclNodeTree = null;

		// First phase: Create the TempNode syntax tree
		tempNodeTree = parseExpression();

		// If this was o.k., trigger the second phase
		if (messages.getNumberOfMessages() == 0) {
			// Connect to the model
			oclNodeTree = tempNodeTree.connectToModelWithContext(this, ctx);
			if (messages.getNumberOfMessages() != 0)
				oclNodeTree = null;
		}
		return oclNodeTree;
	}

	/**
	 * Perform a full parse of an OCL expression addressed by a Readable. The
	 * output is returned as an OclNode.OclExpression.
	 * <br><br>
	 * This alternate version of the function starts parsing directly at the
	 * pure OCL statement. Condition type and name of the constraint have to be
	 * passed along as parameters.
	 * 
	 * 
	 * @param inOcl
	 *                  Readable to fetch the OCL input from.
	 * @param ctx
	 *                  Object context in UML model, mostly ClassInfo
	 * @param type
	 *                  Condition type, such as inv, init, derive ...
	 * @param name
	 *                  The name of the constraint (may be empty)
	 * @return OclNode.OclExpression object being the starting point of a syntax
	 *         tree. In case of error this is null.
	 */
	public OclNode.Expression parseOcl(Readable inOcl, Info ctx, String type,
			String name) {

		// Perform the required initializations
		inLex = new Lexer(inOcl);
		messages = new MessageCollection();
		comments = new ArrayList<String>(1);
		tempNodeTree = null;
		oclNodeTree = null;

		// First phase: Create the TempNode syntax tree
		tempNodeTree = parseExpression(type, name);

		// If this was o.k., trigger the second phase
		if (messages.getNumberOfMessages() == 0) {
			// Connect to the model
			oclNodeTree = tempNodeTree.connectToModelWithContext(this, ctx);
			if (messages.getNumberOfMessages() != 0)
				oclNodeTree = null;
		}
		return oclNodeTree;
	}

	/**
	 * This inquires the number of comments encountered in the OCL expression.
	 * 
	 * @return Number of comments in expression.
	 */
	public int getNumberOfComments() {
		return comments.size();
	}

	/**
	 * This methods inquires the payload of all comments encountered in parsing
	 * the OCL constraint. The comments will be presented in the sequence as
	 * found in the expression and will be stripped from surrounding blanks and
	 * java comment markup.
	 * 
	 * @return Array of comments encountered.
	 */
	public String[] getComments() {
		
		List<String> res = new ArrayList<String>();
		for (String c : comments) {
			String tmp = c.trim();
			if (tmp.startsWith("/*")) {
				tmp = tmp.substring(2);
			}
			if (tmp.endsWith("*/")) {
				tmp = tmp.substring(0, tmp.length() - 2);
			}
			tmp = tmp.trim();
			res.add(tmp);
		}
		
		// Convert list to array of strings
		return res.toArray(new String[0]);
	}

	/**
	 * Inquire the current number of messages for this OclParser.
	 * 
	 * @return Number of messages.
	 */
	public int getNumberOfMessages() {
		return messages == null ? 0 : messages.getNumberOfMessages();
	}

	/**
	 * Inquire the current message collection for this OclParser.
	 * 
	 * @return MessageCollection object
	 */
	public MessageCollection getMessageCollection() {
		return messages;
	}

	/**
	 * 
	 * Obtain a syntactical representation of the TempNode intermediate for
	 * debugging purposes.
	 * 
	 * 
	 * @return String with syntactical structure
	 */
	public String debugTempNodes() {
		StringWriter str = new StringWriter();
		PrintWriter write = new PrintWriter(str);
		if (tempNodeTree != null) {
			tempNodeTree.debugPrint(write);
			return str.toString();
		} else
			return "-- NO DEBUG STRING AVAILABLE --";
	}

	/**
	 * 
	 * Obtain a syntactical representation of the OclNode result for debugging
	 * purposes.
	 * 
	 * @return String of syntactical structure.
	 */
	public String debugOclNodes() {
		StringWriter str = new StringWriter();
		PrintWriter write = new PrintWriter(str);
		if (this.oclNodeTree != null) {
			oclNodeTree.debugPrint(write);
			return str.toString();
		} else
			return "-- NO DEBUG STRING AVAILABLE --";
	}

	/**
	 * s method adds a new variable declaration to the set of so-called
	 * environment variables. An environment variable is another variable
	 * declaration in addition to <i>self</i> which is automatically set up from
	 * the context of an expression.
	 * <br><br>
	 * This is an extension to OCL, which is sometimes useful to add predefined
	 * references to a set of constraints. It works like a big <i>let</i>, which
	 * surrounds the expression.
	 * 
	 * 
	 * @param name
	 *                      Name of the variable to be declared
	 * @param dt
	 *                      The datatype of the variable
	 * @param initValue
	 *                      Its initial value
	 */
	public void addEnvironmentVariable(String name, OclNode.DataType dt,
			OclNode initValue) {
		Declaration decl = new OclNode.Declaration(name, dt, initValue, null,
				null, false);
		int i;
		for (i = 0; i < environmentDeclarations.size(); i++)
			if (environmentDeclarations.get(i).name.equals(name))
				break;
		if (i == environmentDeclarations.size())
			environmentDeclarations.add(decl);
		else
			environmentDeclarations.set(i, decl);
	}

	/**
	 * 
	 * This one removes an environment variable by name.
	 * 
	 * 
	 * @param name
	 *                 The name of the environment variable
	 */
	public void removeEnvironmentVariable(String name) {
		for (int i = 0; i < environmentDeclarations.size(); i++)
			if (environmentDeclarations.get(i).name.equals(name)) {
				environmentDeclarations.remove(i);
				break;
			}
	}

	/**
	 * 
	 * This method adds an operation to the set of operations the parser
	 * recognizes as valid OCL operations. The operation is identified by its
	 * name, the number of parameters and the type of object it can be applied
	 * to.
	 * 
	 * 
	 * @param name
	 *                     Name of operation
	 * @param objtype
	 *                     Datatype the operation can be applied to. If given as
	 *                     null, the operation can be applied to any type.
	 * @param rettype
	 *                     The return type of the operation
	 * @param noOfArgs
	 *                     The number of parameters. If given as -1 any number
	 *                     of parameters is permitted
	 */
	public void addOperation(String name, OclNode.DataType objtype,
			OclNode.DataType rettype, int noOfArgs) {
		for (AddedOperationSignature ao : additionalOperations) {
			if (!ao.name.equals(name))
				continue;
			if (ao.objType != null
					&& (objtype == null || !objtype.isSubTypeOf(ao.objType)))
				continue;
			if (ao.noOfArgs >= 0 && ao.noOfArgs != noOfArgs)
				continue;
			return;
		}
		additionalOperations.add(
				new AddedOperationSignature(name, objtype, rettype, noOfArgs));
	}

	/**
	 * 
	 * By this method you can remove operations from the set of operations the
	 * parser recognizes as valid OCL operations. The operations to be removed
	 * are identified by by their names, the number of parameters and the type
	 * of object it can be applied to.
	 * 
	 * 
	 * @param name
	 *                     The name of the operations to be removed
	 * @param objtype
	 *                     The Datatype the operation can be applied to. Can be
	 *                     specified as null with a wildcard meaning.
	 * @param noOfArgs
	 *                     The number of parameters. If given as -1 any number
	 *                     of parameters will be eligible for removal.
	 */
	public void removeOperation(String name, OclNode.DataType objtype,
			int noOfArgs) {

		Iterator<AddedOperationSignature> it = additionalOperations.iterator();
		while (it.hasNext()) {
			AddedOperationSignature ao = it.next();
			if (!ao.name.equals(name))
				continue;
			if (objtype != null
					&& (ao.objType == null || !ao.objType.isSubTypeOf(objtype)))
				continue;
			if (noOfArgs >= 0 && ao.noOfArgs != noOfArgs)
				continue;
			it.remove();
		}
	}

	/**
	 * This OclParser method parses an OCL expression including the type of the
	 * expression. Parse is started at the condition type like <i>inv:</i>.
	 * 
	 * @return A TempNode.Expression object
	 */
	TempNode.Expression parseExpression() {

		// First thing to fetch is the condition, inv:, derive: etc.
		Token condtoken = getNextLegalToken();
		Token.Type cond = condtoken.getType();
		if (cond != Token.Type.INV && cond != Token.Type.PRE
				&& cond != Token.Type.POST && cond != Token.Type.INIT
				&& cond != Token.Type.DERIVE && cond != Token.Type.BODY) {
			MessageCollection.Message mess = messages.new Message(15);
			mess.substitute(1, condtoken.getDenotation());
			mess.addSourceReference(condtoken.getSourceReference());
		}

		// Next maybe a name for the constraint.
		Token token = getNextLegalToken();
		Token.Type type = token.getType();
		String name = null;
		if (type == Token.Type.IDENTIFIER)
			name = ((Token.Identifier) token).getName();
		else
			inLex.unfetchToken(token);

		// Next must be colon.
		token = getNextLegalToken();
		type = token.getType();
		if (type != Token.Type.COLON) {
			MessageCollection.Message mess = messages.new Message(16);
			mess.substitute(1, token.getDenotation());
			mess.addSourceReference(token.getSourceReference());
			inLex.unfetchToken(token);
		}

		// Now the expression itself ...
		TempNode expr = parsePureExpression();

		// Create and return the TempNode.Expression object.
		return new TempNode.Expression(cond, name, expr);
	}

	/**
	 * This is an alternate OclParser method to parse an OCL expression. For
	 * this method the condition type (such as inv...) is not analyzed from the
	 * OCL statement, but is instead passed to the method as a parameter. The
	 * same applies to the name of the constraint.
	 * 
	 * @param type
	 *                 Condition type, such as inv, init, derive ...
	 * @param name
	 *                 The name of the constraint (may be empty)
	 * @return A TempNode.Expression object
	 */
	TempNode.Expression parseExpression(String type, String name) {

		// Translate the condition type to a Token symbol
		final String[] typenames = { "inv", "pre", "post", "init", "derive",
				"body" };
		final Token.Type[] typetokens = { Token.Type.INV, Token.Type.PRE,
				Token.Type.POST, Token.Type.INIT, Token.Type.DERIVE,
				Token.Type.BODY };
		Token.Type cond = Token.Type.INV;
		for (int i = 0; i < typenames.length; i++)
			if (type.equals(typenames[i]))
				cond = typetokens[i];

		// Now the expression itself ...
		TempNode expr = parsePureExpression();

		// Create and return the TempNode.Expression object.
		return new TempNode.Expression(cond, name, expr);
	}

	/**
	 * This OclParser method parses an OCL expression without the introducing
	 * context head, which means everything following inv:, derive:, etc.
	 * 
	 * @return TempNode tree of parse
	 */
	TempNode parsePureExpression() {

		// Pass to topmost infix operation ...
		TempNode tempNode = parseInfix(impType);

		// Make sure we reached end of text. If not, complain ...
		Token token = getNextLegalToken();
		if (token.getType() != Token.Type.END_OF_TEXT) {
			MessageCollection.Message mess = messages.new Message(14);
			mess.substitute(1, token.getDenotation());
			mess.addSourceReference(token.getSourceReference());
		}

		return tempNode;
	}

	/**
	 * This function parses any of the OCL infix operators according to the
	 * adapted OCL operator priority scheme. <br>
	 * The recognized syntax is: <i>operand</i> { <i>operator</i> <i>operand</i>
	 * }* <br>
	 * Chains of infix operators of one binding level are treated in a
	 * left-associative way, such as: ((a+b)+c)+d. <br>
	 * The distinct levels from weakest to strongest operator binding are:
	 * <ul>
	 * <li>implies
	 * <li>and | or | xor
	 * <li>= | <>
	 * <li>< | > | <= | >=
	 * <li>+ | -
	 * <li>* | /
	 * <li>prefix operations (not handled by this function)
	 * <li>. | -&gt;
	 * </ul>
	 * 
	 * @param itype
	 *                  Kind of infix operator expressed by instance of
	 *                  InfixType
	 * @return Node of the OCL syntax tree representing the infix operation.
	 */
	TempNode parseInfix(InfixType itype) {

		// Determine next stronger binding level of infix operators.
		InfixType itype1 = itype.nextStronger();

		// Initialize operands and operator variables.
		TempNode operand1 = null, operand2 = null;
		Token operator = null;

		// Recognize operand operator pairs until no further valid operator
		// can be seen any more. Invalid tokens we just skip
		do {
			// Execute operand parse on next lower level
			if (itype1 != null)
				operand2 = parseInfix(itype1);
			else if (itype == mulType)
				operand2 = parsePrefix();
			else // Must be refType
				operand2 = parsePrimary();

			// Last operand (operand1) is the one just determined in the first
			// go and the last Infix-node otherwise
			if (operator == null)
				operand1 = operand2;
			else {
				operand1 = new TempNode.Infix(operator.getType(), operand1,
						operand2);
				operand1.addSourceReference(operator);
			}

			// Now try the next operator token. If we see ILLEGAL tokens we
			// skip these and generate diagnostics.
			operator = getNextLegalToken();
		} while (itype.includesOperator(operator));

		// So, the last token was not a proper operator. Put it back for use
		// by the next higher level.
		inLex.unfetchToken(operator);

		// The last operand is the one to be returned.
		return operand1;
	}

	/**
	 * parsePrefix carries out the parsing for prefix operations, as there are
	 * prefix minus (-) and not. Prefix operations bind stronger than
	 * multiplication and division but weaker than object referencing by dot and
	 * arrow. <br>
	 * The recognized syntax is: { <i>operator</i> }* <i>operand</i> <br>
	 * Chains of prefix operations are processed in a right-associative way,
	 * such as in: <i>-(-(-a))</i>
	 * 
	 * @return Node of the temporary syntax tree representing the left most
	 *         prefix operation
	 */
	TempNode parsePrefix() {

		// Result variable
		TempNode result = null;

		// Get the first token and its type.
		Token token = getNextLegalToken();
		Token.Type type = token.getType();

		// Do we have a prefix operation?
		if (type == Token.Type.MINUS || type == Token.Type.NOT) {
			// Yes: So we construct a Prefix node out of the recursive rest
			TempNode rest = parsePrefix();
			result = new TempNode.Prefix(type, rest);
			result.addSourceReference(token);
			// If a Literal is prefixed and if the type of the literal fits
			// the prefix, fold the prefix into the literal ...
			if (rest instanceof TempNode.Literal) {
				TempNode.Literal<?> literal = (TempNode.Literal<?>) rest;
				Object value = literal.getValue();
				TempNode result2 = null;
				if (type == Token.Type.MINUS) {
					if (value instanceof Long)
						result2 = new TempNode.Literal<Long>(-(Long) value);
					else if (value instanceof Double)
						result2 = new TempNode.Literal<Double>(-(Double) value);
				} else if (type == Token.Type.NOT && value instanceof Boolean)
					result2 = new TempNode.Literal<Boolean>(!(Boolean) value);
				if (result2 != null) {
					result2.addSourceReference(result);
					result = result2;
				}
			}
		} else {
			// No: Rid the token and go to the next stronger binding level
			inLex.unfetchToken(token);
			result = parseInfix(refType);
		}

		return result;
	}

	/**
	 * The parsePrimary function is responsible for parsing primary constructs
	 * like identifiers, function invocations, literals, if, let and bracketed
	 * expressions.
	 * 
	 * @return Node of the temporary syntax tree representing the primary
	 *         expression.
	 */
	TempNode parsePrimary() {

		TempNode expression = null;

		// Fetch the first token to decide how to proceed
		Token token = getNextLegalToken();
		switch (token.getType()) {

		case IF:
			// if expression then expression else expression endif
			inLex.unfetchToken(token);
			return parseIf();

		case LET:
			// let vardecl in expression
			inLex.unfetchToken(token);
			return parseLet();

		case O_BRACKET:
			// Must be a bracketed expression: ( expression )
			expression = parseInfix(impType);
			expression.addSourceReference(token);
			token = getNextLegalToken();
			if (token.getType() != Token.Type.C_BRACKET) {
				// Closing bracket not found. Nag and unfetch the token.
				MessageCollection.Message mess = messages.new Message(2);
				mess.substitute(1, token.getDenotation());
				mess.addSourceReference(token.getSourceReference());
				inLex.unfetchToken(token);
			} else
				expression.addSourceReference(token);
			return expression;

		case TEXT:
		case NUMBER:
			// This is a clear case of literal expression
			inLex.unfetchToken(token);
			return parseSimpleLiteral();

		case IDENTIFIER:
			// This can be either a proper identifier or the introduction
			// to a constant. We need to find out by means of the name.
			// Note that we cannot detect Enumeration constants, because
			// they require UML model access.
			Token.Identifier idtok = (Token.Identifier) token;
			String name = "$" + idtok.getName() + "$";
			inLex.unfetchToken(token);
			if ("$true$false$null$".indexOf(name) >= 0)
				return parseSimpleLiteral();
			if ("$Set$Bag$Sequence$Collection$OrderedSet$Tuple$"
					.indexOf(name) >= 0)
				return parseComplexLiteral();
			if ("$Date$".indexOf(name) >= 0)
				return parseDateLiteral();
			// Constants are treated and removed. Must be ordinary
			// identifier, which means it can be scoped name.
			expression = parsePathName();
			// An argument list may follow ...
			token = getNextLegalToken();
			Token.Type type = token.getType();
			inLex.unfetchToken(token);
			TempNode argumentlist = null;
			if (type == Token.Type.O_BRACKET) {
				// There is an argument list. Go and get it recognized.
				argumentlist = parseArgumentList();
			}
			// We construct a property node, which contains the identifier
			// and the argument list, if present.
			expression = new TempNode.Property(expression, argumentlist);
			return expression;

		default:
			// So, this is something, which does not belong here.
			// Nag, leave skipped and create an Invalid node.
			MessageCollection.Message mess = messages.new Message(3);
			mess.substitute(1, token.getDenotation());
			mess.addSourceReference(token.getSourceReference());
			expression = new TempNode.Invalid();
			expression.addSourceReference(token);
			return expression;
		}
	}

	/**
	 * 
	 * This function is responsible for parsing simple literals, which in this
	 * stage are integers, floats, strings, booleans. Note that enumerations in
	 * this stage are indistinguishable from properties, because this can only
	 * be determined from the UML model.
	 * 
	 * @return Node of appropriate type.
	 */
	TempNode parseSimpleLiteral() {

		TempNode expression = null;

		// Fetch the first token and decide how to proceed ...
		Token token = getNextLegalToken();
		switch (token.getType()) {

		case TEXT:
			// Create a TempNode.Literal<String>
			expression = new TempNode.Literal<String>(
					((Token.Text) token).getValue());
			break;

		case NUMBER:
			// Create one of TempNode.Literal<Long> or <Double>
			Token.Number numtok = (Token.Number) token;
			if (numtok.isInteger())
				expression = new TempNode.Literal<Long>(
						(long) numtok.getValue());
			else {
				expression = new TempNode.Literal<Double>(numtok.getValue());
			}
			break;

		case IDENTIFIER:
			// If we find one of true or false, create TempNode.Literal<Boolean>
			Token.Identifier idtok = (Token.Identifier) token;
			String id = idtok.getName();
			Boolean t = id.equals("true");
			Boolean f = id.equals("false");
			if (t || f) {
				expression = new TempNode.Literal<Boolean>(t);
				break;
			}
			// May be also "null", in which case we generate a
			// TempNode.Literal<Nothing>
			if (id.equals("null")) {
				expression = new TempNode.Literal<TempNode.Nothing>(
						new TempNode.Nothing());
				break;
			}

		default:
			// Function has been called with illegal token
			MessageCollection.Message mess = messages.new Message(0);
			mess.substitute(1,
					"SimpleLiteral failure at " + token.getDenotation());
			mess.addSourceReference(token.getSourceReference());
		}

		// Attach source reference ...
		if (expression != null)
			expression.addSourceReference(token);

		return expression;
	}

	/**
	 * 
	 * This is an incomplete parser for complex literals. In its final state
	 * this function shall deal with all flavors of Collections and Tuples.
	 * 
	 * 
	 * @return TempNode representing the complex literal
	 */
	TempNode parseComplexLiteral() {

		TempNode result = null;
		MessageCollection.Message mess = null;

		Token token1 = inLex.fetchToken();
		Token.Type type = token1.getType();
		SourceReference sourceRef = token1.getSourceReference();
		String name = null;
		if (type == Token.Type.IDENTIFIER) {
			name = ((Token.Identifier) token1).getName();
			// Complex literal not supported. Create a diagnostic and skip
			// a matching pair of braces.
			mess = messages.new Message(8);
			mess.substitute(1, name);
			mess.addSourceReference(sourceRef);
			(result = new TempNode.Invalid()).addSourceReference(token1);
			skipPairedBrackets(Token.Type.O_CU_BRACKET);
			return result;
		} else {
			// System error: All complex literal start with an identifier ...
			mess = messages.new Message(0);
			mess.substitute(1,
					"Complex Literal starting with " + token1.getDenotation());
			mess.addSourceReference(sourceRef);
			(result = new TempNode.Invalid()).addSourceReference(token1);
			return result;
		}
	}

	/**
	 * 
	 * This is a parser for the special literal type Date. Date literals are
	 * recognized as follows:
	 * 
	 * <ul>
	 * <li><i>Date()</i>, which represents the current date at the time of using
	 * the constraint, and
	 * <li><i>Date(iso8601)</i>, where iso8601 must be a string literal of a
	 * date according to ISO 8601. The ISO format is
	 * <i>yyyy-mm-ddThh:mm:ss.nnnZone</i>. <i>Zone</i> is a signed offset from
	 * GMT given in <i>hh:mm</i> format or the letter <i>Z</i>. The <i>nnn</i>
	 * represent fractions of a second.
	 * </ul>
	 * 
	 * @return TempNode representing the date literal
	 */
	@SuppressWarnings("unchecked")
	TempNode parseDateLiteral() {

		TempNode result = null;
		MessageCollection.Message mess = null;

		Token token1 = inLex.fetchToken();
		Token.Type type = token1.getType();
		SourceReference sourceRef = token1.getSourceReference();
		String name = null;

		if (type == Token.Type.IDENTIFIER) {
			name = ((Token.Identifier) token1).getName();
			if (!name.equals("Date")) {
				// This function does not support anything except Date(...)
				mess = messages.new Message(8);
				mess.substitute(1, name);
				mess.addSourceReference(sourceRef);
				(result = new TempNode.Invalid()).addSourceReference(token1);
				skipPairedBrackets(Token.Type.O_CU_BRACKET);
				return result;
			}
		} else {
			// System error: All complex literal start with an identifier ...
			mess = messages.new Message(0);
			mess.substitute(1,
					"Complex Literal starting with " + token1.getDenotation());
			mess.addSourceReference(sourceRef);
			(result = new TempNode.Invalid()).addSourceReference(token1);
			return result;
		}

		// Must be Date ( ... ). We recognize either a zero-parameter form,
		// which stands for the current date and time and a one-parameter form
		// containing a string in ISO8610 format.

		Token token2 = getNextLegalToken();
		if (token2.getType() != Token.Type.O_BRACKET) {
			mess = messages.new Message(9);
			mess.substitute(1, name);
			mess.addSourceReference(token2.getSourceReference());
			result = new TempNode.Invalid();
			result.addSourceReference(token1);
			inLex.unfetchToken(token2);
			return result;
		}
		Token token3 = getNextLegalToken();
		if (token3.getType() == Token.Type.C_BRACKET) {

			// *** Current date at the time of use of the expression is meant.

			result = new TempNode.Literal<TempNode.CurrentDateTime>(
					new TempNode.CurrentDateTime());
			result.addSourceReference(token1);
			result.addSourceReference(token2);
			result.addSourceReference(token3);
		} else {

			// *** Date is given as ISO8101 string.

			// First put the token back
			inLex.unfetchToken(token3);

			// Get the expression containing the string ...
			TempNode expr = parseInfix(impType);

			// Followed by a ) ?...
			token3 = getNextLegalToken();
			if (token3.getType() != Token.Type.C_BRACKET) {
				mess = messages.new Message(10);
				sourceRef = token3.getSourceReference();
				mess.substitute(1, token3.getDenotation());
				mess.addSourceReference(sourceRef);
				result = new TempNode.Invalid();
				result.addSourceReference(token1);
				result.addSourceReference(token2);
				result.addSourceReference(expr);
				inLex.unfetchToken(token3);
				return result;
			}

			// Yes, a ). The expression must be a string constant ...
			if (!(expr instanceof TempNode.Literal
					&& ((TempNode.Literal<?>) expr)
							.getValue() instanceof String)) {
				mess = messages.new Message(11);
				SourceReference[] sourceRefs = expr.getSourceReferences();
				for (int i = 0; i < sourceRefs.length; i++)
					mess.addSourceReference(sourceRefs[i]);
				result = new TempNode.Invalid();
				result.addSourceReference(token1);
				result.addSourceReference(token2);
				result.addSourceReference(expr);
				result.addSourceReference(token3);
				return result;
			}

			// Now we have to convert the string from ISO8101 to Java Calendar
			// format. We are using a regular expression ...
			String regex = "^(\\d{4})(?:-(\\d{2})(?:-(\\d{2})(?:T(\\d{2})(?::(\\d{2})(?::(\\d{2})(?:\\.(\\d+))?)?)?(?:(?:(Z)|(\\+|-)(\\d{2}):(\\d{2})))?)?)?)?$";
			// 0 00 1 11 2 22 3 33 4 44 5 55 6 65 4 3 3 4 5 5 5 55 5 5 543 2 1 0

			Pattern iso8601 = Pattern.compile(regex);
			String isodatetime = ((TempNode.Literal<String>) expr).getValue();
			Matcher matcher = iso8601.matcher(isodatetime);
			boolean matches = matcher.matches();

			// Check if the format was valid ...
			if (!matches) {
				mess = messages.new Message(11);
				SourceReference[] sourceRefs = expr.getSourceReferences();
				for (int i = 0; i < sourceRefs.length; i++)
					mess.addSourceReference(sourceRefs[i]);
				result = new TempNode.Invalid();
				result.addSourceReference(token1);
				result.addSourceReference(token2);
				result.addSourceReference(expr);
				result.addSourceReference(token3);
				return result;
			}

			// Now, nothing much can happen from here on, because the
			// pattern has made sure, we have the right syntax. Date
			// specification errors we will treat in a lenient way.

			// Fetch and convert individual fields ...
			String year = matcher.group(1);
			String month = matcher.group(2);
			String day = matcher.group(3);
			String hour = matcher.group(4);
			String minute = matcher.group(5);
			String second = matcher.group(6);
			String fract = matcher.group(7);
			boolean zulu = matcher.group(8) != null;
			String sign = matcher.group(9);
			String offhour = matcher.group(10);
			String offmin = matcher.group(11);

			TimeZone zone = null;
			if (zulu) {
				// We are using GMT ...
				zone = new SimpleTimeZone(0, "GMT+00:00");
			} else if (offhour != null) {
				// Time zone is explicitly specified ...
				int offs = Integer.parseInt(offhour) * 60;
				offs += Integer.parseInt(offmin);
				offs *= 60000; // Milliseconds ...
				if (sign.equals("-"))
					offs = -offs;
				zone = new SimpleTimeZone(offs,
						"GMT" + sign + offhour + ":" + offmin);
			} else
				// The default one is meant ...
				zone = TimeZone.getDefault();

			TempNode.DateTime cal = new TempNode.DateTime();
			cal.clear();
			cal.setTimeZone(zone);

			cal.set(Calendar.YEAR, Integer.parseInt(year));
			if (month != null)
				cal.set(Calendar.MONTH, Integer.parseInt(month) - 1);

			if (day != null)
				cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(day));
			if (hour != null)
				cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour));
			if (minute != null)
				cal.set(Calendar.MINUTE, Integer.parseInt(minute));
			if (second != null)
				cal.set(Calendar.SECOND, Integer.parseInt(second));
			if (fract != null) {
				double fr = Double.parseDouble("0." + fract);
				long milli = Math.round(fr * 1000);
				cal.set(Calendar.MILLISECOND, (int) milli);
			}

			// Finally, create the Literal object to be returned ...
			result = new TempNode.Literal<TempNode.DateTime>(cal);
			result.addSourceReference(token1);
			result.addSourceReference(token2);
			result.addSourceReference(expr);
			result.addSourceReference(token3);
		}

		return result;
	}

	/**
	 * 
	 * The parsePathName method analyzes sequences of identifiers which are
	 * separated by :: symbols. If a scoping operator :: is indeed present it
	 * indicates a path in the UML model from packages towards classes and
	 * possibly enum values. If no :: is present, the identifier can mean
	 * anything, including an OCL variable.
	 * <br><br>
	 * PathNames are evaluated left-associative. So, a PathName node consists of
	 * a context (which is null or a Identifier) and an identifier name.
	 * <br><br>
	 * In excess to the syntax described above, the implementation at hand also
	 * gives support to syntactical variations in the last step of the path. In
	 * this position, besides well-formed identifiers, also tokens of the text
	 * literal type or integer number type are permitted. The string content of
	 * these tokens forms the name of that identifier. The reason for this
	 * extended syntax is the support for enumeration references, which refer to
	 * integer numbers or any sort of character.
	 * 
	 * 
	 * @return TempNode representing the scoped identifier
	 */
	TempNode parsePathName() {

		// Initialize qualifiers.
		TempNode scopedId = null;
		Token token = null, tokenid = null;

		// Recognize identifiers (or in the last step also other selected
		// text carrying tokens) until no further qualification operator (::)
		// is seen.
		boolean first = true;
		do {
			// Fetch the next token, which is supposed to be an identifier.
			tokenid = getNextLegalToken();

			// Check, what we got, and extract the identifier name
			String idname = null;
			boolean error = false;
			if (tokenid.getType() == Token.Type.IDENTIFIER) {
				// Indeed an identifer. Extract name ..
				idname = ((Token.Identifier) tokenid).getName();
			} else if (first) {
				// Not an identifier in first position. This is an error ...
				error = true;
			} else if (inspectNextLegalToken()
					.getType() == Token.Type.DOUBLE_COLON) {
				// Not an identifier in non-last position. Error ...
				error = true;
			} else if (tokenid.getType() == Token.Type.TEXT) {
				// A text literal. The content will be the name ...
				idname = ((Token.Text) tokenid).getValue();
			} else if (tokenid.getType() == Token.Type.NUMBER) {
				// A number will be permitted if it is an integer ...
				Token.Number tn = (Token.Number) tokenid;
				if (tn.isInteger())
					idname = tn.getStringValue();
				else
					error = true;
			} else {
				// Everything else is an error ...
				error = true;
			}

			// No longer first time
			first = false;

			// If we found out this was not suited, create a diagnostic and
			// skip it.
			if (error) {
				MessageCollection.Message mess = messages.new Message(4);
				mess.substitute(1, tokenid.getDenotation());
				mess.addSourceReference(tokenid.getSourceReference());
				continue;
			}

			// Everything fine. Create an Identifier TempNode.
			TempNode lastId = scopedId;
			scopedId = new TempNode.Identifier(scopedId, idname);
			if (token != null) {
				scopedId.addSourceReference(lastId);
				scopedId.addSourceReference(token);
			}
			scopedId.addSourceReference(tokenid);
		} while ((token = getNextLegalToken())
				.getType() == Token.Type.DOUBLE_COLON);

		// So, the last token was not a scope operator (::). Put it back for
		// use by the next higher level.
		inLex.unfetchToken(token);

		// If we did not recognize anything at all, set up an Invalid node
		if (scopedId == null) {
			scopedId = new TempNode.Invalid();
			scopedId.addSourceReference(token);
		}

		// The last Identifier is the one to be returned.
		return scopedId;
	}

	// This is an auxiliary exception class for simplifying the code of
	// parseIf() and parseLet() ...
	@SuppressWarnings("serial")
	private class IllegalSyntax extends Exception {
		final String what;
		final Token found;

		IllegalSyntax(final String what, final Token found) {
			this.what = what;
			this.found = found;
		}
	}

	/**
	 * 
	 * parseIf analyzes the OCL if ... then ... else ... endif construct. We
	 * permit arbitrary expressions in each ... part of the phrase. If the
	 * proper reserved words of the sequence are not seen in their correct
	 * places the Invalid is returned.
	 * 
	 * 
	 * @return TempNode representing the if-construct.
	 */
	TempNode parseIf() {

		TempNode result = null;
		Token token1 = null;
		Token token2 = null;
		Token token3 = null;
		Token token4 = null;

		try {
			// First must be an "if" ...
			token1 = inLex.fetchToken();
			if (token1.getType() != Token.Type.IF)
				throw new IllegalSyntax("if", token1);

			// Go and get the condition ...
			TempNode condition = parseInfix(impType);

			// Next must be a "then" ...
			token2 = getNextLegalToken();
			if (token2.getType() != Token.Type.THEN)
				throw new IllegalSyntax("then", token2);

			// Recognize the "then" expression ...
			TempNode thenpart = parseInfix(impType);

			// Next must be an "else" ...
			token3 = getNextLegalToken();
			if (token3.getType() != Token.Type.ELSE)
				throw new IllegalSyntax("else", token3);

			// Recognize the "then" expression ...
			TempNode elsepart = parseInfix(impType);

			// Finally there must be an "endif" ...
			token4 = getNextLegalToken();
			if (token4.getType() != Token.Type.ENDIF)
				throw new IllegalSyntax("endif", token4);

			// Went fine. Construct the IfClause ...
			result = new TempNode.IfClause(condition, thenpart, elsepart);
			result.addSourceReference(token1);
			result.addSourceReference(token2);
			result.addSourceReference(token3);
			result.addSourceReference(token4);

		} catch (IllegalSyntax ill) {
			// This collects all sorts of "proper next keyword not found"
			// situations, which can arise from recognizing the if-construct.
			// We create a diagnostic, skip to endif (or EOD) and return an
			// Invalid TempNode.
			result = new TempNode.Invalid();
			result.addSourceReference(token1);
			MessageCollection.Message mess = messages.new Message(5);
			mess.substitute(1, ill.what);
			mess.substitute(2, ill.found.getDenotation());
			mess.addSourceReference(ill.found.getSourceReference());
			if (ill.found.getType() != Token.Type.END_OF_TEXT)
				skipToOperator(Token.Type.ENDIF);
		}

		return result;
	}

	/**
	 * 
	 * This method parses the OCL let-construct. The result is a TempNode of
	 * type LetClause, which contains the declarations and the expression these
	 * declaration apply to.
	 * 
	 * 
	 * @return TempNode representing the let construct.
	 */
	TempNode parseLet() {

		TempNode result = null;
		Token token1 = null;
		Token token2 = null;
		ArrayList<Token> tokList = new ArrayList<Token>(5);
		Token.Type type;

		try {
			// Get the "let" ...
			token1 = inLex.fetchToken();
			if (token1.getType() != Token.Type.LET)
				throw new IllegalSyntax("let", token1);

			// Now eat up the list of variable declarations ...
			ArrayList<TempNode> declList = new ArrayList<TempNode>(4);
			do {
				TempNode decl = parseVarDecl();
				if (decl != null)
					declList.add(decl);
				token2 = getNextLegalToken();
				type = token2.getType();
				if (type == Token.Type.IN)
					break;
				tokList.add(token2);
			} while (type == Token.Type.COMMA && type != Token.Type.IN);

			// The one last seen must be an 'in'. Otherwise the syntax is broken
			if (type != Token.Type.IN)
				throw new IllegalSyntax("in", token2);

			// The 'in' is present. We are parsing the expression now ...
			TempNode expression = parseInfix(impType);

			// All went fine. Construct the LetClause ...
			TempNode[] declarations = new TempNode[declList.size()];
			declarations = declList.toArray(declarations);
			result = new TempNode.LetClause(declarations, expression);
			result.addSourceReference(token1);
			result.addSourceReference(token2);
			for (Token tok : tokList)
				result.addSourceReference(tok);

		} catch (IllegalSyntax ill) {

			// Here we arrive, when the 'in' had not been found. We diagnose
			// that we give up on the 'let', and try to get back on track by
			// attempting to recognize a full expression.
			MessageCollection.Message mess = messages.new Message(7);
			mess.substitute(1, ill.what);
			mess.substitute(2, ill.found.getDenotation());
			mess.addSourceReference(ill.found.getSourceReference());

			// Try to see an expression ...
			result = parseInfix(impType);
		}

		return result;
	}

	/**
	 * 
	 * This method analyzes all sorts of argument lists, which in the general
	 * case consist of a bracketed construct containing an optional prefix part
	 * of variable declarations (aimed at iterator constructs) followed by a '|'
	 * and a list of arbitrary expressions.</i>
	 * 
	 * @return TempNode object representing the argument list. Can be null.
	 */
	TempNode parseArgumentList() {

		// Initialize
		TempNode result = null;

		// Eat the introducing bracket.
		Token token1 = inLex.fetchToken();
		if (token1.getType() != Token.Type.O_BRACKET) {
			// Not an opening bracket. This is regarded a system error
			// because this should already have been checked outside.
			MessageCollection.Message mess = messages.new Message(0);
			mess.substitute(1, "Argument list does not start with bracket.");
			mess.addSourceReference(token1.getSourceReference());
			return result;
		}

		// Recognize instances of variable declarations, if present.

		// Note that the OCL grammar here is strictly non-LL1, so we have to
		// store away all tokens and push them back in case the '|' does not
		// appear following the would-be declarations.
		Lexer.Checkpoint lexer_record = inLex.captureState();
		// We will also have to capture the state of the diagnostics.
		MessageCollection.Checkpoint mess_record = messages.captureState();

		// Parse variable declarations ...
		Token token2 = null;
		ArrayList<Token> tokList = new ArrayList<Token>(20);
		ArrayList<TempNode> vardecls = new ArrayList<TempNode>(2);
		do {
			// Parse
			TempNode vardecl = parseVarDecl();
			// Collect
			if (vardecl != null)
				vardecls.add(vardecl);
			// Next token, also record it
			token2 = getNextLegalToken();
			// Collect delimiter Tokens for proper SourceReferences
			tokList.add(token2);
		} while (token2.getType() == Token.Type.COMMA);

		// If all went well, we will have a '|' as the current token. Check
		if (token2.getType() == Token.Type.BAR) {
			// Yes, there is a bar. We will have to rid the checkpoints ...
			inLex.releaseState(lexer_record);
			messages.releaseState(mess_record);
		} else {
			// No, there is no bar. So, nothing of what we saw was real and
			// we back up to the state right after the bracket ...
			inLex.restoreState(lexer_record);
			messages.restoreState(mess_record);
			vardecls = null;
			tokList.clear();
		}

		// Recognize the argument list proper
		ArrayList<TempNode> arguments = new ArrayList<TempNode>(10);
		if ((token2 = inLex.fetchToken()).getType() != Token.Type.C_BRACKET) {
			inLex.unfetchToken(token2);
			token2 = null;
			do {
				// Collect delimiters
				if (token2 != null)
					tokList.add(token2);
				// Parse
				TempNode expression = parseInfix(impType);
				// Collect
				arguments.add(expression);
				// Next token
				token2 = getNextLegalToken();
			} while (token2.getType() == Token.Type.COMMA);
		}

		// Eat the final bracket
		if (token2.getType() != Token.Type.C_BRACKET) {
			// Not a closing bracket. Set up a diagnostic, but return, what we
			// have found so far ...
			MessageCollection.Message mess = messages.new Message(2);
			mess.substitute(1, token2.getDenotation());
			mess.addSourceReference(token2.getSourceReference());
			inLex.unfetchToken(token2);
			token2 = null;
		}

		// Create an Arguments TempNode object from what we found:
		// Convert lists to arrays ...
		TempNode[] vardeclA = null;
		if (vardecls != null) {
			vardeclA = new TempNode[vardecls.size()];
			vardeclA = vardecls.toArray(vardeclA);
		}
		TempNode[] argumentA = new TempNode[arguments.size()];
		argumentA = arguments.toArray(argumentA);
		// Create the TempNode ...
		result = new TempNode.Arguments(vardeclA, argumentA);
		// Attach all the source references ...
		result.addSourceReference(token1);
		if (token2 != null)
			result.addSourceReference(token2);
		for (Token tok : tokList)
			result.addSourceReference(tok);

		return result;
	}

	/**
	 * This method parses a variable declaration and initialization. The syntax
	 * recognized is:
	 * 
	 * <code>string [ <i>:</i> pathname ] [ <i>=</i> expression ]</code>
	 * 
	 * where the mandatory string is the identifier name, the optional pathname
	 * designates a type (full OCL provides more syntactic possibilities here)
	 * and the optional assignment provides the initial value.
	 * 
	 * 
	 * @return TempNode representing the variable declaration, may be null.
	 */
	TempNode parseVarDecl() {

		// First thing we meet must be an identifier ...
		Token identifier = getNextLegalToken();
		SourceReference idsource = identifier.getSourceReference();
		if (identifier.getType() != Token.Type.IDENTIFIER) {
			// Not an identifier token. We have reason to complain ...
			MessageCollection.Message mess = messages.new Message(13);
			mess.addSourceReference(idsource);
			mess.substitute(1, identifier.getDenotation());
			// Go back
			inLex.unfetchToken(identifier);
			return null;
		}

		// Now, 2nd item may be a ':' followed by a type. In our simplified OCL
		// grammar this can only be a pathname.
		Token token1 = getNextLegalToken();
		TempNode typename = null;
		if (token1.getType() == Token.Type.COLON) {
			// Yes, a colon. So we expect a path name ...
			typename = parsePathName();
		} else {
			// No, put it back ...
			inLex.unfetchToken(token1);
			token1 = null;
		}

		// 3rd item may be an initializer term. It is introduced by means of an
		// '=' and followed by an arbitrary expression ...
		Token token2 = getNextLegalToken();
		TempNode initializer = null;
		if (token2.getType() == Token.Type.EQUAL) {
			// Yes, an equal sign. Next must be an initializer expression ...
			initializer = this.parseInfix(impType);
		} else {
			// No, put it back ...
			inLex.unfetchToken(token2);
			token2 = null;
		}

		// Collect into a Declaration object ...
		String idname = ((Token.Identifier) identifier).getName();
		TempNode result = new TempNode.Declaration(idname, typename,
				initializer);
		result.addSourceReference(identifier);
		if (token1 != null)
			result.addSourceReference(token1);
		if (token2 != null)
			result.addSourceReference(token2);

		return result;
	}

	/**
	 * This auxiliary function is for skipping illegal tokens and comments and
	 * for automatically creating appropriate message regarding the illegal
	 * tokens. The comments are collected at the OclParser object, where they
	 * can be retrieved by the user of the parser.
	 * 
	 * @return Next legal, non-comment token
	 */
	Token getNextLegalToken() {

		// Initialize ...
		SourceReference oldSourceRef = null;
		String items = "";
		MessageCollection.Message mess = null;
		Token token = null;
		Token.Type type;

		// Loop while the next fetched token is ILLEGAL or a COMMENT ...
		boolean first = true;
		while ((type = (token = inLex.fetchToken()).type) == Token.Type.ILLEGAL
				|| type == Token.Type.COMMENT) {

			// Treat comments. They are just collected and removed
			if (type == Token.Type.COMMENT) {
				comments.add(((Token.Comment) token).getValue());
				continue;
			}

			// All of the following treats illegal tokens:

			// In the first go with an illegal one create a message.
			if (first) {
				mess = messages.new Message(1);
				first = false;
			}

			// Fetch source reference and illegal item string.
			SourceReference sourceRef = token.getSourceReference();
			String item = ((Token.Illegal) token).getItem();

			// Find out if we can merge this with the predecessor item.
			if (oldSourceRef != null
					&& oldSourceRef.canBeMerged(sourceRef, false)) {
				// Yes: Same line and columns are adjacent. Combine ...
				oldSourceRef.merge(sourceRef);
				items += item;
			} else {
				// No: Old combined reference (if there is one) has to go into
				// the message. We also append an additional space to the string
				// to indicate there had been a break.
				if (oldSourceRef != null) {
					mess.addSourceReference(oldSourceRef);
					items += " ";
				}
				// Start new aggregation with new values
				oldSourceRef = new SourceReference(sourceRef);
				items += item;
			}
		}
		// The last illegal token still needs to be appended.
		if (mess != null) {
			mess.addSourceReference(oldSourceRef);
			mess.substitute(1, items);
		}

		// Returned value is the legal, non-comment token, which broke the loop.
		return token;
	}

	/**
	 * This auxiliary function also skips illegal tokens creating appropriate
	 * messages in doing so. When it finds a legal token, it pushes it back for
	 * the next call to retrieve it again.
	 * 
	 * @return Next legal token, which is next to be fetched
	 */
	Token inspectNextLegalToken() {
		// Get the token
		Token token = getNextLegalToken();
		this.inLex.unfetchToken(token);
		// and put it back
		return token;
	}

	/**
	 * This auxiliary method skips tokens until it either finds a token of the
	 * type given or Token.Type.END_OF_TEXT. Finding the latter it creates a
	 * diagnostic and pushes the token back to the input. Comments are also
	 * skipped, but are collected.
	 * 
	 * @param type
	 *                 Type of token to be skipped to and over.
	 */
	void skipToOperator(Token.Type type) {

		// Loop until we see the requested type or END_OF_TEXT ...
		Token token;
		while ((token = inLex.fetchToken()).type != type
				&& token.type == Token.Type.COMMENT
				&& token.type != Token.Type.END_OF_TEXT) {
			if (token.type == Token.Type.COMMENT)
				// A comment: Grab it
				comments.add(((Token.Comment) token).getValue());
		}

		if (token.type == Token.Type.END_OF_TEXT) {
			// Diagnose EOT encountered
			MessageCollection.Message mess = messages.new Message(6);
			SourceReference sourceRef = token.getSourceReference();
			mess.addSourceReference(sourceRef);
			// Push EOT back to the token stream
			inLex.unfetchToken(token);
		}
	}

	// Auxiliary Map for bracket counting ...
	static HashMap<Token.Type, Integer> pairedBrackets = new HashMap<Token.Type, Integer>();
	static {
		pairedBrackets.put(Token.Type.O_BRACKET, 1);
		pairedBrackets.put(Token.Type.C_BRACKET, -1);
		pairedBrackets.put(Token.Type.O_SQ_BRACKET, 2);
		pairedBrackets.put(Token.Type.C_SQ_BRACKET, -2);
		pairedBrackets.put(Token.Type.O_CU_BRACKET, 3);
		pairedBrackets.put(Token.Type.C_CU_BRACKET, -3);
	}

	/**
	 * This auxiliary method skips tokens until it finds a bracket pairing to
	 * the open one supplied as a parameter. All nested brackets are also
	 * checked for pairedness. Finding END_OF_TEXT instead creates a diagnostic.
	 * 
	 * @param type
	 *                 Type of opening bracket to be matched.
	 */
	void skipPairedBrackets(Token.Type type) {

		// First item must be a bracket of the indicated type. If it isn't
		// just quit after putting the token back ...
		Token token = getNextLegalToken();
		if (token.getType() != type) {
			this.inLex.unfetchToken(token);
			return;
		}

		// Initialization
		int[] count = { 0, 0, 0, 0 };

		// Count and loop until count is back to zero ...
		do {
			// Update bracket flavor count
			Integer addind = pairedBrackets.get(type);
			if (addind != null) {
				if (addind > 0)
					count[addind] += 1;
				else
					count[-addind] -= 1;
			}
			// Done if all flavors are back to zero
			if (count[1] == 0 && count[2] == 0 && count[3] == 0)
				break;
			// Proceed to next token
			token = getNextLegalToken();
			type = token.getType();
		} while (type != Token.Type.END_OF_TEXT);

		// Emit a diagnostic if we ran into EOT ...
		if (token.type == Token.Type.END_OF_TEXT) {
			// Diagnose EOT encountered
			MessageCollection.Message mess = messages.new Message(12);
			SourceReference sourceRef = token.getSourceReference();
			mess.addSourceReference(sourceRef);
			// Push EOT back to the token stream
			inLex.unfetchToken(token);
		}
	}
}