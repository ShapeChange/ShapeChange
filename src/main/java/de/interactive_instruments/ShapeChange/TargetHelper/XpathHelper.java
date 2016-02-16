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

package de.interactive_instruments.ShapeChange.TargetHelper;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.interactive_instruments.ShapeChange.MapEntry;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.OclConstraint;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Ocl.OclNode;
import de.interactive_instruments.ShapeChange.Target.XmlSchema.XmlSchema;

public class XpathHelper implements MessageSource {

	public HashMap<String,String> namespaces = new HashMap<String,String>();
	private Options options = null;
	private String classname = null;
	private ShapeChangeResult result = null;

	private String currentOclConstraintName = null; 
	private ClassInfo currentOclConstraintClass = null;	

	public static class ExtensionFunctionTemplate {
		public String nsPrefix;
		public String namespace;
		public String function;
		public ExtensionFunctionTemplate( String nsp, String ns, String fct ) {
			nsPrefix = nsp; namespace = ns; function = fct;
		}
	}
	
	String alpha = "#";
	String beta = "";
	HashMap<String,ExtensionFunctionTemplate> extensionFunctions =
		new HashMap<String,ExtensionFunctionTemplate>();
	
	public XpathHelper(Options o, ShapeChangeResult r) {
		options = o;
		result = r;

		classname = XmlSchema.class.getName();
		
		// Get prefix and postfix of xlink:href references
		String s = options.parameter( classname, "schematronXlinkHrefPrefix" );
		if(s!=null)
			alpha = s;
		s = options.parameter( classname, "schematronXlinkHrefPostfix" );		
		if(s!=null)
			beta = s;
		
		// Read extension function templates
		String pats = "^schematronExtension\\.(\\w+?)\\.function";
		String[] extdecls = options.parameterNamesByRegex( classname, pats );
		Pattern pat = Pattern.compile( pats );
		for( String ext : extdecls ) {
			// Pick the function name from the parameter key
			Matcher mat = pat.matcher( ext );
			mat.matches();
			String fctname = mat.group( 1 );
			// Obtain the function pattern
			String fcts = options.parameter( classname, ext );
			// Obtain associated namespace parameter or a default
			String nss = options.parameter( 
				classname, "schematronExtension."+fctname+".namespace" );
			if( nss==null || nss.length()==0 ) {
				nss = "java:java";
			}
			// Split namespace and prefix 
			int col = nss.indexOf( ":" );
			String nspx = "java";
			String ns = nss;
			if( col>=0 ) {
				nspx = nss.substring( 0, col );
				ns = nss.substring( col+1 );
			}
			// Record an extension template
			extensionFunctions.put(
				fctname, new ExtensionFunctionTemplate(nspx, ns, fcts) );
		}	
	}

	/**
	 * This method registers a namespace (prefix and namespace proper)
	 * when the namespace occurs the first time
	 * @param xmlns Namespace prefix
	 * @param ns Namespace proper
	 */
	public void registerNamespace( String xmlns, String ns ) {
		if( ! namespaces.containsKey(xmlns) ) {
			namespaces.put(xmlns, ns);
		}
	}
	
	/**
	 * This auxiliary method registers a well-known namespace prefix
	 * when the namespace occurs the first time
	 * @param xmlns Namespace prefix
	 */
	public void registerNamespace( String xmlns ) {
		if( ! namespaces.containsKey(xmlns) ) {
			String ns = options.fullNamespace( xmlns );
			registerNamespace( xmlns, ns );
		}
	}
	
	/**
	 * Auxiliary method to find out the full, namespace adorned name of a 
	 * property from the model. As a side effect the method registers the namespace
	 * @param pi PropertyInfo object
	 * @return Element name of property
	 */
	public String getAndRegisterXmlName( PropertyInfo pi ) {
		String nspref = pi.inClass().pkg().xmlns();
		String proper = nspref + ":" + pi.name();
		registerNamespace( nspref, pi.inClass() );
		return proper;
	}
	
	/**
	 * <p>Auxiliary method to find out the full, namespace adorned name of a 
	 * class from the the mapping or the model. As a side effect the method 
	 * makes the namespace also known to the Schematron schema, appending 
	 * another &lt;ns> element if necessary.</p>
	 * <p>The method considers the mapping in first priority. If the class
	 * turns out to map to a basic type, <i>null</i> is returned instead of
	 * a name.</p>
	 * @param pi ClassInfo object
	 * @return Element name of class
	 */
	public String getAndRegisterXmlName( ClassInfo ci ) {
		String nspref = null;
		String fulnam = null;
		MapEntry me = 
			ci.options().elementMapEntry(ci.name(),ci.encodingRule("xsd"));
		if( me!=null ) {
			if( me.p1==null || me.p1.length()==0 )
				return null;
			fulnam = me.p1;
			String[] parts = fulnam.split( ":" );
			if( parts.length>1 ) nspref = parts[0];
			registerNamespace( nspref );
		} else {
			nspref = ci.pkg().xmlns();
			fulnam = nspref + ":" + ci.name();
			registerNamespace( nspref, ci );
		}
		return fulnam;
	}

	/**
	 * This special variant of the method above considers the class object to
	 * determine the full namespace uri. If this is not successful it resorts
	 * to the method above, which uses the namespace mapping mechanism in the
	 * Options object.
	 * @param xmlns Namespace prefix
	 * @param ci ClassInfo object to fetch the namespace uri from
	 */
	public void registerNamespace( String xmlns, ClassInfo ci ) {
		if( ! namespaces.containsKey(xmlns) ) {
			String ns = ci.pkg().targetNamespace();
			if( ns==null || ns.length()==0 )
				registerNamespace( xmlns );
			else {
				registerNamespace( xmlns, ns );
			}
		}
	}
	
	/**
	 * Take an OCL constraint and translate it into an Xpath expression
	 * @param ci ClassInfo object, which is context to the constraint.
	 * @param c OCL constraint. Must be invariant.
	 */
	public XpathConstraintNode.XpathFragment translateConstraintToXpath(ClassInfo ci, OclConstraint c) {
		if (c == null)
			return null;
		
		// Drop abstract classes
		if( ci.isAbstract() )
			return null;

		// Set environment for possible error messages
		currentOclConstraintName = c.name(); 
		currentOclConstraintClass = c.contextClass();

		// Get hold of the syntax tree
		OclNode.Expression oclex = c.syntaxTree();
		
		// Derive the target Schematron syntax tree from the OCL tree,
		// quit if in error due to implementation restrictions
		XpathConstraintNode scn =
			translateConstraintToSchematronNode( oclex, null, false );
		if (scn==null) 
			return null;

		// Now, translate this to an Xpath fragment object, which is supposed
		// to contain all necessary information to generate the Rule.
		XpathConstraintNode.BindingContext ctx = 
			new XpathConstraintNode.BindingContext(
				XpathConstraintNode.BindingContext.CtxState.ATCURRENT );
		XpathConstraintNode.XpathFragment xpath = scn.translate(ctx);
		
		// The generated Xpath syntax may still contain errors, which have
		// been detected during the compilation process and which are coded
		// in the result by means of a particular string pattern. Find out.
		if( checkErrorsInXpathFragment(xpath) ) return null;
		
		return xpath;
	}
	
	/**
	 * <p>This function recursively descends into an OclConstraint following
	 * the OclNode structure. In doing so it generates an equivalent syntax
	 * tree which is more in line with Xpath syntax and its use in the 
	 * Schematron schema.</p>
	 * @param ocl OclNode of some level, initially called with 
	 * OclNode.Expression
	 * @param enclosing Enclosing target construct, may be null
	 * @param negate Flag to indicate that a logical negation is to be pushed
	 * downwards
	 * @return Constructed XpathConstraintNode tree. null if in error.
	 */
	protected XpathConstraintNode translateConstraintToSchematronNode( 
		OclNode ocl, XpathConstraintNode enclosing, boolean negate ) {
		
		XpathConstraintNode scn = null;
		
		if( ocl instanceof OclNode.Expression ) {
			// The OCL Expression wrapper ...
			OclNode.Expression ex = (OclNode.Expression) ocl;
			scn = translateConstraintToSchematronNode( 
				ex.expression, null, negate );
			// Check on implementation restrictions
			if( scn.containsError() ) return null;
		} else if( ocl instanceof OclNode.IterationCallExp ) {
			// IterationCallExp
			OclNode.IterationCallExp iter = (OclNode.IterationCallExp) ocl;
			scn = translateConstraintIterationToSchematronNode( 
					iter, enclosing, negate );
		} else if( ocl instanceof OclNode.OperationCallExp ) {
			// OperationCallExp
			OclNode.OperationCallExp oper = (OclNode.OperationCallExp) ocl;
			scn = translateConstraintOperationToSchematronNode( 
					oper, enclosing, negate );
		} else if( ocl instanceof OclNode.AttributeCallExp ) {
			// AttributeCallExp
			OclNode.AttributeCallExp attr = (OclNode.AttributeCallExp) ocl;
			scn = translateConstraintAttributeToSchematronNode( 
					attr, enclosing, negate );
		} else if( ocl instanceof OclNode.LiteralExp ) {
			// LiteralExp
			OclNode.LiteralExp lit = (OclNode.LiteralExp) ocl;
			scn = translateConstraintLiteralToSchematronNode( lit, enclosing, negate );
		} else if( ocl instanceof OclNode.VariableExp ) {
			// VariableExp
			OclNode.VariableExp var = (OclNode.VariableExp) ocl;
			scn = new XpathConstraintNode.Variable( 
					this, var.declaration, negate );
			if( enclosing!=null ) enclosing.addChild( scn );
		} else if( ocl instanceof OclNode.IfExp ) {
			// IfExp
			OclNode.IfExp ifex = (OclNode.IfExp) ocl;
			scn = translateConstraintIfExpToSchematronNode( 
					ifex, enclosing, negate );
		} else if( ocl instanceof OclNode.LetExp ) {
			// LetExp
			result.addError( 
				this, 102, currentOclConstraintName, 
				currentOclConstraintClass.name() );
			scn = new XpathConstraintNode.Error( this );
		} else {
			// Anything unknown
			String clname = ocl.getClass().getSimpleName();
			result.addError( 
				this, 101, clname, currentOclConstraintName, 
				currentOclConstraintClass.name() );
			scn = new XpathConstraintNode.Error( this );
		}
		
		return scn;
	}	

	/**
	 * <p>This function treats the implemented IterationCallExp objects in an
	 * OCL expression. Doing so it generates intermediate code which is better
	 * suited for Schematron generation than the original OCL constructs.</p>
	 * @param iter The IterationCallExp node to be processed
	 * @param enclosing Enclosing target construct
	 * @param negate Flag to indicate that a logical negation is to be pushed
	 * downwards
	 * @return Constructed XpathConstraintNode tree
	 */
	protected XpathConstraintNode translateConstraintIterationToSchematronNode(
		OclNode.IterationCallExp iter, XpathConstraintNode enclosing, 
		boolean negate ) {
		
		String opname = iter.selector.name;

		if( opname.equals( "exists" ) || opname.equals( "forAll" ) ) {

			// Note: forAll is mapped to exists 
			boolean isForAll = opname.equals( "forAll" );
			// Create the Exists node
			XpathConstraintNode.Exists exists =
				new XpathConstraintNode.Exists( 
					this, iter.declarations[0], negate^isForAll );
			// Evaluate the operand and the body
			exists.addChild(
				translateConstraintToSchematronNode( 
					iter.object, null, false ) );
			exists.addChild(
				translateConstraintToSchematronNode( 
					iter.arguments[0], null, isForAll ) );
			// Merge Exists node into enclosing logic if there is one
			if( enclosing!=null ) enclosing.addChild( exists );
			return exists;
		
		} else if( opname.equals( "isUnique" ) ) {

			// Create the Unique node
			XpathConstraintNode.Unique unique =
				new XpathConstraintNode.Unique( 
					this, iter.declarations[0], false );
			// Evaluate the operand and the body
			unique.addChild(
				translateConstraintToSchematronNode( 
					iter.object, null, false ) );
			unique.addChild(
				translateConstraintToSchematronNode( 
					iter.arguments[0], null, false ) );
			// Merge Unique node into enclosing logic if there is one
			if( enclosing!=null ) enclosing.addChild( unique );
			return unique;
			
			
		} else if( opname.equals( "select" ) ) {

			// Create the Exists node
			XpathConstraintNode.Select select =
				new XpathConstraintNode.Select( this, iter.declarations[0] );
			// Evaluate the operand and the body
			select.addChild(
				translateConstraintToSchematronNode( iter.object, null, false ) );
			select.addChild(
				translateConstraintToSchematronNode( iter.arguments[0], null, false ) );
			// Merge Exists node into enclosing logic if there is one
			if( enclosing!=null ) enclosing.addChild( select );
			return select;
				
		} else {
			// Operation not supported
			result.addError( 
				this, 103, opname, currentOclConstraintName, 
				currentOclConstraintClass.name() );
		}
		return new XpathConstraintNode.Error( this );
	}
	
	/**
	 * <p>This function treats the implemented OperationCallExp objects in an
	 * OCL expression. Doing so it generates intermediate code which is better
	 * suited for Schematron generation than the original OCL constructs.</p>
	 * <p>Particularly, all logical operations are collected in Logic objects
	 * of the three flavors AND, OR and XOR, where AND and OR have as many as 
	 * possible children. NOT is pushed downwards by using De Morgan's rule.</p> 
	 * @param oper The OperationCallExp node to be processed
	 * @param enclosing Enclosing target construct
	 * @param negate Flag to indicate that a logical negation is to be pushed
	 * downwards
	 * @return Constructed XpathConstraintNode tree
	 */
	protected XpathConstraintNode translateConstraintOperationToSchematronNode(
		OclNode.OperationCallExp oper, XpathConstraintNode enclosing, 
		boolean negate ) {
		
		String opname = oper.selector.name;

		if( opname.equals( "implies" ) ) {
			
			// 'implies' is realized as ~a | b, so it is basically an OR
			boolean and = false, neg1 = true, neg2 = false;
			if( negate ) { and = !and; neg1 = !neg1; neg2 = !neg2; }
			// Merge into enclosing logic? If not so, create a Logic node
			boolean passencl = enclosing!=null && enclosing.isAndOrLogic( and );
			XpathConstraintNode scn = 
				passencl ? enclosing 
					: new XpathConstraintNode.Logic( 
						this, 
						and ? 
						XpathConstraintNode.Logic.LogicType.AND : 
						XpathConstraintNode.Logic.LogicType.OR );
			// Recursion ...
			translateConstraintToSchematronNode( oper.object, scn, neg1 );
			translateConstraintToSchematronNode( oper.arguments[0], scn, neg2 );
			// Merge into caller if necessary
			if( !passencl && enclosing!=null ) enclosing.addChild( scn );
			return scn;
			
		} else if( opname.equals( "and" ) || opname.equals( "or" ) ) {
			
			// 'and' or 'or', find out what we are ...
			boolean and = opname.equals( "and" );
			boolean neg = false;
			if( negate ) { and = !and; neg = !neg;}
			// Merge into enclosing logic? If not so create a Logic node
			boolean passencl = enclosing!=null && enclosing.isAndOrLogic( and );
			XpathConstraintNode scn = 
				passencl ? enclosing 
					: new XpathConstraintNode.Logic( 
						this, 
						and ?
						XpathConstraintNode.Logic.LogicType.AND :
						XpathConstraintNode.Logic.LogicType.OR );
			// Recursion ...
			translateConstraintToSchematronNode( oper.object, scn, neg );
			translateConstraintToSchematronNode( oper.arguments[0], scn, neg );
			// Merge into caller if necessary
			if( !passencl && enclosing!=null ) enclosing.addChild( scn );
			return scn;
			
		} else if( opname.equals( "xor" )) {
			
			// In Xpath this will be expressed by a != on boolean operands,
			// negation transposes this to an = comparison (equivalence).
			XpathConstraintNode.Logic.LogicType xor =
				XpathConstraintNode.Logic.LogicType.XOR;
			if( negate )
				xor = XpathConstraintNode.Logic.LogicType.EQV;
			// Construct the node
			XpathConstraintNode fcn =
				new XpathConstraintNode.Logic( this, xor );
			fcn.addChild( 
				translateConstraintToSchematronNode( oper.object, null, false ) );
			fcn.addChild( 
				translateConstraintToSchematronNode( oper.arguments[0], null, false ) );
			// If there is an enclosing logic, add the created one
			if( enclosing!=null ) enclosing.addChild( fcn );
			return fcn;		
			
		} else if( opname.equals( "not" ) ) {
			
			// A 'not' just pushes a negation downwards
			return translateConstraintToSchematronNode( 
				oper.object, enclosing, ! negate );
			
		} else if( "<>=<=".indexOf( opname ) >= 0 ) {
			
			// A relational operator absorbs a pushed down negation by 
			// inverting the relation.
			final String[] relops = { "=", "<>", "<", "<=", ">", ">=" };
			final String[] invops = { "<>", "=", ">=", ">", "<=", "<" };
			String name = opname;
			if( negate ) {
				for( int i=0; i<relops.length; i++ ) { 
					if( relops[i].equals(opname) ) { name = invops[i]; break; }
				}
			}
			// Create the Comparison
			XpathConstraintNode.Comparison scn = 
				new XpathConstraintNode.Comparison( this, name );
			// Compute and add the operands
			scn.addChild( 
				translateConstraintToSchematronNode( oper.object, null, false ) );
			scn.addChild( 
				translateConstraintToSchematronNode( oper.arguments[0], null, false ) );
			// If there is an enclosing logic, add the created Comparison
			if( enclosing!=null ) enclosing.addChild( scn );
			return scn;
			
		} else if( opname.equals( "notEmpty" ) || opname.equals( "isEmpty" ) ) {

			// Create the Empty node
			XpathConstraintNode.Empty empty =
				new XpathConstraintNode.Empty( 
						this, opname.equals("notEmpty") ^ negate );
			// Evaluate the operand
			empty.addChild(
				translateConstraintToSchematronNode( oper.object, null, false ) );
			// Merge Empty node into enclosing logic if there is one
			if( enclosing!=null ) enclosing.addChild( empty );
			return empty;
						
		} else if( opname.equals( "size" ) ) {
			
			// Create the Size node
			boolean setoper = 
				oper.selector.category == OclNode.PropertyCategory.SETOPER;
			XpathConstraintNode.Size size =
				new XpathConstraintNode.Size( this, setoper );
			// Evaluate the operand
			size.addChild(
				translateConstraintToSchematronNode( oper.object, null, false ) );
			return size;

		} else if( opname.equals( "concat" ) ) {
			
			// Create the Concatenate node
			XpathConstraintNode.Concatenate concat =
				new XpathConstraintNode.Concatenate( this );
			// Evaluate the operands
			concat.addChild(
				translateConstraintToSchematronNode( oper.object, null, false ) );
			concat.addChild(
				translateConstraintToSchematronNode( oper.arguments[0], null, false ) );
			return concat;	

		} else if( opname.equals( "substring" ) ) {
			
			// Create the Substring node
			XpathConstraintNode.Substring substr =
				new XpathConstraintNode.Substring( this );
			// Evaluate the operands
			substr.addChild(
				translateConstraintToSchematronNode( oper.object, null, false ) );
			substr.addChild(
				translateConstraintToSchematronNode( oper.arguments[0], null, false ) );
			substr.addChild(
				translateConstraintToSchematronNode( oper.arguments[1], null, false ) );
			return substr;

		} else if( opname.equals( "matches" ) ) {
			
			// Check if this is configured
			if( extensionFunctions.get( "matches" )==null )
				result.addError( 
						this, 107, opname, currentOclConstraintName, 
						currentOclConstraintClass.name() );
			else {
				// Create the Matches node
				XpathConstraintNode.Matches matches =
					new XpathConstraintNode.Matches( this );
				// Evaluate the operands
				matches.addChild(
					translateConstraintToSchematronNode( oper.object, null, false ) );
				matches.addChild(
					translateConstraintToSchematronNode( oper.arguments[0], null, false ) );
				return matches;
			}
					
		} else if( opname.equals( "length" ) || opname.equals( "area" ) ) {
			
			result.addError( 
				this, 103, opname, currentOclConstraintName, 
				currentOclConstraintClass.name() );

		} else if( opname.equals( "toUpper" ) || opname.equals( "toLower" ) ) {
			
			result.addError( 
				this, 103, opname, currentOclConstraintName, 
				currentOclConstraintClass.name() );
			
		} else if( "+-*/".indexOf( opname ) >= 0 ) {
			
			// Create the Arithmetic node
			XpathConstraintNode.Arithmetic arith =
				new XpathConstraintNode.Arithmetic( this, opname );
			// Evaluate the operands
			arith.addChild( 
				translateConstraintToSchematronNode( oper.object, null, false ) );
			if( oper.arguments.length>0 )
				arith.addChild(
					translateConstraintToSchematronNode( 
						oper.arguments[0], null, false ) );
			return arith;
			
		} else if( opname.equals( "oclIsKindOf" ) || 
				   opname.equals( "oclIsTypeOf" ) ) {
			
			// isTypeOf is mapped as an option of KindOf ...
			boolean exactType = opname.equals( "oclIsTypeOf" );
			// Create the KindOf node
			XpathConstraintNode.KindOf kindOf =
				new XpathConstraintNode.KindOf( this, exactType, negate );
			// Evaluate the operand to be tested
			kindOf.addChild(
				translateConstraintToSchematronNode( oper.object, null, false ) );
			// Evaluate the type and make sure it is a class constant.
			XpathConstraintNode clex = 
				translateConstraintToSchematronNode( oper.arguments[0], null, false ); 
			boolean assumeError = true;
			if( clex instanceof XpathConstraintNode.Literal ) {
				XpathConstraintNode.Literal cllit = 
					(XpathConstraintNode.Literal)clex;
				OclNode.LiteralExp lex = cllit.literal;
				if( lex instanceof OclNode.ClassLiteralExp ) {
					OclNode.ClassLiteralExp lcl = (OclNode.ClassLiteralExp)lex;
					ClassInfo ci = lcl.umlClass;
					kindOf.setClass( ci );
					assumeError = false;
				}
			}
			// The object must be the first child
			kindOf.addChild( clex );
			
			// Since for the time being functionality is restricted to class 
			// constants , we emit an error message if this not the case ...
			if( assumeError ) {
				result.addError( this, 104, currentOclConstraintName, 
					currentOclConstraintClass.name(), "oclIsKindOf" );
				XpathConstraintNode err =  
					new XpathConstraintNode.Error( this );
				if( enclosing!=null ) enclosing.addChild( err );
				return err;
			}
				
			// Merge KindOf node into enclosing logic if there is one
			if( enclosing!=null ) enclosing.addChild( kindOf );
			return kindOf;

		} else if( opname.equals( "oclAsType" ) ) {
			
			// Create the KindOf node
			XpathConstraintNode.Cast cast =
				new XpathConstraintNode.Cast( this );
			// Evaluate the operand to be tested
			cast.addChild(
				translateConstraintToSchematronNode( oper.object, null, false ) );
			// Evaluate the type and make sure it is a class constant of 
			// geometry type.
			XpathConstraintNode clex = 
				translateConstraintToSchematronNode( oper.arguments[0], null, false ); 
			boolean assumeError = true;
			if( clex instanceof XpathConstraintNode.Literal ) {
				XpathConstraintNode.Literal cllit = 
					(XpathConstraintNode.Literal)clex;
				OclNode.LiteralExp lex = cllit.literal;
				if( lex instanceof OclNode.ClassLiteralExp ) {
					OclNode.ClassLiteralExp lcl = (OclNode.ClassLiteralExp)lex;
					ClassInfo ci = lcl.umlClass;
					cast.setClass( ci );
					assumeError = false;
				}
			}
			// The object must be the first child
			cast.addChild( clex );
			
			// Since for the time being functionality is restricted to class
			// constants, we emit an error message if this not the case ...
			if( assumeError ) {
				result.addError( this, 104, currentOclConstraintName, 
					currentOclConstraintClass.name(), "oclAsType" );
				XpathConstraintNode err =  
					new XpathConstraintNode.Error( this );
				if( enclosing!=null ) enclosing.addChild( err );
				return err;
			}
				
			// Merge Cast node into enclosing logic if there is one
			if( enclosing!=null ) enclosing.addChild( cast );
			return cast;			
			
		} else if( opname.equals( "allInstances" ) ) {
						
			// Evaluate the type and make sure it is a class constant with
			// a database table attached
			XpathConstraintNode clex = 
				translateConstraintToSchematronNode( oper.object, null, false ); 
			boolean assumeError = true;
			ClassInfo ci = null;
			if( clex instanceof XpathConstraintNode.Literal ) {
				XpathConstraintNode.Literal cllit = 
					(XpathConstraintNode.Literal)clex;
				OclNode.LiteralExp lex = cllit.literal;
				if( lex instanceof OclNode.ClassLiteralExp ) {
					OclNode.ClassLiteralExp lcl = (OclNode.ClassLiteralExp)lex;
					ci = lcl.umlClass;
					if( ci!=null )
						assumeError = false;
				}
			}
			// Since for the time being functionality is restricted to constant
			// types, we emit an error if this is not the case
			if( assumeError ) {
				result.addError( this, 105, currentOclConstraintName, 
					currentOclConstraintClass.name() );
				XpathConstraintNode err =  
					new XpathConstraintNode.Error( this );
				if( enclosing!=null ) enclosing.addChild( err );
				return err;
			}
			// All is ok. Create the AllInstances node
			XpathConstraintNode.AllInstances allinst =
				new XpathConstraintNode.AllInstances( this, ci, negate );
			// Merge AllInstances node into enclosing logic if there is one
			if( enclosing!=null ) enclosing.addChild( allinst );
			return allinst;
			
		} else if( opname.startsWith( "error_" ) ) {

			// Create the ErrorComment node
			XpathConstraintNode.MessageComment msgcom =
				new XpathConstraintNode.MessageComment( this, opname );
			
			// Evaluate the arguments
			for( OclNode arg : oper.arguments ) {
				msgcom.addChild(
					translateConstraintToSchematronNode( arg, null, false ) );
			}
			return msgcom;
			
		} else {
			// Operation not found
			result.addError(
				this, 103, opname, currentOclConstraintName, 
				currentOclConstraintClass.name() );
			
		}
		return new XpathConstraintNode.Error( this );
	}
	
	/**
	 * <p>This method converts AttibuteCallExp objects into intermediary
	 * SchematronConstraintsNodes in a first step to realize these in Xpath 
	 * code.</p>
	 * @param attr The AttibuteCallExp object
	 * @param enclosing If an enclosing Logic object is passed, the attribute
	 * must be of type Boolean. Otherwise an error is generated.
	 * @param negate A pushed down negation will only be considered if the
	 * attribute is of type Boolean.
	 * @return Constructed XpathConstraintNode tree
	 */
	protected XpathConstraintNode translateConstraintAttributeToSchematronNode(
		OclNode.AttributeCallExp attr, XpathConstraintNode enclosing, 
		boolean negate ) {

		// Have the name of the attribute available
		String attrname = attr.selector.name;

		// Find out if we are appending a nilReason property
		int absorptionType = 0;
		Info info = attr.selector.modelProperty;
		if( info instanceof PropertyInfo ) {
			PropertyInfo pi = (PropertyInfo) info;
			boolean nilreason = pi.implementedByNilReason();
			if( nilreason) absorptionType = 2;
		}
				
		// Translate the object part of the attribute
		XpathConstraintNode objnode = 
			translateConstraintToSchematronNode( attr.object, null, false  );
		
		// Is this a selector based on some information source such as a
		// Variable, AllInstances or Select?
		if( objnode instanceof XpathConstraintNode.Variable ||
			objnode instanceof XpathConstraintNode.AllInstances ||
			objnode instanceof XpathConstraintNode.Select ) {

			// Find out, whether this is a normal attribute or if it is
			// absorbing the current attribute member (which includes nilReason
			// treatment) ...
			// Find the generating attribute if there is any
			XpathConstraintNode.Attribute atn = 
				objnode.generatingAttribute();
			if( atn!=null && atn.isPropertyAbsorbing() ) {
				// Set up this retrieved attribute component as absorbing 
				// the new attribute component
				if( absorptionType==0 ) absorptionType = 1;
				atn.appendAbsorbedAttribute( absorptionType, attr );
				// Result is the object node itself
				return objnode;
			}
			
			// So, we are not treating an absorbing base attribute. Create the 
			// Attribute object, then ...
			atn = new XpathConstraintNode.Attribute( this, attr, negate );
			// Attach information source node
			atn.addChild( objnode );
			// Embed into enclosing context
			if( enclosing!=null ) enclosing.addChild( atn );
			return atn;
			
		} else if( objnode instanceof XpathConstraintNode.Attribute ) {

			// The object of an AttributeExp turns out to be an Attribute node,
			// which means that we are just another qualification for the 
			// latter. So we just need to append the new AttributeExp to the
			// existing Attribute node  ...
			XpathConstraintNode.Attribute atn =
				(XpathConstraintNode.Attribute) objnode;
			// Different ways to append on findings concerning property 
			// absorption and nilReason treatment ...
			if( atn.isPropertyAbsorbing() ) {
				// Set up attribute component as absorbed
				if( absorptionType==0 ) absorptionType = 1;
				atn.appendAbsorbedAttribute( absorptionType, attr );
			}
			else
				// Append info to Attribute node normally as another step
				atn.appendAttribute( attr );
			// Embed into enclosing context
			if( enclosing!=null ) enclosing.addChild( atn );
			return atn;
		}
		
		// Not implemented attribute construct
		result.addError( 
			this, 106, attrname, currentOclConstraintName, 
			currentOclConstraintClass.name() );

		return new XpathConstraintNode.Error( this );
	}
	
	/**
	 * <p>This method is supposed to transform the OclNode Literals to an
	 * intermediary node structure which is suited for PL/SQL generation.</p>
	 * @param lit The OclNode.Literal object
	 * @param enclosing If an enclosing Logic object is passed, the literal
	 * must be of type Boolean. 
	 * @param negate A pushed down negation will only be considered if the
	 * literal is of type Boolean.
	 * @return Constructed XpathConstraintNode tree
	 */
	protected XpathConstraintNode translateConstraintLiteralToSchematronNode(
		OclNode.LiteralExp lit, XpathConstraintNode enclosing, 
		boolean negate ) {
	
		// Construct Literal Node (just wrapping the OclNode)
		XpathConstraintNode.Literal litn = 
			new XpathConstraintNode.Literal( this, lit, negate );
		// Embed into enclosing context
		if( enclosing!=null ) enclosing.addChild( litn );
		return litn;
	}
	
	/**
	 * <p>This method will transform an OclNode.IfExp to an intermediary node
	 * structure suited for Schematron code generation.</p>
	 * @param ifex The OclNode.IfExp object
	 * @param enclosing If an enclosing Logic object is passed, the type of
	 * the IfExp must be Boolean. 
	 * @param negate A pushed down negation will switch the then and else 
	 * parts.
	 * @return Constructed XpathConstraintNode tree
	 */
	protected XpathConstraintNode translateConstraintIfExpToSchematronNode(
		OclNode.IfExp ifex, XpathConstraintNode enclosing, boolean negate ) {
		
		// Create the FMEConstraintNode
		XpathConstraintNode.IfThenElse ifthenelse = 
			new XpathConstraintNode.IfThenElse( this );
		
		// Translate the condition
		ifthenelse.addChild(
			translateConstraintToSchematronNode( ifex.condition, null, false ));
		
		// We swap -then- and -else- if this is to be negated
		OclNode[] branch = { ifex.ifExpression, ifex.elseExpression };
		int is = negate ? 1 : 0;
		for( int i=0; i<2; i++ ) {
			// Compile both branches
			ifthenelse.addChild(
				translateConstraintToSchematronNode( branch[is], null, false ) );
			is = 1 - is;
		}

		// If there is an enclosing logic, add the created IfThenElse
		if( enclosing!=null ) enclosing.addChild( ifthenelse );
		
		return ifthenelse;
	}	
	
	/**
	 * Auxiliary function to determine if the generated Xpath code contains
	 * any expricit errors. If it does, provide messages accordingly.
	 * @param xpath The Xpath fragment to examine.
	 * @return Flag, if true an error has been found
	 */
	private boolean checkErrorsInXpathFragment( 
		XpathConstraintNode.XpathFragment xpath ) {
		// Concatenate all the generated stuff to ease pattern matching
		String allofit = "";
		if( xpath.lets!=null )
			for( String let : xpath.lets.values() ) {
				allofit += let;
			}
		allofit += xpath.fragment;
		// Match for the ERROR pattern and emit messages
	    Pattern p = Pattern.compile( "\\*\\*\\*ERROR\\[(.*?)\\]\\*\\*\\*" );
	    Matcher m = p.matcher( allofit );
	    int count = 0;
	    while( m.find() ) {
	    	count++;
	    	String argsl = m.group( 1 );
	    	String[] args = argsl.split( "," );
	    	int mnr = Integer.parseInt( args[0] );
	    	if( args.length==1 )
	    		result.addError( 
	    			this, mnr, currentOclConstraintName, 
	    			currentOclConstraintClass.name() );
	    	else if( args.length>=2 )
	    		result.addError( 
	    			this, mnr, currentOclConstraintName, 
	    			currentOclConstraintClass.name(), args[1] );
	    }
		return count>0;
	}

	/** 
	 * <p>This method returns messages belonging to the XpathHelper object.
	 * The messages are retrieved by 
	 * their message number. The organization corresponds to the logic in module 
	 * ShapeChangeResult.</p>
	 * @param mnr Message number
	 * @return Message text, including $x$ substitution points.
	 */
	public String message( int mnr ) {
		// Get the message proper and return it with an identification prefixed
		String mess = messageText( mnr );
		if( mess==null ) return null;
		String prefix = "";
		if( mess.startsWith("??") ) {
			prefix = "??";
			mess = mess.substring( 2 );
		}
		return prefix + "Xpath/Schematron Target: " + mess;
	}
	
	/**
	 * This is the message text provision proper. It returns a message for a number.
	 * @param mnr Message number
	 * @return Message text or null
	 */
	protected String messageText( int mnr ) {
		switch( mnr ) {			

		case 101:
			return "Failure to compile OCL constraint named \"$2$\" in class \"$3$\". Node class \"$1$\" not implemented.";
		case 102: 
			return "Implementation restriction - in OCL constraint \"$1$\" in class \"$2$\" assignment construct \"let\" not supported.";
		case 103:
			return "Failure to compile OCL constraint named \"$2$\" in class \"$3$\". Operation \"$1$\" not implemented.";
		case 104:
			return "Implementation restriction - in OCL constraint \"$1$\" in class \"$2$\" argument to operator \"$3$\" must be class constant.";
		case 105:
			return "Implementation restriction - in OCL constraint \"$1$\" in class \"$2$\" object of operator \"allInstances\" must be class constant.";
		case 106: 
			return "Failure to compile OCL constraint named \"$2$\" in class \"$3$\". Attribute construct named \"$1$\" not implemented.";
		case 107:
			return "Failure to compile OCL constraint named \"$2$\" in class \"$3$\". Schematron extension operation \"$1$\" is not properly configured.";
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
		}
		return null;
	}
}
