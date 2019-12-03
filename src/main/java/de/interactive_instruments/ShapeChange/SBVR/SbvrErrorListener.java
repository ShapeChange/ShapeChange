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
package de.interactive_instruments.ShapeChange.SBVR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.ShapeChange.SBVR.SbvrErrorInfo.Category;


/**
 * @author Johannes Echterhoff
 *
 */
public class SbvrErrorListener extends BaseErrorListener {
		
	private List<SbvrErrorInfo> errors = new ArrayList<SbvrErrorInfo>();

	@Override
	public void syntaxError(Recognizer<?, ?> recognizer,
			Object offendingSymbol, int line, int charPositionInLine,
			String msg, RecognitionException e) {
		
		SbvrErrorInfo err = new SbvrErrorInfo();
		
		err.setErrorCategory(Category.SYNTAX_ERROR);
		err.setErrorMessage(msg);
		err.setMetadataFromToken((Token)offendingSymbol);
				
		List<String> stack = ((Parser) recognizer).getRuleInvocationStack();
		Collections.reverse(stack);
		err.setRuleInvocationStack(StringUtils.join(stack," "));
		
		/*
		 * 2015-03-30 JE: the following code would get the sbvr rule text
		 * containing the token if the input contained line breaks. However, due
		 * to SBVR rule text now being preprocessed (in
		 * SbvrConstraintImpl.setText()) to replace such whitespace, we only
		 * have a single line of text which is the whole SBVR rule.
		 */
//		CommonTokenStream tokens = (CommonTokenStream) recognizer
//				.getInputStream();
//		String input = tokens.getTokenSource().getInputStream().toString();		
//		String[] lines = input.split("\n");		
//		String errorLine = lines[line - 1];
//		err.setErrorLineText(errorLine);
		
		this.errors.add(err);
	}
		
	public boolean hasErrors() {
		return !this.errors.isEmpty();
	}
	
	public List<SbvrErrorInfo> getErrors() {
		return errors;
	}
	
	protected <T extends Token> void underlineError(
			Recognizer<T, ?> recognizer, Token offendingToken, int line,
			int charPositionInLine) {
		
		CommonTokenStream tokens = (CommonTokenStream) recognizer
				.getInputStream();
		
		String input = tokens.getTokenSource().getInputStream().toString();
		
		String[] lines = input.split("\n");
		
		String errorLine = lines[line - 1];
		
		System.out.println(errorLine);
		
		for (int i = 0; i < charPositionInLine; i++)
			System.out.print(" ");
		
		int start = offendingToken.getStartIndex();
		int stop = offendingToken.getStopIndex();
		
		if (start >= 0 && stop >= 0) {
			for (int i = start; i <= stop; i++)
				System.out.print("^");
		}
		System.out.println();
	}
}
