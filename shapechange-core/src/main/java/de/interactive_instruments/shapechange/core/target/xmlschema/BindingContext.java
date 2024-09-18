package de.interactive_instruments.shapechange.core.target.xmlschema;

import java.util.ArrayList;

import de.interactive_instruments.shapechange.core.ocl.OclNode.Declaration;

/**
 * The primary information stored in this class is whether there is currently a
 * nodeset context at all - NONE if the expression is not a nodeset - and if the
 * context is currently identical to current() - ATCURRENT. All other contexts
 * are combined in OTHER.
 *
 * <p>
 * The vars part comes into living as soon as variables are encountered. They
 * are tracked together with the information how far they are up the stack.
 *
 * <p>
 * Also has a flag to indicate if an expression is to be translated to an XPath
 * fragment that will be contained within a predicate (...[...fragment...]).
 * This information is relevant for preventing the creation of a let variable to
 * store an expression for a byReference property that must be evaluated in the
 * expression context, but which would be evaluated in the overall context
 * (current()) due to being a let variable. Such a case could only be supported
 * with XSLT/XPath 2.0.
 */
public class BindingContext {

    public enum CtxState {
	NONE, ATCURRENT, OTHER
    }

    public CtxState state;

    /**
     * <code>true</code> if an expression would be evaluated to an XPath fragment
     * that will be contained within an XPath predicate; else <code>false</code>.
     */
    public boolean inPredicateExpression = false;

    /**
     * <code>true</code> if an expression would be evaluated to an XPath fragment
     * with the intent to access property metadata (via XML attribute @metadata)
     * from the resulting nodes; else <code>false</code>.
     */
    public boolean propertyMetadataAccess = false;

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
	if (vars != null) {
	    for (CtxElmt ce : vars) {
		copy.pushDeclaration(ce.vardecl);
		copy.vars.get(copy.vars.size() - 1).noOfSteps = ce.noOfSteps;
	    }
	}
	copy.inPredicateExpression = inPredicateExpression;
	return copy;
    }

    // Reset state
    public void setState(CtxState state) {
	this.state = state;
	this.vars = null;
    }

    public void setStateKeepingVariables(CtxState state) {
	this.state = state;
    }

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
