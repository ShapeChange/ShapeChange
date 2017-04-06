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
 * (c) 2002-2015 interactive instruments GmbH, Bonn, Germany
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
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */
package de.interactive_instruments.ShapeChange;

/**
 * Enumeration of all targets known to ShapeChange, with a readable name and
 * target ID that should be unique. The ID is used to specify for which targets
 * a class has already been processed - see methods "processed" in interface
 * ClassInfo.
 * 
 * @author Johannes Echterhoff (echterhoff <at> interactive-instruments
 *         <dot> de)
 *
 */
public enum TargetIdentification {
	/*
	 * Note: ID '0' is used by the Converter in getCurrentTargetID() in case
	 * that the target is null.
	 */
	RESERVED_0("-reserved-", 0), XML_SCHEMA("XML Schema", 1), RESERVED_2(
			"-reserved-", 2), RDF("RDF", 3), DEFINITIONS("Definitions",
					4), EXCEL_MAPPING("Excel Mapping", 5), KML_XSLT("KML XSLT",
							6), JSON("JSON Schema", 7), CODELIST_DICTIONARY(
									"Code List Dictionary",
									8), FEATURE_CATALOGUE("Feature Catalogue",
											9), DECODER("Decoder",
													10), APP_CONFIG(
															"App Configuration",
															11), FOL2SCHEMATRON(
																	"First Order Logic to Schematron",
																	12), REPLICATION_SCHEMA(
																			"Replication XML Schema",
																			13), APP_SCHEMA_STATISTICS(
																					"Application Schema Statistics",
																					14), APP_SCHEMA_METADATA(
																							"Application Schema Metadata",
																							15), CODELIST_REGISTER(
																									"Codelist register",
																									19), OWLISO19150(
																											"ISO 19150-2 OWL Ontology",
																											20), SQLDDL(
																													"SQL DDL",
																													30), MODELEXPORT(
																															"Model Export",
																															40), UML_MODEL(
																																	"UML Model",
																																	67), ARCGIS_WORKSPACE(
																																			"ArcGIS Workspace",
																																			111), OBJEKTARTENKATALOG(
																																					"Objektartenkatalog",
																																					401), AAA_PROFIL(
																																							"AAA-Profil (3AP)",
																																							402), AAA_MODELLART(
																																									"AAA-Modellart (3AM)",
																																									404);

	private final String name;
	private final int id;

	TargetIdentification(String name, int id) {
		this.name = name;
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public int getId() {
		return id;
	}
}