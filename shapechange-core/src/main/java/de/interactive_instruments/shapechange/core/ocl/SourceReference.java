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

/**
 * A SourceReference object stands for a distinct source reference in a line of
 * OCL code. SourceReference objects are initially created in the Lexer, they
 * are, however, kept and maintained in higher value syntactical constructs 
 * until they are finally used in diagnostic messages. 
 * <br>All reference numbers count from zero. The end column points to the last 
 * character, not the one following it.
 * 
 * @version 0.1
 * @author Reinhard Erstling (c) interactive instruments GmbH, Bonn, Germany
 */

public class SourceReference {
	
	short lineNo, colFrom, colTo;
	short tokFrom, tokTo;

	/**
	 * This constructs a SourceReference from a line number, two column
	 * numbers and a range of token serial numbers. The latter are used
	 * in constructing compound SourceReferences to determine whether
	 * Source References address adjacent tokens.
	 * @param lineNo Line number
	 * @param colFrom 1st column in line
	 * @param colTo Last column in line
	 * @param tokFrom 1st token number the reference stands for
	 * @param tokTo last token number of token range
	 */
	SourceReference( 
		short lineNo, short colFrom, short colTo, short tokFrom, short tokTo ) {
		this.lineNo = lineNo;
		this.colFrom = colFrom;
		this.colTo = colTo;
		this.tokFrom = tokFrom;
		this.tokTo = tokTo;
	}
	
	/**
	 * This constructs a new SourceReference as a copy of a given one.
	 * @param sourceref SourceReference to copy.
	 */
	SourceReference( SourceReference sourceref ) {
		lineNo = sourceref.lineNo;
		colFrom = sourceref.colFrom;
		colTo = sourceref.colTo;
		tokFrom = sourceref.tokFrom;
		tokTo = sourceref.tokTo;
	}
		
	/**
	 * This function is for inquiring the line number from a SourceReference.
	 * @return Line number counting from zero.
	 */
	public short getLineNumber() {
		return this.lineNo;
	}
	
	/**
	 * This function is for inquiring the start column from a Source Reference.
	 * @return First column in line counting from zero.
	 */
	public short getColumnFrom() {
		return this.colFrom;
	}
	
	/**
	 * This function is for inquiring the end column from a Source Reference.
	 * @return Last column in line counting from zero.
	 */
	public short getColumnTo() {
		return this.colTo;
	}
	
	/**
	 * This function is for inquiring the start token number from a Source 
	 * Reference.
	 * @return 1st token serial number in source reference
	 */
	public short getTokenFrom() {
		return this.tokFrom;
	}

	/**
	 * This function is for inquiring the end token number from a Source 
	 * Reference.
	 * @return Last token serial number in source reference
	 */
	public short getTokenTo() {
		return this.tokTo;
	}
	
	/**
	 * <p>This predicate function finds out, whether a given SourceReference is
	 * adjacent to this SourceReference or has an overlap with it. There are two 
	 * modes of adjacency/overlap testing, which can be selected by an 
	 * additional parameter flag, named <i>byToken</i>:</p>
	 * <ul>
	 * <li><i>false</i>: Direct adjacency/overlap by column range is tested.
	 * <li><i>true</i>: Adjacency/overlap by token serial number is tested.
	 * </ul>
	 * <p>Note that SourceReferences in different lines never can get 
	 * merged.</p> 
	 * 
	 * @param toBeMerged SourceReference to be tested.
	 * @param byToken Token adjacency/overlap flag
	 * @return <i>SourceReferences can be merged</i> flag
	 */
	boolean canBeMerged( SourceReference toBeMerged, boolean byToken ) {
		
		// Determine relative position ...
		short pos = this.relativePosition( toBeMerged, byToken );
		
		// We can merge if the absolute value of the outcome is 1 or 0 ...
		return Math.abs(pos) <= 1;
	}

	/**
	 * <p>This function determines the relative position of the SourceReference
	 * argument compared to this SourceReference object.</p>
	 * The following numerical return describes the relative position: 
	 * <ul>
	 * <li>-2: Argument is entirely preceding this object, no adjacency
	 * <li>-1: Argument is preceding this one, it is directly adjacent
	 * <li> 0: Argument and this object overlap
	 * <li>+1: Argument is succeeding this one, it is directly adjacent
	 * <li>+2: Argument is entirely succeeding this object, no adjacency
	 * </ul>
	 * <p>There are two modes of adjacency/overlap testing, which can be 
	 * selected by an additional parameter flag, named <i>byToken</i>:</p>
	 * <ul>
	 * <li><i>false</i>: Direct adjacency/overlap by column range
	 * <li><i>true</i>: Adjacency/overlap by token serial number
	 * </ul>
	 * <p>Note that SourceReferences in different lines are never adjacent.</p> 
	 * 
	 * @param toBeCompared Argument SourceReference object
	 * @param byToken Token serial number comparison flag
	 * @return
	 */
	short relativePosition( SourceReference toBeCompared, boolean byToken ) {
		
		// References on different line numbers always compare disjunct ...
		if( lineNo<toBeCompared.lineNo ) return 2;
		if( lineNo>toBeCompared.lineNo ) return -2;

		// Normalize setup ...
		short[] lb = new short[2]; short[] ub = new short[2];
		if( byToken ) {
			lb[0] = this.tokFrom; lb[1] = toBeCompared.tokFrom;
			ub[0] = this.tokTo;   ub[1] = toBeCompared.tokTo;
		} else {
			lb[0] = this.colFrom; lb[1] = toBeCompared.colFrom;
			ub[0] = this.colTo;   ub[1] = toBeCompared.colTo;			
		}
		
		// Determine the index (0|1) of the smaller lower bound ...
		int idx_small_lb = lb[0]<lb[1] ? 0 : 1;
		
		// The intervals are disjunct iff the upper bound on this (smaller
		// lower bound) index is smaller than the lower bound on the other
		// index. We determine the distance between these ...
		int dist = lb[1-idx_small_lb] - ub[idx_small_lb];
		
		// If the distance is greater 1, the intervals are entirely disjunct.
		if( dist>1 )
			return (short)(idx_small_lb==0 ? 2 : -2);

		// If the distance is 1, the intervals are directly adjacent.
		if( dist==1 )
			return (short)(idx_small_lb==0 ? 1 : -1 );

		// Otherwise we have some sort of overlap ...
		return 0;
	}
	
	/**
	 * This merges another SourceReference to this one. No checking whether 
	 * this leads to sensible results is done. Use canBeMerged() to check first.
	 * @param toBeMerged SourceReference to be merged. 
	 */
	void merge( SourceReference toBeMerged ) {
		
		if( toBeMerged.colFrom<colFrom ) colFrom = toBeMerged.colFrom;
		if( toBeMerged.colTo>colTo ) colTo = toBeMerged.colTo;
		if( toBeMerged.tokFrom<tokFrom ) tokFrom = toBeMerged.tokFrom;
		if( toBeMerged.tokTo>tokTo ) tokTo = toBeMerged.tokTo;
	}

	/**
	 * This function merges the object into a given array of SourceReferences.
	 * The array must comply to the invariant that its contained 
	 * SourceReferences are in ascending order and do not overlap or can be 
	 * merged. This is the case if the array is maintained by the function
	 * at hand.
	 * @param srl Array of SourceReferences the object is to be merged into
	 * @param byToken Token serial number comparison flag
	 * @return Either a new array, if the length had to be adjusted or the one
	 * passed in argument srl.
	 */
	SourceReference[] merge( SourceReference[] srl, boolean byToken ) {

		// Initialize indexes which determine where to insert or merge ...
		int insert_before = srl.length;
		int merge_lo = -1;
		int merge_hi = -1;
		
		// The result is primarily the input array ...
		SourceReference[] res = srl;
		
		// Now step through the array and find out where ...
		for( int i=0; i<srl.length; i++ ) {
			// Relative position of array element in respect to this
			short relpos = this.relativePosition( srl[i], byToken );
			// Not yet in place ...
			if( relpos<-1 ) continue;
			// Just beyond the place. Memorize for insertion ...
			if( relpos>1 ) {
				insert_before = i; break;
			}
			// We have some sort of adjacency or overlap, record the
			// interval ...
			if( merge_lo==-1 ) merge_lo = i;
			merge_hi = i;
		}
		
		// Merge or insert case ?
		if( merge_lo==-1 ) {
			// This is a pure insert.
			res = new SourceReference[srl.length+1];
			// Copy, leave a space ...
			for( int i=0, j=0; i<srl.length; i++ ) {
				if( i==insert_before ) j++;
				res[j++] = srl[i];
			}
			// Insert ...
			res[insert_before] = new SourceReference( this );
		} else if( merge_hi==merge_lo ) {
			// We are merging in-place ...
			res[merge_lo].merge( this );
		} else {		
			// We will have to collapse the interval into one ...
			int n = merge_hi - merge_lo + 1;
			res = new SourceReference[ srl.length - n + 1 ];
			// Copy or merge ...
			SourceReference tomerge = new SourceReference( this );
			for( int i=0, j=0; i<srl.length; i++ ) {
				if( merge_lo<=i && i<=merge_hi ) {
					// We are in the merge interval. Merge ...
					tomerge.merge( srl[i] );
					if( i==merge_hi ) res[j++] = tomerge;
				} else {
					// Outside merge interval, copy ...
					res[j++] = srl[i];
				}
			}
		}
		
		// Return whatever we have ...
		return res;	
	}

	/**
	 * <p>This static function merges the SourceReference array srl2 into the
	 * SourceReference array srl1. This is done by merging each element of
	 * srl1 individually into srl1.</p>
	 * <p>The constraints concerning ordering of SourceReference array as
	 * explained in method merge() above do also apply here. The function
	 * can be expected to alter and/or return srl1. The other array, srl2,
	 * will not be altered.</p>
	 * @param srl1 Array into which srl2 will be merged.
	 * @param srl2 The array to be merged into srl1
	 * @param byToken Token serial number comparison flag
	 * @return Either a new array, if the length had to be adjusted or the one
	 * passed in argument srl1.
	 */
	static SourceReference[] merge( 
		SourceReference[] srl1, SourceReference[] srl2, boolean byToken ) {
		for( int i=0; i<srl2.length; i++ ) 
			srl1 = srl2[i].merge( srl1, byToken );
		return srl1;
	}	
}