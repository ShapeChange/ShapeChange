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

package de.interactive_instruments.ShapeChange.Target.XmlSchema;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.ShapeChange.MapEntry;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Ocl.OclNode;
import de.interactive_instruments.ShapeChange.Ocl.OclNode.AttributeCallExp;
import de.interactive_instruments.ShapeChange.Ocl.OclNode.Declaration;
import de.interactive_instruments.ShapeChange.Ocl.OclNode.MultiplicityMapping;

/**
 * <p>
 * SchematronConstraintNode and its concrete derivations stand for a
 * representation of OCL contents, which are close to the capabilities of
 * Schematron and the logic, which can be realized within Schematron Rules.
 * </p>
 * <p>
 * There are two basic patterns of use of these classes:
 * </p>
 * <ul>
 * <li>Creation of SchematronConstraintNode objects while interpreting the
 * original OclConstraint objects.
 * <li>Translating Rules and Assert code fragments by calling the abstract
 * method <i>translateToAssertTest.</i>.
 * </ul>
 */
public abstract class SchematronConstraintNode {

	/** The children of the SchematronConstraintNode */
	protected ArrayList<SchematronConstraintNode> children = new ArrayList<SchematronConstraintNode>();

	/** General negation flag for all nodes */
	protected boolean negated = false;

	/** The parent reference */
	protected SchematronConstraintNode parent = null;

	/** Link back to SchematronSchema object */
	protected SchematronSchema schemaObject = null;

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
	public void addChild(SchematronConstraintNode child) {
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
	 *            The Declaration of the variable
	 * @return Flag indicating the inquired dependency
	 */
	public boolean isDependentOn(OclNode.Declaration vardecl) {
		for (SchematronConstraintNode scn : children)
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
	 *            The variable Declaration object
	 * @return Node the variable is bound to or null
	 */
	public SchematronConstraintNode nodeVariableIsBoundTo(
			OclNode.Declaration vardecl) {
		return null;
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
	 *            The Declaration of the variable
	 * @return Flag indicating the inquired property
	 */
	public boolean isVarOrAttribBased(OclNode.Declaration vardecl) {
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
	public Attribute generatingAttribute() {
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

	/**
	 * Determine if the given class is instantiable, i.e. an XML element can be
	 * used to encode an object of that class.
	 * 
	 * @param ci
	 * @return <code>false</code> if the class is abstract or suppressed
	 *         (includes checking that rule-xsd-cls-suppress applies to the
	 *         class), otherwise <code>true</code>
	 */
	private static boolean isInstantiable(ClassInfo ci) {

		if (ci.isAbstract()
				|| (ci.suppressed() && ci.matches("rule-xsd-cls-suppress"))) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * <p>
	 * Find out whether this construct contains a node of type
	 * SchematronConstraintNode.Error. In this case the whole tree is in error.
	 * </p>
	 * 
	 * @return Error flag
	 */
	public boolean containsError() {
		if (this instanceof Error)
			return true;
		for (SchematronConstraintNode node : children)
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

		public class CtxElmt {
			public Declaration vardecl;
			public int noOfSteps = 0;

			CtxElmt(Declaration vd) {
				vardecl = vd;
			}
		}

		ArrayList<CtxElmt> vars = null;

		// Ctor
		BindingContext(CtxState state) {
			this.state = state;
		}

		// clone() override
		public BindingContext clone() {
			BindingContext copy = new BindingContext(state);
			if (vars != null)
				for (CtxElmt ce : vars) {
					copy.pushDeclaration(ce.vardecl);
					copy.vars
							.get(copy.vars.size() - 1).noOfSteps = ce.noOfSteps;
				}
			return copy;
		}

		// Reset state
		public void setState(CtxState state) {
			this.state = state;
			this.vars = null;
		}

		/** Push a new variable declaration */
		public void pushDeclaration(Declaration vd) {
			if (vars == null)
				vars = new ArrayList<CtxElmt>();
			vars.add(new CtxElmt(vd));
			this.state = CtxState.OTHER;
		}

		/** Increment the child step counter from the last declaration */
		public void addStep() {
			if (vars == null || vars.size() == 0)
				return;
			++(vars.get(vars.size() - 1).noOfSteps);
		}

		/** Do away with the last variable declaration */
		public void popDeclaration() {
			if (vars == null || vars.size() == 0)
				return;
			vars.remove(vars.size() - 1);
		}

		/** Merge another context */
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
				int thissize = vars == null ? 0 : vars.size();
				int ctxsize = ctx.vars == null ? 0 : ctx.vars.size();
				int i = thissize - 1;
				int j = ctxsize - 1;
				for (; i >= 0 && j >= 0; --i, --j) {
					CtxElmt cei = vars.get(i);
					CtxElmt cej = ctx.vars.get(j);
					if (cei.vardecl != cej.vardecl)
						break;
					if (cei.noOfSteps != cej.noOfSteps)
						break;
				}
				while (i >= 0)
					vars.remove(i--);
				if (vars != null && vars.size() == 0)
					vars = null;
			} else {
				state = CtxState.OTHER;
				vars = null;
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
		public boolean variableMerging = true;
		public String fragment;
		public XpathType type;
		public TreeMap<String, String> lets = null;
		public BindingContext atEnd = new BindingContext(
				BindingContext.CtxState.NONE);

		// Constructor from priority, type and expression
		public XpathFragment(int p, String f, XpathType t) {
			priority = p;
			fragment = f;
			type = t;
		}

		/**
		 * @param p
		 * @param f
		 * @param t
		 * @param vm
		 *            <code>true</code> to perform variable merging, else
		 *            <code>false</code>
		 */
		public XpathFragment(int p, String f, XpathType t, boolean vm) {
			this(p, f, t);
			variableMerging = vm;
		}

		// Constructor from priority and expression. Type assumed 'nodeset'
		public XpathFragment(int p, String f) {
			priority = p;
			fragment = f;
			type = XpathType.NODESET;
		}

		/**
		 * @param p
		 * @param f
		 * @param vm
		 *            <code>true</code> to perform variable merging, else
		 *            <code>false</code>
		 */
		public XpathFragment(int p, String f, boolean vm) {
			this(p, f, XpathType.NODESET);
			variableMerging = vm;
		}

		/**
		 * Bracket the current expression
		 */
		public void bracket() {
			fragment = "(" + fragment + ")";
			priority = 11;
		}

		/**
		 * Add another fragment performing let variable merging. The argument
		 * fragment is destroyed. If binding contexts are given they are also
		 * merged.
		 * 
		 * @return the merged fragment string
		 */
		public String merge(XpathFragment xf) {
			if (variableMerging && xf.variableMerging) {
				xf.replace("\\$(\\w*)", "%$1");
				if (xf.lets != null)
					for (Map.Entry<String, String> ve : xf.lets.entrySet()) {
						String vn = ve.getKey();
						String ex = ve.getValue();
						String vnew = findOrAdd(ex);
						xf.replace("%" + vn, "\\$" + vnew);
					}
				if (atEnd != null)
					atEnd.merge(xf.atEnd);
				return xf.fragment;
			} else {
				return xf.fragment;
			}
		}

		/**
		 * Function to find or add a variable given the expression
		 */
		public String findOrAdd(String ex) {

			if (lets == null) {
				lets = new TreeMap<String, String>();
			}

			/*
			 * If ex is an existing let variable, just return it (and thus
			 * prevent a let expression with a variable as value).
			 */
			if (ex.startsWith("$") && lets.containsKey(ex.substring(1))) {
				return ex.substring(1);
			}

			/*
			 * Determine if a let variable with the given expression already
			 * exists.
			 */
			for (Map.Entry<String, String> ve : lets.entrySet()) {
				if (ve.getValue().equals(ex))
					return ve.getKey();
			}

			/*
			 * Create a new let variable.
			 */
			String newkey = "A";
			if (!lets.isEmpty()) {
				String last = lets.lastKey();
				String lc = last.substring(last.length() - 1);
				if (lc.equals("Z"))
					newkey = last + "A";
				else {
					try {
						byte[] bytes = lc.getBytes("US-ASCII");
						bytes[0]++;
						newkey = last.substring(0, last.length() - 1)
								+ new String(bytes, "US-ASCII");
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
			}
			lets.put(newkey, ex);
			return newkey;
		}

		/**
		 * Auxiliary function to replace variable names
		 */
		private void replace(String from, String to) {
			Pattern pat = Pattern.compile(from);
			if (lets != null)
				for (Map.Entry<String, String> ve : lets.entrySet()) {
					String ex = ve.getValue();
					Matcher matcher = pat.matcher(ex);
					ve.setValue(matcher.replaceAll(to));
				}
			Matcher matcher = pat.matcher(fragment);
			fragment = matcher.replaceAll(to);
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
	public static class Logic extends SchematronConstraintNode {

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
		public Logic(SchematronSchema schemaObject, LogicType logic) {
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

			// In turn compile the node's children and connect them with logic
			// particles. Bracket the subexpression if necessary. When
			// processing a XOR/EQV (which is implemented as !=/= of booleans)
			// do the necessary conversion to boolean first.
			XpathFragment result = null;
			for (SchematronConstraintNode ocn : children) {
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
				if (result == null)
					result = child_xpt;
				else {
					String mrge = result.merge(child_xpt);
					result.fragment += " " + particle + " ";
					result.fragment += mrge;
				}
			}

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
	public static class Comparison extends SchematronConstraintNode {

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
		public Comparison(SchematronSchema schemaObject, String name) {
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

				SchematronConstraintNode child = children.get(i);

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
	public static class Empty extends SchematronConstraintNode {

		// negated means: isEmpty (false) or notEmpty (true)

		/**
		 * Ctor
		 * 
		 * @param schemaObject
		 *            The schema object
		 * @param neg
		 *            Flag: isEmpty (false) and notEmpty (true)
		 */
		public Empty(SchematronSchema schemaObject, boolean neg) {
			this.schemaObject = schemaObject;
			negated = neg;
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
			SchematronConstraintNode obj = children.get(0);
			XpathFragment xpt = obj.translate(ctx);
			if (xpt.fragment.length() == 0) {
				xpt.fragment = ".";
			}

			// isEmpty() requires an additional not(...)
			if (!negated) {
				xpt.fragment = "not(" + xpt.fragment + ")";
				xpt.type = XpathType.BOOLEAN;
				xpt.priority = 11;
				xpt.atEnd.setState(BindingContext.CtxState.NONE);
			}

			return xpt;
		}
	}

	/**
	 * ************************************************************************
	 * This class represents the Exists iterator predicate. Being part of the
	 * Logic system Exists can be negated, which is realized by applying an
	 * additional not(). Note that Exists also realizes the forAll() iterator.
	 * The mapping according to the rules of 1st order logic is achieved when
	 * the Exists node is created.
	 */
	public static class Exists extends SchematronConstraintNode {

		// The variable declaration attached to the iterator
		OclNode.Declaration vardecl;

		/**
		 * Ctor
		 * 
		 * @param schemaObject
		 *            The schema object
		 * @param vardecl
		 *            OclNode.Declaration object
		 * @param neg
		 *            Negation flag
		 */
		public Exists(SchematronSchema schemaObject,
				OclNode.Declaration vardecl, boolean neg) {
			this.schemaObject = schemaObject;
			this.negated = neg;
			this.vardecl = vardecl;
		}

		/**
		 * This method determines whether the Exists binds the given variable
		 * declaration and if it does, which is the expression the variable is
		 * bound to.
		 * 
		 * @param vardecl
		 *            The variable Declaration object
		 * @return Node the variable is bound to or null
		 */
		public SchematronConstraintNode nodeVariableIsBoundTo(
				OclNode.Declaration vardecl) {
			if (this.vardecl == vardecl)
				return children.get(0);
			return null;
		}

		/**
		 * <p>
		 * This compiles the node and its children to an Xpath expression
		 * fragment.
		 * </p>
		 * <p>
		 * The object is translated in the given context and its ending
		 * position, supplemented by the binding variable, defines the context
		 * for the compilation of the body, which is appended as a predicate
		 * bracket. If negated an additional not() is applied.
		 * </p>
		 * 
		 * @param ctx
		 *            BindingContext this node shall be compiled in
		 * @return Object containing the fragment and its operator priority
		 */
		public XpathFragment translate(BindingContext ctx) {

			// Fetch and compile the object
			SchematronConstraintNode obj = children.get(0);
			XpathFragment xpt = obj.translate(ctx);

			// Prepare the binding context for compilation of the iterator
			// body. This is primarily the context at the end of the object
			// plus the variable.
			BindingContext bodyctx = xpt.atEnd.clone();
			bodyctx.pushDeclaration(vardecl);

			// Compile the boolean expression in the iterator body
			SchematronConstraintNode pred = children.get(1);
			XpathFragment prd = pred.translate(bodyctx);
			prd.atEnd = null; // This suppresses the merging of ending contexts

			// Append the boolean expression as a predicate filter
			String filter = xpt.merge(prd);
			if (xpt.priority < 10) {
				xpt.bracket();
			}
			xpt.fragment += "[" + filter + "]";
			xpt.priority = 10;

			// Consider negated form
			if (negated) {
				xpt.fragment = "not(" + xpt.fragment + ")";
				xpt.type = XpathType.BOOLEAN;
				xpt.priority = 11;
				xpt.atEnd.setState(BindingContext.CtxState.NONE);
			}

			return xpt;
		}
	}

	/**
	 * ************************************************************************
	 * This class represents the isUnique iterator predicate. Being part of the
	 * Logic system Unique can be negated, which is realized by applying an
	 * additional not().
	 */
	public static class Unique extends SchematronConstraintNode {

		// The variable declaration attached to the iterator
		OclNode.Declaration vardecl;

		/**
		 * Ctor
		 * 
		 * @param schemaObject
		 *            The schema object
		 * @param vardecl
		 *            OclNode.Declaration object
		 * @param neg
		 *            Negation flag
		 */
		public Unique(SchematronSchema schemaObject,
				OclNode.Declaration vardecl, boolean neg) {
			this.schemaObject = schemaObject;
			this.negated = neg;
			this.vardecl = vardecl;
		}

		/**
		 * This method determines whether the Unique binds the given variable
		 * declaration and if it does, which is the expression the variable is
		 * bound to.
		 * 
		 * @param vardecl
		 *            The variable Declaration object
		 * @return Node the variable is bound to or null
		 */
		public SchematronConstraintNode nodeVariableIsBoundTo(
				OclNode.Declaration vardecl) {
			if (this.vardecl == vardecl)
				return children.get(0);
			return null;
		}

		/**
		 * <p>
		 * This compiles the node and its children to an Xpath expression
		 * fragment.
		 * </p>
		 * <p>
		 * The object is translated in the given context. If negated an
		 * additional not() is applied.
		 * </p>
		 * 
		 * @param ctx
		 *            BindingContext this node shall be compiled in
		 * @return Object containing the fragment and its operator priority
		 */
		public XpathFragment translate(BindingContext ctx) {

			// Local negation variable allows to absorb negation en route
			boolean neg = negated;

			// Fetch and compile the object
			SchematronConstraintNode obj = children.get(0);
			XpathFragment xpt = obj.translate(ctx);

			// Get hold of the expression argument and take cases on its
			// nature ...
			SchematronConstraintNode expr = children.get(1);
			if (!expr.isDependentOn(vardecl)) {

				// A constant expression can be unique only if the object
				// is of cardinality zero or one.
				xpt.fragment = "count(" + xpt.fragment + ") "
						+ (neg ? ">" : "<=") + " 1";
				neg = false;
				xpt.priority = 4;
				xpt.type = XpathType.BOOLEAN;
				xpt.atEnd.setState(BindingContext.CtxState.NONE);

			} else if (expr.isVarOrAttribBased(vardecl)
					&& expr instanceof Variable) {

				// If the expression is the identity mapping, the result depends
				// on the nature of the object types.
				boolean simple = false;
				// Determine the Attribute behind the object
				Attribute obat = null;
				if (obj instanceof Attribute)
					obat = (Attribute) obj;
				else if (obj instanceof Variable) {
					obat = obj.generatingAttribute();
				}
				if (obat != null) {
					ClassInfo ci = obat.attributes[obat.attributes.length
							- 1].main.dataType.umlClass;
					if (ci != null) {
						Boolean indicatorSimpleType = XmlSchema
								.indicatorForObjectElementWithSimpleContent(ci);
						simple = !XmlSchema.classHasObjectElement(ci)
								|| (indicatorSimpleType != null
										&& indicatorSimpleType);
					} else {
						String tname = obat.attributes[obat.attributes.length
								- 1].main.dataType.name;
						String er = schemaObject.currentOclConstraintClass
								.encodingRule("xsd");
						MapEntry me = schemaObject.options.typeMapEntry(tname,
								er);
						if (me != null)
							simple = me.p2.equalsIgnoreCase("simple/simple")
									|| me.p2.equalsIgnoreCase("complex/simple");
					}
				}
				if (simple) {
					/*
					 * The object is expressed by means of a type with simple
					 * content. The nodeset will need to undergo a pairwise
					 * value comparison. Note that 19139 treatment has already
					 * been done on the object compiled to xpt.fragment.
					 */
					String var = null;
					if (xpt.fragment.matches("^\\$[A-Z]+$"))
						var = xpt.fragment;
					else
						var = "$" + xpt.findOrAdd(xpt.fragment);
					xpt.fragment = var
							+ "[. = (preceding::*|ancestor::*)[count(.|" + var
							+ ")=count(" + var + ")]]";
					xpt.priority = 10;
					neg = !neg;
				} else {
					// The object is element valued. Elements in a nodeset are
					// are always distinct, so uniqueness is trivially true.
					xpt.fragment = neg ? "false()" : "true()";
					neg = false;
					xpt.priority = 11;
					xpt.type = XpathType.BOOLEAN;
					xpt.atEnd.setState(BindingContext.CtxState.NONE);
				}
			} else if (expr.isVarOrAttribBased(vardecl)) {

				// If we have an attribute, which is based on the binding
				// variable, we can try a translation. Precondition, however,
				// is that all selectors have an attached cardinality of 1
				// or 0.
				if (!expr.isMultiple()) {
					// Non-multiple attribute access using the binding variable.
					Attribute exat = (Attribute) expr;
					Attribute exat1 = exat.splitBefore(1);
					if (exat1 == null) {
						// Just 1 selector contained. We can now translate into
						// an equivalent Xpath expression ...
						if (!exat.hasSimpleType()) {
							// Class is expressed by means of an element
							// construct.
							SchematronConstraintNode sv = exat.children.get(0);
							exat.children.set(0, children.get(0));
							XpathFragment xpta = exat.translate(ctx);
							xpt.merge(xpta);
							xpt.fragment = "count(" + xpt.fragment + ") "
									+ (neg ? "!=" : "=") + " count("
									+ xpta.fragment + ")";
							neg = false;
							xpt.priority = 4;
							xpt.type = XpathType.BOOLEAN;
							xpt.atEnd.setState(BindingContext.CtxState.NONE);
							exat.children.set(0, sv);
						} else {
							/*
							 * Class is expressed by means of a simple type.
							 * This also includes 'reason' access in GML's
							 * nilReason treatment.
							 */
							String var = null;
							if (xpt.fragment.matches("^\\$[A-Z]+$"))
								var = xpt.fragment;
							else
								var = "$" + xpt.findOrAdd(xpt.fragment);
							PropertyInfo pi = (PropertyInfo) exat.attributes[0].main.selector.modelProperty;
							String prop = schemaObject
									.getAndRegisterXmlName(pi);
							// Find out about modified circumstances concerning
							// value access ...
							String cid = pi.typeInfo().id;
							ClassInfo ci = null;
							if (cid != null)
								ci = pi.model().classById(cid);
							boolean iscodelist = ci != null
									&& ci.category() == Options.CODELIST;
							boolean is19139 = ci != null
									&& ci.matches("rule-xsd-all-naming-19139");
							// Treat special cases in property access
							if (exat.attributes[0].absType == 0) {
								if (is19139) {
									prop += "/*";
									if (iscodelist)
										prop += "/@codeListValue";
								} else if (iscodelist
										&& pi.inClass().matches(
												"rule-xsd-cls-codelist-asDictionaryGml33")
										&& (ci.asDictionary()
												|| ci.asDictionaryGml33())
										&& !is19139) {
									prop += "/@xlink:href";
									schemaObject.registerNamespace("xlink");
								}
							}
							// NilReason treatment ...
							String nil = "";
							if (exat.attributes[0].absType == 2) {
								if (is19139) {
									nil = "[not(*)]/@gco:nilReason";
									schemaObject.registerNamespace("gco");
								} else {
									nil = "[@xsi:nil='true']/@nilReason";
									schemaObject.registerNamespace("xsi");
								}
							}
							// Compile the overall syntax
							xpt.fragment = var + "[" + prop + nil
									+ " = (preceding::*|ancestor::*)[count(.|"
									+ var + ")=count(" + var + ")]/" + prop
									+ nil + "]";
							xpt.priority = 10;
							neg = !neg;
						}
					} else {
						// The split was successful. We will recursively descend
						// into the split parts of the attribute and combine the
						// results with an 'and' ...
						boolean savednegated = negated;
						negated = false;
						// 1st: obj->isUnique(v|v.a1)
						children.set(1, exat1.children.get(0));
						XpathFragment xpt1 = translate(ctx);
						// 2nd: obj.a1->isUnique(w|w.a2....) -- Note variable
						// is incorrectly used - Type is not o.k.
						Attribute exat0 = (Attribute) exat1.children.get(0);
						Variable v0 = (Variable) exat0.children.get(0);
						exat0.children.set(0, children.get(0));
						exat1.children.set(0, v0); // Note wrong type
						children.set(0, exat0);
						children.set(1, exat1);
						XpathFragment xpt2 = translate(ctx);
						// Merge and create result
						String frag2 = xpt1.merge(xpt2);
						if (xpt1.priority < 2)
							xpt1.bracket();
						if (xpt2.priority < 2)
							frag2 = "(" + frag2 + ")";
						xpt1.fragment += " and " + frag2;
						xpt1.priority = 2;
						xpt1.type = XpathType.BOOLEAN;
						xpt1.atEnd.setState(BindingContext.CtxState.NONE);
						xpt = xpt1;
						// Restore object
						children.set(0, obj);
						children.set(1, expr);
						negated = savednegated;
					}

				} else {
					// Multiple selectors encountered. Signal implementation
					// restriction ...
					return new XpathFragment(11, "***ERROR[121]***");
				}
			} else {

				// Expression other than constant, identity of attribute
				// access. We cannot express this in Xpath syntax
				return new XpathFragment(11, "***ERROR[122]***");
			}

			// Consider negated form
			if (neg) {
				xpt.fragment = "not(" + xpt.fragment + ")";
				xpt.type = XpathType.BOOLEAN;
				xpt.priority = 11;
				xpt.atEnd.setState(BindingContext.CtxState.NONE);
			}

			return xpt;
		}
	}

	/**
	 * ************************************************************************
	 * This class represents the Select iterator filter.
	 */
	public static class Select extends SchematronConstraintNode {

		// The variable declaration attached to the iterator
		protected OclNode.Declaration vardecl;

		// Stored generator body
		protected SchematronConstraintNode generatorBody = null;

		/**
		 * Ctor
		 * 
		 * @param schemaObject
		 *            The schema object
		 * @param vardecl
		 *            OclNode.Declaration object
		 */
		public Select(SchematronSchema schemaObject,
				OclNode.Declaration vardecl) {
			this.schemaObject = schemaObject;
			this.vardecl = vardecl;
		}

		/**
		 * This method determines whether the Select binds the given variable
		 * declaration and if it does, which is the expression the variable is
		 * bound to.
		 * 
		 * @param vardecl
		 *            The variable Declaration object
		 * @return Node the variable is bound to or null
		 */
		public SchematronConstraintNode nodeVariableIsBoundTo(
				OclNode.Declaration vardecl) {
			if (this.vardecl == vardecl)
				return children.get(0);
			return null;
		}

		/**
		 * <p>
		 * By means of this function you can inquire which Attribute node is
		 * generating the objects of this Select node if any.
		 * </p>
		 * 
		 * @return The retrieved Attribute node if there is such a thing
		 */
		public Attribute generatingAttribute() {
			return children.get(0).generatingAttribute();
		}

		/**
		 * <p>
		 * The value of Select is always a set.
		 * </p>
		 * 
		 * @return Flag indicating whether the node can return multiple values
		 */
		public boolean isMultiple() {
			return true;
		}

		/**
		 * <p>
		 * This predicate finds out whether the Select results in a simple XML
		 * schema type.
		 * </p>
		 * 
		 * @return Flag indicating whether the node has a simple type
		 */
		public boolean hasSimpleType() {
			return children.get(0).hasSimpleType();
		}

		/**
		 * <p>
		 * This predicate finds out whether the Select results in a collection
		 * of instances, which conceptually have identity.
		 * </p>
		 * 
		 * @return Flag indicating whether the node is an identity carrying type
		 */
		public boolean hasIdentity() {
			return children.get(0).hasIdentity();
		}

		/**
		 * <p>
		 * This compiles the Select node and its children to an Xpath expression
		 * fragment.
		 * </p>
		 * <p>
		 * The object is translated in the given context and its ending
		 * position, supplemented by the binding variable, defines the context
		 * for the compilation of the body, which is appended as a predicate
		 * bracket. Note that Select is very similar to Exists - the only
		 * diffence being that the result is not interpreted in a Boolean way.
		 * </p>
		 * 
		 * @param ctx
		 *            BindingContext this node shall be compiled in
		 * @return Object containing the fragment and its operator priority
		 */
		public XpathFragment translate(BindingContext ctx) {

			// Fetch and compile the object
			SchematronConstraintNode obj = children.get(0);
			XpathFragment xpt = obj.translate(ctx);

			// Prepare the binding context for compilation of the iterator
			// body. This is primarily the context at the end of the object
			// plus the variable.
			BindingContext bodyctx = xpt.atEnd.clone();
			bodyctx.pushDeclaration(vardecl);

			// Compile the boolean expression in the iterator body
			SchematronConstraintNode pred = children.get(1);
			XpathFragment prd = pred.translate(bodyctx);
			prd.atEnd = null; // This suppresses the merging of ending contexts

			// Append the boolean expression as a predicate filter
			String filter = xpt.merge(prd);
			if (xpt.priority < 10)
				xpt.bracket();
			xpt.fragment += "[" + filter + "]";
			xpt.priority = 10;

			return xpt;
		}
	}

	/**
	 * ************************************************************************
	 * <p>
	 * This class represents the OCL operation allInstances(). AllInstances is
	 * based on a class literal and represents all instances of that class.
	 * </p>
	 */
	public static class AllInstances extends SchematronConstraintNode {

		protected ClassInfo objectClass;

		/**
		 * Ctor
		 * 
		 * @param schemaObject
		 *            The schema object
		 * @param ci
		 *            ClassInfo of the class to enumerate
		 * @param negated
		 *            May be negated if of type boolean
		 */
		public AllInstances(SchematronSchema schemaObject, ClassInfo ci,
				boolean negated) {
			this.schemaObject = schemaObject;
			this.objectClass = ci;
			this.negated = negated;
		}

		/**
		 * <p>
		 * Allinstances always produces a set.
		 * </p>
		 * 
		 * @return Flag indicating whether the node can return multiple values
		 */
		public boolean isMultiple() {
			return true;
		}

		/**
		 * <p>
		 * allInstances() is never simple.
		 * </p>
		 * 
		 * @return Flag indicating whether the node has a simple type
		 */
		public boolean hasSimpleType() {
			return false;
		}

		/**
		 * <p>
		 * This predicate finds out whether the allInstances results in a
		 * collection of instances, which conceptually have identity.
		 * </p>
		 * 
		 * @return Flag indicating whether the node is an identity carrying type
		 */
		public boolean hasIdentity() {
			return XmlSchema.classCanBeReferenced(objectClass);
		}

		/**
		 * <p>
		 * allInstances() is translated to a search for the given type. The
		 * result is a nodeset containing all the given features.
		 * </p>
		 * <p>
		 * In compiling x.allInstances() we create a nodeset union (n
		 * <sub>1</sub>|...|n<sub>i</sub>), where n<sub>k</sub>=//T<sub>k</sub>
		 * [@gml:id] and T<sub>k</sub> is one of the concrete derivations of the
		 * type x, including x.
		 * </p>
		 * 
		 * @param ctx
		 *            BindingContext this node shall be compiled in
		 */
		public XpathFragment translate(BindingContext ctx) {

			// 1st obtain the necessary classes from the model
			SortedSet<String> subtypes = objectClass.subtypes();
			Set<String> classnames = new HashSet<String>();
			if (subtypes != null) {
				for (String stid : subtypes) {
					ClassInfo ci = objectClass.model().classById(stid);
					if (isInstantiable(ci)) {
						classnames.add(schemaObject.getAndRegisterXmlName(ci));
					}
				}
			}
			if (isInstantiable(objectClass)) {
				classnames.add(schemaObject.getAndRegisterXmlName(objectClass));
			}

			// 2nd create the Xpath expression combining all those classes
			String fragment = "";
			if (classnames.size() == 0) {
				fragment = "/*[false()]";
			} else {
				for (String name : classnames) {
					if (fragment.length() != 0)
						fragment += " | ";
					fragment += "//" + name;
					// 2010-08-06/re formerly the path also contained +
					// "[@gml:id]"
					// which disambiguated against properties but rendered
					// <<dataType>>s
					// unavailable.
					// schemaObject.registerNamespace( "gml" );
				}
			}

			// Wrap and provide proper priority. Set binding context.
			XpathFragment xpt = new XpathFragment(classnames.size() > 1 ? 8 : 9,
					fragment);
			xpt.atEnd = new BindingContext(BindingContext.CtxState.OTHER);

			return xpt;
		}

	}

	/**
	 * ************************************************************************
	 * This class represents oclIsKindOf and oclIsTypeOf nodes. The difference
	 * between the two is expressed in an <i>exact</i> flag. The object also
	 * carries a negation flag to express that an object is NOT kind of some
	 * type. The type is given by the first of the children.
	 */
	public static class KindOf extends SchematronConstraintNode {

		// The class to be tested against
		protected ClassInfo argumentClass = null;
		// Exact match?
		protected boolean exact = false;

		/**
		 * Ctor
		 * 
		 * @param schemaObject
		 *            The schema object
		 * @param exact
		 *            Flag: Only check the given type
		 * @param neg
		 *            Flag: Negated meaning
		 */
		public KindOf(SchematronSchema schemaObject, boolean exact,
				boolean neg) {
			this.schemaObject = schemaObject;
			this.exact = exact;
			negated = neg;
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

		/**
		 * <p>
		 * This compiles the KindOf predicate (and its negation) to an
		 * equivalent Xpath expression fragment. KindOf is translated to a
		 * predicate which compares the element name against all concrete
		 * subtypes of the given type.
		 * </p>
		 * 
		 * @param ctx
		 *            BindingContext this node shall be compiled in
		 * @return Object containing the Xpath fragment
		 */
		public XpathFragment translate(BindingContext ctx) {

			// Translate the object in the given context
			SchematronConstraintNode objnode = children.get(0);
			XpathFragment xptobj = objnode.translate(ctx);
			boolean emptyobject = xptobj.fragment.length() == 0;

			// Obtain the necessary classes from the model

			SortedSet<ClassInfo> relevantClasses = new TreeSet<>();

			if (!exact) {
				SortedSet<String> subtypes = argumentClass.subtypes();

				for (String stid : subtypes) {

					ClassInfo ci = argumentClass.model().classById(stid);

					if (isInstantiable(ci)) {
						relevantClasses.add(ci);
					}
				}
			}

			if (isInstantiable(argumentClass)) {
				relevantClasses.add(argumentClass);
			}

			// Construct the result expression
			if (relevantClasses.size() == 0) {

				// There is no concrete class known. So, this must be false.
				xptobj.fragment = negated ? "true()" : "false()";
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
				if (!emptyobject)
					xptobj.fragment += "[";

				/*
				 * If we have more than one component, surround each part we add
				 * with parentheses (only relevant for non-negated case).
				 */
				boolean surroundWithBrackets = relevantClasses.size() > 1;

				for (ClassInfo ci : relevantClasses) {

					if (!first) {
						xptobj.fragment += negated ? " and " : " or ";
					}

					first = false;

					/*
					 * 2018-05-16 JE: comparison based on QName as literal value
					 * (like 'ex:ClassX') is dangerous, because it depends on a
					 * fixed namespace prefix. However, the prefix of a
					 * namespace can vary. Therefore, I changed the logic from
					 * name()=aQName to use local-name() and namespace-uri().
					 */

					String namespace = ci.pkg().targetNamespace();
					String localName = ci.name();

					MapEntry me = ci.options().elementMapEntry(ci.name(),
							ci.encodingRule("xsd"));

					if (me != null && StringUtils.isNotBlank(me.p1)) {

						String xmlElement = me.p1;
						String[] parts = xmlElement.split(":");

						if (parts.length > 1) {
							String nspref = parts[0];
							String ns = schemaObject.options
									.fullNamespace(nspref);
							if (ns != null) {
								namespace = ns;
								localName = parts[1];
							}
						}
					}

					if (negated) {
						xptobj.fragment += "not(";
					} else if (surroundWithBrackets) {
						xptobj.fragment += "(";
					}

					xptobj.fragment += "local-name()='" + localName
							+ "' and namespace-uri()='" + namespace + "'";

					if (negated || surroundWithBrackets) {
						xptobj.fragment += ")";
					}
				}
				xptobj.priority = 3;
				if (relevantClasses.size() > 1) {
					xptobj.priority = negated ? 2 : 1;
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
	 * <p>
	 * This class represents oclAsType(), which is for casting a type to one of
	 * its subtypes.
	 * </p>
	 */
	public static class Cast extends SchematronConstraintNode {

		// The class to be cast to
		protected ClassInfo argumentClass = null;

		// Connection info, required to access the config document
		protected String targetClassName;

		/**
		 * Ctor
		 * 
		 * @param schemaObject
		 *            The schema object
		 */
		public Cast(SchematronSchema schemaObject) {
			this.schemaObject = schemaObject;
		}

		/**
		 * If the class to be cast to is already known (it is not an expression)
		 * this reference can be set via this method.
		 * 
		 * @param ci
		 *            ClassInfo representing the type to be cast to
		 */
		public void setClass(ClassInfo ci) {
			argumentClass = ci;
		}

		/**
		 * <p>
		 * This predicate finds out whether the Cast results in a simple XML
		 * schema type.
		 * </p>
		 * 
		 * @return Flag indicating whether the node has a simple type
		 */
		public boolean hasSimpleType() {
			return children.get(0).hasSimpleType();
		}

		/**
		 * <p>
		 * This predicate finds out whether the Cast results in an instance,
		 * which conceptually has identity.
		 * </p>
		 * 
		 * @return Flag indicating whether the node is an identity carrying type
		 */
		public boolean hasIdentity() {
			return children.get(0).hasIdentity();
		}

		/**
		 * <p>
		 * This compiles the Cast to an Xpath fragment.
		 * </p>
		 * <p>
		 * We realize this by making sure the current element is of the
		 * requested type or any of its concrete subtypes.
		 * </p>
		 * 
		 * @param ctx
		 *            BindingContext this node shall be compiled in
		 * @return Object containing the Xpath fragment
		 */
		public XpathFragment translate(BindingContext ctx) {

			// Translate the object in the given context
			SchematronConstraintNode objnode = children.get(0);
			XpathFragment xptobj = objnode.translate(ctx);
			if (xptobj.fragment.length() == 0) {
				xptobj.fragment = "self::*";
				xptobj.priority = 10;
			}

			// Obtain the necessary classes from the model
			SortedSet<String> subtypes = argumentClass.subtypes();

			SortedSet<ClassInfo> relevantClasses = new TreeSet<>();

			for (String stid : subtypes) {

				ClassInfo ci = argumentClass.model().classById(stid);

				if (isInstantiable(ci)) {
					relevantClasses.add(ci);
				}
			}

			if (isInstantiable(argumentClass)) {
				relevantClasses.add(argumentClass);
			}

			// Append a predicate which compares against all possible
			// names.
			boolean first = true;
			if (xptobj.priority < 9) {
				xptobj.bracket();
			}
			xptobj.fragment += "[";

			/*
			 * If we have more than one component, surround each part we add
			 * with parentheses.
			 */
			boolean surroundWithBrackets = relevantClasses.size() > 1;

			for (ClassInfo ci : relevantClasses) {

				if (!first) {
					xptobj.fragment += " or ";
				}

				first = false;

				String namespace = ci.pkg().targetNamespace();
				String localName = ci.name();

				MapEntry me = ci.options().elementMapEntry(ci.name(),
						ci.encodingRule("xsd"));

				if (me != null && StringUtils.isNotBlank(me.p1)) {

					String xmlElement = me.p1;
					String[] parts = xmlElement.split(":");

					if (parts.length > 1) {
						String nspref = parts[0];
						String ns = schemaObject.options.fullNamespace(nspref);
						if (ns != null) {
							namespace = ns;
							localName = parts[1];
						}
					}
				}

				if (surroundWithBrackets) {
					xptobj.fragment += "(";
				}

				/*
				 * 2018-02-06 JE: comparison based on QName as literal value
				 * (like 'ex:ClassX') is dangerous, because it depends on a
				 * fixed namespace prefix. However, the prefix of a namespace
				 * can vary. Therefore, I changed the logic from name()=aQName
				 * to use local-name() and namespace-uri().
				 */
				xptobj.fragment += "local-name()='" + localName
						+ "' and namespace-uri()='" + namespace + "'";

				if (surroundWithBrackets) {
					xptobj.fragment += ")";
				}
			}

			// for case in which no relevant class was found
			if (first) {
				xptobj.fragment += "false()";
			}

			xptobj.fragment += "]";
			xptobj.priority = 10;

			// If we are casting a CharacterString to a Codelist, we have to add
			// code, which property expresses the codelist access according to
			// the applied encoding rule.

			if (argumentClass.category() == Options.CODELIST
					&& (argumentClass.matches("rule-xsd-all-naming-19139")
							|| (argumentClass.matches(
									"rule-xsd-cls-codelist-asDictionaryGml33")
									&& argumentClass.asDictionaryGml33())
							|| (argumentClass.matches(
									"rule-xsd-cls-codelist-asDictionary")
									&& argumentClass.asDictionary()))) {

				// We need to know, whether we are subject to the 19139 regime
				// or if GML 3.3 encoding rules apply
				Attribute att = objnode.generatingAttribute();
				PropertyInfo pip = ((PropertyInfo) (att.attributes[att.attributes.length
						- 1].main.selector.modelProperty));
				boolean is19139 = argumentClass
						.matches("rule-xsd-all-naming-19139");
				boolean isgml33 = pip != null && pip.inClass()
						.matches("rule-xsd-cls-codelist-asDictionaryGml33");

				/*
				 * If not translating according to GML 3.3 we need to prepare
				 * the CodeListValuePattern.
				 */
				String clvpat = "{value}";
				int nsubst = 1;

				if (!isgml33) {

					String uri = argumentClass.taggedValue("codeList");
					if (uri != null && uri.length() > 0) {
						clvpat = "{codeList}/{value}";
					}

					clvpat = this.schemaObject.determineCodeListValuePattern(
							argumentClass, clvpat);

					clvpat = clvpat.replace("{codeList}", "',{codeList},'");
					clvpat = clvpat.replace("{value}", "',{value},'");
					if (clvpat.startsWith("',")) {
						clvpat = clvpat.substring(2);
					}
					if (clvpat.endsWith(",'")) {
						clvpat = clvpat.substring(0, clvpat.length() - 2);
					}
					if (!clvpat.startsWith("{")) {
						clvpat = "'" + clvpat;
					}
					if (!clvpat.endsWith("}")) {
						clvpat += "'";
					}
					if (!clvpat.equals("{value}")) {
						clvpat = "concat(" + clvpat + ")";
						nsubst = 2;
					}
				}

				// Special cases ...
				boolean atcurr = ctx.state == BindingContext.CtxState.ATCURRENT;
				if (is19139) {
					// 19139 encoding
					if (nsubst == 2 && atcurr) {
						String v = xptobj.findOrAdd(xptobj.fragment);
						xptobj.fragment = "$" + v;
					}
					String clvp = clvpat.replace("{codeList}",
							xptobj.fragment + "/@codeList");
					xptobj.fragment = clvp.replace("{value}",
							xptobj.fragment + "/@codeListValue");
				} else if (!isgml33) {
					// In elder GMLs we might find the codespace in
					// the codespace attribute
					if (nsubst == 2 && atcurr) {
						String v = xptobj.findOrAdd(xptobj.fragment);
						xptobj.fragment = "$" + v;
					}
					String clvp = clvpat.replace("{codeList}",
							xptobj.fragment + "/@codeSpace");
					xptobj.fragment = clvp.replace("{value}", xptobj.fragment);
				} else {
					// If using GML 3.3 type codelist treatment, we have
					// to refer to the xlink:href attribute
					xptobj.fragment += "/@xlink:href";
					schemaObject.registerNamespace("xlink");
				}
			}

			return xptobj;
		}
	}

	/**
	 * ************************************************************************
	 * This class represents an OCL invocation of the size operation. Size can
	 * be applied to anything with a -> and returns the number of elements of
	 * the object interpreted as a collection. If applied to a String it
	 * determines its length.
	 */
	public static class Size extends SchematronConstraintNode {

		/** Flag: This is a set operation */
		boolean setoper = false;

		/**
		 * Ctor
		 * 
		 * @param schemaObject
		 *            The schema object
		 * @param set
		 *            Flag: This is a set operation
		 */
		public Size(SchematronSchema schemaObject, boolean set) {
			this.schemaObject = schemaObject;
			this.setoper = set;
		}

		/**
		 * <p>
		 * Compile to an equivalent Xpath expression. The Set variant is
		 * compiled to count() and the String variant goes to string-length().
		 * </p>
		 * 
		 * @param ctx
		 *            BindingContext this node shall be compiled in
		 * @return Object containing the Xpath fragment
		 */
		public XpathFragment translate(BindingContext ctx) {

			// Translate the object
			SchematronConstraintNode obj = children.get(0);
			XpathFragment xpt = obj.translate(ctx);
			if (xpt.fragment.length() == 0)
				xpt.fragment = ".";

			// Is it the string length function or a set count?
			if (!setoper) {

				// It's a string length.
				xpt.fragment = "string-length(" + xpt.fragment + ")";

			} else {

				// It's a count
				xpt.fragment = "count(" + xpt.fragment + ")";
			}

			// And now the common treatment
			xpt.type = XpathType.NUMBER;
			xpt.priority = 11;
			xpt.atEnd = new BindingContext(BindingContext.CtxState.NONE);

			return xpt;
		}
	}

	/**
	 * ************************************************************************
	 * This class stands for an OCL concat operation. The operation concatenates
	 * the string type object with the string type argument. This implementation
	 * allows to specify more than one argument.
	 */
	public static class Concatenate extends SchematronConstraintNode {

		/**
		 * Ctor
		 * 
		 * @param schemaObject
		 *            The schema object
		 */
		public Concatenate(SchematronSchema schemaObject) {
			this.schemaObject = schemaObject;
		}

		/**
		 * <p>
		 * This compiles a multivalued Concatenate, which has been built from a
		 * series of OCL concat() functions to Xpath concat().
		 * </p>
		 * 
		 * @param ctx
		 *            BindingContext this node shall be compiled in
		 * @return Object containing the Xpath fragment
		 */
		public XpathFragment translate(BindingContext ctx) {

			// Loop over the arguments and construct the parameter list
			XpathFragment result = null;
			for (SchematronConstraintNode arg : children) {
				XpathFragment xptarg = arg.translate(ctx);
				if (result == null) {
					// First argument / the object
					result = xptarg;
				} else {
					// Other argument, merge and append
					String a1 = result.merge(xptarg);
					result.fragment += ", " + a1;
				}
			}

			// Surround with concat() function
			result.fragment = "concat(" + result.fragment + ")";

			// Accompany with the required fragment attributes
			result.type = XpathType.STRING;
			result.priority = 11;
			result.atEnd.setState(BindingContext.CtxState.NONE);

			return result;
		}
	}

	/**
	 * ************************************************************************
	 * This class stands for an OCL substring operation. The operation picks a
	 * range from a string type object.
	 */
	public static class Substring extends SchematronConstraintNode {

		/**
		 * Ctor
		 * 
		 * @param schemaObject
		 *            The schema object
		 */
		public Substring(SchematronSchema schemaObject) {
			this.schemaObject = schemaObject;
		}

		/**
		 * <p>
		 * This compiles a Substring object to its Xpath equivalent.
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
			XpathFragment xptfr = children.get(1).translate(ctx);
			if (xptfr.fragment.length() == 0)
				xptfr.fragment = ".";
			XpathFragment xptto = children.get(2).translate(ctx);
			if (xptto.fragment.length() == 0)
				xptto.fragment = ".";

			// Merge
			String fr = xptobj.merge(xptfr);
			// String to = xptobj.merge( xptto );

			// Construct the substring function
			xptobj.fragment = "substring(" + xptobj.fragment + ", ";
			xptobj.fragment += fr + ", ";
			if (xptto.priority < 5)
				xptto.bracket();
			if (xptfr.priority <= 5)
				xptfr.bracket();
			xptobj.fragment += xptto.fragment + " - " + xptfr.fragment
					+ " + 1)";

			// Accompany with the required fragment attributes
			xptobj.type = XpathType.STRING;
			xptobj.priority = 11;
			xptobj.atEnd.setState(BindingContext.CtxState.NONE);

			return xptobj;
		}
	}

	/**
	 * ************************************************************************
	 * This class stands for an the changeCase operations on the CharacterString
	 * object.
	 */
	public static class ChangeCase extends SchematronConstraintNode {

		protected String operation = null;

		/**
		 * Ctor
		 * 
		 * @param schemaObject
		 *            The schema object
		 * @param oper
		 *            the actual operation: toUpper, toLower
		 */
		public ChangeCase(SchematronSchema schemaObject, String oper) {
			this.schemaObject = schemaObject;
			this.operation = oper;
		}

		/**
		 * <p>
		 * ChangeCase operations cannot be translated into Xpath 1.0.
		 * </p>
		 * 
		 * @param ctx
		 *            BindingContext this node shall be compiled in
		 * @return Object containing the Xpath fragment
		 */
		public XpathFragment translate(BindingContext ctx) {

			// Result data
			XpathFragment result = new XpathFragment(11, "***ERROR[123]***");

			return result;
		}
	}

	/**
	 * ************************************************************************
	 * This class stands for matches operation, which this implemention added to
	 * OCL's core functions.
	 */
	public static class Matches extends SchematronConstraintNode {

		/**
		 * Ctor
		 * 
		 * @param schemaObject
		 *            The schema object
		 */
		public Matches(SchematronSchema schemaObject) {
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
			SchematronSchema.ExtensionFunctionTemplate eft = schemaObject.extensionFunctions
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
	public static class Arithmetic extends SchematronConstraintNode {

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
		public Arithmetic(SchematronSchema schemaObject, String oper) {
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
	 * This class represents an OCL variable. It wraps the Declaration node of
	 * the OclNode object. If it happens to be boolean it can be negated.
	 */
	public static class Variable extends SchematronConstraintNode {

		// Wrapped declaration object
		protected OclNode.Declaration vardecl;

		/**
		 * Ctor
		 * 
		 * @param schemaObject
		 *            The schema object
		 * @param vardecl
		 *            OclNode.Declaration object
		 * @param neg
		 *            Negation flag
		 */
		public Variable(SchematronSchema schemaObject,
				OclNode.Declaration vardecl, boolean neg) {
			this.schemaObject = schemaObject;
			this.vardecl = vardecl;
			this.negated = neg;
		}

		/**
		 * Variable name inquiry function.
		 * 
		 * @return Name of variable
		 */
		public String getName() {
			return vardecl.name;
		}

		/**
		 * <p>
		 * This method determines whether this variable is identical to the one
		 * passed as argument.
		 * </p>
		 * 
		 * @param vardecl
		 *            The Declaration of the variable
		 * @return Flag indicating the inquired dependency
		 */
		public boolean isDependentOn(OclNode.Declaration vardecl) {
			return this.vardecl == vardecl;
		}

		/**
		 * <p>
		 * This method determines whether this variable is identical to the one
		 * passed as argument.
		 * </p>
		 * 
		 * @param vardecl
		 *            The Declaration of the variable
		 * @return Flag indicating the inquired property
		 */
		public boolean isVarOrAttribBased(OclNode.Declaration vardecl) {
			return isDependentOn(vardecl);
		}

		/**
		 * <p>
		 * This inquires the Attribute node this Variable is generated by if
		 * any.
		 * </p>
		 * 
		 * @return The retrieved Attribute node if there is such a thing
		 */
		public Attribute generatingAttribute() {
			// Find the iterator, which binds this variable
			SchematronConstraintNode binds = null;
			SchematronConstraintNode boundTo = null;
			for (SchematronConstraintNode scn = parent; scn != null; scn = scn.parent) {
				if ((boundTo = scn.nodeVariableIsBoundTo(vardecl)) != null) {
					binds = scn;
					break;
				}
			}
			if (binds == null)
				return null; // Should not occur!
			// Ask the node the variable is bound to
			return boundTo.generatingAttribute();
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
			SchematronConstraintNode binds = null;
			SchematronConstraintNode boundTo = null;
			for (SchematronConstraintNode scn = parent; scn != null; scn = scn.parent) {
				if ((boundTo = scn.nodeVariableIsBoundTo(vardecl)) != null) {
					binds = scn;
					break;
				}
			}
			if (binds == null)
				return false;
			return boundTo.hasSimpleType();
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
			SchematronConstraintNode binds = null;
			SchematronConstraintNode boundTo = null;
			for (SchematronConstraintNode scn = parent; scn != null; scn = scn.parent) {
				if ((boundTo = scn.nodeVariableIsBoundTo(vardecl)) != null) {
					binds = scn;
					break;
				}
			}
			if (binds == null)
				return true;
			return boundTo.hasIdentity();
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

			// Find out the parent node, which binds the variable
			SchematronConstraintNode binds = null;
			for (SchematronConstraintNode scn = parent; scn != null; scn = scn.parent) {
				if (scn.nodeVariableIsBoundTo(vardecl) != null) {
					binds = scn;
					break;
				}
			}

			// Take cases on what can be done for Xpath 1.0
			if (binds != null && binds instanceof Let) {
				// Treatment for 'let' expression ...
				// Grab the initial value expression
				Let let = (Let) binds;
				SchematronConstraintNode expr = null;
				for (int i = 0; i < let.children.size() - 1; i++) {
					if (let.vardecls[i] == vardecl) {
						expr = let.children.get(i);
						break;
					}
				}
				// Translation depends on binding context
				if (let.letctx.state == BindingContext.CtxState.ATCURRENT) {
					// If we are at the initial binding context, we wrap the
					// result in a variable. The value must be translated in
					// the binding context of the let.
					xpt = expr.translate(let.letctx);
					String var = xpt.findOrAdd(xpt.fragment);
					xpt.fragment = "$" + var;
					xpt.priority = 11;
				} else
					// Elsewhere: The current binding context applies and the
					// expression is just substituted.
					xpt = expr.translate(ctx);
			} else if (getName().equals("self")) {
				// Must be iterator context. Treat self
				xpt.atEnd = new BindingContext(
						BindingContext.CtxState.ATCURRENT);
				if (ctx.state != BindingContext.CtxState.ATCURRENT) {
					xpt.fragment = "current()";
				}
			} else {
				// Variable other than self
				if (ctx.state != BindingContext.CtxState.OTHER)
					xpt.fragment = "***ERROR[124," + getName() + "]***";
				else {
					int steps = 0;
					boolean found = false;
					if (ctx.vars != null)
						for (int i = ctx.vars.size() - 1; i >= 0; --i) {
							BindingContext.CtxElmt ce = ctx.vars.get(i);
							steps += ce.noOfSteps;
							if (ce.vardecl == vardecl) {
								found = true;
								break;
							}
						}
					if (!found)
						xpt.fragment = "***ERROR[124," + getName() + "]***";
					else {
						// So, the variable is on the path
						if (steps > 0) {
							xpt.fragment = "..";
							for (int i = 2; i <= steps; i++)
								xpt.fragment += "/..";
						}
						if (steps > 1)
							xpt.priority = 9;
					}
					xpt.atEnd = new BindingContext(
							BindingContext.CtxState.OTHER);
				}
			}

			return xpt;
		}
	}

	/**
	 * ************************************************************************
	 * <p>
	 * This class represents a chain of attribute selectors based on some value
	 * source such as a variable, a select() or allInstances. The value source
	 * is the sole child of the Attribute object.
	 * </p>
	 */
	public static class Attribute extends SchematronConstraintNode {

		protected static class AttrComp {
			protected OclNode.AttributeCallExp main = null;
			protected OclNode.AttributeCallExp absAttr = null;
			protected int absType = 0; // 0=normal, 1=absorption, 2=nilReason

			protected AttrComp(OclNode.AttributeCallExp at) {
				main = at;
			}

			protected AttrComp(AttrComp atc) {
				main = atc.main;
				absAttr = atc.absAttr;
				absType = atc.absType;
			}
		}

		protected AttrComp[] attributes;

		/**
		 * Ctor - starting from AttributeCallExp
		 * 
		 * @param schemaObject
		 *            The schema object
		 * @param attr
		 *            The (possibly first) AttributeCallExp object
		 * @param negated
		 *            May be negated if of type boolean
		 */
		public Attribute(SchematronSchema schemaObject,
				OclNode.AttributeCallExp attr, boolean negated) {
			this.schemaObject = schemaObject;
			this.attributes = new AttrComp[] { new AttrComp(attr) };
			this.negated = negated;
		}

		/**
		 * Ctor - starting from AttrComp
		 * 
		 * @param schemaObject
		 *            The schema object
		 * @param atc
		 *            AttrComp object
		 * @param negated
		 *            May be negated if of type boolean
		 */
		public Attribute(SchematronSchema schemaObject, AttrComp atc,
				boolean negated) {
			this.schemaObject = schemaObject;
			this.attributes = new AttrComp[] { new AttrComp(atc) };
			this.negated = negated;
		}

		/**
		 * <p>
		 * Append another AttributeCallExp and associated layout info as an
		 * additional qualification.
		 * </p>
		 * 
		 * @param aex
		 *            The AttributeCallExp to be appended be null)
		 */
		public void appendAttribute(OclNode.AttributeCallExp aex) {
			AttrComp[] attribs = new AttrComp[attributes.length + 1];
			for (int i = 0; i < attributes.length; i++) {
				attribs[i] = attributes[i];
			}
			attribs[attributes.length] = new AttrComp(aex);
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
		 * The type (1=simple absorption, 2=reason) and the associated OclNode
		 * are stored together with the last attribute component.
		 * 
		 * @param absorptionType
		 *            Implementation type: 1=normal absorption, 2=reason
		 * @param attr
		 *            The OclNode representing the absorbed property.
		 */
		public void appendAbsorbedAttribute(int absorptionType,
				OclNode.AttributeCallExp attr) {
			int last = attributes.length - 1;
			attributes[last].absAttr = attr;
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
		public Attribute splitBefore(int at) {
			if (at < 0 || at >= attributes.length)
				return null;
			// Create a new Attribute node, which contains the selectors
			// starting at the given position. This will also be the
			// returned value.
			Attribute atrite = new Attribute(schemaObject, attributes[at],
					negated);
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
				Attribute atleft = new Attribute(schemaObject, attributes[0],
						negated);
				for (int i = 1; i < at; i++)
					appendAttribute(attributes[i]);
				atleft.addChild(children.get(0));
				atrite.addChild(atleft);
			}
			return atrite;
		}

		/**
		 * <p>
		 * This Attribute predicate finds out, whether the last attribute
		 * component in the object is implemented as a group and is therefore
		 * absorbing its properties. If there is already a property absorbed on
		 * the attribute, the absorbed property will be asked.
		 * </p>
		 * <p>
		 * Note that this is a necessary condition for appying GML's nilReason
		 * pattern.
		 * </p>
		 * 
		 * @return The required flag indicating that properties are absorbed
		 */
		public boolean isPropertyAbsorbing() {

			AttrComp ac = attributes[attributes.length - 1];
			AttributeCallExp acex = ac.absType == 0 ? ac.main : ac.absAttr;
			PropertyInfo pi = (PropertyInfo) acex.selector.modelProperty;
			Type t = pi.typeInfo();
			ClassInfo ci = schemaObject.model.classById(t.id);
			if (ci != null) {
				if (ci.isUnionDirect())
					return true;
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
		 *            The Declaration of the variable
		 * @return Flag indicating the inquired property
		 */
		public boolean isVarOrAttribBased(OclNode.Declaration vardecl) {
			return children.get(0).isVarOrAttribBased(vardecl);
		}

		/**
		 * <p>
		 * This inquires the Attribute node this Attribute is generated by.
		 * Alas, it's this Attribute!
		 * </p>
		 * 
		 * @return The retrieved Attribute node
		 */
		public Attribute generatingAttribute() {
			return this;
		}

		/**
		 * <p>
		 * This method returns true if any of the OclNode.Attribute objects it
		 * is made of has a possible cardinality greater than 1.
		 * </p>
		 */
		public boolean isMultiple() {
			for (AttrComp at : attributes) {
				if (at.main.multMapping == MultiplicityMapping.ONE2MANY
						|| at.absType == 1
								&& at.absAttr.multMapping == MultiplicityMapping.ONE2MANY)
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

			ClassInfo ci;
			boolean result = true;

			switch (attributes[idx].absType) {

			case 0: // Normal attribute
				ci = attributes[idx].main.dataType.umlClass;
				if (ci != null) {
					Boolean indicatorSimpleType = XmlSchema
							.indicatorForObjectElementWithSimpleContent(ci);
					result = !XmlSchema.classHasObjectElement(ci)
							|| (indicatorSimpleType != null
									&& indicatorSimpleType);
				}
				break;
			case 1: // Normal absorption
				ci = attributes[idx].absAttr.dataType.umlClass;
				if (ci != null) {
					Boolean indicatorSimpleType = XmlSchema
							.indicatorForObjectElementWithSimpleContent(ci);
					result = !XmlSchema.classHasObjectElement(ci)
							|| (indicatorSimpleType != null
									&& indicatorSimpleType);
				}
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
		 * index <i>idx</i> results in a schema type, which carries identity.
		 * Note that for convenience reasons this also includes GML's xsi:nil
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
				ci = attributes[idx].main.dataType.umlClass;
				if (ci != null)
					result = XmlSchema.classCanBeReferenced(ci);
				break;
			case 1: // Normal absorption
				ci = attributes[idx].absAttr.dataType.umlClass;
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
		 * that attribute. Attributes can be negated, in which case they are
		 * boolean and not multiple.
		 * </p>
		 * 
		 * @param ctx
		 *            BindingContext this node shall be compiled in
		 * @return XpathFragment representing the Attribute access
		 */
		public XpathFragment translate(BindingContext ctx) {

			// First, obtain the feature attribute expressed by PropertyInfo
			// objects.
			PropertyInfo[] props = new PropertyInfo[attributes.length];
			for (int i = 0; i < props.length; i++) {
				props[i] = (PropertyInfo) attributes[i].main.selector.modelProperty;
			}

			// Translate the object. This is a variable (self in most of the
			// cases), or AllInstances() or any type of object producing
			// iterator such as Select().
			SchematronConstraintNode objnode = children.get(0);
			XpathFragment obj = objnode.translate(ctx);

			// Get the prefixes that possibly surround the identifier contained
			// in xlink:href references
			String alpha = schemaObject.alpha;
			String beta = schemaObject.beta;
			boolean alphaEx = alpha != null && alpha.length() > 0;
			boolean betaEx = beta != null && beta.length() > 0;

			// Now step along the properties and generate the associated Xpath
			// code for it.
			boolean lastWasSimple = objnode.hasSimpleType();

			for (int idx = 0; idx < props.length && !lastWasSimple; idx++) {

				// The property
				PropertyInfo pi = props[idx];

				// Find out how the relation to the contained object is realized
				// in XML Schema:
				// 0 = simple, 1 = in-line embedded, 2 = per xlink:href, 3 = 1|2
				int conCode = 0;
				String cid = pi.typeInfo().id;
				ClassInfo ci = null;
				ClassInfo cip = null;
				if (cid != null) {
					cip = ci = pi.model().classById(cid);
				}
				// If absorbing assume the type of the absorbed entity
				if (attributes[idx].absType == 1) {
					ci = attributes[idx].absAttr.dataType.umlClass;
				}
				if (!hasSimpleType(idx)) {

					/*
					 * Not a simple type, use in-line as default, then check if
					 * byReference or inlineOrByReference
					 */
					conCode = 1;

					if (ci != null && XmlSchema.classCanBeReferenced(ci)) {

						String ref = pi.inlineOrByReference();
						if ("byreference".equals(ref)) {
							conCode = 2;
						} else if (!"inline".equals(ref)) {
							conCode = 3;
						}
					}
				}

				String propertyQName = schemaObject.getAndRegisterXmlName(pi);

				boolean is19139 = pi.matches("rule-xsd-all-naming-19139")
						|| (ci != null && ci.matches(
								"rule-xsd-cls-standard-19139-property-types"));
				boolean piIsGml33Encoded = pi.inClass()
						.matches("rule-xsd-cls-codelist-asDictionaryGml33");

				// Dispatch on containment cases
				if (conCode == 0) {

					/*
					 * 0: Simple type is contained. This also comprises access
					 * to property 'reason' in GML's nilReason treatment.
					 */

					if (obj.fragment.length() > 0) {

						if (obj.priority < 9) {
							obj.bracket();
						}

						obj.fragment += "/";
						obj.priority = 9;
					}
					obj.fragment += propertyQName;

					/*
					 * We also need to know whether the property has a codelist
					 * type.
					 */
					boolean iscodelist = ci != null
							&& ci.category() == Options.CODELIST
							&& ((ci.matches(
									"rule-xsd-cls-codelist-asDictionaryGml33")
									&& ci.asDictionaryGml33())
									|| (ci.matches(
											"rule-xsd-cls-codelist-asDictionary")
											&& ci.asDictionary())
									|| ci.matches(
											"rule-xsd-cls-standard-19139-property-types"));

					/*
					 * If it's a codelist and the property is not encoded
					 * according to GML 3.3 we prepare the CodeListValuePattern,
					 * to construct an XPath expression which can be used in
					 * comparisons with enumeration literals (for example when
					 * testing on allowed code values). an enumeration literal
					 * is always constructed according to the code list value
					 * pattern.
					 */
					String clvpat = "{value}";
					int nsubst = 1;

					if (ci != null && iscodelist && !piIsGml33Encoded) {

						String uri = ci.taggedValue("codeList");
						if (uri != null && uri.length() > 0) {
							clvpat = "{codeList}/{value}";
						}

						clvpat = this.schemaObject
								.determineCodeListValuePattern(ci, clvpat);

						clvpat = clvpat.replace("{codeList}", "',{codeList},'");
						clvpat = clvpat.replace("{value}", "',{value},'");
						if (clvpat.startsWith("',")) {
							clvpat = clvpat.substring(2);
						}
						if (clvpat.endsWith(",'")) {
							clvpat = clvpat.substring(0, clvpat.length() - 2);
						}
						if (!clvpat.startsWith("{")) {
							clvpat = "'" + clvpat;
						}
						if (!clvpat.endsWith("}")) {
							clvpat += "'";
						}
						if (!clvpat.equals("{value}")) {

							clvpat = "concat(" + clvpat + ")";
							nsubst = 2;

							/*
							 * We will be using a function as location step in
							 * the path expression. This is not supported by
							 * (the syntax of) XPath 1.0. Also see comments from
							 * Dimitre Novatchev and Michael Kay on
							 * https://stackoverflow.com/questions/333249/how-to
							 * -apply-the- xpath-function-substring-after and
							 * https://www.oxygenxml.com/archives/xsl-list/
							 * 200603/msg00610. html. However, XPath 2.0 / the
							 * xslt2 query binding supports this.
							 */
							schemaObject.setQueryBinding("xslt2");
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

					// Adjust relative adressing of variables
					if (obj.atEnd != null) {
						obj.atEnd.addStep();
					}

					// In a normal property access, we still have to treat some
					// special cases ...
					boolean atcurr = ctx.state == BindingContext.CtxState.ATCURRENT;

					if (attributes[idx].absType == 0) {

						if (is19139) {

							/*
							 * Under 19139 encoding, we will have to match
							 * another element level, even for simple types
							 */
							obj.fragment += "/*";
							if (obj.atEnd != null) {
								obj.atEnd.addStep();
							}

							// For codelists we have to add an attribute access
							if (ci != null
									&& ci.category() == Options.CODELIST) {

								if (nsubst == 2 && atcurr) {
									String v = obj.findOrAdd(obj.fragment);
									obj.fragment = "$" + v;
								}

								/*
								 * Only consider elements that have a value. In
								 * 19139 encoding, that means that a child
								 * element must be present. If we didn't add
								 * this check, then the comparison with the
								 * translated code/enumeration literal would
								 * fail for an empty element. However, we do not
								 * need the check if clvpat is just {value},
								 * because then an empty element would
								 * automatically be ignored.
								 */
								if (!clvpat.equals("{value}")) {
									obj.fragment += "[*]";
								}

								obj.fragment = createCodeListValueExpression(
										obj.fragment, clvpat, "@codeList",
										"@codeListValue");
							}

						} else if (iscodelist) {

							if (!piIsGml33Encoded) {

								/*
								 * So the property is not encoded according to
								 * the GML 3.3 encoding rule. In elder GMLs we
								 * might find the code list URI in
								 * the @codeSpace attribute. The value is the
								 * text of the property element (which has type
								 * gml:CodeType).
								 * 
								 * NOTE: We need to actually access the text
								 * node of the property element, not just the
								 * element node. Otherwise '.' would not work,
								 * for example when checking for
								 * property->notEmpty().
								 */

								/*
								 * Only consider elements that are not nil. If
								 * we didn't add this check, then the comparison
								 * with the translated code/enumeration literal
								 * would fail for an element that is nil.
								 * However, we only need this check if the
								 * property is nillable and clvpat is not equal
								 * to {value} (if it is just {value} then a nil
								 * element without text content would
								 * automatically be ignored).
								 */
								if (pi.voidable()
										&& !clvpat.equals("{value}")) {
									obj.fragment += "[not(@xsi:nil='true')]";
									schemaObject.registerNamespace("xsi");
								}

								obj.fragment = createCodeListValueExpression(
										obj.fragment, clvpat, "@codeSpace",
										"text()");

							} else {

								/*
								 * If using GML 3.3 type codelist treatment, we
								 * have to refer to the xlink:href attribute
								 */
								if (nsubst == 2 && atcurr) {
									String v = obj.findOrAdd(obj.fragment);
									obj.fragment = "$" + v;
								}
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
					String frag_inl = null;
					String frag_ref = null;
					boolean isVar = false;

					if (conCode == 2 || conCode == 3) {

						/*
						 * If not inline, we will create a 'let' variable unless
						 * we are in the middle of some relative addressing
						 * scheme
						 */
						if (obj.atEnd.state == BindingContext.CtxState.OTHER
								&& obj.fragment.length() > 0
								&& !obj.fragment.startsWith(".")) {

							// store current obj.fragment as a variable
							String var = obj.findOrAdd(obj.fragment);
							obj.fragment = "$" + var;
							obj.priority = 11;
							isVar = true;
						}
					}

					if (conCode == 1 || conCode == 3) {

						// In-line containment must be treated
						// --> .../attr/*
						frag_inl = obj.fragment;

						if (frag_inl.length() > 0) {

							if (obj.priority < 9) {

								frag_inl = "(" + frag_inl + ")";
								/*
								 * NOTE: Setting the obj.priority here (to 11)
								 * is not necessary. The priority of the whole
								 * obj.fragment, once it has been created, is
								 * important. It will be set later on.
								 */
							}

							frag_inl += "/";
						}
						frag_inl += propertyQName;
						if (obj.atEnd != null)
							obj.atEnd.addStep();
						frag_inl += "/*";
						if (obj.atEnd != null)
							obj.atEnd.addStep();

						if (obj.atEnd != null) {
							obj.atEnd.setState(BindingContext.CtxState.OTHER);
						}
					}

					if (conCode == 2 || conCode == 3) {

						// Reference containment must be treated

						if (!isVar) {
							// If not stored in a variable, the object now
							// has to be compiled in an undefined context.
							BindingContext ctx1 = ctx.clone();
							ctx1.setState(BindingContext.CtxState.OTHER);
							XpathFragment obj1 = children.get(0)
									.translate(ctx1);

							String frag = obj.merge(obj1);
							obj.fragment = frag;
						}

						// --> //*[@gml:id]=.../attr/@xlink:href]
						// --> if @gml:id is surrounded with additional text,
						// --> a concat() construct is used.

						String idAttributeFrag;
						if (is19139) {
							idAttributeFrag = "@id";
						} else {
							idAttributeFrag = "@gml:id";
							schemaObject.registerNamespace("gml");
						}

						String attxlink = obj.fragment;
						if (attxlink.length() > 0) {

							if (obj.priority < 9) {
								attxlink = "(" + attxlink + ")";
								/*
								 * NOTE: Setting the obj.priority here (to 11)
								 * is not necessary. The priority of the whole
								 * obj.fragment, once it has been created, is
								 * important. It will be set later on.
								 */
							}

							attxlink += "/";
						}
						attxlink += propertyQName;
						attxlink += "/@xlink:href";
						frag_ref = "//*[";
						if (alphaEx || betaEx) {
							frag_ref += "concat(";
							if (alphaEx) {
								frag_ref += "'" + alpha + "',";
							}
							frag_ref += idAttributeFrag;
							if (betaEx) {
								frag_ref += ",'" + beta + "'";
							}
							frag_ref += ")";
						} else {
							frag_ref += idAttributeFrag;
						}
						frag_ref += "=" + attxlink + "]";
						schemaObject.registerNamespace("xlink");

						// Whatever binding context we had before, it is lost
						if (obj.atEnd != null) {
							obj.atEnd.setState(BindingContext.CtxState.OTHER);
						}
					}

					// Set the fragment value, possibly combining both
					// containment representations
					if (conCode == 3) {

						obj.fragment = frag_inl + " | " + frag_ref;
						obj.priority = 8;

					} else if (conCode == 1) {

						obj.fragment = frag_inl;
						obj.priority = 9;

					} else {

						obj.fragment = frag_ref;
						obj.priority = 10;
					}
				}
			}

			// Treat negation. Note that if this is being negated it must be
			// unique and boolean ...
			if (negated) {
				obj.fragment = "not(" + obj.fragment + ")";
				obj.priority = 11;
				obj.atEnd.setState(BindingContext.CtxState.NONE);
			}

			// Return fragment
			return obj;
		}

		private String createCodeListValueExpression(String xpathFragment,
				String codeListValuePattern,
				String codeListReplacementExpression,
				String valueReplacementExpression) {

			/*
			 * 2018-03-02 JE: The expression must support the case that multiple
			 * values are encoded for the code list valued property. In that
			 * situation, the {codeList} access as well as the {value} access
			 * must result in single values, not node sets (because they are not
			 * supported by fn:concat(...), which is used in the code list value
			 * pattern unless that pattern is equal to {value}). For a
			 * multi-valued property, the expression should create a sequence of
			 * text values.
			 * 
			 * NOTE: The UnitTest
			 * SchematronTest.schematronTestOclOnCodelistTypedProperty is used
			 * to check this encoding.
			 */
			String clvp = codeListValuePattern
					.replace("{codeList}", codeListReplacementExpression)
					.replace("{value}", valueReplacementExpression);

			String result = xpathFragment + "/" + clvp;

			return result;
		}
	}

	/**
	 * ************************************************************************
	 * This wraps any form of Literal value from the OclNode.
	 */
	public static class Literal extends SchematronConstraintNode {

		OclNode.LiteralExp literal;

		/**
		 * Ctor
		 * 
		 * @param schemaObject
		 *            The schema object
		 * @param lit
		 *            OclNode.LiteralExp object
		 * @param neg
		 *            Negation flag
		 */
		public Literal(SchematronSchema schemaObject, OclNode.LiteralExp lit,
				boolean neg) {
			this.schemaObject = schemaObject;
			literal = lit;
			negated = neg;
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
			String value = literal.asString();
			XpathType type = XpathType.STRING;

			// Some special treatments
			if (literal instanceof OclNode.StringLiteralExp) {

				// If string, surround with ''
				value = "'" + value + "'";

			} else if (literal instanceof OclNode.EnumerationLiteralExp) {

				// Enums and codelists are to be treated like strings, except
				// Boolean class ones, which are treated as Boolean constants
				OclNode.EnumerationLiteralExp litex = (OclNode.EnumerationLiteralExp) literal;
				ClassInfo ci = litex.dataType.umlClass;

				if (ci != null && ci.name().equals("Boolean")) {

					// The Boolean case. Translate as if true or false had been
					// written instead of Boolean::true and ::false.
					boolean val = value.equalsIgnoreCase("TRUE");
					if (negated) {
						val = !val;
					}
					value = val ? "true()" : "false()";
					type = XpathType.BOOLEAN;

				} else {

					/*
					 * Enums and codelist constants will generally be translated
					 * to string constants representing their value.
					 * 
					 * Codelists, additionally, can carry the 'codeList' tagged
					 * value the value of which will be combined according to a
					 * pattern given by the 'codeListValuePattern' tagged value
					 * to produce an identifier.
					 */

					/*
					 * TBD 2018-02-01 JE: The value of a code/enum typically is
					 * the literal as a String, i.e. in case of a code/enum its
					 * name. However, some communities may prefer using the
					 * initial value, if it exists. In that case, the initial
					 * value would have to be used as value. However, it's
					 * unclear how to configure this preference. Should it be a
					 * global option? But what about code lists in external
					 * schemas, which may not have been encoded according to
					 * this preference? Does that mean that we can check based
					 * upon the encoding rule of the code list? Or should the
					 * tagging of a code list be extended, for example adding a
					 * tagged value 'initialValueAsCode' of type boolean, which,
					 * if set to true, would instruct ShapeChange to encode a
					 * code/enum as literal using the initial value, if it
					 * exists?
					 */
					// PropertyInfo pi = litex.umlProperty;
					// if (pi != null && pi.initialValue() != null) {
					//
					// }

					boolean iscodelist = ci != null
							&& ci.category() == Options.CODELIST;

					// Default for enums
					String clUri = "";
					String clVPat = "{value}";

					// Treatment of codelists
					if (iscodelist) {

						String uri = ci.taggedValue("codeList");
						if (uri != null && uri.length() > 0) {
							clVPat = "{codeList}/{value}";
							clUri = uri;
						}
						clVPat = this.schemaObject
								.determineCodeListValuePattern(ci, clVPat);
					}

					// Everything is in place. We can now generate the
					// codelist value from the pattern ...
					clVPat = clVPat.replace("{value}", value);
					value = clVPat.replace("{codeList}", clUri);
					// Make literal value
					value = "'" + value + "'";
				}

			} else if (literal instanceof OclNode.BooleanLiteralExp) {

				// Booleans may need consideration of negation

				OclNode.BooleanLiteralExp lit = (OclNode.BooleanLiteralExp) literal;
				boolean val = lit.value;
				if (negated) {
					val = !val;
				}
				value = val ? "true()" : "false()";
				type = XpathType.BOOLEAN;

			} else if (literal instanceof OclNode.IntegerLiteralExp
					|| literal instanceof OclNode.RealLiteralExp) {

				// The value is already o.k., but we need to set the type
				type = XpathType.NUMBER;

			} else if (literal instanceof OclNode.DateTimeLiteralExp) {

				OclNode.DateTimeLiteralExp lt = (OclNode.DateTimeLiteralExp) literal;
				if (lt.current) {
					// This references the current date and cannot be
					// represented in Xpath ...
					value = "***ERROR[125]***";
				} else {
					// A defined date is being used
					GregorianCalendar dt = lt.dateTime;
					int y = dt.get(Calendar.YEAR);
					int m = dt.get(Calendar.MONTH) + 1;
					int d = dt.get(Calendar.DAY_OF_MONTH);
					value = String.format("'%04d-%02d-%02d'", y, m, d);
				}
				type = XpathType.BOOLEAN;

			} else if (literal instanceof OclNode.OclVoidLiteralExp) {

				// Guess this is an empty nodeset
				value = "/*[false()]";
				type = XpathType.NODESET;

			}

			// Return what we have
			XpathFragment xpt = new XpathFragment(11, value, type);
			return xpt;
		}
	}

	/**
	 * ************************************************************************
	 * This class represents an if ... then ... else ... endif construct.
	 * Negation is already applied in construction by switching then and else.
	 */
	public static class IfThenElse extends SchematronConstraintNode {

		/**
		 * Ctor
		 * 
		 * @param schemaObject
		 *            The schema object
		 */
		public IfThenElse(SchematronSchema schemaObject) {
			this.schemaObject = schemaObject;
		}

		/**
		 * <p>
		 * This predicate finds out whether the IfThenElse results in a simple
		 * XML schema type.
		 * </p>
		 * 
		 * @return Flag indicating whether the node has a simple type
		 */
		public boolean hasSimpleType() {
			return children.get(1).hasSimpleType()
					&& children.get(2).hasSimpleType();
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

			// The 3 arguments
			SchematronConstraintNode con = children.get(0);
			SchematronConstraintNode thn = children.get(1);
			SchematronConstraintNode els = children.get(2);

			// First things to compile are the then and else parts. They have
			// to compile in the outer context of the if-then-else.
			XpathFragment xptthn = thn.translate(ctx);
			XpathFragment xptels = els.translate(ctx);

			// Get Xpath types of both operands
			XpathType thntype = xptthn.type;
			XpathType elstype = xptels.type;

			// Both have to be merged, xptthn now carries the common info
			String elsepart = xptthn.merge(xptels);

			// If both results are nodesets, we can apply the nodeset trick.
			// Otherwise we apply the string trick.
			if (thntype == XpathType.NODESET && elstype == XpathType.NODESET) {

				// Both branches are nodesets. So we will compile the condition
				// in the merged context of both braches
				XpathFragment xptcon = con.translate(xptthn.atEnd);

				// Merge it and transfer the condition to a let variable
				String conpart = xptthn.merge(xptcon);
				String convar = xptthn.findOrAdd(conpart);

				// Construct the result expression
				xptthn.fragment += "[$" + convar + "] | " + elsepart + "[not($"
						+ convar + ")]";
				xptthn.priority = 8;

			} else {

				// At least one branch is not a nodeset. We first have to
				// compile the condition using the original context.
				XpathFragment xptcon = con.translate(ctx);

				// Merge to common fragment object and create let variables out
				// of all 3 constituents.
				String conpart = xptthn.merge(xptcon);
				String convar = xptthn.findOrAdd(conpart);
				String thnvar = xptthn.findOrAdd(xptthn.fragment);
				String elsvar = xptthn.findOrAdd(elsepart);

				/*
				 * Construct the result expression. The trick is to concatenate
				 * substrings which either comprise the full argument or
				 * nothing, depending on the value of the predicate.
				 */
				xptthn.fragment = "concat( " + "substring($" + thnvar + ","
						+ "number(not($" + convar + "))*" + "string-length($"
						+ thnvar + ")+1), " + "substring($" + elsvar + ","
						+ "number($" + convar + ")*" + "string-length($"
						+ elsvar + ")+1)" + " )";
				xptthn.priority = 11;

				// Adjust the type if necessary
				xptthn.atEnd.setState(BindingContext.CtxState.NONE);
				if (thntype == XpathType.NUMBER
						|| elstype == XpathType.NUMBER) {
					xptthn.fragment = "number( " + xptthn.fragment + " )";
					xptthn.type = XpathType.NUMBER;
				} else if (thntype == XpathType.BOOLEAN
						|| elstype == XpathType.BOOLEAN) {
					xptthn.fragment = "boolean( " + xptthn.fragment + " )";
					xptthn.type = XpathType.BOOLEAN;
				} else
					xptthn.type = XpathType.STRING;
			}

			return xptthn;
		}
	}

	/**
	 * ************************************************************************
	 * This class represents the 'let' construct. Being part of the Logic system
	 * Let can be negated, which is realized by passing the negation down to the
	 * 'body' expression. This is mananged from outside when the Let node is
	 * created.
	 */
	public static class Let extends SchematronConstraintNode {

		// The variable declaration attached to the iterator
		OclNode.Declaration[] vardecls;
		// The binding context of the Let while translating it
		BindingContext letctx;

		/**
		 * Ctor
		 * 
		 * @param schemaObject
		 *            The schema object
		 * @param vardecl
		 *            OclNode.Declaration object
		 */
		public Let(SchematronSchema schemaObject,
				OclNode.Declaration[] vardecls) {
			this.schemaObject = schemaObject;
			this.vardecls = vardecls;
		}

		/**
		 * This method determines whether the Let binds the given variable
		 * declaration and if it does, which is the expression the variable is
		 * bound to.
		 * 
		 * @param vardecl
		 *            The variable Declaration object
		 * @return Node the variable is bound to or null
		 */
		public SchematronConstraintNode nodeVariableIsBoundTo(
				OclNode.Declaration vardecl) {
			int idx = 0;
			for (OclNode.Declaration dcl : vardecls) {
				if (dcl == vardecl)
					return this.children.get(idx);
				idx++;
			}
			return null;
		}

		/**
		 * <p>
		 * This compiles the node and its children to an Xpath expression
		 * fragment.
		 * </p>
		 * <p>
		 * The object is translated in the given context and its ending
		 * position, supplemented by the defined binding variables bearing
		 * values given by expressions. The variables define the context for the
		 * compilation of the body, however this is not represented in the
		 * BindingContext, because the method of fetching the variables is
		 * completely different than with iterator variables.
		 * </p>
		 * 
		 * @param ctx
		 *            BindingContext this node shall be compiled in
		 * @return Object containing the fragment and its operator priority
		 */
		public XpathFragment translate(BindingContext ctx) {

			// Compile the body. Note that all treatment of the variable
			// definitions of the 'let' construct itself is done way down in
			// the Variable nodes whenever a variable of the 'let' is made
			// use of.
			SchematronConstraintNode body = children.get(children.size() - 1);
			letctx = ctx;
			XpathFragment xpt = body.translate(ctx);
			return xpt;
		}
	}

	/**
	 * ************************************************************************
	 * This is generated for unimplemented material.
	 */
	public static class Error extends SchematronConstraintNode {

		/**
		 * Ctor
		 * 
		 * @param schemaObject
		 *            The schema object
		 */
		public Error(SchematronSchema schemaObject) {
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
	public static class MessageComment extends SchematronConstraintNode {

		protected String name = null;

		/**
		 * Ctor
		 * 
		 * @param schemaObject
		 *            The schema object
		 */
		public MessageComment(SchematronSchema schemaObject, String name) {
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
			for (SchematronConstraintNode arg : children) {
				XpathFragment sql = arg.translate(null);
				arglist[i++] = sql.fragment == null ? "*ERROR*" : sql.fragment;
			}
			return arglist;
		}

		@Override
		public XpathFragment translate(BindingContext ctx) {
			return null;
		}
	}
}
