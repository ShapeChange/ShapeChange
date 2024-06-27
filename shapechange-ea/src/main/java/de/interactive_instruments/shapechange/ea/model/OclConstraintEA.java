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

package de.interactive_instruments.shapechange.ea.model;

import java.io.StringReader;
import org.sparx.AttributeConstraint;

import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.model.OclConstraint;
import de.interactive_instruments.shapechange.core.model.OclConstraintImpl;
import de.interactive_instruments.shapechange.core.ocl.MessageCollection;
import de.interactive_instruments.shapechange.core.ocl.OclParser;

/**
 * <p>This implements the OclConstraint interface for the Enterprise Architect
 * model platform.</p>
 * <ul>
 * <li>The concepts of name and status are directly taken from
 * the corresponding dialog fields.
 * <li>The constraint text is directly from the Notes field of the dialog. 
 * <li>The context is determined from the particular context of Ctor invocation.
 * <li>Condition type (<i>inv:</i>, etc) is part of the constraint 
 * interpretation, which also delivers a syntax tree of OCL specific node types.
 * </ul>
 */
public class OclConstraintEA 
	extends OclConstraintImpl implements OclConstraint {

	/** The model object */
	protected EADocument document = null;
	
	/** The EA OCL constraint object */
	protected org.sparx.Constraint eaConstraintClass;
	protected AttributeConstraint eaConstraintAttribute; 
	
	public OclConstraintEA(EADocument doc, ClassInfoEA ci,
		org.sparx.Constraint constr) {
		
		// Record the containment links.
		document = doc;
		contextClass = ci;
		contextModelElmtType = ModelElmtContextType.CLASS;
		contextModelElmt = ci;
		
		// The EA constraint object
		eaConstraintClass = constr;
		
		// Name and status
		constraintName = constr.GetName();
		constraintStatus = constr.GetStatus();		

		constraintText = constr.GetNotes();
		if(constraintText!=null)
			constraintText = 
				doc.applyEAFormatting(
						constraintText);
		
		// Compiler
		syntaxTree = null;
		Readable instream = new StringReader(constraintText);
		MessageCollection messages;
		OclParser parse = new OclParser();
		
		// Parse the constraint
		document.result.addDebug(null, 10006, ci.name(), constraintName);
		syntaxTree = 
			parse.parseOcl(instream, contextModelElmt);
		
		// Get condition type.
		if(syntaxTree!=null)
			conditionType = syntaxTree.expressionType;
		
		// Get the comments
		comments = parse.getComments();
		
		// Syntax tree as debug output + comments
		if(document.options.parameter("reportLevel").equals("DEBUG")) {
			document.result.addDebug(null, 10024, parse.debugTempNodes());
			if(syntaxTree!=null)
				document.result.addDebug(null, 10024, parse.debugOclNodes());
			String[] comments = parse.getComments();
			for(String c : comments) {
				document.result.addDebug(null, 10025, c);			
			}
		}
		
		// Output error messages, if there are ones
		if(parse.getNumberOfMessages()>0) {
			ShapeChangeResult.MessageContext messctx =
				document.result.addError(
					null, 133, ci.name(), constraintName, "class");
			if( messctx!=null ) {
				messages = parse.getMessageCollection();
				MessageCollection.Message[] msg = messages.getMessages();
				for(MessageCollection.Message m : msg) {
					final String[] del = {"/","-",","};
					String sr = m.getFormattedSourceReferences(1, 1, del);
					String ms = m.getMessageText();
					messctx.addDetail(null, 134, sr, ms);
				}
				String includeConstraintInMessages = document.options.parameter("includeConstraintInMessages");			
				if (includeConstraintInMessages!=null && includeConstraintInMessages.equals("true"))
					messctx.addDetail("Constraint: "+constraintText);
			}
		}
	}

	public OclConstraintEA(EADocument doc, PropertyInfoEA pi,
		AttributeConstraint constr) {
		
		// Record the containment links.
		document = doc;
		contextClass = pi.inClass();
		contextModelElmtType = ModelElmtContextType.ATTRIBUTE;
		contextModelElmt = pi;
		
		// The EA constraint object
		eaConstraintAttribute = constr;
		
		// Name and status. Since EA does not deliver a status for attribute
		// constraints we have to extract this from the name. Syntax is
		// 'name[status]'.
		constraintName = constr.GetName().trim();
		int ib = constraintName.indexOf("[");
		int ie = constraintName.indexOf("]", ib);
		constraintStatus = "";
		if(ib!=-1&&ie!=-1) {
			constraintStatus = constraintName.substring(ib+1,ie).trim();
			constraintName = constraintName.substring(0, ib);
		}
			
		constraintText = constr.GetNotes();
		if(constraintText!=null)
			constraintText = 
				doc.applyEAFormatting(
						constraintText);
	
		// Compiler
		syntaxTree = null;
		Readable instream = new StringReader(constraintText);
		MessageCollection messages;
		OclParser parse = new OclParser();
		
		// Parse the constraint
		document.result.addDebug(
			null, 10006, contextClass.name()+"."+pi.name(), constraintName);
		syntaxTree = 
			parse.parseOcl(instream, contextModelElmt);
		
		// Get condition type.
		if(syntaxTree!=null)
			conditionType = syntaxTree.expressionType;
		
		// Get the comments
		comments = parse.getComments();
		
		// Syntax tree as debug output + comments
		if(document.options.parameter("reportLevel").equals("DEBUG")) {
			document.result.addDebug(null, 10024, parse.debugTempNodes());
			if(syntaxTree!=null)
				document.result.addDebug(null, 10024, parse.debugOclNodes());
			String[] comments = parse.getComments();
			for(String c : comments) {
				document.result.addDebug(null, 10025, c);			
			}
		}
		
		// Output error messages, if there are ones
		if(parse.getNumberOfMessages()>0) {
			ShapeChangeResult.MessageContext messctx =
				document.result.addError(
					null, 133, contextClass.name()+"."+pi.name(), 
					constraintName, "property");
			if( messctx!=null ) {
				messages = parse.getMessageCollection();
				MessageCollection.Message[] msg = messages.getMessages();
				for(MessageCollection.Message m : msg) {
					final String[] del = {"/","-",","};
					String sr = m.getFormattedSourceReferences(1, 1, del);
					String ms = m.getMessageText();
					messctx.addDetail(null, 134, sr, ms);
				}
				String includeConstraintInMessages = document.options.parameter("includeConstraintInMessages");			
				if (includeConstraintInMessages!=null && includeConstraintInMessages.equals("true"))
					messctx.addDetail("Constraint: "+constraintText);
			}
		}
	}
}
