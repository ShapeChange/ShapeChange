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

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Target.sql_encoding_util.SqlClassEncodingInfo;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class LdpSqlProviderHelper {

    public String databaseTableName(ClassInfo ci, boolean isAssociativeTableContextAndNotReflexiveRelationshipContext) {

	if (!Ldproxy2Target.sqlEncodingInfos.isEmpty() && Ldproxy2Target.sqlEncodingInfos.hasClassEncodingInfo(ci)) {

	    SqlClassEncodingInfo scei = Ldproxy2Target.sqlEncodingInfos.getClassEncodingInfo(ci);

	    return scei.getTable();

	} else {

	    String result = ci.name();

	    if (isAssociativeTableContextAndNotReflexiveRelationshipContext) {
		result = result + Ldproxy2Target.associativeTableColumnSuffix;
	    }

	    result = result.toLowerCase(Locale.ENGLISH);

	    result = StringUtils.substring(result, 0, Ldproxy2Target.maxNameLength);

	    return result;
	}
    }
}
