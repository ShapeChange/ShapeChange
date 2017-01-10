package de.interactive_instruments.ShapeChange.Target.SQL;

import java.util.Set;

public class NullNamingScheme implements DatabaseObjectNamingScheme {

	@Override
	public String normalizeName(String name) {
		return "";
	}

	@Override
	public String createNameCheckConstraint(String tableName, String propertyName, Set<String> allConstraintNames) {
		return "";
	}

	@Override
	public String createNameForeignKey(String tableName, String targetTableName, String fieldName,
			Set<String> allConstraintNames) {
		return "";
	}

}
