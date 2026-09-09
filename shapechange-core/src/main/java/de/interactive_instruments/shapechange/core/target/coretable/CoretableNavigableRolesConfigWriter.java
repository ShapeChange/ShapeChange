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
 * (c) 2002-2026 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core.target.coretable;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.SortedSet;

import org.apache.commons.io.FileUtils;

import de.interactive_instruments.shapechange.core.MessageSource;
import de.interactive_instruments.shapechange.core.ShapeChangeResult;
import de.interactive_instruments.shapechange.core.target.coretable.CoretableNavigableRole.DependentPart;
import de.interactive_instruments.shapechange.core.util.LineEndingNormalizingWriter;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class CoretableNavigableRolesConfigWriter implements MessageSource {

    private ShapeChangeResult result;

    public CoretableNavigableRolesConfigWriter(ShapeChangeResult result) {
	this.result = result;
    }

    public void write(SortedSet<CoretableNavigableRole> navigableRoles, String outputDirectory, String outputFilename,
	    String targetName, String dbSchemaName, boolean generateInsertStatements, String lineSeparator) {

	if (navigableRoles.isEmpty()) {
	    result.addInfo(this, 101);
	} else {

	    checkOutputDirectory(outputDirectory);

	    printNavigableRolesConfiguration(navigableRoles, outputDirectory, outputFilename, targetName, dbSchemaName,
		    generateInsertStatements, lineSeparator);
	}
    }

    private void printNavigableRolesConfiguration(SortedSet<CoretableNavigableRole> navigableRoles,
	    String outputDirectory, String outputFilename, String targetName, String dbSchemaName,
	    boolean generateInsertStatements, String lineSeparator) {

	String fileName = outputFilename + "-navigable-roles.sql";
	File file = new File(outputDirectory, fileName);

	try (PrintWriter writer = new PrintWriter(new LineEndingNormalizingWriter(
		Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8), lineSeparator))) {

	    for (CoretableNavigableRole cnr : navigableRoles) {

		String s;

		if (generateInsertStatements) {
		    s = "INSERT INTO " + dbSchemaName + ".navigable_roles_config "
			    + "(source_featuretype, navigable_role, target_featuretype, appschema, appschema_version, rel_direction, dependent_part) VALUES ('";
		} else {
		    s = "SELECT " + dbSchemaName + ".add_navigable_role('";
		}

		s += cnr.getSourceFeatureType().name() + "','" + cnr.getNavigableRole().name() + "','"
			+ cnr.getTargetFeatureType().name() + "','" + cnr.getAppSchema() + "','" + cnr.getVersion()
			+ "','" + cnr.getRelDirection().name() + "',"
			+ (cnr.getDependentPart() == DependentPart.none ? "NULL"
				: "'" + cnr.getDependentPart().name() + "'");

		if (generateInsertStatements) {

		    s += ") ON CONFLICT (source_featuretype, navigable_role, target_featuretype, appschema, version, rel_direction) "
			    + "DO NOTHING;";

		} else {
		    s += ");";
		}

		writer.println(s);
	    }

	    writer.close();

	    result.addResult(targetName, outputDirectory, fileName, null);

	} catch (IOException e) {
	    result.addError(this, 102, fileName, e.getMessage());
	}
    }

    private void checkOutputDirectory(String outputDirectory) {
	File outputDirectoryFile = new File(outputDirectory);
	if (!outputDirectoryFile.exists()) {
	    try {
		FileUtils.forceMkdir(outputDirectoryFile);
	    } catch (Exception e) {
		result.addError(this, 100, outputDirectory, e.getMessage());
	    }
	}
    }

    @Override
    public String message(int mnr) {

	return switch (mnr) {
	case 0 -> "Context: association role $1$";
	case 1 ->
	    "Context: association between class '$1$' (with property '$2$') and class '$3$' (with property '$4$')";

	case 100 -> "Could not create output directory '$1$'. Exception message is: $2$";
	case 101 -> "??No navigable roles have been identified.";
	case 102 -> "Could not write output file '$1$'. Exception message is: $2$";

	default -> "(" + this.getClass().getName() + ") Unknown message with number: " + mnr;
	};
    }
}
