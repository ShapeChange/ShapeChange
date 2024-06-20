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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Advanced process configuration entry with model element selection information
 * for a specific process rule.
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class ProcessRuleModelElementSelectionConfigurationEntry {

    private String rule = null;
    private ModelElementSelectionInfo meselection = null;

    public ProcessRuleModelElementSelectionConfigurationEntry(String rule, ModelElementSelectionInfo meselect) {
	super();
	this.rule = rule;
	this.meselection = meselect;
    }

    /**
     * @return the descriptor
     */
    public String getRule() {
	return this.rule;
    }

    /**
     * @return the modelElementSelectionInfo; cannot be <code>null</code>
     */
    public ModelElementSelectionInfo getModelElementSelectionInfo() {
	return this.meselection;
    }

    /**
     * @param advancedProcessConfigurations An sc:advancedProcessConfigurations
     *                                      element; can be <code>null</code>
     * @return list of ProcessRuleModelElementSelection configuration entries; can
     *         be empty but not <code>null</code>
     * @throws ShapeChangeParseException tbd
     */
    public static List<ProcessRuleModelElementSelectionConfigurationEntry> parseAndValidateConfigurationEntries(
	    Element advancedProcessConfigurations) throws ShapeChangeParseException {

	List<ProcessRuleModelElementSelectionConfigurationEntry> prmesEntries = new ArrayList<>();

	if (advancedProcessConfigurations == null) {
	    return prmesEntries;
	}
	
	// identify ProcessRuleModelElementSelection elements
	List<Element> prmesEs = new ArrayList<>();

	NodeList prmesNl = advancedProcessConfigurations.getElementsByTagName("ProcessRuleModelElementSelection");

	if (prmesNl != null && prmesNl.getLength() != 0) {
	    for (int k = 0; k < prmesNl.getLength(); k++) {
		Node n = prmesNl.item(k);
		if (n.getNodeType() == Node.ELEMENT_NODE) {
		    prmesEs.add((Element) n);
		}
	    }
	}

	List<String> compilationErrors = new ArrayList<>();
	
	for (int i = 0; i < prmesEs.size(); i++) {

	    String indexForMsg = "" + (i + 1);

	    Element prmesE = prmesEs.get(i);

	    // parse rule name
	    String ruleName = prmesE.getAttribute("rule").trim();

	    ModelElementSelectionInfo meselect = ModelElementSelectionInfo.parse(prmesE);
	    try {
		meselect.validate();
	    } catch (ModelElementSelectionParseException e) {
		compilationErrors.add(indexForMsg + " ProcessRuleModelElementSelection element: " + e.getMessage());
	    }
	    
	    prmesEntries.add(new ProcessRuleModelElementSelectionConfigurationEntry(ruleName, meselect));
	}

	if (!compilationErrors.isEmpty()) {
	    throw new ShapeChangeParseException(StringUtils.join(compilationErrors, ", "));
	}
	
	return prmesEntries;
    }
}
