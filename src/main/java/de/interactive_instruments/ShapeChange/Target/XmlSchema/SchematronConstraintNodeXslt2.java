/**
 * ShapeChange - processing application schemas for geographic information
 *
 * <p>This file is part of ShapeChange. ShapeChange takes a ISO 19109 Application Schema from a UML
 * model and translates it into a GML Application Schema or other implementation representations.
 *
 * <p>Additional information about the software can be found at http://shapechange.net/
 *
 * <p>(c) 2002-2012 interactive instruments GmbH, Bonn, Germany
 *
 * <p>This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * <p>You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 *
 * <p>Contact: interactive instruments GmbH Trierer Strasse 70-72 53115 Bonn Germany
 */
package de.interactive_instruments.ShapeChange.Target.XmlSchema;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import de.interactive_instruments.ShapeChange.MapEntry;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Ocl.OclNode;
import de.interactive_instruments.ShapeChange.Ocl.OclNode.AttributeCallExp;
import de.interactive_instruments.ShapeChange.Ocl.OclNode.MultiplicityMapping;

/**
 * SchematronConstraintNodeXslt2 and its concrete derivations stand for a
 * representation of OCL contents, which are close to the capabilities of
 * Schematron and the logic that can be realized within Schematron rules, based
 * on the xslt2 query binding.
 *
 * <p>
 * There are two basic patterns of use of these classes:
 *
 * <ul>
 * <li>Creation of SchematronConstraintNodeXslt2 objects while interpreting the
 * original OclConstraint objects.
 * <li>Translating Rules and Assert code fragments by calling the abstract
 * method <i>translate</i>.
 * </ul>
 * 
 * @author Reinhard Erstling
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments <dot>
 *         de)
 */
public abstract class SchematronConstraintNodeXslt2 {

    /** The children of the SchematronConstraintNodeXslt2 */
    protected ArrayList<SchematronConstraintNodeXslt2> children = new ArrayList<SchematronConstraintNodeXslt2>();

    /** General negation flag for all nodes */
    protected boolean negated = false;

    /** The parent reference */
    protected SchematronConstraintNodeXslt2 parent = null;

    /** Link back to SchematronSchema object */
    protected AbstractSchematronSchema schemaObject = null;

    /**
     * Method to add children to a node and at the same time establish the node as
     * parent of the child to be added.
     *
     * @param child The Child node to be added
     */
    public void addChild(SchematronConstraintNodeXslt2 child) {
	children.add(child);
	child.parent = this;
    }

    /**
     * Method to inquire whether the the node inquired is a Logic node AND this
     * logic node has the same <i>isAnd</i> polarity as specified in the parameter.
     *
     * <p>
     *
     * <p>
     * This implementation installs the default for all derivations except Logic.
     *
     * @param isAnd Flag: Are we an AND? (not an OR)?
     * @return True if this is Logic with the same polarity
     */
    public boolean isAndOrLogic(boolean isAnd) {
	return false;
    }

    /**
     * This method determines whether the given expression depends on the Variable
     * passed as argument.
     *
     * <p>
     * This implementation defines the default behavior: Descend down and try to
     * find the variable somewhere.
     *
     * @param vardecl The Declaration of the variable
     * @return Flag indicating the inquired dependency
     */
    public boolean isDependentOn(OclNode.Declaration vardecl) {
	for (SchematronConstraintNodeXslt2 scn : children)
	    if (scn.isDependentOn(vardecl))
		return true;
	return false;
    }

    /**
     * This method determines whether the node binds the given variable declaration
     * (this can only happen for iterators) and if it does, which is the expression
     * the variable is bound to.
     *
     * @param vardecl The variable Declaration object
     * @return Node the variable is bound to or null
     */
    public SchematronConstraintNodeXslt2 nodeVariableIsBoundTo(OclNode.Declaration vardecl) {
	return null;
    }

    /**
     * This method determines whether the given expression is a Variable or an
     * Attribute based on a Variable, which is identical to the one passed as
     * argument.
     *
     * <p>
     * This implementation defines the default behavior.
     *
     * @param vardecl The Declaration of the variable
     * @return Flag indicating the inquired property
     */
    public boolean isVarOrAttribBased(OclNode.Declaration vardecl) {
	return false;
    }

    /**
     * By means of this function you can inquire which Attribute node is generating
     * the objects represented by this node. Note that invocation is only sensible
     * for iterators and attributes.
     *
     * @return The retrieved Attribute node if there is such a thing
     */
    public Attribute generatingAttribute() {
	return null;
    }

    /**
     * This predicate finds out whether the given node may produce a set.
     *
     * <p>
     * This is the default implementation providing the value false.
     *
     * @return Flag indicating whether the node can return multiple values
     */
    public boolean isMultiple() {
	return false;
    }

    /**
     * This predicate finds out whether the given node is realized by means of a
     * simple XML schema type.
     *
     * @return Flag indicating whether the node has a simple type
     */
    public boolean hasSimpleType() {
	return true;
    }

    /**
     * This predicate finds out whether the given node is realized by means of a
     * class, which conceptually has identity.
     *
     * @return Flag indicating whether the node is an identity carrying type
     */
    public boolean hasIdentity() {
	return false;
    }

    /**
     * Determine if the given class is instantiable, i.e. an XML element can be used
     * to encode an object of that class.
     *
     * @param ci
     * @return <code>false</code> if the class is abstract or suppressed (includes
     *         checking that rule-xsd-cls-suppress applies to the class), otherwise
     *         <code>true</code>
     */
    private static boolean isInstantiable(ClassInfo ci) {

	if (ci.isAbstract() || (ci.suppressed() && ci.matches("rule-xsd-cls-suppress"))) {
	    return false;
	} else {
	    return true;
	}
    }

    /**
     * @param ci
     * @return All direct and indirect subtypes of ci that are instantiable, i.e.
     *         not abstract or suppressed (includes checking that
     *         rule-xsd-cls-suppress applies to the class). Can be empty but not
     *         <code>null</code>.
     */
    private static SortedSet<ClassInfo> getInstantiableSubtypesInCompleteHierarchy(ClassInfo ci) {

	SortedSet<ClassInfo> instantiableClasses = new TreeSet<>();

	for (ClassInfo st : ci.subtypesInCompleteHierarchy()) {

	    if (isInstantiable(st)) {
		instantiableClasses.add(st);
	    }
	}

	return instantiableClasses;
    }

    /**
     * An expression - including its XPath priority - which can be used to select
     * elements based upon the combination of local-names and namespace-uris of the
     * given classes. We differentiate three cases:
     *
     * <p>
     *
     * <ul>
     * <li>Case multiple classes: (local-name()='localName_ci_1' and
     * namespace-uri()='namespace_ci_1') or ... or (local-name()='localName_ci_n'
     * and namespace-uri()='namespace_ci_n')
     * <li>Case single class: local-name()='localName_ci' and
     * namespace-uri()='namespace_ci'
     * <li>Case no class: false()
     * </ul>
     *
     * @param cis
     * @return a pair, with left=expression and right=priority
     */
    private static Pair<String, Integer> getExpressionForTypeRestriction(SortedSet<ClassInfo> cis) {

	String fragment = "";
	Integer priority = -1;

	/*
	 * If we have more than one component, surround each part we add with
	 * parentheses.
	 */
	boolean surroundWithBrackets = cis.size() > 1;

	boolean first = true;

	for (ClassInfo ci : cis) {

	    if (!first) {
		fragment += " or ";
	    }

	    first = false;

	    String namespace = ci.pkg().targetNamespace();
	    String localName = ci.name();

	    MapEntry me = ci.options().elementMapEntry(ci.name(), ci.encodingRule("xsd"));

	    if (me != null && StringUtils.isNotBlank(me.p1)) {

		String xmlElement = me.p1;
		String[] parts = xmlElement.split(":");

		if (parts.length > 1) {
		    String nspref = parts[0];
		    String ns = ci.options().fullNamespace(nspref);
		    if (ns != null) {
			namespace = ns;
			localName = parts[1];
		    }
		}
	    }

	    if (surroundWithBrackets) {
		fragment += "(";
	    }

	    /*
	     * 2018-02-06 JE: comparison based on QName as literal value (like 'ex:ClassX')
	     * is dangerous, because it depends on a fixed namespace prefix. However, the
	     * prefix of a namespace can vary. Therefore, I changed the logic to use
	     * local-name() and namespace-uri().
	     */
	    fragment += "local-name()='" + localName + "' and namespace-uri()='" + namespace + "'";

	    if (surroundWithBrackets) {
		fragment += ")";
	    }
	}

	// for case in which no relevant class was found
	// 2019-09-10 JE: TBD - better to report an error here?
	if (cis.isEmpty()) {
	    fragment += "false()";
	    priority = 20;
	} else if (cis.size() == 1) {
	    // expression consisting of a single 'and'
	    priority = 5;
	} else {
	    // expression concatenated by one or more 'or'
	    priority = 4;
	}

	return new ImmutablePair<String, Integer>(fragment, priority);
    }

    /**
     * Find out whether this construct contains a node of type
     * SchematronConstraintNodeXslt2.Error. In this case the whole tree is in error.
     *
     * @return Error flag
     */
    public boolean containsError() {
	if (this instanceof Error)
	    return true;
	for (SchematronConstraintNodeXslt2 node : children)
	    if (node.containsError())
		return true;
	return false;
    }

    /**
     * This abstract method compiles a node to an XPath expression fragment.
     *
     * @param ctx BindingContext this node shall be compiled in
     * @return Object containing the Xpath fragment
     */
    public abstract XpathFragment translate(BindingContext ctx);

    /**
     * This class stands for logical operations AND, OR, XOR and EQV. Which of these
     * is is coded in the state member logic.
     */
    public static class Logic extends SchematronConstraintNodeXslt2 {

	protected enum LogicType {
	    AND, OR, XOR, EQV
	}

	protected LogicType logic;

	/**
	 * Ctor
	 *
	 * @param schemaObject The schema object
	 * @param isAnd        Flag to make this an AND (true) or an OR (false)
	 */
	public Logic(AbstractSchematronSchema schemaObject, LogicType logic) {
	    this.schemaObject = schemaObject;
	    this.logic = logic;
	}

	/**
	 * Method to inquire whether the node inquired is a Logic node and this logic
	 * node has the same <i>isAnd</i> polarity as specified in the parameter. XORs
	 * and EQVs are ignored and yield false.
	 *
	 * <p>
	 *
	 * @param isAnd Flag: Are we an AND? (not an OR)?
	 * @return True if this is Logic with the same AND/OR polarity
	 */
	public boolean isAndOrLogic(boolean isAnd) {

	    if (logic == LogicType.XOR || logic == LogicType.EQV)
		return false;
	    return (this.logic == LogicType.AND) == isAnd;
	}

	/**
	 * This compiles the node and its children to an Xpath predicate, which can be
	 * inserted into a &lt;rule>.
	 *
	 * <p>
	 * AND and OR are translated into their Xpath counterparts <i>and</i> and
	 * <i>or</i>. XOR will be realized as a != operator, EQV by an = operator.
	 *
	 * @param ctx BindingContext this node shall be compiled in
	 * @return Object containing the Xpath fragment and its operator priority
	 */
	public XpathFragment translate(BindingContext ctx) {

	    // Just one child? Pass through ...
	    if (children.size() == 1)
		return children.get(0).translate(ctx);

	    // Which logic particle?
	    String particle = null;
	    int refprio;
	    if (logic == LogicType.AND) {
		particle = "and";
		refprio = 5;
	    } else if (logic == LogicType.OR) {
		particle = "or";
		refprio = 4;
	    } else if (logic == LogicType.XOR) {
		particle = "!=";
		refprio = 6;
	    } else { // EQV
		particle = "=";
		refprio = 6;
	    }

	    // In turn compile the node's children and connect them with logic
	    // particles. Bracket the subexpression if necessary. When
	    // processing a XOR/EQV (which is implemented as !=/= of booleans)
	    // do the necessary conversion to boolean first.
	    XpathFragment result = null;

	    for (SchematronConstraintNodeXslt2 ocn : children) {

		// Bracket or do the necessary boolean conversion
		XpathFragment child_xpt = ocn.translate(ctx);

		if ((logic == LogicType.XOR || logic == LogicType.EQV) && child_xpt.type != XpathType.BOOLEAN) {

		    child_xpt.fragment = "boolean(" + child_xpt.fragment + ")";
		    child_xpt.type = XpathType.BOOLEAN;
		    child_xpt.priority = 20;

		} else if (child_xpt.priority < refprio) {

		    child_xpt.bracket();
		}

		// Apply operation and merge
		child_xpt.atEnd.setState(BindingContext.CtxState.NONE);
		if (result == null) {
		    result = child_xpt;
		} else {
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
     * This class stands for comparisons. The operator is given as a String, which
     * can take the values: =, <>, <, <=, >, >=.
     */
    public static class Comparison extends SchematronConstraintNodeXslt2 {

	// The relational operator
	String opname;

	/**
	 * Ctor
	 *
	 * @param schemaObject The schema object
	 * @param name         One of =, <>, <, <=, >, >=
	 */
	public Comparison(AbstractSchematronSchema schemaObject, String name) {
	    this.schemaObject = schemaObject;
	    opname = name.equals("<>") ? "!=" : name;
	}

	/**
	 * This compiles the node and its children to Xpath. Xpath can express all
	 * required comparison operators.
	 *
	 * @param ctx BindingContext this node shall be compiled in
	 * @return Object containing the XPath fragment and its operator priority
	 */
	public XpathFragment translate(BindingContext ctx) {

	    // Operator priority
	    int refprio = 6;

	    // Check and compile children
	    XpathFragment[] child_xpt = new XpathFragment[2];

	    for (int i = 0; i < 2; i++) {

		SchematronConstraintNodeXslt2 child = children.get(i);

		if (!child.hasSimpleType() && !child.hasIdentity()) {
		    return new XpathFragment(20, "***ERROR[126]***");
		}

		child_xpt[i] = child.translate(ctx);
		if (child.hasIdentity()) {
		    child_xpt[i].fragment = "generate-id(" + child_xpt[i].fragment + ")";
		    child_xpt[i].priority = 20;
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
     * This one stands for the OCL <i>isEmpty()</i> and <i>notEmpty()</i> predicate
     * operations. Which of these is meant is expressed in the state variable
     * <i>negated</i>.
     */
    public static class Empty extends SchematronConstraintNodeXslt2 {

	// negated means: isEmpty (false) or notEmpty (true)

	/**
	 * Ctor
	 *
	 * @param schemaObject The schema object
	 * @param neg          Flag: isEmpty (false) and notEmpty (true)
	 */
	public Empty(AbstractSchematronSchema schemaObject, boolean neg) {
	    this.schemaObject = schemaObject;
	    negated = neg;
	}

	/**
	 * This compiles the node and its children to an Xpath fragment. The translation
	 * is essentially the nodeset derived from the object part of the expression,
	 * because notEmpty() is fulfilled for a nodeset, which converts to a boolean
	 * true. isEmpty() requires an additional not().
	 *
	 * @param ctx BindingContext this node shall be compiled in
	 * @return Object containing the fragment and its operator priority
	 */
	public XpathFragment translate(BindingContext ctx) {

	    // Fetch and compile the object
	    SchematronConstraintNodeXslt2 obj = children.get(0);
	    XpathFragment xpt = obj.translate(ctx);
	    if (xpt.fragment.length() == 0) {
		xpt.fragment = ".";
	    }

	    // isEmpty() requires an additional not(...)
	    if (!negated) {
		xpt.fragment = "not(" + xpt.fragment + ")";
		xpt.type = XpathType.BOOLEAN;
		xpt.priority = 20;
		xpt.atEnd.setState(BindingContext.CtxState.NONE);
	    }

	    return xpt;
	}
    }

    /**
     * This class represents the Exists iterator predicate. Being part of the Logic
     * system Exists can be negated, which is realized by applying an additional
     * not().
     */
    public static class Exists extends SchematronConstraintNodeXslt2 {

	// The variable declaration attached to the iterator
	OclNode.Declaration vardecl;

	/**
	 * Ctor
	 *
	 * @param schemaObject The schema object
	 * @param vardecl      OclNode.Declaration object
	 * @param neg          Negation flag
	 */
	public Exists(AbstractSchematronSchema schemaObject, OclNode.Declaration vardecl, boolean neg) {
	    this.schemaObject = schemaObject;
	    this.negated = neg;
	    this.vardecl = vardecl;
	}

	/**
	 * This method determines whether the Exists binds the given variable
	 * declaration and if it does, which is the expression the variable is bound to.
	 *
	 * @param vardecl The variable Declaration object
	 * @return Node the variable is bound to or null
	 */
	public SchematronConstraintNodeXslt2 nodeVariableIsBoundTo(OclNode.Declaration vardecl) {
	    if (this.vardecl == vardecl)
		return children.get(0);
	    return null;
	}

	/**
	 * This compiles the node and its children to an Xpath expression fragment.
	 *
	 * @param ctx BindingContext this node shall be compiled in
	 * @return Object containing the fragment and its operator priority
	 */
	public XpathFragment translate(BindingContext ctx) {

	    // Fetch and compile the object
	    SchematronConstraintNodeXslt2 selection = children.get(0);
	    XpathFragment xpt = selection.translate(ctx);

	    // Prepare the binding context for compilation of the iterator
	    // body. This is primarily the context at the end of the object
	    // plus the variable.
	    BindingContext bodyctx = xpt.atEnd.clone();
	    bodyctx.pushDeclaration(vardecl);
	    bodyctx.inPredicateExpression = false;

	    // Compile the boolean expression in the iterator body
	    SchematronConstraintNodeXslt2 condition = children.get(1);
	    XpathFragment cond = condition.translate(bodyctx);
	    cond.atEnd = null; // This suppresses the merging of ending contexts

	    String filter = xpt.merge(cond);
	    if (xpt.priority < 3) {
		xpt.bracket();
	    }
	    xpt.fragment = "some $" + vardecl.name + " in " + xpt.fragment + " satisfies " + filter;
	    xpt.priority = 3;

	    // Consider negated form
	    if (negated) {
		xpt.fragment = "not(" + xpt.fragment + ")";
		xpt.type = XpathType.BOOLEAN;
		xpt.priority = 20;
		xpt.atEnd.setState(BindingContext.CtxState.NONE);
	    }

	    return xpt;
	}
    }

    /** This class represents the ForAll iterator predicate. */
    public static class ForAll extends SchematronConstraintNodeXslt2 {

	// The variable declaration attached to the iterator
	OclNode.Declaration vardecl;

	/**
	 * Ctor
	 *
	 * @param schemaObject The schema object
	 * @param vardecl      OclNode.Declaration object
	 * @param neg          Negation flag
	 */
	public ForAll(AbstractSchematronSchema schemaObject, OclNode.Declaration vardecl, boolean neg) {
	    this.schemaObject = schemaObject;
	    this.negated = neg;
	    this.vardecl = vardecl;
	}

	/**
	 * This method determines whether the ForAll binds the given variable
	 * declaration and if it does, which is the expression the variable is bound to.
	 *
	 * @param vardecl The variable Declaration object
	 * @return Node the variable is bound to or null
	 */
	public SchematronConstraintNodeXslt2 nodeVariableIsBoundTo(OclNode.Declaration vardecl) {
	    if (this.vardecl == vardecl)
		return children.get(0);
	    return null;
	}

	/**
	 * This compiles the node and its children to an Xpath expression fragment.
	 *
	 * @param ctx BindingContext this node shall be compiled in
	 * @return Object containing the fragment and its operator priority
	 */
	public XpathFragment translate(BindingContext ctx) {

	    // Fetch and compile the object
	    SchematronConstraintNodeXslt2 selection = children.get(0);
	    XpathFragment xpt = selection.translate(ctx);

	    // Prepare the binding context for compilation of the iterator
	    // body. This is primarily the context at the end of the object
	    // plus the variable.
	    BindingContext bodyctx = xpt.atEnd.clone();
	    bodyctx.pushDeclaration(vardecl);
	    bodyctx.inPredicateExpression = false;

	    // Compile the boolean expression in the iterator body
	    SchematronConstraintNodeXslt2 condition = children.get(1);
	    XpathFragment cond = condition.translate(bodyctx);
	    cond.atEnd = null; // This suppresses the merging of ending contexts

	    String filter = xpt.merge(cond);
	    if (xpt.priority < 3) {
		xpt.bracket();
	    }
	    xpt.fragment = "every $" + vardecl.name + " in " + xpt.fragment + " satisfies " + filter;
	    xpt.priority = 3;

	    // Consider negated form
	    if (negated) {
		xpt.fragment = "not(" + xpt.fragment + ")";
		xpt.type = XpathType.BOOLEAN;
		xpt.priority = 20;
		xpt.atEnd.setState(BindingContext.CtxState.NONE);
	    }

	    return xpt;
	}
    }

    /**
     * This class represents the isUnique iterator predicate. Being part of the
     * Logic system Unique can be negated, which is realized by applying an
     * additional not().
     */
    public static class Unique extends SchematronConstraintNodeXslt2 {

	// The variable declaration attached to the iterator
	OclNode.Declaration vardecl;

	/**
	 * Ctor
	 *
	 * @param schemaObject The schema object
	 * @param vardecl      OclNode.Declaration object
	 * @param neg          Negation flag
	 */
	public Unique(AbstractSchematronSchema schemaObject, OclNode.Declaration vardecl, boolean neg) {
	    this.schemaObject = schemaObject;
	    this.negated = neg;
	    this.vardecl = vardecl;
	}

	/**
	 * This method determines whether the Unique binds the given variable
	 * declaration and if it does, which is the expression the variable is bound to.
	 *
	 * @param vardecl The variable Declaration object
	 * @return Node the variable is bound to or null
	 */
	public SchematronConstraintNodeXslt2 nodeVariableIsBoundTo(OclNode.Declaration vardecl) {
	    if (this.vardecl == vardecl)
		return children.get(0);
	    return null;
	}

	/**
	 * This compiles the node and its children to an Xpath expression fragment.
	 *
	 * <p>
	 * The object is translated in the given context. If negated an additional not()
	 * is applied.
	 *
	 * @param ctx BindingContext this node shall be compiled in
	 * @return Object containing the fragment and its operator priority
	 */
	public XpathFragment translate(BindingContext ctx) {

	    // Local negation variable allows to absorb negation en route
	    boolean neg = negated;

	    // Fetch and compile the object
	    SchematronConstraintNodeXslt2 obj = children.get(0);
	    XpathFragment xpt = obj.translate(ctx);

	    // Get hold of the expression argument and take cases on its
	    // nature ...
	    SchematronConstraintNodeXslt2 expr = children.get(1);
	    if (!expr.isDependentOn(vardecl)) {

		// A constant expression can be unique only if the object
		// is of cardinality zero or one.
		xpt.fragment = "count(" + xpt.fragment + ") " + (neg ? ">" : "<=") + " 1";
		neg = false;
		xpt.priority = 6;
		xpt.type = XpathType.BOOLEAN;
		xpt.atEnd.setState(BindingContext.CtxState.NONE);

	    } else if (expr.isVarOrAttribBased(vardecl) && expr instanceof Variable) {

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
		    ClassInfo ci = obat.attributes[obat.attributes.length - 1].main.dataType.umlClass;
		    if (ci != null) {
			Boolean indicatorSimpleType = XmlSchema.indicatorForObjectElementWithSimpleContent(ci);
			simple = !XmlSchema.classHasObjectElement(ci)
				|| (indicatorSimpleType != null && indicatorSimpleType);
		    } else {
			String tname = obat.attributes[obat.attributes.length - 1].main.dataType.name;
			String er = schemaObject.currentOclConstraintClass.encodingRule("xsd");
			MapEntry me = schemaObject.options.typeMapEntry(tname, er);
			if (me != null)
			    simple = me.p2.equalsIgnoreCase("simple/simple")
				    || me.p2.equalsIgnoreCase("complex/simple");
		    }
		}
		if (simple) {
		    /*
		     * The object is expressed by means of a type with simple content. The nodeset
		     * will need to undergo a pairwise value comparison. Note that 19139 treatment
		     * has already been done on the object compiled to xpt.fragment.
		     */
		    String var = null;
		    if (xpt.fragment.matches("^\\$[A-Z]+$"))
			var = xpt.fragment;
		    else
			var = "$" + xpt.findOrAdd(xpt.fragment);
		    xpt.fragment = var + "[. = (preceding::*|ancestor::*)[count(.|" + var + ")=count(" + var + ")]]";
		    xpt.priority = 19;
		    neg = !neg;
		} else {
		    // The object is element valued. Elements in a nodeset are
		    // are always distinct, so uniqueness is trivially true.
		    xpt.fragment = neg ? "false()" : "true()";
		    neg = false;
		    xpt.priority = 20;
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
			    SchematronConstraintNodeXslt2 sv = exat.children.get(0);
			    exat.children.set(0, children.get(0));
			    XpathFragment xpta = exat.translate(ctx);
			    xpt.merge(xpta);
			    xpt.fragment = "count(" + xpt.fragment + ") " + (neg ? "!=" : "=") + " count("
				    + xpta.fragment + ")";
			    neg = false;
			    xpt.priority = 6;
			    xpt.type = XpathType.BOOLEAN;
			    xpt.atEnd.setState(BindingContext.CtxState.NONE);
			    exat.children.set(0, sv);
			} else {
			    /*
			     * Class is expressed by means of a simple type. This also includes 'reason'
			     * access in GML's nilReason treatment.
			     */
			    String var = null;
			    if (xpt.fragment.matches("^\\$[A-Z]+$"))
				var = xpt.fragment;
			    else
				var = "$" + xpt.findOrAdd(xpt.fragment);
			    PropertyInfo pi = (PropertyInfo) exat.attributes[0].main.selector.modelProperty;
			    String prop = schemaObject.getAndRegisterXmlName(pi);
			    // Find out about modified circumstances concerning
			    // value access ...
			    String cid = pi.typeInfo().id;
			    ClassInfo ci = null;
			    if (cid != null)
				ci = pi.model().classById(cid);
			    boolean iscodelist = ci != null && ci.category() == Options.CODELIST;
			    boolean is19139 = ci != null && ci.matches("rule-xsd-all-naming-19139");
			    // Treat special cases in property access
			    if (exat.attributes[0].absType == 0) {
				if (is19139) {
				    prop += "/*";
				    if (iscodelist)
					prop += "/@codeListValue";
				} else if (iscodelist && pi.inClass().matches("rule-xsd-cls-codelist-asDictionaryGml33")
					&& (ci.asDictionary() || ci.asDictionaryGml33()) && !is19139) {
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
			    xpt.fragment = var + "[" + prop + nil + " = (preceding::*|ancestor::*)[count(.|" + var
				    + ")=count(" + var + ")]/" + prop + nil + "]";
			    xpt.priority = 19;
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
			if (xpt1.priority < 5)
			    xpt1.bracket();
			if (xpt2.priority < 5)
			    frag2 = "(" + frag2 + ")";
			xpt1.fragment += " and " + frag2;
			xpt1.priority = 5;
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
		    return new XpathFragment(20, "***ERROR[121]***");
		}
	    } else {

		// Expression other than constant, identity of attribute
		// access. We cannot express this in Xpath syntax
		return new XpathFragment(20, "***ERROR[122]***");
	    }

	    // Consider negated form
	    if (neg) {
		xpt.fragment = "not(" + xpt.fragment + ")";
		xpt.type = XpathType.BOOLEAN;
		xpt.priority = 20;
		xpt.atEnd.setState(BindingContext.CtxState.NONE);
	    }

	    return xpt;
	}
    }

    /** This class represents the Select iterator filter. */
    public static class Select extends SchematronConstraintNodeXslt2 {

	// The variable declaration attached to the iterator
	protected OclNode.Declaration vardecl;

	// Stored generator body
	protected SchematronConstraintNodeXslt2 generatorBody = null;

	/**
	 * Ctor
	 *
	 * @param schemaObject The schema object
	 * @param vardecl      OclNode.Declaration object
	 */
	public Select(AbstractSchematronSchema schemaObject, OclNode.Declaration vardecl) {
	    this.schemaObject = schemaObject;
	    this.vardecl = vardecl;
	}

	/**
	 * This method determines whether the Select binds the given variable
	 * declaration and if it does, which is the expression the variable is bound to.
	 *
	 * @param vardecl The variable Declaration object
	 * @return Node the variable is bound to or null
	 */
	public SchematronConstraintNodeXslt2 nodeVariableIsBoundTo(OclNode.Declaration vardecl) {
	    if (this.vardecl == vardecl)
		return children.get(0);
	    return null;
	}

	/**
	 * By means of this function you can inquire which Attribute node is generating
	 * the objects of this Select node if any.
	 *
	 * @return The retrieved Attribute node if there is such a thing
	 */
	public Attribute generatingAttribute() {
	    return children.get(0).generatingAttribute();
	}

	/**
	 * The value of Select is always a set.
	 *
	 * @return Flag indicating whether the node can return multiple values
	 */
	public boolean isMultiple() {
	    return true;
	}

	/**
	 * This predicate finds out whether the Select results in a simple XML schema
	 * type.
	 *
	 * @return Flag indicating whether the node has a simple type
	 */
	public boolean hasSimpleType() {
	    return children.get(0).hasSimpleType();
	}

	/**
	 * This predicate finds out whether the Select results in a collection of
	 * instances, which conceptually have identity.
	 *
	 * @return Flag indicating whether the node is an identity carrying type
	 */
	public boolean hasIdentity() {
	    return children.get(0).hasIdentity();
	}

	/**
	 * This compiles the Select node and its children to an Xpath expression
	 * fragment.
	 *
	 * <p>
	 * The object is translated in the given context and its ending position,
	 * supplemented by the binding variable, defines the context for the compilation
	 * of the body, which is appended as a predicate bracket.
	 *
	 * @param ctx BindingContext this node shall be compiled in
	 * @return Object containing the fragment and its operator priority
	 */
	public XpathFragment translate(BindingContext ctx) {

	    // Fetch and compile the object
	    SchematronConstraintNodeXslt2 obj = children.get(0);
	    XpathFragment xpt = obj.translate(ctx);

	    // Prepare the binding context for compilation of the iterator
	    // body. This is primarily the context at the end of the object
	    // plus the variable.
	    BindingContext bodyctx = xpt.atEnd.clone();
	    bodyctx.pushDeclaration(vardecl);
	    
	    bodyctx.inPredicateExpression = true;
	  
	    // Compile the boolean expression in the iterator body
	    SchematronConstraintNodeXslt2 pred = children.get(1);
	    XpathFragment prd = pred.translate(bodyctx);
	    prd.atEnd = null; // This suppresses the merging of ending contexts

	    // Append the boolean expression as a predicate filter
	    String filter = xpt.merge(prd);
	    if (xpt.priority < 19) {
		xpt.bracket();
	    }
	    
	    xpt.fragment += "[";
	    if(!vardecl.name.equalsIgnoreCase("(noname)")) {
		xpt.fragment += "for $"+vardecl.name + " in . return ";
	    }
	    xpt.fragment += filter + "]";
	    
	    xpt.priority = 19;

	    return xpt;
	}
    }

    /**
     * This class represents the OCL operation allInstances(). AllInstances is based
     * on a class literal and represents all instances of that class.
     */
    public static class AllInstances extends SchematronConstraintNodeXslt2 {

	protected ClassInfo objectClass;

	/**
	 * Ctor
	 *
	 * @param schemaObject The schema object
	 * @param ci           ClassInfo of the class to enumerate
	 * @param negated      May be negated if of type boolean
	 */
	public AllInstances(AbstractSchematronSchema schemaObject, ClassInfo ci, boolean negated) {
	    this.schemaObject = schemaObject;
	    this.objectClass = ci;
	    this.negated = negated;
	}

	/**
	 * Allinstances always produces a set.
	 *
	 * @return Flag indicating whether the node can return multiple values
	 */
	public boolean isMultiple() {
	    return true;
	}

	/**
	 * allInstances() is never simple.
	 *
	 * @return Flag indicating whether the node has a simple type
	 */
	public boolean hasSimpleType() {
	    return false;
	}

	/**
	 * This predicate finds out whether the allInstances results in a collection of
	 * instances, which conceptually have identity.
	 *
	 * @return Flag indicating whether the node is an identity carrying type
	 */
	public boolean hasIdentity() {
	    return XmlSchema.classCanBeReferenced(objectClass);
	}

	/**
	 * allInstances() is translated to a search for the given type. The result is a
	 * nodeset containing all the given features.
	 *
	 * <p>
	 * In compiling x.allInstances() we create an expression that selects all
	 * elements based upon a predicate which uses a combination of local-names and
	 * namespace-uris of the type and its subtypes (if they are instantiable, i.e.
	 * not abstract and not suppressed). We differentiate three cases:
	 *
	 * <ul>
	 * <li>Case multiple instantiable classes: //*[(local-name()='localName_class_1'
	 * and namespace-uri()='namespace_class_1') or ... or
	 * (local-name()='localName_class_n' and namespace-uri()='namespace_class_n')]
	 * <li>Case single instantiable class: //*[local-name()='localName_class' and
	 * namespace-uri()='namespace_class']
	 * <li>Case no instantiable class: //*[false()]
	 * </ul>
	 *
	 * @param ctx BindingContext this node shall be compiled in
	 */
	public XpathFragment translate(BindingContext ctx) {

	    // 1st obtain the necessary classes from the model
	    SortedSet<ClassInfo> relevantClasses = getInstantiableSubtypesInCompleteHierarchy(objectClass);

	    // also take into account the object class itself
	    if (isInstantiable(objectClass)) {
		relevantClasses.add(objectClass);
	    }

	    // 2nd create the Xpath expression combining all those classes
	    String fragment = "//*[";
	    fragment += getExpressionForTypeRestriction(relevantClasses).getLeft();
	    fragment += "]";

	    // Wrap and provide proper priority. Set binding context.
	    XpathFragment xpt = new XpathFragment(19, fragment);
	    xpt.atEnd = new BindingContext(BindingContext.CtxState.OTHER);

	    String var = xpt.findOrAdd(fragment);
	    xpt.fragment = "$" + var;
	    xpt.priority = 20;

	    return xpt;
	}
    }

    /**
     * This class represents the operation propertyMetadata(). It selects the
     * metadata object associated with a property.
     */
    public static class PropertyMetadata extends SchematronConstraintNodeXslt2 {

	protected ClassInfo metadataType;

	/**
	 * Ctor
	 *
	 * @param schemaObject The schema object
	 */
	public PropertyMetadata(AbstractSchematronSchema schemaObject, ClassInfo metadataType, boolean negated) {
	    this.schemaObject = schemaObject;
	    this.metadataType = metadataType;
	    this.negated = negated;
	}

	/**
	 * PropertyMetadata can produce a set. That depends on the collection (of
	 * property values, taking into account multiple property steps, i.e. a sequence
	 * of implicit collect operations) or variable it is operating on.
	 *
	 * @return Flag indicating whether the node can return multiple values
	 */
	public boolean isMultiple() {
	    return true;
	}

	/**
	 * propertyMetadata() is never simple. Otherwise it could not be referenced.
	 *
	 * @return Flag indicating whether the node has a simple type
	 */
	public boolean hasSimpleType() {
	    return false;
	}

	/**
	 * This predicate finds out whether the propertyMetadata results in a collection
	 * of instances, which conceptually have identity.
	 *
	 * @return Flag indicating whether the node is an identity carrying type
	 */
	public boolean hasIdentity() {
	    return XmlSchema.classCanBeReferenced(metadataType);
	}

	/**
	 * propertyMetadata() is translated to a lookup of referenced metadata. The
	 * result is a nodeset containing metadata objects.
	 *
	 * @param ctx BindingContext this node shall be compiled in
	 */
	public XpathFragment translate(BindingContext ctx) {

	    boolean metadataTypeIs19139Encoded = metadataType.matches("rule-xsd-cls-standard-19139-property-types");

	    SchematronConstraintNodeXslt2 objnode = children.get(0);
	    // update the binding context to let attribute translation know that access to
	    // property metadata is the intent
	    ctx.propertyMetadataAccess = true;
	    XpathFragment obj = objnode.translate(ctx);
	    ctx.propertyMetadataAccess = false;

	    if (objnode instanceof SchematronConstraintNodeXslt2.Variable) {
		obj.fragment = "***ERROR[128]***";
	    } else {

		// Get the prefixes that possibly surround the identifier contained
		// in xlink:href references
		String alpha = schemaObject.alpha;
		String beta = schemaObject.beta;
		boolean alphaEx = alpha != null && alpha.length() > 0;
		boolean betaEx = beta != null && beta.length() > 0;

		String idAttributeFrag;
		if (metadataTypeIs19139Encoded) {
		    idAttributeFrag = "@id";
		} else {
		    idAttributeFrag = "@gml:id";
		    schemaObject.registerNamespace("gml");
		}

		String metalink = (obj.fragment.isEmpty() ? "current()" : obj.fragment);

		if (obj.priority < 18) {
		    metalink = "(" + metalink + ")";
		}

		metalink += "/@metadata";

		String frag_ref = "//*[";
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
		frag_ref += "=" + metalink + "]";

		obj.fragment = "for $META in " + frag_ref + " return $META";
		obj.priority = 3;

		// Treat negation. Note that if this is being negated it must be
		// unique and boolean ...
		if (negated) {
		    obj.fragment = "not(" + obj.fragment + ")";
		    obj.priority = 20;
		    obj.atEnd.setState(BindingContext.CtxState.NONE);
		}
	    }
	    
	    return obj;
	}
    }

    /**
     * This class represents oclIsKindOf and oclIsTypeOf nodes. The difference
     * between the two is expressed in an <i>exact</i> flag. The object also carries
     * a negation flag to express that an object is NOT kind of some type. The type
     * is given by the first of the children.
     */
    public static class KindOf extends SchematronConstraintNodeXslt2 {

	// The class to be tested against
	protected ClassInfo argumentClass = null;
	// Exact match?
	protected boolean exact = false;

	/**
	 * Ctor
	 *
	 * @param schemaObject The schema object
	 * @param exact        Flag: Only check the given type
	 * @param neg          Flag: Negated meaning
	 */
	public KindOf(AbstractSchematronSchema schemaObject, boolean exact, boolean neg) {
	    this.schemaObject = schemaObject;
	    this.exact = exact;
	    negated = neg;
	}

	/**
	 * If the class to be tested against is already known (it is not an expression)
	 * this reference can be set via this method.
	 *
	 * @param ci ClassInfo representing the type to be tested against
	 */
	public void setClass(ClassInfo ci) {
	    argumentClass = ci;
	}

	/**
	 * This compiles the KindOf predicate (and its negation) to an equivalent Xpath
	 * expression fragment. KindOf is translated to a predicate which compares the
	 * element name against all instantiable subtypes of the given type, as well as
	 * that type.
	 *
	 * @param ctx BindingContext this node shall be compiled in
	 * @return Object containing the Xpath fragment
	 */
	public XpathFragment translate(BindingContext ctx) {

	    // Translate the object in the given context
	    SchematronConstraintNodeXslt2 objnode = children.get(0);
	    XpathFragment xptobj = objnode.translate(ctx);
	    boolean emptyobject = xptobj.fragment.length() == 0;

	    // Obtain the necessary classes from the model
	    SortedSet<ClassInfo> relevantClasses = new TreeSet<>();

	    if (!exact) {
		relevantClasses = getInstantiableSubtypesInCompleteHierarchy(argumentClass);
	    }

	    if (isInstantiable(argumentClass)) {
		relevantClasses.add(argumentClass);
	    }

	    // Construct the result expression
	    if (relevantClasses.size() == 0) {

		// There is no concrete class known. So, this must be false.
		xptobj.fragment = negated ? "true()" : "false()";
		xptobj.priority = 20;
		xptobj.type = XpathType.BOOLEAN;
		xptobj.atEnd.setState(BindingContext.CtxState.NONE);

	    } else {

		// There are one or more classes, append a predicate which
		// compares against all possible names.

		Pair<String, Integer> pair = getExpressionForTypeRestriction(relevantClasses);
		String expression = pair.getLeft();
		int expressionPriority = pair.getRight();

		if (negated) {
		    expression = "not(" + expression + ")";
		}

		if (!emptyobject) {
		    if (xptobj.priority < 19) {
			xptobj.bracket();
		    }
		    xptobj.fragment += "[" + expression + "]";
		    xptobj.priority = 19;
		} else {
		    xptobj.fragment = expression;
		    xptobj.priority = negated ? 20 : expressionPriority;
		}
	    }

	    return xptobj;
	}
    }

    /**
     * This class represents oclAsType(), which is for casting a type to one of its
     * subtypes.
     */
    public static class Cast extends SchematronConstraintNodeXslt2 {

	// The class to be cast to
	protected ClassInfo argumentClass = null;

	// Connection info, required to access the config document
	protected String targetClassName;

	/**
	 * Ctor
	 *
	 * @param schemaObject The schema object
	 */
	public Cast(AbstractSchematronSchema schemaObject) {
	    this.schemaObject = schemaObject;
	}

	/**
	 * If the class to be cast to is already known (it is not an expression) this
	 * reference can be set via this method.
	 *
	 * @param ci ClassInfo representing the type to be cast to
	 */
	public void setClass(ClassInfo ci) {
	    argumentClass = ci;
	}

	/**
	 * This predicate finds out whether the Cast results in a simple XML schema
	 * type.
	 *
	 * @return Flag indicating whether the node has a simple type
	 */
	public boolean hasSimpleType() {
	    return children.get(0).hasSimpleType();
	}

	/**
	 * This predicate finds out whether the Cast results in an instance, which
	 * conceptually has identity.
	 *
	 * @return Flag indicating whether the node is an identity carrying type
	 */
	public boolean hasIdentity() {
	    return children.get(0).hasIdentity();
	}

	/**
	 * This compiles the Cast to an Xpath fragment.
	 *
	 * <p>
	 * We realize this by making sure the current element is of the requested type
	 * or any of its concrete subtypes.
	 *
	 * @param ctx BindingContext this node shall be compiled in
	 * @return Object containing the Xpath fragment
	 */
	public XpathFragment translate(BindingContext ctx) {

	    // Translate the object in the given context
	    SchematronConstraintNodeXslt2 objnode = children.get(0);
	    XpathFragment xptobj = objnode.translate(ctx);
	    if (xptobj.fragment.length() == 0) {
		xptobj.fragment = "self::*";
		xptobj.priority = 19;
	    }

	    // Obtain the necessary classes from the model
	    SortedSet<ClassInfo> relevantClasses = getInstantiableSubtypesInCompleteHierarchy(argumentClass);

	    // also take into account the argument class itself
	    if (isInstantiable(argumentClass)) {
		relevantClasses.add(argumentClass);
	    }

	    // Append a predicate which compares against all possible
	    // names.
	    if (xptobj.priority < 19) {
		xptobj.bracket();
	    }
	    xptobj.fragment += ("[" + getExpressionForTypeRestriction(relevantClasses).getLeft() + "]");
	    xptobj.priority = 19;

	    // If we are casting a CharacterString to a Codelist, we have to add
	    // code, which property expresses the codelist access according to
	    // the applied encoding rule.

	    if (argumentClass.category() == Options.CODELIST && (argumentClass.matches("rule-xsd-all-naming-19139")
		    || (argumentClass.matches("rule-xsd-cls-codelist-asDictionaryGml33")
			    && argumentClass.asDictionaryGml33())
		    || (argumentClass.matches("rule-xsd-cls-codelist-asDictionary") && argumentClass.asDictionary()))) {

		// We need to know, whether we are subject to the 19139 regime
		// or if GML 3.3 encoding rules apply
		Attribute att = objnode.generatingAttribute();
		PropertyInfo pip = ((PropertyInfo) (att.attributes[att.attributes.length
			- 1].main.selector.modelProperty));
		boolean is19139 = argumentClass.matches("rule-xsd-all-naming-19139");
		boolean isgml33 = pip != null && pip.inClass().matches("rule-xsd-cls-codelist-asDictionaryGml33");

		/*
		 * If not translating according to GML 3.3 we need to prepare the
		 * CodeListValuePattern.
		 */
		String clvpat = "{value}";
		int nsubst = 1;

		if (!isgml33) {

		    String uri = argumentClass.taggedValue("codeList");
		    if (uri != null && uri.length() > 0) {
			clvpat = "{codeList}/{value}";
		    }

		    clvpat = this.schemaObject.determineCodeListValuePattern(argumentClass, clvpat);

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
		    String clvp = clvpat.replace("{codeList}", xptobj.fragment + "/@codeList");
		    xptobj.fragment = clvp.replace("{value}", xptobj.fragment + "/@codeListValue");
		} else if (!isgml33) {
		    // In elder GMLs we might find the codespace in
		    // the codespace attribute
		    if (nsubst == 2 && atcurr) {
			String v = xptobj.findOrAdd(xptobj.fragment);
			xptobj.fragment = "$" + v;
		    }
		    String clvp = clvpat.replace("{codeList}", xptobj.fragment + "/@codeSpace");
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
     * This class represents an OCL invocation of the size operation. Size can be
     * applied to anything with a -> and returns the number of elements of the
     * object interpreted as a collection. If applied to a String it determines its
     * length.
     */
    public static class Size extends SchematronConstraintNodeXslt2 {

	/** Flag: <code>true</code> if this is a set operation */
	boolean setoper = false;

	/**
	 * Ctor
	 *
	 * @param schemaObject The schema object
	 * @param set          Flag: This is a set operation
	 */
	public Size(AbstractSchematronSchema schemaObject, boolean set) {
	    this.schemaObject = schemaObject;
	    this.setoper = set;
	}

	/**
	 * Compile to an equivalent Xpath expression. The set variant is compiled to
	 * count() and the String variant goes to string-length().
	 *
	 * @param ctx BindingContext this node shall be compiled in
	 * @return Object containing the Xpath fragment
	 */
	public XpathFragment translate(BindingContext ctx) {

	    // Translate the object
	    SchematronConstraintNodeXslt2 obj = children.get(0);
	    XpathFragment xpt = obj.translate(ctx);
	    if (xpt.fragment.length() == 0) {
		xpt.fragment = ".";
	    }

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
	    xpt.priority = 20;
	    xpt.atEnd = new BindingContext(BindingContext.CtxState.NONE);

	    return xpt;
	}
    }

    /**
     * This class stands for an OCL concat operation. The operation concatenates the
     * string type object with the string type argument. This implementation allows
     * to specify more than one argument.
     */
    public static class Concatenate extends SchematronConstraintNodeXslt2 {

	/**
	 * Ctor
	 *
	 * @param schemaObject The schema object
	 */
	public Concatenate(AbstractSchematronSchema schemaObject) {
	    this.schemaObject = schemaObject;
	}

	/**
	 * This compiles a multivalued Concatenate, which has been built from a series
	 * of OCL concat() functions to Xpath concat().
	 *
	 * @param ctx BindingContext this node shall be compiled in
	 * @return Object containing the Xpath fragment
	 */
	public XpathFragment translate(BindingContext ctx) {

	    // Loop over the arguments and construct the parameter list
	    XpathFragment result = null;

	    for (SchematronConstraintNodeXslt2 arg : children) {

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
	    result.priority = 20;
	    result.atEnd.setState(BindingContext.CtxState.NONE);

	    return result;
	}
    }

    /**
     * This class stands for an OCL substring operation. The operation picks a range
     * from a string type object.
     */
    public static class Substring extends SchematronConstraintNodeXslt2 {

	/**
	 * Ctor
	 *
	 * @param schemaObject The schema object
	 */
	public Substring(AbstractSchematronSchema schemaObject) {
	    this.schemaObject = schemaObject;
	}

	/**
	 * This compiles a Substring object to its Xpath equivalent.
	 *
	 * @param ctx BindingContext this node shall be compiled in
	 * @return Object containing the Xpath fragment
	 */
	public XpathFragment translate(BindingContext ctx) {

	    // Translate the arguments
	    XpathFragment xptobj = children.get(0).translate(ctx);
	    if (xptobj.fragment.length() == 0) {
		xptobj.fragment = ".";
	    }
	    XpathFragment xptfr = children.get(1).translate(ctx);
	    if (xptfr.fragment.length() == 0) {
		xptfr.fragment = ".";
	    }
	    XpathFragment xptto = children.get(2).translate(ctx);
	    if (xptto.fragment.length() == 0) {
		xptto.fragment = ".";
	    }

	    // Merge
	    String fr = xptobj.merge(xptfr);
	    // String to = xptobj.merge( xptto );

	    // Construct the substring function
	    xptobj.fragment = "substring(" + xptobj.fragment + ", ";
	    xptobj.fragment += fr + ", ";
	    if (xptto.priority < 8) {
		xptto.bracket();
	    }
	    if (xptfr.priority <= 8) {
		xptfr.bracket();
	    }
	    xptobj.fragment += xptto.fragment + " - " + xptfr.fragment + " + 1)";

	    // Accompany with the required fragment attributes
	    xptobj.type = XpathType.STRING;
	    xptobj.priority = 20;
	    xptobj.atEnd.setState(BindingContext.CtxState.NONE);

	    return xptobj;
	}
    }

    /**
     * This class stands for matches operation, which this implemention added to
     * OCL's core functions.
     */
    public static class Matches extends SchematronConstraintNodeXslt2 {

	/**
	 * Ctor
	 *
	 * @param schemaObject The schema object
	 */
	public Matches(AbstractSchematronSchema schemaObject) {
	    this.schemaObject = schemaObject;
	}

	/**
	 * Matches operations are translated to fn:matches(..).
	 *
	 * @param ctx BindingContext this node shall be compiled in
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

	    /*
	     * 2019-09-10 JE: previous logic no longer required as we translate directly to
	     * XPath fn:matches(..) function
	     */

	    // // Fetch the extension template
	    // AbstractSchematronSchema.ExtensionFunctionTemplate eft =
	    // schemaObject.extensionFunctions.get("matches");
	    // if (eft == null) return new XpathFragment(20, "***ERROR[123]***");
	    //
	    // // Construct the extension function call
	    // String fcall =
	    // eft.function.replace("$object$", xptobj.fragment).replace("$pattern$",
	    // patstring);
	    // xptobj.fragment = eft.nsPrefix + ":" + fcall;
	    //
	    // // Accompany with the required fragment attributes
	    // xptobj.type = XpathType.STRING;
	    // xptobj.priority = 20;
	    // xptobj.atEnd.setState(BindingContext.CtxState.NONE);
	    //
	    // // We need to declare the namespace
	    // schemaObject.registerNamespace(eft.nsPrefix, eft.namespace);

	    xptobj.fragment = "matches(" + xptobj.fragment + ", " + patstring + ")";

	    // Accompany with the required fragment attributes
	    xptobj.type = XpathType.BOOLEAN;
	    xptobj.priority = 20;
	    xptobj.atEnd.setState(BindingContext.CtxState.NONE);

	    return xptobj;
	}
    }

    /** This class stands for OCL arithmetic. */
    public static class Arithmetic extends SchematronConstraintNodeXslt2 {

	/** The operation */
	String operation;

	/**
	 * Ctor
	 *
	 * @param schemaObject The schema object
	 * @param oper         The operation symbol, one of + ,-, *, /
	 */
	public Arithmetic(AbstractSchematronSchema schemaObject, String oper) {
	    this.schemaObject = schemaObject;
	    this.operation = oper;
	}

	/**
	 * This compiles a node to an Xpath expression, which realizes the given
	 * arithmetic operation. OCL and Xpath are very similar here.
	 *
	 * @param ctx BindingContext this node shall be compiled in
	 * @return Object containing the Xpath fragment
	 */
	public XpathFragment translate(BindingContext ctx) {

	    // One argument to be compiled in any case
	    XpathFragment xpt1 = children.get(0).translate(ctx);

	    // Take cases on operation and number of operands
	    if (children.size() == 1) {

		// Must be prefix - (i.e. unary operator '-')
		if (xpt1.priority <= 16) {
		    xpt1.bracket();
		}
		xpt1.priority = 16;

	    } else {

		// Two arguments. Get the second
		XpathFragment xpt2 = children.get(1).translate(ctx);
		// Find priority
		int prio;
		if (operation.equals("*") || operation.equals("/")) {
		    prio = 9;
		} else {
		    // operation is + or -
		    prio = 8;
		}
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
		xpt1.fragment += " " + (operation.equals("/") ? "div" : operation) + " " + op2;
		xpt1.priority = prio;
	    }

	    xpt1.type = XpathType.NUMBER;
	    xpt1.atEnd = new BindingContext(BindingContext.CtxState.NONE);
	    return xpt1;
	}
    }

    /**
     * This class represents an OCL variable. It wraps the Declaration node of the
     * OclNode object. If it happens to be boolean it can be negated.
     */
    public static class Variable extends SchematronConstraintNodeXslt2 {

	// Wrapped declaration object
	protected OclNode.Declaration vardecl;

	/**
	 * Ctor
	 *
	 * @param schemaObject The schema object
	 * @param vardecl      OclNode.Declaration object
	 * @param neg          Negation flag
	 */
	public Variable(AbstractSchematronSchema schemaObject, OclNode.Declaration vardecl, boolean neg) {
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
	 * This method determines whether this variable is identical to the one passed
	 * as argument.
	 *
	 * @param vardecl The Declaration of the variable
	 * @return Flag indicating the inquired dependency
	 */
	public boolean isDependentOn(OclNode.Declaration vardecl) {
	    return this.vardecl == vardecl;
	}

	/**
	 * This method determines whether this variable is identical to the one passed
	 * as argument.
	 *
	 * @param vardecl The Declaration of the variable
	 * @return Flag indicating the inquired property
	 */
	public boolean isVarOrAttribBased(OclNode.Declaration vardecl) {
	    return isDependentOn(vardecl);
	}

	/**
	 * This inquires the Attribute node this Variable is generated by if any.
	 *
	 * @return The retrieved Attribute node if there is such a thing
	 */
	public Attribute generatingAttribute() {
	    // Find the iterator, which binds this variable
	    SchematronConstraintNodeXslt2 binds = null;
	    SchematronConstraintNodeXslt2 boundTo = null;
	    for (SchematronConstraintNodeXslt2 scn = parent; scn != null; scn = scn.parent) {
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
	 * This predicate finds out whether the Variable results in a simple XML schema
	 * type.
	 *
	 * @return Flag indicating whether the node has a simple type
	 */
	public boolean hasSimpleType() {
	    SchematronConstraintNodeXslt2 binds = null;
	    SchematronConstraintNodeXslt2 boundTo = null;
	    for (SchematronConstraintNodeXslt2 scn = parent; scn != null; scn = scn.parent) {
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
	 * This predicate finds out whether the Variable results in an instance, which
	 * conceptually has identity.
	 *
	 * @return Flag indicating whether the node is an identity carrying type
	 */
	public boolean hasIdentity() {
	    SchematronConstraintNodeXslt2 binds = null;
	    SchematronConstraintNodeXslt2 boundTo = null;
	    for (SchematronConstraintNodeXslt2 scn = parent; scn != null; scn = scn.parent) {
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
	 * This compiles a node to an Xpath expression, which stands for the given
	 * variable.
	 *
	 * <p>
	 * If the variable is defined in a surrounding 'let' construct, a proper
	 * translation for the use of the variable can always be achieved, given that
	 * the initial value of the variable translates properly. If the use of the
	 * variable is in ISCURRENT context, the variable definition will be mapped into
	 * a Schematron &lt;let&gt; definition. Otherwise the initial value is
	 * substituted in place of the variable.
	 *
	 * <p>
	 * Other variable references are treated as follows.
	 *
	 * <p>
	 * The only variable which can be properly translated in all cases is
	 * <i>self</i>, which will be mapped to <i>current()</i> or to '.', if compiled
	 * in a ISCURRENT context. Variable definitions from iterators require to be on
	 * the context stack of the expression, which is widely dependent on how the
	 * expression environment could be represented in Xpath.
	 *
	 * @param ctx BindingContext this node shall be compiled in
	 * @return Object containing the Xpath fragment
	 */
	public XpathFragment translate(BindingContext ctx) {

	    // Initialize a PathFragment
	    XpathFragment xpt = new XpathFragment(20, "");

	    // Find out the parent node, which binds the variable
	    SchematronConstraintNodeXslt2 binds = null;
	    for (SchematronConstraintNodeXslt2 scn = parent; scn != null; scn = scn.parent) {
		if (scn.nodeVariableIsBoundTo(vardecl) != null) {
		    binds = scn;
		    break;
		}
	    }

	    if (binds != null && binds instanceof Let) {
		// Treatment for 'let' expression ...
		// Grab the initial value expression
		Let let = (Let) binds;
		SchematronConstraintNodeXslt2 expr = null;
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
		    xpt.priority = 20;
		} else
		    // Elsewhere: The current binding context applies and the
		    // expression is just substituted.
		    xpt = expr.translate(ctx);
	    } else if (getName().equals("self")) {
		// Must be iterator context. Treat self
		xpt.atEnd = new BindingContext(BindingContext.CtxState.ATCURRENT);
		if (ctx.state != BindingContext.CtxState.ATCURRENT) {
		    xpt.fragment = "current()";
		}
	    } else if (binds != null && binds instanceof Exists) {

		xpt.fragment = "$" + this.vardecl.name;

	    } else if (binds != null && binds instanceof ForAll) {

		xpt.fragment = "$" + this.vardecl.name;

	    } else if (binds != null && binds instanceof Select) {

		xpt.fragment = "$" + this.vardecl.name;

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
			    xpt.priority = 18;
		    }
		    xpt.atEnd = new BindingContext(BindingContext.CtxState.OTHER);
		}
	    }

	    // if (xpt.fragment.isEmpty()) {
	    // xpt.fragment = "current()";
	    // }

	    return xpt;
	}
    }

    /**
     * This class represents a chain of attribute selectors based on some value
     * source such as a variable, a select() or allInstances. The value source is
     * the sole child of the Attribute object.
     */
    public static class Attribute extends SchematronConstraintNodeXslt2 {

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
	 * @param schemaObject The schema object
	 * @param attr         The (possibly first) AttributeCallExp object
	 * @param negated      May be negated if of type boolean
	 */
	public Attribute(AbstractSchematronSchema schemaObject, OclNode.AttributeCallExp attr, boolean negated) {
	    this.schemaObject = schemaObject;
	    this.attributes = new AttrComp[] { new AttrComp(attr) };
	    this.negated = negated;
	}

	/**
	 * Ctor - starting from AttrComp
	 *
	 * @param schemaObject The schema object
	 * @param atc          AttrComp object
	 * @param negated      May be negated if of type boolean
	 */
	public Attribute(AbstractSchematronSchema schemaObject, AttrComp atc, boolean negated) {
	    this.schemaObject = schemaObject;
	    this.attributes = new AttrComp[] { new AttrComp(atc) };
	    this.negated = negated;
	}

	/**
	 * Append another AttributeCallExp and associated layout info as an additional
	 * qualification.
	 *
	 * @param aex The AttributeCallExp to be appended be null)
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
	 * Append another AttrComp and associated layout info as an additional
	 * qualification.
	 *
	 * @param aex The AttrComp object to be appended be null)
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
	 * This method marks the last attribute component as being absorbed by the
	 * construct before. This also includes nilReason implementation. The type
	 * (1=simple absorption, 2=reason) and the associated OclNode are stored
	 * together with the last attribute component.
	 *
	 * @param absorptionType Implementation type: 1=normal absorption, 2=reason
	 * @param attr           The OclNode representing the absorbed property.
	 */
	public void appendAbsorbedAttribute(int absorptionType, OclNode.AttributeCallExp attr) {
	    int last = attributes.length - 1;
	    attributes[last].absAttr = attr;
	    attributes[last].absType = absorptionType;
	}

	/**
	 * Split the Attribute object before the given index.
	 *
	 * @param at The index before to split
	 * @return Right hand part of the split, which contains the left hand part as
	 *         its child.
	 */
	public Attribute splitBefore(int at) {
	    if (at < 0 || at >= attributes.length)
		return null;
	    // Create a new Attribute node, which contains the selectors
	    // starting at the given position. This will also be the
	    // returned value.
	    Attribute atrite = new Attribute(schemaObject, attributes[at], negated);
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
		Attribute atleft = new Attribute(schemaObject, attributes[0], negated);
		for (int i = 1; i < at; i++)
		    appendAttribute(attributes[i]);
		atleft.addChild(children.get(0));
		atrite.addChild(atleft);
	    }
	    return atrite;
	}

	/**
	 * This Attribute predicate finds out, whether the last attribute component in
	 * the object is implemented as a group and is therefore absorbing its
	 * properties. If there is already a property absorbed on the attribute, the
	 * absorbed property will be asked.
	 *
	 * <p>
	 * Note that this is a necessary condition for appying GML's nilReason pattern.
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
	 * This method determines whether this Attribute is dependent on the Variable
	 * passed as argument.
	 *
	 * @param vardecl The Declaration of the variable
	 * @return Flag indicating the inquired property
	 */
	public boolean isVarOrAttribBased(OclNode.Declaration vardecl) {
	    return children.get(0).isVarOrAttribBased(vardecl);
	}

	/**
	 * This inquires the Attribute node this Attribute is generated by. It's this
	 * Attribute!
	 *
	 * @return The retrieved Attribute node
	 */
	public Attribute generatingAttribute() {
	    return this;
	}

	/**
	 * This method returns true if any of the OclNode.Attribute objects it is made
	 * of has a possible cardinality greater than 1.
	 */
	public boolean isMultiple() {
	    for (AttrComp at : attributes) {
		if (at.main.multMapping == MultiplicityMapping.ONE2MANY
			|| at.absType == 1 && at.absAttr.multMapping == MultiplicityMapping.ONE2MANY)
		    return true;
	    }
	    return false;
	}

	/**
	 * This predicate finds out whether the Attribute as a whole results in a simple
	 * XML schema type. Note that for convenience reasons this also includes the
	 * GML's xsi:nil construct.
	 *
	 * @return Flag indicating whether the Attribute has a simple type
	 */
	public boolean hasSimpleType() {
	    int last = attributes.length - 1;
	    return hasSimpleType(last);
	}

	/**
	 * This predicate finds out whether the Attribute component at the given index
	 * <i>idx</i> results in a simple XML schema type. Note that for convenience
	 * reasons this also includes the GML's xsi:nil construct.
	 *
	 * @param idx Index of the attribute component
	 * @return Flag indicating whether the attribute component has a simple type
	 */
	public boolean hasSimpleType(int idx) {

	    ClassInfo ci;
	    boolean result = true;

	    switch (attributes[idx].absType) {
	    case 0: // Normal attribute
		ci = attributes[idx].main.dataType.umlClass;
		if (ci != null) {
		    Boolean indicatorSimpleType = XmlSchema.indicatorForObjectElementWithSimpleContent(ci);
		    result = !XmlSchema.classHasObjectElement(ci)
			    || (indicatorSimpleType != null && indicatorSimpleType);
		}
		break;
	    case 1: // Normal absorption
		ci = attributes[idx].absAttr.dataType.umlClass;
		if (ci != null) {
		    Boolean indicatorSimpleType = XmlSchema.indicatorForObjectElementWithSimpleContent(ci);
		    result = !XmlSchema.classHasObjectElement(ci)
			    || (indicatorSimpleType != null && indicatorSimpleType);
		}
		break;
	    case 2: // Nil-implementation attribute with a "reason" selector
	    }
	    return result;
	}

	/**
	 * This predicate finds out whether the Attribute as a whole results in
	 * instances, which conceptually have identity.
	 *
	 * @return Flag indicating whether the node is an identity carrying type
	 */
	public boolean hasIdentity() {
	    int last = attributes.length - 1;
	    return hasIdentity(last);
	}

	/**
	 * This predicate finds out whether the Attribute component at the given index
	 * <i>idx</i> results in a schema type, which carries identity. Note that for
	 * convenience reasons this also includes GML's xsi:nil construct.
	 *
	 * @param idx Index of the attribute component
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
	 * This function translates the Attribute to an Xpath fragment accessing that
	 * attribute. Attributes can be negated, in which case they are boolean and not
	 * multiple.
	 *
	 * @param ctx BindingContext this node shall be compiled in
	 * @return XpathFragment representing the Attribute access
	 */
	public XpathFragment translate(BindingContext ctx) {

	    // First, obtain the feature attribute expressed by PropertyInfo
	    // objects.
	    PropertyInfo[] props = new PropertyInfo[attributes.length];
	    for (int i = 0; i < props.length; i++) {
		props[i] = (PropertyInfo) attributes[i].main.selector.modelProperty;
	    }

	    /*
	     * Translate the object. This is a variable (self in most of the cases), or
	     * allInstances() or any type of iterator such as exists, forAll, select.
	     */
	    SchematronConstraintNodeXslt2 objnode = children.get(0);
	    XpathFragment obj = objnode.translate(ctx);

	    if (obj.fragment.length() > 0) {
		if (obj.priority < 18) {
		    obj.bracket();
		}
	    }

	    // Get the prefixes that possibly surround the identifier contained
	    // in xlink:href references
	    String alpha = schemaObject.alpha;
	    String beta = schemaObject.beta;
	    boolean alphaEx = alpha != null && alpha.length() > 0;
	    boolean betaEx = beta != null && beta.length() > 0;

	    // Now step along the properties and generate the associated Xpath
	    // code for it.
	    boolean lastWasSimple = objnode.hasSimpleType();

	    // TODO JE: invent a way to automatically check the OCL expression for variable
	    // name conflict, and to compute a new name if necessary.
	    String forExprVariablePrefix = "VAR";
	    int forExprVariableCounter = 0;

	    // contains the $ sign as prefix
	    String lastForExprVariable;
	    if (obj.fragment.isEmpty()) {
		lastForExprVariable = "current()";
	    } else {
		lastForExprVariable = obj.fragment;
	    }

	    String fragmentForInlinePropertySequence = "";

	    /*
	     * Reset the fragment to avoid generating the variable twice. Explanation: the
	     * variable name is now stored in lastForExprVariable, which is added by the
	     * following code - which also adds to obj.fragment.
	     */
	    obj.fragment = "";

	    int countForExpr = 0;

	    for (int idx = 0; idx < props.length && !lastWasSimple; idx++) {

		// The property
		PropertyInfo pi = props[idx];

		// Find out how the relation to the contained object is realized
		// in XML Schema:
		// 0 = simple, 1 = in-line embedded, 2 = per xlink:href, 3 = 1|2
		// Start by assuming 'simple'

		int conCode = getContainmentCode(idx, pi);

		// determine if property metadata access applies
		// in that case, we would simply attach the qname of the property
		boolean isPropertyMetadataAccess = ctx.propertyMetadataAccess && idx == props.length - 1;

		String typeCiId = pi.typeInfo().id;
		ClassInfo typeCi = null;
		if (typeCiId != null) {
		    typeCi = pi.model().classById(typeCiId);
		}
		// If absorbing assume the type of the absorbed entity
		if (attributes[idx].absType == 1) {
		    typeCi = attributes[idx].absAttr.dataType.umlClass;
		}

		/*
		 * Now that relation to contained object is known, see if we have one or more
		 * inline properties that need to be appended now.
		 */
		if (!fragmentForInlinePropertySequence.isEmpty()) {

		    // so there is a fragment for inline props from previous iterations

		    if (isPropertyMetadataAccess || conCode == 0) {

			/*
			 * Current property has a simple type, or property metadata access is the goal.
			 * We need to append the fragment built in previous iterations now. No need for
			 * a 'for' expression.
			 */
			obj.fragment += (lastForExprVariable.isEmpty() ? "" : lastForExprVariable + "/")
				+ fragmentForInlinePropertySequence;

			lastForExprVariable = "";
			fragmentForInlinePropertySequence = "";

		    } else if (conCode > 1) {

			/*
			 * The current property is encoded byReference (and maybe inline). We need to
			 * incorporate the fragment built in previous iteration(s) now, using a 'for'
			 * expression.
			 */

			forExprVariableCounter += 1;
			String newForExprVar = "$" + forExprVariablePrefix + forExprVariableCounter;
			obj.fragment += "(for " + newForExprVar + " in "
				+ (lastForExprVariable.isEmpty() ? "" : lastForExprVariable + "/")
				+ fragmentForInlinePropertySequence + " return ";
			lastForExprVariable = newForExprVar;
			fragmentForInlinePropertySequence = "";
			countForExpr++;

		    }

		} else if ((isPropertyMetadataAccess || conCode == 0) && !lastForExprVariable.isEmpty()) {

		    /*
		     * No inline fragment, but a for expr variable is available. It must be added
		     * before we encode the property (with simple type, or for property metadata
		     * access).
		     */
		    obj.fragment += lastForExprVariable;
		    lastForExprVariable = "";
		}

		String propertyQName = schemaObject.getAndRegisterXmlName(pi);

		boolean is19139 = pi.matches("rule-xsd-all-naming-19139")
			|| (typeCi != null && typeCi.matches("rule-xsd-cls-standard-19139-property-types"));
		boolean piIsGml33Encoded = pi.inClass().matches("rule-xsd-cls-codelist-asDictionaryGml33");

		// Dispatch on containment cases and property metadata access
		if (isPropertyMetadataAccess) {

		    // the goal is to access the metadata of the current property; simply append the
		    // QName of the property; ignore containment (because metadata access occurs via
		    // the @metadata XML attribute of the property element)
		    obj.fragment += "/" + propertyQName;

		} else if (conCode == 0) {

		    /*
		     * 0: Simple type is contained. This also comprises access to property 'reason'
		     * in GML's nilReason treatment.
		     */

		    if (obj.fragment.length() > 0) {

			// if (obj.priority < 18) {
			// obj.bracket();
			// }

			// DONE? TODO JE: we likely have to check if we need to append to a custom
			// 'for'-variable
			// here, instead of to the object fragment
			obj.fragment += "/";
			// obj.priority = 18;
		    }
		    obj.fragment += propertyQName;

		    /*
		     * We also need to know if the property has a codelist type.
		     */
		    boolean iscodelist = typeCi != null && typeCi.category() == Options.CODELIST
			    && ((typeCi.matches("rule-xsd-cls-codelist-asDictionaryGml33")
				    && typeCi.asDictionaryGml33())
				    || (typeCi.matches("rule-xsd-cls-codelist-asDictionary") && typeCi.asDictionary())
				    || typeCi.matches("rule-xsd-cls-standard-19139-property-types"));

		    /*
		     * If it's a codelist and the property is not encoded according to GML 3.3 we
		     * prepare the CodeListValuePattern, to construct an XPath expression which can
		     * be used in comparisons with enumeration literals (for example when testing on
		     * allowed code values). an enumeration literal is always constructed according
		     * to the code list value pattern.
		     */
		    String clvpat = "{value}";
		    // int nsubst = 1;

		    if (typeCi != null && iscodelist && !piIsGml33Encoded) {

			String uri = typeCi.taggedValue("codeList");
			if (uri != null && uri.length() > 0) {
			    clvpat = "{codeList}/{value}";
			}

			clvpat = this.schemaObject.determineCodeListValuePattern(typeCi, clvpat);

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
			    // nsubst = 2;
			    //
			    // /*
			    // * We will be using a function as location step in
			    // * the path expression. This is not supported by
			    // * (the syntax of) XPath 1.0. Also see comments from
			    // * Dimitre Novatchev and Michael Kay on
			    // * https://stackoverflow.com/questions/333249/how-to
			    // * -apply-the- xpath-function-substring-after and
			    // * https://www.oxygenxml.com/archives/xsl-list/
			    // * 200603/msg00610. html. However, XPath 2.0 / the
			    // * xslt2 query binding supports this.
			    // */
			    // schemaObject.setQueryBinding("xslt2");
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
			obj.priority = 18;
		    }

		    // Nillable 'value' access in GML
		    if (attributes[idx].absType == 1) {
			if (typeCi.isUnionDirect()) {
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
		    // boolean atcurr = ctx.state == BindingContext.CtxState.ATCURRENT;

		    if (attributes[idx].absType == 0) {

			if (is19139) {

			    /*
			     * Under 19139 encoding, we will have to match another element level, even for
			     * simple types
			     */
			    obj.fragment += "/*";
			    if (obj.atEnd != null) {
				obj.atEnd.addStep();
			    }

			    // For codelists we have to add an attribute access
			    if (typeCi != null && typeCi.category() == Options.CODELIST) {

				/*
				 * TODO JE: binding to a let variable will likely be a problem (better to avoid
				 * this) - test with UnitTest
				 * SchematronTest.schematronTestOclOnCodelistTypedProperty - and XSLT2 binding.
				 */
				// if (nsubst == 2 && atcurr) {
				// String v = obj.findOrAdd(obj.fragment);
				// obj.fragment = "$" + v;
				// }

				// /*
				// * Only consider elements that have a value. In
				// * 19139 encoding, that means that a child
				// * element must be present. If we didn't add
				// * this check, then the comparison with the
				// * translated code/enumeration literal would
				// * fail for an empty element. However, we do not
				// * need the check if clvpat is just {value},
				// * because then an empty element would
				// * automatically be ignored.
				// */
				// if (!clvpat.equals("{value}")) {
				// obj.fragment += "[*]";
				// }

				obj.fragment = createCodeListValueExpression(obj.fragment, clvpat, "@codeList",
					"@codeListValue");
			    }

			} else if (iscodelist) {

			    if (!piIsGml33Encoded) {

				/*
				 * So the property is not encoded according to the GML 3.3 encoding rule. In
				 * elder GMLs we might find the code list URI in the @codeSpace attribute. The
				 * value is the text of the property element (which has type gml:CodeType).
				 *
				 * NOTE: We need to actually access the text node of the property element, not
				 * just the element node. Otherwise '.' would not work, for example when
				 * checking for property->notEmpty().
				 */

				/*
				 * Only consider elements that are not nil. If we didn't add this check, then
				 * the comparison with the translated code/enumeration literal would fail for an
				 * element that is nil. However, we only need this check if the property is
				 * nillable and clvpat is not equal to {value} (if it is just {value} then a nil
				 * element without text content would automatically be ignored).
				 */
				if (pi.voidable() && !clvpat.equals("{value}")) {
				    obj.fragment += "[not(@xsi:nil='true')]";
				    schemaObject.registerNamespace("xsi");
				}

				obj.fragment = createCodeListValueExpression(obj.fragment, clvpat, "@codeSpace",
					"text()");

			    } else {

				/*
				 * If using GML 3.3 type codelist treatment, we have to refer to the xlink:href
				 * attribute
				 */
				// TODO JE: again, why replace with let variable here? Better to avoid this!
				// if (nsubst == 2 && atcurr) {
				// String v = obj.findOrAdd(obj.fragment);
				// obj.fragment = "$" + v;
				// }
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
		    String frag_inl = "";
		    String frag_ref = "";

		    if (conCode == 1 || conCode == 3) {

			if (fragmentForInlinePropertySequence.isEmpty()) {
			    if (!lastForExprVariable.isEmpty()) {
				frag_inl = lastForExprVariable + "/";
			    }
			} else {
			    frag_inl += "/";
			}
			frag_inl += propertyQName;
			if (obj.atEnd != null)
			    obj.atEnd.addStep();
			frag_inl += "/*";
			if (obj.atEnd != null)
			    obj.atEnd.addStep();
		    }

		    if (conCode == 2 || conCode == 3) {

			// Reference containment must be treated

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

			String attxlink = (lastForExprVariable.isEmpty() ? "" : lastForExprVariable + "/");
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
		    }

		    lastForExprVariable = "";

		    // Set the fragment value, possibly combining both
		    // containment representations
		    if (conCode == 1) {

			fragmentForInlinePropertySequence += frag_inl;

		    } else {

			String inExpr;
			if (conCode == 2) {
			    inExpr = frag_ref;
			} else {
			    inExpr = "(" + frag_inl + " | " + frag_ref + ")";
			}

			forExprVariableCounter += 1;
			String newForExprVar = "$" + forExprVariablePrefix + forExprVariableCounter;
			obj.fragment += "(for " + newForExprVar + " in "
			// + (lastForExprVariable.isEmpty() ? "" :
			// lastForExprVariable+ "/")
				+ inExpr + " return ";
			lastForExprVariable = newForExprVar;
			countForExpr++;
		    }

		    if (obj.atEnd != null) {
			obj.atEnd.setState(BindingContext.CtxState.OTHER);
		    }

		    // // Other: different modes of containment are possible
		    // String frag_inl = null;
		    // String frag_ref = null;
		    // boolean isVar = false;
		    //
		    // if (conCode == 2 || conCode == 3) {
		    //
		    // /*
		    // * If not inline, we will create a 'let' variable unless
		    // * we are in the middle of some relative addressing
		    // * scheme
		    // */
		    // if (obj.atEnd.state == BindingContext.CtxState.OTHER
		    // && obj.fragment.length() > 0
		    // && !obj.fragment.startsWith(".")) {
		    //
		    // // store current obj.fragment as a variable
		    // String var = obj.findOrAdd(obj.fragment);
		    // obj.fragment = "$" + var;
		    // obj.priority = 20;
		    // isVar = true;
		    //
		    // if (ctx.inPredicateExpression) {
		    // obj.fragment = "***ERROR[127," + pi.name() + "]***";
		    // }
		    // }
		    // }
		    //
		    // if (conCode == 1 || conCode == 3) {
		    //
		    // // In-line containment must be treated
		    // // --> .../attr/*
		    // frag_inl = obj.fragment;
		    //
		    // if (frag_inl.length() > 0) {
		    //
		    // if (obj.priority < 18) {
		    //
		    // frag_inl = "(" + frag_inl + ")";
		    // /*
		    // * NOTE: Setting the obj.priority here (to 11)
		    // * is not necessary. The priority of the whole
		    // * obj.fragment, once it has been created, is
		    // * important. It will be set later on.
		    // */
		    // }
		    //
		    // frag_inl += "/";
		    // }
		    // frag_inl += propertyQName;
		    // if (obj.atEnd != null) obj.atEnd.addStep();
		    // frag_inl += "/*";
		    // if (obj.atEnd != null) obj.atEnd.addStep();
		    //
		    // if (obj.atEnd != null) {
		    // obj.atEnd.setState(BindingContext.CtxState.OTHER);
		    // }
		    // }
		    //
		    // if (conCode == 2 || conCode == 3) {
		    //
		    // // Reference containment must be treated
		    //
		    // if (!isVar) {
		    //
		    // // If not stored in a variable, the object now
		    // // has to be compiled in an undefined context.
		    // BindingContext ctx1 = ctx.clone();
		    //
		    // /*
		    // * 2018-06-19 JE: Setting a cloned context with
		    // * state OTHER is fine. However, setState() also
		    // * sets the variables to null. That is an issue in
		    // * case that the variable need to be looked up, for
		    // * example in a constraint like: inv:
		    // * prop1->forAll(x|x.prop2->size() = 1)
		    // *
		    // * The way that let variables are created further
		    // * above can be an issue if a longer property path
		    // * was used in forAll(), like:
		    // * prop1->forAll(x|x.prop2.prop3->size() = 1)
		    // *
		    // * If prop3 can be encoded byReference (conCode==2
		    // * or conCode==3), then at the moment a let variable
		    // * would be created to compute objects of prop2.
		    // * However, the context of prop2 in the let
		    // * statement would be the "current" object, not an
		    // * object of prop1. That is a limitation with
		    // * XSLT/XPath 1. With XSLT/XPath 2 we could use the
		    // * XPath expression
		    // * "for $var in expr return expr".
		    // *
		    // * To support the simple case of a single property
		    // * step within forAll(), we reset the state but keep
		    // * the variables.
		    // */
		    //
		    // // ctx1.setState(BindingContext.CtxState.OTHER);
		    // ctx1.setStateKeepingVariables(BindingContext.CtxState.OTHER);
		    //
		    // XpathFragment obj1 = children.get(0).translate(ctx1);
		    // String frag = obj.merge(obj1);
		    // obj.fragment = frag;
		    // }
		    //
		    // // --> //*[@gml:id]=.../attr/@xlink:href]
		    // // --> if @gml:id is surrounded with additional text,
		    // // --> a concat() construct is used.
		    //
		    // String idAttributeFrag;
		    // if (is19139) {
		    // idAttributeFrag = "@id";
		    // } else {
		    // idAttributeFrag = "@gml:id";
		    // schemaObject.registerNamespace("gml");
		    // }
		    //
		    // String attxlink = obj.fragment;
		    // if (attxlink.length() > 0) {
		    //
		    // if (obj.priority < 18) {
		    // attxlink = "(" + attxlink + ")";
		    // /*
		    // * NOTE: Setting the obj.priority here (to 11)
		    // * is not necessary. The priority of the whole
		    // * obj.fragment, once it has been created, is
		    // * important. It will be set later on.
		    // */
		    // }
		    //
		    // attxlink += "/";
		    // }
		    // attxlink += propertyQName;
		    // attxlink += "/@xlink:href";
		    // frag_ref = "//*[";
		    // if (alphaEx || betaEx) {
		    // frag_ref += "concat(";
		    // if (alphaEx) {
		    // frag_ref += "'" + alpha + "',";
		    // }
		    // frag_ref += idAttributeFrag;
		    // if (betaEx) {
		    // frag_ref += ",'" + beta + "'";
		    // }
		    // frag_ref += ")";
		    // } else {
		    // frag_ref += idAttributeFrag;
		    // }
		    // frag_ref += "=" + attxlink + "]";
		    // schemaObject.registerNamespace("xlink");
		    //
		    // // Whatever binding context we had before, it is lost
		    // if (obj.atEnd != null) {
		    // obj.atEnd.setState(BindingContext.CtxState.OTHER);
		    // }
		    // }
		    //
		    // // Set the fragment value, possibly combining both
		    // // containment representations
		    // if (conCode == 3) {
		    //
		    // obj.fragment = frag_inl + " | " + frag_ref;
		    // obj.priority = 10;
		    //
		    // } else if (conCode == 1) {
		    //
		    // obj.fragment = frag_inl;
		    // obj.priority = 18;
		    //
		    // } else {
		    //
		    // obj.fragment = frag_ref;
		    // obj.priority = 19;
		    // }
		}
	    }

	    if (!fragmentForInlinePropertySequence.isEmpty()) {
		obj.fragment += (lastForExprVariable.isEmpty() ? "" : lastForExprVariable + "/")
			+ fragmentForInlinePropertySequence;
	    } else if (!lastForExprVariable.isEmpty()) {
		obj.fragment += lastForExprVariable;
	    }
	    // add closing brackets, strip overall surrounding brackets
	    if (countForExpr > 0) {
		obj.fragment += StringUtils.repeat(")", countForExpr - 1);
		obj.fragment = obj.fragment.substring(1);
		obj.priority = 3;
	    } else {
		obj.priority = 18;
	    }

	    // Treat negation. Note that if this is being negated it must be
	    // unique and boolean ...
	    if (negated) {
		obj.fragment = "not(" + obj.fragment + ")";
		obj.priority = 20;
		obj.atEnd.setState(BindingContext.CtxState.NONE);
	    }

	    // Return fragment
	    return obj;
	}

	private int getContainmentCode(int idx, PropertyInfo pi) {

	    int conCode = 0;

	    String typeCiId = pi.typeInfo().id;
	    ClassInfo typeCi = null;
	    if (typeCiId != null) {
		typeCi = pi.model().classById(typeCiId);
	    }
	    // If absorbing assume the type of the absorbed entity
	    if (attributes[idx].absType == 1) {
		typeCi = attributes[idx].absAttr.dataType.umlClass;
	    }
	    if (!hasSimpleType(idx)) {

		/*
		 * Not a simple type, use in-line as default, then check if byReference or
		 * inlineOrByReference
		 */
		conCode = 1;

		if (typeCi != null && XmlSchema.classCanBeReferenced(typeCi)) {

		    String ref = pi.inlineOrByReference();
		    if ("byreference".equals(ref)) {
			conCode = 2;
		    } else if (!"inline".equals(ref)) {
			conCode = 3;
		    }
		}
	    }

	    return conCode;
	}

	private String createCodeListValueExpression(String xpathFragment, String codeListValuePattern,
		String codeListReplacementExpression, String valueReplacementExpression) {

	    /*
	     * 2018-03-02 JE: The expression must support the case that multiple values are
	     * encoded for the code list valued property. In that situation, the {codeList}
	     * access as well as the {value} access must result in single values, not node
	     * sets (because they are not supported by fn:concat(...), which is used in the
	     * code list value pattern unless that pattern is equal to {value}). For a
	     * multi-valued property, the expression should create a sequence of text
	     * values.
	     *
	     * NOTE: The UnitTest SchematronTest.schematronTestOclOnCodelistTypedProperty is
	     * used to check this encoding.
	     */
	    String clvp = codeListValuePattern.replace("{codeList}", codeListReplacementExpression).replace("{value}",
		    valueReplacementExpression);

	    String result = xpathFragment + "/" + clvp;

	    return result;
	}
    }

    /** This wraps any form of Literal value from the OclNode. */
    public static class Literal extends SchematronConstraintNodeXslt2 {

	OclNode.LiteralExp literal;

	/**
	 * Ctor
	 *
	 * @param schemaObject The schema object
	 * @param lit          OclNode.LiteralExp object
	 * @param neg          Negation flag
	 */
	public Literal(AbstractSchematronSchema schemaObject, OclNode.LiteralExp lit, boolean neg) {
	    this.schemaObject = schemaObject;
	    literal = lit;
	    negated = neg;
	}

	/**
	 * This function translates the Literal to equivalent Xpath code.
	 *
	 * @param ctx BindingContext this node shall be compiled in
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
		     * Enums and codelist constants will generally be translated to string constants
		     * representing their value.
		     *
		     * Codelists, additionally, can carry the 'codeList' tagged value the value of
		     * which will be combined according to a pattern given by the
		     * 'codeListValuePattern' tagged value to produce an identifier.
		     */

		    /*
		     * TBD 2018-02-01 JE: The value of a code/enum typically is the literal as a
		     * String, i.e. in case of a code/enum its name. However, some communities may
		     * prefer using the initial value, if it exists. In that case, the initial value
		     * would have to be used as value. However, it's unclear how to configure this
		     * preference. Should it be a global option? But what about code lists in
		     * external schemas, which may not have been encoded according to this
		     * preference? Does that mean that we can check based upon the encoding rule of
		     * the code list? Or should the tagging of a code list be extended, for example
		     * adding a tagged value 'initialValueAsCode' of type boolean, which, if set to
		     * true, would instruct ShapeChange to encode a code/enum as literal using the
		     * initial value, if it exists?
		     */
		    // PropertyInfo pi = litex.umlProperty;
		    // if (pi != null && pi.initialValue() != null) {
		    //
		    // }

		    boolean iscodelist = ci != null && ci.category() == Options.CODELIST;

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
			clVPat = this.schemaObject.determineCodeListValuePattern(ci, clVPat);
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

	    } else if (literal instanceof OclNode.IntegerLiteralExp || literal instanceof OclNode.RealLiteralExp) {

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
	    XpathFragment xpt = new XpathFragment(20, value, type);
	    return xpt;
	}
    }

    /**
     * This class represents an if ... then ... else ... endif construct. Negation
     * is already applied in construction by switching then and else.
     */
    public static class IfThenElse extends SchematronConstraintNodeXslt2 {

	/**
	 * Ctor
	 *
	 * @param schemaObject The schema object
	 */
	public IfThenElse(AbstractSchematronSchema schemaObject) {
	    this.schemaObject = schemaObject;
	}

	/**
	 * This predicate finds out whether the IfThenElse results in a simple XML
	 * schema type.
	 *
	 * @return Flag indicating whether the node has a simple type
	 */
	public boolean hasSimpleType() {
	    return children.get(1).hasSimpleType() && children.get(2).hasSimpleType();
	}

	/**
	 * This compiles the construct to an equivalent Xpath expression.
	 *
	 * @param ctx BindingContext this node shall be compiled in
	 * @return Object containing the Xpath fragment
	 */
	public XpathFragment translate(BindingContext ctx) {

	    // The 3 arguments
	    SchematronConstraintNodeXslt2 con = children.get(0);
	    SchematronConstraintNodeXslt2 thn = children.get(1);
	    SchematronConstraintNodeXslt2 els = children.get(2);

	    XpathFragment xptcon = con.translate(ctx);
	    XpathFragment xptthn = thn.translate(ctx);
	    XpathFragment xptels = els.translate(ctx);

	    // merge let variables of then- and else-part
	    String elsepart = xptthn.merge(xptels);
	    String thenpart = xptcon.merge(xptthn);

	    xptcon.fragment = "if (" + xptcon.fragment + ") then " + thenpart + " else " + elsepart;
	    xptcon.priority = 3;

	    /*
	     * Identify the type of the resulting xpath expression. If the type of then and
	     * else parts are the same, then it would be that type; otherwise the type is
	     * undetermined.
	     */
	    if (xptthn.type == xptels.type) {
		xptcon.type = xptthn.type;
	    } else {
		xptcon.type = XpathType.UNDEFINED;
	    }

	    return xptcon;
	}
    }

    /**
     * This class represents the 'let' construct. Being part of the Logic system Let
     * can be negated, which is realized by passing the negation down to the 'body'
     * expression. This is mananged from outside when the Let node is created.
     */
    public static class Let extends SchematronConstraintNodeXslt2 {

	// The variable declaration attached to the iterator
	OclNode.Declaration[] vardecls;
	// The binding context of the Let while translating it
	BindingContext letctx;

	/**
	 * Ctor
	 *
	 * @param schemaObject The schema object
	 * @param vardecl      OclNode.Declaration object
	 */
	public Let(AbstractSchematronSchema schemaObject, OclNode.Declaration[] vardecls) {
	    this.schemaObject = schemaObject;
	    this.vardecls = vardecls;
	}

	/**
	 * This method determines whether the Let binds the given variable declaration
	 * and if it does, which is the expression the variable is bound to.
	 *
	 * @param vardecl The variable Declaration object
	 * @return Node the variable is bound to or null
	 */
	public SchematronConstraintNodeXslt2 nodeVariableIsBoundTo(OclNode.Declaration vardecl) {
	    int idx = 0;
	    for (OclNode.Declaration dcl : vardecls) {
		if (dcl == vardecl)
		    return this.children.get(idx);
		idx++;
	    }
	    return null;
	}

	/**
	 * This compiles the node and its children to an Xpath expression fragment.
	 *
	 * <p>
	 * The object is translated in the given context and its ending position,
	 * supplemented by the defined binding variables bearing values given by
	 * expressions. The variables define the context for the compilation of the
	 * body, however this is not represented in the BindingContext, because the
	 * method of fetching the variables is completely different than with iterator
	 * variables.
	 *
	 * @param ctx BindingContext this node shall be compiled in
	 * @return Object containing the fragment and its operator priority
	 */
	public XpathFragment translate(BindingContext ctx) {

	    // Compile the body. Note that all treatment of the variable
	    // definitions of the 'let' construct itself is done way down in
	    // the Variable nodes whenever a variable of the 'let' is made
	    // use of.
	    SchematronConstraintNodeXslt2 body = children.get(children.size() - 1);
	    letctx = ctx;
	    XpathFragment xpt = body.translate(ctx);
	    return xpt;
	}
    }

    /** This is generated for unimplemented material. */
    public static class Error extends SchematronConstraintNodeXslt2 {

	/**
	 * Ctor
	 *
	 * @param schemaObject The schema object
	 */
	public Error(AbstractSchematronSchema schemaObject) {
	    this.schemaObject = schemaObject;
	    // Dummy - no action required
	}

	// Dummy, will never be called.
	public XpathFragment translate(BindingContext ctx) {
	    return null;
	}
    }

    /** This represents an error message comment. */
    public static class MessageComment extends SchematronConstraintNodeXslt2 {

	protected String name = null;

	/**
	 * Ctor
	 *
	 * @param schemaObject The schema object
	 */
	public MessageComment(AbstractSchematronSchema schemaObject, String name) {
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
	 * This method returns a vector or Schematron SQL value expressions in
	 * interpretation of a MessageComment object. The latter is created from the
	 * message text comment syntax contained in the constraints.
	 *
	 * @return Array of message arguments in FME value syntax
	 */
	public String[] compileAsMessageArgumentList() {

	    String[] arglist = new String[children.size()];
	    int i = 0;
	    for (SchematronConstraintNodeXslt2 arg : children) {
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
