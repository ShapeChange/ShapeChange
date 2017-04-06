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

package de.interactive_instruments.ShapeChange.Target.FOL2Schematron;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;

import de.interactive_instruments.ShapeChange.AIXMSchemaInfos.AIXMSchemaInfo;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.FOL.Literal;
import de.interactive_instruments.ShapeChange.FOL.PropertyCall;
import de.interactive_instruments.ShapeChange.FOL.Quantification;
import de.interactive_instruments.ShapeChange.FOL.Quantifier;
import de.interactive_instruments.ShapeChange.FOL.RealLiteral;
import de.interactive_instruments.ShapeChange.FOL.StringLiteral;
import de.interactive_instruments.ShapeChange.FOL.StringLiteralList;
import de.interactive_instruments.ShapeChange.FOL.Variable;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.XmlSchema.XmlSchema;

/**
 * FolSchematronNode and its concrete derivations stand for a representation of
 * First Order Logic contents, which are close to the capabilities of Schematron
 * and the logic that can be realized within Schematron Rules.
 * 
 * @author Johannes Echterhoff
 *
 */
public abstract class FolSchematronNode {

	public static final int SIMPLE_TYPE = 0;
	public static final int INLINE = 1;
	public static final int BY_REFERENCE = 2;
	public static final int INLINE_OR_BY_REFERENCE = 3;

	private static final String COUNTING_VARIABLE_NAME = "$c";

	/** The children of the SchematronNode */
	protected ArrayList<FolSchematronNode> children = new ArrayList<FolSchematronNode>();

	/** The parent reference */
	protected FolSchematronNode parent = null;

	/** Link back to FOL2Schematron object */
	protected FOL2Schematron schemaObject = null;

	/** Types of XPath */
	protected enum XpathType {
		BOOLEAN, NUMBER, STRING, NODESET
	}

	/**
	 * Method to add children to a node and at the same time establish the node
	 * as parent of the child to be added.
	 * 
	 * @param child
	 *            The Child node to be added
	 */
	public void addChild(FolSchematronNode child) {
		children.add(child);
		child.parent = this;
	}

	/**
	 * <p>
	 * Method to inquire whether the the node inquired is a Logic node AND this
	 * logic node has the same <i>isAnd</i> polarity as specified in the
	 * parameter.
	 * <p>
	 * <p>
	 * This implementation installs the default for all derivations except
	 * Logic.
	 * </p>
	 * 
	 * @param isAnd
	 *            Flag: Are we an AND? (not an OR)?
	 * @return True if this is Logic with the same polarity
	 */
	public boolean isAndOrLogic(boolean isAnd) {
		return false;
	}

	/**
	 * <p>
	 * This method determines whether the given expression depends on the
	 * Variable passed as argument.
	 * </p>
	 * <p>
	 * This implementation defines the default behavior: Descend down and try to
	 * find the variable somewhere.
	 * </p>
	 * 
	 * @param vardecl
	 *            The Variable of the variable
	 * @return Flag indicating the inquired dependency
	 */
	public boolean isDependentOn(Variable vardecl) {
		for (FolSchematronNode scn : children)
			if (scn.isDependentOn(vardecl))
				return true;
		return false;
	}

	/**
	 * This method determines whether the node binds the given variable
	 * declaration (this can only happen for iterators) and if it does, which is
	 * the expression the variable is bound to.
	 * 
	 * @param vardecl
	 *            The variable Variable object
	 * @return Node the variable is bound to or null
	 */
	public FolSchematronNode nodeVariableIsBoundTo(Variable vardecl) {
		return null;
	}

	public XpathFragment objValueFromVariable(Variable var) {

		String alpha = schemaObject.alpha;
		String beta = schemaObject.beta;
		boolean alphaEx = alpha != null && alpha.length() > 0;
		boolean betaEx = beta != null && beta.length() > 0;

		XpathFragment obj = null;

		if (var.isSelf()) {

			obj = new XpathFragment(0, "current()");

		} else {

			obj = new XpathFragment(0, "$" + var.getName());

			/*
			 * compute value from last effective property call in variable value
			 */
			PropertyCall lastPC = var.lastPropertyCallInEffectiveValue();

			if (lastPC == null) {

				/*
				 * Fine - then no need to compute the value because the variable
				 * context ultimately must be self
				 */

			} else {

				PropertyInfo piFromLastPC = lastPC.getSchemaElement();
				// get the actual variable 'value'
				if (!hasSimpleType(piFromLastPC)) {

					int conCode;

					String ref = piFromLastPC.inlineOrByReference();

					if (ref.equalsIgnoreCase("byreference"))
						conCode = BY_REFERENCE;
					else if (ref.equalsIgnoreCase("inline"))
						conCode = INLINE;
					else
						conCode = INLINE_OR_BY_REFERENCE;

					// Other: different modes of containment are possible
					String frag_inl = null;
					String frag_ref = null;

					if (conCode == INLINE || conCode == INLINE_OR_BY_REFERENCE) {

						// In-line containment must be treated
						// --> $var/*
						frag_inl = obj.fragment;
						frag_inl += "/*";
					}

					if (conCode == BY_REFERENCE
							|| conCode == INLINE_OR_BY_REFERENCE) {

						// --> //*[@gml:id]=.../$var/@xlink:href]
						// --> if @gml:id is surrounded with additional
						// text,
						// --> a concat() construct is used.
						String attxlink = obj.fragment;
						attxlink += "/@xlink:href";
						frag_ref = "//*[";
						if (alphaEx || betaEx) {
							frag_ref += "concat(";
							if (alphaEx)
								frag_ref += "'" + alpha + "',";
							frag_ref += "@gml:id";
							if (betaEx)
								frag_ref += ",'" + beta + "'";
							frag_ref += ")";
						} else {
							frag_ref += "@gml:id";
						}
						frag_ref += "=" + attxlink + "]";
						schemaObject.registerNamespace("xlink");
						schemaObject.registerNamespace("gml");

						// Whatever binding context we had before, it is
						// lost
						if (obj.atEnd != null)
							obj.atEnd.setState(BindingContext.CtxState.OTHER);
					}

					// Set the fragment value, possibly combining both
					// containment representations
					if (conCode == INLINE_OR_BY_REFERENCE) {
						obj.fragment = frag_inl + " | " + frag_ref;
						obj.priority = 8;
					} else if (conCode == INLINE) {
						obj.fragment = frag_inl;
						obj.priority = 9;
					} else {
						obj.fragment = frag_ref;
						obj.priority = 10;
					}
				}
			}
		}

		return obj;
	}

	/**
	 * <p>
	 * This method determines whether the given expression is a Variable or an
	 * Attribute based on a Variable, which is identical to the one passed as
	 * argument.
	 * </p>
	 * <p>
	 * This implementation defines the default behavior.
	 * </p>
	 * 
	 * @param vardecl
	 *            The Variable of the variable
	 * @return Flag indicating the inquired property
	 */
	public boolean isVarOrAttribBased(Variable vardecl) {
		return false;
	}

	/**
	 * <p>
	 * By means of this function you can inquire which Attribute node is
	 * generating the objects represented by this node. Note that invocation is
	 * only sensible for iterators and attributes.
	 * </p>
	 * 
	 * @return The retrieved Attribute node if there is such a thing
	 */
	public AttributeNode generatingAttribute() {
		return null;
	}

	/**
	 * <p>
	 * This predicate finds out whether the given node may produce a set.
	 * </p>
	 * <p>
	 * This is the default implementation providing the value false.
	 * </p>
	 * 
	 * @return Flag indicating whether the node can return multiple values
	 */
	public boolean isMultiple() {
		return false;
	}

	/**
	 * <p>
	 * This predicate finds out whether the given node is realized by means of a
	 * simple XML schema type.
	 * </p>
	 * 
	 * @return Flag indicating whether the node has a simple type
	 */
	public boolean hasSimpleType() {
		return true;
	}

	public boolean hasSimpleType(PropertyInfo pi) {

		boolean result = true;

		ClassInfo ci = getTypeClassInfo(pi);

		if (ci != null)
			result = !XmlSchema.classHasObjectElement(ci);

		return result;
	}

	/**
	 * <p>
	 * This predicate finds out whether the given node is realized by means of a
	 * class, which conceptually has identity.
	 * </p>
	 * 
	 * @return Flag indicating whether the node is an identity carrying type
	 */
	public boolean hasIdentity() {
		return false;
	}

	public boolean isAIXMExtension(Info i) {

		if (schemaObject.options.getAIXMSchemaInfos() != null) {

			AIXMSchemaInfo si = schemaObject.options.getAIXMSchemaInfos().get(
					i.id());

			if (si != null)
				return si.isExtension();
		}

		return false;
	}

	/**
	 * <p>
	 * Find out whether this construct contains a node of type
	 * SchematronNode.Error. In this case the whole tree is in error.
	 * </p>
	 * 
	 * @return Error flag
	 */
	public boolean containsError() {
		if (this instanceof Error)
			return true;
		for (FolSchematronNode node : children)
			if (node.containsError())
				return true;
		return false;
	}

	/**
	 * <p>
	 * The primary information stored in this class is whether there is
	 * currently a nodeset context at all - NONE if the expression is not a
	 * nodeset - and if the context is currently identical to current() -
	 * ATCURRENT. All other contexts are combined in OTHER.
	 * </p>
	 * <p>
	 * The vars part comes into living as soon as variables are encountered.
	 * They are tracked together with the information how far they are up the
	 * stack.
	 * </p>
	 */
	public static class BindingContext {
		public enum CtxState {
			NONE, ATCURRENT, OTHER
		}

		public CtxState state;

		// Ctor
		BindingContext(CtxState state) {
			this.state = state;
		}

		// clone() override
		public BindingContext clone() {

			BindingContext copy = new BindingContext(state);

			return copy;
		}

		// Reset state
		public void setState(CtxState state) {
			this.state = state;
			// this.vars = null;
		}

		// Merge another context
		public void merge(BindingContext ctx) {
			if (ctx == null)
				return;
			if (state == CtxState.NONE)
				return;
			if (ctx.state == CtxState.NONE) {
				setState(CtxState.NONE);
				return;
			}
			if (ctx.state == CtxState.ATCURRENT && state == CtxState.ATCURRENT)
				return;
			if (ctx.state == CtxState.OTHER && state == CtxState.OTHER) {

			} else {
				state = CtxState.OTHER;
				// vars = null;
			}
		}
	}

	/**
	 * <p>
	 * This auxiliary class encapsulates an Xpath expression, which can be
	 * formulated using variables defined using &lt;let> expressions of a
	 * Schematron &lt;rule>. Additionally there is a number indicating the XPath
	 * operator precedence of that fragment. Priorities are as follows:
	 * </p>
	 * <ol>
	 * <li>or
	 * <li>and
	 * <li>Equality operators
	 * <li>Other comparison operators
	 * <li>Infix +, -
	 * <li>*, div, mod
	 * <li>Prefix -
	 * <li>union |
	 * <li>PathExpression
	 * <li>FilterExpression id[...]
	 * <li>(bracketed expressions) or identifier
	 * </ol>
	 */
	protected static class XpathFragment {
		public int priority;
		public String fragment;
		public XpathType type;
		// public TreeMap<String, String> lets = null;
		public BindingContext atEnd = new BindingContext(
				BindingContext.CtxState.NONE);

		// Constructor from priority, type and expression
		public XpathFragment(int p, String f, XpathType t) {
			priority = p;
			fragment = f;
			type = t;
		}

		// Constructor from priority and expression. Type assumed 'nodeset'
		public XpathFragment(int p, String f) {
			priority = p;
			fragment = f;
			type = XpathType.NODESET;
		}

		// Bracket the current expression
		public void bracket() {
			fragment = "(" + fragment + ")";
			priority = 11;
		}

		// Add another fragment performing let variable merging. The argument
		// fragment is destroyed. Returned value is the merged fragment string.
		// If binding contexts are given they are also merged.
		public String merge(XpathFragment xf) {

			if (atEnd != null)
				atEnd.merge(xf.atEnd);
			return xf.fragment;
		}
	}

	/**
	 * <p>
	 * This abstract method compiles a node to an XPath expression fragment.
	 * </p>
	 * 
	 * @param ctx
	 *            BindingContext this node shall be compiled in
	 * @return Object containing the Xpath fragment
	 */
	abstract public XpathFragment translate(BindingContext ctx);

	/**
	 * ************************************************************************
	 * <p>
	 * This class stands for logical operations AND, OR, XOR and EQV. Which of
	 * these is is coded in the state member logic.
	 * </p>
	 */
	public static class Logic extends FolSchematronNode {

		protected enum LogicType {
			AND, OR, XOR, EQV
		}

		protected LogicType logic;

		/**
		 * Ctor
		 * 
		 * @param schemaObject
		 *            The schema object
		 * @param isAnd
		 *            Flag to make this an AND (true) or an OR (false)
		 */
		public Logic(FOL2Schematron schemaObject, LogicType logic) {
			this.schemaObject = schemaObject;
			this.logic = logic;
		}

		/**
		 * <p>
		 * Method to inquire whether the node inquired is a Logic node and this
		 * logic node has the same <i>isAnd</i> polarity as specified in the
		 * parameter. XORs and EQVs are ignored and yield false.
		 * <p>
		 * 
		 * @param isAnd
		 *            Flag: Are we an AND? (not an OR)?
		 * @return True if this is Logic with the same AND/OR polarity
		 */
		public boolean isAndOrLogic(boolean isAnd) {

			if (logic == LogicType.XOR || logic == LogicType.EQV)
				return false;
			return (this.logic == LogicType.AND) == isAnd;
		}

		/**
		 * <p>
		 * This compiles the node and its children to an Xpath predicate, which
		 * can be inserted into a &lt;rule>.
		 * </p>
		 * <p>
		 * AND and OR are translated into their Xpath counterparts <i>and</i>
		 * and <i>or</i>. XOR will be realized as a != operator, EQV by an =
		 * operator.
		 * </p>
		 * 
		 * @param ctx
		 *            BindingContext this node shall be compiled in
		 * @return Object containing the Xpath fragment and its operator
		 *         priority
		 */
		public XpathFragment translate(BindingContext ctx) {

			// Just one child? Pass through ...
			if (children.size() == 1)
				return children.get(0).translate(ctx);

			// Which logic particle?
			String particle = null;
			int refprio = -1;
			if (logic == LogicType.AND) {
				particle = "and";
				refprio = 2;
			} else if (logic == LogicType.OR) {
				particle = "or";
				refprio = 1;
			} else if (logic == LogicType.XOR) {
				particle = "!=";
				refprio = 3;
			} else { // EQV
				particle = "=";
				refprio = 3;
			}

			// In turn compile the node's children and con nect them with logic
			// particles. Bracket the subexpression if necessary. When
			// processing a XOR/EQV (which is implemented as !=/= of booleans)
			// do the necessary conversion to boolean first.
			XpathFragment result = null;
			result = new XpathFragment(0, "(");
			boolean first = true;

			for (FolSchematronNode ocn : children) {
				// Bracket or do the necessary boolean conversion
				XpathFragment child_xpt = ocn.translate(ctx);
				if ((logic == LogicType.XOR || logic == LogicType.EQV)
						&& child_xpt.type != XpathType.BOOLEAN) {
					child_xpt.fragment = "boolean(" + child_xpt.fragment + ")";
					child_xpt.type = XpathType.BOOLEAN;
					child_xpt.priority = 11;
				} else if (child_xpt.priority < refprio)
					child_xpt.bracket();
				// Apply operation and merge
				child_xpt.atEnd.setState(BindingContext.CtxState.NONE);
				if (first) {
					result.fragment += child_xpt.fragment;
					first = false;
				} else {
					String mrge = result.merge(child_xpt);
					result.fragment += " " + particle + " ";
					result.fragment += mrge;
				}
			}

			result.fragment += ")";

			// Correct priority and type
			result.priority = refprio;
			result.type = XpathType.BOOLEAN;
			// BindingContext at expression end is already set correctly to
			// NONE.

			return result;
		}
	}

	/**
	 * ************************************************************************
	 * <p>
	 * This class stands for comparisons. The operator is given as a String,
	 * which can take the values: =, <>, <, <=, >, >=.
	 * </p>
	 */
	public static class ComparisonNode extends FolSchematronNode {

		// The relational operator
		String opname;

		/**
		 * Ctor
		 * 
		 * @param schemaObject
		 *            The schema object
		 * @param name
		 *            One of =, <>, <, <=, >, >=
		 */
		public ComparisonNode(FOL2Schematron schemaObject, String name) {
			this.schemaObject = schemaObject;
			opname = name.equals("<>") ? "!=" : name;
		}

		/**
		 * <p>
		 * This compiles the node and its children to Xpath. Xpath can express
		 * all required comparison operators.
		 * </p>
		 * 
		 * @param ctx
		 *            BindingContext this node shall be compiled in
		 * @return Object containing the SQL fragment and its operator priority
		 */
		public XpathFragment translate(BindingContext ctx) {

			// Operator priority
			int refprio = 4;
			if (opname.equals("=") || opname.equals("!="))
				refprio = 3;

			// Check and compile children
			XpathFragment[] child_xpt = new XpathFragment[2];

			for (int i = 0; i < 2; i++) {

				FolSchematronNode child = children.get(i);

				if (!child.hasSimpleType() && !child.hasIdentity()) {
					return new XpathFragment(11, "***ERROR[126]***");
				}

				child_xpt[i] = child.translate(ctx);

				if (child.hasIdentity()) {
					child_xpt[i].fragment = "generate-id("
							+ child_xpt[i].fragment + ")";
					child_xpt[i].priority = 11;
				}

				if (child_xpt[i].fragment.length() == 0) {
					child_xpt[i].fragment = ".";
				}

				if (child_xpt[i].priority <= refprio) {
					child_xpt[i].bracket();
				}
				child_xpt[i].atEnd.setState(BindingContext.CtxState.NONE);
			}

			// Construct the result
			String op2 = child_xpt[0].merge(child_xpt[1]);
			child_xpt[0].fragment += " " + opname + " " + op2;
			child_xpt[0].type = XpathType.BOOLEAN;
			child_xpt[0].priority = refprio;

			return child_xpt[0];
		}
	}

	/**
	 * ************************************************************************
	 * <p>
	 * This one stands for the OCL <i>isEmpty()</i> and <i>notEmpty()</i>
	 * predicate operations. Which of these is meant is expressed in the state
	 * variable <i>negated</i>.
	 * </p>
	 */
	public static class Empty extends FolSchematronNode {

		// negated means: isEmpty (false) or notEmpty (true)

		/**
		 * Ctor
		 * 
		 * @param schemaObject
		 *            The schema object
		 */
		public Empty(FOL2Schematron schemaObject) {
			this.schemaObject = schemaObject;
		}

		/**
		 * <p>
		 * This compiles the node and its children to an Xpath fragment. The
		 * translation is essentially the nodeset derived from the object part
		 * of the expression, because notEmpty() is fulfilled for a nodeset,
		 * which converts to a boolean true. isEmpty() requires an additional
		 * not().
		 * </p>
		 * 
		 * @param ctx
		 *            BindingContext this node shall be compiled in
		 * @return Object containing the fragment and its operator priority
		 */
		public XpathFragment translate(BindingContext ctx) {

			// Fetch and compile the object
			FolSchematronNode obj = children.get(0);
			XpathFragment xpt = obj.translate(ctx);
			if (xpt.fragment.length() == 0)
				xpt.fragment = ".";

			// isEmpty() requires an additional not(...)
			xpt.fragment = "not(" + xpt.fragment + ")";
			xpt.type = XpathType.BOOLEAN;
			xpt.priority = 11;
			xpt.atEnd.setState(BindingContext.CtxState.NONE);

			return xpt;
		}
	}

	/**
	 * ************************************************************************
	 * <p>
	 * </p>
	 */
	public static class NotNode extends FolSchematronNode {

		/**
		 * Ctor
		 * 
		 * @param schemaObject
		 *            The schema object
		 */
		public NotNode(FOL2Schematron schemaObject) {
			this.schemaObject = schemaObject;
		}

		public XpathFragment translate(BindingContext ctx) {

			// Fetch and compile the object
			FolSchematronNode obj = children.get(0);
			XpathFragment xpt = obj.translate(ctx);

			xpt.fragment = "not(" + xpt.fragment + ")";
			xpt.type = XpathType.BOOLEAN;
			xpt.priority = 11;
			xpt.atEnd.setState(BindingContext.CtxState.NONE);

			return xpt;
		}
	}

	/**
	 * ************************************************************************
	 * This class represents the isNull filter.
	 */
	public static class IsNullNode extends FolSchematronNode {

		// Stored generator body
		protected FolSchematronNode generatorBody = null;

		/**
		 * Ctor
		 * 
		 * @param schemaObject
		 *            The schema object
		 */
		public IsNullNode(FOL2Schematron schemaObject) {
			this.schemaObject = schemaObject;
		}

		public XpathFragment translate(BindingContext ctx) {

			// Fetch and compile the object
			FolSchematronNode obj = children.get(0);
			XpathFragment xptobj = obj.translate(ctx);

			// Note: function nilled() is not a good fit here, because it
			// requires the XML Schema of the input XML to be referenced in the
			// instance (evaluation of the nilled function apparently requires
			// this)

			if (obj instanceof AttributeNode || obj instanceof VariableNode) {

				AttributeNode atn = obj.generatingAttribute();

				if (atn != null && atn.implementedAsXmlAttribute()) {

					XpathFragment result = new XpathFragment(0, "");
					result.fragment += "not(string-length(normalize-space(";
					result.fragment += xptobj.fragment;
					result.fragment += ")))";

					return result;
				}
			}

			xptobj.fragment += "[@xsi:nil='true']";
			schemaObject.registerNamespace("xsi");
			return xptobj;
		}
	}

	/**
	 * ************************************************************************
	 * This class represents a type check. The type is given by the first of the
	 * children.
	 */
	public static class IsTypeOfNode extends FolSchematronNode {

		// The class to be tested against
		protected ClassInfo argumentClass = null;
		protected Variable var = null;

		/**
		 * Ctor
		 * 
		 * @param schemaObject
		 *            The schema object
		 */
		public IsTypeOfNode(FOL2Schematron schemaObject) {
			this.schemaObject = schemaObject;
		}

		/**
		 * If the class to be tested against is already known (it is not an
		 * expression) this reference can be set via this method.
		 * 
		 * @param ci
		 *            ClassInfo representing the type to be tested against
		 */
		public void setClass(ClassInfo ci) {
			argumentClass = ci;
		}

		public void setVariable(Variable var) {
			this.var = var;
		}

		/**
		 * <p>
		 * This compiles the FOL IsTypeOf predicate to an equivalent Xpath
		 * expression fragment. It is translated to a predicate which compares
		 * the element name against all concrete subtypes of the given type.
		 * </p>
		 * 
		 * @param ctx
		 *            BindingContext this node shall be compiled in
		 * @return Object containing the Xpath fragment
		 */
		public XpathFragment translate(BindingContext ctx) {

			XpathFragment xptobj = objValueFromVariable(this.var);

			boolean emptyobject = xptobj.fragment.length() == 0;

			/*
			 * TBD: a schema aware XPath processor could also leverage the
			 * schema-element() function
			 */

			// Obtain the necessary classes from the model
			TreeSet<String> classnames = new TreeSet<String>();

			SortedSet<ClassInfo> subtypes = argumentClass.subtypesInCompleteHierarchy();

			for (ClassInfo subtype : subtypes) {
				if (subtype.isAbstract())
					continue;
				classnames.add(schemaObject.getAndRegisterXmlName(subtype));
			}			

			if (!argumentClass.isAbstract())
				classnames.add(schemaObject
						.getAndRegisterXmlName(argumentClass));

			// Construct the result expression
			if (classnames.size() == 0) {

				// There is no concrete class known. So, this must be false.
				xptobj.fragment = "false()";
				xptobj.priority = 11;
				xptobj.type = XpathType.BOOLEAN;
				xptobj.atEnd.setState(BindingContext.CtxState.NONE);

			} else {

				// There are one or more classes, append a predicate which
				// compares against all possible names.
				boolean first = true;
				if (xptobj.priority < 9) {
					xptobj.bracket();
					xptobj.priority = 11;
				}
				if (!emptyobject) {
					xptobj.fragment += "[";
				}
				for (String name : classnames) {
					if (!first)
						xptobj.fragment += " or ";
					first = false;
					xptobj.fragment += "name()='" + name + "'";
				}
				xptobj.priority = 3;
				if (classnames.size() > 1) {
					xptobj.priority = 1;
				}
				if (!emptyobject) {
					xptobj.fragment += "]";
					xptobj.priority = 10;
				}
			}

			return xptobj;
		}
	}

	/**
	 * ************************************************************************
	 * This class stands for matches operation, which this implemention added to
	 * OCL's core functions.
	 */
	public static class Matches extends FolSchematronNode {

		/**
		 * Ctor
		 * 
		 * @param schemaObject
		 *            The schema object
		 */
		public Matches(FOL2Schematron schemaObject) {
			this.schemaObject = schemaObject;
		}

		/**
		 * <p>
		 * Matches operations are translated to an appropriate extension
		 * function (XPath 1.0) or directly to Xpath 2.0.
		 * </p>
		 * 
		 * @param ctx
		 *            BindingContext this node shall be compiled in
		 * @return Object containing the Xpath fragment
		 */
		public XpathFragment translate(BindingContext ctx) {

			// Translate the arguments
			XpathFragment xptobj = children.get(0).translate(ctx);
			if (xptobj.fragment.length() == 0)
				xptobj.fragment = ".";
			XpathFragment xptpat = children.get(1).translate(ctx);
			if (xptpat.fragment.length() == 0)
				xptpat.fragment = ".";

			// Merge
			String patstring = xptobj.merge(xptpat);

			// Fetch the extension template
			FOL2Schematron.ExtensionFunctionTemplate eft = schemaObject.extensionFunctions
					.get("matches");
			if (eft == null)
				return new XpathFragment(11, "***ERROR[123]***");

			// Construct the extension function call
			String fcall = eft.function.replace("$object$", xptobj.fragment)
					.replace("$pattern$", patstring);
			xptobj.fragment = eft.nsPrefix + ":" + fcall;

			// Accompany with the required fragment attributes
			xptobj.type = XpathType.STRING;
			xptobj.priority = 11;
			xptobj.atEnd.setState(BindingContext.CtxState.NONE);

			// We need to declare the namespace
			schemaObject.registerNamespace(eft.nsPrefix, eft.namespace);

			return xptobj;
		}
	}

	/**
	 * ************************************************************************
	 * This class stands for OCL arithmetic.
	 */
	public static class Arithmetic extends FolSchematronNode {

		/** The operation */
		String operation;

		/**
		 * Ctor
		 * 
		 * @param schemaObject
		 *            The schema object
		 * @param oper
		 *            The operation symbol, one of + ,-, *, /
		 */
		public Arithmetic(FOL2Schematron schemaObject, String oper) {
			this.schemaObject = schemaObject;
			this.operation = oper;
		}

		/**
		 * <p>
		 * This compiles a node to an Xpath expression, which realizes the given
		 * arithmetic operation. OCL and Xpath are very similar here.
		 * </p>
		 * 
		 * @param ctx
		 *            BindingContext this node shall be compiled in
		 * @return Object containing the Xpath fragment
		 */
		public XpathFragment translate(BindingContext ctx) {

			// One argument to be compiled in any case
			XpathFragment xpt1 = children.get(0).translate(ctx);

			// Take cases on operation and number of operands
			if (children.size() == 1) {

				// Must be prefix -
				if (xpt1.priority <= 7)
					xpt1.bracket();
				xpt1.priority = 7;

			} else {

				// Two arguments. Get the second
				XpathFragment xpt2 = children.get(1).translate(ctx);
				// Find priority
				int prio = 5;
				if (operation.equals("*") || operation.equals("/"))
					prio = 6;
				// Do the necessary bracketing
				if (xpt1.priority < prio)
					xpt1.bracket();
				if (operation.equals("/") || operation.equals("-")) {
					if (xpt2.priority <= prio)
						xpt2.bracket();
				} else {
					if (xpt2.priority < prio)
						xpt2.bracket();
				}
				// Merge
				xpt2.atEnd = null; // No need to merge ending contexts
				String op2 = xpt1.merge(xpt2);
				xpt1.fragment += " "
						+ (operation.equals("/") ? "div" : operation) + " "
						+ op2;
				xpt1.priority = prio;

			}

			xpt1.type = XpathType.NUMBER;
			xpt1.atEnd = new BindingContext(BindingContext.CtxState.NONE);
			return xpt1;
		}
	}

	/**
	 * ************************************************************************
	 * This class represents a variable. It wraps a Variable object.
	 */
	public static class VariableNode extends FolSchematronNode {

		// Wrapped declaration object
		protected Variable vardecl;
		private FolSchematronNode value;

		/**
		 * Ctor
		 * 
		 * @param schemaObject
		 *            The schema object
		 * @param vardecl
		 *            Variable object
		 */
		public VariableNode(FOL2Schematron schemaObject, Variable var) {
			this.schemaObject = schemaObject;
			this.vardecl = var;
		}

		/**
		 * Variable name inquiry function.
		 * 
		 * @return Name of variable
		 */
		public String getName() {
			return vardecl.getName();
		}

		/**
		 * <p>
		 * This method determines whether this variable is identical to the one
		 * passed as argument.
		 * </p>
		 * 
		 * @param vardecl
		 *            The Variable of the variable
		 * @return Flag indicating the inquired dependency
		 */
		public boolean isDependentOn(Variable vardecl) {
			return this.vardecl == vardecl;
		}

		/**
		 * <p>
		 * This method determines whether this variable is identical to the one
		 * passed as argument.
		 * </p>
		 * 
		 * @param vardecl
		 *            The Variable of the variable
		 * @return Flag indicating the inquired property
		 */
		public boolean isVarOrAttribBased(Variable vardecl) {
			return isDependentOn(vardecl);
		}

		/**
		 * <p>
		 * Finds the generating attribute of the variable (searching in outer
		 * scopes if necessary) - it may be null if the variable is purely based
		 * on other variables without actual value (also in case it is 'self').
		 * </p>
		 * 
		 * @return The retrieved Attribute node if there is such a thing
		 */
		public AttributeNode generatingAttribute() {

			// do we have a value?
			if (value != null) {
				// look up generatingAttribute in the value
				return value.generatingAttribute();

			} else {

				if (vardecl.getNextOuterScope() != null) {

					return this.schemaObject.varNodesByVarName.get(
							vardecl.getNextOuterScope().getName())
							.generatingAttribute();

				} else {

					// this can happen if this variable is 'self'
					return null;
				}
			}
		}

		/**
		 * <p>
		 * This predicate finds out whether the Variable results in a simple XML
		 * schema type.
		 * </p>
		 * 
		 * @return Flag indicating whether the node has a simple type
		 */
		public boolean hasSimpleType() {

			if (value != null) {

				if (value instanceof AttributeNode) {

					AttributeNode an = (AttributeNode) value;
					return an.hasSimpleType();

				} else {

					// TODO
					// System.out
					// .println("ERROR - no AttributeNode in child of VariableNode");
					return super.hasSimpleType();
				}

			} else {

				if (vardecl.getNextOuterScope() != null) {

					return this.schemaObject.varNodesByVarName.get(
							vardecl.getNextOuterScope().getName())
							.hasSimpleType();
				} else {

					/*
					 * this variable points to self, which must be of complex
					 * type
					 */
					return false;
				}
			}
		}

		/**
		 * <p>
		 * This predicate finds out whether the Variable results in an instance,
		 * which conceptually has identity.
		 * </p>
		 * 
		 * @return Flag indicating whether the node is an identity carrying type
		 */
		public boolean hasIdentity() {

			if (value != null) {

				if (value instanceof AttributeNode) {
					AttributeNode an = (AttributeNode) value;
					return an.hasIdentity();

				} else {
					// TODO
					// System.out
					// .println("ERROR - no AttributeNode in child of VariableNode");
					return super.hasIdentity();
				}

			} else {

				if (vardecl.getNextOuterScope() != null) {

					return this.schemaObject.varNodesByVarName.get(
							vardecl.getNextOuterScope().getName())
							.hasIdentity();
				} else {

					/*
					 * this variable points to self, which we assume has
					 * identity TBD: would FOL constraints be written for
					 * classes that do not have identity?
					 */
					return true;
				}
			}
		}

		/**
		 * <p>
		 * This compiles a node to an Xpath expression, which stands for the
		 * given variable.
		 * </p>
		 * <p>
		 * If the variable is defined in a surrounding 'let' construct, a proper
		 * translation for the use of the variable can always be achieved, given
		 * that the initial value of the variable translates properly. If the
		 * use of the variable is in ISCURRENT context, the variable definition
		 * will be mapped into a Schematron &lt;let&gt; definition. Otherwise
		 * the initial value is substituted in place of the variable.
		 * </p>
		 * <p>
		 * Other variable references are treated as follows.
		 * </p>
		 * <p>
		 * The only variable which can be properly translated in all cases is
		 * <i>self</i>, which will be mapped to <i>current()</i> or to '.', if
		 * compiled in a ISCURRENT context. Variable definitions from iterators
		 * require to be on the context stack of the expression, which is widely
		 * dependent on how the expression environment could be represented in
		 * Xpath.
		 * </p>
		 * 
		 * @param ctx
		 *            BindingContext this node shall be compiled in
		 * @return Object containing the Xpath fragment
		 */
		public XpathFragment translate(BindingContext ctx) {

			// Initialize a PathFragment
			XpathFragment xpt = new XpathFragment(11, "");

			String varContext = "current()";

			if (!vardecl.getName().equals(Variable.SELF_VARIABLE_NAME)) {
				varContext = "$" + vardecl.getName();
			}
			xpt.fragment += varContext;

			return xpt;
		}

		public void setValue(FolSchematronNode varValue) {
			this.value = varValue;
		}

		public FolSchematronNode value() {
			return value;
		}
	}

	/**
	 * ************************************************************************
	 * <p>
	 * This class represents a chain of attribute selectors based on some value
	 * source such as a variable. The value source is the sole child of the
	 * Attribute object.
	 * </p>
	 */
	public static class AttributeNode extends FolSchematronNode {

		protected static class AttrComp {
			protected PropertyCall main = null;
			protected PropertyCall absAttr = null;
			protected int absType = 0; // 0=normal, 1=absorption, 2=nilReason

			protected AttrComp(PropertyCall pc) {
				main = pc;
			}

			protected AttrComp(AttrComp atc) {
				main = atc.main;
				absAttr = atc.absAttr;
				absType = atc.absType;
			}
		}

		protected AttrComp[] attributes;

		protected VariableNode var;

		/**
		 * Ctor - starting from PropertyCall
		 * 
		 * @param schemaObject
		 *            The schema object
		 * @param attr
		 *            The (possibly first) PropertyCall object
		 */
		public AttributeNode(FOL2Schematron schemaObject, PropertyCall pc) {
			this.schemaObject = schemaObject;
			this.attributes = new AttrComp[] { new AttrComp(pc) };
		}

		/**
		 * Ctor - initialisation without any property call
		 * 
		 * @param schemaObject
		 *            The schema object
		 */
		public AttributeNode(FOL2Schematron schemaObject) {
			this.schemaObject = schemaObject;
			this.attributes = new AttrComp[] {};
		}

		/**
		 * Ctor - starting from AttrComp
		 * 
		 * @param schemaObject
		 *            The schema object
		 * @param atc
		 *            AttrComp object
		 */
		public AttributeNode(FOL2Schematron schemaObject, AttrComp atc) {
			this.schemaObject = schemaObject;
			this.attributes = new AttrComp[] { new AttrComp(atc) };
		}

		/**
		 * <p>
		 * Append another PropertyCall and associated layout info as an
		 * additional qualification.
		 * </p>
		 * 
		 * @param aex
		 *            The PropertyCall to be appended be null)
		 */
		public void appendAttribute(PropertyCall pc) {

			AttrComp[] attribs = new AttrComp[attributes.length + 1];

			for (int i = 0; i < attributes.length; i++) {
				attribs[i] = attributes[i];
			}

			attribs[attributes.length] = new AttrComp(pc);
			attributes = attribs;
		}

		/**
		 * <p>
		 * Append another AttrComp and associated layout info as an additional
		 * qualification.
		 * </p>
		 * 
		 * @param aex
		 *            The AttrComp object to be appended be null)
		 */
		public void appendAttribute(AttrComp atc) {

			AttrComp[] attribs = new AttrComp[attributes.length + 1];

			for (int i = 0; i < attributes.length; i++) {
				attribs[i] = attributes[i];
			}

			attribs[attributes.length] = new AttrComp(atc);
			attributes = attribs;
		}

		/**
		 * This method marks the last attribute component as being absorbed by
		 * the construct before. This also includes nilReason implementation.
		 * The type (1=simple absorption, 2=reason) and the associated
		 * PropertyCall are stored together with the last attribute component.
		 * 
		 * @param absorptionType
		 *            Implementation type: 1=normal absorption, 2=reason
		 * @param pc
		 *            The PropertyCall representing the absorbed property.
		 */
		public void appendAbsorbedAttribute(int absorptionType, PropertyCall pc) {
			int last = attributes.length - 1;
			attributes[last].absAttr = pc;
			attributes[last].absType = absorptionType;
		}

		/**
		 * Split the Attribute object before the given index.
		 * 
		 * @param at
		 *            The index before to split
		 * @return Right hand part of the split, which contains the left hand
		 *         part as its child.
		 */
		public AttributeNode splitBefore(int at) {
			if (at < 0 || at >= attributes.length)
				return null;
			// Create a new Attribute node, which contains the selectors
			// starting at the given position. This will also be the
			// returned value.
			AttributeNode atrite = new AttributeNode(schemaObject,
					attributes[at]);
			for (int i = at + 1; i < attributes.length; i++)
				appendAttribute(attributes[i]);
			// Now treat everything left to the given position and make
			// it the object child of attribute created formerly.
			if (at == 0) {
				// There is nothing but the object, So, transfer this.
				atrite.addChild(children.get(0));
			} else {
				// Create another Attribute with the selectors in front of the
				// splitting point. Make it the object of the right hand one.
				AttributeNode atleft = new AttributeNode(schemaObject,
						attributes[0]);
				for (int i = 1; i < at; i++)
					appendAttribute(attributes[i]);
				atleft.addChild(children.get(0));
				atrite.addChild(atleft);
			}
			return atrite;
		}

		/**
		 * <p>
		 * This Attribute predicate finds out if the last attribute component in
		 * the object is implemented as a group and is therefore absorbing its
		 * properties. If there is already a property absorbed on the attribute,
		 * the absorbed property will be asked.
		 * </p>
		 * <p>
		 * Note that this is a necessary condition for applying GML's nilReason
		 * pattern.
		 * </p>
		 * 
		 * @return The required flag indicating that properties are absorbed
		 */
		public boolean isPropertyAbsorbing() {

			if (attributes.length == 0) {

				if (this.var != null) {

					AttributeNode atnFromVar = var.generatingAttribute();
					if (atnFromVar != null) {
						return atnFromVar.isPropertyAbsorbing();
					}
				}

			} else {

				AttrComp ac = attributes[attributes.length - 1];
				PropertyCall pc = ac.absType == 0 ? ac.main : ac.absAttr;
				PropertyInfo pi = pc.getSchemaElement();
				Type t = pi.typeInfo();
				ClassInfo ci = schemaObject.model.classById(t.id);
				if (ci != null) {
					if (ci.isUnionDirect())
						return true;
				}
			}

			return false;
		}

		/**
		 * <p>
		 * This method determines whether this Attribute is dependent on the
		 * Variable passed as argument.
		 * </p>
		 * 
		 * @param vardecl
		 *            The Variable of the variable
		 * @return Flag indicating the inquired property
		 */
		public boolean isVarOrAttribBased(Variable vardecl) {
			return var.isVarOrAttribBased(vardecl);
		}

		/**
		 * <p>
		 * This inquires the Attribute node this Attribute is generated by.
		 * Alas, it's this Attribute!
		 * </p>
		 * 
		 * @return The retrieved Attribute node
		 */
		public AttributeNode generatingAttribute() {
			return this;
		}

		/**
		 * <p>
		 * This method returns true if any of the PropertyCall objects it is
		 * made of has a maximum cardinality greater than 1.
		 * </p>
		 */
		public boolean isMultiple() {

			for (AttrComp at : attributes) {

				if (at.main.getSchemaElement().cardinality().maxOccurs > 1
						|| at.absType == 1
						&& at.absAttr.getSchemaElement().cardinality().maxOccurs > 1)
					return true;
			}

			return false;
		}

		/**
		 * <p>
		 * This predicate finds out whether the Attribute as a whole results in
		 * a simple XML schema type. Note that for convenience reasons this also
		 * includes the GML's xsi:nil construct.
		 * </p>
		 * 
		 * @return Flag indicating whether the Attribute has a simple type
		 */
		public boolean hasSimpleType() {
			int last = attributes.length - 1;
			return hasSimpleType(last);
		}

		public boolean implementedAsXmlAttribute() {
			int last = attributes.length - 1;
			return implementedAsXmlAttribute(last);
		}

		public boolean implementedAsXmlAttribute(int idx) {

			boolean result = false;

			switch (attributes[idx].absType) {

			case 0: // Normal attribute
				result = implementedAsXmlAttribute(attributes[idx].main
						.getSchemaElement());
				break;
			case 1: // Normal absorption
				result = implementedAsXmlAttribute(attributes[idx].absAttr
						.getSchemaElement());
				break;
			case 2: // Nil-implementation attribute with a "reason" selector
			}

			return result;
		}

		public boolean implementedAsXmlAttribute(PropertyInfo pi) {

			boolean result = XmlSchema.implementedAsXmlAttribute(pi);

			return result;
		}

		/**
		 * <p>
		 * This predicate finds out whether the Attribute component at the given
		 * index <i>idx</i> results in a simple XML schema type. Note that for
		 * convenience reasons this also includes the GML's xsi:nil construct.
		 * </p>
		 * 
		 * @param idx
		 *            Index of the attribute component
		 * @return Flag indicating whether the attribute component has a simple
		 *         type
		 */
		public boolean hasSimpleType(int idx) {

			boolean result = true;

			switch (attributes[idx].absType) {

			case 0: // Normal attribute
				result = hasSimpleType(attributes[idx].main.getSchemaElement());
				break;
			case 1: // Normal absorption
				result = hasSimpleType(attributes[idx].absAttr
						.getSchemaElement());
				break;
			case 2: // Nil-implementation attribute with a "reason" selector
			}

			return result;
		}

		/**
		 * <p>
		 * This predicate finds out whether the Attribute as a whole results in
		 * instances, which conceptually have identity.
		 * </p>
		 * 
		 * @return Flag indicating whether the node is an identity carrying type
		 */
		public boolean hasIdentity() {
			int last = attributes.length - 1;
			return hasIdentity(last);
		}

		/**
		 * <p>
		 * This predicate finds out whether the Attribute component at the given
		 * index <i>idx</i> results in a schema type that carries identity. Note
		 * that for convenience reasons this also includes GML's xsi:nil
		 * construct.
		 * </p>
		 * 
		 * @param idx
		 *            Index of the attribute component
		 * @return Flag indicating whether the attribute component has identity
		 */
		public boolean hasIdentity(int idx) {

			ClassInfo ci;
			boolean result = false;

			switch (attributes[idx].absType) {

			case 0: // Normal attribute
				ci = getTypeClassInfo(attributes[idx].main.getSchemaElement());
				if (ci != null)
					result = XmlSchema.classCanBeReferenced(ci);
				break;
			case 1: // Normal absorption
				ci = getTypeClassInfo(attributes[idx].absAttr
						.getSchemaElement());
				if (ci != null)
					result = XmlSchema.classCanBeReferenced(ci);
				break;
			case 2: // Nil-implementation attribute with a "reason" selector
			}

			return result;
		}

		/**
		 * <p>
		 * This function translates the Attribute to an Xpath fragment accessing
		 * that attribute.
		 * </p>
		 * 
		 * @param ctx
		 *            BindingContext this node shall be compiled in
		 * @return XpathFragment representing the Attribute access
		 */
		public XpathFragment translate(BindingContext ctx) {

			// Translate the variable.
			XpathFragment obj = objValueFromVariable(this.var.vardecl);

			// Get the prefixes that possibly surround the identifier contained
			// in xlink:href references
			String alpha = schemaObject.alpha;
			String beta = schemaObject.beta;
			boolean alphaEx = alpha != null && alpha.length() > 0;
			boolean betaEx = beta != null && beta.length() > 0;

			// --- translate the value represented in the attributes[]

			// obtain the feature attribute expressed by PropertyInfo
			// objects.
			PropertyInfo[] props = new PropertyInfo[attributes.length];

			for (int i = 0; i < attributes.length; i++) {
				props[i] = attributes[i].main.getSchemaElement();
			}

			// Now step along the properties and generate the associated Xpath
			// code for it.
			boolean lastWasSimple = false;

			/*
			 * We translate all properties including the first with simple type
			 */
			for (int idx = 0; idx < props.length; idx++) {

				PropertyInfo pi = props[idx];

				// special treatment for properties encoded as XML attributes
				if (XmlSchema.implementedAsXmlAttribute(pi)) {

					if (obj.fragment.length() > 0) {
						obj.fragment += "/";
						// TODO - correct priority??
						obj.priority = 9;
					}

					obj.fragment += "@" + pi.name();

					/*
					 * now jump out of the loop because we cannot get any deeper
					 * in the path if we have reached an XML attribute
					 */
					break;
				}

				if (lastWasSimple) {

					/*
					 * If the last property was a simple type then we'll break
					 * the loop. But first we determine if any specific case
					 * must still be handled
					 */

					/*
					 * If this property is named 'uom' we may want to apply a
					 * special encoding that allows us to access the 'uom' as an
					 * attribute
					 */
					if (XmlSchema.implementedAsXmlAttribute(pi)) {

						if (obj.fragment.length() > 0) {
							obj.fragment += "/";
							// TODO - correct priority??
							obj.priority = 9;
						}

						obj.fragment += "@" + pi.name();
					}

					/*
					 * now jump out of the loop because we cannot get any deeper
					 * in the path if we have reached an XML attribute
					 */
					break;
				}

				/*
				 * Find out how the relation to the contained object is realized
				 * in XML Schema: 0 = simple, 1 = inline, 2 = byReference, 3 =
				 * inlineOrByReference
				 */
				int conCode = SIMPLE_TYPE;

				String typeId = pi.typeInfo().id;
				ClassInfo ci = null;
				ClassInfo cip = null;

				if (typeId != null)
					cip = ci = pi.model().classById(typeId);

				// If absorbing assume the type of the absorbed entity
				if (attributes[idx].absType == 1)
					ci = getTypeClassInfo(attributes[idx].absAttr
							.getSchemaElement());

				if (!hasSimpleType(idx)) {
					// Not a simple type, assume in-line
					conCode = INLINE;
					if (ci != null && XmlSchema.classCanBeReferenced(ci)) {
						String ref = pi.inlineOrByReference();
						if (ref == null)
							ref = "";
						if (ref.equalsIgnoreCase("byreference"))
							conCode = BY_REFERENCE;
						else if (!ref.equalsIgnoreCase("inline"))
							conCode = INLINE_OR_BY_REFERENCE;
					}
				}

				// take into account AIXM <<extension>> encoding
				if (isAIXMExtension(pi)) {

					if (obj.fragment.length() > 0) {
						obj.fragment += "/";
						// TODO - correct priority??
						obj.priority = 9;
					}

					obj.fragment += schemaObject.getAndRegisterXmlns(pi
							.inClass()) + ":extension/*";

				}

				// Namespace and namespace adorned property
				String proper = schemaObject.getAndRegisterXmlName(pi);

				// Dispatch on containment cases
				if (conCode == SIMPLE_TYPE) {

					// 0: Simple type is contained. This also comprises
					// access to property 'reason' in GML's nilReason
					// treatment.

					if (obj.fragment.length() > 0) {
						obj.fragment += "/";
						obj.priority = 9;
					}
					obj.fragment += proper;

					/*
					 * We will have to know, whether we are subject to the
					 * 19139, regime, so go and find out
					 */
					boolean is19139 = pi.matches("rule-xsd-all-naming-19139")
							|| (ci != null && ci
									.matches("rule-xsd-cls-standard-19139-property-types"));

					/*
					 * We also need to know, whether the property has a codelist
					 * type and whether it is going to be treated according to
					 * the GML 3.3 way
					 */
					boolean iscodelist = ci != null
							&& ci.category() == Options.CODELIST
							&& ((ci.matches("rule-xsd-cls-codelist-asDictionaryGml33") && ci
									.asDictionaryGml33()) || (ci
									.matches("rule-xsd-cls-codelist-asDictionary") && ci
									.asDictionary()));

					// If its a codelist and not GML 3.3 we need to prepare the
					// CodeListValuePattern.
					String clvpat = "{value}";
					// int nsubst = 1;
					if (ci != null
							&& iscodelist
							&& !(schemaObject.options.matchesEncRule(
									pi.encodingRule("xsd"), "gml33") || is19139)) {
						String uri = ci.taggedValue("codeList");
						if (uri != null && uri.length() > 0) {
							clvpat = "{codeList}/{value}";
						}
						String vp = ci.taggedValue("codeListValuePattern");
						if (vp != null && vp.length() > 0)
							clvpat = vp;
						clvpat = clvpat.replace("{codeList}", "',{codeList},'");
						clvpat = clvpat.replace("{value}", "',{value},'");
						if (clvpat.startsWith("',"))
							clvpat = clvpat.substring(2);
						if (clvpat.endsWith(",'"))
							clvpat = clvpat.substring(0, clvpat.length() - 2);
						if (!clvpat.startsWith("{"))
							clvpat = "'" + clvpat;
						if (!clvpat.endsWith("}"))
							clvpat += "'";
						if (!clvpat.equals("{value}")) {
							clvpat = "concat(" + clvpat + ")";
							// nsubst = 2;
						}
					}

					// 'reason' access in GML or 19139 nilReason treatment?
					if (attributes[idx].absType == 2) {
						if (is19139) {
							obj.fragment += "[not(*)]/@gco:nilReason";
							schemaObject.registerNamespace("gco");
						} else {
							obj.fragment += "[@xsi:nil='true']/@nilReason";
							schemaObject.registerNamespace("xsi");
						}
						obj.priority = 9;
					}

					// Nillable 'value' access in GML
					if (attributes[idx].absType == 1) {
						if (cip.isUnionDirect()) {
							obj.fragment += "[not(@xsi:nil='true')]";
							schemaObject.registerNamespace("xsi");
						}
					}

					// In a normal property access, we still have to treat some
					// special cases ...
					if (attributes[idx].absType == 0) {
						if (is19139) {
							/*
							 * Under 19139 encoding, we will have to match
							 * another element level, which is for carrying the
							 * type, but does not concern us a lot ...
							 */
							obj.fragment += "/*";

							// For codelists we have to add an attribute access
							if (iscodelist) {

								String clvp = clvpat.replace("{codeList}",
										obj.fragment + "/@codeList");
								obj.fragment = clvp.replace("{value}",
										obj.fragment + "/@codeListValue");
							}

						} else if (iscodelist) {

							if (!ci.matches("rule-xsd-cls-codelist-asDictionaryGml33")) {
								// In elder GMLs we might find the codespace in
								// the codespace attribute
								String clvp = clvpat.replace("{codeList}",
										obj.fragment + "/@codeSpace");
								obj.fragment = clvp.replace("{value}",
										obj.fragment);
							} else {
								/*
								 * If using GML 3.3 type codelist treatment, we
								 * have to refer to the xlink:href attribute
								 */
								obj.fragment += "/@xlink:href";
								schemaObject.registerNamespace("xlink");
							}
						}

					}

					// No need to process attributes any further, because we
					// just found out this one was mapped to a simple type.
					lastWasSimple = true;

				} else {

					// Other: different modes of containment are possible

					/*
					 * If the current property is the last property, we simply
					 * refer to it, NOT also its value (the value will be
					 * computed when it is actually accessed, i.e. at the start
					 * of other AttributeNode translations). This is quite
					 * similar to the inline encoding, but again, we don't
					 * access the value itself.
					 */
					if (idx == props.length - 1) {

						String frag_propAccess = null;

						frag_propAccess = obj.fragment;
						frag_propAccess += "/";
						frag_propAccess += proper;

						obj.fragment = frag_propAccess;
						obj.priority = 9;

					} else {

						String frag_inl = null;
						String frag_ref = null;

						if (conCode == INLINE
								|| conCode == INLINE_OR_BY_REFERENCE) {

							// In-line containment must be treated
							// --> .../attr/*
							frag_inl = obj.fragment;
							frag_inl += "/";
							frag_inl += proper;
							frag_inl += "/*";
						}

						if (conCode == BY_REFERENCE
								|| conCode == INLINE_OR_BY_REFERENCE) {

							String attxlink = obj.fragment;
							if (attxlink.length() > 0)
								attxlink += "/";
							attxlink += proper;
							attxlink += "/@xlink:href";
							frag_ref = "//*[";
							if (alphaEx || betaEx) {
								frag_ref += "concat(";
								if (alphaEx)
									frag_ref += "'" + alpha + "',";
								frag_ref += "@gml:id";
								if (betaEx)
									frag_ref += ",'" + beta + "'";
								frag_ref += ")";
							} else {
								frag_ref += "@gml:id";
							}
							frag_ref += "=" + attxlink + "]";
							schemaObject.registerNamespace("xlink");
							schemaObject.registerNamespace("gml");

							// Whatever binding context we had before, it is
							// lost
							if (obj.atEnd != null)
								obj.atEnd
										.setState(BindingContext.CtxState.OTHER);
						}

						// Set the fragment value, possibly combining both
						// containment representations
						if (conCode == INLINE_OR_BY_REFERENCE) {
							obj.fragment = frag_inl + " | " + frag_ref;
							obj.priority = 8;
						} else if (conCode == INLINE) {
							obj.fragment = frag_inl;
							obj.priority = 9;
						} else {
							obj.fragment = frag_ref;
							obj.priority = 10;
						}
					}
				}
			}

			// Return fragment
			return obj;
		}

		public void setVariable(VariableNode varnode) {
			this.var = varnode;
		}
	}

	private static ClassInfo getTypeClassInfo(PropertyInfo pi) {

		if (pi == null) {
			return null;
		} else {
			Model m = pi.model();
			return m.classById(pi.typeInfo().id);
		}
	}

	/**
	 * ************************************************************************
	 * This wraps any form of Literal value.
	 */
	public static class LiteralNode extends FolSchematronNode {

		Literal literal;

		/**
		 * Ctor
		 * 
		 * @param schemaObject
		 *            The schema object
		 * @param lit
		 *            OclNode.LiteralExp object
		 */
		public LiteralNode(FOL2Schematron schemaObject, Literal lit) {
			this.schemaObject = schemaObject;
			literal = lit;
		}

		/**
		 * <p>
		 * This function translates the Literal to equivalent Xpath code.
		 * </p>
		 * 
		 * @param ctx
		 *            BindingContext this node shall be compiled in
		 * @return XpathFragment representing the literal
		 */
		public XpathFragment translate(BindingContext ctx) {

			// As a first guess retrieve the string value from the literal
			String value = literal.toString();
			XpathType type = XpathType.STRING;

			// Some special treatments
			if (literal instanceof StringLiteral) {

				// If string, surround with ''
				value = "'" + ((StringLiteral) literal).getValue() + "'";

			} else if (literal instanceof StringLiteralList) {

				StringLiteralList sll = (StringLiteralList) literal;

				List<String> values = sll.getValues();

				if (values == null || values.isEmpty()) {
					value = "()";
				} else {
					value = "('" + StringUtils.join(values, "','") + "')";
				}

			} else if (literal instanceof RealLiteral) {

				// value is ok, set type
				type = XpathType.NUMBER;
			}

			// Return what we have
			XpathFragment xpt = new XpathFragment(11, value, type);
			return xpt;
		}
	}

	/**
	 * ************************************************************************
	 * This class represents a universal or existential quantification. TODO
	 * Negation is ignored.
	 */
	public static class QuantificationNode extends FolSchematronNode {

		Quantification q;
		private VariableNode variableNode;
		private FolSchematronNode condition;

		/**
		 * Ctor
		 * 
		 * @param schemaObject
		 *            The schema object
		 */
		public QuantificationNode(FOL2Schematron schemaObject, Quantification q) {
			this.schemaObject = schemaObject;
			this.q = q;
		}

		/**
		 * <p>
		 * This compiles the construct to an equivalent Xpath expression.
		 * </p>
		 * 
		 * @param ctx
		 *            BindingContext this node shall be compiled in
		 * @return Object containing the Xpath fragment
		 */
		public XpathFragment translate(BindingContext ctx) {

			XpathFragment xptvarvalue = variableNode.value().translate(ctx);

			// Compile the boolean expression in the condition of the
			// quantification
			XpathFragment prd = condition().translate(ctx);

			Quantifier quan = q.getQuantifier();

			// TODO: which priority?
			XpathFragment xpt = new XpathFragment(0, "", XpathType.BOOLEAN);

			if (quan.isUniversal()) {

				xpt.fragment += "every $" + q.getVar().getName() + " in "
						+ xptvarvalue.fragment + " satisfies (" + prd.fragment
						+ ")";

			} else {

				// in order for the counting variable names used in the
				// for-expression to be unique throughout the whole XPath
				// expression that represents a constraint, we make it unique by
				// appending an index
				int cvIndex = this.schemaObject.getNextVarIndex();

				xpt.fragment += "for " + COUNTING_VARIABLE_NAME + cvIndex
						+ " in count(for $" + q.getVar().getName() + " in "
						+ xptvarvalue.fragment + " return if (" + prd.fragment
						+ ") then 1 else ()) return (";

				if (quan.getLowerBoundary() != null
						&& quan.getUpperBoundary() != null) {

					if (quan.getLowerBoundary().intValue() == quan
							.getUpperBoundary().intValue()) {
						xpt.fragment += COUNTING_VARIABLE_NAME + cvIndex
								+ " = " + quan.getLowerBoundary();
					} else {
						xpt.fragment += COUNTING_VARIABLE_NAME + cvIndex
								+ " >= " + quan.getLowerBoundary() + " and "
								+ COUNTING_VARIABLE_NAME + cvIndex + " <= "
								+ quan.getUpperBoundary();
					}
				} else if (quan.getLowerBoundary() != null) {
					xpt.fragment += COUNTING_VARIABLE_NAME + cvIndex + " >= "
							+ quan.getLowerBoundary();
				} else {
					xpt.fragment += COUNTING_VARIABLE_NAME + cvIndex + " <= "
							+ quan.getUpperBoundary();
				}

				xpt.fragment += ")";
			}

			return xpt;
		}

		public void setVariableNode(VariableNode vn) {
			this.variableNode = vn;
		}

		public VariableNode variableNode() {
			return variableNode;
		}

		public void setCondition(FolSchematronNode fsn) {
			this.condition = fsn;
		}

		public FolSchematronNode condition() {
			return condition;
		}
	}

	/**
	 * ************************************************************************
	 * This is generated for unimplemented material.
	 */
	public static class Error extends FolSchematronNode {

		/**
		 * Ctor
		 * 
		 * @param schemaObject
		 *            The schema object
		 */
		public Error(FOL2Schematron schemaObject) {
			this.schemaObject = schemaObject;
			// Dummy - no action required
		}

		// Dummy, will never be called.
		public XpathFragment translate(BindingContext ctx) {
			return null;
		}
	}

	/**
	 * This represents an error message comment.
	 */
	public static class MessageComment extends FolSchematronNode {

		protected String name = null;

		/**
		 * Ctor
		 * 
		 * @param schemaObject
		 *            The schema object
		 */
		public MessageComment(FOL2Schematron schemaObject, String name) {
			this.schemaObject = schemaObject;
			this.name = name;
		}

		/**
		 * Extract the error number from the operator name.
		 * 
		 * @return The error number as a String
		 */
		public String getErrorNumber() {
			return name.substring(6);
		}

		/**
		 * <p>
		 * This method returns a vector or Schematron SQL value expressions in
		 * interpretation of a MessageComment object. The latter is created from
		 * the message text comment syntax contained in the constraints.
		 * </p>
		 * 
		 * @return Array of message arguments in FME value syntax
		 */
		public String[] compileAsMessageArgumentList() {

			String[] arglist = new String[children.size()];
			int i = 0;
			for (FolSchematronNode arg : children) {
				XpathFragment sql = arg.translate(null);
				arglist[i++] = sql.fragment == null ? "*ERROR*" : sql.fragment;
			}
			return arglist;
		}

		@Override
		public XpathFragment translate(BindingContext ctx) {
			// TODO Auto-generated method stub
			return null;
		}
	}
}
