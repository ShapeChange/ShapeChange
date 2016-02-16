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
package de.interactive_instruments.ShapeChange;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.interactive_instruments.ShapeChange.Model.Model;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.Stereotypes;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class PackageSelector {

	private Pattern schemaNamePattern;
	private Pattern namePattern;
	private Pattern stereotypePattern;

	public PackageSelector(Pattern schemaNamePattern,
			Pattern namePatternPattern, Pattern stereotypePattern) {

		this.schemaNamePattern = schemaNamePattern;
		this.namePattern = namePatternPattern;
		this.stereotypePattern = stereotypePattern;
	}

	public PackageSelector() {
	}

	/**
	 * @param m
	 * @return A set containing all selected schema packages that satisfy the
	 *         schemaName selection criterion (if provided), as well as all
	 *         child packages of these schemas that themselves satisfy the name
	 *         and stereotype selection criteria. The resulting set may be empty
	 *         but not <code>null</code>.
	 */
	public Set<PackageInfo> selectPackages(Model m) {

		Set<PackageInfo> result = new HashSet<PackageInfo>();

		SortedSet<? extends PackageInfo> selectedSchemas = m.selectedSchemas();
		Set<PackageInfo> selSchemas = new HashSet<PackageInfo>(selectedSchemas);

		if (selectedSchemas == null || selectedSchemas.isEmpty()) {

			return result;

		} else {

			/*
			 * filter selected schemas
			 */

			// filter by schemaName
			Set<PackageInfo> matchingSelectedSchemas = filterByName(
					schemaNamePattern, selSchemas);

			result.addAll(matchingSelectedSchemas);

			/*
			 * now get all packages belonging to the matching selected schemas
			 * that are in their target namespace
			 */

			Set<PackageInfo> pisInTnsOfSelSchemas = new HashSet<PackageInfo>();

			for (PackageInfo mss : matchingSelectedSchemas) {
				pisInTnsOfSelSchemas.addAll(
						identifyChildPackagesInSameTargetNamespace(mss));
			}

			// filter by name
			Set<PackageInfo> matchingPisInTnsOfSelSchemas = filterByName(
					namePattern, pisInTnsOfSelSchemas);

			// filter by stereotype
			matchingPisInTnsOfSelSchemas = filterByStereotype(stereotypePattern,
					matchingPisInTnsOfSelSchemas);

			result.addAll(matchingPisInTnsOfSelSchemas);
		}

		return result;
	}

	private Set<PackageInfo> identifyChildPackagesInSameTargetNamespace(
			PackageInfo pi) {

		Set<PackageInfo> result = new HashSet<PackageInfo>();

		if (pi != null && pi.containedPackages() != null) {

			for (PackageInfo childPi : pi.containedPackages()) {

				if (childPi.targetNamespace().equals(pi.targetNamespace())) {
					result.add(childPi);
					result.addAll(identifyChildPackagesInSameTargetNamespace(
							childPi));
				}
			}
		}

		return result;
	}

	/**
	 * 
	 * @param p
	 * @param pis
	 * @return a new set with all PackageInfos that have at least one stereotype
	 *         matching the given pattern - or with all PackageInfos in case
	 *         that the pattern is <code>null</code>
	 */
	private Set<PackageInfo> filterByStereotype(Pattern p,
			Set<PackageInfo> pis) {

		Set<PackageInfo> matches = new HashSet<PackageInfo>();

		if (p != null) {

			for (PackageInfo pi : pis) {

				Stereotypes sts = pi.stereotypes();

				String[] stsArr = sts.asArray();

				for (String st : stsArr) {
					if (matchesRegex(st, p)) {
						matches.add(pi);
						break;
					}
				}
			}

		} else {

			matches.addAll(pis);
		}

		return matches;
	}

	private Set<PackageInfo> filterByName(Pattern p, Set<PackageInfo> pis) {

		Set<PackageInfo> matches = new HashSet<PackageInfo>();

		if (p != null) {

			for (PackageInfo pi : pis) {

				if (matchesRegex(pi.name(), p)) {
					matches.add(pi);
				}
			}

		} else {

			matches.addAll(pis);
		}

		return matches;
	}

	private boolean matchesRegex(String s, Pattern p) {

		Matcher matcher = p.matcher(s);

		return matcher.matches();
	}

	/**
	 * @param schemaNamePattern
	 *            the schemaNamePattern to set
	 */
	public void setSchemaNamePattern(Pattern schemaNamePattern) {
		this.schemaNamePattern = schemaNamePattern;
	}

	/**
	 * @param namePattern
	 *            the namePattern to set
	 */
	public void setNamePattern(Pattern namePattern) {
		this.namePattern = namePattern;
	}

	/**
	 * @param stereotypePattern
	 *            the stereotypePattern to set
	 */
	public void setStereotypePattern(Pattern stereotypePattern) {
		this.stereotypePattern = stereotypePattern;
	}

}
