package de.interactive_instruments.ShapeChange.Target.Ontology;

import org.apache.jena.rdf.model.Resource;

import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;

public interface OntologyDocument {

	String getRdfNamespace();

	String getCodeNamespace();

	void addClass(ClassInfo ci);

	void finalizeDocument();

	String getPrefix();

	String getPrefixForCode();

	void print(String outputDirectory, ShapeChangeResult r);

	String getBackPath();

	String getFileName();

	String getPath();

	String getName();

	Resource getResource(ClassInfo ci);

}
