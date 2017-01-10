package de.interactive_instruments.ShapeChange;

import org.junit.Test;

public class OntologyTest extends WindowsBasicTest {

	@Test
	public void testSkosCodelists() {
		/*
		 * SKOS codelists
		 */
		String[] rdfskos = { "Codelists" };
		rdfTest("src/test/resources/config/testEA_skos.xml", rdfskos, "testResults/ea/skos/INPUT",
				"src/test/resources/reference/rdf/skos");
	}

	@Test
	public void testSingleOntologyPerSchema() {
		/*
		 * Ontology (based on ISO 19150-2) - single ontology per schema
		 */
		multiTest("src/test/resources/config/testEA_owliso_singleOntologyPerSchema.xml", new String[] { "ttl" },
				"testResults/owl/singleOntologyPerSchema/owl",
				"src/test/resources/reference/owl/singleOntologyPerSchema/owl");
	}

	@Test
	public void testMultipleOntologiesOnePerPackage() {
		/*
		 * Ontology (based on ISO 19150-2) - multiple ontologies - one per
		 * package
		 */
		multiTest("src/test/resources/config/testEA_owliso_multipleOntologiesPerSchema.xml", new String[] { "ttl" },
				"testResults/owl/multipleOntologiesPerSchema/owl",
				"src/test/resources/reference/owl/multipleOntologiesPerSchema/owl");
	}

}
