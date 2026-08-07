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
package de.interactive_instruments.shapechange.app;

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
	rdfTest("src/integrationtests/rdf/skosCodelists/testEA_skos.xml", rdfskos, "testResults/rdf/skosCodelists/INPUT",
		"src/integrationtests/rdf/skosCodelists/reference");
    }

    @Test
    public void testSingleOntologyPerSchema() {
	/*
	 * Ontology (based on ISO 19150-2) - single ontology per schema
	 */
	multiTest("src/integrationtests/owl/singleOntologyPerSchema/testEA_owliso_singleOntologyPerSchema.xml",
		new String[] { "ttl" }, "testResults/owl/singleOntologyPerSchema",
		"src/integrationtests/owl/singleOntologyPerSchema/reference");
    }

    @Test
    public void testRuleOwlPropExternalReference() {
	/*
	 * Target: OWLISO19150; conversion rule: rule-owl-prop-external-reference
	 */
	multiTest("src/integrationtests/owl/propExternalReference/testEA_owliso_propExternalReference.xml",
		new String[] { "ttl" }, "testResults/owl/propExternalReference/owl",
		"src/integrationtests/owl/propExternalReference/reference");
    }

    @Test
    public void testMultipleOntologiesOnePerPackage() {
	/*
	 * Ontology (based on ISO 19150-2) - multiple ontologies - one per package
	 */
	multiTest(
		"src/integrationtests/owl/multipleOntologiesOnePerPackage/testEA_owliso_multipleOntologiesPerSchema.xml",
		new String[] { "ttl" }, "testResults/owl/multipleOntologiesPerSchema/owl",
		"src/integrationtests/owl/multipleOntologiesOnePerPackage/reference/owl");
    }

    @Test
    public void testLabelFromLocalName() {

	multiTest("src/integrationtests/owl/labelFromLocalName/testEA_owliso_labelFromLocalName.xml",
		new String[] { "ttl" }, "testResults/owl/labelFromLocalName/owl",
		"src/integrationtests/owl/labelFromLocalName/reference/owl");
    }

    @Test
    public void testPropertyGeneralizationAndEnrichment() {

	multiTest(
		"src/integrationtests/owl/propertyGeneralizationAndEnrichment/testEA_owl_propertyGeneralizationAndEnrichment.xml",
		new String[] { "ttl" }, "testResults/owl/propertyGeneralizationAndEnrichment/owl",
		"src/integrationtests/owl/propertyGeneralizationAndEnrichment/reference/owl");
    }

    @Test
    public void testOwl_unionAlternativesFromAssociations() {

	/*
	 * rule-owl-cls-union puts every navigable property of a «union» class into one
	 * union set, which is the ISO 19150-2 reading: a union type has alternatives and
	 * nothing else. A model that uses the stereotype for a class carrying both
	 * alternatives and a property every alternative has produces a class expression
	 * that is not merely imprecise but unsatisfiable - the expression asserts
	 * owl:cardinality 0 on that property in every member except its own, while the
	 * property's own minimum multiplicity requires it always, so no instance can
	 * satisfy both.
	 *
	 * rule-owl-cls-unionAlternativesFromAssociations restricts the union set to the
	 * class's association ends, where the model separates the two structurally.
	 *
	 * The fixture pins the case and two controls in one model. ChoiceWithAttribute is
	 * a «union» with the attribute name and associations to AlternativeA and
	 * AlternativeB: its union must cover the two association ends only, and name must
	 * get a qualified cardinality restriction of its own instead - which
	 * addMultiplicity would otherwise omit, because it skips every property of a
	 * «union» class on the assumption that the union expression states the
	 * multiplicity. ChoiceOfAssociations is a «union» with no attribute and must be
	 * unaffected; PlainType is not a union at all and must not move.
	 *
	 * Reverting only the behaviour in OntologyModel.isUnionAlternative, leaving the
	 * rule declared, makes this test fail on ChoiceWithAttribute alone: its union
	 * gains a third member and name loses its restriction, while all four other
	 * classes stay identical.
	 *
	 * The model is provided directly as SCXML rather than derived from an Enterprise
	 * Architect repository, so the test runs without EA on any platform.
	 */
	multiTest(
		"src/integrationtests/owl/unionAlternativesFromAssociations/testEA_owl_unionAlternativesFromAssociations_runWithSCXML.xml",
		new String[] { "ttl" }, "testResults/owl/unionAlternativesFromAssociations",
		"src/integrationtests/owl/unionAlternativesFromAssociations/reference");
    }

    @Test
    public void testQualifiedCardinalityRestrictions() {
	/*
	 * rule-owl-prop-multiplicityAsQualifiedCardinalityRestriction: qualified
	 * cardinality restrictions on datatype properties must use owl:onDataRange
	 * (W3C OWL 2 Structural Specification, Sec. 8.5; Mapping to RDF Graphs,
	 * Sec. 3.2), while restrictions on object properties use owl:onClass
	 * (Sec. 8.3).
	 *
	 * The original model is the Enterprise Architect repository
	 * test_qualifiedCardinalityRestrictions.qea; the SCXML based resources
	 * (test_qualifiedCardinalityRestrictions.zip and the *_runWithSCXML config)
	 * are derived from it via the standard mechanism (run the test with system
	 * variable updateOrCreateScxmlResources=true), so the test executes without
	 * Enterprise Architect on 64bit Java.
	 */
	multiTest(
		"src/integrationtests/owl/qualifiedCardinalityRestrictions/testEA_owl_qualifiedCardinalityRestrictions.xml",
		new String[] { "ttl" }, "testResults/owl/qualifiedCardinalityRestrictions",
		"src/integrationtests/owl/qualifiedCardinalityRestrictions/reference");
    }
}
