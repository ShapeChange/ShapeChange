/**
 * ShapeChange - processing application schemas for geographic information
 *
 * This file is part of ShapeChange. ShapeChange takes a ISO 19109 
 * Application Schema from a UML model and translates it into a 
 * GML Application Schema or other implementation representations.
 *
 * Additional information about the software can be found at
 * http://shapechange.net/
 *
 * (c) 2002-2017 interactive instruments GmbH, Bonn, Germany
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact:
 * interactive instruments GmbH
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("SCXML")
public class OntologyTest extends BasicTestSCXML {

    @Test
    public void testSkosCodelists() {
	/*
	 * SKOS codelists
	 */
	String[] rdfskos = { "Codelists" };
	rdfTest("src/test/resources/rdf/skosCodelists/testEA_skos.xml", rdfskos, "testResults/rdf/skosCodelists/INPUT",
		"src/test/resources/rdf/skosCodelists/reference");
    }

    @Test
    public void testSingleOntologyPerSchema() {
	/*
	 * Ontology (based on ISO 19150-2) - single ontology per schema
	 */
	multiTest("src/test/resources/owl/singleOntologyPerSchema/testEA_owliso_singleOntologyPerSchema.xml",
		new String[] { "ttl" }, "testResults/owl/singleOntologyPerSchema",
		"src/test/resources/owl/singleOntologyPerSchema/reference");
    }

    @Test
    public void testRuleOwlPropExternalReference() {
	/*
	 * Target: OWLISO19150; conversion rule: rule-owl-prop-external-reference
	 */
	multiTest("src/test/resources/owl/propExternalReference/testEA_owliso_propExternalReference.xml",
		new String[] { "ttl" }, "testResults/owl/propExternalReference/owl",
		"src/test/resources/owl/propExternalReference/reference");
    }

    @Test
    public void testMultipleOntologiesOnePerPackage() {
	/*
	 * Ontology (based on ISO 19150-2) - multiple ontologies - one per package
	 */
	multiTest(
		"src/test/resources/owl/multipleOntologiesOnePerPackage/testEA_owliso_multipleOntologiesPerSchema.xml",
		new String[] { "ttl" }, "testResults/owl/multipleOntologiesPerSchema/owl",
		"src/test/resources/owl/multipleOntologiesOnePerPackage/reference/owl");
    }

    @Test
    public void testLabelFromLocalName() {

	multiTest("src/test/resources/owl/labelFromLocalName/testEA_owliso_labelFromLocalName.xml",
		new String[] { "ttl" }, "testResults/owl/labelFromLocalName/owl",
		"src/test/resources/owl/labelFromLocalName/reference/owl");
    }

    @Test
    public void testPropertyGeneralizationAndEnrichment() {

	multiTest(
		"src/test/resources/owl/propertyGeneralizationAndEnrichment/testEA_owl_propertyGeneralizationAndEnrichment.xml",
		new String[] { "ttl" }, "testResults/owl/propertyGeneralizationAndEnrichment/owl",
		"src/test/resources/owl/propertyGeneralizationAndEnrichment/reference/owl");
    }
}
