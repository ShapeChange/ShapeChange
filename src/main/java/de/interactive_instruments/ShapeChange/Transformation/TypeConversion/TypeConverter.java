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
 * (c) 2002-2020 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Transformation.TypeConversion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.Multiplicity;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ProcessRuleSet;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.ShapeChangeResult.MessageContext;
import de.interactive_instruments.ShapeChange.TransformerConfiguration;
import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Model.Stereotypes;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericAssociationInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericClassInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel.PropertyCopyDuplicatBehaviorIndicator;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericPropertyInfo;
import de.interactive_instruments.ShapeChange.Transformation.Transformer;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class TypeConverter implements Transformer, MessageSource {

    public static final String TV_DISSOLVE_ASSOCIATION = "dissolveAssociation";
    public static final String TV_TO_CODELIST = "toCodelist";
    public static final String TV_DISSOLVE_ASSOCIATION_ATTRIBUTE_TYPE = "dissolveAssociationAttributeType";
    public static final String TV_DISSOLVE_ASSOCIATION_IORBR = "dissolveAssociationInlineOrByReference";

    /**
     * Define a suffix to be added to the names of attributes that have been
     * transformed from association roles. Applies to
     * {@value #RULE_DISSOLVE_ASSOCIATIONS}. Default value is the empty string.
     */
    public static final String PARAM_DISSOLVE_ASSOCIATIONS_ATTRIBUTE_NAME_SUFFIX = "attributeNameSuffix";

    /**
     * Define the type to set for attributes that have been transformed from
     * association roles. Applies to {@value #RULE_DISSOLVE_ASSOCIATIONS}. Default
     * value is {@value #DEFAULT_DISSOLVE_ASSOCIATIONS_ATTRIBUTE_TYPE}.
     */
    public static final String PARAM_DISSOLVE_ASSOCIATIONS_ATTRIBUTE_TYPE = "attributeType";
    public static final String DEFAULT_DISSOLVE_ASSOCIATIONS_ATTRIBUTE_TYPE = "CharacterString";

    /**
     * Regular expression to identify types from the schemas selected for processing
     * to convert to feature types with {@value #RULE_TO_FEATURETYPE} .
     * Identification is based on a match on the name of the type. There is no
     * default value.
     */
    public static final String PARAM_TO_FEATURE_TYPE_NAME_REGEX = "toFeatureTypeNameRegex";

    /**
     * Regular expression to identify enumerations (in the whole model) to NOT
     * convert to code lists under {@value #RULE_ENUMERATION_TO_CODELIST} .
     * Identification is based on a match on the name of the type. There is no
     * default value.
     */
    public static final String PARAM_ENUMERATION_TO_CODELIST_EXCLUSION_REGEX = "toCodelistExclusionRegex";

    /**
     * Identify the name of the tagged value that is used to tag types that shall be
     * converted to feature types under {@value #RULE_TO_FEATURETYPE}. Default is
     * 'toFeatureType'.
     */
    public static final String PARAM_TO_FEATURE_TYPE_TAGGED_VALUE_NAME = "toFeatureTypeTaggedValueName";

    /**
     * Converts enumerations to code lists. For each converted enumeration, tagged
     * value 'asDictionary' is set to 'false'.
     * <p>
     * Enumerations can be excluded from this conversion if their name matches the
     * regular expression given via parameter
     * {@value #PARAM_ENUMERATION_TO_CODELIST_EXCLUSION_REGEX} or if they have
     * tagged value {@value #TV_TO_CODELIST}='false'.
     * <p>
     * NOTE: This rule converts all enumerations within the model to code lists. If
     * a restriction is desired, for example to enumerations that belong to the
     * schemas selected for processing, or to specifically identified application
     * schemas (e.g. identified by name or target namespace regex), the
     * implementation would need to be enhanced (for example through new
     * parameters).
     * <p>
     * Constraints are not updated.
     */
    public static final String RULE_ENUMERATION_TO_CODELIST = "rule-trf-enumeration-to-codelist";

    /**
     * Convert types either identified via parameter
     * {@value #PARAM_TO_FEATURE_TYPE_NAME_REGEX} or with tagged value
     * 'toFeatureType=true' to feature types. The name of the tagged value can be
     * configured via parameter {@value #PARAM_TO_FEATURE_TYPE_TAGGED_VALUE_NAME}.
     * All subtypes of these types are also converted to feature types.
     */
    public static final String RULE_TO_FEATURETYPE = "rule-trf-toFeatureType";

    /**
     * Convert all object types from schemas selected for processing to feature
     * types. All subtypes of these types are also converted to feature types.
     */
    public static final String RULE_OBJECTTYPES_TO_FEATURETYPES = "rule-trf-objectTypesToFeatureTypes";

    /**
     * Dissolves associations that are navigable from types in the schemas selected
     * for processing. Navigable roles of such associations are transformed into
     * attributes. A suffix can be added to the names of these attributes, via
     * parameter {@value #PARAM_DISSOLVE_ASSOCIATIONS_ATTRIBUTE_NAME_SUFFIX}. The
     * type of these attributes is set to the type defined by parameter
     * {@value #PARAM_DISSOLVE_ASSOCIATIONS_ATTRIBUTE_TYPE}.
     * <p>
     * Associations can be excluded from this transformation by:
     * <ul>
     * <li>setting tagged value {@value #TV_DISSOLVE_ASSOCIATION} on the association
     * with value 'false'</li>
     * <li>including
     * {@value #RULE_DISSOLVE_ASSOCIATIONS_EXCLUDE_MANYTOMANY_RELATIONSHIPS} in the
     * encoding rule</li>
     * </ul>
     * If an association that is dissolves has an association class, a warning is
     * logged and the class is removed.
     */
    public static final String RULE_DISSOLVE_ASSOCIATIONS = "rule-trf-dissolveAssociations";

    /**
     * If this rule is included together with {@value #RULE_DISSOLVE_ASSOCIATIONS},
     * an association is not dissolved if it represents a many-to-many relationship
     * between two types (i.e., all navigable roles have max multiplicity greater
     * than 1).
     */
    public static final String RULE_DISSOLVE_ASSOCIATIONS_EXCLUDE_MANYTOMANY_RELATIONSHIPS = "rule-trf-dissolveAssociations-excludeManyToManyRelationships";

    /**
     * If this rule is included together with {@value #RULE_DISSOLVE_ASSOCIATIONS},
     * then attributes that result from dissolving an association and have maximum
     * multiplicity greater than 1 are removed.
     */
    public static final String RULE_DISSOLVE_ASSOCIATIONS_REMOVE_TRANSFORMED_ROLE_IF_MULTIPLE = "rule-trf-dissolveAssociations-removeTransformedAttributeIfMultiple";

    /**
     * If this rule is included, then the type of resulting attributes is kept
     * as-is, i.e. parameter {@value #PARAM_DISSOLVE_ASSOCIATIONS_ATTRIBUTE_TYPE}
     * will have no effect.
     */
    public static final String RULE_DISSOLVE_ASSOCIATIONS_KEEP_TYPE = "rule-trf-dissolveAssociations-keepType";

    /**
     * Convert the &lt;&lt;propertyMetadata&gt;&gt; stereotype to an additional
     * property, as follows: First, identify the metadata type that applies to the
     * property with that stereotype. Consult the tagged value 'metadataType' of the
     * property (see {@link PropertyInfo#propertyMetadataType()}). If no metadata
     * type could be identified, use the type defined by configuration parameter
     * {@value #PARAM_METADATA_TYPE}. If that also failed, log an error message and
     * remove the stereotype from the property. Otherwise, if the metadata type is a
     * type with identity (feature or object type) then create a directed
     * association to the metadata type - else create an attribute with the metadata
     * type as value type. The name of the new association role or attribute is the
     * property name plus suffix defined by configuration parameter
     * {@value #PARAM_METADATA_PROPERTY_NAME_SUFFIX}. If a new association role was
     * created, set tagged value inlineOrByReference to the value defined by
     * configuration parameter
     * {@value #PARAM_METADATA_PROPERTY_INLINEORBYREFERENCE}.
     */
    public static final String RULE_PROPERTYMETADATA_STEREOTYPE_TO_PROPERTY = "rule-trf-propertyMetadata-stereotype-to-metadata-property";
    /**
     * Name of the type from the conceptual model, which shall be used as metadata
     * type for all properties with stereotype &lt;&lt;propertyMetadata&gt;&gt; that
     * do not define a metadata type via tagged value 'metadataType'. The value can
     * be the pure type name, if it is unique within the conceptual model.
     * Otherwise, identify the correct type by providing its full name (omitting
     * packages that are outside of the schema the class belongs to). The default
     * value for this parameter is 'MD_Metadata' (which typically refers to the type
     * defined by ISO 19115).
     */
    public static final String PARAM_METADATA_TYPE = "metadataType";
    /**
     * Defines the suffix that shall be added to the name of a new property created
     * by {@value #RULE_PROPERTYMETADATA_STEREOTYPE_TO_PROPERTY}. Default is
     * '_metadata'.
     */
    public static final String PARAM_METADATA_PROPERTY_NAME_SUFFIX = "metadataPropertyNameSuffix";
    /**
     * Defines the value for tag 'inlineOrByReference' of a new association role
     * created by {@value #RULE_PROPERTYMETADATA_STEREOTYPE_TO_PROPERTY}. Default is
     * 'inlineOrByReference'. Other allowed values are 'byReference' and 'inline'.
     */
    public static final String PARAM_METADATA_PROPERTY_INLINEORBYREFERENCE = "metadataPropertyInlineOrByReference";

    private GenericModel genModel = null;
    private Options options = null;
    private TransformerConfiguration trfConfig = null;
    private ShapeChangeResult result = null;

    @Override
    public void process(GenericModel genModel, Options options, TransformerConfiguration trfConfig,
	    ShapeChangeResult result)

	    throws ShapeChangeAbortException {

	this.genModel = genModel;
	this.options = options;
	this.trfConfig = trfConfig;
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
	 * because there are no mandatory - in other words default - rules for this
	 * transformer simply return the model if no rules are defined in the rule sets
	 * (which the schema allows)
	 */
	if (rules.isEmpty())
	    return;

	// apply pre-processing (nothing to do right now)

	// execute rules

	if (rules.contains(RULE_ENUMERATION_TO_CODELIST)) {
	    applyRuleEnumerationToCodelist(genModel, trfConfig);
	}

	if (rules.contains(RULE_DISSOLVE_ASSOCIATIONS)) {
	    applyRuleDissolveAssociations(genModel, trfConfig);
	}

	if (rules.contains(RULE_TO_FEATURETYPE)) {
	    applyRuleToFeatureType(genModel, trfConfig);
	}

	if (rules.contains(RULE_OBJECTTYPES_TO_FEATURETYPES)) {
	    applyRuleObjectTypesToFeatureTypes();
	}

	if (rules.contains(RULE_PROPERTYMETADATA_STEREOTYPE_TO_PROPERTY)) {
	    applyRulePropertyMetadataStereotypeToProperty();
	}

	// apply post-processing (nothing to do right now)
    }

    private void applyRulePropertyMetadataStereotypeToProperty() {

	String paramMetadataType = trfConfig.parameterAsString(PARAM_METADATA_TYPE, "MD_Metadata", false, true);
	ClassInfo defaultMetadataType = null;

	/*
	 * Does the default metadata type name contain a semicolon? If so, search by
	 * fully qualified name. Otherwise, search by name only.
	 */
	if (paramMetadataType.contains(":")) {
	    defaultMetadataType = genModel.classByFullNameInSchema(paramMetadataType);
	} else {
	    defaultMetadataType = genModel.classByName(paramMetadataType);
	}

	if (defaultMetadataType == null) {
	    result.addWarning(this, 200, paramMetadataType);
	}

	String nameSuffix = trfConfig.parameterAsString(PARAM_METADATA_PROPERTY_NAME_SUFFIX, "_metadata", false, true);
	String inlineOrByReference = trfConfig.parameterAsString(PARAM_METADATA_PROPERTY_INLINEORBYREFERENCE,
		"inlineOrByReference", false, true);

	SortedSet<GenericClassInfo> selCis = genModel.selectedSchemaClasses();

	for (GenericClassInfo genCi : selCis) {
	    
	    /*
	     * create copy of class property collection, to prevent concurrent modification
	     * exception when new properties are added to the class
	     */
	    List<PropertyInfo> classPis = new ArrayList<>(genCi.properties().values());
	    for (PropertyInfo pi : classPis) {

		if (pi.propertyMetadata()) {

		    pi.stereotypes().remove("propertymetadata");

		    ClassInfo mdt = pi.propertyMetadataType();

		    if (mdt == null) {
			mdt = defaultMetadataType;
		    }

		    if (mdt == null) {

			MessageContext mc = result.addError(this, 201, pi.name(), genCi.name());
			if (mc != null) {
			    mc.addDetail(this, 0, pi.fullNameInSchema());
			}

		    } else {

			GenericPropertyInfo mdPi = new GenericPropertyInfo(genModel, pi.id() + "_metadataProperty",
				pi.name() + nameSuffix);
			mdPi.setCardinality(new Multiplicity(0, 1));

			mdPi.setTypeInfo(new Type(mdt.id(), mdt.name()));
			mdPi.setInClass(genCi);

			mdPi.setSequenceNumber(pi.sequenceNumber().createCopyWithSuffix(1), true);
			genCi.addProperty(mdPi, PropertyCopyDuplicatBehaviorIndicator.IGNORE);

			if (mdt.category() == Options.FEATURE || mdt.category() == Options.OBJECT) {

			    mdPi.setAttribute(false);
			    mdPi.setTaggedValue("inlineOrByReference", inlineOrByReference, true);

			    // create other association role
			    String otherRoleIdAndName = pi.id() + "_metadataProperty_otherRole";
			    GenericPropertyInfo otherRole = new GenericPropertyInfo(genModel, otherRoleIdAndName,
				    otherRoleIdAndName);
			    otherRole.setNavigable(false);
			    otherRole.setAttribute(false);			    
			    otherRole.setCardinality(new Multiplicity("0..*"));
			    otherRole.setTypeInfo(new Type(genCi.id(), genCi.name()));
			    otherRole.setInClass(mdt);
			    genModel.register(otherRole);

			    // create directed association
			    GenericAssociationInfo genAi = new GenericAssociationInfo();
			    genAi.setId("association_for_" + mdPi.id());
			    genAi.setEnd1(mdPi);
			    mdPi.setAssociation(genAi);
			    genAi.setEnd2(otherRole);
			    otherRole.setAssociation(genAi);
			    genAi.setModel(genModel);
			    genAi.setOptions(options);
			    genAi.setResult(result);

			    genModel.addAssociation(genAi);

			} else {

			    mdPi.setTaggedValue("inlineOrByReference", "inline", true);
			}
		    }
		}
	    }
	}
    }

    private void applyRuleObjectTypesToFeatureTypes() {

	SortedSet<GenericClassInfo> relevantTypes = new TreeSet<GenericClassInfo>();

	SortedSet<GenericClassInfo> selCis = genModel.selectedSchemaClasses();

	for (GenericClassInfo genCi : selCis) {

	    if (genCi.category() == Options.OBJECT) {

		relevantTypes.add(genCi);

		SortedSet<ClassInfo> allSubtypes = genCi.subtypesInCompleteHierarchy();

		/*
		 * add all subtypes that belong to schemas selected for processing
		 */
		for (ClassInfo subtype : allSubtypes) {

		    if (selCis.contains(subtype)) {
			relevantTypes.add((GenericClassInfo) subtype);
		    }
		}
	    }
	}

	for (GenericClassInfo type : relevantTypes) {

	    /*
	     * overwrite stereotypes cache
	     */
	    Stereotypes stereo = options.stereotypesFactory();
	    stereo.add("featuretype");
	    type.setStereotypes(stereo);

	    type.setCategory(Options.FEATURE);
	}

    }

    private void applyRuleToFeatureType(GenericModel genModel, TransformerConfiguration trfConfig) {

	String typeNameRegexParamValue = trfConfig.parameterAsString(PARAM_TO_FEATURE_TYPE_NAME_REGEX, null, false,
		true);

	String toFeatureTypeTVName = trfConfig.parameterAsString(PARAM_TO_FEATURE_TYPE_TAGGED_VALUE_NAME,
		"toFeatureType", false, true);

	try {

	    Pattern typeNameRegex = null;
	    if (typeNameRegexParamValue != null) {
		typeNameRegex = Pattern.compile(typeNameRegexParamValue);
	    }

	    SortedSet<GenericClassInfo> relevantTypes = new TreeSet<GenericClassInfo>();

	    SortedSet<GenericClassInfo> selCis = genModel.selectedSchemaClasses();

	    for (GenericClassInfo genCi : selCis) {

		if ((typeNameRegex != null && typeNameRegex.matcher(genCi.name()).matches())
			|| "true".equalsIgnoreCase(genCi.taggedValue(toFeatureTypeTVName))) {

		    relevantTypes.add(genCi);

		    SortedSet<ClassInfo> allSubtypes = genCi.subtypesInCompleteHierarchy();

		    /*
		     * add all subtypes that belong to schemas selected for processing
		     */
		    for (ClassInfo subtype : allSubtypes) {

			if (selCis.contains(subtype)) {
			    relevantTypes.add((GenericClassInfo) subtype);
			}
		    }
		}
	    }

	    for (GenericClassInfo type : relevantTypes) {

		/*
		 * overwrite stereotypes cache
		 */
		Stereotypes stereo = options.stereotypesFactory();
		stereo.add("featuretype");
		type.setStereotypes(stereo);

		type.setCategory(Options.FEATURE);
	    }

	} catch (PatternSyntaxException e) {
	    result.addError(this, 10, typeNameRegexParamValue, PARAM_TO_FEATURE_TYPE_NAME_REGEX, e.getMessage(),
		    RULE_TO_FEATURETYPE);
	}
    }

    private void applyRuleDissolveAssociations(GenericModel genModel, TransformerConfiguration trfConfig) {

	String attributeNameSuffix = trfConfig.parameterAsString(PARAM_DISSOLVE_ASSOCIATIONS_ATTRIBUTE_NAME_SUFFIX, "",
		false, true);
	String attributeTypeName = trfConfig.parameterAsString(PARAM_DISSOLVE_ASSOCIATIONS_ATTRIBUTE_TYPE,
		DEFAULT_DISSOLVE_ASSOCIATIONS_ATTRIBUTE_TYPE, false, true);
	Type attributeTypeFromParameter = genModel.typeByName(attributeTypeName);

	boolean keepType = trfConfig.hasRule(RULE_DISSOLVE_ASSOCIATIONS_KEEP_TYPE);

	SortedSet<GenericAssociationInfo> associations = new TreeSet<GenericAssociationInfo>();

	/*
	 * Identify associations that are navigable from types in the schemas selected
	 * for processing.
	 */
	for (GenericPropertyInfo genPi : genModel.selectedSchemaProperties()) {
	    if (!genPi.isAttribute()) {
		associations.add((GenericAssociationInfo) genPi.association());
	    }
	}

	SortedSet<GenericAssociationInfo> associationsToExclude = new TreeSet<GenericAssociationInfo>();

	for (GenericAssociationInfo genAi : associations) {

	    /*
	     * Exclude association if tagged value "dissolveAssociation" on the association
	     * is set to 'false'.
	     */
	    if ("false".equalsIgnoreCase(genAi.taggedValue(TV_DISSOLVE_ASSOCIATION))) {
		associationsToExclude.add(genAi);
	    }

	    /*
	     * Exclude association if it represents a many-to-many relationship and
	     * rule-trf-dissolveAssociations-excludeManyToManyRelationships is enabled.
	     */
	    if (trfConfig.hasRule(RULE_DISSOLVE_ASSOCIATIONS_EXCLUDE_MANYTOMANY_RELATIONSHIPS)
		    && (!genAi.end1().isNavigable() || genAi.end1().cardinality().maxOccurs > 1)
		    && (!genAi.end2().isNavigable() || genAi.end2().cardinality().maxOccurs > 1)) {
		associationsToExclude.add(genAi);
	    }
	}

	associations.removeAll(associationsToExclude);

	// Dissolve remaining associations
	for (GenericAssociationInfo genAi : associations) {

	    /*
	     * If an association that is dissolved has an association class, log a warning.
	     */
	    if (genAi.assocClass() != null) {
		MessageContext mc = result.addWarning(this, 100, genAi.assocClass().name());
		if (mc != null) {
		    mc.addDetail(this, 2, genAi.assocClass().fullNameInSchema());
		}
	    }

	    GenericPropertyInfo end1 = (GenericPropertyInfo) genAi.end1();
	    GenericPropertyInfo end2 = (GenericPropertyInfo) genAi.end2();

	    /*
	     * Dissolve the association: Transform association roles into attributes, remove
	     * possibly existing association class, remove association
	     */
	    genModel.dissolveAssociation(genAi);

	    if (end1.isNavigable()) {
		handleAttributeAfterDissolvingAssociation(end1, attributeTypeFromParameter, attributeNameSuffix,
			keepType);
	    }
	    if (end2.isNavigable()) {
		handleAttributeAfterDissolvingAssociation(end2, attributeTypeFromParameter, attributeNameSuffix,
			keepType);
	    }
	}
    }

    private void handleAttributeAfterDissolvingAssociation(GenericPropertyInfo attribute,
	    Type attributeTypeFromParameter, String attributeNameSuffix, boolean keepType) {

	if (attribute.cardinality().maxOccurs > 1
		&& trfConfig.hasRule(RULE_DISSOLVE_ASSOCIATIONS_REMOVE_TRANSFORMED_ROLE_IF_MULTIPLE)) {

	    genModel.remove(attribute, false);

	} else {

	    // determine type to use
	    Type newAttributeType = attributeTypeFromParameter;

	    String dissolveAssociationAttributeType = attribute.taggedValue(TV_DISSOLVE_ASSOCIATION_ATTRIBUTE_TYPE);
	    if (StringUtils.isNotBlank(dissolveAssociationAttributeType)) {
		newAttributeType = genModel.typeByName(dissolveAssociationAttributeType);
	    }

	    // Add suffix to attribute name and set attribute type
	    attribute.setName(attribute.name() + attributeNameSuffix);
	    if (!keepType) {
		attribute.setTypeInfo(newAttributeType.createCopy());
	    }

	    // Update inlineOrByReference if necessary
	    String newIobr = attribute.taggedValue(TV_DISSOLVE_ASSOCIATION_IORBR);
	    if (StringUtils.isNotBlank(newIobr)) {
		attribute.setTaggedValue("inlineOrByReference", newIobr, true);
	    }
	}
    }

    private void applyRuleEnumerationToCodelist(GenericModel genModel, TransformerConfiguration trfConfig) {

	String exclusionRegexParamValue = trfConfig.parameterAsString(PARAM_ENUMERATION_TO_CODELIST_EXCLUSION_REGEX,
		null, false, true);

	Pattern exclusionRegex = null;
	if (exclusionRegexParamValue != null) {
	    exclusionRegex = Pattern.compile(exclusionRegexParamValue);
	}

	/*
	 * --- update class category ---
	 */
	for (GenericClassInfo genCi : genModel.getGenClasses().values()) {

	    if (genCi.category() == Options.ENUMERATION
		    && (exclusionRegex == null || !exclusionRegex.matcher(genCi.name()).matches())
		    && !"false".equalsIgnoreCase(genCi.taggedValue(TV_TO_CODELIST))) {

		genCi.setCategory(Options.CODELIST);

		Stereotypes st = genCi.stereotypes();
		st.remove("enumeration");
		st.add("codelist");
		// we need to explicitly set the new Stereotypes cache
		genCi.setStereotypes(st);

		genCi.setTaggedValue("asDictionary", "false", false);
	    }
	}
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

	case 10:
	    return "Syntax exception for regular expression '$1$' of parameter '$2$'. Message is: $3$. $4$ will not have any effect.";

	// Messages for RULE_DISSOLVE_ASSOCIATIONS
	case 100:
	    return "Association class '$1$' will be removed.";

	// Messages for RULE_PROPERTYMETADATA_STEREOTYPE_TO_PROPERTY
	case 200:
	    return "Metadata type '$1$' identified by configuration parameter " + PARAM_METADATA_TYPE
		    + " was not found in the model. This is ok if all <<propertyMetadata>> properties have their metadata type correctly defined via tagged value 'metadataType'. Otherwise, i.e. the tagged value is not set correctly on such a property, a new metadata property will not be created for the property.";

	default:
	    return "(" + TypeConverter.class.getName() + ") Unknown message with number: " + mnr;
	}
    }
}
