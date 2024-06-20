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
 * (c) 2002-2012 interactive instruments GmbH, Bonn, Germany
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

package de.interactive_instruments.ShapeChange.Model;

import java.util.List;
import java.util.Map;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;

/**
 * Note: this class (more precisely: InfoImpl, which implements compareTo(..)
 * globally) has a natural ordering that is inconsistent with equals.
 *
 */
public interface Info extends Comparable<Info> {

    public Options options();

    public ShapeChangeResult result();

    public Model model();

    /**
     * @return an identifier of the model element that is globally unique, i.e.
     *         unique across models, or <code>null</code> if such an identifier is
     *         not available
     */
    public String globalIdentifier();

    /**
     * Retrieves available values for all descriptors as defined by the
     * configuration. Values can be given with or without language identifier,
     * depending on what can be retrieved from the specific descriptor source.
     * 
     * @return all descriptor values; can be empty but not <code>null</code>
     */
    public Descriptors descriptors();

    /**
     * Return unique id of model element. IDs of model elements - even if they are
     * of different type (packages, classes, properties, associations, operations) -
     * shall be unique.
     * 
     * @return tbd
     */
    public String id();

    /**
     * Obtain the name of the model element.
     * 
     * @return tbd
     */
    public String name();

    /**
     * Fully qualified UML name (Package1::...::PackageN::InfoName) with scope being
     * the whole model. The package name part may therefore contain package names
     * that are outside the schema (and potentially different per user that created
     * the model).<br>
     * <br>
     * 
     * NOTE: for an Association- or OperationInfo its name is returned
     * 
     * @return tbd
     */
    public String fullName();

    /**
     * Fully qualified UML name (Package1::...::PackageN::InfoName) with scope being
     * the schema that the Info object belongs to. The package name part therefore
     * only contains package names that are within the same schema (and thus is
     * independent on how a user incorporated the schema in the model).<br>
     * <br>
     * 
     * NOTE: for an Association- or OperationInfo its name is returned
     * 
     * @return tbd
     */
    public String fullNameInSchema();

    /**
     * The primary language used in the information about the model element. Use
     * codes from IETF RFC 5646. See also ISO 19109.<br>
     * <br>
     * 
     * If no value is provided for the model element, it is derived from the parent
     * element, eventually the application schema.<br>
     * <br>
     * 
     * &lt;DescriptorSource descriptor="language" source="tag"
     * tag="language"/&gt;<br>
     * 
     * @return primary language code; can be <code>null</code> if no language
     *         information is provided in the schema.
     */
    public String language();

    /**
     * Short code of the model element.<br>
     * <br>
     * 
     * This may either be taken from the text documentation, where the value would
     * be separated from other parts by configurable separators, or in a tagged
     * value. Default separators and tags exist, but may be configured. <br>
     * <br>
     * 
     * &lt;DescriptorSource descriptor="primaryCode" source="tag"
     * tag="code"/&gt;<br>
     * &lt;DescriptorSource descriptor="primaryCode" source="tag"
     * tag="primaryCode"/&gt;<br>
     * 
     * @return primary code of the model element, can be <code>null</code>
     */
    public String primaryCode();

    /**
     * Full text documentation of the model element, in EA the notes field. The
     * alias, definition, description (notes and/or examples), legal basis, data
     * capturing rules, etc. may be maintained in the text documentation of the
     * element using configurable separators in a fixed sequence or in separate
     * tagged values, in which case the documentation is compiled from the parts on
     * demand. Only one value per language.<br>
     * <br>
     * 
     * If text is available in multiple languages, the default language as stated in
     * the configuration is returned, if available. The first fallback is English,
     * the next text with no language classification.<br>
     * <br>
     * 
     * Note that this operation should not be used in transformations or targets.
     * Use the specific descriptors instead or derivedDocumentation(). <br>
     * <br>
     * 
     * &lt;DescriptorSource descriptor="documentation" source="ea:notes"/&gt; <br>
     * &lt;DescriptorSource descriptor="documentation" source="tag"
     * tag="documentation"/&gt;<br>
     * 
     * @return text documentation of UML model element from the model, can be the
     *         empty string but not <code>null</code>
     */
    public String documentation();

    /**
     * @param template The text to use as a template for the documentation. The
     *                 placeholders [[descriptor]] will be replaced by the value of
     *                 the descriptor. I.e. '[[definition]]' will be replaced by the
     *                 definition of the model element
     * @param novalue  The value that will be used, if the descriptor is empty or
     *                 null
     * @return text documentation of the model element derived from the descriptors
     *         using the template
     */
    public String derivedDocumentation(String template, String novalue);

    /**
     * Alias of the model element, typically a human readable name. For now only one
     * value per language, but this is mainly for backwards compatibility. <br>
     * <br>
     * 
     * This may either be taken from the text documentation, where the value would
     * be separated from other parts by configurable separators, or in a tagged
     * value. Default separators and tags exist, but may be configured. <br>
     * <br>
     * 
     * If the alias is available in multiple languages, the default language as
     * stated in the configuration is returned, if available. The first fallback is
     * English, the next text with no language classification.<br>
     * <br>
     * 
     * &lt;DescriptorSource descriptor="alias" source="ea:alias"/&gt;<br>
     * &lt;DescriptorSource descriptor="alias" source="tag" tag="alias"/&gt; <br>
     * &lt;DescriptorSource descriptor="alias" source="tag"
     * tag="designation"/&gt;<br>
     * 
     * @return alias of the model element, can be <code>null</code>
     */
    public String aliasName();

    /**
     * Definition of the model element. This should be an exact statement of the
     * nature, scope, or meaning of the model element. Only one value per language.
     * 
     * This may either be taken from the text documentation, where the value would
     * be separated from other parts by configurable separators, or in a tagged
     * value. Default separators and tags exist, but may be configured. <br>
     * <br>
     * 
     * If text is available in multiple languages, the default language as stated in
     * the configuration is returned, if available. The first fallback is English,
     * the next text with no language classification.<br>
     * <br>
     * 
     * &lt;DescriptorSource descriptor="definition" source="sc:extract"
     * token="PROLOG"/&gt;<br>
     * &lt;DescriptorSource descriptor="definition" source="sc:extract"
     * token="Definition"/&gt;<br>
     * &lt;DescriptorSource descriptor="definition" source="tag"
     * tag="definition"/&gt;<br>
     * 
     * @return definition of model element, can be empty but not <code>null</code>
     */
    public String definition();

    /**
     * Supplementary description of the model element providing additional
     * information intended to assist the understanding or use of the model element.
     * Only one value per language.
     * 
     * This may either be taken from the text documentation, where the value would
     * be separated from other parts by configurable separators, or in a tagged
     * value. Default separators and tags exist, but may be configured. <br>
     * <br>
     * 
     * If text is available in multiple languages, the default language as stated in
     * the configuration is returned, if available. The first fallback is English,
     * the next text with no language classification.<br>
     * <br>
     * 
     * &lt;DescriptorSource descriptor="description" source="sc:extract"
     * token="Description"/&gt;<br>
     * &lt;DescriptorSource descriptor="description" source="tag"
     * tag="description"/&gt;<br>
     * 
     * @return description of model element, can be <code>null</code>
     */
    public String description();

    /**
     * Examples of the use of the model element.
     * 
     * This may either be taken from the text documentation, where the value would
     * be separated from other parts by configurable separators, or in a tagged
     * value. Default separators and tags exist, but may be configured. <br>
     * <br>
     * 
     * If text is available in multiple languages, the default language as stated in
     * the configuration is returned, if available. The first fallback is English,
     * the next text with no language classification.<br>
     * <br>
     * 
     * &lt;DescriptorSource descriptor="example" source="tag" tag="example"/&gt;
     * <br>
     * 
     * @return array of examples; can be empty but not <code>null</code>
     */
    public String[] examples();

    /**
     * Specification of the legal basis that is the basis for collecting this model
     * element. Only one value per language.<br>
     * <br>
     * 
     * This may either be taken from the text documentation, where the value would
     * be separated from other parts by configurable separators, or in a tagged
     * value. Default separators and tags exist, but may be configured. <br>
     * <br>
     * 
     * If text is available in multiple languages, the default language as stated in
     * the configuration is returned, if available. The first fallback is English,
     * the next text with no language classification.<br>
     * <br>
     * 
     * &lt;DescriptorSource descriptor="legalBasis" source="tag"
     * tag="legalBasis"/&gt;<br>
     * 
     * @return legal basis for this model element, can be <code>null</code>
     */
    public String legalBasis();

    /**
     * Data capture statements for collecting data for this model element. A general
     * description of the sources and the processes to be used.<br>
     * <br>
     * 
     * This may either be taken from the text documentation, where the value would
     * be separated from other parts by configurable separators, or in a tagged
     * value. Default separators and tags exist, but may be configured. <br>
     * <br>
     * 
     * If text is available in multiple languages, the default language as stated in
     * the configuration is returned, if available. The first fallback is English,
     * the next text with no language classification.<br>
     * <br>
     * 
     * &lt;DescriptorSource descriptor="dataCaptureStatement" source="tag"
     * tag="dataCaptureStatement"/&gt;<br>
     * 
     * @return array of data capture statements; can be empty but not
     *         <code>null</code>
     */
    public String[] dataCaptureStatements();

    /**
     * Retrieves a copy of all (normalized) stereotypes of this model element.
     * <p>
     * NOTE: The returned object is a copy; modifications to that copy do NOT change
     * the stereotypes of this Info object.
     * 
     * @return all (normalized) stereotypes of the model element; can be empty but
     *         not <code>null</code>
     */
    public Stereotypes stereotypes();

    /**
     * Test whether the model element carries a certain stereotype.
     * 
     * @param stereotype a normalized stereotype
     * @return <code>true</code>, if the element carries the stereotype,
     *         <code>false</code> otherwise
     */
    public boolean stereotype(String stereotype);

    /**
     * Retrieves a selected tagged value of the model element. Use only normalized
     * tags.
     * 
     * With UML 2, there may be multiple values per tag. This method issues a
     * warning, if more than one value exists for the tag. I.e., use this method
     * only for cases, where only one value per tag may be provided.
     * 
     * TODO: review existing uses of this method for correct use. Change all other
     * cases to use <code>taggedValuesForTag(String tag)</code> instead.
     * 
     * @param tag normalized tag name of the tagged value to look up
     * @return The tagged value for the tag given or <code>null</code> if the tagged
     *         value is missing. If there are multiple values with the tag only one
     *         is provided.
     */
    public String taggedValue(String tag);

    /**
     * Retrieves all tagged values of the model element with the specified tag.
     * 
     * @param tag normalized tag name of the tagged value to look up
     * @return List of values for the given tag (as objects that have a string value
     *         and optional language indicator); can be empty if no value has been
     *         found but not <code>null</code> .
     */
    public List<LangString> taggedValuesForTagAsLangStrings(String tag);

    /**
     * Retrieves all tagged values of the model element with the specified tag.
     * <br>
     * <br>
     * 
     * @param tag normalized tag name of the tagged value to look up
     * @return The tagged values for the tag given or an emtpy collection if no
     *         value has been found.
     */
    public String[] taggedValuesForTag(String tag);

    /**
     * Retrieves a selected tagged value of the model element, if it is associated
     * with the requested language.
     * 
     * With UML 2, there may be multiple values per tag. This method issues a
     * warning, if more than one value exists for the tag in the language. I.e., use
     * this method only for cases, where only one value per tag may be provided.
     * 
     * @param tag      normalized tag name of the tagged value to look up
     * @param language the language to use, use codes from IETF RFC 5646, e.g. "en".
     * @return The tagged value (in the requested language) for the tag given or
     *         <code>null</code> if the tagged value is missing. If there are
     *         multiple values with the tag only one (in the given language) is
     *         provided.
     */
    public String taggedValueInLanguage(String tag, String language);

    /**
     * Retrieves all tagged values of the model element with the specified tag and
     * if it is associated with the requested language.
     * 
     * @param tag      normalized tag name of the tagged value to look up
     * @param language the language to use, use codes from IETF RFC 5646, e.g. "en".
     * @return The tagged values (in the requested language or without language
     *         classification) for the tag given or an emtpy list if no value has
     *         been found.
     */
    public String[] taggedValuesInLanguage(String tag, String language);

    /**
     * Retrieves selected tagged values of the model element. The method replaces
     * taggedValue(String tag) and taggedValues(String taglist).
     * 
     * @param tagList name(s) of the tagged values to look up. If multiple tags are
     *                provided, tags are assumed to be separated by commas.
     * @return The values for the given tags; can be empty but not
     *         <code>null</code>. The result is a copy of the tagged values of this
     *         Info object - thus modifications to the result won't affect the
     *         tagged values of this Info object.
     */
    public TaggedValues taggedValuesForTagList(String tagList);

    /**
     * Retrieves all tagged values of the model element. The method replaces
     * taggedValues().<br>
     * <br>
     * 
     * The suffix "All" has been added to avoid name clashes with the deprecated
     * method.
     * 
     * @return a copy of the tagged values defined for this object (so subsequent
     *         modifications of the returned object will not affect the tagged
     *         values of this Info object); can be an empty map but not
     *         <code>null</code>.
     */
    public TaggedValues taggedValuesAll();

    public void removeTaggedValue(String tag);

    /**
     * Retrieves all tagged values of the model element.
     * 
     * @deprecated (since="2.5.0") With UML 2, there may be multiple values per tag.
     *             Use <code>taggedValuesMult()</code> instead.
     * 
     * 
     * @return a map with the tagged values defined for this object (key: tagged
     *         value name, value: the value of the tagged value); can be an empty
     *         map but not <code>null</code>. If there are multiple values for a tag
     *         only one is provided.
     */
    @Deprecated
    public Map<String, String> taggedValues();

    /**
     * Return all the tagged values listed in the input string. Tags are assumed to
     * be normalized and separated by commas.
     * 
     * @param tagList tbd
     * 
     * @deprecated (since="2.5.0") With UML 2, there may be multiple values per tag.
     *             Use <code>taggedValuesMult(tagList)</code> instead.
     * 
     * @return a map with the tagged values defined for this object (key: tagged
     *         value name, value: the value of the tagged value); can be an empty
     *         map but not <code>null</code>. If there are multiple values for a tag
     *         only one is provided.
     */
    @Deprecated
    public Map<String, String> taggedValues(String tagList);

    /**
     * Validate cache of tagged values.
     */
    public void validateTaggedValuesCache();

    /**
     * Validate cache of stereotypes.
     */
    public void validateStereotypesCache();

    /**
     * Identifies the encoding rule relevant on the element, given the platform.
     * 
     * @param platform tbd
     * @return the encoding rule relevant on the element
     */
    public String encodingRule(String platform);

    /**
     * Check whether a requirement or conversion rule applies for this model
     * element.
     * 
     * @param rule tbd
     * @return tbd
     */
    public boolean matches(String rule);

    /**
     * 1. Postprocess the model element to execute any actions that require that the
     * complete model has been loaded.<br>
     * 
     * 2. Validate the model element against all applicable requirements and
     * recommendations. All rules applicable to all model elements are validated
     * here, the more specific rules are all validated in the subclasses.
     */
    public void postprocessAfterLoadingAndValidate();

}