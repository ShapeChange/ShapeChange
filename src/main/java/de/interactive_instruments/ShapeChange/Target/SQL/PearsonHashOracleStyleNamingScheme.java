package de.interactive_instruments.ShapeChange.Target.SQL;

import java.util.Set;

import org.apache.commons.lang.StringUtils;

import de.interactive_instruments.ShapeChange.ShapeChangeResult;

/**
 * Uses Pearson hashing to create unique names for constraints.
 */
public class PearsonHashOracleStyleNamingScheme extends CommonOracleStyleNamingScheme {

	private PearsonHash pearsonHash = new PearsonHash();

	public PearsonHashOracleStyleNamingScheme(ShapeChangeResult result) {
		super(result);
	}

	/**
	 * Constraints in Oracle are in their own namespace and have the maximum length as the other database objects.
	 */
	@Override
	public String createNameCheckConstraint(String tableName, String propertyName, Set<String> allConstraintNames) {
		String tableNameUpperCase = replaceIllegalCharactersAndConvertToUpperCase(tableName);
		String propertyNameUpperCase = replaceIllegalCharactersAndConvertToUpperCase(propertyName);
		
		String proposedCheckConstraintNameWithoutAffix = StringUtils.substring(tableNameUpperCase, 0, 11)
				+ "_"
				+ StringUtils.substring(propertyNameUpperCase, 0, 11)
				+ pearsonHash.createPearsonHashAsLeftPaddedString(tableNameUpperCase + propertyNameUpperCase);
		// TODO support for choice between prefix and suffix (via rule determined in SqlDdl? new naming scheme might be overkill here?)
		String proposedCheckConstraintName = "CK_" + proposedCheckConstraintNameWithoutAffix;
		String checkConstraintName = makeConstraintNameUnique(proposedCheckConstraintName, allConstraintNames);
		allConstraintNames.add(checkConstraintName);
		return checkConstraintName;
	}

	@Override
	public String createNameForeignKey(String tableName, String targetTableName, String fieldName, Set<String> allConstraintNames) {
		String tableNameUpperCase = replaceIllegalCharactersAndConvertToUpperCase(tableName);
		String targetTableNameUpperCase = replaceIllegalCharactersAndConvertToUpperCase(targetTableName);
		String fieldNameUpperCase = replaceIllegalCharactersAndConvertToUpperCase(fieldName);
		
		String proposedForeignKeyNameWithoutAffix = StringUtils.substring(tableNameUpperCase, 0, 7)
			+ "_"
			+ StringUtils.substring(targetTableNameUpperCase, 0, 7)
			+ "_"
			+ StringUtils.substring(fieldNameUpperCase, 0, 7)
			+ pearsonHash.createPearsonHashAsLeftPaddedString(tableNameUpperCase + targetTableNameUpperCase + fieldNameUpperCase);
		// TODO support for choice between prefix and suffix (via rule determined in SqlDdl? new naming scheme might be overkill here?)
		String proposedForeignKeyName = "FK_" + proposedForeignKeyNameWithoutAffix;
		String foreignKeyName = makeConstraintNameUnique(proposedForeignKeyName, allConstraintNames);
		allConstraintNames.add(foreignKeyName);
		return foreignKeyName;
	}
	
	@Override
	public String message(int mnr) {
		// use message numbers between 100 and 199 in this class
		switch (mnr) {
		default:
			return super.message(mnr);
		}
	}


}
