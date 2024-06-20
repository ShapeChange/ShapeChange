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
 * (c) 2002-2018 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target.Ontology;

import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;

import de.interactive_instruments.ShapeChange.Util.XMLUtil;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot
 *         de)
 *
 */
public class GeneralObjectProperty extends RdfGeneralProperty {

	private SortedSet<String> inverseProperty;
	private SortedSet<PropertyAxiom> propertyCharacteristicAxioms = new TreeSet<>();

	public GeneralObjectProperty(Element gopE) {

		super(gopE);

		this.inverseProperty = new TreeSet<>(
				XMLUtil.getTextContentOfChildElements(gopE, "inverseProperty"));

		String s = XMLUtil.getTextContentOfFirstElement(gopE,
				"propertyCharacteristicAxioms");

		if (StringUtils.isNotBlank(s)) {
			String[] characteristics = s.trim().split(" ");
			for (String c : characteristics) {
				PropertyAxiom opca = PropertyAxiom
						.fromString(c);
				propertyCharacteristicAxioms.add(opca);
			}
		}
	}

	/**
	 * @return the inverseProperty; can be empty but not <code>null</code>
	 */
	public SortedSet<String> getInverseProperty() {
		return inverseProperty;
	}

	/**
	 * @return the propertyCharacteristicAxioms; can be empty but not
	 *         <code>null</code>
	 */
	public SortedSet<PropertyAxiom> getPropertyCharacteristicAxioms() {
		return propertyCharacteristicAxioms;
	}
}
