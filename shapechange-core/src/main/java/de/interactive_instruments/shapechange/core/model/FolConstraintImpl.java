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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import de.interactive_instruments.shapechange.core.fol.FolExpression;

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

	@Override
	public String name() {
		return constraintName.trim();
	}

	@Override
	public String status() {
		return constraintStatus;
	}

	@Override
	public String text() {
		return constraintText;
	}

	@Override
	public void setText(String text) {
		if (text == null) {
			this.constraintText = text;
		} else {
			this.constraintText = text.replaceAll("\\s+", " ");
		}
	}

	@Override
	public Info contextModelElmt() {
		return contextModelElmt;
	}
	
	@Override
	public ModelElmtContextType contextModelElmtType() {
		return contextModelElmtType;
	}

	@Override
	public void setFolExpression(FolExpression folExpr) {
		this.folExpr = folExpr;
	}

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

	@Override
	public void mergeComments(String[] additionalComments) {

		if (this.comments == null || this.comments.length == 0) {
			
			this.comments = additionalComments;
			
		} else if (additionalComments == null || additionalComments.length == 0) {
			/*
			 * Nothing to do. Just keep comments as set.
			 */
		} else {
			/*
			 * Merge existing comments and additional comments. Each additional comment
			 * that is not contained in the existing comments shall be added to
			 * the existing comments.
			 */
			List<String> additionalCommentsList = new ArrayList<>();
			for (String c : additionalComments) {
				if (!Arrays.stream(this.comments).anyMatch(c::equals)) {
					additionalCommentsList.add(c);
				}
			}

			this.comments = ArrayUtils.addAll(this.comments,
					additionalCommentsList.stream().toArray(String[]::new));
		}
	}

}
