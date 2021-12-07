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
package de.interactive_instruments.ShapeChange.Transformation.CityGML;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessRuleSet;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.TransformerConfiguration;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericClassInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel;
import de.interactive_instruments.ShapeChange.Transformation.Transformer;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
public class CityGMLTransformer implements MessageSource, Transformer {

	public static final String RULE_CITYGML_CREATE_ADE = "rule-trf-CityGML-createADE";

	public static final String TV_CITYGML_TARGET = "cityGmlTargetType";

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

		if (rules.contains(RULE_CITYGML_CREATE_ADE)) {
			applyRuleCreateADE(genModel, trfConfig);
		}

		// apply post-processing (nothing to do right now)
	}

	private void applyRuleCreateADE(GenericModel genModel,
			TransformerConfiguration trfConfig) {

		/*
		 * Look up the _CityObject feature type (NOT an <<ADEElement>>).
		 */
		GenericClassInfo cityObject = null;
		for (GenericClassInfo genCi : genModel.getGenClasses().values()) {

			if (genCi.category() == Options.FEATURE
					&& genCi.name().equals("_CityObject")
					&& !genCi.stereotype("adeelement")) {
				cityObject = genCi;
				break;
			}
		}

		if (cityObject == null) {
			result.addError(this, 102);
			return;
		}

		/*
		 * Create map with key = qname, value = according class. It will be used
		 * to find CityGML classes to which schema classes are mapped.
		 */
		Map<String, GenericClassInfo> classByQname = new HashMap<String, GenericClassInfo>();

		for (GenericClassInfo genCi : genModel.getGenClasses().values()) {

			if (genCi.pkg() != null && genCi.pkg().xmlns() != null) {

				String qname = genCi.qname();

				if (classByQname.containsKey(qname)) {

					GenericClassInfo existingCls = classByQname.get(qname);

					MessageContext mc = result.addWarning(this, 100, qname);
					if (mc != null) {
						mc.addDetail(this, 1, existingCls.fullNameInSchema());
						mc.addDetail(this, 1, genCi.fullNameInSchema());
					}

				} else {
					classByQname.put(qname, genCi);
				}
			}
		}

		/*
		 * Inspect all feature types from selected schemas: if tagged value
		 * TV_CITYGML_TARGET is defined, or if a map entry is defined for the
		 * type (either one with a QName as value), then establish an
		 * inheritance relationship to that type (if it can be found in the
		 * QName mappings). Otherwise map the feature type to _CityObject.
		 * Change the category of previously existing supertypes to mixin.
		 */
		for (GenericClassInfo genCi : genModel.selectedSchemaClasses()) {

			if (genCi.category() != Options.FEATURE) {
				continue;
			}

			String qname = null;

			if (StringUtils.isNotBlank(genCi.taggedValue(TV_CITYGML_TARGET))) {

				qname = genCi.taggedValue(TV_CITYGML_TARGET).trim();

			} else if (trfConfig.hasMappingForType(RULE_CITYGML_CREATE_ADE,
					genCi.name())) {

				qname = trfConfig.getMappingForType(RULE_CITYGML_CREATE_ADE,
						genCi.name()).getTargetType().trim();
			}

			GenericClassInfo targetType = null;

			if (StringUtils.isNotBlank(qname)) {

				if (classByQname.containsKey(qname)) {

					targetType = classByQname.get(qname);

				} else {

					MessageContext mc = result.addError(this, 101, genCi.name(),
							qname);
					if (mc != null) {
						mc.addDetail(this, 1, genCi.fullNameInSchema());
					}

					continue;
				}

			} else {

				targetType = cityObject;
			}

			/*
			 * first change category of already existing supertypes to mixins
			 */
			for (String supertypeId : genCi.supertypes()) {
				GenericClassInfo supertype = genModel.getGenClasses()
						.get(supertypeId);
				supertype.setCategory(Options.MIXIN);
			}

			// now establish inheritance relationship to target type
			targetType.addSubtype(genCi.id());
			genCi.addSupertype(targetType.id());
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

		// Messages for RULE_CITYGML_CREATE_ADE
		case 100:
			return "QName '$1$' not unique within the model. No QName mapping will be established for the second class listed in the details of this message.";
		case 101:
			return "QName of class '$1$' is '$2$'. No mapping is available for this QName.";
		case 102:
			return "Type '_CityObject' was not found in the model. This type is required for processing of '"
					+ RULE_CITYGML_CREATE_ADE
					+ "'. The rule will not be applied.";

		default:
			return "(" + this.getClass().getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
