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
package de.interactive_instruments.ShapeChange.Transformation.TaggedValues;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
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
import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.TaggedValues;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericClassInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericPropertyInfo;
import de.interactive_instruments.ShapeChange.Transformation.Transformer;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class TaggedValueTransformer implements Transformer, MessageSource {

	/**
	 * Comma-separated list of names of tagged values for which
	 * {@value #RULE_TV_INHERITANCE} shall be applied. This parameter is
	 * required.
	 */
	public static final String PARAM_TV_INHERITANCE_GENERAL_LIST = "taggedValueInheritanceGeneralList";
	/**
	 * Comma-separated list of names of tagged values. If a subtype already has
	 * a tagged value that would be copied from a supertype under
	 * {@value #RULE_TV_INHERITANCE}, and that tagged value is contained in the
	 * list, then the tagged value shall be overwritten in the subtype, rather
	 * than being retained.
	 * <p>
	 * NOTE: Overwriting a tagged value has higher priority than appending (see
	 * {@value #PARAM_TV_INHERITANCE_APPEND_LIST}). If a tagged value is listed
	 * for both parameters {@value #PARAM_TV_INHERITANCE_OVERWRITE_LIST} and
	 * {@value #PARAM_TV_INHERITANCE_APPEND_LIST} then it will be ignored in the
	 * latter.
	 */
	public static final String PARAM_TV_INHERITANCE_OVERWRITE_LIST = "taggedValueInheritanceOverwriteList";
	/**
	 * Comma-separated list of names of tagged values. If a subtype already has
	 * a tagged value that would be copied from a supertype under
	 * {@value #RULE_TV_INHERITANCE}, and that tagged value is contained in the
	 * list, then the value from the tagged value of the supertype shall be
	 * appended to the value of the tagged value from the subtype, using the
	 * separator defined by configuration parameter
	 * {@value #PARAM_TV_INHERITANCE_APPEND_SEPARATOR}.
	 * <p>
	 * NOTE: Appending a tagged value has lower priority than overwriting (see
	 * {@value #PARAM_TV_INHERITANCE_OVERWRITE_LIST}). If a tagged value is
	 * listed for both parameters {@value #PARAM_TV_INHERITANCE_OVERWRITE_LIST}
	 * and {@value #PARAM_TV_INHERITANCE_APPEND_LIST} then it will be ignored in
	 * the latter.
	 */
	public static final String PARAM_TV_INHERITANCE_APPEND_LIST = "taggedValueInheritanceAppendList";
	/**
	 * Define the separator to use when a tagged value inherited from a
	 * supertype under {@value #RULE_TV_INHERITANCE} shall be appended to the
	 * tagged value of the subtype. Default value is
	 * {@value #DEFAULT_TV_INHERITANCE_APPEND_SEPARATOR}.
	 */
	public static final String PARAM_TV_INHERITANCE_APPEND_SEPARATOR = "taggedValueInheritanceAppendSeparator";
	public static final String DEFAULT_TV_INHERITANCE_APPEND_SEPARATOR = ", ";

	/**
	 * Comma-separated list of names of tagged values to copy in
	 * {@value #RULE_TV_COPY_FROM_VALUE_TYPE}. Default value is the empty
	 * string.
	 */
	public static final String PARAM_TV_COPYFROMVALUETYPE_TVSTOCOPY = "taggedValuesToCopy";

	/**
	 * Regular expression to match the name of value types from which to copy
	 * tagged values in {@value #RULE_TV_COPY_FROM_VALUE_TYPE}. Default is '.*'
	 * - to match any value type.
	 */
	public static final String PARAM_TV_COPYFROMVALUETYPE_TYPENAMEREGEX = "valueTypeNameRegex";

	/**
	 * Copies the tagged values specified via configuration parameter
	 * {@value #PARAM_TV_INHERITANCE_GENERAL_LIST} from supertypes of the whole
	 * model down to their subtypes, starting at the top of inheritance trees.
	 * If the tagged value already exists in the subtype, then by default it is
	 * retained. However, the value can also be overwritten and the two values
	 * can be merged - for further details, see configuration parameters
	 * {@value #PARAM_TV_INHERITANCE_OVERWRITE_LIST} and
	 * {@value #PARAM_TV_INHERITANCE_APPEND_LIST}.
	 * <p>
	 * NOTE: Care should be taken in case that the model contains classes with
	 * multiple supertypes.
	 * <p>
	 * NOTE: The implementation currently does not support tagged values with
	 * multiple values.
	 */
	public static final String RULE_TV_INHERITANCE = "rule-trf-taggedValue-inheritance";

	/**
	 * Copy specific set of tagged values (specified via parameter
	 * {@value #PARAM_TV_COPYFROMVALUETYPE_TVSTOCOPY}) from types (specified via
	 * parameter {@value #PARAM_TV_COPYFROMVALUETYPE_TYPENAMEREGEX}) to
	 * properties that have one of these types as value type. This can be useful
	 * for in case of tagged values like 'length', 'rangeMinimum',
	 * 'rangeMaximum', and 'pattern' that are defined on types (especially:
	 * basic types) rather than on properties, and these types are mapped to
	 * other types (e.g. 'CharacterString').
	 */
	public static final String RULE_TV_COPY_FROM_VALUE_TYPE = "rule-trf-taggedValue-copyFromValueType";

	private GenericModel genModel = null;
	private Options options = null;
	private ShapeChangeResult result = null;

	@Override
	public void process(GenericModel genModel, Options options,
			TransformerConfiguration trfConfig, ShapeChangeResult result)
			throws ShapeChangeAbortException {

		this.genModel = genModel;
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

		if (rules.contains(RULE_TV_INHERITANCE)) {
			applyRuleTaggedValueInheritance(genModel, trfConfig);
		}

		if (rules.contains(RULE_TV_COPY_FROM_VALUE_TYPE)) {
			applyRuleTaggedValueCopyFromValueType(genModel, trfConfig);
		}

		// apply post-processing (nothing to do right now)
	}

	private void applyRuleTaggedValueCopyFromValueType(GenericModel genModel2,
			TransformerConfiguration trfConfig) {

		List<String> tvsToCopyAsList = trfConfig.parameterAsStringList(
				PARAM_TV_COPYFROMVALUETYPE_TVSTOCOPY, null, true, true);

		if (tvsToCopyAsList.isEmpty()) {
			result.addError(this, 200);
			return;
		}

		Joiner joiner = Joiner.on(",");
		String tvsToCopy = joiner.join(tvsToCopyAsList);

		String typeNameRegexParamValue = trfConfig.parameterAsString(
				PARAM_TV_COPYFROMVALUETYPE_TYPENAMEREGEX, ".*", false, true);
		Pattern typeNameRegex = null;
		try {
			typeNameRegex = Pattern.compile(typeNameRegexParamValue);
		} catch (PatternSyntaxException e) {
			result.addError(this, 10, typeNameRegexParamValue,
					PARAM_TV_COPYFROMVALUETYPE_TYPENAMEREGEX, e.getMessage(),
					RULE_TV_COPY_FROM_VALUE_TYPE);
			return;
		}

		for (GenericPropertyInfo genPi : genModel.selectedSchemaProperties()) {

			Type ti = genPi.typeInfo();

			if (ti.name != null && typeNameRegex.matcher(ti.name).matches()) {

				ClassInfo valueType = genModel.classByIdOrName(ti);
				
				if (valueType != null) {

					TaggedValues valueTypeTVs = valueType
							.taggedValuesForTagList(tvsToCopy);
					
					TaggedValues genPiTVsCopy = genPi.taggedValuesAll();
					genPiTVsCopy.putAll(valueTypeTVs);
					genPi.setTaggedValues(genPiTVsCopy, true);
				}
			}
		}

	}

	private void applyRuleTaggedValueInheritance(GenericModel genModel,
			TransformerConfiguration trfConfig) {

		List<String> generalIn = trfConfig.parameterAsStringList(
				PARAM_TV_INHERITANCE_GENERAL_LIST, null, true, true);

		if (generalIn.isEmpty()) {
			/*
			 * NOTE: The configuration validator checks that the parameter
			 * contains an actual value. However, since validation of the
			 * configuration can be disabled, we still have this check.
			 */
			return;
		}

		List<String> overwriteIn = trfConfig.parameterAsStringList(
				PARAM_TV_INHERITANCE_OVERWRITE_LIST, null, true, true);
		List<String> appendIn = trfConfig.parameterAsStringList(
				PARAM_TV_INHERITANCE_APPEND_LIST, null, true, true);

		String appendSeparator = trfConfig.parameterAsString(
				PARAM_TV_INHERITANCE_APPEND_SEPARATOR,
				DEFAULT_TV_INHERITANCE_APPEND_SEPARATOR, true, false);

		/*
		 * Normalize tagged values. Ignore overwrite-TVs that are not contained
		 * in general-TVs. Ignore append-TVs that are not contained in
		 * general-TVs or contained in overwrite-TVs.
		 */
		SortedSet<String> generalTVs = new TreeSet<String>();
		SortedSet<String> overwriteTVs = new TreeSet<String>();
		SortedSet<String> appendTVs = new TreeSet<String>();

		/*
		 * NOTE: Normalization of tags is currently restricted to a set of
		 * well-known tags (related to descriptors).
		 */

		for (String tv : generalIn) {
			generalTVs.add(options.normalizeTag(tv));
		}
		for (String tv : overwriteIn) {
			String normalizedTV = options.normalizeTag(tv);
			if (generalTVs.contains(normalizedTV)) {
				overwriteTVs.add(normalizedTV);
			}
		}
		for (String tv : appendIn) {
			String normalizedTV = options.normalizeTag(tv);
			if (generalTVs.contains(normalizedTV)
					&& !overwriteTVs.contains(normalizedTV)) {
				appendTVs.add(normalizedTV);
			}
		}

		// identify top-level supertypes in model
		SortedSet<GenericClassInfo> topLevelSupertypes = new TreeSet<GenericClassInfo>();

		for (GenericClassInfo genCi : genModel.getGenClasses().values()) {
			if (!genCi.hasSupertypes() && genCi.hasSubtypes()) {
				topLevelSupertypes.add(genCi);
			}
		}

		for (GenericClassInfo genCi : topLevelSupertypes) {
			applyTaggedValueInheritance(genCi, generalTVs, overwriteTVs,
					appendTVs, appendSeparator);
		}
	}

	private void applyTaggedValueInheritance(GenericClassInfo genCi,
			SortedSet<String> generalTVs, SortedSet<String> overwriteTVs,
			SortedSet<String> appendTVs, String appendSeparator) {

		for (String subtypeId : genCi.subtypes()) {

			GenericClassInfo subtype = (GenericClassInfo) genModel
					.classById(subtypeId);

			for (String tv : generalTVs) {

				String tvValue = genCi.taggedValue(tv);

				if (tvValue != null) {

					String subtypeTvValue = subtype.taggedValue(tv);

					if (subtypeTvValue == null) {

						subtype.setTaggedValue(tv, tvValue, false);

						MessageContext mc = result.addInfo(this, 100, tv,
								tvValue, subtype.name());
						if (mc != null) {
							mc.addDetail(this, 4, genCi.fullName());
							mc.addDetail(this, 5, subtype.fullName());
						}

					} else {

						// determine behavior
						if (overwriteTVs.contains(tv)) {

							// overwrite TV
							subtype.setTaggedValue(tv, tvValue, false);

							MessageContext mc = result.addInfo(this, 101, tv,
									tvValue, subtype.name(), subtypeTvValue);
							if (mc != null) {
								mc.addDetail(this, 4, genCi.fullName());
								mc.addDetail(this, 5, subtype.fullName());
							}

						} else if (appendTVs.contains(tv)) {

							// append TV
							String newSubtypeTvValue = subtypeTvValue
									+ appendSeparator + tvValue;

							subtype.setTaggedValue(tv, newSubtypeTvValue,
									false);

							MessageContext mc = result.addInfo(this, 102,
									tvValue, tv, subtype.name(),
									newSubtypeTvValue);
							if (mc != null) {
								mc.addDetail(this, 4, genCi.fullName());
								mc.addDetail(this, 5, subtype.fullName());
							}

						} else {

							// retain TV
							MessageContext mc = result.addInfo(this, 103, tv,
									subtypeTvValue, subtype.name());
							if (mc != null) {
								mc.addDetail(this, 4, genCi.fullName());
								mc.addDetail(this, 5, subtype.fullName());
							}
						}
					}
				}
			}

			// recursively apply to subtype
			applyTaggedValueInheritance(subtype, generalTVs, overwriteTVs,
					appendTVs, appendSeparator);
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
		case 4:
			return "Context: supertype '$1$'";
		case 5:
			return "Context: subtype '$1$'";

		case 10:
			return "Syntax exception for regular expression '$1$' of parameter '$2$'. Message is: $3$. $4$ will not have any effect.";

		// Messages for RULE_TV_INHERITANCE
		case 100:
			return "Adding tagged value $1$=$2$ to $3$.";
		case 101:
			return "Overwriting tagged value $1$=$2$ in $3$. Previous value was: $4$";
		case 102:
			return "Appending '$1$' to tagged value $2$ in $3$. New value is: $4$.";
		case 103:
			return "Retaining tagged value $1$=$2$ in $3$.";

		case 200:
			return "Required parameter '" + PARAM_TV_COPYFROMVALUETYPE_TVSTOCOPY
					+ "' was not set or does not contain any value. '"
					+ RULE_TV_COPY_FROM_VALUE_TYPE
					+ "' will not have any effect.";

		default:
			return "(" + this.getClass().getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
