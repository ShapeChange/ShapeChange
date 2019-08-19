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

import de.interactive_instruments.ShapeChange.Options;

/**
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments <dot>
 *         de)
 *
 */
public class TaggedValueNormalizer {

	/*
	 * the list of tagged values specified by ISO 19109 (2015)
	 * 
	 * note: designation is not included, it is considered to be the same as
	 * alias
	 */
	protected static String[] iso19109Tags = { "language", "definition",
			"description" };

	/*
	 * the list of tagged values specified by the GML encoding rule
	 */
	protected static String[] gmlTags = { "targetNamespace", "xmlns", "version",
			"xsdDocument", "gmlProfileSchema", "sequenceNumber",
			"noPropertyType", "byValuePropertyType", "isCollection",
			"asDictionary", "inlineOrByReference", "isMetadata",
			"defaultCodeSpace", "xmlSchemaType", "documentation", "resourceURI",
			"codeList" };
	// TODO clean-up list

	/*
	 * the list of tagged values specified by the JSON encoding rule
	 */
	protected static String[] jsonTags = { "jsonBaseURI", "jsonLayerTableURI",
			"jsonDirectory" };

	/*
	 * the list of tagged values specified by the ArcGIS encoding rule
	 */
	protected static String[] arcgisTags = { "HasZ", "HasM", "fieldType" };

	/*
	 * the list of tagged values specified by the ontology target
	 */
	protected static String[] owlTags = { "owlSubPropertyOf",
			"owlEquivalentProperties", "owlDisjointProperties",
			"owlInverseProperties", "owlLogicalCharacteristics" };

	/*
	 * the list of tagged values specified by other encoding rules
	 */
	protected static String[] shapeChangeTags = { "xsdEncodingRule",
			"xsdAsAttribute", "xsdDerivation", "gmlAsGroup", "length",
			"maxLength", "base", "rangeMinimum", "rangeMaximum", "default",
			"nilReasonAllowed", "gmlImplementedByNilReason",
			"implementedByNilReason", "primaryCode", "secondaryCode",
			"oclExpressions", "schPatterns", "unitMeasure", "voidable", "alias",
			"gmlAsCharacterString", "gmlMixin", "nillable", "suppress",
			"codeListValuePattern", "codeListRepresentation", "uomResourceURI",
			"uomResourceValuePattern", "uomResourceRepresentation",
			"physicalQuantity", "recommendedMeasure", "noncomparableMeasure",
			"asXMLAttribute", "soft-typed", "parent", "AAA:Kennung",
			"AAA:Datum", "AAA:Organisation", "AAA:Modellart", "AAA:Profile",
			"AAA:Grunddatenbestand", "AAA:Nutzungsart",
			"AAA:Nutzungsartkennung", "AAA:objektbildend", "AAA:Themen",
			"AAA:Revisionsnummer", "AAA:Version", "AAA:AAAVersion",
			"reverseRoleNAS", "allowedTypesNAS", "gmlArrayProperty",
			"gmlListProperty", "example", "dataCaptureStatement", "legalBasis",
			"profiles", "name", "infoURL", "broaderListedValue",
			"skosConceptSchemeSubclassName", "size", "omitWhenFlattened",
			"maxOccurs", "isFlatTarget", "Title", "formrows", "formcols",
			"validate", "Reiter", "generationDateTime", "ontologyName",
			"alwaysVoid", "neverVoid", "appliesTo", "vocabulary",
			"associativeTable", "jsonEncodingRule", "sqlEncodingRule", "status",
			"geometry", "oneToManyReferenceColumnName", "dissolveAssociation",
			"precision", "scale", "numericType", "toFeatureType", "toCodelist",
			"sqlUnique", "codelistType", "sqlOnUpdate", "sqlOnDelete",
			"shortName", "codeListSource", "codeListSourceCharset",
			"codeListSourceRepresentation", "codeListRestriction",
			"arcgisDefaultSubtype", "arcgisSubtypeCode", "arcgisUsedBySubtypes",
			"arcgisSubtypeInitialValues", "codeListXML", "reportable",
			"dissolveAssociationAttributeType", "extensibility", "obligation",
			"metadataType", "voidReasonType" };

	/*
	 * List of allowed tags of tagged values
	 */
	protected HashSet<String> allowedTags = null;
	protected Options options = null;

	public TaggedValueNormalizer(Options options) {

		this.options = options;

		allowedTags = new HashSet<String>(100);
		for (String s : iso19109Tags)
			allowedTags.add(s);
		for (String s : gmlTags)
			allowedTags.add(s);
		for (String s : jsonTags)
			allowedTags.add(s);
		for (String s : arcgisTags)
			allowedTags.add(s);
		for (String s : owlTags)
			allowedTags.add(s);
		for (String s : shapeChangeTags)
			allowedTags.add(s);
		for (String s : options.parameter("representTaggedValues").split("\\,"))
			allowedTags.add(s.trim());
		for (String s : options.parameter("addTaggedValues").split("\\,"))
			allowedTags.add(s.trim());
	}

	/**
	 * Tagged values normalization. This returns the tag given, a tag name based
	 * upon mapping as defined by a TagAlias configuration element, a
	 * de-deprecated tag, or null.
	 * 
	 * @param tag
	 * @return the normalized tag name; can be <code>null</code>
	 */
	public String normalizeTaggedValue(String tag) {

		/*
		 * Note: UML tools may have their own way of naming tagged values.
		 * Therefore, tool specific mappings are included here, too. Currently
		 * specific support exists for Rational Rose and Enterprise Architect.
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
