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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;

/**
 * <p>A MessageCollection object collects all messages obtained from parsing an
 * OCL expression.</p> 
 * <p>Message is an inner class of MessageCollection. You create Messages
 * from outside by qualified new, which causes Messages to automatically 
 * register with their MessageCollection. When creating Messages you specify a
 * message number, which refers to a text statically defined in this class 
 * construct. These texts may contain substitution points of the form %number%,
 * which can be substituted on Message objects by String values. You can also
 * attach a severity code and source references to a Message. The default 
 * severity code is MessageCollection.Severity.ERROR.</p>
 * <p>The state of MessageCollections can be captured and restored by means
 * of a pair of methods - <i>captureState()</i> and <i>restoreState()</i>.</p>
 * <p></p> 
 * 
 * @version 0.1
 * @author Reinhard Erstling (c) interactive instruments GmbH, Bonn, Germany
 */

public class MessageCollection {
	
	/**
	 * The Severity enum class exposes two members, which stand for different 
	 * levels of severity of diagnostics regarding the OCL translation.
	 */
	public enum Severity {
		WARNING,	// A finding which may lead to undesired behavior
		ERROR,		// A syntactical error
		SYSTEMERROR	// A compiler or code generator error
	}
	
	ArrayList<Message> messages = new ArrayList<Message>();
		
	/**
	 * <p>Message objects stand for messages from the OCL compilation process.
	 * They consist of an message number, an associated message text,
	 * which contains substitution points, a severity code and a collection of 
	 * source code references.</p>
	 * <p>The default severity code of a message is ERROR, except for those
	 * messages, which stand for a compiler malfunction. In the latter case
	 * the code SYSTEMERROR is chosen.</p>
	 * <p>A Message object is always part of a MessageCollection. You have to
	 * create Messages by means of the new operator, qualified by the associated 
	 * MessageCollection object.</p> 
	 */
	public class Message {
		int messageNo;
		String messageText;
		Severity severity = Severity.ERROR;
		ArrayList<SourceReference> sourceRef 
			= new ArrayList<SourceReference>( 1 );
		/**
		 * Create a message by message number. You have to use a qualified
		 * new for using this constructor.
		 * @param messNo Message number
		 */
		Message( Integer messNo ) {
			messageNo = messNo;
			messageText = messageTexts.get( messNo );
			if( messageText==null ) {
				messageText = unknown;
				messageNo = 0;
				this.substitute( 1, messNo.toString() );
			}
			messages.add( this );
			if( messageNo==0 ) severity = Severity.SYSTEMERROR;
		}
		/**
		 * Substitution points of the message will be replaced by means of this
		 * function.
		 * @param pos Number of the substitution point 1, 2, ...
		 * @param text Text to be substituted
		 */
		void substitute( Integer pos, String text ) {
			String quotedText = Matcher.quoteReplacement(text); 
			messageText = messageText.replaceAll( 
				"%"+pos.toString()+"%", quotedText );
		}
		/**
		 * This function adds a source reference to a Message object.
		 * @param sourceref SourceReference object
		 */
		void addSourceReference( SourceReference sourceref ) {
			sourceRef.add( sourceref );
		}
		
		/**
		 * This function adds an array of source references to a Message
		 * object.
		 * @param sourcerefs Array of source references or null
		 */
		void addSourceReferences( SourceReference[] sourcerefs ) {
			if( sourcerefs==null )
				return;
			for( SourceReference sr : sourcerefs )
				addSourceReference( sr );
		}
		
		/**
		 * This method lets you inquire the source code references of the
		 * message as an array of SourceReference objects.
		 * @return Array of SourceReference objects
		 */
		public SourceReference[] getSourceReferences() {
			SourceReference[] sr = new SourceReference[sourceRef.size()];
			sr = sourceRef.toArray( sr );
			return sr;
		}
		
		/**
		 * This function lets you set the severity code. You need not set the
		 * severity of a message if Severity.ERROR is o.k.
		 * @param severity The severity of the message.
		 */
		void setSeverity( Severity severity ) {
			this.severity = severity;
		}
		
		/**
		 * Inquire the severity of a Message by means of this method.
		 * @return Severity of message
		 */
		public Severity getSeverity() {
			return severity;
		}
		
		/**
		 * This method returns the text of the message object.
		 * @return Message text as String object
		 */
		public String getMessageText() {
			return messageText;
		}
		
		/**
		 * <p>Use this method to obtain a formatted representation of the source
		 * references of a message.</p>
		 * <p>The format will be: <i>line/col1:col2,line/col1:col2,...</i>, 
		 * where "/", ":" and "," can be defined with other strings in index 0, 
		 * 1 and 2 of the delims array.</p>
		 * @param offLine Offset to add to line numbers, originally counting 
		 * from 0.
		 * @param offCol Offset to add to column numbers, originally counting
		 * from 0.
		 * @param delims Array of Strings defining delimiters "/", ":" and ",".
		 * @return Formatted string
		 */
		public String getFormattedSourceReferences(
			int offLine, int offCol, String[] delims ) {
			String sr = "";
			boolean first = true;
			for( SourceReference s : sourceRef ) {
				int l = s.getLineNumber() + offLine;
				int cf = s.getColumnFrom() + offCol;
				int ct = s.getColumnTo() + offCol;
				if(!first)
					sr += ((delims.length>2)?delims[2]:",");
				first = false;
				sr += String.valueOf(l) + ((delims.length>0)?delims[0]:"/");
				sr += String.valueOf(cf);
				if(cf!=ct) 
					sr += ((delims.length>1)?delims[1]:":")
						+ String.valueOf(ct);
			}
			return sr;
		}
	}

	/**
	 * <p>A Checkpoint encapsulates a state of a MessageCollection, which you 
	 * can capture and restore by means of the captureState and restoreState 
	 * methods.</p> 
	 */
	static class Checkpoint {
		private int checkpoint_index;
	}
	
	/**
	 * <p>This method returns the current filling state of the MessageCollection
	 * encapsulated in a Checkpoint object. You can later restore to this 
	 * former state by means of the restoreState method.</p>
	 * @return Checkpoint object
	 */
	Checkpoint captureState() {
		Checkpoint chp = new Checkpoint();
		chp.checkpoint_index = messages.size();
		return chp;
	}
	
	/**
	 * <p>This method releases the checkpoint once captured by means of method
	 * captureState().</p>
	 * @param chp
	 */
	void releaseState( Checkpoint chp ) {
		// Nothing to do ...
	}
	
	/**
	 * <p>The restoreState method restores the filling state of the 
	 * MessageCollection to a former state once captured by means of the
	 * method captureState().</p>
	 * @param chp Former state given as a Checkpoint object
	 */
	void restoreState( Checkpoint chp ) {
		messages.subList( chp.checkpoint_index, messages.size() ).clear();
	}
	
	/**
	 * <p>Inquire all messages from the MessageCollection object with this 
	 * method.</p>
	 * @return Array of Messages.
	 */
	public Message[] getMessages() {
		Message[] messes = new Message[messages.size()];
		messes = messages.toArray( messes );
		return messes;		
	}
	
	/**
	 * <p>Inquire number of messages in this MessageCollection.</p>
	 * @return Number of messages in collection
	 */
	public int getNumberOfMessages() {
		return messages.size();
	}
		
	// Static initialisations of message texts ...
	static HashMap<Integer,String> messageTexts = new HashMap<Integer,String>();
	static {
		messageTexts.put( 0, "System error: Assertion failed [%1%]" );
		messageTexts.put( 1, "Unrecognized syntax [%1%] encountered and ignored." );
		messageTexts.put( 2, "Closing bracket ')' expected preceding %1% token, assumed." );
		messageTexts.put( 3, "Identifier, literal or bracketed expression expected preceding %1% token. Invalid assumed." );
		messageTexts.put( 4, "Name expected as part of a scoped identifier. Found %1%, ignored." );
		messageTexts.put( 5, "Operator %1% expected as part of if-construct. Found %2%, skipping to endif" );
		messageTexts.put( 6, "End of text encountered while skipping for operator." );
		messageTexts.put( 7, "Operator %1% expected as part of let-construct. Found %2%, ignoring construct." );
		messageTexts.put( 8, "Complex literal of type %1% not implemented, ignoring construct." );
		messageTexts.put( 9, "Opening bracket '(' expected in initializer of %1% literal." );
		messageTexts.put( 10, "Closing bracket ')' expected preceding %1% token, assumed." );
		messageTexts.put( 11, "Date constructor argument must be string constant complying to ISO8601 syntax." );
		messageTexts.put( 12, "End of text encountered while skipping for paired bracket." );
		messageTexts.put( 13, "Name expected as mandatory part of variable declaration preceding '%1%' token, ignored." );
		messageTexts.put( 14, "Extra tokens following OCL expression ignored, starting with '%1%'." );
		messageTexts.put( 15, "Condition keyword (like inv:) expected introducing expression. Found '%1%', ignored." );
		messageTexts.put( 16, "Colon ':' missing following condition keyword. Found '%1%'." );
		messageTexts.put( 21, "Class named '%1%' not found in model.");
		messageTexts.put( 22, "Class named '%1%' not found in package '%2%'." );
		messageTexts.put( 23, "Package named '%1%' not found in model.");
		messageTexts.put( 24, "Package named '%1%' not found in package '%2%'." );
		messageTexts.put( 25, "Right hand side of referencing operation '%1%' must be identifier." );
		messageTexts.put( 26, "Attribute named '%1%' not available on class '%2%'." );
		messageTexts.put( 27, "Operation named '%1%' (%3% non-collection arguments) not available on class '%2%'." );
		messageTexts.put( 28, "Set properties are not available on class 'class'." );
		messageTexts.put( 29, "Variable declaration syntax not permitted on non-iterative operation '%1%'." );
		messageTexts.put( 30, "Scope of identifier '%1%' does not designate same or super class of '%2%'." );
		messageTexts.put( 31, "Excess binding variable declarations on iterator '%1%'." );
		messageTexts.put( 32, "Operation named '%1%' (%2% arguments) not available on any implicit variable in scope." );
		messageTexts.put( 33, "Value '%1%' not found in Enumeration or Codelist '%2%'." );
		messageTexts.put( 34, "Identifier named '%1%' could not be resolved to either a variable, a property on an implicit variable, a class or an enumeration." );
		messageTexts.put( 35, "Condition part of if-clause must be of Boolean type." );
		messageTexts.put( 36, "Common supertype of type '%1%' of if-part and and type '%2%' of else-part could not be determined." );
		messageTexts.put( 37, "Declaration of variable '%1%' in let-construct does not exhibit type information." );
		messageTexts.put( 38, "Type name '%1%' in declaration of variable '%2%' does not correspond to any known type." );
		messageTexts.put( 39, "Declared type '%1%' and type of initializer '%2%' are not compatible in declaration of variable '%3%'." );
		messageTexts.put( 40, "Type implied by context '%1%' and declared type '%2%' are not compatible in declaration of variable '%3%'." );
		messageTexts.put( 41, "Type of 'invariant' condition must be 'Boolean', type determined is '%1%'." );
		messageTexts.put( 42, "Type of 'derive' or 'init' condition must correspond to the type '%1%' of its context property, type determined is '%2%'." );
		messageTexts.put( 43, "Infix operation '%1%' is not available for operand types '%2%' and '%3%'." );
		messageTexts.put( 44, "Prefix operation '%1%' is not available for operand of type '%2%'." );
		messageTexts.put( 45, "Collection type result is not permitted for an 'invariant' condition." );
		messageTexts.put( 46, "A collection type of a 'derive' or 'init' condition result requires a context property, which has a cardinality greater 1." );
		messageTexts.put( 47, "Operation propertyMetadata() is invoked on a property or variable '%1%'. Either a metadata type is not declared for the corresponding property via tagged value 'metadataType' or that type cannot be found in the model." );
	}
	static String 
		unknown = "System error: Unknown message number %1% encountered.";
}