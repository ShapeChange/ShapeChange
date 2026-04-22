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
 * (c) 2002-2026 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core.transformation.property;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;

import de.interactive_instruments.shapechange.core.ModelElementSelectionInfo;
import de.interactive_instruments.shapechange.core.ModelElementSelectionParseException;
import de.interactive_instruments.shapechange.core.ShapeChangeParseException;
import de.interactive_instruments.shapechange.core.util.XMLUtil;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class PropertyTransformationParser {

    /**
     * @param advancedProcessConfigElmt the advancedProcessConfigurations element
     *                                  from the transformer configuration
     * @return list of JSON Schema annotation elements found in the
     *         advancedProcessConfigurations element; can be empty but not
     *         <code>null</code>
     * @throws ShapeChangeParseException If one of the annotation attributes
     *                                   contained an invalid value.
     */
    public static List<PropertyTransformationElement> parseAndValidatePropertyTransformationElements(
	    Element advancedProcessConfigElmt) throws ShapeChangeParseException {

	List<PropertyTransformationElement> result = new ArrayList<>();

	Element ptsElmt = XMLUtil.getFirstElement(advancedProcessConfigElmt, "PropertyTransformations");

	if (ptsElmt != null) {

	    Element transformations = XMLUtil.getFirstElement(ptsElmt, "transformations");

	    List<Element> transformationElements = XMLUtil.getElementNodes(transformations.getChildNodes());

	    List<String> compilationErrors = new ArrayList<>();

	    for (int i = 0; i < transformationElements.size(); i++) {

		Element elmt = transformationElements.get(i);

		PropertyTransformationElement pte = new PropertyTransformationElement();

		String rule = elmt.getAttribute("rule");
		pte.setRule(rule);

		ModelElementSelectionInfo selectionInfo = ModelElementSelectionInfo.parse(elmt);
		try {
		    selectionInfo.validate();
		} catch (ModelElementSelectionParseException e) {
		    compilationErrors.add(i + " Property Transformation element (rule '" + rule
			    + "'), model element selection attribute(s): " + e.getMessage());
		}
		pte.setModelElementSelectionInfo(selectionInfo);

		result.add(pte);
	    }

	    if (!compilationErrors.isEmpty()) {
		throw new ShapeChangeParseException(StringUtils.join(compilationErrors, ", "));
	    }
	}

	return result;
    }
}
