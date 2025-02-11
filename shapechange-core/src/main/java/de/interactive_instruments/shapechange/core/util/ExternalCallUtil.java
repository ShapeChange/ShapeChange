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
 * (c) 2002-2022 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.shapechange.core.util;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.shapechange.core.target.featurecatalogue.StreamGobbler;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class ExternalCallUtil {

    public static String call(List<String> cmds) throws ExternalCallException {

	ProcessBuilder pb = new ProcessBuilder(cmds);

	try {
	    java.lang.Process proc = pb.start();

	    StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream());
	    StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream());

	    errorGobbler.start();
	    outputGobbler.start();

	    errorGobbler.join();
	    outputGobbler.join();

	    int exitVal = proc.waitFor();

	    if (exitVal != 0) {

		String errorOutput = "<no error output>";
		if (errorGobbler.hasResult()) {
		    errorOutput = errorGobbler.getResult();
		}

		throw new ExternalCallException("Invocation of external call resulted in an error. The command was: "
			+ String.join(" ", cmds) + ". Error output: " + errorOutput);
	    }

	    return outputGobbler.getResult();

	} catch (InterruptedException | IOException e) {

	    String excMsg = StringUtils.isBlank(e.getMessage()) ? "<no exception message>" : e.getMessage();

	    throw new ExternalCallException("Exception occurred during invocation of external call. The command was: "
		    + String.join(" ", cmds) + ". Exception message: " + excMsg);
	}
    }
}
