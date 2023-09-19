package de.interactive_instruments.ShapeChange.Target.Ldproxy2.provider;

import java.util.Optional;

import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.interactive_instruments.ShapeChange.Model.ClassInfo;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.Ldproxy2.LdpPropertyEncodingContext;
import de.interactive_instruments.ShapeChange.Target.Ldproxy2.LdpSourcePathInfo;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public interface LdpProvider {

    /**
     * @param ci               The type for which to property encoding context is
     *                         created
     * @param isTypeDefinition <code>true</code>, if the property is encoded in a
     *                         type definition, <code>false</code> if it is encoded
     *                         in a fragment definition
     * @return the new initial context for encoding properties
     */
    public LdpPropertyEncodingContext createInitialPropertyEncodingContext(ClassInfo ci, boolean isTypeDefinition);

    public LdpPropertyEncodingContext createChildContext(LdpPropertyEncodingContext parentContext,
	    ClassInfo actualTypeCi, LdpSourcePathInfo spi);

    public LdpPropertyEncodingContext createChildContext(LdpPropertyEncodingContext parentContext, ClassInfo typeCi);

    public Type valueTypeForFeatureRef(PropertyInfo pi, LdpSourcePathInfo spi);

    public Optional<ClassInfo> actualTypeClass(LdpSourcePathInfo spi, PropertyInfo pi);

    public Type objectIdentifierType();

    public boolean isDatatypeWithSubtypesEncodedInFragmentWithSingularSchemaAndObjectType();

}
