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
	protected String name = null;

	protected PropertyInfo end1 = null;
	protected PropertyInfo end2 = null;
	protected ClassInfo assocClass = null;

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.Info#options()
	 */
	@Override
	public Options options() {
		return options;
	}

	/**
	 * @param options
	 */
	public void setOptions(Options options) {
		this.options = options;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.Info#result()
	 */
	@Override
	public ShapeChangeResult result() {
		return result;
	}

	/**
	 * @param result
	 */
	public void setResult(ShapeChangeResult result) {
		this.result = result;

	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.Info#model()
	 */
	@Override
	public GenericModel model() {
		return model;
	}

	/**
	 * @param model
	 */
	public void setModel(GenericModel model) {
		this.model = model;

	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.Info#id()
	 */
	@Override
	public String id() {
		return id;
	}

	/**
	 * @param id
	 */
	public void setId(String id) {
		this.id = id;

	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.Info#name()
	 */
	@Override
	public String name() {
		return name;
	}

	/**
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;

	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.AssociationInfo#end1()
	 */
	@Override
	public PropertyInfo end1() {
		return end1;
	}

	/**
	 * @param end1
	 *            the end1 to set
	 */
	public void setEnd1(PropertyInfo end1) {
		this.end1 = end1;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.AssociationInfo#end2()
	 */
	@Override
	public PropertyInfo end2() {
		return end2;
	}

	/**
	 * @param end2
	 *            the end2 to set
	 */
	public void setEnd2(PropertyInfo end2) {
		this.end2 = end2;
	}

	/**
	 * @see de.interactive_instruments.ShapeChange.Model.AssociationInfo#assocClass()
	 */
	@Override
	public ClassInfo assocClass() {
		return assocClass;
	}

	/**
	 * @param assocClass
	 *            the assocClass to set
	 */
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

	} // validateStereotypesCache

	/**
	 * @param stereotypeSet
	 */
	public void setStereotypes(Stereotypes stereotypeSet) {
		// reset cache
		stereotypesCache = options().stereotypesFactory();
		if (stereotypeSet != null && !stereotypeSet.isEmpty()) {
			for (String st : stereotypeSet.asArray()) {
				stereotypesCache.add(
						options.internalize(options.normalizeStereotype(st)));
			}
		}
	}

	/**
	 * @param stereotype
	 */
	public void setStereotype(String stereotype) {
		// reset cache
		stereotypesCache = options().stereotypesFactory();
		if (stereotype != null) {
			stereotypesCache.add(options
					.internalize(options.normalizeStereotype(stereotype)));
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
	 * @param taggedValues
	 */
	public void setTaggedValues(TaggedValues taggedValues) {
		// clone tagged values
		taggedValuesCache = options().taggedValueFactory(taggedValues);
	}

	/**
	 * Adds the prefix to the 'id' of this class. Does NOT update the
	 * 'globalIdentifier'.
	 * 
	 * NOTE: this method is used by the FeatureCatalogue target to ensure that
	 * IDs used in a reference model are unique to that model and do not get
	 * mixed up with the IDs of the input model.
	 * 
	 * @param prefix
	 */
	public void addPrefixToModelElementIDs(String prefix) {

		this.id = prefix + id;
	}

}
