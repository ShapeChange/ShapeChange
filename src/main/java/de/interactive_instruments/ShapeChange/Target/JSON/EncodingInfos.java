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
package de.interactive_instruments.ShapeChange.Target.JSON;

import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.ShapeChange.ShapeChangeException;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class EncodingInfos {

    protected String entityTypeMemberPath = null;
    protected Boolean entityTypeMemberRequired = null;
    protected String idMemberPath = null;
    protected Boolean idMemberRequired = null;
    protected SortedSet<String> idMemberTypes = new TreeSet<>();
    protected SortedSet<String> idMemberFormats = new TreeSet<>();

    /**
     * @return the entityTypeMemberPath
     */
    public Optional<String> getEntityTypeMemberPath() {
	return Optional.ofNullable(entityTypeMemberPath);
    }

    /**
     * @param entityTypeMemberPath the entityTypeMemberPath to set; can be
     *                             <code>null</code>
     */
    public void setEntityTypeMemberPath(String entityTypeMemberPath) {
	this.entityTypeMemberPath = entityTypeMemberPath;
    }

    /**
     * @return the entityTypeMemberRequired
     */
    public Optional<Boolean> getEntityTypeMemberRequired() {
	return Optional.ofNullable(entityTypeMemberRequired);
    }

    /**
     * @param entityTypeMemberRequired the entityTypeMemberRequired to set; can be
     *                                 <code>null</code>
     */
    public void setEntityTypeMemberRequired(Boolean entityTypeMemberRequired) {
	this.entityTypeMemberRequired = entityTypeMemberRequired;
    }

    /**
     * @return the idMemberPath
     */
    public Optional<String> getIdMemberPath() {
	return Optional.ofNullable(idMemberPath);
    }

    /**
     * @param idMemberPath the idMemberPath to set; can be <code>null</code>
     */
    public void setIdMemberPath(String idMemberPath) {
	this.idMemberPath = idMemberPath;
    }

    /**
     * @return the idMemberRequired
     */
    public Optional<Boolean> getIdMemberRequired() {
	return Optional.ofNullable(idMemberRequired);
    }

    /**
     * @param idMemberRequired the idMemberRequired to set; can be <code>null</code>
     */
    public void setIdMemberRequired(Boolean idMemberRequired) {
	this.idMemberRequired = idMemberRequired;
    }

    /**
     * @return the idMemberTypes; can be empty but not <code>null</code>
     */
    public SortedSet<String> getIdMemberTypes() {
	return idMemberTypes;
    }

    /**
     * @param idMemberTypes the idMemberTypes to set; the contents will be copied
     *                      into a new set
     */
    public void setIdMemberTypes(SortedSet<String> idMemberTypes) {
	this.idMemberTypes = new TreeSet<>();
	if (idMemberTypes != null) {
	    this.idMemberTypes.addAll(idMemberTypes);
	}
    }

    public void addIdMemberType(String type) {
	this.idMemberTypes.add(type);
    }

    /**
     * @return the idMemberFormats; can be empty but not <code>null</code>
     */
    public SortedSet<String> getIdMemberFormats() {
	return idMemberFormats;
    }

    /**
     * @param idMemberFormats the idMemberFormats to set; the contents will be
     *                        copied into a new set
     */
    public void setIdMemberFormats(SortedSet<String> idMemberFormats) {
	this.idMemberFormats = new TreeSet<>();
	if (idMemberFormats != null) {
	    this.idMemberFormats.addAll(idMemberFormats);
	}
    }

    public void addIdMemberFormat(String format) {
	this.idMemberFormats.add(format);
    }

    public String toParamValue() {

	StringBuilder sb = new StringBuilder();

	if (entityTypeMemberPath != null) {
	    sb.append(JsonSchemaConstants.ME_PARAM_ENCODING_INFOS_CHAR_ENTITY_TYPE_MEMBER_PATH);
	    sb.append("=");
	    sb.append(entityTypeMemberPath);
	    if (entityTypeMemberRequired != null) {
		sb.append(";");
		sb.append(JsonSchemaConstants.ME_PARAM_ENCODING_INFOS_CHAR_ENTITY_TYPE_MEMBER_REQUIRED);
		sb.append("=");
		sb.append(entityTypeMemberRequired ? "true" : "false");
	    }
	}

	if (idMemberPath != null) {
	    if (sb.length() > 0) {
		sb.append(";");
	    }
	    sb.append(JsonSchemaConstants.ME_PARAM_ENCODING_INFOS_CHAR_ID_MEMBER_PATH);
	    sb.append("=");
	    sb.append(idMemberPath);
	    if (idMemberRequired != null) {
		sb.append(";");
		sb.append(JsonSchemaConstants.ME_PARAM_ENCODING_INFOS_CHAR_ID_MEMBER_REQUIRED);
		sb.append("=");
		sb.append(idMemberRequired ? "true" : "false");
	    }
	    if (!idMemberTypes.isEmpty()) {
		sb.append(";");
		sb.append(JsonSchemaConstants.ME_PARAM_ENCODING_INFOS_CHAR_ID_MEMBER_TYPES);
		sb.append("=");
		sb.append(StringUtils.join(idMemberTypes, ","));
	    }
	    if (!idMemberFormats.isEmpty()) {
		sb.append(";");
		sb.append(JsonSchemaConstants.ME_PARAM_ENCODING_INFOS_CHAR_ID_MEMBER_FORMATS);
		sb.append("=");
		sb.append(StringUtils.join(idMemberFormats, ","));
	    }
	}

	return sb.toString();
    }

    /**
     * @param s the string that contains the encoding infos
     * @return the encoding infos parsed from the given string; <code>null</code>,
     *         if the string is blank
     * @throws IllegalArgumentException if the format of the encoding infos is not
     *                                  as expected
     */
    public static EncodingInfos from(String s) throws IllegalArgumentException {

	if (StringUtils.isBlank(s)) {
	    throw new IllegalArgumentException("String with encoding infos is blank.");
	}

	EncodingInfos encInfo = new EncodingInfos();

	String[] characteristics = s.split("\\s*;\\s*");

	for (String ctmp : characteristics) {

	    String c = ctmp.trim();
	    String[] cParts = c.split("\\s*=\\s*");

	    if (cParts.length <= 1 || cParts.length > 2 || StringUtils.isBlank(cParts[0])
		    || StringUtils.isBlank(cParts[1])) {
		throw new IllegalArgumentException("String with encoding infos contains invalid characteristic: "
			+ StringUtils.defaultIfBlank(c, "<blank value>"));
	    }

	    parseCharacteristic(encInfo, cParts[0], cParts[1]);
	}

	return encInfo;
    }

    private static void parseCharacteristic(EncodingInfos encInfo, String characteristic, String value) {

	if (characteristic.equalsIgnoreCase(JsonSchemaConstants.ME_PARAM_ENCODING_INFOS_CHAR_ENTITY_TYPE_MEMBER_PATH)) {
	    encInfo.setEntityTypeMemberPath(value);
	} else if (characteristic
		.equalsIgnoreCase(JsonSchemaConstants.ME_PARAM_ENCODING_INFOS_CHAR_ENTITY_TYPE_MEMBER_REQUIRED)) {
	    encInfo.setEntityTypeMemberRequired(StringUtils.equalsAnyIgnoreCase(value, "true", "1"));
	} else if (characteristic.equalsIgnoreCase(JsonSchemaConstants.ME_PARAM_ENCODING_INFOS_CHAR_ID_MEMBER_PATH)) {
	    encInfo.setIdMemberPath(value);
	} else if (characteristic
		.equalsIgnoreCase(JsonSchemaConstants.ME_PARAM_ENCODING_INFOS_CHAR_ID_MEMBER_REQUIRED)) {
	    encInfo.setIdMemberRequired(StringUtils.equalsAnyIgnoreCase(value, "true", "1"));
	} else if (characteristic.equalsIgnoreCase(JsonSchemaConstants.ME_PARAM_ENCODING_INFOS_CHAR_ID_MEMBER_TYPES)) {
	    for (String typeTmp : value.split("\\s*,\\s*")) {
		String type = typeTmp.toLowerCase(Locale.ENGLISH).trim();
		if (StringUtils.equalsAny(type, "integer", "number", "string", "boolean")) {
		    encInfo.addIdMemberType(type);
		} else {
		    throw new IllegalArgumentException("Invalid value for characteristic "
			    + JsonSchemaConstants.ME_PARAM_ENCODING_INFOS_CHAR_ID_MEMBER_TYPES + ": " + value);
		}
	    }
	} else if (characteristic
		.equalsIgnoreCase(JsonSchemaConstants.ME_PARAM_ENCODING_INFOS_CHAR_ID_MEMBER_FORMATS)) {
	    for (String formatTmp : value.split("\\s*,\\s*")) {
		String format = formatTmp.trim();
		if (StringUtils.isNotBlank(format)) {
		    encInfo.addIdMemberFormat(format);
		} else {
		    throw new IllegalArgumentException("Invalid value for characteristic "
			    + JsonSchemaConstants.ME_PARAM_ENCODING_INFOS_CHAR_ID_MEMBER_FORMATS + ": " + value);
		}
	    }
	} else {
	    throw new IllegalArgumentException("Unknown characteristic " + characteristic);
	}
    }

    public static EncodingInfos from(Map<String, String> mapEntryCharacteristicsMap) {

	if (mapEntryCharacteristicsMap == null || mapEntryCharacteristicsMap.isEmpty()) {
	    throw new IllegalArgumentException("No encoding info characteristics found.");
	}

	EncodingInfos encInfo = new EncodingInfos();

	for (Entry<String, String> e : mapEntryCharacteristicsMap.entrySet()) {
	    parseCharacteristic(encInfo, e.getKey(), e.getValue());
	}

	return encInfo;
    }

    /**
     * @return <code>true</code>, if none of the encoding info fields has an actual
     *         value; else <code>false</code>
     */
    public boolean isEmpty() {
	return this.entityTypeMemberRequired == null && this.entityTypeMemberPath == null
		&& this.idMemberRequired == null && this.idMemberPath == null && this.idMemberFormats.isEmpty()
		&& this.idMemberTypes.isEmpty();
    }

    /**
     * Merges two sets of encoding infos. Only works for cases in which encoding
     * infos about a specific JSON member is only available in one of the two sets.
     * 
     * @param encodingInfos1 first set of encoding infos
     * @param encodingInfos2 second set of encoding infos
     * @return the resulting encoding infos object
     * @throws ShapeChangeException in case that the two sets of encoding infos are
     *                              incompatible, i.e., define different information
     *                              for the same JSON member; the exception message
     *                              identifies the JSON member
     */
    public static EncodingInfos merge(EncodingInfos encodingInfos1, EncodingInfos encodingInfos2)
	    throws ShapeChangeException {

	if (encodingInfos1.getEntityTypeMemberPath().isPresent()
		&& encodingInfos2.getEntityTypeMemberPath().isPresent()) {

	    if (encodingInfos1.getEntityTypeMemberPath().equals(encodingInfos2.getEntityTypeMemberPath())
		    && encodingInfos1.getEntityTypeMemberRequired()
			    .equals(encodingInfos2.getEntityTypeMemberRequired())) {
		// fine
	    } else {
		throw new ShapeChangeException("entity type");
	    }

	} else if (encodingInfos1.getIdMemberPath().isPresent() && encodingInfos2.getIdMemberPath().isPresent()) {

	    if (encodingInfos1.getIdMemberPath().equals(encodingInfos2.getIdMemberPath())
		    && encodingInfos1.getIdMemberRequired().equals(encodingInfos2.getIdMemberRequired())
		    && encodingInfos1.getIdMemberTypes().equals(encodingInfos2.getIdMemberTypes())
		    && encodingInfos1.getIdMemberFormats().equals(encodingInfos2.getIdMemberFormats())) {
		// fine
	    } else {
		throw new ShapeChangeException("identifier");
	    }
	}

	EncodingInfos result = new EncodingInfos();

	EncodingInfos infosForEntityTypeMember = encodingInfos1.getEntityTypeMemberPath().isPresent() ? encodingInfos1
		: encodingInfos2;
	if (infosForEntityTypeMember.getEntityTypeMemberPath().isPresent()) {
	    result.setEntityTypeMemberPath(infosForEntityTypeMember.getEntityTypeMemberPath().get());
	    // only set additional entity type member infos if the path is present
	    if (infosForEntityTypeMember.getEntityTypeMemberRequired().isPresent()) {
		result.setEntityTypeMemberRequired(infosForEntityTypeMember.getEntityTypeMemberRequired().get());
	    }
	}

	EncodingInfos infosForIdMember = encodingInfos1.getIdMemberPath().isPresent() ? encodingInfos1 : encodingInfos2;
	if (infosForIdMember.getIdMemberPath().isPresent()) {
	    result.setIdMemberPath(infosForIdMember.getIdMemberPath().get());
	    // only set additional id member infos if the path is present
	    if (infosForIdMember.getIdMemberRequired().isPresent()) {
		result.setIdMemberRequired(infosForIdMember.getIdMemberRequired().get());
	    }
	    if (!infosForIdMember.getIdMemberTypes().isEmpty()) {
		result.setIdMemberTypes(infosForIdMember.getIdMemberTypes());
	    }
	    if (!infosForIdMember.getIdMemberFormats().isEmpty()) {
		result.setIdMemberFormats(infosForIdMember.getIdMemberFormats());
	    }
	}

	return result;
    }

}
