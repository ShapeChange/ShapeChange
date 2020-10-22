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
package de.interactive_instruments.ShapeChange.Transformation.LinkedDocument;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.wml.P;

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
import de.interactive_instruments.ShapeChange.Util.docx.DocxUtil;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
public class LinkedDocumentTransformer implements Transformer, MessageSource {

	public static final String PARAM_PREPEND_TEXT_VALUE = "prependTextValue";
	public static final String PARAM_PREPEND_TEXT_HORIZONTALLINE = "prependHorizontalLine";

	public static final String RULE_PREPEND_TEXT = "rule-trf-all-prependText";

	private Options options = null;
	private ShapeChangeResult result = null;
	private Set<String> rules = null;
	private TransformerConfiguration config = null;
	private GenericModel model = null;

	@Override
	public void process(GenericModel model, Options options,
			TransformerConfiguration config, ShapeChangeResult result)
			throws ShapeChangeAbortException {

		this.model = model;
		this.options = options;
		this.result = result;
		this.config = config;

		Map<String, ProcessRuleSet> ruleSets = config.getRuleSets();

		// for now we simply get the set of all rules defined for the
		// transformation
		rules = new HashSet<String>();
		if (!ruleSets.isEmpty()) {
			for (ProcessRuleSet ruleSet : ruleSets.values()) {
				if (ruleSet.getAdditionalRules() != null) {
					rules.addAll(ruleSet.getAdditionalRules());
				}
			}
		}

		if (rules.contains(RULE_PREPEND_TEXT)) {

			result.addProcessFlowInfo(null, 20103, RULE_PREPEND_TEXT);
			applyRulePrependText();
		}
	}

	private void applyRulePrependText() {

		String text = config.parameterAsString(PARAM_PREPEND_TEXT_VALUE, null,
				false, true);
		boolean horizontalLine = config
				.parameterAsBoolean(PARAM_PREPEND_TEXT_HORIZONTALLINE, false);

		if (StringUtils.isBlank(text) && !horizontalLine) {
			// then there's nothing to transform
			return;
		}

		for (GenericClassInfo genCi : model.selectedSchemaClasses()) {

			if (genCi.getLinkedDocument() != null) {

				String specificText = text == null ? null
						: text.replaceAll("\\$TYPE\\$", genCi.name());

				try {
					File trfFile = File.createTempFile("transformedLinkedDoc",
							".docx", options.linkedDocumentsTmpDir());
					trfFile.deleteOnExit();

					FileInputStream linkedDocSupertypeIS = new FileInputStream(
							genCi.getLinkedDocument());
					WordprocessingMLPackage wmlPackage = WordprocessingMLPackage
							.load(linkedDocSupertypeIS);
					MainDocumentPart mdp = wmlPackage.getMainDocumentPart();

					/*
					 * When prepending, ensure that the order in which you add
					 * content to the MainDocumentPart at index 0 is from last
					 * to first. So, since the horizontal line shall be placed
					 * in the document after the text, prepend it first -
					 * followed by the text.
					 */
					if (horizontalLine) {
						P pHorizLine = DocxUtil.createHorizontalLine();
						mdp.getContent().add(0, pHorizLine);
					}

					if (specificText != null) {
						P pText = DocxUtil.createText(specificText);
						mdp.getContent().add(0, pText);
					}

					/*
					 * Finally, save the modified docx and set it as new linked
					 * document of the class.
					 */
					wmlPackage.save(trfFile);
					genCi.setLinkedDocument(trfFile);

				} catch (IOException | Docx4JException e) {
					MessageContext mc = result.addError(this, 100, genCi.name(),
							e.getMessage());
					if (mc != null) {
						mc.addDetail(this, 2, genCi.fullName());
					}
				}
			}
		}
	}
	    
	@Override
	public String message(int mnr) {

		/*
		 * NOTE: A leading ?? in a message text suppresses multiple appearance
		 * of a message in the output.
		 */
		switch (mnr) {

		case 1:
			return "Context: property '$1$'";
		case 2:
			return "Context: class '$1$'";

		// 100-199 Messages for RULE_PREPEND_TEXT
		case 100:
			return "An exception occurred while processing the linked document of type '$1$'. Message is: $2$";

		default:
			return "(" + this.getClass().getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
