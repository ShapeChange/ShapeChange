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
 * (c) 2002-2012 interactive instruments GmbH, Bonn, Germany
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

import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.Generic.GenericModel;

/**
 * Helper class to store type information (the internal id within the model and
 * the local, unqualified name)
 * 
 * @author portele, Johannes Echterhoff
 *
 */
public class Type {

    public String id = null;
    public String name = null;

    public Type() {

    }

    public Type(String id, String name) {
	this.id = id;
	this.name = name;
    }

    public Type createCopy() {

	Type copy = new Type();
	copy.name = this.name;
	copy.id = this.id;

	return copy;
    }

    public static Type from(ClassInfo ci) {

	Type t = new Type();
	t.name = ci.name();
	t.id = ci.id();

	return t;
    }

    /**
     * Create a {@link Type} based on a name and a generic model. If a
     * {@link ClassInfo} with the given name is found in the {@link GenericModel}
     * the type will get the id of that {@link ClassInfo}, otherwise the id will be
     * set to "unknown".
     * 
     * @param typeName name of the type to find and create
     * @param genModel generic model
     * @return a new Type object with ID matching the ID of the ClassInfo with given
     *         type name, if found in the model, otherwise 'unknown'; the name will
     *         be the given name
     * 
     */
    public static Type from(String typeName, GenericModel genModel) {
	ClassInfo classInfo = genModel.classByName(typeName);
	Type type = new Type();
	type.id = (classInfo != null) ? classInfo.id() : "unknown";
	type.name = typeName;
	return type;
    }
}
