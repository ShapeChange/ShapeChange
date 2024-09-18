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
package de.interactive_instruments.shapechange.core;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.interactive_instruments.shapechange.core.model.ClassInfo;
import de.interactive_instruments.shapechange.core.model.Model;
import de.interactive_instruments.shapechange.core.model.PackageInfo;
import de.interactive_instruments.shapechange.core.model.Stereotypes;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
public class ClassSelector {

	private Pattern namePattern;
	private Pattern stereotypePattern;

	public ClassSelector(Pattern namePattern, Pattern stereotypePattern) {

		this.namePattern = namePattern;
		this.stereotypePattern = stereotypePattern;
	}
	
	public ClassSelector() {
	}

	/**
	 * @param m tbd
	 * @param owners
	 *            can be <code>null</code> in case that being owned by specific
	 *            packages (from the set of all selected schemas with their
	 *            child packages (in same target namespace)) is irrelevant
	 * @return A set containing all classes that are 1) contained in selected
	 *         schemas, 2) owned by one of the given packages (if provided), and
	 *         that 3) satisfy the name as well as stereotype selection criteria
	 *         (if provided). The resulting set may be empty but not
	 *         <code>null</code>.
	 */
	public SortedSet<ClassInfo> selectClasses(Model m, Set<PackageInfo> owners) {

		SortedSet<ClassInfo> result = new TreeSet<ClassInfo>();

		SortedSet<? extends PackageInfo> selectedSchemas = m.selectedSchemas();

		if (selectedSchemas != null && !selectedSchemas.isEmpty()) {

			SortedSet<ClassInfo> selSchemaClasses = new TreeSet<ClassInfo>();

			/*
			 * Gather all classes contained in selected schemas.
			 */
			for (PackageInfo selSchema : selectedSchemas) {
				selSchemaClasses.addAll(m.classes(selSchema));
			}

			/*
			 * Apply filtering
			 */
			for (ClassInfo ci : selSchemaClasses) {

				/*
				 * Add a class to the result set if it: a) is owned by one of
				 * the given packages, b) matches the name pattern (if
				 * provided), c) matches the stereotype pattern (if provided)
				 */
				if ((owners == null || owners.contains(ci.pkg()))
						&& nameMatches(namePattern, ci)
						&& oneStereotypeMatches(stereotypePattern, ci)) {
					result.add(ci);
				}
			}
		}

		return result;
	}

	/**
	 * 
	 * @param p
	 * @param ci
	 * @return a new set with all PackageInfos that have at least one stereotype
	 *         matching the given pattern - or with all PackageInfos in case
	 *         that the pattern is <code>null</code>
	 */
	private boolean oneStereotypeMatches(Pattern p, ClassInfo ci) {

		if (p != null) {

			Stereotypes sts = ci.stereotypes();

			String[] stsArr = sts.asArray();

			for (String st : stsArr) {
				if (matchesRegex(st, p)) {
					return true;
				}
			}

			return false;

		} else {

			return true;
		}
	}

	private boolean nameMatches(Pattern p, ClassInfo ci) {

		if (p != null) {

			if (matchesRegex(ci.name(), p)) {
				return true;
			} else {
				return false;
			}

		} else {

			return true;
		}
	}

	private boolean matchesRegex(String s, Pattern p) {

		Matcher matcher = p.matcher(s);

		return matcher.matches();
	}

	/**
	 * @param namePattern the namePattern to set
	 */
	public void setNamePattern(Pattern namePattern) {
		this.namePattern = namePattern;
	}

	/**
	 * @param stereotypePattern the stereotypePattern to set
	 */
	public void setStereotypePattern(Pattern stereotypePattern) {
		this.stereotypePattern = stereotypePattern;
	}

}
