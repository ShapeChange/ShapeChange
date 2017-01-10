package de.interactive_instruments.ShapeChange.Target.SQL;

import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import de.interactive_instruments.ShapeChange.MessageSource;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;

/**
 * Creates names that are suitable for Oracle database objects. This includes e.g. creating uppercase names that have a maximum
 * length of 30.
 */
public abstract class CommonOracleStyleNamingScheme extends CommonDatabaseObjectNamingScheme implements MessageSource {
	
	protected final int maxLength = 30;
	
	protected ShapeChangeResult result;

	public CommonOracleStyleNamingScheme(ShapeChangeResult result) {
		this.result = result;
	}

	@Override
	public String normalizeName(String name) {
		String upperCaseName = replaceIllegalCharactersAndConvertToUpperCase(name);
		String normalizedName = StringUtils.substring(upperCaseName, 0, maxLength);
		if (upperCaseName.length() != normalizedName.length()) {
			result.addWarning(this, 1, upperCaseName, normalizedName);
		}
		return normalizedName;
	}

	/**
	 * 
	 * @param string
	 * @return an empty string is returned when the argument is null
	 */
	protected String replaceIllegalCharactersAndConvertToUpperCase(String string) {
		return replaceIllegalCharacters(StringUtils.defaultString(string).toUpperCase(Locale.ENGLISH));
	}
	
	/**
	 * Adds a digit to the given constraint name if that name was already assigned to a constraint. The method calling
	 * this one needs to ensure that the max length is not exceeded.
	 */
	protected String makeConstraintNameUnique(String proposedConstraintName, Set<String> allConstraintNames) {
		String newProposedConstraintName = proposedConstraintName;
		if (allConstraintNames.contains(proposedConstraintName)) {
			// make constraint name unique by adding a digit to it and testing again for uniqueness
			for (int i = 0; i <= 9; i++) {
				newProposedConstraintName = proposedConstraintName + i;
				if (!allConstraintNames.contains(newProposedConstraintName)) {
					break;
				}
			}
			if (allConstraintNames.contains(newProposedConstraintName)) {
				result.addWarning(this, 2, newProposedConstraintName);
			}
		}
		return newProposedConstraintName;
	}

	@Override
	public String message(int mnr) {
		// use message numbers between 0 and 99 in this class
		switch (mnr) {
		case 0:
			return "Context: class " + this.getClass().getSimpleName();
		case 1:
			return "Name '$1$' is truncated to '$2$'";
		case 2:
			return "Constraint name '$1$' will be present more than once, no unique constraint name could be created.";
		default:
			return "(Unknown message)";
		}
	}

}