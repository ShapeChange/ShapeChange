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
package de.interactive_instruments.shapechange.core.target.ldproxy2;

import java.util.LinkedHashMap;
import java.util.Optional;

import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableSchemaConstraints;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class LdpCommonEncoder {

    private boolean measureFragmentCreated = false;

    /**
     * @param propertyMapForBuilder   property map for the ldproxy encoding of the
     *                                property whose value type is Measure
     * @param ldpTypeForValueProperty ldproxy type for the value type of the
     *                                property (that shall be implemented as a
     *                                Measure object)
     */
    public void measureSchema(LinkedHashMap<String, FeatureSchema> propertyMapForBuilder,
	    Type ldpTypeForValueProperty) {

	ImmutableSchemaConstraints.Builder constraintsBuilder = new ImmutableSchemaConstraints.Builder();
	constraintsBuilder.required(true);
	Optional<ImmutableSchemaConstraints> constraints = Optional.of(constraintsBuilder.build());

	ImmutableFeatureSchema valueProp = new ImmutableFeatureSchema.Builder().name("value")
		.type(ldpTypeForValueProperty).label(Ldproxy2Target.measureValueLabel).sourcePath("value")
		.constraints(constraints).build();
	propertyMapForBuilder.put("value", valueProp);

	ImmutableFeatureSchema uomProp = new ImmutableFeatureSchema.Builder().name("uom").type(Type.STRING)
		.label(Ldproxy2Target.measureUomLabel).sourcePath("uom").constraints(constraints).build();
	propertyMapForBuilder.put("uom", uomProp);
    }

    public boolean measureFragmentCreated() {
	return this.measureFragmentCreated;
    }

    public ImmutableFeatureSchema createMeasureFragment(Type ldpTypeForValueProperty) {

	this.measureFragmentCreated = true;

	LinkedHashMap<String, FeatureSchema> propertyMapForMeasureBuilder = new LinkedHashMap<>();

	this.measureSchema(propertyMapForMeasureBuilder, ldpTypeForValueProperty);

	ImmutableFeatureSchema.Builder fragmentBuilder = new ImmutableFeatureSchema.Builder().type(Type.OBJECT)
		.name(Ldproxy2Constants.MEASURE_FRAGMENT_NAME).objectType(Ldproxy2Constants.MEASURE_OBJECT_TYPE)
		.propertyMap(propertyMapForMeasureBuilder);

	return fragmentBuilder.build();
    }
}
