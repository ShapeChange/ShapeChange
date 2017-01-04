package de.interactive_instruments.ShapeChange.Target.SQL;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;

public class TruncateOracleStyleNamingSchemeTest {
	
	private TruncateOracleStyleNamingScheme truncateOracleStyleNamingScheme;
	
	public TruncateOracleStyleNamingSchemeTest() {
		ShapeChangeResult result = new ShapeChangeResult(new Options());
		truncateOracleStyleNamingScheme = new TruncateOracleStyleNamingScheme(result);
	}

	@Test
	public void testCreateNameCheckConstraint() {
		Set<String> allConstraints = new HashSet<String>();
		String checkConstraintName = truncateOracleStyleNamingScheme.createNameCheckConstraint("FeatureWithLongName", "PropertyOfFeature1", allConstraints);
		assertEquals("FEATUREWITHLOPROPERTYOFFEA_CK", checkConstraintName);
		String checkConstraintName2 = truncateOracleStyleNamingScheme.createNameCheckConstraint("FeatureWithLongName", "PropertyOfFeature2", allConstraints);
		assertEquals("FEATUREWITHLOPROPERTYOFFEA_CK0", checkConstraintName2);
	}

	@Test
	public void testCreateNameForeignKey() {
		Set<String> allConstraints = new HashSet<String>();
		String foreignKeyName = truncateOracleStyleNamingScheme.createNameForeignKey("FeatureT1", "FeatureT2", "LongField", allConstraints);
		assertEquals("FK_FEATURETFEATURETLONGFIEL", foreignKeyName);
		String foreignKeyName2 = truncateOracleStyleNamingScheme.createNameForeignKey("FeatureT1", "FeatureT3", "LongField", allConstraints);
		assertEquals("FK_FEATURETFEATURETLONGFIEL0", foreignKeyName2);
		String foreignKeyName3 = truncateOracleStyleNamingScheme.createNameForeignKey("FeatureT1", "FeatureT4", "LongField", allConstraints);
		assertEquals("FK_FEATURETFEATURETLONGFIEL1", foreignKeyName3);
	}

}
