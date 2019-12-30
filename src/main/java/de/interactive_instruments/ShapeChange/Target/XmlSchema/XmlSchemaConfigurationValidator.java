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
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange.Target.XmlSchema;

import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.ShapeChange.ConfigurationValidator;
import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessConfiguration;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.Descriptor;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot
 *         de)
 */
public class XmlSchemaConfigurationValidator
		implements ConfigurationValidator, MessageSource {

	// these fields will be initialized when isValid(...) is called
	private ProcessConfiguration config = null;
	private Options options = null;
	private ShapeChangeResult result = null;

	@Override
	public boolean isValid(ProcessConfiguration config, Options options,
			ShapeChangeResult result) {

		this.config = config;
		this.options = options;
		this.result = result;

		boolean isValid = true;
		
		// check parameter: schematronQueryBinding
		String explicitSchematronQueryBinding = options.parameterAsString(this.getClass().getName(), "schematronQueryBinding", null, false, true);
		if(explicitSchematronQueryBinding != null) {
		    if(!explicitSchematronQueryBinding.equalsIgnoreCase("xslt2")) {
			result.addError(this, 101, explicitSchematronQueryBinding);
		isValid = false;
		    }
		}

		// check parameter: representDescriptors
		String representDescriptorsParamValue = options.parameterAsString(
				Options.TargetXmlSchemaClass, "representDescriptors", null,
				false, true);
		if (representDescriptorsParamValue != null) {

			List<String> namesOfDescriptorsToRepresent = options
					.parameterAsStringList(Options.TargetXmlSchemaClass,
							"representDescriptors", null, true, true);

			SortedSet<String> unknownDescriptors = new TreeSet<>();

			for (String descriptorName : namesOfDescriptorsToRepresent) {
				try {
					Descriptor.valueOf(
							descriptorName.toUpperCase(Locale.ENGLISH));
				} catch (IllegalArgumentException e) {
					unknownDescriptors.add(descriptorName);
				}
			}

			if (!unknownDescriptors.isEmpty()) {
				String unknownDescriptorsAsString = StringUtils
						.join(unknownDescriptors, ", ");
				result.addError(this, 100, representDescriptorsParamValue,
						unknownDescriptorsAsString);
				isValid = false;
			}
		}

		return isValid;
	}

	@Override
	public String message(int mnr) {

		switch (mnr) {
		case 0:
			return "Context: XmlSchema target configuration element with 'inputs'='$1$'.";

		case 100:
			return "Configuration parameter 'representDescriptors' contains unknown descriptors. Parameter value is: '$1$'. Unknown descriptors are: '$2$'.";
		case 101:
			return "Configuration parameter 'schematronQueryBinding', if set, must have a value equal to (ignoring case) 'xslt2'. Found parameter value: '$1$'.";

		default:
			return "(" + XmlSchemaConfigurationValidator.class.getName()
					+ ") Unknown message with number: " + mnr;
		}
	}
}
