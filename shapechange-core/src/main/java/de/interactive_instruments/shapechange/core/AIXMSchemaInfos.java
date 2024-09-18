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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.shapechange.core;

import java.util.Map;

import de.interactive_instruments.shapechange.core.model.Info;

/**
 * Used to store information on classes and properties of AIXM schemas. At the
 * moment the following information items are stored:
 * <ul>
 * <li>XML target namespace</li>
 * <li>preferred prefix for XML target namespace</li>
 * <li>whether or not the info object belongs to an AIXM extension class</li>
 * </ul>
 * 
 * @author Johannes Echterhoff
 *
 */
public class AIXMSchemaInfos {

	public static class AIXMSchemaInfo {

		private String xmlns;
		private String targetNamespace;
		private boolean isExtension;

		public AIXMSchemaInfo(String xmlns, String targetNamespace,
				boolean isExtension) {
			this.xmlns = xmlns;
			this.targetNamespace = targetNamespace;
			this.isExtension = isExtension;
		}

		/**
		 * @return the xmlns
		 */
		public String xmlns() {
			return xmlns;
		}

		/**
		 * @return the targetNamespace
		 */
		public String targetNamespace() {
			return targetNamespace;
		}

		/**
		 * @return the isExtension
		 */
		public boolean isExtension() {
			return isExtension;
		}
	}

	private Map<String, AIXMSchemaInfo> schemaInfos;

	/**
	 * @param schemaInfosByInfoObjectId
	 *            map of schema infos, with key being the id of the info object
	 *            the AIXMSchemaInfo is defined for
	 */
	public AIXMSchemaInfos(Map<String, AIXMSchemaInfo> schemaInfosByInfoObjectId) {

		this.schemaInfos = schemaInfosByInfoObjectId;
	}

	public String xmlns(Info i) {

		if (schemaInfos.containsKey(i.id())) {
			return schemaInfos.get(i.id()).xmlns;
		} else {
			return null;
		}
	}

	public String targetNamespace(Info i) {

		if (schemaInfos.containsKey(i.id())) {
			return schemaInfos.get(i.id()).targetNamespace;
		} else {
			return null;
		}
	}

	public boolean isExtension(Info i) {

		if (schemaInfos.containsKey(i.id())) {
			return schemaInfos.get(i.id()).isExtension;
		} else {
			return false;
		}
	}

}
