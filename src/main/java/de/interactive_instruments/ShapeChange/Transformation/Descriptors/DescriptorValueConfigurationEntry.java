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
import java.util.List;

import de.interactive_instruments.ShapeChange.ModelElementSelectionInfo;
import de.interactive_instruments.ShapeChange.Model.Descriptor;
import de.interactive_instruments.ShapeChange.Model.LangString;

/**
 * Advanced process configuration entry with information to update descriptors
 * of model elements.
 * 
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public class DescriptorValueConfigurationEntry {

	private Descriptor descriptor = null;
	private List<LangString> values = null;
	private ModelElementSelectionInfo meselection = null;

	public DescriptorValueConfigurationEntry(Descriptor descriptor,
			List<LangString> values, ModelElementSelectionInfo meselect) {
		super();
		this.descriptor = descriptor;
		this.values = values;
		this.meselection = meselect;
	}

	/**
	 * @return the descriptor
	 */
	public Descriptor getDescriptor() {
		return this.descriptor;
	}

	/**
	 * @return a copy of the list of values; can be empty but not
	 *         <code>null</code>
	 */
	public List<LangString> getCopyOfValues() {

		List<LangString> valueCopy = new ArrayList<LangString>();

		if (values != null) {
			for (LangString ls : values) {
				valueCopy.add(new LangString(ls.getValue(), ls.getLang()));
			}
		}

		return valueCopy;
	}

	public boolean hasValues() {
		return this.values != null && !this.values.isEmpty();
	}

	/**
	 * @return the modelElementSelectionInfo; cannot be <code>null</code>
	 */
	public ModelElementSelectionInfo getModelElementSelectionInfo() {
		return this.meselection;
	}
}
