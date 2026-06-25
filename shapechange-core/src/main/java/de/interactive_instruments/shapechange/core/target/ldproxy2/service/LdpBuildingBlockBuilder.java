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
 * Bundeskanzlerplatz 2d
 * 53113 Bonn
 * Germany
 */
package de.interactive_instruments.shapechange.core.target.ldproxy2.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import de.ii.ldproxy.cfg.LdproxyCfgWriter;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.interactive_instruments.shapechange.core.model.ClassInfo;
import de.interactive_instruments.shapechange.core.model.PropertyInfo;
import de.interactive_instruments.shapechange.core.target.ldproxy2.Ldproxy2Constants;

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

    /**
     * Property transformations, to be added to the global building blocks in the
     * service configuration.
     * 
     * map with key: property path, value: list of property transformations
     */
    protected SortedMap<String, List<PropertyTransformation>> propertyTransformationsForGlobalBuildingBlockOfServiceConfig = new TreeMap<>();

    public void addPropertyTransformationToBuildingBlockOfCollectionInServiceConfiguration(ClassInfo topLevelClass,
	    PropertyInfo pi, String propertyPath, ImmutablePropertyTransformation trf) {

	SortedMap<String, List<PropertyTransformation>> serviceConfigTrfsByPropPath;

	/*
	 * Determine where to add the property transformation in the service
	 * configuration: If tagged value ldpGlobalPropertyTransformations=true, then
	 * add the transformations globally. Otherwise add them within the collection
	 * definition.
	 */
	if (isGlobalPropertyDefinitions(pi)) {

	    serviceConfigTrfsByPropPath = propertyTransformationsForGlobalBuildingBlockOfServiceConfig;

	} else {

	    if (propertyTransformationsForBuildingBlockOfServiceConfigCollectionsByTopLevelClass
		    .containsKey(topLevelClass)) {
		serviceConfigTrfsByPropPath = propertyTransformationsForBuildingBlockOfServiceConfigCollectionsByTopLevelClass
			.get(topLevelClass);
	    } else {
		serviceConfigTrfsByPropPath = new TreeMap<>();
		propertyTransformationsForBuildingBlockOfServiceConfigCollectionsByTopLevelClass.put(topLevelClass,
			serviceConfigTrfsByPropPath);
	    }
	}

	List<PropertyTransformation> propertyTransformations;
	if (serviceConfigTrfsByPropPath.containsKey(propertyPath)) {
	    propertyTransformations = serviceConfigTrfsByPropPath.get(propertyPath);
	} else {
	    propertyTransformations = new ArrayList<>();
	    serviceConfigTrfsByPropPath.put(propertyPath, propertyTransformations);
	}

	// only add the transformation if it is not already contained in the list
	if (!propertyTransformations.stream().anyMatch(pt -> pt.equals(trf))) {
	    propertyTransformations.add(trf);
	}
    }

    protected boolean isGlobalPropertyDefinitions(PropertyInfo pi) {
	return "true".equalsIgnoreCase(pi.taggedValue(Ldproxy2Constants.TV_GLOBAL_PROPERTY_DEFINITIONS));
    }

    public Map<ClassInfo, SortedMap<String, List<PropertyTransformation>>> getPropertyTransformationsForBuildingBlockOfServiceConfigCollectionsByTopLevelClass() {
	return this.propertyTransformationsForBuildingBlockOfServiceConfigCollectionsByTopLevelClass;
    }

    public boolean hasTransformations(ClassInfo ci) {
	return this.propertyTransformationsForBuildingBlockOfServiceConfigCollectionsByTopLevelClass.containsKey(ci)
		&& !this.propertyTransformationsForBuildingBlockOfServiceConfigCollectionsByTopLevelClass.get(ci)
			.isEmpty();
    }

    public abstract boolean hasInputForServiceCollection(ClassInfo ci);

    public abstract ExtensionConfiguration getConfigurationForServiceCollection(LdproxyCfgWriter cfg, ClassInfo ci);

    public abstract ExtensionConfiguration getServiceConfiguration(LdproxyCfgWriter cfg);
}
