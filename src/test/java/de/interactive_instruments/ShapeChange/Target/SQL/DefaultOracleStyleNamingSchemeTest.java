package de.interactive_instruments.ShapeChange.Target.SQL;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;

public class DefaultOracleStyleNamingSchemeTest {
	
	private DefaultOracleStyleNamingScheme defaultOracleStyleNamingScheme;
	
	public DefaultOracleStyleNamingSchemeTest() {
		ShapeChangeResult result = new ShapeChangeResult(new Options());
		defaultOracleStyleNamingScheme = new DefaultOracleStyleNamingScheme(result);
	}

	@Test
	public void testCreateNameCheckConstraint() {
		Set<String> allConstraints = new HashSet<String>();
		String checkConstraintName = defaultOracleStyleNamingScheme.createNameCheckConstraint("FeatureWithLongName", "PropertyOfFeature1", allConstraints);
		assertEquals("FEATUREWITHLO_PROPERTYOFFEA_CK", checkConstraintName);
		String checkConstraintName2 = defaultOracleStyleNamingScheme.createNameCheckConstraint("FeatureWithLongName", "PropertyOfFeature2", allConstraints);
		assertEquals("FEATUREWITHLO_PROPERTYOFFEA_CK", checkConstraintName2);
	}

	@Test
	public void testCreateNameForeignKey() {
		Set<String> allConstraints = new HashSet<String>();
		String foreignKeyName = defaultOracleStyleNamingScheme.createNameForeignKey("FeatureT1", "FeatureT2", "LongField", allConstraints);
		assertEquals("FK_FEATURET1_LONGFIELD", foreignKeyName);
		String foreignKeyName2 = defaultOracleStyleNamingScheme.createNameForeignKey("FeatureT1", "FeatureT3", "LongField", allConstraints);
		assertEquals("FK_FEATURET1_LONGFIELD", foreignKeyName2);
		String foreignKeyName3 = defaultOracleStyleNamingScheme.createNameForeignKey("FeatureT1", "FeatureT4", "VeryVeryVeryLongField", allConstraints);
		assertEquals("FK_FEATURET1_VERYVERYVERYLONGF", foreignKeyName3);
	}

}
