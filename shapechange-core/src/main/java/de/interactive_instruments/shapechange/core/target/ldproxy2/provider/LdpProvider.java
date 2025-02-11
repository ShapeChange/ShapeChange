package de.interactive_instruments.shapechange.core.target.ldproxy2.provider;

import java.util.Optional;

import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.interactive_instruments.shapechange.core.model.ClassInfo;
import de.interactive_instruments.shapechange.core.model.PropertyInfo;
import de.interactive_instruments.shapechange.core.target.ldproxy2.LdpPropertyEncodingContext;
import de.interactive_instruments.shapechange.core.target.ldproxy2.LdpSourcePathInfo;

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

    public Type idValueTypeForFeatureRef(PropertyInfo pi, LdpSourcePathInfo spi);

    public Optional<ClassInfo> actualTypeClass(LdpSourcePathInfo spi, PropertyInfo pi);

    public Type objectIdentifierType();

    public boolean isDatatypeWithSubtypesEncodedInFragmentWithSingularSchemaAndObjectType();

}
