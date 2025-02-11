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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.shapechange.core.model;

import de.interactive_instruments.shapechange.core.fol.FolExpression;

/**
 * A First Order Logic constraint.
 * 
 * @author Johannes Echterhoff
 *
 */
public interface FolConstraint extends TextConstraint {

	public static final String TYPE = "FOL";

	/**
	 * Get comments regarding the logic expression
	 * @return  tbd
	 * */
	public String[] comments();
	
	public boolean hasComments();
	
	public void setComments(String[] comments);
	
	public void mergeComments(String[] additionalComments);
	
	/**
	 * @return the first order logic expression represented by this constraint;
	 *         can be <code>null</code> if parsing the constraint text did not
	 *         succeed
	 */
	public FolExpression folExpression();

	/**
	 * @return the type of the source from which the constraint was created (e.g. 'SBVR')
	 */
	public String sourceType();

	/**
	 * Sets the constraint text, replacing all occurrences of one or more
	 * consecutive whitespace characters with a single space character (thus
	 * removing tabs and line breaks to construct a single line with text).
	 * 
	 * @param text tbd
	 */
	public void setText(String text);

	public void setFolExpression(FolExpression folExpr);

	public void setSourceType(String sourceType);

	public void setName(String name);

	public void setStatus(String status);

	public void setContextModelElmt(Info newContextModelElmt);

	public void setContextModelElmtType(
			ModelElmtContextType newContextModelElmtType);
}
