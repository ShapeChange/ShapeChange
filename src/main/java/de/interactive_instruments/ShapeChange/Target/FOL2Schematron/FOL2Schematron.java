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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.SerializerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.interactive_instruments.ShapeChange.AIXMSchemaInfos.AIXMSchemaInfo;
import de.interactive_instruments.ShapeChange.MapEntry;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.TargetIdentification;
import de.interactive_instruments.ShapeChange.FOL.AndOr;
import de.interactive_instruments.ShapeChange.FOL.AndOrType;
import de.interactive_instruments.ShapeChange.FOL.BinaryComparisonPredicate;
import de.interactive_instruments.ShapeChange.FOL.ClassLiteral;
import de.interactive_instruments.ShapeChange.FOL.EqualTo;
import de.interactive_instruments.ShapeChange.FOL.FolExpression;
import de.interactive_instruments.ShapeChange.FOL.HigherOrEqualTo;
import de.interactive_instruments.ShapeChange.FOL.HigherThan;
import de.interactive_instruments.ShapeChange.FOL.IsNull;
import de.interactive_instruments.ShapeChange.FOL.IsTypeOf;
import de.interactive_instruments.ShapeChange.FOL.Literal;
import de.interactive_instruments.ShapeChange.FOL.LowerOrEqualTo;
import de.interactive_instruments.ShapeChange.FOL.LowerThan;
import de.interactive_instruments.ShapeChange.FOL.Not;
import de.interactive_instruments.ShapeChange.FOL.Predicate;
import de.interactive_instruments.ShapeChange.FOL.PropertyCall;
import de.interactive_instruments.ShapeChange.FOL.Quantification;
import de.interactive_instruments.ShapeChange.FOL.SchemaCall;
import de.interactive_instruments.ShapeChange.FOL.Variable;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Model.FolConstraint;
import de.interactive_instruments.ShapeChange.Target.Target;
import de.interactive_instruments.ShapeChange.Target.FOL2Schematron.FolSchematronNode.AttributeNode;
import de.interactive_instruments.ShapeChange.Target.FOL2Schematron.FolSchematronNode.VariableNode;

/**
 * Translates the First Order Logic expressions defined for a given schema into
 * a set of Schematron rules.
 * 
 * @author Johannes Echterhoff
 *
 */
public class FOL2Schematron implements Target, MessageSource {

	protected ShapeChangeResult result;
	protected PackageInfo schema = null;
	protected Options options;
	protected Model model;
	protected boolean diagnosticsOnly = false;

	private String outputDirectory;

	private boolean printed = false;
	private boolean atLeastOneSchematronRuleExists = false;

	private Document document;
	private Element pattern;
	private Element root;

	protected String currentConstraintName = null;
	protected ClassInfo currentConstraintClass = null;

	private String classname = this.getClass().getSimpleName();

	/**
	 * Variable index to be used for counting 'for's in XPath expressions
	 */
	private int varIndex = 0;

	public static class RuleCreationStatus {
		public FolSchematronNode.XpathFragment lastPathStatus;
		public Element ruleElement;
		public Element firstAssertElement = null;
		HashSet<String> letVarsAlreadyOutput = new HashSet<String>();
	}

	HashMap<String, RuleCreationStatus> ruleCreationStatusMap = new HashMap<String, RuleCreationStatus>();

	public static class ExtensionFunctionTemplate {
		public String nsPrefix;
		public String namespace;
		public String function;

		public ExtensionFunctionTemplate(String nsp, String ns, String fct) {
			nsPrefix = nsp;
			namespace = ns;
			function = fct;
		}
	}

	HashMap<String, ExtensionFunctionTemplate> extensionFunctions = new HashMap<String, ExtensionFunctionTemplate>();

	private HashSet<String> namespaces = new HashSet<String>();

	// Prefix and postfix for xlink:href references
	protected String alpha = "#";
	protected String beta = "";

	protected Map<String, FolSchematronNode.VariableNode> varNodesByVarName = new HashMap<String, FolSchematronNode.VariableNode>();

	/**
	 * The XML namespace prefix to use for elements of the schema that is being
	 * processed.
	 */
	protected String xmlns;

	@Override
	public void initialise(PackageInfo schemaPi, Model m, Options o,
			ShapeChangeResult r, boolean diagOnly)
			throws ShapeChangeAbortException {

		this.schema = schemaPi;
		this.options = o;
		this.model = m;
		this.result = r;
		this.diagnosticsOnly = diagOnly;

		result.addDebug(this, 1, schemaPi.name());

		this.outputDirectory = options.parameter(this.getClass().getName(),
				"outputDirectory");
		if (outputDirectory == null)
			outputDirectory = options.parameter("outputDirectory");
		if (outputDirectory == null)
			outputDirectory = options.parameter(".");

		// create output directory, if necessary
		if (!this.diagnosticsOnly) {

			// Check whether we can use the given output directory
			File outputDirectoryFile = new File(outputDirectory);
			boolean exi = outputDirectoryFile.exists();
			if (!exi) {
				outputDirectoryFile.mkdirs();
				exi = outputDirectoryFile.exists();
			}
			boolean dir = outputDirectoryFile.isDirectory();
			boolean wrt = outputDirectoryFile.canWrite();
			boolean rea = outputDirectoryFile.canRead();
			if (!exi || !dir || !wrt || !rea) {
				result.addFatalError(this, 3, outputDirectory);
				return;
			}
		}

		// Get prefix and postfix of xlink:href references
		String s = options.parameter(classname, "schematronXlinkHrefPrefix");
		if (s != null)
			alpha = s;
		s = options.parameter(classname, "schematronXlinkHrefPostfix");
		if (s != null)
			beta = s;

		this.xmlns = schemaPi.xmlns();

		// Create the Schematron document
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			document = db.newDocument();
		} catch (ParserConfigurationException e) {
			result.addFatalError(null, 2);
			String msg = e.getMessage();
			if (msg != null) {
				result.addFatalError(msg);
			}
			e.printStackTrace(System.err);
			System.exit(1);
		} catch (Exception e) {
			result.addFatalError(e.getMessage());
			e.printStackTrace(System.err);
			System.exit(1);
		}

		// Create a root and attach the Schematron and application schema
		// namespace definitions.
		root = document.createElementNS(Options.SCHEMATRON_NS, "schema");
		document.appendChild(root);
		addAttribute(document, root, "xmlns:sch", Options.SCHEMATRON_NS);

		addAttribute(document, root, "queryBinding", "xslt2");

		// Add a title element to document the schema the rules belong to
		Element e1 = document.createElementNS(Options.SCHEMATRON_NS, "title");
		root.appendChild(e1);
		e1.appendChild(document
				.createTextNode("Schematron constraints for schema '"
						+ schema.name() + "'"));

		// Add a namespace declaration for Schematron
		e1 = document.createElementNS(Options.SCHEMATRON_NS, "ns");
		root.appendChild(e1);
		namespaces.add("sch");
		addAttribute(document, e1, "prefix", "sch");
		addAttribute(document, e1, "uri", Options.SCHEMATRON_NS);

		// Add a namespace declaration for the package
		e1 = document.createElementNS(Options.SCHEMATRON_NS, "ns");
		root.appendChild(e1);
		namespaces.add(schema.xmlns());
		addAttribute(document, e1, "prefix", schema.xmlns());
		addAttribute(document, e1, "uri", schema.targetNamespace());

		// Finally add the <pattern> element. It is to hold all the rules
		// to be generated
		pattern = document.createElementNS(Options.SCHEMATRON_NS, "pattern");
		root.appendChild(pattern);

		// reset processed flags on all classes in the schema
		for (Iterator<ClassInfo> k = model.classes(schema).iterator(); k
				.hasNext();) {
			ClassInfo ci = k.next();
			ci.processed(getTargetID(), false);
		}
	}

	@Override
	public void process(ClassInfo ci) {

		if (ci.processed(getTargetID()))
			return;

		result.addDebug(this, 2, ci.name());

		List<Constraint> cons = ci.constraints();

		if (cons != null && !cons.isEmpty()) {

			/*
			 * Ignore AIXM <<extension>> types that have constraints (inherited
			 * from supertypes)
			 */
			if (ci.category() == Options.AIXMEXTENSION) {

				result.addDebug(this, 4, ci.name());
				ci.processed(getTargetID(), true);
				return;
			}

			// sort the constraints by name
			Collections.sort(cons, new Comparator<Constraint>() {
				@Override
				public int compare(Constraint o1, Constraint o2) {
					return o1.name().compareTo(o2.name());
				}
			});

			// process the constraints
			for (Constraint con : cons) {

				if (con instanceof FolConstraint) {

					FolConstraint folCon = (FolConstraint) con;

					/*
					 * translate first order logic expression - if it exists -
					 * to schematron
					 * 
					 * NOTE: the FolExpression of an FolConstraint can be null
					 * if parsing the constraint text did not succeed.
					 */
					result.addInfo("Class '"
							+ ci.name()
							+ "' has FOL constraint '"
							+ con.name()
							+ "' with FOL expression: "
							+ (folCon.folExpression() == null ? "<null>"
									: folCon.folExpression()));

					if (folCon.folExpression() != null) {

						this.reset();

						addAssertion(ci, folCon);
					}

				} else {

					// fine - we ignore other types of constraints
				}
			}
		}

		ci.processed(getTargetID(), true);
	}

	/**
	 * Add another constraint and translate it into a Schematron &lt;assert>,
	 * which is subsequently appended to the Schematron document within the
	 * proper &lt;rule> context.
	 * 
	 * @param ci
	 *            ClassInfo object, which is context to the constraint.
	 * @param c
	 *            FOL constraint.
	 */
	protected void addAssertion(ClassInfo ci, FolConstraint c) {

		// Drop null constraints and abstract classes
		if (c == null)
			return;
		if (ci.isAbstract())
			return;

		// Get hold of the FOL expression
		FolExpression folExpr = c.folExpression();

		/*
		 * There may have been issues parsing the source text of the FOL
		 * constraint to an FOL expression during common model postprocessing.
		 * The current approach of mostly having read-only support in the Model
		 * interfaces does not allow replacing FolConstraints with pure
		 * TextConstraints. Thus the fallback in case of a parsing issue in a
		 * FolConstraint is to leave the FOL expression empty. This is what we
		 * check here.
		 */
		if (folExpr == null)
			return;

		// Set environment for possible error messages during the constraint
		// translation process
		currentConstraintName = c.name();
		currentConstraintClass = ci;

		// Derive the target Schematron syntax tree from the FOL expression,
		// quit if in error due to implementation restrictions
		FolSchematronNode scn = translateConstraint(folExpr, null);
		if (scn == null)
			return;

		// Now, translate this to an Xpath fragment object, which is supposed
		// to contain all necessary information to generate the Rule.
		FolSchematronNode.BindingContext ctx = new FolSchematronNode.BindingContext(
				FolSchematronNode.BindingContext.CtxState.ATCURRENT);
		FolSchematronNode.XpathFragment xpath = scn.translate(ctx);

		// The generated Xpath syntax may still contain errors, which have
		// been detected during the compilation process and which are coded
		// in the result by means of a particular string pattern. Find out.
		if (checkErrorsInXpathFragment(xpath))
			return;

		// We will have to create an assertion. Besides the test, which is
		// contained in the xpath object, we can output some explanatory text,
		// which we create from the name of the constraint and any OCL
		// comments, which we find in the constraint

		String text = "";
		String[] comments = c.comments();

		if (comments != null && comments.length > 0) {
			for (String cl : comments) {
				if (cl.startsWith("/*"))
					cl = cl.substring(2);
				if (cl.endsWith("*/"))
					cl = cl.substring(0, cl.length() - 2);
				text += " " + cl;
			}
		}

		text = text.trim();

		addAssertion(ci, xpath, text, currentConstraintName);
	}

	/**
	 * Add an assertion statement embodied in an XpathFragment object and output
	 * it as a Schematron &lt;assert> element, which is contained in a proper
	 * &lt;rule> context. &lt;let> elements are searched for identities and are
	 * merged including the necessary name corrections in the text.
	 * 
	 * @param ci
	 *            ClassInfo object, which is context to the constraint.
	 * @param xpath
	 *            Assertion embodied in an XpathFragment object.
	 * @param text
	 *            Explanatory text concerning the assertion
	 * @param id
	 * @param
	 */
	protected void addAssertion(ClassInfo ci,
			FolSchematronNode.XpathFragment xpath, String text, String id) {

		// Drop abstract classes
		if (ci.isAbstract())
			return;

		// We will have to create an assertion. Find out about the rule, where
		// the new assertion will go. This info is kept in RuleCreationStatus
		// object for each feature type name ...
		String ftn = getAndRegisterXmlName(ci);
		RuleCreationStatus rulecs = ruleCreationStatusMap.get(ftn);
		String asserttext;
		if (rulecs == null) {
			// First time we encounter this feature type: Create a <rule>
			Element rule = document.createElementNS(Options.SCHEMATRON_NS,
					"rule");
			pattern.appendChild(rule);
			addAttribute(document, rule, "context", ftn);
			// Initialize the necessary DOM hooks and info
			rulecs = new RuleCreationStatus();
			rulecs.ruleElement = rule;
			// Initialize accumulation of the result fragments
			rulecs.lastPathStatus = xpath;
			asserttext = xpath.fragment;
			// Store away
			ruleCreationStatusMap.put(ftn, rulecs);
		} else {
			// Second time: We need to merge the result fragment
			asserttext = rulecs.lastPathStatus.merge(xpath);
		}

		// Add the assertion
		Element ass = document.createElementNS(Options.SCHEMATRON_NS, "assert");
		rulecs.ruleElement.appendChild(ass);
		if (rulecs.firstAssertElement == null)
			rulecs.firstAssertElement = ass;
		addAttribute(document, ass, "id", id);
		addAttribute(document, ass, "test", asserttext);
		ass.appendChild(document.createTextNode(text));

		// Memorize we have output at least one rule
		atLeastOneSchematronRuleExists = true;
	}

	/**
	 * Auxiliary function to determine if the generated Xpath code contains any
	 * explicit errors. If it does, provide messages accordingly.
	 * 
	 * @param xpath
	 *            The Xpath fragment to examine.
	 * @return Flag, if true an error has been found
	 */
	private boolean checkErrorsInXpathFragment(
			FolSchematronNode.XpathFragment xpath) {

		// Concatenate all the generated stuff to ease pattern matching
		String allofit = "";
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
				result.addError(this, mnr, currentConstraintName,
						currentConstraintClass.name());
			else if (args.length >= 2)
				result.addError(this, mnr, currentConstraintName,
						currentConstraintClass.name(), args[1]);
		}
		return count > 0;
	}

	/**
	 * Auxiliary method to find out the full, namespace adorned name of a
	 * property from the model. As a side effect the method makes the namespace
	 * also known to the Schematron schema, appending another &lt;ns> element if
	 * necessary.
	 * 
	 * @param pi
	 *            PropertyInfo object
	 * @return Element name of property
	 */
	public String getAndRegisterXmlName(PropertyInfo pi) {

		String nspref = null;
		AIXMSchemaInfo aixmSchemaInfo = null;

		if (options.isAIXM()) {
			if (options.getAIXMSchemaInfos() != null) {
				aixmSchemaInfo = options.getAIXMSchemaInfos().get(pi.id());
				if (aixmSchemaInfo == null) {
					result.addWarning(this, 6, pi.fullNameInSchema());
				}
			} else {
				result.addWarning(this, 5);
			}
		}

		if (aixmSchemaInfo == null) {
			nspref = pi.inClass().pkg().xmlns();
			registerNamespace(nspref, pi.inClass());
		} else {
			nspref = aixmSchemaInfo.xmlns();
			registerNamespace(nspref, aixmSchemaInfo.targetNamespace());
		}

		String proper = nspref + ":" + pi.name();

		return proper;
	}

	/**
	 * <p>
	 * Auxiliary method to find out the full, namespace adorned name of a class
	 * from the the mapping or the model. As a side effect the method makes the
	 * namespace also known to the Schematron schema, appending another &lt;ns>
	 * element if necessary.
	 * </p>
	 * <p>
	 * The method considers the mapping in first priority. If the class turns
	 * out to map to a basic type, <i>null</i> is returned instead of a name.
	 * </p>
	 * 
	 * @param pi
	 *            ClassInfo object
	 * @return Element name of class
	 */
	public String getAndRegisterXmlName(ClassInfo ci) {

		String nspref = null;
		String fulnam = null;

		MapEntry me = ci.options().elementMapEntry(ci.name(),
				ci.encodingRule("xsd"));

		if (me != null) {

			if (me.p1 == null || me.p1.length() == 0)
				return null;

			fulnam = me.p1;
			String[] parts = fulnam.split(":");

			if (parts.length > 1)
				nspref = parts[0];

			registerNamespace(nspref);

		} else {

			AIXMSchemaInfo aixmSchemaInfo = null;

			if (options.isAIXM()) {
				if (options.getAIXMSchemaInfos() != null) {
					aixmSchemaInfo = options.getAIXMSchemaInfos().get(ci.id());
					if (aixmSchemaInfo == null) {
						result.addWarning(this, 6, ci.fullNameInSchema());
					}
				} else {
					result.addWarning(this, 5);
				}
			}

			if (aixmSchemaInfo == null) {
				nspref = ci.pkg().xmlns();
				registerNamespace(nspref, ci);
			} else {
				nspref = aixmSchemaInfo.xmlns();
				registerNamespace(nspref, aixmSchemaInfo.targetNamespace());
			}

			fulnam = nspref + ":" + ci.name();
		}
		return fulnam;
	}

	public String getAndRegisterXmlns(ClassInfo ci) {

		String xmlns;

		AIXMSchemaInfo aixmSchemaInfo = null;

		if (options.isAIXM()) {
			if (options.getAIXMSchemaInfos() != null) {
				aixmSchemaInfo = options.getAIXMSchemaInfos().get(ci.id());
				if (aixmSchemaInfo == null) {
					result.addWarning(this, 6, ci.fullNameInSchema());
				}
			} else {
				result.addWarning(this, 5);
			}
		}

		if (aixmSchemaInfo == null) {
			xmlns = ci.pkg().xmlns();
			registerNamespace(xmlns);
		} else {
			xmlns = aixmSchemaInfo.xmlns();
			registerNamespace(xmlns, aixmSchemaInfo.targetNamespace());
		}

		return xmlns;
	}

	/**
	 * This auxiliary method registers a namespace (prefix and namespace proper)
	 * with the Schematron schema. It adds another &lt;ns> element when the
	 * namespace occurs the first time.
	 * 
	 * @param xmlns
	 *            Namespace prefix
	 * @param ns
	 *            Namespace proper
	 */
	public void registerNamespace(String xmlns, String ns) {
		if (!namespaces.contains(xmlns)) {
			Element e = document.createElementNS(Options.SCHEMATRON_NS, "ns");
			addAttribute(document, e, "prefix", xmlns);
			if (ns == null)
				ns = "FIXME";
			addAttribute(document, e, "uri", ns);
			root.insertBefore(e, pattern);
			namespaces.add(xmlns);
		}
	}

	/**
	 * This auxiliary method registers a namespace prefix with the Schematron
	 * schema. It adds another &lt;ns> element when the namespace occurs the
	 * first time
	 * 
	 * @param xmlns
	 *            Namespace prefix
	 */
	public void registerNamespace(String xmlns) {
		if (!namespaces.contains(xmlns)) {
			String ns = options.fullNamespace(xmlns);
			registerNamespace(xmlns, ns);
		}
	}

	/**
	 * This special variant of the method above considers the class object to
	 * determine the full namespace uri. If this is not successful it resorts to
	 * the method above, which uses the namespace mapping mechanism in the
	 * Options object.
	 * 
	 * @param xmlns
	 *            Namespace prefix
	 * @param ci
	 *            ClassInfo object to fetch the namespace uri from
	 */
	public void registerNamespace(String xmlns, ClassInfo ci) {

		if (!namespaces.contains(xmlns)) {

			String ns = ci.pkg().targetNamespace();
			if (ns == null || ns.length() == 0)
				registerNamespace(xmlns);
			else {
				registerNamespace(xmlns, ns);
			}
		}
	}

	/**
	 * <p>
	 * This function recursively descends into an FolConstraint following the
	 * FOL expression structure. In doing so it generates an equivalent syntax
	 * tree which is more in line with Xpath syntax and its use in the
	 * Schematron schema.
	 * </p>
	 * 
	 * @param folExpr
	 *            FolExpression of some level
	 * @param enclosing
	 *            Enclosing target construct, may be null
	 * @return Constructed SchematronNode tree. null if in error.
	 */
	protected FolSchematronNode translateConstraint(FolExpression folExpr,
			FolSchematronNode enclosing) {

		// TBD: what if folExpr is null?

		FolSchematronNode scn = null;

		if (folExpr instanceof Quantification) {

			Quantification q = (Quantification) folExpr;
			scn = translateQuantification(q, enclosing);

		} else if (folExpr instanceof SchemaCall) {

			SchemaCall sc = (SchemaCall) folExpr;
			scn = translateSchemaCall(sc, enclosing, false);

		} else if (folExpr instanceof Literal) {

			Literal lit = (Literal) folExpr;
			scn = translateLiteral(lit, enclosing);

		} else if (folExpr instanceof Variable) {

			scn = translateVariable((Variable) folExpr, enclosing);

		} else if (folExpr instanceof Not) {

			scn = translateNot((Not) folExpr, enclosing);

		} else if (folExpr instanceof EqualTo) {

			scn = translateComparisonOperation(
					(BinaryComparisonPredicate) folExpr, "=", enclosing);

		} else if (folExpr instanceof HigherOrEqualTo) {

			scn = translateComparisonOperation(
					(BinaryComparisonPredicate) folExpr, ">=", enclosing);

		} else if (folExpr instanceof HigherThan) {

			scn = translateComparisonOperation(
					(BinaryComparisonPredicate) folExpr, ">", enclosing);

		} else if (folExpr instanceof LowerOrEqualTo) {

			scn = translateComparisonOperation(
					(BinaryComparisonPredicate) folExpr, "<=", enclosing);

		} else if (folExpr instanceof LowerThan) {

			scn = translateComparisonOperation(
					(BinaryComparisonPredicate) folExpr, "<", enclosing);

		} else if (folExpr instanceof AndOr) {

			scn = translateAndOr((AndOr) folExpr, enclosing);

		} else if (folExpr instanceof IsNull) {

			scn = translateIsNull((IsNull) folExpr, enclosing);

		} else if (folExpr instanceof IsTypeOf) {

			scn = translateIsTypeOf((IsTypeOf) folExpr, enclosing);

		} else {
			// Anything unknown
			if (folExpr == null) {
				result.addError(this, 101, "<NULL>", currentConstraintName,
						currentConstraintClass.name());
			} else {
				String clname = folExpr.getClass().getSimpleName();
				result.addError(this, 101, clname, currentConstraintName,
						currentConstraintClass.name());
			}
			scn = new FolSchematronNode.Error(this);
		}

		// Check on implementation restrictions
		// if (scn.containsError())
		// return null;

		return scn;
	}

	private FolSchematronNode translateIsTypeOf(IsTypeOf folExpr,
			FolSchematronNode enclosing) {

		// FOL IsTypeOf is mapped like the OCL IsKindOf ...

		// Create the KindOf node
		FolSchematronNode.IsTypeOfNode typeOf = new FolSchematronNode.IsTypeOfNode(
				this);

		// Evaluate the operand to be tested
		if (folExpr.getExprLeft() instanceof Variable) {

			Variable var = (Variable) folExpr.getExprLeft();
			typeOf.setVariable(var);

		} else {

			result.addError(this, 7);
			FolSchematronNode err = new FolSchematronNode.Error(this);
			if (enclosing != null)
				enclosing.addChild(err);
			return err;
		}

		// Evaluate the type and make sure it is a class constant.
		FolSchematronNode clex = translateConstraint(folExpr.getExprRight(),
				null);

		boolean assumeError = true;

		if (clex instanceof FolSchematronNode.LiteralNode) {

			FolSchematronNode.LiteralNode cllit = (FolSchematronNode.LiteralNode) clex;
			Literal l = cllit.literal;

			if (l instanceof ClassLiteral) {

				ClassLiteral lcl = (ClassLiteral) l;
				ClassInfo ci = lcl.getSchemaElement();
				typeOf.setClass(ci);
				assumeError = false;
			}
		}

		// Since for the time being functionality is restricted to class
		// constants , we emit an error message if this not the case ...
		if (assumeError) {
			result.addError(this, 104, currentConstraintName,
					currentConstraintClass.name(), "IsTypeOf");
			FolSchematronNode err = new FolSchematronNode.Error(this);
			if (enclosing != null)
				enclosing.addChild(err);
			return err;
		}

		// Merge IsTypeOfNode into enclosing logic if there is one
		if (enclosing != null)
			enclosing.addChild(typeOf);

		return typeOf;
	}

	private FolSchematronNode translateIsNull(IsNull fol,
			FolSchematronNode enclosing) {

		// Create the IsNull node
		FolSchematronNode.IsNullNode inn = new FolSchematronNode.IsNullNode(
				this);

		// Evaluate the operand
		inn.addChild(translateConstraint(fol.getExpr(), null));

		if (enclosing != null)
			enclosing.addChild(inn);

		return inn;
	}

	private VariableNode translateVariable(Variable var,
			FolSchematronNode enclosing) {

		// TODO does 'enclosing' make sense for a variable?

		if (varNodesByVarName.containsKey(var.getName())) {

			return varNodesByVarName.get(var.getName());

		} else {

			FolSchematronNode.VariableNode vn = new FolSchematronNode.VariableNode(
					this, var);

			this.varNodesByVarName.put(var.getName(), vn);

			/*
			 * now translate the variable in the outer scope, if that exists and
			 * has not already been translated
			 */
			if (var.getNextOuterScope() != null
					&& !this.varNodesByVarName.containsKey(var
							.getNextOuterScope().getName())) {
				translateVariable(var.getNextOuterScope(), enclosing);
			}

			if (var.getValue() != null) {

				FolSchematronNode varValue = translateSchemaCall(
						var.getValue(), null, true);

				if (varValue == null) {
					// should only be the case for the 'self' variable
				} else {
					vn.setValue(varValue);
				}
			}

			if (enclosing != null)
				enclosing.addChild(vn);

			return vn;
		}
	}

	private FolSchematronNode translateAndOr(AndOr fol,
			FolSchematronNode enclosing) {

		List<Predicate> predicates = fol.getPredicateList();

		// 'and' or 'or', find out what we are ...
		boolean and = fol.getType().equals(AndOrType.and);

		// Merge into enclosing logic? If not so create a Logic node
		boolean passencl = enclosing != null && enclosing.isAndOrLogic(and);

		FolSchematronNode scn = passencl ? enclosing
				: new FolSchematronNode.Logic(this,
						and ? FolSchematronNode.Logic.LogicType.AND
								: FolSchematronNode.Logic.LogicType.OR);

		/*
		 * we know that we can plug all predicates into the same Logic node
		 * (represented by scn) because the logic operator does not change for
		 * the predicates in an AndOr
		 */
		for (int i = 0; i < predicates.size(); i++) {
			translateConstraint(predicates.get(i), scn);
		}

		// Merge into caller if necessary
		if (!passencl && enclosing != null)
			enclosing.addChild(scn);

		return scn;
	}

	private FolSchematronNode translateComparisonOperation(
			BinaryComparisonPredicate fol, String operatorSymbol,
			FolSchematronNode enclosing) {

		// Create the Comparison
		FolSchematronNode.ComparisonNode scn = new FolSchematronNode.ComparisonNode(
				this, operatorSymbol);
		// Compute and add the operands
		scn.addChild(translateConstraint(fol.getExprLeft(), null));
		scn.addChild(translateConstraint(fol.getExprRight(), null));

		// If there is an enclosing logic, add the created Comparison
		if (enclosing != null)
			enclosing.addChild(scn);
		return scn;
	}

	private FolSchematronNode translateNot(Not fol, FolSchematronNode enclosing) {

		// Create the not node
		FolSchematronNode.NotNode not = new FolSchematronNode.NotNode(this);

		// Evaluate the operand
		not.addChild(translateConstraint(fol.getPredicate(), null));

		if (enclosing != null)
			enclosing.addChild(not);

		return not;
	}

	private FolSchematronNode translateQuantification(Quantification q,
			FolSchematronNode enclosing) {

		FolSchematronNode.QuantificationNode qn = new FolSchematronNode.QuantificationNode(
				this, q);

		qn.setVariableNode(translateVariable(q.getVar(), null));

		qn.setCondition(translateConstraint(q.getCondition(), null));

		// Embed into enclosing context
		if (enclosing != null)
			enclosing.addChild(qn);

		return qn;
	}

	private FolSchematronNode translateLiteral(Literal lit,
			FolSchematronNode enclosing) {

		// OK so far. Construct Literal Node (just wrapping the OclNode)
		FolSchematronNode.LiteralNode litn = new FolSchematronNode.LiteralNode(
				this, lit);
		// Embed into enclosing context
		if (enclosing != null)
			enclosing.addChild(litn);
		return litn;
	}

	private FolSchematronNode translateSchemaCall(SchemaCall sc,
			FolSchematronNode enclosing, boolean isVariableValue) {

		// handle variable context
		FolSchematronNode.AttributeNode atn = new FolSchematronNode.AttributeNode(
				this);

		if (sc.hasVariableContext()) {

			FolSchematronNode.VariableNode varnode = varNodesByVarName.get(sc
					.getVariableContext().getName());

			atn.setVariable(varnode);
		}

		if (sc instanceof PropertyCall) {

			PropertyCall pc = (PropertyCall) sc;

			// Find out if we are appending a nilReason property
			int absorptionType = 0;

			PropertyInfo pi = pc.getSchemaElement();
			boolean nilreason = pi.implementedByNilReason();

			if (nilreason) {
				absorptionType = 2;
			}

			// Is this a selector based on a Variable?
			if (pc.hasVariableContext()) {

				FolSchematronNode.VariableNode varnode = varNodesByVarName
						.get(pc.getVariableContext().getName());

				/*
				 * Find the generating attribute of the variable (searching in
				 * parents if necessary) - it may be null if the variable is
				 * purely based on other variables without actual value
				 */
				FolSchematronNode.AttributeNode varatn = varnode
						.generatingAttribute();

				/*
				 * Find out if the variable value is absorbing the current
				 * attribute member (which includes nilReason treatment)
				 */
				if (varatn != null && varatn.isPropertyAbsorbing()) {

					/*
					 * Set up the retrieved variable attribute component as
					 * absorbing the new attribute component
					 */
					if (absorptionType == 0)
						absorptionType = 1;

					varatn.appendAbsorbedAttribute(absorptionType, pc);

					// TBD: not sure about the following
					// atn = varatn;

				} else {

					/*
					 * So, we are not treating an absorbing base attribute.
					 */
					atn.appendAttribute(pc);
				}

			} else {

				// This PropertyCall is just another qualification for the
				// enclosing AttributeNode. So we just need to append to the
				// existing AttributeNode ...

				atn = (AttributeNode) enclosing;

				// Different ways to append on findings concerning property
				// absorption and nilReason treatment ...

				if (atn.isPropertyAbsorbing()) {

					// Set up attribute component as absorbed
					if (absorptionType == 0)
						absorptionType = 1;

					atn.appendAbsorbedAttribute(absorptionType, pc);

				} else {

					// Append info to Attribute node normally as another step
					atn.appendAttribute(pc);
				}
			}

		} else {

			// TBD: at the moment we ignore a type restriction that could be
			// expressed via the class call

			// prevent ClassCall without variableContext or nextElement from
			// being added
			if (!(sc.hasVariableContext() || sc.hasNextElement())) {
				return null;
			}

			if (enclosing != null && enclosing instanceof AttributeNode) {
				atn = (AttributeNode) enclosing;
			}
		}

		if (sc.hasNextElement()) {
			translateSchemaCall(sc.getNextElement(), atn, false);
		}

		return atn;
	}

	@Override
	public void write() {

		if (printed || !atLeastOneSchematronRuleExists) {
			return;
		}

		// Choose serialization parameters
		Properties outputFormat = OutputPropertiesFactory
				.getDefaultMethodProperties("xml");
		outputFormat.setProperty("indent", "yes");
		outputFormat.setProperty("{http://xml.apache.org/xalan}indent-amount",
				"2");
		// outputFormat.setProperty("encoding", model.characterEncoding());
		outputFormat.setProperty("encoding", "UTF-8");

		// Do the actual writing
		try {

			String fileName = schema.name().replace("/", "_").replace(" ", "_")
					+ "_SchematronSchema.xml";

			OutputStream fout = new FileOutputStream(outputDirectory + "/"
					+ fileName);
			OutputStreamWriter outputXML = new OutputStreamWriter(fout,
					outputFormat.getProperty("encoding"));

			Serializer serializer = SerializerFactory
					.getSerializer(outputFormat);

			serializer.setWriter(outputXML);
			serializer.asDOMSerializer().serialize(document);

			outputXML.close();

		} catch (Exception e) {
			String m = e.getMessage();
			if (m != null) {
				result.addError(m);
			}
			e.printStackTrace(System.err);
		}

		// Indicate we did it to avoid doing it again
		printed = true;
	}

	/** Add attribute to an element */
	private void addAttribute(Document document, Element e, String name,
			String value) {
		Attr att = document.createAttribute(name);
		att.setValue(value);
		e.setAttributeNode(att);
	}

	@Override
	public int getTargetID() {
		return TargetIdentification.FOL2SCHEMATRON.getId();
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.MessageSource#message(int)
	 */
	public String message(int mnr) {

		switch (mnr) {
		case 1:
			return "Generating Schematron from First Order Logic constraints for application schema '$1$'.";
		case 2:
			return "Processing class '$1$'.";
		case 3:
			return "Directory named '$1$' does not exist or is not accessible.";
		case 4:
			return "Class '$1$' is an AIXM <<extension>> for which constraints are being ignored (only constraints in actual AIXM <<feature>> and <<object>> types are relevant).";
		case 5:
			return "??Processing a AIXM schemas but Options does not contain AIXMSchemaInfos. Ensure that the selected AIXM schema have been merged before executing this target.";
		case 6:
			return "??No AIXMSchemaInfo found for Info object '$1$'.";
		case 7:
			return "Expected variable in left hand side of is-type-of expression.";
		case 101:
			return "Failure to compile FOL constraint named \"$2$\" in class \"$3$\". Node class \"$1$\" not implemented.";
		case 104:
			return "Implementation restriction - in FOL constraint \"$1$\" in class \"$2$\" argument to operator \"$3$\" must be class constant.";
		case 106:
			return "Failure to compile FOL constraint named \"$2$\" in class \"$3$\". Property call construct named \"$1$\" not implemented.";
		case 126:
			return "Implementation restriction - in FOL constraint \"$1$\" in class \"$2$\" comparison between structured non-object types not supported.";
		default:
			return "(Unknown message: " + mnr + ")";
		}
	}

	/**
	 * @return the varIndex
	 */
	public int getNextVarIndex() {
		varIndex++;
		return varIndex;
	}

	public void reset() {
		varIndex = 0;
		varNodesByVarName = new HashMap<String, FolSchematronNode.VariableNode>();
	}

	public String schemaQname(String elementName) {

		if (elementName == null) {
			return null;
		} else {
			return this.xmlns + ":" + elementName;
		}
	}

}
