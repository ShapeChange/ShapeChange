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
 * (c) 2002-2016 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Transformation.TypeConversion;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessRuleSet;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.TransformerConfiguration;
import de.interactive_instruments.ShapeChange.Model.Stereotypes;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericClassInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericPropertyInfo;
import de.interactive_instruments.ShapeChange.Transformation.Transformer;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class TypeConverter implements Transformer, MessageSource {

	public static final String RULE_ENUMERATION_TO_CODELIST = "rule-trf-enumeration-to-codelist";

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

		if (rules.contains(RULE_ENUMERATION_TO_CODELIST)) {
			applyRuleEnumerationToCodelist(genModel, trfConfig);
		}

		// apply post-processing (nothing to do right now)
	}

	private void applyRuleEnumerationToCodelist(GenericModel genModel,
			TransformerConfiguration trfConfig) {

		/*
		 * NOTE: This rule converts all enumerations found in the model to code
		 * lists. If a restriction is desired, for example to enumerations that
		 * belong to the schemas selected for processing, or to specifically
		 * identified application schemas (identified by name or target
		 * namespace regex), then that would need to be added (for example
		 * through new parameters).
		 * 
		 * Likewise, no specific tagged values are set. Rules and parameters to
		 * control the tagged values of relevant classes can be added in the
		 * future.
		 * 
		 * Constraints are not updated either.
		 */

		/*
		 * --- update category of value of properties ---
		 */
		for (GenericPropertyInfo genPi : genModel.getGenProperties().values()) {

			if (genPi.categoryOfValue() == Options.ENUMERATION) {
				genPi.setCategoryOfValue(Options.CODELIST);
			}
		}

		/*
		 * --- update class category ---
		 */
		for (GenericClassInfo genCi : genModel.getGenClasses().values()) {

			if (genCi.category() == Options.ENUMERATION) {
				genCi.setCategory(Options.CODELIST);
				Stereotypes st = genCi.stereotypes();
				st.remove("enumeration");
				st.add("codelist");
				// we need to explicitly set the new Stereotypes cache
				genCi.setStereotypes(st);
			}
		}
	}

	public String message(int mnr) {

		switch (mnr) {
		case 0:
			return "";
		case 1:
			return "";
		default:
			return "(Unknown message)";
		}
	}
}
