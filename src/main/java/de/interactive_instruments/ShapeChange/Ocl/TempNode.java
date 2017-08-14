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
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.SortedSet;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.OclConstraint;
import de.interactive_instruments.ShapeChange.Model.OperationInfo;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Ocl.OclNode.BuiltInType;
import de.interactive_instruments.ShapeChange.Ocl.OclNode.DataType;
import de.interactive_instruments.ShapeChange.Ocl.OclNode.MultiplicityMapping;
import de.interactive_instruments.ShapeChange.Ocl.OclNode.PropertyCallExp;
import de.interactive_instruments.ShapeChange.Ocl.OclNode.VariableExp;

/**
 * <p>
 * TempNodes are the constituents of a first level syntax tree for the supported
 * subset of OCL. They are constructed from the syntactic structure of an OCL
 * expression alone, without referring to its context, which is given by the UML
 * class the expression belongs to and its UML environment.
 * </p>
 * <p>
 * Tempnode itself is abstract and comes in 10 concrete flavors, namely:
 * </p>
 * <ul>
 * <li>Invalid - representing an invalid piece of syntax,
 * <li>Infix - standing for all infix operations,
 * <li>Prefix - representing a not or -,
 * <li>Property - standing for an attribute or operation call
 * <li>Identifier - denoting scoped or simple identifiers
 * <li>Declaration - representing declaration and initialization of a variable,
 * <li>Literal<T> - representing a constant of some type,
 * <li>IfClause - combining the three components of an <i>if ... then ... else
 * ... endif</i>,
 * <li>LetClause - combining Declarations and expressions of <i>let ... in
 * ...</i>,
 * <li>Arguments - comprising iterator variable Declarations (optionally) and
 * operation call arguments.
 * </ul>
 * <p>
 * TempNodes carry an array of SourceReferences which bundles and aggregates the
 * full scope of each TempNode. Each input line is represented by a separate
 * SourceReference.
 * 
 * @version 0.1
 * @author Reinhard Erstling (c) interactive instruments GmbH, Bonn, Germany
 */

abstract class TempNode {

	protected SourceReference[] sourceReferences = null;

	/**
	 * <p>
	 * The SourceReferences attached to a TempNode object are returned. They
	 * come as an array of SourceReference objects, which is ordered by line
	 * number and token serial number.
	 * </p>
	 * 
	 * @return SourceReference array attached to TempNode.
	 */
	SourceReference[] getSourceReferences() {
		return sourceReferences;
	}

	/**
	 * <p>
	 * This function adds a SourceReference object to the SourceReferences of a
	 * TempNode.
	 * </p>
	 * 
	 * @param sourceRef
	 *            SourceReference object to be added.
	 */
	void addSourceReference(SourceReference sourceRef) {
		if (sourceReferences == null) {
			// First one. Create the array.
			sourceReferences = new SourceReference[1];
			sourceReferences[0] = sourceRef;
		} else {
			// Other. Merge with what is already present.
			sourceReferences = sourceRef.merge(sourceReferences, true);
		}
	}

	/**
	 * <p>
	 * This is a convenience function to add the SourceReference of a Token to
	 * the SourceReferences of a TempNode.
	 * </p>
	 * 
	 * @param token
	 *            Token, the SourceReference of which is to be added.
	 */
	void addSourceReference(Token token) {
		SourceReference sr = token.getSourceReference();
		if (sr != null)
			addSourceReference(sr);
	}

	/**
	 * <p>
	 * This convenience function adds all SourceReference of the argument
	 * TempNode to the SourceReference of this TempNode.
	 * </p>
	 * 
	 * @param tn
	 *            TempNode the SourceReferences of which are to be added.
	 */
	void addSourceReference(TempNode tn) {
		// Make sure all arrays are properly in place ...
		if (tn.sourceReferences == null)
			return;
		if (sourceReferences == null)
			sourceReferences = new SourceReference[0];
		// Pass to static SourceReference merger ...
		sourceReferences = SourceReference.merge(sourceReferences,
				tn.sourceReferences, true);
	}

	/**
	 * <p>
	 * The abstract method debugPrint outputs the content of a TempNode for the
	 * purpose of debugging this software.
	 * </p>
	 * 
	 * @param stream
	 *            PrintWriter onto which the debug output is to be directed.
	 */
	abstract void debugPrint(PrintWriter stream);

	/**
	 * <p>
	 * This defines an abstract standard method for model assignment to any of
	 * the TempNodes.
	 * 
	 * @param p
	 *            parser object
	 * @param model
	 *            the UML model
	 * @param varctx
	 *            the variable declaration context
	 * @return generated OclNode
	 */
	abstract OclNode connectToModel(OclParser p, Model model,
			OclNode.Declaration varctx);

	/**
	 * The class Expression stands for a full blown OCL expression, representing
	 * its type (such as inv:, init:, ...), its name and the TempNode syntax
	 * tree itself.
	 */
	static class Expression extends TempNode {

		ClassInfo classContext = null;
		Info generalContext = null;
		Token.Type expressionType;
		String expressionName;
		TempNode expression;

		/**
		 * This constructs an Expression from its constituents.
		 * 
		 * @param type
		 *            The TokenType of the introducing expression keyword such
		 *            as <i>inv</i>.
		 * @param name
		 *            The name of the constraint if given
		 * @param expr
		 *            The TempNode representing the value of the expression.
		 */
		Expression(Token.Type type, String name, TempNode expr) {
			this.expressionType = type;
			this.expressionName = name;
			this.expression = expr;
		}

		/**
		 * <p>
		 * This connects a TempNode.Expression to the model. Before delegating
		 * to the standard interface connectToModel() the function checks,
		 * whether the a correct context is available.
		 * </p>
		 * 
		 * @param p
		 *            Parser object
		 * @param ctx
		 *            Context of the expression (class or property)
		 * @return
		 */
		OclNode.Expression connectToModelWithContext(OclParser p, Info ctx) {

			// Check if context is set
			if (ctx == null) {
				MessageCollection.Message mess = p
						.getMessageCollection().new Message(0);
				mess.substitute(1,
						"Cannot connect Expression without class context.");
				mess.addSourceReferences(sourceReferences);
				return null;
			}

			// Establish the class and model context
			classContext = null;
			if (ctx instanceof ClassInfo) {
				classContext = (ClassInfo) ctx;
			} else if (ctx instanceof PropertyInfo) {
				classContext = ((PropertyInfo) ctx).inClass();
			} else if (ctx instanceof OperationInfo) {
				// TODO To-class function currently not available
				classContext = null;
			}
			Model model = classContext.model();
			generalContext = ctx;

			// Pass to general connect function
			OclNode res = connectToModel(p, model, null);
			if (res != null)
				return (OclNode.Expression) res;
			else
				return null;
		}

		/**
		 * <p>
		 * This connects a TempNode.Expression to the UML model. This includes
		 * the checks necessary to ensure that correct data types are delivered.
		 * The method also makes sure that a correct implicit declaration for
		 * <i>self</i> is established before proceeding, including additional
		 * environment variable declarations, which constitute an extension to
		 * this implementation of OCL.
		 * </p>
		 * <p>
		 * Note that all this requires that at least a class context is
		 * established from the caller side.
		 * </p>
		 * 
		 * @param p
		 *            parser object
		 * @param model
		 *            the UML model
		 * @param varctx
		 *            the variable declaration context
		 * @return The OclNode.OclExpression or null
		 */
		OclNode connectToModel(OclParser p, Model model,
				OclNode.Declaration varctx) {

			// Assign the OclExpressionType
			OclConstraint.ConditionType et = null;
			if (expressionType == Token.Type.INV)
				et = OclConstraint.ConditionType.INVARIANT;
			else if (expressionType == Token.Type.INIT)
				et = OclConstraint.ConditionType.INITIAL;
			else if (expressionType == Token.Type.DERIVE)
				et = OclConstraint.ConditionType.DERIVE;
			else {
				MessageCollection.Message mess = p
						.getMessageCollection().new Message(0);
				mess.substitute(1, "Unsupported OCL expression type '"
						+ expressionType.name() + "'.");
				mess.addSourceReferences(sourceReferences);
				return null;
			}

			// If OclExpressionType requires a Property context, check it's
			// there ...
			if (et == OclConstraint.ConditionType.DERIVE
					|| et == OclConstraint.ConditionType.INITIAL) {
				if (generalContext == null) {
					MessageCollection.Message mess = p
							.getMessageCollection().new Message(0);
					mess.substitute(1,
							"Cannot connect init/derive expression without "
									+ "Property context.");
					mess.addSourceReferences(sourceReferences);
					return null;
				}
			}

			// If there are environment variable declarations present, establish
			// them in the outer scope of 'self'.
			OclNode.Declaration last = null;
			for (OclNode.Declaration decl : p.environmentDeclarations) {
				decl.nextOuter = last;
				last = decl;
			}

			// Create an implicit Declaration for 'self'.
			DataType type = new DataType(classContext);
			OclNode.Declaration dcl = new OclNode.Declaration("self", type,
					null, last, null, true);

			// Connect the expression
			OclNode exp = expression.connectToModel(p, model, dcl);
			if (exp == null)
				return null;
			sourceReferences = expression.sourceReferences;

			// Create the OclNode.OclExpression and link to self and environment
			OclNode.Expression ex = new OclNode.Expression(expressionName, et,
					exp, dcl, p.environmentDeclarations);
			dcl.ownerNode = ex;
			for (OclNode.Declaration decl : p.environmentDeclarations)
				decl.ownerNode = ex;

			// Check its type and multiplicity
			DataType extype = ex.getDataType();
			boolean isMult = ex.isMultiple();
			if (et == OclConstraint.ConditionType.INVARIANT) {
				if (extype.builtInType != BuiltInType.BOOLEAN) {
					MessageCollection.Message mess = p
							.getMessageCollection().new Message(41);
					mess.substitute(1, extype.name);
					mess.addSourceReferences(sourceReferences);
					return null;
				} else if (isMult) {
					MessageCollection.Message mess = p
							.getMessageCollection().new Message(45);
					mess.addSourceReferences(sourceReferences);
					return null;
				}
			} else {
				PropertyInfo pi = (PropertyInfo) generalContext;
				boolean pIsMult = pi.cardinality().maxOccurs > 1;
				de.interactive_instruments.ShapeChange.Type ptype = pi
						.typeInfo();
				ClassInfo ci = model.classById(ptype.id);
				if (ci == null)
					ci = model.classByName(ptype.name);
				DataType ttype = null;
				if (ci != null)
					ttype = new DataType(ci);
				else
					ttype = new DataType(ptype.name);
				if (!extype.isSubTypeOf(ttype)) {
					MessageCollection.Message mess = p
							.getMessageCollection().new Message(42);
					mess.substitute(1, ttype.name);
					mess.substitute(2, extype.name);
					mess.addSourceReferences(sourceReferences);
					return null;
				} else if (isMult && !pIsMult) {
					MessageCollection.Message mess = p
							.getMessageCollection().new Message(46);
					mess.addSourceReferences(sourceReferences);
					return null;
				}
			}

			// All checks made, return
			return ex;
		}

		// Implementation of debugPrint in this class.
		void debugPrint(PrintWriter stream) { /** TODO not public */
			String opsym = Token.getLexicalStringFromType(expressionType);
			stream.print(opsym);
			stream.print(": ");
			if (expressionName != null) {
				stream.print(expressionName);
				stream.print(" ");
			}
			expression.debugPrint(stream);
		}
	}

	/**
	 * <p>
	 * Invalid objects stand for a primary item which should be present in the
	 * syntax, but isn't. It shall be treated as special constant leading to a
	 * system error, if compiled.
	 * </p>
	 */
	static class Invalid extends TempNode {

		/**
		 * This constructs an Invalid node.
		 */
		Invalid() {
			// Nothing to do ...
		}

		/**
		 * The attempt to connect TempNode.Invalid to the model results in a
		 * system error. TempNode syntax trees <b>shall not</b> be connected to
		 * the model if they contain Invalid nodes.
		 * 
		 * @param p
		 *            parser object
		 * @param model
		 *            the UML model
		 * @param varctx
		 *            the variable declaration context
		 * @return Always null.
		 */
		OclNode connectToModel(OclParser p, Model model,
				OclNode.Declaration varctx) {
			// This will be punished with a system error.
			MessageCollection.Message mess = p
					.getMessageCollection().new Message(0);
			mess.substitute(1,
					"Attempt to connect INVALID TempNode to the model.");
			mess.addSourceReferences(sourceReferences);
			return null;
		}

		// Implementation of debugPrint in this class.
		void debugPrint(PrintWriter stream) {
			stream.print("INVALID");
		}
	}

	/**
	 * <p>
	 * Infix objects represent all sorts of infix operations in all binding
	 * levels: implies - logical - identity - comparison - addition -
	 * multiplication - referencing. TempNode.Infix objects carry the operation
	 * token type and references to both operands.
	 * </p>
	 */
	static class Infix extends TempNode {

		Token.Type type;
		TempNode operand1;
		TempNode operand2;

		/**
		 * <p>
		 * This constructs a TempNode.Infix out of its constituents. The
		 * SourceReferences of both operands are automatically included.
		 * </p>
		 * 
		 * @param type
		 *            Token type of infix operation.
		 * @param op1
		 *            TempNode reference to left operand
		 * @param op2
		 *            TempNode reference to right operand
		 */
		Infix(Token.Type type, TempNode op1, TempNode op2) {
			// Take the values
			this.type = type;
			this.operand1 = op1;
			this.operand2 = op2;
			// Add the operand's SourceReferences
			addSourceReference(op1);
			addSourceReference(op2);
		}

		/**
		 * <p>
		 * This implements the abstract standard method for model assignment of
		 * infix operations.
		 * </p>
		 * <p>
		 * ARROW (->) and DOT (.) are translated to OclNode.PropertyExp objects
		 * of the proper kind (AttributeExp, OperationExp, IteratorExp)
		 * considering the classes from the UML model.
		 * </p>
		 * <p>
		 * All other Infix operations will be matched against the table of
		 * defined operations and are transformed to OclNode.OperationExp
		 * objects, which contain the first operand as the object and the second
		 * as the only argument.
		 * </p>
		 * 
		 * @param p
		 *            parser object
		 * @param model
		 *            the UML model
		 * @param varctx
		 *            the variable declaration context
		 * @return generated OclNode for this infix operation.
		 */
		OclNode connectToModel(OclParser p, Model model,
				OclNode.Declaration varctx) {

			// Since all infix operations are left associative we can first
			// simply evaluate the first branch.
			OclNode object = operand1.connectToModel(p, model, varctx);
			if (object == null)
				return null;
			DataType objDataType = object.getDataType();
			OclNode result = null;

			// Take cases on the various operation symbols
			switch (type) {

			case ARROW:
			case DOT:
				// Referencing operations
				// Second branch must be a Property - make sure it is.
				if (!(operand2 instanceof Property)) {
					// Second operator is not an identifier. Quit.
					MessageCollection.Message mess = p
							.getMessageCollection().new Message(25);
					mess.substitute(1, Token.getLexicalStringFromType(type));
					mess.addSourceReferences(operand2.sourceReferences);
					return null;
				}
				// So, we can safely downcast to a property ...
				Property property = (Property) operand2;
				// The rest is being done by the Property, however, dependent
				// on the nature of the first operand.
				ClassInfo ci = objDataType.umlClass;
				if (ci != null && type != Token.Type.ARROW) {
					// A property on the UML model ...
					result = property.connectAsUmlProperty(p, model, varctx,
							object, false);
				} else {
					// Otherwise make attempt to find a built-in ...
					result = property.connectAsBuiltInProperty(p, model, varctx,
							object, type == Token.Type.ARROW, false);
				}
				break;

			default:
				// All other Infix operations ...
				// First step is to connect both operands to the model
				OclNode op1 = object;
				OclNode op2 = operand2.connectToModel(p, model, varctx);
				if (op1 == null || op2 == null)
					return null;
				// The DataTypes of both operands
				OclNode.DataType dt1 = objDataType;
				OclNode.DataType dt2 = op2.getDataType();
				OclNode.BuiltInType bit1 = dt1.builtInType;
				OclNode.BuiltInType bit2 = dt2.builtInType;
				// The operation name ...
				String name = Token.getLexicalStringFromType(type);
				// Now match against the table of defined operations and
				// transform to OclNode.OperationExp objects, which contain the
				// first operand as the object and the second as the only
				// argument.
				for (OclNode.BuiltInDescr bid : OclNode.operSymbDescriptors) {
					// Check name ...
					if (!name.equals(bid.name))
						continue;
					// Check type of object ...
					OclNode.BuiltInType bidtype = bid.applType;
					if (bidtype != OclNode.BuiltInType.ANY) {
						if (bidtype != bit1)
							continue;
					}
					// Check number of arguments (must be 1)...
					int bidnargs = 0;
					if (bid.arguTypes != null)
						bidnargs = bid.arguTypes.length;
					if (bidnargs != 1)
						continue;
					// Check type of argument
					if (bid.arguTypes[0] != OclNode.BuiltInType.ANY) {
						if (bid.arguTypes[0] != bit2)
							continue;
					}
					// O.k., we found it. Create and return the OperationCallExp
					result = new OclNode.OperationCallExp(op1, false, name,
							new OclNode[] { op2 }, new DataType(bid.resType),
							MultiplicityMapping.ONE2ONE, false);
					break;
				}
				if (result == null) {
					// Infix operation not supported between the given types
					MessageCollection.Message mess = p
							.getMessageCollection().new Message(43);
					mess.substitute(1, name);
					mess.substitute(2, dt1.name);
					mess.substitute(3, dt2.name);
					mess.addSourceReferences(operand2.sourceReferences);
				}
			}
			return result;
		}

		// Implementation of debugPrint in this class.
		void debugPrint(PrintWriter stream) {
			String opsym = Token.getLexicalStringFromType(type);
			stream.print(opsym);
			stream.print("(");
			operand1.debugPrint(stream);
			stream.print(",");
			operand2.debugPrint(stream);
			stream.print(")");
		}
	}

	/**
	 * <p>
	 * Prefix objects represent the prefix operations MINUS (-) and NOT (not).
	 * They carry the operation token type and and reference the single operand.
	 * </p>
	 */
	static class Prefix extends TempNode {

		Token.Type type;
		TempNode operand;

		/**
		 * <p>
		 * This constructs a TempNode.Prefix out of its constituents. The
		 * SourceReferences of the operand are automatically included.
		 * </p>
		 * 
		 * @param type
		 *            Token type of prefix operation (MINUS or NOT)
		 * @param op
		 *            TempNode reference of operand
		 */
		Prefix(Token.Type type, TempNode op) {
			// The values
			this.type = type;
			this.operand = op;
			// Add operand SourceReferences
			addSourceReference(op);
		}

		/**
		 * <p>
		 * This implements the abstract standard method for model assignment of
		 * prefix operations.
		 * </p>
		 * <p>
		 * Prefix operations will be matched against the table of defined
		 * operations and are transformed to OclNode.OperationExp objects, which
		 * contain the operand as the object.
		 * </p>
		 * 
		 * @param p
		 *            parser object
		 * @param model
		 *            the UML model
		 * @param varctx
		 *            the variable declaration context
		 * @return generated OclNode for this prefix operation.
		 */
		OclNode connectToModel(OclParser p, Model model,
				OclNode.Declaration varctx) {

			// We first evaluate the operand
			OclNode object = operand.connectToModel(p, model, varctx);
			if (object == null)
				return null;
			DataType objDataType = object.getDataType();
			OclNode.BuiltInType bit = objDataType.builtInType;
			OclNode result = null;
			// The operation name ...
			String name = Token.getLexicalStringFromType(type);

			// Now match against the table of defined operations ...
			for (OclNode.BuiltInDescr bid : OclNode.operSymbDescriptors) {
				// Check name ...
				if (!name.equals(bid.name))
					continue;
				// Check type of object ...
				OclNode.BuiltInType bidtype = bid.applType;
				if (bidtype != OclNode.BuiltInType.ANY) {
					if (bidtype != bit)
						continue;
				}
				// Check number of arguments (must be 1)...
				int bidnargs = 0;
				if (bid.arguTypes != null)
					bidnargs = bid.arguTypes.length;
				if (bidnargs != 0)
					continue;
				// O.k., we found it. Create and return the OperationCallExp
				result = new OclNode.OperationCallExp(object, false, name,
						new OclNode[] {}, new DataType(bid.resType),
						MultiplicityMapping.ONE2ONE, false);
				break;
			}

			// Something found?
			if (result == null) {
				// Prefix operation not supported on given type
				MessageCollection.Message mess = p
						.getMessageCollection().new Message(44);
				mess.substitute(1, name);
				mess.substitute(2, objDataType.name);
				mess.addSourceReferences(operand.sourceReferences);
			}

			return result;
		}

		// Implementation of debugPrint in this class.
		void debugPrint(PrintWriter stream) {
			String opsym = Token.getLexicalStringFromType(type);
			stream.print(opsym);
			stream.print("(");
			operand.debugPrint(stream);
			stream.print(")");
		}
	}

	/**
	 * <p>
	 * Property objects stand for all kinds of references using identifiers.
	 * Reference can be either into the model or to OCL defined variables - this
	 * cannot be distinguished in this stage of processing.
	 * </p>
	 * <p>
	 * Property objects can also stand for references to operation invocations,
	 * in which case an additional argument list object is given in addition to
	 * the name object.
	 * </p>
	 */
	static class Property extends TempNode {

		TempNode name;
		TempNode arguments;

		/**
		 * <p>
		 * This constructs a TempNode.Property, which represents a name
		 * (possibly qualified) and optionally an argument list. All
		 * SourceReferences of name and arguments are automatically added to the
		 * Property object.
		 * </p>
		 * 
		 * @param name
		 *            Name node, may be qualified
		 * @param arguments
		 *            Argument node, may be null
		 */
		Property(TempNode name, TempNode arguments) {
			// Constituents
			this.name = name;
			this.arguments = arguments;
			// SourceReferences
			addSourceReference(name);
			if (arguments != null)
				addSourceReference(arguments);
		}

		/**
		 * Determine whether the Property has a simple, unqualified name.
		 * 
		 * @return flag Property-has-simple-name
		 */
		boolean hasSimpleName() {
			if (name == null)
				return false;
			if (!(name instanceof Identifier))
				return false;
			Identifier id = (Identifier) name;
			if (id.name != null)
				return false;
			return true;
		}

		/**
		 * Determine the name of a Property.
		 * 
		 * @return Name of the Property
		 */
		String name() {
			if (name == null)
				return "(null)";
			if (!(name instanceof Identifier))
				return "(invalid)";
			Identifier id = (Identifier) name;
			return id.name();
		}

		/**
		 * Determine the number of arguments of a property. Properties, which do
		 * not possess an argument list (attributes/roles), return -1.
		 * 
		 * @return Number of arguments or -1
		 */
		int numberOfArguments() {
			if (arguments == null || !(arguments instanceof Arguments))
				return -1;
			return ((Arguments) arguments).expressions.length;
		}

		/**
		 * <p>
		 * This method connects this Property to the model in a stand-alone way.
		 * This means that there is no explicit object available on which the
		 * Property acts as a selector.
		 * </p>
		 * <p>
		 * There are various outputs this can generate:
		 * </p>
		 * <ul>
		 * <li>Without an argument list and with an non-qualified identifier
		 * this may explicitly represent some in-scope variable such as
		 * <i>self</i>.
		 * <li>It may be a property (attribute or operation) on some implicit
		 * variable (such as <i>self</i>) in scope.
		 * <li>It may represent a ClassLiteral.
		 * <li>It may represent a reference to an enumerated value.
		 * </ul>
		 */
		OclNode connectToModel(OclParser p, Model model,
				OclNode.Declaration varctx) {

			// Some security checks and some convenience handles
			if (name == null || !(name instanceof Identifier)) {
				// It is a system error if this is not an Identifier token
				MessageCollection.Message mess = p
						.getMessageCollection().new Message(0);
				mess.substitute(1,
						"Property is named by non-Identifier token.");
				mess.addSourceReferences(sourceReferences);
				return null;
			}
			Identifier id = (Identifier) name;
			OclNode.Declaration dcl = null;

			// First we check, if this is a variable reference. In order to be
			// a variable reference, the name needs to be simple and there must
			// be no argument lists and the name needs to be found in the
			// context of variables found in the argument 'varctx'.
			if (id.isSimpleName() && arguments == null) {
				String name = id.name();
				for (dcl = varctx; dcl != null; dcl = dcl.nextOuter)
					if (name.equals(dcl.name))
						break;
				if (dcl != null) {
					VariableExp var = new VariableExp(dcl);
					return var;
				}
			}

			// So, this is not a variable. Next question to answer is: Is there
			// an implicit variable context in scope on which this might be a
			// valid property?
			for (dcl = varctx; dcl != null; dcl = dcl.nextOuter) {
				// Skip explicit ones ...
				if (!dcl.isImplicit)
					continue;
				// Temporarily create a VariableExp ...
				VariableExp var = new VariableExp(dcl);
				// Give it a try using the implicit variable as an object
				DataType type = dcl.getDataType();
				ClassInfo ci = type.umlClass;
				OclNode result = null;
				if (ci != null) {
					// A property on the UML model ...
					result = connectAsUmlProperty(p, model, varctx, var, true);
				} else {
					// Otherwise make attempt to find a built-in ...
					result = connectAsBuiltInProperty(p, model, varctx, var,
							false, true);
				}
				// Once we are successful, we mark this implicit and return the
				// result.
				if (result != null) {
					if (result instanceof PropertyCallExp)
						((PropertyCallExp) result).isImplicit = true;
					return result;
				}
			}

			// Nothing of that kind found. Concerning an operation call (when
			// we see arguments) we have now run out of options and will
			// generate a diagnostic.
			if (arguments != null) {
				MessageCollection.Message mess = p
						.getMessageCollection().new Message(32);
				mess.substitute(1, name());
				mess.substitute(2, String.valueOf(numberOfArguments()));
				mess.addSourceReferences(sourceReferences);
				return null;
			}

			// The Property may still represent a Class literal (a class
			// constant) or an enumerated UML type.
			OclNode result = id.connectToModel(p, model, varctx);
			if (result == null) {
				// Tried everything and no interpretation of the the Property
				// found. Generate a final diagnostic.
				MessageCollection.Message mess = p
						.getMessageCollection().new Message(34);
				mess.substitute(1, name());
				mess.addSourceReferences(sourceReferences);
			}

			return result;
		}

		/**
		 * <p>
		 * The method connects this Property as a selector to some given
		 * OclNode, representing a UML class, creating an appropriate
		 * OclNode.PropertyCallExp. If the Property cannot be connected to the
		 * model, null is returned. The method also checks for UML operations
		 * and OCL-builtins which apply to UML defined classes.
		 * </p>
		 * 
		 * @param p
		 *            Parser object for accessing the diagnostics stream
		 * @param model
		 *            The UML model object
		 * @param varctx
		 *            Variable declaration context
		 * @param object
		 *            OclNode object the property is supposed to act on
		 * @param quiet
		 *            Do not generate diagnostics
		 * @return generated OclNode.PropertyCallExpr or null in case of error
		 */
		OclNode connectAsUmlProperty(OclParser p, Model model,
				OclNode.Declaration varctx, OclNode object, boolean quiet) {

			// Get the UML object class we are working on
			ClassInfo ci = object.getDataType().umlClass;
			if (ci == null) {
				MessageCollection.Message mess = p
						.getMessageCollection().new Message(0);
				mess.substitute(1,
						"connectAsUmlProperty failed due to null object.");
				mess.addSourceReferences(sourceReferences);
				return null;
			}

			// Have a look at the identifier. If it is qualified obtain the
			// ClassInfo object the qualification points to. We then also
			// have to make sure this is compatible with the object we are
			// working on.
			Identifier id = (Identifier) name;
			if (!id.isSimpleName()) {
				ClassInfo qci = id.classFromQualification(p, model, quiet);
				if (qci == null)
					return null;
				ClassInfo c;
				for (c = ci; c != null && c != qci; c = c.baseClass())
					;
				if (c == null) {
					// Not compatible. Complain.
					if (!quiet) {
						MessageCollection.Message mess = p
								.getMessageCollection().new Message(30);
						mess.substitute(1, name());
						mess.substitute(2, ci.name());
						mess.addSourceReferences(sourceReferences);
					}
					return null;
				}
				ci = qci;
			}

			// Find out whether this is meant to be an attribute or an
			// operation, take cases accordingly
			if (arguments == null) {
				// Must be attribute/role in this class or some base class
				PropertyInfo pi = ci.property(name());
				if (pi != null)
					return new OclNode.AttributeCallExp(object, pi, false);
				// So, we missed it. Say so ...
				if (!quiet) {
					MessageCollection.Message mess = p
							.getMessageCollection().new Message(26);
					mess.substitute(1, name());
					mess.substitute(2, ci.name());
					mess.addSourceReferences(id.sourceReferences);
				}
			} else {
				// Must be operation
				// Prepare list of argument types to compare to
				Arguments args = (Arguments) arguments;
				if (args.declarations == null) {
					TempNode[] parms = args.expressions;
					OclNode[] oclparms = new OclNode[parms.length];
					String[] types = new String[parms.length + 1];
					for (int i = 0; i < parms.length; i++) {
						// Compile
						oclparms[i] = parms[i].connectToModel(p, model, varctx);
						if (oclparms[i] == null)
							return null;
						// Get type string
						types[i] = oclparms[i].getDataType().name;
					}
					types[parms.length] = "*"; // Not interested in return type
					// Search in model
					OperationInfo oi = ci.operation(name(), types);
					// On success, this is it ..
					if (oi != null)
						return new OclNode.OperationCallExp(object, oi,
								oclparms, false);
				}
				// If there is no success with UML-defined operations it might
				// hit one of the OCL-added functions applicable to any type.
				OclNode res = connectAsBuiltInProperty(p, model, varctx, object,
						false, quiet);
				if (res != null)
					return res;
				// Error message has already been performed by above method.
			}

			return null;
		}

		/**
		 * <p>
		 * This connects the Property as a selector to some given OclNode, the
		 * selector representing some operation from the implemented subset of
		 * operations from the OCL specification.
		 * </p>
		 * 
		 * @param p
		 *            Parser object for accessing the diagnostics stream
		 * @param model
		 *            The UML model object
		 * @param varctx
		 *            Variable declaration context
		 * @param object
		 *            OclNode object the property is supposed to act on
		 * @param arrow
		 *            Flag: set operation selected (->)
		 * @param quiet
		 *            Do not generate diagnostics
		 * @return generated OclNode.PropertyCallExpr or null in case of error
		 */
		OclNode connectAsBuiltInProperty(OclParser p, Model model,
				OclNode.Declaration varctx, OclNode object, boolean arrow,
				boolean quiet) {

			// Get type info of object
			OclNode.DataType type = object.getDataType();
			OclNode.BuiltInType bit = type.builtInType;
			String typename = type.name;

			// Get identifier and its name
			Identifier id = (Identifier) name;
			String name = id.name();

			// Reject attribute calls. All built-ins are operations.
			if (arguments == null) {
				if (!quiet) {
					MessageCollection.Message mess = p
							.getMessageCollection().new Message(26);
					mess.substitute(1, name);
					mess.substitute(2, typename);
					mess.addSourceReferences(id.sourceReferences);
				}
				return null;
			}

			// We have to make a first go to divide normal functions from
			// iterators. This is necessary because the argument of iterators
			// needs to be compiled in a different variable context.
			Arguments args = (Arguments) arguments;
			int nargs = args.expressions.length;
			ArrayList<OclNode.BuiltInDescr> bids = new ArrayList<OclNode.BuiltInDescr>(
					1);
			int itersFound = 0;
			for (OclNode.BuiltInDescr bid : OclNode.builtInDescriptors) {
				// Check name ...
				if (!name.equals(bid.name))
					continue;
				// Check type of object ...
				OclNode.BuiltInType bidtype = bid.applType;
				if (bidtype != OclNode.BuiltInType.ANY) {
					if (bidtype != bit)
						continue;
				}
				// Check number of arguments ...
				int bidnargs = 0;
				if (bid.arguTypes != null)
					bidnargs = bid.arguTypes.length;
				if (nargs != bidnargs)
					continue;
				// Check on set vs. object operation ...
				if (arrow != bid.arrow)
					continue;
				// O.k., we found a candidate. Add it to the list
				bids.add(bid);
				// Count iterators
				if (bid.noOfDecls > 0)
					itersFound++;
			}

			// If there was no success, try the additional operations declared
			// against the parser object.
			boolean isAddedOper = false;
			OclNode.DataType addedResultType = null;
			if (bids.size() == 0) {
				// Fetch the set of additional operations
				ArrayList<OclParser.AddedOperationSignature> aolist = p.additionalOperations;
				// Try them
				for (OclParser.AddedOperationSignature ao : aolist) {
					// Check against added operation signature
					if (!name.equals(ao.name))
						continue;
					if (ao.objType != null)
						if (!type.isSubTypeOf(ao.objType))
							continue;
					if (ao.noOfArgs >= 0)
						if (ao.noOfArgs != nargs)
							continue;
					// Current instance is subsumed
					addedResultType = ao.retType;
					isAddedOper = true;
					break;
				}
			}

			// If nothing remained, we have to complain.
			if (!isAddedOper && bids.size() == 0) {
				if (!quiet) {
					MessageCollection.Message mess = p
							.getMessageCollection().new Message(27);
					mess.substitute(1, name);
					mess.substitute(2, typename);
					mess.substitute(3, String.valueOf(nargs));
					mess.addSourceReferences(sourceReferences);
				}
				return null;
			}

			// Check if we have a proper decision for either iterators or normal
			// operations. It must be possible to decide in this place, so if
			// it does not work it is a system error.
			if (itersFound > 0 && bids.size() != itersFound) {
				MessageCollection.Message mess = p
						.getMessageCollection().new Message(0);
				mess.substitute(1, "");
				mess.addSourceReferences(id.sourceReferences);
				return null;
			}

			// If we are heading for an iterator, we need to supply a proper
			// binding context, which we need to compile the body (the argument)
			// of the iterator call. If there are declarations these will be
			// explicitly used, otherwise an implicit context will be added.
			ArrayList<OclNode.Declaration> ocldecls = null;
			if (itersFound > 0) {
				ocldecls = new ArrayList<OclNode.Declaration>(1);
				if (args.declarations != null) {
					// There are Declarations, loop ...
					for (TempNode tempdecl : args.declarations) {
						Declaration decl = (Declaration) tempdecl;
						// Compile it to a type-checked Declaration
						OclNode ocldecl = decl.connectToModelWithType(p, model,
								varctx, type);
						if (ocldecl == null)
							return null;
						OclNode.Declaration newVarctx = (OclNode.Declaration) ocldecl;
						// Link this into the variable declaration stack and
						// memorize in an array.
						varctx = newVarctx;
						ocldecls.add(newVarctx);
					}
				} else {
					// No Declarations present. We have to create an implicit
					// one.
					OclNode.Declaration defaultVarctx = new OclNode.Declaration(
							null, type, null, varctx, null, true);
					// Link and memorize it.
					varctx = defaultVarctx;
					ocldecls.add(defaultVarctx);
				}
			}

			// Now that we have the proper binding context available, we can
			// translate the argument list of the Property.
			OclNode[] oclargs = new OclNode[nargs];
			for (int i = 0; i < nargs; i++) {
				OclNode arg = args.expressions[i].connectToModel(p, model,
						varctx);
				if (arg == null)
					return null;
				oclargs[i] = arg;
			}

			// We can now finally execute the final selection by means of the
			// types of the arguments.
			OclNode.BuiltInDescr selectedbid = null;
			for (OclNode.BuiltInDescr bid : bids) {
				// Check argument types
				OclNode.BuiltInType[] bidtypes = bid.arguTypes;
				boolean matched = true;
				if (bidtypes != null) {
					for (int i = 0; i < bidtypes.length && !matched; i++) {
						// Only objects (no collections) allowed as arguments
						// in all supported operations
						if (!oclargs[i].isMultiple()) {
							// Are argument datatypes as requested?
							if (bidtypes[i] == OclNode.BuiltInType.ANY)
								continue;
							if (bidtypes[i] == oclargs[i]
									.getDataType().builtInType)
								continue;
						}
						// No: break
						matched = false;
					}
				}
				if (!matched)
					continue;
				// So, we found one. Use it.
				selectedbid = bid;
				break;
			}

			// If nothing remained, we have to complain.
			if (!isAddedOper && selectedbid == null) {
				if (!quiet) {
					MessageCollection.Message mess = p
							.getMessageCollection().new Message(27);
					mess.substitute(1, name);
					mess.substitute(2, typename);
					mess.substitute(3, String.valueOf(nargs));
					mess.addSourceReferences(sourceReferences);
				}
				return null;
			}

			// Final checks.
			if (itersFound > 0) {
				// Excess binding variable declarations.
				if (ocldecls.size() > selectedbid.noOfDecls) {
					if (!quiet) {
						MessageCollection.Message mess = p
								.getMessageCollection().new Message(31);
						mess.substitute(1, name);
						mess.addSourceReferences(sourceReferences);
					}
					return null;
				}
			}

			// Now create the result object
			OclNode.PropertyCallExp callexp = null;
			type = isAddedOper ? addedResultType
					: new DataType(selectedbid.resType);

			// Some special treatment on types
			if (!isAddedOper) {
				switch (selectedbid.specialTreatment) {
				case 1: // Type is instance type of class given as object
					if (bit == OclNode.BuiltInType.CLASS)
						type = new DataType(
								((OclNode.ClassLiteralExp) object).umlClass);
					break;
				case 2: // Type is type of object
					type = object.getDataType();
					break;
				case 3: // Type is instance type of class given as first
						// argument
					if (oclargs.length > 0
							&& oclargs[0].dataType.builtInType == OclNode.BuiltInType.CLASS) {
						type = new DataType(
								((OclNode.ClassLiteralExp) oclargs[0]).umlClass);
					}
					break;
				case 4: // Reverse-find a UML type for the built-in if possible
						// TODO
					for (String cn : OclNode.iso19103Map.keySet()) {
						OclNode.BuiltInType bityp = OclNode.iso19103Map.get(cn);
						if (bityp != null && bityp == type.builtInType) {
							ClassInfo mci = model.classByName(cn);
							if (mci != null) {
								type.umlClass = mci;
								break;
							}
						}
					}
					break;
				}
			}

			if (itersFound > 0) {
				// We have an Iterator
				OclNode.Declaration[] ocldclarray = new OclNode.Declaration[ocldecls
						.size()];
				ocldclarray = ocldecls.toArray(ocldclarray);
				callexp = new OclNode.IterationCallExp(object, name,
						ocldclarray, oclargs, type, selectedbid.multMap);
				// Associate it to the Declarations of the binding vars
				for (OclNode.Declaration dcl : ocldecls) {
					dcl.ownerNode = callexp;
				}
			} else {
				// This is just a simple Operation
				MultiplicityMapping mm = isAddedOper
						? MultiplicityMapping.ONE2ONE : selectedbid.multMap;
				callexp = new OclNode.OperationCallExp(object, arrow, name,
						oclargs, type, mm, false);
			}

			return callexp;
		}

		// Implementation of debugPrint in this class.
		void debugPrint(PrintWriter stream) {
			stream.print("P[");
			name.debugPrint(stream);
			if (arguments != null)
				arguments.debugPrint(stream);
			stream.print("]");
		}
	}

	/**
	 * <p>
	 * Identifier objects hold names. Names (if referring to the UML model) can
	 * be qualified by packages and classes. The qualification itself is
	 * expressed by an Identifier node.
	 * </p>
	 */
	static class Identifier extends TempNode {
		String name;
		TempNode qualification;

		/**
		 * <p>
		 * This constructor takes a qualification and a name to form an
		 * Identifier object. The qualification must be an Identifier object or
		 * null. The SourceReferences of the qualification - if given - are
		 * automatically added.
		 * </p>
		 * 
		 * @param qualification
		 *            Identifier object used as scope for the Identifier being
		 *            created.
		 * @param name
		 *            Name of the Identifier
		 * @param qualification
		 *            Identifier node containing the qualification, may be null.
		 */
		Identifier(TempNode qualification, String name) {
			// Constituents
			this.name = name;
			this.qualification = qualification;
			// SourceReferences
			if (qualification != null)
				addSourceReference(qualification);
		}

		/**
		 * <p>
		 * Determine the name of the Identifier. Qualifications are prefixed
		 * using ::.
		 * </p>
		 * 
		 * @return Name of Identifier.
		 */
		String name() {
			String scope = "";
			if (qualification != null) {
				if (qualification instanceof Identifier)
					scope = ((Identifier) qualification).name() + "::";
			}
			return scope + name;
		}

		/**
		 * <p>
		 * Find out whether the Identifier represents a simple, not-qualified
		 * name.
		 * <p>
		 * 
		 * @return Identifier is simple.
		 */
		boolean isSimpleName() {
			return qualification == null;
		}

		/**
		 * <p>
		 * This method translates the qualification of an Identifier into a
		 * ClassInfo reference. A value of null is returned if there is no
		 * qualification or if it cannot be resolved.
		 * </p>
		 * 
		 * @param p
		 *            The parser object for accessing the diagnostics stream
		 * @param model
		 *            The UML model object
		 * @param quiet
		 *            Do no output diagnostics
		 * @return The ClassInfo object or null
		 */
		ClassInfo classFromQualification(OclParser p, Model model,
				boolean quiet) {
			if (qualification == null)
				return null;
			if (qualification instanceof Identifier) {
				Identifier qualid = (Identifier) qualification;
				OclNode cls = qualid.connectAsClassOrPackage(p, model, 1,
						quiet);
				if (cls == null || !(cls instanceof OclNode.ClassLiteralExp))
					return null;
				return ((OclNode.ClassLiteralExp) cls).umlClass;
			} else {
				// It is a system error if this is not an Identifier token
				MessageCollection.Message mess = p
						.getMessageCollection().new Message(0);
				mess.substitute(1, "Qualifier is non-Identifier token.");
				mess.addSourceReferences(this.sourceReferences);
			}
			return null;
		}

		/**
		 * This method connects an Identifier to the model. The only cases
		 * considered are stand-alone uses of the identifier as UML-defined
		 * constants. All other possible uses of the identifier (including the
		 * use as a variable) are ignored and have to be treated outside.
		 */
		OclNode connectToModel(OclParser p, Model model,
				OclNode.Declaration varctx) {

			// We first make an attempt to interpret the identifier as an
			// enumeration constant.
			if (qualification != null && qualification instanceof Identifier) {
				ClassInfo ci = classFromQualification(p, model, true);
				if (ci != null) {
					int cat = ci.category();
					if (cat == Options.ENUMERATION || cat == Options.CODELIST) {
						// So, this is indeed a construct of the form
						// qualifier::idname, where qualifier designates
						// a class of the UML model. From here onwards
						// we assume this has to be an enumeration.
						PropertyInfo pi = ci.property(name);
						if (pi != null) {
							// Verified. Construct and return the LiteralExp
							// for it ...
							return new OclNode.EnumerationLiteralExp(pi);
						} else {
							// Not found
							if (ci.matches(
									"rule-xsd-cls-codelist-constraints-codeAbsenceInModelAllowed")
									|| ci.matches("rule-xsd-all-notEncoded")) {
								return new OclNode.EnumerationLiteralExp(name,
										ci);
							} else {
								// Then this has to be regarded an error
								MessageCollection.Message mess = p
										.getMessageCollection().new Message(33);
								mess.substitute(1, name);
								mess.substitute(2, ci.name());
								mess.addSourceReferences(sourceReferences);
								return null;
							}
						}
					}
				}
			}

			// Failed to recognize an enumeration. In this case we have to
			// assume it is a class constant.
			OclNode ret = connectAsClassOrPackage(p, model, 1, true);

			return ret;
		}

		/**
		 * This auxiliary method is applied to an Identifier TempNode, which is
		 * known to be a UML class or package element. Which of both is actually
		 * expected is expressed by the argument 'level', where 1 means class
		 * and 2 or more means package.
		 * 
		 * @param p
		 *            The parser object for accessing the diagnostics stream
		 * @param model
		 *            The UML model object
		 * @param level
		 *            Level of nesting (1=class/2+=package)
		 * @param quiet
		 *            Do not output diagnostics
		 * @return A ClassLiteralExp or a PackageLiteralExp or null (in case of
		 *         error)
		 */
		OclNode connectAsClassOrPackage(OclParser p, Model model, int level,
				boolean quiet) {

			// If this Identifier is still qualified, we will have to resolve
			// this first.
			Identifier qual = null;
			OclNode qctx = null;
			if (qualification != null) {
				if (qualification instanceof Identifier) {
					// Connect the qualification to the model
					qual = (Identifier) qualification;
					qctx = qual.connectAsClassOrPackage(p, model, level + 1,
							quiet);
					// If this did not work: Quit - a message already has been
					// provided ...
					if (qctx == null)
						return null;
				} else {
					// It is a system error if this is not an Identifier token
					MessageCollection.Message mess = p
							.getMessageCollection().new Message(0);
					mess.substitute(1, "Qualifier is non-Identifier token.");
					mess.addSourceReferences(this.sourceReferences);
					return null;
				}
			}

			// Now do the requested model search, this is different code for
			// level==1 (class is searched) and level==2 and higher (package is
			// searched.

			if (level == 1) {

				// *** Search for a class
				// **********************

				ClassInfo ci = null;
				if (qctx == null) {
					// ** Unconstrained search
					ci = model.classByName(name);
					if (ci == null && !quiet) {
						// Class not found in model. Complain ...
						MessageCollection.Message mess = p
								.getMessageCollection().new Message(21);
						mess.substitute(1, name);
						mess.addSourceReferences(sourceReferences);
					}
				} else {
					// ** Constrain search to the package
					OclNode.PackageLiteralExp plit = (OclNode.PackageLiteralExp) qctx;
					PackageInfo pi = plit.getPackage();
					SortedSet<ClassInfo> cls = model.classes(pi);
					for (ClassInfo c : cls) {
						if (c.name().equals(name)) {
							ci = c;
							break;
						}
					}
					if (ci == null || ci.pkg() != pi) {
						// Not found in package. Complain ...
						if (!quiet) {
							MessageCollection.Message mess = p
									.getMessageCollection().new Message(22);
							mess.substitute(1, name);
							mess.substitute(2, pi.name());
							mess.addSourceReferences(this.sourceReferences);
						}
						ci = null;
					}
				}
				// Return the class wrapped into a TypeLiteral (or null)
				if (ci != null)
					return new OclNode.ClassLiteralExp(ci);
				else
					return null;

			} else {

				// *** Search for a package
				// ************************

				PackageInfo pi = null;
				if (qctx == null) {
					// ** Unconstrained search
					// TODO Note: This is a bit hard because the interfaces do
					// not really support this well.
					SortedSet<PackageInfo> pckseed = model.packages();
					// Get descend started from the hooks we got delivered
					LinkedList<PackageInfo> eval = new LinkedList<PackageInfo>();
					for (PackageInfo pck : pckseed)
						eval.add(pck);
					pi = getPackageToName(eval);
					if (pi == null && !quiet) {
						// Package not found in model. Complain ...
						MessageCollection.Message mess = p
								.getMessageCollection().new Message(23);
						mess.substitute(1, name);
						mess.addSourceReferences(this.sourceReferences);
					}
				} else {
					// ** Constrain search to the package
					OclNode.PackageLiteralExp plit = (OclNode.PackageLiteralExp) qctx;
					PackageInfo pi2 = plit.getPackage();
					try {
						SortedSet<PackageInfo> pks = pi2.containedPackages();
						for (PackageInfo pk : pks) {
							if (pk.name().equals(name)) {
								pi = pk;
								break;
							}
						}
					} catch (Exception e) {
					}
					if (pi == null && !quiet) {
						// Not found in package. Complain ...
						MessageCollection.Message mess = p
								.getMessageCollection().new Message(24);
						mess.substitute(1, name);
						mess.substitute(2, pi2.name());
						mess.addSourceReferences(this.sourceReferences);
					}
				}
				// Return the package wrapped into a PackageLiteral (or null)
				if (pi != null)
					return new OclNode.PackageLiteralExp(pi);
				else
					return null;
			}
		}

		// Remove packages from the list, adding descendants as we go.
		PackageInfo getPackageToName(LinkedList<PackageInfo> eval) {
			LinkedList<PackageInfo> evalSub = null;
			for (PackageInfo cpi : eval) {
				// Are we the one we are looking for?
				if (cpi.name().equals(name)) {
					return cpi;
				}
				// No, prepare descend to next packages
				try {
					SortedSet<PackageInfo> npis = cpi.containedPackages();
					if (evalSub == null && npis.size() > 0)
						evalSub = new LinkedList<PackageInfo>();
					for (PackageInfo npi : npis)
						evalSub.add(npi);
				} catch (Exception e) {
				}
			}
			if (evalSub != null)
				return getPackageToName(evalSub);
			else
				return null;
		}

		// Implementation of debugPrint in this class.
		void debugPrint(PrintWriter stream) {
			if (qualification != null) {
				qualification.debugPrint(stream);
				stream.print("::");
			}
			stream.print(name);
		}
	}

	/**
	 * <p>
	 * Declaration stands for declarations at various places in the OCL syntax.
	 * It combines a simple name (the variable name) with a type (in this
	 * reduced syntax limited to path names, which are represented by a
	 * TempNode.Identifier) and an initializing expression. The latter two items
	 * may be missing.
	 * </p>
	 */
	static class Declaration extends TempNode {

		String name;
		TempNode typename;
		TempNode initializer;

		/**
		 * <p>
		 * This constructor creates a variable declaration out of a simple name,
		 * a typename and an initializer. Both, typename and initializer may be
		 * null. If present, the SourceReferences of typename and initializer
		 * are automatically added.
		 * </p>
		 * 
		 * @param name
		 *            String containing the name of the variable.
		 * @param typename
		 *            Identifier TempNode, which stands for a type. May be null.
		 * @param initializer
		 *            Arbitrary OCL expression. May be null.
		 */
		Declaration(String name, TempNode typename, TempNode initializer) {
			// Constituents
			this.name = name;
			this.typename = typename;
			this.initializer = initializer;
			// SourceReferences
			if (typename != null)
				addSourceReference(typename);
			if (initializer != null)
				addSourceReference(initializer);
		}

		/**
		 * Create a OclNode.Declaration from this TempNode.Declaration. The
		 * given type argument defines the type and must be compatible with a
		 * possible type found in the declaration.
		 * 
		 * @param p
		 *            Parser object for accessing the diagnostics stream
		 * @param model
		 *            The UML model object
		 * @param varctx
		 *            Variable declaration context
		 * @param type
		 *            The type for the variable
		 * @return OclNode.Declaration or null
		 */
		OclNode connectToModelWithType(OclParser p, Model model,
				OclNode.Declaration varctx, OclNode.DataType type) {

			// First construct a Declaration without the additional type info
			OclNode decl = connectToModel(p, model, varctx);
			if (decl == null)
				return null;
			OclNode.Declaration dcl = (OclNode.Declaration) decl;

			// No employ the type information
			if (dcl.getDataType().builtInType == OclNode.BuiltInType.VOID) {
				// The additional type wins ...
				dcl.dataType = type;
			} else {
				// Both types present. They need to be identical.
				boolean ok = dcl.getDataType().isSubTypeOf(type)
						&& type.isSubTypeOf(dcl.getDataType());
				if (!ok) {
					MessageCollection.Message mess = p
							.getMessageCollection().new Message(39);
					mess.substitute(1, type.name);
					mess.substitute(2, dcl.getDataType().name);
					mess.substitute(3, dcl.name);
					mess.addSourceReferences(sourceReferences);
					return null;
				}
			}

			// Tests and corrections passed. Return Declaration.
			return dcl;
		}

		/**
		 * Create a OclNode.Declaration from this TempNode.Declaration. The type
		 * is derived from either the explicitly specified type or implicitly
		 * from the initial value. If neither is present, the type 'void' is
		 * established and the caller is responsible for providing a proper
		 * type.
		 * 
		 * @param p
		 *            Parser object for accessing the diagnostics stream
		 * @param model
		 *            The UML model object
		 * @param varctx
		 *            Variable declaration context
		 * @param type
		 *            The type for the variable
		 * @return OclNode.Declaration or null
		 */
		OclNode connectToModel(OclParser p, Model model,
				OclNode.Declaration varctx) {

			// First try to translate the typename to a DataType
			DataType type = null;
			if (typename != null) {
				if (!(typename instanceof Identifier)) {
					MessageCollection.Message mess = p
							.getMessageCollection().new Message(0);
					mess.substitute(1,
							"Declaration type name is not an Identifer.");
					mess.addSourceReferences(typename.sourceReferences);
					return null;
				}
				Identifier typeid = (Identifier) typename;
				OclNode cls = typeid.connectAsClassOrPackage(p, model, 1, true);
				if (cls != null) {
					OclNode.ClassLiteralExp cl = (OclNode.ClassLiteralExp) cls;
					ClassInfo ci = cl.umlClass;
					type = new DataType(ci);
				} else {
					type = new DataType(typeid.name());
					if (type.builtInType == OclNode.BuiltInType.VOID)
						type = null;
				}
				if (type == null) {
					MessageCollection.Message mess = p
							.getMessageCollection().new Message(38);
					mess.substitute(1, typeid.name());
					mess.substitute(2, this.name);
					mess.addSourceReferences(typename.sourceReferences);
					return null;
				}
			}

			// Now translate the initial value, in case it is present. In
			// absence of a type the initial value also determines the type of
			// the declaration. Otherwise types have to be compatible.
			OclNode init = null;
			if (initializer != null) {
				// The initializer has to be connected to the model ...
				init = initializer.connectToModel(p, model, varctx);
				if (init == null)
					return null;
				// Check its type
				DataType inittype = init.getDataType();
				if (type == null)
					// Type info can be derived from initializer.
					type = inittype;
				else {
					// There is type info from an explicit type and from the
					// initial value. Make sure the initial value is a subtype
					// of the declared type.
					if (!inittype.isSubTypeOf(type)) {
						MessageCollection.Message mess = p
								.getMessageCollection().new Message(39);
						mess.substitute(1, type.name);
						mess.substitute(2, inittype.name);
						mess.substitute(3, this.name);
						mess.addSourceReferences(sourceReferences);
						return null;
					}
				}
			}

			// If there is still no type, we create a Void
			if (type == null)
				type = new DataType(OclNode.BuiltInType.VOID);

			// This sets up the OclNode.Declaration.
			return new OclNode.Declaration(name, type, init, varctx, null,
					false);
		}

		// Implementation of debugPrint in this class.
		void debugPrint(PrintWriter stream) {
			stream.print("Declare[");
			stream.print(name);
			if (typename != null) {
				stream.print(":");
				typename.debugPrint(stream);
			}
			if (initializer != null) {
				stream.print("=");
				initializer.debugPrint(stream);
			}
			stream.print("]");
		}
	}

	/**
	 * <p>
	 * This is an auxiliary class, which stands for the current date and time
	 * when the expression is used - not when it is compiled.
	 * </p>
	 * <p>
	 * The class therefore has no properties except for a bit of better
	 * formatting when printed.
	 * </p>
	 * 
	 */
	static class CurrentDateTime {
		/**
		 * Conversion to String delivers a constant String.
		 * 
		 * @return Constant string "CurrentDateTime"
		 */
		public String toString() {
			return "CurrentDateTime";
		}
	}

	/**
	 * <p>
	 * This auxiliary class extends GregorianCalender and is employed to
	 * represent a Date literal value. Wrapping GregorianCalendar in DateTime
	 * has the mere purpose to allow overwriting the toString() method, which as
	 * implemented in GregorianCalendar is not useful for debugging purposes.
	 * </p>
	 *
	 */
	@SuppressWarnings("serial")
	static class DateTime extends GregorianCalendar {
		/**
		 * Conversion to String delivers the standard string representation of
		 * Date.
		 * 
		 * @return Readable string containing date and time in the local zone.
		 */
		public String toString() {
			return this.getTime().toString();
		}
	}

	/**
	 * <p>
	 * This auxiliary class is for setting up an OclVoid type literal.
	 * </p>
	 */
	static class Nothing {
		/**
		 * This delivers the String "null".
		 * 
		 * @return "null"
		 */
		public String toString() {
			return "null";
		}
	}

	/**
	 * <p>
	 * Literal objects stand for constant values such as integers, floating
	 * point numbers, strings or dates. The class is generic an take types of
	 * any primitive data type required. Types to be used are Long, Double,
	 * String, etc.
	 * </p>
	 * 
	 * @param <T>
	 *            Type of Literal, must be one of Long, Double, String, Boolean,
	 *            TempNode.DateTime, TempNode.CurrentDateTime, TempNode.Nothing.
	 */
	static class Literal<T> extends TempNode {
		T value;

		/**
		 * </p>
		 * Constructs a literal node of some type T. Types to be used are Long,
		 * Double, String, Boolean, Calendar, CurrentDate.
		 * </p>
		 * <p>
		 * Calendar stands for DateTimes. The local class CurrentDate models the
		 * Date at the time of use.
		 * </p>
		 * 
		 * @param value
		 *            The value of the Literal.
		 */
		Literal(T value) {
			this.value = value;
		}

		/**
		 * This returns the value of the TempNode.Literal.
		 * 
		 * @return Value of the Literal.
		 */
		T getValue() {
			return value;
		}

		/**
		 * This method connects a Literal to the model, which means that the
		 * literal value is wrapped into some appropriate OclNode.LiteralExp.
		 * 
		 * @param p
		 *            Parser object for accessing the diagnostics stream
		 * @param model
		 *            The UML model object
		 * @param varctx
		 *            Variable declaration context
		 * @return OclNode.LiteralExp in same concrete subclass - or null
		 */
		OclNode connectToModel(OclParser p, Model model,
				OclNode.Declaration varctx) {

			if (value.getClass() == Double.class) {
				return new OclNode.RealLiteralExp((Double) value);
			} else if (value.getClass() == Integer.class
					|| value.getClass() == Long.class) {
				return new OclNode.IntegerLiteralExp((Long) value);
			} else if (value.getClass() == String.class) {
				return new OclNode.StringLiteralExp((String) value);
			} else if (value.getClass() == Boolean.class) {
				return new OclNode.BooleanLiteralExp((Boolean) value);
			} else if (value.getClass() == TempNode.DateTime.class) {
				return new OclNode.DateTimeLiteralExp(
						(GregorianCalendar) value);
			} else if (value.getClass() == TempNode.CurrentDateTime.class) {
				return new OclNode.DateTimeLiteralExp();
			} else if (value.getClass() == TempNode.Nothing.class) {
				return new OclNode.OclVoidLiteralExp();
			}
			// So, this must be an error
			MessageCollection.Message mess = p
					.getMessageCollection().new Message(0);
			String name = value.getClass().getName();
			mess.substitute(1,
					"Attempt to create a literal of type '" + name + "'.");
			mess.addSourceReferences(this.sourceReferences);
			return null;
		}

		// Implementation of debugPrint in this class.
		void debugPrint(PrintWriter stream) {
			stream.print("L[");
			stream.print(value);
			stream.print("]");
		}
	}

	/**
	 * <p>
	 * IfClause represents an if...then...else...endif construct. The three
	 * parts are condition, ifPart and elsePart all represent arbitrary OCL
	 * expressions.
	 * </p>
	 */
	static class IfClause extends TempNode {

		TempNode condition;
		TempNode ifPart;
		TempNode elsePart;

		/**
		 * <p>
		 * This constructs an IfClause out of its three constituents.
		 * SourceReferences of the constituents are automatically added.
		 * </p>
		 * 
		 * @param condition
		 *            The condition of the if
		 * @param ifPart
		 *            The part used if the condition is true
		 * @param elsepart
		 *            The part used if the condition is false
		 */
		IfClause(TempNode condition, TempNode ifPart, TempNode elsepart) {
			// Constituents
			this.condition = condition;
			this.ifPart = ifPart;
			this.elsePart = elsepart;
			// SourceReferences
			addSourceReference(condition);
			addSourceReference(ifPart);
			addSourceReference(elsePart);
		}

		/**
		 * This connects an IfClause to the model. All three parts of the
		 * construct are compiled and their resulting types are checked
		 * according to the rules of OCL. The result will be an OclNode.IfExp.
		 * 
		 * @param p
		 *            Parser object for accessing the diagnostics stream
		 * @param model
		 *            The UML model object
		 * @param varctx
		 *            Variable declaration context
		 * @return OclNode.IfExp or null if in error
		 */
		OclNode connectToModel(OclParser p, Model model,
				OclNode.Declaration varctx) {

			// First bind the three parts to the model ...
			OclNode cond_p = condition.connectToModel(p, model, varctx);
			OclNode if_p = ifPart.connectToModel(p, model, varctx);
			OclNode else_p = elsePart.connectToModel(p, model, varctx);

			// If any of the parts is null, we are done
			if (cond_p == null)
				return null;
			if (if_p == null)
				return null;
			if (else_p == null)
				return null;

			// Condition part must be of Boolean type.
			if (cond_p
					.getDataType().builtInType != OclNode.BuiltInType.BOOLEAN) {
				MessageCollection.Message mess = p
						.getMessageCollection().new Message(35);
				mess.addSourceReferences(condition.sourceReferences);
				return null;
			}

			// Types of if-part and else-part must have a common superclass
			// which is also the type of the if-expression.
			DataType common = if_p.getDataType()
					.commonSuperType(else_p.getDataType());
			if (common == null) {
				MessageCollection.Message mess = p
						.getMessageCollection().new Message(36);
				mess.substitute(1, if_p.getDataType().name);
				mess.substitute(2, else_p.getDataType().name);
				mess.addSourceReferences(sourceReferences);
				return null;
			}

			// That's it
			return new OclNode.IfExp(common, cond_p, if_p, else_p);
		}

		// Implementation of debugPrint in this class.
		void debugPrint(PrintWriter stream) {
			stream.print("if-then-else[");
			condition.debugPrint(stream);
			stream.print(",");
			ifPart.debugPrint(stream);
			stream.print(",");
			elsePart.debugPrint(stream);
			stream.print("]");
		}
	}

	/**
	 * <p>
	 * A LetClause stands for a let ... in ... construct. The declarations part
	 * and expression part are the constituents of the objects.
	 * </p>
	 *
	 */
	static class LetClause extends TempNode {

		TempNode[] declarations;
		TempNode expression;

		/**
		 * <p>
		 * This constructor initializes a LetClause using an array of
		 * declarations and and an expression. SourceReferences of the
		 * constituents are automatically added.
		 * </p>
		 * 
		 * @param declarations
		 *            Array of declarations
		 * @param expression
		 *            The expression to which the declarations apply
		 */
		LetClause(TempNode[] declarations, TempNode expression) {
			// Constituents
			this.declarations = declarations;
			this.expression = expression;
			// Source References
			for (int i = 0; i < declarations.length; i++)
				addSourceReference(declarations[i]);
			addSourceReference(expression);
		}

		/**
		 * <p>
		 * This connects a let-clause to the model. Declarations are checked for
		 * exhibiting a proper type and an initial value. The body expression is
		 * compiled in the context of the declarations and a OclNode.LetExp is
		 * returned containing body and declarations.
		 * 
		 * @param p
		 *            Parser object for accessing the diagnostics stream
		 * @param model
		 *            The UML model object
		 * @param varctx
		 *            Variable declaration context
		 * @return OclNode.LetExp or null if in error
		 */
		OclNode connectToModel(OclParser p, Model model,
				OclNode.Declaration varctx) {

			// First we have to prepare the declarations because the body
			// expression needs to be compiled in the context established by
			// those.
			ArrayList<OclNode.Declaration> dcls = new ArrayList<OclNode.Declaration>();
			if (declarations != null) {
				for (TempNode declaration : declarations) {
					// Security check
					if (declaration == null
							|| !(declaration instanceof Declaration)) {
						// This is a system error.
						MessageCollection.Message mess = p
								.getMessageCollection().new Message(0);
						mess.substitute(1,
								"Illegal Declaration in 'let' construct.");
						if (declaration == null)
							mess.addSourceReferences(sourceReferences);
						else
							mess.addSourceReferences(
									declaration.sourceReferences);
						return null;
					}
					// We have a Declaration. Connect it to the model
					OclNode ocldecl = declaration.connectToModel(p, model,
							varctx);
					if (ocldecl == null)
						return null;
					OclNode.Declaration dcl = (OclNode.Declaration) ocldecl;
					// Check whether it exhibits a proper type
					if (dcl.getDataType().builtInType == OclNode.BuiltInType.VOID) {
						MessageCollection.Message mess = p
								.getMessageCollection().new Message(37);
						mess.substitute(1, dcl.name);
						mess.addSourceReferences(declaration.sourceReferences);
						return null;
					}
					// The Declaration of a let-construct also needs a value
					if (dcl.initialValue == null) {
						MessageCollection.Message mess = p
								.getMessageCollection().new Message(38);
						mess.substitute(1, dcl.name);
						mess.substitute(2, dcl.getDataType().name);
						mess.addSourceReferences(declaration.sourceReferences);
						return null;
					}
					// O.k. - Collect it for later update of owner field
					dcls.add(dcl);
					// .. and push it on the scope of declarations ..
					varctx = dcl;
				}
			}

			// Now we can treat the body of the let and connect it to
			// the model in the context of the declarations.
			OclNode body = expression.connectToModel(p, model, varctx);
			if (body == null)
				return null;

			// Create the LetExp and make it owner of its Declarations.
			OclNode.Declaration[] dclarray = new OclNode.Declaration[dcls
					.size()];
			dclarray = dcls.toArray(dclarray);
			OclNode let = new OclNode.LetExp(dclarray, body);
			for (OclNode.Declaration d : dcls)
				d.ownerNode = let;

			return let;
		}

		// Implementation of debugPrint in this class.
		void debugPrint(PrintWriter stream) {
			stream.print("let[");
			for (int i = 0; i < declarations.length; i++) {
				if (i > 0)
					stream.print(",");
				declarations[i].debugPrint(stream);
			}
			stream.print(";");
			expression.debugPrint(stream);
			stream.print("]");
		}
	}

	/**
	 * <p>
	 * The Arguments object represents an argument list in a property call and -
	 * besides the arguments proper - may contain variable declarations, which
	 * stand for the binding variable in an iterative statement.
	 * </p>
	 */
	static class Arguments extends TempNode {

		TempNode[] declarations;
		TempNode[] expressions;

		/**
		 * <p>
		 * This initializes an Arguments object given the declarations and the
		 * arguments proper. SourceReferences of the constituents are
		 * automatically handled.
		 * </p>
		 * 
		 * @param declarations
		 *            VarDecl objects representing declarations
		 * @param expressions
		 *            The arguments proper
		 */
		Arguments(TempNode[] declarations, TempNode[] expressions) {
			// Constituents
			this.declarations = declarations;
			this.expressions = expressions;
			// Source references
			if (declarations != null)
				for (int i = 0; i < declarations.length; i++)
					addSourceReference(declarations[i]);
			if (expressions != null)
				for (int i = 0; i < expressions.length; i++)
					addSourceReference(expressions[i]);
		}

		/**
		 * <p>
		 * This implements the connectToModel abstract method on Arguments.
		 * Since Arguments are fully treated in the context of Property there is
		 * no need to call this method on Arguments.
		 * </p>
		 * 
		 * @param p
		 *            Parser object for accessing the diagnostics stream
		 * @param model
		 *            The UML model object
		 * @param varctx
		 *            Variable declaration context
		 * @return Always null
		 */
		OclNode connectToModel(OclParser p, Model model,
				OclNode.Declaration varctx) {
			// This will be punished with a system error.
			MessageCollection.Message mess = p
					.getMessageCollection().new Message(0);
			mess.substitute(1,
					"Attempt to connect TempNode.Arguments to the model.");
			mess.addSourceReferences(sourceReferences);
			return null;
		}

		// Implementation of debugPrint in this class.
		void debugPrint(PrintWriter stream) {
			stream.print("(");
			if (declarations != null && declarations.length > 0) {
				for (int i = 0; i < declarations.length; i++) {
					if (i > 0)
						stream.print(",");
					declarations[i].debugPrint(stream);
				}
				stream.print("|");
			}
			for (int i = 0; i < expressions.length; i++) {
				if (i > 0)
					stream.print(",");
				expressions[i].debugPrint(stream);
			}
			stream.print(")");
		}
	}
}