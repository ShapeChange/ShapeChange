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
 * (c) 2002-2016 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core.target.ontology;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

/**
 * @author Clemens Portele, Johannes Echterhoff
 *
 */
public class SKOS {
	
	private static Model ontmodel = ModelFactory.createDefaultModel();
	
	/*
	 * Properties
	 */
	public static final Property prefLabel = ontmodel
			.createProperty(OWLISO19150.RDF_NS_W3C_SKOS + "prefLabel");
	public static final Property altLabel = ontmodel
			.createProperty(OWLISO19150.RDF_NS_W3C_SKOS + "altLabel");
	public static final Property definition = ontmodel
			.createProperty(OWLISO19150.RDF_NS_W3C_SKOS + "definition");
	public static final Property scopeNote = ontmodel
			.createProperty(OWLISO19150.RDF_NS_W3C_SKOS + "scopeNote");
	public static final Property notation = ontmodel
			.createProperty(OWLISO19150.RDF_NS_W3C_SKOS + "notation");
	public static final Property inScheme = ontmodel
			.createProperty(OWLISO19150.RDF_NS_W3C_SKOS + "inScheme");
	public static final Property member = ontmodel
			.createProperty(OWLISO19150.RDF_NS_W3C_SKOS + "member");
	public static final Property broader = ontmodel
			.createProperty(OWLISO19150.RDF_NS_W3C_SKOS + "broader");
	public static final Property topConceptOf = ontmodel
			.createProperty(OWLISO19150.RDF_NS_W3C_SKOS + "topConceptOf");
	
	/*
	 * Classes
	 */
	public static final Resource Concept = ontmodel
			.createResource(OWLISO19150.RDF_NS_W3C_SKOS + "Concept");
	public static final Resource ConceptScheme = ontmodel
			.createResource(OWLISO19150.RDF_NS_W3C_SKOS + "ConceptScheme");
	public static final Resource Collection = ontmodel
			.createResource(OWLISO19150.RDF_NS_W3C_SKOS + "Collection");
}
