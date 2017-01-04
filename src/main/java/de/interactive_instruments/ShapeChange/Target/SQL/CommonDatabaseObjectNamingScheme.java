package de.interactive_instruments.ShapeChange.Target.SQL;

public abstract class CommonDatabaseObjectNamingScheme implements DatabaseObjectNamingScheme {
	
	/**
	 * @return string with any occurrence of '.' and '-' replaced by '_'.
	 */
	protected String replaceIllegalCharacters(String string) {
		return string.replace(".", "_").replace("-", "_");
	}

}
