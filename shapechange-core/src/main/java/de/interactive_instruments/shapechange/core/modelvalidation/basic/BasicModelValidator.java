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
 * (c) 2002-2024 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core.modelvalidation.basic;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;

import de.interactive_instruments.shapechange.core.util.XMLUtil;
import de.interactive_instruments.shapechange.core.ModelElementSelectionInfo;
import de.interactive_instruments.shapechange.core.ModelElementSelectionParseException;
import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ShapeChangeAbortException;
import de.interactive_instruments.shapechange.core.ShapeChangeParseException;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.ValidatorConfiguration;
import de.interactive_instruments.shapechange.core.model.DescriptorAndTagResolver;
import de.interactive_instruments.shapechange.core.model.Info;
import de.interactive_instruments.shapechange.core.model.Model;
import de.interactive_instruments.shapechange.core.modelvalidation.AbstractModelValidator;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class BasicModelValidator extends AbstractModelValidator {

    @Override
    public boolean isValid(Model model, ValidatorConfiguration validatorConfig) throws ShapeChangeAbortException {

	boolean modelIsValid = true;

	modelIsValid = modelIsValid && applyContentTests(model, validatorConfig);

	// further validation tasks to be added as needed in the future

	return modelIsValid;
    }

    private boolean applyContentTests(Model model, ValidatorConfiguration validatorConfig) {

	Options options = model.options();
	ShapeChangeResult result = model.result();

	// ==========================
	// Parse relevant items from the configuration
	// ==========================

	List<AbstractContentTestElement> contentTestElements = new ArrayList<>();

	/*
	 * Check if advanced process configuration has relevant content. If so, execute
	 * according validation.
	 */
	if (validatorConfig.hasAdvancedProcessConfigurations()) {

	    Element advancedProcessConfigElmt = validatorConfig.getAdvancedProcessConfigurations();

	    try {
		List<AbstractContentTestElement> contentTestElmts = parseAndValidateContentTestElements(
			advancedProcessConfigElmt);
		contentTestElements = contentTestElmts;
	    } catch (ShapeChangeParseException e) {
		result.addError(this, 100, e.getMessage());
	    }
	}

	// ==========================
	// Perform model validation
	// ==========================

	boolean testResult = true;

	List<Info> infoTypesFromSelectedSchemas = new ArrayList<Info>();

	infoTypesFromSelectedSchemas.addAll(model.allPackagesFromSelectedSchemas());
	infoTypesFromSelectedSchemas.addAll(model.selectedSchemaClasses());
	infoTypesFromSelectedSchemas.addAll(model.selectedSchemaProperties());
	infoTypesFromSelectedSchemas.addAll(model.selectedSchemaAssociations());

	for (Info i : infoTypesFromSelectedSchemas) {

	    for (AbstractContentTestElement testElmt : contentTestElements) {

		if (testElmt instanceof DescriptorContentTestElement descriptorTestElmt) {

		    if (descriptorTestElmt.getModelElementSelectionInfo().matches(i)) {

			String desc = descriptorTestElmt.getDescriptorOrTaggedValue();

			/*
			 * identify the descriptor or tagged value from the field and get value(s)
			 */
			List<String> values = new ArrayList<>();
			boolean descRecognized = DescriptorAndTagResolver.resolveDescriptorOrTag(i, desc,
				options.language(), values);

			if (descRecognized) {

			    if (values.isEmpty()) {
				values.add("");
			    }

			    for (String value : values) {
				Matcher m = descriptorTestElmt.getRegexPattern().matcher(value);
				if (!m.matches()) {
				    testResult = false;
				    report(i, this, 102, desc, descriptorTestElmt.getRegex(), value,
					    validatorConfig.getValidationMode());
				}
			    }

			} else {
			    // should be checked via configuration validator
			    result.addError(this, 101, desc);
			}
		    }
		}
	    }
	}

	return testResult;
    }

    /**
     * @param advancedProcessConfigElmt the advancedProcessConfigurations element
     *                                  from the validator configuration
     * @return list of content test elements found in the
     *         advancedProcessConfigurations element; can be empty but not
     *         <code>null</code>
     * @throws ShapeChangeParseException If one of the content test attributes
     *                                   contained an invalid value.
     */
    public static List<AbstractContentTestElement> parseAndValidateContentTestElements(
	    Element advancedProcessConfigElmt) throws ShapeChangeParseException {

	List<AbstractContentTestElement> result = new ArrayList<>();

	Element basicContentTestsElmt = XMLUtil.getFirstElement(advancedProcessConfigElmt, "BasicContentTests");

	if (basicContentTestsElmt != null) {

	    Element tests = XMLUtil.getFirstElement(basicContentTestsElmt, "tests");

	    List<Element> contentTestElements = XMLUtil.getElementNodes(tests.getChildNodes());

	    List<String> compilationErrors = new ArrayList<>();

	    for (int i = 0; i < contentTestElements.size(); i++) {

		Element elmt = contentTestElements.get(i);

		AbstractContentTestElement actElmt = null;

		if ("DescriptorContentTest".equalsIgnoreCase(elmt.getLocalName())) {

		    String descriptorOrTaggedValue = elmt.getAttribute("descriptorOrTaggedValue");
		    String regex = elmt.getAttribute("regex");
		    Pattern regexPattern = null;

		    try {
			regexPattern = Pattern.compile(regex);
		    } catch (PatternSyntaxException e) {
			compilationErrors
				.add(i + " Descriptor content test element (regex attribute): " + e.getMessage());
		    }

		    DescriptorContentTestElement dctElmt = new DescriptorContentTestElement(descriptorOrTaggedValue,
			    regex, regexPattern);

		    actElmt = dctElmt;

		} else {

		    // future work
		}

		// set common optional fields

		ModelElementSelectionInfo selectionInfo = ModelElementSelectionInfo.parse(elmt);
		try {
		    selectionInfo.validate();
		} catch (ModelElementSelectionParseException e) {
		    compilationErrors
			    .add(i + " Content test element (model element selection attribute(s)): " + e.getMessage());
		}
		actElmt.setModelElementSelectionInfo(selectionInfo);

		result.add(actElmt);
	    }

	    if (!compilationErrors.isEmpty()) {
		throw new ShapeChangeParseException(StringUtils.join(compilationErrors, ", "));
	    }
	}

	return result;
    }

    @Override
    public String message(int mnr) {

	switch (mnr) {

	case 0:
	    return "Context: class '$1$'";
	case 1:
	    return "Context: property '$1$'";
	case 2:
	    return "Context: $1$";

	case 100:
	    return "Invalid content test element(s) encountered (they will be ignored): $1$";
	case 101:
	    return "??Descriptor content test: descriptor or tagged value '$1$' is not recognized.";
	case 102:
	    return "Descriptor content test failed (descriptorOrTaggedValue '$1$', regex '$2$', value '$3$').";

	default:
	    return "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	}
    }

}
