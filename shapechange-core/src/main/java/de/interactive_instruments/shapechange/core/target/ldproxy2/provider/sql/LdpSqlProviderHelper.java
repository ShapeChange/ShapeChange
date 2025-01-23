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
package de.interactive_instruments.shapechange.core.target.ldproxy2.provider.sql;

import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import de.interactive_instruments.shapechange.core.target.sql_encoding_util.SqlClassEncodingInfo;
import de.interactive_instruments.shapechange.core.model.ClassInfo;
import de.interactive_instruments.shapechange.core.model.PropertyInfo;
import de.interactive_instruments.shapechange.core.target.ldproxy2.Ldproxy2Constants;
import de.interactive_instruments.shapechange.core.target.ldproxy2.Ldproxy2Target;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class LdpSqlProviderHelper {

    /**
     * @param ci                                                          - tbd
     * @param isAssociativeTableContextAndNotReflexiveRelationshipContext - tbd
     * @return the determined database table name for ci; can be <code>null</code>,
     *         if ci is a mixin, or if ci is a datatype, and those types are encoded
     *         in the SQL with usage-specific tables (e.g. multiple sql encoding
     *         infos were found for the class, or no encoding infos are available
     *         but the class matches
     *         {@value Ldproxy2Constants#RULE_CLS_DATATYPES_ONETOMANY_SEVERAL_TABLES})
     */
    public String databaseTableName(ClassInfo ci, boolean isAssociativeTableContextAndNotReflexiveRelationshipContext) {

	if (!Ldproxy2Target.sqlEncodingInfos.isEmpty() && Ldproxy2Target.sqlEncodingInfos.hasClassEncodingInfo(ci)) {

	    List<SqlClassEncodingInfo> scei = Ldproxy2Target.sqlEncodingInfos.getClassEncodingInfos(ci);

	    if (scei.size() == 1) {
		return scei.getFirst().getTable();
	    } else {
		return null;
	    }

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

    /**
     * @param pi - tbd
     * @return the value of tag 'AAA:Kennung', in lower case (locale: english)
     */
    public String aaaKennung(PropertyInfo pi) {
	return pi.taggedValue("AAA:Kennung").toLowerCase(Locale.ENGLISH);
    }
}
