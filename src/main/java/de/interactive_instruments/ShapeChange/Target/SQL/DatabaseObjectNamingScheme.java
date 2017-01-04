package de.interactive_instruments.ShapeChange.Target.SQL;

import java.util.Set;

/**
 * Specifies ways of assigning names (case, length, composition of different model elements, ...)
 * to certain database objects (tables, constraints, ...).
 * 
 * This interface and its implementing classes are an implementation of the Strategy design pattern.
 */
public interface DatabaseObjectNamingScheme {

	/**
	 * @return name that has a suitable case, length, ...
	 */
	String normalizeName(String name);

	/**
	 * @return suitable name for a check constraint, also taking into account the maximum length of names of database objects and the constraint namespace, if applicable
	 */
	String createNameCheckConstraint(String tableName, String propertyName, Set<String> allConstraintNames);

	/**
	 * @return suitable name for a foreign key, also taking into account the maximum length of names of database objects and the constraint namespace, if applicable
	 */
	String createNameForeignKey(String tableName, String targetTableName, String fieldName,
			Set<String> allConstraintNames);

}
