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
import de.interactive_instruments.ShapeChange.Multiplicity;
import de.interactive_instruments.ShapeChange.StructuredNumber;
import de.interactive_instruments.ShapeChange.Type;
import de.interactive_instruments.ShapeChange.Profile.Profiles;

public interface PropertyInfo extends Info {

    /**
     * Default start value for assigning sequence numbers to attributes whose
     * sequence number has not been set explicitly. Will be incremented each time a
     * new attribute without explicitly assigned sequence number is encountered.
     * NOTE: counter is not restarted for a new class.
     */
    public static final int GLOBAL_SEQUENCE_NUMBER_START_VALUE_FOR_ATTRIBUTES = -1073741824;
    /**
     * Default start value for assigning sequence numbers to association roles whose
     * sequence number has not been set explicitly. Will be incremented each time a
     * new association role without explicitly assigned sequence number is
     * encountered. NOTE 1: counter is not restarted for a new class. NOTE 2: value
     * has been chosen so that association roles are placed behind attributes.
     */
    public static final int GLOBAL_SEQUENCE_NUMBER_START_VALUE_FOR_ASSOCIATIONROLES = -536870912;

    /**
     * Provides the next value to use as sequence number for attributes whose
     * sequence number has not explicitly been set. It starts at the value of
     * {@link #GLOBAL_SEQUENCE_NUMBER_START_VALUE_FOR_ATTRIBUTES} which is
     * {@value #GLOBAL_SEQUENCE_NUMBER_START_VALUE_FOR_ATTRIBUTES}. Each call of
     * this method increments the returned value by 1.
     * 
     * @return
     */
    public int getNextNumberForAttributeWithoutExplicitSequenceNumber();

    /**
     * Provides the next value to use as sequence number for association roles whose
     * sequence number has not explicitly been set. It starts at the value of
     * {@link #GLOBAL_SEQUENCE_NUMBER_START_VALUE_FOR_ASSOCIATIONROLES} which is
     * {@value #GLOBAL_SEQUENCE_NUMBER_START_VALUE_FOR_ASSOCIATIONROLES}. Each call
     * of this method increments the returned value by 1.
     * 
     * @return
     */
    public int getNextNumberForAssociationRoleWithoutExplicitSequenceNumber();

    /**
     * Find out whether this property is a derived one.
     */
    public boolean isDerived();

    /**
     * Indicate whether this property is an attribute (and not a role)
     */
    public boolean isAttribute();

    public Type typeInfo();

    /**
     * @return the class that is the value type of the property; looked up by id,
     *         and - if not found by id - by name; can be null if the class was not
     *         found in the model
     */
    public ClassInfo typeClass();

    /**
     * Indicate whether the property is navigable. An attribute is always navigable,
     * while an association role may not be navigable.
     */
    public boolean isNavigable();

    /**
     * Find out if the property represents an ordered collection. Default is false
     * in UML 2.4.1.
     */
    public boolean isOrdered();

    /**
     * Find out if the property allows duplicates in the values or if only unique
     * values are allowed. Default is true in UML 2.4.1.
     */
    public boolean isUnique();

    /**
     * @return true if the attribute may not be written to after initialization,
     *         else false (default).
     */
    public boolean isReadOnly();

    /**
     * Find out whether this property is a composition.
     */
    public boolean isComposition();

    /**
     * Find out whether this property is an aggregation.
     */
    public boolean isAggregation();

    /**
     * @return <code>true</code>, if the property is an association end that is
     *         owned (by the {@link #inClass()}), else <code>false</code> (then it
     *         is either an attribute or owned by the association)
     */
    public boolean isOwned();

    public Multiplicity cardinality();

    /**
     * @return the initial value of the property in case such a thing is specified
     *         in the model, null otherwise. This works only for attributes.
     */
    public String initialValue();

    public boolean isRestriction();

    /**
     * @return 'inline' if the property shall be encoded inline, 'byreference' if
     *         the property shall be encoded by reference; otherwise the default is
     *         to assume 'inlineOrByReference' encoding
     */
    public String inlineOrByReference();

    /**
     * @return value of tagged value "defaultCodeSpace" or the empty string if the
     *         tagged value is not set on this property
     */
    public String defaultCodeSpace();

    /**
     * @return <code>true</code> if the property has tagged value 'isMetadata' with
     *         value 'true', else <code>false</code> .
     */
    public boolean isMetadata();

    /**
     * @return the property on the other end of the association; <code>null</code>
     *         for attribute properties.
     */
    public PropertyInfo reverseProperty();

    /**
     * @return the class object to which this property belongs.
     */
    public ClassInfo inClass();

    public void inClass(ClassInfo ci);

    /**
     * @return the name of the property adorned with the namespace prefix of its
     *         class's package.
     */
    public String qname();

    public StructuredNumber sequenceNumber();

    /**
     * @return <code>true</code> if the property allows for nil value treatment.
     *         That is the case if either 1. the tagged value
     *         implementedByNilReason/gmlImplementedByNilReason is
     *         <code>true</code>, or 2. the inClass is a union whose name ends with
     *         "Reason", that has two properties, and the name of the property
     *         itself is equal to (ignoring case) "reason", or 3. the input
     *         parameter "isAIXM" is <code>true</code> and the inClass is a feature
     *         type, object type, or AIXM extension.
     */
    public boolean implementedByNilReason();

    public boolean nilReasonAllowed();

    public void nilReasonAllowed(boolean b);

    /**
     * @return <code>true</code> if the property owns the stereotype 'voidable', or
     *         has tagged value 'nillable' = <code>true</code>
     */
    public boolean voidable();

    /**
     * @return <code>true</code> if the property owns the (normalized) stereotype
     *         'propertymetadata', else <code>false</code>
     */
    public boolean propertyMetadata();

    /**
     * @return the metadata type declared via tagged value 'metadataType', if it is
     *         defined as such, and can be found in the model, else
     *         <code>null</code>
     */
    public ClassInfo propertyMetadataType();

    /**
     * This method returns the constraints associated with the property.
     * 
     * @return the constraints associated with the property; can be empty but not
     *         <code>null</code>
     */
    public List<Constraint> constraints();

    public AssociationInfo association();

    /**
     * @return the category of the property value
     */
    public int categoryOfValue();

    public Qualifier qualifier(String name);

    public List<Qualifier> qualifiers();

    /**
     * @return the profiles defined for this property; can be empty but not
     *         <code>null</code>
     */
    public Profiles profiles();
}
