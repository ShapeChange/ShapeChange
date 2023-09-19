package de.interactive_instruments.ShapeChange.Target.Ldproxy2.provider;

import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.interactive_instruments.ShapeChange.Model.PropertyInfo;
import de.interactive_instruments.ShapeChange.Target.Ldproxy2.LdpSourcePathInfo;

/**
 * @author Johannes Echterhoff (echterhoff at interactive-instruments dot de)
 *
 */
public abstract class AbstractLdpProvider implements LdpProvider {

    @Override
    public Type valueTypeForFeatureRef(PropertyInfo pi, LdpSourcePathInfo spi) {
	return spi.valueType != null ? spi.valueType : Type.INTEGER;
    }
}
