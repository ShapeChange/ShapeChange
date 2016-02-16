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

package de.interactive_instruments.ShapeChange.Ocl;

import java.util.Scanner;
import java.util.Stack;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * <p>A Lexer object reads characters from an input stream, generating a sequence 
 * of Token objects, which correspond to low level constructs of OCL (actually, 
 * a subset of OCL). For the definition of Token objects see class Token.</p>
 *
 * @see Token 
 * 
 * @version 0.1
 * @author Reinhard Erstling (c) interactive instruments GmbH, Bonn, Germany
 */

public class Lexer {
	
	private Scanner inScan;
	private short serialNumber = 0;
	private boolean atStart = true;
	private Token eotToken = null;
	private short lineno = 0; 
	private short baseOffset = 0;
	private Stack<Token> unfetchStack = new Stack<Token>();
	private Stack<Token> recordingStack = new Stack<Token>();
	private int checkpointCount = 0;
	private int commentStack = 0;


	/**
	 * The following pattern controls recognition of all OCL tokens except
	 * reserved words.
	 */
	static private String scanRegexJava6 = 
		"\\s*(" +				
			"(--.*)|" +				// Line Comment
			"(/\\*\\s*)|" +			// Start of a Paragraph Comment
			"([a-zA-Z_]\\w*)|" +	// Identifier
			"((?:\\d+\\.\\d*|\\d*\\.\\d+)(?:[eE][+-]?\\d+)?|\\d+[eE][+-]?\\d+)|" +
			"(\\d+)|" +				// Last: Floating Point, this: Integer
			"('(?:[^']|'')*')|" +	// String Literal '...''...'
			"\\(|\\)|" + 
			"\\[|\\]|" + 
			"\\{|\\}|" + 
			"=|<>|<=|>=|<|>|" +
			"->|\\." + 
			"\\+|-|\\*|/|" + 
			"\\||,|;|::|:|" +
			"\\S" + 				// Illegal material
		")\\s*";
	static private String scanRegexCommentJava6 =
		"(" +					// Activate Unicode support
			"(.*\\*/)|" +			// Anything up to the end of the comment
			"(.*/\\*)|" +			// Anything up to the start of a nested one
			"(.*)" + 				// Anything at all
		")";
	static private String scanRegexJava7 = 
			"(?U)\\s*(" +				// Activate Unicode support
				"(--.*)|" +				// Line Comment
				"(/\\*\\s*)|" +			// Start of a Paragraph Comment
				"([\\p{Alpha}_$]\\w*)|" +	// Identifier
				"((?:\\d+\\.\\d*|\\d*\\.\\d+)(?:[eE][+-]?\\d+)?|\\d+[eE][+-]?\\d+)|" +
				"(\\d+)|" +				// Last: Floating Point, this: Integer
				"('(?:[^']|'')*')|" +	// String Literal '...''...'
				"\\(|\\)|" + 
				"\\[|\\]|" + 
				"\\{|\\}|" + 
				"=|<>|<=|>=|<|>|" +
				"->|\\." + 
				"\\+|-|\\*|/|" + 
				"\\||,|;|::|:|" +
				"\\S" + 				// Illegal material
			")\\s*";
	static private String scanRegexCommentJava7 =
			"(?U)(" +					// Activate Unicode support
				"(.*\\*/)|" +			// Anything up to the end of the comment
				"(.*/\\*)|" +			// Anything up to the start of a nested one
				"(.*)" + 				// Anything at all
			")";
	static String javaVersion = System.getProperty("java.version");
	static char major = javaVersion.charAt(0);
	static char minor = javaVersion.charAt(2);
	
	static private Pattern scanPattern = 
			(major == '1' && minor == '6' ? 
			Pattern.compile( scanRegexJava6, Pattern.MULTILINE ): 
			Pattern.compile( scanRegexJava7, Pattern.MULTILINE ));
	static private Pattern scanPatternComment =
			(major == '1' && minor == '6' ? 
			Pattern.compile( scanRegexCommentJava6, Pattern.MULTILINE ):
			Pattern.compile( scanRegexCommentJava7, Pattern.MULTILINE ));
	static private Pattern doubleQuotes = Pattern.compile( "''" );
	
	/**
	 * <p>The Lexer object is created with a Readable to fetch the input characters
	 * from. It is important that the input is delivered by the Readable in 
	 * single character buffers reflecting lines of the original input. The 
	 * Token stream returned from the Lexer will refer to line numbers and column
	 * on these, which are subsequently used in the parser to generate 
	 * appropriate error messages.</p>
	 * @param inStream Readable to fetch the input from.
	 */
	public Lexer( Readable inStream ) {
		inScan = new Scanner( inStream );
	}
	
	/**
	 * <p>You fetch Tokens from the Lexer object one by one by means of the 
	 * fetchToken function. See the definition of the Token class to understand 
	 * the nature of Tokens. The last Token conceptually received is an 
	 * Operator token of Type END_OF_TEXT. All further invocations of fetchToken 
	 * will result in this kind of Token object.</p>
	 * <p>If there are "unfetched" Tokens, these will be returned prior to the
	 * ones recognized from the input stream. Unfetched Tokens come in a 
	 * last-in-first-out sequence.</p>
	 * @return Next Token
	 */
	public Token fetchToken() {
		
		Token token = fetchTokenProper();
		if( checkpointCount>0 ) recordingStack.push( token );
		return token;
	}

	// The following private method is the fetchToken method proper, which does
	// the real work. It is called from the externally visible method, which
	// adds the functionality required for setting and restoring checkpoints.
	private Token fetchTokenProper() {
		
		// First check if something has been unfetched. If so, use this.
		if( !unfetchStack.isEmpty() )
			return unfetchStack.pop();

		// Did we check on the existence of a first line?
		if( atStart ) {
			atStart = false;
			if( !inScan.hasNextLine() ) {
				eotToken = new Token.Operator( 
					serialNumber, Token.Type.END_OF_TEXT );
				eotToken.setSourceReference( (short)0, (short)0, (short)0 );
			}
		}
		
		// If we already hit the end just keep saying so.
		if( eotToken!=null )
			return eotToken;
		
		// Fair enough! We will have to figure out the next one from
		// Java's Scanner object ...
		Token token = null;
		String line;
		short start, end = -1;
		String comment = "";
		do {
			// Which pattern?
			Pattern pattern = 
				commentStack==0 ? scanPattern : scanPatternComment;
			
			// Try to scan the next token in the current line 
			if( ( line=inScan.findInLine( pattern ) ) != null ) 
			{
				// Found one. Find out where in the input we found it
				MatchResult match = inScan.match();
				line = match.group( 1 );
				start = (short) match.start( 1 );
				end = (short) ( match.end( 1 ) - 1 );
				
				// Normal or Comment ?
				if( commentStack>0 ) {
					
					// Digging through a possibly nested paragraph comment ...
					
					if( match.end(2)-match.start(2)>0 ) {
						// Running into an end of a comment - may be nested
						if( --commentStack==0 ) {
							token = new Token.Comment( 
								serialNumber++, comment + line );
							comment = "";
						} else {
							comment += line;
							continue;
						}
					} else if( match.end(3)-match.start(3)>0 ) {
						// A nested comment has been started
						commentStack++;
						comment += line;
						continue;
					} else {
						// Must be comment extending to the end of the line
						token = new Token.Comment( 
								serialNumber++, comment + line );
						comment = "";
					}
					
				} else {
					
					// Normal processing of tokens ...
					
					// Construct Token object depending on type
					token = null;
					Token.Type type = Token.getTypeFromLexicalString( line );
					if( type != null )
						// Found among the list of fixed tokens. Must be Operator
						token = new Token.Operator( serialNumber++, type );
					else if( match.end(2)-match.start(2)>0 )
						// A line comment.
						token = new Token.Comment( serialNumber++, line); 
					else if( match.end(3)-match.start(3)>0 ) {
						// Start of a paragraph comment
						commentStack++; comment = line;
						continue;
					} else if( match.end(4)-match.start(4)>0 )
						// Must be Identifier, because reserved words have already 
						// been seen.
						token = new Token.Identifier( serialNumber++, line );
					else if( match.end(5)-match.start(5)>0 )
						// Must be Floating Point
						token = new Token.Number( 
							serialNumber++, line, false );
					else if( match.end(6)-match.start(6)>0 )
						// So this must be an Integer.
						token = new Token.Number( 
							serialNumber++, line, true );
					else if( match.end(7)-match.start(7)>0 ) {
						// This covers String literals '...'
						line = line.substring( 1, line.length()-1 );
						token = new Token.Text( 
							serialNumber++,
							doubleQuotes.matcher( line ).replaceAll( "'" ) );
					}
					else
						// Must be illegal
						token = new Token.Illegal( serialNumber++, line );
				}
				// Return the Token determined
				token.setSourceReference( 
					lineno, (short)(start /*-baseOffset*/), 
					(short)(end /*-baseOffset*/) );
				return token;
			}
			
			// Skip to next line
			if( inScan.hasNextLine() ) {
				inScan.nextLine();
				baseOffset = (short) inScan.match().end();
				lineno++; end = -1;
			}
			
		} while( inScan.hasNextLine() );

		// We hit the bottom. No more to come ...
		eotToken = new Token.Operator( serialNumber, Token.Type.END_OF_TEXT );
		eotToken.setSourceReference( 
			lineno, (short)(end+1/*-baseOffset*/), (short)(end+1/*-baseOffset*/) );
		return eotToken;
	}

	
	/**
	 * <p>Tokens fetched in excess can be pushed back to the Lexer object by 
	 * means of the unfetchToken function. An arbitrary number of tokens can be 
	 * unfetched.</p>
	 * <p>Warning: Generally, no check can be done, whether the Tokens pushed 
	 * back are the same as previously delivered. Do not regard this to be a 
	 * feature. If a Checkpoint is active, however, recording of delivered
	 * Tokens takes place. In this case an <b>assert</b> makes indeed sure that 
	 * Tokens are unfetched exactly as delivered.</p>
	 * @param token Token to be unfetched.
	 */
	public void unfetchToken( Token token ) {
		assert 
			checkpointCount<=0 || 
			( ! recordingStack.isEmpty() && recordingStack.peek()==token )
			: "Token recording stack is out of sync.";
		if( checkpointCount>0 ) recordingStack.pop();
		unfetchStack.push( token );
	}

	/**
	 * A Checkpoint encapsulates a state of a Lexer object, which you can
	 * capture and restore by means of the captureState and restoreState 
	 * methods. 
	 */
	public static class Checkpoint {
		private int checkpoint_index;
	}
	
	/**
	 * <p>This method returns the current state of Token delivery of the Lexer
	 * encapsulated in a Checkpoint object. You can later restore to this 
	 * former state by means of the restoreState() method. If you do not restore
	 * you need to release the Checkpoint by calling releaseState().</p>
	 * @return Checkpoint object
	 */
	public Checkpoint captureState() {
		Checkpoint chp = new Checkpoint();
		chp.checkpoint_index = recordingStack.size();
		checkpointCount++;
		return chp;
	}

	/**
	 * <p>This method releases the checkpoint once captured by means of method
	 * captureState().</p>
	 * @param chp
	 */
	public void releaseState( Checkpoint chp ) {
		// Control checkpoint nesting ...
		--checkpointCount;
		// If this disables checkpointing altogether, clear the recording
		// stack ...
		assert checkpointCount>=0 : "Lexer checkpointing out of balance.";
		if( checkpointCount==0 )
			recordingStack.clear();
	}	
	
	/**
	 * <p>The restoreState method restores the state of Token delivery of the 
	 * Lexer to a former state once captured by means of the method
	 * captureState().</p>
	 * @param chp Former state given as a Checkpoint object
	 */
	public void restoreState( Checkpoint chp ) {
		for( int i=recordingStack.size(); i>chp.checkpoint_index; --i )
			unfetchToken( recordingStack.peek() );
		--checkpointCount;
	}

}
