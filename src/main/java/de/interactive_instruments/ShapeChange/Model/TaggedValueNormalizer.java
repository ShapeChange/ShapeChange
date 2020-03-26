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
 * (c) 2002-2019 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Model;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.interactive_instruments.ShapeChange.Options;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class TaggedValueNormalizer {

    /*
     * the list of tagged values specified by ISO 19109 (2015)
     * 
     * note: designation is not included, it is considered to be the same as alias
     */
    protected static final Set<String> iso19109Tags = Stream.of("language", "definition", "description")
	    .collect(Collectors.toSet());

    /*
     * the list of tagged values specified by the GML encoding rule
     */
    protected static final Set<String> gmlTags = Stream
	    .of("targetNamespace", "xmlns", "version", "xsdDocument", "gmlProfileSchema", "sequenceNumber",
		    "noPropertyType", "byValuePropertyType", "isCollection", "asDictionary", "inlineOrByReference",
		    "isMetadata", "defaultCodeSpace", "xmlSchemaType", "documentation", "resourceURI", "codeList")
	    .collect(Collectors.toSet());
    // TODO clean-up list

    /*
     * the list of tagged values specified by the JSON encoding rule
     */
    protected static final Set<String> jsonTags = Stream
	    .of("jsonFormat", "jsonDocument", "jsonBaseURI", "jsonBaseUri", "jsonLayerTableURI", "jsonDirectory", "defaultGeometry")
	    .collect(Collectors.toSet());

    /*
     * the list of tagged values specified by the ArcGIS encoding rule
     */
    protected static final Set<String> arcgisTags = Stream.of("HasZ", "HasM", "fieldType").collect(Collectors.toSet());

    /*
     * the list of tagged values specified by the ontology target
     */
    protected static final Set<String> owlTags = Stream.of("owlSubPropertyOf", "owlEquivalentProperties",
	    "owlDisjointProperties", "owlInverseProperties", "owlLogicalCharacteristics").collect(Collectors.toSet());

    /*
     * the list of tagged values specified by the GeoPackage target
     */
    protected static final Set<String> gpkgTags = Stream.of("gpkgZ", "gpkgM").collect(Collectors.toSet());

    /*
     * the list of tagged values specified by other encoding rules
     */
    protected static final Set<String> shapeChangeTags = Stream.of("xsdEncodingRule", "xsdAsAttribute", "gmlAsGroup",
	    "length", "maxLength", "base", "rangeMinimum", "rangeMaximum", "nilReasonAllowed",
	    "gmlImplementedByNilReason", "implementedByNilReason", "primaryCode", "oclExpressions", "alias",
	    "gmlAsCharacterString", "gmlMixin", "nillable", "suppress", "codeListValuePattern",
	    "codeListRepresentation", "uomResourceURI", "uomResourceValuePattern", "uomResourceRepresentation",
	    "physicalQuantity", "recommendedMeasure", "noncomparableMeasure", "asXMLAttribute", "soft-typed", "parent",
	    "AAA:Kennung", "AAA:Datum", "AAA:Organisation", "AAA:Modellart", "AAA:Profile", "AAA:Grunddatenbestand",
	    "AAA:Nutzungsart", "AAA:Nutzungsartkennung", "AAA:objektbildend", "AAA:Themen", "AAA:Revisionsnummer",
	    "AAA:Version", "AAA:AAAVersion", "reverseRoleNAS", "allowedTypesNAS", "gmlArrayProperty", "gmlListProperty",
	    "example", "dataCaptureStatement", "legalBasis", "profiles", "name", "infoURL", "broaderListedValue",
	    "skosConceptSchemeSubclassName", "size", "omitWhenFlattened", "maxOccurs", "isFlatTarget", "Title",
	    "formrows", "formcols", "validate", "Reiter", "generationDateTime", "ontologyName", "alwaysVoid",
	    "neverVoid", "appliesTo", "vocabulary", "associativeTable", "jsonEncodingRule", "sqlEncodingRule", "status",
	    "geometry", "oneToManyReferenceColumnName", "dissolveAssociation", "precision", "scale", "numericType",
	    "toFeatureType", "toCodelist", "sqlUnique", "codelistType", "sqlOnUpdate", "sqlOnDelete", "shortName",
	    "codeListSource", "codeListSourceCharset", "codeListSourceRepresentation", "codeListRestriction",
	    "arcgisDefaultSubtype", "arcgisSubtypeCode", "arcgisUsedBySubtypes", "arcgisSubtypeInitialValues",
	    "reportable", "dissolveAssociationAttributeType", "dissolveAssociationInlineOrByReference", "extensibility",
	    "obligation", "metadataType", "voidReasonType", "valueTypeOptions", "xsdForcedImports", "pattern").collect(Collectors.toSet());

    /*
     * List of allowed tags of tagged values
     */
    protected HashSet<String> allowedTags = null;
    protected Options options = null;

    public TaggedValueNormalizer(Options options) {

	this.options = options;

	allowedTags = new HashSet<String>(100);

	allowedTags.addAll(iso19109Tags);
	allowedTags.addAll(gmlTags);
	allowedTags.addAll(jsonTags);
	allowedTags.addAll(arcgisTags);
	allowedTags.addAll(owlTags);
	allowedTags.addAll(gpkgTags);
	allowedTags.addAll(shapeChangeTags);

	for (String s : options.parameter("representTaggedValues").split("\\,"))
	    allowedTags.add(s.trim());
	for (String s : options.parameter("addTaggedValues").split("\\,"))
	    allowedTags.add(s.trim());
    }

    /**
     * Tagged values normalization. This returns the tag given, a tag name based
     * upon mapping as defined by a TagAlias configuration element, a de-deprecated
     * tag, or null.
     * 
     * @param tag tbd
     * @return the normalized tag name; can be <code>null</code>
     */
    public String normalizeTaggedValue(String tag) {

	/*
	 * Note: UML tools may have their own way of naming tagged values. Therefore,
	 * tool specific mappings are included here, too. Currently specific support
	 * exists for Rational Rose and Enterprise Architect.
	 */
	if (tag.startsWith("RationalRose$UGAS:")) {
	    tag = tag.substring(18);
	} else if (tag.startsWith("RationalRose$ShapeChange:")) {
	    tag = tag.substring(25);
	} else if (tag.startsWith("RationalRose$")) {
	    tag = tag.substring(13);
	} else if (tag.contains("::")) {
	    // tagged value is qualified - remove name space
	    tag = tag.substring(tag.lastIndexOf("::") + 2);
	}

	// Now check tag aliases provided in the configuration
	tag = options.normalizeTag(tag);

	// Now allow for some deprecated stuff
	if (tag.equals("xmlNamespace"))
	    return "targetNamespace";
	if (tag.equals("xmlNamespaceAbbreviation"))
	    return "xmlns";
	if (tag.equals("xsdName"))
	    return "xsdDocument";
	if (tag.equals("asGroup"))
	    return "gmlAsGroup";
	if (tag.equals("implementedByNilReason"))
	    return "gmlImplementedByNilReason";

	if (options.allowAllTags() || allowedTags.contains(tag))
	    return tag;

	// None of these, return null
	return null;
    }
}
