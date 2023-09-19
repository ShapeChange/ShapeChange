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
package de.interactive_instruments.ShapeChange.Target.Ldproxy2.provider.sql;

import java.util.Optional;
import java.util.SortedSet;

import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.PackageInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.Ldproxy2.LdpPropertyEncodingContext;
import de.interactive_instruments.ShapeChange.Target.Ldproxy2.LdpSourcePathInfo;
import de.interactive_instruments.ShapeChange.Target.Ldproxy2.Ldproxy2Constants;
import de.interactive_instruments.ShapeChange.Target.Ldproxy2.Ldproxy2Target;
import de.interactive_instruments.ShapeChange.Target.Ldproxy2.provider.AbstractLdpProvider;
import de.interactive_instruments.ShapeChange.Target.sql_encoding_util.SqlClassEncodingInfo;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class LdpSqlProvider extends AbstractLdpProvider {

    protected LdpSqlProviderHelper sqlProviderHelper = new LdpSqlProviderHelper();

    @Override
    public LdpSqlPropertyEncodingContext createInitialPropertyEncodingContext(ClassInfo ci, boolean isTypeDefinition) {

	LdpSqlPropertyEncodingContext pec = new LdpSqlPropertyEncodingContext();

	pec.setType(ci);

	if (isTypeDefinition) {

	    pec.setInFragment(false);
	    pec.setSourceTable(sqlProviderHelper.databaseTableName(ci, false));

	} else {

	    pec.setInFragment(true);

	    // if fragments are enabled, the source path for datatype is only available for
	    // non-usage-specific datatype encoding
	    if (ci.category() == Options.MIXIN || (ci.category() == Options.DATATYPE
		    && ci.matches(Ldproxy2Constants.RULE_CLS_DATATYPES_ONETOMANY_SEVERAL_TABLES))) {
		pec.setSourceTable(sqlProviderHelper.databaseTableName(ci, false));
	    }
	}

	return pec;
    }

    protected LdpSqlPropertyEncodingContext createChildContextBase(LdpPropertyEncodingContext parentContext,
	    ClassInfo typeCi) {

	LdpSqlPropertyEncodingContext childContext = new LdpSqlPropertyEncodingContext();

	childContext.setInFragment(parentContext.isInFragment());
	childContext.setType(typeCi);
	childContext.setParentContext(parentContext);

	return childContext;
    }

    @Override
    public LdpPropertyEncodingContext createChildContext(LdpPropertyEncodingContext parentContext, ClassInfo typeCi,
	    LdpSourcePathInfo spix) {

	LdpSqlPropertyEncodingContext childContext = createChildContextBase(parentContext, typeCi);

	LdpSqlSourcePathInfo spi = (LdpSqlSourcePathInfo) spix;
	childContext.setSourceTable(spi.targetTable);

	return childContext;
    }

    @Override
    public LdpPropertyEncodingContext createChildContext(LdpPropertyEncodingContext parentContext, ClassInfo typeCi) {

	LdpSqlPropertyEncodingContext childContext = createChildContextBase(parentContext, typeCi);

	childContext.setSourceTable(sqlProviderHelper.databaseTableName(typeCi, false));

	return childContext;
    }
    
    /**
     * @param spix - tbd
     * @param pi              - tbd
     * @return the actual type class; can be empty if the class could not be
     *         determined
     */
    public Optional<ClassInfo> actualTypeClass(LdpSourcePathInfo spix, PropertyInfo pi) {
	
	LdpSqlSourcePathInfo spi = (LdpSqlSourcePathInfo)spix;	
	String targetTable = spi.targetTable;

	if (!Ldproxy2Target.sqlEncodingInfos.isEmpty()
		&& Ldproxy2Target.sqlEncodingInfos.hasClassEncodingInfoForTable(targetTable)) {

	    SqlClassEncodingInfo scei = Ldproxy2Target.sqlEncodingInfos.getClassEncodingInfoForTable(targetTable);

	    String className = scei.hasOriginalClassName() ? scei.getOriginalClassName() : scei.getClassName();
	    String schemaName = scei.hasOriginalSchemaName() ? scei.getOriginalSchemaName() : scei.getSchemaName();

	    SortedSet<PackageInfo> schemas = Ldproxy2Target.model.schemas(schemaName);

	    for (PackageInfo schema : schemas) {
		Optional<ClassInfo> optCi = Ldproxy2Target.model.classes(schema).stream()
			.filter(ci -> ci.name().equals(className)).findFirst();
		if (optCi.isPresent()) {
		    return optCi;
		}
	    }
	}

	// use the type class of pi as fallback
	return Optional.ofNullable(pi.typeClass());
    }

    @Override
    public Type objectIdentifierType() {
	return Type.INTEGER;
    }

    @Override
    public boolean isDatatypeWithSubtypesEncodedInFragmentWithSingularSchemaAndObjectType() {
	return false;
    }
    
    
}
