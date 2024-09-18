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

package de.interactive_instruments.shapechange.core.model.xmi10;

import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.model.OclConstraint;
import de.interactive_instruments.shapechange.core.model.OclConstraintImpl;
import de.interactive_instruments.shapechange.core.ocl.MessageCollection;
import de.interactive_instruments.shapechange.core.ocl.OclParser;

/**
 * <p>This implements the OclConstraint interface for the XMI 1.0
 * model platform.</p>
 * <ul>
 * <li>The concept of constraint name is not supported. The constraint text 
 * itself is used as a name.
 * <li>The concept of status is not supported at all. 
 * <li>The constraint text is found in the text string supplied to the
 * constructor function. 
 * <li>Condition type is always <i>inv:</i>.
 * </ul>
 */
public class OclConstraintXmi10 extends OclConstraintImpl implements
		OclConstraint {

	/** The model object */
	protected Xmi10Document document = null;
	
	public OclConstraintXmi10(Xmi10Document doc, ClassInfoXmi10 ci,
		String constraintText ) {
		
		// Record the containment links.
		document = doc;
		contextClass = ci;
		contextModelElmtType = ModelElmtContextType.CLASS;
		contextModelElmt = ci;
			
		// Compiler
		syntaxTree = null;
		Readable instream = new StringReader(constraintText);
		MessageCollection messages;
		OclParser parse = new OclParser();
		
		// Obtain the name of the constraint if any
		constraintName = null;
		Pattern pat = Pattern.compile("inv\\s*(\\w*)\\s*:");
		Matcher mat = pat.matcher(constraintText);
		if (mat.find()) {
			constraintName = mat.group(1);
		}
		// TODO Sometimes we need to provide the full text of a constraint for the name
		// This is the place to do it.
		if (constraintName==null || constraintName.length()==0)
			constraintName = constraintText.trim();
		this.constraintText = constraintText;

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
			if(messctx!=null) {
				messages = parse.getMessageCollection();
				MessageCollection.Message[] msg = messages.getMessages();
				for(MessageCollection.Message m : msg) {
					final String[] del = {"/","-",","};
					String sr = m.getFormattedSourceReferences(1, 1, del);
					String ms = m.getMessageText();
					messctx.addDetail(null, 134, sr, ms);
				}
			}
		}
	}
}
