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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.ShapeChange.AbstractConfigurationValidator;
import de.interactive_instruments.ShapeChange.ConstraintMapping;
import de.interactive_instruments.ShapeChange.DescriptorTarget;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessConfiguration;
import de.interactive_instruments.ShapeChange.PropertyConversionParameter;
import de.interactive_instruments.ShapeChange.RdfPropertyMapEntry;
import de.interactive_instruments.ShapeChange.RdfTypeMapEntry;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.StereotypeConversionParameter;
import de.interactive_instruments.ShapeChange.TargetOwlConfiguration;
import de.interactive_instruments.ShapeChange.TypeConversionParameter;
import de.interactive_instruments.ShapeChange.Model.DescriptorAndTagResolver;

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
    protected List<Pattern> regexForAllowedParametersWithDynamicNames = null;

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

	/*
	 * Check defaultTypeImplementation - default is owl:Class but we don't check
	 * that here.
	 */
	String defaultTypeImplementation = config.parameterAsString(OWLISO19150.PARAM_DEFAULT_TYPE_IMPL, null, false,
		true);
	if (defaultTypeImplementation != null) {
	    if (!isValidQName(defaultTypeImplementation, config)) {
		result.addError(this, 101, OWLISO19150.PARAM_DEFAULT_TYPE_IMPL, defaultTypeImplementation);
		isValid = false;
	    }
	}

	String propExternalReference_targetProperty = config.parameterAsString(
		OWLISO19150.PARAM_PROP_EXTERNAL_REFERENCE_TARGET_PROPERTY, "rdfs:seeAlso", false, true);
	if (!isValidQName(propExternalReference_targetProperty, config)) {
	    result.addError(this, 101, OWLISO19150.PARAM_PROP_EXTERNAL_REFERENCE_TARGET_PROPERTY,
		    propExternalReference_targetProperty);
	    isValid = false;
	}

	// ===== DescriptorTarget =====

	Pattern templatePattern = Pattern.compile("\\[\\[(.+?)\\]\\]");

	for (DescriptorTarget dt : config.getDescriptorTargets()) {

	    /* Check template */
	    Matcher matcher = templatePattern.matcher(dt.getTemplate());
	    while (matcher.find()) {
		String desc = matcher.group(1).trim();
		/*
		 * Check that if template starts with TV, it matches the regular expression
		 */
		if (desc.startsWith("TV")) {
		    Matcher m = DescriptorAndTagResolver.taggedValuePattern.matcher(desc);
		    if (!m.matches()) {
			result.addError(this, 110, desc, dt.getTarget());
			isValid = false;
		    }
		}
	    }

	    /*
	     * check that 'target' is a QName with prefix matching one of the namespaces
	     * declared in the configuration
	     */
	    String target = dt.getTarget();
	    if (!isValidQName(target, config)) {
		result.addError(this, 102, target);
		isValid = false;
	    }
	}

	// ===== RdfTypeMapEntry =====

	final Map<String, Set<String>> existingSchemaNameByTypeName = new HashMap<String, Set<String>>();
	final SortedMap<String, SortedSet<String>> duplicateSchemaNameByTypeName = new TreeMap<String, SortedSet<String>>();

	for (RdfTypeMapEntry me : config.getRdfTypeMapEntries().values()) {

	    /*
	     * check that 'target' is a QName with prefix matching one of the namespaces
	     * declared in the configuration
	     */
	    String target = me.getTarget();
	    if (!isValidQName(target, config)) {
		result.addError(this, 103, me.getType(), StringUtils.defaultIfBlank(me.getSchema(), ""), target);
		isValid = false;
	    }

	    /*
	     * Gather information to check that type mappings do not have the same
	     * combination of type and schema
	     */
	    gatherInfoOnDuplicateSchema(me.getType(), me.getSchema(), existingSchemaNameByTypeName,
		    duplicateSchemaNameByTypeName);
	}

	/*
	 * Evaluate information to check that type mappings do not have the same
	 * combination of type and schema
	 */
	for (String type : duplicateSchemaNameByTypeName.keySet()) {
	    for (String schema : duplicateSchemaNameByTypeName.get(type)) {
		result.addError(this, 111, type, schema);
		isValid = false;
	    }
	}

	// ===== RdfPropertyMapEntry =====

	final Map<String, Set<String>> existingSchemaNameByPropertyName = new HashMap<String, Set<String>>();
	final SortedMap<String, SortedSet<String>> duplicateSchemaNameByPropertyName = new TreeMap<String, SortedSet<String>>();

	for (RdfPropertyMapEntry me : config.getRdfPropertyMapEntries().values()) {

	    /*
	     * Check that 'property' has non-empty content on both sides of "::" if it has
	     * that separator.
	     */
	    if (!isValidPropertyReferenceWithOptionalClassName(me.getProperty())) {
		result.addError(this, 112, me.getProperty(), me.hasSchema() ? me.getSchema() : "");
		isValid = false;
	    }

	    // check @target
	    String target = me.getTarget();
	    if (!isValidQName(target, config)) {
		result.addError(this, 104, me.getProperty(), StringUtils.defaultIfBlank(me.getSchema(), ""), target);
		isValid = false;
	    }

	    // check @range, if set
	    if (me.hasRange()) {
		String range = me.getRange();
		if (!isValidQName(range, config)) {
		    result.addError(this, 105, me.getProperty(), StringUtils.defaultIfBlank(me.getSchema(), ""), range);
		    isValid = false;
		}
	    }

	    /*
	     * Gather information to check that property mappings do not have the same
	     * combination of type and schema
	     */
	    gatherInfoOnDuplicateSchema(me.getProperty(), me.getSchema(), existingSchemaNameByPropertyName,
		    duplicateSchemaNameByPropertyName);
	}

	/*
	 * Evaluate information to check that property mappings do not have the same
	 * combination of property and schema
	 */
	for (String property : duplicateSchemaNameByPropertyName.keySet()) {
	    for (String schema : duplicateSchemaNameByPropertyName.get(property)) {
		result.addError(this, 113, property, schema);
		isValid = false;
	    }
	}

	// ===== StereotypeConversionParameter =====

	for (List<StereotypeConversionParameter> scps : config.getStereotypeConversionParameters().values()) {
	    for (StereotypeConversionParameter scp : scps) {
		// check QNames in @subClassOf
		SortedSet<String> invalidQNames = identifyInvalidQNames(scp.getSubClassOf(), config);
		if (!invalidQNames.isEmpty()) {
		    result.addError(this, 106, scp.getWellknown(), StringUtils.join(invalidQNames, ", "));
		    isValid = false;
		}
	    }
	}

	// ===== TypeConversionParameter =====

	for (TypeConversionParameter tcp : config.getTypeConversionParameters().values()) {
	    // check QNames in @subClassOf
	    SortedSet<String> invalidQNames = identifyInvalidQNames(tcp.getSubClassOf(), config);
	    if (!invalidQNames.isEmpty()) {
		result.addError(this, 107, tcp.getType(), StringUtils.defaultIfBlank(tcp.getSchema(), ""),
			StringUtils.join(invalidQNames, ", "));
		isValid = false;
	    }
	}

	// ===== PropertyConversionParameter =====

	for (PropertyConversionParameter pcp : config.getPropertyConversionParameters().values()) {

	    /*
	     * Check that 'property' has non-empty content on both sides of "::" if it has
	     * that separator.
	     */
	    if (!isValidPropertyReferenceWithOptionalClassName(pcp.getProperty())) {
		result.addError(this, 115, pcp.getProperty(), pcp.hasSchema() ? pcp.getSchema() : "");
		isValid = false;
	    }

	    if (pcp.hasTarget()) {

		/*
		 * Check that 'target' is not empty and does include "::".
		 */
		String target = pcp.getTarget();
		if (target.isEmpty() || !target.contains("::")) {
		    result.addError(this, 116, target, pcp.getProperty(), pcp.hasSchema() ? pcp.getSchema() : "");
		    isValid = false;
		}

		/*
		 * Check that 'targetSchema' is also set.
		 */
		if (!pcp.hasTargetSchema()) {
		    result.addError(this, 116, pcp.getProperty(), pcp.hasSchema() ? pcp.getSchema() : "");
		    isValid = false;
		}
	    }

	    // check QNames in @subPropertyOf, if set
	    if (pcp.hasSubPropertyOf()) {
		SortedSet<String> invalidQNames = identifyInvalidQNames(pcp.getSubPropertyOf(), config);
		if (!invalidQNames.isEmpty()) {
		    result.addError(this, 108, pcp.getProperty(), StringUtils.defaultIfBlank(pcp.getSchema(), ""),
			    StringUtils.join(invalidQNames, ", "));
		    isValid = false;
		}
	    }

	    /*
	     * check that if global is true then the property is a property name scoped to a
	     * class from a specific schema.
	     */
	    if (pcp.isGlobal()
		    && (!pcp.hasSchema() || pcp.getSchema().trim().isEmpty() || !pcp.getProperty().contains("::"))) {
		result.addError(this, 114, pcp.getProperty(), pcp.hasSchema() ? pcp.getSchema() : "");
		isValid = false;
	    }
	}

	// ===== ConstraintMapping =====

	for (ConstraintMapping cm : config.getConstraintMappings().values()) {
	    // check QName of @target (which has a default value)
	    String target = cm.getTarget();
	    if (!isValidQName(target, config)) {
		result.addError(this, 109, cm.getConstraintType().name(), target);
		isValid = false;
	    }
	}

	// ===== General properties =====
	for (RdfGeneralProperty gp : config.getGeneralProperties()) {

	    /*
	     * We cannot check the @namespaceAbbreviation here because it may also be one
	     * defined for the schemas that shall be processed.
	     */

	    if (gp.hasDomain() && !isValidQName(gp.getDomain(), config)) {
		result.addError(this, 118, gp.getName(), gp.getDomain());
		isValid = false;
	    }

	    if (gp.hasRange() && !isValidQName(gp.getRange(), config)) {
		result.addError(this, 119, gp.getName(), gp.getRange());
		isValid = false;
	    }

	    isValid = isValid
		    && checkPropertyIdentifiers(result, gp.getName(), gp.getEquivalentProperty(), "equivalentProperty");

	    isValid = isValid
		    && checkPropertyIdentifiers(result, gp.getName(), gp.getDisjointProperty(), "disjointProperty");

	    isValid = isValid && checkPropertyIdentifiers(result, gp.getName(), gp.getSubPropertyOf(), "subPropertyOf");

	    if (gp instanceof GeneralObjectProperty) {
		GeneralObjectProperty gop = (GeneralObjectProperty) gp;
		isValid = isValid
			&& checkPropertyIdentifiers(result, gop.getName(), gop.getInverseProperty(), "inverseProperty");
	    }

	    for (String apQName : gp.getAdditionalProperties().keySet()) {

		if (!isValidQNameSyntax(apQName)) {
		    result.addError(this, 121, gp.getName(), apQName);
		    isValid = false;
		}
	    }
	}

	return isValid;
    }

    private boolean isValidQNameSyntax(String qname) {

	String[] qnameParts = qname.split(":");

	if (qnameParts.length != 2) {

	    return false;

	} else {

	    String prefix = qnameParts[0];
	    String resourceName = qnameParts[1];

	    if (StringUtils.isBlank(prefix) || StringUtils.isBlank(resourceName)) {
		return false;
	    } else {
		return true;
	    }
	}
    }

    private boolean checkPropertyIdentifiers(ShapeChangeResult result, String name,
	    SortedSet<String> propertyIdentifiers, String sourceElementName) {

	boolean isValid = true;
	for (String propertyIdentifier : propertyIdentifiers) {

	    if (propertyIdentifier.contains("::")) {
		if (!isValidPropertyFullName(propertyIdentifier)) {
		    result.addError(this, 120, name, sourceElementName, propertyIdentifier);
		    isValid = false;
		}
	    } else if (propertyIdentifier.contains(":")) {
		/*
		 * we can only check the QName syntax here, not that the prefix matches a
		 * namespace, because the target also looks up namespaces in the set of created
		 * ontologies
		 */
		if (!isValidQNameSyntax(propertyIdentifier)) {
		    result.addError(this, 122, name, sourceElementName, propertyIdentifier);
		    isValid = false;
		}
	    } else {
		result.addError(this, 123, name, sourceElementName, propertyIdentifier);
		isValid = false;
	    }

	}

	return isValid;
    }

    private boolean isValidPropertyReferenceWithOptionalClassName(String propertyReference) {

	if (propertyReference.contains("::")) {
	    String[] parts = propertyReference.split("::");
	    if (parts.length != 2 || parts[0].trim().length() == 0 || parts[1].trim().length() == 0) {
		return false;
	    }
	}

	return true;
    }

    private boolean isValidPropertyFullName(String propertyIdentifier) {

	final int countSeparators = StringUtils.countMatches(propertyIdentifier, "::");
	final String[] parts = propertyIdentifier.split("::");

	if (countSeparators < 2 || parts.length != (countSeparators + 1)
		|| Arrays.stream(parts).anyMatch(part -> StringUtils.isBlank(part))) {
	    return false;
	}

	return true;

    }

    /**
     * @param relevantName                      tbd
     * @param schema                            can be <code>null</code>
     * @param existingSchemaNameByRelevantName  tbd
     * @param duplicateSchemaNameByRelevantName tbd
     */
    private void gatherInfoOnDuplicateSchema(String relevantName, String schemaIn,
	    Map<String, Set<String>> existingSchemaNameByRelevantName,
	    SortedMap<String, SortedSet<String>> duplicateSchemaNameByRelevantName) {

	String schema = StringUtils.isNotBlank(schemaIn) ? schemaIn : "<all>";

	if (existingSchemaNameByRelevantName.containsKey(relevantName)) {

	    Set<String> existingSchemaNames = existingSchemaNameByRelevantName.get(relevantName);

	    if (existingSchemaNames.contains(schema)) {

		// so we have a duplicate; log it

		if (duplicateSchemaNameByRelevantName.containsKey(relevantName)) {
		    existingSchemaNameByRelevantName.get(relevantName).add(schema);
		} else {
		    SortedSet<String> duplicateSchemaNames = new TreeSet<String>();
		    duplicateSchemaNames.add(schema);
		    existingSchemaNameByRelevantName.put(relevantName, duplicateSchemaNames);
		}
	    } else {
		existingSchemaNames.add(schema);
	    }

	} else {

	    Set<String> existingSchemaNames = new HashSet<String>();
	    existingSchemaNames.add(schema);
	    existingSchemaNameByRelevantName.put(relevantName, existingSchemaNames);
	}
    }

    private SortedSet<String> identifyInvalidQNames(Set<String> qnames, TargetOwlConfiguration config) {
	SortedSet<String> invalidQNames = new TreeSet<>();
	for (String qname : qnames) {
	    if (!isValidQName(qname, config)) {
		invalidQNames.add(qname);
	    }
	}
	return invalidQNames;
    }

    private boolean isValidQName(String qname, TargetOwlConfiguration config) {

	String[] qnameParts = qname.split(":");

	if (qnameParts.length != 2) {

	    return false;

	} else {

	    String prefix = qnameParts[0];
	    String resourceName = qnameParts[1];

	    if (StringUtils.isBlank(prefix) || StringUtils.isBlank(resourceName)
		    || !config.hasNamespaceWithAbbreviation(prefix)) {
		return false;
	    } else {
		return true;
	    }
	}
    }

    @Override
    public String message(int mnr) {

	switch (mnr) {
	case 0:
	    return "Context: OWLISO19150 target configuration element with 'inputs'='$1$'.";

	case 100:
	    return "Configuration parameter '" + OWLISO19150.PARAM_GENERAL_PROPERTY_NSABR
		    + "' is set, with value '$1$'. However, no namespace is configured with that abbreviation.";
	case 101:
	    return "Configuration parameter '$1$' is set (maybe using the default), with invalid value '$2$'. The value must be a QName of the form '{prefix}:{name}', where {prefix} matches the abbreviation of one of the namespaces defined in the target configuration.";
	case 102:
	    return "??Found DescriptorTarget with invalid @target value '$1$'. The value must be a QName of the form '{prefix}:{name}', where {prefix} matches the abbreviation of one of the namespaces defined in the target configuration.";
	case 103:
	    return "??RdfTypeMapEntry with @type '$1$' and @schema '$2$' has invalid @target value '$3$'. The value must be a QName of the form '{prefix}:{name}', where {prefix} matches the abbreviation of one of the namespaces defined in the target configuration.";
	case 104:
	    return "??RdfPropertyMapEntry with @property '$1$' and @schema '$2$' has invalid @target value '$3$'. The value must be a QName of the form '{prefix}:{name}', where {prefix} matches the abbreviation of one of the namespaces defined in the target configuration.";
	case 105:
	    return "??RdfPropertyMapEntry with @property '$1$' and @schema '$2$' has invalid @range value '$3$'. The value must be a QName of the form '{prefix}:{name}', where {prefix} matches the abbreviation of one of the namespaces defined in the target configuration.";
	case 106:
	    return "??StereotypeConversionParameter with @wellknown '$1$' has invalid QName(s) in its @subClassOf, namely: '$2$'. Each value in @subClassOf must be a QName of the form '{prefix}:{name}', where {prefix} matches the abbreviation of one of the namespaces defined in the target configuration.";
	case 107:
	    return "??TypeConversionParameter with @type '$1$' and @schema '$2$' has invalid QName(s) in its @subClassOf, namely: '$3$'. Each value in @subClassOf must be a QName of the form '{prefix}:{name}', where {prefix} matches the abbreviation of one of the namespaces defined in the target configuration.";
	case 108:
	    return "??PropertyConversionParameter with @property '$1$' and @schema '$2$' has invalid QName(s) in its @subPropertyOf, namely: '$3$'. Each value in @subPropertyOf must be a QName of the form '{prefix}:{name}', where {prefix} matches the abbreviation of one of the namespaces defined in the target configuration.";
	case 109:
	    return "??ConstraintMapping with @constraintType '$1$' has invalid (maybe using the default) @target value '$2$'. The value must be a QName of the form '{prefix}:{name}', where {prefix} matches the abbreviation of one of the namespaces defined in the target configuration.";
	case 110:
	    return "Value of field [[$1$]] in @template of DescriptorTarget configuration element for 'target' $2$ does not match regular expression TV(\\(.+?\\))?:(.+)";
	case 111:
	    return "Multiple RdfTypeMapEntry elements with @type '$1$' and @schema '$2$' encountered. The combination of type and schema must be unique.";
	case 112:
	    return "Value '$1$' of @property in the RdfPropertyMapEntry configuration element (for @property '$1$' and @schema '$2$') is not well-formed.";
	case 113:
	    return "Multiple RdfPropertyMapEntry elements with @property '$1$' and @schema '$2$' encountered. The combination of property and schema must be unique.";
	case 114:
	    return "The PropertyConversionParameter (for @property '$1$' and @schema '$2$')' with 'global' set to true is not well-formed. The 'property' must be scoped to a class and a schema must be given so that the parameter identifies a single property from the model.";
	case 115:
	    return "Value '$1$' of @property in the PropertyConversionParameter configuration element (for @property '$1$' and @schema '$2$') is not well-formed.";
	case 116:
	    return "Value '$1$' of @target in the PropertyConversionParameter configuration element (for @property '$2$' and @schema '$3$'"
		    + ") is not well-formed. It must not be empty and must include '::' (because it shall identify a global property, scoped to a specific class, and from a specific schema).";
	case 117:
	    return "The PropertyConversionParameter configuration element (for @property '$1$'"
		    + " and schema '$2$') has a value for @target but does not have a value for @targetSchema - both must be set or none of them.";
	case 118:
	    return "??General property configuration element with name '$1$' has invalid domain value '$2$'. The value must be a QName of the form '{prefix}:{name}', where {prefix} matches the abbreviation of one of the namespaces defined in the target configuration.";
	case 119:
	    return "??General property configuration element with name '$1$' has invalid range value '$2$'. The value must be a QName of the form '{prefix}:{name}', where {prefix} matches the abbreviation of one of the namespaces defined in the target configuration.";
	case 120:
	    return "??General property configuration element with name '$1$' has invalid $2$ value '$3$'. The value contains '::' and therefore denotes the full name of a UML property (i.e. the package qualified name of the UML property, starting with the application schema package, and using '::' as separator). The full name is malformed (either it has less than three parts [example: A::B], or at least one of the parts does not represent a name [examples: A::B::::C, A::B::]).";
	case 121:
	    return "??General property configuration element with name '$1$' has additionalProperty with invalid property identifier value '$2$'. The value must be a QName of the form '{prefix}:{name}'.";
	case 122:
	    return "??General property configuration element with name '$1$' has invalid $2$ value '$3$'. The value contains ':' (and not '::') and therefore denotes a QName, but the value is not of the required form '{prefix}:{name}'.";
	case 123:
	    return "??General property configuration element with name '$1$' has invalid $2$ value '$3$'. The value neither contains ':' nor '::' and is therefore not of the required format (either a QName or the full name of a UML property).";

	default:
	    return "(" + OWLISO19150ConfigurationValidator.class.getName() + ") Unknown message with number: " + mnr;
	}
    }
}
