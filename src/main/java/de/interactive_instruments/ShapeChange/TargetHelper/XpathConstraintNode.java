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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */

package de.interactive_instruments.ShapeChange.TargetHelper;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Ocl.OclNode;
import de.interactive_instruments.ShapeChange.Ocl.OclNode.AttributeCallExp;
import de.interactive_instruments.ShapeChange.Ocl.OclNode.Declaration;
import de.interactive_instruments.ShapeChange.Ocl.OclNode.MultiplicityMapping;
import de.interactive_instruments.ShapeChange.Target.XmlSchema.XmlSchema;

/**
 * 
 * XpathConstraintNode and its concrete derivations stand for a representation
 * of OCL contents, which are close to the capabilities of Xpath/Schematron and
 * the logic, which can be realized within Schematron Rules.
 * 
 * <br><br>
 * There are two basic patterns of use of these classes:
 * 
 * <ul>
 * <li>Creation of XpathConstraintNode objects while interpreting the original
 * OclConstraint objects.
 * <li>Translating Rules and Assert code fragments by calling the abstract
 * method <i>translateToAssertTest.</i>.
 * </ul>
 */
public abstract class XpathConstraintNode {

	/** The children of the XpathConstraintNode */
	protected ArrayList<XpathConstraintNode> children = new ArrayList<XpathConstraintNode>();

	/** General negation flag for all nodes */
	protected boolean negated = false;

	/** The parent reference */
	protected XpathConstraintNode parent = null;

	/** Link back to XpathHelper object */
	protected XpathHelper xpathHelper = null;

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
	public void addChild(XpathConstraintNode child) {
		children.add(child);
		child.parent = this;
	}

	/**
	 * 
	 * Method to inquire whether the the node inquired is a Logic node AND this
	 * logic node has the same <i>isAnd</i> polarity as specified in the
	 * parameter.
	 * <br><br>
	 * This implementation installs the default for all derivations except
	 * Logic.
	 * 
	 * 
	 * @param isAnd
	 *            Flag: Are we an AND? (not an OR)?
	 * @return True if this is Logic with the same polarity
	 */
	public boolean isAndOrLogic(boolean isAnd) {
		return false;
	}

	/**
	 * 
	 * This method determines whether the given expression depends on the
	 * Variable passed as argument.
	 * <br><br>
	 * This implementation defines the default behavior: Descend down and try to
	 * find the variable somewhere.
	 * 
	 * 
	 * @param vardecl
	 *            The Declaration of the variable
	 * @return Flag indicating the inquired dependency
	 */
	public boolean isDependentOn(OclNode.Declaration vardecl) {
		for (XpathConstraintNode scn : children)
			if (scn.isDependentOn(vardecl))
				return true;
		return false;
	}

	/**
	 * This method determines whether the node binds the given variable
	 * declaration. This can only happen for iterators.
	 * 
	 * @param vardecl
	 *            The variable Declaration object
	 * @return Flag indicating the requested property
	 */
	public boolean bindsVariable(OclNode.Declaration vardecl) {
		return false;
	}

	/**
	 * 
	 * This method determines whether the given expression is a Variable or an
	 * Attribute based on a Variable, which is identical to the one passed as
	 * argument.
	 * <br><br>
	 * This implementation defines the default behavior.
	 * 
	 * 
	 * @param vardecl
	 *            The Declaration of the variable
	 * @return Flag indicating the inquired property
	 */
	public boolean isVarOrAttribBased(OclNode.Declaration vardecl) {
		return false;
	}

	/**
	 * 
	 * By means of this function you can inquire which Attribute node is
	 * generating the objects represented by this node. Note that invocation is
	 * only sensible for iterators and attributes.
	 * 
	 * 
	 * @return The retrieved Attribute node if there is such a thing
	 */
	public Attribute generatingAttribute() {
		return null;
	}

	/**
	 * 
	 * This predicate finds out whether the given node may produce a set.
	 * <br><br>
	 * This is the default implementation providing the value false.
	 * 
	 * 
	 * @return Flag indicating whether the node can return multiple values
	 */
	public boolean isMultiple() {
		return false;
	}

	/**
	 * 
	 * This predicate finds out whether the given node is realized by means of a
	 * simple XML schema type.
	 * 
	 * 
	 * @return Flag indicating whether the node has a simple type
	 */
	public boolean hasSimpleType() {
		return true;
	}

	/**
	 * 
	 * This predicate finds out whether the given node is realized by means of a
	 * class, which conceptually has identity.
	 * 
	 * 
	 * @return Flag indicating whether the node is an identity carrying type
	 */
	public boolean hasIdentity() {
		return false;
	}

	/**
	 * 
	 * Find out whether this construct contains a node of type
	 * XpathConstraintNode.Error. In this case the whole tree is in error.
	 * 
	 * 
	 * @return Error flag
	 */
	public boolean containsError() {
		if (this instanceof Error)
			return true;
		for (XpathConstraintNode node : children)
			if (node.containsError())
				return true;
		return false;
	}

	/**
	 * 
	 * The primary information stored in this class is whether there is
	 * currently a nodeset context at all - NONE if the expression is not a
	 * nodeset - and if the context is currently identical to current() -
	 * ATCURRENT. All other contexts are combined in OTHER.
	 * <br><br>
	 * The vars part comes into living as soon as variables are encountered.
	 * They are tracked together with the information how far they are up the
	 * stack.
	 * 
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

		// Push a new variable declaration
		public void pushDeclaration(Declaration vd) {
			if (vars == null)
				vars = new ArrayList<CtxElmt>();
			vars.add(new CtxElmt(vd));
			this.state = CtxState.OTHER;
		}

		// Increment the child step counter from the last declaration
		public void addStep() {
			if (vars == null || vars.size() == 0)
				return;
			++(vars.get(vars.size() - 1).noOfSteps);
		}

		// Do away with the last variable declaration
		public void popDeclaration() {
			if (vars == null || vars.size() == 0)
				return;
			vars.remove(vars.size() - 1);
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
	 * 
	 * This auxiliary class encapsulates an Xpath expression, which can be
	 * formulated using variables defined using &lt;let&gt; expressions of a
	 * Schematron &lt;rule&gt;. Additionally there is a number indicating the XPath
	 * operator precedence of that fragment. Priorities are as follows:
	 * 
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
	public static class XpathFragment {
		public int priority;
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
		}

		// Function to find or add a variable given the expression
		public String findOrAdd(String ex) {
			if (lets == null)
				lets = new TreeMap<String, String>();
			for (Map.Entry<String, String> ve : lets.entrySet()) {
				if (ve.getValue().equals(ex))
					return ve.getKey();
			}
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

		// Auxiliary function to replace variable names
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
	 * 
	 * This abstract method compiles a node to an XPath expression fragment.
	 * 
	 * 
	 * @param ctx
	 *            BindingContext this node shall be compiled in
	 * @return Object containing the Xpath fragment
	 */
	abstract public XpathFragment translate(BindingContext ctx);

	/**
	 * ************************************************************************
	 * 
	 * This class stands for logical operations AND, OR, XOR and EQV. Which of
	 * these is is coded in the state member logic.
	 * 
	 */
	public static class Logic extends XpathConstraintNode {

		protected enum LogicType {
			AND, OR, XOR, EQV
		}

		protected LogicType logic;

		/**
		 * Ctor
		 * 
		 * @param xpathHelper
		 *            The schema object
		 * @param logic
		 *            Flag to make this an AND (true) or an OR (false)
		 */
		public Logic(XpathHelper xpathHelper, LogicType logic) {
			this.xpathHelper = xpathHelper;
			this.logic = logic;
		}

		/**
		 * 
		 * Method to inquire whether the node inquired is a Logic node and this
		 * logic node has the same <i>isAnd</i> polarity as specified in the
		 * parameter. XORs and EQVs are ignored and yield false.
		 * 
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
		 * 
		 * This compiles the node and its children to an Xpath predicate, which
		 * can be inserted into a &lt;rule&gt;.
		 * <br><br>
		 * AND and OR are translated into their Xpath counterparts <i>and</i>
		 * and <i>or</i>. XOR will be realized as a != operator, EQV by an =
		 * operator.
		 * 
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
			for (XpathConstraintNode ocn : children) {
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
	 * 
	 * This class stands for comparisons. The operator is given as a String,
	 * which can take the values: =, &lt;&gt;, &lt;, &lt;=, &gt;, &gt;=.
	 * 
	 */
	public static class Comparison extends XpathConstraintNode {

		// The relational operator
		String opname;

		/**
		 * Ctor
		 * 
		 * @param xpathHelper
		 *            The schema object
		 * @param name
		 *            One of =, &lt;&gt;, &lt;, &lt;=, &gt;, &gt;=
		 */
		public Comparison(XpathHelper xpathHelper, String name) {
			this.xpathHelper = xpathHelper;
			opname = name.equals("<>") ? "!=" : name;
		}

		/**
		 * 
		 * This compiles the node and its children to Xpath. Xpath can express
		 * all required comparison operators.
		 * 
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
				XpathConstraintNode child = children.get(i);
				if (!child.hasSimpleType() && !child.hasIdentity()) {
					return new XpathFragment(11, "***ERROR[126]***");
				}
				child_xpt[i] = child.translate(ctx);
				if (child.hasIdentity()) {
					child_xpt[i].fragment = "generate-id("
							+ child_xpt[i].fragment + ")";
					child_xpt[i].priority = 11;
				}
				if (child_xpt[i].fragment.length() == 0)
					child_xpt[i].fragment = ".";
				if (child_xpt[i].priority <= refprio)
					child_xpt[i].bracket();
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
	 * 
	 * This one stands for the OCL <i>isEmpty()</i> and <i>notEmpty()</i>
	 * predicate operations. Which of these is meant is expressed in the state
	 * variable <i>negated</i>.
	 * 
	 */
	public static class Empty extends XpathConstraintNode {

		// negated means: isEmpty (false) or notEmpty (true)

		/**
		 * Ctor
		 * 
		 * @param xpathHelper
		 *            The schema object
		 * @param neg
		 *            Flag: isEmpty (false) and notEmpty (true)
		 */
		public Empty(XpathHelper xpathHelper, boolean neg) {
			this.xpathHelper = xpathHelper;
			negated = neg;
		}

		/**
		 * 
		 * This compiles the node and its children to an Xpath fragment. The
		 * translation is essentially the nodeset derived from the object part
		 * of the expression, because notEmpty() is fulfilled for a nodeset,
		 * which converts to a boolean true. isEmpty() requires an additional
		 * not().
		 * 
		 * 
		 * @param ctx
		 *            BindingContext this node shall be compiled in
		 * @return Object containing the fragment and its operator priority
		 */
		public XpathFragment translate(BindingContext ctx) {

			// Fetch and compile the object
			XpathConstraintNode obj = children.get(0);
			XpathFragment xpt = obj.translate(ctx);
			if (xpt.fragment.length() == 0)
				xpt.fragment = ".";

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
	public static class Exists extends XpathConstraintNode {

		// The variable declaration attached to the iterator
		OclNode.Declaration vardecl;

		/**
		 * Ctor
		 * 
		 * @param xpathHelper
		 *            The schema object
		 * @param vardecl
		 *            OclNode.Declaration object
		 * @param neg
		 *            Negation flag
		 */
		public Exists(XpathHelper xpathHelper, OclNode.Declaration vardecl,
				boolean neg) {
			this.xpathHelper = xpathHelper;
			this.negated = neg;
			this.vardecl = vardecl;
		}

		/**
		 * This method determines whether the Exists binds the given variable
		 * declaration.
		 * 
		 * @param vardecl
		 *            The variable Declaration object
		 * @return Flag indicating the requested property
		 */
		public boolean bindsVariable(OclNode.Declaration vardecl) {
			return this.vardecl == vardecl;
		}

		/**
		 * 
		 * This compiles the node and its children to an Xpath expression
		 * fragment.
		 * <br><br>
		 * The object is translated in the given context and its ending
		 * position, supplemented by the binding variable, defines the context
		 * for the compilation of the body, which is appended as a predicate
		 * bracket. If negated an additional not() is applied.
		 * 
		 * 
		 * @param ctx
		 *            BindingContext this node shall be compiled in
		 * @return Object containing the fragment and its operator priority
		 */
		public XpathFragment translate(BindingContext ctx) {

			// Fetch and compile the object
			XpathConstraintNode obj = children.get(0);
			XpathFragment xpt = obj.translate(ctx);

			// Prepare the binding context for compilation of the iterator
			// body. This is primarily the context at the end of the object
			// plus the variable.
			BindingContext bodyctx = xpt.atEnd.clone();
			bodyctx.pushDeclaration(vardecl);

			// Compile the boolean expression in the iterator body
			XpathConstraintNode pred = children.get(1);
			XpathFragment prd = pred.translate(bodyctx);
			prd.atEnd = null; // This suppresses the merging of ending contexts

			// Append the boolean expression as a predicate filter
			String filter = xpt.merge(prd);
			if (xpt.priority < 10)
				xpt.bracket();
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
	public static class Unique extends XpathConstraintNode {

		// The variable declaration attached to the iterator
		OclNode.Declaration vardecl;

		/**
		 * Ctor
		 * 
		 * @param xpathHelper
		 *            The schema object
		 * @param vardecl
		 *            OclNode.Declaration object
		 * @param neg
		 *            Negation flag
		 */
		public Unique(XpathHelper xpathHelper, OclNode.Declaration vardecl,
				boolean neg) {
			this.xpathHelper = xpathHelper;
			this.negated = neg;
			this.vardecl = vardecl;
		}

		/**
		 * This method determines whether the Unique binds the given variable
		 * declaration.
		 * 
		 * @param vardecl
		 *            The variable Declaration object
		 * @return Flag indicating the requested property
		 */
		public boolean bindsVariable(OclNode.Declaration vardecl) {
			return this.vardecl == vardecl;
		}

		/**
		 * 
		 * This compiles the node and its children to an Xpath expression
		 * fragment.
		 * <br><br>
		 * The object is translated in the given context. If negated an
		 * additional not() is applied.
		 * 
		 * 
		 * @param ctx
		 *            BindingContext this node shall be compiled in
		 * @return Object containing the fragment and its operator priority
		 */
		public XpathFragment translate(BindingContext ctx) {

			// Local negation variable allows to absorb negation en route
			boolean neg = negated;

			// Fetch and compile the object
			XpathConstraintNode obj = children.get(0);
			XpathFragment xpt = obj.translate(ctx);

			// Get hold of the expression argument and take cases on its
			// nature ...
			XpathConstraintNode expr = children.get(1);
			if (!expr.isDependentOn(vardecl)) {

				// A constant expression can be unique only if the preimage
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
				if (obj instanceof Attribute) {
					Attribute obat = (Attribute) obj;
					ClassInfo ci = obat.attributes[obat.attributes.length
							- 1].main.dataType.umlClass;
					if (ci != null) {
						Boolean indicatorSimpleType = XmlSchema
								.indicatorForObjectElementWithSimpleContent(
										ci);
						simple = !XmlSchema.classHasObjectElement(ci)
								|| (indicatorSimpleType != null
										&& indicatorSimpleType);
					}
				}
				if (simple) {
					// The object is expressed by means of a simple type. The
					// nodeset will need to undergo a pairwise value comparison
					String var = xpt.findOrAdd(xpt.fragment);
					xpt.fragment = "$" + var
							+ "[. = (preceding::*|ancestor::*)[count(.|$" + var
							+ ")=count($" + var + ")]]";
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
							XpathConstraintNode sv = exat.children.get(0);
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
							// Class is expressed by means of a simple type.
							// This
							// also includes 'reason' access in GML's nilReason
							// treatment.
							String var = xpt.findOrAdd(xpt.fragment);
							PropertyInfo pi = (PropertyInfo) exat.attributes[0].main.selector.modelProperty;
							String prop = xpathHelper.getAndRegisterXmlName(pi);
							String nil = "";
							if (exat.attributes[0].absType == 2)
								nil = "[@xsi:nil='true']/@nilReason";
							xpt.fragment = "$" + var + "[" + prop + nil
									+ " = (preceding::*|ancestor::*)[count(.|$"
									+ var + ")=count($" + var + ")]/" + prop
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
	public static class Select extends XpathConstraintNode {

		// The variable declaration attached to the iterator
		protected OclNode.Declaration vardecl;

		// Stored generator body
		protected XpathConstraintNode generatorBody = null;

		/**
		 * Ctor
		 * 
		 * @param xpathHelper
		 *            The schema object
		 * @param vardecl
		 *            OclNode.Declaration object
		 */
		public Select(XpathHelper xpathHelper, OclNode.Declaration vardecl) {
			this.xpathHelper = xpathHelper;
			this.vardecl = vardecl;
		}

		/**
		 * This method determines whether the Select binds the given variable
		 * declaration.
		 * 
		 * @param vardecl
		 *            The variable Declaration object
		 * @return Flag indicating the requested property
		 */
		public boolean bindsVariable(OclNode.Declaration vardecl) {
			return this.vardecl == vardecl;
		}

		/**
		 * 
		 * By means of this function you can inquire which Attribute node is
		 * generating the objects of this Select node if any.
		 * 
		 * 
		 * @return The retrieved Attribute node if there is such a thing
		 */
		public Attribute generatingAttribute() {
			return children.get(0).generatingAttribute();
		}

		/**
		 * 
		 * The value of Select is always a set.
		 * 
		 * 
		 * @return Flag indicating whether the node can return multiple values
		 */
		public boolean isMultiple() {
			return true;
		}

		/**
		 * 
		 * This predicate finds out whether the Select results in a simple XML
		 * schema type.
		 * 
		 * 
		 * @return Flag indicating whether the node has a simple type
		 */
		public boolean hasSimpleType() {
			return children.get(0).hasSimpleType();
		}

		/**
		 * 
		 * This predicate finds out whether the Select results in a collection
		 * of instances, which conceptually have identity.
		 * 
		 * 
		 * @return Flag indicating whether the node is an identity carrying type
		 */
		public boolean hasIdentity() {
			return children.get(0).hasIdentity();
		}

		/**
		 * 
		 * This compiles the Select node and its children to an Xpath expression
		 * fragment.
		 * <br><br>
		 * The object is translated in the given context and its ending
		 * position, supplemented by the binding variable, defines the context
		 * for the compilation of the body, which is appended as a predicate
		 * bracket. Note that Select is very similar to Exists - the only
		 * diffence being that the result is not interpreted in a Boolean way.
		 * 
		 * 
		 * @param ctx
		 *            BindingContext this node shall be compiled in
		 * @return Object containing the fragment and its operator priority
		 */
		public XpathFragment translate(BindingContext ctx) {

			// Fetch and compile the object
			XpathConstraintNode obj = children.get(0);
			XpathFragment xpt = obj.translate(ctx);

			// Prepare the binding context for compilation of the iterator
			// body. This is primarily the context at the end of the object
			// plus the variable.
			BindingContext bodyctx = xpt.atEnd.clone();
			bodyctx.pushDeclaration(vardecl);

			// Compile the boolean expression in the iterator body
			XpathConstraintNode pred = children.get(1);
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
	 * 
	 * This class represents the OCL operation allInstances(). AllInstances is
	 * based on a class literal and represents all instances of that class.
	 * 
	 */
	public static class AllInstances extends XpathConstraintNode {

		protected ClassInfo objectClass;

		/**
		 * Ctor
		 * 
		 * @param xpathHelper
		 *            The schema object
		 * @param ci
		 *            ClassInfo of the class to enumerate
		 * @param negated
		 *            May be negated if of type boolean
		 */
		public AllInstances(XpathHelper xpathHelper, ClassInfo ci,
				boolean negated) {
			this.xpathHelper = xpathHelper;
			this.objectClass = ci;
			this.negated = negated;
		}

		/**
		 * 
		 * Allinstances always produces a set.
		 * 
		 * 
		 * @return Flag indicating whether the node can return multiple values
		 */
		public boolean isMultiple() {
			return true;
		}

		/**
		 * 
		 * allInstances() is never simple.
		 * 
		 * 
		 * @return Flag indicating whether the node has a simple type
		 */
		public boolean hasSimpleType() {
			return false;
		}

		/**
		 * 
		 * This predicate finds out whether the allInstances results in a
		 * collection of instances, which conceptually have identity.
		 * 
		 * 
		 * @return Flag indicating whether the node is an identity carrying type
		 */
		public boolean hasIdentity() {
			return XmlSchema.classCanBeReferenced(objectClass);
		}

		/**
		 * 
		 * allInstances() is translated to a search for the given type. The
		 * result is a nodeset containing all the given features.
		 * <br><br>
		 * In compiling x.allInstances() we create a nodeset union (n
		 * <sub>1</sub>|...|n<sub>i</sub>), where n<sub>k</sub>=//T<sub>k</sub>
		 * [@gml:id] and T<sub>k</sub> is one of the concrete derivations of the
		 * type x, including x.
		 * 
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
					if (ci.isAbstract())
						continue;
					classnames.add(xpathHelper.getAndRegisterXmlName(ci));
				}
			}
			if (!objectClass.isAbstract())
				classnames.add(xpathHelper.getAndRegisterXmlName(objectClass));

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
					// xpathHelper.registerNamespace( "gml" );
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
	public static class KindOf extends XpathConstraintNode {

		// The class to be tested against
		protected ClassInfo argumentClass = null;
		// Exact match?
		protected boolean exact = false;

		/**
		 * Ctor
		 * 
		 * @param xpathHelper
		 *            The schema object
		 * @param exact
		 *            Flag: Only check the given type
		 * @param neg
		 *            Flag: Negated meaning
		 */
		public KindOf(XpathHelper xpathHelper, boolean exact, boolean neg) {
			this.xpathHelper = xpathHelper;
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
		 * 
		 * This compiles the KindOf predicate (and its negation) to an
		 * equivalent Xpath expression fragment. KindOf is translated to a
		 * predicate which compares the element name against all concrete
		 * subtypes of the given type.
		 * 
		 * 
		 * @param ctx
		 *            BindingContext this node shall be compiled in
		 * @return Object containing the Xpath fragment
		 */
		public XpathFragment translate(BindingContext ctx) {

			// Translate the object in the given context
			XpathConstraintNode objnode = children.get(0);
			XpathFragment xptobj = objnode.translate(ctx);
			boolean emptyobject = xptobj.fragment.length() == 0;

			// Obtain the necessary classes from the model
			HashSet<String> classnames = new HashSet<String>();
			if (!exact) {
				SortedSet<String> subtypes = argumentClass.subtypes();
				if (subtypes != null) {
					for (String stid : subtypes) {
						ClassInfo ci = argumentClass.model().classById(stid);
						if (ci.isAbstract())
							continue;
						classnames.add(xpathHelper.getAndRegisterXmlName(ci));
					}
				}
			}
			if (!argumentClass.isAbstract())
				classnames
						.add(xpathHelper.getAndRegisterXmlName(argumentClass));

			// Construct the result expression
			if (classnames.size() == 0) {

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
				for (String name : classnames) {
					if (!first)
						xptobj.fragment += negated ? "and" : " or ";
					first = false;
					xptobj.fragment += "name()" + (negated ? "!=" : "=") + "'"
							+ name + "'";
				}
				xptobj.priority = 3;
				if (classnames.size() > 1)
					xptobj.priority = negated ? 2 : 1;
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
	 * 
	 * This class represents oclAsType(), which is for casting a type to one of
	 * its subtypes.
	 * 
	 */
	public static class Cast extends XpathConstraintNode {

		// The class to be cast to
		protected ClassInfo argumentClass = null;

		// Connection info, required to access the config document
		protected String targetClassName;

		/**
		 * Ctor
		 * 
		 * @param xpathHelper
		 *            The schema object
		 */
		public Cast(XpathHelper xpathHelper) {
			this.xpathHelper = xpathHelper;
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
		 * 
		 * This predicate finds out whether the Cast results in a simple XML
		 * schema type.
		 * 
		 * 
		 * @return Flag indicating whether the node has a simple type
		 */
		public boolean hasSimpleType() {
			return children.get(0).hasSimpleType();
		}

		/**
		 * 
		 * This predicate finds out whether the Cast results in an instance,
		 * which conceptually has identity.
		 * 
		 * 
		 * @return Flag indicating whether the node is an identity carrying type
		 */
		public boolean hasIdentity() {
			return children.get(0).hasIdentity();
		}

		/**
		 * 
		 * This compiles the Cast to an Xpath fragment.
		 * <br><br>
		 * We realize this by making sure the current element is of the
		 * requested type or any of its concrete subtypes.
		 * 
		 * 
		 * @param ctx
		 *            BindingContext this node shall be compiled in
		 * @return Object containing the Xpath fragment
		 */
		public XpathFragment translate(BindingContext ctx) {

			// Translate the object in the given context
			XpathConstraintNode objnode = children.get(0);
			XpathFragment xptobj = objnode.translate(ctx);
			if (xptobj.fragment.length() == 0) {
				xptobj.fragment = "self::*";
				xptobj.priority = 10;
			}

			// Obtain the necessary classes from the model
			SortedSet<String> subtypes = argumentClass.subtypes();
			Set<String> classnames = new HashSet<String>();
			for (String stid : subtypes) {
				ClassInfo ci = argumentClass.model().classById(stid);
				if (ci.isAbstract())
					continue;
				classnames.add(xpathHelper.getAndRegisterXmlName(ci));
			}
			if (!argumentClass.isAbstract())
				classnames
						.add(xpathHelper.getAndRegisterXmlName(argumentClass));

			// Append a predicate which compares against all possible
			// names.
			boolean first = true;
			if (xptobj.priority < 9)
				xptobj.bracket();
			xptobj.fragment += "[";
			for (String name : classnames) {
				if (!first)
					xptobj.fragment += " or ";
				first = false;
				xptobj.fragment += "name()='" + name + "'";
			}
			if (first)
				xptobj.fragment += "false()";
			xptobj.fragment += "]";
			xptobj.priority = 10;

			return xptobj;
		}
	}

	/**
	 * ************************************************************************
	 * This class represents an OCL invocation of the size operation. Size can
	 * be applied to anything with a -&gt; and returns the number of elements of
	 * the object interpreted as a collection. If applied to a String it
	 * determines its length.
	 */
	public static class Size extends XpathConstraintNode {

		/** Flag: This is a set operation */
		boolean setoper = false;

		/**
		 * Ctor
		 * 
		 * @param xpathHelper
		 *            The schema object
		 * @param set
		 *            Flag: This is a set operation
		 */
		public Size(XpathHelper xpathHelper, boolean set) {
			this.xpathHelper = xpathHelper;
			this.setoper = set;
		}

		/**
		 * 
		 * Compile to an equivalent Xpath expression. The Set variant is
		 * compiled to count() and the String variant goes to string-length().
		 * 
		 * 
		 * @param ctx
		 *            BindingContext this node shall be compiled in
		 * @return Object containing the Xpath fragment
		 */
		public XpathFragment translate(BindingContext ctx) {

			// Translate the object
			XpathConstraintNode obj = children.get(0);
			XpathFragment xpt = obj.translate(ctx);

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
	public static class Concatenate extends XpathConstraintNode {

		/**
		 * Ctor
		 * 
		 * @param xpathHelper
		 *            The schema object
		 */
		public Concatenate(XpathHelper xpathHelper) {
			this.xpathHelper = xpathHelper;
		}

		/**
		 * 
		 * This compiles a multivalued Concatenate, which has been built from a
		 * series of OCL concat() functions to Xpath concat().
		 * 
		 * 
		 * @param ctx
		 *            BindingContext this node shall be compiled in
		 * @return Object containing the Xpath fragment
		 */
		public XpathFragment translate(BindingContext ctx) {

			// Loop over the arguments and construct the parameter list
			XpathFragment result = null;
			for (XpathConstraintNode arg : children) {
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
	public static class Substring extends XpathConstraintNode {

		/**
		 * Ctor
		 * 
		 * @param xpathHelper
		 *            The schema object
		 */
		public Substring(XpathHelper xpathHelper) {
			this.xpathHelper = xpathHelper;
		}

		/**
		 * 
		 * This compiles a Substring object to its Xpath equivalent.
		 * 
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
	public static class ChangeCase extends XpathConstraintNode {

		protected String operation = null;

		/**
		 * Ctor
		 * 
		 * @param xpathHelper
		 *            The schema object
		 * @param oper
		 *            the actual operation: toUpper, toLower
		 */
		public ChangeCase(XpathHelper xpathHelper, String oper) {
			this.xpathHelper = xpathHelper;
			this.operation = oper;
		}

		/**
		 * 
		 * ChangeCase operations cannot be translated into Xpath 1.0.
		 * 
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
	public static class Matches extends XpathConstraintNode {

		/**
		 * Ctor
		 * 
		 * @param xpathHelper
		 *            The schema object
		 */
		public Matches(XpathHelper xpathHelper) {
			this.xpathHelper = xpathHelper;
		}

		/**
		 * 
		 * Matches operations are translated to an appropriate extension
		 * function (XPath 1.0) or directly to Xpath 2.0.
		 * 
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
			XpathHelper.ExtensionFunctionTemplate eft = xpathHelper.extensionFunctions
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
			xpathHelper.registerNamespace(eft.nsPrefix, eft.namespace);

			return xptobj;
		}
	}

	/**
	 * ************************************************************************
	 * This class stands for OCL arithmetic.
	 */
	public static class Arithmetic extends XpathConstraintNode {

		/** The operation */
		String operation;

		/**
		 * Ctor
		 * 
		 * @param xpathHelper
		 *            The schema object
		 * @param oper
		 *            The operation symbol, one of + ,-, *, /
		 */
		public Arithmetic(XpathHelper xpathHelper, String oper) {
			this.xpathHelper = xpathHelper;
			this.operation = oper;
		}

		/**
		 * 
		 * This compiles a node to an Xpath expression, which realizes the given
		 * arithmetic operation. OCL and Xpath are very similar here.
		 * 
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
	public static class Variable extends XpathConstraintNode {

		// Wrapped declaration object
		protected OclNode.Declaration vardecl;

		/**
		 * Ctor
		 * 
		 * @param xpathHelper
		 *            The schema object
		 * @param vardecl
		 *            OclNode.Declaration object
		 * @param neg
		 *            Negation flag
		 */
		public Variable(XpathHelper xpathHelper, OclNode.Declaration vardecl,
				boolean neg) {
			this.xpathHelper = xpathHelper;
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
		 * 
		 * This method determines whether this variable is identical to the one
		 * passed as argument.
		 * 
		 * 
		 * @param vardecl
		 *            The Declaration of the variable
		 * @return Flag indicating the inquired dependency
		 */
		public boolean isDependentOn(OclNode.Declaration vardecl) {
			return this.vardecl == vardecl;
		}

		/**
		 * 
		 * This method determines whether this variable is identical to the one
		 * passed as argument.
		 * 
		 * 
		 * @param vardecl
		 *            The Declaration of the variable
		 * @return Flag indicating the inquired property
		 */
		public boolean isVarOrAttribBased(OclNode.Declaration vardecl) {
			return isDependentOn(vardecl);
		}

		/**
		 * 
		 * This inquires the Attribute node this Variable is generated by if
		 * any.
		 * 
		 * 
		 * @return The retrieved Attribute node if there is such a thing
		 */
		public Attribute generatingAttribute() {
			// Find the iterator, which binds this variable
			XpathConstraintNode binds = null;
			for (XpathConstraintNode scn = parent; scn != null; scn = scn.parent) {
				if (scn.bindsVariable(vardecl)) {
					binds = scn;
					break;
				}
			}
			if (binds == null)
				return null; // Should not occur!
			// Ask this iterator's object child
			return binds.children.get(0).generatingAttribute();
		}

		/**
		 * 
		 * This predicate finds out whether the Variable results in a simple XML
		 * schema type.
		 * 
		 * 
		 * @return Flag indicating whether the node has a simple type
		 */
		public boolean hasSimpleType() {
			XpathConstraintNode binds = null;
			for (XpathConstraintNode scn = parent; scn != null; scn = scn.parent) {
				if (scn.bindsVariable(vardecl)) {
					binds = scn;
					break;
				}
			}
			if (binds == null)
				return false;
			return binds.children.get(0).hasSimpleType();
		}

		/**
		 * 
		 * This predicate finds out whether the Variable results in an instance,
		 * which conceptually has identity.
		 * 
		 * 
		 * @return Flag indicating whether the node is an identity carrying type
		 */
		public boolean hasIdentity() {
			XpathConstraintNode binds = null;
			for (XpathConstraintNode scn = parent; scn != null; scn = scn.parent) {
				if (scn.bindsVariable(vardecl)) {
					binds = scn;
					break;
				}
			}
			if (binds == null)
				return true;
			return binds.children.get(0).hasIdentity();
		}

		/**
		 * 
		 * This compiles a node to an Xpath expression, which stands for the
		 * given variable.
		 * <br><br>
		 * The only variable which can be properly translated in all cases is
		 * <i>self</i>, which will be mapped to <i>current()</i> or to '.', if
		 * compiled in a ISCURRENT context. Other variables require to be on the
		 * context stack of the expression, which is widely dependent on how the
		 * expression environment could be represented in Xpath.
		 * 
		 * 
		 * @param ctx
		 *            BindingContext this node shall be compiled in
		 * @return Object containing the Xpath fragment
		 */
		public XpathFragment translate(BindingContext ctx) {

			XpathFragment xpt = new XpathFragment(11, "");
			if (getName().equals("self")) {
				// self
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
	 * 
	 * This class represents a chain of attribute selectors based on some value
	 * source such as a variable, a select() or allInstances. The value source
	 * is the sole child of the Attribute object.
	 * 
	 */
	public static class Attribute extends XpathConstraintNode {

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
		 * @param xpathHelper
		 *            The schema object
		 * @param attr
		 *            The (possibly first) AttributeCallExp object
		 * @param negated
		 *            May be negated if of type boolean
		 */
		public Attribute(XpathHelper xpathHelper, OclNode.AttributeCallExp attr,
				boolean negated) {
			this.xpathHelper = xpathHelper;
			this.attributes = new AttrComp[] { new AttrComp(attr) };
			this.negated = negated;
		}

		/**
		 * Ctor - starting from AttrComp
		 * 
		 * @param xpathHelper
		 *            The schema object
		 * @param atc
		 *            AttrComp object
		 * @param negated
		 *            May be negated if of type boolean
		 */
		public Attribute(XpathHelper xpathHelper, AttrComp atc,
				boolean negated) {
			this.xpathHelper = xpathHelper;
			this.attributes = new AttrComp[] { new AttrComp(atc) };
			this.negated = negated;
		}

		/**
		 * 
		 * Append another AttributeCallExp and associated layout info as an
		 * additional qualification.
		 * 
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
		 * 
		 * Append another AttrComp and associated layout info as an additional
		 * qualification.
		 * 
		 * 
		 * @param atc
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
			Attribute atrite = new Attribute(xpathHelper, attributes[at],
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
				Attribute atleft = new Attribute(xpathHelper, attributes[0],
						negated);
				for (int i = 1; i < at; i++)
					appendAttribute(attributes[i]);
				atleft.addChild(children.get(0));
				atrite.addChild(atleft);
			}
			return atrite;
		}

		/**
		 * 
		 * This Attribute predicate finds out, whether the last attribute
		 * component in the object is implemented as a group and is therefore
		 * absorbing its properties. If there is already a property absorbed on
		 * the attribute, the absorbed property will be asked.
		 * <br><br>
		 * Note that this is a necessary condition for appying GML's nilReason
		 * pattern.
		 * 
		 * 
		 * @return The required flag indicating that properties are absorbed
		 */
		public boolean isPropertyAbsorbing() {

			AttrComp ac = attributes[attributes.length - 1];
			AttributeCallExp acex = ac.absType == 0 ? ac.main : ac.absAttr;
			PropertyInfo pi = (PropertyInfo) acex.selector.modelProperty;
			Type t = pi.typeInfo();
			ClassInfo ci = pi.model().classById(t.id);
			if (ci != null) {
				if (ci.isUnionDirect())
					return true;
			}
			return false;
		}

		/**
		 * 
		 * This method determines whether this Attribute is dependent on the
		 * Variable passed as argument.
		 * 
		 * 
		 * @param vardecl
		 *            The Declaration of the variable
		 * @return Flag indicating the inquired property
		 */
		public boolean isVarOrAttribBased(OclNode.Declaration vardecl) {
			return children.get(0).isVarOrAttribBased(vardecl);
		}

		/**
		 * 
		 * This inquires the Attribute node this Attribute is generated by.
		 * Alas, it's this Attribute!
		 * 
		 * 
		 * @return The retrieved Attribute node
		 */
		public Attribute generatingAttribute() {
			return this;
		}

		/**
		 * 
		 * This method returns true if any of the OclNode.Attribute objects it
		 * is made of has a possible cardinality greater than 1.
		 * 
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
		 * 
		 * This predicate finds out whether the Attribute as a whole results in
		 * a simple XML schema type. Note that for convenience reasons this also
		 * includes the GML's xsi:nil construct.
		 * 
		 * 
		 * @return Flag indicating whether the Attribute has a simple type
		 */
		public boolean hasSimpleType() {
			int last = attributes.length - 1;
			return hasSimpleType(last);
		}

		/**
		 * 
		 * This predicate finds out whether the Attribute component at the given
		 * index <i>idx</i> results in a simple XML schema type. Note that for
		 * convenience reasons this also includes the GML's xsi:nil construct.
		 * 
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
							.indicatorForObjectElementWithSimpleContent(
									ci);
					result = !XmlSchema.classHasObjectElement(ci)
							|| (indicatorSimpleType != null
									&& indicatorSimpleType);
				}
				break;
			case 1: // Normal absorption
				ci = attributes[idx].absAttr.dataType.umlClass;
				if (ci != null) {
					Boolean indicatorSimpleType = XmlSchema
							.indicatorForObjectElementWithSimpleContent(
									ci);
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
		 * 
		 * This predicate finds out whether the Attribute as a whole results in
		 * instances, which conceptually have identity.
		 * 
		 * 
		 * @return Flag indicating whether the node is an identity carrying type
		 */
		public boolean hasIdentity() {
			int last = attributes.length - 1;
			return hasIdentity(last);
		}

		/**
		 * 
		 * This predicate finds out whether the Attribute component at the given
		 * index <i>idx</i> results in a schema type, which carries identity.
		 * Note that for convenience reasons this also includes GML's xsi:nil
		 * construct.
		 * 
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
		 * 
		 * This function translates the Attribute to an Xpath fragment accessing
		 * that attribute. Attributes can be negated, in which case they are
		 * boolean and not multiple.
		 * 
		 * 
		 * @param ctx
		 *            BindingContext this node shall be compiled in
		 * @return XpathFragment representing the Attribute access
		 */
		public XpathFragment translate(BindingContext ctx) {

			// First, obtain the feature attribute expressed by PropertyInfo
			// objects.
			PropertyInfo[] props = new PropertyInfo[attributes.length];
			for (int i = 0; i < props.length; i++)
				props[i] = (PropertyInfo) attributes[i].main.selector.modelProperty;

			// Translate the object. This is a variable (self in most of the
			// cases), or AllInstances() or any type of object producing
			// iterator such as Select().
			XpathFragment obj = children.get(0).translate(ctx);

			// Get the prefixes that possibly surround the identifier contained
			// in xlink:href references
			String alpha = xpathHelper.alpha;
			String beta = xpathHelper.beta;

			boolean alphaEx = alpha != null && alpha.length() > 0;
			boolean betaEx = beta != null && beta.length() > 0;

			// Now step along the properties and generate the associated Xpath
			// code for it.
			for (int idx = 0; idx < props.length; idx++) {

				// The property
				PropertyInfo pi = props[idx];

				// Find out how the relation to the contained object is realized
				// in XML Schema:
				// 0 = simple, 1 = in-line embedded, 2 = per xlink:href, 3 = 1|2
				int conCode = 0;
				String cid = pi.typeInfo().id;
				ClassInfo ci = null;
				if (cid != null)
					ci = pi.model().classById(cid);
				if (!hasSimpleType(idx)) {
					// Not a simple type, assume in-line
					conCode = 1;
					if (ci != null && XmlSchema.classCanBeReferenced(ci)) {
						String ref = pi.inlineOrByReference();
						if (ref == null)
							ref = "";
						if (ref.equals("byreference"))
							conCode = 2;
						else if (!ref.equals("inline"))
							conCode = 3;
					}
				}

				// Namespace and namespace adorned property
				String proper = xpathHelper.getAndRegisterXmlName(pi);

				/*
				 * We will have to know whether we are subject to the 19139,
				 * regime, so go and find out
				 */
				boolean is19139 = pi.matches("rule-xsd-all-naming-19139")
						|| (ci != null && ci.matches(
								"rule-xsd-cls-standard-19139-property-types"));
				
				// Dispatch on containment cases
				if (conCode == 0) {

					// 0: Simple type is contained. This also comprises
					// access to property 'reason' in GML's nilReason
					// treatment.

					if (obj.fragment.length() > 0) {
						obj.fragment += "/";
						obj.priority = 9;
					}
					obj.fragment += proper;

					// 'reason' access in GML's nilReason treatment?

					if (attributes[idx].absType == 2) {
						obj.fragment += "[@xsi:nil='true']/@nilReason";
						obj.priority = 9;
					}

					// Adjust relative adressing of variables
					if (obj.atEnd != null)
						obj.atEnd.addStep();

				} else {

					// other: different modes of containment are possible
					String frag_inl = null;
					String frag_ref = null;
					boolean isVar = false;

					if (conCode == 3) {

						// If both containments are to be realized, we will
						// create a 'let' variable unless we are in the middle
						// of some relative addressing scheme
						if (obj.atEnd.state == BindingContext.CtxState.OTHER
								&& obj.fragment.length() > 0
								&& !obj.fragment.startsWith(".")) {
							String var = obj.findOrAdd(obj.fragment);
							obj.fragment = "$" + var;
							isVar = true;
						}
					}

					if (conCode == 1 || conCode == 3) {

						// In-line containment must be treated
						// --> .../attr/*
						frag_inl = obj.fragment;
						if (frag_inl.length() > 0)
							frag_inl += "/";
						frag_inl += proper;
						if (obj.atEnd != null)
							obj.atEnd.addStep();
						frag_inl += "/*";
						if (obj.atEnd != null)
							obj.atEnd.addStep();
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
							xpathHelper.registerNamespace("gml");
						}
						
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
							frag_ref += idAttributeFrag;
							if (betaEx)
								frag_ref += ",'" + beta + "'";
							frag_ref += ")";
						} else {
							frag_ref += idAttributeFrag;
						}
						frag_ref += "=" + attxlink + "]";
						xpathHelper.registerNamespace("xlink");

						// Whatever binding context we had before, it is lost
						if (obj.atEnd != null)
							obj.atEnd.setState(BindingContext.CtxState.OTHER);
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
	}

	/**
	 * ************************************************************************
	 * This wraps any form of Literal value from the OclNode.
	 */
	public static class Literal extends XpathConstraintNode {

		OclNode.LiteralExp literal;

		/**
		 * Ctor
		 * 
		 * @param xpathHelper
		 *            The schema object
		 * @param lit
		 *            OclNode.LiteralExp object
		 * @param neg
		 *            Negation flag
		 */
		public Literal(XpathHelper xpathHelper, OclNode.LiteralExp lit,
				boolean neg) {
			this.xpathHelper = xpathHelper;
			literal = lit;
			negated = neg;
		}

		/**
		 * 
		 * This function translates the Literal to equivalent Xpath code.
		 * 
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

				// Enums to be treated like strings, except Boolean class
				// ones, which are treated as Boolean constants
				OclNode.EnumerationLiteralExp litex = (OclNode.EnumerationLiteralExp) literal;
				ClassInfo ci = litex.dataType.umlClass;
				if (ci != null && ci.name().equals("Boolean")) {
					boolean val = value.equalsIgnoreCase("TRUE");
					if (negated)
						val = !val;
					value = val ? "true()" : "false()";
					type = XpathType.BOOLEAN;
				} else {
					if (litex.umlProperty != null) {
						// Check whether there is a resourceURI tag on the
						// property
						String resuri = litex.umlProperty
								.taggedValue("resourceURI");
						// If there is, this is going to be it
						if (StringUtils.isNotBlank(resuri))
							value = resuri;
					}
					// Make literal value
					value = "'" + value + "'";
				}

			} else if (literal instanceof OclNode.BooleanLiteralExp) {
				// Booleans may need consideration of negation

				OclNode.BooleanLiteralExp lit = (OclNode.BooleanLiteralExp) literal;
				boolean val = lit.value;
				if (negated)
					val = !val;
				value = val ? "true()" : "false()";
				type = XpathType.BOOLEAN;

			} else if (literal instanceof OclNode.IntegerLiteralExp
					|| literal instanceof OclNode.RealLiteralExp) {

				// The value is already o.k., but we need to set the type
				type = XpathType.NUMBER;

			} else if (literal instanceof OclNode.DateTimeLiteralExp) {

				OclNode.DateTimeLiteralExp lt = (OclNode.DateTimeLiteralExp) literal;
				if (lt.current)
					// This references the current date and cannot be
					// represented in Xpath ...
					value = "***ERROR[125]***";
				else {
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
	public static class IfThenElse extends XpathConstraintNode {

		/**
		 * Ctor
		 * 
		 * @param xpathHelper
		 *            The schema object
		 */
		public IfThenElse(XpathHelper xpathHelper) {
			this.xpathHelper = xpathHelper;
		}

		/**
		 * 
		 * This predicate finds out whether the IfThenElse results in a simple
		 * XML schema type.
		 * 
		 * 
		 * @return Flag indicating whether the node has a simple type
		 */
		public boolean hasSimpleType() {
			return children.get(1).hasSimpleType()
					&& children.get(2).hasSimpleType();
		}

		/**
		 * 
		 * This compiles the construct to an equivalent Xpath expression.
		 * 
		 * 
		 * @param ctx
		 *            BindingContext this node shall be compiled in
		 * @return Object containing the Xpath fragment
		 */
		public XpathFragment translate(BindingContext ctx) {

			// The 3 arguments
			XpathConstraintNode con = children.get(0);
			XpathConstraintNode thn = children.get(1);
			XpathConstraintNode els = children.get(2);

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

				// Construct the result expression. The trick is to concatenate
				// substrings which either comprise the full argument or
				// nothing,
				// depending on the value of the predicate.
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
	 * This is generated for unimplemented material.
	 */
	public static class Error extends XpathConstraintNode {

		/**
		 * Ctor
		 * 
		 * @param xpathHelper
		 *            The schema object
		 */
		public Error(XpathHelper xpathHelper) {
			this.xpathHelper = xpathHelper;
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
	public static class MessageComment extends XpathConstraintNode {

		protected String name = null;

		public MessageComment(XpathHelper xpathHelper, String name) {
			this.xpathHelper = xpathHelper;
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
		 * 
		 * This method returns a vector or Schematron SQL value expressions in
		 * interpretation of a MessageComment object. The latter is created from
		 * the message text comment syntax contained in the constraints.
		 * 
		 * 
		 * @return Array of message arguments in FME value syntax
		 */
		public String[] compileAsMessageArgumentList() {

			String[] arglist = new String[children.size()];
			int i = 0;
			for (XpathConstraintNode arg : children) {
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
