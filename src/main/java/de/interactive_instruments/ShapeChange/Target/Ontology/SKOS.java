package de.interactive_instruments.ShapeChange.Target.Ontology;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;

public class SKOS {
	private static Model ontmodel = ModelFactory.createDefaultModel();
	public static final Property prefLabel = ontmodel.createProperty(OWLISO19150.RDF_NS_W3C_SKOS+"prefLabel");
	public static final Property altLabel = ontmodel.createProperty(OWLISO19150.RDF_NS_W3C_SKOS+"altLabel");
	public static final Property definition = ontmodel.createProperty(OWLISO19150.RDF_NS_W3C_SKOS+"definition");
	public static final Property scopeNote = ontmodel.createProperty(OWLISO19150.RDF_NS_W3C_SKOS+"scopeNote");
	public static final Property notation = ontmodel.createProperty(OWLISO19150.RDF_NS_W3C_SKOS+"notation");
	public static final Property inScheme = ontmodel.createProperty(OWLISO19150.RDF_NS_W3C_SKOS+"inScheme");

}
