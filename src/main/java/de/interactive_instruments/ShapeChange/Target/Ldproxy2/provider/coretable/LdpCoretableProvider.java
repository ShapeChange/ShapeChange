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
package de.interactive_instruments.ShapeChange.Target.Ldproxy2.provider.coretable;

import java.util.Optional;

import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.Ldproxy2.LdpPropertyEncodingContext;
import de.interactive_instruments.ShapeChange.Target.Ldproxy2.LdpSourcePathInfo;
import de.interactive_instruments.ShapeChange.Target.Ldproxy2.Ldproxy2Target;
import de.interactive_instruments.ShapeChange.Target.Ldproxy2.provider.AbstractLdpProvider;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public class LdpCoretableProvider extends AbstractLdpProvider {

    @Override
    public Type idValueTypeForFeatureRef(PropertyInfo pi, LdpSourcePathInfo spi) {
	return spi.getIdValueType().isPresent() ? spi.getIdValueType().get()
		: Ldproxy2Target.coretableIdColumnLdproxyType;
    }

    @Override
    public LdpPropertyEncodingContext createInitialPropertyEncodingContext(ClassInfo ci, boolean isTypeDefinition) {

	LdpPropertyEncodingContext pec = new LdpPropertyEncodingContext();
	pec.setType(ci);
	pec.setInFragment(!isTypeDefinition);
	return pec;
    }

    protected LdpPropertyEncodingContext createChildContextBase(LdpPropertyEncodingContext parentContext,
	    ClassInfo typeCi) {

	LdpPropertyEncodingContext childContext = new LdpPropertyEncodingContext();

	childContext.setInFragment(parentContext.isInFragment());
	childContext.setType(typeCi);
	childContext.setParentContext(parentContext);

	return childContext;
    }

    @Override
    public LdpPropertyEncodingContext createChildContext(LdpPropertyEncodingContext parentContext, ClassInfo typeCi,
	    LdpSourcePathInfo spix) {

	LdpPropertyEncodingContext childContext = createChildContextBase(parentContext, typeCi);

	return childContext;
    }

    @Override
    public LdpPropertyEncodingContext createChildContext(LdpPropertyEncodingContext parentContext, ClassInfo typeCi) {

	LdpPropertyEncodingContext childContext = createChildContextBase(parentContext, typeCi);

	return childContext;
    }

    /**
     * @param spi - tbd
     * @param pi  - tbd
     * @return the actual type class; can be empty if the class could not be
     *         determined
     */
    public Optional<ClassInfo> actualTypeClass(LdpSourcePathInfo spi, PropertyInfo pi) {
	return Optional.ofNullable(pi.typeClass());
    }

    @Override
    public Type objectIdentifierType() {
	return Ldproxy2Target.coretableIdColumnLdproxyType;
    }

    @Override
    public boolean isDatatypeWithSubtypesEncodedInFragmentWithSingularSchemaAndObjectType() {
	return true;
    }
}
