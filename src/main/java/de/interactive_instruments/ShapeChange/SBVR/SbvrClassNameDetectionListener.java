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

import de.interactive_instruments.antlr.sbvr.SBVRBaseListener;
import de.interactive_instruments.antlr.sbvr.SBVRParser.SentenceUsingObligationContext;
import de.interactive_instruments.antlr.sbvr.SBVRParser.SentenceUsingShallContext;

/**
 * Retrieves the main class name stated in an SBVR rule. The main class name is
 * the first part of the noun in the first quantification, minus a potentially
 * existing "TimeSlice" suffix (to take AIXM SBVR rules into account).
 * 
 * @author Johannes Echterhoff
 *
 */
public class SbvrClassNameDetectionListener extends SBVRBaseListener {

	private String mainClassName;

	@Override
	public void enterSentenceUsingObligation(SentenceUsingObligationContext ctx) {
		findMainClassName(ctx.quantification().noun.getText());
	}

	private void findMainClassName(String noun) {
		String concept_ = noun.trim();
		String[] parts = concept_.split("\\.");
		mainClassName = parts[0];

		if (mainClassName.indexOf("TimeSlice") > -1) {
			mainClassName = mainClassName.substring(0,
					mainClassName.indexOf("TimeSlice"));
		}
	}

	@Override
	public void enterSentenceUsingShall(SentenceUsingShallContext ctx) {
		findMainClassName(ctx.quantification().noun.getText());
	}

	/**
	 * Retrieves the main class name stated in an SBVR rule. The main class name
	 * is the first part of the noun in the first quantification, minus a
	 * potentially existing "TimeSlice" suffix (to take AIXM SBVR rules into
	 * account).
	 * 
	 * @return the mainClassName
	 */
	public String getMainClassName() {
		return mainClassName;
	}
}
