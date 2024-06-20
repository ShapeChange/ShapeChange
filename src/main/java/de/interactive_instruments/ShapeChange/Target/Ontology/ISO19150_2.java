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
package de.interactive_instruments.ShapeChange.Target.Ontology;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;

public class ISO19150_2 {
	private static Model ontmodel = ModelFactory.createDefaultModel();
	public static final Property isAbstract = ontmodel.createProperty(OWLISO19150.RDF_NS_ISO_19150_2+"isAbstract");
	public static final Property constraint = ontmodel.createProperty(OWLISO19150.RDF_NS_ISO_19150_2+"constraint");
	public static final Property aggregationType = ontmodel.createProperty(OWLISO19150.RDF_NS_ISO_19150_2+"aggregationType");
	public static final Property associationName = ontmodel.createProperty(OWLISO19150.RDF_NS_ISO_19150_2+"associationName");
	public static final Property isEnumeration = ontmodel.createProperty(OWLISO19150.RDF_NS_ISO_19150_2+"isEnumeration");
}
