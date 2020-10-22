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
package de.interactive_instruments.ShapeChange.Target.Ontology;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.ShapeChange.AbstractConfigurationValidator;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessConfiguration;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.TargetOwlConfiguration;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class OWLISO19150ConfigurationValidator extends AbstractConfigurationValidator {

    protected SortedSet<String> allowedParametersWithStaticNames = new TreeSet<>(Stream
	    .of(OWLISO19150.PARAM_CODE_LIST_OWL_CLASS_NAMESPACE,
		    OWLISO19150.PARAM_CODE_LIST_OWL_CLASS_NAMESPACE_FOR_ENUMERATIONS, OWLISO19150.PARAM_CODE_NAMESPACE,
		    OWLISO19150.PARAM_CODE_NAMESPACE_FOR_ENUMERATIONS, OWLISO19150.PARAM_DEFAULT_TYPE_IMPL,
		    OWLISO19150.PARAM_GENERAL_PROPERTY_NSABR, OWLISO19150.PARAM_LANGUAGE,
		    OWLISO19150.PARAM_ONTOLOGYNAME_CODE_NAME, OWLISO19150.PARAM_ONTOLOGYNAME_TAGGED_VALUE_NAME,
		    OWLISO19150.PARAM_OUTPUTFORMAT, OWLISO19150.PARAM_PROP_EXTERNAL_REFERENCE_TARGET_PROPERTY,
		    OWLISO19150.PARAM_RDF_NAMESPACE_SEPARATOR, OWLISO19150.PARAM_RDFXMLWRITER_BLOCKRULES,
		    OWLISO19150.PARAM_SKOS_CONCEPT_SCHEME_SUBCLASS_NAME_SUFFIX,
		    OWLISO19150.PARAM_SKOS_CONCEPT_SCHEME_SUFFIX, OWLISO19150.PARAM_SOURCE,
		    OWLISO19150.PARAM_SOURCE_TAGGED_VALUE_NAME,
		    OWLISO19150.PARAM_SUPPRESS_MESSAGES_FOR_UNSUPPORTED_CLASS_CATEGORY, OWLISO19150.PARAM_URIBASE)
	    .collect(Collectors.toSet()));
    protected Pattern regexForAllowedParametersWithDynamicNames = null;

    @Override
    public boolean isValid(ProcessConfiguration pConfig, Options options, ShapeChangeResult result) {

	TargetOwlConfiguration config = (TargetOwlConfiguration) pConfig;

	boolean isValid = true;

	allowedParametersWithStaticNames.addAll(getCommonTargetParameters());
	isValid = validateParameters(allowedParametersWithStaticNames, regexForAllowedParametersWithDynamicNames,
		config.getParameters().keySet(), result) && isValid;

	String generalPropertyNamespaceAbbreviation = config
		.getParameterValue(OWLISO19150.PARAM_GENERAL_PROPERTY_NSABR);

	if (StringUtils.isNotBlank(generalPropertyNamespaceAbbreviation)) {
	    /*
	     * Check that the target configuration contains a namespace definition with
	     * matching abbreviation.
	     */
	    if (!config.hasNamespaceWithAbbreviation(generalPropertyNamespaceAbbreviation)) {
		result.addError(this, 100, generalPropertyNamespaceAbbreviation);
		isValid = false;
	    }
	}

	return isValid;
    }

    @Override
    public String message(int mnr) {

	switch (mnr) {
	case 0:
	    return "Context: OWLISO19150 target configuration element with 'inputs'='$1$'.";

	case 100:
	    return "Configuration parameter '" + OWLISO19150.PARAM_GENERAL_PROPERTY_NSABR
		    + "' is set, with value '$1$'. However, no namespace is configured with that abbreviation.";

	default:
	    return "(" + OWLISO19150ConfigurationValidator.class.getName() + ") Unknown message with number: " + mnr;
	}
    }
}
