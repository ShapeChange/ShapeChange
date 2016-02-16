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
package de.interactive_instruments.ShapeChange.Target.Statistics;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 */
public class ClassStatistic {

	private ClassInfo ci;

	private int numProps;
	private int numFeatureRels;
	private double percentFeatureRelationships;

	public ClassStatistic(ClassInfo ci) {
		this.ci = ci;
		computeStatistics();
	}

	private void computeStatistics() {

		// number of all (non-inherited) properties
		numProps = ci.properties().isEmpty() ? 0 : ci.properties().size();

		/*
		 * compute percentage of feature relationships
		 */
		if(numProps > 0) {
			numFeatureRels = 0;
			for(PropertyInfo pi : ci.properties().values()) {
				if(pi.categoryOfValue() == Options.FEATURE) {
					numFeatureRels++;
				}
			}
			percentFeatureRelationships = (double)numFeatureRels/numProps*100;
		}
		
	}
	
	public ClassInfo getClassInfo() {
		return ci;
	}

	public double percentOfFeatureRelationships() {
		return percentFeatureRelationships;
	}

	public int numberOfProperties() {
		return numProps;
	}
	
	public int numberOfFeatureRelationships() {
		return numFeatureRels;
	}
}
