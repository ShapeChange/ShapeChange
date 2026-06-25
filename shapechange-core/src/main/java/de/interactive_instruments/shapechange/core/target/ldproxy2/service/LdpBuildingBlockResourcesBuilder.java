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
package de.interactive_instruments.shapechange.core.target.ldproxy2.service;

import de.ii.ldproxy.cfg.LdproxyCfgWriter;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.resources.domain.ImmutableResourcesConfiguration;
import de.interactive_instruments.shapechange.core.model.ClassInfo;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class LdpBuildingBlockResourcesBuilder extends LdpBuildingBlockBuilder {

    @Override
    public ImmutableResourcesConfiguration getConfigurationForServiceCollection(LdproxyCfgWriter cfg, ClassInfo ci) {
	return null;
    }

    @Override
    public boolean hasInputForServiceCollection(ClassInfo ci) {
	return false;
    }

    @Override
    public ExtensionConfiguration getServiceConfiguration(LdproxyCfgWriter cfg) {

	ImmutableResourcesConfiguration.Builder scConfigBuilder = cfg.builder().ogcApiExtension().resources();
	scConfigBuilder.enabled(true);

	return scConfigBuilder.build();
    }

}
