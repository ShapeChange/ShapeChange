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
 * (c) 2002-2023 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core.transformation.taggedvalues;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.logging.log4j.util.Strings;

import com.google.common.base.Joiner;

import de.interactive_instruments.shapechange.core.MessageSource;
import de.interactive_instruments.shapechange.core.Options;
import de.interactive_instruments.shapechange.core.ProcessRuleSet;
import de.interactive_instruments.shapechange.core.ShapeChangeAbortException;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.ShapeChangeResult.MessageContext;
import de.interactive_instruments.shapechange.core.TransformerConfiguration;
import de.interactive_instruments.shapechange.core.Type;
import de.interactive_instruments.shapechange.core.model.AssociationInfo;
import de.interactive_instruments.shapechange.core.model.ClassInfo;
import de.interactive_instruments.shapechange.core.model.PackageInfo;
import de.interactive_instruments.shapechange.core.model.TaggedValues;
import de.interactive_instruments.shapechange.core.model.generic.GenericClassInfo;
import de.interactive_instruments.shapechange.core.model.generic.GenericModel;
import de.interactive_instruments.shapechange.core.model.generic.GenericPropertyInfo;
import de.interactive_instruments.shapechange.core.transformation.Transformer;
import shadow.org.apache.commons.lang3.StringUtils;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class TaggedValueTransformer implements Transformer, MessageSource {

    private GenericModel genModel = null;
    private Options options = null;
    private ShapeChangeResult result = null;

    @Override
    public void process(GenericModel genModel, Options options, TransformerConfiguration trfConfig,
	    ShapeChangeResult result) throws ShapeChangeAbortException {

	this.genModel = genModel;
	this.options = options;
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

	if (rules.contains(TaggedValueTransformerConstants.RULE_TV_INHERITANCE)) {
	    result.addProcessFlowInfo(null, 20103, TaggedValueTransformerConstants.RULE_TV_INHERITANCE);
	    applyRuleTaggedValueInheritance(trfConfig);
	}

	if (rules.contains(TaggedValueTransformerConstants.RULE_TV_COPY_FROM_VALUE_TYPE)) {
	    result.addProcessFlowInfo(null, 20103, TaggedValueTransformerConstants.RULE_TV_COPY_FROM_VALUE_TYPE);
	    applyRuleTaggedValueCopyFromValueType(trfConfig);
	}

	if (rules.contains(TaggedValueTransformerConstants.RULE_TV_CREATE_ORIGINAL_SCHEMA_INFO_TAGS)) {
	    result.addProcessFlowInfo(null, 20103,
		    TaggedValueTransformerConstants.RULE_TV_CREATE_ORIGINAL_SCHEMA_INFO_TAGS);
	    applyRuleCreateOriginalSchemaInformationTags(trfConfig);
	}

	if (rules.contains(TaggedValueTransformerConstants.RULE_TV_CREATE_PROPERTY_VALUE_TYPE_INFO_TAG)) {
	    result.addProcessFlowInfo(null, 20103,
		    TaggedValueTransformerConstants.RULE_TV_CREATE_PROPERTY_VALUE_TYPE_INFO_TAG);
	    applyRuleCreatePropertyValueTypeInformationTag(trfConfig);
	}

	if (rules.contains(TaggedValueTransformerConstants.RULE_TV_CREATE_REVERSE_PROPERTY_NAME_TAG)) {
	    result.addProcessFlowInfo(null, 20103,
		    TaggedValueTransformerConstants.RULE_TV_CREATE_REVERSE_PROPERTY_NAME_TAG);
	    applyRuleCreateReversePropertyNameTag(trfConfig);
	}

	if (rules.contains(TaggedValueTransformerConstants.RULE_TV_CREATE_ROLE_SOURE_OR_TARGET_TAG)) {
	    result.addProcessFlowInfo(null, 20103,
		    TaggedValueTransformerConstants.RULE_TV_CREATE_ROLE_SOURE_OR_TARGET_TAG);
	    applyRuleCreateAssociationRoleSourceOrNameTag(trfConfig);
	}

	if (rules.contains(TaggedValueTransformerConstants.RULE_TV_CREATE_CLASSIFIER_NAMESPACE_TAGS)) {
	    result.addProcessFlowInfo(null, 20103,
		    TaggedValueTransformerConstants.RULE_TV_CREATE_CLASSIFIER_NAMESPACE_TAGS);
	    applyRuleCreateClassifierNamespaceTags(trfConfig);
	}

	// apply post-processing (nothing to do right now)
    }

    private void applyRuleCreateReversePropertyNameTag(TransformerConfiguration trfConfig) {

	for (GenericPropertyInfo genPi : genModel.selectedSchemaProperties()) {

	    if (!genPi.isAttribute() && genPi.association().isBiDirectional() && genPi.reverseProperty() != null) {
		genPi.setTaggedValue("reversePropertyName", genPi.reverseProperty().name(), false);
	    }
	}
    }

    private void applyRuleCreateAssociationRoleSourceOrNameTag(TransformerConfiguration trfConfig) {

	for (GenericPropertyInfo genPi : genModel.selectedSchemaProperties()) {

	    if (!genPi.isAttribute() && genPi.association().isBiDirectional()) {
		AssociationInfo ai = genPi.association();
		((GenericPropertyInfo) ai.end1()).setTaggedValue("sourceOrTarget", "source", false);
		((GenericPropertyInfo) ai.end2()).setTaggedValue("sourceOrTarget", "target", false);
	    }
	}
    }

    private void applyRuleCreatePropertyValueTypeInformationTag(TransformerConfiguration trfConfig) {

	String propertyValueTypeInfoTagName = trfConfig.parameterAsString(
		TaggedValueTransformerConstants.PARAM_CREATEPROPERTYVALUETYPEINFO_TAGNAME,
		TaggedValueTransformerConstants.PARAM_CREATEPROPERTYVALUETYPEINFO_TAGNAME_DEFAULT, false, true);

	for (GenericPropertyInfo genPi : genModel.selectedSchemaProperties()) {

	    SortedSet<String> valueTypeNames = new TreeSet<>();

	    ClassInfo typeCi = genPi.typeClass();

	    if (typeCi == null) {

		valueTypeNames.add(StringUtils.defaultIfBlank(genPi.typeInfo().name, ""));

	    } else {

		if (genModel.isInSelectedSchemas(typeCi)) {

		    if (typeCi.subtypes().isEmpty()) {
			valueTypeNames.add(StringUtils.defaultIfBlank(genPi.typeInfo().name, ""));
		    } else {
			if (!typeCi.isAbstract()) {
			    valueTypeNames.add(typeCi.name());
			}
			for (ClassInfo subtype : typeCi.subtypesInCompleteHierarchy()) {
			    if (!subtype.isAbstract()) {
				valueTypeNames.add(subtype.name());
			    }
			}
		    }

		} else {
		    valueTypeNames.add(StringUtils.defaultIfBlank(genPi.typeInfo().name, ""));
		}
	    }

	    String tv = valueTypeNames.isEmpty() ? "" : Strings.join(valueTypeNames, ',');

	    genPi.setTaggedValue(propertyValueTypeInfoTagName, tv, false);
	}

    }

    private void applyRuleCreateOriginalSchemaInformationTags(TransformerConfiguration trfConfig) {

	for (GenericClassInfo genCi : genModel.selectedSchemaClasses()) {

	    PackageInfo schemaPi = genModel.schemaPackage(genCi);
	    genCi.setTaggedValue(TaggedValueTransformerConstants.TV_ORIG_SCHEMA_NAME,
		    schemaPi == null ? "" : schemaPi.name(), false);
	    genCi.setTaggedValue(TaggedValueTransformerConstants.TV_ORIG_CLASS_NAME, genCi.name(), false);
	}

	for (GenericPropertyInfo genPi : genModel.selectedSchemaProperties()) {

	    PackageInfo schemaPi = genModel.schemaPackage(genPi.inClass());
	    genPi.setTaggedValue(TaggedValueTransformerConstants.TV_ORIG_SCHEMA_NAME,
		    schemaPi == null ? "" : schemaPi.name(), false);
	    genPi.setTaggedValue(TaggedValueTransformerConstants.TV_ORIG_INCLASS_NAME, genPi.inClass().name(), false);
	    genPi.setTaggedValue(TaggedValueTransformerConstants.TV_ORIG_PROPERTY_NAME, genPi.name(), false);
	    genPi.setTaggedValue(TaggedValueTransformerConstants.TV_ORIG_PROPERTY_MULTIPLICITY,
		    genPi.cardinality().toString(), false);
	    genPi.setTaggedValue(TaggedValueTransformerConstants.TV_ORIG_PROPERTY_VALUETYPE, genPi.typeInfo().name,
		    false);
	}
    }

    private void applyRuleCreateClassifierNamespaceTags(TransformerConfiguration trfConfig) {

	for (GenericClassInfo genCi : genModel.selectedSchemaClasses()) {

	    PackageInfo schemaPi = genModel.schemaPackage(genCi);
	    genCi.setTaggedValue("namespace", StringUtils.defaultIfBlank(schemaPi.targetNamespace(), ""), false);
	    genCi.setTaggedValue(TaggedValueTransformerConstants.TV_ORIG_CLASS_NAME, genCi.name(), false);
	}
    }

    private void applyRuleTaggedValueCopyFromValueType(TransformerConfiguration trfConfig) {

	List<String> tvsToCopyAsList = trfConfig.parameterAsStringList(
		TaggedValueTransformerConstants.PARAM_TV_COPYFROMVALUETYPE_TVSTOCOPY, null, true, true);

	if (tvsToCopyAsList.isEmpty()) {
	    result.addError(this, 200);
	    return;
	}

	Joiner joiner = Joiner.on(",");
	String tvsToCopy = joiner.join(tvsToCopyAsList);

	String typeNameRegexParamValue = trfConfig.parameterAsString(
		TaggedValueTransformerConstants.PARAM_TV_COPYFROMVALUETYPE_TYPENAMEREGEX, ".*", false, true);
	Pattern typeNameRegex = null;
	try {
	    typeNameRegex = Pattern.compile(typeNameRegexParamValue);
	} catch (PatternSyntaxException e) {
	    result.addError(this, 10, typeNameRegexParamValue,
		    TaggedValueTransformerConstants.PARAM_TV_COPYFROMVALUETYPE_TYPENAMEREGEX, e.getMessage(),
		    TaggedValueTransformerConstants.RULE_TV_COPY_FROM_VALUE_TYPE);
	    return;
	}

	for (GenericPropertyInfo genPi : genModel.selectedSchemaProperties()) {

	    Type ti = genPi.typeInfo();

	    if (ti.name != null && typeNameRegex.matcher(ti.name).matches()) {

		ClassInfo valueType = genModel.classByIdOrName(ti);

		if (valueType != null) {

		    TaggedValues valueTypeTVs = valueType.taggedValuesForTagList(tvsToCopy);

		    TaggedValues genPiTVsCopy = genPi.taggedValuesAll();
		    genPiTVsCopy.putAll(valueTypeTVs);
		    genPi.setTaggedValues(genPiTVsCopy, true);
		}
	    }
	}

    }

    private void applyRuleTaggedValueInheritance(TransformerConfiguration trfConfig) {

	List<String> generalIn = trfConfig.parameterAsStringList(
		TaggedValueTransformerConstants.PARAM_TV_INHERITANCE_GENERAL_LIST, null, true, true);

	if (generalIn.isEmpty()) {
	    /*
	     * NOTE: The configuration validator checks that the parameter contains an
	     * actual value. However, since validation of the configuration can be disabled,
	     * we still have this check.
	     */
	    return;
	}

	List<String> overwriteIn = trfConfig.parameterAsStringList(
		TaggedValueTransformerConstants.PARAM_TV_INHERITANCE_OVERWRITE_LIST, null, true, true);
	List<String> appendIn = trfConfig.parameterAsStringList(
		TaggedValueTransformerConstants.PARAM_TV_INHERITANCE_APPEND_LIST, null, true, true);

	String appendSeparator = trfConfig.parameterAsString(
		TaggedValueTransformerConstants.PARAM_TV_INHERITANCE_APPEND_SEPARATOR,
		TaggedValueTransformerConstants.DEFAULT_TV_INHERITANCE_APPEND_SEPARATOR, true, false);

	/*
	 * Normalize tagged values. Ignore overwrite-TVs that are not contained in
	 * general-TVs. Ignore append-TVs that are not contained in general-TVs or
	 * contained in overwrite-TVs.
	 */
	SortedSet<String> generalTVs = new TreeSet<String>();
	SortedSet<String> overwriteTVs = new TreeSet<String>();
	SortedSet<String> appendTVs = new TreeSet<String>();

	/*
	 * NOTE: Normalization of tags is currently restricted to a set of well-known
	 * tags (related to descriptors).
	 */

	for (String tv : generalIn) {
	    generalTVs.add(options.normalizeTag(tv));
	}
	for (String tv : overwriteIn) {
	    String normalizedTV = options.normalizeTag(tv);
	    if (generalTVs.contains(normalizedTV)) {
		overwriteTVs.add(normalizedTV);
	    }
	}
	for (String tv : appendIn) {
	    String normalizedTV = options.normalizeTag(tv);
	    if (generalTVs.contains(normalizedTV) && !overwriteTVs.contains(normalizedTV)) {
		appendTVs.add(normalizedTV);
	    }
	}

	// identify top-level supertypes in model
	SortedSet<GenericClassInfo> topLevelSupertypes = new TreeSet<GenericClassInfo>();

	for (GenericClassInfo genCi : genModel.getGenClasses().values()) {
	    if (!genCi.hasSupertypes() && genCi.hasSubtypes()) {
		topLevelSupertypes.add(genCi);
	    }
	}

	for (GenericClassInfo genCi : topLevelSupertypes) {
	    applyTaggedValueInheritance(genCi, generalTVs, overwriteTVs, appendTVs, appendSeparator);
	}
    }

    private void applyTaggedValueInheritance(GenericClassInfo genCi, SortedSet<String> generalTVs,
	    SortedSet<String> overwriteTVs, SortedSet<String> appendTVs, String appendSeparator) {

	for (String subtypeId : genCi.subtypes()) {

	    GenericClassInfo subtype = (GenericClassInfo) genModel.classById(subtypeId);

	    for (String tv : generalTVs) {

		String tvValue = genCi.taggedValue(tv);

		/*
		 * Here we really compare with null, since we are fine inheriting empty tags
		 */
		if (tvValue != null) {

		    String subtypeTvValue = subtype.taggedValue(tv);

		    if (subtypeTvValue == null) {

			subtype.setTaggedValue(tv, tvValue, false);

			MessageContext mc = result.addInfo(this, 100, tv, tvValue, subtype.name());
			if (mc != null) {
			    mc.addDetail(this, 4, genCi.fullName());
			    mc.addDetail(this, 5, subtype.fullName());
			}

		    } else {

			// determine behavior
			if (overwriteTVs.contains(tv)) {

			    // overwrite TV
			    subtype.setTaggedValue(tv, tvValue, false);

			    MessageContext mc = result.addInfo(this, 101, tv, tvValue, subtype.name(), subtypeTvValue);
			    if (mc != null) {
				mc.addDetail(this, 4, genCi.fullName());
				mc.addDetail(this, 5, subtype.fullName());
			    }

			} else if (appendTVs.contains(tv)) {

			    // append TV
			    String newSubtypeTvValue = subtypeTvValue + appendSeparator + tvValue;

			    subtype.setTaggedValue(tv, newSubtypeTvValue, false);

			    MessageContext mc = result.addInfo(this, 102, tvValue, tv, subtype.name(),
				    newSubtypeTvValue);
			    if (mc != null) {
				mc.addDetail(this, 4, genCi.fullName());
				mc.addDetail(this, 5, subtype.fullName());
			    }

			} else {

			    // retain TV
			    MessageContext mc = result.addInfo(this, 103, tv, subtypeTvValue, subtype.name());
			    if (mc != null) {
				mc.addDetail(this, 4, genCi.fullName());
				mc.addDetail(this, 5, subtype.fullName());
			    }
			}
		    }
		}
	    }

	    // recursively apply to subtype
	    applyTaggedValueInheritance(subtype, generalTVs, overwriteTVs, appendTVs, appendSeparator);
	}
    }

    @Override
    public String message(int mnr) {

	return switch (mnr) {
	case 0 -> "Context: property '$1$'.";
	case 1 -> "Context: class '$1$'.";
	case 2 -> "Context: association class '$1$'.";
	case 3 -> "Context: association between class '$1$' (with property '$2$') and class '$3$' (with property '$4$')";
	case 4 -> "Context: supertype '$1$'";
	case 5 -> "Context: subtype '$1$'";

	case 10 -> "Syntax exception for regular expression '$1$' of parameter '$2$'. Message is: $3$. $4$ will not have any effect.";

	// Messages for RULE_TV_INHERITANCE
	case 100 -> "Adding tagged value $1$=$2$ to $3$.";
	case 101 -> "Overwriting tagged value $1$=$2$ in $3$. Previous value was: $4$";
	case 102 -> "Appending '$1$' to tagged value $2$ in $3$. New value is: $4$.";
	case 103 -> "Retaining tagged value $1$=$2$ in $3$.";

	case 200 -> "Required parameter '" + TaggedValueTransformerConstants.PARAM_TV_COPYFROMVALUETYPE_TVSTOCOPY
		    + "' was not set or does not contain any value. '"
		    + TaggedValueTransformerConstants.RULE_TV_COPY_FROM_VALUE_TYPE + "' will not have any effect.";

	default -> "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	};
    }
}
