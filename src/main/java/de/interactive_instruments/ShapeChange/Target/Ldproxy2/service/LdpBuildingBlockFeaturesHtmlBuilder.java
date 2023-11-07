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
package de.interactive_instruments.ShapeChange.Target.Ldproxy2.service;

import java.util.List;
import java.util.SortedMap;

import de.ii.ldproxy.cfg.LdproxyCfgWriter;
import de.ii.ogcapi.features.html.domain.ImmutableFeaturesHtmlConfiguration;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Target.Ldproxy2.LdpInfo;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class LdpBuildingBlockFeaturesHtmlBuilder extends LdpBuildingBlockBuilder {

    public ImmutableFeaturesHtmlConfiguration createConfigurationForServiceCollection(LdproxyCfgWriter cfg, ClassInfo ci) {

	ImmutableFeaturesHtmlConfiguration.Builder fhtmlBuilder = cfg.builder().ogcApiExtension().featuresHtml();

	fhtmlBuilder.featureTitleTemplate(LdpInfo.featureTitleTemplate(ci));

	if (super.propertyTransformationsForBuildingBlockOfServiceConfigCollectionsByTopLevelClass.containsKey(ci)) {
	    SortedMap<String, List<PropertyTransformation>> featuresHtmlPropertyTransformations = super.propertyTransformationsForBuildingBlockOfServiceConfigCollectionsByTopLevelClass
		    .get(ci);
	    fhtmlBuilder.transformations(featuresHtmlPropertyTransformations);
	}

	return fhtmlBuilder.build();
    }

}
