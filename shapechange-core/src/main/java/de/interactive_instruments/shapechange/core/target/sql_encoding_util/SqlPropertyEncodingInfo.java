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
package de.interactive_instruments.shapechange.core.target.sql_encoding_util;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class SqlPropertyEncodingInfo extends ModelElementSqlEncodingInfo
	implements Comparable<SqlPropertyEncodingInfo> {

    public static final Comparator<SqlPropertyEncodingInfo> comparator = Comparator
	    .comparing(SqlPropertyEncodingInfo::getSchemaName, Comparator.nullsFirst(Comparator.naturalOrder()))
	    .thenComparing(SqlPropertyEncodingInfo::getOriginalSchemaName,
		    Comparator.nullsFirst(Comparator.naturalOrder()))
	    .thenComparing(SqlPropertyEncodingInfo::getPropertyName, Comparator.nullsFirst(Comparator.naturalOrder()))
	    .thenComparing(SqlPropertyEncodingInfo::getOriginalPropertyName,
		    Comparator.nullsFirst(Comparator.naturalOrder()))
	    .thenComparing(SqlPropertyEncodingInfo::getInClassName, Comparator.nullsFirst(Comparator.naturalOrder()))
	    .thenComparing(SqlPropertyEncodingInfo::getOriginalInClassName,
		    Comparator.nullsFirst(Comparator.naturalOrder()))
	    .thenComparing(SqlPropertyEncodingInfo::getPropertyValueType,
		    Comparator.nullsFirst(Comparator.naturalOrder()))
	    .thenComparing(SqlPropertyEncodingInfo::getOriginalPropertyValueType,
		    Comparator.nullsFirst(Comparator.naturalOrder()))
	    .thenComparing(SqlPropertyEncodingInfo::getPropertyMultiplicity,
		    Comparator.nullsFirst(Comparator.naturalOrder()))
	    .thenComparing(SqlPropertyEncodingInfo::getOriginalPropertyMultiplicity,
		    Comparator.nullsFirst(Comparator.naturalOrder()))
	    .thenComparing(SqlPropertyEncodingInfo::getSourceTable, Comparator.nullsFirst(Comparator.naturalOrder()))
	    .thenComparing(SqlPropertyEncodingInfo::getSourceTableSchema,
		    Comparator.nullsFirst(Comparator.naturalOrder()))
	    .thenComparing(SqlPropertyEncodingInfo::getValueSourcePath,
		    Comparator.nullsFirst(Comparator.naturalOrder()))
	    .thenComparing(SqlPropertyEncodingInfo::getIdSourcePath, Comparator.nullsFirst(Comparator.naturalOrder()))
	    .thenComparing(SqlPropertyEncodingInfo::getIdValueType, Comparator.nullsFirst(Comparator.naturalOrder()))
	    .thenComparing(SqlPropertyEncodingInfo::getTargetTable, Comparator.nullsFirst(Comparator.naturalOrder()))
	    .thenComparing(SqlPropertyEncodingInfo::getTargetTableSchema,
		    Comparator.nullsFirst(Comparator.naturalOrder()));

    protected String propertyName;
    protected String originalPropertyName;
    protected String inClassName;
    protected String originalInClassName;
    protected String propertyValueType;
    protected String originalPropertyValueType;
    protected String propertyMultiplicity;
    protected String originalPropertyMultiplicity;
    protected String sourceTable;
    protected String sourceTableSchema;
    protected String valueSourcePath;
    protected String idSourcePath;
    protected String idValueType;
    protected String targetTable;
    protected String targetTableSchema;

    /**
     * @return the propertyName - cannot be <code>null</code>
     */
    public String getPropertyName() {
	return propertyName;
    }

    /**
     * @param propertyName the propertyName to set
     */
    public void setPropertyName(String propertyName) {
	this.propertyName = propertyName;
    }

    /**
     * @return the originalPropertyName - can be <code>null</code>
     */
    public String getOriginalPropertyName() {
	return originalPropertyName;
    }

    /**
     * @param originalPropertyName the originalPropertyName to set
     */
    public void setOriginalPropertyName(String originalPropertyName) {
	this.originalPropertyName = originalPropertyName;
    }

    /**
     * @return the inClassName - cannot be <code>null</code>
     */
    public String getInClassName() {
	return inClassName;
    }

    /**
     * @param inClassName the inClassName to set
     */
    public void setInClassName(String inClassName) {
	this.inClassName = inClassName;
    }

    /**
     * @return the originalInClassName - can be <code>null</code>
     */
    public String getOriginalInClassName() {
	return originalInClassName;
    }

    /**
     * @param originalInClassName the originalInClassName to set
     */
    public void setOriginalInClassName(String originalInClassName) {
	this.originalInClassName = originalInClassName;
    }

    /**
     * @return the propertyValueType - cannot be <code>null</code>
     */
    public String getPropertyValueType() {
	return propertyValueType;
    }

    /**
     * @param propertyValueType the propertyValueType to set
     */
    public void setPropertyValueType(String propertyValueType) {
	this.propertyValueType = propertyValueType;
    }

    /**
     * @return the originalPropertyValueType - can be <code>null</code>
     */
    public String getOriginalPropertyValueType() {
	return originalPropertyValueType;
    }

    /**
     * @param originalPropertyValueType the originalPropertyValueType to set
     */
    public void setOriginalPropertyValueType(String originalPropertyValueType) {
	this.originalPropertyValueType = originalPropertyValueType;
    }

    /**
     * @return the sourceTable - cannot be <code>null</code>
     */
    public String getSourceTable() {
	return sourceTable;
    }

    /**
     * @param sourceTable the sourceTable to set
     */
    public void setSourceTable(String sourceTable) {
	this.sourceTable = sourceTable;
    }

    /**
     * @return the sourceTableSchema - can be <code>null</code>
     */
    public String getSourceTableSchema() {
	return sourceTableSchema;
    }

    /**
     * @param sourceTableSchema the sourceTableSchema to set
     */
    public void setSourceTableSchema(String sourceTableSchema) {
	this.sourceTableSchema = sourceTableSchema;
    }

    /**
     * @return the valueSourcePath - cannot be <code>null</code>
     */
    public String getValueSourcePath() {
	return valueSourcePath;
    }

    /**
     * @param valueSourcePath the valueSourcePath to set
     */
    public void setValueSourcePath(String valueSourcePath) {
	this.valueSourcePath = valueSourcePath;
    }

    /**
     * @return the idSourcePath - can be <code>null</code>
     */
    public String getIdSourcePath() {
	return idSourcePath;
    }

    /**
     * @param idSourcePath   the idSourcePath to set
     * @param idValueTypeOpt the idValueType to set, may be empty
     */
    public void setIdInfos(String idSourcePath, Optional<String> idValueTypeOpt) {
	this.idSourcePath = idSourcePath;
	if (idValueTypeOpt.isPresent()) {
	    this.idValueType = idValueTypeOpt.get();
	}
    }

    /**
     * @return the targetTable - can be <code>null</code>
     */
    public String getTargetTable() {
	return targetTable;
    }

    /**
     * @param targetTable the targetTable to set
     */
    public void setTargetTable(String targetTable) {
	this.targetTable = targetTable;
    }

    /**
     * @return the targetTableSchema - can be <code>null</code>
     */
    public String getTargetTableSchema() {
	return targetTableSchema;
    }

    /**
     * @param targetTableSchema the targetTableSchema to set
     */
    public void setTargetTableSchema(String targetTableSchema) {
	this.targetTableSchema = targetTableSchema;
    }

    public boolean hasOriginalPropertyName() {
	return StringUtils.isNotBlank(this.originalPropertyName);
    }

    public boolean hasOriginalInClassName() {
	return StringUtils.isNotBlank(this.originalInClassName);
    }

    public boolean hasOriginalPropertyValueType() {
	return StringUtils.isNotBlank(this.originalPropertyValueType);
    }

    public boolean hasOriginalPropertyMultiplicity() {
	return StringUtils.isNotBlank(this.originalPropertyMultiplicity);
    }

    public boolean hasSourceTableSchema() {
	return StringUtils.isNotBlank(this.sourceTableSchema);
    }

    public boolean hasTargetTableSchema() {
	return StringUtils.isNotBlank(this.targetTableSchema);
    }

    public boolean hasTargetTable() {
	return StringUtils.isNotBlank(this.targetTable);
    }

    public boolean hasIdSourcePath() {
	return StringUtils.isNotBlank(this.idSourcePath);
    }

    @Override
    public int compareTo(SqlPropertyEncodingInfo o) {
	if (this == o) {
	    return 0;
	} else {
	    return comparator.compare(this, o);
	}
    }

    /**
     * @return the propertyMultiplicity - cannot be <code>null</code>
     */
    public String getPropertyMultiplicity() {
	return propertyMultiplicity;
    }

    /**
     * @param propertyMultiplicity the propertyMultiplicity to set
     */
    public void setPropertyMultiplicity(String propertyMultiplicity) {
	this.propertyMultiplicity = propertyMultiplicity;
    }

    /**
     * @return the originalPropertyMultiplicity - can be <code>null</code>
     */
    public String getOriginalPropertyMultiplicity() {
	return originalPropertyMultiplicity;
    }

    /**
     * @param originalPropertyMultiplicity the originalPropertyMultiplicity to set
     */
    public void setOriginalPropertyMultiplicity(String originalPropertyMultiplicity) {
	this.originalPropertyMultiplicity = originalPropertyMultiplicity;
    }

    /**
     * @return the idValueType - can be <code>null</code>
     */
    public String getIdValueType() {
	return idValueType;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = super.hashCode();
	result = prime * result + Objects.hash(idSourcePath, idValueType, inClassName, originalInClassName,
		originalPropertyMultiplicity, originalPropertyName, originalPropertyValueType, propertyMultiplicity,
		propertyName, propertyValueType, sourceTable, sourceTableSchema, targetTable, targetTableSchema,
		valueSourcePath);
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (!super.equals(obj))
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	SqlPropertyEncodingInfo other = (SqlPropertyEncodingInfo) obj;
	return Objects.equals(idSourcePath, other.idSourcePath) && Objects.equals(idValueType, other.idValueType)
		&& Objects.equals(inClassName, other.inClassName)
		&& Objects.equals(originalInClassName, other.originalInClassName)
		&& Objects.equals(originalPropertyMultiplicity, other.originalPropertyMultiplicity)
		&& Objects.equals(originalPropertyName, other.originalPropertyName)
		&& Objects.equals(originalPropertyValueType, other.originalPropertyValueType)
		&& Objects.equals(propertyMultiplicity, other.propertyMultiplicity)
		&& Objects.equals(propertyName, other.propertyName)
		&& Objects.equals(propertyValueType, other.propertyValueType)
		&& Objects.equals(sourceTable, other.sourceTable)
		&& Objects.equals(sourceTableSchema, other.sourceTableSchema)
		&& Objects.equals(targetTable, other.targetTable)
		&& Objects.equals(targetTableSchema, other.targetTableSchema)
		&& Objects.equals(valueSourcePath, other.valueSourcePath);
    }
}
