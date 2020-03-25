/**
 * ShapeChange - processing application schemas for geographic information
 *
 * <p>This file is part of ShapeChange. ShapeChange takes a ISO 19109 Application Schema from a UML
 * model and translates it into a GML Application Schema or other implementation representations.
 *
 * <p>Additional information about the software can be found at http://shapechange.net/
 *
 * <p>(c) 2002-2019 interactive instruments GmbH, Bonn, Germany
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

import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.OclConstraint;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Ocl.OclNode;

/**
 * Creates a Schematron Schema based on xslt1 query binding, which may be
 * changed to the xslt2 query binding if necessary (e.g. to translate the
 * matches function).
 * <p>
 * NOTE: This Schematron Schema implementation may be deprecated in the near
 * future. It is recommended to switch to / use the
 * {@link SchematronSchemaXslt2} instead.
 * 
 * @author Reinhard Erstling
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class SchematronSchemaOld extends AbstractSchematronSchema implements MessageSource {

    /**
     * Ctor
     *
     * @param mdl               Model object
     * @param o                 Options object
     * @param r                 Result object
     * @param schemaPackage     the package that represents the schema that is being
     *                          encoded by the XmlSchema target
     * @param schemaXsdBaseName the name of the XSD document, for which this
     *                          Schematron schema will contain assertions
     * @param segmentation      true if schematron segmentation is enabled, else
     *                          false
     */
    public SchematronSchemaOld(Model mdl, Options o, ShapeChangeResult r, PackageInfo schemaPackage,
	    String schemaXsdBaseName, boolean segmentation) {
	super(mdl, o, r, schemaPackage, schemaXsdBaseName, segmentation);
    }

    @Override
    public void setQueryBinding(String qb) {
	addAttribute(document, root, "queryBinding", qb);
    }

    @Override
    public void addAssertion(ClassInfo ci, OclConstraint c) {

	// Drop null constraints and abstract classes
	if (c == null)
	    return;
	if (ci.isAbstract())
	    return;

	// Set environment for possible error messages during the constraint
	// translation process
	currentOclConstraintName = c.name();
	currentOclConstraintClass = c.contextClass();

	// Find out, whether the context is a suppressed class, and if so, the
	// object representing the class it stands for ...
	ClassInfo stci = null;
	if (ci.suppressed()) {
	    stci = ci.unsuppressedSupertype(trojanSuppressedType);
	    if (stci == null) {
		result.addError(this, 108, currentOclConstraintName, currentOclConstraintClass.name());
		return;
	    }
	}

	// Get hold of the syntax tree
	OclNode.Expression oclex = c.syntaxTree();

	// Derive the target Schematron syntax tree from the OCL tree,
	// quit if in error due to implementation restrictions
	SchematronConstraintNode scn = translateConstraintToSchematronNode(oclex, null, false);
	if (scn == null)
	    return;

	// Now, translate this to an Xpath fragment object, which is supposed
	// to contain all necessary information to generate the Rule.
	BindingContext ctx = new BindingContext(BindingContext.CtxState.ATCURRENT);
	XpathFragment xpath = scn.translate(ctx);

	// The generated Xpath syntax may still contain errors, which have
	// been detected during the compilation process and which are coded
	// in the result by means of a particular string pattern. Find out.
	if (checkErrorsInXpathFragment(xpath))
	    return;

	// We will have to create an assertion. Besides the test, which is
	// contained in the xpath object, we can output some explanatory text,
	// which we create from the name of the constraint and any OCL
	// comments, which we find in the constraint
	String text = c.name();
	String[] comments = c.comments();
	if (comments != null && comments.length > 0) {
	    text += ":";
	    for (String cl : comments) {
		text += " " + cl;
	    }
	}

	// The real work of creating the <assert> and the <rule> is done by a
	// more basic subroutine ...
	if (stci == null)
	    // Unsuppressed classes directly receive an assertion
	    addAssertion(ci, xpath, text);
	else {
	    if (!trojanSuppressedType)
		// strictUML interpretation: Attribute to the one target class
		addAssertion(stci, xpath, text);
	    else {
		// trojanType interpretation: Attribute to full concrete subtype
		// tree, except suppressed ones and constraint overrides ...
		LinkedList<ClassInfo> eval = new LinkedList<ClassInfo>();
		eval.add(stci);
		while (!eval.isEmpty()) {
		    ClassInfo cci = eval.removeFirst();
		    if (cci.suppressed())
			continue;
		    if (!cci.isAbstract() && !cci.hasConstraint(currentOclConstraintName))
			addAssertion(cci, xpath, text);
		    SortedSet<String> subids = cci.subtypes();
		    for (String subid : subids)
			eval.add(model.classById(subid));
		}
	    }
	}
    }

    /**
     * Add an assertion statement - that will result by translating the given OCL
     * constraint, which is defined for a property, to an XpathFragment object - and
     * output it as a Schematron &lt;assert&gt; element. Does not add an assertion
     * to abstract or suppressed classes.
     *
     * <p>
     * The rule context is a class, which is determined by parameter cib. This
     * supports cases in which the context class is a subtype of the class that owns
     * the property: b:subtype/a:property instead of a:owner/a:property.
     *
     * <p>
     * &lt;let&gt; elements are searched for identities and are merged including the
     * necessary name corrections in the text.
     *
     * @param c                              OCL constraint that shall be translated
     *                                       to the check of a Schematron assertion
     * @param cib                            ClassInfo object, which defines the
     *                                       rule context. Can be <code>null</code>,
     *                                       then the class that owns the property
     *                                       for which the OCL constraint is defined
     *                                       provides the rule context.
     * @param addToSubtypesInSelectedSchemas true to add the assertion to direct and
     *                                       indirect subtypes of cib (or the class
     *                                       that owns the property for which the
     *                                       OCL constraint is defined) that are in
     *                                       the schemas selected for processing
     */
    @Override
    public void addAssertionForPropertyConstraint(OclConstraint c, ClassInfo cib,
	    boolean addToSubtypesInSelectedSchemas) {

	// Get hold of the syntax tree
	OclNode.Expression oclex = c.syntaxTree();

	// Derive the target Schematron syntax tree from the OCL tree,
	// quit if in error due to implementation restrictions
	SchematronConstraintNode scn = translateConstraintToSchematronNode(oclex, null, false);
	if (scn == null)
	    return;

	// Now, translate this to an Xpath fragment object, which is supposed
	// to contain all necessary information to generate the Rule.
	BindingContext ctx = new BindingContext(BindingContext.CtxState.ATCURRENT);
	XpathFragment xpath = scn.translate(ctx);

	// The generated Xpath syntax may still contain errors, which have
	// been detected during the compilation process and which are coded
	// in the result by means of a particular string pattern. Find out.
	if (checkErrorsInXpathFragment(xpath))
	    return;

	// We will have to create an assertion. Besides the test, which is
	// contained in the xpath object, we can output some explanatory text,
	// which we create from the name of the constraint and any OCL
	// comments, which we find in the constraint
	String text = c.name();
	String[] comments = c.comments();
	if (comments != null && comments.length > 0) {
	    text += ":";
	    for (String cl : comments) {
		text += " " + cl;
	    }
	}

	PropertyInfo pi = (PropertyInfo) c.contextModelElmt();

	ClassInfo ci = cib != null ? cib : pi.inClass();

	/*
	 * Do not add assertion for abstract and suppressed classes.
	 *
	 * Also, if the class or the property is prohibited in the profile schema, do
	 * not create any checks for the property and this class.
	 */
	if (!ci.isAbstract() && !ci.suppressed()
		&& !(ci.matches("rule-xsd-all-propertyAssertion-ignoreProhibited")
			&& "true".equalsIgnoreCase(ci.taggedValue("prohibitedInProfile")))
		&& !(pi.matches("rule-xsd-all-propertyAssertion-ignoreProhibited")
			&& "true".equalsIgnoreCase(pi.taggedValue("prohibitedInProfile")))) {

	    registerNamespace(ci);
	    registerNamespace(pi.inClass());

	    /*
	     * Create an assertion. Find out if a rule for the required context already
	     * exists.
	     *
	     * TBD: add nil check to context?
	     */
	    String ruleContext = ci.qname();
	    RuleCreationStatus rulecs = ruleCreationStatusMap.get(ruleContext);
	    String asserttext;

	    if (rulecs == null) {

		// First time we encounter this context: Create a <rule>
		Element rule = document.createElementNS(Options.SCHEMATRON_NS, "rule");
		pattern.appendChild(rule);
		addAttribute(document, rule, "context", ruleContext);

		// Initialize the necessary DOM hooks and info
		rulecs = new RuleCreationStatus();
		rulecs.ruleElement = rule;

		// Initialize accumulation of the result fragments
		rulecs.lastPathStatus = xpath;
		asserttext = xpath.fragment;

		// Store away
		ruleCreationStatusMap.put(ruleContext, rulecs);

	    } else {

		// Second time: We need to merge the result fragment
		asserttext = rulecs.lastPathStatus.merge(xpath);
	    }

	    // Add the let-assignments, which are new for this assert
	    if (xpath.lets != null) {

		for (Entry<String, String> l : rulecs.lastPathStatus.lets.entrySet()) {

		    if (rulecs.letVarsAlreadyOutput.contains(l.getKey())) {
			continue;
		    }

		    Element let = document.createElementNS(Options.SCHEMATRON_NS, "let");
		    rulecs.ruleElement.insertBefore(let, rulecs.firstAssertElement);
		    addAttribute(document, let, "name", l.getKey());
		    addAttribute(document, let, "value", l.getValue());
		    rulecs.letVarsAlreadyOutput.add(l.getKey());
		}
	    }

	    // Add the assertion
	    Element ass = document.createElementNS(Options.SCHEMATRON_NS, "assert");
	    rulecs.ruleElement.appendChild(ass);
	    if (rulecs.firstAssertElement == null) {
		rulecs.firstAssertElement = ass;
	    }
	    addAttribute(document, ass, "test", asserttext);
	    ass.appendChild(document.createTextNode(text));

	    // Memorize we have output at least one rule
	    assertion = true;
	}

	if (addToSubtypesInSelectedSchemas) {

	    for (String subtypeId : ci.subtypes()) {

		ClassInfo subtype = model.classById(subtypeId);

		if (model.isInSelectedSchemas(subtype)) {

		    addAssertionForPropertyConstraint(c, subtype, true);

		    if (!schematronTitleExtended
			    && !subtype.pkg().targetNamespace().equalsIgnoreCase(ci.pkg().targetNamespace())) {
			schematronTitleHook
				.setTextContent(schematronTitleHook.getTextContent() + " and dependent schema(s)");
			schematronTitleExtended = true;
		    }
		}
	    }
	}
    }

    /**
     * This function recursively descends into an OclConstraint following the
     * OclNode structure. In doing so it generates an equivalent syntax tree which
     * is more in line with Xpath syntax and its use in the Schematron schema.
     *
     * @param ocl       OclNode of some level, initially called with
     *                  OclNode.Expression
     * @param enclosing Enclosing target construct, may be null
     * @param negate    Flag to indicate that a logical negation is to be pushed
     *                  downwards
     * @return Constructed SchematronConstraintNode tree. null if in error.
     */
    protected SchematronConstraintNode translateConstraintToSchematronNode(OclNode ocl,
	    SchematronConstraintNode enclosing, boolean negate) {

	SchematronConstraintNode scn = null;

	if (ocl instanceof OclNode.Expression) {
	    // The OCL Expression wrapper ...
	    OclNode.Expression ex = (OclNode.Expression) ocl;
	    scn = translateConstraintToSchematronNode(ex.expression, null, negate);
	    // Check on implementation restrictions
	    if (scn.containsError())
		return null;
	} else if (ocl instanceof OclNode.IterationCallExp) {
	    // IterationCallExp
	    OclNode.IterationCallExp iter = (OclNode.IterationCallExp) ocl;
	    scn = translateConstraintIterationToSchematronNode(iter, enclosing, negate);
	} else if (ocl instanceof OclNode.OperationCallExp) {
	    // OperationCallExp
	    OclNode.OperationCallExp oper = (OclNode.OperationCallExp) ocl;
	    scn = translateConstraintOperationToSchematronNode(oper, enclosing, negate);
	} else if (ocl instanceof OclNode.AttributeCallExp) {
	    // AttributeCallExp
	    OclNode.AttributeCallExp attr = (OclNode.AttributeCallExp) ocl;
	    scn = translateConstraintAttributeToSchematronNode(attr, enclosing, negate);
	} else if (ocl instanceof OclNode.LiteralExp) {
	    // LiteralExp
	    OclNode.LiteralExp lit = (OclNode.LiteralExp) ocl;
	    scn = translateConstraintLiteralToSchematronNode(lit, enclosing, negate);
	} else if (ocl instanceof OclNode.VariableExp) {
	    // VariableExp
	    OclNode.VariableExp var = (OclNode.VariableExp) ocl;
	    scn = new SchematronConstraintNode.Variable(this, var.declaration, negate);
	    if (enclosing != null)
		enclosing.addChild(scn);
	} else if (ocl instanceof OclNode.IfExp) {
	    // IfExp
	    OclNode.IfExp ifex = (OclNode.IfExp) ocl;
	    scn = translateConstraintIfExpToSchematronNode(ifex, enclosing, negate);
	} else if (ocl instanceof OclNode.LetExp) {
	    // LetExp
	    OclNode.LetExp letex = (OclNode.LetExp) ocl;
	    scn = translateConstraintLetExpToSchematronNode(letex, enclosing, negate);
	} else {
	    // Anything unknown
	    if (ocl == null) {
		result.addError(this, 101, "<NULL>", currentOclConstraintName, currentOclConstraintClass.name());
	    } else {
		String clname = ocl.getClass().getSimpleName();
		result.addError(this, 101, clname, currentOclConstraintName, currentOclConstraintClass.name());
	    }
	    scn = new SchematronConstraintNode.Error(this);
	}

	return scn;
    }

    /**
     * This function treats the implemented IterationCallExp objects in an OCL
     * expression. Doing so it generates intermediate code which is better suited
     * for Schematron generation than the original OCL constructs.
     *
     * @param iter      The IterationCallExp node to be processed
     * @param enclosing Enclosing target construct
     * @param negate    Flag to indicate that a logical negation is to be pushed
     *                  downwards
     * @return Constructed SchematronConstraintNode tree
     */
    protected SchematronConstraintNode translateConstraintIterationToSchematronNode(OclNode.IterationCallExp iter,
	    SchematronConstraintNode enclosing, boolean negate) {

	String opname = iter.selector.name;

	if (opname.equals("exists") || opname.equals("forAll")) {

	    // Note: forAll is mapped to exists
	    boolean isForAll = opname.equals("forAll");
	    // Create the Exists node
	    SchematronConstraintNode.Exists exists = new SchematronConstraintNode.Exists(this, iter.declarations[0],
		    negate ^ isForAll);
	    // Evaluate the operand and the body
	    exists.addChild(translateConstraintToSchematronNode(iter.object, null, false));
	    exists.addChild(translateConstraintToSchematronNode(iter.arguments[0], null, isForAll));
	    // Merge Exists node into enclosing logic if there is one
	    if (enclosing != null)
		enclosing.addChild(exists);
	    return exists;

	} else if (opname.equals("isUnique")) {

	    // Create the Unique node
	    SchematronConstraintNode.Unique unique = new SchematronConstraintNode.Unique(this, iter.declarations[0],
		    false);
	    // Evaluate the operand and the body
	    unique.addChild(translateConstraintToSchematronNode(iter.object, null, false));
	    unique.addChild(translateConstraintToSchematronNode(iter.arguments[0], null, false));
	    // Merge Unique node into enclosing logic if there is one
	    if (enclosing != null)
		enclosing.addChild(unique);
	    return unique;

	} else if (opname.equals("select")) {

	    // Create the Exists node
	    SchematronConstraintNode.Select select = new SchematronConstraintNode.Select(this, iter.declarations[0]);
	    // Evaluate the operand and the body
	    select.addChild(translateConstraintToSchematronNode(iter.object, null, false));
	    select.addChild(translateConstraintToSchematronNode(iter.arguments[0], null, false));
	    // Merge Exists node into enclosing logic if there is one
	    if (enclosing != null)
		enclosing.addChild(select);
	    return select;

	} else {
	    // Operation not supported
	    result.addError(this, 103, opname, currentOclConstraintName, currentOclConstraintClass.name());
	}
	return new SchematronConstraintNode.Error(this);
    }

    /**
     * This function treats the implemented OperationCallExp objects in an OCL
     * expression. Doing so it generates intermediate code which is better suited
     * for Schematron generation than the original OCL constructs.
     *
     * <p>
     * Particularly, all logical operations are collected in Logic objects of the
     * three flavors AND, OR and XOR, where AND and OR have as many as possible
     * children. NOT is pushed downwards by using De Morgan's rule.
     *
     * @param oper      The OperationCallExp node to be processed
     * @param enclosing Enclosing target construct
     * @param negate    Flag to indicate that a logical negation is to be pushed
     *                  downwards
     * @return Constructed SchematronConstraintNode tree
     */
    protected SchematronConstraintNode translateConstraintOperationToSchematronNode(OclNode.OperationCallExp oper,
	    SchematronConstraintNode enclosing, boolean negate) {

	String opname = oper.selector.name;

	if (opname.equals("implies")) {

	    // 'implies' is realized as ~a | b, so it is basically an OR
	    boolean and = false, neg1 = true, neg2 = false;
	    if (negate) {
		and = !and;
		neg1 = !neg1;
		neg2 = !neg2;
	    }
	    // Merge into enclosing logic? If not so, create a Logic node
	    boolean passencl = enclosing != null && enclosing.isAndOrLogic(and);
	    SchematronConstraintNode scn = passencl ? enclosing
		    : new SchematronConstraintNode.Logic(this, and ? SchematronConstraintNode.Logic.LogicType.AND
			    : SchematronConstraintNode.Logic.LogicType.OR);
	    // Recursion ...
	    translateConstraintToSchematronNode(oper.object, scn, neg1);
	    translateConstraintToSchematronNode(oper.arguments[0], scn, neg2);
	    // Merge into caller if necessary
	    if (!passencl && enclosing != null)
		enclosing.addChild(scn);
	    return scn;

	} else if (opname.equals("and") || opname.equals("or")) {

	    // 'and' or 'or', find out what we are ...
	    boolean and = opname.equals("and");
	    boolean neg = false;
	    if (negate) {
		and = !and;
		neg = !neg;
	    }
	    // Merge into enclosing logic? If not so create a Logic node
	    boolean passencl = enclosing != null && enclosing.isAndOrLogic(and);
	    SchematronConstraintNode scn = passencl ? enclosing
		    : new SchematronConstraintNode.Logic(this, and ? SchematronConstraintNode.Logic.LogicType.AND
			    : SchematronConstraintNode.Logic.LogicType.OR);
	    // Recursion ...
	    translateConstraintToSchematronNode(oper.object, scn, neg);
	    translateConstraintToSchematronNode(oper.arguments[0], scn, neg);
	    // Merge into caller if necessary
	    if (!passencl && enclosing != null)
		enclosing.addChild(scn);
	    return scn;

	} else if (opname.equals("xor")) {

	    // In Xpath this will be expressed by a != on boolean operands,
	    // negation transposes this to an = comparison (equivalence).
	    SchematronConstraintNode.Logic.LogicType xor = SchematronConstraintNode.Logic.LogicType.XOR;
	    if (negate)
		xor = SchematronConstraintNode.Logic.LogicType.EQV;
	    // Construct the node
	    SchematronConstraintNode fcn = new SchematronConstraintNode.Logic(this, xor);
	    fcn.addChild(translateConstraintToSchematronNode(oper.object, null, false));
	    fcn.addChild(translateConstraintToSchematronNode(oper.arguments[0], null, false));
	    // If there is an enclosing logic, add the created one
	    if (enclosing != null)
		enclosing.addChild(fcn);
	    return fcn;

	} else if (opname.equals("not")) {

	    // A 'not' just pushes a negation downwards
	    return translateConstraintToSchematronNode(oper.object, enclosing, !negate);

	} else if ("<>=<=".indexOf(opname) >= 0) {

	    // A relational operator absorbs a pushed down negation by
	    // inverting the relation.
	    final String[] relops = { "=", "<>", "<", "<=", ">", ">=" };
	    final String[] invops = { "<>", "=", ">=", ">", "<=", "<" };
	    String name = opname;
	    if (negate) {
		for (int i = 0; i < relops.length; i++) {
		    if (relops[i].equals(opname)) {
			name = invops[i];
			break;
		    }
		}
	    }
	    // Create the Comparison
	    SchematronConstraintNode.Comparison scn = new SchematronConstraintNode.Comparison(this, name);
	    // Compute and add the operands
	    scn.addChild(translateConstraintToSchematronNode(oper.object, null, false));
	    scn.addChild(translateConstraintToSchematronNode(oper.arguments[0], null, false));
	    // If there is an enclosing logic, add the created Comparison
	    if (enclosing != null)
		enclosing.addChild(scn);
	    return scn;

	} else if (opname.equals("notEmpty") || opname.equals("isEmpty")) {

	    // Create the Empty node
	    SchematronConstraintNode.Empty empty = new SchematronConstraintNode.Empty(this,
		    opname.equals("notEmpty") ^ negate);
	    // Evaluate the operand
	    empty.addChild(translateConstraintToSchematronNode(oper.object, null, false));
	    // Merge Empty node into enclosing logic if there is one
	    if (enclosing != null)
		enclosing.addChild(empty);
	    return empty;

	} else if (opname.equals("size")) {

	    // Create the Size node
	    boolean setoper = oper.selector.category == OclNode.PropertyCategory.SETOPER;
	    SchematronConstraintNode.Size size = new SchematronConstraintNode.Size(this, setoper);
	    // Evaluate the operand
	    size.addChild(translateConstraintToSchematronNode(oper.object, null, false));
	    return size;

	} else if (opname.equals("concat")) {

	    // Create the Concatenate node
	    SchematronConstraintNode.Concatenate concat = new SchematronConstraintNode.Concatenate(this);
	    // Evaluate the operands
	    concat.addChild(translateConstraintToSchematronNode(oper.object, null, false));
	    concat.addChild(translateConstraintToSchematronNode(oper.arguments[0], null, false));
	    return concat;

	} else if (opname.equals("substring")) {

	    // Create the Substring node
	    SchematronConstraintNode.Substring substr = new SchematronConstraintNode.Substring(this);
	    // Evaluate the operands
	    substr.addChild(translateConstraintToSchematronNode(oper.object, null, false));
	    substr.addChild(translateConstraintToSchematronNode(oper.arguments[0], null, false));
	    substr.addChild(translateConstraintToSchematronNode(oper.arguments[1], null, false));
	    return substr;

	} else if (opname.equals("matches")) {

	    // Check if this is configured
	    if (extensionFunctions.get("matches") == null)
		result.addError(this, 107, opname, currentOclConstraintName, currentOclConstraintClass.name());
	    else {
		// Create the Matches node
		SchematronConstraintNode.Matches matches = new SchematronConstraintNode.Matches(this);
		// Evaluate the operands
		matches.addChild(translateConstraintToSchematronNode(oper.object, null, false));
		matches.addChild(translateConstraintToSchematronNode(oper.arguments[0], null, false));
		// Merge Matches node into enclosing logic if there is one
		if (enclosing != null)
		    enclosing.addChild(matches);
		return matches;
	    }

	} else if (opname.equals("length") || opname.equals("area")) {

	    result.addError(this, 103, opname, currentOclConstraintName, currentOclConstraintClass.name());

	} else if (opname.equals("toUpper") || opname.equals("toLower")) {

	    result.addError(this, 103, opname, currentOclConstraintName, currentOclConstraintClass.name());

	} else if ("+-*/".indexOf(opname) >= 0) {

	    // Create the Arithmetic node
	    SchematronConstraintNode.Arithmetic arith = new SchematronConstraintNode.Arithmetic(this, opname);
	    // Evaluate the operands
	    arith.addChild(translateConstraintToSchematronNode(oper.object, null, false));
	    if (oper.arguments.length > 0)
		arith.addChild(translateConstraintToSchematronNode(oper.arguments[0], null, false));
	    return arith;

	} else if (opname.equals("oclIsKindOf") || opname.equals("oclIsTypeOf")) {

	    // isTypeOf is mapped as an option of KindOf ...
	    boolean exactType = opname.equals("oclIsTypeOf");
	    // Create the KindOf node
	    SchematronConstraintNode.KindOf kindOf = new SchematronConstraintNode.KindOf(this, exactType, negate);
	    // Evaluate the operand to be tested
	    kindOf.addChild(translateConstraintToSchematronNode(oper.object, null, false));
	    // Evaluate the type and make sure it is a class constant.
	    SchematronConstraintNode clex = translateConstraintToSchematronNode(oper.arguments[0], null, false);
	    boolean assumeError = true;
	    if (clex instanceof SchematronConstraintNode.Literal) {
		SchematronConstraintNode.Literal cllit = (SchematronConstraintNode.Literal) clex;
		OclNode.LiteralExp lex = cllit.literal;
		if (lex instanceof OclNode.ClassLiteralExp) {
		    OclNode.ClassLiteralExp lcl = (OclNode.ClassLiteralExp) lex;
		    ClassInfo ci = lcl.umlClass;
		    kindOf.setClass(ci);
		    assumeError = false;
		}
	    }
	    // The object must be the first child
	    kindOf.addChild(clex);

	    // Since for the time being functionality is restricted to class
	    // constants , we emit an error message if this not the case ...
	    if (assumeError) {
		result.addError(this, 104, currentOclConstraintName, currentOclConstraintClass.name(), "oclIsKindOf");
		SchematronConstraintNode err = new SchematronConstraintNode.Error(this);
		if (enclosing != null)
		    enclosing.addChild(err);
		return err;
	    }

	    // Merge KindOf node into enclosing logic if there is one
	    if (enclosing != null)
		enclosing.addChild(kindOf);
	    return kindOf;

	} else if (opname.equals("oclAsType")) {

	    // Create the KindOf node
	    SchematronConstraintNode.Cast cast = new SchematronConstraintNode.Cast(this);
	    // Evaluate the operand to be tested
	    cast.addChild(translateConstraintToSchematronNode(oper.object, null, false));
	    // Evaluate the type and make sure it is a class constant of
	    // geometry type.
	    SchematronConstraintNode clex = translateConstraintToSchematronNode(oper.arguments[0], null, false);
	    boolean assumeError = true;
	    if (clex instanceof SchematronConstraintNode.Literal) {
		SchematronConstraintNode.Literal cllit = (SchematronConstraintNode.Literal) clex;
		OclNode.LiteralExp lex = cllit.literal;
		if (lex instanceof OclNode.ClassLiteralExp) {
		    OclNode.ClassLiteralExp lcl = (OclNode.ClassLiteralExp) lex;
		    ClassInfo ci = lcl.umlClass;
		    cast.setClass(ci);
		    assumeError = false;
		}
	    }
	    // The object must be the first child
	    cast.addChild(clex);

	    // Since for the time being functionality is restricted to class
	    // constants, we emit an error message if this not the case ...
	    if (assumeError) {
		result.addError(this, 104, currentOclConstraintName, currentOclConstraintClass.name(), "oclAsType");
		SchematronConstraintNode err = new SchematronConstraintNode.Error(this);
		if (enclosing != null)
		    enclosing.addChild(err);
		return err;
	    }

	    // Merge Cast node into enclosing logic if there is one
	    if (enclosing != null)
		enclosing.addChild(cast);
	    return cast;

	} else if (opname.equals("allInstances")) {

	    // Evaluate the type and make sure it is a class constant with
	    // a database table attached
	    SchematronConstraintNode clex = translateConstraintToSchematronNode(oper.object, null, false);
	    boolean assumeError = true;
	    ClassInfo ci = null;
	    if (clex instanceof SchematronConstraintNode.Literal) {
		SchematronConstraintNode.Literal cllit = (SchematronConstraintNode.Literal) clex;
		OclNode.LiteralExp lex = cllit.literal;
		if (lex instanceof OclNode.ClassLiteralExp) {
		    OclNode.ClassLiteralExp lcl = (OclNode.ClassLiteralExp) lex;
		    ci = lcl.umlClass;
		    if (ci != null)
			assumeError = false;
		}
	    }
	    // Since for the time being functionality is restricted to constant
	    // types, we emit an error if this is not the case
	    if (assumeError) {
		result.addError(this, 105, currentOclConstraintName, currentOclConstraintClass.name());
		SchematronConstraintNode err = new SchematronConstraintNode.Error(this);
		if (enclosing != null)
		    enclosing.addChild(err);
		return err;
	    }
	    // All is ok. Create the AllInstances node
	    SchematronConstraintNode.AllInstances allinst = new SchematronConstraintNode.AllInstances(this, ci, negate);
	    // Merge AllInstances node into enclosing logic if there is one
	    if (enclosing != null)
		enclosing.addChild(allinst);
	    return allinst;

	} else if (opname.equals("propertyMetadata")) {

	    SchematronConstraintNode.PropertyMetadata pm = new SchematronConstraintNode.PropertyMetadata(this,
		    oper.object.dataType.metadataType, negate);

	    // Evaluate the operand to be tested
	    pm.addChild(translateConstraintToSchematronNode(oper.object, null, false));

	    // Merge PropertyMetadata node into enclosing logic if there is one
	    if (enclosing != null)
		enclosing.addChild(pm);
	    return pm;

	} else if (opname.startsWith("error_")) {

	    // Create the ErrorComment node
	    SchematronConstraintNode.MessageComment msgcom = new SchematronConstraintNode.MessageComment(this, opname);

	    // Evaluate the arguments
	    for (OclNode arg : oper.arguments) {
		msgcom.addChild(translateConstraintToSchematronNode(arg, null, false));
	    }
	    return msgcom;

	} else {
	    // Operation not found
	    result.addError(this, 103, opname, currentOclConstraintName, currentOclConstraintClass.name());
	}
	return new SchematronConstraintNode.Error(this);
    }

    /**
     * This method converts AttibuteCallExp objects into intermediary
     * SchematronConstraintsNodes in a first step to realize these in Xpath code.
     *
     * @param attr      The AttibuteCallExp object
     * @param enclosing If an enclosing Logic object is passed, the attribute must
     *                  be of type Boolean. Otherwise an error is generated.
     * @param negate    A pushed down negation will only be considered if the
     *                  attribute is of type Boolean.
     * @return Constructed SchematronConstraintNode tree
     */
    protected SchematronConstraintNode translateConstraintAttributeToSchematronNode(OclNode.AttributeCallExp attr,
	    SchematronConstraintNode enclosing, boolean negate) {

	// Have the name of the attribute available
	String attrname = attr.selector.name;

	// Find out if we are appending a nilReason property
	int absorptionType = 0;
	Info info = attr.selector.modelProperty;
	if (info instanceof PropertyInfo) {
	    PropertyInfo pi = (PropertyInfo) info;
	    boolean nilreason = pi.implementedByNilReason();
	    if (nilreason)
		absorptionType = 2;
	}

	// Translate the object part of the attribute
	SchematronConstraintNode objnode = translateConstraintToSchematronNode(attr.object, null, false);

	// Is this a selector based on some information source such as a
	// Variable, AllInstances or Select?
	if (objnode instanceof SchematronConstraintNode.Variable
		|| objnode instanceof SchematronConstraintNode.AllInstances
		|| objnode instanceof SchematronConstraintNode.Select
		|| objnode instanceof SchematronConstraintNode.PropertyMetadata) {

	    // Find out, whether this is a normal attribute or if it is
	    // absorbing the current attribute member (which includes nilReason
	    // treatment) ...
	    // Find the generating attribute if there is any
	    SchematronConstraintNode.Attribute atn = objnode.generatingAttribute();
	    if (atn != null && atn.isPropertyAbsorbing()) {
		// Set up this retrieved attribute component as absorbing
		// the new attribute component
		if (absorptionType == 0)
		    absorptionType = 1;
		atn.appendAbsorbedAttribute(absorptionType, attr);
		// Result is the object node itself
		return objnode;
	    }

	    // So, we are not treating an absorbing base attribute. Create the
	    // Attribute object, then ...
	    atn = new SchematronConstraintNode.Attribute(this, attr, negate);
	    // Attach information source node
	    atn.addChild(objnode);
	    // Embed into enclosing context
	    if (enclosing != null)
		enclosing.addChild(atn);
	    return atn;

	} else if (objnode instanceof SchematronConstraintNode.Attribute) {

	    // The object of an AttributeExp turns out to be an Attribute node,
	    // which means that we are just another qualification for the
	    // latter. So we just need to append the new AttributeExp to the
	    // existing Attribute node ...
	    SchematronConstraintNode.Attribute atn = (SchematronConstraintNode.Attribute) objnode;
	    // Different ways to append on findings concerning property
	    // absorption and nilReason treatment ...
	    if (atn.isPropertyAbsorbing()) {
		// Set up attribute component as absorbed
		if (absorptionType == 0)
		    absorptionType = 1;
		atn.appendAbsorbedAttribute(absorptionType, attr);
	    } else
		// Append info to Attribute node normally as another step
		atn.appendAttribute(attr);
	    // Embed into enclosing context
	    if (enclosing != null)
		enclosing.addChild(atn);
	    return atn;

	} else if (objnode instanceof SchematronConstraintNode.Cast) {

	    /*
	     * 2018-02-06 JE: Added cast as allowed part of a property expression. This
	     * supports cases in which property A with type T is followed by property B, but
	     * property B can only be found in a specific subtype of T, let's call it S.
	     * Then the value of B can be cast to S.
	     */

	    SchematronConstraintNode.Attribute atn = new SchematronConstraintNode.Attribute(this, attr, negate);

	    // Attach information source node
	    atn.addChild(objnode);

	    // Embed into enclosing context
	    if (enclosing != null) {
		enclosing.addChild(atn);
	    }

	    return atn;
	}

	// Not implemented attribute construct
	result.addError(this, 106, attrname, currentOclConstraintName, currentOclConstraintClass.name());

	return new SchematronConstraintNode.Error(this);
    }

    /**
     * This method is supposed to transform the OclNode Literals to an intermediary
     * node structure which is suited for PL/SQL generation.
     *
     * @param lit       The OclNode.Literal object
     * @param enclosing If an enclosing Logic object is passed, the literal must be
     *                  of type Boolean.
     * @param negate    A pushed down negation will only be considered if the
     *                  literal is of type Boolean.
     * @return Constructed SchematronConstraintNode tree
     */
    protected SchematronConstraintNode translateConstraintLiteralToSchematronNode(OclNode.LiteralExp lit,
	    SchematronConstraintNode enclosing, boolean negate) {

	// Reject class constants, which stand for suppressed classes
	if (lit instanceof OclNode.ClassLiteralExp) {
	    OclNode.ClassLiteralExp lcl = (OclNode.ClassLiteralExp) lit;
	    ClassInfo ci = lcl.umlClass;
	    if (ci.suppressed()) {
		result.addError(this, 109, currentOclConstraintName, currentOclConstraintClass.name(), ci.name());
		return new SchematronConstraintNode.Error(this);
	    }
	}

	// OK so far. Construct Literal Node (just wrapping the OclNode)
	SchematronConstraintNode.Literal litn = new SchematronConstraintNode.Literal(this, lit, negate);
	// Embed into enclosing context
	if (enclosing != null)
	    enclosing.addChild(litn);
	return litn;
    }

    /**
     * This method will transform an OclNode.IfExp to an intermediary node structure
     * suited for Schematron code generation.
     *
     * @param ifex      The OclNode.IfExp object
     * @param enclosing If an enclosing Logic object is passed, the type of the
     *                  IfExp must be Boolean.
     * @param negate    A pushed down negation will switch the then and else parts.
     * @return Constructed SchematronConstraintNode tree
     */
    protected SchematronConstraintNode translateConstraintIfExpToSchematronNode(OclNode.IfExp ifex,
	    SchematronConstraintNode enclosing, boolean negate) {

	// Create the FMEConstraintNode
	SchematronConstraintNode.IfThenElse ifthenelse = new SchematronConstraintNode.IfThenElse(this);

	// Translate the condition
	ifthenelse.addChild(translateConstraintToSchematronNode(ifex.condition, null, false));

	// We swap -then- and -else- if this is to be negated
	OclNode[] branch = { ifex.ifExpression, ifex.elseExpression };
	int is = negate ? 1 : 0;
	for (int i = 0; i < 2; i++) {
	    // Compile both branches
	    ifthenelse.addChild(translateConstraintToSchematronNode(branch[is], null, false));
	    is = 1 - is;
	}

	// If there is an enclosing logic, add the created IfThenElse
	if (enclosing != null)
	    enclosing.addChild(ifthenelse);

	return ifthenelse;
    }

    /**
     * This method will transform an OclNode.LetExp to an intermediary node
     * structure suited for Schematron code generation.
     *
     * @param letex     The OclNode.LetExp object
     * @param enclosing If an enclosing Logic object is passed, the type of the
     *                  LetExp must be Boolean.
     * @param negate    A pushed down negation will be passed to the body.
     * @return Constructed SchematronConstraintNode tree
     */
    protected SchematronConstraintNode translateConstraintLetExpToSchematronNode(OclNode.LetExp letex,
	    SchematronConstraintNode enclosing, boolean negate) {

	// Create the FMEConstraintNode
	SchematronConstraintNode.Let let = new SchematronConstraintNode.Let(this, letex.declarations);

	// Translate and add the variable values
	for (OclNode.Declaration dcl : letex.declarations) {
	    OclNode val = dcl.initialValue;
	    let.addChild(translateConstraintToSchematronNode(val, null, false));
	}

	// Translate and append the body. Negation will be handled there.
	let.addChild(translateConstraintToSchematronNode(letex.body, null, negate));

	// If there is an enclosing logic, add the created Let
	if (enclosing != null)
	    enclosing.addChild(let);

	return let;
    }

    /**
     * Auxiliary function to determine if the generated Xpath code contains any
     * explicit errors. If it does, provide messages accordingly.
     *
     * @param xpath The Xpath fragment to examine.
     * @return Flag, if true an error has been found
     */
    private boolean checkErrorsInXpathFragment(XpathFragment xpath) {
	// Concatenate all the generated stuff to ease pattern matching
	String allofit = "";
	if (xpath.lets != null)
	    for (String let : xpath.lets.values()) {
		allofit += let;
	    }
	allofit += xpath.fragment;
	// Match for the ERROR pattern and emit messages
	Pattern p = Pattern.compile("\\*\\*\\*ERROR\\[(.*?)\\]\\*\\*\\*");
	Matcher m = p.matcher(allofit);
	int count = 0;
	while (m.find()) {
	    count++;
	    String argsl = m.group(1);
	    String[] args = argsl.split(",");
	    int mnr = Integer.parseInt(args[0]);
	    if (args.length == 1)
		result.addError(this, mnr, currentOclConstraintName, currentOclConstraintClass.name());
	    else if (args.length >= 2)
		result.addError(this, mnr, currentOclConstraintName, currentOclConstraintClass.name(), args[1]);
	}
	return count > 0;
    }

    /**
     * This method returns messages belonging to the SchematronSchema object which
     * accompanies the XmlSchema target. The messages are retrieved by their message
     * number. The organization corresponds to the logic in module
     * ShapeChangeResult.
     *
     * @param mnr Message number
     * @return Message text, including $x$ substitution points.
     */
    public String message(int mnr) {
	// Get the message proper and return it with an identification prefixed
	String mess = messageText(mnr);
	if (mess == null)
	    return null;
	String prefix = "";
	if (mess.startsWith("??")) {
	    prefix = "??";
	    mess = mess.substring(2);
	}
	return prefix + "Schematron Target: " + mess;
    }

    /**
     * This is the message text provision proper. It returns a message for a number.
     *
     * @param mnr Message number
     * @return Message text or null
     */
    protected String messageText(int mnr) {
	switch (mnr) {
	case 101:
	    return "Failure to compile OCL constraint named \"$2$\" in class \"$3$\". Node class \"$1$\" not implemented.";
	case 102:
	    return "Failure to compile OCL constraint named \"$1$\" in class \"$2$\". Codelist named \"$3$\" being translated according to GML 3.3 rules, but \"codeList\" tagged value is not present";
	case 103:
	    return "Failure to compile OCL constraint named \"$2$\" in class \"$3$\". Operation \"$1$\" not implemented.";
	case 104:
	    return "Implementation restriction - in OCL constraint \"$1$\" in class \"$2$\": the argument to operator \"$3$\" must be a class constant.";
	case 105:
	    return "Implementation restriction - in OCL constraint \"$1$\" in class \"$2$\": the object of operator \"allInstances\" must be a class constant.";
	case 106:
	    return "Failure to compile OCL constraint named \"$2$\" in class \"$3$\". Attribute construct named \"$1$\" not implemented.";
	case 107:
	    return "Failure to compile OCL constraint named \"$2$\" in class \"$3$\". Schematron extension operation \"$1$\" is not properly configured.";
	case 108:
	    return "OCL constraint named \"$1$\" in suppressed class \"$2$\" cannot uniquely be attributed to any superclass.";
	case 109:
	    return "OCL constraint named \"$1$\" in class \"$2$\" contains a class constant refering to a suppressed class \"$3$\".";
	case 121:
	    return "Implementation restriction - in OCL constraint \"$1$\" in class \"$2$\" attribute access expressions in isUnique() bodies must not contain attributes with a cardinality > 1.";
	case 122:
	    return "Implementation restriction - in OCL constraint \"$1$\" in class \"$2$\" isUnique bodies must not contain expressions other than constants, identity or attribute access.";
	case 123:
	    return "Implementation restriction - in OCL constraint \"$1$\" in class \"$2$\" toUpper() or toLower() CharacterString operations are not supported by Xpath 1.0.";
	case 124:
	    return "Implementation restriction - in OCL constraint \"$1$\" in class \"$2$\" variable \"$3$\" used in nested iterator construct cannot be resolved due to limitations of Xpath 1.0.";
	case 125:
	    return "Implementation restriction - in OCL constraint \"$1$\" in class \"$2$\" 'current date' OCL extension cannot be expressed in Xpath 1.0.";
	case 126:
	    return "Implementation restriction - in OCL constraint \"$1$\" in class \"$2$\" comparison between structured non-object types not supported.";
	case 127:
	    return "??Implementation restriction - in OCL constraint \"$1$\" in class \"$2$\" nested byReference property \"$3$\" in iterator construct cannot be resolved due to limitations of Xpath 1.0 (variables inside XPath expression not supported).";
	case 128:
	    return "??Implementation restriction - in OCL constraint \"$1$\" in class \"$2$\" propertyMetadata() in iterator construct cannot be resolved due to limitations of Xpath 1.0 (variables inside XPath expression not supported).";
	case 131:
	    return "";
	}
	return null;
    }
}
