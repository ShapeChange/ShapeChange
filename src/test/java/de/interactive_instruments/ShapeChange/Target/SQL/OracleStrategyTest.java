package de.interactive_instruments.ShapeChange.Target.SQL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.junit.Test;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;

public class OracleStrategyTest {
	
	private static final int MAX_ORACLE_LENGTH = 30;
	
	private OracleStrategy oracleStrategy = new OracleStrategy(new ShapeChangeResult(new Options()));
	
	private PearsonHash pearsonHash = new PearsonHash();

	@Test
	public void testUniqueNameCheckConstraint() {
		Set<String> allConstraints = new HashSet<String>();
		String nameCheckConstraint;
		String[] attributeNames = {"anAttribute1kPPvAZ", "anAttribute1w", "anAttribute1VPHR", "anAttribute1qglh", "anAttribute1DYHuHF", "anAttribute1Rinqr", "anAttribute1uld", "anAttribute1BoDlm", "anAttribute1GlLYmU", "anAttribute1wqIfTUEx"};
		for (int i = 0; i < attributeNames.length; i++) {
			// test of the test
			assertEquals("Combination of 'FeatureType' and the attribute name, as uppercase, should give the same pearson hash", "023", pearsonHash.createPearsonHashAsLeftPaddedString(("FeatureType" + attributeNames[i]).toUpperCase(Locale.ENGLISH)).toUpperCase(Locale.ENGLISH));
		}
		nameCheckConstraint = oracleStrategy.createNameCheckConstraint("FeatureType", attributeNames[0], allConstraints);
		assertEquals(MAX_ORACLE_LENGTH - 1, nameCheckConstraint.length());
		assertEquals(nameCheckConstraint.toUpperCase(Locale.ENGLISH), nameCheckConstraint);
		assertEquals(1, allConstraints.size());
		for (int i = 1; i < attributeNames.length; i++) {
			nameCheckConstraint = oracleStrategy.createNameCheckConstraint("FeatureType", attributeNames[i], allConstraints);
			assertEquals(MAX_ORACLE_LENGTH, nameCheckConstraint.length());
			assertEquals(nameCheckConstraint.toUpperCase(Locale.ENGLISH), nameCheckConstraint);
			assertEquals(i+1, allConstraints.size());
			assertTrue(nameCheckConstraint.endsWith(String.valueOf(i-1)));
		}
	}

	@Test
	public void testCreateNameForeignKey() {
		// short test, does not test all possibilities
		Set<String> allConstraints = new HashSet<String>();
		String foreignKeyName = oracleStrategy.createNameForeignKey("FeatureT1", "FeatureT2", "LongField", allConstraints);
		assertEquals(MAX_ORACLE_LENGTH - 1, foreignKeyName.length());
		assertEquals(foreignKeyName.toUpperCase(Locale.ENGLISH), foreignKeyName);
		String foreignKeyName2 = oracleStrategy.createNameForeignKey("FeatureT1", "FeatureT3", "LongField2", allConstraints);
		assertEquals(MAX_ORACLE_LENGTH - 1, foreignKeyName2.length());
		assertEquals(foreignKeyName2.toUpperCase(Locale.ENGLISH), foreignKeyName2);
		assertFalse(foreignKeyName.equals(foreignKeyName2));
	}

}
