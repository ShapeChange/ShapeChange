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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */

package de.interactive_instruments.ShapeChange.Model;

import java.io.File;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import de.interactive_instruments.ShapeChange.StructuredNumber;
import de.interactive_instruments.ShapeChange.Profile.Profiles;

/** Information about a UML class. */
public interface ClassInfo extends Info {

    /**
     * @return the XML schema type corresponding to the class. This is contained in
     *         the tagged value with tag 'xmlSchemaType'. If this is not specified,
     *         <code>null</code> is returned.
     */
    public String xmlSchemaType();

    /**
     * Determine whether a standard property type shall be included in the schema.
     * This applies for classes that are object or data types.
     * 
     * @return tbd
     */
    public boolean includePropertyType();

    /**
     * Determine whether a property type shall be included in the schema for the
     * object type that does not support Xlinks but requires encoding the property
     * value inline.
     * 
     * @return tbd
     */
    public boolean includeByValuePropertyType();

    /**
     * This predicate determines if the class is a collection. This is done by
     * inspecting the associated tagged value.
     * 
     * @return tbd
     */
    public boolean isCollection();

    /**
     * Find out if this class has to be output as a dictionary.
     * 
     * @return tbd
     */
    public boolean asDictionary();

    /**
     * If a &lt;&lt;Union&gt;&gt; class has a tagged value "asGroup" with a value
     * "true" then it is encoded as a global group which is referenced wherever a
     * property is defined that has the union class as its value. Note that this is
     * only valid, if it is clear from the context how to map the individual values
     * to the conceptual model.
     * 
     * @return tbd
     */
    public boolean asGroup();

    /**
     * If a &lt;&lt;Union&gt;&gt; class has a tagged value "gmlAsCharacterString"
     * with a value "true" it will be translated into an xsd:string simple type
     * regardless of how it is actually built. <br>
     * Note: This is experimental code which is prone to being removed as soon as a
     * better solution for the problem at hand is found.
     * 
     * @return tbd
     */
    public boolean asCharacterString();

    /**
     * @return <code>true</code> if the class has a property (not inherited from a
     *         supertype) for which {@link PropertyInfo#implementedByNilReason()} is
     *         <code>true</code>, else <code>false</code>.
     */
    public boolean hasNilReason();

    /**
     * @return the parent package of the class, i.e. the package the class belongs
     *         to; can be <code>null</code> in rare cases (e.g. a core datatype in an XMI model)
     */
    public PackageInfo pkg();

    public boolean isAbstract();

    /**
     * Determine whether the class is tagged as being a leaf class
     * 
     * @return tbd
     */
    public boolean isLeaf();

    public AssociationInfo isAssocClass();

    /**
     * Provides the ids of all direct base classes of this class (includes the class
     * that is returned by method {@link #baseClass()}).
     * 
     * @return Set of ids of the base classes of this class; an empty set if this
     *         class has no supertypes
     */
    public SortedSet<String> supertypes();

    /**
     * @return Set with the ids of all direct subtypes of this class (WARNING: this
     *         can be a shallow copy or derived set, thus it is not safe to assume
     *         that modifications to this set will update the subtype information in
     *         the class itself). Can be empty but not <code>null</code>.
     */
    public SortedSet<String> subtypes();

    /**
     * @return Set with all direct - and indirect - subtypes of this class (WARNING:
     *         this can be a shallow copy or derived set, thus it is not safe to
     *         assume that modifications to this set will update the subtype
     *         information in the class itself). Can be empty but not
     *         <code>null</code>
     */
    public SortedSet<ClassInfo> subtypesInCompleteHierarchy();

    /**
     * @return Set with all direct - and indirect - supertypes of this class
     *         (WARNING: this can be a shallow copy or derived set, thus it is not
     *         safe to assume that modifications to this set will update the
     *         supertype information in the class itself). Can be empty but not
     *         <code>null</code>
     */
    public SortedSet<ClassInfo> supertypesInCompleteHierarchy();

    /**
     * @return Set with all direct supertypes of this class (WARNING: this can be a
     *         shallow copy or derived set, thus it is not safe to assume that
     *         modifications to this set will update the supertype information in
     *         the class itself). Can be empty but not <code>null</code>
     */
    public SortedSet<ClassInfo> supertypeClasses();
    
    /**
     * @return Set with all direct subtypes of this class (WARNING: this can be a
     *         shallow copy or derived set, thus it is not safe to assume that
     *         modifications to this set will update the subtype information in
     *         the class itself). Can be empty but not <code>null</code>
     */
    public SortedSet<ClassInfo> subtypeClasses();

    /**
     * Check whether the class and the package pi are part of the same schema (= XML
     * namespace).
     * 
     * @param pi tbd
     * @return tbd
     */
    public boolean inSchema(PackageInfo pi);

    /**
     * This determines the particular base class of a class in the sense of ISO19136
     * annex D+E. Only classes of categories resulting from the acknowledged
     * stereotypes are considered. A base class is selected if it has the same
     * category as this class or category unknown. However mixin classes are always
     * ignored.
     * 
     * @return tbd
     */
    public ClassInfo baseClass();

    /**
     * Return the namespace-prefixed class name. The namespace prefix is fetched
     * from the package the class belongs to. If no prefix is found, the class name
     * alone is returned.
     * 
     * @return tbd
     */
    public String qname();

    /**
     * Return the category of the class.
     * 
     * @return tbd
     */
    public int category();

    /**
     * This is supposed to find out, whether the given category 'cat' applied in
     * 'this' class complies to the categories of all its base classes. If at least
     * one base class does not comply, 'false' is returned.
     * 
     * @param cat tbd
     * @return tbd
     */
    public boolean checkSupertypes(int cat);

    /**
     * Get a map of all navigable properties (attributes and association roles) that
     * belong to this class - NOT one of its base classes.
     * 
     * @return A map containing all navigable properties (attributes and association
     *         roles) that belong to this class (NOT one of its base classes),
     *         sorted by their sequence / structured numbers. The map can be empty
     *         but not <code>null</code>.
     */
    public SortedMap<StructuredNumber, PropertyInfo> properties();

    /**
     * Get a set of all navigable properties (attributes and association roles) that
     * belong to this class or one of the types in its supertype hierarchy.
     * Overridden properties are omitted!
     * 
     * @return A set containing all navigable properties (attributes and association
     *         roles) that belong to this class or one of the types in its supertype
     *         hierarchy - excluding overridden properties. The set can be empty but
     *         not <code>null</code>.
     */
    public SortedSet<PropertyInfo> propertiesAll();

    /**
     * This method returns the constraints associated with the class, i.e. the
     * constraints that are directly defined for the class, as well as constraints
     * from supertypes - except for those constraints from a supertype that are
     * overridden by subtypes.
     * 
     * @return a new list with the constraints associated with the class (direct
     *         constraints as well as those from supertypes, minus overridden ones);
     *         can be empty but not <code>null</code>; note that the list is
     *         computed from the direct constraints of the class and its supertypes;
     *         modifying the list has no impact on the direct constraints
     */
    public List<Constraint> constraints();

    /**
     * @return the constraints directly defined for the class; can be empty but not
     *         <code>null</code>
     */
    public List<Constraint> directConstraints();

    /**
     * Find out if the class is associated with a constraint of the given name.
     * Reviews all constraints of the class (direct constraints, and those of
     * supertypes). More efficient overwrites should be added in the various models.
     * 
     * @param name tbd
     * @return tbd
     */
    public boolean hasConstraint(String name);

    /**
     * Find the property given by its name in this class or (if not present there)
     * recursively in its base classes.
     * 
     * Note: a ClassInfo does not keep track of non-navigable properties. Such
     * properties occur in directed associations and are only referenced there.
     * 
     * @param name tbd
     * @return a property with that name (from the class or one of its supertypes),
     *         or <code>null</code> if no property was found
     */
    public PropertyInfo property(String name);

    /**
     * Look up the property with the given name in the properties owned by this
     * class. The search does not extend to supertypes of the class.
     * 
     * @param name tbd
     * @return The property with the given name, or <code>null</code> if no such
     *         property exists.
     */
    public PropertyInfo ownedProperty(String name);

    public OperationInfo operation(String name, String[] types);

    /**
     * Determine whether this type is a direct or indirect subtype of the argument
     * type.
     * 
     * @param ci tbd
     * @return tbd
     */
    public boolean isSubtype(ClassInfo ci);

    /**
     * Determine if this type is of the given kind, i.e. if one of its (direct or
     * indirect) supertypes has a name that equals the given one.
     * 
     * @param supertypeName tbd
     * @return tbd
     */
    public boolean isKindOf(String supertypeName);

    /**
     * Determine whether this is a 'suppressed' class. A suppressed class is for
     * attaching constraints to its next direct or indirect unsuppressed superclass.
     * 
     * @return tbd
     */
    public boolean suppressed();

    /**
     * Find the next direct or indirect superclass of this class which is not
     * suppressed. Only concrete classes are considered if permitAbstract is false,
     * otherwise also abstract classes are deemed a valid return values. <br>
     * The logic of superclass determination is as defined by method
     * baseClass().<br>
     * If no such class can be found <i>null</i> is returned. If the class where we
     * start (this class) is already found unsuppressed, then this class is
     * returned.
     * 
     * @param permitAbstract tbd
     * @return tbd
     */
    public ClassInfo unsuppressedSupertype(boolean permitAbstract);

    /**
     * Find out if this class has to be output as a dictionary.
     * 
     * @return tbd
     */
    public boolean asDictionaryGml33();

    /**
     * Special case of a &lt;&lt;union&gt;&gt; with two properties (value and
     * reasons) that can be mapped to a native nil/void/null mechanism in some
     * implementation environments, e.g. XML Schema
     * 
     * @return tbd
     */
    boolean isUnionDirect();

    /**
     * @return metadata about the diagrams relevant for this class,
     *         <code>null</code> if no diagrams are available
     */
    public List<ImageMetadata> getDiagrams();

    /**
     * @param diagrams metadata about the diagrams relevant for this class
     */
    public void setDiagrams(List<ImageMetadata> diagrams);

    /**
     * @return a .docx file that is linked to this class, typically providing an
     *         extensive amount of formatted documentation; can be <code>null</code>
     */
    public File getLinkedDocument();

    /**
     * @param linkedDocument a .docx file that is linked to this class; can be
     *                       <code>null</code>
     */
    public void setLinkedDocument(File linkedDocument);

    /**
     * @return the profiles defined for this class; can be empty but not
     *         <code>null</code>
     */
    public Profiles profiles();
}