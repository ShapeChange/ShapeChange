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
 * (c) 2002-2017 interactive instruments GmbH, Bonn, Germany
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
package de.interactive_instruments.ShapeChange.Target;

import java.util.SortedSet;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;

public class TargetUtil {
	
	public static final String PARAM_MAIN_APP_SCHEMA = "mainAppSchema";

	private TargetUtil() {}

	public static PackageInfo findMainSchemaForSingleTargets(SortedSet<? extends PackageInfo> selectedSchemas, Options options, ShapeChangeResult result) {
		PackageInfo mainAppSchema;
		String mainAppSchemaName = options.parameter(TargetUtil.PARAM_MAIN_APP_SCHEMA);
		if (StringUtils.isBlank(mainAppSchemaName)) {
			if (selectedSchemas.size() == 1) {
				mainAppSchema = selectedSchemas.first();
			} else {
				mainAppSchema = null;
			}
		} else {
			Optional<? extends PackageInfo> tryFindMainAppSchemaResult = Iterables.tryFind(selectedSchemas, new Predicate<PackageInfo>() {
				
				@Override
				public boolean apply(PackageInfo packageInfo) {
					return mainAppSchemaName.equalsIgnoreCase(packageInfo.name());
				}
			});
			if (tryFindMainAppSchemaResult.isPresent()) {
				mainAppSchema = tryFindMainAppSchemaResult.get();
			} else {
				result.addError("Parameter " + TargetUtil.PARAM_MAIN_APP_SCHEMA + " set to " + mainAppSchemaName + " but no schema with this name was selected in the configuration");
				mainAppSchema = null;
			}
		}
		return mainAppSchema;
	}
	
	

}
