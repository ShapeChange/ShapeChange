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
package de.interactive_instruments.shapechange.core.transformation.modelcleaner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.shapechange.core.MessageSource;
import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ProcessRuleSet;
import de.interactive_instruments.shapechange.core.ShapeChangeAbortException;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.StructuredNumber;
import de.interactive_instruments.shapechange.core.TransformerConfiguration;
import de.interactive_instruments.shapechange.core.ShapeChangeResult.MessageContext;
import de.interactive_instruments.shapechange.core.model.PropertyInfo;
import de.interactive_instruments.shapechange.core.model.generic.GenericClassInfo;
import de.interactive_instruments.shapechange.core.model.generic.GenericModel;
import de.interactive_instruments.shapechange.core.model.generic.GenericPropertyInfo;
import de.interactive_instruments.shapechange.core.transformation.Transformer;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
public class ModelCleaner implements Transformer, MessageSource {

	/* ------------------------------------------- */
	/* --- configuration parameter identifiers --- */
	/* ------------------------------------------- */

	/**
	 * Identifier of the parameter that defines the regular expression for
	 * matching the name of the property that should be implemented by nilreason
	 * in a union with two properties. Example: 'reason'.
	 * 
	 * Applies to rule {@value #RULE_TRF_CLEANER_FIX_UNION_DIRECT}.
	 * 
	 * The parameter is required.
	 */
	public static final String PARAM_REASON_PROPERTY_REGEX = "unionDirectReasonPropertyNameRegex";

	/* ------------------------ */
	/* --- rule identifiers --- */
	/* ------------------------ */

	/**
	 * If a union has exactly two properties then this rule sets the tagged
	 * value {@value #TV_IMPLEMENTED_BY_NILREASON} to <code>true</code> for each
	 * of the two properties whose name matches the regular expression provided
	 * via the required parameter {@value #PARAM_REASON_PROPERTY_REGEX} (with
	 * default value {@value #DEFAULT_REASON_PROP_NAME_REGEX}). The internal
	 * fields are updated based upon the updated set of tagged values (the
	 * internal field 'implementedByNilReason' is therefore also updated to
	 * match the tagged value).
	 */
	public static final String RULE_TRF_CLEANER_FIX_UNION_DIRECT = "rule-trf-cleaner-fix-union-direct";

	/* -------------------- */
	/* --- other fields --- */
	/* -------------------- */

	public static final String TV_IMPLEMENTED_BY_NILREASON = "gmlImplementedByNilReason";

	public static final String DEFAULT_REASON_PROP_NAME_REGEX = "reason";

	// private Options options = null;
	private ShapeChangeResult result = null;

	@Override
	public void process(GenericModel m, Options o,
			TransformerConfiguration trfConfig, ShapeChangeResult r)
					throws ShapeChangeAbortException {

		// this.options = o;
		this.result = r;

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

		if (rules.contains(RULE_TRF_CLEANER_FIX_UNION_DIRECT)) {

			result.addProcessFlowInfo(null, 20103, RULE_TRF_CLEANER_FIX_UNION_DIRECT);
			applyRuleFixUnionDirect(m, trfConfig);
		}

		// apply post-processing (nothing to do right now)
	}

	/**
	 * 
	 * @see #RULE_TRF_CLEANER_FIX_UNION_DIRECT
	 * @param m
	 * @param trfConfig
	 */
	private void applyRuleFixUnionDirect(GenericModel m,
			TransformerConfiguration trfConfig) {

		String reasonPropNameRegex = trfConfig
				.getParameterValue(PARAM_REASON_PROPERTY_REGEX);

		if (reasonPropNameRegex == null) {

			// use default
			reasonPropNameRegex = DEFAULT_REASON_PROP_NAME_REGEX;
		}

		reasonPropNameRegex = reasonPropNameRegex.trim();

		if (reasonPropNameRegex.length() == 0) {
			/*
			 * a regular expression is required but was not provided
			 */
			MessageContext mc = result.addError(this, 1,
					PARAM_REASON_PROPERTY_REGEX,
					RULE_TRF_CLEANER_FIX_UNION_DIRECT);
			mc.addDetail(this, 0);
			return;
		}

		Pattern reasonPropNamePattern = null;

		try {

			reasonPropNamePattern = Pattern.compile(reasonPropNameRegex);

		} catch (PatternSyntaxException e) {

			MessageContext mc = result.addError(this, 3,
					PARAM_REASON_PROPERTY_REGEX,
					RULE_TRF_CLEANER_FIX_UNION_DIRECT, reasonPropNameRegex,
					e.getMessage());
			mc.addDetail(this, 0);
			return;
		}

		for (GenericClassInfo genCi : m.selectedSchemaClasses()) {

			if (genCi.category() == Options.UNION
					&& genCi.properties().size() == 2) {

				SortedMap<StructuredNumber, PropertyInfo> genPis = genCi
						.properties();

				List<PropertyInfo> props = new ArrayList<PropertyInfo>(
						genPis.values());

				GenericPropertyInfo p1 = (GenericPropertyInfo) props.getFirst();
				GenericPropertyInfo p2 = (GenericPropertyInfo) props.get(1);

				/*
				 * WARNING: do inspect the gmlImplementedByNilReason tagged
				 * value, NOT just the implementedByNilReason field, because in
				 * case of a union-direct the latter may have been set to true
				 * even if the tagged value had been - and remained to be -
				 * false.
				 */
				
				// check the first property
				if (matchesRegex(p1.name(), reasonPropNamePattern)) {

					// check that tagged value and field for
					// implementedByNilReason are set correctly
					if (StringUtils.isNotBlank(p1.taggedValue(TV_IMPLEMENTED_BY_NILREASON))
							&& Boolean.parseBoolean(
									p1.taggedValue(TV_IMPLEMENTED_BY_NILREASON)
											.trim())) {
						
						// tagged value is set correctly
						
					} else {
						
						// set tagged value
						p1.setTaggedValue(TV_IMPLEMENTED_BY_NILREASON, "true",
								true);
						result.addDebug(this, 4, genCi.name(), p1.name());
					}					
				}
				
				// now also check the second property
				if (matchesRegex(p2.name(), reasonPropNamePattern)) {

					// check that tagged value and field for
					// implementedByNilReason are set correctly
					if (StringUtils.isNotBlank(p2.taggedValue(TV_IMPLEMENTED_BY_NILREASON))
							&& Boolean.parseBoolean(
									p2.taggedValue(TV_IMPLEMENTED_BY_NILREASON)
											.trim())) {

						// tagged value is set correctly

					} else {

						// set tagged value
						p2.setTaggedValue(TV_IMPLEMENTED_BY_NILREASON, "true",
								true);

						result.addDebug(this, 4, genCi.name(), p2.name());
					}
				}
			}
		}
	}

	private boolean matchesRegex(String s, Pattern p) {

		Matcher matcher = p.matcher(s);

		return matcher.matches();
	}

	public String message(int mnr) {

		switch (mnr) {
		case 0:
			return "Context: class ModelCleaner";
		case 1:
			return "No non-empty string value provided for configuration parameter '$1$'. Execution of rule '$2$' aborted.";
		case 2:
			return "Configuration parameter '$1$' required for execution of rule '$2$' was not provided. Execution of rule '$2$' aborted.";
		case 3:
			return "Syntax exception for regular expression value of configuration parameter '$1$' (required for execution of rule '$2$'). Regular expression value was: $3$. Exception message: $4$. Execution of rule '$2$' aborted.";
		case 4:
			return "$1$::$2$: set '" + TV_IMPLEMENTED_BY_NILREASON
					+ "' tagged value to true.";
		case 5:
			return "Union $1$: set 'hasNilReason' field to true.";
		case 6:
			return "$1$::$2$: set 'implementedByNilReason' field to true.";
		default:
			return "(" + ModelCleaner.class.getName()
					+ ") Unknown message with number: " + mnr;
		}
	}

}
