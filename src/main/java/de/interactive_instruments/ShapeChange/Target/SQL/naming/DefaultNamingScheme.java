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
 * (c) 2002-2017 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target.SQL.naming;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;

/**
 * Handles the creation of names for various constraints (check, foreign key,
 * unique). Different kinds of strategies are applied to 1) create the
 * constraint name, 2) normalize the name, and 3) ensure that the name is unique
 * within this naming scheme.
 * 
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class DefaultNamingScheme implements SqlNamingScheme, MessageSource {

    private NameNormalizer normalizer;
    private ForeignKeyNamingStrategy fkNaming;
    private CheckConstraintNamingStrategy ckNaming;
    private UniqueConstraintNamingStrategy ukNaming;
    private ShapeChangeResult result;
    private UniqueNamingStrategy uniqueNaming;

    // if necessary, add naming strategy for schema names

    public DefaultNamingScheme(ShapeChangeResult result, NameNormalizer normalizer, ForeignKeyNamingStrategy fkNaming,
	    CheckConstraintNamingStrategy ckNaming, UniqueConstraintNamingStrategy ukNaming,
	    UniqueNamingStrategy uniqueNaming) {

	this.result = result;
	this.normalizer = normalizer;
	this.fkNaming = fkNaming;
	this.ckNaming = ckNaming;
	this.ukNaming = ukNaming;
	this.uniqueNaming = uniqueNaming;
    }

    @Override
    public String nameForCheckConstraint(String tableName, String propertyName) {

	String nonNormalizedConstraintName = this.ckNaming.nameForCheckConstraint(tableName, propertyName);

	String normalizedConstraintName = this.normalizer.normalize(nonNormalizedConstraintName);

	String constraintName = this.uniqueNaming.makeUnique(normalizedConstraintName);

	if (constraintName.length() != nonNormalizedConstraintName.length()) {
	    result.addWarning(this, 1, nonNormalizedConstraintName, constraintName);
	}

	return constraintName;
    }

    @Override
    public String nameForForeignKeyConstraint(String tableName, String fieldName, String targetTableName) {

	String nonNormalizedConstraintName = this.fkNaming.nameForForeignKeyConstraint(tableName, fieldName,
		targetTableName);

	String normalizedConstraintName = this.normalizer.normalize(nonNormalizedConstraintName);

	String constraintName = this.uniqueNaming.makeUnique(normalizedConstraintName);

	if (constraintName.length() != nonNormalizedConstraintName.length()) {
	    result.addWarning(this, 2, nonNormalizedConstraintName, constraintName);
	}

	return constraintName;
    }

    @Override
    public String nameForUniqueConstraint(String tableName, String columnName) {

	String nonNormalizedConstraintName = this.ukNaming.nameForUniqueConstraint(tableName, columnName);

	String normalizedConstraintName = this.normalizer.normalize(nonNormalizedConstraintName);

	String constraintName = this.uniqueNaming.makeUnique(normalizedConstraintName);

	if (constraintName.length() != nonNormalizedConstraintName.length()) {
	    result.addWarning(this, 3, nonNormalizedConstraintName, constraintName);
	}

	return constraintName;
    }

    public NameNormalizer getNameNormalizer() {
	return this.normalizer;
    }

    @Override
    public String nameForGeometryIndex(String tableName, String columnName) {
	return "idx_" + tableName + "_" + columnName;
    }

    @Override
    public String message(int mnr) {
	switch (mnr) {
	case 1:
	    return "Name '$1$' for check constraint is truncated to '$2$'";
	case 2:
	    return "Name '$1$' for foreign key constraint is truncated to '$2$'";
	case 3:
	    return "Name '$1$' for unique constraint is truncated to '$2$'";
	default:
	    return "(" + DefaultNamingScheme.class.getName() + ") Unknown message with number: " + mnr;
	}
    }
}
