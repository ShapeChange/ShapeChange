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
package de.interactive_instruments.ShapeChange.Model;

import de.interactive_instruments.ShapeChange.FOL.FolExpression;

/**
 * Common root for all First Order Logic constraint implementations
 *
 * @author Johannes Echterhoff
 *
 */
public class FolConstraintImpl implements FolConstraint {
	
	protected String sourceType;
	protected FolExpression folExpr;
	
	/**
	 * Model Element Context - class, attribute, operation, etc. This has to be
	 * downcast to the proper xxxInfo as specified by contextModelElmtType.
	 */
	protected Info contextModelElmt = null;

	/**
	 * Model Element Context Type - the nature of the model context the
	 * constraint expression is specified in.
	 */
	protected ModelElmtContextType contextModelElmtType = null;

	/** Name of the constraint */
	protected String constraintName = null;

	/** The textual representation of the constraint */
	protected String constraintText = null;

	/**
	 * Constraint status. A string reflecting the status of the constraint in
	 * conspiracy between the model source and the code generator.
	 */
	protected String constraintStatus = null;
	
	protected String[] comments = null;

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.Constraint#name()
	 */
	@Override
	public String name() {
		return constraintName.trim();
	} // name()

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.Constraint#status()
	 */
	@Override
	public String status() {
		return constraintStatus;
	} // status()

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.Constraint#text()
	 */
	@Override
	public String text() {
		return constraintText;
	} // text()

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.FolConstraint#setText(java.lang.String)
	 */
	@Override
	public void setText(String text) {
		if (text == null) {
			this.constraintText = text;
		} else {
			this.constraintText = text.replaceAll("\\s+", " ");
		}
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.Constraint#contextModelElmt()
	 */
	@Override
	public Info contextModelElmt() {
		return contextModelElmt;
	} // contextModelElmt()

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.Constraint#contextModelElmtType()
	 */
	@Override
	public ModelElmtContextType contextModelElmtType() {
		return contextModelElmtType;
	} // contextModelElmtType()

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.FolConstraint#setFolExpression(de.interactive_instruments.ShapeChange.FOL.FolExpression)
	 */
	@Override
	public void setFolExpression(FolExpression folExpr) {
		this.folExpr = folExpr;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.FolConstraint#folExpression()
	 */
	@Override
	public FolExpression folExpression() {
		return folExpr;
	}

	/**
	 * Inquire the type of the source from which this constraint was generated
	 * (e.g. 'SBVR').
	 * 
	 */
	@Override
	public String sourceType() {
		return sourceType;
	}
	
	/**
	 * @see de.interactive_instruments.ShapeChange.Model.FolConstraint#setSourceType(java.lang.String)
	 */
	@Override
	public void setSourceType(String sourceType) {
		this.sourceType = sourceType;
	}

	@Override
	public String type() {		
		return FolConstraint.TYPE;
	}

	@Override
	public void setName(String name) {
		this.constraintName = name;
	}

	@Override
	public void setStatus(String status) {
		this.constraintStatus = status;
	}

	@Override
	public void setContextModelElmt(Info newContextModelElmt) {
		this.contextModelElmt = newContextModelElmt;
	}

	@Override
	public void setContextModelElmtType(
			ModelElmtContextType newContextModelElmtType) {
		this.contextModelElmtType = newContextModelElmtType;
	}

	@Override
	public String[] comments() {
		return comments;
	}
	
	@Override
	public boolean hasComments() {
		return this.comments != null && this.comments.length > 0;
	}

	@Override
	public void setComments(String[] comments) {
		this.comments = comments;
	}

}
