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

package de.interactive_instruments.shapechange.core.ocl;

import java.util.HashMap;

/**
 * <p>A Token object stands for a single unit of information from an OCL 
 * expression, like an identifier or a number or bracket or a ->. Tokens are
 * auxiliary objects, which are provided to the Parser object by the Lexer
 * object.</p>  
 * <p>Token itself is abstract and comes in 6 concrete flavors, namely
 * Identifier (carrying all sort of names except reserved words), Text 
 * (containing quoted literal material - without the quotes), Number (for integer 
 * and floating point constants), Operator (carrying nothing except the type of 
 * operator, which also includes all reserved words), Comment (transporting the 
 * payload of OCL end-of-line comments) and Illegal (containing 
 * unrecognized items).</p>
 * <p>Token objects carry a sourceReference object consisting of a line number 
 * and a column number pair.</p> 
 * @see SourceReference  
 * 
 * @version 0.1
 * @author Reinhard Erstling (c) interactive instruments GmbH, Bonn, Germany
 */

abstract class Token {

	/**
	 * All Token objects have a Type (realized by an enum), which may be 
	 * inquired.
	 */

	enum Type {
		IDENTIFIER,		// Alphanumeric (+ _) string starting with letter or _
		TEXT,			// Anything enclosed in quotes, quotes removed
		NUMBER,			// Integer or floating point number, no sign included
		COMMENT,		// Comment
		O_BRACKET,		// ( - all of the following except ILLEGAL are operators
		C_BRACKET,		// ) - ...
		O_SQ_BRACKET,	// [
		C_SQ_BRACKET,	// ]
		O_CU_BRACKET,	// {
		C_CU_BRACKET,	// }
		BAR,			// |
		COMMA,			// ,
		SEMICOLON,		// ;
		COLON,			// :
		DOUBLE_COLON,	// ::
		EQUAL,			// =
		NOT_EQUAL,		// <>
		LESS,			// <
		GREATER,		// >
		LESS_EQUAL,		// <=
		GREATER_EQUAL,	// >=
		PLUS,			// +
		MINUS,			// -
		ASTERISK,		// *
		SLASH,			// /
		DOT,			// .
		ARROW,			// ->
		AND,			// and			} Note: This is the complete set of
		OR,				// or			} OCL's reserved words. 
		XOR,			// xor			}   
		NOT,			// not			} 
		IMPLIES,		// implies		}
		IF,				// if			}
		THEN,			// then			}
		ELSE,			// else			}
		ENDIF,			// endif		}
		LET,			// let			}
		IN,				// in			}
		CONTEXT,		// context		}
		INV,			// inv			}
		ATTR,			// attr			} The ones from here on are recognized
		DEF,			// def			} as reserved words, but are never used
		ENDPACKAGE,		// endpackage	} in this implementation.
		OPER,			// oper			}
		PACKAGE,		// package		}
		POST,			// post			}
		PRE,			// pre			}
		INIT,			// init 		}
		DERIVE,			// derive		}
		BODY,			// body			}
		END_OF_TEXT,	// Assumed token indicating the end of the expression
		ILLEGAL			// unrecognized character or character sequence
	}	
	
	/**
	 * <p>This static function translates the lexical representations of token 
	 * types into the formalized enum representation. For example, "::" is
	 * translated into Token.Type.DOUBLE_COLON.</p>
	 * <p>The following types are not supported, because there is no unique
	 * lexical representation:</p>
	 * <ul>
	 * <li>Token.Type.IDENTIFIER
	 * <li>Token.Type.TEXT
	 * <li>Token.Type.NUMBER
	 * <li>Token.Type.END_OF_TEXT
	 * <li>Token.Type.ILLEGAL
	 * </ul>
	 * @param lexstr Lexical String type representation
	 * @return Equivalent Type enum value
	 */
	static Type getTypeFromLexicalString( String lexstr ) {
		return fixedTokens.get( lexstr );
	}
	
	/**
	 * <p>This static function translates the formalized enum representation
	 * of a token type to the lexical representation as a String. For example, 
	 * Token.Type.COMMA is translated into ",".</p>
	 * <p>As in the reverse method GetTypeFromLexicalString the following types
	 * are not supported, because there is no unique lexical representation:</p>
	 * <ul>
	 * <li>Token.Type.IDENTIFIER
	 * <li>Token.Type.TEXT
	 * <li>Token.Type.NUMBER
	 * <li>Token.Type.END_OF_TEXT
	 * <li>Token.Type.ILLEGAL
	 * </ul>
	 * @param type Enum Type representation
	 * @return Equivalent lexical representation as a String value
	 */
	static String getLexicalStringFromType( Type type ) {
		return fixedStrings.get( type );
	}
	
	// The following creates the setup of maps necessary for the pair of 
	// translation functions above.
	
	// A static map to translate fixed tokens into Token codes. 
	static private HashMap<String,Token.Type> 
		fixedTokens = new HashMap<String,Token.Type>();
	// A static map to translate Token codes into fixed Strings.
	static private HashMap<Token.Type,String>
		fixedStrings = new HashMap<Token.Type,String>();
	// A private static function, feeding its input into both HashMaps.
	static private void feedMaps( String lexstr, Type type ) {
		fixedTokens.put( lexstr, type );
		fixedStrings.put( type, lexstr );
	}
	// Now, the static initializer block to feed the maps ...
	static {
		feedMaps( "(", Token.Type.O_BRACKET );	
		feedMaps( ")", Token.Type.C_BRACKET );		
		feedMaps( "[", Token.Type.O_SQ_BRACKET );	
		feedMaps( "]", Token.Type.C_SQ_BRACKET );
		feedMaps( "{", Token.Type.O_CU_BRACKET );
		feedMaps( "}", Token.Type.C_CU_BRACKET );
		feedMaps( "|", Token.Type.BAR );
		feedMaps( ",", Token.Type.COMMA );			
		feedMaps( ";", Token.Type.SEMICOLON );
		feedMaps( ":", Token.Type.COLON );	
		feedMaps( "::", Token.Type.DOUBLE_COLON );	
		feedMaps( "=", Token.Type.EQUAL );			
		feedMaps( "<>", Token.Type.NOT_EQUAL );		
		feedMaps( "<", Token.Type.LESS );			
		feedMaps( ">", Token.Type.GREATER );		
		feedMaps( "<=", Token.Type.LESS_EQUAL );
		feedMaps( ">=", Token.Type.GREATER_EQUAL );	
		feedMaps( "+", Token.Type.PLUS );			
		feedMaps( "-", Token.Type.MINUS );
		feedMaps( "*", Token.Type.ASTERISK );		
		feedMaps( "/", Token.Type.SLASH );			
		feedMaps( ".", Token.Type.DOT );			
		feedMaps( "->", Token.Type.ARROW );			
		feedMaps( "and", Token.Type.AND );			
		feedMaps( "or", Token.Type.OR );				 
		feedMaps( "xor", Token.Type.XOR );			
		feedMaps( "not", Token.Type.NOT );			
		feedMaps( "implies", Token.Type.IMPLIES );		
		feedMaps( "if", Token.Type.IF );				
		feedMaps( "then", Token.Type.THEN );			
		feedMaps( "else", Token.Type.ELSE );			
		feedMaps( "endif", Token.Type.ENDIF );
		feedMaps( "let", Token.Type.LET );			
		feedMaps( "in", Token.Type.IN );	
		feedMaps( "context", Token.Type.CONTEXT );
		feedMaps( "inv", Token.Type.INV );
		feedMaps( "attr", Token.Type.ATTR );
		feedMaps( "def", Token.Type.DEF );
		feedMaps( "endpackage", Token.Type.ENDPACKAGE );
		feedMaps( "oper", Token.Type.OPER );
		feedMaps( "package", Token.Type.PACKAGE );
		feedMaps( "post", Token.Type.POST );
		feedMaps( "pre", Token.Type.PRE );
		feedMaps( "init", Token.Type.INIT );
		feedMaps( "derive", Token.Type.DERIVE );
		feedMaps( "body", Token.Type.BODY );
	}	
	
	// From here non-static material ...
	
	// Type of a Token object
	protected Type type;
	
	// Serial number of Token in token input sequence
	protected short serialNumber;
	
	// Source reference of a Token object
	private SourceReference sourceReference;
	
	/**
	 * <p>This function lets you inquire the Type of the Token object.</p>
	 * @return Type of the Token object.
	 */
	Type getType() {
		return type;
	}
	
	/**
	 * <p>The function delivers the serial number of the Token object in
	 * the token input sequence. Neighbored Tokens have adjacent serial
	 * numbers.</p>
	 * @return Serial number of the Token object
	 */
	int getSerialNumber() {
		return serialNumber;
	}
	
	/**
	 * <p>This function returns a clear text denotation indicating the
	 * characteristic of the Token. It is used for formulating readable
	 * diagnostics.</p>
	 * @return A clear text term indicating the nature of this Token.
	 */
	abstract String getDenotation();
	
	/**
	 * <p>This function is for setting the source code reference for a Token by
	 * means of distinct values for line and column range.</p>
	 * @param lineno  Line number of Token counting from zero.
	 * @param colFrom First column of Token in line counting from zero.
	 * @param colTo   Last column of Token counting from zero.
	 */
	void setSourceReference( 
		short lineno, short colFrom, short colTo ) {
		sourceReference = new SourceReference( 
			lineno, colFrom, colTo, serialNumber, serialNumber );
	}

	/**
	 * <p>This function inquires the SourceReference object from a Token.</p>
	 * @return SourceReference object of the Token
	 */
	SourceReference getSourceReference() {
		return sourceReference;
	}
	
	/**
	 * <p>Identifier token objects contain the name of the recognized 
	 * identifier. OCL Reserved words are not presented as Identifiers - 
	 * they comes as Operators.</p>
	 */
	
	static class Identifier extends Token {
		private String name;
		/**
		 * This implements the abstract function on Token.
		 */
		String getDenotation() {
			return "identifier \"" + name + "\"";
		}
		/**
		 * This function returns the name of the Identifier token.
		 * @return Name of Identifier token.
		 */
		String getName(){
			return name;
		}
		/**
		 * Constructs and initializes an Identifier token.
		 * @param serial Serial number of Token in input stream
		 * @param name Name of Identifier token.
		 */
		Identifier( short serial, String name ) {
			this.name = name;
			this.type = Type.IDENTIFIER;
			this.serialNumber = serial;
		}
	}

	/**
	 * <p>Text tokens stand for text literals and let you access the value of 
	 * the text literal, which comes stripped from the enclosing quotes.</p> 
	 */
	
	static class Text extends Token {
		private String value;
		/**
		 * This implements the abstract function on Token.
		 */
		String getDenotation() {
			int lv = value.length();
			return 
				"string literal \"" + 
				value.substring(0,(lv>15?15:lv)) +
				(lv>15?"...":"") +
				"\"";
		}
		/**
		 * This function returns the value of the Text token.
		 * @return Value of Text token without enclosing quotes.
		 */
		String getValue(){
			return value;
		}
		/**
		 * Constructs and initializes a Text token.
		 * @param serial Serial number of Token in input stream
		 * @param value Value of Text token. Must come without quotes.
		 */
		Text( short serial, String value ) {
			this.value = value;
			this.type = Type.TEXT;
			this.serialNumber = serial;
		}
	}
	
	/**
	 * <p>Number tokens represent floating point or integer numbers. The value 
	 * and whether the value is integral can be inquired.</p>
	 */
	
	static class Number extends Token {
		private double value;
		private String stringValue;
		private boolean isInteger;
		/**
		 * This implements the abstract function on Token.
		 */
		String getDenotation() {
			Double val = value;
			if( isInteger )
				return "integer number '" + Long.toString(val.longValue()) + "'";
			else
				return "floating point number '" + val.toString() + "'";
		}
		/**
		 * This function returns the value of the Number token.
		 * @return Value of the Number token.
		 */
		double getValue(){
			return value;
		}
		/**
		 * <p>This function returns the value of the Number token as a string.
		 * The string is as originally specified in the input.</p> 
		 * @return Value of the Number token as specified in the input.
		 */
		String getStringValue() {
			return stringValue;
		}
		/**
		 * <p>This function returns a flag indicating whether the Number token 
		 * contains an integral value.</p>
		 * @return Value-is-integral flag.
		 */
		boolean isInteger() {
			return isInteger;
		}
		/**
		 * <p>Constructs and initializes a Number token.</p>
		 * @param serial Serial number of Token in input stream
		 * @param value Value of Number token, given as String.
		 * @param isInteger Value-is-integral flag. Must be specified truthfully.
		 */
		Number( short serial, String value, boolean isInteger ) {
			this.stringValue = value;
			this.value = Double.parseDouble( value );
			this.isInteger = isInteger;
			this.type = Type.NUMBER;
			this.serialNumber = serial;
		}
	}

	/**
	 * <p>Operator tokens stand for everything characterized by its token type
	 * alone. There is no recognized value attached to this kind.</p>
	 */

	static class Operator extends Token {
		/**
		 * This implements the abstract function on Token.
		 */
		String getDenotation() {
			if( type==Type.END_OF_TEXT )
				return "end of text";
			else
				return "operator \"" + getLexicalStringFromType( type ) + "\"";
		}
		/**
		 * Constructs and initializes an Operator token.
		 * @param serial Serial number of Token in input stream
		 * @param type Type of the Token to be constructed.
		 */
		Operator( short serial, Type type ) {
			this.type = type;
			this.serialNumber = serial;
		}
	}

	/**
	 * <p>Comment tokens stand for the content of comments encountered during 
	 * lexical analysis of the OCL expression. The token lets you access the 
	 * payload of the comment, which is the trimmed content following the --
	 * up to but not including the following newline.</p> 
	 */
	
	static class Comment extends Token {
		private String value;
		/**
		 * This implements the abstract function on Token.
		 */
		String getDenotation() {
			int lv = value.length();
			return 
				"comment \"" + 
				value.substring(0,(lv>15?15:lv)) +
				(lv>15?"...":"") +
				"\"";
		}
		/**
		 * This function returns the trimmed value of the Comment token.
		 * @return Value of Comment token 
		 */
		String getValue(){
			return value;
		}
		/**
		 * Constructs and initializes a Comment token.
		 * @param serial Serial number of Token in input stream
		 * @param value Value of Comment token including --
		 */
		Comment( short serial, String value ) {
			if(value.startsWith("--")) 
				value = value.substring(2);
			this.value = value.trim();
			this.type = Type.COMMENT;
			this.serialNumber = serial;
		}
	}
	
	/**
	 * <p>Illegal tokens result from finding unrecognized characters in the 
	 * input stream.</p> 
	 */
	static class Illegal extends Token {
		private String item;
		/**
		 * This implements the abstract function on Token.
		 */
		String getDenotation() {
			return "illegal characters";
		}
		/** 
		 * <p>This function returns the illegal character sequence of the 
		 * Illegal token.</p>
		 * @return Objected item.
		 */
		String getItem(){
			return item;
		}
		/** 
		 * <p>Constructs and initializes an Illegal token.</p>
		 * @param serial Serial number of Token in input stream
		 * @param item Objected item.
		 */
		Illegal( short serial, String item ) {
			this.item = item;
			this.type = Type.ILLEGAL;
			this.serialNumber = serial;
		}
	}
}
