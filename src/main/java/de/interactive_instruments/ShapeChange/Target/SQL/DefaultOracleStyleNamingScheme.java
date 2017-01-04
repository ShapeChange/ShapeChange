package de.interactive_instruments.ShapeChange.Target.SQL;

import java.util.Set;

import org.apache.commons.lang.StringUtils;

import de.interactive_instruments.ShapeChange.ShapeChangeResult;

/**
 * Naming scheme to support backwards compatibility.
 */
public class DefaultOracleStyleNamingScheme extends CommonOracleStyleNamingScheme {

	public DefaultOracleStyleNamingScheme(ShapeChangeResult result) {
		super(result);
	}

	@Override
	public String createNameCheckConstraint(String tableName, String propertyName, Set<String> allConstraintNames) {
		String tableNameUpperCase = replaceIllegalCharactersAndConvertToUpperCase(tableName);
		String propertyNameUpperCase = replaceIllegalCharactersAndConvertToUpperCase(propertyName);
		
		String truncatedName = StringUtils.substring(tableNameUpperCase, 0, 13) + "_" + StringUtils.substring(propertyNameUpperCase, 0, 13);
		String checkConstraintName = truncatedName  + "_CK";
		return checkConstraintName;
	}

	@Override
	public String createNameForeignKey(String tableName, String targetTableName, String fieldName,
			Set<String> allConstraintNames) {
		String tableNameUpperCase = replaceIllegalCharactersAndConvertToUpperCase(tableName);
		String fieldNameUpperCase = replaceIllegalCharactersAndConvertToUpperCase(fieldName);
		
		String proposedForeignKeyName = "FK_" + tableNameUpperCase + "_" + fieldNameUpperCase;
		String foreignKeyName = normalizeName(proposedForeignKeyName); // make sure length is <= 30
		allConstraintNames.add(foreignKeyName);
		return foreignKeyName;
	}

}
