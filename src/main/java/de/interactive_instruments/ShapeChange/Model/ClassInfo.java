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

package de.interactive_instruments.ShapeChange.Model;

import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import de.interactive_instruments.ShapeChange.StructuredNumber;

/** Information about a UML class. */
public interface ClassInfo extends Info {
	public String xmlSchemaType();

	/**
	 * Determine whether a standard property type shall be included in the
	 * schema. This applies for classes that are object or data types.
	 */
	public boolean includePropertyType();

	/**
	 * Determine whether a property type shall be included in the schema for the
	 * object type that does not support Xlinks but requires encoding the
	 * property value inline.
	 */
	public boolean includeByValuePropertyType();

	public boolean isCollection();

	public boolean asDictionary();

	public boolean asGroup();

	public boolean asCharacterString();

	public boolean hasNilReason();

	public PackageInfo pkg();

	public boolean isAbstract();

	public boolean isLeaf();

	public AssociationInfo isAssocClass();

	/**
	 * Provides the ids of all direct base classes of this class (includes the
	 * class that is returned by method {@link #baseClass()}).
	 * 
	 * @return Set of ids of the base classes of this class; an empty set if
	 *         this class has no supertypes
	 */
	public SortedSet<String> supertypes();

	/**
	 * @return Set with the ids of all direct subtypes of this class (WARNING:
	 *         this can be a shallow copy or derived set, thus it is not safe to
	 *         assume that modifications to this set will update the subtype
	 *         information in the class itself). Can be empty but not
	 *         <code>null</code>.
	 */
	public SortedSet<String> subtypes();

	/**
	 * @return Set with the ids of all direct - and indirect - subtypes of this
	 *         class (WARNING: this can be a shallow copy or derived set, thus
	 *         it is not safe to assume that modifications to this set will
	 *         update the subtype information in the class itself).
	 */
	public SortedSet<String> subtypesInCompleteSubtypeHierarchy();

	/**
	 * Check whether the class and the package pi are part of the same schema (=
	 * XML namespace).
	 */
	public boolean inSchema(PackageInfo pi);

	public ClassInfo baseClass();

	public String qname();

	public boolean processed(int t);

	public void processed(int t, boolean p);

	public int category();

	/**
	 * This is supposed to find out, whether the given category 'cat' applied in
	 * 'this' class complies to the categories of all its base classes. If at
	 * least one base class does not comply, 'false' is returned.
	 */
	public boolean checkSupertypes(int cat);

	/**
	 * Get a map of all navigable properties (attributes and association roles)
	 * that belong to this class - NOT one of its base classes.
	 * 
	 * @return A map containing all navigable properties (attributes and
	 *         association roles) that belong to this class (NOT one of its base
	 *         classes), sorted by their sequence / structured numbers. The map
	 *         can be empty but not <code>null</code>.
	 */
	public SortedMap<StructuredNumber, PropertyInfo> properties();

	/**
	 * This method returns the constraints associated with the class.
	 * 
	 * @return the constraints associated with the class; can be empty but not
	 *         <code>null</code>
	 */
	public List<Constraint> constraints();

	public boolean hasConstraint(String name);

	/**
	 * Find the property given by its name in this class or (if not present
	 * there) recursively in its base classes.
	 * 
	 * Note: a ClassInfo does not keep track of non-navigable properties. Such
	 * properties occur in directed associations and are only referenced there.
	 * 
	 * @param name
	 * @return
	 */
	public PropertyInfo property(String name);

	public OperationInfo operation(String name, String[] types);

	/**
	 * Determine whether this type is a direct or indirect subtype of the
	 * argument type.
	 */
	public boolean isSubtype(ClassInfo ci);

	/**
	 * Determine if this type is of the given kind, i.e. if one of its (direct
	 * or indirect) supertypes has a name that equals the given one.
	 * 
	 * @param supertypeName
	 * @return
	 */
	public boolean isKindOf(String supertypeName);

	public boolean suppressed();

	public ClassInfo unsuppressedSupertype(boolean permitAbstract);

	public boolean asDictionaryGml33();

	/**
	 * Special case of a <<union>> with two properties (value and reasons) that
	 * can be mapped to a native nil/void/null mechanism in some implementation
	 * environments, e.g. XML Schema
	 * 
	 * @return
	 */
	boolean isUnionDirect();

	/**
	 * @return metadata about the diagrams relevant for this class,
	 *         <code>null</code> if no diagrams are available
	 */
	public List<ImageMetadata> getDiagrams();

	/**
	 * @param diagrams
	 *            metadata about the diagrams relevant for this class
	 */
	public void setDiagrams(List<ImageMetadata> diagrams);

	/**
	 * @return an identifier of the class that is globally unique, i.e. unique
	 *         across models, or <code>null</code> if such an identifier is not
	 *         available
	 */
	public String globalId();
}