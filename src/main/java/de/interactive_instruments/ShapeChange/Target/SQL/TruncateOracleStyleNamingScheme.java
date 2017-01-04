package de.interactive_instruments.ShapeChange.Target.SQL;

import java.util.Set;

import org.apache.commons.lang.StringUtils;

import de.interactive_instruments.ShapeChange.ShapeChangeResult;

/**
 * Uses simple truncation to create constraint names that do not exceed the maximum length in Oracle for database objects and makes
 * sure that constraint names are unique by adding an index if needed.
 */
public class TruncateOracleStyleNamingScheme extends CommonOracleStyleNamingScheme {

	public TruncateOracleStyleNamingScheme(ShapeChangeResult result) {
		super(result);
	}

	@Override
	public String createNameCheckConstraint(String tableName, String propertyName, Set<String> allConstraintNames) {
		String tableNameUpperCase = replaceIllegalCharactersAndConvertToUpperCase(tableName);
		String propertyNameUpperCase = replaceIllegalCharactersAndConvertToUpperCase(propertyName);
		
		String truncatedName = StringUtils.substring(tableNameUpperCase, 0, 13) + StringUtils.substring(propertyNameUpperCase, 0, 13);
		String proposedCheckConstraintName = truncatedName  + "_CK";
		String checkConstraintName = makeConstraintNameUnique(proposedCheckConstraintName, allConstraintNames);
		allConstraintNames.add(checkConstraintName);
		return checkConstraintName;
	}

	/**
	 * Generates foreign key identifiers as follows:
	 * "FK_" + tableNameForFK + "" + targetTableNameForFK + "" + fieldNameForFK + count
	 * where:
	 * <ul><li>tableNameForFK is the name of the table that contains the field with the foreign key, clipped to the first eight characters</li>
	 * <li>targetTableNameForFK is the name of the table that the field with foreign key references, clipped to the first eight characters</li>
	 * <li>fieldNameForFK is the name of the field that contains the foreign key, clipped to the first eight characters</li>
	 * <li>count is the number of times the foreign key identifier has been assigned; it ranges from 0-9 and can also be omitted, thus supporting eleven unambiguous uses of the foreign key identifier</li></ul>
	 */
	@Override
	public String createNameForeignKey(String tableName, String targetTableName, String fieldName,
			Set<String> allConstraintNames) {
		String tableNameUpperCase = replaceIllegalCharactersAndConvertToUpperCase(tableName);
		String targetTableNameUpperCase = replaceIllegalCharactersAndConvertToUpperCase(targetTableName);
		String fieldNameUpperCase = replaceIllegalCharactersAndConvertToUpperCase(fieldName);
		
		String proposedForeignKeyName = "FK_" + StringUtils.substring(tableNameUpperCase, 0, 8) + StringUtils.substring(targetTableNameUpperCase, 0, 8) + StringUtils.substring(fieldNameUpperCase, 0, 8);
		String foreignKeyName = makeConstraintNameUnique(proposedForeignKeyName, allConstraintNames);
		allConstraintNames.add(foreignKeyName);
		return foreignKeyName;
	}
	
	@Override
	public String message(int mnr) {
		// use message numbers between 200 and 299 in this class
		switch (mnr) {
		default:
			return super.message(mnr);
		}
	}

}
