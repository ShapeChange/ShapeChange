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
package de.interactive_instruments.ShapeChange.Util.ea;

import java.util.Set;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments
 *         dot de)
 *
 */
public abstract class AbstractEAUtil {

	public static String createMessage(String m, String p1) {
		String _p1 = p1 == null ? "" : p1;
		return m.replace("$1$", _p1);
	}

	public static String createMessage(String m, String p1, String p2) {
		String _p1 = p1 == null ? "" : p1;
		String _p2 = p2 == null ? "" : p2;
		return m.replace("$1$", _p1).replace("$2$", _p2);
	}

	public static String createMessage(String m, String p1, String p2,
			String p3) {
		String _p1 = p1 == null ? "" : p1;
		String _p2 = p2 == null ? "" : p2;
		String _p3 = p3 == null ? "" : p3;
		return m.replace("$1$", _p1).replace("$2$", _p2).replace("$3$", _p3);
	}

	public static String createMessage(String m, String p1, String p2,
			String p3, String p4) {
		String _p1 = p1 == null ? "" : p1;
		String _p2 = p2 == null ? "" : p2;
		String _p3 = p3 == null ? "" : p3;
		String _p4 = p4 == null ? "" : p4;
		return m.replace("$1$", _p1).replace("$2$", _p2).replace("$3$", _p3)
				.replace("$4$", _p4);
	}

	/**
	 * @param stereotypes tbd
	 * @return comma separated list of the set of given stereotypes
	 */
	public static String stereotypesCSV(Set<String> stereotypes) {
		String res = "";
		if (stereotypes != null)
			for (String s : stereotypes) {
				res += (res.isEmpty() ? "" : ",");
				res += s;
			}
		return res;
	}
}
