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
 * (c) 2002-2020 interactive instruments GmbH, Bonn, Germany
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

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class DefaultInputAndLogParameterProvider implements InputAndLogParameterProvider {

    protected SortedSet<String> allowedInputParametersWithStaticNames = new TreeSet<>(Stream.of("addTaggedValues",
	    "addStereotypes", "applyDescriptorSourcesWhenLoadingScxml", "appSchemaName", "appSchemaNameRegex",
	    "appSchemaNamespaceRegex", "checkingConstraints", "constraintLoading", "classTypesToCreateConstraintsFor",
	    "codeAbsenceInModelAllowed", "constraintCreationForProperties", "constraintExcelFile",
	    "dontConstructAssociationNames", "excludedPackages", "extractSeparator", "folConstraintTypeRegex", "id",
	    "ignoreEncodingRuleTaggedValues", "ignoreTaggedValues", "inputFile", "inputModelType", "isAIXM",
	    "kmlReferenceType", "language", "loadConstraintsForSelectedSchemasOnly", "loadLinkedDocuments",
	    "mainAppSchema", "navigatingNonNavigableAssociationsWhenParsingOcl", "oclConstraintTypeRegex",
	    "onlyDeferrableOutputWrite", "prohibitLoadingClassesWithStatusTaggedValue", "publicOnly",
	    "representTaggedValues", "repositoryFileNameOrConnectionString", "scxmlXsdLocation",
	    "skipSemanticValidationOfShapeChangeConfiguration", "sortedOutput", "sortedSchemaOutput",
	    "taggedValueImplementation", "tmpDirectory", "transformer", "username and password", "useStringInterning",
	    "loadDiagrams", "sortDiagramsByName", "packageDiagramRegex", "classDiagramRegex")
	    .collect(Collectors.toSet()));

    protected List<Pattern> regexesForAllowedInputParametersWithDynamicNames = null;

    protected SortedSet<String> allowedLogParametersWithStaticNames = new TreeSet<>(
	    Stream.of("reportLevel", "processFlowReportLevel", "logFile", "xsltFile",
		    "reportUnrecognizedParametersAsWarnings").collect(Collectors.toSet()));

    protected List<Pattern> regexesForAllowedLogParametersWithDynamicNames = null;

    @Override
    public SortedSet<String> allowedInputParametersWithStaticNames() {
	return allowedInputParametersWithStaticNames;
    }

    @Override
    public List<Pattern> regexesForAllowedInputParametersWithDynamicNames() {
	return regexesForAllowedInputParametersWithDynamicNames;
    }

    @Override
    public SortedSet<String> allowedLogParametersWithStaticNames() {
	return allowedLogParametersWithStaticNames;
    }

    @Override
    public List<Pattern> regexesForAllowedLogParametersWithDynamicNames() {
	return regexesForAllowedLogParametersWithDynamicNames;
    }

}
