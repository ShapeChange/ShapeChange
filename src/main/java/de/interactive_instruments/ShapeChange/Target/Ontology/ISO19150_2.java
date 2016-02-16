package de.interactive_instruments.ShapeChange.Target.Ontology;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;

public class ISO19150_2 {
	private static Model ontmodel = ModelFactory.createDefaultModel();
	public static final Property isAbstract = ontmodel.createProperty(OWLISO19150.RDF_NS_ISO_19150_2+"isAbstract");
	public static final Property constraint = ontmodel.createProperty(OWLISO19150.RDF_NS_ISO_19150_2+"constraint");
	public static final Property aggregationType = ontmodel.createProperty(OWLISO19150.RDF_NS_ISO_19150_2+"aggregationType");
	public static final Property associationName = ontmodel.createProperty(OWLISO19150.RDF_NS_ISO_19150_2+"associationName");
	public static final Property isEnumeration = ontmodel.createProperty(OWLISO19150.RDF_NS_ISO_19150_2+"isEnumeration");
}
