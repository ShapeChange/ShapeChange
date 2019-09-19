package de.interactive_instruments.ShapeChange.Target.XmlSchema;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class encapsulates an Xpath expression, which can be formulated using
 * variables defined using &lt;let> expressions of a Schematron &lt;rule>.
 *
 * @author Reinhard Erstling
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments <dot> de)
 *
 */
public class XpathFragment {

    /**
     * Defines operator priority. The higher the number, the higher the priority.
     * XPath 1.0 and 2.0 have different sets of operators. The following two tables
     * define operator precedence for both XPath 1.0 and 2.0.
     *
     * <p>
     * XPath 2.0 operator precedence order is defined at:
     * https://www.w3.org/TR/xpath20/#id-precedence-order
     *
     * <p>
     *
     * <table border="1">
     * <tbody>
     * <tr>
     * <th># of precedence (from lowest to highest)</th>
     * <th>Operator</th>
     * <th>Associativity</th>
     * </tr>
     * <tr>
     * <td>1</td>
     * <td>, (comma)</td>
     * <td>left-to-right</td>
     * </tr>
     * <tr>
     * <td>3</td>
     * <td>for, some, every, if</a></td>
     * <td>left-to-right</td>
     * </tr>
     * <tr>
     * <td>4</td>
     * <td>or</td>
     * <td>left-to-right</td>
     * </tr>
     * <tr>
     * <td>5</td>
     * <td>and</td>
     * <td>left-to-right</td>
     * </tr>
     * <tr>
     * <td>6</td>
     * <td>eq, ne, lt, le, gt, ge, =, !=, &lt;, &lt;=, &gt;, &gt;=, is, &lt;&lt;,
     * &gt;&gt;</td>
     * <td>left-to-right</td>
     * </tr>
     * <tr>
     * <td>7</td>
     * <td>to</td>
     * <td>left-to-right</td>
     * </tr>
     * <tr>
     * <td>8</td>
     * <td>+, -</td>
     * <td>left-to-right</td>
     * </tr>
     * <tr>
     * <td>9</td>
     * <td>*, div, idiv, mod</td>
     * <td>left-to-right</td>
     * </tr>
     * <tr>
     * <td>10</td>
     * <td>union, |</td>
     * <td>left-to-right</td>
     * </tr>
     * <tr>
     * <td>11</td>
     * <td>intersect, except</td>
     * <td>left-to-right</td>
     * </tr>
     * <tr>
     * <td>12</td>
     * <td>instance of</td>
     * <td>left-to-right</td>
     * </tr>
     * <tr>
     * <td>13</td>
     * <td>treat</td>
     * <td>left-to-right</td>
     * </tr>
     * <tr>
     * <td>14</td>
     * <td>castable</td>
     * <td>left-to-right</td>
     * </tr>
     * <tr>
     * <td>15</td>
     * <td>cast</td>
     * <td>left-to-right</td>
     * </tr>
     * <tr>
     * <td>16</td>
     * <td>-(unary), +(unary)</td>
     * <td>right-to-left</td>
     * </tr>
     * <tr>
     * <td>17</td>
     * <td>?, *(OccurrenceIndicator), +(OccurrenceIndicator)</td>
     * <td>left-to-right</td>
     * </tr>
     * <tr>
     * <td>18</td>
     * <td>Path expression: /, //</td>
     * <td>left-to-right</td>
     * </tr>
     * <tr>
     * <td>19</td>
     * <td>Filter/predicate expression: [ ]</td>
     * <td>left-to-right</td>
     * </tr>
     * <tr>
     * <td>20</td>
     * <td>(defined by ShapeChange): (bracketed expressions) - includes not(..),
     * generate-id(..), concat(..), true(), false(), boolean(..), string-length(..),
     * count(..), substring(..) - or (variable) identifier</td>
     * <td>left-to-right</td>
     * </tr>
     * </tbody>
     * </table>
     *
     * <p>
     * XPath 1.0 operator precedence order is defined as (from lowest to highest):
     *
     * <p>
     *
     * <table border="1">
     * <tbody>
     * <tr>
     * <th># of precedence (from lowest to highest)</th>
     * <th>Operator</th>
     * </tr>
     * <tr>
     * <td>1</td>
     * <td>or</td>
     * </tr>
     * <tr>
     * <td>2</td>
     * <td>and</td>
     * </tr>
     * <tr>
     * <td>3</td>
     * <td>Equality operators: =, !=</td>
     * </tr>
     * <tr>
     * <td>4</td>
     * <td>Other comparison operators: &lt;, &lt;=, &gt;, &gt;=</td>
     * </tr>
     * <tr>
     * <td>5</td>
     * <td>Infix +, -</td>
     * </tr>
     * <tr>
     * <td>6</td>
     * <td>*, div, mod</td>
     * </tr>
     * <tr>
     * <td>7</td>
     * <td>Prefix -</td>
     * </tr>
     * <tr>
     * <td>8</td>
     * <td>union, |</td>
     * </tr>
     * <tr>
     * <td>9</td>
     * <td>PathExpression: /, //</td>
     * </tr>
     * <tr>
     * <td>10</td>
     * <td>FilterExpression id[...]</td>
     * </tr>
     * <tr>
     * <td>11</td>
     * <td>(bracketed expressions) - includes not(..), generate-id(..), concat(..),
     * true(), false(), boolean(..), string-length(..), count(..), substring(..) -
     * or (variable) identifier</td>
     * </tr>
     * </tbody>
     * </table>
     */
    public int priority;

    public String fragment;
    public XpathType type;
    public TreeMap<String, String> lets = null;
    public BindingContext atEnd = new BindingContext(BindingContext.CtxState.NONE);

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

    /** Bracket the current expression */
    public void bracket() {
	fragment = "(" + fragment + ")";
	/*
	 * Bracketing an expression results in highest precedence/priority. The value of
	 * 20 for XPath 2.0 is also fine for XPath 1.0 expressions.
	 */
	priority = 20;
    }

    /**
     * Add another fragment performing let variable merging. The argument fragment
     * is destroyed. If binding contexts are given they are also merged.
     *
     * @return the merged fragment string
     */
    public String merge(XpathFragment xf) {

	if (xf.lets != null) {

	    // replace $ in front of let variables (not other variables!) with %
	    for (Map.Entry<String, String> ve : xf.lets.entrySet()) {
		String vn = ve.getKey();
		xf.replace("\\$" + vn, "%" + vn);
	    }

	    /*
	     * Now merge with this fragment; find new names for the let variables, using the
	     * lets of this fragment as a basis (for finding new names). Replace the old
	     * variable name with the new one.
	     */
	    for (Map.Entry<String, String> ve : xf.lets.entrySet()) {
		String vn = ve.getKey();
		String ex = ve.getValue();
		String vnew = findOrAdd(ex);
		xf.replace("%" + vn, "\\$" + vnew);
	    }
	}
	if (atEnd != null)
	    atEnd.merge(xf.atEnd);
	return xf.fragment;
    }

    /** Function to find or add a variable given the expression */
    public String findOrAdd(String ex) {

	if (lets == null) {
	    lets = new TreeMap<String, String>();
	}

	/*
	 * If ex is an existing let variable, just return it (and thus prevent a let
	 * expression with a variable as value).
	 */
	if (ex.startsWith("$") && lets.containsKey(ex.substring(1))) {
	    return ex.substring(1);
	}

	/*
	 * Determine if a let variable with the given expression already exists.
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
		    newkey = last.substring(0, last.length() - 1) + new String(bytes, "US-ASCII");
		} catch (UnsupportedEncodingException e) {
		    e.printStackTrace();
		}
	    }
	}
	lets.put(newkey, ex);
	return newkey;
    }

    /** Auxiliary function to replace variable names */
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
