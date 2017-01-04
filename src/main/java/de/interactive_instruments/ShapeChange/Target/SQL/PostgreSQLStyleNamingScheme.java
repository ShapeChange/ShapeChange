package de.interactive_instruments.ShapeChange.Target.SQL;

import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

/**
 * Creates names that are suitable for PostgreSQL database objects. This includes e.g. creating lowercase names.
 */
public class PostgreSQLStyleNamingScheme extends CommonDatabaseObjectNamingScheme {
	
	private String replaceIllegalCharactersAndConvertToLowerCase(String string) {
		return replaceIllegalCharacters(StringUtils.defaultString(string).toLowerCase(Locale.ENGLISH));
	}

	@Override
	public String normalizeName(String name) {
		return replaceIllegalCharactersAndConvertToLowerCase(name);
	}

	@Override
	public String createNameCheckConstraint(String tableName, String propertyName, Set<String> allConstraintNames) {
		String name = replaceIllegalCharactersAndConvertToLowerCase(tableName) + "_" + replaceIllegalCharactersAndConvertToLowerCase(propertyName) + "_chk";
		// TODO support for choice between prefix and suffix (via rule determined in SqlDdl? new naming scheme might be overkill here?)
		allConstraintNames.add(name);
		return name;
	}

	@Override
	public String createNameForeignKey(String tableName, String targetTableName, String fieldName,
			Set<String> allConstraintNames) {
		String name = "fk_" + replaceIllegalCharactersAndConvertToLowerCase(tableName) + "_" + replaceIllegalCharactersAndConvertToLowerCase(fieldName);
		// TODO support for choice between prefix and suffix (via rule determined in SqlDdl? new naming scheme might be overkill here?)
		allConstraintNames.add(name);
		return name;
	}

}
