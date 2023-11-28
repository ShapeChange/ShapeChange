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
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange.Target.sql_encoding_util;

import java.util.Comparator;
import java.util.Objects;

import org.junit.platform.commons.util.StringUtils;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class SqlClassEncodingInfo extends ModelElementSqlEncodingInfo implements Comparable<SqlClassEncodingInfo> {

    public static final Comparator<SqlClassEncodingInfo> comparator = Comparator
	    .comparing(SqlClassEncodingInfo::getSchemaName, Comparator.nullsFirst(Comparator.naturalOrder()))
	    .thenComparing(SqlClassEncodingInfo::getOriginalSchemaName,
		    Comparator.nullsFirst(Comparator.naturalOrder()))
	    .thenComparing(SqlClassEncodingInfo::getClassName, Comparator.nullsFirst(Comparator.naturalOrder()))
	    .thenComparing(SqlClassEncodingInfo::getOriginalClassName, Comparator.nullsFirst(Comparator.naturalOrder()))
	    .thenComparing(SqlClassEncodingInfo::getTable, Comparator.nullsFirst(Comparator.naturalOrder()))
	    .thenComparing(SqlClassEncodingInfo::getDatabaseSchema, Comparator.nullsFirst(Comparator.naturalOrder()));

    protected String className;
    protected String originalClassName;
    protected String table;
    protected String databaseSchema;

    /**
     * @return the className
     */
    public String getClassName() {
	return className;
    }

    /**
     * @param className the className to set
     */
    public void setClassName(String className) {
	this.className = className;
    }

    /**
     * @return the originalClassName
     */
    public String getOriginalClassName() {
	return originalClassName;
    }

    /**
     * @param originalClassName the originalClassName to set
     */
    public void setOriginalClassName(String originalClassName) {
	this.originalClassName = originalClassName;
    }

    /**
     * @return the table
     */
    public String getTable() {
	return table;
    }

    /**
     * @param table the table to set
     */
    public void setTable(String table) {
	this.table = table;
    }

    /**
     * @return the databaseSchema
     */
    public String getDatabaseSchema() {
	return databaseSchema;
    }

    /**
     * @param databaseSchema the databaseSchema to set
     */
    public void setDatabaseSchema(String databaseSchema) {
	this.databaseSchema = databaseSchema;
    }

    public boolean hasOriginalClassName() {
	return StringUtils.isNotBlank(this.originalClassName);
    }

    public boolean hasDatabaseSchema() {
	return StringUtils.isNotBlank(this.databaseSchema);
    }

    @Override
    public int compareTo(SqlClassEncodingInfo o) {
	if (this == o) {
	    return 0;
	} else {
	    return comparator.compare(this, o);
	}
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = super.hashCode();
	result = prime * result
		+ Objects.hash(className, databaseSchema, originalClassName, table);
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
	SqlClassEncodingInfo other = (SqlClassEncodingInfo) obj;
	return Objects.equals(className, other.className) && Objects.equals(databaseSchema, other.databaseSchema)
		&& Objects.equals(originalClassName, other.originalClassName)
		&& Objects.equals(table, other.table);
    }
}
