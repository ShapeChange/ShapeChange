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
 * (c) 2002-2013 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Model.Generic;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.AssociationInfoImpl;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Model.Stereotypes;
import de.interactive_instruments.ShapeChange.Model.TaggedValues;

/**
 * @author echterhoff
 * 
 */
public class GenericAssociationInfo extends AssociationInfoImpl {

    protected Options options = null;
    protected ShapeChangeResult result = null;
    protected GenericModel model = null;

    protected String id = null;
//	protected String name = null;

    protected PropertyInfo end1 = null;
    protected PropertyInfo end2 = null;
    protected ClassInfo assocClass = null;

    @Override
    public Options options() {
	return options;
    }

    public void setOptions(Options options) {
	this.options = options;
    }

    @Override
    public ShapeChangeResult result() {
	return result;
    }

    public void setResult(ShapeChangeResult result) {
	this.result = result;

    }

    @Override
    public GenericModel model() {
	return model;
    }

    public void setModel(GenericModel model) {
	this.model = model;

    }

    @Override
    public String id() {
	return id;
    }

    public void setId(String id) {
	this.id = id;

    }

    public void setName(String name) {
	this.name = name;

    }

    @Override
    public PropertyInfo end1() {
	return end1;
    }

    public void setEnd1(PropertyInfo end1) {
	this.end1 = end1;
    }

    @Override
    public PropertyInfo end2() {
	return end2;
    }

    public void setEnd2(PropertyInfo end2) {
	this.end2 = end2;
    }

    @Override
    public ClassInfo assocClass() {
	return assocClass;
    }

    public void setAssocClass(ClassInfo assocClass) {
	this.assocClass = assocClass;
    }

    /** Save the (normalized) stereotypes in the cache. */
    public void validateStereotypesCache() {
	// create cache, if necessary
	if (stereotypesCache == null)
	    stereotypesCache = options().stereotypesFactory();

	// do nothing else, stereotypes have to be set explicitly using
	// setStereotypes

    }

    public void setStereotypes(Stereotypes stereotypeSet) {
	// reset cache
	stereotypesCache = options().stereotypesFactory();
	if (stereotypeSet != null && !stereotypeSet.isEmpty()) {
	    for (String st : stereotypeSet.asArray()) {
		stereotypesCache.add(options.internalize(options.normalizeStereotype(st)));
	    }
	}
    }

    public void setStereotype(String stereotype) {
	// reset cache
	stereotypesCache = options().stereotypesFactory();
	if (stereotype != null) {
	    stereotypesCache.add(options.internalize(options.normalizeStereotype(stereotype)));
	}
    }

    public void validateTaggedValuesCache() {
	// create cache, if necessary
	if (taggedValuesCache == null)
	    taggedValuesCache = options().taggedValueFactory();

	// do nothing else, tagged values have to be set explicitly using
	// setTaggedValues
    }

    /**
     * @param taggedValues tbd
     * @param updateFields true if class fields should be updated based upon
     *                     information from given tagged values, else false
     */
    public void setTaggedValues(TaggedValues taggedValues, boolean updateFields) {

	// clone tagged values
	taggedValuesCache = options().taggedValueFactory(taggedValues);

	// Now update fields, if they are affected by tagged values
	if (updateFields && !taggedValuesCache.isEmpty()) {

	    for (String key : taggedValues.keySet()) {
		updateFieldsForTaggedValue(key, taggedValuesCache.getFirstValue(key)); // FIXME first
										       // only?
	    }
	}
    }

    /**
     * Puts the given tagged value into the existing tagged values cache.
     * 
     * @param tvName       tbd
     * @param tvValue      tbd
     */
    public void setTaggedValue(String tvName, String tvValue) {

	validateTaggedValuesCache();

	taggedValuesCache.put(tvName, tvValue);
    }

    /**
     * Encapsulates the logic to update class fields based upon the value of a named
     * tagged value.
     * 
     * @param tvName  tbd
     * @param tvValue tbd
     */
    private void updateFieldsForTaggedValue(String tvName, String tvValue) {

	// TODO add more updates for relevant tagged values
    }

    /**
     * Adds the prefix to the 'id' of this class. Does NOT update the
     * 'globalIdentifier'.
     * 
     * NOTE: this method is used by the FeatureCatalogue target to ensure that IDs
     * used in a reference model are unique to that model and do not get mixed up
     * with the IDs of the input model.
     * 
     * @param prefix tbd
     */
    public void addPrefixToModelElementIDs(String prefix) {

	this.id = prefix + id;
    }

}
