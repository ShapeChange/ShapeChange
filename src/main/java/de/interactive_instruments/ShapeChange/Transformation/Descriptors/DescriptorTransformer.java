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
package de.interactive_instruments.ShapeChange.Transformation.Descriptors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.ModelElementSelectionInfo;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessRuleSet;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.TransformerConfiguration;
import de.interactive_instruments.ShapeChange.Model.Descriptor;
import de.interactive_instruments.ShapeChange.Model.Info;
import de.interactive_instruments.ShapeChange.Model.LangString;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel;
import de.interactive_instruments.ShapeChange.Transformation.Transformer;
import de.interactive_instruments.ShapeChange.Util.XMLUtil;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class DescriptorTransformer implements MessageSource, Transformer {

	/**
	 * Updates descriptors of specific model elements. The 'DescriptorValue'
	 * elements contained in the advanced process configuration of the
	 * transformation define which descriptors of which model elements are
	 * updated.
	 * <p>
	 * Each 'DescriptorValue' element contains the name of a specific
	 * descriptor. The element also contains attributes to select the model
	 * elements for which the descriptor shall be updated. If the element has a
	 * 'value', the descriptor of selected model elements is set to it
	 * (replacing any previously stored values. If no 'value' is present, then
	 * the descriptor will be removed on selected model elements.
	 */
	public static final String RULE_UPDATE_DESCRIPTORS = "rule-trf-all-updateDescriptors";

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

		if (rules.contains(RULE_UPDATE_DESCRIPTORS)) {
			applyRuleUpdateDescriptors(trfConfig);
		}

		// apply post-processing (nothing to do right now)
	}

	private void applyRuleUpdateDescriptors(
			TransformerConfiguration trfConfig) {

		/*
		 * identify DescriptorValues in advancedProcessConfigurations element of
		 * transformer configuration
		 */
		if (trfConfig.getAdvancedProcessConfigurations() == null) {

			result.addWarning(this, 100);

		} else {

			List<DescriptorValueConfigurationEntry> dvcEntries = parseDescriptorValueConfigurationEntries(
					trfConfig.getAdvancedProcessConfigurations());

			if (!dvcEntries.isEmpty()) {

				List<Info> infoTypesFromSelectedSchemas = new ArrayList<Info>();

				infoTypesFromSelectedSchemas
						.addAll(genModel.allPackagesFromSelectedSchemas());
				infoTypesFromSelectedSchemas
						.addAll(genModel.selectedSchemaClasses());
				infoTypesFromSelectedSchemas
						.addAll(genModel.selectedSchemaProperties());
				infoTypesFromSelectedSchemas
						.addAll(genModel.selectedSchemaAssociations());

				for (Info i : infoTypesFromSelectedSchemas) {

					for (DescriptorValueConfigurationEntry dvce : dvcEntries) {

						if (dvce.getModelElementSelectionInfo().matches(i)) {

							if (dvce.hasValues()) {
								i.descriptors().put(dvce.getDescriptor(),
										dvce.getCopyOfValues());
							} else {
								i.descriptors().remove(dvce.getDescriptor());
							}
						}
					}
				}
			}
		}
	}

	/**
	 * @param advancedProcessConfigurations
	 * @return list of descriptor value configuration entries; can be empty but
	 *         not <code>null</code>
	 */
	private List<DescriptorValueConfigurationEntry> parseDescriptorValueConfigurationEntries(
			Element advancedProcessConfigurations) {

		List<DescriptorValueConfigurationEntry> dvcEntries = new ArrayList<DescriptorValueConfigurationEntry>();

		// identify DescriptorValue elements
		List<Element> dvEs = new ArrayList<Element>();

		NodeList dvNl = advancedProcessConfigurations
				.getElementsByTagName("DescriptorValue");

		if (dvNl != null && dvNl.getLength() != 0) {
			for (int k = 0; k < dvNl.getLength(); k++) {
				Node n = dvNl.item(k);
				if (n.getNodeType() == Node.ELEMENT_NODE) {

					dvEs.add((Element) n);
				}
			}
		}

		for (int i = 0; i < dvEs.size(); i++) {

			String indexForMsg = "" + (i + 1);

			Element dvE = dvEs.get(i);

			// parse descriptor name
			String name = dvE.getAttribute("descriptorName");
			Descriptor descriptor = null;
			try {
				descriptor = Descriptor
						.valueOf(name.toUpperCase(Locale.ENGLISH));
			} catch (IllegalArgumentException e) {
				result.addError(this, 101, name, indexForMsg);
				continue;
			}

			List<LangString> descriptorValues = new ArrayList<LangString>();

			Element valuesE = XMLUtil.getFirstElement(dvE, "values");
			if (valuesE != null) {

				List<Element> langStringEs = XMLUtil.getChildElements(valuesE,
						"LangString");

				for (Element lsE : langStringEs) {

					String lang = null;
					if (lsE.hasAttribute("lang")) {
						lang = lsE.getAttribute("lang");
					}

					String value = lsE.getTextContent();
					if (StringUtils.isBlank(value)) {
						value = null;
					}

					if (lang != null || value != null) {
						LangString ls = new LangString(value, lang);
						descriptorValues.add(ls);
					}
				}
			}

			ModelElementSelectionInfo meselect = ModelElementSelectionInfo
					.parse(dvE);

			dvcEntries.add(new DescriptorValueConfigurationEntry(descriptor,
					descriptorValues, meselect));
		}

		return dvcEntries;
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

		// 100-199 Messages for RULE_UPDATE_DESCRIPTORS
		case 100:
			return "No 'advancedProcessConfigurations' element present in the configuration. Descriptors will not be updated.";
		case 101:
			return "Descriptor '$1$' of $2$ DescriptorValue element from the transformer configuration is unknown. The DescriptorValue will be ignored.";

		default:
			return "(" + this.getClass().getName()
					+ ") Unknown message with number: " + mnr;
		}
	}

}
