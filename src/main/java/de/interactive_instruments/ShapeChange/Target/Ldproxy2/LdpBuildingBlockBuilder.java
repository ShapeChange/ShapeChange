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
 * (c) 2002-2023 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target.Ldproxy2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public abstract class LdpBuildingBlockBuilder {

    /**
     * Property transformations, to be added to the building blocks of type
     * collections in the service configuration.
     * 
     * key outer map: top level type; value outer map: map with key: property path,
     * value: list of property transformations
     */
    protected Map<ClassInfo, SortedMap<String, List<PropertyTransformation>>> propertyTransformationsForBuildingBlockOfServiceConfigCollectionsByTopLevelClass = new HashMap<>();

    public void addPropertyTransformationToBuildingBlockOfCollectionInServiceConfiguration(ClassInfo topLevelClass,
	    String propertyPath, ImmutablePropertyTransformation trf) {

	SortedMap<String, List<PropertyTransformation>> serviceConfigTrfsByPropPath;
	if (propertyTransformationsForBuildingBlockOfServiceConfigCollectionsByTopLevelClass
		.containsKey(topLevelClass)) {
	    serviceConfigTrfsByPropPath = propertyTransformationsForBuildingBlockOfServiceConfigCollectionsByTopLevelClass
		    .get(topLevelClass);
	} else {
	    serviceConfigTrfsByPropPath = new TreeMap<>();
	    propertyTransformationsForBuildingBlockOfServiceConfigCollectionsByTopLevelClass.put(topLevelClass,
		    serviceConfigTrfsByPropPath);
	}

	if (serviceConfigTrfsByPropPath.containsKey(propertyPath)) {
	    serviceConfigTrfsByPropPath.get(propertyPath).add(trf);
	} else {
	    List<PropertyTransformation> propTransforms = new ArrayList<>();
	    propTransforms.add(trf);
	    serviceConfigTrfsByPropPath.put(propertyPath, propTransforms);
	}
    }

    public Map<ClassInfo, SortedMap<String, List<PropertyTransformation>>> getPropertyTransformationsForBuildingBlockOfServiceConfigCollectionsByTopLevelClass() {
	return this.propertyTransformationsForBuildingBlockOfServiceConfigCollectionsByTopLevelClass;
    }
}
