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
 * (c) 2002-2017 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Transformation.Constraints;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.google.common.base.Joiner;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessRuleSet;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.TransformerConfiguration;
import de.interactive_instruments.ShapeChange.Model.Constraint;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericClassInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel;
import de.interactive_instruments.ShapeChange.Transformation.Transformer;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class ConstraintConverter implements Transformer, MessageSource {

	private static final Joiner commaJoiner = Joiner.on(",").skipNulls();

	/* ------------------------------------------- */
	/* --- configuration parameter identifiers --- */
	/* ------------------------------------------- */

	/**
	 *
	 */
	public static final String PARAM_GEOM_REP_CONSTRAINT_REGEX = "geometryRepresentationConstraintRegex";

	/**
	 * 
	 */
	public static final String PARAM_GEOM_REP_TYPES = "geometryRepresentationTypes";

	/**
	 *
	 */
	public static final String PARAM_GEOM_REP_VALUE_TYPE_REGEX = "geometryRepresentationValueTypeRegex";

	/* ------------------------ */
	/* --- rule identifiers --- */
	/* ------------------------ */

	/**
	 * 
	 */
	public static final String RULE_TRF_CLS_CONSTRAINTS_GEOMRESTRICTIONTOGEOMTV_INCL = "rule-trf-cls-constraints-geometryRestrictionToGeometryTV-inclusion";

	/**
	 * 
	 */
	public static final String RULE_TRF_CLS_CONSTRAINTS_GEOMRESTRICTIONTOGEOMTV_EXCL = "rule-trf-cls-constraints-geometryRestrictionToGeometryTV-exclusion";

	public static final String RULE_TRF_CLS_CONSTRAINTS_GEOMRESTRICTIONTOGEOMTV_NORESTRICTION_BYVALUETYPE = "rule-trf-cls-constraints-geometryRestrictionToGeometryTV-typesWithoutRestriction-byValueTypeMatch";

	private Options options = null;
	private ShapeChangeResult result = null;

	@Override
	public void process(GenericModel genModel, Options options,
			TransformerConfiguration trfConfig, ShapeChangeResult result)
			throws ShapeChangeAbortException {

		this.options = options;
		this.result = result;

		Map<String, ProcessRuleSet> ruleSets = trfConfig.getRuleSets();

		// get the set of all rules defined for the transformation
		Set<String> rules = new HashSet<String>();
		if (!ruleSets.isEmpty()) {
			for (ProcessRuleSet ruleSet : ruleSets.values()) {
				if (ruleSet.getAdditionalRules() != null) {
					rules.addAll(ruleSet.getAdditionalRules());
				}
			}
		}

		/*
		 * because there are no mandatory - in other words default - rules for
		 * this transformer simply return the model if no rules are defined in
		 * the rule sets (which the schema allows)
		 */
		if (rules.isEmpty())
			return;

		// apply pre-processing (nothing to do right now)

		// execute rules

		if (rules.contains(
				RULE_TRF_CLS_CONSTRAINTS_GEOMRESTRICTIONTOGEOMTV_INCL)) {

			result.addInfo(null, 20103,
					RULE_TRF_CLS_CONSTRAINTS_GEOMRESTRICTIONTOGEOMTV_INCL);
			applyRuleGeometryRestrictionToGeometryTaggedValue(genModel,
					trfConfig, rules, true);

		} else if (rules.contains(
				RULE_TRF_CLS_CONSTRAINTS_GEOMRESTRICTIONTOGEOMTV_EXCL)) {

			result.addInfo(null, 20103,
					RULE_TRF_CLS_CONSTRAINTS_GEOMRESTRICTIONTOGEOMTV_EXCL);
			applyRuleGeometryRestrictionToGeometryTaggedValue(genModel,
					trfConfig, rules, false);
		}

		// apply post-processing (nothing to do right now)
	}

	private void applyRuleGeometryRestrictionToGeometryTaggedValue(
			GenericModel genModel, TransformerConfiguration config,
			Set<String> rules, boolean isInclusion) {

		String geomRepConstrRegex = config.parameterAsString(
				PARAM_GEOM_REP_CONSTRAINT_REGEX, null, false, false);
		Pattern geomRepConstrPattern = null;

		if (geomRepConstrRegex != null) {
			// parse regex
			try {
				geomRepConstrPattern = Pattern.compile(geomRepConstrRegex);
			} catch (PatternSyntaxException e) {
				// reported via configuration validator
			}
		}

		String geomRepValueTypeRegex = config.parameterAsString(
				PARAM_GEOM_REP_VALUE_TYPE_REGEX, null, false, false);
		Pattern geomRepValueTypePattern = null;

		if (geomRepValueTypeRegex != null && rules.contains(
				RULE_TRF_CLS_CONSTRAINTS_GEOMRESTRICTIONTOGEOMTV_NORESTRICTION_BYVALUETYPE)) {
			// parse regex
			try {
				geomRepValueTypePattern = Pattern
						.compile(geomRepValueTypeRegex);
			} catch (PatternSyntaxException e) {
				// reported via configuration validator
			}
		}

		List<String> geomRepTypesIn = config.parameterAsStringList(
				ConstraintConverter.PARAM_GEOM_REP_TYPES, null, true, true,
				";");

		Map<String, String> geomRepTypes = new HashMap<String, String>();
		boolean foundInvalidGeomRepTypeValue = false;

		if (geomRepTypesIn != null && !geomRepTypesIn.isEmpty()) {

			for (String s : geomRepTypesIn) {

				String[] map = s.split("=");

				if (map.length != 2 || map[0].trim().isEmpty()
						|| map[1].trim().isEmpty()) {

					foundInvalidGeomRepTypeValue = true;
					break;
					// details are reported via configuration validator

				} else {

					geomRepTypes.put(map[0].trim(), map[1].trim());
				}
			}
		}

		if (foundInvalidGeomRepTypeValue || geomRepConstrPattern == null
				|| geomRepTypes.size() == 0) {

			result.addError(this, 100);

		} else {

			for (GenericClassInfo genCi : genModel.selectedSchemaClasses()) {
				
				if(genCi.category() == Options.ENUMERATION || genCi.category() == Options.CODELIST) {
					continue;
				}

				SortedSet<String> geometryTVValues = new TreeSet<String>();

				int countGeomRepConstrFound = 0;

				for (Constraint con : genCi.constraints()) {

					Matcher matcherOnConstraintName = geomRepConstrPattern
							.matcher(con.name());

					if (matcherOnConstraintName.matches()) {

						countGeomRepConstrFound++;

						if (countGeomRepConstrFound == 1) {

							for (Entry<String, String> geomRepType : geomRepTypes
									.entrySet()) {

								String geomTypeName = geomRepType.getKey();
								String geomTypeAbbrev = geomRepType.getValue();

								if ((isInclusion
										&& con.text().contains(geomTypeName))
										|| (!isInclusion && !con.text()
												.contains(geomTypeName))) {

									geometryTVValues.add(geomTypeAbbrev);
								}
							}
						}
					}
				}

				if (countGeomRepConstrFound > 1) {

					MessageContext mc = result.addError(this, 101,
							"" + countGeomRepConstrFound, genCi.name());
					if (mc != null) {
						mc.addDetail(this, 1, genCi.fullName());
					}

				} else if (countGeomRepConstrFound == 0
						&& geomRepValueTypePattern != null) {

					for (PropertyInfo pi : genCi.propertiesAll()) {

						Matcher matcherOnPiTypeName = geomRepValueTypePattern
								.matcher(pi.typeInfo().name);

						if (matcherOnPiTypeName.matches()) {

							for (String geomTypeAbbrev : geomRepTypes
									.values()) {

								geometryTVValues.add(geomTypeAbbrev);
							}

							break;
						}
					}
				}

				if (geometryTVValues.size() > 0) {

					String join = commaJoiner.join(geometryTVValues);

					String geometryTV = genCi.taggedValue("geometry");
					if (geometryTV != null) {
						MessageContext mc = result.addWarning(this, 102, genCi.name(), geometryTV, join);
						if(mc != null) {
							mc.addDetail(this, 1, genCi.fullName());
						}
					}

					genCi.setTaggedValue("geometry", join, false);
				}
			}
		}

	}

	@Override
	public String message(int mnr) {

		switch (mnr) {
		case 0:
			return "Context: property '$1$'.";
		case 1:
			return "Context: class '$1$'.";
		case 2:
			return "Context: association class '$1$'.";
		case 3:
			return "Context: association between class '$1$' (with property '$2$') and class '$3$' (with property '$4$')";

		// Messages for RULE_
		case 100:
			return "Invalid value(s) for configuration parameters '"
					+ PARAM_GEOM_REP_CONSTRAINT_REGEX + "' and/or '"
					+ PARAM_GEOM_REP_TYPES
					+ "'. For further details, check the configuration validator log messages.";
		case 101:
			return "Found multiple ($1$) constraints to restrict geometry representation on type '$2$'. An arbitrary constraint is chosen.";
		case 102:
			return "Overwriting tagged value 'geometry' in type '$1$'. Old value: '$2$'. New value: '$3$'.";
			
		default:
			return "(" + this.getClass().getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
